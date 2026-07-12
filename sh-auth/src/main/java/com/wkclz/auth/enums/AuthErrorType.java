package com.wkclz.auth.enums;

/** 认证错误类型 */
public enum AuthErrorType {
    BAD_CREDENTIALS(52, "凭证错误"),
    USER_NOT_FOUND(32, "用户不存在"),
    USERNAME_OR_PASSWORD_ERROR(31, "用户名或密码错误"),
    ACCOUNT_LOCKED(47, "账号锁定"),
    ACCOUNT_DISABLED(53, "账号禁用"),
    ACCOUNT_EXPIRED(49, "账号过期"),
    CREDENTIALS_EXPIRED(48, "凭证过期"),
    TOKEN_MISSING(80, "缺少Token"),
    TOKEN_EXPIRED(81, "Token过期"),
    TOKEN_INVALID(82, "Token无效"),
    AUTH_IDENTIFIER_INVALID(83, "三方标识无效"),
    AUTH_TYPE_UNSUPPORTED(7, "认证类型不支持"),
    TENANT_INVALID(84, "租户无效"),
    CAPTCHA_REQUIRED(60, "需要验证码"),
    CAPTCHA_ERROR(54, "验证码错误"),
    CAPTCHA_TIMEOUT(61, "验证码超时"),
    MFA_REQUIRED(90, "需要MFA"),
    MFA_ERROR(91, "MFA错误"),
    RATE_LIMITED(51, "频率限制"),
    SESSION_EXPIRED(120, "会话过期"),
    ACCESS_DENIED(121, "无权访问"),
    UNKNOWN(50, "登录失败"),
    INTERNAL_ERROR(1, "内部错误"),
    SERVICE_UNAVAILABLE(6, "服务不可用");

    private final int code;
    private final String desc;
    AuthErrorType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }
    public String getMessage() { return desc; }

    /**
     * 从 JWT 错误码映射为 AuthErrorType
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
