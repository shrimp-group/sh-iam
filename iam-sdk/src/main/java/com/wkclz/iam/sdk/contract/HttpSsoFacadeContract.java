package com.wkclz.iam.sdk.contract;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wkclz.auth.bean.RequestRecord;
import com.wkclz.iam.sdk.contract.bean.req.SessionCreateReq;
import com.wkclz.iam.sdk.contract.bean.resp.LoginResp;
import com.wkclz.iam.sdk.contract.config.ContractSettings;
import com.wkclz.iam.sdk.contract.facade.SsoFacadeContract;
import com.wkclz.iam.sdk.helper.AkSignHelper;
import com.wkclz.core.exception.SystemException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnMissingBean(SsoFacadeContract.class)
public class HttpSsoFacadeContract implements SsoFacadeContract {

    private static final String URI_PREFIX = "/sign";

    @Override
    public LoginResp login(SessionCreateReq req) {
        String serverUrl = ContractSettings.getServerUrl();
        if (StringUtils.isBlank(serverUrl)) {
            throw SystemException.of("iam.contract.server-url 未配置，无法远程登录，请配置 SSO 服务端地址");
        }
        log.info("远程创建会话，authIdentifier: {}", req.getAuthIdentifier());
        String responseBody = postDataWithResponse("/login", req);
        JSONObject jsonObject = JSON.parseObject(responseBody);
        Object data = jsonObject.get("data");
        if (data == null) {
            throw SystemException.of("远程登录响应异常，无 data 字段");
        }
        return JSON.parseObject(data.toString(), LoginResp.class);
    }

    @Override
    public void saveLog(RequestRecord requestLog) {
        try {
            postData("/saveLog", requestLog);
        } catch (Exception e) {
            log.error("远程保存请求日志失败: {}", e.getMessage());
        }
    }

    @Override
    public void logout(String token) {
        if (StringUtils.isBlank(token)) return;
        try {
            postData("/logout", token);
        } catch (Exception e) {
            log.error("远程登出失败: {}", e.getMessage());
        }
    }

    private void postData(String uri, Object data) {
        if (StringUtils.isBlank(uri) || data == null) return;
        String url = ContractSettings.getServerUrl() + URI_PREFIX + uri;
        String sign = AkSignHelper.sign(ContractSettings.getAppId(), ContractSettings.getAppSecret());
        HttpRequest post = HttpUtil.createPost(url);
        post.header("app-id", ContractSettings.getAppId());
        post.header("sign", sign);
        post.body(JSONObject.toJSONString(data));
        HttpResponse execute = post.execute();
        if (execute.getStatus() != 200) {
            throw SystemException.of("请求{}异常: {}", uri, execute.getStatus());
        }
    }

    private String postDataWithResponse(String uri, Object data) {
        if (StringUtils.isBlank(uri)) throw SystemException.of("uri 不能为空");
        if (data == null) throw SystemException.of("data 不能为空");
        String url = ContractSettings.getServerUrl() + URI_PREFIX + uri;
        String sign = AkSignHelper.sign(ContractSettings.getAppId(), ContractSettings.getAppSecret());
        HttpRequest post = HttpUtil.createPost(url);
        post.header("app-id", ContractSettings.getAppId());
        post.header("sign", sign);
        post.body(JSONObject.toJSONString(data));
        HttpResponse execute = post.execute();
        if (execute.getStatus() != 200) {
            throw SystemException.of("请求{}异常: {}", uri, execute.getStatus());
        }
        return execute.body();
    }
}
