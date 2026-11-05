package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JapanRed
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxNeonGreen

/**
 * Nhóm 2: Điều khiển & Tiện ích nhanh (Làm nét, Giảm nền, Máy yếu, Dịch VI) với cỡ chữ >= 11sp.
 */
@Composable
internal fun DenseFeatureTogglesSection(
    clarityBoostActive: Boolean,
    hideStreamStats: Boolean,
    translationActive: Boolean,
    translatedCount: Int,
    onToggleClarityBoost: (Boolean) -> Unit,
    onToggleHideStreamStats: (Boolean) -> Unit,
    onToggleTranslation: (Boolean) -> Unit,
    onToggleGpuLoadReduction: () -> Unit
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF101C16),
        border = androidx.compose.foundation.BorderStroke(0.7.dp, XboxCardBorder.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = if (isEn) "Controls & Quick Toggles" else "Điều khiển & Tối ưu nhanh",
                color = DirectCyan,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DenseQuickTogglePill(
                    label = if (clarityBoostActive) {
                        if (isEn) "Sharpen: ON" else "Làm nét: Bật"
                    } else {
                        if (isEn) "Sharpen: OFF" else "Làm nét: Tắt"
                    },
                    active = clarityBoostActive,
                    activeColor = XboxNeonGreen,
                    onClick = { onToggleClarityBoost(!clarityBoostActive) },
                    modifier = Modifier.weight(1f)
                )
                DenseQuickTogglePill(
                    label = if (config.gpuLoadReductionEnabled) {
                        if (isEn) "Light BG: ON" else "Giảm nền: Bật"
                    } else {
                        if (isEn) "Light BG: OFF" else "Giảm nền: Tắt"
                    },
                    active = config.gpuLoadReductionEnabled,
                    activeColor = DirectCyan,
                    onClick = onToggleGpuLoadReduction,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DenseQuickTogglePill(
                    label = if (config.lowEndDeviceMode) {
                        if (isEn) "Low-End: ON" else "Máy yếu: Bật"
                    } else {
                        if (isEn) "Low-End: OFF" else "Máy yếu: Tắt"
                    },
                    active = config.lowEndDeviceMode,
                    activeColor = DirectCyan,
                    onClick = { com.example.data.SettingsRepository.updateLowEndDeviceMode(!config.lowEndDeviceMode) },
                    modifier = Modifier.weight(1f)
                )
                DenseQuickTogglePill(
                    label = if (translationActive) {
                        if (isEn) {
                            if (translatedCount > 0) "VI ($translatedCount)" else "VI Trans: ON"
                        } else {
                            if (translatedCount > 0) "Dịch VI ($translatedCount)" else "Dịch VI: Bật"
                        }
                    } else {
                        if (isEn) "VI Trans: OFF" else "Dịch VI: Tắt"
                    },
                    active = translationActive,
                    activeColor = XboxNeonGreen,
                    onClick = { onToggleTranslation(!translationActive) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
internal fun DenseQuickTogglePill(
    label: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = if (active) activeColor.copy(alpha = 0.18f) else Color(0xFF14221A),
        border = androidx.compose.foundation.BorderStroke(
            0.7.dp,
            if (active) activeColor.copy(alpha = 0.55f) else XboxCardBorder.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (active) activeColor else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/**
 * Nhóm Tùy chỉnh nhanh tay cầm ảo (Độ mờ, Kích thước nút) ngay trên HUD khi đang chơi.
 */
@Composable
internal fun DenseQuickGamepadSection(
    virtualControllerEnabled: Boolean,
    onInteraction: () -> Unit = {}
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    if (!virtualControllerEnabled) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF101C16),
        border = androidx.compose.foundation.BorderStroke(0.7.dp, XboxNeonGreen.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEn) "🎮 Virtual Gamepad Quick Adjust" else "🎮 Tùy chỉnh nhanh tay cầm ảo",
                    color = XboxNeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "${(config.gamepadOpacity * 100).toInt()}% • ${String.format("%.2f", config.gamepadScale)}x",
                    color = DirectCyan,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Opacity quick presets: 40%, 65%, 85%
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEn) "Opacity" else "Độ mờ",
                    color = TextSecondary,
                    fontSize = 10.5.sp,
                    modifier = Modifier.width(44.dp)
                )
                listOf(0.40f to "40%", 0.65f to "65%", 0.85f to "85%").forEach { (opVal, opLabel) ->
                    val isSel = kotlin.math.abs(config.gamepadOpacity - opVal) < 0.10f
                    DenseQualityPresetChip(
                        label = opLabel,
                        isSelected = isSel,
                        onClick = {
                            onInteraction()
                            com.example.data.SettingsRepository.updateGamepadOpacity(opVal)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Scale quick presets: 0.85x, 1.0x, 1.20x
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEn) "Scale" else "Cỡ nút",
                    color = TextSecondary,
                    fontSize = 10.5.sp,
                    modifier = Modifier.width(44.dp)
                )
                listOf(0.85f to "0.85x", 1.0f to "1.0x", 1.20f to "1.2x").forEach { (scVal, scLabel) ->
                    val isSel = kotlin.math.abs(config.gamepadScale - scVal) < 0.08f
                    DenseQualityPresetChip(
                        label = scLabel,
                        isSelected = isSel,
                        onClick = {
                            onInteraction()
                            com.example.data.SettingsRepository.updateGamepadScale(scVal)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Hàng nút tác vụ dưới cùng: Tải lại, Xoay, Đồ họa, Thoát (xếp 1 hàng ngang, biểu tượng ở trên và nhãn >= 11sp ở dưới).
 */
@Composable
internal fun DenseBottomActionsSection(
    onReloadPage: () -> Unit,
    onToggleOrientation: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestExitHome: () -> Unit
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DenseBottomActionChip(
            icon = Icons.Default.Refresh,
            label = if (isEn) "Reload" else "Tải lại",
            tint = XboxNeonGreen,
            onClick = onReloadPage,
            modifier = Modifier.weight(1f),
            testTag = "hud_expanded_reload_button"
        )
        DenseBottomActionChip(
            icon = Icons.Default.ScreenRotation,
            label = if (isEn) "Rotate" else "Xoay",
            tint = DirectCyan,
            onClick = onToggleOrientation,
            modifier = Modifier.weight(1f),
            testTag = "hud_expanded_rotate_button"
        )
        DenseBottomActionChip(
            icon = Icons.Default.Settings,
            label = if (isEn) "Graphics" else "Đồ họa",
            tint = DirectCyan,
            onClick = onOpenSettings,
            modifier = Modifier.weight(1f),
            testTag = "hud_expanded_settings_button"
        )
        DenseBottomActionChip(
            icon = Icons.Default.Home,
            label = if (isEn) "Exit" else "Thoát",
            tint = JapanRed,
            onClick = onRequestExitHome,
            modifier = Modifier.weight(1f),
            testTag = "hud_expanded_home_button"
        )
    }
}

@Composable
internal fun DenseBottomActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(7.dp),
        color = Color(0xFF101915),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, tint.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
