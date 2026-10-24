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

enum class QualityTier(val title: String, val badge: String, val resolutionTag: String) {
    ULTRA("1080p+ • Rất cao", "🔥 1080p+ Rất cao", "1080p+"),
    HIGH("1080p • Cao", "✨ 1080p Cao", "1080p"),
    BALANCED("900p • Vừa", "🌿 900p Vừa", "900p"),
    SAVER("720p • Thấp", "❄️ 720p Thấp", "720p"),
    EMERGENCY("720p • Siêu nhẹ", "🛡️ 720p Nhẹ", "720p")
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
    val qualityNotice: String = "Tự động điều chỉnh độ phân giải (720p ⇄ 1080p) và băng thông theo mạng thực tế",
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
        title = "Tự động • ${tier.title} (${mbps} Mbps)",
        description = "Tự động điều chỉnh 720p ⇄ 1080p (1..20 Mbps) theo mạng 4G/5G & Wi-Fi"
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
    private var highDecodeStartMs = 0L
    private var highDecodeTicks = 0
    private var savedNextSessionMbps: Int? = null

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

        val isEn = config.isEnglishUI()
        val nextSessionNote = if (isEn) "New level takes effect next session" else "Mức mới có tác dụng ở lần chơi sau"

        if (config.lowEndDeviceMode) {
            val lowEndMbps = 5.0
            val lowEndBuf = 20.0
            val lowEndStep = BitrateLadderStep(
                tier = QualityTier.SAVER,
                targetBitrateMbps = lowEndMbps,
                bufferHintMs = lowEndBuf.roundToInt(),
                title = if (isEn) "Low-End Mode • 5 Mbps" else "Chế Độ Máy Yếu • 5 Mbps",
                description = if (isEn) "Cool & smooth 60 FPS, pauses translation in-game" else "Nhẹ & mát máy, giữ mượt 60 FPS"
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
                qualityGrade = if (isEn) "Low-End (5M)" else "Chế Độ Máy Yếu (5M)",
                qualityNotice = "🛡️ $nextSessionNote"
            )
            QualityAdapter.applyAdaptiveBitrate(
                targetBitrateMbps = lowEndMbps,
                bufferHintMs = lowEndBuf,
                smoothGlide = true
            )
            return
        }

        if (cellularMode == CellularOptimizationMode.AUTO) {
            val isCellular = status.isCellular || (!isWifi && status.physicalType == PhysicalNetworkType.CELLULAR_4G_5G)
            val networkDefaultMbps = when {
                isWifi && status.physicalType == PhysicalNetworkType.WIFI_6GHZ -> 15.0
                isWifi && status.physicalType == PhysicalNetworkType.WIFI_5GHZ -> 12.0
                isWifi -> 10.0
                isCellular -> 6.0
                else -> 8.0
            }
            val initialSeedMbps = savedNextSessionMbps?.toDouble() ?: networkDefaultMbps
            val initialBufMs = when {
                current.isAutoAdapting && emaDecodeMs > 0.0 -> current.smoothBufferMs.coerceIn(6.0, 42.0)
                isCellular -> 18.0
                initialSeedMbps >= 14.0 -> 10.0
                initialSeedMbps >= 10.0 -> 12.0
                else -> 14.0
            }

            val roundedMbps = formatOneDecimal(initialSeedMbps)
            val tier = tierForMbps(roundedMbps)
            val idx = (roundedMbps.roundToInt() - 1).coerceIn(0, FULL_CONTINUOUS_SPECTRUM.lastIndex)
            val dynamicStep = BitrateLadderStep(
                tier = tier,
                targetBitrateMbps = roundedMbps,
                bufferHintMs = initialBufMs.roundToInt(),
                title = if (isEn) "Auto • ${roundedMbps.toInt()} Mbps" else "Tự Động • ${roundedMbps.toInt()} Mbps (${tier.title})",
                description = if (isEn) "Smoothly adjusts 4G/Wi-Fi buffer delay (6–42ms)" else "Tự chỉnh mượt bộ đệm 4G/Wi-Fi chống giật & giảm trễ (6–42ms)"
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
                qualityGrade = if (isEn) "Auto" else "Tự Động",
                qualityNotice = "${status.physicalType.icon} $nextSessionNote"
            )

            QualityAdapter.applyAdaptiveBitrate(
                targetBitrateMbps = roundedMbps,
                bufferHintMs = initialBufMs,
                smoothGlide = true
            )
        } else {
            val fixedMbps = cellularMode.bitrateMbps.toDouble()
            val fixedBuf = when (cellularMode) {
                CellularOptimizationMode.DATA_SAVER_HIGH_SPEED -> 18.0
                CellularOptimizationMode.MEDIUM -> 14.0
                CellularOptimizationMode.HIGH -> 10.0
                CellularOptimizationMode.MAX_SPEED_UNCAPPED -> 8.0
                CellularOptimizationMode.AUTO -> 12.0
            }
            val tier = tierForMbps(fixedMbps)
            val fixedStep = BitrateLadderStep(
                tier = tier,
                targetBitrateMbps = fixedMbps,
                bufferHintMs = fixedBuf.roundToInt(),
                title = if (isEn) "Fixed • ${fixedMbps.toInt()} Mbps" else "Cố Định • ${cellularMode.title} (${fixedMbps.toInt()} Mbps)",
                description = cellularMode.localizedDesc(isEn)
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
                qualityGrade = if (isEn) "Fixed (${fixedMbps.toInt()}M)" else "Cố Định (${fixedMbps.toInt()}M)",
                qualityNotice = "📌 $nextSessionNote"
            )

            QualityAdapter.applyAdaptiveBitrate(
                targetBitrateMbps = fixedMbps,
                bufferHintMs = fixedBuf,
                smoothGlide = true
            )
        }
    }

    /**
     * Đánh giá thông số luồng thực tế trên cả 4G và Wi-Fi:
     * - Khi sóng 4G có xung nhiễu HARQ (jitter > 14ms hoặc loss > 0.5%): tự nâng nhẹ bộ đệm (lên tới 42ms) để hứng trọn gói tin trễ, tránh vứt bỏ khung hình và ngăn bão I-Frame gây đứng hình.
     * - Khi mạng Wi-Fi / 4G sạch (jitter <= 5ms, loss <= 0.1%): tự thu nhỏ bộ đệm xuống 6ms–10ms giúp triệt tiêu độ trễ tay cầm/cảm ứng.
     */
    fun evaluateTelemetry(telemetry: StreamTelemetry) {
        val current = _state.value
        val decodeTime = telemetry.decodeTimeMs
        val loss = telemetry.packetsLostPercent
        val rtt = telemetry.rttMs.toDouble()
        val jitter = telemetry.jitterMs.toDouble()
        val fps = telemetry.fps

        if (decodeTime > 0.0) {
            emaDecodeMs = if (emaDecodeMs == 0.0) decodeTime else (emaDecodeMs * 0.60 + decodeTime * 0.40)
        }
        if (rtt > 0.0) {
            emaRttMs = if (emaRttMs == 0.0) rtt else (emaRttMs * 0.65 + rtt * 0.35)
        }
        if (jitter >= 0.0) {
            emaJitterMs = if (emaJitterMs == 0.0) jitter else (emaJitterMs * 0.55 + jitter * 0.45)
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

        val isEn = SettingsRepository.config.value.isEnglishUI()
        val nextSessionNote = if (isEn) "New level takes effect next session" else "Mức mới có tác dụng ở lần chơi sau"

        val grade = when {
            loss > 2.0 || effectiveDecode > 11.5 || effectiveJitter > 22.0 -> if (isEn) "4G Burst Cushion" else "Đang Hứng Sóng 4G Chống Giật"
            effectiveDecode > 7.8 || decodeTrendDelta >= 1.0 || effectiveJitter > 9.0 || effectiveRtt > 92.0 -> if (isEn) "Steady Buffer" else "Ổn Định Bộ Đệm"
            effectiveDecode in 0.1..6.5 && loss <= 0.2 && effectiveRtt in 1.0..65.0 && effectiveJitter <= 6.0 -> if (isEn) "Ultra Low-Latency" else "Siêu Mượt • Trễ Cực Thấp"
            else -> if (isEn) "Stable" else "Ổn Định"
        }

        // Theo dõi áp lực mạng 4G/5G & tải giải mã GPU:
        // Khi bật Tự động (isAutoAdapting == true), chủ động hạ mượt Bitrate & Độ phân giải (1080p ➔ 900p ➔ 720p)
        // để giảm độ trễ ngay lập tức, và nâng dần lại khi đường truyền sạch ổn định.
        var steppedDownForNextSession = false
        var fixedDecodeSlowHint = false
        if (effectiveDecode > 10.5 || emaLossPercent > 1.5 || (NetworkOptimizer.status.value.isCellular && (effectiveRtt > 105.0 || effectiveJitter > 16.0))) {
            val now = System.currentTimeMillis()
            if (highDecodeStartMs == 0L) highDecodeStartMs = now
            highDecodeTicks++
            if (now - highDecodeStartMs >= 6_000L || highDecodeTicks >= 4) {
                if (current.isAutoAdapting) {
                    val baseMbps = (savedNextSessionMbps ?: current.smoothBitrateMbps.roundToInt()).coerceAtLeast(5)
                    val nextLowerMbps = when {
                        baseMbps >= 15 -> 12
                        baseMbps >= 12 -> 9
                        baseMbps >= 9 -> 6
                        else -> 5
                    }
                    if (savedNextSessionMbps != nextLowerMbps) {
                        savedNextSessionMbps = nextLowerMbps
                        steppedDownForNextSession = true
                    }
                } else {
                    fixedDecodeSlowHint = true
                }
                highDecodeStartMs = now
                highDecodeTicks = 0
            }
        } else {
            highDecodeStartMs = 0L
            highDecodeTicks = 0
        }

        if (current.isAutoAdapting) {
            val isCellular = NetworkOptimizer.status.value.isCellular
            val minBufMs = if (isCellular) 10.0 else 6.0
            val maxBufMs = if (isCellular) 42.0 else 32.0
            val maxTargetMbps = when {
                isCellular -> 10.0
                NetworkOptimizer.status.value.physicalType == PhysicalNetworkType.WIFI_6GHZ -> 15.0
                NetworkOptimizer.status.value.physicalType == PhysicalNetworkType.WIFI_5GHZ -> 14.0
                else -> 12.0
            }

            // Tính toán mức đệm lý tưởng dựa theo độ dao động sóng (jitter) thực tế của 4G/5G hoặc Wi-Fi
            val jitterMatchedTargetMs = (effectiveJitter * 1.45 + emaLossPercent * 3.5 + (effectiveDecode * 0.4)).coerceIn(minBufMs, maxBufMs)
            var deltaBufferMs = 0.0
            var deltaBitrateMbps = 0.0
            var actionNote: String? = null

            val isHeavyPressure = effectiveDecode > 10.0 ||
                decodeTime > 11.0 ||
                emaLossPercent > 1.0 ||
                loss > 1.4 ||
                effectiveRtt > 115.0 ||
                effectiveJitter > 14.0 ||
                (fps in 1..48 && loss > 0.4)

            val isEarlyWarmup = (effectiveDecode in 7.6..10.0) ||
                (decodeTime >= 7.0 && decodeTrendDelta >= 0.9) ||
                (effectiveJitter in 8.0..14.0) ||
                (jitterTrendDelta >= 3.5 && jitter >= 7.5) ||
                (effectiveRtt in 85.0..115.0 && rttTrendDelta >= 8.0) ||
                (emaLossPercent in 0.3..1.0)

            if (isHeavyPressure) {
                deltaBufferMs = if (jitterMatchedTargetMs > current.smoothBufferMs) {
                    (jitterMatchedTargetMs - current.smoothBufferMs).coerceIn(1.5, 4.5)
                } else {
                    +1.5
                }
                deltaBitrateMbps = if (isCellular) -1.5 else -1.0
                actionNote = if (isEn) "Auto lowering resolution/bitrate to cut 4G/5G lag" else "Tự hạ độ phân giải & băng thông giảm trễ 4G/5G"
            } else if (isEarlyWarmup) {
                deltaBufferMs = +0.8
                deltaBitrateMbps = -0.5
                actionNote = if (isEn) "Stabilizing 4G/Wi-Fi latency" else "Ổn định độ phân giải & bộ đệm chống trễ"
            } else if (
                emaLossPercent <= 0.15 &&
                (effectiveDecode in 0.1..6.8 || effectiveDecode == 0.0) &&
                decodeTrendDelta <= 0.4 &&
                (effectiveRtt in 1.0..80.0 || effectiveRtt == 0.0) &&
                effectiveJitter <= 6.5
            ) {
                deltaBufferMs = -0.8
                if (current.smoothBitrateMbps < maxTargetMbps) {
                    deltaBitrateMbps = +0.5
                }
                actionNote = if (isEn) "Tightening live latency" else "Siết bộ đệm giảm độ trễ"
            }

            val nextBufMs = formatOneDecimal(
                (current.smoothBufferMs + deltaBufferMs).coerceIn(minBufMs, maxBufMs)
            )
            val nextBitrateMbps = formatOneDecimal(
                (current.smoothBitrateMbps + deltaBitrateMbps).coerceIn(4.5, maxTargetMbps)
            )
            val bufferChanged = abs(nextBufMs - current.smoothBufferMs) >= 0.3
            val bitrateChanged = abs(nextBitrateMbps - current.smoothBitrateMbps) >= 0.4
            val gradeChanged = current.qualityGrade != grade

            if (bufferChanged || bitrateChanged || gradeChanged || steppedDownForNextSession) {
                val updatedTier = tierForMbps(nextBitrateMbps)
                val updatedIdx = (nextBitrateMbps.roundToInt() - 1).coerceIn(0, FULL_CONTINUOUS_SPECTRUM.lastIndex)
                val updatedStep = current.currentStep.copy(
                    tier = updatedTier,
                    targetBitrateMbps = nextBitrateMbps,
                    bufferHintMs = nextBufMs.roundToInt(),
                    title = if (isEn) {
                        "Auto • ${updatedTier.resolutionTag} (${nextBitrateMbps.roundToInt()} Mbps)"
                    } else {
                        "Tự động • ${updatedTier.title} (${nextBitrateMbps.roundToInt()} Mbps)"
                    }
                )
                val notice = when {
                    actionNote != null && (bufferChanged || bitrateChanged) -> "🌊 $actionNote (${updatedTier.resolutionTag} • ${nextBufMs.roundToInt()}ms)"
                    steppedDownForNextSession -> "📌 $nextSessionNote"
                    else -> "🌊 $nextSessionNote"
                }

                _state.value = current.copy(
                    currentStep = updatedStep,
                    currentStepIndex = updatedIdx,
                    smoothBitrateMbps = nextBitrateMbps,
                    smoothBufferMs = nextBufMs,
                    qualityGrade = grade,
                    qualityNotice = notice,
                    lastDegradeReason = if (deltaBufferMs > 0 || deltaBitrateMbps < 0) actionNote else current.lastDegradeReason,
                    lastUpgradeReason = if (deltaBufferMs < 0 || deltaBitrateMbps > 0) actionNote else current.lastUpgradeReason
                )

                if (bufferChanged || bitrateChanged) {
                    QualityAdapter.applyAdaptiveBitrate(
                        targetBitrateMbps = nextBitrateMbps,
                        bufferHintMs = nextBufMs,
                        smoothGlide = true
                    )
                }
            }
            return
        }

        val fixedGrade = if (isEn) "Fixed (${current.smoothBitrateMbps.toInt()}M)" else "Cố Định (${current.smoothBitrateMbps.toInt()}M)"
        val fixedSuggestion = if (isEn) {
            "💡 Device is busy, consider choosing a lower quality level"
        } else {
            "💡 Máy hơi quá tải, gợi ý chọn mức chất lượng thấp hơn"
        }
        if (current.qualityGrade != fixedGrade || fixedDecodeSlowHint) {
            _state.value = current.copy(
                qualityGrade = fixedGrade,
                qualityNotice = if (fixedDecodeSlowHint) fixedSuggestion else "📌 $nextSessionNote"
            )
        }
    }

    fun setStepManually(index: Int) {
        val isEn = SettingsRepository.config.value.isEnglishUI()
        val nextSessionNote = if (isEn) "New level takes effect next session" else "Mức mới có tác dụng ở lần chơi sau"
        val ladder = _state.value.ladderSteps
        if (index in ladder.indices) {
            val step = ladder[index]
            _state.value = _state.value.copy(
                isAutoAdapting = false,
                currentStepIndex = index,
                currentStep = step.copy(title = if (isEn) "Fixed • ${step.targetBitrateMbps.toInt()} Mbps" else "Cố Định • ${step.targetBitrateMbps.toInt()} Mbps"),
                smoothBitrateMbps = step.targetBitrateMbps,
                smoothBufferMs = step.bufferHintMs.toDouble(),
                qualityNotice = "📌 $nextSessionNote"
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
        val isEn = SettingsRepository.config.value.isEnglishUI()
        val nextSessionNote = if (isEn) "New level takes effect next session" else "Mức mới có tác dụng ở lần chơi sau"
        val current = _state.value
        val tier = tierForMbps(current.smoothBitrateMbps)
        val updatedStep = current.currentStep.copy(
            targetBitrateMbps = current.smoothBitrateMbps,
            title = if (enabled) {
                if (isEn) "Auto • ${current.smoothBitrateMbps.roundToInt()} Mbps" else "Tự Động • ${current.smoothBitrateMbps.roundToInt()} Mbps (${tier.title})"
            } else {
                if (isEn) "Fixed • ${current.smoothBitrateMbps.roundToInt()} Mbps" else "Cố Định • ${current.smoothBitrateMbps.roundToInt()} Mbps (${tier.title})"
            }
        )
        _state.value = current.copy(
            isAutoAdapting = enabled,
            currentStep = updatedStep,
            qualityNotice = "📌 $nextSessionNote"
        )
    }
}
