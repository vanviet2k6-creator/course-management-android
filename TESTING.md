# Kết quả kiểm tra — 20/10/2026

## Kết quả

- Build `assembleDebug`: thành công. APK debug đã cài và chạy trên máy ảo.
- `testDebugUnitTest`: **4 test, 0 lỗi**, kiểm tra số âm/NaN/Infinity, ngày lịch, URL và tìm kiếm tiếng Việt không dấu.
- `lintDebug`: thành công, không có lỗi chặn build. Còn cảnh báo về phiên bản thư viện/SDK và chuỗi tiếng Việt chưa tách thành tài nguyên đa ngôn ngữ.
- Kiểm thử Android trên **MockAPI thật**: **4 test, 0 lỗi**, thời gian 31,222 giây. Kết quả ở `test-results/instrumentation-live.txt`.
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

10 URL ảnh mẫu trả HTTP 200, content type image/jpeg. Trailer Sintel trả HTTP 200, video/mp4 và đã phát được trong app. Ảnh màn hình bàn giao được cập nhật từ lần kiểm thử API thật. Ảnh `06-mockapi-project.png` ghi lại project có đủ 3 và 10 bản ghi.

Trước khi có MockAPI, app từng được kiểm tra với REST fixture cục bộ. Lần xác minh cuối dùng Base URL MockAPI nêu trên, không dùng fixture hoặc dữ liệu hardcode làm danh sách dự phòng.
