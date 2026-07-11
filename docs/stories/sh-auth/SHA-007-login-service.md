# SHA-007：LoginService 模板方法

## 故事描述

**作为** 接入方开发者
**我想要** LoginService 提供统一的登录流程模板，内置频控、验证码、认证、MFA、Token 生成、会话创建和日志记录
**以便** 接入方只需实现 AuthenticationProvider 等子步骤 SPI，无需关心流程编排

## 验收标准

1. `LoginService` 为抽象类，位于 `com.wkclz.auth.contract.auth` 包
2. 注入以下 8 个 SPI（通过构造函数或 setter 注入）：
   - `RateLimitChecker rateLimitChecker`
   - `CaptchaService captchaService`
   - `List<AuthenticationProvider> authenticationProviders`
   - `AccountStatusChecker accountStatusChecker`
   - `MfaService mfaService`
   - `TokenService tokenService`
   - `SessionStore sessionStore`
   - `RequestLogger requestLogger`（可选，记录登录日志）
3. 模板方法 `login(AuthRequest request) → AuthResult` 执行 7 步流程：
   - **Step 1 - 频控检查**：`rateLimitChecker.checkRateLimit(rateLimitKey)`，超过阈值抛出 `RateLimitException`
   - **Step 2 - 验证码校验**：如 `request.getCredential().getCaptchaId()` 非空，调用 `captchaService.validate(captchaId, captchaCode)`，失败抛出 `InvalidCredentialsException`
   - **Step 3 - 凭证认证**：遍历 `authenticationProviders`，匹配 `supports(credentialType)`，调用 `authenticate(subject)`，全部不匹配或认证失败抛出 `InvalidCredentialsException`
   - **Step 4 - 账号状态检查**：`accountStatusChecker.check(principal)`，状态异常抛出对应 `AccountStatusException` 子类
   - **Step 5 - MFA 判断**：如 `mfaService.requiresMfa(principal)` 为 true，返回 `AuthResult(AuthStatus.MFA_REQUIRED, mfaChallenge)`，不继续后续步骤
   - **Step 6 - Token 生成**：`tokenService.generateToken(principal, claims)`，返回 `AuthToken`
   - **Step 7 - 会话创建**：`sessionStore.createSession(principal, authToken)`，返回 `Session`
4. 异常处理机制：
   - `InvalidCredentialsException` → `rateLimitChecker.recordAttempt(rateLimitKey)` → 返回 `AuthResult(FAILED, errorType=INVALID_CREDENTIALS)`
   - `AccountStatusException` → 返回 `AuthResult(FAILED, errorType=对应状态错误码)`
   - `PasswordExpiredException` → 返回 `AuthResult(FAILED, errorType=PASSWORD_EXPIRED)`
   - `RateLimitException` → 返回 `AuthResult(FAILED, errorType=RATE_LIMITED)`
   - 其他 `Exception` → log.error 记录 → 返回 `AuthResult(FAILED, errorType=INTERNAL_ERROR)`
5. 每次登录尝试（无论成功失败）都调用 `rateLimitChecker.recordAttempt(rateLimitKey)`
6. 登录成功后异步调用 `requestLogger.save(loginRecord)` 记录登录日志
7. `AuthResult.status` 包含三态：
   - `SUCCESS` — 登录成功，携带 `token` 和 `session`
   - `MFA_REQUIRED` — 需要 MFA 二次验证，携带 `mfaChallenge`
   - `FAILED` — 登录失败，携带 `errorType` 和 `message`
8. 提供可被子类重写的钩子方法：
   - `beforeLogin(AuthRequest)` — 登录前钩子
   - `afterLoginSuccess(AuthResult)` — 登录成功后钩子
   - `afterLoginFailed(AuthResult)` — 登录失败后钩子

## 技术要点

- 模板方法使用 `final` 修饰 `login()` 防止子类破坏流程
- `rateLimitKey` 生成规则：`login:rate:{identifier}:{clientIp}`
- Step 6（Token 生成）必须在 Step 7（会话创建）之前，因为会话依赖 Token
- `AuthResult` 使用 Builder 模式构造，便于设置不同状态的字段组合

## 关联故事

- 依赖：SHA-001, SHA-002, SHA-003, SHA-004, SHA-005
- 被依赖：SHA-010
