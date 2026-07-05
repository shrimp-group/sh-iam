# API 索引

## API 列表

### SSO 公开接口 (`/iam-sso/public/**`)

| 方法   | 路径                       | 说明      | 模块      |
|------|--------------------------|---------|---------|
| POST | /iam-sso/public/login    | 用户名密码登录 | iam-sso |
| POST | /iam-sso/public/captcha  | 获取图形验证码 | iam-sso |
| POST | /iam-sso/public/register | 用户注册    | iam-sso |

### SSO 用户接口 (`/iam-sso/user/**`)

| 方法   | 路径                   | 说明       | 模块      |
|------|----------------------|----------|---------|
| GET  | /iam-sso/user/info   | 获取当前用户信息 | iam-sso |
| GET  | /iam-sso/user/menus  | 获取当前用户菜单 | iam-sso |
| POST | /iam-sso/user/logout | 用户登出     | iam-sso |

### Admin 接口 (`/iam-admin/**`)

| 方法   | 路径                   | 说明       | 模块        |
|------|----------------------|----------|-----------|
| POST | /iam-admin/user/page | 用户分页查询   | iam-admin |
| POST | /iam-admin/role/list | 角色列表查询   | iam-admin |
| POST | /iam-admin/menu/list | 菜单列表查询   | iam-admin |
| POST | /iam-admin/app/page  | 应用分页查询   | iam-admin |
| POST | /iam-admin/api/page  | API 分页查询 | iam-admin |

## 认证与授权

- SSO 公开接口无需鉴权
- SSO 用户接口和 Admin 接口需要 JWT Token 鉴权
- Token 通过 Header 的 `Authorization` 或 `token` 字段传递
