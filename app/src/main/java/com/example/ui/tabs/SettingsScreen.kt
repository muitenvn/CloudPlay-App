package com.example.ui.tabs

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AppConfig
import com.example.data.AppLanguage
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
import androidx.compose.ui.graphics.Brush
import java.util.Locale

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

    val context = LocalContext.current
    val systemLocale = remember { Locale.getDefault() }
    val resolvedSystemLocale = remember { SettingsRepository.resolveSystemXboxLocale() }
    val isEn = config.isEnglishUI()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Console System Profile Overview Banner
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = XboxSurface.copy(alpha = 0.96f),
                border = androidx.compose.foundation.BorderStroke(
                    1.2.dp,
                    Brush.horizontalGradient(
                        listOf(XboxNeonGreen.copy(alpha = 0.55f), XboxCardBorder, DirectCyan.copy(alpha = 0.55f))
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isEn) "PRO SYSTEM CONFIGURATION CENTER" else "TRUNG TÂM CẤU HÌNH HỆ THỐNG PRO",
                                color = TextPrimary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = XboxNeonGreen.copy(alpha = 0.18f),
                            border = androidx.compose.foundation.BorderStroke(0.6.dp, XboxNeonGreen.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "60-120Hz READY",
                                color = XboxNeonGreen,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple(if (isEn) "GRAPHICS" else "ĐỒ HỌA", config.cellularMode.localizedShortLabel(isEn), XboxNeonGreen),
                            Triple(if (isEn) "WEB REGION" else "VÙNG WEB", config.getEffectiveLocale(), DirectCyan),
                            Triple("CLARITY", if (config.clarityBoostEnabled) (if (isEn) "ON" else "Đang Bật") else (if (isEn) "OFF" else "Tắt"), XboxNeonGreen)
                        ).forEach { (label, value, color) ->
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF08100C),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, color.copy(alpha = 0.35f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = label,
                                        color = TextSecondary,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = value,
                                        color = color,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 0: App Language (Multilingual UI Support)
        item {
            SettingsCard(title = stringResource(R.string.section_app_language)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.app_language_desc),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    // 1. System Default (Auto)
                    val isSystemSelected = config.appLanguage == AppLanguage.SYSTEM
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                1.dp,
                                if (isSystemSelected) XboxNeonGreen else XboxCardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { SettingsRepository.updateAppLanguage(AppLanguage.SYSTEM) }
                            .testTag("app_lang_system_button"),
                        color = if (isSystemSelected) XboxSurfaceVariant else Color.Transparent
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
                                Text("🌐", fontSize = 16.sp)
                                Column {
                                    Text(
                                        text = stringResource(R.string.lang_system_default),
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    val sys = Locale.getDefault()
                                    val detectedStr = if (sys.language.lowercase().startsWith("vi")) "Tiếng Việt (${sys.displayName})" else "English (${sys.displayName})"
                                    Text(
                                        text = if (isEn) "System: $detectedStr" else "Hệ thống: $detectedStr",
                                        color = DirectCyan,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            if (isSystemSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // 2. Tiếng Việt (Vietnamese)
                    val isViSelected = config.appLanguage == AppLanguage.VIETNAMESE
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                1.dp,
                                if (isViSelected) XboxNeonGreen else XboxCardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { SettingsRepository.updateAppLanguage(AppLanguage.VIETNAMESE) }
                            .testTag("app_lang_vi_button"),
                        color = if (isViSelected) XboxSurfaceVariant else Color.Transparent
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
                                Text("🇻🇳", fontSize = 16.sp)
                                Column {
                                    Text(
                                        text = stringResource(R.string.lang_vietnamese),
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = if (isEn) "Natural Vietnamese interface & auto-translator" else "Giao diện tiếng Việt chuẩn, tự nhiên",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            if (isViSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // 3. English
                    val isEnSelected = config.appLanguage == AppLanguage.ENGLISH
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                1.dp,
                                if (isEnSelected) XboxNeonGreen else XboxCardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { SettingsRepository.updateAppLanguage(AppLanguage.ENGLISH) }
                            .testTag("app_lang_en_button"),
                        color = if (isEnSelected) XboxSurfaceVariant else Color.Transparent
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
                                Text("🇺🇸", fontSize = 16.sp)
                                Column {
                                    Text(
                                        text = stringResource(R.string.lang_english),
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Clean, native English UI (disables Vietnamese web auto-translation)",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            if (isEnSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Section 1: Xbox Cloud Web Region
        item {
            SettingsCard(title = stringResource(R.string.section_xbox_web_language)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.xbox_web_language_desc),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    // Option A: Use System Language (with fallback to en-US)
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
                                        text = if (isEn) "Follow System Language (Auto fallback to en-US)" else "Theo ngôn ngữ hệ thống (Tự động fallback en-US)",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isEn) {
                                        "Device: ${systemLocale.displayName} (${systemLocale.toLanguageTag()}) ➔ Xbox Locale: $resolvedSystemLocale"
                                    } else {
                                        "Thiết bị: ${systemLocale.displayName} (${systemLocale.toLanguageTag()}) ➔ Áp dụng Xbox: $resolvedSystemLocale"
                                    },
                                    color = DirectCyan,
                                    fontSize = 10.sp
                                )
                                if (systemLocale.language.lowercase() == "vi") {
                                    Text(
                                        text = if (isEn) {
                                            "*(Vietnamese is not natively on xCloud ➔ auto-defaults to English en-US)"
                                        } else {
                                            "*(Tiếng Việt chưa có trên xCloud ➔ tự động chuyển tiếng Anh en-US để dễ đọc)"
                                        },
                                        color = TextSecondary,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            if (config.useSystemLanguage) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = XboxNeonGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isEn) "OR SELECT A SPECIFIC XBOX LOCALE" else "HOẶC CHỌN NGÔN NGỮ CỤ THỂ",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    // Popular Language Chips / Items
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
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(text = lang.flag, fontSize = 16.sp)
                                        Column {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = lang.localizedName(isEn),
                                                    color = TextPrimary,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 12.sp
                                                )
                                                if (lang.code == "en-US") {
                                                    Text(
                                                        text = if (isEn) "RECOMMENDED" else "KHUYÊN DÙNG",
                                                        color = Color.Black,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        modifier = Modifier
                                                            .background(XboxNeonGreen, RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = if (isEn) "${lang.nativeName} • Code: ${lang.code}" else "${lang.nativeName} • Mã: ${lang.code}",
                                                color = TextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = XboxNeonGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Option C: Manual Language input / Paste link (e.g. "xbox.com/en-US")
                    Text(
                        text = if (isEn) "ENTER CUSTOM LOCALE CODE OR XBOX URL" else "NHẬP TÙY CHỌN MÃ NGÔN NGỮ HOẶC LINK XBOX",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    OutlinedTextField(
                        value = customInputText,
                        onValueChange = { customInputText = it },
                        label = { Text(if (isEn) "Example: xbox.com/en-US or en-US" else "Ví dụ: xbox.com/en-US hoặc en-US") },
                        placeholder = { Text("xbox.com/en-US") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = XboxNeonGreen,
                            unfocusedBorderColor = XboxCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.3f)
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
                            onClick = {
                                SettingsRepository.updateCustomLocale(customInputText)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isEn) "Apply Code" else "Áp dụng mã này", fontSize = 11.sp)
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

                    // Display Current Active Target URL
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)),
                        color = Color.Black.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = DirectCyan, modifier = Modifier.size(14.dp))
                            Text(
                                text = if (isEn) "Current Xbox Cloud URL: ${config.targetRegionUrl}" else "Link Xbox Cloud hiện tại: ${config.targetRegionUrl}",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Section 1.5: BỘ DỊCH TIẾNG VIỆT XBOX CLOUD (TỐI ƯU ĐỘC QUYỀN)
        item {
            SettingsCard(
                title = if (isEn) {
                    "XBOX CLOUD TRANSLATOR & OVERLAY FILTER"
                } else {
                    "BỘ DỊCH TIẾNG VIỆT XBOX CLOUD (TỐI ƯU ĐỘC QUYỀN)"
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp)),
                        color = Color.Black.copy(alpha = 0.4f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(16.dp))
                                Text(
                                    text = if (isEn) {
                                        "Smart Gaming Web Translator (Auto-paused in English UI & In-Game)"
                                    } else {
                                        "Google Dịch Thông Minh Cho Xbox Cloud (Smart Gaming Translator)"
                                    },
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isEn) {
                                    "✓ When App Language is set to English, web auto-translation to Vietnamese is automatically disabled so Xbox Cloud stays 100% in English.\n✓ Automatically pauses DOM translation during WebRTC gameplay for 0ms latency overhead.\n✓ Protects original game titles (Fortnite, Forza, Halo...) and controller prompts."
                                } else {
                                    "✓ Dịch y chang Google Dịch tự nhiên 100% mọi mô tả cốt truyện, hướng dẫn, đánh giá và menu game.\n✓ Biết hết chỗ nào để né: Không đụng thẻ video stream WebRTC, không làm giật lag hay mất 60 FPS.\n✓ Tự động bảo vệ tên game (Fortnite, Forza, Halo, Starfield...) và phím tay cầm (RT, LT, D-pad).\n✓ Kết hợp từ điển 0ms tức thì cho giao diện và Google Translate mượt mà cho đoạn văn dài."
                                },
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Toggle 1: Auto Translate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEn) "Auto-translate Xbox web catalog to Vietnamese" else "Tự động dịch sang Tiếng Việt",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isEn) {
                                    "Currently inactive while English UI is selected (switch App Language to Vietnamese to enable)"
                                } else {
                                    "Dịch toàn bộ menu, danh mục game, nút bấm và thông báo"
                                },
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = config.xcloudTranslateEnabled,
                            onCheckedChange = { SettingsRepository.updateXcloudTranslate(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = XboxNeonGreen),
                            modifier = Modifier.testTag("settings_switch_xcloud_translate")
                        )
                    }

                    // Toggle 2: Preserve Game Titles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEn) "Preserve Original Game Titles" else "Bảo vệ tên tựa game gốc",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isEn) "Keep original game names (Fortnite, Forza Horizon, Halo Infinite...)" else "Giữ nguyên tên gốc các game (Fortnite, Forza Horizon, Halo Infinite...)",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = config.preserveGameTitles,
                            onCheckedChange = { SettingsRepository.updatePreserveGameTitles(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = XboxNeonGreen)
                        )
                    }

                    // Toggle 3: Bilingual mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEn) "Bilingual Mode (Vietnamese - English)" else "Chế độ song ngữ (Việt - Anh)",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isEn) "Display original English term in parentheses next to Vietnamese text" else "Kèm từ gốc Tiếng Anh trong ngoặc bên cạnh bản dịch tiếng Việt",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = config.bilingualMode,
                            onCheckedChange = { SettingsRepository.updateBilingualMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = XboxNeonGreen)
                        )
                    }

                    // Toggle 4: Hide Decode Profile / Stream Stats overlay
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEn) "Hide Web Decode Profile / Stream Stats Overlay" else "Ẩn bảng Decode Profile / Stream Stats đè màn hình",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isEn) "Automatically hide intrusive web technical overlays blocking gameplay" else "Tự động ẩn bảng thông số kỹ thuật (Decode profile, FPS, Bitrate) che tầm nhìn khi chơi",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = config.hideStreamStatsOverlay,
                            onCheckedChange = { SettingsRepository.updateHideStreamStatsOverlay(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = DirectCyan),
                            modifier = Modifier.testTag("settings_switch_hide_decode_profile")
                        )
                    }

                    // Toggle 5: GitHub Better-xCloud Dictionary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEn) "Full Better-xCloud Gaming Dictionary (550+ entries)" else "Từ điển Gaming đầy đủ từ GitHub (550+ mục)",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isEn) "O(1) instant lookup gaming terminology dictionary" else "Trọn bộ từ điển Better-xCloud GitHub với thuật toán tra cứu O(1) tức thì",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = config.githubDictionaryEnabled,
                            onCheckedChange = { SettingsRepository.updateGithubDictionaryEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = XboxNeonGreen)
                        )
                    }
                }
            }
        }

        // Section: Graphics Quality & Device Performance Optimization
        item {
            SettingsCard(title = if (isEn) "GRAPHICS & DEVICE PERFORMANCE" else "ĐỒ HỌA & HIỆU NĂNG MÁY") {
                Text(
                    text = if (isEn) {
                        "Customize stream bitrate, mobile data usage, and CPU/GPU decode workload (prevents overheating, saves battery, and locks smooth 60 FPS)."
                    } else {
                        "Tùy chỉnh mức đồ họa, dung lượng tiêu thụ và giảm tải CPU/GPU cho điện thoại (chống nóng máy, tiết kiệm pin và giữ 60 FPS mượt mà)."
                    },
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                val selectedMode = config.cellularMode
                val isAutoSelected = selectedMode == com.example.data.CellularOptimizationMode.AUTO

                // 1. Nút Chế độ Tự Động (Nổi bật trên cùng)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.2.dp,
                            if (isAutoSelected) XboxNeonGreen else XboxCardBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { SettingsRepository.updateCellularMode(com.example.data.CellularOptimizationMode.AUTO) }
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
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isAutoSelected) XboxNeonGreen.copy(alpha = 0.2f) else Color(0xFF18261E),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (isAutoSelected) XboxNeonGreen else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (isEn) "Auto (Network & Device Adaptive)" else "Tự Động (Theo Mạng & Máy)",
                                        color = if (isAutoSelected) XboxNeonGreen else TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = XboxNeonGreen.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = if (isEn) "RECOMMENDED" else "KHUYÊN DÙNG",
                                            color = XboxNeonGreen,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isEn) "Automatically optimizes for Wi-Fi / 4G & throttles load when device warms up" else "Tự động tối ưu theo Wi-Fi / 4G & giảm tải khi máy nóng",
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

                // 2. Lưới 2x2 cho 4 mức độ (Thấp, Trung Bình, Cao, Rất Cao)
                val manualModes = listOf(
                    com.example.data.CellularOptimizationMode.DATA_SAVER_HIGH_SPEED,
                    com.example.data.CellularOptimizationMode.MEDIUM,
                    com.example.data.CellularOptimizationMode.HIGH,
                    com.example.data.CellularOptimizationMode.MAX_SPEED_UNCAPPED
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    manualModes.chunked(2).forEach { rowModes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowModes.forEach { mode ->
                                val isSelected = selectedMode == mode
                                val isLowOrMed = mode == com.example.data.CellularOptimizationMode.DATA_SAVER_HIGH_SPEED ||
                                    mode == com.example.data.CellularOptimizationMode.MEDIUM
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
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
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
                                            } else {
                                                Text(
                                                    text = "${mode.bitrateMbps}M",
                                                    color = TextSecondary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
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

                // 3. Khung tóm tắt chi tiết chế độ đang chọn
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0B140E),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEn) "Selected: ${selectedMode.localizedTitle(true)}" else "Đang chọn: ${selectedMode.localizedTitle(false)}",
                                color = XboxNeonGreen,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = selectedMode.localizedBadge(isEn),
                                color = DirectCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = selectedMode.localizedDesc(isEn),
                            color = TextSecondary,
                            fontSize = 10.5.sp,
                            lineHeight = 14.5.sp
                        )
                    }
                }

                // 4. Công tắc Ưu tiên độ trễ thấp & giảm tải khi mạng yếu
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
                            Icon(Icons.Default.Speed, contentDescription = null, tint = DirectCyan, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (isEn) "Prioritize Ultra-Low Latency & Anti-Overheat" else "Ưu Tiên Độ Trễ Thấp & Chống Nóng Máy",
                                color = if (config.wifiSpeedFirstPriority) DirectCyan else TextPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isEn) {
                                "Eliminates WebRTC jitter buffer bloat and lowers decode overhead when signal drops or GPU decode slows down."
                            } else {
                                "Khi sóng yếu hoặc GPU giải mã chậm, tự động triệt tiêu đệm trễ và hạ tải đồ họa để thao tác luôn mượt tức thì."
                            },
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

                Spacer(modifier = Modifier.height(8.dp))

                // 5. Công tắc Chế độ máy yếu (tùy chọn do người dùng bật, không tự áp cho mọi máy)
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
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = if (config.lowEndDeviceMode) XboxNeonGreen else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isEn) "Low-End Device Mode" else "Chế độ máy yếu",
                                    color = if (config.lowEndDeviceMode) XboxNeonGreen else TextPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isEn) {
                                    "Locks fixed low bitrate (~5 Mbps), forces lightweight H.264 Baseline in SDP handshake, prioritizes 60 FPS, disables HUD/loading infinite animations, and auto-pauses the web translator."
                                } else {
                                    "Khóa bitrate thấp cố định (~5 Mbps), dùng H.264 nhẹ trong SDP lúc kết nối, ưu tiên 60 FPS, tắt hiệu ứng động (HUD & màn tải) và tự tạm dừng bộ dịch."
                                },
                                color = TextSecondary,
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp
                            )
                        }
                        Switch(
                            checked = config.lowEndDeviceMode,
                            onCheckedChange = { enabled ->
                                SettingsRepository.updateLowEndDeviceMode(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = XboxNeonGreen
                            ),
                            modifier = Modifier.testTag("settings_low_end_device_mode_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (config.lowEndDeviceMode) XboxNeonGreen.copy(alpha = 0.14f) else Color(0xFF0E1712),
                        border = androidx.compose.foundation.BorderStroke(
                            0.6.dp,
                            if (config.lowEndDeviceMode) XboxNeonGreen.copy(alpha = 0.45f) else XboxCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isEn) "💡 Exit and relaunch the game to apply" else "💡 Thoát game và vào lại để áp dụng",
                            color = if (config.lowEndDeviceMode) XboxNeonGreen else DirectCyan,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Section 2: Smart VPN & Auto Bypass Logic
        item {
            SettingsCard(title = if (isEn) "SMART VPN (AUTO ON/OFF) & ZERO RELOAD" else "VPN THÔNG MINH (SMART VPN AUTO ON/OFF) & ZERO RELOAD") {
                // Smart VPN Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEn) "Enable Smart VPN (Auto On/Off)" else "Kích hoạt VPN Thông Minh (Auto On/Off)",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isEn) {
                                "Automatically detects your IP region: turns VPN on when launching Xbox if unsupported, and disconnects once in-game"
                            } else {
                                "Tự động phân tích IP người chơi: Tự bật VPN khi mở game nếu chưa hỗ trợ, tự tắt khi vào trận"
                            },
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = config.smartVpnEnabled,
                        onCheckedChange = { SettingsRepository.updateSmartVpnEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = XboxNeonGreen),
                        modifier = Modifier.testTag("settings_switch_smart_vpn")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEn) "Auto-disconnect VPN when entering game" else "Tự động ngắt VPN khi vào game",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isEn) {
                                "Switches to Direct ISP immediately after WebRTC stream connects without reloading the page"
                            } else {
                                "Chuyển về mạng thường (Direct ISP) ngay khi luồng WebRTC kết nối, không reload trang"
                            },
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = config.autoBypassEnabled,
                        onCheckedChange = { SettingsRepository.updateAutoBypass(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = XboxNeonGreen),
                        modifier = Modifier.testTag("settings_switch_auto_bypass")
                    )
                }

                if (config.autoBypassEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(10.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = if (isEn) "Countdown delay before disconnecting VPN" else "Thời gian đếm ngược trước khi ngắt VPN",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = if (isEn) "${config.autoBypassDelaySeconds} sec" else "${config.autoBypassDelaySeconds} giây",
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
                            colors = SliderDefaults.colors(thumbColor = DirectCyan, activeTrackColor = DirectCyan, inactiveTrackColor = Color.DarkGray)
                        )
                    }
                }
            }
        }

        // Section 3: Engine Selection
        item {
            SettingsCard(title = if (isEn) "REGION BYPASS MODE (VPN ENGINE)" else "CHẾ ĐỘ VƯỢT RÀO (VPN ENGINE)") {
                listOf(
                    Triple(
                        VpnEngine.SMART_BYPASS,
                        if (isEn) "Smart Japan Bypass 🇯🇵 (Recommended)" else "Smart Japan Bypass 🇯🇵 (Khuyên dùng)",
                        if (isEn) "Direct Better-xCloud header bypass, 0ms extra latency, immune to proxy downtime" else "Vượt rào trực tiếp Better-xCloud, 0 độ trễ, không sợ chết proxy"
                    ),
                    Triple(
                        VpnEngine.PROXY_TUNNEL,
                        if (isEn) "Proxy Route 🌐 (20+ Servers)" else "Proxy Route 🌐 (20+ Máy chủ)",
                        if (isEn) "Routes login traffic via regional proxies with automatic failover" else "Định tuyến qua máy chủ Proxy Nhật Bản với tự động đổi máy chủ dự phòng"
                    ),
                    Triple(
                        VpnEngine.EXTERNAL_VPN,
                        if (isEn) "External VPN (1.1.1.1 / WARP / Custom)" else "VPN Ngoài (1.1.1.1 / WARP / Kiwi)",
                        if (isEn) "For players using a separate third-party VPN app" else "Dành cho người chơi dùng app VPN riêng"
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
                            modifier = Modifier.padding(12.dp),
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

        // Section 4: Stream Quality & Visuals
        item {
            SettingsCard(title = if (isEn) "STREAM VISUALS & CONTROLS" else "CHẤT LƯỢNG HÌNH ẢNH & ĐIỀU KHIỂN") {
                // 1080p Edge UA
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEn) "1080p Full HD Resolution (Edge Desktop UA)" else "Độ phân giải 1080p Full HD (Edge UA)",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isEn) "Spoofs Desktop Edge so Xbox streams at 1080p instead of 720p" else "Giả lập Edge Desktop để Xbox stream ở 1080p thay vì 720p",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = config.forceDesktopUserAgent,
                        onCheckedChange = { SettingsRepository.updateDesktopUserAgent(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = XboxNeonGreen)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Clarity Boost
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEn) "Clarity Boost Filter (Sharpening)" else "Bộ lọc Clarity Boost (Làm nét)",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isEn) "Enhances video stream edge sharpness and contrast" else "Tăng độ sắc nét và tương phản video stream",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = config.clarityBoostEnabled,
                        onCheckedChange = { SettingsRepository.updateClarityBoost(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = XboxNeonGreen)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Virtual Touch Gamepad
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEn) "On-Screen Virtual Touch Controller" else "Tay cầm ảo trên màn hình",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isEn) "Show virtual D-Pad, thumbsticks, and ABXY buttons when no physical controller is connected" else "Hiện D-Pad, cần Analog và nút ABXY khi không có tay cầm Bluetooth",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = config.showVirtualController,
                        onCheckedChange = { SettingsRepository.updateShowVirtualController(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = XboxNeonGreen)
                    )
                }
            }
        }

        // Section 5: Storage & Reset
        item {
            SettingsCard(title = if (isEn) "CACHE & ACCOUNT SESSION" else "BỘ NHỚ ĐỆM & TÀI KHOẢN") {
                Button(
                    onClick = onClearCookies,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = XboxSurfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (isEn) "Clear Cookies & Re-login to Xbox" else "Xóa Cookie & Đăng Nhập Lại Xbox",
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Section 6: Legal & Open Source (Better-xCloud MIT License)
        item {
            SettingsCard(title = if (isEn) "TERMS OF USE & OPEN SOURCE LICENSE" else "ĐIỀU KHOẢN & BẢN QUYỀN NGUỒN MỞ") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isEn) {
                            "Independent non-profit application integrating open-source optimizations from the Better-xCloud project (MIT License). Not affiliated with Microsoft or Xbox."
                        } else {
                            "Ứng dụng phi lợi nhuận độc lập, tích hợp các giải pháp tối ưu hóa mã nguồn mở từ dự án Better-xCloud (MIT License). Hoàn toàn không liên kết với Microsoft hay Xbox."
                        },
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
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
                                text = if (isEn) "View Terms of Use & MIT License" else "Xem Điều Khoản Sử Dụng & Giấy Phép MIT",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(28.dp)) }
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
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(XboxNeonGreen.copy(alpha = 0.35f), XboxCardBorder, DirectCyan.copy(alpha = 0.25f))
                ),
                RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = XboxSurface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.verticalGradient(listOf(XboxNeonGreen, DirectCyan))
                        )
                )
                Text(
                    text = title,
                    color = XboxNeonGreen,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
            }
            content()
        }
    }
}

