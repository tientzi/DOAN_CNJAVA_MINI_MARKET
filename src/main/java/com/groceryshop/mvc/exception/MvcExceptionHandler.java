package com.groceryshop.mvc.exception;

import com.groceryshop.exception.BadRequestException;
import com.groceryshop.exception.ResourceNotFoundException;
import com.groceryshop.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.UUID;

@ControllerAdvice(basePackages = "com.groceryshop.mvc.controller")
public class MvcExceptionHandler {

    public static final String SESSION_CSRF_KEY = "MVC_CSRF_TOKEN";

    @ModelAttribute
    public void populateCommonAttributes(Model model, HttpServletRequest request, HttpSession session) {
        String csrfToken = (String) session.getAttribute(SESSION_CSRF_KEY);
        if (csrfToken == null) {
            csrfToken = UUID.randomUUID().toString();
            session.setAttribute(SESSION_CSRF_KEY, csrfToken);
        }
        model.addAttribute("mvcCsrfToken", csrfToken);
        model.addAttribute("currentUri", request.getRequestURI());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        model.addAttribute("isAuthenticated", isAuthenticated);

        if (isAuthenticated) {
            model.addAttribute("currentUsername", auth.getName());
            String role = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("ROLE_USER");
            model.addAttribute("currentRole", role);
            model.addAttribute("isAdmin", "ROLE_ADMIN".equals(role));
            model.addAttribute("isShipper", "ROLE_SHIPPER".equals(role));
            model.addAttribute("isUser", "ROLE_USER".equals(role));

            if (auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
                model.addAttribute("currentUserId", userPrincipal.getId());
                model.addAttribute("currentEmail", userPrincipal.getEmail());
            }
        } else {
            model.addAttribute("currentUsername", "Khách");
            model.addAttribute("currentRole", "GUEST");
            model.addAttribute("isAdmin", false);
            model.addAttribute("isShipper", false);
            model.addAttribute("isUser", false);
        }
    }

    public static void verifyCsrf(HttpSession session, String submittedToken) {
        String expected = (String) session.getAttribute(SESSION_CSRF_KEY);
        if (expected != null && (submittedToken == null || !expected.equals(submittedToken))) {
            throw new BadRequestException("Phiên làm việc hoặc mã bảo mật CSRF không hợp lệ. Vui lòng tải lại trang và thử lại!");
        }
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(ResourceNotFoundException ex, HttpServletRequest request, Model model) {
        model.addAttribute("errorCode", 404);
        model.addAttribute("errorTitle", "Không tìm thấy dữ liệu yêu cầu");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("requestUri", request.getRequestURI());
        return "mvc/error";
    }

    @ExceptionHandler(BadRequestException.class)
    public String handleBadRequest(BadRequestException ex, HttpServletRequest request, Model model) {
        model.addAttribute("errorCode", 400);
        model.addAttribute("errorTitle", "Yêu cầu không hợp lệ hoặc vi phạm ràng buộc nghiệp vụ");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("requestUri", request.getRequestURI());
        return "mvc/error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, HttpServletRequest request, Model model) {
        model.addAttribute("errorCode", 500);
        model.addAttribute("errorTitle", "Đã xảy ra lỗi trong quá trình xử lý");
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "Lỗi hệ thống không xác định");
        model.addAttribute("requestUri", request.getRequestURI());
        return "mvc/error";
    }
}

