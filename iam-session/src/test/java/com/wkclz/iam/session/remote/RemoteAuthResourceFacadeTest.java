package com.wkclz.iam.session.remote;

import com.alibaba.fastjson2.JSON;
import com.wkclz.core.base.R;
import com.wkclz.core.exception.SystemException;
import com.wkclz.iam.session.bean.auth.AppAuthResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

/**
 * RemoteAuthResourceFacade 单元测试 — URL 拼装/HTTP 异常/R 成功解包/R 失败码（executeGet 以 spy 桩替）。
 */
class RemoteAuthResourceFacadeTest {

    private static final String SERVER = "http://sso.example.com";

    private RemoteAuthResourceFacade facade;

    @BeforeEach
    void setUp() {
        RemoteClientConfig config = new RemoteClientConfig();
        config.setServerUrl(SERVER);
        config.setAuthToken("shared-token");
        facade = spy(new RemoteAuthResourceFacade());
        org.springframework.test.util.ReflectionTestUtils.setField(facade, "config", config);
    }

    @Test
    void loadAppResourceAssemblesUrlAndUnwrapsData() {
        AppAuthResource resource = AppAuthResource.empty("appA");
        String json = JSON.toJSONString(R.ok(resource));
        doReturn(json).when(facade).executeGet(anyString());

        AppAuthResource result = facade.loadAppResource("app A");

        // URL 拼装与 query 编码
        List<String> urls = org.mockito.Mockito.mockingDetails(facade).getInvocations().stream()
            .filter(inv -> "executeGet".equals(inv.getMethod().getName()))
            .map(inv -> (String) inv.getArgument(0))
            .toList();
        assertEquals(1, urls.size());
        assertEquals(SERVER + "/iam-portal/public/auth/app-resource?appCode=app+A", urls.get(0));
        // R 成功码解包返回 data
        assertNotNull(result);
        assertEquals(resource.getRoleCodes(), result.getRoleCodes());
    }

    @Test
    void loadUserRoleCodesUnwrapsList() {
        String json = JSON.toJSONString(R.ok(List.of("role_1", "role_2")));
        doReturn(json).when(facade).executeGet(anyString());

        List<String> roles = facade.loadUserRoleCodes("user_1");

        assertEquals(List.of("role_1", "role_2"), roles);
    }

    @Test
    void non200StatusPropagatesSystemException() {
        // executeGet 内部非 200 抛 SystemException，getData 不吞异常（fail-fast）
        doThrow(SystemException.of("请求远程端点异常: status=500"))
            .when(facade).executeGet(anyString());

        assertThrows(SystemException.class, () -> facade.loadAppResource("appA"));
    }

    @Test
    void failureCodeThrowsSystemException() {
        String json = JSON.toJSONString(R.error("服务端异常"));
        doReturn(json).when(facade).executeGet(anyString());

        assertThrows(SystemException.class, () -> facade.loadUserRoleCodes("user_1"));
    }
}
