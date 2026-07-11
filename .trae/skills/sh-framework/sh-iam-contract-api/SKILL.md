---
name: "sh-iam-contract-api"
description: "sh-framework IAM 契约层 API 模块知识库。包含认证(AuthContract)/鉴权(AuthzContract)/AK签名(AkSignContract)/SSO门面(SsoFacadeContract)四契约SPI、Principal/Session/AuthResult等10个中性模型、LoginFailType登录失败类型枚举(10值+中文message)、LoginResp失败建模(success+failType+failReason+静态工厂)、PrincipalContext双存储上下文、ContractSettings静态配置、AuthException异常体系、AuthErrorType认证错误类型枚举。当涉及IAM契约集成、Principal读取、契约接口调用、登录失败建模时调用。"
---

# sh-iam-contract-api 模块知识库

sh-iam-contract-api 是 sh-framework IAM 契约层的 API 模块，提供认证、鉴权、AK 签名、SSO 门面四契约 SPI，配合
Principal/Session 等中性模型、PrincipalContext 双存储上下文、ContractSettings 静态配置、AuthException
异常体系。零业务依赖，业务系统引入此模块即可使用 PrincipalContext 读取用户、调用契约 SPI，无需引入 default 模块的实现。

## 模块定位

- **零业务依赖**：仅 `spring-boot-starter-web`（provided）+ lombok + swagger-annotations
- **职责**：定义契约 SPI + 中性模型 + 上下文 + 静态配置 + 异常体系
- **不含实现**：单独引入此模块不会注册任何 Bean；需配合 iam-contract-default 或自行实现
- **Maven 坐标**：`com.wkclz.framework:iam-contract-api:${revision}`（当前版本 5.0.1-SNAPSHOT）
- **Java 版本**：Java 25 编译，与框架整体一致

## 包结构

`com.wkclz.iam.contract` 下 7 个顶层子包（bean 下含 req/resp 两个子子包）：

```
com.wkclz.iam.contract
├── service/         # 3 个契约 SPI（AuthContract / AuthzContract / AkSignContract）
├── facade/          # SsoFacadeContract（SSO 门面）
├── bean/            # 10 个中性模型（Principal/Session/AuthResult/Tenant/App/Menu/Api/FieldPermission/DataDimension/RequestLog）
│   ├── req/         # SessionCreateReq
│   └── resp/        # LoginResp
├── context/         # PrincipalContext（双存储上下文）
├── config/          # ContractSettings（静态配置持有器）, FilterOrder（过滤器顺序常量）
├── enums/           # AuthScene, LoginFailType, AuthErrorType, JwtErrorCodes（JWT 错误码常量）
└── exception/       # AuthException
```

## 四契约 SPI 详解

### AuthContract — 认证契约

位于 `com.wkclz.iam.contract.service.AuthContract`。负责从 HTTP 请求或 token 中认证用户，返回 Principal + Session。

| 方法                      | 参数                                      | 返回值                   | 异常              | 说明                                                                                                                                                         |
|-------------------------|-----------------------------------------|-----------------------|-----------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `authenticate`          | `HttpServletRequest request`            | `AuthResult` / `null` | `AuthException` | 从 HTTP 请求中认证用户（过滤器主入口）。token 不存在时返回 null（由过滤器据此放行 public 路径）；token 无效/签名错误/会话过期抛 AuthException                                                             |
| `doAuthenticate`        | `String token`                          | `AuthResult`          | `AuthException` | 从 token 中认证用户（核心认证逻辑），由 `checkToken` 模板方法调用。与 `authenticate` 的认证逻辑一致，区别在于 token 已传入无需从请求头提取                                                                |
| `checkToken`（`default`） | `String token`, `String authIdentifier` | `Session`             | `AuthException` | 模板方法，内置通用校验：token 空值→TOKEN_MISSING → 调用 doAuthenticate → Session 为空→SESSION_EXPIRED → authIdentifier 不一致→TOKEN_INVALID。实现方只需实现 `doAuthenticate` 无需重复编写通用校验 |

**实现职责（authenticate）**：

1. 从请求头提取 token（Authorization / token，去 Bearer 前缀）
2. 校验 JWT 签名与有效期
3. 校验 Session 存在性（如 Redis）
4. 返回 Principal + Session 聚合为 AuthResult

**实现职责（doAuthenticate）**：

1. 校验 JWT 签名与有效期
2. 从 Redis 或其他存储获取 Session
3. 返回 Principal + Session 聚合为 AuthResult

> 与 authenticate 的认证逻辑保持一致，区别在于 token 已由调用方传入，无需从请求头提取。

### AuthzContract — 鉴权契约（六维度 + 三重载）

位于 `com.wkclz.iam.contract.service.AuthzContract`。覆盖租户/应用/菜单/接口/字段/数据六个维度，每个维度提供三种重载模式：

- **完整参数版本**：显式传入 `Principal` + 其他参数，用于单元测试和非 HTTP 场景
- **上下文重载版本**（default）：从 `PrincipalContext` 自动获取 Principal 等信息，用于业务代码
- **请求全自动版本**（default）：从 `HttpServletRequest` 自动获取所有信息，用于过滤器

| 维度      | 完整参数方法                                                                          | 上下文重载（default）                                                                               | 备注                                    |
|---------|---------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------|---------------------------------------|
| 1. 租户   | `listTenants(Principal)`                                                        | `listTenants()`                                                                              | 查询用户可访问的租户列表                          |
| 2. 应用   | `listApps(Principal, String tenantCode)`                                        | `listApps()` / `listApps(String tenantCode)`                                                 | 半上下文重载：Principal 从上下文，tenantCode 显式传入 |
| 3. 菜单树  | `getMenuTree(Principal, String appCode)`                                        | `getMenuTree()` / `getMenuTree(String appCode)`                                              | 多根节点列表（多个顶级菜单）；树构建由实现层负责              |
| 4. 接口鉴权 | `canAccessApi(Principal, String appCode, String apiUri, String apiMethod)`      | `canAccessApi(String apiUri, String apiMethod)` / `canAccessApi(HttpServletRequest request)` | 请求全自动重载从 request 自动获取 uri + method    |
| 5. 字段权限 | `listFieldPermissions(Principal, String appCode, String menuCode)`              | `listFieldPermissions(String menuCode)`                                                      | 返回字段权限列表（含 visible / editable）        |
| 5. 字段过滤 | `filterFields(Principal, String appCode, String menuCode, List<String> fields)` | `filterFields(String menuCode, List<String> fields)`                                         | 根据权限过滤字段，返回有权限的字段列表                   |
| 6. 数据权限 | `getDataDimensions(Principal, String appCode)`                                  | `getDataDimensions()` / `getDataDimensions(String appCode)`                                  | 返回数据维度列表（如"部门"维度 + 授权的部门 ID 列表）       |

> 注：字段维度有 2 个方法（listFieldPermissions + filterFields），所以共 7 个方法组、6 个维度。`canAccessApi` 返回
> boolean，其余返回 List。

### AkSignContract — AK 签名契约

位于 `com.wkclz.iam.contract.service.AkSignContract`。用于服务间 RPC 调用的签名认证。

| 方法                    | 参数                                                        | 返回值       | 异常              | 说明                                                               |
|-----------------------|-----------------------------------------------------------|-----------|-----------------|------------------------------------------------------------------|
| `sign`                | `String appId`, `String appSecret`                        | `String`  | —               | 生成 AK 签名（客户端调用），appSecret 为 RSA 私钥；返回签名字符串放入请求头 sign 字段          |
| `sign`（default）       | 无                                                         | `String`  | —               | 重载：从 `ContractSettings` 获取 appId + appSecret                     |
| `verifySign`          | `String sign`, `String publicKey`, `String expectedAppId` | `boolean` | `AuthException` | 验证 AK 签名（服务端调用）；验签通过返回 true，失败抛 AuthException                    |
| `verifySign`（default） | `HttpServletRequest request`                              | `boolean` | `AuthException` | 重载：从请求头 sign / app-id + `ContractSettings.getPublicKey()` 自动获取参数 |

**verifySign 实现职责**：

1. RSA 公钥解密签名，解析参数（appId / nonce / timestamp）
2. 校验签名中的 appId 与请求头 app-id 一致
3. 校验 timestamp 在 5 分钟有效期内
4. nonce 防重放校验（如 Redis SETNX）

### SsoFacadeContract — SSO 门面契约

位于 `com.wkclz.iam.contract.facade.SsoFacadeContract`。客户端应用通过此契约调用 SSO 服务端。

| 方法                | 参数                     | 返回值         | 异常 | 说明                                                                                  |
|-------------------|------------------------|-------------|----|-------------------------------------------------------------------------------------|
| `login`           | `SessionCreateReq req` | `LoginResp` | —  | 远程登录（创建会话并记录登录日志）。业务登录失败统一通过 LoginResp 返回（login 永不抛业务失败异常），仅系统级错误抛 RuntimeException |
| `saveLog`         | `RequestLog log`       | `void`      | —  | 远程保存请求日志（客户端应用将请求日志上报到 SSO 服务端集中存储）                                                 |
| `logout`          | `String token`         | `void`      | —  | 远程登出（指定 token）                                                                      |
| `logout`（default） | 无                      | `void`      | —  | 重载：从 `PrincipalContext.getToken()` 获取 token                                         |

### login() 语义边界

| 失败性质   | 传达方式                                      | 示例                  |
|--------|-------------------------------------------|---------------------|
| 业务登录失败 | `LoginResp.fail(failType, failReason)` 返回 | 密码错误、账号锁定、验证码错误     |
| 系统级错误  | 抛 RuntimeException                        | SSO 不可达、未配置实现、序列化失败 |

`login()` 永不抛业务登录失败异常，由调用方判断 `LoginResp.success`。

## 中性模型（10 个 bean）

所有 bean 位于 `com.wkclz.iam.contract.bean`（req/resp 子包除外），均 `implements Serializable` 并使用 `@Data` + `@Schema`
注解。

### Principal — 用户主体

从 JWT claims 解析，包含认证后的固有属性（你是谁）。**不含 tenantCode**（租户是运行时动态切换值，从请求头获取）。

| 字段       | 类型     | 说明   |
|----------|--------|------|
| userCode | String | 用户编码 |
| username | String | 用户名  |
| nickname | String | 昵称   |
| avatar   | String | 头像   |

### Session — 用户会话

仅保留 JWT 无法携带的动态会话数据，不与 Principal 重复字段。

| 字段             | 类型     | 说明                             |
|----------------|--------|--------------------------------|
| userCode       | String | 用户编码                           |
| authType       | String | 认证类型：PASSWORD / LDAP / OAUTH 等 |
| authIdentifier | String | 认证标识符（用户名或三方平台标识）              |

### AuthResult — 认证结果

Principal + Session 聚合，由 `AuthContract.authenticate()` 返回。

| 字段        | 类型        | 说明   |
|-----------|-----------|------|
| principal | Principal | 用户主体 |
| session   | Session   | 会话信息 |

### Tenant — 租户

| 字段         | 类型     | 说明   |
|------------|--------|------|
| tenantCode | String | 租户编码 |
| tenantName | String | 租户名称 |

### App — 应用

| 字段      | 类型     | 说明   |
|---------|--------|------|
| appCode | String | 应用编码 |
| appName | String | 应用名称 |
| icon    | String | 应用图标 |

### Menu — 菜单（树形结构）

仅包含核心展示字段，不含管理字段；树构建由实现层负责。

| 字段         | 类型           | 说明                      |
|------------|--------------|-------------------------|
| menuCode   | String       | 菜单编码                    |
| parentCode | String       | 父级菜单编码                  |
| menuName   | String       | 菜单名称                    |
| menuType   | String       | 菜单类型：MENU / BUTTON      |
| routePath  | String       | 路由路径                    |
| component  | String       | 前端组件路径                  |
| buttonCode | String       | 按钮编码（menuType=BUTTON 时） |
| icon       | String       | 图标                      |
| sort       | Integer      | 排序                      |
| children   | List\<Menu\> | 子菜单列表                   |

### Api — API 路由

| 字段        | 类型      | 说明                                |
|-----------|---------|-----------------------------------|
| apiCode   | String  | API 编码                            |
| apiName   | String  | API 名称                            |
| apiMethod | String  | HTTP 方法：GET / POST / PUT / DELETE |
| apiUri    | String  | URI 路径                            |
| writeFlag | Boolean | 是否写操作                             |

### FieldPermission — 字段权限

| 字段        | 类型      | 说明    |
|-----------|---------|-------|
| fieldCode | String  | 字段编码  |
| fieldName | String  | 字段名称  |
| visible   | Boolean | 是否可见  |
| editable  | Boolean | 是否可编辑 |

### DataDimension — 数据权限维度

`authorizedValues` 为通用值列表，业务层根据 `dimensionCode` 解释含义。

| 字段               | 类型             | 说明                  |
|------------------|----------------|---------------------|
| dimensionCode    | String         | 维度编码                |
| dimensionName    | String         | 维度名称                |
| authorizedValues | List\<String\> | 授权值列表（如部门 ID、区域编码等） |

### RequestLog — 请求日志

由 `SsoFacadeContract.saveLog()` 上报到 SSO 服务端。

| 字段             | 类型      | 说明      |
|----------------|---------|---------|
| uri            | String  | 请求 URI  |
| method         | String  | HTTP 方法 |
| requestBody    | String  | 请求体     |
| responseStatus | Integer | 响应状态码   |
| responseBody   | String  | 响应体     |
| requestTime    | Long    | 请求时间    |
| responseTime   | Long    | 响应时间    |
| duration       | Long    | 耗时(ms)  |
| clientIp       | String  | 客户端 IP  |
| userCode       | String  | 用户编码    |
| appCode        | String  | 应用编码    |

### SessionCreateReq（bean/req/）

由 `SsoFacadeContract.login()` 调用。

| 字段             | 类型     | 说明     |
|----------------|--------|--------|
| userCode       | String | 用户编码   |
| username       | String | 用户名    |
| nickname       | String | 昵称     |
| avatar         | String | 头像 URL |
| authType       | String | 认证类型   |
| authIdentifier | String | 认证标识符  |

> clientIp 与 userAgent 已从 SessionCreateReq 移除，实现方通过 `PrincipalContext` / `RequestHelper` 从请求上下文获取，与旧
> SsoFacadeImpl 行为一致。

### LoginResp（bean/resp/）

由 `SsoFacadeContract.login()` 返回，同时建模登录成功与失败。

| 字段         | 类型            | 说明                                |
|------------|---------------|-----------------------------------|
| success    | Boolean       | 是否登录成功                            |
| failType   | LoginFailType | 登录失败类型；成功时为 null                  |
| failReason | String        | 登录失败动态详情（如"请 300 秒后重试"）；成功时为 null |
| token      | String        | JWT Token；失败时为 null               |
| userCode   | String        | 用户编码；失败时为 null                    |
| username   | String        | 用户名；失败时为 null                     |
| nickname   | String        | 昵称；失败时为 null                      |
| avatar     | String        | 头像 URL；失败时为 null                  |

**3 个静态工厂**：

- `success(token, userCode, username, nickname, avatar)` → success=true，失败字段为 null
- `fail(failType)` → success=false，failReason 为 null
- `fail(failType, failReason)` → success=false，含动态详情

**语义不变量**：成功时失败字段必为 null，失败时成功字段必为 null。前端展示优先 `failReason`，为空时回退到
`failType.getMessage()`。

## PrincipalContext — 双存储上下文

位于 `com.wkclz.iam.contract.context.PrincipalContext`。替代 sh-core `UserContext` 的用户信息读取职责。

### 双存储策略

- **request.setAttribute**（主存储）：跟随请求生命周期，Servlet 规范保证线程安全
- **ThreadLocal**（辅助存储）：支持子线程读取（异步场景），由 `clear()` 在 finally 中清理

写入时同时写入两个存储；读取时优先 ThreadLocal，回落到 request attribute。

### 方法列表

| 方法                  | 参数                                                                     | 返回值                  | 说明                                                 |
|---------------------|------------------------------------------------------------------------|----------------------|----------------------------------------------------|
| `cache`             | `HttpServletRequest request`, `Principal principal`, `Session session` | `void`               | 缓存 Principal + Session 到当前请求上下文（过滤器调用）             |
| `clear`             | 无                                                                      | `void`               | 清理 ThreadLocal 上下文（请求结束时调用，防内存泄漏，**必须 finally**）   |
| `getPrincipal`      | 无                                                                      | `Principal` / `null` | 获取当前 Principal；无上下文返回 null                         |
| `getSession`        | 无                                                                      | `Session` / `null`   | 获取当前 Session；无上下文返回 null                           |
| `getUserCode`       | 无                                                                      | `String` / `null`    | 获取当前用户编码（从 Principal）                              |
| `getUsername`       | 无                                                                      | `String` / `null`    | 获取当前用户名（从 Principal）                               |
| `getNickname`       | 无                                                                      | `String` / `null`    | 获取当前昵称（从 Principal）                                |
| `getTenantCode`     | 无                                                                      | `String` / `null`    | 获取当前租户编码（**从请求头 tenant-code**，动态值，不属于用户身份）         |
| `getAppCode`        | 无                                                                      | `String` / `null`    | 获取当前应用编码（从请求头 app-code）                            |
| `getToken`          | 无                                                                      | `String` / `null`    | 获取当前 token（从请求头 Authorization 或 token，去 Bearer 前缀） |
| `getAuthIdentifier` | 无                                                                      | `String` / `null`    | 获取当前认证标识符（从 Session）                               |
| `match`             | `String pattern`, `String uri`                                         | `boolean`            | 路径匹配（Ant 风格，基于 `AntPathMatcher`）                   |

### 与 sh-core UserContext 的关系

`PrincipalContext` 替代 `UserContext` 的用户信息读取职责：

- `UserContext`（sh-core）：基于 ThreadLocal\<UserInfo\>，仅在线程内有效
- `PrincipalContext`：双存储，request 主 + ThreadLocal 辅，支持异步子线程读取，且能从请求头读取 tenantCode/appCode/token
  等运行时值

在引入 IAM 契约层的项目中，业务代码应优先使用 `PrincipalContext` 读取用户信息。

## ContractSettings — 静态配置持有器

位于 `com.wkclz.iam.contract.config.ContractSettings`。`final` 类，私有构造器，全部静态字段 + 静态 getter/setter。

### 5 个静态字段

| 字段           | 类型     | 说明                      | 对应配置项                         |
|--------------|--------|-------------------------|-------------------------------|
| appId        | String | AK 签名 appId             | `iam.contract.app-id`         |
| appSecret    | String | AK 签名 appSecret（RSA 私钥） | `iam.contract.app-secret`     |
| publicKey    | String | AK 验签 publicKey（RSA 公钥） | `iam.contract.public-key`     |
| serverUrl    | String | SSO 服务端地址               | `iam.contract.server-url`     |
| jwtSecretKey | String | JWT 密钥（供实现层使用）          | `iam.contract.jwt-secret-key` |

> 配置项的默认值、注入位置与详细说明见 [sh-iam-contract-default](../sh-iam-contract-default/SKILL.md) skill 的"##
> 配置项总表"章节。

### 为何用静态持有器

契约接口的 `default` 方法（如 `AkSignContract.sign()`、`AkSignContract.verifySign(request)`、`SsoFacadeContract.logout()`
）无法通过 `@Autowired` 访问 Spring 上下文，因此通过静态持有器桥接配置。

### 初始化时机

由 iam-contract-default 模块的 `IamContractAutoConfig`（或业务方自行实现的配置类）在 `@PostConstruct` 中调用 setter
同步配置。详见 [sh-iam-contract-default](../sh-iam-contract-default/SKILL.md) skill。

## AuthException — 契约层统一异常

位于 `com.wkclz.iam.contract.exception.AuthException`。继承 `RuntimeException`，使用 `@Getter`，持有 `errorType` 字段。

### 构造器

```java
public AuthException(AuthErrorType errorType, String message)
public AuthException(AuthErrorType errorType, String message, Throwable cause)
```

### AuthErrorType 枚举（8 个值）

位于 `com.wkclz.iam.contract.enums.AuthErrorType`。枚举内携带 HTTP 状态码（httpStatus）与友好提示语（message）。

| 枚举值               | HTTP 状态码 | 友好提示      | 说明                 |
|-------------------|----------|-----------|--------------------|
| `TOKEN_MISSING`   | 401      | token 不存在 | token 不存在          |
| `TOKEN_INVALID`   | 401      | JWT 签名无效  | JWT 签名无效           |
| `TOKEN_EXPIRED`   | 401      | JWT 已过期   | JWT 已过期            |
| `SESSION_EXPIRED` | 401      | 会话已过期     | 会话已过期（如 Redis 无记录） |
| `AK_SIGN_INVALID` | 401      | AK 签名无效   | AK 签名无效            |
| `AK_SIGN_EXPIRED` | 401      | AK 签名已过期  | AK 签名已过期           |
| `AK_NONCE_REPLAY` | 401      | 重放检测命中    | nonce 重放检测命中       |
| `ACCESS_DENIED`   | 403      | 接口鉴权拒绝    | 接口鉴权拒绝             |

DefaultAuthFilter 根据 `e.getErrorType().getHttpStatus()` 设置响应状态码：401 类错误返回 401 Unauthorized，ACCESS_DENIED
返回 403 Forbidden。友好提示通过 `getMessage()` 获取。

提供 `fromJwtErrorCode(String)` 静态方法，将 JWT 错误码映射为对应的 AuthErrorType，减少实现方样板代码：

```java
// 实现方捕获 JWT 异常后直接使用
throw new AuthException(AuthErrorType.fromJwtErrorCode(e.getErrorCode()), e.getMessage(), e);
```

映射规则：

- `JwtErrorCodes.EXPIRED` → `TOKEN_EXPIRED`
- `JwtErrorCodes.SIGNATURE` / `MALFORMED` / `UNSUPPORTED` / `ILLEGAL_ARGUMENT` → `TOKEN_INVALID`
- `null` / 无法识别 → `TOKEN_INVALID`（兜底）

详见 [JwtErrorCodes](file:///d:/code/sh-framework/sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/enums/JwtErrorCodes.java)。

### 使用方式

```java
import com.wkclz.iam.contract.exception.AuthException;

// 抛出（无 cause）
throw new AuthException(AuthErrorType.TOKEN_EXPIRED, "Token 已过期");
// 抛出（带 cause）
throw new AuthException(AuthErrorType.TOKEN_INVALID, "签名无效", e);
// 捕获并判断类型
try {
    authzContract.canAccessApi(uri, method);
} catch (AuthException e) {
    AuthErrorType type = e.getErrorType();
    // 根据 type 处理
}
```

## AuthScene 枚举

位于 `com.wkclz.iam.contract.enums.AuthScene`。鉴权场景枚举。

| 枚举值       | 说明           |
|-----------|--------------|
| `TOKEN`   | JWT Token 认证 |
| `AK_SIGN` | AK 签名认证      |
| `PUBLIC`  | 公开接口（无需认证）   |

## LoginFailType 枚举

位于 `com.wkclz.iam.contract.enums.LoginFailType`。登录失败类型枚举，*
*纯枚举 + `private final String message` + `getMessage()`**，枚举内完成翻译，不带数字 code（保持契约层中性定位）。由
`SsoFacadeContract` 实现方在登录失败时选用，通过 `LoginResp.fail()` 返回。

共 10 个值：

| 枚举值                          | 中文 message | 覆盖场景                                   |
|------------------------------|------------|----------------------------------------|
| `USERNAME_OR_PASSWORD_ERROR` | 用户名或密码错误   | 密码登录 / LDAP 49 凭据无效；用户名错误与密码错误合并，防用户枚举 |
| `ACCOUNT_DISABLED`           | 账号已禁用      | LDAP 533 账号禁用 / 管理员停用                  |
| `ACCOUNT_LOCKED`             | 账号已锁定      | LDAP 775 登录次数超限锁定 / 人工锁定               |
| `CREDENTIALS_EXPIRED`        | 凭据已过期      | LDAP 532/773 密码过期需修改                   |
| `CAPTCHA_REQUIRED`           | 需要验证码      | 风控触发要求图形 / 短信验证码                       |
| `CAPTCHA_ERROR`              | 验证码错误      | 图形 / 短信 / 邮箱验证码校验失败                    |
| `TENANT_INVALID`             | 租户无效       | 租户不存在或已停用                              |
| `AUTH_TYPE_UNSUPPORTED`      | 认证类型不支持    | authType 不在支持列表                        |
| `AUTH_IDENTIFIER_INVALID`    | 三方标识无效     | authIdentifier 对应的三方账号无效               |
| `UNKNOWN`                    | 登录失败       | 兜底，无法归类的失败                             |

### 设计要点

- **枚举内完成翻译**：`getMessage()` 返回中文含义，前端可直接展示
- **不带数字 code**：保持契约层中性定位，避免与 HTTP 状态码 / 业务码混淆
- **USERNAME_OR_PASSWORD_ERROR 合并用户名与密码错误**：防用户枚举攻击（安全最佳实践）
- **UNKNOWN 兜底**：无法归类的失败统一归入此值
- **failReason 提供运行时补充**：`LoginResp.fail(type, reason)` 的 reason 为动态详情（如"请 300 秒后重试"），前端优先展示
  failReason，为空时回退到 failType.getMessage()

## 集成指南

### 引入依赖

```xml
<dependency>
    <groupId>com.wkclz.framework</groupId>
    <artifactId>iam-contract-api</artifactId>
    <version>${revision}</version>
</dependency>
```

> 单独引入此模块不会注册任何 Bean。若需快速启动，请同时引入 `iam-contract-default`（见"启用 default 模块快速启动"）。

### 请求头约定

业务系统需约定以下请求头（由前端或网关注入）：

| 请求头             | 必填 | 说明                               | PrincipalContext 读取方法                     |
|-----------------|----|----------------------------------|-------------------------------------------|
| `Authorization` | 否  | `Bearer {token}` 格式              | `getToken()`                              |
| `token`         | 否  | 备用 token 字段（Authorization 缺失时使用） | `getToken()`                              |
| `tenant-code`   | 否  | 租户编码（动态切换值）                      | `getTenantCode()`                         |
| `app-code`      | 否  | 应用编码                             | `getAppCode()`                            |
| `sign`          | 否  | AK 签名（RPC 场景）                    | 由 `AkSignContract.verifySign(request)` 读取 |
| `app-id`        | 否  | AK 签名 appId（RPC 场景）              | 由 `AkSignContract.verifySign(request)` 读取 |

> token 提取规则：优先 `Authorization`，缺失时回退 `token`；去除 `Bearer ` 前缀。

### 读取当前用户

```java
// 在 Controller / Service / 任意请求线程内
Principal principal = PrincipalContext.getPrincipal();
String userCode = PrincipalContext.getUserCode();
String username = PrincipalContext.getUsername();
String tenantCode = PrincipalContext.getTenantCode();
String appCode = PrincipalContext.getAppCode();
String token = PrincipalContext.getToken();
```

### 调用契约 SPI

通过 `@Autowired` 注入契约接口，由 Spring 容器解析到对应实现（默认实现或业务方自定义实现）：

```java
@Autowired
private AuthContract authContract;          // 认证
@Autowired
private AuthzContract authzContract;        // 鉴权
@Autowired
private AkSignContract akSignContract;      // AK 签名
@Autowired
private SsoFacadeContract ssoFacadeContract; // SSO 门面
```

### 启用 default 模块快速启动

引入 `iam-contract-default` 即可获得四契约的默认实现（读宽容验证严格）+ DefaultAuthFilter +
IamContractAutoConfig。详见 [sh-iam-contract-default](../sh-iam-contract-default/SKILL.md) skill。

```xml
<dependency>
    <groupId>com.wkclz.framework</groupId>
    <artifactId>iam-contract-default</artifactId>
    <version>${revision}</version>
</dependency>
```

业务方声明同类型 `@Component` Bean 即可让默认实现失效（`@ConditionalOnMissingBean` 替换机制）。

## 完整示例代码（基于 sh-framework 生态）

### 示例 1：业务 Controller 读取当前用户

```java
@Slf4j
@RestController
@RequestMapping(Route.PREFIX)
public class OrderRest {
    @Autowired
    private OrderService orderService;

    @GetMapping("/order/list")
    @Operation(summary = "查询当前用户的订单")
    public R<List<OrderResp>> list() {
        Principal principal = PrincipalContext.getPrincipal();
        String tenantCode = PrincipalContext.getTenantCode();
        log.info("用户 {} 查询订单列表, tenant={}", principal.getUsername(), tenantCode);
        return R.ok(orderService.listByUserCode(principal.getUserCode()));
    }
}
```

### 示例 2：Service 调用鉴权 SPI（接口鉴权 + 字段过滤）

```java
@Service
public class OrderService extends BaseService<Order, OrderMapper> {
    @Autowired
    private AuthzContract authzContract;

    public void deleteOrder(Long id) {
        boolean canAccess = authzContract.canAccessApi("/api/order/delete", "DELETE");
        if (!canAccess) {
            throw UnauthorizedException.of("无权删除订单");
        }
        // ...
    }

    public List<String> listVisibleFields(String menuCode, List<String> allFields) {
        return authzContract.filterFields(menuCode, allFields);
    }
}
```

### 示例 3：RPC 客户端调用 AK 签名

```java
@Component
public class RemoteApiClient {
    @Autowired
    private AkSignContract akSignContract;

    public String callRemote(String body) {
        String sign = akSignContract.sign(); // 从 ContractSettings 获取 appId/appSecret
        // 放入请求头 sign 字段，app-id 字段放 appId
        // HTTP 客户端由业务方选择（RestTemplate / OkHttp / HttpClient 等）
        return sign;
    }
}
```

### 示例 4：自定义过滤器缓存 Principal（不使用 DefaultAuthFilter 的场景）

```java
@Component
public class CustomAuthFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            Principal principal = parseJwt(PrincipalContext.getToken());
            Session session = buildSession(principal);
            PrincipalContext.cache(request, principal, session);
            chain.doFilter(request, response);
        } finally {
            PrincipalContext.clear(); // 必须 finally 清理
        }
    }
}
```

### 示例 5：错误处理（捕获 AuthException）

```java
try {
    authzContract.canAccessApi(uri, method);
} catch (AuthException e) {
    if (e.getErrorType() == AuthErrorType.ACCESS_DENIED) {
        // 鉴权拒绝处理
    }
}
```

## 注意事项

1. **api 模块不含实现**：单独引入 `iam-contract-api` 不会注册任何 Bean，必须配合 `iam-contract-default` 或自行实现契约接口并通过
   `@Component` 注册
2. **PrincipalContext.clear() 必须 finally**：在过滤器或请求入口的 finally 块中调用，防止 ThreadLocal 内存泄漏
3. **tenantCode / appCode 不属于 Principal**：这两个值是运行时动态切换的，从请求头读取（`getTenantCode()` / `getAppCode()`
   ），不应放入 Principal
4. **请求头由前端或网关注入**：业务系统需保证 `Authorization` / `token` / `tenant-code` / `app-code` / `sign` / `app-id`
   等请求头由前端或 API 网关注入
5. **default 方法依赖 ContractSettings**：`AkSignContract.sign()` / `AkSignContract.verifySign(request)` /
   `SsoFacadeContract.logout()` 等 default 方法依赖 `ContractSettings` 静态字段，必须确保 `ContractConfig` 的
   `@PostConstruct` 已执行
6. **替代 UserContext**：引入 IAM 契约层后，业务代码应优先使用 `PrincipalContext` 而非 sh-core 的 `UserContext` 读取用户信息
