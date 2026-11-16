package com.groceryshop.task;

import com.groceryshop.entity.Order;
import com.groceryshop.entity.Payment;
import com.groceryshop.repository.OrderRepository;
import com.groceryshop.service.OrderQueueService;
import com.groceryshop.service.OrderService;
import com.groceryshop.service.SystemActivityService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bộ quét tự động xử lý đơn thanh toán Online (VietQR / MoMo) quá hạn 30 giây mà Admin chưa duyệt.
 * Chỉ áp dụng cho các đơn được tạo sau thời điểm server khởi động để không ảnh hưởng dữ liệu mẫu lịch sử.
 */
@Component
public class PaymentExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentExpiryScheduler.class);
    private static final long PAYMENT_APPROVAL_TIMEOUT_SECONDS = 30L;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderQueueService orderQueueService;

    @Autowired
    private SystemActivityService systemActivityService;

    private LocalDateTime schedulerBootTime;

    @PostConstruct
    public void init() {
        this.schedulerBootTime = LocalDateTime.now();
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void checkExpiredOnlineOrders() {
        if (systemActivityService.isWriteLocked()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusSeconds(PAYMENT_APPROVAL_TIMEOUT_SECONDS);

        List<Order> pendingOrders = orderRepository.findByStatusAndCreatedAtAfterOrderByCreatedAtAsc("CHO_XAC_NHAN", schedulerBootTime);
        for (Order order : pendingOrders) {
            String pm = order.getPaymentMethod();
            if (pm == null || "COD".equalsIgnoreCase(pm) || "TIEN_MAT".equalsIgnoreCase(pm)) {
                continue;
            }

            Payment payment = order.getPayment();
            String pStatus = payment != null ? payment.getPaymentStatus() : "PENDING";
            if (!"PENDING".equals(pStatus)) {
                continue;
            }

            LocalDateTime referenceTime = order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt();
            if (!referenceTime.isAfter(cutoff)) {
                try {
                    boolean expired = orderService.expireUnapprovedOnlineOrder(order.getId());
                    if (expired) {
                        String reason = "Thanh toán không thành công (Quá hạn 30 giây Admin chưa duyệt giao dịch)";
                        orderQueueService.markOrderCancelledOrExpired(order.getId(), reason);
                        systemActivityService.addLog(
                                "EXPIRY",
                                "Thanh toán không thành công (Quá hạn 30s)",
                                "Đơn #" + order.getId() + " (" + pm + ") đã tự hủy và hoàn trả toàn bộ tồn kho",
                                "WARN"
                        );
                        log.warn("[Luồng: {}] [EXPIRY 30s] Đơn #{} quá hạn duyệt thanh toán -> Tự động hủy & hoàn kho!",
                                Thread.currentThread().getName(), order.getId());
                    }
                } catch (Exception e) {
                    log.error("Lỗi khi hủy đơn quá hạn #{}: {}", order.getId(), e.getMessage());
                }
            }
        }
    }
}
