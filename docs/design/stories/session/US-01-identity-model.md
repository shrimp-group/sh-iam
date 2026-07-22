# US-01：用户身份模型与请求级上下文

> **归属**：`sh-core`（框架层 `com.wkclz.core.identity`）
> **依赖**：仅 JDK + Lombok
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

| 类                 | 包                         | 说明                                   |
|-------------------|---------------------------|--------------------------------------|
| `UserIdentity`    | `com.wkclz.core.identity` | 用户身份实体类（含 appCode/tenantCode 线程变量）   |
| `IdentityContext` | `com.wkclz.core.identity` | 身份上下文工具类，全部 static 方法，ThreadLocal 实现 |

> 已从独立模块 `sh-identity` 迁移至框架层 `sh-core`。

## 契约

```java
// 用户身份 — 简单实体类（@Data + Serializable）
public class UserIdentity implements Serializable {
    private String userCode;
    private String username;
    private String nickname;
    private String avatar;
    private Map<String, Object> attributes = Collections.emptyMap();
    // addAttribute(key, value) 便捷方法
}
```

```java
// 身份上下文 — final 类，全部 static 方法
public final class IdentityContext {
    // 身份
    public static void set(UserIdentity identity, String token);

    public static UserIdentity get();

    public static String getToken();

    public static String getUserCode();

    public static String getUsername();

    public static String getNickname();

    public static String getAvatar();

    // 应用/租户（独立于 UserIdentity，线程变量）
    public static void setAppCode(String appCode);

    public static String getAppCode();

    public static void setTenantCode(String tenantCode);

    public static String getTenantCode();

    // 清理
    public static void clear();
}
```

## 验收标准

- [ ] `UserIdentity` 仅含 userCode / username / nickname / avatar / attributes，不含任何权限字段
- [ ] `IdentityContext.set()` 后同线程 `get()` 可获取到相同身份
- [ ] `IdentityContext.clear()` 后 `get()` 返回 null
- [ ] 不依赖任何外部框架（无 Spring、无 Servlet、无 Redis）
- [ ] 不依赖项目中任何其他模块

## 开发进度

### 2026-07-18 — 迁移至 sh-core

`UserIdentity` 和 `IdentityContext` 已从独立模块 `sh-identity` 迁移至框架层
`sh-core/src/main/java/com/wkclz/core/identity/`。`UserIdentity` 使用 `@Data`（Lombok）+ `Serializable`。原 `sh-identity`
模块已移除。

### 2026-07-18 — 简化重构

删除 `SimpleUserIdentity` 和 `DefaultIdentityContext`，`UserIdentity` 改为简单实体类，`IdentityContext` 改为全部 static
方法的 final 类，新增 `getUserCode()`/`getUsername()`/`getNickname()`/`getAvatar()` + `setAppCode`/`getAppCode`/
`setTenantCode`/`getTenantCode` 便捷方法。

**验收状态：** 满足所有 5 条验收标准。
