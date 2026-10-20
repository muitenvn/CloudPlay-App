package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.XboxDark
import com.example.ui.theme.XboxNeonGreen

/**
 * Atmospheric background backdrop that provides high-end Xbox cyberpunk glow.
 * Replaces plain flat backgrounds with a luxurious gaming aura.
 */
@Composable
fun CyberBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(XboxDark)
            .drawBehind {
                // Top-left Xbox Neon Green soft ambient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            XboxNeonGreen.copy(alpha = 0.08f),
                            XboxNeonGreen.copy(alpha = 0.02f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.08f),
                        radius = size.width * 0.75f
                    )
                )

                // Bottom-right Cyber Cyan ambient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CyberCyan.copy(alpha = 0.06f),
                            CyberCyan.copy(alpha = 0.01f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.9f, size.height * 0.85f),
                        radius = size.width * 0.7f
                    )
                )

                // Center subtle atmospheric tint
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF14241B).copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.4f),
                        radius = size.width * 0.9f
                    )
                )
            }
    ) {
        content()
    }
}
