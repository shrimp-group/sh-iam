package com.wkclz.iam.session.service;

import com.wkclz.iam.session.bean.ApiRequestControl;
import com.wkclz.iam.session.config.IamSessionConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * API 请求控制配置解析器
 * <p>
 * 将 API 级请求控制配置（ApiRequestControl）与全局默认配置（IamSessionConfig）合并：
 * <ul>
 *   <li>入参为 null 或总开关未开启（enable 非 true）时返回 null</li>
 *   <li>总开关开启时返回新对象：子对象数值字段为空则回落至全局默认值，子开关 enable 原样保留（可为 null）</li>
 * </ul>
 *
 * @author shrimp
 */
@Slf4j
@Component
public class RequestControlResolver {

    /**
     * 全局默认配置（mutex 超时 30s / rateLimit 窗口 60s·100 次）
     */
    private final IamSessionConfig config;

    public RequestControlResolver(IamSessionConfig config) {
        this.config = config;
    }

    /**
     * 解析 API 请求控制配置，填充默认值
     *
     * @param raw API 级原始配置
     * @return 填充默认值后的配置；无配置或总开关关闭时返回 null
     */
    public ApiRequestControl resolve(ApiRequestControl raw) {
        log.info("解析 API 请求控制配置: enable={}", raw == null ? null : raw.getEnable());
        if (raw == null || !Boolean.TRUE.equals(raw.getEnable())) {
            return null;
        }
        ApiRequestControl resolved = new ApiRequestControl();
        resolved.setEnable(true);
        // mutex：数值字段为空时回落至全局默认值，子开关 enable 透传（可为 null）
        ApiRequestControl.Mutex mutex = new ApiRequestControl.Mutex();
        mutex.setEnable(raw.getMutex() != null ? raw.getMutex().getEnable() : null);
        mutex.setTimeoutSeconds(raw.getMutex() != null && raw.getMutex().getTimeoutSeconds() != null
            ? raw.getMutex().getTimeoutSeconds()
            : config.getRequestControlMutexTimeoutSeconds());
        resolved.setMutex(mutex);
        // rateLimit：数值字段为空时回落至全局默认值，子开关 enable 透传（可为 null）
        ApiRequestControl.RateLimit rateLimit = new ApiRequestControl.RateLimit();
        rateLimit.setEnable(raw.getRateLimit() != null ? raw.getRateLimit().getEnable() : null);
        rateLimit.setWindowSeconds(raw.getRateLimit() != null && raw.getRateLimit().getWindowSeconds() != null
            ? raw.getRateLimit().getWindowSeconds()
            : config.getRequestControlRateLimitWindowSeconds());
        rateLimit.setMaxRequests(raw.getRateLimit() != null && raw.getRateLimit().getMaxRequests() != null
            ? raw.getRateLimit().getMaxRequests()
            : config.getRequestControlRateLimitMaxRequests());
        resolved.setRateLimit(rateLimit);
        return resolved;
    }
}
