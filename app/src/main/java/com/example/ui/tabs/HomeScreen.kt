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
    onNavigateToServers: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
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
        // 1. Banner chính UI 3.0: nút vào Xbox, nút Fortnite, và thanh chỉnh nhanh Video 16:9 / Quay Luồng / Đồ họa
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
            onNavigateToSettings = onNavigateToSettings,
            onOpenGuide = { onOpenGuide(GuideSection.QUICK_START) }
        )

        // 2. Bảng điều khiển mạng UI 3.0: cột sóng + tên máy chủ + ping + nút bật/tắt VPN & đổi máy chủ
        QuickNetworkCockpitCard(
            vpnState = vpnState,
            onConnectVpn = onConnectVpn,
            onDisconnectVpn = onDisconnectVpn,
            onNavigateToServers = onNavigateToServers
        )

        // 3. Mục Game nổi bật UI 3.0 + thẻ "Chơi tiếp" (Quick Resume) cho game vừa chơi gần nhất
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
    onNavigateToSettings: () -> Unit,
    onOpenGuide: () -> Unit
) {
    val config by SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    val borderGlowAlpha = if (config.lowEndDeviceMode) {
        0.65f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "hero_glow")
        val animatedAlpha by infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 0.92f,
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
                        DirectCyan.copy(alpha = 0.65f),
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
            Image(
                painter = painterResource(id = R.drawable.gamepass_hub_banner_1791394609676),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(294.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(294.dp)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0x22040806),
                                0.30f to Color(0xA8050A07),
                                0.62f to Color(0xEB060B08),
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
                // Hàng trạng thái trên cùng của Hero Deck
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
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

                        // Badge báo chống bóp hình & quay trực tiếp
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xD907130E),
                            border = androidx.compose.foundation.BorderStroke(
                                0.8.dp,
                                DirectCyan.copy(alpha = 0.55f)
                            )
                        ) {
                            Text(
                                text = if (isEn) "16:9 HDR • 0% LAG" else "CHỐNG BÓP HÌNH • 0% LAG",
                                color = DirectCyan,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
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

                Spacer(modifier = Modifier.height(6.dp))

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

                // Hai nút khởi chạy chính với phụ đề thông số Console
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenXboxCloud,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
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
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = stringResource(R.string.btn_launch_xcloud),
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1
                            )
                            Text(
                                text = if (isEn) "60FPS • 16:9 Ready" else "60FPS • Chuẩn 16:9",
                                color = Color.White.copy(alpha = 0.85f),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    Button(
                        onClick = onLaunchFortnite,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
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
                            modifier = Modifier.size(19.dp)
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
                                text = if (isEn) "Free • No Pass Needed" else "Miễn phí • Vào ngay",
                                color = GoldStar.copy(alpha = 0.95f),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Thanh chỉnh nhanh tương tác (1 chạm để đổi Tỉ lệ Video, Đồ họa 4G/Wi-Fi, hoặc mở Cài đặt Video)
                val regionCode = config.getEffectiveLocale().takeLast(2).uppercase()
                val graphicsShort = when (config.cellularMode) {
                    com.example.data.CellularOptimizationMode.AUTO -> if (isEn) "Auto" else "Tự động"
                    com.example.data.CellularOptimizationMode.DATA_SAVER_HIGH_SPEED -> if (isEn) "Low" else "Thấp"
                    com.example.data.CellularOptimizationMode.MEDIUM -> if (isEn) "Medium" else "Vừa"
                    com.example.data.CellularOptimizationMode.HIGH -> if (isEn) "High" else "Cao"
                    com.example.data.CellularOptimizationMode.MAX_SPEED_UNCAPPED -> if (isEn) "Ultra" else "Rất cao"
                }
                val aspectShort = when (config.videoAspectRatioMode) {
                    com.example.data.VideoAspectRatioMode.FIT_16_9 -> "16:9"
                    com.example.data.VideoAspectRatioMode.CINEMA_18_9 -> "18:9"
                    com.example.data.VideoAspectRatioMode.ZOOM_COVER -> if (isEn) "Fill" else "Đầy"
                    com.example.data.VideoAspectRatioMode.STRETCH_FULL -> if (isEn) "Full" else "Giãn"
                    com.example.data.VideoAspectRatioMode.CUSTOM_SCALE ->
                        "${(config.videoScaleX * 100).toInt()}%"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HeroCapabilityChip(
                        icon = "🎬",
                        label = if (isEn) "Ratio: $aspectShort" else "Tỉ lệ: $aspectShort",
                        accentColor = DirectCyan,
                        onClick = {
                            val modes = com.example.data.VideoAspectRatioMode.entries
                            val nextIdx = (modes.indexOf(config.videoAspectRatioMode) + 1) % modes.size
                            SettingsRepository.updateVideoAspectRatioMode(modes[nextIdx])
                        },
                        modifier = Modifier.weight(1.05f)
                    )
                    HeroCapabilityChip(
                        icon = "✨",
                        label = if (isEn) "Quality: $graphicsShort" else "Đồ họa: $graphicsShort",
                        accentColor = XboxNeonGreen,
                        onClick = {
                            val modes = com.example.data.CellularOptimizationMode.entries
                            val nextIdx = (modes.indexOf(config.cellularMode) + 1) % modes.size
                            SettingsRepository.updateCellularMode(modes[nextIdx])
                        },
                        modifier = Modifier.weight(1.15f)
                    )
                    HeroCapabilityChip(
                        icon = if (gamepadStatus.hasPhysicalController) "🎮" else "⚙️",
                        label = if (gamepadStatus.hasPhysicalController) {
                            if (isEn) "Gamepad" else "Tay cầm"
                        } else {
                            if (isEn) "Video ($regionCode)" else "Video ($regionCode)"
                        },
                        accentColor = if (gamepadStatus.hasPhysicalController) XboxNeonGreen else GoldStar,
                        onClick = onNavigateToSettings,
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
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = if (onClick != null) {
            modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onClick() }
        } else {
            modifier
        },
        shape = RoundedCornerShape(10.dp),
        color = Color(0xD908110C),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, accentColor.copy(alpha = 0.42f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 10.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickNetworkCockpitCard(
    vpnState: VpnConnectionState,
    onConnectVpn: () -> Unit,
    onDisconnectVpn: () -> Unit,
    onNavigateToServers: () -> Unit = {}
) {
    val config by SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    val isConnected = vpnState.mode != NetworkMode.DISCONNECTED
    val directPing = vpnState.directLatencyMs
    val hasPing = directPing != null && directPing > 0
    val activeBars = when {
        !hasPing -> 3
        directPing < 55 -> 4
        directPing < 95 -> 3
        directPing < 150 -> 2
        else -> 1
    }
    val signalTint = when (activeBars) {
        4 -> XboxNeonGreen
        3 -> DirectCyan
        2 -> WarningAmber
        else -> JapanRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        if (isConnected) XboxNeonGreen.copy(alpha = 0.55f) else XboxCardBorder,
                        DirectCyan.copy(alpha = 0.35f)
                    )
                ),
                RoundedCornerShape(18.dp)
            )
            .testTag("network_cockpit_card"),
        colors = CardDefaults.cardColors(containerColor = XboxSurface.copy(alpha = 0.97f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onNavigateToServers() }
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = vpnState.currentServer.flag, fontSize = 19.sp)

                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = vpnState.currentServer.name,
                        color = TextPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isEn) "Tap to switch server" else "Chạm để đổi máy chủ",
                        color = TextSecondary,
                        fontSize = 9.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Cột sóng 4 vạch trực quan + Ping
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = signalTint.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        signalTint.copy(alpha = 0.48f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 4-bar micro signal meter
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                            modifier = Modifier.height(11.dp)
                        ) {
                            for (bar in 1..4) {
                                val barHeight = (3 + bar * 2).dp
                                Box(
                                    modifier = Modifier
                                        .width(2.2.dp)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(
                                            if (bar <= activeBars) signalTint
                                            else TextSecondary.copy(alpha = 0.28f)
                                        )
                                )
                            }
                        }
                        Text(
                            text = if (hasPing) "${directPing}ms" else if (isEn) "Ready" else "Sẵn sàng",
                            color = signalTint,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }
            }

            Button(
                onClick = { if (isConnected) onDisconnectVpn() else onConnectVpn() },
                modifier = Modifier
                    .height(38.dp)
                    .testTag("quick_vpn_toggle_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) XboxSurfaceElevated else XboxGreen
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(11.dp)
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
    val config by SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()

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
                        .size(4.dp, 20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(XboxNeonGreen, DirectCyan)
                            )
                        )
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
                color = XboxSurfaceVariant.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxNeonGreen.copy(alpha = 0.45f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onNavigateToLibrary() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.btn_view_all),
                        color = XboxNeonGreen,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
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

        // Thẻ "Chơi tiếp" (Quick Resume Console Card) cho game vừa chơi gần nhất nếu đã có dữ liệu
        if (lastPlayedGame != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onLaunchBookmark(lastPlayedGame) }
                    .testTag("continue_last_game_button"),
                shape = RoundedCornerShape(16.dp),
                color = XboxSurfaceVariant.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(
                    1.2.dp,
                    Brush.horizontalGradient(
                        listOf(DirectCyan.copy(alpha = 0.65f), XboxNeonGreen.copy(alpha = 0.65f))
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DirectCyan.copy(alpha = 0.16f),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, DirectCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Gamepad,
                                    contentDescription = null,
                                    tint = DirectCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isEn) "QUICK RESUME" else "CHƠI TIẾP NGAY",
                                    color = DirectCyan,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "• 16:9 STREAM",
                                    color = XboxNeonGreen,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${stringResource(R.string.btn_continue_game)}: ${lastPlayedGame.title}",
                                color = TextPrimary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = XboxGreen,
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxNeonGreen.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = stringResource(R.string.btn_continue_game),
                                color = Color.White,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        if (featuredGames.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = XboxSurface.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = null,
                        tint = DirectCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isEn) {
                            "Auto-fetching game info from Xbox Cloud (0 MB image data)..."
                        } else {
                            "Đang tự động tải thông tin game từ Xbox Cloud (không tải ảnh, tiết kiệm 4G/Wi-Fi)..."
                        },
                        color = TextSecondary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
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
}

@Composable
private fun FeaturedGameCard(
    game: GameBookmark,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onLaunch: () -> Unit
) {
    val config by SettingsRepository.config.collectAsState()
    val isImportingCatalog by GameLibraryRepository.isSyncingCatalog.collectAsState()
    val isEn = config.isEnglishUI()
    val localizedBadge = game.localizedBadge(isEn)
    val localizedCategory = game.localizedCategory(isEn)
    val monogram = remember(game.title) {
        game.title.split(" ")
            .filter { it.isNotBlank() && it.first().isLetterOrDigit() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "XB" }
    }
    Card(
        modifier = Modifier
            .width(188.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(
                1.1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color(game.accentColorHex).copy(alpha = 0.65f),
                        XboxCardBorder,
                        DirectCyan.copy(alpha = 0.4f)
                    )
                ),
                RoundedCornerShape(18.dp)
            )
            .clickable { onLaunch() }
            .testTag("featured_game_${game.id}"),
        colors = CardDefaults.cardColors(containerColor = XboxSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 7.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(game.accentColorHex).copy(alpha = 0.48f),
                                    XboxSurfaceVariant.copy(alpha = 0.92f),
                                    XboxDark
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isImportingCatalog || game.imageUrl.isNullOrBlank()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0x88060B08),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color(game.accentColorHex).copy(alpha = 0.75f)
                                ),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = monogram,
                                        color = Color.White,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            if (!game.publisher.isNullOrBlank()) {
                                Text(
                                    text = game.publisher,
                                    color = DirectCyan.copy(alpha = 0.9f),
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (!isImportingCatalog && !game.imageUrl.isNullOrBlank()) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = remember(game.imageUrl) {
                            ImageRequest.Builder(context)
                                .data(game.imageUrl)
                                .crossfade(true)
                                .build()
                        },
                        contentDescription = game.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (game.drawableResId != null) {
                    Image(
                        painter = painterResource(id = game.drawableResId),
                        contentDescription = game.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, XboxSurface.copy(alpha = 0.94f)),
                                startY = 115f
                            )
                        )
                )

                if (localizedBadge != null) {
                    Surface(
                        shape = RoundedCornerShape(bottomEnd = 10.dp),
                        color = Color(0xD9060B08),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, XboxNeonGreen.copy(alpha = 0.55f)),
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

                // Bottom-left Console Telemetry Micro-Pills (60FPS • 16:9)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = Color(0xD9060B08),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, DirectCyan.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "60 FPS",
                            color = DirectCyan,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.5.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = Color(0xD9060B08),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, XboxNeonGreen.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "16:9",
                            color = XboxNeonGreen,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.5.dp)
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
                        border = androidx.compose.foundation.BorderStroke(0.7.dp, XboxNeonGreen.copy(alpha = 0.55f))
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

