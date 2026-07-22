package com.wkclz.iam.session.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Token 提取器 — 从 HTTP 请求中提取认证 Token。
 *
 * <p>提取顺序：
 * <ol>
 *   <li>{@code Authorization: Bearer xxx} 头</li>
 *   <li>{@code token} 自定义头</li>
 * </ol>
 * 若未找到返回 null。
 */
@Component
public class TokenResolver {

    /**
     * 从请求中提取 Token，若未找到返回 null。
     */
    public String resolve(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        token = request.getHeader("token");
        if (StringUtils.hasText(token)) {
            return token;
        }
        return null;
    }
}
