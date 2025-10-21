# Solution Architect Playbook — Song Ngữ (Việt / Anh)

Tài liệu tổng hợp các nội dung đã thảo luận, cung cấp giải thích chi tiết bằng cả tiếng Việt và tiếng Anh để hỗ trợ ôn luyện phỏng vấn hoặc chia sẻ nội bộ.

---

## 1. Hệ thống thanh toán real-time
- **VN:** Hệ thống thanh toán real-time phải nhận giao dịch, xác thực, kiểm tra số dư, ghi nợ/ghi có đồng thời và trả kết quả trong vòng vài giây. Kiến trúc điển hình phân tầng: kênh giao dịch → gateway → orchestration → core banking/ledger. Để đạt “real-time”, cần API tốc độ cao, core ledger cập nhật tức thì, cơ chế HA/DR và giám sát chủ động. Chuẩn thông điệp (ISO 20022), bảo mật (mã hóa, tokenization) và SLA gần 100% là bắt buộc trong môi trường ngân hàng.
- **EN:** A real-time payment platform ingests requests, authenticates users, checks balances, debits/credits accounts synchronously, and returns results within seconds. The reference architecture spans channels → gateway → orchestration → core ledger. Achieving real-time latency requires high-throughput APIs, immediate ledger updates, resilient HA/DR design, and proactive monitoring. Standards such as ISO 20022, strong encryption/tokenisation, and near-100% SLAs are mandatory in banking.

## 2. Mở rộng sang nền tảng đặt cược online
- **VN:** Tận dụng lõi thanh toán có thể rút ngắn thời gian ra mắt, nhưng phải bổ sung: quản lý ví cược (số dư tạm giữ, hoàn tiền), dịch vụ odds và kết quả, giám sát gian lận đặc thù, đảm bảo tuân thủ pháp lý (giấy phép, giới hạn độ tuổi, AML). Lớp UI/UX phải hỗ trợ theo dõi vé cược và thông báo kết quả tức thời. Khả năng co giãn ngang và xử lý lưu lượng tăng đột biến theo sự kiện là yếu tố sống còn.
- **EN:** Reusing the payment core accelerates rollout yet requires add-ons: betting wallet management (held balances, settlements), odds feeds and result processing, anti-fraud tuned for betting, and compliance (licensing, age limits, AML). UX must surface bet slips, status tracking, and instant result notifications. Horizontal scalability and burst handling around live events are critical.

## 3. Giao tiếp API qua một port (ví dụ 5050) và vai trò socket
- **VN:** Server lắng nghe trên socket `IP:5050`; mỗi kết nối TCP được OS gán cặp `client_ip:ephemeral_port ↔ server_ip:5050`. Server dùng `accept()` để tạo socket con, worker đọc request và ghi response trên chính socket đó. Client không cần mở port riêng cho response, nhưng sẽ được OS cấp một port tạm cho chiều nguồn. Nếu lưu lượng lớn, dùng thread pool hoặc mô hình non-blocking để xử lý song song. Load balancer có thể phân tán sang nhiều instance cùng mở port 5050 khi cần mở rộng.
- **EN:** The server listens on `IP:5050`; the OS maps each TCP session as `client_ip:ephemeral_port ↔ server_ip:5050`. Server calls `accept()` to spawn a per-client socket, workers read requests and write responses through that socket. Clients do not require dedicated response ports; the OS assigns ephemeral source ports automatically. High concurrency is handled via thread pools or non-blocking I/O, and load balancers can distribute traffic across multiple instances on the same port.

## 4. Connection pool với cơ sở dữ liệu
- **VN:** Connection pool giữ sẵn các kết nối DB để tái sử dụng, giảm chi phí handshake. Cần cấu hình `max/min size`, `connection timeout`, `max lifetime`, `idle timeout`, heartbeat và validation query. Monitor số kết nối đang dùng/chờ và log khi pool cạn để phát hiện rò rỉ. Backpressure giúp bảo vệ DB khi pool hết permit. Khi scale ngang nhiều instance, tổng số kết nối pool không được vượt quá ngưỡng DB hỗ trợ.
- **EN:** A database connection pool maintains reusable connections, avoiding the overhead of creating/destroying sessions. Configure `max/min size`, `connection timeout`, `max lifetime`, `idle timeout`, plus heartbeat/validation. Monitor in-use, idle, and waiting connections; log pool exhaustion to detect leaks. Backpressure prevents overload when no permits remain. When scaling horizontally, ensure the aggregate pool size stays within the database’s connection capacity.

## 5. Oracle Tuxedo WSL configuration
- **VN:** Cấu hình WSL mẫu: `-n //10.192.26.87:8000` nghe ban đầu trên mạng nội bộ, `-H//202.58.245.87:8000` trả địa chỉ công khai cho client sau handshake, `-x 2` bật chế độ dual-address. Dải port `-p 9501 -P 9999` dành cho WSH con phục vụ từng session; nếu firewall không mở dải này, client không thể sử dụng dịch vụ. `-m 200 -M 500` ấn định lượng WSH tối thiểu/tối đa; `-T 10` đặt timeout 10 giây cho handshake.
- **EN:** Sample WSL config: `-n //10.192.26.87:8000` listens on the internal network, `-H//202.58.245.87:8000` hands back the public address, and `-x 2` enables dual-address/NAT mode. The port range `-p 9501 -P 9999` is reserved for child WSH handlers; without firewall access, clients cannot complete sessions. `-m 200 -M 500` sets minimum/maximum WSH counts; `-T 10` enforces a 10-second handshake timeout.

## 6. So sánh kiến trúc multi-port (WSH) vs single-port
- **VN:** Kiểu multi-port (WSL/WSH) tạo tiến trình handler riêng cho mỗi phiên, dễ cô lập và debug nhưng yêu cầu mở dải port rộng, khó quản trị firewall và chạm giới hạn port. Kiểu single-port (HTTP, gRPC, WebSocket) xử lý song song trên một port nhờ thread pool hoặc multiplexing; firewall đơn giản, dễ scale nhưng cần framework hiện đại và quản lý tài nguyên chặt. Phần lớn hệ thống mới chọn single-port vì hiệu quả vận hành cao hơn.
- **EN:** The multi-port pattern (WSL/WSH) spawns a dedicated handler per session, offering strong isolation yet requiring broad port ranges, complex firewall rules, and hits OS limits earlier. The single-port pattern (HTTP, gRPC, WebSocket) multiplexes many sessions through one port using thread pools or event loops; it simplifies firewalls and scaling but demands modern frameworks and precise resource governance. Most modern systems prefer single-port for operational efficiency.

## 7. Thread pool, multiplexing, semaphore và backpressure
- **VN:** Thread pool giữ sẵn worker; mỗi kết nối mới được gán cho một thread để xử lý, không cần mở port phụ. Multiplexing (select/epoll, HTTP/2 stream) cho phép một thread theo dõi nhiều socket và xử lý từng event khi sẵn sàng. Semaphore đặt giới hạn số request đồng thời tiêu thụ tài nguyên (ví dụ kết nối DB); khi hết permit, request phải chờ. Backpressure đẩy áp lực ngược: khi queue đầy hoặc semaphore cạn, server trả 429/503 hoặc trì hoãn để client giảm tốc, tránh sập hệ thống.
- **EN:** Thread pools keep pre-started workers; each accepted connection is handed to a thread with no extra ports needed. Multiplexing (select/epoll, HTTP/2 streams) lets one loop watch thousands of sockets and handle events on readiness. Semaphores cap the number of concurrent requests hitting shared resources (e.g., DB connections); when permits are exhausted requests wait. Backpressure pushes upstream resistance: with full queues or exhausted permits the server returns 429/503 or slows responses so clients throttle, preserving stability.

## 8. Semaphore vs Mutex
- **VN:** Semaphore chứa nhiều permit, cho phép tối đa `N` thread vào vùng dùng chung; một thread `acquire`, thread khác hoàn toàn có thể `release`. Mutex là dạng đặc biệt chỉ có 1 permit và ràng buộc sở hữu: thread nào khóa phải tự mở. Semaphore linh hoạt để giới hạn tài nguyên (pool, slot), mutex phù hợp bảo vệ vùng phê bình cần độc quyền.
- **EN:** Semaphores hold multiple permits, allowing up to `N` threads into a shared section; one thread may release permits acquired by another. A mutex is a special case with a single permit and ownership semantics: the locking thread must unlock. Use semaphores to cap shared resources (pools, slots) and mutexes for strict mutual exclusion.

## 9. Ví dụ 1.000 request với semaphore
- **VN:** Với semaphore 100 permit, 100 request đầu `acquire` và tiếp tục xử lý; 900 request còn lại bị đẩy vào hàng chờ nội bộ của semaphore, thread bị block nên không tốn CPU. Khi một request hoàn tất và `release`, semaphore tăng permit lên 1, đánh thức một thread đang chờ. Bộ nhớ thêm rất ít (chỉ bộ đếm và danh sách chờ), không tạo 1.000 bản sao dữ liệu. Đây là cách kiểm soát số request hoạt động đồng thời.
- **EN:** With a 100-permit semaphore, the first 100 requests acquire permits and proceed; the remaining 900 enter the semaphore’s wait queue, blocking without burning CPU. When a request finishes and releases its permit, the semaphore increments the count and wakes one waiting thread. Memory overhead is minimal (counter plus wait list); there are no thousand data replicas. This effectively caps concurrent active requests.

## 10. Kiến trúc single-port với semaphore điều tiết
- **VN:** Luồng chuẩn: listener nhận socket → queue nội bộ → semaphore kiểm tra còn permit → worker xử lý và truy cập tài nguyên → trả kết quả → release permit. Nếu semaphore hết permit, request bị chặn hoặc trả lỗi, tạo backpressure tự nhiên cho client/gateway. Sơ đồ ASCII:
```
Client -> API Listener (port 5050) -> Request Queue -> Semaphore (permits N)
        -> Worker Pool -> Shared Resource (DB/cache) -> Response -> Client
```
- **EN:** Standard flow: listener accepts sockets → internal queue → semaphore checks permits → worker processes and hits shared resources → response → release permit. When permits are exhausted, requests block or return errors, providing natural backpressure to clients/gateways. ASCII sketch identical to above.

## 11. Thiết kế hệ thống giao dịch real-time (phỏng vấn)
- **VN:** Khi gặp câu hỏi “real-time trading <50 ms cho hàng triệu người dùng”, cần làm rõ phạm vi: chỉ phát giá hay khớp lệnh, latency tính từ đâu, vùng địa lý, quy định. Kiến trúc gồm: ingestion (FIX/FAST/ITCH → bus in-memory), pricing/matching engine giữ dữ liệu trong RAM, phân phối qua pub/sub (gRPC/WebSocket/QUIC) với coalescing, lưu log append-only, và cơ chế backpressure cho client chậm. Ops cần observability chi tiết, replay log, circuit breaker, DR active-active. Cách trả lời phải có latency budget cho từng đoạn, so sánh trade-off (bare-metal vs cloud) và đề cập compliance (audit, MiFID/SEC).
- **EN:** When asked to design “a real-time trading system updating millions of users under 50 ms,” clarify scope: quote-only or order capture, latency definition, regions, regulations. Architecture spans ingestion (FIX/FAST/ITCH → in-memory bus), pricing/matching engines running in RAM, distribution via pub/sub (gRPC/WebSocket/QUIC) with coalescing, append-only logging, and backpressure for slow clients. Operations require deep observability, replayable logs, circuit breakers, and active-active DR. The interview answer should allocate latency budgets per stage, explain trade-offs (bare-metal vs managed cloud), and mention compliance/audit considerations.

## 12. Mối liên hệ giữa kiến trúc phần mềm và cách máy tính hoạt động
- **VN:** Hiểu kiến trúc phần mềm phải gắn với cách máy tính vận hành: tiến trình, thread, socket, semaphore, cache, scheduler quyết định cách hệ thống phản ứng khi tải cao hoặc lỗi. Tuy nhiên kiến trúc còn bao gồm nghiệp vụ, tổ chức, chi phí, tuân thủ. Một Solution Architect cần nắm cả nền tảng máy lẫn bối cảnh kinh doanh để thiết kế hệ thống bền vững.
- **EN:** Understanding software architecture is inseparable from how computers operate: processes, threads, sockets, semaphores, caches, and schedulers dictate behaviour under load or failure. Architecture also spans business domains, organisational structures, cost, and compliance. A Solution Architect must bridge computer fundamentals with business context to craft resilient systems.

---

> Ghi chú: Nội dung Việt/Anh có thể được điều chỉnh cho phù hợp với từng buổi phỏng vấn hoặc tài liệu đào tạo cụ thể.

