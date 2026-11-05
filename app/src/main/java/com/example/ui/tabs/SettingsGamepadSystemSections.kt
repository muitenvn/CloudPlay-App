package com.example.ui.tabs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppConfig
import com.example.data.GamepadHapticLevel
import com.example.data.GamepadLayoutPreset
import com.example.data.SettingsRepository
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant

@Composable
internal fun SettingsGamepadTouchSection(
    config: AppConfig,
    isEn: Boolean,
    expandControls: Boolean,
    onToggleExpand: () -> Unit,
    onRequestResetGamepadConfirm: () -> Unit
) {
    val context = LocalContext.current
    CollapsibleSettingsGroup(
        title = if (isEn) "Gamepad & Touch Controls" else "Tay cầm & Cảm ứng",
        subtitle = if (config.showVirtualController) {
            if (isEn) "Virtual Gamepad: ON" else "Tay cầm ảo: Đang bật"
        } else {
            if (isEn) "Virtual Gamepad: OFF" else "Tay cầm ảo: Đang tắt"
        },
        icon = Icons.Default.Gamepad,
        expanded = expandControls,
        onToggle = onToggleExpand,
        testTag = "settings_group_controls"
    ) {
        var activeRemapSourceBtn by remember { mutableStateOf<Int?>(null) }
        var activeShortcutTargetAction by remember { mutableStateOf<String?>(null) }

        SettingsCard(title = if (isEn) "Touch Gamepad" else "Tay cầm ảo trên màn hình") {
            SettingToggleRow(
                title = if (isEn) "Show Virtual Gamepad" else "Bật tay cầm ảo",
                subtitle = if (isEn) "On-screen buttons with custom drag layout" else "Hiện nút bấm ảo, kéo thả vị trí tự do",
                checked = config.showVirtualController,
                onCheckedChange = { SettingsRepository.updateShowVirtualController(it) },
                switchTestTag = "settings_switch_virtual_controller"
            )

            SettingToggleRow(
                title = if (isEn) "Auto-Hide Virtual Gamepad on Physical Controller" else "Tự ẩn tay cầm ảo khi cắm tay cầm thật",
                subtitle = if (isEn) "Automatically hide on-screen buttons when a Bluetooth/USB controller is connected" else "Tự động ẩn nút cảm ứng trên màn hình khi phát hiện tay cầm Bluetooth/USB",
                checked = config.autoHideVirtualOnPhysicalController,
                onCheckedChange = { SettingsRepository.updateAutoHideVirtualOnPhysicalController(it) },
                activeColor = DirectCyan,
                switchTestTag = "settings_switch_auto_hide_virtual_pad"
            )

            SettingToggleRow(
                title = if (isEn) "Save Layout Per Game" else "Lưu bố cục riêng theo từng game",
                subtitle = if (isEn) "Remember custom button positions, sizes, and hidden buttons separately for each game" else "Tự nhớ vị trí, kích thước và các nút ẩn riêng cho từng tựa game đang chơi",
                checked = config.usePerGameGamepadLayout,
                onCheckedChange = { SettingsRepository.updateUsePerGameGamepadLayout(it) },
                activeColor = XboxNeonGreen,
                switchTestTag = "settings_switch_per_game_layout"
            )

            val savedPerGameCount = remember(config.perGameLayoutsJson) {
                SettingsRepository.parseAllPerGameLayouts(config.perGameLayoutsJson).size
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        SettingsRepository.updateUsePerGameGamepadLayout(false)
                        Toast.makeText(
                            context,
                            if (isEn) "Switched to Global Layout for all games" else "Đã chuyển sang Dùng bố cục chung cho mọi game",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = if (isEn) "Use Global Layout" else "Dùng bố cục chung"
                        }
                        .testTag("settings_use_global_gamepad_layout_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isEn) "Use Global Layout" else "Dùng bố cục chung",
                        color = DirectCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                OutlinedButton(
                    onClick = {
                        SettingsRepository.clearAllPerGameLayouts()
                        SettingsRepository.resetGamepadCustomOffsets()
                        Toast.makeText(
                            context,
                            if (isEn) "Reset all gamepad layouts to default" else "Đã đặt lại bố cục tay cầm ảo về mặc định",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = if (isEn) "Reset Gamepad Layout" else "Đặt lại bố cục tay cầm"
                        }
                        .testTag("settings_reset_per_game_layouts_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (savedPerGameCount > 0) {
                            if (isEn) "Reset ($savedPerGameCount)" else "Đặt lại ($savedPerGameCount)"
                        } else {
                            if (isEn) "Reset" else "Đặt lại"
                        },
                        color = XboxNeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Text(
                text = if (isEn) "Button Layout" else "Bố cục nút bấm",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GamepadLayoutPreset.entries.forEach { preset ->
                    val isSelected = config.gamepadLayoutPreset == preset
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                1.dp,
                                if (isSelected) XboxNeonGreen else XboxCardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { SettingsRepository.updateGamepadLayoutPreset(preset) },
                        color = if (isSelected) XboxSurfaceVariant else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.localizedTitle(isEn),
                                    color = if (isSelected) XboxNeonGreen else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = preset.localizedDesc(isEn),
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LabeledSlider(
                    label = if (isEn) "Button Size" else "Kích thước nút",
                    valueText = "${(config.gamepadScale * 100).toInt()}%",
                    value = config.gamepadScale,
                    onValueChange = { SettingsRepository.updateGamepadScale(it) },
                    valueRange = 0.75f..1.35f,
                    accentColor = XboxNeonGreen
                )
                LabeledSlider(
                    label = if (isEn) "Opacity" else "Độ đậm mờ",
                    valueText = "${(config.gamepadOpacity * 100).toInt()}%",
                    value = config.gamepadOpacity,
                    onValueChange = { SettingsRepository.updateGamepadOpacity(it) },
                    valueRange = 0.25f..0.95f,
                    accentColor = DirectCyan
                )
                LabeledSlider(
                    label = if (isEn) "Stick Sensitivity" else "Độ nhạy cần xoay",
                    valueText = "${(config.gamepadSensitivity * 100).toInt()}%",
                    value = config.gamepadSensitivity,
                    onValueChange = { SettingsRepository.updateGamepadSensitivity(it) },
                    valueRange = 0.60f..1.60f,
                    accentColor = XboxNeonGreen
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEn) "Virtual Stick Inner / Outer Deadzones" else "Vùng chết trong / ngoài (Cần ảo)",
                        color = DirectCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isEn) "Reset" else "Đặt lại",
                        color = XboxNeonGreen,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { SettingsRepository.resetVirtualStickDeadzones() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .semantics {
                                contentDescription = if (isEn) "Reset virtual stick deadzones" else "Đặt lại vùng chết cần ảo"
                            }
                            .testTag("settings_reset_virtual_deadzone_btn")
                    )
                }
                LabeledSlider(
                    label = if (isEn) "Left Stick Inner Deadzone" else "Cần trái - Vùng chết trong",
                    valueText = "${(config.leftStickInnerDeadzone * 100).toInt()}%",
                    value = config.leftStickInnerDeadzone,
                    onValueChange = { SettingsRepository.updateLeftStickInnerDeadzone(it) },
                    valueRange = 0.00f..0.30f,
                    accentColor = DirectCyan
                )
                LabeledSlider(
                    label = if (isEn) "Left Stick Outer Threshold" else "Cần trái - Ngưỡng ngoài",
                    valueText = "${(config.leftStickOuterDeadzone * 100).toInt()}%",
                    value = config.leftStickOuterDeadzone,
                    onValueChange = { SettingsRepository.updateLeftStickOuterDeadzone(it) },
                    valueRange = 0.70f..1.00f,
                    accentColor = XboxNeonGreen
                )
                LabeledSlider(
                    label = if (isEn) "Right Stick Inner Deadzone" else "Cần phải - Vùng chết trong",
                    valueText = "${(config.rightStickInnerDeadzone * 100).toInt()}%",
                    value = config.rightStickInnerDeadzone,
                    onValueChange = { SettingsRepository.updateRightStickInnerDeadzone(it) },
                    valueRange = 0.00f..0.30f,
                    accentColor = DirectCyan
                )
                LabeledSlider(
                    label = if (isEn) "Right Stick Outer Threshold" else "Cần phải - Ngưỡng ngoài",
                    valueText = "${(config.rightStickOuterDeadzone * 100).toInt()}%",
                    value = config.rightStickOuterDeadzone,
                    onValueChange = { SettingsRepository.updateRightStickOuterDeadzone(it) },
                    valueRange = 0.70f..1.00f,
                    accentColor = XboxNeonGreen
                )
            }

            Text(
                text = if (isEn) "Touch Button Vibration" else "Độ rung khi bấm nút cảm ứng",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GamepadHapticLevel.entries.forEach { level ->
                    val selected = config.gamepadHapticLevel == level
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { SettingsRepository.updateGamepadHapticLevel(level) }
                            .semantics {
                                contentDescription = if (isEn) "Touch vibration ${level.localizedTitle(true)}" else "Rung khi bấm ${level.localizedTitle(false)}"
                            },
                        color = if (selected) XboxNeonGreen.copy(alpha = 0.22f) else Color(0xFF0E1813),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) XboxNeonGreen else XboxCardBorder
                        )
                    ) {
                        Text(
                            text = level.localizedTitle(isEn),
                            color = if (selected) XboxNeonGreen else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 7.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Text(
                text = if (isEn) "In-Game Rumble to Phone (Virtual Pad Only)" else "Rung phản hồi từ Game sang Điện thoại (Khi dùng tay cầm ảo)",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isEn) {
                    "Forwards game vibrationActuator effects to phone vibrator when playing with virtual controls. Physical controllers keep their native actuator."
                } else {
                    "Chuyển lệnh rung của game sang bộ rung điện thoại khi chơi bằng tay cầm ảo (có giới hạn nhịp chống nóng máy). Tay cầm thật giữ nguyên rung gốc."
                },
                color = TextSecondary,
                fontSize = 10.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GamepadHapticLevel.entries.forEach { level ->
                    val selected = config.gameRumbleLevel == level
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { SettingsRepository.updateGameRumbleLevel(level) }
                            .semantics {
                                contentDescription = if (isEn) "Game rumble ${level.localizedTitle(true)}" else "Rung của game ${level.localizedTitle(false)}"
                            }
                            .testTag("settings_game_rumble_${level.name.lowercase()}_btn"),
                        color = if (selected) DirectCyan.copy(alpha = 0.22f) else Color(0xFF0E1813),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) DirectCyan else XboxCardBorder
                        )
                    ) {
                        Text(
                            text = level.localizedTitle(isEn),
                            color = if (selected) DirectCyan else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 7.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            SettingToggleRow(
                title = if (isEn) "🏃 Auto-Sprint" else "🏃 Đẩy kịch cần trái để chạy nhanh",
                subtitle = if (isEn) "Push left stick forward to sprint" else "Tự chạy nhanh khi đẩy cần hết cỡ",
                checked = config.gamepadSprintLockEnabled,
                onCheckedChange = { SettingsRepository.updateGamepadSprintLockEnabled(it) }
            )

            if (config.gamepadCustomOffsets.isNotBlank()) {
                OutlinedButton(
                    onClick = onRequestResetGamepadConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = if (isEn) "Reset Default Layout" else "Đặt lại bố cục mặc định"
                        }
                        .testTag("settings_reset_gamepad_positions_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isEn) "Reset Default Layout" else "Đặt lại bố cục mặc định",
                        color = DirectCyan,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // THẺ 3.2: TAY CẦM THẬT (ĐỔI NÚT, VÙNG CHẾT TRONG/NGOÀI) & TỔ HỢP PHÍM TẮT
        SettingsCard(title = if (isEn) "Physical Controller & Combo Shortcuts" else "Tay cầm thật & Tổ hợp phím tắt") {
            val activeButtonMap = remember(config.physicalGamepadButtonMap) {
                SettingsRepository.parsePhysicalButtonMap(config.physicalGamepadButtonMap)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEn) "Physical Controller Button Remapping" else "Đổi nút cho tay cầm thật (Remap)",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isEn) {
                            "Applies only to physical Bluetooth/USB controllers. Virtual gamepad is unaffected."
                        } else {
                            "Chỉ áp dụng cho đầu vào từ tay cầm thật (Bluetooth/USB), không ảnh hưởng tay cầm ảo."
                        },
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
                OutlinedButton(
                    onClick = {
                        SettingsRepository.resetPhysicalGamepadButtonMap()
                        Toast.makeText(
                            context,
                            if (isEn) "Reset physical controller button mapping" else "Đã đặt lại ánh xạ nút tay cầm thật",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .semantics {
                            contentDescription = if (isEn) "Reset physical button mapping" else "Đặt lại ánh xạ nút tay cầm thật"
                        }
                        .testTag("settings_reset_physical_remap_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isEn) "Reset" else "Đặt lại",
                        color = XboxNeonGreen,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            SettingsRepository.STANDARD_GAMEPAD_BUTTONS.chunked(4).forEach { rowButtons ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowButtons.forEach { (btnIdx, shortName, _) ->
                        val mappedTarget = activeButtonMap[btnIdx] ?: btnIdx
                        val targetLabel = SettingsRepository.getGamepadButtonShortLabel(mappedTarget)
                        val isRemapped = mappedTarget != btnIdx
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { activeRemapSourceBtn = btnIdx }
                                .semantics {
                                    contentDescription = if (isEn) {
                                        "Remap physical button $shortName, currently mapped to $targetLabel"
                                    } else {
                                        "Đổi nút $shortName trên tay cầm thật, đang gán thành $targetLabel"
                                    }
                                }
                                .testTag("settings_remap_btn_$btnIdx"),
                            color = if (isRemapped) XboxNeonGreen.copy(alpha = 0.20f) else Color(0xFF0E1813),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isRemapped) XboxNeonGreen else XboxCardBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = shortName,
                                    color = TextSecondary,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isRemapped) "→ $targetLabel" else targetLabel,
                                    color = if (isRemapped) XboxNeonGreen else TextPrimary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    repeat(4 - rowButtons.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEn) "Physical Stick Inner / Outer Deadzones" else "Vùng chết trong / ngoài (Tay cầm thật)",
                            color = DirectCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEn) {
                                "Default: 0% inner / 100% outer (passthrough without stacking on game deadzone)"
                            } else {
                                "Mặc định: Trong 0% / Ngoài 100% (giữ nguyên tín hiệu gốc, chỉ lọc khi bạn tự chỉnh)"
                            },
                            color = TextSecondary,
                            fontSize = 9.5.sp
                        )
                    }
                    Text(
                        text = if (isEn) "Reset" else "Đặt lại",
                        color = XboxNeonGreen,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { SettingsRepository.resetPhysicalStickDeadzones() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .semantics {
                                contentDescription = if (isEn) "Reset physical stick deadzones" else "Đặt lại vùng chết tay cầm thật"
                            }
                            .testTag("settings_reset_physical_deadzone_btn")
                    )
                }
                LabeledSlider(
                    label = if (isEn) "Physical Left Stick - Inner" else "Cần trái thật - Vùng chết trong",
                    valueText = "${(config.physLeftStickInnerDeadzone * 100).toInt()}%",
                    value = config.physLeftStickInnerDeadzone,
                    onValueChange = { SettingsRepository.updatePhysLeftStickInnerDeadzone(it) },
                    valueRange = 0.00f..0.30f,
                    accentColor = DirectCyan
                )
                LabeledSlider(
                    label = if (isEn) "Physical Left Stick - Outer" else "Cần trái thật - Ngưỡng ngoài",
                    valueText = "${(config.physLeftStickOuterDeadzone * 100).toInt()}%",
                    value = config.physLeftStickOuterDeadzone,
                    onValueChange = { SettingsRepository.updatePhysLeftStickOuterDeadzone(it) },
                    valueRange = 0.70f..1.00f,
                    accentColor = XboxNeonGreen
                )
                LabeledSlider(
                    label = if (isEn) "Physical Right Stick - Inner" else "Cần phải thật - Vùng chết trong",
                    valueText = "${(config.physRightStickInnerDeadzone * 100).toInt()}%",
                    value = config.physRightStickInnerDeadzone,
                    onValueChange = { SettingsRepository.updatePhysRightStickInnerDeadzone(it) },
                    valueRange = 0.00f..0.30f,
                    accentColor = DirectCyan
                )
                LabeledSlider(
                    label = if (isEn) "Physical Right Stick - Outer" else "Cần phải thật - Ngưỡng ngoài",
                    valueText = "${(config.physRightStickOuterDeadzone * 100).toInt()}%",
                    value = config.physRightStickOuterDeadzone,
                    onValueChange = { SettingsRepository.updatePhysRightStickOuterDeadzone(it) },
                    valueRange = 0.70f..1.00f,
                    accentColor = XboxNeonGreen
                )
            }

            SettingToggleRow(
                title = if (isEn) "Controller Combo Shortcuts (Hold View + Button)" else "Tổ hợp phím tắt trên tay cầm (Giữ View + nút)",
                subtitle = if (isEn) {
                    "Hold View (Back) for 400ms + press trigger button. Single View presses pass to the game immediately."
                } else {
                    "Giữ nút View (Back) 400ms + bấm nút phụ để bật/tắt nhanh. Bấm View đơn lẻ vẫn vào game ngay lập tức."
                },
                checked = config.controllerShortcutsEnabled,
                onCheckedChange = { SettingsRepository.updateControllerShortcutsEnabled(it) },
                activeColor = XboxNeonGreen,
                switchTestTag = "settings_switch_controller_shortcuts"
            )

            if (config.controllerShortcutsEnabled) {
                val shortcutItems = listOf(
                    Triple(
                        "toggle_stats",
                        if (isEn) "Show / Hide Stream Stats Overlay" else "Ẩn / hiện bảng thông số luồng phát",
                        config.shortcutToggleStatsButton
                    ),
                    Triple(
                        "toggle_virtual_pad",
                        if (isEn) "Show / Hide Virtual Gamepad" else "Bật / tắt tay cầm ảo trên màn hình",
                        config.shortcutToggleVirtualPadButton
                    ),
                    Triple(
                        "toggle_recording",
                        if (isEn) "Start / Stop ZeroLag Screen Recording" else "Bật / tắt quay màn hình ZeroLag",
                        config.shortcutToggleRecordingButton
                    )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.28f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    shortcutItems.forEach { (actionKey, actionTitle, assignedBtnIdx) ->
                        val btnShort = SettingsRepository.getGamepadButtonShortLabel(assignedBtnIdx)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = actionTitle,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { activeShortcutTargetAction = actionKey }
                                    .semantics {
                                        contentDescription = if (isEn) {
                                            "Change shortcut button for $actionTitle, currently View plus $btnShort"
                                        } else {
                                            "Đổi nút phím tắt cho $actionTitle, hiện tại là View cộng $btnShort"
                                        }
                                    }
                                    .testTag("settings_shortcut_btn_$actionKey"),
                                color = DirectCyan.copy(alpha = 0.18f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DirectCyan)
                            ) {
                                Text(
                                    text = "View + $btnShort",
                                    color = DirectCyan,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { SettingsRepository.resetControllerShortcuts() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = if (isEn) "Reset controller shortcuts" else "Đặt lại tổ hợp phím tắt tay cầm"
                            }
                            .testTag("settings_reset_controller_shortcuts_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isEn) "Reset Default Shortcuts" else "Đặt lại phím tắt mặc định",
                            color = XboxNeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (activeRemapSourceBtn != null) {
            val srcIdx = activeRemapSourceBtn ?: 0
            val srcName = SettingsRepository.getGamepadButtonLabel(srcIdx, isEn)
            val currentMap = SettingsRepository.parsePhysicalButtonMap(config.physicalGamepadButtonMap)
            val currentDest = currentMap[srcIdx] ?: srcIdx
            AlertDialog(
                onDismissRequest = { activeRemapSourceBtn = null },
                containerColor = XboxSurface,
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary,
                title = {
                    Text(
                        text = if (isEn) "Remap Physical Button: $srcName" else "Đổi nút tay cầm thật: $srcName",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isEn) {
                                "Select which Xbox button signal is sent when you press $srcName on your physical controller:"
                            } else {
                                "Chọn tín hiệu nút Xbox sẽ gửi vào game khi bạn bấm nút $srcName trên tay cầm thật:"
                            },
                            fontSize = 12.sp
                        )
                        SettingsRepository.STANDARD_GAMEPAD_BUTTONS.chunked(3).forEach { rowBtns ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowBtns.forEach { (dstIdx, dstShort, _) ->
                                    val selected = dstIdx == currentDest
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                SettingsRepository.updatePhysicalGamepadSingleButtonMap(srcIdx, dstIdx)
                                                activeRemapSourceBtn = null
                                            }
                                            .semantics {
                                                contentDescription = if (isEn) "Map $srcName to $dstShort" else "Gán $srcName thành $dstShort"
                                            },
                                        color = if (selected) XboxNeonGreen.copy(alpha = 0.25f) else Color(0xFF0E1813),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (selected) XboxNeonGreen else XboxCardBorder
                                        )
                                    ) {
                                        Text(
                                            text = dstShort,
                                            color = if (selected) XboxNeonGreen else TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 7.dp)
                                        )
                                    }
                                }
                                repeat(3 - rowBtns.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { activeRemapSourceBtn = null },
                        modifier = Modifier.semantics {
                            contentDescription = if (isEn) "Close button remap dialog" else "Đóng hộp thoại đổi nút"
                        }
                    ) {
                        Text(text = if (isEn) "Close" else "Đóng", color = TextSecondary)
                    }
                }
            )
        }

        if (activeShortcutTargetAction != null) {
            val actionKey = activeShortcutTargetAction ?: ""
            val selectableButtons = remember {
                SettingsRepository.STANDARD_GAMEPAD_BUTTONS.filter { it.first != 8 && it.first != 16 }
            }
            AlertDialog(
                onDismissRequest = { activeShortcutTargetAction = null },
                containerColor = XboxSurface,
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary,
                title = {
                    Text(
                        text = if (isEn) "Select Trigger Button (View + Button)" else "Chọn nút kích hoạt (Giữ View + nút)",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isEn) {
                                "Choose the secondary button to press while holding View (Back) for 400ms:"
                            } else {
                                "Chọn nút thứ hai để bấm khi đang giữ nút View (Back) từ 400ms trở lên:"
                            },
                            fontSize = 12.sp
                        )
                        selectableButtons.chunked(3).forEach { rowBtns ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowBtns.forEach { (btnIdx, btnShort, _) ->
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                when (actionKey) {
                                                    "toggle_stats" -> SettingsRepository.updateShortcutToggleStatsButton(btnIdx)
                                                    "toggle_virtual_pad" -> SettingsRepository.updateShortcutToggleVirtualPadButton(btnIdx)
                                                    "toggle_recording" -> SettingsRepository.updateShortcutToggleRecordingButton(btnIdx)
                                                }
                                                activeShortcutTargetAction = null
                                            }
                                            .semantics {
                                                contentDescription = if (isEn) "Set shortcut to View plus $btnShort" else "Đặt phím tắt là View cộng $btnShort"
                                            },
                                        color = Color(0xFF0E1813),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, DirectCyan.copy(alpha = 0.65f))
                                    ) {
                                        Text(
                                            text = "View + $btnShort",
                                            color = DirectCyan,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 7.dp)
                                        )
                                    }
                                }
                                repeat(3 - rowBtns.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { activeShortcutTargetAction = null },
                        modifier = Modifier.semantics {
                            contentDescription = if (isEn) "Close shortcut button dialog" else "Đóng hộp thoại chọn phím tắt"
                        }
                    ) {
                        Text(text = if (isEn) "Close" else "Đóng", color = TextSecondary)
                    }
                }
            )
        }
    }
}
