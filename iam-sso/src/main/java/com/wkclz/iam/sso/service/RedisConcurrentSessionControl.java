package com.wkclz.iam.sso.service;

import com.wkclz.auth.contract.auth.ConcurrentSessionControl;
import com.wkclz.iam.sso.config.IamSsoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class RedisConcurrentSessionControl implements ConcurrentSessionControl {

    private static final Logger log = LoggerFactory.getLogger(RedisConcurrentSessionControl.class);

    private static final String SESSION_KEY_PREFIX = "sh-auth:session:";
    private static final String SESSION_LIST_KEY_PREFIX = "sh-auth:session:list:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private IamSsoConfig iamSsoConfig;

    @Override
    public void enforce(String username) {
        Integer max = iamSsoConfig.getMaxConcurrentSessions();
        if (max == null || max <= 0) {
            return;
        }

        String listKey = SESSION_LIST_KEY_PREFIX + username;
        Long count = redisTemplate.opsForZSet().size(listKey);
        if (count == null || count <= max) {
            return;
        }

        Set<String> earliest = redisTemplate.opsForZSet().range(listKey, 0, count - max - 1);
        if (earliest == null || earliest.isEmpty()) {
            return;
        }

        for (String tokenMd5 : earliest) {
            String sessionKey = SESSION_KEY_PREFIX + tokenMd5;
            redisTemplate.delete(sessionKey);
            redisTemplate.opsForZSet().remove(listKey, tokenMd5);
            log.info("用户 {} 并发会话超限，踢出最早会话, tokenMd5={}", username, tokenMd5);
        }
    }

    @Override
    public int getCurrentCount(String subjectId) {
        String listKey = SESSION_LIST_KEY_PREFIX + subjectId;
        Long count = redisTemplate.opsForZSet().size(listKey);
        return count == null ? 0 : count.intValue();
    }

    @Override
    public int getMaxSessions() {
        Integer max = iamSsoConfig.getMaxConcurrentSessions();
        return max == null || max <= 0 ? Integer.MAX_VALUE : max;
    }
}
