# Cloud-Native + Legacy Integration Playbook

Tài liệu này tổng hợp các nguyên tắc, pattern và khuyến nghị khi kết nối kiến trúc cloud-native (container, microservice, event stream) với hệ thống legacy (Oracle Tuxedo, Pro*C, mainframe). Tập trung vào bối cảnh CoreBank Payment Lab, nơi Java 21/Spring Boot kết hợp với Oracle Tuxedo thông qua gateway hybrid.

---

## 1. Mục tiêu & Thách thức
- **Mục tiêu**  
  - Tận dụng hệ thống lõi legacy ổn định (Tuxedo + Pro*C) mà vẫn mở rộng được năng lực trên cloud.  
  - Giảm latency/tăng throughput nhờ hạ tầng cloud-native (autoscaling, virtual threads, caching).  
  - Chuẩn hóa API cho đối tác, kênh số, đồng thời duy trì tuân thủ bảo mật/PCI.
- **Thách thức chính**  
  - Khác biệt mô hình triển khai: container/Kubernetes vs domain Tuxedo truyền thống.  
  - Tốc độ phát triển: cloud-native release nhanh, legacy release chậm & phức tạp.  
  - Quan sát & giám sát: log/metric tách biệt, khó correlation.  
  - Quản lý giao dịch: legacy dùng XA/2PC, cloud-native hướng tới eventual consistency.

---

## 2. Nguyên tắc kiến trúc
- **Strangler Fig Pattern**: bọc legacy bằng API gateway/microservice, sau đó rút dần chức năng ra cloud khi sẵn sàng.
- **Anticorruption Layer**: Java service chuẩn hóa DTO, business rule nhẹ, tránh logic bị “rò” từ legacy sang front-end.
- **Idempotency & Retry**: bắt buộc cho các call sync vào legacy để xử lý lỗi mạng.  
- **Event-Driven Extensions**: dùng outbox → Kafka để mở rộng nghiệp vụ (analytics, alert, monitoring) mà không can thiệp legacy.
- **Security by Design**: đặt gateway ở zone an toàn, WAF + IAM + mTLS, map user context vào legacy theo nguyên tắc least privilege.
- **Observability**: correlation ID xuyên suốt, export metric từ cả hai phía để giám sát end-to-end.

---

## 3. Patterns tích hợp
| Pattern | Mô tả | Áp dụng trong Lab |
|---------|------|-------------------|
| **API Gateway / BFF** | Java Spring Boot đứng trước, xử lý auth, throttling, chuyển đổi payload. | `corebank-payment`, `corebank-orchestrator`, `corebank-investigation`. |
| **Service Adapter** | Java ↔ Tuxedo thông qua Jolt/ATMI; map DTO ↔ FML32/VIEW. | `MockTuxedoClient`, `TuxedoGateway`. |
| **Outbox Pattern** | Ghi log giao dịch vào DB rồi phát sự kiện bất đồng bộ. | `OutboxRelay`, `PaymentEventStreamProcessor`. |
| **Sidecar Observability** | Thu log/metric legacy qua agent (Logstash, Fluentd, Prometheus exporter). | Đề xuất cho Tuxedo `ULOG` và TMIB. |
| **Config Federation** | Spring Cloud Config kết hợp config legacy truyền thống (UBBCONFIG). | Profile `lab-cloud` + thủ công trong Tuxedo. |
| **Feature Toggle & Gradual Cutover** | Bật/tắt route sang legacy hoặc microservice mới theo từng nhóm khách hàng. | Dùng LaunchDarkly/Config Server hoặc field trong DB. |

---

## 4. Kiến trúc tham chiếu
```
K8s Cluster (Cloud-Native)
  ├── API Gateway / Ingress (mTLS, WAF)
  ├── Spring Boot Services (Java 21, virtual threads)
  │      ├── REST Controllers
  │      ├── Tuxedo Gateway Client (Jolt/ATMI)
  │      └── Event Outbox / Kafka Producer
  ├── Redis / Cache Layer
  └── Observability Stack (Prometheus, Grafana, Loki, OpenTelemetry Collector)

Legacy Zone (Data Center / VM)
  ├── Oracle Tuxedo Domain
  │      ├── Tuxedo servers (Pro*C binaries)
  │      └── UBBCONFIG, DMCONFIG
  ├── Oracle Database
  └── Monitoring (TMIB, ULOG, SNMP)

Integration
  - Secure VPN or Direct Connect
  - TLS termination + client cert auth
  - Correlation ID propagation
```

---

## 5. Quy trình giao dịch mẫu
1. Client → Java API (`POST /payments`) kèm header `X-Correlation-ID`.  
2. Java orchestrator validate, map DTO → FML32, gọi Tuxedo qua `TuxedoGateway`.  
3. Tuxedo route service `PAYMENT_INIT` → Pro*C server, xử lý DB transaction.  
4. Pro*C trả response + status; Tuxedo gửi lại Java via Jolt/ATMI.  
5. Java ghi outbox, trả kết quả REST (201/200).  
6. Outbox relay publish event to Kafka → streaming analytics/cloud services.  
7. Observability: logs & metrics ở cả hai phía gắn chung correlation ID.

---

## 6. Sắp xếp đội hình & quy trình
- **Hybrid Squad**: developer Java + developer Pro*C + architect Tuxedo + SRE.  
- **Change Management**:  
  - Cloud side deploy theo CI/CD (Blue/Green, Canary).  
  - Legacy side deploy theo release window; automation bằng `tmshutdown`, `tmboot` script.  
  - Định kỳ sync config (version hóa UBBCONFIG, DMCONFIG).  
- **Environment Strategy**:  
  - Dev: mock Tuxedo (embedded server hoặc service stub).  
  - SIT/UAT: staging domain Tuxedo đồng bộ data sample.  
  - Production: network segmentation, allow-list port Jolt/ATMI.

---

## 7. Bảo mật & Compliance
- **Network**: VPN/MPLS, firewall rule rõ ràng, TLS mutual.  
- **Credential**: Java service account map tới user Tuxedo, limit transaction scope.  
- **Audit**: log auth event, request/response, error mapping.  
- **Data Residency**: xác định dữ liệu nào có thể lưu cache cloud, dữ liệu nào phải giữ on-prem.  
- **Key Management**: sử dụng HSM/KMS cho secret dùng chung (DB password, keystore).

---

## 8. Hiệu năng & Độ tin cậy
- **Latency**  
  - Virtual threads + structured concurrency trong Java giảm overhead.  
  - Giữ message payload gọn, minimize FML size.  
  - Tối ưu Jolt pool size, ATMI timeout.  
- **Throughput**  
  - Scale horizontal Java service qua Kubernetes HPA.  
  - Dành thread pool riêng cho gọi Tuxedo để tránh nghẽn.  
  - Cache lookups, batch calls khi có thể.  
- **Resilience**  
  - Retry (with backoff) cho lỗi transient, Circuit Breaker (Resilience4j).  
  - Fallback -> queue (Kafka) nếu Tuxedo offline, xử lý khi hệ thống trở lại.  
  - Health check từ Java tới Tuxedo (ping service) expose ra `/actuator/health`.

---

## 9. Roadmap modernize
- **Ngắn hạn**: expose API, unify monitoring, áp dụng outbox/event streaming.  
- **Trung hạn**: refactor các dịch vụ Pro*C đơn giản sang Java microservice; dần thay Tuxedo by domain.  
- **Dài hạn**: re-platform DB sang cloud (Oracle Autonomous DB), thay 2PC bằng Saga pattern cho dịch vụ mới.

---

## 10. Checklist triển khai cloud-native integration
- [ ] Đảm bảo kết nối mạng bảo mật (VPN, firewall rules).  
- [ ] Cấu hình profile `lab-java21,lab-cloud`, verify Spring Cloud Config & Eureka hoạt động.  
- [ ] Thiết lập `TuxedoGateway` thực tế (Jolt hoặc ATMI) và kiểm tra load.  
- [ ] Ánh xạ correlation ID xuyên suốt (Java ↔ Tuxedo ↔ log DB).  
- [ ] Bật Prometheus scrape & log shipping cho cả Java lẫn Tuxedo.  
- [ ] Kiểm thử hiệu năng với workload thực (latency, throughput, failure scenarios).  
- [ ] Cập nhật tài liệu vận hành & playbook sự cố.  
- [ ] Đào tạo đội SRE/DevOps về process deploy song song (cloud + legacy).

---

## 11. Nguồn tham khảo
- `docs/hybrid_java_proc_tuxedo_architecture.md` – kiến trúc hybrid chi tiết Java ↔ Pro*C.  
- `docs/corebank_payment_lab_cloud.md` – hướng dẫn Spring Cloud với lab Java 21.  
- Oracle Tuxedo Deployment Guide, Oracle Cloud Infrastructure Networking, CNCF Cloud Native Glossary.
