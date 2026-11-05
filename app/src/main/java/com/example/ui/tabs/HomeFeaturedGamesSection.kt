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
import com.example.ui.theme.XboxDark
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant
import com.example.ui.theme.rememberXboxMotionEnabled
import com.example.ui.theme.xboxPressScale

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
    val motionEnabled = rememberXboxMotionEnabled(
        lowEndDeviceMode = config.lowEndDeviceMode,
        gpuLoadReductionEnabled = config.gpuLoadReductionEnabled
    )

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
                    .xboxPressScale(enabled = motionEnabled, pressedScale = 0.95f)
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
                    .xboxPressScale(enabled = motionEnabled, pressedScale = 0.97f)
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
                                    text = config.tr("CHƠI TIẾP NGAY", "QUICK RESUME"),
                                    color = DirectCyan,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = config.tr("• STREAM 60FPS", "• 60FPS STREAM"),
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

            if (recentGames.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(recentGames.drop(1), key = { "recent_${it.id}" }) { recent ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = XboxSurface.copy(alpha = 0.9f),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxCardBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onLaunchBookmark(recent) }
                                .testTag("recent_quick_chip_${recent.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = DirectCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = recent.title,
                                    color = TextPrimary,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        if (featuredGames.isEmpty()) {
            val isSyncing by GameLibraryRepository.isSyncingCatalog.collectAsState()
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
            }
        } else {
            val isImportingCatalog by GameLibraryRepository.isSyncingCatalog.collectAsState()
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(start = 2.dp, end = 12.dp)
            ) {
                items(
                    items = featuredGames,
                    key = { it.id },
                    contentType = { "featured_game_card" }
                ) { game ->
                    FeaturedGameCard(
                        game = game,
                        isFavorite = game.id in favoriteIds,
                        isEn = isEn,
                        isImportingCatalog = isImportingCatalog,
                        onToggleFavorite = { onToggleFavorite(game) },
                        onLaunch = { onLaunchBookmark(game) }
                    )
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
    onLaunch: () -> Unit
) {
    val localizedBadge = game.localizedBadge(isEn)
    val localizedCategory = game.localizedCategory(isEn)
    val monogram = game.monogram
    val secondaryColorHex = game.secondaryColorHex
    val cardBorderBrush = remember(game.accentColorHex) {
        Brush.verticalGradient(
            listOf(
                Color(game.accentColorHex).copy(alpha = 0.65f),
                XboxCardBorder,
                DirectCyan.copy(alpha = 0.4f)
            )
        )
    }
    val fallbackBgBrush = remember(game.accentColorHex, secondaryColorHex) {
        Brush.linearGradient(
            listOf(
                Color(game.accentColorHex).copy(alpha = 0.65f),
                Color(secondaryColorHex).copy(alpha = 0.35f),
                XboxDark
            )
        )
    }
    val bottomScrimBrush = remember {
        Brush.verticalGradient(
            listOf(Color.Transparent, XboxSurface.copy(alpha = 0.94f)),
            startY = 115f
        )
    }
    var coverImageLoaded by remember(game.id, game.imageUrl) {
        mutableStateOf(false)
    }
    Card(
        modifier = Modifier
            .width(156.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.1.dp, cardBorderBrush, RoundedCornerShape(18.dp))
            .clickable { onLaunch() }
            .testTag("featured_game_${game.id}"),
        colors = CardDefaults.cardColors(containerColor = XboxSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(fallbackBgBrush),
                    contentAlignment = Alignment.Center
                ) {
                    if (isImportingCatalog || (game.imageUrl.isNullOrBlank() && game.drawableResId == null) || !coverImageLoaded) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xAA060B08),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.2.dp,
                                    Color(game.accentColorHex).copy(alpha = 0.85f)
                                ),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = monogram,
                                        color = Color.White,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = 15.sp,
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
                                .size(320, 440)
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

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bottomScrimBrush)
                )

                if (localizedBadge != null) {
                    Surface(
                        shape = RoundedCornerShape(bottomEnd = 10.dp),
                        color = Color(0xF0050A07),
                        border = androidx.compose.foundation.BorderStroke(0.7.dp, XboxNeonGreen.copy(alpha = 0.65f)),
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
