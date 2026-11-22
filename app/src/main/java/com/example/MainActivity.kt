package com.example

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.DataSaverOn
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.AddCustomStreamDialog
import com.example.ui.components.MobileDataQuickSheet
import com.example.ui.screens.DataSettingsScreen
import com.example.ui.screens.LiveTvScreen
import com.example.ui.screens.TimeshiftGuideScreen
import com.example.ui.theme.AmberGoldTertiary
import com.example.ui.theme.LankaTvTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SaffronContainer
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel

const val ROUTE_LIVE_TV = "live_tv"
const val ROUTE_TIMESHIFT_GUIDE = "timeshift_guide"
const val ROUTE_DATA_SETTINGS = "data_settings"

private data class NavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val appDestinations = listOf(
    NavDestination(
        route = ROUTE_LIVE_TV,
        label = "Live TV",
        selectedIcon = Icons.Filled.LiveTv,
        unselectedIcon = Icons.Outlined.LiveTv,
        testTag = "nav_live_tv",
    ),
    NavDestination(
        route = ROUTE_TIMESHIFT_GUIDE,
        label = "1h Replay",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_timeshift_guide",
    ),
    NavDestination(
        route = ROUTE_DATA_SETTINGS,
        label = "Data & Buffer",
        selectedIcon = Icons.Filled.DataSaverOn,
        unselectedIcon = Icons.Outlined.DataSaverOn,
        testTag = "nav_data_settings",
    ),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        setContent {
            LankaTvTheme {
                LankaTvAppRoot()
            }
        }
    }
}

@Composable
fun LankaTvAppRoot(
    viewModel: MainViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_LIVE_TV

    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var showDataSheet by rememberSaveable { mutableStateOf(false) }
    var showAddStreamDialog by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    // Pause live playback when app goes to background so mobile data is never wasted
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (viewModel.playerManager.player.isPlaying) {
                    viewModel.playerManager.player.pause()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Handle fullscreen system bars & orientation cleanly
    LaunchedEffect(isFullscreen) {
        activity?.let { act ->
            val window = act.window
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullscreen) {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    val navigateTo: (String) -> Unit = { route ->
        isFullscreen = false
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
    ) {
        val useNavRail = maxWidth >= 840.dp && !isFullscreen

        Scaffold(
            containerColor = ObsidianBg,
            contentWindowInsets = if (isFullscreen) WindowInsets(0, 0, 0, 0) else WindowInsets.safeDrawing,
            bottomBar = {
                if (!isFullscreen && !useNavRail) {
                    NavigationBar(
                        containerColor = SlateSurface,
                        contentColor = TextPrimary,
                        tonalElevation = 0.dp,
                    ) {
                        appDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label,
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SaffronPrimary,
                                    selectedTextColor = SaffronPrimary,
                                    indicatorColor = SaffronContainer,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                ),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = SlateSurface,
                        contentColor = TextPrimary,
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                    ) {
                        appDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label,
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag),
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = SaffronPrimary,
                                    selectedTextColor = SaffronPrimary,
                                    indicatorColor = SaffronContainer,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                ),
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_LIVE_TV,
                    ) {
                        composable(ROUTE_LIVE_TV) {
                            LiveTvScreen(
                                uiState = uiState,
                                player = viewModel.playerManager.player,
                                isFullscreen = isFullscreen,
                                onToggleFullscreen = { isFullscreen = !isFullscreen },
                                onSelectChannel = viewModel::selectChannel,
                                onToggleFavorite = viewModel::toggleChannelFavorite,
                                onSelectCategory = viewModel::selectCategory,
                                onSelectLanguage = viewModel::selectLanguage,
                                onToggleOnlyFavorites = viewModel::toggleFavoritesFilter,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onRefreshDirectory = { viewModel.refreshChannelDirectory(false) },
                                onOpenDataSheet = { showDataSheet = true },
                                onOpenAddCustomDialog = { showAddStreamDialog = true },
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onSeekBackBy = viewModel::seekBackBy,
                                onSeekForwardBy = viewModel::seekForwardBy,
                                onSeekToFraction = viewModel::seekToBufferFraction,
                                onJumpToLive = viewModel::jumpToLiveEdge,
                                onRetryStream = viewModel::retryStream,
                                onTryBackupStream = viewModel::tryBackupStream,
                            )
                        }

                        composable(ROUTE_TIMESHIFT_GUIDE) {
                            TimeshiftGuideScreen(
                                uiState = uiState,
                                onSeekBackBy = { delta ->
                                    viewModel.seekBackBy(delta)
                                    navigateTo(ROUTE_LIVE_TV)
                                },
                                onJumpToLive = {
                                    viewModel.jumpToLiveEdge()
                                    navigateTo(ROUTE_LIVE_TV)
                                },
                                onWatchChannel = { channel ->
                                    viewModel.selectChannel(channel)
                                    navigateTo(ROUTE_LIVE_TV)
                                },
                                onToggleFavorite = viewModel::toggleChannelFavorite,
                            )
                        }

                        composable(ROUTE_DATA_SETTINGS) {
                            DataSettingsScreen(
                                uiState = uiState,
                                onSelectPreset = viewModel::requestDataSaverPreset,
                                onSetAllowMobileData = viewModel::setAllowMobileDataStreaming,
                                onSetWarnHdOnMobile = viewModel::setWarnHdOnMobileData,
                                onSetReverseBufferMinutes = viewModel::setReverseBufferMinutes,
                                onClearBufferCache = viewModel::clearTimeshiftCache,
                                onRefreshDirectory = { viewModel.refreshChannelDirectory(false) },
                                onOpenAddCustomDialog = { showAddStreamDialog = true },
                                onRemoveCustomChannel = viewModel::removeCustomChannel,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDataSheet) {
        MobileDataQuickSheet(
            currentPreset = uiState.preferences.dataSaverPreset,
            telemetry = uiState.networkTelemetry,
            onSelectPreset = viewModel::requestDataSaverPreset,
            onDismiss = { showDataSheet = false },
        )
    }

    if (showAddStreamDialog) {
        AddCustomStreamDialog(
            onAdd = { name, url, cat, lang ->
                viewModel.addCustomChannel(name, url, cat, lang)
                showAddStreamDialog = false
                navigateTo(ROUTE_LIVE_TV)
            },
            onDismiss = { showAddStreamDialog = false },
        )
    }

    val pendingHdPreset = uiState.pendingHdMobileWarningPreset
    if (pendingHdPreset != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissHdMobileWarning,
            containerColor = SlateSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            icon = {
                Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = AmberGoldTertiary)
            },
            title = {
                Text("High Data Usage on Mobile Data", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Text(
                    text = "You are currently on Mobile Data. Switching to ${pendingHdPreset.title} (${pendingHdPreset.estUsagePerHour}) can consume your mobile data bundle faster.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmHdMobileWarning,
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                ) {
                    Text("Use ${pendingHdPreset.shortLabel}", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissHdMobileWarning) {
                    Text("Keep Data Saver", color = TextSecondary)
                }
            },
        )
    }
}
