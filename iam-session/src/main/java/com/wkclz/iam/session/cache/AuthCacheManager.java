package com.wkclz.iam.session.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wkclz.iam.session.bean.auth.AppAuthResource;
import com.wkclz.iam.session.bean.auth.PermissionChangeCommand;
import com.wkclz.iam.session.config.IamSessionConfig;
import com.wkclz.iam.session.spi.AuthResourceFacade;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 权限缓存管理器与状态枢纽 — L1/L2 直持 + L3 委托 {@link AuthComputedCache}，
 * 指令去重与联动清理的唯一入口（订阅 Task 11 与轮询 Task 19 共用，幂等）。
 *
 * <p><b>权威状态映射表</b>（问题排查以此与 [auth-cache] 日志为准）：</p>
 *
 * <table border="1">
 *   <tr><th>层</th><th>类型</th><th>键</th><th>加载</th><th>失效</th></tr>
 *   <tr><td>L1</td><td>APP_STATIC</td><td>appCode</td><td>首次计算按需（facade.loadAppResource，一次加载一个应用全量）</td>
 *       <td>APP_STATIC 指令精确清理 + 容量 LRU，无 TTL（远程形态由 Task 19 追加 TTL 硬界）</td></tr>
 *   <tr><td>L2</td><td>USER_ROLE</td><td>userCode</td><td>首次触发该用户权限计算（facade.loadUserRoleCodes）</td>
 *       <td>USER 指令 / 会话终结（evictUser reason=SESSION_END）+ 空闲 TTL（expireAfterAccess）+ 容量 LRU</td></tr>
 *   <tr><td>L3</td><td>USER_COMPUTED</td><td>appCode→userCode→urlKey</td><td>首次判断该 URL 按需单点计算</td>
 *       <td>URL 级 TTL（expireAfterWrite）+ 用户级空闲 TTL + 指令/会话终结联动清理 + 容量 LRU</td></tr>
 * </table>
 *
 * <p><b>联动清理映射</b>：</p>
 * <ul>
 *   <li>APP_STATIC(appCode) → L1[appCode] + L3 整组[appCode]（动态计算受静态影响，一同清理）</li>
 *   <li>USER(userCode) → L2[userCode] + L3 各应用内[userCode]（经 {@link #evictUser}，USER 指令与会话终结共用）</li>
 * </ul>
 *
 * <p><b>状态流转</b>：NOT_LOADED →(ON_DEMAND) LOADED →(CHANGE_CMD | SESSION_END | TTL | LRU) EVICTED(=NOT_LOADED)，条目状态由 Guava 存在性编码；<br>
 * <b>指令流转</b>：PUBLISHED → RECEIVED → APPLIED | IGNORED_STALE（时间戳去重：
 * {@code lastAppliedTs} 记录 type:targetKey 最后已处理时间戳，仅处理更新的指令，重复/乱序旧指令忽略——逐出幂等，重复无害）。</p>
 *
 * @author shrimp
 */
@Slf4j
@Component
public class AuthCacheManager {

    private final AuthResourceFacade facade;
    private final AuthComputedCache computedCache;

    /**
     * L1 应用静态资源缓存（无 TTL，仅指令清理 + 容量 LRU）
     */
    private final Cache<String, AppAuthResource> staticCache;

    /**
     * L2 用户角色缓存（空闲 TTL + 容量 LRU）
     */
    private final Cache<String, List<String>> userRoleCache;

    /**
     * 指令时间戳去重表：type:targetKey → 最后已处理时间戳
     */
    private final ConcurrentHashMap<String, Long> lastAppliedTs = new ConcurrentHashMap<>();

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthCacheManager(AuthResourceFacade facade, AuthComputedCache computedCache, IamSessionConfig config) {
        this.facade = facade;
        this.computedCache = computedCache;
        this.staticCache = buildStaticCache(config);
        this.userRoleCache = CacheBuilder.newBuilder()
            .maximumSize(config.getAuthCacheUserMaxUsers())
            .expireAfterAccess(config.getAuthCacheUserIdleSeconds(), TimeUnit.SECONDS)
            .build();
    }

    /**
     * L1 构建工厂（远程形态 TTL 由 Task 19 增补扩展点）
     */
    private Cache<String, AppAuthResource> buildStaticCache(IamSessionConfig config) {
        return CacheBuilder.newBuilder()
            .maximumSize(config.getAuthCacheStaticMaxApps())
            .build();
    }

    /**
     * 获取应用静态资源（L1，未命中经 facade 按需加载一次）。
     *
     * @param appCode 应用编码
     * @return 应用静态资源
     */
    public AppAuthResource getAppResource(String appCode) {
        try {
            return staticCache.get(appCode, () -> {
                AppAuthResource resource = facade.loadAppResource(appCode);
                logState("APP_STATIC", appCode, "NOT_LOADED→LOADED", "ON_DEMAND");
                return resource;
            });
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException("加载应用静态资源缓存失败: appCode=" + appCode + ", error=" + cause.getMessage(), cause);
        }
    }

    /**
     * 获取用户全部有效角色（L2，未命中经 facade 按需加载一次）。
     *
     * @param userCode 用户编码
     * @return 角色编码列表
     */
    public List<String> getUserRoleCodes(String userCode) {
        try {
            return userRoleCache.get(userCode, () -> {
                List<String> roleCodes = facade.loadUserRoleCodes(userCode);
                logState("USER_ROLE", userCode, "NOT_LOADED→LOADED", "ON_DEMAND");
                return roleCodes;
            });
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException("加载用户角色缓存失败: userCode=" + userCode + ", error=" + cause.getMessage(), cause);
        }
    }

    /**
     * URL 级权限判断（L3，true/false 均缓存，未命中执行 computer 单点计算后写回）。
     *
     * @param appCode  应用编码
     * @param userCode 用户编码
     * @param urlKey   URL 键（METHOD:URI）
     * @param computer 单点计算回调
     * @return 是否有权限
     */
    public boolean hasUrlPermission(String appCode, String userCode, String urlKey, Supplier<Boolean> computer) {
        return Boolean.TRUE.equals(computedCache.get(appCode, userCode, urlKey, computer::get));
    }

    /**
     * 应用权限变更指令（订阅与轮询共用入口，幂等）。
     *
     * <p>时间戳去重：同 type:targetKey 仅处理更新的指令；按类型联动清理
     * （APP_STATIC → L1+L3 整组；USER → L2+L3 各应用内）。</p>
     *
     * @param cmd 变更指令
     */
    public void applyCommand(PermissionChangeCommand cmd) {
        if (cmd == null || cmd.getType() == null || StringUtils.isBlank(cmd.getTargetKey())) {
            log.warn("[auth-cache] 非法变更指令，忽略: cmd={}", cmd);
            return;
        }
        String key = cmd.getType() + ":" + cmd.getTargetKey();
        Long last = lastAppliedTs.get(key);
        if (last != null && cmd.getTimestamp() <= last) {
            log.info("[auth-cache] 指令忽略 IGNORED_STALE: key={}, cmdTs={}, lastTs={}", key, cmd.getTimestamp(), last);
            return;
        }
        lastAppliedTs.put(key, cmd.getTimestamp());
        switch (cmd.getType()) {
            case APP_STATIC -> {
                staticCache.invalidate(cmd.getTargetKey());
                computedCache.evictApp(cmd.getTargetKey());
                logState("APP_STATIC", cmd.getTargetKey(), "LOADED→EVICTED", "CHANGE_CMD");
            }
            case USER -> evictUser(cmd.getTargetKey(), "CHANGE_CMD");
        }
        log.info("[auth-cache] 指令应用 APPLIED: type={}, key={}, ts={}", cmd.getType(), cmd.getTargetKey(), cmd.getTimestamp());
    }

    /**
     * 用户级清理（USER 变更指令与 Task 08 会话终结共用入口）。
     *
     * @param userCode 用户编码
     * @param reason   清理原因（CHANGE_CMD / SESSION_END）
     */
    public void evictUser(String userCode, String reason) {
        userRoleCache.invalidate(userCode);
        computedCache.evictUser(userCode, reason);
        logState("USER_ROLE", userCode, "LOADED→EVICTED", reason);
    }

    /**
     * 匹配请求控制配置（AntPathMatcher 最长匹配，增加 appCode 维度）。
     *
     * <p>方法匹配规则：配置的 apiMethod 为 {@code REQUEST} 时可匹配任意请求方法，否则忽略大小写精确相等。</p>
     *
     * @param appCode 应用编码
     * @param method  HTTP 方法
     * @param uri     请求 URI
     * @return 匹配到的配置条目；无命中返回 null
     */
    public AppAuthResource.ApiControlEntry matchRequestControl(String appCode, String method, String uri) {
        AppAuthResource resource = getAppResource(appCode);
        List<AppAuthResource.ApiControlEntry> entries = resource.getRequestControls();
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        AppAuthResource.ApiControlEntry matched = null;
        for (AppAuthResource.ApiControlEntry entry : entries) {
            if (entry.apiMethod() != null && entry.apiUri() != null
                && ("REQUEST".equalsIgnoreCase(entry.apiMethod()) || entry.apiMethod().equalsIgnoreCase(method))
                && pathMatcher.match(entry.apiUri(), uri)) {
                // 取 apiUri 最长（最具体）者
                if (matched == null || entry.apiUri().length() > matched.apiUri().length()) {
                    matched = entry;
                }
            }
        }
        return matched;
    }

    private void logState(String type, String key, String transition, String reason) {
        log.info("[auth-cache] 状态变迁: type={}, key={}, {} reason={}", type, key, transition, reason);
    }
}
