package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.DataSaverPreset
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.MonoMetricStyle
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateContainer
import com.example.ui.theme.SlateContainerHigh
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.FormatUtils
import com.example.viewmodel.MainUiState
import kotlin.math.roundToInt

@Composable
fun DataSettingsScreen(
    uiState: MainUiState,
    onSelectPreset: (DataSaverPreset) -> Unit,
    onSetAllowMobileData: (Boolean) -> Unit,
    onSetWarnHdOnMobile: (Boolean) -> Unit,
    onSetReverseBufferMinutes: (Int) -> Unit,
    onClearBufferCache: () -> Unit,
    onRefreshDirectory: () -> Unit,
    onOpenAddCustomDialog: () -> Unit,
    onRemoveCustomChannel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val prefs = uiState.preferences
    val telemetry = uiState.networkTelemetry

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Mobile Data & Buffer Controls",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
            )
            Text(
                text = "Optimize Sri Lankan live TV for 3G/4G/5G mobile data plans and configure your 1-hour reverse DVR ring buffer.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }

        // Network Telemetry Overview
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SlateContainer,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, SlateBorder),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.SignalCellularAlt,
                            contentDescription = null,
                            tint = EmeraldSecondary,
                        )
                        Text(
                            text = "Live Connection Telemetry",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TelemetryTile(
                            label = "Network",
                            value = telemetry.networkType.label,
                            modifier = Modifier.weight(1f),
                        )
                        TelemetryTile(
                            label = "Session Data",
                            value = FormatUtils.formatBytes(telemetry.sessionBytesUsed),
                            modifier = Modifier.weight(1f),
                        )
                        TelemetryTile(
                            label = "Stream Res",
                            value = telemetry.videoResolution,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Stream on Mobile Data",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                            )
                            Text(
                                text = "Allow live video playback over 4G/5G cellular connections",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        Switch(
                            checked = prefs.allowMobileDataStreaming,
                            onCheckedChange = onSetAllowMobileData,
                            modifier = Modifier.testTag("switch_allow_mobile_data"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldSecondary,
                            ),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Warn before HD on Mobile Data",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                            )
                            Text(
                                text = "Confirm before switching to 720p+ HD over metered cellular",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        Switch(
                            checked = prefs.warnHdOnMobileData,
                            onCheckedChange = onSetWarnHdOnMobile,
                            modifier = Modifier.testTag("switch_warn_hd_mobile"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SaffronPrimary,
                            ),
                        )
                    }
                }
            }
        }

        // Data Saver Presets
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SlateContainer,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, SlateBorder),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.DataSaverOn, contentDescription = null, tint = SaffronPrimary)
                        Text(
                            text = "Mobile Data Quality Cap",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                    }

                    DataSaverPreset.entries.forEach { preset ->
                        val selected = preset == prefs.dataSaverPreset
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectPreset(preset) }
                                .testTag("settings_preset_${preset.name}"),
                            color = if (selected) EmeraldContainer else SlateContainerHigh,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (selected) EmeraldSecondary else SlateBorder),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
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
                                        Text(
                                            text = preset.estUsagePerHour,
                                            style = MonoMetricStyle,
                                            color = EmeraldSecondary,
                                        )
                                    }
                                    Text(
                                        text = preset.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = "Active",
                                        tint = EmeraldSecondary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1-Hour Reverse Buffer Configuration
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SlateContainer,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, SlateBorder),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Filled.History, contentDescription = null, tint = EmeraldSecondary)
                            Text(
                                text = "Reverse Buffer Window",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                        }
                        Text(
                            text = "${prefs.reverseBufferMinutes} min",
                            style = MonoMetricStyle,
                            color = SaffronPrimary,
                        )
                    }

                    Slider(
                        value = prefs.reverseBufferMinutes.toFloat(),
                        onValueChange = { raw ->
                            val snapped = ((raw / 15f).roundToInt() * 15).coerceIn(15, 60)
                            onSetReverseBufferMinutes(snapped)
                        },
                        valueRange = 15f..60f,
                        steps = 2,
                        colors = SliderDefaults.colors(
                            thumbColor = SaffronPrimary,
                            activeTrackColor = SaffronPrimary,
                            inactiveTrackColor = SlateContainerHigh,
                        ),
                        modifier = Modifier.testTag("buffer_duration_slider"),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("15 min", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("30 min", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("45 min", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("60 min (1 Hour)", style = MaterialTheme.typography.labelSmall, color = EmeraldSecondary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Disk used: ${FormatUtils.formatBytes(uiState.bufferInfo.diskUsedBytes)}",
                            style = MonoMetricStyle,
                            color = TextSecondary,
                        )
                        OutlinedButton(
                            onClick = onClearBufferCache,
                            border = BorderStroke(1.dp, SlateBorder),
                            modifier = Modifier.testTag("clear_buffer_cache_button"),
                        ) {
                            Icon(
                                Icons.Filled.DeleteSweep,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Clear Buffer", color = TextPrimary)
                        }
                    }
                }
            }
        }

        // Live Channel Directory Sync & Custom Streams
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SlateContainer,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, SlateBorder),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Sri Lanka Live Directory & Custom Streams",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Status: ${uiState.directorySyncLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldSecondary,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onRefreshDirectory,
                            border = BorderStroke(1.dp, EmeraldSecondary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_sync_directory_button"),
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = EmeraldSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Sync LK Playlist", color = EmeraldSecondary)
                        }

                        Button(
                            onClick = onOpenAddCustomDialog,
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_add_stream_button"),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add Stream", color = Color.White)
                        }
                    }
                }
            }
        }

        if (prefs.customChannels.isNotEmpty()) {
            item {
                Text(
                    text = "Saved Custom Streams",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary,
                )
            }
            items(prefs.customChannels, key = { "custom_mgr_${it.id}" }) { custom ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SlateContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(custom.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                custom.streamUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                maxLines = 1,
                            )
                        }
                        IconButton(
                            onClick = { onRemoveCustomChannel(custom.id) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Remove ${custom.name}",
                                tint = LiveCrimson,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = SlateContainerHigh,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Text(value, style = MonoMetricStyle, color = TextPrimary)
        }
    }
}
