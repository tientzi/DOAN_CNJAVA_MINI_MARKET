package com.groceryshop.mvc.controller;

import com.groceryshop.dto.DashboardStatsDTO;
import com.groceryshop.dto.OrderDTO;
import com.groceryshop.dto.ProductDTO;
import com.groceryshop.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/mvc")
public class MvcDashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private OrderService orderService;

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        DashboardStatsDTO stats = dashboardService.getStats();
        List<ProductDTO> allProducts = productService.getAllProductsAdmin();
        List<ProductDTO> lowStockProducts = allProducts.stream()
                .filter(p -> p.getCurrentStock() != null && p.getMinimumStock() != null
                        && p.getCurrentStock() <= p.getMinimumStock())
                .collect(Collectors.toList());

        List<OrderDTO> recentOrders = orderService.getAllOrders().stream()
                .limit(8)
                .collect(Collectors.toList());

        model.addAttribute("pageTitle", "Tổng quan Hệ thống MiniMart (Thymeleaf + Bootstrap)");
        model.addAttribute("stats", stats);
        model.addAttribute("totalCategories", categoryService.getAllCategories().size());
        model.addAttribute("totalSuppliers", supplierService.getAllSuppliers().size());
        model.addAttribute("lowStockCount", lowStockProducts.size());
        model.addAttribute("lowStockProducts", lowStockProducts);
        model.addAttribute("recentOrders", recentOrders);
        return "mvc/dashboard";
    }
}

