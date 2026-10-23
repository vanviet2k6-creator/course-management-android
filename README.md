# CourseHub — Quản lý khóa học

Ứng dụng Android viết bằng Java cho bài tập **Mobile App – Quản lý khóa học | MockAPI**. Có mã nguồn, dữ liệu mẫu, APK debug và hướng dẫn chạy. Video thuyết minh được cung cấp riêng cùng bản bàn giao; chức năng phát trailer nằm trong ứng dụng.

**Trạng thái bàn giao:** MockAPI đã có hai resource, 3 danh mục và 10 khóa học. Ứng dụng đã qua kiểm thử CRUD và giao diện với API thật. Mở app là có dữ liệu; không cần tạo lại resource hoặc nạp mẫu lần nữa.

## Chạy bằng Android Studio

1. Mở **Android Studio → Open**, chọn thư mục `CourseManagement` chứa `settings.gradle`.
2. Đợi Gradle Sync. Dùng **Gradle JDK 21** (Embedded JDK của Android Studio hoặc JDK 21 đã cài).
3. Cài Android SDK Platform 35 và Android SDK Build-Tools nếu IDE yêu cầu. Chọn điện thoại/máy ảo **Android 8.0 / API 26 trở lên**.
4. Chọn cấu hình `app`, bấm **Run ▶**.
5. App dùng Base URL trong `gradle.properties`. Có thể đổi tại **Cài đặt → Base URL → Lưu và kết nối**.

Máy hiện tại đã có SDK tại `C:\Users\VIET\AppData\Local\Android\Sdk`. `local.properties` là cấu hình riêng của máy; Android Studio sẽ tạo lại khi giải nén source trên máy khác.

Nếu IDE báo `JAVA_HOME` trỏ tới thư mục JDK không tồn tại, chọn lại Gradle JDK hoặc dùng script `build-local.ps1`. Script chỉ đặt biến cho tiến trình build, không sửa cấu hình Windows.

```powershell
.\build-local.ps1
```

Lệnh Gradle tương đương:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

APK được tạo tại `app/build/outputs/apk/debug/app-debug.apk`. Có thể cài APK debug trực tiếp lên điện thoại Android để thử, hoặc chạy từ IDE.

## MockAPI

- Project quản trị: https://mockapi.io/projects/6ad6ffc2f71f154ac4879556
- Base URL do người dùng cung cấp: https://6ad6ffc2f71f154ac4879555.mockapi.io
- Categories: https://6ad6ffc2f71f154ac4879555.mockapi.io/categories
- Courses: https://6ad6ffc2f71f154ac4879555.mockapi.io/courses

Chuỗi `/:endpoint` trong trang MockAPI là chỗ giữ chỗ, **không đưa vào Base URL**. Nếu project có tiền tố `/api/v1`, thêm đúng tiền tố đó vào Base URL.

Project hiện tại đã có hai **resource độc lập**, tên chính xác `categories` và `courses`, theo schema dưới đây. Khi chuyển sang project khác, tạo lại hai resource theo schema này; không cần nested resource hoặc backend riêng.

| Resource | Trường | Kiểu trên MockAPI |
|---|---|---|
| categories | id | Object ID / tự sinh |
| categories | name, description | String |
| courses | id | Object ID / tự sinh |
| courses | title, description, categoryId, instructor | String |
| courses | price, duration | Number |
| courses | thumbnail, trailerUrl, createdAt | String |

Với các field String, chọn giá trị mặc định nếu giao diện bắt buộc chọn generator. App và script sẽ gửi giá trị cụ thể khi POST/PUT. `createdAt` dùng chuỗi ngày `yyyy-MM-dd`. ID luôn lấy từ phản hồi API.

### Nạp 3 danh mục và 10 khóa học mẫu

Dữ liệu đã được nạp sẵn trên project bàn giao. Nếu chuyển sang project mới hoặc cần bổ sung lại các mục mẫu bị xóa, chọn **Cài đặt → Nạp dữ liệu mẫu** trong app, hoặc chạy Python 3:

```powershell
python tools/seed_api.py
```

Với Base URL khác:

```powershell
python tools/seed_api.py https://PROJECT_ID.mockapi.io/api/v1
```

Script dùng thư viện chuẩn Python, không cần cài thêm package. Script không xóa dữ liệu có sẵn, tránh tạo lại mục cùng tên, lấy ID danh mục thật rồi gán `courses.categoryId`. Nếu việc nạp bị gián đoạn, có thể chạy lại. Không chạy nhiều tiến trình nạp cùng lúc.

Dữ liệu mẫu nằm trong `app/src/main/assets/sample-data.json`, chỉ dùng để **POST dữ liệu khởi tạo lên API** khi có yêu cầu. Màn hình danh sách luôn GET dữ liệu từ backend, không dùng file này làm danh sách thay thế khi mất mạng.

Ảnh bìa dùng các URL HTTPS từ Unsplash. Trailer mẫu dùng chung MP4 **Flower (CC0)** từ `https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4` để kiểm tra chức năng phát video; đây là video minh họa, không phải trailer riêng của từng khóa học. Có thể thay URL ở form sửa khóa học. Video Flower là tài nguyên minh họa CC0 của MDN Web Docs; có thể thay bằng trailer MP4 phù hợp của từng khóa học.

## Các màn hình và chức năng

| Màn hình | Hành vi |
|---|---|
| Danh sách khóa học | GET categories + courses; ảnh, tên, giảng viên, tên danh mục, học phí; tìm kiếm theo tên, hỗ trợ không dấu; lọc theo danh mục; tải lại |
| Chi tiết khóa học | GET courses/{id}; đầy đủ thông tin, ảnh, danh mục từ API; xem trailer; sửa; xóa có xác nhận |
| Form thêm/sửa | Toàn bộ trường dữ liệu; Spinner lấy danh mục từ API; xem trước ảnh URL; POST hoặc PUT; kiểm tra dữ liệu trước khi gửi |
| Quản lý danh mục | Danh sách, số khóa học liên quan; thêm, sửa, xóa; chặn xóa khi còn khóa học |
| Cài đặt | Đổi Base URL và nạp dữ liệu mẫu theo yêu cầu |
| Trailer | VideoView + MediaController; phát MP4 ngay trong app; tua, dừng, phát lại; báo tải và báo lỗi |

Các trường bắt buộc được kiểm tra. Học phí phải là số hữu hạn ≥ 0; thời lượng > 0; ngày phải tồn tại; URL ảnh và video phải là HTTPS hợp lệ; danh mục phải được chọn và còn tồn tại trên backend. URL phải dẫn trực tiếp đến ảnh/video; đường dẫn trang YouTube không được dùng thay MP4.

Khi không tải được dữ liệu, app báo lỗi và có nút thử lại. Danh sách không có bản ghi hoặc không khớp tìm kiếm có thông báo riêng. Khi lưu/xóa, app khóa thao tác lặp và hiển thị trạng thái đồng bộ. Sau thay đổi thành công, app GET lại danh sách từ backend.

## REST API đã triển khai

| Method | Endpoint | Mục đích |
|---|---|---|
| GET | /categories | Lấy danh mục cho tên hiển thị, bộ lọc và Spinner |
| POST | /categories | Thêm danh mục |
| PUT | /categories/{id} | Sửa danh mục |
| DELETE | /categories/{id} | Xóa sau khi kiểm tra khóa học liên quan |
| GET | /courses | Lấy danh sách, đồng bộ và kiểm tra ràng buộc |
| GET | /courses/{id} | Lấy chi tiết mới nhất |
| POST | /courses | Thêm khóa học |
| PUT | /courses/{id} | Sửa khóa học |
| DELETE | /courses/{id} | Xóa khóa học |

POST/PUT gửi JSON với `Content-Type: application/json; charset=utf-8`. Các khóa `price` và `duration` được gửi dưới dạng Number. Mọi yêu cầu chạy trên executor nền; callback cập nhật giao diện trên main thread.

## Giải thích mã nguồn

- `Models.java`: ánh xạ JSON thành `Category` và `Course`; khóa ngoại là chuỗi `categoryId`.
- `ApiClient.java`: GET/POST/PUT/DELETE bằng HttpURLConnection, timeout, xử lý mã HTTP và lỗi mạng. `saveCourse()` kiểm tra danh mục hiện tại; `deleteCategory()` GET lại khóa học và chặn xóa nếu có tham chiếu.
- `MainActivity.java`: điều hướng các màn hình, tìm kiếm/lọc, tên danh mục từ dữ liệu API, form và trạng thái giao diện. Glide tải và cache ảnh từ URL.
- `Validation.java`: kiểm tra số, ngày, URL và chuẩn hóa tìm kiếm tiếng Việt.
- `TrailerActivity.java`: phát video trong app, quản lý vòng đời và khôi phục vị trí phát.
- `tools/seed_api.py`: nạp dữ liệu mẫu bằng API, xác minh backend giữ đủ các trường và không có categoryId mồ côi.

Ràng buộc xóa danh mục nằm phía client theo đúng đề. MockAPI không hỗ trợ transaction/foreign key thật: hai thiết bị sửa đồng thời vẫn có thể tạo tranh chấp giữa bước kiểm tra và bước xóa. Bài tập này kiểm tra lại backend ngay trước thao tác để giảm nguy cơ dùng dữ liệu cũ.

## Kiểm thử

Unit test:

```powershell
.\gradlew.bat testDebugUnitTest
```

Kiểm thử tích hợp và giao diện trên điện thoại/máy ảo, khi API đã có đúng 3 danh mục và 10 khóa học mẫu:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

`ApiIntegrationTest` tạo bản ghi tạm, kiểm tra CRUD và hai ràng buộc danh mục rồi dọn đúng các bản ghi do nó tạo. `UiSmokeTest` kiểm tra danh sách, tìm kiếm, lọc, chi tiết, báo lỗi form và phát MP4 thực tế. Bộ test UI giả định dữ liệu mẫu chưa bị thay đổi; nếu tự thêm/xóa khóa học, cần điều chỉnh số lượng kỳ vọng trong test.

Để tự kiểm tra trước khi nộp:

1. Danh sách có ít nhất 3 danh mục và 10 khóa học, ảnh tải được.
2. Tìm `android`, sau đó thử kết hợp tìm kiếm với bộ lọc danh mục.
3. Vào chi tiết, so sánh tên danh mục với resource categories trên MockAPI.
4. Xem trailer và thử dừng/tua/phát lại ngay trong app.
5. Thêm khóa học, thử để trống tên và nhập học phí âm trước; sau đó lưu dữ liệu hợp lệ.
6. Mở MockAPI xác nhận bản ghi được tạo; sửa khóa học trong app rồi kiểm tra PUT đã lưu.
7. Hủy xóa một khóa học, sau đó xác nhận xóa và kiểm tra danh sách được tải lại.
8. Thêm/sửa danh mục. Thử xóa danh mục có khóa học phải bị chặn; danh mục mới rỗng phải xóa được.
9. Tắt mạng để kiểm tra báo lỗi và nút thử lại; bật lại mạng để tải dữ liệu.

Tài liệu nền tảng: [MockAPI Quick start](https://github.com/mockapi-io/docs/wiki/Quick-start-guide), [Android VideoView](https://developer.android.com/reference/android/widget/VideoView).
