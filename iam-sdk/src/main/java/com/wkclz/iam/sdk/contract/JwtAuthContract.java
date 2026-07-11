package com.wkclz.iam.sdk.contract;

import com.alibaba.fastjson2.JSON;
import com.wkclz.auth.bean.Principal;
import com.wkclz.iam.contract.bean.AuthResult;
import com.wkclz.iam.contract.bean.Session;
import com.wkclz.iam.contract.config.ContractSettings;
import com.wkclz.iam.contract.context.PrincipalContext;
import com.wkclz.iam.contract.enums.AuthErrorType;
import com.wkclz.iam.contract.exception.AuthException;
import com.wkclz.iam.contract.service.AuthContract;
import com.wkclz.core.exception.SystemException;
import com.wkclz.iam.sdk.bean.UserJwt;
import com.wkclz.iam.sdk.exception.JwtValidationException;
import com.wkclz.iam.sdk.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class JwtAuthContract implements AuthContract {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public AuthResult authenticate(HttpServletRequest request) {
        String token = PrincipalContext.getToken();
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return doAuthenticate(token);
    }

    @Override
    public AuthResult doAuthenticate(String token) {
        UserJwt userJwt;
        try {
            userJwt = JwtUtil.parseToken(token, ContractSettings.getJwtSecretKey());
        } catch (JwtValidationException e) {
            throw new AuthException(
                    AuthErrorType.fromJwtErrorCode(e.getErrorCode()),
                    e.getMessage(), e);
        } catch (RuntimeException e) {
            log.error("JWT 解析发生未知异常", e);
            throw new AuthException(AuthErrorType.TOKEN_INVALID, "Token 解析失败", e);
        }

        String username = userJwt.getUsername();
        String sessionKey = JwtUtil.getTokenRedisKey(token, username);
        String sessionJson;
        try {
            sessionJson = redisTemplate.opsForValue().get(sessionKey);
        } catch (Exception e) {
            log.error("认证 Redis 查询失败, username={}", username, e);
            throw new SystemException("认证 Redis 查询失败", e);
        }
        if (sessionJson == null) {
            log.info("用户 {} 的会话已过期", username);
            throw new AuthException(AuthErrorType.SESSION_EXPIRED, "会话已过期");
        }

        Session session;
        try {
            session = JSON.parseObject(sessionJson, Session.class);
        } catch (Exception e) {
            log.error("Session 反序列化失败, username={}", username, e);
            throw new AuthException(AuthErrorType.TOKEN_INVALID, "会话数据损坏", e);
        }

        Principal result = new Principal();
        result.setUserCode(userJwt.getUserCode());
        result.setUsername(username);
        result.setNickname(userJwt.getNickname());
        result.setAvatar(userJwt.getAvatar());
        result.setAuthIdentifier(session.getAuthIdentifier());

        AuthResult authResult = new AuthResult();
        authResult.setPrincipal(result);
        authResult.setSession(session);
        return authResult;
    }
}
