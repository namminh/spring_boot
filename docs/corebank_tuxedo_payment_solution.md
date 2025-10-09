# Bộ giải pháp CoreBank Payment sử dụng Oracle Tuxedo + C/Pro*C

## 1. Mục tiêu & phạm vi
- Tận dụng nguyên vẹn tuyến xử lý hiện hữu `Tuxedo Adapter → Oracle Tuxedo Domain (Pro*C Services) → Oracle DB` như lõi thanh toán kế thừa, đảm bảo không gián đoạn nghiệp vụ và vẫn khai thác giấy phép Oracle Tuxedo.
- Bao bọc lõi cũ bằng kiến trúc hiện đại: Spring Boot orchestrator, nền tảng sự kiện, kho dữ liệu để mở rộng năng lực sản phẩm, báo cáo realtime và khả năng tích hợp với hệ sinh thái số.
- Chuẩn hóa quy trình DevOps/CI-CD, quan trắc, bảo mật nhằm đáp ứng yêu cầu vận hành chuyên nghiệp của tổ chức tài chính.
- Cung cấp roadmap triển khai, cơ chế giám sát và hướng dẫn kỹ thuật cho đội phát triển theo đúng định hướng giải pháp trong `yeu_cau.txt`.

## 2. Kiến trúc tổng thể mục tiêu
Kiến trúc phân tầng, trong đó lõi thanh toán kế thừa (Tuxedo + Pro*C + Oracle DB) giữ nguyên, còn các lớp bao quanh được thiết kế mới בהתאם định hướng microservice, cloud-native:

1. **Channel Layer:** Web/Mobile App, đối tác Open API, Back Office, kênh nội bộ.
2. **API Gateway & Orchestrator (Spring Boot):** Microservice Java điều phối nghiệp vụ, idempotency, audit, expose REST/gRPC.
3. **Adapter & Service Mesh Layer:** Tuxedo adapter (Java Jolt/C++ ATMI), adapter phụ trợ (Kafka, Outbox) chạy container/Kubernetes, tuân thủ service mesh.
4. **Legacy Core (kế thừa):** Oracle Tuxedo domain với dịch vụ Pro*C và Oracle RAC/Exadata làm sổ cái.
5. **Event Streaming & Data Platform:** Kafka, Stream Processor (Flink/Kafka Streams), Landing Zone, Data Warehouse/Mart.
6. **Shared Services & Observability:** Notification service, monitoring stack (Prometheus-Grafana, ELK/Otel), Vault/KMS, CI/CD pipelines.
7. **Enterprise Integration:** IAM, ESB/Lombok (nếu có), batch settlement, báo cáo nội bộ.

Sơ đồ tác vụ chính:
```
>              +---------+        +----------------+        +---------------------+
  Client ----> |  API    | -----> | Payment         | -----> | Tuxedo Adapter       |
  Channels     | Gateway |        | Orchestrator    |        |  (Bridge)            |
               +----+----+        +--------+--------+        +----------+----------+
                    |                        |                          |
                    |                        |                          v
                    |                        |            +--------------------------+
                    |                        |            | Oracle Tuxedo Domain     |
                    |                        |            | (Pro*C Services)         |
                    |                        |            +-----------+--------------+
                    |                        |                        |
                    |                        |                        v
                    |                        |            +--------------------------+
                    |                        |            | Oracle Database (Ledger, |
                    |                        |            | Audit, Outbox, CDC tables)|
                    |                        |            +--------------------------+
                    |                        |                       /|\
                    |                        |                        |
                    |                        v                        |
                    |             +----------------------+             |
                    |             | Event Publisher      |             |
                    |             +----------+-----------+             |
                    |                        |                         |
                    |                        v                         |
                    |          +--------------------------+            |
                    |          | Kafka Cluster (replicated|            |
                    |          | topic logs on broker disk)|           |
                    |          +-----------+--------------+            |
                    |                      |                           |
                    |                      v                           |
                    |      +-----------------------------+             |
                    |      | Stream Processor (Flink/KS) |             |
                    |      +--------------+--------------+             |
                    |                     v                            |
                    |        +---------------------------+             |
                    |        | Landing Zone (Object      |             |
                    |        | Storage: S3/MinIO/HDFS)   |             |
                    |        +-------------+-------------+             |
                    |                      |                           |
                    |                      v                           |
                    |        +---------------------------+             |
                    |        | Data Warehouse / BI Marts |<------------+
                    |        | (Snowflake/ADW/BigQuery)  |
                    |        +-------------+-------------+
                    |                      ^
                    |                      |
                    |      +---------------+----------------+
                    |      | Ledger CDC (GoldenGate/        |
                    |      | Debezium) → Kafka Connectors   |
                    |      +---------------+----------------+
                    |                      |
                    |                      v
                    |        +---------------------------+
                    |        | Notification Service      |
                    |        | (deliveries + audit store |
                    |        |  in Postgres/Redis/Kafka)  |
                    |        +-------------+-------------+
                    |                      |
                    v                      v
       +-------------------+     +--------------------------+
       | Metrics Store     |     | Logs/Traces Store        |
       | (Prometheus + TSDB)|    | (ELK/OpenSearch, Jaeger) |
       +-------------------+     +--------------------------+

  Lưu trữ dữ liệu theo từng chặng

  - API Gateway: stateless; cấu hình và log được đẩy về Observability stack (Prometheus/ELK), không lưu giao dịch.
  - Payment Orchestrator: dùng Redis/Caffeine cho idempotency & metadata cache; audit tạm thời vào Oracle Outbox rồi
  flush sang Kafka/DWH.
  - Tuxedo Adapter: giữ cấu hình, metric trong Prometheus/exporter; không lưu giao dịch lâu dài.
  - Oracle Tuxedo Domain: xử lý nghiệp vụ và ghi ledger, journal, queue nội bộ trong Oracle Database.
  - Oracle DB: kho chính cho ledger, tài khoản, outbox event, bảng CDC; tuân thủ backup/HA.
  - Kafka: lưu sự kiện ở log partition trên đĩa từng broker; replication đảm bảo an toàn dữ liệu trung gian.
  - Stream Processor: trạng thái phiên (state store) trên RocksDB/HDFS, checkpoint trong object storage, sau đó đẩy dữ
  liệu đã enrich.
  - Landing Zone: object storage giữ raw event (Parquet/Avro) làm nguồn cho DWH.
  - DWH/BI Mart: lưu dữ liệu mô hình Kimball/Data Vault phục vụ báo cáo, dashboard.
  - Ledger CDC: sử dụng GoldenGate/Debezium ghi offset trong Kafka Connect + metadata store (internal DB).
  - Notification Service: lưu trạng thái gửi (PostgreSQL/Redis), DLQ trên Kafka, audit vào DWH.
  - Observability Pipelines: metrics ở Prometheus + TSDB; log/traces trong Elastic/OpenSearch + Jaeger/Tempo, phục vụ
  theo dõi toàn tuyến.
```
> Oracle Outbox ở đây là bảng (hoặc tập bảng) nằm trong chính Oracle Database của core thanh toán, dùng để áp dụng
  Outbox Pattern:

  - corebank_tuxedo_payment_solution.md:128 mô tả worker EVENT_PUB_SRV đọc Outbox và đẩy sự kiện lên Kafka. Khi Pro*C xử
  lý giao dịch, nó ghi ledger/audit và cùng lúc chèn bản ghi sự kiện vào bảng Outbox trong cùng transaction Oracle. Điều
  này bảo đảm nếu giao dịch commit thành công thì sự kiện chắc chắn tồn tại; nếu rollback thì cả hai cùng bị hủy, tránh
  lệch giữa DB và message broker.
  - corebank_tuxedo_payment_solution.md:92 cho biết Orchestrator dùng Outbox để lưu audit/tạm thời trước khi chuyển dữ
  liệu sang Kafka/DWH, nên Outbox chính là nơi “giữ hộ” sự kiện trong Oracle DB.
  - corebank_tuxedo_payment_solution.md:172 nhấn mạnh việc giám sát backlog Outbox: depth bảng Outbox cần theo dõi,
  autoscale worker, và nếu backlog lớn Orchestrator trả 202 PROCESSING để giảm tải.
  - Trong blueprint tổng thể (corebank_project_blueprint.md:56, corebank_project_blueprint.md:116), Outbox còn làm cơ
  chế fallback và rollback: khi core không phản hồi, yêu cầu được đưa vào Outbox/queue tạm; nếu lỗi sau khi core đã ghi
  sổ, Outbox giữ sự kiện để vẫn phát ra đẩy khôi phục đồng bộ.

  Tóm lại, Oracle Outbox là bảng sự kiện nằm trong Oracle Database của hệ thống, được ghi cùng giao dịch nghiệp vụ để
  đảm bảo tính đúng đắn, sau đó worker chuyên dụng đọc bảng này và publish lên Kafka/DWH hay các hàng đợi tiếp theo.
> Prometheus là hệ thống giám sát mã nguồn mở do SoundCloud phát triển, được Cloud Native Computing Foundation (CNCF)
  bảo trợ. Điểm chính:

  - Thu thập số liệu: theo mô hình pull—Prometheus định kỳ scrape endpoint /metrics (thường dạng HTTP) từ service để lấy
  time-series.
  - Lưu trữ: lưu dữ liệu theo mô hình key-value + timestamp trong TSDB nội bộ; hỗ trợ retention, downsampling, remote
  write.
  - Ngôn ngữ truy vấn: PromQL cho phép tính toán biểu đồ, alerting rule (ví dụ P95 latency, error ratio).
  - Alerting integration: Alertmanager xử lý rule firing, gửi thông báo tới Slack, email, PagerDuty…
  - Ecosystem: exporter sẵn có cho Linux, JVM, Oracle, Kafka…, tích hợp tốt với Grafana để hiển thị dashboard.

  Trong kiến trúc hiện tại, Prometheus thu thập metric từ API Gateway, Orchestrator, Tuxedo Adapter, Kafka… và cung cấp
  nền tảng đo SLA, phát hiện bất thường vận hành 

  - Orchestrator/worker publish sự kiện giao dịch vào các topic Kafka.
  - Stream Processor (Flink/Kafka Streams) subscribe topic đó, xử lý/enrich rồi ghi ra Landing Zone hoặc topic trung
  gian. Việc đẩy file Parquet/Avro xuống Landing Zone thường do connector (Kafka Connect/Sink) hoặc chính job stream
  processor thực thi.
  - Notification Service là microservice khác cũng subscribe cùng topic, dùng dữ liệu sự kiện để gửi SMS/Email/Push. Nó
  chạy độc lập, chỉ cần Kafka đảm bảo sự kiện đến đúng thứ tự và có offset để consumer đọc.
  - Các consumer hoạt động song song, không phụ thuộc nhau, vì vậy cùng một sự kiện khi được publish lên Kafka có thể
  đồng thời được Stream Processor đọc để đẩy Landing Zone và Notification Service đọc để gửi thông báo.
## 3. Thành phần chi tiết (ngoài lõi kế thừa)
### 3.1 API Gateway & Payment Orchestrator
- Spring Cloud Gateway/Kong làm reverse proxy, WAF, rate limiting, OAuth2/OIDC, JWT, mTLS nội bộ.
- Orchestrator Spring Boot 3.x (Java 17) triển khai theo mô hình hexagonal, chia package `api`, `domain`, `application`, `infrastructure`.
- Tích hợp rule engine (Drools/Decision Model) để đáp ứng nhu cầu phân tích, thiết kế nghiệp vụ thay đổi nhanh.
- Áp dụng Resilience4j (bulkhead, circuit breaker, retry), support trace context (OpenTelemetry) và structured logging JSON.
- Hiển thị contract OpenAPI/AsyncAPI cho kênh bên ngoài, hỗ trợ SDK generator.

### 3.2 Lớp tích hợp Oracle Tuxedo (Bridge mới)
- **Adapter dịch vụ:**
  - Java Jolt client cho các luồng online; C++ ATMI adapter khi cần hiệu năng cao cho batch.
  - Mapping DTO ↔ FML32/VIEW32, version control bằng schema registry nội bộ, unit test bằng mock Tuxedo.
  - Sử dụng connection pooling, heartbeat với `tuxedo tlisten`, tự động reconnect.
- **Giao dịch & bảo mật:**
  - Hỗ trợ `tpbegin/tpcommit` cho các nghiệp vụ XA; fallback `TPNOTRAN` cho truy vấn.
  - mTLS client cert từ Vault; ACL mapping theo user/service account.
- **Monitoring:** Export metric từ adapter (latency, tp return code) ra Prometheus, log error map sang kênh ELK.

### 3.3 Event Streaming & Data Platform
- Kafka cluster 3-5 node, topic chuẩn hóa `payments.txn.completed`, `payments.txn.failed`, `notifications.dispatch`, version schema Avro/Protobuf.
- Outbox table tại Tuxedo domain được worker `EVENT_PUB_SRV` đọc và publish để bảo toàn tính đúng đắn.
- Stream Processor (Kafka Streams/Flink) enrich, làm sạch dữ liệu, đẩy Landing Zone (S3/MinIO) rồi ETL sang DWH (Snowflake/BigQuery/Redshift, hoặc Oracle ADW/on-premise tùy điều kiện).
- Data Mart BI đáp ứng báo cáo quản trị, compliance; hỗ trợ self-service analytics.

### 3.4 Notification & Integration Services
- Microservice thông báo (Java/Spring hoặc Node.js) tiêu thụ Kafka, gửi SMS/Email/Push theo SLA <30s, hỗ trợ workflow song phương (sender/receiver).
- ESB/Integration hub (nếu tổ chức đã có) truy xuất qua REST/gRPC adapter; batch settlement qua `/Q` legacy hoặc file SFTP.
- Cung cấp API nội bộ cho Back Office thực hiện tra soát, reverse transaction, theo dõi status.

### 3.5 Observability & Governance
- **Monitoring:** Prometheus (orchestrator, adapter), custom exporter đọc `tmadmin status`, log aggregator (Filebeat/Fluent-bit → ELK/Otel), Grafana dashboard.
- **Alert:** Latency core > SLA, backlog `/Q`, tỷ lệ lỗi `TPESVCERR`, dung lượng Outbox.
- **Tracing:** OpenTelemetry kết hợp Jaeger/Tempo; correlation-id xuyên suốt từ gateway → Tuxedo → event.
- **Audit:** Centralized audit trail (WORM storage) thỏa PCI DSS/NHNN.

### 3.6 DevOps & CI/CD
- Pipeline hợp nhất: GitLab/Jenkins → build orchestrator (unit, contract test, SAST SonarQube), build adapter (C++/Java), precompile Pro*C (nếu thay đổi) rồi package domain artifact (`ubbconfig`, script).
- ArgoCD (GitOps) deploy container workloads, ansible/terraform provisioning VM cho Tuxedo.
- Automated regression environment (Kubernetes pod + Tuxedo container base image) để test chức năng/hiệu năng.
- Infrastructure as Code: Terraform quản lý network, Kafka, Vault; Ansible cấu hình Tuxedo domain, Oracle client.

## 4. Legacy Core (kế thừa, mô tả ngắn)
- Tuyến `Tuxedo Adapter → Oracle Tuxedo Domain (Pro*C Services) → Oracle DB` giữ nguyên logic, schema, quy trình vận hành.
- Domain `PAYMENT_DOM` gồm các server group `PAY_AUTH_SRV`, `PAY_POST_SRV`, `PAY_STL_SRV`, `EVENT_PUB_SRV`; load balancing, failover theo `MAXGEN`, `LDBAL`.
- Oracle RAC/Exadata duy trì ledger double-entry, compliance, encryption TDE.
- Các thay đổi chỉ tập trung vào bao bọc, giám sát, không sửa logic Pro*C trừ khi có sprint refactor riêng.

## 5. Mô hình triển khai & hạ tầng
- **Môi trường:** Dev (mock Tuxedo), SIT (domain thu gọn), UAT, Pre-prod, Production active-active.
- **Container & VM:** Orchestrator/event/notification chạy trên Kubernetes (HPA), service mesh (Istio/Linkerd) cung cấp mTLS, observability. Tuxedo domain chạy VM/bare metal được quản lý bằng Ansible.
- **Networking:** Phân tầng DMZ (gateway), app tier (orchestrator, adapter), core tier (Tuxedo, Oracle); firewall, zero trust với Vault-issued certificates.
- **DR & Backup:** Dataguard/RMAN, `tmloadcf -t` backup cấu hình, script failover domain, Kafka MirrorMaker 2.

## 6. Lộ trình triển khai đề xuất
1. **Sprint 1-2:** Nghiên cứu yêu cầu, chuẩn hóa API, dựng orchestrator skeleton, tạo mock adapter ATMI (in-memory), viết test contract.
2. **Sprint 3-4:** Xây adapter thật (Jolt/C++), kết nối domain kế thừa, hoàn thiện pipeline build C/Pro*C, đưa monitoring cơ bản.
3. **Sprint 5:** Triển khai Kafka, Outbox → Event Processor, Landing Zone & ETL tối thiểu, dashboard SLA.
4. **Sprint 6:** Hiệu năng (Gatling/K6), tuning Tuxedo `/Q`, thiết lập cảnh báo, hardened security (ACL, Vault, TLS), viết runbook.
5. **Sprint 7+:** Mở rộng analytics (fraud detection, revenue assurance), triển khai DR test, tối ưu hoá container platform.

## 7. Rủi ro & biện pháp
- **Năng lực vận hành Tuxedo hạn chế:** Đào tạo, runbook chi tiết, pair với chuyên gia, script healthcheck.
- **Thắt cổ chai legacy:** Theo dõi metric, scale horizontal server group, caching metadata tại orchestrator/Redis, phân tuyến giao dịch nóng/lạnh.
- **Đồng bộ schema DTO/FML32:** Qua versioning, contract test, review tự động pipeline.
- **Backlog `/Q`/Outbox:** Giám sát depth, autoscale worker, áp dụng backpressure (HTTP 202 `PROCESSING`).
- **Bảo mật dữ liệu:** Tokenization, masking, audit trail, penetration test định kỳ.
- **Phụ thuộc vendor:** Ghi chép quy trình IaC, chuẩn hóa packaging (RPM/Container), duy trì license management.

## 8. Gắn kết yêu cầu năng lực (theo `yeu_cau.txt`)
- **Phân tích yêu cầu & thiết kế giải pháp:** Orchestrator và data platform hỗ trợ phân tích, rule engine linh hoạt, đáp ứng yêu cầu nghiệp vụ thay đổi nhanh.
- **Kinh nghiệm Solution Architect:** Kiến trúc microservice + legacy integration, chiến lược DevOps, IaC, containerization phù hợp bối cảnh ngân hàng.
- **Kỹ năng lập trình đa ngôn ngữ:** Java (Orchestrator), C++ (adapter), Pro*C (legacy), Python/Scala (ETL) giúp dẫn dắt đội đa nền tảng.
- **DevOps/CI-CD & Cloud:** Pipelines CI/CD, Kubernetes, Terraform, Vault đáp ứng tiêu chuẩn vận hành hiện đại.
- **Quản trị dữ liệu & DWH:** Kafka, Landing Zone, DWH/Mart bảo đảm năng lực báo cáo và analytics.
- **Giám sát & xử lý sự cố:** Observability stack, runbook, cảnh báo chủ động giúp giải quyết sự cố nhanh.
- **Đề xuất cải tiến:** Roadmap sprint, hạng mục tối ưu, experimentation (feature flag/canary) cho phép cải thiện liên tục.

## 9. Deliverables chủ đạo
- `docs/payment-openapi.yaml`, `docs/payment-asyncapi.yaml` (contract API/sự kiện).
- `src/payment/orchestrator/**` (service Spring Boot), `src/payment/tuxedo/TuxedoClient.java`, `src/payment/tuxedo/tuxedo_adapter.cpp`.
- `tuxedo/ubbconfig`, `tuxedo/DMCONFIG`, script `tmboot`, `tmshutdown`, `tmadmin` automation.
- Pipeline definition (GitLab CI/Jenkinsfile), Ansible/Terraform playbook.
- Runbook vận hành (start/stop domain, xử lý giao dịch treo, DR drill), dashboard Grafana.
- Bộ test contract, performance suite (Gatling/K6), checklist bảo mật/PCI DSS.


