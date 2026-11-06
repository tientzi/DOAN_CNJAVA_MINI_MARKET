package com.groceryshop.mvc.controller;

import com.groceryshop.dto.ProductDTO;
import com.groceryshop.exception.BadRequestException;
import com.groceryshop.mvc.exception.MvcExceptionHandler;
import com.groceryshop.mvc.form.MvcProductForm;
import com.groceryshop.service.BrandService;
import com.groceryshop.service.CategoryService;
import com.groceryshop.service.ProductService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mvc/products")
public class MvcProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;

    @GetMapping
    public String listProducts(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Long categoryId,
                               @RequestParam(required = false) Long brandId,
                               @RequestParam(defaultValue = "id_asc") String sortBy,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "5") int size,
                               Model model) {
        Page<ProductDTO> productPage = productService.getProductsPagedAdmin(keyword, categoryId, brandId, sortBy, page, size);

        model.addAttribute("pageTitle", "Quản lý Sản phẩm (CRUD + Tìm kiếm + Phân trang + Sắp xếp)");
        model.addAttribute("productPage", productPage);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("brands", brandService.getAllBrands());
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("brandId", brandId);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("currentPage", productPage.getNumber());
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalItems", productPage.getTotalElements());
        model.addAttribute("pageSize", size);
        return "mvc/products";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        MvcProductForm form = MvcProductForm.builder()
                .unit("Hộp")
                .weightG(500)
                .minimumStock(5)
                .location("Kệ A1")
                .isActive(true)
                .currentStock(0)
                .build();
        populateFormReferences(model, form, "Thêm Sản phẩm mới");
        return "mvc/product-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        ProductDTO dto = productService.getProductById(id);
        MvcProductForm form = MvcProductForm.fromDTO(dto);
        populateFormReferences(model, form, "Chỉnh sửa Sản phẩm #" + id);
        return "mvc/product-form";
    }

    @PostMapping("/save")
    public String saveProduct(@Valid @ModelAttribute("productForm") MvcProductForm form,
                              BindingResult bindingResult,
                              @RequestParam(name = "_mvcCsrf", required = false) String csrfToken,
                              HttpSession session,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        MvcExceptionHandler.verifyCsrf(session, csrfToken);

        if (form.getSalePrice() != null && form.getPrice() != null
                && form.getSalePrice().compareTo(form.getPrice()) > 0) {
            bindingResult.rejectValue("salePrice", "salePrice.invalid",
                    "Giá khuyến mãi không được lớn hơn giá bán niêm yết");
        }

        if (bindingResult.hasErrors()) {
            populateFormReferences(model, form,
                    form.getId() == null ? "Thêm Sản phẩm mới" : "Chỉnh sửa Sản phẩm #" + form.getId());
            return "mvc/product-form";
        }

        try {
            if (form.getId() == null) {
                ProductDTO created = productService.createProduct(form.toDTO());
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã thêm sản phẩm mới thành công: " + created.getName() + " (Mã SKU: " + created.getSku() + ")");
            } else {
                ProductDTO updated = productService.updateProduct(form.getId(), form.toDTO());
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã cập nhật thông tin sản phẩm thành công: " + updated.getName());
            }
            return "redirect:/mvc/products";
        } catch (BadRequestException ex) {
            if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("sku")) {
                bindingResult.rejectValue("sku", "sku.duplicate", ex.getMessage());
            } else {
                bindingResult.rejectValue("name", "name.duplicate", ex.getMessage());
            }
            populateFormReferences(model, form,
                    form.getId() == null ? "Thêm Sản phẩm mới" : "Chỉnh sửa Sản phẩm #" + form.getId());
            return "mvc/product-form";
        }
    }

    @PostMapping("/toggle/{id}")
    public String toggleStatus(@PathVariable Long id,
                               @RequestParam(name = "_mvcCsrf", required = false) String csrfToken,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        MvcExceptionHandler.verifyCsrf(session, csrfToken);
        ProductDTO updated = productService.toggleProductStatus(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã đổi trạng thái sản phẩm '" + updated.getName() + "' sang: "
                        + (Boolean.TRUE.equals(updated.getIsActive()) ? "Đang mở bán" : "Tạm ngừng kinh doanh"));
        return "redirect:/mvc/products";
    }

    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id,
                                @RequestParam(name = "_mvcCsrf", required = false) String csrfToken,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        MvcExceptionHandler.verifyCsrf(session, csrfToken);
        try {
            ProductDTO target = productService.getProductById(id);
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã xóa sản phẩm '" + target.getName() + "' khỏi cơ sở dữ liệu.");
        } catch (BadRequestException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/mvc/products";
    }

    private void populateFormReferences(Model model, MvcProductForm form, String title) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("productForm", form);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("brands", brandService.getAllBrands());
    }
}

