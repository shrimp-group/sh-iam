# SHA-004：SecurityContext

## 故事描述

**作为** 框架开发者
**我想要** 实现基于 ThreadLocal 的安全上下文，支持跨层传递用户主体、令牌和租户信息
**以便** 过滤器和业务代码可以在任何位置获取当前请求的安全上下文，无需参数传递

## 验收标准

1. `SecurityContext` 类定义在 `com.wkclz.auth.context` 包
2. 提供以下静态方法：
   - `setPrincipal(Principal)` / `getPrincipal()` — 设置/获取当前用户主体
   - `setToken(AuthToken)` / `getToken()` — 设置/获取当前认证令牌
   - `setTenantCode(String)` / `getTenantCode()` — 设置/获取租户编码（使用独立 ThreadLocal，不依赖 Principal）
   - `setAppCode(String)` / `getAppCode()` — 设置/获取应用编码（使用独立 ThreadLocal）
3. `clear()` 方法清除所有 ThreadLocal，防止内存泄漏：
   - 清除 Principal ThreadLocal
   - 清除 Token ThreadLocal
   - 清除 tenantCode ThreadLocal
   - 清除 appCode ThreadLocal
4. `getTenantCode()` 和 `getAppCode()` 使用各自独立的 `ThreadLocal<String>`，不依赖于 `getPrincipal()`
5. 所有 ThreadLocal 使用 `ThreadLocal.withInitial(() -> null)` 初始化
6. 提供 `hasPrincipal()` 便捷方法判断当前是否已认证

## 技术要点

- `com.wkclz.auth.context.SecurityContext` — 纯静态方法工具类，不依赖 Spring Bean
- tenantCode 独立 ThreadLocal 的设计原因：tenantCode 是运行时从请求中解析的参数，与 Principal 生命周期不同（例如跨租户管理场景）
- `clear()` 必须在请求结束时调用（由 RequestRecordFilter 的 finally 块保证）
- 不可直接暴露 ThreadLocal 引用，仅提供 get/set/clear 静态方法

## 关联故事

- 依赖：SHA-001, SHA-002, SHA-003
- 被依赖：SHA-007, SHA-008, SHA-009, SHA-010, SHA-011
