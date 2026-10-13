package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GameOptimizationProfile
import com.example.data.GameOptimizationRepository
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JapanRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxDark
import com.example.ui.theme.XboxNeonGreen
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun InGameLoadingOverlay(
    isLoading: Boolean,
    progress: Float,
    targetServerName: String = "Tokyo, Nhật Bản",
    hasGamepad: Boolean = false,
    gameProfile: GameOptimizationProfile = GameOptimizationRepository.FORTNITE_PROFILE,
    realLatencyMs: Int? = null,
    onNavigateHome: () -> Unit = {},
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(400)),
        modifier = modifier
    ) {
        val primaryAccent = Color(gameProfile.primaryAccentHex)
        val secondaryAccent = Color(gameProfile.secondaryAccentHex)

        // Infinite transitions for dynamic aerospace physics and particle effects
        val infiniteTransition = rememberInfiniteTransition(label = "flight_dynamics")

        // Altitude hover oscillation (cruising physics)
        val altitudeHoverY by infiniteTransition.animateFloat(
            initialValue = -6f,
            targetValue = 6f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "altitude"
        )

        // Bank tilt angle (-1.8 deg to +1.8 deg)
        val bankTiltAngle by infiniteTransition.animateFloat(
            initialValue = -1.5f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(3400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bank_tilt"
        )

        // Supersonic plasma engine pulse
        val engineFlareScale by infiniteTransition.animateFloat(
            initialValue = 0.88f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(650, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "engine_flare"
        )

        // Sonic boom shockwave expansion progress
        val shockwaveProgress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shockwave"
        )

        // Speed warp streaks animation
        val warpOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "warp_streaks"
        )

        // Rotate through tips smoothly
        var tipIndex by remember { mutableIntStateOf(0) }
        LaunchedEffect(gameProfile.tips) {
            while (true) {
                delay(3500)
                if (gameProfile.tips.isNotEmpty()) {
                    tipIndex = (tipIndex + 1) % gameProfile.tips.size
                }
            }
        }

        // Realistic Ping & Quality status
        val measuredPing = realLatencyMs ?: 29
        val pingColor = when {
            measuredPing < 50 -> DirectCyan
            measuredPing < 90 -> XboxNeonGreen
            measuredPing < 130 -> Color(0xFFFFB300)
            else -> JapanRed
        }
        val pingRating = when {
            measuredPing < 50 -> "Cực mượt"
            measuredPing < 90 -> "Rất tốt"
            measuredPing < 130 -> "Ổn định"
            else -> "Cao"
        }

        // Dynamic flight phase synced to loading progress
        val effectiveProgress = progress.coerceIn(0.08f, 1f)
        val flightPhaseTitle = when {
            effectiveProgress < 0.25f -> "Khởi động buồng lái Cloud Stream..."
            effectiveProgress < 0.50f -> "Bứt tốc xuyên tầng mây • Đang kết nối máy chủ Tokyo..."
            effectiveProgress < 0.75f -> "Đồng bộ hóa luồng WebRTC H.264 / 60 FPS..."
            effectiveProgress < 0.92f -> "Hiệu chỉnh độ trễ siêu thấp (Zero-Lag) & Sẵn sàng..."
            else -> "Đã vào không phận game! Đang hạ cánh..."
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(XboxDark)
                .testTag("ingame_loading_overlay"),
            contentAlignment = Alignment.Center
        ) {
            // LAYER 1: Futuristic Atmospheric Backdrop (Darkened cosmic nebula)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF0F2420),
                                Color(0xFF071110),
                                Color(0xFF030708)
                            )
                        )
                    )
            )

            // LAYER 2: Warp Speed Streaks Canvas (Aerospace Cloud Flight)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw high-speed light streaks rushing past the aircraft
                val streakCount = 35
                for (i in 0 until streakCount) {
                    val seed = (i * 137).toDouble()
                    val y = (seed * 97 % h).toFloat()
                    val speed = 1.2f + (i % 5) * 0.4f
                    val currentX = ((warpOffset * speed + (i * 73)) % (w + 400)) - 200
                    val streakLength = 80f + (i % 4) * 60f
                    val alpha = (0.15f + (i % 3) * 0.12f).coerceIn(0.1f, 0.45f)

                    val streakColor = if (i % 3 == 0) DirectCyan else XboxNeonGreen

                    drawLine(
                        color = streakColor.copy(alpha = alpha),
                        start = Offset(currentX, y),
                        end = Offset(currentX + streakLength, y + (altitudeHoverY * 0.4f)),
                        strokeWidth = (1.5f + (i % 2)).dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Sonic Boom Shockwave Ring expanding from center
                val shockwaveRadius = shockwaveProgress * (w * 0.55f)
                val shockwaveAlpha = (1f - shockwaveProgress).coerceIn(0f, 0.65f)
                drawCircle(
                    color = DirectCyan.copy(alpha = shockwaveAlpha * 0.35f),
                    radius = shockwaveRadius,
                    center = Offset(w * 0.5f, h * 0.34f),
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // LAYER 3: Main Scrollable Content Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // TOP BAR: Home Escape Button & Real Latency Indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home Button (Direct escape to dashboard)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = DirectCyan.copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DirectCyan.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onNavigateHome() }
                            .testTag("loading_home_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Trang chủ",
                                tint = DirectCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Trang Chủ",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Real Latency Telemetry Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = pingColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, pingColor.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("loading_ping_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(pingColor)
                            )
                            Text(
                                text = "⚡ ${measuredPing}ms",
                                color = TextPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• $pingRating",
                                color = pingColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // CENTERPIECE: The Futuristic Supersonic Cloud Gaming Jet Artwork with Physics Animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .offset(y = altitudeHoverY.dp)
                        .rotate(bankTiltAngle),
                    contentAlignment = Alignment.Center
                ) {
                    // Ambient Engine Glow Aura behind the craft
                    Box(
                        modifier = Modifier
                            .size(240.dp, 140.dp)
                            .scale(engineFlareScale)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        DirectCyan.copy(alpha = 0.35f),
                                        XboxNeonGreen.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Hero Aircraft Visual: Supersonic Cloud Jet
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(185.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .border(
                                1.2.dp,
                                Brush.horizontalGradient(listOf(DirectCyan.copy(alpha = 0.7f), XboxNeonGreen.copy(alpha = 0.7f))),
                                RoundedCornerShape(22.dp)
                            ),
                        color = Color(0xFF081216),
                        shadowElevation = 18.dp
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter = painterResource(id = R.drawable.cloud_jet_loading),
                                contentDescription = "Supersonic Cloud Gaming Jet",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Sleek dark vignette edge gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.3f),
                                                Color(0xCC050A0E)
                                            )
                                        )
                                    )
                            )

                            // In-flight Speed Protocol Badge (Bottom-Right of cockpit banner)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.65f),
                                border = androidx.compose.foundation.BorderStroke(0.6.dp, DirectCyan.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FlightTakeoff,
                                        contentDescription = null,
                                        tint = XboxNeonGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "X-STREAM CRUISE // 60 FPS",
                                        color = TextPrimary,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Game Title & Optimization Profile
                Text(
                    text = gameProfile.title.uppercase(),
                    color = TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    textAlign = TextAlign.Center
                )

                // Flight Status Text (Dynamically changes with progress)
                Text(
                    text = flightPhaseTitle,
                    color = DirectCyan,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp, start = 8.dp, end = 8.dp)
                )

                // Modern Sleek Progress Bar with Percentage
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tiến độ tải dữ liệu luồng",
                        color = TextSecondary,
                        fontSize = 10.5.sp
                    )
                    Text(
                        text = "${(effectiveProgress * 100).toInt()}%",
                        color = XboxNeonGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                LinearProgressIndicator(
                    progress = { effectiveProgress },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(6.5.dp)
                        .clip(RoundedCornerShape(3.5.dp)),
                    color = XboxNeonGreen,
                    trackColor = Color(0xFF101B24)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Flight Optimization & Hardware Telemetry Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, XboxCardBorder, RoundedCornerShape(16.dp)),
                    color = Color(0xFF091218).copy(alpha = 0.94f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        // Server Route
                        GameOptimizationRow(
                            icon = Icons.Default.Public,
                            title = "Đường truyền & Máy chủ",
                            status = targetServerName,
                            accentColor = DirectCyan
                        )

                        // Controller
                        GameOptimizationRow(
                            icon = Icons.Default.Gamepad,
                            title = "Hệ thống điều khiển",
                            status = if (hasGamepad) "Tay cầm vật lý kết nối" else (if (gameProfile.nativeTouchSupport) "Cảm ứng Native Epic + Ảo" else "Phím ảo sẵn sàng"),
                            accentColor = if (hasGamepad) XboxNeonGreen else DirectCyan
                        )

                        // Zero-lag protocol
                        GameOptimizationRow(
                            icon = Icons.Default.Bolt,
                            title = "Giao thức Zero-Lag",
                            status = "Tự động kích hoạt khi vào luồng",
                            accentColor = XboxNeonGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Rotating Pro Tip
                if (gameProfile.tips.isNotEmpty()) {
                    val currentTip = gameProfile.tips.getOrNull(tipIndex) ?: gameProfile.tips[0]
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(0.8.dp, DirectCyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                        color = Color(0xFF0B171F).copy(alpha = 0.85f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = XboxNeonGreen,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = currentTip,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Row: Skip / Enter Now
                Button(
                    onClick = onSkip,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DirectCyan.copy(alpha = 0.22f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DirectCyan.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .testTag("skip_loading_button")
                ) {
                    Text(
                        text = "Vào Trận Ngay ➜",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun GameOptimizationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    status: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
        Text(
            text = status,
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
