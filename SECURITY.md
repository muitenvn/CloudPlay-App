# Chính sách Bảo mật / Security Policy

Tài liệu này trình bày các nguyên tắc bảo vệ tài khoản, quyền riêng tư dữ liệu và cách báo cáo lỗ hổng bảo mật cho dự án **CloudPlay Optimizer**.

This document outlines account protection principles, data privacy practices, and how to report security vulnerabilities for **CloudPlay Optimizer**.

---

## 1. Phiên bản được hỗ trợ / Supported Versions

Chỉ phiên bản mới nhất trên nhánh `main` được cập nhật bản vá bảo mật:
Only the latest version on the `main` branch actively receives security updates:

| Phiên bản / Version | Hỗ trợ / Supported |
| :--- | :--- |
| `main` (Mới nhất / Latest) | :white_check_mark: Có / Yes |
| Các bản cũ / Older releases | :x: Không / No |

---

## 2. Nguyên tắc Bảo mật & Quyền riêng tư / Security & Privacy Principles

### 2.1. Bảo vệ tài khoản Microsoft & Xbox / Account Protection
- **Đăng nhập trực tiếp (Direct Sign-In):** Mọi thao tác đăng nhập diễn ra trực tiếp trên trang chính thức của Microsoft (`login.live.com`, `microsoftonline.com`, `xbox.com`) qua kết nối mã hóa HTTPS.
- **Không thu thập mật khẩu (Zero Credential Collection):** Ứng dụng không đọc, không ghi nhật ký và không gửi mật khẩu, mã xác thực hai bước (2FA) hay thông tin cá nhân của bạn ra bất kỳ máy chủ bên thứ ba nào.
- **Direct HTTPS Sign-In:** All authentication happens directly on official Microsoft pages over HTTPS. The app never reads, logs, or transmits your password, 2FA codes, or personal credentials to any third-party server.

### 2.2. Cách ly trang thanh toán & liên kết ngoài / Payment & External Page Isolation
- **Tự động ngắt chuyển tiếp mạng khi thanh toán:** Khi phát hiện trang mua hàng, thanh toán (`cart`, `checkout`, `billing`, `payment`, `redeem`) hoặc tên miền bên ngoài danh sách tin cậy, ứng dụng sẽ tự động tạm dừng chế độ chuyển tiếp mạng hoặc mở bằng trình duyệt mặc định của điện thoại để bảo vệ thông tin thẻ/tài khoản ngân hàng.
- **Automatic Payment Isolation:** When navigating to billing, checkout, or untrusted external pages, network relay automatically pauses or opens in your system browser to keep payment details strictly between your device and Microsoft.

### 2.3. Dữ liệu 100% nội bộ trên máy / 100% Local On-Device Storage
- **Lưu trữ riêng tư (`MODE_PRIVATE`):** Cài đặt ứng dụng, danh sách game yêu thích và lịch sử phiên chơi (thời lượng, FPS, độ trễ mạng) chỉ được lưu cục bộ trong bộ nhớ riêng của ứng dụng trên điện thoại.
- **Không theo dõi từ xa (No Cloud Tracking):** Dự án không tích hợp bộ thu thập hành vi người dùng và không đồng bộ lịch sử chơi lên đám mây. Bạn có thể xóa toàn bộ dữ liệu bất cứ lúc nào trong phần **Cài đặt**.
- **Strictly Local Data:** Settings, saved games, and play session history (duration, average FPS, ping) are stored exclusively on your device (`SharedPreferences` in `MODE_PRIVATE`) and can be cleared anytime in **Settings**.

### 2.4. Ràng buộc an toàn hệ thống / Technical Safeguards
- **Chặn kết nối không mã hóa (`usesCleartextTraffic="false"`):** Toàn bộ kết nối trong ứng dụng bắt buộc dùng HTTPS.
- **Kiểm tra tên miền nghiêm ngặt (Strict Origin Verification):** Cầu nối giao tiếp giữa trang web và ứng dụng (`JavascriptInterface`) cùng quyền truy cập âm thanh/tay cầm chỉ hoạt động khi trang hiện tại thuộc danh sách tên miền chính thức của Microsoft/Xbox qua giao thức `https://`.
- **Chơi công bằng (Fair Play):** Ứng dụng chỉ hỗ trợ hiển thị nút bấm cảm ứng tiêu chuẩn và tối ưu độ mượt hình ảnh; hoàn toàn không có tính năng bắn liên thanh (macro/turbo), khóa tâm hay can thiệp dữ liệu trò chơi.
- **System Safeguards:** Enforces HTTPS-only traffic (`usesCleartextTraffic="false"`), strict domain boundary validation for WebView bridges and permissions, safe parameter quoting, and a strict Fair Play policy (no macros, turbo fire, or game data modification).

---

## 3. Báo cáo Lỗ hổng Bảo mật / Reporting a Vulnerability

Nếu bạn phát hiện lỗi bảo mật (ví dụ: rò rỉ phiên đăng nhập, vượt qua kiểm tra tên miền tin cậy, hoặc lỗi thực thi mã trong WebView), vui lòng **không công khai lỗi ngay trên mục Issues chung**:

If you discover a security vulnerability (such as session leakage, origin validation bypass, or WebView injection), please **do not open a public issue immediately**:

1. Sử dụng tính năng **Report a vulnerability** (Private Security Advisory) trong tab **Security** của kho lưu trữ GitHub này. / Use the **Report a vulnerability** button under the **Security** tab of this GitHub repository.
2. Mô tả ngắn gọn các bước tái hiện lỗi và mức độ ảnh hưởng. / Provide clear steps to reproduce the issue and its potential impact.
3. Nhóm phát triển sẽ phản hồi trong vòng **48 giờ** và phối hợp phát hành bản vá sớm nhất. / We will review and respond within **48 hours** to coordinate a fix.
