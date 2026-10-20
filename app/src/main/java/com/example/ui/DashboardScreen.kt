package com.example.ui

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.components.HowToUseGuideDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cloudplay.GamepadDetector
import com.example.data.GameLibraryRepository
import com.example.data.SettingsRepository
import com.example.ui.tabs.GameLibraryScreen
import com.example.ui.tabs.HomeScreen
import com.example.ui.tabs.ServersScreen
import com.example.ui.tabs.SettingsScreen
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxDark
import com.example.ui.theme.XboxGlassBorder
import com.example.ui.theme.XboxGlassSurface
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant
import com.example.vpn.VpnEngine
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

    var currentTab by remember { mutableStateOf(DashboardTab.HOME) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var guideInitialSection by remember { mutableStateOf(com.example.ui.components.GuideSection.QUICK_START) }
    var showTranslatorDialog by remember { mutableStateOf(false) }

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            VpnManager.connect(context)
        } else {
            VpnManager.setEngine(VpnEngine.SMART_BYPASS)
            VpnManager.connect(context)
        }
    }

    fun handleConnect() {
        VpnManager.connect(context)
    }

    Scaffold(
        modifier = modifier.testTag("dashboard_screen"),
        containerColor = XboxDark,
        topBar = {
            Surface(
                color = XboxSurface.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, XboxCardBorder)
            ) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, XboxNeonGreen.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.cloudplay_launcher_icon_1791392788421),
                                    contentDescription = "CloudPlay Logo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                            Column {
                                Text(
                                    text = "CloudPlay",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Xbox Cloud Gaming",
                                    color = XboxNeonGreen,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                )
                            }
                        }
                    },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            // Live Ping Status Badge
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, DirectCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                                color = DirectCyan.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = "Ping",
                                        tint = DirectCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    val directPing = vpnState.directLatencyMs
                                    val hasPing = directPing != null && directPing > 0
                                    Text(
                                        text = if (hasPing) "${directPing}ms" else "-- ms",
                                        color = if (hasPing) DirectCyan else Color.Gray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            // Language badge with direct toggle & sync with system
                            val currentLangLabel = when (config.appLanguage) {
                                com.example.data.AppLanguage.SYSTEM -> {
                                    val sys = java.util.Locale.getDefault()
                                    if (sys.language.lowercase().startsWith("vi")) "VI (Auto)" else "EN (Auto)"
                                }
                                com.example.data.AppLanguage.VIETNAMESE -> "Tiếng Việt"
                                com.example.data.AppLanguage.ENGLISH -> "English"
                            }
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, XboxNeonGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
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
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Quick Smart Translator button
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, XboxNeonGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .clickable { showTranslatorDialog = true }
                                    .testTag("topbar_translator_button"),
                                color = XboxSurfaceVariant.copy(alpha = 0.9f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Translate,
                                        contentDescription = "Bộ Dịch Thông Minh",
                                        tint = XboxNeonGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Dịch xCloud",
                                        color = XboxNeonGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
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
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .border(
                        1.dp,
                        XboxGlassBorder,
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    ),
                color = XboxSurface.copy(alpha = 0.98f)
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == DashboardTab.HOME,
                        onClick = { currentTab = DashboardTab.HOME },
                        icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.tab_home)) },
                        label = { Text(stringResource(R.string.tab_home), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = XboxNeonGreen,
                            indicatorColor = XboxNeonGreen,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )

                    NavigationBarItem(
                        selected = currentTab == DashboardTab.LIBRARY,
                        onClick = { currentTab = DashboardTab.LIBRARY },
                        icon = { Icon(Icons.Default.SportsEsports, contentDescription = stringResource(R.string.tab_library)) },
                        label = { Text(stringResource(R.string.tab_library), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = XboxNeonGreen,
                            indicatorColor = XboxNeonGreen,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )

                    NavigationBarItem(
                        selected = currentTab == DashboardTab.SERVERS,
                        onClick = { currentTab = DashboardTab.SERVERS },
                        icon = { Icon(Icons.Default.Speed, contentDescription = stringResource(R.string.tab_servers)) },
                        label = { Text(stringResource(R.string.tab_servers), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = XboxNeonGreen,
                            indicatorColor = XboxNeonGreen,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )

                    NavigationBarItem(
                        selected = currentTab == DashboardTab.SETTINGS,
                        onClick = { currentTab = DashboardTab.SETTINGS },
                        icon = { Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.tab_settings)) },
                        label = { Text(stringResource(R.string.tab_settings), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = XboxNeonGreen,
                            indicatorColor = XboxNeonGreen,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        com.example.ui.components.CyberBackdrop(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
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
                                android.widget.Toast.makeText(context, "Đã xóa toàn bộ Cookie & Dữ liệu Xbox!", android.widget.Toast.LENGTH_SHORT).show()
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
