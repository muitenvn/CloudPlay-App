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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    var showCustomDialog by remember { mutableStateOf(false) }
    var detailServer by remember { mutableStateOf<ServerLocation?>(null) }

    val jpLabel = stringResource(R.string.cat_japan)
    val krLabel = stringResource(R.string.cat_korea)
    val usLabel = stringResource(R.string.cat_usa)
    val allLabel = stringResource(R.string.filter_all)

    val allServers = remember(vpnState.customServers, vpnState.liveVerifiedServers) {
        mutableStateListOf<ServerLocation>().apply {
            val combined = (vpnState.customServers + DEFAULT_SERVERS + vpnState.liveVerifiedServers)
                .distinctBy { it.addressPortKey.lowercase() }
            addAll(combined)
        }
    }

    val serverPings = vpnState.serverPings
    val serverMeasurements = vpnState.serverMeasurements
    val isTestingAll = vpnState.isTestingPings
    val recentSessions by com.example.data.SessionHistoryRepository.sessions.collectAsState()
    val isSmartBypassEngine = vpnState.engine == com.example.vpn.VpnEngine.SMART_BYPASS
    val userNeedsVpn = VpnManager.needsVpnToPlay()

    // Bước nâng cấp v1.3.0 + tự động chọn vùng mặc định bằng probe ngắn khi mở lần đầu / đổi GeoIP / hết hạn cache
    LaunchedEffect(Unit) {
        VpnManager.ensureInitialOrGeoUpdatedOptimalServer(context = context, force = false)
        if (serverPings.isEmpty() && !isTestingAll) {
            VpnManager.testAllServers(autoPickBest = false, context = context, forceRefresh = false)
        }
        VpnManager.autoSyncPublicProxiesIfNeeded(context = context, countryFilter = "ALL", force = false)
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
                val distKm = com.example.vpn.RegionalProbeTester.getServerDistanceKm(
                    server = best,
                    geo = vpnState.detectedGeoIp,
                    allowTimezoneFallback = true
                )
                val distSuffix = if (distKm != null) " • ~${distKm}km" else ""
                if (bestPing > 0) {
                    val msg = if (isEn) {
                        "Selected best server: ${best.flag} ${best.name} (${bestPing}ms$distSuffix)"
                    } else {
                        "Đã chọn máy chủ tốt nhất: ${best.flag} ${best.name} (${bestPing}ms$distSuffix)"
                    }
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                } else if (isSmartBypassEngine) {
                    val msg = if (isEn) {
                        "Selected optimal server: ${best.flag} ${best.name}$distSuffix"
                    } else {
                        "Đã chọn máy chủ tối ưu: ${best.flag} ${best.name}$distSuffix"
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
        config.autoBypassEnabled,
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
            measurementsMap = serverMeasurements,
            prioritizeDistanceForAutoBypass = config.autoBypassEnabled
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

    // Gợi ý máy chủ nhanh hơn (kèm giới hạn khoảng cách <= 5.500 km):
    // - Chỉ hiện sau khi đợt đo ping kết thúc (!isTestingAll) để tránh nhấp nháy.
    // - Ứng viên gợi ý bắt buộc phải nằm trong giới hạn khoảng cách cho phép (<= 5.500 km, không gợi ý máy chủ Mỹ quá xa khi người dùng ở Châu Á).
    // - Nếu máy chủ đang chọn vượt giới hạn khoảng cách (> 5.500 km), gợi ý ngay máy chủ gần có cổng chính sống.
    val fasterServerSuggestion: Pair<ServerLocation, Int>? = remember(
        currentServer,
        allServersSnapshot,
        serverPings,
        serverMeasurements,
        vpnState.unlockRecords,
        vpnState.detectedGeoIp,
        isTestingAll,
        userNeedsVpn
    ) {
        if (!userNeedsVpn || isTestingAll) {
            null
        } else {
            val isCurrentTooFar = !com.example.vpn.RegionalProbeTester.isWithinAutoSelectDistanceLimit(
                server = currentServer,
                servers = allServersSnapshot,
                geo = vpnState.detectedGeoIp,
                allowTimezoneFallback = true
            )
            val currentMeasuredPing = serverPings[currentServer.id] ?: -1
            if (currentMeasuredPing <= 0 && !isCurrentTooFar) {
                null
            } else {
                val currentStatus = VpnManager.getServerUnlockStatus(currentServer, vpnState.unlockRecords)
                val hasAnyUnlockedNearbyServer = allServersSnapshot.any {
                    com.example.vpn.RegionalProbeTester.isWithinAutoSelectDistanceLimit(
                        server = it,
                        servers = allServersSnapshot,
                        geo = vpnState.detectedGeoIp,
                        allowTimezoneFallback = true
                    ) && VpnManager.getServerUnlockStatus(it, vpnState.unlockRecords) == RegionUnlockStatus.UNLOCKED
                }
                val allowUntestedFallback = isCurrentTooFar || (currentStatus != RegionUnlockStatus.UNLOCKED && !hasAnyUnlockedNearbyServer)

                allServersSnapshot
                    .asSequence()
                    .filter { it.id != currentServer.id }
                    .filter { candidate ->
                        com.example.vpn.RegionalProbeTester.isWithinAutoSelectDistanceLimit(
                            server = candidate,
                            servers = allServersSnapshot,
                            geo = vpnState.detectedGeoIp,
                            allowTimezoneFallback = true
                        )
                    }
                    .filter { candidate ->
                        val m = serverMeasurements[candidate.id]
                        m == null || m.status == ServerPingStatus.MEASURED_PRIMARY
                    }
                    .filter { candidate ->
                        val st = VpnManager.getServerUnlockStatus(candidate, vpnState.unlockRecords)
                        st == RegionUnlockStatus.UNLOCKED || (allowUntestedFallback && st == RegionUnlockStatus.UNTESTED)
                    }
                    .map { candidate -> candidate to (serverPings[candidate.id] ?: -1) }
                    .filter { (_, ping) ->
                        ping > 0 && (isCurrentTooFar || (currentMeasuredPing - ping) >= 30)
                    }
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
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0A1912),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, XboxNeonGreen.copy(alpha = 0.75f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
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
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1A0D10),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, JapanRed.copy(alpha = 0.75f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
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
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        1.2.dp,
                        Brush.horizontalGradient(
                            listOf(XboxNeonGreen.copy(alpha = 0.55f), XboxCardBorder, DirectCyan.copy(alpha = 0.55f))
                        ),
                        RoundedCornerShape(20.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = XboxSurface.copy(alpha = 0.96f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    fontSize = 11.5.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.6.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${allServers.size} " + stringResource(R.string.servers_subtitle),
                                    color = TextSecondary,
                                    fontSize = 10.5.sp,
                                    lineHeight = 13.sp,
                                    maxLines = 2,
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
                            val prefix = if (isEn) "Latency: " else "Độ trễ: "
                            prefix + currentMeasurement.formatDisplayBadge(isEn, showPort = false)
                        }
                        hasTriedCurrentPing && currentPingMs > 0 -> if (isEn) {
                            "Latency: ${currentPingMs}ms"
                        } else {
                            "Độ trễ: ${currentPingMs}ms"
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { detailServer = currentServer },
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
                                val selectionReasonText = remember(
                                    currentServer.id,
                                    isEn,
                                    vpnState.detectedGeoIp,
                                    serverMeasurements[currentServer.id],
                                    serverPings[currentServer.id],
                                    recentSessions,
                                    vpnState.userManuallySelectedServer
                                ) {
                                    VpnManager.buildServerSelectionReasonText(
                                        server = currentServer,
                                        isEn = isEn,
                                        geo = vpnState.detectedGeoIp,
                                        measurementsMap = serverMeasurements,
                                        pingMap = serverPings,
                                        sessions = recentSessions
                                    )
                                }
                                Text(
                                    text = selectionReasonText,
                                    color = DirectCyan,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .testTag("selected_server_reason_label")
                                        .semantics { contentDescription = selectionReasonText }
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = currentUnlockText,
                                        color = currentUnlockColor,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        softWrap = false,
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
                                        maxLines = 1,
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

                    // Bước nâng cấp v1.3.0: Thuật toán chọn máy chủ theo chế độ chơi (Tự tắt VPN vs Giữ VPN luôn)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("v130_smart_routing_algorithm_card"),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF07120D),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, DirectCyan.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 11.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isEn) {
                                        "⚡ Smart Server Selection"
                                    } else {
                                        "⚡ Thuật toán chọn máy chủ thông minh"
                                    },
                                    color = DirectCyan,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 6.dp)
                                )
                                Text(
                                    text = if (config.autoBypassEnabled) {
                                        if (isEn) "Session ➔ Unlock" else "RTT phiên ➔ Mở khóa"
                                    } else {
                                        if (isEn) "VPN Ping ➔ Session" else "Ping VPN ➔ Phiên"
                                    },
                                    color = XboxNeonGreen,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val isAutoOff = config.autoBypassEnabled
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (!isAutoOff) {
                                                SettingsRepository.updateAutoBypass(true)
                                                VpnManager.runV130SmartRoutingUpgradeStep(context = context, force = true)
                                            }
                                        }
                                        .testTag("mode_auto_off_distance_first_chip"),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isAutoOff) XboxGreen.copy(alpha = 0.28f) else XboxSurfaceVariant.copy(alpha = 0.65f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.9.dp,
                                        if (isAutoOff) XboxNeonGreen else XboxCardBorder
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = if (isEn) "Auto-Off VPN in game" else "Tự tắt VPN khi vào game",
                                            color = if (isAutoOff) XboxNeonGreen else TextPrimary,
                                            fontFamily = SpaceGroteskFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (isEn) "75% Session • 25% Unlock" else "75% RTT phiên • 25% Mở khóa",
                                            color = if (isAutoOff) Color(0xFFD6EAE0) else TextSecondary,
                                            fontSize = 8.8.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (isAutoOff) {
                                                SettingsRepository.updateAutoBypass(false)
                                                VpnManager.runV130SmartRoutingUpgradeStep(context = context, force = true)
                                            }
                                        }
                                        .testTag("mode_keep_vpn_ping_first_chip"),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (!isAutoOff) XboxGreen.copy(alpha = 0.28f) else XboxSurfaceVariant.copy(alpha = 0.65f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.9.dp,
                                        if (!isAutoOff) XboxNeonGreen else XboxCardBorder
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = if (isEn) "Keep VPN on in game" else "Bật VPN luôn khi chơi",
                                            color = if (!isAutoOff) XboxNeonGreen else TextPrimary,
                                            fontFamily = SpaceGroteskFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (isEn) "75% VPN Ping • 25% Session" else "75% Ping VPN • 25% RTT phiên",
                                            color = if (!isAutoOff) Color(0xFFD6EAE0) else TextSecondary,
                                            fontSize = 8.8.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Action buttons row: Đo ping + Thêm (không cần nút Cập nhật thủ công)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { testAllServers(autoPickBest = false, forceRefresh = true) },
                            enabled = !isTestingAll,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 42.dp)
                                .xboxPressScale(enabled = motionEnabled, pressedScale = 0.95f)
                                .testTag("ping_test_all_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                            shape = RoundedCornerShape(11.dp)
                        ) {
                            if (isTestingAll) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = config.tr("Đang đo ping...", "Pinging..."),
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = config.tr("Đo ping", "Ping Test"),
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            }
                        }

                        Button(
                            onClick = { showCustomDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 42.dp)
                                .xboxPressScale(enabled = motionEnabled, pressedScale = 0.95f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = XboxSurfaceElevated),
                            shape = RoundedCornerShape(11.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = config.tr("Thêm proxy riêng", "Add Custom"),
                                color = XboxNeonGreen,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 11.5.sp,
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

                item(key = "manual_sync_public_proxies_chip") {
                    Surface(
                        modifier = Modifier
                            .xboxPressScale(enabled = motionEnabled, pressedScale = 0.95f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !vpnState.isAutoSyncingLiveProxies) {
                                VpnManager.autoSyncPublicProxiesIfNeeded(
                                    context = context,
                                    countryFilter = "ALL",
                                    force = true
                                )
                            }
                            .testTag("manual_sync_public_proxies_chip"),
                        color = XboxSurfaceVariant.copy(alpha = 0.88f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, XboxCardBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = XboxNeonGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (vpnState.isAutoSyncingLiveProxies) {
                                    config.tr("Đang cập nhật...", "Updating...")
                                } else {
                                    config.tr("Cập nhật danh sách", "Refresh Public List")
                                },
                                color = XboxNeonGreen,
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Cảnh báo ngắn cạnh phần máy chủ live công cộng (Mục 4)
        item(key = "public_proxy_third_party_warning") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("public_proxy_warning_banner"),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF141108),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, WarningAmber.copy(alpha = 0.5f))
            ) {
                Text(
                    text = config.tr(
                        "Máy chủ từ danh sách công cộng, bên thứ ba vận hành. Không dùng cho tài khoản/dữ liệu nhạy cảm ngoài Xbox.",
                        "Servers from public lists, operated by third parties. Do not use for sensitive accounts/data outside Xbox."
                    ),
                    color = WarningAmber,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // Servers list: Huy hiệu "★ ĐỀ XUẤT" chỉ dành riêng cho máy chủ nằm trong giới hạn khoảng cách (<= 5.500 km), đã mở khóa thành công (UNLOCKED) và có ping thật trên cổng chính (MEASURED_PRIMARY)
        val recommendedServerId = filteredServers.firstOrNull { server ->
            val measuredPing = serverPings[server.id] ?: -1
            val m = serverMeasurements[server.id]
            val isPrimaryOk = m == null || m.status == ServerPingStatus.MEASURED_PRIMARY
            val st = VpnManager.getServerUnlockStatus(server, vpnState.unlockRecords)
            val withinDistLimit = selectedCategory != "ALL" || com.example.vpn.RegionalProbeTester.isWithinAutoSelectDistanceLimit(
                server = server,
                servers = allServersSnapshot,
                geo = vpnState.detectedGeoIp,
                allowTimezoneFallback = true
            )
            measuredPing > 0 && isPrimaryOk && st == RegionUnlockStatus.UNLOCKED && withinDistLimit
        }?.id

        items(
            items = filteredServers,
            key = { it.id },
            contentType = { "server_row_card" }
        ) { server ->
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
            val distanceKm = com.example.vpn.RegionalProbeTester.getServerDistanceKm(
                server = server,
                geo = vpnState.detectedGeoIp,
                allowTimezoneFallback = true
            )
            val isTooFarForAutoSelect = !com.example.vpn.RegionalProbeTester.isWithinAutoSelectDistanceLimit(
                server = server,
                servers = allServersSnapshot,
                geo = vpnState.detectedGeoIp,
                allowTimezoneFallback = true
            )

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
                distanceKm = distanceKm,
                isTooFarForAutoSelect = isTooFarForAutoSelect,
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

    detailServer?.let { srv ->
        val srvMeasurement = serverMeasurements[srv.id]
        val srvPing = serverPings[srv.id] ?: -1
        val srvUnlock = VpnManager.getServerUnlockStatus(srv, vpnState.unlockRecords)
        ServerDetailDialog(
            server = srv,
            measurement = srvMeasurement,
            pingMs = srvPing,
            unlockStatus = srvUnlock,
            isEn = isEn,
            onDismiss = { detailServer = null }
        )
    }
}


