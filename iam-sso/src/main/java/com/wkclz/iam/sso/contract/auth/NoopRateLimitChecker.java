package com.wkclz.iam.sso.contract.auth;

import com.wkclz.auth.contract.auth.RateLimitChecker;
import org.springframework.stereotype.Component;

/**
 * IAM 限流检查器 — 空实现，IAM 使用验证码机制代替独立限流。
 *
 * @author shrimp
 */
@Component
public class NoopRateLimitChecker implements RateLimitChecker {

    @Override
    public void check(String identifier) {
        // IAM 不使用独立限流
    }

    @Override
    public void recordAttempt(String identifier, boolean success) {
        // IAM 不计入独立限流计数器
    }

    @Override
    public void reset(String identifier) {
        // no-op
    }
}
