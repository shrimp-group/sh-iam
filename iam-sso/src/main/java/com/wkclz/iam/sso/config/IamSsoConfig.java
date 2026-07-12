package com.wkclz.iam.sso.config;

import io.jsonwebtoken.security.Keys;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * IAM SSO 配置属性
 *
 * @author shrimp
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "iam.sso")
public class IamSsoConfig {

    // ───── JWT ─────
    /** JWT 签名密钥（至少 32 字符，建议 64 字符随机字符串） */
    private String jwtSecretKey;

    /** 从 jwtSecretKey 构建 SecretKey（每次调用重新构建，确保热刷新生效） */
    public SecretKey getJwtSecret() {
        return Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8));
    }

    // ───── 登录 ─────
    /**
     * 登录密码过期天数（默认 180）
     */
    private int passwordExpireDays = 180;
    /**
     * 最大并发会话数（0=不限制）
     */
    private int maxConcurrentSessions = 0;
    /** RSA 公钥（前端加密用） */
    private String publicKey;
    /** RSA 私钥（后端解密用） */
    private String privateKey;

    // ───── SDK 契约（HTTP RPC 场景） ─────
    /**
     * SSO 服务端地址
     */
    private String serverUrl;
    /**
     * 应用 ID
     */
    private String appId;
    /** 应用密钥 */
    private String appSecret;
}
