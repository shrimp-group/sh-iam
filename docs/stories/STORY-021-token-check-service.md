# STORY-021 — Token 校验服务实现

| 属性 | 值 |
|------|-----|
| Story ID | STORY-021 |
| 所属 Epic | SSO 登录认证模块 |
| 所属模块 | iam-sso |
| 优先级 | P0 |
| 状态 | 待开发 |

## 用户故事

**作为** IAM 鉴权系统，**我希望** 在 SSO 服务端实现 Token 校验的本地直连版本，**以便** 鉴权过滤器能从 Redis 直接获取用户会话，无需远程调用。

## 验收标准

1. `IamSsoServiceImpl` 实现 `IamSsoService` 接口
2. `tokenCheck(token, authIdentifier)` 方法从 Redis 获取 UserSession
3. Redis Key：`iam:session:{authIdentifier}:{md5(token)}`
4. 会话不存在返回 null（触发 401 响应）
5. 自动覆盖 SDK 中的默认远程实现

## 技术实现要点

- 使用 `@Service` 注册，自动覆盖 SDK 中 `@ConditionalOnMissingBean` 的默认实现
- Redis 操作使用 `RedisTemplate`，Value 为 UserSession 的 JSON 序列化
- authIdentifier 通常为 username
- Token 校验 = JWT 验证（在 IamAuthFilter 中完成）+ Redis 会话存在性检查

## 依赖故事

- STORY-014（IamSsoService 接口定义）
- STORY-005（JWT 令牌工具，Redis Key 生成）

## 涉及文件

| 文件 | 路径 |
|------|------|
| IamSsoServiceImpl | iam-sso/src/main/java/com/wkclz/iam/sso/service/IamSsoServiceImpl.java |
