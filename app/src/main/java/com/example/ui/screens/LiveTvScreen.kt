package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TvOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.R
import com.example.model.ChannelCategory
import com.example.model.ChannelLanguage
import com.example.model.TvChannel
import com.example.ui.components.LiveChannelCard
import com.example.ui.components.LiveTvPlayerStage
import com.example.ui.components.NetworkDataStatusPill
import com.example.ui.components.QuickAccessFavoritesStrip
import com.example.ui.components.QuickRewindStrip
import com.example.ui.theme.AmberGoldTertiary
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.MonoMetricStyle
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SaffronContainer
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateContainer
import com.example.ui.theme.SlateContainerHigh
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.FormatUtils
import com.example.viewmodel.MainUiState

@Composable
fun LiveTvScreen(
    uiState: MainUiState,
    player: Player,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onSelectChannel: (TvChannel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSelectCategory: (ChannelCategory) -> Unit,
    onSelectLanguage: (ChannelLanguage) -> Unit,
    onToggleOnlyFavorites: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRefreshDirectory: () -> Unit,
    onOpenDataSheet: () -> Unit,
    onOpenAddCustomDialog: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekBackBy: (Long) -> Unit,
    onSeekForwardBy: (Long) -> Unit,
    onSeekToFraction: (Float) -> Unit,
    onJumpToLive: () -> Unit,
    onRetryStream: () -> Unit,
    onTryBackupStream: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isFullscreen) {
        LiveTvPlayerStage(
            player = player,
            activeChannel = uiState.activeChannel,
            playbackState = uiState.playbackState,
            bufferInfo = uiState.bufferInfo,
            networkTelemetry = uiState.networkTelemetry,
            dataSaverPreset = uiState.preferences.dataSaverPreset,
            isPlaying = uiState.isPlaying,
            isFullscreen = true,
            onToggleFullscreen = onToggleFullscreen,
            onTogglePlayPause = onTogglePlayPause,
            onSeekBackBy = onSeekBackBy,
            onSeekForwardBy = onSeekForwardBy,
            onSeekToFraction = onSeekToFraction,
            onJumpToLive = onJumpToLive,
            onRetry = onRetryStream,
            onTryBackup = onTryBackupStream,
            onOpenDataSheet = onOpenDataSheet,
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideLayout = maxWidth >= 760.dp

        if (isWideLayout) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StudioTopBar(
                        uiState = uiState,
                        onOpenDataSheet = onOpenDataSheet,
                        onOpenAddCustomDialog = onOpenAddCustomDialog,
                        onRefreshDirectory = onRefreshDirectory,
                    )
                    LiveTvPlayerStage(
                        player = player,
                        activeChannel = uiState.activeChannel,
                        playbackState = uiState.playbackState,
                        bufferInfo = uiState.bufferInfo,
                        networkTelemetry = uiState.networkTelemetry,
                        dataSaverPreset = uiState.preferences.dataSaverPreset,
                        isPlaying = uiState.isPlaying,
                        isFullscreen = false,
                        onToggleFullscreen = onToggleFullscreen,
                        onTogglePlayPause = onTogglePlayPause,
                        onSeekBackBy = onSeekBackBy,
                        onSeekForwardBy = onSeekForwardBy,
                        onSeekToFraction = onSeekToFraction,
                        onJumpToLive = onJumpToLive,
                        onRetry = onRetryStream,
                        onTryBackup = onTryBackupStream,
                        onOpenDataSheet = onOpenDataSheet,
                    )
                    ActiveChannelStatusDeck(
                        uiState = uiState,
                        onToggleActiveFavorite = {
                            uiState.activeChannel?.id?.let(onToggleFavorite)
                        },
                        onSeekBackBy = onSeekBackBy,
                        onJumpToLive = onJumpToLive,
                    )
                }

                ChannelDirectoryPanel(
                    uiState = uiState,
                    onSelectChannel = onSelectChannel,
                    onToggleFavorite = onToggleFavorite,
                    onSelectCategory = onSelectCategory,
                    onSelectLanguage = onSelectLanguage,
                    onToggleOnlyFavorites = onToggleOnlyFavorites,
                    onSearchQueryChange = onSearchQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                StudioTopBar(
                    uiState = uiState,
                    onOpenDataSheet = onOpenDataSheet,
                    onOpenAddCustomDialog = onOpenAddCustomDialog,
                    onRefreshDirectory = onRefreshDirectory,
                )
                LiveTvPlayerStage(
                    player = player,
                    activeChannel = uiState.activeChannel,
                    playbackState = uiState.playbackState,
                    bufferInfo = uiState.bufferInfo,
                    networkTelemetry = uiState.networkTelemetry,
                    dataSaverPreset = uiState.preferences.dataSaverPreset,
                    isPlaying = uiState.isPlaying,
                    isFullscreen = false,
                    onToggleFullscreen = onToggleFullscreen,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeekBackBy = onSeekBackBy,
                    onSeekForwardBy = onSeekForwardBy,
                    onSeekToFraction = onSeekToFraction,
                    onJumpToLive = onJumpToLive,
                    onRetry = onRetryStream,
                    onTryBackup = onTryBackupStream,
                    onOpenDataSheet = onOpenDataSheet,
                )
                ActiveChannelStatusDeck(
                    uiState = uiState,
                    onToggleActiveFavorite = {
                        uiState.activeChannel?.id?.let(onToggleFavorite)
                    },
                    onSeekBackBy = onSeekBackBy,
                    onJumpToLive = onJumpToLive,
                )
                ChannelDirectoryPanel(
                    uiState = uiState,
                    onSelectChannel = onSelectChannel,
                    onToggleFavorite = onToggleFavorite,
                    onSelectCategory = onSelectCategory,
                    onSelectLanguage = onSelectLanguage,
                    onToggleOnlyFavorites = onToggleOnlyFavorites,
                    onSearchQueryChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StudioTopBar(
    uiState: MainUiState,
    onOpenDataSheet: () -> Unit,
    onOpenAddCustomDialog: () -> Unit,
    onRefreshDirectory: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SlateBorder),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_broadcast_hero),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.32f),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                ObsidianBg.copy(alpha = 0.85f),
                                ObsidianBg.copy(alpha = 0.55f),
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "LankaTV",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        Surface(
                            color = SaffronContainer,
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = uiState.colomboClockText,
                                style = MonoMetricStyle,
                                color = SaffronPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    NetworkDataStatusPill(
                        telemetry = uiState.networkTelemetry,
                        preset = uiState.preferences.dataSaverPreset,
                        onClick = onOpenDataSheet,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRefreshDirectory,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("sync_channels_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh Sri Lanka TV directory",
                            tint = if (uiState.isSyncingDirectory) SaffronPrimary else TextSecondary,
                        )
                    }
                    IconButton(
                        onClick = onOpenAddCustomDialog,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("add_custom_stream_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add custom stream",
                            tint = SaffronPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveChannelStatusDeck(
    uiState: MainUiState,
    onToggleActiveFavorite: () -> Unit,
    onSeekBackBy: (Long) -> Unit,
    onJumpToLive: () -> Unit,
) {
    val active = uiState.activeChannel
    val isActiveFavorite = active != null && active.id in uiState.preferences.favoriteChannelIds
    val currentShow = active?.currentProgramAtHour(uiState.colomboHour24)
    val nextShow = active?.nextProgramAtHour(uiState.colomboHour24)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SlateContainer,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SlateBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = active?.let { "${it.name} • ${it.localName}" } ?: "Select a Sri Lankan channel",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = currentShow?.let { "Now: ${it.title} • Next: ${nextShow?.title.orEmpty()}" }
                            ?: active?.description.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (active != null) {
                        IconButton(
                            onClick = onToggleActiveFavorite,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("active_channel_favorite_button"),
                        ) {
                            Icon(
                                imageVector = if (isActiveFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isActiveFavorite) {
                                    "Remove ${active.name} from favorites"
                                } else {
                                    "Save ${active.name} to favorites"
                                },
                                tint = if (isActiveFavorite) SaffronPrimary else TextSecondary,
                            )
                        }
                    }

                    Surface(
                        color = EmeraldContainer,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, EmeraldSecondary.copy(alpha = 0.4f)),
                    ) {
                        Text(
                            text = if (uiState.bufferInfo.isProxyActive) {
                                "1h DVR ${FormatUtils.formatDurationMmSs(uiState.bufferInfo.bufferedDurationMs)}"
                            } else {
                                "Direct Live"
                            },
                            style = MonoMetricStyle,
                            color = if (uiState.bufferInfo.isProxyActive) EmeraldSecondary else AmberGoldTertiary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            QuickRewindStrip(
                bufferInfo = uiState.bufferInfo,
                onSeekBackBy = onSeekBackBy,
                onJumpToLive = onJumpToLive,
            )
        }
    }
}

@Composable
private fun ChannelDirectoryPanel(
    uiState: MainUiState,
    onSelectChannel: (TvChannel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSelectCategory: (ChannelCategory) -> Unit,
    onSelectLanguage: (ChannelLanguage) -> Unit,
    onToggleOnlyFavorites: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickAccessFavoritesStrip(
            favoriteChannels = uiState.favoriteChannels,
            activeChannelId = uiState.activeChannel?.id,
            onSelectChannel = onSelectChannel,
            onRemoveFavorite = onToggleFavorite,
        )

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    "Search Sri Lankan channels (Rupavahini, Derana, Sirasa...)",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted)
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear search", tint = TextMuted)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("channel_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SlateContainer,
                unfocusedContainerColor = SlateContainer,
                focusedBorderColor = SaffronPrimary,
                unfocusedBorderColor = SlateBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedPlaceholderColor = TextMuted,
                unfocusedPlaceholderColor = TextMuted,
            ),
        )

        // Category & Favorite filter chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
        ) {
            item {
                FilterChip(
                    selected = uiState.onlyFavorites,
                    onClick = onToggleOnlyFavorites,
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                    },
                    label = { Text("Favorites (${uiState.favoriteChannels.size})") },
                    modifier = Modifier.testTag("filter_favorites_chip"),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SlateContainer,
                        labelColor = TextSecondary,
                        selectedContainerColor = SaffronContainer,
                        selectedLabelColor = SaffronPrimary,
                        selectedLeadingIconColor = SaffronPrimary,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = uiState.onlyFavorites,
                        borderColor = SlateBorder,
                        selectedBorderColor = SaffronPrimary,
                    ),
                )
            }
            items(ChannelCategory.entries) { category ->
                val selected = uiState.selectedCategory == category
                FilterChip(
                    selected = selected,
                    onClick = { onSelectCategory(category) },
                    label = { Text(category.label) },
                    modifier = Modifier.testTag("filter_cat_${category.name}"),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SlateContainer,
                        labelColor = TextSecondary,
                        selectedContainerColor = SaffronContainer,
                        selectedLabelColor = SaffronPrimary,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = SlateBorder,
                        selectedBorderColor = SaffronPrimary,
                    ),
                )
            }
        }

        // Language sub-filter row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
        ) {
            items(ChannelLanguage.entries) { lang ->
                val selected = uiState.selectedLanguage == lang
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectLanguage(lang) }
                        .testTag("filter_lang_${lang.name}"),
                    color = if (selected) SlateContainerHigh else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selected) EmeraldSecondary.copy(alpha = 0.6f) else SlateBorder.copy(alpha = 0.5f),
                    ),
                ) {
                    Text(
                        text = lang.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) EmeraldSecondary else TextMuted,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }

        if (uiState.filteredChannels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Filled.TvOff,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(36.dp),
                    )
                    Text(
                        text = "No matching Sri Lankan channels",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary,
                    )
                    Text(
                        text = "Clear filters or tap + to add a custom M3U8 live stream.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 14.dp),
            ) {
                items(uiState.filteredChannels, key = { it.id }) { channel ->
                    LiveChannelCard(
                        channel = channel,
                        isActive = uiState.activeChannel?.id == channel.id,
                        isFavorite = channel.id in uiState.preferences.favoriteChannelIds,
                        colomboHour24 = uiState.colomboHour24,
                        onSelect = { onSelectChannel(channel) },
                        onToggleFavorite = { onToggleFavorite(channel.id) },
                    )
                }
            }
        }
    }
}
