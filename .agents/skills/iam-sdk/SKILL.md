---
name: "iam-sdk"
description: "sh-iam SDK模块知识库。提供第三方应用接入IAM的完整能力：IamAuthFilter鉴权过滤器(JWT+Redis会话校验)、LoggingFilter请求日志(AK签名+SsoFacade远程保存)、JwtUtil令牌工具(HS256/24h/时钟偏差容忍)、SessionHelper用户上下文、CaptchaHelper图形验证码(Java AWT生成/Redis 5min过期)、AkSignHelper AK签名(RSA私钥加密/字典排序)。当涉及IAM接入、JWT鉴权、请求日志、验证码、AK签名、用户会话时调用。"
---

# iam-sdk 模块知识库

iam-sdk 是 sh-iam 的 SDK 模块，供第三方应用引入，提供鉴权 Filter、JWT 令牌、会话管理、请求日志、验证码、AK 签名等完整 IAM 接入能力。

## 包结构

```
com.wkclz.iam.sdk
├── IamSdkAutoConfig                    # 自动配置（@ConditionalOnProperty）
├── config/
│   └── IamSdkConfig                    # SDK配置 + SsoFacade默认Bean
├── facade/
│   ├── SsoFacade                       # 门面接口
│   └── impl/SsoFacadeImpl              # HTTP远程调用实现
├── service/
│   └── IamSsoService                   # Token校验接口
├── filter/
│   ├── RequestWrapperFilter            # 请求体包装（Order=MIN_VALUE）
│   ├── LoggingFilter                   # 请求日志（Order=MIN_VALUE+1）
│   ├── IamAuthFilter                   # 鉴权过滤器
│   └── EagerContentCachingRequestWrapper # 急切缓存请求体
├── helper/
│   ├── SessionHelper                   # 用户会话/上下文工具
│   ├── AkSignHelper                    # AK签名工具
│   ├── CaptchaHelper                   # 图形验证码生成
│   └── ResponseHelper                  # 鉴权错误响应工具
├── model/
│   ├── UserJwt                         # JWT载荷模型
│   ├── UserSession                     # Redis会话模型
│   ├── LoginRequest                    # 登录请求
│   ├── LoginResponse                   # 登录响应
│   ├── RegisterRequest                 # 注册请求（空类）
│   ├── PictureCaptchaResponse          # 验证码响应
│   └── RequestLog                      # 请求日志模型（30+字段）
├── enums/
│   ├── AuthType                        # 认证类型（PASSWORD）
│   └── LoginStatus                     # 登录状态（15种）
└── util/
    └── JwtUtil                         # JWT工具类
```

## 自动配置

```java
@AutoConfiguration
@ComponentScan(basePackages = {"com.wkclz.iam.sdk"})
@ConditionalOnProperty(prefix = "sh.iam.sdk", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IamSdkAutoConfig {}
```

引入 `iam-sdk` 依赖后自动生效，通过 `sh.iam.sdk.enabled=false` 可关闭。

**SsoFacade 可替换设计**：`IamSdkConfig` 中 `@Bean @ConditionalOnMissingBean` 注册默认实现；iam-sso 的 `SsoFacadeImpl` 自动覆盖。

## IamSdkConfig — 配置项

| 字段 | 配置键 | 默认值 | 说明 |
|------|--------|--------|------|
| enabled | `iam.sdk.enabled` | true | SDK启用开关 |
| appCode | `iam.sdk.app-code` | 空 | 应用编码 |
| jwtSecretKey | `iam.sdk.jwt.secret-key` | qwertyuiop...（64字符） | JWT签名密钥（**生产必须覆盖**） |
| staticEnabled | `iam.sdk.static.enabled` | false | 静态资源日志过滤开关 |
| staticSubfix | `iam.sdk.static.subfix` | js\|css\|jpg\|png\|... | 静态资源后缀（竖线分隔正则组） |
| serverUrl | `iam.sdk.server-url` | 空 | SSO服务端地址 |
| appId | `iam.sdk.app-id` | default | 应用ID（AK鉴权用） |
| appSecret | `iam.sdk.app-secret` | default | 应用密钥（AK鉴权用） |

## Filter链 — 执行顺序

```
Request (原始)
  → RequestWrapperFilter  (Order=Integer.MIN_VALUE)
       包装为 EagerContentCachingRequestWrapper（支持多次读取请求体）
  → LoggingFilter         (Order=Integer.MIN_VALUE+1)
       记录请求信息 + 包装Response + 异步保存日志
  → IamAuthFilter         (默认Order)
       JWT验证 + Redis会话校验 + 缓存用户信息到请求上下文
  → Controller
  → LoggingFilter (finally: 记录响应 + 异步保存日志)
```

### RequestWrapperFilter

- `@Order(Integer.MIN_VALUE)` 确保最先执行
- 将原始 `HttpServletRequest` 包装为 `EagerContentCachingRequestWrapper`
- 使后续 Filter/Controller 可多次读取请求体

### EagerContentCachingRequestWrapper

继承 Spring 的 `ContentCachingRequestWrapper`，核心差异为**急切缓存**而非惰性缓存。

| 方法 | 说明 |
|------|------|
| `makeBodyCache()` | **核心方法**：从原始 request 读取 InputStream 到 `cachedBody`，只调用一次 |
| `getInputStream()` | 若 cachedBody 已缓存，用 ByteArrayInputStream 返回；否则降级为 super |
| `getContentAsByteArray()` | 若 cachedBody 已缓存直接返回；否则 delegate to super |

### IamAuthFilter — 鉴权过滤器

**完整鉴权流程**：

```
1. 根路径"/"拦截 → 403 Forbidden
2. 公开路径放行 → URI匹配 /*/public/** 模式（AntPathMatcher）
3. 提取Token → SessionHelper.getToken(request)
     优先 Authorization 头 → 其次 token 头，去除 "Bearer " 前缀
4. Token非空校验 → 空 → 401 "token 不存在!"
5. JWT有效性校验 → JwtUtil.validateToken() → 无效 → 401 "无效的token!"
6. 解析JWT → JwtUtil.parseToken() → 获取 UserJwt
7. Redis会话校验 → iamSsoService.tokenCheck(token, username) → 获取 UserSession
8. 缓存用户信息 → SessionHelper.cacheUserInfo() → request.attribute + UserContext ThreadLocal
9. 放行 → chain.doFilter()
10. 异常兜底 → 步骤6-8异常 → 401 "token验证失败!"
```

### LoggingFilter — 请求日志过滤器

**核心流程**：

```
1. 清理ThreadLocal（REQUEST_LOG, REQUEST_ERROR）
2. 记录requestTime，将HttpServletRequest存入LocalThreadHelper
3. 包装Response: ContentCachingResponseWrapper
4. fetchRequestLog() 填充请求前信息（30+字段，含UA解析）
5. 读取请求体（仅EagerContentCachingRequestWrapper，multipart跳过）
6. chain.doFilter() — 有异常记录errorMsg并re-throw
7. finally:
   - 从SessionHelper获取用户信息填充log
   - 获取响应状态码和响应体（仅application/json）
   - wrappedResponse.copyBodyToResponse()（必须调用！）
   - 合并异常信息 + 计算耗时
   - 日志输出（Debug模式含响应体，非Debug仅耗时/方法/URI/参数）
   - saveResponseLog() → subLog截断 + maskPwd脱敏 → ssoFacade.saveLog()异步保存
```

**日志过滤规则**：
- `NO_LOGS = ["/public/status"]` — 不记录日志的URI
- `staticEnabled=true` 时正则匹配静态资源后缀，跳过日志
- `LOGS_SET` HashMap 缓存URI是否需要记录（synchronized访问）

**密码脱敏**（maskPwd）：正则 `assword"\s*:\s*"(.*?)"` 将密码值替换为等长 `*`

**字段截断**（subLog）：每个字段有 maxLength 限制（requestBody=4095, userAgent=1023, cookie=2047 等）

## SsoFacade — 门面接口

```java
public interface SsoFacade {
    void saveLog(RequestLog log);
}
```

**SDK默认实现**（SsoFacadeImpl）：HTTP远程调用，流程：
1. 校验 uri 和 data 非空、serverUrl 和 appId 非空
2. 拼接 URL：`serverUrl + "/sign" + uri`
3. `AkSignHelper.sign(appId, appSecret)` 生成签名
4. Hutool `HttpUtil.createPost(url)` 发送POST
5. Header：`app-id`、`sign`
6. 请求体：`JSONObject.toJSONString(data)`
7. 响应非200则抛 RuntimeException

## IamSsoService — Token校验接口

```java
public interface IamSsoService {
    UserSession tokenCheck(String token, String authIdentifier);
}
```

由 iam-sso 模块的 `IamSsoServiceImpl` 实现，从 Redis 读取 UserSession。

## SessionHelper — 用户会话工具

| 方法 | 说明 |
|------|------|
| `getToken(HttpServletRequest)` | 从 `Authorization` 或 `token` 头获取，去除 `Bearer ` 前缀 |
| `cacheUserInfo(HttpServletRequest, UserJwt, UserSession)` | 核心：设置 request.attribute + UserContext ThreadLocal |
| `getUserJwt(HttpServletRequest)` | 从 request.attribute("userJwt") 获取 |
| `getUserSession(HttpServletRequest)` | 从 request.attribute("userSession") 获取 |
| `getUserCode()` | 便捷方法，从 getUserJwt() 取 userCode |
| `getAppCode(HttpServletRequest)` | 从请求头 `app-code` 获取应用编码 |

**cacheUserInfo 逻辑**：
```java
request.setAttribute("userJwt", userJwt);
request.setAttribute("userSession", userSession);
UserInfo userInfo = new UserInfo();
userInfo.setUserCode(userJwt.getUserCode());
userInfo.setUsername(userJwt.getUsername());
userInfo.setNickname(userJwt.getNickname());
userInfo.setAvatar(userJwt.getAvatar());
userInfo.setTenantCode("default");  // 当前硬编码
userInfo.setOpenId(userSession.getOpenId());
UserContext.setUserInfo(userInfo);
```

## JwtUtil — JWT工具类

基于 JJWT 库，HMAC-SHA256 签名。

### Token生成

```java
// 默认24小时有效期
String token = JwtUtil.generateToken(userJwt, secretKey);

// 自定义有效期
String token = JwtUtil.generateToken(userJwt, secretKey, expirationMs);
```

**关键细节**：
- 签发时间回退1分钟（`System.currentTimeMillis() - 60*1000`），容忍时钟偏差
- Claims：userCode, username, nickname, avatar
- 算法：HS256

### Token解析与验证

```java
UserJwt userJwt = JwtUtil.parseToken(token, secretKey);
boolean valid = JwtUtil.validateToken(token, secretKey);
boolean expired = JwtUtil.isExpired(token, secretKey);
String token = JwtUtil.refreshToken(oldToken, secretKey);  // 刷新24h
```

### Redis Session Key格式

```java
String redisKey = JwtUtil.getTokenRedisKey(token, username);
// 格式: iam:session:{username}:{MD5(token)}
```

### 异常转换

| JWT异常 | 中文提示 |
|---------|---------|
| ExpiredJwtException | JWT已过期 |
| MalformedJwtException | JWT格式错误 |
| UnsupportedJwtException | 不支持的JWT |
| SignatureException | JWT签名错误 |
| IllegalArgumentException | JWT参数错误 |

### 快捷获取方法

```java
String userCode = JwtUtil.getUserCode(token, secretKey);
String username = JwtUtil.getUsername(token, secretKey);
String nickname = JwtUtil.getNickname(token, secretKey);
String avatar = JwtUtil.getAvatar(token, secretKey);
Date expiration = JwtUtil.getExpirationDate(token, secretKey);
Date issuedAt = JwtUtil.getIssuedAt(token, secretKey);
```

## CaptchaHelper — 图形验证码

```java
// 生成验证码（含captchaCode明文 + captchaId + base64图片 + 过期时间）
PictureCaptchaResponse captcha = CaptchaHelper.getCaptcha();

// Redis Key格式
String redisKey = CaptchaHelper.getCaptchaRedisKey(captchaId);
// 格式: iam:captcha:{captchaId}
```

**配置常量**：

| 常量 | 值 | 说明 |
|------|-----|------|
| CAPTCHA_EXPIRATION | 5 | 有效时间（分钟） |
| CAPTCHA_LENGTH | 4 | 验证码字符长度 |
| WIDTH | 120 | 图片宽度(px) |
| HEIGHT | 40 | 图片高度(px) |

**字符集**：`23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz`（排除 0/O/1/I/l 易混淆字符）

**图片生成**：白色背景 + 5条浅灰干扰线 + 50个噪声点 + 暗色随机旋转字符

## AkSignHelper — AK签名工具

```java
// 生成签名
String sign = AkSignHelper.sign(appId, appSecret);
```

**签名算法**：

```
1. 获取 timestamp = System.currentTimeMillis()
2. 生成 nonce = MD5(UUID + timestamp)
3. 构建待签名Map: {appId, nonce, timestamp}
4. 对key数组字典排序 (Arrays.sort)
5. 拼接为 key1=value1&key2=value2&... （跳过空值）
6. RSA私钥加密拼接串 → 返回签名
```

**签名验证**（deSign，已注释未启用）：
- RSA公钥解密 + 校验appId + 5分钟时间窗口 + RedisLock防重放

## LoginStatus — 登录状态枚举

参照 LDAP 返回码设计，15个枚举值：

| 枚举 | code | message |
|------|------|---------|
| SUCCESS | 0 | 登录成功 |
| INTERNAL_ERROR | 1 | 内部错误 |
| NETWORK_ERROR | 3 | 网络错误 |
| SERVICE_UNAVAILABLE | 6 | 服务不可用 |
| UNSUPPORTED_AUTH_METHOD | 7 | 不支持的认证方法 |
| INVALID_PASSWORD | 31 | 密码错误（**注释：统一使用32**） |
| USER_NOT_FOUND | 32 | 用户不存在 |
| ACCOUNT_LOCKED | 47 | 账号锁定 |
| EXPIRED_PASSWORD | 48 | 密码过期 |
| EXPIRED_ACCOUNT | 49 | 账号过期 |
| AUTHENTICATION_FAILED | 50 | 认证失败 |
| TOO_MANY_ATTEMPTS | 51 | 登录尝试次数过多 |
| INVALID_CREDENTIALS | 52 | 无效凭证 |
| ACCOUNT_DISABLED | 53 | 账号禁用 |
| INVALID_CAPTCHA | 54 | 验证码错误 |
| NEED_CAPTCHA | 60 | 需要验证码 |
| CAPTCHA_TIMEOUT | 61 | 验证码超时 |

**反向查询**：`LoginStatus.getByCode(int code)` — 遍历匹配，未找到返回 null

## Redis Key命名规范

| 业务 | Key格式 | 过期时间 |
|------|---------|---------|
| 用户会话 | `iam:session:{username}:{MD5(token)}` | 由SSO服务控制 |
| 验证码 | `iam:captcha:{captchaId}` | 5分钟 |

## 可扩展点

| 扩展点 | 接口 | 默认实现 | 替换方式 |
|--------|------|---------|---------|
| 请求日志存储 | `SsoFacade` | SDK空实现(HTTP远程调用) | `@ConditionalOnMissingBean` 自动覆盖 |
| Token校验逻辑 | `IamSsoService` | 由iam-sso模块提供 | `@Component`覆盖 |
| 签名验证 | `AkSignHelper.deSign` | 已注释，未启用 | 取消注释+补充RedisLock |

## 依赖

- `sh-web` — Web基础能力（IpHelper, RequestHelper, R响应封装, LocalThreadHelper）
- `spring-boot-autoconfigure` — 自动配置基础
- `fastjson2` — JSON序列化
- `jjwt-api/impl/jackson` — JWT三件套
