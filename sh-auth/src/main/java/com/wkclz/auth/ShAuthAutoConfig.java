package com.wkclz.auth;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * sh-auth 全栈自动配置入口（需显式 opt-in：sh.auth.enabled=true）。
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.wkclz.auth")
public class ShAuthAutoConfig {
}
