# Nghiên cứu sâu: Giảm độ trễ & tăng thông lượng trong CoreBank Payment Lab

Tài liệu này đào sâu vào các biện pháp kỹ thuật đã áp dụng (và đề xuất) để giảm latency, tăng throughput sau khi nâng cấp CoreBank Payment Lab lên Java 21 và Spring Boot 3.2.5. Nội dung tập trung vào cơ chế vận hành, chỉ số quan sát và kế hoạch thử nghiệm hiệu năng nhằm giúp đội phát triển/vận hành hiểu rõ lợi ích cũng như rủi ro cần kiểm soát.

---

## 1. Mục tiêu & Phạm vi
- Làm rõ cách virtual threads, structured concurrency và caching hiện tại giúp thu ngắn thời gian đáp ứng các API, job nền.
- Xác định tác động tới throughput của các vùng quan trọng: xử lý REST (`PaymentController`), relay outbox, giao tiếp Tuxedo (mock), pipeline Kafka.
- Đề xuất phương pháp đo lường, benchmark và chiến lược quan sát để xác nhận lợi ích trong môi trường lab/production-like.
- Cảnh báo các tình huống có thể làm mất lợi thế latency/throughput, đưa ra biện pháp giảm thiểu.

---

## 2. Bối cảnh & baseline
- **Trước nâng cấp (Java 17, thread pool cố định)**: mỗi request REST hoặc job outbox chiếm một platform thread trong `taskExecutor`. Khi khối lượng tăng, việc chờ I/O (JDBC, mock Tuxedo) dẫn tới queue dồn và độ trễ tail (p95/p99) tăng đáng kể.
- **Sau nâng cấp (Java 21)**:
  - Spring Boot tự khởi tạo executor dựa trên virtual threads khi profile `lab-java21` bật `spring.threads.virtual.enabled=true`.
  - Các đoạn xử lý song song chuyển sang `StructuredTaskScope.ShutdownOnFailure`, tận dụng virtual threads để gom kết quả nhanh mà không tăng chi phí context-switch vật lý.
  - Caching (`@Cacheable/@CachePut`) giảm số query trùng lặp cho `findByReference` và `countByStatus`, giảm tải DB.
  - Record + pattern matching giảm overhead tạo object và mô hình control flow rõ ràng hơn, giảm CPU cho logic mapping.

---

## 3. Cơ chế giảm latency

### 3.1 Virtual threads cho REST metrics
- Endpoint `GET /api/v1/payments/metrics/status-count` (`PaymentController.statusBreakdown`) fork một subtask cho mỗi trạng thái thanh toán.
- Tác động: 4 truy vấn `count(*)` được thực hiện đồng thời thay vì tuần tự. Kết quả chung trả về trong thời gian tương đương truy vấn chậm nhất, giảm ~3-4 lần latency so với baseline.
- Tình huống cần lưu ý: nếu `countByStatus` bị pin (ví dụ driver JDBC không hỗ trợ virtual threads), request sẽ bị buộc quay về platform thread. Theo dõi metric `jvm.threads.virtual.pinned` để phát hiện.

### 3.2 Structured concurrency trong OutboxRelay
- Job `OutboxRelay.forwardPendingEvents` lấy batch tối đa 20 event, fork subtask publish cho từng event. Virtual threads giúp xử lý đồng thời mà không phải cấu hình thread pool cơ sở.
- Khi một subtask lỗi, `ShutdownOnFailure` hủy cả batch và fail fast, đảm bảo không giữ tài nguyên lâu.
- Thời gian flush batch giảm từ `N * latency_publish` xuống `max(latency_publish)`, tạo bước nhảy lớn về throughput outbox.

### 3.3 Caching kết quả nóng
- `PaymentOrchestratorService.findByReference` cache kết quả để retry cùng reference hoặc dashboard truy vấn liên tục không phải hitting DB.
- `countByStatus` cache 60s (TTL theo config cache). Latency của dashboard metrics ổn định ngay cả khi DB load tăng.
- Lưu ý: invalidation toàn bộ cache khi orchestrate payment (`@CacheEvict(allEntries=true)`) khiến batch processing lớn có thể tăng latency tạm thời do cache miss liên tục. Có thể cân nhắc cache aside hoặc time-based eviction nếu cần.

### 3.4 Giảm overhead logic ứng dụng
- `Payment` record, `OutboxDispatchResult` record pattern giúp runtime tạo ít lớp proxy/bean. Dù lợi ích không lớn như virtual threads, CPU tiết kiệm giúp tài nguyên dành cho xử lý IO-bound.
- Tuxedo mock delay (`Thread.sleep`) vẫn được bao kín trong virtual thread; latency gọi Tuxedo không đổi nhưng không làm block carrier thread, do đó request khác không bị dồn.

---

## 4. Cơ chế tăng throughput

### 4.1 Tái sử dụng connection pool
- Virtual threads giúp tận dụng tối đa pool JDBC hiện có (Hikari). Khi một virtual thread chờ I/O, carrier thread có thể chuyển sang virtual thread khác, giúp throughput request cao hơn mà không tăng `maximumPoolSize`.
- Cần giám sát `hikaricp.connections.pending` để đảm bảo pool đủ lớn cho workload mới.

### 4.2 Outbox dispatch song song
- Batch 20 event chạy đồng thời → throughput ~`20 / max(latency_publish)` event/s, so với tuần tự chỉ đạt `1 / latency_publish`.
- Thử nghiệm thực tế nên bật/tắt structured concurrency để đo chênh lệch, nhất là khi chuyển từ log fallback sang Kafka thật.

### 4.3 Kafka Streams & cache
- Khi event được phát nhanh hơn, `PaymentEventStreamProcessor` cần được tune để xử lý kịp: cập nhật `max.task.idle.ms`, `num.stream.threads`. Virtual threads không áp dụng trực tiếp cho Kafka Streams nhưng throughput upstream tăng sẽ đẩy nhu cầu tuning downstream.

---

## 5. Phương pháp đo lường & benchmark

### 5.1 Scenario đề xuất
1. **REST latency**: dùng k6 hoặc Gatling bắn `POST /api/v1/payments` (mix data) và `GET /api/v1/payments/{ref}` với load tăng dần. So sánh p95/p99 latency khi bật/tắt virtual threads (profile `lab-java21` vs `lab` cũ) để định lượng lợi ích.
2. **Metrics endpoint**: benchmark `GET /api/v1/payments/metrics/status-count` ở 100 RPS, theo dõi time-to-first-byte và DB query latency.
3. **Outbox throughput**: seed 1k bản ghi pending, kích hoạt relay, đo số event/s publish thành công. Quan sát log `NAMNM OUTBOX` và metric custom `payment.outbox.publish.duration`.
4. **End-to-end pipeline**: firehose ~200 event/s (mock) kiểm tra latency từ REST → outbox → Kafka → Kafka Streams → consumer (nếu có).

### 5.2 Công cụ
- **OpenTelemetry + Micrometer**: thu thập `http.server.requests`, `jvm.threads.virtual.*`, `hikaricp.*`, `payment.outbox.*`.
- **JDK Flight Recorder / Async Profiler**: quan sát stack pinning, hotspot CPU khi virtual thread bị chuyển sang platform thread.
- **Prometheus + Grafana**: tạo dashboard latency/throughput, highlight sự khác biệt khi toggling virtual threads/caching.
- **Replay log**: sử dụng `scripts/replay_outbox.sh` (nếu có) để tái hiện lô pending trong môi trường staging.

### 5.3 KPI mục tiêu
- REST p95 < 150 ms ở 200 RPS (lab).
- Outbox flush batch 20 event trong < 120 ms (khi publish tới Kafka mock).
- `jvm.threads.virtual.pinned` ~ 0 trong suốt thời gian benchmark.
- `hikaricp.connections.usage` trung bình < 0.7 để đảm bảo headroom.

---

## 6. Quan sát & cảnh báo
- **Metric bắt buộc**: `jvm.threads.virtual.count`, `jvm.threads.virtual.daemon`, `jvm.threads.virtual.pinned`, `payment.outbox.publish.duration`, `payment.outbox.batch.size`, `http.server.requests{uri="/api/v1/payments"}`.
- **Log pattern**: prefix `NAMNM REST` (REST flow), `NAMNM SRV` (service layer), `NAMNM OUTBOX` (relay), `Tuxedo mock`. Nên cấu hình log aggregator filter theo prefix để truy tìm latency spikes.
- **Alert**:
  - Nếu `payment.outbox.publish.failure.rate` > 5% trong 5 phút → cảnh báo nền tảng.
  - Nếu `http.server.requests{status="5xx"}` tăng song song với `jvm.threads.virtual.pinned` → nghi ngờ pinning driver.

---

## 7. Rủi ro & biện pháp
- **Pinning bởi JDBC/Gateway legacy**: kiểm tra driver Oracle/Tuxedo thật; nếu pinning cao, cân nhắc chuyển riêng endpoint sang executor platform thread truyền thống (`@VirtualThreads(false)` hoặc custom `TaskExecutor`).
- **Pool giới hạn**: throughput tăng nhưng connection pool quá nhỏ sẽ kéo dài wait time. Theo dõi `hikaricp.connections.pending` và điều chỉnh `maximumPoolSize`.
- **Outbox storm**: Khi event publish thất bại liên tục, batch retry ngay có thể gây storm. Nên thêm backoff hoặc chuyển sang `ShutdownOnSuccess` với retry controller.
- **GC pressure**: record tạo nhiều đối tượng ngắn sống, nhưng cũng tăng áp lực GC. Theo dõi `jvm.gc.pause` khi load cao.

---

## 8. Lộ trình tiếp theo
1. Thiết lập benchmark tự động (GitHub Actions nightly) chạy k6/gatling, so sánh profile `lab` vs `lab-java21`.
2. Bổ sung Micrometer meter custom cho thời gian fetch `PaymentRepository.countByStatus` và `OutboxRepository.findTop20...`.
3. Viết ADR về việc áp dụng virtual threads + structured concurrency, kèm kết quả benchmark.
4. POC tích hợp real Kafka/Tuxedo (hoặc giả lập gần thực tế) để xác nhận throughput khi giao tiếp với hệ thống ngoài core banking.
5. Đánh giá tiềm năng sử dụng sealed classes cho state machine của điều tra nhằm tiếp tục giảm boilerplate và điều phối concurrency an toàn hơn.

---

## 9. Tài liệu tham khảo
- `docs/corebank_payment_lab_java21_architecture.md` – mô tả tổng quan kiến trúc sau nâng cấp Java 21.
- `src/main/java/com/corebank/payment/api/PaymentController.java` – `statusBreakdown` sử dụng structured concurrency.
- `src/main/java/com/corebank/payment/infrastructure/outbox/OutboxRelay.java` – dispatch song song qua virtual threads.
- Micrometer & Spring Boot 3.2 reference guide – chương `Virtual threads` & `Observation`.
- JEP 444 (Virtual Threads), JEP 453 (Structured Concurrency) – cơ sở kỹ thuật cho các tối ưu.
