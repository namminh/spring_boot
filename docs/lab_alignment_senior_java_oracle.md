# Senior Java (Oracle) Role Alignment with CoreBank Payment Lab

This document maps the job description requirements for the "Senior Java (Oracle)" position to concrete assets and practices inside the CoreBank Payment Lab. It can be used as interview preparation, capability evidence, or onboarding reference.

---

## 1. Technical Leadership & Incident Response
- **Relevant Lab Assets**
  - `src/main/java/com/corebank/payment/application/PaymentOrchestratorService.java` – shows end-to-end orchestration with clear logging (`NAMNM SRV`) and error handling for Tuxedo adapter failures.
  - `src/main/java/com/corebank/orchestrator/orchestrator/MonitoringOrchestrator.java` and `src/main/java/com/corebank/orchestrator/api/MonitoringController.java` – capture health snapshots and alert workflows, now logging with `NAMNM ORCH` prefix.
- **Practices Demonstrated**
  - Root-cause ready logging for incidents; resilience through Resilience4j retry (config at `src/main/resources/application.yml`).
  - Manual alert pipeline and event publishing for incident management.

## 2. Java Backend & Oracle/Relational Databases
- **Lab Fit**
  - Spring Boot 3-based service (`com.corebank.payment` package) with layered architecture: API, application, domain, infrastructure.
  - Persistence via JPA repositories (`src/main/java/com/corebank/payment/infrastructure/persistence`) with RDBMS-agnostic setup; defaults to H2 but easily swapped for Oracle/PostgreSQL using environment variables.
- **Evidence Points**
  - Entities (`PaymentEntity`), repositories, and service logic align with enterprise backend modules.
  - Outbox pattern and transaction handling mimic real Oracle-backed systems.

## 3. AWS Cloud Operations
- **Applicable Patterns**
  - Container-ready Spring Boot app; easy to deploy on EKS/ECS with health endpoints (`/actuator/*`) exposed.
  - Externalized configuration via environment variables for DB, Kafka, Redis – matches AWS Secrets Manager and Parameter Store usage.
- **Talking Points**
  - Describe running payment service on Amazon EKS + RDS (Oracle), using CloudWatch, ALB, and IAM roles (see previous guidance).
  - Mention readiness to add S3/Kinesis integration through existing outbox/events modules.

## 4. Monitoring, Incident Response, Optimization
- **Monitoring**: Actuator metrics, `/api/v1/payments/metrics/status-count`, and the new `scripts/monitor.groovy` for automated FAILED-count alerts (Slack-ready).
- **Incident Response**: Runbook-friendly logs (`NAMNM SRV/ORCH`, status snapshots) and `scripts/test_monitoring_flow.sh` to verify recovery.
- **Optimization**: Redis caching, retry tuning (`resilience4j.retry`), and `scripts/test_payment_flow.sh` for regression/performance smoke.

## 5. CI/CD, DevOps, Automation
- **Pipeline Demo**: Sample GitLab CI sections provided earlier – build, smoke, package. Aligns with requirement to operate/improve pipelines.
- **Automation**: Groovy scripts for monitoring (Slack alert) and Jenkins pipeline examples; `scripts/start_all.sh` automates local environment start.
- **DevOps Tooling**: Docker/Kubernetes readiness (Spring Boot actuator, stateless services), Kafka integration toggles, Redis cache.

## 6. ITIL & Managed Services Practices
- **Evidence**
  - Monitoring/alerting model with severity mapping in `MonitoringOrchestrator` mirrors Incident/Problem workflows.
  - Documentation in `docs/corebank_tuxedo_payment_solution.md` covers operations, deployment, and runbook guidance.
  - Scripts facilitate routine checks (`test_monitoring_flow.sh`, `monitor.groovy`), supporting change & incident processes.

## 7. Groovy Scripting
- **In Repo**: `scripts/monitor.groovy` – automation example for alerting.
- **Extensible**: Use Groovy for Jenkins pipelines, data migrations, or integration glue leveraging JVM libraries.

## 8. Supporting Credentials & Experience
- **Possible Talking Points**
  - Demonstrate 7+ years: highlight orchestrator design, Tuxedo adapter integration path (`docs/corebank_tuxedo_payment_solution.md`).
  - Show domain knowledge in payments, incident leadership, managed services via lab architecture sections.

## 9. Interview / Documentation Usage
- Walk recruiters through lab repo: start both services with `scripts/start_all.sh`, show metrics/monitor endpoints, run smoke scripts.
- Discuss how the lab replicates production-grade concerns (observability, outbox, retries, CI/CD automation).
- Prepare stories on AWS deployment, Groovy automation, ITIL-based response using lab artifacts as proof.

---

**Next Steps**
1. Tailor `monitor.groovy` to actual Slack workspace and schedule it (cron or Jenkins) as a live demo.
2. Practice running `scripts/test_payment_flow.sh` and `scripts/test_monitoring_flow.sh` while narrating incident response scenarios.
3. Update résumé/portfolio to reference this lab as a managed-services playground supporting Oracle-based systems.
