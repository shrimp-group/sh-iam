# US-04：Redis 会话存储实现

> **模块**：iam-session（会话管理层）
> **依赖**：US-02（SessionStore 接口）
> **来源设计**：[session-design.md](../../session-design.md) — SES-06

## 用户故事

**作为** 开发者
**我想要** 基于 Redis 的 `SessionStore` 实现，使用 Hash 结构存储 Session 字段 + ZSet 维护用户会话索引，Key 为
`iam:session:{sessionId}` 和 `iam:session:index:{subjectId}`
**以便** 会话数据持久化到 Redis 并支持 TTL 自动过期

## 包含功能点

| ID     | 功能         | 说明                                                                |
|--------|------------|-------------------------------------------------------------------|
| SES-06 | Redis 会话存储 | `RedisSessionStore` 实现 `SessionStore` 接口，Hash + ZSet 结构，简化 Key 设计 |

## 明确不包含

- 不做 `SessionStore` 接口定义（属于 US-02）
- 不做会话创建/销毁业务逻辑（属于 US-05/07）
- 不做会话验证/续期（属于 US-06）

## 输入

- US-02：`SessionStore` 接口、`Session` 模型

## 输出

- `RedisSessionStore` 实现类
- Redis Key 前缀常量

## Redis Key 设计

| Key                             | 类型   | 内容                                                                                              | TTL          |
|---------------------------------|------|-------------------------------------------------------------------------------------------------|--------------|
| `iam:session:{sessionId}`       | Hash | sessionId, subjectId, authType, userIdentity(JSON), clientIp, userAgent, createTime, expireTime | 可配置 (默认 24h) |
| `iam:session:index:{subjectId}` | ZSet | member=sessionId, score=createTime                                                              | 跟随 session   |

> 相比现状从 3 个 Key 简化为 2 个，sessionId 直接使用 UUID + token 前缀，不再需要 MD5 单向映射。

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
