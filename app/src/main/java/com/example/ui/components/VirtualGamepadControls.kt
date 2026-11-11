package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.XboxNeonGreen
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
    val currentAutoSprint by rememberUpdatedState(enableAutoSprintL3)

    fun applyDeadzoneAndCurve(rawNorm: Float): Float {
        val absVal = abs(rawNorm)
        val effInner = currentInnerDeadzone.coerceIn(0f, 0.30f)
        val effOuter = currentOuterDeadzone.coerceIn((effInner + 0.10f).coerceAtMost(1f), 1f)
        if (absVal <= effInner) return 0f
        val rescaled = ((absVal - effInner) / (effOuter - effInner)).coerceIn(0f, 1f)
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
                                if (abs(nextStickX - stickX) >= 0.5f || abs(nextStickY - stickY) >= 0.5f) {
                                    stickX = nextStickX
                                    stickY = nextStickY
                                }
                                val rawX = (nextStickX / maxRadiusPx).coerceIn(-1f, 1f)
                                val rawY = (nextStickY / maxRadiusPx).coerceIn(-1f, 1f)
                                val curvedX = applyDeadzoneAndCurve(rawX)
                                val curvedY = applyDeadzoneAndCurve(rawY)

                                val deltaX = abs(curvedX - lastSentX)
                                val deltaY = abs(curvedY - lastSentY)
                                if (deltaX >= 0.014f || deltaY >= 0.014f || (curvedX == 0f && curvedY == 0f && (lastSentX != 0f || lastSentY != 0f))) {
                                    val nowMs = android.os.SystemClock.uptimeMillis()
                                    val isFastFlickOrZero = forceImmediate ||
                                        (curvedX == 0f && curvedY == 0f) ||
                                        deltaX >= 0.16f ||
                                        deltaY >= 0.16f
                                    if (isFastFlickOrZero || (nowMs - lastSentAtMs) >= 12L) {
                                        lastSentX = curvedX
                                        lastSentY = curvedY
                                        lastSentAtMs = nowMs
                                        onMove(curvedX, curvedY)
                                    }
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
                color = Color.White.copy(alpha = 0.12f),
                radius = maxRadiusPx * currentOuterDeadzone.coerceIn(0.50f, 1.0f),
                center = c,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = DirectCyan.copy(alpha = 0.18f),
                radius = maxRadiusPx * currentInnerDeadzone.coerceIn(0.0f, 0.35f),
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
internal fun StandaloneDpadButton(
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
internal fun DpadVisualKey(
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
internal fun GamepadTriggerButton(
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
internal fun GamepadShoulderButton(
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
internal fun GamepadThumbClickMiniButton(
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
internal fun GamepadSmallButton(
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
internal fun GamepadXboxNexusButton(
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
internal fun StandaloneAbxyButton(
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
internal fun VirtualAbxyPad(
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
