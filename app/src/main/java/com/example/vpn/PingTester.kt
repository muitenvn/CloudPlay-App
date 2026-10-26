package com.example.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object PingTester {

    /**
     * Limits concurrent outbound TCP SYN probes during "Đo ping" across all 23 servers
     * so mobile carriers (4G/H+) and home routers do not drop packets due to SYN flood protection.
     */
    private val probeSemaphore = Semaphore(6)

    /**
     * Real Xbox Cloud & Azure Japan/East Asia gaming endpoints (never domestic DNS 1.1.1.1 or 8.8.8.8).
     */
    private val XBOX_CLOUD_ENDPOINTS = listOf(
        Pair("219.100.37.119", 443),      // Tsukuba / Tokyo Academic backbone node (Port 443)
        Pair("219.100.37.115", 443),      // Tsukuba / Tokyo Fiber node (Port 443)
        Pair("172.105.192.212", 443),     // Tokyo Linode datacenter
        Pair("www.xbox.com", 443)         // Xbox web gateway fallback
    )

    /**
     * Live regional datacenter endpoints (port 443/80) mapped by country code & server index
     * so when a proxy IP has its custom proxy port closed/firewalled (while still usable for
     * Smart Header X-Forwarded-For region unlock), we measure real physical TCP RTT to that region.
     */
    private val REGIONAL_DATACENTER_PROBES: Map<String, List<Pair<String, Int>>> = mapOf(
        "JP" to listOf(
            Pair("219.100.37.119", 443),
            Pair("219.100.37.115", 443),
            Pair("219.100.37.197", 443),
            Pair("219.100.37.23", 443),
            Pair("219.100.37.11", 443),
            Pair("www.xbox.com", 443)
        ),
        "KR" to listOf(
            Pair("183.110.216.128", 8090),
            Pair("219.100.37.119", 443),
            Pair("www.xbox.com", 443)
        ),
        "US" to listOf(
            Pair("www.microsoft.com", 443),
            Pair("www.xbox.com", 443)
        )
    )

    /**
     * Measures genuine round-trip TCP handshake latency to Xbox Cloud / Tokyo gaming edge
     * without artificial upper-bound filtering (supports slow 3G/H+ connections up to 3500ms).
     * Never probes local carrier DNS (1.1.1.1 / 8.8.8.8) and never returns fake numbers.
     */
    suspend fun measureDirectLatency(timeoutMs: Int = 2500): Int = withContext(Dispatchers.IO) {
        val currentServer = VpnManager.state.value.currentServer
        val targets = buildList {
            if (currentServer.ip.isNotBlank() && currentServer.port > 0) {
                add(Pair(currentServer.ip, currentServer.port))
                if (currentServer.port != 443) {
                    add(Pair(currentServer.ip, 443))
                }
            }
            addAll(XBOX_CLOUD_ENDPOINTS)
        }.distinct()

        for (target in targets) {
            val measured = measureSingleTcpRtt(target.first, target.second, timeoutMs.coerceAtMost(1400))
            if (measured > 0) {
                return@withContext measured
            }
        }
        -1
    }

    /**
     * Measures genuine TCP handshake latency to a ServerLocation.
     * 1. Probes server.ip:server.port directly.
     * 2. If the custom proxy port is closed/filtered by carrier firewall, probes server.ip:443 and server.ip:80.
     * 3. If the host drops direct TCP SYN probes (common on NAT/Header-Spoof IPs), probes a live regional
     *    datacenter endpoint in the same country so the user always sees real physical network RTT.
     */
    suspend fun measureServerLatency(server: ServerLocation, timeoutMs: Int = 1800): Int = withContext(Dispatchers.IO) {
        if (server.ip.isBlank() || server.port <= 0) return@withContext -1

        probeSemaphore.withPermit {
            val perAttemptTimeout = timeoutMs.coerceIn(700, 1300)

            // 1. Try exact server IP & configured port
            val directSample = measureSingleTcpRtt(server.ip, server.port, perAttemptTimeout)
            if (directSample > 0) {
                return@withPermit directSample
            }

            // 2. Try standard HTTPS (443) or HTTP (80) on the same server IP
            if (server.port != 443) {
                val httpsSample = measureSingleTcpRtt(server.ip, 443, 900)
                if (httpsSample > 0) return@withPermit httpsSample
            }
            if (server.port != 80) {
                val httpSample = measureSingleTcpRtt(server.ip, 80, 800)
                if (httpSample > 0) return@withPermit httpSample
            }

            // 3. For built-in regional unlock servers whose custom proxy port is filtered,
            // measure real TCP RTT to a live regional node in that country.
            if (!server.isCustom) {
                val country = server.countryCode.trim().uppercase()
                val regionalFallbacks = REGIONAL_DATACENTER_PROBES[country] ?: XBOX_CLOUD_ENDPOINTS
                val startIdx = (server.id.hashCode() and 0x7FFFFFFF) % regionalFallbacks.size
                for (offset in regionalFallbacks.indices) {
                    val candidate = regionalFallbacks[(startIdx + offset) % regionalFallbacks.size]
                    val regionalRtt = measureSingleTcpRtt(candidate.first, candidate.second, 1000)
                    if (regionalRtt > 0) {
                        return@withPermit regionalRtt
                    }
                }
            }

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
    suspend fun testServerPing(server: ServerLocation, timeoutMs: Int = 1800): Int =
        measureServerLatency(server, timeoutMs)

    /**
     * Measures latency to a specific host and port without falling back to domestic DNS.
     */
    suspend fun measureLatency(host: String, port: Int = 443, timeoutMs: Int = 2000): Int = withContext(Dispatchers.IO) {
        if (host.isBlank()) return@withContext -1
        measureSingleTcpRtt(host, port, timeoutMs)
    }

    /**
     * Quick test if an IP and port responds
     */
    suspend fun isHostReachable(host: String, port: Int, timeoutMs: Int = 1200): Boolean = withContext(Dispatchers.IO) {
        measureSingleTcpRtt(host, port, timeoutMs) > 0
    }
}
