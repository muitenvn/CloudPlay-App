package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.example.data.GamepadButtonPressMode
import com.example.data.GamepadHapticLevel
import com.example.data.GamepadLayoutPreset
import com.example.data.GamepadStickCurve
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
    nodeBehavior: GamepadButtonBehavior,
    customButtonDef: CustomGamepadButtonDef?,
    abxySplitMode: Boolean,
    dpadSplitMode: Boolean,
    onScaleChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onHapticChange: (GamepadHapticLevel) -> Unit,
    onBehaviorChange: (GamepadButtonBehavior) -> Unit,
    onCustomButtonDefChange: (CustomGamepadButtonDef) -> Unit,
    onNudge: (Float, Float) -> Unit,
    onHideOrDeleteNode: () -> Unit,
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
                        "Khôi phục toàn bộ vị trí, cỡ, độ mờ, kiểu bấm, nút trống và các nút ẩn về mặc định?",
                        "Restore all button positions, sizes, opacity, press modes, custom buttons, and hidden buttons to default?"
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
        customButtonDef != null -> {
            val badge = customButtonDef.resolvedBadge(isEnUi)
            config.tr("Nút tùy chỉnh ($badge)", "Custom Button ($badge)")
        }
        nodeInfo != null -> config.tr(nodeInfo.labelVi, nodeInfo.labelEn)
        else -> nodeId
    }

    Surface(
        modifier = modifier
            .width(256.dp)
            .heightIn(max = 356.dp)
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
                        nodeInfo = nodeInfo,
                        nodeOffset = nodeOffset,
                        nodeScaleMult = nodeScaleMult,
                        nodeOpacity = nodeOpacity,
                        nodeHaptic = nodeHaptic,
                        nodeBehavior = nodeBehavior,
                        customButtonDef = customButtonDef,
                        abxySplitMode = abxySplitMode,
                        dpadSplitMode = dpadSplitMode,
                        onScaleChange = onScaleChange,
                        onOpacityChange = onOpacityChange,
                        onHapticChange = onHapticChange,
                        onBehaviorChange = onBehaviorChange,
                        onCustomButtonDefChange = onCustomButtonDefChange,
                        onNudge = onNudge,
                        onToggleAbxySplit = onToggleAbxySplit,
                        onToggleDpadSplit = onToggleDpadSplit,
                        onHideOrDeleteNode = onHideOrDeleteNode,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SingleNodePopoverBody(
    nodeId: String,
    config: AppConfig,
    nodeInfo: GamepadEditableNodeInfo?,
    nodeOffset: Pair<Float, Float>,
    nodeScaleMult: Float,
    nodeOpacity: Float,
    nodeHaptic: GamepadHapticLevel,
    nodeBehavior: GamepadButtonBehavior,
    customButtonDef: CustomGamepadButtonDef?,
    abxySplitMode: Boolean,
    dpadSplitMode: Boolean,
    onScaleChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onHapticChange: (GamepadHapticLevel) -> Unit,
    onBehaviorChange: (GamepadButtonBehavior) -> Unit,
    onCustomButtonDefChange: (CustomGamepadButtonDef) -> Unit,
    onNudge: (Float, Float) -> Unit,
    onToggleAbxySplit: () -> Unit,
    onToggleDpadSplit: () -> Unit,
    onHideOrDeleteNode: () -> Unit,
    onResetNode: () -> Unit
) {
    val isStick = nodeId == "left_stick" || nodeId == "right_stick"
    val isCluster = nodeId == "abxy" || nodeId == "dpad"

    var showComboSelector by remember(nodeId) { mutableStateOf(nodeBehavior.extraButtons.isNotEmpty()) }
    var showCustomDimensionMenu by remember(nodeId) { mutableStateOf(nodeBehavior.hasCustomDimensions()) }

    // 1. Uniform Size Stepper ("Cỡ")
    HudSurfaceRow(minHeight = 34.dp, bgColor = HudSurface) {
        Text(
            text = config.tr("Cỡ chung nút", "Size"),
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

    // 1b. Dedicated Sub-Menu for Free Width & Height ("Tùy chỉnh bề ngang · bề dọc")
    if (!isStick && !isCluster) {
        val wPct = (nodeBehavior.widthScale * 100).roundToInt()
        val hPct = (nodeBehavior.heightScale * 100).roundToInt()
        val hasCustomDim = nodeBehavior.hasCustomDimensions()
        val arrowPrefix = if (showCustomDimensionMenu) "▾" else "▸"
        val dimSubTitle = if (hasCustomDim) {
            config.tr(
                "$arrowPrefix Chỉnh bề ngang · dọc ($wPct% × $hPct%)",
                "$arrowPrefix Width · Height ($wPct% × $hPct%)"
            )
        } else {
            config.tr(
                "$arrowPrefix Tùy chỉnh bề ngang · bề dọc",
                "$arrowPrefix Custom Width & Height"
            )
        }

        HudButton(
            text = dimSubTitle,
            onClick = { showCustomDimensionMenu = !showCustomDimensionMenu },
            minHeight = 32.dp,
            fontSize = 11.5.sp,
            containerOverride = if (showCustomDimensionMenu || hasCustomDim) HudSelBg else HudSurface,
            contentOverride = if (showCustomDimensionMenu || hasCustomDim) HudAccent else HudMuted,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("virtual_gamepad_custom_dimensions_toggle")
        )

        if (showCustomDimensionMenu) {
            HudSliderRow(
                label = config.tr("Bề ngang", "Width"),
                valueText = "$wPct%",
                value = nodeBehavior.widthScale,
                valueRange = 0.50f..2.50f,
                onValueChange = { nextW ->
                    onBehaviorChange(nodeBehavior.copy(widthScale = nextW.coerceIn(0.50f, 2.50f)))
                },
                bgColor = HudSurface
            )

            HudSliderRow(
                label = config.tr("Bề dọc", "Height"),
                valueText = "$hPct%",
                value = nodeBehavior.heightScale,
                valueRange = 0.50f..2.50f,
                onValueChange = { nextH ->
                    onBehaviorChange(nodeBehavior.copy(heightScale = nextH.coerceIn(0.50f, 2.50f)))
                },
                bgColor = HudSurface
            )

            if (hasCustomDim) {
                HudButton(
                    text = config.tr("↺ Đặt lại ngang · dọc (100%)", "↺ Reset Width · Height"),
                    onClick = {
                        onBehaviorChange(nodeBehavior.copy(widthScale = 1.0f, heightScale = 1.0f))
                    },
                    minHeight = 28.dp,
                    fontSize = 11.sp,
                    containerOverride = HudSurface,
                    contentOverride = HudCyan,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
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

    if (isStick) {
        val isLeft = nodeId == "left_stick"
        val stickSens = if (isLeft) config.leftStickSensitivity else config.rightStickSensitivity
        val stickCurve = if (isLeft) config.leftStickCurve else config.rightStickCurve
        val innerDz = if (isLeft) config.leftStickInnerDeadzone else config.rightStickInnerDeadzone
        val outerDz = if (isLeft) config.leftStickOuterDeadzone else config.rightStickOuterDeadzone

        HudSliderRow(
            label = if (isLeft) {
                config.tr("Độ nhạy cần trái", "Left Stick Sens")
            } else {
                config.tr("Độ nhạy cần phải", "Right Stick Sens")
            },
            valueText = "${(stickSens * 100).roundToInt()}%",
            value = stickSens,
            valueRange = 0.60f..1.60f,
            onValueChange = { next ->
                if (isLeft) {
                    SettingsRepository.updateLeftStickSensitivity(next)
                } else {
                    SettingsRepository.updateRightStickSensitivity(next)
                }
            },
            bgColor = HudSurface
        )

        val curveOptions = remember(config.appLanguage) {
            GamepadStickCurve.entries.map { config.tr(it.titleVi, it.titleEn) }
        }
        HudSectionLabel(text = config.tr("Cảm giác phản hồi", "Response Curve"))
        HudSegmentedControl(
            options = curveOptions,
            selectedIndex = GamepadStickCurve.entries.indexOf(stickCurve).coerceAtLeast(0),
            onSelect = { idx ->
                GamepadStickCurve.entries.getOrNull(idx)?.let { nextCurve ->
                    if (isLeft) {
                        SettingsRepository.updateLeftStickCurve(nextCurve)
                    } else {
                        SettingsRepository.updateRightStickCurve(nextCurve)
                    }
                }
            },
            bgColor = HudSurface,
            minButtonHeight = 30.dp,
            fontSize = 11.sp
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
                subtitle = config.tr("Vuốt vượt đỉnh cần để bật L3", "Swipe past top rim to sprint"),
                checked = config.gamepadSprintLockEnabled,
                onCheckedChange = { SettingsRepository.updateGamepadSprintLockEnabled(it) },
                bgColor = HudSurface
            )
        }
    } else {
        // Custom Blank Button shape & target Xbox buttons selector
        if (customButtonDef != null) {
            val shapeSelectedIdx = when {
                showCustomDimensionMenu || nodeBehavior.hasCustomDimensions() -> 2
                customButtonDef.isRound -> 0
                else -> 1
            }
            HudSectionLabel(text = config.tr("Hình dáng nút", "Button Shape"))
            HudSegmentedControl(
                options = listOf(
                    config.tr("Tròn", "Round"),
                    config.tr("Viên thuốc", "Pill"),
                    config.tr("Tự chỉnh", "Custom")
                ),
                selectedIndex = shapeSelectedIdx,
                onSelect = { idx ->
                    when (idx) {
                        0 -> {
                            showCustomDimensionMenu = false
                            onCustomButtonDefChange(customButtonDef.copy(isRound = true))
                            if (nodeBehavior.hasCustomDimensions()) {
                                onBehaviorChange(nodeBehavior.copy(widthScale = 1.0f, heightScale = 1.0f))
                            }
                        }
                        1 -> {
                            showCustomDimensionMenu = false
                            onCustomButtonDefChange(customButtonDef.copy(isRound = false))
                            if (nodeBehavior.hasCustomDimensions()) {
                                onBehaviorChange(nodeBehavior.copy(widthScale = 1.0f, heightScale = 1.0f))
                            }
                        }
                        2 -> {
                            showCustomDimensionMenu = true
                        }
                    }
                },
                bgColor = HudSurface,
                minButtonHeight = 30.dp,
                fontSize = 11.sp
            )

            HudSectionLabel(
                text = config.tr(
                    "Gán nút Xbox (chọn 1 hoặc nhiều):",
                    "Assign Xbox Buttons (1 or multiple):"
                )
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                XBOX_TARGET_BUTTON_CHOICES.forEach { choice ->
                    val selected = choice.index in customButtonDef.targetButtons
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) HudSelBg else HudSurface,
                        border = BorderStroke(1.dp, if (selected) HudAccent else HudLine),
                        modifier = Modifier.clickable {
                            val nextSet = customButtonDef.targetButtons.toMutableSet()
                            if (selected) nextSet.remove(choice.index) else nextSet.add(choice.index)
                            onCustomButtonDefChange(customButtonDef.copy(targetButtons = nextSet))
                        },
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = choice.badge,
                            color = if (selected) HudAccent else HudMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Single-button Press Mode ("Bấm thả" / "Chạm khóa" / "Nhấp nhanh")
        if (!isCluster) {
            val pressModeLabels = remember(config.appLanguage) {
                GamepadButtonPressMode.entries.map { config.tr(it.titleVi, it.titleEn) }
            }
            HudSectionLabel(text = config.tr("Cách bấm", "Press Mode"))
            HudSegmentedControl(
                options = pressModeLabels,
                selectedIndex = GamepadButtonPressMode.entries.indexOf(nodeBehavior.pressMode).coerceAtLeast(0),
                onSelect = { idx ->
                    GamepadButtonPressMode.entries.getOrNull(idx)?.let { mode ->
                        onBehaviorChange(nodeBehavior.copy(pressMode = mode))
                    }
                },
                bgColor = HudSurface,
                minButtonHeight = 30.dp,
                fontSize = 11.sp
            )

            if (nodeBehavior.pressMode == GamepadButtonPressMode.TURBO) {
                HudSliderRow(
                    label = config.tr("Tốc độ nhấp", "Turbo Speed"),
                    valueText = "${nodeBehavior.turboHz} /s",
                    value = nodeBehavior.turboHz.toFloat(),
                    valueRange = 5f..20f,
                    onValueChange = { onBehaviorChange(nodeBehavior.copy(turboHz = it.roundToInt().coerceIn(5, 20))) },
                    bgColor = HudSurface
                )
            }

            // Individual Analog Trigger Swipe toggle for LT / RT
            if (nodeId == "btn_lt" || nodeId == "btn_rt") {
                val effectiveAnalog = nodeBehavior.analogSwipe
                    ?: (config.gamepadLayoutPreset == GamepadLayoutPreset.RACING)
                HudSwitchRow(
                    title = config.tr("Vuốt chỉnh lực cò", "Analog Trigger Swipe"),
                    subtitle = config.tr("Vuốt xuống để rà ga/phanh 0–100%", "Swipe down for 0–100% throttle/brake"),
                    checked = effectiveAnalog,
                    onCheckedChange = { enabled ->
                        onBehaviorChange(nodeBehavior.copy(analogSwipe = enabled))
                    },
                    bgColor = HudSurface
                )
            }

            // Optional Combo Button Picker for standard single buttons
            if (customButtonDef == null && nodeInfo?.primaryButtonIndex != null) {
                val primaryIdx = nodeInfo.primaryButtonIndex
                val comboCount = nodeBehavior.extraButtons.size
                HudButton(
                    text = if (comboCount > 0) {
                        val names = nodeBehavior.extraButtons.sorted().mapNotNull { idx ->
                            XBOX_TARGET_BUTTON_CHOICES.find { it.index == idx }?.badge
                        }.joinToString("+")
                        config.tr("＋ Bấm kèm: $names", "＋ Combo: $names")
                    } else {
                        config.tr("＋ Gán nút bấm kèm (tổ hợp)", "＋ Add Combo Button")
                    },
                    onClick = { showComboSelector = !showComboSelector },
                    minHeight = 32.dp,
                    fontSize = 11.5.sp,
                    containerOverride = if (comboCount > 0) HudSelBg else HudSurface,
                    contentOverride = if (comboCount > 0) HudAccent else HudMuted,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showComboSelector) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        XBOX_TARGET_BUTTON_CHOICES.filter { it.index != primaryIdx }.forEach { choice ->
                            val selected = choice.index in nodeBehavior.extraButtons
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) HudSelBg else HudSurface,
                                border = BorderStroke(1.dp, if (selected) HudAccent else HudLine),
                                modifier = Modifier.clickable {
                                    val nextExtras = nodeBehavior.extraButtons.toMutableSet()
                                    if (selected) nextExtras.remove(choice.index) else nextExtras.add(choice.index)
                                    onBehaviorChange(nodeBehavior.copy(extraButtons = nextExtras))
                                },
                                shadowElevation = 0.dp
                            ) {
                                Text(
                                    text = choice.badge,
                                    color = if (selected) HudAccent else HudMuted,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

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

    // Footer: "Ẩn nút" (or "Xóa nút" for custom blank button) & "↺ Nút này"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        HudButton(
            text = if (customButtonDef != null) {
                config.tr("Xóa nút này", "Delete")
            } else {
                config.tr("Ẩn nút", "Hide")
            },
            onClick = onHideOrDeleteNode,
            minHeight = 34.dp,
            fontSize = 11.5.sp,
            containerOverride = HudSurface,
            contentOverride = if (customButtonDef != null) HudDanger else HudInk,
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
