package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface

@Composable
fun UserTermsAgreementDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    isDismissible: Boolean = false,
    onDismissRequest: () -> Unit = onDecline
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    var agreedToTerms by remember { mutableStateOf(!isDismissible.not()) }
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = {
            if (isDismissible) onDismissRequest()
        },
        properties = DialogProperties(
            dismissOnBackPress = isDismissible,
            dismissOnClickOutside = isDismissible,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .border(1.5.dp, XboxNeonGreen, RoundedCornerShape(24.dp))
                .testTag("user_terms_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = XboxSurface,
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
            ) {
                // Header with Shield & Badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(XboxNeonGreen.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = XboxNeonGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEn) "Terms of Use & Disclaimer" else "Điều Khoản & Miễn Trừ Trách Nhiệm",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isEn) "User Agreement & Open Source License" else "Thỏa thuận người dùng & Giấy phép nguồn mở",
                            fontSize = 12.sp,
                            color = DirectCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable legal terms & open-source notices
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .border(1.dp, XboxCardBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LegalSection(
                        icon = Icons.Default.SportsEsports,
                        title = if (isEn) "1. Purpose & Fair Play Commitment" else "1. Mục Đích Sử Dụng & Cam Kết Chơi Công Bằng",
                        content = if (isEn) {
                            "CloudPlay is an independent, non-profit open-source utility designed to help reduce connection delay and improve image clarity for cloud gaming. The app strictly adheres to Fair Play: it contains NO cheats, NO macro/turbo fire, and NEVER modifies game files or memory."
                        } else {
                            "CloudPlay là tiện ích mã nguồn mở độc lập, phi lợi nhuận giúp giảm độ trễ kết nối và cải thiện độ nét hình ảnh khi chơi game đám mây. Ứng dụng tuân thủ tuyệt đối nguyên tắc Chơi công bằng (Fair Play): KHÔNG chứa tính năng gian lận, KHÔNG có bắn liên thanh (macro/turbo) và KHÔNG can thiệp dữ liệu trò chơi."
                        }
                    )

                    LegalSection(
                        icon = Icons.Default.Lock,
                        title = if (isEn) "2. Account Security, Payment Isolation & Local Data" else "2. Bảo Mật Tài Khoản, Thanh Toán & Dữ Liệu Cục Bộ",
                        content = if (isEn) {
                            "• No credential collection: Sign-in takes place directly on official Microsoft pages (login.live.com, xbox.com) over HTTPS.\n• Payment protection: Relay servers are automatically paused when navigating to external or payment pages.\n• 100% local storage: Settings, game bookmarks, and play session history are stored only on your device and can be deleted anytime."
                        } else {
                            "• Không thu thập tài khoản: Quá trình đăng nhập diễn ra trực tiếp trên trang chính thức của Microsoft (login.live.com, xbox.com) qua kết nối mã hóa HTTPS.\n• Bảo vệ giao dịch: Tự động ngắt máy chủ trung gian khi mở trang thanh toán hoặc trang bên ngoài.\n• Lưu trữ cục bộ 100%: Cài đặt, danh sách game và lịch sử phiên chơi chỉ lưu trên điện thoại của bạn, có thể xóa bất cứ lúc nào."
                        }
                    )

                    LegalSection(
                        icon = Icons.Default.Gavel,
                        title = if (isEn) "3. Microsoft & Xbox Non-Affiliation Disclaimer" else "3. Tuyên Bố Độc Lập Với Microsoft & Xbox",
                        content = if (isEn) {
                            "This application is NOT affiliated with, owned by, sponsored, authorized, or endorsed by Microsoft Corporation or Xbox. All trademarks, logos, and brand names (Xbox, Xbox Cloud Gaming, Game Pass, Microsoft) belong to Microsoft Corporation."
                        } else {
                            "Ứng dụng HOÀN TOÀN KHÔNG liên kết, không thuộc sở hữu, không được tài trợ hay ủy quyền bởi Microsoft Corporation hoặc Xbox. Mọi nhãn hiệu, biểu trưng và tên gọi (Xbox, Xbox Cloud Gaming, Game Pass, Microsoft) thuộc quyền sở hữu của Microsoft Corporation."
                        }
                    )

                    LegalSection(
                        icon = Icons.Default.Code,
                        title = if (isEn) "4. Open-Source License (MIT License)" else "4. Giấy Phép Mã Nguồn Mở (MIT License)",
                        content = if (isEn) {
                            "This app incorporates stream display and touch control improvements inspired by the open-source project Better-xCloud (https://github.com/redphx/better-xcloud) by redphx under the MIT License (provided 'AS IS', without warranty of any kind)."
                        } else {
                            "Ứng dụng kế thừa một số giải pháp hiển thị và điều khiển cảm ứng từ dự án mã nguồn mở cộng đồng Better-xCloud (https://github.com/redphx/better-xcloud) của tác giả redphx theo giấy phép MIT License (cung cấp 'NGUYÊN TRẠNG', không kèm bảo đảm dưới bất kỳ hình thức nào)."
                        }
                    )

                    LegalSection(
                        icon = Icons.Default.Person,
                        title = if (isEn) "5. User Responsibility" else "5. Trách Nhiệm Người Dùng",
                        content = if (isEn) {
                            "You are responsible for using your personal account in accordance with the Microsoft Services Agreement, game publisher rules, and applicable local regulations."
                        } else {
                            "Bạn tự chịu trách nhiệm khi sử dụng tài khoản cá nhân và cam kết tuân thủ Thỏa thuận Dịch vụ của Microsoft, quy tắc của nhà phát hành trò chơi cũng như quy định pháp luật hiện hành."
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mandatory Agreement Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = agreedToTerms,
                        onCheckedChange = { agreedToTerms = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = XboxNeonGreen,
                            uncheckedColor = TextSecondary,
                            checkmarkColor = Color.Black
                        ),
                        modifier = Modifier.testTag("terms_checkbox")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEn) {
                            "I have read, understood the security & privacy notice, and agree to the terms above."
                        } else {
                            "Tôi đã đọc, hiểu rõ chính sách bảo mật & miễn trừ và đồng ý với các điều khoản trên."
                        },
                        fontSize = 12.sp,
                        color = if (agreedToTerms) TextPrimary else TextSecondary,
                        fontWeight = if (agreedToTerms) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("decline_terms_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDismissible) TextSecondary else Color(0xFFFF5252)
                        )
                    ) {
                        Text(
                            text = if (isDismissible) {
                                if (isEn) "Close" else "Đóng"
                            } else {
                                if (isEn) "Decline & Exit" else "Từ chối & Thoát"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onAccept,
                        enabled = agreedToTerms,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("accept_terms_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = XboxNeonGreen,
                            contentColor = Color.Black,
                            disabledContainerColor = Color.DarkGray,
                            disabledContentColor = Color.LightGray
                        )
                    ) {
                        Text(
                            text = if (isEn) "I Agree" else "Tôi Đồng Ý",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalSection(
    icon: ImageVector,
    title: String,
    content: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DirectCyan,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        Text(
            text = content,
            fontSize = 11.5.sp,
            color = TextSecondary,
            lineHeight = 17.sp,
            fontFamily = FontFamily.Default
        )
    }
}
