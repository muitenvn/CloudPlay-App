# Hệ Thống Đo Ping Qua Trạm Test (Probe) Gần Nhất & Nhật Ký Hệ Thống (App Log)

Triển khai hệ thống đo độ trễ thực tế cho danh sách máy chủ thông qua **Trạm Test (Probe) khu vực gần người dùng nhất** (ví dụ: TP. Hồ Chí Minh, Hà Nội hoặc trạm quốc gia lân cận), khắc phục triệt để lỗi gán nhầm `83ms (RTT WebRTC)` khi bấm chọn máy chủ, và bổ sung **Nút xem Log trong mục Cài đặt** để theo dõi chi tiết hoạt động bên dưới mà không làm rối giao diện bằng các thông báo đổi trạm.

## User Review & Critical Decisions

> [!IMPORTANT]
> Kế hoạch đã được cập nhật theo đúng yêu cầu chỉnh sửa của bạn:

- **Cách tính số ping hiển thị**: **Chỉ lấy nguyên số ping đo được từ Trạm Test tới Máy chủ VPN** (không cộng thêm độ trễ nội bộ từ điện thoại tới trạm test).
- **Thứ tự xử lý dự phòng ngầm (Silent Fallback)**: **Thử trạm test lân cận trước rồi mới chuyển về đo trực tiếp trên điện thoại**. Toàn bộ quá trình chuyển đổi trạm đo khi có lỗi diễn ra hoàn toàn tự động ở tầng nền, **không hiện thông báo hay nhãn báo đổi chỗ test trên màn hình Máy chủ**.
- **Theo dõi hoạt động qua Nút Log ở mục Cài đặt**: Thay vì hiện thông báo trên giao diện chính, thêm **nút "Nhật ký hệ thống (App Log)" trong mục Cài đặt** để bạn có thể mở xem chi tiết lịch sử hoạt động của app bất cứ lúc nào (đang chọn trạm đo nào, khi nào đổi sang trạm dự phòng, kết quả đo từng máy chủ, trạng thái mở khóa vùng, kết nối WebRTC...).
- **Phạm vi áp dụng nghiêm ngặt**: Hệ thống Trạm Test **chỉ phục vụ mục Máy chủ** để đo và chọn server; **tuyệt đối không sử dụng** để đo ping khi đang chơi game (ping trong trận vẫn lấy 100% từ luồng WebRTC thực tế).

---

## 1. Overview & Core Concept

- **Nguyên nhân lỗi "Nhấn vào máy chủ nào là máy chủ đó lên 83ms" hiện tại**:
  1. Trong mục Máy chủ, thẻ đang được chọn (`isSelected`) đang bị gán trực tiếp chỉ số `streamTelemetry.rttMs` (`83ms`) ngay khi bạn bấm chọn bất kỳ máy chủ nào khác trong danh sách.
  2. Các máy chủ chưa bấm vào đang lấy trung bình lịch sử vùng (`Ping lần chơi gần nhất` ~113ms cho Nhật, ~185ms cho Mỹ), và nút **"Đo ping"** đang bỏ qua phép đo thực tế khi ở chế độ `SMART_BYPASS`.
- **Giải pháp mới**:
  - Sửa logic hiển thị thẻ máy chủ: chỉ hiển thị `RTT WebRTC` cho đúng máy chủ đang truyền phát luồng game thực tế; bấm chọn sang các máy chủ khác sẽ giữ nguyên số ping riêng của từng máy chủ đó.
  - Khi bấm **"Đo ping" / "Chọn tối ưu"** ở mục Máy chủ, ứng dụng âm thầm chọn **Trạm Test gần vị trí người dùng nhất** (như **TP. Hồ Chí Minh**, **Hà Nội**, hoặc trạm quốc gia gần nhất) để đo ping thực tế tới từng máy chủ và cập nhật trực tiếp số `ms` lên danh sách.
  - Mọi chi tiết kỹ thuật (chọn trạm nào, đổi trạm dự phòng khi lỗi, kết quả đo, sự kiện mạng) được ghi vào **Bộ ghi Nhật ký (App Logger)** và xem qua **nút Log trong mục Cài đặt**.

---

## 2. User Experience & Visual Design

- **Luồng trải nghiệm người dùng (Key User Flows)**:
  1. **Tại mục Máy chủ**:
     - Giao diện giữ nguyên sự gọn gàng, không hiện popup hay thông báo mỗi khi hệ thống tự động chọn hoặc chuyển đổi trạm test.
     - Khi nhấn **"Đo ping"** hoặc **"Chọn tối ưu"**, từng máy chủ hiển thị số ping thực tế đo được từ trạm gần nhất.
  2. **Tại mục Cài đặt (Nút Log mới)**:
     - Có thêm nút/thẻ **"Nhật ký hoạt động (App Log)"** trong màn hình Cài đặt.
     - Khi nhấn vào, một hộp thoại/màn hình xem Log trực quan hiện ra với các dòng nhật ký theo thời gian thực (kèm mốc giờ `HH:mm:ss`, phân loại màu theo nhóm `PING`, `PROBE`, `VPN`, `STREAM`), có nút **Sao chép (Copy)** và **Xóa log (Clear)** để tiện kiểm tra và theo dõi app.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Đo nguyên gốc từ Trạm Test tới Máy chủ đích (Probe-to-Server Raw RTT)**
  - *Chosen Approach*: Lấy trực tiếp kết quả RTT từ Trạm Test gần nhất (ví dụ đặt tại TP.HCM hoặc Hà Nội) tới IP máy chủ VPN.
  - *Why*: Tránh sai số do đường hầm VPN nội bộ của điện thoại hoặc nghẽn hàng đợi mạng di động khi đo hàng loạt.
- **Decision 2: Chuyển trạm dự phòng ngầm + Tập trung thông tin kỹ thuật vào Nút Log ở Cài đặt**
  - *Chosen Approach*: Khi trạm số 1 bận, hệ thống tự nhảy sang trạm lân cận rồi mới về đo trực tiếp từ máy mà không làm phiền người dùng bằng thông báo; toàn bộ quá trình được ghi rõ trong mục Log ở Cài đặt.
  - *Why*: Giữ trải nghiệm ở mục Máy chủ mượt mà, sạch sẽ, đồng thời vẫn cung cấp đầy đủ dữ liệu chi tiết trong mục Cài đặt khi bạn cần kiểm tra.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌───────────────────────────────────┐   ┌───────────────────────────────────┐
│     MỤC MÁY CHỦ (Servers Tab)     │   │    MỤC CÀI ĐẶT (Settings Tab)     │
│ • Hiển thị Ping thật từng Server  │   │ • Nút "Nhật ký hệ thống (Log)"    │
│ • Không hiện thông báo đổi trạm   │   │ • Xem / Sao chép / Xóa dòng Log   │
└─────────────────┬─────────────────┘   └─────────────────▲─────────────────┘
                  │ Yêu cầu "Đo ping"                     │ Đọc luồng sự kiện
                  ▼                                       │
┌─────────────────────────────────────────────────────────┴─────────────────┐
│              BỘ ĐIỀU PHỐI ĐO PING & NHẬT KÝ (Ping & AppLogger)            │
│  • Xác định vị trí (Tọa độ / Thành phố / Quốc gia / Múi giờ)              │
│  • Chọn Trạm Test gần nhất bằng công thức khoảng cách Haversine           │
│  • Ghi nhật ký mọi bước (Chọn trạm, Đổi trạm dự phòng, Kết quả ms, Lỗi)   │
└─────────┬─────────────────────────┬───────────────────────────┬───────────┘
          │ Ưu tiên 1 (Ngầm)        │ Dự phòng 1 (Ngầm)         │ Dự phòng 2 (Ngầm)
          ▼                         ▼                           ▼
┌───────────────────┐     ┌───────────────────┐       ┌───────────────────┐
│  TRẠM TEST SỐ 1   │     │ TRẠM TEST LÂN CẬN │       │ ĐO TRỰC TIẾP MÁY  │
│ (VD: TP.Hồ Chí    │────▶│ (VD: Hà Nội hoặc  │──────▶│ (Giới hạn 3 luồng │
│  Minh / Nội địa)  │ Lỗi │  Trạm sát biên)   │  Lỗi  │  độc lập, chống   │
│                   │     │                   │       │  nghẽn 100ms)     │
└───────────────────┘     └───────────────────┘       └───────────────────┘
```

### Data Model & State
- **Bộ ghi Nhật ký Ứng dụng (`AppLogger`)**:
  - Lưu trữ danh sách các bản ghi sự kiện trong bộ nhớ (tối đa 300 dòng gần nhất qua `StateFlow`), mỗi dòng gồm: Thời gian (`HH:mm:ss.SSS`), Nhãn phân loại (`PROBE`, `PING`, `VPN`, `UNLOCK`, `WEBRTC`) và Nội dung chi tiết (ví dụ: `[PROBE] Vị trí VN (HCM) -> Chọn trạm đo gần nhất: Ho Chi Minh City`, `[PROBE] Trạm HCM bận -> Chuyển ngầm sang trạm lân cận: Hanoi`, `[PING] Japan Tokyo Bay #2: 64ms`).
- **Danh mục Trạm Test Khu Vực (Regional Probe Nodes)**:
  - Bao gồm tọa độ vĩ độ/kinh độ, mã quốc gia, tên thành phố (`TP. Hồ Chí Minh`, `Hà Nội`, `Singapore`, `Bangkok`, `Hong Kong`, `Taipei`, `Tokyo`, `Seoul`, `Silicon Valley`, `Frankfurt`...).
  - Tự động tính khoảng cách địa lý Haversine để chọn trạm gần nhất cho cả Việt Nam lẫn bất kỳ quốc gia nhỏ nào khác trên thế giới.

### Interactive Component & State Mapping
1. **Sửa lỗi nhảy `83ms` khi bấm vào máy chủ**:
   - Chỉ hiển thị `RTT WebRTC` cho đúng máy chủ đang chạy phiên stream thực tế; khi bấm sang các máy chủ khác trong mục Máy chủ, mỗi thẻ hiển thị đúng số ping đo được của chính máy chủ đó.
2. **Đo ping thực tế trong mục Máy chủ cho mọi chế độ**:
   - Khi nhấn **"Đo ping"** hoặc **"Chọn tối ưu"**, hệ thống đo qua Trạm Test gần nhất và cập nhật trực tiếp vào `serverPings`, không hiện thông báo đổi trạm trên màn hình.
3. **Hộp thoại Xem Log trong mục Cài đặt**:
   - Trong `SettingsScreen`, thêm nút mở hộp thoại **Nhật ký hệ thống (App Log)** hiển thị danh sách log tự động cuộn theo thời gian thực, hỗ trợ lọc nhanh, sao chép vào bộ nhớ tạm (Clipboard) và xóa sạch log.
