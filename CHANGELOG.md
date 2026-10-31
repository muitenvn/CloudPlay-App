# Nhật ký cập nhật / Changelog (CloudPlay)

Tài liệu này ghi nhận các thay đổi và sửa lỗi theo từng phiên bản của ứng dụng.

## [v1.4.0] - Sửa lỗi quay màn hình .MP4, giảm khựng hình và sắp xếp lại giao diện cài đặt
### Sửa lỗi & Cải thiện
- **Giữ nguyên bố cục `<video>` khi đang chơi:** Bỏ việc thay đổi lại thuộc tính CSS `transform` / `contain` trên thẻ `<video>` lúc luồng game đang phát hoặc khi bắt đầu quay màn hình để tránh làm trình duyệt tính toán lại bố cục gây khựng hình.
- **Sửa lỗi bộ lọc làm nét và giảm nạp lại script WebView:** Sửa lỗi Clarity Boost tự bật lại khi chuyển trang, tránh nạp lại script nhiều lần trong `onPageFinished` và bỏ thao tác ghi đĩa không cần thiết trong `AdaptiveQualityManager`.
- **Sắp xếp lại mục Cài đặt:** Đưa nhóm Độ phân giải, FPS và chế độ máy yếu lên đầu; gộp phần chỉnh tỉ lệ màn hình và màu sắc vào chung nhóm hiển thị; chuyển phần quay màn hình xuống mục phụ phía dưới.
- **Sửa lỗi quay video `.mp4` bị mất tiếng:** Thu âm thanh từ WebRTC / AudioContext qua bộ mã hóa AAC-LC & H.264 và lưu ra tệp `.mp4` thay vì `.webm`.
- **Cập nhật kịch bản tạo ghi chú phát hành (Release Notes):** Lấy danh sách thay đổi từ `git diff` giữa hai phiên bản để tóm tắt đúng các mục vừa sửa thay vì lặp lại nội dung của bản cũ.

---

## [v1.3.0] - Hỗ trợ chế độ cảm ứng cho Fortnite và bổ sung tùy chỉnh tay cầm ảo
### Thay đổi trong phiên bản
- **Giảm kích thước bộ đệm WebRTC cho Fortnite:** Giảm thông số `playoutDelayHint` / `jitterBufferTarget` và hỗ trợ tùy chọn 120 FPS trên màn hình có tần số quét cao.
- **Chuyển đổi giữa cảm ứng gốc và tay cầm ảo trong Fortnite:** Khi chọn chế độ cảm ứng của game, ứng dụng ngắt tín hiệu tay cầm giả lập (`gamepaddisconnected`) để giao diện nút chạm của Fortnite hiển thị bình thường; khi bật lại tay cầm ảo sẽ chuyển về bố cục nút bấm trên màn hình.
- **Tùy chỉnh âm thanh Stereo Opus:** Cho phép cấu hình luồng âm thanh Stereo kèm sửa lỗi gói tin (FEC) khi mạng không ổn định.
- **Tắt cử chỉ cuộn trang ngoài ý muốn khi bấm nhiều ngón:** Áp dụng `touch-action: none` trên vùng chơi game và tự đóng thông báo nhắc kết nối tay cầm khi vào bằng cảm ứng.

### Cải thiện tay cầm ảo trên màn hình
- **Hỗ trợ trượt trên cụm phím D-Pad và ABXY:** Cho phép vuốt ngón tay qua các hướng chéo trên D-Pad và giữa các nút ABXY liền kề.
- **Bổ sung nút L3 / R3 và tùy chọn tự chạy nhanh:** Thêm nút L3/R3 rời và tùy chọn kích hoạt L3 khi đẩy hết cỡ cần điều khiển bên trái.
- **Hỗ trợ ngắm bằng cảm biến con quay hồi chuyển (Gyroscope):** Cho phép dùng cảm biến nghiêng của điện thoại để hỗ trợ điều chỉnh hướng nhìn của cần phải.
- **Lưu vị trí kéo thả nút bấm:** Cho phép chọn các mẫu bố cục có sẵn, chỉnh kích thước, độ mờ, độ nhạy và kéo từng cụm nút tới vị trí mong muốn.

---

## [v1.2.0] - Bổ sung giao diện Tiếng Anh, giảm tải quét trang và sửa lỗi dịch vụ VPN
### Giao diện & Ngôn ngữ
- **Hỗ trợ giao diện Tiếng Anh:** Bổ sung bản dịch Tiếng Anh cho các màn hình chính, hộp thoại cài đặt, thanh công cụ nổi và thông báo trạng thái.
- **Tự tắt dịch trang web khi để giao diện Tiếng Anh:** Khi ngôn ngữ ứng dụng là Tiếng Anh, tính năng dịch văn bản trên trang web Xbox sang Tiếng Việt sẽ tự tắt để giữ nguyên văn bản gốc.
- **Thu gọn nút điều khiển nổi sát mép màn hình:** Khi kéo nút nổi sát cạnh trái hoặc phải, nút sẽ thu nhỏ lại để đỡ che màn hình chơi game.
- **Hiển thị trạng thái mở khóa vùng của từng máy chủ:** Kiểm tra mã quốc gia của IP và ghi nhận kết quả mở khóa thực tế (`Đã mở khóa`, `Không mở khóa`, `Chưa thử`, `Sai vùng`) để người dùng dễ chọn lại khi gặp lỗi vùng.

### Hiệu năng & Sửa lỗi
- **Tránh nạp lại script nhiều lần khi trang đang tải:** Chỉ chạy script hỗ trợ một lần khi trang khởi tạo thay vì gọi lặp lại trong `onProgressChanged`.
- **Giảm số lần quét văn bản kiểm tra khóa vùng:** Giới hạn kiểm tra bằng `textContent` tối đa 4 lần cho mỗi đường dẫn và dừng kiểm tra sau khi luồng video đã kết nối.
- **Sửa lỗi kiểm tra IP bị chặn bởi CSP và lỗi dừng Foreground Service:** Chuyển việc kiểm tra địa chỉ IP sang tầng mạng Android (`GeoIpDetector`) và đảm bảo gọi `startForeground` đúng quy trình trong `XboxCloudVpnService`.

---

## [v1.1.0] - Thêm chế độ máy yếu và tạm dừng dịch trang khi vào game
- Thêm tùy chọn **Chế độ máy yếu** trong Cài đặt: giới hạn băng thông ở mức thấp (~5 Mbps), ưu tiên codec H.264 và giảm bớt hiệu ứng chuyển động trên giao diện.
- Tự động tạm dừng việc quét dịch văn bản trên trang web ngay khi luồng video trò chơi bắt đầu phát.

---

## [v1.0.0] - Phiên bản đầu tiên
- Đóng gói giao diện WebView truy cập Xbox Cloud Gaming kèm tính năng kết nối proxy/VPN mở khóa vùng, tay cầm ảo trên màn hình, bộ lọc tăng độ nét cơ bản và danh sách phím tắt mở game.
