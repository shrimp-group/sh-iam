package com.wkclz.iam.sso.service;

import com.wkclz.iam.sdk.bean.UserJwt;
import com.wkclz.iam.sdk.config.IamSdkConfig;
import com.wkclz.iam.sdk.util.JwtUtil;
import com.wkclz.tool.tools.Md5Tool;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class IamSessionService {

    private static final Logger log = LoggerFactory.getLogger(IamSessionService.class);


    @Autowired
    private IamSdkConfig iamSdkConfig;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void invalidateAllSessions(String username) {
        String sessionListKey = JwtUtil.getSessionListRedisKey(username);
        Set<String> tokenMd5Set = redisTemplate.opsForZSet().range(sessionListKey, 0, -1);
        if (tokenMd5Set != null && !tokenMd5Set.isEmpty()) {
            for (String tokenMd5 : tokenMd5Set) {
                String sessionKey = "iam:session:" + username + ":" + tokenMd5;
                redisTemplate.delete(sessionKey);
            }
        }
        redisTemplate.delete(sessionListKey);
        log.info("用户 {} 的所有会话已失效，共清理 {} 个会话", username, tokenMd5Set == null ? 0 : tokenMd5Set.size());
    }

    /**
     * 本地登出：根据 token 删除对应的 Redis 会话
     *
     * @param token JWT token
     */
    public void logout(String token) {
        if (StringUtils.isBlank(token)) {
            log.warn("logout 传入 token 为空，跳过登出处理");
            return;
        }

        String username;
        try {
            UserJwt userJwt = JwtUtil.parseToken(token, iamSdkConfig.getJwtSecretKey());
            username = userJwt.getUsername();
        } catch (Exception e) {
            log.warn("logout 解析 JWT 失败，跳过登出处理: {}", e.getMessage());
            return;
        }

        log.info("用户 {} 开始本地登出", username);

        // 删除会话 key
        String tokenRedisKey = JwtUtil.getTokenRedisKey(token, username);
        Boolean deleted = redisTemplate.delete(tokenRedisKey);
        log.info("删除会话 key: {}, 结果: {}", tokenRedisKey, deleted);

        // 从会话列表中移除该 token
        String sessionListKey = JwtUtil.getSessionListRedisKey(username);
        String tokenMd5 = Md5Tool.md5(token);
        Long removed = redisTemplate.opsForZSet().remove(sessionListKey, tokenMd5);
        log.info("用户 {} 从会话列表移除 token, 结果: {}", username, removed);
    }

}
