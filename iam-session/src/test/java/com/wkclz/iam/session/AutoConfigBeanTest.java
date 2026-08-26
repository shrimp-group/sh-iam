package com.wkclz.iam.session;

import com.wkclz.iam.session.cache.ApiControlCache;
import com.wkclz.iam.session.spi.ApiRequestControlProvider;
import com.wkclz.redis.helper.RedisHelper;
import com.wkclz.redis.helper.RedisLock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 自动配置 Bean 注册复现测试 — 验证 @ConditionalOnMissingBean + @Component 在 iam-session 自动配置中的行为。
 */
class AutoConfigBeanTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(IamSessionAutoConfig.class))
        .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
        .withBean(RedisTemplate.class, () -> mock(RedisTemplate.class))
        .withBean(RedisHelper.class, () -> mock(RedisHelper.class))
        .withBean(RedisLock.class, () -> mock(RedisLock.class));

    @Test
    void sessionAloneShouldRegisterNoOpProviderAndBuildCache() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(ApiControlCache.class);
            // 无外部实现时，NoOp 始终注册作为兜底
            assertThat(context).hasSingleBean(ApiRequestControlProvider.class);
            assertThat(context.getBean(ApiRequestControlProvider.class))
                .isInstanceOf(com.wkclz.iam.session.spi.NoOpApiRequestControlProvider.class);
        });
    }

    @Test
    void externalPrimaryProviderShouldOverrideNoOp() {
        runner.withBean(ApiRequestControlProvider.class,
                () -> mock(ApiRequestControlProvider.class),
                beanDefinition -> beanDefinition.setPrimary(true))
            .run(context -> {
                assertThat(context).hasSingleBean(ApiControlCache.class);
                // 有 @Primary 实现时，注入选择 Primary 而非 NoOp
                assertThat(context.getBean(ApiRequestControlProvider.class))
                    .isNotInstanceOf(com.wkclz.iam.session.spi.NoOpApiRequestControlProvider.class);
            });
    }
}
