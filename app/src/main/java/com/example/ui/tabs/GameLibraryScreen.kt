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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.GameFilterTab
import com.example.data.GameLibraryRepository
import com.example.ui.theme.DirectCyan
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
import com.example.ui.theme.XboxSurfaceVariant

private enum class GameTabSection {
    LIBRARY,
    HISTORY
}

@Composable
fun GameLibraryScreen(
    games: List<GameBookmark>,
    onLaunchGame: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val favoriteIds by GameLibraryRepository.favoriteIds.collectAsState()
    val isSyncingCatalog by GameLibraryRepository.isSyncingCatalog.collectAsState()
    val isEn = config.isEnglishUI()
    var activeSection by remember { mutableStateOf(GameTabSection.LIBRARY) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(GameFilterTab.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredGames = remember(games, selectedTab, searchQuery, favoriteIds) {
        games.filter { game ->
            val matchesTab = when (selectedTab) {
                GameFilterTab.ALL -> true
                GameFilterTab.FAVORITES -> game.id in favoriteIds
                GameFilterTab.FREE_TO_PLAY -> game.freeToPlay
                GameFilterTab.TOUCH_FRIENDLY -> game.supportsTouch
                GameFilterTab.GAME_PASS -> !game.freeToPlay
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                game.title.contains(searchQuery, ignoreCase = true) ||
                    game.category.contains(searchQuery, ignoreCase = true) ||
                    game.categoryEn.contains(searchQuery, ignoreCase = true)
            }
            matchesTab && matchesSearch
        }.sortedByDescending { it.id in favoriteIds }
    }

    val favCount = remember(games, favoriteIds) { games.count { it.id in favoriteIds } }
    val freeCount = remember(games) { games.count { it.freeToPlay } }
    val touchCount = remember(games) { games.count { it.supportsTouch } }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Segmented switch: "Thư viện" | "Lịch sử"
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("game_tab_segmented_bar"),
                shape = RoundedCornerShape(14.dp),
                color = XboxSurfaceVariant.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, XboxCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val sections = listOf(
                        GameTabSection.LIBRARY to (if (isEn) "Library" else "Thư viện"),
                        GameTabSection.HISTORY to (if (isEn) "History" else "Lịch sử")
                    )
                    sections.forEach { (section, label) ->
                        val isSelected = activeSection == section
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { activeSection = section }
                                .testTag(
                                    if (section == GameTabSection.LIBRARY) "segment_library_button"
                                    else "segment_history_button"
                                ),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) XboxGreen else Color.Transparent,
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(0.8.dp, XboxNeonGreen.copy(alpha = 0.65f))
                            } else null
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            if (activeSection == GameTabSection.HISTORY) {
                SessionHistoryScreen(
                    onLaunchGame = { url ->
                        GameLibraryRepository.recordLastPlayedByUrl(url)
                        onLaunchGame(url)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                // 1. Vault Telemetry Header Strip
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = XboxSurface.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(XboxNeonGreen.copy(alpha = 0.45f), XboxCardBorder, DirectCyan.copy(alpha = 0.45f))
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if (isEn) "XBOX CLOUD GAMING VAULT" else "KHO GAME XBOX CLOUD VAULT",
                                color = XboxNeonGreen,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = if (isSyncingCatalog) {
                                    if (isEn) "Syncing live Xbox catalog & covers..." else "Đang tự động tải kho game & ảnh bìa Xbox..."
                                } else {
                                    if (isEn) "${games.size} titles ready • $freeCount Free • $touchCount Touch" else "${games.size} tựa game sẵn sàng • $freeCount Free • $touchCount Cảm ứng"
                                },
                                color = if (isSyncingCatalog) DirectCyan else TextSecondary,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DirectCyan.copy(alpha = 0.14f),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, DirectCyan.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = !isSyncingCatalog) {
                                        GameLibraryRepository.syncLiveXboxCatalog(force = true)
                                    }
                                    .testTag("sync_xbox_catalog_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = DirectCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (isSyncingCatalog) {
                                            if (isEn) "..." else "..."
                                        } else {
                                            if (isEn) "Sync" else "Tải kho"
                                        },
                                        color = DirectCyan,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = XboxNeonGreen.copy(alpha = 0.16f),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxNeonGreen.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showAddDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = XboxNeonGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.btn_add_game),
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

                // 2. High-Tech Console Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("library_search_input"),
                    placeholder = {
                        Text(
                            stringResource(R.string.search_games_placeholder),
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = XboxNeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = XboxNeonGreen,
                        unfocusedBorderColor = XboxCardBorder,
                        focusedContainerColor = XboxSurfaceVariant.copy(alpha = 0.92f),
                        unfocusedContainerColor = XboxSurface.copy(alpha = 0.88f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // 3. Category Tabs Carousel
                val filterOptions: List<Pair<GameFilterTab, String>> = listOf(
                    GameFilterTab.ALL to "🎮 ${stringResource(R.string.filter_all)} (${games.size})",
                    GameFilterTab.FAVORITES to (if (isEn) "❤️ Favorites ($favCount)" else "❤️ Yêu thích ($favCount)"),
                    GameFilterTab.FREE_TO_PLAY to "⚡ ${stringResource(R.string.filter_free)} ($freeCount)",
                    GameFilterTab.TOUCH_FRIENDLY to "👆 ${stringResource(R.string.filter_touch)} ($touchCount)",
                    GameFilterTab.GAME_PASS to "💎 ${stringResource(R.string.filter_gamepass)}"
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filterOptions) { (tab, label) ->
                        val isSelected = selectedTab == tab
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) XboxGreen else XboxSurfaceVariant.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) XboxNeonGreen else XboxCardBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedTab = tab }
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                // 4. Adaptive Grid of Games (2 columns in Portrait, 3-4 in Landscape)
                if (filteredGames.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gamepad,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (isEn) "No matching games found" else "Không tìm thấy tựa game nào phù hợp",
                                color = TextSecondary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 152.dp),
                        contentPadding = PaddingValues(bottom = 84.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredGames, key = { it.id }) { game ->
                            LibraryGameCard(
                                game = game,
                                isFavorite = game.id in favoriteIds,
                                onToggleFavorite = { GameLibraryRepository.toggleFavorite(game.id) },
                                onPlay = {
                                    GameLibraryRepository.recordLastPlayed(game.id)
                                    onLaunchGame(game.playUrl)
                                },
                                onDeleteCustom = if (game.id.startsWith("custom_")) {
                                    { GameLibraryRepository.removeCustomGame(game.id) }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        // FAB to Add Game (only shown on Library section)
        if (activeSection == GameTabSection.LIBRARY) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = XboxNeonGreen,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("fab_add_game")
            ) {
                Icon(Icons.Default.Add, contentDescription = if (isEn) "Add Game" else "Thêm Game")
            }
        }

        if (showAddDialog) {
            AddGameDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { title, url ->
                    GameLibraryRepository.addCustomGame(title, url)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
private fun LibraryGameCard(
    game: GameBookmark,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onPlay: () -> Unit,
    onDeleteCustom: (() -> Unit)? = null
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    val localizedBadge = game.localizedBadge(isEn)
    val localizedCategory = game.localizedCategory(isEn)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(XboxCardBorder, Color(game.accentColorHex).copy(alpha = 0.65f))
                ),
                RoundedCornerShape(20.dp)
            )
            .clickable { onPlay() },
        colors = CardDefaults.cardColors(containerColor = XboxSurface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (game.drawableResId != null) {
                Image(
                    painter = painterResource(id = game.drawableResId),
                    contentDescription = game.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(game.accentColorHex), XboxDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(52.dp)
                    )
                    if (!game.imageUrl.isNullOrBlank()) {
                        val context = LocalContext.current
                        val request = remember(game.imageUrl, config.lowEndDeviceMode) {
                            ImageRequest.Builder(context)
                                .data(game.imageUrl)
                                .crossfade(!config.lowEndDeviceMode)
                                .build()
                        }
                        AsyncImage(
                            model = request,
                            contentDescription = game.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // High-Contrast Console Vignette Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0x4D000000),
                                0.45f to Color.Transparent,
                                0.78f to Color(0xD9060A08),
                                1.0f to Color(0xF7060A08)
                            )
                        )
                    )
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
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = Color(0xCC060B08),
                    border = androidx.compose.foundation.BorderStroke(0.6.dp, GoldStar.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (game.supportsTouch) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xCC060B08),
                            border = androidx.compose.foundation.BorderStroke(0.6.dp, DirectCyan.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = "Touch",
                                tint = DirectCyan,
                                modifier = Modifier
                                    .padding(5.dp)
                                    .size(13.dp)
                            )
                        }
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

            // Bottom Info & Play Trigger
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(11.dp)
                    .align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (localizedBadge != null) {
                    Text(
                        text = localizedBadge,
                        color = XboxNeonGreen,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                        modifier = Modifier.weight(1f)
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

@Composable
private fun AddGameDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://www.xbox.com/en-US/play/games/") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEn) "Add Game to Vault" else "Thêm Game Vào Thư Viện",
                color = TextPrimary,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isEn) "Paste an Xbox Cloud Gaming URL to create a persistent 1-tap launch shortcut." else "Dán link trò chơi trên Xbox Cloud Gaming để tạo phím tắt vào thẳng game 1 chạm (được lưu bền vững trên máy).",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isEn) "Game Title (e.g. Persona 3 Reload)" else "Tên Game (VD: Persona 3 Reload)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = XboxNeonGreen,
                        unfocusedBorderColor = XboxCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
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
