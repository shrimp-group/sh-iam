package com.wkclz.auth.filter;

import com.wkclz.auth.bean.SecurityHeaders;
import com.wkclz.auth.contract.infra.SecurityHeaderProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * HTTP 安全头注入过滤器
 */
@Slf4j
@RequiredArgsConstructor
public class SecurityHeaderFilter extends OncePerRequestFilter {

    private final List<SecurityHeaderProvider> headerProviders;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        for (SecurityHeaderProvider provider : headerProviders) {
            SecurityHeaders headers = provider.getHeaders();
            if (headers == null) continue;
            setIfNotNull(response, "X-Frame-Options", headers.getXFrameOptions());
            setIfNotNull(response, "X-Content-Type-Options", headers.getXContentTypeOptions());
            setIfNotNull(response, "Strict-Transport-Security", headers.getStrictTransportSecurity());
            setIfNotNull(response, "X-XSS-Protection", headers.getXXssProtection());
            setIfNotNull(response, "Referrer-Policy", headers.getReferrerPolicy());
            setIfNotNull(response, "Content-Security-Policy", headers.getContentSecurityPolicy());
        }
        filterChain.doFilter(request, response);
    }

    private void setIfNotNull(HttpServletResponse response, String name, String value) {
        if (value != null && !value.isEmpty()) {
            response.setHeader(name, value);
        }
    }
}
