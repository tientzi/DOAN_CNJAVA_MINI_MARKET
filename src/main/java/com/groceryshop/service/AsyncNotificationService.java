package com.groceryshop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AsyncNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AsyncNotificationService.class);

    /**
     * Tác vụ bất đồng bộ: Gửi thông báo và ghi nhận kiểm toán đơn hàng mới
     * Luồng thực thi: async-task-* (không chặn luồng web request)
     */
    @Async("taskExecutor")
    public void sendOrderCreatedNotification(Long orderId, String customerName, BigDecimal totalAmount) {
        String threadName = Thread.currentThread().getName();
        log.info("[Luồng: {}] [BẤT ĐỒNG BỘ] Bắt đầu xử lý gửi thông báo & audit log cho đơn hàng #{} - Khách hàng: {} - Tổng tiền: {} đ",
                threadName, orderId, customerName, totalAmount);

        try {
            // Giả lập thời gian gửi email / thông báo push / ghi log kiểm toán bên thứ 3
            Thread.sleep(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("[Luồng: {}] [BẤT ĐỒNG BỘ] Đã hoàn thành gửi thông báo xác nhận cho đơn hàng #{} thành công!",
                threadName, orderId);
    }
}
