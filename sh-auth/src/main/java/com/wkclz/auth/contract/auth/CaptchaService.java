package com.wkclz.auth.contract.auth;

import com.wkclz.auth.bean.Captcha;

public interface CaptchaService {
    Captcha generate();
    boolean verify(String captchaId, String captchaCode);
}
