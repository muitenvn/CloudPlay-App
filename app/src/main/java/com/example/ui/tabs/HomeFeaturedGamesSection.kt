package com.example.ui.tabs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
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
import com.example.data.GameBookmark
import com.example.data.GameLibraryRepository
import com.example.data.SettingsRepository
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.GoldStar
import com.example.ui.theme.JapanRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant

private val VIEW_ALL_SHAPE = RoundedCornerShape(8.dp)
private val QUICK_RESUME_SHAPE = RoundedCornerShape(12.dp)
private val QUICK_RESUME_ICON_SHAPE = RoundedCornerShape(10.dp)
private val RECENT_CHIP_SHAPE = RoundedCornerShape(8.dp)
private val GAME_CARD_SHAPE = RoundedCornerShape(12.dp)
private val GAME_BADGE_SHAPE = RoundedCornerShape(999.dp)

@Composable
internal fun FeaturedGamesSection(
    featuredGames: List<GameBookmark>,
    favoriteIds: Set<String>,
    lastPlayedGame: GameBookmark?,
    recentGames: List<GameBookmark> = emptyList(),
    onLaunchBookmark: (GameBookmark) -> Unit,
    onToggleFavorite: (GameBookmark) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onRemovePlayedGame: (GameBookmark) -> Unit = {},
    onClearPlayedGames: () -> Unit = {}
) {
    val config by SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    val context = LocalContext.current

    val orderedPlayedGames = remember(lastPlayedGame, recentGames) {
        val list = ArrayList<GameBookmark>()
        val seen = HashSet<String>()
        if (lastPlayedGame != null && seen.add(lastPlayedGame.id.lowercase())) {
            list.add(lastPlayedGame)
        }
        for (g in recentGames) {
            if (seen.add(g.id.lowercase())) {
                list.add(g)
            }
        }
        list.toList()
    }
    val primaryContinueGame = orderedPlayedGames.firstOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Mục "Tiếp tục chơi" — danh sách riêng các game đã thực sự chơi trên Xbox (tự nhận diện từ đường link)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_continue_playing_section"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = config.tr("Tiếp tục chơi", "Continue Playing"),
                        color = TextPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .clip(GAME_BADGE_SHAPE)
                            .background(
                                if (orderedPlayedGames.isNotEmpty()) Color(0xFF16352A) else XboxSurfaceVariant,
                                GAME_BADGE_SHAPE
                            )
                            .border(
                                1.dp,
                                if (orderedPlayedGames.isNotEmpty()) XboxNeonGreen else XboxCardBorder,
                                GAME_BADGE_SHAPE
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (orderedPlayedGames.isNotEmpty()) {
                                "${orderedPlayedGames.size} game"
                            } else {
                                config.tr("Tự nhận diện link", "Auto-detect URL")
                            },
                            color = if (orderedPlayedGames.isNotEmpty()) XboxNeonGreen else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (orderedPlayedGames.isNotEmpty()) {
                    Text(
                        text = config.tr("Xóa danh sách", "Clear list"),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(VIEW_ALL_SHAPE)
                            .clickable { onClearPlayedGames() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("clear_played_games_button")
                    )
                }
            }

            if (primaryContinueGame != null) {
                // Thẻ nổi bật của game vừa chơi gần nhất trên Xbox
                var thumbLoaded by remember(primaryContinueGame.id, primaryContinueGame.imageUrl) { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(QUICK_RESUME_SHAPE)
                        .background(XboxSurface, QUICK_RESUME_SHAPE)
                        .border(1.dp, XboxNeonGreen.copy(alpha = 0.65f), QUICK_RESUME_SHAPE)
                        .clickable { onLaunchBookmark(primaryContinueGame) }
                        .padding(10.dp)
                        .testTag("continue_last_game_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(QUICK_RESUME_ICON_SHAPE)
                            .background(Color(0xFF1F4E5F), QUICK_RESUME_ICON_SHAPE)
                            .border(1.dp, XboxCardBorder, QUICK_RESUME_ICON_SHAPE),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!thumbLoaded) {
                            Text(
                                text = primaryContinueGame.monogram,
                                color = TextPrimary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        if (!primaryContinueGame.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = remember(primaryContinueGame.imageUrl) {
                                    ImageRequest.Builder(context)
                                        .data(primaryContinueGame.imageUrl)
                                        .size(160, 160)
                                        .allowRgb565(true)
                                        .crossfade(false)
                                        .build()
                                },
                                contentDescription = primaryContinueGame.title,
                                contentScale = ContentScale.Crop,
                                onSuccess = { thumbLoaded = true },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (primaryContinueGame.drawableResId != null) {
                            Image(
                                painter = painterResource(id = primaryContinueGame.drawableResId),
                                contentDescription = primaryContinueGame.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = config.tr(
                                "Đang chơi gần nhất • Tự nhận diện trên Xbox",
                                "Last played • Auto-detected on Xbox"
                            ),
                            color = XboxNeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = primaryContinueGame.title,
                            color = TextPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = primaryContinueGame.localizedCategory(isEn),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .clip(RECENT_CHIP_SHAPE)
                            .background(XboxNeonGreen, RECENT_CHIP_SHAPE)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF03140B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = config.tr("Chơi tiếp", "Resume"),
                            color = Color(0xFF03140B),
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Danh sách riêng các game đã chơi (hiển thị đầy đủ cả khi có từ 1 game trở lên)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_continue_playing_list")
                ) {
                    items(orderedPlayedGames, key = { "played_${it.id}" }) { playedGame ->
                        var itemThumbLoaded by remember(playedGame.id, playedGame.imageUrl) { mutableStateOf(false) }
                        val isLatest = playedGame.id.equals(primaryContinueGame.id, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .width(228.dp)
                                .clip(QUICK_RESUME_SHAPE)
                                .background(
                                    if (isLatest) Color(0xFF102119) else XboxSurfaceVariant,
                                    QUICK_RESUME_SHAPE
                                )
                                .border(
                                    1.dp,
                                    if (isLatest) XboxNeonGreen.copy(alpha = 0.5f) else XboxCardBorder,
                                    QUICK_RESUME_SHAPE
                                )
                                .clickable { onLaunchBookmark(playedGame) }
                                .padding(8.dp)
                                .testTag("continue_played_item_${playedGame.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(QUICK_RESUME_ICON_SHAPE)
                                    .background(Color(0xFF1F4E5F), QUICK_RESUME_ICON_SHAPE)
                                    .border(1.dp, XboxCardBorder, QUICK_RESUME_ICON_SHAPE),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!itemThumbLoaded) {
                                    Text(
                                        text = playedGame.monogram,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (!playedGame.imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = remember(playedGame.imageUrl) {
                                            ImageRequest.Builder(context)
                                                .data(playedGame.imageUrl)
                                                .size(120, 120)
                                                .allowRgb565(true)
                                                .crossfade(false)
                                                .build()
                                        },
                                        contentDescription = playedGame.title,
                                        contentScale = ContentScale.Crop,
                                        onSuccess = { itemThumbLoaded = true },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Text(
                                    text = playedGame.title,
                                    color = TextPrimary,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isLatest) {
                                        config.tr("Vừa chơi gần nhất", "Most recent")
                                    } else {
                                        config.tr("Đã chơi trên Xbox", "Played on Xbox")
                                    },
                                    color = if (isLatest) XboxNeonGreen else TextSecondary,
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0D1511), CircleShape)
                                    .border(1.dp, XboxCardBorder, CircleShape)
                                    .clickable { onRemovePlayedGame(playedGame) }
                                    .testTag("remove_played_${playedGame.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "×",
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // Trạng thái khi chưa có game nào được chơi thật trên Xbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(QUICK_RESUME_SHAPE)
                        .background(XboxSurface, QUICK_RESUME_SHAPE)
                        .border(1.dp, XboxCardBorder, QUICK_RESUME_SHAPE)
                        .padding(12.dp)
                        .testTag("continue_playing_empty_card"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(QUICK_RESUME_ICON_SHAPE)
                            .background(Color(0xFF142019), QUICK_RESUME_ICON_SHAPE)
                            .border(1.dp, XboxCardBorder, QUICK_RESUME_ICON_SHAPE),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = null,
                            tint = XboxNeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = config.tr("Chưa có game trong Tiếp tục chơi", "No games in Continue Playing yet"),
                            color = TextPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = config.tr(
                                "Khi bạn vào Xbox và thực sự khởi chạy một game, ứng dụng sẽ tự lấy thông tin từ đường link và ghi vào danh sách này.",
                                "When you enter Xbox and actually launch a game stream, the app automatically reads the URL and adds it here."
                            ),
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // 2. Hàng tiêu đề: Game nổi bật | Tất cả ›
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = config.tr("Game nổi bật", "Featured Games"),
                color = TextPrimary,
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier
                    .clip(VIEW_ALL_SHAPE)
                    .clickable { onNavigateToLibrary() }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = config.tr("Tất cả", "See all"),
                    color = XboxNeonGreen,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = XboxNeonGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (featuredGames.isEmpty()) {
            val isSyncing by GameLibraryRepository.isSyncingCatalog.collectAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(XboxSurface, QUICK_RESUME_SHAPE)
                    .border(1.dp, XboxCardBorder, QUICK_RESUME_SHAPE)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Gamepad,
                    contentDescription = null,
                    tint = DirectCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isSyncing) {
                        config.tr("Đang tải game...", "Loading games...")
                    } else {
                        config.tr("Chưa có game", "No games yet")
                    },
                    color = TextSecondary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            val isImportingCatalog by GameLibraryRepository.isSyncingCatalog.collectAsState()
            val carouselGames = remember(featuredGames) { featuredGames.take(4) }
            val gridRows = remember(featuredGames) { featuredGames.drop(4).take(4).chunked(2) }

            // 3. Hàng Game nổi bật vuốt ngang (LazyRow) có nút ▶ tròn xanh trên mỗi thẻ
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(carouselGames, key = { "carousel_${it.id}" }) { game ->
                    FeaturedGameCard(
                        game = game,
                        isFavorite = game.id in favoriteIds,
                        isEn = isEn,
                        isImportingCatalog = isImportingCatalog,
                        showPlayCircleButton = true,
                        onToggleFavorite = { onToggleFavorite(game) },
                        onLaunch = { onLaunchBookmark(game) },
                        modifier = Modifier.width(216.dp)
                    )
                }
            }

            // 4. Lưới 2 cột (grid2) bên dưới hàng vuốt ngang
            if (gridRows.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    gridRows.forEach { rowGames ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowGames.forEach { game ->
                                FeaturedGameCard(
                                    game = game,
                                    isFavorite = game.id in favoriteIds,
                                    isEn = isEn,
                                    isImportingCatalog = isImportingCatalog,
                                    showPlayCircleButton = false,
                                    onToggleFavorite = { onToggleFavorite(game) },
                                    onLaunch = { onLaunchBookmark(game) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowGames.size == 1) {
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun FeaturedGameCard(
    game: GameBookmark,
    isFavorite: Boolean,
    isEn: Boolean,
    isImportingCatalog: Boolean,
    showPlayCircleButton: Boolean = false,
    onToggleFavorite: () -> Unit,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val localizedCategory = game.localizedCategory(isEn)
    val monogram = game.monogram
    var coverImageLoaded by remember(game.id, game.imageUrl) {
        mutableStateOf(false)
    }

    Box(
        modifier = modifier
            .height(if (showPlayCircleButton) 144.dp else 118.dp)
            .clip(GAME_CARD_SHAPE)
            .background(XboxSurfaceVariant, GAME_CARD_SHAPE)
            .border(1.dp, XboxCardBorder, GAME_CARD_SHAPE)
            .clickable { onLaunch() }
            .testTag("featured_game_${game.id}")
    ) {
        if (isImportingCatalog || (game.imageUrl.isNullOrBlank() && game.drawableResId == null) || !coverImageLoaded) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = monogram,
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        if (!isImportingCatalog && !game.imageUrl.isNullOrBlank()) {
            val context = LocalContext.current
            AsyncImage(
                model = remember(game.imageUrl) {
                    ImageRequest.Builder(context)
                        .data(game.imageUrl)
                        .size(320, 220)
                        .allowRgb565(true)
                        .crossfade(false)
                        .build()
                },
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                onSuccess = { coverImageLoaded = true },
                onError = { coverImageLoaded = false },
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

        // Bottom dark gradient scrim for readable overlaid text
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x99060A08),
                            Color(0xEB060A08)
                        )
                    )
                )
        )

        // Top-Left Rating Pill ("★ 4.6")
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(GAME_BADGE_SHAPE)
                .background(Color(0xD1060A08), GAME_BADGE_SHAPE)
                .border(1.dp, XboxCardBorder, GAME_BADGE_SHAPE)
                .padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "★",
                color = GoldStar,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = game.rating,
                color = TextPrimary,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }

        // Top-Right Favorite Button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(38.dp)
                .clickable { onToggleFavorite() }
                .testTag("fav_featured_${game.id}"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xD1060A08), CircleShape)
                    .border(1.dp, XboxCardBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isEn) "Favorite" else "Yêu thích",
                    tint = if (isFavorite) JapanRed else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Bottom Overlaid Title + Category (+ Round Play Button on Carousel Cards)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = game.title,
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = if (showPlayCircleButton) 14.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = localizedCategory,
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (showPlayCircleButton) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(XboxNeonGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF03140B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

