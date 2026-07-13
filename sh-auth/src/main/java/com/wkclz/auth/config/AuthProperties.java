package com.wkclz.auth.config;

import com.wkclz.auth.enums.MfaType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * sh-auth 配置属性
 */
@Data
@ConfigurationProperties(prefix = "sh.auth")
public class AuthProperties {

    /** 是否启用 */
    private boolean enabled = true;

    /** 会话配置 */
    private Session session = new Session();

    /** 密码策略 */
    private Password password = new Password();

    /** MFA 配置 */
    private Mfa mfa = new Mfa();

    /** 频率限制 */
    private RateLimit rateLimit = new RateLimit();

    /** 白名单 */
    private WhiteList whiteList = new WhiteList();

    /** CORS 配置 */
    private Cors cors = new Cors();

    @Data
    public static class Session {
        /**
         * 会话 TTL，默认 24 小时
         */
        private long ttl = 86400;
        /** 同一用户最大并发会话数，0 表示不限制（建议设置合理上限防止会话洪水） */
        private int maxConcurrent = 5;
    }

    @Data
    public static class Password {
        private int expireDays = 180;
        private int historySize = 5;
        private int minLength = 8;
    }

    @Data
    public static class Mfa {
        private MfaType defaultType = null;
    }

    @Data
    public static class RateLimit {
        private int maxAttempts = 5;
        private int windowMinutes = 60;
    }

    @Data
    public static class WhiteList {
        /** 白名单路径（Ant 风格），匹配的路径无需认证 */
        private List<String> paths = List.of(
            "/public/**",
            "/**/public/**",
            "/error"
        );
    }

    /** CORS 配置（默认开启；若 CORS 由 Nginx/网关层处理，设置为 false 关闭） */
    @Data
    public static class Cors {
        private boolean enabled = true;
        /** 允许的来源域名（多个用逗号分隔），启用 CORS 时必须配置具体值，不可使用 * */
        private String allowedOrigins = "";
        private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";
        /** 允许的请求头，建议配置具体值而非 * */
        private String allowedHeaders = "Content-Type,Authorization";
        private long maxAge = 3600;
    }
}
