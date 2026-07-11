# SHA-003：核心数据模型

## 故事描述

**作为** 框架开发者
**我想要** 创建全部核心 bean 数据模型，覆盖认证、授权、会话、资源等所有上下文对象
**以便** SPI 接口、过滤器、缓存等组件可以基于统一的模型进行交互

## 验收标准

1. 定义 25 个核心 bean，全部实现 `java.io.Serializable`，放置于 `com.wkclz.auth.bean` 包：
   - `Principal` — 用户主体（userCode, username, nickname, email, phone, avatar, userStatus），不含 tenantCode（tenantCode 为运行时参数，通过 SecurityContext 传递）
   - `Subject` — 认证主体（principal, credentials, tenantCode, appCode, clientIp, userAgent）
   - `Credential` — 凭据载体（credentialType, identifier, secret, captchaId, captchaCode）
   - `AuthRequest` — 认证请求（subject, loginTime, clientIp, userAgent, sessionId）
   - `AuthResult` — 认证结果（status: AuthStatus, principal, token, mfaChallenge, errorType, message）
   - `AuthToken` — 认证令牌（tokenType, tokenValue, refreshToken, expiresIn, issuedAt, expiresAt）
   - `Session` — 会话（sessionId, principal, token, tenantCode, appCode, loginTime, lastAccessTime, expiresAt, attributes: Map）
   - `Captcha` — 验证码（captchaId, captchaImage, captchaCode, expiresAt）
   - `MfaChallenge` — MFA 质询（challengeId, principal, mfaType, challengeData, expiresAt）
   - `AuthPermission` — 权限描述（apiMethod, apiUri, permissionCode）
   - `Role` — 角色（roleCode, roleName, appCode, parentCode, applicable）
   - `MenuNode` — 菜单节点（menuCode, menuName, parentCode, menuType, routePath, component, buttonCode, icon, visible, children），不含 sort 字段
   - `ApiResource` — API 资源（module, appCode, apiCode, apiMethod, apiUri, apiName, writeFlag）
   - `DataScope` — 数据权限范围（dimensionCode, dimensionName, scopeType, scopeValues）
   - `FieldPermission` — 字段权限（fieldName, permissionType: FieldPermissionType）
   - `SubjectRole` — 主体-角色关联（subjectId, roleCode, startTime, endTime, enableStatus）
   - `RoleDataScope` — 角色-数据范围关联（roleCode, dimensionCode, scopeType, scopeValues）
   - `ApiField` — API 返回字段定义（apiCode, fieldName, fieldType, fieldDesc）
   - `LoginRecord` — 登录记录（loginId, principal, loginTime, loginIp, loginStatus, failReason, userAgent）
   - `RequestRecord` — 请求日志记录，字段对齐现有 `IamRequestLog`（35字段：id, requestId, userId, username, appCode, requestMethod, requestUri, requestIp, requestParams, requestBody, responseBody, responseStatus, responseTime, userAgent, referer, serverIp, serverPort, tenantCode, traceId, spanId, createTime, createBy, updateTime, updateBy, remark, version, deleted, clientType, osFamily, osVersion, browserFamily, browserVersion, deviceType, deviceModel, isMobile）
   - `SecurityHeaders` — 安全响应头配置（key-value Map，支持 X-Frame-Options/CSP/HSTS/X-Content-Type-Options 等）
   - `AuthMetadata` — 认证元数据（roles, menuNodes, apiResources, dataScopes, fieldPermissions），用于全局缓存
   - `SubjectAuthorization` — 用户级授权（roleCodes, menuCodes, apiPermissions, dataScopes, fieldPermissions），与用户关联
   - `ResolvedAuthorization` — 计算结果授权（apiPermissions: Set, fieldPermissions: Map, dataScopes: Map），运行时计算结果缓存
2. 所有 bean 使用 Lombok `@Data` + `@EqualsAndHashCode(callSuper = false)`（如需继承）
3. 字段使用 `@Schema(description = "xxx")` 注解
4. 每个 bean 提供静态工厂方法 `of(...)` 或 Builder 模式构造

## 技术要点

- `Principal` 不包含 `tenantCode`，因为 `tenantCode` 是运行时请求上下文参数
- `MenuNode` 不含 `sort` 字段，排序由 admin 模块在转换时处理
- `RequestRecord` 的 35 个字段完整对齐现有 `IamRequestLog` 实体
- `Subject` 包含 `clientIp`、`userAgent` 等上下文信息，避免参数层层传递
- `AuthResult` 支持 `SUCCESS`、`MFA_REQUIRED`、`FAILED` 三态，其中 `MFA_REQUIRED` 时携带 `mfaChallenge`

## 关联故事

- 依赖：SHA-001, SHA-002
- 被依赖：SHA-004, SHA-005, SHA-006, SHA-007, SHA-008, SHA-009, SHA-010, SHA-011, SHA-013
