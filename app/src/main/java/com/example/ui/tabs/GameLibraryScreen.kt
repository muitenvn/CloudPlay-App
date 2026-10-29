package com.example.ui.tabs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Info
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
import com.example.ui.theme.rememberXboxMotionEnabled
import com.example.ui.theme.xboxPressScale
import com.example.ui.theme.xboxSlideFadeIn
import com.example.ui.theme.xboxTabEnterTransition
import com.example.ui.theme.xboxTabExitTransition

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
    val isSearchingLiveXbox by GameLibraryRepository.isSearchingLiveXbox.collectAsState()
    val xboxStatus by GameLibraryRepository.xboxStatus.collectAsState()
    val isEn = config.isEnglishUI()
    val motionEnabled = rememberXboxMotionEnabled(
        lowEndDeviceMode = config.lowEndDeviceMode,
        gpuLoadReductionEnabled = config.gpuLoadReductionEnabled
    )
    var activeSection by remember { mutableStateOf(GameTabSection.LIBRARY) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(GameFilterTab.ALL) }
    var selectedGenre by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDetailGame by remember { mutableStateOf<GameBookmark?>(null) }

    val filteredGames = remember(games, selectedTab, selectedGenre, searchQuery, favoriteIds) {
        games.filter { game ->
            val matchesTab = when (selectedTab) {
                GameFilterTab.ALL -> true
                GameFilterTab.FAVORITES -> game.id in favoriteIds
                GameFilterTab.POPULAR -> game.isPopular || game.rating >= "4.9"
                GameFilterTab.RECENTLY_ADDED -> game.isRecentlyAdded
                GameFilterTab.FREE_TO_PLAY -> game.freeToPlay
                GameFilterTab.TOUCH_FRIENDLY -> game.supportsTouch
                GameFilterTab.GAME_PASS -> !game.freeToPlay
            }
            val matchesGenre = when (selectedGenre) {
                "ALL" -> true
                "SHOOTER" -> game.category.contains("Bắn súng", ignoreCase = true) ||
                    game.categoryEn.contains("Shooter", ignoreCase = true) ||
                    game.categoryEn.contains("Battle Royale", ignoreCase = true)
                "ACTION" -> game.category.contains("Hành động", ignoreCase = true) ||
                    game.category.contains("Phiêu lưu", ignoreCase = true) ||
                    game.categoryEn.contains("Action", ignoreCase = true) ||
                    game.categoryEn.contains("Adventure", ignoreCase = true)
                "RACING" -> game.category.contains("Đua xe", ignoreCase = true) ||
                    game.category.contains("Lái", ignoreCase = true) ||
                    game.categoryEn.contains("Racing", ignoreCase = true) ||
                    game.categoryEn.contains("Simulation", ignoreCase = true)
                "RPG" -> game.category.contains("Nhập vai", ignoreCase = true) ||
                    game.category.contains("Sinh tồn", ignoreCase = true) ||
                    game.categoryEn.contains("RPG", ignoreCase = true) ||
                    game.categoryEn.contains("Survival", ignoreCase = true)
                "SPORTS" -> game.category.contains("Thể thao", ignoreCase = true) ||
                    game.category.contains("Đối kháng", ignoreCase = true) ||
                    game.categoryEn.contains("Sports", ignoreCase = true) ||
                    game.categoryEn.contains("Fighting", ignoreCase = true)
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                game.title.contains(searchQuery, ignoreCase = true) ||
                    game.category.contains(searchQuery, ignoreCase = true) ||
                    game.categoryEn.contains(searchQuery, ignoreCase = true) ||
                    (game.publisher?.contains(searchQuery, ignoreCase = true) == true)
            }
            matchesTab && matchesGenre && matchesSearch
        }.sortedByDescending { it.id in favoriteIds }
    }

    val favCount = remember(games, favoriteIds) { games.count { it.id in favoriteIds } }
    val popCount = remember(games) { games.count { it.isPopular || it.rating >= "4.9" } }
    val recentCount = remember(games) { games.count { it.isRecentlyAdded } }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
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
                            GameTabSection.LIBRARY to "${stringResource(R.string.tab_library)} (${games.size})",
                            GameTabSection.HISTORY to config.tr("Lịch sử", "History")
                        )
                        sections.forEach { (section, label) ->
                            val isSelected = activeSection == section
                            val segmentBg by animateColorAsState(
                                targetValue = if (isSelected) XboxGreen else Color.Transparent,
                                animationSpec = tween(durationMillis = if (motionEnabled) 160 else 0),
                                label = "library_segment_bg"
                            )
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .xboxPressScale(enabled = motionEnabled, pressedScale = 0.96f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { activeSection = section }
                                    .testTag(
                                        if (section == GameTabSection.LIBRARY) "segment_library_button"
                                        else "segment_history_button"
                                    ),
                                shape = RoundedCornerShape(10.dp),
                                color = segmentBg,
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
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                if (activeSection == GameTabSection.LIBRARY) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DirectCyan.copy(alpha = 0.14f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DirectCyan.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .xboxPressScale(enabled = motionEnabled, pressedScale = 0.95f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !isSyncingCatalog) {
                                GameLibraryRepository.syncLiveXboxCatalog(force = true)
                            }
                            .testTag("sync_xbox_catalog_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = config.tr("Làm mới", "Refresh"),
                                tint = DirectCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isSyncingCatalog) {
                                    "..."
                                } else {
                                    config.tr("Làm mới", "Refresh")
                                },
                                color = DirectCyan,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = activeSection,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                transitionSpec = {
                    if (!motionEnabled) {
                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    } else {
                        val direction = targetState.ordinal.compareTo(initialState.ordinal)
                        xboxTabEnterTransition(direction) togetherWith xboxTabExitTransition(direction)
                    }
                },
                label = "library_history_section_anim"
            ) { sectionState ->
            if (sectionState == GameTabSection.HISTORY) {
                SessionHistoryScreen(
                    onLaunchGame = { url ->
                        GameLibraryRepository.recordLastPlayedByUrl(url)
                        onLaunchGame(url)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                // 1. High-Tech Console Search Bar (no fixed 52.dp clipping)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_search_input"),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    placeholder = {
                        Text(
                            stringResource(R.string.search_games_placeholder),
                            fontSize = 13.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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

                // 2. Category Tabs Carousel
                val filterOptions: List<Pair<GameFilterTab, String>> = listOf(
                    GameFilterTab.ALL to "🎮 ${stringResource(R.string.filter_all)} (${games.size})",
                    GameFilterTab.FAVORITES to "❤️ ${config.tr("Yêu thích", "Favorites")} ($favCount)",
                    GameFilterTab.POPULAR to "🔥 ${config.tr("Phổ biến Xbox", "Xbox Popular")} ($popCount)",
                    GameFilterTab.RECENTLY_ADDED to "🆕 ${config.tr("Mới lên kệ", "New on Cloud")} ($recentCount)",
                    GameFilterTab.FREE_TO_PLAY to "⚡ ${stringResource(R.string.filter_free)} ($freeCount)",
                    GameFilterTab.TOUCH_FRIENDLY to "👆 ${stringResource(R.string.filter_touch)} ($touchCount)",
                    GameFilterTab.GAME_PASS to "💎 ${stringResource(R.string.filter_gamepass)}"
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(filterOptions) { (tab, label) ->
                        val isSelected = selectedTab == tab
                        val pillBg by animateColorAsState(
                            targetValue = if (isSelected) XboxGreen else XboxSurfaceVariant.copy(alpha = 0.85f),
                            animationSpec = tween(durationMillis = if (motionEnabled) 150 else 0),
                            label = "filter_tab_bg"
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = pillBg,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) XboxNeonGreen else XboxCardBorder
                            ),
                            modifier = Modifier
                                .xboxPressScale(enabled = motionEnabled, pressedScale = 0.95f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedTab = tab }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // 3. Quick Genre Chips + Live Result Counter
                val genreOptions = listOf(
                    "ALL" to config.tr("Mọi thể loại", "All Genres"),
                    "SHOOTER" to config.tr("Bắn súng", "Shooter / FPS"),
                    "ACTION" to config.tr("Hành động", "Action & Adv"),
                    "RACING" to config.tr("Đua xe / Lái", "Racing & Sim"),
                    "RPG" to config.tr("Nhập vai / Sinh tồn", "RPG & Survival"),
                    "SPORTS" to config.tr("Thể thao / Đối kháng", "Sports & Fight")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(genreOptions) { (genreCode, genreLabel) ->
                            val isGenreSelected = selectedGenre == genreCode
                            val genreBg by animateColorAsState(
                                targetValue = if (isGenreSelected) DirectCyan.copy(alpha = 0.2f) else XboxSurface.copy(alpha = 0.8f),
                                animationSpec = tween(durationMillis = if (motionEnabled) 150 else 0),
                                label = "genre_chip_bg"
                            )
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = genreBg,
                                border = androidx.compose.foundation.BorderStroke(
                                    0.8.dp,
                                    if (isGenreSelected) DirectCyan else XboxCardBorder
                                ),
                                modifier = Modifier
                                    .xboxPressScale(enabled = motionEnabled, pressedScale = 0.95f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable { selectedGenre = genreCode }
                                    .testTag("genre_chip_${genreCode.lowercase()}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = genreLabel,
                                        color = if (isGenreSelected) DirectCyan else TextSecondary,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = if (isGenreSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = if (isSyncingCatalog) {
                            config.tr("Đang tải...", "Syncing...")
                        } else {
                            if (isEn) "${filteredGames.size} games" else "${filteredGames.size} game"
                        },
                        color = if (isSyncingCatalog) DirectCyan else XboxNeonGreen,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Live Microsoft Xbox Store Search bar trigger when user types a query
                if (searchQuery.trim().length >= 2) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DirectCyan.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, DirectCyan.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isSearchingLiveXbox) {
                                GameLibraryRepository.searchMicrosoftXboxCatalog(searchQuery)
                            }
                            .testTag("live_xbox_search_bar_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = DirectCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (isSearchingLiveXbox) {
                                        if (isEn) "Searching Xbox Store for \"$searchQuery\"..."
                                        else "Đang tìm \"$searchQuery\" trên Xbox Store..."
                                    } else {
                                        if (isEn) "Tap to search \"$searchQuery\" on Xbox Store"
                                        else "Chạm để tìm thêm \"$searchQuery\" trên Xbox Store"
                                    },
                                    color = DirectCyan,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gamepad,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (isSyncingCatalog && games.isEmpty()) {
                                    config.tr("Đang tải danh sách game...", "Loading games from Xbox Cloud...")
                                } else if (games.isEmpty()) {
                                    config.tr("Chưa có game", "No games yet")
                                } else {
                                    config.tr("Chưa có game phù hợp", "No matching games found")
                                },
                                color = if (isSyncingCatalog && games.isEmpty()) DirectCyan else TextSecondary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (searchQuery.trim().length >= 2) {
                                Button(
                                    onClick = { GameLibraryRepository.searchMicrosoftXboxCatalog(searchQuery) },
                                    enabled = !isSearchingLiveXbox,
                                    colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (isSearchingLiveXbox) {
                                            if (isEn) "Searching Xbox..." else "Đang tìm trên Xbox..."
                                        } else {
                                            if (isEn) "Import \"$searchQuery\" Info from Xbox" else "Nhập thông tin \"$searchQuery\" từ Xbox"
                                        },
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 152.dp),
                        contentPadding = PaddingValues(bottom = 112.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(
                            items = filteredGames,
                            key = { it.id },
                            contentType = { "library_game_card" }
                        ) { game ->
                            LibraryGameCard(
                                game = game,
                                isFavorite = game.id in favoriteIds,
                                isEn = isEn,
                                isImportingCatalog = isSyncingCatalog,
                                onToggleFavorite = { GameLibraryRepository.toggleFavorite(game.id) },
                                onShowDetail = { selectedDetailGame = game },
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
                    .xboxPressScale(enabled = motionEnabled, pressedScale = 0.93f)
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
                },
                onLaunchDirect = { game ->
                    showAddDialog = false
                    GameLibraryRepository.recordLastPlayed(game.id)
                    onLaunchGame(game.playUrl)
                }
            )
        }

        selectedDetailGame?.let { detailGame ->
            XboxGameDetailDialog(
                game = detailGame,
                isFavorite = detailGame.id in favoriteIds,
                isImportingCatalog = isSyncingCatalog,
                onToggleFavorite = { GameLibraryRepository.toggleFavorite(detailGame.id) },
                onDismiss = { selectedDetailGame = null },
                onPlay = {
                    selectedDetailGame = null
                    GameLibraryRepository.recordLastPlayed(detailGame.id)
                    onLaunchGame(detailGame.playUrl)
                }
            )
        }
    }
}

@Composable
private fun LibraryGameCard(
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
                Text(
                    text = badgeText,
                    color = if (localizedBadge != null || game.freeToPlay) XboxNeonGreen else DirectCyan.copy(alpha = 0.9f),
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

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

@Composable
private fun XboxGameDetailDialog(
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
private fun AddGameDialog(
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
