package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GamepadHapticLevel
import com.example.data.GamepadLayoutPreset
import com.example.data.SettingsRepository
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JapanRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AddOrManageButtonsModal(
    isEn: Boolean,
    hiddenButtons: Set<String>,
    abxySplitMode: Boolean,
    dpadSplitMode: Boolean,
    onToggleButtonVisibility: (String) -> Unit,
    onRestoreAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeNodes = remember(abxySplitMode, dpadSplitMode) {
        ALL_GAMEPAD_NODES.filter { node ->
            when (node.id) {
                "abxy" -> !abxySplitMode
                "btn_a", "btn_b", "btn_x", "btn_y" -> abxySplitMode
                "dpad" -> !dpadSplitMode
                "dpad_up", "dpad_down", "dpad_left", "dpad_right" -> dpadSplitMode
                else -> true
            }
        }
    }

    Surface(
        modifier = modifier
            .widthIn(max = 440.dp)
            .testTag("virtual_gamepad_add_button_modal"),
        shape = RoundedCornerShape(16.dp),
        color = XboxSurface.copy(alpha = 0.97f),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, DirectCyan.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEn) "＋ ADD OR REMOVE BUTTONS" else "＋ THÊM / XÓA NÚT TAY CẦM ẢO",
                    color = DirectCyan,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (hiddenButtons.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = XboxNeonGreen.copy(alpha = 0.18f),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxNeonGreen),
                            modifier = Modifier.clickable { onRestoreAll() }
                        ) {
                            Text(
                                text = if (isEn) "Show All" else "Hiện tất cả",
                                color = XboxNeonGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1A2820),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxCardBorder),
                        modifier = Modifier.clickable { onClose() }
                    ) {
                        Text(
                            text = "✕",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Text(
                text = if (isEn) {
                    "Tap a button below to add (+) it back to the screen or hide (✓) it:"
                } else {
                    "Chạm vào nút bên dưới để thêm lại (+) lên màn hình hoặc ẩn/xóa (✓):"
                },
                color = TextSecondary,
                fontSize = 10.5.sp
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                activeNodes.forEach { node ->
                    val isHidden = node.id in hiddenButtons
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isHidden) Color(0xFF1B1416) else XboxNeonGreen.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(
                            0.9.dp,
                            if (isHidden) JapanRed.copy(alpha = 0.65f) else XboxNeonGreen
                        ),
                        modifier = Modifier.clickable { onToggleButtonVisibility(node.id) }
                    ) {
                        Text(
                            text = if (isHidden) "＋ ${node.shortBadge}" else "✓ ${node.shortBadge}",
                            color = if (isHidden) Color(0xFFFF8A80) else XboxNeonGreen,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ControlQuickTunerPanel(
    isEn: Boolean,
    scale: Float,
    opacity: Float,
    deadzone: Float,
    sensitivity: Float,
    preset: GamepadLayoutPreset,
    hapticLevel: GamepadHapticLevel,
    gameRumbleLevel: GamepadHapticLevel,
    sprintLockEnabled: Boolean,
    abxySplitMode: Boolean,
    dpadSplitMode: Boolean,
    isEditPositionMode: Boolean,
    hasActiveGameKey: Boolean,
    hasPerGameCustomLayout: Boolean,
    gameDisplayName: String?,
    onUseGlobalLayout: () -> Unit,
    onToggleEditPosition: () -> Unit,
    onToggleAbxySplit: () -> Unit,
    onToggleDpadSplit: () -> Unit,
    onOpenAddButtons: () -> Unit,
    onResetPositions: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    if (showResetConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            containerColor = XboxSurface,
            titleContentColor = Color.White,
            textContentColor = TextSecondary,
            title = {
                Text(
                    text = if (isEn) "Reset Default Layout?" else "Đặt lại bố cục mặc định?",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            },
            text = {
                Text(
                    text = if (isEn) {
                        "Restore custom button coordinates to default positions? Split modes and hidden buttons will be preserved."
                    } else {
                        "Khôi phục tọa độ vị trí tùy chỉnh của các nút về mặc định? Chế độ tách nút và nút ẩn vẫn được giữ nguyên."
                    },
                    fontSize = 12.5.sp
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showResetConfirmDialog = false
                        onResetPositions()
                    },
                    modifier = Modifier.testTag("tuner_confirm_reset_positions_btn")
                ) {
                    Text(
                        text = if (isEn) "Reset" else "Đặt lại",
                        color = XboxNeonGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showResetConfirmDialog = false },
                    modifier = Modifier.testTag("tuner_cancel_reset_positions_btn")
                ) {
                    Text(
                        text = if (isEn) "Cancel" else "Hủy",
                        color = TextSecondary
                    )
                }
            }
        )
    }

    Surface(
        modifier = modifier
            .widthIn(max = 430.dp)
            .testTag("virtual_gamepad_tuner_panel"),
        shape = RoundedCornerShape(18.dp),
        color = XboxSurface.copy(alpha = 0.96f),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, XboxNeonGreen.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEn) "🎮 PRO CONTROL TUNER" else "🎮 TÙY CHỈNH TAY CẦM ẢO PRO",
                    color = XboxNeonGreen,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DirectCyan.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, DirectCyan),
                        modifier = Modifier.clickable { onToggleEditPosition() }
                    ) {
                        Text(
                            text = if (isEditPositionMode) {
                                if (isEn) "✓ Lock Pos" else "✓ Khóa vị trí"
                            } else {
                                if (isEn) "✥ Edit Buttons" else "✥ Sửa từng nút"
                            },
                            color = DirectCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1A2820),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxCardBorder),
                        modifier = Modifier
                            .clickable { showResetConfirmDialog = true }
                            .testTag("tuner_reset_default_layout_btn")
                    ) {
                        Text(
                            text = if (isEn) "Reset" else "Mặc định",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = XboxNeonGreen.copy(alpha = 0.22f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxNeonGreen),
                        modifier = Modifier.clickable { onClose() }
                    ) {
                        Text(
                            text = "✕",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Per-button Split / Merge & Add/Delete Quick Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                QuickFeatureChip(
                    modifier = Modifier.weight(1f),
                    label = if (abxySplitMode) {
                        if (isEn) "⇄ Merge ABXY" else "⇄ Gộp ABXY"
                    } else {
                        if (isEn) "⇄ Split A/B/X/Y" else "⇄ Tách A/B/X/Y"
                    },
                    active = abxySplitMode,
                    onClick = onToggleAbxySplit
                )
                QuickFeatureChip(
                    modifier = Modifier.weight(1f),
                    label = if (dpadSplitMode) {
                        if (isEn) "⇄ Merge D-Pad" else "⇄ Gộp D-Pad"
                    } else {
                        if (isEn) "⇄ Split D-Pad" else "⇄ Tách D-Pad"
                    },
                    active = dpadSplitMode,
                    onClick = onToggleDpadSplit
                )
                QuickFeatureChip(
                    modifier = Modifier.weight(1f),
                    label = if (isEn) "＋/🗑 Buttons" else "＋/🗑 Thêm/Xóa nút",
                    active = false,
                    onClick = onOpenAddButtons
                )
            }

            // Layout Presets Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                GamepadLayoutPreset.entries.forEach { item ->
                    val selected = preset == item
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { SettingsRepository.updateGamepadLayoutPreset(item) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) XboxNeonGreen.copy(alpha = 0.22f) else Color(0xFF0E1813),
                        border = androidx.compose.foundation.BorderStroke(
                            0.8.dp,
                            if (selected) XboxNeonGreen else XboxCardBorder
                        )
                    ) {
                        Text(
                            text = when (item) {
                                GamepadLayoutPreset.STANDARD -> "Xbox"
                                GamepadLayoutPreset.FPS_PRO -> "FPS Pro"
                                GamepadLayoutPreset.RACING -> "Racing"
                                GamepadLayoutPreset.COMPACT -> "Compact"
                            },
                            color = if (selected) XboxNeonGreen else TextSecondary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 5.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Per-Game Layout Bar (Use Global Layout + Reset)
            if (hasActiveGameKey) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (hasPerGameCustomLayout) {
                            if (isEn) "📌 Layout: ${gameDisplayName ?: "This Game"}" else "📌 Bố cục riêng: ${gameDisplayName ?: "Game này"}"
                        } else {
                            if (isEn) "🌐 Using Global Layout" else "🌐 Đang dùng bố cục chung"
                        },
                        color = if (hasPerGameCustomLayout) XboxNeonGreen else TextSecondary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF16241C),
                        border = androidx.compose.foundation.BorderStroke(
                            0.8.dp,
                            if (hasPerGameCustomLayout) DirectCyan else XboxCardBorder
                        ),
                        modifier = Modifier
                            .semantics {
                                contentDescription = if (isEn) "Use Global Layout" else "Dùng bố cục chung"
                            }
                            .clickable { onUseGlobalLayout() }
                            .testTag("tuner_use_global_layout_btn")
                    ) {
                        Text(
                            text = if (isEn) "Use Global Layout" else "Dùng bố cục chung",
                            color = if (hasPerGameCustomLayout) DirectCyan else TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1A2820),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, WarningAmber.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .semantics {
                                contentDescription = if (isEn) "Reset Layout" else "Đặt lại"
                            }
                            .clickable { showResetConfirmDialog = true }
                            .testTag("tuner_per_game_reset_btn")
                    ) {
                        Text(
                            text = if (isEn) "Reset" else "Đặt lại",
                            color = WarningAmber,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Quick Feature Toggles: Game Rumble, Auto-Sprint L3, Touch Haptics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                QuickFeatureChip(
                    modifier = Modifier.weight(1f),
                    label = if (isEn) "🎮 Rumble: ${gameRumbleLevel.localizedTitle(true)}" else "🎮 Rung game: ${gameRumbleLevel.localizedTitle(false)}",
                    active = gameRumbleLevel != GamepadHapticLevel.OFF,
                    onClick = {
                        val next = when (gameRumbleLevel) {
                            GamepadHapticLevel.OFF -> GamepadHapticLevel.LIGHT
                            GamepadHapticLevel.LIGHT -> GamepadHapticLevel.MEDIUM
                            GamepadHapticLevel.MEDIUM -> GamepadHapticLevel.STRONG
                            GamepadHapticLevel.STRONG -> GamepadHapticLevel.OFF
                        }
                        SettingsRepository.updateGameRumbleLevel(next)
                    }
                )
                QuickFeatureChip(
                    modifier = Modifier.weight(1f),
                    label = if (isEn) "🏃 Auto L3" else "🏃 Tự chạy L3",
                    active = sprintLockEnabled,
                    onClick = { SettingsRepository.updateGamepadSprintLockEnabled(!sprintLockEnabled) }
                )
                QuickFeatureChip(
                    modifier = Modifier.weight(1f),
                    label = if (isEn) "📳 Touch: ${hapticLevel.localizedTitle(true)}" else "📳 Rung nút: ${hapticLevel.localizedTitle(false)}",
                    active = hapticLevel != GamepadHapticLevel.OFF,
                    onClick = {
                        val next = when (hapticLevel) {
                            GamepadHapticLevel.OFF -> GamepadHapticLevel.LIGHT
                            GamepadHapticLevel.LIGHT -> GamepadHapticLevel.MEDIUM
                            GamepadHapticLevel.MEDIUM -> GamepadHapticLevel.STRONG
                            GamepadHapticLevel.STRONG -> GamepadHapticLevel.OFF
                        }
                        SettingsRepository.updateGamepadHapticLevel(next)
                    }
                )
            }

            // Compact Sliders: Global Size & Opacity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = if (isEn) "Global Size" else "Cỡ chung",
                            color = TextSecondary,
                            fontSize = 9.5.sp
                        )
                        Text(
                            text = "${(scale * 100).roundToInt()}%",
                            color = XboxNeonGreen,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = scale,
                        onValueChange = { SettingsRepository.updateGamepadScale(it) },
                        valueRange = 0.75f..1.35f,
                        modifier = Modifier.height(24.dp),
                        colors = SliderDefaults.colors(thumbColor = XboxNeonGreen, activeTrackColor = XboxNeonGreen)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = if (isEn) "Opacity" else "Độ đậm mờ",
                            color = TextSecondary,
                            fontSize = 9.5.sp
                        )
                        Text(
                            text = "${(opacity * 100).roundToInt()}%",
                            color = DirectCyan,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = opacity,
                        onValueChange = { SettingsRepository.updateGamepadOpacity(it) },
                        valueRange = 0.25f..0.95f,
                        modifier = Modifier.height(24.dp),
                        colors = SliderDefaults.colors(thumbColor = DirectCyan, activeTrackColor = DirectCyan)
                    )
                }
            }

            // Compact Sliders: Stick Sensitivity & Deadzone
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = if (isEn) "Stick Sens" else "Độ nhạy cần",
                            color = TextSecondary,
                            fontSize = 9.5.sp
                        )
                        Text(
                            text = "${(sensitivity * 100).roundToInt()}%",
                            color = XboxNeonGreen,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = sensitivity,
                        onValueChange = { SettingsRepository.updateGamepadSensitivity(it) },
                        valueRange = 0.60f..1.60f,
                        modifier = Modifier.height(24.dp),
                        colors = SliderDefaults.colors(thumbColor = XboxNeonGreen, activeTrackColor = XboxNeonGreen)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = if (isEn) "Deadzone" else "Vùng chết",
                            color = TextSecondary,
                            fontSize = 9.5.sp
                        )
                        Text(
                            text = "${(deadzone * 100).roundToInt()}%",
                            color = DirectCyan,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = deadzone,
                        onValueChange = { SettingsRepository.updateGamepadDeadzone(it) },
                        valueRange = 0.02f..0.25f,
                        modifier = Modifier.height(24.dp),
                        colors = SliderDefaults.colors(thumbColor = DirectCyan, activeTrackColor = DirectCyan)
                    )
                }
            }
        }
    }
}

@Composable
internal fun QuickFeatureChip(
    modifier: Modifier = Modifier,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (active) DirectCyan.copy(alpha = 0.20f) else Color(0xFF0E1813),
        border = androidx.compose.foundation.BorderStroke(
            0.8.dp,
            if (active) DirectCyan else XboxCardBorder
        )
    ) {
        Text(
            text = label,
            color = if (active) DirectCyan else TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}
