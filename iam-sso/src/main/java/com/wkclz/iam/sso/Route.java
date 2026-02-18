package com.wkclz.iam.sso;

import com.wkclz.core.annotation.Desc;
import com.wkclz.core.annotation.Router;

@Router(module = "iam-sso", prefix = Route.PREFIX)
public interface Route {

    String PREFIX = "/iam-sso";

    // 登录模块
    @Desc("1. 验证码")
    String PUBLIC_CAPTCHA_CHART = "/public/captcha/chart";
    @Desc("2. sso 单点登录")
    String PUBLIC_SSO_LOGIN = "/public/sso/login";
    @Desc("3. sso 单点登出")
    String PUBLIC_SSO_LOGOUT = "/public/sso/logout";
    @Desc("4. sso 刷新令牌")
    String PUBLIC_SSO_REFRESH_TOKEN = "/public/sso/refresh-token";
    @Desc("5. sso 忘记密码")
    String PUBLIC_SSO_FORGOT_PASSWORD = "/public/sso/forgot-password";
    @Desc("6. sso 重置密码")
    String PUBLIC_SSO_RESET_PASSWORD = "/public/sso/reset-password";


    @Desc("4. 用户菜单树")
    String USER_MENU_TREE = "/user/menu/tree";
    @Desc("4. 用户菜单树")
    String USER_MENU_TREE_RUOYI = "/user/menu/tree/ruoyi";

    // 注册模块
    @Desc("1. sso 单点注册(发送验证码)")
    String PUBLIC_SSO_REGISTER = "/public/sso/register";
    @Desc("2. sso 注册验证")
    String PUBLIC_SSO_REGISTER_VERIFY = "/public/sso/register/verify";

    // 个人中心模块
    @Desc("1. 用户信息")
    String USER_INFO = "/user/info";
    @Desc("2. 更新用户信息")
    String USER_UPDATE = "/user/update";
    @Desc("3. 修改密码")
    String USER_CHANGE_PASSWORD = "/user/change-password";
    @Desc("4. 绑定手机号")
    String USER_BIND_PHONE = "/user/bind-phone";
    @Desc("5. 绑定邮箱")
    String USER_BIND_EMAIL = "/user/bind-email";
    @Desc("6. 登录记录")
    String USER_LOGIN_RECORDS = "/user/login-records";
    @Desc("7. 操作日志")
    String USER_OPERATE_RECORDS = "/user/operate-records";

    // 门户模块
    @Desc("1. 首页数据")
    String PORTAL_INDEX_DATA = "/portal/index-data";
    @Desc("2. 公告列表")
    String PORTAL_NOTICES = "/portal/notices";
    @Desc("3. 统计数据")
    String PORTAL_STATISTICS = "/portal/statistics";
    @Desc("4. 待办事项")
    String PORTAL_TODO_LIST = "/portal/todo-list";

}
