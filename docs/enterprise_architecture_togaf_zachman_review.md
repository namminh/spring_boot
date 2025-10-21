# Ôn luyện kiến trúc doanh nghiệp (TOGAF, Zachman) gắn với kinh nghiệm thực tế

Tài liệu này giúp tổng hợp kiến thức trọng tâm về kiến trúc doanh nghiệp theo TOGAF và Zachman, đồng thời ánh xạ với các dự án đã trình bày trong `CV_Nguyen_Minh_Nam_Hybrid_Architect.md`. Dùng để chuẩn bị phỏng vấn Solution/Enterprise Architect hoặc trình bày với lãnh đạo.

---

## 1. TOGAF – Architecture Development Method (ADM)

### 1.1 Các pha ADM và ví dụ dự án
| Pha ADM | Mục tiêu | Liên hệ dự án |
|---------|----------|---------------|
| **Preliminary** | Thiết lập governance, xác định capability | 2023: Thiết lập hội đồng kiến trúc (architecture review board) cho CoreBank Payment Platform, định nghĩa quy trình phê duyệt thiết kế và trách nhiệm vendor. |
| **Phase A – Architecture Vision** | Đặt mục tiêu, stakeholder, scope | 2020: Dự án API Gateway Bộ Tài chính, xây vision “Mobile-first data access” kèm business outcome (truy cập realtime cho 63 tỉnh). |
| **Phase B – Business Architecture** | Mô hình hóa quy trình nghiệp vụ, capability | 2019–2024: CoreBank modernisation, phân hạng “Lindy core” vs “adaptive edge”, map journey thanh toán, LOS/LMS. |
| **Phase C – Information Systems** | Kiến trúc Data & Application | 2017: VNTA MNP SMS Gateway – mô tả domain OTP/notification, integration với Tuxedo, ESB. |
| **Phase D – Technology Architecture** | Hạ tầng, nền tảng, tiêu chuẩn | 2021: AWS hybrid adoption – define EKS landing zone, connectivity on-prem ↔ cloud, mTLS, Vault. |
| **Phase E – Opportunities & Solutions** | Lập portfolio dự án, work package | 2022: Kế hoạch mở rộng loan/card: chia thành work package “LOS microservices”, “Card API wrapper”. |
| **Phase F – Migration Planning** | Roadmap, thứ tự ưu tiên | CoreBank Tuxedo: roadmap 3 waves (API layer, analytics, live swap) với error budget SLA 99.97%. |
| **Phase G – Implementation Governance** | Kiểm soát khi triển khai | Duyệt thiết kế chi tiết, check-list ISO 27001/PCI DSS, sign-off vendor deliverable. |
| **Phase H – Architecture Change Management** | Quản lý thay đổi dài hạn | Theo dõi KPI, thực hiện post-implementation review, cập nhật Lindy scorecard mỗi quý. |

### 1.2 Artefact TOGAF hay bị hỏi
- Architecture Vision, Statement of Architecture Work.
- Business Capability Map, Application/Data Catalogue.
- Architecture Roadmap & Implementation/Transition Plan.
- Standards Information Base (SIB) – ví dụ checklist ISO 27001/PCI DSS/SBV.
- Architecture Contract: áp dụng khi thuê vendor biết rõ acceptance criteria.

### 1.3 Mẫu câu trả lời ngắn gọn
- “Trong dự án CoreBank, tôi áp dụng ADM từ Vision tới Migration Planning bằng cách…” (nêu 2–3 pha trọng tâm).
- “Governance của tôi bao gồm ARB hàng tuần, checklist compliance, và change log theo Phase H.”

---

## 2. Zachman Framework – bảo đảm coverage

### 2.1 Ma trận 6×6
| Tầng nhìn | What (Data) | How (Function) | Where (Network) | Who (People) | When (Time) | Why (Motivation) |
|-----------|-------------|----------------|------------------|--------------|-------------|------------------|
| **Planner** | Domain concept (danh mục dịch vụ tài chính) | Chuỗi giá trị ngân hàng | Vùng triển khai (Data center, AWS region) | Bên liên quan | SLA, release cadence | Chiến lược ngân hàng |
| **Owner** | Business object (hồ sơ khoản vay, giao dịch) | Quy trình LOS/LMS, thanh toán | Kênh (branch, mobile, API partner) | Bộ phận nghiệp vụ | Sự kiện định kỳ | Chính sách, quy định SBV |
| **Designer** | Logical data model | Application architecture | Topology mạng/hybrid | Vai trò (Ops, Dev, Vendor) | Event flow | Rule kiến trúc (security, compliance) |
| **Builder** | Physical schema (Oracle RAC, Kafka topic) | Module & API | Deployment (EKS node, Tuxedo domain) | Team cấu trúc | Schedulers, batch window | SLA/SLO, error budget |
| **Subcontractor** | Script triển khai | Code cụ thể | Host/VM | Nhân sự outsource | Cron job | Story-level justification |
| **Enterprise Ops** | Data thực tế | Chạy dịch vụ | Monitoring dashboard | NOC, SOC | Alert/incident timeline | KPI thực tế |

### 2.2 Ứng dụng thực tế
- Với mỗi dự án, dùng Zachman để rà soát: liệu đã có artefact cho tất cả ô quan trọng? Ví dụ, trong MNP SMS Gateway:
  - What: Schema tin nhắn OTP/AUDIT.
  - How: luồng xác thực OTP, retry logic.
  - Where: DC chính + DR, kết nối SMPP.
  - Who: đội vận hành VNTA, nhà mạng.
  - When: SLA gửi OTP < 30s, lịch bảo trì.
  - Why: tuân thủ yêu cầu Bộ TT&TT, chống gian lận.
- Tích hợp TOGAF & Zachman: TOGAF cho quy trình, Zachman cho coverage kiểm tra khi review artefact.

---

## 3. Bộ câu hỏi luyện tập

### 3.1 Về TOGAF
1. “Anh/chị áp dụng ADM như thế nào để modern hóa hệ thống core payment?” → Chuẩn bị nêu Phase A–F với ví dụ.
2. “Kiến trúc mục tiêu của anh/chị bao gồm những building block gì? Baseline → Target?” → Dùng CoreBank layers.
3. “Governance anh/chị thiết kế đảm bảo compliance nào?” → Liệt kê ISO 27001, PCI DSS, SBV; checklist, ARB, DevSecOps.

### 3.2 Về Zachman/coverage
1. “Khi review thiết kế, anh/chị đảm bảo không bỏ sót yếu tố nào?” → Nói về sử dụng ma trận Zachman làm checklist.
2. “Cho ví dụ mapping Zachman với dự án Loan Origination.” → Data: hồ sơ tín dụng; Function: scoring, approval; Where: branch, API; Who: risk officer; When: T+0 approval; Why: tăng doanh thu retail.

### 3.3 Về dự án trong CV
1. CoreBank Payment Modernisation: highlight API Gateway, ESB, Kafka, Tuxedo, AWS hybrid.
2. National Price Data API Gateway: nhấn mạnh mobile-first, rate limiting, OAuth2.
3. VNTA MNP SMS Gateway: OTP compliance, HA SMPP.
4. Loan/Card Integration: orchestrate LOS/LMS, card switch; gắn với banking requirement.

---

## 4. Checklist tự đánh giá trước buổi phỏng vấn
- [ ] Có sơ đồ thể hiện các pha ADM và artefact tương ứng từng dự án.
- [ ] Chuẩn bị câu chuyện 3 phút cho mỗi dự án key, mapping vào TOGAF/Zachman.
- [ ] Thuộc số liệu chính: SLA 99.97%, MTTR 15 phút, 3,600 TPS, số lượng API, số tỉnh/thành.
- [ ] Nắm rõ tiêu chuẩn tuân thủ đã áp dụng: ISO 27001, PCI DSS, SBV.
- [ ] Ôn lại roadmap AWS hybrid (EKS, IAM, VPC peering) và cách bảo vệ dữ liệu.
- [ ] Có ví dụ dẫn chứng leadership: ARB, mentoring, phối hợp vendor, quản lý error budget.

---

## 5. Tài liệu tham khảo nội bộ
- `CV_Nguyen_Minh_Nam_Hybrid_Architect.md` – mô tả chi tiết kinh nghiệm.
- `docs/corebank_payment_lab_java21_detailed.md` – kiến trúc dịch vụ payment cập nhật.
- `docs/corebank_tuxedo_payment_solution.md` – tổng quan payment modernisation.
- `docs/cloud_native_legacy_integration.md` – chiến lược tích hợp legacy ↔ cloud.
- `docs/corebank_payment_lab_cloud.md` – deployment/cloud profile (AWS, Kubernetes).

Sử dụng tài liệu này như flash card để nhắc nhanh: *TOGAF = quy trình, Zachman = coverage, Project Story = bằng chứng thực tế.* Khi luyện tập, hãy chuyển mỗi bullet thành câu trả lời 2–3 câu rõ ràng, có số liệu đo được.
