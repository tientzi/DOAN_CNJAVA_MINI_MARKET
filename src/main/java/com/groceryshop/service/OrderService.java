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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    @Autowired
    private AsyncNotificationService asyncNotificationService;

    @Autowired
    private CartService cartService;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private List<CartItem> resolveCheckoutItems(Cart cart, List<Long> selectedProductIds) {
        if (selectedProductIds == null || selectedProductIds.isEmpty()) {
            return new ArrayList<>(cart.getItems());
        }
        Set<Long> selectedSet = new HashSet<>(selectedProductIds);
        List<CartItem> filtered = cart.getItems().stream()
                .filter(ci -> ci.getProduct() != null && selectedSet.contains(ci.getProduct().getId()))
                .collect(Collectors.toList());
        if (filtered.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ít nhất 1 sản phẩm hợp lệ trong giỏ hàng để thanh toán");
        }
        return filtered;
    }

    private void removeCheckedOutItems(Cart cart, List<CartItem> checkedOutItems) {
        Set<Long> checkedIds = checkedOutItems.stream()
                .map(ci -> ci.getProduct().getId())
                .collect(Collectors.toSet());
        cart.getItems().removeIf(ci -> ci.getProduct() != null && checkedIds.contains(ci.getProduct().getId()));
        cartRepository.save(cart);
    }

    @Transactional
    public OrderDTO createOrder(Long userId, OrderDTO orderDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giỏ hàng của người dùng"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng của bạn đang trống");
        }

        List<CartItem> checkoutItems = resolveCheckoutItems(cart, orderDTO.getSelectedProductIds());

        BigDecimal totalAmount = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();

        class ItemAllocation {
            Product product;
            String productName;
            int quantity;
            BigDecimal price;
        }
        List<ItemAllocation> itemAllocations = new java.util.ArrayList<>();

        for (CartItem item : checkoutItems) {
            Product product = item.getProduct();
            if (!product.getIsActive()) {
                throw new BadRequestException("Sản phẩm '" + product.getName() + "' hiện tại không hoạt động");
            }
            Inventory inventory = inventoryRepository.findByProductIdWithLock(product.getId())
                    .orElse(product.getInventory());
            if (inventory != null) {
                entityManager.refresh(inventory, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            }
            int currentStock = inventory != null ? inventory.getCurrentStock() : 0;
            if (item.getQuantity() > currentStock) {
                throw new BadRequestException("Sản phẩm '" + product.getName() + "' không đủ hàng trong kho (Còn lại: " + currentStock + ")");
            }

            int reqQty = item.getQuantity();

            // Tìm lô cận date đang có salePrice và còn hạn
            List<ProductBatch> validBatches = productBatchRepository.findByProductIdAndQuantityGreaterThanOrderByExpiryDateAsc(product.getId(), 0)
                    .stream()
                    .filter(b -> b.getExpiryDate() != null && !b.getExpiryDate().isBefore(today))
                    .collect(Collectors.toList());

            ProductBatch saleBatch = validBatches.stream()
                    .filter(b -> b.getSalePrice() != null && b.getSalePrice().compareTo(BigDecimal.ZERO) > 0 
                            && product.getPrice() != null && b.getSalePrice().compareTo(product.getPrice()) < 0)
                    .findFirst()
                    .orElse(null);

            if (saleBatch != null && saleBatch.getQuantity() > 0 && reqQty > saleBatch.getQuantity()) {
                // Tách 2 dòng:
                // 1. Phần lô sale
                int saleQty = saleBatch.getQuantity();
                BigDecimal salePrice = saleBatch.getSalePrice();
                ItemAllocation a1 = new ItemAllocation();
                a1.product = product;
                a1.productName = product.getName() + " (Xả kho cận date - HSD: " + saleBatch.getExpiryDate() + ")";
                a1.quantity = saleQty;
                a1.price = salePrice;
                itemAllocations.add(a1);
                totalAmount = totalAmount.add(salePrice.multiply(BigDecimal.valueOf(saleQty)));

                // 2. Phần lô tiêu chuẩn
                int normalQty = reqQty - saleQty;
                BigDecimal normalPrice = product.getPrice();
                ItemAllocation a2 = new ItemAllocation();
                a2.product = product;
                a2.productName = product.getName() + " (Lô tiêu chuẩn)";
                a2.quantity = normalQty;
                a2.price = normalPrice;
                itemAllocations.add(a2);
                totalAmount = totalAmount.add(normalPrice.multiply(BigDecimal.valueOf(normalQty)));
            } else {
                BigDecimal itemPrice = (saleBatch != null && reqQty <= saleBatch.getQuantity())
                        ? saleBatch.getSalePrice()
                        : (product.getSalePrice() != null ? product.getSalePrice() : product.getPrice());

                String displayName = product.getName();
                if (saleBatch != null && reqQty <= saleBatch.getQuantity()) {
                    displayName += " (Xả kho cận date - HSD: " + saleBatch.getExpiryDate() + ")";
                }

                ItemAllocation a = new ItemAllocation();
                a.product = product;
                a.productName = displayName;
                a.quantity = reqQty;
                a.price = itemPrice;
                itemAllocations.add(a);
                totalAmount = totalAmount.add(itemPrice.multiply(BigDecimal.valueOf(reqQty)));
            }
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        Coupon appliedCoupon = null;
        if (orderDTO.getCouponCode() != null && !orderDTO.getCouponCode().trim().isEmpty()) {
            List<com.groceryshop.dto.CouponItemInfo> itemInfos = checkoutItems.stream()
                    .map(item -> com.groceryshop.dto.CouponItemInfo.builder()
                            .productId(item.getProduct().getId())
                            .categoryId(item.getProduct().getCategory() != null ? item.getProduct().getCategory().getId() : null)
                            .price(item.getProduct().getSalePrice() != null ? item.getProduct().getSalePrice() : item.getProduct().getPrice())
                            .quantity(item.getQuantity())
                            .build())
                    .collect(Collectors.toList());
            discountAmount = couponService.calculateDiscountForItems(orderDTO.getCouponCode(), totalAmount, itemInfos);
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

        for (ItemAllocation alloc : itemAllocations) {
            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .product(alloc.product)
                    .quantity(alloc.quantity)
                    .price(alloc.price)
                    .productName(alloc.productName)
                    .productImage(alloc.product.getMainImage())
                    .build();
            orderItemRepository.save(orderItem);
            savedOrder.getItems().add(orderItem);
        }

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            Inventory inventory = inventoryRepository.findByProductIdWithLock(product.getId())
                    .orElse(product.getInventory());
            if (inventory != null) {
                inventory.setCurrentStock(inventory.getCurrentStock() - item.getQuantity());
                inventoryRepository.save(inventory);
                
                // Deduct from batches (FIFO, strictly ignoring expired batches)
                int quantityToDeduct = item.getQuantity();
                List<ProductBatch> batches = productBatchRepository.findByProductIdAndQuantityGreaterThanOrderByExpiryDateAsc(product.getId(), 0);
                for (ProductBatch batch : batches) {
                    if (quantityToDeduct <= 0) break;
                    if (batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(today)) {
                        continue; // Không trừ vào lô đã hết hạn khi bán lẻ
                    }
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

        removeCheckedOutItems(cart, checkoutItems);

        // Kích hoạt tác vụ bất đồng bộ gửi thông báo & audit log đơn hàng
        asyncNotificationService.sendOrderCreatedNotification(
                savedOrder.getId(),
                savedOrder.getShippingName() != null ? savedOrder.getShippingName() : (user != null ? user.getFullName() : "Khách hàng"),
                savedOrder.getFinalAmount()
        );

        return EntityMapper.toOrderDTO(savedOrder);
    }

    /**
     * Bước 1 của luồng Hàng đợi: Tạo đơn hàng ở trạng thái CHO_TIEP_NHAN (chưa trừ tồn kho),
     * xóa giỏ hàng để Worker nền kiểm tra và trừ kho bất đồng bộ.
     */
    @Transactional
    public OrderDTO submitOrderToQueue(Long userId, OrderDTO orderDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giỏ hàng của người dùng"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng của bạn đang trống");
        }

        List<CartItem> checkoutItems = resolveCheckoutItems(cart, orderDTO.getSelectedProductIds());

        BigDecimal totalAmount = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();

        class QueuedAllocation {
            Product product;
            String productName;
            int quantity;
            BigDecimal price;
        }
        List<QueuedAllocation> allocations = new java.util.ArrayList<>();

        for (CartItem item : checkoutItems) {
            Product product = item.getProduct();
            if (!Boolean.TRUE.equals(product.getIsActive())) {
                throw new BadRequestException("Sản phẩm '" + product.getName() + "' hiện tại không hoạt động");
            }
            int reqQty = item.getQuantity();

            List<ProductBatch> validBatches = productBatchRepository.findByProductIdAndQuantityGreaterThanOrderByExpiryDateAsc(product.getId(), 0)
                    .stream()
                    .filter(b -> b.getExpiryDate() != null && !b.getExpiryDate().isBefore(today))
                    .collect(Collectors.toList());

            ProductBatch saleBatch = validBatches.stream()
                    .filter(b -> b.getSalePrice() != null && b.getSalePrice().compareTo(BigDecimal.ZERO) > 0
                            && product.getPrice() != null && b.getSalePrice().compareTo(product.getPrice()) < 0)
                    .findFirst()
                    .orElse(null);

            if (saleBatch != null && saleBatch.getQuantity() > 0 && reqQty > saleBatch.getQuantity()) {
                int saleQty = saleBatch.getQuantity();
                BigDecimal salePrice = saleBatch.getSalePrice();
                QueuedAllocation a1 = new QueuedAllocation();
                a1.product = product;
                a1.productName = product.getName() + " (Xả kho cận date - HSD: " + saleBatch.getExpiryDate() + ")";
                a1.quantity = saleQty;
                a1.price = salePrice;
                allocations.add(a1);
                totalAmount = totalAmount.add(salePrice.multiply(BigDecimal.valueOf(saleQty)));

                int normalQty = reqQty - saleQty;
                BigDecimal normalPrice = product.getPrice();
                QueuedAllocation a2 = new QueuedAllocation();
                a2.product = product;
                a2.productName = product.getName() + " (Lô tiêu chuẩn)";
                a2.quantity = normalQty;
                a2.price = normalPrice;
                allocations.add(a2);
                totalAmount = totalAmount.add(normalPrice.multiply(BigDecimal.valueOf(normalQty)));
            } else {
                BigDecimal itemPrice = (saleBatch != null && reqQty <= saleBatch.getQuantity())
                        ? saleBatch.getSalePrice()
                        : (product.getSalePrice() != null ? product.getSalePrice() : product.getPrice());

                String displayName = product.getName();
                if (saleBatch != null && reqQty <= saleBatch.getQuantity()) {
                    displayName += " (Xả kho cận date - HSD: " + saleBatch.getExpiryDate() + ")";
                }

                QueuedAllocation a = new QueuedAllocation();
                a.product = product;
                a.productName = displayName;
                a.quantity = reqQty;
                a.price = itemPrice;
                allocations.add(a);
                totalAmount = totalAmount.add(itemPrice.multiply(BigDecimal.valueOf(reqQty)));
            }
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (orderDTO.getCouponCode() != null && !orderDTO.getCouponCode().trim().isEmpty()) {
            List<com.groceryshop.dto.CouponItemInfo> itemInfos = checkoutItems.stream()
                    .map(item -> com.groceryshop.dto.CouponItemInfo.builder()
                            .productId(item.getProduct().getId())
                            .categoryId(item.getProduct().getCategory() != null ? item.getProduct().getCategory().getId() : null)
                            .price(item.getProduct().getSalePrice() != null ? item.getProduct().getSalePrice() : item.getProduct().getPrice())
                            .quantity(item.getQuantity())
                            .build())
                    .collect(Collectors.toList());
            discountAmount = couponService.calculateDiscountForItems(orderDTO.getCouponCode(), totalAmount, itemInfos);
        }

        BigDecimal tierDiscount = BigDecimal.ZERO;
        if (user.getMembershipTier() != null) {
            if ("SILVER".equals(user.getMembershipTier())) {
                tierDiscount = totalAmount.multiply(BigDecimal.valueOf(0.02));
            } else if ("GOLD".equals(user.getMembershipTier())) {
                tierDiscount = totalAmount.multiply(BigDecimal.valueOf(0.05));
            } else if ("DIAMOND".equals(user.getMembershipTier())) {
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
                .status("CHO_TIEP_NHAN")
                .shippingName(orderDTO.getShippingName())
                .shippingPhone(orderDTO.getShippingPhone())
                .shippingAddress(orderDTO.getShippingAddress())
                .paymentMethod(pm)
                .couponCode(orderDTO.getCouponCode())
                .note(orderDTO.getNote())
                .build();

        Order savedOrder = orderRepository.save(order);

        for (QueuedAllocation alloc : allocations) {
            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .product(alloc.product)
                    .quantity(alloc.quantity)
                    .price(alloc.price)
                    .productName(alloc.productName)
                    .productImage(alloc.product.getMainImage())
                    .build();
            orderItemRepository.save(orderItem);
            savedOrder.getItems().add(orderItem);
        }

        String initialPayStatus = "PENDING";
        if (!"COD".equalsIgnoreCase(pm) && !"TIEN_MAT".equalsIgnoreCase(pm)
                && "UNPAID".equalsIgnoreCase(orderDTO.getPaymentStatus())) {
            initialPayStatus = "UNPAID";
        }

        Payment payment = Payment.builder()
                .order(savedOrder)
                .paymentMethod(pm)
                .paymentStatus(initialPayStatus)
                .amount(finalAmount)
                .build();
        paymentRepository.save(payment);
        savedOrder.setPayment(payment);

        removeCheckedOutItems(cart, checkoutItems);

        OrderDTO dto = EntityMapper.toOrderDTO(savedOrder);
        dto.setMembershipTier(user.getMembershipTier());
        return dto;
    }

    /**
     * Bước 2 (Worker nền / Retry): Khóa bi quan JPA trên SQL Server để kiểm tra và trừ kho thật.
     */
    @Transactional
    public OrderDTO processQueuedOrderStock(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng #" + orderId));

        if (!"CHO_TIEP_NHAN".equals(order.getStatus()) && !"TU_CHOI".equals(order.getStatus())) {
            throw new BadRequestException("Đơn hàng #" + orderId + " đang ở trạng thái '" + order.getStatus() + "', không thể xử lý lại trong hàng đợi");
        }

        LocalDate today = LocalDate.now();
        java.util.Map<Long, Integer> qtyByProduct = new java.util.LinkedHashMap<>();
        java.util.Map<Long, Product> productMap = new java.util.HashMap<>();

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product != null) {
                qtyByProduct.merge(product.getId(), item.getQuantity(), Integer::sum);
                productMap.putIfAbsent(product.getId(), product);
            }
        }

        for (java.util.Map.Entry<Long, Integer> entry : qtyByProduct.entrySet()) {
            Long productId = entry.getKey();
            int reqQty = entry.getValue();
            Product product = productMap.get(productId);

            if (!Boolean.TRUE.equals(product.getIsActive())) {
                throw new BadRequestException("Sản phẩm '" + product.getName() + "' hiện không hoạt động");
            }

            Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                    .orElse(product.getInventory());
            if (inventory != null) {
                entityManager.refresh(inventory, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            }
            int currentStock = inventory != null ? inventory.getCurrentStock() : 0;
            if (reqQty > currentStock) {
                throw new BadRequestException("Thiếu tồn kho sản phẩm '" + product.getName() + "' (Cần " + reqQty + ", còn lại: " + currentStock + ")");
            }
        }

        // Trừ tồn kho, lô hàng và ghi sổ cái
        for (java.util.Map.Entry<Long, Integer> entry : qtyByProduct.entrySet()) {
            Long productId = entry.getKey();
            int reqQty = entry.getValue();
            Product product = productMap.get(productId);

            Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                    .orElse(product.getInventory());
            if (inventory != null) {
                inventory.setCurrentStock(inventory.getCurrentStock() - reqQty);
                inventoryRepository.save(inventory);

                int quantityToDeduct = reqQty;
                List<ProductBatch> batches = productBatchRepository.findByProductIdAndQuantityGreaterThanOrderByExpiryDateAsc(productId, 0);
                for (ProductBatch batch : batches) {
                    if (quantityToDeduct <= 0) break;
                    if (batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(today)) {
                        continue;
                    }
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
                        -reqQty,
                        order.getId(),
                        "Xuất kho qua Worker hàng đợi cho đơn #" + order.getId(),
                        order.getUser()
                );
            }
        }

        if (order.getCouponCode() != null && !order.getCouponCode().trim().isEmpty()) {
            couponRepository.findByCode(order.getCouponCode()).ifPresent(coupon -> {
                coupon.setUsedCount((coupon.getUsedCount() != null ? coupon.getUsedCount() : 0) + 1);
                couponRepository.save(coupon);
            });
        }

        order.setStatus("CHO_XAC_NHAN");
        if (order.getDeliveryNote() != null && order.getDeliveryNote().startsWith("[Worker từ chối]")) {
            order.setDeliveryNote(null);
        }
        order.setUpdatedAt(LocalDateTime.now());
        if (order.getPayment() != null && ("FAILED".equals(order.getPayment().getPaymentStatus()))) {
            order.getPayment().setPaymentStatus("PENDING");
            paymentRepository.save(order.getPayment());
        }

        Order saved = orderRepository.save(order);

        asyncNotificationService.sendOrderCreatedNotification(
                saved.getId(),
                saved.getShippingName() != null ? saved.getShippingName() : "Khách hàng",
                saved.getFinalAmount()
        );

        return EntityMapper.toOrderDTO(saved);
    }

    @Transactional
    public void markQueuedOrderFailed(Long orderId, String reason) {
        if (orderId == null) return;
        orderRepository.findById(orderId).ifPresent(order -> {
            if ("CHO_TIEP_NHAN".equals(order.getStatus()) || "TU_CHOI".equals(order.getStatus())) {
                order.setStatus("TU_CHOI");
                order.setDeliveryNote("[Worker từ chối]: " + (reason != null ? reason : "Không đủ tồn kho"));
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);

                if (order.getPayment() != null) {
                    order.getPayment().setPaymentStatus("FAILED");
                    paymentRepository.save(order.getPayment());
                }
            }
        });
    }

    /**
     * Hủy đơn thanh toán Online quá hạn 30 giây mà Admin chưa duyệt (Thanh toán không thành công)
     * và tự động hoàn trả tồn kho, lô hàng, lượt dùng mã giảm giá.
     */
    @Transactional
    public boolean expireUnapprovedOnlineOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || !"CHO_XAC_NHAN".equals(order.getStatus())) {
            return false;
        }
        String pm = order.getPaymentMethod();
        if (pm == null || "COD".equalsIgnoreCase(pm) || "TIEN_MAT".equalsIgnoreCase(pm)) {
            return false;
        }
        Payment payment = order.getPayment();
        String pStatus = payment != null ? payment.getPaymentStatus() : "PENDING";
        if (!"PENDING".equals(pStatus) && !"UNPAID".equals(pStatus)) {
            return false;
        }

        order.setStatus("HUY");
        order.setDeliveryNote("[Hệ thống]: Thanh toán không thành công (Quá hạn 30s chưa duyệt giao dịch)");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        if (payment != null) {
            payment.setPaymentStatus("FAILED");
            paymentRepository.save(payment);
        }

        restoreInventoryAndCoupon(order, null, "Hoàn trả kho do thanh toán không thành công (quá hạn 30s) - Đơn #" + order.getId());
        return true;
    }

    /**
     * Khách hàng bấm "Thanh toán lại" từ Lịch sử mua hàng cho đơn đang ở trạng thái UNPAID.
     * Chuyển paymentStatus -> PENDING và cập nhật updatedAt để bắt đầu cửa sổ duyệt 30s phía Admin.
     */
    @Transactional
    public OrderDTO retryPayment(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng #" + orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền thao tác trên đơn hàng này");
        }
        if (!"CHO_XAC_NHAN".equals(order.getStatus()) && !"CHO_TIEP_NHAN".equals(order.getStatus())) {
            throw new BadRequestException("Chỉ có thể thanh toán lại cho đơn hàng đang chờ xử lý");
        }
        Payment payment = order.getPayment();
        if (payment == null || !"UNPAID".equals(payment.getPaymentStatus())) {
            throw new BadRequestException("Đơn hàng này không ở trạng thái Chưa thanh toán");
        }

        payment.setPaymentStatus("PENDING");
        paymentRepository.save(payment);

        if (order.getNote() != null && order.getNote().contains("Chưa xác nhận chuyển khoản")) {
            order.setNote(order.getNote().replace(" | Chưa xác nhận chuyển khoản (quá 10s)", "").replace("Chưa xác nhận chuyển khoản (quá 10s)", "").trim());
        }
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        return EntityMapper.toOrderDTO(saved);
    }

    /**
     * Khách hàng bấm "Đặt hàng lại" từ Lịch sử mua hàng:
     * Đưa toàn bộ sản phẩm của đơn hàng vào Giỏ hàng. Nếu đơn cũ đang là UNPAID (chưa thanh toán),
     * tự động hủy đơn cũ và hoàn kho để tránh giữ kho trùng lặp.
     */
    @Transactional
    public boolean reorderToCart(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng #" + orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền đặt lại đơn hàng này");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new BadRequestException("Đơn hàng không có sản phẩm nào để đặt lại");
        }

        // Gom số lượng theo từng productId (tránh nhân đôi nếu trước đó tách 2 dòng lô sale + lô chuẩn)
        java.util.Map<Long, Integer> qtyByProduct = new java.util.LinkedHashMap<>();
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null) {
                qtyByProduct.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
            }
        }

        boolean cancelledOldUnpaid = false;
        // Nếu đơn đang chưa thanh toán (UNPAID) và chưa giao -> hủy đơn cũ để hoàn kho
        Payment payment = order.getPayment();
        if (payment != null && "UNPAID".equals(payment.getPaymentStatus())
                && ("CHO_XAC_NHAN".equals(order.getStatus()) || "CHO_TIEP_NHAN".equals(order.getStatus()))) {
            boolean stockWasHeld = "CHO_XAC_NHAN".equals(order.getStatus());
            order.setStatus("HUY");
            order.setDeliveryNote("[Hệ thống]: Khách chọn đặt hàng lại đơn #" + order.getId());
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            payment.setPaymentStatus("FAILED");
            paymentRepository.save(payment);

            if (stockWasHeld) {
                restoreInventoryAndCoupon(order, order.getUser(), "Khách đặt hàng lại từ đơn chưa thanh toán #" + order.getId());
            }
            cancelledOldUnpaid = true;
        }

        for (java.util.Map.Entry<Long, Integer> entry : qtyByProduct.entrySet()) {
            cartService.addToCart(userId, entry.getKey(), entry.getValue());
        }
        return cancelledOldUnpaid;
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

        if (!order.getStatus().equals("CHO_TIEP_NHAN") && !order.getStatus().equals("CHO_XAC_NHAN")
                && !order.getStatus().equals("DA_XAC_NHAN") && !order.getStatus().equals("DA_NHAN_DON")) {
            throw new BadRequestException("Chỉ được phép hủy đơn hàng khi chưa bắt đầu giao (Chờ xử lý, Chờ xác nhận, Đã xác nhận, Đã nhận đơn)");
        }

        boolean stockAlreadyDeducted = !order.getStatus().equals("CHO_TIEP_NHAN");
        order.setStatus("HUY");
        Order updatedOrder = orderRepository.save(order);

        if (stockAlreadyDeducted) {
            restoreInventoryAndCoupon(order, order.getUser(), "Hoàn trả do khách hàng hủy đơn #" + order.getId());
        }

        if (order.getPayment() != null) {
            Payment payment = order.getPayment();
            payment.setPaymentStatus("FAILED");
            paymentRepository.save(payment);
        }

        return EntityMapper.toOrderDTO(updatedOrder);
    }

    @Transactional
    public void restoreInventoryAndCoupon(Order order, User performer, String reason) {
        if (order == null || order.getItems() == null) return;

        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null) {
                Inventory inventory = item.getProduct().getInventory();
                if (inventory != null) {
                    inventory.setCurrentStock(inventory.getCurrentStock() + item.getQuantity());
                    inventoryRepository.save(inventory);
                    
                    // Add back to batch (add to the earliest valid batch)
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
                        (reason != null && !reason.trim().isEmpty()) ? reason : ("Hoàn trả do hủy đơn #" + order.getId()), 
                        performer
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

        if (newStatus.equals("DA_XAC_NHAN")) {
            Payment payment = order.getPayment();
            boolean isCod = "TIEN_MAT".equalsIgnoreCase(order.getPaymentMethod()) || "COD".equalsIgnoreCase(order.getPaymentMethod());
            if (payment == null) {
                payment = Payment.builder()
                        .order(order)
                        .paymentMethod(order.getPaymentMethod())
                        .amount(order.getFinalAmount())
                        .paymentStatus(isCod ? "APPROVED_COD" : "COMPLETED")
                        .paidAt(isCod ? null : LocalDateTime.now())
                        .build();
            } else {
                payment.setPaymentStatus(isCod ? "APPROVED_COD" : "COMPLETED");
                if (!isCod && payment.getPaidAt() == null) {
                    payment.setPaidAt(LocalDateTime.now());
                }
            }
            paymentRepository.save(payment);
            order.setPayment(payment);
        }

        if (newStatus.equals("HUY")) {
            restoreInventoryAndCoupon(order, null, "Hoàn trả do hủy đơn (Admin/Shipper) #" + order.getId());
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

        if ("CHO_XAC_NHAN".equals(order.getStatus())) {
            order.setStatus("DA_XAC_NHAN");
        }
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        return EntityMapper.toOrderDTO(saved);
    }
}
