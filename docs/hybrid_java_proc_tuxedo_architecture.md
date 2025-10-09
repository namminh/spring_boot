# Nghiên cứu kiến trúc hybrid: Java API & Pro*C backend qua Oracle Tuxedo

Tài liệu này đi sâu vào mô hình hybrid kết hợp Java (Spring Boot) cho lớp API, Pro*C cho xử lý nghiệp vụ lõi, với Oracle Tuxedo làm cầu nối dịch vụ. Mục tiêu là cung cấp cái nhìn toàn diện về kiến trúc, luồng dữ liệu, cơ chế giao tiếp và các khía cạnh vận hành khi đội CoreBank cần duy trì hệ thống Pro*C hiện hữu nhưng vẫn mở API hiện đại cho kênh số.

---

## 1. Động lực kiến trúc hybrid
- **Khai thác tài sản hiện hữu**: các chức năng thanh toán/giải ngân đã được chuẩn hóa bằng Pro*C chạy trên Oracle DB; chi phí viết lại lớn.
- **Áp lực mở API**: ngân hàng cần cung cấp REST/gRPC cho kênh số, đối tác, open banking – Java/Spring Boot phù hợp hơn.
- **Tính ổn định của Tuxedo**: domain Tuxedo đang quản lý hàng trăm service Pro*C với cơ chế transaction, failover đã kiểm chứng.
- **Lộ trình chuyển đổi dần**: cho phép từng service Pro*C được refactor sang Java hoặc Microservice mà không ảnh hưởng toàn hệ thống.

---

## 2. Tổng quan kiến trúc
```
Client (Mobile/Web/Partner)
        |
   Java API Layer (Spring Boot, REST, Security)
        |
  Oracle Tuxedo Bridge (Jolt/Tuxedo GW, ATMI client)
        |
  Tuxedo Domain (BBL, BRIDGE, /WS, Bulletin Board)
        |
 Pro*C Services (Server Groups, XA, Oracle DB)
        |
   Oracle Database / Legacy Systems
```

- **Java API Layer**: cung cấp REST, authentication, throttling, logging, monitoring hiện đại.
- **Tuxedo Bridge**: chuyển đổi JSON ↔ Bản tin FML32 hoặc buffer RAW; quản lý connection pool tới domain.
- **Tuxedo Domain**: đăng ký service name, routing, transaction monitor, load balancing, restart.
- **Pro*C Services**: xử lý nghiệp vụ, tương tác với Oracle DB qua SQL embedded, đảm bảo ACID.

---

## 3. Thành phần Java API
- **Spring Boot Application**  
  - Controller/Handler nhận REST, gọi service orchestrator.  
  - Dùng `spring-boot-starter-security` (JWT, mTLS hoặc Basic tùy use-case).  
  - Cấu hình `spring.threads.virtual.enabled=true` để tối ưu latency.
- **Service Orchestrator**  
  - Ánh xạ DTO → thông điệp Tuxedo (FML32 map).  
  - Gọi gateway (Jolt hoặc ATMI) đồng bộ.  
  - Áp dụng Resilience4j cho retry, timeout, circuit breaker.
- **Outbox & Kafka (tuỳ chọn)**  
  - Sau khi Pro*C xử lý thành công, Java layer ghi outbox để phát sự kiện cho hệ thống số liệu hoặc cảnh báo.
- **Caching & Validation**  
  - Pre-validation request (schema, business rule nhẹ).  
  - Cache reference data (fx rate, branch) để giảm call legacy.

---

## 4. Oracle Tuxedo Domain
- **Cấu trúc cơ bản**  
  - `BBL`, `BRIDGE`, `DBBL` giúp quản lý bulletin board, giao tiếp liên domain.  
  - Server group chứa các process Pro*C, đăng ký service name (ví dụ: `PAYMENT_INIT`, `INVESTIGATION_LOOKUP`).
- **Giao tiếp với Java**  
  - **Jolt**: Java API → Jolt Connection Pools → Tuxedo domain. Dễ tích hợp Spring nhờ Jolt client.  
  - **ATMI client**: Java dùng ATMI API thông qua thư viện native (JNIAccess). Cho phép buffer FML, VIEW, CARRAY.
- **Transaction**  
  - X/Open XA hỗ trợ 2PC giữa Tuxedo và Oracle DB.  
  - Java layer nên chuyển transaction boundary cho Tuxedo để tránh phân tán 2-phase.
- **Quản lý cấu hình**  
  - `UBBCONFIG` định nghĩa server group, machine, network address.  
  - `DMCONFIG` nếu có domain link qua TCP/IP.  
  - Logging qua `ULOG` và optional monitoring (Tuxedo SNMP, TMIB).

---

## 5. Dịch vụ Pro*C
- **Pattern triển khai**  
  - Mỗi service Pro*C compile thành server binary (ví dụ `PAYSRV`), đăng ký các entry point `tpsvrinit`, `tpsvrdone`, `PAYMENT_INIT`.  
  - Sử dụng embedded SQL, gọi package Oracle PL/SQL, commit/rollback qua `tpcommit`/`tpabort`.
- **Quản lý tham số**  
  - Dữ liệu vào/out mapping `FML32` ↔ struct C do Pro*C định nghĩa.  
  - Khi cần payload phức tạp, dùng `VIEW` buffer (map sang struct) hoặc JSON string (CARRAY).
- **Hiệu năng**  
  - Tận dụng connection pooling trong Tuxedo.  
  - Dùng `TPNOREPLY`, `TPNOTRAN` khi lệnh chỉ đọc để giảm overhead transaction.
- **Quan sát**  
  - Log `USERLOG` theo chuẩn, include correlation ID do Java gửi (`TPTRANID`).  
  - Export metric (custom) qua file hoặc pipe cho Prometheus sidecar (nếu containerized).

---

## 6. Cầu nối thông điệp Java ↔ Pro*C
- **Mapping Layer**  
  - Java DTO → `FML32` key-value: reuse schema file `.fld` để bảo đảm type alignment.  
  - Build utility `FmlField` enum + converter (charsets, numeric).  
  - Đối với binary/large payload: encode Base64 hoặc chuyển sang `VIEW` buffer.
- **Correlation & Idempotency**  
  - Header `X-Correlation-ID` từ client truyền xuống Pro*C (qua field `CORR_ID`).  
  - Pro*C lưu bảng idempotent để tránh xử lý trùng.
- **Error Handling**  
  - Pro*C trả `tperrno`, `tperrordet` → map sang HTTP status và error code rõ ràng.  
  - Định nghĩa bảng chuyển đổi `TuxedoErrorMapper`.
- **Timeouts**  
  - Java set `tpacall`/`tpcall` timeout < SLA.  
  - Tuxedo `MAXGTTIME` và server-specific `RQ` (request queue) tuning.

---

## 7. Bảo mật & Compliance
- **AuthN/Z**  
  - Java layer làm gatekeeper: OAuth2, mTLS, WAF integration.  
  - Tuxedo domain chạy trong network phân tách, yêu cầu service account.  
  - Dùng `tpinit` với credential map theo user nhóm để audit.
- **Data Protection**  
  - Mask thông tin nhạy cảm trong log Java & Tuxedo.  
  - Encrypt in-transit qua TLS (Jolt over SSL, ATMI with SSL add-on).  
  - Tuân thủ PCI/GLBA: audit trail, PII retention rules.
- **Segregation of Duty**  
  - Dev Java không cần quyền deploy Pro*C và ngược lại.  
  - Sử dụng pipeline tách (CI/CD Java vs release domain Tuxedo).

---

## 8. Vận hành & Triển khai
- **Deployment Pattern**  
  - Java service container hóa (Docker/K8s), autoscale theo tải.  
  - Tuxedo & Pro*C vẫn on-prem hoặc VM cố định; có thể container hóa với base image Oracle, nhưng cần license.  
  - Bridge network: sử dụng VPN/Service Mesh (mTLS) giữa Kubernetes và Tuxedo LAN.
- **Observability**  
  - Java: Prometheus, OpenTelemetry, log structured.  
  - Tuxedo: TMIB, SNMP, custom exporter; gom log `ULOG` về ELK.  
  - Tích hợp APM (Dynatrace/AppDynamics) cho Tuxedo qua plugin nếu cần.
- **CI/CD**  
  - Java pipeline: unit test, integration test (mock Tuxedo), contract test.  
  - Pro*C pipeline: compile `.pc`, chạy test harness (SQL*Plus), deploy server group qua `tmloadcf`.
- **Disaster Recovery**  
  - Tuxedo hỗ trợ active-active replication (domain link).  
  - Java layer scale across AZ/region; failover Tuxedo endpoint qua DNS hoặc gateway.

---

## 9. Lộ trình hiện đại hóa
- **Phase 1 – Gateway hóa**: thêm Java API layer, không thay đổi Pro*C.  
- **Phase 2 – Hợp nhất logging/metrics**: correlation ID end-to-end, convert log Pro*C.  
- **Phase 3 – Dịch chuyển dần**: trích service Pro*C ít phụ thuộc, rewrite sang Java, deploy song song, route bằng feature flag.  
- **Phase 4 – Dỡ bỏ Tuxedo**: khi phần lớn nghiệp vụ đã chuyển, giữ Tuxedo cho batch/legacy còn lại hoặc chuyển sang messaging (Kafka) + microservice.

---

## 10. Checklist triển khai pilot
- [ ] Config domain Tuxedo test, expose Jolt listener với TLS.  
- [ ] Build Java gateway prototype, call service `PAYMENT_INIT`.  
- [ ] Thiết lập mapper FML32 ↔ DTO, unit test roundtrip.  
- [ ] Pro*C bổ sung logging correlation ID, xử lý timeout chuẩn.  
- [ ] Thiết lập monitoring song song (Prometheus + ULOG collector).  
- [ ] Chạy performance test: đo latency end-to-end, kiểm tra throughput, tune thread/connection pool.  
- [ ] Tài liệu hóa error mapping và playbook vận hành.

---

## 11. Tài liệu tham khảo
- Oracle Tuxedo Application Runtime for C/C++ Developer’s Guide.  
- Oracle Jolt Programmer’s Guide.  
- Spring Boot & Resilience4j Reference.  
- Tài liệu nội bộ: `docs/corebank_tuxedo_payment_solution.md`, `docs/corebank_payment_lab_java21_detailed.md`.
