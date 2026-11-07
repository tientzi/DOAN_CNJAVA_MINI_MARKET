package com.groceryshop.task;

import com.groceryshop.entity.Inventory;
import com.groceryshop.entity.ProductBatch;
import com.groceryshop.repository.InventoryRepository;
import com.groceryshop.repository.ProductBatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class ScheduledInventoryTask {

    private static final Logger log = LoggerFactory.getLogger(ScheduledInventoryTask.class);

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductBatchRepository productBatchRepository;

    /**
     * Tác vụ định kỳ (Scheduled): Tự động quét kiểm tra hàng tồn kho thấp và lô hàng cận date
     * Chạy mỗi 5 phút (initial delay 20 giây sau khi ứng dụng khởi động)
     * Luồng thực thi: scheduled-scan-*
     */
    @Scheduled(fixedRate = 300000, initialDelay = 20000)
    @Transactional(readOnly = true)
    public void scanLowStockAndExpiringBatches() {
        String threadName = Thread.currentThread().getName();
        log.info("[Luồng: {}] [ĐỊNH KỲ] Bắt đầu quét kiểm tra tự động cảnh báo kho hàng...", threadName);

        try {
            // 1. Quét sản phẩm sắp hết hàng (tồn kho < 10)
            List<Inventory> lowStockList = inventoryRepository.findAll().stream()
                    .filter(inv -> inv.getCurrentStock() != null && inv.getCurrentStock() < 10)
                    .toList();

            if (!lowStockList.isEmpty()) {
                log.warn("[Luồng: {}] [CẢNH BÁO TỒN THẤP] Phát hiện {} sản phẩm có tồn kho dưới 10 đơn vị:",
                        threadName, lowStockList.size());
                for (Inventory inv : lowStockList) {
                    String productName = inv.getProduct() != null ? inv.getProduct().getName() : "ID #" + inv.getId();
                    log.warn("   -> Sản phẩm: '{}' | Tồn hiện tại: {} | Tồn tối thiểu: {}",
                            productName, inv.getCurrentStock(), inv.getMinimumStock());
                }
            } else {
                log.info("[Luồng: {}] Tồn kho an toàn, không có sản phẩm nào dưới ngưỡng tối thiểu.", threadName);
            }

            // 2. Quét lô hàng cận date (còn hạn < 7 ngày hoặc đã hết hạn)
            LocalDate today = LocalDate.now();
            LocalDate warningDate = today.plusDays(7);
            List<ProductBatch> expiringBatches = productBatchRepository.findAll().stream()
                    .filter(b -> b.getQuantity() != null && b.getQuantity() > 0)
                    .filter(b -> b.getExpiryDate() != null && !b.getExpiryDate().isAfter(warningDate))
                    .toList();

            if (!expiringBatches.isEmpty()) {
                log.warn("[Luồng: {}] [CẢNH BÁO HẠN DÙNG] Phát hiện {} lô sản phẩm cận date (dưới 7 ngày) hoặc hết hạn:",
                        threadName, expiringBatches.size());
                for (ProductBatch b : expiringBatches) {
                    String pName = b.getProduct() != null ? b.getProduct().getName() : "SP #" + b.getId();
                    log.warn("   -> Lô: '{}' | SP: '{}' | HSD: {} | SL còn lại: {}",
                            b.getBatchName(), pName, b.getExpiryDate(), b.getQuantity());
                }
            }

            log.info("[Luồng: {}] [ĐỊNH KỲ] Quét kiểm tra kho hàng hoàn tất.", threadName);
        } catch (Exception e) {
            log.error("[Luồng: {}] Lỗi trong quá trình quét định kỳ: {}", threadName, e.getMessage());
        }
    }
}