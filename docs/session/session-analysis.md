# 会话全流程功能分析文档

> 生成日期：2026-07-17
> 用途：会话相关功能重构前的现状分析
> 范围：登录、登出、权限验证、日志记录等与会话生命周期相关的全部逻辑

---

## 一、模块与会话职责总览

```
┌──────────────────────────────────────────────────────────────┐
│                    会话相关模块分布                              │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  sh-auth (框架级，零 IAM 依赖)                                  │
│  ├── 过滤器链：包装 → 日志采集 → 安全头 → 认证 → 鉴权              │
│  ├── LoginService：登录模板方法                                  │
│  ├── StandardLoginPipeline：会话创建（Token + Session + 持久化）  │
│  ├── SPI 契约：SessionStore / TokenService / LogoutService ... │
│  ├── SecurityContext：ThreadLocal 安全上下文                     │
│  ├── 缓存体系：三层 Guava Cache                                  │
│  └── 配置：AuthProperties / AuthConstants                      │
│                                                              │
│  iam-sso (SSO 实现层)                                          │
│  ├── REST：LoginRest / CaptchaRest / RegisterRest / UserInfoRest│
│  ├── IamLoginService：SSO 登录编排 + 登出 + 改密                 │
│  ├── SPI 实现：RedisSessionStore / RedisConcurrentSessionControl│
│  ├── IamLoginPipeline：验证码 + 登录日志                         │
│  ├── IamPasswordAuthenticationProvider：密码凭证校验             │
│  ├── IamRequestService：请求日志持久化                            │
│  └── 配置：IamSsoConfig (JWT密钥 / RSA密钥 / 密码过期天数)       │
│                                                              │
│  iam-admin (管理后台层)                                         │
│  ├── REST：用户/角色/菜单/API/密钥/认证方式 CRUD                   │
│  ├── 日志查询：LoginLogRest / RequestLogRest                     │
│  ├── 定时任务：UserRoleExpireJobHandler                          │
│  ├── API 扫描：RestfulScan                                      │
│  └── 【缺口】多处管理操作未联动会话失效                              │
│                                                              │
│  iam-admin-ui (前端)                                           │
│  └── 【缺口】无活跃会话列表 / 强制下线界面                          │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## 二、会话完整生命周期

### 2.1 创建（登录）

```
前端 POST /iam-sso/public/login { username, password, captchaId, captchaCode }
  │
  ▼
LoginRest.publicSsoLogin()
  │
  ▼
IamLoginService.loginByUsernameAndPassword(request)
  │  1. RSA 私钥解密前端密码（仅 length > 32 时解密）
  │  2. 查询 1h 内失败记录 → 判断是否需要验证码
  │  3. 构建 AuthRequest(authType=PASSWORD, credential, extra.username)
  │
  ▼
LoginService.login(authRequest, httpRequest)  [模板方法]
  │  a. checkRateLimit()          → IamLoginPipeline 实现为 no-op
  │  b. checkCaptcha()            → IamLoginPipeline → captchaService.verify()
  │  c. AuthenticationProvider.authenticate(request)
  │       └─ IamPasswordAuthenticationProvider
  │            ├─ 三表 JOIN 查询（iam_user + iam_user_auth + iam_user_auth_password）
  │            ├─ 账号状态校验：authStatus=0 → 认证方式禁用
  │            │               userStatus=3 → 用户锁定
  │            │               userStatus=2 → 用户禁用
  │            ├─ PasswordEncoder.matches() 密码对比
  │            │     └─ DefaultPasswordEncoder: {PBKDF2} / {MD5} / 无前缀 路由
  │            └─ 密码过期检查（lastChangedTime + expireDays > now）
  │  d. AccountStatusChecker.checkStatus() → no-op（已在 authenticate 校验）
  │  e. checkMfa()                → 返回 null（暂不支持 MFA）
  │  f. StandardLoginPipeline.execute(principal, authType, ip, ua)
  │       ├─ Step1: TokenService.generateToken(Principal)
  │       │     └─ JwtTokenService: HS256 签名 JWT，claims 含 userCode/username/
  │       │        nickname/avatar/appCode/authIdentifier，有效期 24h
  │       ├─ Step2: buildSession(principal, authType, tokenValue, ip, ua)
  │       │     └─ Session.sessionId = tokenValue（Token 即 SessionId）
  │       ├─ Step3: SessionStore.save(session)
  │       │     └─ RedisSessionStore:
  │       │          SET  sh-auth:session:{tokenMd5} = Session JSON, TTL 24h
  │       │          SET  sh-auth:token:owner:{tokenMd5} = username
  │       │          ZADD sh-auth:session:list:{username} {timestamp} {tokenMd5}
  │       └─ Step4: ConcurrentSessionControl.enforce(username)
  │             └─ RedisConcurrentSessionControl:
  │                  ZSet size > maxConcurrent → ZRANGE 最早 (size-max) 个 → 逐个删除
  │  g. recordLoginLog()          → INSERT iam_login_log
  │
  ▼ SSO 后处理
  │  4. updateUserLoginInfoByUserCode(userCode, ip) → 更新最后登录 IP
  │  5. AuthResult → LoginResp { token, userCode, username, nickname, avatar }
  │
  ▼
前端收到 LoginResp，存储 token 后续请求携带
```

### 2.2 验证（每次请求）

```
请求进入
  │
  ▼
RequestWrapperFilter（order: MIN_VALUE）
  └─ EagerContentCachingRequestWrapper 包装请求体（缓存 body 字节数组）
  ▼
RequestRecordFilter（order: MIN_VALUE + 1000）
  └─ ContentCachingResponseWrapper 包装响应 + 记录 startTime
  ▼
SecurityHeaderFilter（order: MIN_VALUE + 2000）
  └─ 注入安全响应头（X-Content-Type-Options: nosniff 等）
  ▼
AuthenticationFilter（order: MIN_VALUE + 3000）★ 会话验证核心
  │  1. 白名单放行：Ant 匹配 /public/**, /**/public/**, /error
  │  2. Token 提取：优先 Authorization: Bearer xxx，其次 token 自定义头
  │  3. TokenService.parseToken(token)     → JWT 解析为 Principal
  │  4. TokenService.validateToken(token)  → JWT 签名 + 过期硬校验
  │  5. SessionStore.get(token)            → Redis 查活
  │     └─ null → 401 "会话不存在或已过期"
  │  6. SecurityContext.setPrincipal(principal) + setToken(token)
  │
  ▼
AuthorizationFilter（order: MIN_VALUE + 4000，条件装配）
  │  1. 白名单放行
  │  2. 无 AccessControlProvider → 放行
  │  3. 遍历 AccessControlProvider.hasPermission() → 任一拒绝 → 403
  │
  ▼
业务 Controller
  │
  ▼
RequestRecordFilter.finally（请求返回后）
  │  1. buildRecord()：采集请求/响应/安全上下文/耗时/异常
  │  2. 脱敏处理：token 截断前8+后4、password 字段正则替换 ***
  │  3. RequestLogger.save()：异步持久化到 iam_request_log
  │  4. SecurityContext.clear()：清理全部 ThreadLocal
  │  5. responseWrapper.copyBodyToResponse()：写回原始响应
  │
  ▼
响应返回客户端
```

### 2.3 销毁（登出 / 强制下线）

```
┌─────────────────── 三种销毁路径 ───────────────────┐
│                                                   │
│  路径1：主动登出                                     │
│  GET /iam-sso/public/logout                       │
│    → IamLoginService.logout()                     │
│      → SecurityContext.getToken()                 │
│      → SessionStore.delete(token)                 │
│          DEL  sh-auth:session:{tokenMd5}          │
│          DEL  sh-auth:token:owner:{tokenMd5}      │
│          ZREM sh-auth:session:list:{user} {token} │
│                                                   │
│  路径2：修改密码（全会话失效）                           │
│  IamLoginService.changePassword()                 │
│    → 旧密码校验 → 历史密码检查 → 编码新密码 → 更新DB    │
│    → SessionStore.deleteBySubjectId(username)     │
│        ZRANGE sh-auth:session:list:{user} 全量     │
│        → 批量 DEL session + owner key             │
│        → DEL list key                             │
│                                                   │
│  路径3：并发会话超限踢出                                │
│  StandardLoginPipeline.enforce()                  │
│    → ZSet size > maxConcurrent                    │
│    → ZRANGE 最早 (size-max) 个                     │
│    → 逐个删除 session                              │
│                                                   │
│  路径4：LogoutService（管理员工具）                     │
│  DefaultLogoutService.logout(sessionId)           │
│    → SessionStore.delete(sessionId)               │
│  DefaultLogoutService.invalidateAllSessions(user) │
│    → SessionStore.deleteBySubjectId(user)          │
│                                                   │
└───────────────────────────────────────────────────┘
```

### 2.4 刷新（Token 续期）

```
当前状态：TokenService.refreshToken() 已实现但未集成到请求链路

JwtTokenService.refreshToken(oldToken)
  → parseToken(oldToken) 获取 Principal
  → generateToken(principal) 生成新 JWT（新 iat/exp）

SessionStore.refresh(sessionId, ttlSeconds) 已定义
  → EXPIRE sh-auth:session:{tokenMd5} {ttl}

★ 问题：AuthenticationFilter 验证通过后未调用 refresh()，Session Redis TTL 不会自动续期
```

---

## 三、各模块功能点详细列表

### 3.1 sh-auth 模块

| # | 功能点 | 文件 | 说明 |
|---|--------|------|------|
| 1 | 请求体缓存包装 | `filter/EagerContentCachingRequestWrapper.java` | 构造函数中主动缓存 body 字节数组，支持后续重复读取 |
| 2 | 请求包装 Filter | `filter/RequestWrapperFilter.java` | order=MIN_VALUE，包装每个请求为 EagerContentCachingRequestWrapper |
| 3 | 请求日志采集 Filter | `filter/RequestRecordFilter.java` | order=MIN_VALUE+1000，finally 块采集全量请求信息、脱敏、持久化、清理 SecurityContext |
| 4 | 安全头注入 Filter | `filter/SecurityHeaderFilter.java` | order=MIN_VALUE+2000，注入 X-Content-Type-Options / X-Frame-Options 等 |
| 5 | 认证 Filter | `filter/AuthenticationFilter.java` | order=MIN_VALUE+3000，JWT 解析+校验 + Redis Session 查活 → 设置 SecurityContext |
| 6 | 鉴权 Filter | `filter/AuthorizationFilter.java` | order=MIN_VALUE+4000，遍历 AccessControlProvider 做权限判断 |
| 7 | 登录模板方法 | `contract/auth/LoginService.java` | 7 步标准流程：限流→验证码→凭证校验→状态检查→MFA→会话创建→日志 |
| 8 | 会话创建管道 | `contract/auth/StandardLoginPipeline.java` | 4 步：JWT 生成 → Session 构建 → 持久化 → 并发控制 |
| 9 | Token 服务 SPI | `contract/auth/TokenService.java` | generateToken / parseToken / validateToken / refreshToken |
| 10 | JWT Token 实现 | `contract/auth/impl/JwtTokenService.java` | sh-auth 内的默认实现（非 Bean），HS256 签名，jjwt 0.12.x |
| 11 | 会话存储 SPI | `contract/auth/SessionStore.java` | save / get / delete / deleteBySubjectId / refresh / getActiveSessions |
| 12 | 登出服务 | `contract/auth/impl/DefaultLogoutService.java` | logout() 当前 / logout(sessionId) 指定 / invalidateAllSessions 全部 |
| 13 | 并发会话控制 SPI | `contract/auth/ConcurrentSessionControl.java` | enforce / getCurrentCount / getMaxSessions |
| 14 | 密码编码器 SPI | `contract/auth/PasswordEncoder.java` | encode / matches |
| 15 | 统一密码编码器 | `contract/auth/impl/DefaultPasswordEncoder.java` | 编码：固定 PBKDF2WithHmacSHA256(100K迭代)；校验：{PBKDF2}/{MD5}/无前缀自动路由 |
| 16 | 安全上下文 | `context/SecurityContext.java` | 4个 ThreadLocal：Principal/Token/TenantCode/AppCode；双存储（ThreadLocal+Request属性） |
| 17 | 三层缓存 | `cache/AuthCacheManager.java` | L1 应用元数据(1h) / L2 用户授权(30min) / L3 权限结果(30min) |
| 18 | 缓存刷新事件 | `cache/AuthCacheRefreshEvent.java` + `AuthCacheRefreshListener.java` | Spring Event 驱动，支持 METADATA/SUBJECT/ALL 三种刷新范围 |
| 19 | 配置属性 | `config/AuthProperties.java` | session.ttl=86400, maxConcurrent=5, password.expireDays=180, rateLimit 等 |
| 20 | AK 签名工具 | `helper/AkSignHelper.java` | RSA 签名/解密/验签 + Redis SETNX 防重放，用于跨应用 HTTP RPC |
| 21 | 验证码 SPI | `contract/auth/CaptchaService.java` | generate / verify |
| 22 | 默认验证码实现 | `contract/auth/impl/DefaultCaptchaService.java` | Guava 本地缓存，TTL 5min |
| 23 | 请求日志 SPI | `contract/infra/RequestLogger.java` | save(RequestRecord) |
| 24 | 认证元数据 SPI | `contract/infra/AuthMetadataService.java` | loadMetadata / loadSubjectAuth |
| 25 | Filter 注册配置 | `config/ShAutFilterConfiguration.java` | 按 FilterOrder 排序注册全部 5 个 Filter |
| 26 | Bean 注册配置 | `config/ShAuthBeanConfiguration.java` | 注册 DefaultLogoutService / SecurityContext / AuthCacheManager |

### 3.2 iam-sso 模块

| # | 功能点 | 文件 | 说明 |
|---|--------|------|------|
| 27 | SSO 登录编排 | `service/IamLoginService.java` | loginByUsernameAndPassword (RSA解密→验证码判断→委托LoginService→更新IP→转换Resp) |
| 28 | SSO 登出 | `service/IamLoginService.java` | logout() → SecurityContext.getToken() → sessionStore.delete(token) |
| 29 | SSO 改密+全会话失效 | `service/IamLoginService.java` | changePassword() → 旧密码校验→历史检查→编码新密码→更新DB→deleteBySubjectId |
| 30 | 登录 REST | `rest/LoginRest.java` | POST /iam-sso/public/login, GET /iam-sso/public/logout |
| 31 | 验证码 REST | `rest/CaptchaRest.java` | 图形验证码获取 |
| 32 | 注册 REST | `rest/RegisterRest.java` | 用户注册（空占位实现） |
| 33 | 用户信息 REST | `rest/UserInfoRest.java` | 用户信息与菜单资源查询 |
| 34 | IAM 登录管道 | `contract/auth/IamLoginPipeline.java` | 继承 LoginService，实现 checkRateLimit(no-op) / checkCaptcha / checkMfa(null) / recordLoginLog |
| 35 | 密码认证提供者 | `contract/auth/IamPasswordAuthenticationProvider.java` | 三表JOIN→状态校验→密码对比→密码过期检查→构建Principal |
| 36 | Redis 会话存储 | `service/RedisSessionStore.java` | 3种 Redis Key：session:{md5} / owner:{md5} / list:{user} ZSet |
| 37 | Redis 并发控制 | `service/RedisConcurrentSessionControl.java` | enforce() 按 ZSet score 踢最早会话 |
| 38 | Redis 验证码 | `service/RedisCaptchaService.java` | Key: iam:captcha:{id}, TTL 5min, getAndDelete 一次性消费 |
| 39 | JWT Token 服务 | `contract/JwtTokenService.java` | @Component Bean（实际生效），密钥来自 IamSsoConfig，24h TTL |
| 40 | 请求日志服务 | `service/IamRequestService.java` | 实现 RequestLogger，异步写入 iam_request_log（含UA解析、截断、脱敏、IP归属地） |
| 41 | 用户名缓存 | `service/UsernameCacheService.java` | Guava Cache: userCode→nickname, 10K条, 60min写/30min读过期 |
| 42 | SSO 资源服务 | `service/SsoResourceService.java` | 菜单资源查询 + 若依格式菜单树转换 |
| 43 | LocalSsoFacadeContract | `contract/LocalSsoFacadeContract.java` | 本地会话创建/登出实现，委托 StandardLoginPipeline |
| 44 | HttpSsoFacadeContract | `contract/HttpSsoFacadeContract.java` | 远程 HTTP RPC 会话创建/登出（AK 签名） |
| 45 | SSO 配置 | `config/IamSsoConfig.java` | jwt-secret-key, maxConcurrentSessions, passwordExpireDays, RSA公私钥 |
| 46 | SSO 自动配置 | `IamSsoAutoConfig.java` | @ComponentScan + @MapperScan |
| 47 | SSO Mapper | `mapper/SsoLoginMapper/LogMapper/RequestLogMapper/ResourceMapper.java` | 登录/日志/资源数据访问 |

### 3.3 iam-admin 模块

| # | 功能点 | 文件 | 说明 |
|---|--------|------|------|
| 48 | 用户 CRUD | `rest/UserRest.java` + `service/IamUserService.java` | 创建/更新/删除/分页查询 |
| 49 | 角色 CRUD | `rest/RoleRest.java` + `service/IamRoleService.java` | 含角色树构建 + 子角色删除校验 |
| 50 | 菜单 CRUD | `rest/MenuRest.java` + `service/IamMenuService.java` | 含路由路径校验 + 子菜单删除校验 |
| 51 | 应用 CRUD | `rest/AppRest.java` | 应用管理 |
| 52 | API CRUD | `rest/ApiRest.java` | API 路由管理 |
| 53 | 访问密钥 CRUD | `rest/AccessKeyRest.java` | AK 密钥管理 |
| 54 | 用户认证方式管理 | `rest/UserAuthRest.java` + `service/IamUserAuthService.java` | authStatus 启用/禁用，authIdentifier 变更 |
| 55 | 管理员重置密码 | `rest/UserAuthRest.java` + `service/IamUserAuthPasswordService.java` | resetPassword(userCode, newPassword) |
| 56 | 登录日志查询 | `rest/LoginLogRest.java` + `service/IamLoginLogService.java` | 分页查询，30 天窗口限制 |
| 57 | 请求日志查询 | `rest/RequestLogRest.java` + `service/IamRequestLogService.java` | 分页查询，支持 username/userCode/requestUri/httpStatus 过滤 |
| 58 | 用户-角色绑定 | `rest/UserRoleRest.java` + `service/IamUserRoleService.java` | 含 startTime/endTime 有效期 + enableStatus |
| 59 | 角色-用户绑定 | `rest/RoleUserRest.java` | 角色批量绑定/解绑用户 |
| 60 | 角色-菜单绑定 | `rest/RoleMenuRest.java` | 角色菜单关联 |
| 61 | 菜单-API 绑定 | `rest/MenuApiRest.java` | 菜单 API 关联 |
| 62 | AK-API 绑定 | `rest/AccessKeyApiRest.java` | AK API 关联 |
| 63 | 角色-数据权限绑定 | `rest/RoleDataRest.java` | 角色数据维度关联 |
| 64 | 数据维度 CRUD | `rest/DataDimensionRest.java` | 数据权限维度管理 |
| 65 | 用户菜单查询 | `rest/UserMenuRest.java` | 通过 SecurityContext.getUserCode() 获取当前用户菜单 |
| 66 | 角色有效期定时任务 | `job/UserRoleExpireJobHandler.java` | Spring @Scheduled + XXL-Job，刷新 enableStatus |
| 67 | API 自动扫描 | `init/RestfulScan.java` | ApplicationRunner，启动时扫描 @RequestMapping → 同步 iam_api 表 |
| 68 | 实体字段分析器 | `helper/EntityFieldAnalyzer.java` | 反射解析实体字段，构建 JSONPath 树 |
| 69 | Admin 自动配置 | `IamAdminAutoConfig.java` | @ComponentScan + @MapperScan |
| 70 | Admin 路由常量 | `Route.java` | 全部管理接口路由常量（前缀 /iam-admin） |

---

## 四、Redis Key 空间分析

### 4.1 新版 Key（sh-auth SPI 体系，当前主要使用）

| Key 模式 | 类型 | TTL | 用途 |
|----------|------|-----|------|
| `sh-auth:session:{md5(token)}` | String (Session JSON) | 24h | 会话主存储 |
| `sh-auth:token:owner:{md5(token)}` | String (username) | 24h | Token→用户反向映射 |
| `sh-auth:session:list:{username}` | ZSet (score=timestamp, member=tokenMd5) | 24h | 用户会话列表，按时间排序 |
| `iam:captcha:{captchaId}` | String | 5min | 验证码存储 |

### 4.2 旧版 Key（JwtAuthContract 体系，可能仍在使用）

| Key 模式 | 说明 |
|----------|------|
| `iam:session:username:tokenMd5` | 旧版会话存储 |
| `iam:session:list:username` | 旧版会话列表 |

> ★ **问题**：两套 Key 空间并存，修改密码时新版 deleteBySubjectId 删除了新版 key，但旧版 key 未被同步清理。

---

## 五、已识别的缺口与问题

### 5.1 安全缺口（高优先级）

| # | 问题 | 位置 | 影响 |
|---|------|------|------|
| G1 | **管理员重置密码不清除会话** | `IamUserAuthPasswordService.resetPassword()` | 管理员重置用户密码后，用户所有现有 Session 仍有效 |
| G2 | **禁用认证方式不清除会话** | `IamUserAuthService.update()` authStatus→0 | 管理员禁用用户认证方式后，用户仍可访问 |
| G3 | **禁用/锁定用户不清除会话** | `IamUserService.update()` userStatus→2/3 | 管理员禁用或锁定用户后，用户仍可访问 |
| G4 | **角色解绑不清除会话** | `IamUserRoleService.unbind()` | 用户角色被移除后，权限缓存不立即失效 |
| G5 | **角色过期不清除会话** | `UserRoleExpireJobHandler` | 角色过期仅更新 enableStatus，不解绑，不刷新缓存 |

### 5.2 架构缺口

| # | 问题 | 说明 |
|---|------|------|
| G6 | **会话 TTL 不自动续期** | AuthenticationFilter 验证通过后未调用 sessionStore.refresh()，持续活跃的用户 24h 后 Session 自动过期 |
| G7 | **两套 Redis Key 空间并存** | 新版 `sh-auth:session:*` 和旧版 `iam:session:*` 未统一，可能导致清理不完整 |
| G8 | **两套认证路径并存** | `AuthenticationFilter`（新 sh-auth 体系）和 `JwtAuthContract`（旧 iam-sso 体系）都在做 JWT+Session 验证 |
| G9 | **Token 刷新未集成** | TokenService.refreshToken() 已实现，但未在任何 Filter 或业务代码中调用 |
| G10 | **无会话管理界面** | 管理员无法查看活跃会话列表、无法在会话级别强制下线 |
| G11 | **IamSessionService 不存在** | AGENTS.md 文档中描述 IamSessionService 存在，但实际代码中会话管理分散在 RedisSessionStore / DefaultLogoutService / StandardLoginPipeline 中 |

### 5.3 数据一致性问题

| # | 问题 | 说明 |
|---|------|------|
| G12 | **Token MD5 单向映射** | Redis Key 使用 token 的 MD5 值，无法从 Key 反推 token，管理界面需额外映射 |
| G13 | **并发 ZSet 无自动过期** | `sh-auth:session:list:{user}` ZSet 在用户所有 session 过期后可能残留空 ZSet |
| G14 | **登录日志与请求日志独立** | 两表无外键关联，难以追踪一次登录对应的全部请求活动 |

---

## 六、跨模块依赖关系总图

```
                    ┌──────────────┐
                    │  iam-admin   │ (管理后台)
                    │  REST CRUD   │
                    └──────┬───────┘
                           │ 依赖
                    ┌──────▼───────┐
                    │   iam-sso    │ (SSO 实现层)
                    │  LoginRest   │
                    │  IamLoginSvc │
                    │  Redis实现    │
                    └──────┬───────┘
                           │ 继承/实现
                    ┌──────▼───────┐
                    │   sh-auth    │ (框架层)
                    │  Filter Chain│
                    │  LoginService│
                    │  SPI 契约    │
                    │  Cache       │
                    └──────┬───────┘
                           │ 依赖
                    ┌──────▼───────┐
                    │ iam-common   │ (公共层)
                    │  Entity/DTO  │
                    │  Mapper      │
                    │  PasswordHlp │
                    └──────────────┘
```

**会话流方向**：
- **创建**：iam-sso → sh-auth → Redis
- **验证**：sh-auth Filter → sh-auth SPI → iam-sso Redis 实现 → Redis
- **销毁**：iam-sso / iam-admin → sh-auth SPI → iam-sso Redis 实现 → Redis
- **审计**：iam-admin 只读查询 iam_login_log / iam_request_log 表

---

## 七、文件清单（64 个关键文件）

### sh-auth（31 个）

| 文件路径（相对于 sh-auth/src/main/java/com/wkclz/auth/） |
|--------------------------------------------------------|
| `filter/EagerContentCachingRequestWrapper.java` |
| `filter/FilterOrder.java` |
| `filter/RequestRecordFilter.java` |
| `filter/RequestWrapperFilter.java` |
| `filter/SecurityHeaderFilter.java` |
| `filter/AuthenticationFilter.java` |
| `filter/AuthorizationFilter.java` |
| `contract/auth/LoginService.java` |
| `contract/auth/LogoutService.java` |
| `contract/auth/StandardLoginPipeline.java` |
| `contract/auth/SessionStore.java` |
| `contract/auth/TokenService.java` |
| `contract/auth/ConcurrentSessionControl.java` |
| `contract/auth/PasswordEncoder.java` |
| `contract/auth/PasswordValidator.java` |
| `contract/auth/CaptchaService.java` |
| `contract/auth/AuthenticationProvider.java` |
| `contract/auth/AccountStatusChecker.java` |
| `contract/auth/RateLimitChecker.java` |
| `contract/auth/MfaService.java` |
| `contract/auth/SmsCodeSender.java` |
| `contract/auth/OAuthProvider.java` |
| `contract/auth/impl/DefaultPasswordEncoder.java` |
| `contract/auth/impl/PBKDF2PasswordEncoder.java` |
| `contract/auth/impl/Md5PasswordEncoder.java` |
| `contract/auth/impl/DefaultCaptchaService.java` |
| `contract/auth/impl/DefaultLogoutService.java` |
| `contract/auth/impl/JwtTokenService.java` |
| `contract/authz/AccessControlProvider.java` |
| `contract/infra/RequestLogger.java` |
| `contract/infra/SecurityHeaderProvider.java` |
| `contract/infra/AuthMetadataService.java` |
| `context/SecurityContext.java` |
| `cache/AuthCacheManager.java` |
| `cache/AuthCacheRefreshEvent.java` |
| `cache/AuthCacheRefreshListener.java` |
| `config/AuthConstants.java` |
| `config/AuthProperties.java` |
| `config/ShAuthAutoConfig.java` |
| `config/ShAuthBeanConfiguration.java` |
| `config/ShAutFilterConfiguration.java` |
| `helper/AkSignHelper.java` |
| `bean/Session.java` |
| `bean/AuthToken.java` |
| `bean/Principal.java` |
| `bean/AuthRequest.java` |
| `bean/AuthResult.java` |
| `bean/RequestRecord.java` |
| `bean/LoginResp.java` |
| `bean/SessionCreateReq.java` |
| `enums/AuthErrorType.java` |
| `enums/AuthStatus.java` |
| `enums/TokenType.java` |
| `enums/RefreshScope.java` |
| `exception/AuthenticationException.java` |
| `exception/SessionExpiredException.java` |
| `exception/AuthException.java` |

### iam-sso（17 个）

| 文件路径（相对于 iam-sso/src/main/java/com/wkclz/iam/sso/） |
|---------------------------------------------------------|
| `service/IamLoginService.java` |
| `service/IamRequestService.java` |
| `service/UsernameCacheService.java` |
| `service/SsoResourceService.java` |
| `service/RedisSessionStore.java` |
| `service/RedisConcurrentSessionControl.java` |
| `service/RedisCaptchaService.java` |
| `rest/LoginRest.java` |
| `rest/CaptchaRest.java` |
| `rest/RegisterRest.java` |
| `rest/UserInfoRest.java` |
| `contract/auth/IamLoginPipeline.java` |
| `contract/auth/IamPasswordAuthenticationProvider.java` |
| `contract/auth/NoopRateLimitChecker.java` |
| `contract/auth/NoopMfaService.java` |
| `contract/JwtTokenService.java` |
| `contract/LocalSsoFacadeContract.java` |
| `contract/HttpSsoFacadeContract.java` |
| `config/IamSsoConfig.java` |
| `IamSsoAutoConfig.java` |
| `Route.java` |
| `mapper/SsoLoginMapper.java` |
| `mapper/SsoLoginLogMapper.java` |
| `mapper/SsoRequestLogMapper.java` |
| `mapper/SsoResourceMapper.java` |

### iam-admin（16 个）

| 文件路径（相对于 iam-admin/src/main/java/com/wkclz/iam/admin/） |
|-----------------------------------------------------------|
| `rest/UserRest.java` |
| `rest/UserAuthRest.java` |
| `rest/LoginLogRest.java` |
| `rest/RequestLogRest.java` |
| `rest/UserRoleRest.java` |
| `rest/RoleUserRest.java` |
| `rest/UserMenuRest.java` |
| `service/IamUserService.java` |
| `service/IamUserAuthService.java` |
| `service/IamUserAuthPasswordService.java` |
| `service/IamLoginLogService.java` |
| `service/IamRequestLogService.java` |
| `service/IamUserRoleService.java` |
| `service/IamUserMenuService.java` |
| `job/UserRoleExpireJobHandler.java` |
| `init/RestfulScan.java` |
| `config/IamAdminConfig.java` |
| `IamAdminAutoConfig.java` |
| `Route.java` |
| `helper/EntityFieldAnalyzer.java` |

---

## 八、附录：枚举与常量速查

### 认证错误码（AuthErrorType）

| 错误码 | 枚举值 | 含义 |
|--------|--------|------|
| 32 | USER_NOT_FOUND | 用户不存在 |
| 47 | ACCOUNT_LOCKED | 账号已锁定 |
| 52 | BAD_CREDENTIALS | 凭证错误 |
| 54 | CAPTCHA_ERROR | 验证码错误 |
| 81 | TOKEN_EXPIRED | Token 过期 |
| 82 | TOKEN_INVALID | Token 无效 |
| 120 | SESSION_EXPIRED | 会话已过期 |
| 121 | ACCESS_DENIED | 访问被拒绝 |

### 配置项速查

| 配置路径 | 默认值 | 说明 |
|----------|--------|------|
| `sh.auth.session.ttl` | 86400 (24h) | 会话 TTL（秒） |
| `sh.auth.session.maxConcurrent` | 5 | 最大并发会话数（0=不限制） |
| `sh.auth.password.expireDays` | 180 | 密码过期天数 |
| `sh.auth.whiteList.paths` | `/public/**, /**/public/**, /error` | 认证白名单 |
| `iam.sso.jwt-secret-key` | - | JWT HS256 签名密钥 |
| `iam.sso.maxConcurrentSessions` | 0 (不限制) | IAM 层最大并发数 |
| `iam.job.user-role-expire.enabled` | false | 角色过期任务开关 |
| `iam.job.user-role-expire.cron` | `0 */5 * * * ?` | 角色过期任务 cron |
