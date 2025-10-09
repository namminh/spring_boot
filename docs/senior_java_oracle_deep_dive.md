# Senior Java (Oracle) Deep Dive

Tài liệu này khai thác chi tiết nội dung JD trong `Senior Java oracle.txt`, phân tích từng yêu cầu và gợi ý bằng chứng/hoạt động cho CoreBank Payment Lab.

---

## 1. Phạm vi trách nhiệm (Responsibilities)
### 1.1 Technical Leadership & Solution Delivery
- **Ý nghĩa**: dẫn dắt module backend phức tạp, đề xuất kiến trúc, kiểm soát chất lượng.
- **Hành động đề xuất**:
  - Trình bày kiến trúc lab (API → Orchestrator → Tuxedo adapter → Outbox/Kafka).
  - Giới thiệu service mới `com.corebank.investigation` để minh hoạ mở rộng domain tra soát.
  - Chuẩn hóa coding guide (log `NAMNM`, resilience pipeline, cache).
  - Review checklist: Idempotency, logging, error map, metrics.

### 1.2 Incident & Problem Management
- **Mục tiêu**: phản ứng và điều tra sự cố theo SLA.
- **Lab minh chứng**: log orchestrator/monitoring, script smoke (`test_payment_flow.sh`, `test_monitoring_flow.sh`), `monitor.groovy` cảnh báo Slack.
- **Runbook mẫu**: thu thập log `NAMNM SRV`, kiểm tra `/metrics/status-count`, xem snapshot `MonitoringOrchestrator`, khởi chạy `start_all.sh` cho lab reproduction.

### 1.3 Backend Implementation (Java + RDBMS)
- **Yêu cầu**: Java, Spring Boot, JDBC/JPA, RDBMS (Oracle/PostgreSQL/MySQL).
- **Lab alignment**:
  - `PaymentOrchestratorService` với Resilience4j, caching, outbox.
  - `PaymentEntity`/`PaymentRepository` (JPA) và profile DB trong `application.yml`.
  - Gợi ý PoC Oracle: thay `SPRING_DATASOURCE_*`, dùng `CallableStatement` hoặc `SimpleJdbcCall` (so sánh Pro*C vs Java).

### 1.4 AWS Cloud Operations
- **Terraform mẫu**: tham khảo `infra/terraform/main.tf` để dựng VPC + EKS + RDS nhanh.
- **JD**: deploy, config, manage app trên AWS đảm bảo HA & security.
- **Chiến lược**: 
  - EKS/ECS cho microservices; RDS Oracle; CloudWatch Logs/Alarms; ALB + ACM.
  - Mẫu Infrastructure as Code: Terraform (VPC, EKS, RDS), ArgoCD/GitLab CI pipeline.
  - Observability: Prometheus-on-EKS, AWS Managed Prometheus, CloudWatch dashboard.

### 1.5 Performance & Scalability
- **Công việc**: tuning, nâng cấp hiệu năng cho managed service.
- **Minh chứng**:
  - Redis cache TTL (CacheConfig) và tuning pool Hikari.
  - Benchmark script (Gatling/k6) vs metrics `/actuator/prometheus`.
  - Điều chỉnh `PAYMENT_OUTBOX_POLL_INTERVAL_MS`, `resilience4j` tham số.

### 1.6 Testing Automation
- **JD**: Unit & integration testing tự động.
- **Hiện trạng**: JUnit MockMvc test + script smoke.
- **Mở rộng**: Test container (Oracle/Postgres), contract test (Spring Cloud Contract), pipeline test stage.

### 1.7 CI/CD Ownership
- **JD**: vận hành & cải tiến pipeline.
- **Lab**: `docs/ci_cd_process.md`, script start/deploy, GitLab CI sample. 
- **Gợi ý**: SonarQube, dependency scanning, canary automation.

### 1.8 ITIL Process Alignment
- **JD**: phối hợp incident/change/problem theo ITIL.
- **Chuyển đổi**:
  - Mapping severity (MonitoringOrchestrator HEALTHY/WARNING/DEGRADED) ↔ Incident priority.
  - Change management: pipeline approvals, rollback plan.
  - Knowledge base: doc runbook, postmortem template.

### 1.9 Documentation & 24/7 Support
- **JD**: tài liệu kỹ thuật, quy trình vận hành, hướng dẫn sự cố.
- **Lab tài liệu**: `docs/corebank_tuxedo_payment_solution.md`, `docs/tong_hop_senior_java_oracle_lab.md`, `docs/ci_cd_process.md`.
- **Việc bổ sung**: SOP cho patch, security update, on-call schedule.

### 1.10 Groovy Automation (Optional)
- **Minh họa**: `scripts/monitor.groovy` (alert Slack), Jenkinsfile Groovy sample.
- **Ý tưởng thêm**: Groovy script migrate dữ liệu, trigger Jenkins, automation runbook.

---

## 2. Kỹ năng & Công nghệ (Technical Skills)
| Kỹ năng JD | Trạng thái trong lab | Đề xuất mở rộng |
|------------|----------------------|------------------|
| Java, OOP, Spring Boot | Có (service payment/monitoring) | Bổ sung module reactive hoặc gRPC nếu cần |
| RDBMS (Oracle/PostgreSQL/MySQL) | H2 mock; design JPA | Add profile PostgreSQL/Oracle (driver, schema migration) |
| AWS operations | Chưa triển khai thực | Thiết kế Terraform + EKS manifest demo |
| Monitoring & incident response | Actuator, MonitoringOrchestrator, scripts | Thêm Prometheus dashboard, Alertmanager |
| CI/CD tools (Jenkins, GitLab, Docker, Kubernetes) | Docs + scripts | Tạo Jenkinsfile, Helm chart thực tế |
| ITIL familiarity | Mapping log/severity | Soạn playbook incident/change |
| Groovy scripting | monitor.groovy | Jenkins pipeline Groovy demo |

---

## 3. Kinh nghiệm & Chứng chỉ
- **JD**: 7+ năm backend & managed service, production ownership.
- **Minh chứng**: nêu dự án trước, gắn với architecture diagram, performance tuning.
- **Chứng chỉ gợi ý**: AWS Solutions Architect, ITIL Foundation, Oracle Certified Professional (Java SE 11/17).

---

## 4. Kịch bản phỏng vấn & Demo
1. **Kiến trúc**: dùng `README.md` + sequence diagram, giải thích orchestrator + outbox + monitoring (làm slide nhanh).
2. **Sự cố giả lập**: mô tả scenario Tuxedo timeout → log `NAMNM SRV` → alert Slack → runbook.
3. **CI/CD**: trình bày pipeline trong `docs/ci_cd_process.md`, show GitLab config.
4. **AWS**: vẽ sơ đồ EKS + RDS + CloudWatch, nêu best practice (IRSA, security group, backup).
5. **Groovy/Automation**: demo `monitor.groovy` hoặc Jenkins Groovy snippet.

---

## 5. Kế hoạch nâng cấp lab theo JD
| Ưu tiên | Hạng mục | Mục tiêu |
|---------|----------|---------|
| P1 | Thêm profile Oracle/PostgreSQL + script schema | Match RDBMS real-case |
| P1 | Helm chart + ArgoCD manifest | Demo CD production-like |
| P2 | Prometheus + Grafana dashboard template | Monitoring/alert maturity |
| P2 | Jenkinsfile (Groovy) & GitLab pipeline song song | Chứng minh CI/CD đa nền tảng |
| P3 | Terraform mẫu cho AWS EKS & RDS | Cloud alignment |
| P3 | Postmortem template + runbook (incident/change) | ITIL alignment |

---

## 6. Tổng kết
JD "Senior Java (Oracle)" yêu cầu kỹ năng full-stack backend + vận hành. Lab hiện tại đáp ứng nhiều tiêu chí cốt lõi (orchestrator, monitoring, automation). Nên tiếp tục mở rộng vào phần triển khai thực (Oracle DB, AWS infra), nâng cấp pipeline, và chuẩn hóa tài liệu ITIL để hoàn thiện hồ sơ "expert".
