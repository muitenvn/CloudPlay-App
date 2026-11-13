package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.WebView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GamepadHapticLevel
import com.example.data.GamepadLayoutPreset
import com.example.data.SettingsRepository
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VirtualGamepadOverlay(
    webView: WebView?,
    opacity: Float? = null,
    gameKey: String? = null,
    gameDisplayName: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val config by SettingsRepository.config.collectAsState()

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

    var isEditPositionMode by remember { mutableStateOf(false) }
    var isPositionLocked by remember { mutableStateOf(false) }
    var activePopoverNodeId by remember { mutableStateOf<String?>(null) }
    var selectedButtonId by remember { mutableStateOf<String?>(null) }
    var showAddButtonModal by remember { mutableStateOf(false) }
    var showEditGrid by remember { mutableStateOf(true) }

    var rootSizePx by remember { mutableStateOf(IntSize.Zero) }
    var rootPosInWindow by remember { mutableStateOf(Offset.Zero) }
    val nodeBoundsMap = remember { mutableStateMapOf<String, Rect>() }

    val normGameKey = remember(gameKey) { SettingsRepository.normalizeGameLayoutKey(gameKey) }
    val perGameLayout = remember(normGameKey, config.usePerGameGamepadLayout, config.perGameLayoutsJson) {
        if (config.usePerGameGamepadLayout && normGameKey.isNotEmpty()) {
            SettingsRepository.getPerGameLayout(normGameKey, config)
        } else null
    }

    val effectiveOffsetsRaw = perGameLayout?.offsets ?: config.gamepadCustomOffsets
    val effectiveScalesRaw = perGameLayout?.scales ?: config.gamepadButtonScales
    val effectiveOpacitiesRaw = perGameLayout?.opacities ?: config.gamepadButtonOpacities
    val effectiveHapticsRaw = perGameLayout?.haptics ?: config.gamepadButtonHaptics
    val effectiveHiddenRaw = perGameLayout?.hiddenButtons ?: config.gamepadHiddenButtons
    val effectiveBehaviorsRaw = perGameLayout?.behaviors ?: config.gamepadButtonBehaviors
    val effectiveCustomButtonsRaw = perGameLayout?.customButtons ?: config.gamepadCustomButtons
    val abxySplitMode = perGameLayout?.abxySplit ?: config.gamepadAbxySplitMode
    val dpadSplitMode = perGameLayout?.dpadSplit ?: config.gamepadDpadSplitMode

    val customOffsets = remember(effectiveOffsetsRaw, normGameKey, config.usePerGameGamepadLayout) {
        mutableStateMapOf<String, Pair<Float, Float>>().apply {
            val parsed = parseControlOffsets(effectiveOffsetsRaw)
            putAll(parsed)
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

    val customButtonScales = remember(effectiveScalesRaw, normGameKey, config.usePerGameGamepadLayout) {
        mutableStateMapOf<String, Float>().apply {
            putAll(parseButtonScales(effectiveScalesRaw))
        }
    }

    val customButtonOpacities = remember(effectiveOpacitiesRaw, normGameKey, config.usePerGameGamepadLayout) {
        mutableStateMapOf<String, Float>().apply {
            putAll(parseButtonOpacities(effectiveOpacitiesRaw))
        }
    }

    val customButtonHaptics = remember(effectiveHapticsRaw, normGameKey, config.usePerGameGamepadLayout) {
        mutableStateMapOf<String, GamepadHapticLevel>().apply {
            putAll(parseButtonHaptics(effectiveHapticsRaw))
        }
    }

    val customButtonBehaviors = remember(effectiveBehaviorsRaw, normGameKey, config.usePerGameGamepadLayout) {
        mutableStateMapOf<String, GamepadButtonBehavior>().apply {
            putAll(parseButtonBehaviors(effectiveBehaviorsRaw))
        }
    }

    val customButtonsList = remember(effectiveCustomButtonsRaw, normGameKey, config.usePerGameGamepadLayout) {
        mutableStateListOf<CustomGamepadButtonDef>().apply {
            addAll(parseCustomButtons(effectiveCustomButtonsRaw))
        }
    }

    val hiddenButtons = remember(effectiveHiddenRaw, normGameKey, config.usePerGameGamepadLayout) {
        parseHiddenButtons(effectiveHiddenRaw)
    }

    fun releaseAllVirtualInputs() {
        webView?.evaluateJavascript(
            "if(window.__cpReleaseAll){window.__cpReleaseAll();}",
            null
        )
    }

    LaunchedEffect(isEditPositionMode) {
        if (isEditPositionMode) {
            releaseAllVirtualInputs()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            releaseAllVirtualInputs()
        }
    }

    fun saveCustomOffsets() {
        SettingsRepository.updateGamepadCustomOffsets(serializeControlOffsets(customOffsets), normGameKey)
    }

    fun saveButtonScales() {
        SettingsRepository.updateGamepadButtonScales(serializeButtonScales(customButtonScales), normGameKey)
    }

    fun saveButtonOpacities() {
        SettingsRepository.updateGamepadButtonOpacities(serializeButtonOpacities(customButtonOpacities), normGameKey)
    }

    fun saveButtonHaptics() {
        SettingsRepository.updateGamepadButtonHaptics(serializeButtonHaptics(customButtonHaptics), normGameKey)
    }

    fun saveButtonBehaviors() {
        SettingsRepository.updateGamepadButtonBehaviors(serializeButtonBehaviors(customButtonBehaviors), normGameKey)
    }

    fun saveCustomButtonsList() {
        SettingsRepository.updateGamepadCustomButtons(serializeCustomButtons(customButtonsList), normGameKey)
    }

    fun setButtonScale(nodeId: String, newMultiplier: Float) {
        val clamped = newMultiplier.coerceIn(0.50f, 2.00f)
        customButtonScales[nodeId] = clamped
        saveButtonScales()
    }

    fun setButtonOpacity(nodeId: String, newOpacity: Float) {
        val clamped = newOpacity.coerceIn(0.20f, 1.00f)
        customButtonOpacities[nodeId] = clamped
        saveButtonOpacities()
    }

    fun setButtonHaptic(nodeId: String, level: GamepadHapticLevel) {
        customButtonHaptics[nodeId] = level
        saveButtonHaptics()
    }

    fun setButtonBehavior(nodeId: String, behavior: GamepadButtonBehavior) {
        customButtonBehaviors[nodeId] = behavior
        saveButtonBehaviors()
    }

    fun updateCustomButtonDef(updatedDef: CustomGamepadButtonDef) {
        val idx = customButtonsList.indexOfFirst { it.id == updatedDef.id }
        if (idx >= 0) {
            customButtonsList[idx] = updatedDef
            saveCustomButtonsList()
        }
    }

    fun addBlankCustomButton() {
        var nextIndex = 1
        val existingIds = customButtonsList.map { it.id }.toSet()
        while ("custom_$nextIndex" in existingIds) {
            nextIndex++
        }
        val newId = "custom_$nextIndex"
        val staggerX = (((nextIndex - 1) % 5) - 2) * 38f
        val staggerY = (((nextIndex - 1) / 5) % 3 - 1) * 36f
        if (staggerX != 0f || staggerY != 0f) {
            customOffsets[newId] = staggerX to staggerY
            saveCustomOffsets()
        }
        customButtonsList.add(
            CustomGamepadButtonDef(
                id = newId,
                isRound = true,
                targetButtons = setOf(0)
            )
        )
        saveCustomButtonsList()
        showAddButtonModal = false
        selectedButtonId = newId
        activePopoverNodeId = newId
    }

    fun removeCustomButton(nodeId: String) {
        customButtonsList.removeAll { it.id == nodeId }
        customOffsets.remove(nodeId)
        customButtonScales.remove(nodeId)
        customButtonOpacities.remove(nodeId)
        customButtonHaptics.remove(nodeId)
        customButtonBehaviors.remove(nodeId)
        nodeBoundsMap.remove(nodeId)
        saveCustomButtonsList()
        saveCustomOffsets()
        saveButtonScales()
        saveButtonOpacities()
        saveButtonHaptics()
        saveButtonBehaviors()
    }

    fun deleteButton(nodeId: String) {
        if (isCustomNodeId(nodeId)) {
            removeCustomButton(nodeId)
            return
        }
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
        customButtonOpacities.remove(nodeId)
        customButtonHaptics.remove(nodeId)
        customButtonBehaviors.remove(nodeId)
        if (isCustomNodeId(nodeId)) {
            val idx = customButtonsList.indexOfFirst { it.id == nodeId }
            if (idx >= 0) {
                customButtonsList[idx] = CustomGamepadButtonDef(
                    id = nodeId,
                    isRound = true,
                    targetButtons = setOf(0)
                )
                saveCustomButtonsList()
            }
        }
        saveCustomOffsets()
        saveButtonScales()
        saveButtonOpacities()
        saveButtonHaptics()
        saveButtonBehaviors()
    }

    fun resetAllCustomizations() {
        customOffsets.clear()
        customButtonScales.clear()
        customButtonOpacities.clear()
        customButtonHaptics.clear()
        customButtonBehaviors.clear()
        customButtonsList.clear()
        SettingsRepository.resetGamepadCustomOffsets(normGameKey)
    }

    fun switchCurrentGameToGlobalLayout() {
        SettingsRepository.useGlobalLayoutForGame(normGameKey)
    }

    fun nodeScale(nodeId: String, baseScale: Float = effectiveScale): Float {
        val mult = customButtonScales[nodeId] ?: 1.0f
        return (baseScale * mult).coerceIn(0.45f, 2.50f)
    }

    fun nodeOpacity(nodeId: String, baseOpacity: Float = effectiveOpacity): Float {
        return (customButtonOpacities[nodeId] ?: baseOpacity).coerceIn(0.20f, 1.00f)
    }

    fun nodeHaptic(nodeId: String): GamepadHapticLevel {
        return customButtonHaptics[nodeId] ?: config.gamepadHapticLevel
    }

    fun nodeBehavior(nodeId: String): GamepadButtonBehavior {
        return customButtonBehaviors[nodeId] ?: GamepadButtonBehavior()
    }

    fun vibrateWithLevel(level: GamepadHapticLevel, stronger: Boolean = false) {
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

    fun vibrate(nodeId: String? = null, stronger: Boolean = false) {
        val level = if (nodeId != null) nodeHaptic(nodeId) else config.gamepadHapticLevel
        vibrateWithLevel(level, stronger)
    }

    fun dispatchRawButtonJs(buttonIndex: Int, pressed: Boolean, analogValue: Float? = null) {
        val v = analogValue ?: if (pressed) 1f else 0f
        val rv = (v * 100f).roundToInt() / 100f
        webView?.evaluateJavascript(
            "if(window.__cpBtn){window.__cpBtn($buttonIndex,$pressed,$rv);}else if(window.dispatchEvent){window.dispatchEvent(new CustomEvent('virtual_gamepad_button',{detail:{index:$buttonIndex,pressed:$pressed,value:$rv}}));}",
            null
        )
    }

    fun sendButtonEvent(
        buttonIndex: Int,
        pressed: Boolean,
        analogValue: Float? = null,
        nodeId: String? = null
    ) {
        val extraTargets = if (nodeId != null) nodeBehavior(nodeId).extraButtons else emptySet()
        val anyHeavy = buttonIndex in setOf(6, 7, 10, 11) || extraTargets.any { it in setOf(6, 7, 10, 11) }
        if (pressed) vibrate(nodeId = nodeId, stronger = anyHeavy)
        dispatchRawButtonJs(buttonIndex, pressed, analogValue)
        extraTargets.forEach { extraIdx ->
            if (extraIdx != buttonIndex && extraIdx in 0..16) {
                dispatchRawButtonJs(extraIdx, pressed, if (pressed) 1f else 0f)
            }
        }
    }

    fun sendCustomButtonEvent(customDef: CustomGamepadButtonDef, pressed: Boolean) {
        val targets = customDef.targetButtons.filter { it in 0..16 }
        if (targets.isEmpty()) return
        val anyHeavy = targets.any { it in setOf(6, 7, 10, 11) }
        if (pressed) vibrate(nodeId = customDef.id, stronger = anyHeavy)
        targets.forEach { targetIdx ->
            dispatchRawButtonJs(targetIdx, pressed, if (pressed) 1f else 0f)
        }
    }

    fun sendStickAxesPair(xAxisIndex: Int, xVal: Float, yAxisIndex: Int, yVal: Float) {
        val rx = (xVal.coerceIn(-1f, 1f) * 100f).roundToInt() / 100f
        val ry = (yVal.coerceIn(-1f, 1f) * 100f).roundToInt() / 100f
        webView?.evaluateJavascript(
            "if(window.__cpStick){window.__cpStick($xAxisIndex,$rx,$yAxisIndex,$ry);}else if(window.dispatchEvent){window.dispatchEvent(new CustomEvent('virtual_gamepad_axes_pair',{detail:{xAxis:$xAxisIndex,xVal:$rx,yAxis:$yAxisIndex,yVal:$ry}}));}",
            null
        )
    }

    fun Modifier.trackNodeBounds(key: String): Modifier = this.onGloballyPositioned { coords ->
        val pos = coords.positionInWindow() - rootPosInWindow
        val next = Rect(pos.x, pos.y, pos.x + coords.size.width, pos.y + coords.size.height)
        val old = nodeBoundsMap[key]
        if (old == null ||
            abs(old.left - next.left) > 1.5f ||
            abs(old.top - next.top) > 1.5f ||
            abs(old.width - next.width) > 1.5f ||
            abs(old.height - next.height) > 1.5f
        ) {
            nodeBoundsMap[key] = next
        }
    }

    fun Modifier.draggableGamepadNode(key: String): Modifier {
        val offsetPair = customOffsets[key] ?: (0f to 0f)
        val isSelected = isEditPositionMode && (selectedButtonId == key || activePopoverNodeId == key)
        val base = this
            .offset { IntOffset(offsetPair.first.roundToInt(), offsetPair.second.roundToInt()) }
            .trackNodeBounds(key)

        return if (!isEditPositionMode) {
            base
        } else {
            val editOutlineModifier = if (isSelected) {
                base
                    .border(2.dp, HudAccent, RoundedCornerShape(14.dp))
                    .background(HudAccent.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            } else {
                base.drawBehind {
                    val strokePx = 1.8.dp.toPx()
                    val dashPx = 6.dp.toPx()
                    val gapPx = 4.dp.toPx()
                    val radiusPx = 14.dp.toPx()
                    drawRoundRect(
                        color = HudAccent.copy(alpha = 0.80f),
                        cornerRadius = CornerRadius(radiusPx, radiusPx),
                        style = Stroke(
                            width = strokePx,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx), 0f)
                        )
                    )
                }
            }

            editOutlineModifier
                .pointerInput(key, isEditPositionMode) {
                    detectTapGestures(
                        onTap = {
                            vibrate(key)
                            selectedButtonId = key
                        },
                        onDoubleTap = {
                            vibrate(key)
                            selectedButtonId = key
                            activePopoverNodeId = key
                            showAddButtonModal = false
                        }
                    )
                }
                .then(
                    if (isPositionLocked) {
                        Modifier
                    } else {
                        Modifier.pointerInput(key, isEditPositionMode, isPositionLocked) {
                            detectDragGestures(
                                onDragStart = { selectedButtonId = key },
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
                )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                rootSizePx = coords.size
                val nextPos = coords.positionInWindow()
                if (abs(rootPosInWindow.x - nextPos.x) > 1f || abs(rootPosInWindow.y - nextPos.y) > 1f) {
                    rootPosInWindow = nextPos
                }
            }
            .testTag("virtual_gamepad_overlay")
    ) {
        // Alignment Grid & Center Crosshair when editing button positions
        if (isEditPositionMode && showEditGrid) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("virtual_gamepad_alignment_grid")
            ) {
                drawRect(color = Color(0x40070B09))
                val stepPx = 32.dp.toPx().coerceAtLeast(16f)
                val gridColor = Color.White.copy(alpha = 0.07f)
                val centerColor = HudAccent.copy(alpha = 0.35f)

                var x = stepPx
                while (x < size.width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += stepPx
                }
                var y = stepPx
                while (y < size.height) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += stepPx
                }

                val midX = size.width / 2f
                val midY = size.height / 2f
                drawLine(centerColor, Offset(midX, 0f), Offset(midX, size.height), strokeWidth = 1.5f)
                drawLine(centerColor, Offset(0f, midY), Offset(size.width, midY), strokeWidth = 1.5f)
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
            // Left Shoulder Buttons: LT, LB, L3
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if ("btn_lt" !in hiddenButtons) {
                    val ltBehavior = nodeBehavior("btn_lt")
                    val ltAnalogDrag = ltBehavior.analogSwipe
                        ?: (config.gamepadLayoutPreset == GamepadLayoutPreset.RACING)
                    Box(modifier = Modifier.draggableGamepadNode("btn_lt")) {
                        GamepadTriggerButton(
                            label = "LT",
                            buttonIndex = 6,
                            scale = nodeScale("btn_lt"),
                            opacity = nodeOpacity("btn_lt"),
                            widthScale = ltBehavior.widthScale,
                            heightScale = ltBehavior.heightScale,
                            analogDragMode = ltAnalogDrag,
                            pressMode = ltBehavior.pressMode,
                            turboHz = ltBehavior.turboHz,
                            isEditMode = isEditPositionMode,
                            onStateChange = { pressed, analogVal ->
                                sendButtonEvent(6, pressed, analogVal, nodeId = "btn_lt")
                            }
                        )
                    }
                }
                if ("btn_lb" !in hiddenButtons) {
                    val lbBehavior = nodeBehavior("btn_lb")
                    Box(modifier = Modifier.draggableGamepadNode("btn_lb")) {
                        GamepadShoulderButton(
                            label = "LB",
                            buttonIndex = 4,
                            scale = nodeScale("btn_lb"),
                            opacity = nodeOpacity("btn_lb"),
                            widthScale = lbBehavior.widthScale,
                            heightScale = lbBehavior.heightScale,
                            pressMode = lbBehavior.pressMode,
                            turboHz = lbBehavior.turboHz,
                            isEditMode = isEditPositionMode,
                            onStateChange = { sendButtonEvent(4, it, nodeId = "btn_lb") }
                        )
                    }
                }
                if ("btn_l3" !in hiddenButtons) {
                    val l3Behavior = nodeBehavior("btn_l3")
                    Box(modifier = Modifier.draggableGamepadNode("btn_l3")) {
                        GamepadThumbClickMiniButton(
                            label = "L3",
                            buttonIndex = 10,
                            scale = nodeScale("btn_l3"),
                            opacity = nodeOpacity("btn_l3"),
                            widthScale = l3Behavior.widthScale,
                            heightScale = l3Behavior.heightScale,
                            pressMode = l3Behavior.pressMode,
                            turboHz = l3Behavior.turboHz,
                            isEditMode = isEditPositionMode,
                            onStateChange = { sendButtonEvent(10, it, nodeId = "btn_l3") }
                        )
                    }
                }
            }

            // Right Shoulder Buttons: R3, RB, RT
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if ("btn_r3" !in hiddenButtons) {
                    val r3Behavior = nodeBehavior("btn_r3")
                    Box(modifier = Modifier.draggableGamepadNode("btn_r3")) {
                        GamepadThumbClickMiniButton(
                            label = "R3",
                            buttonIndex = 11,
                            scale = nodeScale("btn_r3"),
                            opacity = nodeOpacity("btn_r3"),
                            widthScale = r3Behavior.widthScale,
                            heightScale = r3Behavior.heightScale,
                            pressMode = r3Behavior.pressMode,
                            turboHz = r3Behavior.turboHz,
                            isEditMode = isEditPositionMode,
                            onStateChange = { sendButtonEvent(11, it, nodeId = "btn_r3") }
                        )
                    }
                }
                if ("btn_rb" !in hiddenButtons) {
                    val rbBehavior = nodeBehavior("btn_rb")
                    Box(modifier = Modifier.draggableGamepadNode("btn_rb")) {
                        GamepadShoulderButton(
                            label = "RB",
                            buttonIndex = 5,
                            scale = nodeScale("btn_rb"),
                            opacity = nodeOpacity("btn_rb"),
                            widthScale = rbBehavior.widthScale,
                            heightScale = rbBehavior.heightScale,
                            pressMode = rbBehavior.pressMode,
                            turboHz = rbBehavior.turboHz,
                            isEditMode = isEditPositionMode,
                            onStateChange = { sendButtonEvent(5, it, nodeId = "btn_rb") }
                        )
                    }
                }
                if ("btn_rt" !in hiddenButtons) {
                    val rtBehavior = nodeBehavior("btn_rt")
                    val rtAnalogDrag = rtBehavior.analogSwipe
                        ?: (config.gamepadLayoutPreset == GamepadLayoutPreset.RACING)
                    Box(modifier = Modifier.draggableGamepadNode("btn_rt")) {
                        GamepadTriggerButton(
                            label = "RT",
                            buttonIndex = 7,
                            scale = nodeScale("btn_rt"),
                            opacity = nodeOpacity("btn_rt"),
                            widthScale = rtBehavior.widthScale,
                            heightScale = rtBehavior.heightScale,
                            analogDragMode = rtAnalogDrag,
                            pressMode = rtBehavior.pressMode,
                            turboHz = rtBehavior.turboHz,
                            isEditMode = isEditPositionMode,
                            onStateChange = { pressed, analogVal ->
                                sendButtonEvent(7, pressed, analogVal, nodeId = "btn_rt")
                            }
                        )
                    }
                }
            }
        }

        // Center Top System Bar: View, Xbox Nexus, Menu + Tune ⚙ Icon
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if ("btn_view" !in hiddenButtons) {
                val viewBehavior = nodeBehavior("btn_view")
                Box(modifier = Modifier.draggableGamepadNode("btn_view")) {
                    GamepadSmallButton(
                        label = "⧉ View",
                        index = 8,
                        scale = customButtonScales["btn_view"] ?: 1.0f,
                        opacity = nodeOpacity("btn_view"),
                        widthScale = viewBehavior.widthScale,
                        heightScale = viewBehavior.heightScale,
                        pressMode = viewBehavior.pressMode,
                        turboHz = viewBehavior.turboHz,
                        isEditMode = isEditPositionMode
                    ) { sendButtonEvent(8, it, nodeId = "btn_view") }
                }
            }
            if ("btn_xbox" !in hiddenButtons) {
                val xboxBehavior = nodeBehavior("btn_xbox")
                Box(modifier = Modifier.draggableGamepadNode("btn_xbox")) {
                    GamepadXboxNexusButton(
                        scale = customButtonScales["btn_xbox"] ?: 1.0f,
                        opacity = nodeOpacity("btn_xbox"),
                        widthScale = xboxBehavior.widthScale,
                        heightScale = xboxBehavior.heightScale,
                        pressMode = xboxBehavior.pressMode,
                        turboHz = xboxBehavior.turboHz,
                        isEditMode = isEditPositionMode
                    ) { sendButtonEvent(16, it, nodeId = "btn_xbox") }
                }
            }
            if ("btn_menu" !in hiddenButtons) {
                val menuBehavior = nodeBehavior("btn_menu")
                Box(modifier = Modifier.draggableGamepadNode("btn_menu")) {
                    GamepadSmallButton(
                        label = "☰ Menu",
                        index = 9,
                        scale = customButtonScales["btn_menu"] ?: 1.0f,
                        opacity = nodeOpacity("btn_menu"),
                        widthScale = menuBehavior.widthScale,
                        heightScale = menuBehavior.heightScale,
                        pressMode = menuBehavior.pressMode,
                        turboHz = menuBehavior.turboHz,
                        isEditMode = isEditPositionMode
                    ) { sendButtonEvent(9, it, nodeId = "btn_menu") }
                }
            }

            // Tune ⚙ Icon Button (matching mockup .tune: tap or double-tap opens Global "Cả tay cầm" popover)
            val isAllOpen = activePopoverNodeId == "all"
            val tuneDesc = config.tr("Tinh chỉnh tay cầm ảo", "Virtual gamepad settings")
            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 30.dp)
                    .trackNodeBounds("all")
                    .alpha(if (isEditPositionMode && !isAllOpen) 0.45f else 1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isAllOpen) HudSelBg else PadBtnBg)
                    .border(
                        1.dp,
                        if (isAllOpen) HudAccent else PadBtnBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                vibrate()
                                activePopoverNodeId = if (activePopoverNodeId == "all") null else "all"
                                showAddButtonModal = false
                            },
                            onDoubleTap = {
                                vibrate()
                                activePopoverNodeId = "all"
                                showAddButtonModal = false
                            }
                        )
                    }
                    .semantics { contentDescription = tuneDesc }
                    .testTag("virtual_gamepad_tune_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = tuneDesc,
                    tint = if (isAllOpen) HudAccent else PadBtnText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Left Controls: Left Analog Stick & D-Pad
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
                        opacity = nodeOpacity("left_stick"),
                        scale = nodeScale("left_stick"),
                        innerDeadzone = config.leftStickInnerDeadzone,
                        outerDeadzone = config.leftStickOuterDeadzone,
                        sensitivity = config.leftStickSensitivity,
                        responseCurve = config.leftStickCurve,
                        label = "L",
                        enableAutoSprintL3 = config.gamepadSprintLockEnabled,
                        isEditMode = isEditPositionMode,
                        onSprintStateChange = { sprinting ->
                            sendButtonEvent(10, sprinting, nodeId = "left_stick")
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
                            opacity = nodeOpacity("dpad"),
                            scale = nodeScale("dpad"),
                            isEditMode = isEditPositionMode,
                            onButton = { index, pressed -> sendButtonEvent(index, pressed, nodeId = "dpad") }
                        )
                    }
                }
            } else {
                val splitBaseSize = 104.dp * effectiveScale
                Box(modifier = Modifier.size(splitBaseSize)) {
                    if ("dpad_up" !in hiddenButtons) {
                        val b = nodeBehavior("dpad_up")
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .draggableGamepadNode("dpad_up")
                        ) {
                            StandaloneDpadButton(
                                symbol = "▲",
                                buttonIndex = 12,
                                scale = nodeScale("dpad_up"),
                                opacity = nodeOpacity("dpad_up"),
                                widthScale = b.widthScale,
                                heightScale = b.heightScale,
                                pressMode = b.pressMode,
                                turboHz = b.turboHz,
                                isEditMode = isEditPositionMode,
                                onButton = { sendButtonEvent(12, it, nodeId = "dpad_up") }
                            )
                        }
                    }
                    if ("dpad_down" !in hiddenButtons) {
                        val b = nodeBehavior("dpad_down")
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .draggableGamepadNode("dpad_down")
                        ) {
                            StandaloneDpadButton(
                                symbol = "▼",
                                buttonIndex = 13,
                                scale = nodeScale("dpad_down"),
                                opacity = nodeOpacity("dpad_down"),
                                widthScale = b.widthScale,
                                heightScale = b.heightScale,
                                pressMode = b.pressMode,
                                turboHz = b.turboHz,
                                isEditMode = isEditPositionMode,
                                onButton = { sendButtonEvent(13, it, nodeId = "dpad_down") }
                            )
                        }
                    }
                    if ("dpad_left" !in hiddenButtons) {
                        val b = nodeBehavior("dpad_left")
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .draggableGamepadNode("dpad_left")
                        ) {
                            StandaloneDpadButton(
                                symbol = "◀",
                                buttonIndex = 14,
                                scale = nodeScale("dpad_left"),
                                opacity = nodeOpacity("dpad_left"),
                                widthScale = b.widthScale,
                                heightScale = b.heightScale,
                                pressMode = b.pressMode,
                                turboHz = b.turboHz,
                                isEditMode = isEditPositionMode,
                                onButton = { sendButtonEvent(14, it, nodeId = "dpad_left") }
                            )
                        }
                    }
                    if ("dpad_right" !in hiddenButtons) {
                        val b = nodeBehavior("dpad_right")
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .draggableGamepadNode("dpad_right")
                        ) {
                            StandaloneDpadButton(
                                symbol = "▶",
                                buttonIndex = 15,
                                scale = nodeScale("dpad_right"),
                                opacity = nodeOpacity("dpad_right"),
                                widthScale = b.widthScale,
                                heightScale = b.heightScale,
                                pressMode = b.pressMode,
                                turboHz = b.turboHz,
                                isEditMode = isEditPositionMode,
                                onButton = { sendButtonEvent(15, it, nodeId = "dpad_right") }
                            )
                        }
                    }
                }
            }
        }

        // Right Controls: Right Analog Stick & ABXY
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
                        opacity = nodeOpacity("right_stick", effectiveOpacity * 0.92f),
                        scale = nodeScale("right_stick", rightStickBaseScale),
                        innerDeadzone = config.rightStickInnerDeadzone,
                        outerDeadzone = config.rightStickOuterDeadzone,
                        sensitivity = config.rightStickSensitivity,
                        responseCurve = config.rightStickCurve,
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
                            opacity = nodeOpacity("abxy"),
                            scale = nodeScale("abxy"),
                            isEditMode = isEditPositionMode,
                            onButton = { index, pressed ->
                                sendButtonEvent(index, pressed, nodeId = "abxy")
                            }
                        )
                    }
                }
            } else {
                val splitAbxyBoxSize = 132.dp * effectiveScale
                Box(modifier = Modifier.size(splitAbxyBoxSize)) {
                    if ("btn_y" !in hiddenButtons) {
                        val b = nodeBehavior("btn_y")
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .draggableGamepadNode("btn_y")
                        ) {
                            StandaloneAbxyButton(
                                label = "Y",
                                accentColor = PadYColor,
                                buttonIndex = 3,
                                scale = nodeScale("btn_y"),
                                opacity = nodeOpacity("btn_y"),
                                widthScale = b.widthScale,
                                heightScale = b.heightScale,
                                pressMode = b.pressMode,
                                turboHz = b.turboHz,
                                isEditMode = isEditPositionMode,
                                onButton = { sendButtonEvent(3, it, nodeId = "btn_y") }
                            )
                        }
                    }
                    if ("btn_a" !in hiddenButtons) {
                        val b = nodeBehavior("btn_a")
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .draggableGamepadNode("btn_a")
                        ) {
                            StandaloneAbxyButton(
                                label = "A",
                                accentColor = PadAColor,
                                buttonIndex = 0,
                                scale = nodeScale("btn_a"),
                                opacity = nodeOpacity("btn_a"),
                                widthScale = b.widthScale,
                                heightScale = b.heightScale,
                                pressMode = b.pressMode,
                                turboHz = b.turboHz,
                                isEditMode = isEditPositionMode,
                                onButton = { sendButtonEvent(0, it, nodeId = "btn_a") }
                            )
                        }
                    }
                    if ("btn_x" !in hiddenButtons) {
                        val b = nodeBehavior("btn_x")
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .draggableGamepadNode("btn_x")
                        ) {
                            StandaloneAbxyButton(
                                label = "X",
                                accentColor = PadXColor,
                                buttonIndex = 2,
                                scale = nodeScale("btn_x"),
                                opacity = nodeOpacity("btn_x"),
                                widthScale = b.widthScale,
                                heightScale = b.heightScale,
                                pressMode = b.pressMode,
                                turboHz = b.turboHz,
                                isEditMode = isEditPositionMode,
                                onButton = { sendButtonEvent(2, it, nodeId = "btn_x") }
                            )
                        }
                    }
                    if ("btn_b" !in hiddenButtons) {
                        val b = nodeBehavior("btn_b")
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .draggableGamepadNode("btn_b")
                        ) {
                            StandaloneAbxyButton(
                                label = "B",
                                accentColor = PadBColor,
                                buttonIndex = 1,
                                scale = nodeScale("btn_b"),
                                opacity = nodeOpacity("btn_b"),
                                widthScale = b.widthScale,
                                heightScale = b.heightScale,
                                pressMode = b.pressMode,
                                turboHz = b.turboHz,
                                isEditMode = isEditPositionMode,
                                onButton = { sendButtonEvent(1, it, nodeId = "btn_b") }
                            )
                        }
                    }
                }
            }
        }

        // Unlimited User-Created Custom Blank Buttons
        customButtonsList.forEach { customDef ->
            val customBehavior = nodeBehavior(customDef.id)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .draggableGamepadNode(customDef.id)
            ) {
                CustomVirtualGamepadButton(
                    badge = customDef.resolvedBadge(),
                    isRound = customDef.isRound,
                    scale = nodeScale(customDef.id),
                    opacity = nodeOpacity(customDef.id),
                    widthScale = customBehavior.widthScale,
                    heightScale = customBehavior.heightScale,
                    pressMode = customBehavior.pressMode,
                    turboHz = customBehavior.turboHz,
                    isEditMode = isEditPositionMode,
                    onStateChange = { pressed ->
                        sendCustomButtonEvent(customDef, pressed)
                    }
                )
            }
        }

        // Compact Edit Mode Toolbar (.edit-bar: hint, + Thêm nút, # Lưới, Khóa vị trí, ✓ Xong)
        if (isEditPositionMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 52.dp, start = 12.dp, end = 12.dp)
                    .widthIn(max = 420.dp)
                    .testTag("virtual_gamepad_edit_toolbar"),
                shape = RoundedCornerShape(16.dp),
                color = HudPanelBg,
                border = BorderStroke(1.dp, HudLine),
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = config.tr(
                            "Kéo để di chuyển · nhấn đúp để chỉnh riêng",
                            "Drag to move · double-tap to customize"
                        ),
                        color = HudDialogBody,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val hiddenCount = hiddenButtons.size
                        HudButton(
                            text = if (hiddenCount > 0) {
                                config.tr("＋ Thêm nút ($hiddenCount)", "＋ Add ($hiddenCount)")
                            } else {
                                config.tr("＋ Thêm nút", "＋ Buttons")
                            },
                            onClick = {
                                vibrate()
                                showAddButtonModal = !showAddButtonModal
                                if (showAddButtonModal) activePopoverNodeId = null
                            },
                            minHeight = 34.dp,
                            fontSize = 11.5.sp,
                            containerOverride = if (showAddButtonModal) HudSelBg else HudSurface2,
                            contentOverride = if (showAddButtonModal) HudAccent else HudInk,
                            modifier = Modifier.testTag("virtual_gamepad_add_button")
                        )

                        HudButton(
                            text = config.tr("# Lưới", "# Grid"),
                            onClick = {
                                vibrate()
                                showEditGrid = !showEditGrid
                            },
                            minHeight = 34.dp,
                            fontSize = 11.5.sp,
                            containerOverride = if (showEditGrid) HudSelBg else HudSurface2,
                            contentOverride = if (showEditGrid) HudAccent else HudMuted,
                            modifier = Modifier.testTag("virtual_gamepad_grid_toggle")
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { isPositionLocked = !isPositionLocked }
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = config.tr("Khóa vị trí", "Lock"),
                                color = HudInk,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            HudSwitch(
                                checked = isPositionLocked,
                                onCheckedChange = { isPositionLocked = it },
                                contentDescription = config.tr("Khóa vị trí", "Lock Position")
                            )
                        }

                        HudButton(
                            text = config.tr("✓ Xong", "✓ Done"),
                            onClick = {
                                vibrate()
                                isEditPositionMode = false
                                showAddButtonModal = false
                                activePopoverNodeId = null
                                selectedButtonId = null
                            },
                            style = HudButtonStyle.MAIN,
                            minHeight = 34.dp,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }

        // Bottom Toast Hint in Edit Mode (.toast) when no popover/modal is open
        if (isEditPositionMode && activePopoverNodeId == null && !showAddButtonModal) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 44.dp, start = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = HudPanelBg,
                border = BorderStroke(1.dp, HudLine),
                shadowElevation = 0.dp
            ) {
                Text(
                    text = config.tr(
                        "Nhấn đúp một nút để chỉnh riêng nút đó",
                        "Double-tap any button to customize it"
                    ),
                    color = HudMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Modal Drawer to Add / Restore or Hide Any Button + Add Unlimited Blank Custom Buttons
        if (showAddButtonModal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(HudScrimColor.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showAddButtonModal = false },
                contentAlignment = Alignment.Center
            ) {
                AddOrManageButtonsModal(
                    config = config,
                    hiddenButtons = hiddenButtons,
                    customButtons = customButtonsList,
                    abxySplitMode = abxySplitMode,
                    dpadSplitMode = dpadSplitMode,
                    onAddBlankCustomButton = {
                        vibrate()
                        addBlankCustomButton()
                    },
                    onSelectCustomButton = { customId ->
                        vibrate(customId)
                        showAddButtonModal = false
                        selectedButtonId = customId
                        activePopoverNodeId = customId
                    },
                    onDeleteCustomButton = { customId ->
                        vibrate(customId, stronger = true)
                        removeCustomButton(customId)
                    },
                    onToggleButtonVisibility = { nodeId ->
                        vibrate(nodeId)
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
                        .padding(horizontal = 16.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}
                )
            }
        }

        // Floating Contextual Popover (.pop) anchored beside double-tapped node or ⚙ icon
        val popNodeId = activePopoverNodeId
        if (popNodeId != null) {
            val anchorRect = nodeBoundsMap[popNodeId]
            val popWidthPx = with(density) { 246.dp.toPx() }
            val popMaxHeightPx = with(density) { 360.dp.toPx() }
            val marginPx = with(density) { 10.dp.toPx() }
            val viewportW = rootSizePx.width.toFloat().coerceAtLeast(popWidthPx + marginPx * 2f)
            val viewportH = rootSizePx.height.toFloat().coerceAtLeast(popMaxHeightPx + marginPx * 2f)

            val (popX, popY) = remember(popNodeId, anchorRect, viewportW, viewportH) {
                if (anchorRect == null) {
                    ((viewportW - popWidthPx) / 2f).roundToInt() to ((viewportH - popMaxHeightPx) / 2f).roundToInt()
                } else if (popNodeId == "all") {
                    val cx = anchorRect.center.x - popWidthPx / 2f
                    val clampedX = cx.coerceIn(marginPx, (viewportW - popWidthPx - marginPx).coerceAtLeast(marginPx))
                    val rawY = anchorRect.bottom + marginPx
                    val clampedY = rawY.coerceIn(marginPx, (viewportH - popMaxHeightPx - marginPx).coerceAtLeast(marginPx))
                    clampedX.roundToInt() to clampedY.roundToInt()
                } else {
                    var x = anchorRect.right + marginPx
                    if (x + popWidthPx > viewportW - marginPx) {
                        x = anchorRect.left - popWidthPx - marginPx
                    }
                    if (x < marginPx) {
                        x = anchorRect.center.x - popWidthPx / 2f
                    }
                    val clampedX = x.coerceIn(marginPx, (viewportW - popWidthPx - marginPx).coerceAtLeast(marginPx))
                    val rawY = anchorRect.top - marginPx
                    val clampedY = rawY.coerceIn(marginPx, (viewportH - popMaxHeightPx - marginPx).coerceAtLeast(marginPx))
                    clampedX.roundToInt() to clampedY.roundToInt()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { activePopoverNodeId = null }
            ) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(popX, popY) }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}
                ) {
                    val activeCustomDef = customButtonsList.firstOrNull { it.id == popNodeId }
                    GamepadNodePopover(
                        nodeId = popNodeId,
                        config = config,
                        isEditPositionMode = isEditPositionMode,
                        hasActiveGameKey = normGameKey.isNotEmpty(),
                        hasPerGameCustomLayout = perGameLayout != null,
                        gameDisplayName = gameDisplayName,
                        nodeOffset = customOffsets[popNodeId] ?: (0f to 0f),
                        nodeScaleMult = customButtonScales[popNodeId] ?: 1.0f,
                        nodeOpacity = nodeOpacity(popNodeId),
                        nodeHaptic = nodeHaptic(popNodeId),
                        nodeBehavior = nodeBehavior(popNodeId),
                        customButtonDef = activeCustomDef,
                        abxySplitMode = abxySplitMode,
                        dpadSplitMode = dpadSplitMode,
                        onScaleChange = { setButtonScale(popNodeId, it) },
                        onOpacityChange = { setButtonOpacity(popNodeId, it) },
                        onHapticChange = {
                            vibrateWithLevel(it)
                            setButtonHaptic(popNodeId, it)
                        },
                        onBehaviorChange = { nextBehavior ->
                            setButtonBehavior(popNodeId, nextBehavior)
                        },
                        onCustomButtonDefChange = { nextCustomDef ->
                            updateCustomButtonDef(nextCustomDef)
                        },
                        onNudge = { dx, dy ->
                            vibrate(popNodeId)
                            val cur = customOffsets[popNodeId] ?: (0f to 0f)
                            customOffsets[popNodeId] = (cur.first + dx) to (cur.second + dy)
                            saveCustomOffsets()
                        },
                        onHideOrDeleteNode = {
                            vibrate(popNodeId, stronger = true)
                            deleteButton(popNodeId)
                            if (selectedButtonId == popNodeId) selectedButtonId = null
                            activePopoverNodeId = null
                        },
                        onResetNode = {
                            vibrate(popNodeId)
                            resetSingleButton(popNodeId)
                        },
                        onToggleAbxySplit = {
                            vibrate(popNodeId)
                            val nextSplit = !abxySplitMode
                            val childIds = listOf("btn_a", "btn_b", "btn_x", "btn_y")
                            if (nextSplit) {
                                customButtonOpacities["abxy"]?.let { clusterOp ->
                                    childIds.forEach { cid ->
                                        if (!customButtonOpacities.containsKey(cid)) {
                                            customButtonOpacities[cid] = clusterOp
                                        }
                                    }
                                    saveButtonOpacities()
                                }
                                customButtonHaptics["abxy"]?.let { clusterHap ->
                                    childIds.forEach { cid ->
                                        if (!customButtonHaptics.containsKey(cid)) {
                                            customButtonHaptics[cid] = clusterHap
                                        }
                                    }
                                    saveButtonHaptics()
                                }
                            } else {
                                if (!customButtonOpacities.containsKey("abxy")) {
                                    childIds.firstNotNullOfOrNull { customButtonOpacities[it] }?.let { op ->
                                        customButtonOpacities["abxy"] = op
                                        saveButtonOpacities()
                                    }
                                }
                                if (!customButtonHaptics.containsKey("abxy")) {
                                    childIds.firstNotNullOfOrNull { customButtonHaptics[it] }?.let { hap ->
                                        customButtonHaptics["abxy"] = hap
                                        saveButtonHaptics()
                                    }
                                }
                            }
                            SettingsRepository.updateGamepadAbxySplitMode(nextSplit, normGameKey)
                            val nextId = if (nextSplit) "btn_a" else "abxy"
                            selectedButtonId = nextId
                            activePopoverNodeId = nextId
                        },
                        onToggleDpadSplit = {
                            vibrate(popNodeId)
                            val nextSplit = !dpadSplitMode
                            val childIds = listOf("dpad_up", "dpad_down", "dpad_left", "dpad_right")
                            if (nextSplit) {
                                customButtonOpacities["dpad"]?.let { clusterOp ->
                                    childIds.forEach { cid ->
                                        if (!customButtonOpacities.containsKey(cid)) {
                                            customButtonOpacities[cid] = clusterOp
                                        }
                                    }
                                    saveButtonOpacities()
                                }
                                customButtonHaptics["dpad"]?.let { clusterHap ->
                                    childIds.forEach { cid ->
                                        if (!customButtonHaptics.containsKey(cid)) {
                                            customButtonHaptics[cid] = clusterHap
                                        }
                                    }
                                    saveButtonHaptics()
                                }
                            } else {
                                if (!customButtonOpacities.containsKey("dpad")) {
                                    childIds.firstNotNullOfOrNull { customButtonOpacities[it] }?.let { op ->
                                        customButtonOpacities["dpad"] = op
                                        saveButtonOpacities()
                                    }
                                }
                                if (!customButtonHaptics.containsKey("dpad")) {
                                    childIds.firstNotNullOfOrNull { customButtonHaptics[it] }?.let { hap ->
                                        customButtonHaptics["dpad"] = hap
                                        saveButtonHaptics()
                                    }
                                }
                            }
                            SettingsRepository.updateGamepadDpadSplitMode(nextSplit, normGameKey)
                            val nextId = if (nextSplit) "dpad_up" else "dpad"
                            selectedButtonId = nextId
                            activePopoverNodeId = nextId
                        },
                        onEnterEditMode = {
                            vibrate()
                            isEditPositionMode = !isEditPositionMode
                            if (!isEditPositionMode) {
                                showAddButtonModal = false
                                selectedButtonId = null
                            }
                            activePopoverNodeId = null
                        },
                        onUseGlobalLayout = {
                            vibrate()
                            switchCurrentGameToGlobalLayout()
                        },
                        onShowAll = {
                            vibrate()
                            SettingsRepository.updateGamepadHiddenButtons("", normGameKey)
                        },
                        onResetAll = {
                            vibrate()
                            resetAllCustomizations()
                            activePopoverNodeId = null
                        },
                        onClose = { activePopoverNodeId = null }
                    )
                }
            }
        }
    }
}
