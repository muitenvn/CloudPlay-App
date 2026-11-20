package com.example.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.example.data.SettingsRepository
import com.example.ui.components.AdaptiveFlatActionButton
import com.example.ui.components.AdaptivePopupFrame
import com.example.ui.components.AdaptivePopupScope
import com.example.ui.components.AdaptiveTwoColumnLayout
import com.example.ui.components.gamepadTextFieldFocus
import com.example.ui.theme.JapanRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.vpn.EndpointPingMeasurement
import com.example.vpn.GeoIpDetector
import com.example.vpn.PingTester
import com.example.vpn.RegionUnlockStatus
import com.example.vpn.ServerLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
internal fun CustomProxyDialog(
    onDismiss: () -> Unit,
    onAddServer: (ServerLocation) -> Unit
) {
    val config by SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8080") }
    var protocol by remember { mutableStateOf("http") }
    var countryCode by remember { mutableStateOf("") }
    var userEditedCountry by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf(if (isEn) "Custom proxy" else "Proxy cá nhân") }
    var speedMbps by remember { mutableStateOf("250") }
    var azureEndpoint by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val cancelFocusRequester = remember { FocusRequester() }
    val saveFocusRequester = remember { FocusRequester() }

    val trimmedHost = host.trim()
    val isPrivateIp = remember(trimmedHost) {
        trimmedHost.isNotEmpty() && GeoIpDetector.isPrivateOrLoopbackIp(trimmedHost)
    }
    val canSave = trimmedHost.isNotBlank() && !isPrivateIp

    val triggerTest: () -> Unit = {
        if (!isTesting && trimmedHost.isNotBlank()) {
            isTesting = true
            testResult = null
            scope.launch(Dispatchers.IO) {
                val p = port.toIntOrNull() ?: 8080
                val ok = !GeoIpDetector.isPrivateOrLoopbackIp(trimmedHost) &&
                    PingTester.isHostReachable(trimmedHost, p, 1500)
                testResult = if (ok) {
                    if (isEn) "✅ Connection successful!" else "✅ Kết nối thành công!"
                } else {
                    if (isEn) "❌ Could not reach this IP/Port" else "❌ Không thể kết nối tới IP/Port này"
                }
                isTesting = false
            }
        }
    }

    val triggerSave: () -> Unit = {
        if (canSave) {
            val p = port.toIntOrNull() ?: 8080
            val speed = speedMbps.toIntOrNull() ?: 200
            val cc = countryCode.trim().uppercase().ifBlank {
                GeoIpDetector.resolveIpCountryCode(trimmedHost, allowNetworkFallback = false)
            }
            val resolvedFlag = GeoIpDetector.getFlagEmoji(cc)
            val defaultDisplayName = GeoIpDetector.formatServerDisplayName(cc, trimmedHost, isEn)
            val resolvedAzure = azureEndpoint.trim().ifBlank {
                GeoIpDetector.getAzureEndpointForCountry(cc)
            }
            val server = ServerLocation(
                id = "custom_${System.currentTimeMillis()}",
                name = name.trim().ifBlank { defaultDisplayName },
                flag = resolvedFlag,
                ip = trimmedHost,
                port = p,
                protocol = protocol.trim().lowercase().let { if (it == "socks5") "socks5" else "http" },
                azureEndpoint = resolvedAzure,
                description = description.ifBlank { if (isEn) "Custom proxy" else "Proxy cá nhân" },
                speedMbps = speed,
                countryCode = cc,
                isCustom = true,
                isVerified = false
            )
            onAddServer(server)
        }
    }

    AdaptivePopupFrame(
        onDismissRequest = onDismiss,
        dialogTestTag = "custom_proxy_dialog",
        defaultFocusRequester = cancelFocusRequester,
        wideWidthFraction = 0.90f,
        portraitWidthFraction = 0.92f
    ) {
        val formScroll = rememberScrollState()
        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = XboxNeonGreen,
            unfocusedBorderColor = XboxCardBorder,
            focusedContainerColor = XboxSurface,
            unfocusedContainerColor = XboxSurface
        )

        @Composable
        fun AdaptivePopupScope.HeaderBlock() {
            Column(verticalArrangement = Arrangement.spacedBy(gapSmall)) {
                Text(
                    text = config.tr("Thêm Proxy Tùy Chỉnh", "Add Custom Proxy"),
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = rSp(16f)
                )
                Text(
                    text = if (isEn) {
                        "Enter a public proxy server IP address and port"
                    } else {
                        "Nhập địa chỉ IP máy chủ công cộng và cổng kết nối riêng"
                    },
                    color = TextSecondary,
                    fontSize = rSp(11.5f),
                    lineHeight = rSp(15.5f)
                )
            }
        }

        @Composable
        fun AdaptivePopupScope.FormFields(maxFormHeight: Dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxFormHeight)
                    .verticalScroll(formScroll),
                verticalArrangement = Arrangement.spacedBy(gapMed)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = {
                        Text(
                            text = if (isEn) "Display Name (Optional)" else "Tên gợi nhớ (Tùy chọn)",
                            fontSize = rSp(11f)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(btnCorner),
                    modifier = Modifier
                        .fillMaxWidth()
                        .gamepadTextFieldFocus(cornerRadius = btnCorner),
                    colors = fieldColors
                )

                OutlinedTextField(
                    value = host,
                    onValueChange = { newHost ->
                        host = newHost
                        val cleanIp = newHost.trim()
                        if (!userEditedCountry) {
                            val detectedCc = GeoIpDetector.resolveIpCountryCode(cleanIp, allowNetworkFallback = false)
                            countryCode = detectedCc
                            azureEndpoint = GeoIpDetector.getAzureEndpointForCountry(detectedCc)
                        }
                    },
                    label = {
                        Text(
                            text = if (isEn) "Public IPv4 Address" else "Địa chỉ IPv4 công cộng",
                            fontSize = rSp(11f)
                        )
                    },
                    placeholder = {
                        Text(
                            text = if (isEn) "e.g., 138.199.21.239" else "vd: 138.199.21.239",
                            fontSize = rSp(11f)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(btnCorner),
                    modifier = Modifier
                        .fillMaxWidth()
                        .gamepadTextFieldFocus(cornerRadius = btnCorner),
                    colors = fieldColors
                )

                if (isPrivateIp) {
                    Text(
                        text = if (isEn) {
                            "⚠️ Private or loopback IP (127.0.0.1, 10.x, 192.168.x) cannot unlock Xbox Cloud."
                        } else {
                            "⚠️ Địa chỉ IP nội bộ / loopback (127.0.0.1, 10.x, 192.168.x) không hợp lệ."
                        },
                        color = JapanRed,
                        fontSize = rSp(11f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(gapMed)) {
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text(if (isEn) "Port" else "Cổng (Port)", fontSize = rSp(11f)) },
                        singleLine = true,
                        shape = RoundedCornerShape(btnCorner),
                        modifier = Modifier
                            .weight(1f)
                            .gamepadTextFieldFocus(cornerRadius = btnCorner),
                        colors = fieldColors
                    )

                    OutlinedTextField(
                        value = protocol,
                        onValueChange = { protocol = it.lowercase() },
                        label = {
                            Text(
                                text = if (isEn) "Protocol (http/socks5)" else "Giao thức (http/socks5)",
                                fontSize = rSp(11f)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(btnCorner),
                        modifier = Modifier
                            .weight(1f)
                            .gamepadTextFieldFocus(cornerRadius = btnCorner),
                        colors = fieldColors
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (isEn) "Description / Notes" else "Ghi chú mô tả", fontSize = rSp(11f)) },
                    singleLine = true,
                    shape = RoundedCornerShape(btnCorner),
                    modifier = Modifier
                        .fillMaxWidth()
                        .gamepadTextFieldFocus(cornerRadius = btnCorner),
                    colors = fieldColors
                )

                Row(horizontalArrangement = Arrangement.spacedBy(gapMed)) {
                    OutlinedTextField(
                        value = speedMbps,
                        onValueChange = { speedMbps = it },
                        label = {
                            Text(
                                text = if (isEn) "Estimated Speed (Mbps)" else "Tốc độ ước tính (Mbps)",
                                fontSize = rSp(11f)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(btnCorner),
                        modifier = Modifier
                            .weight(1f)
                            .gamepadTextFieldFocus(cornerRadius = btnCorner),
                        colors = fieldColors
                    )

                    OutlinedTextField(
                        value = countryCode,
                        onValueChange = { newCc ->
                            userEditedCountry = newCc.isNotBlank()
                            val cleanCc = newCc.trim().uppercase()
                            countryCode = cleanCc
                            azureEndpoint = GeoIpDetector.getAzureEndpointForCountry(cleanCc)
                        },
                        label = {
                            Text(
                                text = if (isEn) "Country ISO (Auto/Manual)" else "Mã quốc gia (Tự nhận/Sửa tay)",
                                fontSize = rSp(11f)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(btnCorner),
                        modifier = Modifier
                            .weight(1f)
                            .gamepadTextFieldFocus(cornerRadius = btnCorner),
                        colors = fieldColors
                    )
                }

                OutlinedTextField(
                    value = azureEndpoint,
                    onValueChange = { azureEndpoint = it },
                    label = {
                        Text(
                            text = if (isEn) "Azure Endpoint (JP/KR only)" else "Azure Endpoint (Chỉ có cho JP/KR)",
                            fontSize = rSp(11f)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(btnCorner),
                    modifier = Modifier
                        .fillMaxWidth()
                        .gamepadTextFieldFocus(cornerRadius = btnCorner),
                    colors = fieldColors
                )
            }
        }

        @Composable
        fun AdaptivePopupScope.ActionsPanel() {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(gapMed)
            ) {
                AdaptiveFlatActionButton(
                    onClick = triggerTest,
                    enabled = !isTesting && trimmedHost.isNotBlank(),
                    isPrimary = false,
                    borderColor = XboxNeonGreen,
                    contentColor = XboxNeonGreen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(rDp(15f)),
                            color = XboxNeonGreen,
                            strokeWidth = rDp(2f)
                        )
                    } else {
                        Text(
                            text = if (isEn) "Test Connection" else "Kiểm tra kết nối",
                            color = XboxNeonGreen,
                            fontSize = rSp(12f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                testResult?.let { msg ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(chipCorner),
                        color = XboxSurface,
                        border = BorderStroke(
                            borderThin,
                            if (msg.startsWith("✅")) XboxNeonGreen else JapanRed
                        )
                    ) {
                        Text(
                            text = msg,
                            color = if (msg.startsWith("✅")) XboxNeonGreen else JapanRed,
                            fontSize = rSp(11.2f),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = padInner, vertical = gapSmall)
                        )
                    }
                }

                AdaptiveFlatActionButton(
                    onClick = triggerSave,
                    enabled = canSave,
                    isPrimary = true,
                    gamepadHint = "A",
                    focusRequester = saveFocusRequester,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isEn) "Save & Use" else "Lưu và dùng",
                        color = if (canSave) Color(0xFF03140B) else TextSecondary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = rSp(12.5f)
                    )
                }

                AdaptiveFlatActionButton(
                    onClick = onDismiss,
                    isPrimary = false,
                    gamepadHint = "B",
                    focusRequester = cancelFocusRequester,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isEn) "Cancel" else "Hủy",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = rSp(12.2f)
                    )
                }
            }
        }

        AdaptiveTwoColumnLayout(
            leftWeight = 1.24f,
            rightWeight = 0.76f,
            leftColumn = {
                HeaderBlock()
                FormFields(maxFormHeight = winH * 0.68f)
            },
            rightColumn = {
                ActionsPanel()
            },
            portraitColumn = {
                HeaderBlock()
                FormFields(maxFormHeight = winH * 0.52f)
                ActionsPanel()
            }
        )
    }
}

@Composable
internal fun ServerDetailDialog(
    server: ServerLocation,
    measurement: EndpointPingMeasurement?,
    pingMs: Int,
    unlockStatus: RegionUnlockStatus,
    isEn: Boolean,
    onDismiss: () -> Unit
) {
    val config by SettingsRepository.config.collectAsState()
    val closeBtnFocusRequester = remember { FocusRequester() }
    val detailBadge = measurement?.formatDisplayBadge(isEn = isEn, showPort = true)
        ?: if (pingMs > 0) "${pingMs}ms (:${server.port})" else "${config.tr("Chưa đo", "Not tested")} (:${server.port})"
    val unlockText = when (unlockStatus) {
        RegionUnlockStatus.UNLOCKED -> config.tr("Đã mở khóa", "Unlocked")
        RegionUnlockStatus.FAILED -> config.tr("Không mở khóa", "Unlock Failed")
        RegionUnlockStatus.UNTESTED -> config.tr("Chưa thử mở khóa", "Untested")
        RegionUnlockStatus.WRONG_REGION -> config.tr("Sai vùng", "Wrong Region")
    }

    AdaptivePopupFrame(
        onDismissRequest = onDismiss,
        dialogTestTag = "server_detail_dialog",
        defaultFocusRequester = closeBtnFocusRequester,
        wideWidthFraction = 0.78f,
        portraitWidthFraction = 0.90f
    ) {
        val infoScroll = rememberScrollState()

        @Composable
        fun AdaptivePopupScope.ServerInfoSection(maxInfoHeight: Dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxInfoHeight)
                    .verticalScroll(infoScroll),
                verticalArrangement = Arrangement.spacedBy(gapSmall)
            ) {
                Text(
                    text = "${server.flag} ${server.name}",
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = rSp(16f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(sectionCorner),
                    color = XboxSurface,
                    border = BorderStroke(borderThin, XboxCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(padInner),
                        verticalArrangement = Arrangement.spacedBy(gapSmall)
                    ) {
                        Text(
                            text = "${if (isEn) "Address & Port" else "Địa chỉ & Cổng"}: ${server.ip}:${server.port} (${server.protocol.uppercase()})",
                            color = TextPrimary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = rSp(11.8f)
                        )
                        Text(
                            text = "${if (isEn) "Measured Latency" else "Độ trễ đo thực tế"}: $detailBadge",
                            color = XboxNeonGreen,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = rSp(12f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${if (isEn) "Region Unlock Status" else "Trạng thái mở khóa vùng"}: $unlockText",
                            color = TextSecondary,
                            fontSize = rSp(11.8f)
                        )
                        Text(
                            text = "${if (isEn) "Check Status" else "Trạng thái kiểm tra"}: ${server.localizedSafetyBadge(isEn)}",
                            color = TextSecondary,
                            fontSize = rSp(11.8f)
                        )
                        Text(
                            text = "${if (isEn) "Description" else "Mô tả"}: ${server.localizedDescription(isEn)}",
                            color = TextSecondary,
                            fontSize = rSp(11.8f)
                        )
                    }
                }
            }
        }

        @Composable
        fun AdaptivePopupScope.CloseButtonPanel() {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(gapMed),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AdaptiveFlatActionButton(
                    onClick = onDismiss,
                    isPrimary = true,
                    gamepadHint = "B",
                    focusRequester = closeBtnFocusRequester,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = config.tr("Đóng", "Close"),
                        color = Color(0xFF03140B),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = rSp(12.5f)
                    )
                }
            }
        }

        AdaptiveTwoColumnLayout(
            leftWeight = 1.25f,
            rightWeight = 0.75f,
            leftColumn = {
                ServerInfoSection(maxInfoHeight = winH * 0.74f)
            },
            rightColumn = {
                CloseButtonPanel()
            },
            portraitColumn = {
                ServerInfoSection(maxInfoHeight = winH * 0.62f)
                CloseButtonPanel()
            }
        )
    }
}
