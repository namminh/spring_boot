# CoreBank Payment Lab Architecture – Java 21 Upgrade

## 1. Purpose & Scope
- Làm rõ kiến trúc lab sau khi nâng cấp lên Java 21 và Spring Boot 3.2.5.
- Tổng hợp thay đổi kỹ thuật: Virtual Threads, Structured Concurrency, pattern matching for switch, record patterns.
- Cung cấp hướng dẫn kích hoạt cấu hình mới và các lưu ý vận hành/kỹ thuật.

## 2. Platform Upgrade Summary
- Java runtime: `java.version=21`, `maven.compiler.release=21` (`pom.xml`).
- Lab profile mặc định: `SPRING_PROFILES_ACTIVE=lab-java21` để bật virtual threads (`scripts/start_all.sh`, `application-lab-java21.yml`).
- Container baseline (khuyến nghị): `eclipse-temurin:21-jre`.

## 3. Service-Level Changes
### 3.1 Payment Service
- `Payment` chuyển sang `record` bất biến, giảm boilerplate map entity ↔ domain.
- `PaymentController` dùng `StructuredTaskScope.ShutdownOnFailure` với virtual threads để gom số liệu song song.
- `PaymentOrchestratorService` sử dụng pattern matching cho kết quả Tuxedo (`switch` trên `TuxedoPaymentResponse`).
- `MockTuxedoClient` áp dụng record pattern + guard clause cho logic kiểm tra amount/random failure.
- Outbox relay: dispatch song song qua virtual threads, thu kết quả có cấu trúc (`OutboxDispatchResult`).

### 3.2 Monitoring Orchestrator
- `CoreStatusSnapshot` chuyển sang `record`, API trả về metadata rõ ràng.
- Business rule vẫn giữ nguyên nhưng log sử dụng accessor của record.

### 3.3 Investigation Service
- Không thay đổi lớn; tương thích với DTO record mới của payment responses.

## 4. Concurrency Model
- Virtual Thread executor: Spring Boot tự khởi tạo khi profile `lab-java21` bật cấu hình `spring.threads.virtual.enabled=true`.
- Structured concurrency dùng `ShutdownOnFailure(Thread.ofVirtual().factory())` đảm bảo mỗi request spawn virtual tasks độc lập.
- Outbox parallelism: mỗi event gởi trong subtask, thu thập `OutboxDispatchResult` để cập nhật trạng thái.

## 5. Language Features Adoption
- **Records:** `Payment`, `CoreStatusSnapshot`, các DTO nội bộ (payload, command) giảm getter/setter.
- **Pattern Matching for switch:** Logic `PaymentOrchestratorService.eventType`, `MockTuxedoClient.processPayment` trực quan hơn.
- **Record Patterns:** Phân rã `PaymentCommand` để truy cập field nhanh chóng.
- **Sealed/pattern matching (điều chỉnh kế hoạch):** chưa áp dụng sealed interface; có thể xem xét cho Investigation case states.

## 6. Configuration & Operations
- Tệp `application-lab-java21.yml` chứa metric tag `execution-profile=lab-java21` để phân biệt khi giám sát.
- Khi chạy local: `SPRING_PROFILES_ACTIVE=lab-java21 ./scripts/start_all.sh` (thông số mặc định).
- JDK 21 bắt buộc trong máy phát triển và CI; cần cập nhật agent build.
- Theo dõi metric `jvm.threads.virtual.count` và backlog outbox để phát hiện pinning.

## 7. Risks & Mitigations
- **JDK Compatibility:** Driver JDBC cũ có thể không thân thiện với virtual threads → fallback sang executor truyền thống cho endpoint cụ thể nếu phát hiện pinning.
- **Tooling:** GitHub Actions và runner nội bộ cần cài Temurin 21; đảm bảo agent tự động cập nhật hoặc bị chốt phiên bản. Tham khảo Jenkins declarative pipeline mẫu ở `infra/jenkins/Jenkinsfile` (dựa trên container `maven:3.9.6-eclipse-temurin-21`).
- **Observability:** Virtual thread stack trace dài hơn; khuyến nghị dùng JDK Mission Control 8.3+.

## 8. Next Steps
1. Theo dõi lần chạy đầu của workflow `.github/workflows/ci.yml` và pipeline Jenkins để xác nhận build/test pass trên Temurin 21.
2. Bổ sung ADR mô tả quyết định áp dụng Virtual Threads + structured concurrency.
3. Đánh giá `MockTuxedoClient` với workload thực để hiệu chỉnh guard/latency.
