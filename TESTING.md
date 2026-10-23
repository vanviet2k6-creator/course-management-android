# Kết quả kiểm tra — cập nhật 23/10/2026

## Kết quả

- Build `assembleDebug`: thành công. APK debug đã cài và chạy trên máy ảo.
- `testDebugUnitTest` (20/10): **4 test, 0 lỗi**, kiểm tra số âm/NaN/Infinity, ngày lịch, URL và tìm kiếm tiếng Việt không dấu.
- `lintDebug` (20/10): thành công, không có lỗi chặn build. Còn cảnh báo về phiên bản thư viện/SDK và chuỗi tiếng Việt chưa tách thành tài nguyên đa ngôn ngữ.
- Kiểm thử Android trên **MockAPI thật** (23/10): **4 test, 0 lỗi**, thời gian 51,526 giây. Kết quả ở `test-results/instrumentation-live.txt`.
- Máy ảo Small_Phone, system image Android API 37.2, dựng hình phần mềm.

## Backend đã hoàn tất

- Project: https://mockapi.io/projects/6ad6ffc2f71f154ac4879556
- Base URL: https://6ad6ffc2f71f154ac4879555.mockapi.io
- Đã tạo hai resource `categories`, `courses`, nạp **3 danh mục và 10 khóa học**, sau đó GET lại để xác minh.
- Mọi `courses.categoryId` đều trỏ tới ID danh mục có thật. `price`, `duration` là Number; các trường văn bản và URL là String.
- Test CRUD tạo bản ghi riêng và dọn đúng các bản ghi tạm đó. Sau test, backend giữ đúng 3 danh mục và 10 khóa học mẫu.
- JSON lấy từ API thật nằm trong `data/categories.json` và `data/courses.json`.

## Các tình huống đã kiểm tra với API thật

1. POST danh mục, PUT cập nhật và GET đối chiếu.
2. Chặn thêm khóa học khi categoryId không tồn tại.
3. POST khóa học, GET chi tiết, PUT đổi tên/học phí và đối chiếu dữ liệu đã lưu.
4. Chặn xóa danh mục còn khóa học; xác nhận danh mục vẫn tồn tại.
5. DELETE khóa học rồi xóa danh mục rỗng; giữ nguyên dữ liệu mẫu.
6. Mở danh sách từ MockAPI trong app, tìm `android`, lọc Web và xem chi tiết.
7. Vào form sửa, để trống tên và xác nhận lỗi bắt buộc.
8. Màn hình danh mục hiển thị đúng tên lấy từ API.
9. MP4 HTTPS phát trong VideoView; vị trí phát thực sự tiến qua 3,5 giây.

10 URL ảnh mẫu từng được xác minh trả HTTP 200, content type image/jpeg ở lần kiểm tra 20/10. Ngày 23/10, URL Sintel cũ trả 403 nên đã thay bằng Flower (CC0) của MDN trong dữ liệu mẫu và cả 10 bản ghi trên backend. URL mới trả HTTP 206 khi yêu cầu Range, content type video/mp4; đã xác minh phát thực tế trong VideoView và ghi hình thành công.

Lần chạy đầu ngày 23/10 gặp timeout khởi động video trên máy ảo dựng hình phần mềm. Thời gian chờ kiểm thử được tăng từ 45 lên 90 giây; điều kiện vẫn yêu cầu video đang phát và vị trí phát đạt ít nhất 3,5 giây. Chạy lại toàn bộ 4 kiểm thử Android thành công trong 51,526 giây.

Video thuyết minh đi kèm giải thích mã nguồn, cách chạy và các thao tác thật. Bộ ghi hình tạm thời đã được tách khỏi source bàn giao. Dữ liệu demo được dọn sau khi quay và sau kiểm thử; backend giữ 3 danh mục, 10 khóa học.

Trước khi có MockAPI, app từng được kiểm tra với REST fixture cục bộ. Lần xác minh cuối dùng Base URL MockAPI nêu trên, không dùng fixture hoặc dữ liệu hardcode làm danh sách dự phòng.
