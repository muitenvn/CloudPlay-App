package com.example.cloudplay

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

enum class QualityTier(val title: String, val badge: String) {
    ULTRA("Ultra 1080p", "💎 Ultra"),
    HIGH("Chất Lượng Cao", "✨ High"),
    BALANCED("Cân Bằng Mượt", "⚡ Cân Bằng"),
    SAVER("Tiết Kiệm 4G", "📶 Tiết Kiệm"),
    EMERGENCY("Chống Giật Lag", "🛡️ Chống Giật")
}

data class BitrateLadderStep(
    val tier: QualityTier,
    val targetBitrateMbps: Double,
    val bufferHintMs: Int,
    val title: String,
    val description: String
)

data class AdaptiveQualityState(
    val isAutoAdapting: Boolean = true,
    val currentStep: BitrateLadderStep = DEFAULT_STEP,
    val currentStepIndex: Int = 1,
    val ladderSteps: List<BitrateLadderStep> = listOf(DEFAULT_STEP),
    val qualityGrade: String = "Sẵn sàng",
    val qualityNotice: String = "Hệ thống tự động sẵn sàng tối ưu luồng theo mạng",
    val lastDegradeReason: String? = null,
    val lastUpgradeReason: String? = null
)

private val DEFAULT_STEP = BitrateLadderStep(
    tier = QualityTier.BALANCED,
    targetBitrateMbps = 10.0,
    bufferHintMs = 18,
    title = "Cân Bằng (10 Mbps)",
    description = "Mức tiêu chuẩn cho trải nghiệm mượt mà 60 FPS"
)

object AdaptiveQualityManager {
    private const val TAG = "AdaptiveQualityManager"
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _state = MutableStateFlow(AdaptiveQualityState())
    val state: StateFlow<AdaptiveQualityState> = _state.asStateFlow()

    private var badSampleStreak = 0
    private var goodSampleStreak = 0

    // Wi-Fi 5GHz Ladder
    val WIFI_5GHZ_LADDER = listOf(
        BitrateLadderStep(QualityTier.EMERGENCY, 8.0, 24, "Wifi 5G An Toàn (8M)", "Hạ tải tức thì, chống nghẽn bộ giải mã"),
        BitrateLadderStep(QualityTier.BALANCED, 12.0, 20, "Wifi 5G Mượt (12M)", "Cân bằng tốc độ cao và độ trễ thấp"),
        BitrateLadderStep(QualityTier.HIGH, 16.0, 18, "Wifi 5G Sắc Nét (16M)", "Hình ảnh 1080p60 sắc nét vượt trội"),
        BitrateLadderStep(QualityTier.ULTRA, 20.0, 16, "Wifi 5G Ultra (20M)", "Bung toàn bộ băng thông Wi-Fi 5GHz cực đỉnh")
    )

    // Wi-Fi 2.4GHz Ladder (Higher interference tolerance)
    val WIFI_24GHZ_LADDER = listOf(
        BitrateLadderStep(QualityTier.EMERGENCY, 6.0, 26, "Wifi 2.4G An Toàn (6M)", "Chống nghẽn sóng và nhiễu xuyên tường"),
        BitrateLadderStep(QualityTier.BALANCED, 9.0, 22, "Wifi 2.4G Cân Bằng (9M)", "Độ ổn định tối đa cho băng tần 2.4GHz"),
        BitrateLadderStep(QualityTier.HIGH, 12.0, 18, "Wifi 2.4G Tối Ưu (12M)", "Đỉnh băng thông 2.4GHz mượt 60 FPS")
    )

    // 4G Data Saver Ladder
    val CELLULAR_SAVER_LADDER = listOf(
        BitrateLadderStep(QualityTier.EMERGENCY, 4.5, 26, "4G Tiết Kiệm Sóng Yếu (4.5M)", "Giữ trận đấu thông suốt khi 4G yếu"),
        BitrateLadderStep(QualityTier.SAVER, 6.0, 22, "4G Tiết Kiệm Vừa (6M)", "Tiết kiệm 60% data, chống nóng GPU"),
        BitrateLadderStep(QualityTier.HIGH, 8.0, 18, "4G Tiết Kiệm Chuẩn (8M)", "Tiết kiệm 50% data, 60 FPS mượt mà")
    )

    // 4G Max Speed Ladder
    val CELLULAR_MAX_LADDER = listOf(
        BitrateLadderStep(QualityTier.EMERGENCY, 6.0, 26, "4G Ổn Định (6M)", "Chống rớt khung khi di chuyển sóng yếu"),
        BitrateLadderStep(QualityTier.BALANCED, 10.0, 22, "4G Cân Bằng (10M)", "Cân bằng tải khi trạm 4G đông đúc"),
        BitrateLadderStep(QualityTier.HIGH, 14.0, 18, "4G Tốc Độ Cao (14M)", "Khung hình mượt mà, độ nét cao"),
        BitrateLadderStep(QualityTier.ULTRA, 20.0, 16, "4G Tối Đa (20M)", "Bung trọn vẹn 20 Mbps 4G/5G max speed")
    )

    init {
        // Observe config changes
        scope.launch {
            SettingsRepository.config.collect { config ->
                reconfigureLadder(NetworkOptimizer.status.value, config)
            }
        }
    }

    fun onNetworkStatusChanged(status: NetworkOptimizerStatus) {
        val config = SettingsRepository.config.value
        reconfigureLadder(status, config)
    }

    private fun reconfigureLadder(status: NetworkOptimizerStatus, config: AppConfig) {
        val isWifi = status.isWifi
        val isCellular = status.isCellular || (!isWifi && status.physicalType == PhysicalNetworkType.CELLULAR_4G_5G)
        val tuningMode = config.networkTuningMode
        val cellularMode = config.cellularMode

        val ladder: List<BitrateLadderStep> = when {
            tuningMode == NetworkTuningMode.WIFI_SPEED_FIRST || (tuningMode == NetworkTuningMode.AUTO && isWifi) -> {
                if (status.physicalType == PhysicalNetworkType.WIFI_5GHZ) WIFI_5GHZ_LADDER else WIFI_24GHZ_LADDER
            }
            tuningMode == NetworkTuningMode.CELLULAR_DATA_SAVER ||
            (tuningMode == NetworkTuningMode.AUTO && isCellular && cellularMode == CellularOptimizationMode.DATA_SAVER_HIGH_SPEED) ||
            (!isWifi && cellularMode == CellularOptimizationMode.DATA_SAVER_HIGH_SPEED) -> {
                CELLULAR_SAVER_LADDER
            }
            else -> {
                CELLULAR_MAX_LADDER
            }
        }

        val currentIdx = _state.value.currentStepIndex.coerceIn(0, ladder.size - 1)
        val defaultIdx = if (ladder.size > 2) ladder.size - 2 else ladder.size - 1
        val selectedIdx = if (_state.value.currentStepIndex in ladder.indices) currentIdx else defaultIdx
        val activeStep = ladder[selectedIdx]

        _state.value = _state.value.copy(
            ladderSteps = ladder,
            currentStep = activeStep,
            currentStepIndex = selectedIdx,
            qualityGrade = "Đang tối ưu",
            qualityNotice = "${status.physicalType.icon} Thang chất lượng: ${activeStep.title} (${activeStep.targetBitrateMbps} Mbps)"
        )

        QualityAdapter.applyAdaptiveBitrate(activeStep.targetBitrateMbps, activeStep.bufferHintMs)
    }

    /**
     * Evaluates real-time WebRTC telemetry (decode time, frame drops, jitter, RTT, bitrate)
     * and auto-scales the bitrate ladder to prevent lag and frame drops.
     */
    fun evaluateTelemetry(telemetry: StreamTelemetry) {
        val current = _state.value
        val ladder = current.ladderSteps
        if (ladder.isEmpty()) return

        val decodeTime = telemetry.decodeTimeMs
        val loss = telemetry.packetsLostPercent
        val rtt = telemetry.rttMs
        val jitter = telemetry.jitterMs
        val fps = telemetry.fps

        // Compute human-readable quality grade
        val grade = when {
            loss > 3.0 || decodeTime > 16.0 -> "Bất Ổn"
            loss > 1.2 || rtt > 120 || decodeTime > 12.0 -> "Cần Cải Thiện"
            decodeTime in 0.1..8.0 && loss <= 0.3 && rtt in 1..65 -> "Xuất Sắc"
            else -> "Tốt (Mượt mà)"
        }

        // Automatic Ladder Stepping
        if (current.isAutoAdapting) {
            // Check for degradation trigger:
            // 1. Hardware decode lagging (taking > 15ms per frame on 60fps video)
            // 2. Network packet loss > 2.0%
            // 3. Severe RTT spike (> 130ms) or Jitter spike (> 20ms)
            val isDegradingCondition = decodeTime > 15.0 || loss > 2.0 || rtt > 130 || jitter > 22 || (fps in 1..35 && loss > 1.0)

            if (isDegradingCondition) {
                badSampleStreak++
                goodSampleStreak = 0

                if (badSampleStreak >= 2 && current.currentStepIndex > 0) {
                    val nextIdx = current.currentStepIndex - 1
                    val newStep = ladder[nextIdx]
                    val reason = when {
                        decodeTime > 15.0 -> "Giải mã GPU chậm (${String.format("%.1f", decodeTime)}ms): Tự hạ bitrate để giải phóng GPU"
                        loss > 2.0 -> "Mất gói mạng (${loss}%): Tự giảm tải băng thông"
                        rtt > 130 -> "Ping tăng vọt (${rtt}ms): Tự hạ bậc để ưu tiên độ trễ #1"
                        else -> "Đường truyền biến động: Tự cân bằng độ mượt"
                    }

                    badSampleStreak = 0
                    Log.d(TAG, "Adaptive Quality: Stepping DOWN to ${newStep.targetBitrateMbps}Mbps - Reason: $reason")

                    _state.value = current.copy(
                        currentStepIndex = nextIdx,
                        currentStep = newStep,
                        qualityGrade = grade,
                        qualityNotice = "⚡ $reason ➔ Bậc ${newStep.targetBitrateMbps}M",
                        lastDegradeReason = reason
                    )

                    QualityAdapter.applyAdaptiveBitrate(newStep.targetBitrateMbps, newStep.bufferHintMs)
                    return
                }
            } else if (loss <= 0.4 && (decodeTime in 0.1..9.5 || decodeTime == 0.0) && (rtt in 1..75 || rtt == 0) && jitter < 10) {
                goodSampleStreak++
                badSampleStreak = 0

                if (goodSampleStreak >= 4 && current.currentStepIndex < ladder.size - 1) {
                    val nextIdx = current.currentStepIndex + 1
                    val newStep = ladder[nextIdx]
                    val reason = "Đường truyền rất ổn định: Tự nâng lên ${newStep.title}"

                    goodSampleStreak = 0
                    Log.d(TAG, "Adaptive Quality: Stepping UP to ${newStep.targetBitrateMbps}Mbps")

                    _state.value = current.copy(
                        currentStepIndex = nextIdx,
                        currentStep = newStep,
                        qualityGrade = grade,
                        qualityNotice = "✨ $reason",
                        lastUpgradeReason = reason
                    )

                    QualityAdapter.applyAdaptiveBitrate(newStep.targetBitrateMbps, newStep.bufferHintMs)
                    return
                }
            } else {
                badSampleStreak = 0
                goodSampleStreak = 0
            }
        }

        val noticeText = if (decodeTime > 0.0) {
            "Giải mã: ${String.format("%.1f", decodeTime)}ms • Ping: ${rtt}ms • Mất gói: ${loss}% • Bậc: ${current.currentStep.targetBitrateMbps}M"
        } else {
            "Ping: ${rtt}ms • Jitter: ${jitter}ms • Bậc: ${current.currentStep.targetBitrateMbps}M"
        }

        _state.value = current.copy(
            qualityGrade = grade,
            qualityNotice = noticeText
        )
    }

    fun setStepManually(index: Int) {
        val ladder = _state.value.ladderSteps
        if (index in ladder.indices) {
            val step = ladder[index]
            _state.value = _state.value.copy(
                currentStepIndex = index,
                currentStep = step,
                qualityNotice = "Đã chỉnh thủ công: ${step.title} (${step.targetBitrateMbps} Mbps)"
            )
            QualityAdapter.applyAdaptiveBitrate(step.targetBitrateMbps, step.bufferHintMs)
        }
    }

    fun toggleAutoAdapting(enabled: Boolean) {
        _state.value = _state.value.copy(
            isAutoAdapting = enabled,
            qualityNotice = if (enabled) "Đã bật tự động thích ứng chất lượng theo mạng" else "Đã cố định bậc bitrate hiện tại"
        )
    }
}
