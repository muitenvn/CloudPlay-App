# CloudPlay — Ứng dụng hỗ trợ chơi Xbox Cloud Gaming trên Android

**CloudPlay** là ứng dụng độc lập, gọn nhẹ dành cho điện thoại Android giúp mở và trải nghiệm **Xbox Cloud Gaming** (`xbox.com/play`) thuận tiện hơn, tích hợp sẵn tay cầm cảm ứng tùy chỉnh, thông số kỹ thuật thực tế, dịch giao diện và bộ công cụ mạng tùy chọn (lấy cảm hứng từ dự án mã nguồn mở [better-xcloud](https://github.com/redphx/better-xcloud)).

**CloudPlay** is a lightweight, independent Android application built for playing **Xbox Cloud Gaming** (`xbox.com/play`) on mobile phones, featuring customizable touch controls, real-time stream telemetry, menu localization, and optional network routing tools (inspired by the open-source [better-xcloud](https://github.com/redphx/better-xcloud) project).

- **Trang chủ & Tải APK / Website & Download:** [https://muitenvn.github.io/CloudPlay-App/](https://muitenvn.github.io/CloudPlay-App/)
- **Điều khoản & Quyền riêng tư (15 mục) / Terms & Privacy:** [https://muitenvn.github.io/CloudPlay-App/terms.html](https://muitenvn.github.io/CloudPlay-App/terms.html)
- **Các phiên bản phát hành / Releases:** [GitHub Releases](https://github.com/muitenvn/CloudPlay-App/releases)

---

## Tính năng chính / Core Features

- **Tay cầm cảm ứng & Tay cầm rời (Custom Touch & Physical Controls):** Hỗ trợ đầy đủ các nút bấm tiêu chuẩn Xbox, cho phép kéo thả vị trí, chỉnh kích thước, độ trong suốt, độ rung phản hồi và lưu bố cục riêng cho từng trò chơi. Tương thích tay cầm Bluetooth và USB-C.
- **Hình ảnh & Thông số thực tế (Stream Tuning & Real Telemetry):** Tùy chỉnh độ phân giải mục tiêu (720p/1080p), giới hạn băng thông, bộ lọc độ nét, quay màn hình và hiển thị FPS, độ trễ (RTT ping), jitter, bitrate thực tế đọc trực tiếp từ kết nối WebRTC.
- **Đa ngôn ngữ & Dịch giao diện (12 Languages & Menu Translation):** Giao diện ứng dụng và các mục menu trên trang Xbox hỗ trợ hiển thị bằng tiếng Việt, tiếng Anh và 10 ngôn ngữ phổ biến khác.
- **Công cụ mạng & Bảo vệ tài khoản (Optional Network Tools & Account Safety):** Đo độ trễ thực tế, hỗ trợ máy chủ proxy tùy chọn do người dùng tự quyết định và tự động ngắt kết nối trung gian khi mở trang đăng nhập hoặc thanh toán của Microsoft.
- **Thư viện & Lịch sử phiên chơi (Game Library & Session History):** Tra cứu nhanh danh mục trò chơi đám mây, đánh dấu trò chơi yêu thích và theo dõi thống kê chất lượng mạng của các phiên chơi gần đây.

---

## An toàn & Quyền riêng tư / Safety & Privacy

Chi tiết đầy đủ được công bố tại [Chính sách Bảo mật (SECURITY.md)](SECURITY.md) và [Trang Điều khoản & Quyền riêng tư](https://muitenvn.github.io/CloudPlay-App/terms.html):

1. **An toàn tài khoản:** Người dùng đăng nhập trực tiếp trên trang chính thức của Microsoft (`login.live.com`, `xbox.com`). Ứng dụng hoàn toàn không đọc, không thu thập và không lưu mật khẩu hay mã xác thực (OTP/2FA).
2. **Bảo vệ khi thanh toán:** Tự động ngắt máy chủ trung gian khi mở các trang mua hàng, nạp mã hoặc quản lý tài khoản của Microsoft.
3. **Dữ liệu lưu cục bộ trên máy:** Mọi cài đặt, danh sách game yêu thích, lịch sử phiên chơi và nhật ký kỹ thuật chỉ lưu trong bộ nhớ riêng của ứng dụng trên điện thoại của người dùng; không có máy chủ thu thập dữ liệu cá nhân và không gắn quảng cáo.
4. **Chơi công bằng (Fair Play):** Chỉ cung cấp nút bấm cảm ứng tiêu chuẩn 1:1 và tùy chỉnh hiển thị; hoàn toàn không có tính năng gian lận (cheat), bắn tự động (macro/turbo) hay can thiệp vào trò chơi.

---

## Lưu ý pháp lý & Liên hệ / Legal Notice & Contact

- **Dự án độc lập:** CloudPlay là dự án cá nhân độc lập, **không** liên kết, không thuộc sở hữu và không được Microsoft Corporation, Xbox hay Epic Games bảo trợ hoặc cấp phép. Mọi tên gọi và nhãn hiệu thuộc về chủ sở hữu tương ứng.
- **Ghi công dữ liệu:** Định vị vị trí IP sử dụng cơ sở dữ liệu miễn phí từ [DB-IP.com](https://db-ip.com) theo giấy phép **CC BY 4.0**.
- **Nhà phát triển / Developer:** Nguyễn Thế Ngọc (`muitenvn`) — Email: [cloudplayxbox@gmail.com](mailto:cloudplayxbox@gmail.com)
