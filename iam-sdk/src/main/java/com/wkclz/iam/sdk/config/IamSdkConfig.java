package com.wkclz.iam.sdk.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class IamSdkConfig {

    @Value(("${iam.sdk.app-code:}"))
    private String appCode;

    @Value(("${iam.sdk.static.enabled:false}"))
    private String staticEnabled;

    @Value(("${iam.sdk.static.subfix:js|css|jpg|png|mp3|html|htm|jpeg|ttf|woff|ico|woff2|map}"))
    private String staticSubfix;

}
