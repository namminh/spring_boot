# SSOT trong CoreBank Payment Lab và định hướng dự án thực tế

Tài liệu này làm rõ cách nguyên tắc Single Source of Truth (SSOT) được áp dụng trong lab hiện tại, đồng thời nêu ra các điều chỉnh cần thiết khi chuyển giao sang dự án sản xuất. Mục tiêu là giúp đội ngũ hiểu ranh giới dữ liệu, quyền sở hữu và cơ chế giao tiếp xuyên suốt chuỗi hệ thống.

---

## 1. Lab: SSOT được hiện thực như thế nào?

### 1.1 Hệ thống ghi nhận
- **Payment Service** đóng vai trò hệ thống ghi nhận cho các thực thể thanh toán (`PaymentEntity`) và sự kiện outbox (`OutboxEventEntity`). Mọi API đọc/ghi đều đi qua lớp orchestrator → DB PostgreSQL.
- **Outbox → Kafka (mock)**: outbox chỉ lưu bản gốc; sau khi publish, trạng thái event chuyển `SENT` nhưng không thay đổi dữ liệu gốc, đảm bảo truy vết.
- **Orchestrator/Investigation** chỉ đọc hoặc đồng bộ theo hợp đồng API; không tự tạo bản ghi thanh toán.

### 1.2 Hợp đồng dữ liệu
- `docs/corebank_payment_lab_java21_detailed.md` mô tả rõ DTO record, `PaymentResponseDto`, `CoreStatusSnapshot`. Các service đều dùng cấu trúc chung, thể hiện contract-first dù lab chưa generate type tự động.
- Schema outbox/REST được xem như nguồn chân lý cho client (frontend giả lập hoặc dịch vụ khác).

### 1.3 Frontend (giả lập)
- Lab không có frontend thật; scripts CLI chỉ gọi API. Điều này củng cố thông điệp: mọi trạng thái hiển thị phải lấy từ backend.
- Nếu viết UI demo, UI chỉ cache ngắn hạn, luôn refetch theo reference và metrics.

### 1.4 Đồng bộ & quan sát
- Các dashboards/metrics (Actuator, Micrometer) lấy dữ liệu trực tiếp từ backend – không duy trì bản sao.
- Khi Structured Concurrency đẩy throughput, vẫn dựa trên cùng nguồn dữ liệu (DB/outbox) → không tạo nguồn sự thật thứ hai.

---

## 2. Dự án thực tế: cần bổ sung gì?

### 2.1 Xác định chủ sở hữu dữ liệu
- Liệt kê từng domain: thanh toán, cảnh báo, hồ sơ điều tra, khách hàng… Chỉ định hệ thống ghi nhận (system of record) và trách nhiệm cập nhật.
- Thiết lập **ma trận RACI dữ liệu**: ai được ghi, ai đọc, ai audit – tránh việc nhiều đội “sở hữu” chung.

### 2.2 Hợp đồng dữ liệu sống
- Chuẩn hóa OpenAPI/GraphQL; lưu schema trong repo riêng hoặc module chung.
- CI lint: cấm breaking change, sinh client/server stub tự động (Java, TypeScript).
- ADR ghi rõ lifecycle version, chiến lược deprecation.

### 2.3 Đồng bộ đa hệ thống
- Với ERP/legacy, dùng event sourcing hoặc CDC để phát sự thật từ hệ thống chủ.
- Nếu cần projection cache (Elastic, Redis), đảm bảo cơ chế reconcile định kỳ và theo dõi drift.
- Quy định quy trình quản lý schema DB (Flyway/Liquibase) chung để tránh lệch giữa environment.

### 2.4 Frontend & ứng dụng vệ tinh
- Frontend coi backend là SSOT: mọi cập nhật → gọi API, nhận phản hồi idempotent. Giữ state cục bộ chỉ tạm thời (optimistic UI) và reconcile lại.
- Dịch vụ vệ tinh (ví dụ Risk) đọc sự kiện và duy trì bản phản chiếu, nhưng khi xung đột phải tôn trọng hệ thống chủ qua API hợp lệ.

### 2.5 Ground rules vận hành
- Monitor drift qua checksum hoặc báo cáo so sánh (sổ cái vs projection).
- Incident runbook: nếu cache lệch → cách làm sạch và rehydrate từ SSOT.
- Thiết lập data governance: lineage, catalog, quyền truy cập.

---

## 3. Ánh xạ lab → sản xuất

| Lab component | Vai trò SSOT hiện tại | Điều chỉnh khi lên dự án |
|---------------|-----------------------|---------------------------|
| Payment Service (PostgreSQL) | System of record thanh toán | Production cần HA (RAC/Streaming Replication), chính sách khóa, auditing |
| Outbox table | Nguồn sự kiện chuẩn để publish | Thêm Dead-letter queue, retry policy, observability áp SLA |
| Monitoring Orchestrator | Projection từ Payment/Alert | Xác định rõ chỉ đọc; triển khai cache có TTL + invalidation |
| Investigation Service | Consumer API Payment, self-owned investigation DB | Đảm bảo contract versioning, sync webhook nếu cần |
| Kafka mock/log | Kênh phát sự thật thứ cấp | Thay bằng Kafka thực/ESB; quy định message schema registry |

---

## 4. Checklist triển khai SSOT cho dự án
1. **Mapping thực thể → hệ thống chủ**: document và cập nhật định kỳ.
2. **Tách schema contract** khỏi code triển khai, tự động hoá kiểm tra.
3. **Thiết kế API idempotent + constraint DB** để khóa race conditions.
4. **Bắt buộc hóa event sourcing/outbox** khi có đa consumer.
5. **Quan sát drift**: metric so sánh, cảnh báo TTL cache quá hạn.
6. **Runbook** xử lý cache/projection lệch so với SSOT.

---

## 5. Kết luận
- Lab CoreBank hiện đã mô phỏng tốt nguyên lý SSOT: backend làm master, các dịch vụ khác chỉ đọc theo hợp đồng. Tuy nhiên khi mở rộng sang dự án thực, cần layer governance và tooling mạnh hơn để duy trì nguyên tắc trong môi trường nhiều đội, nhiều hệ thống.
- Đặt SSOT làm tiêu chí đầu tiên trong mọi quyết định kiến trúc: chọn công nghệ chỉ là bước sau khi xác định dữ liệu thuộc về ai và bằng cách nào sự thật được lan tỏa.
