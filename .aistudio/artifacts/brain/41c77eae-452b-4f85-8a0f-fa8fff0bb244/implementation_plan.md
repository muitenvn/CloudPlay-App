# Kế hoạch tối ưu Giao diện Trong Game, Chỉnh Control & Màn hình Loading (Phong cách Gaming Đơn giản & Thực dụng)

## 1. Định hướng thiết kế
- **Đơn giản, gọn gàng, đậm chất Gaming thực dụng**: Sử dụng tông nền tối lì (`#0E1512` / `#141E19`), chữ trắng/xám rõ nét và điểm nhấn xanh Xbox (`#107C10` / `#3FB950`) vừa đủ.
- **Loại bỏ sự rườm rà / "quá tương lai"**: Không dùng hiệu ứng phát sáng lòe loẹt, không dùng viền đa sắc cầu kỳ hay nhãn kỹ thuật rối mắt. Mọi thành phần đều ưu tiên **dễ nhìn, dễ bấm, ít chiếm diện tích màn hình**.

---

## 2. Chi tiết nâng cấp theo từng khu vực

### A. Giao diện khi vào game (`FloatingHud.kt`)
1. **Viên thuốc HUD tối giản & Tự động mờ khi chơi**:
   - Thu gọn viên thuốc HUD thành một thanh nhỏ gọn, nền tối lì, chỉ hiện đúng thông số cần thiết (`Ping • FPS`) với chấm màu trạng thái đơn giản.
   - Sau 3.5 giây không chạm, viên thuốc tự động mờ dịu xuống (`alpha ~ 0.38f`) để người chơi tập trung hoàn toàn vào hình ảnh game.
2. **Bảng mở rộng HUD gọn thành các thẻ đơn giản**:
   - Bỏ các khung trang trí phức tạp; gom gọn thành 3 phần rõ ràng, dễ bấm:
     - **Dải thông số ngắn gọn**: Ping, FPS, Trạng thái mạng và Pin.
     - **Hàng nút công cụ nhanh**: Mạng/VPN, Tay cầm ảo, Độ nét, Dịch, Xoay màn hình, Quay màn hình.
     - **Chọn nhanh chất lượng & Thoát/Tải lại**: Nút chọn 720p / 1080p / Tự động và nút Về trang chủ.

### B. Giao diện chỉnh Control tay cầm ảo (`VirtualGamepad.kt`)
1. **Lưới căn chỉnh (Grid) đơn giản & Tọa độ khi kéo từng nút**:
   - Khi bật chế độ **Sửa vị trí**, hiển thị **lưới ô vuông mảnh, mờ dịu** trên nền tối cùng **2 đường tâm ngang/dọc** để dễ gióng thẳng hàng các nút trái/phải mà không gây rối mắt.
   - Hiển thị nhãn nhỏ gọn ngay trên thanh công cụ và cạnh nút đang kéo: **`Tên nút • Tọa độ (X, Y) • Cỡ %`** (ví dụ: `Nút A • X: +15, Y: -10 • 110%`).
2. **Thanh công cụ sửa nút (Inspector) gọn và trực quan**:
   - Thiết kế dạng thanh ngang đơn giản ở mép trên (có nút thu gọn `▲/▼` nếu muốn nhìn góc trên màn hình):
     - Hàng 1: Tên nút đang chọn + Nút **`- Cỡ` / `+ Cỡ`** + Thanh trượt kích thước + **`↺ Nút này`** (đặt lại riêng nút đang chọn) + **`🗑 Xóa nút`**.
     - Hàng 2: **`# Lưới`** (bật/tắt lưới), **`⇄ Tách/Gộp ABXY`**, **`⇄ Tách/Gộp D-Pad`**, **`＋ Thêm nút`**, **`↺ Mặc định`**, **`✓ Xong`**.

### C. Màn hình Loading đơn giản, chuẩn Console (`InGameLoadingOverlay.kt`)
1. **Bỏ hiệu ứng màu mè, tập trung vào Ảnh bìa & Tiến trình vào game**:
   - Nền tối sạch sẽ (bỏ các vòng sáng chuyển động rườm rà).
   - **Khung ảnh bìa game (Banner)** bo góc gọn gàng kèm tên game và nhãn FPS.
   - **Thanh tiến trình đơn giản, rõ ràng**: Hiển thị trạng thái kết nối hiện tại, phần trăm `%` và 4 bước ngắn gọn (`Kết nối` → `Định tuyến` → `Luồng 60FPS` → `Sẵn sàng`).
   - **Thông tin Máy chủ + Tay cầm + Mẹo ngắn** gom gọn trong một khung đơn giản, đi cùng nút **`Vào Trận Ngay`** rõ ràng, dễ bấm.
