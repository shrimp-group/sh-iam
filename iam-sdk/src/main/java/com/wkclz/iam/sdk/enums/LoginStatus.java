package com.wkclz.iam.sdk.enums;

/**
 * 登录状态枚举（参照LDAP返回码设计）
 */
public enum LoginStatus {
    
    /**
     * 登录成功
     */
    SUCCESS(0, "登录成功"),
    
    /**
     * 用户不存在
     */
    USER_NOT_FOUND(32, "用户不存在"),
    
    /**
     * 密码错误 【统一使用 32, 不能使用 31】
     */
    INVALID_PASSWORD(31, "密码错误"),
    
    /**
     * 账号锁定
     */
    ACCOUNT_LOCKED(47, "账号锁定"),
    
    /**
     * 账号禁用
     */
    ACCOUNT_DISABLED(53, "账号禁用"),
    
    /**
     * 密码过期
     */
    EXPIRED_PASSWORD(48, "密码过期"),
    
    /**
     * 账号过期
     */
    EXPIRED_ACCOUNT(49, "账号过期"),
    
    /**
     * 无效凭证（通用错误）
     */
    INVALID_CREDENTIALS(52, "无效凭证"),
    
    /**
     * 认证失败（通用错误）
     */
    AUTHENTICATION_FAILED(50, "认证失败"),
    
    /**
     * 登录尝试次数过多
     */
    TOO_MANY_ATTEMPTS(51, "登录尝试次数过多"),

    /**
     * 验证码错误
     */
    INVALID_CAPTCHA(54, "验证码错误"),

    /**
     * 需要验证码
     */
    NEED_CAPTCHA(60, "需要验证码"),

    /**
     * 验证码错误
     */
    CAPTCHA_TIMEOUT(61, "验证码超时"),
    
    /**
     * 内部错误
     */
    INTERNAL_ERROR(1, "内部错误"),
    
    /**
     * 网络错误
     */
    NETWORK_ERROR(3, "网络错误"),
    
    /**
     * 服务不可用
     */
    SERVICE_UNAVAILABLE(6, "服务不可用"),
    
    /**
     * 不支持的认证方法
     */
    UNSUPPORTED_AUTH_METHOD(7, "不支持的认证方法");
    
    private final int code;
    private final String message;
    
    LoginStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
    
    /**
     * 根据状态码获取LoginStatus枚举
     * @param code 状态码
     * @return LoginStatus枚举
     */
    public static LoginStatus getByCode(int code) {
        for (LoginStatus status : LoginStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return null;
    }
}
