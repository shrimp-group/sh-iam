package com.wkclz.iam.sso;

import com.wkclz.core.annotation.Router;

@Router(module = "iam-sso", prefix = Route.PREFIX)
public interface Route {

    String PREFIX = "/iam-sso";

    // 登录模块
    String PUBLIC_CAPTCHA_CHART = "/public/captcha/chart";
    String PUBLIC_SSO_LOGIN = "/public/sso/login";
    String PUBLIC_SSO_LOGOUT = "/public/sso/logout";
    String PUBLIC_SSO_FORGOT_PASSWORD = "/public/sso/forgot-password";
    String PUBLIC_SSO_RESET_PASSWORD = "/public/sso/reset-password";


    String USER_MENU_TREE = "/user/menu/tree";
    String USER_MENU_TREE_RUOYI = "/user/menu/tree/ruoyi";

    // 注册模块
    String PUBLIC_SSO_REGISTER = "/public/sso/register";
    String PUBLIC_SSO_REGISTER_VERIFY = "/public/sso/register/verify";

    // 个人中心模块
    String USER_INFO = "/user/info";
    String USER_UPDATE = "/user/update";
    String USER_CHANGE_PASSWORD = "/user/change-password";
    String USER_BIND_PHONE = "/user/bind-phone";
    String USER_BIND_EMAIL = "/user/bind-email";
    String USER_LOGIN_RECORDS = "/user/login-records";
    String USER_OPERATE_RECORDS = "/user/operate-records";

    // 门户模块
    String PORTAL_INDEX_DATA = "/portal/index-data";
    String PORTAL_NOTICES = "/portal/notices";
    String PORTAL_STATISTICS = "/portal/statistics";
    String PORTAL_TODO_LIST = "/portal/todo-list";

}
