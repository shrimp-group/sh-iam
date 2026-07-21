package com.wkclz.iam.session.remote;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSONObject;
import com.wkclz.core.exception.SystemException;
import com.wkclz.core.identity.IdentityContext;
import com.wkclz.iam.session.bean.RequestRecord;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 远程 SSO 门面 HTTP 实现 — 第三方应用通过 HTTP 调用 SSO 服务端。
 *
 * <p>历史：从 iam-sdk facade.impl.SsoFacadeImpl 迁入并重命名。</p>
 *
 * <p>变更：
 * <ul>
 *   <li>包名改为 com.wkclz.iam.session.remote</li>
 *   <li>配置类 IamSdkConfig → RemoteClientConfig</li>
 *   <li>日志载体 RequestLog → RequestRecord（iam-session 统一载体）</li>
 *   <li>Token 获取 SessionHelper.getToken → IdentityContext.getToken</li>
 *   <li>移除 LogoutReq，直接传 token 字符串到 /logout 端点</li>
 *   <li>新增 @ConditionalOnProperty 条件装配，仅在配置 server-url 时启用</li>
 * </ul>
 * </p>
 *
 * <p>条件装配：仅当 {@code iam.session.remote.server-url} 配置时注册，
 * 避免服务端部署时误启用远程调用。</p>
 */
@Component
@ConditionalOnProperty(name = "iam.session.remote.server-url")
public class RemoteSsoFacadeImpl implements SsoFacade {

    private static final Logger log = LoggerFactory.getLogger(RemoteSsoFacadeImpl.class);

    private static final String URI_PREFIX = "/sign";

    @Resource
    private RemoteClientConfig config;

    @Override
    public void saveLog(RequestRecord record) {
        postData("/saveLog", record);
    }

    @Override
    public void logout(String token) {
        if (StringUtils.isBlank(token)) {
            return;
        }
        String serverUrl = config.getServerUrl();
        if (StringUtils.isBlank(serverUrl)) {
            log.error("iam.session.remote.server-url 未配置，无法远程登出，请配置 SSO 服务端地址");
            throw SystemException.of("iam.session.remote.server-url 未配置，无法远程登出，请配置 SSO 服务端地址");
        }
        log.info("远程登出，token: {}", token);
        // 直接传 token 字符串作为请求体，SSO 服务端 /sign/logout 端点接收 token 字符串
        postData("/logout", token);
    }

    @Override
    public void logout() {
        String token = IdentityContext.getToken();
        if (StringUtils.isBlank(token)) {
            return;
        }
        logout(token);
    }

    private void postData(String uri, Object data) {
        if (StringUtils.isBlank(uri)) {
            throw SystemException.of("uri 不能为空");
        }
        if (data == null) {
            return;
        }
        String serverUrl = config.getServerUrl();
        if (StringUtils.isBlank(serverUrl)) {
            throw SystemException.of("server-url 不能为空");
        }
        String appId = config.getAppId();
        if (StringUtils.isBlank(appId)) {
            throw SystemException.of("app-id 不能为空");
        }

        String url = serverUrl + URI_PREFIX + uri;
        String sign = AkSignHelper.sign(config.getAppId(), config.getAppSecret());

        HttpRequest post = HttpUtil.createPost(url);
        post.header("app-id", appId);
        post.header("sign", sign);

        try {
            post.body(JSONObject.toJSONString(data));
            HttpResponse execute = post.execute();
            int status = execute.getStatus();
            if (status != 200) {
                throw SystemException.of("请求{}异常: {}", uri, status);
            }
        } catch (SystemException e) {
            // 已是统一异常，直接抛出
            throw e;
        } catch (RuntimeException e) {
            // TD-025: 保留原始 cause，使用统一异常体系
            throw new SystemException("请求" + uri + "异常:" + e.getMessage(), e);
        }
    }

}
