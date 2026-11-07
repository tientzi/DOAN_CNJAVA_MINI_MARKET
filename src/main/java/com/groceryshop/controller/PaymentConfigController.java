package com.groceryshop.controller;

import com.groceryshop.entity.PaymentMethodConfig;
import com.groceryshop.service.PaymentConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
public class PaymentConfigController {

    @Autowired
    private PaymentConfigService paymentConfigService;

    /**
     * Public API cho trang Checkout: Chỉ lấy các phương thức đang BẬT
     */
    @GetMapping("/api/public/payment-methods")
    public ResponseEntity<List<PaymentMethodConfig>> getPublicPaymentMethods() {
        return ResponseEntity.ok(paymentConfigService.getPublicPaymentMethods());
    }

    /**
     * Admin API: Lấy toàn bộ cấu hình phương thức thanh toán
     */
    @GetMapping("/api/admin/payment-methods")
    public ResponseEntity<List<PaymentMethodConfig>> getAllPaymentMethods() {
        return ResponseEntity.ok(paymentConfigService.getAllPaymentMethods());
    }

    /**
     * Admin API: Tải lên ảnh mã QR riêng biệt và tự động xóa ảnh cũ nếu có
     */
    @PostMapping(value = "/api/admin/payment-methods/upload-qr", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadQrImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "oldQrUrl", required = false) String oldQrUrl) {
        try {
            String savedUrl = paymentConfigService.uploadQrImage(file, oldQrUrl);
            return ResponseEntity.ok(Map.of("url", savedUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi tải ảnh mã QR: " + e.getMessage()));
        }
    }

    /**
     * Admin API: Cập nhật cấu hình phương thức thanh toán
     */
    @PutMapping("/api/admin/payment-methods/{id}")
    public ResponseEntity<?> updatePaymentMethod(
            @PathVariable Long id,
            @RequestBody PaymentMethodConfig request) {
        return ResponseEntity.ok(paymentConfigService.updatePaymentMethod(id, request));
    }

    /**
     * Admin API: Lấy danh sách lịch sử giao dịch thanh toán để đối soát
     */
    @GetMapping("/api/admin/payments/transactions")
    public ResponseEntity<?> getAllTransactions() {
        return ResponseEntity.ok(paymentConfigService.getAllTransactions());
    }

    /**
     * Admin API: Duyệt giao dịch thanh toán của khách hàng
     */
    @PostMapping("/api/admin/payments/transactions/{id}/approve")
    public ResponseEntity<?> approvePayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentConfigService.approvePayment(id));
    }

    /**
     * Admin API: Từ chối giao dịch thanh toán
     */
    @PostMapping("/api/admin/payments/transactions/{id}/reject")
    public ResponseEntity<?> rejectPayment(@PathVariable Long id, @RequestBody(required = false) Map<String, String> payload) {
        String reason = payload != null ? payload.get("reason") : null;
        return ResponseEntity.ok(paymentConfigService.rejectPayment(id, reason));
    }
}