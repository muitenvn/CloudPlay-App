package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.provider.Settings
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.cloudplay.GamepadDetector
import com.example.data.GameLibraryRepository
import com.example.data.SettingsRepository
import com.example.ui.components.GuideSection
import com.example.ui.components.HowToUseGuideDialog
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
import com.example.ui.theme.XboxGlassBorder
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant
import com.example.ui.theme.animateXboxIntAsState
import com.example.ui.theme.rememberXboxMotionEnabled
import com.example.ui.theme.xboxPressScale
import com.example.ui.theme.xboxTabEnterTransition
import com.example.ui.theme.xboxTabExitTransition
import com.example.vpn.NetworkMode
import com.example.vpn.VpnManager

enum class DashboardTab {
    HOME,
    LIBRARY,
    SERVERS,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLaunchUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vpnState by VpnManager.state.collectAsState()
    val config by SettingsRepository.config.collectAsState()
    val allGames by GameLibraryRepository.games.collectAsState()

    val gamepadDetector = remember { GamepadDetector(context) }
    val gamepadStatus by gamepadDetector.status.collectAsState()

    DisposableEffect(Unit) {
        onDispose { gamepadDetector.release() }
    }

    var currentTab by rememberSaveable { mutableStateOf(DashboardTab.HOME) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var guideInitialSection by remember { mutableStateOf(GuideSection.QUICK_START) }
    var showTranslatorDialog by remember { mutableStateOf(false) }
    var showWebPortalDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val motionEnabled = rememberXboxMotionEnabled(
        lowEndDeviceMode = config.lowEndDeviceMode,
        gpuLoadReductionEnabled = config.gpuLoadReductionEnabled
    )
    val reduceMotion = !motionEnabled

    // Giữ các ảnh bìa nội bộ trong bộ nhớ đệm của DashboardScreen để khi chuyển qua lại giữa các tab
    // (Trang chủ ⇄ Game ⇄ Máy chủ ⇄ Cài đặt) luồng UI chính không phải giải mã lại Bitmap gây khựng.
    val prewarmedHeroBanner = painterResource(id = R.drawable.gamepass_hub_banner_1791394609676)
    val prewarmedFortnitePoster = painterResource(id = R.drawable.fortnite_cloud_poster_1791719536404)
    val prewarmedForzaCover = painterResource(id = R.drawable.forza_cover_art_1791438033687)
    val prewarmedCyberBanner = painterResource(id = R.drawable.cyber_gaming_banner_1791438050320)
    val prewarmedCloudPlayHero = painterResource(id = R.drawable.cloudplay_hero_banner_1791392804712)
    remember(
        prewarmedHeroBanner,
        prewarmedFortnitePoster,
        prewarmedForzaCover,
        prewarmedCyberBanner,
        prewarmedCloudPlayHero
    ) { true }

    fun handleConnect() {
        VpnManager.connect(context)
    }

    val dotPulse by animateFloatAsState(
        targetValue = if (vpnState.mode == NetworkMode.DISCONNECTED) 0.55f else 1f,
        animationSpec = tween(
            durationMillis = if (reduceMotion) 0 else 180,
            easing = FastOutSlowInEasing
        ),
        label = "header_status_alpha"
    )

    Scaffold(
        modifier = modifier.testTag("dashboard_screen"),
        containerColor = XboxDark,
        topBar = {
            Surface(
                color = XboxSurface.copy(alpha = 0.94f),
                border = androidx.compose.foundation.BorderStroke(
                    0.8.dp,
                    Brush.horizontalGradient(
                        listOf(
                            XboxNeonGreen.copy(alpha = 0.38f),
                            XboxCardBorder,
                            DirectCyan.copy(alpha = 0.38f)
                        )
                    )
                ),
                shadowElevation = 10.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                        title = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Console Emblem with Neon Rim
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            1.4.dp,
                                            Brush.linearGradient(
                                                listOf(XboxNeonGreen, DirectCyan)
                                            ),
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.cloudplay_launcher_icon_1791392788421),
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
                                        text = "CLOUDPLAY",
                                        color = TextPrimary,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.5.sp,
                                        letterSpacing = 0.4.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (vpnState.mode) {
                                                        NetworkMode.DIRECT_NETWORK -> DirectCyan.copy(alpha = dotPulse)
                                                        NetworkMode.VPN_JAPAN -> XboxNeonGreen.copy(alpha = dotPulse)
                                                        NetworkMode.DISCONNECTED -> TextSecondary
                                                    }
                                                )
                                        )
                                        Text(
                                            text = when (vpnState.mode) {
                                                NetworkMode.DIRECT_NETWORK -> stringResource(R.string.net_mode_direct)
                                                NetworkMode.VPN_JAPAN -> "${vpnState.currentServer.flag} ${vpnState.currentServer.name}"
                                                NetworkMode.DISCONNECTED -> "Xbox Cloud Console"
                                            },
                                            color = if (vpnState.mode == NetworkMode.DIRECT_NETWORK) DirectCyan else XboxNeonGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            val isEnUi = config.isEnglishUI()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                // 1. Live Telemetry Ping Pill (only when real ping is measured)
                                val directPing = vpnState.directLatencyMs
                                val hasPing = directPing != null && directPing > 0
                                if (hasPing) {
                                    val animatedPing by animateXboxIntAsState(
                                        targetValue = directPing,
                                        motionEnabled = motionEnabled,
                                        label = "topbar_ping_counter"
                                    )
                                    val targetPingTint = when {
                                        directPing < 55 -> DirectCyan
                                        directPing < 100 -> XboxNeonGreen
                                        else -> WarningAmber
                                    }
                                    val pingTint by animateColorAsState(
                                        targetValue = targetPingTint,
                                        animationSpec = tween(if (reduceMotion) 0 else 180),
                                        label = "topbar_ping_tint"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                            .xboxPressScale(enabled = motionEnabled, pressedScale = 0.94f)
                                            .clickable { currentTab = DashboardTab.SERVERS },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(9.dp))
                                                .border(1.dp, pingTint.copy(alpha = 0.45f), RoundedCornerShape(9.dp)),
                                            color = pingTint.copy(alpha = 0.10f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Speed,
                                                    contentDescription = "Ping",
                                                    tint = pingTint,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "${animatedPing}ms",
                                                    color = pingTint,
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }

                                // 2. Language Button (48dp touch target)
                                val currentLangLabel = when (config.appLanguage) {
                                    com.example.data.AppLanguage.SYSTEM -> {
                                        val sys = java.util.Locale.getDefault()
                                        if (sys.language.lowercase().startsWith("vi")) "VI" else "EN"
                                    }
                                    com.example.data.AppLanguage.VIETNAMESE -> "VI"
                                    com.example.data.AppLanguage.ENGLISH -> "EN"
                                }
                                Box(
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        .xboxPressScale(enabled = motionEnabled, pressedScale = 0.94f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            val nextLang = when (config.appLanguage) {
                                                com.example.data.AppLanguage.SYSTEM -> com.example.data.AppLanguage.VIETNAMESE
                                                com.example.data.AppLanguage.VIETNAMESE -> com.example.data.AppLanguage.ENGLISH
                                                com.example.data.AppLanguage.ENGLISH -> com.example.data.AppLanguage.SYSTEM
                                            }
                                            SettingsRepository.updateAppLanguage(nextLang)
                                        }
                                        .testTag("topbar_language_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .border(1.dp, XboxCardBorder, RoundedCornerShape(9.dp)),
                                        color = XboxSurfaceVariant.copy(alpha = 0.9f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Language,
                                                contentDescription = stringResource(R.string.current_language),
                                                tint = XboxNeonGreen,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = currentLangLabel,
                                                color = TextPrimary,
                                                fontFamily = SpaceGroteskFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                // 3. Translator Button (48dp touch target)
                                val isTranslateOn = config.isEffectiveTranslateEnabled()
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .xboxPressScale(enabled = motionEnabled, pressedScale = 0.94f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { showTranslatorDialog = true }
                                        .testTag("topbar_translator_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .border(
                                                1.dp,
                                                if (isTranslateOn) XboxNeonGreen.copy(alpha = 0.55f) else XboxCardBorder,
                                                RoundedCornerShape(9.dp)
                                            ),
                                        color = if (isTranslateOn) XboxNeonGreen.copy(alpha = 0.14f) else XboxSurfaceVariant.copy(alpha = 0.9f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Translate,
                                                contentDescription = if (isEnUi) "Auto Translate" else "Dịch tự động",
                                                tint = XboxNeonGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // 4. Overflow Menu ("⋯") for secondary items (Web Download Portal & User Guide)
                                Box(contentAlignment = Alignment.Center) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { showOverflowMenu = true }
                                            .testTag("topbar_overflow_menu_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(9.dp))
                                                .border(1.dp, XboxCardBorder, RoundedCornerShape(9.dp)),
                                            color = XboxSurfaceVariant.copy(alpha = 0.9f)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = if (isEnUi) "More options" else "Thêm tùy chọn",
                                                    tint = TextPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showOverflowMenu,
                                        onDismissRequest = { showOverflowMenu = false },
                                        modifier = Modifier
                                            .background(XboxSurface)
                                            .border(1.dp, XboxCardBorder, RoundedCornerShape(12.dp))
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = if (isEnUi) "Share & QR Code" else "Mã QR & Chia sẻ",
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
                                                showWebPortalDialog = true
                                            },
                                            modifier = Modifier.testTag("topbar_web_portal_button")
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = stringResource(R.string.guide_button),
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
                                                showGuideDialog = true
                                            },
                                            modifier = Modifier.testTag("topbar_guide_button")
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        },
        bottomBar = {
            ConsoleFloatingNavigationDock(
                currentTab = currentTab,
                gamesCount = allGames.size,
                isVpnActive = vpnState.mode != NetworkMode.DISCONNECTED,
                motionEnabled = motionEnabled,
                onSelectTab = { currentTab = it }
            )
        }
    ) { innerPadding ->
        com.example.ui.components.CyberBackdrop(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    if (reduceMotion) {
                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    } else {
                        val direction = targetState.ordinal.compareTo(initialState.ordinal)
                        xboxTabEnterTransition(direction) togetherWith xboxTabExitTransition(direction)
                    }
                },
                label = "tab_animation"
            ) { tab ->
                when (tab) {
                    DashboardTab.HOME -> {
                        HomeScreen(
                            vpnState = vpnState,
                            gamepadStatus = gamepadStatus,
                            featuredGames = allGames,
                            onConnectVpn = { handleConnect() },
                            onDisconnectVpn = { VpnManager.disconnect(context) },
                            onSwitchEngine = { VpnManager.setEngine(it) },
                            onLaunchGame = { onLaunchUrl(SettingsRepository.formatXboxUrlWithLocale(it, config.getEffectiveLocale())) },
                            onOpenXboxCloud = { onLaunchUrl(config.targetRegionUrl) },
                            onNavigateToLibrary = { currentTab = DashboardTab.LIBRARY },
                            onNavigateToServers = { currentTab = DashboardTab.SERVERS },
                            onNavigateToSettings = { currentTab = DashboardTab.SETTINGS },
                            onOpenGuide = { section ->
                                guideInitialSection = section
                                showGuideDialog = true
                            },
                            onOpenTranslator = {
                                showTranslatorDialog = true
                            }
                        )
                    }
                    DashboardTab.LIBRARY -> {
                        GameLibraryScreen(
                            games = allGames,
                            onLaunchGame = { onLaunchUrl(SettingsRepository.formatXboxUrlWithLocale(it, config.getEffectiveLocale())) }
                        )
                    }
                    DashboardTab.SERVERS -> {
                        ServersScreen(
                            currentServer = vpnState.currentServer,
                            onSelectServer = { VpnManager.selectServer(it, context) }
                        )
                    }
                    DashboardTab.SETTINGS -> {
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
                                val msg = if (config.isEnglishUI()) "Cleared saved login & web data!" else "Đã làm mới dữ liệu đăng nhập Xbox!"
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showGuideDialog) {
        HowToUseGuideDialog(
            initialSection = guideInitialSection,
            onDismiss = { showGuideDialog = false }
        )
    }

    if (showTranslatorDialog) {
        com.example.ui.components.SmartTranslatorDialog(
            onDismissRequest = { showTranslatorDialog = false }
        )
    }

    if (showWebPortalDialog) {
        com.example.ui.components.WebDownloadPortalDialog(
            onDismiss = { showWebPortalDialog = false }
        )
    }
}

@Composable
private fun ConsoleLiveTelemetryRibbon(
    config: com.example.data.AppConfig,
    hasPhysicalController: Boolean,
    onOpenSettings: () -> Unit
) {
    val isEn = config.isEnglishUI()
    val graphicsLabel = if (isEn) config.cellularMode.shortLabelEn else config.cellularMode.shortLabel
    val fpsCapLabel = "${config.streamFpsCap.fps} FPS"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xD907100B))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Ưu tiên 1: Chế độ Đồ họa (1 chạm để chuyển Tự động / Thấp / Vừa / Cao / Rất cao)
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    val modes = com.example.data.CellularOptimizationMode.entries
                    val nextIndex = (modes.indexOf(config.cellularMode) + 1) % modes.size
                    SettingsRepository.updateCellularMode(modes[nextIndex])
                }
                .testTag("ribbon_graphics_mode_chip"),
            shape = RoundedCornerShape(8.dp),
            color = XboxNeonGreen.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(
                0.7.dp,
                XboxNeonGreen.copy(alpha = 0.48f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "✨", fontSize = 9.sp)
                Text(
                    text = if (isEn) "Graphics: $graphicsLabel" else "Đồ họa: $graphicsLabel",
                    color = XboxNeonGreen,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
        }

        // 2. Ưu tiên 2: Nhịp khung hình (1 chạm để đổi 30 / 60 / 120 FPS)
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    val caps = com.example.data.StreamFpsCap.entries
                    val nextIndex = (caps.indexOf(config.streamFpsCap) + 1) % caps.size
                    SettingsRepository.updateStreamFpsCap(caps[nextIndex])
                }
                .testTag("ribbon_fps_cap_chip"),
            shape = RoundedCornerShape(8.dp),
            color = DirectCyan.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(
                0.7.dp,
                DirectCyan.copy(alpha = 0.45f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "⚡", fontSize = 9.sp)
                Text(
                    text = fpsCapLabel,
                    color = DirectCyan,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
        }

        // 3. Ưu tiên 3: Chế độ điều khiển (chạm mở Cài đặt)
        Surface(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onOpenSettings() }
                .testTag("ribbon_fps_input_chip"),
            shape = RoundedCornerShape(8.dp),
            color = XboxSurfaceVariant.copy(alpha = 0.75f),
            border = androidx.compose.foundation.BorderStroke(0.7.dp, XboxCardBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (hasPhysicalController) {
                        if (isEn) "🎮 Controller Ready" else "🎮 Tay cầm sẵn sàng"
                    } else {
                        if (isEn) "👆 Touch Controls" else "👆 Cảm ứng"
                    },
                    color = TextSecondary,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ConsoleFloatingNavigationDock(
    currentTab: DashboardTab,
    gamesCount: Int,
    isVpnActive: Boolean,
    motionEnabled: Boolean = true,
    onSelectTab: (DashboardTab) -> Unit
) {
    val navItems = listOf(
        Triple(DashboardTab.HOME, Icons.Default.Home, stringResource(R.string.tab_home)),
        Triple(DashboardTab.LIBRARY, Icons.Default.SportsEsports, stringResource(R.string.tab_library)),
        Triple(DashboardTab.SERVERS, Icons.Default.Speed, stringResource(R.string.tab_servers)),
        Triple(DashboardTab.SETTINGS, Icons.Default.Tune, stringResource(R.string.tab_settings))
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        XboxNeonGreen.copy(alpha = 0.48f),
                        XboxGlassBorder,
                        DirectCyan.copy(alpha = 0.48f)
                    )
                ),
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .testTag("bottom_navigation_bar"),
        color = XboxSurface.copy(alpha = 0.98f),
        shadowElevation = 18.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { (tab, icon, label) ->
                val isSelected = currentTab == tab
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.06f else 1f,
                    animationSpec = tween(
                        durationMillis = if (motionEnabled) 160 else 0,
                        easing = FastOutSlowInEasing
                    ),
                    label = "dock_item_scale"
                )
                val accentColor = if (tab == DashboardTab.SERVERS) DirectCyan else XboxNeonGreen
                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) accentColor else Color(0xFFC8DDD0),
                    animationSpec = tween(durationMillis = if (motionEnabled) 165 else 0),
                    label = "dock_icon_tint"
                )
                val indicatorWidth by animateDpAsState(
                    targetValue = if (isSelected) 22.dp else 0.dp,
                    animationSpec = tween(
                        durationMillis = if (motionEnabled) 165 else 0,
                        easing = FastOutSlowInEasing
                    ),
                    label = "dock_indicator_width"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .xboxPressScale(enabled = motionEnabled, pressedScale = 0.94f)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) {
                                Brush.verticalGradient(
                                    listOf(
                                        accentColor.copy(alpha = 0.24f),
                                        accentColor.copy(alpha = 0.07f)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                            }
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) accentColor.copy(alpha = 0.6f) else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelectTab(tab) }
                        .testTag("nav_tab_${tab.name.lowercase()}")
                        .padding(vertical = 8.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // Top glowing pill indicator
                        Box(
                            modifier = Modifier
                                .width(indicatorWidth)
                                .height(2.5.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) accentColor else Color.Transparent)
                        )

                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = iconTint,
                                modifier = Modifier.size(21.dp)
                            )
                            if (tab == DashboardTab.LIBRARY && gamesCount > 0) {
                                Surface(
                                    modifier = Modifier.padding(start = 16.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    color = DirectCyan.copy(alpha = 0.22f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.6.dp,
                                        DirectCyan.copy(alpha = 0.7f)
                                    )
                                ) {
                                    Text(
                                        text = if (gamesCount > 99) "99+" else "$gamesCount",
                                        color = DirectCyan,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 3.5.dp, vertical = 0.5.dp)
                                    )
                                }
                            } else if (tab == DashboardTab.SERVERS && isVpnActive) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 14.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(XboxNeonGreen)
                                )
                            }
                        }

                        Text(
                            text = label,
                            color = if (isSelected) TextPrimary else Color(0xFFD6EAE0),
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Architectural Note:
 * ConsoleLiveTelemetryRibbon and Dashboard HUD are strictly dedicated to core real-time
 * telemetry metrics:
 *  1. Graphics Mode & Cellular Optimization Profile (Auto / Low / Balanced / High / Ultra).
 *  2. Anti-squish Aspect Ratio (16:9 Fit, 18:9, Zoom, Full Stretch) for true visual proportion.
 *  3. Live Ping (ms) & Packet Jitter across cloud server endpoints.
 *  4. 60 FPS Target & Controller Connection Status (Xbox Gamepad / Virtual Touch).
 *
 * Secondary features such as screen recording, clip export, and local media captures have been
 * intentionally decoupled from the top HUD and Home screen. They reside exclusively in their
 * own dedicated, secondary section in Settings (Nhóm 5: Tính năng phụ) to guarantee a zero-distraction,
 * maximum-performance gameplay experience.
 */


