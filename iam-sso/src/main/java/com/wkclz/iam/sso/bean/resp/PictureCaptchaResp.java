package com.wkclz.iam.sso.bean.resp;

import java.io.Serializable;

/**
 * 图形验证码生成响应。
 */
public class PictureCaptchaResp implements Serializable {

    private final String captchaId;
    private final String base64Image;

    public PictureCaptchaResp(String captchaId, String base64Image) {
        this.captchaId = captchaId;
        this.base64Image = base64Image;
    }

    public String getCaptchaId() {
        return captchaId;
    }

    public String getBase64Image() {
        return base64Image;
    }
}
