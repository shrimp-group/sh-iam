package com.wkclz.iam.sso;

import com.wkclz.core.annotation.Desc;
import com.wkclz.core.annotation.Router;

@Router(value = "iam-sso")
public interface Route {

    String PREFIX = "/iam-sso";


    @Desc("sso 验证码")
    String PUBLIC_CAPTCHA_CHART = "/public/captcha/chart";
    @Desc("sso 单点登录")
    String PUBLIC_SSO_LOGIN = "/public/sso/login";
    @Desc("sso 单点登出")
    String PUBLIC_SSO_LOGOUT = "/public/sso/logout";


    @Desc("sso 用户信息")
    String USER_INFO = "/user/info";
    @Desc("sso 用户菜单树")
    String USER_MENU_TREE = "/user/menu/tree";

}
