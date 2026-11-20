package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.example.data.SettingsRepository
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JapanRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface

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
    val config by SettingsRepository.config.collectAsState()
    val visibleSections = remember {
        listOf(
            GuideSection.QUICK_START,
            GuideSection.ZERO_LAG,
            GuideSection.CONTROLLER,
            GuideSection.FAQS
        )
    }

    val gotItFocusRequester = remember { FocusRequester() }
    val closeIconFocusRequester = remember { FocusRequester() }

    AdaptivePopupFrame(
        onDismissRequest = onDismiss,
        dialogTestTag = "how_to_use_guide_dialog",
        defaultFocusRequester = gotItFocusRequester,
        wideWidthFraction = 0.94f,
        portraitWidthFraction = 0.94f,
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
        fun sectionLabel(section: GuideSection): String = when (section) {
            GuideSection.QUICK_START -> config.tr("Bắt Đầu", "Start")
            GuideSection.ZERO_LAG -> config.tr("Mạng & Hình", "Quality")
            GuideSection.CONTROLLER -> config.tr("Tay Cầm", "Controls")
            GuideSection.FAQS -> config.tr("Sửa Lỗi", "Help")
            GuideSection.REVOLUTION -> config.tr("Tối Ưu", "Optimize")
        }

        @Composable
        fun HeaderSection() {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(gapMed),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(iconBoxSize)
                            .clip(RoundedCornerShape(btnCorner))
                            .background(DirectCyan.copy(alpha = 0.15f))
                            .border(borderThin, DirectCyan.copy(alpha = 0.45f), RoundedCornerShape(btnCorner)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = DirectCyan,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = config.tr("CÁCH DÙNG CƠ BẢN", "HOW TO USE"),
                            color = TextPrimary,
                            fontSize = rSp(15f),
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = config.tr(
                                "Các bước đăng nhập, kiểm tra mạng và chỉnh nút điều khiển",
                                "Sign in, check network, and set up controls"
                            ),
                            color = XboxNeonGreen,
                            fontSize = rSp(10.5f),
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = XboxSurface,
                    border = BorderStroke(borderThin, XboxCardBorder),
                    modifier = Modifier
                        .size(rDp(30f))
                        .clip(CircleShape)
                        .gamepadClickable(
                            cornerRadius = rDp(15f),
                            focusRequester = closeIconFocusRequester,
                            onClick = onDismiss
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = config.tr("Đóng", "Close"),
                            tint = TextSecondary,
                            modifier = Modifier.size(rDp(16f))
                        )
                    }
                }
            }
        }

        @Composable
        fun GotItActionButton() {
            AdaptiveFlatActionButton(
                onClick = onDismiss,
                isPrimary = true,
                focusRequester = gotItFocusRequester,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = config.tr("Đã hiểu", "Got It"),
                    fontSize = rSp(12.5f),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF04150B),
                    maxLines = 1
                )
            }
        }

        @Composable
        fun GuideContentList(maxListHeight: Dp) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = rDp(180f), max = maxListHeight),
                verticalArrangement = Arrangement.spacedBy(gapMed)
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
                                title = config.tr("Cách mở Menu nhanh trong game", "How to open in-game Quick Menu"),
                                desc = config.tr(
                                    "• Giao diện thường (Điện thoại): Bấm vào nút nổi nhỏ ở góc màn hình (hoặc vuốt nút Quay lại) để mở Menu nhanh.\n• Giao diện TV: Nút nổi được tắt hẳn để không che màn hình chơi game. Hãy giữ đồng thời 2 nút View + Menu khoảng 0,6 giây trên tay cầm (hoặc bấm nút Quay lại) để mở/đóng Menu nhanh.",
                                    "• Normal UI (Phone): Tap the small floating button at the corner of the screen (or swipe Back) to open the Quick Menu.\n• TV UI: The floating button is turned off so it never blocks gameplay. Hold View + Menu together for 0.6s on your controller (or press Back) to toggle the Quick Menu."
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
                                    "Khi truyền hình ảnh trò chơi trực tiếp qua mạng của bạn mà không đi vòng qua máy chủ trung gian, tín hiệu tay cầm và hình ảnh sẽ phản hồi nhanh hơn. Bạn có thể xem độ trễ (ping) thực tế ngay trên viên thuốc ở góc màn hình.",
                                    "Streaming video directly over your own network without routing through an extra relay server keeps controls and video more responsive. You can check your real-time latency (ping) on the corner pill."
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
                                title = config.tr("Tay cầm ảo trên màn hình", "On-Screen Touch Gamepad"),
                                content = config.tr(
                                    "Bấm vào nút nổi ở góc màn hình để bật hoặc tắt các nút bấm ảo. Bạn có thể chọn bố cục sẵn có hoặc kéo đổi vị trí nút trong mục Cài đặt.",
                                    "Tap the floating button at the corner of the screen to show or hide virtual buttons. You can choose a preset layout or reposition buttons in Settings."
                                )
                            )
                        }
                    }

                    GuideSection.FAQS -> {
                        item {
                            GuideDetailCard(
                                icon = Icons.Default.Language,
                                iconColor = JapanRed,
                                title = config.tr("Dịch tiếng Việt cho menu trang Xbox", "Translate Xbox web menus"),
                                content = config.tr(
                                    "Bấm nút nổi ở góc màn hình (hoặc giữ View + Menu 0,6s trên TV) -> sang thẻ 'Điều khiển' để bật dịch các nút và thông báo của trang Xbox sang tiếng Việt.",
                                    "Tap the corner floating button (or hold View + Menu 0.6s on TV) -> go to 'Controls' to translate Xbox web menus and prompts."
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
        }

        AdaptiveTwoColumnLayout(
            leftWeight = 0.80f,
            rightWeight = 1.20f,
            leftColumn = {
                HeaderSection()
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(gapSmall)
                ) {
                    visibleSections.forEach { section ->
                        val isSelected = selectedSection == section
                        AdaptiveFlatActionButton(
                            onClick = { selectedSection = section },
                            isPrimary = isSelected,
                            containerColor = if (isSelected) Color(0xFF16352A) else XboxSurface,
                            contentColor = if (isSelected) XboxNeonGreen else TextPrimary,
                            borderColor = if (isSelected) XboxNeonGreen else XboxCardBorder,
                            focusedBorderColor = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(gapSmall)
                            ) {
                                Icon(
                                    imageVector = section.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) XboxNeonGreen else TextSecondary,
                                    modifier = Modifier.size(rDp(15f))
                                )
                                Text(
                                    text = sectionLabel(section),
                                    color = if (isSelected) XboxNeonGreen else TextPrimary,
                                    fontSize = rSp(11.5f),
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                GotItActionButton()
            },
            rightColumn = {
                GuideContentList(maxListHeight = winH * 0.78f)
            },
            portraitColumn = {
                HeaderSection()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(sectionCorner))
                        .background(XboxSurface)
                        .padding(rDp(3f)),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    visibleSections.forEach { section ->
                        val isSelected = selectedSection == section
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(btnCorner))
                                .gamepadClickable(
                                    cornerRadius = btnCorner,
                                    onStepHorizontal = { dir ->
                                        val curIdx = visibleSections.indexOf(selectedSection).coerceAtLeast(0)
                                        val nextIdx = (curIdx + dir).coerceIn(0, visibleSections.lastIndex)
                                        selectedSection = visibleSections[nextIdx]
                                    }
                                ) { selectedSection = section }
                                .padding(rDp(1f)),
                            color = if (isSelected) Color(0xFF16352A) else Color.Transparent,
                            shape = RoundedCornerShape(btnCorner)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = rDp(7f), horizontal = rDp(2f)),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(rDp(2f))
                            ) {
                                Icon(
                                    imageVector = section.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) XboxNeonGreen else TextSecondary,
                                    modifier = Modifier.size(rDp(15f))
                                )
                                Text(
                                    text = sectionLabel(section),
                                    color = if (isSelected) XboxNeonGreen else TextSecondary,
                                    fontSize = rSp(10f),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                GuideContentList(maxListHeight = winH * 0.48f)
                GotItActionButton()
            }
        )
    }
}

@Composable
private fun AdaptivePopupScope.GuideStepCard(
    stepNum: String,
    badge: String,
    title: String,
    desc: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(sectionCorner))
            .border(borderThin, XboxCardBorder, RoundedCornerShape(sectionCorner))
            .gamepadClickable(cornerRadius = sectionCorner) {},
        colors = CardDefaults.cardColors(containerColor = XboxSurface)
    ) {
        Column(
            modifier = Modifier.padding(padInner),
            verticalArrangement = Arrangement.spacedBy(gapSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(gapSmall),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(rDp(22f))
                            .clip(CircleShape)
                            .background(XboxNeonGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stepNum,
                            color = XboxNeonGreen,
                            fontSize = rSp(11.5f),
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = rSp(12.5f),
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(rDp(6f)),
                    color = DirectCyan.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badge,
                        color = DirectCyan,
                        fontSize = rSp(9f),
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = rDp(6f), vertical = rDp(2f))
                    )
                }
            }

            Text(
                text = desc,
                color = TextSecondary,
                fontSize = rSp(11f),
                lineHeight = rSp(15.5f)
            )
        }
    }
}

@Composable
private fun AdaptivePopupScope.GuideDetailCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    content: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(sectionCorner))
            .border(borderThin, XboxCardBorder, RoundedCornerShape(sectionCorner))
            .gamepadClickable(cornerRadius = sectionCorner) {},
        colors = CardDefaults.cardColors(containerColor = XboxSurface)
    ) {
        Row(
            modifier = Modifier.padding(padInner),
            horizontalArrangement = Arrangement.spacedBy(gapMed)
        ) {
            Box(
                modifier = Modifier
                    .size(rDp(30f))
                    .clip(RoundedCornerShape(chipCorner))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(rDp(16f))
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(rDp(3f))
            ) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = rSp(12.5f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = content,
                    color = TextSecondary,
                    fontSize = rSp(11f),
                    lineHeight = rSp(15.5f)
                )
            }
        }
    }
}
