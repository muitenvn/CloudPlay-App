package com.example.ui.tabs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import com.example.ui.theme.GoldStar
import com.example.ui.theme.JapanRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxDark
import com.example.ui.theme.XboxSurfaceVariant

@OptIn(ExperimentalFoundationApi::class)
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
    val localizedCategory = game.localizedCategory(isEn)
    val monogram = game.monogram
    val secondaryGradientColor = Color(game.secondaryColorHex)
    val fallbackBgBrush = remember(game.accentColorHex, game.secondaryColorHex) {
        Brush.linearGradient(
            listOf(
                Color(game.accentColorHex).copy(alpha = 0.65f),
                secondaryGradientColor.copy(alpha = 0.45f),
                XboxDark
            )
        )
    }
    val vignetteScrimBrush = remember {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color.Transparent,
                0.52f to Color(0x8C060A08),
                1.0f to Color(0xF5060A08)
            )
        )
    }
    var coverImageLoaded by remember(game.imageUrl) { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(cardShape)
            .background(XboxSurfaceVariant, cardShape)
            .border(1.dp, XboxCardBorder, cardShape)
            .combinedClickable(
                onClick = onPlay,
                onLongClick = onShowDetail
            )
            .testTag("library_card_${game.id}")
    ) {
        // Fallback Monogram Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(fallbackBgBrush),
            contentAlignment = Alignment.Center
        ) {
            if ((game.imageUrl.isNullOrBlank() && game.drawableResId == null) || !coverImageLoaded) {
                Text(
                    text = monogram,
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }
        }

        // Cover Art
        if (!game.imageUrl.isNullOrBlank()) {
            val context = LocalContext.current
            AsyncImage(
                model = remember(game.imageUrl) {
                    ImageRequest.Builder(context)
                        .data(game.imageUrl)
                        .size(260, 350)
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

        // Dark Bottom Gradient Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(vignetteScrimBrush)
        )

        // Top Row: Rating Pill ("★ 4.2") + Heart Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(7.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color(0xD1060A08),
                border = androidx.compose.foundation.BorderStroke(1.dp, XboxCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "★",
                        color = GoldStar,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = game.rating,
                        color = TextPrimary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (onDeleteCustom != null) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xD11E090D),
                        border = androidx.compose.foundation.BorderStroke(1.dp, JapanRed.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { onDeleteCustom() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = stringResource(R.string.btn_delete),
                                tint = JapanRed,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xD1060A08),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isFavorite) JapanRed.copy(alpha = 0.75f) else XboxCardBorder
                    ),
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { onToggleFavorite() }
                        .testTag("fav_library_${game.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isEn) "Favorite" else "Yêu thích",
                            tint = if (isFavorite) JapanRed else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Bottom Overlaid Title (up to 2 lines) & Category
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = game.title,
                color = TextPrimary,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = localizedCategory,
                color = TextSecondary,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
