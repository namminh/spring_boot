# CoreBank Payment Lab – Hướng dẫn Spring Cloud Deployment

Tài liệu này mô tả cách kích hoạt và vận hành bộ dịch vụ CoreBank Payment Lab khi triển khai trong môi trường đám mây có sử dụng Spring Cloud Config, Netflix Eureka và Spring Cloud LoadBalancer. Đây là phần mở rộng cho tài liệu chi tiết Java 21, tập trung vào cấu hình và quy trình chạy.

---

## 1. Thành phần & Kiến trúc
- **Config Server**: cung cấp cấu hình tập trung cho các dịch vụ. Tài liệu này giả định endpoint mặc định `http://localhost:8888`.
- **Eureka Server**: registry cho phép các dịch vụ đăng ký và tìm nhau. Endpoint mặc định `http://localhost:8761/eureka`.
- **CoreBank Services** (đã bật Spring Cloud):
  - `corebank-payment`
  - `corebank-orchestrator`
  - `corebank-investigation`
  - `corebank-payment-stream`
- **Client-side Load Balancing**: `PaymentStatusClient` sử dụng RestTemplate có `@LoadBalanced` khi discovery bật, để gọi Payment Service thông qua service-id thay vì hostname cố định.

---

## 2. Hồ sơ cấu hình (Profiles)
- `lab-java21`: bật virtual thread, structured concurrency và tối ưu JVM 21.
- `lab-cloud`: bật Spring Cloud Config, discovery và load balancer.
- Khi triển khai cloud, đặt biến môi trường `SPRING_PROFILES_ACTIVE=lab-java21,lab-cloud`.

---

## 3. Biến môi trường quan trọng
| Biến | Mặc định | Mô tả |
|------|----------|-------|
| `SPRING_CLOUD_CONFIG_ENABLED` | `true` (khi bật `lab-cloud`) | Bật client Config Server. |
| `SPRING_CLOUD_CONFIG_URI` | `http://localhost:8888` | URL Config Server. |
| `EUREKA_CLIENT_ENABLED` | `true` (khi bật `lab-cloud`) | Cho phép dịch vụ đăng ký vào Eureka. |
| `EUREKA_CLIENT_SERVICE_URL` | `http://localhost:8761/eureka` | Endpoint Eureka. |
| `INVESTIGATION_PAYMENT_BASE_URL` | `http://corebank-payment-lab` (khi bật `lab-cloud`) | Service-ID để Investigation gọi Payment qua LoadBalancer. |
| `SPRING_APPLICATION_NAME` | tùy service (`corebank-payment`, `corebank-orchestrator`, `corebank-investigation`, `corebank-payment-stream`) | Có thể override nếu muốn namespace khác trong registry. |

> **Lưu ý**: Trong môi trường cục bộ không có Config/Eureka, có thể tắt bằng `SPRING_CLOUD_DISCOVERY_ENABLED=false`, `EUREKA_CLIENT_ENABLED=false` để dịch vụ khởi động với cấu hình mặc định.

---

## 4. Quy trình khởi chạy
1. **Chuẩn bị JDK & Build**  
   - Sử dụng Temurin/OpenJDK 21 (`JAVA_HOME` phải trỏ đến JDK 21).  
   - Chạy `mvn clean package -DskipTests` để xác nhận build thành công.
2. **Khởi động hạ tầng Spring Cloud**  
   - Config Server và Eureka có thể chạy qua Docker Compose, Kubernetes hoặc dịch vụ managed. Ví dụ nhanh bằng Spring Cloud sample:  
     ```bash
     docker run --rm -p 8888:8888 --name config-server yourorg/config-server:latest
     docker run --rm -p 8761:8761 --name eureka-server yourorg/eureka-server:latest
     ```
   - Đảm bảo hai endpoint truy cập được từ các dịch vụ.
3. **Chạy các dịch vụ CoreBank**  
   ```bash
   SPRING_PROFILES_ACTIVE=lab-java21,lab-cloud \
   java -jar target/corebank-orchestrator-0.0.1-SNAPSHOT.jar
   ```
   - Lặp lại cho `corebank-payment`, `corebank-investigation`, `corebank-payment-stream`.  
   - Có thể sử dụng Docker Compose/Kubernetes manifest để tự động inject biến môi trường.
4. **Xác nhận đăng ký**  
   - Truy cập `http://localhost:8761` để kiểm tra các service đã xuất hiện với tên tương ứng.  
   - Kiểm tra actuator endpoint `GET /actuator/health` của từng service (`curl -u user:password http://<service-host>:<port>/actuator/health`).
5. **Kiểm thử liên thông**  
   - Gọi `POST /api/v1/payments` lên service Payment.  
   - Xác nhận Orchestrator/Investigation truy vấn status thành công (đã thông qua load balancer).

---

## 5. Cấu hình bổ sung
- **Config Server**: đặt các file cấu hình `application-lab-cloud.yml` trong repo config central để override Kafka, Redis, database cho môi trường cloud thực tế.
- **TLS & Auth**: khi triển khai production, cấu hình thêm `spring.cloud.config.username/password`, `eureka.client.serviceUrl.defaultZone` dùng HTTPS.
- **Observability**: duy trì Prometheus scrape qua `prometheus` actuator, hoặc bổ sung Zipkin/OTel nếu cần trace phân tán.

---

## 6. Xử lý sự cố thường gặp
| Hiện tượng | Nguyên nhân khả dĩ | Cách khắc phục |
|------------|-------------------|----------------|
| Service khởi động báo `release version 21 not supported` | JVM không phải JDK 21 | Cài JDK 21 và cập nhật `JAVA_HOME`. |
| Không đăng ký được vào Eureka | Endpoint sai/Tường lửa chặn | Kiểm tra `EUREKA_CLIENT_SERVICE_URL`, network hoặc bật log `logging.level.com.netflix.discovery=DEBUG`. |
| Investigation không gọi được Payment | Discovery chưa bật hoặc thiếu DNS | Kiểm tra profile, đảm bảo `SPRING_CLOUD_DISCOVERY_ENABLED=true` và payment service đã đăng ký. |
| Fail lấy cấu hình từ Config Server | Config Server down hoặc credential sai | Kiểm tra `SPRING_CLOUD_CONFIG_URI`, bật log `logging.level.org.springframework.cloud=DEBUG`, thử `curl` trực tiếp. |

---

## 7. Tài liệu tham khảo
- [Spring Cloud 2023.0.x Release Train](https://spring.io/projects/spring-cloud)  
- [Netflix Eureka Documentation](https://github.com/Netflix/eureka)  
- [Spring Cloud LoadBalancer Reference](https://docs.spring.io/spring-cloud-commons/docs/current/reference/html/#spring-cloud-loadbalancer)  
- Tài liệu kiến trúc chi tiết Java 21: `docs/corebank_payment_lab_java21_detailed.md`
