# IAM 契约层实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 为 sh-iam-contract-api 提供 AuthContract + SsoFacadeContract 的具体实现，替换 iam-sdk/iam-sso 中旧的会话抽象，使 iam 全面接入契约层体系。

**架构：** iam-sdk 实现 `JwtAuthContract`（JWT+Redis 认证）+ `HttpSsoFacadeContract`（客户端远程骨架）；iam-sso 实现 `LocalSsoFacadeContract`（本地会话创建）；通过 `@ConditionalOnMissingBean` 替换契约层默认实现。

**技术栈：** Spring Boot 3.x, Java 25, jjwt, fastjson2, Redis, MyBatis

---

## 文件结构

### 创建文件
- `iam-sdk/src/main/java/com/wkclz/iam/sdk/contract/JwtAuthContract.java` — JWT+Redis 认证契约实现
- `iam-sdk/src/main/java/com/wkclz/iam/sdk/contract/HttpSsoFacadeContract.java` — 客户端远程 SSO 门面实现
- `iam-sso/src/main/java/com/wkclz/iam/sso/contract/LocalSsoFacadeContract.java` — SSO 服务端本地会话门面实现
- `iam-sso/src/test/java/com/wkclz/iam/sso/service/IamSessionServiceTest.java` — IamSessionService 单元测试
- `iam-sso/src/test/java/com/wkclz/iam/sso/contract/LocalSsoFacadeContractTest.java` — LocalSsoFacadeContract 单元测试
- `iam-sdk/src/test/java/com/wkclz/iam/sdk/contract/JwtAuthContractTest.java` — JwtAuthContract 单元测试

### 修改文件
- `iam-sdk/pom.xml` — 新增 iam-contract-api + iam-contract-default 依赖
- `iam-sdk/src/main/java/com/wkclz/iam/sdk/config/IamSdkConfig.java` — 精简字段，删除与契约层重复配置
- `iam-sdk/src/main/java/com/wkclz/iam/sdk/helper/AkSignHelper.java` — 改为从 ContractSettings 读 appId/appSecret
- `iam-sdk/src/main/java/com/wkclz/iam/sdk/filter/LoggingFilter.java` — SsoFacadeContract + PrincipalContext
- `iam-sso/src/main/java/com/wkclz/iam/sso/service/IamSessionService.java` — 抽离 createSession/enforceMaxConcurrentSessions
- `iam-sso/src/main/java/com/wkclz/iam/sso/service/IamLoginService.java` — LoginFailType 映射 + SsoFacadeContract
- `iam-sso/src/main/java/com/wkclz/iam/sso/rest/LoginRest.java` — 返回契约层 LoginResp
- `iam-sso/src/main/java/com/wkclz/iam/sso/rest/UserInfoRest.java` — PrincipalContext + 返回 Principal
- `iam-admin/src/main/java/com/wkclz/iam/admin/rest/UserMenuRest.java` — PrincipalContext

### 删除文件
- `iam-sdk/src/main/java/com/wkclz/iam/sdk/filter/IamAuthFilter.java`
- `iam-sdk/src/main/java/com/wkclz/iam/sdk/service/IamSsoService.java`
- `iam-sdk/src/main/java/com/wkclz/iam/sdk/helper/SessionHelper.java`
- `iam-sdk/src/main/java/com/wkclz/iam/sdk/bean/UserJwt.java`
- `iam-sdk/src/main/java/com/wkclz/iam/sdk/bean/UserSession.java`
- `iam-sdk/src/main/java/com/wkclz/iam/sdk/facade/SsoFacade.java`
- `iam-sdk/src/main/java/com/wkclz/iam/sdk/facade/impl/SsoFacadeImpl.java`
- `iam-sdk/src/main/java/com/wkclz/iam/sdk/bean/req/SessionCreateReq.java`
- `iam-sdk/src/main/java/com/wkclz/iam/sdk/bean/resp/LoginResp.java`
- `iam-sso/src/main/java/com/wkclz/iam/sso/service/SsoFacadeImpl.java`
- `iam-sso/src/main/java/com/wkclz/iam/sso/service/IamSsoServiceImpl.java`

---

## 关键实现说明

### JwtValidationException 异常映射

`JwtUtil.parseToken()` 抛 `JwtValidationException`（非标准 JWT 库异常），含 errorCode 字段。JwtAuthContract 需按 errorCode 映射到 `AuthErrorType`：

| JwtValidationException.errorCode | AuthErrorType |
|---|---|
| `JWT_EXPIRED` | `TOKEN_EXPIRED` |
| `JWT_SIGNATURE_ERROR` | `TOKEN_INVALID` |
| `JWT_MALFORMED` | `TOKEN_INVALID` |
| `JWT_UNSUPPORTED` | `TOKEN_INVALID` |
| `JWT_ILLEGAL_ARGUMENT` | `TOKEN_INVALID` |

### RequestLog 字段映射问题

契约层 `RequestLog`（13 字段）与 iam-sdk `RequestLog`（30+ 字段）字段差异巨大。

**处理方案**：
- `LoggingFilter` 保持使用 iam-sdk `RequestLog`（内部完整模型）
- `LoggingFilter` 调用 `SsoFacadeContract.saveLog()` 时，将 iam-sdk `RequestLog` 转换为契约层 `RequestLog`（仅复制共有字段）
- `LocalSsoFacadeContract.saveLog()` 接收契约层 `RequestLog` 后，转换为 iam-sdk `RequestLog` 再委托 `IamRequestService.insertLog()`（缺失字段为 null，不影响日志写入）

### Principal 含 authIdentifier 字段

契约层 `Principal` 含 `authIdentifier` 字段。`JwtAuthContract.authenticate()` 和 `LocalSsoFacadeContract.login()` 构建 Principal 时需设置此字段（从 Session 获取）。

---

## 任务 1：iam-sdk pom.xml 依赖调整

**文件：**
- 修改：`iam-sdk/pom.xml`

- [ ] **步骤 1：读取当前 pom.xml**

读取 `iam-sdk/pom.xml`，找到 `<dependencies>` 节点位置。

- [ ] **步骤 2：新增契约层依赖**

在 `iam-sdk/pom.xml` 的 `<dependencies>` 节点中，在现有依赖之前添加：

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

- [ ] **步骤 3：Commit**

```bash
git add iam-sdk/pom.xml
git commit -m "feat(iam-sdk): 新增 iam-contract-api/default 依赖"
```

---

## 任务 2：IamSdkConfig 精简

**文件：**
- 修改：`iam-sdk/src/main/java/com/wkclz/iam/sdk/config/IamSdkConfig.java`

- [ ] **步骤 1：精简 IamSdkConfig**

将 `iam-sdk/src/main/java/com/wkclz/iam/sdk/config/IamSdkConfig.java` 完整替换为：

```java
package com.wkclz.iam.sdk.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class IamSdkConfig {

    @Value(("${iam.sdk.app-code:}"))
    private String appCode;

    @Value(("${iam.sdk.static.enabled:false}"))
    private String staticEnabled;

    @Value(("${iam.sdk.static.subfix:js|css|jpg|png|mp3|html|htm|jpeg|ttf|woff|ico|woff2|map}"))
    private String staticSubfix;

}
```

删除的字段（由契约层 `ContractSettings` 持有）：`enabled`、`jwtSecretKey`、`serverUrl`、`appId`、`appSecret`。
删除的 Bean：`getCasFacade()`（由 `DefaultSsoFacadeContract` + `HttpSsoFacadeContract` 替代）。

- [ ] **步骤 2：Commit**

```bash
git add iam-sdk/src/main/java/com/wkclz/iam/sdk/config/IamSdkConfig.java
git commit -m "refactor(iam-sdk): 精简 IamSdkConfig，删除与契约层重复的配置字段"
```

---

## 任务 3：AkSignHelper 改用 ContractSettings

**文件：**
- 修改：`iam-sdk/src/main/java/com/wkclz/iam/sdk/helper/AkSignHelper.java`

- [ ] **步骤 1：修改 AkSignHelper 字段注入**

在 `iam-sdk/src/main/java/com/wkclz/iam/sdk/helper/AkSignHelper.java` 中：

1. 删除 `import com.wkclz.iam.sdk.config.IamSdkConfig;`
2. 新增 `import com.wkclz.iam.contract.config.ContractSettings;`
3. 删除字段 `@Autowired private IamSdkConfig config;`（第 25-26 行）
4. `verifySign` 方法中不需要修改（不依赖 config）

`sign()` 是静态方法，已在签名中通过参数接收 appId/appSecret，无需改动。`verifySign` 是实例方法但也不依赖 config 字段。删除 `config` 字段即可。

- [ ] **步骤 2：Commit**

```bash
git add iam-sdk/src/main/java/com/wkclz/iam/sdk/helper/AkSignHelper.java
git commit -m "refactor(iam-sdk): AkSignHelper 移除 IamSdkConfig 依赖"
```

---

## 任务 4：JwtAuthContract 实现

**文件：**
- 创建：`iam-sdk/src/main/java/com/wkclz/iam/sdk/contract/JwtAuthContract.java`

- [ ] **步骤 1：创建 JwtAuthContract**

创建文件 `iam-sdk/src/main/java/com/wkclz/iam/sdk/contract/JwtAuthContract.java`：

```java
package com.wkclz.iam.sdk.contract;

import com.alibaba.fastjson2.JSON;
import com.wkclz.iam.contract.bean.AuthResult;
import com.wkclz.iam.contract.bean.Principal;
import com.wkclz.iam.contract.bean.Session;
import com.wkclz.iam.contract.config.ContractSettings;
import com.wkclz.iam.contract.context.PrincipalContext;
import com.wkclz.iam.contract.exception.AuthException;
import com.wkclz.iam.contract.exception.AuthException.AuthErrorType;
import com.wkclz.iam.contract.service.AuthContract;
import com.wkclz.iam.sdk.bean.UserJwt;
import com.wkclz.iam.sdk.exception.JwtValidationException;
import com.wkclz.iam.sdk.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class JwtAuthContract implements AuthContract {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public AuthResult authenticate(HttpServletRequest request) {
        String token = PrincipalContext.getToken();
        if (!StringUtils.hasText(token)) {
            return null;
        }

        UserJwt userJwt = parseAndValidateToken(token);
        String username = userJwt.getUsername();

        String sessionJson = getSessionFromRedis(token, username);

        Session session = deserializeSession(sessionJson, username);

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

    @Override
    public Session checkToken(String token, String authIdentifier) {
        if (!StringUtils.hasText(token)) {
            throw new AuthException(AuthErrorType.TOKEN_MISSING, "token 不能为空");
        }

        UserJwt userJwt = parseAndValidateToken(token);
        String username = userJwt.getUsername();

        String sessionJson = getSessionFromRedis(token, username);

        Session session = deserializeSession(sessionJson, username);

        if (StringUtils.hasText(authIdentifier)) {
            boolean passwordSceneMatch = authIdentifier.equals(username);
            boolean thirdPartySceneMatch = authIdentifier.equals(session.getAuthIdentifier());
            if (!passwordSceneMatch && !thirdPartySceneMatch) {
                log.warn("checkToken authIdentifier 不匹配, expected={}, username={}, session.authId={}",
                        authIdentifier, username, session.getAuthIdentifier());
                throw new AuthException(AuthErrorType.TOKEN_INVALID, "认证标识不匹配");
            }
        }

        return session;
    }

    private UserJwt parseAndValidateToken(String token) {
        try {
            return JwtUtil.parseToken(token, ContractSettings.getJwtSecretKey());
        } catch (JwtValidationException e) {
            String errorCode = e.getErrorCode();
            String message = e.getMessage();
            switch (errorCode) {
                case JwtValidationException.CODE_EXPIRED:
                    throw new AuthException(AuthErrorType.TOKEN_EXPIRED, "Token 已过期", e);
                case JwtValidationException.CODE_SIGNATURE:
                    throw new AuthException(AuthErrorType.TOKEN_INVALID, "Token 签名无效", e);
                case JwtValidationException.CODE_MALFORMED:
                    throw new AuthException(AuthErrorType.TOKEN_INVALID, "Token 格式错误", e);
                case JwtValidationException.CODE_UNSUPPORTED:
                    throw new AuthException(AuthErrorType.TOKEN_INVALID, "Token 格式不支持", e);
                case JwtValidationException.CODE_ILLEGAL_ARGUMENT:
                    throw new AuthException(AuthErrorType.TOKEN_INVALID, "Token 格式错误", e);
                default:
                    throw new AuthException(AuthErrorType.TOKEN_INVALID, "Token 无效: " + message, e);
            }
        }
    }

    private String getSessionFromRedis(String token, String username) {
        String sessionKey = JwtUtil.getTokenRedisKey(token, username);
        try {
            String sessionJson = redisTemplate.opsForValue().get(sessionKey);
            if (sessionJson == null) {
                throw new AuthException(AuthErrorType.SESSION_EXPIRED, "会话已过期");
            }
            return sessionJson;
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("checkToken Redis 查询失败, username={}", username, e);
            throw new RuntimeException("会话存储不可用", e);
        }
    }

    private Session deserializeSession(String sessionJson, String username) {
        try {
            return JSON.parseObject(sessionJson, Session.class);
        } catch (Exception e) {
            log.error("checkToken Session 反序列化失败, username={}", username, e);
            throw new AuthException(AuthErrorType.TOKEN_INVALID, "会话数据损坏", e);
        }
    }
}
```

- [ ] **步骤 2：Commit**

```bash
git add iam-sdk/src/main/java/com/wkclz/iam/sdk/contract/JwtAuthContract.java
git commit -m "feat(iam-sdk): 实现 JwtAuthContract 认证契约"
```

---

## 任务 5：IamSessionService 重构

**文件：**
- 修改：`iam-sso/src/main/java/com/wkclz/iam/sso/service/IamSessionService.java`

- [ ] **步骤 1：重构 IamSessionService**

将 `iam-sso/src/main/java/com/wkclz/iam/sso/service/IamSessionService.java` 完整替换为：

```java
package com.wkclz.iam.sso.service;

import com.alibaba.fastjson2.JSON;
import com.wkclz.iam.contract.bean.Principal;
import com.wkclz.iam.contract.bean.Session;
import com.wkclz.iam.contract.config.ContractSettings;
import com.wkclz.iam.sdk.bean.UserJwt;
import com.wkclz.iam.sdk.util.JwtUtil;
import com.wkclz.tool.tools.Md5Tool;
import com.wkclz.iam.sso.config.IamSsoConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class IamSessionService {

    private static final Logger log = LoggerFactory.getLogger(IamSessionService.class);

    @Autowired
    private IamSsoConfig iamSsoConfig;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 创建会话：Redis SET + ZSET 注册
     */
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

    /**
     * 并发会话数控制：超过上限踢出最早会话
     */
    public void enforceMaxConcurrentSessions(String username) {
        Integer max = iamSsoConfig.getMaxConcurrentSessions();
        if (max == null || max <= 0) {
            return;
        }

        String sessionListKey = JwtUtil.getSessionListRedisKey(username);
        Long count = redisTemplate.opsForZSet().size(sessionListKey);
        if (count == null || count <= max) {
            return;
        }

        Set<String> earliest = redisTemplate.opsForZSet().range(sessionListKey, 0, count - max - 1);
        if (earliest == null) {
            return;
        }

        for (String tokenMd5 : earliest) {
            String sessionKey = "iam:session:" + username + ":" + tokenMd5;
            redisTemplate.delete(sessionKey);
            redisTemplate.opsForZSet().remove(sessionListKey, tokenMd5);
            log.info("用户 {} 并发会话超限，踢出最早会话, tokenMd5={}", username, tokenMd5);
        }
    }

    /**
     * 登出：删除会话 + ZSET 移除
     */
    public void logout(String token) {
        if (StringUtils.isBlank(token)) {
            log.warn("logout 传入 token 为空，跳过登出处理");
            return;
        }

        String username;
        try {
            UserJwt userJwt = JwtUtil.parseToken(token, ContractSettings.getJwtSecretKey());
            username = userJwt.getUsername();
        } catch (Exception e) {
            log.warn("logout 解析 JWT 失败，跳过登出处理: {}", e.getMessage());
            return;
        }

        log.info("用户 {} 开始本地登出", username);

        String tokenRedisKey = JwtUtil.getTokenRedisKey(token, username);
        Boolean deleted = redisTemplate.delete(tokenRedisKey);
        log.info("删除会话 key: {}, 结果: {}", tokenRedisKey, deleted);

        String sessionListKey = JwtUtil.getSessionListRedisKey(username);
        String tokenMd5 = Md5Tool.md5(token);
        Long removed = redisTemplate.opsForZSet().remove(sessionListKey, tokenMd5);
        log.info("用户 {} 从会话列表移除 token, 结果: {}", username, removed);
    }

    /**
     * 批量失效（改密后踢出所有会话）
     */
    public void invalidateAllSessions(String username) {
        String sessionListKey = JwtUtil.getSessionListRedisKey(username);
        Set<String> tokenMd5Set = redisTemplate.opsForZSet().range(sessionListKey, 0, -1);
        if (tokenMd5Set != null && !tokenMd5Set.isEmpty()) {
            Collection<String> sessionKeys = new ArrayList<>(tokenMd5Set.size());
            for (String tokenMd5 : tokenMd5Set) {
                sessionKeys.add("iam:session:" + username + ":" + tokenMd5);
            }
            Long deleted = redisTemplate.delete(sessionKeys);
            log.info("用户 {} 批量删除 {} 个会话 key，实际删除 {} 个", username, sessionKeys.size(), deleted);
        }
        redisTemplate.delete(sessionListKey);
        log.info("用户 {} 的所有会话已失效，共清理 {} 个会话", username, tokenMd5Set == null ? 0 : tokenMd5Set.size());
    }

}
```

- [ ] **步骤 2：Commit**

```bash
git add iam-sso/src/main/java/com/wkclz/iam/sso/service/IamSessionService.java
git commit -m "refactor(iam-sso): IamSessionService 抽离 createSession/enforceMaxConcurrentSessions 方法"
```

---

## 任务 6：LocalSsoFacadeContract 实现

**文件：**
- 创建：`iam-sso/src/main/java/com/wkclz/iam/sso/contract/LocalSsoFacadeContract.java`

- [ ] **步骤 1：创建 LocalSsoFacadeContract**

创建文件 `iam-sso/src/main/java/com/wkclz/iam/sso/contract/LocalSsoFacadeContract.java`：

```java
package com.wkclz.iam.sso.contract;

import com.wkclz.iam.common.entity.IamLoginLog;
import com.wkclz.iam.contract.bean.Principal;
import com.wkclz.iam.contract.bean.Session;
import com.wkclz.iam.contract.bean.RequestLog;
import com.wkclz.iam.contract.bean.req.SessionCreateReq;
import com.wkclz.iam.contract.bean.resp.LoginResp;
import com.wkclz.iam.contract.config.ContractSettings;
import com.wkclz.iam.contract.facade.SsoFacadeContract;
import com.wkclz.iam.sdk.bean.UserJwt;
import com.wkclz.iam.sdk.bean.enums.LoginStatus;
import com.wkclz.iam.sdk.util.JwtUtil;
import com.wkclz.iam.sso.mapper.SsoLoginLogMapper;
import com.wkclz.iam.sso.service.IamRequestService;
import com.wkclz.iam.sso.service.IamSessionService;
import com.wkclz.web.helper.IpHelper;
import com.wkclz.web.helper.RequestHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LocalSsoFacadeContract implements SsoFacadeContract {

    @Autowired
    private IamSessionService iamSessionService;
    @Autowired
    private IamRequestService requestLogService;
    @Autowired
    private SsoLoginLogMapper ssoLoginLogMapper;

    @Override
    public LoginResp login(SessionCreateReq req) {
        log.info("SsoFacade 本地创建会话, authIdentifier: {}", req.getAuthIdentifier());

        // 1. 构建 Principal（JWT claims 来源）
        Principal principal = new Principal();
        principal.setUserCode(req.getUserCode());
        principal.setUsername(req.getUsername());
        principal.setNickname(req.getNickname());
        principal.setAvatar(req.getAvatar());
        principal.setAuthIdentifier(req.getAuthIdentifier());

        // 2. 构建 Session（Redis 存储来源）
        Session session = new Session();
        session.setUserCode(req.getUserCode());
        session.setAuthType(req.getAuthType());
        session.setAuthIdentifier(req.getAuthIdentifier());

        // 3. 生成 JWT
        UserJwt userJwt = new UserJwt();
        userJwt.setUserCode(req.getUserCode());
        userJwt.setUsername(req.getUsername());
        userJwt.setNickname(req.getNickname());
        userJwt.setAvatar(req.getAvatar());
        String token = JwtUtil.generateToken(userJwt, ContractSettings.getJwtSecretKey());

        // 4. 缓存 Session + 注册会话列表
        iamSessionService.createSession(token, principal, session);

        // 5. 并发会话数控制
        iamSessionService.enforceMaxConcurrentSessions(req.getUsername());

        // 6. 记录登录日志（成功）
        recordLoginLog(req);

        // 7. 返回成功响应
        return LoginResp.success(token, req.getUserCode(), req.getUsername(),
                req.getNickname(), req.getAvatar());
    }

    @Override
    public void saveLog(RequestLog log) {
        // 契约层 RequestLog 转换为 iam-sdk RequestLog，委托 IamRequestService
        com.wkclz.iam.sdk.bean.RequestLog sdkLog = new com.wkclz.iam.sdk.bean.RequestLog();
        sdkLog.setAppCode(log.getAppCode());
        sdkLog.setUserCode(log.getUserCode());
        sdkLog.setRequestUri(log.getUri());
        sdkLog.setMethod(log.getMethod());
        sdkLog.setRequestBody(log.getRequestBody());
        sdkLog.setHttpStatus(log.getResponseStatus());
        sdkLog.setResponseBody(log.getResponseBody());
        sdkLog.setCostTime(log.getDuration());
        sdkLog.setRemoteAddr(log.getClientIp());
        requestLogService.insertLog(sdkLog);
    }

    @Override
    public void logout(String token) {
        iamSessionService.logout(token);
    }

    private void recordLoginLog(SessionCreateReq req) {
        IamLoginLog loginLog = new IamLoginLog();
        loginLog.setAuthIdentifier(req.getAuthIdentifier());
        loginLog.setAuthType(req.getAuthType());
        loginLog.setLoginStatus(LoginStatus.SUCCESS.getCode());
        loginLog.setMessage(LoginStatus.SUCCESS.getMessage());
        loginLog.setUserCode(req.getUserCode());
        loginLog.setUsername(req.getAuthIdentifier());
        loginLog.setCreateBy(req.getAuthIdentifier());
        loginLog.setUpdateBy(req.getAuthIdentifier());

        // 优先使用 SessionCreateReq 中的 clientIp/userAgent，兜底从请求上下文获取
        if (req.getClientIp() != null) {
            loginLog.setIpAddress(req.getClientIp());
        }
        if (req.getUserAgent() != null) {
            loginLog.setUserAgent(req.getUserAgent());
        }
        HttpServletRequest request = RequestHelper.getRequest();
        if (request != null) {
            if (loginLog.getIpAddress() == null) {
                loginLog.setIpAddress(IpHelper.getOriginIp(request));
            }
            if (loginLog.getUserAgent() == null) {
                loginLog.setUserAgent(request.getHeader("User-Agent"));
            }
        }
        ssoLoginLogMapper.insertLoginLog(loginLog);
    }

}
```

- [ ] **步骤 2：Commit**

```bash
git add iam-sso/src/main/java/com/wkclz/iam/sso/contract/LocalSsoFacadeContract.java
git commit -m "feat(iam-sso): 实现 LocalSsoFacadeContract 本地会话门面"
```

---

## 任务 7：HttpSsoFacadeContract 实现

**文件：**
- 创建：`iam-sdk/src/main/java/com/wkclz/iam/sdk/contract/HttpSsoFacadeContract.java`

- [ ] **步骤 1：创建 HttpSsoFacadeContract**

创建文件 `iam-sdk/src/main/java/com/wkclz/iam/sdk/contract/HttpSsoFacadeContract.java`：

```java
package com.wkclz.iam.sdk.contract;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wkclz.iam.contract.bean.RequestLog;
import com.wkclz.iam.contract.bean.req.SessionCreateReq;
import com.wkclz.iam.contract.bean.resp.LoginResp;
import com.wkclz.iam.contract.config.ContractSettings;
import com.wkclz.iam.contract.facade.SsoFacadeContract;
import com.wkclz.iam.sdk.helper.AkSignHelper;
import com.wkclz.core.exception.SystemException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnMissingBean(SsoFacadeContract.class)
public class HttpSsoFacadeContract implements SsoFacadeContract {

    private static final String URI_PREFIX = "/sign";

    @Override
    public LoginResp login(SessionCreateReq req) {
        String serverUrl = ContractSettings.getServerUrl();
        if (StringUtils.isBlank(serverUrl)) {
            throw SystemException.of("iam.contract.server-url 未配置，无法远程登录，请配置 SSO 服务端地址");
        }
        log.info("远程创建会话，authIdentifier: {}", req.getAuthIdentifier());
        String responseBody = postDataWithResponse("/login", req);
        JSONObject jsonObject = JSON.parseObject(responseBody);
        Object data = jsonObject.get("data");
        if (data == null) {
            log.error("远程登录响应异常，无 data 字段，response: {}", responseBody);
            throw SystemException.of("远程登录响应异常，无 data 字段");
        }
        return JSON.parseObject(data.toString(), LoginResp.class);
    }

    @Override
    public void saveLog(RequestLog log) {
        try {
            postData("/saveLog", log);
        } catch (Exception e) {
            log.error("远程保存请求日志失败: {}", e.getMessage());
        }
    }

    @Override
    public void logout(String token) {
        if (StringUtils.isBlank(token)) {
            return;
        }
        try {
            postData("/logout", token);
        } catch (Exception e) {
            log.error("远程登出失败: {}", e.getMessage());
        }
    }

    private void postData(String uri, Object data) {
        if (StringUtils.isBlank(uri)) {
            throw SystemException.of("uri 不能为空");
        }
        if (data == null) {
            return;
        }
        String serverUrl = ContractSettings.getServerUrl();
        if (StringUtils.isBlank(serverUrl)) {
            throw SystemException.of("server-url 不能为空");
        }
        String appId = ContractSettings.getAppId();
        if (StringUtils.isBlank(appId)) {
            throw SystemException.of("app-id 不能为空");
        }

        String url = serverUrl + URI_PREFIX + uri;
        String sign = AkSignHelper.sign(ContractSettings.getAppId(), ContractSettings.getAppSecret());

        HttpRequest post = HttpUtil.createPost(url);
        post.header("app-id", appId);
        post.header("sign", sign);

        try {
            post.body(JSONObject.toJSONString(data));
            HttpResponse execute = post.execute();
            int status = execute.getStatus();
            if (status != 200) {
                throw SystemException.of("请求{}异常: {}", uri, status);
            }
        } catch (SystemException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new SystemException("请求" + uri + "异常:" + e.getMessage(), e);
        }
    }

    private String postDataWithResponse(String uri, Object data) {
        if (StringUtils.isBlank(uri)) {
            throw SystemException.of("uri 不能为空");
        }
        if (data == null) {
            throw SystemException.of("data 不能为空");
        }
        String serverUrl = ContractSettings.getServerUrl();
        if (StringUtils.isBlank(serverUrl)) {
            throw SystemException.of("server-url 不能为空");
        }
        String appId = ContractSettings.getAppId();
        if (StringUtils.isBlank(appId)) {
            throw SystemException.of("app-id 不能为空");
        }

        String url = serverUrl + URI_PREFIX + uri;
        String sign = AkSignHelper.sign(ContractSettings.getAppId(), ContractSettings.getAppSecret());

        HttpRequest post = HttpUtil.createPost(url);
        post.header("app-id", appId);
        post.header("sign", sign);

        try {
            post.body(JSONObject.toJSONString(data));
            HttpResponse execute = post.execute();
            int status = execute.getStatus();
            if (status != 200) {
                log.error("请求{}异常，HTTP状态码: {}", uri, status);
                throw SystemException.of("请求{}异常: {}", uri, status);
            }
            return execute.body();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("请求{}异常: {}", uri, e.getMessage());
            throw new SystemException("请求" + uri + "异常:" + e.getMessage(), e);
        }
    }

}
```

- [ ] **步骤 2：Commit**

```bash
git add iam-sdk/src/main/java/com/wkclz/iam/sdk/contract/HttpSsoFacadeContract.java
git commit -m "feat(iam-sdk): 实现 HttpSsoFacadeContract 远程会话门面骨架"
```

---

## 任务 8：IamLoginService 改造

**文件：**
- 修改：`iam-sso/src/main/java/com/wkclz/iam/sso/service/IamLoginService.java`

- [ ] **步骤 1：修改 imports**

在 `iam-sso/src/main/java/com/wkclz/iam/sso/service/IamLoginService.java` 中：

1. 删除以下 import：
```java
import com.wkclz.iam.sdk.bean.enums.LoginStatus;
import com.wkclz.iam.sdk.helper.SessionHelper;
import com.wkclz.iam.sdk.bean.req.SessionCreateReq;
import com.wkclz.iam.sdk.bean.resp.LoginResp;
import com.wkclz.iam.sdk.facade.SsoFacade;
```

2. 新增以下 import：
```java
import com.wkclz.iam.contract.bean.req.SessionCreateReq;
import com.wkclz.iam.contract.bean.resp.LoginResp;
import com.wkclz.iam.contract.context.PrincipalContext;
import com.wkclz.iam.contract.enums.LoginFailType;
import com.wkclz.iam.contract.facade.SsoFacadeContract;
```

3. 保留 `import com.wkclz.iam.sdk.bean.enums.LoginStatus;`（loginLog 方法仍用于日志记录）

- [ ] **步骤 2：修改字段注入**

将第 46 行 `private SsoFacade ssoFacade;` 替换为：
```java
private SsoFacadeContract ssoFacadeContract;
```

- [ ] **步骤 3：修改 loginByUsernameAndPassword 方法**

将 `loginByUsernameAndPassword` 方法中所有 `failResp(LoginStatus.XXX)` 调用替换为 `LoginResp.fail(LoginFailType.YYY)`，并补充 `sessionCreateReq.setClientIp/setUserAgent`：

1. 第 91 行 `return failResp(LoginStatus.NEED_CAPTCHA);` 替换为：
```java
return LoginResp.fail(LoginFailType.CAPTCHA_REQUIRED);
```

2. 第 102 行 `return failResp(LoginStatus.CAPTCHA_TIMEOUT);` 替换为：
```java
return LoginResp.fail(LoginFailType.CAPTCHA_ERROR, "验证码已过期");
```

3. 第 107 行 `return failResp(LoginStatus.INVALID_CAPTCHA);` 替换为：
```java
return LoginResp.fail(LoginFailType.CAPTCHA_ERROR);
```

4. 第 115 行 `return failResp(LoginStatus.USER_NOT_FOUND, "用户不存在, 或密码错误!");` 替换为：
```java
return LoginResp.fail(LoginFailType.USERNAME_OR_PASSWORD_ERROR);
```

5. 第 121 行 `return failResp(LoginStatus.EXPIRED_ACCOUNT);` 替换为：
```java
return LoginResp.fail(LoginFailType.ACCOUNT_DISABLED);
```

6. 第 127 行 `return failResp(LoginStatus.ACCOUNT_LOCKED);` 替换为：
```java
return LoginResp.fail(LoginFailType.ACCOUNT_LOCKED);
```

7. 第 133 行 `return failResp(LoginStatus.ACCOUNT_DISABLED);` 替换为：
```java
return LoginResp.fail(LoginFailType.ACCOUNT_DISABLED);
```

8. 第 139 行 `return failResp(LoginStatus.INVALID_CREDENTIALS);` 替换为：
```java
return LoginResp.fail(LoginFailType.USERNAME_OR_PASSWORD_ERROR);
```

9. 第 150 行 `return failResp(LoginStatus.EXPIRED_PASSWORD);` 替换为：
```java
return LoginResp.fail(LoginFailType.CREDENTIALS_EXPIRED);
```

10. 第 154-161 行 `SessionCreateReq` 构建后，补充 clientIp 和 userAgent：
```java
SessionCreateReq sessionCreateReq = new SessionCreateReq();
sessionCreateReq.setUserCode(auth.getUserCode());
sessionCreateReq.setUsername(auth.getUsername());
sessionCreateReq.setAuthIdentifier(auth.getAuthIdentifier());
sessionCreateReq.setNickname(auth.getNickname());
sessionCreateReq.setAvatar(auth.getAvatar());
sessionCreateReq.setAuthType(auth.getAuthType());
sessionCreateReq.setClientIp(IpHelper.getOriginIp(request));
sessionCreateReq.setUserAgent(request.getHeader("User-Agent"));
```

11. 第 163 行 `LoginResp response = ssoFacade.login(sessionCreateReq);` 替换为：
```java
LoginResp response = ssoFacadeContract.login(sessionCreateReq);
```

- [ ] **步骤 4：修改 logout 方法**

将第 175-182 行 `logout` 方法替换为：
```java
public void logout(HttpServletRequest request) {
    String token = PrincipalContext.getToken();
    if (StringUtils.isBlank(token)) {
        return;
    }
    ssoFacadeContract.logout(token);
}
```

- [ ] **步骤 5：修改 changePassword 方法**

将第 190 行 `String userCode = SessionHelper.getUserCode();` 替换为：
```java
String userCode = PrincipalContext.getUserCode();
```

将第 236 行 `String username = SessionHelper.getUserJwt().getUsername();` 替换为：
```java
String username = PrincipalContext.getUsername();
```

- [ ] **步骤 6：删除 failResp 私有方法**

删除第 268-280 行的 `failResp` 两个私有方法（不再使用）：
```java
// 删除整个 failResp(LoginStatus loginStatus) 方法
// 删除整个 failResp(LoginStatus loginStatus, String message) 方法
```

保留 `loginLog` 方法（仍用于登录日志记录）。

- [ ] **步骤 7：Commit**

```bash
git add iam-sso/src/main/java/com/wkclz/iam/sso/service/IamLoginService.java
git commit -m "refactor(iam-sso): IamLoginService 改用 LoginFailType 和 SsoFacadeContract"
```

---

## 任务 9：LoginRest 改造

**文件：**
- 修改：`iam-sso/src/main/java/com/wkclz/iam/sso/rest/LoginRest.java`

- [ ] **步骤 1：修改 imports**

在 `iam-sso/src/main/java/com/wkclz/iam/sso/rest/LoginRest.java` 中：

1. 删除 `import com.wkclz.iam.sdk.bean.resp.LoginResp;`
2. 新增 `import com.wkclz.iam.contract.bean.resp.LoginResp;`

- [ ] **步骤 2：Commit**

```bash
git add iam-sso/src/main/java/com/wkclz/iam/sso/rest/LoginRest.java
git commit -m "refactor(iam-sso): LoginRest 返回契约层 LoginResp"
```

---

## 任务 10：UserInfoRest 改造

**文件：**
- 修改：`iam-sso/src/main/java/com/wkclz/iam/sso/rest/UserInfoRest.java`

- [ ] **步骤 1：修改 imports**

在 `iam-sso/src/main/java/com/wkclz/iam/sso/rest/UserInfoRest.java` 中：

1. 删除：
```java
import com.wkclz.iam.sdk.bean.UserSession;
import com.wkclz.iam.sdk.helper.SessionHelper;
```

2. 新增：
```java
import com.wkclz.iam.contract.bean.Principal;
import com.wkclz.iam.contract.context.PrincipalContext;
```

- [ ] **步骤 2：修改 userInfo 方法**

将第 37-42 行 `userInfo` 方法替换为：

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

- [ ] **步骤 3：修改 userMenuTree 方法**

将第 47 行 `String appCode = SessionHelper.getAppCode(request);` 替换为：
```java
String appCode = PrincipalContext.getAppCode();
```

- [ ] **步骤 4：修改 userMenuTreeRuoyi 方法**

将第 58 行 `String appCode = SessionHelper.getAppCode(request);` 替换为：
```java
String appCode = PrincipalContext.getAppCode();
```

- [ ] **步骤 5：Commit**

```bash
git add iam-sso/src/main/java/com/wkclz/iam/sso/rest/UserInfoRest.java
git commit -m "refactor(iam-sso): UserInfoRest 改用 PrincipalContext，返回 Principal"
```

---

## 任务 11：UserMenuRest 改造

**文件：**
- 修改：`iam-admin/src/main/java/com/wkclz/iam/admin/rest/UserMenuRest.java`

- [ ] **步骤 1：修改 imports**

在 `iam-admin/src/main/java/com/wkclz/iam/admin/rest/UserMenuRest.java` 中：

1. 删除 `import com.wkclz.iam.sdk.helper.SessionHelper;`
2. 新增 `import com.wkclz.iam.contract.context.PrincipalContext;`

- [ ] **步骤 2：修改 userMenuList 方法**

将第 36 行 `entity.setUserCode(SessionHelper.getUserCode());` 替换为：
```java
entity.setUserCode(PrincipalContext.getUserCode());
```

- [ ] **步骤 3：修改 userMenuTree 方法**

将第 45 行 `entity.setUserCode(SessionHelper.getUserCode());` 替换为：
```java
entity.setUserCode(PrincipalContext.getUserCode());
```

- [ ] **步骤 4：Commit**

```bash
git add iam-admin/src/main/java/com/wkclz/iam/admin/rest/UserMenuRest.java
git commit -m "refactor(iam-admin): UserMenuRest 改用 PrincipalContext"
```

---

## 任务 12：LoggingFilter 改造

**文件：**
- 修改：`iam-sdk/src/main/java/com/wkclz/iam/sdk/filter/LoggingFilter.java`

- [ ] **步骤 1：修改 imports**

在 `iam-sdk/src/main/java/com/wkclz/iam/sdk/filter/LoggingFilter.java` 中：

1. 删除：
```java
import com.wkclz.iam.sdk.facade.SsoFacade;
import com.wkclz.iam.sdk.helper.SessionHelper;
import com.wkclz.iam.sdk.bean.UserSession;
```

2. 新增：
```java
import com.wkclz.iam.contract.bean.Principal;
import com.wkclz.iam.contract.context.PrincipalContext;
import com.wkclz.iam.contract.facade.SsoFacadeContract;
import com.wkclz.iam.contract.bean.RequestLog as ContractRequestLog;
```

注意：Java 不支持 import as 语法。由于契约层和 iam-sdk 都有 `RequestLog` 类，需要用全限定名区分。不新增 import，在代码中用全限定名 `com.wkclz.iam.contract.bean.RequestLog`。

最终新增：
```java
import com.wkclz.iam.contract.bean.Principal;
import com.wkclz.iam.contract.context.PrincipalContext;
import com.wkclz.iam.contract.facade.SsoFacadeContract;
```

- [ ] **步骤 2：修改字段注入**

将第 58-59 行：
```java
@Autowired(required = false)
private SsoFacade ssoFacade;
```

替换为：
```java
@Autowired(required = false)
private SsoFacadeContract ssoFacadeContract;
```

- [ ] **步骤 3：修改 finally 块中的用户信息读取**

将第 87-92 行：
```java
UserSession user = SessionHelper.getUserSession(request);
if (user != null) {
    log.setUserCode(user.getUserCode());
    log.setUsername(user.getUsername());
    log.setNickname(user.getNickname());
}
```

替换为：
```java
Principal principal = PrincipalContext.getPrincipal(request);
if (principal != null) {
    log.setUserCode(principal.getUserCode());
    log.setUsername(principal.getUsername());
    log.setNickname(principal.getNickname());
}
```

- [ ] **步骤 4：修改 fetchRequestLog 方法**

将第 151-153 行：
```java
log.setToken(SessionHelper.getToken(request));
log.setTenantCode(SessionHelper.getTenantCode());
log.setAppCode(SessionHelper.getAppCode(request));
```

替换为：
```java
log.setToken(PrincipalContext.getToken());
log.setTenantCode(PrincipalContext.getTenantCode());
log.setAppCode(PrincipalContext.getAppCode());
```

注意：`PrincipalContext.getTenantCode()` 从请求头 `tenant-code` 读取，与旧 `SessionHelper.getTenantCode()` 返回硬编码 "default" 行为不同。这是预期的改进。

- [ ] **步骤 5：修改 saveResponseLog 方法**

将第 237-247 行 `saveResponseLog` 方法中的 `ssoFacade.saveLog(log)` 调用改为契约层调用。

将整个 `saveResponseLog` 方法替换为：

```java
private void saveResponseLog(RequestLog log) {
    if (ssoFacadeContract == null) {
        return;
    }
    subLog(log);
    ThreadUtil.execAsync(() -> {
        try {
            com.wkclz.iam.contract.bean.RequestLog contractLog = convertToContractRequestLog(log);
            ssoFacadeContract.saveLog(contractLog);
        } catch (Exception e) {
            logger.error("save request log error: log: {}, error: {}", log, e.getMessage());
        }
    });
}

private com.wkclz.iam.contract.bean.RequestLog convertToContractRequestLog(RequestLog log) {
    com.wkclz.iam.contract.bean.RequestLog contractLog = new com.wkclz.iam.contract.bean.RequestLog();
    contractLog.setUri(log.getRequestUri());
    contractLog.setMethod(log.getMethod());
    contractLog.setRequestBody(log.getRequestBody());
    contractLog.setResponseStatus(log.getHttpStatus());
    contractLog.setResponseBody(log.getResponseBody());
    contractLog.setDuration(log.getCostTime());
    contractLog.setClientIp(log.getRemoteAddr());
    contractLog.setUserCode(log.getUserCode());
    contractLog.setAppCode(log.getAppCode());
    return contractLog;
}
```

- [ ] **步骤 6：Commit**

```bash
git add iam-sdk/src/main/java/com/wkclz/iam/sdk/filter/LoggingFilter.java
git commit -m "refactor(iam-sdk): LoggingFilter 改用 SsoFacadeContract 和 PrincipalContext"
```

---

## 任务 13：删除旧抽象

**文件：**
- 删除：11 个文件

- [ ] **步骤 1：删除 iam-sdk 旧文件**

删除以下 9 个文件：
```
iam-sdk/src/main/java/com/wkclz/iam/sdk/filter/IamAuthFilter.java
iam-sdk/src/main/java/com/wkclz/iam/sdk/service/IamSsoService.java
iam-sdk/src/main/java/com/wkclz/iam/sdk/helper/SessionHelper.java
iam-sdk/src/main/java/com/wkclz/iam/sdk/bean/UserJwt.java
iam-sdk/src/main/java/com/wkclz/iam/sdk/bean/UserSession.java
iam-sdk/src/main/java/com/wkclz/iam/sdk/facade/SsoFacade.java
iam-sdk/src/main/java/com/wkclz/iam/sdk/facade/impl/SsoFacadeImpl.java
iam-sdk/src/main/java/com/wkclz/iam/sdk/bean/req/SessionCreateReq.java
iam-sdk/src/main/java/com/wkclz/iam/sdk/bean/resp/LoginResp.java
```

- [ ] **步骤 2：删除 iam-sso 旧文件**

删除以下 2 个文件：
```
iam-sso/src/main/java/com/wkclz/iam/sso/service/SsoFacadeImpl.java
iam-sso/src/main/java/com/wkclz/iam/sso/service/IamSsoServiceImpl.java
```

- [ ] **步骤 3：搜索残留引用**

运行搜索，确认无残留引用：

```
Grep pattern: "IamAuthFilter|IamSsoService|SessionHelper|com.wkclz.iam.sdk.bean.UserJwt|com.wkclz.iam.sdk.bean.UserSession|com.wkclz.iam.sdk.facade.SsoFacade|com.wkclz.iam.sdk.bean.req.SessionCreateReq|com.wkclz.iam.sdk.bean.resp.LoginResp"
```

如果有残留引用，逐一修复。

- [ ] **步骤 4：Commit**

```bash
git add -A
git commit -m "refactor: 删除旧会话抽象，迁移到契约层"
```

---

## 任务 14：编译验证

- [ ] **步骤 1：编译 iam-common**

运行：`mvn compile -pl iam-common -am`
预期：BUILD SUCCESS

- [ ] **步骤 2：编译 iam-sdk**

运行：`mvn compile -pl iam-sdk -am`
预期：BUILD SUCCESS

如果有编译错误，根据错误信息修复（常见：遗漏的 import、类型不匹配）。

- [ ] **步骤 3：编译 iam-sso**

运行：`mvn compile -pl iam-sso -am`
预期：BUILD SUCCESS

- [ ] **步骤 4：编译 iam-admin**

运行：`mvn compile -pl iam-admin -am`
预期：BUILD SUCCESS

- [ ] **步骤 5：编译全部**

运行：`mvn compile`
预期：BUILD SUCCESS

如有编译错误，修复后重新编译。

- [ ] **步骤 6：Commit（如有修复）**

```bash
git add -A
git commit -m "fix: 编译错误修复"
```

---

## 任务 15：JwtAuthContract 单元测试

**文件：**
- 创建：`iam-sdk/src/test/java/com/wkclz/iam/sdk/contract/JwtAuthContractTest.java`

- [ ] **步骤 1：创建测试类**

创建文件 `iam-sdk/src/test/java/com/wkclz/iam/sdk/contract/JwtAuthContractTest.java`：

```java
package com.wkclz.iam.sdk.contract;

import com.wkclz.iam.contract.bean.AuthResult;
import com.wkclz.iam.contract.bean.Session;
import com.wkclz.iam.contract.config.ContractSettings;
import com.wkclz.iam.contract.exception.AuthException;
import com.wkclz.iam.contract.exception.AuthException.AuthErrorType;
import com.wkclz.iam.sdk.bean.UserJwt;
import com.wkclz.iam.sdk.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthContractTest {

    private static final String SECRET_KEY = "qwertyuioplkjhgfdsazxcvbnmqwertyuioplkjhgfdsazxcvbnm";

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private JwtAuthContract jwtAuthContract;

    @BeforeEach
    void setUp() {
        ContractSettings.setJwtSecretKey(SECRET_KEY);
    }

    @Test
    void authenticate_noToken_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AuthResult result = jwtAuthContract.authenticate(request);
        assertNull(result);
    }

    @Test
    void authenticate_validToken_returnsAuthResult() {
        UserJwt userJwt = new UserJwt();
        userJwt.setUserCode("user_001");
        userJwt.setUsername("admin");
        userJwt.setNickname("管理员");
        userJwt.setAvatar("avatar.png");
        String token = JwtUtil.generateToken(userJwt, SECRET_KEY);

        Session session = new Session();
        session.setUserCode("user_001");
        session.setAuthType("PASSWORD");
        session.setAuthIdentifier("admin");
        String sessionJson = com.alibaba.fastjson2.JSON.toJSONString(session);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(sessionJson);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        AuthResult result = jwtAuthContract.authenticate(request);
        assertNotNull(result);
        assertNotNull(result.getPrincipal());
        assertEquals("user_001", result.getPrincipal().getUserCode());
        assertEquals("admin", result.getPrincipal().getUsername());
        assertEquals("admin", result.getPrincipal().getAuthIdentifier());
        assertNotNull(result.getSession());
        assertEquals("PASSWORD", result.getSession().getAuthType());
    }

    @Test
    void authenticate_expiredToken_throwsTOKEN_EXPIRED() {
        UserJwt userJwt = new UserJwt();
        userJwt.setUsername("admin");
        String token = JwtUtil.generateToken(userJwt, SECRET_KEY, -1);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        AuthException ex = assertThrows(AuthException.class,
                () -> jwtAuthContract.authenticate(request));
        assertEquals(AuthErrorType.TOKEN_EXPIRED, ex.getErrorType());
    }

    @Test
    void authenticate_sessionNotExists_throwsSESSION_EXPIRED() {
        UserJwt userJwt = new UserJwt();
        userJwt.setUsername("admin");
        String token = JwtUtil.generateToken(userJwt, SECRET_KEY);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        AuthException ex = assertThrows(AuthException.class,
                () -> jwtAuthContract.authenticate(request));
        assertEquals(AuthErrorType.SESSION_EXPIRED, ex.getErrorType());
    }

    @Test
    void checkToken_nullToken_throwsTOKEN_MISSING() {
        AuthException ex = assertThrows(AuthException.class,
                () -> jwtAuthContract.checkToken(null, "admin"));
        assertEquals(AuthErrorType.TOKEN_MISSING, ex.getErrorType());
    }

    @Test
    void checkToken_blankToken_throwsTOKEN_MISSING() {
        AuthException ex = assertThrows(AuthException.class,
                () -> jwtAuthContract.checkToken("  ", "admin"));
        assertEquals(AuthErrorType.TOKEN_MISSING, ex.getErrorType());
    }

    @Test
    void checkToken_invalidFormat_throwsTOKEN_INVALID() {
        AuthException ex = assertThrows(AuthException.class,
                () -> jwtAuthContract.checkToken("invalid.jwt.token", "admin"));
        assertEquals(AuthErrorType.TOKEN_INVALID, ex.getErrorType());
    }

    @Test
    void checkToken_authIdentifierMismatch_throwsTOKEN_INVALID() {
        UserJwt userJwt = new UserJwt();
        userJwt.setUsername("admin");
        String token = JwtUtil.generateToken(userJwt, SECRET_KEY);

        Session session = new Session();
        session.setAuthIdentifier("admin");
        String sessionJson = com.alibaba.fastjson2.JSON.toJSONString(session);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(sessionJson);

        AuthException ex = assertThrows(AuthException.class,
                () -> jwtAuthContract.checkToken(token, "other_user"));
        assertEquals(AuthErrorType.TOKEN_INVALID, ex.getErrorType());
    }

    @Test
    void checkToken_authIdentifierMatchesUsername_returnsSession() {
        UserJwt userJwt = new UserJwt();
        userJwt.setUsername("admin");
        String token = JwtUtil.generateToken(userJwt, SECRET_KEY);

        Session session = new Session();
        session.setAuthIdentifier("admin");
        String sessionJson = com.alibaba.fastjson2.JSON.toJSONString(session);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(sessionJson);

        Session result = jwtAuthContract.checkToken(token, "admin");
        assertNotNull(result);
        assertEquals("admin", result.getAuthIdentifier());
    }

    @Test
    void checkToken_authIdentifierNull_skipsValidation() {
        UserJwt userJwt = new UserJwt();
        userJwt.setUsername("admin");
        String token = JwtUtil.generateToken(userJwt, SECRET_KEY);

        Session session = new Session();
        session.setAuthIdentifier("admin");
        String sessionJson = com.alibaba.fastjson2.JSON.toJSONString(session);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(sessionJson);

        Session result = jwtAuthContract.checkToken(token, null);
        assertNotNull(result);
    }

    @Test
    void checkToken_redisException_throwsRuntimeException() {
        UserJwt userJwt = new UserJwt();
        userJwt.setUsername("admin");
        String token = JwtUtil.generateToken(userJwt, SECRET_KEY);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis down"));

        assertThrows(RuntimeException.class,
                () -> jwtAuthContract.checkToken(token, "admin"));
    }
}
```

- [ ] **步骤 2：运行测试验证通过**

运行：`mvn test -pl iam-sdk -Dtest=JwtAuthContractTest`
预期：所有测试 PASS

- [ ] **步骤 3：Commit**

```bash
git add iam-sdk/src/test/java/com/wkclz/iam/sdk/contract/JwtAuthContractTest.java
git commit -m "test(iam-sdk): JwtAuthContract 单元测试"
```

---

## 任务 16：IamSessionService 单元测试

**文件：**
- 创建：`iam-sso/src/test/java/com/wkclz/iam/sso/service/IamSessionServiceTest.java`

- [ ] **步骤 1：创建测试类**

创建文件 `iam-sso/src/test/java/com/wkclz/iam/sso/service/IamSessionServiceTest.java`：

```java
package com.wkclz.iam.sso.service;

import com.wkclz.iam.contract.bean.Principal;
import com.wkclz.iam.contract.bean.Session;
import com.wkclz.iam.contract.config.ContractSettings;
import com.wkclz.iam.sdk.bean.UserJwt;
import com.wkclz.iam.sdk.util.JwtUtil;
import com.wkclz.iam.sso.config.IamSsoConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IamSessionServiceTest {

    private static final String SECRET_KEY = "qwertyuioplkjhgfdsazxcvbnmqwertyuioplkjhgfdsazxcvbnm";

    @Mock
    private IamSsoConfig iamSsoConfig;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private IamSessionService iamSessionService;

    @BeforeEach
    void setUp() {
        ContractSettings.setJwtSecretKey(SECRET_KEY);
    }

    @Test
    void createSession_setsRedisAndZSet() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        Principal principal = new Principal();
        principal.setUsername("admin");
        principal.setUserCode("user_001");

        Session session = new Session();
        session.setAuthType("PASSWORD");
        session.setAuthIdentifier("admin");

        String token = "test_token";

        iamSessionService.createSession(token, principal, session);

        verify(valueOperations).set(anyString(), anyString(), eq(JwtUtil.SESSION_TTL_SECONDS), eq(TimeUnit.SECONDS));
        verify(zSetOperations).add(anyString(), anyString(), anyLong());
    }

    @Test
    void enforceMaxConcurrentSessions_underLimit_noKick() {
        when(iamSsoConfig.getMaxConcurrentSessions()).thenReturn(5);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.size(anyString())).thenReturn(3L);

        iamSessionService.enforceMaxConcurrentSessions("admin");

        verify(zSetOperations, never()).range(anyString(), anyLong(), anyLong());
    }

    @Test
    void enforceMaxConcurrentSessions_disabled_noOp() {
        when(iamSsoConfig.getMaxConcurrentSessions()).thenReturn(0);

        iamSessionService.enforceMaxConcurrentSessions("admin");

        verify(redisTemplate, never()).opsForZSet();
    }

    @Test
    void logout_validToken_deletesFromRedis() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        UserJwt userJwt = new UserJwt();
        userJwt.setUsername("admin");
        String token = JwtUtil.generateToken(userJwt, SECRET_KEY);

        iamSessionService.logout(token);

        verify(redisTemplate).delete(anyString());
        verify(zSetOperations).remove(anyString(), anyString());
    }

    @Test
    void logout_blankToken_noOp() {
        iamSessionService.logout("");

        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void logout_invalidToken_noOp() {
        iamSessionService.logout("invalid.jwt.token");

        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void invalidateAllSessions_deletesAllKeys() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.range(anyString(), eq(0L), eq(-1L)))
                .thenReturn(Set.of("token_md5_1", "token_md5_2"));

        iamSessionService.invalidateAllSessions("admin");

        verify(redisTemplate).delete(anyList());
        verify(redisTemplate).delete(anyString());
    }
}
```

- [ ] **步骤 2：运行测试验证通过**

运行：`mvn test -pl iam-sso -Dtest=IamSessionServiceTest`
预期：所有测试 PASS

- [ ] **步骤 3：Commit**

```bash
git add iam-sso/src/test/java/com/wkclz/iam/sso/service/IamSessionServiceTestTest.java
git commit -m "test(iam-sso): IamSessionService 单元测试"
```

---

## 任务 17：LocalSsoFacadeContract 单元测试

**文件：**
- 创建：`iam-sso/src/test/java/com/wkclz/iam/sso/contract/LocalSsoFacadeContractTest.java`

- [ ] **步骤 1：创建测试类**

创建文件 `iam-sso/src/test/java/com/wkclz/iam/sso/contract/LocalSsoFacadeContractTest.java`：

```java
package com.wkclz.iam.sso.contract;

import com.wkclz.iam.contract.bean.Principal;
import com.wkclz.iam.contract.bean.Session;
import com.wkclz.iam.contract.bean.req.SessionCreateReq;
import com.wkclz.iam.contract.bean.resp.LoginResp;
import com.wkclz.iam.contract.config.ContractSettings;
import com.wkclz.iam.sso.service.IamSessionService;
import com.wkclz.iam.sso.mapper.SsoLoginLogMapper;
import com.wkclz.iam.sso.service.IamRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocalSsoFacadeContractTest {

    private static final String SECRET_KEY = "qwertyuioplkjhgfdsazxcvbnmqwertyuioplkjhgfdsazxcvbnm";

    @Mock
    private IamSessionService iamSessionService;
    @Mock
    private IamRequestService requestLogService;
    @Mock
    private SsoLoginLogMapper ssoLoginLogMapper;

    @InjectMocks
    private LocalSsoFacadeContract localSsoFacadeContract;

    @BeforeEach
    void setUp() {
        ContractSettings.setJwtSecretKey(SECRET_KEY);
    }

    @Test
    void login_success_returnsSuccessResp() {
        SessionCreateReq req = new SessionCreateReq();
        req.setUserCode("user_001");
        req.setUsername("admin");
        req.setNickname("管理员");
        req.setAvatar("avatar.png");
        req.setAuthType("PASSWORD");
        req.setAuthIdentifier("admin");
        req.setClientIp("127.0.0.1");
        req.setUserAgent("test-agent");

        LoginResp resp = localSsoFacadeContract.login(req);

        assertTrue(resp.getSuccess());
        assertNotNull(resp.getToken());
        assertEquals("user_001", resp.getUserCode());
        assertEquals("admin", resp.getUsername());
        assertEquals("管理员", resp.getNickname());
        assertEquals("avatar.png", resp.getAvatar());
    }

    @Test
    void login_callsCreateSession() {
        SessionCreateReq req = new SessionCreateReq();
        req.setUsername("admin");
        req.setUserCode("user_001");
        req.setAuthType("PASSWORD");
        req.setAuthIdentifier("admin");

        localSsoFacadeContract.login(req);

        verify(iamSessionService).createSession(anyString(), any(Principal.class), any(Session.class));
    }

    @Test
    void login_callsEnforceMaxConcurrentSessions() {
        SessionCreateReq req = new SessionCreateReq();
        req.setUsername("admin");
        req.setUserCode("user_001");
        req.setAuthType("PASSWORD");
        req.setAuthIdentifier("admin");

        localSsoFacadeContract.login(req);

        verify(iamSessionService).enforceMaxConcurrentSessions("admin");
    }

    @Test
    void login_recordsLoginLog() {
        SessionCreateReq req = new SessionCreateReq();
        req.setUsername("admin");
        req.setUserCode("user_001");
        req.setAuthType("PASSWORD");
        req.setAuthIdentifier("admin");
        req.setClientIp("127.0.0.1");
        req.setUserAgent("test-agent");

        localSsoFacadeContract.login(req);

        verify(ssoLoginLogMapper).insertLoginLog(any());
    }

    @Test
    void logout_delegatesToIamSessionService() {
        localSsoFacadeContract.logout("test_token");
        verify(iamSessionService).logout("test_token");
    }

    @Test
    void saveLog_delegatesToRequestLogService() {
        com.wkclz.iam.contract.bean.RequestLog log = new com.wkclz.iam.contract.bean.RequestLog();
        log.setUri("/test");
        log.setMethod("GET");

        localSsoFacadeContract.saveLog(log);

        verify(requestLogService).insertLog(any(com.wkclz.iam.sdk.bean.RequestLog.class));
    }
}
```

- [ ] **步骤 2：运行测试验证通过**

运行：`mvn test -pl iam-sso -Dtest=LocalSsoFacadeContractTest`
预期：所有测试 PASS

注意：任务 13 会删除 `com.wkclz.iam.sdk.bean.RequestLog`，但本测试在任务 13 之前执行，此时该类仍存在。如果任务顺序调整，需要同步调整测试中的引用。

实际上，任务 13 删除的是 `UserJwt`/`UserSession`/`SessionHelper` 等，`RequestLog` 不在删除列表中。`LoggingFilter` 仍使用 iam-sdk `RequestLog`，所以保留。此测试安全。

- [ ] **步骤 3：Commit**

```bash
git add iam-sso/src/test/java/com/wkclz/iam/sso/contract/LocalSsoFacadeContractTest.java
git commit -m "test(iam-sso): LocalSsoFacadeContract 单元测试"
```

---

## 任务 18：最终编译与测试验证

- [ ] **步骤 1：全量编译**

运行：`mvn compile`
预期：BUILD SUCCESS

- [ ] **步骤 2：全量测试**

运行：`mvn test`
预期：所有测试 PASS

- [ ] **步骤 3：如有失败，修复并重新验证**

如果编译或测试失败，根据错误信息修复。常见问题：
- 遗漏的 import
- `UserJwt` 被删除但 `JwtUtil` 仍引用（`UserJwt` 不在删除列表，应保留）
- `RequestLog` 全限定名引用错误

- [ ] **步骤 4：Commit（如有修复）**

```bash
git add -A
git commit -m "fix: 最终编译测试修复"
```

---

## 任务 19：更新 AGENTS.md

**文件：**
- 修改：`AGENTS.md`

- [ ] **步骤 1：更新 AGENTS.md 模块结构**

在 `AGENTS.md` 中更新 iam-sdk 和 iam-sso 的类索引，反映：
- iam-sdk 新增 `contract` 包（JwtAuthContract + HttpSsoFacadeContract）
- iam-sso 新增 `contract` 包（LocalSsoFacadeContract）
- 删除的类从索引中移除
- `IamSessionService` 新增方法说明

- [ ] **步骤 2：Commit**

```bash
git add AGENTS.md
git commit -m "docs: 更新 AGENTS.md 反映契约层改造"
```

---

## 自检结果

### 1. 规格覆盖度

- ✅ JwtAuthContract 实现（任务 4）
- ✅ LocalSsoFacadeContract 实现（任务 6）
- ✅ HttpSsoFacadeContract 实现（任务 7）
- ✅ IamLoginService 改造（任务 8）
- ✅ IamSessionService 重构（任务 5）
- ✅ LoggingFilter 改造（任务 12）
- ✅ SessionHelper 调用方迁移（任务 10、11）
- ✅ IamSdkConfig 精简（任务 2）
- ✅ pom.xml 依赖调整（任务 1）
- ✅ AkSignHelper 改造（任务 3）
- ✅ 删除旧抽象（任务 13）
- ✅ LoginRest 改造（任务 9）
- ✅ 单元测试（任务 15、16、17）
- ✅ 编译验证（任务 14、18）

### 2. 占位符扫描

无占位符。所有步骤包含完整代码。

### 3. 类型一致性

- `JwtAuthContract` 使用 `ContractSettings.getJwtSecretKey()` ✓
- `IamSessionService` 使用 `ContractSettings.getJwtSecretKey()` ✓
- `LocalSsoFacadeContract` 使用 `ContractSettings.getJwtSecretKey()` ✓
- `Principal` 构建 5 个字段（含 authIdentifier）✓
- `Session` 构建 3 个字段 ✓
- `LoginResp.success()` 签名一致 ✓
- `LoginResp.fail()` 签名一致 ✓
- `SsoFacadeContract` 接口方法一致 ✓
- `AuthContract` 接口方法一致 ✓

### 4. 重要注意事项

1. **`UserJwt` 保留**：虽然 `UserJwt` 是旧模型，但 `JwtUtil` 仍依赖它（内部工具），不删除。仅删除对外暴露的 `UserSession`/`SessionHelper` 等。
2. **iam-sdk `RequestLog` 保留**：`LoggingFilter` 仍使用 iam-sdk `RequestLog`（完整字段），仅在与契约层交互时转换。
3. **`LoginStatus` 保留**：日志记录仍用 `LoginStatus`（日志表结构不变），仅对外 API 用 `LoginFailType`。
4. **`JwtUtil` 保留**：作为 iam-sdk 内部工具，`UserJwt` 作为内部模型。
5. **`AkSignHelper` 保留**：`HttpSsoFacadeContract` 依赖它，`AkSignContract` 不在本次范围。
6. **`JwtValidationException` 保留**：`JwtUtil` 内部使用。
7. **过滤器顺序**：`LoggingFilter`（`Integer.MIN_VALUE + 1`）先于 `DefaultAuthFilter`（`HIGHEST_PRECEDENCE + 10`）执行，LoggingFilter 的 finally 后执行。用 `PrincipalContext.getPrincipal(request)` 从 request attribute 读，不依赖 ThreadLocal。
