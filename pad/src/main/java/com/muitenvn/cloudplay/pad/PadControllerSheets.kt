package com.muitenvn.cloudplay.pad

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muitenvn.cloudplay.protocol.RemotePadButtons
import kotlin.math.roundToInt

private val SheetBg = Color(0xFF0D1511)
private val SheetCard = Color(0xFF142019)
private val SheetSelected = Color(0xFF16352A)
private val SheetBorder = Color(0xFF284235)
private val SheetAccent = Color(0xFF27D884)
private val SheetCyan = Color(0xFF3FD8EE)
private val SheetAmber = Color(0xFFE3B341)
private val SheetDanger = Color(0xFFF0706B)
private val SheetDangerBg = Color(0xFF2D1517)
private val SheetText = Color(0xFFEEF6F1)
private val SheetMuted = Color(0xFF9EB4A7)

internal data class PadStandardNodeMeta(
    val key: String,
    val shortBadge: String,
    val titleVi: String,
    val titleEn: String
)

internal val ALL_PAD_STANDARD_NODES: List<PadStandardNodeMeta> = listOf(
    PadStandardNodeMeta("btn_lt", "LT", "Cò trái LT", "Left Trigger LT"),
    PadStandardNodeMeta("btn_lb", "LB", "Nút vai LB", "Left Bumper LB"),
    PadStandardNodeMeta("btn_l3", "L3", "Ấn cần L3", "Left Stick Click L3"),
    PadStandardNodeMeta("btn_r3", "R3", "Ấn cần R3", "Right Stick Click R3"),
    PadStandardNodeMeta("btn_rb", "RB", "Nút vai RB", "Right Bumper RB"),
    PadStandardNodeMeta("btn_rt", "RT", "Cò phải RT", "Right Trigger RT"),
    PadStandardNodeMeta("btn_view", "View", "Nút ⧉ View", "View Button"),
    PadStandardNodeMeta("btn_xbox", "Xbox", "Nút Xbox Guide", "Xbox Guide"),
    PadStandardNodeMeta("btn_menu", "Menu", "Nút ☰ Menu", "Menu Button"),
    PadStandardNodeMeta("stick_left", "Cần L", "Cần Analog Trái (L)", "Left Analog Stick"),
    PadStandardNodeMeta("stick_right", "Cần R", "Cần Analog Phải (R)", "Right Analog Stick"),
    PadStandardNodeMeta("dpad", "D-Pad", "Cụm D-Pad", "D-Pad Cluster"),
    PadStandardNodeMeta("dpad_up", "D-Pad ▲", "D-Pad ▲ Lên", "D-Pad ▲ Up"),
    PadStandardNodeMeta("dpad_down", "D-Pad ▼", "D-Pad ▼ Xuống", "D-Pad ▼ Down"),
    PadStandardNodeMeta("dpad_left", "D-Pad ◀", "D-Pad ◀ Trái", "D-Pad ◀ Left"),
    PadStandardNodeMeta("dpad_right", "D-Pad ▶", "D-Pad ▶ Phải", "D-Pad ▶ Right"),
    PadStandardNodeMeta("abxy", "ABXY", "Cụm ABXY", "ABXY Cluster"),
    PadStandardNodeMeta("btn_a", "Nút A", "Nút A", "Button A"),
    PadStandardNodeMeta("btn_b", "Nút B", "Nút B", "Button B"),
    PadStandardNodeMeta("btn_x", "Nút X", "Nút X", "Button X"),
    PadStandardNodeMeta("btn_y", "Nút Y", "Nút Y", "Button Y")
)

/**
 * Thanh công cụ gọn nhẹ ở đỉnh màn hình khi bật Chế độ sửa vị trí nút (đồng bộ 100% với VirtualGamepad của app chính).
 * Không che giữa màn hình — chỉ khi chạm 2 lần vào 1 nút mới mở Popover nổi cạnh nút đó.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PadCompactEditToolbar(
    lang: PadLanguage,
    hiddenCount: Int,
    showAddButtonModal: Boolean,
    showEditGrid: Boolean,
    isPositionLocked: Boolean,
    onToggleAddButtonModal: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleLock: (Boolean) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .widthIn(max = 430.dp)
            .testTag("pad_customize_toolbar"),
        shape = RoundedCornerShape(16.dp),
        color = SheetBg.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, SheetBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = PadStrings.tr(
                    lang,
                    "Kéo để di chuyển · chạm 2 lần để chỉnh riêng từng nút",
                    "Drag to move · double-tap any button to customize"
                ),
                color = SheetMuted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PadPopoverActionBtn(
                    text = if (hiddenCount > 0) {
                        PadStrings.tr(lang, "＋ Thêm nút ($hiddenCount)", "＋ Add ($hiddenCount)")
                    } else {
                        PadStrings.tr(lang, "＋ Thêm nút", "＋ Buttons")
                    },
                    onClick = onToggleAddButtonModal,
                    minHeight = 32.dp,
                    bgColor = if (showAddButtonModal) SheetSelected else SheetCard,
                    textColor = if (showAddButtonModal) SheetAccent else SheetText,
                    borderColor = if (showAddButtonModal) SheetAccent else SheetBorder,
                    testTag = "pad_toolbar_add_btn"
                )

                PadPopoverActionBtn(
                    text = PadStrings.tr(lang, "# Lưới", "# Grid"),
                    onClick = onToggleGrid,
                    minHeight = 32.dp,
                    bgColor = if (showEditGrid) SheetSelected else SheetCard,
                    textColor = if (showEditGrid) SheetAccent else SheetMuted,
                    borderColor = if (showEditGrid) SheetAccent else SheetBorder,
                    testTag = "virtual_gamepad_grid_toggle"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isPositionLocked) SheetSelected else SheetCard)
                        .border(
                            1.dp,
                            if (isPositionLocked) SheetAccent else SheetBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onToggleLock(!isPositionLocked) }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("pad_lock_position_btn")
                ) {
                    Text(
                        text = if (isPositionLocked) {
                            PadStrings.tr(lang, "🔒 Khóa vị trí", "🔒 Locked")
                        } else {
                            PadStrings.tr(lang, "Khóa vị trí", "Lock")
                        },
                        color = if (isPositionLocked) SheetAccent else SheetText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                PadPopoverActionBtn(
                    text = PadStrings.tr(lang, "✓ Xong", "✓ Done"),
                    onClick = onDone,
                    minHeight = 32.dp,
                    isMain = true,
                    testTag = "pad_customize_done_btn"
                )
            }
        }
    }
}

/**
 * Popover nổi nhỏ gọn (260.dp) neo ngay cạnh nút vừa chạm 2 lần hoặc ngay dưới nút ⚙ (nodeKey == "all").
 * Đồng bộ toàn bộ tính năng tùy chỉnh theo từng nút từ VirtualGamepadPopovers.kt của app chính.
 */
@Composable
internal fun PadNodePopover(
    nodeKey: String,
    lang: PadLanguage,
    tvName: String,
    pingMs: Int?,
    activeGameTitle: String,
    hasActiveGameKey: Boolean,
    hasPerGameCustomLayout: Boolean,
    isEditPositionMode: Boolean,
    currentPreset: PadLayoutPreset,
    globalSettings: PadGlobalControllerSettings,
    nodeTuning: PadNodeTuning,
    customButtonSpec: PadCustomButtonSpec?,
    screenW: Float,
    screenH: Float,
    onSelectPreset: (PadLayoutPreset) -> Unit,
    onUpdateGlobalSettings: (PadGlobalControllerSettings) -> Unit,
    onUpdateNodeTuning: (PadNodeTuning) -> Unit,
    onUpdateCustomButtonSpec: (PadCustomButtonSpec) -> Unit,
    onNudgePx: (Float, Float) -> Unit,
    onHideOrDeleteNode: () -> Unit,
    onResetSingleNode: () -> Unit,
    onToggleAbxySplit: () -> Unit,
    onToggleDpadSplit: () -> Unit,
    onEnterOrExitEditMode: () -> Unit,
    onUseGlobalLayout: () -> Unit,
    onShowAllNodes: () -> Unit,
    onResetAllNodes: () -> Unit,
    onRecenterGyro: () -> Unit,
    onOpenFullSettingsSheet: () -> Unit,
    onDisconnect: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetAllConfirm by remember { mutableStateOf(false) }

    if (showResetAllConfirm) {
        AlertDialog(
            onDismissRequest = { showResetAllConfirm = false },
            containerColor = SheetCard,
            shape = RoundedCornerShape(20.dp),
            titleContentColor = SheetText,
            textContentColor = SheetMuted,
            title = {
                Text(
                    text = PadStrings.tr(lang, "Đặt lại bố cục mặc định?", "Reset Default Layout?"),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = PadStrings.tr(
                        lang,
                        "Khôi phục toàn bộ vị trí, cỡ, kiểu bấm, nút trống và các nút ẩn về mặc định?",
                        "Restore all button positions, sizes, press modes, custom buttons, and hidden buttons to default?"
                    ),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetAllConfirm = false
                        onResetAllNodes()
                    },
                    modifier = Modifier.testTag("tuner_confirm_reset_positions_btn")
                ) {
                    Text(
                        text = PadStrings.tr(lang, "Đặt lại", "Reset"),
                        color = SheetDanger,
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
                        text = PadStrings.tr(lang, "Hủy", "Cancel"),
                        color = SheetMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }

    val stdMeta = remember(nodeKey) { ALL_PAD_STANDARD_NODES.find { it.key == nodeKey } }
    val title = when {
        nodeKey == "all" -> PadStrings.tr(lang, "Cả tay cầm", "All Gamepad")
        customButtonSpec != null -> {
            val badge = formatMaskBadge(customButtonSpec.assignedMask).ifBlank { customButtonSpec.label }
            PadStrings.tr(lang, "Nút tùy chỉnh ($badge)", "Custom Button ($badge)")
        }
        stdMeta != null -> PadStrings.tr(lang, stdMeta.titleVi, stdMeta.titleEn)
        else -> friendlyNodeTitle(nodeKey, lang, emptyList())
    }

    Surface(
        modifier = modifier
            .width(262.dp)
            .heightIn(max = 356.dp)
            .testTag(if (nodeKey == "all") "virtual_gamepad_tuner_panel" else "virtual_gamepad_node_popover"),
        shape = RoundedCornerShape(18.dp),
        color = SheetCard,
        border = BorderStroke(1.dp, SheetBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: "✥ <Tên nút>" + Nút đóng ✕
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✥ $title",
                    color = SheetText,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onClose() }
                        .testTag("virtual_gamepad_popover_close"),
                    shape = RoundedCornerShape(8.dp),
                    color = SheetBg,
                    shadowElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✕", color = SheetMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Nội dung cuộn của Popover
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (nodeKey == "all") {
                    PadGlobalPopoverBody(
                        lang = lang,
                        tvName = tvName,
                        pingMs = pingMs,
                        activeGameTitle = activeGameTitle,
                        hasActiveGameKey = hasActiveGameKey,
                        hasPerGameCustomLayout = hasPerGameCustomLayout,
                        isEditPositionMode = isEditPositionMode,
                        currentPreset = currentPreset,
                        settings = globalSettings,
                        onSelectPreset = onSelectPreset,
                        onUpdateSettings = onUpdateGlobalSettings,
                        onEnterOrExitEditMode = onEnterOrExitEditMode,
                        onUseGlobalLayout = onUseGlobalLayout,
                        onShowAllNodes = onShowAllNodes,
                        onRequestResetAll = { showResetAllConfirm = true },
                        onRecenterGyro = onRecenterGyro,
                        onOpenFullSettingsSheet = onOpenFullSettingsSheet,
                        onDisconnect = onDisconnect
                    )
                } else {
                    PadSingleNodePopoverBody(
                        nodeKey = nodeKey,
                        lang = lang,
                        currentPreset = currentPreset,
                        globalSettings = globalSettings,
                        nodeTuning = nodeTuning,
                        customButtonSpec = customButtonSpec,
                        screenW = screenW,
                        screenH = screenH,
                        onUpdateGlobalSettings = onUpdateGlobalSettings,
                        onUpdateNodeTuning = onUpdateNodeTuning,
                        onUpdateCustomButtonSpec = onUpdateCustomButtonSpec,
                        onNudgePx = onNudgePx,
                        onToggleAbxySplit = onToggleAbxySplit,
                        onToggleDpadSplit = onToggleDpadSplit,
                        onHideOrDeleteNode = onHideOrDeleteNode,
                        onResetSingleNode = onResetSingleNode
                    )
                }
            }
        }
    }
}

@Composable
private fun PadGlobalPopoverBody(
    lang: PadLanguage,
    tvName: String,
    pingMs: Int?,
    activeGameTitle: String,
    hasActiveGameKey: Boolean,
    hasPerGameCustomLayout: Boolean,
    isEditPositionMode: Boolean,
    currentPreset: PadLayoutPreset,
    settings: PadGlobalControllerSettings,
    onSelectPreset: (PadLayoutPreset) -> Unit,
    onUpdateSettings: (PadGlobalControllerSettings) -> Unit,
    onEnterOrExitEditMode: () -> Unit,
    onUseGlobalLayout: () -> Unit,
    onShowAllNodes: () -> Unit,
    onRequestResetAll: () -> Unit,
    onRecenterGyro: () -> Unit,
    onOpenFullSettingsSheet: () -> Unit,
    onDisconnect: () -> Unit
) {
    // Hàng trạng thái TV gọn + Ngắt kết nối
    PadPopoverSurfaceRow {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(SheetAccent)
            )
            val pingStr = pingMs?.let { "$it ms" } ?: "-- ms"
            Text(
                text = "$tvName · $pingStr",
                color = SheetText,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PadPopoverActionBtn(
                text = PadStrings.tr(lang, "Đầy đủ", "Full"),
                onClick = onOpenFullSettingsSheet,
                minHeight = 26.dp,
                fontSize = 10.5.sp,
                bgColor = SheetCard,
                textColor = SheetCyan,
                testTag = "pad_open_full_settings_from_pop"
            )
            PadPopoverActionBtn(
                text = PadStrings.tr(lang, "Ngắt", "Leave"),
                onClick = onDisconnect,
                minHeight = 26.dp,
                fontSize = 10.5.sp,
                bgColor = SheetCard,
                textColor = SheetDanger,
                borderColor = SheetDanger.copy(alpha = 0.6f),
                testTag = "pad_controller_disconnect_btn"
            )
        }
    }

    PadPopoverSectionLabel(PadStrings.tr(lang, "Bố cục", "Layout"))
    PadPopoverSegmentedControl(
        options = listOf(
            PadStrings.tr(lang, "Chung", "Global"),
            PadStrings.tr(lang, "Riêng cho game", "Per-game")
        ),
        selectedIndex = if (settings.layoutScope == PadLayoutScope.PER_GAME) 1 else 0,
        onSelect = { idx ->
            val scope = if (idx == 1) PadLayoutScope.PER_GAME else PadLayoutScope.GLOBAL
            onUpdateSettings(settings.copy(layoutScope = scope))
        }
    )

    if (settings.layoutScope == PadLayoutScope.PER_GAME) {
        if (hasActiveGameKey) {
            PadPopoverSurfaceRow {
                Text(
                    text = activeGameTitle.takeIf { it.isNotBlank() } ?: PadStrings.tr(lang, "Game hiện tại", "Current game"),
                    color = if (hasPerGameCustomLayout) SheetAccent else SheetMuted,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (hasPerGameCustomLayout) {
                    Spacer(modifier = Modifier.width(6.dp))
                    PadPopoverActionBtn(
                        text = PadStrings.tr(lang, "Dùng chung", "Use Global"),
                        onClick = onUseGlobalLayout,
                        minHeight = 26.dp,
                        fontSize = 10.5.sp,
                        bgColor = SheetCard,
                        textColor = SheetCyan,
                        testTag = "tuner_use_global_layout_btn"
                    )
                }
            }
        } else {
            Text(
                text = PadStrings.tr(
                    lang,
                    "Đang ở sảnh TV — vào một game cụ thể để lưu bố cục riêng.",
                    "In TV hub — launch a game to store a per-game layout."
                ),
                color = SheetMuted,
                fontSize = 11.sp
            )
        }
    }

    PadPopoverSectionLabel(PadStrings.tr(lang, "Kiểu có sẵn", "Preset"))
    PadPopoverSegmentedControl(
        options = listOf("Xbox", "FPS", "Action", "Racing", "Swap"),
        selectedIndex = PadLayoutPreset.entries.indexOf(currentPreset).coerceAtLeast(0),
        onSelect = { idx ->
            PadLayoutPreset.entries.getOrNull(idx)?.let(onSelectPreset)
        }
    )

    PadPopoverSliderRow(
        label = PadStrings.tr(lang, "Cỡ chung", "Global Size"),
        valueText = "${(settings.globalScale * 100).roundToInt()}%",
        value = settings.globalScale,
        valueRange = 0.60f..1.65f,
        onValueChange = { onUpdateSettings(settings.copy(globalScale = it.coerceIn(0.60f, 1.65f))) }
    )

    val hapticLabels = remember(lang) {
        PadHapticLevel.entries.map { it.localizedTitle(lang) }
    }
    PadPopoverSectionLabel(PadStrings.tr(lang, "Rung nút mặc định", "Button Haptics"))
    PadPopoverSegmentedControl(
        options = hapticLabels,
        selectedIndex = PadHapticLevel.entries.indexOf(settings.buttonHapticLevel).coerceAtLeast(0),
        onSelect = { idx ->
            PadHapticLevel.entries.getOrNull(idx)?.let {
                onUpdateSettings(settings.copy(buttonHapticLevel = it))
            }
        }
    )

    PadPopoverSectionLabel(PadStrings.tr(lang, "Rung từ game", "Game Rumble"))
    PadPopoverSegmentedControl(
        options = hapticLabels,
        selectedIndex = PadHapticLevel.entries.indexOf(settings.gameRumbleLevel).coerceAtLeast(0),
        onSelect = { idx ->
            PadHapticLevel.entries.getOrNull(idx)?.let {
                onUpdateSettings(settings.copy(gameRumbleLevel = it))
            }
        }
    )

    val effectiveAutoSprint = when (settings.autoSprintL3Override) {
        0 -> false
        1 -> true
        else -> currentPreset.autoSprintL3
    }
    PadPopoverSwitchRow(
        title = PadStrings.tr(lang, "Tự chạy nhanh (L3)", "Auto Sprint (L3)"),
        checked = effectiveAutoSprint,
        onCheckedChange = { enabled ->
            onUpdateSettings(settings.copy(autoSprintL3Override = if (enabled) 1 else 0))
        }
    )

    // Con quay hồi chuyển (Gyroscope - Mặc định TẮT)
    PadPopoverSwitchRow(
        title = PadStrings.tr(lang, "Con quay hồi chuyển", "Gyroscope"),
        subtitle = PadStrings.tr(
            lang,
            "Mặc định tắt · Bật để nghiêng máy xoay tâm / lái xe",
            "Default off · Tilt phone for aiming or steering"
        ),
        checked = settings.gyroEnabled,
        onCheckedChange = { enabled ->
            onUpdateSettings(settings.copy(gyroEnabled = enabled))
            if (enabled) onRecenterGyro()
        }
    )

    if (settings.gyroEnabled) {
        PadPopoverSegmentedControl(
            options = listOf(
                PadStrings.tr(lang, "Bắn súng/VR", "FPS/VR"),
                PadStrings.tr(lang, "Đua xe", "Racing"),
                PadStrings.tr(lang, "Cần trái", "L-Stick")
            ),
            selectedIndex = PadGyroMode.entries.indexOf(settings.gyroMode).coerceAtLeast(0),
            onSelect = { idx ->
                PadGyroMode.entries.getOrNull(idx)?.let { mode ->
                    onUpdateSettings(settings.copy(gyroMode = mode))
                    onRecenterGyro()
                }
            }
        )

        PadPopoverSliderRow(
            label = if (settings.gyroMode == PadGyroMode.RACING_STEER) {
                PadStrings.tr(lang, "Độ nhạy lái", "Steer Sens")
            } else {
                PadStrings.tr(lang, "Độ nhạy ngang", "Horiz Sens")
            },
            valueText = "${settings.gyroSensX}%",
            value = settings.gyroSensX / 100f,
            valueRange = 0.10f..3.00f,
            onValueChange = {
                onUpdateSettings(settings.copy(gyroSensX = (it * 100).roundToInt().coerceIn(10, 300)))
            }
        )

        if (settings.gyroMode != PadGyroMode.RACING_STEER) {
            PadPopoverSliderRow(
                label = PadStrings.tr(lang, "Độ nhạy dọc", "Vert Sens"),
                valueText = "${settings.gyroSensY}%",
                value = settings.gyroSensY / 100f,
                valueRange = 0.10f..3.00f,
                onValueChange = {
                    onUpdateSettings(settings.copy(gyroSensY = (it * 100).roundToInt().coerceIn(10, 300)))
                }
            )
        }

        PadPopoverActionBtn(
            text = PadStrings.tr(lang, "🎯 Đặt lại tâm con quay (0°)", "🎯 Recenter Gyro (0°)"),
            onClick = onRecenterGyro,
            minHeight = 32.dp,
            bgColor = SheetBg,
            textColor = SheetCyan,
            modifier = Modifier.fillMaxWidth(),
            testTag = "pad_gyro_recenter_btn"
        )
    }

    PadPopoverActionBtn(
        text = if (isEditPositionMode) {
            PadStrings.tr(lang, "✓ Xong sửa nút", "✓ Done Editing")
        } else {
            PadStrings.tr(lang, "✥ Sửa vị trí từng nút", "✥ Edit Buttons")
        },
        onClick = onEnterOrExitEditMode,
        minHeight = 36.dp,
        isMain = true,
        modifier = Modifier.fillMaxWidth(),
        testTag = "pad_open_edit_mode_btn"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PadPopoverActionBtn(
            text = PadStrings.tr(lang, "Hiện tất cả", "Show All"),
            onClick = onShowAllNodes,
            minHeight = 34.dp,
            bgColor = SheetBg,
            modifier = Modifier.weight(1f),
            testTag = "pad_show_all_hidden_btn"
        )
        PadPopoverActionBtn(
            text = PadStrings.tr(lang, "Đặt lại tất cả", "Reset All"),
            onClick = onRequestResetAll,
            minHeight = 34.dp,
            bgColor = SheetBg,
            textColor = SheetDanger,
            modifier = Modifier.weight(1f),
            testTag = "tuner_reset_default_layout_btn"
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PadSingleNodePopoverBody(
    nodeKey: String,
    lang: PadLanguage,
    currentPreset: PadLayoutPreset,
    globalSettings: PadGlobalControllerSettings,
    nodeTuning: PadNodeTuning,
    customButtonSpec: PadCustomButtonSpec?,
    screenW: Float,
    screenH: Float,
    onUpdateGlobalSettings: (PadGlobalControllerSettings) -> Unit,
    onUpdateNodeTuning: (PadNodeTuning) -> Unit,
    onUpdateCustomButtonSpec: (PadCustomButtonSpec) -> Unit,
    onNudgePx: (Float, Float) -> Unit,
    onToggleAbxySplit: () -> Unit,
    onToggleDpadSplit: () -> Unit,
    onHideOrDeleteNode: () -> Unit,
    onResetSingleNode: () -> Unit
) {
    val isStick = nodeKey == "stick_left" || nodeKey == "stick_right"
    val isCluster = nodeKey == "abxy" || nodeKey == "dpad"

    var showComboSelector by remember(nodeKey) { mutableStateOf(nodeTuning.comboMask != 0) }
    var showCustomDimensionMenu by remember(nodeKey) {
        mutableStateOf(
            nodeTuning.hasCustomDimensions() ||
                (customButtonSpec != null && customButtonSpec.shape == PadCustomBtnShape.CUSTOM_RECT)
        )
    }

    // 1. Cỡ chung nút ("-" / "+")
    PadPopoverSurfaceRow {
        Text(
            text = PadStrings.tr(lang, "Cỡ chung nút", "Size"),
            color = SheetText,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        onUpdateNodeTuning(
                            nodeTuning.copy(scaleMult = (nodeTuning.scaleMult - 0.10f).coerceIn(0.55f, 1.75f))
                        )
                    }
                    .testTag("pad_customize_scale_down_btn"),
                shape = RoundedCornerShape(8.dp),
                color = SheetCard,
                shadowElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("−", color = SheetText, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Text(
                text = "${(nodeTuning.scaleMult * 100).roundToInt()}%",
                color = SheetAccent,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Surface(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        onUpdateNodeTuning(
                            nodeTuning.copy(scaleMult = (nodeTuning.scaleMult + 0.10f).coerceIn(0.55f, 1.75f))
                        )
                    }
                    .testTag("pad_customize_scale_up_btn"),
                shape = RoundedCornerShape(8.dp),
                color = SheetCard,
                shadowElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("+", color = SheetText, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }

    // 1b. Menu thu gọn chỉnh Bề ngang · Bề dọc (giống hệt app chính)
    if (!isStick && !isCluster) {
        val wPct = (nodeTuning.widthMult * 100).roundToInt()
        val hPct = (nodeTuning.heightMult * 100).roundToInt()
        val hasCustomDim = nodeTuning.hasCustomDimensions()
        val arrowPrefix = if (showCustomDimensionMenu) "▾" else "▸"
        val dimTitle = if (hasCustomDim) {
            PadStrings.tr(
                lang,
                "$arrowPrefix Chỉnh bề ngang · dọc ($wPct% × $hPct%)",
                "$arrowPrefix Width · Height ($wPct% × $hPct%)"
            )
        } else {
            PadStrings.tr(
                lang,
                "$arrowPrefix Tùy chỉnh bề ngang · bề dọc",
                "$arrowPrefix Custom Width & Height"
            )
        }

        PadPopoverActionBtn(
            text = dimTitle,
            onClick = { showCustomDimensionMenu = !showCustomDimensionMenu },
            minHeight = 32.dp,
            bgColor = if (showCustomDimensionMenu || hasCustomDim) SheetSelected else SheetBg,
            textColor = if (showCustomDimensionMenu || hasCustomDim) SheetAccent else SheetMuted,
            borderColor = if (showCustomDimensionMenu || hasCustomDim) SheetAccent else SheetBorder,
            modifier = Modifier.fillMaxWidth(),
            testTag = "virtual_gamepad_custom_dimensions_toggle"
        )

        if (showCustomDimensionMenu) {
            PadPopoverSliderRow(
                label = PadStrings.tr(lang, "Bề ngang", "Width"),
                valueText = "$wPct%",
                value = nodeTuning.widthMult,
                valueRange = 0.50f..2.50f,
                onValueChange = { nextW ->
                    onUpdateNodeTuning(nodeTuning.copy(widthMult = nextW.coerceIn(0.50f, 2.50f)))
                }
            )
            PadPopoverSliderRow(
                label = PadStrings.tr(lang, "Bề dọc", "Height"),
                valueText = "$hPct%",
                value = nodeTuning.heightMult,
                valueRange = 0.50f..2.50f,
                onValueChange = { nextH ->
                    onUpdateNodeTuning(nodeTuning.copy(heightMult = nextH.coerceIn(0.50f, 2.50f)))
                }
            )
            if (hasCustomDim) {
                PadPopoverActionBtn(
                    text = PadStrings.tr(lang, "↺ Đặt lại ngang · dọc (100%)", "↺ Reset Width · Height"),
                    onClick = {
                        onUpdateNodeTuning(nodeTuning.copy(widthMult = 1.0f, heightMult = 1.0f))
                    },
                    minHeight = 28.dp,
                    fontSize = 11.sp,
                    bgColor = SheetBg,
                    textColor = SheetCyan,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "pad_reset_dimensions_btn"
                )
            }
        }
    }

    // 2. Tọa độ trực tiếp & Dịch vi chỉnh 2px (◀ ▲ ▼ ▶)
    val coordX = (nodeTuning.dxNorm * screenW).roundToInt()
    val coordY = (nodeTuning.dyNorm * screenH).roundToInt()
    val xSign = if (coordX > 0) "+$coordX" else "$coordX"
    val ySign = if (coordY > 0) "+$coordY" else "$coordY"

    PadPopoverSurfaceRow {
        Text(
            text = "X:$xSign Y:$ySign",
            color = SheetMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag("virtual_gamepad_live_coords")
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(
                Triple("◀", -2f to 0f, "pad_nudge_left_btn"),
                Triple("▲", 0f to -2f, "pad_nudge_up_btn"),
                Triple("▼", 0f to 2f, "pad_nudge_down_btn"),
                Triple("▶", 2f to 0f, "pad_nudge_right_btn")
            ).forEach { (sym, delta, tag) ->
                Surface(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .clickable { onNudgePx(delta.first, delta.second) }
                        .testTag(tag),
                    shape = RoundedCornerShape(7.dp),
                    color = SheetCard,
                    shadowElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(sym, color = SheetText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (isStick) {
        // Cần Analog Trái / Phải: Độ nhạy, Đường cong phản hồi, Vùng chết trong/ngoài, Tự chạy nhanh L3
        val isLeft = nodeKey == "stick_left"
        val stickSens = if (isLeft) globalSettings.leftStickSens else globalSettings.rightStickSens
        val stickCurve = if (isLeft) globalSettings.leftStickCurve else globalSettings.rightStickCurve
        val innerDz = if (isLeft) globalSettings.innerDeadzone else globalSettings.rightInnerDeadzone
        val outerDz = if (isLeft) globalSettings.outerDeadzone else globalSettings.rightOuterDeadzone

        PadPopoverSliderRow(
            label = if (isLeft) {
                PadStrings.tr(lang, "Độ nhạy cần trái", "Left Stick Sens")
            } else {
                PadStrings.tr(lang, "Độ nhạy cần phải", "Right Stick Sens")
            },
            valueText = "${(stickSens * 100).roundToInt()}%",
            value = stickSens,
            valueRange = 0.60f..1.60f,
            onValueChange = { next ->
                if (isLeft) {
                    onUpdateGlobalSettings(globalSettings.copy(leftStickSens = next.coerceIn(0.60f, 1.60f)))
                } else {
                    onUpdateGlobalSettings(globalSettings.copy(rightStickSens = next.coerceIn(0.60f, 1.60f)))
                }
            }
        )

        val curveOptions = remember(lang) {
            PadStickCurve.entries.map { it.localizedTitle(lang) }
        }
        PadPopoverSectionLabel(PadStrings.tr(lang, "Cảm giác phản hồi", "Response Curve"))
        PadPopoverSegmentedControl(
            options = curveOptions,
            selectedIndex = PadStickCurve.entries.indexOf(stickCurve).coerceAtLeast(0),
            onSelect = { idx ->
                PadStickCurve.entries.getOrNull(idx)?.let { nextCurve ->
                    if (isLeft) {
                        onUpdateGlobalSettings(globalSettings.copy(leftStickCurve = nextCurve))
                    } else {
                        onUpdateGlobalSettings(globalSettings.copy(rightStickCurve = nextCurve))
                    }
                }
            }
        )

        PadPopoverSliderRow(
            label = PadStrings.tr(lang, "Vùng chết trong", "Inner Deadzone"),
            valueText = "${(innerDz * 100).roundToInt()}%",
            value = innerDz,
            valueRange = 0.00f..0.30f,
            onValueChange = { next ->
                val clamped = next.coerceIn(0.00f, 0.30f)
                if (isLeft) {
                    onUpdateGlobalSettings(globalSettings.copy(innerDeadzone = clamped))
                } else {
                    onUpdateGlobalSettings(globalSettings.copy(rightInnerDeadzone = clamped))
                }
            }
        )

        PadPopoverSliderRow(
            label = PadStrings.tr(lang, "Vùng chết ngoài", "Outer Deadzone"),
            valueText = "${(outerDz * 100).roundToInt()}%",
            value = outerDz,
            valueRange = 0.70f..1.00f,
            onValueChange = { next ->
                val clamped = next.coerceIn(0.70f, 1.00f)
                if (isLeft) {
                    onUpdateGlobalSettings(globalSettings.copy(outerDeadzone = clamped))
                } else {
                    onUpdateGlobalSettings(globalSettings.copy(rightOuterDeadzone = clamped))
                }
            }
        )

        if (isLeft) {
            val effectiveAutoSprint = when (globalSettings.autoSprintL3Override) {
                0 -> false
                1 -> true
                else -> currentPreset.autoSprintL3
            }
            PadPopoverSwitchRow(
                title = PadStrings.tr(lang, "Tự chạy nhanh (L3)", "Auto Sprint (L3)"),
                subtitle = PadStrings.tr(lang, "Vuốt vượt đỉnh cần để bật L3", "Swipe past top rim to sprint"),
                checked = effectiveAutoSprint,
                onCheckedChange = { enabled ->
                    onUpdateGlobalSettings(globalSettings.copy(autoSprintL3Override = if (enabled) 1 else 0))
                }
            )
        }
    } else {
        // Nút trống tùy chỉnh: Hình dáng nút & Gán 1 hoặc nhiều nút Xbox
        if (customButtonSpec != null) {
            val shapeSelectedIdx = when {
                showCustomDimensionMenu || nodeTuning.hasCustomDimensions() || customButtonSpec.shape == PadCustomBtnShape.CUSTOM_RECT -> 2
                customButtonSpec.shape == PadCustomBtnShape.ROUND -> 0
                else -> 1
            }
            PadPopoverSectionLabel(PadStrings.tr(lang, "Hình dáng nút", "Button Shape"))
            PadPopoverSegmentedControl(
                options = listOf(
                    PadStrings.tr(lang, "Tròn", "Round"),
                    PadStrings.tr(lang, "Viên thuốc", "Pill"),
                    PadStrings.tr(lang, "Tự chỉnh", "Custom")
                ),
                selectedIndex = shapeSelectedIdx,
                onSelect = { idx ->
                    when (idx) {
                        0 -> {
                            showCustomDimensionMenu = false
                            onUpdateCustomButtonSpec(customButtonSpec.copy(shape = PadCustomBtnShape.ROUND))
                            if (nodeTuning.hasCustomDimensions()) {
                                onUpdateNodeTuning(nodeTuning.copy(widthMult = 1.0f, heightMult = 1.0f))
                            }
                        }
                        1 -> {
                            showCustomDimensionMenu = false
                            onUpdateCustomButtonSpec(customButtonSpec.copy(shape = PadCustomBtnShape.PILL))
                            if (nodeTuning.hasCustomDimensions()) {
                                onUpdateNodeTuning(nodeTuning.copy(widthMult = 1.0f, heightMult = 1.0f))
                            }
                        }
                        2 -> {
                            showCustomDimensionMenu = true
                            onUpdateCustomButtonSpec(customButtonSpec.copy(shape = PadCustomBtnShape.CUSTOM_RECT))
                        }
                    }
                }
            )

            PadPopoverSectionLabel(
                PadStrings.tr(
                    lang,
                    "Gán nút Xbox (chọn 1 hoặc nhiều):",
                    "Assign Xbox Buttons (1 or multiple):"
                )
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                AllPadAssignableButtons.forEach { opt ->
                    val selected = (customButtonSpec.assignedMask and opt.bit) != 0
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) SheetSelected else SheetBg,
                        border = BorderStroke(1.dp, if (selected) SheetAccent else SheetBorder),
                        modifier = Modifier
                            .clickable {
                                val nextMask = if (selected) {
                                    customButtonSpec.assignedMask and opt.bit.inv()
                                } else {
                                    customButtonSpec.assignedMask or opt.bit
                                }
                                val nextBadge = formatMaskBadge(nextMask).ifBlank {
                                    PadStrings.tr(lang, "Trống", "Blank")
                                }
                                onUpdateCustomButtonSpec(
                                    customButtonSpec.copy(
                                        assignedMask = nextMask,
                                        label = nextBadge.take(10)
                                    )
                                )
                            }
                            .testTag("pad_custom_assign_${opt.shortLabel}"),
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = opt.shortLabel,
                            color = if (selected) SheetAccent else SheetMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Cách bấm (Bấm thả / Chạm khóa / Nhấp nhanh) cho mọi nút đơn
        if (!isCluster) {
            val pressModeLabels = remember(lang) {
                listOf(
                    PadStrings.tr(lang, "Bấm thả", "Normal"),
                    PadStrings.tr(lang, "Chạm khóa", "Toggle"),
                    PadStrings.tr(lang, "Nhấp nhanh", "Turbo")
                )
            }
            PadPopoverSectionLabel(PadStrings.tr(lang, "Cách bấm", "Press Mode"))
            PadPopoverSegmentedControl(
                options = pressModeLabels,
                selectedIndex = PadPressMode.entries.indexOf(nodeTuning.pressMode).coerceAtLeast(0),
                onSelect = { idx ->
                    PadPressMode.entries.getOrNull(idx)?.let { mode ->
                        onUpdateNodeTuning(nodeTuning.copy(pressMode = mode))
                    }
                }
            )

            if (nodeTuning.pressMode == PadPressMode.TURBO) {
                PadPopoverSliderRow(
                    label = PadStrings.tr(lang, "Tốc độ nhấp", "Turbo Speed"),
                    valueText = "${nodeTuning.turboHz} /s",
                    value = nodeTuning.turboHz.toFloat(),
                    valueRange = 5f..20f,
                    onValueChange = {
                        onUpdateNodeTuning(nodeTuning.copy(turboHz = it.roundToInt().coerceIn(5, 20)))
                    }
                )
            }

            // Riêng cho Cò LT / RT: Vuốt chỉnh lực cò (0–100%)
            if (nodeKey == "btn_lt" || nodeKey == "btn_rt") {
                val effectiveAnalog = when (nodeTuning.analogTriggerDrag) {
                    1 -> true
                    0 -> false
                    else -> currentPreset == PadLayoutPreset.RACING
                }
                PadPopoverSwitchRow(
                    title = PadStrings.tr(lang, "Vuốt chỉnh lực cò", "Analog Trigger Swipe"),
                    subtitle = PadStrings.tr(
                        lang,
                        "Vuốt xuống để rà ga/phanh 0–100%",
                        "Swipe down for 0–100% throttle/brake"
                    ),
                    checked = effectiveAnalog,
                    onCheckedChange = { enabled ->
                        onUpdateNodeTuning(nodeTuning.copy(analogTriggerDrag = if (enabled) 1 else 0))
                    }
                )
            }

            // Riêng cho Nút vai LB / RB: Vuốt theo hướng (↑ Lên, ↓ Xuống, ← Trái, → Phải) + Gửi kèm nút vai
            if (nodeKey == "btn_lb" || nodeKey == "btn_rb") {
                val primaryBit = primaryBitForNodeKey(nodeKey)
                PadPopoverSwitchRow(
                    title = PadStrings.tr(lang, "Vuốt theo hướng", "Directional Swipe"),
                    subtitle = PadStrings.tr(
                        lang,
                        "Vuốt lên/xuống/trái/phải trên nút vai để bấm nhanh nút khác",
                        "Swipe up/down/left/right on bumper to trigger another button"
                    ),
                    checked = nodeTuning.directionalSwipeEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateNodeTuning(nodeTuning.copy(directionalSwipeEnabled = enabled))
                    }
                )

                if (nodeTuning.directionalSwipeEnabled) {
                    var activeDirPicker by remember(nodeKey) { mutableStateOf<String?>(null) }
                    val directionRows = listOf(
                        Triple("up", PadStrings.tr(lang, "↑ Lên", "↑ Up"), nodeTuning.swipeUpBit),
                        Triple("down", PadStrings.tr(lang, "↓ Xuống", "↓ Down"), nodeTuning.swipeDownBit),
                        Triple("left", PadStrings.tr(lang, "← Trái", "← Left"), nodeTuning.swipeLeftBit),
                        Triple("right", PadStrings.tr(lang, "→ Phải", "→ Right"), nodeTuning.swipeRightBit)
                    )

                    directionRows.forEach { (dirKey, dirLabel, currentBit) ->
                        val currentBadge = AllPadAssignableButtons.firstOrNull { it.bit == currentBit }?.shortLabel
                            ?: PadStrings.tr(lang, "Không", "None")
                        val isExpanded = activeDirPicker == dirKey

                        PadPopoverSurfaceRow(
                            bgColor = if (isExpanded) SheetSelected else SheetBg,
                            onClick = { activeDirPicker = if (isExpanded) null else dirKey }
                        ) {
                            Text(
                                text = dirLabel,
                                color = SheetText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = if (currentBit != 0) SheetSelected else SheetCard,
                                border = BorderStroke(
                                    1.dp,
                                    if (currentBit != 0) SheetAccent else SheetBorder
                                ),
                                shadowElevation = 0.dp
                            ) {
                                Text(
                                    text = if (isExpanded) "$currentBadge ▾" else "$currentBadge ▸",
                                    color = if (currentBit != 0) SheetAccent else SheetMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (isExpanded) {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                val noneSelected = currentBit == 0
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (noneSelected) SheetSelected else SheetBg,
                                    border = BorderStroke(1.dp, if (noneSelected) SheetAccent else SheetBorder),
                                    modifier = Modifier.clickable {
                                        val updated = when (dirKey) {
                                            "up" -> nodeTuning.copy(swipeUpBit = 0)
                                            "down" -> nodeTuning.copy(swipeDownBit = 0)
                                            "left" -> nodeTuning.copy(swipeLeftBit = 0)
                                            else -> nodeTuning.copy(swipeRightBit = 0)
                                        }
                                        onUpdateNodeTuning(updated)
                                        activeDirPicker = null
                                    },
                                    shadowElevation = 0.dp
                                ) {
                                    Text(
                                        text = PadStrings.tr(lang, "Không", "None"),
                                        color = if (noneSelected) SheetAccent else SheetMuted,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                    )
                                }

                                AllPadAssignableButtons.filter { it.bit != primaryBit }.forEach { choice ->
                                    val selected = currentBit == choice.bit
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selected) SheetSelected else SheetBg,
                                        border = BorderStroke(1.dp, if (selected) SheetAccent else SheetBorder),
                                        modifier = Modifier.clickable {
                                            val updated = when (dirKey) {
                                                "up" -> nodeTuning.copy(swipeUpBit = choice.bit)
                                                "down" -> nodeTuning.copy(swipeDownBit = choice.bit)
                                                "left" -> nodeTuning.copy(swipeLeftBit = choice.bit)
                                                else -> nodeTuning.copy(swipeRightBit = choice.bit)
                                            }
                                            onUpdateNodeTuning(updated)
                                            activeDirPicker = null
                                        },
                                        shadowElevation = 0.dp
                                    ) {
                                        Text(
                                            text = choice.shortLabel,
                                            color = if (selected) SheetAccent else SheetMuted,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    PadPopoverSwitchRow(
                        title = PadStrings.tr(lang, "Gửi kèm nút vai", "Include Shoulder Button"),
                        subtitle = if (nodeTuning.swipeIncludeShoulder) {
                            PadStrings.tr(
                                lang,
                                "Giữ nút vai trong lúc vuốt (ra tổ hợp)",
                                "Hold shoulder button while swiping (combo)"
                            )
                        } else {
                            PadStrings.tr(
                                lang,
                                "Vuốt chỉ gửi nút hướng; chạm rồi nhấc mới gửi nút vai",
                                "Swipe sends direction only; tap & release sends bumper"
                            )
                        },
                        checked = nodeTuning.swipeIncludeShoulder,
                        onCheckedChange = { include ->
                            onUpdateNodeTuning(nodeTuning.copy(swipeIncludeShoulder = include))
                        }
                    )
                }
            }

            // Gán nút bấm kèm (Tổ hợp / Combo) cho các nút đơn tiêu chuẩn
            val primaryBit = primaryBitForNodeKey(nodeKey)
            if (customButtonSpec == null && primaryBit != 0) {
                val comboBadge = formatMaskBadge(nodeTuning.comboMask)
                val hasCombo = comboBadge.isNotEmpty()
                PadPopoverActionBtn(
                    text = if (hasCombo) {
                        PadStrings.tr(lang, "＋ Bấm kèm: $comboBadge", "＋ Combo: $comboBadge")
                    } else {
                        PadStrings.tr(lang, "＋ Gán nút bấm kèm (tổ hợp)", "＋ Add Combo Button")
                    },
                    onClick = { showComboSelector = !showComboSelector },
                    minHeight = 32.dp,
                    bgColor = if (hasCombo) SheetSelected else SheetBg,
                    textColor = if (hasCombo) SheetAccent else SheetMuted,
                    borderColor = if (hasCombo) SheetAccent else SheetBorder,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "pad_toggle_combo_selector_btn"
                )

                if (showComboSelector) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        AllPadAssignableButtons.filter { it.bit != primaryBit }.forEach { choice ->
                            val selected = (nodeTuning.comboMask and choice.bit) != 0
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) SheetSelected else SheetBg,
                                border = BorderStroke(1.dp, if (selected) SheetAccent else SheetBorder),
                                modifier = Modifier
                                    .clickable {
                                        val nextMask = if (selected) {
                                            nodeTuning.comboMask and choice.bit.inv()
                                        } else {
                                            nodeTuning.comboMask or choice.bit
                                        }
                                        onUpdateNodeTuning(nodeTuning.copy(comboMask = nextMask))
                                    }
                                    .testTag("pad_combo_opt_${choice.shortLabel}"),
                                shadowElevation = 0.dp
                            ) {
                                Text(
                                    text = choice.shortLabel,
                                    color = if (selected) SheetAccent else SheetMuted,
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

        // Rung khi bấm nút này
        val hapticLabels = remember(lang) {
            PadHapticLevel.entries.map { it.localizedTitle(lang) }
        }
        val effectiveNodeHaptic = if (nodeTuning.hapticOverride in 0..3) {
            PadHapticLevel.fromId(nodeTuning.hapticOverride)
        } else {
            globalSettings.buttonHapticLevel
        }
        PadPopoverSectionLabel(PadStrings.tr(lang, "Rung khi bấm", "Haptic on Press"))
        PadPopoverSegmentedControl(
            options = hapticLabels,
            selectedIndex = PadHapticLevel.entries.indexOf(effectiveNodeHaptic).coerceAtLeast(0),
            onSelect = { idx ->
                PadHapticLevel.entries.getOrNull(idx)?.let { lvl ->
                    onUpdateNodeTuning(nodeTuning.copy(hapticOverride = lvl.id))
                }
            }
        )

        if (nodeKey == "abxy" || nodeKey in setOf("btn_a", "btn_b", "btn_x", "btn_y")) {
            PadPopoverActionBtn(
                text = if (globalSettings.abxySplitMode) {
                    PadStrings.tr(lang, "⇄ Gộp ABXY", "⇄ Merge ABXY")
                } else {
                    PadStrings.tr(lang, "⇄ Tách A/B/X/Y", "⇄ Split A/B/X/Y")
                },
                onClick = onToggleAbxySplit,
                minHeight = 34.dp,
                bgColor = SheetBg,
                textColor = if (globalSettings.abxySplitMode) SheetAccent else SheetText,
                modifier = Modifier.fillMaxWidth(),
                testTag = "pad_customize_split_abxy_btn"
            )
        }

        if (nodeKey == "dpad" || nodeKey in setOf("dpad_up", "dpad_down", "dpad_left", "dpad_right")) {
            PadPopoverActionBtn(
                text = if (globalSettings.dpadSplitMode) {
                    PadStrings.tr(lang, "⇄ Gộp D‑Pad", "⇄ Merge D-Pad")
                } else {
                    PadStrings.tr(lang, "⇄ Tách D‑Pad", "⇄ Split D-Pad")
                },
                onClick = onToggleDpadSplit,
                minHeight = 34.dp,
                bgColor = SheetBg,
                textColor = if (globalSettings.dpadSplitMode) SheetAccent else SheetText,
                modifier = Modifier.fillMaxWidth(),
                testTag = "pad_customize_split_dpad_btn"
            )
        }
    }

    // Hàng nút cuối: "Ẩn nút" (hoặc "Xóa nút này" cho nút trống) & "↺ Nút này"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PadPopoverActionBtn(
            text = if (customButtonSpec != null) {
                PadStrings.tr(lang, "Xóa nút này", "Delete")
            } else {
                PadStrings.tr(lang, "Ẩn nút", "Hide")
            },
            onClick = onHideOrDeleteNode,
            minHeight = 34.dp,
            bgColor = SheetBg,
            textColor = if (customButtonSpec != null) SheetDanger else SheetText,
            modifier = Modifier.weight(1f),
            testTag = if (customButtonSpec != null) "pad_delete_custom_node_btn" else "pad_hide_single_node_btn"
        )
        PadPopoverActionBtn(
            text = PadStrings.tr(lang, "↺ Nút này", "↺ Reset"),
            onClick = onResetSingleNode,
            minHeight = 34.dp,
            bgColor = SheetBg,
            modifier = Modifier.weight(1f),
            testTag = "pad_reset_single_node_btn"
        )
    }
}

/**
 * Hộp thoại gọn nhẹ "＋ Thêm / Ẩn nút" đồng bộ với AddOrManageButtonsModal của app chính:
 * - Bấm 1 chạm "＋ Thêm nút trống tùy chỉnh (không giới hạn)" là tạo ngay nút trống và mở luôn Popover của nút đó.
 * - Chạm vào bất kỳ nút trống nào đã tạo để mở Popover chỉnh nút đó, hoặc chạm ✕ để xóa.
 * - Chạm vào nút mặc định để Ẩn (✓) hoặc Hiện lại (＋).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PadManageButtonsModal(
    lang: PadLanguage,
    dpadSplitMode: Boolean,
    abxySplitMode: Boolean,
    nodeTunings: Map<String, PadNodeTuning>,
    customButtons: List<PadCustomButtonSpec>,
    onToggleNodeHidden: (String) -> Unit,
    onShowAllNodes: () -> Unit,
    onAddBlankCustomButton: () -> Unit,
    onSelectCustomButton: (String) -> Unit,
    onDeleteCustomButton: (String) -> Unit,
    onClose: () -> Unit
) {
    val activeNodes = remember(dpadSplitMode, abxySplitMode) {
        ALL_PAD_STANDARD_NODES.filter { node ->
            when (node.key) {
                "abxy" -> !abxySplitMode
                "btn_a", "btn_b", "btn_x", "btn_y" -> abxySplitMode
                "dpad" -> !dpadSplitMode
                "dpad_up", "dpad_down", "dpad_left", "dpad_right" -> dpadSplitMode
                else -> true
            }
        }
    }
    val hiddenCount = activeNodes.count { nodeTunings[it.key]?.hidden == true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88050807))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose
            )
            .testTag("pad_manage_buttons_modal_backdrop"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .widthIn(max = 440.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .testTag("pad_manage_buttons_modal"),
            shape = RoundedCornerShape(22.dp),
            color = SheetCard,
            border = BorderStroke(1.dp, SheetBorder),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = PadStrings.tr(lang, "＋ Thêm / Ẩn nút", "＋ Add / Hide Buttons"),
                        color = SheetText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (hiddenCount > 0) {
                            PadPopoverActionBtn(
                                text = PadStrings.tr(lang, "Hiện tất cả", "Show All"),
                                onClick = onShowAllNodes,
                                minHeight = 30.dp,
                                bgColor = SheetSelected,
                                textColor = SheetAccent,
                                borderColor = SheetAccent,
                                testTag = "pad_modal_show_all_btn"
                            )
                        }
                        PadPopoverActionBtn(
                            text = "✕",
                            onClick = onClose,
                            minHeight = 30.dp,
                            bgColor = SheetBg,
                            textColor = SheetMuted,
                            testTag = "pad_manage_buttons_close_btn"
                        )
                    }
                }

                // Nút 1 chạm tạo nút trống tùy chỉnh (không giới hạn) giống hệt app chính
                PadPopoverActionBtn(
                    text = PadStrings.tr(
                        lang,
                        "＋ Thêm nút trống tùy chỉnh (không giới hạn)",
                        "＋ Add Blank Custom Button (Unlimited)"
                    ),
                    onClick = onAddBlankCustomButton,
                    minHeight = 38.dp,
                    fontSize = 12.5.sp,
                    isMain = true,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "pad_create_custom_btn_confirm"
                )

                if (customButtons.isNotEmpty()) {
                    PadPopoverSectionLabel(
                        PadStrings.tr(
                            lang,
                            "Nút trống đã tạo (${customButtons.size}) — chạm để chỉnh, ✕ để xóa:",
                            "Custom buttons (${customButtons.size}) — tap to edit, ✕ to delete:"
                        )
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        customButtons.forEach { item ->
                            val badge = formatMaskBadge(item.assignedMask).ifBlank { item.label }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SheetBg,
                                border = BorderStroke(1.dp, SheetCyan),
                                shadowElevation = 0.dp
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp)
                                ) {
                                    Text(
                                        text = "✥ $badge",
                                        color = SheetCyan,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { onSelectCustomButton(item.id) }
                                    )
                                    Text(
                                        text = "✕",
                                        color = SheetDanger,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier
                                            .clickable { onDeleteCustomButton(item.id) }
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = PadStrings.tr(
                        lang,
                        "Nút mặc định — chạm để hiện lại (＋) hoặc ẩn (✓):",
                        "Standard buttons — tap to show (＋) or hide (✓):"
                    ),
                    color = SheetMuted,
                    fontSize = 12.sp
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeNodes.forEach { node ->
                        val isHidden = nodeTunings[node.key]?.hidden == true
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isHidden) SheetDangerBg else SheetSelected,
                            border = BorderStroke(1.dp, if (isHidden) SheetDanger else SheetAccent),
                            modifier = Modifier
                                .clickable { onToggleNodeHidden(node.key) }
                                .testTag("pad_toggle_vis_${node.key}"),
                            shadowElevation = 0.dp
                        ) {
                            Text(
                                text = if (isHidden) "＋ ${node.shortBadge}" else "✓ ${node.shortBadge}",
                                color = if (isHidden) SheetDanger else SheetAccent,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PadGlobalSettingsSheet(
    lang: PadLanguage,
    tvName: String,
    tvHost: String,
    activeGameTitle: String,
    pingMs: Int?,
    currentPreset: PadLayoutPreset,
    settings: PadGlobalControllerSettings,
    hiddenNodeCount: Int,
    customButtonCount: Int,
    onSelectPreset: (PadLayoutPreset) -> Unit,
    onUpdateSettings: (PadGlobalControllerSettings) -> Unit,
    onOpenEditLayoutMode: () -> Unit,
    onOpenManageButtonsModal: () -> Unit,
    onShowAllHiddenNodes: () -> Unit,
    onResetCurrentLayout: () -> Unit,
    onTestVibration: () -> Unit,
    onRecenterGyro: () -> Unit = {},
    onDisconnect: () -> Unit,
    onForgetDevice: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB8050807))
            .clickable(onClick = onClose)
            .testTag("pad_global_settings_sheet_backdrop"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 760.dp)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(18.dp))
                .background(SheetBg)
                .border(1.5.dp, SheetBorder, RoundedCornerShape(18.dp))
                .clickable(enabled = false) {}
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .testTag("pad_global_settings_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = PadStrings.tr(lang, "⚙ Cài đặt tay cầm Ravno Pad", "⚙ Ravno Pad Controller Settings"),
                        color = SheetText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(SheetSelected)
                            .border(1.dp, SheetAccent.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        val pingLabel = pingMs?.let { "$it ms" } ?: "-- ms"
                        Text(
                            text = "$tvName · $pingLabel",
                            color = SheetAccent,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                SheetPillButton(
                    label = PadStrings.tr(lang, "✓ Đóng", "✓ Close"),
                    isPrimary = true,
                    onClick = onClose,
                    testTag = "pad_settings_close_btn"
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SheetSectionCard(
                    title = PadStrings.tr(lang, "1. MẪU BỐ CỤC & HIỂN THỊ CHUNG", "1. LAYOUT PRESET & GLOBAL DISPLAY")
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PadLayoutPreset.entries.forEach { preset ->
                            SheetChoiceChip(
                                label = preset.shortTitle(lang),
                                selected = currentPreset == preset,
                                onClick = { onSelectPreset(preset) },
                                testTag = "pad_preset_chip_${preset.id}"
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = PadStrings.tr(lang, "Phạm vi lưu bố cục", "Layout storage scope"),
                                color = SheetText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val gameDisplay = activeGameTitle.trim().takeIf {
                                it.isNotEmpty() && !it.equals("generic", ignoreCase = true)
                            } ?: PadStrings.tr(lang, "Mặc định", "Default")
                            Text(
                                text = PadStrings.tr(
                                    lang,
                                    "Game hiện tại trên TV: $gameDisplay",
                                    "Active TV game: $gameDisplay"
                                ),
                                color = SheetMuted,
                                fontSize = 11.sp
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PadLayoutScope.entries.forEach { scope ->
                                SheetChoiceChip(
                                    label = scope.localizedTitle(lang),
                                    selected = settings.layoutScope == scope,
                                    onClick = { onUpdateSettings(settings.copy(layoutScope = scope)) },
                                    testTag = "pad_scope_chip_${scope.id}"
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = PadStrings.tr(lang, "Kích thước chung", "Global scale"),
                                color = SheetText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${(settings.globalScale * 100).roundToInt()}%",
                                color = SheetAccent,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = settings.globalScale,
                            onValueChange = {
                                onUpdateSettings(settings.copy(globalScale = it.coerceIn(0.60f, 1.65f)))
                            },
                            valueRange = 0.60f..1.65f,
                            colors = SliderDefaults.colors(
                                thumbColor = SheetAccent,
                                activeTrackColor = SheetAccent,
                                inactiveTrackColor = SheetBorder
                            )
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SheetPillButton(
                            label = PadStrings.tr(
                                lang,
                                "✥ Sửa vị trí & chạm 2 lần chỉnh từng nút",
                                "✥ Edit Layout & Double-Tap Buttons"
                            ),
                            isPrimary = true,
                            onClick = onOpenEditLayoutMode,
                            testTag = "pad_open_edit_mode_btn"
                        )
                        SheetPillButton(
                            label = PadStrings.tr(
                                lang,
                                "＋ Thêm nút trống / Ẩn nút ($customButtonCount)",
                                "＋ Custom / Hide Buttons ($customButtonCount)"
                            ),
                            onClick = onOpenManageButtonsModal,
                            testTag = "pad_open_manage_buttons_btn"
                        )
                        if (hiddenNodeCount > 0) {
                            SheetPillButton(
                                label = PadStrings.tr(
                                    lang,
                                    "👁 Hiện tất cả nút ẩn ($hiddenNodeCount)",
                                    "👁 Show All Hidden ($hiddenNodeCount)"
                                ),
                                onClick = onShowAllHiddenNodes,
                                testTag = "pad_show_all_hidden_btn"
                            )
                        }
                        SheetPillButton(
                            label = PadStrings.tr(lang, "↺ Đặt lại mặc định", "↺ Reset Default"),
                            isDanger = true,
                            onClick = onResetCurrentLayout,
                            testTag = "pad_sheet_reset_layout_btn"
                        )
                    }
                }

                SheetSectionCard(
                    title = PadStrings.tr(lang, "2. PHẢN HỒI RUNG (HAPTIC & GAME RUMBLE)", "2. VIBRATION & HAPTICS")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = PadStrings.tr(lang, "Rung khi chạm nút", "Button touch haptic"),
                            color = SheetText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PadHapticLevel.entries.forEach { lvl ->
                                SheetChoiceChip(
                                    label = lvl.localizedTitle(lang),
                                    selected = settings.buttonHapticLevel == lvl,
                                    onClick = { onUpdateSettings(settings.copy(buttonHapticLevel = lvl)) },
                                    testTag = "pad_btn_haptic_${lvl.id}"
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = PadStrings.tr(lang, "Rung từ trò chơi trên TV", "In-game rumble from TV"),
                                color = SheetText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = PadStrings.tr(
                                    lang,
                                    "Nhận mô-tơ rung trái/phải trực tiếp từ Xbox Cloud Gaming",
                                    "Receives dual-motor rumble packets from Xbox Cloud Gaming"
                                ),
                                color = SheetMuted,
                                fontSize = 11.sp
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PadHapticLevel.entries.forEach { lvl ->
                                SheetChoiceChip(
                                    label = lvl.localizedTitle(lang),
                                    selected = settings.gameRumbleLevel == lvl,
                                    onClick = { onUpdateSettings(settings.copy(gameRumbleLevel = lvl)) },
                                    testTag = "pad_game_rumble_${lvl.id}"
                                )
                            }
                            SheetPillButton(
                                label = PadStrings.tr(lang, "Thử rung", "Test"),
                                onClick = onTestVibration,
                                testTag = "pad_test_vibration_btn"
                            )
                        }
                    }
                }

                SheetSectionCard(
                    title = PadStrings.tr(lang, "3. CẦN ANALOG, VÙNG CHẾT & TÁCH CỤM NÚT", "3. ANALOG STICKS, DEADZONES & CLUSTERS")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = PadStrings.tr(lang, "Độ nhạy cần Trái (L)", "Left Stick Sensitivity"),
                                    color = SheetText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${(settings.leftStickSens * 100).roundToInt()}%",
                                    color = SheetAccent,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = settings.leftStickSens,
                                onValueChange = {
                                    onUpdateSettings(settings.copy(leftStickSens = it.coerceIn(0.60f, 1.60f)))
                                },
                                valueRange = 0.60f..1.60f,
                                colors = SliderDefaults.colors(
                                    thumbColor = SheetAccent,
                                    activeTrackColor = SheetAccent,
                                    inactiveTrackColor = SheetBorder
                                )
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PadStickCurve.entries.forEach { curve ->
                                    SheetChoiceChip(
                                        label = curve.localizedTitle(lang),
                                        selected = settings.leftStickCurve == curve,
                                        onClick = { onUpdateSettings(settings.copy(leftStickCurve = curve)) },
                                        testTag = "pad_left_curve_${curve.id}"
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = PadStrings.tr(lang, "Độ nhạy cần Phải (R)", "Right Stick Sensitivity"),
                                    color = SheetText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${(settings.rightStickSens * 100).roundToInt()}%",
                                    color = SheetCyan,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = settings.rightStickSens,
                                onValueChange = {
                                    onUpdateSettings(settings.copy(rightStickSens = it.coerceIn(0.60f, 1.60f)))
                                },
                                valueRange = 0.60f..1.60f,
                                colors = SliderDefaults.colors(
                                    thumbColor = SheetCyan,
                                    activeTrackColor = SheetCyan,
                                    inactiveTrackColor = SheetBorder
                                )
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PadStickCurve.entries.forEach { curve ->
                                    SheetChoiceChip(
                                        label = curve.localizedTitle(lang),
                                        selected = settings.rightStickCurve == curve,
                                        onClick = { onUpdateSettings(settings.copy(rightStickCurve = curve)) },
                                        testTag = "pad_right_curve_${curve.id}"
                                    )
                                }
                            }
                        }
                    }

                    val effectiveAutoSprint = when (settings.autoSprintL3Override) {
                        0 -> false
                        1 -> true
                        else -> currentPreset.autoSprintL3
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SheetChoiceChip(
                            label = if (effectiveAutoSprint) {
                                PadStrings.tr(lang, "🏃 Tự chạy nhanh L3: Bật", "🏃 Auto-Sprint L3: On")
                            } else {
                                PadStrings.tr(lang, "🏃 Tự chạy nhanh L3: Tắt", "🏃 Auto-Sprint L3: Off")
                            },
                            selected = effectiveAutoSprint,
                            onClick = {
                                val nextVal = if (effectiveAutoSprint) 0 else 1
                                onUpdateSettings(settings.copy(autoSprintL3Override = nextVal))
                            },
                            testTag = "pad_toggle_autosprint_btn"
                        )

                        SheetChoiceChip(
                            label = if (settings.dpadSplitMode) {
                                PadStrings.tr(lang, "✚ D-Pad: Đang tách 4 nút", "✚ D-Pad: Split 4 Keys")
                            } else {
                                PadStrings.tr(lang, "✚ D-Pad: Chữ thập 8 hướng", "✚ D-Pad: 8-Way Cross")
                            },
                            selected = settings.dpadSplitMode,
                            onClick = {
                                onUpdateSettings(settings.copy(dpadSplitMode = !settings.dpadSplitMode))
                            },
                            testTag = "pad_sheet_toggle_split_dpad"
                        )

                        SheetChoiceChip(
                            label = if (settings.abxySplitMode) {
                                PadStrings.tr(lang, "◆ ABXY: Đang tách rời 4 nút", "◆ ABXY: Split 4 Buttons")
                            } else {
                                PadStrings.tr(lang, "◆ ABXY: Cụm trượt tổ hợp", "◆ ABXY: Slide Cluster")
                            },
                            selected = settings.abxySplitMode,
                            onClick = {
                                onUpdateSettings(settings.copy(abxySplitMode = !settings.abxySplitMode))
                            },
                            testTag = "pad_sheet_toggle_split_abxy"
                        )
                    }
                }

                SheetSectionCard(
                    title = PadStrings.tr(
                        lang,
                        "🧭 CON QUAY HỒI CHUYỂN (BẮN SÚNG / VR / ĐUA XE)",
                        "🧭 GYROSCOPE MOTION CONTROL (FPS / VR / RACING)"
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = PadStrings.tr(
                                    lang,
                                    "Con quay hồi chuyển trên điện thoại",
                                    "Phone Gyroscope Motion Control"
                                ),
                                color = SheetText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = PadStrings.tr(
                                    lang,
                                    "Mặc định TẮT. Bật khi muốn nghiêng điện thoại để xoay mọi góc (Bắn súng/VR) hoặc lái xe qua lại (Đua xe).",
                                    "Default OFF. Turn ON to tilt phone for 360° aiming/VR or left/right steering in racing games."
                                ),
                                color = SheetMuted,
                                fontSize = 11.sp
                            )
                        }
                        SheetChoiceChip(
                            label = if (settings.gyroEnabled) {
                                PadStrings.tr(lang, "● Đang bật", "● Enabled")
                            } else {
                                PadStrings.tr(lang, "○ Đang tắt (Mặc định)", "○ Off (Default)")
                            },
                            selected = settings.gyroEnabled,
                            onClick = {
                                val next = !settings.gyroEnabled
                                onUpdateSettings(settings.copy(gyroEnabled = next))
                                if (next) onRecenterGyro()
                            },
                            testTag = "pad_sheet_toggle_gyro_btn"
                        )
                    }

                    if (settings.gyroEnabled) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PadGyroMode.entries.forEach { gMode ->
                                SheetChoiceChip(
                                    label = gMode.shortTitle(lang),
                                    selected = settings.gyroMode == gMode,
                                    onClick = {
                                        onUpdateSettings(settings.copy(gyroMode = gMode))
                                        onRecenterGyro()
                                    },
                                    testTag = "pad_gyro_mode_${gMode.id}"
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (settings.gyroMode == PadGyroMode.RACING_STEER) {
                                            PadStrings.tr(lang, "Độ nhạy lái (Trái/Phải)", "Steering Sensitivity")
                                        } else {
                                            PadStrings.tr(lang, "Độ nhạy xoay ngang", "Horizontal Sensitivity")
                                        },
                                        color = SheetText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${settings.gyroSensX}%",
                                        color = SheetAccent,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Slider(
                                    value = settings.gyroSensX / 100f,
                                    onValueChange = {
                                        onUpdateSettings(settings.copy(gyroSensX = (it * 100).roundToInt().coerceIn(10, 300)))
                                    },
                                    valueRange = 0.10f..3.00f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = SheetAccent,
                                        activeTrackColor = SheetAccent,
                                        inactiveTrackColor = SheetBorder
                                    )
                                )
                            }

                            if (settings.gyroMode != PadGyroMode.RACING_STEER) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = PadStrings.tr(lang, "Độ nhạy xoay dọc", "Vertical Sensitivity"),
                                            color = SheetText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${settings.gyroSensY}%",
                                            color = SheetCyan,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Slider(
                                        value = settings.gyroSensY / 100f,
                                        onValueChange = {
                                            onUpdateSettings(settings.copy(gyroSensY = (it * 100).roundToInt().coerceIn(10, 300)))
                                        },
                                        valueRange = 0.10f..3.00f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = SheetCyan,
                                            activeTrackColor = SheetCyan,
                                            inactiveTrackColor = SheetBorder
                                        )
                                    )
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SheetChoiceChip(
                                label = if (settings.gyroHoldLtOnly) {
                                    PadStrings.tr(lang, "🎯 Chỉ bật khi giữ LT: Bật", "🎯 Hold LT Only: On")
                                } else {
                                    PadStrings.tr(lang, "🎯 Chỉ bật khi giữ LT: Tắt", "🎯 Hold LT Only: Off")
                                },
                                selected = settings.gyroHoldLtOnly,
                                onClick = {
                                    onUpdateSettings(settings.copy(gyroHoldLtOnly = !settings.gyroHoldLtOnly))
                                },
                                testTag = "pad_gyro_hold_lt_chip"
                            )

                            SheetChoiceChip(
                                label = if (settings.gyroInvertX) {
                                    PadStrings.tr(lang, "⇄ Đảo ngang: Bật", "⇄ Invert X: On")
                                } else {
                                    PadStrings.tr(lang, "⇄ Đảo ngang: Tắt", "⇄ Invert X: Off")
                                },
                                selected = settings.gyroInvertX,
                                onClick = {
                                    onUpdateSettings(settings.copy(gyroInvertX = !settings.gyroInvertX))
                                },
                                testTag = "pad_gyro_invert_x_chip"
                            )

                            if (settings.gyroMode != PadGyroMode.RACING_STEER) {
                                SheetChoiceChip(
                                    label = if (settings.gyroInvertY) {
                                        PadStrings.tr(lang, "⇅ Đảo dọc: Bật", "⇅ Invert Y: On")
                                    } else {
                                        PadStrings.tr(lang, "⇅ Đảo dọc: Tắt", "⇅ Invert Y: Off")
                                    },
                                    selected = settings.gyroInvertY,
                                    onClick = {
                                        onUpdateSettings(settings.copy(gyroInvertY = !settings.gyroInvertY))
                                    },
                                    testTag = "pad_gyro_invert_y_chip"
                                )
                            }

                            SheetPillButton(
                                label = PadStrings.tr(lang, "🎯 Đặt lại tâm con quay (0°)", "🎯 Recenter Gyro (0°)"),
                                isPrimary = true,
                                onClick = onRecenterGyro,
                                testTag = "pad_gyro_recenter_btn"
                            )
                        }
                    }
                }

                SheetSectionCard(
                    title = PadStrings.tr(lang, "4. KẾT NỐI TV HIỆN TẠI", "4. ACTIVE TV CONNECTION")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tvName,
                                color = SheetText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "IP: $tvHost · Ping: ${pingMs?.let { "$it ms" } ?: "-- ms"}",
                                color = SheetMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SheetPillButton(
                                label = PadStrings.tr(lang, "Quên TV này", "Forget TV"),
                                isDanger = true,
                                onClick = onForgetDevice,
                                testTag = "pad_sheet_forget_tv_btn"
                            )
                            SheetPillButton(
                                label = PadStrings.tr(lang, "Ngắt kết nối", "Disconnect"),
                                isDanger = true,
                                onClick = onDisconnect,
                                testTag = "pad_sheet_disconnect_btn"
                            )
                        }
                    }
                }
            }
        }
    }
}

// === CÁC THÀNH PHẦN GIAO DIỆN GỌN NHẸ TRONG POPOVER (Đồng bộ với HudMockupPrimitives của app chính) ===

@Composable
private fun PadPopoverSectionLabel(text: String) {
    Text(
        text = text,
        color = SheetMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 2.dp)
    )
}

@Composable
private fun PadPopoverSurfaceRow(
    bgColor: Color = SheetBg,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, SheetBorder.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun PadPopoverSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SheetBg)
            .border(1.dp, SheetBorder.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEachIndexed { idx, label ->
            val selected = idx == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (selected) SheetSelected else Color.Transparent)
                    .then(
                        if (selected) {
                            Modifier.border(1.dp, SheetAccent, RoundedCornerShape(7.dp))
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelect(idx) }
                    .padding(vertical = 6.dp, horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) SheetAccent else SheetMuted,
                    fontSize = 10.5.sp,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PadPopoverSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SheetBg)
            .border(1.dp, SheetBorder.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = SheetText,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = valueText,
                color = SheetAccent,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.height(26.dp),
            colors = SliderDefaults.colors(
                thumbColor = SheetAccent,
                activeTrackColor = SheetAccent,
                inactiveTrackColor = SheetBorder
            )
        )
    }
}

@Composable
private fun PadPopoverSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    PadPopoverSurfaceRow(
        bgColor = if (checked) SheetSelected.copy(alpha = 0.55f) else SheetBg,
        onClick = { onCheckedChange(!checked) }
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SheetText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = SheetMuted,
                    fontSize = 10.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (checked) SheetAccent else SheetCard)
                .border(1.dp, if (checked) SheetAccent else SheetBorder, RoundedCornerShape(999.dp))
                .padding(horizontal = 2.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (checked) Color(0xFF062012) else SheetMuted)
            )
        }
    }
}

@Composable
private fun PadPopoverActionBtn(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: androidx.compose.ui.unit.Dp = 32.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.5.sp,
    isMain: Boolean = false,
    bgColor: Color = SheetBg,
    textColor: Color = SheetText,
    borderColor: Color = SheetBorder,
    testTag: String? = null
) {
    val finalBg = if (isMain) SheetAccent else bgColor
    val finalBorder = if (isMain) SheetAccent else borderColor
    val finalText = if (isMain) Color(0xFF062012) else textColor

    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(10.dp))
            .background(finalBg)
            .border(1.dp, finalBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = finalText,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SheetSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SheetCard)
            .border(1.dp, SheetBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            color = SheetAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
        content()
    }
}

@Composable
private fun SheetChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) SheetSelected else SheetBg)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) SheetAccent else SheetBorder,
                shape = RoundedCornerShape(9.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) SheetAccent else SheetText,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun SheetPillButton(
    label: String,
    isPrimary: Boolean = false,
    isDanger: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    val bgColor = when {
        isPrimary -> SheetAccent
        isDanger -> SheetDanger.copy(alpha = 0.14f)
        else -> SheetCard
    }
    val borderColor = when {
        isPrimary -> SheetAccent
        isDanger -> SheetDanger
        else -> SheetBorder
    }
    val textColor = when {
        isPrimary -> Color(0xFF062012)
        isDanger -> SheetDanger
        else -> SheetText
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}
