# Hướng dẫn triển khai CoreBank Payment Lab lên Cloud

Tài liệu này mô tả từng bước triển khai lớp API (Payment, Orchestrator, Investigation, Stream) của CoreBank Payment Lab lên môi trường đám mây. Hướng dẫn bao gồm đóng gói ứng dụng, chuẩn bị hạ tầng Spring Cloud, thiết lập platform (Kubernetes hoặc container service tương đương), bảo mật, giám sát và quy trình CI/CD.

---

## 1. Tổng quan kiến trúc triển khai
```
                  ┌──────────────────────┐
                  │ Spring Cloud Config  │
                  │ (Config Server)      │
                  └────────┬─────────────┘
                           │
                  ┌────────▼──────────┐
                  │ Eureka Server      │
                  │ (Service Registry) │
                  └────────┬───────────┘
                           │
               ┌───────────┴───────────────┐
               │ Kubernetes / ECS / AKS    │
               │  ├─ corebank-payment      │
               │  ├─ corebank-orchestrator │
               │  ├─ corebank-investigation│
               │  └─ corebank-payment-stream│
               └───────────┬───────────────┘
                           │
                     Oracle Tuxedo
                   (Legacy Pro*C svc)
```

---

## 2. Điều kiện tiên quyết
- **Môi trường build**: JDK Temurin 21, Maven 3.9+.  
- **Registry container**: GitHub Packages, ECR, ACR, GCR, hoặc registry nội bộ.  
- **Hạ tầng Spring Cloud**: Config Server và Eureka Server (có thể chạy độc lập trên VM hoặc container).  
- **Kết nối mạng**: VPN/VPC tới zone chạy Oracle Tuxedo để các service Java gọi được gateway.  
- **Secret management**: Vault, AWS Secrets Manager, Azure Key Vault, hoặc Kubernetes Secret.  
- **Monitoring stack**: Prometheus/Grafana, ELK/EFK, OpenTelemetry Collector (khuyến nghị).

---

## 3. Đóng gói & Container hóa
1. **Build jar**
   ```bash
   ./mvnw clean package -DskipTests
   ```
   Artefact: `target/corebank-orchestrator-0.0.1-SNAPSHOT.jar` (tương tự cho các module khác).

2. **Tạo Dockerfile tối giản**
   ```dockerfile
   FROM eclipse-temurin:21-jre
   WORKDIR /app
   COPY target/corebank-orchestrator-0.0.1-SNAPSHOT.jar app.jar
   ENV SPRING_PROFILES_ACTIVE=lab-java21,lab-cloud
   ENTRYPOINT ["java","-jar","/app/app.jar"]
   ```

3. **Build & push image**
   ```bash
   docker build -t registry.example.com/corebank/corebank-orchestrator:1.0.0 .
   docker push registry.example.com/corebank/corebank-orchestrator:1.0.0
   ```
   Lặp lại cho payment, investigation, stream (có thể dùng multi-stage build hoặc Jib).

---

## 4. Chuẩn bị Spring Cloud Config & Eureka
### 4.1 Config Server
- Tạo repo cấu hình (ví dụ `corebank-config`) chứa `application-lab-cloud.yml`, `corebank-payment-lab.yml`, v.v.
- Deploy Config Server (Spring Boot) trỏ tới repo đó (Git HTTPS hoặc S3).  
- Mở port `8888`, bật TLS và basic-auth trong production.

### 4.2 Eureka Server
- Deploy Spring Cloud Netflix Eureka (single node hoặc cluster 3 node).  
- Cấu hình `eureka.client.registerWithEureka=false` và `fetchRegistry=false` cho server.  
- Mở port `8761`, enable TLS theo nhu cầu.

### 4.3 Kiểm tra
- Gọi `curl http://<config-server>:8888/application/default` xem trả cấu hình.  
- Truy cập `http://<eureka-server>:8761` confirm trạng thái up.

---

## 5. Thiết lập môi trường runtime
### 5.1 Kubernetes (EKS/AKS/GKE/On-prem)
- **Namespace**: `kubectl create namespace corebank-lab`.
- **Secret**:  
  ```bash
  kubectl -n corebank-lab create secret generic corebank-config \
    --from-literal=SPRING_CLOUD_CONFIG_URI=https://config.example.com \
    --from-literal=EUREKA_CLIENT_SERVICE_URL=https://eureka.example.com/eureka \
    --from-literal=SPRING_SECURITY_USER_PASSWORD=changeme
  ```
- **Deployment template (orchestrator)**
  ```yaml
  apiVersion: apps/v1
  kind: Deployment
  metadata:
    name: corebank-orchestrator
    namespace: corebank-lab
  spec:
    replicas: 2
    selector:
      matchLabels:
        app: corebank-orchestrator
    template:
      metadata:
        labels:
          app: corebank-orchestrator
      spec:
        containers:
          - name: orchestrator
            image: registry.example.com/corebank/corebank-orchestrator:1.0.0
            ports:
              - containerPort: 8081
            env:
              - name: SPRING_PROFILES_ACTIVE
                value: lab-java21,lab-cloud
              - name: SPRING_CLOUD_CONFIG_URI
                valueFrom:
                  secretKeyRef:
                    name: corebank-config
                    key: SPRING_CLOUD_CONFIG_URI
              - name: EUREKA_CLIENT_SERVICE_URL
                valueFrom:
                  secretKeyRef:
                    name: corebank-config
                    key: EUREKA_CLIENT_SERVICE_URL
              - name: SPRING_SECURITY_USER_PASSWORD
                valueFrom:
                  secretKeyRef:
                    name: corebank-config
                    key: SPRING_SECURITY_USER_PASSWORD
            readinessProbe:
              httpGet:
                path: /actuator/health/readiness
                port: 8081
              initialDelaySeconds: 20
              periodSeconds: 10
            livenessProbe:
              httpGet:
                path: /actuator/health/liveness
                port: 8081
              initialDelaySeconds: 60
              periodSeconds: 30
        nodeSelector:
          node.kubernetes.io/instance-type: app
  ```
- **Service/Ingress**: expose qua `ClusterIP` + Ingress hoặc Service mesh (Istio/Linkerd). Bật mTLS nếu mesh hỗ trợ.

### 5.2 AWS ECS/Fargate (ví dụ)
- Tạo Task Definition cho từng service (container port 8080/8081/8082).  
- Thiết lập Environment / Secrets (SSM Parameter Store hoặc Secrets Manager).  
- Service chạy trong private subnet, ALB/Public LB cho các endpoint cần public.  
- Đảm bảo security group cho phép call tới Config & Eureka và network tới Tuxedo.

### 5.3 Azure App Service / Container Apps
- Upload image vào ACR.  
- Tạo Web App for Containers, set environment variables `SPRING_PROFILES_ACTIVE`, `SPRING_CLOUD_CONFIG_URI`, `EUREKA_CLIENT_SERVICE_URL`.  
- Cấu hình VNet Integration để gọi vào Tuxedo subnet.

---

## 6. Bảo mật & Secret
- Sử dụng TLS cho mọi traffic (Config, Eureka, API ingress).  
- Quản lý credential qua secret manager, không hardcode trong image.  
- Bật Spring Security HTTP Basic hoặc OAuth2 (Keycloak, Cognito).  
- Sử dụng mTLS hoặc IP allow-list giữa cluster và Tuxedo.  
- Rotate mật khẩu định kỳ, theo dõi audit log.

---

## 7. Observability & Alert
- **Metrics**: scrape `/actuator/prometheus`, theo dõi `jvm.threads.virtual.count`, `payment.outbox.pending`.  
- **Logging**: ship log (JSON) lên ELK/CloudWatch/Stackdriver.  
- **Tracing**: bật OpenTelemetry exporter nếu cần trace tới Tuxedo gateway.  
- **Health**: configure Kubernetes `readiness`/`liveness`, ECS health check, App Service health check endpoint.

---

## 8. Quy trình CI/CD đề xuất
1. **CI Pipeline**
   - Bước 1: `mvn test`.  
   - Bước 2: `mvn package`.  
   - Bước 3: Build & push Docker image (tag theo commit SHA).  
   - Bước 4: Scan image (Trivy/Grype).  
   - Bước 5: Publish Helm chart/Kustomize overlay nếu dùng.
2. **CD Pipeline**
   - Deploy Config/Eureka (nếu thay đổi).  
   - Apply manifest Kubernetes (kubectl/Helm ArgoCD).  
   - Smoke test: gọi `/actuator/health`, `POST /api/v1/payments`.  
   - Promote qua môi trường (dev → sit → prod) bằng GitOps hoặc pipeline multi-stage.  
   - Rollback: giữ lại revision/helm history, `kubectl rollout undo`.

---

## 9. Kiểm thử sau triển khai
- **Verification script**
  ```bash
  curl -u orchestrator:changeme https://api.example.com/actuator/health
  curl -u orchestrator:changeme https://api.example.com/api/v1/payments/metrics/status-count
  ```
- **Integration test**: chạy `scripts/test_*` với endpoint cloud.  
- **Monitoring**: check dashboard latency, error rate, queue backlog.  
- **Chaos test**: tắt một replica, verify automatic recovery & load balancer behavior.

---

## 10. Xử lý sự cố nhanh
| Hiện tượng | Khả năng | Hướng xử lý |
|------------|----------|-------------|
| Service không đăng ký Eureka | Wrong URL / TLS | Kiểm tra env `EUREKA_CLIENT_SERVICE_URL`, certificate truststore. |
| Config Server không trả config | Repo thiếu file / auth lỗi | Kiểm tra log Config Server, verify credential. |
| Latency cao | Network tới Tuxedo / thread pool nhỏ | Tăng connection pool, bật metrics, tối ưu virtual threads. |
| Pod crash loop | Secret thiếu / Config lỗi | Xem log, đảm bảo env var đầy đủ, fallback profile khi cần. |
| Không truy cập được DB | Security group/VNet chưa mở | Mở firewall hoặc dùng private endpoint. |

---

## 11. Tham khảo thêm
- `docs/corebank_payment_lab_cloud.md`: cấu hình Spring Cloud chi tiết.  
- `docs/cloud_native_legacy_integration.md`: playbook tích hợp cloud-native ↔ legacy.  
- `docs/hybrid_java_proc_tuxedo_architecture.md`: kiến trúc hybrid Java & Pro*C với Tuxedo.  
- Spring Cloud documentation, CNCF Kubernetes best practices, Oracle Tuxedo deployment guide.
