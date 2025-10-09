# CoreBank Payment Technical Optimization Playbook

## 1. Mục đích
Làm rõ các kỹ thuật tối ưu dành riêng cho CoreBank Payment Lab khi vận hành trên Spring Boot + Oracle/Tuxedo hybrid. Tài liệu này bổ sung cho `docs/architecture_document_skeleton.md` và tập trung vào cách tinh chỉnh runtime, persistence, adapter, event streaming, observability và quy trình kiểm thử.

## 2. Nguyên tắc Chung
- Giữ transaction idempotent: mọi flow phải có `payment.reference` duy nhất, retry luôn kiểm tra state hiện hữu trước khi gọi Tuxedo.
- Ưu tiên cấu hình qua biến môi trường (`SPRING_*`, `PAYMENT_*`) để phù hợp các môi trường lab/staging/prod.
- Đối với mỗi tối ưu, xác định KPI đi kèm (latency p95, throughput, error rate) và log outcome.

## 3. Tối ưu Runtime Spring Boot
- JVM: chạy với `JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"` cho môi trường lab; khi prod đặt `-Xms=-Xmx` bằng 60–70% container limit để tránh heap expansion.
- Thread pools: cấu hình `server.tomcat.threads.max=200`, `spring.task.scheduling.pool.size=6` và monitor qua `/actuator/metrics/jvm.threads.*`; tăng khi latency backend cao.
- HikariCP: dùng `spring.datasource.hikari.maximum-pool-size=30` cho PostgreSQL/Oracle; theo dõi `hikaricp.connections` để tránh cạn connection.
- GC log: bật `-Xlog:gc*:file=./logs/gc.log:time` để phân tích pause khi load test.

## 4. Payment Service & Outbox
- Retry adapter: `resilience4j.retry.instances.tuxedo-process.max-attempts=3`, `wait-duration=500ms`; dùng exponential backoff khi tích hợp thật (`randomized-wait-factor=0.5`).
- Outbox polling: `payment.outbox.poll-interval-ms=500` khi bật Kafka để giảm lag; nếu backlog >1k records thì giảm interval xuống 250ms hoặc tăng batch size `payment.outbox.max-batch-size` (thêm property) lên 200.
- Idempotent guard: đảm bảo bảng `payments` có unique constraint `ref_channel` để reject trùng; log với MDC để điều tra.
- Serialization: chuẩn hóa `PaymentEventPayload` ở JSON Schema (`docs/payment-asyncapi.yaml`); validation khi build event để tránh reject downstream.

## 5. Tuxedo Adapter (Mock/Thật)
- Mock latency: cấu hình `PAYMENT_TUXEDO_MOCK_LATENCY_MS` để mô phỏng real; thiết lập default 120–250ms.
- Timeouts: map `resilience4j.timelimiter.instances.tuxedo-process.timeout-duration=1500ms`; khi dùng ATMI thật, đồng bộ với `DMCONFIG` timeout để tránh double abort.
- Connection pool: nếu dùng Jolt, chuẩn bị pool `tuxedo.jolt.pool-size=20`; log pool stats định kỳ.
- Error mapping: maintain bảng mapping `TuxedoError → PaymentStatus`; dùng metrics `payment.adapter.failures` để quan sát.

## 6. Database & Persistence
- Schema PostgreSQL/Oracle: tạo index composite `(status, updated_at)` cho bảng `payments` giúp truy vấn monitoring nhanh.
- Outbox table: thêm index `status, created_at`; archive record sau khi gửi (cron daily) để tránh phình to.
- Batch insert: bật `spring.jpa.properties.hibernate.jdbc.batch_size=50` và `order_inserts=true` để giảm roundtrip khi ingest giao dịch hàng loạt.
- Liquibase/Flyway: chuẩn hóa script trong `src/main/resources/db/migration`; add checksum guard để không drift giữa môi trường.

## 7. Kafka & Event Streaming
- Topic config: `payments.txn.completed|failed` → `partitions=6`, `replication-factor=3`, `retention.ms=604800000` (7 ngày); check with platform team.
- Producer tuning: `acks=all`, `retries=5`, `linger.ms=5`, `batch.size=32768` để đảm bảo durability và hiệu suất.
- Schema evolution: lưu version trong header `event-version`; update AsyncAPI khi thay đổi payload.
- Backpressure: khi Kafka down, outbox backlog phải được giám sát -> `payment.outbox.max-backlog-warning=5000` (tạo metric gauge).

## 8. Observability & Alerting
- Metrics: publish custom meter `payment.tuxedo.latency` và `payment.outbox.backlog` để feed Prometheus; define SLO dashboard (latency p95 < 2s, error rate <1%).
- Logging: chuẩn hóa format JSON với `logging.pattern.level=%5p [%X{paymentRef}]` để dễ truy vết.
- Tracing: tích hợp OpenTelemetry exporter (OTLP) với sampler 10%; propagate context qua adapter để trace end-to-end.
- Alert policy: 
  - Critical: `payment.outbox.backlog > 3000` trong 5 phút.
  - Warning: `payment.tuxedo.latency_p95 > 1500ms` trong 10 phút.
  - Info: `payment.http.5xx_rate > 1%` trong 5 phút.

## 9. Bảo mật & Tuân thủ
- Bật TLS nội bộ: reverse proxy (Ingress) terminate TLS, forward bằng mTLS nếu nội bộ khắt khe.
- Secret: dùng Vault hoặc Kubernetes Secret + KMS; tuyệt đối không commit `.env` chứa password.
- Audit: log đầy đủ `paymentRef`, user/channel, IP, outcome trong `audit_events`; đảm bảo `retention=18 tháng` để đáp ứng yêu cầu NHNN.

## 10. Kiểm thử & Benchmark
- Unit + Contract: mở rộng `src/test/java` với testcase cho adapter timeout và outbox failover.
- Load test: Gatling/JMeter profile `300 TPS`, mix 95% thành công, 5% inject fault; thu thập heap, thread dump sau mỗi run.
- Chaos: viết script tắt Kafka (docker-compose) trong 5 phút để kiểm tra backlog + recovery.
- DR drill: mô phỏng failover DB (PostgreSQL → standby) và Tuxedo (switch domain); ghi lại runbook.

## 11. Chuẩn bị Production
- Container image: set `USER 1000` không chạy root, bật `readOnlyRootFilesystem`.
- Health check: `/actuator/health/liveness` và `/actuator/health/readiness`; delay readiness 10s khi cold start.
- Deployment: HPA target `cpu=60%`, `memory=70%`, min 2 replicas cho payment; monitoring/investigation tùy SLA.
- Backup: snapshot DB 15 phút; event log backup Kafka bằng MirrorMaker hoặc tiered storage.

## 12. Checklist Triển khai
1. Cập nhật cấu hình JVM, Hikari, retry theo môi trường.
2. Review index/db schema và áp dụng migration mới nhất.
3. Xác nhận Kafka topic + producer config khớp AsyncAPI.
4. Thiết lập dashboard + alert; chạy smoke test `scripts/test_*` trước go-live.
5. Lưu trữ evidence benchmark/chaos, trình bày với kiến trúc sư/phê duyệt rủi ro.

---
Mọi cập nhật phát sinh phải đồng bộ với tài liệu kiến trúc chính và ADR tương ứng.
