package com.example.player

import android.content.Context
import android.util.Log
import com.example.model.BufferSegmentInfo
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
import okhttp3.Request
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Local loopback HTTP HLS proxy that maintains a disk-backed sliding ring buffer
 * up to 1 hour (3,600,000 ms) of live TV segments.
 *
 * Benefits:
 * - Works on ALL live HLS streams even when upstream broadcasters only keep 18-30s on their CDN playlist.
 * - Rewinding up to 60 minutes serves cached segments straight from disk with ZERO extra mobile data usage.
 * - Preserves multivariant streams so ExoPlayer's DefaultTrackSelector still enforces 360p/480p/720p Data Saver caps.
 */
class HlsTimeshiftBufferProxy(
    private val context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
) {
    companion object {
        private const val TAG = "TimeshiftProxy"
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 LankaTV/1.0"
        private const val MAX_CACHE_BYTES = 384L * 1024L * 1024L // 384 MB mobile-safe disk cap
    }

    data class CachedSegment(
        val sequence: Long,
        val upstreamUrl: String,
        val durationSec: Double,
        val file: File,
        val byteSize: Long,
        val ext: String,
        val keyLine: String? = null,
        val programDateTimeLine: String? = null,
        val discontinuityBefore: Boolean = false,
    )

    private class VariantState(
        val index: Int,
        val upstreamUrl: String,
        val ingestLock: Any = Any(),
        val segments: ArrayDeque<CachedSegment> = ArrayDeque(),
        val seenUpstreamUrls: HashSet<String> = HashSet(),
        @Volatile var targetDurationSec: Int = 6,
        @Volatile var nextSequence: Long = 1L,
        @Volatile var lastUpstreamSeq: Long = -1L,
        @Volatile var lastUpstreamDiscontinuitySeq: Long = -1L,
        @Volatile var evictedDiscontinuities: Int = 0,
        @Volatile var mapLine: String? = null,
        @Volatile var lastActivePollMs: Long = 0L,
        @Volatile var hasByteRange: Boolean = false,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var port: Int = 0
    private val started = AtomicBoolean(false)

    private var currentChannelId: String = ""
    private var currentUpstreamUrl: String = ""
    private var maxBufferDurationMs: Long = 3_600_000L

    private val variants = ConcurrentHashMap<Int, VariantState>()
    private var activeVariantIdx: Int = 0
    private var pollerJob: Job? = null
    private var sessionRootDir: File? = null

    private val _bufferInfo = MutableStateFlow(BufferSegmentInfo())
    val bufferInfo: StateFlow<BufferSegmentInfo> = _bufferInfo.asStateFlow()

    private val totalProxyBytesDownloaded = AtomicLong(0L)
    var onNetworkBytesDownloaded: ((Long) -> Unit)? = null

    fun ensureStarted(): Int {
        if (started.compareAndSet(false, true)) {
            val socket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
            serverSocket = socket
            port = socket.localPort
            scope.launch { acceptLoop(socket) }
        }
        return port
    }

    fun startSession(channelId: String, upstreamUrl: String, maxBufferMinutes: Int): String {
        val localPort = ensureStarted()
        pollerJob?.cancel()
        variants.clear()
        clearAllCacheDirsExcept(null)

        currentChannelId = channelId
        currentUpstreamUrl = upstreamUrl
        maxBufferDurationMs = maxBufferMinutes.coerceIn(15, 60) * 60_000L
        activeVariantIdx = 0

        val root = File(context.cacheDir, "timeshift/${channelId.replace(Regex("[^a-zA-Z0-9_-]"), "_")}")
        root.mkdirs()
        sessionRootDir = root

        _bufferInfo.value = BufferSegmentInfo(
            bufferedDurationMs = 0L,
            currentOffsetFromLiveMs = 0L,
            maxBufferCapacityMs = maxBufferDurationMs,
            diskUsedBytes = 0L,
            segmentsCached = 0,
            isProxyActive = true,
            isDirectFallback = false,
        )

        val isLikelyHls = upstreamUrl.contains(".m3u8", ignoreCase = true) ||
            upstreamUrl.contains("m3u8", ignoreCase = true) ||
            upstreamUrl.contains("playlist", ignoreCase = true)

        return if (isLikelyHls) {
            "http://127.0.0.1:$localPort/master.m3u8"
        } else {
            _bufferInfo.update { it.copy(isProxyActive = false, isDirectFallback = true) }
            upstreamUrl
        }
    }

    fun updateMaxBufferMinutes(minutes: Int) {
        maxBufferDurationMs = minutes.coerceIn(15, 60) * 60_000L
        _bufferInfo.update { it.copy(maxBufferCapacityMs = maxBufferDurationMs) }
    }

    fun clearCache() {
        pollerJob?.cancel()
        variants.clear()
        clearAllCacheDirsExcept(null)
        _bufferInfo.update {
            it.copy(
                bufferedDurationMs = 0L,
                diskUsedBytes = 0L,
                segmentsCached = 0,
            )
        }
    }

    fun shutdown() {
        pollerJob?.cancel()
        runCatching { serverSocket?.close() }
        clearAllCacheDirsExcept(null)
        scope.cancel()
    }

    private suspend fun acceptLoop(server: ServerSocket) {
        while (scope.isActive && !server.isClosed) {
            try {
                val client = server.accept()
                scope.launch { handleClient(client) }
            } catch (_: Exception) {
                break
            }
        }
    }

    private suspend fun handleClient(socket: Socket) {
        socket.use { sock ->
            sock.soTimeout = 15_000
            val reader = BufferedReader(InputStreamReader(sock.getInputStream()))
            val out = sock.getOutputStream()

            val requestLine = reader.readLine() ?: return
            // Drain headers
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
            }

            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val path = parts[1].substringBefore("?")

            when {
                path == "/master.m3u8" -> serveMasterPlaylist(out)
                path.startsWith("/variant/") && path.endsWith("/playlist.m3u8") -> {
                    val idx = path.removePrefix("/variant/").removeSuffix("/playlist.m3u8").toIntOrNull() ?: 0
                    serveVariantPlaylist(idx, out)
                }
                path.startsWith("/seg/") -> {
                    val segParts = path.removePrefix("/seg/").split("/")
                    val vIdx = segParts.getOrNull(0)?.toIntOrNull() ?: 0
                    val seq = segParts.getOrNull(1)?.substringBefore(".")?.toLongOrNull() ?: -1L
                    serveSegment(vIdx, seq, out)
                }
                else -> writeHttpError(out, 404, "Not Found")
            }
        }
    }

    private fun serveMasterPlaylist(out: OutputStream) {
        val fetched = fetchText(currentUpstreamUrl)
        if (fetched == null) {
            writeRedirect(out, currentUpstreamUrl)
            return
        }
        val (finalUrl, body) = fetched
        if (!body.contains("#EXTM3U", ignoreCase = true)) {
            _bufferInfo.update { it.copy(isProxyActive = false, isDirectFallback = true) }
            writeRedirect(out, finalUrl)
            return
        }

        if (body.contains("#EXT-X-BYTERANGE", ignoreCase = true)) {
            _bufferInfo.update { it.copy(isProxyActive = false, isDirectFallback = true) }
            writeRedirect(out, finalUrl)
            return
        }

        val lines = body.lines()
        val isMultivariant = lines.any { it.startsWith("#EXT-X-STREAM-INF", ignoreCase = true) }

        if (isMultivariant) {
            variants.clear()
            var variantIdx = 0
            val outSb = StringBuilder()
            var pendingStreamInf = false

            for (raw in lines) {
                val line = raw.trim()
                if (line.isEmpty()) continue
                when {
                    line.startsWith("#EXT-X-STREAM-INF", ignoreCase = true) -> {
                        pendingStreamInf = true
                        outSb.appendLine(line)
                    }
                    line.startsWith("#EXT-X-MEDIA", ignoreCase = true) -> {
                        outSb.appendLine(rewriteUriAttribute(line, finalUrl))
                    }
                    line.startsWith("#") -> {
                        outSb.appendLine(line)
                    }
                    pendingStreamInf -> {
                        pendingStreamInf = false
                        val resolved = resolveUrl(finalUrl, line)
                        val idx = variantIdx++
                        variants[idx] = VariantState(index = idx, upstreamUrl = resolved)
                        outSb.appendLine("http://127.0.0.1:$port/variant/$idx/playlist.m3u8")
                    }
                    else -> outSb.appendLine(line)
                }
            }
            if (variants.isEmpty()) {
                writeRedirect(out, finalUrl)
                return
            }
            writeHttpText(out, "application/vnd.apple.mpegurl", outSb.toString())
        } else {
            variants.clear()
            val vState = VariantState(index = 0, upstreamUrl = finalUrl)
            variants[0] = vState
            ingestMediaPlaylist(vState, finalUrl, body)
            ensurePollerRunning(0)

            val syntheticMaster = buildString {
                appendLine("#EXTM3U")
                appendLine("#EXT-X-VERSION:3")
                appendLine("#EXT-X-STREAM-INF:BANDWIDTH=1200000,NAME=\"Live\"")
                appendLine("http://127.0.0.1:$port/variant/0/playlist.m3u8")
            }
            writeHttpText(out, "application/vnd.apple.mpegurl", syntheticMaster)
        }
    }

    private fun serveVariantPlaylist(variantIdx: Int, out: OutputStream) {
        val state = variants[variantIdx]
        if (state == null) {
            writeHttpError(out, 404, "Variant not found")
            return
        }

        if (activeVariantIdx != variantIdx) {
            activeVariantIdx = variantIdx
        }

        val isEmpty = synchronized(state) { state.segments.isEmpty() }
        if (isEmpty) {
            val fetched = fetchText(state.upstreamUrl)
            if (fetched != null) {
                if (fetched.second.contains("#EXT-X-BYTERANGE", ignoreCase = true)) {
                    state.hasByteRange = true
                    _bufferInfo.update { it.copy(isProxyActive = false, isDirectFallback = true) }
                    writeRedirect(out, fetched.first)
                    return
                }
                ingestMediaPlaylist(state, fetched.first, fetched.second)
            }
        }
        // Start background poller AFTER initial synchronous ingestion so they cannot race on startup
        ensurePollerRunning(variantIdx)

        if (state.hasByteRange) {
            writeRedirect(out, state.upstreamUrl)
            return
        }

        val playlistText = synchronized(state) {
            if (state.segments.isEmpty()) return@synchronized null
            val firstSeq = state.segments.first().sequence
            buildString {
                appendLine("#EXTM3U")
                appendLine("#EXT-X-VERSION:3")
                appendLine("#EXT-X-TARGETDURATION:${state.targetDurationSec.coerceAtLeast(2)}")
                appendLine("#EXT-X-MEDIA-SEQUENCE:$firstSeq")
                if (state.evictedDiscontinuities > 0) {
                    appendLine("#EXT-X-DISCONTINUITY-SEQUENCE:${state.evictedDiscontinuities}")
                }
                state.mapLine?.let { appendLine(it) }
                var lastKey: String? = null
                for (seg in state.segments) {
                    if (seg.discontinuityBefore) {
                        appendLine("#EXT-X-DISCONTINUITY")
                    }
                    if (seg.keyLine != null && seg.keyLine != lastKey) {
                        appendLine(seg.keyLine)
                        lastKey = seg.keyLine
                    }
                    seg.programDateTimeLine?.let { appendLine(it) }
                    appendLine("#EXTINF:${"%.3f".format(seg.durationSec)},")
                    appendLine("http://127.0.0.1:$port/seg/$variantIdx/${seg.sequence}.${seg.ext}")
                }
            }
        }

        if (playlistText == null) {
            writeRedirect(out, state.upstreamUrl)
        } else {
            writeHttpText(out, "application/vnd.apple.mpegurl", playlistText)
        }
    }

    private fun serveSegment(variantIdx: Int, sequence: Long, out: OutputStream) {
        val state = variants[variantIdx]
        val seg = state?.let {
            synchronized(it) { it.segments.firstOrNull { s -> s.sequence == sequence } }
        }
        if (seg == null || !seg.file.exists()) {
            writeHttpError(out, 404, "Segment evicted or missing")
            return
        }

        val bytes = runCatching { seg.file.readBytes() }.getOrNull()
        if (bytes == null) {
            writeHttpError(out, 500, "Read failed")
            return
        }
        val contentType = if (seg.ext == "m4s" || seg.ext == "mp4") "video/mp4" else "video/mp2t"
        val headers = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Cache-Control: no-cache\r\n")
            append("Connection: close\r\n\r\n")
        }
        out.write(headers.toByteArray(Charsets.US_ASCII))
        out.write(bytes)
        out.flush()
    }

    private fun ensurePollerRunning(variantIdx: Int) {
        val currentState = variants[variantIdx] ?: return
        currentState.lastActivePollMs = System.currentTimeMillis()
        if (pollerJob?.isActive == true && activeVariantIdx == variantIdx) return

        pollerJob?.cancel()
        activeVariantIdx = variantIdx
        pollerJob = scope.launch {
            while (isActive) {
                val intervalMs = (currentState.targetDurationSec * 500L).coerceIn(1500L, 4000L)
                delay(intervalMs)
                val state = variants[activeVariantIdx] ?: break
                val fetched = fetchText(state.upstreamUrl)
                if (fetched != null) {
                    ingestMediaPlaylist(state, fetched.first, fetched.second)
                }
            }
        }
    }

    private fun ingestMediaPlaylist(state: VariantState, baseUrl: String, body: String) {
        if (!body.contains("#EXTM3U", ignoreCase = true)) return
        if (body.contains("#EXT-X-BYTERANGE", ignoreCase = true)) {
            state.hasByteRange = true
            return
        }

        // Serialize check -> download -> append per variant so poller and HTTP handler never duplicate segments
        synchronized(state.ingestLock) {
            var parsedTargetDur = state.targetDurationSec
            var upstreamMediaSeq = -1L
            var upstreamDiscontinuitySeq = 0L
            var hasDiscontinuitySeqTag = false
            var currentKeyLine: String? = null
            var pendingDurSec: Double? = null
            var pendingProgramDateTime: String? = null
            var pendingDiscontinuity = false

            data class UpstreamSeg(
                val upstreamSeq: Long,
                val url: String,
                val durSec: Double,
                val keyLine: String?,
                val programDateTimeLine: String?,
                val discontinuity: Boolean,
            )

            val discovered = mutableListOf<UpstreamSeg>()
            var segIndexInPlaylist = 0L
            for (raw in body.lines()) {
                val line = raw.trim()
                if (line.isEmpty()) continue
                when {
                    line.startsWith("#EXT-X-TARGETDURATION:", ignoreCase = true) -> {
                        parsedTargetDur = line.substringAfter(":").trim().toDoubleOrNull()?.toInt()?.coerceAtLeast(1) ?: 6
                    }
                    line.startsWith("#EXT-X-MEDIA-SEQUENCE:", ignoreCase = true) -> {
                        upstreamMediaSeq = line.substringAfter(":").trim().toLongOrNull() ?: -1L
                    }
                    line.startsWith("#EXT-X-DISCONTINUITY-SEQUENCE:", ignoreCase = true) -> {
                        upstreamDiscontinuitySeq = line.substringAfter(":").trim().toLongOrNull() ?: 0L
                        hasDiscontinuitySeqTag = true
                    }
                    line.startsWith("#EXT-X-MAP:", ignoreCase = true) -> {
                        state.mapLine = rewriteUriAttribute(line, baseUrl)
                    }
                    line.startsWith("#EXT-X-KEY:", ignoreCase = true) -> {
                        currentKeyLine = rewriteUriAttribute(line, baseUrl)
                    }
                    line.startsWith("#EXT-X-PROGRAM-DATE-TIME:", ignoreCase = true) -> {
                        pendingProgramDateTime = line
                    }
                    line.startsWith("#EXT-X-DISCONTINUITY", ignoreCase = true) -> {
                        pendingDiscontinuity = true
                    }
                    line.startsWith("#EXTINF:", ignoreCase = true) -> {
                        val durStr = line.removePrefix("#EXTINF:").substringBefore(",").trim()
                        pendingDurSec = durStr.toDoubleOrNull() ?: parsedTargetDur.toDouble()
                    }
                    !line.startsWith("#") && pendingDurSec != null -> {
                        val absUrl = resolveUrl(baseUrl, line)
                        val computedSeq = if (upstreamMediaSeq >= 0L) upstreamMediaSeq + segIndexInPlaylist else -1L
                        discovered += UpstreamSeg(
                            upstreamSeq = computedSeq,
                            url = absUrl,
                            durSec = pendingDurSec ?: 6.0,
                            keyLine = currentKeyLine,
                            programDateTimeLine = pendingProgramDateTime,
                            discontinuity = pendingDiscontinuity,
                        )
                        segIndexInPlaylist++
                        pendingDurSec = null
                        pendingProgramDateTime = null
                        pendingDiscontinuity = false
                    }
                }
            }
            state.targetDurationSec = parsedTargetDur

            val newSegments = synchronized(state) {
                val lastSeq = state.lastUpstreamSeq
                discovered.filter { seg ->
                    val urlNotSeen = seg.url !in state.seenUpstreamUrls
                    val seqIsNewer = seg.upstreamSeq < 0L || lastSeq < 0L || seg.upstreamSeq > lastSeq
                    urlNotSeen && seqIsNewer
                }
            }
            if (newSegments.isEmpty()) return

            val variantDir = File(sessionRootDir ?: context.cacheDir, "v${state.index}").apply { mkdirs() }
            for (seg in newSegments) {
                val ext = seg.url.substringBefore("?").substringAfterLast(".", "ts")
                    .lowercase()
                    .takeIf { it in setOf("ts", "m4s", "mp4", "aac") } ?: "ts"
                val seq = synchronized(state) {
                    if (seg.upstreamSeq >= 0L && seg.upstreamSeq >= state.nextSequence) {
                        state.nextSequence = seg.upstreamSeq + 1L
                        seg.upstreamSeq
                    } else {
                        state.nextSequence++
                    }
                }
                val destFile = File(variantDir, "seg_${seq}.$ext")
                val downloadedBytes = downloadToFile(seg.url, destFile)
                if (downloadedBytes > 0L) {
                    totalProxyBytesDownloaded.addAndGet(downloadedBytes)
                    onNetworkBytesDownloaded?.invoke(downloadedBytes)
                    synchronized(state) {
                        val gapDetected = state.lastUpstreamSeq >= 0L &&
                            seg.upstreamSeq >= 0L &&
                            seg.upstreamSeq > state.lastUpstreamSeq + 1L
                        val discSeqJumped = hasDiscontinuitySeqTag &&
                            state.lastUpstreamDiscontinuitySeq >= 0L &&
                            upstreamDiscontinuitySeq > state.lastUpstreamDiscontinuitySeq
                        val needsDiscontinuity = seg.discontinuity || gapDetected || discSeqJumped

                        if (seg.upstreamSeq >= 0L) {
                            state.lastUpstreamSeq = seg.upstreamSeq
                        }
                        if (hasDiscontinuitySeqTag) {
                            state.lastUpstreamDiscontinuitySeq = upstreamDiscontinuitySeq
                        }

                        state.seenUpstreamUrls.add(seg.url)
                        state.segments.addLast(
                            CachedSegment(
                                sequence = seq,
                                upstreamUrl = seg.url,
                                durationSec = seg.durSec,
                                file = destFile,
                                byteSize = downloadedBytes,
                                ext = ext,
                                keyLine = seg.keyLine,
                                programDateTimeLine = seg.programDateTimeLine,
                                discontinuityBefore = needsDiscontinuity,
                            )
                        )
                        evictIfNeededLocked(state)
                    }
                } else {
                    destFile.delete()
                }
            }
            publishBufferMetrics(state)
        }
    }

    private fun evictIfNeededLocked(state: VariantState) {
        var totalDurMs = state.segments.sumOf { (it.durationSec * 1000.0).toLong() }
        var totalBytes = state.segments.sumOf { it.byteSize }
        while (state.segments.size > 3 &&
            (totalDurMs > maxBufferDurationMs || totalBytes > MAX_CACHE_BYTES)
        ) {
            val oldest = state.segments.removeFirst()
            if (oldest.discontinuityBefore) {
                state.evictedDiscontinuities++
            }
            totalDurMs -= (oldest.durationSec * 1000.0).toLong()
            totalBytes -= oldest.byteSize
            state.seenUpstreamUrls.remove(oldest.upstreamUrl)
            oldest.file.delete()
        }
    }

    private fun publishBufferMetrics(state: VariantState) {
        val (durMs, bytes, count) = synchronized(state) {
            val d = state.segments.sumOf { (it.durationSec * 1000.0).toLong() }
            val b = state.segments.sumOf { it.byteSize }
            Triple(d, b, state.segments.size)
        }
        _bufferInfo.update {
            it.copy(
                bufferedDurationMs = durMs.coerceAtMost(maxBufferDurationMs),
                maxBufferCapacityMs = maxBufferDurationMs,
                diskUsedBytes = bytes,
                segmentsCached = count,
                isProxyActive = true,
                isDirectFallback = false,
            )
        }
    }

    private fun fetchText(url: String): Pair<String, String>? {
        return runCatching {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val finalUrl = resp.request.url.toString()
                val text = resp.body?.string() ?: return@use null
                finalUrl to text
            }
        }.onFailure {
            Log.d(TAG, "fetchText failed for $url: ${it.message}")
        }.getOrNull()
    }

    private fun downloadToFile(url: String, dest: File): Long {
        return runCatching {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use 0L
                val body = resp.body ?: return@use 0L
                val bytes = body.bytes()
                dest.writeBytes(bytes)
                bytes.size.toLong()
            }
        }.getOrDefault(0L)
    }

    private fun resolveUrl(baseUrl: String, relativeOrAbsolute: String): String {
        if (relativeOrAbsolute.startsWith("http://", ignoreCase = true) ||
            relativeOrAbsolute.startsWith("https://", ignoreCase = true)
        ) {
            return relativeOrAbsolute
        }
        return runCatching {
            URI(baseUrl).resolve(relativeOrAbsolute).toString()
        }.getOrDefault(relativeOrAbsolute)
    }

    private fun rewriteUriAttribute(line: String, baseUrl: String): String {
        val uriRegex = Regex("""URI="([^"]+)"""")
        return uriRegex.replace(line) { match ->
            val rawUri = match.groupValues[1]
            val abs = resolveUrl(baseUrl, rawUri)
            "URI=\"$abs\""
        }
    }

    private fun writeHttpText(out: OutputStream, contentType: String, content: String) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val header = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Cache-Control: no-cache\r\n")
            append("Connection: close\r\n\r\n")
        }
        out.write(header.toByteArray(Charsets.US_ASCII))
        out.write(bytes)
        out.flush()
    }

    private fun writeRedirect(out: OutputStream, location: String) {
        val header = buildString {
            append("HTTP/1.1 302 Found\r\n")
            append("Location: $location\r\n")
            append("Content-Length: 0\r\n")
            append("Connection: close\r\n\r\n")
        }
        out.write(header.toByteArray(Charsets.US_ASCII))
        out.flush()
    }

    private fun writeHttpError(out: OutputStream, code: Int, message: String) {
        val bytes = message.toByteArray(Charsets.UTF_8)
        val header = buildString {
            append("HTTP/1.1 $code $message\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Connection: close\r\n\r\n")
        }
        out.write(header.toByteArray(Charsets.US_ASCII))
        out.write(bytes)
        out.flush()
    }

    private fun clearAllCacheDirsExcept(keep: File?) {
        val base = File(context.cacheDir, "timeshift")
        if (!base.exists()) return
        base.listFiles()?.forEach { child ->
            if (keep == null || child != keep) {
                child.deleteRecursively()
            }
        }
    }
}
