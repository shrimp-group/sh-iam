package com.wkclz.iam.sso.contract;

import com.alibaba.fastjson2.JSON;
import com.wkclz.auth.bean.Principal;
import com.wkclz.auth.bean.Session;
import com.wkclz.auth.enums.AuthErrorType;
import com.wkclz.auth.exception.AuthException;
import com.wkclz.auth.exception.AuthenticationException;
import com.wkclz.auth.bean.AuthResult;
import com.wkclz.auth.context.SecurityContext;
import com.wkclz.iam.sso.contract.service.AuthContract;
import com.wkclz.core.exception.SystemException;
import com.wkclz.tool.tools.Md5Tool;
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

    @Autowired
    private JwtTokenService jwtTokenService;

    @Override
    public AuthResult authenticate(HttpServletRequest request) {
        String token = SecurityContext.getToken(request);
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return doAuthenticate(token);
    }

    @Override
    public AuthResult doAuthenticate(String token) {
        Principal principal;
        try {
            principal = jwtTokenService.parseToken(token);
        } catch (AuthenticationException e) {
            throw new AuthException(e.getErrorType(), e.getMessage(), e);
        } catch (RuntimeException e) {
            log.error("JWT 解析发生未知异常", e);
            throw new AuthException(AuthErrorType.TOKEN_INVALID, "Token 解析失败", e);
        }

        String username = principal.getUsername();
        String sessionKey = "iam:session:" + username + ":" + Md5Tool.md5(token);
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

        Principal result = session.getPrincipal();

        AuthResult authResult = new AuthResult();
        authResult.setPrincipal(result);
        authResult.setSession(session);
        return authResult;
    }
}
