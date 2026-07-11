# SHA-002：枚举与异常体系

## 故事描述

**作为** 框架开发者
**我想要** 定义完整的枚举类和异常体系
**以便** 为认证授权框架提供统一的错误码、状态描述和异常处理机制

## 验收标准

1. 定义 9 个枚举类，每个枚举包含 `code` 字段和 `desc` 中文描述字段：
   - `AccountStatus` — 账号状态（NORMAL/LOCKED/DISABLED/EXPIRED）
   - `AuthErrorType` — 认证错误类型（INVALID_CREDENTIALS/ACCOUNT_LOCKED/ACCOUNT_DISABLED/ACCOUNT_EXPIRED/PASSWORD_EXPIRED/MFA_REQUIRED/MFA_FAILED/CAPTCHA_REQUIRED/CAPTCHA_ERROR/RATE_LIMITED/SESSION_EXPIRED/TOKEN_INVALID/UNAUTHORIZED/FORBIDDEN/INTERNAL_ERROR）
   - `AuthStatus` — 认证结果状态（SUCCESS/MFA_REQUIRED/FAILED）
   - `CredentialType` — 凭据类型（PASSWORD/SMS_CODE/EMAIL_CODE/TOTP/LDAP/OAUTH2）
   - `FieldPermissionType` — 字段权限类型（VISIBLE/EDITABLE/MASKED/HIDDEN）
   - `MenuType` — 菜单类型（MENU/BUTTON/DIRECTORY）
   - `MfaType` — MFA 类型（TOTP/SMS/EMAIL）
   - `TokenType` — Token 类型（ACCESS/REFRESH/ID）
   - `RefreshScope` — 缓存刷新范围（METADATA/SUBJECT/ALL）
2. 定义异常体系：
   - `AuthException` — 基类，继承 `RuntimeException`，携带 `AuthErrorType`，提供静态工厂方法如 `invalidCredentials()`、`accountLocked()` 等
   - `InvalidCredentialsException` — 凭据无效异常
   - `AccountStatusException` — 账号状态异常（锁定/禁用/过期）
   - `PasswordExpiredException` — 密码过期异常
   - `MfaRequiredException` — 需要 MFA 异常
   - `RateLimitException` — 频控异常
   - `UnauthorizedException` — 未授权/未认证异常
3. `AuthException` 提供 `getErrorType()`、`getArgs()` 方法支持国际化消息参数

## 技术要点

- 枚举定义在 `com.wkclz.auth.enums` 包
- 异常定义在 `com.wkclz.auth.exception` 包
- `AuthException` 构造函数接受 `AuthErrorType` + 可变参数 `Object... args`
- 子类异常通过 `super(errorType, args)` 传递，保持统一构造模式
- 异常消息可通过 `MessageSource` 国际化，`AuthErrorType.code` 作为消息 key

## 关联故事

- 依赖：SHA-001
- 被依赖：SHA-004, SHA-005, SHA-006, SHA-007
