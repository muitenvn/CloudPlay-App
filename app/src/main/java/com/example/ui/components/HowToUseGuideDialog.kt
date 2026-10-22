package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JapanRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxDark
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant

enum class GuideSection(val title: String, val icon: ImageVector) {
    QUICK_START("Bắt Đầu", Icons.Default.RocketLaunch),
    REVOLUTION("Tối Ưu", Icons.Default.AutoAwesome),
    ZERO_LAG("Mạng & Hình", Icons.Default.Speed),
    CONTROLLER("Tay Cầm", Icons.Default.Gamepad),
    FAQS("Mẹo Nhanh", Icons.Default.Lightbulb)
}

@Composable
fun HowToUseGuideDialog(
    initialSection: GuideSection = GuideSection.QUICK_START,
    onDismiss: () -> Unit
) {
    val startSection = if (initialSection == GuideSection.REVOLUTION) GuideSection.ZERO_LAG else initialSection
    var selectedSection by remember { mutableStateOf(startSection) }
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isVi = !config.isEnglishUI()
    val visibleSections = remember {
        listOf(
            GuideSection.QUICK_START,
            GuideSection.ZERO_LAG,
            GuideSection.CONTROLLER,
            GuideSection.FAQS
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    1.5.dp,
                    Brush.verticalGradient(listOf(XboxNeonGreen, DirectCyan, XboxCardBorder)),
                    RoundedCornerShape(24.dp)
                )
                .testTag("how_to_use_guide_dialog"),
            color = XboxDark.copy(alpha = 0.98f),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DirectCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = null,
                                tint = DirectCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (isVi) "HƯỚNG DẪN NHANH" else "QUICK GUIDE",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = if (isVi) "Cách chơi Xbox Cloud mượt và dễ dàng" else "Simple tips for smooth Xbox Cloud play",
                                color = XboxNeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isVi) "Đóng" else "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Section Tabs Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(XboxSurfaceVariant.copy(alpha = 0.8f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    visibleSections.forEach { section ->
                        val isSelected = selectedSection == section
                        val tabTitle = when (section) {
                            GuideSection.QUICK_START -> if (isVi) "Bắt Đầu" else "Start"
                            GuideSection.ZERO_LAG -> if (isVi) "Mạng & Hình" else "Quality"
                            GuideSection.CONTROLLER -> if (isVi) "Tay Cầm" else "Controls"
                            GuideSection.FAQS -> if (isVi) "Sửa Lỗi" else "Help"
                            GuideSection.REVOLUTION -> if (isVi) "Tối Ưu" else "Optimize"
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedSection = section }
                                .padding(1.dp),
                            color = if (isSelected) XboxGreen else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = section.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = tabTitle,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Scrollable Content per Section
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (selectedSection) {
                        GuideSection.QUICK_START -> {
                            item {
                                GuideStepCard(
                                    stepNum = "1",
                                    badge = if (isVi) "Tài khoản" else "Account",
                                    title = if (isVi) "Đăng nhập tài khoản Microsoft" else "Sign in with Microsoft account",
                                    desc = if (isVi) "• Fortnite hoàn toàn miễn phí, chỉ cần tài khoản Microsoft thường.\n• Các game khác trong thư viện cần gói Game Pass." else "• Fortnite is completely free with a standard Microsoft account.\n• Other catalog games require a Game Pass subscription."
                                )
                            }
                            item {
                                GuideStepCard(
                                    stepNum = "2",
                                    badge = if (isVi) "Vào game" else "Launch",
                                    title = if (isVi) "Chọn game và bấm Chơi" else "Pick a game and tap Play",
                                    desc = if (isVi) "• Ứng dụng tự động mở khóa vùng khi bạn bấm vào game.\n• Khi đã vào trận, mạng sẽ tự chuyển sang kết nối trực tiếp để giảm độ trễ." else "• The app automatically unlocks region access when you launch a game.\n• Once the game starts, it switches to direct connection for low delay."
                                )
                            }
                            item {
                                GuideStepCard(
                                    stepNum = "3",
                                    badge = if (isVi) "Tùy chỉnh" else "Controls",
                                    title = if (isVi) "Dùng nút nổi trong lúc chơi" else "Use the floating menu in-game",
                                    desc = if (isVi) "• Chạm nút tròn nổi trên màn hình để bật tay cầm cảm ứng, đổi mức hình ảnh hoặc mở Cài đặt nhanh." else "• Tap the floating button on screen to toggle touch controls, adjust image quality, or open Quick Settings."
                                )
                            }
                        }

                        GuideSection.ZERO_LAG, GuideSection.REVOLUTION -> {
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.Bolt,
                                    iconColor = DirectCyan,
                                    title = if (isVi) "Giảm trễ tự động khi vào game" else "Automatic low-delay connection",
                                    content = if (isVi) {
                                        "Ứng dụng chỉ dùng máy chủ trung gian lúc mới mở game. Ngay khi hình ảnh hiện lên, kết nối sẽ chuyển về mạng trực tiếp của bạn để thao tác nhạy và mượt hơn."
                                    } else {
                                        "The app only uses a relay server when starting the game. As soon as video appears, it switches to your direct internet connection for responsive controls."
                                    }
                                )
                            }
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.Wifi,
                                    iconColor = XboxNeonGreen,
                                    title = if (isVi) "Chọn mức chất lượng phù hợp" else "Choose the right quality level",
                                    content = if (isVi) {
                                        "• Tự động (Khuyên dùng): Tự cân bằng độ nét và độ mượt theo mạng.\n• Thấp / Chế độ máy yếu: Giúp máy mát, đỡ hao pin và tiết kiệm dữ liệu.\n• Cao / Rất cao: Dành cho mạng Wi-Fi mạnh và máy khỏe."
                                    } else {
                                        "• Auto (Recommended): Balances sharpness and smoothness automatically.\n• Low / Low-End Mode: Keeps your device cool and saves mobile data.\n• High / Ultra: Best for strong Wi-Fi and fast devices."
                                    }
                                )
                            }
                        }

                        GuideSection.CONTROLLER -> {
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.Gamepad,
                                    iconColor = XboxNeonGreen,
                                    title = if (isVi) "Tay cầm rời (Bluetooth / USB-C)" else "Physical Controller (Bluetooth / USB-C)",
                                    content = if (isVi) {
                                        "Kết nối tay cầm Xbox, PlayStation hoặc tay cầm chơi game điện thoại qua Bluetooth/cổng sạc. Ứng dụng sẽ tự nhận diện ngay."
                                    } else {
                                        "Connect any Xbox, PlayStation, or mobile gamepad via Bluetooth or USB-C. The app detects it automatically."
                                    }
                                )
                            }
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.CheckCircle,
                                    iconColor = DirectCyan,
                                    title = if (isVi) "Tay cầm cảm ứng trên màn hình" else "On-Screen Touch Controller",
                                    content = if (isVi) {
                                        "Không có tay cầm rời? Hãy bật biểu tượng Tay cầm trên thanh nổi khi đang chơi để hiện các nút bấm ảo trên màn hình."
                                    } else {
                                        "No physical gamepad? Tap the Controller icon on the floating menu while playing to show on-screen touch buttons."
                                    }
                                )
                            }
                        }

                        GuideSection.FAQS -> {
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.Language,
                                    iconColor = JapanRed,
                                    title = if (isVi) "Trang Xbox hiện tiếng nước ngoài?" else "Xbox page showing foreign language?",
                                    content = if (isVi) {
                                        "Bật công tắc 'Tự dịch tiếng Việt' trong Cài đặt nhanh, hoặc vào tab Cài đặt chọn lại ngôn ngữ trang Xbox."
                                    } else {
                                        "Enable 'Auto Translate' in Quick Settings, or choose your preferred Xbox page language in the Settings tab."
                                    }
                                )
                            }
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.Speed,
                                    iconColor = WarningAmber,
                                    title = if (isVi) "Báo lỗi vùng hoặc vào game chậm?" else "Region error or slow loading?",
                                    content = if (isVi) {
                                        "Sang tab 'Máy chủ' và chọn một máy chủ khác có đèn xanh, sau đó mở lại game."
                                    } else {
                                        "Open the 'Servers' tab, pick another server with a green status badge, and launch the game again."
                                    }
                                )
                            }
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.Bolt,
                                    iconColor = DirectCyan,
                                    title = if (isVi) "Đứng hình hoặc mất tiếng?" else "Frozen screen or no audio?",
                                    content = if (isVi) {
                                        "Mở thanh điều khiển nổi trên màn hình chơi và bấm 'Tải lại' để kết nối lại ngay mà không mất phiên chơi."
                                    } else {
                                        "Open the floating menu in-game and tap 'Reload' to reconnect without losing your game session."
                                    }
                                )
                            }
                        }
                    }
                }

                // Bottom Action
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isVi) "Đã hiểu • Bắt đầu chơi 🎮" else "Got It • Start Playing 🎮",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideStepCard(
    stepNum: String,
    badge: String,
    title: String,
    desc: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, XboxCardBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = XboxSurface.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(XboxNeonGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stepNum,
                            color = XboxNeonGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DirectCyan.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badge,
                        color = DirectCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = desc,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun GuideDetailCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    content: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, XboxCardBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = XboxSurface.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = content,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
