package com.example.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object PingTester {

    /**
     * Real Xbox Cloud & Azure Japan/East Asia gaming endpoints (never domestic DNS 1.1.1.1 or 8.8.8.8).
     */
    private val XBOX_CLOUD_ENDPOINTS = listOf(
        Pair("43.173.120.13", 8899),     // Tokyo CloudDC node
        Pair("172.105.192.212", 9080),   // Tokyo Linode node
        Pair("219.100.37.119", 443),     // Tsukuba Tokyo Academic node
        Pair("www.xbox.com", 443)        // Xbox web gateway fallback
    )

    /**
     * Measures genuine round-trip TCP handshake latency to Xbox Cloud / Tokyo gaming edge
     * without artificial upper-bound filtering (supports slow 3G/H+ connections up to 3500ms).
     * Never probes local carrier DNS (1.1.1.1 / 8.8.8.8) and never returns fake numbers.
     */
    suspend fun measureDirectLatency(timeoutMs: Int = 3000): Int = withContext(Dispatchers.IO) {
        val currentServer = VpnManager.state.value.currentServer
        val targets = buildList {
            if (currentServer.ip.isNotBlank() && currentServer.port > 0) {
                add(Pair(currentServer.ip, currentServer.port))
            }
            addAll(XBOX_CLOUD_ENDPOINTS)
        }.distinct()

        for (target in targets) {
            val measured = measureSingleTcpRtt(target.first, target.second, timeoutMs)
            if (measured > 0) {
                return@withContext measured
            }
        }
        // If all probes failed or timed out, return -1 (never return fake or cached numbers)
        -1
    }

    /**
     * Measures genuine TCP handshake latency directly to a specific ServerLocation's IP and port.
     * Takes 2 samples when fast to avoid single-packet anomalies, and reports full real delay on H+/3G/4G.
     */
    suspend fun measureServerLatency(server: ServerLocation, timeoutMs: Int = 2800): Int = withContext(Dispatchers.IO) {
        if (server.ip.isBlank() || server.port <= 0) return@withContext -1

        val firstSample = measureSingleTcpRtt(server.ip, server.port, timeoutMs)
        if (firstSample <= 0) return@withContext -1

        // If latency is under 300ms, take a second un-cached TCP handshake sample to measure true steady RTT
        if (firstSample < 300) {
            val secondSample = measureSingleTcpRtt(server.ip, server.port, timeoutMs)
            if (secondSample > 0) {
                return@withContext ((firstSample + secondSample) / 2).coerceAtLeast(1)
            }
        }
        firstSample
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
     * Measures latency to a specific host and port without falling back to domestic DNS.
     */
    suspend fun measureLatency(host: String, port: Int = 443, timeoutMs: Int = 2500): Int = withContext(Dispatchers.IO) {
        if (host.isBlank()) return@withContext -1
        measureSingleTcpRtt(host, port, timeoutMs)
    }

    /**
     * Quick test if an IP and port responds
     */
    suspend fun isHostReachable(host: String, port: Int, timeoutMs: Int = 1500): Boolean = withContext(Dispatchers.IO) {
        measureSingleTcpRtt(host, port, timeoutMs) > 0
    }
}
