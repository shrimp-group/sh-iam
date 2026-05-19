# STORY-009 — 请求体可重复读取包装

| 属性 | 值 |
|------|-----|
| Story ID | STORY-009 |
| 所属 Epic | SDK 鉴权与安全模块 |
| 所属模块 | iam-sdk |
| 优先级 | P0 |
| 状态 | 待开发 |

## 用户故事

**作为** SDK 开发者，**我希望** 将原始 HttpServletRequest 包装为可重复读取请求体的版本，**以便** LoggingFilter 和其他下游组件能够多次读取请求体内容而不消耗 InputStream。

## 验收标准

1. `RequestWrapperFilter` 继承 `OncePerRequestFilter`，Order = `Integer.MIN_VALUE`
2. 在所有过滤器之前执行，将原始请求包装为 `EagerContentCachingRequestWrapper`
3. `EagerContentCachingRequestWrapper` 继承 Spring 的 `ContentCachingRequestWrapper`
4. 新增 `cachedBody` 字段缓存请求体字节数组
5. `makeBodyCache()` 主动将请求体读入缓存（按需调用）
6. 重写 `getInputStream()` / `getReader()` 优先返回缓存内容
7. 重写 `getContentAsByteArray()` 优先返回缓存内容
8. 文件上传场景不主动缓存请求体，避免内存压力

## 技术实现要点

- 执行顺序：Order = `Integer.MIN_VALUE`，确保在所有过滤器之前
- 继承 Spring 的 `ContentCachingRequestWrapper`，在其基础上增加主动缓存能力
- `makeBodyCache()` 按需调用，非自动触发，避免文件上传等大请求体场景的内存问题
- 缓存后 `getInputStream()` 和 `getReader()` 可多次调用返回相同内容

## 依赖故事

无

## 涉及文件

| 文件 | 路径 |
|------|------|
| RequestWrapperFilter | iam-sdk/src/main/java/com/wkclz/iam/sdk/filter/RequestWrapperFilter.java |
| EagerContentCachingRequestWrapper | iam-sdk/src/main/java/com/wkclz/iam/sdk/filter/EagerContentCachingRequestWrapper.java |
