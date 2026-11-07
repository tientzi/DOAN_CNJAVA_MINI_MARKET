package com.groceryshop.mvc.controller;

import com.groceryshop.entity.User;
import com.groceryshop.repository.UserRepository;
import com.groceryshop.security.CustomUserDetailsService;
import com.groceryshop.security.JwtTokenProvider;
import com.groceryshop.security.UserPrincipal;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mvc")
public class MvcAuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("pageTitle", "Đăng nhập Phân hệ Thymeleaf MVC");
        return "mvc/login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          HttpServletRequest request,
                          HttpServletResponse response,
                          RedirectAttributes redirectAttributes,
                          Model model) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username.trim(), password)
            );
            saveAuthenticationAndSyncJwt(authentication, request, response);
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

    /**
     * Hỗ trợ cả GET và POST để chuyển đổi nhanh Role demo (bấm link hoặc gửi form)
     */
    @RequestMapping(value = "/quick-login", method = {RequestMethod.GET, RequestMethod.POST})
    public String quickLoginDemo(@RequestParam(required = false) String username,
                                 @RequestParam(required = false) String role,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 RedirectAttributes redirectAttributes) {
        String targetUser = username;
        if (targetUser == null || targetUser.trim().isEmpty()) {
            if ("shipper".equalsIgnoreCase(role)) {
                targetUser = "shipper1";
            } else if ("user".equalsIgnoreCase(role)) {
                targetUser = "user1";
            } else {
                targetUser = "admin";
            }
        }

        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(targetUser.trim());
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            );
            saveAuthenticationAndSyncJwt(auth, request, response);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã chuyển nhanh sang tài khoản Demo: " + userDetails.getUsername() + " (" + userDetails.getAuthorities() + ")");
            return "redirect:/mvc";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tài khoản demo: " + targetUser);
            return "redirect:/mvc/login";
        }
    }

    @RequestMapping(value = "/logout", method = {RequestMethod.GET, RequestMethod.POST})
    public String logout(HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        // Xóa cookie đồng bộ nếu có
        Cookie tokenCookie = new Cookie("jwt_sync_token", "");
        tokenCookie.setPath("/");
        tokenCookie.setMaxAge(0);
        response.addCookie(tokenCookie);

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

    private void saveAuthenticationAndSyncJwt(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        try {
            // Sinh JWT token để đồng bộ sang React SPA (SSO)
            String jwt = tokenProvider.generateToken(authentication, true);
            session.setAttribute("jwtSyncToken", jwt);

            String username = authentication.getName();
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                String role = user.getRole() != null ? user.getRole().getName() : "ROLE_USER";
                String userJson = String.format("{\"id\":%d,\"username\":\"%s\",\"email\":\"%s\",\"role\":\"%s\",\"fullName\":\"%s\"}",
                        user.getId(), user.getUsername(), user.getEmail() != null ? user.getEmail() : "", role,
                        user.getFullName() != null ? user.getFullName() : user.getUsername());
                session.setAttribute("userSyncJson", userJson);
            }
        } catch (Exception ignored) {}
    }
}