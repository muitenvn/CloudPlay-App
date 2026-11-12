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
    @Suppress("UNUSED_PARAMETER") recentGames: List<GameBookmark> = emptyList(),
    onLaunchBookmark: (GameBookmark) -> Unit,
    onToggleFavorite: (GameBookmark) -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    val config by SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    val context = LocalContext.current

    val continueGame = lastPlayedGame ?: featuredGames.firstOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Thẻ "Chơi tiếp" (Continue card) với ảnh vuông bên trái + tên game ở giữa + nút "Tiếp" bên phải
        if (continueGame != null) {
            var thumbLoaded by remember(continueGame.id, continueGame.imageUrl) { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(QUICK_RESUME_SHAPE)
                    .background(XboxSurface, QUICK_RESUME_SHAPE)
                    .border(1.dp, XboxCardBorder, QUICK_RESUME_SHAPE)
                    .clickable { onLaunchBookmark(continueGame) }
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
                            text = continueGame.monogram,
                            color = TextPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    if (!continueGame.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = remember(continueGame.imageUrl) {
                                ImageRequest.Builder(context)
                                    .data(continueGame.imageUrl)
                                    .size(160, 160)
                                    .allowRgb565(true)
                                    .crossfade(false)
                                    .build()
                            },
                            contentDescription = continueGame.title,
                            contentScale = ContentScale.Crop,
                            onSuccess = { thumbLoaded = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (continueGame.drawableResId != null) {
                        Image(
                            painter = painterResource(id = continueGame.drawableResId),
                            contentDescription = continueGame.title,
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
                        text = if (lastPlayedGame != null) {
                            config.tr("Chơi tiếp", "Continue playing")
                        } else {
                            config.tr("Vào nhanh", "Quick launch")
                        },
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = continueGame.title,
                        color = TextPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = continueGame.localizedCategory(isEn),
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
                        text = config.tr("Tiếp", "Play"),
                        color = Color(0xFF03140B),
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
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

