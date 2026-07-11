package com.wkclz.auth.enums;

/** Token 类型 */
public enum TokenType {
    JWT("JWT"),
    OAUTH_BEARER("OAuth Bearer"),
    SESSION_ID("SessionId");

    private final String desc;
    TokenType(String desc) { this.desc = desc; }
    public String getDesc() { return desc; }
}
