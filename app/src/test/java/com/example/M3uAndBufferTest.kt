package com.example

import com.example.data.M3uPlaylistParser
import com.example.data.SriLankaChannelsCatalog
import com.example.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uAndBufferTest {

    @Test
    fun parseM3uExtractsSriLankanChannels() {
        val sampleM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="Rupavahini.lk" tvg-logo="https://example.com/rupa.png" group-title="General",Rupavahini (720p)
            https://live.peotv.com/bpk-tv/003/output/index.m3u8
            #EXTINF:-1 tvg-id="AdaDerana24.lk" group-title="News",Ada Derana 24
            https://live.peotv.com/bpk-tv/011/output/index.m3u8
        """.trimIndent()

        val parsed = M3uPlaylistParser.parse(sampleM3u)
        assertEquals(2, parsed.size)
        assertEquals("Rupavahini", parsed[0].name)
        assertEquals("Rupavahini.lk", parsed[0].tvgId)
        assertEquals("https://example.com/rupa.png", parsed[0].logoUrl)
        assertEquals("https://live.peotv.com/bpk-tv/011/output/index.m3u8", parsed[1].streamUrl)
    }

    @Test
    fun formatDurationsAndOffsetsForOneHourBuffer() {
        assertEquals("LIVE", FormatUtils.formatOffsetBehindLive(4_000L))
        assertEquals("-15:00", FormatUtils.formatOffsetBehindLive(15 * 60_000L))
        assertEquals("1:00:00", FormatUtils.formatDurationMmSs(3_600_000L))
        assertEquals("240.0 MB", FormatUtils.formatBytes(240L * 1024L * 1024L))
    }

    @Test
    fun catalogChannelsCoverAll24Hours() {
        SriLankaChannelsCatalog.defaultChannels.forEach { channel ->
            for (hour in 0..23) {
                assertTrue(
                    "Channel ${channel.name} missing schedule at hour $hour",
                    channel.currentProgramAtHour(hour) != null,
                )
            }
        }
    }
}
