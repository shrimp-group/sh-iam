package com.wkclz.iam.contract.defaults.service;

import com.wkclz.iam.contract.enums.AuthErrorType;
import com.wkclz.iam.contract.exception.AuthException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultAkSignContract 单元测试
 * 验证 sign/verifySign 均抛异常
 *
 * @author shrimp
 */
class DefaultAkSignContractTest {

    private final DefaultAkSignContract contract = new DefaultAkSignContract();

    @Test
    void sign_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class,
                () -> contract.sign("appId", "appSecret"));
    }

    @Test
    void verifySign_throwsAkSignInvalid() {
        AuthException ex = assertThrows(AuthException.class,
                () -> contract.verifySign("any-sign", "publicKey", "appId"));
        assertEquals(AuthErrorType.AK_SIGN_INVALID, ex.getErrorType());
    }
}
