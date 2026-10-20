package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JapanRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurfaceVariant
import com.example.vpn.NetworkMode

@Composable
fun PingVisualizerCard(
    currentPing: Int,
    history: List<Int>,
    mode: NetworkMode,
    modifier: Modifier = Modifier
) {
    val quality = when {
        currentPing in 1..35 -> "S • Xuất sắc" to XboxNeonGreen
        currentPing in 36..65 -> "A • Tốt" to DirectCyan
        currentPing in 66..110 -> "B • Trung bình" to WarningAmber
        else -> "C • Có độ trễ" to JapanRed
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, XboxCardBorder, RoundedCornerShape(16.dp)),
        color = XboxSurfaceVariant
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ĐỘ TRỄ MẠNG THEO THỜI GIAN",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = quality.second.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = quality.first,
                        color = quality.second,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "$currentPing",
                        color = if (mode == NetworkMode.DIRECT_NETWORK) DirectCyan else JapanRed,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "ms RTT",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Text(
                    text = if (mode == NetworkMode.DIRECT_NETWORK) "⚡ Trực tiếp Azure Tokyo" else "🇯🇵 Qua Relay Japan",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Canvas chart
            val strokeColor = if (mode == NetworkMode.DIRECT_NETWORK) DirectCyan else JapanRed
            val sampleData = if (history.isEmpty()) listOf(130, 125, 120, 115, 30, 28, 26, 25) else history

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
            ) {
                val paddingPx = 8.dp.toPx()
                val effectiveWidth = (size.width - 2 * paddingPx).coerceAtLeast(1f)
                val height = size.height
                if (sampleData.size < 2) return@Canvas

                val maxVal = (sampleData.maxOrNull() ?: 150).coerceAtLeast(100).toFloat()
                val minVal = (sampleData.minOrNull() ?: 20).coerceAtMost(20).toFloat()
                val range = (maxVal - minVal).coerceAtLeast(1f)

                val stepX = effectiveWidth / (sampleData.size - 1)
                val path = Path()
                val fillPath = Path()

                sampleData.forEachIndexed { index, value ->
                    val x = paddingPx + index * stepX
                    val normalizedY = (value - minVal) / range
                    val y = height - (normalizedY * (height * 0.7f) + height * 0.15f)

                    if (index == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }

                val lastX = paddingPx + effectiveWidth
                fillPath.lineTo(lastX, height)
                fillPath.close()

                // Draw gradient fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(strokeColor.copy(alpha = 0.35f), Color.Transparent),
                        startY = 0f,
                        endY = height
                    )
                )

                // Draw stroke
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Highlight latest point safely without edge clipping
                val lastVal = sampleData.last()
                val lastY = height - (((lastVal - minVal) / range) * (height * 0.7f) + height * 0.15f)
                drawCircle(color = strokeColor, radius = 5.dp.toPx(), center = Offset(lastX, lastY))
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(lastX, lastY))
            }
        }
    }
}
