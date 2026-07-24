package com.wkclz.iam.session.filter;

import com.alibaba.fastjson2.JSON;
import com.wkclz.core.identity.IdentityContext;
import com.wkclz.core.identity.UserIdentity;
import com.wkclz.iam.session.bean.Session;
import com.wkclz.iam.session.bean.TokenInfo;
import com.wkclz.iam.session.service.SessionManager;
import com.wkclz.iam.session.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 会话认证过滤器 — 每次请求时尽力设置 IdentityContext，再做准入判断。
 *
 * <p>职责：
 * <ul>
 *   <li>无条件设置 appCode + tenantCode（每个请求都有）</li>
 *   <li>Best-effort 获取用户身份（允许失败，不阻断）</li>
 *   <li>准入判断：白名单放行 / 非白名单无身份则 401</li>
 *   <li>不做 IdentityContext.clear()（由外层 RequestRecordFilter 负责）</li>
 * </ul>
 */
@Component
public class SessionAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SessionAuthFilter.class);

    @Autowired
    private TokenService tokenService;
    @Autowired
    private SessionManager sessionManager;
    @Autowired
    private WhiteListMatcher whiteListMatcher;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String requestUri = request.getRequestURI();

        // 1. 无条件设置 appCode + tenantCode
        IdentityContext.setAppCode(request.getHeader("app-code"));
        IdentityContext.setTenantCode(request.getHeader("tenant-code"));

        // 2. Best-effort 获取用户身份（允许失败，不抛异常）
        String token = TokenService.resolve(request);
        if (token != null) {
            trySessionIdentity(token);
            if (IdentityContext.get() == null) {
                tryTokenIdentity(token);
            }
        }

        // 3. 准入判断
        if (whiteListMatcher.isWhiteListed(requestUri)) {
            // 白名单：无论有无身份，放行
            chain.doFilter(request, response);
            return;
        }
        if (IdentityContext.get() == null) {
            // 非白名单 + 无身份：401
            log.warn("No identity for non-whitelisted URI: {}", requestUri);
            writeUnauthorized(response, token == null ? "缺少认证 Token" : "会话无效或已过期");
            return;
        }

        // 非白名单 + 有身份：放行
        chain.doFilter(request, response);
    }

    /**
     * 优先策略：通过完整会话验证获取身份。
     */
    private void trySessionIdentity(String token) {
        try {
            Session session = sessionManager.validateAndRefresh(token);
            if (session != null && session.getUserIdentity() != null) {
                UserIdentity userIdentity = JSON.parseObject(session.getUserIdentity(), UserIdentity.class);
                IdentityContext.set(userIdentity, token);
                log.debug("Identity set from session: userCode={}", userIdentity.getUserCode());
            }
        } catch (Exception e) {
            log.debug("Session validation failed (best-effort): {}", e.getMessage());
        }
    }

    /**
     * 兜底策略：从 JWT claims 直接解析身份（含过期 token）。
     */
    private void tryTokenIdentity(String token) {
        try {
            TokenInfo tokenInfo = tokenService.parseClaimsBestEffort(token);
            if (tokenInfo != null && tokenInfo.getUserCode() != null) {
                UserIdentity userIdentity = new UserIdentity();
                userIdentity.setUserCode(tokenInfo.getUserCode());
                userIdentity.setUsername(tokenInfo.getUsername());
                userIdentity.setNickname(tokenInfo.getNickname());
                IdentityContext.set(userIdentity, token);
                log.debug("Identity set from token claims (best-effort): userCode={}", tokenInfo.getUserCode());
            }
        } catch (Exception e) {
            log.debug("Token parse failed (best-effort): {}", e.getMessage());
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(JSON.toJSONString(new ErrorResponse(message)).getBytes(StandardCharsets.UTF_8));
    }

    private record ErrorResponse(String message) {
    }
}
