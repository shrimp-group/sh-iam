package com.wkclz.iam.session.filter;

import com.wkclz.core.identity.IdentityContext;
import com.wkclz.core.identity.UserIdentity;
import com.wkclz.iam.session.bean.ApiRequestControl;
import com.wkclz.iam.session.cache.ApiControlCache;
import com.wkclz.iam.session.config.IamSessionConfig;
import com.wkclz.iam.session.cache.ApiControlCache.ApiControlEntry;
import com.wkclz.iam.session.service.RequestControlResolver;
import com.wkclz.iam.session.service.SlidingWindowRateLimiter;
import com.wkclz.redis.helper.LockHolder;
import com.wkclz.redis.helper.RedisLock;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RequestControlFilter 单元测试 — 全局开关/身份/匹配/互斥/限流/释放锁分支。
 */
class RequestControlFilterTest {

    private IamSessionConfig config;
    private RequestControlResolver resolver;
    private ApiControlCache apiControlCache;
    private RedisLock redisLock;
    private SlidingWindowRateLimiter limiter;
    private RequestControlFilter filter;

    @BeforeEach
    void setUp() {
        config = new IamSessionConfig();
        config.setRequestControlEnabled(true);
        config.setRequestControlMutexTimeoutSeconds(30);
        config.setRequestControlRateLimitWindowSeconds(60);
        config.setRequestControlRateLimitMaxRequests(100);
        resolver = new RequestControlResolver(config);
        apiControlCache = mock(ApiControlCache.class);
        redisLock = mock(RedisLock.class);
        limiter = mock(SlidingWindowRateLimiter.class);
        filter = new RequestControlFilter(config, resolver, apiControlCache, redisLock, limiter);
    }

    @AfterEach
    void tearDown() {
        IdentityContext.clear();
    }

    private ApiControlEntry enabledEntry() {
        ApiRequestControl control = new ApiRequestControl();
        control.setEnable(true);
        ApiRequestControl.Mutex mutex = new ApiRequestControl.Mutex();
        mutex.setEnable(true);
        mutex.setTimeoutSeconds(30);
        control.setMutex(mutex);
        ApiRequestControl.RateLimit rateLimit = new ApiRequestControl.RateLimit();
        rateLimit.setEnable(true);
        rateLimit.setWindowSeconds(60);
        rateLimit.setMaxRequests(100);
        control.setRateLimit(rateLimit);
        return new ApiControlEntry("app", "api_1", "GET", "/api/test/**", control);
    }

    private void setIdentity() {
        UserIdentity identity = new UserIdentity();
        identity.setUserCode("user_1");
        IdentityContext.set(identity, "token");
    }

    @Test
    void globalDisabledPassesThrough() throws Exception {
        config.setRequestControlEnabled(false);
        HttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/1");
        HttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void noIdentityPassesThrough() throws Exception {
        HttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/1");
        HttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void noMatchPassesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        setIdentity();

        when(apiControlCache.match("GET", "/api/test/1")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void controlDisabledPassesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        setIdentity();

        ApiControlEntry entry = enabledEntry();
        entry.requestControl().setEnable(false);
        when(apiControlCache.match("GET", "/api/test/1")).thenReturn(entry);

        filter.doFilterInternal(request, response, chain);
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void mutexRejectedReturns429() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        setIdentity();

        when(apiControlCache.match("GET", "/api/test/1")).thenReturn(enabledEntry());
        when(redisLock.tryLockWithWatchdog(anyString(), anyLong(), any())).thenReturn(null);

        filter.doFilterInternal(request, response, chain);
        assertEquals(429, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void rateLimitRejectedReturns429() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        setIdentity();

        when(apiControlCache.match("GET", "/api/test/1")).thenReturn(enabledEntry());
        when(redisLock.tryLockWithWatchdog(anyString(), anyLong(), any()))
            .thenReturn(new LockHolder("k", "r"));
        when(limiter.tryAcquire(anyString(), anyLong(), anyLong())).thenReturn(false);

        filter.doFilterInternal(request, response, chain);
        assertEquals(429, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
        verify(redisLock, times(1)).releaseLock(any(LockHolder.class));
    }

    @Test
    void allPassThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        setIdentity();

        when(apiControlCache.match("GET", "/api/test/1")).thenReturn(enabledEntry());
        when(redisLock.tryLockWithWatchdog(anyString(), anyLong(), any()))
            .thenReturn(new LockHolder("k", "r"));
        when(limiter.tryAcquire(anyString(), anyLong(), anyLong())).thenReturn(true);

        filter.doFilterInternal(request, response, chain);
        verify(chain, times(1)).doFilter(request, response);
        verify(redisLock, times(1)).releaseLock(any(LockHolder.class));
    }

    @Test
    void chainExceptionReleasesMutexLock() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        setIdentity();

        when(apiControlCache.match("GET", "/api/test/1")).thenReturn(enabledEntry());
        when(redisLock.tryLockWithWatchdog(anyString(), anyLong(), any()))
            .thenReturn(new LockHolder("k", "r"));
        when(limiter.tryAcquire(anyString(), anyLong(), anyLong())).thenReturn(true);
        try {
            doThrow(new RuntimeException("boom")).when(chain).doFilter(any(), any());
        } catch (IOException | ServletException e) {
            throw new RuntimeException(e);
        }

        ServletException ex = assertThrows(ServletException.class,
            () -> filter.doFilterInternal(request, response, chain));
        assertTrue(ex.getCause() instanceof RuntimeException);
        verify(redisLock, times(1)).releaseLock(any(LockHolder.class));
    }
}
