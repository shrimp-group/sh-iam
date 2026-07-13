package com.wkclz.auth.filter;

import com.wkclz.auth.bean.Principal;
import com.wkclz.auth.config.AuthProperties;
import com.wkclz.auth.context.SecurityContext;
import com.wkclz.auth.contract.authz.AccessControlProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 鉴权过滤器
 */
@Slf4j
@RequiredArgsConstructor
public class AuthorizationFilter extends OncePerRequestFilter {

    private final List<AccessControlProvider> accessControlProviders;
    private final AuthProperties properties;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isWhiteListed(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (accessControlProviders == null || accessControlProviders.isEmpty()) {
            log.debug("无 AccessControlProvider 实现，鉴权放行: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        Principal principal = SecurityContext.getPrincipal();
        if (principal == null) {
            log.warn("鉴权时 Principal 为空，拒绝访问: uri={}", request.getRequestURI());
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "未认证");
            return;
        }

        for (AccessControlProvider provider : accessControlProviders) {
            if (!provider.hasPermission(principal, request.getMethod(), request.getRequestURI())) {
                log.warn("鉴权拒绝: user={}, method={}, uri={}", principal.getUsername(),
                        request.getMethod(), request.getRequestURI());
                sendError(response, HttpServletResponse.SC_FORBIDDEN, "无权访问");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isWhiteListed(String uri) {
        return properties.getWhiteList().getPaths().stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, uri));
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + escapeJson(message) + "\"}");
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
