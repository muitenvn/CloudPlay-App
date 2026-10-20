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
                            text = "Điều Khoản & Miễn Trừ Trách Nhiệm",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Thỏa thuận người dùng & Giấy phép nguồn mở",
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
                        title = "1. Mục Đích Sử Dụng & Tính Chất Độc Lập",
                        content = "CloudPlay là tiện ích công cụ mã nguồn mở độc lập, phi lợi nhuận. Ứng dụng cung cấp môi trường trình duyệt WebRTC tối ưu hóa phần cứng và tiện ích định tuyến mạng cá nhân nhằm hỗ trợ cải thiện độ trễ và chất lượng đường truyền khi trải nghiệm chơi game đám mây cá nhân."
                    )

                    LegalSection(
                        icon = Icons.Default.Gavel,
                        title = "2. Tuyên Bố Miễn Trừ Trách Nhiệm Với Microsoft & Xbox",
                        content = "Ứng dụng HOÀN TOÀN KHÔNG liên kết, không thuộc quyền sở hữu, không được tài trợ, ủy quyền hay chứng thực bởi Microsoft Corporation, Xbox, Xbox Game Pass hay bất kỳ công ty con nào. Mọi nhãn hiệu, biểu trưng và tên thương mại liên quan (Xbox, Xbox Cloud Gaming, Microsoft) thuộc quyền sở hữu độc quyền của Microsoft Corporation."
                    )

                    LegalSection(
                        icon = Icons.Default.Code,
                        title = "3. Tuân Thủ Giấy Phép Mã Nguồn Mở GitHub (MIT License)",
                        content = "Ứng dụng trích dẫn và tích hợp các giải pháp tối ưu hóa stream WebRTC, Clarity Boost và vượt kiểm tra khu vực dựa trên dự án cộng đồng Better-xCloud (https://github.com/redphx/better-xcloud) của tác giả redphx theo giấy phép mã nguồn mở MIT License:\n\n\"Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files... THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED...\""
                    )

                    LegalSection(
                        icon = Icons.Default.Lock,
                        title = "4. Bảo Mật Tuyệt Đối & Quyền Riêng Tư",
                        content = "Ứng dụng cam kết tuyệt đối không thu thập, không lưu trữ và không gửi thông tin đăng nhập, tài khoản, mật khẩu Microsoft hay bất kỳ dữ liệu cá nhân nào của bạn ra máy chủ bên ngoài. Mọi thao tác xác thực diễn ra hoàn toàn trực tiếp giữa thiết bị của bạn và hệ thống chính thức của Microsoft (login.live.com) qua cơ chế WebView bảo mật."
                    )

                    LegalSection(
                        icon = Icons.Default.Person,
                        title = "5. Trách Nhiệm Người Dùng",
                        content = "Bạn tự chịu trách nhiệm về tài khoản Xbox Game Pass cá nhân và cam kết tuân thủ Thỏa thuận Dịch vụ của Microsoft (Microsoft Services Agreement). Ứng dụng chỉ đóng vai trò công cụ hỗ trợ trải nghiệm duyệt web kỹ thuật số cho người dùng cuối."
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
                        text = "Tôi đã đọc, hiểu rõ tuyên bố miễn trừ và đồng ý với các điều khoản trên.",
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
                    ) {
                        Text("Từ chối & Thoát", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
                        Text("Tôi Đồng Ý", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
