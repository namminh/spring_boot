# Quy trình CI/CD cho CoreBank Payment Lab

Tài liệu mô tả pipeline CI/CD mẫu để build, kiểm thử, và triển khai hai microservice trong lab: **Payment Orchestrator** và **Monitoring Orchestrator**, kèm processor Kafka Streams.

---

## 1. Mục tiêu
- Đảm bảo mọi thay đổi đều qua các bước build, test, smoke trước khi phát hành.
- Tự động hoá đóng gói artifact và triển khai (rolling/canary) mà không làm gián đoạn dịch vụ.
- Cung cấp điểm kiểm soát rõ ràng để rollback hoặc mở rộng.

## 2. Chuỗi pipeline chuẩn

### 2.1 Giai đoạn CI (Continuous Integration)
1. **Checkout**: Git fetch code, cache `.m2` để giảm thời gian build.
2. **Build & Unit Test** (`mvn -B -ntp clean verify`) trên JDK 21 (Temurin recommend).
   - Chạy toàn bộ test JUnit, kiểm tra cấu hình (bao gồm profile `test`).
   - Sinh artifact `target/orchestrator-0.0.1-SNAPSHOT.jar`.
3. **Static Analysis** (tuỳ chọn): SonarQube, Dependency Check.
4. **Smoke API**: dùng script có sẵn
 - `scripts/test_payment_flow.sh` xác nhận endpoint `/api/v1/payments` và metrics hoạt động.
 - `MONITOR_ALERT_SEVERITY=CRITICAL scripts/test_monitoring_flow.sh` xác nhận `/api/v1/monitoring` hoạt động.
  - `scripts/test_investigation_flow.sh` xác nhận dịch vụ tra soát `/api/v1/investigations` hoạt động.
5. **Publish Artifact**: lưu jar hoặc build Docker image nội bộ (stage tiếp nhận).

### 2.2 Giai đoạn CD (Continuous Delivery/Deployment)
1. **Build Docker Image** (Kaniko/Jib):
   - Payment và Monitoring microservice tách image.
   - Gắn tag theo `CI_COMMIT_SHORT_SHA` hoặc version.
2. **Push Registry**: đẩy image lên GitLab Registry/ACR/ECR.
3. **Triển khai**:
   - **Rolling update** trên Kubernetes/ECS: mỗi service có Deployment, readiness probe `/actuator/health` hoặc `/actuator/health/readiness`.
   - Hoặc **Blue/Green**: deploy stack mới, kiểm thử, chuyển traffic.
   - **Canary** (tuỳ chọn): route 5% traffic bằng Istio/NGINX Ingress, quan sát metrics.
4. **Post-deploy Check**:
   - Tự động gọi lại `scripts/test_payment_flow.sh`/`scripts/test_monitoring_flow.sh` với base URL staging/prod.
   - Kiểm tra dashboard (Prometheus/Grafana) và log `NAMNM` để phát hiện bất thường.
5. **Notify**: gửi Slack/email trạng thái deploy.

## 3. Biến môi trường quan trọng
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `SPRING_SECURITY_USER_NAME`, `SPRING_SECURITY_USER_PASSWORD`
- `SPRING_REDIS_HOST`, `SPRING_REDIS_PORT`
- `PAYMENT_EVENTS_KAFKA_ENABLED`, `KAFKA_BOOTSTRAP_SERVERS`
- `PAYMENT_STREAMS_ENABLED`, `PAYMENT_OUTBOX_POLL_INTERVAL_MS`
- Thông tin registry (`CI_REGISTRY`, `CI_REGISTRY_IMAGE`, credentials)

## 4. Ví dụ `.gitlab-ci.yml`
```yaml
stages:
  - build
  - smoke
  - package

build:
  stage: build
  image: maven:3.9.6-eclipse-temurin-21
  cache:
    paths: [ .m2/repository ]
  script:
    - mvn -B clean verify
  artifacts:
    paths:
      - target/orchestrator-0.0.1-SNAPSHOT.jar
    expire_in: 1 day

smoke_payment:
  stage: smoke
  image: curlimages/curl:8.8.0
  needs: ["build"]
  before_script:
    - apk add --no-cache jq bash
  script:
    - ./scripts/test_payment_flow.sh

smoke_monitoring:
  stage: smoke
  image: curlimages/curl:8.8.0
  needs: ["build"]
  before_script:
    - apk add --no-cache jq bash
  script:
    - MONITOR_ALERT_SEVERITY=CRITICAL ./scripts/test_monitoring_flow.sh

smoke_investigation:
  stage: smoke
  image: curlimages/curl:8.8.0
  needs: ["build"]
  before_script:
    - apk add --no-cache jq bash
  script:
    - ./scripts/test_investigation_flow.sh

package_image:
  stage: package
  image: gcr.io/kaniko-project/executor:debug
  needs: ["build"]
  script:
    - /kaniko/executor \
        --context $CI_PROJECT_DIR \
        --dockerfile Dockerfile.payment \
        --destination $CI_REGISTRY_IMAGE/payment:${CI_COMMIT_SHORT_SHA}
    - /kaniko/executor \
        --context $CI_PROJECT_DIR \
        --dockerfile Dockerfile.monitoring \
        --destination $CI_REGISTRY_IMAGE/monitoring:${CI_COMMIT_SHORT_SHA}
  only: ["main"]
```

### 2.3 GitHub Actions mẫu
Repo đã kèm workflow `.github/workflows/ci.yml` dùng `actions/setup-java@v4` cài Temurin 21 và chạy `mvn -B -ntp test` cho mọi push/pull request.

### 2.4 Jenkins Pipeline mẫu
Thư mục `infra/jenkins/Jenkinsfile` cấu hình Jenkins Declarative pipeline sử dụng container `maven:3.9.6-eclipse-temurin-21`. Các bước chính:
- Khởi tạo cache `.m2` (tùy chọn).
- Chạy `mvn -B -ntp clean verify` với JDK 21.
- Lưu trữ artifact `target/*.jar` và báo cáo Surefire JUnit.
Gán pipeline cho agent/label có hỗ trợ Docker hoặc thay bằng agent cài sẵn Temurin 21.

## 5. Chiến lược triển khai
- **Kubernetes**: Deployments tách cho payment, monitoring, streams; config `maxUnavailable: 0`, liveness/readiness probe; HPA dựa trên CPU hoặc metric tùy chỉnh.
- **Blue/Green**: sử dụng ingress (Nginx/Istio) đổi routing, chạy smoke test trước khi flip.
- **Canary**: release manager quan sát metrics (`/actuator/metrics`, Prometheus) và log `NAMNM`.
- **Rollback**: giữ version trước (Docker tag, Helm release). Dùng `helm rollback` hoặc `kubectl rollout undo`.

## 6. Kiểm soát chất lượng & bảo mật
- SonarQube (code smell, coverage), DependencyCheck (CVE), SAST (GitLab/Snyk).
- Secret quản lý ở GitLab CI/CD variables, Vault hoặc AWS Secrets Manager.
- Tracing/Monitoring: Actuator + Prometheus, script `scripts/monitor.groovy` để cảnh báo FAILED.

## 7. Checklist trước deploy Production
- Database migration chạy trước (Liquibase/Flyway) và backwards compatible.
- Secrets valid, Redis/Kafka endpoint sẵn sàng.
- Smoke test pass trên staging.
- Runbook sự cố cập nhật (MonitoringOrchestrator snapshot status).
- Lập lịch deploy ngoài giờ cao điểm hoặc bật canary.

## 8. Công cụ hỗ trợ trong repo
- `scripts/start_all.sh`: khởi chạy cả payment & monitoring local.
- `scripts/start_streams.sh`: chạy Kafka Streams processor theo profile `streams`.
- `scripts/test_payment_flow.sh`, `scripts/test_monitoring_flow.sh`: smoke test thủ công hoặc tích hợp CI.
- `scripts/monitor.groovy`: giám sát FAILED count, gửi alert.

---

## 9. Mở rộng tương lai
- Tự động hóa chạy Gatling/k6 load test trước mỗi release lớn.
- Áp dụng GitOps (ArgoCD) để đồng bộ manifest.
- Tạo dashboard Grafana chuẩn hóa (latency, backlog, alert) để canary/blue-green quyết định nhanh.
- Tích hợp chatops (Slack bot) để trigger deploy và báo cáo kết quả pipeline.

Tài liệu này có thể điều chỉnh cho từng môi trường (dev/stg/prod) và mở rộng khi thêm service mới.
