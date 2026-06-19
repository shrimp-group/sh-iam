# 业务模块索引

最后更新：2026-06-19

## 模块列表

| 模块名    | 业务职责                        | 关联技术模块             |
|--------|-----------------------------|--------------------|
| 身份认证   | 用户登录、登出、Token 校验            | iam-sso            |
| 访问控制   | RBAC 权限模型，用户-角色-菜单权限链       | iam-admin, iam-sso |
| 用户管理   | 用户 CRUD、认证方式管理、密码管理         | iam-admin          |
| 角色管理   | 角色 CRUD、角色树、角色-菜单/用户/数据权限绑定 | iam-admin          |
| 菜单管理   | 菜单 CRUD、菜单树、菜单-API 绑定       | iam-admin          |
| 应用管理   | 应用 CRUD、多应用隔离               | iam-admin          |
| API 管理 | API 路由 CRUD、自动扫描、API 字段管理   | iam-admin          |
| 密钥管理   | AK/SK CRUD、AK-API 绑定        | iam-admin          |
| 数据权限   | 数据维度管理、角色-数据维度绑定            | iam-admin          |
| 审计日志   | 登录日志、请求日志查询                 | iam-admin, iam-sso |
| SDK 集成 | 第三方应用鉴权、请求日志采集              | iam-sdk            |
