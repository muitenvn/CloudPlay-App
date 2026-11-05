package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.WebView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sqrt

@Composable
fun VirtualGamepadOverlay(
    webView: WebView?,
    opacity: Float? = null,
    gameKey: String? = null,
    gameDisplayName: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()

    val effectiveOpacity = (opacity ?: config.gamepadOpacity).coerceIn(0.25f, 0.95f)
    val presetScaleMultiplier = when (config.gamepadLayoutPreset) {
        GamepadLayoutPreset.STANDARD -> 1.0f
        GamepadLayoutPreset.FPS_PRO -> 1.04f
        GamepadLayoutPreset.RACING -> 1.02f
        GamepadLayoutPreset.COMPACT -> 0.86f
    }
    val effectiveScale = (config.gamepadScale * presetScaleMultiplier).coerceIn(0.70f, 1.45f)
    val rightStickBaseScale = if (config.gamepadLayoutPreset == GamepadLayoutPreset.FPS_PRO) {
        (effectiveScale * 1.15f).coerceAtMost(1.50f)
    } else {
        effectiveScale
    }

    var showControlTuner by remember { mutableStateOf(false) }
    var isEditPositionMode by remember { mutableStateOf(false) }
    var selectedButtonId by remember { mutableStateOf<String?>("abxy") }
    var showAddButtonModal by remember { mutableStateOf(false) }
    var showEditGrid by remember { mutableStateOf(true) }
    var isToolbarCollapsed by remember { mutableStateOf(false) }

    val normGameKey = remember(gameKey) { SettingsRepository.normalizeGameLayoutKey(gameKey) }
    val perGameLayout = remember(normGameKey, config.usePerGameGamepadLayout, config.perGameLayoutsJson) {
        if (config.usePerGameGamepadLayout && normGameKey.isNotEmpty()) {
            SettingsRepository.getPerGameLayout(normGameKey, config)
        } else null
    }

    val effectiveOffsetsRaw = perGameLayout?.offsets ?: config.gamepadCustomOffsets
    val effectiveScalesRaw = perGameLayout?.scales ?: config.gamepadButtonScales
    val effectiveHiddenRaw = perGameLayout?.hiddenButtons ?: config.gamepadHiddenButtons
    val abxySplitMode = perGameLayout?.abxySplit ?: config.gamepadAbxySplitMode
    val dpadSplitMode = perGameLayout?.dpadSplit ?: config.gamepadDpadSplitMode

    // Custom draggable per-button offsets (in pixels), backward-compatible with legacy cluster keys
    val customOffsets = remember(effectiveOffsetsRaw, normGameKey, config.usePerGameGamepadLayout) {
        mutableStateMapOf<String, Pair<Float, Float>>().apply {
            val parsed = parseControlOffsets(effectiveOffsetsRaw)
            putAll(parsed)
            // Backward compatibility: if user had legacy cluster offsets, seed individual button offsets if unset
            parsed["left_shoulder"]?.let { (cx, cy) ->
                if (cx != 0f || cy != 0f) {
                    if (!containsKey("btn_lt")) put("btn_lt", cx to cy)
                    if (!containsKey("btn_lb")) put("btn_lb", cx to cy)
                    if (!containsKey("btn_l3")) put("btn_l3", cx to cy)
                }
            }
            parsed["right_shoulder"]?.let { (cx, cy) ->
                if (cx != 0f || cy != 0f) {
                    if (!containsKey("btn_r3")) put("btn_r3", cx to cy)
                    if (!containsKey("btn_rb")) put("btn_rb", cx to cy)
                    if (!containsKey("btn_rt")) put("btn_rt", cx to cy)
                }
            }
            parsed["center_bar"]?.let { (cx, cy) ->
                if (cx != 0f || cy != 0f) {
                    if (!containsKey("btn_view")) put("btn_view", cx to cy)
                    if (!containsKey("btn_xbox")) put("btn_xbox", cx to cy)
                    if (!containsKey("btn_menu")) put("btn_menu", cx to cy)
                }
            }
        }
    }

    // Per-button individual scale multiplier (0.50f .. 2.00f)
    val customButtonScales = remember(effectiveScalesRaw, normGameKey, config.usePerGameGamepadLayout) {
        mutableStateMapOf<String, Float>().apply {
            putAll(parseButtonScales(effectiveScalesRaw))
        }
    }

    // Hidden (deleted) buttons set
    val hiddenButtons = remember(effectiveHiddenRaw, normGameKey, config.usePerGameGamepadLayout) {
        parseHiddenButtons(effectiveHiddenRaw)
    }

    fun saveCustomOffsets() {
        SettingsRepository.updateGamepadCustomOffsets(serializeControlOffsets(customOffsets), normGameKey)
    }

    fun saveButtonScales() {
        SettingsRepository.updateGamepadButtonScales(serializeButtonScales(customButtonScales), normGameKey)
    }

    fun setButtonScale(nodeId: String, newMultiplier: Float) {
        val clamped = newMultiplier.coerceIn(0.50f, 2.00f)
        customButtonScales[nodeId] = clamped
        saveButtonScales()
    }

    fun deleteButton(nodeId: String) {
        val updated = hiddenButtons.toMutableSet()
        updated.add(nodeId)
        SettingsRepository.updateGamepadHiddenButtons(serializeHiddenButtons(updated), normGameKey)
    }

    fun restoreButton(nodeId: String) {
        val updated = hiddenButtons.toMutableSet()
        updated.remove(nodeId)
        SettingsRepository.updateGamepadHiddenButtons(serializeHiddenButtons(updated), normGameKey)
        selectedButtonId = nodeId
    }

    fun resetSingleButton(nodeId: String) {
        customOffsets.remove(nodeId)
        customButtonScales.remove(nodeId)
        saveCustomOffsets()
        saveButtonScales()
    }

    fun resetAllCustomizations() {
        customOffsets.clear()
        customButtonScales.clear()
        SettingsRepository.resetGamepadCustomOffsets(normGameKey)
    }

    fun switchCurrentGameToGlobalLayout() {
        SettingsRepository.useGlobalLayoutForGame(normGameKey)
    }

    fun nodeScale(nodeId: String, baseScale: Float = effectiveScale): Float {
        val mult = customButtonScales[nodeId] ?: 1.0f
        return (baseScale * mult).coerceIn(0.45f, 2.50f)
    }

    fun vibrate(stronger: Boolean = false) {
        val level = config.gamepadHapticLevel
        if (level == GamepadHapticLevel.OFF) return
        try {
            val dur = if (stronger) (level.durationMs + 10L) else level.durationMs
            val amp = if (stronger) (level.amplitude + 50).coerceAtMost(255) else level.amplitude
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vib = vm?.defaultVibrator
                if (vib != null && vib.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val effect = when {
                            stronger -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                            level == GamepadHapticLevel.LIGHT -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                            else -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                        }
                        vib.vibrate(effect)
                    } else if (amp > 0) {
                        vib.vibrate(VibrationEffect.createOneShot(dur, amp))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (v != null && v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val effect = when {
                            stronger -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                            level == GamepadHapticLevel.LIGHT -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                            else -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                        }
                        v.vibrate(effect)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && amp > 0) {
                        v.vibrate(VibrationEffect.createOneShot(dur, amp))
                    } else {
                        v.vibrate(dur)
                    }
                }
            }
        } catch (_: Exception) {
            try {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(level.durationMs.coerceAtLeast(10L))
            } catch (_: Exception) {}
        }
    }

    fun sendButtonEvent(buttonIndex: Int, pressed: Boolean, analogValue: Float? = null) {
        if (pressed) vibrate(stronger = buttonIndex in setOf(6, 7, 10, 11))
        val v = analogValue ?: if (pressed) 1f else 0f
        val rv = (v * 100f).roundToInt() / 100f
        webView?.evaluateJavascript(
            "if(window.__cpBtn){window.__cpBtn($buttonIndex,$pressed,$rv);}else if(window.dispatchEvent){window.dispatchEvent(new CustomEvent('virtual_gamepad_button',{detail:{index:$buttonIndex,pressed:$pressed,value:$rv}}));}",
            null
        )
    }

    fun sendStickAxesPair(xAxisIndex: Int, xVal: Float, yAxisIndex: Int, yVal: Float) {
        val rx = (xVal.coerceIn(-1f, 1f) * 100f).roundToInt() / 100f
        val ry = (yVal.coerceIn(-1f, 1f) * 100f).roundToInt() / 100f
        webView?.evaluateJavascript(
            "if(window.__cpStick){window.__cpStick($xAxisIndex,$rx,$yAxisIndex,$ry);}else if(window.dispatchEvent){window.dispatchEvent(new CustomEvent('virtual_gamepad_axes_pair',{detail:{xAxis:$xAxisIndex,xVal:$rx,yAxis:$yAxisIndex,yVal:$ry}}));}",
            null
        )
    }

    fun Modifier.draggableGamepadNode(key: String): Modifier {
        val offsetPair = customOffsets[key] ?: (0f to 0f)
        val isSelected = isEditPositionMode && selectedButtonId == key
        val base = this.offset { IntOffset(offsetPair.first.roundToInt(), offsetPair.second.roundToInt()) }
        return if (!isEditPositionMode) {
            base
        } else {
            base
                .border(
                    width = if (isSelected) 2.2.dp else 1.2.dp,
                    color = if (isSelected) XboxNeonGreen else DirectCyan.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(14.dp)
                )
                .background(
                    color = if (isSelected) XboxNeonGreen.copy(alpha = 0.22f) else DirectCyan.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(14.dp)
                )
                .pointerInput(key, isEditPositionMode) {
                    detectTapGestures(
                        onTap = {
                            vibrate()
                            selectedButtonId = key
                        }
                    )
                }
                .pointerInput(key, isEditPositionMode) {
                    detectDragGestures(
                        onDragStart = {
                            selectedButtonId = key
                        },
                        onDragEnd = { saveCustomOffsets() },
                        onDragCancel = { saveCustomOffsets() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            selectedButtonId = key
                            val cur = customOffsets[key] ?: (0f to 0f)
                            customOffsets[key] = (cur.first + dragAmount.x) to (cur.second + dragAmount.y)
                        }
                    )
                }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("virtual_gamepad_overlay")
    ) {
        // Simple Alignment Grid & Center Crosshair when editing button positions
        if (isEditPositionMode && showEditGrid) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("virtual_gamepad_alignment_grid")
            ) {
                drawRect(color = Color(0x40060B09))
                val stepPx = 32.dp.toPx().coerceAtLeast(16f)
                val gridColor = Color.White.copy(alpha = 0.08f)
                val centerColor = XboxNeonGreen.copy(alpha = 0.35f)

                var x = stepPx
                while (x < size.width) {
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )
                    x += stepPx
                }
                var y = stepPx
                while (y < size.height) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += stepPx
                }

                val midX = size.width / 2f
                val midY = size.height / 2f
                drawLine(
                    color = centerColor,
                    start = Offset(midX, 0f),
                    end = Offset(midX, size.height),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = centerColor,
                    start = Offset(0f, midY),
                    end = Offset(size.width, midY),
                    strokeWidth = 1.5f
                )
            }
        }

        // Top Shoulder Triggers & L3/R3 Individual Draggable Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Shoulder Buttons: LT, LB, L3 (each independently draggable & resizable)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if ("btn_lt" !in hiddenButtons) {
                    Box(modifier = Modifier.draggableGamepadNode("btn_lt")) {
                        GamepadTriggerButton(
                            label = "LT",
                            buttonIndex = 6,
                            scale = nodeScale("btn_lt"),
                            opacity = effectiveOpacity,
                            analogDragMode = config.gamepadLayoutPreset == GamepadLayoutPreset.RACING,
                            isEditMode = isEditPositionMode,
                            onStateChange = { pressed, analogVal ->
                                sendButtonEvent(6, pressed, analogVal)
                            }
                        )
                    }
                }
                if ("btn_lb" !in hiddenButtons) {
                    Box(modifier = Modifier.draggableGamepadNode("btn_lb")) {
                        GamepadShoulderButton(
                            label = "LB",
                            buttonIndex = 4,
                            scale = nodeScale("btn_lb"),
                            opacity = effectiveOpacity,
                            isEditMode = isEditPositionMode,
                            onStateChange = { sendButtonEvent(4, it) }
                        )
                    }
                }
                if ("btn_l3" !in hiddenButtons) {
                    Box(modifier = Modifier.draggableGamepadNode("btn_l3")) {
                        GamepadThumbClickMiniButton(
                            label = "L3",
                            buttonIndex = 10,
                            scale = nodeScale("btn_l3"),
                            opacity = effectiveOpacity,
                            isEditMode = isEditPositionMode,
                            onStateChange = { sendButtonEvent(10, it) }
                        )
                    }
                }
            }

            // Right Shoulder Buttons: R3, RB, RT (each independently draggable & resizable)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if ("btn_r3" !in hiddenButtons) {
                    Box(modifier = Modifier.draggableGamepadNode("btn_r3")) {
                        GamepadThumbClickMiniButton(
                            label = "R3",
                            buttonIndex = 11,
                            scale = nodeScale("btn_r3"),
                            opacity = effectiveOpacity,
                            isEditMode = isEditPositionMode,
                            onStateChange = { sendButtonEvent(11, it) }
                        )
                    }
                }
                if ("btn_rb" !in hiddenButtons) {
                    Box(modifier = Modifier.draggableGamepadNode("btn_rb")) {
                        GamepadShoulderButton(
                            label = "RB",
                            buttonIndex = 5,
                            scale = nodeScale("btn_rb"),
                            opacity = effectiveOpacity,
                            isEditMode = isEditPositionMode,
                            onStateChange = { sendButtonEvent(5, it) }
                        )
                    }
                }
                if ("btn_rt" !in hiddenButtons) {
                    Box(modifier = Modifier.draggableGamepadNode("btn_rt")) {
                        GamepadTriggerButton(
                            label = "RT",
                            buttonIndex = 7,
                            scale = nodeScale("btn_rt"),
                            opacity = effectiveOpacity,
                            analogDragMode = config.gamepadLayoutPreset == GamepadLayoutPreset.RACING,
                            isEditMode = isEditPositionMode,
                            onStateChange = { pressed, analogVal ->
                                sendButtonEvent(7, pressed, analogVal)
                            }
                        )
                    }
                }
            }
        }

        // Center Top System Bar: View, Xbox Nexus, Menu + Pro Control Tuner Button (⚙)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if ("btn_view" !in hiddenButtons) {
                Box(modifier = Modifier.draggableGamepadNode("btn_view")) {
                    GamepadSmallButton(
                        label = "⧉ View",
                        index = 8,
                        scale = customButtonScales["btn_view"] ?: 1.0f,
                        opacity = effectiveOpacity,
                        isEditMode = isEditPositionMode
                    ) { sendButtonEvent(8, it) }
                }
            }
            if ("btn_xbox" !in hiddenButtons) {
                Box(modifier = Modifier.draggableGamepadNode("btn_xbox")) {
                    GamepadXboxNexusButton(
                        scale = customButtonScales["btn_xbox"] ?: 1.0f,
                        opacity = effectiveOpacity,
                        isEditMode = isEditPositionMode
                    ) { sendButtonEvent(16, it) }
                }
            }
            if ("btn_menu" !in hiddenButtons) {
                Box(modifier = Modifier.draggableGamepadNode("btn_menu")) {
                    GamepadSmallButton(
                        label = "☰ Menu",
                        index = 9,
                        scale = customButtonScales["btn_menu"] ?: 1.0f,
                        opacity = effectiveOpacity,
                        isEditMode = isEditPositionMode
                    ) { sendButtonEvent(9, it) }
                }
            }

            // Control Quick-Tune Pill (always accessible so user can edit or restore buttons)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (showControlTuner || isEditPositionMode) {
                            XboxNeonGreen.copy(alpha = 0.28f)
                        } else {
                            Color.Black.copy(alpha = effectiveOpacity * 0.75f)
                        }
                    )
                    .border(
                        1.dp,
                        if (showControlTuner || isEditPositionMode) XboxNeonGreen else DirectCyan.copy(alpha = 0.7f),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        vibrate()
                        if (isEditPositionMode) {
                            isEditPositionMode = false
                            showAddButtonModal = false
                        } else {
                            showControlTuner = !showControlTuner
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("virtual_gamepad_tune_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isEditPositionMode) (if (isEn) "✓ Done" else "✓ Xong") else "⚙ Control",
                    color = if (showControlTuner || isEditPositionMode) XboxNeonGreen else DirectCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Left Controls: Left Analog Stick & D-Pad (Cluster or Split 4 Directional Buttons)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            if ("left_stick" !in hiddenButtons) {
                Box(modifier = Modifier.draggableGamepadNode("left_stick")) {
                    VirtualAnalogStick(
                        opacity = effectiveOpacity,
                        scale = nodeScale("left_stick"),
                        innerDeadzone = config.leftStickInnerDeadzone,
                        outerDeadzone = config.leftStickOuterDeadzone,
                        sensitivity = config.gamepadSensitivity,
                        label = "L",
                        enableAutoSprintL3 = config.gamepadSprintLockEnabled,
                        isEditMode = isEditPositionMode,
                        onSprintStateChange = { sprinting ->
                            sendButtonEvent(10, sprinting)
                        },
                        onMove = { x, y ->
                            sendStickAxesPair(0, x, 1, y)
                        }
                    )
                }
            }

            if (!dpadSplitMode) {
                if ("dpad" !in hiddenButtons) {
                    Box(modifier = Modifier.draggableGamepadNode("dpad")) {
                        VirtualDpadCross(
                            opacity = effectiveOpacity,
                            scale = nodeScale("dpad"),
                            isEditMode = isEditPositionMode,
                            onButton = { index, pressed -> sendButtonEvent(index, pressed) }
                        )
                    }
                }
            } else {
                // Split D-Pad: 4 individually draggable & resizable direction buttons
                val splitBaseSize = 104.dp * effectiveScale
                Box(modifier = Modifier.size(splitBaseSize)) {
                    if ("dpad_up" !in hiddenButtons) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .draggableGamepadNode("dpad_up")
                        ) {
                            StandaloneDpadButton(
                                symbol = "▲",
                                buttonIndex = 12,
                                scale = nodeScale("dpad_up"),
                                opacity = effectiveOpacity,
                                isEditMode = isEditPositionMode,
                                onButton = { sendButtonEvent(12, it) }
                            )
                        }
                    }
                    if ("dpad_down" !in hiddenButtons) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .draggableGamepadNode("dpad_down")
                        ) {
                            StandaloneDpadButton(
                                symbol = "▼",
                                buttonIndex = 13,
                                scale = nodeScale("dpad_down"),
                                opacity = effectiveOpacity,
                                isEditMode = isEditPositionMode,
                                onButton = { sendButtonEvent(13, it) }
                            )
                        }
                    }
                    if ("dpad_left" !in hiddenButtons) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .draggableGamepadNode("dpad_left")
                        ) {
                            StandaloneDpadButton(
                                symbol = "◀",
                                buttonIndex = 14,
                                scale = nodeScale("dpad_left"),
                                opacity = effectiveOpacity,
                                isEditMode = isEditPositionMode,
                                onButton = { sendButtonEvent(14, it) }
                            )
                        }
                    }
                    if ("dpad_right" !in hiddenButtons) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .draggableGamepadNode("dpad_right")
                        ) {
                            StandaloneDpadButton(
                                symbol = "▶",
                                buttonIndex = 15,
                                scale = nodeScale("dpad_right"),
                                opacity = effectiveOpacity,
                                isEditMode = isEditPositionMode,
                                onButton = { sendButtonEvent(15, it) }
                            )
                        }
                    }
                }
            }
        }

        // Right Controls: Right Analog Stick & ABXY (Cluster or Split A, B, X, Y Independent Buttons)
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            if ("right_stick" !in hiddenButtons) {
                Box(modifier = Modifier.draggableGamepadNode("right_stick")) {
                    VirtualAnalogStick(
                        opacity = effectiveOpacity * 0.92f,
                        scale = nodeScale("right_stick", rightStickBaseScale),
                        innerDeadzone = config.rightStickInnerDeadzone,
                        outerDeadzone = config.rightStickOuterDeadzone,
                        sensitivity = config.gamepadSensitivity,
                        label = "R",
                        enableAutoSprintL3 = false,
                        isEditMode = isEditPositionMode,
                        onSprintStateChange = {},
                        onMove = { x, y ->
                            sendStickAxesPair(2, x, 3, y)
                        }
                    )
                }
            }

            if (!abxySplitMode) {
                if ("abxy" !in hiddenButtons) {
                    Box(modifier = Modifier.draggableGamepadNode("abxy")) {
                        VirtualAbxyPad(
                            opacity = effectiveOpacity,
                            scale = nodeScale("abxy"),
                            isEditMode = isEditPositionMode,
                            onButton = { index, pressed ->
                                sendButtonEvent(index, pressed)
                            }
                        )
                    }
                }
            } else {
                // Split ABXY Mode: 4 independent draggable, resizable, removable buttons A, B, X, Y
                val splitAbxyBoxSize = 132.dp * effectiveScale
                Box(modifier = Modifier.size(splitAbxyBoxSize)) {
                    if ("btn_y" !in hiddenButtons) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .draggableGamepadNode("btn_y")
                        ) {
                            StandaloneAbxyButton(
                                label = "Y",
                                accentColor = Color(0xFFFFD166),
                                buttonIndex = 3,
                                scale = nodeScale("btn_y"),
                                opacity = effectiveOpacity,
                                isEditMode = isEditPositionMode,
                                onButton = { sendButtonEvent(3, it) }
                            )
                        }
                    }
                    if ("btn_a" !in hiddenButtons) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .draggableGamepadNode("btn_a")
                        ) {
                            StandaloneAbxyButton(
                                label = "A",
                                accentColor = Color(0xFF00E676),
                                buttonIndex = 0,
                                scale = nodeScale("btn_a"),
                                opacity = effectiveOpacity,
                                isEditMode = isEditPositionMode,
                                onButton = { sendButtonEvent(0, it) }
                            )
                        }
                    }
                    if ("btn_x" !in hiddenButtons) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .draggableGamepadNode("btn_x")
                        ) {
                            StandaloneAbxyButton(
                                label = "X",
                                accentColor = Color(0xFF00B4D8),
                                buttonIndex = 2,
                                scale = nodeScale("btn_x"),
                                opacity = effectiveOpacity,
                                isEditMode = isEditPositionMode,
                                onButton = { sendButtonEvent(2, it) }
                            )
                        }
                    }
                    if ("btn_b" !in hiddenButtons) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .draggableGamepadNode("btn_b")
                        ) {
                            StandaloneAbxyButton(
                                label = "B",
                                accentColor = Color(0xFFFF3366),
                                buttonIndex = 1,
                                scale = nodeScale("btn_b"),
                                opacity = effectiveOpacity,
                                isEditMode = isEditPositionMode,
                                onButton = { sendButtonEvent(1, it) }
                            )
                        }
                    }
                }
            }
        }

        // Per-Button Edit Mode Inspector Toolbar (Simple Gaming Style + Live X/Y Coordinates)
        if (isEditPositionMode && !showControlTuner) {
            val activeId = selectedButtonId
            val activeInfo = remember(activeId) {
                ALL_GAMEPAD_NODES.find { it.id == activeId }
            }
            val activeScaleMult = if (activeId != null) (customButtonScales[activeId] ?: 1.0f) else 1.0f
            val activeOffset = if (activeId != null) (customOffsets[activeId] ?: (0f to 0f)) else (0f to 0f)
            val coordX = activeOffset.first.roundToInt()
            val coordY = activeOffset.second.roundToInt()
            val xSign = if (coordX > 0) "+$coordX" else "$coordX"
            val ySign = if (coordY > 0) "+$coordY" else "$coordY"

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 46.dp, start = 12.dp, end = 12.dp)
                    .widthIn(max = 540.dp)
                    .testTag("virtual_gamepad_edit_toolbar"),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xF50E1612),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF283D31))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Row 1: Selected button name + Live Coordinates (X, Y) + Size % + Collapse toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (activeInfo != null) {
                                    val title = if (isEn) activeInfo.labelEn else activeInfo.labelVi
                                    "✥ $title"
                                } else {
                                    if (isEn) "✥ Tap a button to edit" else "✥ Chạm nút bất kỳ để sửa"
                                },
                                color = XboxNeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )

                            if (activeId != null) {
                                Surface(
                                    shape = RoundedCornerShape(5.dp),
                                    color = Color(0xFF16241C),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF2B4235))
                                ) {
                                    Text(
                                        text = "X:$xSign  Y:$ySign  •  ${(activeScaleMult * 100).roundToInt()}%",
                                        color = Color.White,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                            .testTag("virtual_gamepad_live_coords")
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (activeId != null && !isToolbarCollapsed) {
                                // Size -10%
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF17241D),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF2E4739)),
                                    modifier = Modifier
                                        .clickable {
                                            vibrate()
                                            setButtonScale(activeId, activeScaleMult - 0.10f)
                                        }
                                        .testTag("virtual_gamepad_btn_scale_minus")
                                ) {
                                    Text(
                                        text = "− Cỡ",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }

                                // Size +10%
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF17241D),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxNeonGreen.copy(alpha = 0.7f)),
                                    modifier = Modifier
                                        .clickable {
                                            vibrate()
                                            setButtonScale(activeId, activeScaleMult + 0.10f)
                                        }
                                        .testTag("virtual_gamepad_btn_scale_plus")
                                ) {
                                    Text(
                                        text = "+ Cỡ",
                                        color = XboxNeonGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }

                                // Reset only selected button
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF17241D),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF2E4739)),
                                    modifier = Modifier
                                        .clickable {
                                            vibrate()
                                            resetSingleButton(activeId)
                                        }
                                        .testTag("virtual_gamepad_reset_single_button")
                                ) {
                                    Text(
                                        text = if (isEn) "↺ Button" else "↺ Nút này",
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }

                                // Delete / Hide Selected Button
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = JapanRed.copy(alpha = 0.18f),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, JapanRed.copy(alpha = 0.75f)),
                                    modifier = Modifier
                                        .clickable {
                                            vibrate(stronger = true)
                                            deleteButton(activeId)
                                            selectedButtonId = null
                                        }
                                        .testTag("virtual_gamepad_delete_button")
                                ) {
                                    Text(
                                        text = if (isEn) "🗑 Delete" else "🗑 Xóa nút",
                                        color = Color(0xFFFF8A80),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // Collapse / Expand Inspector Toolbar
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF17241D),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF2E4739)),
                                modifier = Modifier.clickable {
                                    isToolbarCollapsed = !isToolbarCollapsed
                                }
                            ) {
                                Text(
                                    text = if (isToolbarCollapsed) "▼" else "▲",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    if (!isToolbarCollapsed) {
                        // Per-button smooth size slider when a button is selected
                        if (activeId != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (isEn) "Size:" else "Cỡ nút:",
                                    color = TextSecondary,
                                    fontSize = 9.5.sp
                                )
                                Slider(
                                    value = activeScaleMult,
                                    onValueChange = { setButtonScale(activeId, it) },
                                    valueRange = 0.50f..2.00f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(20.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = XboxNeonGreen,
                                        activeTrackColor = XboxNeonGreen
                                    )
                                )
                            }
                        }

                        // Row 2: Grid toggle, Split/Merge ABXY, Split/Merge D-Pad, + Add Button, Reset All, Done
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Grid Toggle
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (showEditGrid) XboxNeonGreen.copy(alpha = 0.20f) else Color(0xFF17241D),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.8.dp,
                                    if (showEditGrid) XboxNeonGreen else Color(0xFF2E4739)
                                ),
                                modifier = Modifier
                                    .clickable {
                                        vibrate()
                                        showEditGrid = !showEditGrid
                                    }
                                    .testTag("virtual_gamepad_grid_toggle")
                            ) {
                                Text(
                                    text = if (isEn) "# Grid" else "# Lưới",
                                    color = if (showEditGrid) XboxNeonGreen else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // Toggle Split / Merge ABXY
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (abxySplitMode) XboxNeonGreen.copy(alpha = 0.20f) else Color(0xFF17241D),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.8.dp,
                                    if (abxySplitMode) XboxNeonGreen else Color(0xFF2E4739)
                                ),
                                modifier = Modifier
                                    .clickable {
                                        vibrate()
                                        val nextSplit = !abxySplitMode
                                        SettingsRepository.updateGamepadAbxySplitMode(nextSplit, normGameKey)
                                        selectedButtonId = if (nextSplit) "btn_a" else "abxy"
                                    }
                                    .testTag("virtual_gamepad_split_abxy_toggle")
                            ) {
                                Text(
                                    text = if (abxySplitMode) {
                                        if (isEn) "⇄ Merge ABXY" else "⇄ Gộp chùm ABXY"
                                    } else {
                                        if (isEn) "⇄ Split A/B/X/Y" else "⇄ Tách rời A/B/X/Y"
                                    },
                                    color = if (abxySplitMode) XboxNeonGreen else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // Toggle Split / Merge D-Pad
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (dpadSplitMode) XboxNeonGreen.copy(alpha = 0.20f) else Color(0xFF17241D),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.8.dp,
                                    if (dpadSplitMode) XboxNeonGreen else Color(0xFF2E4739)
                                ),
                                modifier = Modifier
                                    .clickable {
                                        vibrate()
                                        val nextSplit = !dpadSplitMode
                                        SettingsRepository.updateGamepadDpadSplitMode(nextSplit, normGameKey)
                                        selectedButtonId = if (nextSplit) "dpad_up" else "dpad"
                                    }
                                    .testTag("virtual_gamepad_split_dpad_toggle")
                            ) {
                                Text(
                                    text = if (dpadSplitMode) {
                                        if (isEn) "⇄ Merge D-Pad" else "⇄ Gộp D-Pad"
                                    } else {
                                        if (isEn) "⇄ Split D-Pad" else "⇄ Tách D-Pad"
                                    },
                                    color = if (dpadSplitMode) XboxNeonGreen else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // + Add / Restore Buttons
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF17241D),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF2E4739)),
                                modifier = Modifier
                                    .clickable {
                                        vibrate()
                                        showAddButtonModal = !showAddButtonModal
                                    }
                                    .testTag("virtual_gamepad_add_button")
                            ) {
                                val hiddenCount = hiddenButtons.size
                                Text(
                                    text = if (hiddenCount > 0) {
                                        if (isEn) "＋ Add Button ($hiddenCount)" else "＋ Thêm nút ($hiddenCount)"
                                    } else {
                                        if (isEn) "＋ Add / Manage Buttons" else "＋ Thêm / Quản lý nút"
                                    },
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // Reset All
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF17241D),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, WarningAmber.copy(alpha = 0.7f)),
                                modifier = Modifier.clickable {
                                    vibrate()
                                    resetAllCustomizations()
                                }
                            ) {
                                Text(
                                    text = if (isEn) "↺ Default" else "↺ Mặc định",
                                    color = WarningAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // Done
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = XboxNeonGreen.copy(alpha = 0.22f),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxNeonGreen),
                                modifier = Modifier.clickable {
                                    vibrate()
                                    isEditPositionMode = false
                                    showAddButtonModal = false
                                }
                            ) {
                                Text(
                                    text = if (isEn) "✓ Done" else "✓ Xong",
                                    color = XboxNeonGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Modal Drawer to Add / Restore or Toggle Any Button
        if (showAddButtonModal) {
            AddOrManageButtonsModal(
                isEn = isEn,
                hiddenButtons = hiddenButtons,
                abxySplitMode = abxySplitMode,
                dpadSplitMode = dpadSplitMode,
                onToggleButtonVisibility = { nodeId ->
                    vibrate()
                    if (nodeId in hiddenButtons) {
                        restoreButton(nodeId)
                    } else {
                        deleteButton(nodeId)
                    }
                },
                onRestoreAll = {
                    vibrate()
                    SettingsRepository.updateGamepadHiddenButtons("", normGameKey)
                },
                onClose = { showAddButtonModal = false },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp)
            )
        }

        // In-Game Floating Control Quick-Tune Modal Panel
        if (showControlTuner) {
            ControlQuickTunerPanel(
                isEn = isEn,
                scale = config.gamepadScale,
                opacity = config.gamepadOpacity,
                deadzone = config.leftStickInnerDeadzone,
                sensitivity = config.gamepadSensitivity,
                preset = config.gamepadLayoutPreset,
                hapticLevel = config.gamepadHapticLevel,
                gameRumbleLevel = config.gameRumbleLevel,
                sprintLockEnabled = config.gamepadSprintLockEnabled,
                abxySplitMode = abxySplitMode,
                dpadSplitMode = dpadSplitMode,
                isEditPositionMode = isEditPositionMode,
                hasActiveGameKey = normGameKey.isNotEmpty(),
                hasPerGameCustomLayout = perGameLayout != null,
                gameDisplayName = gameDisplayName,
                onUseGlobalLayout = {
                    vibrate()
                    switchCurrentGameToGlobalLayout()
                },
                onToggleEditPosition = {
                    isEditPositionMode = !isEditPositionMode
                    if (isEditPositionMode) showControlTuner = false
                },
                onToggleAbxySplit = {
                    SettingsRepository.updateGamepadAbxySplitMode(!abxySplitMode, normGameKey)
                },
                onToggleDpadSplit = {
                    SettingsRepository.updateGamepadDpadSplitMode(!dpadSplitMode, normGameKey)
                },
                onOpenAddButtons = {
                    showControlTuner = false
                    isEditPositionMode = true
                    showAddButtonModal = true
                },
                onResetPositions = {
                    resetAllCustomizations()
                },
                onClose = { showControlTuner = false },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

