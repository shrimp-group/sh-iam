# SHA-005：认证域 SPI

## 故事描述

**作为** SPI 接入方
**我想要** 定义认证域所有 SPI 接口，覆盖密码编码、Token 管理、会话存储、MFA、频控等扩展点
**以便** 接入方可按需实现具体逻辑（JWT/Redis/SMS/TOTP 等），框架层只依赖接口

## 验收标准

1. 认证域 SPI 接口定义在 `com.wkclz.auth.contract.auth` 包，共 16 个接口：
   - `AuthenticationProvider` — 认证提供者（`authenticate(Subject) → AuthResult`，`supports(CredentialType) → boolean`）
   - `TokenService` — Token 服务（`generateToken(Principal, Map<String,Object>) → AuthToken`，`validateToken(String) → AuthToken`，`refreshToken(String) → AuthToken`，`revokeToken(String)`）
   - `LoginService` — 登录服务模板（`login(AuthRequest) → AuthResult`），定义 7 步流程：频控检查→验证码校验→凭证认证→账号状态检查→MFA 判断→Token 生成→会话创建
   - `LogoutService` — 登出服务（`logout(Session)`，`logoutAll(String subjectId)`）
   - `DefaultLogoutService` — 默认登出实现（调用 `TokenService.revokeToken()` + `SessionStore.invalidate()`）
   - `PasswordEncoder` — 密码编码器（`encode(String) → String`，`matches(String raw, String encoded) → boolean`）
   - `PasswordValidator` — 密码强度校验（`validate(String password) → List<String> errors`）
   - `CredentialProvider` — 凭据提供者（`loadCredentials(String identifier, CredentialType) → Credential`）
   - `SmsCodeSender` — 短信验证码发送（`send(String phone, String code)`）
   - `OAuthProvider` — OAuth2 认证提供者（`getAuthorizeUrl(String state) → String`，`handleCallback(Map params) → AuthResult`）
   - `CaptchaService` — 图形验证码服务（`generate() → Captcha`，`validate(String captchaId, String captchaCode) → boolean`）
   - `MfaService` — MFA 服务（`generateChallenge(Principal, MfaType) → MfaChallenge`，`verifyChallenge(String challengeId, String code) → boolean`）
   - `AccountStatusChecker` — 账号状态检查（`check(Principal) → AccountStatus`）
   - `SessionStore` — 会话存储（`createSession(Principal, AuthToken) → Session`，`getSession(String sessionId) → Session`，`updateSession(Session)`，`invalidateSession(String sessionId)`，`invalidateAllSessions(String subjectId)`，`enforceMaxConcurrentSessions(String subjectId, int max)`）
   - `ConcurrentSessionControl` — 并发会话控制（`getActiveSessionCount(String subjectId) → int`，`enforceLimit(String subjectId, int max)`）
   - `RateLimitChecker` — 频控检查（`checkRateLimit(String key) → boolean`，`recordAttempt(String key)`，`resetAttempts(String key)`）
2. `LoginService` 注入以下 SPI 实现：`RateLimitChecker`、`CaptchaService`、`AuthenticationProvider`（支持多个）、`AccountStatusChecker`、`MfaService`、`TokenService`、`SessionStore`
3. `LoginService.login()` 模板方法流程：
   - Step 1: 频控检查 → `rateLimitChecker.checkRateLimit(subject.getIdentifier())`
   - Step 2: 验证码校验（如需要）→ `captchaService.validate()`
   - Step 3: 遍历 `authenticationProviders`，调用 `supports()` 匹配后执行 `authenticate()`
   - Step 4: 账号状态检查 → `accountStatusChecker.check()`
   - Step 5: MFA 判断 → 如需要返回 `AuthResult(MFA_REQUIRED, mfaChallenge)`
   - Step 6: Token 生成 → `tokenService.generateToken(principal, claims)`，在 `createSession` 之前调用
   - Step 7: 会话创建 → `sessionStore.createSession(principal, authToken)`
4. 模板方法中捕获 4 种异常：
   - `InvalidCredentialsException` → 调用 `rateLimitChecker.recordAttempt()` → 返回 `FAILED`
   - `AccountStatusException` → 返回 `FAILED`
   - `MfaRequiredException` → 返回 `MFA_REQUIRED`
   - `Exception`（兜底）→ 日志记录 → 返回 `INTERNAL_ERROR`
5. `TokenService.generateToken()` 必须在 `SessionStore.createSession()` 之前调用

## 技术要点

- 所有 SPI 接口为 Java `interface`，不提供默认实现（除 `DefaultLogoutService` 外）
- `AuthenticationProvider` 使用责任链模式，`supports()` 判断凭据类型，多个 Provider 时按顺序匹配
- `LoginService` 为抽象类（非接口），提供 `login()` 模板方法 + 可被子类重写的钩子方法
- `RateLimitChecker.recordAttempt()` 在 Step 3 认证失败后调用（非 Step 1 调用）

## 关联故事

- 依赖：SHA-001, SHA-002, SHA-003
- 被依赖：SHA-007, SHA-010
