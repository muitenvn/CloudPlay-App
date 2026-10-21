package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
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
    REVOLUTION("5 Tầng Tối Ưu", Icons.Default.AutoAwesome),
    ZERO_LAG("Giảm Ping 0ms", Icons.Default.Speed),
    CONTROLLER("Tay Cầm", Icons.Default.Gamepad),
    FAQS("Khắc Phục Lỗi", Icons.Default.Lightbulb)
}

@Composable
fun HowToUseGuideDialog(
    initialSection: GuideSection = GuideSection.QUICK_START,
    onDismiss: () -> Unit
) {
    var selectedSection by remember { mutableStateOf(initialSection) }
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isVi = !config.isEnglishUI()

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
                                text = if (isVi) "HƯỚNG DẪN CÁCH DÙNG" else "HOW TO PLAY GUIDE",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = if (isVi) "Cẩm nang chơi Xbox Cloud 0ms lag tại Việt Nam" else "Zero-latency Xbox Cloud Gaming handbook",
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
                    GuideSection.entries.forEach { section ->
                        val isSelected = selectedSection == section
                        val tabTitle = when (section) {
                            GuideSection.QUICK_START -> if (isVi) "Bắt Đầu" else "Start"
                            GuideSection.REVOLUTION -> if (isVi) "5 Tầng" else "5 Layers"
                            GuideSection.ZERO_LAG -> "0ms Ping"
                            GuideSection.CONTROLLER -> if (isVi) "Tay Cầm" else "Gamepad"
                            GuideSection.FAQS -> if (isVi) "Sửa Lỗi" else "FAQs"
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
                        .heightIn(min = 280.dp, max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (selectedSection) {
                        GuideSection.QUICK_START -> {
                            item {
                                GuideStepCard(
                                    stepNum = "1",
                                    badge = if (isVi) "Tài khoản" else "Account",
                                    title = if (isVi) "Chuẩn bị tài khoản chơi game" else "Prepare your gaming account",
                                    desc = if (isVi) "• Nếu có tài khoản Game Pass Ultimate: Bạn có thể chơi hàng trăm game AAA như Forza Horizon 5, Starfield, GTA V...\n• Nếu CHƯA có Game Pass: FORTNITE hoàn toàn MIỄN PHÍ! Đăng nhập tài khoản Microsoft thường là chơi được ngay." else "• With Xbox Game Pass Ultimate: Play hundreds of games like Forza, Starfield, and GTA V.\n• No Game Pass? FORTNITE is 100% FREE! Just log in with a free Microsoft account."
                                )
                            }
                            item {
                                GuideStepCard(
                                    stepNum = "2",
                                    badge = if (isVi) "Vượt rào" else "Bypass",
                                    title = if (isVi) "Bấm 'CHƠI XBOX CLOUD' hoặc chọn game" else "Tap 'PLAY XBOX CLOUD' or select game",
                                    desc = if (isVi) "• Bạn không cần tự bật VPN thủ công!\n• Hệ thống Smart VPN sẽ tự động kết nối IP Nhật Bản để Microsoft cấp quyền truy cập phòng chờ game (vượt qua kiểm tra vùng)." else "• No need to manually toggle VPN!\n• Smart VPN automatically routes initial handshake via Japan nodes to bypass geo-restrictions."
                                )
                            }
                            item {
                                GuideStepCard(
                                    stepNum = "3",
                                    badge = "0ms Lag",
                                    title = if (isVi) "Tự động tắt VPN khi vào trận" else "Auto-disconnect VPN once stream connects",
                                    desc = if (isVi) "• Ngay khi luồng video WebRTC bắt đầu, hệ thống TỰ ĐỘNG NGẮT VPN và chuyển về mạng nhà trực tiếp (Direct ISP).\n• Trang web KHÔNG reload, luồng game giữ nguyên 100%, Ping tụt từ ~140ms xuống chỉ còn 25-35ms cực mượt!" else "• Once WebRTC stream launches, VPN is instantly dropped without page reload.\n• Latency plummets from 140ms to 25-35ms direct esports-grade speed!"
                                )
                            }
                            item {
                                GuideStepCard(
                                    stepNum = "4",
                                    badge = if (isVi) "Thanh HUD" else "HUD Dock",
                                    title = if (isVi) "Dùng thanh điều khiển nổi (Floating HUD)" else "Floating Telemetry HUD controls",
                                    desc = if (isVi) "• Thanh HUD tròn nổi trên màn hình cho phép: Chuyển mạng tức thì, bật tay cầm ảo, bật Clarity Boost và quay về Trang Chủ." else "• In-game floating pill lets you toggle bypass, turn on virtual controller, boost clarity and return home anytime."
                                )
                            }
                        }

                        GuideSection.REVOLUTION -> {
                            item {
                                GuideStepCard(
                                    stepNum = "1",
                                    badge = if (isVi) "Tầng 1 • Vượt Rào" else "Layer 1 • Bypass",
                                    title = "Stealth Header Spoofing",
                                    desc = if (isVi) {
                                        "Ứng dụng ngụy trang toàn bộ Header mạng HTTP như thể đang kết nối trực tiếp từ Tokyo. Microsoft tự động nhận diện bạn thuộc vùng hỗ trợ Xbox Cloud Gaming mà không cần VPN nặng nề."
                                    } else {
                                        "Disguises HTTP network headers as if connecting directly from supported regions (e.g., Tokyo), passing Microsoft region checks without heavy VPN overhead."
                                    }
                                )
                            }
                            item {
                                GuideStepCard(
                                    stepNum = "2",
                                    badge = if (isVi) "Tầng 2 • Tự Lành" else "Layer 2 • Self-Healing",
                                    title = "Mesh Proxy Auto-Healing",
                                    desc = if (isVi) {
                                        "Mạng lưới proxy phân tán tự động đo ping mỗi giây. Nếu proxy hiện tại bị nghẽn (>150ms) hoặc rớt mạng, hệ thống chuyển sang node dự phòng trong vòng 300ms mà không làm đứt kết nối."
                                    } else {
                                        "Distributed proxy mesh continuously measures latency and region unlock status, automatically failing over to the fastest verified node if a server goes offline."
                                    }
                                )
                            }
                            item {
                                GuideStepCard(
                                    stepNum = "3",
                                    badge = if (isVi) "Tầng 3 • 1080p 60FPS" else "Layer 3 • 1080p 60FPS",
                                    title = "WebRTC SDP Bitrate Booster",
                                    desc = if (isVi) {
                                        "Can thiệp trực tiếp vào giao thức bắt tay WebRTC SDP của trình duyệt, ép mở trần băng thông video lên 15Mbps Full HD 60FPS mượt mà cùng âm thanh vòm Stereo 128kbps."
                                    } else {
                                        "Optimizes the browser WebRTC SDP handshake to unlock high-bitrate 1080p 60FPS streaming (or lightweight H.264 5Mbps in Low-End Device Mode) with crisp stereo audio."
                                    }
                                )
                            }
                            item {
                                GuideStepCard(
                                    stepNum = "4",
                                    badge = if (isVi) "Tầng 4 • 0ms VPN Lag" else "Layer 4 • 0ms VPN Lag",
                                    title = "Zero-Reload Direct Handshake",
                                    desc = if (isVi) {
                                        "Kỹ thuật đột phá ngắt VPN và chuyển luồng WebRTC sang đường truyền Internet trực tiếp của nhà mạng Việt Nam (Viettel, VNPT, FPT) mà không cần tải lại trang. Ping về 25-35ms chuẩn esports."
                                    } else {
                                        "Disconnects the VPN tunnel as soon as the WebRTC stream connects, switching media packets to your direct ISP connection without reloading the page."
                                    }
                                )
                            }
                            item {
                                GuideStepCard(
                                    stepNum = "5",
                                    badge = if (isVi) "Tầng 5 • Ngôn Ngữ" else "Layer 5 • Locale",
                                    title = "Anti-Japanese Enforcer",
                                    desc = if (isVi) {
                                        "Chặn đứng tình trạng Xbox tự động ép ngôn ngữ tiếng Nhật khi dùng server Tokyo. Toàn bộ giao diện trò chơi và phụ đề được ép theo ngôn ngữ bạn chọn (Tiếng Anh en-US hoặc tiếng máy)."
                                    } else {
                                        "Prevents Xbox from forcing Japanese menus when routing via Tokyo servers. Game UI and subtitles stay locked to your chosen locale (e.g., English en-US)."
                                    }
                                )
                            }
                        }

                        GuideSection.ZERO_LAG -> {
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.Bolt,
                                    iconColor = DirectCyan,
                                    title = if (isVi) "Cơ chế Zero-Reload Direct Network" else "Zero-Reload Direct Network Mechanism",
                                    content = if (isVi) {
                                        "Bình thường nếu chơi qua VPN, toàn bộ dữ liệu game nặng phải chạy vòng qua máy chủ Tokyo khiến ping cao (~140ms) và giật lag. Ứng dụng này chỉ dùng VPN lúc bấm nút Play để vượt qua bước duyệt IP của Microsoft, sau đó ngắt kết nối VPN để luồng video truyền trực tiếp về điện thoại bạn."
                                    } else {
                                        "Normally, playing over a VPN forces all heavy video traffic through a remote relay, causing high ping (~140ms). CloudPlay only uses the proxy/VPN when clicking Play to pass Microsoft's region check, then switches to your direct ISP connection for ultra-low latency."
                                    }
                                )
                            }
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.Wifi,
                                    iconColor = XboxNeonGreen,
                                    title = if (isVi) "Chất lượng Đồ họa & Hiệu năng máy" else "Graphics Quality & Device Performance",
                                    content = if (isVi) {
                                        "• Tự Động (Khuyên dùng): Tự điều chỉnh đồ họa theo điều kiện mạng (Wi-Fi / 4G) và giảm tải GPU khi máy nóng để giữ 60 FPS mượt mà.\n• Thấp (Tiết Kiệm Dung Lượng - 5M): Tiết kiệm 60% dung lượng (~2.2GB/giờ), giải mã H.264 Baseline <4ms siêu mát máy và đỡ hao pin.\n• Trung Bình (9M) / Cao (14M) / Rất Cao (20M): Tùy chọn 4 mức độ từ cân bằng mát máy đến bung tối đa đồ họa 1080p60 sắc nét."
                                    } else {
                                        "• Auto (Recommended): Dynamically tunes bitrate for Wi-Fi / 4G and throttles GPU load if the device warms up.\n• Low (Data Saver - 5M) & Low-End Mode: Saves 60% data (~2.2GB/hr) with <4ms H.264 Baseline hardware decode.\n• Medium (9M) / High (14M) / Ultra (20M): Choose between cool balanced play and maximum 1080p60 clarity."
                                    }
                                )
                            }
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.Speed,
                                    iconColor = WarningAmber,
                                    title = if (isVi) "Bật bộ lọc Clarity Boost (Làm Nét)" else "Enable Clarity Boost Filter",
                                    content = if (isVi) {
                                        "Vào tab Cài Đặt hoặc mở thanh HUD trong game -> Bật 'Clarity Boost'. Bộ lọc thông minh sẽ tăng độ tương phản và độ sắc nét giúp hình ảnh 1080p sắc sảo như đang chơi trên máy console thật."
                                    } else {
                                        "Open Settings or the in-game Floating HUD -> Turn on 'Clarity Boost' to sharpen edges and enhance contrast for a crisp console-like image."
                                    }
                                )
                            }
                        }

                        GuideSection.CONTROLLER -> {
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.Gamepad,
                                    iconColor = XboxNeonGreen,
                                    title = if (isVi) "Kết nối tay cầm Bluetooth / Type-C" else "Connect Bluetooth / USB-C Controller",
                                    content = if (isVi) {
                                        "• Hỗ trợ 100%: Tay cầm Xbox One/Series, PS4 DualShock 4, PS5 DualSense, Gamesir, Flydigi, EasySMX...\n• Bật Bluetooth trên điện thoại -> Ghép đôi tay cầm -> Đèn báo tay cầm trên ứng dụng sẽ chuyển màu Xanh lá cây báo hiệu sẵn sàng."
                                    } else {
                                        "• 100% compatible with Xbox One/Series, PS4 DualShock 4, PS5 DualSense, Gamesir, Flydigi, Backbone, and USB-C gamepads.\n• Pair via Bluetooth or plug in -> The controller status indicator turns Neon Green when ready."
                                    }
                                )
                            }
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.CheckCircle,
                                    iconColor = DirectCyan,
                                    title = if (isVi) "Chơi bằng Tay Cầm Ảo trên màn hình" else "Play with On-Screen Virtual Touch Gamepad",
                                    content = if (isVi) {
                                        "• Nếu không có tay cầm vật lý, vào Cài Đặt -> Bật công tắc 'Tay cầm ảo trên màn hình'.\n• Hệ thống sẽ hiển thị cần xoay Analog 360°, cụm nút ABXY, D-Pad và các nút cò LT/RT/LB/RB với độ phản hồi rung haptic chân thực."
                                    } else {
                                        "• No physical controller? Toggle the gamepad icon on the Floating HUD or enable 'On-Screen Virtual Touch Controller' in Settings.\n• Provides 360° dual thumbsticks, ABXY, D-Pad, and LT/RT/LB/RB triggers with haptic feedback."
                                    }
                                )
                            }
                        }

                        GuideSection.FAQS -> {
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.Language,
                                    iconColor = JapanRed,
                                    title = if (isVi) "Bị ép hiển thị tiếng Nhật thì làm sao?" else "What if Xbox displays in Japanese?",
                                    content = if (isVi) {
                                        "Mở tab 'Cài Đặt' -> Tại mục Ngôn Ngữ, chọn 'Tiếng Anh (en-US)' hoặc ngôn ngữ máy -> Bấm 'Áp dụng'. Sau đó kéo xuống dưới cùng bấm 'Xóa Cookie & Đăng Nhập Lại'. Giao diện sẽ vĩnh viễn chuyển sang tiếng Anh dễ đọc!"
                                    } else {
                                        "Open 'Settings' -> Select 'English (en-US)' under Xbox Web Language -> Tap 'Apply Code', or scroll down and tap 'Clear Cookies & Re-login to Xbox'."
                                    }
                                )
                            }
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.Speed,
                                    iconColor = WarningAmber,
                                    title = if (isVi) "Máy chủ phản hồi chậm hoặc báo lỗi vùng?" else "Slow server or 'Unavailable in your region'?",
                                    content = if (isVi) {
                                        "Chuyển sang tab 'Máy Chủ' -> Bấm nút 'Kiểm Tra Ping' hoặc 'Tải Proxy Sống' để cập nhật danh sách máy chủ mới nhất. Bạn cũng có thể bấm 'Đổi Server Dự Phòng' ngay trên Trang Chủ."
                                    } else {
                                        "Go to the 'Servers' tab -> Select a server with the green 'Unlocked' badge, or tap 'Test All Ping' / 'Fetch Live' to refresh live proxies."
                                    }
                                )
                            }
                            item {
                                GuideDetailCard(
                                    icon = Icons.Default.Bolt,
                                    iconColor = DirectCyan,
                                    title = if (isVi) "Game bị đứng hình hoặc màn hình đen?" else "Frozen frame or black screen?",
                                    content = if (isVi) {
                                        "Chạm vào thanh HUD nổi trên màn hình -> Nhấn biểu tượng 🔄 'Tải Lại Trang'. Hệ thống sẽ tự động bắt tay lại với máy chủ Xbox mà không cần thoát ứng dụng."
                                    } else {
                                        "Expand the Floating HUD -> Tap 'Reload'. The browser will re-establish the Xbox stream session without losing your progress."
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
                        text = if (isVi) "Đã Hiểu • Bắt Đầu Chơi Ngay 🎮" else "Got It • Play Now 🎮",
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
