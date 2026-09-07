package com.groceryshop.service;

import com.groceryshop.dto.OrderDTO;
import com.groceryshop.entity.*;
import com.groceryshop.exception.BadRequestException;
import com.groceryshop.exception.ResourceNotFoundException;
import com.groceryshop.mapper.EntityMapper;
import com.groceryshop.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CouponService couponService;

    @Autowired
    private InventoryLedgerService ledgerService;

    @Autowired
    private ProductBatchRepository productBatchRepository;

    @Transactional
    public OrderDTO createOrder(Long userId, OrderDTO orderDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giỏ hàng của người dùng"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng của bạn đang trống");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (!product.getIsActive()) {
                throw new BadRequestException("Sản phẩm '" + product.getName() + "' hiện tại không hoạt động");
            }
            Inventory inventory = product.getInventory();
            int currentStock = inventory != null ? inventory.getCurrentStock() : 0;
            if (item.getQuantity() > currentStock) {
                throw new BadRequestException("Sản phẩm '" + product.getName() + "' không đủ hàng trong kho (Còn lại: " + currentStock + ")");
            }

            BigDecimal itemPrice = product.getSalePrice() != null ? product.getSalePrice() : product.getPrice();
            totalAmount = totalAmount.add(itemPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        Coupon appliedCoupon = null;
        if (orderDTO.getCouponCode() != null && !orderDTO.getCouponCode().trim().isEmpty()) {
            discountAmount = couponService.calculateDiscount(orderDTO.getCouponCode(), totalAmount);
            appliedCoupon = couponRepository.findByCode(orderDTO.getCouponCode()).orElse(null);
        }

        // Apply Membership Tier Discount
        BigDecimal tierDiscount = BigDecimal.ZERO;
        if (user.getMembershipTier() != null) {
            if (user.getMembershipTier().equals("SILVER")) {
                tierDiscount = totalAmount.multiply(BigDecimal.valueOf(0.02));
            } else if (user.getMembershipTier().equals("GOLD")) {
                tierDiscount = totalAmount.multiply(BigDecimal.valueOf(0.05));
            } else if (user.getMembershipTier().equals("DIAMOND")) {
                tierDiscount = totalAmount.multiply(BigDecimal.valueOf(0.08));
            }
        }
        
        discountAmount = discountAmount.add(tierDiscount);

        BigDecimal finalAmount = totalAmount.subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        String pm = orderDTO.getPaymentMethod();
        if (pm == null || pm.trim().isEmpty() || "undefined".equalsIgnoreCase(pm.trim())) {
            pm = "COD";
        }

        Order order = Order.builder()
                .user(user)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .status("CHO_XAC_NHAN")
                .shippingName(orderDTO.getShippingName())
                .shippingPhone(orderDTO.getShippingPhone())
                .shippingAddress(orderDTO.getShippingAddress())
                .paymentMethod(pm)
                .couponCode(orderDTO.getCouponCode())
                .note(orderDTO.getNote())
                .build();

        Order savedOrder = orderRepository.save(order);

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            BigDecimal itemPrice = product.getSalePrice() != null ? product.getSalePrice() : product.getPrice();

            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .product(product)
                    .quantity(item.getQuantity())
                    .price(itemPrice)
                    .productName(product.getName())
                    .productImage(product.getMainImage())
                    .build();
            orderItemRepository.save(orderItem);
            savedOrder.getItems().add(orderItem);

            Inventory inventory = product.getInventory();
            if (inventory != null) {
                inventory.setCurrentStock(inventory.getCurrentStock() - item.getQuantity());
                inventoryRepository.save(inventory);
                
                // Deduct from batches (FIFO)
                int quantityToDeduct = item.getQuantity();
                List<ProductBatch> batches = productBatchRepository.findByProductIdAndQuantityGreaterThanOrderByExpiryDateAsc(product.getId(), 0);
                for (ProductBatch batch : batches) {
                    if (quantityToDeduct <= 0) break;
                    if (batch.getQuantity() >= quantityToDeduct) {
                        batch.setQuantity(batch.getQuantity() - quantityToDeduct);
                        quantityToDeduct = 0;
                    } else {
                        quantityToDeduct -= batch.getQuantity();
                        batch.setQuantity(0);
                    }
                    productBatchRepository.save(batch);
                }
                
                ledgerService.recordLog(
                    inventory, 
                    "SELL", 
                    -item.getQuantity(), 
                    savedOrder.getId(), 
                    "Bán hàng theo đơn #" + savedOrder.getId(), 
                    user
                );
            }
        }

        if (appliedCoupon != null) {
            appliedCoupon.setUsedCount(appliedCoupon.getUsedCount() + 1);
            couponRepository.save(appliedCoupon);
        }

        Payment payment = Payment.builder()
                .order(savedOrder)
                .paymentMethod(orderDTO.getPaymentMethod())
                .paymentStatus("PENDING")
                .amount(finalAmount)
                .build();
        paymentRepository.save(payment);
        savedOrder.setPayment(payment);

        cart.getItems().clear();
        cartRepository.save(cart);

        return EntityMapper.toOrderDTO(savedOrder);
    }

    public List<OrderDTO> getMyOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(EntityMapper::toOrderDTO)
                .collect(Collectors.toList());
    }

    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(EntityMapper::toOrderDTO)
                .collect(Collectors.toList());
    }

    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id: " + id));
        return EntityMapper.toOrderDTO(order);
    }

    @Transactional
    public OrderDTO cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền hủy đơn hàng này");
        }

        if (!order.getStatus().equals("CHO_XAC_NHAN") && !order.getStatus().equals("DA_XAC_NHAN") && !order.getStatus().equals("DA_NHAN_DON")) {
            throw new BadRequestException("Chỉ được phép hủy đơn hàng khi chưa bắt đầu giao (Chờ xác nhận, Đã xác nhận, Đã nhận đơn)");
        }

        order.setStatus("HUY");
        Order updatedOrder = orderRepository.save(order);

        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null) {
                Inventory inventory = item.getProduct().getInventory();
                if (inventory != null) {
                    inventory.setCurrentStock(inventory.getCurrentStock() + item.getQuantity());
                    inventoryRepository.save(inventory);
                    
                    // Add back to batch (just add to the earliest valid batch, or any batch)
                    List<ProductBatch> batches = productBatchRepository.findByProductIdAndQuantityGreaterThanOrderByExpiryDateAsc(item.getProduct().getId(), -1);
                    if (!batches.isEmpty()) {
                        ProductBatch firstBatch = batches.get(0); // Add back to the closest expiry batch
                        firstBatch.setQuantity(firstBatch.getQuantity() + item.getQuantity());
                        productBatchRepository.save(firstBatch);
                    }
                    
                    ledgerService.recordLog(
                        inventory, 
                        "RETURN", 
                        item.getQuantity(), 
                        order.getId(), 
                        "Hoàn trả do hủy đơn #" + order.getId(), 
                        order.getUser()
                    );
                }
            }
        }

        if (order.getCouponCode() != null) {
            couponRepository.findByCode(order.getCouponCode()).ifPresent(coupon -> {
                if (coupon.getUsedCount() > 0) {
                    coupon.setUsedCount(coupon.getUsedCount() - 1);
                    couponRepository.save(coupon);
                }
            });
        }

        if (order.getPayment() != null) {
            Payment payment = order.getPayment();
            payment.setPaymentStatus("FAILED");
            paymentRepository.save(payment);
        }

        return EntityMapper.toOrderDTO(updatedOrder);
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id: " + orderId));

        String currentStatus = order.getStatus();

        if (currentStatus.equals("HUY") || currentStatus.equals("HOAN_THANH")) {
            throw new BadRequestException("Không thể chuyển đổi trạng thái cho đơn hàng đã Hủy hoặc Hoàn thành");
        }

        boolean isValidTransition = false;
        if (currentStatus.equals("CHO_XAC_NHAN") && (newStatus.equals("DA_XAC_NHAN") || newStatus.equals("HUY"))) {
            isValidTransition = true;
        } else if (currentStatus.equals("DA_XAC_NHAN") && (newStatus.equals("DA_NHAN_DON") || newStatus.equals("HUY"))) {
            isValidTransition = true;
        } else if (currentStatus.equals("DA_NHAN_DON") && (newStatus.equals("DANG_GIAO") || newStatus.equals("HUY"))) {
            isValidTransition = true;
        } else if (currentStatus.equals("DANG_GIAO") && newStatus.equals("DA_GIAO")) {
            isValidTransition = true;
        } else if (currentStatus.equals("DA_GIAO") && newStatus.equals("HOAN_THANH")) {
            isValidTransition = true;
        }

        if (!isValidTransition) {
            throw new BadRequestException("Luồng chuyển trạng thái không hợp lệ: Không được nhảy từ '" + currentStatus + "' sang '" + newStatus + "'");
        }

        order.setStatus(newStatus);

        if (newStatus.equals("HUY")) {
            for (OrderItem item : order.getItems()) {
                if (item.getProduct() != null) {
                    Inventory inventory = item.getProduct().getInventory();
                    if (inventory != null) {
                        inventory.setCurrentStock(inventory.getCurrentStock() + item.getQuantity());
                        inventoryRepository.save(inventory);
                        
                        // Add back to batch (just add to the earliest valid batch)
                        List<ProductBatch> batches = productBatchRepository.findByProductIdAndQuantityGreaterThanOrderByExpiryDateAsc(item.getProduct().getId(), -1);
                        if (!batches.isEmpty()) {
                            ProductBatch firstBatch = batches.get(0);
                            firstBatch.setQuantity(firstBatch.getQuantity() + item.getQuantity());
                            productBatchRepository.save(firstBatch);
                        }
                        
                        ledgerService.recordLog(
                            inventory, 
                            "RETURN", 
                            item.getQuantity(), 
                            order.getId(), 
                            "Hoàn trả do hủy đơn (Admin/Shipper) #" + order.getId(), 
                            null
                        );
                    }
                }
            }
            if (order.getCouponCode() != null) {
                couponRepository.findByCode(order.getCouponCode()).ifPresent(coupon -> {
                    if (coupon.getUsedCount() > 0) {
                        coupon.setUsedCount(coupon.getUsedCount() - 1);
                        couponRepository.save(coupon);
                    }
                });
            }
            if (order.getPayment() != null) {
                Payment payment = order.getPayment();
                payment.setPaymentStatus("FAILED");
                paymentRepository.save(payment);
            }
        }

        if (newStatus.equals("HOAN_THANH")) {
            if (order.getPayment() != null) {
                Payment payment = order.getPayment();
                payment.setPaymentStatus("COMPLETED");
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);
            }
            
            // Add Loyalty Points (1 point per 100,000 VND)
            if (order.getUser() != null && order.getFinalAmount() != null) {
                User user = order.getUser();
                int earnedPoints = order.getFinalAmount().divide(BigDecimal.valueOf(100000)).intValue();
                user.setLoyaltyPoints((user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : 0) + earnedPoints);
                
                // Update tier logic if needed (Bronze -> Silver -> Gold -> Diamond)
                int points = user.getLoyaltyPoints();
                if (points >= 1000) {
                    user.setMembershipTier("DIAMOND");
                } else if (points >= 500) {
                    user.setMembershipTier("GOLD");
                } else if (points >= 100) {
                    user.setMembershipTier("SILVER");
                }
                userRepository.save(user);
            }
        }

        Order updatedOrder = orderRepository.save(order);
        return EntityMapper.toOrderDTO(updatedOrder);
    }

    public List<OrderDTO> getAvailableOrdersForShipper() {
        return orderRepository.findByStatusAndShipperIsNullOrderByCreatedAtAsc("DA_XAC_NHAN").stream()
                .map(EntityMapper::toOrderDTO)
                .collect(Collectors.toList());
    }

    public List<OrderDTO> getMyDeliveries(Long shipperUserId) {
        return orderRepository.findByShipperIdOrderByCreatedAtDesc(shipperUserId).stream()
                .map(EntityMapper::toOrderDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDTO acceptOrder(Long orderId, Long shipperUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng #" + orderId));

        if (!"DA_XAC_NHAN".equals(order.getStatus())) {
            throw new BadRequestException("Chỉ có thể nhận đơn hàng ở trạng thái 'Đã xác nhận'");
        }

        if (order.getShipper() != null) {
            throw new BadRequestException("Đơn hàng này đã được shipper khác nhận giao");
        }

        User shipper = userRepository.findById(shipperUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin shipper"));

        order.setShipper(shipper);
        order.setStatus("DA_NHAN_DON");
        order.setUpdatedAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);
        return EntityMapper.toOrderDTO(saved);
    }

    @Transactional
    public OrderDTO updateDeliveryStatus(Long orderId, Long shipperUserId, String newStatus, String deliveryNote, String deliveryFailedReason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng #" + orderId));

        if (order.getShipper() == null || !order.getShipper().getId().equals(shipperUserId)) {
            // Cho phép Admin can thiệp nếu cần, nhưng đối với shipper phải là chính chủ
            User currentUser = userRepository.findById(shipperUserId).orElse(null);
            boolean isAdmin = currentUser != null && currentUser.getRole() != null && "ROLE_ADMIN".equals(currentUser.getRole().getName());
            if (!isAdmin) {
                throw new BadRequestException("Bạn không được phân công giao đơn hàng này");
            }
        }

        String currentStatus = order.getStatus();

        if ("DANG_GIAO".equals(newStatus)) {
            if (!"DA_NHAN_DON".equals(currentStatus)) {
                throw new BadRequestException("Đơn hàng phải ở trạng thái 'Đã nhận đơn' trước khi bắt đầu giao");
            }
            order.setStatus("DANG_GIAO");
        } else if ("DA_GIAO".equals(newStatus)) {
            if (!"DANG_GIAO".equals(currentStatus)) {
                throw new BadRequestException("Đơn hàng phải ở trạng thái 'Đang giao' trước khi xác nhận đã giao");
            }
            order.setStatus("DA_GIAO");
            order.setDeliveredAt(LocalDateTime.now());
            if (order.getPayment() != null && "COD".equalsIgnoreCase(order.getPaymentMethod())) {
                order.getPayment().setPaymentStatus("COMPLETED");
                order.getPayment().setPaidAt(LocalDateTime.now());
                paymentRepository.save(order.getPayment());
            }
        } else if ("GIAO_THAT_BAI".equals(newStatus)) {
            if (!"DANG_GIAO".equals(currentStatus)) {
                throw new BadRequestException("Chỉ có thể báo giao thất bại khi đang trong quá trình giao");
            }
            order.setStatus("GIAO_THAT_BAI");
            order.setDeliveryFailedReason(deliveryFailedReason != null ? deliveryFailedReason : "Khách hàng không nhận hàng hoặc không liên lạc được");
        } else {
            throw new BadRequestException("Trạng thái giao hàng không hợp lệ: " + newStatus);
        }

        if (deliveryNote != null && !deliveryNote.trim().isEmpty()) {
            order.setDeliveryNote(deliveryNote.trim());
        }
        order.setUpdatedAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);
        return EntityMapper.toOrderDTO(saved);
    }

    @Transactional
    public OrderDTO confirmPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng #" + orderId));

        if (order.getPayment() == null) {
            Payment payment = Payment.builder()
                    .order(order)
                    .paymentMethod(order.getPaymentMethod())
                    .amount(order.getFinalAmount())
                    .paymentStatus("COMPLETED")
                    .paidAt(LocalDateTime.now())
                    .build();
            paymentRepository.save(payment);
            order.setPayment(payment);
        } else {
            Payment payment = order.getPayment();
            payment.setPaymentStatus("COMPLETED");
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);
        }

        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        return EntityMapper.toOrderDTO(saved);
    }
}
