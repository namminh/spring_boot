# Enterprise Architecture Study Notes / Ghi chú luyện tập Kiến trúc Doanh nghiệp

Tài liệu song ngữ Anh–Việt giúp ôn tập nhanh các khái niệm cốt lõi (TOGAF, Zachman, Core Banking, Payment, Loan Processing) và luyện phản xạ trả lời phỏng vấn. Mỗi mục gồm phần tiếng Anh (EN) và tiếng Việt (VI).

---

## 1. TOGAF ADM Overview / Tổng quan TOGAF ADM

**EN**: TOGAF’s Architecture Development Method (ADM) consists of eight iterative phases (A–H) plus preliminary setup. It provides a repeatable roadmap to transform business intent into governed technology change.

**VI**: Phương pháp ADM của TOGAF gồm tám pha lặp (A–H) sau bước chuẩn bị ban đầu, dùng để chuyển hóa chiến lược kinh doanh thành thay đổi công nghệ được quản trị chặt chẽ.

| Phase | Key Question (EN) | Câu hỏi chính (VI) | My Project Example / Ví dụ dự án |
|-------|-------------------|--------------------|----------------------------------|
| Preliminary | What is the EA capability and governance model? | Năng lực kiến trúc và mô hình quản trị gồm những gì? | 2023 ARB thiết lập tại CoreBank Payment Platform. |
| Phase A | What outcomes justify the transformation? | Kết quả nào chứng minh nhu cầu chuyển đổi? | 2020 Mobile API Gateway cho Bộ Tài chính. |
| Phase B | What business capabilities/processes exist or need change? | Năng lực/quy trình kinh doanh nào hiện hữu hoặc cần thay đổi? | Phân loại Lindy core vs adaptive edge cho thanh toán, LOS. |
| Phase C | Which applications & data support the business? | Ứng dụng và dữ liệu nào hỗ trợ nghiệp vụ? | VNTA MNP SMS Gateway domain model. |
| Phase D | What technology stack and standards are required? | Hạ tầng và tiêu chuẩn công nghệ nào cần có? | AWS hybrid (EKS, IAM), Vault, mTLS. |
| Phase E | Which solution building blocks deliver quick wins? | Thành phần giải pháp nào đem lại lợi ích sớm? | Work package “LOS microservices”, “Card API wrapper”. |
| Phase F | How do we prioritise and schedule migration? | Ưu tiên và lịch chuyển đổi ra sao? | Roadmap 3 waves, SLA 99.97%. |
| Phase G | How do we ensure implementation compliance? | Làm sao đảm bảo tuân thủ khi triển khai? | Checklist ISO 27001 / PCI DSS / SBV trước go-live. |
| Phase H | How do we manage architecture change post-deployment? | Quản lý thay đổi kiến trúc sau triển khai thế nào? | Lindy scorecard, KPI review hàng quý. |

---

## 2. Zachman Framework Cheat Sheet / Ghi chú nhanh Zachman

**EN**: Zachman is a taxonomy ensuring every stakeholder perspective (Planner → Operations) addresses six interrogatives (What, How, Where, Who, When, Why).

**VI**: Zachman là bảng phân loại giúp đảm bảo mỗi góc nhìn cổ đông (Planner → Operations) trả lời được sáu câu hỏi cơ bản (Cái gì, Như thế nào, Ở đâu, Ai, Khi nào, Tại sao).

| Perspective / Góc nhìn | What / Cái gì | How / Như thế nào | Where / Ở đâu | Who / Ai | When / Khi nào | Why / Tại sao |
|------------------------|---------------|-------------------|---------------|----------|---------------|---------------|
| Planner | Domain catalog | Value streams | Deployment regions | Stakeholders | Release cadence | Strategic intent |
| Owner | Business objects | Processes (LOS, payments) | Channels | Business units | Business events | Policies, SBV regs |
| Designer | Logical data | Application architecture | Network topology | Roles & RACI | Event flow | Architecture principles |
| Builder | Physical schema | Modules/API | Pods, domains | Dev/Ops teams | Schedules | SLA/SLO |
| Subcontractor | DDL files | Code | Hosts | Vendors | Cron jobs | Acceptance criteria |
| Operations | Live data | Runbooks | Monitoring dashboards | NOC/SOC | Incident timeline | KPI & improvement |

Practice prompt / Câu luyện tập: **EN** “How do you ensure no architectural aspect is missed?” **VI** “Anh/chị làm thế nào để không bỏ sót khía cạnh kiến trúc?” → Mention using the Zachman matrix as a review checklist.

---

## 3. Domain Deep Dives / Đào sâu theo miền

### 3.1 Core Banking
- **EN**: Protect ledger integrity using hybrid architecture; expose microservices via API Gateway & ESB; leverage two-phase commit or outbox for consistency.
- **VI**: Bảo toàn sổ cái bằng kiến trúc lai; mở API qua Gateway/ESB; dùng 2PC hoặc outbox để đảm bảo nhất quán.
- **Project tie-in**: CoreBank Payment Modernisation, T24 integration.

### 3.2 Payment Systems
- **EN**: Enforce idempotent orchestrators, structured concurrency (Java 21), Kafka-backed outbox, SLA 99.97% with autoscaling and runbooks.
- **VI**: Tổ chức orchestrator idempotent, structured concurrency (Java 21), outbox qua Kafka, duy trì SLA 99.97% bằng autoscaling và runbook.
- **Project tie-in**: Payment Lab, ESB consolidation, VNTA SMS Gateway (for OTP flows).

### 3.3 Loan Processing (LOS/LMS)
- **EN**: Separate LOS (workflow, scoring) from LMS (servicing), integrate via ESB, externalise credit rules via Drools/decision microservices.
- **VI**: Tách LOS (quy trình, scoring) khỏi LMS (quản lý khoản vay), tích hợp qua ESB, chuẩn hóa chính sách tín dụng bằng Drools/microservice quyết định.
- **Project tie-in**: Loan origination workflow, digital lending acceleration.

---

## 4. Bilingual Flash Q&A / Câu hỏi – Trả lời song ngữ

1. **Q (EN)**: “Describe your governance approach when leading architecture change.”  
   **A (EN)**: “I run an architecture review board aligned with TOGAF Phase G, enforcing ISO 27001/PCI DSS/SBV checklists before deployment.”  
   **Hỏi (VI)**: “Anh/chị quản trị thay đổi kiến trúc như thế nào?”  
   **Đáp (VI)**: “Tôi tổ chức Architecture Review Board theo pha G của TOGAF, kiểm tra checklist ISO 27001/PCI DSS/SBV trước khi triển khai.”

2. **Q (EN)**: “How do you keep SLA 99.97% for payment services?”  
   **A (EN)**: “We combine Resilience4j retries, Redis caching, HPA scaling, Istio traffic shaping, and runbooks with MTTR 15 minutes.”  
   **Hỏi (VI)**: “Làm sao giữ SLA 99.97% cho dịch vụ thanh toán?”  
   **Đáp (VI)**: “Áp dụng retry Resilience4j, cache Redis, HPA, Istio điều phối lưu lượng và runbook với MTTR 15 phút.”

3. **Q (EN)**: “What is the difference between LOS and LMS?”  
   **A (EN)**: “LOS handles origination workflow and decisioning; LMS records schedules, payments, and GL postings; they share standard contracts over ESB.”  
   **Hỏi (VI)**: “Khác biệt giữa LOS và LMS là gì?”  
   **Đáp (VI)**: “LOS xử lý quy trình khởi tạo và phê duyệt, LMS quản lý hợp đồng, lịch trả nợ và bút toán; hai hệ thống trao đổi qua ESB với hợp đồng dữ liệu chuẩn.”

4. **Q (EN)**: “How do TOGAF and Zachman complement each other?”  
   **A (EN)**: “TOGAF gives the process steps (ADM); Zachman offers a matrix to verify coverage of data/function/location/people/time/motivation across stakeholders.”  
   **Hỏi (VI)**: “TOGAF và Zachman bổ trợ nhau thế nào?”  
   **Đáp (VI)**: “TOGAF cung cấp quy trình (ADM); Zachman là ma trận đảm bảo đủ các khía cạnh dữ liệu/chức năng/vị trí/nhân sự/thời gian/động cơ cho mọi bên liên quan.”

5. **Q (EN)**: “Explain your experience integrating Core T24 with payment and card systems.”  
   **A (EN)**: “I led an ESB program leveraging Oracle Service Bus and MuleSoft to harmonise T24, treasury, and card switch flows with ISO 20022 schemas and fault isolation policies.”  
   **Hỏi (VI)**: “Kinh nghiệm tích hợp Core T24 với hệ thống thanh toán, thẻ của anh/chị?”  
   **Đáp (VI)**: “Tôi dẫn dắt chương trình ESB dùng Oracle Service Bus và MuleSoft, hợp nhất luồng T24, treasury, card switch theo chuẩn ISO 20022 và chính sách cô lập lỗi.”

---

## 5. Practice Routine / Lộ trình luyện tập
- **EN**: Spend 10 minutes daily reading one section aloud in English, then paraphrase in Vietnamese.  
  **VI**: Mỗi ngày dành 10 phút đọc to một mục bằng tiếng Anh, sau đó diễn giải lại bằng tiếng Việt.
- **EN**: Record mock answers to the flash Q&A, review clarity and metrics.  
  **VI**: Ghi âm câu trả lời thử, kiểm tra sự rõ ràng và số liệu đưa ra.
- **EN**: Update examples with latest deliverables (e.g., new APIs, compliance audits).  
  **VI**: Cập nhật ví dụ bằng thành tựu mới (API, audit).

---

Keep this document alongside `docs/enterprise_architecture_togaf_zachman_review.md` and `docs/core_banking_payment_loan_research.md` for quick cross-reference.
