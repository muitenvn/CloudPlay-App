package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.XboxNeonGreen
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun VirtualGamepadOverlay(
    webView: WebView?,
    opacity: Float = 0.65f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
        } catch (ignored: Exception) {
        }
    }

    fun sendButtonEvent(buttonIndex: Int, pressed: Boolean) {
        if (pressed) vibrate()
        webView?.evaluateJavascript(
            "window.dispatchEvent&&window.dispatchEvent(new CustomEvent('virtual_gamepad_button',{detail:{index:$buttonIndex,pressed:$pressed}}));",
            null
        )
    }

    fun sendStickAxesPair(xAxisIndex: Int, xVal: Float, yAxisIndex: Int, yVal: Float) {
        val rx = (xVal * 100f).roundToInt() / 100f
        val ry = (yVal * 100f).roundToInt() / 100f
        webView?.evaluateJavascript(
            "window.dispatchEvent&&window.dispatchEvent(new CustomEvent('virtual_gamepad_axes_pair',{detail:{xAxis:$xAxisIndex,xVal:$rx,yAxis:$yAxisIndex,yVal:$ry}}));",
            null
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("virtual_gamepad_overlay")
    ) {
        // Ergonomic Shoulder Triggers (Top Left and Top Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GamepadShoulderButton("LT", 6, opacity) { sendButtonEvent(6, it) }
                GamepadShoulderButton("LB", 4, opacity) { sendButtonEvent(4, it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GamepadShoulderButton("RB", 5, opacity) { sendButtonEvent(5, it) }
                GamepadShoulderButton("RT", 7, opacity) { sendButtonEvent(7, it) }
            }
        }

        // Center Xbox Controls: View, Nexus Xbox, Menu
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GamepadSmallButton("⧉ View", 8, opacity) { sendButtonEvent(8, it) }
            GamepadXboxNexusButton(opacity) { sendButtonEvent(16, it) }
            GamepadSmallButton("☰ Menu", 9, opacity) { sendButtonEvent(9, it) }
        }

        // Left Controls: Analog Thumbstick & D-Pad Cross
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            VirtualAnalogStick(
                opacity = opacity,
                label = "L",
                onMove = { x, y ->
                    sendStickAxesPair(0, x, 1, y)
                }
            )

            VirtualDpadCross(
                opacity = opacity,
                onButton = { index, pressed -> sendButtonEvent(index, pressed) }
            )
        }

        // Right Controls: Right Analog Stick & Authentic Xbox ABXY Pad
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            VirtualAnalogStick(
                opacity = opacity * 0.9f,
                label = "R",
                onMove = { x, y ->
                    sendStickAxesPair(2, x, 3, y)
                }
            )

            VirtualAbxyPad(opacity = opacity) { index, pressed ->
                sendButtonEvent(index, pressed)
            }
        }
    }
}

@Composable
private fun VirtualAnalogStick(
    opacity: Float,
    label: String,
    onMove: (Float, Float) -> Unit
) {
    var stickX by remember { mutableFloatStateOf(0f) }
    var stickY by remember { mutableFloatStateOf(0f) }
    var lastSentX by remember { mutableFloatStateOf(0f) }
    var lastSentY by remember { mutableFloatStateOf(0f) }
    val maxRadius = 55f

    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = opacity * 0.65f))
            .border(1.5.dp, Color(0x6600E676), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        stickX = 0f
                        stickY = 0f
                        lastSentX = 0f
                        lastSentY = 0f
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        stickX = 0f
                        stickY = 0f
                        lastSentX = 0f
                        lastSentY = 0f
                        onMove(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newX = stickX + dragAmount.x
                        val newY = stickY + dragAmount.y
                        val dist = sqrt(newX * newX + newY * newY)
                        if (dist <= maxRadius) {
                            stickX = newX
                            stickY = newY
                        } else {
                            stickX = (newX / dist) * maxRadius
                            stickY = (newY / dist) * maxRadius
                        }
                        val normX = stickX / maxRadius
                        val normY = stickY / maxRadius
                        if (kotlin.math.abs(normX - lastSentX) >= 0.015f || kotlin.math.abs(normY - lastSentY) >= 0.015f) {
                            lastSentX = normX
                            lastSentY = normY
                            onMove(normX, normY)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Crosshair subtle guide lines
        Box(
            modifier = Modifier
                .size(70.dp)
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
        )

        // Inner thumb knob
        Box(
            modifier = Modifier
                .offset { IntOffset(stickX.roundToInt(), stickY.roundToInt()) }
                .size(50.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF2A3D32).copy(alpha = opacity),
                            Color(0xFF142018).copy(alpha = opacity)
                        )
                    )
                )
                .border(2.dp, XboxNeonGreen.copy(alpha = opacity * 0.9f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = opacity),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun VirtualDpadCross(
    opacity: Float,
    onButton: (Int, Boolean) -> Unit
) {
    // Standard Xbox button index for DPad: Up=12, Down=13, Left=14, Right=15
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = opacity * 0.6f))
            .border(1.dp, Color(0x4400E676), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Up
        DpadDirectionButton(Modifier.align(Alignment.TopCenter), "▲", 12, opacity, onButton)
        // Down
        DpadDirectionButton(Modifier.align(Alignment.BottomCenter), "▼", 13, opacity, onButton)
        // Left
        DpadDirectionButton(Modifier.align(Alignment.CenterStart), "◀", 14, opacity, onButton)
        // Right
        DpadDirectionButton(Modifier.align(Alignment.CenterEnd), "▶", 15, opacity, onButton)
    }
}

@Composable
private fun DpadDirectionButton(
    modifier: Modifier,
    symbol: String,
    buttonIndex: Int,
    opacity: Float,
    onButton: (Int, Boolean) -> Unit
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1E2E24).copy(alpha = opacity * 0.8f))
            .pointerInput(buttonIndex) {
                detectTapGestures(
                    onPress = {
                        onButton(buttonIndex, true)
                        tryAwaitRelease()
                        onButton(buttonIndex, false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = Color.White.copy(alpha = opacity),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GamepadShoulderButton(
    label: String,
    buttonIndex: Int,
    opacity: Float,
    onStateChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 62.dp, height = 36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF263D2E).copy(alpha = opacity * 0.9f),
                        Color(0xFF0F1A13).copy(alpha = opacity * 0.9f)
                    )
                )
            )
            .border(1.2.dp, XboxNeonGreen.copy(alpha = opacity * 0.7f), RoundedCornerShape(10.dp))
            .pointerInput(buttonIndex) {
                detectTapGestures(
                    onPress = {
                        onStateChange(true)
                        tryAwaitRelease()
                        onStateChange(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = opacity),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun GamepadSmallButton(
    label: String,
    index: Int,
    opacity: Float,
    onButton: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = opacity * 0.7f))
            .border(1.dp, Color(0x6600E676), RoundedCornerShape(8.dp))
            .pointerInput(index) {
                detectTapGestures(
                    onPress = {
                        onButton(true)
                        tryAwaitRelease()
                        onButton(false)
                    }
                )
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = opacity),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GamepadXboxNexusButton(
    opacity: Float,
    onButton: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = opacity))
            .border(2.dp, XboxNeonGreen.copy(alpha = opacity), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onButton(true)
                        tryAwaitRelease()
                        onButton(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "✕",
            color = XboxNeonGreen.copy(alpha = opacity),
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun VirtualAbxyPad(
    opacity: Float,
    onButton: (Int, Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .size(126.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = opacity * 0.6f))
            .border(1.5.dp, Color(0x6600E676), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Iconic Xbox Button Colors: Y=Yellow, A=Green, X=Blue, B=Red
        AbxyButton(Modifier.align(Alignment.TopCenter), "Y", Color(0xFFFFD166), 3, opacity, onButton)
        AbxyButton(Modifier.align(Alignment.BottomCenter), "A", Color(0xFF00E676), 0, opacity, onButton)
        AbxyButton(Modifier.align(Alignment.CenterStart), "X", Color(0xFF00B4D8), 2, opacity, onButton)
        AbxyButton(Modifier.align(Alignment.CenterEnd), "B", Color(0xFFFF3366), 1, opacity, onButton)
    }
}

@Composable
private fun AbxyButton(
    modifier: Modifier,
    label: String,
    accentColor: Color,
    index: Int,
    opacity: Float,
    onButton: (Int, Boolean) -> Unit
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        accentColor.copy(alpha = opacity * 0.35f),
                        Color.Black.copy(alpha = opacity * 0.9f)
                    )
                )
            )
            .border(1.8.dp, accentColor.copy(alpha = opacity), CircleShape)
            .pointerInput(index) {
                detectTapGestures(
                    onPress = {
                        onButton(index, true)
                        tryAwaitRelease()
                        onButton(index, false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = accentColor.copy(alpha = opacity),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp
        )
    }
}
