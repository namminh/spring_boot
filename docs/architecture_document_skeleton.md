# Tài liệu Kiến trúc CoreBank Payment Lab

## 1. Executive Summary
- **Purpose:** Cung cấp cái nhìn end-to-end về kiến trúc lab CoreBank Payment, giúp đội dự án align khi mở rộng ra môi trường thực hoặc tích hợp với lõi Tuxedo hiện hữu.
- **Scope:** Bao gồm payment orchestrator (Spring Boot), monitoring & investigation service, mock Tuxedo adapter, kênh sự kiện Kafka, cơ chế outbox, hạ tầng triển khai kèm CI/CD nội bộ và lộ trình nâng cấp Java 21.
- **Key Outcomes:** Chuẩn hóa API và quy trình orchestration, xác nhận mô hình outbox → Kafka, khai thác Virtual Threads + structured concurrency để tăng throughput xử lý, áp dụng pattern matching (switch, record patterns) làm sạch code domain, tạo nền tảng thí nghiệm cho resiliency, observability và DevOps pipeline trước khi go-live.

## 2. Context
### 2.1 Business Drivers
- Tái hiện nghiệp vụ thanh toán liên ngân hàng dựa trên lõi Oracle Tuxedo nhưng giảm phụ thuộc vào môi trường thật trong giai đoạn R&D.
- Chuẩn bị cho hành trình hiện đại hóa CoreBank bằng việc bóc tách orchestration layer khỏi logic Pro*C kế thừa.
- Nâng cao năng lực báo cáo realtime, phân tích sự cố và tích hợp đa kênh qua REST/Events.
- Cập nhật nền tảng ngôn ngữ lên Java 21 (LTS) nhằm tận dụng Virtual Threads, pattern matching để đơn giản hóa xử lý nghiệp vụ và tăng khả năng mở rộng với chi phí thấp.

### 2.2 Stakeholders
- Product Owner: CoreBank Payment Tribe.
- Solution/Domain Architect: nhóm Hybrid Architect Lab.
- Đội phát triển: squad Payment, Monitoring, Investigation.
- Vận hành & Bảo trì: IT Operation Center, DBA Oracle/Tuxedo admin.
- Tuân thủ & Audit: Risk/Compliance, Internal Audit.

### 2.3 Success Metrics
- >95% giao dịch lab hoàn tất dưới 2s ở profile mặc định (H2 + mock Tuxedo).
- Smoke test 3 dịch vụ chính (`scripts/test_*`) xanh liên tục trong CI pipeline.
- Tỷ lệ lỗi 5xx của API pembayaran < 1% trong thử nghiệm tải chuẩn.

## 3. Current State Overview
### 3.1 System Landscape
- 3 dịch vụ Spring Boot độc lập (`payment`, `monitoring`, `investigation`) chia sẻ cùng codebase.
- Adapter Tuxedo mock mô phỏng cuộc gọi ATMI/Pro*C, lưu vết vào outbox.
- Kafka publisher tùy chọn, mặc định tắt; khi bật sẽ push topic `payments.txn.*`.
- H2 in-memory kiêm persistence và outbox; hỗ trợ chuyển sang PostgreSQL/Oracle qua biến môi trường.

### 3.2 Pain Points
- Adapter mock chưa phản ánh đầy đủ latency/timeout thật của Tuxedo.
- Chưa có sơ đồ hạ tầng chính thức (Kubernetes, mạng, bảo mật) trong repo.
- Thiếu tự động hóa triển khai monitoring stack (Prometheus/Grafana) và pipeline IaC hoàn chỉnh.

## 4. Target Architecture Overview
### 4.1 Solution Narrative
- Giữ nguyên lõi Tuxedo/Oracle, bổ sung orchestration layer xử lý idempotency, retry, outbox.
- Virtual Threads (JEP 444) bao bọc từng request/payment workflow trên executor `newVirtualThreadPerTaskExecutor()`, hỗ trợ structured concurrency (JEP 453) cho các cuộc gọi Tuxedo + integration downstream, giảm chi phí context switching.
- Dữ liệu sự kiện được phát lên Kafka để downstream (Fraud, Data Platform) tiêu thụ.
- Monitoring/Investigation dựa trên REST và metrics để theo dõi tình trạng giao dịch.

### 4.2 Architecture Diagram
- Sử dụng ASCII chart trong `docs/corebank_tuxedo_payment_solution.md` và bản draw.io tại SharePoint đội dự án (chưa commit). Placeholder: cập nhật link khi xuất bản.

### 4.3 Capability Breakdown
- Payment Orchestration: tiếp nhận, validate, điều phối giao dịch.
- Monitoring: tổng hợp health snapshot, metrics, cảnh báo ITIL.
- Investigation: quản lý tra soát, đối soát với Payment service.
- Event Streaming: publish/stream ra Kafka/Streams để mở rộng phân tích.

## 5. Detailed Views
### 5.1 Component View
- `com.corebank.payment.*`: API REST, workflow điều phối, resilience4j retry, outbox relay; mỗi request chạy trên Virtual Thread để tối ưu I/O bound call.
- `com.corebank.payment.infrastructure.tuxedo`: mock client, điểm thay thế bằng Jolt/ATMI thật; đóng gói bằng record để mô tả payload bất biến, dùng pattern matching switch phân nhánh trạng thái response.
- `com.corebank.orchestrator.*`: aggregator cho monitoring dashboards; áp dụng record patterns để destructure thông điệp event.
- `com.corebank.investigation.*`: nghiệp vụ tra soát, phụ thuộc payment client, sử dụng pattern matching for switch khi phân loại case state.

### 5.2 Integration View
- REST JSON qua HTTP Basic Auth giữa channels ↔ orchestrator (port 8080/8081/8082); Virtual Threads xử lý đồng thời hàng nghìn request mà không phải mở rộng thread pool truyền thống.
- Event contract `docs/payment-asyncapi.yaml` mô tả topic `payments.txn.completed|failed`; serializer/deserializer dùng record làm DTO bất biến.
- Outbox table đồng bộ qua relay scheduler, tùy chọn enable Kafka.
- Adapter giao tiếp với Tuxedo mock qua interface Java; khi production sẽ dùng Jolt/ATMI + DMCONFIG; structured concurrency song song hóa multi-step call (validate, reserve, confirm) và cancel toàn bộ khi gặp lỗi.

### 5.3 Data View
- Payment persistence: `payments`, `payment_attempts`, `outbox_events` (H2/Postgres schema) ánh xạ sang `record` trong layer domain để đảm bảo immutability.
- Monitoring sử dụng view tổng hợp, caching tạm thời (Redis tuỳ chọn); sử dụng record pattern để bóc tách metric snapshot.
- Investigation lưu case + audit trail gắn với payment reference; áp dụng sealed interface + pattern matching switch phân loại trạng thái (open/escalated/closed).
- Archive & analytics: hướng tới landing zone S3/MinIO (được mô tả trong tài liệu giải pháp chính).

### 5.4 Deployment View
- Local/dev: Maven + Spring Boot, cấu hình `.env` hoặc biến môi trường; yêu cầu JDK 21.
- Scripts `scripts/start_all.sh` orchestrate 3 service với profile `lab`; flag `spring.threads.virtual.enabled=true` bật Virtual Threads khi chạy profile `lab-java21`.
- Production mục tiêu: Kubernetes (payment stack) + VM/Bare Metal (Tuxedo), Terraform quản lý network, GitOps (ArgoCD) deploy image container; container base image nâng lên `eclipse-temurin:21-jre`.
- CI GitHub Actions (`.github/workflows/ci.yml`) dùng Temurin 21 và `mvn -B -ntp test`; runner nội bộ cần đồng bộ JDK.

### 5.5 Operational View
- Actuator (`/actuator/health|metrics|prometheus`) mở cho monitoring, bổ sung metric `jvm.threads.virtual.count` và tracking StructuredTaskScope khi bật Java 21.
- Basic auth credentials: payment & monitoring `orchestrator/changeme`, investigation `investigator/changeme`.
- Retry/resilience policy cấu hình qua properties (`resilience4j.retry.instances.tuxedo-process`).
- Runbook dự kiến: `docs/corebank_tuxedo_payment_operational_addendum.md` (chưa hoàn chỉnh); bổ sung checklist kiểm tra Virtual Threads pinning (synchronized, JDBC driver) trong ca sự cố.

## 6. Non-Functional Requirements
- **Performance & Scalability:** 300 TPS mục tiêu lab; autoscale HPA trên Kubernetes; partition Kafka ≥3; Virtual Threads giảm nhu cầu mở rộng pod khi tải I/O tăng gấp 5 lần.
- **Availability & Reliability:** HA theo mô hình active-active payment pods, Tuxedo domain failover qua `MAXGEN` + Oracle RAC; StructuredTaskScope hỗ trợ cancel toàn cụm call khi carrier thread gặp sự cố.
- **Security & Compliance:** TLS nội bộ, Vault quản lý secret, audit trail đáp ứng PCI DSS/NHNN; kiểm soát quyền truy cập khi enable preview features (nếu dùng).
- **Resilience & Recovery:** Outbox đảm bảo eventual delivery; backup DB theo snapshot; DR site dùng Dataguard + Tuxedo failover; fallback sang executor cố định nếu phát hiện pinning kéo dài >5s.
- **Maintainability & Supportability:** Codebase Spring Boot chuẩn, pipeline CI với unit + contract test, ADR lưu ở `docs/adr/*` (đang lập); pattern matching giảm số lượng class `*Handler`, record giúp rút gọn DTO và giảm lỗi mapping.

## 7. Technology Choices
- Spring Boot 3.2, Java 21 LTS (Virtual Threads GA), Spring Security, Spring Data JPA.
- Structured concurrency API (`StructuredTaskScope`) và pattern matching (`switch` + record patterns) áp dụng trong layer orchestration.
- Kafka + Kafka Streams, Resilience4j.
- H2/PostgreSQL/Oracle cho persistence; Redis (optional cache).
- Terraform, Ansible, ArgoCD/GitLab CI cho IaC & CI/CD.
- Oracle Tuxedo (legacy), Pro*C services, Oracle DB.

## 8. Risks & Mitigations
- Thiếu môi trường Tuxedo thật → duy trì mock + xây dựng test contract với vendor; lập kế hoạch tích hợp sớm.
- Phức tạp triển khai hybrid Kubernetes + VM → chuẩn hóa network blueprint, áp dụng service mesh cho layer API.
- Khả năng backlog outbox khi Kafka down → giám sát backlog, cho phép replay thủ công, bật backpressure HTTP 202.
- Technical debt do code lab → thiết lập deadline hardening, review ADR, refactor khi chuyển production.
- Thư viện bên thứ ba chưa tương thích Virtual Threads (ví dụ JDBC driver cũ) → theo dõi danh sách driver certified, fallback sang thread pool truyền thống cho endpoint ảnh hưởng.

## 9. Assumptions & Decisions
- Lab mặc định chạy offline (no Kafka) trừ khi đặt `PAYMENT_EVENTS_KAFKA_ENABLED=true`.
- Chưa triển khai thực sự Redis; code có sẵn integration nếu cần cache.
- Tuxedo adapter hiện thời chỉ log + giả lập response, chưa có giao thức ATMI thực.
- Mọi service chia sẻ cơ sở mã nguồn chung để giảm overhead build; khi tách domain sẽ modular hóa.
- Java 21 làm baseline; nếu phải chạy trên Java 17, profile `compat` vô hiệu Virtual Threads và fallback về thread pool cố định.

## 10. Open Questions & Follow-Ups
- Xác nhận timeline tích hợp với Oracle Tuxedo production domain.
- Quyết định lựa chọn công cụ observability: Prometheus/Grafana chuẩn hóa hay dùng Elastic stack sẵn có.
- Hoàn thiện runbook vận hành & DR drill (tài liệu addendum).
- Kiểm chứng performance trên PostgreSQL/Oracle thật (chưa có kết quả benchmark chính thức).

## 11. Appendix
- **Glossary:**
  - Tuxedo: Middleware transaction Oracle.
  - Outbox Pattern: Cơ chế đảm bảo transactional event publishing.
  - RTO/RPO: Recovery Time/Point Objective.
  - Virtual Thread: Lightweight thread do JVM quản lý, mapping động lên carrier thread.
- **References:** `docs/corebank_tuxedo_payment_solution.md`, `docs/corebank_tuxedo_payment_operational_addendum.md`, `docs/payment-openapi.yaml`, `docs/payment-asyncapi.yaml`, `docs/ci_cd_process.md`, JEP 444/453/441.
- **Changelog:**
  - 2024-04-xx: Khởi tạo skeleton (Codex CLI).
  - 2024-05-??: Cập nhật nội dung theo trạng thái lab hiện tại.
  - 2024-06-??: Nâng lộ trình Java 21, bổ sung Virtual Threads, pattern matching.
