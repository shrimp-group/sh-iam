package com.wkclz.auth.filter;

import com.wkclz.auth.bean.SecurityHeaders;
import com.wkclz.auth.contract.infra.SecurityHeaderProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * HTTP 安全头注入过滤器。
 * <p>
 * 先设置内置默认安全头，再应用外部 SecurityHeaderProvider 提供的值（可覆盖默认值）。
 */
@Slf4j
public class SecurityHeaderFilter extends OncePerRequestFilter {

    /**
     * 内置默认安全头（不包含 HSTS 和 CSP，需站点按需配置）
     */
    private static final SecurityHeaders DEFAULT_HEADERS = new SecurityHeaders();

    static {
        DEFAULT_HEADERS.setXContentTypeOptions("nosniff");
        DEFAULT_HEADERS.setXFrameOptions("DENY");
        DEFAULT_HEADERS.setXXssProtection("1; mode=block");
        DEFAULT_HEADERS.setReferrerPolicy("strict-origin-when-cross-origin");
    }

    private final List<SecurityHeaderProvider> headerProviders;

    public SecurityHeaderFilter(List<SecurityHeaderProvider> headerProviders) {
        this.headerProviders = headerProviders;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 优先设置内置默认安全头
        applyHeaders(response, DEFAULT_HEADERS);

        // 外部 Provider 可覆盖默认值
        if (headerProviders != null) {
            for (SecurityHeaderProvider provider : headerProviders) {
                SecurityHeaders headers = provider.getHeaders();
                if (headers != null) {
                    applyHeaders(response, headers);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void applyHeaders(HttpServletResponse response, SecurityHeaders headers) {
        setIfNotNull(response, "X-Frame-Options", headers.getXFrameOptions());
        setIfNotNull(response, "X-Content-Type-Options", headers.getXContentTypeOptions());
        setIfNotNull(response, "Strict-Transport-Security", headers.getStrictTransportSecurity());
        setIfNotNull(response, "X-XSS-Protection", headers.getXXssProtection());
        setIfNotNull(response, "Referrer-Policy", headers.getReferrerPolicy());
        setIfNotNull(response, "Content-Security-Policy", headers.getContentSecurityPolicy());
    }

    private void setIfNotNull(HttpServletResponse response, String name, String value) {
        if (value != null && !value.isEmpty()) {
            response.setHeader(name, value);
        }
    }
}
