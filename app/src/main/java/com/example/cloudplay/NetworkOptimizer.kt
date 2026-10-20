package com.example.cloudplay

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
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

enum class PhysicalNetworkType(val label: String, val icon: String) {
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
    val activeProfileName: String = "4G Tiết Kiệm Data (Tốc Độ Cao)",
    val effectiveBitrateMbps: Int = 8,
    val speedFirstPriority: Boolean = true,
    val wifiFrequencyMhz: Int = 0,
    val summaryText: String = "Đang tối ưu hóa kết nối...",
    val profileBadge: String = "⚡ Tốc độ #1"
)

data class StreamTelemetry(
    val fps: Int = 0,
    val bitrateMbps: Double = 0.0,
    val decodeTimeMs: Double = 0.0,
    val rttMs: Int = 0,
    val jitterMs: Int = 0,
    val packetsLostPercent: Double = 0.0,
    val framesDropped: Int = 0,
    val speedPriorityActive: Boolean = true,
    val appliedProfile: String = "4G Tiết Kiệm",
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
    private var isInitialized = false

    // Telemetry tracking for estimated data savings
    private var streamSecondsTracked = 0L
    private var totalDataSavedMegabytes = 0.0

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        try {
            connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val cm = connectivityManager ?: return

            // Initial evaluation
            evaluateCurrentNetwork(context, cm.activeNetwork)

            // Register reactive network callback
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    evaluateCurrentNetwork(context, network)
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    evaluateCurrentNetwork(context, network)
                }

                override fun onLost(network: Network) {
                    evaluateCurrentNetwork(context, null)
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

    private fun evaluateCurrentNetwork(context: Context, network: Network?) {
        val cm = connectivityManager ?: return
        val currentNetwork = network ?: cm.activeNetwork
        val caps = if (currentNetwork != null) cm.getNetworkCapabilities(currentNetwork) else null

        var type = PhysicalNetworkType.UNKNOWN
        var isWifi = false
        var isCellular = false
        var isMetered = false
        var wifiFreq = 0

        if (caps != null) {
            isMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                isWifi = true
                wifiFreq = detectWifiFrequency(context, caps)
                type = when {
                    wifiFreq >= 4900 -> PhysicalNetworkType.WIFI_5GHZ
                    wifiFreq in 2400..2500 -> PhysicalNetworkType.WIFI_2_4GHZ
                    else -> PhysicalNetworkType.WIFI_STANDARD
                }
            } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                isCellular = true
                type = PhysicalNetworkType.CELLULAR_4G_5G
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
            wifiFrequencyMhz = wifiFreq
        )

        recomputeEffectiveProfile(config)
    }

    @Suppress("DEPRECATION")
    private fun detectWifiFrequency(context: Context, caps: NetworkCapabilities): Int {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val wifiInfo = caps.transportInfo as? WifiInfo
                if (wifiInfo != null && wifiInfo.frequency > 0) {
                    return wifiInfo.frequency
                }
            }
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val connectionInfo = wifiManager?.connectionInfo
            if (connectionInfo != null && connectionInfo.frequency > 0) {
                return connectionInfo.frequency
            }
        } catch (ignored: Exception) {}
        return 0
    }

    fun recomputeEffectiveProfile(config: AppConfig) {
        val current = _status.value
        val isWifi = current.isWifi
        val isCellular = current.isCellular || (!isWifi && current.physicalType == PhysicalNetworkType.CELLULAR_4G_5G)

        val tuningMode = config.networkTuningMode
        val cellularMode = config.cellularMode
        val speedFirst = config.wifiSpeedFirstPriority

        val profileName: String
        val bitrate: Int
        val summary: String
        val badge: String

        when {
            // Explicit Wi-Fi mode OR Auto on Wi-Fi
            tuningMode == NetworkTuningMode.WIFI_SPEED_FIRST || (tuningMode == NetworkTuningMode.AUTO && isWifi) -> {
                val freqLabel = if (current.physicalType == PhysicalNetworkType.WIFI_5GHZ) "5GHz" else if (current.physicalType == PhysicalNetworkType.WIFI_2_4GHZ) "2.4GHz" else "Wifi"
                profileName = "Wifi Tự Động Tinh Chỉnh ($freqLabel)"
                bitrate = if (current.physicalType == PhysicalNetworkType.WIFI_5GHZ) 16 else 12
                summary = "Ưu tiên Tốc độ & Độ nhạy #1 (Đồ hoạ #2). Tự động giảm trễ đệm khi ping tăng."
                badge = "🛜 Wifi Speed #1"
            }
            // Explicit 4G Data Saver OR Auto on Cellular with Data Saver
            tuningMode == NetworkTuningMode.CELLULAR_DATA_SAVER ||
            (tuningMode == NetworkTuningMode.AUTO && isCellular && cellularMode == CellularOptimizationMode.DATA_SAVER_HIGH_SPEED) ||
            (!isWifi && cellularMode == CellularOptimizationMode.DATA_SAVER_HIGH_SPEED) -> {
                profileName = "4G Tiết Kiệm Data (Tốc Độ Cao)"
                bitrate = 8
                summary = "Tiết kiệm 50% data di động (~3.2GB/h). Giữ nguyên 60 FPS, độ trễ đệm 0ms, chống nóng chip."
                badge = "📶 4G Tiết Kiệm (8M)"
            }
            // 4G Max Speed ("Có nhiêu sài hết")
            else -> {
                profileName = "4G Có Nhiêu Sài Hết (Max Speed)"
                bitrate = 20
                summary = "Bung trọn vẹn 20 Mbps băng thông 4G/5G. Khung hình mượt mà tuyệt đối, 0ms input lag."
                badge = "🚀 4G Max (20M)"
            }
        }

        _status.value = current.copy(
            activeProfileName = profileName,
            effectiveBitrateMbps = bitrate,
            speedFirstPriority = speedFirst,
            summaryText = summary,
            profileBadge = badge
        )
        AdaptiveQualityManager.onNetworkStatusChanged(_status.value)
    }

    /**
     * Parses real-time WebRTC telemetry sent from the injected stream monitor.
     */
    fun onTelemetryReceived(telemetryJson: String?) {
        if (telemetryJson.isNullOrBlank()) return
        try {
            val json = JSONObject(telemetryJson)
            val fps = json.optInt("fps", 0)
            val bitrate = json.optDouble("bitrateMbps", 0.0)
            val decodeTime = json.optDouble("decodeTimeMs", 0.0)
            val rtt = json.optInt("rttMs", 0)
            val jitter = json.optInt("jitterMs", 0)
            val loss = json.optDouble("packetsLostPercent", 0.0)
            val framesDropped = json.optInt("framesDropped", 0)

            val currentStatus = _status.value
            val isDataSaver = currentStatus.effectiveBitrateMbps <= 8

            // Calculate estimated saved MB (standard Xbox stream consumes 15-20Mbps, Data Saver uses ~8Mbps -> saves ~1.2MB every second)
            streamSecondsTracked += 2
            if (isDataSaver && bitrate in 0.1..14.0) {
                totalDataSavedMegabytes += (14.0 - bitrate).coerceAtLeast(0.0) * (2.0 / 8.0) // (Mbps saved) * seconds / 8 bits
            }

            val newTelemetry = StreamTelemetry(
                fps = fps,
                bitrateMbps = bitrate,
                decodeTimeMs = decodeTime,
                rttMs = rtt,
                jitterMs = jitter,
                packetsLostPercent = loss,
                framesDropped = framesDropped,
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
