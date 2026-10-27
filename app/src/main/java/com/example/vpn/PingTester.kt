package com.example.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object PingTester {

    /**
     * Limits concurrent outbound TCP SYN probes during "Đo ping" across servers
     * so mobile carriers (4G/H+) and home routers do not drop packets due to SYN flood protection.
     */
    private val probeSemaphore = Semaphore(8)

    /**
     * Measures reference TCP handshake latency to the currently selected server (median of 3 samples).
     * Uses the exact [timeoutMs] passed by the caller without clamping to 1400ms, and never falls back
     * to www.xbox.com, www.microsoft.com, or any other domain/server.
     */
    suspend fun measureDirectLatency(timeoutMs: Int = 3500): Int = withContext(Dispatchers.IO) {
        val currentServer = VpnManager.state.value.currentServer
        if (currentServer.ip.isBlank() || currentServer.port <= 0) return@withContext -1
        val safeTimeout = timeoutMs.coerceAtLeast(500)

        probeSemaphore.withPermit {
            val portsToTry = listOf(currentServer.port, 443, 80).distinct()
            for (port in portsToTry) {
                val samples = mutableListOf<Int>()
                repeat(3) { attempt ->
                    val rtt = measureSingleTcpRtt(currentServer.ip, port, safeTimeout)
                    if (rtt > 0) {
                        samples.add(rtt)
                    } else if (attempt == 0 && samples.isEmpty()) {
                        // First probe to this port timed out or was refused; try next port on the same IP
                        return@repeat
                    }
                }
                if (samples.isNotEmpty()) {
                    samples.sort()
                    return@withPermit samples[samples.size / 2]
                }
            }
            -1
        }
    }

    /**
     * Measures TCP handshake latency strictly to [server]'s own IP address (`server.ip:port`, `:443`, `:80`).
     * Never probes any other server in the same country, regional datacenter probe lists, or Anycast domains
     * (`www.xbox.com` / `www.microsoft.com`). If `server.ip:port`, `:443`, and `:80` all fail to respond,
     * returns -1 so the UI displays "Không đo được".
     */
    suspend fun measureServerLatency(server: ServerLocation, timeoutMs: Int = 2500): Int = withContext(Dispatchers.IO) {
        if (server.ip.isBlank() || server.port <= 0) return@withContext -1
        val safeTimeout = timeoutMs.coerceAtLeast(500)

        probeSemaphore.withPermit {
            val candidatePorts = listOf(server.port, 443, 80).distinct()
            for (port in candidatePorts) {
                val samples = mutableListOf<Int>()
                val first = measureSingleTcpRtt(server.ip, port, safeTimeout)
                if (first > 0) {
                    samples.add(first)
                    val second = measureSingleTcpRtt(server.ip, port, safeTimeout)
                    if (second > 0) samples.add(second)
                    val third = measureSingleTcpRtt(server.ip, port, safeTimeout)
                    if (third > 0) samples.add(third)
                    samples.sort()
                    return@withPermit samples[samples.size / 2]
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
    suspend fun testServerPing(server: ServerLocation, timeoutMs: Int = 2500): Int =
        measureServerLatency(server, timeoutMs)

    /**
     * Measures latency to a specific host and port (median of up to 3 samples) using the full [timeoutMs].
     */
    suspend fun measureLatency(host: String, port: Int = 443, timeoutMs: Int = 2500): Int = withContext(Dispatchers.IO) {
        if (host.isBlank()) return@withContext -1
        val safeTimeout = timeoutMs.coerceAtLeast(500)
        val samples = mutableListOf<Int>()
        repeat(3) { attempt ->
            val rtt = measureSingleTcpRtt(host, port, safeTimeout)
            if (rtt > 0) {
                samples.add(rtt)
            } else if (attempt == 0 && samples.isEmpty()) {
                return@withContext -1
            }
        }
        if (samples.isEmpty()) -1 else {
            samples.sort()
            samples[samples.size / 2]
        }
    }

    /**
     * Quick test if an IP and port responds.
     */
    suspend fun isHostReachable(host: String, port: Int, timeoutMs: Int = 1500): Boolean = withContext(Dispatchers.IO) {
        measureSingleTcpRtt(host, port, timeoutMs.coerceAtLeast(500)) > 0
    }
}
