# iam-sdk 模块彻底替代设计规格

> 日期：2026-07-21
> 依据：[docs/design/session-design.md](../../design/session-design.md)（会话功能重设计）
> 决策来源：用户在 brainstorming 阶段逐节确认
> 状态：待用户审查

---

## 一、目标与范围

### 1.1 目标

**彻底删除 iam-sdk 模块**，其原有职责由以下模块共同覆盖：

| 新归宿 | 覆盖的 iam-sdk 职责 |
|--------|--------------------|
| iam-session（会话管理层）| 鉴权 Filter（SessionAuthFilter）、Token 提取、白名单匹配、身份上下文设置、请求日志采集、远程 SsoFacade 调用 |
| iam-sso（单点登录层）| 密码登录编排（已实现，无需变更）|
| sh-core（框架层）| `UserIdentity` + `IdentityContext` 替代 `UserJwt` + `UserSession` + `SessionHelper` |

### 1.2 原则约束（来自用户）

1. **iam-session 只负责会话管理相关内容**，目标兼容任意登录逻辑（账号密码、小程序、GitHub OAuth 均共用 iam-session）
2. **iam-sso 只负责辅助 iam-session 实现密码登录**，iam-sso 不负责会话的任何内容
3. 任何不确定的内容，经用户确认后再执行

### 1.3 当前断裂状态（必须修复）

- 无任何模块 pom 依赖 iam-sdk（但代码有 import）
- [IamSsoServiceImpl.java](../../../iam-sso/src/main/java/com/wkclz/iam/sso/service/IamSsoServiceImpl.java) 引用不存在的 `com.wkclz.iam.sdk.service.IamSsoService` 接口 → 项目当前无法编译
- iam-sso 的 [IamSessionService.java](../../../iam-sso/src/main/java/com/wkclz/iam/sso/service/IamSessionService.java) 是会话包装类，违反原则 2

---

## 二、用户决策记录

| 决策点 | 用户选择 |
|--------|---------|
| 第二节：5 个过时类（UserJwt/UserSession/RequestLog/LogoutReq/SessionHelper）删除 | **同意** |
| 第三节：远程 SsoFacade/LoggingFilter/AkSignHelper/IamSdkConfig 归宿 | **方案 B：迁入 iam-session 作为可选子包（允许 iam-session 增加 sh-web 依赖）** |
| 第四节：iam-sso 的 IamSsoServiceImpl.java / SsoFacadeImpl.java | **删除** |
| 第四节：iam-admin UserMenuRest.java 修复 | **同意**（SessionHelper → IdentityContext）|

### 2.1 推断决策（基于原则 2 的必然结论，待用户最终确认）

| 推断项 | 理由 |
|--------|------|
| 删除 iam-sso 的 [IamSessionService.java](../../../iam-sso/src/main/java/com/wkclz/iam/sso/service/IamSessionService.java) | 删除 SsoFacadeImpl 后它成为 dead code；本身是会话包装类，违反原则 2；其 `logout` 逻辑已由 PasswordLoginService.logout() 直接调用 SessionManager.destroySession() 替代 |

---

## 三、iam-sdk 10 个类的归宿总表

| # | 类 | 当前位置 | 归宿 | 理由 |
|---|----|---------|------|------|
| 1 | `UserJwt` | iam-sdk/bean | **删除** | JWT 内部载荷模型，已被 sh-core `UserIdentity` 替代 |
| 2 | `UserSession` | iam-sdk/bean | **删除** | 旧会话上下文模型，已被 `UserIdentity` 替代 |
| 3 | `RequestLog` | iam-sdk/bean | **删除** | 已被 iam-session `RequestRecord` 替代 |
| 4 | `LogoutReq` | iam-sdk/bean/req | **删除** | 第三方应用直接 HTTP 调用 `GET /iam-sso/logout` |
| 5 | `SessionHelper` | iam-sdk/helper | **删除** | Token 提取已由 `TokenResolver` 替代；用户身份已由 `IdentityContext` 替代 |
| 6 | `SsoFacade`（接口）| iam-sdk/facade | **迁入 iam-session** `remote` 子包 | 远程 SSO 门面契约 |
| 7 | `SsoFacadeImpl`（远程 HTTP 实现）| iam-sdk/facade/impl | **迁入 iam-session** `remote` 子包，建议重命名 `RemoteSsoFacadeImpl` | 第三方应用 HTTP 调用 SSO 服务端 |
| 8 | `LoggingFilter`（客户端请求日志）| iam-sdk/filter | **迁入 iam-session** `filter` 子包 | 第三方应用请求日志采集，依赖 SsoFacade |
| 9 | `AkSignHelper` | iam-sdk/helper | **迁入 iam-session** `remote` 子包 | 远程调用 AK 签名 |
| 10 | `IamSdkConfig` | iam-sdk/config | **拆分迁移** | 见下表 |

### 3.1 IamSdkConfig 配置项拆分归宿

| 配置项 | 当前默认值 | 归宿 | 备注 |
|--------|----------|------|------|
| `iam.sdk.enabled` | true | **删除** | iam-sdk 整体删除，无需启用开关 |
| `iam.sdk.app-code` | - | **删除** | 由请求头 `app-code` 传递（SessionAuthFilter 已处理）|
| `iam.sdk.jwt.secret-key` | qwerty... | **已替代** | 由 `iam.session.token.secret-key` 替代（IamSessionConfig 已实现）|
| `iam.sdk.server-url` | - | 迁入 iam-session `RemoteClientConfig` | 远程 SSO 服务端地址 |
| `iam.sdk.app-id` | default | 迁入 iam-session `RemoteClientConfig` | AK 鉴权应用 ID |
| `iam.sdk.app-secret` | default | 迁入 iam-session `RemoteClientConfig` | AK 鉴权应用密钥 |
| `iam.sdk.static.enabled` | false | 迁入 iam-session `RemoteClientConfig` | LoggingFilter 静态资源过滤开关 |
| `iam.sdk.static.subfix` | js\|css\|... | 迁入 iam-session `RemoteClientConfig` | LoggingFilter 静态资源后缀 |

**建议的新配置前缀**：`iam.session.remote.*`（如 `iam.session.remote.server-url`），避免与 `iam.sdk.*` 混淆。

---

## 四、iam-session 模块变更（方案 B 落地）

### 4.1 pom.xml 变更

新增 `sh-web` 依赖（用户明确允许）：

```xml
<dependency>
    <groupId>com.wkclz.framework</groupId>
    <artifactId>sh-web</artifactId>
</dependency>
```

> sh-web 提供 `IpHelper`、`RequestHelper`、`LocalThreadHelper`、`ErrorHandler` 等，LoggingFilter 迁移后需要。

### 4.2 新增 `remote` 子包

```
iam-session/src/main/java/com/wkclz/iam/session/remote/
├── SsoFacade.java                 # 接口（从 iam-sdk 迁入）
├── RemoteSsoFacadeImpl.java       # 远程 HTTP 实现（从 iam-sdk SsoFacadeImpl 迁入并重命名）
├── AkSignHelper.java              # AK 签名工具（从 iam-sdk 迁入）
└── RemoteClientConfig.java        # 远程客户端配置（从 IamSdkConfig 拆分迁入）
```

### 4.3 `LoggingFilter` 迁移与改造

从 `iam-sdk/filter/LoggingFilter.java` 迁入 `iam-session/filter/LoggingFilter.java`。

**必须的代码改造**（迁入时一并完成）：

1. **替换用户身份获取方式**：
   - 旧：`SessionHelper.getUserSession(request)` → `UserSession`
   - 新：`IdentityContext.get()` → `UserIdentity`
   - 影响 `LoggingFilter` 第 87-92 行的用户信息回填逻辑

2. **Token 提取方式（可选）**：
   - 旧 `LoggingFilter` 未直接提取 token（仅通过 `SessionHelper.getUserSession(request)` 获取用户信息）
   - 迁移后若需补充 token 字段，注入 `TokenResolver` 并调用 `tokenResolver.resolve(request)`（非必需，保持现有行为即可）

3. **包名调整**：`com.wkclz.iam.sdk.filter` → `com.wkclz.iam.session.filter`

4. **Bean 引用调整**：原 `IamSdkConfig` → 新 `RemoteClientConfig`

### 4.4 服务端 vs 客户端部署的 Filter 互斥

iam-session 同时存在两个请求日志 Filter，需通过配置区分启用场景：

| Filter | 启用场景 | 持久化方式 |
|--------|---------|----------|
| `RequestRecordFilter`（服务端）| SSO 服务端 / Admin 服务端 | 本地 `RequestRecordHandler` SPI（iam-sso 提供 `RequestRecordHandlerImpl`）|
| `LoggingFilter`（客户端）| 第三方应用 | 远程 `RemoteSsoFacade.saveLog()` HTTP 调用 |

**现状分析**（影响互斥机制选择）：

[`NoOpRequestRecordHandler`](../../../iam-session/src/main/java/com/wkclz/iam/session/spi/NoOpRequestRecordHandler.java) 已用 `@ConditionalOnMissingBean(RequestRecordHandler.class)` 注册：
- 服务端部署（iam-sso 存在）：`RequestRecordHandlerImpl` 注册 → `NoOp` 不注册 → `RequestRecordFilter` 正常持久化
- 客户端部署（无 iam-sso）：`RequestRecordHandlerImpl` 不存在 → `NoOp` 注册为默认 → `RequestRecordFilter` 调用 `NoOp`（空操作，无害但浪费一次调用）

**推荐方案 c（最实用，写入实现）**：

| Filter | 注册条件 | 客户端行为 | 服务端行为 |
|--------|---------|----------|----------|
| `RequestRecordFilter` | `@Component` 无条件注册 | 调用 `NoOp`（空操作，无害）| 调用 `RequestRecordHandlerImpl` 持久化 |
| `LoggingFilter` | `@ConditionalOnProperty("iam.session.remote.server-url")` —— 仅当配置了远程 server-url 时注册 | 注册并远程持久化 | 不注册（未配置 server-url）|

理由：
- 方案 b（`@ConditionalOnMissingBean(RequestRecordHandler.class)`）不可行——客户端 `NoOp` 总会先注册，导致 `LoggingFilter` 的 `@ConditionalOnMissingBean` 永不触发
- 方案 a（`@ConditionalOnProperty("iam.session.request-log-mode")`）需要用户显式声明部署模式，额外认知负担
- 方案 c 利用"是否配置 server-url"作为客户端部署的自然信号，零配置自动区分

> 当前 `RequestRecordFilter` 是 `@Component` 无条件注册，方案 c 下保持不变；`LoggingFilter` 迁入时补充 `@ConditionalOnProperty` 注解。

---

## 五、iam-sso 模块变更

### 5.1 删除文件（3 个）

| 文件 | 删除理由 |
|------|---------|
| [IamSsoServiceImpl.java](../../../iam-sso/src/main/java/com/wkclz/iam/sso/service/IamSsoServiceImpl.java) | 引用不存在的 `IamSsoService` 接口（编译断裂）；无人 `@Autowired` 使用（grep 确认）；`tokenCheck` 逻辑已由 `SessionAuthFilter` + `SessionManager.validateAndRefresh` 在请求级统一处理 |
| [SsoFacadeImpl.java](../../../iam-sso/src/main/java/com/wkclz/iam/sso/service/SsoFacadeImpl.java) | 本地 SsoFacade 实现，用户确认删除；其 `saveLog` 已被 `RequestRecordFilter` + `RequestRecordHandlerImpl` 替代；其 `logout` 已被 `PasswordLoginService.logout()` 替代 |
| [IamSessionService.java](../../../iam-sso/src/main/java/com/wkclz/iam/sso/service/IamSessionService.java) | 删除 `SsoFacadeImpl` 后成为 dead code；本身是会话包装类，违反原则 2（推断决策，待用户最终确认）|

### 5.2 保留文件（无需变更）

以下 iam-sso 文件符合设计，本次不动：

- `PasswordLoginService.java`（已委托 SessionManager）
- `SessionEventListenerImpl.java`（SSO-12a 审计实现）
- `SessionInvalidationListener.java`（SSO-17/18 事件监听）
- `CaptchaService.java`、`DefaultCredentialChecker.java`、`Pbkdf2PasswordEncoder.java`
- `UsernameCacheService.java`、`SsoResourceService.java`、`IamRequestService.java`
- `RequestRecordHandlerImpl.java`（iam-session `RequestRecordHandler` SPI 的持久化实现）

---

## 六、iam-admin 模块变更

### 6.1 修复 [UserMenuRest.java](../../../iam-admin/src/main/java/com/wkclz/iam/admin/rest/UserMenuRest.java)

**变更**：

```java
// 旧（第 10 行 + 第 36/45 行）
import com.wkclz.iam.sdk.helper.SessionHelper;
...
entity.setUserCode(SessionHelper.getUserCode());

// 新
import com.wkclz.core.identity.IdentityContext;
...
entity.setUserCode(IdentityContext.getUserCode());
```

共 2 处调用（第 36、45 行）+ 1 处 import（第 10 行）。

### 6.2 pom.xml 变更

无需变更。iam-admin → iam-sso → iam-session → sh-core 传递依赖链已覆盖 `IdentityContext`。

---

## 七、根 pom.xml 变更

移除 iam-sdk 模块声明：

```xml
<!-- 删除以下行 -->
<module>iam-sdk</module>
```

`iam-sdk/` 目录本身建议保留至验证全部通过后，由用户决定何时物理删除（避免 git 历史断裂）。

---

## 八、验证清单

完成所有变更后，依次执行：

1. **编译验证**（无 Maven CLI 时在 IDE 中执行）：
   - `mvn compile -pl iam-session -am` —— iam-session 编译通过
   - `mvn compile -pl iam-sso -am` —— iam-sso 编译通过（不再引用 iam-sdk）
   - `mvn compile -pl iam-admin -am` —— iam-admin 编译通过
   - `mvn compile -pl iam-sso-starter -am` —— starter 编译通过
   - `mvn compile -pl iam-admin-starter -am` —— starter 编译通过

2. **残留引用扫描**：
   ```powershell
   # 期望：仅匹配 docs/ 和 .qoder/ 下的文档，无 .java 文件
   rg "com\.wkclz\.iam\.sdk\." --type java
   ```

3. **iam-sdk 模块声明扫描**：
   ```powershell
   # 期望：仅匹配 iam-sdk/pom.xml 自身，无其他 pom 引用
   rg "<artifactId>iam-sdk</artifactId>" --glob "**/pom.xml"
   ```

4. **`iam.sdk.*` 配置残留扫描**：
   ```powershell
   # 期望：仅 docs/ 下有文档残留
   rg "iam\.sdk\." --glob "**/*.yml"
   ```

---

## 九、实现决策点（留给 writing-plans 阶段细化）

以下点在实现时需要进一步决策，但规格层面不阻塞：

1. ~~**LoggingFilter vs RequestRecordFilter 互斥机制**~~：**已定方案 c**（见第 4.4 节），`LoggingFilter` 用 `@ConditionalOnProperty("iam.session.remote.server-url")` 注册
2. **`RemoteClientConfig` 配置前缀**：建议 `iam.session.remote.*`，但需与现有 `iam.session.*` 命名空间核对
3. **`RemoteSsoFacadeImpl` 的注册条件**：与 `LoggingFilter` 同步——`@ConditionalOnProperty("iam.session.remote.server-url")`，第三方应用启用远程 impl，服务端不启用
4. **LoggingFilter 的 Order 调整**：迁入后需与 `RequestRecordFilter`（`Ordered.LOWEST_PRECEDENCE - 10`）和 `SessionAuthFilter`（无显式 Order）协调执行顺序。由于方案 c 下两者互斥（不会同时注册），Order 冲突可忽略
5. **iam-sdk 目录是否物理删除**：建议本次仅从根 pom 移除模块声明，目录保留至验证通过后

---

## 十、影响范围总览

| 模块 | 变更类型 | 文件数 |
|------|---------|-------|
| iam-sdk | 整体从根 pom 移除（目录保留）| 1（根 pom.xml）|
| iam-session | pom + 4 个新文件 + 1 个迁移文件 | ~6 |
| iam-sso | 删除 3 个文件 | 3 |
| iam-admin | 修改 1 个文件 | 1 |
| 根 pom.xml | 移除 1 行模块声明 | 1 |

**总计**：约 12 个文件变更，无业务逻辑变更（仅模块边界调整 + dead code 清理）。
