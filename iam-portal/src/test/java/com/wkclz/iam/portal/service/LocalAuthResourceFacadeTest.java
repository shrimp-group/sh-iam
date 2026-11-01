package com.wkclz.iam.portal.service;

import com.wkclz.iam.common.entity.IamApi;
import com.wkclz.iam.common.entity.IamMenuApi;
import com.wkclz.iam.common.entity.IamRoleMenu;
import com.wkclz.iam.portal.mapper.SsoAuthResourceMapper;
import com.wkclz.iam.session.bean.auth.AppAuthResource;
import com.wkclz.iam.session.bean.auth.AuthApi;
import com.wkclz.iam.session.bean.auth.AuthApiField;
import com.wkclz.iam.session.bean.auth.AuthMenu;
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
    }

    /**
     * 桩基础数据（loadAppResource 会调用全部 7 个查询，逐一 stub 避免严格模式告警）
     */
    private void stubAll() {
        when(mapper.listAppRoles(anyString())).thenReturn(new LinkedHashSet<>(List.of("r1", "r2")));
        when(mapper.listAppMenus(anyString())).thenReturn(new ArrayList<>(List.of(
            menu("m1", "0", "MENU", null, 1),
            menu("m2", "m1", "MENU", null, 2),
            menu("b1", "m1", "BUTTON", "b1", 3))));
        when(mapper.listRoleMenus(anyString())).thenReturn(new ArrayList<>(List.of(
            roleMenu("r1", "m1"), roleMenu("r1", "m2"), roleMenu("r2", "m1"))));
        when(mapper.listAppApis(anyString())).thenReturn(new ArrayList<>(List.of(
            api("api_1", "GET", "/a/b"), api("api_2", "POST", "/a/**"))));
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
        assertEquals(Set.of("r1", "r2"), resource.getRoleCodes());
        assertTrue(resource.getLoadTime() > 0);

        // menuMap：键与顺序（LinkedHashMap 保 sort 序）
        assertEquals(List.of("m1", "m2", "b1"), new ArrayList<>(resource.getMenuMap().keySet()));

        // roleMenuMap：roleCode → menuCodes
        assertEquals(Set.of("m1", "m2"), resource.getRoleMenuMap().get("r1"));
        assertEquals(Set.of("m1"), resource.getRoleMenuMap().get("r2"));

        // apiMap
        assertEquals(2, resource.getApiMap().size());
        assertEquals("GET", resource.getApiMap().get("api_1").getApiMethod());

        // menuApiMap：m1 双 API；m2 的孤儿关系被跳过
        assertEquals(Set.of("api_1", "api_2"), resource.getMenuApiMap().get("m1"));
        assertFalse(resource.getMenuApiMap().containsKey("m2"));

        // apiFields / requestControls
        assertEquals(1, resource.getApiFields().size());
        assertEquals(1, resource.getRequestControls().size());
        assertEquals("api_1", resource.getRequestControls().get(0).apiCode());
        assertEquals(Boolean.TRUE, resource.getRequestControls().get(0).requestControl().getEnable());
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
    void dbErrorPropagatesForFailOpen() {
        // 仅桩到抛错点（listAppApis 前的调用），避免严格模式下的未消费桩
        when(mapper.listAppRoles(anyString())).thenReturn(new LinkedHashSet<>(List.of("r1")));
        when(mapper.listAppMenus(anyString())).thenReturn(new ArrayList<>());
        when(mapper.listRoleMenus(anyString())).thenReturn(new ArrayList<>());
        when(mapper.listAppApis(anyString())).thenThrow(new RuntimeException("db down"));
        assertThrows(RuntimeException.class, () -> facade.loadAppResource("appA"));
    }

    // ========== 构造辅助 ==========

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
        AuthApi a = new AuthApi();
        a.setApiCode(apiCode);
        a.setApiMethod(apiMethod);
        a.setApiUri(apiUri);
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
