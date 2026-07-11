package com.wkclz.auth.exception;

import com.wkclz.auth.enums.AuthErrorType;

/** 会话过期异常 */
public class SessionExpiredException extends AuthException {
    public SessionExpiredException(String message) {
        super(AuthErrorType.SESSION_EXPIRED, message);
    }
}
