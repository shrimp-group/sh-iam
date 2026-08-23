package com.wkclz.iam.session.filter;

import com.alibaba.fastjson2.JSON;
import com.wkclz.core.base.R;
import com.wkclz.core.identity.IdentityContext;
import com.wkclz.iam.session.bean.ApiRequestControl;
import com.wkclz.iam.session.cache.ApiControlCache;
import com.wkclz.iam.session.config.IamSessionConfig;
import com.wkclz.iam.session.service.RequestControlResolver;
import com.wkclz.iam.session.service.SlidingWindowRateLimiter;
import com.wkclz.redis.helper.LockHolder;
import com.wkclz.redis.helper.RedisLock;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 请求控制过滤器
 * <p>
 * 基于登录用户（IdentityContext）对已配置请求控制的 API 执行互斥（RedisLock，watchdog 自动续期）与
 * 滑动窗口限流（SlidingWindowRateLimiter）双重控制；任一控制拒绝则返回 429。
 *
 * <p>执行流程：全局开关 → 用户身份 → 配置匹配 → 配置解析 → 互斥控制 → 限流控制 → 放行 → 释放互斥锁。</p>
 *
 * <p>执行位置：@Order(Ordered.LOWEST_PRECEDENCE)（= MAX，注意不能 +1 否则溢出为 Integer.MIN_VALUE 排到链最外层），
 * 位于 SessionAuthFilter（LOWEST_PRECEDENCE - 5）之后，处于过滤器链最内层，执行时身份已由 SessionAuthFilter 设置。</p>
 *
 * @author shrimp
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class RequestControlFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestControlFilter.class);

    /**
     * 互斥锁 Redis Key 前缀
     */
    private static final String MUTEX_KEY_PREFIX = "iam:req:mutex:";

    /**
     * 限流 Redis Key 前缀
     */
    private static final String RATE_LIMIT_KEY_PREFIX = "iam:req:rl:";

    /**
     * 429 响应文案：互斥拒绝
     */
    private static final String MSG_MUTEX_REJECTED = "请求处理中，请稍后重试";

    /**
     * 429 响应文案：限流拒绝
     */
    private static final String MSG_RATE_LIMIT_REJECTED = "请求过于频繁，请稍后重试";

    private final IamSessionConfig config;
    private final RequestControlResolver resolver;
    private final ApiControlCache apiControlCache;
    private final RedisLock redisLock;
    private final SlidingWindowRateLimiter rateLimiter;

    public RequestControlFilter(IamSessionConfig config, RequestControlResolver resolver,
                                ApiControlCache apiControlCache, RedisLock redisLock,
                                SlidingWindowRateLimiter rateLimiter) {
        this.config = config;
        this.resolver = resolver;
        this.apiControlCache = apiControlCache;
        this.redisLock = redisLock;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        log.info("请求控制过滤器开始处理: method={}, uri={}", request.getMethod(), request.getRequestURI());

        // 1. 全局开关关闭 → 直接放行
        if (!Boolean.TRUE.equals(config.getRequestControlEnabled())) {
            log.info("请求控制全局开关关闭，直接放行: uri={}", request.getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        // 2. 无用户身份 → 放行（仅对已登录用户生效）
        String userCode = IdentityContext.getUserCode();
        if (StringUtils.isBlank(userCode)) {
            log.info("请求无用户身份，直接放行: uri={}", request.getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        // 3. 未匹配到请求控制 API → 放行
        ApiControlCache.ApiControlEntry entry = apiControlCache.match(request.getMethod(), request.getRequestURI());
        if (entry == null) {
            log.info("请求未匹配到请求控制 API，直接放行: method={}, uri={}", request.getMethod(), request.getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        // 4. 解析 API 请求控制配置，总开关未开启 → 放行
        ApiRequestControl control = resolver.resolve(entry.requestControl());
        if (control == null) {
            log.info("API 请求控制未开启，直接放行: apiCode={}", entry.apiCode());
            chain.doFilter(request, response);
            return;
        }
        log.info("请求匹配到请求控制配置: method={}, uri={}, apiCode={}, userCode={}",
            request.getMethod(), request.getRequestURI(), entry.apiCode(), userCode);

        LockHolder lockHolder = null;
        try {
            // 5. 互斥控制：获取 Redis 锁（watchdog 自动续期），失败 → 429
            if (Boolean.TRUE.equals(control.getMutex().getEnable())) {
                String mutexKey = MUTEX_KEY_PREFIX + entry.apiCode() + ":" + userCode;
                Integer timeoutSeconds = control.getMutex().getTimeoutSeconds();
                try {
                    lockHolder = redisLock.tryLockWithWatchdog(mutexKey, timeoutSeconds, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("获取互斥锁异常: key={}, error={}", mutexKey, e.getMessage(), e);
                }
                // 互斥为 fail-closed 有意设计：锁获取失败（含 Redis 故障时异常被捕获、holder 降级为 null）一律视为冲突拒绝（429），
                // 保证互斥语义不被外部依赖故障破坏；与限流 fail-open 策略（仅超限拒绝、Redis 异常时放行）不同
                if (lockHolder == null) {
                    log.info("互斥控制拒绝请求: key={}", mutexKey);
                    writeTooManyRequests(response, MSG_MUTEX_REJECTED);
                    return;
                }
            }

            // 6. 限流控制：滑动窗口限流，超限 → 429
            if (Boolean.TRUE.equals(control.getRateLimit().getEnable())) {
                String rateLimitKey = RATE_LIMIT_KEY_PREFIX + entry.apiCode() + ":" + userCode;
                boolean allowed = rateLimiter.tryAcquire(rateLimitKey,
                    control.getRateLimit().getWindowSeconds(), control.getRateLimit().getMaxRequests());
                if (!allowed) {
                    log.info("限流控制拒绝请求: key={}", rateLimitKey);
                    writeTooManyRequests(response, MSG_RATE_LIMIT_REJECTED);
                    return;
                }
            }

            // 7. 放行
            chain.doFilter(request, response);
        } catch (IOException | ServletException e) {
            log.error("请求控制过滤器执行异常: uri={}, error={}", request.getRequestURI(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("请求控制过滤器执行异常: uri={}, error={}", request.getRequestURI(), e.getMessage(), e);
            throw new ServletException("请求控制过滤器执行异常", e);
        } finally {
            // 8. 释放互斥锁（若持有）
            if (lockHolder != null) {
                try {
                    redisLock.releaseLock(lockHolder);
                } catch (Exception e) {
                    log.error("释放互斥锁异常: key={}, error={}", lockHolder.getLockKey(), e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 输出 429 Too Many Requests 响应（JSON 格式）
     *
     * @param response HTTP 响应
     * @param message  提示文案
     */
    private void writeTooManyRequests(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(R.error(429, message)));
    }
}
