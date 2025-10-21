# Cốt lõi kiến trúc: Single Source of Truth (SSOT)

Thông điệp trọng tâm: quyết định kiến trúc quan trọng nhất không nằm ở việc chọn React hay Vue, monolith hay microservices, mà là xác định quyền sở hữu dữ liệu, ranh giới dữ liệu và cách các thành phần giao tiếp dựa trên một nguồn sự thật duy nhất (Single Source of Truth – SSOT). Khi cơ sở dữ liệu và dịch vụ backend đóng vai trò hệ thống ghi nhận duy nhất, còn frontend chỉ phản chiếu trạng thái qua hợp đồng API rõ ràng, hệ thống sẽ ổn định, dễ mở rộng và ít lỗi hơn.

---

## 1. SSOT là gì?
- SSOT (Single Source of Truth) là nguyên tắc mỗi mẩu dữ liệu có đúng một nơi “sở hữu” và được coi là bản chân lý để mọi nơi khác đồng bộ.
- Trong môi trường phân tán, SSOT giúp tránh lệch pha và mâu thuẫn dữ liệu vì mọi hệ thống khác chỉ tham chiếu đến một điểm thẩm quyền duy nhất.
- Ở cấp doanh nghiệp, triển khai SSOT thường đi kèm cơ chế tích hợp và luồng đồng bộ (events, ETL, polling) để đảm bảo mọi bản sao đều được cấp từ nguồn chính.

---

## 2. Vì sao không để frontend “sở hữu” sự thật?
- Khi frontend giữ logic nghiệp vụ hoặc trạng thái cốt lõi song song với backend, thực chất đang tạo hai nguồn sự thật cạnh tranh.
- Độ trễ mạng và tải cao dễ làm nảy sinh race condition và sai lệch trạng thái; bug khó tái hiện vì lệch nhịp giữa client/server.
- Đối xử frontend như projection lấy dữ liệu qua hợp đồng chuẩn giúp giữ logic tập trung, giảm bug ẩn, tăng tính đoán định và kiểm soát chất lượng.

---

## 3. Hợp đồng dữ liệu trước (Contract-first)
- Thiết kế schema/hợp đồng trước bằng OpenAPI, GraphQL, JSON Schema hoặc protobuf.
- Backend và frontend chia sẻ cùng cấu trúc, ràng buộc, mẫu lỗi trả về; có thể sinh mã, mock và test tự động.
- Quy trình CI/CD phải chặn thay đổi phá vỡ hợp đồng: treat schema như hiện thân chung của “sự thật”.
- Khi tiến hóa API, dùng versioning rõ ràng và tài liệu thay đổi để đảm bảo mọi consumer cập nhật kịp.

---

## 4. Kiểm soát concurrency & bất biến ở tầng dữ liệu
- Đưa bất biến (constraints, ràng buộc duy nhất) vào database để mọi thao tác ghi đều kiểm tra tại nguồn.
- Sử dụng giao dịch ACID giúp chuỗi kiểm-tra–cập-nhật diễn ra nguyên tử, không phụ thuộc logic UI.
- Endpoint nên thiết kế idempotent, trả về cùng kết quả khi request lặp lại nhằm loại bỏ race do retry/delay.
- Cơ chế khóa lạc quan/bi quan, phiên bản hàng (`version`, `updated_at`) giúp đảm bảo không ghi đè mất dữ liệu.

---

## 5. Monolith hay Microservices?
- Cả hai kiến trúc đều có thể mở rộng nếu ranh giới và quyền sở hữu dữ liệu rõ ràng.
- Tách microservices sớm mà chưa xác định SSOT thường khiến dữ liệu bị phân mảnh, tích hợp phức tạp và lệch thẩm quyền.
- Bắt đầu từ ranh giới miền (bounded context) mạch lạc; chỉ tách dịch vụ khi cần về tổ chức hoặc hiệu năng, nhưng vẫn giữ cơ chế chia sẻ sự thật thông qua hợp đồng và sự kiện.

---

## 6. Áp dụng thực tế
- **Chỉ định hệ thống ghi nhận** cho từng thực thể: ai được ghi, ai chỉ đọc, cách lan truyền thay đổi (event/polling).
- **Quản lý hợp đồng API** tập trung, lint/CI chống breaking change, sinh type cho client/server từ cùng một schema.
- **Ghi dữ liệu nguyên tử & idempotent**, áp ràng buộc duy nhất, cơ chế khóa cạnh tranh để đóng mọi khe đua.
- **Dùng event để phát tán sự thật** tới cache/projection, nhưng giữ “master” quyết định xung đột.
- **Frontend như cache hiển thị**: áp dụng TTL, ETag, hoặc subscription sự kiện; tránh tự tách nhánh nghiệp vụ.

---

## 7. Kết luận
- SSOT, hợp đồng dữ liệu nghiêm ngặt và kiểm soát đồng thời ở tầng lưu trữ là đòn bẩy chính mang lại độ tin cậy, khả năng mở rộng và tốc độ phát triển.
- Tranh luận về framework UI hay mô hình triển khai chỉ mang lại lợi ích nhỏ nếu thiếu nền tảng dữ liệu thống nhất.
- Đặt SSOT làm kim chỉ nam giúp đội ngũ tập trung giải quyết vấn đề khó nhất: dữ liệu nhất quán, chuẩn hóa giao tiếp và vận hành bền vững trong dài hạn.
