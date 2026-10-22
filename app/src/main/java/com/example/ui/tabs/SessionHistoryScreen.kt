package com.example.ui.tabs

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.GameLibraryRepository
import com.example.data.PlaySessionRecord
import com.example.data.SessionHistoryRepository
import com.example.data.SettingsRepository
import com.example.ui.components.PingSparklineChart
import com.example.ui.theme.DirectCyan
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { filterMode = mode }
                            .testTag("history_filter_${mode.name.lowercase()}"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) XboxGreen.copy(alpha = 0.28f) else XboxSurfaceVariant.copy(alpha = 0.85f),
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
                            "No play sessions yet. Launch a game to start recording."
                        } else {
                            "Chưa có phiên chơi nào. Vào game để bắt đầu ghi lại."
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

@Composable
private fun WeeklySummaryCard(
    totalDurationSec: Long,
    sessionCount: Int,
    avgPingMs: Int,
    hasAnyHistory: Boolean,
    isEn: Boolean,
    onClearAllClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weekly_summary_card"),
        shape = RoundedCornerShape(16.dp),
        color = XboxSurface.copy(alpha = 0.95f),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(XboxNeonGreen.copy(alpha = 0.45f), XboxCardBorder, DirectCyan.copy(alpha = 0.45f))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEn) "THIS WEEK" else "TUẦN NÀY",
                    color = XboxNeonGreen,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )

                if (hasAnyHistory) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = JapanRed.copy(alpha = 0.14f),
                        border = BorderStroke(0.8.dp, JapanRed.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onClearAllClick() }
                            .testTag("clear_all_history_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = JapanRed,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isEn) "Clear All" else "Xóa tất cả lịch sử",
                                color = JapanRed,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryStatTile(
                    label = if (isEn) "Play Time" else "Tổng thời gian",
                    value = formatDurationCompact(totalDurationSec, isEn),
                    valueColor = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                SummaryStatTile(
                    label = if (isEn) "Sessions" else "Số phiên",
                    value = "$sessionCount",
                    valueColor = DirectCyan,
                    modifier = Modifier.weight(1f)
                )
                SummaryStatTile(
                    label = if (isEn) "Avg Ping" else "Ping TB",
                    value = if (avgPingMs > 0) "${avgPingMs}ms" else "--",
                    valueColor = if (avgPingMs > 0) pingMetricColor(avgPingMs) else TextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryStatTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = XboxSurfaceVariant.copy(alpha = 0.85f),
        border = BorderStroke(0.8.dp, XboxCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                color = valueColor,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SmartSuggestionBanner(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_smart_suggestion"),
        shape = RoundedCornerShape(12.dp),
        color = DirectCyan.copy(alpha = 0.10f),
        border = BorderStroke(0.8.dp, DirectCyan.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = DirectCyan,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                color = TextPrimary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SessionHistoryItemCard(
    session: PlaySessionRecord,
    coverDrawableResId: Int?,
    coverImageUrl: String? = null,
    accentColorHex: Long,
    isEn: Boolean,
    onClick: () -> Unit,
    onPlayAgain: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, XboxCardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("session_card_${session.id}"),
        colors = CardDefaults.cardColors(containerColor = XboxSurface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ảnh bìa game hoặc biểu tượng mặc định
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(0.8.dp, Color(accentColorHex).copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (coverDrawableResId != null) {
                    Image(
                        painter = painterResource(id = coverDrawableResId),
                        contentDescription = session.gameTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(accentColorHex).copy(alpha = 0.7f), XboxDark)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                        if (!coverImageUrl.isNullOrBlank()) {
                            val context = LocalContext.current
                            val request = remember(coverImageUrl) {
                                ImageRequest.Builder(context)
                                    .data(coverImageUrl)
                                    .crossfade(false)
                                    .build()
                            }
                            AsyncImage(
                                model = request,
                                contentDescription = session.gameTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Hàng tiêu đề + thời lượng
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.gameTitle,
                        color = TextPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatDurationCompact(session.durationSeconds, isEn),
                        color = XboxNeonGreen,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Ngày giờ + máy chủ/mạng
                Text(
                    text = "${formatSessionDateTime(session.startTimeMillis)} • ${session.formattedNetworkLabel(isEn)}",
                    color = TextSecondary,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 3 chip số liệu (Ping / FPS / Giải mã) + nút Chơi lại nhanh
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MetricChip(
                            text = "Ping ${session.avgPingMs}ms",
                            color = pingMetricColor(session.avgPingMs)
                        )
                        MetricChip(
                            text = "${session.avgFps} FPS",
                            color = fpsMetricColor(session.avgFps)
                        )
                        MetricChip(
                            text = if (isEn) "Dec ${session.avgDecodeMs}ms" else "Giải mã ${session.avgDecodeMs}ms",
                            color = decodeMetricColor(session.avgDecodeMs)
                        )
                    }

                    if (onPlayAgain != null) {
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = XboxGreen.copy(alpha = 0.25f),
                            border = BorderStroke(0.7.dp, XboxNeonGreen.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .clickable { onPlayAgain() }
                                .testTag("session_play_again_${session.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = if (isEn) "Play Again" else "Chơi lại",
                                    tint = XboxNeonGreen,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = if (isEn) "Play" else "Chơi lại",
                                    color = XboxNeonGreen,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                if (session.isThermalHot || session.isSlowDecode) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (session.isThermalHot) {
                            WarningBadgePill(
                                text = if (isEn) "Device Hot" else "Máy nóng",
                                color = JapanRed
                            )
                        }
                        if (session.isSlowDecode) {
                            WarningBadgePill(
                                text = if (isEn) "Slow Decode" else "Giải mã chậm",
                                color = WarningAmber
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(0.7.dp, color.copy(alpha = 0.45f))
    ) {
        Text(
            text = text,
            color = color,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
        )
    }
}

@Composable
private fun WarningBadgePill(
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.18f),
        border = BorderStroke(0.6.dp, color.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Whatshot,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = text,
                color = color,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SessionDetailDialog(
    session: PlaySessionRecord,
    smartTip: String?,
    isEn: Boolean,
    onDismiss: () -> Unit,
    onPlayAgain: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    val chartSamples = remember(session.pingSamples, session.avgPingMs) {
        if (session.pingSamples.isNotEmpty()) session.pingSamples else listOf(session.avgPingMs.coerceAtLeast(25))
    }
    val minPing = remember(chartSamples) { chartSamples.minOrNull() ?: session.avgPingMs }
    val maxPing = remember(chartSamples) { chartSamples.maxOrNull() ?: session.avgPingMs }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(22.dp))
                .border(
                    1.2.dp,
                    Brush.verticalGradient(listOf(XboxNeonGreen, DirectCyan, XboxCardBorder)),
                    RoundedCornerShape(22.dp)
                )
                .testTag("session_detail_dialog"),
            color = XboxDark.copy(alpha = 0.98f),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.gameTitle,
                            color = TextPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${formatSessionDateTime(session.startTimeMillis)} • ${session.formattedNetworkLabel(isEn)}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isEn) "Close" else "Đóng",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Biểu đồ đường ping theo thời gian
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = XboxSurfaceVariant.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, XboxCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = DirectCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isEn) "PING OVER TIME" else "BIỂU ĐỒ PING THEO THỜI GIAN",
                                    color = TextSecondary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (isEn) "Min ${minPing}ms • Max ${maxPing}ms" else "Thấp ${minPing}ms • Cao ${maxPing}ms",
                                color = DirectCyan,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        PingSparklineChart(
                            samples = chartSamples,
                            strokeColor = pingMetricColor(session.avgPingMs),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                        )
                    }
                }

                // Số liệu đầy đủ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryStatTile(
                        label = if (isEn) "Duration" else "Thời lượng",
                        value = formatDurationCompact(session.durationSeconds, isEn),
                        valueColor = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatTile(
                        label = if (isEn) "Avg Ping" else "Ping TB",
                        value = "${session.avgPingMs}ms",
                        valueColor = pingMetricColor(session.avgPingMs),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryStatTile(
                        label = if (isEn) "Avg FPS" else "FPS TB",
                        value = "${session.avgFps} FPS",
                        valueColor = fpsMetricColor(session.avgFps),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatTile(
                        label = if (isEn) "Decode Time" else "Giải mã TB",
                        value = "${session.avgDecodeMs}ms",
                        valueColor = decodeMetricColor(session.avgDecodeMs),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Trạng thái thiết bị
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = XboxSurface.copy(alpha = 0.9f),
                    border = BorderStroke(0.8.dp, XboxCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEn) "Device Status" else "Trạng thái máy",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        val statusText = when {
                            session.isThermalHot && session.isSlowDecode ->
                                if (isEn) "Device Hot • Slow Decode" else "Máy nóng • Giải mã chậm"
                            session.isThermalHot ->
                                if (isEn) "Device Hot" else "Máy nóng"
                            session.isSlowDecode ->
                                if (isEn) "Slow Decode (>12ms)" else "Giải mã chậm (>12ms)"
                            else ->
                                if (isEn) "Cool & Smooth" else "Mát & Ổn định"
                        }
                        val statusColor = when {
                            session.isThermalHot -> JapanRed
                            session.isSlowDecode -> WarningAmber
                            else -> XboxNeonGreen
                        }
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Gợi ý thông minh ở cuối chi tiết
                if (smartTip != null) {
                    SmartSuggestionBanner(text = smartTip)
                }

                // Nút Chơi lại, Xóa phiên và Đóng
                if (onPlayAgain != null) {
                    Button(
                        onClick = onPlayAgain,
                        colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("detail_play_again_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEn) "Play Again: ${session.gameTitle}" else "Chơi lại: ${session.gameTitle}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = JapanRed.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, JapanRed.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("delete_session_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = JapanRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEn) "Delete Session" else "Xóa phiên này",
                            color = JapanRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = XboxSurfaceVariant),
                        border = BorderStroke(1.dp, XboxCardBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Text(
                            text = if (isEn) "Close" else "Đóng",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun pingMetricColor(pingMs: Int): Color = when {
    pingMs <= 0 -> TextSecondary
    pingMs <= 60 -> XboxNeonGreen
    pingMs <= 110 -> WarningAmber
    else -> JapanRed
}

private fun fpsMetricColor(fps: Int): Color = when {
    fps >= 55 -> XboxNeonGreen
    fps >= 40 -> WarningAmber
    else -> JapanRed
}

private fun decodeMetricColor(decodeMs: Double): Color = when {
    decodeMs <= 8.0 -> XboxNeonGreen
    decodeMs <= 12.0 -> WarningAmber
    else -> JapanRed
}

private fun formatDurationCompact(durationSec: Long, isEn: Boolean): String {
    if (durationSec <= 0L) return if (isEn) "0m" else "0 phút"
    val hours = durationSec / 3600L
    val minutes = (durationSec % 3600L) / 60L
    val seconds = durationSec % 60L
    return when {
        hours > 0L -> if (isEn) "${hours}h ${minutes}m" else "${hours}g ${minutes}p"
        minutes > 0L -> if (isEn) "${minutes}m ${seconds}s" else "${minutes}p ${seconds}s"
        else -> "${seconds}s"
    }
}

private fun formatSessionDateTime(timestampMs: Long): String {
    return try {
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        sdf.format(Date(timestampMs))
    } catch (_: Exception) {
        ""
    }
}

private fun buildGlobalSmartTip(sessions: List<PlaySessionRecord>, isEn: Boolean): String? {
    if (sessions.isEmpty()) return null
    val recent = sessions.take(10)
    val slowOrHotCount = recent.count { it.isSlowDecode || it.isThermalHot }
    if (slowOrHotCount >= 2 || (recent.size == 1 && slowOrHotCount == 1)) {
        return if (isEn) {
            "Tip: Enable Low-End Device Mode in Settings for cooler and smoother play."
        } else {
            "Gợi ý: Bật Chế độ máy yếu trong Cài đặt để máy mát và chơi mượt hơn."
        }
    }
    return null
}

private fun buildSessionSmartTip(
    session: PlaySessionRecord,
    allSessions: List<PlaySessionRecord>,
    isEn: Boolean
): String? {
    val sameGameSessions = allSessions.filter {
        it.gameTitle.equals(session.gameTitle, ignoreCase = true) &&
            it.avgPingMs > 0 &&
            it.serverName.isNotBlank()
    }
    val byServer = sameGameSessions
        .groupBy { it.serverName }
        .mapValues { (_, list) -> list.sumOf { it.avgPingMs } / list.size }

    if (byServer.size >= 2) {
        val sorted = byServer.entries.sortedBy { it.value }
        val best = sorted.first()
        val secondBest = sorted[1]
        if (secondBest.value - best.value >= 5) {
            return if (isEn) {
                "Best server for this game: ${best.key} (~${best.value}ms)"
            } else {
                "Máy chủ tốt nhất cho game này: ${best.key} (~${best.value}ms)"
            }
        }
    }

    if (session.isSlowDecode || session.isThermalHot) {
        return if (isEn) {
            "Tip: Turn on Low-End Device Mode to reduce heat and frame delay."
        } else {
            "Gợi ý: Bật Chế độ máy yếu để giảm nóng máy và xử lý hình ảnh nhanh hơn."
        }
    }

    if (byServer.size == 1) {
        val only = byServer.entries.first()
        return if (isEn) {
            "Best server for this game: ${only.key} (~${only.value}ms)"
        } else {
            "Máy chủ tốt nhất cho game này: ${only.key} (~${only.value}ms)"
        }
    }
    return null
}
