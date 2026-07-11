# sh-auth 公共认证-授权-鉴权模块设计方案

## 1. 概述

### 1.1 背景

当前 IAM 系统的认证、授权、鉴权逻辑分散在 `iam-sdk`、`iam-sso`、`iam-admin` 三个模块中，彼此耦合且职责边界模糊。例如登录请求对象（`LoginReq`）和验证码工具（`CaptchaHelper`）被放置在 `iam-sdk` 中，而实际只有 `iam-sso` 使用它们；鉴权逻辑（`JwtAuthContract`）直接依赖 Redis，导致 SDK 与基础设施紧耦合。

### 1.2 目标

建设 `sh-auth` — 一个纯框架级、零 IAM 依赖的公共认证-授权-鉴权模块。通过 SPI 契约机制，支持多种认证方式（密码/短信/第三方 OAuth）和多种授权体系（IAM 角色菜单 / 小程序权限表 / 第三方策略引擎）的可插拔接入。

### 1.3 核心原则

- **零 IAM 依赖**：`sh-auth` 仅依赖 `sh-web`、`sh-redis`、servlet-api、slf4j，不引用任何 `iam-*` 模块
- **SPI 可插拔**：所有能力通过契约接口定义，接入方实现接口即可接入
- **一期范围**：仅设计 `sh-auth` 模块本身，不涉及现有 IAM 代码改造（IAM 改造为二期）
- **覆盖完整**：认证方式覆盖密码/短信验证码/第三方 OAuth/MFA；授权覆盖 API 级/菜单级/数据级/字段级四级

---

## 2. 模块定位与依赖

### 2.1 定位

`sh-auth` 是框架级公共模块，与 `sh-web`、`sh-redis`、`sh-mybatis` 同级，由 `sh-parent` BOM 统一管理版本。

### 2.2 Maven 坐标

```xml
<groupId>com.wkclz.framework</groupId>
<artifactId>sh-auth</artifactId>
<version>5.1.0-SNAPSHOT</version>
```

### 2.3 依赖关系

```
sh-auth
├── sh-web           (IpHelper, RequestHelper, R 响应封装)
├── sh-redis         (RedisTemplate, 提供 SessionStore 默认实现; 标记为 optional, 不使用 Redis 的可排除)
├── jakarta.servlet-api  (HttpServletRequest/Response, Filter)
├── slf4j-api            (日志门面)
└── spring-boot-autoconfigure  (自动配置)
```

**硬约束**：不得出现 `com.wkclz.iam` 包下的任何 import。

### 2.4 与现有模块的关系（二期规划参考）

```
sh-auth (框架公共层)
  ↑ 实现 SPI
  ├── iam-sso   → 实现 AuthenticationProvider(JWT), SessionStore(Redis), MfaService(SMS), ...
  ├── iam-admin → 实现 AccessControlProvider(RBAC), MenuProvider, DataScopeProvider, ...
  └── iam-sdk   → 不实现 SPI, 仅引入 sh-auth 获得过滤器链 + SecurityContext
```

---

## 3. 包结构

```
com.wkclz.auth/
├── ShAuthAutoConfiguration.java          # @AutoConfiguration 自动配置入口（唯一）
├── config/
│   ├── AuthProperties.java               # 配置属性类
│   └── AuthConstants.java                # 常量定义
├── context/
│   └── SecurityContext.java              # ThreadLocal 安全上下文
├── contract/
│   ├── auth/                             # 认证域 SPI
│   │   ├── AuthenticationProvider.java   # 认证提供者（核心扩展点）
│   │   ├── TokenService.java             # Token 生命周期管理
│   │   ├── LoginService.java             # 登录编排模板方法
│   │   ├── LogoutService.java            # 登出接口
│   │   ├── DefaultLogoutService.java      # 登出默认实现
│   │   ├── PasswordEncoder.java          # 密码编码器
│   │   ├── PasswordValidator.java        # 密码策略校验器
│   │   ├── CredentialProvider.java       # 凭证提供者 SPI
│   │   ├── SmsCodeSender.java            # 短信验证码发送
│   │   ├── OAuthProvider.java            # 第三方 OAuth 接入 SPI
│   │   ├── CaptchaService.java           # 图形验证码服务
│   │   ├── MfaService.java               # MFA 服务
│   │   ├── AccountStatusChecker.java     # 账号状态检查
│   │   ├── SessionStore.java             # 会话存储
│   │   ├── ConcurrentSessionControl.java # 并发会话控制
│   │   └── RateLimitChecker.java         # 登录频率限制
│   ├── authz/                            # 授权域 SPI
│   │   ├── AccessControlProvider.java    # API 级访问控制
│   │   ├── MenuProvider.java             # 菜单/资源树提供者
│   │   ├── DataScopeProvider.java        # 数据权限范围提供者
│   │   └── FieldPermissionProvider.java  # 字段级权限提供者
│   └── infra/                            # 基础设施 SPI
│       ├── RequestLogger.java            # 请求日志记录器
│       └── SecurityHeaderProvider.java   # HTTP 安全头提供者
├── filter/
│   ├── RequestWrapperFilter.java         # 请求体可重复读取包装
│   ├── RequestLogFilter.java             # 请求日志采集（最外层，确保异常请求也记录）
│   ├── SecurityHeaderFilter.java         # HTTP 安全头注入
│   ├── AuthenticationFilter.java         # 认证过滤器
│   └── AuthorizationFilter.java          # 鉴权过滤器
├── model/
│   ├── Principal.java                    # 用户主体
│   ├── Subject.java                      # 认证主体（账号实体）
│   ├── Credential.java                   # 凭据抽象
│   ├── Role.java                         # 角色
│   ├── Permission.java                   # 权限定义
│   ├── MenuNode.java                     # 菜单节点（树形）
│   ├── ApiResource.java                  # API 资源
│   ├── DataScope.java                    # 数据权限范围
│   ├── FieldPermission.java              # 字段权限
│   ├── Session.java                      # 会话
│   ├── AuthToken.java                    # 认证令牌抽象
│   ├── AuthRequest.java                  # 认证请求
│   ├── AuthResult.java                   # 认证结果
│   ├── Captcha.java                      # 验证码模型
│   ├── LoginRecord.java                  # 登录日志模型
│   ├── RequestRecord.java                # 请求日志模型
│   ├── SecurityHeaders.java              # HTTP 安全头
│   ├── MfaChallenge.java                 # MFA 挑战
│   ├── SubjectRole.java                  # 用户-角色关联
│   ├── RoleDataScope.java                # 角色-数据权限关联
│   ├── ApiField.java                     # API-字段权限关联
│   ├── AuthMetadata.java                 # 应用级 RBAC 缓存快照（单例）
│   └── SubjectAuthorization.java         # 用户级授权缓存快照（轻量）
├── exception/
│   ├── AuthenticationException.java      # 认证异常
│   ├── AuthorizationException.java       # 授权异常
│   ├── SessionExpiredException.java      # 会话过期异常
│   ├── AccountStatusException.java       # 账号状态异常
│   ├── MfaRequiredException.java         # MFA 要求异常
│   └── RateLimitException.java           # 频率限制异常
└── enums/
    ├── AuthErrorType.java                # 认证错误类型
    ├── AuthStatus.java                   # 认证状态
    ├── AccountStatus.java                # 账号状态枚举
    ├── TokenType.java                    # Token 类型
    ├── CredentialType.java               # 凭证类型
    ├── MfaType.java                      # MFA 类型
    ├── MenuType.java                     # 菜单类型
    └── FieldPermissionType.java          # 字段权限类型
```

---

## 4. 数据模型

### 4.1 Principal — 用户主体

```java
public class Principal implements Serializable {
    private String userCode;      // 用户唯一标识（通用于各种体系）
    private String username;      // 用户名
    private String nickname;      // 昵称
    private String avatar;        // 头像 URL
    private String tenantCode;    // 租户编码（多租户支持）
    private String appCode;       // 当前应用编码
    private String authIdentifier;// 认证标识（用户名/手机号/openId，按 authType 区分含义，可选）
}
```

### 4.2 Subject — 认证主体

```java
public class Subject implements Serializable {
    private String subjectId;         // 主体 ID
    private String authType;          // 认证类型（PASSWORD / LDAP / SMS / SOCIAL）
    private String authIdentifier;    // 认证标识（手机号/邮箱/用户名/unionId）
    private AccountStatus status;     // 账号状态
    private LocalDateTime expireTime; // 账号过期时间
}
```

### 4.3 Credential — 凭据

```java
public class Credential implements Serializable {
    private CredentialType type;      // PASSWORD / SMS_CODE / SOCIAL_CODE
    private String credentialValue;   // 密码原文 / 短信验证码 / OAuth code
    private String captchaCode;       // 图形验证码（登录时携带）
    private String captchaId;         // 验证码 ID
}
```

### 4.4 AuthRequest — 认证请求

```java
public class AuthRequest implements Serializable {
    private String authType;          // 指定的认证方式（null 时自动探测）
    private Credential credential;    // 凭据
    private Map<String, Object> extra; // 扩展参数（如微信 code、state 等）
}
```

### 4.5 AuthResult — 认证结果

```java
public class AuthResult implements Serializable {
    private AuthStatus status;        // 整体认证状态（SUCCESS / MFA_REQUIRED / FAILED）
    private AuthErrorType errorType;  // 失败时的错误类型
    private String errorMessage;      // 失败时的错误描述
    private AuthToken token;          // 成功时的认证令牌
    private Principal principal;      // 成功时的用户主体
    private MfaChallenge mfaChallenge;// MFA 挑战（status=MFA_REQUIRED 时）

    public boolean isSuccess() {
        return status == AuthStatus.SUCCESS;
    }

    public static AuthResult success(Principal principal, AuthToken token) {
        AuthResult r = new AuthResult();
        r.status = AuthStatus.SUCCESS;
        r.principal = principal;
        r.token = token;
        return r;
    }

    public static AuthResult fail(AuthErrorType errorType, String message) {
        AuthResult r = new AuthResult();
        r.status = AuthStatus.FAILED;
        r.errorType = errorType;
        r.errorMessage = message;
        return r;
    }
}
```

### 4.6 AuthToken — 认证令牌

```java
public class AuthToken implements Serializable {
    private TokenType type;           // JWT / OAUTH_BEARER / SESSION_ID
    private String tokenValue;        // 令牌值
    private LocalDateTime expireTime; // 过期时间
    private LocalDateTime issueTime;  // 签发时间
}
```

### 4.7 Session — 会话

```java
public class Session implements Serializable {
    private String sessionId;         // 会话 ID
    private String subjectId;         // 关联的主体 ID
    private Principal principal;      // 主体信息
    private String authType;          // 认证方式
    private String authIdentifier;    // 认证标识
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime expireTime; // 过期时间
    private String clientIp;          // 登录 IP
    private String userAgent;         // 登录 UA
}
```

### 4.8 MfaChallenge — MFA 挑战

```java
public class MfaChallenge implements Serializable {
    private String challengeId;       // 挑战 ID
    private MfaType mfaType;          // SMS / TOTP
    private String target;            // 目标（手机号 / 无，TOTP 不需要）
    private LocalDateTime expireTime; // 过期时间
    private boolean used;             // 是否已使用
}
```

### 4.9 Captcha — 图形验证码

```java
public class Captcha implements Serializable {
    private String captchaId;         // 验证码 ID
    private String captchaCode;       // 验证码明文
    private String captchaImage;      // Base64 data URI 图片
    private LocalDateTime expireTime; // 过期时间（默认 5 分钟）
}
```

### 4.10 Permission — 权限定义

```java
public class Permission implements Serializable {
    private String resource;          // 资源标识（如 apiUri）
    private String action;            // 操作类型（GET / POST / PUT / DELETE / READ / WRITE）
    private String description;       // 权限描述
}
```

### 4.11 Role — 角色

```java
public class Role implements Serializable {
    private String roleCode;          // 角色编码（唯一）
    private String roleName;          // 角色名称
    private String parentCode;        // 父角色编码（树形结构）
    private String appCode;           // 所属应用
    private String tenantCode;        // 租户编码
    private Boolean applicable;       // 是否可申请（仅作用于树节点展示）
}
```

### 4.12 MenuNode — 菜单节点

```java
public class MenuNode implements Serializable {
    private String menuCode;          // 菜单编码
    private String parentCode;        // 父菜单编码
    private String menuName;          // 菜单名称
    private MenuType menuType;        // MENU / BUTTON
    private String routePath;         // 前端路由路径
    private String component;         // 前端组件
    private String buttonCode;        // 按钮权限标识
    private String appCode;           // 所属应用
    private Integer sort;             // 排序
    private List<MenuNode> children;  // 子节点（构建树时填充）
}
```

### 4.13 ApiResource — API 资源

```java
public class ApiResource implements Serializable {
    private String apiCode;           // API 编码
    private String apiMethod;         // HTTP 方法（GET/POST/PUT/DELETE）
    private String apiUri;            // URI 路径
    private String apiName;           // API 名称
    private String module;            // 所属模块
    private String appCode;           // 所属应用
    private Boolean writeFlag;        // 是否写操作
}
```

### 4.14 DataScope — 数据权限范围

```java
public class DataScope implements Serializable {
    private String dimensionCode;     // 维度编码（如 dept_id）
    private String dimensionName;     // 维度名称（如 部门ID）
    private String scopeValue;        // 范围值（具体值或表达式）
    private String appCode;           // 所属应用
}
```

### 4.15 FieldPermission — 字段权限

```java
public class FieldPermission implements Serializable {
    private String fieldName;         // 字段名
    private FieldPermissionType type; // READ / WRITE / HIDDEN
    private String apiCode;           // 所属 API
}
```

### 4.16 关联关系模型

#### SubjectRole — 用户-角色关联

```java
public class SubjectRole implements Serializable {
    private String subjectId;         // 主体 ID
    private String roleCode;          // 角色编码
    private LocalDateTime startTime;  // 生效开始时间
    private LocalDateTime endTime;    // 生效结束时间
    private Boolean enableStatus;     // 启用状态
}
```

#### RoleDataScope — 角色-数据权限关联

```java
public class RoleDataScope implements Serializable {
    private String roleCode;          // 角色编码
    private String dimensionCode;     // 维度编码
    private String scopeValue;        // 范围值
}
```

#### ApiField — API-字段权限关联

```java
public class ApiField implements Serializable {
    private String apiCode;           // API 编码
    private String fieldName;         // 字段名
    private FieldPermissionType type; // 权限类型
}
```

### 4.17 缓存模型

#### AuthMetadata — 应用级 RBAC 缓存快照

单例，每应用仅一份，启动时加载，配置变更时刷新。所有用户共享。

```java
public class AuthMetadata implements Serializable {
    private String appCode;                              // 所属应用
    private Map<String, Role> roles;                     // roleCode → Role
    private Map<String, MenuNode> menus;                 // menuCode → MenuNode（已构建为树）
    private Map<String, ApiResource> apis;               // apiCode → ApiResource
    private Map<String, List<String>> roleMenus;         // roleCode → menuCodes
    private Map<String, List<String>> menuApis;          // menuCode → apiCodes
    private Map<String, List<ApiField>> apiFields;       // apiCode → 字段权限列表
    private LocalDateTime loadTime;                      // 加载时间
}
```

#### SubjectAuthorization — 用户级授权缓存快照

每用户一份，体积极小（仅角色编码列表 + 数据权限），按需加载。

```java
public class SubjectAuthorization implements Serializable {
    private String subjectId;                                  // 主体 ID
    private List<SubjectRole> roles;                           // 角色列表（含有效期）
    private Map<String, List<RoleDataScope>> roleDataScopes;   // roleCode → 数据权限范围
    private LocalDateTime loadTime;                            // 加载时间
}
```

**鉴权流程**（`AccessControlProvider` 实现方伪代码）：

```
hasPermission(principal, method, uri):
    AuthMetadata meta = metadataCache.get(principal.appCode);        // 单例
    SubjectAuthorization user = userAuthCache.get(principal.userCode); // 轻量

    for (SubjectRole sr : user.roles):
        if 过期 or 禁用 → skip
        for (menuCode in meta.roleMenus[sr.roleCode]):
            for (apiCode in meta.menuApis[menuCode]):
                if meta.apis[apiCode] matches (method, uri) → true
    → false
```

### 4.18 日志模型

#### LoginRecord — 登录日志

```java
public class LoginRecord implements Serializable {
    private String id;                // 记录 ID
    private String subjectId;         // 主体 ID
    private String username;          // 用户名
    private String authType;          // 认证方式
    private String clientIp;          // 登录 IP
    private String userAgent;         // UA
    private Boolean success;          // 是否成功
    private String failReason;        // 失败原因（失败时）
    private LocalDateTime loginTime;  // 登录时间
}
```

#### RequestRecord — 请求日志

```java
public class RequestRecord implements Serializable {
    private String id;                // 记录 ID
    private String requestUri;        // 请求 URI
    private String requestMethod;     // 请求方法
    private String userId;            // 用户 ID（已认证时）
    private String username;          // 用户名（已认证时）
    private String clientIp;          // 客户端 IP
    private String userAgent;         // UA
    private String requestBody;       // 请求体（脱敏后）
    private Integer responseStatus;   // 响应状态码
    private String responseBody;      // 响应体（截断后）
    private Long costTime;            // 耗时(ms)
    private LocalDateTime requestTime;// 请求时间
    private String authType;          // 认证方式
    private String exception;         // 异常信息（如有）
}
```

### 4.18 SecurityHeaders — HTTP 安全头

```java
public class SecurityHeaders implements Serializable {
    private String contentSecurityPolicy;
    private String xFrameOptions;
    private String xContentTypeOptions;
    private String strictTransportSecurity;
    private String xXssProtection;
    private String referrerPolicy;
}
```

---

## 5. 契约接口

### 5.1 认证域

#### AuthenticationProvider — 认证提供者 SPI

```java
public interface AuthenticationProvider {
    /**
     * 执行认证，返回 AuthResult
     * @param request 认证请求（包含凭据类型和凭据值）
     * @param httpRequest 原始 HTTP 请求（可选，OAuth 回调用）
     * @return 认证结果
     */
    AuthResult authenticate(AuthRequest request, HttpServletRequest httpRequest);

    /**
     * 支持的认证方式列表，用于 LoginService 自动探测
     */
    List<String> supportedAuthTypes();

    /**
     * 优先级，数值越小越优先（默认 0）
     */
    default int getOrder() { return 0; }
}
```

#### TokenService — Token 生命周期管理

```java
public interface TokenService {
    /** 根据 Principal 生成 Token */
    AuthToken generateToken(Principal principal);
    /** 解析 Token 获取 Principal */
    Principal parseToken(String tokenValue);
    /** 验证 Token 是否有效 */
    boolean validateToken(String tokenValue);
    /** 刷新 Token */
    AuthToken refreshToken(String tokenValue);
    /** 获取 Token 类型 */
    TokenType getTokenType();
}
```

#### LoginService — 登录编排

```java
public abstract class LoginService {

    // === 注入的 SPI ===
    protected AuthenticationProvider authProvider;
    protected TokenService tokenService;
    protected SessionStore sessionStore;
    protected RateLimitChecker rateLimitChecker;
    protected CaptchaService captchaService;
    protected AccountStatusChecker accountStatusChecker;
    protected MfaService mfaService;
    protected ConcurrentSessionControl concurrentSessionControl;

    /** 模板方法：统一登录流程 */
    public AuthResult login(AuthRequest request, HttpServletRequest httpRequest) {
        String identifier = resolveIdentifier(request, httpRequest);
        try {
            // 1. 登录频率检查
            checkRateLimit(request, httpRequest);

            // 2. 图形验证码校验
            checkCaptcha(request);

            // 3. 调用 AuthenticationProvider 执行认证
            AuthResult result = authProvider.authenticate(request, httpRequest);
            if (!result.isSuccess()) {
                rateLimitChecker.recordAttempt(identifier, false);
                recordLoginLog(result, httpRequest);
                return result;
            }

            // 4. 账号状态检查
            accountStatusChecker.checkStatus(result.getPrincipal().getUserCode());

            // 5. MFA 二次验证（如需要）
            MfaChallenge challenge = checkMfa(result.getPrincipal());
            if (challenge != null) {
                result.setStatus(AuthStatus.MFA_REQUIRED);
                result.setMfaChallenge(challenge);
                return result;
            }

            // 6. 生成 Token 并创建会话
            AuthToken token = tokenService.generateToken(result.getPrincipal());
            result.setToken(token);
            result.setStatus(AuthStatus.SUCCESS);

            Session session = createSession(result.getPrincipal(), token, httpRequest);
            concurrentSessionControl.enforce(session.getSubjectId());
            sessionStore.save(session);

            // 7. 记录登录日志
            rateLimitChecker.recordAttempt(identifier, true);
            recordLoginLog(result, httpRequest);

            return result;
        } catch (AuthenticationException e) {
            log.warn("认证失败: {}", e.getMessage());
            rateLimitChecker.recordAttempt(identifier, false);
            return AuthResult.fail(AuthErrorType.BAD_CREDENTIALS, e.getMessage());
        } catch (RateLimitException e) {
            log.warn("登录频率超限: {}", e.getMessage());
            return AuthResult.fail(AuthErrorType.RATE_LIMITED, e.getMessage());
        } catch (AccountStatusException e) {
            log.warn("账号状态异常: {}", e.getMessage());
            return AuthResult.fail(AuthErrorType.ACCOUNT_DISABLED, e.getMessage());
        } catch (Exception e) {
            log.error("登录异常", e);
            return AuthResult.fail(AuthErrorType.INTERNAL_ERROR, e.getMessage());
        }
    }

    /** 解析限流标识（默认取 IP，子类可覆盖为用户名） */
    protected String resolveIdentifier(AuthRequest request, HttpServletRequest httpRequest) {
        return IpHelper.getClientIp(httpRequest);
    }

    /** 子步骤可由子类覆盖 */
    protected abstract void checkRateLimit(AuthRequest request, HttpServletRequest httpRequest);
    protected abstract void checkCaptcha(AuthRequest request);
    protected abstract MfaChallenge checkMfa(Principal principal);
    protected abstract Session createSession(Principal principal, AuthToken token, HttpServletRequest httpRequest);
    protected abstract void recordLoginLog(AuthResult result, HttpServletRequest httpRequest);
}
```

#### LogoutService — 登出

```java
public interface LogoutService {
    /** 登出当前会话 */
    void logout();
    /** 踢出指定会话 */
    void logout(String sessionId);
    /** 使某用户所有会话失效（改密场景） */
    void invalidateAllSessions(String subjectId);
}
```

**DefaultLogoutService — 默认登出实现**

sh-auth 提供基于 `SessionStore` 的默认实现，接入方无需重复编写注销逻辑：

```java
public class DefaultLogoutService implements LogoutService {
    private final SessionStore sessionStore;

    @Override
    public void logout() {
        String token = SecurityContext.getToken();
        if (token != null) {
            sessionStore.delete(token);
        }
    }

    @Override
    public void logout(String sessionId) {
        sessionStore.delete(sessionId);
    }

    @Override
    public void invalidateAllSessions(String subjectId) {
        sessionStore.deleteBySubjectId(subjectId);
    }
}
```

#### PasswordEncoder — 密码编码器

```java
public interface PasswordEncoder {
    /** 编码密码 */
    String encode(String rawPassword);
    /** 校验密码 */
    boolean matches(String rawPassword, String encodedPassword);
}
```

#### PasswordValidator — 密码策略校验器

```java
public interface PasswordValidator {
    /** 校验密码强度 */
    void validateStrength(String password);
    /** 检查是否在历史密码中 */
    boolean isHistoryPassword(String subjectId, String password);
    /** 检查密码是否过期 */
    boolean isExpired(String subjectId);
    /** 获取密码过期剩余天数 */
    long getRemainingDays(String subjectId);
}
```

#### CredentialProvider — 凭证提供者 SPI

```java
public interface CredentialProvider {
    /** 支持的凭证类型 */
    CredentialType supportedType();
    /** 校验凭证，返回主体 ID */
    String verify(Credential credential);
}
```

#### SmsCodeSender — 短信验证码发送

```java
public interface SmsCodeSender {
    /** 发送验证码到指定手机号，返回发送是否成功 */
    boolean send(String phoneNumber, String code);
}
```

#### OAuthProvider — 第三方 OAuth 接入 SPI

```java
public interface OAuthProvider {
    /** 提供商标识（如 wechat、github） */
    String getProviderName();
    /** 获取授权 URL（跳转） */
    String getAuthorizationUrl(String redirectUri, String state);
    /** 通过授权码换取用户信息 */
    Principal getUserInfo(String code, String state);
    /** 通过授权码获取 AccessToken（部分场景需要） */
    AuthToken getAccessToken(String code, String state);
}
```

#### CaptchaService — 图形验证码

```java
public interface CaptchaService {
    /** 生成图形验证码 */
    Captcha generate();
    /** 校验图形验证码 */
    boolean verify(String captchaId, String captchaCode);
}
```

#### MfaService — MFA 服务

```java
public interface MfaService {
    /** 检查是否需要 MFA */
    boolean isMfaRequired(String subjectId);
    /** 发起 MFA 挑战 */
    MfaChallenge sendChallenge(String subjectId, MfaType type);
    /** 校验 MFA 挑战 */
    boolean verifyChallenge(String challengeId, String code);
}
```

#### AccountStatusChecker — 账号状态检查

```java
public interface AccountStatusChecker {
    /** 检查账号状态，异常时抛出 AccountStatusException */
    void checkStatus(String subjectId) throws AccountStatusException;
}
```

#### SessionStore — 会话存储

```java
public interface SessionStore {
    /** 保存会话 */
    void save(Session session);
    /** 根据 sessionId 获取会话 */
    Session get(String sessionId);
    /** 删除会话 */
    void delete(String sessionId);
    /** 删除某用户所有会话 */
    void deleteBySubjectId(String subjectId);
    /** 刷新会话过期时间 */
    void refresh(String sessionId, long ttlSeconds);
    /** 获取某用户所有活跃会话 */
    List<Session> getActiveSessions(String subjectId);
}
```

#### ConcurrentSessionControl — 并发会话控制

```java
public interface ConcurrentSessionControl {
    /** 强制控制并发数，超过则踢出最早的会话 */
    void enforce(String subjectId);
    /** 获取当前并发会话数 */
    int getCurrentCount(String subjectId);
    /** 获取最大并发会话数配置 */
    int getMaxSessions();
}
```

#### RateLimitChecker — 登录频率限制

```java
public interface RateLimitChecker {
    /** 检查是否超过频率限制，超过抛出 RateLimitException */
    void check(String identifier);
    /** 记录一次尝试 */
    void recordAttempt(String identifier, boolean success);
    /** 重置计数 */
    void reset(String identifier);
}
```

### 5.2 授权域

#### AccessControlProvider — API 级访问控制

```java
public interface AccessControlProvider {
    /**
     * 检查当前主体是否有权访问指定 API
     * @param principal  当前用户主体
     * @param apiMethod  HTTP 方法
     * @param apiUri     URI 路径
     * @return true=可访问, false=拒绝
     */
    boolean hasPermission(Principal principal, String apiMethod, String apiUri);

    /**
     * 获取当前主体有权限的所有 API 列表
     */
    List<Permission> getUserPermissions(Principal principal);
}
```

#### MenuProvider — 菜单/资源树提供者

```java
public interface MenuProvider {
    /** 获取当前用户的菜单树 */
    List<MenuNode> getUserMenuTree(Principal principal);
    /** 获取当前用户的按钮权限列表 */
    List<String> getUserButtonPermissions(Principal principal);
}
```

#### DataScopeProvider — 数据权限范围提供者

```java
public interface DataScopeProvider {
    /** 获取当前用户的数据权限范围 */
    List<DataScope> getDataScopes(Principal principal);
    /** 按维度获取数据权限范围 */
    List<DataScope> getDataScopesByDimension(Principal principal, String dimensionCode);
}
```

#### FieldPermissionProvider — 字段级权限提供者

```java
public interface FieldPermissionProvider {
    /** 获取指定 API 的字段权限配置 */
    List<FieldPermission> getFieldPermissions(String apiCode);
    /** 获取当前用户对指定 API 的字段权限 */
    List<FieldPermission> getUserFieldPermissions(Principal principal, String apiCode);
}
```

### 5.3 基础设施

#### RequestLogger — 请求日志记录器

```java
public interface RequestLogger {
    /** 异步保存请求日志 */
    void save(RequestRecord record);
}
```

#### SecurityHeaderProvider — HTTP 安全头提供者

```java
public interface SecurityHeaderProvider {
    /** 获取安全头配置 */
    SecurityHeaders getHeaders();
}
```

---

## 6. 过滤器链

### 6.1 过滤器定义与顺序

| 序号 | 过滤器 | 职责 | Order | 说明 |
|------|--------|------|-------|------|
| 1 | `RequestWrapperFilter` | 请求体可重复读取包装 | `Integer.MIN_VALUE` | 缓存请求体字节，确保后续过滤器可重复读取 |
| 2 | `RequestLogFilter` | 请求日志全量采集 | `FilterOrder.LOGGING` | 最外层 try/finally，确保被拦截请求也记录 |
| 3 | `SecurityHeaderFilter` | HTTP 安全头注入 | `FilterOrder.SEC_HEADER` | 调用 SecurityHeaderProvider 注入响应头 |
| 4 | `AuthenticationFilter` | 认证 | `FilterOrder.AUTH` | 调用 AuthenticationProvider，设置 SecurityContext |
| 5 | `AuthorizationFilter` | 鉴权 | `FilterOrder.AUTHZ` | 调用 AccessControlProvider，判断 API 权限 |

**关键设计：`RequestLogFilter` 位置**

`RequestLogFilter` 包裹了认证和鉴权过滤器，采用 `try { chain.doFilter() } finally { saveLog() }` 模式。即使 `AuthenticationFilter` 或 `AuthorizationFilter` 抛出异常，日志依然在 finally 中被记录，确保任何请求——无论成功被拦截还是正常通过——都有日志留存。

### 6.2 过滤器行为详解

**RequestWrapperFilter**：
- 输入：原始 `HttpServletRequest`
- 输出：`EagerContentCachingRequestWrapper`（主动缓存 body，Spring 原生需要等流读完才缓存）
- 异常处理：无异常，纯包装

**RequestLogFilter**：
- 前置：记录请求 URI、方法、IP、UA、headers（token 脱敏）
- 执行：`chain.doFilter()`
- 后置（finally）：采集响应状态码、响应体（截断到合理长度）、计算耗时 → 异步保存 → **`SecurityContext.clear()` 清理 ThreadLocal**
- 脱敏规则：token 前 8 后 4 截断、password 字段屏蔽
- 异步保存：通过 `RequestLogger.save()` 异步写入，不阻塞主请求
- 排除规则：静态资源（可配置后缀正则）、health check 路径（`/public/status`）、debug 模式的完整日志

**SecurityHeaderFilter**：
- 从 `SecurityHeaderProvider` 获取配置
- 注入 `X-Frame-Options`、`X-Content-Type-Options`、`Strict-Transport-Security`、`X-XSS-Protection`、`Referrer-Policy`、`Content-Security-Policy` 等响应头

**AuthenticationFilter**：
- 路径白名单：`/public/**`、`/actuator/**`、`/error` 等放行
- 从请求头获取 token（`Authorization: Bearer xxx` 或自定义 `token` 头）
- 调用 `AuthenticationProvider.authenticate()` 执行认证
- 认证成功后：`SecurityContext.setPrincipal()`、`SecurityContext.setToken()` 设置上下文
- 认证失败后：返回 401，详细错误码写入响应
- 无 token 的受保护路径：返回 401

**AuthorizationFilter**：
- 路径白名单同认证过滤器
- 从 `SecurityContext` 获取当前 `Principal`
- 调用 `AccessControlProvider.hasPermission(principal, method, uri)` 鉴权
- 鉴权失败：返回 403
- 无 `AccessControlProvider` 实现（未接入授权时）：放行（不做鉴权），仅记录 debug 日志

### 6.3 路径白名单机制

`AuthProperties.whiteList.paths` 配置项：

```yaml
sh.auth.white-list.paths:
  - /public/**
  - /actuator/**
  - /error
  - /swagger-ui/**
  - /v3/api-docs/**
```

白名单路径跳过认证和鉴权，但 `RequestLogFilter` 依然记录日志（由日志排除规则单独控制）。

### 6.4 SecurityContext 生命周期

```
请求到达
  → SecurityContext 初始化（空）
    → RequestLogFilter 记录请求
      → AuthenticationFilter: SecurityContext.setPrincipal() / setToken()
        → AuthorizationFilter: 读取 SecurityContext
      → 业务控制器: SecurityContext.getPrincipal() 可用
    → RequestLogFilter finally: 保存日志 → SecurityContext.clear() 清理
```

---

## 7. 异常体系

```
RuntimeException
└── AuthException (基类)
    ├── AuthenticationException     # 认证异常 (401)
    │   ├── [badCredentials]        凭证错误
    │   ├── [captchaError]          验证码错误
    │   ├── [tokenExpired]          Token 过期
    │   └── [tokenInvalid]          Token 无效
    ├── AuthorizationException      # 授权异常 (403)
    │   ├── [accessDenied]          无权访问
    │   └── [fieldDenied]           字段级权限不足
    ├── SessionExpiredException     # 会话过期 (401)
    ├── AccountStatusException      # 账号状态异常 (401)
    │   ├── [accountLocked]         账号锁定
    │   ├── [accountDisabled]       账号禁用
    │   └── [accountExpired]        账号过期
    ├── MfaRequiredException        # MFA 要求 (401，携带 challengeId)
    └── RateLimitException          # 频率限制 (429)
```

所有异常通过全局异常处理器（或 Filter 的 response 写入）返回统一格式的 JSON 错误响应。

---

## 8. 枚举定义

### AuthStatus — 认证状态

```java
public enum AuthStatus {
    SUCCESS,        // 认证成功（含 MFA）
    MFA_REQUIRED,   // 需要 MFA 二次验证
    FAILED          // 认证失败
}
```

### AuthErrorType — 认证错误类型

```java
public enum AuthErrorType {
    BAD_CREDENTIALS,        // 凭证错误
    USER_NOT_FOUND,         // 用户不存在
    ACCOUNT_LOCKED,         // 账号锁定
    ACCOUNT_DISABLED,       // 账号禁用
    ACCOUNT_EXPIRED,        // 账号过期
    CREDENTIALS_EXPIRED,    // 密码过期
    TOKEN_EXPIRED,          // Token 过期
    TOKEN_INVALID,          // Token 无效
    CAPTCHA_REQUIRED,       // 需要验证码
    CAPTCHA_ERROR,          // 验证码错误
    CAPTCHA_TIMEOUT,        // 验证码超时
    MFA_REQUIRED,           // 需要 MFA
    MFA_ERROR,              // MFA 错误
    RATE_LIMITED,           // 频率限制
    SESSION_EXPIRED,        // 会话过期
    ACCESS_DENIED,          // 无权访问
    INTERNAL_ERROR,         // 内部错误
    SERVICE_UNAVAILABLE     // 服务不可用
}
```

### AccountStatus

```java
public enum AccountStatus {
    ENABLED,        // 启用
    DISABLED,       // 禁用
    LOCKED          // 锁定
}
```

### TokenType

```java
public enum TokenType {
    JWT,            // JWT 令牌（HS256/RS256）
    OAUTH_BEARER,   // OAuth2 Bearer Token
    SESSION_ID      // Session ID（传统会话）
}
```

### CredentialType

```java
public enum CredentialType {
    PASSWORD,       // 密码
    SMS_CODE,       // 短信验证码
    SOCIAL_CODE     // 社交登录授权码（微信等）
}
```

### MfaType

```java
public enum MfaType {
    SMS,            // 短信验证码
    TOTP            // 基于时间的一次性密码
}
```

### MenuType

```java
public enum MenuType {
    MENU,           // 菜单
    BUTTON          // 按钮
}
```

### FieldPermissionType

```java
public enum FieldPermissionType {
    READ,           // 只读
    WRITE,          // 可写
    HIDDEN          // 隐藏
}
```

---

## 9. 配置

### AuthProperties

```java
@ConfigurationProperties(prefix = "sh.auth")
@Data
public class AuthProperties {
    /** 是否启用 sh-auth */
    private boolean enabled = true;

    /** 会话配置 */
    private Session session = new Session();

    /** 密码策略 */
    private Password password = new Password();

    /** MFA 配置 */
    private Mfa mfa = new Mfa();

    /** 频率限制 */
    private RateLimit rateLimit = new RateLimit();

    /** 白名单 */
    private WhiteList whiteList = new WhiteList();

    /** CORS 配置 */
    private Cors cors = new Cors();

    @Data
    public static class Session {
        /** 会话超时秒数，默认 24h */
        private long ttl = 86400;
        /** 最大并发会话数，0 表示不限制 */
        private int maxConcurrent = 0;
    }

    @Data
    public static class Password {
        /** 密码过期天数 */
        private int expireDays = 180;
        /** 密码历史保留数量 */
        private int historySize = 5;
        /** 最小密码长度 */
        private int minLength = 8;
    }

    @Data
    public static class Mfa {
        /** 默认 MFA 方式 */
        private MfaType defaultType = null;
    }

    @Data
    public static class RateLimit {
        /** 最大尝试次数 */
        private int maxAttempts = 5;
        /** 限流窗口（分钟） */
        private int windowMinutes = 60;
    }

    @Data
    public static class WhiteList {
        /** 跳过认证和鉴权的路径 */
        private List<String> paths = List.of(
            "/public/**",
            "/actuator/**",
            "/error"
        );
    }

    @Data
    public static class Cors {
        private boolean enabled = false;
        private String allowedOrigins = "*";
        private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";
        private String allowedHeaders = "*";
        private long maxAge = 3600;
    }
}
```

### AuthConstants

```java
public final class AuthConstants {
    /** 默认 Token 请求头名称 */
    public static final String DEFAULT_TOKEN_HEADER = "Authorization";
    /** Bearer Token 前缀 */
    public static final String BEARER_PREFIX = "Bearer ";
    /** 自定义 Token 请求头（兼容旧版） */
    public static final String CUSTOM_TOKEN_HEADER = "token";
    /** 登录频率限制 Redis Key 模板 */
    public static final String RATE_LIMIT_KEY_PREFIX = "auth:rate_limit:";
    /** 验证码 Redis Key 模板 */
    public static final String CAPTCHA_KEY_PREFIX = "auth:captcha:";
    /** Session Redis Key 模板 */
    public static final String SESSION_KEY_PREFIX = "auth:session:";
    /** 用户会话列表 Redis Key 模板 */
    public static final String SESSION_LIST_KEY_PREFIX = "auth:session:list:";
    /** MFA 挑战 Redis Key 模板 */
    public static final String MFA_CHALLENGE_KEY_PREFIX = "auth:mfa:challenge:";

    private AuthConstants() {}
}
```

---

## 10. 自动配置

### ShAuthAutoConfiguration

```java
@AutoConfiguration
@ConditionalOnProperty(prefix = "sh.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.wkclz.auth")
@EnableConfigurationProperties(AuthProperties.class)
public class ShAuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SecurityContext securityContext() {
        return new SecurityContext();
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultLogoutService defaultLogoutService(SessionStore sessionStore) {
        return new DefaultLogoutService(sessionStore);
    }

    // 注册过滤器 Bean
    @Bean
    @ConditionalOnMissingBean
    public RequestWrapperFilter requestWrapperFilter() {
        return new RequestWrapperFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public RequestLogFilter requestLogFilter(List<RequestLogger> requestLoggers) {
        return new RequestLogFilter(requestLoggers);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityHeaderFilter securityHeaderFilter(List<SecurityHeaderProvider> headerProviders) {
        return new SecurityHeaderFilter(headerProviders);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationFilter authenticationFilter(
            TokenService tokenService,
            SessionStore sessionStore,
            AuthProperties properties) {
        return new AuthenticationFilter(tokenService, sessionStore, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationFilter authorizationFilter(
            List<AccessControlProvider> accessControlProviders,
            AuthProperties properties) {
        return new AuthorizationFilter(accessControlProviders, properties);
    }
}
```

### 自动配置注册文件

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：

```
com.wkclz.auth.ShAuthAutoConfiguration
```

### SPI 注入策略

所有 SPI 接口采用 `List<Xxx>` 注入方式，支持多实现共存：

- `AuthenticationProvider`：多实现时按 `getOrder()` 排序，依次尝试直到某个成功
- `AccessControlProvider`：多实现时按 `getOrder()` 排序，任一拒绝即拒绝（一票否决）
- `RequestLogger`：多实现时全部调用（广播模式）
- 其余 SPI：单实现，通过 `@ConditionalOnMissingBean` 自动选择

---

## 11. 过滤器注册

过滤器统一通过 `FilterRegistrationBean` 注册到 Servlet 容器，而非使用 `@WebFilter`，以保证顺序可控：

```java
// 示例（在 ShAuthAutoConfiguration 中）
@Bean
public FilterRegistrationBean<AuthenticationFilter> authFilterReg(AuthenticationFilter filter) {
    FilterRegistrationBean<AuthenticationFilter> reg = new FilterRegistrationBean<>();
    reg.setFilter(filter);
    reg.addUrlPatterns("/*");
    reg.setOrder(FilterOrder.AUTH);  // 通过 FilterOrder 常量控制顺序
    reg.setName("authenticationFilter");
    return reg;
}
```

每个 Filter 对应一个 `FilterRegistrationBean`。

---

## 12. 认证流程全景

```
┌───────────────────────────────────────────────────────────────┐
│                        sh-auth 认证流程                         │
├───────────────────────────────────────────────────────────────┤
│                                                                │
│  HTTP Request                                                  │
│      │                                                         │
│      ▼                                                         │
│  RequestWrapperFilter ─── 包装请求体可重复读                     │
│      │                                                         │
│      ▼                                                         │
│  RequestLogFilter ─── 记录请求信息（最外层）                      │
│      │ try {                                                   │
│      ▼                                                         │
│  SecurityHeaderFilter ─── 注入安全头                             │
│      │                                                         │
│      ▼                                                         │
│  AuthenticationFilter                                          │
│      │                                                         │
│      ├── 路径在白名单？ ──→ 放行                                │
│      │                                                         │
│      ├── 获取 Token（Header / 参数）                            │
│      │                                                         │
│      ├── TokenService.parseToken() 解析 Token                   │
│      │                                                         │
│      ├── TokenService.validateToken() 验证签名/过期             │
│      │                                                         │
│      ├── SessionStore.get() 检查会话                            │
│      │                                                         │
│      ├── SecurityContext.setPrincipal() 设置上下文               │
│      │                                                         │
│      └── 失败 → 返回 401                                       │
│      │                                                         │
│      ▼                                                         │
│  AuthorizationFilter                                           │
│      │                                                         │
│      ├── 路径在白名单？ ──→ 放行                                │
│      │                                                         │
│      ├── 无 AccessControlProvider？ ──→ 放行（仅日志）           │
│      │                                                         │
│      ├── AccessControlProvider.hasPermission() 鉴权              │
│      │                                                         │
│      └── 失败 → 返回 403                                       │
│      │                                                         │
│      ▼                                                         │
│  业务 Controller                                                │
│      │                                                         │
│      ▼                                                         │
│  } finally {                                                   │
│      RequestLogFilter.saveLog() ─── 异步保存日志                │
│  }                                                             │
│      │                                                         │
│      ▼                                                         │
│  SecurityContext.clear() ─── 清理 ThreadLocal                   │
│                                                                │
└───────────────────────────────────────────────────────────────┘
```

---

## 13. 登录流程全景

```
用户 → LoginRest（iam-sso 层，二期实现）
      → LoginService.login()（sh-auth 模板方法）

LoginService.login() 模板步骤：
  ① checkRateLimit(identifier)
     → RateLimitChecker.check() → 查 Redis 计数 → 超限抛 RateLimitException
  ② checkCaptcha(request)
     → CaptchaService.verify(captchaId, captchaCode) → 校验图形验证码
  ③ authenticate(request)
     → AuthenticationProvider.authenticate():
       ├── [密码登录] CredentialProvider.verify(credential)
       │   → PasswordEncoder.matches(raw, encoded)
       ├── [短信登录] CredentialProvider.verify(credential)
       │   → 查 Redis 短信验证码 → 比对
       └── [社交登录] OAuthProvider.getUserInfo(code, state)
           → 微信 API 换 access_token → 换用户信息
  ④ checkAccountStatus(subjectId)
     → AccountStatusChecker.checkStatus() → 检查锁定/禁用/过期
  ⑤ checkMfa(principal)
     → MfaService.isMfaRequired(subjectId)
       → 需要则 sendChallenge() → 返回 MfaChallenge
       → 不需要则放行
  ⑥ createSession(principal)
     → TokenService.generateToken() → AuthToken
     → SessionStore.save(session)
     → ConcurrentSessionControl.enforce(subjectId)
  ⑦ recordLoginLog(result)
     → LoginRecord 构建 → 异步写入
```

---

## 14. 扩展点汇总

| 扩展点 | 类型 | 场景 | 默认实现 |
|--------|------|------|---------|
| `AuthenticationProvider` | SPI 多实现 | 添加新认证方式 | 无（由接入方实现） |
| `CredentialProvider` | SPI 多实现 | 添加新凭证类型 | 无 |
| `OAuthProvider` | SPI 多实现 | 添加新第三方登录 | 无 |
| `TokenService` | SPI 单实现 | 替换 Token 方案 | 无 |
| `PasswordEncoder` | SPI 单实现 | 替换加密算法 | 无（建议 MD5+salt 或 BCrypt） |
| `PasswordValidator` | SPI 单实现 | 自定义密码策略 | 无 |
| `CaptchaService` | SPI 单实现 | 替换验证码生成方式 | 无（建议 AWT 图形） |
| `MfaService` | SPI 单实现 | 自定义 MFA 方式 | 无 |
| `SmsCodeSender` | SPI 单实现 | 接入短信平台 | 无 |
| `AccountStatusChecker` | SPI 单实现 | 自定义账号状态逻辑 | 无 |
| `SessionStore` | SPI 单实现 | 替换会话存储后端 | 无（建议 Redis） |
| `ConcurrentSessionControl` | SPI 单实现 | 自定义并发策略 | 无 |
| `RateLimitChecker` | SPI 单实现 | 自定义限流算法 | 无 |
| `AccessControlProvider` | SPI 多实现 | 添加新授权体系 | 无 |
| `MenuProvider` | SPI 单实现 | 自定义菜单结构 | 无 |
| `DataScopeProvider` | SPI 单实现 | 自定义数据权限 | 无 |
| `FieldPermissionProvider` | SPI 单实现 | 自定义字段权限 | 无 |
| `RequestLogger` | SPI 多实现 | 日志多路输出 | 无 |
| `SecurityHeaderProvider` | SPI 多实现 | 自定义安全头 | 无 |

---

## 15. IAM 接入映射（二期规划参考）

| sh-auth SPI | IAM 实现层 | IAM 实现类（规划） |
|-------------|-----------|-------------------|
| `AuthenticationProvider` | iam-sso | `IamAuthenticationProvider`（JWT + Redis Session） |
| `TokenService` | iam-sdk | `JwtTokenService`（HS256 JWT） |
| `CredentialProvider` | iam-sso | `PasswordCredentialProvider`（MD5+salt） |
| `PasswordEncoder` | iam-common | `Md5PasswordEncoder` |
| `PasswordValidator` | iam-sso | `IamPasswordValidator`（历史查 iam_user_password_his） |
| `CaptchaService` | iam-sdk/iam-sso | `AwtCaptchaService`（AWT 图形） |
| `SessionStore` | iam-sso | `RedisSessionStore`（Redis） |
| `AccessControlProvider` | iam-admin | `RbacAccessControlProvider`（角色-菜单-API 联查） |
| `MenuProvider` | iam-admin | `IamMenuProvider`（若依格式菜单树） |
| `DataScopeProvider` | iam-admin | `IamDataScopeProvider` |
| `RequestLogger` | iam-sso | `IamRequestLogger`（写 iam_request_log 表） |

---

## 16. 测试策略

### 单元测试

- 每个 SPI 接口的默认实现（DefaultXxx）需有单元测试
- `LoginService` 模板方法需 Mock 所有 SPI 后测试流程分支
- 每个 Filter 需测试正常路径和异常路径
- 覆盖率目标：接口层 100%，Filter 层 90%+

### 集成测试

- 无需外部依赖（无 IAM、无数据库），所有 SPI 用 Mock 实现
- 验证过滤器链执行顺序
- 验证 SecurityContext 生命周期（set/get/clear）
- 验证路径白名单生效
- 验证异常时日志仍然保存

---

## 17. 风险与待定项

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| SPI 接口变更频繁 | 破坏接入方 | 一期设计充分，预留 extra 扩展 Map |
| 认证方式复杂度过高 | 模板方法不够灵活 | LoginService 关键步骤允许子类完全覆盖 |
| IAM 迁移工作量大 | 二期周期长 | 一期 sh-auth 独立设计，二期 IAM 逐步迁移 |
| sh-redis 依赖太重 | 不需要 Redis 的接入方 | 仅 `SessionStore` 默认实现用 Redis，其余无 Redis 依赖，且 `sh-redis` 标记为 optional |
