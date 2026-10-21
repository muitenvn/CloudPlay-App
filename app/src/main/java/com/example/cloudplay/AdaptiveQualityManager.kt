package com.example.cloudplay

import android.util.Log
import com.example.data.AppConfig
import com.example.data.CellularOptimizationMode
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

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
    val currentStepIndex: Int = 9,
    val ladderSteps: List<BitrateLadderStep> = FULL_CONTINUOUS_SPECTRUM,
    val smoothBitrateMbps: Double = 10.0,
    val smoothBufferMs: Double = 18.0,
    val minAutoMbps: Double = 1.0,
    val maxAutoMbps: Double = 20.0,
    val qualityGrade: String = "Sẵn sàng",
    val qualityNotice: String = "Tự động biến thiên tự do (1M ➔ 20M): Tăng giảm từng nấc mượt, không giới hạn bậc cố định",
    val lastDegradeReason: String? = null,
    val lastUpgradeReason: String? = null
)

private fun tierForMbps(mbps: Double): QualityTier = when {
    mbps >= 16.0 -> QualityTier.ULTRA
    mbps >= 11.5 -> QualityTier.HIGH
    mbps >= 7.0 -> QualityTier.BALANCED
    mbps >= 4.0 -> QualityTier.SAVER
    else -> QualityTier.EMERGENCY
}

private val FULL_CONTINUOUS_SPECTRUM: List<BitrateLadderStep> = (1..20).map { mbps ->
    val tier = tierForMbps(mbps.toDouble())
    val buf = (24 - ((mbps - 1) * 8 / 19)).coerceIn(16, 24)
    BitrateLadderStep(
        tier = tier,
        targetBitrateMbps = mbps.toDouble(),
        bufferHintMs = buf,
        title = "Tự Động Linh Hoạt • ${mbps} Mbps (${tier.title})",
        description = "Biến thiên tự do từng nấc 1..20 Mbps theo mạng & tải máy thực tế"
    )
}

private val DEFAULT_STEP = FULL_CONTINUOUS_SPECTRUM[9] // 10 Mbps starting point

object AdaptiveQualityManager {
    private const val TAG = "AdaptiveQualityManager"
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _state = MutableStateFlow(AdaptiveQualityState())
    val state: StateFlow<AdaptiveQualityState> = _state.asStateFlow()

    // Exponential Moving Averages & Early Trend Predictors for Zero-Lag 1-by-1 Stepping
    private var emaDecodeMs = 0.0
    private var prevDecodeMs = 0.0
    private var emaRttMs = 0.0
    private var prevRttMs = 0.0
    private var emaJitterMs = 0.0
    private var prevJitterMs = 0.0
    private var emaLossPercent = 0.0

    // Kept for unit test & legacy compatibility
    val WIFI_5GHZ_LADDER = listOf(
        BitrateLadderStep(QualityTier.SAVER, 5.0, 22, "Tiết Kiệm Dung Lượng (5M)", "Giảm tải GPU tối đa, siêu mát máy"),
        BitrateLadderStep(QualityTier.BALANCED, 9.0, 20, "Trung Bình Cân Bằng (9M)", "Mát máy, giữ 60 FPS ổn định"),
        BitrateLadderStep(QualityTier.HIGH, 14.0, 18, "Cao Sắc Nét 1080p (14M)", "Hình ảnh 1080p60 sắc nét, chi tiết cao"),
        BitrateLadderStep(QualityTier.ULTRA, 20.0, 16, "Rất Cao Tối Đa (20M)", "Bung trọn vẹn đồ họa xCloud tối đa")
    )

    val WIFI_24GHZ_LADDER = listOf(
        BitrateLadderStep(QualityTier.SAVER, 5.0, 24, "Tiết Kiệm Dung Lượng (5M)", "Chống nghẽn sóng và giảm tải phần cứng"),
        BitrateLadderStep(QualityTier.BALANCED, 9.0, 20, "Trung Bình Cân Bằng (9M)", "Độ ổn định tối ưu, máy chạy mát"),
        BitrateLadderStep(QualityTier.HIGH, 14.0, 18, "Cao Sắc Nét (14M)", "Hình ảnh 1080p sắc nét mượt 60 FPS")
    )

    val LOW_SAVER_LADDER = listOf(
        BitrateLadderStep(QualityTier.EMERGENCY, 3.5, 24, "Siêu Tiết Kiệm (3.5M)", "Siêu nhẹ cho máy yếu & sóng yếu, giải mã <3ms"),
        BitrateLadderStep(QualityTier.SAVER, 5.0, 20, "Thấp • Cố Định (5M)", "Tiết kiệm 60% dung lượng, siêu mát máy & đỡ hao pin")
    )

    val MEDIUM_BALANCED_LADDER = listOf(
        BitrateLadderStep(QualityTier.SAVER, 5.0, 22, "Tiết Kiệm Dung Lượng (5M)", "Hạ tải tạm thời khi mạng hoặc GPU bận"),
        BitrateLadderStep(QualityTier.BALANCED, 9.0, 18, "Trung Bình • Cố Định (9M)", "Cân bằng độ nét và nhiệt độ máy khi chơi lâu")
    )

    val HIGH_QUALITY_LADDER = listOf(
        BitrateLadderStep(QualityTier.BALANCED, 9.0, 20, "Trung Bình (9M)", "Giữ mượt 60 FPS khi sóng dao động"),
        BitrateLadderStep(QualityTier.HIGH, 14.0, 18, "Cao • Cố Định (14M)", "Chuẩn đồ họa 1080p60 sắc nét, chi tiết cao")
    )

    val ULTRA_MAX_LADDER = listOf(
        BitrateLadderStep(QualityTier.BALANCED, 9.0, 22, "Trung Bình (9M)", "Chống rớt khung khi mạng biến động"),
        BitrateLadderStep(QualityTier.HIGH, 14.0, 18, "Cao (14M)", "Khung hình 1080p60 mượt mà, độ nét cao"),
        BitrateLadderStep(QualityTier.ULTRA, 20.0, 16, "Rất Cao • Cố Định (20M)", "Bung tối đa 20 Mbps đồ họa xCloud")
    )

    val CELLULAR_SAVER_LADDER = LOW_SAVER_LADDER
    val CELLULAR_MAX_LADDER = ULTRA_MAX_LADDER

    init {
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

    private fun formatOneDecimal(value: Double): Double {
        return ((value * 10.0).roundToInt()) / 10.0
    }

    private fun reconfigureLadder(status: NetworkOptimizerStatus, config: AppConfig) {
        val isWifi = status.isWifi
        val cellularMode = config.cellularMode
        val current = _state.value

        if (config.lowEndDeviceMode) {
            val lowEndMbps = 5.0
            val lowEndBuf = 20.0
            val lowEndStep = BitrateLadderStep(
                tier = QualityTier.SAVER,
                targetBitrateMbps = lowEndMbps,
                bufferHintMs = lowEndBuf.roundToInt(),
                title = "Chế Độ Máy Yếu • 5 Mbps (H.264 Nhẹ • 60FPS)",
                description = "Khóa cố định 5 Mbps, H.264 nhẹ, ưu tiên 60 FPS, tắt hiệu ứng động & dừng dịch khi chơi"
            )
            val idx = (lowEndMbps.roundToInt() - 1).coerceIn(0, FULL_CONTINUOUS_SPECTRUM.lastIndex)
            _state.value = current.copy(
                isAutoAdapting = false,
                ladderSteps = FULL_CONTINUOUS_SPECTRUM,
                currentStep = lowEndStep,
                currentStepIndex = idx,
                smoothBitrateMbps = lowEndMbps,
                smoothBufferMs = lowEndBuf,
                minAutoMbps = 1.0,
                maxAutoMbps = 5.0,
                qualityGrade = "Chế Độ Máy Yếu (5M)",
                qualityNotice = "🛡️ Chế độ máy yếu: Khóa 5 Mbps • H.264 nhẹ • Ưu tiên 60 FPS (Thoát game và vào lại để áp dụng)"
            )
            QualityAdapter.applyAdaptiveBitrate(
                targetBitrateMbps = lowEndMbps,
                bufferHintMs = lowEndBuf,
                smoothGlide = true
            )
            return
        }

        if (cellularMode == CellularOptimizationMode.AUTO) {
            // Chế độ Tự Động: Không bị khóa vào 1 thang cố định mà mở toàn dải 1M..20M (1, 2, 3, 4, 5, 6, 7, 8, 9... 20)
            val initialSeedMbps = when {
                current.isAutoAdapting && emaDecodeMs > 0.0 -> current.smoothBitrateMbps.coerceIn(1.0, 20.0)
                isWifi && status.physicalType == PhysicalNetworkType.WIFI_6GHZ -> 16.0
                isWifi && status.physicalType == PhysicalNetworkType.WIFI_5GHZ -> 13.0
                isWifi -> 10.0
                else -> 8.0
            }
            val initialBufMs = when {
                current.isAutoAdapting && emaDecodeMs > 0.0 -> current.smoothBufferMs.coerceIn(15.0, 24.0)
                initialSeedMbps >= 14.0 -> 17.0
                initialSeedMbps >= 9.0 -> 18.0
                else -> 19.5
            }

            val roundedMbps = formatOneDecimal(initialSeedMbps)
            val tier = tierForMbps(roundedMbps)
            val idx = (roundedMbps.roundToInt() - 1).coerceIn(0, FULL_CONTINUOUS_SPECTRUM.lastIndex)
            val dynamicStep = BitrateLadderStep(
                tier = tier,
                targetBitrateMbps = roundedMbps,
                bufferHintMs = initialBufMs.roundToInt(),
                title = "Tự Động Linh Hoạt • ${roundedMbps} Mbps (${tier.title})",
                description = "Tăng giảm mượt từng nấc (1 ➔ 20 Mbps) theo thời gian thực, không cố định bậc"
            )

            _state.value = current.copy(
                isAutoAdapting = true,
                ladderSteps = FULL_CONTINUOUS_SPECTRUM,
                currentStep = dynamicStep,
                currentStepIndex = idx,
                smoothBitrateMbps = roundedMbps,
                smoothBufferMs = formatOneDecimal(initialBufMs),
                minAutoMbps = 1.0,
                maxAutoMbps = 20.0,
                qualityGrade = "Tự Động Linh Hoạt",
                qualityNotice = "${status.physicalType.icon} Tự động từng nấc: hiện tại ${roundedMbps}M (Dải tự do 1M–20M)"
            )

            QualityAdapter.applyAdaptiveBitrate(
                targetBitrateMbps = roundedMbps,
                bufferHintMs = initialBufMs,
                smoothGlide = true
            )
        } else {
            // Chế độ Cố Định (Thấp 5M, Trung Bình 9M, Cao 14M, Rất Cao 20M): Giữ cố định giá trị người dùng chọn
            val fixedMbps = cellularMode.bitrateMbps.toDouble()
            val fixedBuf = when (cellularMode) {
                CellularOptimizationMode.DATA_SAVER_HIGH_SPEED -> 20.0
                CellularOptimizationMode.MEDIUM -> 18.5
                CellularOptimizationMode.HIGH -> 17.5
                CellularOptimizationMode.MAX_SPEED_UNCAPPED -> 16.0
                CellularOptimizationMode.AUTO -> 18.0
            }
            val tier = tierForMbps(fixedMbps)
            val fixedStep = BitrateLadderStep(
                tier = tier,
                targetBitrateMbps = fixedMbps,
                bufferHintMs = fixedBuf.roundToInt(),
                title = "Cố Định • ${cellularMode.title} (${fixedMbps.toInt()} Mbps)",
                description = cellularMode.description
            )
            val idx = (fixedMbps.roundToInt() - 1).coerceIn(0, FULL_CONTINUOUS_SPECTRUM.lastIndex)

            _state.value = current.copy(
                isAutoAdapting = false,
                ladderSteps = FULL_CONTINUOUS_SPECTRUM,
                currentStep = fixedStep,
                currentStepIndex = idx,
                smoothBitrateMbps = fixedMbps,
                smoothBufferMs = fixedBuf,
                minAutoMbps = 1.0,
                maxAutoMbps = 20.0,
                qualityGrade = "Cố Định (${fixedMbps.toInt()}M)",
                qualityNotice = "📌 Đang khóa cố định mức ${cellularMode.title} (${fixedMbps.toInt()} Mbps)"
            )

            QualityAdapter.applyAdaptiveBitrate(
                targetBitrateMbps = fixedMbps,
                bufferHintMs = fixedBuf,
                smoothGlide = true
            )
        }
    }

    /**
     * Evaluates real-time WebRTC telemetry and steps smoothly across 1..20 Mbps (e.g. 10 -> 9 -> 8 -> 7
     * or 8 -> 9 -> 10 -> 11 -> 12) proactively BEFORE lag spikes occur, never jumping across big fixed gaps.
     */
    fun evaluateTelemetry(telemetry: StreamTelemetry) {
        val current = _state.value
        val decodeTime = telemetry.decodeTimeMs
        val loss = telemetry.packetsLostPercent
        val rtt = telemetry.rttMs.toDouble()
        val jitter = telemetry.jitterMs.toDouble()
        val fps = telemetry.fps

        // Compute Exponential Moving Averages & Early Trend Velocity
        if (decodeTime > 0.0) {
            emaDecodeMs = if (emaDecodeMs == 0.0) decodeTime else (emaDecodeMs * 0.60 + decodeTime * 0.40)
        }
        if (rtt > 0.0) {
            emaRttMs = if (emaRttMs == 0.0) rtt else (emaRttMs * 0.65 + rtt * 0.35)
        }
        if (jitter >= 0.0) {
            emaJitterMs = if (emaJitterMs == 0.0) jitter else (emaJitterMs * 0.60 + jitter * 0.40)
        }
        emaLossPercent = emaLossPercent * 0.55 + loss * 0.45

        val decodeTrendDelta = if (prevDecodeMs > 0.0 && decodeTime > 0.0) decodeTime - prevDecodeMs else 0.0
        val jitterTrendDelta = if (prevJitterMs > 0.0) jitter - prevJitterMs else 0.0
        val rttTrendDelta = if (prevRttMs > 0.0 && rtt > 0.0) rtt - prevRttMs else 0.0

        if (decodeTime > 0.0) prevDecodeMs = decodeTime
        if (rtt > 0.0) prevRttMs = rtt
        prevJitterMs = jitter

        val effectiveDecode = if (emaDecodeMs > 0.0) emaDecodeMs else decodeTime
        val effectiveRtt = if (emaRttMs > 0.0) emaRttMs else rtt
        val effectiveJitter = if (emaJitterMs > 0.0) emaJitterMs else jitter

        val grade = when {
            loss > 2.0 || effectiveDecode > 11.5 -> "Đang Hạ Từng Nấc Êm"
            effectiveDecode > 7.8 || decodeTrendDelta >= 1.0 || effectiveJitter > 9.0 || effectiveRtt > 92.0 -> "Chủ Động Đệm Mượt"
            effectiveDecode in 0.1..6.5 && loss <= 0.2 && effectiveRtt in 1.0..65.0 -> "Siêu Mượt (Tự Do 1–20M)"
            else -> "Ổn Định Linh Hoạt"
        }

        if (current.isAutoAdapting) {
            var deltaMbps = 0.0
            var deltaBufferMs = 0.0
            var actionNote: String? = null

            // Early Predictive Detection: Step down smoothly by 0.5 - 1.0 Mbps BEFORE lag happens
            val isEarlyWarmup = (effectiveDecode in 7.6..10.0) ||
                (decodeTime >= 7.0 && decodeTrendDelta >= 0.9) ||
                (effectiveJitter in 8.0..14.0) ||
                (jitterTrendDelta >= 3.5 && jitter >= 7.5) ||
                (effectiveRtt in 82.0..115.0 && rttTrendDelta >= 8.0) ||
                (emaLossPercent in 0.3..1.0)

            // Stronger pressure: Step down by 1.0 - 1.5 Mbps per tick (still smooth 1-by-1 progression, no sudden cliff)
            val isHeavyPressure = effectiveDecode > 10.0 ||
                decodeTime > 11.0 ||
                emaLossPercent > 1.0 ||
                loss > 1.4 ||
                effectiveRtt > 115.0 ||
                effectiveJitter > 14.0 ||
                (fps in 1..48 && loss > 0.4)

            if (isHeavyPressure) {
                val stepDown = if (decodeTime > 13.5 || loss > 2.5) 1.5 else 1.0
                deltaMbps = -stepDown
                deltaBufferMs = +0.6
                actionNote = when {
                    effectiveDecode > 10.0 || decodeTime > 11.0 ->
                        "Hạ êm -${stepDown}M chống nóng GPU (${String.format("%.1f", decodeTime)}ms)"
                    emaLossPercent > 1.0 || loss > 1.4 ->
                        "Hạ êm -${stepDown}M chống rớt khung (${String.format("%.1f", loss)}%)"
                    else ->
                        "Lướt nhẹ -${stepDown}M giữ mượt 60FPS"
                }
            } else if (isEarlyWarmup) {
                // Proactive 0.5M - 1.0M step down BEFORE any stutter occurs
                val stepDown = if (effectiveDecode > 8.8 || decodeTrendDelta >= 1.4) 1.0 else 0.5
                deltaMbps = -stepDown
                deltaBufferMs = +0.35
                actionNote = "Dự đoán tải sớm: giảm nhẹ -${stepDown}M trước khi kịp lag"
            } else if (
                emaLossPercent <= 0.2 &&
                (effectiveDecode in 0.1..7.0 || effectiveDecode == 0.0) &&
                decodeTrendDelta <= 0.4 &&
                (effectiveRtt in 1.0..75.0 || effectiveRtt == 0.0) &&
                effectiveJitter <= 7.0
            ) {
                // Smoothly climb 1-by-1 (+0.5M or +1.0M) up through 1..20 Mbps whenever network & GPU have headroom
                val stepUp = if (effectiveDecode in 0.1..5.0 && effectiveJitter <= 4.0) 1.0 else 0.5
                deltaMbps = +stepUp
                deltaBufferMs = -0.25
                if (current.smoothBitrateMbps + deltaMbps <= current.maxAutoMbps) {
                    actionNote = "Mạng & máy thoáng: nâng mượt +${stepUp}M"
                }
            }

            val nextMbps = formatOneDecimal(
                (current.smoothBitrateMbps + deltaMbps).coerceIn(current.minAutoMbps, current.maxAutoMbps)
            )
            val nextBufMs = formatOneDecimal(
                (current.smoothBufferMs + deltaBufferMs).coerceIn(15.0, 24.0)
            )

            val tier = tierForMbps(nextMbps)
            val nearestIdx = (nextMbps.roundToInt() - 1).coerceIn(0, FULL_CONTINUOUS_SPECTRUM.lastIndex)

            val updatedStep = BitrateLadderStep(
                tier = tier,
                targetBitrateMbps = nextMbps,
                bufferHintMs = nextBufMs.roundToInt(),
                title = "Tự Động Linh Hoạt • ${nextMbps} Mbps (${tier.title})",
                description = "Biến thiên từng nấc tự do (1 ➔ 20 Mbps), chuyển độ họa êm không khựng"
            )

            val notice = when {
                actionNote != null && abs(nextMbps - current.smoothBitrateMbps) >= 0.1 ->
                    "🌊 $actionNote ➔ ${nextMbps}M"
                decodeTime > 0.0 ->
                    "🌊 Tự động: ${nextMbps}M • Giải mã: ${String.format("%.1f", decodeTime)}ms • Ping: ${telemetry.rttMs}ms"
                else ->
                    "🌊 Tự động: ${nextMbps}M • Ping: ${telemetry.rttMs}ms • Jitter: ${telemetry.jitterMs}ms"
            }

            _state.value = current.copy(
                currentStepIndex = nearestIdx,
                currentStep = updatedStep,
                smoothBitrateMbps = nextMbps,
                smoothBufferMs = nextBufMs,
                qualityGrade = grade,
                qualityNotice = notice,
                lastDegradeReason = if (deltaMbps < 0) actionNote else current.lastDegradeReason,
                lastUpgradeReason = if (deltaMbps > 0) actionNote else current.lastUpgradeReason
            )

            if (abs(nextMbps - current.smoothBitrateMbps) >= 0.1 || abs(nextBufMs - current.smoothBufferMs) >= 0.2) {
                Log.d(TAG, "Smooth 1-by-1 Auto Step: ${current.smoothBitrateMbps}M -> ${nextMbps}M (buf=${nextBufMs}ms)")
                QualityAdapter.applyAdaptiveBitrate(
                    targetBitrateMbps = nextMbps,
                    bufferHintMs = nextBufMs,
                    smoothGlide = true
                )
            }
            return
        }

        // Manual fixed mode telemetry summary
        val fixedMbps = current.smoothBitrateMbps
        val noticeText = if (decodeTime > 0.0) {
            "📌 Cố định: ${fixedMbps.toInt()}M • Giải mã: ${String.format("%.1f", decodeTime)}ms • Ping: ${telemetry.rttMs}ms"
        } else {
            "📌 Cố định: ${fixedMbps.toInt()}M • Ping: ${telemetry.rttMs}ms • Jitter: ${telemetry.jitterMs}ms"
        }

        _state.value = current.copy(
            qualityGrade = "Cố Định (${fixedMbps.toInt()}M)",
            qualityNotice = noticeText
        )
    }

    fun setStepManually(index: Int) {
        val ladder = _state.value.ladderSteps
        if (index in ladder.indices) {
            val step = ladder[index]
            _state.value = _state.value.copy(
                isAutoAdapting = false,
                currentStepIndex = index,
                currentStep = step.copy(title = "Cố Định • ${step.targetBitrateMbps.toInt()} Mbps (${step.tier.title})"),
                smoothBitrateMbps = step.targetBitrateMbps,
                smoothBufferMs = step.bufferHintMs.toDouble(),
                qualityNotice = "📌 Đã chọn mức cố định: ${step.targetBitrateMbps.toInt()} Mbps (${step.tier.title})"
            )
            QualityAdapter.applyAdaptiveBitrate(
                targetBitrateMbps = step.targetBitrateMbps,
                bufferHintMs = step.bufferHintMs.toDouble(),
                smoothGlide = true
            )
        }
    }

    fun setExactBitrateManually(mbps: Int) {
        val clamped = mbps.coerceIn(1, 20)
        setStepManually(clamped - 1)
    }

    fun toggleAutoAdapting(enabled: Boolean) {
        val current = _state.value
        val tier = tierForMbps(current.smoothBitrateMbps)
        val updatedStep = current.currentStep.copy(
            targetBitrateMbps = current.smoothBitrateMbps,
            title = if (enabled) {
                "Tự Động Linh Hoạt • ${current.smoothBitrateMbps} Mbps (${tier.title})"
            } else {
                "Cố Định • ${current.smoothBitrateMbps.roundToInt()} Mbps (${tier.title})"
            }
        )
        _state.value = current.copy(
            isAutoAdapting = enabled,
            currentStep = updatedStep,
            qualityNotice = if (enabled) {
                "🌊 Đã bật Tự Động Linh Hoạt (1M ➔ 20M): Tăng giảm từng nấc mượt, không giới hạn cố định"
            } else {
                "📌 Đã khóa cố định tại mức ${current.smoothBitrateMbps.roundToInt()} Mbps"
            }
        )
    }
}
