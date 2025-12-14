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


}
