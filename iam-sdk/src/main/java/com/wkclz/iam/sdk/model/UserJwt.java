package com.wkclz.iam.sdk.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录后的JWT对象，用于给前端传递信息
 * @author shrimp
 */
@Data
public class UserJwt implements Serializable {

    // 用户编码
    private String userCode;
    // 用户名
    private String username;
    // 昵称
    private String nickname;
    // 头像
    private String avatar;

}
