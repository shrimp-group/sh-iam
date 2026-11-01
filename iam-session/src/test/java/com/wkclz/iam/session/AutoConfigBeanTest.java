package com.wkclz.iam.session;

import com.wkclz.iam.session.spi.AuthResourceFacade;
import com.wkclz.iam.session.spi.NoOpAuthResourceFacade;
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
 * 自动配置 Bean 注册测试 — 验证 SPI 装配模式（NoOp 始终注册 + @Primary 覆盖）在 iam-session 自动配置中的行为。
 */
class AutoConfigBeanTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(IamSessionAutoConfig.class))
        .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
        .withBean(RedisTemplate.class, () -> mock(RedisTemplate.class))
        .withBean(RedisHelper.class, () -> mock(RedisHelper.class))
        .withBean(RedisLock.class, () -> mock(RedisLock.class));

    @Test
    void sessionAloneShouldRegisterNoOpFacade() {
        runner.run(context -> {
            // 无外部实现时，NoOp 始终注册作为兜底
            assertThat(context).hasSingleBean(AuthResourceFacade.class);
            assertThat(context.getBean(AuthResourceFacade.class))
                .isInstanceOf(NoOpAuthResourceFacade.class);
        });
    }

    @Test
    void externalPrimaryFacadeShouldOverrideNoOp() {
        runner.withBean(AuthResourceFacade.class,
                () -> mock(AuthResourceFacade.class),
                beanDefinition -> beanDefinition.setPrimary(true))
            .run(context -> {
                // 有 @Primary 实现时，注入选择 Primary 而非 NoOp
                assertThat(context.getBean(AuthResourceFacade.class))
                    .isNotInstanceOf(NoOpAuthResourceFacade.class);
            });
    }
}
