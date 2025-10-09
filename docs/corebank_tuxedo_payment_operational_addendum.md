# CoreBank Payment - Sequence & Operational Addendum

## 1. End-to-End Payment Sequence (REST -> Legacy Core -> Event Consumers)
```mermaid
sequenceDiagram
    participant CH as Client Channel
    participant GW as API Gateway
    participant OR as Payment Orchestrator
    participant RD as Redis Cache
    participant AD as Tuxedo Adapter
    participant TX as Tuxedo Service (Pro*C)
    participant DB as Oracle DB + Outbox
    participant WP as EVENT_PUB_SRV Worker
    participant KF as Kafka Cluster
    participant NS as Notification Service
    participant SP as Stream Processor

    CH->>GW: REST Payment Request (idempotency key, auth)
    GW->>OR: Forward request + trace context
    OR->>OR: Validate, dedupe, audit log
    OR->>RD: Check cached payment by reference?
    RD-->>OR: Cache miss (continue)
    OR->>AD: Invoke service (within tpbegin XA)
    AD->>TX: ATMI Call (FML32 payload)
    TX->>DB: Ledger update + insert outbox row (same commit)
    TX-->>AD: Response (success/failure code)
    AD-->>OR: Map to domain response
    OR->>RD: Cache payment result & evict status metrics
    OR-->>GW: REST response (ACK/ERR)
    GW-->>CH: Final response

    Note over OR,AD,TX,DB: Transaction boundary closes on tpcommit + Oracle commit

    WP->>DB: Poll outbox rows (status=NEW)
    DB-->>WP: Batch of events
    WP->>KF: Publish events (Avro/Protobuf)
    WP->>DB: Mark outbox row as PUBLISHED (with offset)
    KF-->>NS: Deliver event (push notification)
    KF-->>SP: Deliver event (analytics/landing zone)
    NS->>NS: Execute notification workflow
    SP->>SP: Enrich, persist to landing zone/DWH
```

- **Transaction boundary**
- Gateway -> Orchestrator -> Adapter -> Tuxedo service share một XA transaction (`tpbegin/tpcommit`).
- Outbox row được chèn cùng lúc với cập nhật tài chính nên đảm bảo tính đúng đắn even nếu worker publish bị lỗi.
- Sau khi commit, worker lấy trách nhiệm đảm bảo delivery ít nhất một lần tới Kafka; offset và trạng thái outbox hỗ trợ idempotency.

## 2. Outbox & CDC Operations Guide
- **Outbox schema:** bảng `PAYMENT_EVENT_OUTBOX` chứa `id`, `event_type`, `payload`, `created_at`, `status`, `retry_count`, `last_error`, `kafka_topic`, `kafka_offset`. Index theo `status`, `created_at` để worker quét nhanh.
- **Writer path:** Pro*C service chèn bản ghi với `status='NEW'` cùng transaction ledger. Payload lưu ở dạng JSON hoặc Avro-binary (Base64) để tránh sốc schema.
- **Worker (EVENT_PUB_SRV):**
  - Đọc batch kích cỡ cấu hình (mặc định 100) với `FOR UPDATE SKIP LOCKED` để song song nhiều worker.
  - Publish Kafka sử dụng producer idempotent, enable `acks=all`, `retries` cao, `enable.idempotence=true`.
  - Nếu publish thành công: cập nhật `status='PUBLISHED'`, lưu `kafka_offset` và `published_at`.
  - Nếu lỗi tạm thời: tăng `retry_count`, ghi `last_error`, chuyển `status='RETRY_WAIT'` và áp dụng backoff (exponential, tối đa 5 phút).
  - Nếu lỗi không phục hồi (ví dụ schema không hợp lệ): chuyển `status='FAILED'`, raise alert, yêu cầu can thiệp thủ công.
- **Retry strategy:**
  - Backoff theo luỹ thừa: 1s, 5s, 30s, 2m, 5m; sau số lần cấu hình (mặc định 8) chuyển sang `FAILED`.
  - Worker giữ bảng điều khiển metric `outbox.retry.total`, `outbox.failed.total`, `outbox.lag.seconds` cho Prometheus.
- **Alerting:**
  - Cảnh báo nếu `lag.seconds > 60`, `failed.total > 0`, hoặc `retry.total` tăng đột biến.
  - Alert tích hợp Grafana/Alertmanager gửi đến kênh NOC, đồng thời tạo ticket tự động.
- **Purge policy:**
  - Giữ bản ghi `PUBLISHED` tối thiểu 7 ngày để phục vụ audit và replay.
  - Hàng ngày job ETL chuyển bản ghi cũ (>30 ngày) sang kho WORM archive, sau đó xoá khỏi bảng nghiệp vụ.
  - `FAILED`/`RETRY_WAIT` không bị purge cho tới khi xử lý xong và có log điều tra.
- **CDC integration:**
  - Khi cần stream tới hệ thống khác, Debezium/Oracle GoldenGate đọc outbox với filter `status='PUBLISHED'` làm nguồn.
  - CDC consumer phải tôn trọng `kafka_offset`/`event_id` để tránh phát lại sự kiện đã tiêu thụ.
  - Runbook ghi rõ bước restore offset khi worker hoặc Kafka downtime.

## 3. IAM & Schema Registry Governance
- **IAM platform:** sử dụng Keycloak self-managed (HA cluster) làm Identity Provider cho kênh đối tác, tích hợp SSO với IAM ngân hàng qua SAML 2.0. Keycloak phát hành OAuth2/OIDC token, quản lý client/realm.
  - API Gateway/Kong thực thi OAuth2, mTLS và mapping role -> scope (ví dụ `PAYMENT.READ`, `PAYMENT.WRITE`).
  - Lifecycle: đề xuất thay đổi client hoặc policy phải đi qua change request, kiểm duyệt bởi Security Architect, deploy bằng GitOps (realm export dưới dạng JSON versioned trong repo).
  - Audit: bật admin event logging, lưu vào ELK + WORM.
- **Secrets & certs:** Vault quản lý TLS certificate cho adapter, worker, Kafka client. Rotation tự động mỗi 90 ngày, Orchestrator reload động qua Spring Cloud Vault.
- **Schema registry:** Confluent Schema Registry (self-host trong cùng Kubernetes cluster) cho Avro/Protobuf topic `payments.*`.
  - Versioning chính sách `BACKWARD` để consumer cũ tiếp tục hoạt động.
  - Mọi schema thay đổi được submit qua merge request trong repo `schemas/`, trigger kiểm tra `sr compatibility` trong CI (sử dụng `sr test compatibility`).
  - Deploy schema thông qua GitOps: ArgoCD apply manifest đăng ký schema (sử dụng `SchemaRegistry` CRD hoặc script).
  - Governance board (Tech Lead + Data Architect + QA) duyệt yêu cầu schema mới, ghi lại quyết định trong ticket hệ thống ITSM.
- **Change management:**
  - Quy trình chuẩn gồm `Design Review -> Security Review -> CI validation -> UAT smoke -> Prod deployment window`.
  - Checklist bắt buộc: cập nhật tài liệu (OpenAPI/AsyncAPI, schema changelog), test contract, cập nhật alert nếu có field mới quan trọng.
  - Version tag được đồng bộ với orchestrator release (ví dụ `v2024.07.1`) để trace deployment.

## 4. Redis Cache Application
- **Mục đích:** giảm tải truy vấn legacy bằng cache kết quả truy vấn thanh toán theo `reference` và thống kê số lượng theo trạng thái. TTL mặc định 5 phút để cân bằng giữa độ chính xác và tốc độ.
- **Cấu hình:** `spring-boot-starter-data-redis`, `CacheConfig` sử dụng `RedisCacheManager` với TTL 5 phút. Tham số hoá địa chỉ qua `SPRING_REDIS_HOST/PORT`.
- **Điểm tích hợp:**
  - `PaymentOrchestratorService.orchestrate` ghi đè cache mỗi khi trạng thái thay đổi và xoá số liệu đếm.
  - `findByReference` đọc từ Redis trước khi chạm DB, `countByStatus` cache kết quả thống kê.
- **Vận hành:** bật Redis bằng `scripts/start_redis.sh` (Docker) trong môi trường dev; production yêu cầu Redis cluster (SLA, replication) và giám sát metric (hit ratio, eviction).

## 5. Stream Processor Service
- **Mã nguồn:** `com.corebank.payment.stream` cung cấp ứng dụng Java Kafka Streams (`PaymentEventStreamApplication`) chạy độc lập với orchestrator, chỉ kích hoạt khi `payment.streams.enabled=true`.
- **Luồng xử lý:** subscribe `payments.txn.completed`, enrich payload (thời gian `processedAt`, `amountMinor`), rồi ghi sang topic `payments.analytics.completed` phục vụ Landing Zone/analytics. Lỗi runtime được log NAMNM và stream đóng graceful.
- **Triển khai:**
  - Structure chạy container trên Kubernetes, HPA scale theo lag; trong lab có thể chạy `mvn spring-boot:run -Dspring-boot.run.main-class=com.corebank.payment.stream.PaymentEventStreamApplication -Dspring-boot.run.profiles=streams` sau khi Kafka hoạt động.
  - Profile `streams` sử dụng file `application-streams.yml` (application-id, exactly-once processing).
- **Vận hành:** cần topic `payments.analytics.completed` (script Kafka đã tạo sẵn). Monitor metric Kafka Streams (lag, error) qua Prometheus/grafana; checkpoint/state store dùng Kafka changelog.

## Tài liệu tham chiếu
- `docs/corebank_tuxedo_payment_solution.md`
- `docs/payment-openapi.yaml`
- `docs/payment-asyncapi.yaml`
- `scripts/start_redis.sh`
- `scripts/start_kafka.sh`
- `src/main/resources/application-streams.yml`
