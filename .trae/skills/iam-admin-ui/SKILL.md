# iam-admin-ui 模块知识库

sh-iam 管理后台前端，基于 Vue3 + Element Plus，提供用户/角色/菜单/应用/API/访问密钥/数据权限/日志等管理界面。动态路由由后端菜单树驱动。

## 关键目录结构

```
src/
├── api/              # API 请求封装
│   ├── user/         # 用户模块 API
│   │   ├── user.js       # 用户 CRUD + 重置密码
│   │   ├── role.js       # 角色 CRUD + 树
│   │   ├── user-role.js  # 用户角色绑定/解绑/角色树/菜单来源/菜单关联
│   │   └── role-user.js  # 角色用户分页/绑定/解绑
│   ├── system/       # 系统管理 API
│   │   ├── app.js        # 应用 CRUD + 选项
│   │   ├── menu.js       # 菜单 CRUD + 树 + 详情页
│   │   ├── api.js        # API CRUD + 选项 + 同步 + 复制/粘贴 + 详情页
│   │   ├── ak.js         # 访问密钥 CRUD
│   │   ├── ak-api.js     # AK-API 关联
│   │   ├── role-menu.js  # 角色菜单保存/列表
│   │   ├── menu-api.js   # 菜单API 绑定/解绑/列表
│   │   ├── dim.js        # 数据权限维度 CRUD
│   │   └── api-field.js  # API字段权限 + 实体字段定位 + 菜单字段
│   ├── log/          # 日志 API
│   │   ├── loginlog.js   # 登录日志分页/详情
│   │   └── requestlog.js # 请求日志分页/详情
│   ├── common.js     # 公共 API
│   ├── config.js     # 配置 API
│   ├── data.js       # 数据 API
│   └── sso.js        # SSO 登录/登出/用户信息/菜单树
├── views/            # 页面组件
│   ├── user/         # 用户管理
│   │   ├── user/         # 用户管理主页面
│   │   │   ├── index.vue         # 用户列表页
│   │   │   └── components/       # 子组件
│   │   │       ├── edit.vue           # 新增/编辑弹窗
│   │   │       ├── detail.vue         # 详情弹窗(基本信息+角色+菜单来源)
│   │   │       ├── reset-password.vue # 重置密码弹窗
│   │   │       ├── user-role.vue      # 角色绑定Tab(应用选择+角色树+绑定)
│   │   │       └── user-menu-source.vue # 菜单来源Tab(树形表格)
│   │   └── role/         # 角色管理
│   │       ├── index.vue         # 角色列表页(列表+树形双Tab)
│   │       └── components/
│   │           ├── edit.vue           # 新增/编辑弹窗
│   │           ├── detail.vue         # 详情弹窗
│   │           ├── menu-bind.vue      # 菜单绑定弹窗
│   │           └── role-user.vue      # 角色用户管理
│   ├── system/       # 系统管理
│   │   ├── app/          # 应用管理(index + edit)
│   │   ├── menu/         # 菜单管理(列表+树形双Tab, index + edit + detail)
│   │   ├── api/          # API管理(index + edit + detail + api-field-edit + field-tree-select)
│   │   ├── ak/           # 访问密钥(index + edit + ak-api)
│   │   └── dimension/    # 数据权限维度(index + edit)
│   ├── log/          # 日志管理
│   │   ├── login/        # 登录日志(index)
│   │   └── request/      # 请求日志(index + detail)
│   ├── components/   # 公共业务组件
│   │   └── AppOptions/   # 应用选择公共组件
│   ├── dashboard/    # 仪表盘(StatsCard/QuickActions/RecentActivity/SystemStatus)
│   ├── login.vue     # 登录页
│   └── error/        # 401/404 错误页
├── components/       # 通用UI组件(Breadcrumb/Pagination/SvgIcon/MonacoEditor等)
├── layout/           # 布局(Sidebar/Navbar/TagsView/TopBar/Copyright)
├── store/modules/    # Pinia 状态管理
│   ├── user.js       # 用户状态(login/getInfo/logOut)
│   ├── permission.js # 权限路由
│   ├── app.js        # 应用状态
│   ├── dict.js       # 字典
│   ├── settings.js   # 设置
│   └── tagsView.js   # 标签页
├── router/           # 路由配置(Hash模式, constantRoutes + dynamicRoutes)
├── utils/            # 工具函数(request/auth/jsencrypt/permission/validate等)
├── directive/        # 自定义指令(hasPermi/hasRole/copyText)
└── plugins/          # 插件(auth/cache/download/modal/tab)
```

## API 模块映射

| API 文件                  | 后端路由前缀                                       | 功能                    |
|-------------------------|----------------------------------------------|-----------------------|
| api/user/user.js        | /iam-admin/user                              | 用户 CRUD               |
| api/user/role.js        | /iam-admin/role                              | 角色 CRUD + 树           |
| api/user/user-role.js   | /iam-admin/user-role + /iam-admin/menu       | 用户角色绑定/菜单来源/菜单关联      |
| api/user/role-user.js   | /iam-admin/role-user                         | 角色用户分页/绑定/解绑          |
| api/system/app.js       | /iam-admin/app                               | 应用 CRUD + 选项          |
| api/system/menu.js      | /iam-admin/menu                              | 菜单 CRUD + 树 + 详情页     |
| api/system/api.js       | /iam-admin/api                               | API CRUD + 同步 + 复制/粘贴 |
| api/system/ak.js        | /iam-admin/access-key                        | AK CRUD               |
| api/system/ak-api.js    | /iam-admin/access-key-api                    | AK-API 关联             |
| api/system/role-menu.js | /iam-admin/role-menu                         | 角色菜单保存/列表             |
| api/system/menu-api.js  | /iam-admin/menu-api                          | 菜单API 绑定/解绑           |
| api/system/dim.js       | /iam-admin/data-dim                          | 数据维度 CRUD             |
| api/system/api-field.js | /iam-admin/api-field + /iam-admin/menu-field | API字段权限 + 菜单字段        |
| api/log/loginlog.js     | /iam-admin/login-log                         | 登录日志                  |
| api/log/requestlog.js   | /iam-admin/request-log                       | 请求日志                  |
| api/sso.js              | /iam-sso                                     | SSO 登录/登出/用户信息/菜单树    |

## 核心组件

| 组件                   | 位置                                              | 说明                              |
|----------------------|-------------------------------------------------|---------------------------------|
| AppOptions           | views/components/AppOptions/                    | 应用选择公共组件，用于角色/菜单等按应用筛选          |
| IamUserEdit          | views/user/user/components/edit.vue             | 用户新增/编辑弹窗                       |
| IamUserDetail        | views/user/user/components/detail.vue           | 用户详情(基本信息+角色Tab+菜单来源Tab)        |
| IamUserRole          | views/user/user/components/user-role.vue        | 用户角色绑定(应用选择+角色树+绑定弹窗+popover详情) |
| IamUserMenuSource    | views/user/user/components/user-menu-source.vue | 用户菜单来源(树形表格,来源角色popover展示有效期)   |
| IamUserResetPassword | views/user/user/components/reset-password.vue   | 重置密码弹窗                          |
| IamRoleEdit          | views/user/role/components/edit.vue             | 角色新增/编辑弹窗                       |
| RoleMenuBind         | views/user/role/components/menu-bind.vue        | 角色菜单绑定弹窗                        |
| RoleUser             | views/user/role/components/role-user.vue        | 角色用户管理                          |

## 用户状态管理 (store/modules/user.js)

- **login**: 调用 ssoLogin，RSA 加密密码，存储 token
- **getInfo**: 解析 JWT payload 获取 userCode/username/nickname/avatar
- **logOut**: 调用 ssoLogout，清除 token

## 使用方式

当涉及以下场景时调用此技能：

- 管理后台前端页面开发（用户/角色/菜单/应用/API/AK等）
- 前端 API 请求封装模式参考
- 用户角色绑定/菜单绑定等关联操作界面
- 应用选择公共组件使用
- 前端路由和权限控制
- Pinia 状态管理
