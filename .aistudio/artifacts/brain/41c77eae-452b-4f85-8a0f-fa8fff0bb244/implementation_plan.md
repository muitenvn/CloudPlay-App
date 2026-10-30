# Tối Ưu Giao Diện Toàn Diện CloudPlay & Tay Cầm Ảo Tùy Biến Từng Nút

Bản kế hoạch này nâng cấp đồng bộ giao diện của cả **App Android CloudPlay** và **Trang Web tải về** theo phong cách Xbox xanh lá đậm hiện đại, đồng thời nâng cấp toàn diện **Tay cầm ảo (Virtual Gamepad)** cho phép người chơi tự do kéo thả từng nút đơn lẻ, tách rời hoặc gộp lại chùm nút `A/B/X/Y`, chỉnh kích thước riêng cho từng nút, cũng như xóa bớt hoặc thêm lại bất kỳ nút nào theo ý muốn.

## User Review & Critical Decisions

> [!IMPORTANT]
> Dưới đây là các lựa chọn thiết kế và yêu cầu nâng cấp vừa được bổ sung của bạn:

- **Confirmed Decision 1 (Phạm vi tối ưu)**: Tối ưu đồng thời cả **Giao diện App Android** và **Trang Web tải về** (`docs` & trang cổng tải tích hợp trong ứng dụng).
- **Confirmed Decision 2 (Nâng cấp Tay cầm ảo tự do từng nút — MỚI)**:
  1. **Di chuyển độc lập từng nút (Per-Button Free Drag)**: Mọi nút (`LT`, `LB`, `L3`, `R3`, `RB`, `RT`, `View`, `Xbox`, `Menu`, `Cần L`, `Cần R`, `D-Pad`, `A`, `B`, `X`, `Y`) đều có thể kéo thả riêng lẻ tới bất kỳ vị trí nào trên màn hình, không còn bị khóa cứng theo từng cụm.
  2. **Tách rời hoặc Gộp lại chùm nút `A/B/X/Y` (Split / Merge ABXY & D-Pad)**: Có nút chuyển đổi nhanh cho phép **Tách rời 4 nút `A`, `B`, `X`, `Y`** thành 4 nút độc lập hoàn toàn (để xếp hình vòng cung hoặc đặt ở bất kỳ đâu) hoặc **Hợp lại thành chùm kim cương ABXY** (hỗ trợ vuốt trượt combo) bất cứ lúc nào. Hỗ trợ tương tự cho cụm `D-Pad` (gộp chữ thập hoặc tách 4 phím mũi tên).
  3. **Tự do chỉnh kích thước riêng từng nút (Per-Button Size Scaling)**: Khi chạm chọn một nút trong chế độ chỉnh sửa, người chơi có thể kéo thanh trượt hoặc bấm `−` / `+` để phóng to / thu nhỏ riêng nút đó (từ `50%` đến `200%`), ví dụ làm nút bắn `RT` hoặc nút nhảy `A` to hơn các nút còn lại.
  4. **Xóa nút và Thêm nút tự do (Remove & Add Buttons)**: Có thể xóa (ẩn) bất kỳ nút nào không dùng để màn hình thoáng tối đa, và bấm **`+ Thêm nút`** để thêm lại bất kỳ nút nào từ danh sách kho nút tay cầm.
- **Confirmed Decision 3 (Đa thiết bị & Làm gọn thẻ)**:
  - Bố cục thích ứng cho PC, iPad/Tablet và điện thoại xoay ngang/màn hình gập.
  - Làm gọn các thẻ trên **Trang chủ**, **Thư viện Game**, **Máy chủ** và làm gọn **Thanh HUD nổi** khi đang chơi.
- **Confirmed Decision 4 (Phong cách thẩm mỹ)**: Giữ bản sắc **Xbox xanh lá đậm hiện đại** (`#0B1210` / `#107C10` / `#3FB950`), gọn và thoáng hơn.

---

## 1. Overview & Core Concept

- **What It Does**:
  - **Tay cầm ảo thế hệ mới (Per-Button Custom Layout Engine)**: Biến màn hình cảm ứng thành bảng điều khiển tùy biến 100% giống các game mobile chuyên nghiệp (PUBG / Call of Duty Mobile): chọn từng nút để kéo đi, chỉnh cỡ to nhỏ riêng cho từng nút, tách rời hoặc hợp nhất chùm `A/B/X/Y`, xóa nút thừa và thêm nút mới.
  - **Trang Web tải về đa thiết bị**: Thích ứng từ điện thoại (1 cột gọn) sang iPad/Tablet (lưới 2 cột, 3 ảnh chụp màn hình dàn ngang kèm Lightbox phóng to) và PC màn hình rộng (bố cục Hero 2 cột trực quan).
  - **App Android gọn & thoáng**: Làm gọn mật độ thẻ trên Trang chủ, Thư viện Game, Máy chủ; hỗ trợ bố cục đa cột khi xoay ngang hoặc dùng trên máy tính bảng; thu gọn và tự làm mờ nhẹ thanh HUD khi đang chơi.
- **Target Audience / Persona**: Người chơi Xbox Cloud Gaming trên điện thoại và máy tính bảng muốn bố trí nút bấm cảm ứng vừa khít ngón tay cho từng tựa game (FPS, đua xe, hành động, đối kháng).
- **Key Value**: Mang lại quyền kiểm soát bố cục nút bấm tuyệt đối cho người chơi cảm ứng, đồng thời tối ưu không gian hiển thị trên mọi kích thước màn hình.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Chỉnh sửa Tay cầm ảo theo từng nút (Per-Button Editor Flow)**:
   - Người chơi bấm nút **`⚙ Control`** → chọn **`✥ Chỉnh sửa nút (Edit Layout)`** để vào chế độ thiết kế trực quan.
   - **Chọn & Kéo thả từng nút**: Chạm vào bất kỳ nút nào (`LT`, `LB`, `L3`, `R3`, `RB`, `RT`, `View`, `Xbox`, `Menu`, `A`, `B`, `X`, `Y`, `Cần L`, `Cần R`, `D-Pad`) để chọn nút đó (viền nút sáng màu xanh lơ/xanh lá nổi bật) và kéo trực tiếp tới vị trí bất kỳ trên màn hình.
   - **Thanh điều khiển nút đang chọn (Floating Inspector Bar)**: Ngay phía trên màn hình hiện bảng điều khiển gọn cho nút đang chọn gồm:
     - **Kích thước nút (`50%` – `200%`)**: Thanh trượt và nút `−` / `+` để chỉnh độ to/nhỏ của riêng nút đang chọn.
     - **Nút `⇄ Tách rời ABXY` / `⇄ Gộp chùm ABXY`** (và tùy chọn tách/gộp `D-Pad`): Khi bấm **Tách rời**, chùm `ABXY` tách thành 4 nút `A`, `B`, `X`, `Y` riêng biệt có thể kéo đi 4 góc khác nhau và chỉnh kích thước riêng từng nút; khi bấm **Gộp chùm**, 4 nút hợp lại thành cụm kim cương chuẩn.
     - **Nút `🗑 Xóa nút`**: Xóa (ẩn) nút đang chọn khỏi màn hình chơi game.
     - **Nút `＋ Thêm nút`**: Mở khay chọn nhanh hiển thị tất cả các nút đang bị ẩn/xóa để chạm thêm lại ngay lập tức.
     - **Nút `Đặt lại` & `✓ Lưu / Xong`**: Khôi phục mặc định hoặc lưu toàn bộ cấu hình (tọa độ từng nút, kích thước từng nút, trạng thái tách/gộp ABXY, danh sách nút ẩn/hiện) vào bộ nhớ bền vững.
2. **Truy cập Trang Web trên PC / iPad / Điện thoại**:
   - **Trên Điện thoại (< 640px)**: Bố cục 1 cột gọn, thanh tải ghim đáy khi cuộn.
   - **Trên iPad / Tablet (640px – 1024px)**: Khung `880px`, 3 ảnh chụp màn hình hiển thị cạnh nhau trên 1 hàng, lưới tính năng 2 cột.
   - **Trên PC (≥ 1024px)**: Khung `1040px` chia Hero 2 cột (thông tin + nút tải bên trái, bộ sưu tập ảnh chụp màn hình bên phải), lưới tính năng 3 cột, bấm vào ảnh mở Lightbox phóng to sắc nét.
3. **Duyệt App Android & Thanh HUD trong game**:
   - **Trang chủ, Thư viện, Máy chủ**: Thu gọn khoảng đệm thẻ, gộp các nhãn trạng thái thành hàng chip ngang gọn gàng, tự động chia 2 cột trên màn hình rộng/xoay ngang.
   - **Thanh HUD trong game**: Thu gọn kích thước viên thuốc `Ping • FPS`, tự động làm mờ nhẹ khi không chạm sau 4 giây để không che khung hình game.

### Visual Identity & Theme

- **Aesthetic Direction**: Xbox Dark Emerald — sắc nét, hiện đại, viền mảnh tinh tế.
- **Color Palette & Mood**:
  - Nền chính: `#0B1210` (App & Web Dark) / `#FFFFFF` (Web Light).
  - Bề mặt thẻ: `#121B17` với viền mảnh `#223029`.
  - Điểm nhấn: `#107C10` / `#3FB950` (Xbox Green) và `#00E5FF` (Direct Cyan cho khung viền nút đang được chọn trong chế độ chỉnh sửa tay cầm).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Lưu cấu hình từng nút độc lập (Per-Button State Persistence) tương thích ngược**
  - *Chosen Approach*: Lưu tọa độ `(dx, dy)`, hệ số kích thước riêng `scale` (`0.5f..2.0f`), danh sách nút đã ẩn/xóa (`hiddenButtons`), và trạng thái tách/gộp `abxySplitMode` / `dpadSplitMode` vào `SettingsRepository` (`SharedPreferences`).
  - *Why*: Đảm bảo khi người dùng thoát game hoặc đóng ứng dụng mở lại, từng nút vẫn nằm đúng vị trí, đúng kích thước và đúng trạng thái tách/gộp như đã thiết lập.
- **Decision 2: Hỗ trợ song song cả Chùm ABXY (Slide-Combo) và 4 nút A, B, X, Y rời**
  - *Chosen Approach*: Khi ở chế độ Gộp chùm, giữ nguyên cơ chế vuốt ngón tay giữa các nút `A/B/X/Y` liền mạch; khi chuyển sang chế độ Tách rời, mỗi nút `A`, `B`, `X`, `Y` là một nút cảm ứng đa điểm độc lập có vị trí và kích thước riêng.
  - *Why*: Phù hợp cho cả người chơi thích cụm nút truyền thống lẫn người chơi thích bố cục nút rời kiểu mobile esports.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                         CLOUDPLAY UI & GAMEPAD SYSTEM                      │
├──────────────────────────────────────┬─────────────────────────────────────┤
│     PER-BUTTON VIRTUAL GAMEPAD       │    APP & WEB RESPONSIVE LAYOUTS     │
│                                      │                                     │
│  ┌────────────────────────────────┐  │  ┌───────────────────────────────┐  │
│  │ SettingsRepository (Persisted) │  │  │ Dashboard & Tabs (Compose)    │  │
│  │ • perButtonOffsets (x, y)      │  │  │ • Compact Hero & Server Cards │  │
│  │ • perButtonScales (0.5x..2.0x) │  │  │ • Adaptive 2-4 Col Grids      │  │
│  │ • hiddenButtonIds (Set<String>)│  │  │ • Auto-Dim Floating HUD Pill  │  │
│  │ • abxySplitMode & dpadSplitMode│  │  └───────────────────────────────┘  │
│  └───────────────┬────────────────┘  │                                     │
│                  ▼                   │  ┌───────────────────────────────┐  │
│  ┌────────────────────────────────┐  │  │ Web Portal (docs & assets)    │  │
│  │ VirtualGamepadOverlay          │  │  │ • Mobile / iPad / PC Grids    │  │
│  │ • Individual Draggable Nodes   │  │  │ • 2-Col Desktop Hero Layout   │  │
│  │   (LT,LB,L3,R3,RB,RT,View,     │  │  │ • Screenshot Lightbox Modal   │  │
│  │    Xbox,Menu,L-Stick,R-Stick)  │  │  └───────────────────────────────┘  │
│  │ • ABXY: Cluster ⇄ Split A,B,X,Y│  │                                     │
│  │ • DPad: Cross ⇄ Split ↑,↓,←,→  │  │                                     │
│  └───────────────┬────────────────┘  │                                     │
│                  ▼                   │                                     │
│  ┌────────────────────────────────┐  │                                     │
│  │ Edit Mode Inspector Toolbar    │  │                                     │
│  │ • Selected Button Size Slider  │  │                                     │
│  │ • Split / Merge ABXY & D-Pad   │  │                                     │
│  │ • Delete Selected Button (🗑)  │  │                                     │
│  │ • Add Button Drawer (＋)       │  │                                     │
│  └────────────────────────────────┘  │                                     │
└──────────────────────────────────────┴─────────────────────────────────────┘
```

### Interactive Component & State Mapping

- **Per-Button Gamepad Customization Engine**:
  - Định danh độc lập cho từng phần tử: `btn_lt`, `btn_lb`, `btn_l3`, `btn_r3`, `btn_rb`, `btn_rt`, `btn_view`, `btn_xbox`, `btn_menu`, `stick_left`, `stick_right`, `dpad_cluster` (hoặc `dpad_up`, `dpad_down`, `dpad_left`, `dpad_right` khi tách), và `abxy_cluster` (hoặc `btn_a`, `btn_b`, `btn_x`, `btn_y` khi tách).
  - Trong chế độ `isEditPositionMode`:
    - Chạm hoặc bắt đầu kéo bất kỳ nút nào sẽ đặt `selectedButtonId = id`.
    - Kéo thả cập nhật trực tiếp tọa độ `(dx, dy)` của riêng `id` đó và lưu ngay vào `SettingsRepository`.
    - Thanh công cụ chỉnh sửa hiển thị thanh trượt kích thước riêng `buttonScales[selectedButtonId]` (`0.5f` – `2.0f`), nút `Tách rời / Gộp lại` cho `ABXY` và `D-Pad`, nút `Xóa nút` (đưa `selectedButtonId` vào `hiddenButtons`), và nút `+ Thêm nút` mở hộp chọn khôi phục bất kỳ nút nào trong `hiddenButtons`.
- **Adaptive Android Screens & Web Portal**:
  - Đồng bộ 100% giữa `docs/index.html` và `app/src/main/assets/web/index.html` với CSS Grid đa thiết bị (Điện thoại / iPad / PC) và Lightbox phóng to ảnh chụp màn hình.
  - Tinh gọn chiều cao thẻ và khoảng đệm trên `HomeScreen`, `GameLibraryScreen`, `ServersScreen`, và bổ sung cơ chế tự làm mờ khi rảnh cho `FloatingHud`.
