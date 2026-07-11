package com.wkclz.auth.exception;

import com.wkclz.auth.enums.AuthErrorType;
import lombok.Getter;

/** MFA 要求异常（携带 challengeId 供全局异常处理器映射） */
@Getter
public class MfaRequiredException extends AuthException {

    private final String challengeId;

    public MfaRequiredException(String challengeId, String message) {
        super(AuthErrorType.MFA_REQUIRED, message);
        this.challengeId = challengeId;
    }
}
