package com.wkclz.iam.common.event;

import java.io.Serializable;

/**
 * 管理员重置密码事件。
 */
public class PasswordResetByAdminEvent implements Serializable {

    private final String userCode;
    private final long timestamp;

    public PasswordResetByAdminEvent(String userCode) {
        this.userCode = userCode;
        this.timestamp = System.currentTimeMillis();
    }

    public String getUserCode() {
        return userCode;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
