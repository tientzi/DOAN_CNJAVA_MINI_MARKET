package com.groceryshop.concurrency;

import com.groceryshop.dto.OrderDTO;
import com.groceryshop.entity.*;
import com.groceryshop.repository.*;
import com.groceryshop.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Kiểm thử tích hợp (@SpringBootTest) trên CSDL SQL Server thật (Tiêu chí 5.2)
 * Gọi trực tiếp OrderService.createOrder() sử dụng khóa bi quan JPA:
 *   @Lock(LockModeType.PESSIMISTIC_WRITE) -> InventoryRepository.findByProductIdWithLock()
 */
@SpringBootTest
public class OrderConcurrencyIT {

    private static final Logger log = LoggerFactory.getLogger(OrderConcurrencyIT.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private InventoryRepository inventoryRepo;

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private CategoryRepository categoryRepo;

    @Autowired
    private BrandRepository brandRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private RoleRepository roleRepo;

    @Autowired
    private CartRepository cartRepo;

    @Autowired
    private OrderRepository orderRepo;

    @Autowired
    private OrderItemRepository orderItemRepo;

    @Autowired
    private PaymentRepository paymentRepo;

    @Autowired
    private InventoryLedgerRepository ledgerRepo;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("5.2 IT - 20 khách hàng mua đồng thời sản phẩm tồn kho = 10 trên SQL Server thật (JPA Pessimistic Lock)")
    void twentyBuyersTenStock_noOverselling() throws Exception {
        final int initialStock = 10;
        final int buyerCount = 20;
        final String suffix = String.valueOf(System.currentTimeMillis());

        List<Long> createdUserIds = new ArrayList<>();
        List<Long> createdOrderIds = Collections.synchronizedList(new ArrayList<>());

        // 1. Tạo sản phẩm test thật với tồn kho = 10 và 20 user có giỏ hàng chứa 1 sản phẩm đó
        Long testProductId = transactionTemplate.execute(status -> {
            Category cat = categoryRepo.findAll().stream().findFirst().orElseGet(() ->
                    categoryRepo.save(Category.builder().name("DM-IT-" + suffix).isActive(true).build()));
            Brand brand = brandRepo.findAll().stream().findFirst().orElseGet(() ->
                    brandRepo.save(Brand.builder().name("TH-IT-" + suffix).build()));
            Role userRole = roleRepo.findByName("ROLE_USER").orElseGet(() ->
                    roleRepo.save(Role.builder().name("ROLE_USER").build()));

            Product p = Product.builder()
                    .name("SP Test Khóa JPA Pessimistic Lock " + suffix)
                    .sku("SKU-IT-" + suffix)
                    .price(BigDecimal.valueOf(25000))
                    .category(cat)
                    .brand(brand)
                    .unit("Hộp")
                    .isActive(true)
                    .build();
            Product savedProduct = productRepo.save(p);

            Inventory inv = Inventory.builder()
                    .product(savedProduct)
                    .currentStock(initialStock)
                    .minimumStock(2)
                    .location("Kệ Test Concurrency")
                    .build();
            inventoryRepo.save(inv);
            savedProduct.setInventory(inv);

            for (int i = 1; i <= buyerCount; i++) {
                User u = User.builder()
                        .username("it_buyer_" + i + "_" + suffix)
                        .email("it_buyer_" + i + "_" + suffix + "@test.local")
                        .password("123456")
                        .fullName("Khách Hàng Đồng Thời #" + i)
                        .phone("09000000" + String.format("%02d", i))
                        .role(userRole)
                        .isActive(true)
                        .build();
                User savedUser = userRepo.save(u);
                createdUserIds.add(savedUser.getId());

                Cart cart = Cart.builder().user(savedUser).build();
                CartItem ci = CartItem.builder()
                        .cart(cart)
                        .product(savedProduct)
                        .quantity(1)
                        .build();
                cart.getItems().add(ci);
                cartRepo.save(cart);
            }

            return savedProduct.getId();
        });

        try {
            // 2. Chạy đồng thời 20 luồng gọi thẳng OrderService.createOrder() trên CSDL SQL Server thật
            ExecutorService pool = Executors.newFixedThreadPool(buyerCount);
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger ok = new AtomicInteger(0);
            AtomicInteger fail = new AtomicInteger(0);

            for (int i = 0; i < buyerCount; i++) {
                final Long userId = createdUserIds.get(i);
                final int buyerIdx = i + 1;
                pool.submit(() -> {
                    start.await();
                    try {
                        OrderDTO req = OrderDTO.builder()
                                .shippingName("Khách Hàng #" + buyerIdx)
                                .shippingPhone("09000000" + String.format("%02d", buyerIdx))
                                .shippingAddress("TP. Hồ Chí Minh")
                                .paymentMethod("COD")
                                .note("Kiểm thử đồng thời OrderConcurrencyIT")
                                .build();
                        OrderDTO created = orderService.createOrder(userId, req);
                        createdOrderIds.add(created.getId());
                        ok.incrementAndGet();
                        log.info("[Luồng {}] Khách #{} ĐẶT HÀNG THÀNH CÔNG -> Mã đơn: #{}",
                                Thread.currentThread().getName(), buyerIdx, created.getId());
                    } catch (Exception e) {
                        fail.incrementAndGet();
                        log.warn("[Luồng {}] Khách #{} BỊ TỪ CHỐI: {}",
                                Thread.currentThread().getName(), buyerIdx, e.getMessage());
                    }
                    return null;
                });
            }

            start.countDown(); // Kích hoạt 20 luồng cùng lúc
            pool.shutdown();
            pool.awaitTermination(30, TimeUnit.SECONDS);

            // 3. Kiểm tra tồn kho thực tế trong SQL Server qua findByProductIdWithLock
            Integer finalStock = transactionTemplate.execute(status ->
                    inventoryRepo.findByProductIdWithLock(testProductId)
                            .map(Inventory::getCurrentStock)
                            .orElse(-1)
            );

            log.info("==================================================================");
            log.info("KẾT QUẢ KIỂM THỬ TÍCH HỢP ORDER CONCURRENCY IT (SQL SERVER THẬT)");
            log.info("  - Sản phẩm kiểm thử ID : #{}", testProductId);
            log.info("  - Tồn kho ban đầu      : {}", initialStock);
            log.info("  - Số khách mua cùng lúc: {} luồng (mỗi khách mua 1 SP)", buyerCount);
            log.info("  - Số đơn THÀNH CÔNG    : {}", ok.get());
            log.info("  - Số đơn BỊ TỪ CHỐI    : {}", fail.get());
            log.info("  - Tồn kho cuối trong DB: {}", finalStock);
            log.info("==================================================================");

            assertEquals(10, ok.get(), "Phải có đúng 10 đơn hàng thành công");
            assertEquals(10, fail.get(), "Phải có đúng 10 đơn hàng bị từ chối khi kho hết");
            assertEquals(0, finalStock, "Tồn kho trong SQL Server phải bằng đúng 0, tuyệt đối không âm");
        } finally {
            // 4. Dọn dẹp dữ liệu test tạm thời để giữ nguyên trạng thái CSDL
            transactionTemplate.executeWithoutResult(status -> {
                for (Long orderId : createdOrderIds) {
                    orderRepo.findById(orderId).ifPresent(o -> {
                        if (o.getPayment() != null) {
                            paymentRepo.delete(o.getPayment());
                        }
                        if (o.getItems() != null) {
                            orderItemRepo.deleteAll(o.getItems());
                        }
                        orderRepo.delete(o);
                    });
                }
                List<InventoryLedger> logs = ledgerRepo.findByInventoryProductIdOrderByCreatedAtDesc(testProductId);
                if (!logs.isEmpty()) {
                    ledgerRepo.deleteAll(logs);
                }
                for (Long uid : createdUserIds) {
                    cartRepo.findByUserId(uid).ifPresent(cartRepo::delete);
                    userRepo.findById(uid).ifPresent(userRepo::delete);
                }
                inventoryRepo.findByProductId(testProductId).ifPresent(inventoryRepo::delete);
                productRepo.findById(testProductId).ifPresent(productRepo::delete);
            });
        }
    }
}

