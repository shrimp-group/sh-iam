---
name: "sh-iam-contract-default"
description: "sh-framework IAM 契约层默认实现模块知识库。包含DefaultAuthContract(读宽容验证严格)/DefaultAuthzContract(读返回空+canAccessApi抛ACCESS_DENIED)/DefaultAkSignContract(功能不可用)/DefaultSsoFacadeContract(login抛异常+saveLog/logout静默)四默认实现、DefaultAuthFilter过滤器、IamContractAutoConfig自动配置(@ConditionalOnMissingBean替换)、ContractConfig配置绑定。当涉及IAM契约替换、自定义认证实现、JWT/Redis集成、DefaultAuthFilter配置时调用。"
---

# sh-iam-contract-default 模块知识库

sh-iam-contract-default 是 sh-framework IAM 契约层的默认实现模块，提供四契约的"读宽容验证严格"默认实现、DefaultAuthFilter
鉴权过滤器、IamContractAutoConfig 自动配置。引入即可快速启动；业务方声明同类型 Bean 即可通过 @ConditionalOnMissingBean
替换默认实现。

## 模块定位

- **依赖**：iam-contract-api + spring-boot-autoconfigure + spring-boot-starter-web
- **职责**：提供四契约默认实现 + DefaultAuthFilter + 自动配置
- **替换机制**：业务方实现契约接口 + @Component，默认实现自动失效（@ConditionalOnMissingBean）
- **Maven 坐标**：`com.wkclz.framework:iam-contract-default:${revision}`（当前版本 5.0.1-SNAPSHOT）
- **Java 版本**：Java 25 编译，与框架整体一致

## 包结构

`com.wkclz.iam.contract.defaults` 下 4 个子包：

```
com.wkclz.iam.contract.defaults
├── config/      # IamContractAutoConfig（自动配置）、ContractConfig（@Value 配置绑定）
├── filter/      # DefaultAuthFilter（鉴权过滤器）
├── facade/      # DefaultSsoFacadeContract
└── service/     # DefaultAuthContract / DefaultAuthzContract / DefaultAkSignContract
```

## 默认实现行为矩阵（核心）

四契约默认实现遵循"读宽容验证严格"
设计原则：读操作不阻断业务（返回空/原值/静默），验证/功能性操作严格拒绝（抛异常），确保系统启动时不依赖外部实现，但运行时不会"
裸奔"放过未授权请求。

| 契约                       | 读操作               | 验证/功能操作                               | 设计理由                      |
|--------------------------|-------------------|---------------------------------------|---------------------------|
| DefaultAuthContract      | 无 token 返回 null   | 有 token 抛 TOKEN_INVALID               | 读宽容（放行 public）验证严格        |
| DefaultAuthzContract     | 返回空列表 / 原字段       | canAccessApi 抛 ACCESS_DENIED          | 读不影响启动，验证防裸奔              |
| DefaultAkSignContract    | -                 | sign/verifySign 抛异常                   | 功能性操作不该被调用                |
| DefaultSsoFacadeContract | saveLog/logout 静默 | login 抛 UnsupportedOperationException | 日志不阻断业务，login 未配置实现属系统级错误 |

### DefaultAuthContract — 认证默认实现

位于 `com.wkclz.iam.contract.defaults.service.DefaultAuthContract`。

| 方法                      | 行为                             | 异常/返回                                                               |
|-------------------------|--------------------------------|---------------------------------------------------------------------|
| `authenticate(request)` | 无 token → 返回 null；有 token → 拒绝 | 返回 null 或抛 `AuthException(TOKEN_INVALID, "无认证实现，请配置 AuthContract")` |
| `doAuthenticate(token)` | 直接拒绝                           | 抛 `AuthException(TOKEN_MISSING, "无认证实现，请配置 AuthContract")`          |

设计意图：过滤器据此放行 public 路径（authenticate 返回 null），有 token 但无实现则严格拒绝。checkToken 继承自模板方法（token
空值校验 → doAuthenticate → Session 存在性 → authIdentifier 一致性），doAuthenticate 无实现时抛 TOKEN_MISSING。

### DefaultAuthzContract — 鉴权默认实现

位于 `com.wkclz.iam.contract.defaults.service.DefaultAuthzContract`。

| 方法                                                                                        | 行为                                                          |
|-------------------------------------------------------------------------------------------|-------------------------------------------------------------|
| `listTenants` / `listApps` / `getMenuTree` / `listFieldPermissions` / `getDataDimensions` | 返回 `Collections.emptyList()`                                |
| `filterFields`                                                                            | 返回原字段列表（不过滤）                                                |
| `canAccessApi`                                                                            | 抛 `AuthException(ACCESS_DENIED, "无鉴权实现，请配置 AuthzContract")` |

设计意图：读操作返回空不影响启动（菜单/权限列表为空），但接口鉴权必须显式拒绝，防止未授权访问。

> **HTTP 状态码**：`DefaultAuthFilter` 捕获 `AuthException` 后，通过 `e.getErrorType().getHttpStatus()`
> 设置响应状态码。TOKEN_MISSING / TOKEN_INVALID / TOKEN_EXPIRED / SESSION_EXPIRED / AK_SIGN_* 返回 401，ACCESS_DENIED 返回
> 403。友好提示通过 `getMessage()` 获取。

### DefaultAkSignContract — AK 签名默认实现

位于 `com.wkclz.iam.contract.defaults.service.DefaultAkSignContract`。

| 方法                                           | 行为  | 异常                                                               |
|----------------------------------------------|-----|------------------------------------------------------------------|
| `sign(appId, appSecret)`                     | 抛异常 | `UnsupportedOperationException("无 AK 签名实现，请配置 AkSignContract")`  |
| `verifySign(sign, publicKey, expectedAppId)` | 抛异常 | `AuthException(AK_SIGN_INVALID, "无 AK 签名实现，请配置 AkSignContract")` |

设计意图：AK 签名是功能性操作，没有实现就不该被调用，抛异常比静默更安全。

### DefaultSsoFacadeContract — SSO 门面默认实现

位于 `com.wkclz.iam.contract.defaults.facade.DefaultSsoFacadeContract`。

| 方法              | 行为             | 异常                                                                  |
|-----------------|----------------|---------------------------------------------------------------------|
| `login(req)`    | 抛异常            | `UnsupportedOperationException("无 SSO 门面实现，请配置 SsoFacadeContract")` |
| `saveLog(log)`  | 静默跳过（debug 日志） | 不抛异常                                                                |
| `logout(token)` | 静默跳过（debug 日志） | 不抛异常                                                                |

设计意图：login 未配置实现属系统级错误（非业务登录失败），抛 UnsupportedOperationException 而非返回 LoginResp.fail(UNKNOWN)
——业务登录失败应由业务方实现的 SsoFacadeContract 通过 LoginResp.fail() 返回。saveLog/logout 失败不阻断业务（日志丢失可容忍）。

> 语义边界：业务登录失败（密码错误/账号锁定等）通过 `LoginResp.fail(failType, failReason)` 返回；系统级错误（SSO 不可达/未配置实现等）抛
> RuntimeException。详见 [sh-iam-contract-api](../sh-iam-contract-api/SKILL.md) skill 的 "### login() 语义边界" 章节。

## DefaultAuthFilter — 鉴权过滤器

位于 `com.wkclz.iam.contract.defaults.filter.DefaultAuthFilter`。继承 `OncePerRequestFilter`，调用 `AuthContract` SPI
完成认证。认证失败时根据 `AuthErrorType.httpStatus` 返回对应状态码（401/403）。

### 过滤流程（5 步）

1. **根路径拒绝**：`uri.equals("/")` → 返回 `403 Forbidden`，直接 return（不进入过滤器链）
2. **public 路径放行**：使用 `PrincipalContext.match(publicPathPattern, uri)` 匹配，命中则 `chain.doFilter()` 放行
3. **调用 SPI 认证**：`authContract.authenticate(request)` 返回 `AuthResult`
4. **认证成功 → 缓存 + 放行**：`authResult != null` → `PrincipalContext.cache(request, principal, session)` →
   `chain.doFilter()`
5. **认证失败 → 动态状态码**：`authResult == null`（无 token）→ 返回 `401 Unauthorized`；抛 `AuthException` → 根据
   `e.getErrorType().getHttpStatus()` 返回对应状态码（401/403）

### finally 块清理

```java
finally {
    PrincipalContext.clear(); // 防 ThreadLocal 内存泄漏
}
```

无论认证成功或失败，finally 块都会调用 `PrincipalContext.clear()` 清理 ThreadLocal 上下文，防止内存泄漏。

### 注册机制

通过 `FilterRegistrationBean<DefaultAuthFilter>` 包装注册（`DefaultAuthFilter` 类本身不加 `@Component`）：

- **urlPatterns**：`/*`
- **order**：`FilterOrder.AUTH`（= `Ordered.HIGHEST_PRECEDENCE + 10`）
- **name**：`defaultAuthFilter`
- **禁用控制**：`ContractConfig.authFilterEnabled == false` 时，`defaultAuthFilterRegistration()` 返回 null，不注册过滤器

### 控制项

| 配置项                                | 默认值            | 说明                     |
|------------------------------------|----------------|------------------------|
| `iam.contract.auth-filter-enabled` | true           | 是否注册 DefaultAuthFilter |
| `iam.contract.public-path-pattern` | `/*/public/**` | 公开路径匹配模式（Ant 风格）       |

## IamContractAutoConfig — 自动配置

位于 `com.wkclz.iam.contract.defaults.config.IamContractAutoConfig`。

### 注解配置

```java
@AutoConfiguration
@ComponentScan(basePackages = {"com.wkclz.iam.contract.defaults"})
@ConditionalOnProperty(prefix = "sh.iam.contract", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IamContractAutoConfig { ... }
```

- `@AutoConfiguration`：Spring Boot 4 自动配置注解
- `@ComponentScan`：扫描 `com.wkclz.iam.contract.defaults` 包
- `@ConditionalOnProperty`：`sh.iam.contract.enabled=false` 时整个自动配置失效（默认启用）

### 5 个 @ConditionalOnMissingBean Bean

| Bean 类型             | 方法名                   | 默认实现                       | 替换条件                                      |
|---------------------|-----------------------|----------------------------|-------------------------------------------|
| `AuthContract`      | `authContract()`      | `DefaultAuthContract`      | 业务方声明 `@Component` 实现 `AuthContract`      |
| `AuthzContract`     | `authzContract()`     | `DefaultAuthzContract`     | 业务方声明 `@Component` 实现 `AuthzContract`     |
| `AkSignContract`    | `akSignContract()`    | `DefaultAkSignContract`    | 业务方声明 `@Component` 实现 `AkSignContract`    |
| `SsoFacadeContract` | `ssoFacadeContract()` | `DefaultSsoFacadeContract` | 业务方声明 `@Component` 实现 `SsoFacadeContract` |
| `DefaultAuthFilter` | `defaultAuthFilter()` | `DefaultAuthFilter`        | 业务方声明同类型 Bean                             |

### FilterRegistrationBean（非 @ConditionalOnMissingBean）

`defaultAuthFilterRegistration(filter, config)` 方法根据 `ContractConfig.authFilterEnabled` 决定是否注册过滤器：

- `authFilterEnabled == false` → 返回 null（不注册）
- `authFilterEnabled == true` → 返回 `FilterRegistrationBean`，urlPatterns=`/*`，order=`FilterOrder.AUTH`（=
  `HIGHEST_PRECEDENCE + 10`）

### 替换机制

业务方声明同类型 `@Component` Bean 即可让默认实现失效（`@ConditionalOnMissingBean` 自动让位）。例如：

```java
@Component
public class JwtAuthContract implements AuthContract { ... }
```

容器中存在 `JwtAuthContract` 后，`IamContractAutoConfig.authContract()` 不再注册 `DefaultAuthContract`。

### 注册文件

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 内容：

```
com.wkclz.iam.contract.defaults.config.IamContractAutoConfig
```

## ContractConfig — 配置绑定

位于 `com.wkclz.iam.contract.defaults.config.ContractConfig`。`@Configuration` + `@Data` + `@Value` 注入。

### 设计要点

- **采用 `@Value` 注入**（对齐现有 IamSdkConfig 风格，非 `@ConfigurationProperties`）
- **`@PostConstruct` 同步到 `ContractSettings`**：将 7 个配置项同步到 `ContractSettings` 静态持有器，供契约接口的 default
  方法访问

### @PostConstruct 初始化

```java
@PostConstruct
public void initContractSettings() {
    ContractSettings.setAppId(appId);
    ContractSettings.setAppSecret(appSecret);
    ContractSettings.setPublicKey(publicKey);
    ContractSettings.setServerUrl(serverUrl);
    ContractSettings.setJwtSecretKey(jwtSecretKey);
}
```

> 为何用静态持有器：契约接口的 `default` 方法（如 `AkSignContract.sign()`、`AkSignContract.verifySign(request)`、
`SsoFacadeContract.logout()`）无法通过 `@Autowired` 访问 Spring
> 上下文，因此通过静态持有器桥接配置。详见 [sh-iam-contract-api](../sh-iam-contract-api/SKILL.md) skill "##
> ContractSettings — 静态配置持有器" 章节。

## 配置项总表

ContractConfig 通过 `@Value` 注入 7 个配置项，加上 `IamContractAutoConfig` 的 `@ConditionalOnProperty` 控制项，共 8 项配置：

| 配置项                                | 默认值            | 说明                      | 注入位置                                                    |
|------------------------------------|----------------|-------------------------|---------------------------------------------------------|
| `sh.iam.contract.enabled`          | true           | 是否启用契约层自动配置             | IamContractAutoConfig `@ConditionalOnProperty`          |
| `iam.contract.auth-filter-enabled` | true           | 是否注册 DefaultAuthFilter  | ContractConfig `@Value`                                 |
| `iam.contract.public-path-pattern` | `/*/public/**` | 公开路径匹配模式（Ant 风格）        | ContractConfig `@Value`                                 |
| `iam.contract.app-id`              | (空)            | AK 签名 appId             | ContractConfig `@Value` → ContractSettings.appId        |
| `iam.contract.app-secret`          | (空)            | AK 签名 appSecret（RSA 私钥） | ContractConfig `@Value` → ContractSettings.appSecret    |
| `iam.contract.public-key`          | (空)            | AK 验签 publicKey（RSA 公钥） | ContractConfig `@Value` → ContractSettings.publicKey    |
| `iam.contract.server-url`          | (空)            | SSO 服务端地址               | ContractConfig `@Value` → ContractSettings.serverUrl    |
| `iam.contract.jwt-secret-key`      | (空)            | JWT 密钥（供实现层使用）          | ContractConfig `@Value` → ContractSettings.jwtSecretKey |

> 后 5 项（appId/appSecret/publicKey/serverUrl/jwtSecretKey）在 `@PostConstruct` 中同步到 `ContractSettings` 静态持有器，供契约接口的
> default 方法访问。

## 实现者指南

### 引入依赖

```xml
<dependency>
    <groupId>com.wkclz.framework</groupId>
    <artifactId>iam-contract-default</artifactId>
    <version>${revision}</version>
</dependency>
```

引入此模块即获得四契约默认实现 + DefaultAuthFilter + IamContractAutoConfig，开箱即用。

### 替换默认实现的两种方式

**方式 A：实现契约接口 + @Component（推荐）**

业务方实现契约接口并标注 `@Component`，`@ConditionalOnMissingBean` 自动让默认实现失效。可单独替换任一契约，无需全部替换。

```java
@Component
public class JwtAuthContract implements AuthContract { ... }

@Component
public class DbAuthzContract implements AuthzContract { ... }
```

> 此方式下，未替换的契约仍使用默认实现（如 `AkSignContract` 仍为 `DefaultAkSignContract`）。

**方式 B：禁用整个自动配置 + 自行注册所有 Bean**

设置 `sh.iam.contract.enabled=false`，整个 `IamContractAutoConfig` 失效，业务方需自行注册全部 4 个契约 + 过滤器（如需）。

```yaml
sh:
  iam:
    contract:
      enabled: false
```

> 此方式适用于完全自定义 IAM 体系（如集成 shiro / sa-token）的场景。

### 禁用 DefaultAuthFilter

集成 shiro / sa-token 等其他鉴权框架时，可单独禁用 DefaultAuthFilter，保留默认契约实现：

```yaml
iam:
  contract:
    auth-filter-enabled: false
```

此时 `IamContractAutoConfig.defaultAuthFilterRegistration()` 返回 null，不注册过滤器，但仍注册 4 个默认契约 Bean。

### 接口契约详解

四契约 SPI 的完整方法签名、参数、返回值、异常详见 [sh-iam-contract-api](../sh-iam-contract-api/SKILL.md) skill "## 四契约
SPI 详解" 章节。

## 完整示例代码（基于 sh-framework 生态）

### 示例 1：JwtAuthContract（基于 jjwt + RedisHelper 实现 JWT 认证）

实现方只需实现 `authenticate(request)`（过滤器入口）和 `doAuthenticate(token)`（核心认证），`checkToken` 的通用校验由模板方法自动完成。

```java
@Slf4j
@Component
public class JwtAuthContract implements AuthContract {
    private static final String SESSION_KEY = "iam:session:";

    @Autowired
    private RedisHelper redisHelper;

    @Override
    public AuthResult authenticate(HttpServletRequest request) {
        String token = PrincipalContext.getToken();
        if (token == null) return null; // 无 token，过滤器据此放行 public
        return doAuthenticate(token); // 委托核心认证逻辑
    }

    @Override
    public AuthResult doAuthenticate(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(ContractSettings.getJwtSecretKey().getBytes()))
                .build().parseClaimsJws(token).getBody();
            String userCode = claims.getSubject();

            String sessionJson = redisHelper.get(SESSION_KEY + userCode);
            if (sessionJson == null) {
                throw new AuthException(AuthErrorType.SESSION_EXPIRED, "会话已过期");
            }
            Session session = JsonUtil.parse(sessionJson, Session.class);

            Principal principal = new Principal();
            principal.setUserCode(userCode);
            principal.setUsername(claims.get("username", String.class));
            principal.setNickname(claims.get("nickname", String.class));
            principal.setAvatar(claims.get("avatar", String.class));

            AuthResult result = new AuthResult();
            result.setPrincipal(principal);
            result.setSession(session);
            return result;
        } catch (ExpiredJwtException e) {
            throw new AuthException(AuthErrorType.TOKEN_EXPIRED, "Token 已过期", e);
        } catch (SignatureException e) {
            throw new AuthException(AuthErrorType.TOKEN_INVALID, "签名无效", e);
        }
    }

    // 登录时调用，生成 token 并缓存 session
    public String generateToken(Principal principal, Session session) {
        redisHelper.set(SESSION_KEY + principal.getUserCode(), JsonUtil.toJson(session), 7200);
        return Jwts.builder()
            .setSubject(principal.getUserCode())
            .claim("username", principal.getUsername())
            .claim("nickname", principal.getNickname())
            .claim("avatar", principal.getAvatar())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 7200_000))
            .signWith(Keys.hmacShaKeyFor(ContractSettings.getJwtSecretKey().getBytes()))
            .compact();
    }
}
```

> 依赖说明：`jjwt-api` / `jjwt-impl` / `jjwt-jackson` 0.13.0（已在 sh-bom 管理），`RedisHelper`（sh-redis），`JsonUtil`
> （sh-tool）

### 示例 2：DbAuthzContract（基于 sh-mybatis 实现六维度鉴权）

```java
@Slf4j
@Component
public class DbAuthzContract implements AuthzContract {
    @Autowired
    private AuthzMapper authzMapper; // 业务方自定义 Mapper

    @Override
    public List<Tenant> listTenants(Principal principal) {
        if (principal == null) return Collections.emptyList();
        return authzMapper.selectTenantsByUserCode(principal.getUserCode());
    }

    @Override
    public List<App> listApps(Principal principal, String tenantCode) {
        if (principal == null) return Collections.emptyList();
        return authzMapper.selectAppsByUserAndTenant(principal.getUserCode(), tenantCode);
    }

    @Override
    public List<Menu> getMenuTree(Principal principal, String appCode) {
        if (principal == null) return Collections.emptyList();
        List<Menu> flat = authzMapper.selectMenusByUserAndApp(principal.getUserCode(), appCode);
        return buildTree(flat); // 业务方自行实现树构建
    }

    @Override
    public boolean canAccessApi(Principal principal, String appCode, String apiUri, String apiMethod) {
        if (principal == null) return false;
        Integer count = authzMapper.countApiPermission(
            principal.getUserCode(), appCode, apiUri, apiMethod);
        return count != null && count > 0;
    }

    @Override
    public List<FieldPermission> listFieldPermissions(Principal principal, String appCode, String menuCode) {
        if (principal == null) return Collections.emptyList();
        return authzMapper.selectFieldPermissions(principal.getUserCode(), appCode, menuCode);
    }

    @Override
    public List<DataDimension> getDataDimensions(Principal principal, String appCode) {
        if (principal == null) return Collections.emptyList();
        return authzMapper.selectDataDimensions(principal.getUserCode(), appCode);
    }

    @Override
    public List<String> filterFields(Principal principal, String appCode, String menuCode, List<String> fields) {
        List<FieldPermission> perms = listFieldPermissions(principal, appCode, menuCode);
        Set<String> visible = perms.stream()
            .filter(FieldPermission::getVisible)
            .map(FieldPermission::getFieldCode)
            .collect(Collectors.toSet());
        return fields.stream().filter(visible::contains).collect(Collectors.toList());
    }
}
```

> 依赖说明：`AuthzMapper` 由业务方自定义（继承 `BaseMapper` 或直接 `@Mapper`），表结构由业务方设计。`Principal` / `Tenant` /
`App` / `Menu` / `FieldPermission` / `DataDimension` 来自 iam-contract-api。

### 示例 3：RsaAkSignContract（基于 RsaTool + RedisHelper 实现 AK 签名）

```java
@Slf4j
@Component
public class RsaAkSignContract implements AkSignContract {
    private static final String NONCE_KEY = "iam:ak:nonce:";

    @Autowired
    private RedisHelper redisHelper;

    @Override
    public String sign(String appId, String appSecret) {
        String nonce = UUID.randomUUID().toString().replace("-", "");
        long timestamp = System.currentTimeMillis();
        String payload = appId + ":" + nonce + ":" + timestamp;
        String signature = RsaTool.sign(payload, appSecret); // 用 RSA 私钥签名
        return payload + ":" + signature;
    }

    @Override
    public boolean verifySign(String sign, String publicKey, String expectedAppId) {
        if (sign == null) {
            throw new AuthException(AuthErrorType.AK_SIGN_INVALID, "签名不存在");
        }
        String[] parts = sign.split(":");
        if (parts.length != 4) {
            throw new AuthException(AuthErrorType.AK_SIGN_INVALID, "签名格式错误");
        }
        String appId = parts[0];
        String nonce = parts[1];
        long timestamp = Long.parseLong(parts[2]);
        String signature = parts[3];

        if (!appId.equals(expectedAppId)) {
            throw new AuthException(AuthErrorType.AK_SIGN_INVALID, "appId 不匹配");
        }
        if (System.currentTimeMillis() - timestamp > 5 * 60_000L) {
            throw new AuthException(AuthErrorType.AK_SIGN_EXPIRED, "签名已过期");
        }
        boolean setOk = redisHelper.setIfAbsent(NONCE_KEY + nonce, "1", 5 * 60L);
        if (!setOk) {
            throw new AuthException(AuthErrorType.AK_NONCE_REPLAY, "重放检测命中");
        }
        String payload = appId + ":" + nonce + ":" + timestamp;
        if (!RsaTool.verify(payload, signature, publicKey)) {
            throw new AuthException(AuthErrorType.AK_SIGN_INVALID, "签名校验失败");
        }
        return true;
    }
}
```

> 依赖说明：`RsaTool`（sh-tool），`RedisHelper`（sh-redis）。`RsaTool.sign` / `RsaTool.verify` 的具体签名以 sh-tool
> 实际方法为准（实现时若签名不一致，调整为对应方法）

### 示例 4：HttpSsoFacadeContract（HTTP 调用 SSO 服务端）

```java
@Slf4j
@Component
public class HttpSsoFacadeContract implements SsoFacadeContract {
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public LoginResp login(SessionCreateReq req) {
        String url = ContractSettings.getServerUrl() + "/api/sso/login";
        ResponseEntity<LoginResp> resp = restTemplate.postForEntity(url, req, LoginResp.class);
        LoginResp body = resp.getBody();
        if (body == null || body.getToken() == null) {
            throw new ApplicationException("SSO 登录失败");
        }
        return body;
    }

    @Override
    public void saveLog(RequestLog requestLog) {
        try {
            String url = ContractSettings.getServerUrl() + "/api/sso/log/save";
            restTemplate.postForEntity(url, requestLog, Void.class);
        } catch (Exception e) {
            log.error("SSO 日志上报失败: {}", e.getMessage(), e); // 不阻断业务
        }
    }

    @Override
    public void logout(String token) {
        try {
            String url = ContractSettings.getServerUrl() + "/api/sso/logout";
            restTemplate.postForEntity(url, Map.of("token", token), Void.class);
        } catch (Exception e) {
            log.warn("SSO 登出失败: {}", e.getMessage());
        }
    }
}
```

> 依赖说明：HTTP 客户端使用 Spring 自带的 `RestTemplate`（业务方也可替换为 OkHttp / Apache HttpClient）。
`ApplicationException` 来自 sh-core。

### 示例 5：application.yml 完整配置

```yaml
iam:
  contract:
    auth-filter-enabled: true
    public-path-pattern: "/*/public/**"
    app-id: "my-app"
    app-secret: "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQ..."  # RSA 私钥
    public-key: "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwQ8O..."  # RSA 公钥
    server-url: "https://sso.example.com"
    jwt-secret-key: "your-jwt-secret-key-at-least-32-chars"

sh:
  iam:
    contract:
      enabled: true
```

## 注意事项

1. **默认实现不含 JWT/Redis 依赖**：`iam-contract-default` 模块仅依赖 `iam-contract-api` + spring-boot-autoconfigure +
   spring-boot-starter-web，不引入 jjwt / sh-redis 等实现依赖。业务方必须自行实现契约或引入额外依赖（如示例 1 需要 jjwt +
   sh-redis）
2. **DefaultAuthFilter order = FilterOrder.AUTH**（`HIGHEST_PRECEDENCE + 10`）：业务方其他过滤器（如 CORS、日志）注意顺序，必要时使用
   `FilterOrder.LOGGING` / `FilterOrder.AUTH` / `FilterOrder.AUTHZ` 等常量，或自定义 `@Order` 调整
3. **ContractConfig 用 @Value 注入**（非 `@ConfigurationProperties`）：对齐现有 IamSdkConfig 风格，配置项以 `iam.contract.*`
   为前缀（注意不是 `sh.iam.contract.*`，`sh.iam.contract.enabled` 是控制项）
4. **替换默认实现后，默认 Bean 不会注册**（`@ConditionalOnMissingBean`）：业务方声明同类型 `@Component` Bean
   即可让默认实现失效，无需额外配置
5. **public-path-pattern 默认 `/*/public/**`**：业务方需自行保证公开路径符合此模式，或修改配置项以匹配实际路径（如
   `/api/public/**`）
6. **ContractSettings 必须在启动时初始化**：`ContractConfig.@PostConstruct` 将配置同步到 `ContractSettings` 静态持有器，契约接口的
   default 方法依赖此初始化。若禁用整个自动配置（`sh.iam.contract.enabled=false`），业务方需自行初始化 `ContractSettings`
7. **RsaTool.sign / RsaTool.verify 的具体方法签名以 sh-tool 实际实现为准**：示例 3 中的
   `RsaTool.sign(payload, privateKey)` / `RsaTool.verify(payload, signature, publicKey)` 为示意，实现时若方法签名不一致，调整为
   sh-tool 对应方法
8. **DefaultAuthFilter 的 5 步流程不可绕过**：根路径 403 / public 放行 / 调用 SPI / 缓存上下文 /
   动态状态码（AuthErrorType.httpStatus），finally 块清理 PrincipalContext。如需自定义过滤器流程，请实现自己的
   `OncePerRequestFilter` 并禁用 DefaultAuthFilter（`iam.contract.auth-filter-enabled=false`）
