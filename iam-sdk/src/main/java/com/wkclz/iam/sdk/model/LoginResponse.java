package com.wkclz.iam.sdk.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class LoginResponse implements Serializable {

    /**
     * 登录状态
     */
    private Integer loginStatus;

    /**
     * 登录信息
     */
    private String loginMessage;

    /**
     * 登录token, jwt
     */
    private String token;

}
