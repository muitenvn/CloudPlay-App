package com.example.ui.tv

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.AppConfig
import com.example.data.GameBookmark
import com.example.data.GameFilterTab
import com.example.data.GameLibraryRepository
import com.example.ui.components.gamepadActionButton
import com.example.ui.components.gamepadClickable
import com.example.ui.components.gamepadTextFieldFocus
import com.example.ui.tabs.AddGameDialog
import com.example.ui.tabs.SessionHistoryScreen
import com.example.ui.tabs.XboxGameDetailDialog
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private enum class TvLibrarySubSection {
    GAMES,
    HISTORY
}

@Composable
internal fun TvLibraryScreen(
    config: AppConfig,
    games: List<GameBookmark>,
    scale: Float,
    focusSearchTrigger: Int,
    favoriteToggleTrigger: Int,
    onLaunchGame: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val favoriteIds by GameLibraryRepository.favoriteIds.collectAsState()
    val playedGameIds by GameLibraryRepository.playedGameIds.collectAsState()
    val isSyncingCatalog by GameLibraryRepository.isSyncingCatalog.collectAsState()
    val isLoadingInitialCatalog by GameLibraryRepository.isLoadingInitialCatalog.collectAsState()
    val isSearchingLiveXbox by GameLibraryRepository.isSearchingLiveXbox.collectAsState()
    val isEn = config.isEnglishUI()

    var subSection by rememberSaveable { mutableStateOf(TvLibrarySubSection.GAMES) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableStateOf(GameFilterTab.ALL) }
    var selectedGenre by rememberSaveable { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDetailGame by remember { mutableStateOf<GameBookmark?>(null) }
    var focusedGameId by remember { mutableStateOf<String?>(null) }

    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(focusSearchTrigger) {
        if (focusSearchTrigger > 0) {
            if (subSection != TvLibrarySubSection.GAMES) {
                subSection = TvLibrarySubSection.GAMES
                delay(50L)
            }
            runCatching { searchFocusRequester.requestFocus() }
        }
    }

    var lastHandledFavTrigger by remember { mutableIntStateOf(favoriteToggleTrigger) }
    LaunchedEffect(favoriteToggleTrigger) {
        if (favoriteToggleTrigger != lastHandledFavTrigger) {
            lastHandledFavTrigger = favoriteToggleTrigger
            val targetId = focusedGameId
            if (!targetId.isNullOrBlank() && subSection == TvLibrarySubSection.GAMES) {
                GameLibraryRepository.toggleFavorite(targetId)
            }
        }
    }

    LaunchedEffect(games.size, isLoadingInitialCatalog) {
        if (!isLoadingInitialCatalog && !isSyncingCatalog && games.size < 120) {
            GameLibraryRepository.syncLiveXboxCatalog(force = true, maxCatalogGames = 600)
        }
    }

    val computedLibraryState by produceState(
        initialValue = Pair(games, intArrayOf(0, 0, 0, 0, 0)),
        games,
        selectedTab,
        selectedGenre,
        searchQuery,
        favoriteIds,
        playedGameIds
    ) {
        value = withContext(Dispatchers.Default) {
            val playedOrderMap = HashMap<String, Int>(playedGameIds.size * 2)
            playedGameIds.forEachIndexed { idx, id ->
                playedOrderMap[id.lowercase()] = idx
            }

            val filtered = games.filter { game ->
                val matchesTab = when (selectedTab) {
                    GameFilterTab.ALL -> true
                    GameFilterTab.CONTINUE_PLAYING -> playedOrderMap.containsKey(game.id.lowercase())
                    GameFilterTab.FAVORITES -> game.id in favoriteIds
                    GameFilterTab.POPULAR -> game.isPopular
                    GameFilterTab.RECENTLY_ADDED -> game.isRecentlyAdded
                    GameFilterTab.FREE_TO_PLAY -> game.freeToPlay
                    GameFilterTab.TOUCH_FRIENDLY -> game.supportsTouch
                    GameFilterTab.GAME_PASS -> !game.freeToPlay
                }
                val matchesGenre = GameLibraryRepository.matchesOfficialGenre(game, selectedGenre)
                val matchesSearch = if (searchQuery.isBlank()) true else {
                    game.title.contains(searchQuery, ignoreCase = true) ||
                        game.category.contains(searchQuery, ignoreCase = true) ||
                        game.categoryEn.contains(searchQuery, ignoreCase = true) ||
                        (game.publisher?.contains(searchQuery, ignoreCase = true) == true)
                }
                matchesTab && matchesGenre && matchesSearch
            }.let { list ->
                if (selectedTab == GameFilterTab.CONTINUE_PLAYING) {
                    list.sortedBy { playedOrderMap[it.id.lowercase()] ?: Int.MAX_VALUE }
                } else {
                    list.sortedByDescending { it.id in favoriteIds }
                }
            }

            var fav = 0
            var pop = 0
            var rec = 0
            var free = 0
            var touch = 0
            for (g in games) {
                if (g.id in favoriteIds) fav++
                if (g.isPopular) pop++
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("tv_library_screen"),
        verticalArrangement = Arrangement.spacedBy((12.dp * scale).coerceAtLeast(8.dp))
    ) {
        // 1. Top Header: "Thư viện" + Segmented (Thư viện / Lịch sử) + Count Pill + Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((12.dp * scale).coerceAtLeast(8.dp))
            ) {
                Text(
                    text = config.tr("Thư viện", "Library"),
                    color = TvInk,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = (22f * scale).coerceAtLeast(16.5f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )

                TvSegmentedControl(
                    options = listOf(
                        config.tr("Game (${games.size})", "Games (${games.size})"),
                        config.tr("Lịch sử", "History")
                    ),
                    selectedIndex = if (subSection == TvLibrarySubSection.GAMES) 0 else 1,
                    onSelect = { idx ->
                        subSection = if (idx == 0) TvLibrarySubSection.GAMES else TvLibrarySubSection.HISTORY
                    },
                    scale = scale * 0.92f
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((8.dp * scale).coerceAtLeast(6.dp))
            ) {
                TvChip(
                    text = if (isSyncingCatalog) {
                        config.tr("Đang đồng bộ...", "Syncing...")
                    } else {
                        config.tr("↻ Đồng bộ Xbox", "↻ Sync Xbox")
                    },
                    selected = isSyncingCatalog,
                    scale = scale * 0.9f,
                    onClick = {
                        if (!isSyncingCatalog) {
                            GameLibraryRepository.syncLiveXboxCatalog(force = true, maxCatalogGames = 600)
                        }
                    }
                )

                TvChip(
                    text = config.tr("+ Thêm game", "+ Add game"),
                    selected = false,
                    scale = scale * 0.9f,
                    onClick = { showAddDialog = true }
                )
            }
        }

        if (subSection == TvLibrarySubSection.HISTORY) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                SessionHistoryScreen(onLaunchGame = onLaunchGame)
            }
        } else {
            // 2. Search Row + Filter Chips (.row-bar in tv-mockup.html)
            val filterOptions: List<Pair<GameFilterTab, String>> = listOf(
                GameFilterTab.ALL to config.tr("Tất cả", "All"),
                GameFilterTab.FAVORITES to if (favCount > 0) {
                    "${config.tr("Yêu thích", "Favorites")} ($favCount)"
                } else {
                    config.tr("Yêu thích", "Favorites")
                },
                GameFilterTab.CONTINUE_PLAYING to if (playedGameIds.isNotEmpty()) {
                    "${config.tr("Gần đây", "Recent")} (${playedGameIds.size})"
                } else {
                    config.tr("Gần đây", "Recent")
                },
                GameFilterTab.FREE_TO_PLAY to "${config.tr("Miễn phí", "Free")} ($freeCount)",
                GameFilterTab.POPULAR to "${config.tr("Phổ biến", "Popular")} ($popCount)",
                GameFilterTab.RECENTLY_ADDED to "${config.tr("Mới", "New")} ($recentCount)"
            )
            val genreOptions = listOf(
                "ACTION" to config.tr("Hành động", "Action"),
                "SHOOTER" to config.tr("Bắn súng", "Shooter"),
                "RPG" to config.tr("Nhập vai", "RPG"),
                "FIGHTING" to config.tr("Đối kháng", "Fighting"),
                "STRATEGY" to config.tr("Chiến thuật", "Strategy"),
                "SIMULATION" to config.tr("Mô phỏng", "Simulation"),
                "INDIE" to "Indie",
                "FAMILY" to config.tr("Gia đình", "Family")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((10.dp * scale).coerceAtLeast(8.dp))
            ) {
                val searchCorner = (14.dp * scale).coerceAtLeast(10.dp)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .width((215.dp * scale).coerceIn(155.dp, 265.dp))
                        .gamepadTextFieldFocus(
                            cornerRadius = searchCorner,
                            focusRequester = searchFocusRequester
                        )
                        .testTag("tv_library_search_input"),
                    placeholder = {
                        Text(
                            text = config.tr("Tìm tên game... (phím X)", "Search games... (X)"),
                            color = TvMuted,
                            fontSize = (12.5f * scale).coerceAtLeast(10.5f).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    textStyle = TextStyle(
                        color = TvInk,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = (13f * scale).coerceAtLeast(11f).sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TvMuted,
                            modifier = Modifier.size((18.dp * scale).coerceAtLeast(15.dp))
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.gamepadActionButton(
                                    cornerRadius = 14.dp,
                                    onClick = { searchQuery = "" }
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = config.tr("Xóa tìm kiếm", "Clear"),
                                    tint = TvMuted,
                                    modifier = Modifier.size((18.dp * scale).coerceAtLeast(14.dp))
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TvAccent,
                        unfocusedBorderColor = TvLine,
                        focusedContainerColor = TvSurface2,
                        unfocusedContainerColor = TvSurface
                    ),
                    shape = RoundedCornerShape(searchCorner),
                    singleLine = true
                )

                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy((8.dp * scale).coerceAtLeast(6.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(filterOptions, key = { "tv_filter_${it.first.name}" }) { (tab, label) ->
                        val isSelected = selectedTab == tab && selectedGenre == "ALL"
                        TvChip(
                            text = label,
                            selected = isSelected,
                            scale = scale,
                            onClick = {
                                selectedTab = tab
                                selectedGenre = "ALL"
                            },
                            modifier = Modifier.testTag("tv_filter_tab_${tab.name.lowercase()}")
                        )
                    }
                    items(genreOptions, key = { "tv_genre_${it.first}" }) { (code, label) ->
                        val isSelected = selectedGenre == code
                        TvChip(
                            text = label,
                            selected = isSelected,
                            scale = scale,
                            onClick = {
                                if (selectedGenre == code) {
                                    selectedGenre = "ALL"
                                    selectedTab = GameFilterTab.ALL
                                } else {
                                    selectedTab = GameFilterTab.ALL
                                    selectedGenre = code
                                }
                            },
                            modifier = Modifier.testTag("tv_genre_chip_${code.lowercase()}")
                        )
                    }
                }
            }

            if (searchQuery.trim().length >= 2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape((12.dp * scale).coerceAtLeast(9.dp)))
                        .background(Color(0xFF0E2A31))
                        .gamepadClickable(
                            enabled = !isSearchingLiveXbox,
                            cornerRadius = (12.dp * scale).coerceAtLeast(9.dp),
                            onClick = { GameLibraryRepository.searchMicrosoftXboxCatalog(searchQuery) }
                        )
                        .padding(horizontal = (14.dp * scale).coerceAtLeast(10.dp), vertical = (8.dp * scale).coerceAtLeast(6.dp)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSearchingLiveXbox) {
                            config.tr(
                                "Đang tìm \"$searchQuery\" trên Xbox Store...",
                                "Searching Xbox Store for \"$searchQuery\"..."
                            )
                        } else {
                            config.tr(
                                "🔍 Bấm A để tìm thêm \"$searchQuery\" trực tiếp trên Microsoft Xbox Store",
                                "🔍 Press A to search \"$searchQuery\" live on Microsoft Xbox Store"
                            )
                        },
                        color = TvCyan,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = (14f * scale).coerceAtLeast(11.5f).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 3. 4-Column TV Game Grid (.grid4 -> .gc in tv-mockup.html)
            if (filteredGames.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy((10.dp * scale).coerceAtLeast(8.dp))
                    ) {
                        Text(
                            text = if (isSyncingCatalog && games.isEmpty()) {
                                config.tr("Đang tải danh sách game từ Xbox Cloud...", "Loading games from Xbox Cloud...")
                            } else {
                                config.tr("Không tìm thấy game phù hợp", "No matching games found")
                            },
                            color = TvMuted,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = (18f * scale).coerceAtLeast(14f).sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (searchQuery.isNotBlank()) {
                            TvButton(
                                text = config.tr("Xóa bộ lọc tìm kiếm", "Clear search filter"),
                                scale = scale,
                                onClick = {
                                    searchQuery = ""
                                    selectedTab = GameFilterTab.ALL
                                    selectedGenre = "ALL"
                                }
                            )
                        }
                    }
                }
            } else {
                val gridState = rememberLazyGridState()
                var visibleLimit by remember(selectedTab, selectedGenre, searchQuery) {
                    mutableIntStateOf(40)
                }
                val displayedGames = remember(filteredGames, visibleLimit) {
                    if (filteredGames.size <= visibleLimit) filteredGames else filteredGames.take(visibleLimit)
                }
                val shouldLoadMore by remember(gridState, displayedGames.size, filteredGames.size) {
                    derivedStateOf {
                        if (displayedGames.size >= filteredGames.size) {
                            false
                        } else {
                            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            lastVisible >= (displayedGames.size - 8).coerceAtLeast(0)
                        }
                    }
                }
                LaunchedEffect(shouldLoadMore) {
                    if (shouldLoadMore && visibleLimit < filteredGames.size) {
                        visibleLimit = (visibleLimit + 40).coerceAtMost(filteredGames.size)
                    }
                }

                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = (145.dp * scale).coerceIn(112.dp, 180.dp)),
                    contentPadding = PaddingValues(bottom = (8.dp * scale).coerceAtLeast(6.dp)),
                    horizontalArrangement = Arrangement.spacedBy((10.dp * scale).coerceAtLeast(8.dp)),
                    verticalArrangement = Arrangement.spacedBy((10.dp * scale).coerceAtLeast(8.dp)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("tv_library_grid")
                ) {
                    itemsIndexed(
                        items = displayedGames,
                        key = { _, g -> "tv_lib_${g.id}" },
                        contentType = { _, _ -> "tv_game_card" }
                    ) { index, game ->
                        val isFav = game.id in favoriteIds
                        TvLibraryGameCard(
                            game = game,
                            subtitle = config.tr(game.category, game.categoryEn),
                            isFavorite = isFav,
                            freeLabel = if (game.freeToPlay) config.tr("Miễn phí", "Free") else null,
                            scale = scale,
                            onFocused = {
                                focusedGameId = game.id
                                if (index >= displayedGames.size - 8 && visibleLimit < filteredGames.size) {
                                    visibleLimit = (visibleLimit + 40).coerceAtMost(filteredGames.size)
                                }
                            },
                            onClick = { onLaunchGame(game.playUrl) },
                            onLongClickOrDetail = { selectedDetailGame = game }
                        )
                    }
                }
            }
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
                onLaunchGame(detailGame.playUrl)
            }
        )
    }
}

@Composable
private fun TvLibraryGameCard(
    game: GameBookmark,
    subtitle: String,
    isFavorite: Boolean,
    freeLabel: String?,
    scale: Float,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongClickOrDetail: () -> Unit
) {
    val context = LocalContext.current
    val cardH = (96.dp * scale).coerceIn(74.dp, 118.dp)
    val corner = (12.dp * scale).coerceAtLeast(9.dp)

    val imageRequest = remember(game.imageUrl) {
        game.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .size(340, 200)
                .allowRgb565(true)
                .crossfade(false)
                .build()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardH)
            .clip(RoundedCornerShape(corner))
            .background(TvSurface)
            .onFocusChanged { state ->
                if (state.isFocused) onFocused()
            }
            .gamepadClickable(
                cornerRadius = corner,
                onClick = onClick
            )
            .testTag("tv_lib_game_card_${game.id}")
    ) {
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                alpha = 0.36f,
                modifier = Modifier.fillMaxSize()
            )
        } else if (game.drawableResId != null) {
            Image(
                painter = painterResource(id = game.drawableResId),
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                alpha = 0.36f,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top-right badges (Favorite star + Info button)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding((10.dp * scale).coerceAtLeast(7.dp)),
            horizontalArrangement = Arrangement.spacedBy((6.dp * scale).coerceAtLeast(4.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (freeLabel != null) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF10301F))
                        .padding(horizontal = (8.dp * scale).coerceAtLeast(6.dp), vertical = (2.dp * scale).coerceAtLeast(1.dp))
                ) {
                    Text(
                        text = freeLabel,
                        color = TvAccent,
                        fontSize = (11f * scale).coerceAtLeast(9.5f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (isFavorite) {
                Text(
                    text = "★",
                    color = TvWarn,
                    fontSize = (16f * scale).coerceAtLeast(13f).sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Bottom gradient + Title & Subtitle (.gc .t & .gc .s in tv-mockup.html)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            TvSurface.copy(alpha = 0.90f)
                        )
                    )
                )
                .padding((14.dp * scale).coerceAtLeast(10.dp)),
            contentAlignment = Alignment.BottomStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy((2.dp * scale).coerceAtLeast(1.dp))
                ) {
                    Text(
                        text = game.title,
                        color = TvInk,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = (13.5f * scale).coerceAtLeast(11f).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        color = TvMuted,
                        fontSize = (11.5f * scale).coerceAtLeast(9.5f).sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width((6.dp * scale).coerceAtLeast(4.dp)))

                Box(
                    modifier = Modifier
                        .size((28.dp * scale).coerceAtLeast(22.dp))
                        .clip(CircleShape)
                        .background(TvSurface2.copy(alpha = 0.85f))
                        .gamepadClickable(
                            cornerRadius = 99.dp,
                            onClick = onLongClickOrDetail
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ℹ",
                        color = TvMuted,
                        fontSize = (13f * scale).coerceAtLeast(10.5f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
