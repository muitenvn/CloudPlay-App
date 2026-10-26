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
    private val scope = CoroutineScope(Dispatchers.Default)

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
    private var prevFreezeCount = 0
    private var prevFramesDropped = 0
    private var highDecodeStartMs = 0L
    private var highDecodeTicks = 0
    private var cleanRecoveryTicks = 0
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
        val nextSessionNote = if (isEn) "Optimized for lowest control lag" else "Đã tối ưu ưu tiên độ nhạy điều khiển"

        if (config.lowEndDeviceMode) {
            val lowEndMbps = 5.0
            val lowEndBuf = 18.0
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
                isWifi && status.isWeakWifiSignal -> 7.0
                isWifi && status.physicalType == PhysicalNetworkType.WIFI_2_4GHZ -> 8.0
                isWifi && status.physicalType == PhysicalNetworkType.WIFI_6GHZ -> 14.0
                isWifi && status.physicalType == PhysicalNetworkType.WIFI_5GHZ -> 12.0
                isWifi -> 10.0
                isCellular && status.isWeakCellularSignal -> 5.0
                isCellular -> 6.0
                else -> 8.0
            }
            val initialSeedMbps = savedNextSessionMbps?.toDouble()?.coerceAtMost(networkDefaultMbps) ?: networkDefaultMbps
            val initialBufMs = when {
                current.isAutoAdapting && emaDecodeMs > 0.0 -> {
                    if (isCellular) current.smoothBufferMs.coerceIn(10.0, 22.0)
                    else current.smoothBufferMs.coerceIn(8.0, 42.0)
                }
                isCellular && status.isWeakCellularSignal -> 14.0
                isCellular -> 12.0
                status.isWeakWifiSignal -> 16.0
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
                description = if (isEn) "Ultra-low latency 4G (10-22ms) & adaptive anti-bufferbloat" else "Siêu nhạy 4G (đệm 10-22ms) & tự chống nghẽn trạm phát"
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
                qualityGrade = if (isCellular) {
                    if (isEn) "4G Low-Lag" else "4G Siêu Nhạy"
                } else {
                    if (isEn) "Auto" else "Tự Động"
                },
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
                CellularOptimizationMode.DATA_SAVER_HIGH_SPEED -> 14.0
                CellularOptimizationMode.MEDIUM -> 12.0
                CellularOptimizationMode.HIGH -> 10.0
                CellularOptimizationMode.MAX_SPEED_UNCAPPED -> 10.0
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
     * - Trên 4G (isCellular): ưu tiên chống nghẽn hàng đợi tại trạm phát LTE (Anti-Bufferbloat) bằng cách
     *   hạ nhanh bitrate (4.0..8.0 Mbps) và khóa trần bộ đệm thu ở 10..22ms, thu hồi đệm sau 3 nhịp (4.5s).
     * - Trên Wi-Fi: cho phép đệm thu 8..42ms và thu hồi sau 6 nhịp (9s) ổn định.
     */
    fun evaluateTelemetry(telemetry: StreamTelemetry) {
        val current = _state.value
        val decodeTime = telemetry.decodeTimeMs
        val loss = telemetry.packetsLostPercent
        val rtt = telemetry.rttMs.toDouble()
        val jitter = telemetry.jitterMs.toDouble()
        val fps = telemetry.fps

        val deltaFreezes = if (prevFreezeCount > 0 && telemetry.freezeCount >= prevFreezeCount) {
            telemetry.freezeCount - prevFreezeCount
        } else 0
        val deltaDropped = if (prevFramesDropped > 0 && telemetry.framesDropped >= prevFramesDropped) {
            telemetry.framesDropped - prevFramesDropped
        } else 0
        prevFreezeCount = telemetry.freezeCount
        prevFramesDropped = telemetry.framesDropped

        if (decodeTime > 0.0) {
            emaDecodeMs = if (emaDecodeMs == 0.0) decodeTime else (emaDecodeMs * 0.65 + decodeTime * 0.35)
        }
        if (rtt > 0.0) {
            emaRttMs = if (emaRttMs == 0.0) rtt else (emaRttMs * 0.65 + rtt * 0.35)
        }
        if (jitter >= 0.0) {
            emaJitterMs = if (emaJitterMs == 0.0) jitter else (emaJitterMs * 0.65 + jitter * 0.35)
        }
        emaLossPercent = emaLossPercent * 0.60 + loss * 0.40

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
        val nextSessionNote = if (isEn) "Optimized for lowest control lag" else "Đã tối ưu ưu tiên độ nhạy điều khiển"

        val netStatus = NetworkOptimizer.status.value
        val isCellular = netStatus.isCellular || (!netStatus.isWifi && netStatus.physicalType == PhysicalNetworkType.CELLULAR_4G_5G)

        val grade = when {
            deltaFreezes > 0 || loss > 1.5 || effectiveDecode > 12.0 || effectiveJitter > 20.0 -> if (isEn) "Anti-Spike Active" else "Đang Chống Khựng Sóng"
            deltaDropped >= 3 || effectiveDecode > 9.0 || effectiveJitter > 11.0 || rttTrendDelta >= 15.0 -> if (isEn) "Low-Lag Stabilizing" else "Đang Giữ Nhạy Điều Khiển"
            isCellular && effectiveDecode <= 8.0 && loss <= 0.4 && effectiveJitter <= 12.0 -> if (isEn) "4G Ultra Low-Lag" else "4G Siêu Nhạy • Trễ Thấp"
            effectiveDecode in 0.1..7.5 && loss <= 0.2 && effectiveRtt in 1.0..85.0 && effectiveJitter <= 8.0 -> if (isEn) "Ultra Low-Latency" else "Siêu Mượt • Trễ Cực Thấp"
            else -> if (isEn) "Stable" else "Ổn Định"
        }

        var steppedDownForNextSession = false
        var fixedDecodeSlowHint = false
        if (deltaFreezes > 0 || effectiveDecode > 12.0 || emaLossPercent > 1.0 || effectiveJitter > 16.0 || rttTrendDelta >= 20.0) {
            val now = System.currentTimeMillis()
            if (highDecodeStartMs == 0L) highDecodeStartMs = now
            highDecodeTicks++
            if (highDecodeTicks >= 2) {
                if (current.isAutoAdapting) {
                    val baseMbps = (savedNextSessionMbps ?: current.smoothBitrateMbps.roundToInt()).coerceAtLeast(5)
                    val nextLowerMbps = when {
                        baseMbps >= 15 -> 11
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
            // Trên 4G: giới hạn chặt bộ đệm thu 10..22ms để không cộng dồn trễ điều khiển
            val minBufMs = if (isCellular) 10.0 else 8.0
            val maxBufMs = if (isCellular) 22.0 else 42.0
            val maxTargetMbps = when {
                isCellular && netStatus.isWeakCellularSignal -> 5.0
                isCellular -> 8.0
                netStatus.isWeakWifiSignal -> 8.0
                netStatus.physicalType == PhysicalNetworkType.WIFI_2_4GHZ -> 9.0
                netStatus.physicalType == PhysicalNetworkType.WIFI_6GHZ -> 15.0
                netStatus.physicalType == PhysicalNetworkType.WIFI_5GHZ -> 14.0
                else -> 12.0
            }

            val rttAllowanceMs = if (!isCellular && effectiveRtt > 90.0) ((effectiveRtt - 90.0) * 0.08).coerceAtMost(10.0) else 0.0
            val jitterWeight = if (isCellular) 0.85 else 1.25
            val jitterMatchedTargetMs = (10.0 + effectiveJitter * jitterWeight + emaLossPercent * 2.5 + (effectiveDecode * 0.35) + rttAllowanceMs)
                .coerceIn(minBufMs, maxBufMs)
            var deltaBufferMs = 0.0
            var deltaBitrateMbps = 0.0
            var actionNote: String? = null

            val isHeavyPressure = deltaFreezes > 0 ||
                deltaDropped >= 4 ||
                effectiveDecode > 12.0 ||
                decodeTime > 14.0 ||
                emaLossPercent > 0.8 ||
                loss > 1.2 ||
                rttTrendDelta >= (if (isCellular) 14.0 else 18.0) ||
                effectiveJitter > (if (isCellular) 14.0 else 16.0) ||
                (fps in 1..48 && (loss > 0.3 || effectiveDecode > 10.0))

            val isEarlyWarmup = deltaDropped in 2..3 ||
                (effectiveDecode in 9.5..12.0) ||
                (decodeTime >= 9.0 && decodeTrendDelta >= 1.5) ||
                (effectiveJitter in (if (isCellular) 9.0 else 10.0)..16.0) ||
                (jitterTrendDelta >= 4.0 && jitter >= 8.0) ||
                (rttTrendDelta >= (if (isCellular) 8.0 else 10.0)) ||
                (emaLossPercent in 0.3..0.8)

            if (isHeavyPressure) {
                cleanRecoveryTicks = 0
                deltaBufferMs = if (isCellular) {
                    (jitterMatchedTargetMs - current.smoothBufferMs).coerceIn(1.0, 2.5)
                } else if (jitterMatchedTargetMs > current.smoothBufferMs) {
                    (jitterMatchedTargetMs - current.smoothBufferMs).coerceIn(2.0, 5.0)
                } else {
                    +2.0
                }
                deltaBitrateMbps = if (isCellular) -2.2 else if (netStatus.isWeakWifiSignal || effectiveDecode > 13.0) -2.0 else -1.5
                actionNote = if (isCellular) {
                    if (isEn) "4G Anti-Bufferbloat: shedding bitrate to keep control snappy" else "Chống nghẽn trạm 4G: hạ nhanh gói tin để giữ nút bấm siêu nhạy"
                } else {
                    if (isEn) "Expanding buffer & lowering bitrate to stop stutter" else "Tăng đệm & hạ nhẹ băng thông để chống khựng hình"
                }
            } else if (isEarlyWarmup) {
                cleanRecoveryTicks = 0
                deltaBufferMs = if (isCellular) +1.0 else +1.5
                deltaBitrateMbps = if (isCellular) -1.2 else -0.8
                actionNote = if (isEn) "Stabilizing Wi-Fi/4G frame queue" else "Ổn định hàng đợi khung hình khi mạng dao động"
            } else if (
                deltaFreezes == 0 &&
                deltaDropped == 0 &&
                emaLossPercent <= 0.20 &&
                (effectiveDecode in 0.1..8.5 || effectiveDecode == 0.0) &&
                decodeTrendDelta <= 0.6 &&
                rttTrendDelta <= 6.0 &&
                effectiveJitter <= (if (isCellular) 10.0 else 8.0)
            ) {
                cleanRecoveryTicks++
                // Trên 4G thu hồi bộ đệm nhanh gấp đôi (3 nhịp = 4.5s) để tránh giữ trễ lâu
                val requiredCleanTicks = if (isCellular) 3 else 6
                if (cleanRecoveryTicks >= requiredCleanTicks) {
                    if (current.smoothBufferMs > minBufMs) {
                        deltaBufferMs = if (isCellular) -2.0 else -1.5
                    }
                    if (current.smoothBitrateMbps < maxTargetMbps) {
                        deltaBitrateMbps = +0.5
                    }
                    cleanRecoveryTicks = 0
                    actionNote = if (isEn) "Stream stable: tightening buffer & restoring clarity" else "Luồng đã ổn định: thu gọn đệm & nâng nhẹ độ nét"
                }
            }

            val nextBufMs = formatOneDecimal(
                (current.smoothBufferMs + deltaBufferMs).coerceIn(minBufMs, maxBufMs)
            )
            val nextBitrateMbps = formatOneDecimal(
                (current.smoothBitrateMbps + deltaBitrateMbps).coerceIn(4.0, maxTargetMbps)
            )
            val bufferChanged = abs(nextBufMs - current.smoothBufferMs) >= (if (isCellular) 1.0 else 1.5)
            val bitrateChanged = abs(nextBitrateMbps - current.smoothBitrateMbps) >= 0.5
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
                    actionNote != null && (bufferChanged || bitrateChanged) -> "⚡ $actionNote (${updatedTier.resolutionTag} • ${nextBufMs.roundToInt()}ms)"
                    steppedDownForNextSession -> "📌 $nextSessionNote"
                    else -> "⚡ $nextSessionNote"
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

    /**
     * Xả ngay bộ đệm tích lũy (Instant Lag Flush) 1 chạm từ bảng HUD:
     * Đưa bộ đệm thu về mức siêu nhạy (10.0ms trên 4G / 8.0ms trên Wi-Fi), xóa bộ đếm phạt trễ
     * và gọi đồng bộ khung hình thời gian thực (Live-Edge Catch-up) trên WebView.
     */
    fun flushLagNow() {
        val isEn = SettingsRepository.config.value.isEnglishUI()
        val netStatus = NetworkOptimizer.status.value
        val isCellular = netStatus.isCellular || (!netStatus.isWifi && netStatus.physicalType == PhysicalNetworkType.CELLULAR_4G_5G)
        val current = _state.value
        val targetBufMs = if (isCellular) 10.0 else 8.0
        val targetMbps = if (isCellular && current.smoothBitrateMbps > 7.0) 6.0 else current.smoothBitrateMbps.coerceIn(4.0, 20.0)

        cleanRecoveryTicks = 0
        highDecodeTicks = 0
        highDecodeStartMs = 0L

        val updatedTier = tierForMbps(targetMbps)
        val updatedStep = current.currentStep.copy(
            tier = updatedTier,
            targetBitrateMbps = targetMbps,
            bufferHintMs = targetBufMs.roundToInt()
        )
        val flushGrade = if (isCellular) {
            if (isEn) "4G Flushed (${targetBufMs.toInt()}ms)" else "4G Đã Xả Trễ (${targetBufMs.toInt()}ms)"
        } else {
            if (isEn) "Lag Flushed (${targetBufMs.toInt()}ms)" else "Đã Xả Trễ (${targetBufMs.toInt()}ms)"
        }
        val flushNotice = if (isEn) {
            "⚡ Flushed receiver queue & synced live frame edge (${targetBufMs.toInt()}ms)"
        } else {
            "⚡ Đã xả sạch hàng đợi khung hình & đồng bộ thời gian thực (${targetBufMs.toInt()}ms)"
        }

        _state.value = current.copy(
            currentStep = updatedStep,
            smoothBitrateMbps = targetMbps,
            smoothBufferMs = targetBufMs,
            qualityGrade = flushGrade,
            qualityNotice = flushNotice,
            lastUpgradeReason = flushNotice
        )

        QualityAdapter.flushStreamLagNow(
            targetBitrateMbps = targetMbps,
            bufferHintMs = targetBufMs
        )
    }
}
