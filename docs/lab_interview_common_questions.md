# CoreBank Payment Lab – Common Interview Topics

## 1. Kiến trúc Tổng thể
- **Lab mô phỏng điều gì?** CoreBank Payment Lab tái hiện lớp orchestration thanh toán bao quanh Oracle Tuxedo. Ứng dụng gồm 3 dịch vụ Spring Boot (payment, monitoring, investigation), mock Tuxedo adapter, outbox → Kafka, hạ tầng script.
- **Tại sao tách orchestration khỏi Tuxedo?** Giảm phụ thuộc Pro*C, mở đường modern hóa, cho phép triển khai microservices/DevOps mà vẫn dùng lõi giao dịch legacy.

## 2. Outbox Pattern & Event Streaming
- **Lý do dùng outbox?** Đảm bảo transactional event publishing: ghi payment + event cùng transaction, relay sang Kafka sau đó → tránh mất/thừa sự kiện.
- **Quy trình relay?** Scheduler đọc `outbox_events` (status `PENDING`), publish qua `PaymentEventPublisher`, đánh dấu `SENT`, có retry khi failure.
- **Xử lý Kafka down?** Outbox backlog tăng; monitor metric `payment.outbox.backlog`, bật alert, cho phép replay thủ công và backpressure HTTP 202.

## 3. Tuxedo Adapter Mock → Thật
- **Mock hoạt động ra sao?** Trả lời giả lập dựa trên latency config, log call, giúp test retry/idempotency.
- **Khi chuyển sang Tuxedo thật cần gì?** Triển khai `TuxedoClient` mới (Jolt/ATMI), cấu hình pool, timeout đồng bộ DMCONFIG, map lỗi, thêm test contract.
- **Đảm bảo SLA <2s thế nào?** Tuning JVM, Hikari, index DB, caching, monitoring latency, circuit breaker tránh chờ quá lâu.

## 4. Resilience & Retry
- **Resilience4j dùng chỗ nào?** Retry `tuxedo-process`, time limiter/circuit breaker bảo vệ adapter; bulkhead tránh nghẽn.
- **Idempotency đảm bảo bằng gì?** Unique reference/channel, kiểm tra state trước khi retry, log audit.
- **Fallback khi dependency lỗi?** Trả HTTP 202 hoặc trạng thái pending, ghi outbox để xử lý sau.

## 5. Observability & Alerting
- **Metrics chính?** Actuator + custom: latency adapter, outbox backlog, HTTP status distribution, Tuxedo failure count.
- **Logging chuẩn?** JSON, MDC chứa `paymentRef`, correlation ID để trace cross-service.
- **Tracing?** OpenTelemetry exporter, propagate context qua adapter để theo dõi end-to-end.

## 6. Database & Giao dịch
- **Schema chính?** `payments`, `payment_attempts`, `outbox_events`, `investigation_cases`, `alerts`.
- **Index nào quan trọng?** `(reference, channel)` idempotency, `(status, updated_at)` cho dashboard, `(status, created_at)` outbox.
- **Batch & transaction scope?** Hibernate batching 50, transaction nhỏ, read-only cho GET.

## 7. Deployment & DevOps
- **Triển khai lab?** `scripts/start_all.sh`, H2 mặc định, config qua biến môi trường.
- **Mục tiêu production?** Kubernetes cho microservice, VM/bare metal cho Tuxedo, GitOps (ArgoCD), Terraform network.
- **Health check?** `/actuator/health/liveness|readiness`, basic auth cho API, TLS nội bộ.

## 8. Bảo mật & Tuân thủ
- **Authentication?** Basic auth (lab), có thể nâng cấp OAuth2; secrets quản trị qua Vault/Secrets.
- **Audit trail?** Ghi user/channel/IP/outcome, retention ≥18 tháng đáp ứng PCI/NHNN.
- **Data privacy?** Mask thông tin nhạy cảm trong log, role-based access với investigation service.

## 9. Testing Strategy
- **Unit/Integration?** MockMvc cho REST, test retry/outbox, Testcontainers nếu kết nối Postgres/Oracle.
- **Smoke scripts?** `scripts/test_payment_flow.sh` etc. used in CI.
- **Performance test?** Gatling/JMeter 300 TPS, capture GC/ASH, validate p95 <2s.
- **Chaos/DR?** Tắt Kafka để kiểm tra backlog, mô phỏng DB failover.

## 10. Leadership & Collaboration
- **Vai trò kiến trúc sư?** Định nghĩa blueprint, mentoring, dẫn dắt RFC/ADR, phối hợp PO/ops.
- **Giao tiếp với bên liên quan?** Giải thích trade-off tech vs SLA/compliance, chuẩn bị kế hoạch migration sang production Tuxedo domain.
- **Rủi ro chính?** Thiếu môi trường Tuxedo thật, hybrid infra phức tạp, backlog outbox; mitigations: mock nâng cao, blueprint network, backpressure & replay.

Sử dụng tài liệu này để luyện trả lời theo cấu trúc ngắn gọn (problem → solution → benefit) và gắn với số liệu thực tế (latency, TPS, giảm MTTR...).
