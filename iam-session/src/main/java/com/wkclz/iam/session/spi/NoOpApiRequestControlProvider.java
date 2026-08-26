package com.wkclz.iam.session.spi;

import com.wkclz.iam.session.bean.ApiControlConfig;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * {@link ApiRequestControlProvider} 空实现 — 始终注册作为兜底，返回空列表（请求控制不生效但不报错）。
 *
 * <p>注：不使用 {@code @ConditionalOnMissingBean} —— 该注解搭配 {@code @Component} 在自动配置的组件扫描中
 * 条件评估不可靠；上层模块（如 iam-sso）通过实现类标注 {@code @Primary} 覆盖默认行为。</p>
 */
@Component
public class NoOpApiRequestControlProvider implements ApiRequestControlProvider {

    @Override
    public List<ApiControlConfig> listRequestControlApis() {
        return Collections.emptyList();
    }
}
