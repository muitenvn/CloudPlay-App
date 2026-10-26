# Tối Ưu Hóa Toàn Diện Hệ Thống Cloud Gaming Android WebView (Theo Báo Cáo Phân Tích Kỹ Thuật)

Triển khai đồng bộ cả 5 nhóm giải pháp kỹ thuật từ báo cáo phân tích chuyên sâu nhằm loại bỏ hiện tượng khựng hình do can thiệp sai chuẩn W3C trên luồng WebRTC, khóa tần số quét 60Hz chống lệch pha VSync và giảm nhiệt độ thiết bị, tự ngắt thông minh bộ điều tốc Anti-DRX theo nhiệt độ/vòng đời, tích hợp công cụ chẩn đoán 4 lớp **"Vì sao tôi bị lag?"**, chuẩn hóa toàn bộ hệ thống thuật ngữ trung thực và xác minh mã băm SHA-256 khi cập nhật APK.

## User Review & Critical Decisions

> [!IMPORTANT]
> Các quyết định sản phẩm và kỹ thuật dưới đây đã được xác nhận trong Bước 1 và sẽ được áp dụng xuyên suốt toàn bộ hệ thống:

- **Confirmed Decision 1 — Phạm vi triển khai**: Áp dụng đầy đủ cả 5 nhóm tối ưu hóa kỹ thuật và giao diện trong báo cáo phân tích (chuẩn hóa WebRTC W3C, khóa 60Hz + Wi-Fi Low Latency + `bindProcessToNetwork`, chuẩn hóa nhãn trung thực + ngưỡng màu ITU-T G.1072, tự ngắt Anti-DRX theo nhiệt độ/trạng thái nghỉ + nhắc ngắt VPN khi vào game, và xác minh SHA-256 + siết chặt `@JavascriptInterface`).
- **Confirmed Decision 2 — Xử lý nút hỗ trợ & Chẩn đoán trên HUD**: Thay thế hoàn toàn cơ chế "Xả trễ tức thì" cũ bằng nút **"Đồng bộ lại khung hình"** (tuân thủ chuẩn W3C, không can thiệp `playbackRate` hay `currentTime`) và bổ sung mục **Chẩn đoán 4 lớp "Vì sao tôi bị lag?"** ngay trong bảng HUD mở rộng.
- **Confirmed Decision 3 — Cơ chế khóa tần số quét 60Hz**: Tự động khóa tần số quét màn hình ở mức **60Hz** (`preferredDisplayModeId`) ngay khi vào màn hình chơi game để khớp nhịp 60fps của WebRTC, và tự động khôi phục tần số quét mặc định của hệ thống khi thoát về màn hình chính.

---

## 1. Overview & Core Concept

- **What It Does**: Hệ thống tối ưu hóa chu trình truyền nhận và hiển thị luồng game đám mây trên Android WebView dựa trên các tiêu chuẩn W3C WebRTC, ITU-T G.1072 và đặc tả mạng di động 3GPP LTE, đồng thời cung cấp thông tin minh bạch, trung thực về tình trạng đường truyền và thiết bị.
- **Target Audience / Persona**: Người chơi Xbox Cloud Gaming tại Việt Nam kết nối tới máy chủ Nhật Bản qua mạng di động 4G/LTE hoặc Wi-Fi, cần độ ổn định khung hình cao, kiểm soát nhiệt độ/pin tốt và hiểu rõ nguyên nhân khi mạng biến động.
- **Key Value**:
  - Chấm dứt hoàn toàn hiện tượng khựng hình, cạn bộ đệm (buffer underrun) và méo tiếng do việc ép `playbackRate = 1.05` / `currentTime` trước đây gây ra.
  - Giảm 3–4°C nhiệt độ máy và loại bỏ hiện tượng giật vi mô (micro-stuttering) nhờ đồng bộ 60Hz VSync kết hợp `requestVideoFrameCallback()`.
  - Bảo vệ pin và ngăn bóp xung nhiệt (Thermal Throttling) nhờ cơ chế tự ngắt xung Anti-DRX khi nhiệt độ tăng hoặc khi tạm dừng/xuống nền.
  - Giúp người chơi tự xác định nguyên nhân gây trễ trong 3 giây qua bộ chẩn đoán 4 lớp (Thiết bị/Nhiệt độ, Mạng cục bộ, Tuyến cáp quốc tế, Trạng thái VPN).

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Vào phiên chơi & Nhắc ngắt VPN thông minh**: Khi luồng hình ảnh WebRTC bắt đầu giải mã thành công (`onStreamConnected`), nếu phát hiện VPN/Proxy vẫn đang bật, hệ thống hiển thị thanh thông báo nổi rõ ràng: *"Game đã tải thành công! Hãy ngắt VPN ngay để giảm độ trễ."*
  2. **Giám sát chất lượng theo chuẩn ITU-T G.1072 trên HUD**:
     - Thanh HUD hiển thị đèn trạng thái 3 màu tiêu chuẩn: **Xanh lá** (Ổn định), **Vàng** (Cảnh báo), **Đỏ** (Kém) kèm theo cấu trúc thông báo tự động: `[1 câu xác định nguyên nhân] + [1 câu gợi ý giải pháp]`.
  3. **Đồng bộ lại khung hình (Thay cho "Xả trễ tức thì")**:
     - Khi người chơi bấm nút **"Đồng bộ lại khung hình"** trên HUD, hệ thống thực hiện đồng bộ lại bộ đệm `jitterBufferTarget` an toàn cho cả hai kênh Audio và Video theo mức biến động mạng thực tế (`jitterMs`) và kích hoạt đồng bộ lại nhịp vẽ khung hình mà tuyệt đối không chạm vào `playbackRate` hay `currentTime`.
  4. **Chẩn đoán nhanh 4 lớp "Vì sao tôi bị lag?"**:
     - Khi người chơi nhấn nút **"Vì sao tôi bị lag?"** trên HUD, hệ thống thực thi kiểm tra 4 lớp (Môi trường nhiệt độ & tiết kiệm pin thiết bị, Kết nối cục bộ Wi-Fi/4G, Tuyến cáp quốc tế sang Nhật, và Trạng thái VPN) rồi hiển thị bảng kết quả trực quan kèm hướng khắc phục ngay lập tức.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Phong cách **Utilitarian Dark** đặc trưng của bảng điều khiển kỹ thuật phòng máy (nền mờ `#CC0E1318`, viền mảnh `#26FFFFFF`), gọn gàng, không che khuất tầm nhìn trong trận.
  - *Color Palette & Mood*:
    - Trạng thái **Tốt (Xanh lá `#107C10` / `#2ECC71`)**: Ping `< 80ms`, Jitter `< 8ms`, Giải mã `< 8ms`, Rớt khung hình `< 0.5%`.
    - Trạng thái **Cảnh báo (Vàng `#FFB300`)**: Ping `80–120ms`, Jitter `8–20ms`, Giải mã `8–16ms`, Rớt khung hình `0.5–2.0%`.
    - Trạng thái **Báo động (Đỏ `#FF4D4F`)**: Ping `> 120ms`, Jitter `> 20ms`, Giải mã `> 16.6ms`, Rớt khung hình `> 2.0%`.
  - *Typography & Honest Terminology*:
    - Thay thế toàn bộ các cụm từ quảng cáo phóng đại bằng thuật ngữ kỹ thuật trung thực:
      - `"4G Siêu Nhạy"` ➔ **`"Ưu tiên độ nhạy 4G"`** / **`"Chế độ phản hồi nhanh (4G)"`**
      - `"Xả trễ tức thì"` / `"Xả trễ"` ➔ **`"Đồng bộ lại khung hình"`** / **`"Khôi phục luồng hình ảnh"`**
      - `"Zero Lag"` / `"ZeroLagScreenRecorder"` (nhãn hiển thị) ➔ **`"Ghi màn hình tối ưu phần cứng"`**
      - `"Tối ưu hoàn hảo"`, `"Chống nghẽn"` ➔ **`"Tự động thích ứng mạng"`**, **`"Hạn chế nghẽn đường truyền"`**
      - `"Tải nhanh (qua máy chủ trung gian, nên kiểm tra mã)"` ➔ **`"Tải từ máy chủ phụ (Khuyến nghị đối chiếu mã SHA-256)"`**
  - *Touch Targets & Ergonomics*:
    - Mọi nút điều khiển tương tác đảm bảo vùng chạm tối thiểu `48dp x 48dp`, áp dụng bộ lọc vùng chết hình tròn (radial deadzone: vùng chết trong `10–12%`, vùng chết ngoài `90%`) và xung rung xúc giác ngắn `10–15ms`.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Loại bỏ hoàn toàn can thiệp `playbackRate` / `currentTime` và đồng bộ `jitterBufferTarget` kép (Audio + Video)**
  - *Chosen Approach*: Xóa toàn bộ các đoạn mã gán `vEl.playbackRate = 1.05` / `1.06` và `vEl.currentTime = ...` trong luồng tự động lẫn nút bấm thủ công; khi điều chỉnh `jitterBufferTarget` và `playoutDelayHint`, luôn áp dụng đồng thời cho cả bộ thu `video` và `audio` với mức chặn dưới bám sát độ biến động thực tế (`max(20ms, jitterMs * 1.8)` trên 4G) thay vì ép cứng 8–10ms.
  - *Why*: Tuân thủ đặc tả W3C Media Capture & Streams và cơ chế đồng bộ A/V của Chromium (NetEq + VCM), giúp loại bỏ triệt để hiện tượng cạn kiệt bộ đệm gây đứng hình và méo tiếng.
- **Decision 2: Đo độ trễ RTT độc quyền từ `candidate-pair.currentRoundTripTime` kết hợp điều tốc Multiplicative Decrease / Additive Increase (AIMD)**
  - *Chosen Approach*: Chỉ sử dụng `currentRoundTripTime` từ `selectedCandidatePair` của WebRTC (loại bỏ hoàn toàn mọi phép đo HTTP HEAD phụ tới tên miền web CDN). Khi phát hiện độ dốc trễ tăng (`rttTrendDelta >= 12ms`), lập tức hạ nhanh một bước lớn (Multiplicative Decrease) xuống dải `4.0–6.0 Mbps` để làm rỗng hàng đợi eNodeB, và chỉ tăng chậm từng bậc nhỏ (Additive Increase) kèm chu kỳ giữ ổn định (hysteresis) để tránh dao động liên tục (hunting effect).
  - *Why*: Phản ánh chính xác 100% độ trễ UDP thực tế tới máy chủ game tại Nhật Bản và xả nhanh hàng đợi trạm phát LTE trong 1–2 giây.
- **Decision 3: Khóa 60Hz VSync khi vào màn chơi và quản lý chặt vòng đời `onPause` / Nhiệt độ cho Anti-DRX**
  - *Chosen Approach*: Chọn `Display.Mode` có cùng độ phân giải vật lý và tần số quét `60Hz` khi vào màn chơi game; tự động tạm dừng luồng WebView, dừng xung Anti-DRX ngay khi `onPause()` (xuống nền/cuộc gọi), khi người chơi nghỉ tay quá 20 giây, hoặc khi nhiệt độ máy đạt ngưỡng `THERMAL_STATUS_MODERATE` trở lên.
  - *Why*: Triệt tiêu hiện tượng lệch pha 60fps trên màn 120Hz, tiết kiệm 15–20% pin, ngăn chặn tiêu tốn dữ liệu ngầm và chống bóp xung nhiệt.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        TẦNG HỆ ĐIỀU HÀNH ANDROID                           │
│  ┌──────────────────────────┐  ┌────────────────────────┐  ┌────────────┐  │
│  │ Khóa Màn Hình 60Hz VSync │  │ Wi-Fi Low Latency Lock │  │ Kiểm Nhiệt │  │
│  │ (preferredDisplayModeId) │  │ + bindProcessToNetwork │  │ & PowerMgr │  │
│  └────────────┬─────────────┘  └───────────┬────────────┘  └─────┬──────┘  │
└───────────────┼────────────────────────────┼─────────────────────┼─────────┘
                ▼                            ▼                     ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                     TẦNG ĐIỀU KHIỂN & TỐI ƯU MẠNG                          │
│  ┌───────────────────────────────────┐  ┌────────────────────────────────┐ │
│  │ Bộ Điều Tốc AIMD & Anti-DRX Pacer │  │ Bộ Chẩn Đoán 4 Lớp (3 Giây)    │ │
│  │ • Hạ nhanh 4–6 Mbps khi phình trễ │  │ 1. Nhiệt độ & Tiết kiệm pin    │ │
│  │ • Tự ngắt xung khi nóng / ngầm    │  │ 2. Sóng cục bộ (Wi-Fi/4G RSSI) │ │
│  │ • Đồng bộ đệm Audio+Video an toàn │  │ 3. Tuyến cáp quốc tế sang Nhật │ │
│  └─────────────────┬─────────────────┘  │ 4. Phát hiện VPN / Proxy bật   │ │
│                    │                    └───────────────┬────────────────┘ │
└────────────────────┼────────────────────────────────────┼──────────────────┘
                     ▼                                    ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                TẦNG GIAO DIỆN HUD & BẢO MẬT CẬP NHẬT APK                   │
│  ┌───────────────────────────────────┐  ┌────────────────────────────────┐ │
│  │ Thanh HUD Chuẩn ITU-T G.1072      │  │ Bảo Mật WebView & SHA-256      │ │
│  │ • Đèn 3 màu: Xanh / Vàng / Đỏ     │  │ • Kiểm tra chặt origin xbox.com│ │
│  │ • [Nguyên nhân] + [Giải pháp]     │  │ • Tạm dừng WebView khi onPause │ │
│  │ • Nút "Đồng bộ lại khung hình"    │  │ • Xác minh SHA-256 file APK    │ │
│  └───────────────────────────────────┘  └────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────────┘
```

### Interactive Component & State Mapping

1. **Chu trình Khóa 60Hz & Vòng đời WebView**:
   - Khi mở màn chơi game: Chọn `Display.Mode` khớp độ phân giải hiện tại với tần số quét gần `60.0Hz` nhất (`59.0f..61.0f`), kích hoạt `WIFI_MODE_FULL_LOW_LATENCY` (trên Wi-Fi) và gọi `ConnectivityManager.bindProcessToNetwork(activeNetwork)` (khi không bật VPN).
   - Khi ứng dụng chuyển xuống nền (`onPause`): Gọi `webView.onPause()`, `webView.pauseTimers()` và tạm ngắt bộ phát xung Anti-DRX để không tiêu thụ dữ liệu 4G ngầm; khôi phục lại khi `onResume()`.
2. **Chu trình Đồng bộ lại khung hình & Điều tốc WebRTC**:
   - Xóa hoàn toàn mọi lệnh gán `playbackRate` và `currentTime` trên thẻ `<video>`.
   - Nút **"Đồng bộ lại khung hình"** tính toán ngưỡng bộ đệm an toàn dựa trên `jitterMs` hiện tại, cập nhật đồng thời cho cả `RTCRtpReceiver` của `video` và `audio`, đồng thời đồng bộ lại khung hình hiển thị qua `requestVideoFrameCallback()`.
   - Đo RTT độc quyền từ `candidate-pair.currentRoundTripTime` của cặp kết nối đang hoạt động (`selectedCandidatePairId` / `nominated`).
3. **Chu trình Chẩn đoán 4 lớp "Vì sao tôi bị lag?"**:
   - Thu thập tức thời 4 lớp dữ liệu thực tế:
     1. *Lớp Thiết bị*: Trạng thái nhiệt độ (`PowerManager.currentThermalStatus`), chế độ tiết kiệm pin (`isPowerSaveMode`), và thời gian giải mã phần cứng (`decodeTimeMs`).
     2. *Lớp Mạng cục bộ*: Loại mạng vật lý (Wi-Fi 2.4GHz/5GHz/6GHz kèm `RSSI dBm` hoặc 4G/LTE/3G kèm băng thông downlink và `jitterMs`).
     3. *Lớp Tuyến quốc tế*: So sánh `rttMs` thực tế từ WebRTC sang máy chủ Nhật Bản để phát hiện nghẽn cáp biển hoặc bóp băng thông giờ cao điểm (`20:00–23:00`).
     4. *Lớp VPN/Định tuyến*: Kiểm tra cờ `TRANSPORT_VPN` / trạng thái Proxy đang bật trong khi stream đã kết nối để nhắc tắt VPN ngay.
4. **Chu trình Bảo mật `@JavascriptInterface` & Xác minh SHA-256 APK**:
   - Kiểm tra nghiêm ngặt nguồn gọi `@JavascriptInterface` chỉ chấp nhận giao thức `https` trên tên miền chính thức `xbox.com` / `*.xbox.com`.
   - Cung cấp bộ kiểm tra tính toàn vẹn tệp APK tải về bằng thuật toán băm mật mã `SHA-256`, đối chiếu khớp với mã SHA-256 công bố từ bản phát hành GitHub trước khi cho phép mở trình cài đặt hệ thống `PackageInstaller`.
