# STORY-042 — Admin 自动配置与路由常量

| 属性 | 值 |
|------|-----|
| Story ID | STORY-042 |
| 所属 Epic | 管理后台 - 自动配置 |
| 所属模块 | iam-admin |
| 优先级 | P0 |
| 状态 | 待开发 |

## 用户故事

**作为** 系统开发者，**我希望** Admin 模块通过自动装配机制注册所有组件，并通过 Route 接口集中管理路由常量，**以便** 引入 iam-admin 依赖后即可使用完整的管理后台功能，且 API 路由能被自动扫描注册。

## 验收标准

1. `IamAdminAutoConfig` 使用 `@AutoConfiguration` + `@ComponentScan` + `@MapperScan`
2. 无条件装配（不依赖配置开关）
3. 注册在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
4. Route 接口定义所有 Admin 路由常量（共 46 个端点定义）
5. 使用 `@Router(module = "iam-admin", prefix = "/iam-admin")` 注解标记
6. 路由前缀：`/iam-admin`
7. 路由分组：用户、用户认证、应用、数据维度、角色、用户角色、角色用户、菜单、角色菜单、用户菜单、角色数据、API、登录日志、请求日志、访问密钥、AK-API、菜单-API

## 技术实现要点

- `@ComponentScan` 扫描 `com.wkclz.iam.admin` 包
- `@MapperScan` 扫描 Mapper 接口
- Route 接口使用 `@Router` 注解，供 RestfulScan 启动时自动扫描注册 API 到数据库
- 路由常量集中定义，确保 RestfulScan 能自动注册
- 新增 REST API 路由必须在 Route 接口中定义常量

## 依赖故事

- STORY-013（SDK 自动装配）

## 涉及文件

| 文件 | 路径 |
|------|------|
| IamAdminAutoConfig | iam-admin/src/main/java/com/wkclz/iam/admin/IamAdminAutoConfig.java |
| Route | iam-admin/src/main/java/com/wkclz/iam/admin/Route.java |
| IamAdminConfig | iam-admin/src/main/java/com/wkclz/iam/admin/config/IamAdminConfig.java |
| AutoConfiguration.imports | iam-admin/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports |
