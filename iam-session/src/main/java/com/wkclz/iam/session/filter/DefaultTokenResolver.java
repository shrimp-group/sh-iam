package com.wkclz.iam.session.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 默认 Token 提取实现 — 优先取 {@code Authorization: Bearer xxx}，其次取 {@code token} 自定义头。
 */
@Component
@ConditionalOnMissingBean(TokenResolver.class)
public class DefaultTokenResolver implements TokenResolver {

    @Override
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
