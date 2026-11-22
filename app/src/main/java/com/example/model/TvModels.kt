package com.example.model

enum class ChannelCategory(val label: String) {
    ALL("All"),
    NEWS("News"),
    ENTERTAINMENT("Entertainment"),
    SPORTS("Sports"),
    RELIGIOUS("Religious"),
    KIDS_EDU("Kids & Edu"),
    CUSTOM("Custom"),
}

enum class ChannelLanguage(val label: String) {
    ALL("All Languages"),
    SINHALA("සිංහල"),
    TAMIL("தமிழ்"),
    ENGLISH("English"),
    MULTILINGUAL("Multi"),
}

enum class DataSaverPreset(
    val title: String,
    val shortLabel: String,
    val maxBitrateBps: Int,
    val maxHeightPx: Int,
    val estUsagePerHour: String,
    val description: String,
) {
    DATA_SAVER(
        title = "Mobile Data Saver",
        shortLabel = "360p Saver",
        maxBitrateBps = 550_000,
        maxHeightPx = 360,
        estUsagePerHour = "~240 MB/hr",
        description = "Optimized for 3G/4G mobile data caps with crisp SD video.",
    ),
    BALANCED(
        title = "Mobile Balanced",
        shortLabel = "480p",
        maxBitrateBps = 1_100_000,
        maxHeightPx = 480,
        estUsagePerHour = "~480 MB/hr",
        description = "Balanced clarity and bandwidth for everyday LTE streaming.",
    ),
    HIGH(
        title = "HD Quality",
        shortLabel = "720p HD",
        maxBitrateBps = 3_000_000,
        maxHeightPx = 720,
        estUsagePerHour = "~1.2 GB/hr",
        description = "High definition playback for 5G or unlimited data plans.",
    ),
    AUTO(
        title = "Adaptive Auto",
        shortLabel = "Auto",
        maxBitrateBps = Int.MAX_VALUE,
        maxHeightPx = Int.MAX_VALUE,
        estUsagePerHour = "Dynamic",
        description = "Automatically switches quality based on live network speed.",
    ),
}

enum class NetworkType(val label: String) {
    MOBILE_DATA("Mobile Data"),
    WIFI("Wi-Fi"),
    OFFLINE("Offline"),
    OTHER("Network"),
}

data class ProgramSlot(
    val startHour24: Int,
    val endHour24: Int,
    val title: String,
    val genre: String,
)

data class TvChannel(
    val id: String,
    val number: Int,
    val name: String,
    val localName: String,
    val category: ChannelCategory,
    val language: ChannelLanguage,
    val streamUrl: String,
    val backupStreamUrl: String? = null,
    val logoUrl: String? = null,
    val accentHex: Long,
    val description: String,
    val schedule: List<ProgramSlot> = emptyList(),
    val isCustom: Boolean = false,
) {
    fun currentProgramAtHour(hour24: Int): ProgramSlot? {
        return schedule.firstOrNull { slot ->
            if (slot.startHour24 <= slot.endHour24) {
                hour24 in slot.startHour24 until slot.endHour24
            } else {
                hour24 >= slot.startHour24 || hour24 < slot.endHour24
            }
        }
    }

    fun nextProgramAtHour(hour24: Int): ProgramSlot? {
        val current = currentProgramAtHour(hour24) ?: return schedule.firstOrNull()
        val idx = schedule.indexOf(current)
        return if (idx >= 0 && schedule.isNotEmpty()) {
            schedule[(idx + 1) % schedule.size]
        } else null
    }
}

data class BufferSegmentInfo(
    val bufferedDurationMs: Long = 0L,
    val currentOffsetFromLiveMs: Long = 0L,
    val maxBufferCapacityMs: Long = 3_600_000L, // 1 hour default
    val diskUsedBytes: Long = 0L,
    val segmentsCached: Int = 0,
    val isProxyActive: Boolean = false,
    val isDirectFallback: Boolean = false,
) {
    val isAtLiveEdge: Boolean
        get() = currentOffsetFromLiveMs <= 12_000L

    val bufferFillFraction: Float
        get() = if (maxBufferCapacityMs <= 0L) 0f
        else (bufferedDurationMs.toFloat() / maxBufferCapacityMs.toFloat()).coerceIn(0f, 1f)

    val playheadFractionOfBuffer: Float
        get() = if (bufferedDurationMs <= 0L) 1f
        else ((bufferedDurationMs - currentOffsetFromLiveMs).toFloat() / bufferedDurationMs.toFloat()).coerceIn(0f, 1f)
}

data class NetworkTelemetry(
    val networkType: NetworkType = NetworkType.OTHER,
    val isMetered: Boolean = false,
    val sessionBytesUsed: Long = 0L,
    val currentBitrateKbps: Int = 0,
    val videoResolution: String = "--",
    val downstreamBandwidthKbps: Int = 0,
)

sealed interface PlayerPlaybackState {
    data object Idle : PlayerPlaybackState
    data object Buffering : PlayerPlaybackState
    data object Playing : PlayerPlaybackState
    data object Paused : PlayerPlaybackState
    data class Error(val message: String, val hasBackup: Boolean) : PlayerPlaybackState
}

data class UserPreferences(
    val favoriteChannelIds: Set<String> = emptySet(),
    val dataSaverPreset: DataSaverPreset = DataSaverPreset.DATA_SAVER,
    val allowMobileDataStreaming: Boolean = true,
    val warnHdOnMobileData: Boolean = true,
    val reverseBufferMinutes: Int = 60, // 1 hour reverse buffering
    val lastChannelId: String = "rupavahini",
    val customChannels: List<TvChannel> = emptyList(),
)
