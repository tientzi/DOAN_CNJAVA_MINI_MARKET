package com.groceryshop.controller;

import com.groceryshop.dto.OrderDTO;
import com.groceryshop.dto.OrderJob;
import com.groceryshop.security.UserPrincipal;
import com.groceryshop.service.IdempotencyService;
import com.groceryshop.service.OrderQueueService;
import com.groceryshop.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private OrderQueueService orderQueueService;

    @PostMapping("/api/orders")
    public ResponseEntity<OrderDTO> createOrder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody OrderDTO orderDTO) {
        Long userId = userPrincipal.getId();
        OrderDTO created = idempotencyService.executeOrderWithIdempotency(
                userId,
                idempotencyKey,
                orderDTO,
                () -> {
                    OrderDTO queuedOrder = orderService.submitOrderToQueue(userId, orderDTO);
                    OrderJob job = orderQueueService.enqueueOrder(queuedOrder, queuedOrder.getMembershipTier());
                    return orderQueueService.awaitJobResultIfActive(job.getJobId(), queuedOrder, 2000L);
                }
        );
        return ResponseEntity.ok(created);
    }

    @GetMapping("/api/orders/my-orders")
    public ResponseEntity<List<OrderDTO>> getMyOrders(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(orderService.getMyOrders(userPrincipal.getId()));
    }

    @PutMapping("/api/orders/{id}/cancel")
    public ResponseEntity<OrderDTO> cancelOrder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        OrderDTO cancelled = orderService.cancelOrder(userPrincipal.getId(), id);
        orderQueueService.markOrderCancelledOrExpired(id, "Khách hàng hủy đơn #" + id);
        return ResponseEntity.ok(cancelled);
    }

    @PostMapping("/api/orders/{id}/retry-payment")
    public ResponseEntity<OrderDTO> retryPayment(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        return ResponseEntity.ok(orderService.retryPayment(userPrincipal.getId(), id));
    }

    @PostMapping("/api/orders/{id}/reorder")
    public ResponseEntity<Map<String, Object>> reorderToCart(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        boolean cancelledOld = orderService.reorderToCart(userPrincipal.getId(), id);
        if (cancelledOld) {
            orderQueueService.markOrderCancelledOrExpired(id, "Khách hàng chọn đặt hàng lại đơn chưa thanh toán #" + id);
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Đã đưa sản phẩm vào giỏ hàng để đặt lại!"));
    }

    @GetMapping("/api/admin/orders")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/api/admin/orders/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PutMapping("/api/admin/orders/{id}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        OrderDTO updated = orderService.updateOrderStatus(id, status);
        if ("DA_XAC_NHAN".equals(status) || "HOAN_THANH".equals(status)) {
            orderQueueService.markOrderPaymentSucceeded(id);
        } else if ("HUY".equals(status)) {
            orderQueueService.markOrderCancelledOrExpired(id, "Đơn hàng #" + id + " đã bị hủy bởi Admin");
        }
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/api/admin/orders/{id}/confirm-payment")
    public ResponseEntity<OrderDTO> confirmPayment(@PathVariable Long id) {
        OrderDTO confirmed = orderService.confirmPayment(id);
        orderQueueService.markOrderPaymentSucceeded(id);
        return ResponseEntity.ok(confirmed);
    }
}
