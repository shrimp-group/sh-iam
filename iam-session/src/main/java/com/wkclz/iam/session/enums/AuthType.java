package com.wkclz.iam.session.enums;

/**
 * 认证方式枚举。
 *
 * <p>独立于 iam-sdk 中的 {@code AuthType}，用于会话管理层标识认证来源。</p>
 */
public enum AuthType {

    /**
     * 用户名密码登录
     */
    PASSWORD("密码登录"),

    /**
     * 微信小程序登录
     */
    WECHAT_MINI("微信小程序"),

    /**
     * 微信公众号授权登录
     */
    WECHAT_MP("微信公众号"),

    /**
     * LDAP 认证
     */
    LDAP("LDAP"),

    /**
     * OAuth 2.0 认证
     */
    OAUTH("OAuth");

    private final String desc;

    AuthType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

}
