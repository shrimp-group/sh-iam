package com.wkclz.iam.sso.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.sdk.helper.CaptchaHelper;
import com.wkclz.iam.sdk.model.PictureCaptchaResponse;
import com.wkclz.iam.sso.Route;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(Route.PREFIX)
public class CaptchaRest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @GetMapping(Route.PUBLIC_CAPTCHA_CHART)
    public R getCaptcha() {
        PictureCaptchaResponse captcha = CaptchaHelper.getCaptcha();
        String captchaId = captcha.getCaptchaId();
        String captchaCode = captcha.getCaptchaCode();


        String redisKey = CaptchaHelper.getCaptchaRedisKey(captchaId);
        long ttl = captcha.getExpireTime() - System.currentTimeMillis() + 10_000;

        // 将验证码存储到Redis，设置过期时间
        redisTemplate.opsForValue().set(redisKey, captchaCode, ttl, TimeUnit.MILLISECONDS);

        captcha.setCaptchaCode(null);
        return R.ok(captcha);
    }
}
