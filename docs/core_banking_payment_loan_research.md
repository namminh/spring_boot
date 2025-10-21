# Core Banking, Payment Systems, and Loan Processing – Bilingual Deep-Dive Notes

Tài liệu song ngữ giúp đào sâu ba miền trọng tâm (Core Banking, Payment Systems, Loan Processing) gắn với các dự án trong `CV_Nguyen_Minh_Nam_Hybrid_Architect.md`. Dùng để chuẩn bị phỏng vấn hoặc workshop cấp lãnh đạo.

---

## 1. Core Banking Domain / Miền Core Banking

### 1.1 Core capabilities / Năng lực cốt lõi
- **EN**: Customer master, account lifecycle, GL postings, interest/fee engines, product factory, batch (BOD/EOD).
- **VI**: Quản lý khách hàng, vòng đời tài khoản, bút toán sổ cái, tính lãi/fee, nhà máy sản phẩm, xử lý batch đầu/giữa/cuối ngày.
- **Regulatory hooks / Điểm bám tuân thủ**: SBV reports, IFRS alignment, fraud monitoring, audit trail, segregation of duties.

### 1.2 Architectural patterns / Mẫu kiến trúc
- **EN**: Preserve ledger on Oracle Tuxedo + Pro*C + Oracle RAC; expose contract-driven APIs via Spring Boot/REST, maintain decoupling through API Gateway + ESB + Kafka.
- **VI**: Giữ sổ cái trên nền Tuxedo/Pro*C/Oracle RAC; mở API dựa trên hợp đồng qua Spring Boot/REST, tách lớp bằng API Gateway, ESB, Kafka.
- **Data consistency / Nhất quán dữ liệu**: two-phase commit, transactional outbox, CDC to analytics.

### 1.3 Project references / Dự án liên quan
- CoreBank Tuxedo Modernisation (2019–2024): 40+ APIs, 3,600 TPS, SLA 99.97%.
- Temenos T24 integration (2014–2022): ISO 20022 transformation, card/treasury bridge.
- Architecture governance (2023–present): TOGAF-aligned roadmap, AWS hybrid landing zone.

### 1.4 Interview notes / Ghi chú phỏng vấn
- **EN**: Explain product factory design, ledger posting sequence, fallback strategy.
- **VI**: Giải thích cách model sản phẩm, chu trình bút toán, phương án fallback.
- Highlight compliance (SBV circulars, ISO 27001, PCI DSS).

---

## 2. Payment Systems / Hệ thống thanh toán

### 2.1 Functional scope / Phạm vi chức năng
- **EN**: HVSS/LVSS, instant payments, corporate bulk, settlement with central bank, reconciliation, investigation.
- **VI**: Thanh toán liên ngân hàng giá trị cao/thấp, thanh toán tức thời, bulk doanh nghiệp, quyết toán với NHNN, đối soát, điều tra.
- Channels: branch, internet banking, partner API, mobile.

### 2.2 Non-functional imperatives / Yêu cầu phi chức năng
- **EN**: SLA ≥ 99.97%, latency < 500 ms, deterministic BOD/EOD window, traceability.
- **VI**: SLA ≥ 99.97%, độ trễ < 500 ms, khung xử lý BOD/EOD cố định, khả năng truy vết đầu-cuối.

### 2.3 Architectural blocks / Khối kiến trúc
- Payment orchestrator (Spring Boot + virtual threads) / Orchestrator thanh toán.
- Tuxedo gateway bridging Pro*C / Gateway Tuxedo nối Pro*C.
- Kafka events → Streams/Flink / Sự kiện Kafka tới Streams/Flink.
- Support: Redis, Resilience4j, mailbox queue / Hỗ trợ: Redis, Resilience4j, mailbox.

### 2.4 Project references
- CoreBank Payment Lab: structured concurrency, transactional outbox to Kafka.
- ESB integration program: unify Core T24, treasury, card.
- National Price Data API Gateway (2020): mobile-first, OAuth2, rate limit.
- VNTA MNP SMS Gateway (2017): OTP compliance, high availability.

### 2.5 Interview prep
- **EN**: Show SLA governance, incident drill (chaos tests, MTTR 15 min), compliance artefacts (PCI DSS).
- **VI**: Trình bày quản trị SLA, quy trình xử lý sự cố, bằng chứng tuân thủ.

---

## 3. Loan Processing (LOS/LMS) / Xử lý khoản vay

### 3.1 End-to-end journey / Hành trình
1. Lead & KYC / Thu thập và định danh.
2. Scoring & underwriting / Chấm điểm, thẩm định.
3. Offer & documents / Tạo đề nghị, quản lý hồ sơ.
4. Disbursement & posting / Giải ngân và ghi sổ.
5. Servicing & collections / Quản lý trả nợ, nhắc nợ.

### 3.2 Architecture patterns
- **EN**: Separate LOS workflow vs LMS servicing; use ESB/API Gateway; externalise rules (Drools).
- **VI**: Tách LOS (workflow) và LMS (servicing); tích hợp qua ESB/API Gateway; tách chính sách bằng Drools.
- Ensure audit trail, integration with risk analytics / Đảm bảo audit trail, kết nối risk analytics.

### 3.3 Project references
- LOS/LMS orchestration (2014–2022): Core T24 + scoring + DMS via ESB.
- Digital lending acceleration: decision microservices, same-day approval.
- E-government API gateway (2021): reusable identity/notification modules.

### 3.4 Interview notes
- **EN**: Discuss data sync (collateral, limits), fallback when scoring fails, regulatory reporting.
- **VI**: Trình bày đồng bộ tài sản đảm bảo/hạn mức, fallback khi scoring lỗi, báo cáo CIC/SBV.

---

## 4. Cross-cutting Concerns / Mối quan tâm xuyên suốt

- **Security & Compliance / Bảo mật & tuân thủ**: ISO 27001, PCI DSS, SBV; mTLS, IAM, Vault/KMS.
- **DevSecOps**: GitLab CI, ArgoCD, Terraform, policy-as-code, SAST/DAST tự động.
- **Cloud & Hybrid**: AWS EKS + on-prem Tuxedo, VPC peering/VPN/Direct Connect, cost governance.
- **Incident & Continuity**: DR drills, chaos tests, runbook MTTR ≤ 15 phút, quản lý error budget.

---

## 5. Interview / Workshop Prompts (EN/VI)

1. **EN**: “Walk me through modernising a T24/Tuxedo core without downtime.”  
   **VI**: “Trình bày cách anh/chị hiện đại hóa core T24/Tuxedo mà không gây downtime.”  
   → Answer using layered architecture, roadmap, Lindy segmentation.

2. **EN**: “How do you sustain 99.97% SLA for payments?”  
   **VI**: “Làm sao duy trì SLA 99.97% cho thanh toán?”  
   → Mention autoscaling, queue governance, monitoring, incident response.

3. **EN**: “Describe your LOS/LMS integration strategy.”  
   **VI**: “Chiến lược tích hợp LOS/LMS của anh/chị là gì?”  
   → Cover ESB orchestration, decision services, compliance.

4. **EN**: “How do you embed ISO 27001/PCI DSS into the delivery lifecycle?”  
   **VI**: “Anh/chị nhúng ISO 27001/PCI DSS vào lifecycle thế nào?”  
   → Talk about DevSecOps, checklists, audits.

5. **EN**: “What governance links technology roadmap with business strategy?”  
   **VI**: “Mô hình governance nào gắn roadmap công nghệ với chiến lược kinh doanh?”  
   → Cite TOGAF ADM, architecture review board, KPI tracking.

---

## 6. Sample Answers / Câu trả lời mẫu

### 6.1 Modernising T24/Tuxedo Core
- **EN**: “We insulated the ledger with a layered architecture: channels hit an API gateway, Spring Boot orchestrators handle validation, idempotency and transactional outbox before delegating to Tuxedo/Pro*C services that keep settlement logic untouched. A TOGAF ADM roadmap staged the work in three waves (API facade, analytics, live swap) with rollback and blue–green cutover to prevent downtime.”
- **VI**: “Chúng tôi bảo vệ sổ cái bằng kiến trúc phân lớp: kênh → API gateway → orchestrator Spring Boot kiểm tra, ghi outbox rồi mới chuyển sang dịch vụ Tuxedo/Pro*C giữ nguyên logic định cư. Lộ trình TOGAF ADM chia ba wave (API facade, analytics, live swap) với kế hoạch rollback và triển khai blue–green nên không gây downtime.”

### 6.2 Maintaining 99.97% SLA for Payments
- **EN**: “Reliability comes from redundancy (two pods per service, active–active Tuxedo domain), HPA autoscaling, Istio traffic shaping and Resilience4j retries. We track SLIs (success ratio, latency, outbox lag), run chaos/DR drills, and runbooks target MTTR ≤ 15 minutes while respecting the monthly 13‑minute error budget.”
- **VI**: “SLA 99,97% đạt được nhờ dư thừa (tối thiểu 2 pod, Tuxedo active–active), HPA tự mở rộng, Istio điều phối lưu lượng, Resilience4j retry. Chúng tôi theo dõi SLI (tỷ lệ thành công, latency, outbox lag), diễn tập chaos/DR; runbook đảm bảo MTTR ≤ 15 phút và mọi thay đổi tuân thủ error budget 13 phút mỗi tháng.”

### 6.3 LOS/LMS Integration Strategy
- **EN**: “LOS handles origination workflow, scoring and document collection via Drools-driven microservices; LMS governs servicing, schedules and GL postings. An ESB/API gateway normalises data contracts, synchronous calls cover validations, while Kafka events sync collateral and limit changes. If scoring fails, we trigger a degraded manual queue with SLA alerts.”
- **VI**: “LOS đảm trách workflow khởi tạo, chấm điểm, thu thập hồ sơ bằng microservice Drools; LMS quản lý hợp đồng, lịch trả nợ và bút toán. ESB/API gateway chuẩn hóa hợp đồng dữ liệu, gọi đồng bộ để kiểm tra tức thời trong khi Kafka đồng bộ biến động tài sản bảo đảm/hạn mức. Nếu scoring lỗi, hệ thống chuyển sang hàng đợi xử lý thủ công kèm cảnh báo SLA.”

### 6.4 Embedding ISO 27001 / PCI DSS
- **EN**: “Security controls are codified in DevSecOps: Terraform enforces network segmentation, GitLab CI runs SAST/DAST and dependency scans, pipelines block merges without compliance sign-off. API gateway mandates mTLS/OAuth2, Vault rotates secrets, PCI zones isolate PAN data, and audits map to TOGAF Phase G checkpoints.”
- **VI**: “Kiểm soát bảo mật được nhúng vào DevSecOps: Terraform đảm bảo phân vùng mạng, GitLab CI chạy SAST/DAST và quét phụ thuộc, pipeline chặn merge khi chưa đạt checklist. API gateway bắt buộc mTLS/OAuth2, Vault quay vòng secret, vùng PCI cô lập dữ liệu thẻ; kiểm toán gắn với checkpoint pha G của TOGAF.”

### 6.5 Governance Linking Roadmap & Strategy
- **EN**: “We run a quarterly architecture review board aligned to TOGAF ADM. Business OKRs feed Phase A vision, capability maps from Phase B shape the roadmap, and Phase F migration plans tie into budget cycles. Each initiative is backed by an architecture contract defining success metrics, compliance gates and stakeholder sign-off.”
- **VI**: “Chúng tôi tổ chức Architecture Review Board hàng quý theo TOGAF ADM. OKR kinh doanh là đầu vào cho Phase A, bản đồ năng lực Phase B định hình roadmap, Phase F ghép vào chu kỳ ngân sách. Mỗi sáng kiến đều có architecture contract nêu KPI, checkpoint tuân thủ và trách nhiệm phê duyệt của các bên liên quan.”

Keep metrics handy / Ghi nhớ số liệu: 3,600 TPS, 99.97% SLA, MTTR 15 phút, số API, số tỉnh/thành.
