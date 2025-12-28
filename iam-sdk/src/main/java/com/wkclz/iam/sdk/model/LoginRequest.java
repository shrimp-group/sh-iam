package com.wkclz.iam.sdk.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class LoginRequest implements Serializable {

    /**
     * 用户名 【用户名/手机号/邮箱账号/工号, 需根据具体特点进行区分】
     */
    private String username;

    /**
     * 密码 【密码，短信验证码，邮件验证码 等值】
     */
    private String password;


    private String captchaCode;
    private String captchaId;


}
