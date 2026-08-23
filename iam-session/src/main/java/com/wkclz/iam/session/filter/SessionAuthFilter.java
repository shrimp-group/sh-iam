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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 会话认证过滤器 — 每次请求时尽力设置 IdentityContext，再做准入判断。
 * <p>显式指定 Order（LOWEST_PRECEDENCE - 5），保证位于 RequestRecordFilter（LOWEST_PRECEDENCE - 10）之后、
 * RequestControlFilter（LOWEST_PRECEDENCE）之前，确保身份设置先于请求控制执行。</p>
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 5)
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

        // 2. 获取身份：准入的唯一依据是"完整会话"（JWT 有效 + Redis 会话存在）。
        //    允许失败，不阻断后续白名单判断
        String token = TokenService.resolve(request);
        if (token != null) {
            trySessionIdentity(token);
        }

        // 3. 白名单接口：无论有无身份都放行；
        //    无有效会话时尽力解析 token claims，仅用于审计日志等场景，不作为准入依据
        if (whiteListMatcher.isWhiteListed(requestUri)) {
            if (IdentityContext.get() == null && token != null) {
                tryTokenIdentity(token);
            }
            chain.doFilter(request, response);
            return;
        }

        // 4. 非白名单：必须持有有效会话（JWT 未过期且 Redis 会话存在）
        if (IdentityContext.get() == null) {
            // 非白名单 + 无有效会话：401
            log.warn("No valid session for non-whitelisted URI: {}", requestUri);
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
     * 白名单接口专用：从 JWT claims 直接解析身份（含过期 token，best-effort）。
     *
     * <p>仅用于白名单接口在无有效会话时，尽最大可能为审计日志提供身份信息；
     * 不作为非白名单接口的准入依据（准入只认完整会话验证 trySessionIdentity）。</p>
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
