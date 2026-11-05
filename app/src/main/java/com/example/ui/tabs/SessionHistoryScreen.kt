package com.example.ui.tabs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameLibraryRepository
import com.example.data.PlaySessionRecord
import com.example.data.SessionHistoryRepository
import com.example.data.SettingsRepository
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JapanRed
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant
import com.example.ui.theme.rememberXboxMotionEnabled
import com.example.ui.theme.xboxPressScale

private enum class HistoryFilterMode {
    ALL,
    SMOOTH,
    ALERTS
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

    val filteredSessions = remember(sessions, filterMode) {
        when (filterMode) {
            HistoryFilterMode.ALL -> sessions
            HistoryFilterMode.SMOOTH -> sessions.filter { it.avgFps >= 55 && !it.isSlowDecode && !it.isThermalHot }
            HistoryFilterMode.ALERTS -> sessions.filter { it.isSlowDecode || it.isThermalHot || it.avgPingMs > 110 }
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

    val oneWeekAgoMs = remember(sessions) {
        System.currentTimeMillis() - 7L * 24L * 3600L * 1000L
    }
    val weeklySessions = remember(sessions, oneWeekAgoMs) {
        sessions.filter { it.startTimeMillis >= oneWeekAgoMs }
    }
    val weeklyTotalSeconds = remember(weeklySessions) {
        weeklySessions.sumOf { it.durationSeconds }
    }
    val weeklyAvgPing = remember(weeklySessions) {
        val valid = weeklySessions.filter { it.avgPingMs > 0 }
        if (valid.isNotEmpty()) valid.sumOf { it.avgPingMs } / valid.size else 0
    }

    val globalSmartTip = remember(sessions, isEn) {
        buildGlobalSmartTip(sessions, isEn)
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Thẻ tổng quan "Tuần này"
        WeeklySummaryCard(
            totalDurationSec = weeklyTotalSeconds,
            sessionCount = weeklySessions.size,
            avgPingMs = weeklyAvgPing,
            hasAnyHistory = sessions.isNotEmpty(),
            isEn = isEn,
            onClearAllClick = { showClearConfirmDialog = true }
        )

        // Bộ lọc nhanh khi có lịch sử
        if (sessions.isNotEmpty()) {
            val smoothCount = remember(sessions) {
                sessions.count { it.avgFps >= 55 && !it.isSlowDecode && !it.isThermalHot }
            }
            val alertCount = remember(sessions) {
                sessions.count { it.isSlowDecode || it.isThermalHot || it.avgPingMs > 110 }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val filterItems = listOf(
                    HistoryFilterMode.ALL to (if (isEn) "All (${sessions.size})" else "Tất cả (${sessions.size})"),
                    HistoryFilterMode.SMOOTH to (if (isEn) "Smooth ($smoothCount)" else "Mượt ($smoothCount)"),
                    HistoryFilterMode.ALERTS to (if (isEn) "Alerts ($alertCount)" else "Cần lưu ý ($alertCount)")
                )
                filterItems.forEach { (mode, label) ->
                    val isSelected = filterMode == mode
                    val filterChipBg by animateColorAsState(
                        targetValue = if (isSelected) XboxGreen.copy(alpha = 0.28f) else XboxSurfaceVariant.copy(alpha = 0.85f),
                        animationSpec = tween(durationMillis = if (motionEnabled) 150 else 0),
                        label = "history_filter_bg"
                    )
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .xboxPressScale(enabled = motionEnabled, pressedScale = 0.96f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { filterMode = mode }
                            .testTag("history_filter_${mode.name.lowercase()}"),
                        shape = RoundedCornerShape(10.dp),
                        color = filterChipBg,
                        border = BorderStroke(
                            0.9.dp,
                            if (isSelected) XboxNeonGreen else XboxCardBorder
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) XboxNeonGreen else TextSecondary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Gợi ý thông minh ngắn ở đầu trang (nếu có)
        if (globalSmartTip != null) {
            SmartSuggestionBanner(text = globalSmartTip)
        }

        // 2. Danh sách phiên chơi hoặc trạng thái trống
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
                            .background(XboxSurfaceVariant.copy(alpha = 0.85f))
                            .border(1.dp, XboxCardBorder, CircleShape),
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
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

    // Hộp xác nhận xóa tất cả lịch sử
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = if (isEn) "Clear all history?" else "Xóa tất cả lịch sử?",
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isEn) {
                        "All recorded play sessions will be permanently removed."
                    } else {
                        "Toàn bộ lịch sử các phiên chơi đã lưu sẽ bị xóa."
                    },
                    color = TextSecondary,
                    fontSize = 12.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        SessionHistoryRepository.clearAllSessions()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JapanRed),
                    modifier = Modifier.testTag("confirm_clear_history_button")
                ) {
                    Text(
                        text = if (isEn) "Delete All" else "Xóa tất cả",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(
                        text = if (isEn) "Cancel" else "Hủy",
                        color = TextSecondary
                    )
                }
            },
            containerColor = XboxSurface
        )
    }
}
