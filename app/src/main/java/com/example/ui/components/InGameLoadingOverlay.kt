package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.example.ui.theme.JapanRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import kotlinx.coroutines.delay

private val LoadingBgColor = Color(0xFF0B120F)
private val LoadingCardColor = Color(0xFF121C17)
private val LoadingBorderColor = Color(0xFF213128)

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
        enter = fadeIn(tween(if (lowEndDeviceMode) 0 else 220)),
        exit = fadeOut(tween(if (lowEndDeviceMode) 0 else 260)),
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
            realLatencyMs < 80 -> if (isEn) "Good" else "Tốt"
            realLatencyMs < 130 -> if (isEn) "Stable" else "Ổn định"
            else -> if (isEn) "High Ping" else "Ping cao"
        }
        val pingText = if (hasValidPing) "${realLatencyMs}ms" else "-- ms"

        val rawProgress = progress.coerceIn(0.06f, 1f)
        val animatedProgress by animateFloatAsState(
            targetValue = rawProgress,
            animationSpec = tween(durationMillis = if (lowEndDeviceMode) 0 else 300, easing = FastOutSlowInEasing),
            label = "smooth_progress"
        )

        val phaseStatusText = when {
            animatedProgress < 0.25f -> if (isEn) "Connecting to Xbox Cloud..." else "Đang kết nối tới Xbox Cloud..."
            animatedProgress < 0.55f -> if (isEn) "Routing via $targetServerName..." else "Đang kết nối máy chủ $targetServerName..."
            animatedProgress < 0.85f -> if (isEn) "Preparing 60FPS video & audio..." else "Đang chuẩn bị hình ảnh 60FPS & âm thanh..."
            else -> if (isEn) "Ready! Entering game..." else "Đã sẵn sàng! Đang mở trò chơi..."
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
                "Tap the top HUD pill anytime in-game to check ping or adjust graphics."
            } else {
                "Chạm vào thanh HUD nhỏ phía trên bất cứ lúc nào để xem ping hoặc chỉnh đồ họa."
            }
        }

        val controllerStatus = if (hasGamepad) {
            if (isEn) "Physical gamepad connected" else "Đã kết nối tay cầm vật lý"
        } else if (gameProfile.nativeTouchSupport) {
            if (isEn) "Direct Touch + Virtual Gamepad" else "Cảm ứng + Tay cầm ảo"
        } else {
            if (isEn) "Virtual Gamepad ready" else "Tay cầm ảo sẵn sàng"
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(LoadingBgColor)
                .testTag("ingame_loading_overlay")
        ) {
            val isLandscape = maxWidth > maxHeight || maxWidth >= 600.dp

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
            .padding(horizontal = 18.dp, vertical = 12.dp)
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
                .widthIn(max = 460.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GameHeroBannerCard(
                gameProfile = gameProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
            )

            LoadingProgressPanel(
                animatedProgress = animatedProgress,
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

        SkipLoadingButton(
            onSkip = onSkip,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
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
            .padding(horizontal = 22.dp, vertical = 10.dp),
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
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GameHeroBannerCard(
                gameProfile = gameProfile,
                modifier = Modifier
                    .weight(0.44f)
                    .fillMaxHeight()
            )

            Column(
                modifier = Modifier
                    .weight(0.56f)
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
            shape = RoundedCornerShape(10.dp),
            color = LoadingCardColor,
            border = BorderStroke(1.dp, LoadingBorderColor),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
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
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = LoadingCardColor,
            border = BorderStroke(1.dp, LoadingBorderColor),
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
                    fontFamily = JetBrainsMonoFontFamily,
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
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, LoadingBorderColor, RoundedCornerShape(16.dp)),
        color = LoadingCardColor
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
                                Color(0x26080D0B),
                                Color(0x88080D0B),
                                Color(0xEB0B120F)
                            )
                        )
                    )
            )

            // Top Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xD90E1612),
                    border = BorderStroke(0.8.dp, LoadingBorderColor)
                ) {
                    Text(
                        text = gameProfile.localizedBadge(isEn),
                        color = TextPrimary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xD90E1612),
                    border = BorderStroke(0.8.dp, XboxNeonGreen.copy(alpha = 0.45f))
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
                            fontFamily = JetBrainsMonoFontFamily,
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
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = gameProfile.title,
                    color = Color.White,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
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
    phaseStatusText: String,
    currentStep: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, LoadingBorderColor, RoundedCornerShape(14.dp)),
        color = LoadingCardColor
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
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    color = XboxNeonGreen,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = XboxGreen,
                trackColor = Color(0xFF1C2A22)
            )

            val config by com.example.data.SettingsRepository.config.collectAsState()
            val isEn = config.isEnglishUI()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StepBadge(label = if (isEn) "Connect" else "Kết nối", isDone = currentStep >= 1, isActive = currentStep == 1)
                StepBadge(label = if (isEn) "Server" else "Máy chủ", isDone = currentStep >= 2, isActive = currentStep == 2)
                StepBadge(label = if (isEn) "60FPS" else "Luồng 60FPS", isDone = currentStep >= 3, isActive = currentStep == 3)
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
        isActive || isDone -> XboxNeonGreen
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
            color = if (isActive) TextPrimary else color,
            fontSize = 10.sp,
            fontWeight = if (isActive || isDone) FontWeight.SemiBold else FontWeight.Normal
        )
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
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, LoadingBorderColor, RoundedCornerShape(14.dp)),
        color = LoadingCardColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = if (compact) 9.dp else 11.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            GameOptimizationRow(
                icon = Icons.Default.Public,
                title = if (isEn) "Server" else "Máy chủ",
                status = targetServerName,
                accentColor = TextPrimary
            )

            GameOptimizationRow(
                icon = Icons.Default.Gamepad,
                title = if (isEn) "Controls" else "Điều khiển",
                status = controllerStatus,
                accentColor = if (hasGamepad) XboxNeonGreen else TextPrimary
            )

            HorizontalDivider(color = LoadingBorderColor, thickness = 0.8.dp)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = XboxNeonGreen,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = tip,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 14.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
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
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(44.dp)
            .testTag("skip_loading_button")
    ) {
        Text(
            text = if (isEn) "Enter Game Now" else "Vào Trận Ngay",
            fontFamily = SpaceGroteskFontFamily,
            fontSize = 13.sp,
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
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
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
