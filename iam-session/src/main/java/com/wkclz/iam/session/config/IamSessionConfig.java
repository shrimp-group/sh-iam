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

}
