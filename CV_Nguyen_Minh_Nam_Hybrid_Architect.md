Nguyen Minh Nam (Mr Nam)
Hybrid Core Banking Architect | Oracle Tuxedo × Spring Boot Modernisation
Hanoi, Vietnam  |  namnmhp89@gmail.com  |  +84 90 292 3096  |  LinkedIn on request

PROFESSIONAL SUMMARY
- 13+ years architecting mission-critical banking ecosystems, overlaying Spring Boot/Kafka microservices on Oracle Tuxedo + Pro*C cores without disrupting ledger integrity.
- Enterprise & solution architect crafting technology roadmaps, target-state architectures, and TOGAF-aligned governance across Core Banking, payments, loans, cards, and channel platforms.
- Compliance-first leader bridging business, vendors, and engineering: steer AWS hybrid adoption, enforce ISO 27001 / PCI DSS / SBV controls, mentor squads, and sustain 99.97% SLA with 15-minute MTTR.

VALUE HIGHLIGHTS
1. Enterprise Blueprinting: map current → target architecture, set capability roadmaps, and run architectural review boards aligning IT spend with retail/corporate banking priorities.
2. Hybrid Lindy Architecture: segment "Lindy core" (Tuxedo/Pro*C, Oracle RAC) vs "adaptive edge" (Spring Boot, Kafka, DWH, AWS) to modernise safely.
3. Security & Compliance: embed ISO 27001, PCI DSS, SBV mandates into design reviews, DevSecOps pipelines, and operational runbooks.
4. Delivery Leadership: lead 8-person squads through Agile cadences, align with PM/BA/executives, and orchestrate multi-vendor delivery across bank and fintech partners.

CORE CAPABILITIES
- **Enterprise Integration:** Core Banking, loan origination/management, card, payments; Java/Spring Boot orchestrators, Tuxedo (ATMI/Jolt) adapters, transactional outbox + CDC.
- **API & Data Architecture:** REST/gRPC, API Gateway, ESB, AsyncAPI/OpenAPI, Kafka, Flink/Kafka Streams, data mart design, near-real-time reconciliation.
- **Performance & Reliability:** throughput tuning (3,600 TPS), XA transaction management, Redis caching, SLA/SLO governance, chaos & DR drills.
- **Cloud & DevSecOps:** AWS hybrid landing zones, GitLab CI, ArgoCD GitOps, Terraform, Ansible, Kubernetes (HPA, Istio), Vault/KMS, ISO 27001 + PCI DSS controls.
- **Leadership & Collaboration:** technology roadmapping with business/execs, code/design reviews, cross-squad mentoring, vendor governance, incident postmortems.

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
- Drive enterprise roadmap with business/PM/BA/executives; convert strategic initiatives (instant payments, digital loans, card tokenisation) into target architectures and investment backlogs.
- Mentor cross-disciplinary engineers, host guild sessions on clean architecture, resilience, and DevOps best practices.
- Conduct chaos tests, DR simulations, and incident RCAs; refresh runbooks and ensure audit readiness.
- Lead AWS hybrid adoption (EKS + on-prem Tuxedo), establishing connectivity, IAM guardrails, and cost governance while maintaining SBV compliance.
- Chair architecture review boards covering API gateway evolution, ESB integrations, and data platform extensions.

**HiPT JSC — Senior Software Engineer** (2014 – 2022)
- Engineered distributed 2PC payment engine; reduced lock contention 40%, improved P95 latency 25% through pooling, batching, and SQL hints.
- Developed ISO 20022/8583 gateways, integrated Temenos T24, and exposed REST/gRPC services that encapsulated Pro*C workloads for partner channels.
- Led ESB integration programme (Oracle Service Bus + MuleSoft) to unify payment/event flows between Core T24, treasury, and card systems; authored governance policies for message versioning, routing, and fault isolation.
- 2017: Architected SMS Gateway platform for VNTA Mobile Number Portability (MNP) project, handling OTP verification and regulatory notifications; designed high-availability SMPP cluster, message throttling, and audit logging compliant VNTA.
- 2020: Designed mobile-facing API gateway for the National Price Data platform (Ministry of Finance), enforcing rate limiting, schema validation, and OAuth2 federation for provincial apps consuming realtime commodity feeds.
- 2021: Delivered API gateway blueprint for national e-government services, consolidating citizen authentication, service catalog routing, and audit trails across ministerial portals.
- Delivered loan origination workflow services (LOS/LMS) integrating Core T24, credit scoring, and document management via ESB orchestration; standardised data contracts enabling faster product rollout.
- Co-led card management integration (switch, card embossing, loyalty) by wrapping legacy COBOL/PAS services with Java APIs, improving time-to-market for debit products.
- Automated Oracle Tuxedo domain builds/deployments, codified monitoring/alerting patterns, and instituted multi-language CI/CD pipelines.

**FPT Software — Software Engineer** (2012 – 2014)
- Delivered middleware for Android set-top boxes, optimised networking and memory footprint, and hardened security/logging modules.

SELECTED INITIATIVES
- Performance Tuning: profiled Spring Boot orchestrator vs Pro*C batch, balanced thread pools, tuned `/Q` queues → sustained 3,600 TPS with 99.97% SLA.
- Operational Excellence: automated `tmboot/tmshutdown`, integrated Prometheus alerts, crafted incident playbooks → MTTR 15 minutes.
- Partner Ecosystem APIs: implemented idempotency cache, asynchronous Kafka flows, circuit breakers → partner transaction volume +25%.
- Hybrid Migration: refactored 42 Pro*C suites into API-backed services using outbox + CDC → accelerated digital-channel delivery without core rewrites.
- Digital Lending Acceleration: mapped LOS/LMS capability model, introduced decision microservices + rule engine (Drools) enabling same-day approval journey.
- Capacity Planning: established performance budgets per service, mapped warning thresholds at 70% utilisation, and triggered proactive scaling.

TECHNICAL TOOLBOX
Languages & Frameworks: Java 17, Spring Boot/WebFlux/Data/Security/Cloud, C/C++, Pro*C, PL/SQL, gRPC, REST.
Platforms & Middleware: Oracle Tuxedo (ATMI/Jolt), Oracle RAC/Exadata, Redis, Kafka, Kafka Streams/Flink, GoldenGate/Debezium CDC, API Gateway, ESB (Oracle Service Bus, MuleSoft).
DevOps & Cloud: Docker, Kubernetes (HPA, Istio), GitLab CI, Jenkins, ArgoCD, Terraform, Ansible, SonarQube, Vault/KMS, AWS (EKS, IAM, VPC, RDS).
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
