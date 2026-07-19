package com.wkclz.iam.session.spi;

import com.wkclz.iam.session.bean.RequestRecord;

/**
 * 请求日志持久化 SPI — iam-session 模块定义接口，由上层模块（如 iam-sso）实现。
 *
 * <p>当 {@code RequestLogFilter} 采集完请求日志后，调用此 SPI 进行持久化。
 * 默认无实现（{@code @ConditionalOnMissingBean}），静默跳过。</p>
 *
 * <p>建议实现方异步处理（{@code @Async}）以免阻塞请求链路。</p>
 *
 * @see com.wkclz.iam.session.filter.RequestRecordFilter
 */
@FunctionalInterface
public interface RequestRecordHandler {

    /**
     * 处理请求日志。
     *
     * @param record 请求日志数据
     */
    void handle(RequestRecord record);
}
