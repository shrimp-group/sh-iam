# US-02：会话领域模型与会话持久化

> **模块**：iam-session（会话管理层）
> **依赖**：US-01（UserIdentity，来自 sh-core `com.wkclz.core.identity`）
> **来源设计**：[session-design.md](../../session-design.md) — SES-01, SES-05, SES-06

## 用户故事

**作为** 开发者
**我想要** 定义 `Session` 领域模型并使用 Redis 持久化会话数据（Hash + ZSet 结构）
**以便** 会话数据可靠存储在 Redis 中，支持 TTL 自动过期和按用户查询

## 包含功能点

| ID     | 功能        | 说明                                             |
|--------|-----------|------------------------------------------------|
| SES-01 | 会话模型      | `Session` 对象 + `AuthType` 枚举                   |
| SES-05 | 会话持久化     | `SessionStore` 具体类（直接依赖 Redis，无接口/实现分离）        |
| —      | Redis Key | 合并原 US-04 内容：Hash + ZSet 结构、Pipeline 批量删除等实现要点 |

## 明确不包含

- 不做会话创建/销毁等业务逻辑（属于 US-05/07）
- 不做 Token 相关（属于 US-03）
- 不做会话验证/续期（属于 US-06）

## 输入

- US-01：`UserIdentity`（`com.wkclz.core.identity`，Session 中引用）

## 输出

- `Session` 领域模型类
- `AuthType` 枚举
- `SessionStore` 具体类（Redis 实现）
- Redis Key 前缀常量

## Session 模型字段

| 字段           | 类型                    | 说明                          |
|--------------|-----------------------|-----------------------------|
| sessionId    | String                | 会话唯一标识（MD5(token)，固定 32 字符） |
| subjectId    | String                | 用户标识（userCode）              |
| authType     | AuthType              | 认证方式                        |
| token        | String                | 原始 JWT Token（存储于 Hash 中）    |
| userIdentity | String                | UserIdentity 的 JSON 序列化     |
| clientIp     | String                | 客户端 IP                      |
| userAgent    | String                | 客户端 UA                      |
| createTime   | long                  | 创建时间戳（毫秒）                   |
| expireTime   | long                  | 过期时间戳（毫秒）                   |
| metadata     | Map\<String, String\> | 扩展属性                        |

## 契约

```java
// 认证方式枚举
enum AuthType {
    PASSWORD,      // 用户名密码
    WECHAT_MINI,   // 微信小程序
    WECHAT_MP,     // 微信公众号授权
    LDAP,          // LDAP
    OAUTH          // OAuth 2.0
}

// 会话模型
class Session {
    String sessionId;   // MD5(token)，固定 32 字符
    String subjectId;
    AuthType authType;
    String token;       // 原始 JWT Token
    String userIdentity; // JSON
    String clientIp;
    String userAgent;
    long createTime;
    long expireTime;
    Map<String, String> metadata;
}

// 会话持久化 — 具体类，直接依赖 StringRedisTemplate
// sessionId = MD5(token)，查找时对 token 做 MD5 定位 Redis Key
class SessionStore {
    void save(Session session, long ttlSeconds);
    Session get(String sessionId);
    void delete(String sessionId);
    void deleteBySubjectId(String subjectId);
    void refresh(String sessionId, long ttlSeconds);
    List<String> getSessionIds(String subjectId);
}
```

> `SessionStore` 不设接口，仅有 Redis 一个实现。若未来需要其他后端，届时再抽象。

## Redis Key 设计

| Key                             | 类型   | 内容                                                                                                              | TTL          |
|---------------------------------|------|-----------------------------------------------------------------------------------------------------------------|--------------|
| `iam:session:{sessionId}`       | Hash | sessionId(MD5), subjectId, authType, userIdentity(JSON), clientIp, userAgent, createTime, expireTime, token(原始) | 可配置 (默认 24h) |
| `iam:session:index:{subjectId}` | ZSet | member=sessionId(MD5), score=createTime                                                                         | 跟随 session   |

> sessionId = MD5(token)，固定 32 字符，方便 Redis 管理工具浏览。Hash 内存储原始 token 字段用于反向定位。

## 核心方法实现要点

| 方法                   | Redis 操作                                                                                               |
|----------------------|--------------------------------------------------------------------------------------------------------|
| `save(session, ttl)` | `HSET iam:session:{MD5} field value...` + `EXPIRE {ttl}` + `ZADD index:{subjectId} {createTime} {MD5}` |
| `get(sessionId)`     | `HGETALL iam:session:{MD5}` → 反序列化为 Session                                                            |
| `delete(sessionId)`  | `DEL iam:session:{MD5}` + `ZREM index:{subjectId} {MD5}`                                               |
| `deleteBySubjectId`  | `ZRANGE index:{sub} 0 -1` → Pipeline 批量 `DEL` session Key → `DEL` index Key                            |
| `refresh(id, ttl)`   | `EXPIRE iam:session:{MD5} {ttl}`                                                                       |
| `getSessionIds(sub)` | `ZRANGE index:{sub} 0 -1`                                                                              |

## 验收标准

- [ ] `Session` 包含全部字段（含 token 原始值）
- [ ] `AuthType` 枚举包含 PASSWORD、WECHAT_MINI、WECHAT_MP、LDAP、OAUTH
- [ ] `SessionStore` 为具体类（非接口），直接依赖 `StringRedisTemplate`
- [ ] Session 以 Hash 结构存入 Redis，所有字段正确序列化/反序列化
- [ ] `save` 同时维护 ZSet 索引，score 为 createTime
- [ ] `delete` 同步从 ZSet 移除对应 member
- [ ] `deleteBySubjectId` 使用 Pipeline 批量删除，避免逐个 DEL
- [ ] `refresh` 正确更新 Hash Key 的 TTL
- [ ] 可通过配置 `iam.session.store.redis.ttl` 设置默认 TTL
- [ ] Redis 连接异常时有合理的错误处理和日志

## 开发进度

### 2026-07-18 — 设计优化

- `AuthType` 新增 `WECHAT_MP`（微信公众号授权登录）
- `SessionStore` 取消接口/实现分离，改为具体类（仅有 Redis 实现，无抽象必要）
- 合并原 US-04 内容（Redis Key 设计、核心方法实现要点、Pipeline 批量删除）
- sessionId 使用 MD5(token) 单向映射，固定 32 字符，方便 Redis 管理工具浏览；Hash 内存储原始 token
- Session 新增 `token` 字段（存储原始 JWT Token）
- 验收标准合并为 10 条
