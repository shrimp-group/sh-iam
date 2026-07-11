package com.wkclz.auth.config;

/**
 * sh-auth 常量定义
 */
public final class AuthConstants {

    /** 默认 Token 请求头 */
    public static final String DEFAULT_TOKEN_HEADER = "Authorization";

    /** Bearer Token 前缀 */
    public static final String BEARER_PREFIX = "Bearer ";

    /** 自定义 Token 请求头（兼容） */
    public static final String CUSTOM_TOKEN_HEADER = "token";

    /** 频率限制 Redis Key 前缀 */
    public static final String RATE_LIMIT_KEY_PREFIX = "auth:rate_limit:";

    /** 验证码 Redis Key 前缀 */
    public static final String CAPTCHA_KEY_PREFIX = "auth:captcha:";

    /** Session Redis Key 前缀 */
    public static final String SESSION_KEY_PREFIX = "auth:session:";

    /** 用户会话列表 Redis Key 前缀 */
    public static final String SESSION_LIST_KEY_PREFIX = "auth:session:list:";

    /** MFA 挑战 Redis Key 前缀 */
    public static final String MFA_CHALLENGE_KEY_PREFIX = "auth:mfa:challenge:";

    private AuthConstants() {
    }
}
