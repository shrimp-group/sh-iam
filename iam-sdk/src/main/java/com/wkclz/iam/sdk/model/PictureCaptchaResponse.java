package com.wkclz.iam.sdk.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 验证码响应对象
 * @author shrimp
 */
@Data
public class PictureCaptchaResponse implements Serializable {

    private String captchaCode;
    private String captchaId;
    private String captchaImage;
    private long expireTime;

}
