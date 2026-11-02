package com.wkclz.iam.session.remote;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.wkclz.core.base.R;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.SystemException;
import com.wkclz.iam.session.bean.auth.AppAuthResource;
import com.wkclz.iam.session.spi.AuthResourceFacade;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 权限资源远程门面 — iam-session 独立部署（第三方应用）时经 HTTP 调用 portal 端点取数。
 *
 * <p>条件装配：仅当 {@code iam.session.remote.server-url} 配置时注册（@Primary 覆盖 NoOp）；
 * co-deploy 时 portal 的 LocalAuthResourceFacade(@Primary) 生效、本类不装配（server-url 未配置）。</p>
 *
 * <p>鉴权：请求头 auth-token（与 portal 端 iam.portal.auth.token 配对；密钥对方式后续重新设计）。
 * 请求失败 fail-fast 抛 SystemException，由缓存调用方 fail-open 处理。</p>
 */
@Slf4j
@Component
@Primary
@ConditionalOnProperty(name = "iam.session.remote.server-url")
public class RemoteAuthResourceFacade implements AuthResourceFacade {

    private static final String APP_RESOURCE_PATH = "/iam-portal/public/auth/app-resource";
    private static final String USER_ROLES_PATH = "/iam-portal/public/auth/user-roles";
    private static final String HEADER_AUTH_TOKEN = "auth-token";

    @Resource
    private RemoteClientConfig config;

    @Override
    public AppAuthResource loadAppResource(String appCode) {
        log.info("远程加载应用静态资源: appCode={}", appCode);
        return getData(APP_RESOURCE_PATH, Map.of("appCode", appCode), new TypeReference<R<AppAuthResource>>() {
        });
    }

    @Override
    public List<String> loadUserRoleCodes(String userCode) {
        log.info("远程加载用户有效角色: userCode={}", userCode);
        return getData(USER_ROLES_PATH, Map.of("userCode", userCode), new TypeReference<R<List<String>>>() {
        });
    }

    /**
     * 组装 URL（query 编码）→ HTTP GET → 解析 R 载荷并校验成功码
     */
    private <T> T getData(String path, Map<String, String> query, TypeReference<R<T>> type) {
        String serverUrl = config.getServerUrl();
        if (StringUtils.isBlank(serverUrl)) {
            throw SystemException.of("iam.session.remote.server-url 未配置");
        }
        String token = config.getAuthToken();
        if (StringUtils.isBlank(token)) {
            throw SystemException.of("iam.session.remote.auth-token 未配置");
        }

        StringJoiner qs = new StringJoiner("&");
        query.forEach((k, v) -> qs.add(URLEncoder.encode(k, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(v, StandardCharsets.UTF_8)));
        String url = serverUrl + path + "?" + qs;

        String body = executeGet(url);
        R<T> r = JSON.parseObject(body, type);
        if (r == null || r.getCode() != ResultCode.SUCCESS.getCode()) {
            log.error("远程取数返回失败: path={}, code={}, msg={}", path, r == null ? null : r.getCode(), r == null ? null : r.getMsg());
            throw SystemException.of("远程取数返回失败: path={}, code={}", path, r == null ? null : r.getCode());
        }
        return r.getData();
    }

    /**
     * 执行 HTTP GET（包可见，便于测试 spy 桩替）
     */
    String executeGet(String url) {
        try (HttpResponse response = HttpUtil.createGet(url)
            .header(HEADER_AUTH_TOKEN, config.getAuthToken())
            .timeout(10_000)
            .execute()) {
            if (response.getStatus() != 200) {
                log.error("远程取数 HTTP 异常: url={}, status={}", url, response.getStatus());
                throw SystemException.of("请求远程端点异常: status={}", response.getStatus());
            }
            return response.body();
        }
    }

}
