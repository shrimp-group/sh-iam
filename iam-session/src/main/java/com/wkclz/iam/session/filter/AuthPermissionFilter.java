package com.wkclz.iam.session.filter;

import com.alibaba.fastjson2.JSON;
import com.wkclz.core.identity.IdentityContext;
import com.wkclz.iam.session.cache.AuthCacheManager;
import com.wkclz.iam.session.config.IamSessionConfig;
import com.wkclz.iam.session.service.AuthPermissionService;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 接口权限判断过滤器 — 会话认证之后对接口执行纳管校验与用户接口权限判断（基于三级权限缓存）。
 *
 * <p>过滤器链顺序：RequestRecordFilter(LOWEST_PRECEDENCE - 10) → SessionAuthFilter(LOWEST_PRECEDENCE - 5)
 * → <b>AuthPermissionFilter(LOWEST_PRECEDENCE - 3)</b> → RequestControlFilter(LOWEST_PRECEDENCE)。</p>
 *
 * <p>鉴权基准为<b>服务归属资源键</b>（与调用方 header appCode 无关，跨应用调用安全）：
 * 配置 {@code iam.session.app-code}（独立部署）→ 按该应用资源与 auth_type；
 * 未配置（合并部署）→ 资源打平为全局（{@code __GLOBAL__}），鉴权深度按全局配置 {@code iam.auth.auth-type}。</p>
 *
 * <p>放行条件（任一满足即放行，判断全部委托 {@link AuthPermissionService}）：白名单 /
 * 无身份（非白名单无会话已被 SessionAuthFilter 401）/ 鉴权类型 TOKEN（仅需会话认证）；
 * TOKEN_API：接口全量纳管（未纳管 403）+ 已纳管无权限 403。</p>
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

        // 1. 白名单 → 放行
        if (whiteListMatcher.isWhiteListed(uri)) {
            chain.doFilter(request, response);
            return;
        }
        // 2. 无身份 → 放行（非白名单无会话已被 SessionAuthFilter 401）
        if (IdentityContext.get() == null) {
            chain.doFilter(request, response);
            return;
        }
        String userCode = IdentityContext.get().getUserCode();
        // 3. 鉴权资源键：独立部署取配置 app-code，合并部署取全局打平键
        String ownerKey = StringUtils.isNotBlank(config.getAppCode()) ? config.getAppCode() : AuthCacheManager.GLOBAL_KEY;
        // 4. 鉴权类型：TOKEN 仅需会话认证，跳过纳管与接口权限校验
        if (!authPermissionService.requireApiPermission(ownerKey)) {
            chain.doFilter(request, response);
            return;
        }
        // 5. 全量纳管：未纳管接口直接拒绝
        if (!authPermissionService.isManagedApi(ownerKey, method, uri)) {
            log.warn("接口权限拒绝（未纳管）: userCode={}, ownerKey={}, method={}, uri={}", userCode, ownerKey, method, uri);
            writeForbidden(response, "无接口访问权限");
            return;
        }
        // 6. 已纳管：判断用户接口权限
        if (authPermissionService.hasApiPermission(ownerKey, userCode, method, uri)) {
            chain.doFilter(request, response);
            return;
        }
        log.warn("接口权限拒绝: userCode={}, ownerKey={}, method={}, uri={}", userCode, ownerKey, method, uri);
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
