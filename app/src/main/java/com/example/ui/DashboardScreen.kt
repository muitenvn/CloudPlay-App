package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.cloudplay.GamepadDetector
import com.example.data.GameLibraryRepository
import com.example.data.SettingsRepository
import com.example.ui.components.GamepadSurfaceController
import com.example.ui.components.GuideSection
import com.example.ui.components.HowToUseGuideDialog
import com.example.ui.components.gamepadClickable
import com.example.ui.tabs.GameLibraryScreen
import com.example.ui.tabs.HomeScreen
import com.example.ui.tabs.ServersScreen
import com.example.ui.tabs.SettingsScreen
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxDark
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant
import com.example.vpn.NetworkMode
import com.example.vpn.VpnManager

enum class DashboardTab {
    HOME,
    LIBRARY,
    SERVERS,
    SETTINGS
}

private val TOP_LOGO_SHAPE = RoundedCornerShape(8.dp)
private val TOP_PILL_SHAPE = RoundedCornerShape(8.dp)
private val NAV_TAB_SHAPE = RoundedCornerShape(12.dp)
private val NAV_BADGE_SHAPE = RoundedCornerShape(5.dp)

@Composable
fun DashboardScreen(
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

    val gamepadDetector = remember { GamepadDetector(context) }
    val gamepadStatus by gamepadDetector.status.collectAsState()

    DisposableEffect(Unit) {
        onDispose { gamepadDetector.release() }
    }

    var currentTab by rememberSaveable { mutableStateOf(DashboardTab.HOME) }
    val visitedTabs = remember { androidx.compose.runtime.mutableStateListOf(currentTab) }
    if (currentTab !in visitedTabs) {
        visitedTabs.add(currentTab)
    }
    val topBarLogoRequest = remember(context) {
        coil.request.ImageRequest.Builder(context)
            .data(R.drawable.cloudplay_launcher_icon_1791392788421)
            .size(96, 96)
            .allowRgb565(true)
            .crossfade(false)
            .build()
    }
    var showGuideDialog by remember { mutableStateOf(false) }
    var guideInitialSection by remember { mutableStateOf(GuideSection.QUICK_START) }
    var showTranslatorDialog by remember { mutableStateOf(false) }
    var showWebPortalDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    fun switchTab(newTab: DashboardTab, reason: String = "") {
        if (currentTab != newTab) {
            val nextScenario = when (newTab) {
                DashboardTab.HOME -> com.example.data.PerfScenario.HOME
                DashboardTab.LIBRARY -> com.example.data.PerfScenario.LIBRARY
                DashboardTab.SERVERS -> com.example.data.PerfScenario.SERVERS
                DashboardTab.SETTINGS -> com.example.data.PerfScenario.SETTINGS
            }
            com.example.data.PerformanceMonitor.setScenario(nextScenario)
            val suffix = if (reason.isNotBlank()) " ($reason)" else ""
            com.example.data.AppLogger.info("UI", "Chuyển tab chính: ${currentTab.name} -> ${newTab.name}$suffix")
            if (newTab !in visitedTabs) {
                visitedTabs.add(newTab)
            }
            currentTab = newTab
        }
    }

    fun handleConnect() {
        VpnManager.connect(context)
    }

    val activeTabFocusRequester = remember { FocusRequester() }
    var librarySearchFocusTick by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val isAnyDialogOpen = showGuideDialog || showTranslatorDialog || showWebPortalDialog || showOverflowMenu || showLanguageMenu

    GamepadSurfaceController(
        enabled = !isAnyDialogOpen,
        defaultFocusRequester = activeTabFocusRequester,
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
            when {
                showLanguageMenu -> showLanguageMenu = false
                showOverflowMenu -> showOverflowMenu = false
                currentTab != DashboardTab.HOME -> switchTab(DashboardTab.HOME, "nút B")
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
                        com.example.data.AppLogger.info("UI", "Bấm phím X tay cầm: Đo lại ping trên tab Máy chủ")
                        VpnManager.testAllServers(
                            autoPickBest = false,
                            context = context,
                            forceRefresh = true,
                            reason = "phím X tay cầm trên tab Máy chủ"
                        )
                    }
                }
                DashboardTab.SETTINGS -> {}
            }
        },
        onSecondaryY = {
            when (currentTab) {
                DashboardTab.HOME -> {
                    com.example.data.AppLogger.info("UI", "Bấm phím Y tay cầm: Mở trình duyệt Xbox Cloud từ Trang chủ")
                    onLaunchUrl(config.targetRegionUrl)
                }
                DashboardTab.LIBRARY,
                DashboardTab.SERVERS,
                DashboardTab.SETTINGS -> {}
            }
        },
        modifier = modifier
    ) {
        Scaffold(
            modifier = Modifier.testTag("dashboard_screen"),
            containerColor = XboxDark,
        topBar = {
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(XboxSurface)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
                val effectiveWidthDp = maxWidth.value / fontScale
                val isCompactTopBar = maxWidth < 360.dp || effectiveWidthDp < 340f

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 46.dp)
                            .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(TOP_LOGO_SHAPE)
                                    .background(Color(0xFF0F2C1D))
                                    .border(1.dp, XboxCardBorder, TOP_LOGO_SHAPE),
                                contentAlignment = Alignment.Center
                            ) {
                                coil.compose.AsyncImage(
                                    model = topBarLogoRequest,
                                    contentDescription = "CloudPlay Logo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Text(
                                    text = "CloudPlay",
                                    color = TextPrimary,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    letterSpacing = 0.1.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val statusColor = when (vpnState.mode) {
                                        NetworkMode.DIRECT_NETWORK -> DirectCyan
                                        NetworkMode.VPN_JAPAN -> XboxNeonGreen
                                        NetworkMode.DISCONNECTED -> XboxNeonGreen
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                    )
                                    Text(
                                        text = when (vpnState.mode) {
                                            NetworkMode.DIRECT_NETWORK -> config.tr("Mạng trực tiếp", "Direct Network")
                                            NetworkMode.VPN_JAPAN -> "${vpnState.currentServer.flag} ${vpnState.currentServer.name}"
                                            NetworkMode.DISCONNECTED -> config.tr("Sẵn sàng", "Ready")
                                        },
                                        color = TextSecondary,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        val isEnUi = config.isEnglishUI()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Language Switcher
                            val currentLangLabel = config.appLanguage.effectiveShortBadge()
                            Box(contentAlignment = Alignment.Center) {
                                Row(
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 42.dp, minHeight = 42.dp)
                                        .clip(TOP_PILL_SHAPE)
                                        .background(Color.Transparent, TOP_PILL_SHAPE)
                                        .gamepadClickable(cornerRadius = 8.dp) { showLanguageMenu = true }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("topbar_language_button"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = currentLangLabel,
                                        color = TextPrimary,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                DropdownMenu(
                                    expanded = showLanguageMenu,
                                    onDismissRequest = { showLanguageMenu = false },
                                    modifier = Modifier
                                        .background(XboxSurfaceVariant)
                                        .border(1.dp, XboxCardBorder, RoundedCornerShape(10.dp))
                                ) {
                                    com.example.data.AppLanguage.entries.forEach { langOption ->
                                        val isSelected = config.appLanguage == langOption
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(text = langOption.flag, fontSize = 15.sp)
                                                    Text(
                                                        text = langOption.localizedTitle(isEnUi),
                                                        color = if (isSelected) XboxNeonGreen else TextPrimary,
                                                        fontFamily = SpaceGroteskFontFamily,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                                    )
                                                }
                                            },
                                            onClick = {
                                                showLanguageMenu = false
                                                SettingsRepository.updateAppLanguage(langOption)
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "✓ ${langOption.flag} ${langOption.title}",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            modifier = Modifier.testTag("topbar_lang_item_${langOption.code}")
                                        )
                                    }
                                }
                            }

                            // Translator Button
                            val isTranslateOn = config.isEffectiveTranslateEnabled()
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(TOP_PILL_SHAPE)
                                    .background(
                                        if (isTranslateOn) Color(0xFF16352A) else Color.Transparent,
                                        TOP_PILL_SHAPE
                                    )
                                    .gamepadClickable(cornerRadius = 8.dp) {
                                        com.example.data.AppLogger.info("UI", "Mở hộp thoại Dịch tự động từ thanh đầu")
                                        showTranslatorDialog = true
                                    }
                                    .testTag("topbar_translator_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = config.tr("Dịch tự động", "Auto Translate"),
                                    tint = if (isTranslateOn) XboxNeonGreen else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Overflow Menu ("⋮")
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(TOP_PILL_SHAPE)
                                        .background(Color.Transparent, TOP_PILL_SHAPE)
                                        .gamepadClickable(cornerRadius = 8.dp) { showOverflowMenu = true }
                                        .testTag("topbar_overflow_menu_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = config.tr("Thêm tùy chọn", "More options"),
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false },
                                    modifier = Modifier
                                        .background(XboxSurfaceVariant)
                                        .border(1.dp, XboxCardBorder, RoundedCornerShape(10.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = config.tr("Mã QR & Chia sẻ", "Share & QR Code"),
                                                color = TextPrimary,
                                                fontFamily = SpaceGroteskFontFamily,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Public,
                                                contentDescription = null,
                                                tint = XboxNeonGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            com.example.data.AppLogger.info("UI", "Mở hộp thoại Mã QR & Chia sẻ")
                                            showWebPortalDialog = true
                                        },
                                        modifier = Modifier.testTag("topbar_web_portal_button")
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = config.tr("Hướng dẫn", "Guide"),
                                                color = TextPrimary,
                                                fontFamily = SpaceGroteskFontFamily,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                                contentDescription = null,
                                                tint = DirectCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            guideInitialSection = GuideSection.QUICK_START
                                            com.example.data.AppLogger.info("UI", "Mở hộp thoại Hướng dẫn sử dụng (${guideInitialSection.name})")
                                            showGuideDialog = true
                                        },
                                        modifier = Modifier.testTag("topbar_guide_button")
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(XboxCardBorder)
                    )
                }
            }
        },
        bottomBar = {
            ConsoleFloatingNavigationDock(
                currentTab = currentTab,
                gamesCount = allGames.size,
                isVpnActive = vpnState.mode != NetworkMode.DISCONNECTED,
                settingsTabLabel = config.tr("Cài đặt", "Settings"),
                activeTabFocusRequester = activeTabFocusRequester,
                onSelectTab = { switchTab(it) }
            )
        }
    ) { innerPadding ->
        com.example.ui.components.CyberBackdrop(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val activePerfScenario = when (currentTab) {
                DashboardTab.HOME -> com.example.data.PerfScenario.HOME
                DashboardTab.LIBRARY -> com.example.data.PerfScenario.LIBRARY
                DashboardTab.SERVERS -> com.example.data.PerfScenario.SERVERS
                DashboardTab.SETTINGS -> com.example.data.PerfScenario.SETTINGS
            }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                com.example.data.PerformanceMonitor.setScenario(activePerfScenario)
            }
            androidx.compose.runtime.SideEffect {
                com.example.data.PerformanceMonitor.recordRecomposition(activePerfScenario)
            }

            val onSelectServerStable = remember(context) {
                { srv: com.example.vpn.ServerLocation -> VpnManager.selectServer(srv, context) }
            }
            Box(modifier = Modifier.fillMaxSize()) {
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
                            TrackFirstTabComposition(tab) {
                                when (tab) {
                                    DashboardTab.HOME -> {
                                        HomeScreen(
                                            vpnState = vpnState,
                                            gamepadStatus = gamepadStatus,
                                            featuredGames = allGames,
                                            onConnectVpn = { handleConnect() },
                                            onDisconnectVpn = { VpnManager.disconnect(context) },
                                            onSwitchEngine = { VpnManager.setEngine(it) },
                                            onLaunchGame = {
                                                onLaunchUrl(
                                                    SettingsRepository.formatXboxUrlWithLocale(
                                                        it,
                                                        config.getEffectiveLocale(),
                                                        com.example.data.XboxWebUiMode.CLASSIC
                                                    )
                                                )
                                            },
                                            onOpenXboxCloud = { onLaunchUrl(config.targetRegionUrl) },
                                            onNavigateToLibrary = { switchTab(DashboardTab.LIBRARY, "từ Trang chủ") },
                                            onNavigateToServers = { switchTab(DashboardTab.SERVERS, "từ Trang chủ") },
                                            onNavigateToSettings = { switchTab(DashboardTab.SETTINGS, "từ Trang chủ") },
                                            onOpenGuide = { section ->
                                                guideInitialSection = section
                                                com.example.data.AppLogger.info("UI", "Mở hộp thoại Hướng dẫn sử dụng (${section.name})")
                                                showGuideDialog = true
                                            },
                                            onOpenTranslator = {
                                                com.example.data.AppLogger.info("UI", "Mở hộp thoại Dịch tự động từ Trang chủ")
                                                showTranslatorDialog = true
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
                                                        config.getEffectiveLocale(),
                                                        com.example.data.XboxWebUiMode.CLASSIC
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
                                        SettingsScreen(
                                            config = config,
                                            vpnState = vpnState,
                                            onOpenGuide = { section ->
                                                guideInitialSection = section
                                                com.example.data.AppLogger.info("UI", "Mở hộp thoại Hướng dẫn sử dụng (${section.name})")
                                                showGuideDialog = true
                                            },
                                            onClearCookies = {
                                                com.example.data.AppLogger.info(
                                                    "RESET",
                                                    "Xóa toàn bộ Cookie & WebStorage của WebView (lý do: người dùng bấm 'Làm mới dữ liệu đăng nhập' trong Cài đặt | chế độ=${config.xboxWebUiMode.name} | lưu ý: xóa cookie sẽ làm mất trạng thái opt-out preview lưu trên trình duyệt)"
                                                )
                                                android.webkit.CookieManager.getInstance().removeAllCookies(null)
                                                android.webkit.WebStorage.getInstance().deleteAllData()
                                                val msg = if (config.isSettingsEnglishUI()) "Cleared saved login & web data!" else "Đã làm mới dữ liệu đăng nhập Xbox!"
                                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
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
    }

    if (showGuideDialog) {
        HowToUseGuideDialog(
            initialSection = guideInitialSection,
            onDismiss = {
                com.example.data.AppLogger.info("UI", "Đóng hộp thoại Hướng dẫn sử dụng")
                showGuideDialog = false
            }
        )
    }

    if (showTranslatorDialog) {
        com.example.ui.components.SmartTranslatorDialog(
            onDismissRequest = {
                com.example.data.AppLogger.info("UI", "Đóng hộp thoại Dịch tự động")
                showTranslatorDialog = false
            }
        )
    }

    if (showWebPortalDialog) {
        com.example.ui.components.WebDownloadPortalDialog(
            onDismiss = {
                com.example.data.AppLogger.info("UI", "Đóng hộp thoại Mã QR & Chia sẻ")
                showWebPortalDialog = false
            }
        )
    }
}

@Composable
private fun ConsoleFloatingNavigationDock(
    currentTab: DashboardTab,
    gamesCount: Int,
    isVpnActive: Boolean,
    settingsTabLabel: String = "Cài đặt",
    activeTabFocusRequester: FocusRequester? = null,
    onSelectTab: (DashboardTab) -> Unit
) {
    val config by SettingsRepository.config.collectAsState()
    val navItems = remember(config.appLanguage, settingsTabLabel) {
        listOf(
            Triple(DashboardTab.HOME, Icons.Default.Home, config.tr("Trang chủ", "Home")),
            Triple(DashboardTab.LIBRARY, Icons.Default.SportsEsports, config.tr("Thư viện", "Library")),
            Triple(DashboardTab.SERVERS, Icons.Default.Speed, config.tr("Máy chủ", "Servers")),
            Triple(DashboardTab.SETTINGS, Icons.Default.Tune, settingsTabLabel)
        )
    }

    val navBarBg = XboxSurface
    val navBarLine = XboxCardBorder
    val navBadgeBg = Color(0xFF16352A)
    val navAccent = XboxNeonGreen
    val navMuted = TextSecondary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(navBarBg)
            .testTag("bottom_navigation_bar")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(navBarLine)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { (tab, icon, label) ->
                val isSelected = currentTab == tab

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 56.dp)
                        .clip(NAV_TAB_SHAPE)
                        .gamepadClickable(
                            cornerRadius = 12.dp,
                            focusRequester = if (isSelected) activeTabFocusRequester else null,
                            onClick = { onSelectTab(tab) }
                        )
                        .testTag("nav_tab_${tab.name.lowercase()}")
                        .padding(vertical = 6.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .width(22.dp)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(navAccent)
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) navAccent else navMuted,
                                modifier = Modifier.size(22.dp)
                            )
                            if (tab == DashboardTab.LIBRARY && gamesCount > 0) {
                                Text(
                                    text = if (gamesCount > 99) "99+" else "$gamesCount",
                                    color = navAccent,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    lineHeight = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(navBadgeBg, CircleShape)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            } else if (tab == DashboardTab.SERVERS && isVpnActive) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(end = 12.dp, top = 1.dp)
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(navAccent)
                                )
                            }
                        }

                        Text(
                            text = label,
                            color = if (isSelected) navAccent else navMuted,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackFirstTabComposition(
    tab: DashboardTab,
    content: @Composable () -> Unit
) {
    val scenario = remember(tab) {
        when (tab) {
            DashboardTab.HOME -> com.example.data.PerfScenario.HOME
            DashboardTab.LIBRARY -> com.example.data.PerfScenario.LIBRARY
            DashboardTab.SERVERS -> com.example.data.PerfScenario.SERVERS
            DashboardTab.SETTINGS -> com.example.data.PerfScenario.SETTINGS
        }
    }
    val composeStartMs = remember(tab) { android.os.SystemClock.elapsedRealtime() }
    val phaseTimingsRef = remember(tab) { longArrayOf(-1L, -1L, -1L) }
    androidx.compose.runtime.SideEffect {
        if (phaseTimingsRef[0] < 0L) {
            phaseTimingsRef[0] =
                (android.os.SystemClock.elapsedRealtime() - composeStartMs).coerceAtLeast(0L)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                if (phaseTimingsRef[1] < 0L) {
                    phaseTimingsRef[1] =
                        (android.os.SystemClock.elapsedRealtime() - composeStartMs).coerceAtLeast(0L)
                }
            }
            .drawWithContent {
                drawContent()
                if (phaseTimingsRef[2] < 0L) {
                    phaseTimingsRef[2] =
                        (android.os.SystemClock.elapsedRealtime() - composeStartMs).coerceAtLeast(0L)
                }
            }
    ) {
        content()
    }
    androidx.compose.runtime.LaunchedEffect(tab) {
        androidx.compose.runtime.withFrameNanos { }
        val firstFrameMs =
            (android.os.SystemClock.elapsedRealtime() - composeStartMs).coerceAtLeast(0L)
        val commitMs =
            if (phaseTimingsRef[0] >= 0L) phaseTimingsRef[0] else firstFrameMs
        val layoutMs =
            if (phaseTimingsRef[1] >= 0L) phaseTimingsRef[1] else commitMs
        val drawMs =
            if (phaseTimingsRef[2] >= 0L) phaseTimingsRef[2] else layoutMs
        com.example.data.PerformanceMonitor.recordFirstTabCompositionIfNeeded(
            scenario = scenario,
            composeCommitMs = commitMs,
            layoutDoneMs = layoutMs,
            drawDoneMs = drawMs,
            firstFrameMs = firstFrameMs
        )
    }
}
