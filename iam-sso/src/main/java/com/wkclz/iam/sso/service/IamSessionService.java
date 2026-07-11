package com.wkclz.iam.sso.service;

import com.alibaba.fastjson2.JSON;
import com.wkclz.auth.bean.Principal;
import com.wkclz.iam.sdk.contract.bean.Session;
import com.wkclz.iam.sdk.contract.config.ContractSettings;
import com.wkclz.iam.sdk.bean.UserJwt;
import com.wkclz.iam.sdk.util.JwtUtil;
import com.wkclz.tool.tools.Md5Tool;
import com.wkclz.iam.sso.config.IamSsoConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class IamSessionService {

    private static final Logger log = LoggerFactory.getLogger(IamSessionService.class);

    @Autowired
    private IamSsoConfig iamSsoConfig;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void createSession(String token, Principal principal, Session session) {
        String username = principal.getUsername();
        String tokenRedisKey = JwtUtil.getTokenRedisKey(token, username);
        redisTemplate.opsForValue().set(tokenRedisKey, JSON.toJSONString(session),
                JwtUtil.SESSION_TTL_SECONDS, TimeUnit.SECONDS);

        String sessionListKey = JwtUtil.getSessionListRedisKey(username);
        String tokenMd5 = Md5Tool.md5(token);
        redisTemplate.opsForZSet().add(sessionListKey, tokenMd5, System.currentTimeMillis());
        log.info("用户 {} 会话已创建, tokenMd5={}", username, tokenMd5);
    }

    public void enforceMaxConcurrentSessions(String username) {
        Integer max = iamSsoConfig.getMaxConcurrentSessions();
        if (max == null || max <= 0) return;

        String sessionListKey = JwtUtil.getSessionListRedisKey(username);
        Long count = redisTemplate.opsForZSet().size(sessionListKey);
        if (count == null || count <= max) return;

        Set<String> earliest = redisTemplate.opsForZSet().range(sessionListKey, 0, count - max - 1);
        if (earliest == null) return;

        for (String tokenMd5 : earliest) {
            String sessionKey = JwtUtil.getTokenRedisKeyByName(username, tokenMd5);
            redisTemplate.delete(sessionKey);
            redisTemplate.opsForZSet().remove(sessionListKey, tokenMd5);
            log.info("用户 {} 并发会话超限，踢出最早会话, tokenMd5={}", username, tokenMd5);
        }
    }

    public void logout(String token) {
        if (StringUtils.isBlank(token)) {
            log.warn("logout 传入 token 为空，跳过登出处理");
            return;
        }

        String username;
        try {
            UserJwt userJwt = JwtUtil.parseToken(token, ContractSettings.getJwtSecretKey());
            username = userJwt.getUsername();
        } catch (Exception e) {
            log.warn("logout 解析 JWT 失败，跳过登出处理: {}", e.getMessage());
            return;
        }

        log.info("用户 {} 开始本地登出", username);

        String tokenRedisKey = JwtUtil.getTokenRedisKey(token, username);
        Boolean deleted = redisTemplate.delete(tokenRedisKey);
        log.info("删除会话 key: {}, 结果: {}", tokenRedisKey, deleted);

        String sessionListKey = JwtUtil.getSessionListRedisKey(username);
        String tokenMd5 = Md5Tool.md5(token);
        Long removed = redisTemplate.opsForZSet().remove(sessionListKey, tokenMd5);
        log.info("用户 {} 从会话列表移除 token, 结果: {}", username, removed);
    }

    public void invalidateAllSessions(String username) {
        String sessionListKey = JwtUtil.getSessionListRedisKey(username);
        Set<String> tokenMd5Set = redisTemplate.opsForZSet().range(sessionListKey, 0, -1);
        if (tokenMd5Set != null && !tokenMd5Set.isEmpty()) {
            Collection<String> sessionKeys = new ArrayList<>(tokenMd5Set.size());
            for (String tokenMd5 : tokenMd5Set) {
                sessionKeys.add(JwtUtil.getTokenRedisKeyByName(username, tokenMd5));
            }
            Long deleted = redisTemplate.delete(sessionKeys);
            log.info("用户 {} 批量删除 {} 个会话 key，实际删除 {} 个", username, sessionKeys.size(), deleted);
        }
        redisTemplate.delete(sessionListKey);
        log.info("用户 {} 的所有会话已失效，共清理 {} 个会话", username,
                tokenMd5Set == null ? 0 : tokenMd5Set.size());
    }
}
