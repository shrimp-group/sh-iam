# US-06：会话验证、滑窗续期与活跃会话查询

> **模块**：iam-session（会话管理层）
> **依赖**：US-02（SessionStore）、US-03（TokenService）、US-04（RedisSessionStore）
> **来源设计**：[session-design.md](../../session-design.md) — SES-07, SES-08, SES-12

## 用户故事

**作为** 用户
**我想要** 每次请求时系统自动验证我的会话是否有效（存在且未过期），当剩余有效期不足 30 分钟时自动续期 30 分钟（累计最长 48
小时），并能查看自己的所有活跃会话
**以便** 我的登录状态持续有效，且能掌握账号的登录情况

## 包含功能点

| ID     | 功能          | 说明                                                                |
|--------|-------------|-------------------------------------------------------------------|
| SES-07 | 会话验证        | 从 Token 定位 Session，检查 Session 是否存在且未过期                            |
| SES-08 | 会话 TTL 滑窗续期 | 剩余 TTL < 阈值（默认 30min）时续期 +30min；累计最长不超过 48h；续期间隔可配置（如 5min 内不重复续） |
| SES-12 | 活跃会话查询      | 按用户查询所有活跃 Session 列表（含 sessionId、创建时间、IP、UA 等元数据）                 |

## 明确不包含

- 不做 Token 从 HTTP 请求提取（属于 US-09）
- 不做 IdentityContext 设置（属于 US-09）
- 不做白名单路径判断（属于 US-09）

## 输入

- US-02：`SessionStore` 接口
- US-03：`TokenService.verifyToken()`
- US-04：`RedisSessionStore`（运行时实现）

## 输出

- `SessionManager.validateAndRefresh()` 方法
- `SessionManager.getActiveSessions()` 方法
- 配置项：
    - `iam.session.renewal.threshold`：续期阈值（默认 1800 秒 = 30min）
    - `iam.session.renewal.max-ttl`：累计最大 TTL（默认 172800 秒 = 48h）
    - `iam.session.renewal.interval`：续期间隔（默认 300 秒 = 5min）

## 核心流程

```
validateAndRefresh(token):
  1. TokenService.verifyToken(token) → TokenInfo(userCode, username)
  2. SessionStore.get(token) → Session
  3. 若 Session 不存在 → 返回 null（上游返回 401）
  4. 若 Session 已过期(expireTime < now) → 返回 null
  5. 滑窗续期判断:
     a. 计算剩余 TTL = expireTime - now
     b. 计算累计时长 = expireTime - createTime
     c. 若 剩余TTL < threshold && 累计 < maxTtl && 距上次续期 > interval:
        SessionStore.refresh(token, 剩余TTL + threshold)
        更新 Session.expireTime
  6. 返回 Session（含 UserIdentity）

getActiveSessions(subjectId):
  1. SessionStore.getSessionIds(subjectId) → sessionId 列表
  2. 批量获取 Session → 过滤已过期的
  3. 返回 SessionInfo 列表（sessionId, createTime, clientIp, userAgent, expireTime）
```

## 验收标准

- [ ] Token → 解析 sessionId → 查 Session → 判断存在且未过期
- [ ] Session 不存在或已过期时返回 null
- [ ] 剩余 TTL < `iam.session.renewal.threshold`（默认 30min）时自动续期
- [ ] 同一 session 在 `iam.session.renewal.interval`（默认 5min）内不重复续期
- [ ] 累计续期不超过 `iam.session.renewal.max-ttl`（默认 48h）
- [ ] `getActiveSessions()` 返回所有活跃 Session 的元数据列表
- [ ] 已过期 Session 不在活跃列表中返回
