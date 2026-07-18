# US-05：会话创建与并发会话控制

> **模块**：iam-session（会话管理层）
> **依赖**：US-01（IdentityContext）、US-02（SessionStore）、US-03（TokenService）、US-04（RedisSessionStore）
> **来源设计**：[session-design.md](../../session-design.md) — SES-02, SES-11

## 用户故事

**作为** 用户
**我想要** 登录成功后系统为我创建一个会话（输入 UserIdentity + authType + 客户端信息 → 生成 Token → 持久化 → 返回
Session + Token），并在超出最大并发数时自动踢出最早会话
**以便** 我能获得有效的登录凭证，且账号安全受并发限制保护

## 包含功能点

| ID     | 功能     | 说明                                                                                  |
|--------|--------|-------------------------------------------------------------------------------------|
| SES-02 | 会话创建   | 输入 UserIdentity + authType + 客户端信息 → 生成 Session + Token → 持久化 → 返回 (Session, Token) |
| SES-11 | 并发会话控制 | 同一用户最大活跃会话数限制，超出时踢最早会话（按创建时间排序）；`maxConcurrent` 可配置（0=不限制）                          |

## 明确不包含

- 不做 Token 生成逻辑（委托 US-03 的 TokenService）
- 不做 Session 存储逻辑（委托 US-04 的 RedisSessionStore）
- 不做密码校验或用户认证（属于 US-10）

## 输入

- US-01：`UserIdentity`（作为 createSession 的输入）
- US-02：`SessionStore` 接口
- US-03：`TokenService.generateToken()`
- US-04：`RedisSessionStore`（运行时实现）

## 输出

- `SessionManager.createSession()` 方法
- `SessionCreateResult` 结果类（token + Session）
- 配置项：`iam.session.max-concurrent`（默认 0 不限制）

## 核心流程

```
createSession(userIdentity, authType):
  1. TokenService.generateToken(userCode, username, nickname) → token
  2. sessionId = MD5(token)（固定 32 字符）
  3. 构建 Session 对象（sessionId=MD5, subjectId, authType, token, createTime, expireTime）
  4. 并发控制 enforceMaxConcurrent(subjectId):
     a. Lua 脚本原子执行 ZCARD → ZRANGE(0,0) → DEL + ZREM
     b. 循环执行直到 count < maxConcurrent
  5. SessionStore.save(session, ttl)
  6. 返回 SessionCreateResult(token, session)
```

## 验收标准

- [ ] 传入 UserIdentity + authType → 返回 SessionCreateResult(token, session)
- [ ] sessionId = MD5(token)，固定 32 字符
- [ ] `maxConcurrent` 通过 `iam.session.max-concurrent` 配置，默认 0（不限制）
- [ ] 并发控制通过 Lua 脚本保证原子性（ZRANGE + DEL 在同一脚本中）
- [ ] 超出限制时踢出创建时间最早的会话，日志记录被踢会话信息
- [ ] 会话创建成功后 Session 已持久化到 Redis

## 开发进度

### 2026-07-18 — 实现完成

**创建文件：**

| 文件                                | 说明                                           |
|-----------------------------------|----------------------------------------------|
| `bean/SessionCreateResult.java`   | 创建结果（token + session）                        |
| `service/SessionManager.java`     | 会话管理器（`@Component`，createSession + Lua 并发控制） |
| `service/SessionManagerTest.java` | 7 个测试用例（创建/关联/持久化/并发/配置）                     |

**变更：**

- `config/IamSessionConfig.java` 新增 `maxConcurrent` 字段（默认 0）

**验收状态：** 满足全部 6 条验收标准，IDE 诊断零错误。
