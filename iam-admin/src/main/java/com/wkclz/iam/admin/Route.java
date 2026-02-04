package com.wkclz.iam.admin;

import com.wkclz.core.annotation.Desc;
import com.wkclz.core.annotation.Router;

@Router(module = "iam-admin", prefix = Route.PREFIX)
public interface Route {

    String PREFIX = "/iam-admin";
    
    @Desc("1. 用户-分页")
    String USER_PAGE = "/user/page";
    @Desc("2. 用户-详情")
    String USER_INFO = "/user/info";
    @Desc("3. 用户-创建")
    String USER_CREATE = "/user/create";
    @Desc("4. 用户-修改")
    String USER_UPDATE = "/user/update";
    @Desc("5. 用户-删除")
    String USER_REMOVE = "/user/remove";


    // 用户认证方式
    @Desc("1. 用户认证方式-列表")
    String USER_AUTH_LIST = "/user-auth/list";
    @Desc("2. 用户认证方式-详情")
    String USER_AUTH_INFO = "/user-auth/info";
    @Desc("3. 用户认证方式-创建")
    String USER_AUTH_CREATE = "/user-auth/create";
    @Desc("4. 用户认证方式-修改")
    String USER_AUTH_UPDATE = "/user-auth/update";
    @Desc("5. 用户认证方式-删除")
    String USER_AUTH_REMOVE = "/user-auth/remove";

    // 应用管理
    @Desc("1. 应用管理-分页")
    String APP_PAGE = "/app/page";
    @Desc("2. 应用管理-详情")
    String APP_INFO = "/app/info";
    @Desc("3. 应用管理-创建")
    String APP_CREATE = "/app/create";
    @Desc("4. 应用管理-修改")
    String APP_UPDATE = "/app/update";
    @Desc("5. 应用管理-删除")
    String APP_REMOVE = "/app/remove";
    @Desc("6. 应用管理-应用选项")
    String APP_OPTIONS = "/app/options";

    // 数据权限维度管理
    @Desc("1. 数据权限维度-分页")
    String DATA_DIM_PAGE = "/data-dim/page";
    @Desc("2. 数据权限维度-详情")
    String DATA_DIM_INFO = "/data-dim/info";
    @Desc("3. 数据权限维度-创建")
    String DATA_DIM_CREATE = "/data-dim/create";
    @Desc("4. 数据权限维度-修改")
    String DATA_DIM_UPDATE = "/data-dim/update";
    @Desc("5. 数据权限维度-删除")
    String DATA_DIM_REMOVE = "/data-dim/remove";

    // 角色
    @Desc("1. 角色管理-列表")
    String ROLE_LIST = "/role/list";
    @Desc("2. 角色管理-详情")
    String ROLE_INFO = "/role/info";
    @Desc("3. 角色管理-创建")
    String ROLE_CREATE = "/role/create";
    @Desc("4. 角色管理-修改")
    String ROLE_UPDATE = "/role/update";
    @Desc("5. 角色管理-删除")
    String ROLE_REMOVE = "/role/remove";

    // 用户角色关联
    @Desc("1. 用户角色-列表")
    String USER_ROLE_LIST = "/user-role/list";
    @Desc("2. 用户角色-绑定")
    String USER_ROLE_BIND = "/user-role/bind";
    @Desc("3. 用户角色-解绑")
    String USER_ROLE_UNBIND = "/user-role/unbind";

    // 角色用户关联
    @Desc("1. 角色用户-列表")
    String ROLE_USER_LIST = "/role-user/list";
    @Desc("2. 角色用户-绑定")
    String ROLE_USER_BIND = "/role-user/bind";
    @Desc("3. 角色用户-解绑")
    String ROLE_USER_UNBIND = "/role-user/unbind";

    // 菜单
    @Desc("1. 菜单管理-列表")
    String MENU_LIST = "/menu/list";
    @Desc("2. 菜单管理-树")
    String MENU_TREE = "/menu/tree";
    @Desc("3. 菜单管理-详情")
    String MENU_INFO = "/menu/info";
    @Desc("4. 菜单管理-创建")
    String MENU_CREATE = "/menu/create";
    @Desc("5. 菜单管理-修改")
    String MENU_UPDATE = "/menu/update";
    @Desc("6. 菜单管理-删除")
    String MENU_REMOVE = "/menu/remove";

    // 角色菜单关联
    @Desc("1. 角色菜单-列表")
    String ROLE_MENU_LIST = "/role-menu/list";
    @Desc("2. 角色菜单-绑定")
    String ROLE_MENU_BIND = "/role-menu/bind";
    @Desc("3. 角色菜单-解绑")
    String ROLE_MENU_UNBIND = "/role-menu/unbind";

    // 用户菜单
    @Desc("1. 用户菜单-列表")
    String USER_MENU_LIST = "/user-menu/list";
    @Desc("2. 用户菜单-树")
    String USER_MENU_TREE = "/user-menu/tree";

    // 角色数据关联
    @Desc("1. 角色数据-列表")
    String ROLE_DATA_LIST = "/role-data/list";
    @Desc("2. 角色数据-绑定")
    String ROLE_DATA_BIND = "/role-data/bind";
    @Desc("3. 角色数据-解绑")
    String ROLE_DATA_UNBIND = "/role-data/unbind";

    // 用户数据关联
    @Desc("1. 用户数据-列表")
    String USER_DATA_LIST = "/user-data/list";
    @Desc("2. 用户数据-绑定")
    String USER_DATA_BIND = "/user-data/bind";
    @Desc("3. 用户数据-解绑")
    String USER_DATA_UNBIND = "/user-data/unbind";

    // API管理
    @Desc("1. API管理-分页")
    String API_PAGE = "/api/page";
    @Desc("2. API管理-详情")
    String API_INFO = "/api/info";
    @Desc("3. API管理-创建")
    String API_CREATE = "/api/create";
    @Desc("4. API管理-修改")
    String API_UPDATE = "/api/update";
    @Desc("5. API管理-删除")
    String API_REMOVE = "/api/remove";
    @Desc("6. API管理-同步")
    String API_SYNC = "/api/sync";

    // 角色API关联
    @Desc("1. 角色API-列表")
    String ROLE_API_LIST = "/role-api/list";
    @Desc("2. 角色API-绑定")
    String ROLE_API_BIND = "/role-api/bind";
    @Desc("3. 角色API-解绑")
    String ROLE_API_UNBIND = "/role-api/unbind";

    // 登录日志
    @Desc("1. 登录日志-分页")
    String LOGIN_LOG_PAGE = "/login-log/page";
    @Desc("2. 登录日志-详情")
    String LOGIN_LOG_INFO = "/login-log/info";

    // 请求日志
    @Desc("1. 请求日志-分页")
    String REQUEST_LOG_PAGE = "/request-log/page";
    @Desc("2. 请求日志-详情")
    String REQUEST_LOG_INFO = "/request-log/info";

    // 访问密钥管理
    @Desc("1. 访问密钥-分页")
    String ACCESS_KEY_PAGE = "/access-key/page";
    @Desc("2. 访问密钥-详情")
    String ACCESS_KEY_INFO = "/access-key/info";
    @Desc("3. 访问密钥-创建")
    String ACCESS_KEY_CREATE = "/access-key/create";
    @Desc("4. 访问密钥-修改")
    String ACCESS_KEY_UPDATE = "/access-key/update";
    @Desc("5. 访问密钥-删除")
    String ACCESS_KEY_REMOVE = "/access-key/remove";

}
