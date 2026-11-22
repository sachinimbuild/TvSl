package com.example.data

import com.example.model.ChannelCategory
import com.example.model.ChannelLanguage
import com.example.model.TvChannel

object M3uPlaylistParser {

    data class ParsedM3uEntry(
        val name: String,
        val streamUrl: String,
        val tvgId: String?,
        val logoUrl: String?,
        val groupTitle: String?,
    )

    private val attrRegex = Regex("""([a-zA-Z0-9_-]+)="([^"]*)"""")

    fun parse(content: String): List<ParsedM3uEntry> {
        val entries = mutableListOf<ParsedM3uEntry>()
        val lines = content.lines()
        var pendingExtInf: String? = null

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                pendingExtInf = line
            } else if (!line.startsWith("#") &&
                (line.startsWith("http://", ignoreCase = true) || line.startsWith("https://", ignoreCase = true))
            ) {
                val info = pendingExtInf
                pendingExtInf = null

                val attrs = mutableMapOf<String, String>()
                if (info != null) {
                    attrRegex.findAll(info).forEach { match ->
                        attrs[match.groupValues[1].lowercase()] = match.groupValues[2].trim()
                    }
                }
                val commaIdx = info?.lastIndexOf(',') ?: -1
                val displayName = if (info != null && commaIdx >= 0 && commaIdx + 1 < info.length) {
                    info.substring(commaIdx + 1).trim()
                } else {
                    attrs["tvg-name"]?.takeIf { it.isNotBlank() } ?: "Sri Lanka TV"
                }

                entries += ParsedM3uEntry(
                    name = displayName.replace(Regex("""\s*\(\d+p\)|\s*\[.*?]"""), "").trim()
                        .ifBlank { displayName },
                    streamUrl = line,
                    tvgId = attrs["tvg-id"]?.takeIf { it.isNotBlank() },
                    logoUrl = attrs["tvg-logo"]?.takeIf { it.isNotBlank() },
                    groupTitle = attrs["group-title"]?.takeIf { it.isNotBlank() },
                )
            }
        }
        return entries
    }

    fun toCustomChannel(entry: ParsedM3uEntry, number: Int): TvChannel {
        val id = "m3u_" + entry.name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        val category = inferCategory(entry.name, entry.groupTitle)
        val language = inferLanguage(entry.name)
        return TvChannel(
            id = id,
            number = number,
            name = entry.name,
            localName = entry.name,
            category = category,
            language = language,
            streamUrl = entry.streamUrl,
            logoUrl = entry.logoUrl,
            accentHex = pickAccentHex(category),
            description = "Live Sri Lankan broadcast stream (${entry.groupTitle ?: category.label}).",
            isCustom = false,
        )
    }

    private fun inferCategory(name: String, group: String?): ChannelCategory {
        val combined = "$name ${group.orEmpty()}".lowercase()
        return when {
            "news" in combined || "24" in combined || "ada derana" in combined -> ChannelCategory.NEWS
            "sport" in combined || "cricket" in combined || "eye" in combined -> ChannelCategory.SPORTS
            "buddh" in combined || "shraddha" in combined || "relig" in combined ||
                "damsath" in combined || "verbum" in combined -> ChannelCategory.RELIGIOUS
            "kids" in combined || "edu" in combined || "nenasa" in combined -> ChannelCategory.KIDS_EDU
            else -> ChannelCategory.ENTERTAINMENT
        }
    }

    private fun inferLanguage(name: String): ChannelLanguage {
        val lower = name.lowercase()
        return when {
            "shakthi" in lower || "vasantham" in lower || "nethra" in lower || "tamil" in lower || "dan" in lower -> ChannelLanguage.TAMIL
            "art" in lower || "tnl" in lower || "english" in lower || "peo" in lower -> ChannelLanguage.ENGLISH
            else -> ChannelLanguage.SINHALA
        }
    }

    private fun pickAccentHex(category: ChannelCategory): Long = when (category) {
        ChannelCategory.NEWS -> 0xFFEF4444
        ChannelCategory.SPORTS -> 0xFF10B981
        ChannelCategory.RELIGIOUS -> 0xFFF59E0B
        ChannelCategory.KIDS_EDU -> 0xFF38BDF8
        else -> 0xFFFF6B2C
    }
}
