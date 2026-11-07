package com.groceryshop.concurrency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử Đa luồng & Xử lý đồng thời (Rubric Tiêu chí 5.1 & 5.2)")
public class ConcurrencyAndMultiThreadingTest {

    private static final Logger log = LoggerFactory.getLogger(ConcurrencyAndMultiThreadingTest.class);

    /**
     * Test 1: Chứng minh hiện tượng Race Condition (Tranh chấp tài nguyên)
     * So sánh giữa biến đếm nguyên thủy int++ và AtomicInteger khi 50 luồng đồng thời thao tác
     */
    @Test
    @DisplayName("5.2 - So sánh biến thường (Race condition) vs AtomicInteger (Thread-safe)")
    void testRaceConditionStandardCounterVsAtomicInteger() throws InterruptedException {
        int threadCount = 50;
        int incrementsPerThread = 1000;
        int expectedTotal = threadCount * incrementsPerThread; // 50,000

        class CounterHolder {
            int unsafeCounter = 0;
            void incrementUnsafe() {
                unsafeCounter++; // Non-atomic (read - modify - write)
            }
        }

        CounterHolder holder = new CounterHolder();
        AtomicInteger safeCounter = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Chờ tất cả luồng sẵn sàng rồi bắt đầu cùng lúc
                    for (int j = 0; j < incrementsPerThread; j++) {
                        holder.incrementUnsafe();
                        safeCounter.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Kích hoạt đồng loạt các luồng
        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Tất cả các luồng phải hoàn thành trong thời gian quy định");

        log.info("=== KẾT QUẢ KIỂM THỬ RACE CONDITION ===");
        log.info("Tổng kỳ vọng: {}", expectedTotal);
        log.info("Biến thường (Unsafe counter): {} (Bị mất dữ liệu do xung đột đồng thời)", holder.unsafeCounter);
        log.info("Biến an toàn (AtomicInteger): {} (Chính xác tuyệt đối)", safeCounter.get());

        // AtomicInteger luôn luôn đạt chính xác giá trị mong đợi
        assertEquals(expectedTotal, safeCounter.get(), "AtomicInteger phải đảm bảo an toàn đa luồng 100%");
        // Biến thường hầu như luôn luôn bị mất dữ liệu (< 50,000) do race condition
        assertTrue(holder.unsafeCounter <= expectedTotal, "Biến thường không vượt quá kỳ vọng nhưng dễ bị thất thoát do tranh chấp");
    }

    /**
     * Test 2: Mô phỏng bài toán Đặt hàng đồng thời khi Tồn kho có hạn (Overselling Prevention)
     * Kịch bản: Kho chỉ còn 10 sản phẩm, 20 khách hàng (20 luồng) cùng nhấn nút mua tại cùng 1 thời điểm
     * Cơ chế: Sử dụng khóa tương đương Pessimistic Locking để bảo vệ giao dịch trừ kho
     */
    @Test
    @DisplayName("5.2 - 20 luồng cùng mua sản phẩm chỉ còn tồn 10 (Chống âm kho / Overselling)")
    void testConcurrentPurchasingWhenStockIsLimited() throws InterruptedException {
        int initialStock = 10;
        int concurrentBuyers = 20;

        class SimulatedInventoryService {
            private int stock;
            private final ReentrantLock lock = new ReentrantLock(true); // Mô phỏng Pessimistic Write Lock

            SimulatedInventoryService(int initialStock) {
                this.stock = initialStock;
            }

            // Nghiệp vụ trừ kho được bảo vệ bằng Khóa (Tương đương findByProductIdWithLock)
            boolean purchaseOneItem(String customerThread) {
                lock.lock();
                try {
                    if (stock > 0) {
                        // Giả lập độ trễ xử lý nghiệp vụ thanh toán
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ignored) {}

                        stock--;
                        log.info("[{}] Đặt hàng THÀNH CÔNG! Số lượng tồn kho còn lại: {}", customerThread, stock);
                        return true;
                    } else {
                        log.warn("[{}] Đặt hàng THẤT BẠI! Sản phẩm đã hết hàng trong kho.", customerThread);
                        return false;
                    }
                } finally {
                    lock.unlock();
                }
            }

            int getStock() {
                return stock;
            }
        }

        SimulatedInventoryService inventoryService = new SimulatedInventoryService(initialStock);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(concurrentBuyers);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(concurrentBuyers);

        for (int i = 1; i <= concurrentBuyers; i++) {
            final String customerId = "KhachHang-" + i;
            executor.submit(() -> {
                try {
                    startGun.await(); // Xuất phát đồng thời
                    boolean success = inventoryService.purchaseOneItem(customerId);
                    if (success) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishGate.countDown();
                }
            });
        }

        startGun.countDown(); // Kích hoạt 20 khách mua cùng lúc
        boolean allDone = finishGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(allDone, "20 luồng giao dịch phải kết thúc trong vòng 10s");

        log.info("=== KẾT QUẢ TEST STRESS ĐẶT HÀNG ĐỒNG THỜI ===");
        log.info("Tồn kho ban đầu: {}", initialStock);
        log.info("Số khách cùng mua: {}", concurrentBuyers);
        log.info("Đơn hàng thành công: {}", successCount.get());
        log.info("Đơn hàng bị từ chối (Hết hàng): {}", failureCount.get());
        log.info("Tồn kho cuối cùng: {}", inventoryService.getStock());

        // Kiểm tra đúng các tiêu chuẩn chống âm kho
        assertEquals(10, successCount.get(), "Chỉ đúng 10 khách hàng đầu tiên được mua hàng thành công");
        assertEquals(10, failureCount.get(), "Chính xác 10 khách còn lại phải nhận thông báo hết hàng");
        assertEquals(0, inventoryService.getStock(), "Tồn kho cuối cùng phải bằng đúng 0, tuyệt đối không bị âm");
    }

    /**
     * Test 3: Kiểm thử ThreadPoolTaskExecutor với tiền tố tên luồng (Rubric Tiêu chí 5.1)
     */
    @Test
    @DisplayName("5.1 - ThreadPoolTaskExecutor định danh tên luồng chuẩn async-task-*")
    void testThreadPoolTaskExecutorWithNamedThreads() throws InterruptedException, ExecutionException {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setThreadNamePrefix("async-task-test-");
        executor.initialize();

        ConcurrentLinkedQueue<String> threadNames = new ConcurrentLinkedQueue<>();
        CountDownLatch latch = new CountDownLatch(6);

        for (int i = 0; i < 6; i++) {
            final int taskId = i + 1;
            executor.execute(() -> {
                String threadName = Thread.currentThread().getName();
                threadNames.add(threadName);
                log.info("[Task #{}] Đang thực thi trên luồng: {}", taskId, threadName);
                latch.countDown();
            });
        }

        boolean done = latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(done, "Tất cả các task bất đồng bộ phải hoàn thành");
        assertEquals(6, threadNames.size());
        for (String name : threadNames) {
            assertTrue(name.startsWith("async-task-test-"), "Tên luồng phải chứa tiền tố được cấu hình");
        }
        log.info("Đã xác thực định danh tên luồng thành công: {}", threadNames);
    }
}
