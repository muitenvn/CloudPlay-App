package com.example.ui.tabs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JapanRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceElevated
import com.example.ui.theme.XboxSurfaceVariant
import com.example.ui.theme.animateXboxIntAsState
import com.example.ui.theme.rememberXboxMotionEnabled
import com.example.ui.theme.xboxPressScale
import com.example.ui.theme.xboxSlideFadeIn
import com.example.data.SettingsRepository
import com.example.vpn.DEFAULT_SERVERS
import com.example.vpn.LiveProxyFetcher
import com.example.vpn.PingTester
import com.example.vpn.RegionUnlockStatus
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
    val config by SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    val motionEnabled = rememberXboxMotionEnabled(
        lowEndDeviceMode = config.lowEndDeviceMode,
        gpuLoadReductionEnabled = config.gpuLoadReductionEnabled
    )
    val scope = rememberCoroutineScope()

    var selectedCategory by remember { mutableStateOf("ALL") }
    var isTestingAll by remember { mutableStateOf(false) }
    var isFetchingLive by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }

    val jpLabel = stringResource(R.string.cat_japan)
    val krLabel = stringResource(R.string.cat_korea)
    val usLabel = stringResource(R.string.cat_usa)
    val seaLabel = stringResource(R.string.cat_sea)
    val allLabel = stringResource(R.string.filter_all)

    val allServers = remember(vpnState.customServers) {
        mutableStateListOf<ServerLocation>().apply {
            addAll(vpnState.customServers)
            addAll(DEFAULT_SERVERS)
        }
    }

    val serverPings = remember { mutableStateMapOf<String, Int>() }

    fun testAllServers(autoPickBest: Boolean = false) {
        isTestingAll = true
        scope.launch(Dispatchers.IO) {
            val deferreds = allServers.map { server ->
                async {
                    val ping = PingTester.measureLatency(server.azureEndpoint)
                    val realPing = if (ping > 0) ping else -1
                    server.id to realPing
                }
            }
            val results = deferreds.awaitAll()
            results.forEach { (id, ping) ->
                serverPings[id] = ping
            }
            isTestingAll = false
            if (autoPickBest) {
                val candidates = VpnManager.sortServersByUnlockAndPing(
                    servers = allServers.toList(),
                    pingMap = serverPings.toMap(),
                    records = vpnState.unlockRecords
                )
                val best = candidates.firstOrNull { (serverPings[it.id] ?: -1) > 0 } ?: candidates.firstOrNull()
                if (best != null) {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        onSelectServer(best)
                        val bestPing = serverPings[best.id] ?: -1
                        val pingSuffix = if (bestPing > 0) " (${bestPing}ms)" else ""
                        val msg = if (isEn) {
                            "Selected best server: ${best.flag} ${best.name}$pingSuffix"
                        } else {
                            "Đã chọn máy chủ tốt nhất: ${best.flag} ${best.name}$pingSuffix"
                        }
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun fetchFreshLiveProxies(country: String) {
        isFetchingLive = true
        scope.launch {
            val live = LiveProxyFetcher.fetchLiveProxies(country)
            VpnManager.preCheckProxiesBatch(live)
            live.forEach { freshServer ->
                if (allServers.none { it.id == freshServer.id }) {
                    allServers.add(0, freshServer)
                }
            }
            isFetchingLive = false
        }
    }

    val rawFilteredServers = when (selectedCategory) {
        "ALL" -> allServers.toList()
        "CUSTOM" -> allServers.filter { it.isCustom }
        else -> allServers.filter { it.countryCode.equals(selectedCategory, ignoreCase = true) }
    }

    // 4. SẮP XẾP: xếp "Đã mở khóa" lên đầu, rồi "Chưa thử" theo ping, "Không mở khóa" và "Sai vùng" xuống cuối
    val filteredServers = VpnManager.sortServersByUnlockAndPing(
        servers = rawFilteredServers,
        pingMap = serverPings.toMap(),
        records = vpnState.unlockRecords
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (vpnState.unlockFailPromptServer != null) {
            item(key = "unlock_fail_prompt_banner") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("unlock_fail_prompt_card"),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1A0D10),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, JapanRed.copy(alpha = 0.75f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if (isEn) "This server cannot unlock the region, switch to another server?" else "Máy chủ này không mở khóa được, đổi sang máy chủ khác?",
                                color = TextPrimary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${vpnState.unlockFailPromptServer?.flag ?: ""} ${vpnState.unlockFailPromptServer?.name ?: ""}",
                                color = JapanRed,
                                fontSize = 10.5.sp
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    val switched = VpnManager.confirmSwitchToNextBestServer(context)
                                    if (switched != null) {
                                        onSelectServer(switched)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 36.dp)
                                    .testTag("confirm_switch_best_server_button")
                            ) {
                                Text(
                                    text = if (isEn) "Confirm" else "Xác nhận",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            TextButton(
                                onClick = { VpnManager.dismissUnlockFailPrompt() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isEn) "Dismiss" else "Đóng",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            // Global Routing Radar Header Deck
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .xboxSlideFadeIn(enabled = motionEnabled, key = "servers_header_deck", durationMillis = 170)
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        1.2.dp,
                        Brush.horizontalGradient(
                            listOf(XboxNeonGreen.copy(alpha = 0.55f), XboxCardBorder, DirectCyan.copy(alpha = 0.55f))
                        ),
                        RoundedCornerShape(22.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = XboxSurface.copy(alpha = 0.96f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(XboxNeonGreen.copy(alpha = 0.22f), DirectCyan.copy(alpha = 0.14f))
                                        )
                                    )
                                    .border(1.dp, XboxNeonGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = XboxNeonGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = stringResource(R.string.servers_title).uppercase(),
                                    color = XboxNeonGreen,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${allServers.size} " + stringResource(R.string.servers_subtitle),
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = XboxGreen.copy(alpha = 0.22f),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxNeonGreen.copy(alpha = 0.55f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = currentServer.flag, fontSize = 12.sp)
                                Text(
                                    text = currentServer.name.take(11),
                                    color = XboxNeonGreen,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Compact Selected Server & Unlock Telemetry Strip (fixes missing weight(1f) vertical squeeze)
                    val currentPingMs = serverPings[currentServer.id] ?: currentServer.lastPingMs ?: -1
                    val currentUnlockStatus = VpnManager.getServerUnlockStatus(currentServer, vpnState.unlockRecords)
                    val currentUnlockText = when (currentUnlockStatus) {
                        RegionUnlockStatus.UNLOCKED -> if (isEn) "Unlocked" else "Đã mở khóa"
                        RegionUnlockStatus.FAILED -> if (isEn) "Unlock Failed" else "Không mở khóa"
                        RegionUnlockStatus.UNTESTED -> if (isEn) "Ready" else "Sẵn sàng"
                        RegionUnlockStatus.WRONG_REGION -> if (isEn) "Wrong Region" else "Sai vùng"
                    }
                    val currentUnlockColor = when (currentUnlockStatus) {
                        RegionUnlockStatus.UNLOCKED -> XboxNeonGreen
                        RegionUnlockStatus.FAILED -> JapanRed
                        RegionUnlockStatus.UNTESTED -> DirectCyan
                        RegionUnlockStatus.WRONG_REGION -> WarningAmber
                    }
                    val currentPingText = when {
                        isTestingAll -> if (isEn) "Testing ping..." else "Đang đo ping..."
                        currentPingMs > 0 -> "Ping ${currentPingMs}ms"
                        else -> if (isEn) "Not tested" else "Chưa đo ping"
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF08100C),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = if (isEn) {
                                        "Selected: ${currentServer.flag} ${currentServer.name}"
                                    } else {
                                        "Đang chọn: ${currentServer.flag} ${currentServer.name}"
                                    },
                                    color = TextPrimary,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = currentUnlockText,
                                        color = currentUnlockColor,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "•",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = currentPingText,
                                        color = if (currentPingMs in 1..99) XboxNeonGreen else DirectCyan,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = XboxGreen.copy(alpha = 0.25f),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxNeonGreen.copy(alpha = 0.65f)),
                                modifier = Modifier
                                    .xboxPressScale(enabled = motionEnabled, pressedScale = 0.95f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = !isTestingAll) { testAllServers(autoPickBest = true) }
                                    .testTag("auto_pick_best_server_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = XboxNeonGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (isEn) "Auto Best" else "Chọn tối ưu",
                                        color = XboxNeonGreen,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Action buttons row: Đo ping + Cập nhật + Thêm (full readable labels on 360dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { testAllServers(autoPickBest = false) },
                            enabled = !isTestingAll,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 42.dp)
                                .xboxPressScale(enabled = motionEnabled, pressedScale = 0.95f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                            shape = RoundedCornerShape(11.dp)
                        ) {
                            if (isTestingAll) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isEn) "Pinging..." else "Đang đo ping",
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isEn) "Ping Test" else "Đo ping",
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            }
                        }

                        Button(
                            onClick = { fetchFreshLiveProxies(selectedCategory) },
                            enabled = !isFetchingLive,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 42.dp)
                                .xboxPressScale(enabled = motionEnabled, pressedScale = 0.95f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = XboxSurfaceElevated),
                            shape = RoundedCornerShape(11.dp)
                        ) {
                            if (isFetchingLive) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = DirectCyan, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isEn) "Updating..." else "Đang tải...",
                                    color = TextPrimary,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            } else {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = DirectCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isEn) "Update" else "Cập nhật",
                                    color = TextPrimary,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            }
                        }

                        Button(
                            onClick = { showCustomDialog = true },
                            modifier = Modifier
                                .weight(0.85f)
                                .defaultMinSize(minHeight = 42.dp)
                                .xboxPressScale(enabled = motionEnabled, pressedScale = 0.95f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = XboxSurfaceElevated),
                            shape = RoundedCornerShape(11.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isEn) "Add" else "Thêm",
                                color = XboxNeonGreen,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
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
                    "JP" to "🇯🇵 $jpLabel (${allServers.count { it.countryCode == "JP" }})",
                    "KR" to "🇰🇷 $krLabel (${allServers.count { it.countryCode == "KR" }})",
                    "US" to "🇺🇸 $usLabel (${allServers.count { it.countryCode == "US" }})",
                    "CUSTOM" to if (isEn) "⚙️ Custom (${allServers.count { it.isCustom }})" else "⚙️ Tùy Chỉnh (${allServers.count { it.isCustom }})",
                    "ALL" to "🌐 $allLabel (${allServers.size})"
                )

                items(categories) { (code, title) ->
                    val isSelected = selectedCategory == code
                    val regionPillBg by animateColorAsState(
                        targetValue = if (isSelected) XboxNeonGreen else XboxSurfaceVariant.copy(alpha = 0.88f),
                        animationSpec = tween(durationMillis = if (motionEnabled) 150 else 0),
                        label = "region_pill_bg"
                    )
                    Surface(
                        modifier = Modifier
                            .xboxPressScale(enabled = motionEnabled, pressedScale = 0.95f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedCategory = code },
                        color = regionPillBg,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) XboxNeonGreen else XboxCardBorder
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.Black else TextSecondary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // Servers list
        val recommendedServerId = filteredServers.firstOrNull { server ->
            val p = serverPings[server.id] ?: server.lastPingMs ?: -1
            val st = VpnManager.getServerUnlockStatus(server, vpnState.unlockRecords)
            p > 0 && st != RegionUnlockStatus.FAILED && st != RegionUnlockStatus.WRONG_REGION
        }?.id ?: filteredServers.firstOrNull()?.id

        items(filteredServers, key = { it.id }) { server ->
            val isSelected = server.id == currentServer.id
            val ping = serverPings[server.id] ?: server.lastPingMs ?: -1
            val unlockStatus = VpnManager.getServerUnlockStatus(server, vpnState.unlockRecords)
            val isRecommended = server.id == recommendedServerId

            ServerCard(
                server = server,
                isSelected = isSelected,
                isRecommended = isRecommended,
                pingMs = ping,
                unlockStatus = unlockStatus,
                isEn = isEn,
                onClick = { onSelectServer(server) },
                onDelete = if (server.isCustom) {
                    {
                        VpnManager.removeCustomServer(server.id)
                        allServers.removeAll { it.id == server.id }
                    }
                } else null
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
    isRecommended: Boolean = false,
    pingMs: Int,
    unlockStatus: RegionUnlockStatus,
    isEn: Boolean = false,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val config by SettingsRepository.config.collectAsState()
    val motionEnabled = rememberXboxMotionEnabled(
        lowEndDeviceMode = config.lowEndDeviceMode,
        gpuLoadReductionEnabled = config.gpuLoadReductionEnabled
    )
    val animatedPingMs by animateXboxIntAsState(
        targetValue = if (pingMs > 0) pingMs else 0,
        motionEnabled = motionEnabled,
        label = "server_ping_${server.id}"
    )
    val targetPingColor = when {
        pingMs <= 0 -> Color.Gray
        pingMs < 60 -> DirectCyan
        pingMs < 100 -> XboxNeonGreen
        pingMs < 160 -> WarningAmber
        else -> JapanRed
    }
    val pingColor by animateColorAsState(
        targetValue = targetPingColor,
        animationSpec = tween(durationMillis = if (motionEnabled) 170 else 0),
        label = "server_ping_color"
    )
    val cardContainerColor by animateColorAsState(
        targetValue = if (isSelected) XboxSurfaceVariant.copy(alpha = 0.96f) else XboxSurface.copy(alpha = 0.94f),
        animationSpec = tween(durationMillis = if (motionEnabled) 170 else 0),
        label = "server_card_bg"
    )

    val activeBars = when {
        pingMs <= 0 -> 2
        pingMs < 60 -> 4
        pingMs < 110 -> 3
        pingMs < 170 -> 2
        else -> 1
    }

    // 3. HIỂN THỊ: mỗi thẻ máy chủ có nhãn nhỏ: "Đã mở khóa" (xanh), "Không mở khóa" (đỏ), "Chưa thử" (xám), "Sai vùng" (vàng)
    val unlockLabel = when (unlockStatus) {
        RegionUnlockStatus.UNLOCKED -> if (isEn) "Unlocked" else "Đã mở khóa"
        RegionUnlockStatus.FAILED -> if (isEn) "Unlock Failed" else "Không mở khóa"
        RegionUnlockStatus.UNTESTED -> if (isEn) "Untested" else "Chưa thử"
        RegionUnlockStatus.WRONG_REGION -> if (isEn) "Wrong Region" else "Sai vùng"
    }
    val unlockBadgeColor = when (unlockStatus) {
        RegionUnlockStatus.UNLOCKED -> XboxNeonGreen
        RegionUnlockStatus.FAILED -> JapanRed
        RegionUnlockStatus.UNTESTED -> Color(0xFF9CA3AF)
        RegionUnlockStatus.WRONG_REGION -> WarningAmber
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .xboxSlideFadeIn(enabled = motionEnabled, key = server.id, durationMillis = 165)
            .xboxPressScale(enabled = motionEnabled, pressedScale = 0.97f)
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = if (isSelected) 1.6.dp else 1.dp,
                brush = if (isSelected) {
                    Brush.horizontalGradient(listOf(XboxNeonGreen, DirectCyan))
                } else {
                    Brush.horizontalGradient(listOf(XboxCardBorder, XboxCardBorder))
                },
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .testTag("server_card_${server.id}"),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 3.dp)
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
                // Flag badge with neon ring when active
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            if (isSelected) XboxNeonGreen.copy(alpha = 0.16f) else XboxSurfaceVariant
                        )
                        .border(
                            1.dp,
                            if (isSelected) XboxNeonGreen else XboxCardBorder,
                            RoundedCornerShape(13.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = server.flag, fontSize = 22.sp)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = server.name,
                        color = if (isSelected) XboxNeonGreen else TextPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (isRecommended) {
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = DirectCyan.copy(alpha = 0.18f),
                                border = androidx.compose.foundation.BorderStroke(0.7.dp, DirectCyan.copy(alpha = 0.65f))
                            ) {
                                Text(
                                    text = if (isEn) "★ BEST" else "★ ĐỀ XUẤT",
                                    color = DirectCyan,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = XboxGreen.copy(alpha = 0.22f),
                            border = androidx.compose.foundation.BorderStroke(0.6.dp, XboxNeonGreen.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = server.protocol.uppercase(),
                                color = XboxNeonGreen,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.5.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = unlockBadgeColor.copy(alpha = 0.16f),
                            border = androidx.compose.foundation.BorderStroke(0.7.dp, unlockBadgeColor.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = unlockLabel,
                                color = unlockBadgeColor,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 5.5.dp, vertical = 1.5.dp)
                            )
                        }
                    }

                    Text(
                        text = "${server.localizedDescription(isEn)} • ${server.ip}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Signal Bars + Ping & Selection state
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 4-Bar Signal Quality Meter
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.height(13.dp)
                        ) {
                            for (bar in 1..4) {
                                val barHeight = (4 + bar * 2).dp
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(
                                            if (bar <= activeBars) pingColor else XboxCardBorder
                                        )
                                )
                            }
                        }

                        Text(
                            text = if (pingMs > 0) "${animatedPingMs}ms" else "--ms",
                            color = pingColor,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }

                    if (isSelected) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = XboxNeonGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isEn) "ACTIVE" else "ĐANG DÙNG",
                                color = XboxNeonGreen,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    } else {
                        val stabilityText = when {
                            pingMs in 1..85 -> if (isEn) "Stable • ${server.speedMbps} Mbps" else "Ổn định • ${server.speedMbps} Mbps"
                            pingMs in 86..150 -> if (isEn) "Good • ${server.speedMbps} Mbps" else "Khá • ${server.speedMbps} Mbps"
                            else -> "${server.speedMbps} Mbps"
                        }
                        Text(
                            text = stabilityText,
                            color = TextSecondary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp
                        )
                    }
                }

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_server_${server.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = if (isEn) "Delete custom proxy" else "Xóa proxy tùy chỉnh",
                            tint = JapanRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    var name by remember { mutableStateOf(if (isEn) "Personal Proxy" else "Proxy Cá Nhân") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8080") }
    var protocol by remember { mutableStateOf("http") }
    var countryCode by remember { mutableStateOf("JP") }
    var description by remember { mutableStateOf(if (isEn) "Custom proxy" else "Proxy cá nhân") }
    var speedMbps by remember { mutableStateOf("250") }
    var azureEndpoint by remember { mutableStateOf("japaneast.cloudapp.azure.com") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEn) "Add Custom Proxy" else "Thêm Proxy Tùy Chỉnh",
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
                        "Enter your custom server address and port"
                    } else {
                        "Nhập địa chỉ máy chủ và cổng kết nối riêng"
                    },
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isEn) "Display Name" else "Tên gợi nhớ") },
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
                    label = { Text(if (isEn) "IP Address or Hostname" else "Địa chỉ IP hoặc Hostname") },
                    placeholder = { Text(if (isEn) "e.g., 127.0.0.1 or 138.199.21.239" else "vd: 127.0.0.1 hoặc 138.199.21.239") },
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
                        label = { Text(if (isEn) "Port" else "Cổng (Port)") },
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
                        label = { Text(if (isEn) "Protocol (http/socks5)" else "Giao thức (http/socks5)") },
                        modifier = Modifier.weight(1f),
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
                    modifier = Modifier.fillMaxWidth(),
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
                        label = { Text(if (isEn) "Speed (Mbps)" else "Tốc độ (Mbps)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = XboxNeonGreen
                        )
                    )

                    OutlinedTextField(
                        value = countryCode,
                        onValueChange = { countryCode = it.uppercase() },
                        label = { Text(if (isEn) "Country (JP/KR/US)" else "Quốc gia (JP/KR/US)") },
                        modifier = Modifier.weight(1f),
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
                    label = { Text(if (isEn) "Azure Endpoint for Ping" else "Azure Endpoint để đo ping") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = XboxNeonGreen
                    )
                )

                // Test connection button
                Button(
                    onClick = {
                        isTesting = true
                        testResult = null
                        scope.launch(Dispatchers.IO) {
                            val p = port.toIntOrNull() ?: 8080
                            val ok = PingTester.isHostReachable(host.trim(), p, 1500)
                            testResult = if (ok) {
                                if (isEn) "✅ Connection successful!" else "✅ Kết nối thành công!"
                            } else {
                                if (isEn) "❌ Could not reach this IP/Port" else "❌ Không thể kết nối tới IP/Port này"
                            }
                            isTesting = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = XboxSurfaceVariant)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = XboxNeonGreen, strokeWidth = 2.dp)
                    } else {
                        Text(if (isEn) "Test Connection" else "Kiểm tra kết nối", color = XboxNeonGreen, fontSize = 12.sp)
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
                    val speed = speedMbps.toIntOrNull() ?: 200
                    val cc = countryCode.trim().uppercase()
                    val resolvedFlag = when (cc) {
                        "JP" -> "🇯🇵"
                        "KR" -> "🇰🇷"
                        "US" -> "🇺🇸"
                        "SG" -> "🇸🇬"
                        "TW" -> "🇹🇼"
                        "HK" -> "🇭🇰"
                        "AU" -> "🇦🇺"
                        "VN" -> "🇻🇳"
                        "DE", "GB", "UK", "FR", "NL", "EU" -> "🇪🇺"
                        else -> "🌐"
                    }
                    val server = ServerLocation(
                        id = "custom_${System.currentTimeMillis()}",
                        name = name.ifBlank { "Custom Proxy" },
                        flag = resolvedFlag,
                        ip = host.trim(),
                        port = p,
                        protocol = protocol.trim().lowercase(),
                        azureEndpoint = azureEndpoint.ifBlank { "japaneast.cloudapp.azure.com" },
                        description = description.ifBlank { if (isEn) "Custom proxy" else "Proxy cá nhân" },
                        speedMbps = speed,
                        countryCode = cc,
                        isCustom = true,
                        isVerified = true
                    )
                    onAddServer(server)
                },
                enabled = host.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = XboxGreen)
            ) {
                Text(if (isEn) "Save & Use" else "Lưu & Sử Dụng")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEn) "Cancel" else "Hủy", color = TextSecondary)
            }
        },
        containerColor = XboxSurface
    )
}

