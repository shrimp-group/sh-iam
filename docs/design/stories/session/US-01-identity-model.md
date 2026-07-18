# US-01：用户身份模型与请求级上下文

> **模块**：iam-identity（用户获取契约层）
> **依赖**：无（纯 JDK 依赖）
> **来源设计**：[session-design.md](../../session-design.md) — IDT-01, IDT-02

## 用户故事

**作为** 系统架构师
**我想要** 一个极简的用户身份模型（`UserIdentity`）和基于 ThreadLocal 的请求级身份上下文（`IdentityContext`），提供
set/get/clear 能力
**以便** 所有上层模块都能以零外部依赖的方式获取"当前请求是谁"

## 包含功能点

| ID     | 功能       | 说明                                                      |
|--------|----------|---------------------------------------------------------|
| IDT-01 | 用户身份模型   | `UserIdentity`：userCode、username、nickname、avatar（仅标识字段） |
| IDT-02 | 请求级身份上下文 | 每个请求线程独立的身份持有者，ThreadLocal 实现，支持 set / get / clear      |

## 明确不包含

- 不做身份上下文的自动清理（属于 US-09 认证过滤器）
- 不做 Token 提取（属于 US-09）
- 不做白名单路径匹配（属于 US-09）
- 不做 JWT 解析
- 不做 Session 查活
- 不做权限判断
- 不做用户信息数据库查询

## 输入

无 — 纯 JDK 依赖，零外部框架依赖

## 输出

- `UserIdentity` 接口
- `IdentityContext` 接口
- `DefaultIdentityContext` — ThreadLocal 默认实现

## 契约接口（概念）

```java
// 用户身份 — 最精简的信息集
interface UserIdentity {
    String getUserCode();

    String getUsername();

    String getNickname();

    String getAvatar();

    // 扩展属性（小程序 openid 等场景使用）
    Map<String, Object> getAttributes();
}

// 身份上下文 — 请求级，纯 ThreadLocal，零外部依赖
interface IdentityContext {
    void set(UserIdentity identity, String token);

    UserIdentity get();

    String getToken();

    void clear();
}
```

## 验收标准

- [ ] `UserIdentity` 仅含 userCode / username / nickname / avatar / attributes，不含任何权限字段
- [ ] `IdentityContext.set()` 后同线程 `get()` 可获取到相同身份
- [ ] `IdentityContext.clear()` 后 `get()` 返回 null
- [ ] 不依赖任何外部框架（无 Spring、无 Servlet、无 Redis）
- [ ] 不依赖项目中任何其他模块
