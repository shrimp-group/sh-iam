package com.wkclz.auth.context;

import com.wkclz.auth.bean.Principal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityContextTest {

    @AfterEach
    void tearDown() {
        SecurityContext.clear();
    }

    @Test
    void testSetAndGetPrincipal() {
        Principal principal = new Principal();
        principal.setUserCode("user_001");
        principal.setUsername("admin");

        SecurityContext.setPrincipal(principal);

        assertNotNull(SecurityContext.getPrincipal());
        assertEquals("user_001", SecurityContext.getUserCode());
        assertEquals("admin", SecurityContext.getUsername());
    }

    @Test
    void testGetPrincipalWhenNull() {
        assertNull(SecurityContext.getPrincipal());
        assertNull(SecurityContext.getUserCode());
        assertNull(SecurityContext.getUsername());
    }

    @Test
    void testSetAndGetToken() {
        SecurityContext.setToken("test-token-123");
        assertEquals("test-token-123", SecurityContext.getToken());
    }

    @Test
    void testSetAndGetAppCode() {
        SecurityContext.setAppCode("test-app");
        assertEquals("test-app", SecurityContext.getAppCode());
    }

    @Test
    void testSetAndGetTenantCode() {
        SecurityContext.setTenantCode("test-tenant");
        assertEquals("test-tenant", SecurityContext.getTenantCode());
    }

    @Test
    void testClear() {
        Principal principal = new Principal();
        principal.setUserCode("user_001");
        SecurityContext.setPrincipal(principal);
        SecurityContext.setToken("token");
        SecurityContext.setTenantCode("tenant");
        SecurityContext.setAppCode("app");

        SecurityContext.clear();

        assertNull(SecurityContext.getPrincipal());
        assertNull(SecurityContext.getToken());
        assertNull(SecurityContext.getTenantCode());
        assertNull(SecurityContext.getAppCode());
    }

    @Test
    void testTenantCodeIndependentOfPrincipal() {
        // tenantCode 是运行时参数，不依赖 Principal
        SecurityContext.setTenantCode("tenant-001");
        assertEquals("tenant-001", SecurityContext.getTenantCode());
        assertNull(SecurityContext.getPrincipal());

        Principal p = new Principal();
        p.setUserCode("user_001");
        SecurityContext.setPrincipal(p);
        assertEquals("tenant-001", SecurityContext.getTenantCode()); // 不受 Principal 影响

        SecurityContext.setTenantCode("tenant-002");
        assertEquals("user_001", SecurityContext.getPrincipal().getUserCode()); // Principal 不受 tenantCode 影响
    }
}
