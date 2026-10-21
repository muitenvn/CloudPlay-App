package com.example.ui.tabs

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.cloudplay.GamepadStatus
import com.example.data.GameBookmark
import com.example.data.SettingsRepository
import com.example.ui.components.GuideSection
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.EpicBlue
import com.example.ui.theme.GoldStar
import com.example.ui.theme.JapanRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxDark
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceElevated
import com.example.ui.theme.XboxSurfaceVariant
import com.example.vpn.NetworkMode
import com.example.vpn.VpnConnectionState
import com.example.vpn.VpnEngine

@Composable
fun HomeScreen(
    vpnState: VpnConnectionState,
    gamepadStatus: GamepadStatus,
    featuredGames: List<GameBookmark>,
    onConnectVpn: () -> Unit,
    onDisconnectVpn: () -> Unit,
    onSwitchEngine: (VpnEngine) -> Unit,
    onLaunchGame: (String) -> Unit,
    onOpenXboxCloud: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onOpenGuide: (GuideSection) -> Unit = {},
    onOpenTranslator: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Quick Play Deck 2.0 (Next-Gen Xbox Series X / ROG Console Showcase)
        HeroConsolePlayDeck(
            vpnState = vpnState,
            gamepadStatus = gamepadStatus,
            onOpenXboxCloud = onOpenXboxCloud,
            onLaunchFortnite = {
                onLaunchGame("https://www.xbox.com/en-US/play/games/fortnite/BT5P2X999VH2")
            },
            onOpenGuide = { onOpenGuide(GuideSection.QUICK_START) }
        )

        // 2. Tactical Hardware & Network Cockpit 2.0 (2-Stage Pipeline + Engine Selector + Controller Telemetry)
        QuickNetworkCockpitCard(
            vpnState = vpnState,
            gamepadStatus = gamepadStatus,
            onConnectVpn = onConnectVpn,
            onDisconnectVpn = onDisconnectVpn,
            onSwitchEngine = onSwitchEngine
        )

        // 3. Smart Gaming Translator Deck 2.0 (Live Preview + Immunity Shield Badges)
        SmartTranslatorHomeCard(
            onOpenTranslator = onOpenTranslator
        )

        // 4. Featured Games Vault Carousel (1-Tap Cloud Stream Launch)
        FeaturedGamesSection(
            featuredGames = featuredGames,
            onLaunchGame = onLaunchGame,
            onNavigateToLibrary = onNavigateToLibrary
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun HeroConsolePlayDeck(
    vpnState: VpnConnectionState,
    gamepadStatus: GamepadStatus,
    onOpenXboxCloud: () -> Unit,
    onLaunchFortnite: () -> Unit,
    onOpenGuide: () -> Unit
) {
    val config by SettingsRepository.config.collectAsState()
    val infiniteTransition = rememberInfiniteTransition(label = "hero_glow")
    val borderGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_glow_alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        XboxNeonGreen.copy(alpha = borderGlowAlpha),
                        DirectCyan.copy(alpha = 0.6f),
                        XboxNeonGreen.copy(alpha = borderGlowAlpha)
                    )
                ),
                RoundedCornerShape(24.dp)
            )
            .testTag("hero_console_play_deck"),
        colors = CardDefaults.cardColors(containerColor = XboxSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background Art: AAA Xbox Cloud artwork
            Image(
                painter = painterResource(id = R.drawable.gamepass_hub_banner_1791394609676),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(274.dp)
            )

            // Bottom-up Dark Gradient Overlay so text at bottom is crisp and doesn't clash with upper banner art
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(274.dp)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0x1A040806),
                                0.30f to Color(0x99050A07),
                                0.55f to Color(0xEB060B08),
                                0.82f to Color(0xFA070D0A),
                                1.0f to XboxSurface
                            )
                        )
                    )
            )

            // Subtle Radial Emerald Spotlight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(274.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                XboxNeonGreen.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            radius = 520f
                        )
                    )
            )

            // Content Layer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Status Ribbon (Clean, no fake FPS badge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xEB09120D),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when (vpnState.mode) {
                                NetworkMode.DIRECT_NETWORK -> DirectCyan.copy(alpha = 0.65f)
                                NetworkMode.VPN_JAPAN -> XboxNeonGreen.copy(alpha = 0.65f)
                                NetworkMode.DISCONNECTED -> XboxCardBorder
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (vpnState.mode) {
                                            NetworkMode.DIRECT_NETWORK -> DirectCyan
                                            NetworkMode.VPN_JAPAN -> XboxNeonGreen
                                            NetworkMode.DISCONNECTED -> WarningAmber
                                        }
                                    )
                            )
                            Text(
                                text = when (vpnState.mode) {
                                    NetworkMode.DIRECT_NETWORK -> "Mạng trực tiếp"
                                    NetworkMode.VPN_JAPAN -> "Mở vùng ${vpnState.currentServer.flag}"
                                    NetworkMode.DISCONNECTED -> "Tự ngắt VPN"
                                },
                                color = TextPrimary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0xD90B150F),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, DirectCyan.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable { onOpenGuide() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = stringResource(R.string.guide_button),
                                tint = DirectCyan,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // Spacer pushes headline down onto the dark bottom gradient away from upper image art
                Spacer(modifier = Modifier.height(22.dp))

                // Hero Headline & Single Short Subtitle Sentence
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = stringResource(R.string.hero_title),
                        color = TextPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.3).sp,
                        maxLines = 1
                    )

                    Text(
                        text = stringResource(R.string.hero_subtitle),
                        color = Color(0xFFD6EAE0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                // Dual Launch Buttons (Short 2-word labels, never truncated with ellipsis)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Primary Launch Button: Xbox Cloud Gaming
                    Button(
                        onClick = onOpenXboxCloud,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("hero_launch_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = XboxGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(XboxNeonGreen.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = stringResource(R.string.btn_launch_xcloud),
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Xbox Cloud",
                                    color = XboxNeonGreen,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Secondary Launch Button: Fortnite Free
                    Button(
                        onClick = onLaunchFortnite,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("hero_launch_fortnite_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EpicBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = GoldStar,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = stringResource(R.string.btn_play_fortnite),
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Miễn phí",
                                    color = Color(0xFFB3E5FC),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Active Capabilities Strip (Short 2-word labels so nothing gets cut off)
                val regionCode = config.getEffectiveLocale().takeLast(2).uppercase()
                val graphicsShort = when (config.cellularMode) {
                    com.example.data.CellularOptimizationMode.AUTO -> "Tự động"
                    com.example.data.CellularOptimizationMode.DATA_SAVER_HIGH_SPEED -> "Thấp"
                    com.example.data.CellularOptimizationMode.MEDIUM -> "Vừa"
                    com.example.data.CellularOptimizationMode.HIGH -> "Cao"
                    com.example.data.CellularOptimizationMode.MAX_SPEED_UNCAPPED -> "Rất cao"
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HeroCapabilityChip(
                        icon = "🌐",
                        label = "Vùng $regionCode",
                        accentColor = XboxNeonGreen,
                        modifier = Modifier.weight(1f)
                    )
                    HeroCapabilityChip(
                        icon = "✨",
                        label = "Đồ họa: $graphicsShort",
                        accentColor = DirectCyan,
                        modifier = Modifier.weight(1.15f)
                    )
                    HeroCapabilityChip(
                        icon = "🎮",
                        label = if (gamepadStatus.hasPhysicalController) "Tay cầm" else "Cảm ứng",
                        accentColor = if (gamepadStatus.hasPhysicalController) XboxNeonGreen else GoldStar,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCapabilityChip(
    icon: String,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = Color(0xCC09120D),
        border = androidx.compose.foundation.BorderStroke(0.7.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 10.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QuickNetworkCockpitCard(
    vpnState: VpnConnectionState,
    gamepadStatus: GamepadStatus,
    onConnectVpn: () -> Unit,
    onDisconnectVpn: () -> Unit,
    onSwitchEngine: (VpnEngine) -> Unit
) {
    val isConnected = vpnState.mode != NetworkMode.DISCONNECTED
    val directPing = vpnState.directLatencyMs
    val hasPing = directPing != null && directPing > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        XboxCardBorder,
                        if (isConnected) XboxNeonGreen.copy(alpha = 0.45f) else XboxCardBorder,
                        XboxCardBorder
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .testTag("network_cockpit_card"),
        colors = CardDefaults.cardColors(containerColor = XboxSurface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Cockpit Header (weight 1f so right badge never overlaps or squishes)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(XboxNeonGreen.copy(alpha = 0.22f), DirectCyan.copy(alpha = 0.14f))
                                )
                            )
                            .border(1.dp, XboxNeonGreen.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = XboxNeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = stringResource(R.string.cockpit_title),
                            color = TextPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                        Text(
                            text = "${vpnState.currentServer.flag} ${vpnState.currentServer.name}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                // Right Status / Ping Pill (Fixed height, never squished vertically)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (hasPing) DirectCyan.copy(alpha = 0.12f) else XboxSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (hasPing) DirectCyan.copy(alpha = 0.55f) else XboxCardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = if (hasPing) DirectCyan else XboxNeonGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (hasPing) "${directPing} ms" else "Sẵn sàng",
                            color = if (hasPing) DirectCyan else XboxNeonGreen,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }
            }

            // Row 2: Clean 2-Stage Summary (Mở khóa vùng • Tự ngắt VPN)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(11.dp),
                color = Color(0xFF08100C),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (vpnState.mode == NetworkMode.VPN_JAPAN) XboxNeonGreen else XboxNeonGreen.copy(alpha = 0.45f)
                                )
                        )
                        Column {
                            Text(
                                text = "Bước 1: Mở khóa vùng",
                                color = XboxNeonGreen,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${vpnState.currentServer.flag} Vào game nhanh",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Text(
                        text = "➔",
                        color = DirectCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (vpnState.mode == NetworkMode.DIRECT_NETWORK) DirectCyan else DirectCyan.copy(alpha = 0.45f)
                                )
                        )
                        Column {
                            Text(
                                text = "Bước 2: Tự ngắt VPN",
                                color = DirectCyan,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Chơi mạng trực tiếp",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Row 3: Quick VPN Engine Mode Selector Pills (Thông minh / Proxy / VPN)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val engineOptions = listOf(
                    VpnEngine.SMART_BYPASS to "⚡ Thông minh",
                    VpnEngine.PROXY_TUNNEL to "🌐 Proxy",
                    VpnEngine.EXTERNAL_VPN to "🛡️ VPN"
                )
                engineOptions.forEach { (engine, label) ->
                    val selected = vpnState.engine == engine
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { onSwitchEngine(engine) },
                        shape = RoundedCornerShape(9.dp),
                        color = if (selected) XboxNeonGreen.copy(alpha = 0.18f) else XboxSurfaceVariant.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(
                            0.8.dp,
                            if (selected) XboxNeonGreen else XboxCardBorder
                        )
                    ) {
                        Text(
                            text = label,
                            color = if (selected) XboxNeonGreen else TextPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // Row 4: Controller & Touch Hardware Status
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(11.dp),
                color = Color(0xFF0A140F),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxCardBorder.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = null,
                            tint = if (gamepadStatus.hasPhysicalController) XboxNeonGreen else DirectCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (gamepadStatus.hasPhysicalController)
                                gamepadStatus.controllerName
                            else
                                stringResource(R.string.virtual_gamepad_title),
                            color = TextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (gamepadStatus.hasPhysicalController) XboxNeonGreen.copy(alpha = 0.16f) else DirectCyan.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = if (gamepadStatus.hasPhysicalController)
                                stringResource(R.string.controller_connected)
                            else
                                stringResource(R.string.controller_touch),
                            color = if (gamepadStatus.hasPhysicalController) XboxNeonGreen else DirectCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            // Row 5: Tactile Connect / Disconnect Toggle Button
            Button(
                onClick = {
                    if (isConnected) onDisconnectVpn() else onConnectVpn()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("quick_vpn_toggle_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) XboxSurfaceElevated else XboxGreen
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = if (isConnected) XboxNeonGreen else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isConnected) stringResource(R.string.net_mode_direct) else stringResource(R.string.btn_enable_vpn),
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedGamesSection(
    featuredGames: List<GameBookmark>,
    onLaunchGame: (String) -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(XboxNeonGreen)
                )
                Column {
                    Text(
                        text = stringResource(R.string.featured_games_title).uppercase(),
                        color = TextPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = stringResource(R.string.featured_games_subtitle),
                        color = TextSecondary,
                        fontSize = 10.5.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = XboxSurfaceVariant.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxNeonGreen.copy(alpha = 0.4f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onNavigateToLibrary() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.btn_view_all),
                        color = XboxNeonGreen,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = XboxNeonGreen,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(featuredGames, key = { it.id }) { game ->
                FeaturedGameCard(game = game, onLaunch = { onLaunchGame(game.playUrl) })
            }
        }
    }
}

@Composable
private fun FeaturedGameCard(
    game: GameBookmark,
    onLaunch: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(184.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(XboxCardBorder, Color(game.accentColorHex).copy(alpha = 0.55f))
                ),
                RoundedCornerShape(18.dp)
            )
            .clickable { onLaunch() }
            .testTag("featured_game_${game.id}"),
        colors = CardDefaults.cardColors(containerColor = XboxSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column {
            // Poster Art with Vignette & Floating Badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.dp)
            ) {
                if (game.drawableResId != null) {
                    Image(
                        painter = painterResource(id = game.drawableResId),
                        contentDescription = game.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(game.accentColorHex), XboxDark)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                // Bottom dark gradient scrim over image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, XboxSurface.copy(alpha = 0.92f)),
                                startY = 120f
                            )
                        )
                )

                // Top-left Badge Tag
                if (game.badge != null) {
                    Surface(
                        shape = RoundedCornerShape(bottomEnd = 10.dp),
                        color = Color(0xD9060B08),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, XboxNeonGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = game.badge,
                            color = XboxNeonGreen,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                // Top-right Touch icon indicator
                if (game.supportsTouch) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xCC060B08),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "Touch",
                            tint = DirectCyan,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(12.dp)
                        )
                    }
                }
            }

            // Game Metadata & Launch Trigger
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = game.title,
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = game.category,
                    color = TextSecondary,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = GoldStar,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = game.rating,
                            color = TextPrimary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = XboxGreen,
                        border = androidx.compose.foundation.BorderStroke(0.7.dp, XboxNeonGreen.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = stringResource(R.string.btn_play_now),
                                color = Color.White,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartTranslatorHomeCard(
    onOpenTranslator: () -> Unit
) {
    val config by SettingsRepository.config.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(XboxNeonGreen.copy(alpha = 0.55f), DirectCyan.copy(alpha = 0.55f))
                ),
                RoundedCornerShape(20.dp)
            )
            .clickable { onOpenTranslator() }
            .testTag("home_smart_translator_card"),
        colors = CardDefaults.cardColors(containerColor = XboxSurface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = XboxNeonGreen.copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, XboxNeonGreen.copy(alpha = 0.45f)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = XboxNeonGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "Dịch tự động",
                            color = TextPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                        Text(
                            text = "Dịch game sang tiếng Việt",
                            color = XboxNeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = if (config.xcloudTranslateEnabled) XboxNeonGreen.copy(alpha = 0.20f) else Color.DarkGray,
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        if (config.xcloudTranslateEnabled) XboxNeonGreen.copy(alpha = 0.6f) else Color.Transparent
                    )
                ) {
                    Text(
                        text = if (config.xcloudTranslateEnabled) "Đang bật" else "Đang tắt",
                        color = if (config.xcloudTranslateEnabled) XboxNeonGreen else TextSecondary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Non-technical protection badges & well-spaced "Mở bảng dịch" button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF08100C),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = XboxSurfaceVariant.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(0.6.dp, XboxCardBorder)
                        ) {
                            Text(
                                text = "🛡️ Giữ tên game",
                                color = TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = XboxSurfaceVariant.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(0.6.dp, XboxCardBorder)
                        ) {
                            Text(
                                text = "🎮 Giữ nút bấm",
                                color = TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = DirectCyan.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(0.9.dp, DirectCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "Mở bảng dịch",
                                color = DirectCyan,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = DirectCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

