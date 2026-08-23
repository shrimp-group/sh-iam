package com.wkclz.iam.session.spi;

import com.wkclz.iam.session.bean.ApiControlConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * {@link ApiRequestControlProvider} 空实现 — 当无其他 Bean 时作为默认值，返回空列表（请求控制不生效但不报错）。
 *
 * <p>使用 {@code @ConditionalOnMissingBean} 条件装配，仅当容器中没有其他
 * {@code ApiRequestControlProvider} 时才生效。</p>
 */
@Component
@ConditionalOnMissingBean(ApiRequestControlProvider.class)
public class NoOpApiRequestControlProvider implements ApiRequestControlProvider {

    @Override
    public List<ApiControlConfig> listRequestControlApis() {
        return Collections.emptyList();
    }
}
