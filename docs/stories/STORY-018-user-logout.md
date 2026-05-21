# STORY-018 — 用户登出

| 属性 | 值 |
|------|-----|
| Story ID | STORY-018 |
| 所属 Epic | SSO 登录认证模块 |
| 所属模块 | iam-sso |
| 优先级 | P0 |
| 状态 | 待开发 |

## 用户故事

**作为** 已登录用户，**我希望** 能够安全地登出系统，**以便** 我的会话被清除，防止他人冒用我的身份。

## 验收标准

1. API 端点：`GET /iam-sso/public/sso/logout`
2. 从请求获取 Token 和 UserSession
3. 删除 Redis 中对应的 Session 缓存
4. 登出后该 Token 即刻失效，无法再次通过鉴权

## 技术实现要点

- Token 获取：从请求头 `Authorization` 或 `token` 获取
- Redis Key：`iam:session:{username}:{md5(token)}`
- 登出即删除 Redis 中的 Session，使 Token 失效
- 即使 JWT 本身未过期，Redis Session 被删除后也无法通过鉴权

## 依赖故事

- STORY-005（JWT 令牌工具）
- STORY-006（用户会话上下文）

## 涉及文件

| 文件 | 路径 |
|------|------|
| LoginRest | iam-sso/src/main/java/com/wkclz/iam/sso/rest/LoginRest.java |
| IamLoginService | iam-sso/src/main/java/com/wkclz/iam/sso/service/IamLoginService.java |
