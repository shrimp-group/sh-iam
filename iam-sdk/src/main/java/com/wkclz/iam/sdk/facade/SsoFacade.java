package com.wkclz.iam.sdk.facade;


import com.wkclz.iam.sdk.bean.RequestLog;
import com.wkclz.iam.sdk.bean.req.SessionCreateReq;
import com.wkclz.iam.sdk.bean.resp.LoginResp;

public interface SsoFacade {

    /**
     * 使用用户信息创建会话并记录登录日志
     */
    LoginResp login(SessionCreateReq req);

    /**
     * 保存请求日志
     */
    void saveLog(RequestLog log);

    /**
     * 登出指定 token 的用户
     */
    void logout(String token);

    /**
     * 登出当前用户（从请求上下文获取 token）
     */
    void logout();

}
