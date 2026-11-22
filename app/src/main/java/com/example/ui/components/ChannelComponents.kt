package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.model.ChannelCategory
import com.example.model.ChannelLanguage
import com.example.model.DataSaverPreset
import com.example.model.NetworkTelemetry
import com.example.model.NetworkType
import com.example.model.TvChannel
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.MonoMetricStyle
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

@Composable
fun ChannelLogoBadge(
    channel: TvChannel,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
) {
    val accent = Color(channel.accentHex)
    val initials = remember(channel.name) {
        channel.name.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "TV" }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.28f),
                        SlateContainerHigh,
                    )
                )
            )
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (!channel.logoUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = channel.logoUrl,
                contentDescription = "${channel.name} logo",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                contentScale = ContentScale.Fit,
                loading = {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                    )
                },
                error = {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun LiveEqualizerBars(
    color: Color = SaffronPrimary,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "eq")
    val h1 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "b1",
    )
    val h2 by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(340), RepeatMode.Reverse),
        label = "b2",
    )
    val h3 by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "b3",
    )

    Row(
        modifier = modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(h1, h2, h3).forEach { frac ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((14.dp * frac).coerceAtLeast(4.dp))
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun LiveChannelCard(
    channel: TvChannel,
    isActive: Boolean,
    isFavorite: Boolean,
    colomboHour24: Int,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentProgram = channel.currentProgramAtHour(colomboHour24)
    val borderColor = if (isActive) SaffronPrimary else SlateBorder
    val containerColor = if (isActive) SaffronContainer.copy(alpha = 0.35f) else SlateContainer

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("channel_card_${channel.id}")
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onSelect),
        color = containerColor,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(if (isActive) 1.5.dp else 1.dp, borderColor),
        tonalElevation = if (isActive) 4.dp else 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChannelLogoBadge(channel = channel, size = 50.dp)

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = String.format("%02d", channel.number),
                        style = MonoMetricStyle,
                        color = if (isActive) SaffronPrimary else TextMuted,
                    )
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isActive) {
                        LiveEqualizerBars(color = SaffronPrimary)
                    }
                }

                Text(
                    text = "${channel.localName} • ${channel.language.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        color = if (isActive) LiveCrimson.copy(alpha = 0.2f) else SlateContainerHigh,
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = if (isActive) "WATCHING" else channel.category.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActive) LiveCrimson else TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Text(
                        text = currentProgram?.title ?: channel.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("favorite_btn_${channel.id}"),
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) SaffronPrimary else TextMuted,
                )
            }
        }
    }
}

@Composable
fun QuickAccessFavoritesStrip(
    favoriteChannels: List<TvChannel>,
    activeChannelId: String?,
    onSelectChannel: (TvChannel) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = SaffronPrimary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Quick Access Favorites",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                )
            }
            Text(
                text = "${favoriteChannels.size} saved",
                style = MonoMetricStyle,
                color = TextMuted,
            )
        }

        if (favoriteChannels.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("favorites_empty_banner"),
                color = SlateContainer,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SlateBorder.copy(alpha = 0.7f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Tap the heart on any Sri Lankan channel to pin it here for 1-tap switching.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
                modifier = Modifier.testTag("quick_favorites_strip"),
            ) {
                items(favoriteChannels, key = { "fav_quick_${it.id}" }) { channel ->
                    val isActive = channel.id == activeChannelId
                    val accent = Color(channel.accentHex)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectChannel(channel) }
                            .testTag("quick_fav_card_${channel.id}"),
                        color = if (isActive) SaffronContainer.copy(alpha = 0.5f) else SlateContainer,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            width = if (isActive) 1.5.dp else 1.dp,
                            color = if (isActive) SaffronPrimary else SlateBorder,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ChannelLogoBadge(channel = channel, size = 32.dp)
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = channel.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TextPrimary,
                                        maxLines = 1,
                                    )
                                    if (isActive) {
                                        LiveEqualizerBars(color = accent)
                                    }
                                }
                                Text(
                                    text = channel.language.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextMuted,
                                )
                            }
                            IconButton(
                                onClick = { onRemoveFavorite(channel.id) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("quick_fav_remove_${channel.id}"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Unpin ${channel.name} from favorites",
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkDataStatusPill(
    telemetry: NetworkTelemetry,
    preset: DataSaverPreset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isMobile = telemetry.networkType == NetworkType.MOBILE_DATA
    val isOffline = telemetry.networkType == NetworkType.OFFLINE
    val pillBg = when {
        isOffline -> LiveCrimson.copy(alpha = 0.18f)
        isMobile -> EmeraldContainer
        else -> SlateContainerHigh
    }
    val pillColor = when {
        isOffline -> LiveCrimson
        isMobile -> EmeraldSecondary
        else -> TextSecondary
    }

    Surface(
        modifier = modifier
            .testTag("network_data_pill")
            .clip(CircleShape)
            .clickable(onClick = onClick),
        color = pillBg,
        shape = CircleShape,
        border = BorderStroke(1.dp, pillColor.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = when {
                    isMobile -> Icons.Filled.SignalCellularAlt
                    else -> Icons.Filled.Wifi
                },
                contentDescription = "Network status",
                tint = pillColor,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = "${telemetry.networkType.label} • ${preset.shortLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                maxLines = 1,
            )
            if (telemetry.sessionBytesUsed > 0L) {
                Text(
                    text = FormatUtils.formatBytes(telemetry.sessionBytesUsed),
                    style = MonoMetricStyle,
                    color = pillColor,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileDataQuickSheet(
    currentPreset: DataSaverPreset,
    telemetry: NetworkTelemetry,
    onSelectPreset: (DataSaverPreset) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SlateSurface,
        contentColor = TextPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.DataSaverOn,
                    contentDescription = null,
                    tint = EmeraldSecondary,
                )
                Column {
                    Text(
                        text = "Mobile Data & Stream Quality",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Active: ${telemetry.networkType.label} • Session: ${FormatUtils.formatBytes(telemetry.sessionBytesUsed)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            DataSaverPreset.entries.forEach { preset ->
                val selected = preset == currentPreset
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preset_option_${preset.name}")
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onSelectPreset(preset)
                            onDismiss()
                        },
                    color = if (selected) EmeraldContainer else SlateContainer,
                    border = BorderStroke(1.dp, if (selected) EmeraldSecondary else SlateBorder),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = preset.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                )
                                Surface(
                                    color = SlateContainerHigh,
                                    shape = RoundedCornerShape(6.dp),
                                ) {
                                    Text(
                                        text = preset.estUsagePerHour,
                                        style = MonoMetricStyle,
                                        color = EmeraldSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = preset.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Selected",
                                tint = EmeraldSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCustomStreamDialog(
    onAdd: (name: String, url: String, category: ChannelCategory, language: ChannelLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ChannelCategory.CUSTOM) }
    var language by remember { mutableStateOf(ChannelLanguage.SINHALA) }

    val isValid = name.isNotBlank() &&
        (url.trim().startsWith("http://", ignoreCase = true) || url.trim().startsWith("https://", ignoreCase = true))

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        icon = {
            Icon(Icons.Filled.LiveTv, contentDescription = null, tint = SaffronPrimary)
        },
        title = {
            Text("Add Sri Lankan Live Stream", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Paste any HLS (.m3u8) or live IPTV URL. LankaTV applies 1-hour reverse buffering and mobile data limits automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Channel Name") },
                    placeholder = { Text("e.g., Derana Live HD") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_channel_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SaffronPrimary,
                        unfocusedBorderColor = SlateBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Stream URL (https://...m3u8)") },
                    placeholder = { Text("https://example.com/live/index.m3u8") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_channel_url_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SaffronPrimary,
                        unfocusedBorderColor = SlateBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                )
                Text("Language", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    listOf(
                        ChannelLanguage.SINHALA,
                        ChannelLanguage.TAMIL,
                        ChannelLanguage.ENGLISH,
                    ).forEach { lang ->
                        FilterChip(
                            selected = language == lang,
                            onClick = { language = lang },
                            label = { Text(lang.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SaffronContainer,
                                selectedLabelColor = SaffronPrimary,
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, url, category, language) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                modifier = Modifier.testTag("confirm_add_stream_button"),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add & Watch", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Cancel", color = TextSecondary)
            }
        },
    )
}
