package com.example.ui.tv

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.AppConfig
import com.example.data.GameBookmark
import com.example.data.GameLibraryRepository
import com.example.data.SessionHistoryRepository
import com.example.data.SettingsRepository
import com.example.data.UpdateCheckState
import com.example.data.UpdateChecker
import com.example.data.XboxWebUiMode
import com.example.ui.components.GuideSection
import com.example.ui.components.gamepadClickable
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.vpn.NetworkMode
import com.example.vpn.VpnConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun TvHomeScreen(
    config: AppConfig,
    vpnState: VpnConnectionState,
    measuredPingMs: Int?,
    featuredGames: List<GameBookmark>,
    scale: Float,
    defaultFocusRequester: FocusRequester,
    onOpenXboxCloud: () -> Unit,
    onLaunchGame: (String) -> Unit,
    onConnectVpn: () -> Unit,
    onDisconnectVpn: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToServers: () -> Unit,
    onOpenLanguagePicker: () -> Unit,
    onOpenTranslator: () -> Unit,
    onOpenWebPortal: () -> Unit,
    onOpenGuide: (GuideSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val favoriteIds by GameLibraryRepository.favoriteIds.collectAsState()
    val repoLastPlayedId by GameLibraryRepository.lastPlayedId.collectAsState()
    val playedGameIds by GameLibraryRepository.playedGameIds.collectAsState()
    val recentSessions by SessionHistoryRepository.sessions.collectAsState()
    val updateState by UpdateChecker.state.collectAsState()

    val initialLists = remember {
        Pair(emptyList<Pair<GameBookmark, String>>(), featuredGames.take(16))
    }

    val computedLists by produceState(
        initialValue = initialLists,
        featuredGames,
        favoriteIds,
        repoLastPlayedId,
        playedGameIds,
        recentSessions,
        config.appLanguage
    ) {
        value = withContext(Dispatchers.Default) {
            val playedPairs = ArrayList<Pair<GameBookmark, String>>()
            val seenIds = HashSet<String>()
            val nowMs = System.currentTimeMillis()

            fun subtitleForGame(game: GameBookmark, index: Int): String {
                val matchingSession = recentSessions.firstOrNull {
                    it.gameTitle.equals(game.title, ignoreCase = true) ||
                        it.gameTitle.contains(game.title, ignoreCase = true)
                }
                if (index == 0) {
                    return config.tr("Chơi gần nhất", "Last played")
                }
                val sessionEndMs = if (matchingSession != null) {
                    matchingSession.startTimeMillis + matchingSession.durationSeconds * 1000L
                } else {
                    0L
                }
                if (sessionEndMs > 0L) {
                    val diffHours = ((nowMs - sessionEndMs).coerceAtLeast(0L) / 3_600_000L).toInt()
                    return when {
                        diffHours < 1 -> config.tr("Vừa chơi xong", "Played just now")
                        diffHours < 24 -> config.tr("Đã chơi $diffHours giờ trước", "Played ${diffHours}h ago")
                        diffHours < 48 -> config.tr("Hôm qua", "Yesterday")
                        else -> config.tr("${diffHours / 24} ngày trước", "${diffHours / 24}d ago")
                    }
                }
                return config.tr(game.category, game.categoryEn)
            }

            if (repoLastPlayedId != null) {
                val last = featuredGames.firstOrNull { it.id.equals(repoLastPlayedId, ignoreCase = true) }
                if (last != null && seenIds.add(last.id.lowercase())) {
                    playedPairs.add(last to subtitleForGame(last, playedPairs.size))
                }
            }
            for (pid in playedGameIds) {
                val found = featuredGames.firstOrNull { it.id.equals(pid, ignoreCase = true) }
                if (found != null && seenIds.add(found.id.lowercase())) {
                    playedPairs.add(found to subtitleForGame(found, playedPairs.size))
                }
            }
            for (session in recentSessions) {
                if (playedPairs.size >= 12) break
                val t = session.gameTitle.trim()
                if (t.isNotBlank()) {
                    val matched = featuredGames.firstOrNull {
                        it.title.equals(t, ignoreCase = true) ||
                            it.title.contains(t, ignoreCase = true) ||
                            t.contains(it.title, ignoreCase = true)
                    }
                    if (matched != null && seenIds.add(matched.id.lowercase())) {
                        playedPairs.add(matched to subtitleForGame(matched, playedPairs.size))
                    }
                }
            }

            val topRecommended = if (favoriteIds.isEmpty()) {
                featuredGames.take(18)
            } else {
                val favs = featuredGames.filter { it.id in favoriteIds }
                val others = featuredGames.filter { it.id !in favoriteIds }.take(18)
                (favs + others).take(18)
            }
            Pair(playedPairs.toList(), topRecommended)
        }
    }

    val continuePlayingItems = computedLists.first
    val recommendedGames = computedLists.second
    val isVpnConnected = vpnState.mode != NetworkMode.DISCONNECTED
    val availableUpdate = updateState as? UpdateCheckState.UpdateAvailable

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("tv_home_screen"),
        verticalArrangement = Arrangement.spacedBy((16.dp * scale).coerceAtLeast(10.dp)),
        contentPadding = PaddingValues(bottom = (12.dp * scale).coerceAtLeast(8.dp))
    ) {
        // 1. Top Header: "Xin chào" + Status Pills & Quick Utility Pills
        item(key = "tv_home_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = config.tr("Xin chào", "Welcome"),
                    color = TvInk,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = (28f * scale).coerceAtLeast(20f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy((8.dp * scale).coerceAtLeast(6.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (availableUpdate != null) {
                        TvPill(
                            text = config.tr(
                                "⬆ Bản mới ${availableUpdate.latestVersion}",
                                "⬆ Update ${availableUpdate.latestVersion}"
                            ),
                            scale = scale,
                            tone = TvPillTone.WARN,
                            onClick = { UpdateChecker.openUpdateDialog() }
                        )
                    }

                    TvPill(
                        text = when (vpnState.mode) {
                            NetworkMode.DIRECT_NETWORK -> config.tr("Mạng trực tiếp", "Direct")
                            NetworkMode.VPN_JAPAN -> config.tr("VPN bật", "VPN On")
                            NetworkMode.DISCONNECTED -> config.tr("Sẵn sàng", "Ready")
                        },
                        scale = scale,
                        tone = TvPillTone.GREEN,
                        onClick = onNavigateToServers
                    )

                    if (measuredPingMs != null && measuredPingMs > 0) {
                        TvPill(
                            text = "$measuredPingMs ms",
                            scale = scale,
                            tone = TvPillTone.CYAN,
                            onClick = onNavigateToServers
                        )
                    }

                    TvPill(
                        text = "${config.appLanguage.effectiveFlag()} ${config.appLanguage.effectiveShortBadge()}",
                        scale = scale,
                        tone = TvPillTone.NEUTRAL,
                        onClick = onOpenLanguagePicker
                    )

                    TvPill(
                        text = if (config.isEffectiveTranslateEnabled()) {
                            config.tr("Dịch: Bật", "Translate: On")
                        } else {
                            config.tr("Dịch", "Translate")
                        },
                        scale = scale,
                        tone = if (config.isEffectiveTranslateEnabled()) TvPillTone.GREEN else TvPillTone.NEUTRAL,
                        onClick = onOpenTranslator
                    )

                    TvPill(
                        text = "QR",
                        scale = scale,
                        tone = TvPillTone.NEUTRAL,
                        onClick = onOpenWebPortal
                    )

                    TvPill(
                        text = "?",
                        scale = scale,
                        tone = TvPillTone.NEUTRAL,
                        onClick = { onOpenGuide(GuideSection.QUICK_START) }
                    )
                }
            }
        }

        // 2. Hero Card (.hero) matching tv-mockup.html
        item(key = "tv_home_hero") {
            val heroCorner = (22.dp * scale).coerceAtLeast(16.dp)
            val heroBrush = remember {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF12301F),
                        Color(0xFF0D1B14),
                        Color(0xFF0B1511)
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = (186.dp * scale).coerceAtLeast(140.dp))
                    .clip(RoundedCornerShape(heroCorner))
                    .background(heroBrush)
                    .padding(
                        horizontal = (28.dp * scale).coerceAtLeast(18.dp),
                        vertical = (24.dp * scale).coerceAtLeast(16.dp)
                    )
                    .testTag("tv_home_hero_card"),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy((6.dp * scale).coerceAtLeast(4.dp))) {
                    Text(
                        text = config.tr("Chơi Xbox Cloud", "Play Xbox Cloud"),
                        color = TvInk,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = (34f * scale).coerceAtLeast(22f).sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = config.tr(
                            "Hàng trăm game, ngay trên màn hình lớn.",
                            "Hundreds of console games, right on the big screen."
                        ),
                        color = Color(0xFFB6CBC0),
                        fontSize = (17f * scale).coerceAtLeast(13f).sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height((18.dp * scale).coerceAtLeast(12.dp)))

                Row(
                    horizontalArrangement = Arrangement.spacedBy((12.dp * scale).coerceAtLeast(8.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvButton(
                        text = config.tr("Chơi ngay", "Play Now"),
                        leadingText = "▶",
                        variant = TvButtonVariant.PRIMARY,
                        scale = scale,
                        focusRequester = defaultFocusRequester,
                        onClick = onOpenXboxCloud,
                        modifier = Modifier.testTag("tv_hero_play_now_btn")
                    )

                    TvButton(
                        text = "Fortnite",
                        variant = TvButtonVariant.DEFAULT,
                        scale = scale,
                        onClick = {
                            onLaunchGame("https://www.xbox.com/en-US/play/games/fortnite/BT5P2X999VH2")
                        },
                        modifier = Modifier.testTag("tv_hero_fortnite_btn")
                    )

                    val webUiLabel = if (config.xboxWebUiMode == XboxWebUiMode.CLASSIC) {
                        config.tr("Giao diện Xbox: Cũ", "Xbox UI: Classic")
                    } else {
                        config.tr("Giao diện Xbox: Mới", "Xbox UI: New")
                    }
                    TvButton(
                        text = webUiLabel,
                        variant = TvButtonVariant.DEFAULT,
                        scale = scale,
                        onClick = {
                            val nextMode = if (config.xboxWebUiMode == XboxWebUiMode.CLASSIC) {
                                XboxWebUiMode.NEW_UI
                            } else {
                                XboxWebUiMode.CLASSIC
                            }
                            SettingsRepository.updateXboxWebUiMode(nextMode)
                        },
                        modifier = Modifier.testTag("tv_hero_web_ui_mode_btn")
                    )

                    TvButton(
                        text = if (isVpnConnected) {
                            config.tr("Tắt VPN", "Turn Off VPN")
                        } else {
                            config.tr("Bật VPN", "Turn On VPN")
                        },
                        variant = if (isVpnConnected) TvButtonVariant.DANGER else TvButtonVariant.DEFAULT,
                        scale = scale,
                        onClick = {
                            if (isVpnConnected) onDisconnectVpn() else onConnectVpn()
                        },
                        modifier = Modifier.testTag("tv_hero_vpn_toggle_btn")
                    )
                }
            }
        }

        // 3. Strip "Tiếp tục chơi" (.strip & .cards -> .rc)
        if (continuePlayingItems.isNotEmpty()) {
            item(key = "tv_home_continue_strip") {
                Column(verticalArrangement = Arrangement.spacedBy((10.dp * scale).coerceAtLeast(7.dp))) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = config.tr("Tiếp tục chơi", "Continue Playing"),
                            color = TvInk,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = (20f * scale).coerceAtLeast(15.5f).sp,
                            fontWeight = FontWeight.Bold
                        )
                        TvChip(
                            text = config.tr("Xóa danh sách", "Clear list"),
                            selected = false,
                            scale = scale * 0.88f,
                            onClick = { GameLibraryRepository.clearPlayedGames() }
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy((14.dp * scale).coerceAtLeast(10.dp)),
                        contentPadding = PaddingValues(end = (8.dp * scale).coerceAtLeast(4.dp))
                    ) {
                        items(
                            items = continuePlayingItems,
                            key = { (game, _) -> "tv_continue_${game.id}" }
                        ) { (game, subLabel) ->
                            TvHomeRecentCard(
                                game = game,
                                subtitle = subLabel,
                                scale = scale,
                                onClick = { onLaunchGame(game.playUrl) }
                            )
                        }
                    }
                }
            }
        }

        // 4. Strip "Đề xuất" (hoặc hiển thị ngay vị trí Tiếp tục chơi nếu chưa có lịch sử)
        item(key = "tv_home_recommended_strip") {
            Column(verticalArrangement = Arrangement.spacedBy((10.dp * scale).coerceAtLeast(7.dp))) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (continuePlayingItems.isEmpty()) {
                            config.tr("Tiếp tục chơi · Đề xuất", "Continue Playing · Featured")
                        } else {
                            config.tr("Đề xuất cho bạn", "Recommended for You")
                        },
                        color = TvInk,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = (20f * scale).coerceAtLeast(15.5f).sp,
                        fontWeight = FontWeight.Bold
                    )
                    TvChip(
                        text = config.tr("Xem thư viện →", "All games →"),
                        selected = false,
                        scale = scale * 0.88f,
                        onClick = onNavigateToLibrary
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy((14.dp * scale).coerceAtLeast(10.dp)),
                    contentPadding = PaddingValues(end = (8.dp * scale).coerceAtLeast(4.dp))
                ) {
                    items(
                        items = recommendedGames,
                        key = { game -> "tv_rec_${game.id}" }
                    ) { game ->
                        TvHomeRecentCard(
                            game = game,
                            subtitle = config.tr(game.category, game.categoryEn),
                            scale = scale,
                            onClick = { onLaunchGame(game.playUrl) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvHomeRecentCard(
    game: GameBookmark,
    subtitle: String,
    scale: Float,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val cardW = (196.dp * scale).coerceIn(146.dp, 250.dp)
    val cardH = (106.dp * scale).coerceIn(82.dp, 136.dp)
    val corner = (16.dp * scale).coerceAtLeast(12.dp)

    val imageRequest = remember(game.imageUrl) {
        game.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .size(320, 180)
                .allowRgb565(true)
                .crossfade(false)
                .build()
        }
    }

    Box(
        modifier = Modifier
            .width(cardW)
            .height(cardH)
            .clip(RoundedCornerShape(corner))
            .background(TvSurface)
            .gamepadClickable(
                cornerRadius = corner,
                onClick = onClick
            )
            .testTag("tv_home_game_card_${game.id}")
    ) {
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                alpha = 0.34f,
                modifier = Modifier.fillMaxSize()
            )
        } else if (game.drawableResId != null) {
            Image(
                painter = painterResource(id = game.drawableResId),
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                alpha = 0.34f,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            TvSurface.copy(alpha = 0.88f)
                        )
                    )
                )
                .padding((13.dp * scale).coerceAtLeast(9.dp)),
            contentAlignment = Alignment.BottomStart
        ) {
            Column(verticalArrangement = Arrangement.spacedBy((2.dp * scale).coerceAtLeast(1.dp))) {
                Text(
                    text = game.title,
                    color = TvInk,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = (16.5f * scale).coerceAtLeast(13f).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = TvMuted,
                    fontSize = (13.5f * scale).coerceAtLeast(11f).sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
