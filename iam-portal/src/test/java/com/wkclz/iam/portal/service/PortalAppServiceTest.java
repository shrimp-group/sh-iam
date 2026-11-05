package com.wkclz.iam.portal.service;

import com.wkclz.iam.common.entity.IamApp;
import com.wkclz.iam.portal.bean.resp.AppOptionResp;
import com.wkclz.iam.portal.config.IamPortalConfig;
import com.wkclz.iam.portal.mapper.SsoAppMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * PortalAppService 单元测试 — TOKEN 恒展示、生效角色应用、super 全量、去重排序。
 */
@ExtendWith(MockitoExtension.class)
class PortalAppServiceTest {

    @Mock
    private SsoAppMapper mapper;

    private PortalAppService service;

    @BeforeEach
    void setUp() {
        // Service 为 @Resource 字段注入，测试经反射装配（对齐主代码注入风格）
        service = new PortalAppService();
        ReflectionTestUtils.setField(service, "ssoAppMapper", mapper);
        ReflectionTestUtils.setField(service, "config", new IamPortalConfig());
    }

    @Test
    void tokenAppsAlwaysVisible() {
        // TOKEN 应用登录即可用，无生效角色也展示
        when(mapper.selectAllApps()).thenReturn(new ArrayList<>(List.of(
            app("portal", "门户", "TOKEN", 1),
            app("appB", "应用B", "TOKEN_API", 2))));
        when(mapper.selectUserActiveAppCodes("u1")).thenReturn(null);

        List<AppOptionResp> result = service.listAccessibleApps("u1");
        assertEquals(List.of("portal"), result.stream().map(AppOptionResp::getAppCode).toList());
        assertEquals("门户", result.get(0).getAppName());
    }

    @Test
    void tokenApiAppsVisibleOnlyWithActiveRole() {
        // TOKEN_API 应用仅当用户有生效角色时展示（appB 有角色，appC 无角色）
        when(mapper.selectAllApps()).thenReturn(new ArrayList<>(List.of(
            app("portal", "门户", "TOKEN", 1),
            app("appB", "应用B", "TOKEN_API", 2),
            app("appC", "应用C", "TOKEN_API", 3))));
        when(mapper.selectUserActiveAppCodes("u1")).thenReturn(List.of("appB"));

        List<AppOptionResp> result = service.listAccessibleApps("u1");
        assertEquals(List.of("portal", "appB"), result.stream().map(AppOptionResp::getAppCode).toList());
    }

    @Test
    void superUserSeesAllApps() {
        // super 用户不查生效角色，返回全部未删除应用
        when(mapper.selectAllApps()).thenReturn(new ArrayList<>(List.of(
            app("portal", "门户", "TOKEN", 1),
            app("appB", "应用B", "TOKEN_API", 2))));

        IamPortalConfig config = new IamPortalConfig();
        config.setSuperUsers(List.of("root"));
        ReflectionTestUtils.setField(service, "config", config);

        List<AppOptionResp> result = service.listAccessibleApps("root");
        assertEquals(List.of("portal", "appB"), result.stream().map(AppOptionResp::getAppCode).toList());
    }

    @Test
    void dedupAndSortAscending() {
        // 重复 appCode 去重（取首条），sort 升序且 null 排最后
        when(mapper.selectAllApps()).thenReturn(new ArrayList<>(List.of(
            app("appC", "应用C", "TOKEN", null),
            app("appB", "应用B-2", "TOKEN", 5),
            app("appA", "应用A", "TOKEN", 1),
            app("appB", "应用B-1", "TOKEN_API", 3))));

        List<AppOptionResp> result = service.listAccessibleApps("u1");
        assertEquals(List.of("appA", "appB", "appC"), result.stream().map(AppOptionResp::getAppCode).toList());
        // 去重取 sort 升序遍历中的首条
        assertEquals("应用B-2", result.get(1).getAppName());
    }

    @Test
    void blankUserCodeYieldsEmptyList() {
        // userCode 空白直接返回空列表，不触达 mapper
        assertTrue(service.listAccessibleApps(null).isEmpty());
        assertTrue(service.listAccessibleApps(" ").isEmpty());
    }

    @Test
    void superUsersTrimmedExactMatch() {
        // 配置尾随空格 trim 后精确匹配，"roo" 不命中
        IamPortalConfig config = new IamPortalConfig();
        config.setSuperUsers(List.of("root ", "u9"));
        ReflectionTestUtils.setField(service, "config", config);

        when(mapper.selectAllApps()).thenReturn(new ArrayList<>(List.of(
            app("appB", "应用B", "TOKEN_API", 2))));

        assertEquals(List.of("appB"), service.listAccessibleApps("root").stream()
            .map(AppOptionResp::getAppCode).toList());
        // 非 super 用户：无 TOKEN 应用且无生效角色 → 空列表
        when(mapper.selectUserActiveAppCodes("roo")).thenReturn(null);
        assertTrue(service.listAccessibleApps("roo").isEmpty());
    }

    // ========== 构造辅助 ==========

    private IamApp app(String appCode, String appName, String authType, Integer sort) {
        IamApp app = new IamApp();
        app.setAppCode(appCode);
        app.setAppName(appName);
        app.setDomain("http://" + appCode + ".example.com");
        app.setAuthType(authType);
        app.setSort(sort);
        return app;
    }
}
