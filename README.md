# CloudPlay (Trình duyệt hỗ trợ Xbox Cloud Gaming trên Android)

**CloudPlay** là một ứng dụng Android nhỏ gọn đóng gói trang web `xbox.com/play` bên trong WebView, kèm theo một số tiện ích cơ bản hỗ trợ người dùng chơi Xbox Cloud Gaming trên điện thoại (lấy cảm hứng từ tiện ích mở rộng [Better-xCloud](https://github.com/redphx/better-xcloud) theo giấy phép MIT).

**CloudPlay** is a lightweight Android WebView wrapper for `xbox.com/play` with basic helper utilities for playing Xbox Cloud Gaming on mobile devices (inspired by the open-source [Better-xCloud](https://github.com/redphx/better-xcloud) project under the MIT License).

---

## Cách dùng cơ bản / How to Use

1. **Đăng nhập tài khoản Microsoft:**
   - Mở ứng dụng và đăng nhập tài khoản Microsoft trực tiếp trên trang `xbox.com/play`.
   - **Fortnite** được Microsoft cho phép chơi miễn phí trên đám mây (chỉ cần tài khoản Microsoft thường). Các trò chơi khác trong danh mục yêu cầu tài khoản có gói **Xbox Game Pass**.
2. **Mở khóa vùng khi vào game (nếu ở Việt Nam hoặc khu vực chưa hỗ trợ):**
   - Xbox Cloud Gaming hiện chưa mở chính thức tại Việt Nam nên cần đi qua máy chủ trung gian (proxy/VPN) ở Nhật Bản, Hàn Quốc hoặc Mỹ lúc bấm mở game.
   - Sau khi hình ảnh trò chơi đã hiện lên màn hình, bạn có thể tắt VPN (hoặc bật tùy chọn *Tự tắt VPN khi vào game*) để kết nối truyền hình ảnh chạy trực tiếp qua mạng của bạn, đỡ bị trễ hơn so với việc đi vòng qua proxy suốt lúc chơi.
3. **Điều khiển trò chơi:**
   - **Tay cầm rời:** Cắm cáp USB-C hoặc kết nối Bluetooth với tay cầm Xbox, PlayStation hoặc tay cầm điện thoại để chơi bình thường.
   - **Tay cầm cảm ứng:** Nếu không có tay cầm rời, chạm vào nút tròn nổi trên màn hình khi đang ở trong game để bật bộ nút bấm cảm ứng ảo (hoặc chọn chế độ cảm ứng riêng của trò chơi nếu trò chơi đó có hỗ trợ).
4. **Chỉnh độ phân giải và xử lý lỗi thường gặp:**
   - Trong mục **Cài đặt**, bạn có thể giới hạn mức độ phân giải/băng thông (720p hoặc 1080p) tùy theo mạng Wi-Fi hay 4G/5G đang dùng.
   - Nếu gặp thông báo giới hạn vùng khi mở game, hãy sang tab **Máy chủ** chọn một máy chủ khác còn hoạt động hoặc dùng một ứng dụng VPN bên ngoài rồi thử lại.
   - Nếu màn hình bị đứng hoặc mất tiếng, mở menu nút nổi trong game và bấm **Tải lại** để kết nối lại phiên đang chơi.

---

## Các tính năng có trong ứng dụng / Features

- **Trình duyệt WebView cho `xbox.com/play`:** Lưu danh sách phím tắt mở nhanh game, lọc danh mục cơ bản và xem lại lịch sử thời lượng các phiên đã chơi trên máy.
- **Hỗ trợ đổi máy chủ trung gian (Proxy / VPN):** Tích hợp danh sách máy chủ công cộng kèm công cụ đo độ trễ (ping) cơ bản và hỗ trợ nhập proxy HTTP/SOCKS5 thủ công để vượt bước kiểm tra vùng khi bắt đầu vào game.
- **Bàn phím / Tay cầm ảo trên màn hình:** Hiển thị các nút bấm ảo cơ bản (ABXY, D-Pad, 2 cần xoay, LB/RB/LT/RT), cho phép kéo đổi vị trí nút, chỉnh kích thước, độ mờ và hỗ trợ nghiêng máy (Gyro) cơ bản. Không có tính năng bắn liên thanh (Turbo/Macro) hay can thiệp vào trò chơi.
- **Tùy chỉnh hiển thị & Dịch menu trang web:** Cho phép giới hạn mức băng thông WebRTC, chỉnh độ sáng/tương phản CSS cơ bản, và dịch các nút menu tiếng Anh ngoài sảnh chờ của trang web Xbox sang tiếng Việt (kết hợp từ điển có sẵn và Google Translate).
- **Quay màn hình cơ bản:** Hỗ trợ ghi lại màn hình phiên chơi ra tệp `.mp4` lưu trên máy.

---

## Bản phát hành & Cập nhật (Releases)

- **Cách đóng gói bản phát hành:** Mỗi khi có thay đổi mã nguồn trên nhánh chính hoặc tạo thẻ phiên bản (`v*`), GitHub Actions sẽ tự động biên dịch tệp cài đặt APK và đăng lên mục **GitHub Releases**.
- **Mô tả bản phát hành (Release Notes):** Nội dung mô tả của từng bản phát hành chỉ liệt kê ngắn gọn các thay đổi, sửa lỗi thực tế trong mã nguồn (`git diff` và lịch sử commit) của bản dựng đó, kèm theo mã kiểm tra toàn vẹn tệp `SHA-256`.
- **Cài đặt cập nhật:** Các bản APK được ký bằng cùng một khóa ký cố định của dự án để người dùng có thể tải tệp APK mới về và cài đè lên phiên bản cũ mà không cần gỡ ứng dụng hay đăng nhập lại từ đầu.

---

## Bảo mật & Quyền riêng tư / Security & Privacy

Chi tiết được trình bày tại [SECURITY.md](SECURITY.md):

1. **Đăng nhập trực tiếp trên trang Microsoft:** Việc đăng nhập diễn ra trên tên miền của Microsoft (`login.live.com`, `xbox.com`). Ứng dụng không thu thập mật khẩu của người dùng.
2. **Lưu ý về máy chủ công cộng:** Các máy chủ lấy từ danh sách công cộng (như VPNGate, ProxyScrape) do bên thứ ba vận hành và chỉ được ứng dụng kiểm tra khả năng kết nối. Không sử dụng các máy chủ công cộng này cho các giao dịch nhạy cảm; ứng dụng cũng tự động ngắt chuyển tiếp khi phát hiện các đường dẫn thanh toán của Microsoft.
3. **Lưu trữ nội bộ trên thiết bị:** Cài đặt, danh sách game yêu thích và lịch sử phiên chơi chỉ lưu trong bộ nhớ ứng dụng trên thiết bị của bạn.

---

## Miễn trừ trách nhiệm / Disclaimer

- **Dự án cộng đồng độc lập:** Ứng dụng này không liên kết, không được tài trợ hay phê duyệt bởi Microsoft Corporation hay Xbox. Các nhãn hiệu *Xbox*, *Xbox Cloud Gaming* và *Fortnite* thuộc về chủ sở hữu tương ứng.
- **Trách nhiệm sử dụng:** Người dùng tự chịu trách nhiệm tuân thủ Điều khoản Dịch vụ của Microsoft khi sử dụng dịch vụ.
- **Giấy phép mã nguồn mở:** Phát hành theo [Giấy phép MIT (MIT License)](https://opensource.org/licenses/MIT), cung cấp "nguyên trạng" (AS-IS) không kèm theo bất kỳ cam kết bảo đảm nào.

---

## Hướng dẫn tự biên dịch / Build Instructions

1. Mở thư mục dự án bằng **Android Studio** (cài đặt sẵn Android SDK 34 trở lên).
2. Đồng bộ Gradle (`Sync Project with Gradle Files`) và chạy cấu hình `app` trên thiết bị Android (Android 8.0 trở lên), hoặc chạy lệnh `gradle assembleDebug` từ dòng lệnh.
