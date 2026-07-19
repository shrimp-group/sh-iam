package com.wkclz.iam.common.event;

import java.io.Serializable;

/**
 * 管理员安全操作事件 — 统一事件类，通过 {@link Type} 区分操作类型。
 *
 * <p>PASSWORD_RESET 时 userCode 非 null；STATUS_CHANGED 时 newStatus 非 null。</p>
 */
public class AdminSecurityEvent implements Serializable {

    public enum Type {
        PASSWORD_RESET, STATUS_CHANGED, ROLE_CHANGED
    }

    private final Type type;
    private final String userCode;
    private final Integer newStatus;
    private final long timestamp;

    private AdminSecurityEvent(Type type, String userCode, Integer newStatus) {
        this.type = type;
        this.userCode = userCode;
        this.newStatus = newStatus;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 管理员重置密码。
     */
    public static AdminSecurityEvent passwordReset(String userCode) {
        return new AdminSecurityEvent(Type.PASSWORD_RESET, userCode, null);
    }

    /**
     * 用户状态变更（禁用/锁定）。
     */
    public static AdminSecurityEvent statusChanged(String userCode, Integer newStatus) {
        return new AdminSecurityEvent(Type.STATUS_CHANGED, userCode, newStatus);
    }

    /**
     * 角色绑定变更。
     */
    public static AdminSecurityEvent roleChanged(String userCode) {
        return new AdminSecurityEvent(Type.ROLE_CHANGED, userCode, null);
    }

    // ========== Getters ==========

    public Type getType() {
        return type;
    }

    public String getUserCode() {
        return userCode;
    }

    public Integer getNewStatus() {
        return newStatus;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
