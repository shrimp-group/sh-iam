package com.wkclz.iam.admin.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class IamAdminConfig {

    @Value("${iam.api.scan.insert:0}")
    private Integer apiScanInsert;


}
