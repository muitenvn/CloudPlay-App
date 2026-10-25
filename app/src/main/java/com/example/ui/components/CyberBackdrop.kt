package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.SettingsRepository
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.XboxDark
import com.example.ui.theme.XboxNeonGreen

/**
 * Next-Gen Console Velocity Backdrop (CloudPlay UI 2.0).
 * Uses drawWithCache to pre-compile all radial/linear brushes and grid geometry once per layout size,
 * eliminating continuous 60 FPS full-screen canvas invalidations and GPU fill-rate contention during tab transitions.
 */
@Composable
fun CyberBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val config by SettingsRepository.config.collectAsState()
    val isLowEnd = config.lowEndDeviceMode

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(XboxDark)
            .drawWithCache {
                val w = size.width
                val h = size.height
                val gridStep = 44.dp.toPx().coerceAtLeast(32f)
                val gridColor = XboxNeonGreen.copy(alpha = 0.024f)
                val beamStroke = 64.dp.toPx()
                val bracketLen = 18.dp.toPx()
                val bracketInset = 8.dp.toPx()
                val bracketColor = DirectCyan.copy(alpha = 0.14f)

                val topLeftAuroraBrush = Brush.radialGradient(
                    colors = listOf(
                        XboxNeonGreen.copy(alpha = 0.13f),
                        XboxNeonGreen.copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.17f, h * 0.06f),
                    radius = (w * 0.85f).coerceAtLeast(1f)
                )

                val bottomRightCyanBrush = Brush.radialGradient(
                    colors = listOf(
                        CyberCyan.copy(alpha = 0.095f),
                        DirectCyan.copy(alpha = 0.02f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.85f, h * 0.86f),
                    radius = (w * 0.78f).coerceAtLeast(1f)
                )

                val centerVignetteBrush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF13261C).copy(alpha = 0.32f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.42f),
                    radius = (w * 0.95f).coerceAtLeast(1f)
                )

                val diagonalBeamBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        XboxNeonGreen.copy(alpha = 0.022f),
                        DirectCyan.copy(alpha = 0.022f),
                        Color.Transparent
                    ),
                    start = Offset(0f, h * 0.15f),
                    end = Offset(w, h * 0.65f)
                )

                val topHorizonBrush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        XboxNeonGreen.copy(alpha = 0.45f),
                        DirectCyan.copy(alpha = 0.45f),
                        Color.Transparent
                    )
                )

                onDrawBehind {
                    // 1. Subtle Tactical Telemetry Grid
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

                    // 2. Top-left Xbox Velocity Green aurora
                    drawCircle(brush = topLeftAuroraBrush)

                    // 3. Bottom-right Direct Cyan ambient reactor glow
                    drawCircle(brush = bottomRightCyanBrush)

                    // 4. Center deep emerald console chamber vignette
                    drawCircle(brush = centerVignetteBrush)

                    // 5. UI 3.0 Diagonal Cyber-Light Beam (skipped in low-end mode)
                    if (!isLowEnd) {
                        drawLine(
                            brush = diagonalBeamBrush,
                            start = Offset(0f, h * 0.15f),
                            end = Offset(w, h * 0.65f),
                            strokeWidth = beamStroke
                        )
                    }

                    // 6. Top-edge subtle laser horizon highlight
                    drawLine(
                        brush = topHorizonBrush,
                        start = Offset(w * 0.03f, 0f),
                        end = Offset(w * 0.97f, 0f),
                        strokeWidth = 2.5f
                    )

                    // 7. Subtle Corner Console HUD Micro-Brackets
                    drawLine(
                        color = bracketColor,
                        start = Offset(bracketInset, bracketInset),
                        end = Offset(bracketInset + bracketLen, bracketInset),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = bracketColor,
                        start = Offset(bracketInset, bracketInset),
                        end = Offset(bracketInset, bracketInset + bracketLen),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = bracketColor,
                        start = Offset(w - bracketInset - bracketLen, bracketInset),
                        end = Offset(w - bracketInset, bracketInset),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = bracketColor,
                        start = Offset(w - bracketInset, bracketInset),
                        end = Offset(w - bracketInset, bracketInset + bracketLen),
                        strokeWidth = 1.5f
                    )
                }
            }
    ) {
        content()
    }
}

