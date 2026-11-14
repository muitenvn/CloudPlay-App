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

    val gotItFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GamepadSurfaceController(
            enabled = true,
            defaultFocusRequester = gotItFocusRequester,
            autoRequestDefaultFocus = true,
            attachWindowMotionListener = true,
            onPreviousTab = {
                val idx = visibleSections.indexOf(selectedSection).coerceAtLeast(0)
                selectedSection = visibleSections[(idx - 1 + visibleSections.size) % visibleSections.size]
            },
            onNextTab = {
                val idx = visibleSections.indexOf(selectedSection).coerceAtLeast(0)
                selectedSection = visibleSections[(idx + 1) % visibleSections.size]
            },
            onBackPress = onDismiss
        ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp)
                .clip(RoundedCornerShape(22.dp))
                .testTag("how_to_use_guide_dialog"),
            color = XboxSurfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                text = config.tr("Các bước đăng nhập, kiểm tra mạng và chỉnh nút điều khiển", "Sign in, check network, and set up controls"),
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
                            .gamepadActionButton(cornerRadius = 16.dp, onClick = onDismiss)
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
                        .background(XboxSurface)
                        .padding(3.dp),
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
                                .clip(RoundedCornerShape(9.dp))
                                .gamepadClickable(
                                    cornerRadius = 9.dp,
                                    onStepHorizontal = { dir ->
                                        val curIdx = visibleSections.indexOf(selectedSection).coerceAtLeast(0)
                                        val nextIdx = (curIdx + dir).coerceIn(0, visibleSections.lastIndex)
                                        selectedSection = visibleSections[nextIdx]
                                    }
                                ) { selectedSection = section }
                                .padding(1.dp),
                            color = if (isSelected) Color(0xFF16352A) else Color.Transparent,
                            shape = RoundedCornerShape(9.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = section.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) XboxNeonGreen else TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = tabTitle,
                                    color = if (isSelected) XboxNeonGreen else TextSecondary,
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
                                    badge = config.tr("Kết nối", "Network"),
                                    title = config.tr("Chọn kết nối mạng ổn định trước khi chơi", "Choose a stable network connection"),
                                    desc = config.tr(
                                        "• Ưu tiên dùng mạng Wi-Fi 5GHz hoặc 4G/5G có sóng khỏe để hình ảnh mượt và ít trễ.\n• Nếu bạn có dùng máy chủ trung gian (VPN/Proxy tùy chọn), việc chuyển về mạng trực tiếp khi đang chơi sẽ giúp giảm độ trễ nút bấm.",
                                        "• Use a stable 5GHz Wi-Fi or strong 4G/5G signal for smooth video and responsive controls.\n• If you use an optional relay server (VPN/Proxy), switching back to direct connection during gameplay helps reduce input lag."
                                    )
                                )
                            }
                            item {
                                GuideStepCard(
                                    stepNum = "3",
                                    badge = config.tr("Menu & Điều khiển", "Menu & Controls"),
                                    title = config.tr("Cách mở Menu nhanh & quay về Trang chủ", "How to open Quick Menu & return Home"),
                                    desc = config.tr(
                                        "• Nút nổi đã được ẩn hoàn toàn để không che màn hình chơi game.\n• Trên điện thoại: Vuốt/bấm nút Quay lại (Back) của máy để mở hoặc đóng Menu nhanh (tại đây có thể bật Tay cầm ảo, đổi giao diện Cũ/Mới, hoặc bấm 'Trang chủ' để thoát).\n• Trên tay cầm / TV: Giữ đồng thời 2 nút View + Menu khoảng 0,6 giây để mở/đóng Menu nhanh.",
                                        "• The floating button is hidden for an unobstructed game screen.\n• On phone: Swipe/press the system Back button to toggle the Quick Menu (toggle virtual buttons, switch Classic/New UI, or tap 'Home' to exit).\n• On controller / TV: Hold View + Menu together for 0.6s to toggle the Quick Menu."
                                    )
                                )
                            }
                        }

                        GuideSection.ZERO_LAG, GuideSection.REVOLUTION -> {
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.Bolt,
                                    iconColor = DirectCyan,
                                    title = config.tr("Mẹo giữ kết nối mượt và giảm độ trễ", "Tips for smooth connection and low latency"),
                                    content = config.tr(
                                        "Khi truyền hình ảnh trò chơi trực tiếp qua mạng của bạn mà không đi vòng qua máy chủ trung gian, tín hiệu tay cầm và hình ảnh sẽ phản hồi nhanh hơn. Bạn có thể xem độ trễ (ping) thực tế ngay trong Menu nhanh.",
                                        "Streaming video directly over your own network without routing through an extra relay server keeps controls and video more responsive. You can check your real-time latency (ping) in the Quick Menu."
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
                                        "Khi bạn nối tay cầm Xbox, PlayStation hoặc tay cầm điện thoại qua Bluetooth/USB-C, trang Xbox sẽ tự nhận tín hiệu tay cầm. Giữ đồng thời 2 nút View + Menu khoảng 0,6 giây để mở Menu nhanh bất kỳ lúc nào.",
                                        "When you connect an Xbox, PlayStation, or mobile controller via Bluetooth/USB-C, the Xbox page detects it automatically. Hold View + Menu together for 0.6s to open the Quick Menu anytime."
                                    )
                                )
                            }
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.CheckCircle,
                                    iconColor = DirectCyan,
                                    title = config.tr("Tay cầm ảo & Mở Menu trên điện thoại", "Touch Gamepad & Opening Menu on Phone"),
                                    content = config.tr(
                                        "Vuốt/bấm nút Quay lại (Back) của điện thoại (hoặc giữ 2 nút View + Menu trên tay cầm ảo 0,6 giây) để mở Menu nhanh, bật/tắt Tay cầm ảo hoặc quay về Trang chủ. Bạn có thể kéo đổi vị trí nút trong mục Cài đặt.",
                                        "Swipe/press your phone's Back button (or hold View + Menu on the virtual gamepad for 0.6s) to open the Quick Menu, toggle virtual buttons, or return Home. You can reposition buttons in Settings."
                                    )
                                )
                            }
                        }

                        GuideSection.FAQS -> {
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.Language,
                                    iconColor = JapanRed,
                                    title = config.tr("Đổi Giao diện Cũ/Mới hoặc dịch menu trang Xbox", "Switch Classic/New UI or translate Xbox menus"),
                                    content = config.tr(
                                        "Mở Menu nhanh (vuốt nút Quay lại trên điện thoại hoặc giữ View + Menu 0,6s) sang thẻ 'Điều khiển' để chuyển tức thì giữa Giao diện cũ (xbox.com/play) và Giao diện mới (play.xbox.com), hoặc bật dịch tiếng Việt.",
                                        "Open the Quick Menu (swipe Back on phone or hold View + Menu for 0.6s) and go to 'Controls' to switch instantly between Classic UI (xbox.com/play) and New UI (play.xbox.com), or toggle menu translation."
                                    )
                                )
                            }
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.Speed,
                                    iconColor = WarningAmber,
                                    title = config.tr("Không kết nối được hoặc tải trang chậm?", "Connection error or slow loading?"),
                                    content = config.tr(
                                        "Các máy chủ công cộng miễn phí đôi khi bị đông người dùng hoặc tạm ngưng. Bạn có thể sang mục 'Máy chủ' bấm Đo Ping để kiểm tra độ trễ, hoặc chuyển sang mạng trực tiếp / kết nối riêng của bạn.",
                                        "Free public servers can sometimes get busy or go offline. Open the 'Servers' tab to test ping and check latency, or switch to your direct connection."
                                    )
                                )
                            }
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.Bolt,
                                    iconColor = DirectCyan,
                                    title = config.tr("Đứng hình, mất tiếng hoặc muốn thoát về Trang chủ?", "Frozen video, missing audio, or returning Home?"),
                                    content = config.tr(
                                        "Vuốt nút Quay lại (Back) trên điện thoại hoặc giữ View + Menu (0,6s) để mở Menu nhanh, sau đó bấm 'Tải lại' để làm mới luồng phát hoặc bấm 'Trang chủ' để thoát ra ngoài.",
                                        "Swipe Back on phone or hold View + Menu (0.6s) to open the Quick Menu, then tap 'Reload' to refresh the stream or 'Home' to exit to the main screen."
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
                        .height(46.dp)
                        .gamepadActionButton(
                            cornerRadius = 12.dp,
                            focusRequester = gotItFocusRequester,
                            onClick = onDismiss
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = XboxNeonGreen,
                        contentColor = Color(0xFF03140B)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = config.tr("Đã hiểu", "Got It"),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF03140B)
                    )
                }
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
            .border(1.dp, XboxCardBorder, RoundedCornerShape(14.dp))
            .gamepadClickable(cornerRadius = 14.dp) {},
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
            .border(1.dp, XboxCardBorder, RoundedCornerShape(14.dp))
            .gamepadClickable(cornerRadius = 14.dp) {},
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
