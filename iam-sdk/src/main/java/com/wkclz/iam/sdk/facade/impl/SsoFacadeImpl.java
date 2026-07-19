package com.wkclz.iam.sdk.facade.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSONObject;
import com.wkclz.iam.sdk.bean.RequestLog;
import com.wkclz.iam.sdk.bean.req.LogoutReq;
import com.wkclz.iam.sdk.config.IamSdkConfig;
import com.wkclz.iam.sdk.facade.SsoFacade;
import com.wkclz.iam.sdk.helper.AkSignHelper;
import com.wkclz.iam.sdk.helper.SessionHelper;
import com.wkclz.core.exception.SystemException;
import com.wkclz.web.helper.RequestHelper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SsoFacadeImpl implements SsoFacade {

    private static final Logger log = LoggerFactory.getLogger(SsoFacadeImpl.class);

    private static final String URI_PREFIX = "/sign";

    @Resource
    private IamSdkConfig config;

    @Override
    public void saveLog(RequestLog log) {
        postData("/saveLog", log);
    }

    @Override
    public void logout(String token) {
        if (StringUtils.isBlank(token)) {
            return;
        }
        String serverUrl = config.getServerUrl();
        if (StringUtils.isBlank(serverUrl)) {
            log.error("iam.sdk.server-url 未配置，无法远程登出，请配置 SSO 服务端地址");
            throw SystemException.of("iam.sdk.server-url 未配置，无法远程登出，请配置 SSO 服务端地址");
        }
        log.info("远程登出，token: {}", token);
        LogoutReq logoutReq = new LogoutReq();
        logoutReq.setToken(token);
        postData("/logout", logoutReq);
    }

    @Override
    public void logout() {
        String token = SessionHelper.getToken(RequestHelper.getRequest());
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
