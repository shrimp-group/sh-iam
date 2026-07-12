package com.wkclz.iam.sso.rest;

import com.wkclz.auth.bean.Captcha;
import com.wkclz.auth.contract.auth.CaptchaService;
import com.wkclz.core.base.R;
import com.wkclz.iam.sso.Route;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Route.PREFIX)
@Validated
@Tag(name = "验证码", description = "图形验证码接口")
public class CaptchaRest {

    @Autowired
    private CaptchaService captchaService;

    @GetMapping(Route.PUBLIC_CAPTCHA_CHART)
    @Operation(summary = "获取图形验证码")
    public R<Captcha> getCaptcha() {
        Captcha captcha = captchaService.generate();
        return R.ok(captcha);
    }
}
