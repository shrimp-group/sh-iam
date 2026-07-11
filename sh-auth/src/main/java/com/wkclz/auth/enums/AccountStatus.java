package com.wkclz.auth.enums;

/** 账号状态 */
public enum AccountStatus {
    ENABLED("启用"),
    DISABLED("禁用"),
    LOCKED("锁定");

    private final String desc;
    AccountStatus(String desc) { this.desc = desc; }
    public String getDesc() { return desc; }
}
