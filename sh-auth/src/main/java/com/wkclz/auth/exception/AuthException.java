package com.wkclz.auth.exception;

import com.wkclz.auth.enums.AuthErrorType;
import lombok.Getter;

/**
 * 认证/授权异常基类
 */
@Getter
public class AuthException extends RuntimeException {

    private final AuthErrorType errorType;

    public AuthException(AuthErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public AuthException(AuthErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }
}
