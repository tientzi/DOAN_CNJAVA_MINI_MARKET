package com.groceryshop.mvc.controller;

import com.groceryshop.dto.OrderDTO;
import com.groceryshop.exception.BadRequestException;
import com.groceryshop.mvc.exception.MvcExceptionHandler;
import com.groceryshop.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping
    public String listOrders(@RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String status,
                             @RequestParam(defaultValue = "newest") String sortBy,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "8") int size,
                             Model model) {
        List<OrderDTO> filtered = orderService.getAllOrders().stream()
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

        model.addAttribute("pageTitle", "Quản lý & Điều phối Đơn hàng");
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
}

