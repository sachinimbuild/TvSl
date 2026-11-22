package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object FormatUtils {

    private val colomboTz: TimeZone = TimeZone.getTimeZone("Asia/Colombo")

    fun formatDurationMmSs(ms: Long): String {
        val totalSec = (ms / 1000L).coerceAtLeast(0L)
        val hours = totalSec / 3600L
        val mins = (totalSec % 3600L) / 60L
        val secs = totalSec % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    fun formatOffsetBehindLive(offsetMs: Long): String {
        if (offsetMs <= 12_000L) return "LIVE"
        return "-${formatDurationMmSs(offsetMs)}"
    }

    fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    fun getColomboClockText(nowMs: Long = System.currentTimeMillis()): String {
        val fmt = SimpleDateFormat("hh:mm a", Locale.US).apply { timeZone = colomboTz }
        return "${fmt.format(Date(nowMs))} SLST"
    }

    fun getColomboHour24(nowMs: Long = System.currentTimeMillis()): Int {
        val fmt = SimpleDateFormat("HH", Locale.US).apply { timeZone = colomboTz }
        return fmt.format(Date(nowMs)).toIntOrNull() ?: 12
    }

    fun formatHourRange(start24: Int, end24: Int): String {
        return "${formatHour12(start24)} – ${formatHour12(end24)}"
    }

    private fun formatHour12(hour24: Int): String {
        val normalized = ((hour24 % 24) + 24) % 24
        val suffix = if (normalized < 12) "AM" else "PM"
        val h12 = when (val mod = normalized % 12) {
            0 -> 12
            else -> mod
        }
        return "$h12:00 $suffix"
    }
}
