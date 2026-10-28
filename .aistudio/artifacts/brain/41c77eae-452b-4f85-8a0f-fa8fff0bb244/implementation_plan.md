# Kế hoạch Thực thi Gia cố (Hardened Implementation Plan): Đo Ping Thực tế, Chẩn đoán Lag & Kiểm toán Bảo mật (A1 → D3)

Bản kế hoạch này tích hợp đầy đủ **Giai đoạn 1 (Khảo sát Repository thực tế & Bảng đối chiếu A1–D3)** và các yêu cầu gia cố kỹ thuật nghiêm ngặt cho **Nhóm A (A1–A8), Nhóm B (B1–B3), Nhóm C (C1–C4), Nhóm D (D1–D3)** cùng **13 bài kiểm thử nghiệm thu bắt buộc**.

---

## Quyết định đã xác nhận & Nguyên tắc Gia cố Bắt buộc

> [!IMPORTANT]
> **10 Nguyên tắc Gia cố Kỹ thuật được áp dụng xuyên suốt:**
> 1. **100% Số đo thực tế**: Xóa bỏ hoàn toàn `calibrateMeasuredRttWithPhysicalDistance`, `computeMinimumPhysicalRttMs`, `normalizeKernelSynRetransmit`. Không cộng, trừ, modulo hay nội suy theo khoảng cách địa lý.
> 2. **Thống kê trung thực (Median & Độ biến động mẫu)**: Không dùng `min()` đơn thuần để che giấu mẫu xấu; lưu và hiển thị **giá trị trung vị (median)** từ các mẫu hợp lệ (kèm `minRttMs`, `maxRttMs` và cờ cảnh báo biến động khi chênh lệch mẫu lớn).
> 3. **Phân biệt rõ ý nghĩa từng loại độ trễ & cổng đo**:
>    - Tách biệt rõ **TCP connect latency (cổng chính)**, **TCP connect latency (cổng phụ `:443` / `:80`)**, **ước lượng từ trạm Globalping**, **WebRTC RTT trong phiên stream**, và **độ trễ thao tác (Input delay)**.
>    - Nếu cổng chính của máy chủ không phản hồi nhưng cổng phụ (`:443` hoặc `:80`) phản hồi, lưu trạng thái riêng (`REACHABLE_FALLBACK_PORT`) và hiển thị rõ ràng hai trạng thái (ví dụ: `63 ms (:443 • Cổng chính không phản hồi)`), không âm thầm coi kết quả cổng 443/80 là cổng proxy chính và không dùng kết quả cổng phụ để đánh tráo xếp hạng cổng chính.
> 4. **Giới hạn kết nối vật lý ở tầng Socket (`Semaphore(5)`) & Epoch chống ghi đè**:
>    - Giới hạn đồng thời 4–6 socket TCP thực sự đang mở trên toàn hệ thống (không chỉ giới hạn số coroutine máy chủ).
>    - Quản lý `measurementGenerationId` (Epoch ID): khi bấm `"Đo ping"`, tăng `generationId`, hủy đợt đo cũ, đóng socket ngay khi hủy/timeout và chặn mọi tác vụ cũ ghi đè kết quả đợt mới.
> 5. **Không giả định Việt Nam khi thiếu GeoIP**: Khi chưa có dữ liệu GeoIP thực (`detectedGeoIp == null`), không mặc định `"VN"`, tự động bỏ qua trạm Globalping và không áp đặt khoảng cách địa lý giả định.

---

## 1. Giai đoạn 1 — Khảo sát Repository & Bảng Đối chiếu A1–D3

| Mục | Thành phần / Hàm liên quan | Hiện trạng đã xác minh trong Repository | Thay đổi cần thực hiện | Kiểm thử nghiệm thu |
| :--- | :--- | :--- | :--- | :--- |
| **A1** | `PingTester.measureDirectLatency`, `measureServerLatency`; `RegionalProbeTester.calibrateMeasuredRttWithPhysicalDistance`, `computeMinimumPhysicalRttMs` | Đang gọi `calibrateMeasuredRttWithPhysicalDistance` ở dòng 43, 95 (`PingTester.kt`) và dòng 313, 400 (`RegionalProbeTester.kt`), cộng bù km cáp quang vào số đo thật. | Xóa hoàn toàn 2 hàm hiệu chuẩn vật lý và mọi lời gọi liên quan; trả về nguyên vẹn số đo thực tế. | Test 1: Số ping trả về giữ nguyên 100% giá trị đo, `grep` không còn hàm hiệu chuẩn. |
| **A2** | `PingTester.normalizeKernelSynRetransmit` | Đang trừ `960` khi `rawMs in 980..1450` và lấy `% 960` khi `> 1450` (dòng 107–114), đồng thời lấy `samples.minOrNull()`. | Xóa hoàn toàn `normalizeKernelSynRetransmit`. Tính và lưu **median** của các mẫu hợp lệ (kèm `minRttMs`, `maxRttMs`, `isUnstable` khi độ lệch mẫu cao hoặc `>= 1000ms`). | Test 1 & Test 3: Mẫu 1050ms giữ nguyên 1050ms (kèm cờ biến động), không bị trừ 960. |
| **A3** | `PingTester`, `ServerPingResult`, `VpnManager`, `ServersScreen` | Hiện chỉ lưu `Map<String, Int>` (`serverPings`), không lưu cổng thực tế phản hồi (`measuredPort`), phương pháp đo (`measurementType`), hay trạng thái cổng chính chết / cổng phụ sống. | Tạo model `EndpointPingMeasurement(rttMs, minRttMs, maxRttMs, measuredPort, configuredPort, measurementType, status, resolvedIp, timestampMs)`. Phân biệt rõ `PRIMARY_PORT_OK` và `FALLBACK_PORT_ONLY`. | Test 2: Cổng chính và cổng phụ (`:443`/`:80`) được lưu và hiển thị tách biệt, không đánh đồng. |
| **A4** | `RegionalProbeTester`, `AppSettings`, `SettingsScreen`, `ServersScreen` | Globalping đang được gọi tự động trong `measureServerLatency`, `resolveUserCoordinates` mặc định `"VN"` khi thiếu GeoIP, chưa có công tắc bật/tắt trong Cài đặt. | Thêm `enableGlobalpingProbe: Boolean = false` (mặc định **TẮT**), bỏ mặc định `"VN"` khi thiếu GeoIP, chỉ đo đơn lẻ cho máy chủ đang chọn, trần cục bộ 200 lượt/giờ (< 250 lượt/giờ miễn phí), xử lý HTTP 429, gắn nhãn rõ nguồn đo. | Test 8: Globalping tắt mặc định, không gọi mạng khi tắt hoặc khi thiếu GeoIP, xử lý đúng giới hạn lượt/giờ. |
| **A5** | `VpnManager.sortServersByUnlockAndPing`, `RegionalProbeTester.getNearestSupportedCountryTier` | Khi thiếu GeoIP vẫn lấy `"VN"` để tính tier; chưa tách bạch hoàn toàn trạng thái `UNTESTED_PING`, `TESTING`, `MEASURED_PRIMARY`, `MEASURED_FALLBACK_ONLY`, `UNREACHABLE`. | Khi thiếu GeoIP, trả về cùng tier `0` cho mọi quốc gia hỗ trợ. Khi có số đo thật, xếp theo trạng thái mở khóa (`UNLOCKED` > `UNTESTED`) → cổng chính phản hồi trước cổng phụ → số đo thực tế, không để khoảng cách địa lý đè lên số đo thật. | Test 1 & Test 2: Máy chủ có cổng chính sống & `UNLOCKED` luôn xếp trên; địa lý không ghi đè ping thật. |
| **A6** | `AppLogger.kt`, `AppLogDialog.kt` | `AppLogger.log` hiện ghi thẳng chuỗi `message` vào `_logs` (tối đa 350 dòng) mà chưa lọc cấu trúc/che dữ liệu nhạy cảm (`Cookie`, `Bearer`, `token`, `password`, email, URL auth). | Thêm bộ lọc bảo mật `sanitizeLogMessage()` ngay trước khi ghi vào bộ nhớ và Logcat; giới hạn cứng `<= 300 dòng` và `<= 64 KB`; hiển thị rõ nơi lưu (RAM cục bộ) và nút Xóa log. | Test 11: Chuỗi log chứa token, cookie, Bearer, password, email, URL query nhạy cảm đều bị che (`[REDACTED]`) trước khi lưu. |
| **A7** | Thư mục `public/` ở gốc dự án | Đã kiểm tra cây thư mục gốc: thư mục `public/` không còn tồn tại và không có tham chiếu trong `build.gradle.kts` hay `src/main`. | Xác nhận giữ sạch cây thư mục, không thêm lại `public/`. | Kiểm tra cây thư mục và build Gradle sạch. |
| **A8** | `PingTester`, `VpnManager`, `ServersScreen` | `testAllServers` hiện tạo coroutine cho toàn bộ danh sách cùng lúc; chưa có đo 2 bước, chưa có đo lười theo vùng hiển thị (`LazyListState`), chưa có cache 5 phút theo endpoint thực tế và chưa có `generationId` chống ghi đè. | Thiết kế đo 2 bước + đo lười (`LazyListState` + `snapshotFlow` + `debounce(250ms)`), giới hạn cứng `Semaphore(5)` ở mức socket thực tế đang mở, thời hạn mỗi đợt 5 giây, cache TTL 5 phút theo `ip:port`, quản lý `generationId`, xếp cuối/ẩn máy chủ `LiveProxyFetcher` chết. | Test 4, 5, 6, 7: Kiểm chứng giới hạn socket đồng thời `<= 5`, lazy loading chỉ đo mục hiển thị, TTL cache 5 phút và chống ghi đè giữa các đợt đo. |
| **B1** | `SessionHistoryRepository.kt`, `PlaySessionRecord`, `StreamController.kt` | `PlaySessionRecord` lưu 10 trường thật (`avgPingMs`, `pingSamples`, `jitterMs`, `jitterBufferDelayMs`, `framesDropped`, `freezeCount`, `totalFreezesDuration`, `avgDecodeMs`, `isDirectNetwork`, `isThermalHot`), chưa lưu `networkType` và tổng số khung hình thực tế đã giải mã (`framesDecoded`). | Bổ sung `networkType: String = ""` và `framesDecoded: Int = 0` tương thích ngược 100% với JSON cũ. Bản ghi cũ thiếu `networkType` giữ rỗng, không suy đoán. | Test 9: Đọc/ghi JSON phiên cũ (không có `networkType`/`framesDecoded`) và phiên mới hoạt động chính xác. |
| **B2** | `SessionHistoryScreen.kt`, `LagDiagnosticConfig` | Hiện mới có `SessionVerdictThresholds`, chưa có khối `"Vì sao có lag?"` tách biệt rõ dấu hiệu mạng vs. giải mã video vs. hiệu năng thiết bị, và đang dùng mẫu số khung hình ước tính mà chưa ghi rõ. | Tạo `LagDiagnosticConfig` và hàm `diagnoseSessionLag(session)`: dùng `framesDecoded` thực tế nếu có (hoặc ghi rõ mẫu số ước tính khi bản ghi cũ thiếu `framesDecoded`); phân biệt rõ mạng vs. giải mã vs. thiết bị; trả về `"Chưa đủ dữ liệu"` khi phiên quá ngắn hoặc thiếu số liệu. | Test 10: Kiểm thử từng nhánh chẩn đoán (mạng, giải mã + rớt khung hình, thiếu dữ liệu / mâu thuẫn). |
| **B3** | `SessionHistoryScreen.kt` | Độc lập với mạng. | Bảo đảm 100% tính toán của Nhóm B chỉ đọc từ `PlaySessionRecord` trong bộ nhớ, không gọi socket hay HTTP. | Kiểm tra tĩnh & unit test chạy offline 100%. |
| **C1** | `SessionHistoryScreen.kt`, `CloudBrowserScreen.kt` (HUD) | Đang dùng thuật ngữ hỗn hợp (`Ping TB`, `Jitter`, `Giải mã TB`, `Khung hình rớt`, `Đứng hình`). | Chuẩn hóa đồng bộ VI & EN: *Độ trễ mạng*, *Độ biến động mạng*, *Thời gian giải mã*, *Bộ đệm dữ liệu*, *Khung hình bị rớt*, *Đứng hình tạm thời*, *Chất lượng truyền tải*, *Độ trễ thao tác*. | Test 12: Kiểm tra chuỗi hiển thị VI/EN đúng chuẩn. |
| **C2** | `SessionHistoryScreen.kt` | Các ô chỉ số trong chi tiết phiên hiện tĩnh, bấm vào không có giải thích. | Cho phép chạm vào từng chỉ số để mở dòng giải thích ngắn gọn (`<= 1 câu`), chính xác về mặt kỹ thuật. | Kiểm tra tương tác chạm trên từng ô chỉ số. |
| **C3** | `SessionHistoryScreen.kt`, `CloudBrowserScreen.kt` | Chỉ đổi màu chữ, chưa kèm nhãn chữ trạng thái (Xanh / Vàng / Đỏ) cho người mù màu theo đúng ngưỡng riêng của từng chỉ số. | Hiển thị màu 3 mức (Xanh / Vàng / Đỏ) kèm nhãn chữ rõ ràng (`Tốt` / `Trung bình` / `Cao`) theo ngưỡng riêng của từng chỉ số. | Test 12: Kiểm tra hàm phân loại mức độ và nhãn chữ đi kèm. |
| **C4** | Toàn bộ `app/src/main/java` | Còn sót một số chuỗi hiển thị có `"0ms"`, `"ULTRA"`, `"Siêu"`, `"Zero-Lag"` trên UI. | Rà soát, chỉ sửa chuỗi hiển thị hứa quá trên giao diện, giữ nguyên tên class/biến kỹ thuật và báo cáo chính xác `file:dòng`. | Kiểm tra `grep` và đối chiếu danh sách `file:dòng`. |
| **D1** | `CloudBrowserScreen.kt`, `MainActivity.kt`, `StreamController.kt` | `ON_PAUSE` tạm dừng WebView khi không ở PiP hoặc khi không bật giữ kết nối nền. | Chỉ rà soát & báo cáo chi tiết hành vi vòng đời, ước lượng tiêu thụ dữ liệu/pin kèm giả định rõ ràng (không tự ý sửa code nhóm D). | Báo cáo kỹ thuật D1 trong báo cáo cuối. |
| **D2** | `XboxBridge.kt` (20 hàm `@JavascriptInterface`) | Cả 20 hàm đều gọi `if (!isCallerTrusted()) return` (kiểm tra `isXboxHost(currentNavigatedUrl)`). Cần phân tích sâu rủi ro race condition của `currentNavigatedUrl` và đầu vào JSON. | Chỉ rà soát & lập bảng kiểm toán chi tiết toàn bộ 20 hàm `@JavascriptInterface` (không tự ý thay đổi hợp đồng API). | Báo cáo kiểm toán bảo mật D2 trong báo cáo cuối. |
| **D3** | `ServerList.kt`, `LiveProxyFetcher.kt`, `GeoIpDetector.kt` | `DEFAULT_SERVERS` chỉ có `JP`, `KR`, `US`; `GeoIpDetector.SUPPORTED_COUNTRIES` có 28 mã quốc gia. | Chỉ rà soát & báo cáo điều kiện mở rộng nguồn máy chủ và cơ chế kiểm tra mở khóa thực tế ngoài JP/KR/US. | Báo cáo kỹ thuật D3 trong báo cáo cuối. |

---

## 2. Kiến trúc Kỹ thuật Chi tiết (Giai đoạn 2 → Giai đoạn 4)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│         LUỒNG ĐO PING ĐA CỔNG, GIỚI HẠN SOCKET & CACHE (NHÓM A1–A8)          │
├──────────────────────────────────────────────────────────────────────────────┤
│  Yêu cầu đo (Đợt đo generationId = N)                                        │
│   ├─► [Kiểm tra Cache theo Endpoint (TTL 5 phút)]                            │
│   │     └─ Khóa: "${ip}:${configuredPort}" -> Lưu tách biệt:                 │
│   │          • primaryRttMs (Cổng cấu hình chính)                            │
│   │          • fallbackRttMs + fallbackPort (:443 hoặc :80)                  │
│   │          • status: UNTESTED | TESTING | PRIMARY_OK | FALLBACK_ONLY |     │
│   │                    UNREACHABLE | EXPIRED                                 │
│   │                                                                          │
│   ├─► [Giới hạn Socket Mở Đồng thời Toàn cục: socketSemaphore = Semaphore(5)]│
│   │     ├─ Mỗi lần mở Socket().connect() phải xin 1 permit từ Semaphore(5)   │
│   │     ├─ Đóng socket tức thì trong khối use/finally khi timeout hoặc hủy   │
│   │     └─ Kiểm tra generationId == activeGenerationId trước khi ghi kết quả │
│   │                                                                          │
│   └─► [Chiến lược 2 Bước + Lazy Viewport (Thời hạn mỗi bước ≤ 5 giây)]       │
│         ├─ Bước 1: Đo đại diện từ nhiều vùng -> Xếp thứ tự ưu tiên sơ bộ     │
│         ├─ Bước 2: Đo ứng viên tốt từ các vùng + máy chủ đang chọn + hiển thị│
│         └─ Cuộn màn hình (visibleItemsInfo + debounce 250ms): đo mục chưa đo │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Chi tiết triển khai Nhóm A (A1 → A8)
1. **Model `EndpointPingMeasurement` minh bạch**:
   - Lưu đầy đủ: `primaryRttMs: Int` (median của các mẫu cổng chính), `minRttMs: Int`, `maxRttMs: Int`, `fallbackRttMs: Int`, `measuredPort: Int`, `configuredPort: Int`, `resolvedIp: String`, `measurementType: PingMeasurementType` (`TCP_PRIMARY_PORT`, `TCP_FALLBACK_PORT`, `GLOBALPING_PROBE`), `status: ServerPingStatus` (`UNTESTED`, `TESTING`, `MEASURED_PRIMARY`, `MEASURED_FALLBACK_ONLY`, `UNREACHABLE`, `EXPIRED`), `isUnstable: Boolean` (khi `maxRttMs - minRttMs >= 80` hoặc `rttMs >= 1000`), và `timestampMs: Long`.
2. **Không nhầm lẫn cổng chính và cổng phụ (A3 & A5)**:
   - Đo cổng chính của máy chủ (`server.port`) trước tiên; chỉ khi cổng chính không phản hồi hoặc khi cần kiểm tra trạng thái sống của IP mới thử `:443` / `:80` (mỗi lần mở socket đều đi qua `socketSemaphore = Semaphore(5)` để tổng số socket TCP đang mở trên toàn app không bao giờ vượt quá 5).
   - Nếu cổng chính phản hồi (`MEASURED_PRIMARY`): dùng `primaryRttMs` để xếp hạng máy chủ.
   - Nếu cổng chính không phản hồi nhưng cổng phụ `:443`/`:80` phản hồi (`MEASURED_FALLBACK_ONLY`): hiển thị rõ trên thẻ máy chủ là **`"Cổng chính tắt • :443 phản hồi (X ms)"`** (EN: **`"Main port closed • :443 alive (X ms)"`**) và **xếp sau** các máy chủ có cổng chính hoạt động bình thường. Tuyệt đối không lấy kết quả cổng phụ `:443`/`:80` giả làm ping của cổng chính.
3. **Globalping an toàn & đúng hạn mức (A4)**:
   - Công tắc `enableGlobalpingProbe` trong `AppSettings` mặc định là **`false`**.
   - Chỉ khi người dùng bật công tắc, có `detectedGeoIp` thực tế (không mặc định `"VN"`) và yêu cầu đo cho máy chủ đang chọn mới gọi API Globalping, lưu riêng ở trường `probeEstimateRttMs` và hiển thị rõ nhãn: `"Ước lượng từ trạm <Thành phố> (không gồm đoạn từ điện thoại tới trạm)"`, không ghi đè lên số đo TCP trực tiếp từ điện thoại.

### Chi tiết triển khai Nhóm B (B1 → B3)
1. **Tương thích ngược `PlaySessionRecord`**:
   - Bổ sung `networkType: String = ""` và `framesDecoded: Int = 0` vào `PlaySessionRecord` (mặc định rỗng / `0` khi đọc JSON lịch sử cũ).
   - Khi tính tỷ lệ khung hình bị rớt:
     - Nếu `framesDecoded > 0`: tính tỷ lệ chính xác `framesDropped / (framesDecoded + framesDropped)` và ghi nhãn `"Thực tế"`.
     - Nếu bản ghi cũ có `framesDecoded == 0`: tính trên tổng khung hình ước tính `durationSeconds * avgFps` và ghi chú rõ `"(ước tính từ FPS trung bình)"`.
2. **Phân tách nguyên nhân trong `LagDiagnosticConfig`**:
   - Nếu `durationSeconds < 30` hoặc không có dữ liệu hợp lệ (`avgPingMs <= 0 && framesDropped == 0 && avgDecodeMs <= 0.0`): hiển thị **"Chưa đủ dữ liệu"** — *"Phiên chơi quá ngắn hoặc chưa thu thập đủ số liệu thống kê để kết luận nguyên nhân."*
   - Phân biệt rõ 3 nhóm dấu hiệu:
     - **Dấu hiệu độ trễ đường truyền (`avgPingMs > 120`)**: Mô tả dấu hiệu độ trễ mạng cao + gợi ý có điều kiện (chọn máy chủ gần hơn hoặc kiểm tra tắt VPN sau khi vào game nếu đang bật VPN).
     - **Dấu hiệu biến động mạng (`jitterMs > 20`)**: Mô tả dấu hiệu độ biến động gói tin cao + gợi ý kiểm tra tín hiệu Wi-Fi 5GHz hoặc giảm tải mạng nền.
     - **Dấu hiệu đứng hình / mất khung hình (`freezeCount > 0` hoặc `dropRatioPct > 2.0%`)**:
       - Nếu đi kèm `avgDecodeMs > 12.0`: nhận định có dấu hiệu **quá tải giải mã hình ảnh trên thiết bị** + gợi ý hạ độ phân giải hoặc đóng ứng dụng nền.
       - Nếu `avgDecodeMs <= 12.0` và `jitterMs > 20`: nhận định có dấu hiệu **mất gói / biến động đường truyền gây rớt khung hình**.
       - Nếu chỉ có `freezeCount > 0` đơn lẻ mà mạng và giải mã đều thấp: mô tả trung thực *"Ghi nhận hiện tượng đứng hình tạm thời nhưng độ trễ và thời gian giải mã trung bình vẫn ở mức bình thường; có thể do khựng cục bộ ngắn hạn."* (không quy chụp bừa cho mạng).

### Chi tiết triển khai Nhóm C (C1 → C4)
- Chuẩn hóa toàn bộ thuật ngữ VI/EN theo C1, thêm giải thích ngắn `<= 1 câu` khi chạm vào từng ô chỉ số trong `SessionHistoryScreen` (C2), áp dụng thang màu 3 trạng thái (Xanh / Vàng / Đỏ) kèm nhãn chữ tương ứng với ngưỡng riêng của từng chỉ số (C3), và làm sạch các chuỗi hiển thị hứa quá còn sót kèm bảng đối chiếu `file:dòng` (C4).

---

## 3. Danh sách 13 Bài Kiểm thử Nghiệm thu Bắt buộc (Giai đoạn 6)

Chúng tôi sẽ cài đặt và chạy thực tế toàn bộ 13 bài kiểm thử trong `ExampleRobolectricTest.kt` bằng lệnh `gradle :app:testDebugUnitTest`:
1. **Test 1**: Số ping trả về giữ nguyên giá trị mẫu đo (median thực tế), không bị trừ 960 hay cộng km địa lý.
2. **Test 2**: Cổng chính (`server.port`) và cổng phụ (`:443`/`:80`) được phân biệt rõ (`MEASURED_PRIMARY` vs `MEASURED_FALLBACK_ONLY`), không lấy ping cổng phụ đánh tráo cổng chính.
3. **Test 3**: Khi socket timeout, đóng socket an toàn và trả về trạng thái `UNREACHABLE` trong thời gian giới hạn mà không treo luồng gọi.
4. **Test 4**: `Semaphore(5)` giới hạn số lượng socket thực sự mở đồng thời không bao giờ vượt quá 5 ngay cả khi gọi đo 30 máy chủ cùng lúc.
5. **Test 5**: Cơ chế đo lười (Lazy loading) chỉ đo tập máy chủ trong vùng hiển thị được truyền vào, các máy chủ ngoài vùng giữ nguyên trạng thái `UNTESTED` (`"Chưa đo"`).
6. **Test 6**: Cache `ip:port` tuân thủ đúng TTL 5 phút, trả về kết quả từ cache khi còn hạn và không trả nhầm kết quả giữa hai endpoint khác cổng hoặc khác IP.
7. **Test 7**: Khi kích hoạt đợt đo mới (`generationId` tăng), kết quả trả về muộn của đợt đo cũ bị loại bỏ, không ghi đè lên đợt mới.
8. **Test 8**: Globalping mặc định tắt (`enableGlobalpingProbe == false`), không gọi khi thiếu GeoIP và tuân thủ giới hạn lượt đo/giờ.
9. **Test 9**: Bản ghi lịch sử phiên cũ (JSON không có `networkType` và `framesDecoded`) vẫn đọc/deserialization chính xác 100%.
10. **Test 10**: Hàm chẩn đoán lag trả về `"Chưa đủ dữ liệu"` khi thiếu dữ liệu/phiên quá ngắn và không quy chụp `freezeCount > 0` đơn lẻ là lỗi mạng khi `jitterMs` và `avgPingMs` đều tốt.
11. **Test 11**: `AppLogger` tự động che (`[REDACTED]`) các trường nhạy cảm (`Cookie`, `Bearer`, `token`, `password`, email, URL chứa khóa xác thực) ngay trước khi lưu vào bộ nhớ.
12. **Test 12**: Các chuỗi thuật ngữ giao diện (C1) và nhãn trạng thái màu 3 mức (C3) hiển thị chính xác ở cả tiếng Việt và tiếng Anh.
13. **Test 13**: Kiểm thử hồi quy tổng thể cho VPN sorting (`UNLOCKED` > `UNTESTED`), `XboxBridge.isTrustedHost`, và biên dịch toàn bộ ứng dụng (`compile_applet`).
