# Kiến trúc chi tiết CoreBank Payment Lab (Java 21)

Tài liệu này mô tả chi tiết kiến trúc và chức năng của ba dịch vụ trong CoreBank Payment Lab sau khi nâng cấp lên Java 21 và Spring Boot 3.2.5. Nội dung đi từ cái nhìn tổng thể tới phân tích từng lớp, từng phương thức quan trọng để tiện tra cứu và onboarding cho đội phát triển/vận hành.

---

## 1. Bức tranh tổng quan
- **Thành phần chính**
  - `corebank-payment`: xử lý khởi tạo và điều phối thanh toán, phát sự kiện ở chế độ outbox, đồng thời có ứng dụng Kafka Streams để phân tích sự kiện.
  - `corebank-orchestrator`: cung cấp API giám sát, hợp nhất số liệu từ thanh toán và cảnh báo vận hành.
  - `corebank-investigation`: quản lý hồ sơ điều tra liên quan tới khiếu nại giao dịch, đồng bộ trạng thái thanh toán qua REST.
- **Nâng cấp nền tảng**
  - Java 21 (`java.version=21`, `maven.compiler.release=21`) và Spring Boot 3.2.5.
  - Virtual Threads & Structured Concurrency cho các tác vụ song song nhẹ (REST metrics, outbox dispatch).
  - Ngôn ngữ Java 21: record, pattern matching cho `switch`, record pattern trong `MockTuxedoClient`.
  - Spring Cloud (Config, Netflix Eureka Client, LoadBalancer) sẵn sàng dùng khi triển khai đám mây.
- **Chế độ chạy Lab**
  - Mặc định `SPRING_PROFILES_ACTIVE=lab-java21` kích hoạt virtual thread (`spring.threads.virtual.enabled=true`).
  - Cấu hình bổ trợ ở `application-lab-java21.yml` (cache, Kafka, metric tag `execution-profile=lab-java21`).

---

## 2. Luồng nghiệp vụ tổng quát
1. **Tạo thanh toán**: REST (`PaymentController.initiatePayment`) nhận lệnh, `PaymentOrchestratorService` ghi DB, gọi Tuxedo (mock), cập nhật trạng thái và put sự kiện vào bảng outbox.
2. **Phát sự kiện**: `OutboxRelay.forwardPendingEvents` chạy theo lịch, dùng structured concurrency để đẩy từng sự kiện song song tới Kafka (hoặc log fallback).
3. **Giám sát**: Orchestrator đọc bảng giao dịch/cảnh báo để trả về `CoreStatusSnapshot`, danh sách giao dịch gần nhất, cảnh báo đang mở; đồng thời bắn `AlertCreatedEvent` khi có alert thủ công.
4. **Điều tra**: Investigation Service tạo/tìm/cập nhật hồ sơ, gọi REST về Payment Service để xác nhận trạng thái thanh toán, lưu ghi chú.
5. **Kafka Streams**: `PaymentEventStreamProcessor` đọc topic `payments.txn.completed`, enrich payload và đẩy sang topic phân tích.

---

## 3. Dịch vụ Payment (`com.corebank.payment`)

### 3.1 Lớp khởi động & cấu hình
- `CorebankPaymentApplication`: bật auto-configuration Spring Boot và scheduling (cần cho outbox relay).
- `CacheConfig.cacheManager(...)`: tạo Redis cache TTL 5 phút khi `payment.cache.enabled=true`.
- `SecurityConfig.securityFilterChain(...)`: cho phép `/actuator/**`, các API khác yêu cầu HTTP Basic.

### 3.2 Lớp API (`api`)
- `PaymentController`
  - `initiatePayment(PaymentRequestDto)`: validate payload, log request, chuyển đổi sang `PaymentCommand`, gọi orchestrator, trả về 201 với `PaymentResponseDto`. Log theo dõi ở đầu/cuối.
  - `fetchPayment(String reference)`: đọc từ orchestrator (có cache), trả về 200/404 tùy tồn tại.
  - `statusBreakdown()`: tạo `StructuredTaskScope.ShutdownOnFailure` (virtual threads) để đếm số lượng theo `PaymentStatus` song song, gom kết quả vào `EnumMap`. Bắt `InterruptedException`/`ExecutionException` để đảm bảo dọn dẹp scope.
- `PaymentRequestDto` / `PaymentResponseDto`: record DTO với constraint validation (số tiền tối thiểu, trường bắt buộc).

### 3.3 Tầng ứng dụng (`application`)
- `PaymentOrchestratorService`
  - `orchestrate(PaymentCommand)`: giao dịch chính, lưu `PaymentEntity` với trạng thái `RECEIVED` → `IN_PROGRESS`, gọi `TuxedoGateway.process(...)`, ánh xạ pattern matching sang `PaymentStatus` cuối (`COMPLETED`/`FAILED`), lưu outbox, cập nhật cache (Put/Evict).
  - `findByReference(String)`: đọc `PaymentRepository` + cache. `unless = "#result.isEmpty()"` tránh cache kết quả rỗng.
  - `countByStatus(PaymentStatus)`: đếm theo trạng thái với cache.
  - `storeOutboxEvent(...)`: build payload JSON bằng `ObjectMapper`, lưu bản ghi pending vào outbox.
  - `eventType(...)`: pattern matching response → tên sự kiện (`COMPLETED`/`FAILED`).
  - `PaymentEventPayload` (record private): cấu trúc JSON outbox.
- `TuxedoGateway.process(PaymentCommand)`: đóng vai trò anti-corruption layer; dùng Resilience4j `@Retry(name="tuxedo-process")` và log trước/ sau khi gọi `TuxedoClient`.

### 3.4 Domain (`domain`)
- `Payment`: record bất biến, ánh xạ từ entity qua `PaymentEntity.toDomain()`.
- `PaymentCommand`: record chứa thông tin tạo giao dịch (từ REST).
- `PaymentStatus`: enum `RECEIVED`, `IN_PROGRESS`, `COMPLETED`, `FAILED`.

### 3.5 Hạ tầng – CSDL (`infrastructure.persistence`)
- `PaymentEntity`
  - Field DB + hook `@PrePersist`, `@PreUpdate` tự set timestamp.
  - `fromDomain(...)` / `toDomain()`: chuyển đổi hai chiều.
  - Getter/setter phục vụ JPA & cập nhật trạng thái.
- `PaymentRepository`: `JpaRepository<PaymentEntity, UUID>` với `findByReference` và `countByStatus`.

### 3.6 Hạ tầng – Outbox (`infrastructure.outbox`)
- `OutboxEventEntity`
  - Factory `pending(...)`: khởi tạo sự kiện PENDING với `eventId` ngẫu nhiên và `createdAt` hiện tại.
  - Thuộc tính `status`, `lastAttemptAt`, `version` (optimistic locking).
- `OutboxRepository`: tìm 20 bản ghi PENDING theo thời gian.
- `OutboxStatus`: enum `PENDING`, `SENT`, `FAILED`.
- `PaymentEventPublisher`: contract phát sự kiện (Kafka hoặc log).
- `OutboxRelay`
  - `forwardPendingEvents()`: chạy theo lịch, nếu rỗng thì log idle; ngược lại dispatch song song, ghi nhận kết quả và cập nhật trạng thái/`lastAttemptAt`.
  - `dispatchInParallel(List)`: structured concurrency với virtual thread (ShutdownOnFailure). Fork mỗi sự kiện, join, throwIfFailed, rồi thu kết quả.
  - `publish(OutboxDispatchCommand)`: thực thi `eventPublisher.publish`, trả về `OutboxDispatchResult` đánh dấu `SENT` hoặc `FAILED`.
  - `OutboxDispatchCommand`/`OutboxDispatchResult`: record nội bộ giúp pattern matching khi cập nhật.

### 3.7 Hạ tầng – Tuxedo (`infrastructure.tuxedo`)
- `TuxedoClient`: interface để thay thế bằng client thật.
- `MockTuxedoClient.processPayment(...)`:
  - Gây trễ ngẫu nhiên 20–120ms (`simulateLatency()`).
  - Pattern matching `PaymentCommand`:
    - Amount > 500.000.000 → `LIMIT_EXCEEDED`.
    - 5% xác suất thất bại ngẫu nhiên → `RANDOM_FAIL`.
    - Còn lại trả `success("APPROVED")`.
- `TuxedoPaymentResponse`: record + factory `success`/`failure`.

### 3.8 Hạ tầng – Sự kiện (`infrastructure.event`)
- `KafkaPaymentEventPublisher.publish(...)`: Khi `payment.events.kafka-enabled=true`, gửi tới topic `payments.txn.<eventType>` (lowercase), log success/failure callback.
- `LoggingPaymentEventPublisher.publish(...)`: fallback log payload (khi Kafka tắt).

### 3.9 Kafka Streams (`stream`)
- `PaymentEventStreamApplication`
  - `streamRunner(...)`: chỉ tạo `ApplicationRunner` khi `payment.streams.enabled=true`.
- `PaymentStreamConfig.paymentEventStreamProcessor(...)`: log cấu hình Streams, tạo processor với `StreamsConfig.originals()`.
- `PaymentEventStreamProcessor`
  - `buildAndStart()`: dựng topology đọc `payments.txn.completed`, log mỗi bản ghi, enrich bằng transformer, ghi sang `payments.analytics.completed`. Đăng ký uncaught exception handler đóng streams nếu gặp lỗi.
- `PaymentEventTransformer.transform(...)`: thêm trường `processedAt` (ISO8601) và `amountMinor` (đơn vị nhỏ nhất), bắt lỗi parse và log cảnh báo.

---

## 4. Monitoring Orchestrator (`com.corebank.orchestrator`)

### 4.1 Cấu hình
- `SecurityConfig`: bật HTTP Basic, user in-memory `orchestrator/changeme`, permit `/actuator/health|info`.

### 4.2 API (`api`)
- `MonitoringController`
  - `status()`: log yêu cầu, gọi `MonitoringOrchestrator.snapshotCoreStatus()`, trả `CoreStatusSnapshot`.
  - `recentTransactions(int hours)`: giới hạn tham số 1–24h, tính `since`, gọi orchestrator, trả danh sách.
  - `activeAlerts()`: trả danh sách cảnh báo chưa acknowledge.
  - `createAlert(CreateAlertRequest)`: dựng đối tượng `Alert`, set timestamp, gửi cho orchestrator lưu + trả 201.
- `CreateAlertRequest`: DTO với validation severity (INFO/WARN/CRITICAL) và message ≤ 512 ký tự.

### 4.3 Orchestrator (`orchestrator`)
- `MonitoringOrchestrator`
  - `snapshotCoreStatus()`: đếm transaction `PENDING`/`FAILED`, alert chưa acknowledge, xác định trạng thái hệ thống (`HEALTHY`/`WARNING`/`DEGRADED`) và thông điệp giải thích. Log cả input/output.
  - `loadRecentTransactions(OffsetDateTime)`: truy vấn 20 giao dịch mới nhất sau thời điểm `since`.
  - `loadActiveAlerts()`: lấy alert chưa acknowledge, order mới nhất.
  - `recordManualAlert(Alert)`: lưu alert, publish `AlertCreatedEvent`, trả entity đã lưu.
- Giao dịch mặc định `@Transactional`, các phương thức read-only đánh dấu rõ để tránh lock không cần thiết.

### 4.4 Domain & Persistence
- `CoreStatusSnapshot`: record cho phản hồi `/status`.
- `Alert`: entity JPA `monitoring_alerts` với trường severity/message/acknowledged/createdAt.
- `PaymentTransaction`: entity lưu giao dịch (reference, amount, status, processedAt).
- `AlertRepository`: `findByAcknowledgedFalseOrderByCreatedAtDesc`, `countByAcknowledgedFalse`.
- `PaymentTransactionRepository`: `findTop20ByProcessedAtAfterOrderByProcessedAtDesc`, `countByStatus`.

### 4.5 Sự kiện
- `MonitoringEventPublisher.publishAlertCreated(Alert)`: interface để decouple hạ tầng publish.
- `SpringMonitoringEventPublisher.publishAlertCreated(...)`: dùng `ApplicationEventPublisher`, log sự kiện cho audit.
- `AlertCreatedEvent`: wrapper gửi trên Spring event bus.

---

## 5. Investigation Service (`com.corebank.investigation`)

### 5.1 Cấu hình chung
- `CorebankInvestigationApplication`: app entry point.
- `InvestigationConfig`: enable `@ConfigurationProperties`, cung cấp `RestTemplate` dùng cho PaymentStatusClient.
- `InvestigationProperties`
  - `defaultAssignee`: cấu hình mặc định người xử lý (default: `ops-investigation`).
  - `paymentService.baseUrl`: URL tới Payment Service (mặc định `http://localhost:8080`).
- `SecurityConfig`: HTTP Basic, user `investigator/changeme`, permit health/info.

### 5.2 API (`api`)
- `InvestigationController`
  - `create(CreateInvestigationRequest)`: tạo mới hoặc trả về case tồn tại; log reference, trả 201 + location header.
  - `getById(UUID)`: trả case hoặc 404.
  - `getByReference(String)`: danh sách tối đa 20 case gần nhất cho reference.
  - `updateStatus(UUID, UpdateInvestigationStatusRequest)`: cập nhật trạng thái, ghi chú, người phụ trách; trả 200/404.
  - `appendNote(UUID, Map<String,String>)`: cập nhật `lastNote` qua PATCH.
  - `statusCount()`: trả map đếm số case theo `InvestigationStatus`.
- DTO
  - `CreateInvestigationRequest`: record với `@NotBlank` reference/reportBy.
  - `UpdateInvestigationStatusRequest`: record với `@NotNull` status, optional note/assignedTo.
  - `InvestigationResponse.fromDomain(...)`: chuyển đổi `InvestigationCase` sang DTO phản hồi.

### 5.3 Tầng dịch vụ (`application`)
- `InvestigationService`
  - `createCase(...)`: idempotent theo reference; nếu chưa có thì sinh UUID mới, set `OPEN`, gán `defaultAssignee`, lưu; sau đó gọi `PaymentStatusClient.fetchStatus` để log trạng thái thanh toán hiện tại.
  - `findById(UUID)` / `findByReference(String)`: trả Optional domain.
  - `updateStatus(...)`: cập nhật trạng thái, `statusChangedAt`, ghi chú (nếu có), người phụ trách (nếu có).
  - `appendNote(UUID, String)`: cập nhật `lastNote`.
  - `statusCount()`: duyệt enum `InvestigationStatus`, đếm từng giá trị bằng repository.
  - `recentByReference(String)`: lấy tối đa 20 case mới nhất cho reference.

### 5.4 Domain & Persistence
- `InvestigationCase`: domain immutable (fields final, getter).
- `InvestigationStatus`: enum `OPEN`, `IN_PROGRESS`, `WAITING_CUSTOMER`, `RESOLVED`, `REJECTED`.
- `InvestigationCaseEntity`
  - `fromDomain`/`toDomain` chuyển đổi.
  - Hook lifecycle cập nhật timestamp.
- `InvestigationCaseRepository`: truy vấn id/reference, đếm theo status, thống kê top 20 theo updatedAt.

### 5.5 Tích hợp Payment
- `PaymentStatusClient.fetchStatus(String)`
  - Gọi REST đến Payment Service (`/api/v1/payments/{reference}`).
  - Nếu thành công: trả `PaymentStatusSnapshot(true, status, null)`.
  - Nếu lỗi HTTP/IO: log cảnh báo, trả `success=false` cùng thông điệp lỗi.

---

## 6. Các mối quan tâm chung
- **Virtual Threads & Structured Concurrency**
  - Bật qua profile `lab-java21`. `PaymentController.statusBreakdown` và `OutboxRelay` là ví dụ điển hình.
  - Khi dùng `StructuredTaskScope.ShutdownOnFailure`, mọi subtask đều chạy trên virtual thread, tự động hủy nếu có subtask lỗi.
- **Caching**
  - Redis cache cho `paymentByReference` và `paymentStatusCount`.
  - Cache evict ở `orchestrate` tránh thống kê cũ.
- **Bảo mật**
  - Cả ba dịch vụ dùng HTTP Basic với user in-memory (phục vụ demo).
  - `/actuator/health|info` không cần auth trừ Payment cho `/actuator/**`.
- **Resilience**
  - `TuxedoGateway` dùng Resilience4j retry (config tên `tuxedo-process` thiết lập trong `application-*.yml`).
- **Logging & Theo dõi**
  - Tất cả lớp chính log với prefix `NAMNM` giúp lọc theo module.
  - Khuyến nghị giám sát `jvm.threads.virtual.count`, outbox backlog, Kafka Streams log.
- **Triển khai Cloud**
  - Profile `lab-cloud` bật Spring Cloud Config client, Eureka discovery và Spring Cloud LoadBalancer; kết hợp cùng `lab-java21` khi deploy trên đám mây.
  - Mỗi service gán `spring.application.name` mặc định (`corebank-payment`, `corebank-orchestrator`, `corebank-investigation`, `corebank-payment-stream`) để đăng ký và định tuyến qua Eureka.
  - `PaymentStatusClient` dùng RestTemplate load-balanced khi discovery bật, tự động rơi về gọi trực tiếp khi chạy môi trường cục bộ.
  - Quy trình đầy đủ xem thêm `docs/corebank_payment_lab_cloud.md`, playbook tích hợp cloud-native `docs/cloud_native_legacy_integration.md` và hướng dẫn triển khai từng bước `docs/cloud_deployment_guide.md`.

---

## 7. Vận hành & Kiểm thử
- Bắt buộc JDK Temurin 21 cho máy dev, CI/CD (`maven:3.9.6-eclipse-temurin-21` trong Jenkins mẫu).
- Khi bật Kafka hoặc Redis trong lab, đảm bảo biến môi trường tương ứng (`payment.events.kafka-enabled`, `spring.data.redis.*`).
- Triển khai cloud: bật `SPRING_PROFILES_ACTIVE=lab-java21,lab-cloud`, cấu hình `EUREKA_CLIENT_SERVICE_URL`, `SPRING_CLOUD_CONFIG_URI` hoặc override bằng biến môi trường tương ứng cho Spring Cloud.
- Quan sát run đầu tiên của `.github/workflows/ci.yml` và Jenkins pipeline để đảm bảo build/test với Java 21 ổn định.
- Cân nhắc viết ADR ghi nhận quyết định virtual threads + structured concurrency, cũng như đánh giá `MockTuxedoClient` bằng workload thực.

---

## 8. Phụ lục – Tra cứu nhanh phương thức

| Thành phần | Phương thức | Mô tả ngắn gọn |
|------------|-------------|----------------|
| `PaymentController` | `initiatePayment` | Nhận yêu cầu thanh toán, gọi orchestrator, trả response 201. |
| | `fetchPayment` | Đọc trạng thái theo reference, 200/404. |
| | `statusBreakdown` | Đếm số lượng theo `PaymentStatus` bằng virtual threads. |
| `PaymentOrchestratorService` | `orchestrate` | Quy trình lưu payment, gọi Tuxedo, ghi outbox, cập nhật cache. |
| | `findByReference` | Lấy payment (cacheable). |
| | `countByStatus` | Đếm payment theo trạng thái (cacheable). |
| | `storeOutboxEvent` | Tạo bản ghi outbox PENDING. |
| `OutboxRelay` | `forwardPendingEvents` | Lấy 20 sự kiện PENDING, dispatch song song, cập nhật trạng thái. |
| | `dispatchInParallel` | Dựng structured concurrency scope để publish. |
| `TuxedoGateway` | `process` | Gọi `TuxedoClient` với retry, log kết quả. |
| `MockTuxedoClient` | `processPayment` | Mô phỏng thành công/thất bại dựa trên amount & random. |
| `MonitoringController` | `status` | Trả snapshot hệ thống. |
| | `recentTransactions` | Danh sách giao dịch mới nhất theo khoảng giờ. |
| | `activeAlerts` | Alert chưa acknowledge. |
| | `createAlert` | Ghi alert thủ công. |
| `MonitoringOrchestrator` | `snapshotCoreStatus` | Tổng hợp số liệu pending/failed/alert để đánh giá trạng thái. |
| | `loadRecentTransactions` | Lấy 20 giao dịch sau thời điểm cho trước. |
| | `recordManualAlert` | Lưu alert và phát event. |
| `InvestigationController` | `create` | Tạo hoặc trả case tồn tại. |
| | `getById` | Tra case theo UUID. |
| | `getByReference` | Danh sách case theo reference. |
| | `updateStatus` | Đổi trạng thái + ghi chú/assignee. |
| | `appendNote` | Cập nhật ghi chú cuối. |
| | `statusCount` | Đếm case theo trạng thái. |
| `InvestigationService` | `createCase` | Tạo case mới + tra cứu Payment Service. |
| | `updateStatus` | Cập nhật trạng thái case. |
| | `appendNote` | Lưu ghi chú mới. |
| | `statusCount` | Thống kê số lượng case từng trạng thái. |
| | `recentByReference` | Lấy case mới nhất theo reference. |
| `PaymentEventStreamProcessor` | `buildAndStart` | Dựng Kafka Streams topology, enrich event, khởi động luồng. |
| `PaymentEventTransformer` | `transform` | Bổ sung trường `processedAt`, `amountMinor`, xử lý lỗi parse. |

---

## 9. Điểm nhấn kỹ năng & Giá trị tuyển dụng
- **Kỹ năng then chốt**
  - Vận dụng đầy đủ tính năng mới của Java 21 (virtual thread, structured concurrency, record, pattern matching) trong bối cảnh sản xuất để giảm latency xử lý và tăng throughput nhờ song song hóa nhẹ.
  - Thiết kế microservice Spring Boot 3.2 với REST API rõ ràng, domain phân lớp, bảo mật HTTP Basic, caching Redis và lớp resilience dựa trên retry/pattern outbox nhằm giữ SLA thấp.
  - Triển khai kiến trúc hướng sự kiện với outbox pattern, Kafka Streams cùng hệ thống giám sát, cảnh báo và metrics hỗ trợ vận hành thực tế, tối ưu pipeline dữ liệu real-time.
  - Chuẩn bị sẵn sàng cho triển khai đám mây bằng Spring Cloud (Config, Service Discovery, Gateway, Circuit Breaker) để mở rộng linh hoạt theo lưu lượng.
- **Giá trị đem lại cho nhà tuyển dụng**
  - Rút ngắn thời gian nâng cấp nền tảng Java nhờ sẵn quy trình, cấu hình và best practice có thể nhân rộng cho các nhóm khác.
  - Liên kết chặt chẽ giữa kiến trúc, vận hành và CI/CD, giúp phát hành ổn định với độ tin cậy cao trong các hệ thống yêu cầu throughput lớn.
  - Cung cấp tài liệu chi tiết và góc nhìn tổng thể để onboarding, chuyển giao tri thức, đảm bảo sản phẩm duy trì được chất lượng dài hạn, hướng vào tối ưu latency đầu-cuối.

---

## 10. Phụ lục – Giải thích các kiến thức Java 21 trọng điểm

### 10.1 Virtual Thread và Structured Concurrency
- **Virtual Thread** (Project Loom) là lightweight thread được JVM quản lý thay vì OS, cho phép tạo hàng nghìn thread chi phí thấp; phù hợp workload I/O-bound như call REST hoặc publish Kafka. Trong Lab, `PaymentController.statusBreakdown` và `OutboxRelay` khai thác thông qua cấu hình `spring.threads.virtual.enabled=true`.
- **StructuredTaskScope** cung cấp structured concurrency: mọi task con sống trong một scope rõ ràng, bị hủy khi scope đóng giúp tránh rò rỉ hoặc task zombie.
- Ví dụ rút gọn từ `statusBreakdown`:

```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Map<PaymentStatus, Future<Long>> futures = new EnumMap<>(PaymentStatus.class);
    for (PaymentStatus status : PaymentStatus.values()) {
        futures.put(status, scope.fork(() -> orchestrator.countByStatus(status)));
    }
    scope.join().throwIfFailed(); // chờ tất cả virtual thread hoàn tất
    return futures.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().resultNow()));
}
```
- **Khi nào dùng**: ưu tiên cho tác vụ có độ trễ I/O cao, muốn giữ model lập trình thread truyền thống nhưng tránh phức tạp của pool. Không nên dùng virtual thread cho CPU-bound kéo dài vì vẫn chia sẻ cùng core.
- **Best practice**: đóng scope trong `try-with-resources`, xử lý `InterruptedException`, ưu tiên bất biến/immutable data để chia sẻ giữa các virtual thread.

### 10.2 Record class và bất biến
- `record` giới thiệu từ Java 16, được tận dụng cho DTO (`PaymentRequestDto`, `PaymentResponseDto`) và domain (`Payment`, `PaymentCommand`). Record tự sinh constructor, accessors (`component()`), `equals/hashCode/toString`, giúp giảm boilerplate và khuyến khích thiết kế bất biến.
- Khi cần logic bổ sung, có thể khai báo compact constructor:

```java
public record PaymentCommand(String reference, BigDecimal amount) {
    public PaymentCommand {
        Objects.requireNonNull(reference);
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}
```
- Record phù hợp vai trò data-carrier; nếu cần mutable state (ví dụ entity JPA) thì tiếp tục dùng class thông thường.

### 10.3 Pattern matching cho `switch` và record pattern
- Java 21 cho phép `switch` dựa trên pattern thay vì so sánh hằng số, giúp code phân nhánh rõ ràng hơn. Ở Lab: `PaymentOrchestratorService.eventType(...)` hoặc `MockTuxedoClient` dùng để phân biệt response.

```java
return switch (response) {
    case TuxedoResponse.Success s -> new PaymentStatusSnapshot(true, s.status(), null);
    case TuxedoResponse.Failure f -> new PaymentStatusSnapshot(false, null, f.reason());
};
```
- **Record pattern**: destructuring trực tiếp record, giảm getter thủ công; compiler kiểm tra exhaustiveness để tránh bỏ sót case.
- **Lưu ý**: luôn thêm `default` hoặc bảo đảm `switch` exhaustive để compiler cảnh báo khi mở rộng sealed hierarchy.

### 10.4 Virtual Thread với API blocking
- Virtual thread tương thích hoàn toàn với API blocking hiện có (JDBC, RestTemplate). Vì vậy `RestTemplate` trong `PaymentStatusClient` hay `JpaRepository` trong service có thể gọi trực tiếp mà không cần chuyển sang reactive.
- **Pitfall**: thư viện khóa `synchronized` dài hoặc thao tác CPU nặng vẫn gây nghẽn. Nên tách logic CPU-bound sang executor chuyên dụng hoặc dùng `StructuredTaskScope` khác.

### 10.5 Công cụ lập trình hàm và bất biến
- `Optional`, `Stream`, `EnumMap` hay `Collectors.toMap` xuất hiện rộng rãi. Kết hợp record và immutable collection giúp code thread-safe hơn, tránh race condition khi chạy song song.
- Ví dụ ghép dữ liệu bất biến:

```java
return Arrays.stream(PaymentStatus.values())
        .collect(Collectors.toUnmodifiableMap(
                Function.identity(),
                status -> orchestrator.countByStatus(status)));
```
- Khi áp dụng trong code thực tế, import thêm `java.util.Arrays`, `java.util.function.Function` và `java.util.stream.Collectors`.

---

## 11. SLA 99.97% – Định nghĩa và cách bảo đảm

### 11.1 Ý nghĩa và cách tính
- SLA 99.97% tương đương tối đa 0.03% downtime trong kỳ đo. Với kỳ 30 ngày, error budget còn lại là **13 phút 1 giây** downtime cho toàn bộ hệ thống CoreBank Payment Lab.
- Công thức tính uptime:

```text
Availability = (Total Time − Downtime) / Total Time × 100%
```

- Downtime bao gồm gián đoạn toàn phần (service/DB/Kafka không phản hồi) và gián đoạn chức năng trọng yếu (không tạo được thanh toán mới > 5 phút). Maintenance có kế hoạch được trừ khỏi total time nếu thông báo ≥24h.

| Kỳ đo | Downtime tối đa cho 99.97% |
|-------|----------------------------|
| 1 ngày | 26 giây |
| 7 ngày | 3 phút 3 giây |
| 30 ngày | 13 phút 1 giây |

### 11.2 Cách đo lường
- **Service Level Indicator (SLI)**: tỉ lệ request HTTP 2xx/3xx cho API thanh toán và orchestration trong rolling window 5 phút. Alert khi SLI < 99.9% trong hai cửa sổ liên tiếp.
- **Synthetic Transaction**: script tạo thanh toán thử 2 phút/lần để phát hiện sớm lỗi Tuxedo/Kafka, ghi vào Prometheus `synthetic_success_ratio`.
- **Outbox Lag**: `pending_events_count` và `max_outbox_delay_ms` giúp phát hiện trễ phát sự kiện (được tính downtime nếu vượt ngưỡng 2 phút).

### 11.3 Kiến trúc hỗ trợ SLA
- **Resilience Pattern**: retry Resilience4j cho Tuxedo, queue relay có backpressure và dead-letter, đảm bảo lỗi tạm thời không gây downtime dài.
- **Cache & Degradation**: Redis cache cho `findByReference` giúp trả lời ngay cả khi DB chậm. Orchestrator cho phép fallback đọc dữ liệu cũ để không làm gián đoạn giám sát.
- **Isolation**: Payment, Orchestrator, Investigation tách biệt; sự cố một service không kéo sập toàn bộ. Kafka Streams chạy riêng `corebank-payment-stream` để tránh ảnh hưởng API.
- **Deployment Strategy**: rolling update với health check `/actuator/health`. Giữ ≥2 replica cho payment và orchestrator để đáp ứng SLA khi 1 instance bảo trì.

### 11.4 Vận hành và error budget
- Error budget 0.03% được sử dụng cho thay đổi sản xuất: mỗi lần release phải ước lượng downtime (ví dụ rolling update 3 phút/30 ngày = 0.0069%, còn 0.0231% cho sự cố).
- Khi error budget còn < 30%, tạm dừng release mới, tập trung tối ưu độ ổn định (game day, chaos testing).
- Runbook downtime phải xác định MTTR mục tiêu ≤ 5 phút: các bước chính gồm kiểm tra health check, Kafka, Redis, DB, Tuxedo Gateway và cho phép failover manual.

### 11.5 Liên hệ SLA với autoscaling Kubernetes
- Để giữ SLA, cần bảo đảm có đủ replica xử lý khi lưu lượng tăng đột biến. HPA (Horizontal Pod Autoscaler) và service mesh Istio hỗ trợ cân bằng tải, điều phối traffic và quan sát chi tiết latency theo pod/route.
- Autoscaling phải được phối hợp cùng error budget: scale-out trước giờ cao điểm giảm rủi ro đụng ngưỡng SLA; scale-in cần grace period để tránh chặn request đang xử lý.

---

## 12. Kubernetes, HPA và Istio trong Payment Lab

### 12.1 Hạ tầng Kubernetes tổng quan
- Mỗi service (`corebank-payment`, `corebank-orchestrator`, `corebank-investigation`, `corebank-payment-stream`) triển khai dạng Deployment + Service trong namespace `payment-lab`.
- ConfigMap/Secret quản lý cấu hình profile `lab-java21,lab-cloud`, thông tin Kafka/Redis được mount qua env.
- Liveness/readiness probe trỏ `/actuator/health/liveness` và `/actuator/health/readiness` để k8s tự phát hiện lỗi, đảm bảo self-healing nhanh hơn MTTR mục tiêu.

### 12.2 Horizontal Pod Autoscaler (HPA)
- HPA dùng Metrics Server hoặc Prometheus Adapter để scale dựa trên CPU, memory hoặc custom metrics (vd `http_server_requests_seconds_count`).
- Đề xuất rule:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: payment-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: corebank-payment
  minReplicas: 2
  maxReplicas: 6
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 55
    - type: Pods
      pods:
        metric:
          name: http_requests_per_second
        target:
          type: AverageValue
          averageValue: "15"
```

- **Lưu ý vận hành**:
  - Đặt `minReplicas` ≥ 2 để đáp ứng SLA dù 1 pod gặp sự cố.
  - Bật `podDisruptionBudget` (ví dụ `minAvailable: 2`) tránh cùng lúc mất nhiều pod khi node bảo trì.
  - Khi metric `http_requests_per_second` vượt ngưỡng lâu dài, tăng `maxReplicas` hoặc tối ưu code để giảm latency.

### 12.3 Istio Service Mesh
- Istio cung cấp traffic management, bảo mật mTLS và observability chi tiết giữa các service.
- Các thành phần chính:
  - **Envoy sidecar**: chèn vào từng pod, cung cấp retries, circuit breaker, telemetry.
  - **VirtualService/Gateway**: định tuyến request từ ingress tới service nội bộ; hỗ trợ canary hoặc A/B test orchestrator.
  - **DestinationRule**: khai báo subset version, bật mTLS `ISTIO_MUTUAL`.
- Ví dụ VirtualService canary 10% traffic cho phiên bản mới của payment service:

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: vs-payment
spec:
  hosts:
    - payment.lab.svc.cluster.local
  http:
    - route:
        - destination:
            host: payment.lab.svc.cluster.local
            subset: v2
          weight: 10
        - destination:
            host: payment.lab.svc.cluster.local
            subset: v1
          weight: 90
```

- **Lợi ích chính**:
  - mTLS end-to-end, phù hợp yêu cầu bảo mật khi kết nối tới kênh Tuxedo hoặc API SBV.
  - Rate limiting, retry và outlier detection cấu hình ở level mesh, giảm phụ thuộc code.
  - Tracing và metric qua Prometheus/Grafana/Kiali giúp theo dõi latency từng hop, bổ trợ tính SLA.

### 12.4 Best practice phối hợp HPA & Istio
- HPA scale-out có thể gây spike traffic tới pod mới; dùng Istio `warmupDurationSecs` để gradual load.
- Kết hợp `RequestAuthentication` + `AuthorizationPolicy` bảo vệ API nội bộ khi mesh bật mTLS.
- Thiết lập `PeerAuthentication` ở chế độ `STRICT` để tránh request không mã hóa.
- Giám sát metric Istio (`istio_requests_total`, `istio_request_duration_milliseconds`) và liên kết với dashboard SLA đã mô tả ở Section 11.

> **Lưu ý**: Khi mở rộng thêm tính năng (ví dụ sealed interface cho Investigation states hoặc tích hợp Tuxedo thật), hãy cập nhật tài liệu này để đảm bảo mọi thay đổi lớp/phương thức đều được ghi nhận.
