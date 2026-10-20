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
    private var isInitialized = false

    // Telemetry tracking for estimated data savings using actual elapsed time
    private var streamSecondsTracked = 0.0
    private var totalDataSavedMegabytes = 0.0
    private var lastTelemetryTimestampMs = 0L

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
                type = classifyWifiBand(wifiFreq)
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

        val cellularMode = config.cellularMode
        val speedFirst = config.wifiSpeedFirstPriority

        val profileName: String
        val bitrate: Int
        val summary: String
        val badge: String

        when (cellularMode) {
            CellularOptimizationMode.AUTO -> {
                when {
                    isWifi && current.physicalType == PhysicalNetworkType.WIFI_6GHZ -> {
                        profileName = "Đồ Họa: Tự Động • Wi-Fi 6GHz (Rất Cao 18M)"
                        bitrate = 18
                        summary = "Mạng Wi-Fi 6GHz siêu tốc: Tự chọn mức Rất Cao (18M) hỗ trợ 1080p 60–120FPS độ trễ cực thấp."
                        badge = "⚡ Tự Động (18M)"
                    }
                    isWifi && current.physicalType == PhysicalNetworkType.WIFI_5GHZ -> {
                        profileName = "Đồ Họa: Tự Động • Wi-Fi 5GHz (Cao 14M)"
                        bitrate = 14
                        summary = "Mạng Wi-Fi 5GHz mạnh: Tự chọn mức Cao (14M) sắc nét và tự hạ tải GPU nếu máy nóng."
                        badge = "⚡ Tự Động (14M)"
                    }
                    isWifi -> {
                        profileName = "Đồ Họa: Tự Động • Wi-Fi (Trung Bình 9M)"
                        bitrate = 9
                        summary = "Mạng Wi-Fi chuẩn: Tự cân bằng mức Trung Bình (9M) giữ 60 FPS êm ái, chống giật lag."
                        badge = "⚡ Tự Động (9M)"
                    }
                    isCellular -> {
                        profileName = "Đồ Họa: Tự Động • Mạng Di Động (Tiết Kiệm 6M)"
                        bitrate = 6
                        summary = "Mạng 4G/5G di động: Tự ưu tiên tiết kiệm dung lượng & giảm tải giải mã GPU chống nóng máy."
                        badge = "⚡ Tự Động (6M)"
                    }
                    else -> {
                        profileName = "Đồ Họa: Tự Động (Cân Bằng 10M)"
                        bitrate = 10
                        summary = "Tự động điều chỉnh chất lượng đồ họa và giảm tải phần cứng theo điều kiện mạng thực tế."
                        badge = "⚡ Tự Động (10M)"
                    }
                }
            }
            CellularOptimizationMode.DATA_SAVER_HIGH_SPEED -> {
                profileName = "Đồ Họa: Thấp (Tiết Kiệm Dung Lượng • 5M)"
                bitrate = 5
                summary = "Giảm tải tối đa cho CPU/GPU điện thoại (<4ms giải mã), tiết kiệm 60% dung lượng (~2.2GB/h), siêu mát máy."
                badge = "❄️ Tiết Kiệm (5M)"
            }
            CellularOptimizationMode.MEDIUM -> {
                profileName = "Đồ Họa: Trung Bình (Cân Bằng • 9M)"
                bitrate = 9
                summary = "Cân bằng độ nét và nhiệt độ điện thoại (~3.8GB/h). Giữ 60 FPS mượt mà, ít hao pin khi chơi lâu."
                badge = "🌿 Trung Bình (9M)"
            }
            CellularOptimizationMode.HIGH -> {
                profileName = "Đồ Họa: Cao (Sắc Nét 1080p • 14M)"
                bitrate = 14
                summary = "Hình ảnh Full HD 1080p60 sắc nét (~5.5GB/h), chi tiết cao cho mạng ổn định và máy tầm trung trở lên."
                badge = "✨ Cao (14M)"
            }
            CellularOptimizationMode.MAX_SPEED_UNCAPPED -> {
                profileName = "Đồ Họa: Rất Cao (Tối Đa • 20M)"
                bitrate = 20
                summary = "Bung tối đa 20 Mbps chất lượng đồ họa xCloud (~7.5GB/h), hỗ trợ 120 FPS & phản hồi tay cầm tức thì."
                badge = "🔥 Rất Cao (20M)"
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
     * Calculates actual elapsed time between telemetry packets (~1500ms) instead of fixed 2.0s.
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

