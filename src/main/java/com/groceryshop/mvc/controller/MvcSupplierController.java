package com.groceryshop.mvc.controller;

import com.groceryshop.dto.SupplierDTO;
import com.groceryshop.dto.SupplierProductDTO;
import com.groceryshop.exception.BadRequestException;
import com.groceryshop.mvc.exception.MvcExceptionHandler;
import com.groceryshop.mvc.form.MvcSupplierForm;
import com.groceryshop.service.SupplierService;
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
@RequestMapping("/mvc/suppliers")
public class MvcSupplierController {

    @Autowired
    private SupplierService supplierService;

    @GetMapping
    public String listSuppliers(@RequestParam(required = false) String keyword,
                                @RequestParam(defaultValue = "id_asc") String sortBy,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "5") int size,
                                Model model) {
        List<SupplierDTO> filtered = supplierService.getAllSuppliers().stream()
                .filter(s -> {
                    if (keyword == null || keyword.trim().isEmpty()) return true;
                    String kw = keyword.trim().toLowerCase();
                    return (s.getName() != null && s.getName().toLowerCase().contains(kw))
                            || (s.getContactName() != null && s.getContactName().toLowerCase().contains(kw))
                            || (s.getPhone() != null && s.getPhone().toLowerCase().contains(kw))
                            || (s.getEmail() != null && s.getEmail().toLowerCase().contains(kw));
                })
                .collect(Collectors.toList());

        if ("name_asc".equals(sortBy)) {
            filtered.sort(Comparator.comparing(s -> s.getName() != null ? s.getName() : ""));
        } else if ("name_desc".equals(sortBy)) {
            filtered.sort(Comparator.comparing((SupplierDTO s) -> s.getName() != null ? s.getName() : "").reversed());
        } else if ("id_desc".equals(sortBy)) {
            filtered.sort(Comparator.comparing(SupplierDTO::getId).reversed());
        } else {
            filtered.sort(Comparator.comparing(SupplierDTO::getId));
        }

        int totalItems = filtered.size();
        int pageSize = Math.max(size, 1);
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        int currentPage = Math.max(0, Math.min(page, Math.max(totalPages - 1, 0)));
        int fromIndex = Math.min(currentPage * pageSize, totalItems);
        int toIndex = Math.min(fromIndex + pageSize, totalItems);
        List<SupplierDTO> pagedList = filtered.subList(fromIndex, toIndex);

        model.addAttribute("pageTitle", "Quản lý Nhà cung cấp Đối tác");
        model.addAttribute("suppliers", pagedList);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("pageSize", pageSize);
        return "mvc/suppliers";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        MvcSupplierForm form = MvcSupplierForm.builder()
                .isActive(true)
                .build();
        model.addAttribute("pageTitle", "Thêm Nhà cung cấp mới");
        model.addAttribute("supplierForm", form);
        return "mvc/supplier-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        SupplierDTO dto = supplierService.getSupplierById(id);
        List<SupplierProductDTO> suppliedProducts = supplierService.getProductsBySupplier(id);
        model.addAttribute("pageTitle", "Chỉnh sửa Nhà cung cấp #" + id);
        model.addAttribute("supplierForm", MvcSupplierForm.fromDTO(dto));
        model.addAttribute("suppliedProducts", suppliedProducts);
        return "mvc/supplier-form";
    }

    @PostMapping("/save")
    public String saveSupplier(@Valid @ModelAttribute("supplierForm") MvcSupplierForm form,
                               BindingResult bindingResult,
                               @RequestParam(name = "_mvcCsrf", required = false) String csrfToken,
                               HttpSession session,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        MvcExceptionHandler.verifyCsrf(session, csrfToken);

        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle",
                    form.getId() == null ? "Thêm Nhà cung cấp mới" : "Chỉnh sửa Nhà cung cấp #" + form.getId());
            if (form.getId() != null) {
                model.addAttribute("suppliedProducts", supplierService.getProductsBySupplier(form.getId()));
            }
            return "mvc/supplier-form";
        }

        try {
            if (form.getId() == null) {
                SupplierDTO created = supplierService.createSupplier(form.toDTO());
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã thêm nhà cung cấp đối tác thành công: " + created.getName());
            } else {
                SupplierDTO updated = supplierService.updateSupplier(form.getId(), form.toDTO());
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã cập nhật thông tin nhà cung cấp thành công: " + updated.getName());
            }
            return "redirect:/mvc/suppliers";
        } catch (BadRequestException ex) {
            bindingResult.rejectValue("name", "name.duplicate", ex.getMessage());
            model.addAttribute("pageTitle",
                    form.getId() == null ? "Thêm Nhà cung cấp mới" : "Chỉnh sửa Nhà cung cấp #" + form.getId());
            return "mvc/supplier-form";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteSupplier(@PathVariable Long id,
                                 @RequestParam(name = "_mvcCsrf", required = false) String csrfToken,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        MvcExceptionHandler.verifyCsrf(session, csrfToken);
        try {
            SupplierDTO target = supplierService.getSupplierById(id);
            supplierService.deleteSupplier(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã xóa nhà cung cấp '" + target.getName() + "' khỏi hệ thống.");
        } catch (BadRequestException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/mvc/suppliers";
    }
}

