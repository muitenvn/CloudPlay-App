package com.example.ui.tv

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cloudplay.RemotePadTvServer
import com.example.cloudplay.TvRemotePadServerState
import com.example.data.AppConfig
import com.example.ui.components.GamepadSurfaceController
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.muitenvn.cloudplay.protocol.TvServiceStatus

/**
 * Màn hình TV "Dùng điện thoại làm tay cầm" theo thiết kế phẳng hiện đại:
 * - Ưu tiên hiển thị MÃ GHÉP ĐÔI 4 SỐ to rõ ở chính giữa, đọc được từ xa trên TV không cần cuộn.
 * - Hướng dẫn 3 bước thu gọn thành hàng chip nhỏ gọn ở dưới, có thể bấm mở rộng chi tiết khi cần.
 * - Chip tên TV và địa chỉ IP tự động co giãn / xuống dòng (FlowRow), không bao giờ bị cắt chữ.
 * - Màn "Điện thoại đã sẵn sàng" gọn gàng, hỗ trợ Thử rung, Ngắt kết nối, Quên thiết bị, Đóng bằng D-Pad/A/B.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TvRemotePadDialog(
    config: AppConfig,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val serverState by RemotePadTvServer.state.collectAsState()
    val defaultFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        RemotePadTvServer.onPairingScreenOpened(context)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(TvBg)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .testTag("tv_remote_pad_dialog")
        ) {
            val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
            val scale = computeTvScale(maxWidth, maxHeight, fontScale)
            val outerH = (maxWidth * 0.045f).coerceAtLeast(16.dp * scale)
            val outerV = (maxHeight * 0.04f).coerceAtLeast(12.dp * scale)
            val cardCorner = (16.dp * scale).coerceAtLeast(12.dp)

            GamepadSurfaceController(
                enabled = true,
                defaultFocusRequester = defaultFocusRequester,
                autoRequestDefaultFocus = true,
                attachWindowMotionListener = true,
                onBackPress = onDismiss
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = outerH, vertical = outerV),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. Thanh đầu trang: Tiêu đề + Chip Tên TV & Địa chỉ IP tự xuống dòng không cắt chữ
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy((6.dp * scale).coerceAtLeast(4.dp))
                    ) {
                        Text(
                            text = config.tr(
                                "Dùng điện thoại làm tay cầm",
                                "Use Phone as Controller"
                            ),
                            color = TvInk,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = (22f * scale).coerceAtLeast(16f).sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy((8.dp * scale).coerceAtLeast(6.dp)),
                            verticalArrangement = Arrangement.spacedBy((6.dp * scale).coerceAtLeast(4.dp))
                        ) {
                            val tvName = serverState.tvDisplayName.ifBlank { "Ravno TV" }
                            val ipLabel = serverState.localIpAddress.ifBlank { "Wi-Fi / LAN" }
                            TvFlexibleInfoChip(
                                leadingDotColor = TvAccent,
                                text = tvName,
                                scale = scale
                            )
                            TvFlexibleInfoChip(
                                leadingDotColor = TvCyan,
                                text = "IP: $ipLabel",
                                scale = scale
                            )
                        }
                    }

                    // 2. Thân chính: Chờ ghép đôi (Mã 4 số to chính giữa) hoặc Điện thoại đã sẵn sàng
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (serverState.status == TvServiceStatus.HAS_CONTROLLER && serverState.connectedDeviceName.isNotBlank()) {
                            TvRemotePadConnectedBody(
                                config = config,
                                serverState = serverState,
                                scale = scale,
                                cardCorner = cardCorner,
                                defaultFocusRequester = defaultFocusRequester,
                                onTestRumble = {
                                    val sent = RemotePadTvServer.sendRumbleToPhone()
                                    Toast.makeText(
                                        context,
                                        if (sent) {
                                            config.tr("Đã gửi tín hiệu rung tới điện thoại", "Sent vibration test to phone")
                                        } else {
                                            config.tr("Không thể gửi tín hiệu rung", "Unable to send vibration test")
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onDisconnect = {
                                    RemotePadTvServer.disconnectActivePhone()
                                },
                                onForgetDevice = {
                                    RemotePadTvServer.forgetDevice(serverState.connectedDeviceId)
                                    Toast.makeText(
                                        context,
                                        config.tr("Đã quên thiết bị và thu hồi mã nhớ", "Device forgotten and token revoked"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onClose = onDismiss
                            )
                        } else {
                            TvRemotePadPairingBody(
                                config = config,
                                serverState = serverState,
                                scale = scale,
                                cardCorner = cardCorner,
                                defaultFocusRequester = defaultFocusRequester,
                                onRegeneratePin = {
                                    RemotePadTvServer.regeneratePin()
                                },
                                onForgetAllRemembered = {
                                    RemotePadTvServer.forgetDevice(null)
                                    Toast.makeText(
                                        context,
                                        config.tr("Đã quên toàn bộ điện thoại đã lưu", "Cleared all remembered phones"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onClose = onDismiss
                            )
                        }
                    }

                    // 3. Thanh gợi ý phím dưới cùng
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy((6.dp * scale).coerceAtLeast(4.dp))
                    ) {
                        val bottomNotice = if (serverState.status == TvServiceStatus.HAS_CONTROLLER) {
                            config.tr(
                                "Tay cầm thật và điện thoại dùng song song · Tự nhả nút an toàn khi mất mạng",
                                "Physical & phone controllers work together · Auto-releases buttons on disconnect"
                            )
                        } else {
                            config.tr(
                                "Mã 4 số đổi mới mỗi lần mở · Sai 3 lần tạm khóa 3 phút",
                                "4-digit code refreshes on open · 3 wrong tries locks for 3 minutes"
                            )
                        }
                        Text(
                            text = bottomNotice,
                            color = TvMuted,
                            fontSize = (12f * scale).coerceAtLeast(10f).sp
                        )

                        TvBottomKeyHintBar(
                            hints = listOf(
                                TvKeyHint(listOf("A" to TvKeyABg), config.tr("Chọn", "Select")),
                                TvKeyHint(listOf("B" to TvKeyBBg), config.tr("Đóng", "Close"))
                            ),
                            scale = scale * 0.9f,
                            modifier = Modifier.widthIn(max = 260.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TvRemotePadPairingBody(
    config: AppConfig,
    serverState: TvRemotePadServerState,
    scale: Float,
    cardCorner: Dp,
    defaultFocusRequester: FocusRequester,
    onRegeneratePin: () -> Unit,
    onForgetAllRemembered: () -> Unit,
    onClose: () -> Unit
) {
    var showExpandedGuide by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((12.dp * scale).coerceAtLeast(8.dp), Alignment.CenterVertically)
    ) {
        Text(
            text = config.tr("Nhập mã này trên điện thoại", "Enter this code on your phone"),
            color = TvMuted,
            fontFamily = SpaceGroteskFontFamily,
            fontSize = (15.5f * scale).coerceAtLeast(12.5f).sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        // 4 ô số PIN cực lớn ở vị trí trung tâm nổi bật nhất
        val digits = serverState.currentPin.padEnd(4, '0').take(4)
        val isLocked = serverState.lockoutRemainingSec > 0
        val hasFailedAttempts = serverState.failedPinAttempts > 0
        val boxW = (84.dp * scale).coerceAtLeast(58.dp)
        val boxH = (106.dp * scale).coerceAtLeast(74.dp)
        val boxCorner = (16.dp * scale).coerceAtLeast(12.dp)

        Row(
            horizontalArrangement = Arrangement.spacedBy((12.dp * scale).coerceAtLeast(8.dp)),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("tv_remote_pad_pin_boxes")
        ) {
            for (i in 0 until 4) {
                val borderColor = when {
                    isLocked -> TvDanger
                    hasFailedAttempts -> TvWarn
                    else -> TvLine
                }
                val digitColor = when {
                    isLocked -> Color(0xFF4D6156)
                    hasFailedAttempts -> TvDanger
                    else -> TvInk
                }
                Box(
                    modifier = Modifier
                        .size(width = boxW, height = boxH)
                        .clip(RoundedCornerShape(boxCorner))
                        .background(TvSurface2)
                        .border(1.5.dp, borderColor, RoundedCornerShape(boxCorner)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isLocked) "–" else digits[i].toString(),
                        color = digitColor,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = (54f * scale).coerceAtLeast(34f).sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Chip trạng thái chờ ngắn gọn ngay dưới mã PIN
        val versionNotice = config.tr(serverState.versionMismatchNoticeVi, serverState.versionMismatchNoticeEn)
        val statusDotColor = when {
            versionNotice.isNotBlank() || isLocked -> TvDanger
            hasFailedAttempts -> TvWarn
            else -> TvAccent
        }
        val statusText = when {
            versionNotice.isNotBlank() -> versionNotice
            isLocked -> {
                val mins = serverState.lockoutRemainingSec / 60
                val secs = serverState.lockoutRemainingSec % 60
                val timeStr = "%d:%02d".format(mins, secs)
                config.tr(
                    "Sai 3 lần · Chờ $timeStr rồi nhập lại",
                    "3 wrong tries · Wait $timeStr to retry"
                )
            }
            hasFailedAttempts -> {
                val rem = (3 - serverState.failedPinAttempts).coerceAtLeast(1)
                config.tr(
                    "Mã chưa đúng · Còn $rem lần thử",
                    "Wrong code · $rem attempts left"
                )
            }
            else -> config.tr(
                "Đang chờ điện thoại…",
                "Waiting for phone…"
            )
        }

        TvFlexibleInfoChip(
            leadingDotColor = statusDotColor,
            text = statusText,
            textColor = if (statusDotColor == TvAccent) TvInk else statusDotColor,
            scale = scale
        )

        // Hàng nút chính: Đổi mã + Đóng (+ Quên thiết bị đã lưu nếu có)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy((12.dp * scale).coerceAtLeast(8.dp), Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy((8.dp * scale).coerceAtLeast(6.dp)),
            modifier = Modifier.fillMaxWidth()
        ) {
            TvButton(
                text = config.tr("Đổi mã", "New Code"),
                variant = TvButtonVariant.PRIMARY,
                scale = scale,
                focusRequester = defaultFocusRequester,
                onClick = onRegeneratePin,
                modifier = Modifier
                    .widthIn(min = (132.dp * scale).coerceAtLeast(100.dp))
                    .testTag("tv_remote_pad_regenerate_pin_btn")
            )
            TvButton(
                text = config.tr("Đóng", "Close"),
                variant = TvButtonVariant.DEFAULT,
                scale = scale,
                onClick = onClose,
                modifier = Modifier
                    .widthIn(min = (120.dp * scale).coerceAtLeast(92.dp))
                    .testTag("tv_remote_pad_close_btn")
            )
            if (serverState.rememberedDevices.isNotEmpty()) {
                TvButton(
                    text = config.tr(
                        "Quên thiết bị (${serverState.rememberedDevices.size})",
                        "Forget Devices (${serverState.rememberedDevices.size})"
                    ),
                    variant = TvButtonVariant.DANGER,
                    scale = scale,
                    onClick = onForgetAllRemembered,
                    modifier = Modifier.testTag("tv_remote_pad_forget_remembered_btn")
                )
            }
        }

        // Hướng dẫn 3 bước thu gọn thành 1 hàng chip gọn gàng (bấm vào có thể mở rộng xem chi tiết)
        val tvNameShort = serverState.tvDisplayName.ifBlank { "TV" }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy((8.dp * scale).coerceAtLeast(6.dp), Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy((6.dp * scale).coerceAtLeast(4.dp)),
            modifier = Modifier.fillMaxWidth()
        ) {
            TvStepCompactChip(
                stepNumber = "1",
                text = config.tr("Mở Ravno Pad", "Open Ravno Pad"),
                scale = scale
            )
            TvStepCompactChip(
                stepNumber = "2",
                text = config.tr("Chọn \"$tvNameShort\"", "Select \"$tvNameShort\""),
                scale = scale
            )
            TvStepCompactChip(
                stepNumber = "3",
                text = config.tr("Nhập mã", "Enter code"),
                scale = scale
            )
            TvChip(
                text = if (showExpandedGuide) {
                    config.tr("Ẩn chi tiết ▲", "Hide details ▲")
                } else {
                    config.tr("Chi tiết ▼", "Details ▼")
                },
                selected = showExpandedGuide,
                scale = scale * 0.9f,
                onClick = { showExpandedGuide = !showExpandedGuide },
                modifier = Modifier.testTag("tv_remote_pad_toggle_guide_btn")
            )
        }

        if (showExpandedGuide) {
            Column(
                modifier = Modifier
                    .widthIn(max = (680.dp * scale).coerceAtLeast(420.dp))
                    .clip(RoundedCornerShape(cardCorner))
                    .background(TvSurface)
                    .border(1.dp, TvLine, RoundedCornerShape(cardCorner))
                    .padding((14.dp * scale).coerceAtLeast(10.dp)),
                verticalArrangement = Arrangement.spacedBy((6.dp * scale).coerceAtLeast(4.dp))
            ) {
                Text(
                    text = config.tr(
                        "• Bước 1: Mở ứng dụng Ravno Pad trên điện thoại Android (bắt cùng mạng Wi-Fi / LAN với TV).",
                        "• Step 1: Open Ravno Pad on your Android phone (connected to the same Wi-Fi / LAN as this TV)."
                    ),
                    color = TvInk,
                    fontSize = (12.5f * scale).coerceAtLeast(10.5f).sp
                )
                Text(
                    text = config.tr(
                        "• Bước 2: Chạm vào tên TV \"$tvNameShort\" (${serverState.localIpAddress.ifBlank { "LAN" }}) trong danh sách.",
                        "• Step 2: Tap \"$tvNameShort\" (${serverState.localIpAddress.ifBlank { "LAN" }}) in the discovered list."
                    ),
                    color = TvInk,
                    fontSize = (12.5f * scale).coerceAtLeast(10.5f).sp
                )
                Text(
                    text = config.tr(
                        "• Bước 3: Nhập 4 chữ số trên màn hình. Từ lần sau điện thoại tự kết nối lại không cần nhập mã.",
                        "• Step 3: Enter the 4 digits shown above. Next time your phone reconnects automatically."
                    ),
                    color = TvInk,
                    fontSize = (12.5f * scale).coerceAtLeast(10.5f).sp
                )
                if (serverState.rememberedDevices.isNotEmpty()) {
                    val names = serverState.rememberedDevices.joinToString(", ") { it.deviceName }
                    Text(
                        text = config.tr(
                            "• Thiết bị đã nhớ (${serverState.rememberedDevices.size}): $names",
                            "• Remembered phones (${serverState.rememberedDevices.size}): $names"
                        ),
                        color = TvCyan,
                        fontSize = (12f * scale).coerceAtLeast(10f).sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TvRemotePadConnectedBody(
    config: AppConfig,
    serverState: TvRemotePadServerState,
    scale: Float,
    cardCorner: Dp,
    defaultFocusRequester: FocusRequester,
    onTestRumble: () -> Unit,
    onDisconnect: () -> Unit,
    onForgetDevice: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = (620.dp * scale).coerceAtLeast(380.dp))
            .clip(RoundedCornerShape(cardCorner))
            .background(TvSurface)
            .border(1.dp, TvLine, RoundedCornerShape(cardCorner))
            .padding((24.dp * scale).coerceAtLeast(16.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((12.dp * scale).coerceAtLeast(8.dp), Alignment.CenterVertically)
    ) {
        // Biểu tượng điện thoại nổi bật
        Box(
            modifier = Modifier
                .size((56.dp * scale).coerceAtLeast(42.dp))
                .clip(RoundedCornerShape((14.dp * scale).coerceAtLeast(10.dp)))
                .background(TvSurface2)
                .border(1.dp, TvAccent, RoundedCornerShape((14.dp * scale).coerceAtLeast(10.dp))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📱",
                fontSize = (26f * scale).coerceAtLeast(20f).sp
            )
        }

        // Tên điện thoại (tự xuống dòng, không bao giờ cắt chữ)
        Text(
            text = serverState.connectedDeviceName,
            color = TvInk,
            fontFamily = SpaceGroteskFontFamily,
            fontSize = (24f * scale).coerceAtLeast(18f).sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        // Chip trạng thái kết nối xanh lá
        TvFlexibleInfoChip(
            leadingDotColor = TvAccent,
            text = config.tr(
                "Đã kết nối · Tay cầm ${serverState.connectedPlayerIndex} · Ổn định",
                "Connected · Controller ${serverState.connectedPlayerIndex} · Stable"
            ),
            textColor = TvAccent,
            scale = scale
        )

        Text(
            text = config.tr(
                "Đóng màn hình này và mở bất kỳ game nào. Điện thoại sẽ điều khiển và rung theo game.",
                "Close this screen and launch any game. Your phone controls gameplay and vibrates with rumble."
            ),
            color = TvMuted,
            fontSize = (13f * scale).coerceAtLeast(10.5f).sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height((4.dp * scale).coerceAtLeast(2.dp)))

        // Hàng nút điều khiển D-Pad
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy((10.dp * scale).coerceAtLeast(8.dp), Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy((8.dp * scale).coerceAtLeast(6.dp)),
            modifier = Modifier.fillMaxWidth()
        ) {
            TvButton(
                text = config.tr("Ngắt kết nối", "Disconnect"),
                variant = TvButtonVariant.PRIMARY,
                scale = scale,
                focusRequester = defaultFocusRequester,
                onClick = onDisconnect,
                modifier = Modifier
                    .widthIn(min = (136.dp * scale).coerceAtLeast(104.dp))
                    .testTag("tv_remote_pad_disconnect_btn")
            )
            TvButton(
                text = config.tr("Quên thiết bị", "Forget Device"),
                variant = TvButtonVariant.DANGER,
                scale = scale,
                onClick = onForgetDevice,
                modifier = Modifier
                    .widthIn(min = (136.dp * scale).coerceAtLeast(104.dp))
                    .testTag("tv_remote_pad_forget_device_btn")
            )
            TvButton(
                text = config.tr("Thử rung", "Test Vibration"),
                variant = TvButtonVariant.DEFAULT,
                scale = scale,
                onClick = onTestRumble,
                modifier = Modifier
                    .widthIn(min = (120.dp * scale).coerceAtLeast(94.dp))
                    .testTag("tv_remote_pad_test_rumble_btn")
            )
            TvButton(
                text = config.tr("Đóng", "Close"),
                variant = TvButtonVariant.DEFAULT,
                scale = scale,
                onClick = onClose,
                modifier = Modifier
                    .widthIn(min = (110.dp * scale).coerceAtLeast(86.dp))
                    .testTag("tv_remote_pad_connected_close_btn")
            )
        }
    }
}

/**
 * Thẻ nổi góc trên phải khi đang trong game mà có điện thoại yêu cầu ghép đôi (Màn 8 trong bản mẫu).
 * Không tự động cướp focus của game; người dùng có thể bấm nút hoặc đóng bằng nút B / chạm.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TvInGamePairingToast(
    config: AppConfig,
    serverState: TvRemotePadServerState,
    onRegeneratePin: () -> Unit,
    onOpenFullDialog: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val digits = serverState.currentPin.padEnd(4, '0').take(4)
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val scale = (1f / fontScale.coerceIn(0.95f, 1.25f)).coerceIn(0.78f, 1.0f)
    val corner = (14.dp * scale).coerceAtLeast(10.dp)
    val boxW = (38.dp * scale).coerceAtLeast(30.dp)
    val boxH = (48.dp * scale).coerceAtLeast(38.dp)
    val boxCorner = (9.dp * scale).coerceAtLeast(7.dp)

    Column(
        modifier = modifier
            .widthIn(min = 220.dp, max = 310.dp)
            .clip(RoundedCornerShape(corner))
            .background(TvSurface.copy(alpha = 0.96f))
            .border(1.5.dp, TvAccent, RoundedCornerShape(corner))
            .padding((12.dp * scale).coerceAtLeast(9.dp))
            .testTag("tv_in_game_pairing_toast"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((8.dp * scale).coerceAtLeast(6.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = config.tr("📱 Điện thoại muốn ghép", "📱 Phone wants to pair"),
                color = TvInk,
                fontFamily = SpaceGroteskFontFamily,
                fontSize = (13f * scale).coerceAtLeast(11f).sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = serverState.localIpAddress.ifBlank { "LAN" },
                color = TvCyan,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = (10.5f * scale).coerceAtLeast(9f).sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy((6.dp * scale).coerceAtLeast(4.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                Box(
                    modifier = Modifier
                        .size(width = boxW, height = boxH)
                        .clip(RoundedCornerShape(boxCorner))
                        .background(TvSurface2)
                        .border(1.dp, TvLine, RoundedCornerShape(boxCorner)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digits[i].toString(),
                        color = TvAccent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = (22f * scale).coerceAtLeast(17f).sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((6.dp * scale).coerceAtLeast(4.dp), Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy((6.dp * scale).coerceAtLeast(4.dp))
        ) {
            TvButton(
                text = config.tr("Đổi mã", "New Code"),
                variant = TvButtonVariant.PRIMARY,
                scale = scale * 0.85f,
                onClick = onRegeneratePin,
                modifier = Modifier.testTag("tv_in_game_toast_regenerate_btn")
            )
            TvButton(
                text = config.tr("Mở rộng", "Expand"),
                variant = TvButtonVariant.DEFAULT,
                scale = scale * 0.85f,
                onClick = onOpenFullDialog,
                modifier = Modifier.testTag("tv_in_game_toast_expand_btn")
            )
            TvButton(
                text = config.tr("Đóng (B)", "Close (B)"),
                variant = TvButtonVariant.DEFAULT,
                scale = scale * 0.85f,
                onClick = onDismiss,
                modifier = Modifier.testTag("tv_in_game_toast_close_btn")
            )
        }
    }
}

@Composable
private fun TvFlexibleInfoChip(
    leadingDotColor: Color,
    text: String,
    scale: Float,
    textColor: Color = TvMuted
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape((99.dp)))
            .background(TvSurface2)
            .border(1.dp, TvLine, RoundedCornerShape(99.dp))
            .padding(
                horizontal = (11.dp * scale).coerceAtLeast(8.dp),
                vertical = (5.dp * scale).coerceAtLeast(3.5.dp)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((6.dp * scale).coerceAtLeast(4.dp))
    ) {
        Box(
            modifier = Modifier
                .size((7.dp * scale).coerceAtLeast(5.dp))
                .clip(CircleShape)
                .background(leadingDotColor)
        )
        Text(
            text = text,
            color = textColor,
            fontFamily = SpaceGroteskFontFamily,
            fontSize = (12.5f * scale).coerceAtLeast(10f).sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TvStepCompactChip(
    stepNumber: String,
    text: String,
    scale: Float
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(TvSurface2)
            .border(1.dp, TvLine, RoundedCornerShape(99.dp))
            .padding(
                horizontal = (11.dp * scale).coerceAtLeast(8.dp),
                vertical = (4.5.dp * scale).coerceAtLeast(3.dp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = TvCyan,
                        fontWeight = FontWeight.ExtraBold
                    )
                ) {
                    append(stepNumber)
                    append(" ")
                }
                withStyle(
                    SpanStyle(
                        color = TvMuted,
                        fontWeight = FontWeight.Medium
                    )
                ) {
                    append(text)
                }
            },
            fontFamily = SpaceGroteskFontFamily,
            fontSize = (12f * scale).coerceAtLeast(9.5f).sp
        )
    }
}
