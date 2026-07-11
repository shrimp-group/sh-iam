# SHA-012：SecurityHeaderFilter

## 故事描述

**作为** 框架开发者
**我想要** 实现 HTTP 安全响应头注入过滤器，自动为所有响应添加 X-Frame-Options、CSP、HSTS 等安全头
**以便** 提升应用的整体 Web 安全防护水平

## 验收标准

1. `SecurityHeaderFilter` 位于 `com.wkclz.auth.filter` 包，实现 `jakarta.servlet.Filter`
2. 过滤器 Order 值在 `AuthorizationFilter` 之后（例如 `@Order(Ordered.HIGHEST_PRECEDENCE + 4)`）
3. 注入 `List<SecurityHeaderProvider> securityHeaderProviders`（可为空）
4. 注入安全头逻辑：
   - 收集所有 `SecurityHeaderProvider.getSecurityHeaders()` 返回的 `SecurityHeaders`
   - 合并所有 Provider 的 headers（相同的 key 后者覆盖前者，以最后注册的 Provider 为准）
   - 遍历 `SecurityHeaders` 的 key-value，逐个调用 `response.setHeader(key, value)`
   - 跳过 `null` 或空字符串的 value
5. 常见安全头默认配置（在无 Provider 实现时使用）：
   - `X-Frame-Options: DENY`
   - `X-Content-Type-Options: nosniff`
   - `X-XSS-Protection: 1; mode=block`
   - `Referrer-Policy: strict-origin-when-cross-origin`
6. CSP 和 HSTS 头需通过 Provider 实现提供，框架不预设
7. 支持 `AuthProperties` 中配置是否启用（`sh.auth.security-headers.enabled=true`）
8. 多个 `SecurityHeaderProvider` 共存时，全部应用（收集所有 headers 后统一设置）

## 技术要点

- 安全头设置在过滤链执行之后（`chain.doFilter()` 返回后再设置），确保响应头覆盖不受业务逻辑干扰
- 多 Provider 时的合并策略：遍历所有 Provider，后续同名 Key 覆盖前者
- 空值跳过：`if (value != null && !value.isEmpty())` 再调用 `setHeader()`
- 过滤器执行必须放行 `chain.doFilter()`，安全头注入不阻塞请求

## 关联故事

- 依赖：SHA-001, SHA-003, SHA-006
- 被依赖：无
