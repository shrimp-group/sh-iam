package com.wkclz.iam.sdk.exception;

import com.wkclz.core.exception.UnauthorizedException;

/**
 * JWT 校验异常
 * 保留原始异常类型信息，调用方可根据 errorCode 区分过期、签名错误等不同情况
 *
 * @author shrimp
 */
public class JwtValidationException extends UnauthorizedException {

    /**
     * Token 已过期
     */
    public static final String CODE_EXPIRED = "JWT_EXPIRED";
    /**
     * Token 格式错误
     */
    public static final String CODE_MALFORMED = "JWT_MALFORMED";
    /**
     * 不支持的 Token
     */
    public static final String CODE_UNSUPPORTED = "JWT_UNSUPPORTED";
    /**
     * Token 签名错误
     */
    public static final String CODE_SIGNATURE = "JWT_SIGNATURE_ERROR";
    /**
     * Token 参数错误
     */
    public static final String CODE_ILLEGAL_ARGUMENT = "JWT_ILLEGAL_ARGUMENT";

    private final String errorCode;

    public JwtValidationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }


    public String getErrorCode() {
        return errorCode;
    }
}
