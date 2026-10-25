# Kế hoạch bổ sung Animation chuẩn Xbox Dashboard (< 220ms, Không Lag)

## 1. Nguyên tắc thiết kế chống lag (Zero-Lag Architecture)
Để đảm bảo hiệu ứng mượt mà tuyệt đối và **không gây tụt FPS hay ăn CPU/GPU**, toàn bộ animation mới sẽ tuân thủ 4 nguyên tắc kỹ thuật:
1. **Chỉ biến đổi trên tầng GPU (`Modifier.graphicsLayer`)**: Mọi hiệu ứng thu phóng, trượt nhẹ và mờ dần đều chạy ở bước Vẽ (Draw phase) qua `graphicsLayer { scaleX; scaleY; alpha; translationY }`, **không** làm thay đổi kích thước bố cục (Layout phase) nên không gây giật khi cuộn danh sách.
2. **Không dùng vòng lặp vô hạn (`infiniteTransition`) tốn pin**: Tránh các hiệu ứng nhấp nháy chạy liên tục nền; chỉ kích hoạt animation khi có tương tác chạm, chuyển tab, hoặc dữ liệu thay đổi, và kết thúc dứt điểm trong **150ms – 220ms**.
3. **Tự động tắt trên máy yếu & khi vào trận**: Tự động bỏ qua animation khi người dùng bật **Chế độ máy yếu (`lowEndDeviceMode`)**, bật **Giảm tải GPU (`gpuLoadReductionEnabled`)**, hoặc tắt hiệu ứng hệ thống Android (`ANIMATOR_DURATION_SCALE == 0f`).
4. **Giữ nguyên 100% tính năng và bố cục**: Không xóa hay rút gọn bất kỳ tính năng, công tắc hay màn hình nào hiện có.

---

## 2. Các hạng mục Animation sẽ bổ sung

### A. Bộ công cụ chuyển động dùng chung (`ui/theme/XboxMotion.kt` - File mới)
- **`Modifier.xboxPressScale(...)`**: Hiệu ứng phản hồi xúc giác chuẩn Xbox Dashboard — khi chạm giữ vào thẻ game, thẻ máy chủ hoặc nút hành động chính, phần tử thu nhẹ về `0.96f` trong `110ms` và nảy mượt về `1.0f` (`FastOutSlowInEasing` / lò xo không dao động thừa) khi thả tay.
- **`Modifier.xboxSlideFadeIn(...)`**: Hiệu ứng xuất hiện nhanh (`180ms`, trượt dọc `12dp` + rõ dần `alpha`) chỉ chạy 1 lần khi phần tử vào khung hình, dùng `graphicsLayer` để không làm giật `LazyColumn` / `LazyVerticalGrid`.
- **`rememberXboxMotionEnabled(lowEndMode)`**: Hàm kiểm tra tập trung giúp tắt tức thì mọi hiệu ứng nếu máy yếu hoặc hệ thống tắt animation.

### B. Thanh điều hướng & Chuyển trang (`DashboardScreen.kt`)
- **Chuyển tab kiểu Xbox (`180ms`)**: Kết hợp mờ dần (`fadeIn 180ms` / `fadeOut 120ms`) với dịch chuyển ngang rất nhẹ (`slideInHorizontally` / `slideOutHorizontally` 4% chiều rộng theo hướng tab) giúp cảm giác lướt qua lại giữa *Trang chủ – Thư viện – Máy chủ – Lịch sử – Cài đặt* liền mạch.
- **Thanh điều hướng dưới (Bottom Navigation)**: Biểu tượng tab đang chọn phóng nhẹ lên `1.08f` (`160ms`) và đổi màu mượt (`animateColorAsState 180ms`).
- **Chip Ping trên thanh đầu trang**: Số ms ping nhảy số mượt (`animateIntAsState 180ms`) và chấm màu trạng thái chuyển màu êm khi đổi vùng.

### C. Trang chủ (`HomeScreen.kt`) & Thư viện Game (`GameLibraryScreen.kt`)
- **Thẻ Game nổi bật & Lưới thư viện game**: Áp dụng `xboxPressScale` khi chạm vào thẻ game và nút **"▶ Chơi ngay"**, kèm hiệu ứng xuất hiện mượt (`160ms`) khi lọc danh mục hoặc đổi từ khóa tìm kiếm.
- **Nút "VÀO GAME NGAY" (Hero CTA)**: Phản hồi nhấn đàn hồi nhanh (`140ms`) và hiệu ứng chuyển trạng thái mượt khi đổi máy chủ hoặc chế độ mạng.

### D. Trang Máy chủ (`ServersScreen.kt`) & Lịch sử phiên (`SessionHistoryScreen.kt`)
- **Thẻ Máy chủ & Nút "Đo ping"**: Khi bấm đo ping hoặc chọn vùng máy chủ mới, thẻ máy chủ phản hồi nhấn `xboxPressScale`, viền thẻ đang chọn chuyển màu xanh neon mượt trong `180ms`, và thông số ping trung bình nhảy số mượt (`animateIntAsState`).
- **Thẻ Lịch sử & Cửa sổ Chi tiết phiên**:
  - Mở rộng/thu gọn danh sách hoặc bộ lọc mượt dưới `200ms`.
  - Trong cửa sổ **Chi tiết phiên**, nhãn kết luận (*Mượt / Hơi giật / Giật nhiều*) và các thanh chỉ số hiệu năng xuất hiện với hiệu ứng trượt nhẹ `180ms`.

### E. Bảng HUD nổi trong game (`FloatingHud.kt`)
- **Mở rộng / Thu gọn HUD (`180ms`)**: Khi bấm mở bảng HUD từ viên thuốc thu gọn, khung HUD bung ra mượt mà bằng `scaleIn(initialScale = 0.92f) + fadeIn(180ms)` và thu lại trong `140ms`, tuyệt đối không gây khựng luồng WebRTC bên dưới.
- **Chuyển tab con trong HUD**: Chuyển đổi mượt (`150ms`) giữa chế độ thu gọn và bảng điều khiển đầy đủ.
