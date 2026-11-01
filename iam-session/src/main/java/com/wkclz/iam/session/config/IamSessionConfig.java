package com.wkclz.iam.session.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * iam-session 配置。
 *
 * <p>Token 配置前缀：{@code iam.session.token}</p>
 */
@Data
@Configuration
public class IamSessionConfig {

    /**
     * JWT 签名密钥（HS256，最低 32 字符）
     */
    @Value(("${iam.session.token.secret-key:qwertyuioplkjhgfdsazxcvbnmqwertyuioplkjhgfdsazxcvbnm}"))
    private String secretKey;

    /**
     * 白名单路径（Ant 风格，逗号分隔），默认 /**&#47;public/**
     */
    @Value("${iam.session.white-list:}")
    private String whiteList;

    /**
     * Token TTL（秒），默认 48h
     */
    @Value(("${iam.session.token.ttl:172800}"))
    private Long ttl;

    /**
     * 同一用户最大并发会话数（0=不限制），默认 0
     */
    @Value(("${iam.session.max-concurrent:0}"))
    private Integer maxConcurrent;

    /**
     * Redis Session 初始 TTL（秒），默认 24h
     */
    @Value(("${iam.session.redis-ttl:86400}"))
    private Long redisTtl;

    /**
     * 续期阈值（秒）：Redis 剩余 TTL 低于此值时触发续期，默认 30min
     */
    @Value(("${iam.session.renewal.threshold:1800}"))
    private Long renewalThreshold;

    /**
     * 续期间隔（秒）：同一 session 在此时长内不重复续期，默认 5min
     */
    @Value(("${iam.session.renewal.interval:300}"))
    private Long renewalInterval;

    /**
     * 请求控制全局开关，默认 true（API 级开关默认 false，需在 API 管理页面显式开启）
     */
    @Value("${iam.request-control.enabled:true}")
    private Boolean requestControlEnabled;

    /**
     * 请求控制互斥锁默认超时（秒），防止请求异常导致死锁，默认 30
     */
    @Value("${iam.request-control.mutex.timeout-seconds:30}")
    private Integer requestControlMutexTimeoutSeconds;

    /**
     * 请求控制限流默认统计窗口时长（秒），默认 60
     */
    @Value("${iam.request-control.rate-limit.window-seconds:60}")
    private Integer requestControlRateLimitWindowSeconds;

    /**
     * 请求控制限流默认窗口内最大请求次数，默认 100
     */
    @Value("${iam.request-control.rate-limit.max-requests:100}")
    private Integer requestControlRateLimitMaxRequests;

    /**
     * 接口权限判断开关（AuthPermissionFilter），默认 true（未纳管接口自动放行，不受影响）
     */
    @Value("${iam.auth.check.enabled:true}")
    private Boolean authCheckEnabled;

    /**
     * L1 应用静态资源缓存最大应用数（容量 LRU），默认 100
     */
    @Value("${iam.auth.cache.static-max-apps:100}")
    private Integer authCacheStaticMaxApps;

    /**
     * 远程 facade 形态下 L1 静态资源 TTL（秒，静态陈旧硬界）；仅配置 iam.session.remote.server-url 时生效，本地形态不受影响，默认 300
     */
    @Value("${iam.auth.cache.static-remote-ttl-seconds:300}")
    private Integer authCacheStaticRemoteTtlSeconds;

    /**
     * L2 用户角色缓存最大用户数（兼 L3 用户层容量），默认 10000
     */
    @Value("${iam.auth.cache.user-max-users:10000}")
    private Integer authCacheUserMaxUsers;

    /**
     * L2 与 L3 用户层空闲 TTL（秒，expireAfterAccess）：用户久不活跃后缓存清理，活跃访问自动续期，默认 30min
     */
    @Value("${iam.auth.cache.user-idle-seconds:1800}")
    private Integer authCacheUserIdleSeconds;

    /**
     * L3 URL 级计算结果时效（秒，expireAfterWrite），默认 300
     */
    @Value("${iam.auth.cache.computed-ttl-seconds:300}")
    private Integer authCacheComputedTtlSeconds;

    /**
     * L3 单用户最大 URL 条目数，默认 1024
     */
    @Value("${iam.auth.cache.computed-max-urls:1024}")
    private Integer authCacheComputedMaxUrls;

    /**
     * 权限变更通知 Redis Pub/Sub channel，默认 iam:auth:change
     */
    @Value("${iam.auth.change.channel:iam:auth:change}")
    private String authChangeChannel;

    /**
     * 远程形态权限变更轮询间隔（秒）：0=关闭（默认），跨 Redis 部署时建议 30
     */
    @Value("${iam.auth.change.poll-interval-seconds:0}")
    private Integer authChangePollIntervalSeconds;

}
