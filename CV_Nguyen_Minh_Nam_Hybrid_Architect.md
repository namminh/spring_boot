Nguyen Minh Nam (Mr Nam)
Hybrid Core Banking Architect | Oracle Tuxedo × Spring Boot Modernisation
Hanoi, Vietnam  |  namnmhp89@gmail.com  |  +84 90 292 3096  |  LinkedIn on request

PROFESSIONAL SUMMARY
- 13+ years architecting mission-critical payment platforms, orchestrating hybrid architectures that overlay Spring Boot/Kafka on top of Oracle Tuxedo + Pro*C without disrupting core ledgers.
- Bridge legacy reliability with cloud-native agility: design idempotent API gateways, transactional outbox patterns, and XA integrations that let the bank innovate while keeping Pro*C 2PC settlement untouched.
- Proven tribe lead and solution architect: roadmap definition, cross-functional mentoring, vendor coordination, and continuous delivery pipelines sustaining 99.97% SLA, MTTR 15 minutes.

VALUE HIGHLIGHTS
1. Hybrid Lindy Architecture: classify workloads into "Lindy core" (Tuxedo/Pro*C, Oracle RAC) vs "adaptive edge" (Spring Boot, Kafka, DWH) to stage safe modernisation waves.
2. Enterprise-Grade Reliability: combine resilience patterns (circuit breaker, bulkhead, retries) with proactive observability (Prometheus, OpenTelemetry, ELK) for fast incident response.
3. Delivery Leadership: lead 8-person squads through Agile cadences, own requirements-to-production lifespan, and synchronise releases with operations, compliance, and vendor teams.

CORE CAPABILITIES
- **Hybrid Integration:** Java/Spring Boot orchestrators, Tuxedo (ATMI/Jolt) adapters, Pro*C optimisations, legacy encapsulation, transactional outbox + CDC.
- **API & Data Platform:** REST/gRPC, OpenAPI/AsyncAPI, Kafka, Flink/Kafka Streams, Landing Zone → Data Mart, near-real-time reconciliation.
- **Performance & Reliability:** throughput tuning (3,600 TPS), XA transaction management, Redis caching, SLA/SLO governance, chaos & DR drills.
- **DevOps & Security:** GitLab CI, ArgoCD GitOps, Terraform, Ansible, Kubernetes (HPA, Istio), Vault/KMS, PCI DSS controls, automated compliance checks.
- **Leadership & Collaboration:** backlog refinement with PM/BA, code/design reviews, mentoring engineers transitioning from Pro*C to Spring Boot, incident postmortems.

FLAGSHIP HYBRID SOLUTION — CoreBank Tuxedo Payment Modernisation (2019 – 2024)
Role: Lead Solution Architect, HiPT JSC
- Designed layered architecture: Channels → API Gateway → Spring Boot orchestrator → Java Jolt/C++ ATMI adapter → Oracle Tuxedo domain → Oracle RAC → Kafka/Flink analytics.
- Implemented dual sync/async orchestration; Spring Boot validates, enforces idempotency, commits transactional outbox, then delegates ledger logic to Pro*C services while emitting Kafka events.
- Led 8-person squad (Java, C++, QA, DevOps, BA) on two-week sprints; partnered with operations, compliance, and Oracle vendor specialists for release readiness.
- Established OpenAPI/AsyncAPI contracts, CI/CD pipelines (Java/C++/Pro*C builds, SAST, contract & performance tests), GitOps deployment, and observability stack.
Key Outcomes:
- Released >40 APIs without regressing legacy processes; reduced effort for new services by 70% through reusable adapters.
- Scaled platform to 3,600 TPS (2× baseline) while keeping CPU <60% and P95 latency 320 ms (−460 ms) via load-balanced Spring Boot orchestration and Pro*C array DML tuning.
- Achieved 99.97% SLA (+0.05%), MTTR 15 minutes (−30 minutes); zero unscheduled downtime across three consecutive quarters.
- Delivered T+0 streaming analytics (Kafka → Landing Zone → DWH) for reconciliation, risk, and compliance dashboards.
- Hardened security posture: tokenisation, mTLS, Vault-secret rotation, WORM audit logs meeting SBV/PCI DSS audits.
- Authored Lindy scorecard guiding migration priorities, enabling stakeholders to balance innovation with operational safety.

PROFESSIONAL EXPERIENCE
**HiPT JSC — Lead Solution Architect, Core Payment Platform** (2023 – Present)
- Govern hybrid legacy-modern architecture, blueprinting Spring Boot orchestrators, rule engines, and Tuxedo adapters for multiple payment journeys.
- Drive product discovery with business/PM/BA; convert requirements into technical designs, estimate effort, and align release plans with operations.
- Mentor cross-disciplinary engineers, host guild sessions on clean architecture, resilience, and DevOps best practices.
- Conduct chaos tests, DR simulations, and incident RCAs; refresh runbooks and ensure audit readiness.

**HiPT JSC — Senior Software Engineer** (2014 – 2022)
- Engineered distributed 2PC payment engine; reduced lock contention 40%, improved P95 latency 25% through pooling, batching, and SQL hints.
- Developed ISO 20022/8583 gateways, integrated Temenos T24, and exposed REST/gRPC services that encapsulated Pro*C workloads for partner channels.
- Automated Oracle Tuxedo domain builds/deployments, codified monitoring/alerting patterns, and instituted multi-language CI/CD pipelines.

**FPT Software — Software Engineer** (2012 – 2014)
- Delivered middleware for Android set-top boxes, optimised networking and memory footprint, and hardened security/logging modules.

SELECTED INITIATIVES
- Performance Tuning: profiled Spring Boot orchestrator vs Pro*C batch, balanced thread pools, tuned `/Q` queues → sustained 3,600 TPS with 99.97% SLA.
- Operational Excellence: automated `tmboot/tmshutdown`, integrated Prometheus alerts, crafted incident playbooks → MTTR 15 minutes.
- Partner Ecosystem APIs: implemented idempotency cache, asynchronous Kafka flows, circuit breakers → partner transaction volume +25%.
- Hybrid Migration: refactored 42 Pro*C suites into API-backed services using outbox + CDC → accelerated digital-channel delivery without core rewrites.
- Capacity Planning: established performance budgets per service, mapped warning thresholds at 70% utilisation, and triggered proactive scaling.

TECHNICAL TOOLBOX
Languages & Frameworks: Java 17, Spring Boot/WebFlux/Data/Security/Cloud, C/C++, Pro*C, PL/SQL, gRPC, REST.
Platforms & Middleware: Oracle Tuxedo (ATMI/Jolt), Oracle RAC/Exadata, Redis, Kafka, Kafka Streams/Flink, GoldenGate/Debezium CDC.
DevOps & Cloud: Docker, Kubernetes (HPA, Istio), GitLab CI, Jenkins, ArgoCD, Terraform, Ansible, SonarQube, Vault/KMS, AWS fundamentals.
Quality & Observability: Prometheus/Grafana, OpenTelemetry, ELK Stack, Gatling, k6, contract & integration testing.

EDUCATION & CERTIFICATIONS
- B.Eng., Electrical & Electronics Engineering — Military Technical Academy, Hanoi (2007 – 2012)
- TOEIC 550+ (self-assessed; strong technical English comprehension)

PROFESSIONAL ATTRIBUTES
- Ownership-driven, accountable from roadmap to operations with transparent stakeholder communication.
- Continuous learner experimenting with service mesh, feature flags, chaos engineering to uplift reliability.
- Mentor and culture builder, guiding engineers transitioning from legacy stacks to modern Java/Spring practices.
- Hybrid mindset blending Lindy-proven assets with adaptive innovation to protect core transactions while unlocking new value.

LANGUAGES
- Vietnamese — Native  |  English — Professional working proficiency

REFERENCES
- Available on request (notably Head of IT, State Bank of Vietnam).
