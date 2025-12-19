package com.wkclz.iam.sso;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan(basePackages = {"com.wkclz.iam.sso"})
@MapperScan(basePackages = {"com.wkclz.iam.sso.mapper"})
public class IamSsoAutoConfig {
}


