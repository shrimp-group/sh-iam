package com.wkclz.auth.enums;

/** 认证错误类型 */
public enum AuthErrorType {
    BAD_CREDENTIALS("凭证错误"),
    USER_NOT_FOUND("用户不存在"),
    ACCOUNT_LOCKED("账号锁定"),
    ACCOUNT_DISABLED("账号禁用"),
    ACCOUNT_EXPIRED("账号过期"),
    CREDENTIALS_EXPIRED("凭证过期"),
    TOKEN_EXPIRED("Token过期"),
    TOKEN_INVALID("Token无效"),
    CAPTCHA_REQUIRED("需要验证码"),
    CAPTCHA_ERROR("验证码错误"),
    CAPTCHA_TIMEOUT("验证码超时"),
    MFA_REQUIRED("需要MFA"),
    MFA_ERROR("MFA错误"),
    RATE_LIMITED("频率限制"),
    SESSION_EXPIRED("会话过期"),
    ACCESS_DENIED("无权访问"),
    INTERNAL_ERROR("内部错误"),
    SERVICE_UNAVAILABLE("服务不可用");

    private final String desc;
    AuthErrorType(String desc) { this.desc = desc; }
    public String getDesc() { return desc; }
}
