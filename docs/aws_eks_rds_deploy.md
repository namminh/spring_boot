# Triển khai CoreBank Payment Lab trên AWS (EKS + RDS)

Tài liệu này hướng dẫn từng bước dựng hạ tầng AWS bằng Terraform và triển khai các microservice (payment, monitoring, investigation) lên Amazon EKS, sử dụng Oracle RDS làm cơ sở dữ liệu.

---

## 1. Chuẩn bị
- **Tài khoản AWS** với quyền tạo VPC, IAM, EKS, RDS, S3.
- **Công cụ**: Terraform ≥ 1.4, kubectl, AWS CLI, Helm, Docker (optional), jq.
- **Repository**: đã clone lab (`infra/terraform` chứa Terraform baseline).
- **S3 State**: Tạo sẵn S3 bucket để lưu Terraform state (ví dụ `your-tf-state-bucket`).

### 1.1 Cấu hình AWS CLI
```bash
aws configure
# nhập Access Key, Secret Key, region (ví dụ ap-southeast-1)
```

### 1.2 Backend Terraform
Trong `infra/terraform/main.tf`, chỉnh `backend "s3"`:
```hcl
backend "s3" {
  bucket = "your-tf-state-bucket"
  key    = "corebank-payment/terraform.tfstate"
  region = "ap-southeast-1"
}
```

---

## 2. Dựng hạ tầng bằng Terraform
1. **Chuyển vào thư mục**
   ```bash
   cd infra/terraform
   ```
2. **Tạo file `terraform.tfvars`** (ví dụ):
   ```hcl
   aws_region = "ap-southeast-1"
   db_master_username = "corebank_admin"
   db_master_password = "StrongPassw0rd!"
   environment = "staging"
   owner = "corebank-team"
   ```
3. **Khởi tạo Terraform**
   ```bash
   terraform init
   ```
4. **Xem trước kế hoạch**
   ```bash
   terraform plan -out=tfplan
   ```
5. **Áp dụng**
   ```bash
   terraform apply tfplan
   ```
   Hạ tầng tạo gồm: VPC + subnets, EKS cluster, EKS node group, Oracle RDS (HA, subnet group, SG).
6. **Lưu ý bảo mật**: RDS bật encryption, multi-AZ, security group chỉ cho phép EKS truy cập.

Sau khi apply, Terraform output cung cấp cluster endpoint, CA data, RDS endpoint.

---

## 3. Kết nối EKS
1. **Cập nhật kubeconfig**
   ```bash
   aws eks update-kubeconfig \
     --name corebank-lab-eks \
     --region $(terraform output -raw aws_region)
   ```
2. **Kiểm tra node**
   ```bash
   kubectl get nodes
   ```

---

## 4. Chuẩn bị RDS Oracle
1. **Lấy endpoint**
   ```bash
   terraform output -raw rds_endpoint
   ```
2. **Tạo schema/tables**: dùng SQL*Plus hoặc công cụ (bạn có thể xuất script từ H2). Ví dụ sử dụng `sqlplus`:
   ```bash
   sqlplus corebank_admin@"(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=<rds-endpoint>)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=COREBANK)))"
   ```
   - Tạo schema `PAYMENT`, `INVESTIGATION`.
   - Chạy script `docs/sql/corebank_oracle_tables.sql` nếu bạn tạo (cần viết script tạo bảng `payments`, `outbox_events`, `investigation_case`).
3. **Tạo user ứng dụng** (ví dụ `orchestrator`), cấp quyền cần thiết.

---

## 5. Build & Push Docker Images
1. **Build image** (payment, monitoring, investigation). Tạo Dockerfile (ví dụ `Dockerfile.payment`, `Dockerfile.monitoring`, `Dockerfile.investigation`).
   ```bash
   docker build -t <registry>/corebank/payment:<tag> -f Dockerfile.payment .
   docker push <registry>/corebank/payment:<tag>
   # lặp lại cho monitoring, investigation
   ```
   (Có thể dùng GitLab CI/Kaniko theo `docs/ci_cd_process.md`).

---

## 6. Triển khai lên EKS
### 6.1 Namespace & Secrets
```bash
kubectl create namespace corebank
kubectl create secret generic payment-db \
  --namespace corebank \
  --from-literal=url="jdbc:oracle:thin:@//<rds-endpoint>:1521/COREBANK" \
  --from-literal=username=orchestrator \
  --from-literal=password='StrongPassw0rd!' \
  --from-literal=driver="oracle.jdbc.OracleDriver"
# tạo secret tương tự cho investigation nếu dùng schema riêng
```

### 6.2 Redis/Kafka (tuỳ chọn)
- Dùng Amazon ElastiCache / MSK hoặc self-managed (deploy Helm chart). Cập nhật service env.

### 6.3 Deployment YAML
Tạo file `k8s/payment-deployment.yaml`, `k8s/monitoring-deployment.yaml`, `k8s/investigation-deployment.yaml` (kèm Service, ConfigMap). Ví dụ rút gọn cho payment:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment-service
  namespace: corebank
spec:
  replicas: 3
  selector:
    matchLabels:
      app: payment-service
  template:
    metadata:
      labels:
        app: payment-service
    spec:
      containers:
        - name: payment
          image: <registry>/corebank/payment:<tag>
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_DATASOURCE_URL
              valueFrom:
                secretKeyRef:
                  name: payment-db
                  key: url
            - name: SPRING_DATASOURCE_USERNAME
              valueFrom:
                secretKeyRef:
                  name: payment-db
                  key: username
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: payment-db
                  key: password
            - name: SPRING_DATASOURCE_DRIVER_CLASS_NAME
              valueFrom:
                secretKeyRef:
                  name: payment-db
                  key: driver
            - name: SPRING_SECURITY_USER_NAME
              value: orchestrator
            - name: SPRING_SECURITY_USER_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: payment-basic
                  key: password
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 20
            periodSeconds: 10
```
Triển khai bằng `kubectl apply -f k8s/`.

### 6.4 Service & Ingress
- Tạo `Service` loại ClusterIP cho mỗi app.
- Dùng Ingress/NLB/ALB (AWS Load Balancer Controller) để expose ra ngoài.
- Ví dụ Ingress (NLB): map `/api/v1/payments` -> payment service, `/api/v1/monitoring` -> monitoring, `/api/v1/investigations` -> investigation.

---

## 7. Kafka Streams & Eventing
- Nếu dùng MSK, cập nhật `KAFKA_BOOTSTRAP_SERVERS` khớp MSK bootstrap.
- Triển khai `PaymentEventStreamApplication` dưới Deployment khác khi cần.

---

## 8. Observability
- Cài Prometheus/Grafana (Helm chart kube-prometheus-stack) trong cluster để scrape `/actuator/prometheus`.
- Xây dashboard hiển thị metrics payment/investigation.
- Thiết lập CloudWatch Logs bằng Fluent Bit daemonset để ship log `NAMNM`.

---

## 9. CI/CD Gợi ý
- GitLab CI hoặc Jenkins pipeline:
  1. Build & test (`mvn clean verify`).
  2. Build/push Docker.
  3. `kubectl apply` hoặc Helm upgrade sử dụng kubeconfig CI.
  4. Smoke test (`scripts/test_payment_flow.sh`, `scripts/test_monitoring_flow.sh`, `scripts/test_investigation_flow.sh` với `BASE_URL` trỏ load balancer).
- Dùng ArgoCD/GitOps: commit manifest/Helm chart, ArgoCD sync cluster.

---

## 10. Rollback & Mở rộng
- **Rollback**: giữ lại image tag cũ và re-run deploy. Với Helm dùng `helm rollback`.
- **AutoScaling**: bật HPA dựa trên CPU (e.g. target 70%).
- **Backup**: RDS backup tự động (7 ngày); snapshot thủ công trước thay đổi lớn. Xem xét cross-region read replica.
- **Security**: 
  - IRSA cho pod truy cập AWS resources (Secrets Manager, S3).
  - Network policies trong namespace `corebank`.
  - ACM để terminate TLS tại ALB/ingress.

---

## 11. Checklist triển khai thành công
- [ ] Terraform apply không lỗi, outputs đầy đủ.
- [ ] kubectl get nodes/pods đều `Ready`.
- [ ] Service endpoint (ALB/NLB) trả về kết quả cho API payment/monitoring/investigation (HTTP 200/201).
- [ ] RDS data nhận transaction/investigation (kiểm tra qua SQL client).
- [ ] Prometheus/Grafana hiển thị metrics; alert/Slack (monitor.groovy) hoạt động.
- [ ] Pipeline CI/CD chạy xanh, smoke test pass sau deploy.

---

## 12. Next Steps
- Hoàn thiện Helm chart + ArgoCD để chuẩn hóa deploy.
- Áp dụng Terraform module IAM/OIDC để cấp quyền IRSA.
- Tích hợp SSM Parameter Store / Secrets Manager thay vì plain Secret.
- Bổ sung runbook ITIL (incident/change/problem) với chi tiết AWS operations.

