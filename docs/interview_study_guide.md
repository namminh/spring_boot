# CoreBank Payment Lab Interview Study Guide

## 1. Elevator Pitch
- **Context:** A Spring Boot 3.x lab that emulates CoreBank payment orchestration on top of Oracle Tuxedo and Oracle DB, letting squads experiment without touching production systems.
- **Core goals:** Decouple orchestration logic from legacy Pro*C, validate the outbox→Kafka pattern, and prepare observability + DevOps practices before migrating to the real estate.
- **Key differentiator:** Hybrid integration of modern Java microservices with a mock (eventually real) Tuxedo domain while preserving high performance, resilience, and compliance.

## 2. Architecture Talking Points
- **Services:** Payment orchestrator, Monitoring aggregator, Investigation case management; each Spring Boot app runs independently.
- **Tuxedo adapter:** Mock client today, pluggable Jolt/ATMI implementation later; design allows latency simulation and retry policies via Resilience4j.
- **Outbox pattern:** H2/Postgres/Oracle store includes `outbox_events`; scheduler forwards events to Kafka topics `payments.txn.completed|failed` when enabled.
- **Deployment vision:** Lab via shell scripts, production via Kubernetes + GitOps (ArgoCD) with Oracle Tuxedo on dedicated VMs.

## 3. Technical Deep Dives to Master
- JVM tuning (G1, heap sizing, GC logging) and Spring Boot thread/connection pools for low-latency REST flows.
- Transaction idempotency and retry orchestration to avoid double-posting payments.
- Schema design for payment, attempts, outbox tables; index strategy for monitoring queries.
- Kafka producer configuration, schema evolution, and backlog handling when brokers are down.
- Observability stack: Actuator metrics, custom meters for adapter latency/outbox backlog, structured logging, OpenTelemetry tracing.

## 4. Sample Interview Questions & Suggested Angles
1. **"How does the lab ensure payments remain consistent under retries?"** → Describe idempotent references, unique constraints, state checks before adapter calls, and outbox confirmation.
2. **"What changes are needed to replace the mock Tuxedo client with the real integration?"** → Cover connection pooling, timeout alignment with DMCONFIG, error mapping, and performance testing.
3. **"How would you scale the payment service for 300 TPS?"** → Discuss JVM/CPU sizing, Hikari pool tuning, HPA, partitioning Kafka topics, and database indexing.
4. **"How is resilience achieved when Kafka is unavailable?"** → Explain outbox backlog monitoring, delayed relay, alert thresholds, and manual replay options.
5. **"What security controls are enforced in this architecture?"** → Reference TLS, Vault-managed secrets, audit logging, compliance requirements (PCI DSS, NHNN).

## 5. Scenario Practice
- **Incident response:** Payment latency spikes; walk through checking JVM metrics, adapter latency histogram, DB locks, and fallback to circuit breaker.
- **DR drill:** Oracle primary fails; explain switching to standby, replaying outbox, validating reconciliation in Monitoring service.
- **Feature extension:** Adding fraud analytics consumer; outline topic subscription, schema contract, and deployment pipeline adjustments.

## 6. Coding & Design Refreshers
- Spring Boot REST controllers, validation, exception handling.
- Resilience4j configuration (retry, time limiter, circuit breaker) with annotations and programmatic APIs.
- JPA/Hibernate batch operations and transaction boundaries.
- Designing high-level sequence diagrams for payment flow and event propagation.

## 7. Communication Checklist
- Link answers to business impact (faster settlement, reliable reporting, reduced dependency on legacy).
- Highlight leadership roles: RFC authoring, cross-team alignment, mentoring engineers on observability and resilience.
- Prepare stories about incident resolution, migration spikes, and collaboration with Oracle/Tuxedo SMEs.

## 8. Mock Interview Plan
1. **Architecture walkthrough (15 min):** Present the system diagram and narrate key decisions.
2. **Deep-dive drill (20 min):** Focus on Tuxedo adapter + outbox integration; anticipate follow-up on latency and failure handling.
3. **Behavioral segment (15 min):** Prepare STAR examples on leading modernization, handling production incidents, mentoring teams.
4. **System design prompt (30 min):** Practice designing an end-to-end payment processing platform that includes external bank integration, fraud checks, and reporting.

Keep refining the stories with metrics (latency p95, error rate, throughput) and align them with Principal-level expectations: strategic influence, enterprise integration expertise, and measurable business outcomes.
