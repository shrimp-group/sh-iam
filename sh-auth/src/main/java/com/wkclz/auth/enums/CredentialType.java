package com.wkclz.auth.enums;

/** 凭证类型 */
public enum CredentialType {
    PASSWORD("密码"),
    SMS_CODE("短信验证码"),
    SOCIAL_CODE("社交授权码");

    private final String desc;
    CredentialType(String desc) { this.desc = desc; }
    public String getDesc() { return desc; }
}
