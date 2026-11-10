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
private val QUICK_RESUME_SHAPE = RoundedCornerShape(10.dp)
private val QUICK_RESUME_ICON_SHAPE = RoundedCornerShape(7.dp)
private val RECENT_CHIP_SHAPE = RoundedCornerShape(8.dp)
private val GAME_CARD_SHAPE = RoundedCornerShape(10.dp)
private val GAME_BADGE_SHAPE = RoundedCornerShape(bottomEnd = 7.dp)

@Composable
internal fun FeaturedGamesSection(
    featuredGames: List<GameBookmark>,
    favoriteIds: Set<String>,
    lastPlayedGame: GameBookmark?,
    recentGames: List<GameBookmark> = emptyList(),
    onLaunchBookmark: (GameBookmark) -> Unit,
    onToggleFavorite: (GameBookmark) -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    val config by SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        // Hàng tiêu đề: GAME NỔI BẬT (weight 1f) | Tất cả (luôn được đo trước, không bao giờ bị che/cắt)
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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(3.dp, 14.dp)
                        .background(XboxNeonGreen, RoundedCornerShape(2.dp))
                )
                Text(
                    text = stringResource(R.string.featured_games_title).uppercase(),
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.3.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier
                    .clip(VIEW_ALL_SHAPE)
                    .background(XboxSurfaceVariant, VIEW_ALL_SHAPE)
                    .border(1.dp, XboxCardBorder, VIEW_ALL_SHAPE)
                    .clickable { onNavigateToLibrary() }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_view_all),
                    color = XboxNeonGreen,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = XboxNeonGreen,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Thẻ "Chơi tiếp" cho game vừa chơi gần nhất
        if (lastPlayedGame != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(QUICK_RESUME_SHAPE)
                    .background(XboxSurfaceVariant, QUICK_RESUME_SHAPE)
                    .border(1.dp, XboxCardBorder, QUICK_RESUME_SHAPE)
                    .clickable { onLaunchBookmark(lastPlayedGame) }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .testTag("continue_last_game_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(DirectCyan.copy(alpha = 0.14f), QUICK_RESUME_ICON_SHAPE)
                            .border(0.8.dp, DirectCyan.copy(alpha = 0.45f), QUICK_RESUME_ICON_SHAPE),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = null,
                            tint = DirectCyan,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = config.tr("CHƠI TIẾP NGAY", "QUICK RESUME"),
                            color = DirectCyan,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = lastPlayedGame.title,
                            color = TextPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .background(XboxGreen, VIEW_ALL_SHAPE)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.btn_continue_game),
                        color = Color.White,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            if (recentGames.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 1.dp)
                ) {
                    items(recentGames.drop(1), key = { "recent_${it.id}" }) { recent ->
                        Row(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 32.dp)
                                .clip(RECENT_CHIP_SHAPE)
                                .background(XboxSurface, RECENT_CHIP_SHAPE)
                                .border(1.dp, XboxCardBorder, RECENT_CHIP_SHAPE)
                                .clickable { onLaunchBookmark(recent) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("recent_quick_chip_${recent.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = DirectCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = recent.title,
                                color = TextPrimary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
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
            val gridRows = remember(featuredGames) {
                featuredGames.take(6).chunked(2)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                gridRows.forEach { rowGames ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowGames.forEach { game ->
                            FeaturedGameCard(
                                game = game,
                                isFavorite = game.id in favoriteIds,
                                isEn = isEn,
                                isImportingCatalog = isImportingCatalog,
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

@Composable
internal fun FeaturedGameCard(
    game: GameBookmark,
    isFavorite: Boolean,
    isEn: Boolean,
    isImportingCatalog: Boolean,
    onToggleFavorite: () -> Unit,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val localizedBadge = game.localizedBadge(isEn)
    val localizedCategory = game.localizedCategory(isEn)
    val monogram = game.monogram
    var coverImageLoaded by remember(game.id, game.imageUrl) {
        mutableStateOf(false)
    }

    Column(
        modifier = modifier
            .clip(GAME_CARD_SHAPE)
            .background(XboxSurface, GAME_CARD_SHAPE)
            .border(1.dp, XboxCardBorder, GAME_CARD_SHAPE)
            .clickable { onLaunch() }
            .testTag("featured_game_${game.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .background(XboxSurfaceVariant)
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
                        fontSize = 15.sp,
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
                            .size(280, 380)
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

            if (localizedBadge != null) {
                Text(
                    text = localizedBadge,
                    color = XboxNeonGreen,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(Color(0xD9050A07), GAME_BADGE_SHAPE)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }

            if (game.supportsTouch) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = "Touch",
                    tint = DirectCyan,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .background(Color(0xCC060B08), CircleShape)
                        .padding(3.dp)
                        .size(12.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(36.dp)
                    .clickable { onToggleFavorite() }
                    .testTag("fav_featured_${game.id}"),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(26.dp)
                        .background(Color(0xCC060B08), CircleShape),
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
        }

        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = game.title,
                color = TextPrimary,
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = localizedCategory,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = GoldStar,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = game.rating,
                        color = TextSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

