package com.wkclz.auth.config;

import com.wkclz.auth.cache.AuthCacheManager;
import com.wkclz.auth.context.SecurityContext;
import com.wkclz.auth.contract.auth.DefaultLogoutService;
import com.wkclz.auth.contract.auth.SessionStore;
import com.wkclz.auth.contract.infra.AuthMetadataService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * sh-auth 过滤器 & 服务 Bean 注册。
 * <p>
 * 仅在 sh.auth.enabled=true 时生效。
 */
@AutoConfiguration
public class ShAuthBeanConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DefaultLogoutService defaultLogoutService(SessionStore sessionStore) {
        return new DefaultLogoutService(sessionStore);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityContext securityContext() {
        return new SecurityContext();
    }


    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AuthMetadataService.class)
    public AuthCacheManager authCacheManager(AuthMetadataService metadataService) {
        return new AuthCacheManager(metadataService);
    }


}
