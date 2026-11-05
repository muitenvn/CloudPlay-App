package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JapanRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxNeonGreen
import com.example.vpn.NetworkMode
import com.example.vpn.VpnConnectionState
import kotlin.math.roundToInt

@Composable
internal fun FortniteExclusiveHudCard(
    isEn: Boolean,
    virtualControllerEnabled: Boolean,
    onSelectFortniteControlMode: (Boolean) -> Unit
) {
    val fortniteBlue = Color(0xFF0095FF)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hud_fortnite_arena_card"),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0A1929).copy(alpha = 0.94f),
        border = androidx.compose.foundation.BorderStroke(1.dp, fortniteBlue.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEn) "Fortnite Controls" else "Điều khiển Fortnite",
                    color = fortniteBlue,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Text(
                    text = if (isEn) "1-Tap Switch" else "Đổi 1 chạm",
                    color = XboxNeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val isEpicTouchActive = !virtualControllerEnabled
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isEpicTouchActive) fortniteBlue.copy(alpha = 0.28f) else Color(0x26FFFFFF),
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        if (isEpicTouchActive) fortniteBlue else Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelectFortniteControlMode(false) }
                        .testTag("hud_fortnite_epic_touch_btn")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEn) "👆 Epic Touch" else "👆 Cảm ứng Epic",
                            color = if (isEpicTouchActive) Color.White else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (virtualControllerEnabled) XboxNeonGreen.copy(alpha = 0.25f) else Color(0x26FFFFFF),
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        if (virtualControllerEnabled) XboxNeonGreen else Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelectFortniteControlMode(true) }
                        .testTag("hud_fortnite_fps_pad_btn")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEn) "🎮 Virtual Pad" else "🎮 Tay cầm ảo",
                            color = if (virtualControllerEnabled) XboxNeonGreen else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * B.7: Thanh tiêu đề bảng mở rộng kèm nút Thu gọn lớn (chuẩn 48dp) rõ ràng, dễ bấm,
 * tự căn theo phía trái hoặc phải nơi HUD đang đứng.
 */
@Composable
internal fun ExpandedHudCollapseHeader(
    isAnchoredRight: Boolean,
    gameProfile: com.example.data.GameOptimizationProfile?,
    onCollapseToCompact: () -> Unit,
    onCollapseToMinimized: () -> Unit
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(9.dp),
        color = Color(0xFF101D16),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxNeonGreen.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (!isAnchoredRight) {
                CollapseTouchButton48dp(
                    isEn = isEn,
                    onCollapse = onCollapseToCompact,
                    onMinimize = onCollapseToMinimized
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = if (isAnchoredRight) Alignment.Start else Alignment.End
            ) {
                Text(
                    text = gameProfile?.title ?: if (isEn) "Quick Controls" else "Điều khiển nhanh",
                    color = TextPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = if (gameProfile != null) {
                        gameProfile.recommendedFps
                    } else {
                        if (isEn) "Tap Collapse to close" else "Chạm Thu gọn để đóng"
                    },
                    color = XboxNeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            if (isAnchoredRight) {
                Spacer(modifier = Modifier.width(6.dp))
                CollapseTouchButton48dp(
                    isEn = isEn,
                    onCollapse = onCollapseToCompact,
                    onMinimize = onCollapseToMinimized
                )
            }
        }
    }
}

/**
 * Nút thu gọn rõ ràng, chiều cao & vùng chạm chuẩn 48dp để người chơi dễ bấm ngay khi đang chơi game.
 */
@Composable
internal fun CollapseTouchButton48dp(
    isEn: Boolean = false,
    onCollapse: () -> Unit,
    onMinimize: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            modifier = Modifier
                .height(48.dp)
                .width(92.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onCollapse() }
                .testTag("hud_collapse_48dp_button"),
            shape = RoundedCornerShape(8.dp),
            color = XboxNeonGreen.copy(alpha = 0.22f),
            border = androidx.compose.foundation.BorderStroke(1.dp, XboxNeonGreen.copy(alpha = 0.7f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = if (isEn) "Collapse HUD" else "Thu gọn bảng HUD",
                    tint = XboxNeonGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (isEn) "Collapse" else "Thu gọn",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        Surface(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onMinimize() }
                .testTag("hud_minimize_48dp_button"),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF16261E),
            border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxCardBorder)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "─",
                    color = DirectCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

/**
 * B.6 & B.7: Bảng thông số cốt lõi trong bảng mở rộng (cỡ chữ >= 11sp).
 */
@Composable
internal fun DenseCoreTelemetrySection(
    vpnState: VpnConnectionState,
    telemetry: com.example.cloudplay.StreamTelemetry,
    compactRawPing: Int?,
    compactHasValidPing: Boolean,
    physicalControllerConnected: Boolean,
    virtualControllerEnabled: Boolean,
    controllerName: String,
    batteryPercent: Int,
    isBatteryCharging: Boolean,
    isThermalSupported: Boolean,
    thermalIcon: String,
    thermalLabel: String,
    thermalColor: Color,
    onToggleNetwork: () -> Unit,
    onToggleVirtualController: () -> Unit,
    adaptiveBufferMs: Int = 12
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()

    // Trong khi chơi chỉ hiển thị RTT từ WebRTC candidate-pair; nếu không có thì hiện "—", không rơi về ping TCP/VPN
    val hasRealSample = telemetry.hasStatsSample
    val effectivePing: Int? = if (hasRealSample && telemetry.rttMs > 0) telemetry.rttMs else null
    val pingTierLabel = when {
        effectivePing == null || effectivePing <= 0 -> if (isEn) "Unmeasured" else "Chưa đo"
        effectivePing < 80 -> if (isEn) "Good" else "Tốt"
        effectivePing <= 120 -> if (isEn) "Moderate" else "Trung bình"
        else -> if (isEn) "High" else "Cao"
    }
    val latencyDisplayText = if (vpnState.mode == NetworkMode.DIRECT_NETWORK) {
        if (effectivePing != null) "Direct • ${effectivePing}ms ($pingTierLabel)" else "Direct • — ($pingTierLabel)"
    } else {
        if (effectivePing != null) "VPN ${vpnState.currentServer.flag} • ${effectivePing}ms ($pingTierLabel)" else "VPN ${vpnState.currentServer.flag} • — ($pingTierLabel)"
    }

    val jitterVal = telemetry.jitterMs
    val hasValidJitter = hasRealSample && (jitterVal > 0 || effectivePing != null)
    val jitterTierLabel = when {
        !hasValidJitter -> if (isEn) "Unmeasured" else "Chưa đo"
        jitterVal < 8 -> if (isEn) "Good" else "Tốt"
        jitterVal <= 20 -> if (isEn) "Moderate" else "Trung bình"
        else -> if (isEn) "High" else "Cao"
    }
    val jitterColor = when {
        !hasValidJitter -> TextSecondary
        jitterVal < 8 -> XboxNeonGreen
        jitterVal <= 20 -> Color(0xFFFFB300)
        else -> JapanRed
    }
    val jitterDisplayText = if (hasValidJitter) "${jitterVal}ms ($jitterTierLabel)" else "— ($jitterTierLabel)"

    val fpsValueText = if (hasRealSample && telemetry.fps > 0) "${telemetry.fps} FPS" else "— FPS"
    val bitrateValueText = if (hasRealSample && telemetry.bitrateMbps > 0) {
        String.format("%.1f Mbps", telemetry.bitrateMbps)
    } else {
        "— Mbps"
    }

    val liveBufMs = if (hasRealSample && telemetry.jitterBufferDelayMs > 1.0) {
        telemetry.jitterBufferDelayMs.roundToInt().coerceIn(6, 60)
    } else {
        adaptiveBufferMs.coerceIn(8, 45)
    }
    val bufferLabel = if (isEn) "Data buffer" else "Bộ đệm dữ liệu"
    val decodeTimeText = if (hasRealSample && telemetry.decodeTimeMs > 0.0) {
        String.format("%.1fms • %s %dms", telemetry.decodeTimeMs, bufferLabel, liveBufMs)
    } else {
        "— • $bufferLabel ${liveBufMs}ms"
    }
    // Chuẩn 60Hz: < 12ms Xanh, 12-18ms Vàng, > 18ms Đỏ
    val decodeColor = when {
        !hasRealSample || telemetry.decodeTimeMs <= 0.0 -> TextSecondary
        telemetry.decodeTimeMs < 12.0 -> XboxNeonGreen
        telemetry.decodeTimeMs <= 18.0 -> Color(0xFFFFB300)
        else -> JapanRed
    }

    val droppedCount = telemetry.framesDropped
    val freezeCount = telemetry.freezeCount
    val dropRatioPct = if (telemetry.framesDecoded > 0) {
        (droppedCount.toDouble() * 100.0) / (telemetry.framesDecoded + droppedCount).toDouble()
    } else 0.0
    val dropTierLabel = when {
        telemetry.framesDecoded <= 0 && droppedCount == 0 -> if (isEn) "Good" else "Tốt"
        dropRatioPct < 0.5 -> if (isEn) "Good" else "Tốt"
        dropRatioPct <= 2.0 -> if (isEn) "Moderate" else "Trung bình"
        else -> if (isEn) "High" else "Cao"
    }
    val dropColor = when {
        dropRatioPct < 0.5 -> XboxNeonGreen
        dropRatioPct <= 2.0 -> Color(0xFFFFB300)
        else -> JapanRed
    }
    val droppedAndFreezeText = if (isEn) {
        "$droppedCount ($dropTierLabel) • Temporary freeze: $freezeCount"
    } else {
        "$droppedCount ($dropTierLabel) • Đứng hình tạm thời: $freezeCount"
    }

    val pingMetricColor = when {
        effectivePing == null || effectivePing <= 0 -> TextSecondary
        effectivePing < 80 -> XboxNeonGreen
        effectivePing <= 120 -> Color(0xFFFFB300)
        else -> JapanRed
    }

    val controllerStatusText = when {
        physicalControllerConnected -> controllerName
        virtualControllerEnabled -> stringResource(R.string.hud_virtual_gamepad)
        else -> stringResource(R.string.controller_disconnected)
    }

    val batteryStatusText = if (batteryPercent >= 0) {
        val chargeStr = if (isBatteryCharging) {
            if (isEn) "Charging ⚡" else "Đang sạc ⚡"
        } else {
            if (isEn) "Battery" else "Pin"
        }
        "$batteryPercent% • $chargeStr"
    } else {
        if (isEn) "--%" else "--%"
    }
    val batteryColor = when {
        isBatteryCharging -> XboxNeonGreen
        batteryPercent in 0..20 -> JapanRed
        batteryPercent in 21..35 -> Color(0xFFFFB300)
        else -> TextPrimary
    }

    // Phân loại tự động 3 trạng thái theo chuẩn ITU-T G.1072: [1 câu nguyên nhân] + [1 câu gợi ý]
    val pVal = effectivePing ?: 0
    val jVal = telemetry.jitterMs
    val dVal = telemetry.decodeTimeMs
    val isRedState = pVal > 120 || jVal > 20 || dVal > 18.0
    val isYellowState = !isRedState && (pVal in 80..120 || jVal in 8..20 || dVal in 12.0..18.0)
    val statusAssessmentColor = when {
        pVal <= 0 && dVal <= 0.0 -> TextSecondary
        isRedState -> JapanRed
        isYellowState -> Color(0xFFFFB300)
        else -> XboxNeonGreen
    }
    val statusAssessmentText = when {
        pVal <= 0 && dVal <= 0.0 -> if (isEn) {
            "Waiting for WebRTC stream metrics..."
        } else {
            "Đang đồng bộ thông số luồng WebRTC..."
        }
        isRedState -> when {
            vpnState.mode != NetworkMode.DIRECT_NETWORK && pVal > 120 -> if (isEn) {
                "High network latency (${pVal}ms). Tip: Turn off VPN now to route directly."
            } else {
                "Độ trễ mạng tăng cao (${pVal}ms). Gợi ý: Tắt VPN ngay sau khi vào game để giảm trễ."
            }
            dVal > 18.0 -> if (isEn) {
                "Slow hardware decode (${String.format("%.1f", dVal)}ms > 18.0ms). Tip: Switch to 720p or cool down device."
            } else {
                "Thời gian giải mã cao (${String.format("%.1f", dVal)}ms > 18.0ms). Gợi ý: Chuyển về 720p hoặc để máy hạ nhiệt."
            }
            jVal > 20 -> if (isEn) {
                "High network jitter (${jVal}ms). Tip: Move closer to Wi-Fi or stronger 4G area."
            } else {
                "Độ biến động mạng cao (${jVal}ms). Gợi ý: Di chuyển tới nơi sóng 4G mạnh hơn hoặc dùng Wi-Fi 5GHz."
            }
            else -> if (isEn) {
                "High network latency (${pVal}ms). Tip: Pause background downloads or check VPN."
            } else {
                "Độ trễ mạng tăng cao (${pVal}ms). Gợi ý: Kiểm tra xem VPN đã tắt chưa hoặc tạm dừng tải ngầm."
            }
        }
        isYellowState -> if (isEn) {
            "Moderate connection (Network latency: ${pVal}ms | Network jitter: ${jVal}ms). Tip: Keep Auto quality enabled."
        } else {
            "Đường truyền biến động nhẹ (Độ trễ mạng: ${pVal}ms | Độ biến động mạng: ${jVal}ms). Gợi ý: Giữ chế độ Tự động."
        }
        else -> if (isEn) {
            "Stable stream (Network latency: ${pVal}ms | Decode time: ${String.format("%.1f", dVal)}ms)."
        } else {
            "Ổn định (Độ trễ mạng: ${pVal}ms | Thời gian giải mã: ${String.format("%.1f", dVal)}ms)."
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF121F18),
        border = androidx.compose.foundation.BorderStroke(0.7.dp, XboxCardBorder.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 1. Dòng Độ trễ mạng (chạm để chuyển nhanh Direct / VPN)
            DenseMetricRowItem(
                label = if (isEn) "Network latency" else "Độ trễ mạng",
                value = latencyDisplayText,
                valueColor = pingMetricColor,
                badgeText = if (vpnState.mode == NetworkMode.DIRECT_NETWORK) {
                    if (isEn) "VPN" else "Đổi VPN"
                } else {
                    if (isEn) "Off" else "Tắt VPN"
                },
                onRowClick = onToggleNetwork
            )

            // 2. Dòng Độ biến động mạng (C1 & C3)
            DenseMetricRowItem(
                label = if (isEn) "Network jitter" else "Độ biến động mạng",
                value = jitterDisplayText,
                valueColor = jitterColor
            )

            // 3. Dòng Chất lượng truyền tải & FPS (C1)
            DenseMetricRowItem(
                label = if (isEn) "Stream bitrate" else "Chất lượng truyền tải",
                value = "$bitrateValueText ($fpsValueText)",
                valueColor = if (telemetry.fps >= 55) XboxNeonGreen else TextPrimary
            )

            // 4. Dòng Thời gian giải mã & Bộ đệm dữ liệu (C1)
            DenseMetricRowItem(
                label = if (isEn) "Decode time" else "Thời gian giải mã",
                value = decodeTimeText,
                valueColor = decodeColor
            )

            // 5. Dòng Khung hình bị rớt & Đứng hình tạm thời (C1 & C3)
            DenseMetricRowItem(
                label = if (isEn) "Dropped frames" else "Khung hình bị rớt",
                value = droppedAndFreezeText,
                valueColor = dropColor
            )

            // 6. Dòng Tay cầm (chạm để bật/tắt tay cầm ảo)
            DenseMetricRowItem(
                label = stringResource(R.string.hud_controller),
                value = controllerStatusText,
                valueColor = if (physicalControllerConnected || virtualControllerEnabled) XboxNeonGreen else TextSecondary,
                badgeText = if (virtualControllerEnabled) {
                    if (isEn) "Hide" else "Ẩn ảo"
                } else {
                    if (isEn) "Show" else "Bật ảo"
                },
                onRowClick = onToggleVirtualController
            )

            // 7. Dòng % Pin & trạng thái sạc (+ Nhiệt độ máy trên API 29+)
            DenseMetricRowItem(
                label = if (isEn) "Battery" else "Pin & Máy",
                value = batteryStatusText,
                valueColor = batteryColor,
                badgeText = if (isThermalSupported) "$thermalIcon $thermalLabel" else null
            )

            // 8. Hộp đánh giá tự động theo chuẩn ITU-T G.1072 ([Nguyên nhân] + [Gợi ý])
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = statusAssessmentColor.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(0.6.dp, statusAssessmentColor.copy(alpha = 0.45f))
            ) {
                Text(
                    text = statusAssessmentText,
                    color = Color.White,
                    fontSize = 10.5.sp,
                    lineHeight = 13.5.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
internal fun DenseMetricRowItem(
    label: String,
    value: String,
    valueColor: Color,
    badgeText: String? = null,
    onRowClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(5.dp))
            .then(
                if (onRowClick != null) {
                    Modifier.clickable { onRowClick() }
                } else {
                    Modifier
                }
            )
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                color = valueColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF1D3126)
                ) {
                    Text(
                        text = badgeText,
                        color = DirectCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Nhóm 1: Trần SDP (phiên sau), Đệm nhận tự động & Chẩn đoán 4 lớp "Vì sao tôi bị lag?" (cỡ chữ >= 11sp).
 */
@Composable
internal fun DenseQualityModeSection(
    networkStatus: com.example.cloudplay.NetworkOptimizerStatus,
    adaptiveQualityState: com.example.cloudplay.AdaptiveQualityState,
    telemetry: com.example.cloudplay.StreamTelemetry,
    isVpnActive: Boolean = false,
    onSelectCellularMode: (com.example.data.CellularOptimizationMode) -> Unit,
    onToggleAutoAdapting: (Boolean) -> Unit,
    onToggleWifiSpeedFirst: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    val liveMbps = adaptiveQualityState.smoothBitrateMbps.coerceIn(1.0, 20.0)
    val liveBufMs = adaptiveQualityState.smoothBufferMs.roundToInt().coerceIn(8, 45)
    val selectedMode = config.cellularMode
    var showLagDiagnostic by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF101C16),
        border = androidx.compose.foundation.BorderStroke(0.7.dp, XboxNeonGreen.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEn) "SDP Cap (next) & Buffer" else "Trần SDP (phiên sau) & Đệm",
                    color = XboxNeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                val wifiBandText = when (networkStatus.physicalType) {
                    com.example.cloudplay.PhysicalNetworkType.WIFI_6GHZ -> "Wi-Fi 6GHz"
                    com.example.cloudplay.PhysicalNetworkType.WIFI_5GHZ -> "Wi-Fi 5GHz"
                    com.example.cloudplay.PhysicalNetworkType.WIFI_2_4GHZ -> "Wi-Fi 2.4GHz"
                    else -> "Wi-Fi"
                }
                val signalInfo = when {
                    networkStatus.isWifi && networkStatus.wifiRssiDbm < 0 ->
                        "$wifiBandText • Đệm ${liveBufMs}ms • SDP ${String.format("%.1f", liveMbps)}M"
                    networkStatus.isCellular ->
                        "📶 4G • Đệm ${liveBufMs}ms • SDP ${String.format("%.1f", liveMbps)}M"
                    else ->
                        "Đệm ${liveBufMs}ms • SDP ${String.format("%.1f", liveMbps)}M"
                }
                val warnAmber = Color(0xFFF59E0B)
                Text(
                    text = signalInfo,
                    color = if (networkStatus.isWeakWifiSignal || networkStatus.isWeakCellularSignal) warnAmber else DirectCyan,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            // Hàng 1: Tự động, 720p, 900p
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DenseQualityPresetChip(
                    label = if (isEn) "Auto" else "Tự động",
                    isSelected = selectedMode == com.example.data.CellularOptimizationMode.AUTO,
                    onClick = { onSelectCellularMode(com.example.data.CellularOptimizationMode.AUTO) },
                    modifier = Modifier.weight(1f)
                )
                DenseQualityPresetChip(
                    label = "720p",
                    isSelected = selectedMode == com.example.data.CellularOptimizationMode.DATA_SAVER_HIGH_SPEED,
                    onClick = { onSelectCellularMode(com.example.data.CellularOptimizationMode.DATA_SAVER_HIGH_SPEED) },
                    modifier = Modifier.weight(1f)
                )
                DenseQualityPresetChip(
                    label = "900p",
                    isSelected = selectedMode == com.example.data.CellularOptimizationMode.MEDIUM,
                    onClick = { onSelectCellularMode(com.example.data.CellularOptimizationMode.MEDIUM) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Hàng 2: 1080p & 1080p+
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DenseQualityPresetChip(
                    label = if (isEn) "1080p High" else "1080p Cao",
                    isSelected = selectedMode == com.example.data.CellularOptimizationMode.HIGH,
                    onClick = { onSelectCellularMode(com.example.data.CellularOptimizationMode.HIGH) },
                    modifier = Modifier.weight(1f)
                )
                DenseQualityPresetChip(
                    label = if (isEn) "1080p+ Max" else "1080p+ Tối đa",
                    isSelected = selectedMode == com.example.data.CellularOptimizationMode.MAX_SPEED_UNCAPPED,
                    onClick = { onSelectCellularMode(com.example.data.CellularOptimizationMode.MAX_SPEED_UNCAPPED) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Nút Chẩn đoán 4 lớp "🩺 Vì sao tôi bị lag?" (đã bỏ nút "Đồng bộ lại khung hình" theo mục 5)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { showLagDiagnostic = !showLagDiagnostic }
                    .testTag("hud_why_lag_button"),
                shape = RoundedCornerShape(6.dp),
                color = if (showLagDiagnostic) XboxNeonGreen.copy(alpha = 0.22f) else Color(0xFF182A20),
                border = androidx.compose.foundation.BorderStroke(
                    0.8.dp,
                    if (showLagDiagnostic) XboxNeonGreen else XboxCardBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEn) "🩺 Why am I lagging? (4-Layer Check)" else "🩺 Vì sao tôi bị lag? (Chẩn đoán 4 lớp)",
                        color = if (showLagDiagnostic) XboxNeonGreen else DirectCyan,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            // Bảng Chẩn đoán nhanh 4 lớp "Vì sao tôi bị lag?" (Mục 15 báo cáo kỹ thuật)
            if (showLagDiagnostic) {
                val powerManager = remember(context) {
                    context.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
                }
                val isPowerSave = remember(showLagDiagnostic, telemetry.rttMs) {
                    runCatching { powerManager?.isPowerSaveMode == true }.getOrDefault(false)
                }
                val currentHour = remember(showLagDiagnostic, telemetry.rttMs) {
                    java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                }
                val isPeakHour = currentHour in 20..23

                // Lớp 1: Môi trường thiết bị (Tiết kiệm pin / Giải mã phần cứng)
                val layer1Bad = isPowerSave || (telemetry.hasStatsSample && telemetry.decodeTimeMs > 18.0)
                val layer1Text = when {
                    isPowerSave -> if (isEn) "1. Device: Battery Saver ON (throttles decoder)" else "1. Thiết bị: Đang bật Tiết kiệm pin (bóp xung giải mã)"
                    !telemetry.hasStatsSample || telemetry.decodeTimeMs <= 0.0 -> if (isEn) "1. Device: 60Hz VSync locked • Decode —" else "1. Thiết bị: Đã khóa 60Hz • Giải mã —"
                    telemetry.decodeTimeMs > 18.0 -> if (isEn) "1. Device: Slow decode (${String.format("%.1f", telemetry.decodeTimeMs)}ms > 18.0ms)" else "1. Thiết bị: Giải mã quá tải (${String.format("%.1f", telemetry.decodeTimeMs)}ms > 18.0ms)"
                    else -> if (isEn) "1. Device: 60Hz VSync locked • Decode OK (${String.format("%.1f", telemetry.decodeTimeMs)}ms)" else "1. Thiết bị: Đã khóa 60Hz • Giải mã tốt (${String.format("%.1f", telemetry.decodeTimeMs)}ms)"
                }

                // Lớp 2: Kết nối cục bộ (Wi-Fi RSSI / 4G Jitter)
                val layer2Bad = networkStatus.isWeakWifiSignal || networkStatus.isWeakCellularSignal || telemetry.jitterMs > 20
                val layer2Text = when {
                    networkStatus.isWifi -> {
                        val band = if (networkStatus.physicalType == com.example.cloudplay.PhysicalNetworkType.WIFI_2_4GHZ) "2.4GHz" else "5GHz"
                        if (networkStatus.isWeakWifiSignal) {
                            if (isEn) "2. Local Net: Weak Wi-Fi $band (${networkStatus.wifiRssiDbm}dBm)" else "2. Mạng cục bộ: Sóng Wi-Fi $band yếu (${networkStatus.wifiRssiDbm}dBm)"
                        } else {
                            if (isEn) "2. Local Net: Wi-Fi $band Low-Latency OK (${networkStatus.wifiRssiDbm}dBm)" else "2. Mạng cục bộ: Wi-Fi $band ổn định (${networkStatus.wifiRssiDbm}dBm)"
                        }
                    }
                    networkStatus.isCellular -> {
                        if (telemetry.jitterMs > 18 || networkStatus.isWeakCellularSignal) {
                            if (isEn) "2. Local Net: 4G queue fluctuation (Jitter ${telemetry.jitterMs}ms)" else "2. Mạng cục bộ: Sóng 4G dao động (Biến động ${telemetry.jitterMs}ms)"
                        } else {
                            if (isEn) "2. Local Net: 4G Direct active (Jitter ${telemetry.jitterMs}ms)" else "2. Mạng cục bộ: 4G ổn định (Biến động ${telemetry.jitterMs}ms)"
                        }
                    }
                    else -> if (isEn) "2. Local Net: Active" else "2. Mạng cục bộ: Đang hoạt động"
                }

                // Lớp 3: Tuyến đường quốc tế tới máy chủ luồng
                val layer3Bad = telemetry.rttMs > 150
                val layer3Text = when {
                    telemetry.rttMs > 160 && !layer2Bad -> if (isEn) {
                        "3. Intl Route: Congested (${telemetry.rttMs}ms)${if (isPeakHour) " • Peak hours 20h-23h" else ""}"
                    } else {
                        "3. Tuyến quốc tế: Nghẽn (${telemetry.rttMs}ms)${if (isPeakHour) " • Giờ cao điểm 20h-23h" else ""}"
                    }
                    telemetry.rttMs > 0 -> if (isEn) {
                        "3. Intl Route: Normal (${telemetry.rttMs}ms)"
                    } else {
                        "3. Tuyến quốc tế: Bình thường (${telemetry.rttMs}ms)"
                    }
                    else -> if (isEn) "3. Intl Route: Measuring WebRTC RTT..." else "3. Tuyến quốc tế: Đang đo độ trễ WebRTC..."
                }

                // Lớp 4: Trạng thái giao diện VPN (tun0 / TRANSPORT_VPN)
                val layer4Bad = isVpnActive
                val layer4Text = if (layer4Bad) {
                    if (isEn) "4. VPN: Active (Adds detour routing latency — turn off after game loads!)" else "4. VPN: Đang bật (Tăng độ trễ đường vòng — hãy tắt khi đã vào game!)"
                } else {
                    if (isEn) "4. VPN: Off (Direct routing)" else "4. VPN: Đã tắt (Truyền trực tiếp không đi vòng)"
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hud_why_lag_panel"),
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0B1410),
                    border = androidx.compose.foundation.BorderStroke(0.7.dp, DirectCyan.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = if (isEn) "4-LAYER LATENCY DIAGNOSTICS" else "CHẨN ĐOÁN 4 LỚP: VÌ SAO TÔI BỊ LAG?",
                            color = DirectCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = layer1Text,
                            color = if (layer1Bad) JapanRed else XboxNeonGreen,
                            fontSize = 10.5.sp,
                            lineHeight = 13.sp
                        )
                        Text(
                            text = layer2Text,
                            color = if (layer2Bad) Color(0xFFFFB300) else XboxNeonGreen,
                            fontSize = 10.5.sp,
                            lineHeight = 13.sp
                        )
                        Text(
                            text = layer3Text,
                            color = if (layer3Bad) Color(0xFFFFB300) else XboxNeonGreen,
                            fontSize = 10.5.sp,
                            lineHeight = 13.sp
                        )
                        Text(
                            text = layer4Text,
                            color = if (layer4Bad) JapanRed else XboxNeonGreen,
                            fontSize = 10.5.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
            }

            if (networkStatus.isWeakWifiSignal || networkStatus.isWeakCellularSignal) {
                Text(
                    text = if (networkStatus.isWeakWifiSignal) {
                        if (isEn) "⚠️ Weak Wi-Fi: Adjusting receiver buffer (${liveBufMs}ms); SDP cap (${String.format("%.1f", liveMbps)}M) applies next session"
                        else "⚠️ Sóng Wi-Fi yếu: Tự chỉnh bộ đệm nhận (${liveBufMs}ms); trần SDP (${String.format("%.1f", liveMbps)}M) áp dụng phiên sau"
                    } else {
                        if (isEn) "⚠️ Weak 4G: Adjusting receiver buffer (${liveBufMs}ms); SDP cap (${String.format("%.1f", liveMbps)}M) applies next session"
                        else "⚠️ Sóng 4G yếu: Tự chỉnh bộ đệm nhận (${liveBufMs}ms); trần SDP (${String.format("%.1f", liveMbps)}M) áp dụng phiên sau"
                    },
                    color = Color(0xFFF59E0B),
                    fontSize = 10.5.sp,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
internal fun DenseQualityPresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = XboxNeonGreen
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.22f) else Color(0xFF15251D),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(0.9.dp, accentColor) else null
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (isSelected) accentColor else TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
