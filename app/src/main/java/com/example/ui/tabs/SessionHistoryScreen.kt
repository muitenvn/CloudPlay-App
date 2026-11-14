package com.example.ui.tabs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.GameLibraryRepository
import com.example.data.PlaySessionRecord
import com.example.data.SessionHistoryRepository
import com.example.data.SettingsRepository
import com.example.ui.components.gamepadActionButton
import com.example.ui.components.gamepadClickable
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JapanRed
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant
import com.example.ui.theme.rememberXboxMotionEnabled
import com.example.ui.theme.xboxPressScale

private enum class HistoryFilterMode {
    ALL,
    SMOOTH,
    ALERTS,
    THIS_WEEK
}

@Composable
fun SessionHistoryScreen(
    onLaunchGame: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sessions by SessionHistoryRepository.sessions.collectAsState()
    val allGames by GameLibraryRepository.games.collectAsState()
    val config by SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    val motionEnabled = rememberXboxMotionEnabled(
        lowEndDeviceMode = config.lowEndDeviceMode,
        gpuLoadReductionEnabled = config.gpuLoadReductionEnabled
    )

    var selectedSession by remember { mutableStateOf<PlaySessionRecord?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var filterMode by remember { mutableStateOf(HistoryFilterMode.ALL) }

    val oneWeekAgoMs = remember(sessions) {
        System.currentTimeMillis() - 7L * 24L * 3600L * 1000L
    }

    val filteredSessions = remember(sessions, filterMode, oneWeekAgoMs) {
        when (filterMode) {
            HistoryFilterMode.ALL -> sessions
            HistoryFilterMode.SMOOTH -> sessions.filter { it.avgFps >= 55 && !it.isSlowDecode && !it.isThermalHot }
            HistoryFilterMode.ALERTS -> sessions.filter { it.isSlowDecode || it.isThermalHot || it.avgPingMs > 110 }
            HistoryFilterMode.THIS_WEEK -> sessions.filter { it.startTimeMillis >= oneWeekAgoMs }
        }
    }

    fun resolvePlayUrlForSession(session: PlaySessionRecord): String {
        val trimmed = session.gameTitle.trim()
        val matched = allGames.firstOrNull { it.title.equals(trimmed, ignoreCase = true) }
            ?: allGames.firstOrNull {
                it.title.contains(trimmed, ignoreCase = true) ||
                    trimmed.contains(it.title, ignoreCase = true)
            }
        return when {
            matched != null -> matched.playUrl
            trimmed.contains("fortnite", ignoreCase = true) -> "https://www.xbox.com/en-US/play/games/fortnite/BT5P2X999VH2"
            else -> config.targetRegionUrl
        }
    }

    val totalSessionsCount = filteredSessions.size
    val totalDurationSeconds = remember(filteredSessions) {
        filteredSessions.sumOf { it.durationSeconds }
    }
    val weeklyAvgPing = remember(filteredSessions) {
        val valid = filteredSessions.filter { it.avgPingMs > 0 }
        if (valid.isNotEmpty()) valid.sumOf { it.avgPingMs } / valid.size else 0
    }

    val globalSmartTip = remember(sessions, isEn) {
        buildGlobalSmartTip(sessions, isEn)
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. 2 ô tổng quan (Số phiên | Tổng thời gian) chuẩn #libHist
        WeeklySummaryCard(
            totalDurationSec = totalDurationSeconds,
            sessionCount = totalSessionsCount,
            avgPingMs = weeklyAvgPing,
            hasAnyHistory = sessions.isNotEmpty(),
            isEn = isEn,
            onClearAllClick = { showClearConfirmDialog = true }
        )

        // 2. Hàng chip lọc: Tất cả, Mượt, Cần lưu ý, Tuần này
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filterItems = listOf(
                HistoryFilterMode.ALL to (if (isEn) "All" else "Tất cả"),
                HistoryFilterMode.SMOOTH to (if (isEn) "Smooth" else "Mượt"),
                HistoryFilterMode.ALERTS to (if (isEn) "Needs Attention" else "Cần lưu ý"),
                HistoryFilterMode.THIS_WEEK to (if (isEn) "This Week" else "Tuần này")
            )
            filterItems.forEach { (mode, label) ->
                val isSelected = filterMode == mode
                val filterChipBg by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF16352A) else XboxSurface,
                    animationSpec = tween(durationMillis = if (motionEnabled) 150 else 0),
                    label = "history_filter_bg"
                )
                Surface(
                    modifier = Modifier
                        .xboxPressScale(enabled = motionEnabled, pressedScale = 0.96f)
                        .clip(CircleShape)
                        .gamepadClickable(cornerRadius = 999.dp) { filterMode = mode }
                        .testTag("history_filter_${mode.name.lowercase()}"),
                    shape = CircleShape,
                    color = filterChipBg
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) XboxNeonGreen else TextSecondary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Gợi ý thông minh ngắn ở đầu trang (nếu có)
        if (globalSmartTip != null) {
            SmartSuggestionBanner(text = globalSmartTip)
        }

        // 3. Danh sách phiên chơi hoặc trạng thái trống
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("session_history_empty_state"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(XboxSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = DirectCyan,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Text(
                        text = if (isEn) {
                            "No session history"
                        } else {
                            "Chưa có lịch sử"
                        },
                        color = TextSecondary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("session_history_list"),
                contentPadding = PaddingValues(bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSessions, key = { it.id }) { session ->
                    val matchedBookmark = remember(session.gameTitle, allGames) {
                        val trimmed = session.gameTitle.trim()
                        allGames.firstOrNull { it.title.equals(trimmed, ignoreCase = true) }
                            ?: allGames.firstOrNull {
                                it.title.contains(trimmed, ignoreCase = true) ||
                                    trimmed.contains(it.title, ignoreCase = true)
                            }
                    }
                    SessionHistoryItemCard(
                        session = session,
                        coverDrawableResId = matchedBookmark?.drawableResId,
                        coverImageUrl = matchedBookmark?.imageUrl,
                        accentColorHex = matchedBookmark?.accentColorHex ?: 0xFF107C10L,
                        isEn = isEn,
                        onClick = { selectedSession = session },
                        onPlayAgain = if (onLaunchGame != null) {
                            { onLaunchGame(resolvePlayUrlForSession(session)) }
                        } else null
                    )
                }

                item(key = "clear_all_history_footer") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .gamepadClickable(cornerRadius = 12.dp) { showClearConfirmDialog = true }
                                .testTag("clear_all_history_button"),
                            shape = RoundedCornerShape(12.dp),
                            color = XboxSurfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isEn) "Clear All History" else "Xóa tất cả lịch sử",
                                    color = JapanRed,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = if (isEn) {
                                "Tap a session to view details and lag diagnosis."
                            } else {
                                "Chạm vào một phiên để xem chi tiết và vì sao có lag."
                            },
                            color = TextSecondary,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }
    }

    // Hộp thoại chi tiết phiên chơi (chỉ vẽ biểu đồ khi mở)
    selectedSession?.let { activeSession ->
        val bestServerTip = remember(activeSession, sessions, isEn) {
            buildSessionSmartTip(activeSession, sessions, isEn)
        }
        SessionDetailDialog(
            session = activeSession,
            smartTip = bestServerTip,
            isEn = isEn,
            onDismiss = { selectedSession = null },
            onPlayAgain = if (onLaunchGame != null) {
                {
                    val url = resolvePlayUrlForSession(activeSession)
                    selectedSession = null
                    onLaunchGame(url)
                }
            } else null,
            onDelete = {
                SessionHistoryRepository.deleteSession(activeSession.id)
                selectedSession = null
            }
        )
    }

    // Hộp xác nhận xóa tất cả lịch sử theo khuôn .dlg
    if (showClearConfirmDialog) {
        val cancelClearFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
        Dialog(onDismissRequest = { showClearConfirmDialog = false }) {
            com.example.ui.components.GamepadSurfaceController(
                enabled = true,
                defaultFocusRequester = cancelClearFocusRequester,
                autoRequestDefaultFocus = true,
                attachWindowMotionListener = true,
                onBackPress = { showClearConfirmDialog = false }
            ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .testTag("clear_history_confirm_dialog"),
                shape = RoundedCornerShape(22.dp),
                color = XboxSurfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isEn) "Clear all history?" else "Xóa tất cả lịch sử?",
                        color = TextPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = if (isEn) {
                            "All recorded play sessions will be permanently removed."
                        } else {
                            "Toàn bộ lịch sử các phiên chơi đã lưu sẽ bị xóa."
                        },
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showClearConfirmDialog = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = XboxSurface,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .gamepadActionButton(
                                    cornerRadius = 12.dp,
                                    focusRequester = cancelClearFocusRequester
                                ) {
                                    showClearConfirmDialog = false
                                }
                        ) {
                            Text(
                                text = if (isEn) "Cancel" else "Hủy",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Button(
                            onClick = {
                                SessionHistoryRepository.clearAllSessions()
                                showClearConfirmDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JapanRed,
                                contentColor = Color(0xFF2A0A09)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .gamepadActionButton(cornerRadius = 12.dp) {
                                    SessionHistoryRepository.clearAllSessions()
                                    showClearConfirmDialog = false
                                }
                                .testTag("confirm_clear_history_button")
                        ) {
                            Text(
                                text = if (isEn) "Delete All" else "Xóa tất cả",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            }
        }
    }
}
