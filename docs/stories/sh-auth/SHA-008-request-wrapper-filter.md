# SHA-008：RequestWrapperFilter

## 故事描述

**作为** 框架开发者
**我想要** 实现请求体可重复读取的包装过滤器
**以便** 后续的日志采集过滤器和认证过滤器可以多次读取 Request Body

## 验收标准

1. `RequestWrapperFilter` 位于 `com.wkclz.auth.filter` 包，实现 `jakarta.servlet.Filter`
2. 过滤器 Order 值设为 `@Order(Ordered.HIGHEST_PRECEDENCE)` 或 `Integer.MIN_VALUE`，确保在所有过滤器中最先执行
3. 使用自定义 `EagerContentCachingRequestWrapper` 包装 `HttpServletRequest`：
   - 继承 `HttpServletRequestWrapper`
   - 在构造函数中主动读取并缓存 `InputStream` 为 `byte[]`
   - 重写 `getInputStream()` 返回新的 `ByteArrayInputStream(cachedBody)`
   - 重写 `getReader()` 返回新的 `BufferedReader(new InputStreamReader(getInputStream()))`
4. 仅包装非文件上传（`content-type` 不含 `multipart`）的请求
5. 后续过滤器可以通过 `request.getInputStream()` 或 `request.getReader()` 多次读取请求体
6. 过滤器不依赖 Spring Bean，无外部 SPI 注入

## 技术要点

- 与现有 `iam-sdk` 中的 `RequestWrapperFilter` 功能类似，但归属 `sh-auth` 模块
- `EagerContentCachingRequestWrapper` 在构造时立即缓存（而非延迟缓存），确保不丢失 body
- 非 multipart 请求才缓存（文件上传不需要缓存 body）
- 过滤链必须调用 `chain.doFilter(wrappedRequest, response)` 传递包装后的请求

## 关联故事

- 依赖：SHA-001
- 被依赖：SHA-009, SHA-010
