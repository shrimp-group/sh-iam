package com.wkclz.iam.sso.event;

import com.wkclz.iam.session.enums.LoginStatus;

import java.io.Serializable;

/**
 * 登录事件 — 统一事件类，通过 {@link Type} 区分成功/失败。
 *
 * <p>SUCCESS 时 token 和 userCode 非 null；FAILED 时 loginStatus 非 null。</p>
 */
public class LoginEvent implements Serializable {

    public enum Type {
        SUCCESS, FAILED
    }

    private final Type type;
    private final String username;
    private final String userCode;
    private final String ip;
    private final String userAgent;
    private final String token;
    private final LoginStatus loginStatus;
    private final long timestamp;

    private LoginEvent(Type type, String username, String userCode,
                       String ip, String userAgent, String token, LoginStatus loginStatus) {
        this.type = type;
        this.username = username;
        this.userCode = userCode;
        this.ip = ip;
        this.userAgent = userAgent;
        this.token = token;
        this.loginStatus = loginStatus;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 创建登录成功事件。
     */
    public static LoginEvent success(String username, String userCode, String ip, String userAgent, String token) {
        return new LoginEvent(Type.SUCCESS, username, userCode, ip, userAgent, token, null);
    }

    /**
     * 创建登录失败事件。
     */
    public static LoginEvent failed(String username, String ip, String userAgent, LoginStatus loginStatus) {
        return new LoginEvent(Type.FAILED, username, null, ip, userAgent, null, loginStatus);
    }

    // ========== Getters ==========

    public Type getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    public String getUserCode() {
        return userCode;
    }

    public String getIp() {
        return ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getToken() {
        return token;
    }

    public LoginStatus getLoginStatus() {
        return loginStatus;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "LoginEvent{type=" + type + ", username='" + username + "'" +
            (type == Type.FAILED && loginStatus != null ? ", loginStatus=" + loginStatus : "") + "}";
    }
}
