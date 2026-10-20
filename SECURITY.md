# Chính Sách Bảo Mật (Security Policy) - CloudPlay

Tài liệu này mô tả các nguyên tắc bảo mật, kiến trúc cách ly và quy trình báo cáo lỗ hổng cho ứng dụng **CloudPlay / Cloud VPN Switcher**.

---

## 1. Triết Lý Bảo Mật: Zero-Trust & Lưu Trữ Cục Bộ (Local-Only)

CloudPlay được thiết kế theo mô hình **Zero-Trust Client** dành riêng cho trải nghiệm chơi game đám mây Xbox Cloud Gaming (xCloud):
- **Không gửi thông tin đăng nhập về máy chủ bên thứ ba**: Mọi quá trình đăng nhập Xbox / Microsoft được thực hiện trực tiếp trên trang chủ chính thức của Microsoft (`login.live.com`, `login.microsoftonline.com`, `xbox.com`).
- **Không can thiệp chứng chỉ SSL (No SSL Bumping/MITM)**: Ứng dụng không bao giờ cài đặt chứng chỉ gốc (root CA) hay giải mã gói tin mã hóa TLS/HTTPS của người dùng.
- **Dữ liệu lưu trữ hoàn toàn trên thiết bị**: Cấu hình proxy, tùy chọn ngôn ngữ, bookmark game và thiết lập chất lượng được lưu trữ cục bộ trong bộ nhớ an toàn của Android (`Context.MODE_PRIVATE`). Ứng dụng không sử dụng dịch vụ đám mây trung gian hay Firebase để thu thập thông tin người dùng.

---

## 2. Kiểm Soát Tên Miền & Cách Ly WebView (Domain Isolation)

Để ngăn chặn các cuộc tấn công qua trang web giả mạo hoặc chuyển hướng độc hại:

1. **Danh sách tên miền tin cậy (Strict Whitelist)**:
   Mọi script can thiệp tối ưu hóa luồng chỉ được thực thi trên các máy chủ được xác minh thuộc hạ tầng của Microsoft:
   - `xbox.com` (và các tên miền con)
   - `xboxlive.com`
   - `xboxservices.com`
   - `live.com` / `live.net`
   - `microsoft.com` / `microsoftonline.com`
   - `msauth.net`

2. **Xác thực giao thức HTTPS**:
   Chỉ cho phép tải nội dung qua giao thức `https://`. Mọi liên kết không an toàn `http://` đều bị từ chối hoặc chặn thực thi bridge.

3. **Cách ly JavaScript Interface (`@JavascriptInterface`)**:
   Tất cả hàm giao tiếp giữa WebView và Kotlin trong `XboxBridge` đều chạy qua cổng kiểm tra nguồn gốc `isCallerTrusted()`. Nếu người dùng điều hướng ra ngoài danh sách tên miền tin cậy, toàn bộ các hàm API của bridge sẽ ngay lập tức bị vô hiệu hóa.

---

## 3. Bảo Vệ Giao Dịch & Thanh Toán (External Navigation Safety)

Khi người dùng thực hiện mua sắm gói Game Pass hoặc thanh toán qua thẻ ngân hàng/3D-Secure:
- Hệ thống tự động kích hoạt cơ chế `pauseProxyForExternalNavigation()`.
- Proxy được ngắt tạm thời để toàn bộ giao dịch đi thẳng qua mạng ISP trực tiếp của thiết bị, loại bỏ hoàn toàn nguy cơ máy chủ proxy ghi log hoặc can thiệp thông tin giao dịch thẻ.
- Sau khi hoàn tất và trở về trang game, proxy hoặc mạng trực tiếp sẽ được khôi phục tự động qua `restoreProxyForGaming()`.

---

## 4. Đo Đạc WebRTC Telemetry Minh Bạch

- Toàn bộ thông số như **Thời gian giải mã (decodeTimeMs)**, **Bitrate (Mbps)**, **Ping (RTT)**, **Jitter**, và **Rớt khung (Frames Dropped)** được đọc trực tiếp từ chuẩn W3C `RTCPeerConnection.getStats()`.
- Dữ liệu này chỉ được xử lý trong RAM nội bộ để phục vụ:
  1. Hiển thị bảng HUD nổi cho người chơi theo dõi độ trễ.
  2. Cung cấp dữ liệu cho `AdaptiveQualityManager` để tự động hạ bậc bitrate khi mạng yếu hoặc GPU quá tải, ngăn ngừa hiện tượng giật đứng khung hình.
- Ứng dụng **không có bất kỳ cơ chế telemetry ngầm** nào gửi dữ liệu này ra bên ngoài thiết bị.

---

## 5. Báo Cáo Lỗ Hổng Bảo Mật (Vulnerability Reporting)

Nếu bạn phát hiện bất kỳ vấn đề bảo mật nào trong mã nguồn hoặc cách ly mạng của CloudPlay:

1. **Vui lòng không công khai lỗ hổng lên issue tracker công cộng trước khi nhóm phát triển khắc phục.**
2. Gửi email chi tiết về mã lỗi, các bước tái hiện (PoC) tới email của người bảo trì dự án hoặc mở một báo cáo bảo mật riêng tư (Private Security Advisory) trên GitHub.
3. Nhóm bảo trì cam kết:
   - Phản hồi xác nhận trong vòng **24-48 giờ**.
   - Cung cấp bản vá khắc phục trong thời gian sớm nhất tùy thuộc vào mức độ nghiêm trọng.

---

## 6. Phiên Bản Được Hỗ Trợ (Supported Versions)

| Phiên Bản | Hỗ Trợ Bảo Mật | Ghi Chú |
| :--- | :--- | :--- |
| 1.0.x (Hiện tại) | Có | Nhận đầy đủ bản vá lỗi mạng & cách ly WebView |
| < 1.0.0 | Không | Vui lòng nâng cấp lên bản mới nhất |
