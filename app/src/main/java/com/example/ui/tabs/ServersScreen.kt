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

private val IDLE_STREAM_TELEMETRY_FLOW =
    kotlinx.coroutines.flow.MutableStateFlow(com.example.cloudplay.StreamTelemetry())

@androidx.compose.runtime.Immutable
private data class ServerRowPrecomputed(
    val basePing: Int,
    val unlockStatus: RegionUnlockStatus,
    val distanceKm: Int?,
    val isTooFarForAutoSelect: Boolean,
    val subtitleText: String
)

@androidx.compose.runtime.Immutable
private data class ComputedServersUiModel(
    val filteredServers: List<ServerLocation> = emptyList(),
    val precomputedServerMeta: Map<String, ServerRowPrecomputed> = emptyMap(),
    val recommendedServerId: String? = null,
    val fasterServerSuggestion: Pair<ServerLocation, Int>? = null
)

@OptIn(FlowPreview::class)
@Composable
fun ServersScreen(
    currentServer: ServerLocation,
    onSelectServer: (ServerLocation) -> Unit,
    isTabActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeVpnStateFlow = remember(isTabActive) {
        if (isTabActive) VpnManager.state else VpnManager.dashboardState
    }
    val vpnState by activeVpnStateFlow.collectAsState(initial = VpnManager.state.value)
    val networkStatus by com.example.cloudplay.NetworkOptimizer.status.collectAsState()
    val activeTelemetryFlow = remember(networkStatus.isLowLatencyStreamActive) {
        if (networkStatus.isLowLatencyStreamActive) {
            com.example.cloudplay.NetworkOptimizer.telemetry
        } else {
            IDLE_STREAM_TELEMETRY_FLOW
        }
    }
    val streamTelemetry by activeTelemetryFlow.collectAsState()
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
        if (!VpnManager.wasStartupMaintenanceRecentlyTriggered()) {
            VpnManager.ensureInitialOrGeoUpdatedOptimalServer(context = context, force = false)
            if (serverPings.isEmpty() && !isTestingAll) {
                VpnManager.testAllServers(
                    autoPickBest = false,
                    context = context,
                    forceRefresh = false,
                    reason = "mở tab Máy chủ (chưa có dữ liệu ping)"
                )
            }
            VpnManager.autoSyncPublicProxiesIfNeeded(context = context, countryFilter = "ALL", force = false)
        } else if (serverPings.isEmpty() && !isTestingAll) {
            VpnManager.testAllServers(
                autoPickBest = false,
                context = context,
                forceRefresh = false,
                reason = "mở tab Máy chủ (bổ sung khi rỗng)"
            )
        }
    }

    // A4: Globalping tắt mặc định; khi bật trong Cài đặt thì hiển thị ước lượng từ trạm gần nhất cho máy chủ đang chọn
    val activeProbeEstimate = remember(currentServer.id, config.enableGlobalpingProbe, vpnState.selectedServerProbeEstimate) {
        if (!config.enableGlobalpingProbe) null
        else vpnState.selectedServerProbeEstimate ?: com.example.vpn.RegionalProbeTester.getCachedEstimateForServer(currentServer)
    }

    fun testAllServers(
        autoPickBest: Boolean = false,
        forceRefresh: Boolean = true,
        reason: String = "thủ công trên tab Máy chủ"
    ) {
        VpnManager.testAllServers(
            autoPickBest = autoPickBest,
            context = context,
            forceRefresh = forceRefresh,
            reason = reason,
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

    val allServersSnapshot = remember(allServers.size, vpnState.customServers, vpnState.liveVerifiedServers) {
        allServers.toList()
    }
    val (jpServerCount, krServerCount, usServerCount, customServerCount) = remember(allServersSnapshot) {
        var jp = 0
        var kr = 0
        var us = 0
        var custom = 0
        for (srv in allServersSnapshot) {
            when (srv.countryCode) {
                "JP" -> jp++
                "KR" -> kr++
                "US" -> us++
            }
            if (srv.isCustom) custom++
        }
        listOf(jp, kr, us, custom)
    }
    val initialServersModel = remember(selectedCategory, allServersSnapshot) {
        val quickList = when (selectedCategory) {
            "ALL" -> allServersSnapshot
            "CUSTOM" -> allServersSnapshot.filter { it.isCustom }
            else -> allServersSnapshot.filter { it.countryCode.equals(selectedCategory, ignoreCase = true) }
        }
        ComputedServersUiModel(filteredServers = quickList)
    }

    val stableOrderCache = remember {
        java.util.concurrent.atomic.AtomicReference<Pair<String, List<String>>?>(null)
    }

    val computedUiModel by androidx.compose.runtime.produceState(
        initialValue = initialServersModel,
        selectedCategory,
        allServersSnapshot,
        serverPings,
        serverMeasurements,
        vpnState.unlockRecords,
        vpnState.detectedGeoIp,
        vpnState.engine,
        recentSessions,
        config.hideUnreachableDynamicServers,
        config.autoBypassEnabled,
        currentServer,
        isTestingAll,
        userNeedsVpn,
        isEn
    ) {
        while (listState.isScrollInProgress && value.filteredServers.isNotEmpty()) {
            kotlinx.coroutines.delay(48)
        }
        val isScrolledDown = listState.isScrollInProgress || listState.firstVisibleItemIndex > 1
        val filterEpochKey = "$selectedCategory|${allServersSnapshot.size}|${vpnState.engine}|${config.hideUnreachableDynamicServers}|${config.autoBypassEnabled}"
        val sortEpochKey = "$filterEpochKey|${VpnManager.getCurrentMeasurementGeneration()}"
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
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

            val fullSorted = VpnManager.sortServersByUnlockAndPing(
                servers = rawFiltered,
                pingMap = serverPings,
                records = vpnState.unlockRecords,
                engine = vpnState.engine,
                sessions = recentSessions,
                measurementsMap = serverMeasurements,
                prioritizeDistanceForAutoBypass = config.autoBypassEnabled,
                geo = vpnState.detectedGeoIp
            )

            // Khi người dùng đang cuộn ở giữa danh sách, giữ nguyên thứ tự các dòng hiện tại
            // (kể cả khi đang đo ping nền isTestingAll) để danh sách không bị đảo/nhảy gây giật khung hình.
            val prevOrderPair = stableOrderCache.get()
            val sortedFiltered = if (isScrolledDown && prevOrderPair != null && prevOrderPair.first.startsWith(filterEpochKey)) {
                val byId = rawFiltered.associateBy { it.id }
                val preserved = ArrayList<ServerLocation>(rawFiltered.size)
                val seen = HashSet<String>(rawFiltered.size * 2)
                for (id in prevOrderPair.second) {
                    val item = byId[id]
                    if (item != null && seen.add(id)) {
                        preserved.add(item)
                    }
                }
                for (item in fullSorted) {
                    if (seen.add(item.id)) {
                        preserved.add(item)
                    }
                }
                preserved
            } else {
                stableOrderCache.set(sortEpochKey to fullSorted.map { it.id })
                fullSorted
            }

            val userCoordsFallback = com.example.vpn.RegionalProbeTester.resolveUserCoordinatesWithTimezoneFallback(vpnState.detectedGeoIp)
            val minCountryDistFallback = if (userCoordsFallback != null) {
                com.example.vpn.RegionalProbeTester.computeMinSupportedCountryDistanceKm(
                    userCoordsFallback.first,
                    userCoordsFallback.second,
                    allServersSnapshot
                )
            } else {
                null
            }

            val metaMap = HashMap<String, ServerRowPrecomputed>(sortedFiltered.size)
            for (server in sortedFiltered) {
                val basePing = VpnManager.getEffectiveServerPing(
                    server = server,
                    engine = vpnState.engine,
                    pingMap = serverPings,
                    sessions = recentSessions
                )
                val unlockStatus = VpnManager.getServerUnlockStatus(server, vpnState.unlockRecords)
                val distanceKm = com.example.vpn.RegionalProbeTester.getServerDistanceKm(
                    server = server,
                    geo = vpnState.detectedGeoIp,
                    allowTimezoneFallback = true
                )
                val isTooFar = !com.example.vpn.RegionalProbeTester.isWithinAutoSelectDistanceLimit(
                    server = server,
                    servers = allServersSnapshot,
                    geo = vpnState.detectedGeoIp,
                    allowTimezoneFallback = true,
                    precomputedUserCoords = userCoordsFallback,
                    precomputedMinCountryDistKm = minCountryDistFallback
                )
                val distLabel = if (distanceKm != null) " • ~${distanceKm}km" else ""
                val regionSessionStats = com.example.data.SessionHistoryRepository.getRegionSessionStats(
                    serverName = server.name,
                    countryCode = server.countryCode,
                    sessionsList = recentSessions
                )
                val directPingNote = if (regionSessionStats != null && regionSessionStats.sampleCount > 0) {
                    if (isEn) {
                        " • Session ~${regionSessionStats.avgRttMs}ms"
                    } else {
                        " • Phiên ~${regionSessionStats.avgRttMs}ms"
                    }
                } else {
                    ""
                }
                val subtitleText = "${server.localizedDescription(isEn)}$distLabel$directPingNote • ${server.ip}"

                metaMap[server.id] = ServerRowPrecomputed(
                    basePing = basePing,
                    unlockStatus = unlockStatus,
                    distanceKm = distanceKm,
                    isTooFarForAutoSelect = isTooFar,
                    subtitleText = subtitleText
                )
            }

            val recId = fullSorted.firstOrNull { server ->
                val measuredPing = serverPings[server.id] ?: -1
                val m = serverMeasurements[server.id]
                val isPrimaryOk = m == null || m.status == ServerPingStatus.MEASURED_PRIMARY
                val meta = metaMap[server.id]
                val st = meta?.unlockStatus ?: VpnManager.getServerUnlockStatus(server, vpnState.unlockRecords)
                val withinDistLimit = selectedCategory != "ALL" || !(meta?.isTooFarForAutoSelect ?: false)
                measuredPing > 0 && isPrimaryOk && st == RegionUnlockStatus.UNLOCKED && withinDistLimit
            }?.id

            val suggestion: Pair<ServerLocation, Int>? = if (!userNeedsVpn || isTestingAll) {
                null
            } else {
                val isCurrentTooFar = !com.example.vpn.RegionalProbeTester.isWithinAutoSelectDistanceLimit(
                    server = currentServer,
                    servers = allServersSnapshot,
                    geo = vpnState.detectedGeoIp,
                    allowTimezoneFallback = true,
                    precomputedUserCoords = userCoordsFallback,
                    precomputedMinCountryDistKm = minCountryDistFallback
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
                            allowTimezoneFallback = true,
                            precomputedUserCoords = userCoordsFallback,
                            precomputedMinCountryDistKm = minCountryDistFallback
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
                                allowTimezoneFallback = true,
                                precomputedUserCoords = userCoordsFallback,
                                precomputedMinCountryDistKm = minCountryDistFallback
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

            ComputedServersUiModel(
                filteredServers = sortedFiltered,
                precomputedServerMeta = metaMap,
                recommendedServerId = recId,
                fasterServerSuggestion = suggestion
            )
        }
    }

    val filteredServers = computedUiModel.filteredServers
    val precomputedServerMeta = computedUiModel.precomputedServerMeta
    val recommendedServerId = computedUiModel.recommendedServerId
    val fasterServerSuggestion = computedUiModel.fasterServerSuggestion

    // A8: Đo lười (lazy viewport probing) chỉ kích hoạt sau khi người dùng dừng thao tác cuộn 300ms,
    // không đọc visibleItemsInfo hay lọc 105 máy chủ trên mỗi pixel cuộn để giữ 60-120fps mượt mà.
    LaunchedEffect(listState, filteredServers, isTabActive) {
        if (!isTabActive) return@LaunchedEffect
        var lastRecordedScrollIndex = -1
        snapshotFlow {
            listState.isScrollInProgress to listState.firstVisibleItemIndex
        }
            .distinctUntilChanged()
            .debounce { (scrolling, _) -> if (scrolling) 0L else 320L }
            .collect { (scrolling, firstIndex) ->
                if (scrolling) {
                    if (firstIndex != lastRecordedScrollIndex) {
                        lastRecordedScrollIndex = firstIndex
                        com.example.data.PerformanceMonitor.recordScrollActivity(com.example.data.PerfScenario.SERVERS)
                    }
                } else if (!vpnState.isTestingPings && filteredServers.isNotEmpty()) {
                    val visibleKeys = listState.layoutInfo.visibleItemsInfo
                        .mapNotNull { it.key as? String }
                        .toSet()
                    if (visibleKeys.isNotEmpty()) {
                        val visibleServers = filteredServers.filter { it.id in visibleKeys }
                        if (visibleServers.isNotEmpty()) {
                            VpnManager.probeVisibleServersLazily(visibleServers)
                        }
                    }
                }
            }
    }

    val bannerShape = remember { RoundedCornerShape(12.dp) }
    val headerCardShape = remember { RoundedCornerShape(14.dp) }
    val modeChipShape = remember { RoundedCornerShape(8.dp) }
    val filterPillShape = remember { RoundedCornerShape(10.dp) }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (fasterServerSuggestion != null) {
            val (fasterServer, fasterPing) = fasterServerSuggestion
            val fasterUnlockStatus = VpnManager.getServerUnlockStatus(fasterServer, vpnState.unlockRecords)
            val untestedNote = if (fasterUnlockStatus == RegionUnlockStatus.UNTESTED) {
                if (isEn) " • Untested" else " • Chưa thử"
            } else {
                ""
            }
            item(key = "faster_server_suggestion_banner") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(bannerShape)
                        .background(Color(0xFF0A1912), bannerShape)
                        .border(1.dp, XboxNeonGreen.copy(alpha = 0.65f), bannerShape)
                        .padding(horizontal = 11.dp, vertical = 8.dp)
                        .testTag("faster_server_suggestion_card"),
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
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Button(
                        onClick = { onSelectServer(fasterServer) },
                        colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .height(48.dp)
                            .testTag("select_faster_server_button")
                    ) {
                        Text(
                            text = if (isEn) "Select" else "Chọn",
                            color = Color.White,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
        if (vpnState.unlockFailPromptServer != null) {
            item(key = "unlock_fail_prompt_banner") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(bannerShape)
                        .background(Color(0xFF1A0D10), bannerShape)
                        .border(1.dp, JapanRed.copy(alpha = 0.65f), bannerShape)
                        .padding(horizontal = 11.dp, vertical = 8.dp)
                        .testTag("unlock_fail_prompt_card"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
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
                            fontSize = 11.sp
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .height(48.dp)
                                .testTag("confirm_switch_best_server_button")
                        ) {
                            Text(
                                text = if (isEn) "Switch server" else "Chuyển máy chủ",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        TextButton(
                            onClick = { VpnManager.dismissUnlockFailPrompt() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .height(48.dp)
                        ) {
                            Text(
                                text = if (isEn) "Dismiss" else "Đóng",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
        item(key = "servers_compact_header_panel") {
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
                    if (streamTelemetry.rttMs > 0) "RTT WebRTC: ${streamTelemetry.rttMs}ms" else "RTT WebRTC: --"
                }
                currentMeasurement != null && currentMeasurement.status != ServerPingStatus.UNTESTED -> {
                    currentMeasurement.formatDisplayBadge(isEn, showPort = false)
                }
                hasTriedCurrentPing && currentPingMs > 0 -> "${currentPingMs}ms"
                isTestingAll -> config.tr("Đang đo...", "Testing...")
                hasTriedCurrentPing && currentPingMs <= 0 -> config.tr("Không đo được", "Unreachable")
                isSmartBypassEngine && currentPingMs > 0 -> config.tr("Phiên trước: ${currentPingMs}ms", "Last session: ${currentPingMs}ms")
                else -> config.tr("Chưa đo", "Not tested")
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(headerCardShape)
                    .background(XboxSurface, headerCardShape)
                    .border(1.dp, XboxCardBorder, headerCardShape)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Selected Server + Auto Best button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(modeChipShape)
                        .background(XboxSurfaceVariant.copy(alpha = 0.6f), modeChipShape)
                        .border(0.8.dp, XboxCardBorder, modeChipShape)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable { detailServer = currentServer }
                            .padding(end = 8.dp)
                            .testTag("header_selected_server_pill"),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${currentServer.flag} ${currentServer.name}",
                            color = TextPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = currentUnlockText,
                                color = currentUnlockColor,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(text = "•", color = TextSecondary, fontSize = 11.sp)
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
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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
                            color = TextSecondary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .testTag("selected_server_reason_label")
                                .semantics { contentDescription = selectionReasonText }
                        )
                        if (config.enableGlobalpingProbe && activeProbeEstimate != null) {
                            Text(
                                text = activeProbeEstimate.formatLabel(isEn),
                                color = WarningAmber,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("globalping_estimate_label")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .height(48.dp)
                            .clip(modeChipShape)
                            .background(XboxGreen.copy(alpha = 0.22f), modeChipShape)
                            .border(0.8.dp, XboxNeonGreen.copy(alpha = 0.6f), modeChipShape)
                            .clickable(enabled = !isTestingAll) {
                                com.example.data.AppLogger.info("UI", "Bấm nút Chọn tối ưu (trên tab Máy chủ)")
                                android.widget.Toast.makeText(
                                    context,
                                    config.tr("⚡ Đang đo ping và chọn máy chủ tối ưu...", "⚡ Measuring ping & selecting best server..."),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                testAllServers(
                                    autoPickBest = true,
                                    forceRefresh = true,
                                    reason = "bấm nút Chọn tối ưu (trên tab Máy chủ)"
                                )
                            }
                            .padding(horizontal = 12.dp)
                            .testTag("auto_pick_best_server_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (isTestingAll) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = XboxNeonGreen,
                                strokeWidth = 1.8.dp
                            )
                            Text(
                                text = config.tr("Đang chọn...", "Selecting..."),
                                color = XboxNeonGreen,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = XboxNeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = config.tr("Chọn tối ưu", "Auto Best"),
                                color = XboxNeonGreen,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Row 2: Compact 2-pill VPN Mode Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("v130_smart_routing_algorithm_card"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isAutoOff = config.autoBypassEnabled
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .height(48.dp)
                            .clip(modeChipShape)
                            .background(
                                if (isAutoOff) XboxGreen.copy(alpha = 0.24f) else XboxSurfaceVariant.copy(alpha = 0.6f),
                                modeChipShape
                            )
                            .border(
                                1.dp,
                                if (isAutoOff) XboxNeonGreen else XboxCardBorder,
                                modeChipShape
                            )
                            .clickable {
                                if (!isAutoOff) {
                                    SettingsRepository.updateAutoBypass(true)
                                    VpnManager.runV130SmartRoutingUpgradeStep(context = context, force = true)
                                }
                                android.widget.Toast.makeText(
                                    context,
                                    config.tr("✓ Chế độ: Tự tắt VPN khi vào game", "✓ Mode: Auto-Off VPN in game"),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                            .padding(horizontal = 8.dp)
                            .testTag("mode_auto_off_distance_first_chip"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = config.tr("Tự tắt VPN khi vào game", "Auto-Off VPN in game"),
                            color = if (isAutoOff) XboxNeonGreen else TextSecondary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 12.sp,
                            fontWeight = if (isAutoOff) FontWeight.ExtraBold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .height(48.dp)
                            .clip(modeChipShape)
                            .background(
                                if (!isAutoOff) XboxGreen.copy(alpha = 0.24f) else XboxSurfaceVariant.copy(alpha = 0.6f),
                                modeChipShape
                            )
                            .border(
                                1.dp,
                                if (!isAutoOff) XboxNeonGreen else XboxCardBorder,
                                modeChipShape
                            )
                            .clickable {
                                if (isAutoOff) {
                                    SettingsRepository.updateAutoBypass(false)
                                    VpnManager.runV130SmartRoutingUpgradeStep(context = context, force = true)
                                }
                                android.widget.Toast.makeText(
                                    context,
                                    config.tr("✓ Chế độ: Bật VPN luôn khi chơi", "✓ Mode: Keep VPN on in game"),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                            .padding(horizontal = 8.dp)
                            .testTag("mode_keep_vpn_ping_first_chip"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = config.tr("Bật VPN luôn khi chơi", "Keep VPN on in game"),
                            color = if (!isAutoOff) XboxNeonGreen else TextSecondary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 12.sp,
                            fontWeight = if (!isAutoOff) FontWeight.ExtraBold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Row 3: Action buttons (Ping Test + Add Custom)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            com.example.data.AppLogger.info("UI", "Bấm nút Đo ping (trên tab Máy chủ)")
                            testAllServers(
                                autoPickBest = false,
                                forceRefresh = true,
                                reason = "bấm nút Đo ping (trên tab Máy chủ)"
                            )
                        },
                        enabled = !isTestingAll,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .height(48.dp)
                            .testTag("ping_test_all_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isTestingAll) {
                            CircularProgressIndicator(modifier = Modifier.size(15.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = config.tr("Đang đo ping...", "Pinging..."),
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = config.tr("Đo ping", "Ping Test"),
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }

                    Button(
                        onClick = { showCustomDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .height(48.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = XboxSurfaceElevated),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = config.tr("Thêm proxy riêng", "Add Custom"),
                            color = XboxNeonGreen,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        item(key = "servers_region_filter_row") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val categories = listOf(
                    "JP" to "🇯🇵 ${config.tr("Nhật Bản", "Japan")} ($jpServerCount)",
                    "KR" to "🇰🇷 ${config.tr("Hàn Quốc", "Korea")} ($krServerCount)",
                    "US" to "🇺🇸 ${config.tr("Hoa Kỳ", "USA")} ($usServerCount)",
                    "CUSTOM" to "⚙️ ${config.tr("Tùy chỉnh", "Custom")} ($customServerCount)",
                    "ALL" to "🌐 $allLabel (${allServersSnapshot.size})"
                )

                items(categories) { (code, title) ->
                    val isSelected = selectedCategory == code
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .height(48.dp)
                            .clip(filterPillShape)
                            .background(
                                if (isSelected) XboxNeonGreen else XboxSurfaceVariant,
                                filterPillShape
                            )
                            .border(
                                1.dp,
                                if (isSelected) XboxNeonGreen else XboxCardBorder,
                                filterPillShape
                            )
                            .clickable { selectedCategory = code }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.Black else TextSecondary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            maxLines = 1
                        )
                    }
                }

                if (allServersSnapshot.any { it.id.startsWith("live_") }) {
                    item(key = "toggle_hide_dead_live_proxies") {
                        val hideActive = config.hideUnreachableDynamicServers
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .height(48.dp)
                                .clip(filterPillShape)
                                .background(
                                    if (hideActive) DirectCyan.copy(alpha = 0.2f) else XboxSurfaceVariant,
                                    filterPillShape
                                )
                                .border(
                                    1.dp,
                                    if (hideActive) DirectCyan else XboxCardBorder,
                                    filterPillShape
                                )
                                .clickable {
                                    val nextHide = !hideActive
                                    SettingsRepository.updateHideUnreachableDynamicServers(nextHide)
                                    android.widget.Toast.makeText(
                                        context,
                                        if (nextHide) {
                                            config.tr("✓ Đã ẩn các proxy động không phản hồi", "✓ Hiding unreachable dynamic proxies")
                                        } else {
                                            config.tr("Đã hiện toàn bộ proxy động", "Showing all dynamic proxies")
                                        },
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .padding(horizontal = 12.dp)
                                .testTag("toggle_hide_dead_live_proxies_chip"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (hideActive) {
                                    config.tr("✓ Đang ẩn proxy chết", "✓ Hiding dead proxies")
                                } else {
                                    config.tr("Ẩn proxy động chết", "Hide dead proxies")
                                },
                                color = if (hideActive) DirectCyan else TextSecondary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                item(key = "manual_sync_public_proxies_chip") {
                    Row(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .height(48.dp)
                            .clip(filterPillShape)
                            .background(XboxSurfaceVariant, filterPillShape)
                            .border(1.dp, XboxCardBorder, filterPillShape)
                            .clickable(enabled = !vpnState.isAutoSyncingLiveProxies) {
                                com.example.data.AppLogger.info("UI", "Bấm nút Cập nhật danh sách proxy công cộng (trên tab Máy chủ)")
                                android.widget.Toast.makeText(
                                    context,
                                    config.tr("Đang cập nhật danh sách proxy công cộng...", "Refreshing public proxy list..."),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                VpnManager.autoSyncPublicProxiesIfNeeded(
                                    context = context,
                                    countryFilter = "ALL",
                                    force = true
                                )
                            }
                            .padding(horizontal = 12.dp)
                            .testTag("manual_sync_public_proxies_chip"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = XboxNeonGreen,
                            modifier = Modifier.size(15.dp)
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
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Cảnh báo ngắn cạnh phần máy chủ công cộng
        item(key = "public_proxy_third_party_warning") {
            Text(
                text = config.tr(
                    "Máy chủ công cộng do bên thứ ba vận hành • Chỉ dùng mở vùng Xbox, không nhập dữ liệu nhạy cảm khác.",
                    "Third-party public servers • Use for Xbox region unlock only, not for sensitive accounts/data."
                ),
                color = WarningAmber,
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141108), modeChipShape)
                    .border(0.8.dp, WarningAmber.copy(alpha = 0.45f), modeChipShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("public_proxy_warning_banner")
            )
        }

        // Servers list: Huy hiệu "★ ĐỀ XUẤT" chỉ dành riêng cho máy chủ nằm trong giới hạn khoảng cách (<= 5.500 km), đã mở khóa thành công (UNLOCKED) và có ping thật trên cổng chính (MEASURED_PRIMARY)
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
            val meta = precomputedServerMeta[server.id]
            val ping = if (isGameActiveOnThisServer) {
                if (streamTelemetry.rttMs > 0) streamTelemetry.rttMs else -1
            } else {
                meta?.basePing ?: VpnManager.getEffectiveServerPing(
                    server = server,
                    engine = vpnState.engine,
                    pingMap = serverPings,
                    sessions = recentSessions
                )
            }
            val unlockStatus = meta?.unlockStatus ?: VpnManager.getServerUnlockStatus(server, vpnState.unlockRecords)
            val isRecommended = server.id == recommendedServerId
            val distanceKm = meta?.distanceKm
            val isTooFarForAutoSelect = meta?.isTooFarForAutoSelect ?: false

            val onClickStable = remember(server, onSelectServer) {
                { onSelectServer(server) }
            }
            val onDeleteStable: (() -> Unit)? = remember(server.id, server.isCustom) {
                if (server.isCustom) {
                    {
                        VpnManager.removeCustomServer(server.id)
                        allServers.removeAll { it.id == server.id }
                        Unit
                    }
                } else null
            }

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
                precomputedSubtitle = meta?.subtitleText,
                isEn = isEn,
                onClick = onClickStable,
                onDelete = onDeleteStable
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


