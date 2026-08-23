package com.wkclz.iam.session.spi;

import com.wkclz.iam.session.bean.ApiControlConfig;
import com.wkclz.iam.session.cache.ApiControlCache;

import java.util.List;

/**
 * API 请求控制配置提供 SPI — iam-session 模块定义接口，由上层模块（如 iam-sso）实现查询 iam_api 表并转换为轻量模型。
 *
 * <p>默认无实现（{@code @ConditionalOnMissingBean}）时返回空列表，请求控制静默不生效。</p>
 *
 * @see ApiControlCache
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
