# 🛒 DỰ ÁN WEBSITE QUẢN LÝ SIÊU THỊ MINI & CHUỖI BÁN LẺ THỰC PHẨM TƯƠI SẠCH (MINIMART)

Chào mừng bạn đến với dự án **Hệ Thống Quản Lý & Bán Hàng Siêu Thị Mini Trực Tuyến (MiniMart Fresh & Convenience)**. Đây là giải pháp phần mềm thương mại điện tử và quản trị bán lẻ toàn diện, được xây dựng theo mô hình **Fullstack Decoupled Architecture** kết hợp giữa nền tảng **Java Spring Boot 3.2 (RESTful API)**, cơ sở dữ liệu **Microsoft SQL Server**, giao diện hiện đại **React 18 SPA (Vite)** và trợ lý ảo thông minh **Google Gemini AI**.

Hệ thống được thiết kế đặc thù cho các chuỗi siêu thị mini, cửa hàng tiện lợi, tạp hóa và thực phẩm tươi sạch; đáp ứng trọn vẹn quy trình nghiệp vụ: từ khách hàng đặt hàng trực tuyến, tích điểm thành viên VIP, quản lý kho hàng nâng cao theo lô & hạn sử dụng, **quy trình kiểm định chất lượng đầu vào (Inward QC)**, nhập hàng từ nhà cung cấp, xuất hóa đơn bán lẻ, quản lý thanh toán QR động, cho đến tự động điều phối shipper giao hàng theo từng quận/huyện.

---

## 📑 MỤC LỤC

1. [🛠️ Công Nghệ Sử Dụng](#️-công-nghệ-sử-dụng)
2. [🏗️ Kiến Trúc & Luồng Hoạt Động](#️-kiến-trúc-ứng-dụng--mô-hình-spring-boot-mvc)
3. [✨ Tính Năng Nổi Bật](#-tính-năng-nổi-bật)
4. [👥 Chi Tiết Các Phân Hệ Chức Năng](#-chi-tiết-các-phân-hệ-chức-năng)
   - [1. Phân Hệ Khách Hàng (User / Client)](#1-phân-hệ-khách-hàng-user--client)
   - [2. Phân Hệ Quản Trị Viên (Admin Console)](#2-phân-hệ-quản-trị-viên-admin-console)
   - [3. Phân Hệ Nhân Viên Giao Hàng (Shipper Console)](#3-phân-hệ-nhân-viên-giao-hàng-shipper-console)
5. [🗄️ Cấu Trúc Cơ Sở Dữ Liệu (Database Schema)](#️-cấu-trúc-cơ-sở-dữ-liệu-database-schema)
6. [📂 Cấu Trúc Thư Mục Dự Án](#-cấu-trúc-thư-mục-dự-án)
7. [🔑 Tài Khoản Trải Nghiệm Mặc Định](#-tài-khoản-trải-nghiệm-mặc-định)
8. [🚀 Hướng Dẫn Cài Đặt & Khởi Chạy](#-hướng-dẫn-cài-đặt--khởi-chạy)
9. [📡 Danh Sách RESTful API Chính](#-danh-sách-restful-api-chính)
10. [⚙️ Cấu Hình Môi Trường & Lưu Ý Quan Trọng](#️-cấu-hình-môi-trường--lưu-ý-quan-trọng)

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
|   - Tầng Service: Xử lý toàn bộ logic nghiệp vụ (Tồn kho, QC Inspection, VIP Loyalty, Shipper...)  |
|   - Tầng Repository & Entity: Spring Data JPA / Hibernate tương tác với Microsoft SQL Server     |
+---------------------------------------------------------------------------------------------------+
```

#### 📦 Model (M - Dữ Liệu & Nghiệp Vụ)
Nằm trọn vẹn tại tầng Backend Java Spring Boot:
* **Entities (`com.groceryshop.entity.*`)**: 22 lớp thực thể ánh xạ trực tiếp với các bảng CSDL SQL Server thông qua JPA/Hibernate (VD: `Product`, `Order`, `Inventory`, `ProductBatch`, `GoodsReceipt`, `GoodsReceiptItem`, `User`,...).
* **Repositories (`com.groceryshop.repository.*`)**: Kế thừa `JpaRepository` của Spring Data JPA, cung cấp sẵn các phương thức CRUD và truy vấn nâng cao (`findBy...`, `@Query`).
* **Services (`com.groceryshop.service.*`)**: Đóng gói toàn bộ logic nghiệp vụ cốt lõi của siêu thị:
  - Tự động trừ kho và lưu vết sổ cái kho khi đơn hàng được xác nhận.
  - **Quy trình Kiểm định chất lượng đầu vào (QC)**: Phân tách số lượng đạt chuẩn (tăng tồn kho và tạo lô) và số lượng không đạt chuẩn (trả về nhà cung cấp, ghi nhận lý do).
  - Khóa chặt tồn kho, tuyệt đối không cho phép nhập tay khi tạo mới hay chỉnh sửa thông tin sản phẩm.
  - Phân loại hạng thẻ và tính toán chiết khấu khách hàng VIP Loyalty.
  - Thuật toán tự động nhận diện khu vực và điều phối đơn hàng cho Shipper theo quận.
  - Xử lý xả hàng cận date và kết nối API Google Gemini AI.
* **DTOs (`com.groceryshop.dto.*`)**: Đóng vai trò lớp vỏ bọc an toàn, chỉ truyền tải dữ liệu cần thiết giữa Controller và View, che giấu các thông tin nhạy cảm của Model CSDL.

#### 🎮 Controller (C - Bộ Điều Khiển)
Sử dụng nền tảng **Spring MVC** (`spring-boot-starter-web`) với bộ điều phối trung tâm `DispatcherServlet`:
* **24 REST Controllers (`com.groceryshop.controller.*`)**: Tiếp nhận các yêu cầu HTTP (GET, POST, PUT, PATCH, DELETE) từ View, kiểm tra dữ liệu đầu vào (`@Valid`), xác thực phân quyền với Spring Security (`@PreAuthorize`) và gọi tầng Service tương ứng.
* **Định Dạng Dữ Liệu Trao Đổi**: Dữ liệu phản hồi được tuần tự hóa tự động (Serialization) thành chuẩn **JSON**, giúp hệ thống nhẹ, nhanh và dễ dàng mở rộng sang các nền tảng khác như Mobile App.
* **SPA Fallback Controller (`SpaController.java`)**: Định tuyến tất cả các route giao diện người dùng về file `index.html` của React khi người dùng F5 hoặc gõ trực tiếp URL.

#### 🖥️ View (V - Giao Diện Người Dùng)
Được hiện đại hóa hoàn toàn bằng **React 18 Single Page Application (SPA)**:
* Chạy trực tiếp trên trình duyệt của người dùng (Client-Side Rendering) thay vì Server-Side Rendering cũ kỹ.
* Chuyển trang mượt mà tức thì không cần tải lại toàn bộ trang web (Zero Page Reload), trạng thái giỏ hàng và dữ liệu người dùng được phản hồi tức thời nhờ React Context API (`AuthContext`, `CartContext`).
* View giao tiếp 100% với Controller thông qua các cuộc gọi bất đồng bộ (**Asynchronous AJAX / Axios**).

---

### 2. Sơ Đồ Luồng Xử Lý Yêu Cầu Chi Tiết (Request Processing Flow)

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
                       │ (Thực thi các nghiệp vụ: Đơn hàng, Tồn kho, QC Inspection, VIP...)
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

## ✨ Tính Năng Nổi Bật

1. **Quản Lý Chất Lượng Đầu Vào (Inward Quality Control - QC) & Chuẩn Hóa Tồn Kho**:
   - **Khóa nhập tay tồn kho**: Khi tạo mới hoặc cập nhật sản phẩm, ô số lượng tồn kho bị vô hiệu hóa (`disabled`) và mặc định bằng 0. Tồn kho chỉ được gia tăng chuẩn hóa qua quy trình nhập kho và kiểm định thực tế.
   - **Quy trình kiểm định QC 2 bước**:
     - Lập phiếu nhập kho từ Nhà cung cấp ➔ Trạng thái `CHỜ KIỂM ĐỊNH QC`.
     - Bộ phận kiểm hàng thực hiện đánh giá: ghi nhận số lượng đạt chuẩn ($Q_{passed}$) và số lượng không đạt chuẩn ($Q_{rejected}$) kèm lý do chi tiết (dập nát, hỏng bao bì, cận date...).
     - **Tự động cân đối**: Chỉ $Q_{passed}$ mới được tăng vào tồn kho khả dụng và tạo lô hàng hoạt động. $Q_{rejected}$ được hoàn trả nhà cung cấp và ghi vết minh bạch vào Thẻ kho.
     - Hỗ trợ xem lại và in Biên bản kiểm định chất lượng trực tiếp.
2. **Giao Diện Hợp Nhất Sản Phẩm & Danh Mục (Unified Products & Categories Console)**:
   - Gộp phân hệ Danh mục vào trang **Sản phẩm & Tồn kho** dưới dạng 2 Sub-tab trực quan (`?tab=products` và `?tab=categories`).
   - Giúp quản trị viên dễ dàng quản lý cây danh mục và tạo sản phẩm mới mà không cần chuyển đổi trang rườm rà. Menu sidebar được tinh gọn tối đa.
3. **Quản Lý Lô Hàng & Hạn Sử Dụng (Product Batches & Clearance Sale)**:
   - Theo dõi chi tiết số lô (`batch_number`), ngày sản xuất (`manufacturing_date`) và hạn sử dụng (`expiry_date`) của từng mặt hàng tươi sống/tiêu dùng.
   - Cảnh báo tự động các sản phẩm cận date (sắp hết hạn trong 30 ngày, 60 ngày).
   - **Xả hàng cận date (Clearance Sale)**: Thiết lập giảm giá % hoặc giá bán xả kho đặc biệt để kích cầu tiêu thụ trước hạn.
4. **Điều Phối Vận Chuyển Thông Minh (Smart Delivery Dispatching)**:
   - Tự động nhận diện Quận/Huyện từ chuỗi địa chỉ nhận hàng của khách hàng (VD: Quận Tân Phú, Tân Bình, Quận 12,...).
   - Tính năng **Tự động phân bổ đơn**: Gán các đơn hàng cần giao cho Shipper phụ trách từng địa bàn tương ứng chỉ với 1 cú click.
   - Hỗ trợ phân công shipper thủ công (đơn lẻ hoặc hàng loạt) và theo dõi tình trạng đơn giao thất bại để tái điều phối.
5. **Quản Lý Hóa Đơn & In Ấn Bán Lẻ (Invoice Manager)**:
   - Tra cứu hóa đơn theo ngày, phương thức thanh toán, trạng thái thanh toán.
   - **In hóa đơn bán lẻ trực tiếp**: Định dạng hóa đơn mini mart chuẩn khổ giấy, sẵn sàng in tại quầy thu ngân.
   - **Xuất dữ liệu ra Excel/CSV**: Phục vụ công tác kế toán và kiểm toán định kỳ.
6. **Hệ Thống Thành Viên & Tích Điểm VIP Loyalty**:
   - 4 Hạng thẻ thành viên: **Đồng (BRONZE) -> Bạc (SILVER) -> Vàng (GOLD) -> Kim Cương (DIAMOND)**.
   - Tích điểm lũy tiến sau mỗi đơn hàng hoàn thành.
   - Hưởng chiết khấu trực tiếp trên đơn hàng (lên tới 8%) cùng đặc quyền miễn phí giao hàng toàn quốc.
7. **Cấu Hình Cổng Thanh Toán Đa Kênh & Upload Mã QR Động**:
   - Bật/Tắt linh hoạt các phương thức: COD, Chuyển khoản ngân hàng (VietQR), Ví MoMo, VNPAY.
   - Hỗ trợ tải trực tiếp ảnh mã QR thanh toán từ Admin và hiển thị tức thì trên trang Checkout của khách.
8. **Trợ Lý AI Tư Vấn Bán Hàng (Google Gemini AI & Fallback)**:
   - Trò chuyện tự nhiên, tư vấn sản phẩm giá tốt nhất, thực phẩm xanh VietGAP, gợi ý công thức nấu ăn và giới thiệu mã giảm giá đang kích hoạt.
9. **Đăng Nhập Đa Dạng (OAuth2 Social Login + JWT)**:
   - Đăng nhập tài khoản truyền thống bằng Username/Email + Password.
   - Đăng nhập 1 chạm bằng tài khoản **Google** hoặc **Facebook** qua giao thức chuẩn OAuth2.

---

## 👥 Chi Tiết Các Phân Hệ Chức Năng

### 1. Phân Hệ Khách Hàng (User / Client)

* **Trang Chủ (Home)**:
  - Banner trượt quảng bá khuyến mãi và sự kiện nổi bật.
  - Danh mục sản phẩm trực quan với icon và hình ảnh bắt mắt.
  - Khối sản phẩm bán chạy, sản phẩm giảm giá sốc (Flash Sale/Clearance) và thực phẩm tươi sống mới về.
* **Danh Mục & Tìm Kiếm Sản Phẩm (Product List)**:
  - Tìm kiếm sản phẩm theo tên theo thời gian thực.
  - Bộ lọc đa chiều: Lọc theo danh mục, thương hiệu, khoảng giá (`minPrice` - `maxPrice`).
  - Sắp xếp linh hoạt: Giá tăng dần, giá giảm dần, mới nhất, tên A-Z.
* **Chi Tiết Sản Phẩm (Product Detail)**:
  - Xem bộ sưu tập hình ảnh sản phẩm chất lượng cao.
  - Hiển thị giá gốc, giá khuyến mãi, phần trăm tiết kiệm, tồn kho khả dụng.
  - Xem thông tin nguồn gốc xuất xứ, hạn sử dụng và đánh giá sao kèm bình luận.
* **Giỏ Hàng Thông Minh (Shopping Cart)**:
  - Điều chỉnh số lượng từng sản phẩm (tự động kiểm tra trần tồn kho thực tế).
  - Tự động tính toán tạm tính, tiền giảm giá và tổng thanh toán.
* **Đặt Hàng & Thanh Toán (Checkout)**:
  - Quản lý sổ địa chỉ giao hàng (chọn địa chỉ có sẵn hoặc nhập địa chỉ mới).
  - Nhập mã giảm giá (Coupon Code) để được giảm thêm tiền đơn hàng.
  - Tự động áp dụng chiết khấu đặc quyền thành viên VIP Loyalty (Giảm giá % theo hạng thẻ & Free Ship).
  - Thanh toán linh hoạt: COD, Quét mã VietQR chuyển khoản, Ví MoMo, VNPAY.
* **Lịch Sử Đơn Hàng (Order History)**:
  - Theo dõi lộ trình đơn: `CHỜ XÁC NHẬN` ➔ `ĐÃ XÁC NHẬN` ➔ `ĐANG GIAO` ➔ `HOÀN THÀNH` (hoặc `ĐÃ HỦY`).
  - Xem chi tiết từng món hàng, địa chỉ nhận hàng và hình thức thanh toán.
* **Trang Thành Viên VIP (Loyalty Portal)**:
  - Hiển thị thẻ VIP cá nhân với giao diện sang trọng (Bronze, Silver, Gold, Diamond).
  - Tiến độ thăng hạng, số điểm tích lũy và kho voucher độc quyền.
* **Trợ Lý AI & Chăm Sóc Khách Hàng**:
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
  - Toàn bộ danh sách đơn hàng với bộ lọc trạng thái và ngày đặt.
  - Xác nhận đơn hàng, chuẩn bị hàng và bàn giao cho đơn vị vận chuyển.
* **Điều Phối Giao Hàng (Delivery Manager)**:
  - Danh sách shipper trực thuộc kèm khu vực phụ trách và số đơn đang nhận.
  - Tự động nhận diện quận/huyện từ địa chỉ giao hàng.
  - Điều phối tự động (Auto-dispatch) đơn hàng về đúng Shipper phụ trách địa bàn.
  - Phân công thủ công đơn lẻ hoặc hàng loạt và xử lý đơn giao thất bại.

#### C. Nhóm Thanh Toán & Hóa Đơn
* **Quản Lý Hóa Đơn (Invoice Manager)**:
  - Tra cứu hóa đơn chi tiết: Mã hóa đơn, khách hàng, số điện thoại, ngày lập, phương thức thanh toán.
  - In hóa đơn bán lẻ chuyên nghiệp chuẩn siêu thị.
  - Xuất toàn bộ hoặc kết quả lọc hóa đơn ra file CSV/Excel.
* **Cấu Hình Cổng Thanh Toán (Payment Manager)**:
  - Bật/tắt các phương thức thanh toán (COD, VietQR, Ví MoMo, VNPAY).
  - Upload ảnh mã QR thanh toán ngân hàng trực tiếp từ máy tính lên hệ thống.

#### D. Nhóm Sản Phẩm & Kho Hàng
* **Quản Lý Sản Phẩm & Tồn Kho (Product Manager)**:
  - **Sub-tab 1: Sản phẩm & Tồn kho**: Quản lý sản phẩm, giá bán, giá vốn, tải ảnh, đơn vị tính. **Tồn kho bị khóa không cho sửa tay** (chỉ tăng qua nhập kho & QC).
  - **Sub-tab 2: Danh mục**: Quản lý danh mục sản phẩm, biểu tượng và hình ảnh đại diện, đồng bộ tự động.
* **Lịch Sử & Nhập Kho Nâng Cao (Warehouse History Manager)**:
  - **Tab 1: Phiếu nhập kho**: Lập phiếu nhập từ NCC, chọn sản phẩm, số lượng nhập, đơn giá và số lô dự kiến.
  - **Tab 2: Quản lý chất lượng đầu vào (Inward QC)**:
    - Tiếp nhận các phiếu nhập chờ kiểm định.
    - Ghi nhận số lượng đạt chuẩn ($Q_{passed}$) và số lượng không đạt chuẩn ($Q_{rejected}$) kèm lý do cụ thể.
    - Cập nhật tồn kho tự động theo đúng $Q_{passed}$, tạo lô hàng hoạt động. $Q_{rejected}$ được trả lại NCC và ghi nhận biên bản.
    - Hỗ trợ in Biên bản kiểm định chất lượng đầu vào.
  - **Tab 3: Sổ cái kho / Thẻ kho**: Tra cứu toàn bộ lịch sử biến động kho (Nhập hàng, Xuất bán, Trả hàng QC, Cân chỉnh).
* **Quản Lý Lô Hàng & Hạn Sử Dụng (Product Batch Manager)**:
  - Danh sách lô hàng: Số lô, ngày sản xuất, hạn sử dụng, số lượng ban đầu và tồn hiện tại.
  - Cảnh báo cận date 30 - 60 ngày; công cụ thiết lập xả hàng giảm giá cận date.
* **Quản Lý Nhà Cung Cấp & Thương Hiệu (Supplier Manager)**:
  - Quản lý hồ sơ nhà cung cấp: Tên công ty, MST, số điện thoại, email, địa chỉ.

#### E. Nhóm Khách Hàng & Tiếp Thị
* **Quản Lý Mã Giảm Giá (Coupon Manager)**:
  - Tạo mới voucher giảm giá theo tỷ lệ (%) hoặc trừ tiền mặt (VNĐ).
  - Cấu hình giá trị đơn tối thiểu, mức giảm tối đa và thời hạn hiệu lực.
* **Quản Lý Người Dùng (User Manager)**:
  - Xem danh sách toàn bộ khách hàng, shipper và quản trị viên.
  - Khóa/Mở khóa tài khoản, phân quyền, xem hạng thành viên VIP và điểm tích lũy.
* **Quản Lý Đánh Giá (Review Manager)**:
  - Duyệt và quản lý bình luận, số sao đánh giá sản phẩm từ người mua.
* **Hộp Thư Khách Hàng (Customer Support Manager)**:
  - Tiếp nhận và phản hồi tin nhắn trực tiếp với khách hàng ngay trên giao diện admin.

---

### 3. Phân Hệ Nhân Viên Giao Hàng (Shipper Console)

Truy cập tại: `/shipper` (Yêu cầu quyền `ROLE_SHIPPER` hoặc `ROLE_ADMIN`).

* **Bảng Điều Khiển Giao Hàng (Shipper Dashboard)**:
  - Thống kê nhanh: Đơn đang cần giao, đơn giao thành công hôm nay, tiền COD cần nộp về quỹ.
* **Danh Sách Đơn Hàng Phụ Trách**:
  - Các đơn hàng được Admin điều phối theo đúng quận/huyện của shipper.
  - Xem chi tiết: Tên khách hàng, số điện thoại bấm gọi ngay, địa chỉ chi tiết, tiền COD cần thu.
* **Cập Nhật Tiến Trình Giao Hàng**:
  - Chuyển trạng thái: `ĐÃ NHẬN ĐƠN` ➔ `ĐANG GIAO HÀNG`.
  - Xác nhận **Giao hàng thành công** hoặc báo cáo **Giao hàng thất bại** kèm lý do cụ thể.

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
| 11 | `goods_receipt_items` | Chi tiết phiếu nhập, đơn vị tính, số lượng nhập, đơn giá, **kết quả kiểm định QC (`passed_quantity`, `rejected_quantity`, `reject_reason`, `qc_status`, `qc_note`, `inspected_at`, `inspected_by`)**. |
| 12 | `inventory_ledger` | Sổ cái kho ghi nhận toàn bộ biến động xuất/nhập/tồn kho và lý do trả hàng QC. |
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
DO_AN_CONGNGHEJAVA/
├── pom.xml                                  # File cấu hình Maven, dependencies và build plugin
├── mvnw / mvnw.cmd                          # Maven Wrapper cho Windows và Linux
├── run.bat                                  # Script khởi chạy nhanh 1-Click trên Windows
├── supermarket_db1.sql                      # File script CSDL chuẩn: tạo 22 bảng, nạp hạt giống và 50 đơn hàng HCM
├── uploads/                                 # Thư mục lưu trữ hình ảnh upload thực tế
├── PLAN.md / plan2.md                       # Tài liệu kế hoạch phát triển và kiến trúc dự án
│
├── src/main/java/com/groceryshop/           # Mã nguồn Backend (Java Spring Boot 3.2)
│   ├── GroceryShopApplication.java          # Main Application Class
│   ├── config/                              # Cấu hình Spring Boot (DataInitializer, WebConfig)
│   ├── security/                            # Kiến trúc bảo mật Spring Security 6 & OAuth2 Google/Facebook
│   │   ├── SecurityConfig.java              # Cấu hình phân quyền endpoints & bộ lọc JWT
│   │   ├── JwtTokenProvider.java            # Tạo, giải mã và xác thực JWT token
│   │   └── JwtAuthenticationFilter.java     # Filter chặn bắt Authorization Bearer header
│   ├── controller/                          # 24 RESTful Controllers xử lý API
│   │   ├── AuthController.java              # Đăng ký, đăng nhập JWT, đổi mật khẩu
│   │   ├── ProductController.java           # Quản lý & Lọc sản phẩm (khóa tồn kho thủ công)
│   │   ├── CategoryController.java          # Quản lý danh mục
│   │   ├── GoodsReceiptController.java      # Phiếu nhập kho & API kiểm định chất lượng QC
│   │   ├── DeliveryController.java          # Điều phối giao hàng & tự động phân shipper theo quận
│   │   ├── ShipperController.java           # Bảng điều khiển dành riêng cho shipper
│   │   ├── ProductBatchController.java      # Quản lý Lô & Hạn sử dụng, xả hàng cận date
│   │   ├── InventoryController.java         # Tồn kho thực tế
│   │   ├── InventoryLedgerController.java   # Sổ cái biến động kho
│   │   ├── OrderController.java             # Đặt hàng, chi tiết đơn hàng
│   │   ├── ReportController.java            # Báo cáo thống kê doanh thu
│   │   ├── ChatController.java              # ChatBot AI Gemini
│   │   └── SpaController.java               # Forwarding route cho Single Page Application
│   ├── entity/                              # 22 Thực thể JPA tương ứng với các bảng DB
│   ├── repository/                          # Giao diện Spring Data JPA Repositories
│   ├── service/                             # Tầng xử lý nghiệp vụ kinh doanh (Business Logic)
│   │   ├── GoodsReceiptService.java         # Xử lý nhập kho & kiểm định QC đầu vào
│   │   ├── ProductService.java              # Xử lý sản phẩm (chuẩn hóa tồn ban đầu = 0)
│   │   ├── OrderService.java                # Xử lý đơn hàng & trừ kho FIFO
│   │   └── ...
│   ├── dto/                                 # Data Transfer Objects
│   │   ├── QCInspectionRequestDTO.java      # DTO tiếp nhận dữ liệu kiểm định QC
│   │   ├── GoodsReceiptItemDTO.java         # DTO chi tiết phiếu nhập kèm trường QC
│   │   └── ...
│   └── exception/                           # Xử lý ngoại lệ tập trung (Global Exception Handler)
│
├── src/main/resources/
│   ├── application.properties               # File cấu hình CSDL, cổng, JWT Secret, Gemini Key
│   └── static/                              # Chứa toàn bộ static bundle được build từ React (HTML/CSS/JS)
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
        ├── layouts/                         # UserLayout và AdminLayout
        ├── pages/                           # Các trang giao diện người dùng
        │   ├── Home.jsx, Cart.jsx, Checkout.jsx, OrderHistory.jsx, Loyalty.jsx, Profile.jsx...
        │   ├── admin/                       # Các trang quản trị chuyên sâu
        │   │   ├── ProductManager.jsx       # Quản lý Sản phẩm & Tồn kho (gồm Sub-tab Sản phẩm & Danh mục)
        │   │   ├── WarehouseHistoryManager.jsx # Quản lý Lịch sử & Nhập kho (Phiếu nhập, QC đầu vào, Sổ cái kho)
        │   │   ├── ProductBatchManager.jsx  # Quản lý Lô & Hạn sử dụng, xả hàng cận date
        │   │   ├── OrderManager.jsx         # Quản lý đơn hàng
        │   │   ├── DeliveryManager.jsx      # Điều phối vận chuyển thông minh theo quận
        │   │   ├── InvoiceManager.jsx       # Quản lý hóa đơn, in ấn và xuất CSV
        │   │   ├── PaymentManager.jsx       # Cấu hình thanh toán & Upload mã QR
        │   │   ├── ReportManager.jsx        # Báo cáo doanh thu & biểu đồ KPI
        │   │   ├── SupplierManager.jsx      # Quản lý nhà cung cấp
        │   │   ├── CouponManager.jsx        # Quản lý mã giảm giá
        │   │   ├── UserManager.jsx          # Quản lý người dùng & VIP Loyalty
        │   │   └── ReviewManager.jsx        # Quản lý đánh giá sản phẩm
        │   └── shipper/
        │       └── ShipperDashboard.jsx     # Bảng điều khiển riêng cho Shipper
        ├── components/                      # ChatBot AI Gemini, Feedback Widget, ProtectedRoute...
        └── utils/                           # Hàm tiện ích (In ấn hóa đơn, Xuất CSV, Format tiền tệ)
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
* **Java Development Kit (JDK)**: Phiên bản **Java 17** hoặc **Java 21 LTS** (Khuyến nghị Eclipse Temurin, Amazon Corretto hoặc Oracle JDK).
* **Cơ sở dữ liệu**: **Microsoft SQL Server** (2012 trở lên).
* **Node.js**: Phiên bản 18+ hoặc 20+ (Dùng để build frontend khi cần tùy biến).
* **Trình quản lý gói**: npm hoặc yarn.

---

### 2. Thiết Lập Cơ Sở Dữ Liệu

1. Mở công cụ **SQL Server Management Studio (SSMS)** hoặc Azure Data Studio.
2. Kết nối tới SQL Server của bạn (mặc định cổng `1433`).
3. Mở file [supermarket_db1.sql](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/supermarket_db1.sql) và nhấn **Execute (F5)**.
   *(Script sẽ tự động tạo cơ sở dữ liệu `supermarket_db1`, toàn bộ 22 bảng thực thể chuẩn JPA, khóa ngoại, chỉ mục, dữ liệu mẫu phân lô date/xả kho và 50 đơn hàng lịch sử thực tế TP.HCM)*.
4. Kiểm tra tài khoản/mật khẩu kết nối CSDL trong file [application.properties](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/src/main/resources/application.properties):
   ```properties
   spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=supermarket_db1;encrypt=true;trustServerCertificate=true
   spring.datasource.username=sa
   spring.datasource.password=123
   ```

---

### 3. Khởi Chạy Nhanh Dự Án

#### ⚡ Cách Khởi Chạy Nhanh Nhất (1-Click Run trên Windows)
Dự án đã tích hợp sẵn file script [run.bat](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/run.bat) tại thư mục gốc:
1. Double-click trực tiếp vào file **`run.bat`** (hoặc mở Command Prompt gõ `run.bat`).
2. Script sẽ tự động nhận diện `JAVA_HOME`, thiết lập mã hóa UTF-8 và khởi chạy ứng dụng Spring Boot.
3. Mở trình duyệt và truy cập:
   * Website Siêu thị: `http://localhost:8080`
   * Đăng nhập: `http://localhost:8080/login`
   * Trang Quản trị: `http://localhost:8080/admin`
   * Bảng Shipper: `http://localhost:8080/shipper`

---

#### Cách Khởi Chạy Chi Tiết Theo Nhu Cầu

##### A. Chạy Tích Hợp Đóng Gói (Production Mode - Cổng 8080)
Khi bạn có thay đổi mã nguồn trong thư mục `frontend` và muốn cập nhật lại bản build:
1. **Build Frontend**:
   ```bash
   cd frontend
   npm install
   npm run build
   cd ..
   ```
   *(Các file sản phẩm sẽ được tự động xuất vào thư mục `src/main/resources/static`)*.
2. **Khởi chạy Backend**:
   ```bash
   .\mvnw.cmd spring-boot:run
   ```

##### B. Chạy Độc Lập Phục Vụ Phát Triển (Development Mode - Live Reload)
Nếu bạn muốn vừa code giao diện vừa xem kết quả tức thì với Vite HMR:
1. **Terminal 1 (Backend)**:
   ```bash
   .\mvnw.cmd spring-boot:run
   ```
   *(Backend chạy tại `http://localhost:8080`)*.
2. **Terminal 2 (Frontend)**:
   ```bash
   cd frontend
   npm run dev
   ```
   *(Frontend chạy tại `http://localhost:5173`, tự động proxy API sang cổng 8080)*.

---

## 📡 Danh Sách RESTful API Chính

| Nhóm Chức Năng | Phương Thức | Endpoint URL | Phân Quyền | Mô Tả Nghiệp Vụ |
| :--- | :--- | :--- | :--- | :--- |
| **Xác Thực (Auth)** | `POST` | `/api/auth/login` | Public | Đăng nhập hệ thống, trả về Bearer JWT token |
| | `POST` | `/api/auth/register` | Public | Đăng ký tài khoản khách hàng mới |
| | `GET` | `/api/auth/me` | Authenticated | Lấy thông tin tài khoản đang đăng nhập |
| **Sản Phẩm & Danh Mục** | `GET` | `/api/public/products` | Public | Danh sách sản phẩm, lọc theo danh mục, giá, từ khóa |
| | `GET` | `/api/public/products/{id}` | Public | Lấy chi tiết thông tin 1 sản phẩm theo ID |
| | `POST` | `/api/admin/products` | ADMIN | Thêm mới sản phẩm (Tồn kho tự động gán = 0) |
| | `PUT` | `/api/admin/products/{id}` | ADMIN | Cập nhật sản phẩm (Khóa chỉnh sửa tồn kho trực tiếp) |
| | `GET` | `/api/public/categories` | Public | Lấy danh sách danh mục sản phẩm |
| | `POST` | `/api/admin/categories` | ADMIN | Thêm mới danh mục sản phẩm |
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
| **Nhập Kho & Kiểm Định QC**| `GET` | `/api/admin/goods-receipts` | ADMIN | Danh sách phiếu nhập kho |
| | `POST` | `/api/admin/goods-receipts` | ADMIN | Tạo phiếu nhập kho mới từ nhà cung cấp |
| | `POST` | `/api/admin/goods-receipts/{id}/qc-inspection` | ADMIN | **Kiểm định chất lượng đầu vào (Duyệt hàng đạt chuẩn nhập kho, loại bỏ hàng lỗi/hỏng)** |
| | `PUT` | `/api/admin/goods-receipts/{id}/complete` | ADMIN | Hoàn tất nhanh phiếu nhập |
| | `GET` | `/api/admin/inventory-ledger` | ADMIN | Xem sổ cái / thẻ kho xuất nhập tồn |
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
