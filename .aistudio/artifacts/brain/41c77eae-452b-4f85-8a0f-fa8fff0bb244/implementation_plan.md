# Kế hoạch Cập nhật Giao diện Trang Web Tải Ứng Dụng CloudPlay

Cập nhật trang web giới thiệu và tải ứng dụng (`docs/index.html` và `app/src/main/assets/web/index.html`) theo đúng phong cách tối giản (nền trắng `#ffffff`, chữ `#1a1a1a`, chữ phụ `#666666`, màu nhấn xanh lá `#107c10`, một cột rộng tối đa `640px`), bổ sung icon ứng dụng, hàng ảnh chụp màn hình vuốt ngang (`scroll-snap`) tự ẩn khi lỗi 404, và nới rộng khoảng cách giữa các khối.

## Giao diện & Bố cục (Visual Direction & Layout)

- **Biểu tượng ứng dụng (`icon.png`)**:
  - Xuất từ icon launcher của ứng dụng ra `docs/img/icon.png` và `app/src/main/assets/web/img/icon.png`.
  - Hiển thị kích thước `64px × 64px`, bo góc nhẹ (`border-radius: 14px`), đặt ngay phía trên tiêu đề `CloudPlay`.
- **Dãy ảnh chụp màn hình vuốt ngang (`scroll-snap`)**:
  - Bỏ hoàn toàn khung xám giữ chỗ cũ (`Ảnh chụp màn hình ứng dụng`).
  - Thay bằng khung cuộn ngang (`display: flex; overflow-x: auto; scroll-snap-type: x mandatory; gap: 12px`) chứa tối đa 3 ảnh dọc: `img/screenshot-1.png`, `img/screenshot-2.png`, `img/screenshot-3.png`.
  - Mỗi ảnh cao `360px`, chiều rộng tự động theo tỉ lệ màn hình dọc, bo góc nhẹ (`border-radius: 8px`), viền mảnh trung tính (`1px solid #e5e5e5`), có thuộc tính `alt` mô tả rõ bằng tiếng Việt và tiếng Anh.
  - Tạo sẵn 3 ảnh minh họa giao diện mẫu đồng bộ trong cả `docs/img/` và `app/src/main/assets/web/img/`.
- **Tự động ẩn khi lỗi tải ảnh (404 Fallback)**:
  - Mỗi thẻ `<img>` trong khối ảnh chụp màn hình gắn sự kiện `onerror`: nếu ảnh không tải được (404), ẩn ngay ảnh đó (`display: none`).
  - Kiểm tra sau mỗi lần lỗi: nếu cả 3 ảnh đều bị ẩn do lỗi tải, ẩn luôn toàn bộ khối chứa ảnh chụp màn hình (`#screenshots-section`) để không xuất hiện khoảng trống hay khung rỗng trên trang.
- **Khoảng cách thoáng giữa các khối**:
  - Tăng khoảng cách dọc (`margin-bottom` từ `24px` lên `36px–40px`, `padding` khung trang thoáng hơn) giữa phần đầu trang (icon + tên + mô tả), khối nút tải APK, khối ảnh chụp màn hình, danh sách 3 tính năng, hộp cảnh báo rủi ro và chân trang.
  - Giữ nguyên quy tắc không thêm màu mới, không đổ bóng, không gradient, không hiệu ứng động.

## Các tệp sẽ tạo mới & cập nhật

1. **`docs/img/icon.png` & `app/src/main/assets/web/img/icon.png`**:
   - Xuất từ `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`.
2. **`docs/img/screenshot-1.png`, `screenshot-2.png`, `screenshot-3.png` (và bản sao trong `app/src/main/assets/web/img/`)**:
   - Tạo 3 ảnh chụp màn hình dọc minh họa giao diện ứng dụng CloudPlay (Màn hình chính chọn chế độ chơi, Bảng theo dõi Ping/chất lượng kết nối, và Cài đặt chất lượng hình ảnh).
3. **`docs/index.html` & `app/src/main/assets/web/index.html`**:
   - Cập nhật HTML/CSS/JS nhẹ cho icon 64px, dãy ảnh `scroll-snap` cao 360px kèm xử lý ẩn khi lỗi 404, và tăng khoảng cách giữa các phần.
   - Cuối cùng kiểm tra biên dịch ứng dụng và báo cáo chi tiết các mục đã thực hiện cùng số dòng trước/sau của từng tệp.
