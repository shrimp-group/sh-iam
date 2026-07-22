package com.wkclz.iam.session.spi;

import com.wkclz.iam.session.bean.RequestRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * {@code RequestLogHandler} 的空实现 — 当无其他 Bean 时作为默认值，避免 NPE。
 *
 * <p>使用 {@code @ConditionalOnMissingBean} 条件装配，仅当容器中没有其他
 * {@code RequestLogHandler} 时才生效。</p>
 */
@Component
@ConditionalOnMissingBean(RequestRecordHandler.class)
public class NoOpRequestRecordHandler implements RequestRecordHandler {

    @Override
    public void handle(RequestRecord record) {
        // no-op
    }
}
