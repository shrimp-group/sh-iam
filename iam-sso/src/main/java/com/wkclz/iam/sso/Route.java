package com.wkclz.iam.sso;

import com.wkclz.core.annotation.ApiDesc;
import com.wkclz.core.annotation.Router;

@Router(module = "iam-sso", prefix = Route.PREFIX)
public interface Route {

    String PREFIX = "/iam-sso";

    // 登录模块
    @ApiDesc("1. 验证码")
    String PUBLIC_CAPTCHA_CHART = "/public/captcha/chart";
    @ApiDesc("2. sso 单点登录")
    String PUBLIC_SSO_LOGIN = "/public/sso/login";
    @ApiDesc("3. sso 单点登出")
    String PUBLIC_SSO_LOGOUT = "/public/sso/logout";
    @ApiDesc("4. sso 刷新令牌")
    String PUBLIC_SSO_REFRESH_TOKEN = "/public/sso/refresh-token";
    @ApiDesc("5. sso 忘记密码")
    String PUBLIC_SSO_FORGOT_PASSWORD = "/public/sso/forgot-password";
    @ApiDesc("6. sso 重置密码")
    String PUBLIC_SSO_RESET_PASSWORD = "/public/sso/reset-password";


    @ApiDesc("4. 用户菜单树")
    String USER_MENU_TREE = "/user/menu/tree";
    @ApiDesc("4. 用户菜单树")
    String USER_MENU_TREE_RUOYI = "/user/menu/tree/ruoyi";

    // 注册模块
    @ApiDesc("1. sso 单点注册(发送验证码)")
    String PUBLIC_SSO_REGISTER = "/public/sso/register";
    @ApiDesc("2. sso 注册验证")
    String PUBLIC_SSO_REGISTER_VERIFY = "/public/sso/register/verify";

    // 个人中心模块
    @ApiDesc("1. 用户信息")
    String USER_INFO = "/user/info";
    @ApiDesc("2. 更新用户信息")
    String USER_UPDATE = "/user/update";
    @ApiDesc("3. 修改密码")
    String USER_CHANGE_PASSWORD = "/user/change-password";
    @ApiDesc("4. 绑定手机号")
    String USER_BIND_PHONE = "/user/bind-phone";
    @ApiDesc("5. 绑定邮箱")
    String USER_BIND_EMAIL = "/user/bind-email";
    @ApiDesc("6. 登录记录")
    String USER_LOGIN_RECORDS = "/user/login-records";
    @ApiDesc("7. 操作日志")
    String USER_OPERATE_RECORDS = "/user/operate-records";

    // 门户模块
    @ApiDesc("1. 首页数据")
    String PORTAL_INDEX_DATA = "/portal/index-data";
    @ApiDesc("2. 公告列表")
    String PORTAL_NOTICES = "/portal/notices";
    @ApiDesc("3. 统计数据")
    String PORTAL_STATISTICS = "/portal/statistics";
    @ApiDesc("4. 待办事项")
    String PORTAL_TODO_LIST = "/portal/todo-list";

}
