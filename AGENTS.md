# AGENTS.md — sh-iam 项目导航

## 项目概述

sh-iam 是基于 Spring Boot 3.x 的身份认证与访问管理 (IAM) 系统，提供 SSO 登录、JWT 鉴权、用户/角色/菜单/应用/密钥管理等完整能力。项目版本 `5.0.0-SNAPSHOT`，Java 25，继承框架父 POM `sh-parent`。

---

## 模块结构

```
sh-iam/
├── iam-common          # 公共实体、DTO、工具类（所有后端模块依赖）
├── iam-sdk             # SDK 模块（第三方应用引入，提供 Filter/JWT/Session）
├── iam-sso             # SSO 登录/个人中心核心业务
├── iam-sso-starter     # SSO 启动器（可部署 Spring Boot 应用）
├── iam-admin           # 管理后台业务（用户/角色/菜单/应用等 CRUD）
├── iam-admin-starter   # Admin 启动器（可部署 Spring Boot 应用）
├── iam-admin-ui/       # Vue3 管理后台前端（不参与 Maven 构建）
└── iam-sso-ui/         # Vue3 SSO 登录前端（不参与 Maven 构建）
```

### 模块依赖关系

```
iam-admin-starter → iam-admin → iam-sso → iam-common
                                       └→ iam-sdk

iam-sso-starter → iam-sso → iam-common
                          └→ iam-sdk
```

### 框架依赖（来自 sh-parent BOM）

| 依赖           | 用途                                                    |
|--------------|-------------------------------------------------------|
| `sh-web`     | Web 基础能力 (IpHelper, RequestHelper, R 响应封装)            |
| `sh-mybatis` | MyBatis 封装 (BaseService, PageQuery, BaseMapper)       |
| `sh-redis`   | Redis 封装 (RedisIdGenerator, RedisTemplate, RedisLock) |
| `sh-xxljob`  | XXL-Job 定时任务 (XxlJobConfig, @XxlJob 注解)               |
| `sh-bom`     | 统一版本管理 (guava 33.5.0-jre, fastjson2, jjwt 等)          |
| `micro-dict` | 字典服务                                                  |

---

## 各模块关键类索引

### iam-common (`com.wkclz.iam.common`)

| 包        | 类                                                                                                                                                                                                                                                                     | 说明                        |
|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------|
| `entity` | IamUser, IamRole, IamMenu, IamApi, IamApp, IamAccessKey, IamUserAuth, IamUserAuthPassword, IamUserPasswordHis, IamLoginLog, IamRequestLog, IamUserRole, IamRoleMenu, IamMenuApi, IamAccessKeyApi, IamRoleData, IamDataDimension, IamApiField, IamMenuField, IamTenant | 20 个实体，对应 20 张表           |
| `dto`    | IamUserDto, IamRoleDto, IamMenuDto, ...                                                                                                                                                                                                                               | 20 个 DTO，均继承对应 Entity     |
| `helper` | PasswordHelper, IpLocalCacheHelper                                                                                                                                                                                                                                    | 密码 MD5+salt 加密校验、IP 归属地缓存 |

### iam-sdk (`com.wkclz.iam.sdk`)

| 包         | 类                                                                                                         | 说明                                                         |
|-----------|-----------------------------------------------------------------------------------------------------------|------------------------------------------------------------|
| `config`  | IamSdkConfig                                                                                              | SDK 配置 (appCode, jwtSecretKey, serverUrl, appId/appSecret) |
| `facade`  | SsoFacade                                                                                                 | 门面接口: `saveLog(RequestLog)`                                |
| `service` | IamSsoService                                                                                             | SSO 接口: `tokenCheck(token, authIdentifier)`                |
| `filter`  | IamAuthFilter, LoggingFilter, RequestWrapperFilter                                                        | 鉴权/日志/请求包装过滤器                                              |
| `helper`  | SessionHelper, AkSignHelper, CaptchaHelper, ResponseHelper                                                | Session/AK签名/验证码/响应工具                                      |
| `model`   | UserJwt, UserSession, LoginReq, LoginResp, ChangePasswordReq, RegisterReq, PictureCaptchaResp, RequestLog | SDK 公共模型 (均使用 @Schema 注解描述)                                |
| `enums`   | AuthType, LoginStatus                                                                                     | 认证类型 (PASSWORD/LDAP)、登录状态枚举                                |
| `util`    | JwtUtil                                                                                                   | JWT 生成/解析/验证/刷新                                            |
| 根包        | IamSdkAutoConfig                                                                                          | 自动配置 (`@AutoConfiguration` + `@ConditionalOnProperty`)     |

### iam-sso (`com.wkclz.iam.sso`)

| 包         | 类                                                                                                                        | 说明                                                               |
|-----------|--------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|
| `config`  | IamSsoConfig                                                                                                             | SSO 配置 (密码过期天数、RSA 公私钥)                                          |
| `rest`    | LoginRest, CaptchaRest, RegisterRest, UserInfoRest                                                                       | 登录/验证码/注册/用户信息接口 (均使用 @Validated + @Tag + @Operation，参数校验通过注解实现) |
| `service` | IamLoginService, IamSsoServiceImpl, SsoFacadeImpl, SsoResourceService(含若依菜单树转换), UsernameCacheService, IamRequestService | SSO 核心业务逻辑                                                       |
| `mapper`  | SsoLoginMapper, SsoLoginLogMapper, SsoRequestLogMapper, SsoResourceMapper                                                | SSO 数据访问                                                         |
| 根包        | IamSsoAutoConfig, Route                                                                                                  | 自动配置 + 路由常量接口 (前缀 `/iam-sso`)                                    |

### iam-admin (`com.wkclz.iam.admin`)

| 包           | 类                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | 说明                                                                                                                |
|-------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| `rest`      | UserRest, RoleRest, MenuRest, AppRest, ApiRest, AccessKeyRest, AccessKeyApiRest, UserAuthRest, DataDimensionRest, RoleMenuRest, MenuApiRest, RoleDataRest, UserMenuRest, LoginLogRest, RequestLogRest, EntityFieldRest, MenuFieldRest, ApiFieldRest                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | REST 控制器 (均使用 @Validated + @Tag + @Operation，参数校验通过 Req 注解实现)                                                     |
| `bean.req`  | UserPageReq, UserCreateReq, UserUpdateReq, UserRoleBindReq, RoleListReq, RoleCreateReq, RoleUpdateReq, RoleMenuSaveReq, RoleUserPageReq, RoleUserBindReq, RoleUserUnbindReq, RoleDataListReq, RoleDataBindReq, MenuListReq, MenuCreateReq, MenuUpdateReq, MenuApiBindReq, MenuApiListReq, MenuFieldBindReq, MenuFieldSaveReq, AppPageReq, AppCreateReq, AppUpdateReq, ApiPageReq, ApiCreateReq, ApiUpdateReq, ApiListReq, ApiPasteReq, ApiFieldCreateReq, ApiFieldUpdateReq, AccessKeyPageReq, AccessKeyCreateReq, AccessKeyUpdateReq, AccessKeyApiBindReq, UserAuthListReq, UserAuthCreateReq, UserAuthUpdateReq, UserAuthResetPasswordReq, DataDimPageReq, DataDimCreateReq, DataDimUpdateReq, UserMenuListReq, LoginLogPageReq, RequestLogPageReq | 请求参数封装 (继承 PageReq/UpdateReq/IdReq/RemoveReq 或 implements Serializable，使用 @NotBlank/@NotNull/@NotEmpty/@Valid 校验) |
| `bean.resp` | UserResp, RoleResp, MenuResp, AppResp, ApiResp, ApiDetailResp, ApiBoundResp, MenuDetailResp, MenuApiResp, MenuRoleResp, MenuUserResp, AccessKeyResp, AccessKeyApiResp, UserAuthResp, DataDimResp, RoleDataResp, RoleBoundResp, RoleUserResp, UserRoleResp, UserMenuSourceResp, LoginLogResp, RequestLogResp, ApiFieldResp, MenuFieldResp, EntityFieldNode                                                                                                                                                                                                                                                                                                                                                                                            | 响应封装 (继承 EntityResp，使用 @Schema 描述)                                                                                |
| `service`   | IamUserService, IamRoleService(含角色树构建+子角色删除校验), IamMenuService(含路由路径校验+子菜单删除校验), IamUserMenuService, IamUserRoleService, ...                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | 17+ Service                                                                                                       |
| `mapper`    | IamUserMapper, IamRoleMapper, ...                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | 17 个 Mapper + XML                                                                                                 |
| `helper`    | EntityFieldAnalyzer                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | 实体字段分析器 (字段树生成、API返回值实体类自动定位、方法参数推断实体类)                                                                           |
| `job`       | UserRoleExpireJobHandler                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             | 用户角色有效期定时任务 (XXL-Job + @Scheduled 双触发)                                                                            |
| `init`      | RestfulScan                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | 启动时扫描 @Router 注解 → 同步 API 到数据库                                                                                    |
| 根包          | IamAdminAutoConfig, Route                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            | 自动配置 + 路由常量接口 (前缀 `/iam-admin`)                                                                                   |

### iam-admin-ui 前端关键组件 (`src/views/`)

| 路径                                          | 组件                   | 说明                                                 |
|---------------------------------------------|----------------------|----------------------------------------------------|
| `user/user/index.vue`                       | IamUser              | 用户管理主页面                                            |
| `user/user/components/edit.vue`             | IamUserEdit          | 用户新增/编辑弹窗（仅基本信息表单）                                 |
| `user/user/components/detail.vue`           | IamUserDetail        | 用户详情弹窗（基本信息只读+角色管理+菜单来源Tab）                        |
| `user/user/components/reset-password.vue`   | IamUserResetPassword | 重置密码弹窗                                             |
| `user/user/components/user-role.vue`        | IamUserRole          | 用户角色绑定 Tab 面板 (应用选择+角色树+绑定角色弹窗+popover绑定详情)        |
| `user/user/components/user-menu-source.vue` | IamUserMenuSource    | 用户菜单来源 Tab 面板 (树形表格展示菜单层级, 来源角色用 popover 展示有效时间范围) |
| `user/role/index.vue`                       | IamRole              | 角色管理主页面（列表视图+树形视图 Tab 切换，树形视图使用 el-table 树形模式）     |
| `user/role/components/edit.vue`             | IamRoleEdit          | 角色新增/编辑弹窗                                          |
| `user/role/components/menu-bind.vue`        | RoleMenuBind         | 角色菜单绑定弹窗                                           |
| `system/menu/`                              | -                    | 菜单管理 (列表视图+el-table树形视图双Tab)                       |
| `system/api/`                               | -                    | API 管理                                             |
| `system/app/`                               | -                    | 应用管理                                               |
| `system/ak/`                                | -                    | 访问密钥管理                                             |
| `components/AppOptions/`                    | AppSelect            | 应用选择公共组件                                           |

---

## 数据库关系

所有表统一包含系统字段: `id`(bigint unsigned AUTO_INCREMENT), `sort`, `create_time`, `create_by`, `update_time`, `update_by`, `remark`, `version`, `deleted`。删除为逻辑删除，乐观锁通过 `version` 字段。

```
iam_app (应用)
  ├── iam_role [appCode]            ── 角色
  ├── iam_menu [appCode]            ── 菜单
  ├── iam_api [appCode]             ── API路由
  ├── iam_data_dimension [appCode]  ── 数据维度
  └── iam_access_key [appCode]      ── AK密钥

iam_user (用户)
  ├── iam_user_auth [userCode]              ── 认证方式 (PASSWORD/LDAP)
  │     └── iam_user_auth_password [userCode]  ── 密码凭据
  ├── iam_user_password_his [userCode]      ── 密码变更历史
  └── iam_user_role [userCode]              ── 用户-角色关联 (含有效期 startTime/endTime, enableStatus)

iam_role (角色)
  ├── iam_role_menu [roleCode]    ── 角色-菜单关联
  └── iam_role_data [roleCode]    ── 角色-数据维度关联

iam_menu (菜单) ── iam_menu_api [menuCode]      ── 菜单-API关联
iam_access_key (AK) ── iam_access_key_api [appId]  ── AK-API关联

iam_login_log   ── 登录日志 (独立)
iam_request_log ── 请求日志 (独立)
```

核心实体字段速查:

| 实体           | 关键字段                                                                                             |
|--------------|--------------------------------------------------------------------------------------------------|
| IamUser      | userCode, username, nickname, email, phone, avatar, userStatus(1启用/2禁用/3锁定)                      |
| IamRole      | tenantCode, appCode, parentCode, roleCode, roleName, applicable(1=可申请/0=仅树节点)                    |
| IamMenu      | appCode, parentCode, menuCode, menuName, menuType(MENU/BUTTON), routePath, component, buttonCode |
| IamApi       | module, appCode, apiCode, apiMethod, apiUri, apiName, writeFlag                                  |
| IamUserAuth  | userCode, authType(PASSWORD/LDAP), authIdentifier, authStatus(0禁用/1启用)                           |
| IamAccessKey | appCode, appId, accessKey, secretKey, enableStatus                                               |

---

## 编码约定

### 命名规范

| 类型              | 命名模式                    | 示例                                           |
|-----------------|-------------------------|----------------------------------------------|
| Entity          | `Iam` + 业务名             | `IamUser`, `IamRole`                         |
| DTO             | `Iam` + 业务名 + `Dto`     | `IamUserDto` (继承 IamUser)                    |
| Req             | 业务名 + 操作 + `Req`        | `UserCreateReq`, `RoleListReq`, `AppPageReq` |
| Resp            | 业务名 + `Resp`            | `UserResp`, `RoleResp`, `ApiResp`            |
| Service         | `Iam` + 业务名 + `Service` | `IamUserService`                             |
| Mapper          | `Iam` + 业务名 + `Mapper`  | `IamUserMapper`                              |
| REST Controller | 业务名 + `Rest`            | `UserRest`, `RoleRest`                       |
| 表名              | `iam_` + 下划线业务名         | `iam_user`, `iam_user_auth`                  |
| 路由常量            | 集中定义在 `Route` 接口        | `Route.PREFIX + "/user/page"`                |

### 注解与风格

- 实体类: `@Data` + `@EqualsAndHashCode(callSuper = false)` + `@Desc("字段描述")`
- Service: `@Service`，继承 `BaseService<Entity, Mapper>`
- 控制器: `@Validated` + `@RestController` + `@RequestMapping(Route.PREFIX)` + `@Tag(name, description)`
- 控制器方法: `@Operation(summary = "xxx")`，POST 参数使用 `@Valid @RequestBody XxxReq`，GET 参数使用 `@Valid XxxReq`
- REST 响应: 统一使用 `R` 封装 (`com.wkclz.core.base.R`)，单对象返回 `R<XxxResp>`，分页返回 `R<PageData<XxxResp>>`
- Req 类: `@Data` + `@EqualsAndHashCode(callSuper = false)` + `@Schema(description)`，必填字段使用 `@NotBlank`/
  `@NotNull`/`@NotEmpty`
- Resp 类: `@Data` + `@EqualsAndHashCode(callSuper = false)` + `@Schema(description)`，字段仅用 `@Schema` 描述（无校验注解）
- 对象转换: `BeanUtil.cp(source, TargetClass.class)` (`com.wkclz.tool.utils.BeanUtil`)，Controller 层使用
  `BeanUtil.cp(req, Entity.class)` 入参转换、`BeanUtil.cp(entity, Resp.class)` 出参转换
- 参数校验: Controller 层通过 Req 注解校验（`@NotBlank`/`@NotNull`/`@NotEmpty` + `@Valid`），Controller 不使用
  Assert；Service 层使用 `Assert.notNull()` + 自定义异常
- 自动配置: `@AutoConfiguration` + `@ComponentScan` + `@MapperScan`
- 条件装配: `@ConditionalOnProperty`, `@ConditionalOnMissingBean`
- 事务: `@Transactional(rollbackFor = Exception.class)`
- 分页: `PageQuery.page()` → `PageData<T>`，转换使用 `page.convert(Resp.class)`
- ID 生成: `RedisIdGenerator.generateIdWithPrefix("user_")`

### 路由规范

- SSO 公开接口: `/iam-sso/public/**` (无需鉴权)
- SSO 用户接口: `/iam-sso/user/**` (需鉴权)
- Admin 接口: `/iam-admin/**` (需鉴权)
- 关联关系操作统一定义为 `bind` / `unbind`

---

## 核心流程

### SSO 登录流程

```
前端 → CaptchaRest (获取验证码，Redis 存储 5min)
     → LoginRest (提交登录)
     → IamLoginService.loginByUsernameAndPassword():
         1. RSA 解密前端密码
         2. 检查是否需要验证码 (1h 内失败次数)
         3. 验证码校验 (Redis)
         4. SsoLoginMapper 跨三表 JOIN 查用户
         5. 用户不存在 / 禁用 / 锁定 → 返回对应状态
         6. 密码校验: MD5(password + salt)
         7. 密码过期检查 (默认 180 天)
         8. 生成 JWT (HS256) + Redis Session
         9. 记录登录日志 + 更新登录信息
```

### 鉴权流程 (IamAuthFilter)

```
请求 → RequestWrapperFilter → LoggingFilter → IamAuthFilter:
  1. 放行 /*/public/** 路径
  2. 从 Header 获取 token (Authorization 或 token)
  3. JWT 验证 + 解析 UserJwt
  4. IamSsoService.tokenCheck() → Redis 获取 UserSession
  5. SessionHelper.cacheUserInfo() → 请求上下文
```

### 用户创建流程 (IamUserService.customCreate)

```
1. 用户名唯一性校验
2. RedisIdGenerator 生成 userCode (前缀 user_)
3. iam_user 入库
4. iam_user_auth 入库 (authType=PASSWORD)
5. iam_user_auth_password 入库 (salt + MD5)
6. iam_user_password_his 入库
```

---

## 自动配置机制

项目使用 Spring Boot 3.x 自动配置注册方式 (`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`)：

| 模块 | 配置类 | 条件 |
|------|--------|------|
| iam-sdk | IamSdkAutoConfig | `sh.iam.sdk.enabled=true` (默认启用) |
| iam-sso | IamSsoAutoConfig | 无条件 (包含 @ComponentScan + @MapperScan) |
| iam-admin | IamAdminAutoConfig | 无条件 (包含 @ComponentScan + @MapperScan) |

**SsoFacade 可替换设计**: iam-sdk 中定义 `@Bean @ConditionalOnMissingBean SsoFacade` 默认空实现；iam-sso 中的 `SsoFacadeImpl` 会自动覆盖。

---

## 配置项

### iam-sdk 配置 (`iam.sdk.*`)

| 配置 | 说明 | 默认值 |
|------|------|--------|
| `iam.sdk.enabled` | 是否启用 SDK | true |
| `iam.sdk.app-code` | 应用编码 | - |
| `iam.sdk.jwt.secret-key` | JWT 签名密钥 | - |
| `iam.sdk.server-url` | SSO 服务端地址 | - |
| `iam.sdk.app-id` | 应用 ID (AK 鉴权) | - |
| `iam.sdk.app-secret` | 应用密钥 (AK 鉴权) | - |
| `iam.sdk.static.enabled` | 静态资源过滤开关 | true |
| `iam.sdk.static.subfix` | 静态资源后缀 | .css,.js,.png,... |

### iam-sso 配置 (`iam.sso.*`)

| 配置 | 说明 | 默认值 |
|------|------|--------|
| `iam.sso.password-expire-days` | 密码过期天数 | 180 |
| `iam.sso.private-key` | RSA 私钥 (密码解密) | - |
| `iam.sso.public-key` | RSA 公钥 (密码加密) | - |

### iam-admin 定时任务配置 (`iam.job.*`)

| 配置                                 | 说明                         | 默认值             |
|------------------------------------|----------------------------|-----------------|
| `iam.job.user-role-expire.enabled` | Spring @Scheduled 触发开关     | false           |
| `iam.job.user-role-expire.cron`    | Spring @Scheduled cron 表达式 | `0 */5 * * * ?` |

---

## 扩展点

| 扩展点 | 模块 | 方式 |
|--------|------|------|
| Token 校验逻辑 | iam-sdk | 实现 `IamSsoService` 接口覆盖默认 Bean |
| 请求日志存储 | iam-sdk | 实现 `SsoFacade` 接口覆盖默认 Bean |
| 验证码生成 | iam-sso | CaptchaRest 可替换 |
| API 自动扫描 | iam-admin | RestfulScan 于启动时执行，基于 @Router 注解 |
| 密码加密策略 | iam-common | PasswordHelper (当前 MD5+salt) |

---

## 开发须知

- 当前环境无 Maven CLI，编译验证需在 IDE 或安装 Maven 后执行: `mvn compile -pl <module> -am`
- 数据库 DDL 模板见 `iam-sso/src/main/resources/db-script/db-base.ddl.sql`
- 新增实体应遵循现有模式: 继承 BaseEntity + @Desc 注解 + copy/copyIfNotNull 静态方法
- 新增 REST API 路由必须在 Route 接口中定义常量，确保 RestfulScan 能自动注册
- 新增 Service 继承 `BaseService<Entity, Mapper>` 以获得标准 CRUD 能力
- 缓存方案优先使用 Guava LoadingCache (已引入 guava 依赖)，注意最大容量与过期策略
- Redis Key 命名遵循 `iam:模块:业务:标识` 模式

---

## Harness 工程规范

### 代码规范

- [Java 编码规范](docs/coding-standards/java.md)
- [Node/前端编码规范](docs/coding-standards/node.md)

### 研发规范

- [研发过程规范](docs/dev-process.md)
- [需求拆解模板](docs/requirement-template.md)
- [技术活文档](docs/living-docs-technical/)
- [业务活文档](docs/living-docs-business/)
- [开发规范](docs/standards/)
- [Harness 规范](docs/harness-spec.md)

### 技术债务

见 [docs/tech-debts/INDEX.md](docs/tech-debts/INDEX.md)

### 变更记录

见 [changes/](changes/) 目录

### 项目上下文

见 [CONTEXT.md](CONTEXT.md)

### 质量门禁

- lint: `mvn checkstyle:check`
- test: `mvn test`
- build: `mvn package -DskipTests`
- typecheck: `mvn compile`

### 编码规则

> 以下规则为 harness 工程强制规范，AI 编码时必须遵循：

1. **禁止调用系统资源**：仅能使用当前目录下的代码资源，不得调用系统级命令或外部系统资源
2. **保留已有注释**：不要移除已添加的注释，除非相关代码块已变动
3. **关键位置加日志**：实现业务逻辑时，在关键位置添加 log 日志打印（方法入口、分支判断、异常捕获、外部调用）
4. **更新文档**：任务完成后，必须更新本文件（AGENTS.md）以及相关的故事文件
5. **Req/Resp 封装**：所有请求参数封装 Req 对象（除非参数只有一个值），所有返回内容封装 Resp 对象（除非返回只有一个值）

---

## 故事索引 (Story Index)

故事文档目录：`docs/stories/`（按模块分为 `公共基础/`、`SDK鉴权/`、`SSO登录/`、`管理后台/` 子目录）

### 实现状态概览

| 状态      | 数量 | 说明                   |
|---------|----|----------------------|
| ✅ 已完成   | 31 | 核心功能已实现              |
| ⚠️ 部分完成 | 12 | 存在差距，详见各故事说明         |
| ❌ 未实现   | 1  | STORY-010 HTTP 安全过滤器 |

### 公共基础模块 (iam-common)

| Story ID  | 故事名称                        | 优先级 | 文档                                             |
|-----------|-----------------------------|-----|------------------------------------------------|
| STORY-001 | ✅ IAM 实体与 DTO 体系定义          | P0  | [STORY-001](docs/stories/公共基础/001-实体与DTO体系.md) |
| STORY-002 | ✅ 关联关系实体定义                  | P0  | [STORY-002](docs/stories/公共基础/002-关联关系实体.md)   |
| STORY-003 | ⚠️ 密码加密校验工具（缺 BCrypt 双算法兼容） | P0  | [STORY-003](docs/stories/公共基础/003-密码加密校验.md)   |
| STORY-004 | ✅ IP 归属地缓存工具                | P1  | [STORY-004](docs/stories/公共基础/004-IP归属地缓存.md)  |

### SDK 鉴权与安全模块 (iam-sdk)

| Story ID  | 故事名称                                            | 优先级 | 文档                                                            |
|-----------|-------------------------------------------------|-----|---------------------------------------------------------------|
| STORY-005 | ✅ JWT 令牌生成与校验                                   | P0  | [STORY-005](docs/stories/SDK鉴权/005-JWT令牌生成与校验.md)             |
| STORY-006 | ✅ 用户会话上下文管理                                     | P0  | [STORY-006](docs/stories/SDK鉴权/006-用户会话上下文.md)                |
| STORY-007 | ✅ IAM 鉴权过滤器                                     | P0  | [STORY-007](docs/stories/SDK鉴权/007-IAM鉴权过滤器.md)               |
| STORY-008 | ✅ 请求日志采集过滤器                                     | P0  | [STORY-008](docs/stories/SDK鉴权/008-请求日志过滤器.md)                |
| STORY-009 | ✅ 请求体可重复读取包装                                    | P0  | [STORY-009](docs/stories/SDK鉴权/009-请求体包装过滤器.md)               |
| STORY-010 | ❌ HTTP 安全过滤器                                    | P1  | [STORY-010](docs/stories/SDK鉴权/010-HTTP安全过滤器.md)              |
| STORY-011 | ✅ AK 签名工具（deSign/verifySign + IamAuthFilter 集成） | P1  | [STORY-011](docs/stories/SDK鉴权/011-AK签名工具.md)                 |
| STORY-012 | ✅ 图形验证码生成                                       | P0  | [STORY-012](docs/stories/SDK鉴权/012-图形验证码生成.md)                |
| STORY-013 | ⚠️ SDK 配置与自动装配（缺 ConfigValidator 启动校验）          | P0  | [STORY-013](docs/stories/SDK鉴权/013-SDK配置与自动装配.md)             |
| STORY-014 | ✅ SsoFacade 门面与 SsoService 接口                   | P0  | [STORY-014](docs/stories/SDK鉴权/014-SsoFacade门面与SsoService.md) |

### SSO 登录认证模块 (iam-sso)

| Story ID  | 故事名称             | 优先级 | 文档                                                             |
|-----------|------------------|-----|----------------------------------------------------------------|
| STORY-015 | ✅ 用户名密码登录        | P0  | [STORY-015](docs/stories/STORY-015-username-password-login.md) |
| STORY-016 | ✅ 图形验证码接口        | P0  | [STORY-016](docs/stories/STORY-016-captcha-rest.md)            |
| STORY-017 | ⚠️ 用户注册接口（空占位实现） | P2  | [STORY-017](docs/stories/STORY-017-user-register.md)           |
| STORY-018 | ✅ 用户登出           | P0  | [STORY-018](docs/stories/STORY-018-user-logout.md)             |
| STORY-019 | ✅ 用户信息与菜单资源查询    | P0  | [STORY-019](docs/stories/STORY-019-user-info-menu-resource.md) |
| STORY-020 | ✅ 若依格式菜单树适配      | P1  | [STORY-020](docs/stories/STORY-020-ruoyi-menu-tree.md)         |
| STORY-021 | ✅ Token 校验服务实现   | P0  | [STORY-021](docs/stories/STORY-021-token-check-service.md)     |
| STORY-022 | ✅ 请求日志持久化服务      | P1  | [STORY-022](docs/stories/STORY-022-request-log-persistence.md) |
| STORY-023 | ✅ 用户名缓存服务        | P1  | [STORY-023](docs/stories/STORY-023-username-cache-service.md)  |
| STORY-024 | ✅ SSO 配置与自动装配    | P0  | [STORY-024](docs/stories/STORY-024-sso-auto-config.md)         |

### 管理后台模块 (iam-admin)

| Story ID     | 故事名称                   | 优先级 | 文档                                                                                                                                                   |
|--------------|------------------------|-----|------------------------------------------------------------------------------------------------------------------------------------------------------|
| STORY-025    | ✅ 用户 CRUD 管理           | P0  | [STORY-025](docs/stories/STORY-025-user-crud.md)                                                                                                     |
| STORY-026    | ⚠️ 用户认证方式管理（部分实现）      | P1  | [STORY-026](docs/stories/STORY-026-user-auth-management.md)                                                                                          |
| STORY-027    | ✅ 角色 CRUD 管理           | P0  | [STORY-027](docs/stories/STORY-027-role-crud.md)                                                                                                     |
| STORY-028    | ✅ 菜单 CRUD 与树形管理        | P0  | [STORY-028](docs/stories/STORY-028-menu-crud-tree.md)                                                                                                |
| STORY-029    | ✅ 应用 CRUD 管理           | P0  | [STORY-029](docs/stories/STORY-029-app-crud.md)                                                                                                      |
| STORY-030    | ✅ API 路由 CRUD 管理       | P0  | [STORY-030](docs/stories/STORY-030-api-crud.md)                                                                                                      |
| STORY-030-01 | ⚠️ API 详情页与菜单绑定数（部分实现） | P1  | API 详情弹窗展示已绑定菜单全路径，API 列表增加 menuBindCount 字段和 menuBindStatus 过滤                                                                                      |
| STORY-031    | ✅ API 自动扫描同步           | P1  | [STORY-031](docs/stories/STORY-031-api-auto-scan.md)                                                                                                 |
| STORY-032    | ✅ 访问密钥 CRUD 管理         | P1  | [STORY-032](docs/stories/STORY-032-access-key-crud.md)                                                                                               |
| STORY-033    | ⚠️ AK-API 关联管理（部分实现）   | P1  | [STORY-033](docs/stories/STORY-033-ak-api-binding.md)                                                                                                |
| STORY-034    | ✅ 角色-菜单关联管理            | P0  | [STORY-034](docs/stories/STORY-034-role-menu-binding.md)                                                                                             |
| STORY-035    | ✅ 角色-用户与用户-角色关联管理      | P0  | [STORY-035](docs/stories/STORY-035-user-role-binding.md) — 角色下用户查询已改为分页，支持 username 精确匹配、nickname 模糊搜索，返回 PageData<RoleUserResp>（含 createTime）       |
| STORY-036    | ✅ 菜单-API 关联管理          | P0  | [STORY-036](docs/stories/STORY-036-menu-api-binding.md)                                                                                              |
| STORY-036-01 | ⚠️ 菜单详情页与穿梭框绑定（部分实现）   | P1  | 菜单详情弹窗（全量API+已绑定API前端组装），已绑定的绑定按钮禁用，绑定/解绑后前端本地更新，菜单列表增加接口数列，已绑定角色（roleCode+roleName），关联用户（username/nickname/roleName/startTime/endTime/enableStatus） |
| STORY-037    | ⚠️ 数据权限维度管理（部分实现）      | P1  | [STORY-037](docs/stories/STORY-037-data-dimension-crud.md)                                                                                           |
| STORY-038    | ⚠️ 角色-数据权限关联管理（部分实现）   | P1  | [STORY-038](docs/stories/STORY-038-role-data-binding.md)                                                                                             |
| STORY-039    | ⚠️ 登录日志查询（部分实现）        | P1  | [STORY-039](docs/stories/STORY-039-login-log-query.md)                                                                                               |
| STORY-040    | ⚠️ 请求日志查询（部分实现）        | P1  | [STORY-040](docs/stories/STORY-040-request-log-query.md)                                                                                             |
| STORY-041    | ✅ 当前用户菜单查询             | P0  | [STORY-041](docs/stories/STORY-041-user-menu-query.md)                                                                                               |
| STORY-042    | ✅ Admin 自动配置与路由常量      | P0  | [STORY-042](docs/stories/STORY-042-admin-auto-config.md)                                                                                             |

### 故事依赖关系概览

```
STORY-001 (实体体系) ──→ STORY-002 (关联实体) ──→ STORY-003 (密码工具)
     │                       │
     ├──→ STORY-005 (JWT) ──→ STORY-006 (会话上下文) ──→ STORY-007 (鉴权过滤器)
     │        │                                          ├──→ STORY-015 (登录)
     │        │                                          └──→ STORY-018 (登出)
     │        └──→ STORY-021 (Token校验)
     │
     ├──→ STORY-012 (验证码) ──→ STORY-016 (验证码接口)
     │
     ├──→ STORY-025 (用户CRUD) ──→ STORY-026 (认证方式管理)
     ├──→ STORY-027 (角色CRUD) ──→ STORY-034 (角色-菜单) ──→ STORY-036 (菜单-API)
     │                            ├──→ STORY-035 (角色-用户)
     │                            └──→ STORY-038 (角色-数据权限)
     ├──→ STORY-028 (菜单CRUD)
     ├──→ STORY-029 (应用CRUD)
     ├──→ STORY-030 (API CRUD) ──→ STORY-031 (API自动扫描)
     └──→ STORY-032 (AK CRUD) ──→ STORY-033 (AK-API关联)

STORY-009 (请求包装) ──→ STORY-008 (日志过滤器) ──→ STORY-022 (日志持久化)
STORY-013 (SDK配置)  ──→ STORY-010 (安全过滤器)
                    ──→ STORY-011 (AK签名)
                    ──→ STORY-014 (SsoFacade) ──→ STORY-021 (Token校验)
STORY-024 (SSO配置)  ──→ STORY-019 (用户信息与菜单)
                    ──→ STORY-020 (若依菜单树)
                    ──→ STORY-023 (用户名缓存)
STORY-042 (Admin配置) ──→ STORY-031 (API自动扫描)
```
