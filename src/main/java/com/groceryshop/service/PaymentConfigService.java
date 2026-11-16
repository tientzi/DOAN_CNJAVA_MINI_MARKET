package com.groceryshop.service;

import com.groceryshop.entity.Order;
import com.groceryshop.entity.Payment;
import com.groceryshop.entity.PaymentMethodConfig;
import com.groceryshop.repository.OrderRepository;
import com.groceryshop.repository.PaymentMethodConfigRepository;
import com.groceryshop.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentConfigService {

    @Autowired
    private PaymentMethodConfigRepository paymentMethodConfigRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private OrderService orderService;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private OrderQueueService orderQueueService;

    public List<PaymentMethodConfig> getPublicPaymentMethods() {
        return paymentMethodConfigRepository.findByIsEnabledTrue();
    }

    public List<PaymentMethodConfig> getAllPaymentMethods() {
        return paymentMethodConfigRepository.findAll();
    }

    public String uploadQrImage(MultipartFile file, String oldQrUrl) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn file ảnh mã QR hợp lệ");
        }

        if (oldQrUrl != null && !oldQrUrl.trim().isEmpty() && oldQrUrl.startsWith("/uploads/")) {
            fileStorageService.deleteFile(oldQrUrl.trim());
        }

        return fileStorageService.saveFile(file);
    }

    @Transactional
    public PaymentMethodConfig updatePaymentMethod(Long id, PaymentMethodConfig request) {
        PaymentMethodConfig config = paymentMethodConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình phương thức thanh toán #" + id));

        if (request.getName() != null) config.setName(request.getName());
        if (request.getDescription() != null) config.setDescription(request.getDescription());
        if (request.getIsEnabled() != null) config.setIsEnabled(request.getIsEnabled());
        if (request.getBankName() != null) config.setBankName(request.getBankName());
        if (request.getAccountNumber() != null) config.setAccountNumber(request.getAccountNumber());
        if (request.getAccountHolder() != null) config.setAccountHolder(request.getAccountHolder());
        if (request.getTransferSyntax() != null) config.setTransferSyntax(request.getTransferSyntax());

        String newQr = request.getQrCodeUrl() != null ? request.getQrCodeUrl() : request.getQrImageUrl();
        String oldQr = config.getQrCodeUrl() != null ? config.getQrCodeUrl() : config.getQrImageUrl();
        if (newQr != null && !newQr.equals(oldQr)) {
            if (oldQr != null && oldQr.startsWith("/uploads/") && !oldQr.equals(newQr)) {
                fileStorageService.deleteFile(oldQr);
            }
            config.setQrCodeUrl(newQr);
            config.setQrImageUrl(newQr);
        }

        config.setUpdatedAt(LocalDateTime.now());
        return paymentMethodConfigRepository.save(config);
    }

    public List<Map<String, Object>> getAllTransactions() {
        List<Payment> payments = paymentRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Payment p : payments) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("orderId", p.getOrder() != null ? p.getOrder().getId() : null);
            map.put("customerName", p.getOrder() != null ? p.getOrder().getShippingName() : "—");
            map.put("orderStatus", p.getOrder() != null ? p.getOrder().getStatus() : null);
            map.put("deliveryNote", p.getOrder() != null ? p.getOrder().getDeliveryNote() : null);
            map.put("paymentMethod", p.getPaymentMethod());
            map.put("paymentStatus", p.getPaymentStatus());
            map.put("amount", p.getAmount());
            map.put("paidAt", p.getPaidAt());
            map.put("createdAt", p.getOrder() != null ? p.getOrder().getCreatedAt() : null);
            result.add(map);
        }
        result.sort((a, b) -> {
            LocalDateTime tA = (LocalDateTime) a.get("createdAt");
            LocalDateTime tB = (LocalDateTime) b.get("createdAt");
            if (tA == null) return 1;
            if (tB == null) return -1;
            return tB.compareTo(tA);
        });
        return result;
    }

    @Transactional
    public Map<String, Object> approvePayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch thanh toán #" + id));

        boolean isCod = "TIEN_MAT".equalsIgnoreCase(payment.getPaymentMethod()) || "COD".equalsIgnoreCase(payment.getPaymentMethod());
        if (isCod) {
            payment.setPaymentStatus("APPROVED_COD");
        } else {
            payment.setPaymentStatus("COMPLETED");
            payment.setPaidAt(LocalDateTime.now());
        }
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        if (order != null && "CHO_XAC_NHAN".equals(order.getStatus())) {
            order.setStatus("DA_XAC_NHAN");
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        }
        if (order != null) {
            orderQueueService.markOrderPaymentSucceeded(order.getId());
        }

        Map<String, Object> res = new HashMap<>();
        res.put("message", isCod ? "Đã duyệt đơn hàng Tiền mặt (COD) thành công!" : "Đã duyệt thanh toán QR thành công!");
        res.put("paymentStatus", payment.getPaymentStatus());
        return res;
    }

    @Transactional
    public Map<String, Object> rejectPayment(Long id, String reason) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch thanh toán #" + id));

        payment.setPaymentStatus("FAILED");
        paymentRepository.save(payment);

        String finalReason = reason != null && !reason.trim().isEmpty() ? reason : "Chưa nhận được tiền";
        Order order = payment.getOrder();
        if (order != null) {
            order.setStatus("HUY");
            order.setDeliveryNote("[Từ chối thanh toán]: " + finalReason);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            // Tự động hoàn lại tồn kho, lô hàng và mã giảm giá
            orderService.restoreInventoryAndCoupon(order, null, "Từ chối thanh toán đơn #" + order.getId() + ": " + finalReason);
            orderQueueService.markOrderCancelledOrExpired(order.getId(), "Từ chối thanh toán: " + finalReason);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Đã từ chối thanh toán, hủy đơn và hoàn trả kho thành công!");
        res.put("paymentStatus", "FAILED");
        return res;
    }
}