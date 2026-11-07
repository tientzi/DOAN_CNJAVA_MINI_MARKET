package com.groceryshop.service;

import com.groceryshop.dto.OrderDTO;
import com.groceryshop.entity.Order;
import com.groceryshop.entity.User;
import com.groceryshop.exception.BadRequestException;
import com.groceryshop.exception.ResourceNotFoundException;
import com.groceryshop.mapper.EntityMapper;
import com.groceryshop.repository.OrderRepository;
import com.groceryshop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeliveryService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Map<String, Object>> getAvailableShippers() {
        List<User> shippers = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && "ROLE_SHIPPER".equals(u.getRole().getName()))
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .sorted(Comparator.comparing(User::getId))
                .collect(Collectors.toList());

        List<Order> activeOrders = orderRepository.findAll().stream()
                .filter(o -> o.getShipper() != null && ("DA_NHAN_DON".equals(o.getStatus()) || "DANG_GIAO".equals(o.getStatus())))
                .collect(Collectors.toList());

        Map<Long, Long> shipperActiveCount = activeOrders.stream()
                .collect(Collectors.groupingBy(o -> o.getShipper().getId(), Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < shippers.size(); i++) {
            User s = shippers.get(i);
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("username", s.getUsername());
            map.put("fullName", s.getFullName() != null ? s.getFullName() : s.getUsername());
            map.put("phone", s.getPhone());

            String assignedDistrict;
            if (s.getUsername().toLowerCase().contains("shipper1") || i == 0) {
                assignedDistrict = "Quận Tân Phú";
            } else if (s.getUsername().toLowerCase().contains("shipper2") || i == 1) {
                assignedDistrict = "Quận Tân Bình";
            } else {
                assignedDistrict = "Quận 12";
            }
            map.put("assignedDistrict", assignedDistrict);
            long count = shipperActiveCount.getOrDefault(s.getId(), 0L);
            map.put("activeOrdersCount", count);
            map.put("activeDeliveries", count);
            result.add(map);
        }

        return result;
    }

    public List<OrderDTO> getUnassignedOrders() {
        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> o.getShipper() == null && "DA_XAC_NHAN".equals(o.getStatus()))
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        return orders.stream().map(EntityMapper::toOrderDTO).collect(Collectors.toList());
    }

    public List<OrderDTO> getTrackingOrders() {
        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> List.of("DA_NHAN_DON", "DANG_GIAO", "GIAO_THAT_BAI").contains(o.getStatus()))
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        return orders.stream().map(EntityMapper::toOrderDTO).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getDeliveryOrders(String district) {
        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> List.of("DA_XAC_NHAN", "DA_NHAN_DON", "DANG_GIAO", "GIAO_THAT_BAI").contains(o.getStatus()))
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Order o : orders) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", o.getId());
            map.put("shippingName", o.getShippingName());
            map.put("shippingPhone", o.getShippingPhone());
            map.put("shippingAddress", o.getShippingAddress());
            map.put("extractedDistrict", EntityMapper.detectDistrict(o.getShippingAddress()));
            map.put("detectedDistrict", EntityMapper.detectDistrict(o.getShippingAddress()));
            map.put("totalAmount", o.getTotalAmount());
            map.put("finalAmount", o.getFinalAmount());
            map.put("paymentMethod", o.getPaymentMethod());
            map.put("status", o.getStatus());
            map.put("shipperId", o.getShipper() != null ? o.getShipper().getId() : null);
            map.put("shipperName", o.getShipper() != null ? (o.getShipper().getFullName() != null ? o.getShipper().getFullName() : o.getShipper().getUsername()) : null);
            map.put("shipperPhone", o.getShipper() != null ? o.getShipper().getPhone() : null);
            map.put("deliveryNote", o.getDeliveryNote());
            map.put("deliveryFailedReason", o.getDeliveryFailedReason());
            map.put("createdAt", o.getCreatedAt());
            map.put("itemsCount", o.getItems() != null ? o.getItems().size() : 0);

            if (district == null || district.trim().isEmpty() || "ALL".equalsIgnoreCase(district) ||
                    ((String) map.get("extractedDistrict")).toLowerCase().contains(district.toLowerCase())) {
                result.add(map);
            }
        }

        return result;
    }

    @Transactional
    public Map<String, Object> assignOrdersToShipper(List<Long> orderIds, Long shipperId) {
        User shipper = userRepository.findById(shipperId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Shipper #" + shipperId));

        if (shipper.getRole() == null || !"ROLE_SHIPPER".equals(shipper.getRole().getName())) {
            throw new BadRequestException("Người dùng này không có vai trò Shipper");
        }

        if (orderIds == null || orderIds.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ít nhất 1 đơn hàng để phân công");
        }

        int count = 0;
        for (Long oId : orderIds) {
            Order order = orderRepository.findById(oId).orElse(null);
            if (order != null) {
                order.setShipper(shipper);
                if ("DA_XAC_NHAN".equals(order.getStatus()) || "CHO_XAC_NHAN".equals(order.getStatus()) || "GIAO_THAT_BAI".equals(order.getStatus())) {
                    order.setStatus("DA_NHAN_DON");
                }
                orderRepository.save(order);
                count++;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("assignedCount", count);
        response.put("shipperName", shipper.getFullName() != null ? shipper.getFullName() : shipper.getUsername());
        return response;
    }

    @Transactional
    public Map<String, Object> autoDispatchByArea(String district) {
        List<User> shippers = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && "ROLE_SHIPPER".equals(u.getRole().getName()))
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .sorted(Comparator.comparing(User::getId))
                .collect(Collectors.toList());

        if (shippers.isEmpty()) {
            throw new BadRequestException("Không có Shipper nào đang hoạt động trong hệ thống");
        }

        List<Order> unassignedOrders = orderRepository.findAll().stream()
                .filter(o -> o.getShipper() == null && ("DA_XAC_NHAN".equals(o.getStatus()) || "CHO_XAC_NHAN".equals(o.getStatus())))
                .filter(o -> district == null || "ALL".equalsIgnoreCase(district) || EntityMapper.detectDistrict(o.getShippingAddress()).equalsIgnoreCase(district))
                .collect(Collectors.toList());

        if (unassignedOrders.isEmpty()) {
            return Map.of(
                    "success", true,
                    "message", "Không có đơn hàng nào cần phân bổ trong khu vực này",
                    "dispatchedCount", 0
            );
        }

        Map<String, User> districtShippers = new HashMap<>();
        for (int i = 0; i < shippers.size(); i++) {
            User s = shippers.get(i);
            if (i == 0 || s.getUsername().contains("1")) districtShippers.put("Quận Tân Phú", s);
            else if (i == 1 || s.getUsername().contains("2")) districtShippers.put("Quận Tân Bình", s);
            else districtShippers.put("Quận 12", s);
        }

        int count = 0;
        int fallbackIndex = 0;
        for (Order order : unassignedOrders) {
            String detected = EntityMapper.detectDistrict(order.getShippingAddress());
            User assignedShipper = districtShippers.get(detected);
            if (assignedShipper == null) {
                assignedShipper = shippers.get(fallbackIndex % shippers.size());
                fallbackIndex++;
            }
            order.setShipper(assignedShipper);
            order.setStatus("DA_NHAN_DON");
            orderRepository.save(order);
            count++;
        }

        return Map.of(
                "success", true,
                "dispatchedCount", count,
                "message", "Đã phân bổ nhanh " + count + " đơn hàng cho " + shippers.size() + " shipper theo khu vực thành công!"
        );
    }

    @Transactional
    public Map<String, Object> reassignOrder(Long orderId, Long newShipperId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng #" + orderId));

        User newShipper = userRepository.findById(newShipperId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Shipper #" + newShipperId));

        order.setShipper(newShipper);
        order.setStatus("DA_NHAN_DON");
        order.setDeliveryFailedReason(null);
        orderRepository.save(order);

        return Map.of(
                "success", true,
                "message", "Đã điều phối lại đơn hàng #" + orderId + " sang shipper " + (newShipper.getFullName() != null ? newShipper.getFullName() : newShipper.getUsername())
        );
    }
}
