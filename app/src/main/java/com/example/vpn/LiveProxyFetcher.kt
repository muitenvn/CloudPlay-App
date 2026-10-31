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
import java.util.concurrent.atomic.AtomicInteger

data class LiveProxySyncResult(
    val verifiedServers: List<ServerLocation>,
    val filteredOutErrorCount: Int
)

object LiveProxyFetcher {

    const val MAX_ALLOWED_LIVE_PING_MS = 250
    const val MIN_VERIFIED_SPEED_MBPS = 260

    private val REMOTE_PORT_REGEX = Regex("remote\\s+[0-9.]+\\s+(\\d+)")

    // Các cổng dịch vụ hệ thống bị loại bỏ khi kiểm tra kết nối proxy công cộng
    private val DISALLOWED_PORTS = setOf(
        21, 22, 23, 25, 53, 69, 110, 111, 135, 137, 138, 139,
        143, 161, 389, 445, 512, 513, 514, 1433, 1521, 2049,
        3306, 3389, 5432, 5900, 6379, 11211, 27017
    )

    private val COMMON_PROXY_PORTS = setOf(443, 995, 8443, 1468, 1557, 1828, 1980, 7551)

    private data class CandidateNode(
        val countryCode: String,
        val ip: String,
        val port: Int,
        val protocol: String,
        val sourceLabel: String,
        val isAcademicBackbone: Boolean = false
    )

    fun isValidPublicEndpoint(ip: String, port: Int): Boolean {
        if (port !in 1..65535 || port in DISALLOWED_PORTS) return false
        if (GeoIpDetector.isPrivateOrLoopbackIp(ip)) return false
        val trimmed = ip.trim()
        // Từ chối các dải IP bogon/multicast/broadcast
        if (trimmed.startsWith("0.") || trimmed.startsWith("224.") ||
            trimmed.startsWith("240.") || trimmed.startsWith("255.")
        ) {
            return false
        }
        return true
    }

    fun computeConnectionPriorityScore(
        port: Int,
        rttMs: Int,
        isAcademicBackbone: Boolean,
        isUnstable: Boolean
    ): Int {
        if (isUnstable || rttMs <= 0 || rttMs > MAX_ALLOWED_LIVE_PING_MS) return 0
        var score = if (isAcademicBackbone) 90 else 84
        if (port in COMMON_PROXY_PORTS) score += 4
        score += when {
            rttMs <= 65 -> 5
            rttMs <= 110 -> 3
            rttMs <= 170 -> 1
            else -> 0
        }
        return score.coerceIn(85, 99)
    }

    suspend fun fetchLiveProxies(countryCode: String = "ALL"): List<ServerLocation> {
        return fetchAndCheckPublicProxies(countryCode).verifiedServers
    }

    /**
     * Tự động quét và kiểm tra kết nối của các máy chủ từ danh sách công cộng (VPNGate, ProxyScrape):
     * 1. Thu thập ứng viên từ danh sách công cộng VPNGate + ProxyScrape (http và socks5).
     * 2. Lọc bỏ các IP nội bộ/bogon, cổng hệ thống (`DISALLOWED_PORTS`), hoặc khác vùng hỗ trợ Xbox.
     * 3. Đo kiểm cổng chính thực tế qua [PingTester.measureServerEndpoint]: loại bỏ máy chủ không phản hồi cổng chính,
     *    chỉ mở cổng phụ, chập chờn (`isUnstable`) hoặc ping > 250ms.
     * 4. Gắn nhãn trạng thái `"Đã kiểm tra kết nối"` (bộ lọc chỉ kiểm tra cổng, IP và ping, không chứng minh độ tin cậy của đơn vị vận hành).
     */
    suspend fun fetchAndCheckPublicProxies(countryCode: String = "ALL"): LiveProxySyncResult = withContext(Dispatchers.IO) {
        if (VpnManager.isStreamActive()) {
            return@withContext LiveProxySyncResult(emptyList(), 0)
        }
        val normalizedFilter = countryCode.trim().uppercase(Locale.ROOT)
        val filterByCountry = normalizedFilter.isNotEmpty() && normalizedFilter != "ALL" && normalizedFilter != "CUSTOM"
        val targetCountries = if (filterByCountry) {
            listOf(normalizedFilter)
        } else {
            listOf("JP", "KR", "US")
        }

        val candidates = java.util.Collections.synchronizedList(mutableListOf<CandidateNode>())
        val seenIpPort = java.util.Collections.synchronizedSet(HashSet<String>())
        val filteredErrorCounter = AtomicInteger(0)

        // 1. Thu thập song song từ danh sách công cộng VPNGate + ProxyScrape
        kotlinx.coroutines.coroutineScope {
            val fetchJobs = mutableListOf<kotlinx.coroutines.Deferred<Unit>>()

            fetchJobs.add(
                async {
                    try {
                        val url = URL("https://www.vpngate.net/api/iphone/")
                        val conn = (url.openConnection() as HttpURLConnection).apply {
                            connectTimeout = 2400
                            readTimeout = 2400
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
                                        val b64Config = parts[14].trim()
                                        val port = extractPortFromOvpnBase64(b64Config) ?: 443
                                        if (!isValidPublicEndpoint(ip, port)) {
                                            filteredErrorCounter.incrementAndGet()
                                            continue
                                        }
                                        val sourceCc = parts[6].trim().uppercase(Locale.ROOT)
                                        val (isRegionValid, resolvedCc) = GeoIpDetector.fastCheckIpSecurityAndRegion(
                                            ip = ip,
                                            declaredCountryCode = sourceCc,
                                            allowFastNetworkFallback = false
                                        )
                                        if (!isRegionValid || resolvedCc.isBlank()) {
                                            filteredErrorCounter.incrementAndGet()
                                            continue
                                        }
                                        if (filterByCountry && resolvedCc != normalizedFilter) continue
                                        if (!filterByCountry && resolvedCc !in targetCountries && sourceCc !in targetCountries) continue

                                        val key = "$ip:$port"
                                        if (seenIpPort.add(key)) {
                                            GeoIpDetector.cacheResolvedIpCountry(ip, resolvedCc)
                                            candidates.add(
                                                CandidateNode(
                                                    countryCode = resolvedCc,
                                                    ip = ip,
                                                    port = port,
                                                    protocol = "http",
                                                    sourceLabel = "VPNGate Public",
                                                    isAcademicBackbone = true
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
                }
            )

            val protocols = listOf("http", "socks5")
            for (cc in targetCountries) {
                for (proto in protocols) {
                    fetchJobs.add(
                        async {
                            try {
                                val url = URL(
                                    "https://api.proxyscrape.com/v2/?request=displayproxies&protocol=$proto&timeout=2000&country=$cc&ssl=all&anonymity=all"
                                )
                                val conn = (url.openConnection() as HttpURLConnection).apply {
                                    connectTimeout = 2000
                                    readTimeout = 2000
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
                                                    if (port == null || !isValidPublicEndpoint(ip, port)) {
                                                        filteredErrorCounter.incrementAndGet()
                                                        continue
                                                    }
                                                    val (isRegionValid, effectiveCc) = GeoIpDetector.fastCheckIpSecurityAndRegion(
                                                        ip = ip,
                                                        declaredCountryCode = cc,
                                                        allowFastNetworkFallback = false
                                                    )
                                                    if (!isRegionValid || effectiveCc.isBlank()) {
                                                        filteredErrorCounter.incrementAndGet()
                                                        continue
                                                    }
                                                    if (filterByCountry && effectiveCc != normalizedFilter) continue
                                                    val key = "$ip:$port"
                                                    if (seenIpPort.add(key)) {
                                                        GeoIpDetector.cacheResolvedIpCountry(ip, effectiveCc)
                                                        candidates.add(
                                                            CandidateNode(
                                                                countryCode = effectiveCc,
                                                                ip = ip,
                                                                port = port,
                                                                protocol = proto,
                                                                sourceLabel = "Public Proxy",
                                                                isAcademicBackbone = false
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
                    )
                }
            }

            fetchJobs.awaitAll()
        }

        if (VpnManager.isStreamActive()) {
            return@withContext LiveProxySyncResult(emptyList(), 0)
        }

        val prioritizedCandidates = synchronized(candidates) {
            candidates
                .sortedWith(
                    compareByDescending<CandidateNode> { it.isAcademicBackbone }
                )
                .take(18)
        }

        if (prioritizedCandidates.isEmpty()) {
            return@withContext LiveProxySyncResult(
                verifiedServers = emptyList(),
                filteredOutErrorCount = filteredErrorCounter.get()
            )
        }

        val currentGen = VpnManager.getCurrentMeasurementGeneration()
        val isEn = try {
            com.example.data.SettingsRepository.config.value.isEnglishUI()
        } catch (_: Exception) {
            false
        }

        // Kiểm tra kết nối TCP cổng chính song song
        val verifiedServers = prioritizedCandidates.map { node ->
            async {
                if (VpnManager.isStreamActive()) return@async null
                val (isValidRegion, realCc) = GeoIpDetector.fastCheckIpSecurityAndRegion(
                    ip = node.ip,
                    declaredCountryCode = node.countryCode,
                    allowFastNetworkFallback = false
                )
                if (!isValidRegion || (realCc.isNotBlank() && !GeoIpDetector.SUPPORTED_COUNTRIES.contains(realCc))) {
                    filteredErrorCounter.incrementAndGet()
                    return@async null
                }
                if (filterByCountry && realCc.isNotBlank() && realCc != normalizedFilter) {
                    filteredErrorCounter.incrementAndGet()
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
                    description = "$countryDisplayName • Đã kiểm tra kết nối",
                    speedMbps = 0,
                    countryCode = realCc,
                    isVerified = false
                )

                val measurement = PingTester.measureServerEndpoint(
                    server = candidateServer,
                    timeoutMs = MAX_ALLOWED_LIVE_PING_MS,
                    useCache = true
                )
                if (measurement.isPrimaryPortAlive && !measurement.isUnstable && measurement.rttMs in 1..MAX_ALLOWED_LIVE_PING_MS) {
                    VpnManager.recordMeasurementIfCurrentGeneration(currentGen, candidateServer.id, measurement)
                    val priorityScore = computeConnectionPriorityScore(
                        port = node.port,
                        rttMs = measurement.rttMs,
                        isAcademicBackbone = node.isAcademicBackbone,
                        isUnstable = measurement.isUnstable
                    )
                    candidateServer.copy(
                        description = "$countryDisplayName • ${node.sourceLabel} • Đã kiểm tra kết nối (${measurement.rttMs}ms)",
                        lastPingMs = measurement.rttMs,
                        trustScore = priorityScore,
                        safetyBadge = "Đã kiểm tra kết nối"
                    )
                } else {
                    filteredErrorCounter.incrementAndGet()
                    null
                }
            }
        }.awaitAll().filterNotNull().sortedWith(
            compareByDescending<ServerLocation> { it.trustScore }
                .thenBy { it.lastPingMs ?: Int.MAX_VALUE }
        )

        LiveProxySyncResult(
            verifiedServers = verifiedServers,
            filteredOutErrorCount = filteredErrorCounter.get()
        )
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
