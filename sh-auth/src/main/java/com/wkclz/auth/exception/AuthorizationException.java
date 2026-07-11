package com.wkclz.auth.exception;

import com.wkclz.auth.enums.AuthErrorType;

/** 授权异常 */
public class AuthorizationException extends AuthException {
    public AuthorizationException(String message) {
        super(AuthErrorType.ACCESS_DENIED, message);
    }
}
