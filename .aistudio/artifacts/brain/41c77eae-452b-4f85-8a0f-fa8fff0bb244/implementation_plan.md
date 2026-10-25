# Kế Hoạch Nâng Cấp Giao Diện & Trải Nghiệm Theo Video Soát Lỗi (9 Mục)

## 1. Nguyên Tắc Thực Hiện & Cam Kết
- **Sửa từng phần, không viết lại màn hình**: Giữ nguyên phong cách neon xanh tối (`XboxNeonGreen`, `XboxDark`, `XboxSurface`), giữ đầy đủ mọi tính năng và công tắc hiện có.
- **Tương thích màn hình nhỏ (`360dp`) & chữ hệ thống phóng to**: Khắc phục triệt để các lỗi thiếu `Modifier.weight(1f)` trong `Row` khiến cột nút bị ép thành vệt xanh dọc `2dp` và khung đen trống lớn.
- **Kiểm soát số dòng code**: Mọi file chỉnh sửa đều giữ nguyên hoặc tăng nhẹ số dòng để bổ sung bố cục co giãn, xử lý ảnh bìa lỗi và nhãn kết luận phiên chơi; không rút gọn bất kỳ file nào quá 5%.

---

## 2. Chi Tiết Thực Hiện Từng Mục (1 → 9)

### Mục 1 — Thanh đầu trang (`DashboardScreen.kt`)
- **Nguyên nhân**: Khu vực `actions` của `TopAppBar` đang đặt 5 mục trên cùng một hàng ngang (Ping, Ngôn ngữ, Dịch, Cổng tải Web/QR, Hướng dẫn), khiến trên màn `360dp` chip Ping ép sát vào chữ `"CLOUDPLAY"`, đồng thời chiều cao nút chỉ `30.dp`.
- **Cách sửa**:
  - Giữ tối đa **3 mục hiện sẵn** trên thanh đầu trang: **Chip Ping**, **Ngôn ngữ (`VI/EN`)**, và **Nút Dịch trong game**, kèm nút mở menu phụ **`"⋯"`**.
  - Đưa 2 mục **Cổng tải Web & Mã QR** và **Hướng dẫn sử dụng** vào `DropdownMenu` của nút **`"⋯"`**.
  - Đảm bảo vùng chạm các nút hành động trên thanh đầu trang đạt chuẩn tối thiểu **`48.dp`** (`Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)`), giúp chữ `"CLOUDPLAY"` luôn hiển thị đủ và thoáng trên màn `360dp`.

### Mục 2 — Trang máy chủ (`ServersScreen.kt`)
- **Nguyên nhân**:
  1. Ở thẻ đầu trang (dòng 385), cột bên trái hiển thị `"MÁY CHỦ ĐANG CHỌN: 🇯🇵 Japan Tokyo CloudDC #11"` **thiếu `Modifier.weight(1f)`**, chiếm hết 99% chiều ngang `Row` và ép cụm nút bên phải (`"Chọn tối ưu"` + `"350 Mbps"`) thành một vệt xanh dọc `2dp` cao 12 dòng — tạo ra khung đen trống khổng lồ bên trái.
  2. Hai nút `"Cập Nhật ..."` và `"+ Thêm ..."` bị cắt chữ do chia 3 cột hẹp.
  3. Tên máy chủ trong `ServerCard` (dòng 727) đang khóa `maxLines = 1` nên bị cắt `"..."`.
- **Cách sửa**:
  - Thu gọn thẻ đầu trang thành **thẻ trạng thái gọn gàng**: hiển thị rõ **Máy chủ đang chọn**, **Trạng thái mở khóa**, **Ping hiện tại**, và nút **`"Đo ping"`** (thêm đầy đủ `Modifier.weight(1f)` cho cột thông tin để không bao giờ bị ép dọc).
  - Bố trí hàng nút công cụ (`"Đo ping"`, `"Cập nhật"`, `"Thêm"`) có icon kèm nhãn hiển thị đủ chữ, không bị cắt `"..."`.
  - Trong `ServerCard`, đổi tên máy chủ từ `maxLines = 1` sang **`maxLines = 2`** để tên máy chủ dài tự xuống 2 dòng rõ ràng.

### Mục 3 — Thư viện game (`GameLibraryScreen.kt`)
- **Nguyên nhân**:
  1. Trong `LibraryGameCard` (dòng 726), nền gradient + chữ cái đầu chỉ hiện khi `imageUrl.isNullOrBlank()`, còn khi `AsyncImage` tải ảnh bị lỗi mạng/404 thì không bắt trạng thái lỗi nên chỉ hiện khung đen trống.
  2. Dòng phụ đề `"502 game • 5 Miễn p..."` (dòng 279) khóa `maxLines = 1` và bị 2 nút bên phải ép hẹp.
  3. Khoảng đệm dưới của lưới `LazyVerticalGrid` (`84.dp`) chưa đủ cao khi có nút `"+"` nổi.
- **Cách sửa**:
  - Theo dõi trạng thái tải ảnh của `AsyncImage` (`onError` / `onSuccess`): khi chưa có ảnh hoặc tải lỗi, hiển thị **nền gradient theo tên game kèm chữ cái đầu nổi bật** ở chính giữa thẻ.
  - Cho phép dòng thống kê `"502 game • 5 Miễn phí • ..."` hiển thị đủ (`maxLines = 2`, bố cục linh hoạt với cụm nút `"Làm mới"` / `"+ Thêm"`).
  - Tăng `contentPadding` đáy của `LazyVerticalGrid` lên **`112.dp`** để nút `"+"` nổi không che mất nút `"PLAY"` của hàng game cuối cùng.

### Mục 4 — Trang chủ (`HomeScreen.kt`)
- **Nguyên nhân**: Mục `"Game nổi bật"` (`FeaturedGamesSection`) đặt `LazyRow` bên trong `Column` có `padding(horizontal = 16.dp)` và mỗi thẻ rộng cố định `188.dp`, làm thẻ thứ hai bị cắt ngang ngay giữa nút `"▶ Chơi ngay"`.
- **Cách sửa**:
  - Chuyển `LazyRow` của `"Game nổi bật"` sang tràn lề ngang với `contentPadding = PaddingValues(horizontal = 16.dp)` và điều chỉnh chiều rộng thẻ hợp lý (`160.dp`), đồng thời đảm bảo nút `"Chơi ngay"` và ảnh bìa dự phòng (gradient + chữ cái đầu khi lỗi ảnh) hiển thị nguyên vẹn, cuộn ngang mượt và rõ ràng.

### Mục 5 — Lịch sử phiên (`SessionHistoryScreen.kt`)
- **Nguyên nhân**:
  1. Trong `SessionHistoryItemCard` (dòng 657–711), hàng chứa 3 chip (`Ping`, `FPS`, `Giải mã`) đặt chung `Row` với nút `"Chơi lại"` mà không có `weight` hay tách hàng, khiến 3 chip chiếm hết chiều ngang và ép nút `"Chơi lại"` thành một thanh dọc màu xanh bên phải kèm khoảng trống lớn.
  2. Chi tiết phiên (`SessionDetailDialog`) chưa có nhãn kết luận tổng hợp ở đầu hộp thoại.
- **Cách sửa**:
  - **Làm gọn thẻ phiên chơi (`SessionHistoryItemCard`)**: Đưa nút `"Chơi lại"` lên khu vực góc phải tiêu đề hoặc tách hàng gọn gàng để 3 chip thông số và nút `"Chơi lại"` luôn hiển thị nguyên vẹn trên màn `360dp`, xóa bỏ hoàn toàn thanh dọc lạ và khoảng trống thừa.
  - **Thêm nhãn kết luận lớn ở đầu `SessionDetailDialog`**:
    - Gom toàn bộ ngưỡng vào một đối tượng hằng số `SessionVerdictThresholds` ngay đầu file để dễ chỉnh về sau:
      - `MIN_DURATION_SECONDS = 120L` (Phiên dưới 2 phút hiện **"Chưa đủ dữ liệu"**)
      - **Mượt**: tỉ lệ khung hình rớt `< 3.0%` **VÀ** số lần đứng hình `< 3.0 lần/phút` **VÀ** `avgPingMs < 130` **VÀ** `jitterMs < 20`
      - **Giật nhiều**: tỉ lệ khung hình rớt `>= 8.0%` **HOẶC** số lần đứng hình `>= 6.0 lần/phút` **HOẶC** `avgPingMs >= 200` **HOẶC** `jitterMs >= 35`
      - **Hơi giật**: các trường hợp còn lại.
    - Hiển thị thẻ kết luận lớn kèm dòng tóm tắt rõ ràng các chỉ số thực tế (`% rớt khung hình`, `lần đứng hình/phút`, `ping`, `jitter`).

### Mục 6 — Trang Cài đặt (`SettingsScreen.kt` & `AppSettings.kt`)
- **Cách sửa**:
  - Cho phép các dòng mô tả phụ đang bị cắt `"..."` trong `SettingsScreen.kt` được xuống dòng tự nhiên (`maxLines = 2` hoặc bỏ giới hạn 1 dòng).
  - Căn lại nút **`"Mã QR & Chia sẻ"`** (`maxLines = 1`, đồng bộ độ rộng và đệm ngang) để luôn nằm gọn trên 1 dòng.
  - Trong `AppSettings.kt` (`ScreenRecordQuality`), đổi nhãn rút gọn từ `"540p • 30F"`, `"720p • 30F"`, `"720p • 60F"` thành **`"540p • 30 FPS"`**, **`"720p • 30 FPS"`**, **`"720p • 60 FPS"`**.

### Mục 7 — Bảng HUD mở rộng (`FloatingHud.kt`)
- **Nguyên nhân**: `panelMaxWidth` đang tính theo `screenMaxWidth * 0.38f` (chỉ `172dp` khi màn dọc) và dùng cỡ chữ `7.5sp–9sp`, khiến các nút bị cắt chữ (`"720p•Thấ"`, `"Tải / Xoa / Độ / Tho"`).
- **Cách sửa**:
  - Giữ bảng HUD gọn theo chiều ngang nhưng đảm bảo độ rộng tối thiểu hợp lý (`248.dp–280.dp` tùy màn hình) để không ép vỡ chữ trên màn dọc.
  - Nâng toàn bộ cỡ chữ trong bảng HUD mở rộng lên **tối thiểu `11.sp`**.
  - Chia bố cục bảng HUD mở rộng thành **4 nhóm tiêu đề rõ ràng**:
    1. **Độ phân giải**
    2. **Điều khiển**
    3. **Màu sắc**
    4. **Quay màn hình**
  - Xếp **4 nút hành động cuối trên 1 hàng ngang** (**Tải lại**, **Xoay**, **Đồ họa**, **Thoát**) với **biểu tượng ở trên và nhãn `>= 11.sp` ở dưới**, hiển thị đầy đủ chữ không bị cắt.

### Mục 8 — Câu chữ trung lập, đúng thực tế (`GameOptimizationProfile.kt` & `GameBookmark.kt`)
- **Cách sửa**:
  - Trong `GameOptimizationProfile.kt`: Đổi mô tả của Fortnite từ *"Đã bật chế độ phản hồi nhanh giúp thao tác ngắm bắn và xây dựng nhạy hơn."* sang câu trung lập **`"Bật phản hồi cảm ứng nhanh"`** (và rà soát các mô tả preset khác để giữ văn phong trung lập, ngắn gọn).
  - Trong `GameBookmark.kt`: Khi nạp danh sách game đã lưu từ `SharedPreferences` (`loadGamesArrayFromPrefs`), tự động thay thế các nhãn cũ chứa `"120FPS"` (như `"Hỗ Trợ 120FPS • Free 100%"`) bằng nhãn trung lập **`"Cảm ứng Epic • Miễn phí"`**.

### Mục 9 — Trạng thái trống / đang tải & chuyển cảnh `< 250ms`
- **Cách sửa**:
  - Đồng bộ câu chữ tiếng Việt ngắn gọn, thân thiện cho các trạng thái: **`"Chưa có game"`**, **`"Chưa có lịch sử"`**, **`"Đang đo ping..."`**.
  - Đảm bảo mọi hiệu ứng chuyển cảnh (`tween`) đều **`< 250ms`** (150ms–220ms) và kiểm tra thêm `Settings.Global.ANIMATOR_DURATION_SCALE` của hệ thống để tự động tắt chuyển động khi người dùng bật "Giảm chuyển động" hoặc "Chế độ máy yếu".
