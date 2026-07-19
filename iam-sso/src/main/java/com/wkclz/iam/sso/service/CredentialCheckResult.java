package com.wkclz.iam.sso.service;

import com.wkclz.core.identity.UserIdentity;

/**
 * 凭证校验结果。
 */
public class CredentialCheckResult {

    /**
     * 失败原因枚举。
     */
    public enum FailReason {
        USER_NOT_FOUND,
        DISABLED,
        LOCKED,
        AUTH_DISABLED,
        PASSWORD_ERROR,
        PASSWORD_EXPIRED
    }

    private final boolean success;
    private final UserIdentity userIdentity;
    private final FailReason failReason;

    private CredentialCheckResult(boolean success, UserIdentity userIdentity, FailReason failReason) {
        this.success = success;
        this.userIdentity = userIdentity;
        this.failReason = failReason;
    }

    public static CredentialCheckResult success(UserIdentity userIdentity) {
        return new CredentialCheckResult(true, userIdentity, null);
    }

    public static CredentialCheckResult fail(FailReason reason) {
        return new CredentialCheckResult(false, null, reason);
    }

    public static CredentialCheckResult fail(FailReason reason, UserIdentity userIdentity) {
        return new CredentialCheckResult(false, userIdentity, reason);
    }

    public boolean isSuccess() {
        return success;
    }

    public UserIdentity getUserIdentity() {
        return userIdentity;
    }

    public FailReason getFailReason() {
        return failReason;
    }
}
