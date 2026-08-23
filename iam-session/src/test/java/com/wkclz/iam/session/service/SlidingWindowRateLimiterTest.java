package com.wkclz.iam.session.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SlidingWindowRateLimiter 单元测试 — Lua 执行结果映射与参数传递。
 */
class SlidingWindowRateLimiterTest {

    @Test
    void tryAcquireWhenRedisReturnsOne_shouldAllow() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(redisTemplate);
        when(redisTemplate.execute(any(), anyList(), any(Object[].class))).thenReturn(1L);

        boolean allowed = limiter.tryAcquire("iam:req:rl:api_1:user_1", 60, 100);
        assertTrue(allowed);
    }

    @Test
    void tryAcquireWhenRedisReturnsZero_shouldDeny() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(redisTemplate);
        when(redisTemplate.execute(any(), anyList(), any(Object[].class))).thenReturn(0L);

        boolean allowed = limiter.tryAcquire("iam:req:rl:api_1:user_1", 60, 100);
        assertFalse(allowed);
    }

    @Test
    void tryAcquireWhenRedisError_shouldAllowByFallback() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(redisTemplate);
        when(redisTemplate.execute(any(), anyList(), any(Object[].class)))
            .thenThrow(new RuntimeException("redis down"));

        boolean allowed = limiter.tryAcquire("iam:req:rl:api_1:user_1", 60, 100);
        assertTrue(allowed);
    }

    @Test
    void tryAcquireWhenRedisReturnsNull_shouldAllowByFallback() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(redisTemplate);
        when(redisTemplate.execute(any(), anyList(), any(Object[].class))).thenReturn(null);

        boolean allowed = limiter.tryAcquire("iam:req:rl:api_1:user_1", 60, 100);
        assertTrue(allowed);
    }

    @Test
    void tryAcquirePassesCorrectParamsToExecute() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(redisTemplate);
        when(redisTemplate.execute(any(), anyList(), any(Object[].class))).thenReturn(1L);

        limiter.tryAcquire("iam:req:rl:api_1:user_1", 60, 100);

        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(redisTemplate).execute(any(RedisScript.class), keysCaptor.capture(), argsCaptor.capture());

        assertEquals("iam:req:rl:api_1:user_1", keysCaptor.getValue().get(0));
        Object[] args = argsCaptor.getValue();
        assertEquals(5, args.length);
        // now 为毫秒时间戳（首个参数，无法断言固定值）
        // windowSeconds*1000 毫秒
        assertEquals("60000", args[1]);
        // maxRequests
        assertEquals("100", args[2]);
        // windowSeconds 秒
        assertEquals("60", args[4]);
    }

    @Test
    void tryAcquireWhenInvalidParams_shouldAllowByFallback() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(redisTemplate);

        assertTrue(limiter.tryAcquire("k", 0, 100));
        assertTrue(limiter.tryAcquire("k", 60, 0));
    }
}
