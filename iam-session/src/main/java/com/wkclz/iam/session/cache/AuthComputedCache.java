package com.wkclz.iam.session.cache;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wkclz.iam.session.config.IamSessionConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * L3 用户计算结果缓存 — 按 URL 布尔结果（true/false 均缓存，未命中触发单点计算后写回）。
 *
 * <p>三层嵌套结构，使联动清理天然精确：</p>
 * <ul>
 *   <li>外层：appCode → 用户层缓存（maximumSize=static-max-apps，无 TTL）</li>
 *   <li>中间层（每应用）：userCode → URL 层缓存（maximumSize=user-max-users +
 *       expireAfterAccess=user-idle-seconds，用户久不活跃整体过期）</li>
 *   <li>内层（每用户）：urlKey → Boolean（maximumSize=computed-max-urls +
 *       expireAfterWrite=computed-ttl-seconds，结果时效硬界）</li>
 * </ul>
 *
 * <p>清理入口：{@link #evictApp(String)} 供应用级静态变更整组清理；
 * {@link #evictUser(String, String)} 供用户级变更/会话终结在各应用内精确清理。</p>
 *
 * @author shrimp
 */
@Slf4j
@Component
public class AuthComputedCache {

    private final long ttlSeconds;
    private final long idleSeconds;
    private final int maxUsers;
    private final int maxUrls;
    private final Ticker ticker;

    /**
     * 外层：appCode → 中间层
     */
    private final Cache<String, Cache<String, Cache<String, Boolean>>> appCache;

    @Autowired
    public AuthComputedCache(IamSessionConfig config) {
        this(config.getAuthCacheComputedTtlSeconds(), config.getAuthCacheUserIdleSeconds(),
            config.getAuthCacheUserMaxUsers(), config.getAuthCacheComputedMaxUrls(),
            config.getAuthCacheStaticMaxApps(), Ticker.systemTicker());
    }

    /**
     * 测试用构造（可注入 Ticker 验证 TTL/空闲过期）。
     *
     * @param ttlSeconds  URL 级 expireAfterWrite（秒）
     * @param idleSeconds 用户级 expireAfterAccess（秒）
     * @param maxUsers    单应用最大用户数（中间层容量）
     * @param maxUrls     单用户最大 URL 条目数（内层容量）
     * @param maxApps     最大应用数（外层容量）
     * @param ticker      时间源
     */
    AuthComputedCache(long ttlSeconds, long idleSeconds, int maxUsers, int maxUrls, int maxApps, Ticker ticker) {
        this.ttlSeconds = ttlSeconds;
        this.idleSeconds = idleSeconds;
        this.maxUsers = maxUsers;
        this.maxUrls = maxUrls;
        this.ticker = ticker;
        this.appCache = CacheBuilder.newBuilder()
            .maximumSize(maxApps)
            .build();
    }

    /**
     * 读取 URL 级计算结果：未命中时执行 loader 单点计算并写回（true/false 均缓存）。
     *
     * @param appCode  应用编码
     * @param userCode 用户编码
     * @param urlKey   URL 键（METHOD:URI）
     * @param loader   计算回调
     * @return 计算结果
     */
    public Boolean get(String appCode, String userCode, String urlKey, Callable<Boolean> loader) {
        try {
            Cache<String, Cache<String, Boolean>> userCache = appCache.get(appCode, this::buildUserCache);
            Cache<String, Boolean> urlCache = userCache.get(userCode, this::buildUrlCache);
            return urlCache.get(urlKey, loader);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException("读取权限计算结果缓存失败: appCode=" + appCode
                + ", userCode=" + userCode + ", urlKey=" + urlKey + ", error=" + cause.getMessage(), cause);
        }
    }

    /**
     * 应用级整组清理（APP_STATIC 变更联动）。
     *
     * @param appCode 应用编码
     */
    public void evictApp(String appCode) {
        appCache.invalidate(appCode);
        logState("USER_COMPUTED", appCode, "LOADED→EVICTED", "CHANGE_CMD");
    }

    /**
     * 用户级精确清理（USER 变更 / 会话终结，遍历各应用中间层）。
     *
     * @param userCode 用户编码
     * @param reason   清理原因（CHANGE_CMD / SESSION_END）
     */
    public void evictUser(String userCode, String reason) {
        appCache.asMap().values().forEach(userCache -> userCache.invalidate(userCode));
        logState("USER_COMPUTED", userCode, "LOADED→EVICTED", reason);
    }

    private Cache<String, Cache<String, Boolean>> buildUserCache() {
        return CacheBuilder.newBuilder()
            .maximumSize(maxUsers)
            .expireAfterAccess(idleSeconds, TimeUnit.SECONDS)
            .ticker(ticker)
            .build();
    }

    private Cache<String, Boolean> buildUrlCache() {
        return CacheBuilder.newBuilder()
            .maximumSize(maxUrls)
            .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
            .ticker(ticker)
            .build();
    }

    private void logState(String type, String key, String transition, String reason) {
        log.info("[auth-cache] 状态变迁: type={}, key={}, {} reason={}", type, key, transition, reason);
    }
}
