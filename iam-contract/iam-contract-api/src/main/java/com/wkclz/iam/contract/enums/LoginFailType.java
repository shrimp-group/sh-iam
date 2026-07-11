package com.wkclz.iam.contract.enums;

/**
 * 登录失败类型枚举
 * 枚举内携带中文 message，翻译在枚举内完成
 * 由 SsoFacadeContract 实现方在登录失败时选用，通过 LoginResp.fail() 返回
 *
 * @author shrimp
 */
public enum LoginFailType {

    /**
     * 用户名或密码错误（用户名错误与密码错误合并，防用户枚举攻击；覆盖 LDAP 49 凭据无效）
     */
    USERNAME_OR_PASSWORD_ERROR("用户名或密码错误"),
    /**
     * 账号已禁用（LDAP 533 账号禁用 / 管理员停用）
     */
    ACCOUNT_DISABLED("账号已禁用"),
    /**
     * 账号已锁定（LDAP 775 登录次数超限锁定 / 人工锁定）
     */
    ACCOUNT_LOCKED("账号已锁定"),
    /**
     * 凭据已过期（LDAP 532/773 密码过期需修改）
     */
    CREDENTIALS_EXPIRED("凭据已过期"),
    /**
     * 需要验证码（风控触发要求图形 / 短信验证码）
     */
    CAPTCHA_REQUIRED("需要验证码"),
    /**
     * 验证码错误（图形 / 短信 / 邮箱验证码校验失败）
     */
    CAPTCHA_ERROR("验证码错误"),
    /**
     * 租户无效（租户不存在或已停用）
     */
    TENANT_INVALID("租户无效"),
    /**
     * 认证类型不支持（authType 不在支持列表）
     */
    AUTH_TYPE_UNSUPPORTED("认证类型不支持"),
    /**
     * 三方标识无效（authIdentifier 对应的三方账号无效）
     */
    AUTH_IDENTIFIER_INVALID("三方标识无效"),
    /**
     * 登录失败（兜底，无法归类的失败）
     */
    UNKNOWN("登录失败");

    private final String message;

    LoginFailType(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
