package com.wkclz.iam.sdk.config;

import com.wkclz.iam.sdk.facade.SsoFacade;
import com.wkclz.iam.sdk.facade.impl.SsoFacadeImpl;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author shrimp
 */
@Data
@Configuration
public class IamSdkConfig {

    @Value("${iam.sdk.enabled:true}")
    private Boolean enabled;


    @Value(("${iam.sdk.app-code:}"))
    private String appCode;

    /**
     * JWT 密钥, 实际使用时，请覆盖配置，不要使用默认配置
     */
    @Value("${iam.sdk.jwt.secret-key:qwertyuioplkjhgfdsazxcvbnmqwertyuioplkjhgfdsazxcvbnm}")
    private String jwtSecretKey;


    @Value(("${iam.sdk.static.enabled:false}"))
    private String staticEnabled;
    @Value(("${iam.sdk.static.subfix:js|css|jpg|png|mp3|html|htm|jpeg|ttf|woff|ico|woff2|map}"))
    private String staticSubfix;



    /**
     * 用于请求服务端的配置
     */
    @Value(("${iam.sdk.server-url:}"))
    private String serverUrl;
    @Value(("${iam.sdk.app-id:default}"))
    private String appId;
    @Value(("${iam.sdk.app-secret:default}"))
    private String appSecret;


    /**
     * SDK 被用在独立部署的应用时生效
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public SsoFacade getCasFacade() {
        return new SsoFacadeImpl();
    }


}
