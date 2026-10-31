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
                                text = config.tr("CÁCH DÙNG CƠ BẢN", "HOW TO USE"),
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = config.tr("Các bước đăng nhập, mở khóa vùng và chỉnh nút điều khiển", "Sign in, unlock region, and set up controls"),
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
                            contentDescription = config.tr("Đóng", "Close"),
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
                            GuideSection.QUICK_START -> config.tr("Bắt Đầu", "Start")
                            GuideSection.ZERO_LAG -> config.tr("Mạng & Hình", "Quality")
                            GuideSection.CONTROLLER -> config.tr("Tay Cầm", "Controls")
                            GuideSection.FAQS -> config.tr("Sửa Lỗi", "Help")
                            GuideSection.REVOLUTION -> config.tr("Tối Ưu", "Optimize")
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
                                    badge = config.tr("Tài khoản", "Account"),
                                    title = config.tr("Đăng nhập tài khoản Microsoft", "Sign in with Microsoft account"),
                                    desc = config.tr(
                                        "• Mở trang Xbox và đăng nhập tài khoản Microsoft của bạn.\n• Fortnite được chơi miễn phí trên đám mây; các game khác cần tài khoản có gói Game Pass.",
                                        "• Open the Xbox page and sign in with your Microsoft account.\n• Fortnite is free to play on cloud; other games require a Game Pass subscription."
                                    )
                                )
                            }
                            item {
                                GuideStepCard(
                                    stepNum = "2",
                                    badge = config.tr("Mở vùng", "Region"),
                                    title = config.tr("Bật VPN/Proxy lúc mở game rồi tắt khi đã vào", "Use VPN/Proxy to launch, then turn it off in-game"),
                                    desc = config.tr(
                                        "• Xbox Cloud chưa hỗ trợ chính thức ở Việt Nam nên cần bật VPN/Proxy lúc bấm mở game.\n• Khi hình ảnh game đã hiện lên, hãy tắt VPN (hoặc bật 'Tự tắt VPN') để truyền hình ảnh trực tiếp qua mạng của bạn, đỡ bị trễ hơn.",
                                        "• If your region is unsupported, enable VPN/Proxy when launching a game.\n• Once video starts, turn off VPN (or use 'Auto turn off VPN') so the video stream uses your direct connection."
                                    )
                                )
                            }
                            item {
                                GuideStepCard(
                                    stepNum = "3",
                                    badge = config.tr("Điều khiển", "Controls"),
                                    title = config.tr("Bật tay cầm ảo hoặc gắn tay cầm rời", "Enable touch buttons or connect a gamepad"),
                                    desc = config.tr(
                                        "• Nếu không có tay cầm Bluetooth/USB-C, chạm vào nút tròn nổi trên màn hình khi vào game để bật các nút bấm cảm ứng.",
                                        "• If you don't have a Bluetooth/USB-C controller, tap the floating button in-game to show on-screen touch buttons."
                                    )
                                )
                            }
                        }

                        GuideSection.ZERO_LAG, GuideSection.REVOLUTION -> {
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.Bolt,
                                    iconColor = DirectCyan,
                                    title = config.tr("Vì sao nên tắt VPN sau khi vào game?", "Why turn off VPN after the game starts?"),
                                    content = config.tr(
                                        "Xbox chỉ kiểm tra quốc gia lúc bạn bấm mở trò chơi. Khi đã vào trong game, việc tắt VPN giúp dữ liệu hình ảnh không phải đi vòng qua máy chủ trung gian nên thao tác đỡ trễ hơn.",
                                        "Xbox only checks your region when launching a game. Once video starts, turning off the relay avoids routing video traffic through an extra server."
                                    )
                                )
                            }
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.Wifi,
                                    iconColor = XboxNeonGreen,
                                    title = config.tr("Chọn độ phân giải theo mạng đang dùng", "Choose resolution based on your network"),
                                    content = config.tr(
                                        "• Tự động (720p ⇄ 1080p): Hạ xuống 720p khi mạng yếu và tăng lên 1080p khi mạng ổn định.\n• 720p • Thấp / 900p • Vừa: Hợp với 4G/5G hoặc khi muốn đỡ tốn dữ liệu và đỡ nóng máy.\n• 1080p • Cao / Rất cao: Chỉ nên chọn khi ngồi gần Wi-Fi 5GHz ổn định.",
                                        "• Auto (720p ⇄ 1080p): Lowers to 720p on weak signal and restores 1080p when stable.\n• 720p • Low / 900p • Med: Good for mobile data or keeping the phone cooler.\n• 1080p • High / Very High: Best when connected to stable 5GHz Wi-Fi."
                                    )
                                )
                            }
                        }

                        GuideSection.CONTROLLER -> {
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.Gamepad,
                                    iconColor = XboxNeonGreen,
                                    title = config.tr("Tay cầm rời (Bluetooth / USB-C)", "Physical Controller (Bluetooth / USB-C)"),
                                    content = config.tr(
                                        "Khi bạn nối tay cầm Xbox, PlayStation hoặc tay cầm điện thoại qua Bluetooth/USB-C, trang Xbox sẽ tự nhận tín hiệu tay cầm.",
                                        "When you connect an Xbox, PlayStation, or mobile controller via Bluetooth/USB-C, the Xbox page detects it automatically."
                                    )
                                )
                            }
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.CheckCircle,
                                    iconColor = DirectCyan,
                                    title = config.tr("Tay cầm ảo trên màn hình", "On-Screen Touch Gamepad"),
                                    content = config.tr(
                                        "Bấm biểu tượng Tay cầm trên nút nổi khi đang chơi để bật/tắt nút ảo. Bạn có thể kéo đổi vị trí nút hoặc đặt lại bố cục mặc định trong mục Cài đặt.",
                                        "Tap the Controller icon on the floating menu in-game to toggle virtual buttons. You can drag buttons to reposition them or reset the default layout in Settings."
                                    )
                                )
                            }
                        }

                        GuideSection.FAQS -> {
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.Language,
                                    iconColor = JapanRed,
                                    title = config.tr("Đổi ngôn ngữ hoặc dịch menu trang Xbox", "Change language or translate Xbox menus"),
                                    content = config.tr(
                                        "Bạn có thể bật tính năng dịch các mục menu trên trang web Xbox sang tiếng Việt trong mục Cài đặt hoặc trên thanh công cụ nổi.",
                                        "You can toggle menu translation or change the display language in the Settings tab or from the floating menu."
                                    )
                                )
                            }
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.Speed,
                                    iconColor = WarningAmber,
                                    title = config.tr("Báo lỗi vùng (Region) hoặc không vào được game?", "Region error or game won't start?"),
                                    content = config.tr(
                                        "Máy chủ công cộng đôi khi bị quá tải hoặc ngừng hoạt động. Hãy sang tab 'Máy chủ' bấm Đo Ping để chọn máy chủ khác, hoặc dùng ứng dụng VPN riêng trên máy của bạn.",
                                        "Public proxy servers can go offline or get crowded. Open the 'Servers' tab to test ping and pick another server, or use your own external VPN app."
                                    )
                                )
                            }
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.Bolt,
                                    iconColor = DirectCyan,
                                    title = config.tr("Đứng hình hoặc mất tiếng khi đang chơi?", "Frozen video or missing audio?"),
                                    content = config.tr(
                                        "Mở menu nút nổi trên màn hình chơi và bấm 'Tải lại' để làm mới lại luồng phát mà không bị thoát khỏi trận đấu.",
                                        "Open the floating menu in-game and tap 'Reload' to refresh the video stream without leaving your active session."
                                    )
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
                        text = config.tr("Đã hiểu • Bắt đầu chơi 🎮", "Got It • Start Playing 🎮"),
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
