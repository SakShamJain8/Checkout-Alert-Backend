package com.checkoutalert.checkoutalert.config;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res,
                         FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;

        // prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // prevent clickjacking
        response.setHeader("X-Frame-Options", "DENY");

        // XSS protection
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // HSTS — force HTTPS for 1 year
        response.setHeader("Strict-Transport-Security",
                "max-age=31536000; includeSubDomains");

        // referrer policy
        response.setHeader("Referrer-Policy",
                "strict-origin-when-cross-origin");

        // content security policy
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline'; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data:; " +
                        "connect-src 'self'");

        chain.doFilter(req, res);
    }
}
