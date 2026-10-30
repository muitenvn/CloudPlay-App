package com.example.ui.components

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import kotlinx.coroutines.delay

private val ConsoleDeepBg = Color(0xFF070D0A)
private val ConsoleCardSurface = Color(0xEB101B15)
private val ConsoleCardElevated = Color(0xF515231B)
private val ConsoleSubtleBorder = Color(0xFF263B2F)
private val ConsoleAccentBorder = Color(0xFF2E573C)

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
        enter = fadeIn(tween(if (lowEndDeviceMode) 0 else 240)),
        exit = fadeOut(tween(if (lowEndDeviceMode) 0 else 280)),
        modifier = modifier
    ) {
        val config by com.example.data.SettingsRepository.config.collectAsState()
        val isEn = config.isEnglishUI()
        val activeTips = gameProfile.localizedTips(isEn)

        var tipIndex by remember { mutableIntStateOf(0) }
        LaunchedEffect(activeTips, lowEndDeviceMode) {
            if (!lowEndDeviceMode) {
                while (true) {
                    delay(4500)
                    if (activeTips.isNotEmpty()) {
                        tipIndex = (tipIndex + 1) % activeTips.size
                    }
                }
            }
        }

        val hasValidPing = realLatencyMs != null && realLatencyMs > 0
        val pingColor = when {
            !hasValidPing -> TextSecondary
            realLatencyMs < 80 -> XboxNeonGreen
            realLatencyMs < 130 -> Color(0xFFFFB300)
            else -> JapanRed
        }
        val pingRating = when {
            !hasValidPing -> if (isEn) "Measuring" else "Đang đo"
            realLatencyMs < 80 -> if (isEn) "Optimal" else "Tối ưu"
            realLatencyMs < 130 -> if (isEn) "Stable" else "Ổn định"
            else -> if (isEn) "High Ping" else "Ping cao"
        }
        val pingText = if (hasValidPing) "${realLatencyMs}ms" else "-- ms"

        val rawProgress = progress.coerceIn(0.08f, 1f)
        val animatedProgress by animateFloatAsState(
            targetValue = rawProgress,
            animationSpec = tween(durationMillis = if (lowEndDeviceMode) 0 else 340, easing = FastOutSlowInEasing),
            label = "smooth_progress"
        )

        val pulseAlpha = if (lowEndDeviceMode) {
            0.85f
        } else {
            val infiniteTransition = rememberInfiniteTransition(label = "console_pulse")
            val animated by infiniteTransition.animateFloat(
                initialValue = 0.45f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1100, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_alpha"
            )
            animated
        }

        val phaseStatusText = when {
            animatedProgress < 0.25f -> if (isEn) "Connecting to Xbox Cloud Console..." else "Đang kết nối hệ thống Xbox Cloud..."
            animatedProgress < 0.55f -> if (isEn) "Routing via $targetServerName..." else "Đang định tuyến qua máy chủ $targetServerName..."
            animatedProgress < 0.85f -> if (isEn) "Synchronizing 60FPS stream & audio..." else "Đang đồng bộ luồng hình 60FPS & âm thanh..."
            else -> if (isEn) "Stream ready! Launching game..." else "Luồng phát hoàn tất! Sẵn sàng vào trận..."
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
                "Tap the top HUD pill anytime in-game to check ping or adjust controls."
            } else {
                "Chạm vào viên thuốc HUD phía trên bất cứ lúc nào để xem ping hoặc chỉnh tay cầm."
            }
        }

        val controllerStatus = if (hasGamepad) {
            if (isEn) "Physical Controller" else "Tay cầm vật lý"
        } else if (gameProfile.nativeTouchSupport) {
            if (isEn) "Touch + Virtual Pad" else "Cảm ứng + Tay cầm ảo"
        } else {
            if (isEn) "Virtual Gamepad" else "Tay cầm ảo"
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(ConsoleDeepBg)
                .testTag("ingame_loading_overlay")
        ) {
            // Atmospheric game cover backdrop (subtle, moody console depth)
            Image(
                painter = painterResource(id = gameProfile.bannerResId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.18f)
            )

            // Multi-stop dark emerald console vignette overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xDE060C09),
                                Color(0xC809140E),
                                Color(0xF2060B08)
                            )
                        )
                    )
            )

            val isLandscape = maxWidth > maxHeight || maxWidth >= 600.dp

            if (isLandscape) {
                LandscapeLoadingContent(
                    gameProfile = gameProfile,
                    animatedProgress = animatedProgress,
                    pulseAlpha = pulseAlpha,
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
                    pulseAlpha = pulseAlpha,
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
    pulseAlpha: Float,
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
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LoadingTopBar(
            pingText = pingText,
            pingRating = pingRating,
            pingColor = pingColor,
            pulseAlpha = pulseAlpha,
            onNavigateHome = onNavigateHome
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GameHeroBannerCard(
                gameProfile = gameProfile,
                pulseAlpha = pulseAlpha,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(198.dp)
            )

            LoadingProgressPanel(
                animatedProgress = animatedProgress,
                pulseAlpha = pulseAlpha,
                phaseStatusText = phaseStatusText,
                currentStep = currentStep
            )

            SystemTelemetryAndTipCard(
                targetServerName = targetServerName,
                controllerStatus = controllerStatus,
                hasGamepad = hasGamepad,
                tip = currentTip
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        SkipLoadingButton(
            onSkip = onSkip,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(bottom = 4.dp)
        )
    }
}

@Composable
private fun LandscapeLoadingContent(
    gameProfile: GameOptimizationProfile,
    animatedProgress: Float,
    pulseAlpha: Float,
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
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LoadingTopBar(
            pingText = pingText,
            pingRating = pingRating,
            pingColor = pingColor,
            pulseAlpha = pulseAlpha,
            onNavigateHome = onNavigateHome
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GameHeroBannerCard(
                gameProfile = gameProfile,
                pulseAlpha = pulseAlpha,
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight()
            )

            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                LoadingProgressPanel(
                    animatedProgress = animatedProgress,
                    pulseAlpha = pulseAlpha,
                    phaseStatusText = phaseStatusText,
                    currentStep = currentStep
                )

                Spacer(modifier = Modifier.height(8.dp))

                SystemTelemetryAndTipCard(
                    targetServerName = targetServerName,
                    controllerStatus = controllerStatus,
                    hasGamepad = hasGamepad,
                    tip = currentTip,
                    compact = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                SkipLoadingButton(
                    onSkip = onSkip,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LoadingTopBar(
    pingText: String,
    pingRating: String,
    pingColor: Color,
    pulseAlpha: Float,
    onNavigateHome: () -> Unit
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = ConsoleCardElevated,
                border = BorderStroke(1.dp, ConsoleSubtleBorder),
                modifier = Modifier
                    .clip(RoundedCornerShape(11.dp))
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
                        tint = TextPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (isEn) "Home" else "Trang chủ",
                        color = TextPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = XboxGreen.copy(alpha = 0.16f),
                border = BorderStroke(0.8.dp, ConsoleAccentBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.5.dp)
                            .clip(CircleShape)
                            .background(XboxNeonGreen.copy(alpha = pulseAlpha))
                    )
                    Text(
                        text = "XBOX CLOUD",
                        color = XboxNeonGreen,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(11.dp),
            color = ConsoleCardElevated,
            border = BorderStroke(1.dp, ConsoleSubtleBorder),
            modifier = Modifier.testTag("loading_ping_badge")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.5.dp)
                        .clip(CircleShape)
                        .background(pingColor)
                )
                Text(
                    text = pingText,
                    color = TextPrimary,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = pingColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = pingRating,
                        color = pingColor,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GameHeroBannerCard(
    gameProfile: GameOptimizationProfile,
    pulseAlpha: Float,
    modifier: Modifier = Modifier
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    Surface(
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        XboxNeonGreen.copy(alpha = 0.48f),
                        ConsoleSubtleBorder,
                        ConsoleAccentBorder
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            ),
        color = ConsoleCardSurface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = gameProfile.bannerResId),
                contentDescription = gameProfile.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Cinematic console gradient vignette
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x40050A08),
                                Color(0x7507100B),
                                Color(0xF208120D)
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
                    color = Color(0xE60B1510),
                    border = BorderStroke(0.8.dp, ConsoleAccentBorder)
                ) {
                    Text(
                        text = gameProfile.localizedBadge(isEn),
                        color = TextPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.5.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xE60B1510),
                    border = BorderStroke(0.8.dp, XboxNeonGreen.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = XboxNeonGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${gameProfile.recommendedFps} • 1080P",
                            color = XboxNeonGreen,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Bottom Console Title Block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(XboxNeonGreen.copy(alpha = pulseAlpha))
                    )
                    Text(
                        text = if (isEn) "CLOUD CONSOLE SESSION" else "PHIÊN CHƠI ĐÁM MÂY XBOX",
                        color = XboxNeonGreen,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.7.sp
                    )
                }

                Text(
                    text = gameProfile.title,
                    color = Color.White,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = gameProfile.localizedSubtitle(isEn),
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LoadingProgressPanel(
    animatedProgress: Float,
    pulseAlpha: Float,
    phaseStatusText: String,
    currentStep: Int
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, ConsoleAccentBorder, RoundedCornerShape(16.dp)),
        color = ConsoleCardSurface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(XboxNeonGreen.copy(alpha = pulseAlpha))
                    )
                    Text(
                        text = phaseStatusText,
                        color = TextPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = XboxGreen.copy(alpha = 0.24f),
                    border = BorderStroke(0.8.dp, XboxNeonGreen.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        color = XboxNeonGreen,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp)
                    )
                }
            }

            // Rich gradient progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF0A140F))
                    .border(0.6.dp, ConsoleSubtleBorder, RoundedCornerShape(5.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.coerceIn(0.05f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(XboxGreen, XboxNeonGreen)
                            )
                        )
                )
            }

            // 4 Step Capsules
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StepBadge(
                    label = if (isEn) "Connect" else "Kết nối",
                    isDone = currentStep >= 1,
                    isActive = currentStep == 1,
                    modifier = Modifier.weight(1f)
                )
                StepBadge(
                    label = if (isEn) "Server" else "Máy chủ",
                    isDone = currentStep >= 2,
                    isActive = currentStep == 2,
                    modifier = Modifier.weight(1f)
                )
                StepBadge(
                    label = if (isEn) "60FPS" else "Luồng 60FPS",
                    isDone = currentStep >= 3,
                    isActive = currentStep == 3,
                    modifier = Modifier.weight(1f)
                )
                StepBadge(
                    label = if (isEn) "Ready" else "Sẵn sàng",
                    isDone = currentStep >= 4,
                    isActive = currentStep == 4,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StepBadge(
    label: String,
    isDone: Boolean,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isActive -> XboxGreen.copy(alpha = 0.28f)
        isDone -> XboxGreen.copy(alpha = 0.14f)
        else -> Color(0xFF0C1510)
    }
    val borderColor = when {
        isActive -> XboxNeonGreen.copy(alpha = 0.7f)
        isDone -> ConsoleAccentBorder
        else -> ConsoleSubtleBorder.copy(alpha = 0.6f)
    }
    val contentColor = when {
        isActive -> XboxNeonGreen
        isDone -> TextPrimary
        else -> TextSecondary.copy(alpha = 0.5f)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(0.8.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isDone || isActive) XboxNeonGreen else contentColor,
                modifier = Modifier.size(10.5.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = contentColor,
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 9.5.sp,
                fontWeight = if (isActive || isDone) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SystemTelemetryAndTipCard(
    targetServerName: String,
    controllerStatus: String,
    hasGamepad: Boolean,
    tip: String,
    compact: Boolean = false
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, ConsoleSubtleBorder, RoundedCornerShape(16.dp)),
        color = ConsoleCardSurface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = if (compact) 9.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            // Two Bento Telemetry Tiles side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryMiniTile(
                    icon = Icons.Default.Public,
                    label = if (isEn) "SERVER ROUTE" else "MÁY CHỦ KẾT NỐI",
                    value = targetServerName,
                    iconTint = DirectCyan,
                    valueColor = TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                TelemetryMiniTile(
                    icon = Icons.Default.Gamepad,
                    label = if (isEn) "CONTROLS" else "ĐIỀU KHIỂN",
                    value = controllerStatus,
                    iconTint = XboxNeonGreen,
                    valueColor = if (hasGamepad) XboxNeonGreen else TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = ConsoleSubtleBorder, thickness = 0.8.dp)

            // Pro Tip Strip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(XboxGreen.copy(alpha = 0.22f))
                        .border(0.8.dp, ConsoleAccentBorder, RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = XboxNeonGreen,
                        modifier = Modifier.size(14.dp)
                    )
                }

                AnimatedContent(
                    targetState = tip,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                    label = "loading_tip_anim",
                    modifier = Modifier.weight(1f)
                ) { targetTip ->
                    Text(
                        text = targetTip,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 14.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryMiniTile(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(11.dp),
        color = Color(0xFF0C1611),
        border = BorderStroke(0.8.dp, ConsoleSubtleBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(15.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = label,
                    color = TextSecondary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    color = valueColor,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
            containerColor = XboxGreen,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        shape = RoundedCornerShape(13.dp),
        modifier = modifier
            .height(46.dp)
            .border(1.dp, XboxNeonGreen.copy(alpha = 0.6f), RoundedCornerShape(13.dp))
            .testTag("skip_loading_button")
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isEn) "ENTER GAME NOW" else "VÀO TRẬN NGAY",
            fontFamily = SpaceGroteskFontFamily,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
    }
}
