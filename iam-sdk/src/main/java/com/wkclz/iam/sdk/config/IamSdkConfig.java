package com.wkclz.iam.sdk.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author shrimp
 */
@Data
@Configuration
public class IamSdkConfig {

    @Value("${iam.sdk.enabled:true}")
    private Boolean enabled;

    /**
     * JWT 密钥, 实际使用时，请覆盖配置，不要使用默认配置
     */
    @Value("${iam.sdk.jwt.secret-key:qwertyuioplkjhgfdsazxcvbnmqwertyuioplkjhgfdsazxcvbnm}")
    private String jwtSecretKey;

}
