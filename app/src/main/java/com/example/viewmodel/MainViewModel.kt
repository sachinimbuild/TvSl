package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChannelRepository
import com.example.data.PreferencesRepository
import com.example.data.SriLankaChannelsCatalog
import com.example.model.BufferSegmentInfo
import com.example.model.ChannelCategory
import com.example.model.ChannelLanguage
import com.example.model.DataSaverPreset
import com.example.model.NetworkTelemetry
import com.example.model.NetworkType
import com.example.model.PlayerPlaybackState
import com.example.model.TvChannel
import com.example.model.UserPreferences
import com.example.player.LiveTvPlayerManager
import com.example.util.FormatUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MainUiState(
    val allChannels: List<TvChannel> = SriLankaChannelsCatalog.defaultChannels,
    val filteredChannels: List<TvChannel> = SriLankaChannelsCatalog.defaultChannels,
    val favoriteChannels: List<TvChannel> = SriLankaChannelsCatalog.defaultChannels.filter {
        it.id in setOf("rupavahini", "tv_derana", "ada_derana_24")
    },
    val activeChannel: TvChannel? = SriLankaChannelsCatalog.defaultChannels.firstOrNull(),
    val selectedCategory: ChannelCategory = ChannelCategory.ALL,
    val selectedLanguage: ChannelLanguage = ChannelLanguage.ALL,
    val onlyFavorites: Boolean = false,
    val searchQuery: String = "",
    val preferences: UserPreferences = UserPreferences(),
    val playbackState: PlayerPlaybackState = PlayerPlaybackState.Idle,
    val bufferInfo: BufferSegmentInfo = BufferSegmentInfo(),
    val networkTelemetry: NetworkTelemetry = NetworkTelemetry(),
    val isPlaying: Boolean = false,
    val isSyncingDirectory: Boolean = false,
    val directorySyncLabel: String = "Built-in SL Lineup",
    val colomboClockText: String = FormatUtils.getColomboClockText(),
    val colomboHour24: Int = FormatUtils.getColomboHour24(),
    val pendingHdMobileWarningPreset: DataSaverPreset? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = PreferencesRepository(application)
    private val channelRepo = ChannelRepository()
    val playerManager = LiveTvPlayerManager(application)

    private val _baseChannels = MutableStateFlow(SriLankaChannelsCatalog.defaultChannels)
    private val _activeChannelId = MutableStateFlow("rupavahini")
    private val _selectedCategory = MutableStateFlow(ChannelCategory.ALL)
    private val _selectedLanguage = MutableStateFlow(ChannelLanguage.ALL)
    private val _onlyFavorites = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _isSyncing = MutableStateFlow(false)
    private val _syncLabel = MutableStateFlow("Sri Lanka Broadcast Lineup")
    private val _colomboClock = MutableStateFlow(
        FormatUtils.getColomboClockText() to FormatUtils.getColomboHour24()
    )
    private val _pendingHdWarning = MutableStateFlow<DataSaverPreset?>(null)

    private data class FilterParams(
        val category: ChannelCategory,
        val language: ChannelLanguage,
        val onlyFavs: Boolean,
        val query: String,
    )

    private val filterFlow = combine(
        _selectedCategory,
        _selectedLanguage,
        _onlyFavorites,
        _searchQuery,
    ) { cat, lang, favs, query ->
        FilterParams(cat, lang, favs, query)
    }

    private data class CatalogBundle(
        val all: List<TvChannel>,
        val filtered: List<TvChannel>,
        val favorites: List<TvChannel>,
        val active: TvChannel?,
    )

    private val catalogFlow = combine(
        _baseChannels,
        prefsRepo.userPreferences,
        _activeChannelId,
        filterFlow,
    ) { base, prefs, activeId, filter ->
        val merged = (prefs.customChannels + base).distinctBy { it.id }
        val favorites = merged.filter { it.id in prefs.favoriteChannelIds }
        val filtered = merged.filter { ch ->
            val catOk = filter.category == ChannelCategory.ALL ||
                (filter.category == ChannelCategory.CUSTOM && ch.isCustom) ||
                ch.category == filter.category
            val langOk = filter.language == ChannelLanguage.ALL || ch.language == filter.language
            val favOk = !filter.onlyFavs || ch.id in prefs.favoriteChannelIds
            val queryOk = filter.query.isBlank() ||
                ch.name.contains(filter.query, ignoreCase = true) ||
                ch.localName.contains(filter.query, ignoreCase = true) ||
                ch.category.label.contains(filter.query, ignoreCase = true)
            catOk && langOk && favOk && queryOk
        }
        val active = merged.firstOrNull { it.id == activeId } ?: merged.firstOrNull()
        CatalogBundle(merged, filtered, favorites, active) to prefs
    }

    private val playerFlow = combine(
        playerManager.playbackState,
        playerManager.bufferInfo,
        playerManager.networkTelemetry,
        playerManager.isPlaying,
    ) { playback, buffer, net, playing ->
        Quad(playback, buffer, net, playing)
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    val uiState: StateFlow<MainUiState> = combine(
        catalogFlow,
        filterFlow,
        playerFlow,
        combine(_isSyncing, _syncLabel, _colomboClock, _pendingHdWarning) { sync, lbl, clk, warn ->
            Quad(sync, lbl, clk, warn)
        },
    ) { (bundle, prefs), filter, pFlow, meta ->
        MainUiState(
            allChannels = bundle.all,
            filteredChannels = bundle.filtered,
            favoriteChannels = bundle.favorites,
            activeChannel = bundle.active,
            selectedCategory = filter.category,
            selectedLanguage = filter.language,
            onlyFavorites = filter.onlyFavs,
            searchQuery = filter.query,
            preferences = prefs,
            playbackState = pFlow.a,
            bufferInfo = pFlow.b,
            networkTelemetry = pFlow.c,
            isPlaying = pFlow.d,
            isSyncingDirectory = meta.a,
            directorySyncLabel = meta.b,
            colomboClockText = meta.c.first,
            colomboHour24 = meta.c.second,
            pendingHdMobileWarningPreset = meta.d,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    init {
        viewModelScope.launch {
            val prefs = prefsRepo.userPreferences.first()
            _activeChannelId.value = prefs.lastChannelId
            refreshChannelDirectory(autoStartLast = true)
        }
        viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                _colomboClock.value = FormatUtils.getColomboClockText() to FormatUtils.getColomboHour24()
            }
        }
    }

    fun refreshChannelDirectory(autoStartLast: Boolean = false) {
        viewModelScope.launch {
            _isSyncing.value = true
            val prefs = prefsRepo.userPreferences.first()
            val result = channelRepo.fetchMergedSriLankaChannels(emptyList())
            _baseChannels.value = result.channels
            _syncLabel.value = if (result.syncedFromDirectory) {
                "Synced ${result.liveCount} live LK feeds"
            } else {
                "Curated Sri Lanka Lineup"
            }
            _isSyncing.value = false

            if (autoStartLast) {
                val all = (prefs.customChannels + result.channels).distinctBy { it.id }
                val target = all.firstOrNull { it.id == prefs.lastChannelId } ?: all.firstOrNull()
                if (target != null) {
                    selectChannel(target)
                }
            }
        }
    }

    fun selectChannel(channel: TvChannel) {
        _activeChannelId.value = channel.id
        viewModelScope.launch {
            prefsRepo.setLastChannelId(channel.id)
            val prefs = prefsRepo.userPreferences.first()
            playerManager.playChannel(
                channel = channel,
                preset = prefs.dataSaverPreset,
                allowMobile = prefs.allowMobileDataStreaming,
                reverseBufferMin = prefs.reverseBufferMinutes,
            )
        }
    }

    fun selectCategory(category: ChannelCategory) {
        _selectedCategory.value = category
    }

    fun selectLanguage(language: ChannelLanguage) {
        _selectedLanguage.value = language
    }

    fun toggleFavoritesFilter() {
        _onlyFavorites.update { !it }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleChannelFavorite(channelId: String) {
        viewModelScope.launch {
            prefsRepo.toggleFavorite(channelId)
        }
    }

    fun requestDataSaverPreset(preset: DataSaverPreset) {
        val currentNet = playerManager.networkTelemetry.value.networkType
        val prefs = uiState.value.preferences
        if (currentNet == NetworkType.MOBILE_DATA &&
            prefs.warnHdOnMobileData &&
            (preset == DataSaverPreset.HIGH || preset == DataSaverPreset.AUTO)
        ) {
            _pendingHdWarning.value = preset
        } else {
            applyDataSaverPresetConfirmed(preset)
        }
    }

    fun confirmHdMobileWarning() {
        val pending = _pendingHdWarning.value ?: return
        _pendingHdWarning.value = null
        applyDataSaverPresetConfirmed(pending)
    }

    fun dismissHdMobileWarning() {
        _pendingHdWarning.value = null
    }

    private fun applyDataSaverPresetConfirmed(preset: DataSaverPreset) {
        playerManager.applyDataSaverPreset(preset)
        viewModelScope.launch {
            prefsRepo.setDataSaverPreset(preset)
        }
    }

    fun setAllowMobileDataStreaming(allowed: Boolean) {
        playerManager.updateMobileDataPolicy(allowed)
        viewModelScope.launch {
            prefsRepo.setAllowMobileDataStreaming(allowed)
        }
    }

    fun setWarnHdOnMobileData(warn: Boolean) {
        viewModelScope.launch {
            prefsRepo.setWarnHdOnMobileData(warn)
        }
    }

    fun setReverseBufferMinutes(minutes: Int) {
        playerManager.updateReverseBufferMinutes(minutes)
        viewModelScope.launch {
            prefsRepo.setReverseBufferMinutes(minutes)
        }
    }

    fun addCustomChannel(
        name: String,
        streamUrl: String,
        category: ChannelCategory,
        language: ChannelLanguage,
    ) {
        viewModelScope.launch {
            prefsRepo.addCustomChannel(name, streamUrl, category, language)
            val updated = prefsRepo.userPreferences.first().customChannels.firstOrNull()
            if (updated != null) {
                selectChannel(updated)
            }
        }
    }

    fun removeCustomChannel(channelId: String) {
        viewModelScope.launch {
            prefsRepo.removeCustomChannel(channelId)
        }
    }

    fun togglePlayPause() = playerManager.togglePlayPause()
    fun seekBackBy(ms: Long) = playerManager.seekBackBy(ms)
    fun seekForwardBy(ms: Long) = playerManager.seekForwardBy(ms)
    fun seekToBufferFraction(fraction: Float) = playerManager.seekToBufferFraction(fraction)
    fun jumpToLiveEdge() = playerManager.jumpToLiveEdge()
    fun tryBackupStream() = playerManager.tryBackupStream()
    fun retryStream() = playerManager.retryCurrentChannel()
    fun clearTimeshiftCache() = playerManager.clearTimeshiftCache()

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
