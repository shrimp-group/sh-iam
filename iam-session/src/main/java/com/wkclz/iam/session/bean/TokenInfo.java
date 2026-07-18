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

}
