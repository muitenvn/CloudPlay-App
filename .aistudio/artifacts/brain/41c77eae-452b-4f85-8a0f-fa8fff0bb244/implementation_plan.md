# Tính Năng Tự Động Kiểm Tra Bản Cập Nhật (CloudPlay)

Bổ sung cơ chế tự động kiểm tra phiên bản mới từ GitHub Releases (`muitenvn/CloudPlay-App`) mỗi 24 giờ khi mở ứng dụng, hiển thị hộp thoại thông báo phiên bản mới và cung cấp nút kiểm tra thủ công trong mục Cài đặt.

## User Review & Critical Decisions

> [!IMPORTANT]
> Dưới đây là các quyết định thiết kế đã được xác nhận theo lựa chọn của bạn:

- **Quyết định đã xác nhận 1 (Cách hiển thị thông báo)**: Hiển thị **Hộp thoại thông báo cập nhật** ngay khi mở ứng dụng nếu phát hiện phiên bản trên GitHub mới hơn phiên bản đang cài đặt, kèm nút mở trang tải bản cập nhật hoặc bỏ qua.
- **Quyết định đã xác nhận 2 (Tần suất & điều khiển)**: Tự động kiểm tra ngầm tối đa **1 lần mỗi 24 giờ** khi mở ứng dụng (có công tắc bật/tắt trong Cài đặt, mặc định bật) kèm nút **"Kiểm tra ngay"** trong mục Cài đặt.

---

## 1. Overview & Core Concept

- **Chức năng chính**:
  - Khi mở ứng dụng (sau khi người dùng đã đồng ý điều khoản sử dụng), ứng dụng kiểm tra ngầm qua API GitHub Releases (`https://api.github.com/repos/muitenvn/CloudPlay-App/releases/latest`, timeout 8 giây) nếu đã qua 24 giờ kể từ lần kiểm tra trước.
  - So sánh phiên bản theo từng cấp số (`X.Y.Z`, ví dụ `1.2.9 < 1.2.10`, tự bỏ tiền tố `v`) với phiên bản hiện tại của ứng dụng.
  - Khi có bản mới, hiển thị hộp thoại thông báo gọn gàng cho biết số phiên bản mới, tóm tắt ngắn gọn và nút mở trang phát hành trên trình duyệt để tải về.
  - Khi mạng lỗi, hết giới hạn API hoặc dữ liệu không hợp lệ tijdens kiểm tra tự động: xử lý im lặng, không làm gián đoạn trải nghiệm người dùng.
- **Đối tượng sử dụng**: Người dùng CloudPlay muốn biết ngay khi có bản APK mới được phát hành trên GitHub mà không cần tự vào web tìm kiếm.
- **Giá trị mang lại**: Luôn cập nhật các bản vá tối ưu mạng, tay cầm và tính năng mới nhất một cách chủ động, nhẹ nhàng, không tốn tài nguyên nền và không yêu cầu thêm quyền truy cập hệ thống.

---

## 2. User Experience & Visual Design

- **Luồng trải nghiệm chính**:
  1. **Khi mở ứng dụng (Tự động mỗi 24 giờ)**:
     - Nếu công tắc *"Tự kiểm tra bản mới khi mở ứng dụng"* đang bật và lần kiểm tra gần nhất cách đây hơn 24 giờ, ứng dụng gọi API GitHub ở luồng nền.
     - Nếu có bản mới hơn (và người dùng chưa bấm "Để sau" cho phiên bản đó trong phiên hiện tại), một **Hộp thoại thông báo cập nhật** sẽ xuất hiện.
     - Người dùng có thể bấm **"Xem & Tải bản mới"** (mở trang phát hành GitHub bằng trình duyệt) hoặc **"Để sau"** (đóng hộp thoại).
  2. **Kiểm tra thủ công trong Cài đặt**:
     - Trong mục **Cài đặt**, thẻ **Cập nhật ứng dụng** hiển thị phiên bản hiện tại, công tắc bật/tắt tự động kiểm tra khi mở ứng dụng và nút **"Kiểm tra ngay"**.
     - Khi bấm **"Kiểm tra ngay"**, ứng dụng kiểm tra ngay lập tức (bỏ qua giới hạn 24 giờ), hiển thị trạng thái ngắn gọn ngay bên dưới (*"Đang kiểm tra..."*, *"Đã là bản mới nhất (vX.Y.Z)"*, hoặc *"Có bản mới vX.Y.Z"* kèm mở hộp thoại cập nhật).
- **Phong cách thiết kế & Giao diện**:
  - **Đồng bộ giao diện Xbox Dark Neon**: Nền tối (`#11161D`), viền xanh neon (`#22C55E`), nút hành động chính màu xanh Xbox nổi bật, chữ phân cấp rõ ràng, giải thích ngắn gọn dễ hiểu cho người dùng phổ thông (hỗ trợ cả Tiếng Việt và Tiếng Anh).

---

## 3. Key Product Decisions & Trade-Offs

- **Quyết định 1: Mở trang tải bằng trình duyệt thay vì tự tải ngầm APK**
  - *Cách tiếp cận*: Khi người dùng chọn cập nhật trên hộp thoại, mở trang phát hành chính thức trên trình duyệt.
  - *Lý do*: Không cần xin quyền nhạy cảm `REQUEST_INSTALL_PACKAGES` (tránh cảnh báo bảo mật của Android và tuân thủ chính sách an toàn), đồng thời giúp người dùng xem rõ thông tin bản phát hành trên GitHub.
- **Quyết định 2: So sánh phiên bản theo cấp số (Semantic Version Tuple) và giới hạn 24 giờ**
  - *Cách tiếp cận*: Tách chuỗi phiên bản (`1.2.10` -> `[1, 2, 10]`) để so sánh chính xác theo số học và lưu mốc thời gian kiểm tra gần nhất vào bộ nhớ cài đặt.
  - *Lý do*: Tránh lỗi so sánh chuỗi (`"1.2.10" < "1.2.9"`) và tránh bị giới hạn tần suất (rate-limit) của GitHub API khi người dùng mở ứng dụng nhiều lần trong ngày.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Sơ đồ kiến trúc & luồng dữ liệu**:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              MainActivity                               │
│  • Khởi chạy kiểm tra tự động (nếu bật & đã qua 24h từ lần kiểm tra cuối)│
│  • Hiển thị hộp thoại UpdateAvailableDialog khi phát hiện bản mới       │
└───────────────────┬─────────────────────────────────────┬───────────────┘
                    │                                     │
                    ▼                                     ▼
┌───────────────────────────────────────┐ ┌───────────────────────────────┐
│            UpdateChecker              │ │        SettingsScreen         │
│  • GET api.github.com/.../latest      │ │  • Công tắc: Tự kiểm tra      │
│  • Timeout 8s, User-Agent chuẩn       │ │    bản mới khi mở ứng dụng    │
│  • So sánh phiên bản số học (X.Y.Z)   │ │  • Nút: Kiểm tra ngay +       │
│  • StateFlow<UpdateCheckState>        │ │    dòng trạng thái kết quả    │
└───────────────────┬───────────────────┘ └───────────────┬───────────────┘
                    │                                     │
                    └──────────────────┬──────────────────┘
                                       ▼
                    ┌─────────────────────────────────────┐
                    │   SettingsRepository (SharedPrefs)  │
                    │  • autoCheckUpdatesOnLaunch = true  │
                    │  • lastUpdateCheckEpochMs           │
                    │  • cachedLatestVersion / ReleaseUrl │
                    └─────────────────────────────────────┘
```

- **Mô hình trạng thái & Tương tác**:
  - **Cấu hình lưu trữ**:
    - `autoCheckUpdatesOnLaunch`: Bật/tắt tự kiểm tra khi mở ứng dụng (mặc định `true`).
    - `lastUpdateCheckEpochMs`: Thời điểm kiểm tra thành công gần nhất để tính chu kỳ 24 giờ.
  - **Trạng thái kiểm tra (`UpdateCheckState`)**:
    - `Idle`: Chưa kiểm tra trong phiên.
    - `Checking`: Đang kết nối máy chủ GitHub.
    - `UpToDate(currentVersion)`: Đang ở phiên bản mới nhất.
    - `UpdateAvailable(currentVersion, latestVersion, releaseUrl, releaseNotes)`: Phát hiện bản mới hơn, kích hoạt hiển thị hộp thoại thông báo cập nhật và nút mở trình duyệt.
    - `SilentError`: Lỗi mạng hoặc giới hạn API (ẩn hoàn toàn khi kiểm tra tự động; chỉ báo ngắn gọn *"Không thể kết nối máy chủ kiểm tra, vui lòng thử lại sau"* nếu người dùng chủ động bấm nút *"Kiểm tra ngay"*).
