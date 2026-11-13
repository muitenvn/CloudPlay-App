package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppConfig
import com.example.data.GamepadHapticLevel
import com.example.data.GamepadLayoutPreset
import com.example.data.SettingsRepository
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.roundToInt

@Composable
internal fun GamepadNodePopover(
    nodeId: String,
    config: AppConfig,
    isEditPositionMode: Boolean,
    hasActiveGameKey: Boolean,
    hasPerGameCustomLayout: Boolean,
    gameDisplayName: String?,
    nodeOffset: Pair<Float, Float>,
    nodeScaleMult: Float,
    nodeOpacity: Float,
    nodeHaptic: GamepadHapticLevel,
    abxySplitMode: Boolean,
    dpadSplitMode: Boolean,
    onScaleChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onHapticChange: (GamepadHapticLevel) -> Unit,
    onNudge: (Float, Float) -> Unit,
    onHideNode: () -> Unit,
    onResetNode: () -> Unit,
    onToggleAbxySplit: () -> Unit,
    onToggleDpadSplit: () -> Unit,
    onEnterEditMode: () -> Unit,
    onUseGlobalLayout: () -> Unit,
    onShowAll: () -> Unit,
    onResetAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetAllConfirm by remember { mutableStateOf(false) }

    if (showResetAllConfirm) {
        AlertDialog(
            onDismissRequest = { showResetAllConfirm = false },
            containerColor = HudSurface2,
            shape = RoundedCornerShape(22.dp),
            titleContentColor = HudInk,
            textContentColor = HudDialogBody,
            title = {
                Text(
                    text = config.tr("Đặt lại bố cục mặc định?", "Reset Default Layout?"),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = config.tr(
                        "Khôi phục toàn bộ vị trí, cỡ, độ mờ, rung và các nút ẩn về mặc định?",
                        "Restore all button positions, sizes, opacity, haptics, and hidden buttons to default?"
                    ),
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetAllConfirm = false
                        onResetAll()
                    },
                    modifier = Modifier.testTag("tuner_confirm_reset_positions_btn")
                ) {
                    Text(
                        text = config.tr("Đặt lại", "Reset"),
                        color = HudDanger,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetAllConfirm = false },
                    modifier = Modifier.testTag("tuner_cancel_reset_positions_btn")
                ) {
                    Text(
                        text = config.tr("Hủy", "Cancel"),
                        color = HudMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }

    val isEnUi = config.isSettingsEnglishUI()
    val nodeInfo = remember(nodeId) { ALL_GAMEPAD_NODES.find { it.id == nodeId } }
    val title = when {
        nodeId == "all" -> config.tr("Cả tay cầm", "All Gamepad")
        nodeInfo != null -> config.tr(nodeInfo.labelVi, nodeInfo.labelEn)
        else -> nodeId
    }

    Surface(
        modifier = modifier
            .width(246.dp)
            .heightIn(max = 340.dp)
            .testTag(if (nodeId == "all") "virtual_gamepad_tuner_panel" else "virtual_gamepad_node_popover"),
        shape = RoundedCornerShape(18.dp),
        color = HudSurface2,
        border = BorderStroke(1.dp, HudLine),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: "✥ <Title>" + Close ✕
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✥ $title",
                    color = HudInk,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { onClose() }
                        .testTag("virtual_gamepad_popover_close"),
                    shape = RoundedCornerShape(9.dp),
                    color = HudSurface,
                    shadowElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✕", color = HudMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Scrollable popover body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (nodeId == "all") {
                    GlobalPopoverBody(
                        config = config,
                        isEnUi = isEnUi,
                        isEditPositionMode = isEditPositionMode,
                        hasActiveGameKey = hasActiveGameKey,
                        hasPerGameCustomLayout = hasPerGameCustomLayout,
                        gameDisplayName = gameDisplayName,
                        onEnterEditMode = onEnterEditMode,
                        onUseGlobalLayout = onUseGlobalLayout,
                        onShowAll = onShowAll,
                        onRequestResetAll = { showResetAllConfirm = true }
                    )
                } else {
                    SingleNodePopoverBody(
                        nodeId = nodeId,
                        config = config,
                        nodeOffset = nodeOffset,
                        nodeScaleMult = nodeScaleMult,
                        nodeOpacity = nodeOpacity,
                        nodeHaptic = nodeHaptic,
                        abxySplitMode = abxySplitMode,
                        dpadSplitMode = dpadSplitMode,
                        onScaleChange = onScaleChange,
                        onOpacityChange = onOpacityChange,
                        onHapticChange = onHapticChange,
                        onNudge = onNudge,
                        onToggleAbxySplit = onToggleAbxySplit,
                        onToggleDpadSplit = onToggleDpadSplit,
                        onHideNode = onHideNode,
                        onResetNode = onResetNode
                    )
                }
            }
        }
    }
}

@Composable
private fun GlobalPopoverBody(
    config: AppConfig,
    isEnUi: Boolean,
    isEditPositionMode: Boolean,
    hasActiveGameKey: Boolean,
    hasPerGameCustomLayout: Boolean,
    gameDisplayName: String?,
    onEnterEditMode: () -> Unit,
    onUseGlobalLayout: () -> Unit,
    onShowAll: () -> Unit,
    onRequestResetAll: () -> Unit
) {
    HudSectionLabel(text = config.tr("Bố cục", "Layout"))
    HudSegmentedControl(
        options = listOf(
            config.tr("Chung", "Global"),
            config.tr("Riêng cho game", "Per-game")
        ),
        selectedIndex = if (config.usePerGameGamepadLayout) 1 else 0,
        onSelect = { idx ->
            SettingsRepository.updateUsePerGameGamepadLayout(idx == 1)
        },
        bgColor = HudSurface,
        minButtonHeight = 30.dp,
        fontSize = 11.5.sp
    )

    if (config.usePerGameGamepadLayout) {
        if (hasActiveGameKey) {
            HudSurfaceRow(minHeight = 34.dp, bgColor = HudSurface) {
                Text(
                    text = gameDisplayName?.takeIf { it.isNotBlank() } ?: config.tr("Game hiện tại", "Current game"),
                    color = if (hasPerGameCustomLayout) HudAccent else HudMuted,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (hasPerGameCustomLayout) {
                    Spacer(modifier = Modifier.width(6.dp))
                    HudButton(
                        text = config.tr("Dùng chung", "Use Global"),
                        onClick = onUseGlobalLayout,
                        minHeight = 28.dp,
                        fontSize = 10.5.sp,
                        containerOverride = HudSurface2,
                        contentOverride = HudCyan,
                        modifier = Modifier.testTag("tuner_use_global_layout_btn")
                    )
                }
            }
        } else {
            Text(
                text = config.tr(
                    "Đang ở sảnh Xbox — vào một game cụ thể để lưu bố cục riêng.",
                    "In Xbox hub — launch a game to store a per-game layout."
                ),
                color = HudFaint,
                fontSize = 11.sp
            )
        }
    }

    HudSectionLabel(text = config.tr("Kiểu có sẵn", "Preset"))
    HudSegmentedControl(
        options = listOf("Xbox", "FPS", "Racing", "Compact"),
        selectedIndex = GamepadLayoutPreset.entries.indexOf(config.gamepadLayoutPreset).coerceAtLeast(0),
        onSelect = { idx ->
            GamepadLayoutPreset.entries.getOrNull(idx)?.let {
                SettingsRepository.updateGamepadLayoutPreset(it)
            }
        },
        bgColor = HudSurface,
        minButtonHeight = 30.dp,
        fontSize = 10.5.sp
    )

    HudSliderRow(
        label = config.tr("Cỡ chung", "Size"),
        valueText = "${(config.gamepadScale * 100).roundToInt()}%",
        value = config.gamepadScale,
        valueRange = 0.75f..1.35f,
        onValueChange = { SettingsRepository.updateGamepadScale(it) },
        bgColor = HudSurface
    )

    HudSliderRow(
        label = config.tr("Độ đậm mờ", "Opacity"),
        valueText = "${(config.gamepadOpacity * 100).roundToInt()}%",
        value = config.gamepadOpacity,
        valueRange = 0.25f..0.95f,
        onValueChange = { SettingsRepository.updateGamepadOpacity(it) },
        bgColor = HudSurface
    )

    val hapticLabels = remember(isEnUi, config.appLanguage) {
        GamepadHapticLevel.entries.map { config.tr(it.titleVi, it.titleEn) }
    }

    HudSectionLabel(text = config.tr("Rung nút mặc định", "Button Haptics"))
    HudSegmentedControl(
        options = hapticLabels,
        selectedIndex = GamepadHapticLevel.entries.indexOf(config.gamepadHapticLevel).coerceAtLeast(0),
        onSelect = { idx ->
            GamepadHapticLevel.entries.getOrNull(idx)?.let {
                SettingsRepository.updateGamepadHapticLevel(it)
            }
        },
        bgColor = HudSurface,
        minButtonHeight = 30.dp,
        fontSize = 11.sp
    )

    HudSectionLabel(text = config.tr("Rung từ game", "Game Rumble"))
    HudSegmentedControl(
        options = hapticLabels,
        selectedIndex = GamepadHapticLevel.entries.indexOf(config.gameRumbleLevel).coerceAtLeast(0),
        onSelect = { idx ->
            GamepadHapticLevel.entries.getOrNull(idx)?.let {
                SettingsRepository.updateGameRumbleLevel(it)
            }
        },
        bgColor = HudSurface,
        minButtonHeight = 30.dp,
        fontSize = 11.sp
    )

    HudSwitchRow(
        title = config.tr("Tự chạy nhanh (L3)", "Auto Sprint (L3)"),
        checked = config.gamepadSprintLockEnabled,
        onCheckedChange = { SettingsRepository.updateGamepadSprintLockEnabled(it) },
        bgColor = HudSurface
    )

    HudButton(
        text = if (isEditPositionMode) {
            config.tr("✓ Xong sửa nút", "✓ Done Editing")
        } else {
            config.tr("✥ Sửa vị trí từng nút", "✥ Edit Buttons")
        },
        onClick = onEnterEditMode,
        style = HudButtonStyle.MAIN,
        minHeight = 36.dp,
        fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth()
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        HudButton(
            text = config.tr("Hiện tất cả", "Show All"),
            onClick = onShowAll,
            minHeight = 34.dp,
            fontSize = 11.5.sp,
            containerOverride = HudSurface,
            modifier = Modifier.weight(1f)
        )
        HudButton(
            text = config.tr("Đặt lại tất cả", "Reset All"),
            onClick = onRequestResetAll,
            minHeight = 34.dp,
            fontSize = 11.5.sp,
            containerOverride = HudSurface,
            contentOverride = HudDanger,
            modifier = Modifier
                .weight(1f)
                .testTag("tuner_reset_default_layout_btn")
        )
    }
}

@Composable
private fun SingleNodePopoverBody(
    nodeId: String,
    config: AppConfig,
    nodeOffset: Pair<Float, Float>,
    nodeScaleMult: Float,
    nodeOpacity: Float,
    nodeHaptic: GamepadHapticLevel,
    abxySplitMode: Boolean,
    dpadSplitMode: Boolean,
    onScaleChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onHapticChange: (GamepadHapticLevel) -> Unit,
    onNudge: (Float, Float) -> Unit,
    onToggleAbxySplit: () -> Unit,
    onToggleDpadSplit: () -> Unit,
    onHideNode: () -> Unit,
    onResetNode: () -> Unit
) {
    // 1. Size Stepper ("Cỡ")
    HudSurfaceRow(minHeight = 34.dp, bgColor = HudSurface) {
        Text(
            text = config.tr("Cỡ", "Size"),
            color = HudInk,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        HudStepperControl(
            valueText = "${(nodeScaleMult * 100).roundToInt()}%",
            onDecrement = { onScaleChange(nodeScaleMult - 0.10f) },
            onIncrement = { onScaleChange(nodeScaleMult + 0.10f) },
            btnBg = HudSurface2,
            decTag = "virtual_gamepad_btn_scale_minus",
            incTag = "virtual_gamepad_btn_scale_plus"
        )
    }

    // 2. Per-button Opacity Slider ("Độ đậm mờ")
    HudSliderRow(
        label = config.tr("Độ đậm mờ", "Opacity"),
        valueText = "${(nodeOpacity * 100).roundToInt()}%",
        value = nodeOpacity,
        valueRange = 0.20f..1.00f,
        onValueChange = onOpacityChange,
        bgColor = HudSurface
    )

    // 3. Live Coordinates & Fine 2px Nudge ("Vị trí")
    val coordX = nodeOffset.first.roundToInt()
    val coordY = nodeOffset.second.roundToInt()
    val xSign = if (coordX > 0) "+$coordX" else "$coordX"
    val ySign = if (coordY > 0) "+$coordY" else "$coordY"
    HudSurfaceRow(minHeight = 34.dp, bgColor = HudSurface) {
        Text(
            text = "X:$xSign Y:$ySign",
            color = HudMuted,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag("virtual_gamepad_live_coords")
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(
                Triple("◀", -2f, 0f),
                Triple("▲", 0f, -2f),
                Triple("▼", 0f, 2f),
                Triple("▶", 2f, 0f)
            ).forEach { (sym, dx, dy) ->
                Surface(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .clickable { onNudge(dx, dy) },
                    shape = RoundedCornerShape(7.dp),
                    color = HudSurface2,
                    shadowElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(sym, color = HudInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    val isStick = nodeId == "left_stick" || nodeId == "right_stick"
    if (isStick) {
        val isLeft = nodeId == "left_stick"
        val innerDz = if (isLeft) config.leftStickInnerDeadzone else config.rightStickInnerDeadzone
        val outerDz = if (isLeft) config.leftStickOuterDeadzone else config.rightStickOuterDeadzone

        HudSliderRow(
            label = config.tr("Độ nhạy", "Sensitivity"),
            valueText = "${(config.gamepadSensitivity * 100).roundToInt()}%",
            value = config.gamepadSensitivity,
            valueRange = 0.60f..1.60f,
            onValueChange = { SettingsRepository.updateGamepadSensitivity(it) },
            bgColor = HudSurface
        )

        HudSliderRow(
            label = config.tr("Vùng chết trong", "Inner Deadzone"),
            valueText = "${(innerDz * 100).roundToInt()}%",
            value = innerDz,
            valueRange = 0.00f..0.30f,
            onValueChange = { next ->
                if (isLeft) {
                    SettingsRepository.updateVirtualStickDeadzones(leftInner = next)
                } else {
                    SettingsRepository.updateVirtualStickDeadzones(rightInner = next)
                }
            },
            bgColor = HudSurface
        )

        HudSliderRow(
            label = config.tr("Vùng chết ngoài", "Outer Deadzone"),
            valueText = "${(outerDz * 100).roundToInt()}%",
            value = outerDz,
            valueRange = 0.70f..1.00f,
            onValueChange = { next ->
                if (isLeft) {
                    SettingsRepository.updateVirtualStickDeadzones(leftOuter = next)
                } else {
                    SettingsRepository.updateVirtualStickDeadzones(rightOuter = next)
                }
            },
            bgColor = HudSurface
        )

        if (isLeft) {
            HudSwitchRow(
                title = config.tr("Tự chạy nhanh (L3)", "Auto Sprint (L3)"),
                checked = config.gamepadSprintLockEnabled,
                onCheckedChange = { SettingsRepository.updateGamepadSprintLockEnabled(it) },
                bgColor = HudSurface
            )
        }
    } else {
        // Button / Cluster Haptics ("Rung khi bấm")
        val hapticLabels = remember(config.appLanguage) {
            GamepadHapticLevel.entries.map { config.tr(it.titleVi, it.titleEn) }
        }
        HudSectionLabel(text = config.tr("Rung khi bấm", "Haptic on Press"))
        HudSegmentedControl(
            options = hapticLabels,
            selectedIndex = GamepadHapticLevel.entries.indexOf(nodeHaptic).coerceAtLeast(0),
            onSelect = { idx ->
                GamepadHapticLevel.entries.getOrNull(idx)?.let(onHapticChange)
            },
            bgColor = HudSurface,
            minButtonHeight = 30.dp,
            fontSize = 11.sp
        )

        if (nodeId == "abxy" || nodeId in setOf("btn_a", "btn_b", "btn_x", "btn_y")) {
            HudButton(
                text = if (abxySplitMode) {
                    config.tr("⇄ Gộp ABXY", "⇄ Merge ABXY")
                } else {
                    config.tr("⇄ Tách A/B/X/Y", "⇄ Split A/B/X/Y")
                },
                onClick = onToggleAbxySplit,
                minHeight = 34.dp,
                fontSize = 12.sp,
                containerOverride = HudSurface,
                contentOverride = if (abxySplitMode) HudAccent else HudInk,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("virtual_gamepad_split_abxy_toggle")
            )
        }

        if (nodeId == "dpad" || nodeId in setOf("dpad_up", "dpad_down", "dpad_left", "dpad_right")) {
            HudButton(
                text = if (dpadSplitMode) {
                    config.tr("⇄ Gộp D‑Pad", "⇄ Merge D-Pad")
                } else {
                    config.tr("⇄ Tách D‑Pad", "⇄ Split D-Pad")
                },
                onClick = onToggleDpadSplit,
                minHeight = 34.dp,
                fontSize = 12.sp,
                containerOverride = HudSurface,
                contentOverride = if (dpadSplitMode) HudAccent else HudInk,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("virtual_gamepad_split_dpad_toggle")
            )
        }
    }

    // Footer: "Ẩn nút" & "↺ Nút này"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        HudButton(
            text = config.tr("Ẩn nút", "Hide"),
            onClick = onHideNode,
            minHeight = 34.dp,
            fontSize = 11.5.sp,
            containerOverride = HudSurface,
            modifier = Modifier
                .weight(1f)
                .testTag("virtual_gamepad_delete_button")
        )
        HudButton(
            text = config.tr("↺ Nút này", "↺ Reset"),
            onClick = onResetNode,
            minHeight = 34.dp,
            fontSize = 11.5.sp,
            containerOverride = HudSurface,
            modifier = Modifier
                .weight(1f)
                .testTag("virtual_gamepad_reset_single_button")
        )
    }
}
