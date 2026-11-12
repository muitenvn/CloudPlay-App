package com.example.ui.tabs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
    val resShortLabel = when (config.cellularMode) {
        CellularOptimizationMode.AUTO -> config.tr("Tự động", "Auto")
        CellularOptimizationMode.DATA_SAVER_HIGH_SPEED -> "720p"
        CellularOptimizationMode.MEDIUM -> "900p"
        CellularOptimizationMode.HIGH -> "1080p"
        CellularOptimizationMode.MAX_SPEED_UNCAPPED -> "1080p+"
    }
    CollapsibleSettingsGroup(
        title = if (isEn) "Resolution & FPS" else "Độ phân giải và FPS",
        subtitle = "$resShortLabel · ${config.streamFpsCap.fps} FPS",
        icon = Icons.Default.Speed,
        expanded = expandGraphics,
        onToggle = onToggleExpand,
        testTag = "settings_group_graphics"
    ) {
        SettingsCard(title = "") {
            Text(
                text = config.tr("Độ phân giải", "Resolution"),
                color = TextSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
            )

            val selectedMode = config.cellularMode
            val resolutionItems = listOf(
                Triple(CellularOptimizationMode.AUTO, config.tr("Tự động", "Auto"), "settings_graphics_mode_auto"),
                Triple(CellularOptimizationMode.DATA_SAVER_HIGH_SPEED, "720p", "settings_graphics_mode_${CellularOptimizationMode.DATA_SAVER_HIGH_SPEED.id}"),
                Triple(CellularOptimizationMode.MEDIUM, "900p", "settings_graphics_mode_${CellularOptimizationMode.MEDIUM.id}"),
                Triple(CellularOptimizationMode.HIGH, "1080p", "settings_graphics_mode_${CellularOptimizationMode.HIGH.id}"),
                Triple(CellularOptimizationMode.MAX_SPEED_UNCAPPED, "1080p+", "settings_graphics_mode_${CellularOptimizationMode.MAX_SPEED_UNCAPPED.id}")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                resolutionItems.forEach { (mode, label, tag) ->
                    val isSelected = selectedMode == mode
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 38.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .border(
                                1.dp,
                                if (isSelected) XboxNeonGreen else XboxCardBorder,
                                RoundedCornerShape(999.dp)
                            )
                            .clickable { SettingsRepository.updateCellularMode(mode) }
                            .testTag(tag),
                        color = if (isSelected) Color(0xFF0F2C1D) else Color(0xFF132019)
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) XboxNeonGreen else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Text(
                text = config.tr(
                    "Tự động giữ ổn định khi mạng yếu.",
                    "Auto keeps stream stable on weak networks."
                ),
                color = TextSecondary,
                fontSize = 12.sp
            )

            Text(
                text = config.tr("Tốc độ khung hình (FPS)", "Frame Rate (FPS)"),
                color = TextSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, XboxCardBorder, RoundedCornerShape(12.dp)),
                color = Color(0xFF132019)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    com.example.data.StreamFpsCap.entries.forEach { fpsCap ->
                        val isSelected = config.streamFpsCap == fpsCap
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 38.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { SettingsRepository.updateStreamFpsCap(fpsCap) }
                                .testTag("settings_fps_cap_${fpsCap.fps}"),
                            color = if (isSelected) XboxNeonGreen else Color.Transparent
                        ) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${fpsCap.fps}",
                                    color = if (isSelected) Color(0xFF03140B) else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            SettingToggleRow(
                title = if (isEn) "Sharpen Image" else "Tăng độ nét",
                subtitle = "",
                checked = config.clarityBoostEnabled,
                onCheckedChange = { SettingsRepository.updateClarityBoost(it) },
                switchTestTag = "settings_switch_clarity_boost"
            )

            SettingToggleRow(
                title = if (isEn) "Prioritize Smooth Response" else "Ưu tiên phản hồi mượt",
                subtitle = "",
                checked = config.wifiSpeedFirstPriority,
                onCheckedChange = { SettingsRepository.updateWifiSpeedFirstPriority(it) },
                switchTestTag = "settings_switch_smooth_response"
            )

            SettingToggleRow(
                title = if (isEn) "Reduce GPU Load" else "Giảm tải GPU",
                subtitle = "",
                checked = config.gpuLoadReductionEnabled,
                onCheckedChange = { SettingsRepository.updateGpuLoadReductionEnabled(it) },
                switchTestTag = "settings_switch_gpu_reduction"
            )

            Text(
                text = if (isEn) "Xbox Web Interface" else "Giao diện web Xbox",
                color = TextSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, XboxCardBorder, RoundedCornerShape(12.dp)),
                color = Color(0xFF132019)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    com.example.data.XboxWebUiMode.entries.forEach { uiMode ->
                        val isSelected = config.xboxWebUiMode == uiMode
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 38.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { SettingsRepository.updateXboxWebUiMode(uiMode) }
                                .testTag("settings_xbox_ui_mode_${uiMode.id}"),
                            color = if (isSelected) XboxNeonGreen else Color.Transparent
                        ) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = uiMode.localizedTitle(isEn),
                                    color = if (isSelected) Color(0xFF03140B) else TextSecondary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            SettingToggleRow(
                title = if (isEn) "Auto-Exit New UI Handler" else "Tự động xử lý thoát Giao diện mới",
                subtitle = "",
                checked = config.autoExitPreviewHandlerEnabled,
                onCheckedChange = { SettingsRepository.updateAutoExitPreviewHandlerEnabled(it) },
                switchTestTag = "settings_switch_auto_exit_preview"
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
