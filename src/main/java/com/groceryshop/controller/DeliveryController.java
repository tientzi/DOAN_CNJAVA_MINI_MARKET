package com.groceryshop.controller;

import com.groceryshop.dto.OrderDTO;
import com.groceryshop.service.DeliveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/deliveries")
public class DeliveryController {

    @Autowired
    private DeliveryService deliveryService;

    /**
     * Lấy danh sách các shipper khả dụng, khu vực phụ trách và số đơn đang giao
     */
    @GetMapping("/shippers")
    public ResponseEntity<?> getAvailableShippers() {
        return ResponseEntity.ok(deliveryService.getAvailableShippers());
    }

    /**
     * Lấy danh sách các đơn hàng chưa phân công shipper
     */
    @GetMapping("/unassigned")
    public ResponseEntity<List<OrderDTO>> getUnassignedOrders() {
        return ResponseEntity.ok(deliveryService.getUnassignedOrders());
    }

    /**
     * Lấy danh sách các đơn hàng đang giao hoặc giao thất bại để theo dõi và điều phối lại
     */
    @GetMapping("/tracking")
    public ResponseEntity<List<OrderDTO>> getTrackingOrders() {
        return ResponseEntity.ok(deliveryService.getTrackingOrders());
    }

    /**
     * Lấy danh sách đơn hàng cần giao hoặc đang giao kèm khu vực (tương thích ngược)
     */
    @GetMapping("/orders")
    public ResponseEntity<?> getDeliveryOrders(@RequestParam(required = false) String district) {
        return ResponseEntity.ok(deliveryService.getDeliveryOrders(district));
    }

    /**
     * Phân công Shipper thủ công (1 hoặc nhiều đơn)
     */
    @PostMapping("/assign")
    public ResponseEntity<?> assignOrdersToShipper(@RequestBody Map<String, Object> payload) {
        Object orderIdsObj = payload.get("orderIds");
        Long shipperId = Long.valueOf(payload.get("shipperId").toString());

        List<Long> orderIds = new ArrayList<>();
        if (orderIdsObj instanceof List) {
            for (Object item : (List<?>) orderIdsObj) {
                orderIds.add(Long.valueOf(item.toString()));
            }
        } else if (orderIdsObj != null) {
            orderIds.add(Long.valueOf(orderIdsObj.toString()));
        }

        return ResponseEntity.ok(deliveryService.assignOrdersToShipper(orderIds, shipperId));
    }

    /**
     * Phân bổ nhanh theo khu vực: Tự động quét địa chỉ và gán cho Shipper phụ trách khu vực tương ứng
     */
    @PostMapping("/auto-dispatch")
    public ResponseEntity<?> autoDispatchByArea(@RequestBody(required = false) Map<String, String> payload) {
        String district = payload != null ? payload.get("district") : null;
        return ResponseEntity.ok(deliveryService.autoDispatchByArea(district));
    }

    /**
     * Điều phối lại đơn hàng sang shipper khác
     */
    @PostMapping("/reassign")
    public ResponseEntity<?> reassignOrder(@RequestBody Map<String, Object> payload) {
        Long orderId = Long.valueOf(payload.get("orderId").toString());
        Long newShipperId = Long.valueOf(payload.get("newShipperId").toString());
        return ResponseEntity.ok(deliveryService.reassignOrder(orderId, newShipperId));
    }
}