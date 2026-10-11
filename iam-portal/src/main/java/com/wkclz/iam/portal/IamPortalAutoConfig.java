package com.wkclz.iam.portal;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan(basePackages = {"com.wkclz.iam.portal"})
@MapperScan(basePackages = {"com.wkclz.iam.portal.mapper"})
public class IamPortalAutoConfig {
}


