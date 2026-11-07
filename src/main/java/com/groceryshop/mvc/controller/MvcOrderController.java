package com.groceryshop.mvc.controller;

import com.groceryshop.dto.OrderDTO;
import com.groceryshop.entity.User;
import com.groceryshop.exception.BadRequestException;
import com.groceryshop.mvc.exception.MvcExceptionHandler;
import com.groceryshop.repository.UserRepository;
import com.groceryshop.security.UserPrincipal;
import com.groceryshop.service.DeliveryService;
import com.groceryshop.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/mvc/orders")
public class MvcOrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public String listOrders(@RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String status,
                             @RequestParam(defaultValue = "newest") String sortBy,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "8") int size,
                             Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentRole = "ROLE_ADMIN";
        Long currentUserId = null;

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            currentRole = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("ROLE_USER");
            currentUserId = getUserIdFromAuth(auth);
        }

        List<OrderDTO> baseList;
        if ("ROLE_USER".equals(currentRole) && currentUserId != null) {
            // Khách hàng thường chỉ xem đơn hàng của chính mình (đồng bộ với React OrderHistory)
            baseList = orderService.getMyOrders(currentUserId);
        } else if ("ROLE_SHIPPER".equals(currentRole) && currentUserId != null) {
            // Shipper xem các đơn được giao cho mình
            baseList = orderService.getMyDeliveries(currentUserId);
        } else {
            // Admin xem toàn bộ đơn hàng
            baseList = orderService.getAllOrders();
        }

        List<OrderDTO> filtered = baseList.stream()
                .filter(o -> {
                    if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase(o.getStatus())) {
                        return false;
                    }
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String kw = keyword.trim().toLowerCase();
                        boolean idMatch = String.valueOf(o.getId()).contains(kw);
                        boolean nameMatch = o.getShippingName() != null && o.getShippingName().toLowerCase().contains(kw);
                        boolean phoneMatch = o.getShippingPhone() != null && o.getShippingPhone().toLowerCase().contains(kw);
                        boolean addrMatch = o.getShippingAddress() != null && o.getShippingAddress().toLowerCase().contains(kw);
                        return idMatch || nameMatch || phoneMatch || addrMatch;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        if ("amount_desc".equals(sortBy)) {
            filtered.sort(Comparator.comparing((OrderDTO o) -> o.getFinalAmount() != null ? o.getFinalAmount() : BigDecimal.ZERO).reversed());
        } else if ("amount_asc".equals(sortBy)) {
            filtered.sort(Comparator.comparing(o -> o.getFinalAmount() != null ? o.getFinalAmount() : BigDecimal.ZERO));
        } else if ("oldest".equals(sortBy)) {
            filtered.sort(Comparator.comparing(OrderDTO::getId));
        } else {
            filtered.sort(Comparator.comparing(OrderDTO::getId).reversed());
        }

        int totalItems = filtered.size();
        int pageSize = Math.max(size, 1);
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        int currentPage = Math.max(0, Math.min(page, Math.max(totalPages - 1, 0)));
        int fromIndex = Math.min(currentPage * pageSize, totalItems);
        int toIndex = Math.min(fromIndex + pageSize, totalItems);
        List<OrderDTO> pagedList = filtered.subList(fromIndex, toIndex);

        model.addAttribute("pageTitle", "ROLE_SHIPPER".equals(currentRole) ? "Bảng Giao Hàng Shipper" : "Quản lý & Điều phối Đơn hàng");
        model.addAttribute("orders", pagedList);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("status", status != null ? status : "");
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("pageSize", pageSize);
        return "mvc/orders";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        OrderDTO order = orderService.getOrderById(id);
        model.addAttribute("pageTitle", "Chi tiết Đơn hàng #" + id);
        model.addAttribute("order", order);
        model.addAttribute("shippers", deliveryService.getAvailableShippers());
        return "mvc/order-detail";
    }

    @PostMapping("/status/{id}")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String newStatus,
                               @RequestParam(name = "_mvcCsrf", required = false) String csrfToken,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        MvcExceptionHandler.verifyCsrf(session, csrfToken);
        try {
            OrderDTO updated = orderService.updateOrderStatus(id, newStatus);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã cập nhật trạng thái Đơn hàng #" + id + " sang '" + updated.getStatus()
                            + "' thành công (Đầy đủ Transaction: cập nhật thanh toán / hoàn kho / cộng điểm VIP).");
        } catch (BadRequestException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/mvc/orders/" + id;
    }

    /**
     * Admin phân công Shipper trực tiếp cho đơn hàng (gọi DeliveryService)
     */
    @PostMapping("/assign-shipper/{id}")
    public String assignShipper(@PathVariable Long id,
                                @RequestParam Long shipperId,
                                @RequestParam(name = "_mvcCsrf", required = false) String csrfToken,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        MvcExceptionHandler.verifyCsrf(session, csrfToken);
        try {
            deliveryService.assignOrdersToShipper(List.of(id), shipperId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã phân công Shipper thành công cho đơn hàng #" + id);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi phân công: " + ex.getMessage());
        }
        return "redirect:/mvc/orders/" + id;
    }

    /**
     * Shipper cập nhật trạng thái giao hàng (Đang giao / Đã giao / Giao thất bại)
     */
    @PostMapping("/delivery-status/{id}")
    public String updateDeliveryStatus(@PathVariable Long id,
                                       @RequestParam String newStatus,
                                       @RequestParam(required = false) String deliveryNote,
                                       @RequestParam(required = false) String deliveryFailedReason,
                                       @RequestParam(name = "_mvcCsrf", required = false) String csrfToken,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        MvcExceptionHandler.verifyCsrf(session, csrfToken);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long shipperUserId = getUserIdFromAuth(auth);

        try {
            orderService.updateDeliveryStatus(id, shipperUserId, newStatus, deliveryNote, deliveryFailedReason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã cập nhật tiến độ giao hàng cho đơn #" + id + " sang: " + newStatus);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/mvc/orders/" + id;
    }

    private Long getUserIdFromAuth(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return 1L;
        if (auth.getPrincipal() instanceof UserPrincipal up) {
            return up.getId();
        }
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        return user != null ? user.getId() : 1L;
    }
}