package com.example.cloudplay

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.example.data.AppConfig
import com.example.data.CellularOptimizationMode
import com.example.data.NetworkTuningMode
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.roundToLong

enum class PhysicalNetworkType(val label: String, val icon: String) {
    WIFI_6GHZ("Wifi 6GHz (Siêu tốc Wi-Fi 6E/7)", "🛜"),
    WIFI_5GHZ("Wifi 5GHz (Tốc độ cao)", "🛜"),
    WIFI_2_4GHZ("Wifi 2.4GHz (Chống nhiễu)", "🛜"),
    WIFI_STANDARD("Wifi (Chuẩn)", "🛜"),
    CELLULAR_4G_5G("Mạng 4G/LTE Di Động", "📶"),
    ETHERNET("Mạng Dây LAN", "🌐"),
    UNKNOWN("Không rõ mạng", "📡")
}

data class NetworkOptimizerStatus(
    val physicalType: PhysicalNetworkType = PhysicalNetworkType.UNKNOWN,
    val isWifi: Boolean = false,
    val isCellular: Boolean = false,
    val isMetered: Boolean = false,
    val activeProfileName: String = "Tự Động (Theo Điều Kiện Mạng & Máy)",
    val effectiveBitrateMbps: Int = 9,
    val speedFirstPriority: Boolean = true,
    val wifiFrequencyMhz: Int = 0,
    val wifiRssiDbm: Int = -55,
    val wifiLinkSpeedMbps: Int = 0,
    val downstreamBandwidthKbps: Int = 0,
    val isWeakWifiSignal: Boolean = false,
    val isWeakCellularSignal: Boolean = false,
    val summaryText: String = "Đang tối ưu hóa đồ họa và hiệu năng thiết bị...",
    val profileBadge: String = "⚡ Tự Động"
)

data class StreamTelemetry(
    val fps: Int = 0,
    val bitrateMbps: Double = 0.0,
    val decodeTimeMs: Double = 0.0,
    val rttMs: Int = 0,
    val jitterMs: Int = 0,
    val packetsLostPercent: Double = 0.0,
    val framesDropped: Int = 0,
    val freezeCount: Int = 0,
    val totalFreezesDurationSec: Double = 0.0,
    val jitterBufferDelayMs: Double = 0.0,
    val speedPriorityActive: Boolean = true,
    val appliedProfile: String = "Tự Động",
    val estimatedDataSavedMb: Double = 0.0
)

object NetworkOptimizer {
    private const val TAG = "NetworkOptimizer"
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _status = MutableStateFlow(NetworkOptimizerStatus())
    val status: StateFlow<NetworkOptimizerStatus> = _status.asStateFlow()

    private val _telemetry = MutableStateFlow(StreamTelemetry())
    val telemetry: StateFlow<StreamTelemetry> = _telemetry.asStateFlow()

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var wifiLowLatencyLock: WifiManager.WifiLock? = null
    private var isInitialized = false

    // Telemetry tracking for estimated data savings using actual elapsed time
    private var streamSecondsTracked = 0.0
    private var totalDataSavedMegabytes = 0.0
    private var lastTelemetryTimestampMs = 0L

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        try {
            val appCtx = context.applicationContext
            connectivityManager = appCtx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val wifiMgr = appCtx.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiMgr != null) {
                @Suppress("DEPRECATION")
                val lockMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                } else {
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF
                }
                wifiLowLatencyLock = wifiMgr.createWifiLock(lockMode, "CloudPlay:UltraLowLatencyWifiLock")?.apply {
                    setReferenceCounted(false)
                }
            }
            val cm = connectivityManager ?: return

            // Initial evaluation
            evaluateCurrentNetwork(appCtx, cm.activeNetwork)

            // Register reactive network callback
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    evaluateCurrentNetwork(appCtx, network)
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    evaluateCurrentNetwork(appCtx, network)
                }

                override fun onLost(network: Network) {
                    evaluateCurrentNetwork(appCtx, null)
                }
            }

            cm.registerNetworkCallback(request, networkCallback!!)

            // Observe user configuration changes to re-tune immediately
            scope.launch {
                SettingsRepository.config.collect { config ->
                    recomputeEffectiveProfile(config)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize NetworkOptimizer", e)
        }
    }

    /**
     * Activates hardware WIFI_MODE_FULL_LOW_LATENCY lock when streaming on Wi-Fi (prevents DTIM radio sleep spikes)
     * and pauses background catalog requests on both 4G and Wi-Fi so 100% of bandwidth serves the game stream.
     */
    fun setStreamActive(active: Boolean) {
        com.example.data.GameLibraryRepository.setStreamActive(active)
        try {
            val lock = wifiLowLatencyLock
            if (lock != null) {
                if (active && _status.value.isWifi) {
                    if (!lock.isHeld) lock.acquire()
                } else {
                    if (lock.isHeld) lock.release()
                }
            }
            if (active && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val cm = connectivityManager
                val net = cm?.activeNetwork
                if (cm != null && net != null) {
                    cm.reportNetworkConnectivity(net, true)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun evaluateCurrentNetwork(context: Context, network: Network?) {
        val cm = connectivityManager ?: return
        val currentNetwork = network ?: cm.activeNetwork
        val caps = if (currentNetwork != null) cm.getNetworkCapabilities(currentNetwork) else null

        var type = PhysicalNetworkType.UNKNOWN
        var isWifi = false
        var isCellular = false
        var isMetered = false
        var wifiFreq = 0
        var wifiRssi = -55
        var wifiLinkMbps = 0
        var downstreamKbps = 0
        var isWeakWifi = false
        var isWeakCellular = false

        if (caps != null) {
            isMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            downstreamKbps = caps.linkDownstreamBandwidthKbps

            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                isWifi = true
                val (freq, rssi, linkMbps) = detectWifiTelemetry(context, caps)
                wifiFreq = freq
                wifiRssi = rssi
                wifiLinkMbps = linkMbps
                type = classifyWifiBand(wifiFreq)
                // Standing far from router / through walls: RSSI <= -70 dBm or linkSpeed drops below 65 Mbps
                isWeakWifi = (wifiRssi in -120..-70) || (wifiLinkMbps in 1..64)
            } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                isCellular = true
                type = PhysicalNetworkType.CELLULAR_4G_5G
                val sigStrength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    caps.signalStrength
                } else {
                    Int.MIN_VALUE
                }
                isWeakCellular = (downstreamKbps in 1..9000) || (sigStrength != Int.MIN_VALUE && sigStrength in -140..-104)
            } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                type = PhysicalNetworkType.ETHERNET
            }
        }

        val config = SettingsRepository.config.value
        _status.value = _status.value.copy(
            physicalType = type,
            isWifi = isWifi,
            isCellular = isCellular,
            isMetered = isMetered,
            wifiFrequencyMhz = wifiFreq,
            wifiRssiDbm = wifiRssi,
            wifiLinkSpeedMbps = wifiLinkMbps,
            downstreamBandwidthKbps = downstreamKbps,
            isWeakWifiSignal = isWeakWifi,
            isWeakCellularSignal = isWeakCellular
        )

        recomputeEffectiveProfile(config)
    }

    /**
     * Classifies Wi-Fi frequency band including 6GHz (5925–7125 MHz), 5GHz, and 2.4GHz.
     */
    fun classifyWifiBand(wifiFreqMhz: Int): PhysicalNetworkType {
        return when {
            wifiFreqMhz in 5925..7125 -> PhysicalNetworkType.WIFI_6GHZ
            wifiFreqMhz >= 4900 -> PhysicalNetworkType.WIFI_5GHZ
            wifiFreqMhz in 2400..2500 -> PhysicalNetworkType.WIFI_2_4GHZ
            else -> PhysicalNetworkType.WIFI_STANDARD
        }
    }

    @Suppress("DEPRECATION")
    private fun detectWifiTelemetry(context: Context, caps: NetworkCapabilities): Triple<Int, Int, Int> {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val wifiInfo = caps.transportInfo as? WifiInfo
                if (wifiInfo != null && wifiInfo.frequency > 0) {
                    return Triple(wifiInfo.frequency, wifiInfo.rssi, wifiInfo.linkSpeed.coerceAtLeast(0))
                }
            }
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val connectionInfo = wifiManager?.connectionInfo
            if (connectionInfo != null && connectionInfo.frequency > 0) {
                return Triple(connectionInfo.frequency, connectionInfo.rssi, connectionInfo.linkSpeed.coerceAtLeast(0))
            }
        } catch (ignored: Exception) {}
        return Triple(0, -55, 0)
    }

    fun recomputeEffectiveProfile(config: AppConfig) {
        val current = _status.value
        val isWifi = current.isWifi
        val isCellular = current.isCellular || (!isWifi && current.physicalType == PhysicalNetworkType.CELLULAR_4G_5G)

        val cellularMode = config.cellularMode

        val profileName: String
        val bitrate: Int
        val summary: String
        val badge: String

        if (config.lowEndDeviceMode) {
            profileName = "Chế Độ Máy Yếu • 720p Nhẹ (5 Mbps)"
            bitrate = 5
            summary = "Đang bật Chế độ máy yếu: Khóa 720p (5 Mbps) để giảm tải giải mã và giữ ổn định trên 4G/Wi-Fi."
            badge = "🛡️ 720p Máy Yếu (5M)"
        } else when (cellularMode) {
            CellularOptimizationMode.AUTO -> {
                val autoMbps = when {
                    isWifi && current.isWeakWifiSignal -> 7 // Đứng xa cục phát Wi-Fi 5GHz/2.4GHz: hạ xuống 7M chống rớt gói qua tường
                    isWifi && current.physicalType == PhysicalNetworkType.WIFI_2_4GHZ -> 8 // Wi-Fi 2.4GHz: giữ gói tin gọn chống nhiễu Bluetooth
                    isWifi && current.physicalType == PhysicalNetworkType.WIFI_6GHZ -> 14
                    isWifi && current.physicalType == PhysicalNetworkType.WIFI_5GHZ -> 12
                    isWifi -> 10
                    isCellular && current.isWeakCellularSignal -> 5 // Sóng 4G yếu / đang chuyển trạm
                    isCellular -> 6
                    else -> 9
                }
                profileName = when {
                    isCellular && current.isWeakCellularSignal -> "Tự Động 4G Chống Rớt Sóng (${autoMbps}M)"
                    isCellular -> "Tự Động 4G/5G (720p ⇄ 1080p • ${autoMbps}M)"
                    isWifi && current.isWeakWifiSignal -> "Tự Động Wi-Fi Xa Cục Phát (${autoMbps}M)"
                    isWifi && current.physicalType == PhysicalNetworkType.WIFI_2_4GHZ -> "Tự Động Wi-Fi 2.4GHz Chống Nhiễu (${autoMbps}M)"
                    else -> "Tự Động Wi-Fi (720p ⇄ 1080p • ${autoMbps}M)"
                }
                bitrate = autoMbps
                summary = when {
                    isCellular -> "Ưu tiên độ trễ thấp nhất, tự hạ nhẹ độ nét trong 1.5s khi sóng 4G yếu hoặc đổi trạm phát."
                    current.isWeakWifiSignal -> "Phát hiện sóng Wi-Fi yếu (${current.wifiRssiDbm} dBm): tự thu gọn luồng ${autoMbps}M để xuyên tường mượt mà."
                    else -> "Ưu tiên thao tác điều khiển nhạy nhất, tự động cân chỉnh độ nét và giải mã < 8ms."
                }
                badge = if (isCellular) "📶 Tự động 4G (${autoMbps}M)" else "⚡ Tự động (${autoMbps}M)"
            }
            CellularOptimizationMode.DATA_SAVER_HIGH_SPEED -> {
                profileName = "720p • Thấp (Cố Định • 5M)"
                bitrate = 5
                summary = "Khóa 720p • 5 Mbps giúp giảm độ trễ trên mạng 4G/5G yếu hoặc Wi-Fi xa cục phát, nhẹ máy và mát pin."
                badge = "❄️ 720p • Thấp (5M)"
            }
            CellularOptimizationMode.MEDIUM -> {
                profileName = "900p • Vừa (Cố Định • 9M)"
                bitrate = 9
                summary = "Khóa cố định 900p • 9 Mbps (~3.8GB/giờ), cân bằng giữa độ nét và độ nhạy trên 4G/5G & Wi-Fi."
                badge = "🌿 900p • Vừa (9M)"
            }
            CellularOptimizationMode.HIGH -> {
                profileName = "1080p • Cao (Cố Định • 14M)"
                bitrate = 14
                summary = "Khóa cố định 1080p Full HD • 14 Mbps sắc nét cho mạng Wi-Fi gần cục phát hoặc 5G mạnh."
                badge = "✨ 1080p • Cao (14M)"
            }
            CellularOptimizationMode.MAX_SPEED_UNCAPPED -> {
                profileName = "1080p+ • Rất cao (Cố Định • 20M)"
                bitrate = 20
                summary = "Khóa cố định tối đa 1080p+ • 20 Mbps cho mạng Wi-Fi 5GHz/6GHz gần cục phát."
                badge = "🔥 1080p+ • Rất cao (20M)"
            }
        }

        _status.value = current.copy(
            activeProfileName = profileName,
            effectiveBitrateMbps = bitrate,
            speedFirstPriority = true,
            summaryText = summary,
            profileBadge = badge
        )
        AdaptiveQualityManager.onNetworkStatusChanged(_status.value)
    }

    /**
     * Parses real-time WebRTC telemetry sent from the injected stream monitor.
     * Calculates actual elapsed time between telemetry packets (~1500ms) instead of fixed 2.0s.
     */
    fun onTelemetryReceived(telemetryJson: String?) {
        if (telemetryJson.isNullOrBlank()) return
        try {
            val json = JSONObject(telemetryJson)
            val fps = json.optInt("fps", 0)
            val bitrate = json.optDouble("bitrateMbps", 0.0)
            val decodeTime = json.optDouble("decodeTimeMs", 0.0)
            val rawRtt = json.optInt("rttMs", 0)
            val rtt = if (rawRtt > 0) {
                rawRtt
            } else {
                _telemetry.value.rttMs.takeIf { it > 0 } ?: 0
            }
            val jitter = json.optInt("jitterMs", 0)
            val loss = json.optDouble("packetsLostPercent", 0.0)
            val framesDropped = json.optInt("framesDropped", 0)
            val freezeCount = json.optInt("freezeCount", 0)
            val totalFreezesDurationSec = json.optDouble("totalFreezesDurationSec", 0.0)
            val jitterBufferDelayMs = json.optDouble("jitterBufferDelayMs", 0.0)

            val nowMs = try {
                val rt = SystemClock.elapsedRealtime()
                if (rt > 0L) rt else System.currentTimeMillis()
            } catch (_: Throwable) {
                System.currentTimeMillis()
            }
            val elapsedSeconds = if (lastTelemetryTimestampMs > 0L && nowMs > lastTelemetryTimestampMs) {
                ((nowMs - lastTelemetryTimestampMs) / 1000.0).coerceIn(0.2, 5.0)
            } else {
                1.5 // Matches XboxBridge 1500ms interval on first sample
            }
            lastTelemetryTimestampMs = nowMs

            val currentStatus = _status.value
            val isSavingData = currentStatus.effectiveBitrateMbps <= 10

            // Calculate estimated saved MB using actual elapsedSeconds compared to uncapped 15Mbps stream
            streamSecondsTracked += elapsedSeconds
            if (isSavingData && bitrate in 0.1..14.0) {
                totalDataSavedMegabytes += (15.0 - bitrate).coerceAtLeast(0.0) * (elapsedSeconds / 8.0)
            }

            val newTelemetry = StreamTelemetry(
                fps = fps,
                bitrateMbps = bitrate,
                decodeTimeMs = decodeTime,
                rttMs = rtt,
                jitterMs = jitter,
                packetsLostPercent = loss,
                framesDropped = framesDropped,
                freezeCount = freezeCount,
                totalFreezesDurationSec = totalFreezesDurationSec,
                jitterBufferDelayMs = jitterBufferDelayMs,
                speedPriorityActive = currentStatus.speedFirstPriority,
                appliedProfile = currentStatus.activeProfileName,
                estimatedDataSavedMb = totalDataSavedMegabytes
            )
            _telemetry.value = newTelemetry
            AdaptiveQualityManager.evaluateTelemetry(newTelemetry)
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing telemetry: $telemetryJson", e)
        }
    }
}

