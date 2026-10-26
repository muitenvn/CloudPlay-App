# Cập Nhật Nhạc Nền (Mặc Định Tắt, Phát Tay, Phát Trong Game) & Xóa Dòng Ghi Nguồn Nhạc

Bản kế hoạch này thực hiện đầy đủ 2 nhóm yêu cầu kiểm duyệt: (1) Điều chỉnh hành vi mặc định của nhạc nền, thêm nút phát/dừng thủ công, công tắc phát nhạc khi đang chơi game không ảnh hưởng âm thanh WebRTC; và (2) Xóa toàn bộ dòng chữ ghi nguồn nhạc trong giao diện ứng dụng.

## Quyết Định & Phản Biện Kỹ Thuật

> [!IMPORTANT]
> **Tất cả các mục trong yêu cầu đều hợp lý và sẽ được thực hiện đầy đủ, kèm 2 lưu ý kỹ thuật quan trọng để đảm bảo không lỗi:**
> 1. **Giữ nguyên cấu hình cho người dùng cũ (Mục 1):** Để chỉ đổi mặc định (`Tự động bật nhạc = TẮT`, `Âm lượng = 8%`) cho người dùng mới mà không ghi đè lựa chọn của người dùng đã chỉnh trước đó, `SettingsRepository` sẽ kiểm tra khóa riêng hoặc chỉ khởi tạo giá trị mặc định mới khi khóa chưa được người dùng lưu thủ công.
> 2. **Xử lý AudioFocus & Quay màn hình khi phát nhạc trong game (Mục 2 & 3):**
>    - Khi dùng `AudioManager.AUDIOFOCUS_GAIN` chuẩn, Chromium `WebView` (chạy WebRTC xCloud) có thể tự động tạm dừng hoặc giảm tiếng game vì tưởng có trình phát nhạc chính chiếm quyền. Việc chuyển sang **`AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`** kết hợp `AudioAttributes.USAGE_MEDIA` giúp nhạc nền phát song song với âm thanh WebRTC của game mà không bao giờ làm mất tiếng game.
>    - Với tính năng **Quay màn hình**: `AudioAttributes.ALLOW_CAPTURE_BY_NONE` (Android 10+) đã chặn hệ thống thu âm luồng `MediaPlayer` này vào file `.mp4`. Nhờ đó, kể cả khi người dùng bật *"Phát nhạc khi đang chơi"* và quay video màn hình, nhạc nền vẫn phát ở loa ngoài cho người chơi nghe nhưng **tệt đối không bị thu lẫn vào video quay màn hình**.

---

## 1. Tổng Quan Thay Đổi

- **Mặc định mới cho người dùng mới (`AppSettings.kt`):**
  - **Tự động bật nhạc khi mở app (`backgroundMusicEnabled`):** Mặc định **TẮT (`false`)** (giữ nguyên giá trị đã lưu nếu người dùng cũ đã từng chỉnh).
  - **Âm lượng nhạc nền mặc định (`backgroundMusicVolume`):** Mặc định **8% (`0.08f`)** (giữ nguyên nếu người dùng cũ đã lưu).
  - **Phát nhạc khi đang chơi (`backgroundMusicPlayInGame`):** Công tắc mới, mặc định **BẬT (`true`)**.
- **Điều khiển phát/dừng thủ công & phát trong game (`BackgroundMusicManager.kt`):**
  - Thêm trạng thái `isPlaying: StateFlow<Boolean>` và hàm bật/tắt thủ công (`toggleManualPlayback()` / `setManualPlayback()`), cho phép người dùng bấm nút **Phát / Dừng** ngay trong Cài đặt kể cả khi chưa bật công tắc *"Tự động bật nhạc khi mở app"*.
  - Khi vào màn hình chơi game (`CloudBrowserScreen`) và khi stream bắt đầu:
    - Nếu **Chế độ máy yếu** đang bật **HOẶC** công tắc *"Phát nhạc khi đang chơi"* đang **TẮT**: nhạc tự dừng mượt với hiệu ứng **fade-out 800ms** rồi giải phóng `MediaPlayer`.
    - Nếu công tắc *"Phát nhạc khi đang chơi"* đang **BẬT** (và không ở chế độ máy yếu) và nhạc đang phát: giữ nguyên `MediaPlayer` phát tiếp ở đúng mức âm lượng hiện tại, không ngắt quãng.
  - Đổi yêu cầu AudioFocus sang `AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` và giữ `AudioAttributes.ALLOW_CAPTURE_BY_NONE` để không cướp âm thanh WebRTC của game và không bị thu vào video quay màn hình.
- **Giao diện Cài đặt & Xóa dòng ghi nguồn nhạc (`SettingsScreen.kt`):**
  - Đổi nhãn công tắc thành **"Tự động bật nhạc khi mở app"** (kèm nút nhỏ **Phát / Dừng** ngay bên cạnh để bật/tắt tay tức thì).
  - Thêm công tắc **"Phát nhạc khi đang chơi"** (mặc định BẬT, tự tắt khi ở Chế độ máy yếu).
  - **Xóa cụm `"🎵 Nhạc nền do AI tạo (Gemini) • "`** ở thẻ Nhạc nền, chỉ giữ lại dòng thông tin hữu ích: `"Không thu vào video quay màn hình"` / `"Excluded from screen recording"`.
  - **Xóa hoàn toàn dòng `"Nhạc nền do AI tạo (Gemini)"`** ở thẻ Cập nhật / Giới thiệu cuối màn hình Cài đặt.
  - Kiểm tra toàn bộ dự án (`docs/index.html`, `assets/web/index.html`, strings) đảm bảo không còn chuỗi ghi nguồn nhạc nào và file thừa `public/assets/aistudio/window_seat_view_*.mp3` đã được dọn sạch.

---

## 2. Sơ Đồ Luồng Hoạt Động Nhạc Nền Mới

```
┌──────────────────────────────────────────────────────────────────────────┐
│                     KHỞI ĐỘNG APP (DashboardScreen)                      │
└────────────────────────────────────┬─────────────────────────────────────┘
                                     │
                 ┌───────────────────┴───────────────────┐
                 ▼                                       ▼
   [Tự động bật nhạc = BẬT]                [Tự động bật nhạc = TẮT (Mặc định)]
   & Không bật Chế độ máy yếu               Người dùng có thể bấm nút [▶ Phát]
                 │                          nhỏ trong Cài đặt để nghe thủ công
                 ▼                                       │
       Fade-in 800ms (8% vol) ◄──────────────────────────┘
       (AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK + ALLOW_CAPTURE_BY_NONE)
                 │
                 ▼
┌──────────────────────────────────────────────────────────────────────────┐
│              VÀO MÀN CHƠI GAME (CloudBrowserScreen / Stream)             │
└────────────────────────────────────┬─────────────────────────────────────┘
                                     │
        ┌────────────────────────────┴────────────────────────────┐
        ▼                                                         ▼
[Phát nhạc khi đang chơi = BẬT]                   [Phát nhạc khi đang chơi = TẮT]
& Không bật Chế độ máy yếu                        HOẶC đang bật Chế độ máy yếu
& Nhạc đang phát                                                  │
        │                                                         ▼
        ▼                                               Fade-out 800ms ➔
Giữ nguyên nhạc phát tiếp ở cùng âm lượng               stop() & release()
(Không làm giảm tiếng game, không lọt vào video quay)   Giải phóng RAM
```
