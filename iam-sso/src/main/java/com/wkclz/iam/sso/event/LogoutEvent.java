package com.wkclz.iam.sso.event;

import java.io.Serializable;

/**
 * 登出事件。
 */
public class LogoutEvent implements Serializable {

    private final String username;
    private final String maskedToken;
    private final long timestamp;

    public LogoutEvent(String username, String token) {
        this.username = username;
        this.maskedToken = maskToken(token);
        this.timestamp = System.currentTimeMillis();
    }

    public String getUsername() {
        return username;
    }

    public String getMaskedToken() {
        return maskedToken;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Token 脱敏：仅保留前 8 位 + ****。
     */
    private static String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return token == null ? "" : token;
        }
        return token.substring(0, 8) + "****";
    }
}
