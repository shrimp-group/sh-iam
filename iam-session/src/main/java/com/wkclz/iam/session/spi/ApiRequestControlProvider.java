package com.wkclz.iam.session.spi;

import com.wkclz.iam.session.bean.ApiControlConfig;

import java.util.List;

/**
 * API 请求控制配置提供 SPI — iam-session 模块定义接口，由上层模块（如 iam-sso）实现查询 iam_api 表并转换为轻量模型。
 *
 * <p>默认实现 {@link NoOpApiRequestControlProvider} 始终注册作为兜底（返回空列表，请求控制静默不生效）；
 * 上层模块实现类应标注 {@code @Primary} 覆盖默认行为。</p>
 *
 * @see com.wkclz.iam.session.cache.ApiControlCache
 * @see NoOpApiRequestControlProvider
 */
@FunctionalInterface
public interface ApiRequestControlProvider {

    /**
     * 查询所有已配置请求控制（request_control 非空）的 API。
     *
     * @return 已配置请求控制的 API 列表（轻量模型）
     */
    List<ApiControlConfig> listRequestControlApis();
}
