package com.wkclz.iam.session.bean;

import lombok.Data;

import java.io.Serializable;

/**
 * 从 Token 解析出的最小信息集。
 */
@Data
public class TokenInfo implements Serializable {

    /**
     * 用户编码
     */
    private String userCode;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * JWT 签发时间戳（毫秒），用于快速判断 Redis 是否必然未过期
     */
    private Long issuedAt;

    /**
     * JWT 过期时间戳（毫秒）
     */
    private Long expireAt;

}
