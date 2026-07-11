package com.wkclz.iam.sdk.contract.enums;

/**
 * 认证错误类型枚举
 * 枚举内携带 HTTP 状态码与友好提示语
 * 由 AuthException 持有，DefaultAuthFilter 根据 getHttpStatus() 设置响应状态码
 *
 * @author shrimp
 */
@Deprecated
public enum AuthErrorType {

    /**
     * token 不存在
     */
    TOKEN_MISSING(401, "token 不存在"),
    /**
     * JWT 签名无效
     */
    TOKEN_INVALID(401, "JWT 签名无效"),
    /**
     * JWT 已过期
     */
    TOKEN_EXPIRED(401, "JWT 已过期"),
    /**
     * 会话已过期（如 Redis 无记录）
     */
    SESSION_EXPIRED(401, "会话已过期"),
    /**
     * AK 签名无效
     */
    AK_SIGN_INVALID(401, "AK 签名无效"),
    /**
     * AK 签名已过期
     */
    AK_SIGN_EXPIRED(401, "AK 签名已过期"),
    /**
     * nonce 重放检测命中
     */
    AK_NONCE_REPLAY(401, "重放检测命中"),
    /**
     * 接口鉴权拒绝
     */
    ACCESS_DENIED(403, "接口鉴权拒绝");

    private final int httpStatus;
    private final String message;

    AuthErrorType(int httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 从 JWT 错误码映射认证错误类型
     * <p>
     * 实现方捕获 JWT 异常后，直接传入异常携带的 errorCode 即可获得对应 AuthErrorType，
     * 无需手动编写 switch 映射：
     * <pre>{@code
     * throw new AuthException(AuthErrorType.fromJwtErrorCode(e.getErrorCode()), e.getMessage(), e);
     * }</pre>
     *
     * @param errorCode JWT 错误码，取值参见 {@link JwtErrorCodes}
     * @return 对应的认证错误类型；无法识别时兜底返回 {@link #TOKEN_INVALID}
     */
    public static AuthErrorType fromJwtErrorCode(String errorCode) {
        if (errorCode == null) {
            return TOKEN_INVALID;
        }
        return switch (errorCode) {
            case JwtErrorCodes.EXPIRED -> TOKEN_EXPIRED;
            case JwtErrorCodes.SIGNATURE, JwtErrorCodes.MALFORMED,
                 JwtErrorCodes.UNSUPPORTED, JwtErrorCodes.ILLEGAL_ARGUMENT -> TOKEN_INVALID;
            default -> TOKEN_INVALID;
        };
    }
}
