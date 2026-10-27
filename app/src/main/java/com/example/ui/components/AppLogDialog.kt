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

    val filteredLogs = remember(logs, selectedFilter) {
        val reversed = logs.asReversed()
        when (selectedFilter) {
            "PROBE" -> reversed.filter { it.tag == "PROBE" || it.tag == "PING" }
            "VPN" -> reversed.filter { it.tag == "VPN" || it.tag == "UNLOCK" }
            "STREAM" -> reversed.filter { it.tag == "STREAM" }
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
                        .background(XboxNeonGreen.copy(alpha = 0.16f))
                        .border(1.dp, XboxNeonGreen.copy(alpha = 0.55f), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = XboxNeonGreen,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Column {
                    Text(
                        text = if (isEn) "System Activity Log" else "Nhật ký hệ thống (App Log)",
                        color = TextPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (isEn) {
                            "${logs.size} events • Probe, Ping, VPN & Stream"
                        } else {
                            "${logs.size} bản ghi • Trạm đo, Ping, VPN & Stream"
                        },
                        color = TextSecondary,
                        fontSize = 11.sp
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
                val filters = listOf(
                    "ALL" to if (isEn) "All (${logs.size})" else "Tất cả (${logs.size})",
                    "PROBE" to if (isEn) "Probe & Ping" else "Trạm đo & Ping",
                    "VPN" to if (isEn) "VPN & Unlock" else "VPN & Vùng",
                    "STREAM" to "Stream"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filters) { (key, label) ->
                        val isSelected = selectedFilter == key
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedFilter = key }
                                .testTag("app_log_filter_$key"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) XboxNeonGreen else XboxSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) XboxNeonGreen else XboxCardBorder
                            )
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else TextSecondary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
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
                                text = if (isEn) {
                                    "No log entries yet. Run 'Ping Test' in Servers tab to see probe activity."
                                } else {
                                    "Chưa có dòng nhật ký nào. Hãy bấm 'Đo ping' ở mục Máy chủ để xem quá trình chọn trạm đo."
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
                                val badgeColor = when (entry.level) {
                                    AppLogLevel.SUCCESS -> XboxNeonGreen
                                    AppLogLevel.WARN -> WarningAmber
                                    AppLogLevel.ERROR -> JapanRed
                                    AppLogLevel.INFO -> DirectCyan
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0E1712))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = entry.timestamp,
                                            color = TextSecondary,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 9.5.sp
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = badgeColor.copy(alpha = 0.18f),
                                            border = androidx.compose.foundation.BorderStroke(
                                                0.6.dp,
                                                badgeColor.copy(alpha = 0.65f)
                                            )
                                        ) {
                                            Text(
                                                text = entry.tag,
                                                color = badgeColor,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = entry.message,
                                        color = TextPrimary,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.5.sp
                                    )
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
                            clipboard?.setPrimaryClip(ClipData.newPlainText("CloudPlay Log", AppLogger.exportAsText()))
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
                onClick = { VpnManager.testAllServers(autoPickBest = false, context = context) },
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
