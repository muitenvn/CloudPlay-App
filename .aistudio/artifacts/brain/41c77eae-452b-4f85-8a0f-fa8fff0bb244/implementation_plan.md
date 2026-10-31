# Kế Hoạch Nâng Cấp Độ Bền, Tối Ưu Hóa Build & Trải Nghiệm Người Dùng (10 Hạng Mục)

Bản kế hoạch này tổng hợp phương án triển khai 10 hạng mục cải tiến cho ứng dụng CloudPlay dựa trên kết quả kiểm tra mã nguồn thực tế và các lựa chọn thiết kế bạn đã xác nhận, tập trung vào việc dọn dẹp cấu hình biên dịch, bật tối ưu hóa R8 an toàn, bổ sung công cụ tự chẩn đoán lỗi (không lộ dữ liệu nhạy cảm), cảnh báo nhiệt độ/pin/mạng thông minh khi đang stream và hoàn thiện khả năng tiếp cận (accessibility).

---

## User Review & Critical Decisions

> [!IMPORTANT]
> Dưới đây là các quyết định thiết kế đã được xác nhận từ câu trả lời của bạn cùng kết quả đối chiếu mã nguồn hiện tại cho toàn bộ 10 mục yêu cầu.

- **Quyết định đã xác nhận 1 — Báo cáo chẩn đoán trong Cài đặt (Mục 5)**: Khi bấm nút **"Sao chép báo cáo chẩn đoán"**, ứng dụng sẽ đồng thời sao chép toàn bộ nội dung báo cáo vào bộ nhớ tạm (Clipboard) kèm thông báo nhanh, và mở bảng chia sẻ hệ thống (`ACTION_SEND`) dưới dạng văn bản thuần. Báo cáo tuyệt đối loại bỏ cookie, token, thông tin tài khoản và địa chỉ IP người dùng.
- **Quyết định đã xác nhận 2 — Cảnh báo nhiệt độ/pin yếu & đổi mạng khi đang stream (Mục 6 & 7)**: Hiển thị dưới dạng **banner nhỏ tự ẩn sau vài giây** tích hợp gọn vào thanh trạng thái nổi (`FloatingHud`), không che tầm nhìn trong game và tuyệt đối không tự ý thay đổi cài đặt độ phân giải hay FPS của người dùng.
- **Quyết định đã xác nhận 3 — Phạm vi đặt lại bố cục tay cầm ảo (Mục 9)**: Khi người dùng xác nhận trong hộp thoại **"Đặt lại bố cục mặc định"**, ứng dụng chỉ đặt lại tọa độ vị trí tùy chỉnh của các nút về mặc định, giữ nguyên cấu hình tách nút (ABXY / D-Pad rời) và danh sách các nút đang ẩn/hiện.

> [!NOTE]
> **Kết quả rà soát trạng thái hiện có trong mã nguồn (Audit & Phản biện kỹ thuật):**
> - **Mục 1 (Bỏ plugin Google Services)**: Đã kiểm tra toàn bộ mã nguồn không còn bất kỳ tham chiếu nào tới Firebase hay cấu hình dịch vụ Google. Có thể gỡ bỏ hoàn toàn plugin và khối cấu hình chiến lược thiếu dịch vụ mà không ảnh hưởng tới quá trình build.
> - **Mục 2 (Kiểm thử máy chủ mặc định & chú thích trung tính)**: Bài kiểm thử hiện đang kiểm tra điều kiện tốc độ trên 250 Mbps và cờ xác minh cho 60 máy chủ mặc định. Sẽ lược bỏ 2 điều kiện này (giữ nguyên kiểm tra số lượng 60 máy chủ và mã quốc gia khớp GeoIP) đồng thời chuyển chú thích sang mô tả trung tính mà không thay đổi dữ liệu máy chủ gốc.
> - **Mục 3 (Bật R8 & quy tắc giữ lớp/phương thức)**: Mã nguồn không sử dụng gọi hàm động qua tên chuỗi (`Class.forName`), nhưng có sử dụng cầu nối JavaScript (`@JavascriptInterface`), lưu trữ cục bộ Room, phân tích JSON và các kiểu liệt kê (`enum`) trong cấu hình. Sẽ bật rút gọn mã/tài nguyên cho bản phát hành kèm bộ quy tắc bảo vệ đầy đủ để không làm gãy cầu nối WebView hoặc tuần tự hóa dữ liệu.
> - **Mục 6, 7, 8, 9 (Cảnh báo nhiệt/mạng, Hộp thoại cập nhật, Đặt lại tay cầm)**: Mã nguồn đã có sẵn một phần nền tảng (như lắng nghe trạng thái nhiệt độ/mạng, làm sạch mô tả bản cập nhật, và nút đặt lại vị trí nút tay cầm), nhưng cần điều chỉnh ngưỡng kích hoạt (`THERMAL_STATUS_SEVERE` hoặc pin `≤ 15%` không sạc), bổ sung giới hạn 1 lần/phiên, thêm tiêu đề **"Có gì mới"** giới hạn 10 dòng đầu, và bổ sung **hộp thoại xác nhận** trước khi đặt lại tọa độ tay cầm.

---

## 1. Overview & Core Concept

- **Mục tiêu cốt lõi**: Giúp bản phát hành (Release) gọn nhẹ và an toàn hơn với R8, cung cấp cơ chế ghi nhận sự cố nội bộ và xuất báo cáo chẩn đoán minh bạch, đồng thời cải thiện phản hồi thời gian thực trong lúc chơi game đám mây mà không làm gián đoạn trải nghiệm.
- **Đối tượng sử dụng**: Người chơi Xbox Cloud Gaming trên thiết bị di động cần sự ổn định cao khi chuyển đổi mạng (Wi-Fi ↔ 4G/5G), kiểm soát hiện tượng quá nhiệt/tụt pin, và dễ dàng gửi thông tin kỹ thuật khi cần hỗ trợ.
- **Giá trị mang lại**:
  - Giảm dung lượng APK phát hành và loại bỏ hoàn toàn các plugin không còn sử dụng.
  - Tự động lưu dấu vết lỗi (stacktrace) gần nhất trong bộ nhớ nội bộ an toàn (tối đa 20 KB, không gửi qua mạng).
  - Cảnh báo tinh tế, đúng thời điểm khi thiết bị quá nóng, yếu pin hoặc chuyển mạng.
  - Hỗ trợ đầy đủ nhãn đọc màn hình (Accessibility) theo ngôn ngữ giao diện đang chọn (Tiếng Việt / Tiếng Anh).

---

## 2. User Experience & Visual Design

- **Luồng trải nghiệm người dùng chính**:
  1. **Sao chép & Chia sẻ Báo cáo Chẩn đoán (Mục 4 & 5)**: Trong màn hình *Cài đặt → Hệ thống*, người dùng chạm vào nút **"Sao chép báo cáo chẩn đoán"**. Ứng dụng lập tức chép bản tóm tắt (phiên bản ứng dụng, phiên bản Android, dòng máy, loại mạng, thị trường Xbox, độ phân giải/FPS/VPN, tóm tắt phiên stream gần nhất và log sự cố gần nhất nếu có) vào bộ nhớ tạm, đồng thời bật bảng chia sẻ hệ thống để gửi nhanh.
  2. **Banner cảnh báo thông minh khi đang Stream (Mục 6 & 7)**:
     - Khi nhiệt độ máy chạm ngưỡng nghiêm trọng (`THERMAL_STATUS_SEVERE`) hoặc pin `≤ 15%` và không cắm sạc: Thanh HUD nổi hiển thị banner nhỏ gọn *"Máy nóng/pin yếu, nên giảm FPS hoặc độ phân giải"* trong vài giây rồi tự thu gọn (tối đa 1 lần cho mỗi loại cảnh báo trong suốt phiên chơi).
     - Khi thiết bị chuyển đổi giữa Wi-Fi và dữ liệu di động: Thanh HUD nổi hiển thị banner *"Mạng vừa đổi, nếu giật hãy thử lại"* mà không ngắt kết nối phiên chơi.
  3. **Xem thông tin "Có gì mới" khi có bản cập nhật (Mục 8)**: Khi hộp thoại cập nhật xuất hiện, nếu bản phát hành trên GitHub có mô tả, ứng dụng làm sạch ký hiệu Markdown thô và hiển thị tối đa 10 dòng đầu tiên dưới tiêu đề **"Có gì mới"**; nếu trống sẽ tự động ẩn khung này.
  4. **Xác nhận đặt lại vị trí nút tay cầm ảo (Mục 9)**: Khi người dùng nhấn nút **"Đặt lại bố cục mặc định"** (trong bảng chỉnh nhanh tay cầm hoặc trong Cài đặt), một hộp thoại xác nhận xuất hiện để tránh thao tác nhầm. Khi đồng ý, chỉ tọa độ tùy chỉnh được đưa về mặc định.
- **Phong cách thị giác & Trợ năng (Mục 10)**:
  - Giữ vững ngôn ngữ thiết kế tối màu đặc trưng của Xbox Cloud (`#0A0E14` nền sâu, điểm nhấn xanh lá `#107C10` / `#2ECC71`, cảnh báo vàng hổ phách `#F59E0B`).
  - Toàn bộ các nút tương tác chỉ có biểu tượng (nút Yêu thích, Thông tin game, nút nổi `+`, các mục trên thanh điều hướng dưới cùng, nút Cài đặt/Ngôn ngữ/Trợ giúp ở thanh tiêu đề) được gắn nhãn mô tả (`contentDescription`) tự động chuyển đổi theo ngôn ngữ Tiếng Việt hoặc Tiếng Anh. Các biểu tượng thuần trang trí đi kèm văn bản vẫn giữ giá trị rỗng để trình đọc màn hình không đọc lặp.

---

## 3. Key Product Decisions & Trade-Offs

- **Quyết định 1: Ghi nhận sự cố cục bộ giới hạn dung lượng (Mục 4)**
  - *Phương án lựa chọn*: Sử dụng bộ bắt ngoại lệ toàn cục (`UncaughtExceptionHandler`) ghi đè vào một tệp duy nhất trong thư mục nội bộ của ứng dụng với giới hạn tối đa ~20 KB, sau đó chuyển tiếp cho trình xử lý mặc định của hệ thống.
  - *Lý do*: Đảm bảo không tiêu tốn dung lượng bộ nhớ theo thời gian, không phát sinh kết nối mạng ngầm và giữ nguyên hành vi báo lỗi chuẩn của hệ điều hành Android.
- **Quyết định 2: Bảo vệ quyền riêng tư tuyệt đối trong Báo cáo Chẩn đoán (Mục 5)**
  - *Phương án lựa chọn*: Chỉ trích xuất các chỉ số cấu hình và hiệu năng tổng hợp (FPS, ping, jitter, bitrate, tỉ lệ mất gói, trạng thái bật/tắt VPN), tuyệt đối không đọc kho lưu trữ cookie WebView, token xác thực hay địa chỉ IP.
  - *Lý do*: Giúp người dùng yên tâm chia sẻ báo cáo chẩn đoán công khai lên diễn đàn hoặc nhóm hỗ trợ mà không sợ lộ phiên đăng nhập Xbox.
- **Quyết định 3: Cảnh báo thụ động thay vì tự động hạ cấu hình khi máy nóng/pin yếu (Mục 6)**
  - *Phương án lựa chọn*: Chỉ hiển thị gợi ý không chặn trên HUD nổi tối đa 1 lần/phiên cho mỗi điều kiện, không tự ý hạ FPS hay độ phân giải đang chạy.
  - *Lý do*: Tránh gây giật khung hình đột ngột hoặc thay đổi chất lượng hình ảnh ngoài ý muốn khi người dùng đang trong tình huống quan trọng của trò chơi.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Kiến trúc luồng chẩn đoán, cảnh báo thời gian thực và bảo vệ mã nguồn

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        KHỞI ĐỘNG & GHI NHẬN SỰ CỐ                          │
│                                                                            │
│  [UncaughtExceptionHandler] ──► Ghi stacktrace (≤ 20 KB) ──► Bộ nhớ nội bộ │
│              │                                                (1 file duy  │
│              ▼                                                   nhất)     │
│   Chuyển tiếp cho Handler gốc của hệ thống                                 │
└────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                   BỘ TỔNG HỢP BÁO CÁO CHẨN ĐOÁN AN TOÀN                    │
│                                                                            │
│  ┌─────────────────────┐  ┌──────────────────────┐  ┌───────────────────┐  │
│  │ Thông tin thiết bị  │  │ Cấu hình & Telemetry │  │ Log crash nội bộ  │  │
│  │ (App/OS/Model/Mạng) │  │ (Res/FPS/VPN/Session)│  │ (Tối đa ~20 KB)   │  │
│  └──────────┬──────────┘  └──────────┬───────────┘  └─────────┬─────────┘  │
│             └────────────────────────┼────────────────────────┘            │
│                                      ▼                                     │
│             [Bộ lọc an toàn: Loại bỏ Cookie / Token / IP]                  │
│                                      │                                     │
│                     ┌────────────────┴────────────────┐                    │
│                     ▼                                 ▼                    │
│          Sao chép vào Clipboard            Chia sẻ qua ACTION_SEND         │
└────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────────┐
│                    GIÁM SÁT PHIÊN STREAM TRÊN FLOATING HUD                 │
│                                                                            │
│  [PowerManager (API 29+)] ──► Nhiệt độ ≥ SEVERE (1 lần/phiên) ─┐           │
│  [Battery Broadcast]      ──► Pin ≤ 15% & Không sạc (1 lần)   ─┼─► Banner  │
│  [NetworkOptimizer Flow]  ──► Đổi Wi-Fi ↔ Di động             ─┘   tự ẩn   │
└────────────────────────────────────────────────────────────────────────────┘
```

### Mô hình dữ liệu & Ánh xạ trạng thái tương tác

- **Trình quản lý log sự cố nội bộ**:
  - Khi xảy ra ngoại lệ chưa được bắt trên bất kỳ luồng nào, trích xuất thông tin ngoại lệ và chuỗi ngăn xếp, cắt gọn trong phạm vi 20.480 byte (20 KB), ghi đè vào tệp lưu trữ nội bộ duy nhất và gọi trình xử lý mặc định ban đầu.
- **Trình tạo báo cáo chẩn đoán**:
  - Thu thập thông tin phiên bản ứng dụng, cấp độ API Android, tên thiết bị, loại kết nối mạng hiện tại (Wi-Fi / Di động), thị trường Xbox đang chọn, cấu hình độ phân giải/FPS/trạng thái VPN, số liệu phiên stream gần nhất từ kho lịch sử phiên, và nội dung tệp sự cố gần nhất.
  - Kết nối trực tiếp với nút bấm trong màn hình Cài đặt để thực hiện cả hai thao tác: chép vào Clipboard và gọi hộp chọn chia sẻ hệ thống.
- **Trạng thái cảnh báo trên HUD nổi**:
  - Duy trì 2 biến cờ trạng thái theo phiên (`hasShownSevereThermalWarning`, `hasShownLowBatteryWarning`) để đảm bảo mỗi loại cảnh báo chỉ xuất hiện tối đa 1 lần trong một phiên chơi.
  - Theo dõi sự thay đổi loại hình kết nối mạng (`Wi-Fi` sang `Cellular` hoặc ngược lại) trong lúc phiên stream đang hoạt động để kích hoạt thông báo ngắn tự ẩn sau 4.5 giây mà không làm gián đoạn WebView.
- **Cấu hình tối ưu hóa R8 & ProGuard**:
  - Bật cờ rút gọn mã (`isMinifyEnabled = true`) và thu gọn tài nguyên (`isShrinkResources = true`) cho cấu hình phát hành.
  - Bổ sung quy tắc giữ nguyên tên lớp và phương thức cho lớp cầu nối WebView có gắn `@JavascriptInterface`, giữ nguyên các lớp thực thể cơ sở dữ liệu Room, các mô hình dữ liệu tuần tự hóa JSON/Moshi và toàn bộ các kiểu liệt kê (`enum`) được lưu trong cấu hình người dùng.
