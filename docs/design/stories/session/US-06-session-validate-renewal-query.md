# US-06：会话验证、滑窗续期与活跃会话查询

> **模块**：iam-session（会话管理层）
> **依赖**：US-02（SessionStore）、US-03（TokenService）、US-05（SessionManager.createSession）
> **来源设计**：[session-design.md](../../session-design.md) — SES-07, SES-08, SES-12
> **讨论日期**：2026-07-19

## 用户故事

**作为** 用户
**我想要** 每次请求时系统自动验证我的会话是否有效（存在且未过期），当 Redis 剩余有效期不足 30 分钟时自动续期（累计不超过 JWT
过期时间），并能查看自己的所有活跃会话
**以便** 我的登录状态持续有效，且能掌握账号的登录情况

## 核心概念变更（与原始设计的差异）

| 概念             | 原始设计                 | 变更后                               | 原因                                              |
|----------------|----------------------|-----------------------------------|-------------------------------------------------|
| JWT 有效期        | `token.ttl`（当时 24h）  | 48h                               | 减少 Token 刷新频率                                   |
| Redis 存活时间     | = JWT TTL            | 独立 `redis-ttl`，默认 24h             | Redis 资源占用与 Token 有效性解耦                         |
| 续期对象           | 续 JWT + Redis        | **只续 Redis**，不重新签发 JWT            | 简化续期流程，被踢时更容易感知                                 |
| 累计续期上限         | `renewal.max-ttl` 配置 | **JWT 的 `exp` 即为硬顶**              | 自然约束，无需额外配置；续期后的 `redisExpireTime` 不能超过 JWT.exp |
| Token issuedAt | `new Date(now)`      | `new Date(now - 60_000)`（前推 1 分钟） | 各服务器时钟不准确，避免 Token 还未生效                         |

**新增 Session 字段：**

| 字段                | 类型   | 说明                                                       |
|-------------------|------|----------------------------------------------------------|
| `lastRenewalTime` | long | 上次续期时间戳（毫秒），初始 = `createTime`，用于续期间隔判断                   |
| `redisExpireTime` | long | Redis Key 实际过期时间戳（毫秒），初始 = `now + redisTtl * 1000`，续期时更新 |

## 包含功能点

| ID     | 功能          | 说明                                                                        |
|--------|-------------|---------------------------------------------------------------------------|
| SES-07 | 会话验证        | 从 Token 定位 Session，检查 Session 是否存在且 Redis 未过期                             |
| SES-08 | 会话 TTL 滑窗续期 | Redis 剩余 TTL < 阈值（默认 30min）时续期 +30min；累计不超过 JWT.exp；续期间隔可配置（如 5min 内不重复续） |
| SES-12 | 活跃会话查询      | 按用户查询所有活跃 Session 列表（sessionId, authType, createTime）                     |

## 明确不包含

- 不做 Token 从 HTTP 请求提取（属于 US-09）
- 不做 IdentityContext 设置（属于 US-09）
- 不做白名单路径判断（属于 US-09）
- `getActiveSessions()` 不返回 clientIp/userAgent（Session 模型不存这些字段，由调用方补充）

## 输入

- US-03：`TokenService.verifyToken()` — JWT 签名/过期校验
- US-05：`SessionStore`（save/get/getSessionIds）
- US-05：`SessionManager.createSession()` — 创建时初始化新字段

## 输出

- `SessionManager.validateAndRefresh(token)` → Session | null
- `SessionManager.getActiveSessions(subjectId)` → List\<Session\>
- `SessionStore.renewSession(sessionId, newRedisExpireTime)` — 新增方法
- 配置项：
    - `iam.session.redis-ttl`：Redis 初始 TTL（默认 86400 秒 = 24h）
    - `iam.session.renewal.threshold`：续期阈值（默认 1800 秒 = 30min）
    - `iam.session.renewal.interval`：续期间隔（默认 300 秒 = 5min）

## 核心流程

### validateAndRefresh(token)

```
1. TokenService.verifyToken(token) → TokenInfo（含 JWT exp，由 JJWT 库已预校验签名+过期）
2. sessionId = MD5(token)
3. SessionStore.get(sessionId) → Session
4. Session == null → return null（上游返回 401，log.warn）
5. redisExpireTime < now → return null（Redis 已过期，上游返回 401，log.warn）
6. 续期判断（全部满足才续）：
   a. redisExpireTime - now < threshold（Redis 剩余不足 30min）
   b. 且 now - lastRenewalTime > interval（距上次续期 > 5min）
   c. 且 JWT.exp > now + threshold（JWT 还有续期空间）
   → 计算 newRedisExpire = min(now + threshold * 1000, JWT.exp（毫秒时间戳）)
   → SessionStore.renewSession(sessionId, newRedisExpire)
   → 更新内存中的 Session.redisExpireTime / lastRenewalTime
7. 返回 Session（含反序列化的 UserIdentity）
```

### getActiveSessions(subjectId)

```
1. SessionStore.getSessionIds(subjectId) → sessionId 列表
2. 批量 SessionStore.get() → 按 redisExpireTime 过滤（>= now）
3. 返回存活 Session 列表
```

### SessionStore.renewSession(sessionId, newRedisExpireTime)

```
通过 Pipeline 原子执行：
1. HSET: key → {redisExpireTime, lastRenewalTime}
2. EXPIRE: key → (newRedisExpireTime - now) / 1000 秒
```

## 影响范围

| 文件                        | 变更类型 | 说明                                                                                                   |
|---------------------------|------|------------------------------------------------------------------------------------------------------|
| `Session.java`            | 修改   | +`lastRenewalTime`, +`redisExpireTime`                                                               |
| `IamSessionConfig.java`   | 修改   | +`redisTtl`, +`renewalThreshold`, +`renewalInterval`                                                 |
| `SessionManager.java`     | 修改   | +`validateAndRefresh()`, +`getActiveSessions()`, `createSession()` 中 expireTime → redisExpireTime 适配 |
| `SessionStore.java`       | 修改   | +`renewSession()`, `save()` 增加存储 `lastRenewalTime`/`redisExpireTime`                                 |
| `TokenInfo.java`          | 修改   | +`expireAt`（JWT exp 毫秒时间戳），供 validateAndRefresh 计算续期上限                                               |
| `TokenService.java`       | 修改   | `verifyToken()` 解析并设置 `expireAt`                                                                     |
| `SessionManagerTest.java` | 修改   | 新增验证/续期/查询测试用例                                                                                       |

## 验收标准

- [ ] JWT 签名错误/过期时 TokenService.verifyToken() 抛异常，validateAndRefresh() 不捕获（由上游调用方处理）
- [ ] Session 不存在（Redis 中无 Hash）时 validateAndRefresh() 返回 null
- [ ] `redisExpireTime < now`（Redis key 已过期但 Key 仍在）时 validateAndRefresh() 返回 null
- [ ] Redis 剩余 TTL < `iam.session.renewal.threshold`（默认 30min）时自动续期 Redis
- [ ] 同一 session 在 `iam.session.renewal.interval`（默认 5min）内不重复续期
- [ ] 续期后的 `redisExpireTime` 不超过 JWT 的 `exp`
- [ ] 续期只操作 Redis（HSET + EXPIRE），不重新签发 JWT
- [ ] `getActiveSessions()` 返回所有 `redisExpireTime >= now` 的 Session 列表
- [ ] `createSession()` 初始化 `redisExpireTime = now + redisTtl * 1000`，`lastRenewalTime = createTime`
