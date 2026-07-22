package com.wkclz.iam.session.remote;

import com.wkclz.iam.session.bean.RequestRecord;

/**
 * 远程 SSO 门面契约 — 第三方应用通过 HTTP 调用 SSO 服务端时使用。
 *
 * <p>历史：从 iam-sdk facade.SsoFacade 迁入。
 * 变更：saveLog 参数类型从 iam-sdk RequestLog 改为 iam-session RequestRecord，
 * 统一日志载体，避免重复定义。</p>
 *
 * <p>实现见 {@link RemoteSsoFacadeImpl}。</p>
 */
public interface SsoFacade {

    /**
     * 远程保存请求日志到 SSO 服务端
     *
     * @param record 请求日志记录（使用 iam-session 的 RequestRecord 载体）
     */
    void saveLog(RequestRecord record);

    /**
     * 远程登出指定 token 的用户
     *
     * @param token JWT token
     */
    void logout(String token);

    /**
     * 远程登出当前用户（从 IdentityContext 获取 token）
     */
    void logout();

}
