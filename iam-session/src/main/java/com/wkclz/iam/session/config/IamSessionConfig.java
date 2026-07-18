package com.wkclz.iam.session.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * iam-session 配置。
 *
 * <p>Token 配置前缀：{@code iam.session.token}</p>
 */
@Data
@Configuration
public class IamSessionConfig {

    /**
     * JWT 签名密钥（HS256，最低 32 字符）
     */
    @Value(("${iam.session.token.secret-key:}"))
    private String secretKey;

    /**
     * Token TTL（秒），默认 24h
     */
    @Value(("${iam.session.token.ttl:86400}"))
    private Long ttl;

    /**
     * 同一用户最大并发会话数（0=不限制），默认 0
     */
    @Value(("${iam.session.max-concurrent:0}"))
    private Integer maxConcurrent;

}
