# IAM 契约层实现计划（v2 — 契约层优化后）

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 为 sh-iam-contract-api 提供 AuthContract + SsoFacadeContract 的具体实现，替换 iam-sdk/iam-sso 中旧的会话抽象。

**架构：** iam-sdk 实现 `JwtAuthContract`（仅 `authenticate` + `doAuthenticate`）+ `HttpSsoFacadeContract`；iam-sso 实现 `LocalSsoFacadeContract`；`checkToken` 由契约层 default 模板方法处理。

**技术栈：** Spring Boot 3.x, Java 25, jjwt, fastjson2, Redis, MyBatis

**契约层优化前提**：AuthErrorType 含 httpStatus + fromJwtErrorCode()、AuthContract 含 doAuthenticate + checkToken 模板、RequestLog 字段完整、SessionCreateReq 已删除 clientIp/userAgent、FilterOrder 常量、JwtErrorCodes 常量。

---

## 文件结构

### 创建文件
- `iam-sdk/src/main/java/com/wkclz/iam/sdk/contract/JwtAuthContract.java`
- `iam-sdk/src/main/java/com/wkclz/iam/sdk/contract/HttpSsoFacadeContract.java`
- `iam-sso/src/main/java/com/wkclz/iam/sso/contract/LocalSsoFacadeContract.java`

### 修改文件
- `iam-sdk/pom.xml`
- `iam-sdk/src/main/java/com/wkclz/iam/sdk/config/IamSdkConfig.java`
- `iam-sdk/src/main/java/com/wkclz/iam/sdk/helper/AkSignHelper.java`
- `iam-sdk/src/main/java/com/wkclz/iam/sdk/filter/LoggingFilter.java`
- `iam-sso/src/main/java/com/wkclz/iam/sso/service/IamSessionService.java`
- `iam-sso/src/main/java/com/wkclz/iam/sso/service/IamLoginService.java`
- `iam-sso/src/main/java/com/wkclz/iam/sso/rest/LoginRest.java`
- `iam-sso/src/main/java/com/wkclz/iam/sso/rest/UserInfoRest.java`
- `iam-admin/src/main/java/com/wkclz/iam/admin/rest/UserMenuRest.java`
- `iam-sso/src/main/java/com/wkclz/iam/sso/service/IamRequestService.java`
- `AGENTS.md`

### 删除文件（12 个）
- `iam-sdk`: IamAuthFilter, IamSsoService, SessionHelper, UserJwt, UserSession, SsoFacade, SsoFacadeImpl, SessionCreateReq, LoginResp, **RequestLog**
- `iam-sso`: SsoFacadeImpl, IamSsoServiceImpl

---

## 任务 1：iam-sdk pom.xml 依赖调整

**文件：** 修改 `iam-sdk/pom.xml`

- [ ] **步骤 1：新增契约层依赖**

在 `<dependencies>` 中添加：

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

- [ ] **步骤 2：Commit**

```bash
git add iam-sdk/pom.xml
git commit -m "feat(iam-sdk): 新增 iam-contract-api/default 依赖"
```

---

## 任务 2：IamSdkConfig 精简

**文件：** 修改 `iam-sdk/src/main/java/com/wkclz/iam/sdk/config/IamSdkConfig.java`

- [ ] **步骤 1：精简 IamSdkConfig**

完整替换为只保留 appCode / staticEnabled / staticSubfix，删除 enabled / jwtSecretKey / serverUrl / appId / appSecret 字段和 getCasFacade() Bean。

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

- [ ] **步骤 2：Commit**

```bash
git add iam-sdk/src/main/java/com/wkclz/iam/sdk/config/IamSdkConfig.java
git commit -m "refactor(iam-sdk): 精简 IamSdkConfig，删除与契约层重复的配置字段"
```

---

## 任务 3：AkSignHelper 移除 IamSdkConfig 依赖

**文件：** 修改 `iam-sdk/src/main/java/com/wkclz/iam/sdk/helper/AkSignHelper.java`

- [ ] **步骤 1：删除字段**

1. 删除 `import com.wkclz.iam.sdk.config.IamSdkConfig;` 和 `@Autowired private IamSdkConfig config;` 字段
2. 字段 `private String serverUrl;` 也一样删除（改用 ContractSettings）
3. `verifySign` 方法中也不依赖 config/serverUrl 字段，可直接删除

- [ ] **步骤 2：Commit**

```bash
git add iam-sdk/src/main/java/com/wkclz/iam/sdk/helper/AkSignHelper.java
git commit -m "refactor(iam-sdk): AkSignHelper 移除 IamSdkConfig 依赖"
```

---

## 任务 4：IamSessionService 重构

**文件：** 修改 `iam-sso/src/main/java/com/wkclz/iam/sso/service/IamSessionService.java`

- [ ] **步骤 1：重构**

完整替换文件，新增 `createSession` 和 `enforceMaxConcurrentSessions` 方法。关键变化：
- `createSession(token, principal, session)` — Redis SET + ZSET 注册
- `enforceMaxConcurrentSessions(username)` — 超过上限踢最早会话
- `logout(token)` — 改用 `ContractSettings.getJwtSecretKey()` 替代 `IamSdkConfig`
- `invalidateAllSessions` — 保持现有逻辑

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
        log.info("用户 {} 的所有会话已失效，共清理 {} 个会话", username,
                tokenMd5Set == null ? 0 : tokenMd5Set.size());
    }
}
```

- [ ] **步骤 2：Commit**

```bash
git add iam-sso/src/main/java/com/wkclz/iam/sso/service/IamSessionService.java
git commit -m "refactor(iam-sso): IamSessionService 抽离 createSession/enforceMaxConcurrentSessions 方法"
```

---

## 任务 5：JwtAuthContract 实现（简化版）

**文件：** 创建 `iam-sdk/src/main/java/com/wkclz/iam/sdk/contract/JwtAuthContract.java`

- [ ] **步骤 1：创建 JwtAuthContract**

只需实现 `authenticate` + `doAuthenticate`。`checkToken` 由契约层 default 模板方法处理。

```java
package com.wkclz.iam.sdk.contract;

import com.alibaba.fastjson2.JSON;
import com.wkclz.iam.contract.bean.AuthResult;
import com.wkclz.iam.contract.bean.Principal;
import com.wkclz.iam.contract.bean.Session;
import com.wkclz.iam.contract.config.ContractSettings;
import com.wkclz.iam.contract.context.PrincipalContext;
import com.wkclz.iam.contract.enums.AuthErrorType;
import com.wkclz.iam.contract.exception.AuthException;
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
        return doAuthenticate(token);
    }

    @Override
    public AuthResult doAuthenticate(String token) {
        UserJwt userJwt;
        try {
            userJwt = JwtUtil.parseToken(token, ContractSettings.getJwtSecretKey());
        } catch (JwtValidationException e) {
            throw new AuthException(
                    AuthErrorType.fromJwtErrorCode(e.getErrorCode()),
                    e.getMessage(), e);
        }

        String username = userJwt.getUsername();
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

        Session session;
        try {
            session = JSON.parseObject(sessionJson, Session.class);
        } catch (Exception e) {
            log.error("Session 反序列化失败, username={}", username, e);
            throw new AuthException(AuthErrorType.TOKEN_INVALID, "会话数据损坏", e);
        }

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
}
```

- [ ] **步骤 2：Commit**

```bash
git add iam-sdk/src/main/java/com/wkclz/iam/sdk/contract/JwtAuthContract.java
git commit -m "feat(iam-sdk): 实现 JwtAuthContract 认证契约（仅 authenticate + doAuthenticate）"
```

---

## 任务 6：LocalSsoFacadeContract 实现

**文件：** 创建 `iam-sso/src/main/java/com/wkclz/iam/sso/contract/LocalSsoFacadeContract.java`

- [ ] **步骤 1：创建 LocalSsoFacadeContract**

关键变化：
- `recordLoginLog()` 通过 `RequestHelper.getRequest()` 获取 IP/UA（`SessionCreateReq` 已删除这两个字段）
- `saveLog()` 直接委托 `IamRequestService`，无需转换（统一用契约层 `RequestLog`）

```java
package com.wkclz.iam.sso.contract;

import com.wkclz.iam.common.entity.IamLoginLog;
import com.wkclz.iam.contract.bean.Principal;
import com.wkclz.iam.contract.bean.RequestLog;
import com.wkclz.iam.contract.bean.Session;
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

        Principal principal = new Principal();
        principal.setUserCode(req.getUserCode());
        principal.setUsername(req.getUsername());
        principal.setNickname(req.getNickname());
        principal.setAvatar(req.getAvatar());
        principal.setAuthIdentifier(req.getAuthIdentifier());

        Session session = new Session();
        session.setUserCode(req.getUserCode());
        session.setAuthType(req.getAuthType());
        session.setAuthIdentifier(req.getAuthIdentifier());

        UserJwt userJwt = new UserJwt();
        userJwt.setUserCode(req.getUserCode());
        userJwt.setUsername(req.getUsername());
        userJwt.setNickname(req.getNickname());
        userJwt.setAvatar(req.getAvatar());
        String token = JwtUtil.generateToken(userJwt, ContractSettings.getJwtSecretKey());

        iamSessionService.createSession(token, principal, session);
        iamSessionService.enforceMaxConcurrentSessions(req.getUsername());

        recordLoginLog(req);

        return LoginResp.success(token, req.getUserCode(), req.getUsername(),
                req.getNickname(), req.getAvatar());
    }

    @Override
    public void saveLog(RequestLog log) {
        // 直接委托，无需转换（iam-sdk RequestLog 已删除，统一用契约层）
        requestLogService.insertLog(log);
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

        // 从请求上下文获取 IP/UA（SessionCreateReq 已删除这两个字段）
        HttpServletRequest request = RequestHelper.getRequest();
        if (request != null) {
            loginLog.setIpAddress(IpHelper.getOriginIp(request));
            loginLog.setUserAgent(request.getHeader("User-Agent"));
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

**文件：** 创建 `iam-sdk/src/main/java/com/wkclz/iam/sdk/contract/HttpSsoFacadeContract.java`

- [ ] **步骤 1：创建 HttpSsoFacadeContract**

与旧 SsoFacadeImpl（iam-sdk）功能一致，改用 `ContractSettings` 读配置，`@ConditionalOnMissingBean` 注册。

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
        if (StringUtils.isBlank(token)) return;
        try {
            postData("/logout", token);
        } catch (Exception e) {
            log.error("远程登出失败: {}", e.getMessage());
        }
    }

    private void postData(String uri, Object data) {
        if (StringUtils.isBlank(uri) || data == null) return;
        String url = ContractSettings.getServerUrl() + URI_PREFIX + uri;
        String sign = AkSignHelper.sign(ContractSettings.getAppId(), ContractSettings.getAppSecret());
        HttpRequest post = HttpUtil.createPost(url);
        post.header("app-id", ContractSettings.getAppId());
        post.header("sign", sign);
        post.body(JSONObject.toJSONString(data));
        HttpResponse execute = post.execute();
        if (execute.getStatus() != 200) {
            throw SystemException.of("请求{}异常: {}", uri, execute.getStatus());
        }
    }

    private String postDataWithResponse(String uri, Object data) {
        if (StringUtils.isBlank(uri)) throw SystemException.of("uri 不能为空");
        if (data == null) throw SystemException.of("data 不能为空");
        String url = ContractSettings.getServerUrl() + URI_PREFIX + uri;
        String sign = AkSignHelper.sign(ContractSettings.getAppId(), ContractSettings.getAppSecret());
        HttpRequest post = HttpUtil.createPost(url);
        post.header("app-id", ContractSettings.getAppId());
        post.header("sign", sign);
        post.body(JSONObject.toJSONString(data));
        HttpResponse execute = post.execute();
        if (execute.getStatus() != 200) {
            throw SystemException.of("请求{}异常: {}", uri, execute.getStatus());
        }
        return execute.body();
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

**文件：** 修改 `iam-sso/src/main/java/com/wkclz/iam/sso/service/IamLoginService.java`

- [ ] **步骤 1：修改 imports**

1. 删除：`import com.wkclz.iam.sdk.bean.enums.LoginStatus;`（仅删 LoginStatus 导入，但仍需用于 loginLog 方法——保留）
   实际删除：`import com.wkclz.iam.sdk.helper.SessionHelper;`、`import com.wkclz.iam.sdk.bean.req.SessionCreateReq;`、`import com.wkclz.iam.sdk.bean.resp.LoginResp;`、`import com.wkclz.iam.sdk.facade.SsoFacade;`

2. 新增：
```java
import com.wkclz.iam.contract.bean.req.SessionCreateReq;
import com.wkclz.iam.contract.bean.resp.LoginResp;
import com.wkclz.iam.contract.context.PrincipalContext;
import com.wkclz.iam.contract.enums.LoginFailType;
import com.wkclz.iam.contract.facade.SsoFacadeContract;
```

3. 保留 `import com.wkclz.iam.sdk.bean.enums.LoginStatus;`（loginLog 方法仍需）

- [ ] **步骤 2：修改字段**

```java
// 旧：private SsoFacade ssoFacade;
// 新：
private SsoFacadeContract ssoFacadeContract;
```

- [ ] **步骤 3：修改 loginByUsernameAndPassword**

所有 `failResp(LoginStatus.XXX)` 替换为 `LoginResp.fail(LoginFailType.YYY)`：

| 旧 | 新 |
|---|---|
| `failResp(LoginStatus.NEED_CAPTCHA)` | `LoginResp.fail(LoginFailType.CAPTCHA_REQUIRED)` |
| `failResp(LoginStatus.CAPTCHA_TIMEOUT)` | `LoginResp.fail(LoginFailType.CAPTCHA_ERROR, "验证码已过期")` |
| `failResp(LoginStatus.INVALID_CAPTCHA)` | `LoginResp.fail(LoginFailType.CAPTCHA_ERROR)` |
| `failResp(LoginStatus.USER_NOT_FOUND, "...")` | `LoginResp.fail(LoginFailType.USERNAME_OR_PASSWORD_ERROR)` |
| `failResp(LoginStatus.EXPIRED_ACCOUNT)` | `LoginResp.fail(LoginFailType.ACCOUNT_DISABLED)` |
| `failResp(LoginStatus.ACCOUNT_LOCKED)` | `LoginResp.fail(LoginFailType.ACCOUNT_LOCKED)` |
| `failResp(LoginStatus.ACCOUNT_DISABLED)` | `LoginResp.fail(LoginFailType.ACCOUNT_DISABLED)` |
| `failResp(LoginStatus.INVALID_CREDENTIALS)` | `LoginResp.fail(LoginFailType.USERNAME_OR_PASSWORD_ERROR)` |
| `failResp(LoginStatus.EXPIRED_PASSWORD)` | `LoginResp.fail(LoginFailType.CREDENTIALS_EXPIRED)` |

SessionCreateReq 构建不再设置 clientIp/userAgent：
```java
SessionCreateReq sessionCreateReq = new SessionCreateReq();
sessionCreateReq.setUserCode(auth.getUserCode());
sessionCreateReq.setUsername(auth.getUsername());
sessionCreateReq.setAuthIdentifier(auth.getAuthIdentifier());
sessionCreateReq.setNickname(auth.getNickname());
sessionCreateReq.setAvatar(auth.getAvatar());
sessionCreateReq.setAuthType(auth.getAuthType());
// 不再设置 setClientIp / setUserAgent

LoginResp response = ssoFacadeContract.login(sessionCreateReq);
```

- [ ] **步骤 4：修改 logout**

```java
public void logout(HttpServletRequest request) {
    String token = PrincipalContext.getToken();
    if (StringUtils.isBlank(token)) return;
    ssoFacadeContract.logout(token);
}
```

- [ ] **步骤 5：修改 changePassword**

```java
String userCode = PrincipalContext.getUserCode();
// ...
String username = PrincipalContext.getUsername();
```

- [ ] **步骤 6：删除 failResp 私有方法**

删除两个 `failResp` 方法。

- [ ] **步骤 7：Commit**

```bash
git add iam-sso/src/main/java/com/wkclz/iam/sso/service/IamLoginService.java
git commit -m "refactor(iam-sso): IamLoginService 改用 LoginFailType 和 SsoFacadeContract"
```

---

## 任务 9：LoginRest 改造

**文件：** 修改 `iam-sso/src/main/java/com/wkclz/iam/sso/rest/LoginRest.java`

- [ ] **步骤 1：修改 import**

删除 `import com.wkclz.iam.sdk.bean.resp.LoginResp;`，新增 `import com.wkclz.iam.contract.bean.resp.LoginResp;`

- [ ] **步骤 2：Commit**

```bash
git add iam-sso/src/main/java/com/wkclz/iam/sso/rest/LoginRest.java
git commit -m "refactor(iam-sso): LoginRest 返回契约层 LoginResp"
```

---

## 任务 10：UserInfoRest 改造

**文件：** 修改 `iam-sso/src/main/java/com/wkclz/iam/sso/rest/UserInfoRest.java`

- [ ] **步骤 1：修改 imports + userInfo 方法**

```java
// 删除
import com.wkclz.iam.sdk.bean.UserSession;
import com.wkclz.iam.sdk.helper.SessionHelper;
// 新增
import com.wkclz.iam.contract.bean.Principal;
import com.wkclz.iam.contract.context.PrincipalContext;

// userInfo 方法
@GetMapping(Route.USER_INFO)
@Operation(summary = "获取用户信息")
public R<Principal> userInfo(HttpServletRequest request) {
    Principal principal = PrincipalContext.getPrincipal(request);
    if (principal == null) {
        return R.error("用户未登录");
    }
    return R.ok(principal);
}

// userMenuTree / userMenuTreeRuoyi: SessionHelper.getAppCode(request) → PrincipalContext.getAppCode()
```

- [ ] **步骤 2：Commit**

```bash
git add iam-sso/src/main/java/com/wkclz/iam/sso/rest/UserInfoRest.java
git commit -m "refactor(iam-sso): UserInfoRest 改用 PrincipalContext，返回 Principal"
```

---

## 任务 11：UserMenuRest 改造

**文件：** 修改 `iam-admin/src/main/java/com/wkclz/iam/admin/rest/UserMenuRest.java`

- [ ] **步骤 1：SessionHelper → PrincipalContext**

```java
// 删除 import com.wkclz.iam.sdk.helper.SessionHelper;
// 新增 import com.wkclz.iam.contract.context.PrincipalContext;
// SessionHelper.getUserCode() → PrincipalContext.getUserCode()
```

- [ ] **步骤 2：Commit**

```bash
git add iam-admin/src/main/java/com/wkclz/iam/admin/rest/UserMenuRest.java
git commit -m "refactor(iam-admin): UserMenuRest 改用 PrincipalContext"
```

---

## 任务 12：LoggingFilter 改造（简化版）

**文件：** 修改 `iam-sdk/src/main/java/com/wkclz/iam/sdk/filter/LoggingFilter.java`

- [ ] **步骤 1：修改 imports**

```java
// 删除
import com.wkclz.iam.sdk.facade.SsoFacade;
import com.wkclz.iam.sdk.helper.SessionHelper;
import com.wkclz.iam.sdk.bean.UserSession;
// 新增
import com.wkclz.iam.contract.bean.Principal;
import com.wkclz.iam.contract.context.PrincipalContext;
import com.wkclz.iam.contract.facade.SsoFacadeContract;
import com.wkclz.iam.contract.config.FilterOrder;
```

- [ ] **步骤 2：修改 order**

```java
// 旧：@Order(Integer.MIN_VALUE + 1)
// 新：
@Order(FilterOrder.LOGGING)
```

- [ ] **步骤 3：修改字段**

```java
// 旧：@Autowired(required = false) private SsoFacade ssoFacade;
// 新：
@Autowired(required = false)
private SsoFacadeContract ssoFacadeContract;
```

- [ ] **步骤 4：修改 finally 块**

```java
Principal principal = PrincipalContext.getPrincipal(request);
if (principal != null) {
    log.setUserCode(principal.getUserCode());
    log.setUsername(principal.getUsername());
    log.setNickname(principal.getNickname());
}
```

- [ ] **步骤 5：修改 fetchRequestLog**

```java
log.setToken(PrincipalContext.getToken());
log.setTenantCode(PrincipalContext.getTenantCode());
log.setAppCode(PrincipalContext.getAppCode());
```

- [ ] **步骤 6：修改 saveResponseLog**（无需转换，直接传契约层 RequestLog）

```java
private void saveResponseLog(RequestLog log) {
    if (ssoFacadeContract == null) return;
    subLog(log);
    ThreadUtil.execAsync(() -> {
        try {
            ssoFacadeContract.saveLog(log);
        } catch (Exception e) {
            logger.error("save request log error: log: {}, error: {}", log, e.getMessage());
        }
    });
}
```

注意：LoggingFilter 中 `RequestLog` 现在就是契约层 `com.wkclz.iam.contract.bean.RequestLog`（iam-sdk RequestLog 已删除）。

- [ ] **步骤 7：Commit**

```bash
git add iam-sdk/src/main/java/com/wkclz/iam/sdk/filter/LoggingFilter.java
git commit -m "refactor(iam-sdk): LoggingFilter 改用 SsoFacadeContract、FilterOrder.LOGGING 和契约层 RequestLog"
```

---

## 任务 13：IamRequestService 修改

**文件：** 修改 `iam-sso/src/main/java/com/wkclz/iam/sso/service/IamRequestService.java`

- [ ] **步骤 1：修改 import**

```java
// 旧：import com.wkclz.iam.sdk.bean.RequestLog;
// 新：import com.wkclz.iam.contract.bean.RequestLog;
```

- [ ] **步骤 2：修改 insertLog 方法签名**

```java
// 旧：public void insertLog(com.wkclz.iam.sdk.bean.RequestLog requestLog)
// 新：public void insertLog(com.wkclz.iam.contract.bean.RequestLog requestLog)
```

内部与 `IamRequestLog` 实体之间的字段映射可能需要调整字段名（契约层用 `requestUri`/`httpStatus`/`remoteAddr`/`costTime`，iam-sdk 也用同名）。检查所有 setter 调用，确保字段名匹配。

- [ ] **步骤 3：Commit**

```bash
git add iam-sso/src/main/java/com/wkclz/iam/sso/service/IamRequestService.java
git commit -m "refactor(iam-sso): IamRequestService.insertLog 参数改为契约层 RequestLog"
```

---

## 任务 14：删除旧抽象（12 文件）

- [ ] **步骤 1：删除 iam-sdk（10 个文件）**

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
iam-sdk/src/main/java/com/wkclz/iam/sdk/bean/RequestLog.java
```

- [ ] **步骤 2：删除 iam-sso（2 个文件）**

```
iam-sso/src/main/java/com/wkclz/iam/sso/service/SsoFacadeImpl.java
iam-sso/src/main/java/com/wkclz/iam/sso/service/IamSsoServiceImpl.java
```

- [ ] **步骤 3：搜索残留引用**（Grep）

```
pattern: "IamAuthFilter|IamSsoService|SessionHelper|com.wkclz.iam.sdk.bean.UserJwt|com.wkclz.iam.sdk.bean.UserSession|com.wkclz.iam.sdk.facade.SsoFacade|com.wkclz.iam.sdk.bean.req.SessionCreateReq|com.wkclz.iam.sdk.bean.resp.LoginResp|com.wkclz.iam.sdk.bean.RequestLog"
```

如有残留引用，逐一修复。

- [ ] **步骤 4：Commit**

```bash
git add -A
git commit -m "refactor: 删除旧会话抽象（12 文件），迁移到契约层"
```

---

## 任务 15：编译验证

- [ ] **步骤 1：全量编译**

```bash
mvn compile
```

预期：BUILD SUCCESS。如有错误，根据错误信息修复。

- [ ] **步骤 2：Commit（如有修复）**

```bash
git add -A
git commit -m "fix: 编译错误修复"
```

---

## 任务 16：更新 AGENTS.md

**文件：** 修改 `AGENTS.md`

- [ ] **步骤 1：更新模块索引**

反映新增 `contract` 包（JwtAuthContract + HttpSsoFacadeContract + LocalSsoFacadeContract），移除已删除的类，新增 `IamSessionService` 方法说明。

- [ ] **步骤 2：Commit**

```bash
git add AGENTS.md
git commit -m "docs: 更新 AGENTS.md 反映契约层改造"
```

---

## 自检结果

### 1. 规格覆盖度

- ✅ JwtAuthContract（任务 5）— 仅 authenticate + doAuthenticate
- ✅ LocalSsoFacadeContract（任务 6）— recordLoginLog 从 RequestHelper 获取 IP/UA
- ✅ HttpSsoFacadeContract（任务 7）
- ✅ IamLoginService（任务 8）— 不设置 clientIp/userAgent
- ✅ IamSessionService（任务 4）
- ✅ LoggingFilter（任务 12）— FilterOrder.LOGGING + 契约层 RequestLog
- ✅ UserInfoRest / UserMenuRest（任务 10/11）
- ✅ IamSdkConfig（任务 2）
- ✅ IamRequestService（任务 13）— 参数改为契约层 RequestLog
- ✅ pom.xml（任务 1）
- ✅ AkSignHelper（任务 3）
- ✅ 删除（任务 14）— 12 文件
- ✅ 编译验证（任务 15）
- ✅ 文档（任务 16）

### 2. 占位符

无。

### 3. 类型一致性

- AuthErrorType 引用路径改为 `com.wkclz.iam.contract.enums.AuthErrorType` ✓
- `AuthErrorType.fromJwtErrorCode()` 签名一致 ✓
- `RequestLog` 统一为 `com.wkclz.iam.contract.bean.RequestLog` ✓
- FilterOrder.LOGGING 值为 `HIGHEST_PRECEDENCE + 1` ✓
- SessionCreateReq 无 clientIp/userAgent，不构建 ✓

### 4. 与 v1 的差异

| v1 | v2 |
|---|---|
| JwtAuthContract 实现 3 方法（authenticate + checkToken + doAuthenticate） | 仅 2 方法（authenticate + doAuthenticate） |
| checkToken 手动 8 种场景校验 | 契约层模板处理 |
| 异常映射 switch 20 行 | `AuthErrorType.fromJwtErrorCode()` 1 行 |
| LoggingFilter RequestLog 转换代码 | 无（直接删除 iam-sdk RequestLog） |
| LocalSsoFacadeContract RequestLog 反向转换 | 无（直接委托） |
| SessionCreateReq 含 clientIp/userAgent | 已删除，从 RequestHelper 获取 |
| Filter order 硬编码 Integer.MIN_VALUE + 1 | FilterOrder.LOGGING |
| 任务 19 个 | 任务 16 个 |
| 删除 11 文件 | 删除 12 文件（+ iam-sdk RequestLog） |
