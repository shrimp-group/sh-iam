package com.wkclz.auth.exception;

import com.wkclz.auth.enums.AuthErrorType;

/** 账号状态异常 */
public class AccountStatusException extends AuthException {
    public AccountStatusException(AuthErrorType errorType, String message) {
        super(errorType, message);
    }
}
