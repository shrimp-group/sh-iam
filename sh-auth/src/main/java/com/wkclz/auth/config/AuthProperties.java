package com.wkclz.auth.config;

import com.wkclz.auth.enums.MfaType;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * sh-auth 配置属性
 */
@Data
@Validated
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
        @Min(60)
        private long ttl = 86400;
        /**
         * 同一用户最大并发会话数，0 表示不限制（建议设置合理上限防止会话洪水）
         */
        @Min(0)
        private int maxConcurrent = 5;
    }

    @Data
    public static class Password {
        @Min(1)
        private int expireDays = 180;
        @Min(0)
        private int historySize = 5;
        @Min(8)
        private int minLength = 8;
    }

    @Data
    public static class Mfa {
        private MfaType defaultType = null;
    }

    @Data
    public static class RateLimit {
        @Min(1)
        private int maxAttempts = 5;
        @Min(1)
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

    @Data
    public static class Cors {
        private boolean enabled = false;
        private String allowedOrigins = "*";
        private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";
        private String allowedHeaders = "*";
        private long maxAge = 3600;
    }
}
