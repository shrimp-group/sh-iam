# STORY-007 — IAM 鉴权过滤器

| 属性 | 值 |
|------|-----|
| Story ID | STORY-007 |
| 所属 Epic | SDK 鉴权与安全模块 |
| 所属模块 | iam-sdk |
| 优先级 | P0 |
| 状态 | 待开发 |

## 用户故事

**作为** 系统管理员，**我希望** 所有非公开的 API 请求都经过 JWT + Redis 会话双重校验，**以便** 确保只有已登录且会话有效的用户才能访问受保护的资源。

## 验收标准

1. 继承 `OncePerRequestFilter`，每个请求只过滤一次
2. 根路径 `/` 请求返回 403 Forbidden
3. `/*/public/**` 路径的请求直接放行
4. 从请求头获取 Token（Authorization 或 token）
5. Token 缺失返回 401 "token 不存在!"
6. JWT 签名或过期校验失败返回 401
7. JWT 解析成功后，调用 `IamSsoService.tokenCheck()` 从 Redis 获取 UserSession
8. 会话不存在返回 401
9. 校验通过后调用 `SessionHelper.cacheUserInfo()` 缓存用户信息到请求上下文
10. 过滤器执行顺序在 SecurityFilter 之后

## 技术实现要点

- 过滤器链执行顺序：RequestWrapperFilter → LoggingFilter → SecurityFilter → IamAuthFilter → Controller
- 公开路径匹配使用 Ant 风格：`/*/public/**`
- Token 获取逻辑：优先 `Authorization` 头（去掉 `Bearer ` 前缀），其次 `token` 头
- JWT 验证 + Redis 会话双重校验，确保 Token 未被主动注销
- 使用 `ResponseHelper` 构建错误响应

## 依赖故事

- STORY-005（JWT 令牌工具）
- STORY-006（用户会话上下文）
- STORY-014（IamSsoService 接口）

## 涉及文件

| 文件 | 路径 |
|------|------|
| IamAuthFilter | iam-sdk/src/main/java/com/wkclz/iam/sdk/filter/IamAuthFilter.java |
