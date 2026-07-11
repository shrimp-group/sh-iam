package com.wkclz.iam.contract.defaults.facade;

import com.wkclz.iam.contract.bean.RequestLog;
import com.wkclz.iam.contract.bean.req.SessionCreateReq;
import com.wkclz.iam.contract.bean.resp.LoginResp;
import com.wkclz.iam.contract.facade.SsoFacadeContract;
import lombok.extern.slf4j.Slf4j;

/**
 * SSO 门面契约默认实现
 * login 抛异常（功能性操作，不该被调用）
 * saveLog/logout 静默跳过（日志丢失不阻断业务）
 *
 * @author shrimp
 */
@Slf4j
public class DefaultSsoFacadeContract implements SsoFacadeContract {

    @Override
    public LoginResp login(SessionCreateReq req) {
        log.warn("DefaultSsoFacadeContract: login 无实现");
        throw new UnsupportedOperationException("无 SSO 门面实现，请配置 SsoFacadeContract");
    }

    @Override
    public void saveLog(RequestLog requestLog) {
        // 日志丢失不阻断业务，静默跳过
        log.debug("DefaultSsoFacadeContract: saveLog 无实现，静默跳过");
    }

    @Override
    public void logout(String token) {
        log.debug("DefaultSsoFacadeContract: logout 无实现，静默跳过");
    }
}
