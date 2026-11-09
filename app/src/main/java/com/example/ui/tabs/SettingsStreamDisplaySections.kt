package com.example.ui.tabs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppConfig
import com.example.data.CellularOptimizationMode
import com.example.data.SettingsRepository
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxNeonGreen

@Composable
internal fun SettingsGraphicsFpsSection(
    config: AppConfig,
    isEn: Boolean,
    expandGraphics: Boolean,
    onToggleExpand: () -> Unit
) {
    CollapsibleSettingsGroup(
        title = if (isEn) "Resolution, Graphics & FPS" else "Độ phân giải, Đồ họa & FPS",
        subtitle = "${config.cellularMode.localizedTitle(isEn)} • ${config.streamFpsCap.fps} FPS",
        icon = Icons.Default.Speed,
        expanded = expandGraphics,
        onToggle = onToggleExpand,
        testTag = "settings_group_graphics"
    ) {
        SettingsCard(title = if (isEn) "Resolution & Bitrate (Wi-Fi / 4G / 5G)" else "Độ phân giải & Băng thông (Wi-Fi / 4G / 5G)") {
            val selectedMode = config.cellularMode
            val isAutoSelected = selectedMode == CellularOptimizationMode.AUTO

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.2.dp,
                        if (isAutoSelected) XboxNeonGreen else XboxCardBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { SettingsRepository.updateCellularMode(CellularOptimizationMode.AUTO) }
                    .testTag("settings_graphics_mode_auto"),
                color = if (isAutoSelected) Color(0xFF13241A) else Color(0xFF0F1813)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isAutoSelected) XboxNeonGreen else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = config.tr("Tự động • 720p ⇄ 1080p (Khuyên dùng)", "Auto • 720p ⇄ 1080p (Recommended)"),
                                color = if (isAutoSelected) XboxNeonGreen else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = config.tr("Tự hạ độ phân giải để giảm độ trễ trên cả 4G, 5G và Wi-Fi", "Auto-lowers resolution to cut lag on 4G, 5G & Wi-Fi"),
                                color = TextSecondary,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                    if (isAutoSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(18.dp))
                    }
                }
            }

            val manualModes = listOf(
                CellularOptimizationMode.DATA_SAVER_HIGH_SPEED,
                CellularOptimizationMode.MEDIUM,
                CellularOptimizationMode.HIGH,
                CellularOptimizationMode.MAX_SPEED_UNCAPPED
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                manualModes.chunked(2).forEach { rowModes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowModes.forEach { mode ->
                            val isSelected = selectedMode == mode
                            val isLowOrMed = mode == CellularOptimizationMode.DATA_SAVER_HIGH_SPEED ||
                                mode == CellularOptimizationMode.MEDIUM
                            val accent = if (isLowOrMed) XboxNeonGreen else DirectCyan

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) accent else XboxCardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { SettingsRepository.updateCellularMode(mode) }
                                    .testTag("settings_graphics_mode_${mode.id}"),
                                color = if (isSelected) {
                                    if (isLowOrMed) Color(0xFF132219) else Color(0xFF0F2327)
                                } else {
                                    Color(0xFF0F1813)
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = mode.localizedTitle(isEn),
                                            color = if (isSelected) accent else TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = accent,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = mode.localizedSubtitle(isEn),
                                        color = TextSecondary,
                                        fontSize = 9.5.sp,
                                        lineHeight = 12.5.sp,
                                        maxLines = 3
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = config.tr("Nhịp khung hình (FPS)", "Frame Rate (FPS)"),
                color = XboxNeonGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.example.data.StreamFpsCap.entries.forEach { fpsCap ->
                    val isSelected = config.streamFpsCap == fpsCap
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                1.dp,
                                if (isSelected) XboxNeonGreen else XboxCardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { SettingsRepository.updateStreamFpsCap(fpsCap) }
                            .testTag("settings_fps_cap_${fpsCap.fps}"),
                        color = if (isSelected) Color(0xFF13241A) else Color(0xFF0F1813)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${fpsCap.fps} FPS",
                                color = if (isSelected) XboxNeonGreen else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = fpsCap.localizedDesc(isEn),
                                color = TextSecondary,
                                fontSize = 9.sp,
                                lineHeight = 11.5.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            SettingToggleRow(
                title = if (isEn) "Sharpen Image" else "Tăng độ nét hình ảnh",
                subtitle = if (isEn) "Makes characters and scenery clearer" else "Làm rõ chi tiết nhân vật và khung cảnh",
                checked = config.clarityBoostEnabled,
                onCheckedChange = { SettingsRepository.updateClarityBoost(it) },
                switchTestTag = "settings_switch_clarity_boost"
            )

            Text(
                text = if (isEn) "Xbox Web Interface (Classic vs New UI)" else "Giao diện web Xbox (Giao diện cũ / Giao diện mới)",
                color = DirectCyan,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.example.data.XboxWebUiMode.entries.forEach { uiMode ->
                    val isSelected = config.xboxWebUiMode == uiMode
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                1.dp,
                                if (isSelected) XboxNeonGreen else XboxCardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { SettingsRepository.updateXboxWebUiMode(uiMode) }
                            .testTag("settings_xbox_ui_mode_${uiMode.id}"),
                        color = if (isSelected) Color(0xFF13241A) else Color(0xFF0F1813)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = uiMode.localizedTitle(isEn),
                                color = if (isSelected) XboxNeonGreen else TextPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = uiMode.localizedDesc(isEn),
                                color = TextSecondary,
                                fontSize = 9.sp,
                                lineHeight = 11.5.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            SettingToggleRow(
                title = if (isEn) "Auto-Exit New UI Handler" else "Tự động xử lý thoát Giao diện mới",
                subtitle = if (isEn) {
                    "Auto-clicks Cancel on the feedback dialog, waits for Xbox opt-out spinner to finish (~3.5s), then switches straight to Classic UI"
                } else {
                    "Tự động bấm Hủy hộp thoại phản hồi, đợi vòng xoay Xbox lưu xong (~3.5 giây) rồi chuyển thẳng về Giao diện cũ"
                },
                checked = config.autoExitPreviewHandlerEnabled,
                onCheckedChange = { SettingsRepository.updateAutoExitPreviewHandlerEnabled(it) },
                switchTestTag = "settings_switch_auto_exit_preview"
            )

            Text(
                text = if (isEn) {
                    "Tip: When enabled, open any game on play.xbox.com → tap your Xbox avatar → tap Exit preview. The app will automatically handle the rest."
                } else {
                    "Lưu ý: Khi bật mục này, nếu bị kẹt ở Giao diện mới, bạn chỉ cần vào trang một game bất kỳ → bấm ảnh đại diện Xbox → gạt Thoát xem trước (Exit preview), ứng dụng sẽ tự động xử lý toàn bộ các bước còn lại."
                },
                color = TextSecondary,
                fontSize = 9.5.sp,
                lineHeight = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Thẻ chỉnh Màu sắc hình ảnh (3 thanh Tương phản, Tươi màu, Độ sáng) & Chế độ máy yếu,
 * được đặt trong mục "Nâng cao" (Tầng 2).
 */
@Composable
internal fun SettingsAdvancedVideoColorsAndPerfCard(
    config: AppConfig,
    isEn: Boolean
) {
    val context = LocalContext.current
    SettingsCard(title = if (isEn) "Image Colors & Low-End Performance" else "Màu sắc hình ảnh & Hiệu năng máy yếu") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LabeledSlider(
                label = if (isEn) "Contrast" else "Độ tương phản",
                valueText = "${config.videoContrast}%",
                value = config.videoContrast.toFloat(),
                onValueChange = { SettingsRepository.updateVideoContrast(it.toInt()) },
                valueRange = 80f..140f,
                accentColor = XboxNeonGreen
            )
            LabeledSlider(
                label = if (isEn) "Color Saturation" else "Độ tươi màu",
                valueText = "${config.videoSaturation}%",
                value = config.videoSaturation.toFloat(),
                onValueChange = { SettingsRepository.updateVideoSaturation(it.toInt()) },
                valueRange = 80f..150f,
                accentColor = DirectCyan
            )
            LabeledSlider(
                label = if (isEn) "Brightness" else "Độ sáng",
                valueText = "${config.videoBrightness}%",
                value = config.videoBrightness.toFloat(),
                onValueChange = { SettingsRepository.updateVideoBrightness(it.toInt()) },
                valueRange = 80f..130f,
                accentColor = XboxNeonGreen
            )
        }

        OutlinedButton(
            onClick = {
                SettingsRepository.resetVideoDisplayDefaults()
                Toast.makeText(
                    context,
                    if (isEn) "Reset colors to default" else "Đã đặt lại màu sắc mặc định",
                    Toast.LENGTH_SHORT
                ).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = if (isEn) "Reset Colors to Default" else "Đặt lại màu sắc mặc định"
                }
                .testTag("advanced_reset_video_defaults_btn"),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = config.tr("Đặt lại màu sắc mặc định", "Reset Colors to Default"),
                color = DirectCyan,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        SettingToggleRow(
            title = if (isEn) "Low-End & Battery Saver Mode" else "Chế độ máy yếu & Tiết kiệm pin",
            subtitle = if (isEn) "Light 720p stream & reduced effects to keep phone cool" else "Dùng mức 720p nhẹ và tắt hiệu ứng thừa giúp máy mát, đỡ hao pin",
            checked = config.lowEndDeviceMode && config.gpuLoadReductionEnabled,
            onCheckedChange = { enabled ->
                SettingsRepository.updateLowEndDeviceMode(enabled)
                SettingsRepository.updateGpuLoadReductionEnabled(enabled)
            },
            switchTestTag = "settings_low_end_device_mode_switch"
        )
    }
}

@Composable
internal fun SettingsScreenRecordingSection(
    config: AppConfig,
    isEn: Boolean,
    expandRecording: Boolean,
    onToggleExpand: () -> Unit
) {
    CollapsibleSettingsGroup(
        title = if (isEn) "Screen Recording" else "Quay màn hình",
        subtitle = config.screenRecordQuality.localizedShort(isEn),
        icon = Icons.Default.Videocam,
        expanded = expandRecording,
        onToggle = onToggleExpand,
        testTag = "settings_group_recording"
    ) {
        SettingsCard(title = if (isEn) "Video Quality (.MP4)" else "Độ nét video (.MP4)") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.example.data.ScreenRecordQuality.entries.forEach { recQuality ->
                    val isSelected = config.screenRecordQuality == recQuality
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                1.dp,
                                if (isSelected) DirectCyan else XboxCardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { SettingsRepository.updateScreenRecordQuality(recQuality) }
                            .testTag("settings_record_quality_${recQuality.id}"),
                        color = if (isSelected) Color(0xFF0F2327) else Color(0xFF0F1813)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = recQuality.localizedTitle(isEn),
                                color = if (isSelected) DirectCyan else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = recQuality.localizedDesc(isEn),
                                color = TextSecondary,
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            Text(
                text = if (isEn) {
                    "Reduces background animations while recording to save battery."
                } else {
                    "Tạm giảm hiệu ứng chuyển động nền khi đang quay để đỡ nặng máy."
                },
                color = TextSecondary,
                fontSize = 10.5.sp
            )
        }
    }
}
