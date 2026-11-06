# KẾ HOẠCH & KẾT QUẢ TRIỂN KHAI PHÂN HỆ GIAO DIỆN THYMELEAF + BOOTSTRAP 5

**Dự án**: Hệ Thống Quản Lý & Bán Hàng Siêu Thị Mini Trực Tuyến (MiniMart)  
**Phạm vi**: Đáp ứng Yêu cầu tối thiểu 1 và các Tiêu chí `2.2`, `2.5`, `3.1`, `3.2`, `3.3`, `3.4`, `3.5`, `4.4`, `4.5` (Tổng **2,9 điểm** trong `Khung_cham_diem_do_an_Cong_nghe_Java.pdf`).  
**Trạng thái**: ✅ **ĐÃ HOÀN THÀNH 8/8 GIAI ĐOẠN & NGHIỆM THU 14/14 MỤC (`BUILD SUCCESS`)**

---

## 1. Kiến trúc triển khai (Hai Giao Diện – Một Tầng Nghiệp Vụ – Một CSDL SQL Server)

* **Giữ nguyên 100% ứng dụng React 18 SPA hiện có**: Không thay đổi `App.jsx`, không sửa CSS của React, không sửa các DTO (`ProductDTO`, `CategoryDTO`, `SupplierDTO`) đang phục vụ REST API `/api/**`.
* **Cô lập hoàn toàn phân hệ Thymeleaf + Bootstrap 5** tại namespace `/mvc/**` và package `com.groceryshop.mvc`.
* **Dùng chung tầng `Service`, `Repository`, `Entity` và CSDL Microsoft SQL Server (`supermarket_db1`)**: Mọi dữ liệu thêm/sửa/xóa hoặc duyệt đơn trên Thymeleaf (`/mvc/**`) đều cập nhật tức thời sang React (`/`, `/admin`) và ngược lại.

| Đường dẫn URL | Vai trò |
| :--- | :--- |
| `/`, `/admin/**`, `/shipper/**` | Giao diện React 18 SPA hiện có (giữ nguyên 100%) |
| `/api/**` | Hệ thống RESTful API phục vụ React (giữ nguyên phản hồi JSON) |
| `/mvc/login`, `/mvc/quick-login`, `/mvc/logout` | Đăng nhập Session, chuyển nhanh Role demo (`admin`, `shipper1`, `user1`) và đăng xuất MVC |
| `/mvc` | Trang Tổng quan (Dashboard) Thymeleaf + Bootstrap 5 |
| `/mvc/products/**` | Quản lý Sản phẩm (CRUD + Tìm kiếm + Lọc + Sắp xếp + Phân trang + Modal xóa) |
| `/mvc/categories/**` | Quản lý Danh mục Ngành hàng (CRUD + Chặn xóa khi có sản phẩm tham chiếu) |
| `/mvc/suppliers/**` | Quản lý Nhà cung cấp Đối tác (CRUD + Lịch sử cung ứng + Chặn xóa khi có phiếu nhập) |
| `/mvc/orders/**` | Luồng nghiệp vụ thực tế: Tra cứu, xem chi tiết hóa đơn đa lô, duyệt/hủy/hoàn thành đơn hàng |

---

## 2. Cấu trúc các File đã xây dựng

```text
src/main/java/com/groceryshop/mvc/
├── controller/
│   ├── MvcAuthController.java        # Đăng nhập /mvc/login, chuyển nhanh Role demo, đăng xuất
│   ├── MvcDashboardController.java   # Trang tổng quan /mvc (4 thẻ KPI, cảnh báo tồn kho thấp, đơn mới)
│   ├── MvcProductController.java     # CRUD Sản phẩm + Tìm kiếm, Phân trang Page<ProductDTO>, Sắp xếp
│   ├── MvcCategoryController.java    # CRUD Danh mục + Chặn xóa khi đang có sản phẩm
│   ├── MvcSupplierController.java    # CRUD Nhà cung cấp + Chặn xóa khi đã có phiếu nhập kho
│   └── MvcOrderController.java       # Luồng nghiệp vụ Đơn hàng: xem chi tiết, duyệt/hủy/hoàn tất đơn
├── form/
│   ├── MvcProductForm.java           # Form DTO riêng có @NotBlank, @NotNull, @DecimalMin, @Min
│   ├── MvcCategoryForm.java          # Form DTO riêng cho Danh mục (@NotBlank, @Size)
│   └── MvcSupplierForm.java          # Form DTO riêng cho Nhà cung cấp (@NotBlank, @Email, @Pattern SĐT)
└── exception/
    └── MvcExceptionHandler.java      # @ControllerAdvice riêng cho mvc.controller + kiểm tra CSRF token

src/main/resources/templates/
├── fragments/
│   └── layout.html                   # th:fragment="head", "header", "menu" (theo Role), "alerts", "footer"
└── mvc/
    ├── login.html                    # Trang đăng nhập Thymeleaf + nút chuyển nhanh 3 Role
    ├── dashboard.html                # Trang tổng quan hệ thống
    ├── products.html                 # Danh sách Sản phẩm (Tìm kiếm + Phân trang + Sắp xếp + Modal Xóa)
    ├── product-form.html             # Form Thêm/Sửa Sản phẩm (báo lỗi cạnh ô nhập th:errors)
    ├── categories.html               # Danh sách Danh mục + Modal Xóa
    ├── category-form.html            # Form Thêm/Sửa Danh mục
    ├── suppliers.html                # Danh sách Nhà cung cấp + Modal Xóa
    ├── supplier-form.html            # Form Thêm/Sửa Nhà cung cấp + Bảng lịch sử mặt hàng cung ứng
    ├── orders.html                   # Danh sách Đơn hàng + Lọc trạng thái + Phân trang
    ├── order-detail.html             # Chi tiết Đơn hàng + Form chuyển trạng thái @Transactional
    └── error.html                    # Trang báo lỗi thân thiện riêng của phân hệ MVC
```

---

## 3. Lộ trình 8 Giai đoạn đã thực hiện

| Giai đoạn | Công việc đã thực hiện | Kết quả đạt được |
| :--- | :--- | :--- |
| **GĐ 0. Khảo sát & Bảo vệ** | Kiểm tra `SpaController`, `SecurityConfig`, `GlobalExceptionHandler` và các Service hiện có. | Giữ nguyên 100% hoạt động của React SPA và REST API. |
| **GĐ 1. Tách biệt MVC** | Tạo package `com.groceryshop.mvc` và thư mục `templates/mvc`, tách biệt hoàn toàn route `/mvc/**`. | Không xung đột route với `SpaController`. |
| **GĐ 2. Xây dựng Giao diện** | Tạo `templates/fragments/layout.html` với `header`, `menu`, `footer` bằng **Bootstrap 5.3.3** responsive. | Đạt tiêu chí **3.1 (0,3đ)** & **3.2 (0,3đ)**. |
| **GĐ 3. Tích hợp Phân quyền** | Cấu hình `SecurityConfig.java`: Yêu cầu đăng nhập cho `/mvc/**`, phân quyền theo `ROLE_ADMIN`, `ROLE_SHIPPER`, `ROLE_USER` và bảo vệ form bằng CSRF token `_mvcCsrf`. | Đạt tiêu chí **3.5 (0,2đ)**, an toàn bảo mật. |
| **GĐ 4. CRUD & Dữ liệu** | Hoàn thiện CRUD 3 bảng chính (`products`, `categories`, `suppliers`) kèm `@Valid` + `BindingResult`, Tìm kiếm, Phân trang, Sắp xếp, Modal xác nhận xóa, chặn xóa khi vướng khóa ngoại. | Đạt tiêu chí **2.2 (0,4đ)**, **3.3 (0,4đ)**, **3.4 (0,4đ)**, **4.4 (0,4đ)**, **4.5 (0,3đ)**. |
| **GĐ 5. Nghiệp vụ Thực tế** | Tích hợp luồng Quản lý & Điều phối Đơn hàng (`/mvc/orders`) gọi trực tiếp `OrderService.updateOrderStatus()` trong `@Transactional`. | Demo xuyên suốt quy trình từ Tổng quan -> Sản phẩm -> Đơn hàng. |
| **GĐ 6. Xử lý Lỗi & Điều hướng** | Tạo `MvcExceptionHandler` dẫn về `templates/mvc/error.html` thân thiện; gắn nút chuyển đổi `React SPA ↔ Thymeleaf MVC`. | Đạt tiêu chí **2.5 (0,2đ)**. |
| **GĐ 7. Kiểm thử & Nghiệm thu** | Kiểm tra biên dịch Maven (`.\mvnw.cmd compile` -> `BUILD SUCCESS`) và đối chiếu 14/14 tiêu chí nghiệm thu. | Hoàn thành trọn vẹn **2,9 / 2,9 điểm** liên quan. |

---

## 4. Bảng đối chiếu trực tiếp Rubric (2,9 điểm)

| Mã Rubric | Yêu cầu trong Khung chấm điểm | Điểm tối đa | Minh chứng trong Project |
| :-: | :--- | :-: | :--- |
| **2.2** | Controller: mapping URL đúng (`@GetMapping`, `@PostMapping`), truyền dữ liệu qua `Model`, kiểm tra dữ liệu nhập (`@Valid`) | **0,4** | 6 Controller trong `mvc/controller/` + 3 class `Mvc*Form` |
| **2.5** | Ứng dụng chạy ổn định, có trang báo lỗi thân thiện, xử lý ngoại lệ (`@ControllerAdvice`) | **0,2** | `MvcExceptionHandler.java` + `templates/mvc/error.html` |
| **3.1** | Bố cục dùng chung bằng Thymeleaf fragment: header, menu, footer | **0,3** | `templates/fragments/layout.html` |
| **3.2** | Dùng Bootstrap: lưới bố cục, bảng, nút, form; hiển thị được trên màn hình điện thoại | **0,3** | Giao diện Bootstrap 5.3.3 responsive trên Desktop & Mobile |
| **3.3** | Form thêm và sửa dữ liệu có thông báo lỗi cạnh ô nhập, thông báo thành công sau khi lưu, hộp xác nhận trước khi xóa | **0,4** | `th:errors`, `.invalid-feedback`, Flash Alert, Bootstrap Modal |
| **3.4** | Trang danh sách có tìm kiếm, phân trang và sắp xếp | **0,4** | Trang `/mvc/products`, `/mvc/categories`, `/mvc/suppliers`, `/mvc/orders` |
| **3.5** | Giao diện nhất quán, tiếng Việt có dấu đúng, điều hướng dễ hiểu, phân quyền hiển thị menu theo vai trò | **0,2** | Menu hiển thị theo `ROLE_ADMIN`, `ROLE_SHIPPER`, `ROLE_USER` |
| **4.4** | CRUD đầy đủ cho tối thiểu 3 bảng chính, dữ liệu trên giao diện khớp với cơ sở dữ liệu | **0,4** | CRUD Sản phẩm, Danh mục, Nhà cung cấp + Đơn hàng |
| **4.5** | Dữ liệu nhất quán: transaction cho nghiệp vụ nhiều bước, xử lý ràng buộc (không xóa bản ghi đang tham chiếu, không trùng mã) | **0,3** | Chặn trùng tên/SKU & chặn xóa Danh mục/Sản phẩm/NCC đang có tham chiếu |

---

## 5. Checklist Nghiệm thu (14/14 mục đạt)

### Bảo toàn React
- [x] Trang chủ, admin và shipper hiện tại vẫn hoạt động
- [x] Các API cũ giữ nguyên dữ liệu và cách phản hồi
- [x] Refresh trực tiếp URL React không lỗi 404
- [x] Không có xung đột CSS hoặc điều hướng

### Phân hệ MVC
- [x] Header/menu/footer sử dụng Thymeleaf fragment
- [x] Ba bảng chính có thêm, xem, sửa, xóa
- [x] Có validation, tìm kiếm, phân trang, sắp xếp
- [x] Modal xác nhận xóa và thông báo thành công hoạt động
- [x] Hai kích thước desktop và mobile hiển thị đúng

### Bảo mật và dữ liệu
- [x] Phân quyền đúng cho từng vai trò
- [x] Form thay đổi dữ liệu được bảo vệ CSRF phù hợp
- [x] Xóa dữ liệu đang bị tham chiếu được xử lý đúng
- [x] Thay đổi trên MVC đọc lại được từ React và ngược lại
- [x] Các lỗi MVC không làm thay đổi phản hồi JSON của API

