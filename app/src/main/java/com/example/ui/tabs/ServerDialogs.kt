package com.example.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import com.example.ui.components.gamepadActionButton
import com.example.ui.components.gamepadControllerScope
import com.example.ui.components.gamepadTextFieldFocus
import com.example.ui.theme.JapanRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant
import com.example.vpn.EndpointPingMeasurement
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
    val config by com.example.data.SettingsRepository.config.collectAsState()
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

    val trimmedHost = host.trim()
    val isPrivateIp = remember(trimmedHost) {
        trimmedHost.isNotEmpty() && com.example.vpn.GeoIpDetector.isPrivateOrLoopbackIp(trimmedHost)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.gamepadControllerScope(
            enabled = true,
            defaultFocusRequester = cancelFocusRequester,
            autoRequestDefaultFocus = true,
            attachWindowMotionListener = true,
            onBackPress = onDismiss
        ),
        title = {
            Text(
                config.tr("Thêm Proxy Tùy Chỉnh", "Add Custom Proxy"),
                color = TextPrimary,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier.verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isEn) {
                        "Enter a public proxy server IP address and port"
                    } else {
                        "Nhập địa chỉ IP máy chủ công cộng và cổng kết nối riêng"
                    },
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isEn) "Display Name (Optional)" else "Tên gợi nhớ (Tùy chọn)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .gamepadTextFieldFocus(cornerRadius = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = XboxNeonGreen
                    )
                )

                OutlinedTextField(
                    value = host,
                    onValueChange = { newHost ->
                        host = newHost
                        val cleanIp = newHost.trim()
                        if (!userEditedCountry) {
                            val detectedCc = com.example.vpn.GeoIpDetector.resolveIpCountryCode(cleanIp, allowNetworkFallback = false)
                            countryCode = detectedCc
                            azureEndpoint = com.example.vpn.GeoIpDetector.getAzureEndpointForCountry(detectedCc)
                        }
                    },
                    label = { Text(if (isEn) "Public IPv4 Address" else "Địa chỉ IPv4 công cộng") },
                    placeholder = { Text(if (isEn) "e.g., 138.199.21.239" else "vd: 138.199.21.239") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .gamepadTextFieldFocus(cornerRadius = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = XboxNeonGreen
                    )
                )

                if (isPrivateIp) {
                    Text(
                        text = if (isEn) {
                            "⚠️ Private or loopback IP (127.0.0.1, 10.x, 192.168.x) cannot unlock Xbox Cloud."
                        } else {
                            "⚠️ Địa chỉ IP nội bộ / loopback (127.0.0.1, 10.x, 192.168.x) không hợp lệ."
                        },
                        color = JapanRed,
                        fontSize = 12.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text(if (isEn) "Port" else "Cổng (Port)") },
                        modifier = Modifier
                            .weight(1f)
                            .gamepadTextFieldFocus(cornerRadius = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = XboxNeonGreen
                        )
                    )

                    OutlinedTextField(
                        value = protocol,
                        onValueChange = { protocol = it.lowercase() },
                        label = { Text(if (isEn) "Protocol (http/socks5)" else "Giao thức (http/socks5)") },
                        modifier = Modifier
                            .weight(1f)
                            .gamepadTextFieldFocus(cornerRadius = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = XboxNeonGreen
                        )
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (isEn) "Description / Notes" else "Ghi chú mô tả") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .gamepadTextFieldFocus(cornerRadius = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = XboxNeonGreen
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = speedMbps,
                        onValueChange = { speedMbps = it },
                        label = { Text(if (isEn) "Estimated Speed (Mbps)" else "Tốc độ ước tính (Mbps)") },
                        modifier = Modifier
                            .weight(1f)
                            .gamepadTextFieldFocus(cornerRadius = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = XboxNeonGreen
                        )
                    )

                    OutlinedTextField(
                        value = countryCode,
                        onValueChange = { newCc ->
                            userEditedCountry = newCc.isNotBlank()
                            val cleanCc = newCc.trim().uppercase()
                            countryCode = cleanCc
                            azureEndpoint = com.example.vpn.GeoIpDetector.getAzureEndpointForCountry(cleanCc)
                        },
                        label = { Text(if (isEn) "Country ISO (Auto/Manual)" else "Mã quốc gia (Tự nhận/Sửa tay)") },
                        modifier = Modifier
                            .weight(1f)
                            .gamepadTextFieldFocus(cornerRadius = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = XboxNeonGreen
                        )
                    )
                }

                OutlinedTextField(
                    value = azureEndpoint,
                    onValueChange = { azureEndpoint = it },
                    label = { Text(if (isEn) "Azure Endpoint (JP/KR only)" else "Azure Endpoint (Chỉ có cho JP/KR)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .gamepadTextFieldFocus(cornerRadius = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = XboxNeonGreen
                    )
                )

                // Test connection button
                val triggerTest: () -> Unit = {
                    if (!isTesting) {
                        isTesting = true
                        testResult = null
                        scope.launch(Dispatchers.IO) {
                            val p = port.toIntOrNull() ?: 8080
                            val ok = !com.example.vpn.GeoIpDetector.isPrivateOrLoopbackIp(trimmedHost) &&
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
                Button(
                    onClick = triggerTest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 46.dp)
                        .gamepadActionButton(
                            cornerRadius = 12.dp,
                            onClick = triggerTest
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = XboxSurface)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = XboxNeonGreen, strokeWidth = 2.dp)
                    } else {
                        Text(if (isEn) "Test Connection" else "Kiểm tra kết nối", color = XboxNeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                testResult?.let { msg ->
                    Text(text = msg, color = if (msg.startsWith("✅")) XboxNeonGreen else JapanRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            val canSave = trimmedHost.isNotBlank() && !isPrivateIp
            val triggerSave: () -> Unit = {
                if (canSave) {
                    val p = port.toIntOrNull() ?: 8080
                    val speed = speedMbps.toIntOrNull() ?: 200
                    val cc = countryCode.trim().uppercase().ifBlank {
                        com.example.vpn.GeoIpDetector.resolveIpCountryCode(trimmedHost, allowNetworkFallback = false)
                    }
                    val resolvedFlag = com.example.vpn.GeoIpDetector.getFlagEmoji(cc)
                    val defaultDisplayName = com.example.vpn.GeoIpDetector.formatServerDisplayName(cc, trimmedHost, isEn)
                    val resolvedAzure = azureEndpoint.trim().ifBlank {
                        com.example.vpn.GeoIpDetector.getAzureEndpointForCountry(cc)
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
            Button(
                onClick = triggerSave,
                enabled = canSave,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 44.dp)
                    .gamepadActionButton(
                        enabled = canSave,
                        cornerRadius = 12.dp,
                        onClick = triggerSave
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = XboxNeonGreen,
                    contentColor = androidx.compose.ui.graphics.Color(0xFF03140B)
                )
            ) {
                Text(if (isEn) "Save & Use" else "Lưu và dùng", color = androidx.compose.ui.graphics.Color(0xFF03140B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 44.dp)
                    .gamepadActionButton(
                        cornerRadius = 8.dp,
                        focusRequester = cancelFocusRequester,
                        onClick = onDismiss
                    )
            ) {
                Text(if (isEn) "Cancel" else "Hủy", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        shape = RoundedCornerShape(22.dp),
        containerColor = XboxSurfaceVariant
    )
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
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val closeBtnFocusRequester = remember { FocusRequester() }
    val detailBadge = measurement?.formatDisplayBadge(isEn = isEn, showPort = true)
        ?: if (pingMs > 0) "${pingMs}ms (:${server.port})" else "${config.tr("Chưa đo", "Not tested")} (:${server.port})"
    val unlockText = when (unlockStatus) {
        RegionUnlockStatus.UNLOCKED -> config.tr("Đã mở khóa", "Unlocked")
        RegionUnlockStatus.FAILED -> config.tr("Không mở khóa", "Unlock Failed")
        RegionUnlockStatus.UNTESTED -> config.tr("Chưa thử mở khóa", "Untested")
        RegionUnlockStatus.WRONG_REGION -> config.tr("Sai vùng", "Wrong Region")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.gamepadControllerScope(
            enabled = true,
            defaultFocusRequester = closeBtnFocusRequester,
            autoRequestDefaultFocus = true,
            attachWindowMotionListener = true,
            onBackPress = onDismiss
        ),
        title = {
            Text(
                text = "${server.flag} ${server.name}",
                color = TextPrimary,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "${if (isEn) "Address & Port" else "Địa chỉ & Cổng"}: ${server.ip}:${server.port} (${server.protocol.uppercase()})",
                    color = TextPrimary,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.5.sp
                )
                Text(
                    text = "${if (isEn) "Measured Latency" else "Độ trễ đo thực tế"}: $detailBadge",
                    color = XboxNeonGreen,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${if (isEn) "Region Unlock Status" else "Trạng thái mở khóa vùng"}: $unlockText",
                    color = TextSecondary,
                    fontSize = 12.5.sp
                )
                Text(
                    text = "${if (isEn) "Check Status" else "Trạng thái kiểm tra"}: ${server.localizedSafetyBadge(isEn)}",
                    color = TextSecondary,
                    fontSize = 12.5.sp
                )
                Text(
                    text = "${if (isEn) "Description" else "Mô tả"}: ${server.localizedDescription(isEn)}",
                    color = TextSecondary,
                    fontSize = 12.5.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 44.dp)
                    .gamepadActionButton(
                        cornerRadius = 12.dp,
                        focusRequester = closeBtnFocusRequester,
                        onClick = onDismiss
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = XboxNeonGreen,
                    contentColor = androidx.compose.ui.graphics.Color(0xFF03140B)
                )
            ) {
                Text(config.tr("Đóng", "Close"), color = androidx.compose.ui.graphics.Color(0xFF03140B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        shape = RoundedCornerShape(22.dp),
        containerColor = XboxSurfaceVariant
    )
}
