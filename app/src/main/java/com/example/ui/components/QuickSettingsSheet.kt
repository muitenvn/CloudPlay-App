package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AppConfig
import com.example.data.CellularOptimizationMode
import com.example.data.ScreenRecordQuality
import com.example.data.SettingsRepository
import com.example.data.StreamFpsCap
import com.example.data.XboxWebUiMode
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
internal fun QuickSettingsSheetDialog(
    config: AppConfig,
    onToggleVirtualController: (Boolean) -> Unit,
    onSelectXboxWebUiMode: (XboxWebUiMode) -> Unit,
    onToggleRecording: () -> Unit,
    onDismiss: () -> Unit
) {
    val recorderState by com.example.cloudplay.ZeroLagScreenRecorder.state.collectAsState()
    val isEn = config.isEnglishUI()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            val useMultiColumn = maxWidth >= 600.dp
            val sheetMaxHeight = (maxHeight - 8.dp).coerceAtLeast(260.dp)

            Surface(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxWidth()
                    .heightIn(max = sheetMaxHeight)
                    .testTag("in_game_quick_settings_dialog"),
                shape = RoundedCornerShape(22.dp),
                color = HudSurface2,
                shadowElevation = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header (.s-head)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = config.tr("Cài đặt nhanh", "Quick settings"),
                                color = HudInk,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = config.tr(
                                    "Độ phân giải, khung hình, tay cầm và màu sắc",
                                    "Resolution, frame rate, gamepad & image colors"
                                ),
                                color = HudFaint,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        HudPanelIconButton(
                            icon = Icons.Default.Close,
                            contentDescription = config.tr("Đóng", "Close"),
                            onClick = onDismiss,
                            size = 34.dp,
                            bgColor = HudSurface,
                            tint = HudMuted
                        )
                    }

                    // Body (.s-body)
                    if (useMultiColumn) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                QuickSettingsColControls(
                                    config = config,
                                    isEn = isEn,
                                    recorderState = recorderState,
                                    onToggleVirtualController = onToggleVirtualController,
                                    onSelectXboxWebUiMode = onSelectXboxWebUiMode,
                                    onToggleRecording = onToggleRecording
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1.05f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                QuickSettingsColQuality(config = config, isEn = isEn)
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                QuickSettingsColDisplay(config = config)
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickSettingsColControls(
                                config = config,
                                isEn = isEn,
                                recorderState = recorderState,
                                onToggleVirtualController = onToggleVirtualController,
                                onSelectXboxWebUiMode = onSelectXboxWebUiMode,
                                onToggleRecording = onToggleRecording
                            )
                            QuickSettingsColQuality(config = config, isEn = isEn)
                            QuickSettingsColDisplay(config = config)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSettingsColControls(
    config: AppConfig,
    isEn: Boolean,
    recorderState: com.example.cloudplay.ScreenRecorderState,
    onToggleVirtualController: (Boolean) -> Unit,
    onSelectXboxWebUiMode: (XboxWebUiMode) -> Unit,
    onToggleRecording: () -> Unit
) {
    HudSectionLabel(config.tr("Điều khiển", "Controls"))

    HudSwitchRow(
        title = config.tr("Tay cầm ảo", "Virtual gamepad"),
        checked = config.showVirtualController,
        onCheckedChange = onToggleVirtualController
    )

    HudSwitchRow(
        title = config.tr("Dịch tiếng Việt", "Translate interface"),
        checked = config.xcloudTranslateEnabled,
        onCheckedChange = { SettingsRepository.updateXcloudTranslate(it) }
    )

    HudSectionLabel(config.tr("Giao diện web Xbox", "Xbox Web UI"))
    HudSegmentedControl(
        options = listOf(
            config.tr("Giao diện cũ", "Classic UI"),
            config.tr("Giao diện mới", "New UI")
        ),
        selectedIndex = if (config.xboxWebUiMode == XboxWebUiMode.CLASSIC) 0 else 1,
        onSelect = { idx ->
            onSelectXboxWebUiMode(if (idx == 0) XboxWebUiMode.CLASSIC else XboxWebUiMode.NEW_UI)
        },
        fontSize = 12.sp
    )
    Text(
        text = config.tr(
            "Cũ: xbox.com/play, khuyên dùng ở VN. Mới: play.xbox.com, ở Mỹ/Nhật hoặc VPN.",
            "Classic: xbox.com/play (stable). New: play.xbox.com (US/JP/VPN)."
        ),
        color = HudFaint,
        fontSize = 11.5.sp,
        lineHeight = 15.sp
    )

    HudSectionLabel(config.tr("Quay màn hình (.MP4)", "Screen recording (.MP4)"))

    // Chọn mức chất lượng quay
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HudSurface)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        ScreenRecordQuality.entries.forEach { q ->
            val selected = config.screenRecordQuality == q
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) HudSelBg else Color.Transparent)
                    .clickable { SettingsRepository.updateScreenRecordQuality(q) }
                    .padding(horizontal = 3.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = q.id,
                    color = if (selected) HudAccent else HudMuted,
                    fontSize = 11.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }

    HudSurfaceRow(minHeight = 42.dp) {
        if (recorderState.isRecording) {
            val mins = recorderState.elapsedSeconds / 60
            val secs = recorderState.elapsedSeconds % 60
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = config.tr("Đang quay", "Recording"),
                    color = HudInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "● %02d:%02d".format(mins, secs),
                    color = HudDanger,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }
        } else {
            Text(
                text = config.screenRecordQuality.localizedShort(isEn),
                color = HudMuted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        HudButton(
            text = if (recorderState.isRecording) {
                config.tr("Dừng", "Stop")
            } else {
                config.tr("Quay", "Record")
            },
            onClick = onToggleRecording,
            style = if (recorderState.isRecording) HudButtonStyle.DANGER else HudButtonStyle.MAIN,
            minHeight = 32.dp,
            fontSize = 12.sp,
            modifier = Modifier.testTag("quick_settings_record_button")
        )
    }
}

@Composable
private fun QuickSettingsColQuality(
    config: AppConfig,
    isEn: Boolean
) {
    HudSectionLabel(config.tr("Độ phân giải và đồ họa (4G / 5G / Wi-Fi)", "Resolution & graphics (4G / 5G / Wi-Fi)"))

    val modeOptions = listOf(
        CellularOptimizationMode.AUTO to config.tr("Tự động", "Auto"),
        CellularOptimizationMode.DATA_SAVER_HIGH_SPEED to "720p",
        CellularOptimizationMode.MEDIUM to "900p",
        CellularOptimizationMode.HIGH to "1080p",
        CellularOptimizationMode.MAX_SPEED_UNCAPPED to "1080p+"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HudSurface)
            .padding(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            modeOptions.take(3).forEach { (mode, label) ->
                QuickSettingsSegOption(
                    label = label,
                    selected = config.cellularMode == mode,
                    onClick = { SettingsRepository.updateCellularMode(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            modeOptions.drop(3).forEach { (mode, label) ->
                QuickSettingsSegOption(
                    label = label,
                    selected = config.cellularMode == mode,
                    onClick = { SettingsRepository.updateCellularMode(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    Text(
        text = config.cellularMode.localizedSubtitle(isEn),
        color = HudCyan,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold
    )
    Text(
        text = config.tr(
            "720p thấp · 900p vừa · 1080p cao · 1080p+ cao nhất",
            "720p low · 900p med · 1080p high · 1080p+ max"
        ),
        color = HudFaint,
        fontSize = 11.5.sp
    )

    HudSectionLabel(config.tr("Khung hình (FPS)", "Frame rate (FPS)"))
    HudSegmentedControl(
        options = StreamFpsCap.entries.map { "${it.fps}" },
        selectedIndex = StreamFpsCap.entries.indexOf(config.streamFpsCap).coerceAtLeast(0),
        onSelect = { idx ->
            StreamFpsCap.entries.getOrNull(idx)?.let { SettingsRepository.updateStreamFpsCap(it) }
        },
        fontSize = 12.sp
    )
}

@Composable
private fun QuickSettingsSegOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) HudSelBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) HudAccent else HudMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun QuickSettingsColDisplay(config: AppConfig) {
    HudSwitchRow(
        title = config.tr("Chế độ máy yếu", "Low-end device mode"),
        subtitle = config.tr("Đồ họa nhẹ 5 Mbps, giữ máy mát", "Light 5 Mbps stream, runs cool"),
        checked = config.lowEndDeviceMode,
        onCheckedChange = { SettingsRepository.updateLowEndDeviceMode(it) }
    )

    HudSwitchRow(
        title = config.tr("Giảm hiệu ứng nền", "Light background effects"),
        subtitle = config.tr("Giúp máy đỡ nóng và đỡ giật hơn", "Helps phone run cooler and smoother"),
        checked = config.gpuLoadReductionEnabled,
        onCheckedChange = { SettingsRepository.updateGpuLoadReductionEnabled(it) }
    )

    HudSectionLabel(config.tr("Màu sắc và độ sáng", "Colors & brightness"))

    HudSliderRow(
        label = config.tr("Độ sáng", "Brightness"),
        valueText = "${config.videoBrightness}%",
        value = config.videoBrightness.toFloat(),
        valueRange = 80f..130f,
        onValueChange = { SettingsRepository.updateVideoBrightness(it.toInt()) }
    )

    HudSliderRow(
        label = config.tr("Độ tương phản", "Contrast"),
        valueText = "${config.videoContrast}%",
        value = config.videoContrast.toFloat(),
        valueRange = 80f..140f,
        onValueChange = { SettingsRepository.updateVideoContrast(it.toInt()) }
    )

    HudSliderRow(
        label = config.tr("Độ tươi màu", "Saturation"),
        valueText = "${config.videoSaturation}%",
        value = config.videoSaturation.toFloat(),
        valueRange = 80f..150f,
        onValueChange = { SettingsRepository.updateVideoSaturation(it.toInt()) }
    )

    HudButton(
        text = config.tr("Mặc định", "Reset defaults"),
        onClick = { SettingsRepository.resetVideoDisplayDefaults() },
        style = HudButtonStyle.DEFAULT,
        containerOverride = HudSurface,
        contentOverride = HudInk,
        minHeight = 40.dp,
        modifier = Modifier.fillMaxWidth()
    )
}
