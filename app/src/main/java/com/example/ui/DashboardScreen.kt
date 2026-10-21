package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    fun handleConnect() {
        VpnManager.connect(context)
    }

    val dotPulse = if (config.lowEndDeviceMode) {
        0.9f
    } else {
        val pulseTransition = rememberInfiniteTransition(label = "header_pulse")
        val animatedDot by pulseTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_pulse"
        )
        animatedDot
    }

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
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Console Emblem with Neon Rim
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        1.2.dp,
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
                                    fontSize = 16.sp,
                                    letterSpacing = 0.6.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                                            NetworkMode.DISCONNECTED -> "Xbox Cloud"
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(end = 10.dp)
                        ) {
                            // 1. Live Telemetry Ping Pill (only when real ping is measured)
                            val directPing = vpnState.directLatencyMs
                            val hasPing = directPing != null && directPing > 0
                            if (hasPing) {
                                val pingTint = when {
                                    directPing < 55 -> DirectCyan
                                    directPing < 100 -> XboxNeonGreen
                                    else -> WarningAmber
                                }
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
                                            text = "${directPing}ms",
                                            color = pingTint,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // 2. Language Icon Button (short 2-char code)
                            val currentLangLabel = when (config.appLanguage) {
                                com.example.data.AppLanguage.SYSTEM -> {
                                    val sys = java.util.Locale.getDefault()
                                    if (sys.language.lowercase().startsWith("vi")) "VI" else "EN"
                                }
                                com.example.data.AppLanguage.VIETNAMESE -> "VI"
                                com.example.data.AppLanguage.ENGLISH -> "EN"
                            }
                            Surface(
                                modifier = Modifier
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .border(1.dp, XboxCardBorder, RoundedCornerShape(9.dp))
                                    .clickable {
                                        val nextLang = when (config.appLanguage) {
                                            com.example.data.AppLanguage.SYSTEM -> com.example.data.AppLanguage.VIETNAMESE
                                            com.example.data.AppLanguage.VIETNAMESE -> com.example.data.AppLanguage.ENGLISH
                                            com.example.data.AppLanguage.ENGLISH -> com.example.data.AppLanguage.SYSTEM
                                        }
                                        SettingsRepository.updateAppLanguage(nextLang)
                                    }
                                    .testTag("topbar_language_button"),
                                color = XboxSurfaceVariant.copy(alpha = 0.9f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = stringResource(R.string.current_language),
                                        tint = XboxNeonGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = currentLangLabel,
                                        color = TextPrimary,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            // 3. Translator Icon Button
                            val isEnUi = config.isEnglishUI()
                            val isTranslateOn = config.isEffectiveTranslateEnabled()
                            Surface(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .border(
                                        1.dp,
                                        if (isTranslateOn) XboxNeonGreen.copy(alpha = 0.55f) else XboxCardBorder,
                                        RoundedCornerShape(9.dp)
                                    )
                                    .clickable { showTranslatorDialog = true }
                                    .testTag("topbar_translator_button"),
                                color = if (isTranslateOn) XboxNeonGreen.copy(alpha = 0.14f) else XboxSurfaceVariant.copy(alpha = 0.9f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Translate,
                                        contentDescription = if (isEnUi) "Auto Translate" else "Dịch tự động",
                                        tint = XboxNeonGreen,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }

                            // 4. Quick User Guide Icon Button
                            Surface(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .border(1.dp, XboxCardBorder, RoundedCornerShape(9.dp))
                                    .clickable {
                                        guideInitialSection = GuideSection.QUICK_START
                                        showGuideDialog = true
                                    }
                                    .testTag("topbar_guide_button"),
                                color = XboxSurfaceVariant.copy(alpha = 0.85f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                        contentDescription = stringResource(R.string.guide_button),
                                        tint = DirectCyan,
                                        modifier = Modifier.size(15.dp)
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
        },
        bottomBar = {
            ConsoleFloatingNavigationDock(
                currentTab = currentTab,
                gamesCount = allGames.size,
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
                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.985f, animationSpec = tween(220))) togetherWith
                        fadeOut(tween(150))
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
                                val msg = if (config.isEnglishUI()) "Cleared all Xbox cookies & site data!" else "Đã xóa toàn bộ Cookie & Dữ liệu Xbox!"
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
}

@Composable
private fun ConsoleFloatingNavigationDock(
    currentTab: DashboardTab,
    gamesCount: Int,
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
                        XboxNeonGreen.copy(alpha = 0.42f),
                        XboxGlassBorder,
                        DirectCyan.copy(alpha = 0.42f)
                    )
                ),
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .testTag("bottom_navigation_bar"),
        color = XboxSurface.copy(alpha = 0.97f),
        shadowElevation = 16.dp
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
                    targetValue = if (isSelected) 1.04f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "dock_item_scale"
                )
                val accentColor = if (tab == DashboardTab.SERVERS) DirectCyan else XboxNeonGreen

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .scale(scale)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) {
                                Brush.verticalGradient(
                                    listOf(
                                        accentColor.copy(alpha = 0.22f),
                                        accentColor.copy(alpha = 0.06f)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                            }
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) accentColor.copy(alpha = 0.55f) else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelectTab(tab) }
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
                                .width(if (isSelected) 20.dp else 0.dp)
                                .height(2.5.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) accentColor else Color.Transparent)
                        )

                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) accentColor else Color(0xFFC8DDD0),
                                modifier = Modifier.size(21.dp)
                            )
                            if (tab == DashboardTab.LIBRARY && gamesCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 14.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(DirectCyan)
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
