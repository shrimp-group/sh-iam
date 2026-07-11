# IAM 契约层实现设计（v2 — 契约层优化后）

- **日期**：2026-07-11
- **主题**：为 sh-iam-contract-api 提供 AuthContract + SsoFacadeContract 的具体实现
- **范围**：登录与会话管理相关功能，不含权限控制（AuthzContract）和 AK 签名（AkSignContract）
- **前置条件**：契约层已完成 6 项优化（见附录 A）

## 1. 背景与目标

sh-iam 已将会话相关功能抽象为契约层（sh-iam-contract-api + sh-iam-contract-default），定义了 4 个契约 SPI：
- `AuthContract`（认证，含 `doAuthenticate` 核心方法 + `checkToken` 模板方法）
- `SsoFacadeContract`（SSO 门面）
- `AuthzContract`（鉴权）
- `AkSignContract`（AK 签名）

本任务为契约层提供 `AuthContract` + `SsoFacadeContract` 的具体实现，替换 iam-sdk / iam-sso 中旧的会话抽象，使 iam 全面接入契约层体系。

### 范围边界

**包含**：
- `JwtAuthContract`（iam-sdk）：JWT + Redis 认证实现（仅实现 `authenticate` + `doAuthenticate`，`checkToken` 由契约层模板方法处理）
- `LocalSsoFacadeContract`（iam-sso）：SSO 服务端本地会话实现
- `HttpSsoFacadeContract`（iam-sdk）：客户端应用远程调用 SSO 服务端骨架
- `IamLoginService` 改造：登录失败用 `LoginResp.fail()` 返回
- `IamSessionService` 重构：会话创建/登出/踢人方法抽离
- `LoggingFilter` 改造：使用 `FilterOrder.LOGGING` + 契约层 `RequestLog`
- `SessionHelper` 调用方迁移到 `PrincipalContext`
- `IamSdkConfig` 精简
- `IamRequestService` 修改：`insertLog()` 参数改为契约层 `RequestLog`
- 删除旧抽象（`IamAuthFilter`/`IamSsoService`/`SessionHelper`/`UserJwt`/`UserSession`/`SsoFacade`/`SsoFacadeImpl`/**`iam-sdk RequestLog`**）
- pom.xml 依赖调整

**不包含**（YAGNI）：
- `AuthzContract` 实现（权限控制）
- `AkSignContract` 实现
- `/sign/login` 服务端接口
- 前端适配
- 配置向后兼容
- 数据库结构变更

## 2. 整体架构

### 2.1 模块依赖关系

```
iam-contract-api (契约定义)
iam-contract-default (DefaultAuthFilter + 默认实现，@ConditionalOnMissingBean 占位)
        ↑ 依赖
iam-sdk (JwtAuthContract + HttpSsoFacadeContract + JwtUtil + LoggingFilter + AkSignHelper)
        ↑ 依赖
iam-sso (LocalSsoFacadeContract + IamLoginService + IamSessionService)
```

### 2.2 替换映射

| 旧抽象 | 新实现 | 模块 | 注册方式 |
|---|---|---|---|
| `IamAuthFilter` | `DefaultAuthFilter`（契约层） | iam-contract-default | 自动注册 |
| `IamSsoService` + `IamSsoServiceImpl` | `JwtAuthContract implements AuthContract` | iam-sdk | `@Component`（`@ConditionalOnMissingBean` 替换 Default） |
| `SsoFacadeImpl`（iam-sso local） | `LocalSsoFacadeContract implements SsoFacadeContract` | iam-sso | `@Component`（无条件，优先） |
| `SsoFacadeImpl`（iam-sdk remote） | `HttpSsoFacadeContract implements SsoFacadeContract` | iam-sdk | `@Component @ConditionalOnMissingBean`（回退） |
| `SessionHelper` | `PrincipalContext` | iam-contract-api | 静态方法 |
| `UserJwt` + `UserSession` | `Principal` + `Session` | iam-contract-api | — |
| `IamSdkConfig.jwtSecretKey` 等 | `ContractSettings.*` | iam-contract-api | `ContractConfig @PostConstruct` |

### 2.3 双 Facade 工作机制

- **iam-sso-starter**（SSO 服务端）：组件扫描到 `LocalSsoFacadeContract`（@Component），`HttpSsoFacadeContract` 的 `@ConditionalOnMissingBean` 检测到已存在 → 回退。SSO 服务端用本地实现直接创建 Redis 会话。
- **客户端应用**（仅依赖 iam-sdk）：无 `LocalSsoFacadeContract`，`HttpSsoFacadeContract` 注册 → 通过 HTTP + AK 签名调用 SSO 服务端。

### 2.4 配置迁移

| 旧配置项（iam.sdk.*） | 新配置项（iam.contract.*） | 持有者 |
|---|---|---|
| `iam.sdk.jwt.secret-key` | `iam.contract.jwt-secret-key` | `ContractSettings.jwtSecretKey` |
| `iam.sdk.server-url` | `iam.contract.server-url` | `ContractSettings.serverUrl` |
| `iam.sdk.app-id` | `iam.contract.app-id` | `ContractSettings.appId` |
| `iam.sdk.app-secret` | `iam.contract.app-secret` | `ContractSettings.appSecret` |
| `iam.sdk.app-code` | `iam.sdk.app-code`（保留） | `IamSdkConfig.appCode` |
| `iam.sdk.static.*` | `iam.sdk.static.*`（保留） | `IamSdkConfig.*` |

**不做向后兼容**：用户需将旧配置项迁移到新配置项。原因：保留兼容会让两套配置并存，难以维护。

## 3. 组件设计

### 3.1 JwtAuthContract（iam-sdk）— 契约层优化后简化

**位置**：`iam-sdk/src/main/java/com/wkclz/iam/sdk/contract/JwtAuthContract.java`

**职责**：替换 `IamSsoServiceImpl` + `IamAuthFilter` 中的认证逻辑。由 `DefaultAuthFilter` 调用。

**契约层优化收益**：
- 只需实现 `authenticate(HttpServletRequest)` + `doAuthenticate(String token)` 两个方法
- `checkToken` 由契约层 default 模板方法处理（内置 token 空值/session 存在性/authIdentifier 一致性校验）
- 异常映射用 `AuthErrorType.fromJwtErrorCode(errorCode)` 一行搞定

**authenticate 方法**（委托 `doAuthenticate`，避免逻辑重复）：

```java
@Override
public AuthResult authenticate(HttpServletRequest request) {
    String token = PrincipalContext.getToken();
    if (!StringUtils.hasText(token)) {
        return null;  // 无 token → 返回 null，过滤器放行 public
    }
    return doAuthenticate(token);
}
```

**doAuthenticate 方法**（核心认证逻辑，authenticate 和 checkToken 共用）：

```java
@Override
public AuthResult doAuthenticate(String token) {
    // 1. JWT 解析（签名 + 过期校验，异常一行映射）
    UserJwt userJwt;
    try {
        userJwt = JwtUtil.parseToken(token, ContractSettings.getJwtSecretKey());
    } catch (JwtValidationException e) {
        throw new AuthException(
            AuthErrorType.fromJwtErrorCode(e.getErrorCode()),  // 一行映射
            e.getMessage(), e);
    }

    String username = userJwt.getUsername();

    // 2. Redis 会话查询（Redis 异常抛 RuntimeException，非 AuthException）
    String sessionKey = JwtUtil.getTokenRedisKey(token, username);
    String sessionJson;
    try {
        sessionJson = redisTemplate.opsForValue().get(sessionKey);
    } catch (Exception e) {
        log.error("认证 Redis 查询失败, username={}", username, e);
        throw new RuntimeException("会话存储不可用", e);
    }
    if (sessionJson == null) {
        throw new AuthException(AuthErrorType.SESSION_EXPIRED, "会话已过期");
    }

    // 3. Session 反序列化
    Session session;
    try {
        session = JSON.parseObject(sessionJson, Session.class);
    } catch (Exception e) {
        log.error("Session 反序列化失败, username={}", username, e);
        throw new AuthException(AuthErrorType.TOKEN_INVALID, "会话数据损坏", e);
    }

    // 4. 构建 Principal + AuthResult
    Principal principal = new Principal();
    principal.setUserCode(userJwt.getUserCode());
    principal.setUsername(username);
    principal.setNickname(userJwt.getNickname());
    principal.setAvatar(userJwt.getAvatar());
    principal.setAuthIdentifier(session.getAuthIdentifier());

    AuthResult result = new AuthResult();
    result.setPrincipal(principal);
    result.setSession(session);
    return result;
}
```

**checkToken 无需实现** — 契约层 `AuthContract.checkToken()` 是 default 模板方法，内置：
1. token 空值 → `TOKEN_MISSING`
2. 调用 `doAuthenticate(token)` → JWT + Redis 校验
3. Session 为空 → `SESSION_EXPIRED`
4. authIdentifier 不一致 → `TOKEN_INVALID`

实现方只需抛出对应 `AuthException`，模板方法自动处理。

**关键决策**：
- `doAuthenticate` 是 `authenticate` 和 `checkToken` 的公共核心，避免逻辑重复
- JWT 异常映射用 `AuthErrorType.fromJwtErrorCode()` 一行完成（`JwtValidationException.CODE_*` 值与契约层 `JwtErrorCodes` 一致）
- `@Component` 注册，通过 `@ConditionalOnMissingBean` 替换 `DefaultAuthContract`
- Redis Session 存储格式不变，旧会话仍可被 `Session.class` 反序列化（多余字段忽略）

**契约层与 SDK 常量映射**：

| JwtValidationException.CODE_* | JwtErrorCodes 常量 | AuthErrorType |
|---|---|---|
| `JWT_EXPIRED` | `EXPIRED` | `TOKEN_EXPIRED` |
| `JWT_SIGNATURE_ERROR` | `SIGNATURE` | `TOKEN_INVALID` |
| `JWT_MALFORMED` | `MALFORMED` | `TOKEN_INVALID` |
| `JWT_UNSUPPORTED` | `UNSUPPORTED` | `TOKEN_INVALID` |
| `JWT_ILLEGAL_ARGUMENT` | `ILLEGAL_ARGUMENT` | `TOKEN_INVALID` |

### 3.2 LocalSsoFacadeContract（iam-sso）

**位置**：`iam-sso/src/main/java/com/wkclz/iam/sso/contract/LocalSsoFacadeContract.java`

**职责**：替换 iam-sso 的 `SsoFacadeImpl`，作为 SSO 服务端的本地会话实现。`@Component` 无条件注册。

**login 方法**（业务失败用 `LoginResp.fail()` 返回，系统级错误抛异常）：

```java
@Override
public LoginResp login(SessionCreateReq req) {
    log.info("SsoFacade 本地创建会话, authIdentifier: {}", req.getAuthIdentifier());

    // 1. 构建 Principal（JWT claims 来源）
    Principal principal = new Principal();
    principal.setUserCode(req.getUserCode());
    principal.setUsername(req.getUsername());
    principal.setNickname(req.getNickname());
    principal.setAvatar(req.getAvatar());

    // 2. 构建 Session（Redis 存储来源）
    Session session = new Session();
    session.setUserCode(req.getUserCode());
    session.setAuthType(req.getAuthType());
    session.setAuthIdentifier(req.getAuthIdentifier());

    // 3. 生成 JWT + 缓存 Session（复用 IamSessionService）
    UserJwt userJwt = new UserJwt();
    userJwt.setUserCode(req.getUserCode());
    userJwt.setUsername(req.getUsername());
    userJwt.setNickname(req.getNickname());
    userJwt.setAvatar(req.getAvatar());
    String token = JwtUtil.generateToken(userJwt, ContractSettings.getJwtSecretKey());

    iamSessionService.createSession(token, principal, session);

    // 4. 并发会话数控制
    iamSessionService.enforceMaxConcurrentSessions(req.getUsername());

    // 5. 记录登录日志（成功）
    recordLoginLog(req, LoginStatus.SUCCESS);

    // 6. 返回成功响应
    return LoginResp.success(token, req.getUserCode(), req.getUsername(),
            req.getNickname(), req.getAvatar());
}
```

**saveLog / logout 方法**：

```java
@Override
public void saveLog(RequestLog log) {
    requestLogService.insertLog(log);  // 委托 IamRequestService（保持现状）
}

@Override
public void logout(String token) {
    iamSessionService.logout(token);
}
```

**关键决策**：
- `login()` 永不抛业务失败异常：仅负责"创建会话"动作，此时用户已通过 `IamLoginService` 的密码校验。业务登录失败在 `IamLoginService` 中已处理。
- `IamSessionService` 重构：抽出 `createSession` 和 `enforceMaxConcurrentSessions` 方法，封装 Redis SET + ZSET 注册 + 踢人逻辑。
- `SessionCreateReq` 已删除 `clientIp`/`userAgent`（契约层优化 4）：`recordLoginLog` 内部从 `RequestHelper.getRequest()` 获取 IP/UA，与旧 `SsoFacadeImpl` 行为一致。

### 3.3 IamLoginService 改造

**核心变化**：登录失败从"返回 `failResp(LoginStatus)`"改为"返回 `LoginResp.fail(LoginFailType, failReason)`"。

**LoginStatus → LoginFailType 映射**：

| 旧 LoginStatus (code) | 新 LoginFailType | failReason（动态） |
|---|---|---|
| `USER_NOT_FOUND` (32) | `USERNAME_OR_PASSWORD_ERROR` | null（合并，防用户枚举） |
| `INVALID_CREDENTIALS` (52) | `USERNAME_OR_PASSWORD_ERROR` | null（合并） |
| `INVALID_PASSWORD` (31) | `USERNAME_OR_PASSWORD_ERROR` | null（合并） |
| `ACCOUNT_LOCKED` (47) | `ACCOUNT_LOCKED` | null |
| `ACCOUNT_DISABLED` (53) | `ACCOUNT_DISABLED` | null |
| `EXPIRED_PASSWORD` (48) | `CREDENTIALS_EXPIRED` | null |
| `EXPIRED_ACCOUNT` (49) | `ACCOUNT_DISABLED` | null（账号过期归入禁用） |
| `NEED_CAPTCHA` (60) | `CAPTCHA_REQUIRED` | null |
| `CAPTCHA_TIMEOUT` (61) | `CAPTCHA_ERROR` | "验证码已过期" |
| `INVALID_CAPTCHA` (54) | `CAPTCHA_ERROR` | null |

**loginByUsernameAndPassword 关键片段**：

```java
public LoginResp loginByUsernameAndPassword(HttpServletRequest request, LoginReq loginReq) {
    // ... 密码解密、验证码逻辑保持不变 ...

    IamUserAuthDto auth = ssoLoginMapper.getUserAuth4PasswordByUsername(username);

    // 验证码阶段失败
    if (需要验证码 && (captchaCode/captchaId 空)) {
        loginLog(loginReq, auth, LoginStatus.NEED_CAPTCHA, AuthType.PASSWORD);
        return LoginResp.fail(LoginFailType.CAPTCHA_REQUIRED);
    }
    // ... 其他验证码失败 ...

    // 1. 用户不存在 → 合并为"用户名或密码错误"（防枚举）
    if (auth == null) {
        loginLog(loginReq, auth, LoginStatus.USER_NOT_FOUND, AuthType.PASSWORD);
        return LoginResp.fail(LoginFailType.USERNAME_OR_PASSWORD_ERROR);
    }

    // 2-4. 登录方式禁用 / 锁定 / 禁用
    // 5. 密码错误 → 合并为"用户名或密码错误"
    // 6. 密码过期

    // 7. 登录成功 → 调用 SsoFacadeContract.login()
    SessionCreateReq sessionReq = new SessionCreateReq();
    // ... 填充字段 ...
    LoginResp response = ssoFacadeContract.login(sessionReq);

    // 更新用户登录信息（IP）
    // ...

    return response;
}
```

**关键决策**：
- `loginLog()` 内部仍用 `LoginStatus`：日志表结构不变，登录日志仍记录 LDAP 风格 code。`LoginStatus` 枚举保留，仅用于日志表写入。
- `LoginResp` 返回契约层类型：`LoginRest` 返回 `R<com.wkclz.iam.contract.bean.resp.LoginResp>`，删除旧 `com.wkclz.iam.sdk.bean.resp.LoginResp`。
- `SsoFacadeContract` 注入：`IamLoginService` 注入 `SsoFacadeContract`（而非旧 `SsoFacade`），运行时解析为 `LocalSsoFacadeContract`。

**logout 改造**：

```java
public void logout(HttpServletRequest request) {
    String token = PrincipalContext.getToken();  // 直接从 Header 读，不依赖过滤器缓存
    if (!StringUtils.hasText(token)) return;
    ssoFacadeContract.logout(token);
}
```

**changePassword 改造**：

```java
String userCode = PrincipalContext.getUserCode();
String username = PrincipalContext.getUsername();
```

### 3.4 HttpSsoFacadeContract（iam-sdk）

**位置**：`iam-sdk/src/main/java/com/wkclz/iam/sdk/contract/HttpSsoFacadeContract.java`

**职责**：替换 iam-sdk 的 `SsoFacadeImpl`（远程 HTTP 调用 SSO 服务端）。`@Component @ConditionalOnMissingBean` 注册。

**login 方法**：

```java
@Override
public LoginResp login(SessionCreateReq req) {
    String serverUrl = ContractSettings.getServerUrl();
    if (!StringUtils.hasText(serverUrl)) {
        throw SystemException.of("iam.contract.server-url 未配置，无法远程登录");
    }
    log.info("远程创建会话，authIdentifier: {}", req.getAuthIdentifier());

    // 构造 AK 签名头（复用 AkSignHelper，AkSignContract 不在本次范围）
    String sign = AkSignHelper.sign(
            ContractSettings.getAppId(),
            ContractSettings.getAppSecret());

    // 请求 SSO 服务端登录接口（/sign/login 路径保持现状）
    // ... HTTP POST + sign/app-id header，解析 R<LoginResp> ...
    return responseBody.getData();
}
```

**saveLog / logout 方法**：HTTP 调用，失败不阻断业务（try-catch + log.error）。

**关键决策**：
- 调用路径继续指向 `/sign/login`（保持与旧 SsoFacadeImpl 一致）。
- SSO 服务端的 `/sign/login` 接口由其他模块或后续任务实现，本次不新增。
- `AkSignHelper` 保持原样（不迁移到 `AkSignContract`），`HttpSsoFacadeContract` 继续使用它做 RPC 签名。
- 配置从 `ContractSettings` 读取（不再从 `IamSdkConfig` 读）。

### 3.5 IamSessionService 重构

**位置**：`iam-sso/src/main/java/com/wkclz/iam/sso/service/IamSessionService.java`

**重构后方法清单**：

| 方法 | 职责 | 调用方 |
|---|---|---|
| `createSession(String token, Principal principal, Session session)` | Redis SET 会话 + ZSET 注册 | `LocalSsoFacadeContract.login()` |
| `enforceMaxConcurrentSessions(String username)` | 并发会话数控制（踢人） | `LocalSsoFacadeContract.login()` |
| `logout(String token)` | 删除 Redis 会话 + ZSET 移除 | `LocalSsoFacadeContract.logout()` |
| `invalidateAllSessions(String username)` | 批量删除用户所有会话 | `IamLoginService.changePassword()` |

**createSession 方法**：

```java
public void createSession(String token, Principal principal, Session session) {
    String username = principal.getUsername();
    String tokenRedisKey = JwtUtil.getTokenRedisKey(token, username);
    redisTemplate.opsForValue().set(tokenRedisKey, JSON.toJSONString(session),
            JwtUtil.SESSION_TTL_SECONDS, TimeUnit.SECONDS);

    String sessionListKey = JwtUtil.getSessionListRedisKey(username);
    String tokenMd5 = Md5Tool.md5(token);
    redisTemplate.opsForZSet().add(sessionListKey, tokenMd5, System.currentTimeMillis());
    log.info("用户 {} 会话已创建, tokenMd5={}", username, tokenMd5);
}
```

**enforceMaxConcurrentSessions 方法**：

```java
public void enforceMaxConcurrentSessions(String username) {
    Integer max = iamSsoConfig.getMaxConcurrentSessions();
    if (max == null || max <= 0) return;

    String sessionListKey = JwtUtil.getSessionListRedisKey(username);
    Long count = redisTemplate.opsForZSet().size(sessionListKey);
    if (count == null || count <= max) return;

    Set<String> earliest = redisTemplate.opsForZSet().range(sessionListKey, 0, count - max - 1);
    if (earliest == null) return;

    for (String tokenMd5 : earliest) {
        String sessionKey = "iam:session:" + username + ":" + tokenMd5;
        redisTemplate.delete(sessionKey);
        redisTemplate.opsForZSet().remove(sessionListKey, tokenMd5);
        log.info("用户 {} 并发会话超限，踢出最早会话, tokenMd5={}", username, tokenMd5);
    }
}
```

**关键决策**：
- `IamSdkConfig` 依赖移除：旧实现注入 `IamSdkConfig` 读 `jwtSecretKey`，改为 `ContractSettings.getJwtSecretKey()`。
- `createSession` 参数用契约层类型：`Principal` + `Session`（而非 `UserJwt` + `UserSession`）。
- Redis Key 格式不变：`iam:session:{username}:{tokenMd5}` + `iam:session:list:{username}`，避免现有会话失效。

### 3.6 SessionHelper 调用方迁移

**SessionHelper 方法迁移**：

| SessionHelper 方法 | PrincipalContext 对应方法 |
|---|---|
| `getToken(request)` | `getToken()` |
| `getAppCode(request)` | `getAppCode()` |
| `getUserCode()` | `getUserCode()` |
| `getUserJwt()` / `getUserJwt(request)` | `getPrincipal()` / `getPrincipal(request)` |
| `getUserSession()` / `getUserSession(request)` | `getSession()` / `getSession(request)` |
| `cacheUserInfo(...)` | `cache(...)`（由 DefaultAuthFilter 调用） |
| `getTenantCode()` | `getTenantCode()`（旧实现返回硬编码 "default"，新实现从请求头读） |
| `match(rule, uri)` | `PrincipalContext.match(pattern, uri)` |

**调用方迁移清单**：

| 文件 | 旧代码 | 新代码 |
|---|---|---|
| `UserInfoRest.java` | `SessionHelper.getUserSession(request)` → `R<UserSession>` | `PrincipalContext.getPrincipal(request)` → `R<Principal>`（见 3.7 节） |
| `UserInfoRest.java` | `SessionHelper.getAppCode(request)` | `PrincipalContext.getAppCode()` |
| `UserMenuRest.java` | `SessionHelper.getUserCode()` | `PrincipalContext.getUserCode()` |
| `IamLoginService.java` | `SessionHelper.getUserCode()` + `SessionHelper.getUserJwt().getUsername()` | `PrincipalContext.getUserCode()` + `PrincipalContext.getUsername()` |
| `IamLoginService.java` | `SessionHelper.getToken(request)`（logout） | `PrincipalContext.getToken()` |

### 3.7 UserInfoRest 返回类型调整

**问题**：旧 `userInfo()` 返回 `R<UserSession>`，契约层 `Session` 仅含 `userCode` + `authType` + `authIdentifier`，丢失了 `username` + `nickname`（前端可能依赖）。

**决策**：返回 `Principal`（含 `userCode` + `username` + `nickname` + `avatar`），而非 `Session`。

```java
@GetMapping(Route.USER_INFO)
@Operation(summary = "获取用户信息")
public R<Principal> userInfo(HttpServletRequest request) {
    Principal principal = PrincipalContext.getPrincipal(request);
    if (principal == null) {
        return R.error("用户未登录");
    }
    return R.ok(principal);
}
```

**前端影响**：`/iam-sso/user/info` 响应结构从 `{userCode, username, nickname, authType, authIdentifier}` 变为 `{userCode, username, nickname, avatar, authIdentifier}`。需前端确认兼容（`authType` 移除，`avatar` 新增）。

### 3.8 LoggingFilter 改造

**位置**：`iam-sdk/src/main/java/com/wkclz/iam/sdk/filter/LoggingFilter.java`

**改动点**：

1. **order 改用 `FilterOrder.LOGGING`**：

```java
// 旧：@Order(Integer.MIN_VALUE + 1)
// 新：
@Order(FilterOrder.LOGGING)
```

2. **`SsoFacade` → `SsoFacadeContract`**：

```java
@Autowired(required = false)
private SsoFacadeContract ssoFacadeContract;
```

3. **`SessionHelper` → `PrincipalContext`**（finally 块）：

```java
// finally 块中，DefaultAuthFilter 的 PrincipalContext.clear() 已执行
// 用 getPrincipal(request) 从 request attribute 读取（不依赖 ThreadLocal）
Principal principal = PrincipalContext.getPrincipal(request);
if (principal != null) {
    log.setUserCode(principal.getUserCode());
    log.setUsername(principal.getUsername());
    log.setNickname(principal.getNickname());
}
```

4. **`fetchRequestLog` 中 SessionHelper 调用迁移**：

```java
log.setToken(PrincipalContext.getToken());
log.setTenantCode(PrincipalContext.getTenantCode());
log.setAppCode(PrincipalContext.getAppCode());
```

5. **直接用契约层 `RequestLog`**（无需转换，契约层优化后字段完全一致）：

```java
// 删除 import com.wkclz.iam.sdk.bean.RequestLog;
// 统一使用 com.wkclz.iam.contract.bean.RequestLog
// saveResponseLog 无需 convertToContractRequestLog()
```

6. **`IamSdkConfig` 依赖保留**：LoggingFilter 仍需 `IamSdkConfig`（`staticEnabled` / `staticSubfix` 用于静态资源过滤）。

**过滤器 finally 顺序问题**：

```
请求 → LoggingFilter.doFilterInternal()
         → chain.doFilter() → DefaultAuthFilter.doFilterInternal()
                                → chain.doFilter() → Controller
                                → finally: PrincipalContext.clear()  ← 先执行
         → finally: 读取 PrincipalContext  ← 后执行，但 ThreadLocal 已 clear
```

**解决方案**：`PrincipalContext` 已提供 `getPrincipal(request)` / `getSession(request)` 方法，从 request attribute 直接读取（不经 ThreadLocal）。即使 `DefaultAuthFilter` 的 `PrincipalContext.clear()` 已执行 ThreadLocal 清理，request attribute 仍可读。

### 3.9 IamSdkConfig 改造 + IamRequestService 修改

**IamSdkConfig 改造后**：

```java
@Data
@Configuration
public class IamSdkConfig {

    @Value("${iam.sdk.app-code:}")
    private String appCode;

    @Value("${iam.sdk.static.enabled:false}")
    private String staticEnabled;

    @Value("${iam.sdk.static.subfix:js|css|jpg|png|mp3|html|htm|jpeg|ttf|woff|ico|woff2|map}")
    private String staticSubfix;

    // 删除：enabled / jwtSecretKey / serverUrl / appId / appSecret
    // 删除：getCasFacade() Bean 方法（DefaultSsoFacadeContract 替代默认）
}
```

`AkSignHelper` 和 `HttpSsoFacadeContract` 改为从 `ContractSettings` 读取 `appId` / `appSecret` / `serverUrl` / `jwtSecretKey`（不再从 `IamSdkConfig` 读）。

**IamRequestService 修改**：`insertLog()` 参数从 iam-sdk `RequestLog` 改为契约层 `com.wkclz.iam.contract.bean.RequestLog`（契约层优化后两者字段完全一致）。

```java
// 旧：import com.wkclz.iam.sdk.bean.RequestLog;
// 新：import com.wkclz.iam.contract.bean.RequestLog;
public void insertLog(com.wkclz.iam.contract.bean.RequestLog requestLog) {
    // 内部实现不变（字段名一致）
}
```

**AuthErrorType 引用路径变更**（契约层优化 2/6）：
```java
// 旧：import com.wkclz.iam.contract.exception.AuthException.AuthErrorType;
// 新：import com.wkclz.iam.contract.enums.AuthErrorType;
```

## 4. 删除清单

### 4.1 iam-sdk 删除

| 文件 | 原因 |
|---|---|
| `filter/IamAuthFilter.java` | `DefaultAuthFilter` 替代 |
| `service/IamSsoService.java` | `AuthContract` 替代 |
| `helper/SessionHelper.java` | `PrincipalContext` 替代 |
| `bean/UserJwt.java` | 内部模型，仅 `JwtUtil` 保留使用（不对外暴露） |
| `bean/UserSession.java` | `Session` 替代 |
| `facade/SsoFacade.java` | `SsoFacadeContract` 替代 |
| `facade/impl/SsoFacadeImpl.java` | `HttpSsoFacadeContract` 替代 |
| `bean/req/SessionCreateReq.java` | 契约层 `SessionCreateReq` 替代 |
| `bean/resp/LoginResp.java` | 契约层 `LoginResp` 替代 |
| **`bean/RequestLog.java`** | **契约层 `RequestLog` 替代（字段完全一致，契约层优化后）** |

### 4.2 iam-sdk 保留

| 文件 | 原因 |
|---|---|
| `util/JwtUtil.java` | 内部工具，`UserJwt` 内部模型保留 |
| `helper/AkSignHelper.java` | `HttpSsoFacadeContract` 依赖，`AkSignContract` 不在本次范围 |
| `filter/LoggingFilter.java` | 改造后保留 |
| `IamSdkAutoConfig.java` | 保留 |
| `helper/CaptchaHelper.java` | 验证码功能保留 |
| `exception/JwtValidationException.java` | `JwtUtil` 内部使用 |

### 4.3 iam-sso 删除

| 文件 | 原因 |
|---|---|
| `service/SsoFacadeImpl.java` | `LocalSsoFacadeContract` 替代 |
| `service/IamSsoServiceImpl.java` | `JwtAuthContract` 替代 |

### 4.4 iam-common 不动

实体/DTO/Mapper 不受影响。

## 5. 依赖调整

### 5.1 iam-sdk/pom.xml

新增依赖：

```xml
<dependency>
    <groupId>com.wkclz.framework</groupId>
    <artifactId>iam-contract-api</artifactId>
    <version>${revision}</version>
</dependency>
<dependency>
    <groupId>com.wkclz.framework</groupId>
    <artifactId>iam-contract-default</artifactId>
    <version>${revision}</version>
</dependency>
```

### 5.2 iam-sso/pom.xml

无需额外依赖（iam-sso 依赖 iam-sdk，传递依赖到契约层）。

### 5.3 自动配置

- `IamContractAutoConfig`（契约层）注册 `DefaultAuthFilter` + 4 个默认契约 Bean
- `IamSdkAutoConfig` 扫描 `com.wkclz.iam.sdk`，注册 `JwtAuthContract` + `HttpSsoFacadeContract`
- `IamSsoAutoConfig` 扫描 `com.wkclz.iam.sso`，注册 `LocalSsoFacadeContract`
- `JwtAuthContract`（@Component）替换 `DefaultAuthContract`（@ConditionalOnMissingBean）
- `LocalSsoFacadeContract`（@Component）替换 `DefaultSsoFacadeContract`（@ConditionalOnMissingBean）
- `AkSignContract` 和 `AuthzContract` 保持默认实现

### 5.4 启动顺序保证

`ContractConfig.@PostConstruct` 必须在 `JwtAuthContract` / `LocalSsoFacadeContract` 使用 `ContractSettings` 前执行。

Spring 启动顺序：`@Configuration` 类的 `@PostConstruct` 在 Bean 初始化阶段执行，早于 `@Component` Bean 的首次方法调用（运行时）。只要 `ContractConfig` 被 `IamContractAutoConfig` 注册，`@PostConstruct` 会在任何契约实现 Bean 被调用前执行。无问题。

## 6. 测试策略

由于当前环境无 Maven CLI，编译验证需在 IDE 执行。测试聚焦单元测试（不依赖 Spring 容器）。

### 6.1 JwtAuthContract 单元测试

| 测试用例 | 输入 | 预期 |
|---|---|---|
| `authenticate_无token_返回null` | 请求无 Authorization header | `null` |
| `authenticate_有效token_返回AuthResult` | 有效 JWT + Redis 有会话 | `AuthResult`（含 Principal + Session） |
| `authenticate_过期token_抛TOKEN_EXPIRED` | 过期 JWT | `AuthException(TOKEN_EXPIRED)` |
| `authenticate_签名错误_抛TOKEN_INVALID` | 签名错误的 JWT | `AuthException(TOKEN_INVALID)` |
| `authenticate_会话不存在_抛SESSION_EXPIRED` | 有效 JWT 但 Redis 无记录 | `AuthException(SESSION_EXPIRED)` |
| `checkToken_token为空_抛TOKEN_MISSING` | `null` / 空白 token | `AuthException(TOKEN_MISSING)` |
| `checkToken_格式错误_抛TOKEN_INVALID` | 非 JWT 结构 | `AuthException(TOKEN_INVALID)` |
| `checkToken_Redis异常_抛RuntimeException` | Redis 抛异常 | `RuntimeException`（非 AuthException） |
| `checkToken_Session反序列化失败_抛TOKEN_INVALID` | JSON 损坏 | `AuthException(TOKEN_INVALID)` |
| `checkToken_authIdentifier不匹配_抛TOKEN_INVALID` | 传入的 authIdentifier 与 username/session 均不一致 | `AuthException(TOKEN_INVALID)` |
| `checkToken_authIdentifier匹配username_通过` | authIdentifier == username | 返回 Session |
| `checkToken_authIdentifier匹配session_通过` | authIdentifier == session.authIdentifier | 返回 Session |
| `checkToken_authIdentifier为空_跳过校验` | authIdentifier == null | 返回 Session |

测试方式：Mock `RedisTemplate` + `JwtUtil.parseToken`（真实 JWT 生成）。不启动 Spring 容器。

### 6.2 LocalSsoFacadeContract 单元测试

| 测试用例 | 验证点 |
|---|---|
| `login_成功_返回LoginResp_success` | `LoginResp.success=true`，含 token + 用户字段 |
| `login_会话创建_Redis被调用` | `redisTemplate.opsForValue().set` 被调用 |
| `login_并发超限_踢人` | `enforceMaxConcurrentSessions` 被调用 |
| `logout_有效token_Redis被删除` | `redisTemplate.delete` 被调用 |

测试方式：Mock `IamSessionService` + `RedisTemplate`。

### 6.3 IamLoginService 单元测试

| 测试用例 | 验证点 |
|---|---|
| `login_用户不存在_返回USERNAME_OR_PASSWORD_ERROR` | `LoginResp.failType == USERNAME_OR_PASSWORD_ERROR` |
| `login_密码错误_返回USERNAME_OR_PASSWORD_ERROR` | 同上（合并验证） |
| `login_账号锁定_返回ACCOUNT_LOCKED` | `LoginResp.failType == ACCOUNT_LOCKED` |
| `login_需要验证码_返回CAPTCHA_REQUIRED` | `LoginResp.failType == CAPTCHA_REQUIRED` |
| `login_成功_调用SsoFacadeContract` | `ssoFacadeContract.login()` 被调用 |

测试方式：Mock `SsoLoginMapper` + `SsoFacadeContract`。

### 6.4 集成验证（手动）

在 IDE 启动 iam-sso-starter 后手动验证：

| 验证项 | 方法 |
|---|---|
| SSO 服务端启动 | `iam-sso-starter` 主类，观察日志无 Bean 冲突 |
| 登录接口 | POST `/iam-sso/public/sso/login`，验证返回 `LoginResp`（success=true + token） |
| 登录失败 | 错误密码，验证返回 `LoginResp.failType == USERNAME_OR_PASSWORD_ERROR` |
| 鉴权过滤器 | 不带 token 访问 `/iam-sso/user/info`，验证 401 |
| 用户信息接口 | 带有效 token 访问 `/iam-sso/user/info`，验证返回 Principal |
| 登出 | POST `/iam-sso/public/sso/logout`，验证 Redis 会话删除 |

## 7. 风险评估

| 风险 | 影响 | 缓解 |
|---|---|---|
| **UserInfoRest 返回类型变更** | 前端从 `UserSession` 变为 `Principal`，`authType` 字段消失 | 文档标注破坏性变更；前端从 `/user/menu` 或后续 `/user/session` 接口获取 authType（若需要） |
| **配置项迁移** | 用户需将 `iam.sdk.jwt.secret-key` 改为 `iam.contract.jwt-secret-key` | 设计文档明确列出配置迁移表；不做向后兼容 |
| **`UserJwt` 保留为内部模型** | iam-sdk 内部仍用 `UserJwt`，但对外暴露契约层 `Principal` | `UserJwt` 不删除，仅限 iam-sdk/iam-sso 内部使用；包结构隔离 |
| **双 Facade 优先级** | `LocalSsoFacadeContract`（@Component）vs `HttpSsoFacadeContract`（@ConditionalOnMissingBean） | `@ConditionalOnMissingBean` 检测到 LocalSsoFacadeContract 后不注册 HttpSsoFacadeContract，无冲突 |
| **LoggingFilter 读取 request attribute** | 依赖 `DefaultAuthFilter` 已缓存 Principal 到 request attribute | `PrincipalContext.getPrincipal(request)` 从 request attribute 读，不依赖 ThreadLocal，即使 ThreadLocal 被 clear 仍可读 |
| **Redis Session 格式兼容** | 旧 `UserSession` JSON 序列化与 `Session` 字段不完全一致 | `Session` 字段（userCode/authType/authIdentifier）是 `UserSession` 字段子集，旧会话仍可被 `Session.class` 反序列化（多余字段忽略） |

## 8. 实现顺序建议

1. **pom.xml 依赖调整**：iam-sdk 新增契约层依赖
2. **IamSdkConfig 改造**：精简字段，删除与契约层重复的配置
3. **JwtAuthContract 实现**（iam-sdk）：AuthContract 实现
4. **IamSessionService 重构**（iam-sso）：抽离 createSession / enforceMaxConcurrentSessions / logout 方法
5. **LocalSsoFacadeContract 实现**（iam-sso）：SsoFacadeContract 本地实现
6. **IamLoginService 改造**：LoginFailType 映射，注入 SsoFacadeContract
7. **HttpSsoFacadeContract 实现**（iam-sdk）：远程骨架
8. **LoggingFilter 改造**：SsoFacadeContract + PrincipalContext
9. **SessionHelper 调用方迁移**：UserInfoRest / UserMenuRest / IamLoginService
10. **删除旧抽象**：IamAuthFilter / IamSsoService / SessionHelper / UserJwt / UserSession / SsoFacade / SsoFacadeImpl
11. **单元测试**：JwtAuthContract / LocalSsoFacadeContract / IamLoginService
12. **集成验证**：IDE 启动 + 手动测试

## 附录 A：契约层 6 项优化

本设计 v2 基于契约层已完成以下 6 项优化：

| # | 优化项 | 对本设计的影响 |
|---|---|---|
| 1 | **统一 RequestLog 模型** | iam-sdk `RequestLog` 可删除，直接使用契约层 `RequestLog`（30 字段完全一致），消除 LoggingFilter 和 LocalSsoFacadeContract 中的所有字段转换代码 |
| 2 | **AuthErrorType 增加 HTTP 状态码** | `DefaultAuthFilter` 已改用 `e.getErrorType().getHttpStatus()`（401/403），iam 侧无需改造 |
| 3 | **AuthContract 提供 `doAuthenticate` + `checkToken` 模板方法** | JwtAuthContract 只需实现 `authenticate` + `doAuthenticate`，`checkToken` 由契约层 default 模板处理（token 空值/session 存在性/authIdentifier 一致性校验） |
| 4 | **SessionCreateReq 删除 `clientIp`/`userAgent`** | `LocalSsoFacadeContract.recordLoginLog()` 从 `RequestHelper.getRequest()` 获取 IP/UA，`IamLoginService` 不再设置这两个字段 |
| 5 | **定义 `FilterOrder` 常量类** | LoggingFilter 用 `FilterOrder.LOGGING`，跨模块顺序统一管理 |
| 6 | **`JwtErrorCodes` + `AuthErrorType.fromJwtErrorCode()`** | JWT 异常映射从 20 行 switch 缩减为 1 行，常量值一致确保映射正确 |

