package com.wkclz.auth;

import com.wkclz.auth.cache.AuthCacheManager;
import com.wkclz.auth.config.AuthProperties;
import com.wkclz.auth.context.SecurityContext;
import com.wkclz.auth.contract.auth.DefaultLogoutService;
import com.wkclz.auth.contract.auth.SessionStore;
import com.wkclz.auth.contract.auth.TokenService;
import com.wkclz.auth.contract.authz.AccessControlProvider;
import com.wkclz.auth.contract.infra.AuthMetadataService;
import com.wkclz.auth.contract.infra.RequestLogger;
import com.wkclz.auth.contract.infra.SecurityHeaderProvider;
import com.wkclz.auth.filter.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.List;

/**
 * sh-auth 自动配置
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "sh.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.wkclz.auth")
@EnableConfigurationProperties(AuthProperties.class)
public class ShAuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SecurityContext securityContext() {
        return new SecurityContext();
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultLogoutService defaultLogoutService(SessionStore sessionStore) {
        return new DefaultLogoutService(sessionStore);
    }

    // ===== FilterRegistrationBean 注册 =====

    @Bean
    public FilterRegistrationBean<RequestWrapperFilter> requestWrapperFilterReg() {
        FilterRegistrationBean<RequestWrapperFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new RequestWrapperFilter());
        reg.addUrlPatterns("/*");
        reg.setOrder(Integer.MIN_VALUE);
        reg.setName("requestWrapperFilter");
        return reg;
    }

    @Bean
    public FilterRegistrationBean<RequestRecordFilter> requestRecordFilterReg(List<RequestLogger> requestLoggers) {
        FilterRegistrationBean<RequestRecordFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new RequestRecordFilter(requestLoggers));
        reg.addUrlPatterns("/*");
        reg.setOrder(FilterOrder.LOGGING);
        reg.setName("requestRecordFilter");
        return reg;
    }

    @Bean
    @ConditionalOnMissingBean
    public FilterRegistrationBean<SecurityHeaderFilter> securityHeaderFilterReg(
            List<SecurityHeaderProvider> headerProviders) {
        FilterRegistrationBean<SecurityHeaderFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new SecurityHeaderFilter(headerProviders));
        reg.addUrlPatterns("/*");
        reg.setOrder(FilterOrder.SEC_HEADER);
        reg.setName("securityHeaderFilter");
        return reg;
    }

    @Bean
    @ConditionalOnMissingBean
    public FilterRegistrationBean<AuthenticationFilter> authenticationFilterReg(
            TokenService tokenService, SessionStore sessionStore, AuthProperties properties) {
        FilterRegistrationBean<AuthenticationFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new AuthenticationFilter(tokenService, sessionStore, properties));
        reg.addUrlPatterns("/*");
        reg.setOrder(FilterOrder.AUTH);
        reg.setName("authenticationFilter");
        return reg;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AccessControlProvider.class)
    public FilterRegistrationBean<AuthorizationFilter> authorizationFilterReg(
            List<AccessControlProvider> accessControlProviders, AuthProperties properties) {
        FilterRegistrationBean<AuthorizationFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new AuthorizationFilter(accessControlProviders, properties));
        reg.addUrlPatterns("/*");
        reg.setOrder(FilterOrder.AUTHZ);
        reg.setName("authorizationFilter");
        return reg;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AuthMetadataService.class)
    public AuthCacheManager authCacheManager(AuthMetadataService metadataService) {
        return new AuthCacheManager(metadataService);
    }
}
