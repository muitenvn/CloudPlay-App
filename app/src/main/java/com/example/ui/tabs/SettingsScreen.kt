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
        // NHÓM 1: ĐỒ HỌA & KHUNG HÌNH (FPS) (Mặc định mở)
        // =========================================================================
        item {
            CollapsibleSettingsGroup(
                title = if (isEn) "Graphics & Frame Rate" else "Đồ họa & Khung hình (FPS)",
                subtitle = "${config.cellularMode.localizedTitle(isEn)} • ${config.streamFpsCap.fps} FPS",
                icon = Icons.Default.Speed,
                expanded = expandGraphics,
                onToggle = { expandGraphics = !expandGraphics },
                testTag = "settings_group_graphics"
            ) {
                // 1.1 Mức đồ họa & Tốc độ khung hình (FPS)
                SettingsCard(title = if (isEn) "Graphics Quality & FPS" else "Chất lượng đồ họa & FPS") {
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
                                        text = if (isEn) "Auto-adjusts to network & keeps device cool" else "Tự chỉnh theo mạng, giữ máy mát",
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

                    Text(
                        text = if (isEn) "Frame Rate (FPS)" else "Nhịp khung hình (FPS)",
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
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // 1.2 Độ phân giải dựng hình & Giảm tải phần cứng
                SettingsCard(title = if (isEn) "Render Scale & Hardware Offload" else "Độ phân giải dựng hình & Giảm tải máy") {
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
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SettingToggleRow(
                        title = if (isEn) "Prioritize Smooth Input" else "Ưu tiên phản hồi mượt",
                        subtitle = if (isEn) "Reduces delay on unstable connections" else "Giảm trễ thao tác khi sóng yếu",
                        checked = config.wifiSpeedFirstPriority,
                        onCheckedChange = { SettingsRepository.updateWifiSpeedFirstPriority(it) },
                        activeColor = DirectCyan
                    )

                    SettingToggleRow(
                        title = if (isEn) "Reduce GPU Load" else "Giảm tải GPU",
                        subtitle = if (isEn) "Disables heavy web blur & shadows" else "Tắt hiệu ứng nền nặng để chống giật",
                        checked = config.gpuLoadReductionEnabled,
                        onCheckedChange = { SettingsRepository.updateGpuLoadReductionEnabled(it) },
                        activeColor = DirectCyan
                    )

                    SettingToggleRow(
                        title = if (isEn) "Low-End Device Mode" else "Chế độ máy yếu",
                        subtitle = if (isEn) "5 Mbps light stream, cool 60 FPS" else "Đồ họa nhẹ 5 Mbps, giữ máy mát",
                        checked = config.lowEndDeviceMode,
                        onCheckedChange = { SettingsRepository.updateLowEndDeviceMode(it) },
                        switchTestTag = "settings_low_end_device_mode_switch"
                    )
                }

                // 1.3 Độ nét & Màu sắc hình ảnh
                SettingsCard(title = if (isEn) "Sharpness & Image Colors" else "Độ nét & Màu sắc hình ảnh") {
                    SettingToggleRow(
                        title = if (isEn) "1080p Full HD Stream" else "Độ phân giải 1080p Full HD",
                        subtitle = if (isEn) "Requests sharp 1080p stream" else "Ưu tiên luồng 1080p sắc nét",
                        checked = config.forceDesktopUserAgent,
                        onCheckedChange = { SettingsRepository.updateDesktopUserAgent(it) }
                    )

                    SettingToggleRow(
                        title = if (isEn) "Clarity Boost" else "Làm nét hình ảnh (Clarity Boost)",
                        subtitle = if (isEn) "Sharpens edges and details" else "Tăng độ nét chi tiết trong game",
                        checked = config.clarityBoostEnabled,
                        onCheckedChange = { SettingsRepository.updateClarityBoost(it) }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LabeledSlider(
                            label = if (isEn) "Contrast" else "Độ tương phản",
                            valueText = "${config.videoContrast}%",
                            value = config.videoContrast.toFloat(),
                            onValueChange = { SettingsRepository.updateVideoContrast(it.toInt()) },
                            valueRange = 80f..140f,
                            accentColor = XboxNeonGreen
                        )
                        LabeledSlider(
                            label = if (isEn) "Color Saturation" else "Độ tươi màu",
                            valueText = "${config.videoSaturation}%",
                            value = config.videoSaturation.toFloat(),
                            onValueChange = { SettingsRepository.updateVideoSaturation(it.toInt()) },
                            valueRange = 80f..150f,
                            accentColor = DirectCyan
                        )
                        LabeledSlider(
                            label = if (isEn) "Brightness" else "Độ sáng",
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
                                if (isEn) "Reset colors to default" else "Đã đặt lại màu sắc mặc định",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("advanced_reset_video_defaults_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isEn) "Reset Colors to Default" else "Đặt lại màu sắc mặc định",
                            color = DirectCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // =========================================================================
        // NHÓM 2: MẠNG & MỞ KHÓA VÙNG (Mặc định gập)
        // =========================================================================
        item {
            CollapsibleSettingsGroup(
                title = if (isEn) "Network & Region Unlock" else "Mạng & Mở khóa vùng",
                subtitle = if (isEn) "Smart VPN, Low Ping Bypass" else "VPN thông minh, vào game không lag",
                icon = Icons.Default.Public,
                expanded = expandNetwork,
                onToggle = { expandNetwork = !expandNetwork },
                testTag = "settings_group_network"
            ) {
                // 2.1 VPN thông minh
                SettingsCard(title = if (isEn) "Smart VPN" else "VPN thông minh") {
                    SettingToggleRow(
                        title = if (isEn) "Smart VPN (Auto On/Off)" else "VPN thông minh (Tự bật/tắt)",
                        subtitle = if (isEn) "Auto connects to open game, disconnects inside" else "Tự bật khi mở game, tự tắt khi vào trận",
                        checked = config.smartVpnEnabled,
                        onCheckedChange = { SettingsRepository.updateSmartVpnEnabled(it) },
                        switchTestTag = "settings_switch_smart_vpn"
                    )

                    SettingToggleRow(
                        title = if (isEn) "Auto disconnect VPN in game" else "Tự tắt VPN khi vào game",
                        subtitle = if (isEn) "Uses direct network for lowest ping" else "Dùng mạng trực tiếp để giảm ping",
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
                                    text = if (isEn) "VPN disconnect delay" else "Thời gian chờ tắt VPN",
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

                // 2.2 Chế độ mở khóa vùng
                SettingsCard(title = if (isEn) "Region Unlock Mode" else "Chế độ mở khóa vùng") {
                    listOf(
                        Triple(
                            VpnEngine.SMART_BYPASS,
                            if (isEn) "Smart Unlock 🇯🇵 (Recommended)" else "Mở khóa thông minh 🇯🇵 (Khuyên dùng)",
                            if (isEn) "Direct unlock without extra ping" else "Vào thẳng cực nhanh, không tăng ping"
                        ),
                        Triple(
                            VpnEngine.PROXY_TUNNEL,
                            if (isEn) "Proxy Server 🌐" else "Máy chủ trung gian 🌐",
                            if (isEn) "Routes via JP/KR/US servers" else "Kết nối qua máy chủ Nhật / Hàn / Mỹ"
                        ),
                        Triple(
                            VpnEngine.EXTERNAL_VPN,
                            if (isEn) "External VPN App" else "Ứng dụng VPN ngoài",
                            if (isEn) "Use with your own VPN app" else "Dùng kèm app VPN riêng của bạn"
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
            }
        }

        // =========================================================================
        // NHÓM 3: TAY CẦM & CẢM ỨNG (Mặc định gập)
        // =========================================================================
        item {
            CollapsibleSettingsGroup(
                title = if (isEn) "Gamepad & Touch Controls" else "Tay cầm & Cảm ứng",
                subtitle = if (config.showVirtualController) {
                    if (isEn) "Virtual Gamepad: ON" else "Tay cầm ảo: Đang bật"
                } else {
                    if (isEn) "Virtual Gamepad: OFF" else "Tay cầm ảo: Đang tắt"
                },
                icon = Icons.Default.Gamepad,
                expanded = expandControls,
                onToggle = { expandControls = !expandControls },
                testTag = "settings_group_controls"
            ) {
                SettingsCard(title = if (isEn) "Touch Gamepad" else "Tay cầm ảo trên màn hình") {
                    SettingToggleRow(
                        title = if (isEn) "Show Virtual Gamepad" else "Bật tay cầm ảo",
                        subtitle = if (isEn) "On-screen sticks, triggers & custom drag layout" else "Cần xoay, nút bấm và kéo thả vị trí tự do",
                        checked = config.showVirtualController,
                        onCheckedChange = { SettingsRepository.updateShowVirtualController(it) },
                        switchTestTag = "settings_switch_virtual_controller"
                    )

                    Text(
                        text = if (isEn) "Button Layout" else "Bố cục nút bấm",
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
                            label = if (isEn) "Opacity" else "Độ đậm mờ",
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
                            label = if (isEn) "Deadzone" else "Vùng chết tâm cần",
                            valueText = "${(config.gamepadDeadzone * 100).toInt()}%",
                            value = config.gamepadDeadzone,
                            onValueChange = { SettingsRepository.updateGamepadDeadzone(it) },
                            valueRange = 0.02f..0.25f,
                            accentColor = DirectCyan
                        )
                    }

                    Text(
                        text = if (isEn) "Vibration" else "Độ rung khi bấm",
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
                        title = if (isEn) "🎯 Gyro Aiming" else "🎯 Ngắm nghiêng máy (Gyro)",
                        subtitle = if (isEn) "Tilt phone to aim right stick" else "Nghiêng máy để chỉnh tâm ngắm",
                        checked = config.gamepadGyroEnabled,
                        onCheckedChange = { SettingsRepository.updateGamepadGyroEnabled(it) },
                        activeColor = DirectCyan
                    )

                    if (config.gamepadGyroEnabled) {
                        LabeledSlider(
                            label = if (isEn) "Gyro Sensitivity" else "Độ nhạy Gyro",
                            valueText = "${(config.gamepadGyroSensitivity * 100).toInt()}%",
                            value = config.gamepadGyroSensitivity,
                            onValueChange = { SettingsRepository.updateGamepadGyroSensitivity(it) },
                            valueRange = 0.40f..2.50f,
                            accentColor = DirectCyan
                        )
                    }

                    SettingToggleRow(
                        title = if (isEn) "🏃 Auto-Sprint" else "🏃 Đẩy kịch cần trái để chạy nhanh",
                        subtitle = if (isEn) "Push left stick forward to sprint" else "Tự kích hoạt chạy nhanh khi đẩy hết cỡ",
                        checked = config.gamepadSprintLockEnabled,
                        onCheckedChange = { SettingsRepository.updateGamepadSprintLockEnabled(it) }
                    )

                    if (config.gamepadCustomOffsets.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                SettingsRepository.resetGamepadCustomOffsets()
                                Toast.makeText(
                                    context,
                                    if (isEn) "Reset button positions" else "Đã đặt lại vị trí nút",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (isEn) "Reset Button Positions" else "Đặt lại vị trí nút mặc định",
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
        // NHÓM 4: DỊCH TIẾNG VIỆT & HỆ THỐNG (Mặc định gập)
        // =========================================================================
        item {
            CollapsibleSettingsGroup(
                title = if (isEn) "Translation, Language & System" else "Dịch Tiếng Việt & Hệ thống",
                subtitle = if (isEn) "Translator, App Language, Account" else "Bộ dịch, Ngôn ngữ, Tài khoản & Bản quyền",
                icon = Icons.Default.Tune,
                expanded = expandAdvanced,
                onToggle = { expandAdvanced = !expandAdvanced },
                testTag = "settings_group_advanced"
            ) {
                // 4.1 Bộ dịch & hiển thị
                SettingsCard(title = if (isEn) "Vietnamese Translator & HUD" else "Bộ dịch Tiếng Việt & Màn hình") {
                    SettingToggleRow(
                        title = if (isEn) "Auto-translate to Vietnamese" else "Tự động dịch Tiếng Việt",
                        subtitle = if (isEn) "Translates Xbox menus and notifications" else "Dịch giao diện và thông báo trên trang Xbox",
                        checked = config.xcloudTranslateEnabled,
                        onCheckedChange = { SettingsRepository.updateXcloudTranslate(it) },
                        switchTestTag = "settings_switch_xcloud_translate"
                    )

                    SettingToggleRow(
                        title = if (isEn) "Keep original game titles" else "Giữ nguyên tên game gốc",
                        subtitle = if (isEn) "Do not translate Fortnite, Forza, Halo..." else "Không dịch tên riêng (Fortnite, Forza, Halo...)",
                        checked = config.preserveGameTitles,
                        onCheckedChange = { SettingsRepository.updatePreserveGameTitles(it) }
                    )

                    SettingToggleRow(
                        title = if (isEn) "Bilingual mode (VI - EN)" else "Chế độ song ngữ (Việt - Anh)",
                        subtitle = if (isEn) "Shows original English in parentheses" else "Hiện kèm tiếng Anh gốc trong ngoặc",
                        checked = config.bilingualMode,
                        onCheckedChange = { SettingsRepository.updateBilingualMode(it) }
                    )

                    SettingToggleRow(
                        title = if (isEn) "Hide technical stats overlay" else "Ẩn thông số kỹ thuật đè màn hình",
                        subtitle = if (isEn) "Keeps gameplay screen clean" else "Giữ màn hình chơi game thoáng sạch",
                        checked = config.hideStreamStatsOverlay,
                        onCheckedChange = { SettingsRepository.updateHideStreamStatsOverlay(it) },
                        activeColor = DirectCyan,
                        switchTestTag = "settings_switch_hide_decode_profile"
                    )
                }

                // 4.2 Ngôn ngữ ứng dụng
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

                // 4.3 Ngôn ngữ vùng trang Xbox
                SettingsCard(title = if (isEn) "Xbox Store Region" else "Vùng ngôn ngữ trang Xbox") {
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
                                    text = if (isEn) "Auto Default ($resolvedSystemLocale)" else "Mặc định chuẩn ($resolvedSystemLocale)",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = if (isEn) "Uses English (en-US) to avoid Japanese text" else "Ưu tiên tiếng Anh (en-US) để tránh chữ Nhật",
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
                        POPULAR_XBOX_LANGUAGES.take(5).forEach { lang ->
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
                        label = { Text(if (isEn) "Custom locale or Xbox URL" else "Mã vùng hoặc link Xbox") },
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
                            Text(if (isEn) "Reset en-US 🇺🇸" else "Đặt en-US 🇺🇸", color = DirectCyan, fontSize = 11.sp)
                        }
                    }
                }

                // 4.4 Tài khoản & Bản quyền
                SettingsCard(title = if (isEn) "Account & License" else "Tài khoản & Bản quyền") {
                    Button(
                        onClick = onClearCookies,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = XboxSurfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (isEn) "Clear Cookies & Re-login" else "Xóa phiên & Đăng nhập lại Xbox",
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Button(
                        onClick = { showTermsDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = XboxSurfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = DirectCyan, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (isEn) "Terms & Open-Source License (MIT)" else "Điều khoản & Bản quyền nguồn mở (MIT)",
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
        // NHÓM 5: QUAY MÀN HÌNH (TÙY CHỌN PHỤ) (Mặc định gập)
        // =========================================================================
        item {
            CollapsibleSettingsGroup(
                title = if (isEn) "Screen Recording (Optional)" else "Quay màn hình (Tùy chọn phụ)",
                subtitle = config.screenRecordQuality.localizedShort(isEn),
                icon = Icons.Default.Videocam,
                expanded = expandRecording,
                onToggle = { expandRecording = !expandRecording },
                testTag = "settings_group_recording"
            ) {
                SettingsCard(title = if (isEn) "Recording Mode & Quality" else "Chế độ & Độ nét video (.MP4)") {
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
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    SettingToggleRow(
                        title = if (isEn) "Reduce GPU load while recording" else "Giảm tải GPU khi đang quay",
                        subtitle = if (isEn) "Keeps gameplay smooth while recording" else "Giữ khung hình ổn định trong lúc ghi hình",
                        checked = config.autoReduceLoadWhenRecording,
                        onCheckedChange = { SettingsRepository.updateAutoReduceLoadWhenRecording(it) },
                        activeColor = XboxNeonGreen
                    )
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
