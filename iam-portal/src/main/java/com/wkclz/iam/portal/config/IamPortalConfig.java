package com.wkclz.iam.portal.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author shrimp
 */
@Data
@Configuration
public class IamPortalConfig {

    @Value("${iam.login.public-key:}")
    private String publicKey;
    @Value("${iam.login.private-key:}")
    private String privateKey;


    @Value("${iam.portal.password.expire-days:180}")
    private Integer passwordExpireDays;

    @Value("${iam.portal.max-concurrent-sessions:0}")
    private Integer maxConcurrentSessions;

    @Value("${iam.portal.password-history-size:5}")
    private Integer passwordHistorySize;



}
