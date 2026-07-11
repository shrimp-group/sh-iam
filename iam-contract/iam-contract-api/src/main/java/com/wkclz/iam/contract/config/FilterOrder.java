package com.wkclz.iam.contract.config;

import org.springframework.core.Ordered;

/**
 * 过滤器顺序常量
 * 统一管理 IAM 契约层相关过滤器的执行顺序
 * 数值越小优先级越高（越先执行）
 *
 * @author shrimp
 */
@Deprecated
public final class FilterOrder {

    private FilterOrder() {
    }

    /**
     * 请求包装过滤器（最前，需先包装 request/response）
     */
    public static final int REQUEST_WRAPPER = Ordered.HIGHEST_PRECEDENCE;

    /**
     * 请求日志过滤器（在认证前记录原始请求，在认证后记录用户信息）
     */
    public static final int LOGGING = Ordered.HIGHEST_PRECEDENCE + 1;

    /**
     * 认证过滤器（在日志之后，业务之前）
     */
    public static final int AUTH = Ordered.HIGHEST_PRECEDENCE + 10;

    /**
     * 鉴权过滤器（在认证之后，业务之前）
     */
    public static final int AUTHZ = Ordered.HIGHEST_PRECEDENCE + 20;
}
