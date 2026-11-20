package com.example.ui.tv

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cloudplay.NetworkOptimizer
import com.example.data.AppLanguage
import com.example.data.AppLogger
import com.example.data.AppUiMode
import com.example.data.GameLibraryRepository
import com.example.data.PerfScenario
import com.example.data.PerformanceMonitor
import com.example.data.SessionHistoryRepository
import com.example.data.SettingsRepository
import com.example.ui.DashboardTab
import com.example.ui.components.GamepadSurfaceController
import com.example.ui.components.GuideSection
import com.example.ui.components.HowToUseGuideDialog
import com.example.ui.components.SmartTranslatorDialog
import com.example.ui.components.WebDownloadPortalDialog
import com.example.ui.components.gamepadClickable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.vpn.ServerLocation
import com.example.vpn.VpnManager

@Composable
fun TvDashboardScreen(
    onLaunchUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val initialVpnState = remember {
        VpnManager.state.value.copy(
            serverPings = emptyMap(),
            serverMeasurements = emptyMap(),
            isTestingPings = false
        )
    }
    val vpnState by VpnManager.dashboardState.collectAsState(initial = initialVpnState)
    val config by SettingsRepository.config.collectAsState()
    val allGames by GameLibraryRepository.games.collectAsState()
    val recentSessions by SessionHistoryRepository.sessions.collectAsState()
    val streamRttMs by remember {
        NetworkOptimizer.telemetry.map { it.rttMs }.distinctUntilChanged()
    }.collectAsState(initial = NetworkOptimizer.telemetry.value.rttMs)

    androidx.compose.runtime.DisposableEffect(context) {
        com.example.ui.components.GamepadUiNavigation.keepFocusVisibleInTvMode = true
        com.example.ui.components.GamepadUiNavigation.onGamepadInteraction()
        com.example.cloudplay.RemotePadTvServer.startServerIfNeeded(context)
        com.example.cloudplay.RemotePadTvServer.onRemotePadButton = { buttonIndex, pressed, _ ->
            com.example.ui.components.GamepadUiNavigation.dispatchRemotePadButton(buttonIndex, pressed)
        }
        com.example.cloudplay.RemotePadTvServer.onRemotePadStick = { xAxisIndex, xVal, yAxisIndex, yVal ->
            if (xAxisIndex == 0 && yAxisIndex == 1) {
                com.example.ui.components.GamepadUiNavigation.dispatchRemotePadStick(xVal, yVal)
            }
        }
        com.example.cloudplay.RemotePadTvServer.onRemotePadReleaseAll = {
            com.example.ui.components.GamepadUiNavigation.releaseAllRemotePadInputs()
        }
        onDispose {
            com.example.ui.components.GamepadUiNavigation.keepFocusVisibleInTvMode = false
            com.example.ui.components.GamepadUiNavigation.releaseAllRemotePadInputs()
        }
    }

    val lastSessionPing = remember(vpnState.currentServer, recentSessions) {
        VpnManager.getLastSessionPingForServer(
            server = vpnState.currentServer,
            sessions = recentSessions
        ).takeIf { it > 0 }
    }
    val measuredPingMs: Int? = streamRttMs.takeIf { it > 0 }
        ?: vpnState.directLatencyMs?.takeIf { it > 0 }
        ?: lastSessionPing

    var currentTab by rememberSaveable { mutableStateOf(DashboardTab.HOME) }

    var showGuideDialog by remember { mutableStateOf(false) }
    var guideInitialSection by remember { mutableStateOf(GuideSection.QUICK_START) }
    var showTranslatorDialog by remember { mutableStateOf(false) }
    var showWebPortalDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showRemotePadDialog by rememberSaveable { mutableStateOf(false) }
    val remotePadServerState by com.example.cloudplay.RemotePadTvServer.state.collectAsState()
    var lastHandledPairPromptTick by rememberSaveable { androidx.compose.runtime.mutableLongStateOf(0L) }

    LaunchedEffect(remotePadServerState.pairPromptTick) {
        val tick = remotePadServerState.pairPromptTick
        if (tick > 0L && tick != lastHandledPairPromptTick) {
            lastHandledPairPromptTick = tick
            showRemotePadDialog = true
        }
    }

    val homePrimaryFocusRequester = remember { FocusRequester() }
    val libraryPrimaryFocusRequester = remember { FocusRequester() }
    val serversPrimaryFocusRequester = remember { FocusRequester() }
    val settingsPrimaryFocusRequester = remember { FocusRequester() }
    val activeRailFocusRequester = remember { FocusRequester() }
    var librarySearchFocusTick by remember { mutableIntStateOf(0) }
    var libraryFavoriteToggleTick by remember { mutableIntStateOf(0) }

    fun primaryRequesterForTab(tab: DashboardTab): FocusRequester = when (tab) {
        DashboardTab.HOME -> homePrimaryFocusRequester
        DashboardTab.LIBRARY -> libraryPrimaryFocusRequester
        DashboardTab.SERVERS -> serversPrimaryFocusRequester
        DashboardTab.SETTINGS -> settingsPrimaryFocusRequester
    }

    fun switchTab(newTab: DashboardTab, reason: String = "") {
        com.example.ui.components.GamepadUiNavigation.onGamepadInteraction()
        if (currentTab != newTab) {
            val nextScenario = when (newTab) {
                DashboardTab.HOME -> PerfScenario.HOME
                DashboardTab.LIBRARY -> PerfScenario.LIBRARY
                DashboardTab.SERVERS -> PerfScenario.SERVERS
                DashboardTab.SETTINGS -> PerfScenario.SETTINGS
            }
            PerformanceMonitor.setScenario(nextScenario)
            val suffix = if (reason.isNotBlank()) " ($reason)" else ""
            AppLogger.info("UI", "Chuyển tab TV: ${currentTab.name} -> ${newTab.name}$suffix")
            currentTab = newTab
        } else {
            runCatching { primaryRequesterForTab(newTab).requestFocus() }
        }
    }

    val isAnyDialogOpen = showGuideDialog || showTranslatorDialog || showWebPortalDialog || showLanguageDialog || showRemotePadDialog
    val activeTabPrimaryFocusRequester = primaryRequesterForTab(currentTab)

    GamepadSurfaceController(
        enabled = !isAnyDialogOpen,
        defaultFocusRequester = activeTabPrimaryFocusRequester,
        onPreviousTab = {
            val tabs = DashboardTab.entries
            val prevIdx = (tabs.indexOf(currentTab) - 1 + tabs.size) % tabs.size
            switchTab(tabs[prevIdx], "LB")
        },
        onNextTab = {
            val tabs = DashboardTab.entries
            val nextIdx = (tabs.indexOf(currentTab) + 1) % tabs.size
            switchTab(tabs[nextIdx], "RB")
        },
        onBackPress = {
            if (currentTab != DashboardTab.HOME) {
                switchTab(DashboardTab.HOME, "nút B")
            }
        },
        onSecondaryX = {
            when (currentTab) {
                DashboardTab.HOME -> {
                    // Không gán X bật/tắt VPN ở Trang chủ để tránh bấm nhầm làm đổi mạng
                }
                DashboardTab.LIBRARY -> {
                    librarySearchFocusTick++
                }
                DashboardTab.SERVERS -> {
                    if (!vpnState.isTestingPings) {
                        AppLogger.info("UI", "Bấm phím X tay cầm trên TV: Đo lại ping trên tab Máy chủ")
                        VpnManager.testAllServers(
                            autoPickBest = false,
                            context = context,
                            forceRefresh = true,
                            reason = "phím X tay cầm trên tab Máy chủ (TV)"
                        )
                    }
                }
                DashboardTab.SETTINGS -> {}
            }
        },
        onSecondaryY = {
            when (currentTab) {
                DashboardTab.HOME -> {
                    AppLogger.info("UI", "Bấm phím Y tay cầm trên TV: Mở trình duyệt Xbox Cloud từ Trang chủ")
                    onLaunchUrl(config.targetRegionUrl)
                }
                DashboardTab.LIBRARY -> {
                    libraryFavoriteToggleTick++
                }
                DashboardTab.SERVERS,
                DashboardTab.SETTINGS -> {}
            }
        },
        modifier = modifier
            .fillMaxSize()
            .background(TvBg)
            .testTag("tv_dashboard_screen")
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
            val scale = computeTvScale(maxWidth, maxHeight, fontScale)
            val safePadH = (22.dp * scale).coerceIn(12.dp, 32.dp)
            val safePadV = (16.dp * scale).coerceIn(10.dp, 24.dp)
            val railGap = (18.dp * scale).coerceIn(10.dp, 24.dp)

            LaunchedEffect(currentTab) {
                com.example.ui.components.GamepadUiNavigation.onGamepadInteraction()
                kotlinx.coroutines.delay(24)
                runCatching { primaryRequesterForTab(currentTab).requestFocus() }
            }

            // Vùng an toàn ở mép màn hình (.safe)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = safePadH, vertical = safePadV)
            ) {
                // Phần thân trên: Thanh bên trái (.rail) + Vùng nội dung chính (.main)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    TvLeftNavigationRail(
                        currentTab = currentTab,
                        config = config,
                        vpnState = vpnState,
                        measuredPingMs = measuredPingMs,
                        scale = scale,
                        activeRailFocusRequester = activeRailFocusRequester,
                        onSelectTab = { switchTab(it) },
                        onMoveRightIntoTab = {
                            runCatching { activeTabPrimaryFocusRequester.requestFocus() }
                        },
                        onVpnChipClick = { switchTab(DashboardTab.SERVERS, "bấm chip VPN") }
                    )

                    Spacer(modifier = Modifier.width(railGap))

                    val onSelectServerStable = remember(context) {
                        { srv: ServerLocation -> VpnManager.selectServer(srv, context) }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        when (currentTab) {
                            DashboardTab.HOME -> {
                                TvHomeScreen(
                                    config = config,
                                    vpnState = vpnState,
                                    measuredPingMs = measuredPingMs,
                                    featuredGames = allGames,
                                    scale = scale,
                                    defaultFocusRequester = homePrimaryFocusRequester,
                                    onOpenXboxCloud = { onLaunchUrl(config.targetRegionUrl) },
                                    onLaunchGame = { rawUrl ->
                                        onLaunchUrl(
                                            SettingsRepository.formatXboxUrlWithLocale(
                                                rawUrl,
                                                config.getEffectiveLocale(),
                                                com.example.data.XboxWebUiMode.CLASSIC
                                            )
                                        )
                                    },
                                    onConnectVpn = { VpnManager.connect(context) },
                                    onDisconnectVpn = { VpnManager.disconnect(context) },
                                    onNavigateToLibrary = { switchTab(DashboardTab.LIBRARY, "từ Trang chủ TV") },
                                    onNavigateToServers = { switchTab(DashboardTab.SERVERS, "từ Trang chủ TV") },
                                    onOpenLanguagePicker = { showLanguageDialog = true },
                                    onOpenTranslator = { showTranslatorDialog = true },
                                    onOpenWebPortal = { showWebPortalDialog = true },
                                    onOpenRemotePad = { showRemotePadDialog = true },
                                    onOpenGuide = { section ->
                                        guideInitialSection = section
                                        showGuideDialog = true
                                    }
                                )
                            }
                            DashboardTab.LIBRARY -> {
                                TvLibraryScreen(
                                    config = config,
                                    games = allGames,
                                    scale = scale,
                                    defaultFocusRequester = libraryPrimaryFocusRequester,
                                    focusSearchTrigger = librarySearchFocusTick,
                                    favoriteToggleTrigger = libraryFavoriteToggleTick,
                                    onLaunchGame = { rawUrl ->
                                        onLaunchUrl(
                                            SettingsRepository.formatXboxUrlWithLocale(
                                                rawUrl,
                                                config.getEffectiveLocale(),
                                                com.example.data.XboxWebUiMode.CLASSIC
                                            )
                                        )
                                    }
                                )
                            }
                            DashboardTab.SERVERS -> {
                                TvServersScreen(
                                    config = config,
                                    currentServer = vpnState.currentServer,
                                    onSelectServer = onSelectServerStable,
                                    isTabActive = true,
                                    scale = scale,
                                    defaultFocusRequester = serversPrimaryFocusRequester
                                )
                            }
                            DashboardTab.SETTINGS -> {
                                TvSettingsScreen(
                                    config = config,
                                    vpnState = vpnState,
                                    scale = scale,
                                    defaultFocusRequester = settingsPrimaryFocusRequester,
                                    onMoveLeftToRail = {
                                        runCatching { activeRailFocusRequester.requestFocus() }
                                    },
                                    onOpenGuide = { section ->
                                        guideInitialSection = section
                                        showGuideDialog = true
                                    },
                                    onClearCookies = {
                                        android.webkit.CookieManager.getInstance().removeAllCookies(null)
                                        android.webkit.WebStorage.getInstance().deleteAllData()
                                        val msg = if (config.isSettingsEnglishUI()) {
                                            "Cleared saved login & web data!"
                                        } else {
                                            "Đã làm mới dữ liệu đăng nhập Xbox!"
                                        }
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height((10.dp * scale).coerceAtLeast(6.dp)))

                // Thanh gợi ý phím ở đáy (.keys)
                val bottomHints = remember(currentTab, config.appLanguage) {
                    when (currentTab) {
                        DashboardTab.HOME -> listOf(
                            TvKeyHint(listOf("A" to TvKeyABg), config.tr("Chọn", "Select")),
                            TvKeyHint(listOf("B" to TvKeyBBg), config.tr("Quay lại", "Back")),
                            TvKeyHint(listOf("LB" to TvSurface2, "RB" to TvSurface2), config.tr("Đổi mục", "Switch Tab")),
                            TvKeyHint(listOf("Y" to TvKeyYBg), config.tr("Trình duyệt Xbox", "Xbox Browser"))
                        )
                        DashboardTab.LIBRARY -> listOf(
                            TvKeyHint(listOf("A" to TvKeyABg), config.tr("Chơi", "Play")),
                            TvKeyHint(listOf("B" to TvKeyBBg), config.tr("Quay lại", "Back")),
                            TvKeyHint(listOf("LB" to TvSurface2, "RB" to TvSurface2), config.tr("Đổi mục", "Switch Tab")),
                            TvKeyHint(listOf("X" to TvKeyXBg), config.tr("Tìm kiếm", "Search")),
                            TvKeyHint(listOf("Y" to TvKeyYBg), config.tr("Yêu thích", "Favorite"))
                        )
                        DashboardTab.SERVERS -> listOf(
                            TvKeyHint(listOf("A" to TvKeyABg), config.tr("Chọn", "Select")),
                            TvKeyHint(listOf("B" to TvKeyBBg), config.tr("Quay lại", "Back")),
                            TvKeyHint(listOf("LB" to TvSurface2, "RB" to TvSurface2), config.tr("Đổi mục", "Switch Tab")),
                            TvKeyHint(listOf("X" to TvKeyXBg), config.tr("Đo lại ping", "Re-test Ping"))
                        )
                        DashboardTab.SETTINGS -> listOf(
                            TvKeyHint(listOf("A" to TvKeyABg), config.tr("Chọn / Bật tắt", "Select / Toggle")),
                            TvKeyHint(listOf("B" to TvKeyBBg), config.tr("Quay lại", "Back")),
                            TvKeyHint(listOf("LB" to TvSurface2, "RB" to TvSurface2), config.tr("Đổi mục", "Switch Tab")),
                            TvKeyHint(listOf("←" to TvSurface2, "→" to TvSurface2), config.tr("Đổi giá trị", "Adjust"))
                        )
                    }
                }

                TvBottomKeyHintBar(
                    hints = bottomHints,
                    scale = scale
                )
            }
        }
    }

    if (showLanguageDialog) {
        val isEnUi = config.isEnglishUI()
        com.example.ui.components.AdaptiveLanguagePickerDialog(
            currentLanguage = config.appLanguage,
            isEnUi = isEnUi,
            titleText = config.tr("Ngôn ngữ ứng dụng", "App Language"),
            closeText = config.tr("Đóng", "Close"),
            onSelectLanguage = { langOption ->
                showLanguageDialog = false
                SettingsRepository.updateAppLanguage(langOption)
                Toast.makeText(
                    context,
                    "✓ ${langOption.flag} ${langOption.title}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showGuideDialog) {
        HowToUseGuideDialog(
            initialSection = guideInitialSection,
            onDismiss = { showGuideDialog = false }
        )
    }

    if (showTranslatorDialog) {
        SmartTranslatorDialog(
            onDismissRequest = { showTranslatorDialog = false }
        )
    }

    if (showWebPortalDialog) {
        WebDownloadPortalDialog(
            onDismiss = { showWebPortalDialog = false }
        )
    }

    if (showRemotePadDialog) {
        TvRemotePadDialog(
            config = config,
            onDismiss = { showRemotePadDialog = false }
        )
    }
}
