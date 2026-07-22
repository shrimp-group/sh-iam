package com.wkclz.iam.sso.event;

import java.io.Serializable;

/**
 * 密码修改事件。
 */
public class PasswordChangedEvent implements Serializable {

    private final String userCode;
    private final long timestamp;

    public PasswordChangedEvent(String userCode) {
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
