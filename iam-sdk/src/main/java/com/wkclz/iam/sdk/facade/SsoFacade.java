package com.wkclz.iam.sdk.facade;


import com.wkclz.iam.sdk.bean.RequestLog;

public interface SsoFacade {

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
