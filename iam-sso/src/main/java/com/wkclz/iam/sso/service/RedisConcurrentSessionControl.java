package com.wkclz.iam.sso.service;

import com.wkclz.auth.contract.auth.ConcurrentSessionControl;
import com.wkclz.iam.sdk.util.JwtUtil;
import com.wkclz.iam.sso.config.IamSsoConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisConcurrentSessionControl implements ConcurrentSessionControl {

    @Autowired
    private IamSessionService iamSessionService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private IamSsoConfig iamSsoConfig;

    @Override
    public void enforce(String subjectId) {
        iamSessionService.enforceMaxConcurrentSessions(subjectId);
    }

    @Override
    public int getCurrentCount(String subjectId) {
        String listKey = JwtUtil.getSessionListRedisKey(subjectId);
        Long count = redisTemplate.opsForZSet().size(listKey);
        return count == null ? 0 : count.intValue();
    }

    @Override
    public int getMaxSessions() {
        Integer max = iamSsoConfig.getMaxConcurrentSessions();
        return max == null || max <= 0 ? Integer.MAX_VALUE : max;
    }
}
