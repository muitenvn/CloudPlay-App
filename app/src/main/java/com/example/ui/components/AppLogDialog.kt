package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppLogLevel
import com.example.data.AppLogger
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JapanRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant
import com.example.vpn.VpnManager

@Composable
fun AppLogDialog(
    isEn: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val logs by AppLogger.logs.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }

    val (errorCount, warnCount) = remember(logs) {
        var errs = 0
        var warns = 0
        for (entry in logs) {
            if (entry.level == AppLogLevel.ERROR) errs++
            else if (entry.level == AppLogLevel.WARN) warns++
        }
        errs to warns
    }

    val filteredLogs = remember(logs, selectedFilter) {
        val reversed = logs.asReversed()
        when (selectedFilter) {
            "ERRORS" -> reversed.filter { it.level == AppLogLevel.ERROR || it.level == AppLogLevel.WARN }
            "UI" -> reversed.filter { it.tag == "UI" || it.tag == "WEBVIEW" }
            "PROBE" -> reversed.filter { it.tag == "PROBE" || it.tag == "PING" }
            "VPN" -> reversed.filter { it.tag == "VPN" || it.tag == "UNLOCK" || it.tag == "SMARTSELECT" }
            "STREAM" -> reversed.filter { it.tag == "STREAM" }
            "PERF" -> reversed.filter { it.tag == "PERF" }
            else -> reversed
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_log_dialog"),
        containerColor = XboxSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            if (errorCount > 0) JapanRed.copy(alpha = 0.20f)
                            else XboxNeonGreen.copy(alpha = 0.16f)
                        )
                        .border(
                            1.dp,
                            if (errorCount > 0) JapanRed.copy(alpha = 0.75f)
                            else XboxNeonGreen.copy(alpha = 0.55f),
                            RoundedCornerShape(9.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = if (errorCount > 0) JapanRed else XboxNeonGreen,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isEn) "System Activity Log" else "Nhật ký hệ thống (App Log)",
                        color = TextPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (isEn) {
                            "${logs.size}/${AppLogger.MAX_LOGS} events • $errorCount errors, $warnCount warnings"
                        } else {
                            "${logs.size}/${AppLogger.MAX_LOGS} bản ghi • $errorCount lỗi, $warnCount cảnh báo"
                        },
                        color = when {
                            errorCount > 0 -> JapanRed
                            warnCount > 0 -> WarningAmber
                            else -> TextSecondary
                        },
                        fontWeight = if (errorCount > 0 || warnCount > 0) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 10.5.sp,
                        lineHeight = 13.5.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Filter chips
                val issueTotal = errorCount + warnCount
                val filters = listOf(
                    "ALL" to if (isEn) "All (${logs.size})" else "Tất cả (${logs.size})",
                    "ERRORS" to if (isEn) "Errors ($issueTotal)" else "Chỉ lỗi ($issueTotal)",
                    "UI" to if (isEn) "Actions [UI]" else "Thao tác [UI]",
                    "PROBE" to if (isEn) "Probe & Ping" else "Trạm đo & Ping",
                    "VPN" to if (isEn) "VPN & Unlock" else "VPN & Vùng",
                    "STREAM" to "Stream",
                    "PERF" to if (isEn) "Perf [PERF]" else "Hiệu năng [PERF]"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filters) { (key, label) ->
                        val isSelected = selectedFilter == key
                        val isErrorChip = key == "ERRORS"
                        val chipActiveColor = if (isErrorChip && errorCount > 0) JapanRed else XboxNeonGreen
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedFilter = key }
                                .testTag("app_log_filter_$key"),
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isSelected -> chipActiveColor
                                isErrorChip && errorCount > 0 -> Color(0xFF2B1115)
                                isErrorChip && warnCount > 0 -> Color(0xFF261E0D)
                                else -> XboxSurfaceVariant
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when {
                                    isSelected -> chipActiveColor
                                    isErrorChip && errorCount > 0 -> JapanRed.copy(alpha = 0.7f)
                                    isErrorChip && warnCount > 0 -> WarningAmber.copy(alpha = 0.65f)
                                    else -> XboxCardBorder
                                }
                            )
                        ) {
                            Text(
                                text = label,
                                color = when {
                                    isSelected -> if (isErrorChip && errorCount > 0) Color.White else Color.Black
                                    isErrorChip && errorCount > 0 -> Color(0xFFFF8A80)
                                    isErrorChip && warnCount > 0 -> WarningAmber
                                    else -> TextSecondary
                                },
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = if (isSelected || (isErrorChip && issueTotal > 0)) FontWeight.ExtraBold else FontWeight.SemiBold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Log list container
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 380.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF070D0A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, XboxCardBorder)
                ) {
                    if (filteredLogs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedFilter == "ERRORS") {
                                    if (isEn) "No errors or warnings recorded." else "Không có lỗi hoặc cảnh báo nào."
                                } else if (isEn) {
                                    "No log entries in this filter yet."
                                } else {
                                    "Chưa có dòng nhật ký nào trong mục lọc này."
                                },
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredLogs, key = { it.id }) { entry ->
                                val isError = entry.level == AppLogLevel.ERROR
                                val isWarn = entry.level == AppLogLevel.WARN
                                val badgeColor = when (entry.level) {
                                    AppLogLevel.SUCCESS -> XboxNeonGreen
                                    AppLogLevel.WARN -> WarningAmber
                                    AppLogLevel.ERROR -> JapanRed
                                    AppLogLevel.INFO -> DirectCyan
                                }
                                val rowBgColor = when (entry.level) {
                                    AppLogLevel.ERROR -> Color(0xFF2B1115)
                                    AppLogLevel.WARN -> Color(0xFF241C0C)
                                    else -> Color(0xFF0E1712)
                                }
                                val rowBorderColor = when (entry.level) {
                                    AppLogLevel.ERROR -> JapanRed.copy(alpha = 0.80f)
                                    AppLogLevel.WARN -> WarningAmber.copy(alpha = 0.65f)
                                    else -> Color.Transparent
                                }
                                val msgColor = when (entry.level) {
                                    AppLogLevel.ERROR -> Color(0xFFFFB4AB)
                                    AppLogLevel.WARN -> Color(0xFFFFE082)
                                    else -> TextPrimary
                                }
                                val levelIcon = when (entry.level) {
                                    AppLogLevel.ERROR -> "⛔ "
                                    AppLogLevel.WARN -> "⚠️ "
                                    AppLogLevel.SUCCESS -> "✓ "
                                    AppLogLevel.INFO -> ""
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(rowBgColor)
                                        .then(
                                            if (isError || isWarn) {
                                                Modifier.border(0.9.dp, rowBorderColor, RoundedCornerShape(8.dp))
                                            } else {
                                                Modifier
                                            }
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isError || isWarn) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 4.dp, height = 42.dp)
                                                .background(badgeColor)
                                        )
                                    }
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = entry.timestamp,
                                                color = if (isError) Color(0xFFFF8A80) else TextSecondary,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 9.5.sp
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = badgeColor.copy(alpha = if (isError) 0.28f else 0.18f),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    0.6.dp,
                                                    badgeColor.copy(alpha = 0.75f)
                                                )
                                            ) {
                                                Text(
                                                    text = "${entry.tag}•${entry.level.name}",
                                                    color = badgeColor,
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "$levelIcon${entry.message}",
                                            color = msgColor,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = if (isError) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Action row: Copy + Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val exportedText = if (selectedFilter == "PERF") {
                                com.example.data.PerformanceMonitor.buildShortPerfReport(isEn)
                            } else {
                                AppLogger.exportAsText() + "\n\n" + com.example.data.PerformanceMonitor.buildShortPerfReport(isEn)
                            }
                            clipboard?.setPrimaryClip(ClipData.newPlainText("CloudPlay Log", exportedText))
                            Toast.makeText(
                                context,
                                if (isEn) "Copied logs to clipboard" else "Đã sao chép nhật ký vào bộ nhớ tạm",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_app_log_btn"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DirectCyan.copy(alpha = 0.65f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = DirectCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.size(5.dp))
                        Text(
                            text = if (isEn) "Copy Log" else "Sao chép",
                            color = DirectCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { AppLogger.clear() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("clear_app_log_btn"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, JapanRed.copy(alpha = 0.65f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = JapanRed,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = if (isEn) "Clear" else "Xóa log",
                            color = JapanRed,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("close_app_log_btn")
            ) {
                Text(
                    text = if (isEn) "Close" else "Đóng",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    AppLogger.info("UI", "Bấm nút Đo ping lại (trong Nhật ký hệ thống)")
                    VpnManager.testAllServers(
                        autoPickBest = false,
                        context = context,
                        reason = "bấm nút Đo ping lại (trong Nhật ký hệ thống)"
                    )
                },
                modifier = Modifier.testTag("retest_ping_from_log_btn")
            ) {
                Text(
                    text = if (isEn) "Run Ping Test" else "Đo ping lại",
                    color = XboxNeonGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
