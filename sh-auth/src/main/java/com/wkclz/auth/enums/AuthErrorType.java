package com.wkclz.auth.enums;

/** 认证错误类型 */
public enum AuthErrorType {
    BAD_CREDENTIALS("凭证错误"),
    USERNAME_OR_PASSWORD_ERROR("用户名或密码错误"),
    ACCOUNT_LOCKED("账号锁定"),
    ACCOUNT_DISABLED("账号禁用"),
    ACCOUNT_EXPIRED("账号过期"),
    CREDENTIALS_EXPIRED("凭证过期"),
    TOKEN_MISSING("缺少Token"),
    TOKEN_EXPIRED("Token过期"),
    TOKEN_INVALID("Token无效"),
    AUTH_IDENTIFIER_INVALID("三方标识无效"),
    AUTH_TYPE_UNSUPPORTED("认证类型不支持"),
    TENANT_INVALID("租户无效"),
    CAPTCHA_REQUIRED("需要验证码"),
    CAPTCHA_ERROR("验证码错误"),
    CAPTCHA_TIMEOUT("验证码超时"),
    MFA_REQUIRED("需要MFA"),
    MFA_ERROR("MFA错误"),
    RATE_LIMITED("频率限制"),
    SESSION_EXPIRED("会话过期"),
    ACCESS_DENIED("无权访问"),
    UNKNOWN("登录失败"),
    INTERNAL_ERROR("内部错误"),
    SERVICE_UNAVAILABLE("服务不可用");

    private final String desc;
    AuthErrorType(String desc) { this.desc = desc; }
    public String getDesc() { return desc; }

    /**
     * 兼容 LoginFailType.message 语义
     */
    public String getMessage() {
        return desc;
    }

    /**
     * 从 JWT 错误码映射为 AuthErrorType
     * JwtValidationException 错误码常量: JWT_EXPIRED / JWT_MALFORMED / JWT_UNSUPPORTED / JWT_SIGNATURE_ERROR / JWT_ILLEGAL_ARGUMENT
     */
    public static AuthErrorType fromJwtErrorCode(String jwtErrorCode) {
        return switch (jwtErrorCode) {
            case "JWT_EXPIRED"        -> TOKEN_EXPIRED;
            case "JWT_MALFORMED"      -> TOKEN_INVALID;
            case "JWT_UNSUPPORTED"    -> TOKEN_INVALID;
            case "JWT_SIGNATURE_ERROR"-> TOKEN_INVALID;
            case "JWT_ILLEGAL_ARGUMENT"-> TOKEN_INVALID;
            default                   -> TOKEN_INVALID;
        };
    }
}
