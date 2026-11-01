package com.wkclz.iam.session.filter;

import com.alibaba.fastjson2.JSON;
import com.wkclz.core.identity.IdentityContext;
import com.wkclz.iam.session.config.IamSessionConfig;
import com.wkclz.iam.session.service.AuthPermissionService;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 接口权限判断过滤器 — 会话认证之后对接口执行纳管校验与用户接口权限判断（接入三级缓存的高频按 URL 路径）。
 *
 * <p>过滤器链顺序：RequestRecordFilter(LOWEST_PRECEDENCE - 10) → SessionAuthFilter(LOWEST_PRECEDENCE - 5)
 * → <b>AuthPermissionFilter(LOWEST_PRECEDENCE - 3)</b> → RequestControlFilter(LOWEST_PRECEDENCE)。</p>
 *
 * <p>放行条件（任一满足即放行，判断全部委托 {@link AuthPermissionService}）：全局开关关闭 / 白名单 /
 * 无身份（非白名单无会话已被 SessionAuthFilter 401）；appCode 必需（缺失 403）；
 * 开关开启后接口全量纳管（未纳管 403）；已纳管无权限 → 403。</p>
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 3)
public class AuthPermissionFilter extends OncePerRequestFilter {

    @Resource
    private IamSessionConfig config;
    @Resource
    private WhiteListMatcher whiteListMatcher;
    @Resource
    private AuthPermissionService authPermissionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        // 1. 全局开关关闭 → 放行
        if (!Boolean.TRUE.equals(config.getAuthCheckEnabled())) {
            chain.doFilter(request, response);
            return;
        }
        // 2. 白名单 → 放行
        if (whiteListMatcher.isWhiteListed(uri)) {
            chain.doFilter(request, response);
            return;
        }
        // 3. 无身份 → 放行（非白名单无会话已被 SessionAuthFilter 401）
        if (IdentityContext.get() == null) {
            chain.doFilter(request, response);
            return;
        }
        String userCode = IdentityContext.get().getUserCode();
        String appCode = IdentityContext.getAppCode();
        // 4. appCode 必需：缺失直接拒绝（无法定位应用静态资源）
        if (appCode == null || appCode.isEmpty()) {
            log.warn("接口权限拒绝（缺少appCode）: userCode={}, method={}, uri={}", userCode, method, uri);
            writeForbidden(response, "缺少应用标识(appCode)");
            return;
        }
        // 5. 开关开启后全量纳管：未纳管接口直接拒绝
        if (!authPermissionService.isManagedApi(appCode, method, uri)) {
            log.warn("接口权限拒绝（未纳管）: userCode={}, appCode={}, method={}, uri={}", userCode, appCode, method, uri);
            writeForbidden(response, "无接口访问权限");
            return;
        }
        // 6. 已纳管：判断用户接口权限
        if (authPermissionService.hasApiPermission(appCode, userCode, method, uri)) {
            chain.doFilter(request, response);
            return;
        }
        log.warn("接口权限拒绝: userCode={}, appCode={}, method={}, uri={}", userCode, appCode, method, uri);
        writeForbidden(response, "无接口访问权限");
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(JSON.toJSONString(new ErrorResponse(message)).getBytes(StandardCharsets.UTF_8));
    }

    private record ErrorResponse(String message) {
    }
}
