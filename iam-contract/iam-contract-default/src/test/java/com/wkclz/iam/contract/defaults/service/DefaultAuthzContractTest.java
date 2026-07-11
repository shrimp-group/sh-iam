package com.wkclz.iam.contract.defaults.service;

import com.wkclz.iam.contract.bean.App;
import com.wkclz.iam.contract.bean.DataDimension;
import com.wkclz.iam.contract.bean.FieldPermission;
import com.wkclz.iam.contract.bean.Menu;
import com.wkclz.iam.contract.bean.Principal;
import com.wkclz.iam.contract.bean.Tenant;
import com.wkclz.iam.contract.enums.AuthErrorType;
import com.wkclz.iam.contract.exception.AuthException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultAuthzContract 单元测试
 * 验证读宽容（返回空/原值）、验证严格（canAccessApi 抛异常）
 *
 * @author shrimp
 */
class DefaultAuthzContractTest {

    private final DefaultAuthzContract contract = new DefaultAuthzContract();
    private final Principal principal = new Principal();

    @Test
    void listTenants_returnsEmptyList() {
        List<Tenant> result = contract.listTenants(principal);
        assertNotNull(result);
        assertTrue(result.isEmpty(), "无实现时应返回空列表");
    }

    @Test
    void listApps_returnsEmptyList() {
        List<App> result = contract.listApps(principal, "tenant-001");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getMenuTree_returnsEmptyList() {
        List<Menu> result = contract.getMenuTree(principal, "app-001");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void listFieldPermissions_returnsEmptyList() {
        List<FieldPermission> result = contract.listFieldPermissions(principal, "app-001", "menu-001");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void filterFields_returnsOriginalFields() {
        // 字段过滤无实现 → 返回原字段列表（不过滤）
        List<String> fields = Arrays.asList("name", "email", "phone");
        List<String> result = contract.filterFields(principal, "app-001", "menu-001", fields);
        assertEquals(fields, result, "无实现时应返回原字段列表");
    }

    @Test
    void filterFields_emptyInput_returnsEmpty() {
        List<String> result = contract.filterFields(principal, "app-001", "menu-001", Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    void getDataDimensions_returnsEmptyList() {
        List<DataDimension> result = contract.getDataDimensions(principal, "app-001");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void canAccessApi_throwsAccessDenied() {
        // 接口鉴权无实现 → 抛 ACCESS_DENIED
        AuthException ex = assertThrows(AuthException.class,
                () -> contract.canAccessApi(principal, "app-001", "/api/test", "GET"));
        assertEquals(AuthErrorType.ACCESS_DENIED, ex.getErrorType());
    }
}
