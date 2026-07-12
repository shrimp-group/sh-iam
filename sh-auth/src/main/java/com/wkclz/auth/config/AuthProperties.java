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
        private long ttl = 86400;
        private int maxConcurrent = 0;
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
        private List<String> paths = List.of(
            "/public/**",
            "/**/public/**",
            "/actuator/**",
            "/error"
        );
    }

    @Data
    public static class Cors {
        private boolean enabled = false;
        private String allowedOrigins = "*";
        private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";
        private String allowedHeaders = "*";
        private long maxAge = 3600;
    }
}
