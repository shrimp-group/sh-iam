package com.wkclz.auth.exception;

import com.wkclz.auth.enums.AuthErrorType;

/** 认证异常 */
public class AuthenticationException extends AuthException {
    public AuthenticationException(AuthErrorType errorType, String message) {
        super(errorType, message);
    }

    public AuthenticationException(AuthErrorType errorType, String message, Throwable cause) {
        super(errorType, message, cause);
    }
}
