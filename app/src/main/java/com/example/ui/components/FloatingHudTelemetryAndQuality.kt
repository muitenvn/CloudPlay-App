package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppConfig
import com.example.data.CellularOptimizationMode
import com.example.data.SettingsRepository
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.vpn.NetworkMode
import com.example.vpn.VpnConnectionState
import kotlin.math.roundToInt

/**
 * Tab "Mạng" trong Menu nổi mở rộng (chuẩn hud-mockup.html: .stats + .g2 + .note + accordion + VPN row).
 */
@Composable
internal fun HudNetworkTabContent(
    config: AppConfig,
    vpnState: VpnConnectionState,
    networkStatus: com.example.cloudplay.NetworkOptimizerStatus,
    telemetry: com.example.cloudplay.StreamTelemetry,
    adaptiveQualityState: com.example.cloudplay.AdaptiveQualityState,
    isThermalWarmOrHot: Boolean,
    onInteraction: () -> Unit,
    onToggleNetwork: () -> Unit
) {
    val context = LocalContext.current
    val hasSample = telemetry.hasStatsSample
    val effectivePing: Int? = if (hasSample && telemetry.rttMs > 0) telemetry.rttMs else null
    val pingStr = effectivePing?.toString() ?: "—"
    val hasValidJitter = hasSample && (telemetry.jitterMs > 0 || effectivePing != null)
    val jitterStr = if (hasValidJitter) "${telemetry.jitterMs}" else "—"
    val fpsStr = if (hasSample && telemetry.fps > 0) "${telemetry.fps}" else "—"
    val mbpsStr = if (hasSample && telemetry.bitrateMbps > 0) String.format("%.1f", telemetry.bitrateMbps) else "—"

    val pingColor = when {
        effectivePing == null -> HudInk
        effectivePing < 80 -> HudCyan
        effectivePing <= 120 -> HudWarn
        else -> HudDanger
    }
    val jitterColor = when {
        !hasValidJitter -> HudInk
        telemetry.jitterMs < 8 -> HudInk
        telemetry.jitterMs <= 20 -> HudWarn
        else -> HudDanger
    }

    val pingTier = when {
        effectivePing == null -> config.tr("Chưa đo", "Unmeasured") to HudTagTier.NEUTRAL
        effectivePing < 80 -> config.tr("Tốt", "Good") to HudTagTier.GOOD
        effectivePing <= 120 -> config.tr("Trung bình", "Moderate") to HudTagTier.MID
        else -> config.tr("Cao", "High") to HudTagTier.BAD
    }
    val routeModeLabel = if (vpnState.mode == NetworkMode.DIRECT_NETWORK) {
        "Direct"
    } else {
        "VPN ${vpnState.currentServer.flag}"
    }
    val latencyMain = if (effectivePing != null) "$routeModeLabel $effectivePing ms" else "$routeModeLabel —"

    val liveBufMs = if (hasSample && telemetry.jitterBufferDelayMs > 1.0) {
        telemetry.jitterBufferDelayMs.roundToInt().coerceIn(6, 60)
    } else {
        adaptiveQualityState.smoothBufferMs.roundToInt().coerceIn(8, 45)
    }
    val bufTier = when {
        !hasSample -> config.tr("Chờ đo", "Waiting") to HudTagTier.NEUTRAL
        liveBufMs <= 30 -> config.tr("Ổn định", "Stable") to HudTagTier.GOOD
        liveBufMs <= 48 -> config.tr("Trung bình", "Moderate") to HudTagTier.MID
        else -> config.tr("Đệm cao", "High") to HudTagTier.BAD
    }

    val decodeStr = if (hasSample && telemetry.decodeTimeMs > 0.0) {
        "${String.format("%.1f", telemetry.decodeTimeMs)} ms"
    } else {
        "— ms"
    }
    val decodeTier = when {
        !hasSample || telemetry.decodeTimeMs <= 0.0 -> config.tr("Chưa đo", "Unmeasured") to HudTagTier.NEUTRAL
        telemetry.decodeTimeMs < 12.0 -> config.tr("Tốt", "Good") to HudTagTier.GOOD
        telemetry.decodeTimeMs <= 18.0 -> config.tr("Trung bình", "Moderate") to HudTagTier.MID
        else -> config.tr("Quá tải", "Slow") to HudTagTier.BAD
    }

    val droppedCount = telemetry.framesDropped
    val freezeCount = telemetry.freezeCount
    val dropRatioPct = if (telemetry.framesDecoded > 0) {
        (droppedCount.toDouble() * 100.0) / (telemetry.framesDecoded + droppedCount).toDouble()
    } else 0.0
    val dropTier = when {
        telemetry.framesDecoded <= 0 && droppedCount == 0 -> config.tr("Tốt", "Good") to HudTagTier.GOOD
        dropRatioPct < 0.5 -> config.tr("Tốt", "Good") to HudTagTier.GOOD
        dropRatioPct <= 2.0 -> config.tr("Hơi giật", "Minor drops") to HudTagTier.MID
        else -> config.tr("Giật", "High drops") to HudTagTier.BAD
    }
    val dropMain = if (freezeCount > 0) {
        "$droppedCount (${config.tr("đứng", "freeze")} $freezeCount)"
    } else {
        "$droppedCount"
    }

    // ITU-T G.1072 summary note
    val pVal = effectivePing ?: 0
    val jVal = telemetry.jitterMs
    val dVal = telemetry.decodeTimeMs
    val isRed = pVal > 120 || jVal > 20 || dVal > 18.0
    val isYellow = !isRed && (pVal in 80..120 || jVal in 8..20 || dVal in 12.0..18.0)
    val assessmentText = when {
        pVal <= 0 && dVal <= 0.0 -> config.tr(
            "Đang đồng bộ thông số luồng WebRTC...",
            "Syncing WebRTC stream metrics..."
        )
        isRed -> when {
            vpnState.mode != NetworkMode.DIRECT_NETWORK && pVal > 120 -> config.tr(
                "Độ trễ cao (${pVal}ms). Hãy tắt VPN để đi thẳng giảm lag.",
                "High latency (${pVal}ms). Turn off VPN for direct routing."
            )
            dVal > 18.0 -> config.tr(
                "Giải mã chậm (${String.format("%.1f", dVal)}ms). Thử hạ về 720p hoặc để máy mát hơn.",
                "Slow decode (${String.format("%.1f", dVal)}ms). Try 720p or let phone cool."
            )
            jVal > 20 -> config.tr(
                "Mạng dao động mạnh (${jVal}ms). Lại gần Wi-Fi hoặc nơi sóng 4G mạnh hơn.",
                "High jitter (${jVal}ms). Move closer to Wi-Fi or stronger signal."
            )
            else -> config.tr(
                "Độ trễ cao (${pVal}ms). Kiểm tra tải ngầm hoặc chuyển mạng.",
                "High latency (${pVal}ms). Pause background downloads or switch network."
            )
        }
        isYellow -> config.tr(
            "Mạng hơi dao động (${pVal}ms | jitter ${jVal}ms). Giữ chất lượng Tự động.",
            "Moderate link (${pVal}ms | jitter ${jVal}ms). Keep Auto quality on."
        )
        else -> config.tr(
            "Đường truyền ổn định (${pVal}ms | giải mã ${String.format("%.1f", dVal)}ms).",
            "Stable stream (${pVal}ms | decode ${String.format("%.1f", dVal)}ms)."
        )
    }

    var showLagDiag by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 4 stat boxes (.stats)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                Triple(pingStr, "ms", pingColor),
                Triple(jitterStr, "jitter", jitterColor),
                Triple(fpsStr, "FPS", HudInk),
                Triple(mbpsStr, "Mbps", HudInk)
            ).forEach { (num, unit, color) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HudSurface)
                        .padding(vertical = 7.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = num,
                        color = color,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = JetBrainsMonoFontFamily,
                        maxLines = 1
                    )
                    Text(
                        text = unit,
                        color = HudMuted,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }

        // 2x2 detail cards (.g2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HudMiniMetricCard(
                title = config.tr("Độ trễ mạng", "Network latency"),
                value = latencyMain,
                tagText = pingTier.first,
                tagTier = pingTier.second,
                modifier = Modifier.weight(1f)
            )
            HudMiniMetricCard(
                title = config.tr("Bộ đệm", "Buffer"),
                value = "$liveBufMs ms",
                tagText = bufTier.first,
                tagTier = bufTier.second,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HudMiniMetricCard(
                title = config.tr("Giải mã", "Decode"),
                value = decodeStr,
                tagText = decodeTier.first,
                tagTier = decodeTier.second,
                modifier = Modifier.weight(1f)
            )
            HudMiniMetricCard(
                title = config.tr("Khung rớt", "Dropped frames"),
                value = dropMain,
                tagText = dropTier.first,
                tagTier = dropTier.second,
                modifier = Modifier.weight(1f)
            )
        }

        // Live assessment .note
        HudNoteCard(
            text = assessmentText,
            isWarning = isRed || isYellow
        )

        // Expandable 4-layer diagnostics accordion
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(HudSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 42.dp)
                    .clickable {
                        onInteraction()
                        showLagDiag = !showLagDiag
                    }
                    .testTag("hud_why_lag_button")
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = config.tr("Vì sao tôi bị lag?", "Why am I lagging?"),
                    color = HudInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = if (showLagDiag) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = HudFaint,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = showLagDiag) {
                val powerManager = remember(context) {
                    context.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
                }
                val isPowerSave = remember(showLagDiag, telemetry.rttMs) {
                    runCatching { powerManager?.isPowerSaveMode == true }.getOrDefault(false)
                }
                val currentHour = remember(showLagDiag, telemetry.rttMs) {
                    java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                }
                val isPeakHour = currentHour in 20..23

                val l1Bad = isPowerSave || (hasSample && telemetry.decodeTimeMs > 18.0) || isThermalWarmOrHot
                val l1Val = when {
                    isPowerSave -> config.tr("Tiết kiệm pin đang bật", "Battery Saver ON")
                    hasSample && telemetry.decodeTimeMs > 18.0 -> "${String.format("%.1f", telemetry.decodeTimeMs)} ms"
                    isThermalWarmOrHot -> config.tr("Máy đang ấm/nóng", "Device warm/hot")
                    else -> config.tr("Mát", "Cool")
                }

                val l2Bad = networkStatus.isWeakWifiSignal || networkStatus.isWeakCellularSignal || telemetry.jitterMs > 20
                val l2Val = when {
                    networkStatus.isWifi -> {
                        val band = if (networkStatus.physicalType == com.example.cloudplay.PhysicalNetworkType.WIFI_2_4GHZ) "2.4GHz" else "5GHz"
                        if (networkStatus.isWeakWifiSignal) config.tr("Wi-Fi $band yếu", "Weak Wi-Fi $band")
                        else config.tr("Wi-Fi $band tốt", "Wi-Fi $band good")
                    }
                    networkStatus.isCellular -> if (l2Bad) config.tr("4G dao động", "4G unstable") else config.tr("4G ổn định", "4G stable")
                    else -> config.tr("Hoạt động", "Active")
                }

                val l3Bad = telemetry.rttMs > 150
                val l3Val = when {
                    telemetry.rttMs > 160 -> config.tr("Nghẽn (${telemetry.rttMs}ms)${if (isPeakHour) " · 20h-23h" else ""}", "Congested (${telemetry.rttMs}ms)")
                    telemetry.rttMs > 0 -> config.tr("Ổn định (${telemetry.rttMs}ms)", "Stable (${telemetry.rttMs}ms)")
                    else -> config.tr("Đang đo", "Measuring")
                }

                val isVpnActive = vpnState.mode != NetworkMode.DIRECT_NETWORK
                val l4Val = if (isVpnActive) {
                    config.tr("Đang bật (gây thêm trễ)", "ON (adds latency)")
                } else {
                    config.tr("Tắt (trực tiếp)", "Off (direct)")
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hud_why_lag_panel")
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    HudDiagLayerRow(config.tr("1. Thiết bị", "1. Device"), l1Val, l1Bad)
                    HudDiagLayerRow(config.tr("2. Mạng cục bộ", "2. Local net"), l2Val, l2Bad)
                    HudDiagLayerRow(config.tr("3. Tuyến quốc tế", "3. Intl route"), l3Val, l3Bad)
                    HudDiagLayerRow(config.tr("4. VPN", "4. VPN"), l4Val, isVpnActive)
                }
            }
        }

        // VPN quick switch row
        val isVpnOn = vpnState.mode != NetworkMode.DIRECT_NETWORK
        HudSurfaceRow(minHeight = 42.dp) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "VPN",
                    color = HudInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = if (isVpnOn) {
                        "${vpnState.currentServer.flag} ${vpnState.currentServer.name}"
                    } else {
                        config.tr("Đang tắt (Direct)", "Off (Direct)")
                    },
                    color = if (isVpnOn) HudCyan else HudMuted,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            HudButton(
                text = if (isVpnOn) {
                    config.tr("Tắt VPN", "Turn Off")
                } else {
                    config.tr("Đổi VPN", "Turn On VPN")
                },
                onClick = {
                    onInteraction()
                    onToggleNetwork()
                },
                style = if (isVpnOn) HudButtonStyle.DANGER else HudButtonStyle.DEFAULT,
                minHeight = 32.dp,
                fontSize = 12.sp,
                modifier = Modifier.testTag("hud_toggle_network_button")
            )
        }
    }
}

@Composable
private fun HudMiniMetricCard(
    title: String,
    value: String,
    tagText: String,
    tagTier: HudTagTier,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(HudSurface)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = title,
            color = HudMuted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = HudInk,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = JetBrainsMonoFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        HudTagChip(text = tagText, tier = tagTier)
    }
}

@Composable
private fun HudDiagLayerRow(label: String, value: String, isWarnOrBad: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = HudMuted, fontSize = 12.sp, maxLines = 1)
        Text(
            text = value,
            color = if (isWarnOrBad) HudWarn else HudAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/**
 * Tab "Hình ảnh" trong Menu nổi mở rộng (chuẩn hud-mockup.html, KHÔNG có nút giả "Đồng bộ lại khung hình").
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HudImageTabContent(
    config: AppConfig,
    clarityBoostActive: Boolean,
    hideStreamStats: Boolean,
    networkStatus: com.example.cloudplay.NetworkOptimizerStatus,
    adaptiveQualityState: com.example.cloudplay.AdaptiveQualityState,
    isThermalWarmOrHot: Boolean,
    onInteraction: () -> Unit,
    onSelectCellularMode: (CellularOptimizationMode) -> Unit,
    onToggleClarityBoost: (Boolean) -> Unit,
    onToggleHideStreamStats: (Boolean) -> Unit,
    onSelectLadderStep: (Int) -> Unit,
    onToggleAutoAdapting: (Boolean) -> Unit
) {
    val liveMbps = adaptiveQualityState.smoothBitrateMbps.coerceIn(1.0, 20.0)
    val liveBufMs = adaptiveQualityState.smoothBufferMs.roundToInt().coerceIn(8, 45)
    val currentStep = adaptiveQualityState.currentStepIndex
    val stepMbps = adaptiveQualityState.currentStep.targetBitrateMbps.roundToInt()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HudSectionLabel(config.tr("Chất lượng truyền tải", "Stream quality"))

        // 5 options from CellularOptimizationMode (Auto, 720p, 900p, 1080p, 1080p+)
        val modeOptions = listOf(
            CellularOptimizationMode.AUTO to config.tr("Tự động", "Auto"),
            CellularOptimizationMode.DATA_SAVER_HIGH_SPEED to "720p",
            CellularOptimizationMode.MEDIUM to "900p",
            CellularOptimizationMode.HIGH to "1080p",
            CellularOptimizationMode.MAX_SPEED_UNCAPPED to "1080p+"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(HudSurface)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            modeOptions.forEach { (mode, shortLabel) ->
                val selected = config.cellularMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selected) HudSelBg else Color.Transparent)
                        .clickable {
                            onInteraction()
                            onSelectCellularMode(mode)
                        }
                        .padding(horizontal = 2.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = shortLabel,
                        color = if (selected) HudAccent else HudMuted,
                        fontSize = 11.5.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }

        HudSwitchRow(
            title = config.tr("Làm nét", "Clarity boost"),
            checked = clarityBoostActive,
            onCheckedChange = {
                onInteraction()
                onToggleClarityBoost(it)
            }
        )

        HudSwitchRow(
            title = config.tr("Giảm nền", "Reduce background GPU"),
            checked = config.gpuLoadReductionEnabled,
            onCheckedChange = {
                onInteraction()
                SettingsRepository.updateGpuLoadReductionEnabled(it)
            }
        )

        HudSwitchRow(
            title = config.tr("Chế độ máy yếu", "Low-end device mode"),
            subtitle = config.tr("Nhẹ 5 Mbps, giữ máy mát", "Light 5 Mbps stream, runs cool"),
            checked = config.lowEndDeviceMode,
            onCheckedChange = {
                onInteraction()
                SettingsRepository.updateLowEndDeviceMode(it)
            }
        )

        HudSwitchRow(
            title = config.tr("Ẩn thống kê Xbox", "Hide Xbox stats bar"),
            checked = hideStreamStats,
            onCheckedChange = {
                onInteraction()
                onToggleHideStreamStats(it)
            }
        )

        // Trần SDP & Bộ đệm
        HudSurfaceRow(minHeight = 44.dp) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
            ) {
                Text(
                    text = config.tr("Trần SDP & đệm", "SDP cap & buffer"),
                    color = HudInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = config.tr("Phiên sau · Đệm ${liveBufMs}ms", "Next session · Buf ${liveBufMs}ms"),
                    color = HudFaint,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (adaptiveQualityState.isAutoAdapting) HudSelBg else HudSurface2)
                        .clickable {
                            onInteraction()
                            onToggleAutoAdapting(!adaptiveQualityState.isAutoAdapting)
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = config.tr("Tự động", "Auto"),
                        color = if (adaptiveQualityState.isAutoAdapting) HudAccent else HudMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                val maxIdx = (adaptiveQualityState.ladderSteps.size - 1).coerceAtLeast(0)
                HudStepperControl(
                    valueText = "${stepMbps}M",
                    onDecrement = {
                        onInteraction()
                        if (currentStep > 0) onSelectLadderStep(currentStep - 1)
                    },
                    onIncrement = {
                        onInteraction()
                        if (currentStep < maxIdx) onSelectLadderStep(currentStep + 1)
                    }
                )
            }
        }

        if (networkStatus.isWeakWifiSignal || networkStatus.isWeakCellularSignal) {
            HudNoteCard(
                text = config.tr(
                    "Sóng yếu: tự chỉnh đệm ${liveBufMs}ms, trần SDP ${String.format("%.1f", liveMbps)}M (phiên sau).",
                    "Weak signal: auto buffer ${liveBufMs}ms, SDP cap ${String.format("%.1f", liveMbps)}M (next session)."
                ),
                isWarning = true
            )
        }

        HudNoteCard(
            text = if (isThermalWarmOrHot) {
                config.tr(
                    "Máy đang nóng, nên hạ chất lượng truyền tải hoặc tắt Làm nét để mát máy.",
                    "Device is warm, consider lowering stream quality or turning off Clarity Boost."
                )
            } else {
                config.tr(
                    "Thay đổi chất lượng áp dụng ở phiên sau. Máy đang nóng thì nên hạ chất lượng.",
                    "Quality changes apply on next session. Lower quality if device runs hot."
                )
            },
            isWarning = isThermalWarmOrHot
        )
    }
}
