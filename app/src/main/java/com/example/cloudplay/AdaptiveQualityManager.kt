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
    EMERGENCY("720p • Nhẹ", "🛡️ 720p Nhẹ", "720p")
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
        BitrateLadderStep(QualityTier.SAVER, 5.0, 22, "Tiết Kiệm Dung Lượng (5M)", "Giảm tải GPU, mát máy"),
        BitrateLadderStep(QualityTier.BALANCED, 9.0, 20, "Trung Bình Cân Bằng (9M)", "Mát máy, giữ 60 FPS ổn định"),
        BitrateLadderStep(QualityTier.HIGH, 14.0, 18, "Cao Sắc Nét 1080p (14M)", "Hình ảnh 1080p60 sắc nét, chi tiết cao"),
        BitrateLadderStep(QualityTier.ULTRA, 20.0, 16, "Rất Cao Tối Đa (20M)", "Bung trọn vẹn đồ họa xCloud tối đa")
    )

    val WIFI_24GHZ_LADDER = listOf(
        BitrateLadderStep(QualityTier.SAVER, 5.0, 24, "Tiết Kiệm Dung Lượng (5M)", "Hạn chế nghẽn sóng và giảm tải phần cứng"),
        BitrateLadderStep(QualityTier.BALANCED, 9.0, 20, "Trung Bình Cân Bằng (9M)", "Độ ổn định tối ưu, máy chạy mát"),
        BitrateLadderStep(QualityTier.HIGH, 14.0, 18, "Cao Sắc Nét (14M)", "Hình ảnh 1080p sắc nét mượt 60 FPS")
    )

    val LOW_SAVER_LADDER = listOf(
        BitrateLadderStep(QualityTier.EMERGENCY, 3.5, 24, "Tiết Kiệm Tối Đa (3.5M)", "Nhẹ cho máy yếu & sóng yếu"),
        BitrateLadderStep(QualityTier.SAVER, 5.0, 20, "Thấp • Cố Định (5M)", "Tiết kiệm 60% dung lượng, mát máy & đỡ hao pin")
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
        BitrateLadderStep(QualityTier.BALANCED, 9.0, 22, "Trung Bình (9M)", "Hạn chế rớt khung khi mạng biến động"),
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
        val nextSessionNote = if (isEn) "Adaptive network & control tuning active" else "Tự động thích ứng mạng & ưu tiên độ nhạy thao tác"

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
                    if (isCellular) current.smoothBufferMs.coerceIn(15.0, 38.0)
                    else current.smoothBufferMs.coerceIn(12.0, 45.0)
                }
                isCellular && status.isWeakCellularSignal -> 20.0
                isCellular -> 18.0
                status.isWeakWifiSignal -> 18.0
                initialSeedMbps >= 14.0 -> 14.0
                initialSeedMbps >= 10.0 -> 16.0
                else -> 18.0
            }

            val roundedMbps = formatOneDecimal(initialSeedMbps)
            val tier = tierForMbps(roundedMbps)
            val idx = (roundedMbps.roundToInt() - 1).coerceIn(0, FULL_CONTINUOUS_SPECTRUM.lastIndex)
            val dynamicStep = BitrateLadderStep(
                tier = tier,
                targetBitrateMbps = roundedMbps,
                bufferHintMs = initialBufMs.roundToInt(),
                title = if (isEn) "Auto • ${roundedMbps.toInt()} Mbps" else "Tự Động • ${roundedMbps.toInt()} Mbps (${tier.title})",
                description = if (isEn) "Fast response 4G mode & queue delay mitigation" else "Chế độ phản hồi nhanh (4G) & hạn chế nghẽn đường truyền"
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
                    if (isEn) "4G Fast Response" else "Ưu Tiên Độ Nhạy 4G"
                } else {
                    if (isEn) "Auto Adaptive" else "Tự Động Thích Ứng"
                },
                qualityNotice = "${status.physicalType.icon} $nextSessionNote"
            )

            QualityAdapter.applyAdaptiveBitrate(
                targetBitrateMbps = roundedMbps,
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
        if (!telemetry.hasStatsSample) return
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
        val nextSessionNote = if (isEn) "Adaptive network & control tuning active" else "Tự động thích ứng mạng & ưu tiên độ nhạy thao tác"

        val netStatus = NetworkOptimizer.status.value
        val isCellular = netStatus.isCellular || (!netStatus.isWifi && netStatus.physicalType == PhysicalNetworkType.CELLULAR_4G_5G)

        val grade = when {
            deltaFreezes > 0 || loss > 1.5 || effectiveDecode > 12.0 || effectiveJitter > 20.0 -> if (isEn) "Adapting Queue" else "Đang Giảm Tải Đường Truyền"
            deltaDropped >= 3 || effectiveDecode > 9.0 || effectiveJitter > 11.0 || rttTrendDelta >= 12.0 -> if (isEn) "Stabilizing Stream" else "Đang Ổn Định Khung Hình"
            isCellular && effectiveDecode <= 8.0 && loss <= 0.4 && effectiveJitter <= 12.0 -> if (isEn) "4G Fast Response" else "4G Ổn Định • Phản Hồi Nhanh"
            effectiveDecode in 0.1..7.5 && loss <= 0.2 && effectiveRtt in 1.0..85.0 && effectiveJitter <= 8.0 -> if (isEn) "Optimal Stream" else "Ổn Định • Độ Trễ Thấp"
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
            // Bám sát biến động vật lý thực tế (jitterMs), tránh ép bộ đệm xuống dưới ngưỡng an toàn gây buffer underrun
            val minBufMs = if (isCellular) (effectiveJitter * 1.5).coerceAtLeast(15.0) else (effectiveJitter * 1.3).coerceAtLeast(12.0)
            val maxBufMs = if (isCellular) 40.0 else 50.0
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
            val jitterWeight = if (isCellular) 1.35 else 1.25
            val jitterMatchedTargetMs = (14.0 + effectiveJitter * jitterWeight + emaLossPercent * 2.5 + (effectiveDecode * 0.35) + rttAllowanceMs)
                .coerceIn(minBufMs, maxBufMs)
            var deltaBufferMs = 0.0
            var deltaBitrateMbps = 0.0
            var multiplicativeTargetMbps: Double? = null
            var actionNote: String? = null

            val isHeavyPressure = deltaFreezes > 0 ||
                deltaDropped >= 4 ||
                effectiveDecode > 12.0 ||
                decodeTime > 14.0 ||
                emaLossPercent > 0.8 ||
                loss > 1.2 ||
                rttTrendDelta >= (if (isCellular) 12.0 else 18.0) ||
                effectiveJitter > (if (isCellular) 15.0 else 18.0) ||
                (fps in 1..48 && (loss > 0.3 || effectiveDecode > 10.0))

            val isEarlyWarmup = deltaDropped in 2..3 ||
                (effectiveDecode in 9.5..12.0) ||
                (decodeTime >= 9.0 && decodeTrendDelta >= 1.5) ||
                (effectiveJitter in (if (isCellular) 9.0 else 10.0)..15.0) ||
                (jitterTrendDelta >= 4.0 && jitter >= 8.0) ||
                (rttTrendDelta >= (if (isCellular) 8.0 else 10.0)) ||
                (emaLossPercent in 0.3..0.8)

            if (isHeavyPressure) {
                cleanRecoveryTicks = 0
                deltaBufferMs = if (jitterMatchedTargetMs > current.smoothBufferMs) {
                    (jitterMatchedTargetMs - current.smoothBufferMs).coerceIn(2.0, 5.0)
                } else {
                    +2.0
                }
                // Lưu ý kỹ thuật WebRTC: Máy khách (receiver) không thể ép máy chủ Xbox đổi bitrate mã hóa
                // ngay giữa phiên nếu không thương lượng lại SDP. Do đó trong phiên hiện tại, bộ điều khiển
                // chỉ điều chỉnh tức thì bộ đệm nhận (jitterBufferTarget / playoutDelayHint), còn mức trần
                // bitrate (×0.55 về 4.0-6.0 Mbps trên 4G) sẽ áp dụng vào SDP (b=AS:) ở lần kết nối/phiên kế tiếp.
                if (isCellular) {
                    multiplicativeTargetMbps = (current.smoothBitrateMbps * 0.55).coerceIn(4.0, 6.0)
                } else {
                    deltaBitrateMbps = if (netStatus.isWeakWifiSignal || effectiveDecode > 13.0) -2.5 else -2.0
                }
                actionNote = if (isCellular) {
                    if (isEn) "4G jitter: adjusting receiver buffer now; SDP cap (4-6M) applies next session" else "Sóng 4G dao động: điều chỉnh đệm nhận ngay; trần SDP (4-6M) áp dụng phiên sau"
                } else {
                    if (isEn) "Expanding receiver buffer now; new SDP bitrate cap applies next session" else "Tăng bộ đệm nhận trong phiên; trần bitrate SDP mới áp dụng phiên sau"
                }
            } else if (isEarlyWarmup) {
                cleanRecoveryTicks = 0
                deltaBufferMs = +1.5
                deltaBitrateMbps = if (isCellular) -1.2 else -0.8
                actionNote = if (isEn) "Adjusting WebRTC receiver buffer for network jitter" else "Điều chỉnh bộ đệm nhận WebRTC khi mạng dao động"
            } else if (
                deltaFreezes == 0 &&
                deltaDropped == 0 &&
                emaLossPercent <= 0.20 &&
                (effectiveDecode in 0.1..8.5 || effectiveDecode == 0.0) &&
                decodeTrendDelta <= 0.6 &&
                rttTrendDelta <= 5.0 &&
                effectiveJitter <= (if (isCellular) 10.0 else 8.0)
            ) {
                cleanRecoveryTicks++
                // Additive Increase kèm chu kỳ trễ (hysteresis 5-6 nhịp) để tránh dao động bộ đệm liên tục
                val requiredCleanTicks = if (isCellular) 5 else 6
                if (cleanRecoveryTicks >= requiredCleanTicks) {
                    if (current.smoothBufferMs > minBufMs) {
                        deltaBufferMs = -1.5
                    }
                    if (current.smoothBitrateMbps < maxTargetMbps) {
                        deltaBitrateMbps = +0.5
                    }
                    cleanRecoveryTicks = 0
                    actionNote = if (isEn) "Stream stable: tightening receiver buffer" else "Đường truyền ổn định: thu gọn dần bộ đệm nhận"
                }
            }

            val nextBufMs = formatOneDecimal(
                (current.smoothBufferMs + deltaBufferMs).coerceIn(minBufMs, maxBufMs)
            )
            val rawNextMbps = multiplicativeTargetMbps ?: (current.smoothBitrateMbps + deltaBitrateMbps)
            val nextBitrateMbps = formatOneDecimal(
                rawNextMbps.coerceIn(4.0, maxTargetMbps)
            )
            val bufferChanged = abs(nextBufMs - current.smoothBufferMs) >= 1.5
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
                        "Buffer ${nextBufMs.roundToInt()}ms • SDP next: ${nextBitrateMbps.roundToInt()}M"
                    } else {
                        "Đệm ${nextBufMs.roundToInt()}ms • Trần SDP phiên sau: ${nextBitrateMbps.roundToInt()}M"
                    }
                )
                val notice = when {
                    actionNote != null && (bufferChanged || bitrateChanged) -> "⚡ $actionNote (Đệm ${nextBufMs.roundToInt()}ms)"
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
                    lastDegradeReason = if (deltaBufferMs > 0 || nextBitrateMbps < current.smoothBitrateMbps) actionNote else current.lastDegradeReason,
                    lastUpgradeReason = if (deltaBufferMs < 0 || nextBitrateMbps > current.smoothBitrateMbps) actionNote else current.lastUpgradeReason
                )

                if (bufferChanged || bitrateChanged) {
                    QualityAdapter.applyAdaptiveBitrate(
                        targetBitrateMbps = nextBitrateMbps,
                        smoothGlide = true
                    )
                }
            }
            return
        }

        val fixedGrade = if (isEn) "SDP Cap (${current.smoothBitrateMbps.toInt()}M)" else "Trần SDP (${current.smoothBitrateMbps.toInt()}M)"
        val fixedSuggestion = if (isEn) {
            "💡 Device is busy, consider choosing a lower quality level for next session"
        } else {
            "💡 Máy hơi quá tải, gợi ý chọn mức trần thấp hơn cho phiên sau"
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
        val nextSessionNote = if (isEn) "Buffer updated now; SDP bitrate cap applies next session" else "Đã chỉnh bộ đệm nhận; trần bitrate SDP áp dụng ở phiên chơi sau"
        val ladder = _state.value.ladderSteps
        if (index in ladder.indices) {
            val step = ladder[index]
            _state.value = _state.value.copy(
                isAutoAdapting = false,
                currentStepIndex = index,
                currentStep = step.copy(title = if (isEn) "SDP Cap • ${step.targetBitrateMbps.toInt()} Mbps" else "Trần SDP • ${step.targetBitrateMbps.toInt()} Mbps"),
                smoothBitrateMbps = step.targetBitrateMbps,
                smoothBufferMs = step.bufferHintMs.toDouble(),
                qualityNotice = "📌 $nextSessionNote"
            )
            QualityAdapter.applyAdaptiveBitrate(
                targetBitrateMbps = step.targetBitrateMbps,
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
        val nextSessionNote = if (isEn) "Auto receiver buffer active; SDP bitrate cap applies next session" else "Tự động chỉnh bộ đệm nhận trong phiên; trần bitrate SDP áp dụng phiên sau"
        val current = _state.value
        val tier = tierForMbps(current.smoothBitrateMbps)
        val updatedStep = current.currentStep.copy(
            targetBitrateMbps = current.smoothBitrateMbps,
            title = if (enabled) {
                if (isEn) "Auto Buffer • SDP ${current.smoothBitrateMbps.roundToInt()}M" else "Tự Động Đệm • Trần SDP ${current.smoothBitrateMbps.roundToInt()}M (${tier.title})"
            } else {
                if (isEn) "Fixed • SDP ${current.smoothBitrateMbps.roundToInt()}M" else "Cố Định • Trần SDP ${current.smoothBitrateMbps.roundToInt()}M (${tier.title})"
            }
        )
        _state.value = current.copy(
            isAutoAdapting = enabled,
            currentStep = updatedStep,
            qualityNotice = "📌 $nextSessionNote"
        )
    }
}
