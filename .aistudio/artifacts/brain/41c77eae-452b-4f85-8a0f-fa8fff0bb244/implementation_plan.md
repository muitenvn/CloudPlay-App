# Kế Hoạch Thiết Kế & Khắc Phục Triệt Để Lỗi Cảnh Báo "Decode" Trên Xbox Cloud Gaming

Bản kế hoạch này tổng hợp kết quả rà soát chuyên sâu toàn bộ luồng giải mã video WebRTC, chỉ ra 6 nguyên nhân gốc rễ (bao gồm 2 lỗi kỹ thuật nghiêm trọng mới phát hiện khiến các bản vá trước chưa phát huy tác dụng) và đưa ra lộ trình thiết kế theo từng bước để hạ thời gian giải mã phần cứng xuống mức `1.5ms – 4.0ms`, loại bỏ hiện tượng báo lỗi **Decode** liên tục.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Giải thích ngắn gọn (bằng tiếng Việt có dấu, dễ hiểu) cho 3 thiết lập tối ưu được chọn mặc định trong kế hoạch này:**
> 1. **Chế độ giải mã video siêu tốc (H.264 Constrained Baseline):** Thay vì để máy chủ Xbox gửi luồng video nén phức tạp khiến chip điện thoại phải "giữ lại 2–3 khung hình trong bộ nhớ đệm" rồi mới chiếu (làm thời gian giải mã bị đội lên 15–30ms và hiện cảnh báo Decode), ứng dụng sẽ yêu cầu máy chủ gửi định dạng **H.264 Cơ bản (Baseline)**. Định dạng này giải mã tới đâu chiếu ngay tới đó (chỉ mất 2–4ms) mà hình ảnh vẫn sắc nét.
> 2. **Công tắc "Ẩn thông số luồng trên màn hình" trong Cài đặt:** Khi bạn bật công tắc này trong phần Cài đặt của ứng dụng, ứng dụng sẽ tự động ẩn luôn biểu tượng cảnh báo mạng/Decode gây vướng mắt của trang web Xbox. Nếu bạn tắt công tắc này, biểu tượng gốc của Xbox sẽ hiện lại bình thường.
> 3. **Cách ứng dụng xử lý khi máy thực sự quá tải (nhiệt độ cao / rớt khung hình):** Ứng dụng sẽ không hiện thông báo làm phiền hay tự ý hạ độ phân giải đột ngột khi mới chỉ có vài khung hình trễ nhẹ lúc chuyển cảnh. Chỉ khi máy thực sự bị quá tải nặng kéo dài liên tục trên 10 giây kèm giật hình rõ rệt, hệ thống mới âm thầm giảm nhẹ bộ lọc làm nét để giúp máy mát và mượt trở lại.

---

## 1. Overview & Core Concept

Khi chơi Xbox Cloud Gaming trên Android WebView, cảnh báo **Decode** (cả biểu tượng cảnh báo gốc của giao diện web Xbox lẫn cảnh báo trong bảng HUD của ứng dụng) xuất hiện khi chỉ số `totalDecodeTime / framesDecoded` của WebRTC vượt ngưỡng cho phép.

Qua quá trình rà soát lại toàn bộ mã nguồn, chúng tôi phát hiện lý do tại sao các giải pháp trước đây chưa dập tắt được lỗi Decode:

1. **Đặt ưu tiên codec H.264 sai thời điểm và chưa chọn đúng cấu hình không trễ (Nguyên nhân quan trọng nhất mới phát hiện):**
   - Hiện tại, hàm sắp xếp ưu tiên codec H.264 đang được gọi bên trong `setLocalDescription` — tức là **sau khi** bản tin thương lượng kết nối (SDP Answer/Offer) đã được tạo xong. Theo chuẩn WebRTC, việc thay đổi danh sách ưu tiên codec sau khi đã tạo SDP hoàn toàn không có tác dụng lên phiên kết nối đó.
   - Đồng thời, hàm sắp xếp trước đây giữ nguyên thứ tự cấu hình H.264 mặc định của trình duyệt (đặt *High Profile* `64001f` và *Main Profile* `4d001f` lên trước *Constrained Baseline* `42e01f`). Trên chip giải mã phần cứng Android (`MediaCodec`), cấu hình High/Main cho phép sắp xếp lại thứ tự khung hình (Picture Reordering), buộc bộ giải mã phải giữ lại 2–3 khung hình trong bộ đệm (DPB) trước khi xuất ra màn hình. Do trình duyệt đo thời gian từ lúc nạp khung hình vào đến lúc lấy khung hình ra khỏi bộ đệm, thời gian giải mã bị cộng dồn thêm `12ms – 25ms` dù chip xử lý thực tế chỉ mất `2ms`.
2. **Chèn giới hạn băng thông (`b=AS:`) sai thứ tự chuẩn RFC 4566 trong bản tin SDP (Lỗi mới phát hiện):**
   - Hàm điều chỉnh SDP hiện đang chèn dòng giới hạn băng thông `b=AS:...` ngay dưới dòng `m=video ...`, tức là nằm **trước** dòng thông tin kết nối `c=IN IP4 ...`. Theo tiêu chuẩn quốc tế RFC 4566 (Mục 5), dòng `b=` bắt buộc phải đứng **sau** dòng `c=`. Việc đặt sai thứ tự khiến bộ phân tích WebRTC của nhân Chromium bỏ qua hoặc từ chối bản tin SDP đã chỉnh sửa.
3. **Giao diện màn hình chơi game tự vẽ lại toàn bộ mỗi 2 giây:**
   - Màn hình trình duyệt đám mây đang lắng nghe biến trạng thái thông số mạng (`telemetry` và `adaptiveQualityState`) ngay ở cấp cao nhất. Cứ mỗi 2 giây khi có số liệu mới, toàn bộ màn hình chính bị kích hoạt vẽ lại (recompose), kéo theo lệnh cập nhật khung chứa WebView và ép tiến trình đồ họa của Android (`RenderThread` / `SurfaceFlinger`) phải ghép lớp lại đè lên bề mặt video phần cứng, gây nghẽn hàng đợi khung hình.
4. **Hai vòng lặp JavaScript chạy song song và truy vấn lại cây giao diện web (DOM) liên tục:**
   - Luồng đọc thông số WebRTC (mỗi 2 giây) và luồng dự phòng đọc thẻ `<video>` (mỗi 2,5 giây) cùng hoạt động và liên tục gọi `document.querySelector` để tìm thẻ video trên trang web chứa hàng nghìn phần tử của Xbox.
5. **Sai số phép chia khi chuyển cảnh và ngưỡng cảnh báo quá nhạy (`9.5ms` / `12.0ms`):**
   - Trong lúc màn hình tải game hoặc chuyển cảnh (số khung hình giải mã chỉ đạt 1–5 khung hình nhưng chứa khung hình gốc dung lượng lớn), phép chia trung bình bị vọt lên cao giả tạo. Bên cạnh đó, ngưỡng cảnh báo nội bộ đang đặt ở `9.5ms – 12.0ms`, quá thấp so với quỹ thời gian thực tế của màn hình 60FPS (`16.67ms`) và 30FPS (`33.33ms`).
6. **Công tắc "Ẩn thông số luồng trên màn hình" đang bị rỗng logic:**
   - Hàm xử lý ẩn lớp phủ thông số mạng/Decode gốc của trang web Xbox hiện chỉ xóa thẻ CSS chứ chưa chèn quy tắc ẩn khi người dùng bật công tắc trong Cài đặt.

---

## 2. User Experience & Visual Design

Giải pháp được thiết kế để mang lại trải nghiệm mượt mà, tự động hoàn toàn ở tầng nền mà không đòi hỏi người dùng phải am hiểu thông số kỹ thuật:

- **Vào game mượt ngay từ giây đầu tiên:** Quá trình thương lượng định dạng video siêu tốc (`Constrained Baseline`) diễn ra tự động ngay khi bắt đầu kết nối máy chủ Xbox, giữ thời gian giải mã ổn định ở mức `1.5ms – 4.0ms`.
- **Không còn cảnh báo giả lúc tải màn chơi:** Khi vào màn hình chờ, xem phim cắt cảnh hoặc chuyển menu trong game, bảng thông số HUD và bộ tự động chất lượng sẽ lọc bỏ các nhịp nhảy số ảo, không còn báo đỏ **Decode** hay tự ý tắt bộ lọc nét một cách đột ngột.
- **Tôn trọng tuyệt đối lựa chọn hiển thị của người dùng:**
  - Nếu người dùng **Tắt** mục *"Ẩn thông số luồng trên màn hình"*: Hiển thị đầy đủ như mặc định để người dùng theo dõi nếu muốn.
  - Nếu người dùng **Bật** mục *"Ẩn thông số luồng trên màn hình"*: Ẩn sạch sẽ các biểu tượng cảnh báo Decode/Network gây xao nhãng của giao diện web Xbox, giữ khung hình game trọn vẹn.
- **Khung hình video ổn định 60FPS:** Tách biệt hoàn toàn bảng thông số nổi (HUD) khỏi khung hình video WebView để việc cập nhật con số mỗi 2 giây trên HUD không bao giờ chạm tới luồng phát video.

---

## 3. Key Product Decisions & Trade-Offs

- **Quyết định 1: Ưu tiên tuyệt đối `H.264 Constrained Baseline (42e01f)` trước khi tạo SDP thay vì `High Profile (64001f)`**
  - *Lợi ích:* Loại bỏ hoàn toàn độ trễ giữ khung hình trong bộ đệm phần cứng Android (`num_reorder_frames = 0`), giảm thời gian giải mã từ `16–30ms` xuống `2–4ms`, triệt tiêu nguyên nhân chính khiến trang web Xbox bật cờ cảnh báo Decode.
  - *Đánh đổi:* Ở cùng một mức băng thông rất thấp, Baseline nén kém hiệu quả hơn High Profile khoảng 5–8%. Tuy nhiên, trên luồng Xbox Cloud Gaming với băng thông ổn định (10–15 Mbps), chất lượng hình ảnh thực tế không có sự khác biệt mắt thường nhận ra, trong khi độ trễ đầu vào và độ mượt tăng vượt trội.
- **Quyết định 2: Lọc nhiễu thống kê giải mã và nâng ngưỡng cảnh báo lên `14.5ms` (nhắc sớm) / `18.0ms` (quá tải thực sự kèm rớt khung hình)**
  - *Lợi ích:* Phản ánh đúng giới hạn vật lý của màn hình 60FPS (`16.67ms`/khung hình), tránh làm phiền người dùng hoặc hạ chất lượng hình ảnh oan khi chip giải mã vẫn đang hoàn thành tốt khung hình đúng hạn.
  - *Đánh đổi:* Những dao động nhỏ dưới `14ms` sẽ được coi là hoạt động bình thường của phần cứng.
- **Quyết định 3: Lưu bộ nhớ đệm (Cache) phần tử `<video>` và hợp nhất nhịp đo thông số**
  - *Lợi ích:* Giảm tải cho luồng chính của trình duyệt (Blink Main Thread), giúp luồng nhận dữ liệu mạng và giải mã khung hình không bị gián đoạn bởi các lệnh quét trang web.

---

## 4. Technical Architecture & Data Strategy

### Sơ đồ Kiến trúc Luồng Giải Mã & Hiển Thị Sau Tối Ưu

```
+-----------------------------------------------------------------------------------+
|                        XBOX CLOUD GAMING WEBRTC STREAM                            |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
| 1. TẦNG THƯƠNG LƯỢNG CODEC & SDP (Trước khi tạo Answer/Offer)                     |
|  - Hook setRemoteDescription + createAnswer/createOffer                           |
|  - Sắp xếp H.264: Constrained Baseline (42e0/4200) -> Main (4d) -> High (64)      |
|  - MediaCapabilities: báo powerEfficient = true ưu tiên cho Baseline & Main       |
|  - Chèn b=AS:<kbps> và sprop-max-don-diff=0 đúng chuẩn RFC 4566 (sau dòng c=IN)   |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
| 2. BỘ GIẢI MÃ PHẦN CỨNG ANDROID (MediaCodec Hardware Decoder)                     |
|  - Không giữ khung hình chờ sắp xếp lại (Zero DPB Reorder Delay)                  |
|  - Xuất khung hình tức thì ra SurfaceTexture (Decode Time: ~1.5ms - 4.0ms)        |
+-----------------------------------------------------------------------------------+
                   |                                             |
                   v                                             v
+-----------------------------------------+   +-------------------------------------+
| 3. KHUNG HÌNH VIDEO (WebView Surface)   |   | 4. BỘ THU THẬP THÔNG SỐ NHẸ         |
|  - Độc lập hoàn toàn, KHÔNG bị vẽ lại   |   |  - Chu kỳ 2500ms, Cache thẻ <video> |
|    khi thông số mạng cập nhật           |   |  - Bỏ qua mẫu nhiễu (<12 frames)    |
|  - Tùy chọn ẩn cảnh báo gốc của web     |   |  - Làm mượt EMA trước khi gửi       |
+-----------------------------------------+   +-------------------------------------+
                                                                 |
                                                                 v
                                              +-------------------------------------+
                                              | 5. LỚP PHỦ HUD CÔ LẬP (Compose)     |
                                              |  - Chỉ tự vẽ lại phần chữ số nhỏ    |
                                              |  - Chỉ báo Decode khi >18ms kéo dài |
                                              |    kèm rớt khung hình thực tế       |
                                              +-------------------------------------+
```

---

### Thiết Kế Chi Tiết Theo Từng Bước (Step-by-Step Design Plan)

#### Bước 1: Sửa thời điểm chọn Codec và ưu tiên cấu hình H.264 Không Trễ (Constrained Baseline)
1. **Phân loại và sắp xếp chi tiết cấu hình H.264:**
   - Trong hàm chọn ưu tiên codec của cầu nối WebRTC, kiểm tra thuộc tính `sdpFmtpLine` của từng codec `video/H264`:
     - **Nhóm ưu tiên 1 (Cao nhất):** H.264 Constrained Baseline (`profile-level-id=42e0...` hoặc `4200...`) kèm `packetization-mode=1`.
     - **Nhóm ưu tiên 2:** H.264 Main Profile (`profile-level-id=4d...`).
     - **Nhóm ưu tiên 3:** Các cấu hình H.264 còn lại (`profile-level-id=64...`).
     - **Nhóm ưu tiên 4:** Các gói sửa lỗi luồng video (`video/rtx`, `video/red`, `video/ulpfec`).
2. **Gọi hàm đặt ưu tiên codec đúng thời điểm chuẩn WebRTC:**
   - Gắn lệnh gọi sắp xếp codec ngay sau khi `setRemoteDescription` hoàn tất **và** ngay trước khi `createAnswer` / `createOffer` sinh ra chuỗi SDP. Nhờ đó, bản tin SDP gửi về máy chủ Xbox sẽ đặt mã định dạng Constrained Baseline lên đầu tiên.
3. **Đồng bộ thông tin năng lực giải mã (`navigator.mediaCapabilities.decodingInfo`):**
   - Khi trang web Xbox hỏi trình duyệt về khả năng giải mã các cấu hình H.264, trả về `supported: true, smooth: true, powerEfficient: true` cho cấu hình Baseline (`avc1.42...`) và Main (`avc1.4d...`), đồng thời trả về `powerEfficient: false` cho cấu hình High Profile (`avc1.64...`) ở độ phân giải cao hoặc chế độ tiết kiệm để máy chủ ưu tiên cấp luồng nhẹ nhất cho chip giải mã.

#### Bước 2: Sửa lỗi cú pháp SDP theo đúng chuẩn RFC 4566
1. **Đặt dòng băng thông `b=AS:` đúng vị trí trong vùng `m=video`:**
   - Phân tách vùng mô tả `m=video` trong chuỗi SDP: nếu trong vùng này có dòng kết nối `c=IN ...`, chèn `b=AS:<targetKbps>` ngay **sau** dòng `c=IN ...`. Chỉ khi không có dòng `c=` trong vùng `m=video` mới chèn ngay sau dòng `m=video`.
   - Kiểm tra tránh chèn trùng lặp nếu chuỗi SDP đã có sẵn dòng `b=AS:` hoặc `b=TIAS:`.
2. **Bổ sung tham số giải mã xuất khung hình ngay (`sprop-max-don-diff=0`):**
   - Trong các dòng `a=fmtp:...` của codec H.264, bổ sung `sprop-max-don-diff=0` để báo cho bộ giải mã phần cứng Android không cần chờ sắp xếp lại thứ tự gói tin.
3. **Đảm bảo tương thích mọi cách gọi `setLocalDescription`:**
   - Xử lý an toàn cả trường hợp trang web gọi `setLocalDescription(desc)` lẫn trường hợp gọi `setLocalDescription()` không truyền tham số, đồng thời áp dụng chỉnh sửa SDP ngay ở bước trả về của `createAnswer` / `createOffer`.

#### Bước 3: Cô lập việc vẽ lại giao diện (Recomposition) khỏi màn hình WebView
1. **Tách lớp phủ thông số (HUD) thành thành phần độc lập:**
   - Không đọc trực tiếp luồng dữ liệu `telemetry` và `adaptiveQualityState` ở hàm giao diện gốc chứa `WebView`.
   - Chuyển việc lắng nghe hai luồng dữ liệu này vào bên trong một khung giao diện con chuyên biệt chỉ dành cho bảng HUD nổi và hộp thoại chi tiết mạng.
   - Nhờ thiết kế này, khi thông số mạng nhảy số mỗi 2,5 giây, chỉ có khung chữ nhỏ của HUD được vẽ lại, còn `AndroidView` chứa `WebView` đứng yên tuyệt đối 100%.
2. **Loại bỏ lệnh ghi nhật ký cảm ứng mỗi giây trên luồng chính:**
   - Bỏ lệnh gọi `Log.d` đếm số lần chạm màn hình mỗi giây trong bộ lắng nghe cảm ứng của `WebView` để tránh tạo rác bộ nhớ khi người dùng thao tác liên tục.

#### Bước 4: Tối ưu hóa vòng lặp đo thông số và bộ nhớ đệm `<video>`
1. **Lưu bộ nhớ đệm (Cache) phần tử `<video>` đang phát:**
   - Khi đã tìm thấy thẻ `<video>` đang phát luồng game (`readyState >= 2`), lưu tham chiếu vào biến đệm và tái sử dụng ở các chu kỳ sau; chỉ tìm kiếm lại trên trang web nếu thẻ video cũ bị ngắt khỏi giao diện (`!isConnected`).
2. **Điều chỉnh nhịp thu thập thông số hợp lý:**
   - Giãn chu kỳ đọc thống kê WebRTC sang `2500ms` và tạm nghỉ vòng lặp dự phòng khi luồng thống kê WebRTC chính đang trả dữ liệu đều đặn, giúp giảm một nửa số tác vụ chạy ngầm trên luồng JavaScript.

#### Bước 5: Chống nhiễu phép tính `decodeTimeMs` và chuẩn hóa ngưỡng cảnh báo
1. **Chặn sai số khi ít khung hình (màn hình chờ / chuyển cảnh):**
   - Chỉ cập nhật phép chia thời gian giải mã trung bình khi số khung hình giải mã mới trong chu kỳ đạt từ `12 khung hình` trở lên (`deltaFrames >= 12`).
   - Áp dụng bộ lọc làm mượt trung bình động (EMA: 70% giá trị ổn định trước đó + 30% giá trị mới) và giới hạn trần hợp lý để một khung hình gốc (keyframe) nặng lúc chuyển cảnh không làm vọt chỉ số.
2. **Chuẩn hóa ngưỡng đánh giá trong bộ quản lý chất lượng và cảnh báo:**
   - Nâng ngưỡng cảnh báo sớm từ `9.5ms` lên `14.5ms` và ngưỡng quá tải giải mã từ `12.0ms` lên `18.0ms` (vượt quỹ thời gian `16.67ms` của 60FPS) kèm điều kiện thực sự có rớt khung hình hoặc tụt FPS.
   - Tăng số mẫu xác nhận liên tiếp lên 5 chu kỳ trước khi hệ thống tự động giảm nhẹ bộ lọc làm nét (Clarity Boost), giúp hình ảnh luôn giữ độ nét ổn định.

#### Bước 6: Khôi phục chức năng cho công tắc "Ẩn thông số luồng trên màn hình"
1. **Hoàn thiện hàm ẩn lớp phủ thống kê trên trang web Xbox:**
   - Khi người dùng bật tùy chọn *"Ẩn thông số luồng trên màn hình"* (`hideStreamStatsOverlay = true`), nạp quy tắc CSS ẩn các khung cảnh báo mạng/Decode nổi của giao diện web Xbox (`StreamStats`, `NetworkQualityIndicator`, `stream-stats`) mà không ảnh hưởng đến bảng điều khiển hay menu game.
   - Khi người dùng tắt tùy chọn này (`hideStreamStatsOverlay = false`), gỡ bỏ quy tắc CSS ngay lập tức để hiển thị lại nguyên bản.
