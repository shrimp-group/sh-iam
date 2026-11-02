package com.wkclz.iam.session.service;

import com.wkclz.iam.session.bean.auth.AppAuthResource;
import com.wkclz.iam.session.bean.auth.AuthApi;
import com.wkclz.iam.session.bean.auth.AuthMenu;
import com.wkclz.iam.session.cache.AuthCacheManager;
import com.wkclz.iam.session.cache.AuthComputedCache;
import com.wkclz.iam.session.config.IamSessionConfig;
import com.wkclz.iam.session.spi.AuthResourceFacade;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AuthPermissionService 单元测试 — 角色交集、祖先链补全、按钮过滤、单 URL 权限计算、
 * 纳管判定、fail-open 与缓存快照不可变（fake facade，纯内存不依赖外部资源）。
 */
class AuthPermissionServiceTest {

    /**
     * 菜单/角色静态数据：
     * <pre>
     * M1 (根)
     *  ├── M11
     *  └── M1_BTN (按钮 btn_1)   ← R1 绑定
     * M2 (根)
     *  └── M2_BTN (按钮 btn_2)   ← 未绑定任何角色
     * M3 (根)                     ← 未被 R2 直接绑定，仅 R2 绑深层 M31（祖先链补全）
     *  └── M31                    ← R2 绑定，绑定 api_2（GET /a/**）
     * </pre>
     * API：api_1 (GET /a/b) 属 M1；api_2 (GET /a/**) 属 M31
     */
    private static class FakeFacade implements AuthResourceFacade {

        final AppAuthResource resource = buildResource();
        boolean failLoads = false;

        @Override
        public AppAuthResource loadAppResource(String appCode) {
            if (failLoads) {
                throw new RuntimeException("facade down");
            }
            return resource;
        }

        @Override
        public List<String> loadUserRoleCodes(String userCode) {
            if (failLoads) {
                throw new RuntimeException("facade down");
            }
            // userR1 携带应用外角色 R9（交集过滤验证点）
            return "userR1R2".equals(userCode) ? List.of("R1", "R2") : List.of("R1", "R9");
        }

        private AppAuthResource buildResource() {
            AppAuthResource res = AppAuthResource.empty("app");
            res.setRoleCodes(new LinkedHashSet<>(List.of("R1", "R2")));

            res.getMenuMap().put("M1", menu("M1", "0", "MENU", null));
            res.getMenuMap().put("M11", menu("M11", "M1", "MENU", null));
            res.getMenuMap().put("M1_BTN", menu("M1_BTN", "M1", "BUTTON", "btn_1"));
            res.getMenuMap().put("M2", menu("M2", "0", "MENU", null));
            res.getMenuMap().put("M2_BTN", menu("M2_BTN", "M2", "BUTTON", "btn_2"));
            res.getMenuMap().put("M3", menu("M3", "0", "MENU", null));
            res.getMenuMap().put("M31", menu("M31", "M3", "MENU", null));

            res.getRoleMenuMap().put("R1", new LinkedHashSet<>(List.of("M1", "M11", "M1_BTN", "M2")));
            res.getRoleMenuMap().put("R2", new LinkedHashSet<>(List.of("M31")));

            AuthApi api1 = new AuthApi();
            api1.setApiCode("api_1");
            api1.setApiMethod("GET");
            api1.setApiUri("/a/b");
            AuthApi api2 = new AuthApi();
            api2.setApiCode("api_2");
            api2.setApiMethod("GET");
            api2.setApiUri("/a/**");
            res.getApiMap().put("api_1", api1);
            res.getApiMap().put("api_2", api2);

            res.getMenuApiMap().put("M1", Set.of("api_1"));
            res.getMenuApiMap().put("M31", Set.of("api_2"));
            return res;
        }

        private AuthMenu menu(String code, String parentCode, String type, String buttonCode) {
            AuthMenu menu = new AuthMenu();
            menu.setMenuCode(code);
            menu.setParentCode(parentCode);
            menu.setMenuName(code);
            menu.setMenuType(type);
            menu.setButtonCode(buttonCode);
            menu.setSort(1);
            return menu;
        }
    }

    private final FakeFacade facade = new FakeFacade();
    private final AuthCacheManager manager = new AuthCacheManager(facade, new AuthComputedCache(config()), config(),
        org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class));
    private final AuthPermissionService service = createService();

    /**
     * AuthPermissionService 为 @Resource 字段注入，测试经反射装配（对齐主代码注入风格）
     */
    private AuthPermissionService createService() {
        AuthPermissionService authPermissionService = new AuthPermissionService();
        ReflectionTestUtils.setField(authPermissionService, "manager", manager);
        return authPermissionService;
    }

    private IamSessionConfig config() {
        IamSessionConfig config = new IamSessionConfig();
        config.setAuthCacheStaticMaxApps(10);
        config.setAuthCacheUserMaxUsers(100);
        config.setAuthCacheUserIdleSeconds(1800);
        config.setAuthCacheComputedTtlSeconds(300);
        config.setAuthCacheComputedMaxUrls(100);
        return config;
    }

    /**
     * 展平树为菜单编码集合
     */
    private Set<String> flatten(List<AuthMenu> roots) {
        Set<String> codes = new LinkedHashSet<>();
        collect(roots, codes);
        return codes;
    }

    private void collect(List<AuthMenu> nodes, Set<String> codes) {
        for (AuthMenu node : nodes) {
            codes.add(node.getMenuCode());
            if (node.getChildren() != null) {
                collect(node.getChildren(), codes);
            }
        }
    }

    @Test
    void roleIntersectionFiltersAppExternalRoles() {
        // userR1 角色 [R1, R9]，R9 不属应用 → 仅 R1 授权部分
        List<AuthMenu> tree = service.getUserMenuTree("userR1", "app");
        Set<String> codes = flatten(tree);
        assertTrue(codes.containsAll(Set.of("M1", "M11", "M1_BTN", "M2")));
        assertFalse(codes.contains("M3"));
        assertFalse(codes.contains("M31"));
    }

    @Test
    void ancestorChainCompletedForDeepBoundMenu() {
        // R2 仅绑深层 M31 → 祖先 M3 由闭包补全
        List<AuthMenu> tree = service.getUserMenuTree("userR1R2", "app");
        Set<String> codes = flatten(tree);
        assertTrue(codes.contains("M31"));
        assertTrue(codes.contains("M3"));
    }

    @Test
    void unboundButtonFilteredOut() {
        List<AuthMenu> tree = service.getUserMenuTree("userR1", "app");
        Set<String> codes = flatten(tree);
        // 已绑定按钮出现
        assertTrue(codes.contains("M1_BTN"));
        // 未绑定任何角色的按钮被过滤
        assertFalse(codes.contains("M2_BTN"));
        List<String> buttonCodes = service.getUserButtonCodes("userR1", "app");
        assertEquals(List.of("btn_1"), buttonCodes);
    }

    @Test
    void singleUrlPermissionComputedCorrectly() {
        // R1 经 M1 → api_1 (GET /a/b)
        assertTrue(service.hasApiPermission("app", "userR1", "GET", "/a/b"));
        // R1 无 M3 → api_2 不可见，POST /a/b 拒绝
        assertFalse(service.hasApiPermission("app", "userR1", "POST", "/a/b"));
        // GET /a/x 仅命中 api_2 (GET /a/**，属 M31) → R1 无 M31，false
        assertFalse(service.hasApiPermission("app", "userR1", "GET", "/a/x"));
        // R1+R2 经 M31 → api_2 → true
        assertTrue(service.hasApiPermission("app", "userR1R2", "GET", "/a/x"));
    }

    @Test
    void managedApiDetection() {
        // 已纳管：任一菜单绑定 API 匹配
        assertTrue(service.isManagedApi("app", "GET", "/a/b"));
        assertTrue(service.isManagedApi("app", "GET", "/a/anything"));
        // 未纳管：放行
        assertFalse(service.isManagedApi("app", "GET", "/not/bound"));
    }

    @Test
    void computationErrorFailsOpen() {
        facade.failLoads = true;
        assertTrue(service.hasApiPermission("app", "userR1", "GET", "/a/b"));
    }

    @Test
    void treeComputationDoesNotMutateCachedSnapshot() {
        List<AuthMenu> tree = service.getUserMenuTree("userR1R2", "app");
        // 本次结果含 M3/M31
        assertTrue(flatten(tree).containsAll(Set.of("M3", "M31")));
        // 缓存快照不可变：menuMap 内节点 children 必须保持 null（浅拷贝约束）
        for (AuthMenu node : facade.resource.getMenuMap().values()) {
            assertNull(node.getChildren(), "缓存快照节点被污染: " + node.getMenuCode());
        }
        assertEquals(7, facade.resource.getMenuMap().size());
        // 二次计算生成全新节点实例（children 不共享）
        List<AuthMenu> second = service.getUserMenuTree("userR1R2", "app");
        assertTrue(tree.get(0) != second.get(0));
        assertTrue(tree.get(0).getChildren() != second.get(0).getChildren());
    }
}
