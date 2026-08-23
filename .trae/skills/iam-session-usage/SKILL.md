---
name: "iam-session-usage"
description: "iam-session 模块使用指南——接入项目如何添加依赖、配置、调用登录/获取用户信息等会话管理方法。涉及 SessionManager/TokenService/SessionAuthFilter/IdentityContext 调用、登出/改密销毁会话、客户端远程接入 SSO 服务端时调用。当用户需要在项目中接入 iam-session、调用会话管理 API、读取当前登录用户、扩展 SPI 时调用。"
---

# iam-session 模块使用指南

iam-session 是 sh-iam 的会话管理层，提供认证方式无关的 Session CRUD、JWT Token 生成与校验、并发会话控制、滑窗续期、请求鉴权过滤等能力。所有接入 sh-iam 体系的应用（包括 SSO 服务端、Admin 服务端、第三方业务系统）都通过此模块管理用户会话。

## 模块定位

| 维度       | 说明                                                                                              |
|----------|-------------------------------------------------------------------------------------------------|
| Maven 坐标 | `com.wkclz.iam:iam-session:${revision}`（当前 5.0.0-SNAPSHOT，父 POM `sh-iam`）                      |
| 核心依赖     | `sh-core`（UserIdentity/IdentityContext）、`sh-redis`（StringRedisTemplate）、`sh-web`（IpHelper）、`jjwt`（HS256） |
| 部署形态     | 服务端模式 / 客户端模式（条件装配自动切换）                                                                         |
| 入口配置类    | `IamSessionAutoConfig`（`@AutoConfiguration` + `@ComponentScan("com.wkclz.iam.session")`）          |
| 关键服务     | `SessionManager` / `TokenService` / `SessionStore` / `SessionAuthFilter` / `RequestRecordFilter` |

## 接入步骤

### 1. 添加依赖

接入项目（业务系统）在 `pom.xml` 中引入：

```xml
<dependency>
    <groupId>com.wkclz.iam</groupId>
    <artifactId>iam-session</artifactId>
    <version>${revision}</version>
</dependency>
```

> 服务端部署（SSO 服务端、Admin 服务端）请同时引入 `iam-sso` 或 `iam-admin`；客户端部署只需 `iam-session`。

iam-session 自身的依赖（无需重复声明，由 BOM 管理）：

```xml
<dependency>
    <groupId>com.wkclz.framework</groupId>
    <artifactId>sh-core</artifactId>
</dependency>
<dependency>
    <groupId>com.wkclz.framework</groupId>
    <artifactId>sh-redis</artifactId>
</dependency>
<dependency>
    <groupId>com.wkclz.framework</groupId>
    <artifactId>sh-web</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
</dependency>
```

### 2. 配置 application.yml

最小可用配置：

```yaml
iam:
  session:
    token:
      # JWT 签名密钥（HS256，必须 ≥ 32 字符，生产环境务必替换）
      secret-key: "your-secret-key-must-be-at-least-32-chars-long"
      # Token TTL（秒），默认 48h
      ttl: 172800
    # Redis Session 初始 TTL（秒），默认 24h
    redis-ttl: 86400
    # 同一用户最大并发会话数（0=不限制），默认 0
    max-concurrent: 0
    # 续期阈值（秒）：Redis 剩余 TTL 低于此值时触发续期，默认 30min
    renewal:
      threshold: 1800
      # 续期间隔（秒）：同一 session 在此时长内不重复续期，默认 5min
      interval: 300
    # 白名单路径（逗号分隔，Ant 风格），默认 /**/public/**
    white-list: "/**/public/**,/favicon.ico"
```

客户端部署（远程调用 SSO 服务端）追加：

```yaml
iam:
  session:
    remote:
      # SSO 服务端地址（配置后启用远程调用）
      server-url: "https://sso.example.com"
      # AK 鉴权应用 ID（在 SSO 管理后台申请）
      app-id: "your-app-id"
      # AK 鉴权应用密钥（RSA 私钥，与 SSO 端公钥配对）
      app-secret: "your-rsa-private-key"
```

### 3. 自动配置注册

iam-session 通过 Spring Boot 3.x 自动配置机制注册：

- 文件：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- 内容：`com.wkclz.iam.session.IamSessionAutoConfig`

接入项目只要在依赖中引入 `iam-session`，且包路径扫描覆盖 `com.wkclz.iam.session`（自动配置机制保证），无需手动 `@Import` 或 `@ComponentScan`。

## 核心组件

### SessionManager — 会话管理器

`com.wkclz.iam.session.service.SessionManager`（`@Component`）。会话生命周期管理入口，认证方式无关。

| 方法                                                                    | 返回值                    | 说明                                                                                          |
|-----------------------------------------------------------------------|------------------------|---------------------------------------------------------------------------------------------|
| `createSession(UserIdentity identity, AuthType authType)`             | `SessionCreateResult`  | 创建会话：生成 JWT → sessionId=MD5(token) → 并发控制 → Redis 持久化 → 发布 SessionCreatedEvent              |
| `validateAndRefresh(String token)`                                    | `Session` / `null`     | 校验 Token + 滑窗续期；JWT 校验失败或 Redis 不存在返回 null                                                    |
| `destroySession(String sessionId, DestroyReason reason)`              | `boolean`              | 销毁单个会话（登出场景）；返回 true 表示已销毁，false 表示会话不存在                                                    |
| `destroyAllSessions(String subjectId, DestroyReason reason)`          | `int`                  | 批量销毁用户所有会话（改密、禁用场景）；返回实际销毁数量                                                                |
| `getActiveSessions(String subjectId)`                                 | `List<Session>`        | 查询用户所有活跃会话（按 redisExpireTime 过滤）                                                             |

`createSession` 关键流程：

1. `TokenService.generateToken()` 生成 JWT
2. `sessionId = Md5Tool.md5(token)`
3. 构建 `Session`（含 `userIdentity` JSON 序列化）
4. 并发控制：若活跃会话数 ≥ `maxConcurrent`，通过 Lua 脚本踢出最早会话
5. `SessionStore.save(session, redisTtl)` 持久化
6. 发布 `SessionEvent.created(session)` 事件

`validateAndRefresh` 滑窗续期策略：

- JWT 校验失败 → 返回 null
- Redis 不存在 → 返回 null
- Redis 已过期 → 发布 `SessionEvent.expired` + 清理 + 返回 null
- 快速路径：Token 签发时间 + (redisTtl - threshold) 还未到 → 跳过续期判断
- 续期条件：剩余 TTL < threshold && 距上次续期 > interval && JWT 还有空间
- 续期只更新 Redis（HSET + EXPIRE），不重新签发 JWT

### TokenService — JWT 令牌服务

`com.wkclz.iam.session.service.TokenService`（`@Service`）。HS256 签名，claims 仅含 `sub(userCode) / username / nickname / iat / exp`。

| 方法                                                                | 返回值          | 说明                                                                         |
|-------------------------------------------------------------------|--------------|----------------------------------------------------------------------------|
| `generateToken(String userCode, String username, String nickname)` | `String`     | 生成 JWT，iat 偏移 -60s（容错时钟偏差），exp = now + ttl*1000                             |
| `verifyToken(String token)`                                       | `TokenInfo`  | 校验签名与过期，失败抛 `IllegalArgumentException`（含中文错误原因）                            |
| `refreshToken(String oldToken)`                                   | `String`     | 基于旧 Token 签发新 Token（新 iat/exp）                                              |
| `parseClaimsBestEffort(String token)`                             | `TokenInfo` / `null` | 尽力解析：过期 Token 仍尝试从 `ExpiredJwtException.getClaims()` 提取身份；完全无法解析返回 null       |
| `resolve(HttpServletRequest request)`（static）                     | `String` / `null` | 从请求头提取 Token：`Authorization: Bearer xxx` 或 `token` 头，未找到返回 null             |

> `parseClaimsBestEffort` 用于白名单接口或会话失效时，尽最大可能获取用户身份信息（如审计日志）。

### SessionStore — Redis 持久化

`com.wkclz.iam.session.service.SessionStore`（`@Component`）。基于 Redis Hash + ZSet，无接口抽象。

- Redis Key：`iam:session:{sessionId}`（Hash，存储 Session 各字段）
- 索引 Key：`iam:session:index:{subjectId}`（ZSet，score=createTime，value=sessionId）
- 关键方法：`save / get / delete / deleteBySubjectId / refresh / renewSession / getSessionIds`

### SessionAuthFilter — 会话认证过滤器

`com.wkclz.iam.session.filter.SessionAuthFilter`（`@Component`，继承 `OncePerRequestFilter`）。每次请求执行流程：

1. **无条件设置 appCode + tenantCode**（从请求头 `app-code` / `tenant-code`）
2. **获取身份（准入唯一依据 = 完整会话）**：
    - `sessionManager.validateAndRefresh(token)`（JWT 有效 + Redis 会话存在）→ 反序列化 `UserIdentity` →
      `IdentityContext.set(identity, token)`
3. **准入判断**：
    - 白名单（`WhiteListMatcher` 默认 `/**/public/**`）→ 放行；无有效会话时 `tokenService.parseClaimsBestEffort(token)`
      仅用于审计日志，不作为准入依据
    - 非白名单 + 无有效会话 → 返回 401 JSON：`{"message":"缺少认证 Token"}` 或 `{"message":"会话无效或已过期"}`（JWT
      过期 / 登出后 Redis 会话已删均被拦截）
    - 非白名单 + 有效会话 → 放行
4. **不调用 `IdentityContext.clear()`**（由外层 `RequestRecordFilter` 在 finally 中清理）

### RequestRecordFilter — 请求日志采集过滤器

`com.wkclz.iam.session.filter.RequestRecordFilter`（`@Component`，`@Order(LOWEST_PRECEDENCE - 10)`）。最外层包装过滤器，Order 优先级高于 `SessionAuthFilter`。

执行流程：
1. 包装 `ContentCachingRequestWrapper` / `ContentCachingResponseWrapper`
2. 调用下层过滤器链
3. `finally` 块中：
   - 采集请求/响应日志（从 `IdentityContext` 读取用户信息）
   - **`IdentityContext.clear()`**（防止 ThreadLocal 内存泄漏）
   - `wrappedResponse.copyBodyToResponse()`（必须，否则客户端收不到响应体）
   - 异步通过 `RequestRecordHandler` SPI 持久化

特性：
- 静态资源后缀（.js/.css/.png 等）和 `/public/status`、`/public/health` 不记录日志
- URI 过滤结果通过 Guava Cache 缓存（最多 1000 条，1h 过期）
- 密码字段脱敏：`"password":"xxx"` → `"password":"***"`
- Token 脱敏：保留前 8 位 + `***` + 后 4 位
- 字段截断：所有字段按数据库长度截断

## sh-core 身份上下文（会话临时保存）

iam-session 依赖 sh-core 提供的请求级身份上下文，这是接入项目获取当前登录用户的核心 API。

### UserIdentity — 用户身份

`com.wkclz.core.identity.UserIdentity`（`@Data`，`implements Serializable`）。最精简的标识信息集，仅回答"当前请求是谁"。

| 字段          | 类型                   | 说明                          |
|-------------|----------------------|-----------------------------|
| userCode    | String               | 用户唯一编码（如 `user_xxx`）         |
| username    | String               | 用户名（登录名）                     |
| nickname    | String               | 用户昵称（显示名）                    |
| avatar      | String               | 头像地址                        |
| attributes  | `Map<String,Object>` | 扩展属性（默认空 Map，如小程序 openid 等） |

便捷方法：`addAttribute(String key, Object value)` — 添加单个扩展属性。

### IdentityContext — 身份上下文（ThreadLocal）

`com.wkclz.core.identity.IdentityContext`（`final` 类，全部 static 方法）。请求级 ThreadLocal 实现，零外部依赖。

#### 写入方法

| 方法                                              | 说明                                            |
|-------------------------------------------------|-----------------------------------------------|
| `set(UserIdentity identity, String token)`     | 设置当前线程的身份和 Token（**identity 和 token 不可为 null**） |
| `setAppCode(String appCode)`                   | 设置当前线程的应用编码（从请求头 `app-code`）                  |
| `setTenantCode(String tenantCode)`             | 设置当前线程的租户编码（从请求头 `tenant-code`）               |

#### 读取方法

| 方法                        | 返回值              | 说明                              |
|---------------------------|------------------|---------------------------------|
| `get()`                   | `UserIdentity` / `null` | 获取当前线程的用户身份                     |
| `getToken()`              | `String` / `null` | 获取当前线程的认证 Token                 |
| `getUserCode()`           | `String` / `null` | 快捷获取 userCode                  |
| `getUsername()`           | `String` / `null` | 快捷获取 username                   |
| `getNickname()`           | `String` / `null` | 快捷获取 nickname                   |
| `getAvatar()`             | `String` / `null` | 快捷获取 avatar                     |
| `getAttribute(String key)` | `String` / `null` | 获取扩展属性                          |
| `getAppCode()`            | `String` / `null` | 获取当前应用编码                        |
| `getTenantCode()`         | `String` / `null` | 获取当前租户编码                        |

#### 清理方法

| 方法         | 说明                                                       |
|------------|----------------------------------------------------------|
| `clear()`  | 清理当前线程的身份信息。**必须由请求入口的 finally 块调用**（iam-session 已在 `RequestRecordFilter` 中处理） |

#### 关键约束

- 不负责自动清理：必须由上层 Filter/Interceptor 在 finally 中调用 `clear()`
- `set()` 要求 identity 和 token 都不能为 null，否则抛 `IllegalArgumentException`
- ThreadLocal 通过 `withInitial(IdentityContext::new)` 初始化，避免 NPE
- 业务代码中**只读不写**：写入由 `SessionAuthFilter` 在请求开始时完成，清理由 `RequestRecordFilter` 在请求结束时完成

## 配置项清单

`com.wkclz.iam.session.config.IamSessionConfig`（`@Configuration` + `@ConfigurationProperties` 隐式绑定）。

| 配置项                                | 字段                 | 类型        | 默认值                                          | 说明                          |
|------------------------------------|--------------------|-----------|----------------------------------------------|-----------------------------|
| `iam.session.token.secret-key`     | secretKey          | String    | `qwertyuioplkjhgfdsazxcvbnmqwertyuioplkjhgfdsazxcvbnm` | JWT 签名密钥（HS256，必须 ≥ 32 字符）   |
| `iam.session.token.ttl`            | ttl                | Long      | `172800`（48h）                                | Token TTL（秒）                |
| `iam.session.max-concurrent`       | maxConcurrent      | Integer   | `0`（不限制）                                     | 同一用户最大并发会话数                 |
| `iam.session.redis-ttl`            | redisTtl           | Long      | `86400`（24h）                                 | Redis Session 初始 TTL（秒）     |
| `iam.session.renewal.threshold`    | renewalThreshold   | Long      | `1800`（30min）                                | 续期阈值（秒）：剩余 TTL 低于此值触发续期     |
| `iam.session.renewal.interval`     | renewalInterval    | Long      | `300`（5min）                                  | 续期间隔（秒）：同一 session 不重复续期    |

`WhiteListMatcher` 通过构造器注入 `iam.session.white-list`（逗号分隔，Ant 风格），默认 `/**/public/**`。

`RemoteClientConfig`（仅客户端模式生效，前缀 `iam.session.remote`）：

| 配置项                                | 字段        | 说明                                |
|------------------------------------|-----------|-----------------------------------|
| `iam.session.remote.server-url`    | serverUrl | SSO 服务端地址（**配置后启用客户端模式**）         |
| `iam.session.remote.app-id`        | appId     | AK 鉴权应用 ID                        |
| `iam.session.remote.app-secret`    | appSecret | AK 鉴权应用密钥（RSA 私钥）                 |
| `iam.session.remote.static.enabled` | enabled   | 静态资源过滤开关，默认 `false`              |
| `iam.session.remote.static.subfix` | subfix    | 静态资源后缀正则片段，默认 `js|css|jpg|png|...` |

## 使用示例

### 示例 1：登录流程（服务端实现）

参考 iam-sso 的 `PasswordLoginService.login()`：

```java
@Service
public class MyLoginService {

    @Autowired
    private SessionManager sessionManager;
    // 其他依赖：用户查询、密码校验、验证码服务等

    public LoginResp login(HttpServletRequest request, LoginReq req) {
        // 1. 业务校验：用户存在性、密码、验证码、账号状态等
        //    （此处省略，由业务方自行实现）
        UserIdentity userIdentity = verifyCredentials(req.getUsername(), req.getPassword());

        // 2. 委托 SessionManager 创建会话
        SessionCreateResult sessionResult = sessionManager.createSession(
            userIdentity,
            AuthType.PASSWORD
        );

        // 3. 构建响应（使用 iam-session 提供的 LoginResp）
        LoginResp resp = new LoginResp();
        resp.setLoginStatus(LoginStatus.SUCCESS.getCode());
        resp.setLoginMessage(LoginStatus.SUCCESS.getMessage());
        resp.setToken(sessionResult.getToken());
        resp.setUserCode(userIdentity.getUserCode());
        resp.setUsername(userIdentity.getUsername());
        resp.setNickname(userIdentity.getNickname());
        resp.setAvatar(userIdentity.getAvatar());
        return resp;
    }
}
```

`UserIdentity` 构建示例：

```java
UserIdentity identity = new UserIdentity();
identity.setUserCode("user_123456");
identity.setUsername("zhangsan");
identity.setNickname("张三");
identity.setAvatar("https://example.com/avatar.jpg");
// 可选：扩展属性
identity.addAttribute("openid", "wx_openid_xxx");
```

### 示例 2：业务代码中获取当前登录用户

```java
@RestController
@RequestMapping("/api/order")
public class OrderRest {

    @GetMapping("/list")
    public R<List<OrderResp>> listMyOrders() {
        // 直接调用静态方法，无需注入
        String userCode = IdentityContext.getUserCode();
        String username = IdentityContext.getUsername();
        String nickname = IdentityContext.getNickname();
        String tenantCode = IdentityContext.getTenantCode();
        String appCode = IdentityContext.getAppCode();
        String token = IdentityContext.getToken();

        // 获取完整 UserIdentity 对象
        UserIdentity identity = IdentityContext.get();
        if (identity == null) {
            throw UnauthorizedException.of("用户未登录");
        }

        log.info("用户 {} 查询订单列表, tenant={}, app={}", username, tenantCode, appCode);
        return R.ok(orderService.listByUserCode(userCode));
    }
}
```

> 业务代码中**只读不写** IdentityContext。设置和清理由 `SessionAuthFilter` / `RequestRecordFilter` 自动完成。

### 示例 3：登出（销毁当前会话）

```java
@Service
public class MyLogoutService {

    @Autowired
    private SessionManager sessionManager;

    public void logout() {
        // 1. 从 IdentityContext 取当前 token
        String token = IdentityContext.getToken();
        if (StringUtils.isBlank(token)) {
            return; // 未登录状态，跳过
        }
        String username = IdentityContext.getUsername();
        log.info("用户 {} 登出", username);

        // 2. sessionId = MD5(token)
        String sessionId = Md5Tool.md5(token);

        // 3. 委托 SessionManager 销毁会话
        sessionManager.destroySession(sessionId, DestroyReason.LOGOUT);

        // 4. 清理身份上下文（如果是自定义入口，需要手动清理；
        //    走标准 Filter 链路由由 RequestRecordFilter 自动清理）
        IdentityContext.clear();
    }
}
```

### 示例 4：改密 / 禁用用户（批量销毁会话）

```java
@Service
public class MyUserService {

    @Autowired
    private SessionManager sessionManager;

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(String userCode, String newPassword) {
        // 1. 业务逻辑：校验旧密码、更新密码、记录历史等
        // ...

        // 2. 全会话失效：让用户重新登录
        int count = sessionManager.destroyAllSessions(
            userCode,
            DestroyReason.PASSWORD_CHANGED
        );
        log.info("用户 {} 修改密码成功，已销毁 {} 个会话", userCode, count);
    }

    public void disableUser(String userCode) {
        // 1. 更新用户状态为禁用
        // ...

        // 2. 全会话失效
        sessionManager.destroyAllSessions(userCode, DestroyReason.USER_DISABLED);
    }
}
```

`DestroyReason` 枚举（6 个值）：

| 枚举值                       | 说明           |
|---------------------------|--------------|
| `LOGOUT`                  | 用户主动登出       |
| `PASSWORD_CHANGED`        | 用户自己修改密码     |
| `PASSWORD_RESET_BY_ADMIN` | 管理员重置密码      |
| `USER_DISABLED`           | 用户被禁用        |
| `CONCURRENT_KICK`         | 并发会话踢出       |
| `SESSION_EXPIRED`         | 会话自然过期       |

### 示例 5：查询用户活跃会话（"在线设备"列表）

```java
@Service
public class MySessionQueryService {

    @Autowired
    private SessionManager sessionManager;

    public List<SessionVO> listMySessions() {
        String userCode = IdentityContext.getUserCode();
        List<Session> sessions = sessionManager.getActiveSessions(userCode);

        return sessions.stream().map(s -> {
            SessionVO vo = new SessionVO();
            vo.setSessionId(s.getSessionId());
            vo.setAuthType(s.getAuthType().getDesc());
            vo.setCreateTime(new Date(s.getCreateTime()));
            vo.setLastActiveTime(new Date(s.getLastRenewalTime()));
            vo.setExpireTime(new Date(s.getRedisExpireTime()));
            // 注意：当前会话判断
            String currentToken = IdentityContext.getToken();
            vo.setCurrent(s.getToken() != null && s.getToken().equals(currentToken));
            return vo;
        }).collect(Collectors.toList());
    }
}
```

### 示例 6：监听会话生命周期事件

实现 `SessionEventListener` SPI（用于审计日志、登录通知等）：

```java
@Component
public class MySessionEventListener implements SessionEventListener {

    @Override
    public void onCreated(Session session) {
        log.info("会话创建: userCode={}, sessionId={}, authType={}",
            session.getSubjectId(), session.getSessionId(), session.getAuthType());
        // 可选：写入审计日志、推送登录通知等
    }

    @Override
    public void onDestroyed(String sessionId, String subjectId, DestroyReason reason) {
        log.info("会话销毁: userCode={}, sessionId={}, reason={}",
            subjectId, sessionId, reason);
        // 可选：异常登录地通知（如改密后的强制下线）
    }

    @Override
    public void onExpired(String sessionId, String subjectId) {
        log.info("会话过期: userCode={}, sessionId={}", subjectId, sessionId);
    }
}
```

> 未实现时使用 `NoOpSessionEventListener` 默认空实现。事件由 `SessionManager` 通过 `ApplicationEventPublisher` 发布，监听器同步执行，请避免在监听器中执行耗时操作。

### 示例 7：客户端部署（远程接入 SSO 服务端）

业务系统作为客户端，配置 `iam.session.remote.server-url` 后，iam-session 自动装配：

- `RemoteClientConfig`：读取 `iam.session.remote.*` 配置
- `RemoteSsoFacadeImpl`：实现 `SsoFacade`，通过 HTTP 调用 SSO 服务端
- `RemoteRequestRecordHandler`：实现 `RequestRecordHandler` SPI，远程持久化请求日志

`SsoFacade` 接口（`com.wkclz.iam.session.remote.SsoFacade`）：

| 方法                            | 参数                    | 说明                          |
|-------------------------------|-----------------------|-----------------------------|
| `saveLog(RequestRecord record)` | `RequestRecord`       | 远程保存请求日志到 SSO 服务端           |
| `logout(String token)`        | `String`              | 远程登出指定 token 的用户            |
| `logout()`                    | 无                     | 远程登出当前用户（从 `IdentityContext.getToken()` 获取） |

客户端业务代码调用：

```java
@Service
public class MyClientService {

    @Autowired
    private SsoFacade ssoFacade; // 自动注入 RemoteSsoFacadeImpl

    public void remoteLogout() {
        // 自动从 IdentityContext 取 token，HTTP 调用 SSO 服务端 /sign/logout
        ssoFacade.logout();
    }
}
```

HTTP 调用细节（由 iam-session 内部处理，业务方无感知）：

- 请求头：`app-id`（应用 ID）+ `sign`（AK 签名）
- 签名算法：`AkSignHelper.sign(appId, appSecret)`，RSA 私钥加密 `appId=xxx&nonce=xxx&timestamp=xxx`
- 签名有效期：5 分钟
- nonce 防重放：SSO 服务端通过 Redis SETNX 校验

### 示例 8：自定义请求日志持久化（服务端）

服务端部署时，iam-sso 提供 `RequestRecordHandlerImpl` 持久化到本地 `iam_request_record` 表。若需自定义持久化（如写入 ES）：

```java
@Component
public class MyEsRequestRecordHandler implements RequestRecordHandler {

    @Autowired
    private ElasticsearchClient esClient;

    @Override
    public void handle(RequestRecord record) {
        try {
            esClient.index(i -> i.index("iam-request-log").document(record));
        } catch (Exception e) {
            log.warn("Failed to persist request log to ES: uri={}, error={}",
                record.getRequestUri(), e.getMessage());
        }
    }
}
```

> 注册自定义 Bean 后，`NoOpRequestRecordHandler` 的 `@ConditionalOnMissingBean` 条件不满足，自动让位。`RequestRecord` 字段包括 method/requestUri/requestBody/responseStatus/userCode/tenantCode/appCode/costTime 等。

### 示例 9：扩展白名单路径

通过配置扩展：

```yaml
iam:
  session:
    white-list: "/**/public/**,/api/health,/api/docs/**,/favicon.ico"
```

或自定义 `WhiteListMatcher` Bean（覆盖默认）：

```java
@Component
public class CustomWhiteListMatcher extends WhiteListMatcher {
    public CustomWhiteListMatcher(@Value("${iam.session.white-list:}") String config) {
        super(config);
    }

    @Override
    public boolean isWhiteListed(String requestUri) {
        // 额外放行所有 OPTIONS 预检请求
        if ("OPTIONS".equals(getCurrentMethod())) {
            return true;
        }
        return super.isWhiteListed(requestUri);
    }
    // ...
}
```

## SPI 扩展点

| SPI                       | 默认实现                            | 触发条件                                  | 用途                          |
|--------------------------|---------------------------------|---------------------------------------|-----------------------------|
| `RequestRecordHandler`   | `NoOpRequestRecordHandler`      | `@ConditionalOnMissingBean`           | 请求日志持久化                     |
| `SessionEventListener`   | `NoOpSessionEventListener`      | Spring 自动选择                           | 会话生命周期审计                    |
| `SsoFacade`              | `RemoteSsoFacadeImpl`           | `@ConditionalOnProperty(server-url)`  | 客户端远程调用 SSO 服务端             |

扩展方式：实现接口 + `@Component` 注册，覆盖默认实现。

## 部署模式对比

| 维度       | 服务端模式                              | 客户端模式                                |
|----------|------------------------------------|--------------------------------------|
| 触发条件     | 未配置 `iam.session.remote.server-url` | 配置了 `iam.session.remote.server-url` |
| 额外依赖     | `iam-sso` 或 `iam-admin`            | 仅 `iam-session`                      |
| 会话存储     | 本地 Redis                           | 本地 Redis（Token 校验） + 远程 SSO（日志、登出）   |
| 请求日志持久化  | `RequestRecordHandlerImpl`（iam-sso）→ 本地数据库 | `RemoteRequestRecordHandler` → HTTP 调用 SSO 服务端 |
| 登出       | `SessionManager.destroySession` 本地销毁 | `SsoFacade.logout()` 远程销毁 + 本地清理      |
| 登录       | 业务方实现（参考 iam-sso `PasswordLoginService`） | 调用 SSO 服务端登录接口（不实现本地登录）              |

## 数据模型

### Session 字段

| 字段               | 类型        | 说明                                  |
|------------------|-----------|-------------------------------------|
| sessionId        | String    | MD5(token)，固定 32 字符                 |
| subjectId        | String    | 用户标识（userCode）                      |
| authType         | AuthType  | 认证方式                                |
| token            | String    | 原始 JWT Token（存于 Redis Hash，用于反向定位）   |
| userIdentity     | String    | `UserIdentity` JSON 序列化             |
| createTime       | long      | 创建时间戳（毫秒）                          |
| expireTime       | long      | JWT 过期时间戳（毫秒）                       |
| redisExpireTime  | long      | Redis Key 实际过期时间戳（毫秒），续期时更新         |
| lastRenewalTime  | long      | 上次续期时间戳（毫秒）                         |
| metadata         | `Map<String,String>` | 扩展属性                                |

> `clientIp` / `userAgent` 不存储在 Session 中，由 SSO 层每次请求从 `HttpServletRequest` 实时获取。

### TokenInfo 字段

| 字段        | 类型     | 说明                |
|-----------|--------|-------------------|
| userCode  | String | 用户编码              |
| username  | String | 用户名               |
| nickname  | String | 昵称                |
| issuedAt  | Long   | JWT 签发时间戳（毫秒）     |
| expireAt  | Long   | JWT 过期时间戳（毫秒）     |

### AuthType 枚举

| 枚举值          | 描述    |
|--------------|-------|
| `PASSWORD`   | 密码登录  |
| `WECHAT_MINI` | 微信小程序 |
| `WECHAT_MP`  | 微信公众号 |
| `LDAP`       | LDAP  |
| `OAUTH`      | OAuth |

### LoginStatus 枚举

参照 LDAP 返回码设计，共 17 个值。常用：

| 枚举值                    | code | 说明       |
|------------------------|------|----------|
| `SUCCESS`              | 0    | 登录成功     |
| `INTERNAL_ERROR`       | 1    | 内部错误     |
| `INVALID_PASSWORD`     | 31   | 密码错误     |
| `USER_NOT_FOUND`       | 32   | 用户不存在    |
| `EXPIRED_PASSWORD`     | 48   | 密码过期     |
| `EXPIRED_ACCOUNT`      | 49   | 账号过期     |
| `AUTHENTICATION_FAILED` | 50   | 认证失败     |
| `TOO_MANY_ATTEMPTS`    | 51   | 登录尝试次数过多 |
| `INVALID_CREDENTIALS`  | 52   | 无效凭证     |
| `ACCOUNT_DISABLED`     | 53   | 账号禁用     |
| `INVALID_CAPTCHA`      | 54   | 验证码错误    |
| `NEED_CAPTCHA`         | 60   | 需要验证码    |
| `CAPTCHA_TIMEOUT`      | 61   | 验证码超时    |
| `ACCOUNT_LOCKED`       | 47   | 账号锁定     |

## 注意事项

1. **secret-key 必须替换**：默认密钥仅用于开发测试，生产环境必须通过配置或环境变量覆盖，长度 ≥ 32 字符
2. **IdentityContext.clear() 由框架处理**：标准 Filter 链路由 `RequestRecordFilter` 自动清理；自定义入口（如非 HTTP 请求）需手动 `IdentityContext.clear()`
3. **并发会话控制需配置**：默认 `max-concurrent=0` 不限制；设置正值后通过 Lua 脚本踢出最早会话，触发 `CONCURRENT_KICK` 事件
4. **续期不重新签发 JWT**：续期只更新 Redis TTL，JWT 过期时间不变；JWT 过期后需重新登录
5. **客户端模式与登录**：客户端部署不实现本地登录，需调用 SSO 服务端登录接口获取 Token
6. **Redis 必须可用**：iam-session 强依赖 Redis，Redis 不可用会导致会话校验失败、所有非白名单请求返回 401
7. **Session.token 字段慎用**：Redis Hash 中存储原始 Token 用于反向定位，日志输出会脱敏；业务代码请使用 `IdentityContext.getToken()` 获取
8. **包扫描**：`IamSessionAutoConfig` 通过 `@ComponentScan("com.wkclz.iam.session")` 扫描，自定义扩展类放在此包下会被自动注册
9. **与 sh-core UserContext 区别**：iam-session 使用 `IdentityContext`（com.wkclz.core.identity），与旧版 `UserContext`（com.wkclz.core.user）不同；接入 iam-session 后业务代码统一用 `IdentityContext`

## 快速参考

### 接入清单

```
1. pom.xml 添加 iam-session 依赖
2. application.yml 配置 iam.session.token.secret-key（必须）+ 其他可选项
3. 业务代码通过 IdentityContext.getUserCode() 等读取当前用户
4. 登录入口调用 SessionManager.createSession(userIdentity, authType)
5. 登出入口调用 SessionManager.destroySession(sessionId, DestroyReason.LOGOUT)
6. 改密/禁用调用 SessionManager.destroyAllSessions(userCode, reason)
7. 客户端部署追加 iam.session.remote.server-url + app-id + app-secret
```

### 核心 API 速查

```java
// 创建会话（登录）
SessionCreateResult result = sessionManager.createSession(userIdentity, AuthType.PASSWORD);
String token = result.getToken();

// 校验 Token（过滤器已自动调用，业务一般不需要）
Session session = sessionManager.validateAndRefresh(token);

// 销毁会话（登出）
sessionManager.destroySession(Md5Tool.md5(token), DestroyReason.LOGOUT);

// 批量销毁（改密/禁用）
sessionManager.destroyAllSessions(userCode, DestroyReason.PASSWORD_CHANGED);

// 查询活跃会话
List<Session> sessions = sessionManager.getActiveSessions(userCode);

// 读取当前用户（业务代码常用）
String userCode = IdentityContext.getUserCode();
UserIdentity identity = IdentityContext.get();
String token = IdentityContext.getToken();

// 从请求中提取 Token
String token = TokenService.resolve(request);

// 尽力解析过期 Token（审计场景）
TokenInfo info = tokenService.parseClaimsBestEffort(token);
```
