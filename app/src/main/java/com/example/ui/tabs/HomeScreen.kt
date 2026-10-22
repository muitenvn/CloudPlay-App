package com.example.ui.tabs

import android.content.Context
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.cloudplay.GamepadStatus
import com.example.data.GameBookmark
import com.example.data.GameLibraryRepository
import com.example.data.SessionHistoryRepository
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

private const val RECENT_GAME_PREFS = "cloudplay_recent_game_prefs"
private const val KEY_LAST_PLAYED_ID = "last_played_game_id"

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
    val context = LocalContext.current
    val recentPrefs = remember(context) {
        context.getSharedPreferences(RECENT_GAME_PREFS, Context.MODE_PRIVATE)
    }
    val favoriteIds by GameLibraryRepository.favoriteIds.collectAsState()
    val repoLastPlayedId by GameLibraryRepository.lastPlayedId.collectAsState()
    val recentSessions by SessionHistoryRepository.sessions.collectAsState()
    var localLastPlayedId by remember {
        mutableStateOf(recentPrefs.getString(KEY_LAST_PLAYED_ID, null))
    }
    val effectiveLastPlayedId = repoLastPlayedId ?: localLastPlayedId

    val recordAndLaunch: (GameBookmark) -> Unit = { game ->
        recentPrefs.edit().putString(KEY_LAST_PLAYED_ID, game.id).apply()
        localLastPlayedId = game.id
        GameLibraryRepository.recordLastPlayed(game.id)
        onLaunchGame(game.playUrl)
    }

    val sortedFeaturedGames = remember(featuredGames, favoriteIds) {
        featuredGames.sortedByDescending { it.id in favoriteIds }.take(18)
    }

    val lastPlayedGame = remember(effectiveLastPlayedId, featuredGames, recentSessions) {
        val byId = effectiveLastPlayedId?.let { id ->
            featuredGames.find { it.id.equals(id, ignoreCase = true) }
        }
        if (byId != null) {
            byId
        } else {
            val latestTitle = recentSessions.firstOrNull()?.gameTitle?.trim()
            if (!latestTitle.isNullOrBlank()) {
                featuredGames.firstOrNull {
                    it.title.equals(latestTitle, ignoreCase = true) ||
                        it.title.contains(latestTitle, ignoreCase = true) ||
                        latestTitle.contains(it.title, ignoreCase = true)
                }
            } else null
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Banner chính: nút vào Xbox, nút Fortnite và 3 chip nhanh
        HeroConsolePlayDeck(
            vpnState = vpnState,
            gamepadStatus = gamepadStatus,
            onOpenXboxCloud = onOpenXboxCloud,
            onLaunchFortnite = {
                val fortniteUrl = "https://www.xbox.com/en-US/play/games/fortnite/BT5P2X999VH2"
                recentPrefs.edit().putString(KEY_LAST_PLAYED_ID, "fortnite").apply()
                localLastPlayedId = "fortnite"
                GameLibraryRepository.recordLastPlayed("fortnite")
                onLaunchGame(fortniteUrl)
            },
            onOpenGuide = { onOpenGuide(GuideSection.QUICK_START) }
        )

        // 2. Hàng trạng thái mạng nhỏ gọn: tên máy chủ + ping + nút bật/tắt VPN
        QuickNetworkCockpitCard(
            vpnState = vpnState,
            onConnectVpn = onConnectVpn,
            onDisconnectVpn = onDisconnectVpn
        )

        // 3. Mục Game nổi bật + nút "Chơi tiếp" cho game vừa chơi gần nhất
        FeaturedGamesSection(
            featuredGames = sortedFeaturedGames,
            favoriteIds = favoriteIds,
            lastPlayedGame = lastPlayedGame,
            onLaunchBookmark = recordAndLaunch,
            onToggleFavorite = { GameLibraryRepository.toggleFavorite(it.id) },
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
    val borderGlowAlpha = if (config.lowEndDeviceMode) {
        0.65f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "hero_glow")
        val animatedAlpha by infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 0.90f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "border_glow_alpha"
        )
        animatedAlpha
    }

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
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.gamepass_hub_banner_1791394609676),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(256.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(256.dp)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0x1A040806),
                                0.35f to Color(0xA6050A07),
                                0.65f to Color(0xEB060B08),
                                1.0f to XboxSurface
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                            val isEn = config.isEnglishUI()
                            Text(
                                text = when (vpnState.mode) {
                                    NetworkMode.DIRECT_NETWORK -> if (isEn) "Direct Net" else "Mạng trực tiếp"
                                    NetworkMode.VPN_JAPAN -> if (isEn) "Unlocked ${vpnState.currentServer.flag}" else "Mở vùng ${vpnState.currentServer.flag}"
                                    NetworkMode.DISCONNECTED -> if (isEn) "Auto VPN" else "Tự tắt VPN"
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
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = stringResource(R.string.guide_button),
                                tint = DirectCyan,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.hero_title),
                        color = TextPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenXboxCloud,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("hero_launch_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = XboxGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.btn_launch_xcloud),
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = onLaunchFortnite,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("hero_launch_fortnite_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EpicBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = GoldStar,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.btn_play_fortnite),
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                }

                val isEn = config.isEnglishUI()
                val regionCode = config.getEffectiveLocale().takeLast(2).uppercase()
                val graphicsShort = when (config.cellularMode) {
                    com.example.data.CellularOptimizationMode.AUTO -> if (isEn) "Auto" else "Tự động"
                    com.example.data.CellularOptimizationMode.DATA_SAVER_HIGH_SPEED -> if (isEn) "Low" else "Thấp"
                    com.example.data.CellularOptimizationMode.MEDIUM -> if (isEn) "Medium" else "Vừa"
                    com.example.data.CellularOptimizationMode.HIGH -> if (isEn) "High" else "Cao"
                    com.example.data.CellularOptimizationMode.MAX_SPEED_UNCAPPED -> if (isEn) "Ultra" else "Rất cao"
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HeroCapabilityChip(
                        icon = "🌐",
                        label = if (isEn) "Region $regionCode" else "Vùng $regionCode",
                        accentColor = XboxNeonGreen,
                        modifier = Modifier.weight(1f)
                    )
                    HeroCapabilityChip(
                        icon = "✨",
                        label = if (isEn) "Quality: $graphicsShort" else "Đồ họa: $graphicsShort",
                        accentColor = DirectCyan,
                        modifier = Modifier.weight(1.15f)
                    )
                    HeroCapabilityChip(
                        icon = "🎮",
                        label = if (gamepadStatus.hasPhysicalController) {
                            if (isEn) "Gamepad" else "Tay cầm"
                        } else {
                            if (isEn) "Touch" else "Cảm ứng"
                        },
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
    onConnectVpn: () -> Unit,
    onDisconnectVpn: () -> Unit
) {
    val config by SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    val isConnected = vpnState.mode != NetworkMode.DISCONNECTED
    val directPing = vpnState.directLatencyMs
    val hasPing = directPing != null && directPing > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isConnected) XboxNeonGreen.copy(alpha = 0.45f) else XboxCardBorder,
                RoundedCornerShape(16.dp)
            )
            .testTag("network_cockpit_card"),
        colors = CardDefaults.cardColors(containerColor = XboxSurface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                Text(text = vpnState.currentServer.flag, fontSize = 18.sp)
                Text(
                    text = vpnState.currentServer.name,
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (hasPing) DirectCyan.copy(alpha = 0.14f) else XboxSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        if (hasPing) DirectCyan.copy(alpha = 0.5f) else XboxCardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = if (hasPing) DirectCyan else XboxNeonGreen,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = if (hasPing) "${directPing}ms" else if (isEn) "Ready" else "Sẵn sàng",
                            color = if (hasPing) DirectCyan else XboxNeonGreen,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            Button(
                onClick = { if (isConnected) onDisconnectVpn() else onConnectVpn() },
                modifier = Modifier
                    .height(36.dp)
                    .testTag("quick_vpn_toggle_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) XboxSurfaceElevated else XboxGreen
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.VpnKey,
                    contentDescription = null,
                    tint = if (isConnected) XboxNeonGreen else Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isConnected) stringResource(R.string.btn_switch_direct) else stringResource(R.string.btn_enable_vpn),
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun FeaturedGamesSection(
    featuredGames: List<GameBookmark>,
    favoriteIds: Set<String>,
    lastPlayedGame: GameBookmark?,
    onLaunchBookmark: (GameBookmark) -> Unit,
    onToggleFavorite: (GameBookmark) -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

        // Nút "Chơi tiếp" cho game vừa chơi gần nhất nếu đã có dữ liệu
        if (lastPlayedGame != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onLaunchBookmark(lastPlayedGame) }
                    .testTag("continue_last_game_button"),
                shape = RoundedCornerShape(14.dp),
                color = XboxSurfaceVariant.copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, DirectCyan.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = null,
                            tint = DirectCyan,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "${stringResource(R.string.btn_continue_game)}: ${lastPlayedGame.title}",
                            color = TextPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = XboxGreen
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = stringResource(R.string.btn_continue_game),
                                color = Color.White,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(featuredGames, key = { it.id }) { game ->
                FeaturedGameCard(
                    game = game,
                    isFavorite = game.id in favoriteIds,
                    onToggleFavorite = { onToggleFavorite(game) },
                    onLaunch = { onLaunchBookmark(game) }
                )
            }
        }
    }
}

@Composable
private fun FeaturedGameCard(
    game: GameBookmark,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onLaunch: () -> Unit
) {
    val config by SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    val localizedBadge = game.localizedBadge(isEn)
    val localizedCategory = game.localizedCategory(isEn)
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
                        if (!game.imageUrl.isNullOrBlank()) {
                            val context = LocalContext.current
                            val request = remember(game.imageUrl, config.lowEndDeviceMode) {
                                ImageRequest.Builder(context)
                                    .data(game.imageUrl)
                                    .crossfade(!config.lowEndDeviceMode)
                                    .build()
                            }
                            AsyncImage(
                                model = request,
                                contentDescription = game.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

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

                if (localizedBadge != null) {
                    Surface(
                        shape = RoundedCornerShape(bottomEnd = 10.dp),
                        color = Color(0xD9060B08),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, XboxNeonGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = localizedBadge,
                            color = XboxNeonGreen,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (game.supportsTouch) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xCC060B08)
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
                    Surface(
                        shape = CircleShape,
                        color = Color(0xCC060B08),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onToggleFavorite() }
                            .testTag("fav_featured_${game.id}")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isEn) "Favorite" else "Yêu thích",
                            tint = if (isFavorite) JapanRed else Color.White.copy(alpha = 0.75f),
                            modifier = Modifier
                                .padding(4.dp)
                                .size(12.dp)
                        )
                    }
                }
            }

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
                    text = localizedCategory,
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
