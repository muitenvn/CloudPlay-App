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
import com.example.ui.tabs.GameLibraryScreen
import com.example.ui.tabs.ServersScreen
import com.example.ui.tabs.SettingsScreen
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
    val streamTelemetry by NetworkOptimizer.telemetry.collectAsState()

    val lastSessionPing = remember(vpnState.currentServer, recentSessions) {
        VpnManager.getLastSessionPingForServer(
            server = vpnState.currentServer,
            sessions = recentSessions
        ).takeIf { it > 0 }
    }
    val measuredPingMs: Int? = streamTelemetry.rttMs.takeIf { it > 0 }
        ?: vpnState.directLatencyMs?.takeIf { it > 0 }
        ?: lastSessionPing

    var currentTab by rememberSaveable { mutableStateOf(DashboardTab.HOME) }
    val visitedTabs = remember { androidx.compose.runtime.mutableStateListOf(currentTab) }
    if (currentTab !in visitedTabs) {
        visitedTabs.add(currentTab)
    }

    var showGuideDialog by remember { mutableStateOf(false) }
    var guideInitialSection by remember { mutableStateOf(GuideSection.QUICK_START) }
    var showTranslatorDialog by remember { mutableStateOf(false) }
    var showWebPortalDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val homePrimaryFocusRequester = remember { FocusRequester() }
    val activeRailFocusRequester = remember { FocusRequester() }
    var librarySearchFocusTick by remember { mutableIntStateOf(0) }

    fun switchTab(newTab: DashboardTab, reason: String = "") {
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
            if (newTab !in visitedTabs) {
                visitedTabs.add(newTab)
            }
            currentTab = newTab
        }
    }

    val isAnyDialogOpen = showGuideDialog || showTranslatorDialog || showWebPortalDialog || showLanguageDialog

    GamepadSurfaceController(
        enabled = !isAnyDialogOpen,
        defaultFocusRequester = if (currentTab == DashboardTab.HOME) homePrimaryFocusRequester else activeRailFocusRequester,
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
                DashboardTab.LIBRARY,
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
            val safePadH = (32.dp * scale).coerceIn(16.dp, 48.dp)
            val safePadV = (22.dp * scale).coerceIn(12.dp, 36.dp)
            val railGap = (24.dp * scale).coerceIn(14.dp, 36.dp)

            LaunchedEffect(currentTab) {
                if (currentTab == DashboardTab.HOME) {
                    runCatching { homePrimaryFocusRequester.requestFocus() }
                }
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
                        visitedTabs.forEach { tab ->
                            androidx.compose.runtime.key(tab) {
                                val isSelectedTab = (currentTab == tab)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .focusProperties { canFocus = isSelectedTab }
                                        .layout { measurable, constraints ->
                                            if (isSelectedTab) {
                                                val placeable = measurable.measure(constraints)
                                                layout(placeable.width, placeable.height) {
                                                    placeable.placeRelative(0, 0)
                                                }
                                            } else {
                                                layout(0, 0) {}
                                            }
                                        }
                                ) {
                                    when (tab) {
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
                                                            config.getEffectiveLocale()
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
                                                onOpenGuide = { section ->
                                                    guideInitialSection = section
                                                    showGuideDialog = true
                                                }
                                            )
                                        }
                                        DashboardTab.LIBRARY -> {
                                            GameLibraryScreen(
                                                games = allGames,
                                                onLaunchGame = {
                                                    onLaunchUrl(
                                                        SettingsRepository.formatXboxUrlWithLocale(
                                                            it,
                                                            config.getEffectiveLocale()
                                                        )
                                                    )
                                                },
                                                focusSearchTrigger = librarySearchFocusTick
                                            )
                                        }
                                        DashboardTab.SERVERS -> {
                                            ServersScreen(
                                                currentServer = vpnState.currentServer,
                                                onSelectServer = onSelectServerStable,
                                                isTabActive = isSelectedTab
                                            )
                                        }
                                        DashboardTab.SETTINGS -> {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalArrangement = Arrangement.spacedBy((10.dp * scale).coerceAtLeast(8.dp))
                                            ) {
                                                // Thanh chuyển nhanh Giao diện (Tự động / Điện thoại / TV) luôn hiện rõ trên TV
                                                val uiModes = AppUiMode.entries
                                                val selectedUiIdx = uiModes.indexOf(config.appUiMode).coerceAtLeast(0)
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape((16.dp * scale).coerceAtLeast(12.dp)))
                                                        .background(TvSurface)
                                                        .gamepadClickable(
                                                            cornerRadius = (16.dp * scale).coerceAtLeast(12.dp),
                                                            onStepHorizontal = { dir ->
                                                                val next = (selectedUiIdx + dir).coerceIn(0, uiModes.lastIndex)
                                                                SettingsRepository.updateAppUiMode(uiModes[next])
                                                            },
                                                            onClick = {
                                                                val next = (selectedUiIdx + 1) % uiModes.size
                                                                SettingsRepository.updateAppUiMode(uiModes[next])
                                                            }
                                                        )
                                                        .padding(
                                                            horizontal = (18.dp * scale).coerceAtLeast(12.dp),
                                                            vertical = (10.dp * scale).coerceAtLeast(8.dp)
                                                        )
                                                        .testTag("tv_settings_quick_ui_mode_row"),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = config.tr("Giao diện (Chế độ nâng cao)", "Interface (Advanced Mode)"),
                                                            color = TvInk,
                                                            fontFamily = SpaceGroteskFontFamily,
                                                            fontSize = (16.5f * scale).coerceAtLeast(13.5f).sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = config.tr(
                                                                "Đổi ngay giữa Tự động, Điện thoại và TV (D-Pad ◀ ▶ hoặc A)",
                                                                "Switch immediately between Auto, Phone, and TV"
                                                            ),
                                                            color = TvMuted,
                                                            fontSize = (13f * scale).coerceAtLeast(11f).sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width((12.dp * scale).coerceAtLeast(8.dp)))
                                                    TvSegmentedControl(
                                                        options = uiModes.map { config.tr(it.titleVi, it.titleEn) },
                                                        selectedIndex = selectedUiIdx,
                                                        onSelect = { idx ->
                                                            uiModes.getOrNull(idx)?.let { SettingsRepository.updateAppUiMode(it) }
                                                        },
                                                        scale = scale,
                                                        containerColor = TvSurface2
                                                    )
                                                }

                                                Box(modifier = Modifier.weight(1f)) {
                                                    SettingsScreen(
                                                        config = config,
                                                        vpnState = vpnState,
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
                                }
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
        val langFocusRequester = remember { FocusRequester() }
        GamepadSurfaceController(
            enabled = true,
            defaultFocusRequester = langFocusRequester,
            onBackPress = { showLanguageDialog = false }
        ) {
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                containerColor = TvSurface2,
                shape = RoundedCornerShape(22.dp),
                titleContentColor = TvInk,
                textContentColor = TvMuted,
                title = {
                    Text(
                        text = config.tr("Ngôn ngữ ứng dụng", "App Language"),
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLanguage.entries.forEachIndexed { idx, langOption ->
                            val isSelected = config.appLanguage == langOption
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) TvAccent else TvLine,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .gamepadClickable(
                                        cornerRadius = 12.dp,
                                        focusRequester = if (isSelected || idx == 0) langFocusRequester else null,
                                        onClick = {
                                            showLanguageDialog = false
                                            SettingsRepository.updateAppLanguage(langOption)
                                            Toast.makeText(
                                                context,
                                                "✓ ${langOption.flag} ${langOption.title}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    ),
                                color = if (isSelected) TvSel else TvSurface
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(text = langOption.flag, fontSize = 18.sp)
                                    Text(
                                        text = langOption.localizedTitle(isEnUi),
                                        color = if (isSelected) TvAccent else TvInk,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showLanguageDialog = false },
                        modifier = Modifier.gamepadClickable(cornerRadius = 10.dp) {
                            showLanguageDialog = false
                        }
                    ) {
                        Text(
                            text = config.tr("Đóng", "Close"),
                            color = TvAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
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
}
