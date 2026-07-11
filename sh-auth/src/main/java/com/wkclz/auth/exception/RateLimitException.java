package com.wkclz.auth.exception;

import com.wkclz.auth.enums.AuthErrorType;

/** 频率限制异常 */
public class RateLimitException extends AuthException {
    public RateLimitException(String message) {
        super(AuthErrorType.RATE_LIMITED, message);
    }
}
