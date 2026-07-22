# US-08：会话生命周期事件 SPI

> **模块**：iam-session（会话管理层）
> **依赖**：US-02（Session 模型）
> **来源设计**：[session-design.md](../../session-design.md) — SES-13
> **讨论日期**：2026-07-19

## 用户故事

**作为** 开发者
**我想要** 会话层定义 `SessionEventListener` 接口（onCreated / onDestroyed / onExpired），会话创建/销毁/过期时通过 Spring
EventBus 发布事件
**以便** 上层模块（如 SSO）可以解耦地监听会话事件并实现审计日志等扩展

## 包含功能点

| ID     | 功能       | 说明                                                                         |
|--------|----------|----------------------------------------------------------------------------|
| SES-13 | 会话事件 SPI | `SessionEventListener` 接口定义 + Spring EventBus 事件类 + 在 SessionManager 中发布事件 |

## 明确不包含

- 不做事件监听的具体实现（属于 US-14）
- 不做登录日志写入（属于 US-14）
- 不做事件存储/持久化
- 不做 `enforceMaxConcurrent` 中的事件发布（CONCURRENT_KICK 枚举已定义，Lua 脚本暂不改）

## 输入

- US-02：`Session` 模型（事件携带的载荷）

## 输出

- `DestroyReason` 枚举（6 个值）
- `SessionEventListener` 接口
- `NoOpSessionEventListener` 默认空实现
- `SessionEvent` 统一事件类（通过 `Type` 枚举区分 CREATED/DESTROYED/EXPIRED）
- `SessionManager` 集成点（4 处）

## 契约接口

```java
interface SessionEventListener {
    void onCreated(Session session);
    void onDestroyed(String sessionId, String subjectId, DestroyReason reason);
    void onExpired(String sessionId, String subjectId);
}

enum DestroyReason {
    LOGOUT, PASSWORD_CHANGED, PASSWORD_RESET_BY_ADMIN,
    USER_DISABLED, CONCURRENT_KICK, SESSION_EXPIRED
}
```

## 事件发布集成点

| 事件                       | 发布位置                                                       | 发布时机                                          |
|--------------------------|------------------------------------------------------------|-----------------------------------------------|
| `SessionEvent.CREATED`   | `SessionManager.createSession()`                           | Session 持久化成功后                                |
| `SessionEvent.DESTROYED` | `SessionManager.destroySession()` / `destroyAllSessions()` | Session 删除后（每个 session 一个事件）                  |
| `SessionEvent.EXPIRED`   | `SessionManager.validateAndRefresh()`                      | 检测到 `redisExpireTime < now` 时，发布事件后删除 Session |

## 影响范围

| 文件                              | 变更类型 | 说明                                                         |
|---------------------------------|------|------------------------------------------------------------|
| `DestroyReason.java`            | 新增   | 6 个枚举值                                                     |
| `SessionEventListener.java`     | 新增   | 接口，3 个方法                                                   |
| `NoOpSessionEventListener.java` | 新增   | 默认空实现                                                      |
| `SessionEvent.java`             | 新增   | 统一事件类（Type + 静态工厂方法 created/destroyed/expired），替代 3 个独立事件类 |
| `SessionManager.java`           | 修改   | 4 处集成点发布 SessionEvent                                      |
| `SessionManagerTest.java`       | 修改   | 新增事件验证断言                                                   |

## 验收标准

- [ ] `SessionEventListener` 接口定义 3 个方法
- [ ] `DestroyReason` 枚举包含 6 个值
- [ ] `NoOpSessionEventListener` 提供默认空实现
- [ ] `SessionManager.createSession()` 发布 `SessionEvent.CREATED`
- [ ] `SessionManager.destroySession()` / `destroyAllSessions()` 发布 `SessionEvent.DESTROYED`（使用 DestroyReason）
- [ ] `SessionManager.validateAndRefresh()` 过期分支发布 `SessionEvent.EXPIRED` 并清理 Session
- [ ] `enforceMaxConcurrent()` 暂不发布事件（CONCURRENT_KICK 枚举已定义，后续实现）
