package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.XboxDark
import com.example.ui.theme.XboxNeonGreen

import androidx.compose.runtime.collectAsState
import com.example.data.SettingsRepository

/**
 * Next-Gen Console Velocity Backdrop (CloudPlay UI 2.0).
 * Combines a breathing ambient aurora with a subtle tactical telemetry grid and top laser rim.
 */
@Composable
fun CyberBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val config by SettingsRepository.config.collectAsState()
    val pulseShift = if (config.lowEndDeviceMode) {
        0.5f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "cyber_backdrop_transition")
        val animated by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 6000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "aurora_pulse"
        )
        animated
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(XboxDark)
            .drawBehind {
                val w = size.width
                val h = size.height

                // 1. Subtle Tactical Telemetry Grid
                val gridStep = 44.dp.toPx().coerceAtLeast(32f)
                val gridColor = XboxNeonGreen.copy(alpha = 0.024f)
                var x = 0f
                while (x < w) {
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1f
                    )
                    x += gridStep
                }
                var y = 0f
                while (y < h) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                    y += gridStep
                }

                // 2. Top-left Xbox Velocity Green breathing aurora
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            XboxNeonGreen.copy(alpha = 0.11f + 0.04f * pulseShift),
                            XboxNeonGreen.copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                        center = Offset(w * (0.14f + 0.06f * pulseShift), h * 0.06f),
                        radius = w * 0.85f
                    )
                )

                // 3. Bottom-right Direct Cyan ambient reactor glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CyberCyan.copy(alpha = 0.08f + 0.03f * (1f - pulseShift)),
                            DirectCyan.copy(alpha = 0.02f),
                            Color.Transparent
                        ),
                        center = Offset(w * (0.88f - 0.05f * pulseShift), h * 0.86f),
                        radius = w * 0.78f
                    )
                )

                // 4. Center deep emerald console chamber vignette
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF13261C).copy(alpha = 0.30f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.5f, h * 0.42f),
                        radius = w * 0.95f
                    )
                )

                // 5. Top-edge subtle laser horizon highlight
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            XboxNeonGreen.copy(alpha = 0.35f),
                            DirectCyan.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(w * 0.05f, 0f),
                    end = Offset(w * 0.95f, 0f),
                    strokeWidth = 2f
                )
            }
    ) {
        content()
    }
}

