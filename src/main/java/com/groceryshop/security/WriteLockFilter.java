package com.groceryshop.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groceryshop.service.SystemActivityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Component
public class WriteLockFilter extends OncePerRequestFilter {

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    @Autowired
    private SystemActivityService systemActivityService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        if (systemActivityService.isWriteLocked() && method != null && MUTATING_METHODS.contains(method.toUpperCase())) {
            boolean allowedDuringLock = uri != null && (
                    uri.startsWith("/api/admin/backups")
                    || uri.startsWith("/api/auth/")
                    || uri.startsWith("/oauth2/")
                    || uri.startsWith("/login/")
            );

            if (!allowedDuringLock) {
                String reason = systemActivityService.getLockReason();
                String msg = "Hệ thống đang tạm khóa thao tác ghi (" + reason + "). Vui lòng thử lại sau vài giây!";
                response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
                response.setCharacterEncoding("UTF-8");
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getWriter(), Map.of(
                        "error", msg,
                        "message", msg,
                        "code", "SYSTEM_WRITE_LOCKED"
                ));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
