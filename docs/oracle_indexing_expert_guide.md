# Oracle Indexing Expert Guide

Tài liệu này tổng hợp các chiến lược và kỹ thuật tối ưu chỉ mục (index) trên Oracle Database dành cho kiến trúc sư và DBA. Nội dung đi sâu vào từng loại index, cách áp dụng, hệ quả vận hành và quy trình tuning.

---

## 1. Nguyên tắc nền tảng
- Index chỉ hữu ích khi giúp truy vấn giảm I/O đọc; mọi index thêm vào phải cân nhắc chi phí ghi (INSERT/UPDATE/DELETE).
- Chọn index dựa trên mẫu truy cập thực tế: predicate, joins, sorting, grouping.
- Luôn đo lường bằng AWR/ASH/SQL Monitor trước và sau khi tạo chỉ mục; tránh tối ưu “mù”.

---

## 2. Phân loại index phổ biến

### 2.1 B-tree index
- **Sử dụng**: cột có độ chọn lọc cao, truy vấn equality hoặc range nhỏ.
- **Biến thể**:
  - *Unique index*: enforce duy nhất và tăng tốc lookup bằng khóa chính.
  - *Reverse key index*: chống hotspot khi khoá tăng tuần tự (giảm contention trên Leaf block).
- **Lưu ý**: tránh dùng cho cột có nhiều giá trị NULL (Oracle bỏ qua NULL trong B-tree chuẩn trừ khi compile `INDEX NULL`).

### 2.2 Bitmap index
- **Sử dụng**: cột có độ chọn lọc thấp (ít giá trị khác nhau), phù hợp OLAP, queries dạng ad-hoc kết hợp nhiều điều kiện.
- **Hạn chế**: không thích hợp OLTP vì ghi/khóa hàng loạt, dễ gây vänting trên bitmap segment.
- **Tối ưu**: dùng với partitioned table → mỗi partition có bitmap index riêng, giảm contention.

### 2.3 Function-based index
- **Sử dụng**: khi predicate sử dụng hàm (UPPER, TRUNC, expression), giúp tránh full scan.
- **Yêu cầu**: enable `QUERY_REWRITE_ENABLED=TRUE` và quyền `create any index`.
- **Ví dụ**: `CREATE INDEX idx_customer_email_upper ON customer (UPPER(email));`

### 2.4 Composite index (multi-column)
- **Sử dụng**: nhiều cột trong predicate; thứ tự cột quan trọng.
- **Quy tắc**:
  - Đặt cột lọc mạnh nhất trước.
  - XOR: predicate phải tham chiếu cột leading (hoặc dùng `INDEX SKIP SCAN` khi optimizer thấy phù hợp).
- **Include columns**: Oracle 12c+ hỗ trợ `INVISIBLE` columns trong index để phục vụ covering.

### 2.5 Partitioned index
- **Sử dụng**: bảng partition; giúp localize truy vấn và quản lý (rebuild/drop từng partition).
- **Loại**:
  - *Local index*: đồng partition với bảng → dễ quản trị.
  - *Global index*: cross partition, cần chăm quản; DDL partition sẽ làm index invalid, phải rebuild.

### 2.6 Index-organized table (IOT)
- **Sử dụng**: bảng nhỏ, truy cập chủ yếu qua khóa chính, cần dữ liệu giữ thứ tự.
- **Hạn chế**: không hỗ trợ đầy đủ một số tính năng (LOB, bitmap index), cần overflow segment cho cột lớn.

### 2.7 Domain index
- **Sử dụng**: dữ liệu đặc biệt (spatial, text). Ví dụ Oracle Text (`CTXCAT`, `CTXAPP`) và Spatial (`SDO_GEOMETRY`).
- **Lưu ý**: quản lý bằng API riêng (ctx_ddl, sdo_migrate); cần job maintenance.

---

## 3. Chiến lược áp dụng

### 3.1 Phân tích workload
- Dùng `DBA_HIST_SQLSTAT`, `V$SQL` để xác định truy vấn chiếm CPU/I/O cao.
- `SQL Access Advisor` đề xuất index/partition theo workload (tập hợp SQL tuning set).
- `SQL Tuning Advisor` cung cấp hint và index dựa trên plan hiện tại.

### 3.2 Thiết kế index
- Ưu tiên index nào giúp query sử dụng `INDEX RANGE SCAN`, `INDEX UNIQUE SCAN`.
- Tránh index trùng lặp: `DBA_INDEXES`, `DBA_IND_COLUMNS` giúp rà soát.
- Dùng `INVISIBLE` index để test mà không ảnh hưởng optimizer → khi ổn thì `ALTER INDEX ... VISIBLE`.

### 3.3 Bảo trì
- `MONITORING USAGE`: `ALTER INDEX idx MONITORING USAGE;` → xem `V$OBJECT_USAGE` để biết index có được dùng.
- Rebuild chỉ khi index phân mảnh nặng (`BLEVEL`, `DEL_LF_ROWS`) hoặc chuyển tablespace.
- `COALESCE` index để gom leaf block khi xóa hàng nhiều (ít tốn hơn rebuild).

### 3.4 Partition & compression
- `COMPRESS` index giúp giảm storage và I/O scan (phân tích `ANALYZE INDEX ... VALIDATE STRUCTURE` để đo benefit).
- Với bảng partition, cân nhắc `LOCAL PREFX` index cho query range theo partition key.

---

## 4. Tuning nâng cao

### 4.1 Thống kê (Statistics)
- Luôn cập nhật stats sau khi tạo index (`DBMS_STATS.GATHER_INDEX_STATS`).
- Dùng histogram (`METHOD_OPT`) cho cột skewed để optimizer chọn index đúng.

### 4.2 Hints và Plan Baselines
- Khi cần kiểm soát: `/*+ INDEX(table index_name) */` hoặc `/*+ NO_INDEX */`.
- `SQL Plan Baseline` giúp giữ plan tốt sau khi thêm index; tạo qua `DBMS_SPM.LOAD_PLANS_FROM_CURSOR_CACHE`.

### 4.3 Adaptive Query Optimization
- Oracle 12c+ có adaptive plans: review plan actual vs estimated (`DBMS_XPLAN.DISPLAY_CURSOR(format=>'ALLSTATS LAST')`).
- Kiểm tra nếu optimizer bỏ qua index → có thể do cardinality estimate sai, cần điều chỉnh stats/ngưỡng.

### 4.4 Ảnh hưởng DML
- Đánh giá `LOG_BUFFER`, `UNDO` khi thêm index mới; ghi chép nhiều hơn.
- Với OLTP, tránh bitmap index trên cột bị cập nhật thường xuyên.
- Sử dụng `ONLINE` rebuild để hạn chế downtime (`ALTER INDEX ... REBUILD ONLINE`).

---

## 5. Quy trình đề xuất
1. Thu thập workload (AWR snapshot, SQL tuning set).
2. Xác định truy vấn ưu tiên, phân tích plan hiện tại.
3. Thiết kế index, kiểm chứng bằng `EXPLAIN PLAN` và `SQL Monitor`.
4. Tạo index dạng `INVISIBLE`, theo dõi performance thực.
5. Khi ổn định → `VISIBLE` + cập nhật runbook/ADR.
6. Thiết lập job định kỳ `DBMS_STATS`, monitor usage, coalesce/rebuild khi cần.

---

## 6. Tài liệu & công cụ tham khảo
- Oracle Database Concepts, Performance Tuning Guide.
- `DBMS_STATS`, `DBMS_SQLTUNE`, `DBMS_ADVISOR`, `DBMS_SPM`.
- Oracle Enterprise Manager, SQL Developer, SQLcl, AWR/ASH report.
- Blog Jonathan Lewis, Tom Kyte – phân tích sâu về index và optimizer.

---

Đặt hiệu năng lên công cụ đo: mọi quyết định index nên dựa trên bằng chứng workload thực, tuân theo quy trình kiểm chứng và giám sát liên tục để đảm bảo lợi ích lâu dài.
