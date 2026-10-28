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
        val sourceLabel: String,
        val speedMbpsHint: Int = 0
    )

    /**
     * Tải danh sách máy chủ sống từ nguồn VPNGate Academic Backbone + ProxyScrape cho đúng quốc gia hỗ trợ Xbox (JP, KR, US)
     * và xác minh kết nối TCP cổng chính trước khi trả về.
     * - Nếu người dùng đang ở tab "ALL" hoặc "CUSTOM", tự động lấy từ 3 quốc gia hỗ trợ chính ["JP", "KR", "US"]
     *   (tuyệt đối không gán countryCode = "ALL" gây lỗi "Sai vùng").
     */
    suspend fun fetchLiveProxies(countryCode: String = "JP"): List<ServerLocation> = withContext(Dispatchers.IO) {
        val targetCountries = when (val normalized = countryCode.trim().uppercase(Locale.ROOT)) {
            "ALL", "CUSTOM", "" -> listOf("JP", "KR", "US")
            else -> listOf(normalized)
        }

        val candidates = mutableListOf<CandidateNode>()
        val seenIpPort = HashSet<String>()

        // 1. Nguồn 1: VPNGate Academic Backbone API (nhiều máy chủ Nhật Bản & Hàn Quốc tốc độ 100-700 Mbps)
        try {
            val url = URL("http://www.vpngate.net/api/iphone/")
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
                            val speedBps = parts[4].trim().toLongOrNull() ?: 0L
                            val cc = parts[6].trim().uppercase(Locale.ROOT)
                            if (cc !in targetCountries || ip.isBlank()) continue

                            val b64Config = parts[14].trim()
                            val port = extractPortFromOvpnBase64(b64Config) ?: 443
                            val key = "$ip:$port"
                            if (port in 1..65535 && seenIpPort.add(key)) {
                                val mbps = (speedBps / 1_000_000L).toInt().coerceIn(20, 1000)
                                candidates.add(
                                    CandidateNode(
                                        countryCode = cc,
                                        ip = ip,
                                        port = port,
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

        // 2. Nguồn 2: ProxyScrape (bổ sung cho từng quốc gia mục tiêu)
        for (cc in targetCountries) {
            val currentCountForCc = candidates.count { it.countryCode == cc }
            if (currentCountForCc >= 12) continue
            try {
                val url = URL(
                    "https://api.proxyscrape.com/v2/?request=displayproxies&protocol=http,socks5&timeout=3000&country=$cc&ssl=all&anonymity=all"
                )
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3500
                    readTimeout = 3500
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }
                if (conn.responseCode == 200) {
                    var addedForCc = 0
                    BufferedReader(InputStreamReader(conn.inputStream)).useLines { lines ->
                        for (line in lines) {
                            val trimmed = line.trim()
                            if (trimmed.isNotBlank() && trimmed.contains(":")) {
                                val parts = trimmed.split(":")
                                if (parts.size == 2) {
                                    val ip = parts[0].trim()
                                    val port = parts[1].trim().toIntOrNull()
                                    val key = "$ip:$port"
                                    if (port != null && port in 1..65535 && seenIpPort.add(key)) {
                                        candidates.add(
                                            CandidateNode(
                                                countryCode = cc,
                                                ip = ip,
                                                port = port,
                                                sourceLabel = "Direct Relay"
                                            )
                                        )
                                        addedForCc++
                                        if (addedForCc >= 10) break
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

        // Giới hạn tối đa 20 ứng viên tốt nhất mỗi đợt cập nhật để kiểm tra cổng chính nhanh
        val prioritizedCandidates = candidates
            .sortedByDescending { it.speedMbpsHint }
            .take(20)

        if (prioritizedCandidates.isEmpty()) {
            return@withContext DEFAULT_SERVERS.filter { it.countryCode.uppercase(Locale.ROOT) in targetCountries }
        }

        val currentGen = VpnManager.getCurrentMeasurementGeneration()

        // Xác minh kết nối TCP cổng chính qua PingTester (tôn trọng giới hạn Semaphore(5) và ghi ngay vào cache)
        val verifiedServers = prioritizedCandidates.map { node ->
            async {
                val cc = node.countryCode.uppercase(Locale.ROOT)
                val flag = when (cc) {
                    "JP" -> "🇯🇵"
                    "KR" -> "🇰🇷"
                    "US" -> "🇺🇸"
                    "SG" -> "🇸🇬"
                    "AU" -> "🇦🇺"
                    "DE" -> "🇩🇪"
                    "GB" -> "🇬🇧"
                    else -> "🌐"
                }
                val regionName = when (cc) {
                    "JP" -> "Nhật Bản (Tokyo/Tsukuba)"
                    "KR" -> "Hàn Quốc (Seoul/Incheon)"
                    "US" -> "Hoa Kỳ"
                    else -> cc
                }
                val azureEndpoint = when (cc) {
                    "JP" -> "japaneast.cloudapp.azure.com"
                    "KR" -> "koreacentral.cloudapp.azure.com"
                    else -> "westus.cloudapp.azure.com"
                }
                val serverId = "live_${cc.lowercase(Locale.ROOT)}_${node.ip.replace('.', '_')}_${node.port}"
                val candidateServer = ServerLocation(
                    id = serverId,
                    name = "$flag ${node.sourceLabel} (${node.ip})",
                    flag = flag,
                    ip = node.ip,
                    port = node.port,
                    protocol = if (node.port == 1080 || node.port == 1081 || node.port == 4145) "socks5" else "http",
                    azureEndpoint = azureEndpoint,
                    description = "$regionName • Cổng :${node.port}",
                    speedMbps = node.speedMbpsHint,
                    countryCode = cc,
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
                        description = "$regionName • Cổng :${node.port} mở (${measurement.rttMs}ms)",
                        lastPingMs = measurement.rttMs
                    )
                } else {
                    null
                }
            }
        }.awaitAll().filterNotNull().sortedBy { it.lastPingMs ?: Int.MAX_VALUE }

        if (verifiedServers.isNotEmpty()) {
            verifiedServers
        } else {
            DEFAULT_SERVERS.filter { it.countryCode.uppercase(Locale.ROOT) in targetCountries }
        }
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
