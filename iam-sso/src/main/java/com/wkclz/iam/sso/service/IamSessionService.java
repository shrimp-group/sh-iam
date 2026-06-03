package com.wkclz.iam.sso.service;

import com.wkclz.iam.sdk.util.JwtUtil;
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

}
