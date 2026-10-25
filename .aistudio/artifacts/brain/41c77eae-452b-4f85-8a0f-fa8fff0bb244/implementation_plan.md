# Bản Kế Hoạch Khắc Phục Lỗi & Nâng Cấp Bảo Mật Cập Nhật APK (SHA-256 Verification)

Bản kế hoạch này tổng hợp đầy đủ các mục rà soát từ bộ phận kiểm tra độc lập kèm phản biện kỹ thuật đã được xác nhận, đồng thời triển khai luồng tải APK trực tiếp trong ứng dụng qua máy chủ trung gian tốc độ cao (`ghfast.top`) với cơ chế đối chiếu mã băm **SHA-256 chính thức từ GitHub API** trước khi mở bộ cài đặt hệ thống.

## Quyết Định Đã Xác Nhận & Phản Biện Kỹ Thuật

> [!IMPORTANT]
> **Các quyết định kỹ thuật và phản biện đã được xác nhận:**
> - **Mục 1 (Phản biện chuyển đổi công tắc 1 lần khi mở app)**:
>   - **VPN thông minh (`smartVpnEnabled` & `autoBypassEnabled`)**: Thực hiện chuyển đổi 1 lần duy nhất (`migrated_unified_switches_v1`): nếu 1 trong 2 giá trị cũ đang bật (`smartVpnEnabled || autoBypassEnabled`) thì bật cả hai để khớp trạng thái thực tế.
>   - **Chế độ máy yếu & Tiết kiệm pin (`lowEndDeviceMode` & `gpuLoadReductionEnabled`)**: **Phản biện việc gộp theo phép HOẶC (`||`)** vì `gpuLoadReductionEnabled` mặc định đang là `true` cho mọi thiết bị trong khi `lowEndDeviceMode` mặc định là `false`. Nếu gộp theo phép HOẶC sẽ khiến 100% thiết bị bị ép vào Chế độ máy yếu (bị khóa cứng băng thông 5 Mbps, hạ độ phân giải 75% và tự tắt dịch Tiếng Việt). Giải pháp đã thống nhất: giữ theo giá trị `lowEndDeviceMode` hiện tại (`gpuLoadReductionEnabled = lowEndDeviceMode`) và đồng bộ hiển thị công tắc theo `config.lowEndDeviceMode && config.gpuLoadReductionEnabled`.
> - **Mục 2 & 3 (Màn hình chờ & Bộ nhớ đệm `CloudBrowserScreen`)**:
>   - Gỡ bỏ đoạn gán sớm `isPageLoading = false` ở mốc `75%` trong `onPageCommitVisible`, chỉ giữ **1 ngưỡng duy nhất `80%`** cho màn hình chờ kèm nút Bỏ qua.
>   - Gỡ bỏ lệnh `clearCache(false)` khi đóng phiên chơi để giữ nguyên tốc độ tải tài nguyên ở các lần mở game tiếp theo.
> - **Mục 4 (Danh sách tên miền HTTPS hợp lệ khi tải APK)**:
>   - Cho phép kết nối HTTPS tới `ghfast.top`, `github.com` và tên miền lưu trữ tài nguyên chuyển hướng `302` chính thức của GitHub là `*.githubusercontent.com` (`objects.githubusercontent.com`, `release-assets.githubusercontent.com`), giới hạn dung lượng tối đa **150 MB**.
> - **Mục 4 & 6 (Mở trình cài đặt APK trực tiếp)**:
>   - Khai báo `REQUEST_INSTALL_PACKAGES` và `FileProvider` nội bộ trong `AndroidManifest.xml` để sau khi file APK trong bộ nhớ đệm riêng (`cacheDir`) vượt qua kiểm tra khớp 100% mã SHA-256 sẽ mở thẳng trình cài đặt hệ thống của Android.

---

## 1. Tổng Quan & Giá Trị Cốt Lõi

- **Mục tiêu**: Đảm bảo tính nhất quán tuyệt đối của các công tắc đã gộp trong phần Cài đặt, tối ưu tốc độ mở lại game và bảo vệ người dùng khỏi nguy cơ tập tin cài đặt bị thay đổi hoặc hỏng khi tải qua máy chủ trung gian (`ghfast.top`).
- **Giá trị mang lại**:
  - Người dùng tại Việt Nam vẫn tận hưởng tốc độ tải cập nhật nhanh qua `ghfast.top`, nhưng mọi tập tin `.apk` đều phải khớp từng ký tự mã băm **SHA-256 gốc lấy trực tiếp từ GitHub (`api.github.com` / `github.com`)** trước khi được phép cài đặt.
  - Tự động chuyển sang tải trực tiếp từ GitHub nếu máy chủ trung gian gặp lỗi hoặc không phản hồi dữ liệu sau 15 giây.

---

## 2. Trải Nghiệm Người Dùng & Giao Diện Hộp Thoại Cập Nhật

- **Luồng Tải & Kiểm Tra Tự Động Trong Hộp Thoại (`UpdateAvailableDialog`)**:
  1. Khi vào ứng dụng và phát hiện bản mới, hộp thoại cập nhật tự động mở kèm dòng ghi chú rõ ràng: *"Tải nhanh qua máy chủ trung gian; file được kiểm tra trước khi cài."*
  2. Khi người dùng nhấn **"⚡ Tải nhanh APK"**, ứng dụng tải tập tin `.apk` vào thư mục bộ nhớ đệm riêng (`cacheDir/updates`), hiển thị thanh tiến độ phần trăm (`%`), số MB đã tải và nút **"Hủy"** ngay trên hộp thoại.
  3. Nếu `ghfast.top` báo lỗi hoặc không truyền dữ liệu sau **15 giây**, ứng dụng tự động chuyển sang tải trực tiếp từ `github.com` mà không làm gián đoạn trải nghiệm.
  4. Ngay khi tải xong, ứng dụng tính toán mã băm SHA-256 của file và đối chiếu từng ký tự với mã SHA-256 chính thức từ GitHub:
     - **Nếu khớp 100%**: Tự động mở trình cài đặt hệ thống Android qua `FileProvider`.
     - **Nếu không khớp hoặc không lấy được mã chính thức**: Xóa ngay tập tin APK trong bộ nhớ đệm, hiển thị cảnh báo *"File không khớp hoặc không kiểm tra được, vui lòng tải từ GitHub"* và làm nổi bật nút **"Tải từ GitHub"**.
  5. Luôn hiển thị nút phụ **"Tải từ GitHub"** (`https://github.com/muitenvn/CloudPlay-App/releases`) ở mọi trạng thái.

---

## 3. Quyết Định Kỹ Thuật Trọng Tâm

- **Kết nối mạng trực tiếp (`Proxy.NO_PROXY`)**:
  - Toàn bộ truy vấn kiểm tra phiên bản (`api.github.com`), tải mã SHA-256 phụ (`.sha256`) và tải tập tin `.apk` đều sử dụng `url.openConnection(java.net.Proxy.NO_PROXY)` để đi thẳng qua mạng gốc của người dùng, hoàn toàn tách biệt với Proxy/VPN trong ứng dụng.
- **Xác thực nguồn mã băm SHA-256 Kép (Chính thức từ GitHub)**:
  - **Ưu tiên 1**: Đọc trường `digest` (định dạng `"sha256:..."`) của asset `.apk` trong phản hồi JSON từ `https://api.github.com/repos/muitenvn/CloudPlay-App/releases/latest`.
  - **Ưu tiên 2 (Dự phòng)**: Nếu phản hồi API chưa có trường `digest`, tải tệp `<tên apk>.sha256` trực tiếp từ `github.com` (tuyệt đối không tải tệp `.sha256` qua `ghfast.top`).
- **Tự động tạo và đính kèm file `.sha256` trên GitHub Actions**:
  - Ngay sau bước đóng gói APK trong workflow phát hành, tạo file `<tên apk>.sha256` chứa mã SHA-256 chuẩn và đính kèm vào GitHub Release cùng file `.apk` mà không thay đổi khóa ký hay các bước khác.

---

## 4. Kiến Trúc Kỹ Thuật & Luồng Dữ Liệu

```
┌───────────────────────────────────────────────────────────────────────────┐
│                     LUỒNG KIỂM TRA & TẢI CẬP NHẬT AN TOÀN                 │
└───────────────────────────────────────────────────────────────────────────┘
                                      │
        1. Mở ứng dụng (Kết nối trực tiếp Proxy.NO_PROXY)
                                      ▼
┌───────────────────────────────────────────────────────────────────────────┐
│  GitHub Release API (api.github.com/repos/muitenvn/CloudPlay-App/...)     │
│  • Lấy thông tin bản mới + URL .apk + Mã SHA-256 gốc (asset.digest)       │
│  • Dự phòng: Lấy mã SHA-256 từ asset phụ <tên apk>.sha256 trên github.com │
└───────────────────────────────────────────────────────────────────────────┘
                                      │
        2. Người dùng nhấn "Tải nhanh APK" trên UpdateAvailableDialog
                                      ▼
┌───────────────────────────────────────────────────────────────────────────┐
│  Bộ Tải Nội Bộ An Toàn (Chỉ HTTPS • ghfast.top / github.com • ≤ 150 MB)   │
│  • Tải vào cache riêng của app + Thanh tiến độ % + Nút Hủy                │
│  • Quá 15s không có dữ liệu hoặc lỗi ──► Tự động chuyển sang github.com   │
└───────────────────────────────────────────────────────────────────────────┘
                                      │
        3. Kiểm tra toàn vẹn SHA-256 từng ký tự (MessageDigest SHA-256)
                   ┌──────────────────┴──────────────────┐
                   ▼ (Khớp 100%)                         ▼ (Sai / Thiếu mã)
┌──────────────────────────────────────┐  ┌─────────────────────────────────┐
│ Mở trình cài đặt Android qua         │  │ Xóa ngay file APK trong cache   │
│ FileProvider + REQUEST_INSTALL_PKG   │  │ Báo lỗi & Hiện nút Tải GitHub   │
└──────────────────────────────────────┘  └─────────────────────────────────┘
```
