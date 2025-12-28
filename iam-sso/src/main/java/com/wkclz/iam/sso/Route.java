package com.wkclz.iam.sso;

import com.wkclz.core.annotation.Desc;
import com.wkclz.core.annotation.Router;

@Router(value = "iam-sso")
public interface Route {



    @Desc("sso 验证码")
    String PUBLIC_CAPTCHA_CHART = "/iam-sso/public/captcha/chart";

    @Desc("sso 单点登录")
    String PUBLIC_SSO_LOGIN = "/iam-sso/public/sso/login";

    @Desc("sso 用户信息")
    String USER_INFO = "/iam-sso/user/info";


}
