package com.groceryshop.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                Long userId = tokenProvider.getUserIdFromJWT(jwt);

                UserDetails userDetails = customUserDetailsService.loadUserById(userId);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);

                // Đồng bộ SecurityContext vào HttpSession để các view Thymeleaf MVC nhúng trong React SPA
                // dùng chung phiên đăng nhập mà không bao giờ cần đăng nhập lại
                String uri = request.getRequestURI();
                if (uri != null && uri.startsWith("/mvc")) {
                    HttpSession session = request.getSession(true);
                    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
                }
            }
        } catch (Exception ex) {
            logger.error("Không thể xác thực người dùng trong Security Context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        // 1. Ưu tiên đọc từ Header Authorization: Bearer <token>
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2. Đọc từ Cookie jwt_token (được đồng bộ tự động từ React AuthContext)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt_token".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }

        // 3. Đọc từ Query Parameter ?jwt=<token> (nếu truyền trực tiếp qua URL nhúng)
        String paramToken = request.getParameter("jwt");
        if (StringUtils.hasText(paramToken)) {
            return paramToken;
        }

        return null;
    }
}
