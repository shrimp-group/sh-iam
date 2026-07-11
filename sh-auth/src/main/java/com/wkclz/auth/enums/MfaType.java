package com.wkclz.auth.enums;

/** MFA 类型 */
public enum MfaType {
    SMS("短信验证"),
    TOTP("TOTP令牌");

    private final String desc;
    MfaType(String desc) { this.desc = desc; }
    public String getDesc() { return desc; }
}
