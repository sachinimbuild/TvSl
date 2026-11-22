package com.example.ui.components

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SignalCellularConnectedNoInternet4Bar
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.model.BufferSegmentInfo
import com.example.model.DataSaverPreset
import com.example.model.NetworkTelemetry
import com.example.model.PlayerPlaybackState
import com.example.model.TvChannel
import com.example.ui.theme.AmberGoldTertiary
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.MonoMetricStyle
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateContainerHigh
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.FormatUtils
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun LiveTvPlayerStage(
    player: Player,
    activeChannel: TvChannel?,
    playbackState: PlayerPlaybackState,
    bufferInfo: BufferSegmentInfo,
    networkTelemetry: NetworkTelemetry,
    dataSaverPreset: DataSaverPreset,
    isPlaying: Boolean,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekBackBy: (Long) -> Unit,
    onSeekForwardBy: (Long) -> Unit,
    onSeekToFraction: (Float) -> Unit,
    onJumpToLive: () -> Unit,
    onRetry: () -> Unit,
    onTryBackup: () -> Unit,
    onOpenDataSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var controlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(controlsVisible, isPlaying, playbackState) {
        if (controlsVisible && isPlaying && playbackState is PlayerPlaybackState.Playing) {
            delay(4_500L)
            controlsVisible = false
        }
    }

    val stageModifier = if (isFullscreen) {
        modifier
            .fillMaxSize()
            .background(Color.Black)
    } else {
        modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(20.dp))
            .background(ObsidianBg)
    }

    Box(
        modifier = stageModifier
            .testTag("live_player_stage")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                controlsVisible = !controlsVisible
            },
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    keepScreenOn = true
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    this.player = player
                }
            },
            update = { view ->
                if (view.player !== player) {
                    view.player = player
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Buffering spinner when stream is connecting
        if (playbackState is PlayerPlaybackState.Buffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(
                        color = SaffronPrimary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp),
                    )
                    Text(
                        text = "Buffering ${activeChannel?.name ?: "Live TV"} (${dataSaverPreset.shortLabel})...",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                    )
                }
            }
        }

        // Error state overlay with real recovery actions
        if (playbackState is PlayerPlaybackState.Error) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBg.copy(alpha = 0.9f))
                    .padding(18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.SignalCellularConnectedNoInternet4Bar,
                        contentDescription = null,
                        tint = LiveCrimson,
                        modifier = Modifier.size(32.dp),
                    )
                    Text(
                        text = activeChannel?.name ?: "Channel Unavailable",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Text(
                        text = playbackState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                            modifier = Modifier.testTag("retry_stream_button"),
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retry", color = Color.White)
                        }
                        if (playbackState.hasBackup) {
                            OutlinedButton(
                                onClick = onTryBackup,
                                border = BorderStroke(1.dp, EmeraldSecondary),
                                modifier = Modifier.testTag("backup_stream_button"),
                            ) {
                                Icon(
                                    Icons.Filled.SwapHoriz,
                                    contentDescription = null,
                                    tint = EmeraldSecondary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Try Mirror", color = EmeraldSecondary)
                            }
                        }
                    }
                }
            }
        }

        // Broadcast Studio HUD Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible && playbackState !is PlayerPlaybackState.Error,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.72f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f),
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                // Top HUD Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        LiveOrTimeshiftBadge(
                            isAtLiveEdge = bufferInfo.isAtLiveEdge,
                            offsetFromLiveMs = bufferInfo.currentOffsetFromLiveMs,
                            onClick = onJumpToLive,
                        )
                        Text(
                            text = activeChannel?.name ?: "Select Channel",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onOpenDataSheet)
                                .testTag("hud_data_preset_pill"),
                            color = SlateContainerHigh.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, EmeraldSecondary.copy(alpha = 0.45f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    Icons.Filled.DataSaverOn,
                                    contentDescription = "Data Saver",
                                    tint = EmeraldSecondary,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = if (networkTelemetry.videoResolution != "--") {
                                        networkTelemetry.videoResolution
                                    } else {
                                        dataSaverPreset.shortLabel
                                    },
                                    style = MonoMetricStyle,
                                    color = TextPrimary,
                                )
                            }
                        }

                        IconButton(
                            onClick = onToggleFullscreen,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("fullscreen_toggle_button"),
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                contentDescription = if (isFullscreen) "Exit fullscreen" else "Fullscreen",
                                tint = TextPrimary,
                            )
                        }
                    }
                }

                // Center Transport Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { onSeekBackBy(15_000L) }
                            .testTag("hud_rewind_15s_button"),
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = CircleShape,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Replay10,
                                contentDescription = "Rewind 15 seconds",
                                tint = TextPrimary,
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onTogglePlayPause)
                            .testTag("hud_play_pause_button"),
                        color = SaffronPrimary.copy(alpha = 0.92f),
                        shape = CircleShape,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { onSeekForwardBy(15_000L) }
                            .testTag("hud_forward_15s_button"),
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = CircleShape,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Forward10,
                                contentDescription = "Forward 15 seconds",
                                tint = TextPrimary,
                            )
                        }
                    }
                }

                // Bottom 1-Hour Reverse Buffer Scrubber
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ReverseBufferTimelineBar(
                        bufferInfo = bufferInfo,
                        onSeekFraction = onSeekToFraction,
                        onJumpToLive = onJumpToLive,
                    )
                }
            }
        }
    }
}

@Composable
fun LiveOrTimeshiftBadge(
    isAtLiveEdge: Boolean,
    offsetFromLiveMs: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (isAtLiveEdge) LiveCrimson else AmberGoldTertiary
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag("live_edge_badge"),
        color = bg,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Text(
                text = if (isAtLiveEdge) "LIVE" else "DVR ${FormatUtils.formatOffsetBehindLive(offsetFromLiveMs)}",
                style = MonoMetricStyle,
                color = if (isAtLiveEdge) Color.White else Color.Black,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun ReverseBufferTimelineBar(
    bufferInfo: BufferSegmentInfo,
    onSeekFraction: (Float) -> Unit,
    onJumpToLive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxCapText = FormatUtils.formatDurationMmSs(bufferInfo.maxBufferCapacityMs)
    val bufferedText = FormatUtils.formatDurationMmSs(bufferInfo.bufferedDurationMs)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Scrubbable track representing the 1-hour reverse buffer window
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .testTag("reverse_buffer_scrubber")
                .pointerInput(bufferInfo.bufferedDurationMs) {
                    detectTapGestures { offset ->
                        val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeekFraction(frac)
                    }
                }
                .pointerInput(bufferInfo.bufferedDurationMs) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val frac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeekFraction(frac)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            ) {
                val trackHeight = 6.dp.toPx()
                val cy = size.height / 2f
                val topY = cy - trackHeight / 2f

                // Total 60-min capacity backdrop
                drawRoundRect(
                    color = SlateBorder.copy(alpha = 0.7f),
                    topLeft = Offset(0f, topY),
                    size = Size(size.width, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f),
                )

                // Active buffered DVR portion (aligned right to live edge)
                val fillW = (size.width * bufferInfo.bufferFillFraction).coerceAtLeast(10.dp.toPx())
                val bufferStartX = (size.width - fillW).coerceAtLeast(0f)

                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(EmeraldSecondary.copy(alpha = 0.75f), SaffronPrimary),
                        startX = bufferStartX,
                        endX = size.width,
                    ),
                    topLeft = Offset(bufferStartX, topY),
                    size = Size(fillW, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f),
                )

                // Playhead thumb inside buffered segment
                val thumbX = (bufferStartX + fillW * bufferInfo.playheadFractionOfBuffer)
                    .coerceIn(7.dp.toPx(), size.width - 7.dp.toPx())

                drawCircle(
                    color = Color.White,
                    radius = 7.dp.toPx(),
                    center = Offset(thumbX, cy),
                )
                drawCircle(
                    color = if (bufferInfo.isAtLiveEdge) LiveCrimson else SaffronPrimary,
                    radius = 4.dp.toPx(),
                    center = Offset(thumbX, cy),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Filled.History,
                    contentDescription = null,
                    tint = EmeraldSecondary,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = "DVR $bufferedText / $maxCapText",
                    style = MonoMetricStyle,
                    color = TextSecondary,
                )
            }

            if (!bufferInfo.isAtLiveEdge) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onJumpToLive)
                        .testTag("go_live_button"),
                    color = LiveCrimson,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = "● GO LIVE",
                        style = MonoMetricStyle,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            } else {
                Text(
                    text = "LIVE EDGE",
                    style = MonoMetricStyle,
                    color = EmeraldSecondary,
                )
            }
        }
    }
}

@Composable
fun QuickRewindStrip(
    bufferInfo: BufferSegmentInfo,
    onSeekBackBy: (Long) -> Unit,
    onJumpToLive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val presets = remember {
        listOf(
            "-30s" to 30_000L,
            "-5m" to 5 * 60_000L,
            "-15m" to 15 * 60_000L,
            "-30m" to 30 * 60_000L,
            "-1h" to 60 * 60_000L,
        )
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(presets) { (label, deltaMs) ->
            val enabled = bufferInfo.bufferedDurationMs >= 10_000L
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = enabled) { onSeekBackBy(deltaMs) }
                    .testTag("quick_rewind_${label.replace("-", "")}"),
                color = if (enabled) SlateContainerHigh else SlateContainerHigh.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SlateBorder),
            ) {
                Text(
                    text = label,
                    style = MonoMetricStyle,
                    color = if (enabled) TextPrimary else TextMuted,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onJumpToLive)
                    .testTag("quick_jump_live"),
                color = if (bufferInfo.isAtLiveEdge) LiveCrimson.copy(alpha = 0.2f) else LiveCrimson,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, LiveCrimson),
            ) {
                Text(
                    text = "● LIVE",
                    style = MonoMetricStyle,
                    color = if (bufferInfo.isAtLiveEdge) LiveCrimson else Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}
