package com.wkclz.iam.sso.contract.facade;

import com.wkclz.auth.bean.RequestRecord;
import com.wkclz.iam.sso.contract.bean.req.SessionCreateReq;
import com.wkclz.iam.sso.contract.bean.resp.LoginResp;
import com.wkclz.auth.context.SecurityContext;

/**
 * SSO RPC 门面契约
 * 客户端应用通过此契约调用 SSO 服务端
 *
 * @author shrimp
 */
public interface SsoFacadeContract {

    /**
     * 远程登录（创建会话并记录登录日志）
     * <p>
     * 语义约定：
     * - 业务登录结果（成功 / 用户名密码错误 / 账号锁定等）统一通过返回的 LoginResp 表达，
     * login() 永不抛业务登录失败异常，由调用方判断 LoginResp.success
     * - 仅系统级错误（SSO 服务端不可达、配置缺失等不可恢复故障）抛 RuntimeException
     *
     * @param req 会话创建请求
     * @return 登录响应；success=false 时 failType 必填，failReason 可选动态详情
     */
    LoginResp login(SessionCreateReq req);

    /**
     * 远程保存请求日志
     * 客户端应用将请求日志上报到 SSO 服务端集中存储
     *
     * @param log 请求日志
     */
    void saveLog(RequestRecord log);

    /**
     * 远程登出（指定 token）
     *
     * @param token JWT token
     */
    void logout(String token);

    /**
     * 远程登出（从 PrincipalContext 获取 token）
     */
    default void logout() {
        logout(SecurityContext.getToken());
    }
}
