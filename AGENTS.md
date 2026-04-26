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

| 依赖 | 用途 |
|------|------|
| `sh-web` | Web 基础能力 (IpHelper, RequestHelper, R 响应封装) |
| `sh-mybatis` | MyBatis 封装 (BaseService, PageQuery, BaseMapper) |
| `sh-redis` | Redis 封装 (RedisIdGenerator, RedisTemplate) |
| `sh-bom` | 统一版本管理 (guava 33.5.0-jre, fastjson2, jjwt 等) |
| `micro-dict` | 字典服务 |

---

## 各模块关键类索引

### iam-common (`com.wkclz.iam.common`)

| 包 | 类 | 说明 |
|----|-----|------|
| `entity` | IamUser, IamRole, IamMenu, IamApi, IamApp, IamAccessKey, IamUserAuth, IamUserAuthPassword, IamUserPasswordHis, IamLoginLog, IamRequestLog, IamUserRole, IamRoleMenu, IamMenuApi, IamAccessKeyApi, IamRoleData, IamDataDimension | 17 个实体，对应 17 张表 |
| `dto` | IamUserDto, IamRoleDto, IamMenuDto, ... | 17 个 DTO，均继承对应 Entity |
| `helper` | PasswordHelper, IpLocalCacheHelper | 密码 MD5+salt 加密校验、IP 归属地缓存 |

### iam-sdk (`com.wkclz.iam.sdk`)

| 包 | 类 | 说明 |
|----|-----|------|
| `config` | IamSdkConfig | SDK 配置 (appCode, jwtSecretKey, serverUrl, appId/appSecret) |
| `facade` | SsoFacade | 门面接口: `saveLog(RequestLog)` |
| `service` | IamSsoService | SSO 接口: `tokenCheck(token, authIdentifier)` |
| `filter` | IamAuthFilter, LoggingFilter, RequestWrapperFilter | 鉴权/日志/请求包装过滤器 |
| `helper` | SessionHelper, AkSignHelper, CaptchaHelper, ResponseHelper | Session/AK签名/验证码/响应工具 |
| `model` | UserJwt, UserSession, LoginRequest, LoginResponse, RequestLog, RegisterRequest, PictureCaptchaResponse | SDK 公共模型 |
| `enums` | AuthType, LoginStatus | 认证类型 (PASSWORD/LDAP)、登录状态枚举 |
| `util` | JwtUtil | JWT 生成/解析/验证/刷新 |
| 根包 | IamSdkAutoConfig | 自动配置 (`@AutoConfiguration` + `@ConditionalOnProperty`) |

### iam-sso (`com.wkclz.iam.sso`)

| 包 | 类 | 说明 |
|----|-----|------|
| `config` | IamSsoConfig | SSO 配置 (密码过期天数、RSA 公私钥) |
| `rest` | LoginRest, CaptchaRest, RegisterRest, UserInfoRest | 登录/验证码/注册/用户信息接口 |
| `service` | IamLoginService, IamSsoServiceImpl, SsoFacadeImpl, SsoResourceService, UsernameCacheService, IamRequestService | SSO 核心业务逻辑 |
| `mapper` | SsoLoginMapper, SsoLoginLogMapper, SsoRequestLogMapper, SsoResourceMapper | SSO 数据访问 |
| 根包 | IamSsoAutoConfig, Route | 自动配置 + 路由常量接口 (前缀 `/iam-sso`) |

### iam-admin (`com.wkclz.iam.admin`)

| 包 | 类 | 说明 |
|----|-----|------|
| `rest` | UserRest, RoleRest, MenuRest, AppRest, ApiRest, AccessKeyRest, ... | 17 个 REST 控制器 |
| `service` | IamUserService, IamRoleService, IamMenuService, ... | 17 个 Service |
| `mapper` | IamUserMapper, IamRoleMapper, ... | 17 个 Mapper + XML |
| `init` | RestfulScan | 启动时扫描 @Router 注解 → 同步 API 到数据库 |
| 根包 | IamAdminAutoConfig, Route | 自动配置 + 路由常量接口 (前缀 `/iam-admin`) |

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
  └── iam_user_role [userCode]              ── 用户-角色关联

iam_role (角色)
  ├── iam_role_menu [roleCode]    ── 角色-菜单关联
  └── iam_role_data [roleCode]    ── 角色-数据维度关联

iam_menu (菜单) ── iam_menu_api [menuCode]      ── 菜单-API关联
iam_access_key (AK) ── iam_access_key_api [appId]  ── AK-API关联

iam_login_log   ── 登录日志 (独立)
iam_request_log ── 请求日志 (独立)
```

核心实体字段速查:

| 实体 | 关键字段 |
|------|---------|
| IamUser | userCode, username, nickname, email, phone, avatar, userStatus(1启用/2禁用/3锁定) |
| IamRole | tenantCode, appCode, parentCode, roleCode, roleName |
| IamMenu | appCode, parentCode, menuCode, menuName, menuType(MENU/BUTTON), routePath, component, buttonCode |
| IamApi | module, appCode, apiCode, apiMethod, apiUri, apiName, writeFlag |
| IamUserAuth | userCode, authType(PASSWORD/LDAP), authIdentifier, authStatus(0禁用/1启用) |
| IamAccessKey | appCode, appId, accessKey, secretKey, enableStatus |

---

## 编码约定

### 命名规范

| 类型 | 命名模式 | 示例 |
|------|---------|------|
| Entity | `Iam` + 业务名 | `IamUser`, `IamRole` |
| DTO | `Iam` + 业务名 + `Dto` | `IamUserDto` (继承 IamUser) |
| Service | `Iam` + 业务名 + `Service` | `IamUserService` |
| Mapper | `Iam` + 业务名 + `Mapper` | `IamUserMapper` |
| REST Controller | 业务名 + `Rest` | `UserRest`, `RoleRest` |
| 表名 | `iam_` + 下划线业务名 | `iam_user`, `iam_user_auth` |
| 路由常量 | 集中定义在 `Route` 接口 | `Route.PREFIX + "/user/page"` |

### 注解与风格

- 实体类: `@Data` + `@EqualsAndHashCode(callSuper = false)` + `@Desc("字段描述")`
- Service: `@Service`，继承 `BaseService<Entity, Mapper>`
- 控制器: `@RestController` + `@RequestMapping(Route.PREFIX)`
- 自动配置: `@AutoConfiguration` + `@ComponentScan` + `@MapperScan`
- 条件装配: `@ConditionalOnProperty`, `@ConditionalOnMissingBean`
- 事务: `@Transactional(rollbackFor = Exception.class)`
- REST 响应: 统一使用 `R` 封装 (`com.wkclz.core.base.R`)
- 分页: `PageQuery.page()` → `PageData<T>`
- ID 生成: `RedisIdGenerator.generateIdWithPrefix("user_")`
- 属性拷贝: 实体自带的 `copyIfNotNull()` 静态方法
- 参数校验: `Assert.notNull()` + 自定义异常 (`ValidationException`, `UserException`)

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
