package com.example.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.GameBookmark
import com.example.data.GameLibraryRepository
import com.example.data.SettingsRepository
import com.example.ui.components.AdaptiveFlatActionButton
import com.example.ui.components.AdaptivePopupFrame
import com.example.ui.components.AdaptivePopupScope
import com.example.ui.components.AdaptiveTwoColumnLayout
import com.example.ui.components.gamepadTextFieldFocus
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.GoldStar
import com.example.ui.theme.JapanRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxDark
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant

@Composable
internal fun XboxGameDetailDialog(
    game: GameBookmark,
    isFavorite: Boolean,
    isImportingCatalog: Boolean = false,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit,
    onPlay: () -> Unit
) {
    val config by SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    val productId = remember(game.playUrl) { GameLibraryRepository.extractProductId(game.playUrl) }
    val heroUrl = game.heroImageUrl ?: game.imageUrl
    val playBtnFocusRequester = remember { FocusRequester() }

    AdaptivePopupFrame(
        onDismissRequest = onDismiss,
        dialogTestTag = "xbox_game_detail_dialog",
        defaultFocusRequester = playBtnFocusRequester,
        wideWidthFraction = 0.88f,
        portraitWidthFraction = 0.92f,
        onSecondaryY = onToggleFavorite
    ) {
        val descScroll = rememberScrollState()

        @Composable
        fun AdaptivePopupScope.TitleAndPublisher() {
            Column(verticalArrangement = Arrangement.spacedBy(rDp(2.5f))) {
                Text(
                    text = game.title,
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = rSp(16.5f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!game.publisher.isNullOrBlank()) {
                    Text(
                        text = game.publisher,
                        color = XboxNeonGreen,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = rSp(11.5f),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        @Composable
        fun AdaptivePopupScope.HeroAndBadges(heroHeight: Dp) {
            Column(verticalArrangement = Arrangement.spacedBy(gapMed)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroHeight)
                        .clip(RoundedCornerShape(sectionCorner))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(game.accentColorHex).copy(alpha = 0.55f), XboxDark)
                            )
                        )
                        .border(borderThin, XboxCardBorder, RoundedCornerShape(sectionCorner))
                ) {
                    if (!isImportingCatalog && !heroUrl.isNullOrBlank()) {
                        val context = LocalContext.current
                        AsyncImage(
                            model = remember(heroUrl) {
                                ImageRequest.Builder(context)
                                    .data(heroUrl)
                                    .crossfade(true)
                                    .build()
                            },
                            contentDescription = game.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(rDp(4f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gamepad,
                                contentDescription = null,
                                tint = XboxNeonGreen,
                                modifier = Modifier.size(rDp(28f))
                            )
                            Text(
                                text = game.localizedBadge(isEn) ?: "Xbox Cloud Gaming",
                                color = Color.White,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = rSp(11.5f),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(chipCorner),
                    color = XboxSurface,
                    border = BorderStroke(borderThin, XboxCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(gapMed),
                        horizontalArrangement = Arrangement.spacedBy(gapSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(chipCorner),
                            color = Color(0xFF2E2408)
                        ) {
                            Text(
                                text = "★ ${game.rating}",
                                color = GoldStar,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = rSp(10.8f),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = rDp(7f), vertical = rDp(3.5f))
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(chipCorner),
                            color = XboxSurfaceVariant
                        ) {
                            Text(
                                text = game.localizedCategory(isEn),
                                color = TextPrimary,
                                fontSize = rSp(10.8f),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = rDp(7f), vertical = rDp(3.5f))
                            )
                        }
                        if (productId.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(chipCorner),
                                color = DirectCyan.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "ID: $productId",
                                    color = DirectCyan,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = rSp(9.8f),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = rDp(6.5f), vertical = rDp(3.5f))
                                )
                            }
                        }
                    }
                }
            }
        }

        @Composable
        fun AdaptivePopupScope.DescriptionBox(maxDescHeight: Dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxDescHeight)
                    .verticalScroll(descScroll)
            ) {
                Text(
                    text = game.description ?: if (isEn) {
                        "Official Xbox Cloud Gaming title linked directly from Microsoft Store. Supports instant cloud streaming without downloading."
                    } else {
                        "Tựa game chính thức liên kết trực tiếp từ máy chủ Microsoft Xbox Cloud Gaming. Hỗ trợ chơi ngay trên đám mây không cần tải về."
                    },
                    color = TextSecondary,
                    fontSize = rSp(12f),
                    lineHeight = rSp(16.8f)
                )
            }
        }

        @Composable
        fun AdaptivePopupScope.ActionButtonsColumn() {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(gapMed)
            ) {
                AdaptiveFlatActionButton(
                    onClick = onPlay,
                    isPrimary = true,
                    gamepadHint = "A",
                    focusRequester = playBtnFocusRequester,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF03140B),
                        modifier = Modifier.size(rDp(16f))
                    )
                    Spacer(modifier = Modifier.width(rDp(5f)))
                    Text(
                        text = config.tr("Chơi ngay", "Play now"),
                        color = Color(0xFF03140B),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = rSp(12.5f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gapMed)
                ) {
                    AdaptiveFlatActionButton(
                        onClick = onToggleFavorite,
                        isPrimary = false,
                        gamepadHint = "Y",
                        borderColor = if (isFavorite) JapanRed else XboxNeonGreen,
                        contentColor = if (isFavorite) JapanRed else XboxNeonGreen,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isFavorite) {
                                config.tr("♥ Đã thích", "♥ Saved")
                            } else {
                                config.tr("♡ Yêu thích", "♡ Favorite")
                            },
                            color = if (isFavorite) JapanRed else XboxNeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = rSp(11.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    AdaptiveFlatActionButton(
                        onClick = onDismiss,
                        isPrimary = false,
                        gamepadHint = "B",
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = config.tr("Đóng", "Close"),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = rSp(11.8f),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        AdaptiveTwoColumnLayout(
            leftWeight = 1.08f,
            rightWeight = 0.92f,
            leftColumn = {
                TitleAndPublisher()
                HeroAndBadges(heroHeight = rDp(118f))
            },
            rightColumn = {
                DescriptionBox(maxDescHeight = winH * 0.44f)
                ActionButtonsColumn()
            },
            portraitColumn = {
                TitleAndPublisher()
                HeroAndBadges(heroHeight = rDp(136f))
                DescriptionBox(maxDescHeight = winH * 0.28f)
                ActionButtonsColumn()
            }
        )
    }
}

@Composable
internal fun AddGameDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
    onLaunchDirect: (GameBookmark) -> Unit = {}
) {
    val config by SettingsRepository.config.collectAsState()
    val isSearchingLiveXbox by GameLibraryRepository.isSearchingLiveXbox.collectAsState()
    val isEn = config.isEnglishUI()
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://www.xbox.com/en-US/play/games/") }
    var liveResults by remember { mutableStateOf<List<GameBookmark>>(emptyList()) }
    val addBtnFocusRequester = remember { FocusRequester() }

    val canSearch = !isSearchingLiveXbox && title.trim().length >= 2
    val triggerSearch: () -> Unit = {
        if (title.trim().length >= 2) {
            GameLibraryRepository.searchMicrosoftXboxCatalog(title) { found ->
                liveResults = found.take(5)
                found.firstOrNull()?.let { first ->
                    title = first.title
                    url = first.playUrl
                }
            }
        }
    }

    val canAdd = title.isNotBlank() && url.isNotBlank()
    val submitAdd: () -> Unit = {
        if (canAdd) {
            onAdd(title.trim(), url.trim())
        }
    }

    AdaptivePopupFrame(
        onDismissRequest = onDismiss,
        dialogTestTag = "add_game_dialog",
        defaultFocusRequester = addBtnFocusRequester,
        wideWidthFraction = 0.90f,
        portraitWidthFraction = 0.92f
    ) {
        val resultsScroll = rememberScrollState()
        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = XboxNeonGreen,
            unfocusedBorderColor = XboxCardBorder,
            focusedContainerColor = XboxSurface,
            unfocusedContainerColor = XboxSurface,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )

        @Composable
        fun AdaptivePopupScope.HeaderAndInputs() {
            Column(verticalArrangement = Arrangement.spacedBy(gapMed)) {
                Text(
                    text = if (isEn) "Add Xbox Cloud Game" else "Thêm game vào thư viện",
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = rSp(16f)
                )
                Text(
                    text = if (isEn) {
                        "Type a game name to search on Xbox Store or paste a game URL."
                    } else {
                        "Gõ tên game rồi bấm Tìm trên Xbox để tự lấy đường dẫn, hoặc dán đường dẫn có sẵn."
                    },
                    color = TextSecondary,
                    fontSize = rSp(11.5f),
                    lineHeight = rSp(15.8f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gapSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier
                            .weight(1f)
                            .gamepadTextFieldFocus(cornerRadius = btnCorner),
                        label = {
                            Text(
                                text = if (isEn) "Game Name (e.g. Black Ops 6)" else "Tên game (VD: Black Ops 6)",
                                fontSize = rSp(11f)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(btnCorner),
                        colors = fieldColors
                    )

                    AdaptiveFlatActionButton(
                        onClick = triggerSearch,
                        enabled = canSearch,
                        isPrimary = false,
                        borderColor = XboxNeonGreen,
                        contentColor = XboxNeonGreen
                    ) {
                        Text(
                            text = if (isSearchingLiveXbox) "..." else if (isEn) "Find Xbox" else "Tìm trên Xbox",
                            color = XboxNeonGreen,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = rSp(11.5f)
                        )
                    }
                }

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .gamepadTextFieldFocus(cornerRadius = btnCorner),
                    label = {
                        Text(
                            text = if (isEn) "Xbox Cloud URL" else "Đường dẫn Xbox Cloud",
                            fontSize = rSp(11f)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(btnCorner),
                    colors = fieldColors
                )
            }
        }

        @Composable
        fun AdaptivePopupScope.LiveSearchResultsList(maxListHeight: Dp) {
            if (liveResults.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxListHeight)
                        .verticalScroll(resultsScroll),
                    verticalArrangement = Arrangement.spacedBy(gapSmall)
                ) {
                    Text(
                        text = if (isEn) "Found on Xbox Store (tap to launch):" else "Đã tìm thấy trên Xbox Store (chạm để mở ngay):",
                        color = XboxNeonGreen,
                        fontSize = rSp(11f),
                        fontWeight = FontWeight.Bold
                    )
                    liveResults.forEach { item ->
                        AdaptiveFlatActionButton(
                            onClick = { onLaunchDirect(item) },
                            isPrimary = false,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = TextPrimary,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = rSp(12f),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.publisher ?: item.localizedCategory(isEn),
                                        color = TextSecondary,
                                        fontSize = rSp(10.5f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(gapSmall))
                                Text(
                                    text = if (isEn) "Play ▶" else "Chơi ▶",
                                    color = XboxNeonGreen,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = rSp(11f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        @Composable
        fun AdaptivePopupScope.BottomActions() {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(gapMed)
            ) {
                AdaptiveFlatActionButton(
                    onClick = submitAdd,
                    enabled = canAdd,
                    isPrimary = true,
                    gamepadHint = "A",
                    focusRequester = addBtnFocusRequester,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isEn) "Add Game" else "Thêm game",
                        color = if (canAdd) Color(0xFF03140B) else TextSecondary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = rSp(12.5f)
                    )
                }

                AdaptiveFlatActionButton(
                    onClick = onDismiss,
                    isPrimary = false,
                    gamepadHint = "B",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isEn) "Cancel" else "Hủy",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = rSp(12.2f)
                    )
                }
            }
        }

        AdaptiveTwoColumnLayout(
            leftWeight = 1.15f,
            rightWeight = 0.85f,
            leftColumn = {
                HeaderAndInputs()
            },
            rightColumn = {
                LiveSearchResultsList(maxListHeight = winH * 0.44f)
                BottomActions()
            },
            portraitColumn = {
                HeaderAndInputs()
                LiveSearchResultsList(maxListHeight = winH * 0.28f)
                BottomActions()
            }
        )
    }
}
