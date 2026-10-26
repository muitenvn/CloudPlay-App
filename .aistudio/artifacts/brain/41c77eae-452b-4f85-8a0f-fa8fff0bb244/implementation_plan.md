# Cập Nhật Giao Diện Trang Web Theo Bản Mẫu (`CloudPlay-web-mau.html`)

Bản kế hoạch này thay thế giao diện của cả hai file `docs/index.html` và `app/src/main/assets/web/index.html` (hai file giống hệt nhau) theo đúng chuẩn bản mẫu `CloudPlay-web-mau.html` đính kèm.

## Quyết Định & Phản Biện Kỹ Thuật

> [!IMPORTANT]
> **Tất cả 5 mục yêu cầu đều hợp lý và sẽ được thực hiện nguyên vẹn theo đúng bản mẫu, kèm 1 tinh chỉnh nhỏ ở mục 2 (`onerror` của ảnh chụp):**
> - Trong bản mẫu `CloudPlay-web-mau.html`, mỗi ảnh chụp màn hình nằm trong thẻ `<figure class="shot"><img ...><figcaption>...</figcaption></figure>`.
> - Khi thay `src` từ base64 sang `img/screenshot-1.png`, `img/screenshot-2.png`, `img/screenshot-3.png`: nếu 1 ảnh bị lỗi, ngoài việc ẩn thẻ `<img>`, hàm `onShotError(img)` sẽ ẩn luôn phần tử bao `<figure class="shot">` của ảnh đó (và chấm tròn tương ứng ở `#dots`) để không bị thừa khung trống chỉ có dòng chữ chú thích. Nếu cả 3 ảnh đều lỗi, toàn bộ khối `<section id="shots-sec">` ("Ảnh chụp") sẽ tự động ẩn (`hidden = true` / `display: none`).

---

## 1. Chi Tiết Thực Hiện 5 Mục Yêu Cầu

1. **Chuẩn bố cục, màu sắc, khoảng cách & thành phần theo bản mẫu:**
   - Giữ nguyên 100% hệ màu sáng (`--bg:#ffffff`, `--surface:#f5f7f6`, `--accent:#107c10`, v.v.), font hệ thống, khoảng cách, thanh đầu trang cố định (`.top`) hiện logo khi cuộn, nút chuyển ngôn ngữ `EN | VI`, hàng 4 ô thông tin (`.facts`: Phiên bản, Dung lượng, Android `7.0+`, Giá `Miễn phí`), ô mã kiểm tra `SHA-256` kèm nút `Sao chép`, mục Tính năng, Cách cài, Câu hỏi thường gặp (`<details>`) và thanh tải dính ở đáy (`.bar`) khi cuộn qua vùng nút tải đầu trang.
   - Không thêm màu, hiệu ứng hay bất kỳ thư viện ngoài nào.
2. **Đường dẫn ảnh thật & xử lý `onerror`:**
   - Thay chuỗi base64 của icon ở `.brand img` và `.hero img` bằng `src="img/icon.png"` kèm `onerror="this.style.display='none'"`.
   - Thay chuỗi base64 của 3 ảnh chụp màn hình bằng `src="img/screenshot-1.png"`, `src="img/screenshot-2.png"`, `src="img/screenshot-3.png"` kèm `onerror="onShotError(this)"`:
     - Nếu 1 ảnh lỗi: ẩn `<figure class="shot">` chứa ảnh đó.
     - Nếu cả 3 ảnh lỗi: ẩn toàn bộ `<section id="shots-sec">`.
   - Giữ nguyên các file ảnh hiện có trong `docs/img/` và `app/src/main/assets/web/img/`.
3. **Link tải thật & đồng bộ GitHub Releases API:**
   - Nút **Tải APK** (`#dl`, `#dl2`) và **Tải nhanh** (`#dlfast`) trỏ tới Release mới nhất của `muitenvn/CloudPlay-App` và tự động cập nhật phiên bản (`#f-ver`), dung lượng thực tế (`#f-size`), link `.apk` trực tiếp và mã `SHA-256` (`#hashv`) qua `https://api.github.com/repos/muitenvn/CloudPlay-App/releases/latest`.
   - Không thêm số sao, lượt tải hay đánh giá giả.
4. **Cảnh báo rủi ro & tuyên bố độc lập:**
   - Giữ nguyên hộp `.notice` (cả tiếng Việt và tiếng Anh): *"Ứng dụng độc lập, không liên kết với Microsoft, Xbox hay Epic Games. Có thể vi phạm điều khoản dịch vụ và dẫn đến khóa tài khoản; người dùng tự chịu rủi ro."*
5. **Đảm bảo hiển thị chuẩn trên màn hình hẹp `360px`:**
   - Không tràn ngang (`scrollWidth == clientWidth`), nút bấm chính cao tối thiểu `50px` (`>= 48px`), tiêu đề câu hỏi thường gặp cao tối thiểu `52px`, mã `SHA-256` tự cắt dấu `...` (`text-overflow: ellipsis`) không làm vỡ khung.

---

## 2. Sơ Đồ Cấu Trúc Trang Web (`docs/index.html` & `assets/web/index.html`)

```
┌──────────────────────────────────────────────────────────────────────┐
│ Header (.top sticky) : [img/icon.png CloudPlay]            [EN | VI] │
├──────────────────────────────────────────────────────────────────────┤
│ Hero (.hero)         : [img/icon.png 96×96]  CloudPlay               │
│                        Tagline mô tả ngắn gọn                        │
│ CTA (#cta)           : [⬇ Tải APK (min-h 50px)] [⚡ Tải nhanh]       │
│ SHA-256 (#hash)      : [SHA-256] [mã kiểm tra tự động...] [Sao chép] │
│ Facts (.facts 4 cột) : [Phiên bản] | [Dung lượng] | [7.0+] | [Free]  │
├──────────────────────────────────────────────────────────────────────┤
│ Ảnh chụp (#shots-sec): [img/screenshot-1.png] [-2.png] [-3.png]      │
│                        (Tự ẩn từng ảnh lỗi; ẩn cả khối nếu lỗi cả 3) │
├──────────────────────────────────────────────────────────────────────┤
│ Tính năng (.feats)   : 1. Theo dõi ping                              │
│                        2. Tự điều chỉnh chất lượng                   │
│                        3. Máy chủ và tay cầm ảo                      │
├──────────────────────────────────────────────────────────────────────┤
│ Cách cài (.steps)    : Bước 1 ➔ Bước 2 ➔ Bước 3                     │
│ FAQ (<details>)      : 3 câu hỏi thường gặp (min-height 52px)        │
│ Lưu ý (.notice)      : Không liên kết Microsoft, Xbox hay Epic Games │
│ Footer               : CloudPlay cho Android · Các bản phát hành     │
├──────────────────────────────────────────────────────────────────────┤
│ Sticky Bar (#bar)    : [⬇ Tải APK (hiện khi cuộn qua cụm nút đầu)]  │
└──────────────────────────────────────────────────────────────────────┘
```
