# CoreBank Payment Lab

Spring Boot 3.x lab environment that wraps a mock Oracle Tuxedo domain and demonstrates the orchestration pattern described in `docs/corebank_tuxedo_payment_solution.md`. The project provides REST APIs, outbox event routing, retry semantics, and pluggable Kafka publishing so squads can experiment without needing the real CoreBank estate.

## Architecture Highlights
- **Payment service** (`com.corebank.payment.*`) handles transaction orchestration, retry, outbox pattern, and exposes REST endpoints for creation/query/metrics.
- **Monitoring service** (`com.corebank.orchestrator.*`) summarizes health snapshots, recent transactions, and alert workflows for ITIL-style operations.
- **Investigation service** (`com.corebank.investigation.*`) manages dispute/tra soát cases, tracks status, and consults the payment service for current transaction state.
- **Tuxedo adapter** (`com.corebank.payment.infrastructure.tuxedo`) contains a mock Oracle Tuxedo client. Replace with the real Jolt/ATMI implementation when ready.
- **Persistence & Outbox** (`com.corebank.payment.infrastructure.persistence|outbox`) store canonical payment state plus outbox events ready for downstream consumption.
- **Event publishing & streaming** (`com.corebank.payment.infrastructure.event`, `com.corebank.payment.stream`) publish lab events to Kafka or perform Kafka Streams processing when enabled.

## Getting Started
1. Requirements: Java 21, Maven 3.9+, (optional) local Kafka if you want to test streaming.
2. Start all lab services: `scripts/start_all.sh` (payment @8080, monitoring @8081, investigation @8082).
   - Use `SKIP_BUILD=1` to skip the upfront compile if already built.
   - Script khởi động với profile `lab-java21`, tự bật virtual threads; override bằng `SPRING_PROFILES_ACTIVE` nếu cần.
   - Each service has its own basic-auth credentials (payment: `orchestrator/changeme`, monitoring: `orchestrator/changeme`, investigation: `investigator/changeme`).
3. The default profile uses in-memory H2 with PostgreSQL compatibility for zero setup. Override `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` to point at PostgreSQL or Oracle tables.
4. Try a payment request:
   ```bash
   curl -u orchestrator:changeme \
     -H "Content-Type: application/json" \
     -d '{
       "reference": "LAB-001",
       "amount": 100000,
       "currency": "VND",
       "channel": "MOBILE",
       "debtorAccount": "1234567890",
       "creditorAccount": "0987654321"
     }' \
     -X POST http://localhost:8080/api/v1/payments
   ```
5. Check status counts via `GET /api/v1/payments/metrics/status-count` (chạy song song trên virtual threads) hoặc inspect emitted outbox rows in the H2 `outbox_events` table.

## Cloud Deployment
- Tham khảo `docs/corebank_payment_lab_cloud.md` để bật Spring Cloud Config, Netflix Eureka và Spring Cloud LoadBalancer khi triển khai trên môi trường đám mây (profile `lab-java21,lab-cloud`).

## Enabling Kafka Streaming
- Set `PAYMENT_EVENTS_KAFKA_ENABLED=true` and `KAFKA_BOOTSTRAP_SERVERS=broker1:9092` before starting the app.
- With the flag turned on, outbox items are forwarded to topics named `payments.txn.completed` or `payments.txn.failed`.

## Observability & Resilience
- Actuator endpoints (`/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`) are exposed without authentication for monitoring integrations.
- Theo dõi `jvm.threads.virtual.count` để quan sát số virtual threads mở khi bật profile `lab-java21`.
- Resilience4j retry (`tuxedo-process`) demonstrates idempotent retry for transient adapter failures.
- Scheduled outbox relay (`payment.outbox.poll-interval-ms`) forwards events every 2s by default; tune via environment variables for load testing.

## Documentation
- `docs/corebank_payment_lab_java21_detailed.md`: kiến trúc chi tiết Java 21 cho ba dịch vụ lab.
- `docs/corebank_payment_lab_cloud.md`: hướng dẫn bật Spring Cloud Config/Eureka khi triển khai đám mây.
- `docs/hybrid_java_proc_tuxedo_architecture.md`: nghiên cứu kiến trúc hybrid Java API ↔ Pro*C backend qua Oracle Tuxedo.
- `docs/cloud_native_legacy_integration.md`: playbook tích hợp cloud-native với hệ thống legacy (Tuxedo, Pro*C).
- `docs/cloud_deployment_guide.md`: quy trình triển khai toàn bộ API lên cloud (build → container → Kubernetes/ECS → observability).

## Tests
- `mvn test` chạy MockMvc flow cho payment (runner CI GitHub dùng Temurin 21, xem `.github/workflows/ci.yml`).
- `scripts/test_payment_flow.sh`, `scripts/test_monitoring_flow.sh`, `scripts/test_investigation_flow.sh` cung cấp smoke test REST cho từng service.

## Next Steps
- Swap `MockTuxedoClient` with a real adapter (C++ ATMI or Jolt) and reuse the same orchestration facade.
- Extend `PaymentEventPayload` with richer audit context (device info, customer, compliance metadata).
- Add message consumer labs (Kafka Streams, Flink) that subscribe to `payments.txn.*` events and land them to object storage or notification services.
- Giám sát workflow CI `.github/workflows/ci.yml` để đảm bảo build/test ổn định trên Temurin 21.
