package com.wkclz.iam.common.event;

import java.io.Serializable;

/**
 * 用户角色绑定变更事件。
 */
public class UserRoleChangedEvent implements Serializable {

    private final String userCode;
    private final long timestamp;

    public UserRoleChangedEvent(String userCode) {
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
