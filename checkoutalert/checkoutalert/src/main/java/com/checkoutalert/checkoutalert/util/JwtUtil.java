package com.checkoutalert.checkoutalert.util;

import com.checkoutalert.checkoutalert.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

public class JwtUtil {

    private final JwtService jwtService;

    public JwtUtil(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    // returns null if token is missing or invalid
    public String extractEmail(HttpServletRequest request) {
        // first try SecurityContext — set by JwtFilterAdapter
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String email) {
            return email;
        }

        // fallback — parse token directly
        try {
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("auth_token".equals(cookie.getName())) {
                        String token = cookie.getValue();
                        if (jwtService.isValid(token)) {
                            return jwtService.extractEmail(token);
                        }
                    }
                }
            }
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                if (jwtService.isValid(token)) {
                    return jwtService.extractEmail(token);
                }
            }
        } catch (Exception e) {
            System.err.println("Token extraction failed: " + e.getClass().getSimpleName());
        }
        return null;
    }
}