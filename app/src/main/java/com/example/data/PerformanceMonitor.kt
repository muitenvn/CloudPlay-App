package com.example.data

import android.os.SystemClock
import android.view.Choreographer
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

enum class PerfScenario(val code: String, val labelVi: String, val labelEn: String) {
    HOME("HOME", "Trang chủ", "Home"),
    LIBRARY("LIBRARY", "Thư viện game", "Game Library"),
    SERVERS("SERVERS", "Danh sách máy chủ", "Server List"),
    SETTINGS("SETTINGS", "Cài đặt", "Settings"),
    WEBVIEW_BROWSE("WEBVIEW", "Duyệt trang Xbox", "Xbox Web Browse"),
    STREAM_ACTIVE("STREAM", "Đang chơi (Stream)", "Active Stream")
}

/**
 * Bộ đo hiệu năng chỉ-đọc, siêu nhẹ (không cấp phát bộ nhớ trong mỗi khung hình `doFrame`):
 * - Đo khung hình thực tế qua `Choreographer.FrameCallback` khi ứng dụng đang ở tiền cảnh (`onAppStart` .. `onAppStop`).
 * - KHÔNG ghi định kỳ 30 giây vào nhật ký chính để tránh làm trôi các dòng log mạng/VPN quan trọng.
 * - Chỉ ghi vào `AppLogger` (tag `[PERF]`, giới hạn tối đa 40 dòng) trong 2 trường hợp:
 *   1. Khi người dùng chuyển màn hình / ngữ cảnh (hoặc đưa app xuống nền) nếu phiên đo vừa qua có >= 10 khung hình hoặc >= 500 ms.
 *   2. Khi phát hiện khung hình đơ nặng trên luồng chính (> 100 ms), có giãn cách tối thiểu 5 giây/lần.
 */
object PerformanceMonitor {

    private const val JANK_FRAME_THRESHOLD_NS = 17_000_000L // ~17.0 ms (> 16.67ms của 60Hz)
    private const val SLOW_FRAME_THRESHOLD_NS = 34_000_000L // ~34.0 ms (> 2 khung hình 60Hz)
    private const val FROZEN_FRAME_THRESHOLD_NS = 100_000_000L // 100.0 ms (khựng rõ trên luồng chính)
    private const val FROZEN_LOG_COOLDOWN_MS = 5_000L
    private const val MIN_WINDOW_DURATION_MS = 500L
    private const val MIN_WINDOW_FRAMES = 10L

    private class ScenarioAggregate {
        var totalWindows: Int = 0
        var totalDurationMs: Long = 0L
        var totalFrames: Long = 0L
        var jankFrames: Long = 0L
        var slowFrames: Long = 0L
        var frozenFrames: Long = 0L
        var maxFrameMs: Double = 0.0
        var recompositions: Long = 0L
        var scrollSamples: Long = 0L
    }

    private val lock = Any()
    private val aggregates = PerfScenario.entries.associateWith { ScenarioAggregate() }
    private val activeWindowRecompositions = PerfScenario.entries.associateWith { AtomicInteger(0) }
    private val activeWindowScrollEvents = PerfScenario.entries.associateWith { AtomicInteger(0) }

    @Volatile
    private var isForegroundActive = false

    @Volatile
    private var currentScenario: PerfScenario = PerfScenario.HOME

    private var windowStartElapsedMs: Long = 0L
    private var lastFrameTimeNanos: Long = 0L
    private var windowFrames: Long = 0L
    private var windowJankFrames: Long = 0L
    private var windowSlowFrames: Long = 0L
    private var windowFrozenFrames: Long = 0L
    private var windowMaxFrameNanos: Long = 0L
    private var lastFrozenLogElapsedMs: Long = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isForegroundActive) return
            val prevNanos = lastFrameTimeNanos
            lastFrameTimeNanos = frameTimeNanos
            if (prevNanos != 0L && frameTimeNanos > prevNanos) {
                val deltaNs = frameTimeNanos - prevNanos
                // Bỏ qua khoảng nghỉ dài > 2.5 giây khi màn hình đứng yên không vẽ gì (idle)
                if (deltaNs <= 2_500_000_000L) {
                    windowFrames++
                    if (deltaNs > windowMaxFrameNanos) {
                        windowMaxFrameNanos = deltaNs
                    }
                    if (deltaNs >= JANK_FRAME_THRESHOLD_NS) {
                        windowJankFrames++
                    }
                    if (deltaNs >= SLOW_FRAME_THRESHOLD_NS) {
                        windowSlowFrames++
                    }
                    if (deltaNs >= FROZEN_FRAME_THRESHOLD_NS) {
                        windowFrozenFrames++
                        val nowMs = nowElapsedMs()
                        if (nowMs - lastFrozenLogElapsedMs >= FROZEN_LOG_COOLDOWN_MS) {
                            lastFrozenLogElapsedMs = nowMs
                            val frameMs = deltaNs / 1_000_000.0
                            val scen = currentScenario
                            val recomp = activeWindowRecompositions[scen]?.get() ?: 0
                            val (usedMb, maxMb) = readHeapMb()
                            AppLogger.warn(
                                "PERF",
                                String.format(
                                    Locale.US,
                                    "Khựng luồng chính tại [%s]: %.1f ms (>100ms) | vẽ lại=%d | RAM=%d/%d MB",
                                    scen.code,
                                    frameMs,
                                    recomp,
                                    usedMb,
                                    maxMb
                                )
                            )
                        }
                    }
                }
            }
            try {
                Choreographer.getInstance().postFrameCallback(this)
            } catch (_: Throwable) {
            }
        }
    }

    private fun nowElapsedMs(): Long {
        return try {
            val rt = SystemClock.elapsedRealtime()
            if (rt > 0L) rt else System.currentTimeMillis()
        } catch (_: Throwable) {
            System.currentTimeMillis()
        }
    }

    private fun readHeapMb(): Pair<Long, Long> {
        return try {
            val rt = Runtime.getRuntime()
            val used = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L)
            val max = rt.maxMemory() / (1024L * 1024L)
            used to max
        } catch (_: Throwable) {
            0L to 0L
        }
    }

    fun onAppStart() {
        synchronized(lock) {
            if (isForegroundActive) return
            isForegroundActive = true
            resetWindowLocked(nowElapsedMs())
        }
        try {
            Choreographer.getInstance().postFrameCallback(frameCallback)
        } catch (_: Throwable) {
        }
    }

    fun onAppStop() {
        val summaryLine: String?
        synchronized(lock) {
            if (!isForegroundActive) return
            isForegroundActive = false
            summaryLine = flushCurrentWindowLocked(nowElapsedMs(), reason = "ẩn ứng dụng")
        }
        try {
            Choreographer.getInstance().removeFrameCallback(frameCallback)
        } catch (_: Throwable) {
        }
        if (summaryLine != null) {
            AppLogger.info("PERF", summaryLine)
        }
    }

    fun setScenario(nextScenario: PerfScenario) {
        var summaryLine: String? = null
        synchronized(lock) {
            if (currentScenario == nextScenario) return
            val nowMs = nowElapsedMs()
            if (isForegroundActive) {
                summaryLine = flushCurrentWindowLocked(nowMs, reason = "chuyển sang ${nextScenario.code}")
            }
            currentScenario = nextScenario
            resetWindowLocked(nowMs)
        }
        if (summaryLine != null) {
            AppLogger.info("PERF", summaryLine!!)
        }
    }

    fun recordRecomposition(scenario: PerfScenario) {
        activeWindowRecompositions[scenario]?.incrementAndGet()
    }

    fun recordScrollActivity(scenario: PerfScenario) {
        activeWindowScrollEvents[scenario]?.incrementAndGet()
    }

    private fun resetWindowLocked(nowMs: Long) {
        windowStartElapsedMs = nowMs
        lastFrameTimeNanos = 0L
        windowFrames = 0L
        windowJankFrames = 0L
        windowSlowFrames = 0L
        windowFrozenFrames = 0L
        windowMaxFrameNanos = 0L
        activeWindowRecompositions[currentScenario]?.set(0)
        activeWindowScrollEvents[currentScenario]?.set(0)
    }

    private fun flushCurrentWindowLocked(nowMs: Long, reason: String): String? {
        val scen = currentScenario
        val durationMs = (nowMs - windowStartElapsedMs).coerceAtLeast(1L)
        val frames = windowFrames
        val jank = windowJankFrames
        val slow = windowSlowFrames
        val frozen = windowFrozenFrames
        val maxFrameMs = windowMaxFrameNanos / 1_000_000.0
        val recomp = activeWindowRecompositions[scen]?.getAndSet(0) ?: 0
        val scrolls = activeWindowScrollEvents[scen]?.getAndSet(0) ?: 0

        if (frames < MIN_WINDOW_FRAMES && durationMs < MIN_WINDOW_DURATION_MS && frozen == 0L) {
            return null
        }

        val agg = aggregates[scen]
        if (agg != null) {
            agg.totalWindows++
            agg.totalDurationMs += durationMs
            agg.totalFrames += frames
            agg.jankFrames += jank
            agg.slowFrames += slow
            agg.frozenFrames += frozen
            if (maxFrameMs > agg.maxFrameMs) {
                agg.maxFrameMs = maxFrameMs
            }
            agg.recompositions += recomp
            agg.scrollSamples += scrolls
        }

        val jankPct = if (frames > 0L) (jank * 100.0) / frames else 0.0
        val (usedMb, maxMb) = readHeapMb()
        val scrollPart = if (scrolls > 0) " | cuộn=$scrolls" else ""
        return String.format(
            Locale.US,
            "[%s -> %s] %.1fs | khung=%d | chậm(>17ms)=%d (%.1f%%) | giật(>34ms)=%d | đơ(>100ms)=%d | trễ nhất=%.1fms | vẽ lại=%d%s | RAM=%d/%dMB",
            scen.code,
            reason,
            durationMs / 1000.0,
            frames,
            jank,
            jankPct,
            slow,
            frozen,
            maxFrameMs,
            recomp,
            scrollPart,
            usedMb,
            maxMb
        )
    }

    /**
     * Tạo báo cáo tổng hợp ngắn gọn (chỉ-đọc) để người dùng sao chép từ hộp thoại Nhật ký hệ thống.
     */
    fun buildShortPerfReport(isEn: Boolean = false): String {
        val (usedMb, maxMb) = readHeapMb()
        val sb = StringBuilder()
        if (isEn) {
            sb.appendLine("=== CLOUDPLAY [PERF] SUMMARY REPORT ===")
            sb.appendLine("Active Scenario: ${currentScenario.code} | Heap RAM: $usedMb / $maxMb MB")
        } else {
            sb.appendLine("=== BÁO CÁO HIỆU NĂNG CLOUDPLAY [PERF] ===")
            sb.appendLine("Màn hình hiện tại: ${currentScenario.code} (${currentScenario.labelVi}) | RAM: $usedMb / $maxMb MB")
        }

        synchronized(lock) {
            for (scen in PerfScenario.entries) {
                val agg = aggregates[scen] ?: continue
                val liveFrames = if (scen == currentScenario) windowFrames else 0L
                val liveJank = if (scen == currentScenario) windowJankFrames else 0L
                val liveSlow = if (scen == currentScenario) windowSlowFrames else 0L
                val liveFrozen = if (scen == currentScenario) windowFrozenFrames else 0L
                val liveMaxMs = if (scen == currentScenario) windowMaxFrameNanos / 1_000_000.0 else 0.0
                val liveRecomp = if (scen == currentScenario) (activeWindowRecompositions[scen]?.get() ?: 0) else 0
                val liveScroll = if (scen == currentScenario) (activeWindowScrollEvents[scen]?.get() ?: 0) else 0

                val totalFrames = agg.totalFrames + liveFrames
                val totalRecomp = agg.recompositions + liveRecomp
                if (totalFrames == 0L && totalRecomp == 0L) continue

                val totalJank = agg.jankFrames + liveJank
                val totalSlow = agg.slowFrames + liveSlow
                val totalFrozen = agg.frozenFrames + liveFrozen
                val maxMs = maxOf(agg.maxFrameMs, liveMaxMs)
                val totalScroll = agg.scrollSamples + liveScroll
                val jankPct = if (totalFrames > 0L) (totalJank * 100.0) / totalFrames else 0.0

                sb.appendLine(
                    String.format(
                        Locale.US,
                        "- [%s] frames=%d | jank(>17ms)=%d (%.1f%%) | slow(>34ms)=%d | frozen(>100ms)=%d | max=%.1fms | recomp=%d | scroll=%d",
                        scen.code,
                        totalFrames,
                        totalJank,
                        jankPct,
                        totalSlow,
                        totalFrozen,
                        maxMs,
                        totalRecomp,
                        totalScroll
                    )
                )
            }
        }

        val perfLogs = AppLogger.logs.value.filter { it.tag == "PERF" }
        if (perfLogs.isNotEmpty()) {
            sb.appendLine(if (isEn) "--- Recent [PERF] Events (${perfLogs.size}/${AppLogger.MAX_PERF_LOGS}) ---" else "--- Dòng nhật ký [PERF] gần đây (${perfLogs.size}/${AppLogger.MAX_PERF_LOGS}) ---")
            for (entry in perfLogs) {
                sb.appendLine("[${entry.timestamp}] [${entry.tag}] [${entry.level.name}] ${entry.message}")
            }
        }
        return sb.toString().trimEnd()
    }
}
