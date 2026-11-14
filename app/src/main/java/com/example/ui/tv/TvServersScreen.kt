package com.example.ui.tv

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppConfig
import com.example.ui.components.gamepadClickable
import com.example.ui.tabs.CustomProxyDialog
import com.example.ui.tabs.ServerDetailDialog
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.vpn.DEFAULT_SERVERS
import com.example.vpn.NetworkMode
import com.example.vpn.RegionUnlockStatus
import com.example.vpn.RegionalProbeTester
import com.example.vpn.ServerLocation
import com.example.vpn.VpnManager

@Composable
internal fun TvServersScreen(
    config: AppConfig,
    currentServer: ServerLocation,
    onSelectServer: (ServerLocation) -> Unit,
    isTabActive: Boolean,
    scale: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeVpnStateFlow = remember(isTabActive) {
        if (isTabActive) VpnManager.state else VpnManager.dashboardState
    }
    val vpnState by activeVpnStateFlow.collectAsState(initial = VpnManager.state.value)
    val isEn = config.isEnglishUI()

    var selectedCategory by rememberSaveable { mutableStateOf("ALL") }
    var showCustomDialog by remember { mutableStateOf(false) }
    var detailServer by remember { mutableStateOf<ServerLocation?>(null) }

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
    val isVpnActive = vpnState.mode != NetworkMode.DISCONNECTED

    LaunchedEffect(Unit) {
        if (!VpnManager.wasStartupMaintenanceRecentlyTriggered()) {
            VpnManager.ensureInitialOrGeoUpdatedOptimalServer(context = context, force = false)
            if (serverPings.isEmpty() && !isTestingAll) {
                VpnManager.testAllServers(
                    autoPickBest = false,
                    context = context,
                    forceRefresh = false,
                    reason = "mở tab Máy chủ TV (chưa có dữ liệu ping)"
                )
            }
            VpnManager.autoSyncPublicProxiesIfNeeded(context = context, countryFilter = "ALL", force = false)
        } else if (serverPings.isEmpty() && !isTestingAll) {
            VpnManager.testAllServers(
                autoPickBest = false,
                context = context,
                forceRefresh = false,
                reason = "mở tab Máy chủ TV (bổ sung khi rỗng)"
            )
        }
    }

    fun triggerPingTest(autoPickBest: Boolean) {
        VpnManager.testAllServers(
            autoPickBest = autoPickBest,
            context = context,
            forceRefresh = true,
            reason = if (autoPickBest) "Tự chọn máy chủ tốt nhất trên TV" else "Đo lại ping trên TV",
            onPickedBest = { best, bestPing ->
                val distKm = RegionalProbeTester.getServerDistanceKm(
                    server = best,
                    geo = vpnState.detectedGeoIp,
                    allowTimezoneFallback = true
                )
                val distSuffix = if (distKm != null) " • ~${distKm}km" else ""
                val msg = if (bestPing > 0) {
                    config.tr(
                        "Đã chọn máy chủ tốt nhất: ${best.flag} ${best.name} (${bestPing}ms$distSuffix)",
                        "Selected best server: ${best.flag} ${best.name} (${bestPing}ms$distSuffix)"
                    )
                } else {
                    config.tr(
                        "Đã chọn máy chủ tối ưu: ${best.flag} ${best.name}$distSuffix",
                        "Selected optimal server: ${best.flag} ${best.name}$distSuffix"
                    )
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    val filteredServers = remember(allServers.size, selectedCategory, serverPings, currentServer.id, vpnState.detectedGeoIp) {
        val base = allServers.filter { srv ->
            when (selectedCategory) {
                "ALL" -> true
                "CUSTOM" -> srv.isCustom
                else -> srv.countryCode.equals(selectedCategory, ignoreCase = true)
            }
        }
        base.sortedWith(
            compareBy<ServerLocation> { it.id != currentServer.id }
                .thenBy {
                    val p = serverPings[it.id] ?: -1
                    if (p in 1..9999) p else Int.MAX_VALUE
                }
                .thenBy {
                    RegionalProbeTester.getServerDistanceKm(
                        server = it,
                        geo = vpnState.detectedGeoIp,
                        allowTimezoneFallback = true
                    ) ?: Int.MAX_VALUE
                }
        )
    }

    val recommendedServerId = remember(allServers.size, serverPings, vpnState.unlockRecords, vpnState.detectedGeoIp) {
        allServers
            .filter { srv ->
                val unlock = VpnManager.getServerUnlockStatus(srv, vpnState.unlockRecords)
                unlock != RegionUnlockStatus.FAILED && unlock != RegionUnlockStatus.WRONG_REGION
            }
            .minWithOrNull(
                compareBy<ServerLocation> {
                    val p = serverPings[it.id] ?: -1
                    if (p in 1..9999) p else Int.MAX_VALUE
                }.thenBy {
                    RegionalProbeTester.getServerDistanceKm(
                        server = it,
                        geo = vpnState.detectedGeoIp,
                        allowTimezoneFallback = true
                    ) ?: Int.MAX_VALUE
                }
            )?.id
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("tv_servers_screen"),
        verticalArrangement = Arrangement.spacedBy((12.dp * scale).coerceAtLeast(8.dp))
    ) {
        // 1. Header (#s-srv .top in tv-mockup.html)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((12.dp * scale).coerceAtLeast(8.dp))
            ) {
                Text(
                    text = config.tr("Máy chủ", "Servers"),
                    color = TvInk,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = (22f * scale).coerceAtLeast(16.5f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                TvPill(
                    text = "${config.tr("Đang dùng", "Active")}: ${currentServer.flag} ${currentServer.name}",
                    scale = scale,
                    tone = TvPillTone.GREEN
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((8.dp * scale).coerceAtLeast(6.dp))
            ) {
                TvChip(
                    text = if (isTestingAll) {
                        config.tr("Đang đo ping...", "Testing...")
                    } else {
                        config.tr("Đo lại ping (X)", "Re-test ping (X)")
                    },
                    selected = isTestingAll,
                    scale = scale * 0.92f,
                    onClick = { if (!isTestingAll) triggerPingTest(autoPickBest = false) },
                    modifier = Modifier.testTag("tv_servers_retest_ping_btn")
                )

                TvChip(
                    text = config.tr("⚡ Tự chọn tốt nhất", "⚡ Auto-select Best"),
                    selected = false,
                    scale = scale * 0.92f,
                    onClick = { if (!isTestingAll) triggerPingTest(autoPickBest = true) },
                    modifier = Modifier.testTag("tv_servers_auto_best_btn")
                )

                TvChip(
                    text = config.tr("+ Máy chủ riêng", "+ Custom Proxy"),
                    selected = false,
                    scale = scale * 0.92f,
                    onClick = { showCustomDialog = true },
                    modifier = Modifier.testTag("tv_servers_add_custom_btn")
                )

                TvButton(
                    text = if (isVpnActive) {
                        config.tr("Tắt VPN", "Turn Off VPN")
                    } else {
                        config.tr("Bật VPN", "Turn On VPN")
                    },
                    variant = if (isVpnActive) TvButtonVariant.DANGER else TvButtonVariant.PRIMARY,
                    scale = scale * 0.9f,
                    heightDp = (32.dp * scale).coerceAtLeast(28.dp),
                    onClick = {
                        if (isVpnActive) VpnManager.disconnect(context) else VpnManager.connect(context)
                    },
                    modifier = Modifier.testTag("tv_servers_vpn_toggle_btn")
                )
            }
        }

        // 2. Country / Category Filter Chips
        val categories = listOf(
            "ALL" to config.tr("Tất cả (${allServers.size})", "All (${allServers.size})"),
            "JP" to config.tr("🇯🇵 Nhật Bản", "🇯🇵 Japan"),
            "KR" to config.tr("🇰🇷 Hàn Quốc", "🇰🇷 Korea"),
            "US" to config.tr("🇺🇸 Hoa Kỳ", "🇺🇸 USA"),
            "CUSTOM" to config.tr("Máy chủ riêng", "Custom")
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy((8.dp * scale).coerceAtLeast(6.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(categories, key = { "tv_srv_cat_${it.first}" }) { (code, label) ->
                TvChip(
                    text = label,
                    selected = selectedCategory == code,
                    scale = scale,
                    onClick = { selectedCategory = code },
                    modifier = Modifier.testTag("tv_server_category_${code.lowercase()}")
                )
            }
        }

        // 3. Server List (.list -> .row in tv-mockup.html)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("tv_servers_list"),
            verticalArrangement = Arrangement.spacedBy((10.dp * scale).coerceAtLeast(7.dp)),
            contentPadding = PaddingValues(bottom = (10.dp * scale).coerceAtLeast(6.dp))
        ) {
            items(
                items = filteredServers,
                key = { srv -> "tv_srv_row_${srv.id}" }
            ) { srv ->
                val isSelected = srv.id == currentServer.id
                val isRecommended = srv.id == recommendedServerId
                val measuredPing = serverPings[srv.id] ?: -1
                val unlockStatus = VpnManager.getServerUnlockStatus(srv, vpnState.unlockRecords)
                val distKm = remember(srv.id, vpnState.detectedGeoIp) {
                    RegionalProbeTester.getServerDistanceKm(
                        server = srv,
                        geo = vpnState.detectedGeoIp,
                        allowTimezoneFallback = true
                    )
                }

                TvServerRow(
                    server = srv,
                    config = config,
                    isSelected = isSelected,
                    isRecommended = isRecommended,
                    measuredPingMs = measuredPing,
                    distanceKm = distKm,
                    unlockStatus = unlockStatus,
                    isTestingAll = isTestingAll,
                    scale = scale,
                    onSelect = { onSelectServer(srv) },
                    onShowDetails = { detailServer = srv },
                    onDeleteCustom = if (srv.isCustom) {
                        { VpnManager.removeCustomServer(srv.id) }
                    } else null
                )
            }
        }
    }

    if (showCustomDialog) {
        CustomProxyDialog(
            onDismiss = { showCustomDialog = false },
            onAddServer = { newServer ->
                VpnManager.addCustomServer(newServer)
                onSelectServer(newServer)
                showCustomDialog = false
            }
        )
    }

    detailServer?.let { srv ->
        val pingMs = serverPings[srv.id] ?: -1
        val measurement = serverMeasurements[srv.id]
        val unlockStatus = VpnManager.getServerUnlockStatus(srv, vpnState.unlockRecords)
        ServerDetailDialog(
            server = srv,
            measurement = measurement,
            pingMs = pingMs,
            unlockStatus = unlockStatus,
            isEn = isEn,
            onDismiss = { detailServer = null }
        )
    }
}

@Composable
private fun TvServerRow(
    server: ServerLocation,
    config: AppConfig,
    isSelected: Boolean,
    isRecommended: Boolean,
    measuredPingMs: Int,
    distanceKm: Int?,
    unlockStatus: RegionUnlockStatus,
    isTestingAll: Boolean,
    scale: Float,
    onSelect: () -> Unit,
    onShowDetails: () -> Unit,
    onDeleteCustom: (() -> Unit)?
) {
    val isEn = config.isEnglishUI()
    val corner = (16.dp * scale).coerceAtLeast(12.dp)
    val rowBg = if (isSelected) Color(0xFF12261D) else TvSurface

    val distPart = if (distanceKm != null && distanceKm > 0) " · ~${distanceKm} km" else ""
    val unlockPart = when (unlockStatus) {
        RegionUnlockStatus.UNLOCKED -> " · ${config.tr("Đã mở khóa vùng", "Region Unlocked")}"
        RegionUnlockStatus.FAILED -> " · ${config.tr("Lỗi mở khóa", "Unlock Failed")}"
        RegionUnlockStatus.WRONG_REGION -> " · ${config.tr("Sai vùng", "Wrong Region")}"
        RegionUnlockStatus.UNTESTED -> ""
    }
    val subtitleText = "${server.localizedDescription(isEn)}$distPart$unlockPart"

    val pingText = when {
        measuredPingMs > 0 -> "$measuredPingMs ms"
        isTestingAll -> "..."
        else -> "--"
    }
    val pingColor = when {
        measuredPingMs in 1..75 -> TvAccent
        measuredPingMs in 76..140 -> TvCyan
        measuredPingMs > 140 -> TvWarn
        else -> TvMuted
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = (50.dp * scale).coerceAtLeast(40.dp))
            .clip(RoundedCornerShape(corner))
            .background(rowBg)
            .gamepadClickable(
                cornerRadius = corner,
                onClick = onSelect
            )
            .padding(
                horizontal = (15.dp * scale).coerceAtLeast(11.dp),
                vertical = (8.dp * scale).coerceAtLeast(6.dp)
            )
            .testTag("tv_server_row_${server.id}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy((2.dp * scale).coerceAtLeast(1.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((6.dp * scale).coerceAtLeast(4.dp))
            ) {
                Text(
                    text = "${server.flag} ${server.name}",
                    color = TvInk,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = (14.5f * scale).coerceAtLeast(12f).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isSelected) {
                    TvPill(
                        text = config.tr("Đang dùng", "Active"),
                        scale = scale * 0.85f,
                        tone = TvPillTone.GREEN
                    )
                } else if (isRecommended) {
                    TvPill(
                        text = config.tr("Khuyên dùng", "Recommended"),
                        scale = scale * 0.85f,
                        tone = TvPillTone.CYAN
                    )
                }
            }

            Text(
                text = subtitleText,
                color = TvMuted,
                fontSize = (11.5f * scale).coerceAtLeast(9.5f).sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width((10.dp * scale).coerceAtLeast(6.dp)))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((8.dp * scale).coerceAtLeast(5.dp))
        ) {
            Text(
                text = pingText,
                color = pingColor,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = (15f * scale).coerceAtLeast(12f).sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )

            Box(
                modifier = Modifier
                    .size((32.dp * scale).coerceAtLeast(26.dp))
                    .clip(CircleShape)
                    .background(TvSurface2)
                    .gamepadClickable(
                        cornerRadius = 99.dp,
                        onClick = onShowDetails
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ℹ",
                    color = TvMuted,
                    fontSize = (14f * scale).coerceAtLeast(11.5f).sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (onDeleteCustom != null) {
                Box(
                    modifier = Modifier
                        .size((32.dp * scale).coerceAtLeast(26.dp))
                        .clip(CircleShape)
                        .background(TvDangerBg)
                        .gamepadClickable(
                            cornerRadius = 99.dp,
                            onClick = onDeleteCustom
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        color = TvDanger,
                        fontSize = (13f * scale).coerceAtLeast(11f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
