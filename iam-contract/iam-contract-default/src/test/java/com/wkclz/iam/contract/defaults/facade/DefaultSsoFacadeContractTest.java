package com.wkclz.iam.contract.defaults.facade;

import com.wkclz.iam.contract.bean.RequestLog;
import com.wkclz.iam.contract.bean.req.SessionCreateReq;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultSsoFacadeContract 单元测试
 * 验证 login 抛异常、saveLog/logout 静默跳过
 *
 * @author shrimp
 */
class DefaultSsoFacadeContractTest {

    private final DefaultSsoFacadeContract facade = new DefaultSsoFacadeContract();

    @Test
    void login_throwsUnsupportedOperationException() {
        SessionCreateReq req = new SessionCreateReq();
        assertThrows(UnsupportedOperationException.class, () -> facade.login(req));
    }

    @Test
    void saveLog_doesNotThrow() {
        // 静默跳过，不抛异常
        RequestLog log = new RequestLog();
        assertDoesNotThrow(() -> facade.saveLog(log));
    }

    @Test
    void logout_doesNotThrow() {
        // 静默跳过，不抛异常
        assertDoesNotThrow(() -> facade.logout("any-token"));
    }

    @Test
    void logout_noArg_doesNotThrow() {
        // 无参重载（从 PrincipalContext 获取 token，无上下文时 token=null）
        assertDoesNotThrow(() -> facade.logout());
    }
}
