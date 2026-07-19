package com.wkclz.iam.session.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthType 认证方式枚举")
class AuthTypeTest {

    @Test
    @DisplayName("包含全部 5 种认证方式")
    void shouldHaveAllFiveTypes() {
        AuthType[] values = AuthType.values();
        assertEquals(5, values.length, "AuthType 应包含 5 种认证方式");
    }

    @Test
    @DisplayName("PASSWORD 密码登录")
    void password() {
        assertEquals("密码登录", AuthType.PASSWORD.getDesc());
    }

    @Test
    @DisplayName("WECHAT_MINI 微信小程序")
    void wechatMini() {
        assertEquals("微信小程序", AuthType.WECHAT_MINI.getDesc());
    }

    @Test
    @DisplayName("WECHAT_MP 微信公众号")
    void wechatMp() {
        assertEquals("微信公众号", AuthType.WECHAT_MP.getDesc());
    }

    @Test
    @DisplayName("LDAP 认证")
    void ldap() {
        assertEquals("LDAP", AuthType.LDAP.getDesc());
    }

    @Test
    @DisplayName("OAUTH 认证")
    void oauth() {
        assertEquals("OAuth", AuthType.OAUTH.getDesc());
    }

    @Test
    @DisplayName("valueOf 可正确解析枚举值")
    void valueOf() {
        assertEquals(AuthType.WECHAT_MP, AuthType.valueOf("WECHAT_MP"));
        assertEquals(AuthType.WECHAT_MINI, AuthType.valueOf("WECHAT_MINI"));
    }

}
