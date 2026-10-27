package com.example.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object PingTester {

    /**
     * Limits concurrent local outbound TCP SYN probes during direct-device fallback to 3
     * so mobile carriers (4G/H+), home routers, and OS socket queues never add artificial queuing latency (~100ms).
     */
    private val probeSemaphore = Semaphore(3)

    /**
     * Measures reference TCP handshake latency to the currently selected server (median of up to 3 samples).
     * Exits the current port immediately if the first probe fails (avoiding 3x timeout on dead ports),
     * uses a short ~1000ms timeout for fallback ports (:443/:80), and never falls back to www.xbox.com
     * or any other domain/server.
     */
    suspend fun measureDirectLatency(timeoutMs: Int = 3500): Int = withContext(Dispatchers.IO) {
        val currentServer = VpnManager.state.value.currentServer
        if (currentServer.ip.isBlank() || currentServer.port <= 0) return@withContext -1
        val safeTimeout = timeoutMs.coerceAtLeast(500)
        val fallbackTimeout = safeTimeout.coerceAtMost(1000)

        probeSemaphore.withPermit {
            val portsToTry = listOf(currentServer.port, 443, 80).distinct()
            for (port in portsToTry) {
                val portTimeout = if (port == currentServer.port) safeTimeout else fallbackTimeout
                val first = measureSingleTcpRtt(currentServer.ip, port, portTimeout)
                if (first <= 0) {
                    // Exit this dead/unreachable port immediately without repeating 2 more times
                    continue
                }
                val samples = mutableListOf(first)
                val second = measureSingleTcpRtt(currentServer.ip, port, portTimeout)
                if (second > 0) samples.add(second)
                val third = measureSingleTcpRtt(currentServer.ip, port, portTimeout)
                if (third > 0) samples.add(third)
                samples.sort()
                return@withPermit samples[samples.size / 2]
            }
            -1
        }
    }

    /**
     * Measures server latency for the Server tab (Mục máy chủ):
     * 1. Queries the Nearest Regional Test Probe (e.g. Ho Chi Minh City or Hanoi in Vietnam, or closest global probe)
     *    for raw probe-to-server latency.
     * 2. If the primary probe is busy, RegionalProbeTester silently tries the next neighboring probe.
     * 3. If all remote probes are unavailable/unreachable, silently falls back to direct TCP measurement on the device
     *    strictly to [server]'s own IP address (`server.ip:port`, `:443`, `:80`) with a 3-permit concurrency limit.
     * Never used for in-game streaming ping (which uses WebRTC RTT exclusively).
     */
    suspend fun measureServerLatency(server: ServerLocation, timeoutMs: Int = 2500): Int = withContext(Dispatchers.IO) {
        if (server.ip.isBlank() || server.port <= 0) return@withContext -1

        // Bước 1 & 2: Đo qua Trạm Test gần nhất (kèm dự phòng ngầm sang trạm lân cận)
        val probeMeasured = RegionalProbeTester.measureServerViaNearestProbe(server, timeoutMs)
        if (probeMeasured != null) {
            return@withContext if (probeMeasured > 0) probeMeasured else -1
        }

        // Bước 3: Dự phòng đo trực tiếp từ thiết bị (chỉ chạy tối đa 3 luồng đồng thời để tránh nghẽn 100ms)
        val safeTimeout = timeoutMs.coerceAtLeast(500)
        val fallbackTimeout = safeTimeout.coerceAtMost(1000)

        probeSemaphore.withPermit {
            val candidatePorts = listOf(server.port, 443, 80).distinct()
            for (port in candidatePorts) {
                val portTimeout = if (port == server.port) safeTimeout else fallbackTimeout
                val first = measureSingleTcpRtt(server.ip, port, portTimeout)
                if (first <= 0) {
                    continue
                }
                val samples = mutableListOf(first)
                val second = measureSingleTcpRtt(server.ip, port, portTimeout)
                if (second > 0) samples.add(second)
                val third = measureSingleTcpRtt(server.ip, port, portTimeout)
                if (third > 0) samples.add(third)
                samples.sort()
                val median = samples[samples.size / 2]
                com.example.data.AppLogger.info(
                    "PING",
                    "Đo trực tiếp từ máy -> ${server.name} (${server.ip}:$port): ${median}ms"
                )
                return@withPermit median
            }
            com.example.data.AppLogger.warn(
                "PING",
                "Máy chủ không phản hồi: ${server.name} (${server.ip}:${server.port})"
            )
            -1
        }
    }

    private fun measureSingleTcpRtt(host: String, port: Int, timeoutMs: Int): Int {
        val startNano = System.nanoTime()
        return try {
            Socket().use { socket ->
                socket.tcpNoDelay = true
                socket.soTimeout = timeoutMs
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                val durationMs = ((System.nanoTime() - startNano) / 1_000_000.0).toInt()
                if (durationMs in 1..timeoutMs) durationMs else -1
            }
        } catch (_: Exception) {
            -1
        }
    }

    /**
     * Measures latency specifically to a ServerLocation's IP and port.
     */
    suspend fun testServerPing(server: ServerLocation, timeoutMs: Int = 2500): Int =
        measureServerLatency(server, timeoutMs)

    /**
     * Measures latency to a specific host and port (median of up to 3 samples) using the full [timeoutMs].
     */
    suspend fun measureLatency(host: String, port: Int = 443, timeoutMs: Int = 2500): Int = withContext(Dispatchers.IO) {
        if (host.isBlank()) return@withContext -1
        val safeTimeout = timeoutMs.coerceAtLeast(500)
        val first = measureSingleTcpRtt(host, port, safeTimeout)
        if (first <= 0) return@withContext -1
        val samples = mutableListOf(first)
        val second = measureSingleTcpRtt(host, port, safeTimeout)
        if (second > 0) samples.add(second)
        val third = measureSingleTcpRtt(host, port, safeTimeout)
        if (third > 0) samples.add(third)
        samples.sort()
        samples[samples.size / 2]
    }

    /**
     * Quick test if an IP and port responds.
     */
    suspend fun isHostReachable(host: String, port: Int, timeoutMs: Int = 1500): Boolean = withContext(Dispatchers.IO) {
        measureSingleTcpRtt(host, port, timeoutMs.coerceAtLeast(500)) > 0
    }
}
