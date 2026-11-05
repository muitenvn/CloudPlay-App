package com.example.ui.tabs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.GoldStar
import com.example.ui.theme.JapanRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxDark
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface

@Composable
internal fun LibraryGameCard(
    game: GameBookmark,
    isFavorite: Boolean,
    isEn: Boolean = false,
    isImportingCatalog: Boolean = false,
    onToggleFavorite: () -> Unit,
    onShowDetail: () -> Unit,
    onPlay: () -> Unit,
    onDeleteCustom: (() -> Unit)? = null
) {
    val localizedBadge = game.localizedBadge(isEn)
    val localizedCategory = game.localizedCategory(isEn)
    val monogram = game.monogram
    val secondaryGradientColor = Color(game.secondaryColorHex)
    val cardBorderBrush = remember(game.accentColorHex) {
        Brush.verticalGradient(
            listOf(XboxCardBorder, Color(game.accentColorHex).copy(alpha = 0.65f))
        )
    }
    val fallbackBgBrush = remember(game.accentColorHex, game.secondaryColorHex) {
        Brush.linearGradient(
            listOf(
                Color(game.accentColorHex).copy(alpha = 0.62f),
                secondaryGradientColor.copy(alpha = 0.45f),
                XboxDark
            )
        )
    }
    val monogramRingBrush = remember(game.accentColorHex) {
        Brush.linearGradient(
            listOf(Color(game.accentColorHex), DirectCyan)
        )
    }
    val vignetteScrimBrush = remember {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color(0x4D000000),
                0.45f to Color.Transparent,
                0.78f to Color(0xD9060A08),
                1.0f to Color(0xF7060A08)
            )
        )
    }
    var coverImageLoaded by remember(game.imageUrl) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, cardBorderBrush, RoundedCornerShape(20.dp))
            .clickable { onPlay() },
        colors = CardDefaults.cardColors(containerColor = XboxSurface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient + Initial Monogram fallback (always shown when cover URL is missing, importing, loading, or failed)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fallbackBgBrush)
            ) {
                if (isImportingCatalog || (game.imageUrl.isNullOrBlank() && game.drawableResId == null) || !coverImageLoaded) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(start = 12.dp, end = 12.dp, bottom = 34.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xAA060B08),
                            border = androidx.compose.foundation.BorderStroke(
                                1.4.dp,
                                monogramRingBrush
                            ),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = monogram,
                                    color = Color.White,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        if (!game.publisher.isNullOrBlank()) {
                            Text(
                                text = game.publisher,
                                color = DirectCyan.copy(alpha = 0.95f),
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Cover Art: loaded when available; updates coverImageLoaded on success so fallback stays visible on error
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

            // High-Contrast Console Vignette Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(vignetteScrimBrush)
            )

            // Top Badges Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(9.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = Color(0xCC060B08),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, GoldStar.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = GoldStar,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = game.rating,
                                color = Color.White,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (game.supportsTouch) {
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = Color(0xCC060B08),
                            border = androidx.compose.foundation.BorderStroke(0.6.dp, DirectCyan.copy(alpha = 0.55f))
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TouchApp,
                                    contentDescription = "Touch",
                                    tint = DirectCyan,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xCC060B08),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, XboxCardBorder),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onShowDetail() }
                            .testTag("info_library_${game.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = if (isEn) "Xbox Info" else "Thông tin Xbox",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier
                                .padding(5.dp)
                                .size(13.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0xCC060B08),
                        border = androidx.compose.foundation.BorderStroke(
                            0.6.dp,
                            if (isFavorite) JapanRed.copy(alpha = 0.75f) else XboxCardBorder
                        ),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onToggleFavorite() }
                            .testTag("fav_library_${game.id}")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isEn) "Favorite" else "Yêu thích",
                            tint = if (isFavorite) JapanRed else Color.White.copy(alpha = 0.75f),
                            modifier = Modifier
                                .padding(5.dp)
                                .size(13.dp)
                        )
                    }

                    if (onDeleteCustom != null) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xCC1E090D),
                            border = androidx.compose.foundation.BorderStroke(0.6.dp, JapanRed.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onDeleteCustom() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = stringResource(R.string.btn_delete),
                                tint = JapanRed,
                                modifier = Modifier
                                    .padding(5.dp)
                                    .size(13.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Info & Play Trigger (Uniform height across all 2-column cards)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(11.dp)
                    .align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val badgeText = localizedBadge ?: if (game.freeToPlay) {
                    stringResource(R.string.badge_free)
                } else {
                    "XBOX CLOUD"
                }
                val badgeAccentColor = if (localizedBadge != null || game.freeToPlay) XboxNeonGreen else DirectCyan.copy(alpha = 0.9f)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xEB050A07),
                    border = androidx.compose.foundation.BorderStroke(0.6.dp, badgeAccentColor.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = badgeText,
                        color = badgeAccentColor,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = game.title,
                    color = Color.White,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp,
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
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = XboxGreen,
                        border = androidx.compose.foundation.BorderStroke(0.7.dp, XboxNeonGreen.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "PLAY",
                                color = Color.White,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
