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
    ULTRA("Rất Cao 1080p", "🔥 Rất Cao"),
    HIGH("Cao (Sắc Nét)", "✨ Cao"),
    BALANCED("Trung Bình (Cân Bằng)", "🌿 Trung Bình"),
    SAVER("Thấp (Tiết Kiệm)", "❄️ Tiết Kiệm"),
    EMERGENCY("Siêu Nhẹ (Chống Giật)", "🛡️ Siêu Nhẹ")
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
    val qualityNotice: String = "Tự động tối ưu đồ họa & giảm tải nhiệt độ máy theo mạng",
    val lastDegradeReason: String? = null,
    val lastUpgradeReason: String? = null
)

private val DEFAULT_STEP = BitrateLadderStep(
    tier = QualityTier.BALANCED,
    targetBitrateMbps = 9.0,
    bufferHintMs = 18,
    title = "Trung Bình (9 Mbps)",
    description = "Cân bằng độ nét và nhiệt độ máy, giữ 60 FPS mượt mà"
)

object AdaptiveQualityManager {
    private const val TAG = "AdaptiveQualityManager"
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _state = MutableStateFlow(AdaptiveQualityState())
    val state: StateFlow<AdaptiveQualityState> = _state.asStateFlow()

    private var badSampleStreak = 0
    private var goodSampleStreak = 0

    // Chế độ Tự Động (Wi-Fi 5GHz)
    val WIFI_5GHZ_LADDER = listOf(
        BitrateLadderStep(QualityTier.SAVER, 5.0, 22, "Tiết Kiệm Dung Lượng (5M)", "Giảm tải GPU tối đa, siêu mát máy"),
        BitrateLadderStep(QualityTier.BALANCED, 9.0, 20, "Trung Bình Cân Bằng (9M)", "Mát máy, giữ 60 FPS ổn định"),
        BitrateLadderStep(QualityTier.HIGH, 14.0, 18, "Cao Sắc Nét 1080p (14M)", "Hình ảnh 1080p60 sắc nét, chi tiết cao"),
        BitrateLadderStep(QualityTier.ULTRA, 20.0, 16, "Rất Cao Tối Đa (20M)", "Bung trọn vẹn đồ họa xCloud tối đa")
    )

    // Chế độ Tự Động (Wi-Fi 2.4GHz / Chuẩn)
    val WIFI_24GHZ_LADDER = listOf(
        BitrateLadderStep(QualityTier.SAVER, 5.0, 24, "Tiết Kiệm Dung Lượng (5M)", "Chống nghẽn sóng và giảm tải phần cứng"),
        BitrateLadderStep(QualityTier.BALANCED, 9.0, 20, "Trung Bình Cân Bằng (9M)", "Độ ổn định tối ưu, máy chạy mát"),
        BitrateLadderStep(QualityTier.HIGH, 14.0, 18, "Cao Sắc Nét (14M)", "Hình ảnh 1080p sắc nét mượt 60 FPS")
    )

    // Mức Thấp (Tiết Kiệm Dung Lượng)
    val LOW_SAVER_LADDER = listOf(
        BitrateLadderStep(QualityTier.EMERGENCY, 3.5, 24, "Siêu Tiết Kiệm (3.5M)", "Siêu nhẹ cho máy yếu & sóng yếu, giải mã <3ms"),
        BitrateLadderStep(QualityTier.SAVER, 5.0, 20, "Thấp • Tiết Kiệm (5M)", "Tiết kiệm 60% dung lượng, siêu mát máy & đỡ hao pin")
    )

    // Mức Trung Bình (Cân Bằng)
    val MEDIUM_BALANCED_LADDER = listOf(
        BitrateLadderStep(QualityTier.SAVER, 5.0, 22, "Tiết Kiệm Dung Lượng (5M)", "Hạ tải tạm thời khi mạng hoặc GPU bận"),
        BitrateLadderStep(QualityTier.BALANCED, 9.0, 18, "Trung Bình • Cân Bằng (9M)", "Cân bằng độ nét và nhiệt độ máy khi chơi lâu")
    )

    // Mức Cao (Sắc Nét 1080p)
    val HIGH_QUALITY_LADDER = listOf(
        BitrateLadderStep(QualityTier.BALANCED, 9.0, 20, "Trung Bình (9M)", "Giữ mượt 60 FPS khi sóng dao động"),
        BitrateLadderStep(QualityTier.HIGH, 14.0, 18, "Cao • Sắc Nét 1080p (14M)", "Chuẩn đồ họa 1080p60 sắc nét, chi tiết cao")
    )

    // Mức Rất Cao (Tối Đa)
    val ULTRA_MAX_LADDER = listOf(
        BitrateLadderStep(QualityTier.BALANCED, 9.0, 22, "Trung Bình (9M)", "Chống rớt khung khi mạng biến động"),
        BitrateLadderStep(QualityTier.HIGH, 14.0, 18, "Cao (14M)", "Khung hình 1080p60 mượt mà, độ nét cao"),
        BitrateLadderStep(QualityTier.ULTRA, 20.0, 16, "Rất Cao • Tối Đa (20M)", "Bung tối đa 20 Mbps đồ họa xCloud")
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
        val cellularMode = config.cellularMode

        val ladder: List<BitrateLadderStep> = when (cellularMode) {
            CellularOptimizationMode.AUTO -> {
                if (isWifi && status.physicalType == PhysicalNetworkType.WIFI_5GHZ) {
                    WIFI_5GHZ_LADDER
                } else if (isWifi) {
                    WIFI_24GHZ_LADDER
                } else {
                    MEDIUM_BALANCED_LADDER
                }
            }
            CellularOptimizationMode.DATA_SAVER_HIGH_SPEED -> LOW_SAVER_LADDER
            CellularOptimizationMode.MEDIUM -> MEDIUM_BALANCED_LADDER
            CellularOptimizationMode.HIGH -> HIGH_QUALITY_LADDER
            CellularOptimizationMode.MAX_SPEED_UNCAPPED -> ULTRA_MAX_LADDER
        }

        val defaultIdx = when (cellularMode) {
            CellularOptimizationMode.AUTO -> if (ladder.size > 2) ladder.size - 2 else ladder.size - 1
            else -> ladder.size - 1
        }
        val selectedIdx = defaultIdx.coerceIn(0, ladder.size - 1)
        val activeStep = ladder[selectedIdx]

        _state.value = _state.value.copy(
            ladderSteps = ladder,
            currentStep = activeStep,
            currentStepIndex = selectedIdx,
            qualityGrade = "Đang tối ưu",
            qualityNotice = "${status.physicalType.icon} Chế độ: ${activeStep.title}"
        )

        QualityAdapter.applyAdaptiveBitrate(activeStep.targetBitrateMbps, activeStep.bufferHintMs)
    }

    /**
     * Evaluates real-time WebRTC telemetry (decode time, frame drops, jitter, RTT, bitrate)
     * and auto-scales the bitrate ladder to prevent phone overheating, GPU overload, and frame drops.
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
            loss > 3.0 || decodeTime > 14.0 -> "Tải Nặng / Mạng Yếu"
            loss > 1.2 || rtt > 120 || decodeTime > 10.5 -> "Đang Giảm Tải"
            decodeTime in 0.1..7.5 && loss <= 0.3 && rtt in 1..65 -> "Siêu Mượt & Mát Máy"
            else -> "Ổn Định (60 FPS)"
        }

        // Automatic Ladder Stepping (Protects both Network Stability and Phone Hardware Load)
        if (current.isAutoAdapting) {
            // Degrade early at decodeTime > 11.5ms to prevent phone thermal throttling and red Decode Performance warnings
            val isDegradingCondition = decodeTime > 11.5 || loss > 1.8 || rtt > 130 || jitter > 20 || (fps in 1..38 && loss > 0.8)

            if (isDegradingCondition) {
                badSampleStreak++
                goodSampleStreak = 0

                if (badSampleStreak >= 2 && current.currentStepIndex > 0) {
                    val nextIdx = current.currentStepIndex - 1
                    val newStep = ladder[nextIdx]
                    val reason = when {
                        decodeTime > 11.5 -> "Tải GPU cao (${String.format("%.1f", decodeTime)}ms): Tự hạ đồ họa để làm mát máy"
                        loss > 1.8 -> "Mất gói mạng (${loss}%): Tự giảm băng thông chống giật"
                        rtt > 130 -> "Ping cao (${rtt}ms): Tự hạ bậc ưu tiên phản hồi tay cầm"
                        else -> "Tự động cân bằng tải để giữ 60 FPS"
                    }

                    badSampleStreak = 0
                    Log.d(TAG, "Adaptive Quality: Stepping DOWN to ${newStep.targetBitrateMbps}Mbps - Reason: $reason")

                    _state.value = current.copy(
                        currentStepIndex = nextIdx,
                        currentStep = newStep,
                        qualityGrade = grade,
                        qualityNotice = "⚡ $reason ➔ ${newStep.targetBitrateMbps}M",
                        lastDegradeReason = reason
                    )

                    QualityAdapter.applyAdaptiveBitrate(newStep.targetBitrateMbps, newStep.bufferHintMs)
                    return
                }
            } else if (loss <= 0.4 && (decodeTime in 0.1..8.0 || decodeTime == 0.0) && (rtt in 1..75 || rtt == 0) && jitter < 10) {
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
