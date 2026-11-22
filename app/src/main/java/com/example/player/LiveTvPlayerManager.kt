package com.example.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.model.BufferSegmentInfo
import com.example.model.DataSaverPreset
import com.example.model.NetworkTelemetry
import com.example.model.NetworkType
import com.example.model.PlayerPlaybackState
import com.example.model.TvChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

@OptIn(UnstableApi::class)
class LiveTvPlayerManager(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val timeshiftProxy = HlsTimeshiftBufferProxy(appContext, okHttpClient)
    private val trackSelector = DefaultTrackSelector(appContext)
    private val sessionBytesCounter = AtomicLong(0L)

    val player: ExoPlayer

    private val _playbackState = MutableStateFlow<PlayerPlaybackState>(PlayerPlaybackState.Idle)
    val playbackState: StateFlow<PlayerPlaybackState> = _playbackState.asStateFlow()

    private val _bufferInfo = MutableStateFlow(BufferSegmentInfo())
    val bufferInfo: StateFlow<BufferSegmentInfo> = _bufferInfo.asStateFlow()

    private val _networkTelemetry = MutableStateFlow(NetworkTelemetry())
    val networkTelemetry: StateFlow<NetworkTelemetry> = _networkTelemetry.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var activeChannel: TvChannel? = null
    private var usingBackupUrl = false
    private var allowMobileData = true
    private var currentPreset = DataSaverPreset.DATA_SAVER
    private var bufferMinutes = 60
    private var positionTickerJob: Job? = null
    private var lastDiscontinuityRecoverMs = 0L
    private var discontinuityRecoverCount = 0

    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = refreshNetworkTelemetry()
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            refreshNetworkTelemetry()
        }
        override fun onLost(network: Network) = refreshNetworkTelemetry()
    }

    private class ResilientAudioSink(
        delegate: AudioSink,
    ) : ForwardingAudioSink(delegate) {

        override fun setListener(listener: AudioSink.Listener) {
            super.setListener(object : AudioSink.Listener by listener {
                override fun onAudioSinkError(audioSinkError: Exception) {
                    if (audioSinkError is AudioSink.UnexpectedDiscontinuityException) {
                        this@ResilientAudioSink.handleDiscontinuity()
                    } else {
                        listener.onAudioSinkError(audioSinkError)
                    }
                }
            })
        }

        override fun handleBuffer(
            buffer: ByteBuffer,
            presentationTimeUs: Long,
            encodedAccessUnitCount: Int,
        ): Boolean {
            return try {
                super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
            } catch (_: AudioSink.UnexpectedDiscontinuityException) {
                super.handleDiscontinuity()
                super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
            }
        }
    }

    init {
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 LankaTV/1.0")

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 4_000,
                /* maxBufferMs = */ 25_000,
                /* bufferForPlaybackMs = */ 1_500,
                /* bufferForPlaybackAfterRebufferMs = */ 3_000,
            )
            // Keep up to 2 minutes in RAM as an additional back-buffer for non-HLS fallback streams
            .setBackBuffer(120_000, true)
            .build()

        val renderersFactory = object : DefaultRenderersFactory(appContext) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink? {
                val baseSink = super.buildAudioSink(
                    context,
                    enableFloatOutput,
                    enableAudioTrackPlaybackParams,
                ) ?: return null
                return ResilientAudioSink(baseSink)
            }
        }.setEnableDecoderFallback(true)

        player = ExoPlayer.Builder(appContext, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(dataSourceFactory)
                    .setLiveTargetOffsetMs(6_000L)
            )
            .build()

        timeshiftProxy.onNetworkBytesDownloaded = { bytes ->
            val total = sessionBytesCounter.addAndGet(bytes)
            _networkTelemetry.update { it.copy(sessionBytesUsed = total) }
        }

        scope.launch {
            timeshiftProxy.bufferInfo.collect { proxyInfo ->
                _bufferInfo.update { current ->
                    current.copy(
                        bufferedDurationMs = maxOf(proxyInfo.bufferedDurationMs, getExoLiveWindowMs()),
                        maxBufferCapacityMs = proxyInfo.maxBufferCapacityMs,
                        diskUsedBytes = proxyInfo.diskUsedBytes,
                        segmentsCached = proxyInfo.segmentsCached,
                        isProxyActive = proxyInfo.isProxyActive,
                        isDirectFallback = proxyInfo.isDirectFallback,
                    )
                }
            }
        }

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                syncPlaybackState()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                syncPlaybackState()
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    _networkTelemetry.update {
                        it.copy(videoResolution = "${videoSize.width}×${videoSize.height}")
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    player.seekToDefaultPosition()
                    player.prepare()
                    return
                }
                if (isAudioDiscontinuityError(error)) {
                    val now = System.currentTimeMillis()
                    if (now - lastDiscontinuityRecoverMs > 30_000L) {
                        discontinuityRecoverCount = 0
                    }
                    if (discontinuityRecoverCount < 5) {
                        lastDiscontinuityRecoverMs = now
                        discontinuityRecoverCount++
                        val duration = player.duration
                        if (duration != C.TIME_UNSET && duration > 0L) {
                            val nextPos = (player.currentPosition + 500L).coerceAtMost((duration - 3_000L).coerceAtLeast(0L))
                            player.seekTo(nextPos)
                        } else {
                            player.seekToDefaultPosition()
                        }
                        player.prepare()
                        player.play()
                        return
                    }
                }
                val hasBackup = !usingBackupUrl && activeChannel?.backupStreamUrl != null
                _playbackState.value = PlayerPlaybackState.Error(
                    message = mapErrorMessage(error),
                    hasBackup = hasBackup,
                )
            }
        })

        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onLoadCompleted(
                eventTime: AnalyticsListener.EventTime,
                loadEventInfo: LoadEventInfo,
                mediaLoadData: MediaLoadData,
            ) {
                // Avoid double counting localhost proxy reads; only count external loads
                val uri = loadEventInfo.uri.toString()
                if (!uri.startsWith("http://127.0.0.1")) {
                    val total = sessionBytesCounter.addAndGet(loadEventInfo.bytesLoaded)
                    _networkTelemetry.update { it.copy(sessionBytesUsed = total) }
                }
                val bitrate = mediaLoadData.trackFormat?.bitrate ?: C.RATE_UNSET_INT
                if (bitrate != C.RATE_UNSET_INT && bitrate > 0) {
                    _networkTelemetry.update { it.copy(currentBitrateKbps = bitrate / 1000) }
                }
            }
        })

        registerNetworkMonitor()
        refreshNetworkTelemetry()
        startPositionTicker()
    }

    fun playChannel(
        channel: TvChannel,
        preset: DataSaverPreset,
        allowMobile: Boolean,
        reverseBufferMin: Int,
        useBackup: Boolean = false,
    ) {
        activeChannel = channel
        usingBackupUrl = useBackup
        currentPreset = preset
        allowMobileData = allowMobile
        bufferMinutes = reverseBufferMin

        applyTrackConstraints(preset)
        timeshiftProxy.updateMaxBufferMinutes(reverseBufferMin)
        refreshNetworkTelemetry()

        val netType = _networkTelemetry.value.networkType
        if (netType == NetworkType.MOBILE_DATA && !allowMobile) {
            player.pause()
            _playbackState.value = PlayerPlaybackState.Error(
                message = "Mobile data streaming is paused in Data Settings.",
                hasBackup = false,
            )
            return
        }

        val targetUrl = if (useBackup && channel.backupStreamUrl != null) {
            channel.backupStreamUrl
        } else {
            channel.streamUrl
        }

        _playbackState.value = PlayerPlaybackState.Buffering
        val playbackUri = timeshiftProxy.startSession(channel.id, targetUrl, reverseBufferMin)

        val mediaItem = MediaItem.Builder()
            .setUri(playbackUri)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(6_000L)
                    .setMinPlaybackSpeed(1.0f)
                    .setMaxPlaybackSpeed(1.02f)
                    .build()
            )
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    fun applyDataSaverPreset(preset: DataSaverPreset) {
        currentPreset = preset
        applyTrackConstraints(preset)
    }

    fun updateMobileDataPolicy(allowed: Boolean) {
        allowMobileData = allowed
        val netType = _networkTelemetry.value.networkType
        if (netType == NetworkType.MOBILE_DATA && !allowed && player.isPlaying) {
            player.pause()
            _playbackState.value = PlayerPlaybackState.Error(
                message = "Mobile data streaming is disabled in settings.",
                hasBackup = false,
            )
        } else if (allowed && activeChannel != null && _playbackState.value is PlayerPlaybackState.Error) {
            retryCurrentChannel()
        }
    }

    fun updateReverseBufferMinutes(minutes: Int) {
        bufferMinutes = minutes.coerceIn(15, 60)
        timeshiftProxy.updateMaxBufferMinutes(bufferMinutes)
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (_playbackState.value is PlayerPlaybackState.Error) {
                retryCurrentChannel()
            } else {
                player.play()
            }
        }
    }

    fun seekBackBy(deltaMs: Long) {
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: return
        val current = player.currentPosition.coerceIn(0L, duration)
        val target = (current - deltaMs).coerceAtLeast(0L)
        player.seekTo(target)
        updateLiveOffsetSnapshot()
    }

    fun seekForwardBy(deltaMs: Long) {
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: return
        val current = player.currentPosition.coerceIn(0L, duration)
        val target = (current + deltaMs).coerceAtMost((duration - 4_000L).coerceAtLeast(0L))
        player.seekTo(target)
        updateLiveOffsetSnapshot()
    }

    fun seekToBufferFraction(fraction: Float) {
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: return
        val clamped = fraction.coerceIn(0f, 1f)
        val target = (duration * clamped).toLong().coerceIn(0L, (duration - 3_000L).coerceAtLeast(0L))
        player.seekTo(target)
        updateLiveOffsetSnapshot()
    }

    fun jumpToLiveEdge() {
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 }
        if (duration != null) {
            player.seekTo((duration - 4_000L).coerceAtLeast(0L))
        } else {
            player.seekToDefaultPosition()
        }
        player.play()
        updateLiveOffsetSnapshot()
    }

    fun tryBackupStream() {
        val ch = activeChannel ?: return
        if (ch.backupStreamUrl != null) {
            playChannel(ch, currentPreset, allowMobileData, bufferMinutes, useBackup = true)
        }
    }

    fun retryCurrentChannel() {
        val ch = activeChannel ?: return
        playChannel(ch, currentPreset, allowMobileData, bufferMinutes, useBackup = usingBackupUrl)
    }

    fun clearTimeshiftCache() {
        timeshiftProxy.clearCache()
    }

    fun release() {
        positionTickerJob?.cancel()
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        player.release()
        timeshiftProxy.shutdown()
        scope.cancel()
    }

    private fun applyTrackConstraints(preset: DataSaverPreset) {
        val builder = trackSelector.buildUponParameters()
        if (preset == DataSaverPreset.AUTO) {
            builder.clearVideoSizeConstraints()
                .setMaxVideoBitrate(Int.MAX_VALUE)
        } else {
            builder.setMaxVideoSize(Int.MAX_VALUE, preset.maxHeightPx)
                .setMaxVideoBitrate(preset.maxBitrateBps)
        }
        trackSelector.setParameters(builder.build())
    }

    private fun startPositionTicker() {
        positionTickerJob = scope.launch {
            while (isActive) {
                updateLiveOffsetSnapshot()
                delay(1000L)
            }
        }
    }

    private fun updateLiveOffsetSnapshot() {
        val exoDuration = getExoLiveWindowMs()
        val currentPos = player.currentPosition.coerceAtLeast(0L)
        val offsetFromLive = if (exoDuration > 0L) {
            (exoDuration - currentPos).coerceAtLeast(0L)
        } else {
            0L
        }
        _bufferInfo.update { info ->
            val mergedBuffered = maxOf(info.bufferedDurationMs, exoDuration)
                .coerceAtMost(info.maxBufferCapacityMs)
            info.copy(
                bufferedDurationMs = mergedBuffered,
                currentOffsetFromLiveMs = offsetFromLive,
            )
        }
    }

    private fun getExoLiveWindowMs(): Long {
        val d = player.duration
        return if (d == C.TIME_UNSET || d < 0L) 0L else d
    }

    private fun syncPlaybackState() {
        if (_playbackState.value is PlayerPlaybackState.Error &&
            player.playbackState == Player.STATE_IDLE
        ) return

        _playbackState.value = when (player.playbackState) {
            Player.STATE_BUFFERING -> PlayerPlaybackState.Buffering
            Player.STATE_READY -> if (player.isPlaying) PlayerPlaybackState.Playing else PlayerPlaybackState.Paused
            Player.STATE_ENDED -> PlayerPlaybackState.Paused
            else -> PlayerPlaybackState.Idle
        }
    }

    private fun registerNetworkMonitor() {
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching { connectivityManager.registerNetworkCallback(req, networkCallback) }
    }

    private fun refreshNetworkTelemetry() {
        val active = connectivityManager.activeNetwork
        val caps = active?.let { connectivityManager.getNetworkCapabilities(it) }
        val type = when {
            caps == null -> NetworkType.OFFLINE
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE_DATA
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            else -> NetworkType.OTHER
        }
        val isMetered = caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val downstreamKbps = caps?.linkDownstreamBandwidthKbps ?: 0

        _networkTelemetry.update {
            it.copy(
                networkType = type,
                isMetered = isMetered,
                downstreamBandwidthKbps = downstreamKbps,
            )
        }
    }

    private fun isAudioDiscontinuityError(error: PlaybackException): Boolean {
        var cause: Throwable? = error
        while (cause != null) {
            if (cause is AudioSink.UnexpectedDiscontinuityException) return true
            cause = cause.cause
        }
        return false
    }

    private fun mapErrorMessage(error: PlaybackException): String {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                "Network timeout or broadcaster stream unreachable on this connection."
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                "Channel stream temporarily offline or geo-restricted by broadcaster."
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ->
                "Unsupported or malformed stream playlist format."
            else -> error.localizedMessage ?: "Unable to play live stream."
        }
    }
}
