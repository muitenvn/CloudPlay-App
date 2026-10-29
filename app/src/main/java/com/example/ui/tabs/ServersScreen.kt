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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.example.vpn.EndpointPingMeasurement
import com.example.vpn.LiveProxyFetcher
import com.example.vpn.PingTester
import com.example.vpn.RegionUnlockStatus
import com.example.vpn.ServerLocation
import com.example.vpn.ServerPingStatus
import com.example.vpn.VpnManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@Composable
fun ServersScreen(
    currentServer: ServerLocation,
    onSelectServer: (ServerLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vpnState by VpnManager.state.collectAsState()
    val networkStatus by com.example.cloudplay.NetworkOptimizer.status.collectAsState()
    val streamTelemetry by com.example.cloudplay.NetworkOptimizer.telemetry.collectAsState()
    val config by SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    val motionEnabled = rememberXboxMotionEnabled(
        lowEndDeviceMode = config.lowEndDeviceMode,
        gpuLoadReductionEnabled = config.gpuLoadReductionEnabled
    )
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var selectedCategory by remember { mutableStateOf("ALL") }
    var isFetchingLive by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }

    val jpLabel = stringResource(R.string.cat_japan)
    val krLabel = stringResource(R.string.cat_korea)
    val usLabel = stringResource(R.string.cat_usa)
    val allLabel = stringResource(R.string.filter_all)

    val allServers = remember(vpnState.customServers) {
        mutableStateListOf<ServerLocation>().apply {
            addAll(vpnState.customServers)
            addAll(DEFAULT_SERVERS)
        }
    }

    val serverPings = vpnState.serverPings
    val serverMeasurements = vpnState.serverMeasurements
    val isTestingAll = vpnState.isTestingPings
    val recentSessions by com.example.data.SessionHistoryRepository.sessions.collectAsState()
    val isSmartBypassEngine = vpnState.engine == com.example.vpn.VpnEngine.SMART_BYPASS
    val userNeedsVpn = VpnManager.needsVpnToPlay()

    // A8: Chỉ đo bước 1 + 2 khi chưa có dữ liệu đo trong phiên; chuyển tab không đo lại.
    LaunchedEffect(Unit) {
        if (serverPings.isEmpty() && !isTestingAll) {
            VpnManager.testAllServers(autoPickBest = false, context = context, forceRefresh = false)
        }
    }

    // A4: Globalping tắt mặc định; khi bật trong Cài đặt thì hiển thị ước lượng từ trạm gần nhất cho máy chủ đang chọn
    val activeProbeEstimate = remember(currentServer.id, config.enableGlobalpingProbe, vpnState.selectedServerProbeEstimate) {
        if (!config.enableGlobalpingProbe) null
        else vpnState.selectedServerProbeEstimate ?: com.example.vpn.RegionalProbeTester.getCachedEstimateForServer(currentServer)
    }

    fun testAllServers(autoPickBest: Boolean = false, forceRefresh: Boolean = true) {
        VpnManager.testAllServers(
            autoPickBest = autoPickBest,
            context = context,
            forceRefresh = forceRefresh,
            onPickedBest = { best, bestPing ->
                if (bestPing > 0) {
                    onSelectServer(best)
                    val msg = if (isEn) {
                        "Selected best server: ${best.flag} ${best.name} (${bestPing}ms)"
                    } else {
                        "Đã chọn máy chủ tốt nhất: ${best.flag} ${best.name} (${bestPing}ms)"
                    }
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                } else if (isSmartBypassEngine) {
                    onSelectServer(best)
                    val msg = if (isEn) {
                        "Selected optimal server: ${best.flag} ${best.name}"
                    } else {
                        "Đã chọn máy chủ tối ưu: ${best.flag} ${best.name}"
                    }
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    val msg = if (isEn) {
                        "Could not measure ping to any proxy server"
                    } else {
                        "Không đo được ping tới máy chủ proxy nào"
                    }
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
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
            VpnManager.testAllServers(autoPickBest = false, context = context, forceRefresh = false)
        }
    }

    val allServersSnapshot = allServers.toList()
    val filteredServers = remember(
        selectedCategory,
        allServersSnapshot,
        serverPings,
        serverMeasurements,
        vpnState.unlockRecords,
        vpnState.engine,
        recentSessions,
        config.hideUnreachableDynamicServers,
        currentServer.id
    ) {
        val rawFiltered = when (selectedCategory) {
            "ALL" -> allServersSnapshot
            "CUSTOM" -> allServersSnapshot.filter { it.isCustom }
            else -> allServersSnapshot.filter { it.countryCode.equals(selectedCategory, ignoreCase = true) }
        }.filter { srv ->
            if (!config.hideUnreachableDynamicServers || srv.id == currentServer.id || !srv.id.startsWith("live_")) {
                true
            } else {
                val m = serverMeasurements[srv.id]
                val p = serverPings[srv.id]
                m?.status != ServerPingStatus.UNREACHABLE && p != -1
            }
        }
        VpnManager.sortServersByUnlockAndPing(
            servers = rawFiltered,
            pingMap = serverPings,
            records = vpnState.unlockRecords,
            engine = vpnState.engine,
            sessions = recentSessions,
            measurementsMap = serverMeasurements
        )
    }

    // A8: Đo lười (lazy viewport probing) khi người dùng cuộn danh sách với debounce 250ms
    LaunchedEffect(listState, filteredServers) {
        snapshotFlow {
            val visibleKeys = listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }.toSet()
            filteredServers.filter { it.id in visibleKeys }
        }
            .distinctUntilChanged { old, new -> old.map { it.id } == new.map { it.id } }
            .debounce(250L)
            .collect { visibleServers ->
                if (visibleServers.isNotEmpty() && !vpnState.isTestingPings) {
                    VpnManager.probeVisibleServersLazily(visibleServers)
                }
            }
    }

    // Gợi ý máy chủ nhanh hơn:
    // - Chỉ hiện sau khi đợt đo ping kết thúc (!isTestingAll) để tránh nhấp nháy.
    // - Cả máy chủ đang chọn và máy chủ gợi ý đều phải có ping thật đo được trên cổng chính (MEASURED_PRIMARY) và chênh >= 30ms.
    // - Không coi máy chủ chỉ phản hồi cổng phụ (:443/:80) là ứng viên gợi ý thay thế cổng chính.
    val fasterServerSuggestion: Pair<ServerLocation, Int>? = remember(
        currentServer,
        allServersSnapshot,
        serverPings,
        serverMeasurements,
        vpnState.unlockRecords,
        isTestingAll,
        userNeedsVpn
    ) {
        if (!userNeedsVpn || isTestingAll) {
            null
        } else {
            val currentMeasuredPing = serverPings[currentServer.id] ?: -1
            if (currentMeasuredPing <= 0) {
                null
            } else {
                val currentStatus = VpnManager.getServerUnlockStatus(currentServer, vpnState.unlockRecords)
                val hasAnyUnlockedServer = allServersSnapshot.any {
                    VpnManager.getServerUnlockStatus(it, vpnState.unlockRecords) == RegionUnlockStatus.UNLOCKED
                }
                val allowUntestedFallback = currentStatus != RegionUnlockStatus.UNLOCKED && !hasAnyUnlockedServer

                allServersSnapshot
                    .asSequence()
                    .filter { it.id != currentServer.id }
                    .filter { candidate ->
                        val m = serverMeasurements[candidate.id]
                        m == null || m.status == ServerPingStatus.MEASURED_PRIMARY
                    }
                    .filter { candidate ->
                        val st = VpnManager.getServerUnlockStatus(candidate, vpnState.unlockRecords)
                        st == RegionUnlockStatus.UNLOCKED || (allowUntestedFallback && st == RegionUnlockStatus.UNTESTED)
                    }
                    .map { candidate -> candidate to (serverPings[candidate.id] ?: -1) }
                    .filter { (_, ping) -> ping > 0 && (currentMeasuredPing - ping) >= 30 }
                    .minWithOrNull(
                        Comparator { a, b ->
                            val stA = VpnManager.getServerUnlockStatus(a.first, vpnState.unlockRecords)
                            val stB = VpnManager.getServerUnlockStatus(b.first, vpnState.unlockRecords)
                            val rankA = if (stA == RegionUnlockStatus.UNLOCKED) 0 else 1
                            val rankB = if (stB == RegionUnlockStatus.UNLOCKED) 0 else 1
                            if (rankA != rankB) rankA.compareTo(rankB) else a.second.compareTo(b.second)
                        }
                    )
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (fasterServerSuggestion != null) {
            val (fasterServer, fasterPing) = fasterServerSuggestion
            val fasterUnlockStatus = VpnManager.getServerUnlockStatus(fasterServer, vpnState.unlockRecords)
            val untestedNote = if (fasterUnlockStatus == RegionUnlockStatus.UNTESTED) {
                if (isEn) " • Untested unlock" else " • Chưa thử mở khóa"
            } else {
                ""
            }
            item(key = "faster_server_suggestion_banner") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("faster_server_suggestion_card"),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0A1912),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, XboxNeonGreen.copy(alpha = 0.75f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEn) {
                                "Faster server: ${fasterServer.flag} ${fasterServer.name} ($fasterPing ms$untestedNote)"
                            } else {
                                "Máy chủ nhanh hơn: ${fasterServer.flag} ${fasterServer.name} ($fasterPing ms$untestedNote)"
                            },
                            color = XboxNeonGreen,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp)
                        )
                        Button(
                            onClick = { onSelectServer(fasterServer) },
                            colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 36.dp)
                                .testTag("select_faster_server_button")
                        ) {
                            Text(
                                text = if (isEn) "Select" else "Chọn",
                                color = Color.White,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
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
                                text = if (isEn) "This server cannot unlock the region, switch to another server?" else "Máy chủ này không mở khóa được vùng, đổi sang máy chủ khác?",
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
                                    text = if (isEn) "Switch server" else "Chuyển máy chủ",
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

                    // Compact Selected Server & Unlock Telemetry Strip
                    val isGameActiveOnSelected = networkStatus.isLowLatencyStreamActive &&
                        vpnState.activeStreamServerId == currentServer.id
                    val currentMeasurement = serverMeasurements[currentServer.id]
                    val hasTriedCurrentPing = serverPings.containsKey(currentServer.id)
                    val currentPingMs = if (isGameActiveOnSelected) {
                        if (streamTelemetry.rttMs > 0) streamTelemetry.rttMs else -1
                    } else {
                        VpnManager.getEffectiveServerPing(
                            server = currentServer,
                            engine = vpnState.engine,
                            pingMap = serverPings,
                            sessions = recentSessions
                        )
                    }
                    val currentUnlockStatus = VpnManager.getServerUnlockStatus(currentServer, vpnState.unlockRecords)
                    val currentUnlockText = when (currentUnlockStatus) {
                        RegionUnlockStatus.UNLOCKED -> config.tr("Đã mở khóa", "Unlocked")
                        RegionUnlockStatus.FAILED -> config.tr("Không mở khóa", "Unlock Failed")
                        RegionUnlockStatus.UNTESTED -> config.tr("Sẵn sàng", "Ready")
                        RegionUnlockStatus.WRONG_REGION -> config.tr("Sai vùng", "Wrong Region")
                    }
                    val currentUnlockColor = when (currentUnlockStatus) {
                        RegionUnlockStatus.UNLOCKED -> XboxNeonGreen
                        RegionUnlockStatus.FAILED -> JapanRed
                        RegionUnlockStatus.UNTESTED -> DirectCyan
                        RegionUnlockStatus.WRONG_REGION -> WarningAmber
                    }
                    val currentPingText = when {
                        isGameActiveOnSelected -> {
                            if (streamTelemetry.rttMs > 0) {
                                "RTT WebRTC: ${streamTelemetry.rttMs}ms"
                            } else {
                                "RTT WebRTC: --"
                            }
                        }
                        currentMeasurement != null && currentMeasurement.status != ServerPingStatus.UNTESTED -> {
                            val prefix = if (isEn) "Network latency: " else "Độ trễ mạng: "
                            prefix + currentMeasurement.formatDisplayBadge(isEn)
                        }
                        hasTriedCurrentPing && currentPingMs > 0 -> if (isEn) {
                            "Network latency: ${currentPingMs}ms (:${currentServer.port})"
                        } else {
                            "Độ trễ mạng: ${currentPingMs}ms (:${currentServer.port})"
                        }
                        isTestingAll -> if (isEn) "Testing network latency..." else "Đang đo độ trễ mạng..."
                        hasTriedCurrentPing && currentPingMs <= 0 -> if (isEn) {
                            "Network latency: Unreachable"
                        } else {
                            "Độ trễ mạng: Không đo được"
                        }
                        isSmartBypassEngine && currentPingMs > 0 -> if (isEn) {
                            "Last session latency: ${currentPingMs}ms"
                        } else {
                            "Độ trễ phiên gần nhất: ${currentPingMs}ms"
                        }
                        else -> if (isEn) {
                            "Network latency: Not tested"
                        } else {
                            "Độ trễ mạng: Chưa đo"
                        }
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
                                    text = "${config.tr("Đang chọn", "Selected")}: ${currentServer.flag} ${currentServer.name}",
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
                                        color = if (currentMeasurement?.status == ServerPingStatus.MEASURED_FALLBACK_ONLY) {
                                            WarningAmber
                                        } else if (currentPingMs in 1..99) {
                                            XboxNeonGreen
                                        } else {
                                            DirectCyan
                                        },
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (config.enableGlobalpingProbe && activeProbeEstimate != null) {
                                    Text(
                                        text = activeProbeEstimate.formatLabel(isEn),
                                        color = WarningAmber,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("globalping_estimate_label")
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
                                    .clickable(enabled = !isTestingAll) { testAllServers(autoPickBest = true, forceRefresh = true) }
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
                                        text = config.tr("Chọn tối ưu", "Auto Best"),
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
                            onClick = { testAllServers(autoPickBest = false, forceRefresh = true) },
                            enabled = !isTestingAll,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 42.dp)
                                .xboxPressScale(enabled = motionEnabled, pressedScale = 0.95f)
                                .testTag("ping_test_all_button"),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                            shape = RoundedCornerShape(11.dp)
                        ) {
                            if (isTestingAll) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = config.tr("Đang đo ping", "Pinging..."),
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = config.tr("Đo ping", "Ping Test"),
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
                                    text = config.tr("Đang tải...", "Updating..."),
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
                                    text = config.tr("Cập nhật", "Update"),
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
                                text = config.tr("Thêm", "Add"),
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
            // Region Selector Pills + Optional Hide Unreachable Dynamic Proxies Chip
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val categories = listOf(
                    "JP" to "🇯🇵 $jpLabel (${allServers.count { it.countryCode == "JP" }})",
                    "KR" to "🇰🇷 $krLabel (${allServers.count { it.countryCode == "KR" }})",
                    "US" to "🇺🇸 $usLabel (${allServers.count { it.countryCode == "US" }})",
                    "CUSTOM" to "⚙️ ${config.tr("Tùy Chỉnh", "Custom")} (${allServers.count { it.isCustom }})",
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

                if (allServersSnapshot.any { it.id.startsWith("live_") }) {
                    item(key = "toggle_hide_dead_live_proxies") {
                        val hideActive = config.hideUnreachableDynamicServers
                        Surface(
                            modifier = Modifier
                                .xboxPressScale(enabled = motionEnabled, pressedScale = 0.95f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    SettingsRepository.updateHideUnreachableDynamicServers(!hideActive)
                                }
                                .testTag("toggle_hide_dead_live_proxies_chip"),
                            color = if (hideActive) DirectCyan.copy(alpha = 0.24f) else XboxSurfaceVariant.copy(alpha = 0.88f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (hideActive) DirectCyan else XboxCardBorder
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = if (isEn) {
                                    if (hideActive) "Hiding dead dynamic" else "Hide dead dynamic"
                                } else {
                                    if (hideActive) "Đang ẩn proxy chết" else "Ẩn proxy động chết"
                                },
                                color = if (hideActive) DirectCyan else TextSecondary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        // Servers list: Huy hiệu "★ ĐỀ XUẤT" chỉ dành riêng cho máy chủ đã mở khóa thành công (UNLOCKED) và có ping thật trên cổng chính (MEASURED_PRIMARY)
        val recommendedServerId = filteredServers.firstOrNull { server ->
            val measuredPing = serverPings[server.id] ?: -1
            val m = serverMeasurements[server.id]
            val isPrimaryOk = m == null || m.status == ServerPingStatus.MEASURED_PRIMARY
            val st = VpnManager.getServerUnlockStatus(server, vpnState.unlockRecords)
            measuredPing > 0 && isPrimaryOk && st == RegionUnlockStatus.UNLOCKED
        }?.id

        items(filteredServers, key = { it.id }) { server ->
            val isSelected = server.id == currentServer.id
            val isGameActiveOnThisServer = networkStatus.isLowLatencyStreamActive &&
                vpnState.activeStreamServerId == server.id
            val measurement = serverMeasurements[server.id]
            val hasTriedPing = serverPings.containsKey(server.id)
            val ping = if (isGameActiveOnThisServer) {
                if (streamTelemetry.rttMs > 0) streamTelemetry.rttMs else -1
            } else {
                VpnManager.getEffectiveServerPing(
                    server = server,
                    engine = vpnState.engine,
                    pingMap = serverPings,
                    sessions = recentSessions
                )
            }
            val unlockStatus = VpnManager.getServerUnlockStatus(server, vpnState.unlockRecords)
            val isRecommended = server.id == recommendedServerId

            ServerCard(
                server = server,
                isSelected = isSelected,
                isRecommended = isRecommended,
                pingMs = ping,
                measurement = measurement,
                hasTriedPing = hasTriedPing,
                isSmartBypass = isSmartBypassEngine,
                isGameActiveNow = isGameActiveOnThisServer,
                isTestingPings = isTestingAll,
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
    measurement: EndpointPingMeasurement? = null,
    hasTriedPing: Boolean = false,
    isSmartBypass: Boolean = false,
    isGameActiveNow: Boolean = false,
    isTestingPings: Boolean = false,
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
    val isFallbackPortOnly = measurement?.status == ServerPingStatus.MEASURED_FALLBACK_ONLY
    val isServerCurrentlyTesting = measurement?.status == ServerPingStatus.TESTING
    val isUnstablePing = measurement?.isUnstable == true || pingMs >= 1000
    val targetPingColor = when {
        pingMs <= 0 -> Color.Gray
        isFallbackPortOnly || isUnstablePing -> WarningAmber
        pingMs <= 80 -> XboxNeonGreen
        pingMs <= 120 -> WarningAmber
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
        pingMs <= 0 -> 0
        isFallbackPortOnly -> 2
        pingMs <= 65 -> 4
        pingMs <= 120 -> 3
        pingMs <= 200 -> 2
        else -> 1
    }

    // 3. HIỂN THỊ: mỗi thẻ máy chủ có nhãn nhỏ: "Đã mở khóa" (xanh), "Không mở khóa" (đỏ), "Chưa thử mở khóa" (xám), "Sai vùng" (vàng)
    val unlockLabel = when (unlockStatus) {
        RegionUnlockStatus.UNLOCKED -> config.tr("Đã mở khóa", "Unlocked")
        RegionUnlockStatus.FAILED -> config.tr("Không mở khóa", "Unlock Failed")
        RegionUnlockStatus.UNTESTED -> config.tr("Chưa thử mở khóa", "Untested unlock")
        RegionUnlockStatus.WRONG_REGION -> config.tr("Sai vùng", "Wrong Region")
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
                                    text = config.tr("★ ĐỀ XUẤT", "★ BEST"),
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

                        val mainPingValueText = when {
                            isGameActiveNow && pingMs > 0 -> "${animatedPingMs}ms"
                            isGameActiveNow -> "--"
                            isServerCurrentlyTesting -> "..."
                            isFallbackPortOnly && pingMs > 0 -> "${animatedPingMs}ms (:${measurement?.measuredPort ?: 443})"
                            pingMs > 0 && isUnstablePing -> "${animatedPingMs}ms • ${config.tr("Mạng chập chờn", "Unstable")}"
                            pingMs > 0 -> "${animatedPingMs}ms"
                            hasTriedPing || measurement?.status == ServerPingStatus.UNREACHABLE -> config.tr("Không đo được", "Unreachable")
                            else -> config.tr("Chưa đo", "Not tested")
                        }

                        Text(
                            text = mainPingValueText,
                            color = if (pingMs > 0) pingColor else if (isServerCurrentlyTesting) DirectCyan else pingColor,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (pingMs <= 0 || isFallbackPortOnly || isUnstablePing) 10.sp else 12.sp
                        )
                    }

                    val metricLabelText = when {
                        isGameActiveNow -> "RTT WebRTC"
                        isServerCurrentlyTesting -> if (isEn) "Testing... • :${server.port}" else "Đang đo... • :${server.port}"
                        isFallbackPortOnly -> {
                            val fbPort = measurement?.measuredPort ?: 443
                            if (isEn) "Fallback :$fbPort • :${server.port} closed" else "Cổng phụ :$fbPort • Cổng :${server.port} đóng"
                        }
                        measurement?.status == ServerPingStatus.MEASURED_PRIMARY && pingMs > 0 -> {
                            val qualityTag = when {
                                isUnstablePing -> if (isEn) "Unstable" else "Mạng chập chờn"
                                pingMs <= 80 -> if (isEn) "Good" else "Tốt"
                                pingMs <= 120 -> if (isEn) "Fair" else "Trung bình"
                                else -> if (isEn) "High" else "Cao"
                            }
                            if (isEn) "Latency ($qualityTag) • :${measurement.measuredPort}" else "Độ trễ mạng ($qualityTag) • :${measurement.measuredPort}"
                        }
                        hasTriedPing && pingMs > 0 -> if (isEn) "Network latency • :${server.port}" else "Độ trễ mạng • :${server.port}"
                        hasTriedPing || measurement?.status == ServerPingStatus.UNREACHABLE -> if (isEn) "Unreachable • :${server.port}" else "Không đo được • :${server.port}"
                        isSmartBypass && pingMs > 0 -> if (isEn) "Last session latency" else "Độ trễ phiên gần nhất"
                        else -> if (isEn) "Not tested • :${server.port}" else "Chưa đo • :${server.port}"
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
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = metricLabelText,
                                color = if (isFallbackPortOnly) WarningAmber else XboxNeonGreen,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = metricLabelText,
                            color = if (isFallbackPortOnly) WarningAmber else TextSecondary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.5.sp
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

    val trimmedHost = host.trim()
    val isPrivateIp = remember(trimmedHost) {
        trimmedHost.isNotEmpty() && com.example.vpn.GeoIpDetector.isPrivateOrLoopbackIp(trimmedHost)
    }

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
                    modifier = Modifier.fillMaxWidth(),
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
                    modifier = Modifier.fillMaxWidth(),
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
                        fontSize = 11.sp
                    )
                }

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
                        onValueChange = { newCc ->
                            userEditedCountry = newCc.isNotBlank()
                            val cleanCc = newCc.trim().uppercase()
                            countryCode = cleanCc
                            azureEndpoint = com.example.vpn.GeoIpDetector.getAzureEndpointForCountry(cleanCc)
                        },
                        label = { Text(if (isEn) "Country ISO (Auto/Manual)" else "Mã quốc gia (Tự nhận/Sửa tay)") },
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
                    label = { Text(if (isEn) "Azure Endpoint (JP/KR only)" else "Azure Endpoint (Chỉ có cho JP/KR)") },
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
                            val ok = !com.example.vpn.GeoIpDetector.isPrivateOrLoopbackIp(trimmedHost) &&
                                PingTester.isHostReachable(trimmedHost, p, 1500)
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
                        isVerified = true
                    )
                    onAddServer(server)
                },
                enabled = trimmedHost.isNotBlank() && !isPrivateIp,
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

