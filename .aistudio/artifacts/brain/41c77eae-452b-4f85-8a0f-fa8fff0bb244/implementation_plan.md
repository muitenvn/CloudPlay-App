# Khắc Phục Giật Lắc Luồng Game & Đo Chính Xác Chỉ Số WebRTC

Kế hoạch điều tra và xử lý triệt để nguyên nhân gây giật/lắc luồng game trên máy yếu và máy chủ xa (như Tokyo, ping 94–267ms), khôi phục số liệu giải mã thực tế từ WebRTC và bổ sung bộ đo Rớt khung hình, Đứng hình, Jitter và Độ trễ bộ đệm.

## Quyết Định Đã Xác Nhận

> [!IMPORTANT]
> Các quyết định kỹ thuật và phản biện dưới đây đã được thống nhất dựa trên bằng chứng đối chiếu mã nguồn:

- **Khôi phục số đo Giải mã thực tế & bỏ đổi tốc độ phát 1.02x**: Gỡ bỏ hoàn toàn công thức trừ ngược jitter và ép trần thời gian giải mã về ~9.1ms; trả về đúng thời gian giải mã phần cứng thực tế (`totalDecodeTime / framesDecoded`) để bộ điều tiết chất lượng tự động nhận biết chính xác khi máy yếu quá tải giải mã. Đồng thời bỏ việc bật/tắt `playbackRate = 1.02` mỗi 1.5 giây khi jitter cao.
- **Tắt `requestUnbufferedDispatch` cho cảm ứng màn hình**: Gỡ bỏ `requestUnbufferedDispatch` trong `onTouchEvent` để hệ điều hành Android gom sự kiện chạm theo nhịp VSync 60Hz (tránh đẩy 120–240 sự kiện/giây làm nghẽn CPU máy yếu khi đang giải mã video); chỉ giữ cho tay cầm vật lý và thêm dòng ghi log debug đếm số sự kiện cảm ứng mỗi giây.
- **Phản biện có bằng chứng cho mục (f) và (h)**:
  - **Mục (f) — Đo ping khi vào trận**: Giá trị `-1` trong bộ quản lý VPN không gây lỗi vì đã có điều kiện chặn `if (ping > 0)`, nhưng lỗi thực sự nằm ở nhánh dự phòng của cầu nối WebRTC gửi `rttMs = 0` khi `currentRoundTripTime` chưa kịp có mặt, làm bộ điều tiết hiểu nhầm `rttOverloaded = false`. Sẽ sửa bằng cách lấy thêm `totalRoundTripTime / responsesReceived` và giữ nguyên mẫu RTT hợp lệ gần nhất thay vì ghi đè `0`.
  - **Mục (h) — Ngưỡng tắt màn hình chờ 80%**: Giữ nguyên ngưỡng 80% kèm nút bỏ qua vì chỉ số này chỉ phản ánh tiến độ tải khung HTML ban đầu của trang SPA Xbox, trong khi màn hình chờ tàu vũ trụ của Xbox tự giữ cho đến khi thẻ `<video>` của luồng WebRTC phát hình.

---

## 1. Tổng Quan & Mục Tiêu Cốt Lõi

- **Vấn đề giải quyết**: Người chơi gặp tình trạng giật/khựng hình và hiện cảnh báo "Giải mã" + "Ping" của Xbox khi chơi Fortnite ở máy chủ Tokyo trên máy yếu, nhưng ứng dụng vẫn báo "Giải mã 9.1ms • Mát & Ổn định" do số liệu giải mã bị ép trần và bộ đệm khung hình bị hạ quá thấp (trần 22–32ms) kèm chu kỳ co giãn liên tục 1.5 giây.
- **Giá trị mang lại**:
  1. **Đo đúng nguyên nhân gây giật**: Thu thập đầy đủ `framesDropped`, `freezeCount`, `totalFreezesDuration`, `jitter`, và `jitterBufferDelay / jitterBufferEmittedCount` từ chuẩn WebRTC `getStats()`.
  2. **Khôi phục độ mượt luồng phát**: Trả lại trần bộ đệm 42–45ms, giữ bộ đệm ổn định có thời gian chờ hạ (cooldown), đặt bộ đệm ngay khi luồng video/audio bắt đầu, tính lại băng thông SDP theo tỷ lệ thu phóng và giới hạn khung hình, đồng thời loại bỏ hoàn toàn các tác vụ nền thừa của bộ quay màn hình khi không quay.

---

## 2. Trải Nghiệm Người Dùng & Giao Diện

- **Giữ nguyên bố cục giao diện**: Không thay đổi cấu trúc các màn hình hiện có ngoài việc hiển thị đúng một dòng thông tin chẩn đoán ngắn gọn trong hộp thoại Chi tiết phiên chơi.
- **Hiển thị trong Chi tiết phiên chơi**:
  - Bổ sung một dòng duy nhất hiển thị rõ ràng:
    `Khung hình rớt: X • Đứng hình: Y lần • Jitter: Z ms`
  - Nhãn trạng thái máy ("Giải mã chậm" / "Mát & Ổn định") phản ánh trung thực thời gian giải mã phần cứng thực tế.

---

## 3. Quyết Định Kỹ Thuật & Đánh Đổi (Bước 1 — Bảng Điều Tra)

- **Mục (a) — Trần bộ đệm & chu kỳ điều chỉnh EMA 1.5 giây**:
  - *Bằng chứng*: Trần bộ đệm trong JS bị khóa ở 32ms và trong bộ điều tiết khóa ở 22ms (Wi-Fi) / 28ms (4G); mặc định hạ xuống 14ms (4G) và 9ms (mạng tốt); mỗi nhịp 1.5 giây không có áp lực là lập tức trừ `-0.8ms` ngay lập tức.
  - *Giải pháp*: Khôi phục trần bộ đệm lên 42ms (Wi-Fi) và 45ms (4G), mặc định 18ms (4G) / 10–12ms (Wi-Fi). Thêm cơ chế giữ ổn định tối thiểu 9 giây liên tiếp không có áp lực mới giảm dần bộ đệm, và chỉ gửi lệnh cập nhật sang WebView khi mức chênh lệch `>= 1.5ms`.
- **Mục (b) — Khởi tạo bộ đệm tại sự kiện `'track'` cho Video & Audio**:
  - *Bằng chứng*: Sự kiện `pc.addEventListener('track')` chỉ lưu tham chiếu track mà không cài `playoutDelayHint` / `jitterBufferTarget`, và hàm cập nhật bộ đệm bỏ qua kênh âm thanh (`audio`).
  - *Giải pháp*: Cài đặt ngay `playoutDelayHint` và `jitterBufferTarget` ngay khi nhận sự kiện `'track'` cho cả `video` và `audio` để luồng ổn định ngay từ giây đầu tiên và không lệch tiếng.
- **Mục (c) — Băng thông SDP, giới hạn FPS và H.264 Profile**:
  - *Bằng chứng*: Hàm chỉnh sửa SDP đang gán cứng `b=AS` tối đa tới 20 Mbps mà không nhân hệ số tỷ lệ hiển thị (`renderScaleFactor`), không thêm `a=framerate`, và không ưu tiên H.264 profile nhẹ cho máy yếu.
  - *Giải pháp*: Tính lại băng thông SDP theo `renderScaleFactor`, giới hạn trần an toàn cho máy yếu/mạng di động, chèn `a=framerate` theo cấu hình FPS và ưu tiên H.264 Constrained Baseline/Main (`42e01f`/`42001f`) khi bật Chế độ máy yếu.
- **Mục (e) — Tách biệt công tắc tự tắt VPN khi vào trận**:
  - *Bằng chứng*: Việc kiểm tra gộp `smartVpnEnabled || autoBypassEnabled` làm VPN tự ngắt giữa phiên ngay cả khi người chơi chỉ muốn bật VPN thông minh mà tắt tự ngắt.
  - *Giải pháp*: Chỉ kích hoạt đếm ngược tự ngắt VPN khi cờ `autoBypassEnabled` thực sự bật.
- **Mục (g) — Tác vụ nền của máy quay màn hình khi không quay**:
  - *Bằng chứng*: Sự kiện `touchstart`/`pointerdown` mở khóa âm thanh chạy lặp lại ở mọi lần chạm cảm ứng, và các hàm `AudioNode.prototype.connect` / `HTMLMediaElement.prototype.play` tự động gắn nhánh thu âm (`GainNode` + `MediaStreamAudioDestinationNode`) ngay cả khi không quay.
  - *Giải pháp*: Dùng tùy chọn `{ once: true }` để gỡ bỏ sự kiện chạm ngay sau lần chạm đầu tiên, và chỉ gắn nhánh thu âm khi người chơi thực sự bấm nút Bắt đầu quay.

---

## 4. Kiến Trúc Kỹ Thuật & Luồng Dữ Liệu

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        WebRTC Inbound Video/Audio                        │
│  • Sự kiện 'track' -> Đặt playoutDelayHint & jitterBufferTarget (V + A)  │
│  • SDP Munging     -> Bitrate * renderScale + a=framerate + H.264 nhẹ    │
└───────────────────────────────────┬──────────────────────────────────────┘
                                    │ Chu kỳ getStats()
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                     Bộ Thu Thập Chỉ Số Thực Tế (JS)                      │
│  • decodeTimeMs thực (bỏ ép trần 9.1ms, bỏ đổi playbackRate 1.02x)       │
│  • framesDropped, freezeCount, totalFreezesDuration                      │
│  • jitter (ms), jitterBufferDelay / jitterBufferEmittedCount (ms)        │
│  • rttMs thực (giữ mẫu hợp lệ gần nhất, không ghi đè 0ms)                │
└─────────────────┬─────────────────────────────────────┬──────────────────┘
                  │                                     │
                  ▼                                     ▼
┌───────────────────────────────────┐ ┌────────────────────────────────────┐
│   Bộ Điều Tiết Chất Lượng & Đệm   │ │      Lưu Trữ Lịch Sử Phiên Chơi    │
│ • Trần đệm 42–45ms (4G 18ms)      │ │ • Lưu framesDropped, freezeCount,  │
│ • Giữ ổn định ≥9s mới giảm đệm    │ │   totalFreezesDuration, jitter,    │
│ • Hạ bậc ngay khi decode > 15ms   │ │   jitterBufferDelayMs              │
│   hoặc freezeCount / rớt hình tăng│ │ • Chi tiết phiên: "Khung hình rớt: │
└───────────────────────────────────┘ │   X • Đứng hình: Y lần • Jitter:   │
                                      │   Z ms"                            │
                                      └────────────────────────────────────┘
```

### Luồng Xử Lý Chi Tiết
1. **Đo & Truyền Chỉ Số (`StreamTelemetry`)**:
   - Mở rộng mô hình dữ liệu telemetry nhận từ WebRTC `getStats()` với 5 trường thực tế: tổng số khung hình rớt (`framesDropped`), số lần đứng hình (`freezeCount`), tổng thời gian đứng hình (`totalFreezesDurationSec`), độ chập chờn mạng (`jitterMs`), và độ trễ bộ đệm thực tế (`jitterBufferDelayMs`).
   - Sử dụng trực tiếp các chỉ số này trong bộ điều tiết chất lượng để phát hiện chính xác hiện tượng đứng hình hoặc quá tải giải mã trên máy yếu.
2. **Lưu Trữ & Hiển Thị Lịch Sử Phiên (`PlaySessionRecord`)**:
   - Ghi nhận giá trị cuối phiên của `framesDropped`, `freezeCount`, `totalFreezesDuration`, cùng giá trị trung bình của `jitterMs` và `jitterBufferDelayMs` vào dữ liệu phiên chơi.
   - Hiển thị dòng tóm tắt `"Khung hình rớt: X • Đứng hình: Y lần • Jitter: Z ms"` trong hộp thoại chi tiết phiên.
3. **Đo Tần Suất Cảm Ứng Debug**:
   - Đếm số lần `onTouchEvent` mỗi giây trên WebView và ghi ra `Log.d("CloudBrowserTouch", ...)` mỗi giây có thao tác chạm (chỉ ghi log hệ thống, không hiển thị lên giao diện).
