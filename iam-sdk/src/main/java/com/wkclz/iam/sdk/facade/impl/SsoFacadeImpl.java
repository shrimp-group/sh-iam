package com.wkclz.iam.sdk.facade.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSONObject;
import com.wkclz.iam.sdk.config.IamSdkConfig;
import com.wkclz.iam.sdk.facade.SsoFacade;
import com.wkclz.iam.sdk.helper.AkSignHelper;
import com.wkclz.iam.sdk.model.RequestLog;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;


public class SsoFacadeImpl implements SsoFacade {

    private static final String URI_PREFIX = "/sign";

    @Resource
    private IamSdkConfig config;

    @Override
    public void saveLog(RequestLog log) {
        postData("/saveLog", log);
    }

    private void postData(String uri, RequestLog data) {
        if (StringUtils.isBlank(uri)) {
            throw new RuntimeException("uri 不能为空");
        }
        if (data == null) {
            return;
        }
        String serverUrl = config.getServerUrl();
        if (StringUtils.isBlank(serverUrl)) {
            throw new RuntimeException("server-url 不能为空");
        }
        String appId = config.getAppId();
        if (StringUtils.isBlank(appId)) {
            throw new RuntimeException("app-id 不能为空");
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
                throw new RuntimeException("请求" + uri + "异常:" + status);
            }
        } catch (Exception e) {
            throw new RuntimeException("请求" + uri + "异常:" + e.getMessage());
        }
    }

}
