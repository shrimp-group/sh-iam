package com.wkclz.auth.bean;

import com.wkclz.auth.enums.AuthErrorType;
import com.wkclz.auth.enums.AuthStatus;
import lombok.Data;
import java.io.Serializable;

/** 认证结果 */
@Data
public class AuthResult implements Serializable {
    private AuthStatus status;
    private AuthErrorType errorType;
    private String errorMessage;
    private AuthToken token;
    private Principal principal;
    private Session session;
    private MfaChallenge mfaChallenge;

    public boolean isSuccess() {
        return status == AuthStatus.SUCCESS;
    }

    public static AuthResult success(Principal principal, AuthToken token) {
        AuthResult r = new AuthResult();
        r.status = AuthStatus.SUCCESS;
        r.principal = principal;
        r.token = token;
        return r;
    }

    public static AuthResult fail(AuthErrorType errorType) {
        AuthResult r = new AuthResult();
        r.status = AuthStatus.FAILED;
        r.errorType = errorType;
        r.errorMessage = errorType.getMessage();
        return r;
    }

}
