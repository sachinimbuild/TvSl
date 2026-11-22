package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.TvChannel
import com.example.ui.components.ChannelLogoBadge
import com.example.ui.components.LiveOrTimeshiftBadge
import com.example.ui.components.QuickAccessFavoritesStrip
import com.example.ui.theme.AmberGoldContainer
import com.example.ui.theme.AmberGoldTertiary
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.MonoMetricStyle
import com.example.ui.theme.SaffronContainer
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateContainer
import com.example.ui.theme.SlateContainerHigh
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.FormatUtils
import com.example.viewmodel.MainUiState

@Composable
fun TimeshiftGuideScreen(
    uiState: MainUiState,
    onSeekBackBy: (Long) -> Unit,
    onJumpToLive: () -> Unit,
    onWatchChannel: (TvChannel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val buffer = uiState.bufferInfo
    val active = uiState.activeChannel

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "1-Hour Reverse Buffer & TV Guide",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
            )
            Text(
                text = "Rewind live Sri Lankan broadcasts up to ${uiState.preferences.reverseBufferMinutes} minutes from your local disk ring buffer without spending extra mobile data.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }

        // Active 1-Hour Reverse Buffer Studio Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SlateContainer,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SlateBorder),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Filled.History,
                                contentDescription = null,
                                tint = EmeraldSecondary,
                            )
                            Column {
                                Text(
                                    text = active?.name ?: "No active stream",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = if (buffer.isProxyActive) {
                                        "Disk Ring Buffer Active • 0 KB Rewind Cost"
                                    } else {
                                        "Standard Stream Buffer"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldSecondary,
                                )
                            }
                        }

                        LiveOrTimeshiftBadge(
                            isAtLiveEdge = buffer.isAtLiveEdge,
                            offsetFromLiveMs = buffer.currentOffsetFromLiveMs,
                            onClick = onJumpToLive,
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Buffered Window: ${FormatUtils.formatDurationMmSs(buffer.bufferedDurationMs)}",
                                style = MonoMetricStyle,
                                color = TextPrimary,
                            )
                            Text(
                                text = "Max: ${FormatUtils.formatDurationMmSs(buffer.maxBufferCapacityMs)}",
                                style = MonoMetricStyle,
                                color = TextMuted,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { buffer.bufferFillFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = EmeraldSecondary,
                            trackColor = SlateContainerHigh,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = SlateContainerHigh,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Storage,
                                    contentDescription = null,
                                    tint = SaffronPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Column {
                                    Text("Buffer Disk", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                    Text(
                                        text = FormatUtils.formatBytes(buffer.diskUsedBytes),
                                        style = MonoMetricStyle,
                                        color = TextPrimary,
                                    )
                                }
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = SlateContainerHigh,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = AmberGoldTertiary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Column {
                                    Text("HLS Segments", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                    Text(
                                        text = "${buffer.segmentsCached} cached",
                                        style = MonoMetricStyle,
                                        color = TextPrimary,
                                    )
                                }
                            }
                        }
                    }

                    // Direct Time-Jump Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            "-5m" to 5 * 60_000L,
                            "-15m" to 15 * 60_000L,
                            "-30m" to 30 * 60_000L,
                            "-1h" to 60 * 60_000L,
                        ).forEach { (label, delta) ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSeekBackBy(delta) }
                                    .testTag("guide_jump_${label.replace("-", "")}"),
                                color = SaffronContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.5f)),
                            ) {
                                Text(
                                    text = label,
                                    style = MonoMetricStyle,
                                    color = SaffronPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            }
                        }
                    }

                    if (!buffer.isAtLiveEdge) {
                        Button(
                            onClick = onJumpToLive,
                            colors = ButtonDefaults.buttonColors(containerColor = LiveCrimson),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("guide_go_live_button"),
                        ) {
                            Text("Jump Back to Live Edge", color = Color.White)
                        }
                    }
                }
            }
        }

        item {
            QuickAccessFavoritesStrip(
                favoriteChannels = uiState.favoriteChannels,
                activeChannelId = uiState.activeChannel?.id,
                onSelectChannel = onWatchChannel,
                onRemoveFavorite = onToggleFavorite,
            )
        }

        item {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Sri Lanka TV Schedule",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
                Surface(
                    color = AmberGoldContainer,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = uiState.colomboClockText,
                        style = MonoMetricStyle,
                        color = AmberGoldTertiary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }

        items(uiState.allChannels, key = { "epg_${it.id}" }) { channel ->
            val current = channel.currentProgramAtHour(uiState.colomboHour24)
            val next = channel.nextProgramAtHour(uiState.colomboHour24)
            val isActive = uiState.activeChannel?.id == channel.id

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onWatchChannel(channel) }
                    .testTag("epg_channel_${channel.id}"),
                color = if (isActive) EmeraldContainer.copy(alpha = 0.45f) else SlateContainer,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isActive) EmeraldSecondary else SlateBorder),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ChannelLogoBadge(channel = channel, size = 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = channel.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                            )
                            Text(
                                text = channel.localName,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (current != null) {
                            Text(
                                text = "NOW (${FormatUtils.formatHourRange(current.startHour24, current.endHour24)}): ${current.title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SaffronPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (next != null) {
                            Text(
                                text = "NEXT: ${next.title} (${next.genre})",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isActive) EmeraldSecondary else SlateContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Watch ${channel.name}",
                            tint = if (isActive) Color.Black else TextPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
