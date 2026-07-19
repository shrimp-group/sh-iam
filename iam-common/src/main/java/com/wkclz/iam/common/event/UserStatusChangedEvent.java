package com.wkclz.iam.common.event;

import java.io.Serializable;

/**
 * 用户状态变更事件。
 */
public class UserStatusChangedEvent implements Serializable {

    /**
     * 1=启用, 2=禁用, 3=锁定
     */
    private final String userCode;
    private final Integer newStatus;
    private final long timestamp;

    public UserStatusChangedEvent(String userCode, Integer newStatus) {
        this.userCode = userCode;
        this.newStatus = newStatus;
        this.timestamp = System.currentTimeMillis();
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
