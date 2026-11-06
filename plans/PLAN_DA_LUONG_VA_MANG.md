# KẾ HOẠCH: ĐA LUỒNG – LẬP TRÌNH MẠNG – TÁCH NGHIỆP VỤ SANG SERVICE

**Dự án**: MiniMart (Spring Boot 3.2 · Java 21 · SQL Server `supermarket_db1`)
**Đối chiếu**: `Khung_cham_diem_do_an_Cong_nghe_Java.pdf` (Tiêu chí 1.5, 2.3, 5.1 – 5.4)
**Tài liệu tham khảo**: `Chuong5_Networking_updated.pptx` (URL/URLConnection, Socket/ServerSocket, Socket đa luồng, RMI)
**Mức độ**: vừa đủ cho đồ án môn học – ưu tiên đơn giản, dễ demo, dễ giải thích khi vấn đáp.
**Ngày lập**: 06/11/2026

---

## PHẦN A. ĐỐI CHIẾU KHUNG CHẤM ĐIỂM VỚI PROJECT (kiểm tra mã nguồn ngày 06/11)

### A.1. Sáu yêu cầu tối thiểu

| # | Yêu cầu tối thiểu | Hiện trạng | Kết quả |
|:-:|---|---|:-:|
| 1 | Spring Boot, Controller – Service – Repository, Thymeleaf + Bootstrap | Có phân hệ `/mvc/**` (6 controller, 12 template, Bootstrap 5.3) | ✅ |
| 2 | SQL Server + Spring Data JPA, ≥ 4 bảng, ≥ 1 quan hệ 1–n | 22 bảng, nhiều `@OneToMany/@ManyToOne` | ✅ |
| 3 | ≥ 1 chức năng xử lý đồng thời có ý nghĩa nghiệp vụ | **Không có** `@Async`, `@Scheduled`, `synchronized`, `Lock`, `Atomic`, `@Lock` | ❌ |
| 4 | ≥ 1 chức năng lập trình mạng + client tách biệt | **Không có** `ServerSocket`, `Socket`, `URLConnection`, RMI | ❌ |
| 5 | Chạy được khi demo | Biên dịch được; phân hệ MVC **chưa chạy thử thực tế** | ⚠️ |
| 6 | Nộp đủ sản phẩm đúng hạn | Chưa có báo cáo, slide, bảng phân công | ⏳ |

### A.2. Chi tiết theo tiêu chí

| Mã | Hạng mục | Điểm | Trạng thái | Ghi chú |
|:-:|---|:-:|:-:|---|
| 1.1 | Mô tả bài toán, nghiệp vụ | 0,4 | ✅ | `QUY_TRINH_NGHIEP_VU_CHI_TIET.md`, `README.md` |
| 1.2 | Chức năng theo vai trò (≥10), phi chức năng | 0,4 | ⚠️ | Thiếu sơ đồ Use Case + mục yêu cầu phi chức năng |
| 1.3 | Thiết kế CSDL, ERD | 0,4 | ⚠️ | Có script đủ PK/FK; thiếu hình ERD trong báo cáo |
| 1.4 | Kiến trúc tầng + sơ đồ tuần tự | 0,4 | ✅ | Có trong README và tài liệu nghiệp vụ |
| **1.5** | **Thiết kế phần đồng thời & mạng** | 0,4 | ❌ | → **Bước 9** của plan này |
| 2.1 | Cấu trúc package, `application.properties` | 0,3 | ✅ | |
| 2.2 | Controller + Model + `@Valid` | 0,4 | ✅ | `mvc/controller/*`, `mvc/form/*` |
| **2.3** | **Nghiệp vụ nằm trong Service, `@Transactional`** | 0,4 | ⚠️ | `DeliveryController` (3 hàm `@Transactional`) và `PaymentConfigController` (2 hàm `@Transactional`) còn gọi Repository trực tiếp → **Bước 1** |
| 2.4 | Đủ chức năng đã đặc tả | 0,3 | ✅ | |
| 2.5 | Trang lỗi thân thiện, `@ControllerAdvice` | 0,2 | ✅ | `MvcExceptionHandler`, `GlobalExceptionHandler` |
| 3.1 – 3.5 | Fragment, Bootstrap, form lỗi/thành công/xác nhận xóa, tìm kiếm–phân trang–sắp xếp, menu theo vai trò | 1,6 | ✅ | Cần chạy thử lại trên trình duyệt + điện thoại |
| 4.1 | Kết nối SQL Server, script + dữ liệu mẫu | 0,2 | ⚠️ | `products`(10), `inventory`(10), `users`(8), `product_batches`(9), `categories`(5) < 20 dòng |
| 4.2 – 4.5 | Entity, Repository tùy chỉnh, CRUD 3 bảng, ràng buộc | 1,4 | ✅ | |
| **5.1** | **Đa luồng có ý nghĩa nghiệp vụ + log tên luồng** | 0,4 | ❌ | → **Bước 2** |
| **5.2** | **Đồng bộ tài nguyên dùng chung + kiểm thử nhiều luồng** | 0,4 | ❌ | → **Bước 3, 4** |
| **5.3** | **Client tách biệt qua Socket/URL/RMI, dữ liệu từ CSDL** | 0,5 | ❌ | → **Bước 5, 6** |
| **5.4** | **Server phục vụ ≥ 3 client, xử lý lỗi, đóng tài nguyên** | 0,3 | ❌ | → **Bước 5, 7** |
| 6.1 – 6.4 | Báo cáo, thuyết trình, vấn đáp, Git | 1,6 | ⏳ | Ngoài phạm vi code |

**Ước tính**: hiện tại ~6,1 / 8,4 điểm kỹ thuật. Làm xong plan này: +1,6 (tiêu chí 5) + 0,4 (1.5) + ~0,1 (2.3).

---

## PHẦN B. CÁC PLAN TRƯỚC ĐÃ THỰC HIỆN ĐƯỢC GÌ

| Plan | Nội dung | Trạng thái thực tế |
|---|---|---|
| `plans/PLAN.md`, `plans/plan2.md` | Lô hết hạn + chặn Sale, xuất hủy, báo cáo tổn thất, gộp Tồn kho vào Sản phẩm, gộp Phiếu nhập + Sổ cái (`WarehouseHistoryManager.jsx`), bán đa lô FIFO tách 2 dòng hóa đơn | ✅ **Đã làm** – `ProductBatch` có `importPrice/salePrice/status`, có API xuất hủy (`ProductBatchService`), có `WarehouseHistoryManager.jsx`, `OrderService.createOrder` đã tách dòng lô sale. Đã commit (`ae43f93`, `2ae953f`). |
| `plans/THYMELEAF_BOOTSTRAP_PLAN.md` | Phân hệ `/mvc/**` Thymeleaf + Bootstrap 5 | ✅ **Đã code, biên dịch OK** – ⚠️ **chưa commit** (thư mục `mvc/`, `templates/` đang untracked) và **chưa chạy thử** trên trình duyệt. |
| `plans/Final_plan.md` – Ưu tiên 1: Đa luồng | `@Async`, `@Scheduled`, khóa chống âm kho, test đa luồng | ❌ Chưa làm |
| `plans/Final_plan.md` – Ưu tiên 2: Mạng | ServerSocket đa luồng + client rời | ❌ Chưa làm |
| `plans/Final_plan.md` – Ưu tiên 3: Dữ liệu mẫu ≥ 20 dòng | `supermarket_db1.sql` | ❌ Chưa làm |
| `plans/Final_plan.md` – Ưu tiên 4: Controller → Service | `DeliveryService`, `PaymentConfigService` | ❌ Chưa làm |
| `plans/Final_plan.md` – Ưu tiên 5: Báo cáo, slide, phân công | | ❌ Chưa làm |

---

## PHẦN C. NGUỒN THAM KHẢO TỪ FILE `Chuong5_Networking_updated.pptx`

| Nội dung slide | Áp dụng vào đồ án |
|---|---|
| Slide 21–30: Socket/ServerSocket, các bước xây dựng server (B1–B6) và client (B1–B5) | **Dùng làm khung chính** cho Socket Server + Client (Bước 5, 6) |
| Slide 37–39: Đa luồng trong lập trình socket (`TCPEchoServerMultiThread`) – mỗi client một luồng | **Dùng** để server phục vụ ≥ 3 client cùng lúc (tiêu chí 5.4) |
| Slide 31–36: Truyền đối tượng bằng `ObjectInputStream/ObjectOutputStream` + `Serializable` | Không dùng (client phải có cùng class/package) → dùng **giao thức văn bản** cho đơn giản |
| Slide 11–20: `URL`, `URLConnection` đọc nội dung URL | **Tùy chọn** (Bước 8): client thứ 2 gọi `/api/public/products` |
| Slide 54–77: RMI | Không dùng (1 cơ chế mạng là đủ) |

> **Không có trong slide** (phải tự thiết kế – xem Bước 1 – 4): `@Async`, `@Scheduled`, khóa JPA chống âm kho, kiểm thử đa luồng, tách Controller → Service.

---

## PHẦN D. KẾ HOẠCH THỰC HIỆN CHI TIẾT

```
Bước 0  Commit phần MVC đang dở                               (15 phút)
Bước 1  Controller → Service (Delivery, PaymentConfig)        [2.3]  (1–2 giờ)
Bước 2  Đa luồng nghiệp vụ: @Async + @Scheduled               [5.1]  (1–2 giờ)
Bước 3  Đồng bộ tồn kho bằng khóa JPA (PESSIMISTIC_WRITE)     [5.2]  (1 giờ)
Bước 4  Chương trình kiểm thử đa luồng                        [5.2]  (1–2 giờ)
Bước 5  Socket Server đa luồng chạy cùng Spring Boot          [5.3, 5.4] (2–3 giờ)
Bước 6  Chương trình Client console tách biệt                 [5.3]  (1–2 giờ)
Bước 7  Kiểm thử 3 client cùng lúc                            [5.4]  (1 giờ)
Bước 8  (Tùy chọn) Client URLConnection                       [5.3]  (30 phút)
Bước 9  README + nội dung báo cáo mục 1.5 và chương 4         [1.5, 6.1] (2 giờ)
```

---

### Bước 0 – Commit phần đang dở

- Chạy `.\mvnw.cmd compile` → `BUILD SUCCESS`.
- Chạy app, mở thử `/mvc/login`, `/mvc/products`, `/` (React) để chắc không hỏng.
- Commit thư mục `src/main/java/com/groceryshop/mvc/`, `src/main/resources/templates/`, `plans/` và 6 file đã sửa.
- **Mỗi bước dưới đây = 1 commit riêng**, nên chia cho các thành viên khác nhau commit (giúp điểm 6.4).

---

### Bước 1 – Chuyển nghiệp vụ từ Controller sang Service (tiêu chí 2.3)

**1.1. Tạo `service/DeliveryService.java`**

Chuyển toàn bộ logic đang nằm trong `controller/DeliveryController.java`:

| Hàm trong Controller | Hàm mới trong `DeliveryService` |
|---|---|
| `getAvailableShippers()` (lọc user là shipper + đếm đơn đang giao) | `List<Map<String,Object>> getAvailableShippers()` |
| `getUnassignedOrders()` | `List<OrderDTO> getUnassignedOrders()` |
| `getTrackingOrders()` | `List<OrderDTO> getTrackingOrders()` |
| `getDeliveryOrders(district)` | `List<...> getDeliveryOrders(String district)` |
| `assignOrdersToShipper(payload)` – `@Transactional` | `@Transactional int assignOrders(Long shipperId, List<Long> orderIds)` |
| `autoDispatchByArea(payload)` – `@Transactional` | `@Transactional Map<String,Object> autoDispatch(String district)` |
| `reassignOrder(payload)` – `@Transactional` | `@Transactional OrderDTO reassignOrder(Long orderId, Long newShipperId)` |

**1.2. Tạo `service/PaymentConfigService.java`**

| Hàm trong Controller | Hàm mới trong Service |
|---|---|
| `getPublicPaymentMethods()`, `getAllPaymentMethods()` | `getEnabledMethods()`, `getAllMethods()` |
| `uploadQrImage(...)`, `updatePaymentMethod(...)` | `uploadQr(...)`, `updateMethod(Long id, ...)` |
| `getTransactions` (dòng 111) | `getTransactions(...)` |
| `approve` – `@Transactional` (dòng 142) | `@Transactional approvePayment(Long paymentId)` |
| `reject` – `@Transactional` (dòng 173) | `@Transactional rejectPayment(Long paymentId, String reason)` |

**1.3. Sửa 2 Controller**
- Xóa `@Autowired ...Repository`, xóa mọi `@Transactional`, chỉ còn: nhận request → gọi Service → trả `ResponseEntity`.
- Lỗi nghiệp vụ ném `BadRequestException` / `ResourceNotFoundException` (đã có `GlobalExceptionHandler` xử lý).
- **Giữ nguyên URL và JSON trả về** để React không bị ảnh hưởng.

**1.4. Kiểm tra**
```powershell
# Không còn Controller nào gọi Repository hoặc dùng @Transactional
Select-String -Path src\main\java\com\groceryshop\controller\*.java -Pattern "Repository;|@Transactional"
```
Kết quả mong muốn: rỗng. Sau đó mở trang Admin → Giao hàng, Thanh toán trên React để kiểm tra lại.

---

### Bước 2 – Đa luồng có ý nghĩa nghiệp vụ (tiêu chí 5.1)

Làm **2 chức năng nhỏ**, đều in **tên luồng** ra console để demo.

**2.1. `config/AsyncConfig.java` – bật và cấu hình thread pool**
```java
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
    @Bean(name = "taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(3);
        ex.setMaxPoolSize(5);
        ex.setQueueCapacity(50);
        ex.setThreadNamePrefix("minimart-async-");
        ex.initialize();
        return ex;
    }
}
```
Thêm vào `application.properties`:
```properties
spring.task.scheduling.thread-name-prefix=minimart-scheduler-
app.scheduler.scan-interval-ms=60000
```

**2.2. Tác vụ nền `@Async`: gửi thông báo khi có đơn hàng mới**
- Ý nghĩa nghiệp vụ: khách bấm đặt hàng → trả kết quả ngay, việc "gửi email/ghi thông báo" (giả lập mất 2–3 giây) chạy ở luồng khác.
- Tạo `event/OrderCreatedEvent.java` (record chứa `orderId`, `orderCode`, `email`).
- Trong `OrderService.createOrder(...)` cuối hàm: `eventPublisher.publishEvent(new OrderCreatedEvent(...))`.
- Tạo `service/OrderNotificationService.java`:
```java
@Slf4j
@Service
public class OrderNotificationService {
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // chỉ chạy khi đơn đã lưu thành công
    public void onOrderCreated(OrderCreatedEvent e) throws InterruptedException {
        log.info("[{}] Bắt đầu gửi thông báo đơn {}", Thread.currentThread().getName(), e.orderCode());
        Thread.sleep(2000); // giả lập gửi email
        log.info("[{}] Đã gửi thông báo đơn {} tới {}", Thread.currentThread().getName(), e.orderCode(), e.email());
    }
}
```
- Demo: log của request chạy trên luồng `http-nio-8080-exec-*`, log gửi thông báo chạy trên `minimart-async-1`.

**2.3. Tác vụ định kỳ `@Scheduled`: quét lô hết hạn & tồn kho thấp**
- Tạo `scheduler/InventoryAlertScheduler.java`:
```java
@Scheduled(fixedDelayString = "${app.scheduler.scan-interval-ms}")
public void scan() {
    String t = Thread.currentThread().getName();
    long expired = productBatchRepository.countExpiredActive(LocalDate.now());
    int lowStock = inventoryService.getLowStockWarnings().size();
    log.warn("[{}] Quét định kỳ: {} lô đã hết hạn còn tồn, {} sản phẩm dưới mức tối thiểu", t, expired, lowStock);
}
```
- Thêm vào `ProductBatchRepository`:
```java
@Query("SELECT COUNT(b) FROM ProductBatch b WHERE b.expiryDate < :today AND b.quantity > 0 AND b.status = 'ACTIVE'")
long countExpiredActive(@Param("today") LocalDate today);
```
- **Chỉ đọc và ghi log** (không tự động hủy lô) → an toàn, không ảnh hưởng nghiệp vụ hiện có.
- (Tùy chọn) Lưu kết quả lần quét gần nhất vào 1 biến `volatile` và hiển thị trên `/mvc` Dashboard.

---

### Bước 3 – Đồng bộ tài nguyên dùng chung: tồn kho (tiêu chí 5.2)

**Tài nguyên dùng chung**: cột `inventory.current_stock` của 1 sản phẩm.
**Vấn đề**: 2 khách cùng mua sản phẩm chỉ còn 1 cái → cả 2 cùng đọc `stock = 1`, cùng qua bước kiểm tra, cùng trừ → kho âm (race condition: đọc – kiểm tra – ghi không nguyên tử).
**Cách đồng bộ**: khóa bi quan của JPA (`PESSIMISTIC_WRITE` → SQL Server `UPDLOCK, ROWLOCK`). Giao dịch sau phải chờ giao dịch trước commit rồi mới đọc được số tồn mới.

**3.1. `InventoryRepository` thêm truy vấn có khóa**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT i FROM Inventory i WHERE i.product.id = :productId")
Optional<Inventory> findByProductIdForUpdate(@Param("productId") Long productId);
```

**3.2. `InventoryService` thêm hàm trừ kho an toàn** (dùng cho cả đơn hàng và kiểm thử)
```java
@Transactional
public void decreaseStock(Long productId, int qty) {
    Inventory inv = inventoryRepository.findByProductIdForUpdate(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Không có tồn kho cho sản phẩm " + productId));
    if (inv.getCurrentStock() < qty) {
        throw new BadRequestException("Không đủ hàng (còn " + inv.getCurrentStock() + ")");
    }
    inv.setCurrentStock(inv.getCurrentStock() - qty);
}
```

**3.3. Sửa `OrderService.createOrder` (khoảng dòng 79–82 và 214)**
- Thay `Inventory inventory = product.getInventory();` bằng `inventoryRepository.findByProductIdForUpdate(product.getId())` để kiểm tra tồn **trong lúc đang giữ khóa**.
- Phần trừ kho ở dòng ~214 giữ nguyên logic, chỉ dùng đúng đối tượng `inventory` đã khóa.
- Không đổi logic FIFO lô / tách dòng sale.

**3.4. (Bổ sung nhỏ) `CouponService`** – nếu có trừ lượt dùng coupon, áp dụng cùng cách khóa để chống dùng vượt số lượt. Không bắt buộc.

---

### Bước 4 – Chương trình kiểm thử đa luồng (tiêu chí 5.2, báo cáo chương 4)

Tạo 2 file test trong `src/test/java/com/groceryshop/concurrency/`.

**4.1. `RaceConditionDemoTest.java` – Java thuần, không cần CSDL** (minh họa lý thuyết)
- 100 luồng, mỗi luồng tăng biến đếm 1000 lần.
- Biến `int` thường → kết quả **< 100.000** (sai).
- Dùng `synchronized` / `AtomicInteger` → kết quả **= 100.000** (đúng).
- Dùng `ExecutorService` + `CountDownLatch` để các luồng xuất phát cùng lúc.

**4.2. `InventoryConcurrencyTest.java` – `@SpringBootTest`, chạy trên SQL Server thật**
```java
@SpringBootTest
class InventoryConcurrencyTest {
    @Autowired InventoryService inventoryService;
    @Autowired InventoryRepository inventoryRepository;
    Long productId; int originalStock;

    @BeforeEach void setUp() {        // chọn 1 sản phẩm có sẵn, lưu tồn gốc, đặt tồn = 10
        Inventory inv = inventoryRepository.findAll().get(0);
        productId = inv.getProduct().getId(); originalStock = inv.getCurrentStock();
        inv.setCurrentStock(10); inventoryRepository.save(inv);
    }
    @AfterEach void restore() { /* trả lại originalStock */ }

    @Test void twentyBuyersTenItems() throws Exception {
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(threads);
        AtomicInteger ok = new AtomicInteger(), fail = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try { start.await(); inventoryService.decreaseStock(productId, 1); ok.incrementAndGet(); }
                catch (Exception e) { fail.incrementAndGet(); }
                finally { done.countDown(); }
            });
        }
        start.countDown(); done.await(30, TimeUnit.SECONDS); pool.shutdown();

        int stockAfter = inventoryRepository.findByProductId(productId).get().getCurrentStock();
        assertEquals(10, ok.get());     // đúng 10 người mua được
        assertEquals(10, fail.get());   // 10 người báo "Không đủ hàng"
        assertEquals(0, stockAfter);    // kho không âm
    }
}
```
- Chạy: `.\mvnw.cmd test -Dtest=InventoryConcurrencyTest` (SQL Server phải đang chạy).
- **Chụp màn hình kết quả** (có log tên luồng `pool-1-thread-*`) cho báo cáo chương 4.
- (Để so sánh trong báo cáo) Tạm bỏ `@Lock` → chạy lại → thấy `ok > 10` hoặc tồn kho sai → chụp màn hình "trước khi đồng bộ", rồi bật lại khóa.

---

### Bước 5 – Socket Server đa luồng chạy cùng Spring Boot (tiêu chí 5.3, 5.4)

Theo slide 25–27 (các bước xây dựng server) và slide 37–39 (mỗi client một luồng).

**5.1. Cấu hình** (`application.properties`)
```properties
app.socket.enabled=true
app.socket.port=9090
app.socket.max-clients=10
```

**5.2. Giao thức trao đổi (văn bản UTF-8, mỗi lệnh 1 dòng)**

| Lệnh client gửi | Ý nghĩa | Dữ liệu lấy từ CSDL qua |
|---|---|---|
| `HELP` | Danh sách lệnh | – |
| `PRODUCTS <từ khóa>` | Tìm sản phẩm theo tên | `ProductService.getProductsPagedAdmin(...)` |
| `PRODUCT <id>` | Chi tiết 1 sản phẩm + tồn kho | `ProductService.getProductById(id)` |
| `LOW_STOCK` | Sản phẩm dưới mức tồn tối thiểu | `InventoryService.getLowStockWarnings()` |
| `ORDER <mã/ID>` | Tra trạng thái đơn hàng | `OrderService.getOrderById(id)` |
| `QUIT` | Ngắt kết nối | – |

Phản hồi của server:
```
OK <số dòng>
<dòng dữ liệu 1, các cột ngăn bởi |>
...
END
```
hoặc `ERROR <thông báo>` + `END`. Dòng `END` giúp client biết đã đọc hết phản hồi.

Ví dụ:
```
> PRODUCT 5
OK 1
5|Sữa tươi Vinamilk 1L|32000|Tồn: 48
END
```

**5.3. Các lớp cần tạo – package `com.groceryshop.network`**

`StockSocketServer.java` – khởi động cùng Spring Boot
```java
@Slf4j
@Component
@ConditionalOnProperty(name = "app.socket.enabled", havingValue = "true")
public class StockSocketServer {
    @Value("${app.socket.port}") int port;
    @Value("${app.socket.max-clients}") int maxClients;
    private final SocketCommandHandler handler;   // inject qua constructor
    private ServerSocket serverSocket;
    private ExecutorService clientPool;
    private volatile boolean running;
    private final AtomicInteger clientCounter = new AtomicInteger();

    @PostConstruct
    public void start() {
        clientPool = Executors.newFixedThreadPool(maxClients);
        running = true;
        Thread acceptThread = new Thread(this::acceptLoop, "socket-acceptor");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void acceptLoop() {
        try (ServerSocket ss = new ServerSocket(port)) {          // B1: tạo ServerSocket
            serverSocket = ss;
            log.info("Socket server đang lắng nghe cổng {}", port);
            while (running) {
                Socket client = ss.accept();                       // B2: chấp nhận kết nối
                int no = clientCounter.incrementAndGet();
                clientPool.submit(new ClientHandler(client, no, handler)); // mỗi client 1 luồng
            }
        } catch (IOException e) {
            if (running) log.error("Lỗi socket server: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stop() throws IOException {                        // B6: đóng ServerSocket
        running = false;
        if (serverSocket != null) serverSocket.close();
        clientPool.shutdownNow();
    }
}
```

`ClientHandler.java` – `Runnable` xử lý 1 client
```java
public void run() {
    Thread.currentThread().setName("socket-client-" + no);
    try (socket;
         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));   // B3
         PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true)) {
        socket.setSoTimeout(5 * 60 * 1000);                     // client im lặng 5 phút thì đóng
        log.info("[{}] Client {} đã kết nối", name(), socket.getRemoteSocketAddress());
        out.println("WELCOME MiniMart Stock Server – gõ HELP");
        String line;
        while ((line = in.readLine()) != null) {                // B4: trao đổi dữ liệu
            if (line.trim().equalsIgnoreCase("QUIT")) { out.println("BYE"); break; }
            log.info("[{}] Nhận lệnh: {}", name(), line);
            out.print(handler.handle(line));
            out.flush();
        }
    } catch (SocketTimeoutException e) {
        log.warn("[{}] Hết thời gian chờ, đóng kết nối", name());
    } catch (IOException e) {
        log.warn("[{}] Client ngắt kết nối đột ngột: {}", name(), e.getMessage());
    } finally {
        log.info("[{}] Đã đóng kết nối", name());               // B5: socket tự đóng nhờ try-with-resources
    }
}
```

`SocketCommandHandler.java` – `@Component`, phân tích lệnh và **chỉ gọi Service** (không gọi Repository, giữ đúng mô hình tầng)
- `switch` theo lệnh đầu dòng; tham số sai → `ERROR Thiếu tham số`; `NumberFormatException` → `ERROR ID phải là số`; `ResourceNotFoundException` → `ERROR Không tìm thấy`.
- Chỉ dùng các hàm Service trả **DTO** để tránh `LazyInitializationException` (vì luồng socket không có session Hibernate mở sẵn như request web).
- Bọc mọi lỗi bằng `try/catch` để 1 lệnh lỗi không làm chết luồng client.

**5.4. Lưu ý**
- Socket server **không đi qua Spring Security** → chỉ cung cấp lệnh **đọc**; ghi rõ trong báo cáo là "phục vụ tra cứu nội bộ cửa hàng".
- Cổng 9090 phải chưa bị chiếm; nếu bị chiếm, server ghi log lỗi nhưng web app vẫn chạy bình thường.

---

### Bước 6 – Chương trình Client tách biệt (tiêu chí 5.3)

Theo slide 28–30 (các bước xây dựng client).

**6.1. Vị trí**: thư mục mới ở gốc dự án, **không nằm trong** `src/` của web:
```
socket-client/
├── MiniMartClient.java     # 1 file duy nhất, Java thuần, không dùng Spring
└── run-client.bat          # java MiniMartClient.java localhost 9090
```
Java 21 chạy được trực tiếp file nguồn: `java MiniMartClient.java` (không cần biên dịch/Maven).

**6.2. Nội dung client (console)**
```java
public class MiniMartClient {
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9090;
        try (Socket socket = new Socket(host, port);                                   // B1
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8)); // B2
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
             Scanner keyboard = new Scanner(System.in, UTF_8)) {
            System.out.println(in.readLine());               // WELCOME
            while (true) {                                   // B4: trao đổi dữ liệu
                System.out.print("> ");
                String cmd = keyboard.nextLine().trim();
                if (cmd.isEmpty()) continue;
                out.println(cmd);
                if (cmd.equalsIgnoreCase("QUIT")) { System.out.println(in.readLine()); break; }
                String line;
                while ((line = in.readLine()) != null && !line.equals("END")) {
                    System.out.println(line.replace("|", "  |  "));
                }
                if (line == null) { System.out.println("Server đã đóng kết nối."); break; }
            }
        } catch (ConnectException e) {
            System.out.println("Không kết nối được server " + host + ":" + port + ". Hãy chạy web app trước.");
        } catch (IOException e) {
            System.out.println("Lỗi kết nối: " + e.getMessage());
        }                                                    // B5: socket tự đóng
    }
}
```
- Windows console: chạy `chcp 65001` trước (đặt sẵn trong `run-client.bat`) để hiện tiếng Việt có dấu.
- (Tùy chọn, nếu còn thời gian) Làm giao diện Swing đơn giản: 1 ô nhập lệnh + 1 `JTextArea` kết quả, theo mẫu `ClientChatter` ở slide 40–53.

---

### Bước 7 – Kiểm thử 3 client cùng lúc (tiêu chí 5.4)

**7.1. Demo thủ công**
1. Chạy web app (`run.bat`) → log thấy `Socket server đang lắng nghe cổng 9090`.
2. Mở **3 cửa sổ CMD**, mỗi cửa sổ chạy `socket-client\run-client.bat`.
3. Cả 3 gõ lệnh xen kẽ: `PRODUCTS sua`, `LOW_STOCK`, `ORDER 1`.
4. Log server hiện `socket-client-1`, `socket-client-2`, `socket-client-3` → chứng minh phục vụ song song.
5. Sửa giá 1 sản phẩm trên web `/mvc/products` → client gõ `PRODUCT <id>` thấy giá mới (dữ liệu lấy từ CSDL).

**7.2. Các tình huống lỗi cần demo**

| Tình huống | Kết quả mong đợi |
|---|---|
| Chạy client khi web app chưa chạy | Client in "Không kết nối được server…", thoát êm |
| Đóng ngang cửa sổ 1 client | Server log "Client ngắt kết nối đột ngột", 2 client còn lại vẫn chạy |
| Gõ lệnh sai `ABC` / `PRODUCT xyz` | `ERROR Lệnh không hợp lệ` / `ERROR ID phải là số` |
| Tắt web app khi client đang mở | Client in "Server đã đóng kết nối." |

**7.3. Test tự động** `src/test/java/com/groceryshop/concurrency/SocketMultiClientTest.java`
- `@SpringBootTest` (server tự khởi động), tạo **5 luồng client** cùng kết nối, mỗi luồng gửi `PRODUCTS a` rồi `QUIT`.
- Kiểm tra cả 5 đều nhận được dòng bắt đầu bằng `OK` và kết thúc `END`.
- Chụp kết quả cho báo cáo chương 4.

---

### Bước 8 – (Tùy chọn) Client dùng `URL` / `HttpURLConnection`

Theo slide 18–20 (Demo 3: Using URLConnection). Làm thêm nếu dư thời gian, chỉ khoảng 30 dòng:
- `socket-client/MiniMartUrlClient.java`: nhập từ khóa → gọi `GET http://localhost:8080/api/public/products?keyword=...` bằng `HttpURLConnection` → in JSON trả về.
- Cho thấy nhóm biết cả 2 cách: TCP Socket (giao thức tự định nghĩa) và HTTP (giao thức chuẩn).

---

### Bước 9 – Tài liệu (tiêu chí 1.5, chương 4, README)

**9.1. Báo cáo – Chương 2, mục "Thiết kế phần đồng thời và phần mạng"** (tiêu chí 1.5)
- *Vì sao cần đa luồng*: gửi thông báo không làm khách chờ; quét kho định kỳ không cần người bấm; nhiều khách mua cùng lúc; nhiều máy trạm tra cứu cùng lúc.
- *Tài nguyên dùng chung*: `inventory.current_stock`; thread pool `minimart-async-*`; thread pool client socket.
- *Cách đồng bộ*: `PESSIMISTIC_WRITE` trong `@Transactional`; `AtomicInteger` đếm client; `volatile running`.
- *Mô hình client/server*: hình vẽ Client ⇄ TCP:9090 ⇄ `StockSocketServer` → `ClientHandler` (mỗi client 1 luồng) → Service → Repository → SQL Server.
- *Giao thức*: bảng lệnh ở mục 5.2 + định dạng `OK n / dữ liệu / END`.
- 1 sơ đồ tuần tự: Client gửi `PRODUCT 5` → ClientHandler → SocketCommandHandler → ProductService → DB → phản hồi.

**9.2. Báo cáo – Chương 4 "Kiểm thử"**
- Bảng kịch bản: Test 4.1 (race condition), Test 4.2 (20 luồng mua 10 sản phẩm), Test 7.3 (5 client), các tình huống lỗi 7.2.
- Mỗi kịch bản: mục tiêu – các bước – kết quả mong đợi – kết quả thực tế – ảnh chụp.

**9.3. `README.md`** thêm mục:
- JDK 21, cách tạo CSDL, chạy server (`run.bat`), cổng 8080 (web) và 9090 (socket).
- Cách chạy client: `cd socket-client` → `run-client.bat` (mở 3 cửa sổ để demo).
- Cách chạy test đa luồng: `.\mvnw.cmd test -Dtest=InventoryConcurrencyTest`.

---

## PHẦN E. DANH SÁCH FILE SẼ TẠO / SỬA

| Loại | File | Bước |
|---|---|:-:|
| Tạo | `service/DeliveryService.java`, `service/PaymentConfigService.java` | 1 |
| Sửa | `controller/DeliveryController.java`, `controller/PaymentConfigController.java` | 1 |
| Tạo | `config/AsyncConfig.java`, `event/OrderCreatedEvent.java`, `service/OrderNotificationService.java`, `scheduler/InventoryAlertScheduler.java` | 2 |
| Sửa | `OrderService.java` (publish event, lấy tồn kho có khóa), `ProductBatchRepository.java`, `InventoryRepository.java`, `InventoryService.java`, `application.properties` | 2, 3 |
| Tạo | `src/test/.../concurrency/RaceConditionDemoTest.java`, `InventoryConcurrencyTest.java`, `SocketMultiClientTest.java` | 4, 7 |
| Tạo | `network/StockSocketServer.java`, `network/ClientHandler.java`, `network/SocketCommandHandler.java` | 5 |
| Tạo | `socket-client/MiniMartClient.java`, `socket-client/run-client.bat`, (tùy chọn) `MiniMartUrlClient.java` | 6, 8 |
| Sửa | `README.md` | 9 |

---

## PHẦN F. CHECKLIST NGHIỆM THU

- [ ] Không còn Controller nào import Repository hoặc dùng `@Transactional` (2.3)
- [ ] Đặt 1 đơn hàng → log thấy `http-nio-*` trả về trước, `minimart-async-*` gửi thông báo sau (5.1)
- [ ] Log `minimart-scheduler-*` in kết quả quét kho mỗi phút (5.1)
- [ ] `RaceConditionDemoTest` cho thấy biến thường sai, `AtomicInteger` đúng (5.2)
- [ ] `InventoryConcurrencyTest`: 20 luồng / tồn 10 → 10 thành công, 10 thất bại, tồn = 0 (5.2)
- [ ] Client chạy độc lập bằng `java MiniMartClient.java`, lấy được dữ liệu sản phẩm/đơn hàng từ SQL Server (5.3)
- [ ] 3 client mở cùng lúc, log server có `socket-client-1/2/3` (5.4)
- [ ] Đóng ngang client, gõ lệnh sai, server chưa chạy → đều có thông báo thân thiện, server không sập (5.4)
- [ ] Web React và `/mvc` vẫn hoạt động bình thường sau khi thêm các phần trên
- [ ] README có hướng dẫn chạy server, client, test; báo cáo có mục 1.5 và chương 4

---

## PHẦN G. VIỆC CÒN LẠI NGOÀI PHẠM VI PLAN NÀY

| Việc | Tiêu chí | Ghi chú |
|---|:-:|---|
| Thêm dữ liệu mẫu ≥ 20 dòng cho `products`, `inventory`, `users`, `product_batches`, `categories` trong `supermarket_db1.sql` | 4.1 | Làm song song, ~1 giờ |
| Sơ đồ Use Case + yêu cầu phi chức năng; hình ERD | 1.2, 1.3 | Đưa vào báo cáo chương 2 |
| Báo cáo Word/PDF 5 chương (25–40 trang), slide ≤ 20 trang, bảng phân công có chữ ký | 6.1, 6.2, 6.4 | |
| Chạy thử toàn bộ `/mvc/**` trên trình duyệt desktop + điện thoại | 3.2, yêu cầu 5 | |
