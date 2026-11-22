package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ChannelCategory
import com.example.model.ChannelLanguage
import com.example.model.DataSaverPreset
import com.example.model.TvChannel
import com.example.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lankatv_prefs")

class PreferencesRepository(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.dataStore)

    private object Keys {
        val FAVORITE_IDS = stringSetPreferencesKey("favorite_channel_ids")
        val DATA_SAVER_PRESET = stringPreferencesKey("data_saver_preset")
        val ALLOW_MOBILE_DATA = booleanPreferencesKey("allow_mobile_data")
        val WARN_HD_MOBILE = booleanPreferencesKey("warn_hd_mobile")
        val BUFFER_MINUTES = intPreferencesKey("reverse_buffer_minutes")
        val LAST_CHANNEL_ID = stringPreferencesKey("last_channel_id")
        val CUSTOM_CHANNELS_JSON = stringPreferencesKey("custom_channels_json")
    }

    val userPreferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        val presetName = prefs[Keys.DATA_SAVER_PRESET] ?: DataSaverPreset.DATA_SAVER.name
        val preset = runCatching { DataSaverPreset.valueOf(presetName) }
            .getOrDefault(DataSaverPreset.DATA_SAVER)

        UserPreferences(
            favoriteChannelIds = prefs[Keys.FAVORITE_IDS] ?: setOf("rupavahini", "tv_derana", "ada_derana_24"),
            dataSaverPreset = preset,
            allowMobileDataStreaming = prefs[Keys.ALLOW_MOBILE_DATA] ?: true,
            warnHdOnMobileData = prefs[Keys.WARN_HD_MOBILE] ?: true,
            reverseBufferMinutes = (prefs[Keys.BUFFER_MINUTES] ?: 60).coerceIn(15, 60),
            lastChannelId = prefs[Keys.LAST_CHANNEL_ID] ?: "rupavahini",
            customChannels = decodeCustomChannels(prefs[Keys.CUSTOM_CHANNELS_JSON]),
        )
    }

    suspend fun toggleFavorite(channelId: String) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_IDS]?.toMutableSet()
                ?: mutableSetOf("rupavahini", "tv_derana", "ada_derana_24")
            if (channelId in current) current.remove(channelId) else current.add(channelId)
            prefs[Keys.FAVORITE_IDS] = current
        }
    }

    suspend fun setFavoriteChannels(ids: Set<String>) {
        dataStore.edit { prefs ->
            prefs[Keys.FAVORITE_IDS] = ids
        }
    }

    suspend fun clearFavorites() {
        dataStore.edit { prefs ->
            prefs[Keys.FAVORITE_IDS] = emptySet()
        }
    }

    suspend fun setDataSaverPreset(preset: DataSaverPreset) {
        dataStore.edit { prefs ->
            prefs[Keys.DATA_SAVER_PRESET] = preset.name
        }
    }

    suspend fun setAllowMobileDataStreaming(allowed: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ALLOW_MOBILE_DATA] = allowed
        }
    }

    suspend fun setWarnHdOnMobileData(warn: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.WARN_HD_MOBILE] = warn
        }
    }

    suspend fun setReverseBufferMinutes(minutes: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.BUFFER_MINUTES] = minutes.coerceIn(15, 60)
        }
    }

    suspend fun setLastChannelId(channelId: String) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_CHANNEL_ID] = channelId
        }
    }

    suspend fun addCustomChannel(name: String, streamUrl: String, category: ChannelCategory, language: ChannelLanguage) {
        dataStore.edit { prefs ->
            val existing = decodeCustomChannels(prefs[Keys.CUSTOM_CHANNELS_JSON]).toMutableList()
            val newChannel = TvChannel(
                id = "custom_${System.currentTimeMillis()}",
                number = 90 + existing.size + 1,
                name = name.trim(),
                localName = name.trim(),
                category = category,
                language = language,
                streamUrl = streamUrl.trim(),
                accentHex = 0xFFFF6B2C,
                description = "User-added custom live IPTV stream.",
                isCustom = true,
            )
            existing.add(0, newChannel)
            prefs[Keys.CUSTOM_CHANNELS_JSON] = encodeCustomChannels(existing)
        }
    }

    suspend fun removeCustomChannel(channelId: String) {
        dataStore.edit { prefs ->
            val filtered = decodeCustomChannels(prefs[Keys.CUSTOM_CHANNELS_JSON])
                .filterNot { it.id == channelId }
            prefs[Keys.CUSTOM_CHANNELS_JSON] = encodeCustomChannels(filtered)
        }
    }

    private fun encodeCustomChannels(channels: List<TvChannel>): String {
        val arr = JSONArray()
        channels.forEach { ch ->
            arr.put(
                JSONObject().apply {
                    put("id", ch.id)
                    put("number", ch.number)
                    put("name", ch.name)
                    put("streamUrl", ch.streamUrl)
                    put("category", ch.category.name)
                    put("language", ch.language.name)
                }
            )
        }
        return arr.toString()
    }

    private fun decodeCustomChannels(json: String?): List<TvChannel> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val cat = runCatching { ChannelCategory.valueOf(obj.optString("category")) }
                        .getOrDefault(ChannelCategory.CUSTOM)
                    val lang = runCatching { ChannelLanguage.valueOf(obj.optString("language")) }
                        .getOrDefault(ChannelLanguage.SINHALA)
                    add(
                        TvChannel(
                            id = obj.getString("id"),
                            number = obj.optInt("number", 90 + i),
                            name = obj.getString("name"),
                            localName = obj.getString("name"),
                            category = cat,
                            language = lang,
                            streamUrl = obj.getString("streamUrl"),
                            accentHex = 0xFFFF6B2C,
                            description = "User-added custom live IPTV stream.",
                            isCustom = true,
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
