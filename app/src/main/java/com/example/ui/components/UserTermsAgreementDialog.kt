package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    var agreedToTerms by remember(isDismissible) { mutableStateOf(isDismissible) }
    val scrollState = rememberScrollState()
    val fullTermsUrl = "https://muitenvn.github.io/CloudPlay-App/terms.html"

    fun openFullTermsInBrowser() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullTermsUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(
                context,
                if (isEn) "Could not open browser: $fullTermsUrl" else "Không mở được trình duyệt: $fullTermsUrl",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

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
                .heightIn(max = 700.dp)
                .border(1.5.dp, XboxNeonGreen, RoundedCornerShape(24.dp))
                .testTag("user_terms_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = XboxSurface,
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
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
                            text = if (isEn) "Terms of Use & Privacy Policy" else "Điều Khoản Sử Dụng & Quyền Riêng Tư",
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isEn) {
                                "Mandatory user agreement required before using the app"
                            } else {
                                "Yêu cầu bắt buộc đồng ý trước khi sử dụng ứng dụng"
                            },
                            fontSize = 11.5.sp,
                            color = DirectCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable legal terms & open-source notices
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .border(1.dp, XboxCardBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    LegalSection(
                        icon = Icons.Default.Gavel,
                        title = if (isEn) "1. Non-Affiliation Disclaimer" else "1. Tuyên Bố Độc Lập",
                        content = if (isEn) {
                            "CloudPlay is an independent open-source application and is NOT affiliated with, owned by, sponsored, or authorized by Microsoft Corporation, Xbox, or Epic Games. All trademarks (Xbox, Xbox Cloud Gaming, Game Pass, Microsoft) belong to their respective owners."
                        } else {
                            "CloudPlay là ứng dụng mã nguồn mở độc lập, HOÀN TOÀN KHÔNG liên kết, không thuộc sở hữu, không được tài trợ hay ủy quyền bởi Microsoft Corporation, Xbox hay Epic Games. Mọi nhãn hiệu thuộc về chủ sở hữu tương ứng."
                        }
                    )

                    LegalSection(
                        icon = Icons.Default.Public,
                        title = if (isEn) "2. VPN, Proxy & Third-Party Public Servers" else "2. VPN, Proxy & Máy Chủ Công Cộng Bên Thứ Ba",
                        content = if (isEn) {
                            "• Using VPN/proxy or switching regions may violate third-party terms of service (Microsoft/Xbox/publishers); you decide whether to enable VPN and assume full responsibility for your account.\n• Public relay servers (VPNGate, ProxyScrape) are operated by independent third parties and may experience instability or disconnection."
                        } else {
                            "• Việc dùng VPN/proxy hoặc đổi vùng có thể vi phạm điều khoản dịch vụ của bên thứ ba (Microsoft/Xbox/nhà phát hành); bạn tự quyết định bật/tắt VPN và tự chịu trách nhiệm với tài khoản của mình.\n• Máy chủ công cộng (VPNGate, ProxyScrape) do bên thứ ba độc lập vận hành, có thể chậm hoặc ngắt kết nối."
                        }
                    )

                    LegalSection(
                        icon = Icons.Default.Lock,
                        title = if (isEn) "3. Privacy, Local Storage & IP Geolocation" else "3. Quyền Riêng Tư, Lưu Trữ Cục Bộ & Định Vị IP",
                        content = if (isEn) {
                            "• No credential collection: Sign-in occurs directly on official Microsoft pages (login.live.com, xbox.com) over HTTPS.\n• Payment isolation: Relay servers are automatically bypassed on external or payment pages.\n• Settings, bookmarks, and session history are stored locally on your device.\n• IP geolocation by DB-IP.com (CC BY 4.0)."
                        } else {
                            "• Không thu thập tài khoản/mật khẩu: Đăng nhập diễn ra trực tiếp trên trang chính thức của Microsoft (login.live.com, xbox.com) qua HTTPS.\n• Tự động ngắt máy chủ trung gian khi mở trang thanh toán hoặc trang ngoài.\n• Cài đặt và lịch sử phiên chơi chỉ lưu cục bộ trên máy bạn.\n• Nguồn định vị IP: IP geolocation by DB-IP.com (CC BY 4.0)."
                        }
                    )

                    LegalSection(
                        icon = Icons.Default.SportsEsports,
                        title = if (isEn) "4. Fair Play & User Responsibility" else "4. Chơi Công Bằng & Trách Nhiệm Người Dùng",
                        content = if (isEn) {
                            "CloudPlay contains NO cheats, NO macro/turbo fire, and NEVER modifies game memory. You are solely responsible for your own conduct and compliance with Microsoft Services Agreement and applicable laws."
                        } else {
                            "CloudPlay KHÔNG chứa gian lận (cheat), KHÔNG có macro/turbo và KHÔNG can thiệp dữ liệu game. Bạn tự chịu trách nhiệm về hành vi sử dụng của chính mình theo Thỏa thuận Dịch vụ Microsoft và pháp luật hiện hành."
                        }
                    )

                    LegalSection(
                        icon = Icons.Default.Code,
                        title = if (isEn) "5. 'AS IS' Provision & Limitation of Liability" else "5. Cung Cấp 'Như Hiện Có' & Giới Hạn Trách Nhiệm",
                        content = if (isEn) {
                            "The app is provided 'AS IS' and 'AS AVAILABLE' without warranty of continuous operation or compatibility with Microsoft/Xbox services. To the maximum extent permitted by law, the developer is not liable for indirect damages, account suspension, data/progress loss, or mobile data charges."
                        } else {
                            "Ứng dụng được cung cấp 'như hiện có' (AS IS) và 'theo khả năng hiện có', không bảo đảm hoạt động liên tục hay tương thích cố định với dịch vụ Microsoft/Xbox. Trong phạm vi pháp luật cho phép, nhà phát triển không chịu trách nhiệm về thiệt hại gián tiếp, khóa tài khoản, mất dữ liệu/tiến trình game hay cước dữ liệu di động."
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Link button to open full 15-section Terms page
                OutlinedButton(
                    onClick = { openFullTermsInBrowser() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("open_full_terms_web_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DirectCyan.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = DirectCyan.copy(alpha = 0.08f),
                        contentColor = DirectCyan
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = DirectCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEn) {
                            "Read Full 15-Section Terms of Use (Web)"
                        } else {
                            "Xem đầy đủ 15 mục Điều khoản sử dụng (Web)"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mandatory Agreement Checkbox Row (tappable across whole row)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { agreedToTerms = !agreedToTerms }
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                        .testTag("terms_checkbox_row")
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
                            "I have read, understood, and agree to the Terms of Use & Privacy Policy above. (Declining will close the app immediately)."
                        } else {
                            "Tôi đã đọc, hiểu rõ và đồng ý với Điều khoản sử dụng & Chính sách quyền riêng tư ở trên. (Nếu không đồng ý, ứng dụng sẽ đóng ngay)."
                        },
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = if (agreedToTerms) TextPrimary else TextSecondary,
                        fontWeight = if (agreedToTerms) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDismissible) XboxCardBorder else Color(0xFFFF5252).copy(alpha = 0.7f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDismissible) TextSecondary else Color(0xFFFF5252)
                        )
                    ) {
                        Text(
                            text = if (isDismissible) {
                                if (isEn) "Close" else "Đóng"
                            } else {
                                if (isEn) "Decline & Exit" else "Không đồng ý (Thoát)"
                            },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onAccept,
                        enabled = agreedToTerms,
                        modifier = Modifier
                            .weight(1.25f)
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
                            text = if (isEn) "I Agree & Enter" else "Tôi Đồng Ý",
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
