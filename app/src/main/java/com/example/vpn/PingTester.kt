package com.example.vpn

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.roundToInt

enum class ServerPingStatus {
    UNTESTED,
    TESTING,
    MEASURED_PRIMARY,
    MEASURED_FALLBACK_ONLY,
    UNREACHABLE,
    EXPIRED
}

enum class PingMeasurementType {
    TCP_PRIMARY_PORT,
    TCP_FALLBACK_PORT,
    NONE
}

/**
 * Kết quả đo độ trễ kết nối TCP thực tế theo đúng Endpoint (IP + Port + Phương pháp đo).
 * - Không cộng, trừ, modulo hay nội suy theo khoảng cách địa lý.
 * - Phân biệt rõ cổng cấu hình chính (MEASURED_PRIMARY) và cổng dự phòng :443/:80 (MEASURED_FALLBACK_ONLY).
 */
data class EndpointPingMeasurement(
    val rttMs: Int,
    val minRttMs: Int = rttMs,
    val maxRttMs: Int = rttMs,
    val configuredPort: Int,
    val measuredPort: Int,
    val resolvedIp: String,
    val measurementType: PingMeasurementType,
    val status: ServerPingStatus,
    val isUnstable: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
) {
    val isPrimaryPortAlive: Boolean
        get() = status == ServerPingStatus.MEASURED_PRIMARY && rttMs > 0

    val isFallbackPortOnly: Boolean
        get() = status == ServerPingStatus.MEASURED_FALLBACK_ONLY && rttMs > 0

    val isReachableAnyPort: Boolean
        get() = isPrimaryPortAlive || isFallbackPortOnly

    fun isExpired(nowMs: Long = System.currentTimeMillis(), ttlMs: Long = PingTester.ENDPOINT_CACHE_TTL_MS): Boolean {
        return (nowMs - timestampMs) > ttlMs
    }

    /**
     * Nhãn hiển thị trung thực trên giao diện (phân biệt rõ cổng chính, cổng phụ :443/:80 và mạng chập chờn).
     * Khi `showPort = false` (ở giao diện thường), ẩn số cổng để giao diện gọn gàng; ở màn chi tiết (`showPort = true`), hiện đầy đủ số cổng.
     */
    fun formatDisplayBadge(isEn: Boolean, showPort: Boolean = true): String {
        return when (status) {
            ServerPingStatus.UNTESTED -> if (isEn) "Not tested" else "Chưa đo"
            ServerPingStatus.TESTING -> if (isEn) "Testing..." else "Đang đo..."
            ServerPingStatus.EXPIRED -> if (isEn) "Expired" else "Đã hết hạn"
            ServerPingStatus.UNREACHABLE -> if (isEn) "Unreachable" else "Không đo được"
            ServerPingStatus.MEASURED_PRIMARY -> {
                val portSuffix = if (showPort) " (:$measuredPort)" else ""
                if (isUnstable) {
                    if (isEn) "${rttMs}ms$portSuffix • Unstable" else "${rttMs}ms$portSuffix • Chập chờn"
                } else {
                    "${rttMs}ms$portSuffix"
                }
            }
            ServerPingStatus.MEASURED_FALLBACK_ONLY -> {
                if (showPort) {
                    if (isEn) {
                        "${rttMs}ms (:$measuredPort • Main :$configuredPort closed)"
                    } else {
                        "${rttMs}ms (:$measuredPort • Cổng :$configuredPort đóng)"
                    }
                } else {
                    if (isEn) {
                        "${rttMs}ms (Fallback port)"
                    } else {
                        "${rttMs}ms (Cổng phụ)"
                    }
                }
            }
        }
    }
}

object PingTester {

    /** Giới hạn cứng số kết nối Socket TCP thực sự mở đồng thời trên toàn ứng dụng (4–6 socket) */
    const val MAX_CONCURRENT_SOCKETS = 5

    /** Thời gian lưu cache kết quả đo trực tiếp theo ip:port (5 phút = 300.000 ms) */
    const val ENDPOINT_CACHE_TTL_MS = 5 * 60 * 1000L

    private val socketSemaphore = Semaphore(MAX_CONCURRENT_SOCKETS)

    // Bộ đếm phục vụ kiểm thử nghiệm thu giới hạn socket mở đồng thời
    private val activeOpenSockets = AtomicInteger(0)
    private val peakOpenSockets = AtomicInteger(0)

    // Bộ nhớ đệm kết quả đo theo khóa chuẩn hóa "host:configuredPort"
    private val endpointCache = ConcurrentHashMap<String, EndpointPingMeasurement>()

    // Lưu số đo cổng chính thực tế gần nhất theo từng quốc gia (chỉ lấy từ cổng chính sống)
    private val recentCountryMeasuredRtt = ConcurrentHashMap<String, Int>()

    fun getActiveOpenSocketsCount(): Int = activeOpenSockets.get()
    fun getPeakOpenSocketsCount(): Int = peakOpenSockets.get()
    fun resetSocketCountersForTest() {
        activeOpenSockets.set(0)
        peakOpenSockets.set(0)
    }

    fun buildEndpointKey(host: String, port: Int): String {
        return "${host.trim().lowercase(Locale.ROOT)}:$port"
    }

    fun getCachedMeasurement(server: ServerLocation, nowMs: Long = System.currentTimeMillis()): EndpointPingMeasurement? {
        val key = buildEndpointKey(server.ip, server.port)
        val cached = endpointCache[key] ?: return null
        if (cached.isExpired(nowMs, ENDPOINT_CACHE_TTL_MS)) {
            return cached.copy(status = ServerPingStatus.EXPIRED)
        }
        return cached
    }

    fun clearCache() {
        endpointCache.clear()
        recentCountryMeasuredRtt.clear()
    }

    fun invalidateServerCache(server: ServerLocation) {
        endpointCache.remove(buildEndpointKey(server.ip, server.port))
    }

    fun putCacheForTest(host: String, port: Int, measurement: EndpointPingMeasurement) {
        endpointCache[buildEndpointKey(host, port)] = measurement
    }

    /**
     * Tính giá trị trung vị (median) và biên độ (min, max) từ danh sách các mẫu đo thực tế hợp lệ (> 0).
     * Tuyệt đối KHÔNG trừ 960, KHÔNG lấy modulo, KHÔNG cộng khoảng cách địa lý.
     */
    fun summarizeRawSamples(samples: List<Int>): Triple<Int, Int, Int>? {
        val valid = samples.filter { it > 0 }.sorted()
        if (valid.isEmpty()) return null
        val minVal = valid.first()
        val maxVal = valid.last()
        val medianVal = if (valid.size % 2 == 1) {
            valid[valid.size / 2]
        } else {
            val mid = valid.size / 2
            ((valid[mid - 1] + valid[mid]) / 2.0).roundToInt()
        }
        return Triple(medianVal, minVal, maxVal)
    }

    /**
     * Đo độ trễ kết nối TCP trực tiếp tới máy chủ đang chọn (cổng cấu hình chính).
     */
    suspend fun measureDirectLatency(timeoutMs: Int = 1000): Int = withContext(Dispatchers.IO) {
        val currentServer = VpnManager.state.value.currentServer
        if (currentServer.ip.isBlank() || currentServer.port <= 0) return@withContext -1
        val safeTimeout = timeoutMs.coerceIn(350, 1000)

        val first = measureSingleTcpRttWithPermit(currentServer.ip, currentServer.port, safeTimeout).first
        if (first <= 0) return@withContext -1

        val samples = mutableListOf(first)
        if (first < 400) {
            val second = measureSingleTcpRttWithPermit(currentServer.ip, currentServer.port, safeTimeout).first
            if (second > 0) samples.add(second)
        }
        val summary = summarizeRawSamples(samples) ?: return@withContext -1
        val medianRtt = summary.first
        if (medianRtt > 0) {
            recentCountryMeasuredRtt[currentServer.countryCode.uppercase(Locale.ROOT)] = medianRtt
        }
        medianRtt
    }

    /**
     * Đo đầy đủ một máy chủ với sự phân tách rõ ràng giữa cổng chính (`server.port`) và cổng dự phòng (`:443`, `:80`):
     * 1. Nếu `useCache = true` và kết quả trong cache còn hạn (<= 5 phút), trả về ngay từ cache.
     * 2. Chạy song song cổng riêng (`server.port`, timeout <= 900ms) và cổng phụ `:443`, `:80` (timeout <= 700ms),
     *    nhưng mỗi socket mở đều đi qua `socketSemaphore = Semaphore(5)` để tổng số socket mở không vượt quá 5.
     * 3. Nếu cổng chính (`server.port`) phản hồi: trả về `MEASURED_PRIMARY` (dùng để xếp hạng chính thức).
     * 4. Nếu cổng chính không phản hồi nhưng `:443` hoặc `:80` phản hồi: trả về `MEASURED_FALLBACK_ONLY` (ghi rõ cổng phụ đã phản hồi, không đánh tráo làm cổng chính).
     */
    suspend fun measureServerEndpoint(
        server: ServerLocation,
        timeoutMs: Int = 900,
        useCache: Boolean = true,
        customProbeExecutor: (suspend (String, Int, Int) -> Pair<Int, String>)? = null
    ): EndpointPingMeasurement = withContext(Dispatchers.IO) {
        val host = server.ip.trim()
        val configuredPort = server.port
        val now = System.currentTimeMillis()

        if (host.isBlank() || configuredPort <= 0) {
            return@withContext EndpointPingMeasurement(
                rttMs = -1,
                configuredPort = configuredPort,
                measuredPort = configuredPort,
                resolvedIp = "",
                measurementType = PingMeasurementType.NONE,
                status = ServerPingStatus.UNREACHABLE,
                timestampMs = now
            )
        }

        val cacheKey = buildEndpointKey(host, configuredPort)
        if (useCache) {
            val cached = endpointCache[cacheKey]
            if (cached != null && !cached.isExpired(now, ENDPOINT_CACHE_TTL_MS)) {
                return@withContext cached
            }
        }

        val primaryTimeout = timeoutMs.coerceIn(300, 1000)
        val fallbackTimeout = (primaryTimeout.coerceAtMost(700)).coerceAtLeast(300)
        val probeFn: suspend (String, Int, Int) -> Pair<Int, String> =
            customProbeExecutor ?: { h, p, t -> measureSingleTcpRttWithPermit(h, p, t) }

        val measurement = coroutineScope {
            // Ưu tiên đo cổng cấu hình chính trước để không chiếm giữ giấy phép Semaphore cho cổng phụ (:80/:443) khi cổng chính đang sống
            val (firstRtt, primaryResolvedIp) = probeFn(host, configuredPort, primaryTimeout)
            val primarySamples = if (firstRtt <= 0) {
                emptyList()
            } else {
                val list = mutableListOf(firstRtt)
                if (firstRtt < 280) {
                    val (secondRtt, _) = probeFn(host, configuredPort, primaryTimeout.coerceAtMost(500))
                    if (secondRtt > 0) list.add(secondRtt)
                }
                list
            }

            val primarySummary = summarizeRawSamples(primarySamples)

            if (primarySummary != null) {
                val (medianRtt, minRtt, maxRtt) = primarySummary
                val unstable = (maxRtt - minRtt >= 80) || (medianRtt >= 1000)
                EndpointPingMeasurement(
                    rttMs = medianRtt,
                    minRttMs = minRtt,
                    maxRttMs = maxRtt,
                    configuredPort = configuredPort,
                    measuredPort = configuredPort,
                    resolvedIp = primaryResolvedIp.ifBlank { host },
                    measurementType = PingMeasurementType.TCP_PRIMARY_PORT,
                    status = ServerPingStatus.MEASURED_PRIMARY,
                    isUnstable = unstable,
                    timestampMs = System.currentTimeMillis()
                )
            } else {
                // Cổng chính không phản hồi -> thử song song cổng phụ :443 và :80 trên cùng IP (timeout <= 700ms)
                val fallbackPorts = listOf(443, 80).filter { it != configuredPort }
                val fallbackDeferreds = fallbackPorts.map { fbPort ->
                    fbPort to async {
                        probeFn(host, fbPort, fallbackTimeout)
                    }
                }

                var bestFallbackPort = -1
                var bestFallbackRtt = Int.MAX_VALUE
                var bestResolvedIp = primaryResolvedIp

                for ((fbPort, deferred) in fallbackDeferreds) {
                    val (fbRtt, fbIp) = try {
                        deferred.await()
                    } catch (_: Exception) {
                        Pair(-1, "")
                    }
                    if (fbRtt in 1 until bestFallbackRtt) {
                        bestFallbackRtt = fbRtt
                        bestFallbackPort = fbPort
                        if (fbIp.isNotBlank()) bestResolvedIp = fbIp
                    }
                }

                if (bestFallbackPort > 0 && bestFallbackRtt < Int.MAX_VALUE) {
                    EndpointPingMeasurement(
                        rttMs = bestFallbackRtt,
                        minRttMs = bestFallbackRtt,
                        maxRttMs = bestFallbackRtt,
                        configuredPort = configuredPort,
                        measuredPort = bestFallbackPort,
                        resolvedIp = bestResolvedIp.ifBlank { host },
                        measurementType = PingMeasurementType.TCP_FALLBACK_PORT,
                        status = ServerPingStatus.MEASURED_FALLBACK_ONLY,
                        isUnstable = bestFallbackRtt >= 1000,
                        timestampMs = System.currentTimeMillis()
                    )
                } else {
                    EndpointPingMeasurement(
                        rttMs = -1,
                        minRttMs = -1,
                        maxRttMs = -1,
                        configuredPort = configuredPort,
                        measuredPort = configuredPort,
                        resolvedIp = bestResolvedIp.ifBlank { host },
                        measurementType = PingMeasurementType.NONE,
                        status = ServerPingStatus.UNREACHABLE,
                        isUnstable = false,
                        timestampMs = System.currentTimeMillis()
                    )
                }
            }
        }

        // Nếu địa chỉ IP phân giải thực tế thay đổi so với lần cache trước, ghi nhận mới
        endpointCache[cacheKey] = measurement
        if (measurement.isPrimaryPortAlive) {
            recentCountryMeasuredRtt[server.countryCode.uppercase(Locale.ROOT)] = measurement.rttMs
        }
        measurement
    }

    /**
     * Hàm tương thích trả về RTT (ms) cho máy chủ:
     * - Trả về `rttMs` (> 0) khi đo được, hoặc `-1` khi không đo được.
     * - Chi tiết cổng chính/cổng phụ được lưu đầy đủ trong `EndpointPingMeasurement`.
     */
    suspend fun measureServerLatency(server: ServerLocation, timeoutMs: Int = 900, useCache: Boolean = false): Int {
        val result = measureServerEndpoint(server, timeoutMs = timeoutMs, useCache = useCache)
        return if (result.isReachableAnyPort) result.rttMs else -1
    }

    /**
     * Mở kết nối TCP dưới sự kiểm soát của `socketSemaphore = Semaphore(5)` và đóng socket tức thì khi hủy/timeout.
     * Trả về `Pair(rttMs, resolvedIpAddress)`.
     */
    suspend fun measureSingleTcpRttWithPermit(
        host: String,
        port: Int,
        timeoutMs: Int,
        simulatedConnectBlock: (() -> Pair<Int, String>)? = null
    ): Pair<Int, String> = socketSemaphore.withPermit {
        val currentOpen = activeOpenSockets.incrementAndGet()
        peakOpenSockets.updateAndGet { peak -> maxOf(peak, currentOpen) }
        try {
            if (simulatedConnectBlock != null) {
                return@withPermit simulatedConnectBlock()
            }
            measureSingleTcpRttCancellable(host, port, timeoutMs)
        } finally {
            activeOpenSockets.decrementAndGet()
        }
    }

    private suspend fun measureSingleTcpRttCancellable(
        host: String,
        port: Int,
        timeoutMs: Int
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val socket = Socket()
        try {
            socket.tcpNoDelay = true
            socket.soTimeout = timeoutMs
            val resolvedAddr = InetAddress.getByName(host)
            val resolvedIp = resolvedAddr.hostAddress ?: host
            val startNano = System.nanoTime()
            socket.connect(InetSocketAddress(resolvedAddr, port), timeoutMs)
            val durationMs = ((System.nanoTime() - startNano) / 1_000_000.0).roundToInt()
            val validRtt = if (durationMs in 1..timeoutMs) durationMs else -1
            Pair(validRtt, resolvedIp)
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            Pair(-1, "")
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun testServerPing(server: ServerLocation, timeoutMs: Int = 1000): Int =
        measureServerLatency(server, timeoutMs, useCache = false)

    /**
     * Đo độ trễ TCP tới một host:port cụ thể (lấy trung vị từ tối đa 3 mẫu hợp lệ, không biến đổi số đo).
     */
    suspend fun measureLatency(host: String, port: Int = 443, timeoutMs: Int = 1000): Int = withContext(Dispatchers.IO) {
        if (host.isBlank()) return@withContext -1
        val safeTimeout = timeoutMs.coerceIn(300, 2500)
        val first = measureSingleTcpRttWithPermit(host, port, safeTimeout).first
        if (first <= 0) return@withContext -1
        val samples = mutableListOf(first)
        val second = measureSingleTcpRttWithPermit(host, port, safeTimeout).first
        if (second > 0) samples.add(second)
        val third = measureSingleTcpRttWithPermit(host, port, safeTimeout).first
        if (third > 0) samples.add(third)
        summarizeRawSamples(samples)?.first ?: -1
    }

    suspend fun isHostReachable(host: String, port: Int, timeoutMs: Int = 1000): Boolean = withContext(Dispatchers.IO) {
        measureSingleTcpRttWithPermit(host, port, timeoutMs.coerceIn(300, 1500)).first > 0
    }
}
