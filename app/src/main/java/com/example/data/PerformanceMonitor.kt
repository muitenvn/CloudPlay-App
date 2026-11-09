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
    private val loggedFirstTabCompositions = java.util.concurrent.ConcurrentHashMap.newKeySet<PerfScenario>()
    private val initMilestoneHistory = java.util.concurrent.CopyOnWriteArrayList<String>()
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val initTimeFormat = java.text.SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    @Volatile
    private var firstFrameLogged = false

    @Volatile
    private var isForegroundActive = false

    @Volatile
    private var currentScenario: PerfScenario = PerfScenario.HOME

    private var windowStartElapsedMs: Long = 0L
    private var currentFrameAnchorNanos: Long = 0L
    private var frameEndScheduled: Boolean = false
    private var windowFrames: Long = 0L
    private var windowJankFrames: Long = 0L
    private var windowSlowFrames: Long = 0L
    private var windowFrozenFrames: Long = 0L
    private var windowMaxFrameNanos: Long = 0L
    private var lastFrozenLogElapsedMs: Long = 0L
    private var lastFrozenLoggedRecomp: Int = 0

    /**
     * Lưu dòng [PERF-INIT] vào bộ nhớ riêng không bao giờ bị xoay vòng bởi giới hạn 40 dòng [PERF],
     * đồng thời ghi vào AppLogger để hiển thị trong Nhật ký hệ thống.
     */
    fun recordInitMilestone(message: String) {
        val ts = synchronized(initTimeFormat) { initTimeFormat.format(java.util.Date()) }
        val formatted = "[$ts] [PERF-INIT] [INFO] $message"
        if (initMilestoneHistory.size < 32) {
            initMilestoneHistory.add(formatted)
        }
        AppLogger.info("PERF-INIT", message)
    }

    /**
     * Ghi nhận 1 dòng duy nhất [PERF-INIT] cho thời gian chạy MainActivity.onCreate
     * và 1 dòng cho thời điểm khung hình đầu tiên (First Frame) được vẽ xong.
     */
    fun recordStartupInitSummary(
        totalOnCreateMs: Long,
        breakdown: String,
        onCreateStartElapsedMs: Long
    ) {
        recordInitMilestone("MainActivity.onCreate: tổng=${totalOnCreateMs}ms ($breakdown)")
        if (!firstFrameLogged) {
            try {
                Choreographer.getInstance().postFrameCallback {
                    if (!firstFrameLogged) {
                        firstFrameLogged = true
                        val firstFrameElapsed =
                            (nowElapsedMs() - onCreateStartElapsedMs).coerceAtLeast(0L)
                        recordInitMilestone(
                            "Khung hình đầu tiên (First Frame): +${firstFrameElapsed}ms từ đầu onCreate"
                        )
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    /**
     * Ghi nhận 1 lần duy nhất cho mỗi tab trong suốt vòng đời tiến trình:
     * bóc tách rõ 4 giai đoạn: commit (outer composition), subcompose+layout (LazyColumn/Grid/BoxWithConstraints measure+layout),
     * draw (ghi RenderNode/Canvas), và khung đầu (withFrameNanos kế tiếp).
     */
    fun recordFirstTabCompositionIfNeeded(
        scenario: PerfScenario,
        composeCommitMs: Long,
        layoutDoneMs: Long,
        drawDoneMs: Long,
        firstFrameMs: Long
    ) {
        if (loggedFirstTabCompositions.add(scenario)) {
            val (usedMb, maxMb) = readHeapMb()
            val effLayoutMs = if (layoutDoneMs >= composeCommitMs) layoutDoneMs else composeCommitMs
            val effDrawMs = if (drawDoneMs >= effLayoutMs) drawDoneMs else effLayoutMs
            val subcomposeLayoutPhaseMs = (effLayoutMs - composeCommitMs).coerceAtLeast(0L)
            val drawPhaseMs = (effDrawMs - effLayoutMs).coerceAtLeast(0L)
            val renderSyncWaitMs = (firstFrameMs - effDrawMs).coerceAtLeast(0L)
            recordInitMilestone(
                "Dựng lần đầu [${scenario.code}]: commit=${composeCommitMs}ms, subcompose+layout=+${subcomposeLayoutPhaseMs}ms (tổng ${effLayoutMs}ms), draw=+${drawPhaseMs}ms (tổng ${effDrawMs}ms), đồng bộ GPU/khung kế=+${renderSyncWaitMs}ms -> khung đầu=${firstFrameMs}ms | RAM=${usedMb}/${maxMb}MB"
            )
        }
    }

    private val endOfFrameRunnable = Runnable {
        frameEndScheduled = false
        if (!isForegroundActive) return@Runnable
        val anchorNanos = currentFrameAnchorNanos
        if (anchorNanos == 0L) return@Runnable
        val workNs = (System.nanoTime() - anchorNanos).coerceAtLeast(0L)
        // Bỏ qua sai số nếu tiến trình vừa ngủ đông/tạm dừng quá 2.5 giây
        if (workNs <= 2_500_000_000L) {
            windowFrames++
            if (workNs > windowMaxFrameNanos) {
                windowMaxFrameNanos = workNs
            }
            if (workNs >= JANK_FRAME_THRESHOLD_NS) {
                windowJankFrames++
            }
            if (workNs >= SLOW_FRAME_THRESHOLD_NS) {
                windowSlowFrames++
            }
            if (workNs >= FROZEN_FRAME_THRESHOLD_NS) {
                windowFrozenFrames++
                val nowMs = nowElapsedMs()
                if (nowMs - lastFrozenLogElapsedMs >= FROZEN_LOG_COOLDOWN_MS) {
                    lastFrozenLogElapsedMs = nowMs
                    val frameMs = workNs / 1_000_000.0
                    val scenCode = currentScenario.code
                    val recomp = activeWindowRecompositions[currentScenario]?.get() ?: 0
                    val deltaRecomp = (recomp - lastFrozenLoggedRecomp).coerceAtLeast(0)
                    lastFrozenLoggedRecomp = recomp
                    AppLogger.logLazy("PERF", AppLogLevel.WARN) {
                        val (usedMb, maxMb) = readHeapMb()
                        String.format(
                            Locale.US,
                            "Khựng luồng chính tại [%s]: %.1f ms (>100ms) | vẽ lại=+%d (tích lũy=%d) | RAM=%d/%d MB",
                            scenCode,
                            frameMs,
                            deltaRecomp,
                            recomp,
                            usedMb,
                            maxMb
                        )
                    }
                }
            }
        }
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isForegroundActive) return
            val nowNano = System.nanoTime()
            currentFrameAnchorNanos = if (frameTimeNanos in (nowNano - 10_000_000_000L)..nowNano) {
                frameTimeNanos
            } else {
                nowNano
            }
            if (!frameEndScheduled) {
                frameEndScheduled = true
                // Chạy ngay sau khi Choreographer.doFrame hoàn tất toàn bộ INPUT + ANIMATION + TRAVERSAL (Compose layout/draw) + COMMIT,
                // giúp đo chính xác thời gian luồng chính thực sự bận trong khung hình mà không tính nhầm khoảng nghỉ VRR 30Hz/10Hz khi màn hình đứng yên.
                mainHandler.postAtFrontOfQueue(endOfFrameRunnable)
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
            resetWindowLocked(nowElapsedMs(), preserveFrameAnchor = false)
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
            mainHandler.removeCallbacks(endOfFrameRunnable)
            frameEndScheduled = false
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
            resetWindowLocked(nowMs, preserveFrameAnchor = isForegroundActive)
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

    private fun resetWindowLocked(nowMs: Long, preserveFrameAnchor: Boolean) {
        windowStartElapsedMs = nowMs
        currentFrameAnchorNanos = if (preserveFrameAnchor) System.nanoTime() else 0L
        windowFrames = 0L
        windowJankFrames = 0L
        windowSlowFrames = 0L
        windowFrozenFrames = 0L
        windowMaxFrameNanos = 0L
        lastFrozenLoggedRecomp = 0
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
        val isTabTransition = reason.startsWith("chuyển sang ")

        if (!isTabTransition && frames < MIN_WINDOW_FRAMES && durationMs < MIN_WINDOW_DURATION_MS && frozen == 0L) {
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
     * Các mốc [PERF-INIT] được lưu trong bộ đệm riêng nên không bao giờ bị mất khi [PERF] xoay vòng 40 dòng.
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

        val allLoggerEntries = AppLogger.logs.value
        val initLines = LinkedHashSet<String>()
        for (line in initMilestoneHistory) {
            initLines.add(line)
        }
        for (entry in allLoggerEntries) {
            if (entry.tag == "PERF-INIT") {
                initLines.add("[${entry.timestamp}] [${entry.tag}] [${entry.level.name}] ${entry.message}")
            }
        }
        if (initLines.isNotEmpty()) {
            sb.appendLine(
                if (isEn) "--- Startup & First-Render Milestones [PERF-INIT] (${initLines.size}) ---"
                else "--- Mốc khởi động & dựng lần đầu [PERF-INIT] (${initLines.size}) ---"
            )
            for (line in initLines) {
                sb.appendLine(line)
            }
        }

        val perfLogs = allLoggerEntries.filter { it.tag == "PERF" }
        if (perfLogs.isNotEmpty()) {
            sb.appendLine(
                if (isEn) "--- Recent [PERF] Events (${perfLogs.size}) ---"
                else "--- Dòng nhật ký [PERF] gần đây (${perfLogs.size}) ---"
            )
            for (entry in perfLogs) {
                sb.appendLine("[${entry.timestamp}] [${entry.tag}] [${entry.level.name}] ${entry.message}")
            }
        }
        return sb.toString().trimEnd()
    }
}
