package com.wkclz.auth.enums;

/** 认证状态 */
public enum AuthStatus {
    SUCCESS("成功"),
    MFA_REQUIRED("需要MFA"),
    FAILED("失败");

    private final String desc;
    AuthStatus(String desc) { this.desc = desc; }
    public String getDesc() { return desc; }
}
