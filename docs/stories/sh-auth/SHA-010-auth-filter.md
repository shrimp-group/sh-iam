# SHA-010：AuthenticationFilter

## 故事描述

**作为** 框架开发者
**我想要** 实现认证过滤器，支持从请求头中解析 Token、验证有效性并设置 SecurityContext
**以便** 所有后续过滤器可以获取当前认证用户信息

## 验收标准

1. `AuthenticationFilter` 位于 `com.wkclz.auth.filter` 包，实现 `jakarta.servlet.Filter`
2. 过滤器 Order 值在 `RequestRecordFilter` 之后（例如 `@Order(Ordered.HIGHEST_PRECEDENCE + 2)`）
3. 注入以下 SPI：
   - `TokenService tokenService` — 验证 Token
   - `SessionStore sessionStore` — 查询会话
4. 认证流程：
   - **Step 1 - 白名单检查**：匹配配置的 `sh.auth.white-list` 路径列表，命中则跳过认证，直接执行 `chain.doFilter()`
   - **Step 2 - Token 提取**：从请求头 `Authorization: Bearer <token>` 或自定义 `X-Auth-Token` 头中提取 Token 值，若无 Token 则跳过认证（不阻塞）
   - **Step 3 - Token 验证**：调用 `tokenService.validateToken(tokenValue)`，验证失败（过期/签名无效/已撤销）返回 401
   - **Step 4 - 会话检查**：调用 `sessionStore.getSession(sessionId)`，Session 不存在或已过期返回 401
   - **Step 5 - 设置 SecurityContext**：调用 `SecurityContext.setPrincipal(session.getPrincipal())`、`SecurityContext.setToken(token)`、`SecurityContext.setTenantCode(session.getTenantCode())`、`SecurityContext.setAppCode(session.getAppCode())`
5. 401 响应返回结构化错误信息：
   ```json
   { "code": 401, "message": "Authentication failed", "errorType": "TOKEN_INVALID" }
   ```
6. 认证异常时向 response 写入错误并 `return`，不继续过滤链
7. 支持 `AuthProperties.getWhiteList()` 配置的白名单路径（Ant 风格匹配）
8. Token 不存在时不报错，仅跳过认证（后续 AuthorizationFilter 会处理未认证情况）

## 技术要点

- 白名单路径匹配使用 AntPathMatcher
- Token 提取支持两种 Header：`Authorization: Bearer xxx` 和 `X-Auth-Token: xxx`，优先取 Authorization
- 401 响应后不清除 SecurityContext（clear 由 RequestRecordFilter 的 finally 统一处理）
- 无 Token 时仍然执行 `chain.doFilter()`，允许公开接口正常访问
- 需要注入 `AuthProperties` 读取白名单配置

## 关联故事

- 依赖：SHA-001, SHA-003, SHA-004, SHA-005, SHA-007
- 被依赖：SHA-011
