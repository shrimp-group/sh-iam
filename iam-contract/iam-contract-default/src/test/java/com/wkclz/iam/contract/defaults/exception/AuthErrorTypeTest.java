package com.wkclz.iam.contract.defaults.exception;

import com.wkclz.iam.contract.enums.AuthErrorType;
import com.wkclz.iam.contract.enums.JwtErrorCodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AuthErrorType 枚举单元测试
 * 验证 HTTP 状态码映射与友好提示语
 *
 * @author shrimp
 */
class AuthErrorTypeTest {

    @Test
    void allValuesHaveHttpStatus() {
        for (AuthErrorType type : AuthErrorType.values()) {
            assertTrue(type.getHttpStatus() > 0, type.name() + " 的 httpStatus 应大于 0");
        }
    }

    @Test
    void allValuesHaveNonNullNonBlankMessage() {
        for (AuthErrorType type : AuthErrorType.values()) {
            assertNotNull(type.getMessage(), type.name() + " 的 message 不应为 null");
            assertFalse(type.getMessage().isBlank(), type.name() + " 的 message 不应为空串");
        }
    }

    @Test
    void tokenRelatedTypesHave401() {
        assertEquals(401, AuthErrorType.TOKEN_MISSING.getHttpStatus());
        assertEquals(401, AuthErrorType.TOKEN_INVALID.getHttpStatus());
        assertEquals(401, AuthErrorType.TOKEN_EXPIRED.getHttpStatus());
        assertEquals(401, AuthErrorType.SESSION_EXPIRED.getHttpStatus());
    }

    @Test
    void akSignTypesHave401() {
        assertEquals(401, AuthErrorType.AK_SIGN_INVALID.getHttpStatus());
        assertEquals(401, AuthErrorType.AK_SIGN_EXPIRED.getHttpStatus());
        assertEquals(401, AuthErrorType.AK_NONCE_REPLAY.getHttpStatus());
    }

    @Test
    void accessDeniedHas403() {
        assertEquals(403, AuthErrorType.ACCESS_DENIED.getHttpStatus());
    }

    @Test
    void accessDeniedMessageIsAccessDenied() {
        assertEquals("接口鉴权拒绝", AuthErrorType.ACCESS_DENIED.getMessage());
    }

    @Test
    void tokenMissingMessageIsTokenNotFound() {
        assertEquals("token 不存在", AuthErrorType.TOKEN_MISSING.getMessage());
    }

    @Test
    void enumCountIs8() {
        assertEquals(8, AuthErrorType.values().length, "枚举数量应为 8");
    }

    @Test
    void fromJwtErrorCode_expired_returnsTokenExpired() {
        assertEquals(AuthErrorType.TOKEN_EXPIRED, AuthErrorType.fromJwtErrorCode(JwtErrorCodes.EXPIRED));
    }

    @Test
    void fromJwtErrorCode_signature_returnsTokenInvalid() {
        assertEquals(AuthErrorType.TOKEN_INVALID, AuthErrorType.fromJwtErrorCode(JwtErrorCodes.SIGNATURE));
    }

    @Test
    void fromJwtErrorCode_malformed_returnsTokenInvalid() {
        assertEquals(AuthErrorType.TOKEN_INVALID, AuthErrorType.fromJwtErrorCode(JwtErrorCodes.MALFORMED));
    }

    @Test
    void fromJwtErrorCode_unsupported_returnsTokenInvalid() {
        assertEquals(AuthErrorType.TOKEN_INVALID, AuthErrorType.fromJwtErrorCode(JwtErrorCodes.UNSUPPORTED));
    }

    @Test
    void fromJwtErrorCode_illegalArgument_returnsTokenInvalid() {
        assertEquals(AuthErrorType.TOKEN_INVALID, AuthErrorType.fromJwtErrorCode(JwtErrorCodes.ILLEGAL_ARGUMENT));
    }

    @Test
    void fromJwtErrorCode_null_returnsTokenInvalid() {
        assertEquals(AuthErrorType.TOKEN_INVALID, AuthErrorType.fromJwtErrorCode(null));
    }

    @Test
    void fromJwtErrorCode_unknown_returnsTokenInvalid() {
        assertEquals(AuthErrorType.TOKEN_INVALID, AuthErrorType.fromJwtErrorCode("UNKNOWN_CODE"));
    }
}
