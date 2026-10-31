package com.example.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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

data class GamepadEditableNodeInfo(
    val id: String,
    val labelVi: String,
    val labelEn: String,
    val shortBadge: String
)

private val ALL_GAMEPAD_NODES = listOf(
    GamepadEditableNodeInfo("btn_lt", "Cò trái LT", "Left Trigger LT", "LT"),
    GamepadEditableNodeInfo("btn_lb", "Nút vai LB", "Left Bumper LB", "LB"),
    GamepadEditableNodeInfo("btn_l3", "Nhấn cần trái L3", "Left Stick Click L3", "L3"),
    GamepadEditableNodeInfo("btn_r3", "Nhấn cần phải R3", "Right Stick Click R3", "R3"),
    GamepadEditableNodeInfo("btn_rb", "Nút vai RB", "Right Bumper RB", "RB"),
    GamepadEditableNodeInfo("btn_rt", "Cò phải RT", "Right Trigger RT", "RT"),
    GamepadEditableNodeInfo("btn_view", "Nút View (⧉)", "View Button (⧉)", "⧉ View"),
    GamepadEditableNodeInfo("btn_xbox", "Nút Xbox", "Xbox Guide Button", "Xbox"),
    GamepadEditableNodeInfo("btn_menu", "Nút Menu (☰)", "Menu Button (☰)", "☰ Menu"),
    GamepadEditableNodeInfo("left_stick", "Cần điều khiển trái (L)", "Left Analog Stick (L)", "Cần L"),
    GamepadEditableNodeInfo("right_stick", "Cần ngắm phải (R)", "Right Analog Stick (R)", "Cần R"),
    GamepadEditableNodeInfo("dpad", "Cụm phím D-Pad", "D-Pad Cross Cluster", "D-Pad"),
    GamepadEditableNodeInfo("dpad_up", "Phím Lên (▲)", "D-Pad Up (▲)", "▲"),
    GamepadEditableNodeInfo("dpad_down", "Phím Xuống (▼)", "D-Pad Down (▼)", "▼"),
    GamepadEditableNodeInfo("dpad_left", "Phím Trái (◀)", "D-Pad Left (◀)", "◀"),
    GamepadEditableNodeInfo("dpad_right", "Phím Phải (▶)", "D-Pad Right (▶)", "▶"),
    GamepadEditableNodeInfo("abxy", "Chùm nút ABXY", "ABXY Action Cluster", "ABXY"),
    GamepadEditableNodeInfo("btn_a", "Nút A (Xanh lá)", "A Button (Green)", "A"),
    GamepadEditableNodeInfo("btn_b", "Nút B (Đỏ)", "B Button (Red)", "B"),
    GamepadEditableNodeInfo("btn_x", "Nút X (Xanh dương)", "X Button (Blue)", "X"),
    GamepadEditableNodeInfo("btn_y", "Nút Y (Vàng)", "Y Button (Yellow)", "Y")
)

private fun parseControlOffsets(raw: String): Map<String, Pair<Float, Float>> {
    if (raw.isBlank()) return emptyMap()
    val result = mutableMapOf<String, Pair<Float, Float>>()
    raw.split(";").forEach { entry ->
        val parts = entry.split(":")
        if (parts.size == 2) {
            val coords = parts[1].split(",")
            if (coords.size == 2) {
                val x = coords[0].toFloatOrNull()
                val y = coords[1].toFloatOrNull()
                if (x != null && y != null) {
                    result[parts[0]] = x to y
                }
            }
        }
    }
    return result
}

private fun serializeControlOffsets(map: Map<String, Pair<Float, Float>>): String {
    return map.entries.joinToString(";") { (k, v) ->
        "${k}:${v.first.roundToInt()},${v.second.roundToInt()}"
    }
}

private fun parseButtonScales(raw: String): Map<String, Float> {
    if (raw.isBlank()) return emptyMap()
    val result = mutableMapOf<String, Float>()
    raw.split(";").forEach { entry ->
        val parts = entry.split(":")
        if (parts.size == 2) {
            val scale = parts[1].toFloatOrNull()
            if (scale != null) {
                result[parts[0]] = scale.coerceIn(0.50f, 2.00f)
            }
        }
    }
    return result
}

private fun serializeButtonScales(map: Map<String, Float>): String {
    return map.entries
        .filter { abs(it.value - 1.0f) >= 0.02f }
        .joinToString(";") { (k, v) ->
            val rounded = (v.coerceIn(0.50f, 2.00f) * 100f).roundToInt() / 100f
            "$k:$rounded"
        }
}

private fun parseHiddenButtons(raw: String): Set<String> {
    if (raw.isBlank()) return emptySet()
    return raw.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()
}

private fun serializeHiddenButtons(set: Set<String>): String {
    return set.filter { it.isNotBlank() }.sorted().joinToString(",")
}

@Composable
fun VirtualGamepadOverlay(
    webView: WebView?,
    opacity: Float? = null,
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

    val abxySplitMode = config.gamepadAbxySplitMode
    val dpadSplitMode = config.gamepadDpadSplitMode

    // Custom draggable per-button offsets (in pixels), backward-compatible with legacy cluster keys
    val customOffsets = remember(config.gamepadCustomOffsets) {
        mutableStateMapOf<String, Pair<Float, Float>>().apply {
            val parsed = parseControlOffsets(config.gamepadCustomOffsets)
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
    val customButtonScales = remember(config.gamepadButtonScales) {
        mutableStateMapOf<String, Float>().apply {
            putAll(parseButtonScales(config.gamepadButtonScales))
        }
    }

    // Hidden (deleted) buttons set
    val hiddenButtons = remember(config.gamepadHiddenButtons) {
        parseHiddenButtons(config.gamepadHiddenButtons)
    }

    fun saveCustomOffsets() {
        SettingsRepository.updateGamepadCustomOffsets(serializeControlOffsets(customOffsets))
    }

    fun saveButtonScales() {
        SettingsRepository.updateGamepadButtonScales(serializeButtonScales(customButtonScales))
    }

    fun setButtonScale(nodeId: String, newMultiplier: Float) {
        val clamped = newMultiplier.coerceIn(0.50f, 2.00f)
        customButtonScales[nodeId] = clamped
        saveButtonScales()
    }

    fun deleteButton(nodeId: String) {
        val updated = hiddenButtons.toMutableSet()
        updated.add(nodeId)
        SettingsRepository.updateGamepadHiddenButtons(serializeHiddenButtons(updated))
    }

    fun restoreButton(nodeId: String) {
        val updated = hiddenButtons.toMutableSet()
        updated.remove(nodeId)
        SettingsRepository.updateGamepadHiddenButtons(serializeHiddenButtons(updated))
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
        SettingsRepository.resetGamepadCustomOffsets()
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && amp > 0) {
                    vib?.vibrate(VibrationEffect.createOneShot(dur, amp))
                } else {
                    vib?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                }
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && amp > 0) {
                    v?.vibrate(VibrationEffect.createOneShot(dur, amp))
                }
            }
        } catch (_: Exception) {
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

    // Track right stick touch vs gyro offset so they combine smoothly
    var touchRightX by remember { mutableFloatStateOf(0f) }
    var touchRightY by remember { mutableFloatStateOf(0f) }
    var gyroRightX by remember { mutableFloatStateOf(0f) }
    var gyroRightY by remember { mutableFloatStateOf(0f) }

    fun sendStickAxesPair(xAxisIndex: Int, xVal: Float, yAxisIndex: Int, yVal: Float) {
        val rx = (xVal.coerceIn(-1f, 1f) * 100f).roundToInt() / 100f
        val ry = (yVal.coerceIn(-1f, 1f) * 100f).roundToInt() / 100f
        webView?.evaluateJavascript(
            "if(window.__cpStick){window.__cpStick($xAxisIndex,$rx,$yAxisIndex,$ry);}else if(window.dispatchEvent){window.dispatchEvent(new CustomEvent('virtual_gamepad_axes_pair',{detail:{xAxis:$xAxisIndex,xVal:$rx,yAxis:$yAxisIndex,yVal:$ry}}));}",
            null
        )
    }

    val currentGyroSens by rememberUpdatedState(config.gamepadGyroSensitivity)
    val currentWebView by rememberUpdatedState(webView)

    // Real Hardware Gyroscope Aiming for Right Stick (Axes 2 & 3)
    DisposableEffect(config.gamepadGyroEnabled) {
        if (!config.gamepadGyroEnabled) {
            gyroRightX = 0f
            gyroRightY = 0f
            return@DisposableEffect onDispose {}
        }
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val gyroSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (sensorManager == null || gyroSensor == null) {
            return@DisposableEffect onDispose {}
        }

        val listener = object : SensorEventListener {
            private var filteredX = 0f
            private var filteredY = 0f

            override fun onSensorChanged(event: SensorEvent?) {
                val ev = event ?: return
                val rawYaw = -ev.values[0] * 0.38f * currentGyroSens
                val rawPitch = ev.values[1] * 0.38f * currentGyroSens

                filteredX = filteredX * 0.65f + (if (abs(rawYaw) < 0.018f) 0f else rawYaw) * 0.35f
                filteredY = filteredY * 0.65f + (if (abs(rawPitch) < 0.018f) 0f else rawPitch) * 0.35f

                val clampedX = filteredX.coerceIn(-0.85f, 0.85f)
                val clampedY = filteredY.coerceIn(-0.85f, 0.85f)

                if (abs(clampedX - gyroRightX) >= 0.015f || abs(clampedY - gyroRightY) >= 0.015f) {
                    gyroRightX = clampedX
                    gyroRightY = clampedY
                    val combinedX = (touchRightX + gyroRightX).coerceIn(-1f, 1f)
                    val combinedY = (touchRightY + gyroRightY).coerceIn(-1f, 1f)
                    val rx = (combinedX * 100f).roundToInt() / 100f
                    val ry = (combinedY * 100f).roundToInt() / 100f
                    currentWebView?.evaluateJavascript(
                        "if(window.__cpStick){window.__cpStick(2,$rx,3,$ry);}",
                        null
                    )
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose {
            sensorManager.unregisterListener(listener)
            gyroRightX = 0f
            gyroRightY = 0f
        }
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
                        deadzone = config.gamepadDeadzone,
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
                        deadzone = config.gamepadDeadzone,
                        sensitivity = config.gamepadSensitivity,
                        label = if (config.gamepadGyroEnabled) "R•🎯" else "R",
                        enableAutoSprintL3 = false,
                        isEditMode = isEditPositionMode,
                        onSprintStateChange = {},
                        onMove = { x, y ->
                            touchRightX = x
                            touchRightY = y
                            val combinedX = (x + gyroRightX).coerceIn(-1f, 1f)
                            val combinedY = (y + gyroRightY).coerceIn(-1f, 1f)
                            sendStickAxesPair(2, combinedX, 3, combinedY)
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
                                        SettingsRepository.updateGamepadAbxySplitMode(nextSplit)
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
                                        SettingsRepository.updateGamepadDpadSplitMode(nextSplit)
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
                    SettingsRepository.updateGamepadHiddenButtons("")
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
                deadzone = config.gamepadDeadzone,
                sensitivity = config.gamepadSensitivity,
                preset = config.gamepadLayoutPreset,
                hapticLevel = config.gamepadHapticLevel,
                gyroEnabled = config.gamepadGyroEnabled,
                sprintLockEnabled = config.gamepadSprintLockEnabled,
                abxySplitMode = abxySplitMode,
                dpadSplitMode = dpadSplitMode,
                isEditPositionMode = isEditPositionMode,
                onToggleEditPosition = {
                    isEditPositionMode = !isEditPositionMode
                    if (isEditPositionMode) showControlTuner = false
                },
                onToggleAbxySplit = {
                    SettingsRepository.updateGamepadAbxySplitMode(!abxySplitMode)
                },
                onToggleDpadSplit = {
                    SettingsRepository.updateGamepadDpadSplitMode(!dpadSplitMode)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddOrManageButtonsModal(
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
private fun ControlQuickTunerPanel(
    isEn: Boolean,
    scale: Float,
    opacity: Float,
    deadzone: Float,
    sensitivity: Float,
    preset: GamepadLayoutPreset,
    hapticLevel: GamepadHapticLevel,
    gyroEnabled: Boolean,
    sprintLockEnabled: Boolean,
    abxySplitMode: Boolean,
    dpadSplitMode: Boolean,
    isEditPositionMode: Boolean,
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

            // Quick Feature Toggles: Gyro, Auto-Sprint L3, Haptics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                QuickFeatureChip(
                    modifier = Modifier.weight(1f),
                    label = if (isEn) "🎯 Gyro Aim" else "🎯 Ngắm Gyro",
                    active = gyroEnabled,
                    onClick = { SettingsRepository.updateGamepadGyroEnabled(!gyroEnabled) }
                )
                QuickFeatureChip(
                    modifier = Modifier.weight(1f),
                    label = if (isEn) "🏃 Auto L3" else "🏃 Tự chạy L3",
                    active = sprintLockEnabled,
                    onClick = { SettingsRepository.updateGamepadSprintLockEnabled(!sprintLockEnabled) }
                )
                QuickFeatureChip(
                    modifier = Modifier.weight(1f),
                    label = if (isEn) "📳 Haptic: ${hapticLevel.localizedTitle(true)}" else "📳 Rung: ${hapticLevel.localizedTitle(false)}",
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
private fun QuickFeatureChip(
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

@Composable
private fun VirtualAnalogStick(
    opacity: Float,
    scale: Float,
    deadzone: Float,
    sensitivity: Float,
    label: String,
    enableAutoSprintL3: Boolean,
    isEditMode: Boolean,
    onSprintStateChange: (Boolean) -> Unit,
    onMove: (Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val outerSizeDp: Dp = (122.dp * scale)
    val knobSizeDp: Dp = (50.dp * scale)
    val maxRadiusPx = remember(density, scale) {
        with(density) { (44.dp * scale).toPx() }.coerceAtLeast(24f)
    }

    var stickX by remember { mutableFloatStateOf(0f) }
    var stickY by remember { mutableFloatStateOf(0f) }
    var lastSentX by remember { mutableFloatStateOf(0f) }
    var lastSentY by remember { mutableFloatStateOf(0f) }
    var isTouching by remember { mutableStateOf(false) }
    var isAutoSprinting by remember { mutableStateOf(false) }

    val currentDeadzone by rememberUpdatedState(deadzone)
    val currentSensitivity by rememberUpdatedState(sensitivity)
    val currentAutoSprint by rememberUpdatedState(enableAutoSprintL3)

    fun applyDeadzoneAndCurve(rawNorm: Float): Float {
        val absVal = abs(rawNorm)
        if (absVal <= currentDeadzone) return 0f
        val rescaled = ((absVal - currentDeadzone) / (1f - currentDeadzone)).coerceIn(0f, 1f)
        val exponent = (1f / currentSensitivity.coerceIn(0.6f, 1.6f)).coerceIn(0.55f, 1.75f)
        return sign(rawNorm) * rescaled.pow(exponent)
    }

    Box(
        modifier = Modifier
            .size(outerSizeDp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = opacity * 0.62f))
            .border(
                width = if (isTouching) 2.dp else 1.5.dp,
                color = when {
                    isAutoSprinting -> DirectCyan
                    isTouching -> XboxNeonGreen
                    else -> Color(0x6600E676)
                },
                shape = CircleShape
            )
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(maxRadiusPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            isTouching = true
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f

                            fun updateFromPos(pos: Offset) {
                                val dx = pos.x - centerX
                                val dy = pos.y - centerY
                                val dist = sqrt(dx * dx + dy * dy)
                                if (dist <= maxRadiusPx) {
                                    stickX = dx
                                    stickY = dy
                                } else if (dist > 0.001f) {
                                    stickX = (dx / dist) * maxRadiusPx
                                    stickY = (dy / dist) * maxRadiusPx
                                }
                                val rawX = (stickX / maxRadiusPx).coerceIn(-1f, 1f)
                                val rawY = (stickY / maxRadiusPx).coerceIn(-1f, 1f)
                                val curvedX = applyDeadzoneAndCurve(rawX)
                                val curvedY = applyDeadzoneAndCurve(rawY)

                                if (abs(curvedX - lastSentX) >= 0.012f || abs(curvedY - lastSentY) >= 0.012f) {
                                    lastSentX = curvedX
                                    lastSentY = curvedY
                                    onMove(curvedX, curvedY)
                                }

                                // Auto-Sprint L3 when Left Stick is pushed strongly forward
                                if (currentAutoSprint) {
                                    val shouldSprint = rawY < -0.88f && abs(rawX) < 0.48f
                                    if (shouldSprint != isAutoSprinting) {
                                        isAutoSprinting = shouldSprint
                                        onSprintStateChange(shouldSprint)
                                    }
                                }
                            }

                            updateFromPos(down.position)

                            do {
                                val event = awaitPointerEvent()
                                val activeChange = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                                if (activeChange != null && activeChange.pressed) {
                                    activeChange.consume()
                                    updateFromPos(activeChange.position)
                                }
                            } while (event.changes.any { it.pressed })

                            isTouching = false
                            stickX = 0f
                            stickY = 0f
                            lastSentX = 0f
                            lastSentY = 0f
                            onMove(0f, 0f)
                            if (isAutoSprinting) {
                                isAutoSprinting = false
                                onSprintStateChange(false)
                            }
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Custom Canvas: Deadzone Ring + Active Vector Line
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = maxRadiusPx * 0.72f,
                center = c,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = DirectCyan.copy(alpha = 0.18f),
                radius = maxRadiusPx * currentDeadzone,
                center = c,
                style = Stroke(width = 1.dp.toPx())
            )
            if (isTouching && (abs(stickX) > 2f || abs(stickY) > 2f)) {
                drawLine(
                    color = if (isAutoSprinting) DirectCyan.copy(alpha = 0.75f) else XboxNeonGreen.copy(alpha = 0.65f),
                    start = c,
                    end = Offset(c.x + stickX, c.y + stickY),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        if (isAutoSprinting) {
            Text(
                text = "🏃 L3",
                color = DirectCyan,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
            )
        }

        // Inner thumb knob
        Box(
            modifier = Modifier
                .offset { IntOffset(stickX.roundToInt(), stickY.roundToInt()) }
                .size(knobSizeDp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            if (isAutoSprinting) DirectCyan.copy(alpha = opacity * 0.55f) else Color(0xFF2E4738).copy(alpha = opacity),
                            Color(0xFF121D16).copy(alpha = opacity)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    color = if (isAutoSprinting) DirectCyan else XboxNeonGreen.copy(alpha = opacity * 0.95f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = opacity),
                fontWeight = FontWeight.ExtraBold,
                fontSize = (12f * scale).sp
            )
        }
    }
}

@Composable
private fun VirtualDpadCross(
    opacity: Float,
    scale: Float,
    isEditMode: Boolean,
    onButton: (Int, Boolean) -> Unit
) {
    // 8-Way Continuous Slide D-Pad: Up=12, Down=13, Left=14, Right=15
    val dpadSize: Dp = (104.dp * scale)
    val btnSize: Dp = (34.dp * scale)
    var upPressed by remember { mutableStateOf(false) }
    var downPressed by remember { mutableStateOf(false) }
    var leftPressed by remember { mutableStateOf(false) }
    var rightPressed by remember { mutableStateOf(false) }

    fun updateDirections(newUp: Boolean, newDown: Boolean, newLeft: Boolean, newRight: Boolean) {
        if (newUp != upPressed) {
            upPressed = newUp
            onButton(12, newUp)
        }
        if (newDown != downPressed) {
            downPressed = newDown
            onButton(13, newDown)
        }
        if (newLeft != leftPressed) {
            leftPressed = newLeft
            onButton(14, newLeft)
        }
        if (newRight != rightPressed) {
            rightPressed = newRight
            onButton(15, newRight)
        }
    }

    Box(
        modifier = Modifier
            .size(dpadSize)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = opacity * 0.62f))
            .border(1.2.dp, Color(0x5500E676), RoundedCornerShape(16.dp))
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(scale) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val minDeadRadius = size.width * 0.12f

                            fun evaluatePosition(pos: Offset) {
                                val dx = pos.x - cx
                                val dy = pos.y - cy
                                val dist = sqrt(dx * dx + dy * dy)
                                if (dist < minDeadRadius) {
                                    updateDirections(false, false, false, false)
                                    return
                                }
                                val deg = atan2(dy, dx) * (180f / PI.toFloat())
                                val u = deg in -157.5f..-22.5f
                                val d = deg in 22.5f..157.5f
                                val r = deg in -67.5f..67.5f
                                val l = deg <= -112.5f || deg >= 112.5f
                                updateDirections(u, d, l, r)
                            }

                            evaluatePosition(down.position)
                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                                if (change != null && change.pressed) {
                                    change.consume()
                                    evaluatePosition(change.position)
                                }
                            } while (event.changes.any { it.pressed })

                            updateDirections(false, false, false, false)
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        DpadVisualKey(Modifier.align(Alignment.TopCenter), "▲", btnSize, upPressed, opacity)
        DpadVisualKey(Modifier.align(Alignment.BottomCenter), "▼", btnSize, downPressed, opacity)
        DpadVisualKey(Modifier.align(Alignment.CenterStart), "◀", btnSize, leftPressed, opacity)
        DpadVisualKey(Modifier.align(Alignment.CenterEnd), "▶", btnSize, rightPressed, opacity)

        Box(
            modifier = Modifier
                .size((14.dp * scale))
                .clip(CircleShape)
                .background(
                    if (upPressed || downPressed || leftPressed || rightPressed) {
                        XboxNeonGreen.copy(alpha = 0.45f)
                    } else {
                        Color.White.copy(alpha = 0.12f)
                    }
                )
        )
    }
}

@Composable
private fun StandaloneDpadButton(
    symbol: String,
    buttonIndex: Int,
    scale: Float,
    opacity: Float,
    isEditMode: Boolean,
    onButton: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val sizeDp = (38.dp * scale)
    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (isPressed) {
                    XboxNeonGreen.copy(alpha = 0.48f)
                } else {
                    Color(0xFF1E2E24).copy(alpha = opacity * 0.85f)
                }
            )
            .border(
                width = if (isPressed) 1.8.dp else 1.1.dp,
                color = if (isPressed) XboxNeonGreen else Color(0x7700E676),
                shape = RoundedCornerShape(9.dp)
            )
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(buttonIndex, scale) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                onButton(true)
                                tryAwaitRelease()
                                isPressed = false
                                onButton(false)
                            }
                        )
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = if (isPressed) Color.White else Color.White.copy(alpha = opacity),
            fontSize = (13f * scale).sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun DpadVisualKey(
    modifier: Modifier,
    symbol: String,
    sizeDp: Dp,
    isPressed: Boolean,
    opacity: Float
) {
    Box(
        modifier = modifier
            .size(sizeDp)
            .padding(2.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(
                if (isPressed) {
                    XboxNeonGreen.copy(alpha = 0.45f)
                } else {
                    Color(0xFF1E2E24).copy(alpha = opacity * 0.82f)
                }
            )
            .border(
                width = if (isPressed) 1.5.dp else 0.8.dp,
                color = if (isPressed) XboxNeonGreen else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(7.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = if (isPressed) Color.White else Color.White.copy(alpha = opacity),
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun GamepadTriggerButton(
    label: String,
    buttonIndex: Int,
    scale: Float,
    opacity: Float,
    analogDragMode: Boolean,
    isEditMode: Boolean,
    onStateChange: (Boolean, Float) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    var analogLevel by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .size(width = (64.dp * scale), height = (38.dp * scale))
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (isPressed) XboxNeonGreen.copy(alpha = 0.48f) else Color(0xFF263D2E).copy(alpha = opacity * 0.9f),
                        Color(0xFF0F1A13).copy(alpha = opacity * 0.9f)
                    )
                )
            )
            .border(
                width = if (isPressed) 1.8.dp else 1.2.dp,
                color = XboxNeonGreen.copy(alpha = opacity * 0.8f),
                shape = RoundedCornerShape(10.dp)
            )
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(buttonIndex, analogDragMode) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            isPressed = true
                            analogLevel = 1f
                            onStateChange(true, 1f)

                            do {
                                val event = awaitPointerEvent()
                                val ch = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                                if (ch != null && ch.pressed && analogDragMode) {
                                    ch.consume()
                                    val dy = (ch.position.y - down.position.y).coerceIn(0f, 80f)
                                    val computed = (0.35f + (dy / 80f) * 0.65f).coerceIn(0.2f, 1f)
                                    analogLevel = computed
                                    onStateChange(true, computed)
                                }
                            } while (event.changes.any { it.pressed })

                            isPressed = false
                            analogLevel = 0f
                            onStateChange(false, 0f)
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (analogDragMode && isPressed) "$label ${(analogLevel * 100).roundToInt()}%" else label,
            color = Color.White.copy(alpha = opacity),
            fontWeight = FontWeight.ExtraBold,
            fontSize = (11.5f * scale).sp
        )
    }
}

@Composable
private fun GamepadShoulderButton(
    label: String,
    buttonIndex: Int,
    scale: Float,
    opacity: Float,
    isEditMode: Boolean,
    onStateChange: (Boolean) -> Unit
) {
    var isHeld by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(width = (58.dp * scale), height = (38.dp * scale))
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (isHeld) XboxNeonGreen.copy(alpha = 0.45f) else Color(0xFF263D2E).copy(alpha = opacity * 0.9f),
                        Color(0xFF0F1A13).copy(alpha = opacity * 0.9f)
                    )
                )
            )
            .border(
                width = if (isHeld) 1.8.dp else 1.2.dp,
                color = XboxNeonGreen.copy(alpha = opacity * 0.75f),
                shape = RoundedCornerShape(10.dp)
            )
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(buttonIndex) {
                        detectTapGestures(
                            onPress = {
                                isHeld = true
                                onStateChange(true)
                                tryAwaitRelease()
                                isHeld = false
                                onStateChange(false)
                            }
                        )
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = opacity),
            fontWeight = FontWeight.Bold,
            fontSize = (11.5f * scale).sp
        )
    }
}

@Composable
private fun GamepadThumbClickMiniButton(
    label: String,
    buttonIndex: Int,
    scale: Float,
    opacity: Float,
    isEditMode: Boolean,
    onStateChange: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(width = (42.dp * scale), height = (34.dp * scale))
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (isPressed) {
                    XboxNeonGreen.copy(alpha = 0.42f)
                } else {
                    Color.Black.copy(alpha = opacity * 0.68f)
                }
            )
            .border(
                width = 1.2.dp,
                color = if (isPressed) XboxNeonGreen else Color(0x7700E676),
                shape = RoundedCornerShape(9.dp)
            )
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(buttonIndex) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                onStateChange(true)
                                tryAwaitRelease()
                                isPressed = false
                                onStateChange(false)
                            }
                        )
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = opacity),
            fontWeight = FontWeight.ExtraBold,
            fontSize = (10.5f * scale).sp
        )
    }
}

@Composable
private fun GamepadSmallButton(
    label: String,
    index: Int,
    scale: Float,
    opacity: Float,
    isEditMode: Boolean,
    onButton: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) XboxNeonGreen.copy(alpha = 0.35f) else Color.Black.copy(alpha = opacity * 0.7f))
            .border(1.dp, if (isPressed) XboxNeonGreen else Color(0x6600E676), RoundedCornerShape(8.dp))
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(index) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                onButton(true)
                                tryAwaitRelease()
                                isPressed = false
                                onButton(false)
                            }
                        )
                    }
                }
            )
            .padding(horizontal = (8.dp * scale), vertical = (4.dp * scale)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = opacity),
            fontSize = (10f * scale).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GamepadXboxNexusButton(
    scale: Float,
    opacity: Float,
    isEditMode: Boolean,
    onButton: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(36.dp * scale)
            .clip(CircleShape)
            .background(if (isPressed) XboxNeonGreen.copy(alpha = 0.35f) else Color.Black.copy(alpha = opacity))
            .border(2.dp, XboxNeonGreen.copy(alpha = opacity), CircleShape)
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                onButton(true)
                                tryAwaitRelease()
                                isPressed = false
                                onButton(false)
                            }
                        )
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "✕",
            color = XboxNeonGreen.copy(alpha = opacity),
            fontSize = (18f * scale).sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun StandaloneAbxyButton(
    label: String,
    accentColor: Color,
    buttonIndex: Int,
    scale: Float,
    opacity: Float,
    isEditMode: Boolean,
    onButton: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val sizeDp = (46.dp * scale)
    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        if (isPressed) accentColor.copy(alpha = 0.78f) else accentColor.copy(alpha = opacity * 0.38f),
                        Color.Black.copy(alpha = opacity * 0.92f)
                    )
                )
            )
            .border(
                width = if (isPressed) 2.4.dp else 1.8.dp,
                color = if (isPressed) Color.White else accentColor.copy(alpha = opacity),
                shape = CircleShape
            )
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(buttonIndex, scale) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                onButton(true)
                                tryAwaitRelease()
                                isPressed = false
                                onButton(false)
                            }
                        )
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) Color.White else accentColor.copy(alpha = opacity),
            fontWeight = FontWeight.ExtraBold,
            fontSize = (16f * scale).sp
        )
    }
}

@Composable
private fun VirtualAbxyPad(
    opacity: Float,
    scale: Float,
    isEditMode: Boolean,
    onButton: (Int, Boolean) -> Unit
) {
    // Slide & Multi-Button Combo ABXY Pad: A=0 (Bottom), B=1 (Right), X=2 (Left), Y=3 (Top)
    val padSize: Dp = (132.dp * scale)
    val buttonSize: Dp = (44.dp * scale)

    var aPressed by remember { mutableStateOf(false) }
    var bPressed by remember { mutableStateOf(false) }
    var xPressed by remember { mutableStateOf(false) }
    var yPressed by remember { mutableStateOf(false) }

    fun updateAbxyState(newA: Boolean, newB: Boolean, newX: Boolean, newY: Boolean) {
        if (newA != aPressed) {
            aPressed = newA
            onButton(0, newA)
        }
        if (newB != bPressed) {
            bPressed = newB
            onButton(1, newB)
        }
        if (newX != xPressed) {
            xPressed = newX
            onButton(2, newX)
        }
        if (newY != yPressed) {
            yPressed = newY
            onButton(3, newY)
        }
    }

    Box(
        modifier = Modifier
            .size(padSize)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = opacity * 0.60f))
            .border(1.5.dp, Color(0x6600E676), CircleShape)
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(scale) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()

                            fun evaluateTouchPoints(points: List<Offset>) {
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                val hitRadius = w * 0.28f
                                val posY = Offset(w * 0.50f, h * 0.18f) // Top (Y=3)
                                val posA = Offset(w * 0.50f, h * 0.82f) // Bottom (A=0)
                                val posX = Offset(w * 0.18f, h * 0.50f) // Left (X=2)
                                val posB = Offset(w * 0.82f, h * 0.50f) // Right (B=1)

                                var hitA = false
                                var hitB = false
                                var hitX = false
                                var hitY = false

                                for (pt in points) {
                                    if (hypotDist(pt, posA) <= hitRadius) hitA = true
                                    if (hypotDist(pt, posB) <= hitRadius) hitB = true
                                    if (hypotDist(pt, posX) <= hitRadius) hitX = true
                                    if (hypotDist(pt, posY) <= hitRadius) hitY = true
                                }
                                updateAbxyState(hitA, hitB, hitX, hitY)
                            }

                            evaluateTouchPoints(listOf(down.position))

                            do {
                                val event = awaitPointerEvent()
                                val pressedPoints = event.changes.filter { it.pressed }.map {
                                    it.consume()
                                    it.position
                                }
                                if (pressedPoints.isNotEmpty()) {
                                    evaluateTouchPoints(pressedPoints)
                                }
                            } while (event.changes.any { it.pressed })

                            updateAbxyState(false, false, false, false)
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        AbxyVisualButton(Modifier.align(Alignment.TopCenter), "Y", Color(0xFFFFD166), buttonSize, yPressed, opacity)
        AbxyVisualButton(Modifier.align(Alignment.BottomCenter), "A", Color(0xFF00E676), buttonSize, aPressed, opacity)
        AbxyVisualButton(Modifier.align(Alignment.CenterStart), "X", Color(0xFF00B4D8), buttonSize, xPressed, opacity)
        AbxyVisualButton(Modifier.align(Alignment.CenterEnd), "B", Color(0xFFFF3366), buttonSize, bPressed, opacity)
    }
}

private fun hypotDist(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}

@Composable
private fun AbxyVisualButton(
    modifier: Modifier,
    label: String,
    accentColor: Color,
    sizeDp: Dp,
    isPressed: Boolean,
    opacity: Float
) {
    Box(
        modifier = modifier
            .size(sizeDp)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        if (isPressed) accentColor.copy(alpha = 0.78f) else accentColor.copy(alpha = opacity * 0.35f),
                        Color.Black.copy(alpha = opacity * 0.92f)
                    )
                )
            )
            .border(
                width = if (isPressed) 2.4.dp else 1.8.dp,
                color = if (isPressed) Color.White else accentColor.copy(alpha = opacity),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) Color.White else accentColor.copy(alpha = opacity),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp
        )
    }
}
