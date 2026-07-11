package com.wkclz.iam.contract.defaults.config;

import com.wkclz.auth.filter.FilterOrder;
import com.wkclz.iam.contract.defaults.facade.DefaultSsoFacadeContract;
import com.wkclz.iam.contract.defaults.filter.DefaultAuthFilter;
import com.wkclz.iam.contract.defaults.service.DefaultAkSignContract;
import com.wkclz.iam.contract.defaults.service.DefaultAuthContract;
import com.wkclz.iam.contract.defaults.service.DefaultAuthzContract;
import com.wkclz.iam.contract.facade.SsoFacadeContract;
import com.wkclz.iam.contract.service.AkSignContract;
import com.wkclz.iam.contract.service.AuthContract;
import com.wkclz.iam.contract.service.AuthzContract;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * 契约层自动配置
 *
 * 注册默认实现（@ConditionalOnMissingBean）：
 * 业务方一旦提供 AuthContract 等 Bean，默认实现自动失效
 *
 * @author shrimp
 */
@Slf4j
@AutoConfiguration
@ComponentScan(basePackages = {"com.wkclz.iam.contract.defaults"})
@ConditionalOnProperty(prefix = "sh.iam.contract", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IamContractAutoConfig {

    @Bean
    @ConditionalOnMissingBean
    public AuthContract authContract() {
        log.info("注册默认 AuthContract（读宽容、验证严格）");
        return new DefaultAuthContract();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthzContract authzContract() {
        log.info("注册默认 AuthzContract（读返回空、鉴权拒绝）");
        return new DefaultAuthzContract();
    }

    @Bean
    @ConditionalOnMissingBean
    public AkSignContract akSignContract() {
        log.info("注册默认 AkSignContract（功能不可用）");
        return new DefaultAkSignContract();
    }

    @Bean
    @ConditionalOnMissingBean
    public SsoFacadeContract ssoFacadeContract() {
        log.info("注册默认 SsoFacadeContract（saveLog 静默、其余不可用）");
        return new DefaultSsoFacadeContract();
    }

    /**
     * DefaultAuthFilter Bean
     * 不加 @Component，由 FilterRegistrationBean 包装注册
     */
    @Bean
    @ConditionalOnMissingBean
    public DefaultAuthFilter defaultAuthFilter() {
        return new DefaultAuthFilter();
    }

    /**
     * 注册 DefaultAuthFilter 到过滤器链
     * 通过 ContractConfig.authFilterEnabled 控制是否注册
     */
    @Bean
    public FilterRegistrationBean<DefaultAuthFilter> defaultAuthFilterRegistration(
            DefaultAuthFilter filter, ContractConfig config) {
        if (Boolean.FALSE.equals(config.getAuthFilterEnabled())) {
            log.info("DefaultAuthFilter 已禁用（iam.contract.auth-filter-enabled=false）");
            return null;
        }
        FilterRegistrationBean<DefaultAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(FilterOrder.AUTH);
        registration.setName("defaultAuthFilter");
        log.info("注册 DefaultAuthFilter");
        return registration;
    }
}
