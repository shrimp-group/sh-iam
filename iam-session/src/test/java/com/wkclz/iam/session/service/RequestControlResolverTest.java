package com.wkclz.iam.session.service;

import com.wkclz.iam.session.bean.ApiRequestControl;
import com.wkclz.iam.session.config.IamSessionConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RequestControlResolver 单元测试 — 默认值回落与总开关语义。
 */
class RequestControlResolverTest {

    private RequestControlResolver resolver;

    @BeforeEach
    void setUp() {
        IamSessionConfig config = new IamSessionConfig();
        config.setRequestControlEnabled(true);
        config.setRequestControlMutexTimeoutSeconds(30);
        config.setRequestControlRateLimitWindowSeconds(60);
        config.setRequestControlRateLimitMaxRequests(100);
        resolver = new RequestControlResolver(config);
    }

    @Test
    void resolveNullReturnsNull() {
        assertNull(resolver.resolve(null));
    }

    @Test
    void resolveEnableFalseReturnsNull() {
        ApiRequestControl raw = new ApiRequestControl();
        raw.setEnable(false);
        assertNull(resolver.resolve(raw));
    }

    @Test
    void resolveEnableNotSetReturnsNull() {
        assertNull(resolver.resolve(new ApiRequestControl()));
    }

    @Test
    void resolveCreatesSubObjectsWhenMissing() {
        ApiRequestControl raw = new ApiRequestControl();
        raw.setEnable(true);

        ApiRequestControl resolved = resolver.resolve(raw);
        assertNotNull(resolved);
        assertTrue(resolved.getEnable());
        // 子对象缺失时仍创建非 null 子对象，enable 透传为 null
        assertNotNull(resolved.getMutex());
        assertNull(resolved.getMutex().getEnable());
        assertEquals(30, resolved.getMutex().getTimeoutSeconds());
        assertNotNull(resolved.getRateLimit());
        assertNull(resolved.getRateLimit().getEnable());
        assertEquals(60, resolved.getRateLimit().getWindowSeconds());
        assertEquals(100, resolved.getRateLimit().getMaxRequests());
    }

    @Test
    void resolveFillsDefaultsWhenSubFieldsEmpty() {
        ApiRequestControl raw = new ApiRequestControl();
        raw.setEnable(true);
        ApiRequestControl.Mutex mutex = new ApiRequestControl.Mutex();
        mutex.setEnable(false);
        raw.setMutex(mutex);
        ApiRequestControl.RateLimit rateLimit = new ApiRequestControl.RateLimit();
        rateLimit.setEnable(false);
        raw.setRateLimit(rateLimit);

        ApiRequestControl resolved = resolver.resolve(raw);
        assertNotNull(resolved);
        assertFalse(resolved.getMutex().getEnable());
        assertEquals(30, resolved.getMutex().getTimeoutSeconds());
        assertFalse(resolved.getRateLimit().getEnable());
        assertEquals(60, resolved.getRateLimit().getWindowSeconds());
        assertEquals(100, resolved.getRateLimit().getMaxRequests());
    }

    @Test
    void resolveKeepsCustomValues() {
        ApiRequestControl raw = new ApiRequestControl();
        raw.setEnable(true);
        ApiRequestControl.Mutex mutex = new ApiRequestControl.Mutex();
        mutex.setEnable(true);
        mutex.setTimeoutSeconds(10);
        raw.setMutex(mutex);
        ApiRequestControl.RateLimit rateLimit = new ApiRequestControl.RateLimit();
        rateLimit.setEnable(true);
        rateLimit.setWindowSeconds(5);
        rateLimit.setMaxRequests(3);
        raw.setRateLimit(rateLimit);

        ApiRequestControl resolved = resolver.resolve(raw);
        assertTrue(resolved.getMutex().getEnable());
        assertEquals(10, resolved.getMutex().getTimeoutSeconds());
        assertTrue(resolved.getRateLimit().getEnable());
        assertEquals(5, resolved.getRateLimit().getWindowSeconds());
        assertEquals(3, resolved.getRateLimit().getMaxRequests());
    }
}
