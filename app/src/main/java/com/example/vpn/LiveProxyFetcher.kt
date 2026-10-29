package com.example.vpn

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object LiveProxyFetcher {

    private val REMOTE_PORT_REGEX = Regex("remote\\s+[0-9.]+\\s+(\\d+)")

    private data class CandidateNode(
        val countryCode: String,
        val ip: String,
        val port: Int,
        val protocol: String,
        val sourceLabel: String,
        val speedMbpsHint: Int = 0
    )

    /**
     * Tải danh sách máy chủ sống từ nguồn VPNGate Academic Backbone (HTTPS) + ProxyScrape (tách riêng http và socks5),
     * tra cứu quốc gia thật của từng IP qua [GeoIpDetector.resolveIpCountryCode] (kết quả tra thật luôn thắng tham số nguồn),
     * loại bỏ mọi IP nội bộ/loopback và trả về `emptyList()` khi không có ứng viên sống (không âm thầm trả DEFAULT_SERVERS).
     */
    suspend fun fetchLiveProxies(countryCode: String = "JP"): List<ServerLocation> = withContext(Dispatchers.IO) {
        val normalizedFilter = countryCode.trim().uppercase(Locale.ROOT)
        val filterByCountry = normalizedFilter.isNotEmpty() && normalizedFilter != "ALL" && normalizedFilter != "CUSTOM"
        val targetCountries = if (filterByCountry) {
            listOf(normalizedFilter)
        } else {
            listOf("JP", "KR", "US")
        }

        val candidates = mutableListOf<CandidateNode>()
        val seenIpPort = HashSet<String>()

        // 1. Nguồn 1: VPNGate Academic Backbone API qua HTTPS
        try {
            val url = URL("https://www.vpngate.net/api/iphone/")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4500
                readTimeout = 4500
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (conn.responseCode == 200) {
                BufferedReader(InputStreamReader(conn.inputStream)).useLines { lines ->
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.isEmpty() || trimmed.startsWith("*") || trimmed.startsWith("#")) continue
                        val parts = trimmed.split(",")
                        if (parts.size > 14) {
                            val ip = parts[1].trim()
                            if (GeoIpDetector.isPrivateOrLoopbackIp(ip)) continue
                            val speedBps = parts[4].trim().toLongOrNull() ?: 0L
                            val sourceCc = parts[6].trim().uppercase(Locale.ROOT)
                            val resolvedCc = GeoIpDetector.resolveIpCountryCode(ip, allowNetworkFallback = false)
                                .ifBlank { sourceCc }
                            if (filterByCountry && resolvedCc != normalizedFilter) continue
                            if (!filterByCountry && resolvedCc.isNotBlank() && resolvedCc !in targetCountries && sourceCc !in targetCountries) continue

                            val b64Config = parts[14].trim()
                            val port = extractPortFromOvpnBase64(b64Config) ?: 443
                            val key = "$ip:$port"
                            if (port in 1..65535 && seenIpPort.add(key)) {
                                val mbps = (speedBps / 1_000_000L).toInt().coerceIn(20, 1000)
                                candidates.add(
                                    CandidateNode(
                                        countryCode = resolvedCc,
                                        ip = ip,
                                        port = port,
                                        protocol = "http",
                                        sourceLabel = "VPNGate Fiber",
                                        speedMbpsHint = mbps
                                    )
                                )
                            }
                        }
                    }
                }
            }
            conn.disconnect()
        } catch (_: Exception) {
        }

        // 2. Nguồn 2: ProxyScrape (tách riêng 2 yêu cầu protocol=http và protocol=socks5 để gán đúng giao thức)
        val protocols = listOf("http", "socks5")
        for (cc in targetCountries) {
            val currentCountForCc = candidates.count { it.countryCode == cc }
            if (currentCountForCc >= 12) continue
            for (proto in protocols) {
                try {
                    val url = URL(
                        "https://api.proxyscrape.com/v2/?request=displayproxies&protocol=$proto&timeout=3000&country=$cc&ssl=all&anonymity=all"
                    )
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 3500
                        readTimeout = 3500
                        setRequestProperty("User-Agent", "Mozilla/5.0")
                    }
                    if (conn.responseCode == 200) {
                        var addedForProto = 0
                        BufferedReader(InputStreamReader(conn.inputStream)).useLines { lines ->
                            for (line in lines) {
                                val trimmed = line.trim()
                                if (trimmed.isNotBlank() && trimmed.contains(":")) {
                                    val parts = trimmed.split(":")
                                    if (parts.size == 2) {
                                        val ip = parts[0].trim()
                                        val port = parts[1].trim().toIntOrNull()
                                        if (port == null || port !in 1..65535 || GeoIpDetector.isPrivateOrLoopbackIp(ip)) continue
                                        val resolvedCc = GeoIpDetector.resolveIpCountryCode(ip, allowNetworkFallback = false)
                                        // Kết quả tra thật từ IP luôn thắng tham số country=$cc của nguồn proxy
                                        val effectiveCc = resolvedCc.ifBlank { cc }
                                        if (filterByCountry && effectiveCc != normalizedFilter) continue
                                        val key = "$ip:$port"
                                        if (seenIpPort.add(key)) {
                                            candidates.add(
                                                CandidateNode(
                                                    countryCode = effectiveCc,
                                                    ip = ip,
                                                    port = port,
                                                    protocol = proto,
                                                    sourceLabel = "Direct Relay"
                                                )
                                            )
                                            addedForProto++
                                            if (addedForProto >= 6) break
                                        }
                                    }
                                }
                            }
                        }
                    }
                    conn.disconnect()
                } catch (_: Exception) {
                }
            }
        }

        // Giới hạn tối đa 20 ứng viên tốt nhất mỗi đợt cập nhật để kiểm tra cổng chính nhanh
        val prioritizedCandidates = candidates
            .sortedByDescending { it.speedMbpsHint }
            .take(20)

        if (prioritizedCandidates.isEmpty()) {
            return@withContext emptyList()
        }

        val currentGen = VpnManager.getCurrentMeasurementGeneration()
        val isEn = try {
            com.example.data.SettingsRepository.config.value.isEnglishUI()
        } catch (_: Exception) {
            false
        }

        // Xác minh kết nối TCP cổng chính qua PingTester (tôn trọng giới hạn Semaphore(5) và ghi ngay vào cache)
        val verifiedServers = prioritizedCandidates.map { node ->
            async {
                val realCc = GeoIpDetector.resolveIpCountryCode(node.ip, allowNetworkFallback = true)
                    .ifBlank { node.countryCode }
                    .trim()
                    .uppercase(Locale.ROOT)
                if (filterByCountry && realCc.isNotBlank() && realCc != normalizedFilter) {
                    return@async null
                }
                val flag = GeoIpDetector.getFlagEmoji(realCc)
                val countryDisplayName = GeoIpDetector.getCountryDisplayName(realCc, isEn)
                val azureEndpoint = GeoIpDetector.getAzureEndpointForCountry(realCc)
                val safeCcId = realCc.ifBlank { "unk" }.lowercase(Locale.ROOT)
                val serverId = "live_${safeCcId}_${node.ip.replace('.', '_')}_${node.port}"
                val candidateServer = ServerLocation(
                    id = serverId,
                    name = "$flag $countryDisplayName • ${node.ip}",
                    flag = flag,
                    ip = node.ip,
                    port = node.port,
                    protocol = node.protocol,
                    azureEndpoint = azureEndpoint,
                    description = "$countryDisplayName • Cổng :${node.port}",
                    speedMbps = node.speedMbpsHint,
                    countryCode = realCc,
                    isVerified = true
                )

                val measurement = PingTester.measureServerEndpoint(
                    server = candidateServer,
                    timeoutMs = 850,
                    useCache = true
                )
                if (measurement.isPrimaryPortAlive) {
                    VpnManager.recordMeasurementIfCurrentGeneration(currentGen, candidateServer.id, measurement)
                    candidateServer.copy(
                        description = "$countryDisplayName • Cổng :${node.port} mở (${measurement.rttMs}ms)",
                        lastPingMs = measurement.rttMs
                    )
                } else {
                    null
                }
            }
        }.awaitAll().filterNotNull().sortedBy { it.lastPingMs ?: Int.MAX_VALUE }

        verifiedServers
    }

    private fun extractPortFromOvpnBase64(b64: String): Int? {
        if (b64.isBlank()) return null
        return try {
            val decoded = String(Base64.decode(b64, Base64.DEFAULT), Charsets.UTF_8)
            val match = REMOTE_PORT_REGEX.find(decoded)
            match?.groupValues?.getOrNull(1)?.toIntOrNull()
        } catch (_: Exception) {
            null
        }
    }
}
