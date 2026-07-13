package com.wkclz.auth.config;

import com.wkclz.auth.contract.auth.SessionStore;
import com.wkclz.auth.contract.auth.TokenService;
import com.wkclz.auth.contract.authz.AccessControlProvider;
import com.wkclz.auth.contract.infra.RequestLogger;
import com.wkclz.auth.contract.infra.SecurityHeaderProvider;
import com.wkclz.auth.filter.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * sh-auth 过滤器 & 服务 Bean 注册。
 * <p>
 * 仅在 sh.auth.enabled=true 时生效。
 */
@AutoConfiguration
public class ShAutFilterConfiguration {


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

}
