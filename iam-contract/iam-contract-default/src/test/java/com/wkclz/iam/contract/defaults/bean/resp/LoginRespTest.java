package com.wkclz.iam.contract.defaults.bean.resp;

import com.wkclz.iam.contract.bean.resp.LoginResp;
import com.wkclz.iam.contract.enums.LoginFailType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoginResp 失败建模单元测试
 *
 * @author shrimp
 */
class LoginRespTest {

    @Test
    void success_setsSuccessFields_andNullFailFields() {
        LoginResp resp = LoginResp.success("token-123", "U001", "admin", "管理员", "avatar.png");

        assertTrue(resp.getSuccess());
        assertNull(resp.getFailType());
        assertNull(resp.getFailReason());
        assertEquals("token-123", resp.getToken());
        assertEquals("U001", resp.getUserCode());
        assertEquals("admin", resp.getUsername());
        assertEquals("管理员", resp.getNickname());
        assertEquals("avatar.png", resp.getAvatar());
    }

    @Test
    void failWithoutReason_setsFailFields_andNullSuccessFields() {
        LoginResp resp = LoginResp.fail(LoginFailType.ACCOUNT_LOCKED);

        assertFalse(resp.getSuccess());
        assertEquals(LoginFailType.ACCOUNT_LOCKED, resp.getFailType());
        assertNull(resp.getFailReason());
        assertNull(resp.getToken());
        assertNull(resp.getUserCode());
        assertNull(resp.getUsername());
        assertNull(resp.getNickname());
        assertNull(resp.getAvatar());
    }

    @Test
    void failWithReason_setsFailFieldsAndReason_andNullSuccessFields() {
        LoginResp resp = LoginResp.fail(LoginFailType.ACCOUNT_LOCKED, "请 300 秒后重试");

        assertFalse(resp.getSuccess());
        assertEquals(LoginFailType.ACCOUNT_LOCKED, resp.getFailType());
        assertEquals("请 300 秒后重试", resp.getFailReason());
        assertNull(resp.getToken());
        assertNull(resp.getUserCode());
        assertNull(resp.getUsername());
        assertNull(resp.getNickname());
        assertNull(resp.getAvatar());
    }

    @Test
    void invariant_successImpliesNullFailType() {
        LoginResp resp = LoginResp.success("t", "u", "n", "nick", "a");
        // 语义不变量：success=true 时 failType 必为 null
        assertTrue(resp.getSuccess());
        assertNull(resp.getFailType());
        assertNull(resp.getFailReason());
    }

    @Test
    void invariant_failImpliesNonNullFailType() {
        LoginResp resp = LoginResp.fail(LoginFailType.UNKNOWN);
        // 语义不变量：success=false 时 failType 必非 null
        assertFalse(resp.getSuccess());
        assertNotNull(resp.getFailType());
    }

    @Test
    void failTypeMessageAccessibleFromResp() {
        // 枚举内完成翻译，前端可通过 failType.getMessage() 获取固定中文标签
        LoginResp resp = LoginResp.fail(LoginFailType.CREDENTIALS_EXPIRED);
        assertEquals("凭据已过期", resp.getFailType().getMessage());
    }
}
