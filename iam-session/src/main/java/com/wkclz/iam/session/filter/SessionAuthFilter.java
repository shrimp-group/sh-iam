package com.wkclz.iam.session.filter;

import com.alibaba.fastjson2.JSON;
import com.wkclz.core.identity.IdentityContext;
import com.wkclz.core.identity.UserIdentity;
import com.wkclz.iam.session.bean.Session;
import com.wkclz.iam.session.service.SessionManager;
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
 * 会话认证过滤器 — 每次请求时验证 Token，设置 IdentityContext，请求结束后清理。
 *
 * <p>职责：
 * <ul>
 *   <li>白名单路径直接放行</li>
 *   <li>从请求中提取 Token（委托 TokenResolver）</li>
 *   <li>调用 SessionManager.validateAndRefresh() 验证会话</li>
 *   <li>设置 IdentityContext（UserIdentity + Token）</li>
 *   <li>请求结束（finally）清理 IdentityContext</li>
 * </ul>
 */
@Component
public class SessionAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SessionAuthFilter.class);

    @Autowired
    private TokenResolver tokenResolver;
    @Autowired
    private WhiteListMatcher whiteListMatcher;
    @Autowired
    private SessionManager sessionManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String requestUri = request.getRequestURI();

        try {
            // 1. 白名单路径直接放行
            if (whiteListMatcher.isWhiteListed(requestUri)) {
                chain.doFilter(request, response);
                return;
            }

            // 2. 提取 Token
            String token = tokenResolver.resolve(request);
            if (token == null) {
                log.warn("Missing token for URI: {}", requestUri);
                writeUnauthorized(response, "缺少认证 Token");
                return;
            }

            // 3. 验证会话
            Session session = sessionManager.validateAndRefresh(token);
            if (session == null || session.getUserIdentity() == null) {
                log.warn("Invalid session for URI: {}", requestUri);
                writeUnauthorized(response, "会话无效或已过期");
                return;
            }

            // 4. 设置 IdentityContext
            UserIdentity userIdentity = JSON.parseObject(session.getUserIdentity(), UserIdentity.class);
            IdentityContext.set(userIdentity, token);

            // 5. 放行
            chain.doFilter(request, response);
        } finally {
            // 6. 清理 IdentityContext，防止 ThreadLocal 内存泄漏
            IdentityContext.clear();
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
