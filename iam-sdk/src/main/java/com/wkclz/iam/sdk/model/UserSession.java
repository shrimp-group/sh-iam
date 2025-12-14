package com.wkclz.iam.sdk.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录后的缓存对象，用于控制用户会话过程
 */
@Data
public class UserSession implements Serializable {

    private String userCode;
    private String username;
    private String nickname;

}
