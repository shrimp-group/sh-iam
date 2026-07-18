# US-02：会话领域模型与持久化 SPI 定义

> **模块**：iam-session（会话管理层）
> **依赖**：US-01（UserIdentity）
> **来源设计**：[session-design.md](../../session-design.md) — SES-01, SES-05

## 用户故事

**作为** 开发者
**我想要** 定义 `Session` 领域模型和 `SessionStore` 持久化 SPI 接口（save / get / delete / deleteBySubjectId /
getSessionIds / refresh）
**以便** 会话存储可抽象化，后续可替换不同后端实现

## 包含功能点

| ID     | 功能        | 说明                                                             |
|--------|-----------|----------------------------------------------------------------|
| SES-01 | 会话模型      | `Session` 对象：sessionId、subjectId、authType、userIdentity(JSON) 等 |
| SES-05 | 会话持久化 SPI | `SessionStore` 接口定义 6 个方法，不含实现                                 |

## 明确不包含

- 不做 Redis 实现（属于 US-04）
- 不做会话创建/销毁等业务逻辑（属于 US-05/06/07）
- 不做 Token 相关（属于 US-03）

## 输入

- US-01：`UserIdentity` 接口（Session 中引用）

## 输出

- `Session` 领域模型类
- `AuthType` 枚举（PASSWORD / WECHAT_MINI / LDAP / OAUTH）
- `SessionStore` SPI 接口

## Session 模型字段

| 字段           | 类型                    | 说明                            |
|--------------|-----------------------|-------------------------------|
| sessionId    | String                | 会话唯一标识（使用 token 作为 sessionId） |
| subjectId    | String                | 用户标识（userCode）                |
| authType     | AuthType              | 认证方式                          |
| userIdentity | String                | UserIdentity 的 JSON 序列化       |
| clientIp     | String                | 客户端 IP                        |
| userAgent    | String                | 客户端 UA                        |
| createTime   | long                  | 创建时间戳                         |
| expireTime   | long                  | 过期时间戳                         |
| metadata     | Map\<String, String\> | 扩展属性                          |

## 契约接口（概念）

```java
enum AuthType {
    PASSWORD, WECHAT_MINI, LDAP, OAUTH
}

class Session {
    String sessionId;
    String subjectId;
    AuthType authType;
    String userIdentity; // JSON
    String clientIp;
    String userAgent;
    long createTime;
    long expireTime;
    Map<String, String> metadata;
}

interface SessionStore {
    void save(Session session, long ttlSeconds);
    Session get(String sessionId);
    void delete(String sessionId);
    void deleteBySubjectId(String subjectId);
    void refresh(String sessionId, long ttlSeconds);
    List<String> getSessionIds(String subjectId);
}
```

## 验收标准

- [ ] `Session` 包含上述全部字段
- [ ] `AuthType` 枚举包含 PASSWORD、WECHAT_MINI、LDAP、OAUTH
- [ ] `SessionStore` 接口定义 6 个方法，无实现
- [ ] 不依赖任何具体存储技术（无 Redis、无 JDBC 依赖）
- [ ] 不包含任何业务逻辑
