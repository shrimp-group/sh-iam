package com.wkclz.iam.session.enums;

/**
 * 权限变更指令类型枚举 — 决定联动清理映射（见 AuthCacheManager 权威状态映射表）。
 *
 * <ul>
 *   <li>APP_STATIC → L1 失效该应用 + L3 整组失效该应用</li>
 *   <li>USER → L2 失效该用户 + L3 各应用内该用户条目</li>
 * </ul>
 */
public enum AuthChangeType {

    /**
     * 应用静态资源变更（角色/菜单/角色菜单/菜单API/API/API字段等），targetKey 为 appCode
     */
    APP_STATIC("应用静态变更"),

    /**
     * 用户资源变更（用户角色绑定/解绑/有效期刷新），targetKey 为 userCode
     */
    USER("用户变更");

    private final String desc;

    AuthChangeType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

}
