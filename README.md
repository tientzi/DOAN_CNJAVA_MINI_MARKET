# 🛒 DỰ ÁN WEBSITE QUẢN LÝ SIÊU THỊ MINI & CHUỖI BÁN LẺ THỰC PHẨM TƯƠI SẠCH (MINIMART)

Chào mừng bạn đến với dự án **Hệ Thống Quản Lý & Bán Hàng Siêu Thị Mini Trực Tuyến (MiniMart Fresh & Convenience)**. Đây là giải pháp phần mềm thương mại điện tử và quản trị bán lẻ toàn diện, được xây dựng theo mô hình **Fullstack Decoupled Architecture** kết hợp giữa nền tảng **Java Spring Boot 3.2 (RESTful API)**, cơ sở dữ liệu **Microsoft SQL Server**, giao diện hiện đại **React 18 SPA (Vite)** và trợ lý ảo thông minh **Google Gemini AI**.

Hệ thống được thiết kế đặc thù cho các chuỗi siêu thị mini, cửa hàng tiện lợi, tạp hóa và thực phẩm tươi sạch; đáp ứng trọn vẹn quy trình nghiệp vụ: từ khách hàng đặt hàng trực tuyến, tích điểm thành viên VIP, quản lý kho hàng nâng cao theo lô & hạn sử dụng, nhập hàng từ nhà cung cấp, xuất hóa đơn bán lẻ, quản lý thanh toán QR động, cho đến tự động điều phối shipper giao hàng theo từng quận/huyện.

---

## 📑 MỤC LỤC

1. [ Công Nghệ Sử Dụng](#️-công-nghệ-sử-dụng)
2. [ Kiến Trúc & Luồng Hoạt Động](#️-kiến-trúc--luồng-hoạt-động)
3. [ Tính Năng Nổi Bật](#-tính-năng-nổi-bật)
4. [ Chi Tiết Các Phân Hệ Chức Năng](#-chi-tiết-các-phân-hệ-chức-năng)
   - [1. Phân Hệ Khách Hàng (User / Client)](#1-phân-hệ-khách-hàng-user--client)
   - [2. Phân Hệ Quản Trị Viên (Admin Console)](#2-phân-hệ-quản-trị-viên-admin-console)
   - [3. Phân Hệ Nhân Viên Giao Hàng (Shipper Console)](#3-phân-hệ-nhân-viên-giao-hàng-shipper-console)
5. [ Cấu Trúc Cơ Sở Dữ Liệu (Database Schema)](#️-cấu-trúc-cơ-sở-dữ-liệu-database-schema)
6. [ Cấu Trúc Thư Mục Dự Án](#-cấu-trúc-thư-mục-dự-án)
7. [ Tài Khoản Trải Nghiệm Mặc Định](#-tài-khoản-trải-nghiệm-mặc-định)
8. [ Hướng Dẫn Cài Đặt & Khởi Chạy](#-hướng-dẫn-cài-đặt--khởi-chạy)
9. [ Danh Sách RESTful API Chính](#-danh-sách-restful-api-chính)
10. [ Cấu Hình Môi Trường & Lưu Ý Quan Trọng](#️-cấu-hình-môi-trường--lưu-ý-quan-trọng)

---

## 🛠️ Công Nghệ Sử Dụng

### 1. Backend (RESTful Web Services)
* **Ngôn ngữ**: Java 21 (LTS).
* **Framework**: Spring Boot 3.2.0.
* **Bảo mật**: Spring Security 6, Stateless JWT Authentication (`jjwt 0.11.5`), Remember-me Token.
* **Đăng nhập mạng xã hội**: Spring Boot Starter OAuth2 Client (Đăng nhập 1 chạm với Google & Facebook).
* **Tương tác dữ liệu**: Spring Data JPA, Hibernate ORM (Hỗ trợ chuẩn hóa chuỗi Unicode `NVARCHAR`).
* **Tiện ích & Mapper**: Project Lombok (`1.18.34`), Jakarta Validation, RestTemplate.
* **Build Tool**: Apache Maven (`mvnw` đi kèm).

### 2. Frontend (Single Page Application - SPA)
* **Framework**: React 18.2.
* **Build Tool**: Vite 5.0 (tốc độ HMR cực nhanh, tối ưu hóa bundle).
* **Điều hướng**: React Router DOM v6 (Nested Routes, Route Guards, ProtectedRoute theo vai trò).
* **HTTP Client**: Axios (Cấu hình Interceptor tự động đính kèm JWT Bearer Token).
* **Biểu đồ & Thống kê**: Recharts (Vẽ biểu đồ AreaChart, BarChart, PieChart doanh thu và đơn hàng).
* **Icon Library**: Lucide React.
* **Giao diện & Trải nghiệm**: CSS3 hiện đại (Glassmorphism, CSS Variables, Flexbox/Grid, Responsive Mobile/Desktop).

### 3. Database & Lưu Trữ
* **Hệ quản trị CSDL**: Microsoft SQL Server (2012, 2016, 2019, 2022).
* **JDBC Driver**: `mssql-jdbc` tương thích cấu hình bảo mật TLS 1.0 - TLS 1.2.
* **Lưu trữ tệp**: Local File System Storage (Thư mục `/uploads` lưu trữ ảnh sản phẩm, danh mục và mã QR thanh toán).

### 4. Trí Tuệ Nhân Tạo (AI)
* **Google Gemini AI API**: Tích hợp mô hình Gemini để tư vấn mua sắm trực tuyến, giải đáp thắc mắc, gợi ý sản phẩm giá tốt và khuyến mãi.
* **Offline Fallback Engine**: Tự động nhận diện từ khóa và gợi ý sản phẩm ngay cả khi chưa cấu hình Gemini API Key.

---

## 🏗️ Kiến Trúc Ứng Dụng & Mô Hình Spring Boot MVC

Dự án được xây dựng dựa trên nền tảng **Spring Boot MVC** cốt lõi kết hợp mô hình phân tách độc lập hiện đại (**Decoupled Client-Server / Single Page Application**). Hệ thống tối ưu hóa toàn diện trải nghiệm người dùng so với mô hình MVC nguyên khối truyền thống, đồng thời giữ vững tính chuẩn mực, an toàn và dễ mở rộng của một ứng dụng doanh nghiệp chuẩn Java.

### 1. Phân Tích Các Thành Phần Mô Hình MVC Trong Dự Án

Mô hình **MVC (Model - View - Controller)** trong dự án được tổ chức bài bản như sau:

```
+---------------------------------------------------------------------------------------------------+
|                                        VIEW (Giao Diện)                                           |
|   React 18 SPA (Client-Side Rendering) - Quản lý giao diện, trạng thái (State) & Router mượt mà   |
+-------------------------------------------------+-------------------------------------------------+
                                                  |  (Gọi bất đồng bộ HTTP Request kèm Bearer JWT)
                                                  v
+---------------------------------------------------------------------------------------------------+
|                                    CONTROLLER (Bộ Điều Phối)                                      |
|   Spring Boot MVC Core: DispatcherServlet -> Security Filter -> 24 REST Controllers (@RestController)   |
|   - Định tuyến endpoint, xác thực phân quyền, Validate dữ liệu và phản hồi dữ liệu chuẩn JSON    |
+-------------------------------------------------+-------------------------------------------------+
                                                  |  (Gọi xử lý nghiệp vụ)
                                                  v
+---------------------------------------------------------------------------------------------------+
|                                      MODEL (Dữ Liệu & Nghiệp Vụ)                                   |
|   - Tầng DTO: Đóng gói và chuẩn hóa dữ liệu trao đổi (Data Transfer Objects)                     |
|   - Tầng Service: Xử lý toàn bộ logic nghiệp vụ (Tồn kho, VIP Loyalty, Shipper, Gemini AI...)    |
|   - Tầng Repository & Entity: Spring Data JPA / Hibernate tương tác với Microsoft SQL Server     |
+---------------------------------------------------------------------------------------------------+
```

#### 📦 Model (M - Dữ Liệu & Nghiệp Vụ)
Nằm trọn vẹn tại tầng Backend Java Spring Boot:
* **Entities (`com.groceryshop.entity.*`)**: 22 lớp thực thể ánh xạ trực tiếp với các bảng CSDL SQL Server thông qua JPA/Hibernate (VD: `Product`, `Order`, `Inventory`, `ProductBatch`, `User`,...).
* **Repositories (`com.groceryshop.repository.*`)**: Kế thừa `JpaRepository` của Spring Data JPA, cung cấp sẵn các phương thức CRUD và truy vấn nâng cao (`findBy...`, `@Query`).
* **Services (`com.groceryshop.service.*`)**: Đóng gói toàn bộ logic nghiệp vụ cốt lõi của siêu thị:
  - Tự động trừ kho và lưu vết sổ cái kho khi đơn hàng được xác nhận.
  - Phân loại hạng thẻ và tính toán chiết khấu khách hàng VIP Loyalty.
  - Thuật toán tự động nhận diện khu vực và điều phối đơn hàng cho Shipper theo quận.
  - Xử lý xả hàng cận date và kết nối API Google Gemini AI.
* **DTOs (`com.groceryshop.dto.*`)**: Đóng vai trò lớp vỏ bọc an toàn, chỉ truyền tải dữ liệu cần thiết giữa Controller và View, che giấu các thông tin nhạy cảm của Model CSDL.

#### 🎮 Controller (C - Bộ Điều Khiển)
Sử dụng nền tảng **Spring MVC** (`spring-boot-starter-web`) với bộ điều phối trung tâm `DispatcherServlet`:
* **24 REST Controllers (`com.groceryshop.controller.*`)**: Được đánh dấu bằng `@RestController`. Tiếp nhận các yêu cầu HTTP (GET, POST, PUT, PATCH, DELETE) từ View, kiểm tra dữ liệu đầu vào (`@Valid`), xác thực phân quyền với Spring Security (`@PreAuthorize`) và gọi tầng Service tương ứng.
* **Định Dạng Dữ Liệu Trao Đổi**: Dữ liệu phản hồi được tuần tự hóa tự động (Serialization) thành chuẩn **JSON**, giúp hệ thống nhẹ, nhanh và dễ dàng mở rộng sang các nền tảng khác như Mobile App.
* **SPA Fallback Controller ([SpaController.java](file:///d:/NAM%20CUOI/CNJAVA/DOAN_COVEON=%29%29/Mini_mart-main_CNJAVA/src/main/java/com/groceryshop/controller/SpaController.java))**: Sử dụng annotation `@Controller` của Spring MVC để định tuyến tất cả các route giao diện người dùng về file `index.html` của React khi người dùng F5 hoặc gõ trực tiếp URL.

#### 🖥️ View (V - Giao Diện Người Dùng)
Được hiện đại hóa hoàn toàn bằng **React 18 Single Page Application (SPA)**:
* Thay thế cơ chế render HTML cũ kỹ tại máy chủ (Server-Side Rendering với JSP hay Thymeleaf), View của hệ thống chạy trực tiếp trên trình duyệt của người dùng (Client-Side Rendering).
* **Ưu điểm vượt trội**: Chuyển trang tức thì mà không cần tải lại toàn bộ trang web (Zero Page Reload), trạng thái giỏ hàng và dữ liệu người dùng được lưu trữ và phản hồi tức thời nhờ React Context API (`AuthContext`, `CartContext`).
* View giao tiếp 100% với Controller thông qua các cuộc gọi bất đồng bộ (**Asynchronous AJAX / Axios**).

---

### 2. So Sánh Mô Hình Spring Boot MVC Truyền Thống vs Mô Hình Của Dự Án

| Tiêu Chí Đánh Giá | Spring Boot MVC Cổ Điển (JSP / Thymeleaf) | Mô Hình Spring Boot MVC Hiện Đại Của Dự Án (REST API + React SPA) |
| :--- | :--- | :--- |
| **Cơ chế Render View** | Máy chủ render ra file HTML tĩnh rồi gửi về Client (Server-Side Rendering). | Trình duyệt Client tự render động dựa trên dữ liệu JSON (Client-Side Rendering). |
| **Tốc độ chuyển trang** | Mỗi thao tác chuyển trang đều khiến trình duyệt tải lại từ đầu (chớp trắng màn hình). | Chuyển trang mượt mà tức thì bằng React Router DOM v6, không tải lại trang. |
| **Tải trọng máy chủ** | Máy chủ vừa phải tính toán logic vừa phải gánh việc sinh mã HTML giao diện. | Máy chủ chỉ tập trung xử lý dữ liệu và phản hồi JSON nhẹ, tiết kiệm đáng kể RAM/CPU và băng thông. |
| **Khả năng mở rộng** | Rất khó tái sử dụng để làm ứng dụng di động (Mobile App iOS / Android). | **Cực kỳ linh hoạt**: Tầng REST API sẵn sàng phục vụ song song cho cả Web, Mobile App và hệ thống bên ngoài. |
| **Độ bảo mật** | Dùng Session lưu trên bộ nhớ server (dễ cạn kiệt tài nguyên khi đông người dùng). | Sử dụng **Stateless JWT Token** chuẩn doanh nghiệp, an toàn và dễ dàng mở rộng theo chiều ngang. |

---

### 3. Sơ Đồ Luồng Xử Lý Yêu Cầu Chi Tiết (Request Processing Flow)

```
[ Người Dùng Thao Tác Trên Giao Diện React SPA ]
                       │
                       ▼ (Gửi HTTP Request + Bearer JWT qua Axios)
         [ Spring Security Filter Chain ]
                       │ (Xác thực JWT Token & Phân quyền Role: ADMIN / SHIPPER / USER)
                       ▼
            [ Spring MVC DispatcherServlet ]
                       │ (Ánh xạ request đến đúng Controller xử lý)
                       ▼
              [ @RestController ]
                       │ (Validate dữ liệu đầu vào với @Valid)
                       ▼
                 [ Service Layer ]
                       │ (Thực thi các nghiệp vụ: Đơn hàng, Tồn kho, VIP Loyalty...)
                       ▼
                [ Repository Layer ]
                       │ (Spring Data JPA / Hibernate)
                       ▼
            [ Microsoft SQL Server DB ]
                       │ (Thực thi SQL và trả dữ liệu Entity)
                       ▼
  [ Service chuyển đổi Entity sang DTO & Trả về Controller ]
                       │
                       ▼ (Serialization)
 [ Phản hồi JSON Response về Client qua mã HTTP 200 OK / 201 Created... ]
                       │
                       ▼
[ React 18 cập nhật State và Re-render Component mượt mà trên màn hình ]
```

---

### 4. Hai Chế Độ Vận Hành Hệ Thống

1. **Chế độ Tích Hợp Đóng Gói (Production / Integrated Mode)**:
   - Toàn bộ mã nguồn React được biên dịch (build) thành các file tĩnh HTML/CSS/JS nằm trong thư mục `src/main/resources/static`.
   - Spring Boot khởi chạy ứng dụng Web toàn diện trên duy nhất một cổng `http://localhost:8080`, phục vụ trọn vẹn cả tầng REST API lẫn giao diện người dùng.
2. **Chế độ Phát Triển Tách Biệt (Development Mode)**:
   - Backend chạy Spring Boot tại `http://localhost:8080`.
   - Frontend chạy Vite Dev Server tại `http://localhost:5173` với tính năng Hot Module Replacement (HMR) cập nhật code tức thì.
   - Vite Proxy được cấu hình sẵn sàng chuyển tiếp toàn bộ request `/api` và `/uploads` sang port 8080 mà không gặp bất kỳ rào cản CORS nào.

---

## ✨ Tính Năng Nổi Bật

1. **Quản Lý Lô Hàng & Hạn Sử Dụng (Product Batches & Clearance Sale)**:
   - Theo dõi chi tiết số lô (`batch_number`), ngày sản xuất (`manufacturing_date`) và hạn sử dụng (`expiry_date`) của từng mặt hàng tươi sống/tiêu dùng.
   - Cảnh báo tự động các sản phẩm cận date (sắp hết hạn trong 30 ngày, 60 ngày).
   - **Xả hàng cận date (Clearance Sale)**: Hỗ trợ giảm giá % hoặc thiết lập mức giá bán xả hàng đặc biệt để kích cầu tiêu thụ trước hạn.
2. **Điều Phối Vận Chuyển Thông Minh (Smart Delivery Dispatching)**:
   - Tự động nhận diện Quận/Huyện từ chuỗi địa chỉ nhận hàng của khách hàng (VD: Quận Tân Phú, Tân Bình, Quận 12,...).
   - Tính năng **Tự động phân bổ đơn**: Gán các đơn hàng cần giao cho Shipper phụ trách từng khu vực địa lý tương ứng chỉ với 1 cú click.
   - Hỗ trợ phân công shipper thủ công (đơn lẻ hoặc hàng loạt) và theo dõi tình trạng đơn giao thất bại để tái điều phối.
3. **Quản Lý Hóa Đơn & In Ấn Bán Lẻ (Invoice Manager)**:
   - Tra cứu hóa đơn theo ngày, phương thức thanh toán, trạng thái thanh toán.
   - **In hóa đơn bán lẻ trực tiếp**: Định dạng hóa đơn mini mart chuẩn khổ giấy, sẵn sàng in tại quầy thu ngân.
   - **Xuất dữ liệu ra Excel/CSV**: Phục vụ công tác kế toán và kiểm toán định kỳ.
4. **Hệ Thống Thành Viên & Tích Điểm VIP Loyalty**:
   - 4 Hạng thẻ thành viên: **Đồng (BRONZE) -> Bạc (SILVER) -> Vàng (GOLD) -> Kim Cương (DIAMOND)**.
   - Tích điểm lũy tiến sau mỗi đơn hàng hoàn thành.
   - Hưởng chiết khấu trực tiếp trên đơn hàng (lên tới 8%) cùng đặc quyền miễn phí giao hàng toàn quốc.
   - Trang thành viên hiển thị thẻ VIP số, voucher độc quyền và quyền lợi riêng biệt.
5. **Cấu Hình Cổng Thanh Toán Đa Kênh & Upload Mã QR Động**:
   - Cho phép Admin Bật/Tắt các phương thức thanh toán: COD, Chuyển khoản ngân hàng (VietQR), Ví MoMo, VNPAY.
   - Hỗ trợ tải trực tiếp ảnh mã QR thanh toán từ Admin và hiển thị tức thì trên trang Checkout của khách.
6. **Trợ Lý AI Tư Vấn Bán Hàng (Google Gemini AI & Fallback)**:
   - Trò chuyện tự nhiên, tư vấn sản phẩm giá tốt nhất, thực phẩm xanh VietGAP, gợi ý công thức nấu ăn và giới thiệu mã giảm giá đang kích hoạt.
7. **Tin Nhắn Chăm Sóc Khách Hàng Trực Tiếp (Direct Customer Support)**:
   - Widget chat tích hợp sẵn trên trang người dùng giúp gửi phản hồi/yêu cầu hỗ trợ tới ban quản trị.
   - Giao diện Admin quản lý hội thoại tập trung, thông báo số lượng tin nhắn chưa đọc và phản hồi trực tiếp.
8. **Đăng Nhập Đa Dạng (OAuth2 Social Login + JWT)**:
   - Đăng nhập tài khoản truyền thống bằng Username/Email + Password.
   - Đăng nhập 1 chạm bằng tài khoản **Google** hoặc **Facebook** qua giao thức chuẩn OAuth2.

---

## 👥 Chi Tiết Các Phân Hệ Chức Năng

### 1. Phân Hệ Khách Hàng (User / Client)

* **Trang Chủ (Home)**:
  - Banner trượt quảng bá khuyến mãi và sự kiện nổi bật.
  - Danh sách danh mục sản phẩm trực quan với icon và hình ảnh bắt mắt.
  - Khối sản phẩm bán chạy, sản phẩm giảm giá sốc (Flash Sale/Clearance) và sản phẩm tươi sống mới về.
* **Danh Mục & Tìm Kiếm Sản Phẩm (Product List)**:
  - Tìm kiếm sản phẩm theo tên theo thời gian thực.
  - Bộ lọc đa chiều: Lọc theo danh mục, thương hiệu, khoảng giá (`minPrice` - `maxPrice`).
  - Sắp xếp linh hoạt: Giá tăng dần, giá giảm dần, mới nhất, tên A-Z.
* **Chi Tiết Sản Phẩm (Product Detail)**:
  - Xem bộ sưu tập hình ảnh sản phẩm.
  - Hiển thị giá gốc, giá khuyến mãi, phần trăm tiết kiệm, tồn kho khả dụng.
  - Xem thông tin nguồn gốc xuất xứ, hạn sử dụng và đánh giá sao kèm bình luận của khách mua trước.
  - Nút thêm vào giỏ hàng hoặc mua ngay.
* **Giỏ Hàng Thông Minh (Shopping Cart)**:
  - Điều chỉnh số lượng từng sản phẩm (tự động kiểm tra trần tồn kho thực tế).
  - Tự động tính toán tạm tính, tiền giảm giá và tổng thanh toán.
  - Xóa sản phẩm hoặc xóa toàn bộ giỏ hàng.
* **Đặt Hàng & Thanh Toán (Checkout)**:
  - Quản lý sổ địa chỉ giao hàng (chọn địa chỉ có sẵn hoặc nhập địa chỉ mới).
  - Nhập mã giảm giá (Coupon Code) để được giảm thêm tiền đơn hàng.
  - Tự động áp dụng chiết khấu đặc quyền thành viên VIP Loyalty (Giảm giá % theo hạng thẻ & Free Ship).
  - Lựa chọn phương thức thanh toán:
    - Tiền mặt khi nhận hàng (COD).
    - Quét mã VietQR chuyển khoản (hiển thị mã QR ngân hàng và hướng dẫn nội dung chuyển tiền).
    - Ví điện tử MoMo / Cổng VNPAY.
* **Lịch Sử Đơn Hàng (Order History)**:
  - Theo dõi lộ trình đơn hàng: `CHỜ XÁC NHẬN` ➔ `ĐÃ XÁC NHẬN` ➔ `ĐANG GIAO` ➔ `HOÀN THÀNH` (hoặc `ĐÃ HỦY`).
  - Xem chi tiết từng món hàng, địa chỉ nhận hàng và hình thức thanh toán.
  - Cho phép hủy đơn hàng nếu đơn vẫn ở trạng thái chờ xác nhận.
* **Trang Thành Viên VIP (Loyalty Portal)**:
  - Hiển thị thẻ VIP cá nhân với giao diện sang trọng (Bronze, Silver, Gold, Diamond).
  - Tiến độ thăng hạng và số điểm tích lũy hiện có.
  - Kho voucher độc quyền dành riêng cho từng cấp bậc thành viên.
* **Trang Cá Nhân & Sổ Địa Chỉ (Profile)**:
  - Cập nhật thông tin cá nhân: Họ tên, số điện thoại, email, mật khẩu.
  - Quản lý danh sách địa chỉ giao hàng tiện lợi.
* **Tương Tác & Hỗ Trợ Khách Hàng**:
  - ChatBot tư vấn bằng AI Google Gemini hoạt động 24/7.
  - Khung gửi tin nhắn góp ý & hỗ trợ kỹ thuật trực tiếp tới Admin.

---

### 2. Phân Hệ Quản Trị Viên (Admin Console)

Truy cập tại: `/admin` (Yêu cầu quyền `ROLE_ADMIN`).

#### A. Nhóm Báo Cáo & Thống Kê
* **Báo Cáo Tổng Quan (Report Manager)**:
  - Thống kê doanh thu theo các mốc: 7 ngày qua, 30 ngày qua, năm nay hoặc khoảng ngày tùy chọn.
  - Biểu đồ tương tác (Recharts): Xu hướng doanh thu, cơ cấu đơn hàng theo phương thức thanh toán.
  - Chỉ số KPI: Tổng doanh thu, tổng số đơn đặt, tỷ lệ giao hàng thành công, giá trị đơn hàng trung bình.

#### B. Nhóm Quản Lý Đơn Hàng & Vận Chuyển
* **Quản Lý Đơn Hàng (Order Manager)**:
  - Danh sách toàn bộ đơn hàng của siêu thị với bộ lọc trạng thái và ngày đặt.
  - Xác nhận đơn, chuyển đơn hàng cho bộ phận kho chuẩn bị và bàn giao vận chuyển.
  - Hủy đơn hàng và ghi nhận lý do.
* **Điều Phối Giao Hàng (Delivery Manager)**:
  - Theo dõi danh sách shipper trực thuộc kèm khu vực phụ trách và số đơn đang giao.
  - Tự động nhận diện quận/huyện từ địa chỉ giao hàng.
  - Điều phối tự động (Auto-dispatch) đơn hàng về đúng Shipper phụ trách địa bàn (Quận Tân Phú, Tân Bình, Quận 12,...).
  - Phân công thủ công đơn lẻ hoặc hàng loạt.
  - Theo dõi đơn hàng đang giao và các đơn giao thất bại để liên hệ xử lý kịp thời.

#### C. Nhóm Thanh Toán & Hóa Đơn
* **Quản Lý Hóa Đơn (Invoice Manager)**:
  - Tra cứu hóa đơn chi tiết: Mã hóa đơn, khách hàng, số điện thoại, ngày lập, hình thức thanh toán.
  - Chức năng in hóa đơn bán lẻ chuyên nghiệp (hỗ trợ in nhiệt / in A4-A5).
  - Xuất toàn bộ hoặc kết quả lọc hóa đơn ra file CSV/Excel.
* **Cấu Hình Cổng Thanh Toán (Payment Manager)**:
  - Danh sách phương thức thanh toán: COD, VietQR, Ví MoMo, VNPAY.
  - Bật/tắt phương thức tức thì.
  - Upload ảnh mã QR thanh toán ngân hàng trực tiếp từ máy tính lên hệ thống.

#### D. Nhóm Sản Phẩm & Kho Hàng
* **Quản Lý Danh Mục (Category Manager)**:
  - Thêm, sửa, xóa danh mục sản phẩm; tải ảnh đại diện danh mục.
* **Quản Lý Sản Phẩm (Product Manager)**:
  - Thêm mới, cập nhật sản phẩm; quản lý giá gốc, giá bán khuyến mãi, đơn vị tính.
  - Tải lên nhiều hình ảnh cho một sản phẩm.
  - Bật/tắt trạng thái kinh doanh của sản phẩm.
* **Quản Lý Lô Hàng & Hạn Sử Dụng (Product Batch Manager)**:
  - Quản lý danh sách lô hàng: Số lô, ngày sản xuất, hạn sử dụng, số lượng ban đầu và số lượng hiện tại.
  - Bộ lọc sản phẩm sắp hết hạn trong 30 ngày, 60 ngày.
  - **Xả hàng cận date**: Thiết lập giảm giá % hoặc giá bán riêng cho sản phẩm sắp hết date.
* **Quản Lý Tồn Kho (Inventory Manager)**:
  - Giám sát số lượng tồn thực tế của từng sản phẩm.
  - Cảnh báo sản phẩm chạm ngưỡng tồn tối thiểu (sắp hết hàng).
* **Sổ Cái Kho / Thẻ Kho (Inventory Ledger Manager)**:
  - Ghi nhận lịch sử chi tiết mọi biến động kho: Nhập kho từ NCC, Xuất hàng theo đơn khách đặt, Điều chỉnh cân kho.
* **Phiếu Nhập Kho (Goods Receipt Manager)**:
  - Lập phiếu nhập kho từ Nhà cung cấp.
  - Chọn sản phẩm, số lượng nhập, đơn giá nhập và tạo lô hàng mới.
  - Duyệt hoàn tất phiếu nhập: Tự động cộng tồn kho và sinh thẻ kho tương ứng.
* **Quản Lý Nhà Cung Cấp & Thương Hiệu (Supplier Manager)**:
  - Quản lý hồ sơ nhà cung cấp: Tên công ty, số điện thoại, email, địa chỉ, mã số thuế.
  - Quản lý danh mục thương hiệu liên kết.

#### E. Nhóm Khách Hàng & Tiếp Thị
* **Quản Lý Mã Giảm Giá (Coupon Manager)**:
  - Tạo mới voucher giảm giá theo tỷ lệ (%) hoặc trừ tiền mặt trực tiếp (VNĐ).
  - Cấu hình giá trị đơn hàng tối thiểu, mức giảm tối đa, giới hạn số lần dùng và thời hạn hiệu lực.
* **Quản Lý Người Dùng (User Manager)**:
  - Xem danh sách toàn bộ khách hàng, shipper và quản trị viên.
  - Khóa hoặc mở khóa tài khoản; phân quyền tài khoản (ROLE_USER, ROLE_ADMIN, ROLE_SHIPPER).
  - Xem hạng thành viên VIP và số điểm tích lũy của từng người dùng.
* **Quản Lý Đánh Giá (Review Manager)**:
  - Duyệt và kiểm soát các bình luận, phản hồi của khách hàng đối với sản phẩm.
* **Hộp Thư Khách Hàng (Customer Support Manager)**:
  - Tiếp nhận tin nhắn gửi từ widget phản hồi của khách hàng.
  - Phản hồi trực tiếp tới từng khách hàng ngay trên giao diện quản trị.

---

### 3. Phân Hệ Nhân Viên Giao Hàng (Shipper Console)

Truy cập tại: `/shipper` (Yêu cầu quyền `ROLE_SHIPPER` hoặc `ROLE_ADMIN`).

* **Bảng Điều Khiển Giao Hàng (Shipper Dashboard)**:
  - Thống kê nhanh: Số đơn đang cần giao, số đơn giao thành công hôm nay, tổng tiền COD cần nộp về quỹ.
* **Danh Sách Đơn Hàng Phụ Trách**:
  - Danh sách các đơn hàng đã được Admin điều phối theo quận/huyện của shipper.
  - Xem đầy đủ thông tin: Tên khách hàng, số điện thoại bấm gọi ngay, địa chỉ chi tiết, phương thức thanh toán (COD cần thu bao nhiêu tiền).
* **Cập Nhật Tiến Trình Giao Hàng**:
  - Chuyển trạng thái: `ĐÃ NHẬN ĐƠN` ➔ `ĐANG GIAO HÀNG`.
  - Xác nhận **Giao hàng thành công**: Ghi nhận hoàn thành và cập nhật trạng thái đơn hàng.
  - Báo cáo **Giao hàng thất bại**: Nhập lý do cụ thể (Khách không nghe máy, sai địa chỉ, khách hẹn lại ngày khác...) để Admin nắm bắt và điều phối lại.

---

## 🗄️ Cấu Trúc Cơ Sở Dữ Liệu (Database Schema)

Hệ thống sử dụng cơ sở dữ liệu **Microsoft SQL Server** với 22 bảng được thiết kế chuẩn hóa và toàn vẹn tham chiếu khóa ngoại:

| STT | Tên Bảng | Mục Đích & Nghiệp Vụ |
| :--- | :--- | :--- |
| 1 | `roles` | Danh sách vai trò hệ thống (`ROLE_ADMIN`, `ROLE_USER`, `ROLE_SHIPPER`). |
| 2 | `users` | Tài khoản người dùng, mật khẩu mã hóa, thông tin cá nhân, hạng VIP và điểm tích lũy. |
| 3 | `categories` | Danh mục sản phẩm (Rau Củ Quả, Thực Phẩm Tươi Sống, Đồ Uống,...). |
| 4 | `brands` | Thương hiệu sản phẩm (Vinamilk, CP, Barona, Masan,...). |
| 5 | `suppliers` | Danh sách nhà cung cấp hàng hóa (tên, MST, số điện thoại, địa chỉ). |
| 6 | `products` | Thông tin sản phẩm, mã vạch, giá gốc, giá khuyến mãi, đơn vị tính, mô tả. |
| 7 | `product_images` | Danh sách hình ảnh chi tiết của từng sản phẩm. |
| 8 | `product_batches` | Quản lý lô hàng, ngày sản xuất, hạn sử dụng và số lượng của từng đợt nhập. |
| 9 | `inventory` | Quản lý số lượng tồn kho thực tế của từng sản phẩm tại siêu thị. |
| 10 | `goods_receipt` | Thông tin phiếu nhập kho hàng từ nhà cung cấp. |
| 11 | `goods_receipt_items` | Chi tiết các mặt hàng, số lượng và đơn giá nhập trong phiếu nhập kho. |
| 12 | `inventory_ledger` | Sổ cái kho ghi nhận toàn bộ biến động xuất/nhập/tồn kho. |
| 13 | `coupons` | Mã khuyến mãi, voucher giảm giá, hạn mức sử dụng và ngày hiệu lực. |
| 14 | `addresses` | Sổ địa chỉ nhận hàng của khách hàng. |
| 15 | `cart` | Giỏ hàng của từng người dùng. |
| 16 | `cart_items` | Chi tiết sản phẩm và số lượng nằm trong giỏ hàng. |
| 17 | `orders` | Đơn hàng, tổng tiền, chiết khấu VIP, trạng thái xử lý, thông tin người nhận, shipper phân công. |
| 18 | `order_items` | Chi tiết từng sản phẩm, đơn giá và số lượng trong đơn hàng. |
| 19 | `payment_method_configs` | Cấu hình bật/tắt các phương thức thanh toán và lưu trữ URL mã QR thanh toán. |
| 20 | `payments` | Lịch sử và trạng thái giao dịch thanh toán của từng đơn hàng. |
| 21 | `reviews` | Đánh giá số sao và bình luận sản phẩm của khách hàng. |
| 22 | `customer_messages` | Tin nhắn trao đổi hai chiều giữa khách hàng và ban quản trị siêu thị. |

---

## 📂 Cấu Trúc Thư Mục Dự Án

```
Mini_mart-main_CNJAVA/
├── pom.xml                                  # File cấu hình Maven, dependencies và build plugin
├── mvnw / mvnw.cmd                          # Maven Wrapper cho Windows và Linux
├── supermarket_db1_JAVA_100_PERCENT_FIXED.sql # File script khởi tạo toàn bộ CSDL SQL Server chuẩn
├── seed_50_historical_orders_hcm.sql        # Script nạp 50 đơn hàng lịch sử thực tế tại TP.HCM
├── node-portable/                           # Thư mục Node.js portable v20 đi kèm tiện lợi
│   └── node-v20.19.2-win-x64/
├── uploads/                                 # Thư mục lưu trữ hình ảnh upload thực tế
│
├── src/main/java/com/groceryshop/           # Mã nguồn Backend (Java Spring Boot 3.2)
│   ├── GroceryShopApplication.java          # Main Application Class
│   ├── config/                              # Cấu hình Spring Boot
│   │   ├── DataInitializer.java            # Tự động nạp dữ liệu mẫu khi khởi chạy
│   │   └── WebConfig.java                  # Cấu hình CORS & Static Resource Handlers
│   ├── security/                            # Kiến trúc bảo mật Spring Security 6
│   │   ├── SecurityConfig.java              # Cấu hình phân quyền endpoints & bộ lọc JWT
│   │   ├── JwtTokenProvider.java            # Tạo, giải mã và xác thực JWT token
│   │   ├── JwtAuthenticationFilter.java     # Filter chặn bắt Authorization Bearer header
│   │   ├── UserPrincipal.java               # Đối tượng UserDetails
│   │   └── oauth2/                          # Xử lý Social Login Google / Facebook
│   ├── controller/                          # 25 RESTful Controllers xử lý API
│   │   ├── AuthController.java              # Đăng ký, đăng nhập JWT, đổi mật khẩu
│   │   ├── ProductController.java           # Quản lý & Lọc sản phẩm
│   │   ├── CategoryController.java          # Quản lý danh mục
│   │   ├── BrandController.java             # Quản lý thương hiệu
│   │   ├── CartController.java              # Giỏ hàng
│   │   ├── OrderController.java             # Đặt hàng, chi tiết đơn hàng
│   │   ├── DeliveryController.java          # Điều phối giao hàng & tự động phân shipper
│   │   ├── ShipperController.java           # Bảng điều khiển dành riêng cho shipper
│   │   ├── GoodsReceiptController.java      # Phiếu nhập kho
│   │   ├── ProductBatchController.java      # Quản lý Lô & Hạn sử dụng, xả hàng cận date
│   │   ├── InventoryController.java         # Tồn kho thực tế
│   │   ├── InventoryLedgerController.java   # Sổ cái biến động kho
│   │   ├── SupplierController.java          # Nhà cung cấp
│   │   ├── PaymentConfigController.java     # Cấu hình thanh toán & Upload mã QR
│   │   ├── ReportController.java            # Báo cáo thống kê doanh thu
│   │   ├── UserController.java              # Quản lý người dùng, phân quyền, VIP Loyalty
│   │   ├── CouponController.java            # Quản lý mã giảm giá
│   │   ├── ReviewController.java            # Đánh giá sản phẩm
│   │   ├── ChatController.java              # ChatBot AI Gemini
│   │   ├── CustomerMessageController.java   # Tin nhắn CSKH trực tiếp
│   │   ├── RestoreController.java           # API nạp/khôi phục dữ liệu tức thì
│   │   └── SpaController.java               # Forwarding route cho Single Page Application
│   ├── entity/                              # 22 Thực thể JPA tương ứng với các bảng DB
│   ├── repository/                          # Giao diện Spring Data JPA Repositories
│   ├── service/                             # Tầng xử lý nghiệp vụ kinh doanh (Business Logic)
│   ├── dto/                                 # Data Transfer Objects
│   └── exception/                           # Xử lý ngoại lệ tập trung (Global Exception Handler)
│
├── src/main/resources/
│   ├── application.properties               # File cấu hình CSDL, cổng, JWT Secret, Gemini Key
│   └── static/                              # Chứa toàn bộ static bundle được build từ React
│
└── frontend/                                # Mã nguồn Frontend (React 18 SPA + Vite)
    ├── package.json                         # Dependencies Frontend (React, Axios, Recharts, Lucide)
    ├── vite.config.js                       # Cấu hình Vite & Proxy chuyển tiếp sang port 8080
    ├── index.html                           # File HTML gốc của SPA
    └── src/
        ├── App.jsx                          # Cấu hình BrowserRouter, Routes và Context Providers
        ├── main.jsx                         # Điểm vào của ứng dụng React
        ├── contexts/                        # State Management (AuthContext, CartContext)
        ├── services/api.js                  # Axios Instance kèm Interceptor gắn JWT token
        ├── layouts/                         # Layout dùng chung
        │   ├── UserLayout.jsx               # Layout website khách hàng (Header, Footer, Chatbot)
        │   └── AdminLayout.jsx              # Layout trang quản trị & Shipper (Sidebar, Topbar)
        ├── pages/                           # Các trang giao diện người dùng
        │   ├── Home.jsx                     # Trang chủ mua sắm
        │   ├── ProductList.jsx              # Trang danh sách sản phẩm & bộ lọc
        │   ├── ProductDetail.jsx            # Chi tiết sản phẩm
        │   ├── Cart.jsx                     # Giỏ hàng
        │   ├── Checkout.jsx                 # Đặt hàng & thanh toán QR
        │   ├── OrderHistory.jsx             # Lịch sử đơn hàng
        │   ├── Loyalty.jsx                  # Cổng khách hàng thân thiết VIP Loyalty
        │   ├── Profile.jsx                  # Thông tin cá nhân & sổ địa chỉ
        │   ├── Login.jsx / Register.jsx     # Đăng nhập & Đăng ký
        │   ├── admin/                       # 16 trang quản trị chuyên sâu
        │   │   ├── ReportManager.jsx        # Báo cáo doanh thu & biểu đồ
        │   │   ├── OrderManager.jsx         # Quản lý đơn hàng
        │   │   ├── DeliveryManager.jsx      # Điều phối vận chuyển thông minh
        │   │   ├── InvoiceManager.jsx       # Quản lý hóa đơn & In ấn & Xuất CSV
        │   │   ├── PaymentManager.jsx       # Cấu hình thanh toán & Upload mã QR
        │   │   ├── ProductManager.jsx       # Quản lý sản phẩm
        │   │   ├── ProductBatchManager.jsx  # Quản lý Lô & Hạn sử dụng, xả hàng
        │   │   ├── GoodsReceiptManager.jsx  # Quản lý phiếu nhập kho
        │   │   ├── InventoryManager.jsx     # Quản lý tồn kho
        │   │   ├── InventoryLedgerManager.jsx # Sổ cái xuất nhập tồn
        │   │   ├── SupplierManager.jsx      # Quản lý nhà cung cấp
        │   │   ├── CategoryManager.jsx      # Quản lý danh mục
        │   │   ├── CouponManager.jsx        # Quản lý mã giảm giá
        │   │   ├── UserManager.jsx          # Quản lý người dùng & VIP
        │   │   └── ReviewManager.jsx        # Quản lý đánh giá
        │   └── shipper/
        │       └── ShipperDashboard.jsx     # Bảng điều khiển riêng cho Shipper
        ├── components/                      # Các thành phần tái sử dụng
        │   ├── ChatBot.jsx                  # Widget trò chuyện AI Gemini
        │   ├── CustomerFeedbackWidget.jsx   # Widget nhắn tin hỗ trợ trực tiếp
        │   └── ProtectedRoute.jsx           # Bảo vệ tuyến đường theo quyền người dùng
        └── utils/                           # Hàm tiện ích (In ấn hóa đơn, Xuất CSV, tìm kiếm)
```

---

## 🔑 Tài Khoản Trải Nghiệm Mặc Định

Hệ thống đã chuẩn bị sẵn các tài khoản demo tương ứng với 3 vai trò người dùng trong hệ thống (Mật khẩu mặc định là: `pass1234`):

| Vai Trò | Tên Đăng Nhập | Mật Khẩu | Họ Tên & Khu Vực / Đặc Quyền | Quyền Hạn (Role) |
| :--- | :--- | :--- | :--- | :--- |
| **Quản Trị Viên** | `admin` | `pass1234` | Quản Trị Viên Hệ Thống | `ROLE_ADMIN` |
| **Shipper 1** | `shipper1` | `pass1234` | Nguyễn Văn Giao (Phụ trách Quận Tân Phú) | `ROLE_SHIPPER` |
| **Shipper 2** | `shipper2` | `pass1234` | Trần Văn Tốc (Phụ trách Quận Tân Bình) | `ROLE_SHIPPER` |
| **Shipper 3** | `shipper3` | `pass1234` | Lê Hoàng Vũ (Phụ trách Quận 12) | `ROLE_SHIPPER` |
| **Khách Hàng VIP 1** | `user1` | `pass1234` | Nguyễn Văn An (**Hạng Kim Cương - DIAMOND**) | `ROLE_USER` |
| **Khách Hàng VIP 2** | `user2` | `pass1234` | Trần Thị Bình (**Hạng Vàng - GOLD**) | `ROLE_USER` |
| **Khách Hàng VIP 3** | `user3` | `pass1234` | Lê Hoàng Cường (**Hạng Bạc - SILVER**) | `ROLE_USER` |
| **Khách Hàng VIP 4** | `user4` | `pass1234` | Phạm Thu Dung (**Hạng Đồng - BRONZE**) | `ROLE_USER` |

---

## 🚀 Hướng Dẫn Cài Đặt & Khởi Chạy

### 1. Yêu Cầu Môi Trường
* **Java Development Kit (JDK)**: Phiên bản **Java 21 LTS** (Khuyến nghị Eclipse Temurin, Amazon Corretto hoặc Oracle JDK 21).
* **Cơ sở dữ liệu**: **Microsoft SQL Server** (2012 trở lên).
* **Node.js**: Phiên bản 18+ hoặc 20+ (hoặc có thể sử dụng trực tiếp bản Node portable có sẵn tại thư mục `node-portable`).
* **Trình quản lý gói**: npm hoặc yarn.

---

### 2. Thiết Lập Cơ Sở Dữ Liệu

1. Mở công cụ **SQL Server Management Studio (SSMS)** hoặc Azure Data Studio.
2. Kết nối tới SQL Server của bạn (mặc định cổng `1433`).
3. Mở file [supermarket_db1_JAVA_100_PERCENT_FIXED.sql](file:///d:/NAM%20CUOI/CNJAVA/DOAN_COVEON=%29%29/Mini_mart-main_CNJAVA/supermarket_db1_JAVA_100_PERCENT_FIXED.sql) và nhấn **Execute (F5)**.
   *(Script sẽ tự động tạo cơ sở dữ liệu `supermarket_db1` cùng đầy đủ 22 bảng và cấu trúc khóa ngoại hoàn chỉnh)*.
4. Mở file [seed_50_historical_orders_hcm.sql](file:///d:/NAM%20CUOI/CNJAVA/DOAN_COVEON=%29%29/Mini_mart-main_CNJAVA/seed_50_historical_orders_hcm.sql) và nhấn **Execute (F5)** để đồng bộ thông tin 50 đơn hàng lịch sử thực tế tại 3 quận TP.HCM.
5. Kiểm tra và chỉnh sửa tài khoản/mật khẩu kết nối CSDL trong file [application.properties](file:///d:/NAM%20CUOI/CNJAVA/DOAN_COVEON=%29%29/Mini_mart-main_CNJAVA/src/main/resources/application.properties):
   ```properties
   spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=supermarket_db1;encrypt=true;trustServerCertificate=true
   spring.datasource.username=sa
   spring.datasource.password=123
   ```

> 💡 **Mẹo Nhanh (Khôi Phục Dữ Liệu Tức Thì)**: Sau khi chạy ứng dụng, bạn có thể truy cập đường dẫn `http://localhost:8080/api/public/restore-data` trên trình duyệt. Hệ thống sẽ tự động dọn dẹp và nạp lại toàn bộ dữ liệu mẫu chuẩn Unicode tiếng Việt 100%.

---

### 3. Khởi Chạy Dự Án

Có hai phương thức khởi chạy dự án:

#### Cách 1: Chạy Tích Hợp Fullstack trên 1 Cổng duy nhất (Cổng 8080) - Khuyên Dùng

Bước này sẽ build mã nguồn React SPA thành file tĩnh đóng gói cùng ứng dụng Spring Boot:

1. **Build Frontend**:
   Mở terminal tại thư mục gốc của dự án:
   ```bash
   cd frontend
   npm install
   npm run build
   cd ..
   ```
   *(Các file sản phẩm sẽ được tự động xuất vào thư mục `src/main/resources/static`)*.

2. **Chạy Backend**:
   Tại thư mục gốc:
   ```bash
   # Sử dụng Maven Wrapper:
   ./mvnw clean spring-boot:run

   # Hoặc trên Windows PowerShell:
   .\mvnw.cmd spring-boot:run
   ```
   *(Hoặc chạy trực tiếp file `GroceryShopApplication.java` trong IntelliJ IDEA, Eclipse, VS Code)*.

3. **Truy cập ứng dụng**:
   * Trang người dùng: `http://localhost:8080`
   * Trang đăng nhập: `http://localhost:8080/login`
   * Trang quản trị Admin: `http://localhost:8080/admin`
   * Bảng điều khiển Shipper: `http://localhost:8080/shipper`

---

#### Cách 2: Chạy Độc Lập Phục Vụ Phát Triển (Development Mode - Live Reload)

Nếu bạn muốn chỉnh sửa giao diện và xem kết quả tức thì với tính năng Vite Hot Module Replacement (HMR):

1. **Khởi chạy Backend (Terminal 1)**:
   ```bash
   .\mvnw.cmd spring-boot:run
   ```
   Backend khởi chạy tại: `http://localhost:8080`.

2. **Khởi chạy Frontend (Terminal 2)**:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Frontend khởi chạy tại: `http://localhost:5173`.
   *(Mọi request `/api` và hình ảnh `/uploads` sẽ được Vite tự động chuyển tiếp về Backend port 8080)*.

---

## 📡 Danh Sách RESTful API Chính

| Nhóm Chức Năng | Phương Thức | Endpoint URL | Phân Quyền | Mô Tả Nghiệp Vụ |
| :--- | :--- | :--- | :--- | :--- |
| **Xác Thực (Auth)** | `POST` | `/api/auth/login` | Public | Đăng nhập hệ thống, trả về Bearer JWT token |
| | `POST` | `/api/auth/register` | Public | Đăng ký tài khoản khách hàng mới |
| | `GET` | `/api/auth/me` | Authenticated | Lấy thông tin tài khoản đang đăng nhập |
| **Sản Phẩm** | `GET` | `/api/public/products` | Public | Danh sách sản phẩm, lọc theo danh mục, giá, từ khóa |
| | `GET` | `/api/public/products/{id}` | Public | Lấy chi tiết thông tin 1 sản phẩm theo ID |
| | `POST` | `/api/admin/products` | ADMIN | Thêm mới sản phẩm |
| | `PUT` | `/api/admin/products/{id}` | ADMIN | Cập nhật thông tin sản phẩm |
| **Giỏ Hàng (Cart)** | `GET` | `/api/cart` | Authenticated | Lấy danh sách sản phẩm trong giỏ hàng hiện tại |
| | `POST` | `/api/cart/add` | Authenticated | Thêm sản phẩm vào giỏ hàng |
| | `PUT` | `/api/cart/update` | Authenticated | Cập nhật số lượng sản phẩm trong giỏ |
| | `DELETE` | `/api/cart/item/{id}` | Authenticated | Xóa 1 sản phẩm khỏi giỏ hàng |
| **Đơn Hàng (Order)** | `POST` | `/api/orders` | Authenticated | Tạo đơn hàng mới từ giỏ hàng |
| | `GET` | `/api/orders/my-orders` | Authenticated | Lịch sử đơn hàng của khách hàng đang đăng nhập |
| | `GET` | `/api/admin/orders` | ADMIN | Danh sách toàn bộ đơn hàng hệ thống |
| | `PATCH` | `/api/admin/orders/{id}/status` | ADMIN | Cập nhật trạng thái xử lý đơn hàng |
| **Vận Chuyển & Shipper** | `GET` | `/api/admin/deliveries/shippers` | ADMIN | Lấy danh sách shipper và khu vực phụ trách |
| | `POST` | `/api/admin/deliveries/assign` | ADMIN | Phân công shipper cho đơn hàng |
| | `POST` | `/api/admin/deliveries/auto-assign` | ADMIN | **Tự động điều phối đơn hàng theo quận** |
| | `GET` | `/api/shipper/orders/my-deliveries` | SHIPPER | Danh sách đơn hàng được gán cho shipper đang đăng nhập |
| | `PATCH` | `/api/shipper/orders/{id}/status` | SHIPPER | Cập nhật trạng thái giao (Thành công / Thất bại) |
| **Lô Hàng & Hạn Sử Dụng** | `GET` | `/api/admin/batches` | ADMIN | Danh sách tất cả các lô hàng |
| | `GET` | `/api/admin/batches/expiring` | ADMIN | Danh sách lô hàng sắp hết hạn (cận date) |
| | `POST` | `/api/admin/batches/{id}/clearance-sale` | ADMIN | **Áp dụng xả hàng giảm giá cận date** |
| **Nhập Kho & Sổ Cái** | `GET` | `/api/admin/goods-receipts` | ADMIN | Danh sách phiếu nhập kho |
| | `POST` | `/api/admin/goods-receipts` | ADMIN | Tạo phiếu nhập kho mới từ nhà cung cấp |
| | `PUT` | `/api/admin/goods-receipts/{id}/complete` | ADMIN | Hoàn tất phiếu nhập và tự động cập nhật tồn kho |
| | `GET` | `/api/admin/inventory-ledger` | ADMIN | Xem sổ cái xuất/nhập/tồn kho |
| **Hóa Đơn & Thanh Toán**| `GET` | `/api/public/payment-methods` | Public | Lấy các phương thức thanh toán đang bật |
| | `POST` | `/api/admin/payment-methods/upload-qr` | ADMIN | Tải lên mã QR chuyển khoản ngân hàng/MoMo |
| **Báo Cáo (Reports)** | `GET` | `/api/admin/reports/overview` | ADMIN | Báo cáo doanh thu, đơn hàng theo mốc thời gian |
| **Trợ Lý Trí Tuệ Nhân Tạo**| `POST` | `/api/public/chat` | Public | Gửi tin nhắn trò chuyện với Trợ lý ảo Google Gemini |
| **Chăm Sóc Khách Hàng** | `POST` | `/api/messages/send` | Authenticated | Khách hàng gửi tin nhắn phản hồi |
| | `GET` | `/api/admin/messages/conversations` | ADMIN | Admin xem danh sách hội thoại khách hàng |
| | `POST` | `/api/admin/messages/conversations/{id}/reply` | ADMIN | Admin trả lời tin nhắn của khách hàng |

---

## ⚙️ Cấu Hình Môi Trường & Lưu Ý Quan Trọng

### 1. Cấu Hình Google Gemini AI
Hệ thống đã tích hợp sẵn khóa API Gemini mã hóa Base64 trong file `application.properties`:
```properties
gemini.api.key.base64=QVEuQWI4Uk42TEZMSWd1dnpYSTFGU1Radl83V3NBM2xXLTEwVTAxZW9qQXp0UGdKZVhLLXc=
```
Nếu bạn muốn sử dụng API Key riêng của bạn:
1. Đăng ký lấy API Key tại [Google AI Studio](https://aistudio.google.com/).
2. Đặt biến môi trường hệ thống: `GEMINI_API_KEY=your_key_here` hoặc mã hóa Base64 và điền vào thuộc tính `gemini.api.key.base64`.

### 2. Cấu Hình Đăng Nhập Mạng Xã Hội Google OAuth2
Để bật tính năng "Đăng nhập bằng Google":
1. Truy cập [Google Cloud Console](https://console.cloud.google.com) ➔ Tạo OAuth 2.0 Client ID (Loại: Web application).
2. Thêm Authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`.
3. Điền thông tin vào `application.properties`:
   ```properties
   spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
   spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
   ```

### 3. Tương Thích SQL Server & Chuẩn Hóa Tiếng Việt
* Ứng dụng đã cấu hình `spring.jpa.properties.hibernate.use_nationalized_character_data=true`, đảm bảo tất cả chuỗi String được ánh xạ chuẩn sang kiểu dữ liệu `NVARCHAR` trong SQL Server, loại bỏ hoàn toàn lỗi hiển thị dấu tiếng Việt.
* Trình điều khiển JDBC được cấu hình tương thích cả với các phiên bản SQL Server yêu cầu giao thức mã hóa TLS 1.0/1.1/1.2.

---

## 🎯 Tổng Kết

Dự án **Website Quản Lý Siêu Thị Mini & Cửa Hàng Tiện Lợi (MiniMart)** là giải pháp công nghệ toàn diện kết hợp giữa khả năng xử lý nghiệp vụ mạnh mẽ, an toàn của Java Spring Boot và giao diện hiện đại, mượt mà của React SPA. Dự án không chỉ đáp ứng tốt yêu cầu đồ án môn học chuyên ngành Công Nghệ Java (CNJAVA) mà còn có tính ứng dụng thực tiễn cao trong quản lý chuỗi bán lẻ thực phẩm và siêu thị hiện đại.
