package com.wkclz.iam.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan(basePackages = {"com.wkclz.iam.admin"})
@MapperScan(basePackages = {"com.wkclz.iam.admin.mapper"})
public class IamAdminAutoConfig {
}


