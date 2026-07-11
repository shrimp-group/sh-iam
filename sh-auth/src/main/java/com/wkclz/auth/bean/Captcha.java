package com.wkclz.auth.bean;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 图形验证码 */
@Data
public class Captcha implements Serializable {
    private String captchaId;
    private String captchaCode;
    private String captchaImage;
    private LocalDateTime expireTime;
}
