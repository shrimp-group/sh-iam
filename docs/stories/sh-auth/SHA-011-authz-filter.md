# SHA-011：AuthorizationFilter

## 故事描述

**作为** 框架开发者
**我想要** 实现鉴权过滤器，根据 API 权限判断请求是否被允许
**以便** 未授权的请求返回 403，已授权的请求正常放行

## 验收标准

1. `AuthorizationFilter` 位于 `com.wkclz.auth.filter` 包，实现 `jakarta.servlet.Filter`
2. 过滤器 Order 值在 `AuthenticationFilter` 之后（例如 `@Order(Ordered.HIGHEST_PRECEDENCE + 3)`）
3. 注入 `List<AccessControlProvider> accessControlProviders`（可为空）
4. 鉴权流程：
   - **Step 1 - 白名单检查**：匹配 `AuthProperties.getWhiteList()` 路径列表，命中跳过鉴权，直接执行 `chain.doFilter()`
   - **Step 2 - 无 Provider 放行**：如果 `accessControlProviders` 为空或无实现，记录 debug 日志后直接放行
   - **Step 3 - 获取认证信息**：从 `SecurityContext.getPrincipal()` 获取当前用户，若无 Principal 则返回 401（未认证不能鉴权）
   - **Step 4 - 遍历 Provider**：依次调用 `provider.hasPermission(principal, method, uri, tenantCode, appCode)`
   - **Step 5 - 任一拒绝即返回 403**：遍历中如任一 Provider 返回 `false`，立即返回 403 并中止遍历
   - **Step 6 - 全部通过则放行**：所有 Provider 返回 `true`，继续过滤链
5. 403 响应返回结构化错误信息：
   ```json
   { "code": 403, "message": "Access denied", "errorType": "FORBIDDEN", "method": "GET", "uri": "/api/users" }
   ```
6. `hasPermission()` 参数从 `HttpServletRequest` 中提取：
   - `apiMethod` = `request.getMethod()`
   - `apiUri` = `request.getRequestURI()`
   - `tenantCode` = `SecurityContext.getTenantCode()`
   - `appCode` = `SecurityContext.getAppCode()`
7. 白名单配置与 `AuthenticationFilter` 共用同一配置源

## 技术要点

- 无 `AccessControlProvider` 实现时仅记录 debug 日志，不报错不阻塞——此设计支持渐进式引入授权
- Provider 遍历使用"任一拒绝"策略（最严格），而非"全部同意"策略
- 未认证（无 Principal）时返回 401，已认证但无权限返回 403，语义严格区分
- 使用 `@ConditionalOnBean(AccessControlProvider.class)` 条件注入（可选）

## 关联故事

- 依赖：SHA-001, SHA-003, SHA-004, SHA-006, SHA-010
- 被依赖：无
