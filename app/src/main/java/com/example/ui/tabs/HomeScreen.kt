package com.example.ui.tabs

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ui.theme.animateXboxIntAsState
import com.example.ui.theme.rememberXboxMotionEnabled
import com.example.ui.theme.xboxPressScale
import com.example.ui.theme.xboxSlideFadeIn
import com.example.vpn.NetworkMode
import com.example.vpn.VpnConnectionState
import com.example.vpn.VpnEngine

import androidx.compose.foundation.layout.BoxWithConstraints

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

    val initialHomeGames = remember {
        Triple(
            featuredGames.take(18),
            null as GameBookmark?,
            emptyList<GameBookmark>()
        )
    }
    val computedHomeGames by androidx.compose.runtime.produceState(
        initialValue = initialHomeGames,
        featuredGames,
        favoriteIds,
        effectiveLastPlayedId,
        recentSessions
    ) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val topFeatured = if (favoriteIds.isEmpty()) {
                featuredGames.take(18)
            } else {
                val favs = ArrayList<GameBookmark>(18)
                val nonFavs = ArrayList<GameBookmark>(18)
                for (g in featuredGames) {
                    if (g.id in favoriteIds) {
                        favs.add(g)
                        if (favs.size >= 18) break
                    } else if (nonFavs.size < 18) {
                        nonFavs.add(g)
                    }
                }
                if (favs.size >= 18) favs else (favs + nonFavs).take(18)
            }

            val byId = effectiveLastPlayedId?.let { id ->
                featuredGames.find { it.id.equals(id, ignoreCase = true) }
            }
            val lastGame = if (byId != null) {
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

            val quickGames = buildList {
                if (lastGame != null) add(lastGame)
                for (session in recentSessions) {
                    if (size >= 4) break
                    val title = session.gameTitle.trim()
                    if (title.isNotBlank()) {
                        val matched = featuredGames.firstOrNull {
                            it.title.equals(title, ignoreCase = true) ||
                                it.title.contains(title, ignoreCase = true) ||
                                title.contains(it.title, ignoreCase = true)
                        }
                        if (matched != null && none { it.id == matched.id }) {
                            add(matched)
                        }
                    }
                }
            }
            Triple(topFeatured, lastGame, quickGames)
        }
    }
    val sortedFeaturedGames = computedHomeGames.first
    val lastPlayedGame = computedHomeGames.second
    val recentQuickGames = computedHomeGames.third

    val config by SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    val updateState by com.example.data.UpdateChecker.state.collectAsState()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 640.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            val availableUpdate = updateState as? com.example.data.UpdateCheckState.UpdateAvailable
            if (availableUpdate != null) {
                item(key = "home_update_banner") {
                    val bannerShape = remember { RoundedCornerShape(10.dp) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(bannerShape)
                            .background(XboxNeonGreen.copy(alpha = 0.12f), bannerShape)
                            .border(1.dp, XboxNeonGreen.copy(alpha = 0.5f), bannerShape)
                            .clickable {
                                com.example.data.AppLogger.info("UI", "Bấm thanh thông báo bản cập nhật mới từ Trang chủ")
                                com.example.data.UpdateChecker.openUpdateDialog()
                            }
                            .padding(horizontal = 12.dp)
                            .testTag("home_update_available_banner"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEn) {
                                "New version ${availableUpdate.latestVersion} available · Tap to update"
                            } else {
                                "Có bản mới ${availableUpdate.latestVersion} · Bấm để tải cập nhật"
                            },
                            color = XboxNeonGreen,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "↗",
                            color = XboxNeonGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            if (isWideScreen) {
                item(key = "home_wide_top_deck") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(modifier = Modifier.weight(1.15f)) {
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
                        }
                        Box(modifier = Modifier.weight(0.85f)) {
                            QuickNetworkCockpitCard(
                                vpnState = vpnState,
                                onConnectVpn = onConnectVpn,
                                onDisconnectVpn = onDisconnectVpn,
                                onNavigateToServers = onNavigateToServers
                            )
                        }
                    }
                }
            } else {
                // 1. Banner chính UI 3.0: nút vào Xbox, nút Fortnite, và thanh chỉnh nhanh Video 16:9 / Quay Luồng / Đồ họa
                item(key = "home_hero_deck") {
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
                }

                // 2. Bảng điều khiển mạng UI 3.0: cột sóng + tên máy chủ + ping + nút bật/tắt VPN & đổi máy chủ
                item(key = "home_network_cockpit") {
                    QuickNetworkCockpitCard(
                        vpnState = vpnState,
                        onConnectVpn = onConnectVpn,
                        onDisconnectVpn = onDisconnectVpn,
                        onNavigateToServers = onNavigateToServers
                    )
                }
            }

            // 3. Mục Game nổi bật UI 3.0 + thẻ "Chơi tiếp" (Quick Resume) cho các game vừa chơi gần nhất
            item(key = "home_featured_games") {
                FeaturedGamesSection(
                    featuredGames = sortedFeaturedGames,
                    favoriteIds = favoriteIds,
                    lastPlayedGame = lastPlayedGame,
                    recentGames = recentQuickGames,
                    onLaunchBookmark = recordAndLaunch,
                    onToggleFavorite = { GameLibraryRepository.toggleFavorite(it.id) },
                    onNavigateToLibrary = onNavigateToLibrary
                )
            }
        }
    }
}

