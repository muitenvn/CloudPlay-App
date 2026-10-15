package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.data.POPULAR_XBOX_LANGUAGES
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.SettingsRepository
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant
import com.example.vpn.DEFAULT_SERVERS
import com.example.vpn.ServerLocation
import com.example.vpn.VpnManager

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onClearCookies: () -> Unit
) {
    val config by SettingsRepository.config.collectAsState()
    val vpnState by VpnManager.state.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, XboxCardBorder, RoundedCornerShape(24.dp))
                .testTag("settings_dialog"),
            color = XboxSurface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
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
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = XboxNeonGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Cài đặt & Tối ưu hóa",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: Xbox Cloud Language (CRITICAL for user request)
                Text(
                    text = "NGÔN NGỮ HIỂN THỊ XBOX CLOUD",
                    color = XboxNeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chọn tiếng Anh hoặc ngôn ngữ hệ thống thay vì bị ép đọc tiếng Nhật.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // System language toggle
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            if (config.useSystemLanguage) XboxNeonGreen else XboxCardBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { SettingsRepository.updateUseSystemLanguage(true) },
                    color = if (config.useSystemLanguage) XboxSurfaceVariant else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = if (config.useSystemLanguage) XboxNeonGreen else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Theo ngôn ngữ hệ thống (Tự động fallback en-US)",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = "Áp dụng: ${config.getEffectiveLocale()}",
                                color = DirectCyan,
                                fontSize = 10.sp
                            )
                        }
                        if (config.useSystemLanguage) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Language selection
                listOf(
                    Triple("en-US", "Tiếng Anh Mỹ (en-US) 🇺🇸", "English (US) - Khuyên dùng"),
                    Triple("en-GB", "Tiếng Anh Anh (en-GB) 🇬🇧", "English (UK)"),
                    Triple("ja-JP", "Tiếng Nhật (ja-JP) 🇯🇵", "日本語"),
                    Triple("ko-KR", "Tiếng Hàn (ko-KR) 🇰🇷", "한국어")
                ).forEach { (code, title, desc) ->
                    val isSelected = !config.useSystemLanguage && config.customLocale.equals(code, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                1.dp,
                                if (isSelected) XboxNeonGreen else XboxCardBorder.copy(alpha = 0.5f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { SettingsRepository.updateCustomLocale(code) },
                        color = if (isSelected) XboxSurfaceVariant else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text(text = desc, color = TextSecondary, fontSize = 10.sp)
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: Xbox Cloud Native Vietnamese Translator (Exclusive optimization)
                Text(
                    text = "BỘ DỊCH TIẾNG VIỆT XBOX CLOUD (TỐI ƯU ĐỘC QUYỀN)",
                    color = XboxNeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp)),
                    color = XboxSurfaceVariant.copy(alpha = 0.6f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Bộ Dịch Google Dịch Thông Minh (Smart xCloud Translator)",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Dịch trọn vẹn mọi mô tả, cốt truyện, thông báo và menu xCloud tự nhiên như Google Dịch. Hệ thống thông minh tự động né tên game bản quyền (Forza, Halo, Starfield,...), phím điều khiển (RT, LT, D-pad) và luồng video để chơi 60 FPS mượt mà 0% gián đoạn.",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Toggle 1: Auto translate
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dịch giao diện xCloud sang Tiếng Việt",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tự động dịch menu, danh mục, hàng chờ và hướng dẫn nút bấm",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = config.xcloudTranslateEnabled,
                        onCheckedChange = { SettingsRepository.updateXcloudTranslate(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = XboxNeonGreen
                        ),
                        modifier = Modifier.testTag("switch_xcloud_translate")
                    )
                }

                // Toggle 2: Preserve game titles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bảo vệ tên game gốc (Khuyên dùng)",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Giữ nguyên tên tựa game (Fortnite, Forza, Halo...) không dịch buồn cười",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = config.preserveGameTitles,
                        onCheckedChange = { SettingsRepository.updatePreserveGameTitles(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = XboxNeonGreen
                        )
                    )
                }

                // Toggle 3: Bilingual mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Chế độ song ngữ (Việt - Anh)",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Hiển thị bản dịch Tiếng Việt kèm từ gốc Tiếng Anh trong ngoặc",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = config.bilingualMode,
                        onCheckedChange = { SettingsRepository.updateBilingualMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = XboxNeonGreen
                        )
                    )
                }

                // Toggle 4: Hide Decode Profile / Stream Stats overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ẩn bảng Decode Profile / Stream Stats đè màn hình",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tự động ẩn bảng thông số kỹ thuật (Decode profile, FPS, Bitrate) che tầm nhìn khi chơi",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = config.hideStreamStatsOverlay,
                        onCheckedChange = { SettingsRepository.updateHideStreamStatsOverlay(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = DirectCyan
                        )
                    )
                }

                // Toggle 5: GitHub Better-xCloud Dictionary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Từ điển Gaming đầy đủ từ GitHub (550+ mục)",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Dịch trọn bộ menu, stream, tay cầm và tính năng Better-xCloud với tốc độ 0ms O(1)",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = config.githubDictionaryEnabled,
                        onCheckedChange = { SettingsRepository.updateGithubDictionaryEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = XboxNeonGreen
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: 4G & Wi-Fi Network Optimization
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SignalCellularAlt,
                        contentDescription = null,
                        tint = XboxNeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "TỐI ƯU MẠNG 4G & WIFI (TỐC ĐỘ HÀNG ĐẦU)",
                        color = XboxNeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tối ưu hóa đường truyền cho tất cả các loại mạng 4G/LTE/5G (Viettel, Vina, Mobi...) và tự động tinh chỉnh mạng Wifi theo chuẩn 'Tốc độ #1, Đồ họa #2'.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 4G Options Header
                Text(
                    text = "CHẾ ĐỘ MẠNG DI ĐỘNG 4G/5G:",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                // 4G Option 1: Data Saver High Speed
                val isDataSaver = config.cellularMode == com.example.data.CellularOptimizationMode.DATA_SAVER_HIGH_SPEED
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            1.dp,
                            if (isDataSaver) XboxNeonGreen else XboxCardBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { SettingsRepository.updateCellularMode(com.example.data.CellularOptimizationMode.DATA_SAVER_HIGH_SPEED) },
                    color = if (isDataSaver) Color(0xFF132219) else Color(0xFF0F1813)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "4G Tiết Kiệm Data (Tốc Độ Cao)",
                                    color = if (isDataSaver) XboxNeonGreen else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = XboxNeonGreen.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "KHUYÊN DÙNG",
                                        color = XboxNeonGreen,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tiết kiệm 50% data di động (~3.2GB/giờ thay vì 7GB). Khóa 60 FPS mượt mà, độ trễ đệm 0ms, H.264 Baseline chống nóng máy cho mọi chip 4G.",
                                color = TextSecondary,
                                fontSize = 10.5.sp
                            )
                        }
                        if (isDataSaver) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 4G Option 2: Max Speed Uncapped ("Có nhiêu sài hết")
                val isMaxSpeed = config.cellularMode == com.example.data.CellularOptimizationMode.MAX_SPEED_UNCAPPED
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            1.dp,
                            if (isMaxSpeed) DirectCyan else XboxCardBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { SettingsRepository.updateCellularMode(com.example.data.CellularOptimizationMode.MAX_SPEED_UNCAPPED) },
                    color = if (isMaxSpeed) Color(0xFF0F2327) else Color(0xFF0F1813)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "4G Có Nhiêu Sài Hết (Max Speed 20M)",
                                    color = if (isMaxSpeed) DirectCyan else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = DirectCyan.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "MAX BĂNG THÔNG",
                                        color = DirectCyan,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Bung trọn vẹn 20 Mbps băng thông 4G/5G tốc độ cao. Ưu tiên hình ảnh sắc nét tối đa, hàng đợi đệm WebRTC 0ms phản hồi tay cầm tức thì.",
                                color = TextSecondary,
                                fontSize = 10.5.sp
                            )
                        }
                        if (isMaxSpeed) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = DirectCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Wifi Auto-Tuning Policy Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (config.wifiSpeedFirstPriority) Color(0xFF0D211F) else Color(0xFF141D17))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = DirectCyan, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Wifi: Tự Động Tinh Chỉnh (Tốc độ #1, Đồ họa #2)",
                                color = if (config.wifiSpeedFirstPriority) DirectCyan else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tự động phân tích chất lượng sóng Wifi (2.4GHz / 5GHz) & độ trễ RTT. Luôn đặt TỐC ĐỘ LÊN HÀNG ĐẦU, khi có dao động sóng sẽ lập tức triệt tiêu độ trễ đệm để không bao giờ bị delay thao tác, đồ họa tự thích ứng theo sau.",
                            color = TextSecondary,
                            fontSize = 10.5.sp
                        )
                    }
                    Switch(
                        checked = config.wifiSpeedFirstPriority,
                        onCheckedChange = { SettingsRepository.updateWifiSpeedFirstPriority(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = DirectCyan
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Auto-Switch Rules
                Text(
                    text = "CƠ CHẾ TỰ ĐỘNG CHUYỂN MẠNG",
                    color = XboxNeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Smart VPN Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "VPN Thông Minh (Auto On/Off)",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tự động phân tích IP người chơi: Bật khi mở game, Tắt khi vào trận",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = config.smartVpnEnabled,
                        onCheckedChange = { SettingsRepository.updateSmartVpnEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = XboxNeonGreen
                        ),
                        modifier = Modifier.testTag("switch_smart_vpn")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Auto Bypass Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tự động ngắt VPN khi vào game",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Chuyển về mạng thường không cần reload trang khi WebRTC kết nối",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = config.autoBypassEnabled,
                        onCheckedChange = { SettingsRepository.updateAutoBypass(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = XboxNeonGreen
                        ),
                        modifier = Modifier.testTag("switch_auto_bypass")
                    )
                }

                // Delay Slider
                if (config.autoBypassEnabled) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(XboxSurfaceVariant)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Thời gian chờ trước khi ngắt VPN",
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${config.autoBypassDelaySeconds} giây",
                                color = DirectCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
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
                            ),
                            modifier = Modifier.testTag("slider_bypass_delay")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: VPN Engine
                Text(
                    text = "CHẾ ĐỘ VƯỢT RÀO (VPN ENGINE)",
                    color = XboxNeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                listOf(
                    Triple(com.example.vpn.VpnEngine.SMART_BYPASS, "Smart Japan Bypass 🇯🇵 (Khuyên dùng)", "Vượt rào trực tiếp Better-xCloud, 0 độ trễ, không sợ chết proxy"),
                    Triple(com.example.vpn.VpnEngine.PROXY_TUNNEL, "Proxy Route 🌐 (20+ Máy chủ)", "Định tuyến qua máy chủ Proxy Nhật Bản với tự động đổi máy chủ dự phòng"),
                    Triple(com.example.vpn.VpnEngine.EXTERNAL_VPN, "VPN Ngoài (1.1.1.1 / WARP / Kiwi)", "Dành cho người chơi dùng app VPN riêng, app sẽ hỗ trợ nhắc ngắt khi vào game")
                ).forEach { (engine, title, desc) ->
                    val isSelected = vpnState.engine == engine
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                if (isSelected) XboxNeonGreen else XboxCardBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { VpnManager.setEngine(engine) },
                        color = if (isSelected) XboxSurfaceVariant else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = desc,
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = XboxNeonGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Preferred Region / VPN Server
                Text(
                    text = "MÁY CHỦ KHU VỰC VƯỢT RÀO",
                    color = XboxNeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                DEFAULT_SERVERS.forEach { server ->
                    val isSelected = vpnState.currentServer.id == server.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                if (isSelected) XboxNeonGreen else XboxCardBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { VpnManager.selectServer(server) },
                        color = if (isSelected) XboxSurfaceVariant else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = server.flag, fontSize = 20.sp)
                                Column {
                                    Text(
                                        text = server.name,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = server.description,
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = XboxNeonGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Visual & Controls
                Text(
                    text = "HÌNH ẢNH & ĐIỀU KHIỂN",
                    color = XboxNeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Desktop 1080p UA Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kích hoạt luồng 1080p (Edge UA)",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Giả lập trình duyệt Edge Desktop để Xbox stream ở chất lượng Full HD",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = config.forceDesktopUserAgent,
                        onCheckedChange = { SettingsRepository.updateDesktopUserAgent(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = XboxNeonGreen
                        )
                    )
                }

                // Clarity Boost
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bộ lọc Clarity Boost (Tăng độ nét)",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Khử nhòe và tăng độ sắc nét cho hình ảnh stream",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = config.clarityBoostEnabled,
                        onCheckedChange = { SettingsRepository.updateClarityBoost(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = XboxNeonGreen
                        )
                    )
                }

                // Virtual Controller Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tay cầm ảo trên màn hình (Touch)",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Hiển thị các nút điều khiển ảo khi không có tay cầm Bluetooth",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = config.showVirtualController,
                        onCheckedChange = { SettingsRepository.updateShowVirtualController(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = XboxNeonGreen
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Clear Cache Button
                Button(
                    onClick = onClearCookies,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("clear_cookies_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = XboxSurfaceVariant)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = TextSecondary)
                        Text(text = "Xóa Cookie & Đăng nhập lại Xbox", color = TextSecondary)
                    }
                }
            }
        }
    }
}
