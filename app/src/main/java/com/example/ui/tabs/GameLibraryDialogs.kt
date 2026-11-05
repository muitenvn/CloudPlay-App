package com.example.ui.tabs

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.GameBookmark
import com.example.data.GameLibraryRepository
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

@Composable
internal fun XboxGameDetailDialog(
    game: GameBookmark,
    isFavorite: Boolean,
    isImportingCatalog: Boolean = false,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit,
    onPlay: () -> Unit
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    val productId = remember(game.playUrl) { GameLibraryRepository.extractProductId(game.playUrl) }
    val heroUrl = game.heroImageUrl ?: game.imageUrl

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = game.title,
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp
                )
                if (!game.publisher.isNullOrBlank()) {
                    Text(
                        text = game.publisher,
                        color = XboxNeonGreen,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(game.accentColorHex).copy(alpha = 0.55f), XboxDark)
                            )
                        )
                        .border(1.dp, XboxCardBorder, RoundedCornerShape(14.dp))
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
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gamepad,
                                contentDescription = null,
                                tint = XboxNeonGreen,
                                modifier = Modifier.size(30.dp)
                            )
                            Text(
                                text = game.localizedBadge(isEn) ?: "Xbox Cloud Gaming",
                                color = Color.White,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = XboxSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(0.7.dp, GoldStar.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "★ ${game.rating}",
                            color = GoldStar,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = XboxSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(0.7.dp, XboxCardBorder)
                    ) {
                        Text(
                            text = game.localizedCategory(isEn),
                            color = TextPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (productId.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DirectCyan.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(0.7.dp, DirectCyan.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "ID: $productId",
                                color = DirectCyan,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Text(
                    text = game.description ?: if (isEn) {
                        "Official Xbox Cloud Gaming title linked directly from Microsoft Store. Supports instant cloud streaming without downloading."
                    } else {
                        "Tựa game chính thức liên kết trực tiếp từ máy chủ Microsoft Xbox Cloud Gaming. Hỗ trợ chơi ngay trên đám mây không cần tải về."
                    },
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(containerColor = XboxGreen)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = config.tr("Chơi trên Xbox Cloud", "Play on Xbox Cloud"),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onToggleFavorite) {
                    Text(
                        text = if (isFavorite) {
                            config.tr("♥ Đã thích", "♥ Saved")
                        } else {
                            config.tr("♡ Yêu thích", "♡ Favorite")
                        },
                        color = if (isFavorite) JapanRed else XboxNeonGreen
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(config.tr("Đóng", "Close"), color = TextSecondary)
                }
            }
        },
        containerColor = XboxSurface
    )
}

@Composable
internal fun AddGameDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
    onLaunchDirect: (GameBookmark) -> Unit = {}
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isSearchingLiveXbox by GameLibraryRepository.isSearchingLiveXbox.collectAsState()
    val isEn = config.isEnglishUI()
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://www.xbox.com/en-US/play/games/") }
    var liveResults by remember { mutableStateOf<List<GameBookmark>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEn) "Link / Add Xbox Cloud Game" else "Liên Kết / Thêm Game Xbox Cloud",
                color = TextPrimary,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isEn) {
                        "Type any game name to fetch game info & link from Microsoft Xbox Store (metadata only, no image downloads to save 4G/Wi-Fi data)."
                    } else {
                        "Gõ tên game bất kỳ rồi bấm 'Tìm Xbox' để tự động lấy thông tin và link chuẩn từ Microsoft (chỉ tải thông tin game, không tải ảnh để tiết kiệm 4G/Wi-Fi)."
                    },
                    color = TextSecondary,
                    fontSize = 11.5.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(if (isEn) "Game Name (e.g. Black Ops 6)" else "Tên Game (VD: Black Ops 6)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = XboxNeonGreen,
                            unfocusedBorderColor = XboxCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Button(
                        onClick = {
                            if (title.trim().length >= 2) {
                                GameLibraryRepository.searchMicrosoftXboxCatalog(title) { found ->
                                    liveResults = found.take(5)
                                    found.firstOrNull()?.let { first ->
                                        title = first.title
                                        url = first.playUrl
                                    }
                                }
                            }
                        },
                        enabled = !isSearchingLiveXbox && title.trim().length >= 2,
                        colors = ButtonDefaults.buttonColors(containerColor = DirectCyan.copy(alpha = 0.2f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isSearchingLiveXbox) "..." else if (isEn) "Find Xbox" else "Tìm Xbox",
                            color = DirectCyan,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                }

                if (liveResults.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = if (isEn) "Imported from Microsoft Xbox Store (tap to launch):" else "Đã tự động thêm từ Microsoft Xbox Store (chạm để chơi ngay):",
                            color = XboxNeonGreen,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        liveResults.forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = XboxSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(0.7.dp, XboxCardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onLaunchDirect(item) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            color = TextPrimary,
                                            fontFamily = SpaceGroteskFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.publisher ?: item.localizedCategory(isEn),
                                            color = TextSecondary,
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = if (isEn) "PLAY ▶" else "CHƠI ▶",
                                        color = XboxNeonGreen,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(if (isEn) "Xbox Cloud URL" else "Đường dẫn Xbox Cloud URL") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = XboxNeonGreen,
                        unfocusedBorderColor = XboxCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && url.isNotBlank()) {
                        onAdd(title.trim(), url.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = XboxGreen)
            ) {
                Text(if (isEn) "Add Game" else "Thêm Game", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEn) "Cancel" else "Hủy", color = TextSecondary)
            }
        },
        containerColor = XboxSurface
    )
}
