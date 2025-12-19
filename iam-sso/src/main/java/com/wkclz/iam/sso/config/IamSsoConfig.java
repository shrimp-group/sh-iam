package com.wkclz.iam.sso.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author shrimp
 */
@Data
@Configuration
public class IamSsoConfig {


    @Value("${iam.sso.password.expire-days:180}")
    private Integer passwordExpireDays;

}
