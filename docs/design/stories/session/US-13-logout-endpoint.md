# US-13：登出端点与登出审计

> **模块**：iam-sso（单点登录层）
> **依赖**：US-01（IdentityContext）、US-07（destroySession）、US-08（SessionEventListener）
> **来源设计**：[session-design.md](../../session-design.md) — SSO-13, SSO-14

## 用户故事

**作为** 用户
**我想要** 通过 `GET /iam-sso/logout` 安全退出登录，系统销毁当前会话并记录登出事件
**以便** 我的会话被安全终止，且操作有审计记录

## 包含功能点

| ID     | 功能     | 说明                                                                                     |
|--------|--------|----------------------------------------------------------------------------------------|
| SSO-13 | 登出端点   | `GET /iam-sso/logout`：从 IdentityContext 取当前 token → 调用 SessionManager.destroySession() |
| SSO-14 | 登出审计事件 | `LogoutEvent`，含 username、token（脱敏）、timestamp                                           |

## 明确不包含

- 不做 Token 提取（通过 US-09 Filter 已设置到 IdentityContext）
- 不做 Session 销毁逻辑（委托 US-07 的 SessionManager）
- 不做登录日志写入（由 US-08 事件 → US-14 监听器处理）

## 输入

- US-01：`IdentityContext.getToken()`
- US-07：`SessionManager.destroySession()`
- US-08：`SessionEventListener`（事件发布）

## 输出

- `LoginRest.logout()` 方法 — `GET /iam-sso/logout`
- `LogoutEvent` 事件类

## 核心流程

```
GET /iam-sso/logout:
  1. identityContext.getToken() → token
     → 若 token 为 null → 返回 "未登录" 或直接成功
  2. sessionManager.destroySession(token)
  3. 发布 LogoutEvent(username, maskedToken, timestamp)
  4. identityContext.clear()
  5. 返回登出成功
```

## 验收标准

- [ ] `GET /iam-sso/logout` 从 IdentityContext 获取当前 token
- [ ] 调用 `SessionManager.destroySession(token)` 销毁会话
- [ ] Token 在日志中脱敏（仅保留前 8 位 + `****`）
- [ ] 发布 LogoutEvent（含 username、脱敏 token、timestamp）
- [ ] 调用 `IdentityContext.clear()` 清理上下文
- [ ] 返回登出成功
- [ ] 未登录状态下调用登出不报错，正常返回成功
