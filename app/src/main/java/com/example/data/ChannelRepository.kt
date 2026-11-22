package com.example.data

import com.example.model.TvChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ChannelRepository(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
) {

    data class SyncResult(
        val channels: List<TvChannel>,
        val syncedFromDirectory: Boolean,
        val liveCount: Int,
    )

    suspend fun fetchMergedSriLankaChannels(customChannels: List<TvChannel>): SyncResult =
        withContext(Dispatchers.IO) {
            val baseCatalog = SriLankaChannelsCatalog.defaultChannels
            val syncedEntries = runCatching {
                val request = Request.Builder()
                    .url(SriLankaChannelsCatalog.IPTV_ORG_LK_M3U_URL)
                    .header("User-Agent", "LankaTV-Android/1.0")
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use emptyList()
                    val body = response.body?.string().orEmpty()
                    M3uPlaylistParser.parse(body)
                }
            }.getOrDefault(emptyList())

            if (syncedEntries.isEmpty()) {
                return@withContext SyncResult(
                    channels = customChannels + baseCatalog,
                    syncedFromDirectory = false,
                    liveCount = 0,
                )
            }

            // Match known channels by fuzzy name or tvg-id so they keep rich Sinhala/Tamil metadata & EPG,
            // while upgrading their stream URL + logoUrl from the live iptv-org lk.m3u directory.
            val matchedEntryUrls = mutableSetOf<String>()
            val mergedCatalog = baseCatalog.map { base ->
                val match = syncedEntries.firstOrNull { entry ->
                    val eName = entry.name.lowercase()
                    val bName = base.name.lowercase()
                    eName == bName ||
                        eName.contains(bName.removePrefix("tv ").removePrefix("the ")) ||
                        bName.contains(eName) ||
                        (entry.tvgId?.contains(base.id.replace("_", ""), ignoreCase = true) == true)
                }
                if (match != null) {
                    matchedEntryUrls += match.streamUrl
                    base.copy(
                        streamUrl = match.streamUrl,
                        backupStreamUrl = base.streamUrl.takeIf { it != match.streamUrl } ?: base.backupStreamUrl,
                        logoUrl = match.logoUrl ?: base.logoUrl,
                    )
                } else {
                    base
                }
            }

            var nextNumber = mergedCatalog.maxOfOrNull { it.number }?.plus(1) ?: 16
            val extraDirectoryChannels = syncedEntries
                .filterNot { it.streamUrl in matchedEntryUrls }
                .distinctBy { it.name.lowercase() }
                .map { entry ->
                    M3uPlaylistParser.toCustomChannel(entry, nextNumber++)
                }

            SyncResult(
                channels = customChannels + mergedCatalog + extraDirectoryChannels,
                syncedFromDirectory = true,
                liveCount = syncedEntries.size,
            )
        }
}
