package com.wkclz.iam.session.enums;

/**
 * 应用鉴权类型枚举（对应 iam_app.auth_type）— 决定接入应用请求的鉴权深度。
 *
 * <ul>
 *   <li>TOKEN → 仅会话认证，不做接口纳管与接口权限校验</li>
 *   <li>TOKEN_API → 会话认证 + 全量纳管 + 接口权限校验</li>
 * </ul>
 */
public enum AppAuthType {

    /**
     * 仅会话认证（JWT + Redis Session）
     */
    TOKEN("token 鉴权"),

    /**
     * 会话认证 + 接口权限校验
     */
    TOKEN_API("token + 接口鉴权");

    private final String desc;

    AppAuthType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 解析鉴权类型：仅精确匹配 TOKEN / TOKEN_API；null、空白或未知值一律安全默认 TOKEN_API
     * （fail-closed，保持既有行为）。
     *
     * @param authType 鉴权类型字符串
     * @return 解析结果（永不返回 null）
     */
    public static AppAuthType of(String authType) {
        if ("TOKEN_API".equals(authType)) {
            return TOKEN_API;
        }
        if ("TOKEN".equals(authType)) {
            return TOKEN;
        }
        return TOKEN_API;
    }

    /**
     * 是否需要接口权限校验
     *
     * @return TOKEN_API 返回 true；TOKEN 返回 false
     */
    public boolean requireApiPermission() {
        return this == TOKEN_API;
    }

}
