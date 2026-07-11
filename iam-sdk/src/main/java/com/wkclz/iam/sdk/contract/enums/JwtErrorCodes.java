package com.wkclz.iam.sdk.contract.enums;

/**
 * JWT 错误码常量
 * 契约层定义的 JWT 校验错误码，不依赖具体 JWT 库实现
 * iam-sdk 的 JwtValidationException.errorCode 值与此保持一致
 * <p>
 * 配合 {@link AuthErrorType#fromJwtErrorCode(String)} 使用，
 * 减少实现方 JWT 异常到 AuthErrorType 的样板映射代码
 *
 * @author shrimp
 */
public final class JwtErrorCodes {

    private JwtErrorCodes() {
    }

    /**
     * JWT 已过期
     */
    public static final String EXPIRED = "JWT_EXPIRED";

    /**
     * JWT 签名无效
     */
    public static final String SIGNATURE = "JWT_SIGNATURE_ERROR";

    /**
     * JWT 格式错误
     */
    public static final String MALFORMED = "JWT_MALFORMED";

    /**
     * JWT 不支持的算法
     */
    public static final String UNSUPPORTED = "JWT_UNSUPPORTED";

    /**
     * JWT 参数非法
     */
    public static final String ILLEGAL_ARGUMENT = "JWT_ILLEGAL_ARGUMENT";
}
