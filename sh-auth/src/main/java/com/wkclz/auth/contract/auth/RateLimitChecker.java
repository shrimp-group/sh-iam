package com.wkclz.auth.contract.auth;

public interface RateLimitChecker {
    void check(String identifier);
    void recordAttempt(String identifier, boolean success);
    void reset(String identifier);
}
