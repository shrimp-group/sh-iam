# SHA-009：RequestRecordFilter

## 故事描述

**作为** 框架开发者
**我想要** 实现全量请求日志采集过滤器，记录所有请求的方法、URI、参数、响应状态、耗时等信息
**以便** 为审计和问题排查提供完整的请求追踪链路

## 验收标准

1. `RequestRecordFilter` 位于 `com.wkclz.auth.filter` 包，实现 `jakarta.servlet.Filter`
2. 过滤器 Order 值在 `RequestWrapperFilter` 之后（例如 `@Order(Ordered.HIGHEST_PRECEDENCE + 1)`）
3. 包裹所有后续过滤器执行逻辑在 `try/finally` 块中：
   - `try` 块：执行过滤链，记录请求开始时间、请求参数、请求体
   - `finally` 块：
     - 记录响应状态码
     - 调用 `SecurityContext.clear()` 清除 ThreadLocal
     - 异步调用 `RequestLogger.save(requestRecord)` 持久化日志
4. 敏感信息脱敏处理：
   - Token 脱敏：截取前 8 位 + `****`，如 `Bearer eyJhbGci****`
   - Password 脱敏：请求体中的 `password` 字段替换为 `******`
   - 响应体截断：超过阈值（如 4KB）时截断并附加 `...(truncated)`
5. 构造 `RequestRecord` 对象填充以下关键字段：
   - `requestId`（UUID 生成）
   - `userId` / `username`（从 `SecurityContext.getPrincipal()` 获取）
   - `requestMethod`、`requestUri`、`requestIp`（从 request 获取，IP 使用框架 IP 工具类）
   - `requestParams`（脱敏后的 Query String）
   - `requestBody`（脱敏后的请求体）
   - `responseBody`（截断后的响应体）
   - `responseStatus`（HTTP 状态码）
   - `responseTime`（耗时，ms）
   - `userAgent`、`referer`（从 Header 获取）
   - `tenantCode`、`appCode`（从 SecurityContext 获取）
   - `traceId`、`spanId`（从 MDC 获取链路追踪信息）
   - `clientType`、`osFamily` 等（从 User-Agent 解析）
6. `RequestLogger.save()` 必须异步调用（例如通过 `@Async` 或线程池提交），不阻塞主请求
7. 注入 `List<RequestLogger>` 支持多个 Logger 实现（全部调用）
8. `finally` 块中必须调用 `SecurityContext.clear()`，确保 ThreadLocal 不泄漏

## 技术要点

- 使用 `System.currentTimeMillis()` 记录请求开始时间，`finally` 中计算耗时
- 脱敏逻辑使用正则替换：`"password"\s*:\s*"[^"]*"` → `"password":"******"`
- Token 脱敏：从 Header 中提取后保留前缀 + 前 8 字符
- 响应体捕获：需要包装 `HttpServletResponse` 为 `ContentCachingResponseWrapper`
- 异步日志不阻塞主请求线程，异常时仅 log.warn 不抛出

## 关联故事

- 依赖：SHA-001, SHA-003, SHA-004, SHA-006, SHA-008
- 被依赖：无
