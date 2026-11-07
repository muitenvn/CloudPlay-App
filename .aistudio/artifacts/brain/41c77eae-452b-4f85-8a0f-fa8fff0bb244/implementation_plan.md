# Kế Hoạch Đo Đạc `[PERF]` & Tối Ưu Hóa `ServersScreen` (Đã Chốt Theo Phản Hồi #21 Của Claude)

Kế hoạch này đã được chốt theo phản hồi #21 của Claude: thực hiện **Hạng mục 1 (Bộ đo `[PERF]` giới hạn dòng)** và **Hạng mục 2 (`ServersScreen.kt` kèm bảo toàn cập nhật `streamTelemetry.rttMs` trực tiếp khi đang chơi)**, đồng thời **bỏ Hạng mục 3 (`CloudBrowserScreen.kt` `onPageStarted`)** để giữ nguyên 100% cơ chế đồng bộ 10 nhóm cài đặt trong `XboxBridge.kt` (dòng `276–288`).

## User Review & Critical Decisions

> [!IMPORTANT]
> **Xác nhận theo phản hồi #21 của Claude:**
> 1. **Đồng ý bỏ Hạng mục 3 (`CloudBrowserScreen.kt` `onPageStarted`)**: Qua đối chiếu `CloudBrowserScreen.kt` (dòng `598–607`) và `XboxBridge.kt` (dòng `276–288`), `addDocumentStartJavaScript` chỉ đăng ký một lần lúc khởi tạo `WebView` (`factory`), trong khi nhánh `if (window.__cloudPlayInjected)` ở đầu `currentInjectionScript` chịu trách nhiệm đồng bộ cả 10 nhóm cấu hình mới nhất (`__updateRegionAndLocale`, `__setClarityPreset`, `__toggleClarityBoost`, `__toggleXCloudTranslation`, `__setXCloudTranslationConfig`, `__toggleStreamStatsOverlay`, `__updateNetworkProfile`, `__updateGraphicsTuning`, `__setVirtualGamepadEnabled`, `__updatePhysicalControllerConfig`, `__pauseTranslator`). Do đó giữ nguyên hoàn toàn cơ chế tiêm script ở `CloudBrowserScreen.kt`.
> 2. **Bảo toàn cập nhật `streamTelemetry.rttMs` trực tiếp trong `ServersScreen.kt` (dòng `581–583` và `1205–1207`)**:
>    - Bảng tiền tính toán (`remember`) chỉ lưu `basePing` (từ `VpnManager.getEffectiveServerPing`), `unlockStatus`, `distanceKm`, `isTooFarForAutoSelect`.
>    - Khi `isGameActiveOnThisServer == true` (đang phát stream trên máy chủ đó), giá trị `ping` truyền vào `ServerCard` (dòng `1205–1207`) và thanh trạng thái đầu trang (dòng `581–583`) vẫn đọc trực tiếp từ `streamTelemetry.rttMs` theo thời gian thực, hoàn toàn không bị đứng yên.

---

## 1. Overview & Core Concept

- **Mục tiêu**:
  1. **Hạng mục 1 (`[PERF]`)**: Thêm công cụ đo nhẹ chỉ-đọc (`PerformanceMonitor`) ghi vào `AppLogger` (tag `PERF`) khi chuyển màn hình hoặc khi có khung hình đơ `> 100 ms`, giới hạn tối đa `40` dòng `[PERF]` để không làm mất các dòng log `VPN`/`PROBE`/`STREAM`.
  2. **Hạng mục 2 (`ServersScreen.kt`)**: Loại bỏ các phép tính khoảng cách lượng giác Haversine $O(N \times M)$ lặp lại trên từng dòng khi cuộn `LazyColumn` và các tính toán không `remember`, đồng thời giữ nguyên cập nhật trực tiếp `streamTelemetry.rttMs` khi đang trong phiên stream.

---

## 2. Chi Tiết Kỹ Thuật Từng Hạng Mục (Kèm `file:dòng` Xác Thực)

### Hạng mục 1 — Bộ đo hiệu năng chỉ-đọc `PerformanceMonitor` & Giới hạn dòng `[PERF]`
1. **Tạo mới `app/src/main/java/com/example/data/PerformanceMonitor.kt`**:
   - Dùng `Choreographer.FrameCallback` chỉ chạy khi app ở tiền cảnh (`MainActivity.onStart` → `MainActivity.onStop`), không cấp phát đối tượng trong mỗi khung hình `doFrame`.
   - Theo dõi 6 ngữ cảnh (`PerfScenario`): `HOME`, `LIBRARY`, `SERVERS`, `SETTINGS`, `WEBVIEW_BROWSE`, `STREAM_ACTIVE`.
   - Chỉ ghi vào `AppLogger` (tag `"PERF"`) khi:
     - **Đổi ngữ cảnh/màn hình** (nếu ngữ cảnh vừa kết thúc kéo dài `>= 500 ms` hoặc có `>= 10` khung hình): ghi 1 dòng tóm tắt (`fpsAvg`, số khung chậm `>16.7ms`, số khung giật `>33.3ms`, số khung đơ `>100ms`, `maxFrameMs`, số lần recomposition, mức RAM `usedMb/maxMb`).
     - **Khung hình đơ `> 100 ms`**: ghi cảnh báo ngay khi phát hiện (có giãn cách chống lặp tối thiểu `5.000 ms` mỗi lần).
   - Hàm `buildShortPerfReport(isEn: Boolean): String` xuất báo cáo ngắn gọn để chủ dự án sao chép gửi lại.
2. **`app/src/main/java/com/example/data/AppLogger.kt` (Tổng 556 dòng)**:
   - Tại dòng `39–43` và `347–360`: Thêm `const val MAX_PERF_LOGS = 40`. Khi ghi bản ghi có `cleanTag == "PERF"`, nếu trong `_logs` đã có từ `40` dòng `"PERF"` trở lên thì chỉ loại bỏ dòng `"PERF"` cũ nhất, giữ an toàn tuyệt đối cho các dòng log hệ thống khác.
3. **`app/src/main/java/com/example/ui/components/AppLogDialog.kt` (Tổng 353 dòng)**:
   - Tại dòng `77–85` và `140–145`: Thêm bộ lọc `"PERF"` (`"PERF" to if (isEn) "Perf [PERF]" else "Hiệu năng [PERF]"`).
   - Tại dòng `269–275` (nút `copy_app_log_btn`): Khi đang chọn bộ lọc `"PERF"`, sao chép bản báo cáo tổng hợp `PerformanceMonitor.buildShortPerfReport(isEn)` kèm danh sách dòng `[PERF]`.
4. **Gắn điểm đo chỉ-đọc vào vòng đời và màn hình**:
   - `app/src/main/java/com/example/MainActivity.kt` (dòng `139–149`): Gọi `PerformanceMonitor.onAppStart()` trong `onStart()` và `PerformanceMonitor.onAppStop()` trong `onStop()`.
   - `app/src/main/java/com/example/ui/DashboardScreen.kt` (dòng `566–619`): Ghi nhận đổi tab (`HOME`, `LIBRARY`, `SERVERS`, `SETTINGS`) và đếm recomposition qua `SideEffect`.
   - `app/src/main/java/com/example/ui/CloudBrowserScreen.kt` (dòng `410–418`): Chỉ gắn `LaunchedEffect(streamUiState.isStreamConnected)` và `SideEffect` chỉ-đọc để chuyển ngữ cảnh đo giữa `WEBVIEW_BROWSE` và `STREAM_ACTIVE` (không sửa bất kỳ logic WebView hay `evaluateJavascript` nào).

### Hạng mục 2 — `[ĐÃ XÁC NHẬN]` Tối ưu hóa `ServersScreen.kt` (Tổng 1285 dòng)
1. **Dòng `116–117` (Ngắt recomposition thừa khi không phát stream, giữ nguyên cập nhật trực tiếp khi đang phát stream)**:
   - Khi `networkStatus.isLowLatencyStreamActive == false`, dùng `StateFlow` tĩnh mặc định để không đánh thức `ServersScreen`.
   - Khi `networkStatus.isLowLatencyStreamActive == true`, thu thập trực tiếp `NetworkOptimizer.telemetry` như hiện tại để `streamTelemetry.rttMs` cập nhật theo thời gian thực ở cả thanh trạng thái đầu trang (dòng `581–583`, `605–611`) và thẻ máy chủ đang phát (dòng `1205–1207`).
2. **Dòng `204` (`allServersSnapshot`)**:
   - Đưa `allServers.toList()` vào `remember(allServers.size, vpnState.customServers, vpnState.liveVerifiedServers)` để không cấp phát `List` mới ở mỗi lần recomposition.
3. **Dòng `1023–1029` (Số đếm máy chủ theo quốc gia trong `LazyRow`)**:
   - Đưa phép đếm `jpCount`, `krCount`, `usCount`, `customCount` vào `remember(allServersSnapshot)`.
4. **Dòng `1181–1251` (`recommendedServerId` và bảng tiền tính toán cho `items(filteredServers)`)**:
   - Bọc `recommendedServerId` trong `remember(...)`.
   - Tiền tính toán bảng `precomputedRows: Map<String, ServerRowPrecomputed>` trong `remember(filteredServers, allServersSnapshot, serverPings, serverMeasurements, vpnState.unlockRecords, vpnState.detectedGeoIp, vpnState.engine, recentSessions)` chứa `basePing`, `unlockStatus`, `distanceKm`, `isTooFarForAutoSelect`.
   - Bên trong `items(filteredServers)` (dòng `1205–1214`), giữ nguyên nhánh:
     ```kotlin
     val ping = if (isGameActiveOnThisServer) {
         if (streamTelemetry.rttMs > 0) streamTelemetry.rttMs else -1
     } else {
         rowMeta?.basePing ?: VpnManager.getEffectiveServerPing(...)
     }
     ```
     đảm bảo khi đang chơi game, `streamTelemetry.rttMs` luôn cập nhật trực tiếp.

---

## 3. Technical Architecture & Data Flow

```
┌──────────────────────────────────────────────────────────────────────────┐
│                 PerformanceMonitor (Chỉ-đọc • Tag [PERF])                │
│  • Choreographer.FrameCallback (MainActivity onStart .. onStop)          │
│  • Ghi vào AppLogger khi đổi màn hình hoặc khi khung hình đơ > 100 ms    │
│  • AppLogger giới hạn tối đa MAX_PERF_LOGS = 40 dòng [PERF]              │
└───────────────────────────────────┬──────────────────────────────────────┘
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                 Tối Ưu Hóa ServersScreen.kt (Hạng Mục 2)                 │
│  • Tiền tính toán basePing, unlockStatus, distanceKm, isTooFar trong     │
│    remember(...) -> Cuộn LazyColumn tra cứu O(1), bỏ lặp Haversine O(N*M)│
│  • Khi isGameActiveOnThisServer == true: vẫn đọc trực tiếp               │
│    streamTelemetry.rttMs (dòng 581–583 & 1205–1207) theo thời gian thực  │
└──────────────────────────────────────────────────────────────────────────┘
```
