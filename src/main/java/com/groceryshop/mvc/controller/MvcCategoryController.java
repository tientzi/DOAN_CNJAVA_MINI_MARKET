package com.groceryshop.mvc.controller;

import com.groceryshop.dto.CategoryDTO;
import com.groceryshop.exception.BadRequestException;
import com.groceryshop.mvc.exception.MvcExceptionHandler;
import com.groceryshop.mvc.form.MvcCategoryForm;
import com.groceryshop.service.CategoryService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/mvc/categories")
public class MvcCategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public String listCategories(@RequestParam(required = false) String keyword,
                                 @RequestParam(defaultValue = "id_asc") String sortBy,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "5") int size,
                                 Model model) {
        List<CategoryDTO> filtered = categoryService.getAllCategories().stream()
                .filter(c -> {
                    if (keyword == null || keyword.trim().isEmpty()) return true;
                    String kw = keyword.trim().toLowerCase();
                    return (c.getName() != null && c.getName().toLowerCase().contains(kw))
                            || (c.getDescription() != null && c.getDescription().toLowerCase().contains(kw));
                })
                .collect(Collectors.toList());

        if ("name_asc".equals(sortBy)) {
            filtered.sort(Comparator.comparing(c -> c.getName() != null ? c.getName() : ""));
        } else if ("name_desc".equals(sortBy)) {
            filtered.sort(Comparator.comparing((CategoryDTO c) -> c.getName() != null ? c.getName() : "").reversed());
        } else if ("id_desc".equals(sortBy)) {
            filtered.sort(Comparator.comparing(CategoryDTO::getId).reversed());
        } else {
            filtered.sort(Comparator.comparing(CategoryDTO::getId));
        }

        int totalItems = filtered.size();
        int pageSize = Math.max(size, 1);
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        int currentPage = Math.max(0, Math.min(page, Math.max(totalPages - 1, 0)));
        int fromIndex = Math.min(currentPage * pageSize, totalItems);
        int toIndex = Math.min(fromIndex + pageSize, totalItems);
        List<CategoryDTO> pagedList = filtered.subList(fromIndex, toIndex);

        model.addAttribute("pageTitle", "Quản lý Danh mục Ngành hàng");
        model.addAttribute("categories", pagedList);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("pageSize", pageSize);
        return "mvc/categories";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        MvcCategoryForm form = MvcCategoryForm.builder()
                .image("/uploads/cat_rau_cu_qua.jpg")
                .isActive(true)
                .build();
        model.addAttribute("pageTitle", "Thêm Danh mục Ngành hàng mới");
        model.addAttribute("categoryForm", form);
        return "mvc/category-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        CategoryDTO dto = categoryService.getCategoryById(id);
        model.addAttribute("pageTitle", "Chỉnh sửa Danh mục #" + id);
        model.addAttribute("categoryForm", MvcCategoryForm.fromDTO(dto));
        return "mvc/category-form";
    }

    @PostMapping("/save")
    public String saveCategory(@Valid @ModelAttribute("categoryForm") MvcCategoryForm form,
                               BindingResult bindingResult,
                               @RequestParam(name = "_mvcCsrf", required = false) String csrfToken,
                               HttpSession session,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        MvcExceptionHandler.verifyCsrf(session, csrfToken);

        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle",
                    form.getId() == null ? "Thêm Danh mục Ngành hàng mới" : "Chỉnh sửa Danh mục #" + form.getId());
            return "mvc/category-form";
        }

        try {
            if (form.getId() == null) {
                CategoryDTO created = categoryService.createCategory(form.toDTO());
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã thêm danh mục mới thành công: " + created.getName());
            } else {
                CategoryDTO updated = categoryService.updateCategory(form.getId(), form.toDTO());
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã cập nhật danh mục thành công: " + updated.getName());
            }
            return "redirect:/mvc/categories";
        } catch (BadRequestException ex) {
            bindingResult.rejectValue("name", "name.duplicate", ex.getMessage());
            model.addAttribute("pageTitle",
                    form.getId() == null ? "Thêm Danh mục Ngành hàng mới" : "Chỉnh sửa Danh mục #" + form.getId());
            return "mvc/category-form";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id,
                                 @RequestParam(name = "_mvcCsrf", required = false) String csrfToken,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        MvcExceptionHandler.verifyCsrf(session, csrfToken);
        try {
            CategoryDTO target = categoryService.getCategoryById(id);
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã xóa danh mục '" + target.getName() + "' thành công.");
        } catch (BadRequestException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/mvc/categories";
    }
}

