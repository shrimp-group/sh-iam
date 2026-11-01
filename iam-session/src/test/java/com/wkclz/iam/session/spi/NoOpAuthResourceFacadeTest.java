package com.wkclz.iam.session.spi;

import com.wkclz.iam.session.bean.auth.AppAuthResource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NoOpAuthResourceFacade 单元测试 — 兜底实现返回空资源且不抛异常。
 */
class NoOpAuthResourceFacadeTest {

    private final NoOpAuthResourceFacade facade = new NoOpAuthResourceFacade();

    @Test
    void loadAppResourceReturnsEmptyNonNullCollections() {
        AppAuthResource resource = facade.loadAppResource("iam-admin");
        assertNotNull(resource);
        assertEquals("iam-admin", resource.getAppCode());
        assertNotNull(resource.getRoleCodes());
        assertTrue(resource.getRoleCodes().isEmpty());
        assertNotNull(resource.getMenuMap());
        assertTrue(resource.getMenuMap().isEmpty());
        assertNotNull(resource.getRoleMenuMap());
        assertTrue(resource.getRoleMenuMap().isEmpty());
        assertNotNull(resource.getApiMap());
        assertTrue(resource.getApiMap().isEmpty());
        assertNotNull(resource.getMenuApiMap());
        assertTrue(resource.getMenuApiMap().isEmpty());
        assertNotNull(resource.getRequestControls());
        assertTrue(resource.getRequestControls().isEmpty());
        assertNotNull(resource.getApiFields());
        assertTrue(resource.getApiFields().isEmpty());
    }

    @Test
    void loadUserRoleCodesReturnsEmptyList() {
        List<String> roles = facade.loadUserRoleCodes("user_x");
        assertNotNull(roles);
        assertTrue(roles.isEmpty());
    }

    @Test
    void nullParamsDoNotThrow() {
        assertDoesNotThrow(() -> facade.loadAppResource(null));
        assertDoesNotThrow(() -> facade.loadUserRoleCodes(null));
    }
}
