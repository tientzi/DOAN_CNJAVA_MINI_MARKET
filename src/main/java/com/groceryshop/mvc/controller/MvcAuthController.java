package com.groceryshop.mvc.controller;

import com.groceryshop.security.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mvc")
public class MvcAuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("pageTitle", "Đăng nhập Phân hệ Thymeleaf MVC");
        return "mvc/login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes,
                          Model model) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username.trim(), password)
            );
            saveAuthenticationToSession(authentication, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đăng nhập thành công với tài khoản: " + authentication.getName());
            return "redirect:/mvc";
        } catch (BadCredentialsException ex) {
            model.addAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu không chính xác!");
            model.addAttribute("username", username);
            model.addAttribute("pageTitle", "Đăng nhập Phân hệ Thymeleaf MVC");
            return "mvc/login";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Không thể đăng nhập: " + ex.getMessage());
            model.addAttribute("username", username);
            model.addAttribute("pageTitle", "Đăng nhập Phân hệ Thymeleaf MVC");
            return "mvc/login";
        }
    }

    @PostMapping("/quick-login")
    public String quickLoginDemo(@RequestParam String username,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username.trim());
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            );
            saveAuthenticationToSession(auth, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã chuyển nhanh sang tài khoản Demo: " + userDetails.getUsername() + " (" + userDetails.getAuthorities() + ")");
            return "redirect:/mvc";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tài khoản demo: " + username);
            return "redirect:/mvc/login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        redirectAttributes.addFlashAttribute("successMessage", "Đã đăng xuất khỏi phân hệ Thymeleaf MVC.");
        return "redirect:/mvc/login";
    }

    @GetMapping("/access-denied")
    public String accessDenied(HttpServletRequest request, Model model) {
        model.addAttribute("errorCode", 403);
        model.addAttribute("errorTitle", "Truy cập bị từ chối (Không đủ quyền hạn)");
        model.addAttribute("errorMessage",
                "Tài khoản hiện tại của bạn không có quyền thực hiện thao tác hoặc truy cập trang quản trị này. Vui lòng đăng nhập bằng tài khoản Quản trị viên (ROLE_ADMIN).");
        model.addAttribute("requestUri", request.getRequestURI());
        return "mvc/error";
    }

    private void saveAuthenticationToSession(Authentication authentication, HttpServletRequest request) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
    }
}

