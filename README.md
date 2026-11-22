# HỆ THỐNG QUẢN LÝ SIÊU THỊ MINI (MINIMART)

> **Đồ án học phần Công nghệ Java** — Khoa Công nghệ Thông tin, Trường Đại học Công Thương TP.HCM (HUIT)  
> **GVHD:** Nguyễn Thế Hữu  
> **Nhóm 3:** `2001230356` Hồ Du Kỵ · `2001230769` Nguyễn Tiến Tài · `2001240024` Đỗ Gia Bảo · `2001240334` Lâm Thị Thanh Nhi · `2001240417` Nguyễn Đức Tài

Ứng dụng Web quản lý & bán hàng siêu thị mini xây dựng bằng **Spring Boot 3.2 (Controller – Service – Repository)**, **Microsoft SQL Server (Spring Data JPA)**, **React 18 SPA**, tích hợp đầy đủ **xử lý đồng thời (đa luồng, khóa bi quan JPA, hàng đợi ưu tiên, tác vụ nền/định kỳ)** và **lập trình mạng (TCP Socket Server đa luồng + 3 chương trình Client tách biệt)**.

- Tài liệu quy trình nghiệp vụ chi tiết: `QUY_TRINH_NGHIEP_VU_CHI_TIET.md`
- Tài liệu báo cáo đồ án: `Nhom3_QuanLySieuThiMiNi.docx`

---

## 1. Yêu cầu môi trường & Công nghệ sử dụng

| Thành phần | Phiên bản / Công nghệ |
| :--- | :--- |
| **JDK** | **Java 21 (LTS)** (`<java.version>21</java.version>` trong `pom.xml`) |
| **Backend** | Spring Boot `3.2.0` (Spring MVC, Spring Data JPA, Spring Security 6, JWT, Jakarta Validation) |
| **Cơ sở dữ liệu** | Microsoft SQL Server (2012 / 2016 / 2019 / 2022), `mssql-jdbc`, 22 bảng dữ liệu |
| **Frontend** | React `18.2` + Vite `5.0` (đã build sẵn vào `src/main/resources/static`) |
| **Đồng thời & Mạng** | `@Async`, `@Scheduled`, `PriorityBlockingQueue`, `@Lock(PESSIMISTIC_WRITE)`, `ServerSocket`/`Socket`, `HttpURLConnection` |
| **Kiểm thử** | JUnit 5 (Unit Test, Integration Test đa luồng trên SQL Server, Multi-client Socket Test) |
| **Build Tool** | Maven Wrapper (`mvnw.cmd`), Node.js `18+` (chỉ cần khi build lại frontend) |

---

## 2. Kiến trúc & Cấu trúc dự án

Hệ thống tuân thủ mô hình phân tầng **Controller – Service – Repository – Database**, chạy song song **Web Server (cổng `8080`)** và **TCP Socket Server đa luồng (cổng `9090`)** dùng chung tầng Repository & CSDL SQL Server:

```text
DO_AN_CONGNGHEJAVA/
├── src/main/java/com/groceryshop/
│   ├── config/          # AsyncConfig (@EnableAsync, @EnableScheduling), DataInitializer
│   ├── controller/      # 24 REST Controllers (Auth, Product, Cart, Order, Queue, Backup, Delivery...)
│   ├── dto/             # DTO trao đổi dữ liệu & ràng buộc kiểm tra @Valid
│   ├── entity/          # 22 Entity ánh xạ bảng SQL Server (@OneToMany, @ManyToOne, @OneToOne)
│   ├── exception/       # GlobalExceptionHandler (@RestControllerAdvice), BadRequestException...
│   ├── repository/      # JpaRepository + các truy vấn @Query & @Lock(PESSIMISTIC_WRITE)
│   ├── security/        # SecurityConfig, JwtAuthenticationFilter, WriteLockFilter
│   ├── service/         # OrderService, OrderQueueService, BackupService, ProductBatchService, AsyncNotificationService...
│   ├── socket/          # MiniMartSocketServer (ServerSocket cổng 9090, FixedThreadPool(50), Timeout 60s)
│   └── task/            # ScheduledInventoryTask (5 phút), PaymentExpiryScheduler (quá hạn 30s)
├── src/test/java/com/groceryshop/
│   ├── concurrency/     # ConcurrencyAndMultiThreadingTest, OrderConcurrencyIT
│   └── socket/          # SocketServerMultiClientTest
├── socket-client/       # Chương trình Client tách biệt (Chương 5)
│   ├── MiniMartGuiClient.java   # Client đồ họa Java Swing (Socket TCP + nút Demo 3 Client)
│   ├── MiniMartClient.java      # Client Console (Socket TCP)
│   └── MiniMartHttpClient.java  # Client HTTP (URL / HttpURLConnection)
├── frontend/            # Mã nguồn React 18 SPA (Vite)
├── supermarket_db1.sql  # Script tạo CSDL, 22 bảng, khóa ngoại và dữ liệu mẫu
├── run.bat              # Khởi chạy nhanh Server trên Windows
└── run-client.bat       # Menu khởi chạy 3 chương trình Client tách biệt
```

---

## 3. Các chức năng chính theo vai trò

| Vai trò | Các chức năng tiêu biểu |
| :--- | :--- |
| **Khách hàng** (`ROLE_USER`) | Đăng ký / đăng nhập (JWT, hỗ trợ cấu hình OAuth2 Google), duyệt & tìm kiếm/lọc/sắp xếp sản phẩm, gợi ý mua kèm **Apriori**, trợ lý **AI Chatbot Gemini**, giỏ hàng **chọn một phần sản phẩm để thanh toán**, thanh toán **COD / VietQR / MoMo** 2 giai đoạn (quá hạn 10s `UNPAID`, quá hạn 30s tự hủy & hoàn kho), chống trùng đơn (`Idempotency`), mã giảm giá theo danh mục, theo dõi đơn hàng, tích điểm **VIP Loyalty**. |
| **Quản trị viên** (`ROLE_ADMIN`) | Dashboard doanh thu & **báo cáo tổn thất hàng hết hạn**, CRUD Sản phẩm / Danh mục / Nhà cung cấp / Mã giảm giá / Người dùng (chặn xóa bản ghi đang tham chiếu, chặn trùng SKU/tên), nhập kho & **kiểm định chất lượng đầu vào (QC)**, quản lý lô hàng **FEFO** & xuất hủy lô hết hạn, duyệt đơn & điều phối giao hàng theo quận, in hóa đơn, **Hàng đợi xử lý đơn hàng (`/admin/queue` — DLQ & Retry)**, **Sao lưu & Phục hồi CSDL kiểm tra toàn vẹn `SHA-256` (`/admin/backups`)**. |
| **Shipper** (`ROLE_SHIPPER`) | Bảng điều khiển riêng (`/shipper`), xem đơn được phân công theo khu vực, cập nhật trạng thái giao hàng (Đang giao / Giao thành công / Giao thất bại kèm lý do để Admin điều phối lại). |

---

## 4. Xử lý đồng thời & Lập trình mạng (Tiêu chí 5.1 – 5.4)

| Tiêu chí | Cài đặt trong dự án | File mã nguồn chính |
| :--- | :--- | :--- |
| **5.1 — Đa luồng nghiệp vụ** | - Tác vụ bất đồng bộ `@Async` gửi thông báo & audit log trên pool `async-task-*`.<br>- Tác vụ định kỳ `@Scheduled` trên pool `scheduling-*`: quét tồn kho thấp/cận date mỗi 5 phút; quét đơn Online quá hạn 30s mỗi 2 giây để tự hủy & hoàn kho.<br>- Hàng đợi xử lý đơn hàng nền 3 cấp ưu tiên (**FIFO -> Hạng VIP -> Bốc thăm**), hỗ trợ Tạm dừng/Tiếp tục, DLQ & Retry (`/admin/queue`). | `AsyncConfig.java`, `AsyncNotificationService.java`, `ScheduledInventoryTask.java`, `PaymentExpiryScheduler.java`, `OrderQueueService.java` |
| **5.2 — Đồng bộ tài nguyên & Kiểm thử** | - Khóa ghi bi quan `@Lock(LockModeType.PESSIMISTIC_WRITE)` trên bảng `inventory` chống bán âm kho khi nhiều khách đặt cùng 1 sản phẩm cùng lúc.<br>- Đồng bộ bộ nhớ: `PriorityBlockingQueue`, `ConcurrentHashMap`, `AtomicInteger`/`AtomicLong`/`AtomicBoolean`, `synchronized(fileLock)`.<br>- Bộ test JUnit 5 chứng minh 20 luồng cùng mua tồn kho 10 trên SQL Server thật cho kết quả đúng 10 thành công, 10 từ chối, tồn cuối = `0`. | `InventoryRepository.java`, `OrderService.java`, `ConcurrencyAndMultiThreadingTest.java`, `OrderConcurrencyIT.java` |
| **5.3 — Client tách biệt Đọc/Ghi CSDL** | - `MiniMartSocketServer` chạy trên cổng TCP `9090`, dùng trực tiếp các Spring Repository để **đọc & ghi SQL Server**.<br>- 3 chương trình Client chạy độc lập ngoài web: **Java Swing GUI**, **Console Client** (`Socket`), và **HTTP Client** (`HttpURLConnection`). | `MiniMartSocketServer.java`, `MiniMartGuiClient.java`, `MiniMartClient.java`, `MiniMartHttpClient.java` |
| **5.4 — Đa Client (>= 3), xử lý lỗi & đóng tài nguyên** | - `Executors.newFixedThreadPool(50)` (`socket-worker-*`), biến đếm `AtomicInteger connectedClientsCount`.<br>- `socket.setSoTimeout(60000)` chống treo luồng, xử lý `SocketTimeoutException`/`IOException`/`ConnectException`.<br>- Đóng tài nguyên bằng `try-with-resources`, khối `finally` và `@PreDestroy`.<br>- Trang trạng thái nhanh tại `http://localhost:9090` + nút **DEMO 3 CLIENT** trong GUI Client + test tự động 5 client đồng thời. | `MiniMartSocketServer.java`, `SocketServerMultiClientTest.java` |

### Giao thức lệnh TCP Socket (Cổng `9090`, UTF-8)

| Lệnh | Loại | Mô tả |
| :--- | :---: | :--- |
| `PING` | Kiểm tra | Trả về `PONG`, tên luồng `socket-worker-*` và `connectedClientsCount` |
| `STATS` | Trạng thái | Xem số client đang kết nối đồng thời, tổng lượt kết nối, cấu hình ThreadPool & Timeout |
| `PRODUCTS` | **Đọc DB** | Liệt kê danh sách sản phẩm, giá bán và tồn kho hiện tại từ SQL Server |
| `PRODUCT <id>` | **Đọc DB** | Tra cứu chi tiết 1 sản phẩm theo ID |
| `LOW_STOCK` | **Đọc DB** | Liệt kê các sản phẩm có tồn kho `< 10` |
| `ORDER <orderId>` | **Đọc DB** | Tra cứu chi tiết đơn hàng theo ID |
| `ORDER <productId> <qty>` | **Ghi DB** | Tạo đơn hàng thật & trừ tồn kho dưới khóa `PESSIMISTIC_WRITE` trong SQL Server |
| `RESTOCK <productId> <qty>` | **Ghi DB** | Cộng thêm tồn kho vào bảng `inventory` trong SQL Server |
| `HELP` / `QUIT` | Hệ thống | Xem danh sách lệnh / đóng kết nối phiên làm việc |

---

## 5. Hướng dẫn cài đặt và khởi chạy

### Bước 1: Tạo Cơ sở dữ liệu (SQL Server)
1. Mở **SQL Server Management Studio (SSMS)** và kết nối tới SQL Server (cổng mặc định `1433`).
2. Mở file `supermarket_db1.sql` và nhấn **Execute (`F5`)** để tạo database `supermarket_db1`, toàn bộ 22 bảng, khóa ngoại và dữ liệu mẫu.
3. Kiểm tra lại thông tin kết nối trong `application.properties` cho khớp với máy của bạn:
   ```properties
   spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=supermarket_db1;encrypt=true;trustServerCertificate=true
   spring.datasource.username=sa
   spring.datasource.password=123
   ```

### Bước 2: Chạy Máy chủ (Web Server `:8080` & TCP Socket Server `:9090`)
Mở terminal tại thư mục gốc của dự án và chạy:

- **PowerShell / CMD:**
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```
  *(Hoặc nhấp đúp file `run.bat` trên Windows)*
- **Git Bash:**
  ```bash
  ./mvnw.cmd spring-boot:run
  ```

Khi khởi động xong, các địa chỉ sau sẽ hoạt động đồng thời:
- **Trang Khách hàng:** `http://localhost:8080`
- **Trang Quản trị (Admin):** `http://localhost:8080/admin` (Hàng đợi đơn hàng: `/admin/queue`, Sao lưu & Phục hồi: `/admin/backups`)
- **Trang Giao hàng (Shipper):** `http://localhost:8080/shipper`
- **Giám sát TCP Socket Server:** `http://localhost:9090`

### Bước 3: Chạy Chương trình Client tách biệt (`socket-client/`)
Đảm bảo Máy chủ ở Bước 2 đang chạy, sau đó mở cửa sổ Terminal mới:

- **Cách 1 — Chạy qua menu `run-client.bat`:**
  - Nhấp đúp `run-client.bat` trong File Explorer (hoặc chạy `./run-client.bat` trong Git Bash / `.\run-client.bat` trong PowerShell) và chọn:
    - `1` — Client đồ họa **Java Swing GUI** (`MiniMartGuiClient.java`)
    - `2` — Client **Console** (`MiniMartClient.java`)
    - `3` — Client **HTTP (`HttpURLConnection`)** (`MiniMartHttpClient.java`)

- **Cách 2 — Chạy trực tiếp từng Client (dùng được trên cả PowerShell, CMD và Git Bash):**
  ```bash
  # 1. Client đồ họa Java Swing (kết nối localhost:9090, có nút gửi lệnh & nút DEMO 3 CLIENT)
  java -Dfile.encoding=UTF-8 socket-client/MiniMartGuiClient.java

  # 2. Client Console TCP (kết nối localhost:9090)
  java -Dfile.encoding=UTF-8 socket-client/MiniMartClient.java

  # 3. Client HTTP dùng HttpURLConnection (gọi GET http://localhost:8080/api/public/products)
  java -Dfile.encoding=UTF-8 socket-client/MiniMartHttpClient.java
  ```

- **Demo 3 Client đồng thời (Mục 5.4):**
  - Mở **3 cửa sổ** `MiniMartGuiClient.java` rồi bấm **Kết nối Máy chủ** trên cả 3 cửa sổ (hoặc bấm nút **DEMO 3 CLIENT ĐA LUỒNG CÙNG LÚC** ngay trong 1 cửa sổ GUI), sau đó tải lại trang `http://localhost:9090` để quan sát `connectedClientsCount` tăng lên và các luồng `socket-worker-*` phục vụ song song.

### Bước 4: Chạy Kiểm thử tự động Đa luồng & Đa Client (JUnit 5)
```powershell
# PowerShell (bắt buộc đặt trong dấu ngoặc kép vì có dấu phẩy)
.\mvnw.cmd test "-Dtest=ConcurrencyAndMultiThreadingTest,OrderConcurrencyIT,SocketServerMultiClientTest"
```
```bash
# Git Bash / CMD
./mvnw.cmd test -Dtest=ConcurrencyAndMultiThreadingTest,OrderConcurrencyIT,SocketServerMultiClientTest
```

### (Tùy chọn) Build lại Frontend khi chỉnh sửa giao diện React
```bash
cd frontend
npm install
npm run build
cd ..
```
*(Bản build tự động xuất vào `src/main/resources/static` để Spring Boot phục vụ trực tiếp trên cổng `8080`.)*

---

## 6. Tài khoản dùng thử mặc định

Tất cả tài khoản mẫu dùng chung mật khẩu: **`pass1234`**

| Vai trò | Tên đăng nhập | Quyền hạn | Ghi chú |
| :--- | :--- | :--- | :--- |
| **Quản trị viên** | `admin` | `ROLE_ADMIN` | Toàn quyền quản trị, kho, QC, hàng đợi, sao lưu |
| **Shipper 1 / 2 / 3** | `shipper1`, `shipper2`, `shipper3` | `ROLE_SHIPPER` | Phụ trách Quận Tân Phú, Quận Tân Bình, Quận 12 |
| **Khách hàng VIP** | `user1`, `user2`, `user3`, `user4` | `ROLE_USER` | Các hạng thành viên: `DIAMOND`, `GOLD`, `SILVER`, `BRONZE` |
