package com.wkclz.iam.session.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

/**
 * 基于 Redis ZSet 的滑动窗口限流（Lua 脚本原子操作）。
 */
@Component
public class SlidingWindowRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(SlidingWindowRateLimiter.class);

    /**
     * Lua 脚本：清理窗口外过期成员 → 判断是否超限 → 未超限则写入当前请求并刷新过期时间。
     * 说明：now 与 windowMs 均为毫秒（ZREMRANGEBYSCORE 边界一致）；windowSec 为秒（EXPIRE 单位是秒）。
     */
    private static final String SCRIPT = """
        local key = KEYS[1]
        local now = tonumber(ARGV[1])
        local windowMs = tonumber(ARGV[2])
        local max = tonumber(ARGV[3])
        local member = ARGV[4]
        local windowSec = tonumber(ARGV[5])
        redis.call('ZREMRANGEBYSCORE', key, 0, now - windowMs)
        if redis.call('ZCARD', key) >= max then
          return 0
        end
        redis.call('ZADD', key, now, member)
        redis.call('EXPIRE', key, windowSec)
        return 1
        """;

    private final DefaultRedisScript<Long> script;
    private final StringRedisTemplate stringRedisTemplate;

    public SlidingWindowRateLimiter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.script = new DefaultRedisScript<>();
        this.script.setScriptText(SCRIPT);
        this.script.setResultType(Long.class);
    }

    /**
     * 尝试获取一次访问额度。
     *
     * @param key           限流键（含 apiCode + userCode）
     * @param windowSeconds 窗口时长（秒）
     * @param maxRequests   窗口内最大请求数
     * @return true-放行；false-超限拒绝；Redis 异常时放行（fail-open）
     */
    public boolean tryAcquire(String key, long windowSeconds, long maxRequests) {
        if (windowSeconds <= 0 || maxRequests <= 0) {
            log.warn("Rate limit invalid params, allow by fallback: key={}, windowSeconds={}, maxRequests={}", key, windowSeconds, maxRequests);
            return true;
        }
        log.info("Rate limit tryAcquire: key={}, windowSeconds={}, maxRequests={}", key, windowSeconds, maxRequests);
        long now = System.currentTimeMillis();
        String member = now + "-" + UUID.randomUUID().toString().substring(0, 8);
        try {
            Long result = stringRedisTemplate.execute(script, Collections.singletonList(key),
                String.valueOf(now), String.valueOf(windowSeconds * 1000), String.valueOf(maxRequests), member, String.valueOf(windowSeconds));
            if (result == null) {
                log.warn("Rate limit script returned null, allow by fallback: {}", key);
                return true;
            }
            return result > 0;
        } catch (Exception e) {
            log.error("Rate limit check error, allow by fallback: key={}, error={}", key, e.getMessage());
            return true;
        }
    }
}
