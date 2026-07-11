package com.wkclz.iam.sdk.contract.exception;

import com.wkclz.iam.sdk.contract.enums.AuthErrorType;
import lombok.Getter;

/**
 * 契约层统一异常
 * 用于认证、鉴权、AK 签名等场景的错误标识
 *
 * @author shrimp
 */
@Deprecated
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
