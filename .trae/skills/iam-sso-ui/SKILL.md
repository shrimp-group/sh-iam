# iam-sso-ui 模块知识库

sh-iam SSO 登录前端，基于 Vue3 + Element Plus，提供登录、注册、个人中心（用户信息/修改密码/登录记录/操作日志）、门户首页等界面。动态路由由后端若依格式菜单树驱动。

## 关键目录结构

```
src/
├── api/              # API 请求封装
│   ├── sso.js        # SSO 接口：验证码/登录/登出/用户信息/菜单树/注册
│   ├── user.js       # 用户接口：修改密码/更新信息/上传头像
│   ├── common.js     # 公共 API
│   └── config.js     # 配置 API
├── views/            # 页面组件
│   ├── login.vue     # 登录页（验证码+RSA加密密码）
│   ├── register.vue  # 注册页
│   ├── user/         # 个人中心
│   │   ├── index.vue         # 个人中心主页面(Tab切换)
│   │   ├── profile/          # 个人资料
│   │   │   ├── index.vue         # 资料主页(基本信息+修改密码Tab)
│   │   │   ├── userInfo.vue      # 用户信息编辑表单
│   │   │   ├── resetPwd.vue      # 修改密码表单
│   │   │   └── userAvatar.vue    # 头像上传
│   │   ├── authRole.vue      # 用户角色信息
│   │   ├── change-password.vue # 独立修改密码页
│   │   ├── login-records/    # 登录记录
│   │   │   └── index.vue         # 登录记录列表
│   │   └── operate-logs/     # 操作日志
│   │       └── index.vue         # 操作日志列表
│   ├── portal/       # 门户首页
│   │   ├── index.vue         # 门户主页
│   │   ├── notices.vue       # 公告列表
│   │   ├── statistics.vue    # 统计数据
│   │   └── todo-list.vue     # 待办事项
│   ├── dashboard/    # 仪表盘
│   │   └── index.vue         # 仪表盘首页
│   ├── error/        # 401/404 错误页
│   └── redirect/     # 路由重定向
├── components/       # 通用UI组件(与iam-admin-ui共享同一套组件库)
├── layout/           # 布局(Sidebar/Navbar/TagsView/TopBar/Copyright)
├── store/modules/    # Pinia 状态管理
│   ├── user.js       # 用户状态(login/getInfo/logOut)
│   ├── permission.js # 权限路由
│   ├── app.js        # 应用状态
│   ├── dict.js       # 字典
│   ├── settings.js   # 设置
│   └── tagsView.js   # 标签页
├── router/           # 路由配置(Hash模式, 含 /login + /register 公共路由)
├── utils/            # 工具函数(与iam-admin-ui共享)
├── directive/        # 自定义指令(hasPermi/hasRole/copyText)
└── plugins/          # 插件(auth/cache/download/modal/tab)
```

## API 模块映射

| API 文件      | 后端路由                          | 功能        |
|-------------|-------------------------------|-----------|
| api/sso.js  | /iam-sso/public/captcha/chart | 获取图形验证码   |
| api/sso.js  | /iam-sso/public/sso/login     | 用户登录      |
| api/sso.js  | /iam-sso/public/sso/logout    | 用户登出      |
| api/sso.js  | /iam-sso/user/info            | 获取用户信息    |
| api/sso.js  | /iam-sso/user/menu/tree/ruoyi | 获取若依格式菜单树 |
| api/sso.js  | /iam-sso/public/sso/register  | 用户注册      |
| api/user.js | /iam-sso/user/change-password | 修改密码      |

## 核心页面

| 页面   | 文件                                 | 说明                    |
|------|------------------------------------|-----------------------|
| 登录页  | views/login.vue                    | 验证码+RSA加密密码登录         |
| 注册页  | views/register.vue                 | 用户注册（验证码验证）           |
| 个人中心 | views/user/index.vue               | Tab 切换：基本信息/修改密码/角色信息 |
| 用户信息 | views/user/profile/userInfo.vue    | 编辑昵称/邮箱/手机号           |
| 修改密码 | views/user/profile/resetPwd.vue    | 旧密码+新密码+确认密码          |
| 头像上传 | views/user/profile/userAvatar.vue  | 头像裁剪上传                |
| 登录记录 | views/user/login-records/index.vue | 登录日志列表                |
| 操作日志 | views/user/operate-logs/index.vue  | 请求日志列表                |
| 门户首页 | views/portal/index.vue             | 公告/统计/待办              |
| 仪表盘  | views/dashboard/index.vue          | 系统概览                  |

## 用户状态管理 (store/modules/user.js)

- **login**: 调用 ssoLogin，RSA 加密密码（jsencrypt），存储 token
- **getInfo**: 解析 JWT payload（Base64 decode）获取 userCode/username/nickname/avatar
- **logOut**: 调用 ssoLogout，清除 token

## 与 iam-admin-ui 的区别

| 维度     | iam-sso-ui                                           | iam-admin-ui               |
|--------|------------------------------------------------------|----------------------------|
| 定位     | SSO 登录 + 个人中心                                        | 管理后台                       |
| 核心页面   | 登录/注册/个人中心/门户                                        | 用户/角色/菜单/应用/API/AK管理       |
| API 前缀 | /iam-sso                                             | /iam-admin                 |
| 特有路由   | /register, /portal/*                                 | 无                          |
| 特有页面   | change-password, login-records, operate-logs, portal | dashboard, system/*, log/* |

## 使用方式

当涉及以下场景时调用此技能：

- SSO 登录/注册页面开发
- 个人中心功能（修改密码/用户信息/头像）
- 登录记录/操作日志展示
- 门户首页开发
- SSO 前端 API 请求封装
- SSO 用户状态管理
