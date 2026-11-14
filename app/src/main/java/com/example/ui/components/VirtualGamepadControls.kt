package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GamepadButtonPressMode
import com.example.data.GamepadStickCurve
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sqrt

@Composable
internal fun VirtualAnalogStick(
    opacity: Float,
    scale: Float,
    innerDeadzone: Float,
    outerDeadzone: Float = 0.95f,
    sensitivity: Float,
    responseCurve: GamepadStickCurve = GamepadStickCurve.STANDARD,
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
    var isTouching by remember { mutableStateOf(false) }
    var isAutoSprinting by remember { mutableStateOf(false) }

    val currentInnerDeadzone by rememberUpdatedState(innerDeadzone)
    val currentOuterDeadzone by rememberUpdatedState(outerDeadzone)
    val currentSensitivity by rememberUpdatedState(sensitivity)
    val currentCurve by rememberUpdatedState(responseCurve)
    val currentAutoSprint by rememberUpdatedState(enableAutoSprintL3)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnSprintChange by rememberUpdatedState(onSprintStateChange)

    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            if (isTouching || stickX != 0f || stickY != 0f) {
                isTouching = false
                stickX = 0f
                stickY = 0f
                currentOnMove(0f, 0f)
            }
            if (isAutoSprinting) {
                isAutoSprinting = false
                currentOnSprintChange(false)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isTouching || stickX != 0f || stickY != 0f) {
                currentOnMove(0f, 0f)
            }
            if (isAutoSprinting) {
                currentOnSprintChange(false)
            }
        }
    }

    fun applyDeadzoneAndCurve(rawNorm: Float): Float {
        val absVal = abs(rawNorm)
        val effInner = currentInnerDeadzone.coerceIn(0f, 0.30f)
        val effOuter = currentOuterDeadzone.coerceIn((effInner + 0.10f).coerceAtMost(1f), 1f)
        if (absVal <= effInner) return 0f
        val rescaled = ((absVal - effInner) / (effOuter - effInner)).coerceIn(0f, 1f)
        val sens = currentSensitivity.coerceIn(0.6f, 1.6f)
        val baseExp = when (currentCurve) {
            GamepadStickCurve.LINEAR -> (1f / sens).coerceIn(0.65f, 1.45f)
            GamepadStickCurve.STANDARD -> (1.12f / sens).coerceIn(0.55f, 1.75f)
            GamepadStickCurve.PRECISION -> (1.58f / sens).coerceIn(0.85f, 2.25f)
        }
        return sign(rawNorm) * rescaled.pow(baseExp)
    }

    val idleBorder = Color.White.copy(alpha = (opacity * 0.28f).coerceIn(0.08f, 0.45f))
    val outerBgAlpha = (opacity * 0.54f).coerceIn(0.12f, 0.72f)
    val knobBgAlpha = (opacity * 0.30f).coerceIn(0.08f, 0.48f)
    val knobBorderAlpha = (opacity * 0.40f).coerceIn(0.10f, 0.56f)

    Box(
        modifier = Modifier
            .size(outerSizeDp)
            .clip(CircleShape)
            .background(HudSurface.copy(alpha = outerBgAlpha))
            .border(
                width = if (isTouching) 1.5.dp else 1.dp,
                color = when {
                    isAutoSprinting -> HudCyan
                    isTouching -> HudAccent
                    else -> idleBorder
                },
                shape = CircleShape
            )
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(maxRadiusPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = true)
                            down.consume()
                            isTouching = true
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            var lastSentX = 0f
                            var lastSentY = 0f
                            var lastSentAtMs = 0L

                            fun updateFromPos(pos: Offset, forceImmediate: Boolean = false) {
                                val dx = pos.x - centerX
                                val dy = pos.y - centerY
                                val dist = sqrt(dx * dx + dy * dy)
                                val nextStickX: Float
                                val nextStickY: Float
                                if (dist <= maxRadiusPx) {
                                    nextStickX = dx
                                    nextStickY = dy
                                } else if (dist > 0.001f) {
                                    nextStickX = (dx / dist) * maxRadiusPx
                                    nextStickY = (dy / dist) * maxRadiusPx
                                } else {
                                    nextStickX = 0f
                                    nextStickY = 0f
                                }
                                if (abs(nextStickX - stickX) >= 0.4f || abs(nextStickY - stickY) >= 0.4f) {
                                    stickX = nextStickX
                                    stickY = nextStickY
                                }
                                val rawX = (nextStickX / maxRadiusPx).coerceIn(-1f, 1f)
                                val rawY = (nextStickY / maxRadiusPx).coerceIn(-1f, 1f)
                                val curvedX = applyDeadzoneAndCurve(rawX)
                                val curvedY = applyDeadzoneAndCurve(rawY)

                                val deltaX = abs(curvedX - lastSentX)
                                val deltaY = abs(curvedY - lastSentY)
                                if (deltaX >= 0.012f || deltaY >= 0.012f || (curvedX == 0f && curvedY == 0f && (lastSentX != 0f || lastSentY != 0f))) {
                                    val nowMs = android.os.SystemClock.uptimeMillis()
                                    val isFastFlickOrZero = forceImmediate ||
                                        (curvedX == 0f && curvedY == 0f) ||
                                        deltaX >= 0.14f ||
                                        deltaY >= 0.14f
                                    if (isFastFlickOrZero || (nowMs - lastSentAtMs) >= 8L) {
                                        lastSentX = curvedX
                                        lastSentY = curvedY
                                        lastSentAtMs = nowMs
                                        onMove(curvedX, curvedY)
                                    }
                                }

                                // Auto-Sprint L3: only activates when finger swipes PAST the top rim (> 128% radius),
                                // then stays latched while the stick remains pushed forward (< -60% Y) so finger can relax.
                                if (currentAutoSprint) {
                                    val rawUnclampedX = dx / maxRadiusPx
                                    val rawUnclampedY = dy / maxRadiusPx
                                    val shouldSprint = if (!isAutoSprinting) {
                                        rawUnclampedY < -1.28f && abs(rawUnclampedX) < 0.56f
                                    } else {
                                        rawY < -0.60f && abs(rawX) < 0.68f
                                    }
                                    if (shouldSprint != isAutoSprinting) {
                                        isAutoSprinting = shouldSprint
                                        onSprintStateChange(shouldSprint)
                                    }
                                } else if (isAutoSprinting) {
                                    isAutoSprinting = false
                                    onSprintStateChange(false)
                                }
                            }

                            updateFromPos(down.position, forceImmediate = true)

                            do {
                                val event = awaitPointerEvent()
                                val activeChange = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                                if (activeChange != null && activeChange.pressed) {
                                    activeChange.consume()
                                    updateFromPos(activeChange.position, forceImmediate = false)
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
        // Custom Canvas: Outer & Inner Deadzone Rings + Active Vector Line
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = Color.White.copy(alpha = 0.14f),
                radius = maxRadiusPx * currentOuterDeadzone.coerceIn(0.50f, 1.0f),
                center = c,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = HudCyan.copy(alpha = 0.22f),
                radius = maxRadiusPx * currentInnerDeadzone.coerceIn(0.0f, 0.35f),
                center = c,
                style = Stroke(width = 1.dp.toPx())
            )
            if (isTouching && (abs(stickX) > 2f || abs(stickY) > 2f)) {
                drawLine(
                    color = if (isAutoSprinting) HudCyan else HudAccent,
                    start = c,
                    end = Offset(c.x + stickX, c.y + stickY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        if (isAutoSprinting || (enableAutoSprintL3 && isTouching)) {
            Text(
                text = if (isAutoSprinting) "🏃 L3" else "▲ L3",
                color = if (isAutoSprinting) HudCyan else HudMuted.copy(alpha = 0.65f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
            )
        }

        // Inner thumb knob matching mockup translucent white knob
        Box(
            modifier = Modifier
                .offset { IntOffset(stickX.roundToInt(), stickY.roundToInt()) }
                .size(knobSizeDp)
                .clip(CircleShape)
                .background(
                    when {
                        isAutoSprinting -> Color(0xFF10313A).copy(alpha = opacity.coerceAtLeast(0.85f))
                        isTouching -> HudSelBg.copy(alpha = opacity.coerceAtLeast(0.85f))
                        else -> Color.White.copy(alpha = knobBgAlpha)
                    }
                )
                .border(
                    width = if (isTouching) 1.5.dp else 1.dp,
                    color = when {
                        isAutoSprinting -> HudCyan
                        isTouching -> HudAccent
                        else -> Color.White.copy(alpha = knobBorderAlpha)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = PadBtnText.copy(alpha = opacity.coerceIn(0.25f, 1.0f)),
                fontWeight = FontWeight.ExtraBold,
                fontSize = (12f * scale).sp
            )
        }
    }
}

@Composable
internal fun VirtualDpadCross(
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
    val currentOnButton by rememberUpdatedState(onButton)

    fun updateDirections(newUp: Boolean, newDown: Boolean, newLeft: Boolean, newRight: Boolean) {
        if (newUp != upPressed) {
            upPressed = newUp
            currentOnButton(12, newUp)
        }
        if (newDown != downPressed) {
            downPressed = newDown
            currentOnButton(13, newDown)
        }
        if (newLeft != leftPressed) {
            leftPressed = newLeft
            currentOnButton(14, newLeft)
        }
        if (newRight != rightPressed) {
            rightPressed = newRight
            currentOnButton(15, newRight)
        }
    }

    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            updateDirections(false, false, false, false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            updateDirections(false, false, false, false)
        }
    }

    val containerBgAlpha = (opacity * 0.48f).coerceIn(0.10f, 0.65f)
    val borderAlpha = (opacity * 0.24f).coerceIn(0.06f, 0.40f)

    Box(
        modifier = Modifier
            .size(dpadSize)
            .clip(RoundedCornerShape(16.dp))
            .background(HudSurface.copy(alpha = containerBgAlpha))
            .border(1.dp, Color.White.copy(alpha = borderAlpha), RoundedCornerShape(16.dp))
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(scale) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = true)
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
                        HudAccent
                    } else {
                        Color.White.copy(alpha = (opacity * 0.25f).coerceIn(0.08f, 0.40f))
                    }
                )
        )
    }
}

/**
 * Shared state + gesture controller supporting:
 * - NORMAL ("Bấm thả"): active while finger is touching
 * - TOGGLE_HOLD ("Chạm khóa"): single tap latches on/off
 * - TURBO ("Nhấp nhanh"): rapid pulse at [turboHz] while finger is touching
 */
@Composable
private fun rememberPressModeController(
    pressMode: GamepadButtonPressMode,
    turboHz: Int,
    isEditMode: Boolean,
    onOutputState: (Boolean) -> Unit
): Pair<Boolean, () -> Unit> {
    var fingerDown by remember { mutableStateOf(false) }
    var latched by remember { mutableStateOf(false) }
    var emittedState by remember { mutableStateOf(false) }
    val currentOnOutput by rememberUpdatedState(onOutputState)

    fun emit(next: Boolean) {
        if (emittedState != next) {
            emittedState = next
            currentOnOutput(next)
        }
    }

    LaunchedEffect(isEditMode, pressMode) {
        if (isEditMode) {
            fingerDown = false
            latched = false
            emit(false)
        } else if (pressMode != GamepadButtonPressMode.TOGGLE_HOLD && latched) {
            latched = false
            if (!fingerDown) emit(false)
        }
    }

    LaunchedEffect(fingerDown, pressMode, turboHz, isEditMode) {
        if (isEditMode) return@LaunchedEffect
        if (pressMode == GamepadButtonPressMode.TURBO && fingerDown) {
            val halfPeriodMs = (500L / turboHz.coerceIn(5, 20)).coerceAtLeast(22L)
            while (fingerDown) {
                emit(true)
                delay(halfPeriodMs)
                emit(false)
                delay(halfPeriodMs)
            }
            emit(false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (emittedState) {
                currentOnOutput(false)
            }
        }
    }

    val isVisualActive = when (pressMode) {
        GamepadButtonPressMode.NORMAL -> fingerDown
        GamepadButtonPressMode.TOGGLE_HOLD -> latched
        GamepadButtonPressMode.TURBO -> fingerDown || emittedState
    }

    val onPointerEvent: (Boolean) -> Unit = { down ->
        if (!isEditMode) {
            when (pressMode) {
                GamepadButtonPressMode.NORMAL -> {
                    fingerDown = down
                    emit(down)
                }
                GamepadButtonPressMode.TOGGLE_HOLD -> {
                    if (down && !fingerDown) {
                        fingerDown = true
                        latched = !latched
                        emit(latched)
                    } else if (!down) {
                        fingerDown = false
                    }
                }
                GamepadButtonPressMode.TURBO -> {
                    fingerDown = down
                    if (!down) emit(false)
                }
            }
        }
    }

    return isVisualActive to {
        // Not used directly, we expose gesture modifier below
        onPointerEvent(false)
    }
}

@Composable
internal fun StandaloneDpadButton(
    symbol: String,
    buttonIndex: Int,
    scale: Float,
    opacity: Float,
    widthScale: Float = 1.0f,
    heightScale: Float = 1.0f,
    pressMode: GamepadButtonPressMode = GamepadButtonPressMode.NORMAL,
    turboHz: Int = 10,
    isEditMode: Boolean,
    onButton: (Boolean) -> Unit
) {
    GamepadConfigurableButtonBox(
        widthDp = 38.dp * scale * widthScale,
        heightDp = 38.dp * scale * heightScale,
        shape = RoundedCornerShape(10.dp),
        opacity = opacity,
        accentColor = HudAccent,
        pressMode = pressMode,
        turboHz = turboHz,
        isEditMode = isEditMode,
        onStateChange = onButton
    ) { isActive, isLatched ->
        Text(
            text = if (isLatched) "$symbol•" else symbol,
            color = if (isActive) HudAccent else PadBtnText.copy(alpha = opacity.coerceIn(0.25f, 1.0f)),
            fontSize = (13f * scale).sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
internal fun DpadVisualKey(
    modifier: Modifier,
    symbol: String,
    sizeDp: Dp,
    isPressed: Boolean,
    opacity: Float
) {
    val bgAlpha = (opacity * 0.72f).coerceIn(0.16f, 0.90f)
    val borderAlpha = (opacity * 0.28f).coerceIn(0.08f, 0.45f)

    Box(
        modifier = modifier
            .size(sizeDp)
            .padding(2.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (isPressed) {
                    HudSelBg.copy(alpha = opacity.coerceAtLeast(0.85f))
                } else {
                    HudSurface.copy(alpha = bgAlpha)
                }
            )
            .border(
                width = if (isPressed) 1.4.dp else 1.dp,
                color = if (isPressed) HudAccent else Color.White.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(9.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = if (isPressed) HudAccent else PadBtnText.copy(alpha = opacity.coerceIn(0.25f, 1.0f)),
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
internal fun GamepadTriggerButton(
    label: String,
    buttonIndex: Int,
    scale: Float,
    opacity: Float,
    widthScale: Float = 1.0f,
    heightScale: Float = 1.0f,
    analogDragMode: Boolean,
    pressMode: GamepadButtonPressMode = GamepadButtonPressMode.NORMAL,
    turboHz: Int = 10,
    isEditMode: Boolean,
    onStateChange: (Boolean, Float) -> Unit
) {
    var fingerDown by remember { mutableStateOf(false) }
    var latched by remember { mutableStateOf(false) }
    var analogLevel by remember { mutableFloatStateOf(0f) }
    var emittedPressed by remember { mutableStateOf(false) }
    val currentOnStateChange by rememberUpdatedState(onStateChange)

    fun emit(pressed: Boolean, level: Float) {
        emittedPressed = pressed
        currentOnStateChange(pressed, if (pressed) level.coerceIn(0.1f, 1f) else 0f)
    }

    LaunchedEffect(isEditMode, pressMode) {
        if (isEditMode) {
            if (fingerDown || latched || emittedPressed) {
                fingerDown = false
                latched = false
                analogLevel = 0f
                emit(false, 0f)
            }
        } else if (pressMode != GamepadButtonPressMode.TOGGLE_HOLD && latched) {
            latched = false
            analogLevel = 0f
            emit(false, 0f)
        }
    }

    LaunchedEffect(fingerDown, pressMode, turboHz, isEditMode) {
        if (isEditMode) return@LaunchedEffect
        if (pressMode == GamepadButtonPressMode.TURBO && fingerDown) {
            val halfPeriodMs = (500L / turboHz.coerceIn(5, 20)).coerceAtLeast(22L)
            while (fingerDown) {
                val lvl = if (analogDragMode && analogLevel > 0f) analogLevel else 1f
                emit(true, lvl)
                delay(halfPeriodMs)
                emit(false, 0f)
                delay(halfPeriodMs)
            }
            emit(false, 0f)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (emittedPressed) {
                currentOnStateChange(false, 0f)
            }
        }
    }

    val isPressedVisual = when (pressMode) {
        GamepadButtonPressMode.NORMAL -> fingerDown
        GamepadButtonPressMode.TOGGLE_HOLD -> latched
        GamepadButtonPressMode.TURBO -> fingerDown || emittedPressed
    }

    val bgAlpha = (opacity * 0.72f).coerceIn(0.16f, 0.90f)
    val borderAlpha = (opacity * 0.28f).coerceIn(0.08f, 0.45f)

    Box(
        modifier = Modifier
            .size(width = (64.dp * scale * widthScale), height = (38.dp * scale * heightScale))
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isPressedVisual) {
                    HudSelBg.copy(alpha = opacity.coerceAtLeast(0.85f))
                } else {
                    HudSurface.copy(alpha = bgAlpha)
                }
            )
            .border(
                width = if (isPressedVisual) 1.5.dp else 1.dp,
                color = when {
                    latched -> HudCyan
                    isPressedVisual -> HudAccent
                    else -> Color.White.copy(alpha = borderAlpha)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(buttonIndex, analogDragMode, pressMode, turboHz) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = true)
                            down.consume()
                            fingerDown = true
                            val initLevel = if (analogDragMode) 0.35f else 1f
                            analogLevel = initLevel

                            when (pressMode) {
                                GamepadButtonPressMode.NORMAL -> emit(true, initLevel)
                                GamepadButtonPressMode.TOGGLE_HOLD -> {
                                    latched = !latched
                                    if (latched) {
                                        emit(true, if (analogDragMode) 1f else 1f)
                                    } else {
                                        analogLevel = 0f
                                        emit(false, 0f)
                                    }
                                }
                                GamepadButtonPressMode.TURBO -> {}
                            }

                            do {
                                val event = awaitPointerEvent()
                                val ch = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                                if (ch != null && ch.pressed && analogDragMode) {
                                    ch.consume()
                                    val dy = (ch.position.y - down.position.y).coerceIn(0f, 80f)
                                    val computed = (0.35f + (dy / 80f) * 0.65f).coerceIn(0.2f, 1f)
                                    analogLevel = computed
                                    if (pressMode == GamepadButtonPressMode.NORMAL || (pressMode == GamepadButtonPressMode.TOGGLE_HOLD && latched)) {
                                        emit(true, computed)
                                    }
                                }
                            } while (event.changes.any { it.pressed })

                            fingerDown = false
                            if (pressMode == GamepadButtonPressMode.NORMAL) {
                                analogLevel = 0f
                                emit(false, 0f)
                            }
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (analogDragMode && isPressedVisual) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(analogLevel.coerceIn(0f, 1f))
                    .background(HudAccent.copy(alpha = 0.25f))
            )
        }
        val displayText = when {
            analogDragMode && isPressedVisual -> "$label ${(analogLevel * 100).roundToInt()}%"
            latched -> "$label •"
            else -> label
        }
        Text(
            text = displayText,
            color = when {
                latched -> HudCyan
                isPressedVisual -> HudAccent
                else -> PadBtnText.copy(alpha = opacity.coerceIn(0.25f, 1.0f))
            },
            fontWeight = FontWeight.ExtraBold,
            fontSize = (12f * scale).sp
        )
    }
}

private enum class ShoulderSwipeDir {
    NONE, UP, DOWN, LEFT, RIGHT
}

private fun shortDirectionBadge(buttonIndex: Int): String? {
    if (buttonIndex !in 0..16) return null
    return XBOX_TARGET_BUTTON_CHOICES.firstOrNull { it.index == buttonIndex }?.badge
}

@Composable
internal fun GamepadShoulderButton(
    label: String,
    buttonIndex: Int,
    scale: Float,
    opacity: Float,
    widthScale: Float = 1.0f,
    heightScale: Float = 1.0f,
    pressMode: GamepadButtonPressMode = GamepadButtonPressMode.NORMAL,
    turboHz: Int = 10,
    directionalSwipe: Boolean = false,
    swipeIncludeShoulder: Boolean = true,
    swipeUpBtn: Int = -1,
    swipeDownBtn: Int = -1,
    swipeLeftBtn: Int = -1,
    swipeRightBtn: Int = -1,
    isEditMode: Boolean,
    onStateChange: (Boolean) -> Unit,
    onDirectionButtonChange: (Int, Boolean) -> Unit = { _, _ -> }
) {
    if (!directionalSwipe) {
        GamepadConfigurableButtonBox(
            widthDp = 58.dp * scale * widthScale,
            heightDp = 38.dp * scale * heightScale,
            shape = RoundedCornerShape(12.dp),
            opacity = opacity,
            accentColor = HudAccent,
            pressMode = pressMode,
            turboHz = turboHz,
            isEditMode = isEditMode,
            onStateChange = onStateChange
        ) { isActive, isLatched ->
            Text(
                text = if (isLatched) "$label •" else label,
                color = when {
                    isLatched -> HudCyan
                    isActive -> HudAccent
                    else -> PadBtnText.copy(alpha = opacity.coerceIn(0.25f, 1.0f))
                },
                fontWeight = FontWeight.Bold,
                fontSize = (12f * scale).sp
            )
        }
        return
    }

    var fingerDown by remember { mutableStateOf(false) }
    var latched by remember { mutableStateOf(false) }
    var emittedShoulder by remember { mutableStateOf(false) }
    var activeDir by remember { mutableStateOf(ShoulderSwipeDir.NONE) }
    var emittedDirBtn by remember { mutableStateOf(-1) }
    var tapPulseSeq by remember { mutableStateOf(0) }

    val currentOnStateChange by rememberUpdatedState(onStateChange)
    val currentOnDirectionButtonChange by rememberUpdatedState(onDirectionButtonChange)

    fun emitShoulder(next: Boolean) {
        if (emittedShoulder != next) {
            emittedShoulder = next
            currentOnStateChange(next)
        }
    }

    fun emitDirection(nextDir: ShoulderSwipeDir) {
        activeDir = nextDir
        val rawBtn = when (nextDir) {
            ShoulderSwipeDir.UP -> swipeUpBtn
            ShoulderSwipeDir.DOWN -> swipeDownBtn
            ShoulderSwipeDir.LEFT -> swipeLeftBtn
            ShoulderSwipeDir.RIGHT -> swipeRightBtn
            ShoulderSwipeDir.NONE -> -1
        }
        val validBtn = if (rawBtn in 0..16) rawBtn else -1
        if (emittedDirBtn != validBtn) {
            if (emittedDirBtn in 0..16) {
                currentOnDirectionButtonChange(emittedDirBtn, false)
            }
            emittedDirBtn = validBtn
            if (validBtn in 0..16) {
                currentOnDirectionButtonChange(validBtn, true)
            }
        }
    }

    LaunchedEffect(isEditMode, pressMode, directionalSwipe) {
        if (isEditMode) {
            fingerDown = false
            latched = false
            emitDirection(ShoulderSwipeDir.NONE)
            emitShoulder(false)
        } else if (pressMode != GamepadButtonPressMode.TOGGLE_HOLD && latched) {
            latched = false
            if (!fingerDown) emitShoulder(false)
        }
    }

    LaunchedEffect(fingerDown, pressMode, turboHz, swipeIncludeShoulder, isEditMode) {
        if (isEditMode) return@LaunchedEffect
        if (pressMode == GamepadButtonPressMode.TURBO && swipeIncludeShoulder && fingerDown) {
            val halfPeriodMs = (500L / turboHz.coerceIn(5, 20)).coerceAtLeast(22L)
            while (fingerDown && swipeIncludeShoulder) {
                emitShoulder(true)
                delay(halfPeriodMs)
                emitShoulder(false)
                delay(halfPeriodMs)
            }
            emitShoulder(false)
        }
    }

    LaunchedEffect(tapPulseSeq, isEditMode) {
        if (isEditMode || tapPulseSeq <= 0) return@LaunchedEffect
        emitShoulder(true)
        delay(65L)
        if (!fingerDown || !swipeIncludeShoulder) {
            emitShoulder(false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (emittedDirBtn in 0..16) {
                currentOnDirectionButtonChange(emittedDirBtn, false)
            }
            if (emittedShoulder) {
                currentOnStateChange(false)
            }
        }
    }

    val isShoulderVisualActive = when (pressMode) {
        GamepadButtonPressMode.NORMAL -> (swipeIncludeShoulder && fingerDown) || emittedShoulder
        GamepadButtonPressMode.TOGGLE_HOLD -> latched
        GamepadButtonPressMode.TURBO -> (swipeIncludeShoulder && fingerDown) || emittedShoulder
    }
    val isAnyVisualActive = isShoulderVisualActive || activeDir != ShoulderSwipeDir.NONE

    val bgAlpha = (opacity * 0.72f).coerceIn(0.16f, 0.90f)
    val borderAlpha = (opacity * 0.28f).coerceIn(0.08f, 0.45f)
    val shape = RoundedCornerShape(12.dp)

    val upBadge = shortDirectionBadge(swipeUpBtn)
    val downBadge = shortDirectionBadge(swipeDownBtn)
    val leftBadge = shortDirectionBadge(swipeLeftBtn)
    val rightBadge = shortDirectionBadge(swipeRightBtn)

    Box(
        modifier = Modifier
            .size(width = 58.dp * scale * widthScale, height = 38.dp * scale * heightScale)
            .clip(shape)
            .background(
                if (isAnyVisualActive) {
                    HudAccent.copy(alpha = 0.26f)
                } else {
                    HudSurface.copy(alpha = bgAlpha)
                }
            )
            .border(
                width = if (isAnyVisualActive) 1.5.dp else 1.dp,
                color = when {
                    activeDir != ShoulderSwipeDir.NONE -> HudCyan
                    latched -> HudCyan
                    isAnyVisualActive -> HudAccent
                    else -> Color.White.copy(alpha = borderAlpha)
                },
                shape = shape
            )
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(
                        pressMode,
                        turboHz,
                        swipeIncludeShoulder,
                        swipeUpBtn,
                        swipeDownBtn,
                        swipeLeftBtn,
                        swipeRightBtn,
                        scale
                    ) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = true)
                            down.consume()
                            val pointerId = down.id
                            val startPos = down.position
                            val enterThresholdPx = (14.dp * scale.coerceIn(0.85f, 1.25f)).toPx()
                            val exitThresholdPx = enterThresholdPx * 0.72f
                            var hasSwipedOut = false

                            fingerDown = true
                            emitDirection(ShoulderSwipeDir.NONE)

                            if (swipeIncludeShoulder) {
                                when (pressMode) {
                                    GamepadButtonPressMode.NORMAL -> emitShoulder(true)
                                    GamepadButtonPressMode.TOGGLE_HOLD -> {
                                        latched = !latched
                                        emitShoulder(latched)
                                    }
                                    GamepadButtonPressMode.TURBO -> {}
                                }
                            }

                            do {
                                val event = awaitPointerEvent()
                                val ch = event.changes.firstOrNull { it.id == pointerId }
                                if (ch != null && ch.pressed) {
                                    ch.consume()
                                    val dx = ch.position.x - startPos.x
                                    val dy = ch.position.y - startPos.y
                                    val dist = sqrt(dx * dx + dy * dy)
                                    val threshold = if (activeDir == ShoulderSwipeDir.NONE) {
                                        enterThresholdPx
                                    } else {
                                        exitThresholdPx
                                    }
                                    if (dist >= threshold) {
                                        hasSwipedOut = true
                                        val nextDir = if (abs(dx) >= abs(dy)) {
                                            if (dx > 0f) ShoulderSwipeDir.RIGHT else ShoulderSwipeDir.LEFT
                                        } else {
                                            if (dy > 0f) ShoulderSwipeDir.DOWN else ShoulderSwipeDir.UP
                                        }
                                        emitDirection(nextDir)
                                    } else {
                                        emitDirection(ShoulderSwipeDir.NONE)
                                    }
                                }
                            } while (event.changes.any { it.id == pointerId && it.pressed })

                            fingerDown = false
                            emitDirection(ShoulderSwipeDir.NONE)

                            if (swipeIncludeShoulder) {
                                if (pressMode == GamepadButtonPressMode.NORMAL) {
                                    emitShoulder(false)
                                }
                            } else if (!hasSwipedOut) {
                                when (pressMode) {
                                    GamepadButtonPressMode.TOGGLE_HOLD -> {
                                        latched = !latched
                                        emitShoulder(latched)
                                    }
                                    GamepadButtonPressMode.NORMAL, GamepadButtonPressMode.TURBO -> {
                                        tapPulseSeq++
                                    }
                                }
                            }
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (upBadge != null) {
            Text(
                text = upBadge,
                color = if (activeDir == ShoulderSwipeDir.UP) HudCyan else HudMuted.copy(alpha = (opacity * 0.78f).coerceIn(0.25f, 0.9f)),
                fontSize = (7.5f * scale).sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 1.dp)
            )
        }
        if (downBadge != null) {
            Text(
                text = downBadge,
                color = if (activeDir == ShoulderSwipeDir.DOWN) HudCyan else HudMuted.copy(alpha = (opacity * 0.78f).coerceIn(0.25f, 0.9f)),
                fontSize = (7.5f * scale).sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 1.dp)
            )
        }
        if (leftBadge != null) {
            Text(
                text = leftBadge,
                color = if (activeDir == ShoulderSwipeDir.LEFT) HudCyan else HudMuted.copy(alpha = (opacity * 0.78f).coerceIn(0.25f, 0.9f)),
                fontSize = (7.5f * scale).sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 3.dp)
            )
        }
        if (rightBadge != null) {
            Text(
                text = rightBadge,
                color = if (activeDir == ShoulderSwipeDir.RIGHT) HudCyan else HudMuted.copy(alpha = (opacity * 0.78f).coerceIn(0.25f, 0.9f)),
                fontSize = (7.5f * scale).sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 3.dp)
            )
        }

        val activeDirBadge = when (activeDir) {
            ShoulderSwipeDir.UP -> upBadge
            ShoulderSwipeDir.DOWN -> downBadge
            ShoulderSwipeDir.LEFT -> leftBadge
            ShoulderSwipeDir.RIGHT -> rightBadge
            ShoulderSwipeDir.NONE -> null
        }
        val centerText = when {
            activeDirBadge != null && swipeIncludeShoulder -> "$label+$activeDirBadge"
            activeDirBadge != null -> activeDirBadge
            latched -> "$label •"
            else -> label
        }
        Text(
            text = centerText,
            color = when {
                activeDir != ShoulderSwipeDir.NONE -> HudCyan
                latched -> HudCyan
                isShoulderVisualActive -> HudAccent
                else -> PadBtnText.copy(alpha = opacity.coerceIn(0.25f, 1.0f))
            },
            fontWeight = FontWeight.Bold,
            fontSize = (11.5f * scale).sp
        )
    }
}

@Composable
internal fun GamepadThumbClickMiniButton(
    label: String,
    buttonIndex: Int,
    scale: Float,
    opacity: Float,
    widthScale: Float = 1.0f,
    heightScale: Float = 1.0f,
    pressMode: GamepadButtonPressMode = GamepadButtonPressMode.NORMAL,
    turboHz: Int = 10,
    isEditMode: Boolean,
    onStateChange: (Boolean) -> Unit
) {
    GamepadConfigurableButtonBox(
        widthDp = 42.dp * scale * widthScale,
        heightDp = 34.dp * scale * heightScale,
        shape = RoundedCornerShape(10.dp),
        opacity = opacity,
        accentColor = HudAccent,
        pressMode = pressMode,
        turboHz = turboHz,
        isEditMode = isEditMode,
        onStateChange = onStateChange
    ) { isActive, isLatched ->
        Text(
            text = if (isLatched) "$label•" else label,
            color = when {
                isLatched -> HudCyan
                isActive -> HudAccent
                else -> PadBtnText.copy(alpha = opacity.coerceIn(0.25f, 1.0f))
            },
            fontWeight = FontWeight.ExtraBold,
            fontSize = (10.5f * scale).sp
        )
    }
}

@Composable
internal fun GamepadSmallButton(
    label: String,
    index: Int,
    scale: Float,
    opacity: Float,
    widthScale: Float = 1.0f,
    heightScale: Float = 1.0f,
    pressMode: GamepadButtonPressMode = GamepadButtonPressMode.NORMAL,
    turboHz: Int = 10,
    isEditMode: Boolean,
    onButton: (Boolean) -> Unit
) {
    val hasCustomDim = abs(widthScale - 1.0f) >= 0.02f || abs(heightScale - 1.0f) >= 0.02f
    GamepadConfigurableButtonBox(
        widthDp = if (hasCustomDim) (64.dp * scale * widthScale) else null,
        heightDp = if (hasCustomDim) (30.dp * scale * heightScale) else null,
        shape = RoundedCornerShape(12.dp),
        opacity = opacity,
        accentColor = HudAccent,
        pressMode = pressMode,
        turboHz = turboHz,
        isEditMode = isEditMode,
        innerPaddingH = if (hasCustomDim) 4.dp else (10.dp * scale),
        innerPaddingV = if (hasCustomDim) 2.dp else (6.dp * scale),
        onStateChange = onButton
    ) { isActive, isLatched ->
        Text(
            text = if (isLatched) "$label •" else label,
            color = when {
                isLatched -> HudCyan
                isActive -> HudAccent
                else -> PadBtnText.copy(alpha = opacity.coerceIn(0.25f, 1.0f))
            },
            fontSize = (11f * scale).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun GamepadXboxNexusButton(
    scale: Float,
    opacity: Float,
    widthScale: Float = 1.0f,
    heightScale: Float = 1.0f,
    pressMode: GamepadButtonPressMode = GamepadButtonPressMode.NORMAL,
    turboHz: Int = 10,
    isEditMode: Boolean,
    onButton: (Boolean) -> Unit
) {
    GamepadConfigurableButtonBox(
        widthDp = 34.dp * scale * widthScale,
        heightDp = 34.dp * scale * heightScale,
        shape = RoundedCornerShape(percent = 50),
        opacity = opacity,
        accentColor = HudAccent,
        idleBorderOverride = HudAccent.copy(alpha = opacity.coerceIn(0.30f, 1.0f)),
        pressMode = pressMode,
        turboHz = turboHz,
        isEditMode = isEditMode,
        onStateChange = onButton
    ) { isActive, isLatched ->
        Text(
            text = "✕",
            color = if (isLatched) HudCyan else if (isActive) Color.White else HudAccent.copy(alpha = opacity.coerceIn(0.35f, 1.0f)),
            fontSize = (16f * scale).sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
internal fun StandaloneAbxyButton(
    label: String,
    accentColor: Color,
    buttonIndex: Int,
    scale: Float,
    opacity: Float,
    widthScale: Float = 1.0f,
    heightScale: Float = 1.0f,
    pressMode: GamepadButtonPressMode = GamepadButtonPressMode.NORMAL,
    turboHz: Int = 10,
    isEditMode: Boolean,
    onButton: (Boolean) -> Unit
) {
    GamepadConfigurableButtonBox(
        widthDp = 46.dp * scale * widthScale,
        heightDp = 46.dp * scale * heightScale,
        shape = RoundedCornerShape(percent = 50),
        opacity = opacity,
        accentColor = accentColor,
        pressMode = pressMode,
        turboHz = turboHz,
        isEditMode = isEditMode,
        onStateChange = onButton
    ) { isActive, isLatched ->
        Text(
            text = if (isLatched) "$label•" else label,
            color = when {
                isLatched -> HudCyan
                isActive -> Color.White
                else -> accentColor.copy(alpha = opacity.coerceIn(0.25f, 1.0f))
            },
            fontWeight = FontWeight.ExtraBold,
            fontSize = (16f * scale).sp
        )
    }
}

@Composable
internal fun CustomVirtualGamepadButton(
    badge: String,
    isRound: Boolean,
    scale: Float,
    opacity: Float,
    widthScale: Float = 1.0f,
    heightScale: Float = 1.0f,
    pressMode: GamepadButtonPressMode,
    turboHz: Int,
    isEditMode: Boolean,
    onStateChange: (Boolean) -> Unit
) {
    val shape = if (isRound) RoundedCornerShape(percent = 50) else RoundedCornerShape(12.dp)
    val baseW = if (isRound) 46.dp else 62.dp
    val baseH = if (isRound) 46.dp else 36.dp
    val widthDp = baseW * scale * widthScale
    val heightDp = baseH * scale * heightScale

    GamepadConfigurableButtonBox(
        widthDp = widthDp,
        heightDp = heightDp,
        shape = shape,
        opacity = opacity,
        accentColor = HudCyan,
        idleBorderOverride = HudCyan.copy(alpha = (opacity * 0.45f).coerceIn(0.15f, 0.70f)),
        pressMode = pressMode,
        turboHz = turboHz,
        isEditMode = isEditMode,
        innerPaddingH = 4.dp,
        innerPaddingV = 2.dp,
        onStateChange = onStateChange
    ) { isActive, isLatched ->
        Text(
            text = if (isLatched) "$badge•" else badge,
            color = when {
                isLatched -> HudAccent
                isActive -> Color.White
                else -> HudCyan.copy(alpha = opacity.coerceIn(0.35f, 1.0f))
            },
            fontWeight = FontWeight.ExtraBold,
            fontSize = (if (badge.length > 3) 10.5f * scale else 13f * scale).sp,
            maxLines = 1
        )
    }
}

@Composable
private fun GamepadConfigurableButtonBox(
    widthDp: Dp?,
    heightDp: Dp?,
    minWidthDp: Dp? = null,
    shape: androidx.compose.ui.graphics.Shape,
    opacity: Float,
    accentColor: Color,
    idleBorderOverride: Color? = null,
    pressMode: GamepadButtonPressMode,
    turboHz: Int,
    isEditMode: Boolean,
    innerPaddingH: Dp = 0.dp,
    innerPaddingV: Dp = 0.dp,
    onStateChange: (Boolean) -> Unit,
    content: @Composable (isActive: Boolean, isLatched: Boolean) -> Unit
) {
    var fingerDown by remember { mutableStateOf(false) }
    var latched by remember { mutableStateOf(false) }
    var emittedState by remember { mutableStateOf(false) }
    val currentOnStateChange by rememberUpdatedState(onStateChange)

    fun emit(next: Boolean) {
        if (emittedState != next) {
            emittedState = next
            currentOnStateChange(next)
        }
    }

    LaunchedEffect(isEditMode, pressMode) {
        if (isEditMode) {
            fingerDown = false
            latched = false
            emit(false)
        } else if (pressMode != GamepadButtonPressMode.TOGGLE_HOLD && latched) {
            latched = false
            if (!fingerDown) emit(false)
        }
    }

    LaunchedEffect(fingerDown, pressMode, turboHz, isEditMode) {
        if (isEditMode) return@LaunchedEffect
        if (pressMode == GamepadButtonPressMode.TURBO && fingerDown) {
            val halfPeriodMs = (500L / turboHz.coerceIn(5, 20)).coerceAtLeast(22L)
            while (fingerDown) {
                emit(true)
                delay(halfPeriodMs)
                emit(false)
                delay(halfPeriodMs)
            }
            emit(false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (emittedState) {
                currentOnStateChange(false)
            }
        }
    }

    val isVisualActive = when (pressMode) {
        GamepadButtonPressMode.NORMAL -> fingerDown
        GamepadButtonPressMode.TOGGLE_HOLD -> latched
        GamepadButtonPressMode.TURBO -> fingerDown || emittedState
    }

    val bgAlpha = (opacity * 0.72f).coerceIn(0.16f, 0.90f)
    val borderAlpha = (opacity * 0.28f).coerceIn(0.08f, 0.45f)

    Box(
        modifier = Modifier
            .then(
                when {
                    widthDp != null && heightDp != null -> Modifier.size(width = widthDp, height = heightDp)
                    heightDp != null && minWidthDp != null -> Modifier.defaultMinSize(minWidth = minWidthDp).height(heightDp)
                    else -> Modifier
                }
            )
            .clip(shape)
            .background(
                if (isVisualActive) {
                    accentColor.copy(alpha = 0.26f)
                } else {
                    HudSurface.copy(alpha = bgAlpha)
                }
            )
            .border(
                width = if (isVisualActive) 1.5.dp else 1.dp,
                color = when {
                    latched -> HudCyan
                    isVisualActive -> accentColor
                    else -> idleBorderOverride ?: Color.White.copy(alpha = borderAlpha)
                },
                shape = shape
            )
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(pressMode, turboHz) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = true)
                            down.consume()
                            fingerDown = true
                            when (pressMode) {
                                GamepadButtonPressMode.NORMAL -> emit(true)
                                GamepadButtonPressMode.TOGGLE_HOLD -> {
                                    latched = !latched
                                    emit(latched)
                                }
                                GamepadButtonPressMode.TURBO -> {}
                            }
                            do {
                                val event = awaitPointerEvent()
                                event.changes.forEach { if (it.pressed) it.consume() }
                            } while (event.changes.any { it.pressed })

                            fingerDown = false
                            if (pressMode == GamepadButtonPressMode.NORMAL) {
                                emit(false)
                            }
                        }
                    }
                }
            )
            .padding(horizontal = innerPaddingH, vertical = innerPaddingV),
        contentAlignment = Alignment.Center
    ) {
        content(isVisualActive, latched)
    }
}

@Composable
internal fun VirtualAbxyPad(
    opacity: Float,
    scale: Float,
    isEditMode: Boolean,
    onButton: (Int, Boolean) -> Unit
) {
    // Slide & Multi-Button Combo ABXY Pad: A=0 (Bottom), B=1 (Right), X=2 (Left), Y=3 (Top)
    val padSize: Dp = (132.dp * scale)
    val buttonSize: Dp = (46.dp * scale)

    var aPressed by remember { mutableStateOf(false) }
    var bPressed by remember { mutableStateOf(false) }
    var xPressed by remember { mutableStateOf(false) }
    var yPressed by remember { mutableStateOf(false) }
    val currentOnButton by rememberUpdatedState(onButton)

    fun updateAbxyState(newA: Boolean, newB: Boolean, newX: Boolean, newY: Boolean) {
        if (newA != aPressed) {
            aPressed = newA
            currentOnButton(0, newA)
        }
        if (newB != bPressed) {
            bPressed = newB
            currentOnButton(1, newB)
        }
        if (newX != xPressed) {
            xPressed = newX
            currentOnButton(2, newX)
        }
        if (newY != yPressed) {
            yPressed = newY
            currentOnButton(3, newY)
        }
    }

    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            updateAbxyState(false, false, false, false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            updateAbxyState(false, false, false, false)
        }
    }

    val containerBgAlpha = (opacity * 0.42f).coerceIn(0.08f, 0.60f)
    val borderAlpha = (opacity * 0.22f).coerceIn(0.06f, 0.36f)

    Box(
        modifier = Modifier
            .size(padSize)
            .clip(CircleShape)
            .background(HudSurface.copy(alpha = containerBgAlpha))
            .border(1.dp, Color.White.copy(alpha = borderAlpha), CircleShape)
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(scale) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = true)
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
        AbxyVisualButton(Modifier.align(Alignment.TopCenter), "Y", PadYColor, buttonSize, yPressed, opacity)
        AbxyVisualButton(Modifier.align(Alignment.BottomCenter), "A", PadAColor, buttonSize, aPressed, opacity)
        AbxyVisualButton(Modifier.align(Alignment.CenterStart), "X", PadXColor, buttonSize, xPressed, opacity)
        AbxyVisualButton(Modifier.align(Alignment.CenterEnd), "B", PadBColor, buttonSize, bPressed, opacity)
    }
}

internal fun hypotDist(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}

@Composable
internal fun AbxyVisualButton(
    modifier: Modifier,
    label: String,
    accentColor: Color,
    sizeDp: Dp,
    isPressed: Boolean,
    opacity: Float
) {
    val bgAlpha = (opacity * 0.72f).coerceIn(0.16f, 0.90f)
    val borderAlpha = (opacity * 0.28f).coerceIn(0.08f, 0.45f)

    Box(
        modifier = modifier
            .size(sizeDp)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                if (isPressed) {
                    accentColor.copy(alpha = 0.28f)
                } else {
                    HudSurface.copy(alpha = bgAlpha)
                }
            )
            .border(
                width = if (isPressed) 1.5.dp else 1.dp,
                color = if (isPressed) accentColor else Color.White.copy(alpha = borderAlpha),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) Color.White else accentColor.copy(alpha = opacity.coerceIn(0.25f, 1.0f)),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp
        )
    }
}
