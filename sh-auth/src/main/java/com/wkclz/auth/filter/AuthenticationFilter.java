package com.wkclz.auth.filter;

import com.wkclz.auth.bean.Principal;
import com.wkclz.auth.bean.Session;
import com.wkclz.auth.config.AuthConstants;
import com.wkclz.auth.config.AuthProperties;
import com.wkclz.auth.context.SecurityContext;
import com.wkclz.auth.contract.auth.SessionStore;
import com.wkclz.auth.contract.auth.TokenService;
import com.wkclz.auth.exception.AuthenticationException;
import com.wkclz.auth.exception.SessionExpiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 认证过滤器
 */
@Slf4j
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final SessionStore sessionStore;
    private final AuthProperties properties;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isWhiteListed(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = resolveToken(request);
            if (token == null || token.isEmpty()) {
                log.warn("请求缺少 Token: uri={}, ip={}", request.getRequestURI(), request.getRemoteAddr());
                sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "缺少认证 Token");
                return;
            }

            Principal principal = tokenService.parseToken(token);
            if (principal == null) {
                log.warn("Token 解析失败: uri={}, token={}...", request.getRequestURI(), token.substring(0, Math.min(8, token.length())));
                sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token 解析失败");
                return;
            }

            if (!tokenService.validateToken(token)) {
                log.warn("Token 无效或已过期: uri={}", request.getRequestURI());
                sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token 无效或已过期");
                return;
            }

            Session session = sessionStore.get(token);
            if (session == null) {
                log.warn("会话不存在或已过期: uri={}, user={}", request.getRequestURI(), principal.getUsername());
                sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "会话不存在或已过期");
                return;
            }

            SecurityContext.setPrincipal(principal);
            SecurityContext.setToken(token);

            filterChain.doFilter(request, response);
        } catch (AuthenticationException e) {
            log.warn("认证失败: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        } catch (SessionExpiredException e) {
            log.warn("会话过期: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(AuthConstants.DEFAULT_TOKEN_HEADER);
        if (bearer != null && bearer.startsWith(AuthConstants.BEARER_PREFIX)) {
            return bearer.substring(AuthConstants.BEARER_PREFIX.length());
        }
        return request.getHeader(AuthConstants.CUSTOM_TOKEN_HEADER);
    }

    private boolean isWhiteListed(String uri) {
        return properties.getWhiteList().getPaths().stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, uri));
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + message + "\"}");
    }
}
