package com.wkclz.iam.admin;

import com.wkclz.core.annotation.ApiDesc;
import com.wkclz.core.annotation.Router;

@Router(module = "iam-admin", prefix = Route.PREFIX)
public interface Route {

    String PREFIX = "/iam-admin";
    
    @ApiDesc("1. 用户-分页")
    String USER_PAGE = "/user/page";
    @ApiDesc("2. 用户-详情")
    String USER_INFO = "/user/info";
    @ApiDesc("3. 用户-创建")
    String USER_CREATE = "/user/create";
    @ApiDesc("4. 用户-修改")
    String USER_UPDATE = "/user/update";
    @ApiDesc("5. 用户-删除")
    String USER_REMOVE = "/user/remove";


    // 用户认证方式
    @ApiDesc("1. 用户认证方式-列表")
    String USER_AUTH_LIST = "/user-auth/list";
    @ApiDesc("2. 用户认证方式-详情")
    String USER_AUTH_INFO = "/user-auth/info";
    @ApiDesc("3. 用户认证方式-创建")
    String USER_AUTH_CREATE = "/user-auth/create";
    @ApiDesc("4. 用户认证方式-修改")
    String USER_AUTH_UPDATE = "/user-auth/update";
    @ApiDesc("5. 用户认证方式-删除")
    String USER_AUTH_REMOVE = "/user-auth/remove";
    @ApiDesc("6. 用户认证方式-重置密码")
    String USER_AUTH_RESET_PASSWORD = "/user-auth/reset-password";

    // 应用管理
    @ApiDesc("1. 应用管理-分页")
    String APP_PAGE = "/app/page";
    @ApiDesc("2. 应用管理-详情")
    String APP_INFO = "/app/info";
    @ApiDesc("3. 应用管理-创建")
    String APP_CREATE = "/app/create";
    @ApiDesc("4. 应用管理-修改")
    String APP_UPDATE = "/app/update";
    @ApiDesc("5. 应用管理-删除")
    String APP_REMOVE = "/app/remove";
    @ApiDesc("6. 应用管理-应用选项")
    String APP_OPTIONS = "/app/options";

    // 数据权限维度管理
    @ApiDesc("1. 数据权限维度-分页")
    String DATA_DIM_PAGE = "/data-dim/page";
    @ApiDesc("2. 数据权限维度-详情")
    String DATA_DIM_INFO = "/data-dim/info";
    @ApiDesc("3. 数据权限维度-创建")
    String DATA_DIM_CREATE = "/data-dim/create";
    @ApiDesc("4. 数据权限维度-修改")
    String DATA_DIM_UPDATE = "/data-dim/update";
    @ApiDesc("5. 数据权限维度-删除")
    String DATA_DIM_REMOVE = "/data-dim/remove";
    @ApiDesc("6. 数据权限维度-列表选项")
    String DATA_DIM_OPTIONS = "/data-dim/options";

    // 角色
    @ApiDesc("1. 角色管理-列表")
    String ROLE_LIST = "/role/list";
    @ApiDesc("2. 角色管理-详情")
    String ROLE_INFO = "/role/info";
    @ApiDesc("3. 角色管理-创建")
    String ROLE_CREATE = "/role/create";
    @ApiDesc("4. 角色管理-修改")
    String ROLE_UPDATE = "/role/update";
    @ApiDesc("5. 角色管理-删除")
    String ROLE_REMOVE = "/role/remove";
    @ApiDesc("6. 角色管理-树")
    String ROLE_TREE = "/role/tree";

    // 用户角色关联
    @ApiDesc("1. 用户角色-列表")
    String USER_ROLE_LIST = "/user-role/list";
    @ApiDesc("2. 用户角色-绑定")
    String USER_ROLE_BIND = "/user-role/bind";
    @ApiDesc("3. 用户角色-解绑")
    String USER_ROLE_UNBIND = "/user-role/unbind";
    @ApiDesc("4. 用户角色-角色树")
    String USER_ROLE_ROLE_TREE = "/user-role/role-tree";
    @ApiDesc("5. 用户角色-菜单来源")
    String USER_MENU_SOURCE_LIST = "/user-role/menu-source";

    // 角色用户关联
    @ApiDesc("1. 角色用户-分页")
    String ROLE_USER_PAGE = "/role-user/page";
    @ApiDesc("2. 角色用户-绑定")
    String ROLE_USER_BIND = "/role-user/bind";
    @ApiDesc("3. 角色用户-解绑")
    String ROLE_USER_UNBIND = "/role-user/unbind";

    // 菜单
    @ApiDesc("1. 菜单管理-列表")
    String MENU_LIST = "/menu/list";
    @ApiDesc("2. 菜单管理-树")
    String MENU_TREE = "/menu/tree";
    @ApiDesc("3. 菜单管理-详情")
    String MENU_INFO = "/menu/info";
    @ApiDesc("4. 菜单管理-创建")
    String MENU_CREATE = "/menu/create";
    @ApiDesc("5. 菜单管理-修改")
    String MENU_UPDATE = "/menu/update";
    @ApiDesc("6. 菜单管理-删除")
    String MENU_REMOVE = "/menu/remove";
    @ApiDesc("7. 菜单管理-详情页")
    String MENU_DETAIL = "/menu/detail";
    @ApiDesc("8. 菜单管理-绑定角色")
    String MENU_BOUND_ROLES = "/menu/bound-roles";
    @ApiDesc("9. 菜单管理-绑定用户")
    String MENU_BOUND_USERS = "/menu/bound-users";

    // 角色菜单关联
    @ApiDesc("1. 角色菜单-列表")
    String ROLE_MENU_LIST = "/role-menu/list";
    @ApiDesc("2. 角色菜单-保存")
    String ROLE_MENU_SAVE = "/role-menu/save";
    @ApiDesc("3. 角色菜单-已绑定角色列表")
    String ROLE_MENU_BOUND_ROLES = "/role-menu/bound-roles";

    // 用户菜单
    @ApiDesc("1. 用户菜单-列表")
    String USER_MENU_LIST = "/user-menu/list";
    @ApiDesc("2. 用户菜单-树")
    String USER_MENU_TREE = "/user-menu/tree";

    // 角色数据关联
    @ApiDesc("1. 角色数据-列表")
    String ROLE_DATA_LIST = "/role-data/list";
    @ApiDesc("2. 角色数据-绑定")
    String ROLE_DATA_BIND = "/role-data/bind";
    @ApiDesc("3. 角色数据-解绑")
    String ROLE_DATA_UNBIND = "/role-data/unbind";


    // API管理
    @ApiDesc("1. API管理-分页")
    String API_PAGE = "/api/page";
    @ApiDesc("2. API管理-详情")
    String API_INFO = "/api/info";
    @ApiDesc("3. API管理-创建")
    String API_CREATE = "/api/create";
    @ApiDesc("4. API管理-修改")
    String API_UPDATE = "/api/update";
    @ApiDesc("5. API管理-删除")
    String API_REMOVE = "/api/remove";
    @ApiDesc("6. API管理-选项")
    String API_OPTIONS = "/api/options";
    @ApiDesc("7. API管理-同步")
    String API_SYNC = "/api/sync";
    @ApiDesc("8. API管理-复制为json")
    String API_COPY = "/api/copy";
    @ApiDesc("9. API管理-粘贴JSON")
    String API_PASTE = "/api/paste";
    @ApiDesc("10. API管理-详情页")
    String API_DETAIL = "/api/detail";

    // 登录日志
    @ApiDesc("1. 登录日志-分页")
    String LOGIN_LOG_PAGE = "/login-log/page";
    @ApiDesc("2. 登录日志-详情")
    String LOGIN_LOG_INFO = "/login-log/info";

    // 请求日志
    @ApiDesc("1. 请求日志-分页")
    String REQUEST_LOG_PAGE = "/request-log/page";
    @ApiDesc("2. 请求日志-详情")
    String REQUEST_LOG_INFO = "/request-log/info";

    // 访问密钥管理
    @ApiDesc("1. 访问密钥-分页")
    String ACCESS_KEY_PAGE = "/access-key/page";
    @ApiDesc("2. 访问密钥-详情")
    String ACCESS_KEY_INFO = "/access-key/info";
    @ApiDesc("3. 访问密钥-创建")
    String ACCESS_KEY_CREATE = "/access-key/create";
    @ApiDesc("4. 访问密钥-修改")
    String ACCESS_KEY_UPDATE = "/access-key/update";
    @ApiDesc("5. 访问密钥-删除")
    String ACCESS_KEY_REMOVE = "/access-key/remove";


    // 访问密钥-API 关系
    @ApiDesc("1. 访问密钥-API-列表")
    String ACCESS_KEY_API_LIST = "/access-key-api/list";
    @ApiDesc("2. 访问密钥-API-创建")
    String ACCESS_KEY_API_BIND = "/access-key-api/bind";
    @ApiDesc("3. 访问密钥-API-删除")
    String ACCESS_KEY_API_UNBIND = "/access-key-api/unbind";


    // 菜单-API 关系
    @ApiDesc("1. 菜单-API-分页")
    String MENU_API_LIST = "/menu-api/list";
    @ApiDesc("2. 菜单-API-绑定")
    String MENU_API_BIND = "/menu-api/bind";
    @ApiDesc("3. 菜单-API-解绑")
    String MENU_API_UNBIND = "/menu-api/unbind";
    @ApiDesc("4. 菜单-API-已绑定列表")
    String MENU_API_BOUND_LIST = "/menu-api/bound-list";

    // API字段权限
    @ApiDesc("1. API字段权限-按API查询")
    String API_FIELD_LIST_BY_API = "/api-field/list-by-api";
    @ApiDesc("2. API字段权限-创建")
    String API_FIELD_CREATE = "/api-field/create";
    @ApiDesc("3. API字段权限-修改")
    String API_FIELD_UPDATE = "/api-field/update";
    @ApiDesc("4. API字段权限-删除")
    String API_FIELD_REMOVE = "/api-field/remove";

    // 实体字段分析
    @ApiDesc("1. 实体字段-根据API定位")
    String ENTITY_FIELD_RESOLVE = "/entity-field/resolve";

    // 菜单字段关系
    @ApiDesc("1. 菜单字段-列表")
    String MENU_FIELD_LIST = "/menu-field/list";
    @ApiDesc("2. 菜单字段-绑定")
    String MENU_FIELD_BIND = "/menu-field/bind";
    @ApiDesc("3. 菜单字段-批量保存")
    String MENU_FIELD_SAVE = "/menu-field/save";
    @ApiDesc("4. 菜单字段-解绑")
    String MENU_FIELD_UNBIND = "/menu-field/unbind";

}
