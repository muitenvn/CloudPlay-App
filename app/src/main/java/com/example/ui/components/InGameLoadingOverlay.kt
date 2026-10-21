package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun InGameLoadingOverlay(
    isLoading: Boolean,
    progress: Float,
    targetServerName: String = "Tokyo, Nhật Bản",
    hasGamepad: Boolean = false,
    gameProfile: GameOptimizationProfile = GameOptimizationRepository.FORTNITE_PROFILE,
    realLatencyMs: Int? = null,
    lowEndDeviceMode: Boolean = false,
    onNavigateHome: () -> Unit = {},
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(tween(if (lowEndDeviceMode) 0 else 260)),
        exit = fadeOut(tween(if (lowEndDeviceMode) 0 else 320)),
        modifier = modifier
    ) {
        val primaryAccent = Color(gameProfile.primaryAccentHex)
        val secondaryAccent = Color(gameProfile.secondaryAccentHex)

        // Khi bật Chế độ máy yếu: tắt các hiệu ứng động (infinite animation) của màn tải
        val ambientGlowAlpha = if (lowEndDeviceMode) {
            0.16f
        } else {
            val infiniteTransition = rememberInfiniteTransition(label = "ambient_loading")
            val animatedAlpha by infiniteTransition.animateFloat(
                initialValue = 0.14f,
                targetValue = 0.28f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "ambient_glow"
            )
            animatedAlpha
        }

        val config by com.example.data.SettingsRepository.config.collectAsState()
        val isEn = config.isEnglishUI()
        val activeTips = gameProfile.localizedTips(isEn)

        var tipIndex by remember { mutableIntStateOf(0) }
        LaunchedEffect(activeTips, lowEndDeviceMode) {
            if (!lowEndDeviceMode) {
                while (true) {
                    delay(4000)
                    if (activeTips.isNotEmpty()) {
                        tipIndex = (tipIndex + 1) % activeTips.size
                    }
                }
            }
        }

        val hasValidPing = realLatencyMs != null && realLatencyMs > 0
        val pingColor = when {
            !hasValidPing -> TextSecondary
            realLatencyMs < 50 -> DirectCyan
            realLatencyMs < 90 -> XboxNeonGreen
            realLatencyMs < 130 -> Color(0xFFFFB300)
            else -> JapanRed
        }
        val pingRating = when {
            !hasValidPing -> if (isEn) "Measuring" else "Đang đo"
            realLatencyMs < 50 -> if (isEn) "Ultra Smooth" else "Cực mượt"
            realLatencyMs < 90 -> if (isEn) "Great" else "Rất tốt"
            realLatencyMs < 130 -> if (isEn) "Stable" else "Ổn định"
            else -> if (isEn) "High Ping" else "Ping cao"
        }
        val pingText = if (hasValidPing) "${realLatencyMs}ms" else "-- ms"

        val rawProgress = progress.coerceIn(0.06f, 1f)
        val animatedProgress by animateFloatAsState(
            targetValue = rawProgress,
            animationSpec = tween(durationMillis = if (lowEndDeviceMode) 0 else 350, easing = FastOutSlowInEasing),
            label = "smooth_progress"
        )

        val phaseStatusText = when {
            animatedProgress < 0.25f -> if (isEn) "Initializing cloud gaming session..." else "Đang khởi tạo phiên kết nối đám mây..."
            animatedProgress < 0.55f -> if (isEn) "Routing via server $targetServerName..." else "Đang định tuyến qua máy chủ $targetServerName..."
            animatedProgress < 0.80f -> if (isEn) "Synchronizing 60 FPS video & audio stream..." else "Đang đồng bộ luồng hình ảnh 60 FPS & âm thanh..."
            animatedProgress < 0.92f -> if (isEn) "Optimizing input latency & controller..." else "Đang tối ưu độ trễ đầu vào & bộ điều khiển..."
            else -> if (isEn) "Ready! Launching game interface..." else "Đã sẵn sàng! Đang mở giao diện trò chơi..."
        }

        val currentStep = when {
            animatedProgress < 0.25f -> 1
            animatedProgress < 0.55f -> 2
            animatedProgress < 0.85f -> 3
            else -> 4
        }

        val currentTip = if (activeTips.isNotEmpty()) {
            activeTips.getOrNull(tipIndex) ?: activeTips.first()
        } else {
            if (isEn) {
                "You can open the Floating HUD anytime to adjust graphics quality and check network latency."
            } else {
                "Bạn có thể mở thanh công cụ nổi bất cứ lúc nào để chỉnh đồ họa và kiểm tra mạng."
            }
        }

        val controllerStatus = if (hasGamepad) {
            if (isEn) "Physical controller connected" else "Tay cầm vật lý đã kết nối"
        } else if (gameProfile.nativeTouchSupport) {
            if (isEn) "Native Touch + Virtual Gamepad" else "Cảm ứng trực tiếp + Phím ảo"
        } else {
            if (isEn) "Virtual Gamepad ready" else "Tay cầm ảo sẵn sàng"
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(XboxDark)
                .testTag("ingame_loading_overlay")
        ) {
            val isLandscape = maxWidth > maxHeight || maxWidth >= 600.dp

            // Subtle ambient background glow
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryAccent.copy(alpha = ambientGlowAlpha),
                            XboxNeonGreen.copy(alpha = ambientGlowAlpha * 0.45f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.25f, h * 0.25f),
                        radius = w.coerceAtLeast(h) * 0.55f
                    ),
                    center = Offset(w * 0.25f, h * 0.25f),
                    radius = w.coerceAtLeast(h) * 0.55f
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            DirectCyan.copy(alpha = ambientGlowAlpha * 0.65f),
                            secondaryAccent.copy(alpha = ambientGlowAlpha * 0.3f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.8f, h * 0.78f),
                        radius = w.coerceAtLeast(h) * 0.5f
                    ),
                    center = Offset(w * 0.8f, h * 0.78f),
                    radius = w.coerceAtLeast(h) * 0.5f
                )
            }

            if (isLandscape) {
                LandscapeLoadingContent(
                    gameProfile = gameProfile,
                    animatedProgress = animatedProgress,
                    phaseStatusText = phaseStatusText,
                    currentStep = currentStep,
                    targetServerName = targetServerName,
                    controllerStatus = controllerStatus,
                    hasGamepad = hasGamepad,
                    pingText = pingText,
                    pingRating = pingRating,
                    pingColor = pingColor,
                    currentTip = currentTip,
                    onNavigateHome = onNavigateHome,
                    onSkip = onSkip
                )
            } else {
                PortraitLoadingContent(
                    gameProfile = gameProfile,
                    animatedProgress = animatedProgress,
                    phaseStatusText = phaseStatusText,
                    currentStep = currentStep,
                    targetServerName = targetServerName,
                    controllerStatus = controllerStatus,
                    hasGamepad = hasGamepad,
                    pingText = pingText,
                    pingRating = pingRating,
                    pingColor = pingColor,
                    currentTip = currentTip,
                    onNavigateHome = onNavigateHome,
                    onSkip = onSkip
                )
            }
        }
    }
}

@Composable
private fun PortraitLoadingContent(
    gameProfile: GameOptimizationProfile,
    animatedProgress: Float,
    phaseStatusText: String,
    currentStep: Int,
    targetServerName: String,
    controllerStatus: String,
    hasGamepad: Boolean,
    pingText: String,
    pingRating: String,
    pingColor: Color,
    currentTip: String,
    onNavigateHome: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LoadingTopBar(
            pingText = pingText,
            pingRating = pingRating,
            pingColor = pingColor,
            onNavigateHome = onNavigateHome
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GameHeroBannerCard(
                gameProfile = gameProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(195.dp)
            )

            LoadingProgressPanel(
                animatedProgress = animatedProgress,
                phaseStatusText = phaseStatusText,
                currentStep = currentStep
            )

            SystemTelemetryCard(
                targetServerName = targetServerName,
                controllerStatus = controllerStatus,
                hasGamepad = hasGamepad
            )

            LoadingTipCard(tip = currentTip)
        }

        SkipLoadingButton(
            onSkip = onSkip,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(top = 8.dp, bottom = 4.dp)
        )
    }
}

@Composable
private fun LandscapeLoadingContent(
    gameProfile: GameOptimizationProfile,
    animatedProgress: Float,
    phaseStatusText: String,
    currentStep: Int,
    targetServerName: String,
    controllerStatus: String,
    hasGamepad: Boolean,
    pingText: String,
    pingRating: String,
    pingColor: Color,
    currentTip: String,
    onNavigateHome: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LoadingTopBar(
            pingText = pingText,
            pingRating = pingRating,
            pingColor = pingColor,
            onNavigateHome = onNavigateHome
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Hero Poster Card
            GameHeroBannerCard(
                gameProfile = gameProfile,
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight()
            )

            // Right Column: Progress, Telemetry, Tip & Action Button
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                LoadingProgressPanel(
                    animatedProgress = animatedProgress,
                    phaseStatusText = phaseStatusText,
                    currentStep = currentStep
                )

                Spacer(modifier = Modifier.height(8.dp))

                SystemTelemetryCard(
                    targetServerName = targetServerName,
                    controllerStatus = controllerStatus,
                    hasGamepad = hasGamepad
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        LoadingTipCard(tip = currentTip, compact = true)
                    }

                    SkipLoadingButton(
                        onSkip = onSkip,
                        modifier = Modifier.width(155.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingTopBar(
    pingText: String,
    pingRating: String,
    pingColor: Color,
    onNavigateHome: () -> Unit
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF101B24).copy(alpha = 0.9f),
            border = BorderStroke(1.dp, XboxCardBorder),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onNavigateHome() }
                .testTag("loading_home_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = if (isEn) "Home" else "Trang chủ",
                    tint = DirectCyan,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (isEn) "Home" else "Trang Chủ",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF101B24).copy(alpha = 0.9f),
            border = BorderStroke(1.dp, pingColor.copy(alpha = 0.45f)),
            modifier = Modifier.testTag("loading_ping_badge")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(pingColor)
                )
                Text(
                    text = pingText,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• $pingRating",
                    color = pingColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun GameHeroBannerCard(
    gameProfile: GameOptimizationProfile,
    modifier: Modifier = Modifier
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        DirectCyan.copy(alpha = 0.5f),
                        XboxNeonGreen.copy(alpha = 0.45f)
                    )
                ),
                RoundedCornerShape(20.dp)
            ),
        color = Color(0xFF0A141C),
        shadowElevation = 12.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = gameProfile.bannerResId),
                contentDescription = gameProfile.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x40050A0E),
                                Color(0x88050A0E),
                                Color(0xEE060D13)
                            )
                        )
                    )
            )

            // Top Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xCC071118),
                    border = BorderStroke(0.8.dp, DirectCyan.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = gameProfile.localizedBadge(isEn),
                        color = DirectCyan,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xCC071118),
                    border = BorderStroke(0.8.dp, XboxNeonGreen.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = XboxNeonGreen,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = gameProfile.recommendedFps,
                            color = XboxNeonGreen,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bottom Title & Subtitle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = gameProfile.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = gameProfile.localizedSubtitle(isEn),
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun LoadingProgressPanel(
    animatedProgress: Float,
    phaseStatusText: String,
    currentStep: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, XboxCardBorder, RoundedCornerShape(16.dp)),
        color = Color(0xFF0A141D).copy(alpha = 0.92f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = phaseStatusText,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    color = XboxNeonGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = XboxNeonGreen,
                trackColor = Color(0xFF152431)
            )

            val config by com.example.data.SettingsRepository.config.collectAsState()
            val isEn = config.isEnglishUI()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StepBadge(label = if (isEn) "Connect" else "Kết nối", isDone = currentStep >= 1, isActive = currentStep == 1)
                StepBadge(label = if (isEn) "Route" else "Định tuyến", isDone = currentStep >= 2, isActive = currentStep == 2)
                StepBadge(label = if (isEn) "60FPS Stream" else "Luồng 60FPS", isDone = currentStep >= 3, isActive = currentStep == 3)
                StepBadge(label = if (isEn) "Ready" else "Sẵn sàng", isDone = currentStep >= 4, isActive = currentStep == 4)
            }
        }
    }
}

@Composable
private fun StepBadge(
    label: String,
    isDone: Boolean,
    isActive: Boolean
) {
    val color = when {
        isActive -> DirectCyan
        isDone -> XboxNeonGreen
        else -> TextSecondary.copy(alpha = 0.45f)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = if (isActive || isDone) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun SystemTelemetryCard(
    targetServerName: String,
    controllerStatus: String,
    hasGamepad: Boolean
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, XboxCardBorder, RoundedCornerShape(16.dp)),
        color = Color(0xFF0A141D).copy(alpha = 0.92f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            GameOptimizationRow(
                icon = Icons.Default.Public,
                title = if (isEn) "Connected Server" else "Máy chủ kết nối",
                status = targetServerName,
                accentColor = DirectCyan
            )

            GameOptimizationRow(
                icon = Icons.Default.Gamepad,
                title = if (isEn) "Controller" else "Bộ điều khiển",
                status = controllerStatus,
                accentColor = if (hasGamepad) XboxNeonGreen else DirectCyan
            )

            GameOptimizationRow(
                icon = Icons.Default.Bolt,
                title = if (isEn) "Route Optimization" else "Tối ưu đường truyền",
                status = if (isEn) "Auto zero-lag switch in-game" else "Tự động xả trễ khi vào game",
                accentColor = XboxNeonGreen
            )
        }
    }
}

@Composable
private fun LoadingTipCard(
    tip: String,
    compact: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(0.8.dp, DirectCyan.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
        color = Color(0xFF0B1722).copy(alpha = 0.88f)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = if (compact) 8.dp else 10.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = XboxNeonGreen,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = tip,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 14.5.sp,
                maxLines = if (compact) 2 else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SkipLoadingButton(
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    Button(
        onClick = onSkip,
        colors = ButtonDefaults.buttonColors(
            containerColor = DirectCyan.copy(alpha = 0.18f),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, DirectCyan.copy(alpha = 0.55f)),
        modifier = modifier
            .height(42.dp)
            .testTag("skip_loading_button")
    ) {
        Text(
            text = if (isEn) "Enter Game Now ➜" else "Vào Trận Ngay ➜",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GameOptimizationRow(
    icon: ImageVector,
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
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = status,
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

