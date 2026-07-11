package com.wkclz.iam.sdk.contract.enums;

/**
 * 鉴权场景枚举
 *
 * @author shrimp
 */
public enum AuthScene {
    /**
     * JWT Token 认证
     */
    TOKEN,
    /**
     * AK 签名认证
     */
    AK_SIGN,
    /**
     * 公开接口（无需认证）
     */
    PUBLIC
}
