package com.wkclz.iam.contract.defaults.filter;

import com.wkclz.iam.contract.bean.AuthResult;
import com.wkclz.auth.context.SecurityContext;
import com.wkclz.iam.contract.defaults.config.ContractConfig;
import com.wkclz.iam.contract.enums.AuthErrorType;
import com.wkclz.iam.contract.exception.AuthException;
import com.wkclz.iam.contract.service.AuthContract;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 默认鉴权过滤器
 * 调用 AuthContract SPI 完成认证，认证失败时根据 AuthErrorType.httpStatus 返回对应状态码
 *
 * 流程：
 * 1. 根路径拒绝
 * 2. public 路径放行（可配置 publicPathPattern）
 * 3. 调用 AuthContract.authenticate()
 * 4. 认证成功 → 缓存 PrincipalContext → 放行
 * 5. 认证失败 → 返回 AuthErrorType 对应的 HTTP 状态码
 *
 * @author shrimp
 */
@Slf4j
public class DefaultAuthFilter extends OncePerRequestFilter {

    @Autowired
    private AuthContract authContract;

    @Autowired
    private ContractConfig contractConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // 1. 根路径拒绝
        if ("/".equals(uri)) {
            log.debug("DefaultAuthFilter: 根路径拒绝");
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return;
        }

        // 2. public 路径放行
        String pattern = contractConfig.getPublicPathPattern();
        if (SecurityContext.match(pattern, uri)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // 3. 调用 AuthContract SPI 认证
            AuthResult authResult = authContract.authenticate(request);

            if (authResult == null) {
                // 无 token → 拒绝（public 已放行，走到这里说明需要认证）
                log.warn("DefaultAuthFilter: token 不存在，uri={}", uri);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return;
            }

            // 4. 缓存到上下文
            request.setAttribute("PRINCIPAL", authResult.getPrincipal());
            SecurityContext.setPrincipal(authResult.getPrincipal());
            request.setAttribute("SESSION", authResult.getSession());

            // 5. 放行
            chain.doFilter(request, response);
        } catch (AuthException e) {
            log.warn("DefaultAuthFilter: 认证失败: {} - {}, uri={}", e.getErrorType(), e.getMessage(), uri);
            // ACCESS_DENIED → 403，其余认证错误 → 401
            response.setStatus(e.getErrorType() == AuthErrorType.ACCESS_DENIED ? 403 : 401);
        } finally {
            SecurityContext.clear();
            request.removeAttribute("PRINCIPAL");
            request.removeAttribute("SESSION");
        }
    }
}
