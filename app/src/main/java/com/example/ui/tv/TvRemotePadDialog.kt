package com.example.ui.tv

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cloudplay.RemotePadTvServer
import com.example.data.AppConfig
import com.example.ui.components.GamepadSurfaceController
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.muitenvn.cloudplay.protocol.TvServiceStatus

/**
 * 2 màn hình TV trong Cài đặt ("Dùng điện thoại làm tay cầm") theo đúng remote-pad-mockup.html:
 * - Màn TV 1 (Chờ ghép đôi): hiện mã 4 số đổi mới mỗi lần mở, hướng dẫn 3 bước, khóa tạm khi sai 3 lần, nút Đổi mã / Đóng / Quên thiết bị.
 * - Màn TV 2 (Đã kết nối): hiện tên điện thoại đang làm Tay cầm 1, nút Thử rung trên điện thoại / Ngắt kết nối / Quên thiết bị / Đóng.
 */
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
            val outerV = (maxHeight * 0.045f).coerceAtLeast(14.dp * scale)
            val cardCorner = (18.dp * scale).coerceAtLeast(12.dp)
            val stepCorner = (14.dp * scale).coerceAtLeast(10.dp)

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
                    // 1. Thanh tiêu đề trên cùng
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = config.tr(
                                "Cài đặt · Dùng điện thoại làm tay cầm",
                                "Settings · Use Phone as Controller"
                            ),
                            color = TvInk,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = (20f * scale).coerceAtLeast(15f).sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy((8.dp * scale).coerceAtLeast(6.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .size((8.dp * scale).coerceAtLeast(6.dp))
                                    .clip(CircleShape)
                                    .background(TvAccent)
                            )
                            val rightBadgeText = if (serverState.status == TvServiceStatus.HAS_CONTROLLER) {
                                config.tr("Đang kết nối 1 tay cầm", "1 controller connected")
                            } else {
                                val ipLabel = serverState.localIpAddress.ifBlank { "LAN" }
                                "${serverState.tvDisplayName} · $ipLabel"
                            }
                            Text(
                                text = rightBadgeText,
                                color = TvMuted,
                                fontSize = (13f * scale).coerceAtLeast(10.5f).sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height((12.dp * scale).coerceAtLeast(8.dp)))

                    // 2. Thân chính 2 cột (Màn TV 1: Chờ ghép đôi | Màn TV 2: Đã kết nối)
                    if (serverState.status == TvServiceStatus.HAS_CONTROLLER && serverState.connectedDeviceName.isNotBlank()) {
                        TvRemotePadConnectedBody(
                            config = config,
                            serverState = serverState,
                            scale = scale,
                            cardCorner = cardCorner,
                            stepCorner = stepCorner,
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
                            onClose = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    } else {
                        TvRemotePadPairingBody(
                            config = config,
                            serverState = serverState,
                            scale = scale,
                            cardCorner = cardCorner,
                            stepCorner = stepCorner,
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
                            onClose = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height((10.dp * scale).coerceAtLeast(6.dp)))

                    // 3. Thanh gợi ý phím dưới cùng
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val bottomNotice = if (serverState.status == TvServiceStatus.HAS_CONTROLLER) {
                            config.tr(
                                "Tay cầm thật trên TV và điện thoại có thể dùng song song",
                                "Physical controller on TV and phone controller work simultaneously"
                            )
                        } else {
                            config.tr(
                                "Mã 4 số tự đổi mới mỗi lần mở màn hình này · Sai 3 lần sẽ tạm khóa ít phút",
                                "4-digit code changes each time this screen opens · 3 wrong tries locks temporarily"
                            )
                        }
                        Text(
                            text = bottomNotice,
                            color = TvMuted,
                            fontSize = (12.5f * scale).coerceAtLeast(10f).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        TvBottomKeyHintBar(
                            hints = listOf(
                                TvKeyHint(listOf("A" to TvKeyABg), config.tr("Chọn", "Select")),
                                TvKeyHint(listOf("B" to TvKeyBBg), config.tr("Quay lại", "Back"))
                            ),
                            scale = scale * 0.9f
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvRemotePadPairingBody(
    config: AppConfig,
    serverState: com.example.cloudplay.TvRemotePadServerState,
    scale: Float,
    cardCorner: androidx.compose.ui.unit.Dp,
    stepCorner: androidx.compose.ui.unit.Dp,
    defaultFocusRequester: FocusRequester,
    onRegeneratePin: () -> Unit,
    onForgetAllRemembered: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((22.dp * scale).coerceAtLeast(14.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cột trái: Hướng dẫn 3 bước + danh sách thiết bị đã nhớ (nếu có)
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy((10.dp * scale).coerceAtLeast(7.dp), Alignment.CenterVertically)
        ) {
            Text(
                text = config.tr("Dùng điện thoại làm tay cầm", "Use Phone as Controller"),
                color = TvInk,
                fontFamily = SpaceGroteskFontFamily,
                fontSize = (24f * scale).coerceAtLeast(18f).sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = config.tr(
                    "Biến điện thoại Android thành tay cầm không dây cho TV qua mạng nội bộ, không cần tay cầm Bluetooth.",
                    "Turn your Android phone into a wireless LAN controller for TV without needing a Bluetooth gamepad."
                ),
                color = TvMuted,
                fontSize = (13.5f * scale).coerceAtLeast(11f).sp,
                lineHeight = (19f * scale).coerceAtLeast(15f).sp
            )

            TvPairingStepCard(
                stepLabel = config.tr("Bước 1", "Step 1"),
                title = config.tr("Mở ứng dụng Ravno Pad trên điện thoại", "Open Ravno Pad on your phone"),
                subtitle = config.tr(
                    "Đảm bảo điện thoại và TV đang bắt cùng một mạng Wi-Fi hoặc mạng nội bộ.",
                    "Make sure your phone and TV are connected to the same Wi-Fi / LAN."
                ),
                scale = scale,
                corner = stepCorner
            )

            TvPairingStepCard(
                stepLabel = config.tr("Bước 2", "Step 2"),
                title = config.tr(
                    "Chọn đúng tên TV \"${serverState.tvDisplayName}\"",
                    "Select TV \"${serverState.tvDisplayName}\""
                ),
                subtitle = config.tr(
                    "Điện thoại sẽ tự tìm thấy TV trong danh sách thiết bị gần đó.",
                    "Your phone will automatically discover this TV on the local network."
                ),
                scale = scale,
                corner = stepCorner
            )

            TvPairingStepCard(
                stepLabel = config.tr("Bước 3", "Step 3"),
                title = config.tr("Nhập 4 chữ số bên phải trên điện thoại", "Enter the 4-digit code on your phone"),
                subtitle = config.tr(
                    "Chỉ cần nhập lần đầu. Những lần sau điện thoại tự kết nối lại không cần nhập mã.",
                    "Only needed once. Next time your phone reconnects automatically."
                ),
                scale = scale,
                corner = stepCorner
            )

            if (serverState.rememberedDevices.isNotEmpty()) {
                val names = serverState.rememberedDevices.joinToString(", ") { it.deviceName }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(stepCorner))
                        .background(TvSurface)
                        .border(1.dp, TvLine, RoundedCornerShape(stepCorner))
                        .padding(
                            horizontal = (12.dp * scale).coerceAtLeast(9.dp),
                            vertical = (8.dp * scale).coerceAtLeast(6.dp)
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = config.tr(
                                "Thiết bị đã nhớ (${serverState.rememberedDevices.size}): $names",
                                "Remembered devices (${serverState.rememberedDevices.size}): $names"
                            ),
                            color = TvInk,
                            fontSize = (12.5f * scale).coerceAtLeast(10f).sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = config.tr("Tự kết nối lại không cần nhập mã", "Auto-reconnects without PIN"),
                            color = TvMuted,
                            fontSize = (11f * scale).coerceAtLeast(9.5f).sp
                        )
                    }
                    Spacer(modifier = Modifier.width((8.dp * scale).coerceAtLeast(6.dp)))
                    TvButton(
                        text = config.tr("Quên thiết bị", "Forget Device"),
                        variant = TvButtonVariant.DANGER,
                        scale = scale * 0.85f,
                        onClick = onForgetAllRemembered,
                        modifier = Modifier.testTag("tv_remote_pad_forget_remembered_btn")
                    )
                }
            }
        }

        // Cột phải: Thẻ Mã ghép đôi 4 chữ số
        Column(
            modifier = Modifier
                .weight(0.95f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(cardCorner))
                .background(TvSurface)
                .border(1.dp, TvLine, RoundedCornerShape(cardCorner))
                .padding((22.dp * scale).coerceAtLeast(14.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((14.dp * scale).coerceAtLeast(10.dp), Alignment.CenterVertically)
        ) {
            Text(
                text = config.tr("MÃ GHÉP ĐÔI MỘT LẦN", "ONE-TIME PAIRING CODE"),
                color = TvMuted,
                fontSize = (12.5f * scale).coerceAtLeast(10.5f).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // 4 ô số PIN
            val digits = serverState.currentPin.padEnd(4, '0').take(4)
            Row(
                horizontalArrangement = Arrangement.spacedBy((10.dp * scale).coerceAtLeast(8.dp)),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("tv_remote_pad_pin_boxes")
            ) {
                for (i in 0 until 4) {
                    val boxW = (62.dp * scale).coerceAtLeast(46.dp)
                    val boxH = (76.dp * scale).coerceAtLeast(56.dp)
                    val boxCorner = (14.dp * scale).coerceAtLeast(10.dp)
                    Box(
                        modifier = Modifier
                            .size(width = boxW, height = boxH)
                            .clip(RoundedCornerShape(boxCorner))
                            .background(TvSurface2)
                            .border(
                                1.5.dp,
                                if (serverState.lockoutRemainingSec > 0) TvDanger else TvLine,
                                RoundedCornerShape(boxCorner)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (serverState.lockoutRemainingSec > 0) "–" else digits[i].toString(),
                            color = if (serverState.lockoutRemainingSec > 0) TvDanger else TvAccent,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = (38f * scale).coerceAtLeast(26f).sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Trạng thái chờ / cảnh báo sai mã / khóa tạm / lệch phiên bản
            val versionNotice = config.tr(serverState.versionMismatchNoticeVi, serverState.versionMismatchNoticeEn)
            val statusColor = when {
                versionNotice.isNotBlank() || serverState.lockoutRemainingSec > 0 -> TvDanger
                serverState.failedPinAttempts > 0 -> TvWarn
                else -> TvAccent
            }
            val statusText = when {
                versionNotice.isNotBlank() -> versionNotice
                serverState.lockoutRemainingSec > 0 -> config.tr(
                    "Đã nhập sai 3 lần · Tạm khóa ghép đôi (${serverState.lockoutRemainingSec} giây)",
                    "3 wrong attempts · Pairing locked (${serverState.lockoutRemainingSec}s)"
                )
                serverState.failedPinAttempts > 0 -> {
                    val rem = (3 - serverState.failedPinAttempts).coerceAtLeast(1)
                    config.tr(
                        "Điện thoại vừa nhập sai mã · Còn $rem lần thử",
                        "Wrong code entered · $rem attempts left"
                    )
                }
                else -> config.tr(
                    "Đang chờ điện thoại nhập mã...",
                    "Waiting for phone to enter code..."
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((8.dp * scale).coerceAtLeast(6.dp))
            ) {
                Box(
                    modifier = Modifier
                        .size((9.dp * scale).coerceAtLeast(7.dp))
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    text = statusText,
                    color = if (statusColor == TvAccent) TvInk else statusColor,
                    fontSize = (13.5f * scale).coerceAtLeast(11f).sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy((12.dp * scale).coerceAtLeast(8.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvButton(
                    text = config.tr("Đổi mã", "New Code"),
                    variant = TvButtonVariant.DEFAULT,
                    scale = scale,
                    focusRequester = defaultFocusRequester,
                    onClick = onRegeneratePin,
                    modifier = Modifier.testTag("tv_remote_pad_regenerate_pin_btn")
                )
                TvButton(
                    text = config.tr("Đóng", "Close"),
                    variant = TvButtonVariant.DEFAULT,
                    scale = scale,
                    onClick = onClose,
                    modifier = Modifier.testTag("tv_remote_pad_close_btn")
                )
            }
        }
    }
}

@Composable
private fun TvRemotePadConnectedBody(
    config: AppConfig,
    serverState: com.example.cloudplay.TvRemotePadServerState,
    scale: Float,
    cardCorner: androidx.compose.ui.unit.Dp,
    stepCorner: androidx.compose.ui.unit.Dp,
    defaultFocusRequester: FocusRequester,
    onTestRumble: () -> Unit,
    onDisconnect: () -> Unit,
    onForgetDevice: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((22.dp * scale).coerceAtLeast(14.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cột trái: Thiết bị đang kết nối + ghi chú
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy((10.dp * scale).coerceAtLeast(7.dp), Alignment.CenterVertically)
        ) {
            Text(
                text = config.tr("Điện thoại đã sẵn sàng", "Phone Controller Ready"),
                color = TvInk,
                fontFamily = SpaceGroteskFontFamily,
                fontSize = (24f * scale).coerceAtLeast(18f).sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = config.tr(
                    "Bạn có thể đóng màn hình này và mở bất kỳ game nào. Điện thoại sẽ điều khiển và rung theo game.",
                    "You can close this screen and launch any game. Your phone will control the game and receive rumble feedback."
                ),
                color = TvMuted,
                fontSize = (13.5f * scale).coerceAtLeast(11f).sp,
                lineHeight = (19f * scale).coerceAtLeast(15f).sp
            )

            // Thẻ thiết bị đang kết nối
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(stepCorner))
                    .background(TvSurface)
                    .border(1.5.dp, TvAccent, RoundedCornerShape(stepCorner))
                    .padding((14.dp * scale).coerceAtLeast(10.dp)),
                verticalArrangement = Arrangement.spacedBy((4.dp * scale).coerceAtLeast(3.dp))
            ) {
                Text(
                    text = config.tr(
                        "Tay cầm ${serverState.connectedPlayerIndex} · Đã ghép đôi",
                        "Controller ${serverState.connectedPlayerIndex} · Paired"
                    ),
                    color = TvAccent,
                    fontSize = (12f * scale).coerceAtLeast(10f).sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = serverState.connectedDeviceName,
                    color = TvInk,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = (18f * scale).coerceAtLeast(14f).sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = config.tr(
                        "Tín hiệu ổn định · Sẵn sàng vào game",
                        "Stable signal · Ready for gameplay"
                    ),
                    color = TvMuted,
                    fontSize = (12.5f * scale).coerceAtLeast(10f).sp
                )
            }

            TvPairingStepCard(
                stepLabel = config.tr("Tự động nhớ thiết bị", "Auto-Remember Device"),
                title = config.tr("Lần sau tự kết nối lại, không cần nhập mã", "Reconnects automatically next time"),
                subtitle = config.tr(
                    "Chọn \"Quên thiết bị\" bên phải nếu bạn không muốn điện thoại này tự kết nối nữa.",
                    "Choose \"Forget Device\" on the right to revoke auto-reconnect access."
                ),
                scale = scale,
                corner = stepCorner
            )

            TvPairingStepCard(
                stepLabel = config.tr("An toàn nút bấm", "Fail-Safe Input Release"),
                title = config.tr("Tự nhả toàn bộ nút ngay khi ngắt kết nối", "Releases all buttons immediately on disconnect"),
                subtitle = config.tr(
                    "Không bao giờ bị kẹt nút chạy hay bắn trong game khi điện thoại tắt màn hình hoặc mất mạng.",
                    "Prevents stuck movement or triggers if your phone locks or loses Wi-Fi."
                ),
                scale = scale,
                corner = stepCorner
            )
        }

        // Cột phải: Quản lý tay cầm
        Column(
            modifier = Modifier
                .weight(0.95f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(cardCorner))
                .background(TvSurface)
                .border(1.dp, TvLine, RoundedCornerShape(cardCorner))
                .padding((22.dp * scale).coerceAtLeast(14.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((10.dp * scale).coerceAtLeast(8.dp), Alignment.CenterVertically)
        ) {
            Text(
                text = config.tr("QUẢN LÝ TAY CẦM", "CONTROLLER MANAGEMENT"),
                color = TvMuted,
                fontSize = (12.5f * scale).coerceAtLeast(10.5f).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            TvButton(
                text = config.tr("Thử rung trên điện thoại", "Test Vibration on Phone"),
                variant = TvButtonVariant.PRIMARY,
                scale = scale,
                focusRequester = defaultFocusRequester,
                onClick = onTestRumble,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tv_remote_pad_test_rumble_btn")
            )

            TvButton(
                text = config.tr("Ngắt kết nối", "Disconnect"),
                variant = TvButtonVariant.DEFAULT,
                scale = scale,
                onClick = onDisconnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tv_remote_pad_disconnect_btn")
            )

            TvButton(
                text = config.tr("Quên thiết bị", "Forget Device"),
                variant = TvButtonVariant.DANGER,
                scale = scale,
                onClick = onForgetDevice,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tv_remote_pad_forget_device_btn")
            )

            TvButton(
                text = config.tr("Đóng", "Close"),
                variant = TvButtonVariant.DEFAULT,
                scale = scale,
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tv_remote_pad_connected_close_btn")
            )
        }
    }
}

@Composable
private fun TvPairingStepCard(
    stepLabel: String,
    title: String,
    subtitle: String,
    scale: Float,
    corner: androidx.compose.ui.unit.Dp
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(corner))
            .background(TvSurface)
            .border(1.dp, TvLine, RoundedCornerShape(corner))
            .padding(
                horizontal = (14.dp * scale).coerceAtLeast(10.dp),
                vertical = (9.dp * scale).coerceAtLeast(6.dp)
            ),
        verticalArrangement = Arrangement.spacedBy((2.dp * scale).coerceAtLeast(1.dp))
    ) {
        Text(
            text = stepLabel,
            color = TvAccent,
            fontSize = (11.5f * scale).coerceAtLeast(9.5f).sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            color = TvInk,
            fontFamily = SpaceGroteskFontFamily,
            fontSize = (14.5f * scale).coerceAtLeast(11.5f).sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            color = TvMuted,
            fontSize = (12f * scale).coerceAtLeast(9.5f).sp
        )
    }
}
