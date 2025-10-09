# Tài liệu chuẩn bị phỏng vấn Senior Java (Oracle) dựa trên CoreBank Payment Lab

Tài liệu này ghi lại cách bạn có thể sử dụng CoreBank Payment Lab để minh họa kinh nghiệm đáp ứng JD "Senior Java (Oracle)". Nội dung được viết bằng tiếng Việt, dễ kể lại trong buổi phỏng vấn hoặc khi viết CV.

---

## 1. Vai trò lãnh đạo kỹ thuật & xử lý sự cố
- **Chuyện để kể**: Bạn chịu trách nhiệm module thanh toán, hiểu luồng `PaymentOrchestratorService` (file `src/main/java/com/corebank/payment/application/PaymentOrchestratorService.java`). Khi adapter Tuxedo lỗi, log `NAMNM SRV` ghi rõ reference, mã lỗi, giúp bạn nhanh khoanh vùng.
- **Sự cố & phản ứng**: Dẫn thêm `MonitoringOrchestrator` và `MonitoringController` (gói `com.corebank.orchestrator`). Chúng ghi log `NAMNM ORCH` với số lượng giao dịch pending/failed, báo động manual. Bạn có thể kể quy trình từ phát hiện (metrics/alert) đến dựng war-room, chạy script kiểm tra, rồi đóng sự cố.
- **Mở rộng domain**: Service tra soát mới (`com.corebank.investigation`) chứng minh khả năng mở rộng microservice, phối hợp payment để hỗ trợ CSKH khi cần truy vấn trạng thái giao dịch.

## 2. Kỹ năng Java backend + Oracle
- **Kiến trúc**: Lab theo chuẩn Spring Boot 3: API → Application → Domain → Infrastructure (xem `README.md` và cấu trúc gói). Đây là bằng chứng bạn quen microservice Java.
- **Database**: Dù lab dùng H2, cấu hình `application.yml` cho phép đổi sang Oracle/PostgreSQL bằng biến môi trường (`SPRING_DATASOURCE_URL`, `USERNAME`, `PASSWORD`). Bạn có thể kể kinh nghiệm migrate qua Oracle, khai thác JPA & transaction.

## 3. Vận hành trên AWS
- **Câu chuyện mẫu**: Trình bày cách đưa app vào Amazon EKS/ECS: build jar → Docker → ECR → Kubernetes (HPA dựa trên metrics Actuator). Database dùng Amazon RDS (Oracle), backup tự động, Multi-AZ. Log đưa lên CloudWatch, cảnh báo CloudWatch Alarm.
- **Chuyển giao**: Nhắc tới việc cấu hình secret qua AWS Secrets Manager/Parameter Store nhờ lab đã tách config bằng biến môi trường.

## 4. Monitoring, incident response, tối ưu
- **Monitoring**: Lab đã có `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`; API `/api/v1/payments/metrics/status-count`; script Groovy `scripts/monitor.groovy` gửi alert Slack khi FAILED vượt ngưỡng.
- **Incident response**: Chạy `scripts/test_monitoring_flow.sh` sau sự cố để kiểm tra endpoint giám sát hoạt động lại. Log `NAMNM SRV/ORCH/INV` cung cấp dữ liệu cho postmortem.
- **Tối ưu**: Trình bày việc dùng Redis cache (`CacheConfig`) giảm tải DB, Resilience4j (`resilience4j.retry`) chống đột biến lỗi, script smoke `scripts/test_payment_flow.sh` để đo hiệu năng cơ bản.

## 5. CI/CD, DevOps, tự động hóa
- **Pipeline**: Giải thích pipeline GitLab CI gồm stage build (`mvn clean verify`), smoke (`scripts/test_payment_flow.sh`), package (Docker). Đề cập file `scripts/start_all.sh` giúp dựng môi trường local nhanh để QA.
- **Automation**: Groovy (`scripts/monitor.groovy`), bash script (`scripts/test_monitoring_flow.sh`) chứng minh bạn chủ động viết tool hỗ trợ vận hành.

## 6. ITIL & Managed Services
- **Process**: `MonitoringOrchestrator` phân loại HEALTHY/WARNING/DEGRADED, giống ITIL Incident severity. Bạn có thể mô tả cách log và alert tạo ticket, sau đó ghi runbook.
- **Tài liệu**: `docs/corebank_tuxedo_payment_solution.md` đã mô tả quy trình triển khai, monitoring, DevOps – dùng làm bằng chứng tuân theo quy trình vận hành chuyên nghiệp.

## 7. Groovy scripting
- **Ví dụ thực tế**: `scripts/monitor.groovy` gọi API metrics, kiểm tra FAILED, gửi Slack. Khi phỏng vấn, mang ví dụ này để chứng minh bạn biết dùng Groovy làm automation.

## 8. Chuẩn bị phỏng vấn/portfolio
1. **Chạy demo**: Dùng `scripts/start_all.sh` để bật cả payment (8080) và monitoring (8081). Lần lượt gọi `curl` hoặc script smoke để minh họa.
2. **Kể câu chuyện**: Mô tả một sự cố giả định – ví dụ Tuxedo trả lỗi – cho thấy bạn đọc log `NAMNM`, xem metrics, tạo alert, xử lý.
3. **Liên hệ AWS**: Nói về kinh nghiệm trước đây + cách sẽ triển khai lab trên AWS (EKS, RDS, CloudWatch). Nếu đã có chứng chỉ (AWS, ITIL, Oracle), nhớ đề cập.
4. **Cập nhật CV**: Thêm phần "CoreBank Payment Lab" mô tả trách nhiệm: xây dựng, vận hành, monitoring, automation.

---

## Kế hoạch hành động nhanh
- Tập chạy `scripts/test_payment_flow.sh` và `scripts/test_monitoring_flow.sh` để quen output.
- Cấu hình Slack token thật cho `scripts/monitor.groovy` và chạy thử.
- Soạn 2-3 câu chuyện STAR (Situation–Task–Action–Result) dựa trên lab về: xử lý sự cố, tối ưu hiệu năng, cải tiến CI/CD.
- Mang tài liệu này vào buổi phỏng vấn để nhớ các điểm chính.

Chúc bạn phỏng vấn thành công với case study CoreBank Payment Lab!
