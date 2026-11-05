# Kế hoạch chi tiết chia nhỏ 7 File Giao diện lớn

Kế hoạch này trình bày cụ thể cách chia 7 file giao diện lớn của CloudPlay (tổng cộng **14.602 dòng**) thành các file nhỏ theo từng nhóm chức năng. Quá trình thực hiện làm **lần lượt từng file từ 1 đến 7**, xong mỗi file sẽ **kiểm tra biên dịch (`compile_applet`) ngay** rồi mới sang file tiếp theo; hoàn toàn **chỉ di chuyển code, không đổi hành vi hay giao diện**.

### User Review & Critical Decisions

> [!IMPORTANT]
> - **Nguyên tắc an toàn tuyệt đối**:
>   1. Làm lần lượt theo đúng thứ tự 1 ➔ 7. Xong mỗi mục đều chạy biên dịch kiểm tra ngay. Nếu mục nào vướng phụ thuộc trạng thái phức tạp hoặc có nguy cơ lỗi, sẽ giữ nguyên mục đó, báo rõ lý do rồi sang mục tiếp theo.
>   2. Các hàm/thành phần tách ra được đặt trong **cùng package** với file gốc (`com.example.ui.tabs` hoặc `com.example.ui.components`) và dùng phạm vi `internal` cho phần dùng chung, giúp mọi file khác trong dự án và bộ Unit Test gọi như cũ mà không bị gãy liên kết.
>   3. **KHÔNG đụng tới**: `XboxBridge.kt`, `VpnManager.kt`, `AppSettings.kt`, `CloudBrowserScreen.kt`, và các file web/tài liệu.

---

### 1. Overview & Core Concept (Chi tiết cách chia 7 file)

#### Mục 1: `SettingsScreen.kt` (Hiện tại: 2.727 dòng ➔ Chia thành 5 file)
- **`SettingsScreen.kt` (~350 dòng)**: Khung màn hình Cài đặt chính, thanh tìm kiếm cài đặt, trạng thái đóng/mở các nhóm và lời gọi tới từng nhóm cài đặt.
- **`SettingsNetworkVpnSections.kt` (~650 dòng)**: Nhóm cài đặt Mạng, VPN, Tự động tắt VPN khi vào game, Chọn vùng/Quốc gia và Chẩn đoán mạng.
- **`SettingsStreamDisplaySections.kt` (~650 dòng)**: Nhóm cài đặt Chất lượng hình ảnh (Bitrate, Độ phân giải, FPS, Codec), Giải mã phần cứng và Âm thanh.
- **`SettingsGamepadSystemSections.kt` (~750 dòng)**: Nhóm cài đặt Tay cầm ảo/vật lý, Cảm biến con quay (Gyro), Rung, Ngôn ngữ, Sao lưu và Thông tin ứng dụng.
- **`SettingsSharedComponents.kt` (~320 dòng)**: Các thành phần giao diện dùng chung trong màn hình cài đặt (`CollapsibleSettingsGroup`, `SettingToggleRow`, `LabeledSlider`, `SettingsCard`).

#### Mục 2: `ServersScreen.kt` (Hiện tại: 1.926 dòng ➔ Chia thành 3 file)
- **`ServersScreen.kt` (~950 dòng)**: Khung màn hình Máy chủ chính, thanh trạng thái máy chủ đang chọn, thẻ chọn chế độ VPN thông minh, bộ lọc quốc gia và danh sách máy chủ.
- **`ServerItemCard.kt` (~400 dòng)**: Thẻ hiển thị từng máy chủ (`ServerCard`), huy hiệu trạng thái mở khóa vùng, khoảng cách, lịch sử phiên và 4 vạch sóng tín hiệu.
- **`ServerDialogs.kt` (~580 dòng)**: Hai hộp thoại tách riêng gồm hộp thoại thêm Proxy cá nhân (`CustomProxyDialog`) và hộp thoại xem chi tiết kỹ thuật máy chủ (`ServerDetailDialog`).

#### Mục 3: `GameLibraryScreen.kt` (Hiện tại: 1.413 dòng ➔ Chia thành 3 file)
- **`GameLibraryScreen.kt` (~650 dòng)**: Khung màn hình Thư viện game chính, thanh tìm kiếm, bộ lọc danh mục (`GameTabSection`), đồng bộ danh mục và lưới hiển thị game.
- **`LibraryGameCard.kt` (~380 dòng)**: Thẻ hiển thị từng tựa game (`LibraryGameCard`), ảnh bìa, nhãn hỗ trợ cảm ứng/chuột bàn phím và nút vào chơi nhanh.
- **`GameLibraryDialogs.kt` (~380 dòng)**: Hai hộp thoại gồm hộp thoại chi tiết game (`XboxGameDetailDialog`) và hộp thoại tự thêm game mới (`AddGameDialog`).

#### Mục 4: `SessionHistoryScreen.kt` (Hiện tại: 1.813 dòng ➔ Chia thành 4 file)
- **`SessionHistoryScreen.kt` (~450 dòng)**: Khung màn hình Lịch sử phiên chơi chính, bộ lọc phiên (`HistoryFilterMode`: Tất cả / Mượt / Cảnh báo) và danh sách phiên.
- **`SessionHistoryCards.kt` (~450 dòng)**: Thẻ tổng kết tuần (`WeeklySummaryCard`), thẻ từng phiên chơi (`SessionHistoryItemCard`), ô chỉ số (`SummaryStatTile`), thẻ gợi ý (`SmartSuggestionBanner`), `MetricChip`, `WarningBadgePill`.
- **`SessionDetailDialog.kt` (~520 dòng)**: Hộp thoại chi tiết phiên chơi (`SessionDetailDialog`), biểu đồ độ trễ mạng và nút chơi lại / xóa phiên.
- **`SessionLagAnalyzer.kt` (~390 dòng)**: Toàn bộ bộ máy tính toán & chẩn đoán dùng chung (`LagDiagnosticConfig`, `SessionLagDiagnosis`, `computeSessionDropRatio`, `diagnoseSessionLag`, `SessionVerdictThresholds`, `evaluateSessionVerdict`, `getMetricOneSentenceExplanation`, và các hàm thang màu/nhãn `pingMetricColor`, `decodeStatusLabel`...).

#### Mục 5: `HomeScreen.kt` (Hiện tại: 1.406 dòng ➔ Chia thành 3 file)
- **`HomeScreen.kt` (~360 dòng)**: Khung màn hình Trang chủ chính, điều phối bố cục dọc/ngang và kết nối trạng thái.
- **`HomeHeroAndNetworkCards.kt` (~520 dòng)**: Bảng điều khiển vào chơi chính (`HeroConsolePlayDeck`) và thẻ trạng thái mạng/VPN nhanh (`QuickNetworkCockpitCard`).
- **`HomeFeaturedGamesSection.kt` (~530 dòng)**: Khu vực danh sách game nổi bật/chơi gần đây (`FeaturedGamesSection`) và thẻ game trang chủ (`FeaturedGameCard`).

#### Mục 6: `VirtualGamepad.kt` (Hiện tại: 2.789 dòng ➔ Chia thành 4 file)
- **`VirtualGamepad.kt` (~1.150 dòng)**: Khung phủ tay cầm ảo (`VirtualGamepadOverlay`), bộ xử lý đa điểm chạm và bố cục vị trí các nút trên màn hình.
- **`VirtualGamepadTunerModals.kt` (~600 dòng)**: Hộp thoại ẩn/hiện nút (`AddOrManageButtonsModal`), bảng chỉnh nhanh kích thước/độ mờ/cảm biến (`ControlQuickTunerPanel`) và `QuickFeatureChip`.
- **`VirtualGamepadControls.kt` (~850 dòng)**: Toàn bộ các thành phần vẽ nút bấm (`VirtualAnalogStick`, `VirtualDpadCross`, `StandaloneDpadButton`, `DpadVisualKey`, `GamepadTriggerButton`, `GamepadShoulderButton`, `GamepadThumbClickMiniButton`, `GamepadSmallButton`, `GamepadXboxNexusButton`, `StandaloneAbxyButton`, `VirtualAbxyPad`, `AbxyVisualButton`).
- **`VirtualGamepadStorage.kt` (~190 dòng)**: Mô hình `GamepadEditableNodeInfo` và các hàm đọc/lưu tọa độ, tỷ lệ kích thước, danh sách nút ẩn (`parseControlOffsets`, `serializeControlOffsets`, `parseButtonScales`, `parseHiddenButtons`...).

#### Mục 7: `FloatingHud.kt` (Hiện tại: 2.528 dòng ➔ Chia thành 3 file)
- **`FloatingHud.kt` (~1.250 dòng)**: Enum `HudMode`, `HudDockedEdge`, và khung bảng điều khiển nổi `FloatingHud` (xử lý kéo thả, neo cạnh màn hình, chế độ thu gọn và khung mở rộng).
- **`FloatingHudTelemetryAndQuality.kt` (~720 dòng)**: Thẻ chuyên biệt Fortnite (`FortniteExclusiveHudCard`), thanh tiêu đề thu gọn (`ExpandedHudCollapseHeader`, `CollapseTouchButton48dp`), khu vực thông số mạng/giải mã (`DenseCoreTelemetrySection`, `DenseMetricRowItem`) và khu vực chỉnh chất lượng luồng (`DenseQualityModeSection`, `DenseQualityPresetChip`).
- **`FloatingHudQuickControls.kt` (~560 dòng)**: Khu vực bật/tắt tính năng nhanh (`DenseFeatureTogglesSection`, `DenseQuickTogglePill`), chỉnh nhanh tay cầm (`DenseQuickGamepadSection`) và hàng nút tác vụ cuối bảng (`DenseBottomActionsSection`, `DenseBottomActionChip`).

---

### 2. User Experience & Visual Design

- **Trải nghiệm người dùng và giao diện giữ nguyên 100%**: Không thay đổi bất kỳ màu sắc, phông chữ, kích thước, khoảng cách hay hiệu ứng chuyển động nào.
- **Bảo toàn toàn bộ `testTag` và `contentDescription`**: Đảm bảo các bài kiểm tra tự động và trình đọc màn hình hoạt động như cũ.

---

### 3. Key Product Decisions & Trade-Offs

- **Quyết định 1**: Giữ nguyên tên file gốc làm điểm vào chính (ví dụ `SettingsScreen.kt` vẫn chứa `fun SettingsScreen(...)`), chỉ tách các thành phần con, hộp thoại và hàm phụ trợ sang các file đi kèm có tiền tố cùng tên (`Settings...`, `Server...`, `Session...`, `VirtualGamepad...`, `FloatingHud...`) để khi mở thư mục code, các file cùng nhóm sẽ nằm sát nhau theo thứ tự bảng chữ cái.
- **Quyết định 2**: Chuyển các hàm/lớp phụ trợ từ `private` sang `internal` trong cùng package để các file con gọi nhau trực tiếp mà không làm lộ API không cần thiết ra ngoài module.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                 CẤU TRÚC NHÓM FILE SAU KHI TÁCH (7 NHÓM)                     │
└──────────────────────────────────────┬───────────────────────────────────────┘
                                       │
        ┌──────────────────────────────┴───────────────────────────────┐
        ▼                                                              ▼
┌──────────────────────────────────────┐      ┌────────────────────────────────┐
│   GÓI GIAO DIỆN TAB (5 MÀN HÌNH)     │      │ GÓI THÀNH PHẦN NỔI (2 CỤM LỚN) │
├──────────────────────────────────────┤      ├────────────────────────────────┤
│ 1. Nhóm Settings (5 file)            │      │ 6. Nhóm VirtualGamepad (4 file)│
│  ├─ Khung chính & tìm kiếm           │      │  ├─ Khung phủ & đa điểm chạm   │
│  ├─ Mạng, VPN & Vùng                 │      │  ├─ Bảng chỉnh nhanh & ẩn/hiện │
│  ├─ Hình ảnh, Âm thanh & Luồng       │      │  ├─ Các nút Analog/Dpad/ABXY   │
│  ├─ Tay cầm, Cảm biến & Hệ thống     │      │  └─ Lưu/đọc tọa độ & tỷ lệ nút │
│  └─ Thành phần khung/nút dùng chung  │      │                                │
│                                      │      │ 7. Nhóm FloatingHud (3 file)   │
│ 2. Nhóm Servers (3 file)             │      │  ├─ Khung kéo thả & neo cạnh   │
│  ├─ Khung danh sách & thanh trạng thái│      │  ├─ Thông số mạng & chất lượng │
│  ├─ Thẻ máy chủ & vạch tín hiệu      │      │  └─ Bật/tắt nhanh & nút tác vụ │
│  └─ Hộp thoại thêm Proxy & chi tiết  │      └────────────────────────────────┘
│                                      │
│ 3. Nhóm GameLibrary (3 file)         │
│  ├─ Khung lọc, tìm kiếm & lưới game  │
│  ├─ Thẻ game & huy hiệu trạng thái   │
│  └─ Hộp thoại chi tiết & thêm game   │
│                                      │
│ 4. Nhóm SessionHistory (4 file)      │
│  ├─ Khung danh sách & bộ lọc phiên   │
│  ├─ Thẻ tuần, thẻ phiên & ô chỉ số   │
│  ├─ Hộp thoại chi tiết & biểu đồ trễ │
│  └─ Bộ chẩn đoán lag & thang màu/nhãn│
│                                      │
│ 5. Nhóm Home (3 file)                │
│  ├─ Khung trang chủ chính            │
│  ├─ Bảng vào chơi & thẻ mạng nhanh   │
│  └─ Khu vực game nổi bật & thẻ game  │
└──────────────────────────────────────┘
```
