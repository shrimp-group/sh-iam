package com.wkclz.iam.contract.defaults.enums;

import com.wkclz.iam.contract.enums.LoginFailType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoginFailType 枚举单元测试
 *
 * @author shrimp
 */
class LoginFailTypeTest {

    @Test
    void allValuesHaveNonNullNonBlankMessage() {
        for (LoginFailType type : LoginFailType.values()) {
            assertNotNull(type.getMessage(), type.name() + " 的 message 不应为 null");
            assertFalse(type.getMessage().isBlank(), type.name() + " 的 message 不应为空串");
        }
    }

    @Test
    void enumCountIsTen() {
        assertEquals(10, LoginFailType.values().length, "枚举数量应为 10");
    }

    @Test
    void keyValuesExist() {
        assertNotNull(LoginFailType.valueOf("USERNAME_OR_PASSWORD_ERROR"));
        assertNotNull(LoginFailType.valueOf("ACCOUNT_DISABLED"));
        assertNotNull(LoginFailType.valueOf("ACCOUNT_LOCKED"));
        assertNotNull(LoginFailType.valueOf("CREDENTIALS_EXPIRED"));
        assertNotNull(LoginFailType.valueOf("CAPTCHA_REQUIRED"));
        assertNotNull(LoginFailType.valueOf("CAPTCHA_ERROR"));
        assertNotNull(LoginFailType.valueOf("TENANT_INVALID"));
        assertNotNull(LoginFailType.valueOf("AUTH_TYPE_UNSUPPORTED"));
        assertNotNull(LoginFailType.valueOf("AUTH_IDENTIFIER_INVALID"));
        assertNotNull(LoginFailType.valueOf("UNKNOWN"));
    }

    @Test
    void usernameOrPasswordErrorMergedForSecurity() {
        // 用户名错误与密码错误合并为单一枚举值，防用户枚举攻击
        assertEquals("用户名或密码错误", LoginFailType.USERNAME_OR_PASSWORD_ERROR.getMessage());
    }

    @Test
    void unknownMessageIsGeneric() {
        assertEquals("登录失败", LoginFailType.UNKNOWN.getMessage());
    }
}
