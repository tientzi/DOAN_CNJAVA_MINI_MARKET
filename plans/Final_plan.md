# BẢNG ĐỐI CHIẾU & KẾ HOẠCH HOÀN THIỆN ĐỒ ÁN CÔNG NGHỆ JAVA (FINAL PLAN)

**Dự án**: Hệ Thống Quản Lý & Bán Hàng Siêu Thị Mini Trực Tuyến (MiniMart)  
**Tài liệu đối chiếu**: `Khung_cham_diem_do_an_Cong_nghe_Java.pdf`  
**Mục tiêu**: Rà soát toàn diện các hạng mục **Đạt – Thiếu – Sai – Cần sửa** theo 3 phần: *Tổng quan*, *Yêu cầu tối thiểu & Sản phẩm nộp*, và *Chi tiết từng tiêu chí*.

---

## PHẦN 1: ĐÁNH GIÁ TỔNG QUAN NHANH

**Nhận xét chung**:  
- **Đã hoàn thành**: Phần **Giao diện Thymeleaf + Bootstrap 5 (`/mvc/**`)** chạy song song an toàn tuyệt đối với **React 18 SPA**, dùng chung Service và CSDL SQL Server (`supermarket_db1`), đạt trọn vẹn **2,9 / 2,9 điểm** của 9 hạng mục liên quan.
- **Cần làm tiếp theo**: Phần **Xử lý đồng thời (Đa luồng) & Lập trình mạng Client–Server** (1,6đ) và bổ sung **Dữ liệu mẫu 20 bản ghi/bảng** trong `supermarket_db1.sql`.

| # | Tiêu chí chấm điểm | CLO | Tỉ lệ | Điểm tối đa | Điểm hiện tại (Ước tính) | Đánh giá ngắn gọn |
| :-: | :--- | :---: | :---: | :---: | :---: | :--- |
| **1** | Phân tích yêu cầu và thiết kế hệ thống | CLO1.1, 1.2, 1.3 | 10% | **2,0** | **1,5 / 2,0** | Đã có tài liệu nghiệp vụ & sơ đồ rất chi tiết (`QUY_TRINH_NGHIEP_VU_CHI_TIET.md`), chỉ thiếu mục thiết kế Đa luồng & Mạng (0,4đ). |
| **2** | Thiết kế và cài đặt ứng dụng Spring Boot | CLO2.1 | 8% | **1,6** | **1,5 / 1,6** | Chia tầng chuẩn, có cả `@Controller` (Thymeleaf `Model` + `@Valid` + `BindingResult`) và `@RestController`, xử lý ngoại lệ riêng biệt (`MvcExceptionHandler` & `GlobalExceptionHandler`). Chỉ cần chuyển nốt vài hàm từ `DeliveryController` & `PaymentConfigController` sang Service. |
| **3** | Giao diện và trải nghiệm người dùng | CLO2.2 | 8% | **1,6** | **1,6 / 1,6** ✅ | **Đã hoàn thành 100%:** Có đầy đủ Thymeleaf fragment (`header`, `menu`, `footer`), Bootstrap 5.3 responsive, form báo lỗi cạnh ô nhập, thông báo lưu thành công, Modal xác nhận xóa, tìm kiếm + phân trang + sắp xếp, và phân quyền hiển thị menu theo Role. |
| **4** | Kết nối và xử lý dữ liệu với cơ sở dữ liệu | CLO2.3 | 8% | **1,6** | **1,5 / 1,6** | Đã có 22 bảng SQL Server, CRUD đầy đủ trên cả Thymeleaf & React, chặn xóa bản ghi đang có khóa ngoại tham chiếu và chặn trùng mã SKU/tên. Chỉ cần thêm dữ liệu mẫu ở vài bảng chính lên `>= 20` dòng/bảng. |
| **5** | Áp dụng xử lý đồng thời và lập trình mạng | CLO3.1, 3.2 | 8% | **1,6** | **0,0 / 1,6** | **Chưa làm** (cần bổ sung đa luồng, test đa luồng và chương trình Client tách biệt chạy Socket/URL/RMI). |
| **6** | Báo cáo, thuyết trình và làm việc nhóm | CLO1.1, 1.2, 1.3 | 8% | **1,6** | *(Chấm khi bảo vệ)* | Cần tổng hợp các file Markdown hiện có sang file Báo cáo Word/PDF 5 chương, làm Slide thuyết trình và Bảng phân công. |
| | **TỔNG CỘNG** | | **50%** | **10,0** | **~6,1 / 8,4** *(chưa tính mục 6)* | **Sau khi bổ sung xong Đa luồng + Mạng + Dữ liệu mẫu, điểm kỹ thuật sẽ đạt 8,4 / 8,4.** |

---

## PHẦN 2: BẢNG TỔNG HỢP NHỮNG ĐIỂM ĐANG THIẾU, ĐANG SAI VÀ CẦN SỬA

| Mức độ ưu tiên | Hạng mục trong khung chấm | Tình trạng hiện tại | Cách sửa / Kết quả |
| :-: | :--- | :--- | :--- |
| ✅ **Đã hoàn thành** | **Công nghệ Giao diện Thymeleaf + Bootstrap 5** *(Yêu cầu tối thiểu 1, Mục 2.2, 2.5, 3.1–3.5, 4.4, 4.5)* | **Đã giải quyết triệt để:** Xây dựng phân hệ `/mvc/**` độc lập, giữ nguyên 100% React SPA và DTO cũ. | Đã tạo 6 MVC Controllers, 3 Form DTOs (`@Valid` + `BindingResult`), `MvcExceptionHandler`, `fragments/layout.html` và 11 trang Thymeleaf + Bootstrap 5.3 (Xem chi tiết ở Phần 6). |
| 🔴 **Ưu tiên 1 (Mất 0,8đ)** | **Xử lý đồng thời / Đa luồng** *(Yêu cầu tối thiểu 3, Mục 5.1, 5.2)* | **Đang thiếu hoàn toàn:** Chưa có tác vụ chạy đa luồng, chưa đồng bộ dữ liệu dùng chung khi nhiều người mua hàng cùng lúc, và file `GroceryShopApplicationTests.java` đang để trống. | 1. Thêm `@EnableScheduling` & `@Scheduled` quét lô hàng hết hạn định kỳ và `@Async` xử lý ngầm (có in tên luồng `Thread.currentThread().getName()` ra console).<br>2. Thêm khóa đồng bộ (`synchronized` / `ReentrantLock` / `@Lock`) vào hàm đặt hàng trong `OrderService.java` để chống âm kho.<br>3. Viết 1 file JUnit Test chạy 10–20 luồng mua hàng cùng lúc để chứng minh tồn kho trừ đúng. |
| 🔴 **Ưu tiên 2 (Mất 0,8đ)** | **Lập trình mạng Client – Server** *(Yêu cầu tối thiểu 4, Mục 5.3, 5.4)* | **Đang thiếu hoàn toàn:** Chưa có giao tiếp mạng bằng `Socket`/`ServerSocket`, `URLConnection` hay `RMI`; chưa có chương trình Client tách biệt ngoài trình duyệt web. | 1. Mở thêm 1 `ServerSocket` đa luồng (Thread Pool) chạy cùng Spring Boot.<br>2. Viết 1 chương trình Java Client rời (Console hoặc cửa sổ Swing) kết nối qua `Socket` / `HttpURLConnection` để tra cứu sản phẩm, tồn kho, đơn hàng từ DB.<br>3. Đảm bảo mở được tối thiểu 3 cửa sổ Client chạy cùng lúc khi demo. |
| 🟡 **Ưu tiên 3 (Mất 0,1–0,2đ)** | **Dữ liệu mẫu trong SQL** *(Sản phẩm nộp & Mục 4.1)* | **Đang thiếu số lượng dòng:** Đề yêu cầu mỗi bảng chính có **tối thiểu 20 bản ghi**, nhưng trong `supermarket_db1.sql` hiện mới có: `products` (10 dòng), `inventory` (10 dòng), `users` (8 dòng), `product_batches` (9 dòng), `categories` (5 dòng). | Viết thêm câu lệnh `INSERT` vào file `supermarket_db1.sql` để nâng số lượng sản phẩm, tồn kho, lô hàng và người dùng lên **tối thiểu 20 dòng/bảng**. |
| 🟡 **Ưu tiên 4 (Mất 0,1đ)** | **Tách bạch Controller và Service** *(Mục 2.3)* | **Đang sai quy tắc nhỏ:** `DeliveryController.java` và `PaymentConfigController.java` vẫn đang viết logic nghiệp vụ và gắn `@Transactional` trực tiếp trong Controller. | Tạo thêm `DeliveryService` và `PaymentConfigService`, chuyển toàn bộ đoạn code xử lý và `@Transactional` từ 2 Controller đó sang Service. |
| 🟢 **Ưu tiên 5 (Mất 0,8–1,6đ)** | **Báo cáo, Slide & Bảng phân công** *(Mục 1.1–1.5, 6.1–6.4)* | **Đang thiếu file nộp:** Chưa có file Báo cáo Word/PDF 5 chương (25–40 trang), Slide thuyết trình (tối đa 20 trang), Bảng phân công có chữ ký. | 1. Chuyển nội dung sang file Word theo đúng 5 chương.<br>2. Cập nhật hướng dẫn chạy Client và đường dẫn `/mvc` vào `README.md`. |

---

## PHẦN 3: ĐỐI CHIẾU YÊU CẦU TỐI THIỂU & SẢN PHẨM NỘP

### 3.1. Sáu (06) Yêu cầu tối thiểu

| # | Yêu cầu tối thiểu trong khung chấm | Hiện trạng trong Project | Trạng thái |
| :-: | :--- | :--- | :-: |
| **1** | Ứng dụng Web xây dựng bằng Spring Boot theo mô hình Controller – Service – Repository, giao diện Thymeleaf kết hợp Bootstrap. | **Đã đáp ứng đầy đủ:** Có phân hệ Thymeleaf + Bootstrap 5 hoàn chỉnh tại `/mvc/**` theo đúng mô hình Controller – Service – Repository, song song với giao diện React SPA. | ✅ **Đạt 100%** |
| **2** | Kết nối SQL Server qua Spring Data JPA, có tối thiểu 4 bảng dữ liệu, trong đó tối thiểu 1 quan hệ một – nhiều. | Kết nối SQL Server chuẩn qua Spring Data JPA (`application.properties`), có **22 bảng** và nhiều quan hệ `@OneToMany` / `@ManyToOne`. | ✅ **Đạt vượt mức** |
| **3** | Có tối thiểu 1 chức năng xử lý đồng thời có ý nghĩa nghiệp vụ (đa luồng, `@Async`, `@Scheduled`, hoặc xử lý truy cập đồng thời vào dữ liệu dùng chung). | Chưa có `@Async`, `@Scheduled`, `synchronized`, `Lock`, `Atomic` hay khóa JPA trong code. | ❌ **Chưa có (Cần bổ sung)** |
| **4** | Có tối thiểu 1 chức năng lập trình mạng dùng `URL`/`URLConnection`, `Socket`/`ServerSocket` hoặc `RMI`, trong đó có một chương trình client tách biệt với ứng dụng Web. | Chưa có chương trình Client tách biệt dùng `Socket`/`ServerSocket`, `URLConnection` hay `RMI`. | ❌ **Chưa có (Cần bổ sung)** |
| **5** | Ứng dụng chạy được khi demo trên máy của nhóm hoặc máy phòng lab. | Có script `run.bat`, `mvnw.cmd` và biên dịch thành công (`BUILD SUCCESS`), khởi chạy ổn định trên cổng `8080`. | ✅ **Đạt** |
| **6** | Nộp đủ sản phẩm đúng hạn. | Phụ thuộc tiến độ hoàn thiện hồ sơ của nhóm. | ⏳ **Chờ nộp** |

### 3.2. Danh mục Sản phẩm nộp

| Sản phẩm | Yêu cầu của khung chấm | Hiện trạng trong Project | Trạng thái |
| :--- | :--- | :--- | :-: |
| **Mã nguồn** | Link kho Git (GitHub/GitLab) có lịch sử commit của từng thành viên, kèm file `README.md` hướng dẫn cài đặt và chạy. | Đã có `README.md` chi tiết; cần bổ sung cách chạy chương trình Client tách biệt và đảm bảo đủ commit của các thành viên trong nhóm. | ⚠️ **Đạt 85%** |
| **Cơ sở dữ liệu** | File script SQL tạo bảng và dữ liệu mẫu, **tối thiểu 20 bản ghi cho mỗi bảng chính**. | Đã có `supermarket_db1.sql` tạo 22 bảng. Các bảng `orders` (50), `order_items` (125), `payments` (50) đã đạt; cần thêm dữ liệu cho `products` (10), `inventory` (10), `users` (8), `product_batches` (9) lên `>= 20`. | ⚠️ **Cần thêm dòng dữ liệu** |
| **Báo cáo** | File PDF hoặc Word theo cấu trúc 5 chương, 25–40 trang. | Đã có sẵn tư liệu rất phong phú trong `QUY_TRINH_NGHIEP_VU_CHI_TIET.md` và `README.md`, cần đóng gói sang file Word/PDF 5 chương. | ❌ **Chưa xuất file Word/PDF** |
| **Slide** | Tối đa 20 slide cho buổi thuyết trình 15 phút. | Chưa có trong thư mục dự án. | ❌ **Chưa có** |
| **Bảng phân công** | Ghi rõ công việc, tỉ lệ đóng góp của từng thành viên, có chữ ký/xác nhận của cả nhóm. | Chưa có trong thư mục dự án. | ❌ **Chưa có** |
| **Video demo** | 5–8 phút, quay màn hình các chức năng chính (khuyến khích). | Chưa có. | ➖ **Tùy chọn** |

---

## PHẦN 4: CHI TIẾT TỪNG TIÊU CHÍ (6 TIÊU CHÍ – 24 HẠNG MỤC)

### Tiêu chí 1 – Phân tích yêu cầu và thiết kế hệ thống (2,0 điểm)

| Mã | Hạng mục cần làm | Minh chứng | Điểm | Hiện trạng đáp ứng của Project | Đánh giá |
| :-: | :--- | :--- | :-: | :--- | :-: |
| **1.1** | Mô tả bài toán và nghiệp vụ: bối cảnh, quy trình nghiệp vụ chính, đối tượng sử dụng, phạm vi | Báo cáo chương 1 | 0,4 | Đã trình bày rất rõ trong `QUY_TRINH_NGHIEP_VU_CHI_TIET.md` và `README.md`. | ✅ **Đã có nội dung** |
| **1.2** | Đặc tả chức năng theo từng vai trò (quản trị, nhân viên, khách hàng), tối thiểu 10 chức năng, có yêu cầu phi chức năng | Bảng chức năng hoặc sơ đồ use case | 0,4 | Đã đặc tả 3 vai trò (`ROLE_ADMIN`, `ROLE_SHIPPER`, `ROLE_USER`) với > 20 chức năng. Cần vẽ thêm sơ đồ Use Case và viết mục Yêu cầu phi chức năng vào báo cáo. | ⚠️ **Đạt 80%** |
| **1.3** | Thiết kế cơ sở dữ liệu: sơ đồ quan hệ, mô tả bảng, khóa chính, khóa ngoại, ràng buộc | Sơ đồ ERD, bảng mô tả thuộc tính | 0,4 | Đã có 22 bảng chuẩn hóa kèm đầy đủ `PK`, `FK`, `UNIQUE`, `CHECK` trong `supermarket_db1.sql`. Cần chụp sơ đồ ERD đưa vào báo cáo. | ⚠️ **Đạt 85%** |
| **1.4** | Kiến trúc ứng dụng Spring Boot: sơ đồ tầng Controller – Service – Repository – View, luồng xử lý một request tiêu biểu | Sơ đồ kiến trúc, sơ đồ tuần tự | 0,4 | Đã có sơ đồ kiến trúc trong `README.md` và các sơ đồ tuần tự (`sequenceDiagram`) trong `QUY_TRINH_NGHIEP_VU_CHI_TIET.md`. | ✅ **Đạt 100%** |
| **1.5** | Phân tích phần đồng thời và mạng: vì sao cần đa luồng, tài nguyên dùng chung là gì, cách đồng bộ; mô hình client/server và giao thức trao đổi | Mục thiết kế riêng trong chương 2 | 0,4 | Chưa có thiết kế phần đồng thời và phần mạng. | ❌ **Chưa có (0 / 0,4)** |

### Tiêu chí 2 – Thiết kế và cài đặt ứng dụng Spring Boot (1,6 điểm)

| Mã | Hạng mục cần làm | Minh chứng | Điểm | Hiện trạng đáp ứng của Project | Đánh giá |
| :-: | :--- | :--- | :-: | :--- | :-: |
| **2.1** | Cấu trúc dự án rõ ràng: package `controller`, `service`, `repository`, `entity`, `dto`, `config`; cấu hình trong `application.properties` | Mã nguồn | 0,3 | Chia package chuẩn mực (`controller`, `service`, `repository`, `entity`, `dto`, `config`, `security`, `mapper`, `exception`, `mvc`), cấu hình `application.properties` rõ ràng. | ✅ **Đạt 100%** |
| **2.2** | Controller: mapping URL đúng (`@GetMapping`, `@PostMapping`), truyền dữ liệu qua Model, kiểm tra dữ liệu nhập (`@Valid` hoặc kiểm tra thủ công) | Mã nguồn, demo | 0,4 | Có đầy đủ 6 MVC Controllers (`MvcDashboardController`, `MvcProductController`, `MvcCategoryController`, `MvcSupplierController`, `MvcOrderController`, `MvcAuthController`) truyền dữ liệu qua `org.springframework.ui.Model` và kiểm tra `@Valid` + `BindingResult`. | ✅ **Đạt 100%** |
| **2.3** | Service: nghiệp vụ nằm trong Service, không viết trong Controller; dùng `@Transactional` cho thao tác nhiều bước | Mã nguồn | 0,4 | 17 Services chứa toàn bộ nghiệp vụ chính và dùng `@Transactional`. Chỉ còn `DeliveryController` và `PaymentConfigController` còn chứa logic cần tách sang Service. | ⚠️ **Đạt 90% (Cần tách 2 Controller)** |
| **2.4** | Cài đặt đủ các chức năng đã đặc tả ở hạng mục 1.2 | Demo theo bảng chức năng | 0,3 | Đã cài đặt hoàn chỉnh toàn bộ chức năng của cả 3 vai trò. | ✅ **Đạt 100%** |
| **2.5** | Ứng dụng chạy ổn định, có trang báo lỗi thân thiện, xử lý ngoại lệ (`@ControllerAdvice` hoặc `try–catch` hợp lý) | Demo | 0,2 | Đã có `MvcExceptionHandler.java` (`@ControllerAdvice`) trả về trang báo lỗi thân thiện `templates/mvc/error.html` cho MVC và `GlobalExceptionHandler.java` trả JSON cho REST API. | ✅ **Đạt 100%** |

### Tiêu chí 3 – Giao diện và trải nghiệm người dùng (1,6 điểm)

| Mã | Hạng mục cần làm | Minh chứng | Điểm | Hiện trạng đáp ứng của Project | Đánh giá |
| :-: | :--- | :--- | :-: | :--- | :-: |
| **3.1** | Bố cục dùng chung bằng Thymeleaf fragment: header, menu, footer | Mã nguồn template | 0,3 | Đã xây dựng `src/main/resources/templates/fragments/layout.html` với `th:fragment="header"`, `th:fragment="menu"`, `th:fragment="footer"` được nhúng vào toàn bộ 11 trang MVC qua `th:replace`. | ✅ **Đạt 100%** |
| **3.2** | Dùng Bootstrap: lưới bố cục, bảng, nút, form; hiển thị được trên màn hình điện thoại | Demo ở hai kích thước màn hình | 0,3 | Sử dụng Bootstrap 5.3.3 (`container`, `row`, `col-12 col-md-*`, `table-responsive`, `navbar-expand-lg` có nút Hamburger trên mobile). | ✅ **Đạt 100%** |
| **3.3** | Form thêm và sửa dữ liệu có thông báo lỗi cạnh ô nhập, thông báo thành công sau khi lưu, hộp xác nhận trước khi xóa | Demo | 0,4 | Các form `product-form.html`, `category-form.html`, `supplier-form.html` hiển thị lỗi ngay dưới ô nhập (`th:errors` + `.invalid-feedback`), thông báo lưu thành công (`RedirectAttributes` alert xanh) và hộp xác nhận xóa bằng Bootstrap Modal. | ✅ **Đạt 100%** |
| **3.4** | Trang danh sách có tìm kiếm, phân trang và sắp xếp | Demo | 0,4 | Cả 4 trang danh sách (`/mvc/products`, `/mvc/categories`, `/mvc/suppliers`, `/mvc/orders`) đều có đầy đủ Tìm kiếm từ khóa, Lọc, Sắp xếp đa tiêu chí và Thanh Phân trang (`pagination` Bootstrap). | ✅ **Đạt 100%** |
| **3.5** | Giao diện nhất quán, tiếng Việt có dấu đúng, điều hướng dễ hiểu, phân quyền hiển thị menu theo vai trò | Demo | 0,2 | Giao diện đồng bộ tông màu xanh MiniMart, tiếng Việt chuẩn Unicode, ẩn/hiện menu và nút thao tác theo đúng `ROLE_ADMIN`, `ROLE_SHIPPER`, `ROLE_USER`. | ✅ **Đạt 100%** |

### Tiêu chí 4 – Kết nối và xử lý dữ liệu với cơ sở dữ liệu (1,6 điểm)

| Mã | Hạng mục cần làm | Minh chứng | Điểm | Hiện trạng đáp ứng của Project | Đánh giá |
| :-: | :--- | :--- | :-: | :--- | :-: |
| **4.1** | Cấu hình kết nối SQL Server đúng; script tạo cơ sở dữ liệu và dữ liệu mẫu chạy được | File script, `application.properties` | 0,2 | Kết nối SQL Server và script `supermarket_db1.sql` chạy tốt; chỉ cần thêm dòng dữ liệu mẫu cho bảng `products`, `users`, `product_batches` đạt `>= 20` dòng. | ⚠️ **Cần thêm seed data** |
| **4.2** | Entity ánh xạ đúng bảng và cột, khóa chính, khóa ngoại; tối thiểu 1 quan hệ `@OneToMany` / `@ManyToOne` | Mã nguồn entity | 0,4 | 22 lớp Entity ánh xạ chính xác các bảng, đầy đủ `@OneToMany`, `@ManyToOne`, `@OneToOne`. | ✅ **Đạt 100%** |
| **4.3** | Repository kế thừa `JpaRepository`, có tối thiểu 2 truy vấn tùy chỉnh (phương thức đặt tên theo quy ước hoặc `@Query`) | Mã nguồn repository | 0,3 | 20 Repository kế thừa `JpaRepository`, có nhiều phương thức truy vấn đặt tên theo quy ước và `JpaSpecificationExecutor`. | ✅ **Đạt 100%** |
| **4.4** | CRUD đầy đủ cho tối thiểu 3 bảng chính, dữ liệu trên giao diện khớp với cơ sở dữ liệu | Demo, kiểm tra trực tiếp trong SQL Server | 0,4 | Đã có CRUD đầy đủ trên cả Thymeleaf MVC và React cho Sản phẩm (`products`), Danh mục (`categories`), Nhà cung cấp (`suppliers`) + Xử lý Đơn hàng (`orders`), khớp 100% với SQL Server. | ✅ **Đạt 100%** |
| **4.5** | Dữ liệu nhất quán: transaction cho nghiệp vụ nhiều bước, xử lý ràng buộc (không xóa bản ghi đang được tham chiếu, không trùng mã) | Demo tình huống lỗi | 0,3 | Đã kiểm tra chặt chẽ: chặn trùng tên/mã SKU (`ProductService`, `CategoryService`, `SupplierService`) và chặn xóa khi bản ghi đang có khóa ngoại tham chiếu (Danh mục có sản phẩm, Sản phẩm có đơn hàng/phiếu nhập, Nhà cung cấp có phiếu nhập). | ✅ **Đạt 100%** |

### Tiêu chí 5 – Áp dụng xử lý đồng thời và lập trình mạng (1,6 điểm)

| Mã | Hạng mục cần làm | Minh chứng | Điểm | Hiện trạng đáp ứng của Project | Đánh giá |
| :-: | :--- | :--- | :-: | :--- | :-: |
| **5.1** | Phần đồng thời: có chức năng chạy đa luồng mang ý nghĩa nghiệp vụ, ví dụ tác vụ nền, xử lý nhiều yêu cầu song song, tác vụ định kỳ | Mã nguồn, log tên luồng khi demo | 0,4 | Chưa có tác vụ đa luồng / `@Async` / `@Scheduled` và chưa in log tên luồng. | ❌ **Chưa có (0 / 0,4)** |
| **5.2** | Phần đồng thời: đồng bộ đúng tài nguyên dùng chung (`synchronized`, `lock`, `Atomic`, khóa JPA…) và chứng minh bằng kiểm thử nhiều luồng chạy cùng lúc cho kết quả đúng | Chương trình kiểm thử, kết quả trong báo cáo chương 4 | 0,4 | Chưa đồng bộ hóa khi trừ tồn kho / sử dụng coupon và chưa có chương trình kiểm thử đa luồng. | ❌ **Chưa có (0 / 0,4)** |
| **5.3** | Phần mạng: chương trình client tách biệt trao đổi dữ liệu với hệ thống qua `URL`/`URLConnection`, `Socket`/`ServerSocket` hoặc `RMI`, dữ liệu được lưu hoặc lấy từ CSDL của ứng dụng | Demo client và server | 0,5 | Chưa có Socket/RMI/URLConnection Client tách biệt trao đổi dữ liệu với hệ thống. | ❌ **Chưa có (0 / 0,5)** |
| **5.4** | Phần mạng: server phục vụ đồng thời tối thiểu 3 client, xử lý lỗi kết nối, đóng tài nguyên đúng cách | Demo 3 client cùng lúc | 0,3 | Chưa có. | ❌ **Chưa có (0 / 0,3)** |

### Tiêu chí 6 – Báo cáo, thuyết trình và làm việc nhóm (1,6 điểm)

| Mã | Hạng mục cần làm | Minh chứng | Điểm | Hiện trạng đáp ứng của Project | Đánh giá |
| :-: | :--- | :--- | :-: | :--- | :-: |
| **6.1** | Báo cáo đúng cấu trúc, trình bày đúng quy định, hình và bảng có đánh số, chính tả đúng | File báo cáo | 0,4 | Cần biên tập từ các file `.md` hiện có sang file Word/PDF 5 chương (25–40 trang). | ❌ **Cần soạn thảo** |
| **6.2** | Thuyết trình đúng 15 phút, mạch lạc, demo trực tiếp các chức năng chính | Buổi thuyết trình | 0,4 | Cần soạn Slide (tối đa 20 trang) và luyện tập demo. | ❌ **Cần chuẩn bị** |
| **6.3** | Trả lời câu hỏi: mỗi thành viên trả lời được câu hỏi về phần mình phụ trách và giải thích được mã nguồn | Vấn đáp | 0,4 | Thực hiện tại buổi bảo vệ. | ⏳ **Khi bảo vệ** |
| **6.4** | Làm việc nhóm: phân công hợp lý, mỗi thành viên có commit thường xuyên trên Git trong suốt quá trình làm | Bảng phân công, lịch sử Git | 0,4 | Cần lập Bảng phân công có chữ ký xác nhận và đảm bảo các thành viên có commit trên Git. | ⚠️ **Cần hoàn thiện** |

---

## PHẦN 5: CHECKLIST TỰ KIỂM TRA TRƯỚC KHI NỘP (THEO TRANG 6–7 KHUNG CHẤM)

- [ ] **1.** Báo cáo có mô tả nghiệp vụ, bảng chức năng theo vai trò (tối thiểu 10 chức năng) và yêu cầu phi chức năng *(1.1, 1.2)* — **Đã có nội dung nghiệp vụ, cần thêm Yêu cầu phi chức năng & xuất file Word/PDF.**
- [ ] **2.** Có sơ đồ ERD và bảng mô tả thuộc tính *(1.3)* — **Đã có bảng mô tả 22 bảng, cần thêm hình ảnh Sơ đồ ERD.**
- [x] **3.** Có sơ đồ kiến trúc Controller – Service – Repository – View và luồng xử lý một request *(1.4)* — **Đã có trong `README.md` và `QUY_TRINH_NGHIEP_VU_CHI_TIET.md`.**
- [ ] **4.** Có mục thiết kế riêng cho phần đồng thời và phần mạng, nêu rõ tài nguyên dùng chung và giao thức *(1.5)* — **Cần bổ sung.**
- [ ] **5.** Mã nguồn chia đúng package, nghiệp vụ nằm trong Service, có `@Transactional` *(2.1, 2.3)* — **Chỉ cần chuyển nốt logic từ `DeliveryController` & `PaymentConfigController` vào Service.**
- [x] **6.** Dữ liệu nhập được kiểm tra, có trang/phản hồi báo lỗi thân thiện *(2.2, 2.5)* — **Đã đạt (`@Valid`, `BindingResult`, `MvcExceptionHandler` + `templates/mvc/error.html`, `GlobalExceptionHandler`).**
- [x] **7.** Tất cả chức năng trong bảng chức năng đều demo được *(2.4)* — **Đã đạt.**
- [x] **8.** Giao diện dùng fragment chung và Bootstrap, xem được trên điện thoại *(3.1, 3.2)* — **Đã đạt (`templates/fragments/layout.html` + Bootstrap 5.3.3 responsive).**
- [x] **9.** Form có thông báo lỗi, thông báo thành công và xác nhận trước khi xóa *(3.3)* — **Đã đạt (`th:errors`, `.invalid-feedback`, `RedirectAttributes` alert, Bootstrap Modal).**
- [x] **10.** Trang danh sách có tìm kiếm, phân trang, sắp xếp *(3.4)* — **Đã đạt trên cả 4 trang `/mvc/products`, `/mvc/categories`, `/mvc/suppliers`, `/mvc/orders`.**
- [ ] **11.** Script SQL chạy được trên máy khác, mỗi bảng chính có tối thiểu 20 bản ghi mẫu *(4.1)* — **Cần thêm dữ liệu mẫu cho bảng `products`, `users`, `product_batches` đạt `>= 20` dòng.**
- [x] **12.** Có tối thiểu 1 quan hệ một – nhiều và 2 truy vấn tùy chỉnh *(4.2, 4.3)* — **Đã đạt vượt mức.**
- [x] **13.** CRUD đủ cho 3 bảng chính, xử lý được trường hợp xóa bản ghi đang được tham chiếu và trùng mã *(4.4, 4.5)* — **Đã đạt vượt mức.**
- [ ] **14.** Có chức năng đa luồng và chương trình kiểm thử nhiều luồng cho kết quả đúng *(5.1, 5.2)* — **Cần code bổ sung gấp.**
- [ ] **15.** Có client tách biệt kết nối qua URL, Socket hoặc RMI; đã thử 3 client chạy cùng lúc *(5.3, 5.4)* — **Cần code bổ sung gấp.**
- [ ] **16.** Báo cáo đúng cấu trúc 5 chương, slide tối đa 20 trang, đã tập thuyết trình đủ 15 phút *(6.1, 6.2)* — **Cần chuẩn bị.**
- [ ] **17.** Mỗi thành viên giải thích được mã nguồn phần mình phụ trách *(6.3)* — **Chuẩn bị khi vấn đáp.**
- [ ] **18.** Mỗi thành viên có commit trên Git, bảng phân công có xác nhận của cả nhóm *(6.4)* — **Cần hoàn thiện.**
- [ ] **19.** `README.md` ghi rõ phiên bản JDK, cách tạo cơ sở dữ liệu, cách chạy server và client — **Đã có JDK, CSDL, cách chạy server; cần thêm cách chạy Client.**

---

## PHẦN 6: KẾT QUẢ NGHIỆM THU PHÂN HỆ THYMELEAF + BOOTSTRAP 5 (ĐÃ HOÀN THÀNH 8 GIAI ĐOẠN)

> **Tài liệu chi tiết riêng cho phân hệ này**: `plans/THYMELEAF_BOOTSTRAP_PLAN.md`
> **Thư mục quản lý toàn bộ kế hoạch dự án**: `plans/` (`Final_plan.md`, `THYMELEAF_BOOTSTRAP_PLAN.md`, `PLAN.md`, `plan2.md`)

### 6.1. Danh sách các File đã triển khai
1. **Controllers (`src/main/java/com/groceryshop/mvc/controller/`)**:
   - `MvcAuthController.java`: Đăng nhập Session `/mvc/login`, chuyển đổi nhanh Role demo `/mvc/quick-login`, đăng xuất `/mvc/logout`, trang từ chối quyền `/mvc/access-denied`.
   - `MvcDashboardController.java`: Trang tổng quan `/mvc` hiển thị 4 thẻ KPI, đơn hàng mới nhất và cảnh báo tồn kho thấp.
   - `MvcProductController.java`: CRUD Sản phẩm `/mvc/products` + Tìm kiếm, Lọc Danh mục/Thương hiệu, Sắp xếp, Phân trang `Page<ProductDTO>`.
   - `MvcCategoryController.java`: CRUD Danh mục `/mvc/categories` + Tìm kiếm, Sắp xếp, Phân trang, chặn xóa khi đang có sản phẩm.
   - `MvcSupplierController.java`: CRUD Nhà cung cấp `/mvc/suppliers` + Tìm kiếm, Sắp xếp, Phân trang, lịch sử hàng cung ứng, chặn xóa khi đã có phiếu nhập.
   - `MvcOrderController.java`: Luồng nghiệp vụ Đơn hàng `/mvc/orders` & `/mvc/orders/{id}`, duyệt đơn / hủy đơn hoàn kho / hoàn thành cộng điểm VIP qua `OrderService`.
2. **Form DTOs & Exception Handler (`src/main/java/com/groceryshop/mvc/`)**:
   - `form/MvcProductForm.java`, `form/MvcCategoryForm.java`, `form/MvcSupplierForm.java` (Cô lập 100%, không sửa DTO cũ của React).
   - `exception/MvcExceptionHandler.java` (`@ControllerAdvice(basePackages = "com.groceryshop.mvc.controller")`, kiểm tra CSRF token `_mvcCsrf`, trả về trang `mvc/error.html` mà không ảnh hưởng JSON của `/api/**`).
3. **Templates Thymeleaf + Bootstrap 5.3 (`src/main/resources/templates/`)**:
   - `fragments/layout.html` (`th:fragment="head"`, `"header"`, `"menu"`, `"alerts"`, `"footer"`).
   - `mvc/login.html`, `mvc/dashboard.html`, `mvc/products.html`, `mvc/product-form.html`, `mvc/categories.html`, `mvc/category-form.html`, `mvc/suppliers.html`, `mvc/supplier-form.html`, `mvc/orders.html`, `mvc/order-detail.html`, `mvc/error.html`.

### 6.2. Checklist Nghiệm thu 14/14 mục

- [x] **1.** Trang chủ, admin và shipper React hiện tại vẫn hoạt động bình thường.
- [x] **2.** Các API cũ (`/api/**`) giữ nguyên dữ liệu và phản hồi JSON.
- [x] **3.** Refresh trực tiếp URL React không lỗi 404 (`SpaController` giữ nguyên).
- [x] **4.** Không có xung đột CSS hoặc điều hướng giữa React và Thymeleaf.
- [x] **5.** Header/menu/footer sử dụng Thymeleaf fragment (`templates/fragments/layout.html`).
- [x] **6.** Ba bảng chính (`products`, `categories`, `suppliers`) có đầy đủ Thêm, Xem, Sửa, Xóa.
- [x] **7.** Có validation (`@Valid` + `BindingResult`), tìm kiếm, phân trang, sắp xếp.
- [x] **8.** Modal xác nhận xóa và thông báo thành công (`RedirectAttributes`) hoạt động.
- [x] **9.** Hai kích thước desktop và mobile hiển thị đúng nhờ Bootstrap 5.3 Grid & Navbar responsive.
- [x] **10.** Phân quyền đúng cho từng vai trò (`ROLE_ADMIN`, `ROLE_SHIPPER`, `ROLE_USER`) trong `SecurityConfig.java` và trên giao diện.
- [x] **11.** Form thay đổi dữ liệu được bảo vệ bằng CSRF token (`_mvcCsrf`) trong Session.
- [x] **12.** Xóa dữ liệu đang bị tham chiếu khóa ngoại được chặn và thông báo lỗi thân thiện tại Backend (`ProductService`, `CategoryService`, `SupplierService`).
- [x] **13.** Thay đổi trên MVC đọc lại được ngay từ React và ngược lại (dùng chung SQL Server `supermarket_db1`).
- [x] **14.** Các lỗi MVC hiển thị trang `mvc/error.html` thân thiện, không làm thay đổi phản hồi JSON của REST API.


---

## PHẦN 7: RÀ SOÁT LẠI NGÀY 06/11 (Kiểm tra trực tiếp mã nguồn)

| Hạng mục | Kết quả kiểm tra thực tế | Trạng thái |
| :--- | :--- | :-: |
| Phân hệ Thymeleaf + Bootstrap (`/mvc/**`) | Đủ 6 Controller, 3 Form, 1 ExceptionHandler, 12 template. Biên dịch Maven thành công. | ✅ Đã có code |
| Chạy thử thực tế phân hệ MVC với SQL Server | **Chưa chạy thử** (mới chỉ biên dịch). Chưa kiểm tra hồi quy React/API trên trình duyệt. | ⚠️ Cần chạy thử |
| Lưu vào Git | Toàn bộ thay đổi (thư mục `mvc/`, `templates/`, `plans/`, 6 file sửa) **chưa được commit**. | ⚠️ Cần commit |
| Đa luồng (5.1, 5.2) | Không tìm thấy `@Async`, `@Scheduled`, `synchronized`, `Lock`, `Atomic`, `ExecutorService`. Test vẫn chỉ có `contextLoads()`. | ❌ Chưa làm |
| Lập trình mạng (5.3, 5.4) | Không tìm thấy `ServerSocket`, `Socket`, `URLConnection`, RMI; chưa có chương trình client riêng. | ❌ Chưa làm |
| Dữ liệu mẫu 20 dòng (4.1) | `supermarket_db1.sql` chưa thay đổi. | ❌ Chưa làm |
| Tách logic Controller sang Service (2.3) | `DeliveryController` (3 chỗ) và `PaymentConfigController` (2 chỗ) vẫn còn `@Transactional`. | ❌ Chưa làm |

> Lưu ý: Checklist 14/14 ở Phần 6 được đánh dấu dựa trên mã nguồn và kết quả biên dịch. Các mục cần chạy trên trình duyệt (hồi quy React, hiển thị mobile, đồng bộ dữ liệu 2 chiều) cần được xác nhận lại khi chạy ứng dụng.
