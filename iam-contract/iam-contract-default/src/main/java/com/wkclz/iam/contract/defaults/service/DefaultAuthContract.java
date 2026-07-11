package com.wkclz.iam.contract.defaults.service;

import com.wkclz.iam.contract.bean.AuthResult;
import com.wkclz.iam.contract.context.PrincipalContext;
import com.wkclz.auth.enums.AuthErrorType;
import com.wkclz.auth.exception.AuthException;
import com.wkclz.iam.contract.service.AuthContract;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 认证契约默认实现
 * 读宽容：无 token 返回 null（过滤器放行 public 路径）
 * 验证严格：有 token 但无实现则拒绝
 *
 * @author shrimp
 */
@Slf4j
public class DefaultAuthContract implements AuthContract {

    @Override
    public AuthResult authenticate(HttpServletRequest request) {
        String token = PrincipalContext.getToken();
        if (token == null) {
            // 无 token：过滤器据此放行 public 路径
            return null;
        }
        // 有 token 但无实现 → 严格拒绝
        log.warn("DefaultAuthContract: token 存在但无 AuthContract 实现，拒绝访问");
        throw new AuthException(AuthErrorType.TOKEN_INVALID,
                "无认证实现，请配置 AuthContract");
    }

    @Override
    public AuthResult doAuthenticate(String token) {
        log.warn("DefaultAuthContract: doAuthenticate 被调用但无实现");
        throw new AuthException(AuthErrorType.TOKEN_MISSING,
                "无认证实现，请配置 AuthContract");
    }
}
