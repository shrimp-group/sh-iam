package com.wkclz.iam.contract.defaults.service;

import com.wkclz.iam.contract.bean.AuthResult;
import com.wkclz.iam.contract.enums.AuthErrorType;
import com.wkclz.iam.contract.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultAuthContract 单元测试
 * 验证读宽容验证严格行为
 *
 * @author shrimp
 */
class DefaultAuthContractTest {

    private final DefaultAuthContract contract = new DefaultAuthContract();

    @BeforeEach
    void setUp() {
        // 设置请求上下文，使 RequestHelper.getRequest() 能返回请求
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        com.wkclz.iam.contract.context.PrincipalContext.clear();
    }

    @Test
    void authenticate_noTokenInRequest_returnsNull() {
        // 无 token → 返回 null
        AuthResult result = contract.authenticate(new MockHttpServletRequest());
        assertNull(result, "无 token 时应返回 null");
    }

    @Test
    void authenticate_tokenExistsButNoImpl_throwsTokenInvalid() {
        // 有 token 但无实现 → 抛 TOKEN_INVALID
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer some-token");
        // 同步设置到 RequestContextHolder（PrincipalContext.getToken 从这里读）
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        AuthException ex = assertThrows(AuthException.class, () -> contract.authenticate(request));
        assertEquals(AuthErrorType.TOKEN_INVALID, ex.getErrorType());
    }

    @Test
    void checkToken_withToken_throwsTokenMissing() {
        // 有 token 但无实现 → doAuthenticate 抛 TOKEN_MISSING
        AuthException ex = assertThrows(AuthException.class,
                () -> contract.checkToken("any-token", "any-identifier"));
        assertEquals(AuthErrorType.TOKEN_MISSING, ex.getErrorType());
    }

    @Test
    void checkToken_nullToken_throwsTokenMissing() {
        AuthException ex = assertThrows(AuthException.class,
                () -> contract.checkToken(null, "any-identifier"));
        assertEquals(AuthErrorType.TOKEN_MISSING, ex.getErrorType());
    }

    @Test
    void checkToken_blankToken_throwsTokenMissing() {
        AuthException ex = assertThrows(AuthException.class,
                () -> contract.checkToken("", "any-identifier"));
        assertEquals(AuthErrorType.TOKEN_MISSING, ex.getErrorType());
    }
}
