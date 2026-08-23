package com.wkclz.iam.sso.service;

import com.wkclz.iam.common.entity.IamApi;
import com.wkclz.iam.session.bean.ApiControlConfig;
import com.wkclz.iam.session.spi.ApiRequestControlProvider;
import com.wkclz.iam.sso.mapper.SsoApiControlMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * API 请求控制配置提供实现 — 查询 iam_api 表已配置请求控制的 API，并转换为 iam-session 轻量模型。
 *
 * <p>iam-session 模块定义的 {@link ApiRequestControlProvider} SPI 实现，覆盖 NoOp 默认实现。</p>
 */
@Component
public class ApiRequestControlProviderImpl implements ApiRequestControlProvider {

    @Autowired
    private SsoApiControlMapper ssoApiControlMapper;

    @Override
    public List<ApiControlConfig> listRequestControlApis() {
        List<IamApi> apis = ssoApiControlMapper.listRequestControlApis();
        List<ApiControlConfig> configs = new ArrayList<>();
        if (apis != null) {
            for (IamApi api : apis) {
                ApiControlConfig config = new ApiControlConfig();
                config.setAppCode(api.getAppCode());
                config.setApiCode(api.getApiCode());
                config.setApiMethod(api.getApiMethod());
                config.setApiUri(api.getApiUri());
                config.setRequestControl(api.getRequestControl());
                configs.add(config);
            }
        }
        return configs;
    }
}
