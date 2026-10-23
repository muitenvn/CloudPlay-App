package com.example.ui.tabs

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AppConfig
import com.example.data.AppLanguage
import com.example.data.CellularOptimizationMode
import com.example.data.GamepadHapticLevel
import com.example.data.GamepadLayoutPreset
import com.example.data.POPULAR_XBOX_LANGUAGES
import com.example.data.SettingsRepository
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant
import com.example.vpn.VpnConnectionState
import com.example.vpn.VpnEngine
import com.example.vpn.VpnManager

@Composable
fun SettingsScreen(
    config: AppConfig,
    vpnState: VpnConnectionState,
    onOpenGuide: (com.example.ui.components.GuideSection) -> Unit = {},
    onClearCookies: () -> Unit,
    modifier: Modifier = Modifier
) {
    var customInputText by remember(config.customLocale) {
        mutableStateOf(if (config.useSystemLanguage) "xbox.com/en-US" else "xbox.com/${config.customLocale}")
    }
    var showTermsDialog by remember { mutableStateOf(false) }

    // 5 nhóm gập/mở (mặc định gập, chỉ nhóm đồ họa chính mở)
    var expandGraphics by rememberSaveable { mutableStateOf(true) }
    var expandNetwork by rememberSaveable { mutableStateOf(false) }
    var expandControls by rememberSaveable { mutableStateOf(false) }
    var expandAdvanced by rememberSaveable { mutableStateOf(false) }
    var expandRecording by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val resolvedSystemLocale = remember { SettingsRepository.resolveSystemXboxLocale() }
    val isEn = config.isEnglishUI()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // =========================================================================
        // NHÓM 1: HÌNH ẢNH & HIỆU NĂNG (Mặc định mở)
        // =========================================================================
        item {
            CollapsibleSettingsGroup(
                title = if (isEn) "Graphics & Performance" else "Hình ảnh & Hiệu năng",
                subtitle = config.cellularMode.localizedTitle(isEn),
                icon = Icons.Default.Speed,
                expanded = expandGraphics,
                onToggle = { expandGraphics = !expandGraphics },
                testTag = "settings_group_graphics"
            ) {
                // 1.1 Đồ họa & hiệu năng máy + Chế độ máy yếu
                SettingsCard(title = if (isEn) "Graphics & Device Performance" else "Đồ họa & hiệu năng máy") {
                    val selectedMode = config.cellularMode
                    val isAutoSelected = selectedMode == CellularOptimizationMode.AUTO

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.2.dp,
                                if (isAutoSelected) XboxNeonGreen else XboxCardBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { SettingsRepository.updateCellularMode(CellularOptimizationMode.AUTO) }
                            .testTag("settings_graphics_mode_auto"),
                        color = if (isAutoSelected) Color(0xFF13241A) else Color(0xFF0F1813)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isAutoSelected) XboxNeonGreen else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isEn) "Auto (Recommended)" else "Tự động (Khuyên dùng)",
                                        color = if (isAutoSelected) XboxNeonGreen else TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isEn) "Adjusts to network and keeps phone cool" else "Tự điều chỉnh theo mạng và giữ máy mát",
                                        color = TextSecondary,
                                        fontSize = 10.5.sp
                                    )
                                }
                            }
                            if (isAutoSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    val manualModes = listOf(
                        CellularOptimizationMode.DATA_SAVER_HIGH_SPEED,
                        CellularOptimizationMode.MEDIUM,
                        CellularOptimizationMode.HIGH,
                        CellularOptimizationMode.MAX_SPEED_UNCAPPED
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        manualModes.chunked(2).forEach { rowModes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowModes.forEach { mode ->
                                    val isSelected = selectedMode == mode
                                    val isLowOrMed = mode == CellularOptimizationMode.DATA_SAVER_HIGH_SPEED ||
                                        mode == CellularOptimizationMode.MEDIUM
                                    val accent = if (isLowOrMed) XboxNeonGreen else DirectCyan

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                1.dp,
                                                if (isSelected) accent else XboxCardBorder,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { SettingsRepository.updateCellularMode(mode) }
                                            .testTag("settings_graphics_mode_${mode.id}"),
                                        color = if (isSelected) {
                                            if (isLowOrMed) Color(0xFF132219) else Color(0xFF0F2327)
                                        } else {
                                            Color(0xFF0F1813)
                                        }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = mode.localizedTitle(isEn),
                                                    color = if (isSelected) accent else TextPrimary,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = accent,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = mode.localizedSubtitle(isEn),
                                                color = TextSecondary,
                                                fontSize = 10.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Ưu tiên phản hồi mượt
                    SettingToggleRow(
                        title = if (isEn) "Prioritize Smooth Response" else "Ưu tiên phản hồi mượt",
                        subtitle = if (isEn) "Reduces delay when network signal drops" else "Giảm trễ thao tác khi sóng yếu",
                        checked = config.wifiSpeedFirstPriority,
                        onCheckedChange = { SettingsRepository.updateWifiSpeedFirstPriority(it) },
                        activeColor = DirectCyan
                    )

                    // Chế độ máy yếu
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (config.lowEndDeviceMode) Color(0xFF13241A) else Color(0xFF141D17))
                            .border(
                                0.8.dp,
                                if (config.lowEndDeviceMode) XboxNeonGreen.copy(alpha = 0.6f) else XboxCardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isEn) "Low-End Device Mode" else "Chế độ máy yếu",
                                    color = if (config.lowEndDeviceMode) XboxNeonGreen else TextPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isEn) {
                                        "Uses light graphics, keeps 60 FPS, turns off animations and pauses translator."
                                    } else {
                                        "Dùng đồ họa nhẹ, giữ 60 FPS, tắt hiệu ứng động và tạm dừng bộ dịch."
                                    },
                                    color = TextSecondary,
                                    fontSize = 10.5.sp
                                )
                            }
                            Switch(
                                checked = config.lowEndDeviceMode,
                                onCheckedChange = { SettingsRepository.updateLowEndDeviceMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = XboxNeonGreen
                                ),
                                modifier = Modifier.testTag("settings_low_end_device_mode_switch")
                            )
                        }
                        Text(
                            text = if (isEn) "💡 Exit and relaunch the game to apply" else "💡 Thoát game và vào lại để áp dụng",
                            color = if (config.lowEndDeviceMode) XboxNeonGreen else DirectCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // 1.2 Chất lượng hình ảnh & điều khiển
                SettingsCard(title = if (isEn) "Image Quality & Controls" else "Chất lượng hình ảnh & điều khiển") {
                    SettingToggleRow(
                        title = if (isEn) "1080p Full HD Resolution" else "Độ phân giải 1080p Full HD",
                        subtitle = if (isEn) "Displays sharp 1080p image instead of 720p" else "Hiển thị hình ảnh 1080p sắc nét thay vì 720p",
                        checked = config.forceDesktopUserAgent,
                        onCheckedChange = { SettingsRepository.updateDesktopUserAgent(it) }
                    )

                    SettingToggleRow(
                        title = if (isEn) "Clarity Boost (Sharpening)" else "Bộ lọc làm nét (Clarity Boost)",
                        subtitle = if (isEn) "Sharpens game details and contrast" else "Tăng độ sắc nét và độ tương phản trong game",
                        checked = config.clarityBoostEnabled,
                        onCheckedChange = { SettingsRepository.updateClarityBoost(it) }
                    )
                }

                // 1.3 Điều chỉnh đồ họa sâu & Giảm gánh nặng phần cứng
                SettingsCard(title = if (isEn) "Graphics Scaling & Hardware Load Reduction" else "Điều chỉnh đồ họa & Giảm tải phần cứng") {
                    Text(
                        text = if (isEn) "Render Resolution Scale" else "Tỷ lệ độ phân giải dựng hình (Giảm nóng & chống tụt FPS)",
                        color = XboxNeonGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        com.example.data.StreamRenderScale.entries.chunked(2).forEach { rowScales ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowScales.forEach { scale ->
                                    val isSelected = config.streamRenderScale == scale
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                1.dp,
                                                if (isSelected) DirectCyan else XboxCardBorder,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { SettingsRepository.updateStreamRenderScale(scale) }
                                            .testTag("settings_render_scale_${scale.id}"),
                                        color = if (isSelected) Color(0xFF0F2327) else Color(0xFF0F1813)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = scale.localizedTitle(isEn),
                                                color = if (isSelected) DirectCyan else TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = scale.localizedDesc(isEn),
                                                color = TextSecondary,
                                                fontSize = 9.5.sp,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = if (isEn) "Frame Rate Cap (FPS)" else "Giới hạn nhịp khung hình (FPS)",
                        color = XboxNeonGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.data.StreamFpsCap.entries.forEach { fpsCap ->
                            val isSelected = config.streamFpsCap == fpsCap
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) XboxNeonGreen else XboxCardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { SettingsRepository.updateStreamFpsCap(fpsCap) }
                                    .testTag("settings_fps_cap_${fpsCap.fps}"),
                                color = if (isSelected) Color(0xFF13241A) else Color(0xFF0F1813)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "${fpsCap.fps} FPS",
                                        color = if (isSelected) XboxNeonGreen else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = fpsCap.localizedDesc(isEn),
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }

                    SettingToggleRow(
                        title = if (isEn) "Hardware GPU Offload & Anti-Lag" else "Giảm tải GPU & Tắt hiệu ứng nền nặng",
                        subtitle = if (isEn) {
                            "Removes web blur, shadows and background animations so GPU focuses 100% on decoding video"
                        } else {
                            "Tắt làm mờ, đổ bóng và hoạt ảnh trang web để GPU tập trung 100% giải mã luồng game mượt nhất"
                        },
                        checked = config.gpuLoadReductionEnabled,
                        onCheckedChange = { SettingsRepository.updateGpuLoadReductionEnabled(it) },
                        activeColor = DirectCyan
                    )
                }

                // 1.4 Tỷ lệ khung hình & Màn hình hiển thị chống bóp dẹt (Aspect Ratio & Anti-Squish)
                SettingsCard(title = if (isEn) "Screen Aspect Ratio & Anti-Squish Display" else "Tỷ lệ khung hình & Chống bóp méo hình") {
                    Text(
                        text = if (isEn) {
                            "Xbox Cloud games stream in native 16:9 aspect ratio. On modern tall smartphone screens (19.5:9 or 20:9), choose how video is displayed so characters and HUD are never squished or stretched."
                        } else {
                            "Game Xbox Cloud phát luồng ở tỉ lệ chuẩn 16:9 gốc. Trên điện thoại màn hình dài (19.5:9, 20:9), bạn có thể chọn chế độ hiển thị phù hợp để nhân vật và nút bấm không bị bóp dẹt hay kéo méo."
                        },
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    com.example.data.VideoAspectRatioMode.entries.forEach { mode ->
                        val isSelected = config.videoAspectRatioMode == mode
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    1.dp,
                                    if (isSelected) DirectCyan else XboxCardBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { SettingsRepository.updateVideoAspectRatioMode(mode) }
                                .testTag("settings_aspect_ratio_${mode.id}"),
                            color = if (isSelected) Color(0xFF0F2327) else Color(0xFF0F1813)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.localizedTitle(isEn),
                                        color = if (isSelected) DirectCyan else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = mode.localizedDesc(isEn),
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = DirectCyan, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    if (config.videoAspectRatioMode == com.example.data.VideoAspectRatioMode.CUSTOM_SCALE) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LabeledSlider(
                                label = if (isEn) "Horizontal Width" else "Độ rộng chiều Ngang",
                                valueText = "${(config.videoScaleX * 100).toInt()}%",
                                value = config.videoScaleX,
                                onValueChange = { SettingsRepository.updateVideoScaleX(it) },
                                valueRange = 0.70f..1.40f,
                                accentColor = DirectCyan
                            )
                            LabeledSlider(
                                label = if (isEn) "Vertical Height" else "Độ cao chiều Dọc",
                                valueText = "${(config.videoScaleY * 100).toInt()}%",
                                value = config.videoScaleY,
                                onValueChange = { SettingsRepository.updateVideoScaleY(it) },
                                valueRange = 0.70f..1.40f,
                                accentColor = XboxNeonGreen
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // NHÓM 2: MẠNG, MÁY CHỦ & DỊCH (Mặc định gập)
        // =========================================================================
        item {
            CollapsibleSettingsGroup(
                title = if (isEn) "Network, Server & Translation" else "Mạng, Máy chủ & Dịch",
                subtitle = if (isEn) "Smart VPN, Region, Translator" else "VPN thông minh, Vượt rào, Dịch",
                icon = Icons.Default.Public,
                expanded = expandNetwork,
                onToggle = { expandNetwork = !expandNetwork },
                testTag = "settings_group_network"
            ) {
                // 2.1 VPN thông minh
                SettingsCard(title = if (isEn) "Smart VPN" else "VPN thông minh") {
                    SettingToggleRow(
                        title = if (isEn) "Enable Smart VPN (Auto On/Off)" else "Bật VPN thông minh (Tự bật/tắt)",
                        subtitle = if (isEn) "Turns on VPN to open game, turns off once inside" else "Tự bật VPN khi mở game, tự tắt khi đã vào trận",
                        checked = config.smartVpnEnabled,
                        onCheckedChange = { SettingsRepository.updateSmartVpnEnabled(it) },
                        switchTestTag = "settings_switch_smart_vpn"
                    )

                    SettingToggleRow(
                        title = if (isEn) "Auto turn off VPN in game" else "Tự tắt VPN khi vào game",
                        subtitle = if (isEn) "Switches to direct network for low ping" else "Chuyển về mạng trực tiếp để giảm ping",
                        checked = config.autoBypassEnabled,
                        onCheckedChange = { SettingsRepository.updateAutoBypass(it) },
                        switchTestTag = "settings_switch_auto_bypass"
                    )

                    if (config.autoBypassEnabled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .padding(10.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = if (isEn) "Delay before turning off VPN" else "Thời gian chờ trước khi tắt VPN",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = if (isEn) "${config.autoBypassDelaySeconds}s" else "${config.autoBypassDelaySeconds} giây",
                                    color = DirectCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            Slider(
                                value = config.autoBypassDelaySeconds.toFloat(),
                                onValueChange = { SettingsRepository.updateAutoBypassDelay(it.toInt()) },
                                valueRange = 1f..8f,
                                steps = 6,
                                colors = SliderDefaults.colors(
                                    thumbColor = DirectCyan,
                                    activeTrackColor = DirectCyan,
                                    inactiveTrackColor = Color.DarkGray
                                )
                            )
                        }
                    }
                }

                // 2.2 Chế độ vượt rào
                SettingsCard(title = if (isEn) "Region Unlock Mode" else "Chế độ vượt rào") {
                    listOf(
                        Triple(
                            VpnEngine.SMART_BYPASS,
                            if (isEn) "Smart Unlock 🇯🇵 (Recommended)" else "Mở khóa thông minh 🇯🇵 (Khuyên dùng)",
                            if (isEn) "Fastest direct region unlock without extra delay" else "Vào game trực tiếp cực nhanh, không tăng ping"
                        ),
                        Triple(
                            VpnEngine.PROXY_TUNNEL,
                            if (isEn) "Proxy Server 🌐" else "Máy chủ trung gian 🌐",
                            if (isEn) "Connects through regional servers with backup switching" else "Kết nối qua danh sách máy chủ Nhật/Hàn/Mỹ"
                        ),
                        Triple(
                            VpnEngine.EXTERNAL_VPN,
                            if (isEn) "External VPN App" else "Ứng dụng VPN ngoài",
                            if (isEn) "Use with your own separate VPN app" else "Dành cho người dùng bật app VPN riêng"
                        )
                    ).forEach { (engine, title, desc) ->
                        val isSelected = vpnState.engine == engine
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, if (isSelected) XboxNeonGreen else XboxCardBorder, RoundedCornerShape(10.dp))
                                .clickable { VpnManager.setEngine(engine) },
                            color = if (isSelected) XboxSurfaceVariant else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text(text = desc, color = TextSecondary, fontSize = 10.sp)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // 2.3 Bộ dịch & bộ lọc
                SettingsCard(title = if (isEn) "Translator & Filter" else "Bộ dịch & bộ lọc") {
                    SettingToggleRow(
                        title = if (isEn) "Auto-translate to Vietnamese" else "Tự động dịch sang Tiếng Việt",
                        subtitle = if (isEn) "Translates menus and descriptions on Xbox pages" else "Dịch menu, danh mục game và thông báo",
                        checked = config.xcloudTranslateEnabled,
                        onCheckedChange = { SettingsRepository.updateXcloudTranslate(it) },
                        switchTestTag = "settings_switch_xcloud_translate"
                    )

                    SettingToggleRow(
                        title = if (isEn) "Keep original game titles" else "Giữ nguyên tên game gốc",
                        subtitle = if (isEn) "Do not translate names like Fortnite, Forza, Halo" else "Không dịch tên riêng của game (Fortnite, Forza, Halo...)",
                        checked = config.preserveGameTitles,
                        onCheckedChange = { SettingsRepository.updatePreserveGameTitles(it) }
                    )

                    SettingToggleRow(
                        title = if (isEn) "Bilingual mode (VI - EN)" else "Chế độ song ngữ (Việt - Anh)",
                        subtitle = if (isEn) "Shows original English in parentheses" else "Hiện kèm từ tiếng Anh gốc trong ngoặc",
                        checked = config.bilingualMode,
                        onCheckedChange = { SettingsRepository.updateBilingualMode(it) }
                    )

                    SettingToggleRow(
                        title = if (isEn) "Hide stream stats overlay" else "Ẩn bảng thông số đè màn hình",
                        subtitle = if (isEn) "Hides technical stats box during gameplay" else "Tự ẩn bảng thông số che tầm nhìn khi đang chơi",
                        checked = config.hideStreamStatsOverlay,
                        onCheckedChange = { SettingsRepository.updateHideStreamStatsOverlay(it) },
                        activeColor = DirectCyan,
                        switchTestTag = "settings_switch_hide_decode_profile"
                    )

                    SettingToggleRow(
                        title = if (isEn) "Extended gaming dictionary" else "Từ điển thuật ngữ game mở rộng",
                        subtitle = if (isEn) "Translates common gaming terms accurately" else "Dịch chuẩn các từ ngữ thông dụng trong game",
                        checked = config.githubDictionaryEnabled,
                        onCheckedChange = { SettingsRepository.updateGithubDictionaryEnabled(it) }
                    )
                }

                // 2.4 Ngôn ngữ trang Xbox
                SettingsCard(title = if (isEn) "Xbox Page Language" else "Ngôn ngữ trang Xbox") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                1.dp,
                                if (config.useSystemLanguage) XboxNeonGreen else XboxCardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { SettingsRepository.updateUseSystemLanguage(true) },
                        color = if (config.useSystemLanguage) XboxSurfaceVariant else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isEn) "Follow Device Language ($resolvedSystemLocale)" else "Theo ngôn ngữ máy ($resolvedSystemLocale)",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = if (isEn) "Defaults to English (en-US) for easy reading" else "Tự chọn tiếng Anh (en-US) để không bị tiếng Nhật",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            if (config.useSystemLanguage) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        POPULAR_XBOX_LANGUAGES.take(6).forEach { lang ->
                            val isSelected = !config.useSystemLanguage && config.customLocale.equals(lang.code, ignoreCase = true)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) XboxNeonGreen else XboxCardBorder.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { SettingsRepository.updateCustomLocale(lang.code) },
                                color = if (isSelected) XboxSurfaceVariant else Color.Black.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(text = lang.flag, fontSize = 15.sp)
                                        Text(
                                            text = "${lang.localizedName(isEn)} (${lang.code})",
                                            color = TextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customInputText,
                        onValueChange = { customInputText = it },
                        label = { Text(if (isEn) "Custom code or Xbox link" else "Mã ngôn ngữ hoặc link Xbox") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = XboxNeonGreen,
                            unfocusedBorderColor = XboxCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_language_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { SettingsRepository.updateCustomLocale(customInputText) },
                            colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isEn) "Apply" else "Áp dụng", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                customInputText = "xbox.com/en-US"
                                SettingsRepository.updateCustomLocale("en-US")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isEn) "Set en-US 🇺🇸" else "Đặt en-US 🇺🇸", color = DirectCyan, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // NHÓM 3: ĐIỀU KHIỂN (Mặc định gập)
        // =========================================================================
        item {
            CollapsibleSettingsGroup(
                title = if (isEn) "Controls" else "Điều khiển",
                subtitle = if (config.showVirtualController) {
                    if (isEn) "Touch Gamepad ON" else "Tay cầm cảm ứng: Bật"
                } else {
                    if (isEn) "Touch Gamepad OFF" else "Tay cầm cảm ứng: Tắt"
                },
                icon = Icons.Default.Gamepad,
                expanded = expandControls,
                onToggle = { expandControls = !expandControls },
                testTag = "settings_group_controls"
            ) {
                SettingsCard(title = if (isEn) "Touch Gamepad System" else "Hệ thống tay cầm cảm ứng") {
                    SettingToggleRow(
                        title = if (isEn) "On-Screen Touch Controller" else "Tay cầm cảm ứng trên màn hình",
                        subtitle = if (isEn) "Virtual buttons, thumbsticks, gyro & drag layout" else "Cần xoay, cụm nút bấm, ngắm nghiêng máy & kéo thả vị trí",
                        checked = config.showVirtualController,
                        onCheckedChange = { SettingsRepository.updateShowVirtualController(it) },
                        switchTestTag = "settings_switch_virtual_controller"
                    )

                    Text(
                        text = if (isEn) "Layout Preset" else "Bố cục nút bấm",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        GamepadLayoutPreset.entries.forEach { preset ->
                            val isSelected = config.gamepadLayoutPreset == preset
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) XboxNeonGreen else XboxCardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { SettingsRepository.updateGamepadLayoutPreset(preset) },
                                color = if (isSelected) XboxSurfaceVariant else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.localizedTitle(isEn),
                                            color = if (isSelected) XboxNeonGreen else TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = preset.localizedDesc(isEn),
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    // 4 Thanh trượt: Kích thước, Độ mờ, Độ nhạy, Vùng chết
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LabeledSlider(
                            label = if (isEn) "Button Size" else "Kích thước nút",
                            valueText = "${(config.gamepadScale * 100).toInt()}%",
                            value = config.gamepadScale,
                            onValueChange = { SettingsRepository.updateGamepadScale(it) },
                            valueRange = 0.75f..1.35f,
                            accentColor = XboxNeonGreen
                        )
                        LabeledSlider(
                            label = if (isEn) "Button Opacity" else "Độ đậm mờ",
                            valueText = "${(config.gamepadOpacity * 100).toInt()}%",
                            value = config.gamepadOpacity,
                            onValueChange = { SettingsRepository.updateGamepadOpacity(it) },
                            valueRange = 0.25f..0.95f,
                            accentColor = DirectCyan
                        )
                        LabeledSlider(
                            label = if (isEn) "Stick Sensitivity" else "Độ nhạy cần xoay",
                            valueText = "${(config.gamepadSensitivity * 100).toInt()}%",
                            value = config.gamepadSensitivity,
                            onValueChange = { SettingsRepository.updateGamepadSensitivity(it) },
                            valueRange = 0.60f..1.60f,
                            accentColor = XboxNeonGreen
                        )
                        LabeledSlider(
                            label = if (isEn) "Stick Deadzone" else "Vùng chết tâm cần",
                            valueText = "${(config.gamepadDeadzone * 100).toInt()}%",
                            value = config.gamepadDeadzone,
                            onValueChange = { SettingsRepository.updateGamepadDeadzone(it) },
                            valueRange = 0.02f..0.25f,
                            accentColor = DirectCyan
                        )
                    }

                    Text(
                        text = if (isEn) "Vibration Strength" else "Độ rung khi bấm nút",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GamepadHapticLevel.entries.forEach { level ->
                            val selected = config.gamepadHapticLevel == level
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { SettingsRepository.updateGamepadHapticLevel(level) },
                                color = if (selected) XboxNeonGreen.copy(alpha = 0.22f) else Color(0xFF0E1813),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selected) XboxNeonGreen else XboxCardBorder
                                )
                            ) {
                                Text(
                                    text = level.localizedTitle(isEn),
                                    color = if (selected) XboxNeonGreen else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 7.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    SettingToggleRow(
                        title = if (isEn) "🎯 Motion Aiming (Gyro)" else "🎯 Ngắm bằng nghiêng máy (Gyro)",
                        subtitle = if (isEn) "Tilt phone to fine-tune right stick aim" else "Nghiêng điện thoại để chỉnh tâm ngắm cần phải",
                        checked = config.gamepadGyroEnabled,
                        onCheckedChange = { SettingsRepository.updateGamepadGyroEnabled(it) },
                        activeColor = DirectCyan
                    )

                    if (config.gamepadGyroEnabled) {
                        LabeledSlider(
                            label = if (isEn) "Gyro Sensitivity" else "Độ nhạy nghiêng máy",
                            valueText = "${(config.gamepadGyroSensitivity * 100).toInt()}%",
                            value = config.gamepadGyroSensitivity,
                            onValueChange = { SettingsRepository.updateGamepadGyroSensitivity(it) },
                            valueRange = 0.40f..2.50f,
                            accentColor = DirectCyan
                        )
                    }

                    SettingToggleRow(
                        title = if (isEn) "🏃 Push Left Stick to Auto-Sprint" else "🏃 Đẩy kịch cần trái để tự chạy nhanh",
                        subtitle = if (isEn) "Pushing left stick fully forward triggers sprint" else "Đẩy cần di chuyển hết cỡ về trước sẽ tự chạy nhanh",
                        checked = config.gamepadSprintLockEnabled,
                        onCheckedChange = { SettingsRepository.updateGamepadSprintLockEnabled(it) }
                    )

                    if (config.gamepadCustomOffsets.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                SettingsRepository.resetGamepadCustomOffsets()
                                Toast.makeText(
                                    context,
                                    if (isEn) "Reset button positions to default" else "Đã đặt lại vị trí nút về mặc định",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (isEn) "Reset Button Positions" else "Đặt lại vị trí nút về mặc định",
                                color = DirectCyan,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // NHÓM 4: NÂNG CAO (Đầy đủ thông số Video, Tỉ lệ màn hình, Luồng Stream, Ngôn ngữ & Bộ nhớ đệm)
        // =========================================================================
        item {
            CollapsibleSettingsGroup(
                title = if (isEn) "Advanced & Color Calibration" else "Nâng cao, Màu sắc Hình ảnh & Ngôn ngữ",
                subtitle = if (isEn) "Screen Ratio, Color Sliders, Language & Cache" else "Tỉ lệ màn hình khi chơi, Độ tươi màu, Độ sáng, Ngôn ngữ & Bộ nhớ đệm",
                icon = Icons.Default.Tune,
                expanded = expandAdvanced,
                onToggle = { expandAdvanced = !expandAdvanced },
                testTag = "settings_group_advanced"
            ) {
                // 4.0 Cân chỉnh Màu sắc, Độ sáng & Khung hình hiển thị
                SettingsCard(
                    title = if (isEn) "🎬 Screen Ratio & Image Color Adjustment" else "🎬 Chỉnh Màu sắc Hình ảnh & Tỉ lệ Màn hình khi chơi"
                ) {
                    Text(
                        text = if (isEn) {
                            "Adjust how the game fits your phone screen while playing, customize width/height, and fine-tune contrast, color saturation, and brightness."
                        } else {
                            "Chọn cách khung hình game hiển thị trên màn hình điện thoại của bạn, tự chỉnh độ rộng Ngang/Dọc theo ý thích, hoặc tăng độ tươi màu và độ sáng để nhìn rõ chi tiết trong tối."
                        },
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Text(
                        text = if (isEn) "1. In-Game Screen Ratio" else "1. Tỉ lệ màn hình lúc đang chơi game",
                        color = DirectCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        com.example.data.VideoAspectRatioMode.entries.forEach { aspectMode ->
                            val isSelected = config.videoAspectRatioMode == aspectMode
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) DirectCyan else XboxCardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { SettingsRepository.updateVideoAspectRatioMode(aspectMode) }
                                    .testTag("advanced_video_aspect_${aspectMode.id}"),
                                color = if (isSelected) Color(0xFF0F2327) else Color(0xFF0E1712)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = aspectMode.localizedTitle(isEn),
                                            color = if (isSelected) DirectCyan else TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = aspectMode.localizedDesc(isEn),
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = DirectCyan, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Hộp thanh trượt tinh chỉnh thông số Ngang/Dọc & Màu sắc chi tiết
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LabeledSlider(
                            label = if (isEn) "Horizontal Width" else "Độ rộng chiều Ngang",
                            valueText = "${(config.videoScaleX * 100).toInt()}%",
                            value = config.videoScaleX,
                            onValueChange = { SettingsRepository.updateVideoScaleX(it) },
                            valueRange = 0.70f..1.40f,
                            accentColor = XboxNeonGreen
                        )
                        LabeledSlider(
                            label = if (isEn) "Vertical Height" else "Độ cao chiều Dọc",
                            valueText = "${(config.videoScaleY * 100).toInt()}%",
                            value = config.videoScaleY,
                            onValueChange = { SettingsRepository.updateVideoScaleY(it) },
                            valueRange = 0.70f..1.40f,
                            accentColor = DirectCyan
                        )
                        LabeledSlider(
                            label = if (isEn) "Image Contrast" else "Độ tương phản hình ảnh",
                            valueText = "${config.videoContrast}%",
                            value = config.videoContrast.toFloat(),
                            onValueChange = { SettingsRepository.updateVideoContrast(it.toInt()) },
                            valueRange = 80f..140f,
                            accentColor = XboxNeonGreen
                        )
                        LabeledSlider(
                            label = if (isEn) "Color Vividity" else "Độ tươi màu",
                            valueText = "${config.videoSaturation}%",
                            value = config.videoSaturation.toFloat(),
                            onValueChange = { SettingsRepository.updateVideoSaturation(it.toInt()) },
                            valueRange = 80f..150f,
                            accentColor = DirectCyan
                        )
                        LabeledSlider(
                            label = if (isEn) "Screen Brightness" else "Độ sáng hình ảnh",
                            valueText = "${config.videoBrightness}%",
                            value = config.videoBrightness.toFloat(),
                            onValueChange = { SettingsRepository.updateVideoBrightness(it.toInt()) },
                            valueRange = 80f..130f,
                            accentColor = XboxNeonGreen
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            SettingsRepository.resetVideoDisplayDefaults()
                            Toast.makeText(
                                context,
                                if (isEn) "Reset screen ratio & colors to default" else "Đã đặt lại tỉ lệ màn hình và màu sắc về mặc định",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("advanced_reset_video_defaults_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isEn) "Reset Screen Ratio & Colors to Default" else "Đặt lại Tỉ lệ màn hình & Màu sắc về Mặc định",
                            color = DirectCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0C1A14),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, DirectCyan.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = if (isEn) "ℹ️ Clear Guide on Screen Ratio & Video Recording" else "ℹ️ Giải thích rõ về Tỉ lệ màn hình & Quay video (.MP4)",
                                color = DirectCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isEn) {
                                    "• Full-Screen Recording (Default): Whatever screen ratio you play on (full phone screen 20:9 or 16:9), the recorded .MP4 video keeps that exact same screen ratio with stereo game audio.\n• In-Game Screen Ratio: Lets you play in standard 16:9, full-screen stretch, or custom width/height without ever resizing mid-game.\n• Saved Location: All recorded clips are saved directly to Gallery / Movies / CloudPlayXbox as .MP4 files."
                                } else {
                                    "• Quay Full màn hình (Mặc định): Bạn đang chơi trên màn hình điện thoại như thế nào (tràn viền 20:9 hay 16:9), video .MP4 xuất ra sẽ giữ nguyên đúng tỉ lệ màn hình đó và có đầy đủ tiếng game.\n• Không tự đổi tỉ lệ khi đang chơi: Tuyệt đối giữ cố định khung hình trong suốt trận đấu, không tự co giãn gây giật hình.\n• Nơi lưu video: Mọi video quay xong đều lưu chuẩn đuôi .MP4 vào Bộ sưu tập ảnh (Thư mục Movies/CloudPlayXbox)."
                                },
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // 4.1 Ngôn ngữ ứng dụng
                SettingsCard(title = if (isEn) "App Language" else "Ngôn ngữ ứng dụng") {
                    listOf(
                        Triple(AppLanguage.SYSTEM, "🌐 " + stringResource(R.string.lang_system_default), "app_lang_system_button"),
                        Triple(AppLanguage.VIETNAMESE, "🇻🇳 " + stringResource(R.string.lang_vietnamese), "app_lang_vi_button"),
                        Triple(AppLanguage.ENGLISH, "🇺🇸 " + stringResource(R.string.lang_english), "app_lang_en_button")
                    ).forEach { (lang, label, tag) ->
                        val isSelected = config.appLanguage == lang
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, if (isSelected) XboxNeonGreen else XboxCardBorder, RoundedCornerShape(10.dp))
                                .clickable { SettingsRepository.updateAppLanguage(lang) }
                                .testTag(tag),
                            color = if (isSelected) XboxSurfaceVariant else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // 4.2 Bộ nhớ đệm & tài khoản
                SettingsCard(title = if (isEn) "Cache & Account" else "Bộ nhớ đệm & tài khoản") {
                    Button(
                        onClick = onClearCookies,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = XboxSurfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (isEn) "Clear Cookies & Re-login to Xbox" else "Xóa Cookie & Đăng nhập lại Xbox",
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // 4.3 Điều khoản, bảo mật & bản quyền
                SettingsCard(title = if (isEn) "Terms, Security & License" else "Điều khoản, bảo mật & bản quyền") {
                    Text(
                        text = if (isEn) {
                            "Independent app • Fair Play (no macro/cheats) • 100% local data • Open-source MIT License (Better-xCloud)."
                        } else {
                            "Ứng dụng độc lập • Chơi công bằng (không macro/gian lận) • Dữ liệu lưu cục bộ 100% • Giấy phép nguồn mở MIT (Better-xCloud)."
                        },
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Button(
                        onClick = { showTermsDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = XboxSurfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = DirectCyan, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (isEn) "View Terms, Security & MIT License" else "Xem điều khoản, bảo mật & giấy phép MIT",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // NHÓM 5: TÍNH NĂNG PHỤ: QUAY VIDEO MÀN HÌNH & LƯU CLIP (Mặc định gập)
        // =========================================================================
        item {
            CollapsibleSettingsGroup(
                title = if (isEn) "Screen Recording (Secondary Feature)" else "Quay video màn hình (Tính năng phụ)",
                subtitle = if (isEn) "Capture engine, video quality, storage location" else "Cơ chế quay luồng stream & chất lượng video",
                icon = Icons.Default.Videocam,
                expanded = expandRecording,
                onToggle = { expandRecording = !expandRecording },
                testTag = "settings_group_recording"
            ) {
                SettingsCard(title = if (isEn) "Video Capture Engine (Direct Stream vs Hardware)" else "Cơ chế quay video (Lấy luồng Stream vs Phần cứng)") {
                    Text(
                        text = if (isEn) {
                            "Secondary utility for recording gameplay clips to Gallery (Movies/XboxCloudPlay). Default: Full Screen device recording via hardware encoder. Optional: Direct WebRTC stream clone for 16:9 game-only capture."
                        } else {
                            "Tính năng phụ hỗ trợ lưu clip vào Thư viện (Movies/XboxCloudPlay): Mặc định quay Full Toàn Màn Hình thiết bị bằng bộ mã hóa phần cứng. Tùy chọn lấy luồng WebRTC trực tiếp dành cho người dùng muốn chỉ thu khung hình game 16:9 gốc."
                        },
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    com.example.data.RecordingCaptureEngine.entries.forEach { engine ->
                        val isSelected = config.recordingCaptureEngine == engine
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    1.dp,
                                    if (isSelected) XboxNeonGreen else XboxCardBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { SettingsRepository.updateRecordingCaptureEngine(engine) }
                                .testTag("settings_capture_engine_${engine.id}"),
                            color = if (isSelected) Color(0xFF13241A) else Color(0xFF0F1813)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = engine.localizedTitle(isEn),
                                        color = if (isSelected) XboxNeonGreen else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = engine.localizedDesc(isEn),
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.data.ScreenRecordQuality.entries.forEach { recQuality ->
                            val isSelected = config.screenRecordQuality == recQuality
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) DirectCyan else XboxCardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { SettingsRepository.updateScreenRecordQuality(recQuality) }
                                    .testTag("settings_record_quality_${recQuality.id}"),
                                color = if (isSelected) Color(0xFF0F2327) else Color(0xFF0F1813)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = recQuality.localizedTitle(isEn),
                                        color = if (isSelected) DirectCyan else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = recQuality.localizedDesc(isEn),
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }

                    SettingToggleRow(
                        title = if (isEn) "Auto-Reduce GPU Load While Recording" else "Tự giảm tải đồ họa khi đang quay",
                        subtitle = if (isEn) {
                            "Automatically enables lightweight GPU mode during screen recording to keep frame rate locked"
                        } else {
                            "Tự động bật chế độ nhẹ tải GPU trong lúc quay video để giữ khung hình luôn mượt"
                        },
                        checked = config.autoReduceLoadWhenRecording,
                        onCheckedChange = { SettingsRepository.updateAutoReduceLoadWhenRecording(it) },
                        activeColor = XboxNeonGreen
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0C1A14),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxNeonGreen.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = if (isEn) "ℹ️ Does Direct Stream Capture have any side effects?" else "ℹ️ Lấy thẳng luồng Video Streaming có tác dụng phụ không?",
                                color = XboxNeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isEn) {
                                    "• 0% extra 4G/Wi-Fi data: Clones the already-received WebRTC track in RAM.\n• 0% GPU screen-capture lag: Does not screenshot the UI or stretch 20:9 screens.\n• Note: Only records the pure 16:9 game video + audio (floating HUD buttons are not drawn onto the video).\n• Storage folder: Internal Storage > Movies > XboxCloudPlay"
                                } else {
                                    "• Không tốn thêm 4G/Wi-Fi: Nhân bản luồng WebRTC (Track Clone) đã tải sẵn trong bộ đệm.\n• Không gây tụt FPS & Không bị bóp hình: Không chụp lại màn hình điện thoại nên giữ nguyên khung hình 16:9 chuẩn gốc của Xbox.\n• Lưu ý nhỏ duy nhất: File video thu được là hình ảnh + âm thanh nguyên bản của game (không dính các nút tay cầm ảo hay nút HUD nổi trên màn hình).\n• Vị trí lưu clip: Bộ nhớ trong > Thư mục Movies > XboxCloudPlay"
                                },
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showTermsDialog) {
        com.example.ui.components.UserTermsAgreementDialog(
            onAccept = { showTermsDialog = false },
            onDecline = { showTermsDialog = false },
            isDismissible = true,
            onDismissRequest = { showTermsDialog = false }
        )
    }
}

@Composable
private fun CollapsibleSettingsGroup(
    title: String,
    subtitle: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(
                    1.dp,
                    if (expanded) XboxNeonGreen.copy(alpha = 0.55f) else XboxCardBorder,
                    RoundedCornerShape(16.dp)
                )
                .clickable { onToggle() }
                .testTag(testTag),
            color = XboxSurface.copy(alpha = 0.96f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (expanded) XboxNeonGreen.copy(alpha = 0.18f) else XboxSurfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (expanded) XboxNeonGreen else DirectCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            color = TextPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = subtitle,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = if (expanded) XboxNeonGreen else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color = XboxNeonGreen,
    switchTestTag: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 10.5.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = activeColor),
            modifier = if (switchTestTag != null) Modifier.testTag(switchTestTag) else Modifier
        )
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, color = TextSecondary, fontSize = 11.sp)
            Text(
                text = valueText,
                color = accentColor,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, XboxCardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = XboxSurface.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 15.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Brush.verticalGradient(listOf(XboxNeonGreen, DirectCyan)))
                )
                Text(
                    text = title,
                    color = XboxNeonGreen,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            content()
        }
    }
}
