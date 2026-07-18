# US-08：会话生命周期事件 SPI

> **模块**：iam-session（会话管理层）
> **依赖**：US-02（Session 模型）
> **来源设计**：[session-design.md](../../session-design.md) — SES-13

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

## 输入

- US-02：`Session` 模型（事件携带的载荷）

## 输出

- `SessionEventListener` 接口
- `DestroyReason` 枚举
- `SessionCreatedEvent` 事件类
- `SessionDestroyedEvent` 事件类
- `SessionExpiredEvent` 事件类
- `NoOpSessionEventListener` 默认空实现
- 在 `SessionManager` 的创建/销毁流程中集成事件发布

## 契约接口（概念）

```java
interface SessionEventListener {
    void onCreated(Session session);
    void onDestroyed(String sessionId, String subjectId, DestroyReason reason);
    void onExpired(String sessionId, String subjectId);
}

enum DestroyReason {
    LOGOUT,                  // 用户主动登出
    PASSWORD_CHANGED,        // 用户自己修改密码
    PASSWORD_RESET_BY_ADMIN, // 管理员重置密码
    USER_DISABLED,           // 用户被禁用
    CONCURRENT_KICK,         // 并发会话踢出
    SESSION_EXPIRED          // 会话自然过期
}
```

## 事件发布集成点

| 事件                      | 发布位置                                                       | 发布时机                         |
|-------------------------|------------------------------------------------------------|------------------------------|
| `SessionCreatedEvent`   | `SessionManager.createSession()`                           | Session 持久化成功后               |
| `SessionDestroyedEvent` | `SessionManager.destroySession()` / `destroyAllSessions()` | Session 删除后（每个 session 一个事件） |
| `SessionExpiredEvent`   | 定时任务或惰性检测                                                  | 检测到 Session 过期时              |

## 验收标准

- [ ] `SessionEventListener` 接口定义 3 个方法
- [ ] `DestroyReason` 枚举包含上述 6 个值
- [ ] `NoOpSessionEventListener` 提供默认空实现，上层未注入时不 NPE
- [ ] Spring EventBus 事件发布使用 `@EventListener` 或 `ApplicationEventPublisher`
- [ ] 事件类包含完整上下文信息（Session 或 sessionId + subjectId + reason）
- [ ] 事件监听是异步的（`@Async`），不阻塞主流程
