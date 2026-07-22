package com.wkclz.iam.admin;

import com.wkclz.core.annotation.Router;

@Router(module = "iam-admin", prefix = Route.PREFIX)
public interface Route {

    String PREFIX = "/iam-admin";

    String USER_PAGE = "/user/page";
    String USER_INFO = "/user/info";
    String USER_CREATE = "/user/create";
    String USER_UPDATE = "/user/update";
    String USER_REMOVE = "/user/remove";


    // 用户认证方式
    String USER_AUTH_LIST = "/user-auth/list";
    String USER_AUTH_INFO = "/user-auth/info";
    String USER_AUTH_CREATE = "/user-auth/create";
    String USER_AUTH_UPDATE = "/user-auth/update";
    String USER_AUTH_REMOVE = "/user-auth/remove";
    String USER_AUTH_RESET_PASSWORD = "/user-auth/reset-password";

    // 应用管理
    String APP_PAGE = "/app/page";
    String APP_INFO = "/app/info";
    String APP_CREATE = "/app/create";
    String APP_UPDATE = "/app/update";
    String APP_REMOVE = "/app/remove";
    String APP_OPTIONS = "/app/options";

    // 数据权限维度管理
    String DATA_DIM_PAGE = "/data-dim/page";
    String DATA_DIM_INFO = "/data-dim/info";
    String DATA_DIM_CREATE = "/data-dim/create";
    String DATA_DIM_UPDATE = "/data-dim/update";
    String DATA_DIM_REMOVE = "/data-dim/remove";
    String DATA_DIM_OPTIONS = "/data-dim/options";

    // 角色
    String ROLE_LIST = "/role/list";
    String ROLE_INFO = "/role/info";
    String ROLE_CREATE = "/role/create";
    String ROLE_UPDATE = "/role/update";
    String ROLE_REMOVE = "/role/remove";
    String ROLE_TREE = "/role/tree";

    // 用户角色关联
    String USER_ROLE_LIST = "/user-role/list";
    String USER_ROLE_BIND = "/user-role/bind";
    String USER_ROLE_UNBIND = "/user-role/unbind";
    String USER_ROLE_ROLE_TREE = "/user-role/role-tree";
    String USER_MENU_SOURCE_LIST = "/user-role/menu-source";

    // 角色用户关联
    String ROLE_USER_PAGE = "/role-user/page";
    String ROLE_USER_BIND = "/role-user/bind";
    String ROLE_USER_UNBIND = "/role-user/unbind";

    // 菜单
    String MENU_LIST = "/menu/list";
    String MENU_TREE = "/menu/tree";
    String MENU_INFO = "/menu/info";
    String MENU_CREATE = "/menu/create";
    String MENU_UPDATE = "/menu/update";
    String MENU_REMOVE = "/menu/remove";
    String MENU_DETAIL = "/menu/detail";
    String MENU_BOUND_ROLES = "/menu/bound-roles";
    String MENU_BOUND_USERS = "/menu/bound-users";

    // 角色菜单关联
    String ROLE_MENU_LIST = "/role-menu/list";
    String ROLE_MENU_SAVE = "/role-menu/save";
    String ROLE_MENU_BOUND_ROLES = "/role-menu/bound-roles";

    // 用户菜单
    String USER_MENU_LIST = "/user-menu/list";
    String USER_MENU_TREE = "/user-menu/tree";

    // 角色数据关联
    String ROLE_DATA_LIST = "/role-data/list";
    String ROLE_DATA_BIND = "/role-data/bind";
    String ROLE_DATA_UNBIND = "/role-data/unbind";


    // API管理
    String API_PAGE = "/api/page";
    String API_INFO = "/api/info";
    String API_CREATE = "/api/create";
    String API_UPDATE = "/api/update";
    String API_REMOVE = "/api/remove";
    String API_OPTIONS = "/api/options";
    String API_SYNC = "/api/sync";
    String API_COPY = "/api/copy";
    String API_PASTE = "/api/paste";
    String API_DETAIL = "/api/detail";
    String API_DOC = "/api/doc";

    // 登录日志
    String LOGIN_LOG_PAGE = "/login-log/page";
    String LOGIN_LOG_INFO = "/login-log/info";

    // 请求日志
    String REQUEST_LOG_PAGE = "/request-log/page";
    String REQUEST_LOG_INFO = "/request-log/info";

    // 访问密钥管理
    String ACCESS_KEY_PAGE = "/access-key/page";
    String ACCESS_KEY_INFO = "/access-key/info";
    String ACCESS_KEY_CREATE = "/access-key/create";
    String ACCESS_KEY_UPDATE = "/access-key/update";
    String ACCESS_KEY_REMOVE = "/access-key/remove";


    // 访问密钥-API 关系
    String ACCESS_KEY_API_LIST = "/access-key-api/list";
    String ACCESS_KEY_API_BIND = "/access-key-api/bind";
    String ACCESS_KEY_API_UNBIND = "/access-key-api/unbind";


    // 菜单-API 关系
    String MENU_API_LIST = "/menu-api/list";
    String MENU_API_BIND = "/menu-api/bind";
    String MENU_API_UNBIND = "/menu-api/unbind";
    String MENU_API_BOUND_LIST = "/menu-api/bound-list";

    // API字段权限
    String API_FIELD_LIST_BY_API = "/api-field/list-by-api";
    String API_FIELD_CREATE = "/api-field/create";
    String API_FIELD_UPDATE = "/api-field/update";
    String API_FIELD_REMOVE = "/api-field/remove";

    // 实体字段分析
    String ENTITY_FIELD_RESOLVE = "/entity-field/resolve";

    // 菜单字段关系
    String MENU_FIELD_LIST = "/menu-field/list";
    String MENU_FIELD_BIND = "/menu-field/bind";
    String MENU_FIELD_SAVE = "/menu-field/save";
    String MENU_FIELD_UNBIND = "/menu-field/unbind";

}
