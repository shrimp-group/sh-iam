package com.wkclz.iam.sdk;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan(basePackages = {"com.wkclz.iam.sdk"})
@ConditionalOnProperty(prefix = "sh.iam.sdk", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IamSdkAutoConfig {
}


