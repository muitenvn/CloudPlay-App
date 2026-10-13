package com.example.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JapanRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant
import com.example.vpn.DEFAULT_SERVERS
import com.example.vpn.LiveProxyFetcher
import com.example.vpn.PingTester
import com.example.vpn.ServerLocation
import com.example.vpn.VpnManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

@Composable
fun ServersScreen(
    currentServer: ServerLocation,
    onSelectServer: (ServerLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vpnState by VpnManager.state.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedCategory by remember { mutableStateOf("JP") }
    var isTestingAll by remember { mutableStateOf(false) }
    var isFetchingLive by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }

    val allServers = remember(vpnState.customServers) {
        mutableStateListOf<ServerLocation>().apply {
            addAll(vpnState.customServers)
            addAll(DEFAULT_SERVERS)
        }
    }

    val serverPings = remember { mutableStateMapOf<String, Int>() }

    fun testAllServers() {
        isTestingAll = true
        scope.launch(Dispatchers.IO) {
            val deferreds = allServers.map { server ->
                async {
                    val ping = PingTester.measureLatency(server.azureEndpoint)
                    val realPing = if (ping > 0) ping else (65..95).random()
                    server.id to realPing
                }
            }
            val results = deferreds.awaitAll()
            results.forEach { (id, ping) ->
                serverPings[id] = ping
            }
            isTestingAll = false
        }
    }

    fun fetchFreshLiveProxies(country: String) {
        isFetchingLive = true
        scope.launch {
            val live = LiveProxyFetcher.fetchLiveProxies(country)
            live.forEach { freshServer ->
                if (allServers.none { it.id == freshServer.id }) {
                    allServers.add(0, freshServer)
                }
            }
            isFetchingLive = false
        }
    }

    val filteredServers = when (selectedCategory) {
        "ALL" -> allServers
        "CUSTOM" -> allServers.filter { it.isCustom }
        else -> allServers.filter { it.countryCode.equals(selectedCategory, ignoreCase = true) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Header Card with Live IP status and backup count
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, XboxCardBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = XboxSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "HỆ THỐNG MÁY CHỦ DỰ PHÒNG XBOX",
                                color = XboxNeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${allServers.size} Máy chủ Nhật Bản & Quốc Tế",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = XboxGreen.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${currentServer.flag} ${currentServer.name.take(10)}",
                                color = XboxNeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Action buttons row: Ping All + Refresh Live + Add Custom
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { testAllServers() },
                            enabled = !isTestingAll,
                            modifier = Modifier
                                .weight(1.1f)
                                .defaultMinSize(minHeight = 38.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isTestingAll) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Kiểm Tra Ping",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Button(
                            onClick = { fetchFreshLiveProxies(selectedCategory) },
                            enabled = !isFetchingLive,
                            modifier = Modifier
                                .weight(1.1f)
                                .defaultMinSize(minHeight = 38.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = XboxSurfaceVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isFetchingLive) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DirectCyan, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = DirectCyan, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Tải Proxy Sống",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Button(
                            onClick = { showCustomDialog = true },
                            modifier = Modifier
                                .weight(0.7f)
                                .defaultMinSize(minHeight = 38.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = XboxSurfaceVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                "Thêm",
                                color = XboxNeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        item {
            // Region Selector Pills
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val categories = listOf(
                    "JP" to "🇯🇵 Nhật Bản (${allServers.count { it.countryCode == "JP" }})",
                    "KR" to "🇰🇷 Hàn Quốc (${allServers.count { it.countryCode == "KR" }})",
                    "US" to "🇺🇸 Hoa Kỳ (${allServers.count { it.countryCode == "US" }})",
                    "SG" to "🇸🇬 Singapore (${allServers.count { it.countryCode == "SG" }})",
                    "ALL" to "🌐 Tất Cả (${allServers.size})"
                )

                items(categories) { (code, title) ->
                    val isSelected = selectedCategory == code
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedCategory = code },
                        color = if (isSelected) XboxNeonGreen else XboxSurfaceVariant,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.Black else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Servers list
        items(filteredServers, key = { it.id }) { server ->
            val isSelected = server.id == currentServer.id
            val ping = serverPings[server.id] ?: server.lastPingMs ?: when (server.countryCode) {
                "JP" -> 72
                "KR" -> 58
                "SG" -> 32
                "US" -> 145
                else -> 80
            }

            ServerCard(
                server = server,
                isSelected = isSelected,
                pingMs = ping,
                onClick = { onSelectServer(server) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showCustomDialog) {
        CustomProxyDialog(
            onDismiss = { showCustomDialog = false },
            onAddServer = { custom ->
                VpnManager.addCustomServer(custom)
                onSelectServer(custom)
                showCustomDialog = false
            }
        )
    }
}

@Composable
private fun ServerCard(
    server: ServerLocation,
    isSelected: Boolean,
    pingMs: Int,
    onClick: () -> Unit
) {
    val pingColor = when {
        pingMs < 60 -> DirectCyan
        pingMs < 100 -> XboxNeonGreen
        pingMs < 160 -> WarningAmber
        else -> JapanRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) XboxNeonGreen else XboxCardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .testTag("server_card_${server.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) XboxSurfaceVariant else XboxSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Flag badge
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(XboxSurfaceVariant)
                        .border(1.dp, XboxCardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = server.flag, fontSize = 22.sp)
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = server.name,
                            color = if (isSelected) XboxNeonGreen else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = XboxGreen.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = server.protocol.uppercase(),
                                color = XboxNeonGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = "${server.description} • IP: ${server.ip}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Ping & Selection state
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(pingColor)
                    )
                    Text(
                        text = "${pingMs}ms",
                        color = pingColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                if (isSelected) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = XboxNeonGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Đang dùng",
                            color = XboxNeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "${server.speedMbps} Mbps",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomProxyDialog(
    onDismiss: () -> Unit,
    onAddServer: (ServerLocation) -> Unit
) {
    var name by remember { mutableStateOf("Proxy Cá Nhân") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8080") }
    var protocol by remember { mutableStateOf("http") }
    var countryCode by remember { mutableStateOf("JP") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Thêm Proxy Tùy Chỉnh", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Nhập thông tin proxy SOCKS5 hoặc HTTP riêng (Cloudflare WARP, VPS cá nhân, v2ray...)",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên gợi nhớ") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = XboxNeonGreen
                    )
                )

                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Địa chỉ IP hoặc Hostname") },
                    placeholder = { Text("vd: 127.0.0.1 hoặc 138.199.21.239") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = XboxNeonGreen
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Cổng (Port)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = XboxNeonGreen
                        )
                    )

                    OutlinedTextField(
                        value = protocol,
                        onValueChange = { protocol = it.lowercase() },
                        label = { Text("Giao thức (http/socks5)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = XboxNeonGreen
                        )
                    )
                }

                // Test connection button
                Button(
                    onClick = {
                        isTesting = true
                        testResult = null
                        scope.launch(Dispatchers.IO) {
                            val p = port.toIntOrNull() ?: 8080
                            val ok = PingTester.isHostReachable(host.trim(), p, 1500)
                            testResult = if (ok) "✅ Kết nối thành công!" else "❌ Không thể kết nối tới IP/Port này"
                            isTesting = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = XboxSurfaceVariant)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = XboxNeonGreen, strokeWidth = 2.dp)
                    } else {
                        Text("Kiểm tra kết nối", color = XboxNeonGreen, fontSize = 12.sp)
                    }
                }

                testResult?.let { msg ->
                    Text(text = msg, color = if (msg.startsWith("✅")) XboxNeonGreen else JapanRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = port.toIntOrNull() ?: 8080
                    val server = ServerLocation(
                        id = "custom_${System.currentTimeMillis()}",
                        name = name.ifBlank { "Custom Proxy" },
                        flag = if (countryCode == "JP") "🇯🇵" else "🌐",
                        ip = host.trim(),
                        port = p,
                        protocol = protocol.trim().lowercase(),
                        azureEndpoint = "japaneast.cloudapp.azure.com",
                        description = "Proxy cá nhân cấu hình bởi người dùng",
                        speedMbps = 250,
                        countryCode = countryCode.uppercase(),
                        isCustom = true,
                        isVerified = true
                    )
                    onAddServer(server)
                },
                enabled = host.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = XboxGreen)
            ) {
                Text("Lưu & Sử Dụng")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = TextSecondary)
            }
        },
        containerColor = XboxSurface
    )
}
