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

    val computedLibraryState by androidx.compose.runtime.produceState(
        initialValue = Pair(games, intArrayOf(0, 0, 0, 0, 0)),
        games,
        selectedTab,
        selectedGenre,
        searchQuery,
        favoriteIds
    ) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val filtered = games.filter { game ->
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

            var fav = 0
            var pop = 0
            var rec = 0
            var free = 0
            var touch = 0
            for (g in games) {
                if (g.id in favoriteIds) fav++
                if (g.isPopular || g.rating >= "4.9") pop++
                if (g.isRecentlyAdded) rec++
                if (g.freeToPlay) free++
                if (g.supportsTouch) touch++
            }
            Pair(filtered, intArrayOf(fav, pop, rec, free, touch))
        }
    }

    val filteredGames = computedLibraryState.first
    val counts = computedLibraryState.second
    val favCount = counts[0]
    val popCount = counts[1]
    val recentCount = counts[2]
    val freeCount = counts[3]
    val touchCount = counts[4]

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    shape = RoundedCornerShape(12.dp),
                    color = XboxSurfaceVariant.copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, XboxCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp),
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
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable { activeSection = section }
                                    .testTag(
                                        if (section == GameTabSection.LIBRARY) "segment_library_button"
                                        else "segment_history_button"
                                    ),
                                shape = RoundedCornerShape(9.dp),
                                color = segmentBg,
                                border = if (isSelected) {
                                    androidx.compose.foundation.BorderStroke(0.8.dp, XboxNeonGreen.copy(alpha = 0.65f))
                                } else null
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = 11.5.sp,
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
                        shape = RoundedCornerShape(12.dp),
                        color = DirectCyan.copy(alpha = 0.14f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DirectCyan.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .xboxPressScale(enabled = motionEnabled, pressedScale = 0.95f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isSyncingCatalog) {
                                GameLibraryRepository.syncLiveXboxCatalog(force = true)
                            }
                            .testTag("sync_xbox_catalog_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    contentDescription = if (isEn) "Clear search" else "Xóa tìm kiếm",
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
                    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
                    var visibleLimit by remember(selectedTab, selectedGenre, searchQuery) {
                        androidx.compose.runtime.mutableIntStateOf(36)
                    }
                    val displayedGames = remember(filteredGames, visibleLimit) {
                        if (filteredGames.size <= visibleLimit) filteredGames else filteredGames.take(visibleLimit)
                    }
                    val shouldLoadMore by remember(gridState, displayedGames.size, filteredGames.size) {
                        androidx.compose.runtime.derivedStateOf {
                            if (displayedGames.size >= filteredGames.size) {
                                false
                            } else {
                                val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                lastVisible >= (displayedGames.size - 8).coerceAtLeast(0)
                            }
                        }
                    }
                    androidx.compose.runtime.LaunchedEffect(shouldLoadMore) {
                        if (shouldLoadMore && visibleLimit < filteredGames.size) {
                            visibleLimit = (visibleLimit + 36).coerceAtMost(filteredGames.size)
                        }
                    }

                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 152.dp),
                        contentPadding = PaddingValues(bottom = 160.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(
                            items = displayedGames,
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
