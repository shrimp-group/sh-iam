package com.wkclz.iam.contract.defaults.config;

import com.wkclz.iam.contract.config.ContractSettings;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 契约层配置绑定
 * 采用 @Value 注入（对齐现有 IamSdkConfig 风格）
 * 通过 @PostConstruct 将配置同步到 ContractSettings 静态持有器
 *
 * @author shrimp
 */
@Data
@Configuration
public class ContractConfig {

    /**
     * 是否注册 DefaultAuthFilter
     */
    @Value("${iam.contract.auth-filter-enabled:true}")
    private Boolean authFilterEnabled;

    /**
     * 公开路径匹配模式
     */
    @Value("${iam.contract.public-path-pattern:/*/public/**}")
    private String publicPathPattern;

    /**
     * AK 签名 appId
     */
    @Value("${iam.contract.app-id:}")
    private String appId;

    /**
     * AK 签名 appSecret（RSA 私钥）
     */
    @Value("${iam.contract.app-secret:}")
    private String appSecret;

    /**
     * AK 验签 publicKey（RSA 公钥）
     */
    @Value("${iam.contract.public-key:}")
    private String publicKey;

    /**
     * SSO 服务端地址
     */
    @Value("${iam.contract.server-url:}")
    private String serverUrl;

    /**
     * JWT 密钥（供实现层使用）
     */
    @Value("${iam.contract.jwt-secret-key:}")
    private String jwtSecretKey;

    /**
     * 启动时将配置同步到 ContractSettings 静态持有器
     * 供契约接口的 default 方法访问
     */
    @PostConstruct
    public void initContractSettings() {
        ContractSettings.setAppId(appId);
        ContractSettings.setAppSecret(appSecret);
        ContractSettings.setPublicKey(publicKey);
        ContractSettings.setServerUrl(serverUrl);
        ContractSettings.setJwtSecretKey(jwtSecretKey);
    }
}
