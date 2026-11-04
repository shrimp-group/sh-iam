package com.wkclz.iam.portal.service;

import com.wkclz.iam.common.entity.IamApi;
import com.wkclz.iam.common.entity.IamMenuApi;
import com.wkclz.iam.common.entity.IamRoleMenu;
import com.wkclz.iam.portal.config.IamPortalConfig;
import com.wkclz.iam.portal.mapper.SsoAuthResourceMapper;
import com.wkclz.iam.session.bean.auth.AppAuthResource;
import com.wkclz.iam.session.bean.auth.AuthApi;
import com.wkclz.iam.session.bean.auth.AuthApiField;
import com.wkclz.iam.session.bean.auth.AuthApp;
import com.wkclz.iam.session.bean.auth.AuthMenu;
import com.wkclz.iam.session.spi.AuthResourceFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * LocalAuthResourceFacade 单元测试 — 装配分组、requestControl 解析跳过、用户角色透传。
 */
@ExtendWith(MockitoExtension.class)
class LocalAuthResourceFacadeTest {

    @Mock
    private SsoAuthResourceMapper mapper;

    private LocalAuthResourceFacade facade;

    @BeforeEach
    void setUp() {
        // Facade 为 @Resource 字段注入，测试经反射装配（对齐主代码注入风格）
        facade = new LocalAuthResourceFacade();
        ReflectionTestUtils.setField(facade, "ssoAuthResourceMapper", mapper);
        ReflectionTestUtils.setField(facade, "config", new IamPortalConfig());
    }

    /**
     * 桩基础数据（loadAppResource 会调用全部 8 个查询，逐一 stub 避免严格模式告警）
     */
    private void stubAll() {
        when(mapper.selectApp(anyString())).thenReturn(app("appA", "应用A", "TOKEN_API"));
        when(mapper.listAppRoles(anyString())).thenReturn(new LinkedHashSet<>(List.of("r1", "r2")));
        when(mapper.listAppPublicRoleCodes(anyString())).thenReturn(new LinkedHashSet<>(List.of("r2")));
        when(mapper.listAppMenus(anyString())).thenReturn(new ArrayList<>(List.of(
            menu("m1", "0", "MENU", null, 1),
            menu("m2", "m1", "MENU", null, 2),
            menu("b1", "m1", "BUTTON", "b1", 3))));
        when(mapper.listRoleMenus(anyString())).thenReturn(new ArrayList<>(List.of(
            roleMenu("r1", "m1"), roleMenu("r1", "m2"), roleMenu("r2", "m1"))));
        when(mapper.listAppApis(anyString())).thenReturn(new ArrayList<>(List.of(
            api("api_1", "GET", "/a/b", 1), api("api_2", "POST", "/a/**", 1), api("api_3", "REQUEST", "/r/x", 1))));
        // m2 关系指向应用外 api_ghost（脏数据），装配时应被跳过
        when(mapper.listMenuApis(anyString())).thenReturn(new ArrayList<>(List.of(
            menuApi("m1", "api_1"), menuApi("m1", "api_2"), menuApi("m2", "api_ghost"))));
        when(mapper.listApiFields(anyString())).thenReturn(new ArrayList<>(List.of(apiField("api_1", "f1"))));
        when(mapper.listRequestControlApis(anyString())).thenReturn(new ArrayList<>(List.of(
            controlApi("api_1", "{\"enable\":true}"))));
    }

    @Test
    void assembleGroupsCorrectly() {
        stubAll();
        AppAuthResource resource = facade.loadAppResource("appA");

        assertEquals("appA", resource.getAppCode());
        assertEquals("TOKEN_API", resource.getApp().getAuthType());
        assertEquals(Set.of("r1", "r2"), resource.getRoleCodes());
        assertEquals(Set.of("r2"), resource.getPublicRoleCodes());
        assertTrue(resource.getLoadTime() > 0);

        // menuMap：键与顺序（LinkedHashMap 保 sort 序）
        assertEquals(List.of("m1", "m2", "b1"), new ArrayList<>(resource.getMenuMap().keySet()));

        // roleMenuMap：roleCode → menuCodes
        assertEquals(Set.of("m1", "m2"), resource.getRoleMenuMap().get("r1"));
        assertEquals(Set.of("m1"), resource.getRoleMenuMap().get("r2"));

        // apiMap
        assertEquals(3, resource.getApiMap().size());
        assertEquals("GET", resource.getApiMap().get("api_1").getApiMethod());

        // menuApiMap：m1 双 API；m2 的孤儿关系被跳过
        assertEquals(Set.of("api_1", "api_2"), resource.getMenuApiMap().get("m1"));
        assertFalse(resource.getMenuApiMap().containsKey("m2"));

        // 白名单索引：精确键（大写 METHOD:URI）+ 通配小列表（URI 含通配或 method=REQUEST）
        assertEquals(Set.of("GET:/a/b"), resource.getWhiteListExactKeys());
        assertEquals(List.of("api_2", "api_3"),
            resource.getWhiteListWildcardApis().stream().map(AuthApi::getApiCode).toList());

        // 纳管索引：api_1 精确、api_2 通配（api_3 未绑定菜单不入纳管索引）
        assertEquals(Set.of("GET:/a/b"), resource.getManagedExactKeys());
        assertEquals(List.of("api_2"),
            resource.getManagedWildcardApis().stream().map(AuthApi::getApiCode).toList());

        // apiFields / requestControls
        assertEquals(1, resource.getApiFields().size());
        assertEquals(1, resource.getRequestControls().size());
        assertEquals("api_1", resource.getRequestControls().get(0).apiCode());
        assertEquals(Boolean.TRUE, resource.getRequestControls().get(0).requestControl().getEnable());
    }

    @Test
    void appMissingYieldsNullApp() {
        // 应用不存在：selectApp 返回 null，app 字段保持 null（消费侧默认规则兜底）
        stubAll();
        when(mapper.selectApp(anyString())).thenReturn(null);
        AppAuthResource resource = facade.loadAppResource("appA");
        assertNull(resource.getApp());
    }

    @Test
    void globalKeyLoadsFlattenedResourceWithoutApp() {
        // 合并部署：__GLOBAL__ 打平键 → selectApp 返回 null（app 为 null → fail-closed），
        // 其余查询不过滤应用（mapper 动态 SQL），全量聚合
        stubAll();
        when(mapper.selectApp("__GLOBAL__")).thenReturn(null);
        AppAuthResource resource = facade.loadAppResource("__GLOBAL__");

        assertNull(resource.getApp());
        assertEquals("__GLOBAL__", resource.getAppCode());
        assertEquals(Set.of("r1", "r2"), resource.getRoleCodes());
        assertEquals(Set.of("r2"), resource.getPublicRoleCodes());
        assertEquals(3, resource.getMenuMap().size());
        assertEquals(3, resource.getApiMap().size());
    }

    @Test
    void blankRequestControlSkipped() {
        stubAll();
        when(mapper.listRequestControlApis(anyString())).thenReturn(new ArrayList<>(List.of(
            controlApi("api_1", " "))));
        AppAuthResource resource = facade.loadAppResource("appA");
        assertTrue(resource.getRequestControls().isEmpty());
    }

    @Test
    void invalidRequestControlSkippedWithoutException() {
        stubAll();
        when(mapper.listRequestControlApis(anyString())).thenReturn(new ArrayList<>(List.of(
            controlApi("api_1", "{bad json"), controlApi("api_2", "{\"enable\":true}"))));
        AppAuthResource resource = facade.loadAppResource("appA");
        // 非法跳过，合法保留
        assertEquals(1, resource.getRequestControls().size());
        assertEquals("api_2", resource.getRequestControls().get(0).apiCode());
    }

    @Test
    void loadUserRoleCodesPassthrough() {
        when(mapper.listUserActiveRoleCodes("u1")).thenReturn(List.of("r1", "r2"));
        assertEquals(List.of("r1", "r2"), facade.loadUserRoleCodes("u1"));

        when(mapper.listUserActiveRoleCodes("u2")).thenReturn(null);
        assertNotNull(facade.loadUserRoleCodes("u2"));
        assertTrue(facade.loadUserRoleCodes("u2").isEmpty());
    }

    @Test
    void superUserYieldsSentinelRole() {
        // 配置 super 用户 → 不查绑定关系，直接返回哨兵角色；配置尾随空格 trim 后精确匹配
        IamPortalConfig config = new IamPortalConfig();
        config.setSuperUsers(List.of("super_1 ", "u9"));
        ReflectionTestUtils.setField(facade, "config", config);

        assertEquals(List.of(AuthResourceFacade.SUPER_ROLE_CODE), facade.loadUserRoleCodes("super_1"));
        // 非配置用户不受影响（mapper 未桩时返回 null → 空列表）
        assertTrue(facade.loadUserRoleCodes("u1").isEmpty());
    }

    @Test
    void sentinelNamedRoleFilteredOut() {
        // DB 中存在与哨兵同名的角色编码时，出口剔除，确保 super 语义只来源于配置判断
        when(mapper.listUserActiveRoleCodes("u1")).thenReturn(
            new ArrayList<>(List.of("r1", AuthResourceFacade.SUPER_ROLE_CODE)));
        assertEquals(List.of("r1"), facade.loadUserRoleCodes("u1"));
    }

    @Test
    void dbErrorPropagatesForFailOpen() {
        // 仅桩到抛错点（listAppApis 前的调用），避免严格模式下的未消费桩
        when(mapper.listAppRoles(anyString())).thenReturn(new LinkedHashSet<>(List.of("r1")));
        when(mapper.listAppMenus(anyString())).thenReturn(new ArrayList<>());
        when(mapper.listRoleMenus(anyString())).thenReturn(new ArrayList<>());
        when(mapper.listAppApis(anyString())).thenThrow(new RuntimeException("db down"));
        assertThrows(RuntimeException.class, () -> facade.loadAppResource("appA"));
    }

    // ========== 构造辅助 ==========

    private AuthApp app(String appCode, String appName, String authType) {
        AuthApp app = new AuthApp();
        app.setAppCode(appCode);
        app.setAppName(appName);
        app.setAuthType(authType);
        return app;
    }

    private AuthMenu menu(String menuCode, String parentCode, String menuType, String buttonCode, int sort) {
        AuthMenu m = new AuthMenu();
        m.setMenuCode(menuCode);
        m.setParentCode(parentCode);
        m.setMenuType(menuType);
        m.setButtonCode(buttonCode);
        m.setSort(sort);
        return m;
    }

    private IamRoleMenu roleMenu(String roleCode, String menuCode) {
        IamRoleMenu rel = new IamRoleMenu();
        rel.setRoleCode(roleCode);
        rel.setMenuCode(menuCode);
        return rel;
    }

    private IamMenuApi menuApi(String menuCode, String apiCode) {
        IamMenuApi rel = new IamMenuApi();
        rel.setMenuCode(menuCode);
        rel.setApiCode(apiCode);
        return rel;
    }

    private AuthApi api(String apiCode, String apiMethod, String apiUri) {
        return api(apiCode, apiMethod, apiUri, null);
    }

    private AuthApi api(String apiCode, String apiMethod, String apiUri, Integer writeFlag) {
        AuthApi a = new AuthApi();
        a.setApiCode(apiCode);
        a.setApiMethod(apiMethod);
        a.setApiUri(apiUri);
        a.setWriteFlag(writeFlag);
        return a;
    }

    private AuthApiField apiField(String apiCode, String fieldCode) {
        AuthApiField f = new AuthApiField();
        f.setApiCode(apiCode);
        f.setFieldCode(fieldCode);
        return f;
    }

    private IamApi controlApi(String apiCode, String requestControl) {
        IamApi a = new IamApi();
        a.setApiCode(apiCode);
        a.setApiMethod("GET");
        a.setApiUri("/a/b");
        a.setRequestControl(requestControl);
        return a;
    }
}
