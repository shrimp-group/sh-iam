# US-04：Redis 会话存储实现（已合并至 US-02）

> **状态**：已合并 — 2026-07-18
> **合并目标**：[US-02-session-model-spi.md](./US-02-session-model-spi.md)

## 合并原因

`SessionStore` 仅有一个 Redis 实现，不适合剥离接口与实现。`SessionStore` 改为具体类，原有 US-04 的 Redis Key
设计、核心方法实现要点等内容已合并至 US-02。

## 原始内容

<details>
<summary>点击展开原始设计内容</summary>

---

> **模块**：iam-session（会话管理层）
> **依赖**：US-02（SessionStore 接口）
> **来源设计**：[session-design.md](../../session-design.md) — SES-06

## 用户故事

**作为** 开发者
**我想要** 基于 Redis 的 `SessionStore` 实现，使用 Hash 结构存储 Session 字段 + ZSet 维护用户会话索引
**以便** 会话数据持久化到 Redis 并支持 TTL 自动过期

## 包含功能点

| ID     | 功能         | 说明                                                                |
|--------|------------|-------------------------------------------------------------------|
| SES-06 | Redis 会话存储 | `RedisSessionStore` 实现 `SessionStore` 接口，Hash + ZSet 结构，简化 Key 设计 |

## Redis Key 设计

| Key                             | 类型   | 内容                                                                                              | TTL          |
|---------------------------------|------|-------------------------------------------------------------------------------------------------|--------------|
| `iam:session:{sessionId}`       | Hash | sessionId, subjectId, authType, userIdentity(JSON), clientIp, userAgent, createTime, expireTime | 可配置 (默认 24h) |
| `iam:session:index:{subjectId}` | ZSet | member=sessionId, score=createTime                                                              | 跟随 session   |

## 核心方法实现要点

| 方法                   | Redis 操作                                                         |
|----------------------|------------------------------------------------------------------|
| `save(session, ttl)` | `HSET iam:session:{id} field value...` + `EXPIRE` + `ZADD index` |
| `get(sessionId)`     | `HGETALL iam:session:{id}` → 反序列化为 Session                       |
| `delete(sessionId)`  | `DEL iam:session:{id}` + `ZREM index:{subjectId} {id}`           |
| `deleteBySubjectId`  | `ZRANGE index:{sub}` → 批量 `DEL` session Key + `DEL` index Key    |
| `refresh(id, ttl)`   | `EXPIRE iam:session:{id} {ttl}`                                  |
| `getSessionIds(sub)` | `ZRANGE index:{sub} 0 -1`                                        |

## 验收标准

- [ ] Session 以 Hash 结构存入 Redis，所有字段正确序列化/反序列化
- [ ] `save` 同时维护 ZSet 索引，score 为 createTime
- [ ] `delete` 同步从 ZSet 移除对应 member
- [ ] `deleteBySubjectId` 使用 Pipeline 批量删除，避免逐个 DEL
- [ ] `refresh` 正确更新 Hash Key 的 TTL
- [ ] 可通过配置 `iam.session.store.redis.ttl` 设置默认 TTL
- [ ] Redis 连接异常时有合理的错误处理和日志

---

</details>
