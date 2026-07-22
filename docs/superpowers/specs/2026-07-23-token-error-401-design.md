# Token 解析异常以 401 返回给前端

## 问题描述

`TokenService.parseClaims()` 在解析 JWT 时捕获各类 JWT 异常（过期、签名错误、格式错误等），将其包装为
`IllegalArgumentException` 并携带具体原因。但这个异常在 `SessionAuthFilter` 中未被捕获，传播到 Servlet 容器后返回 **500
Internal Server Error**，前端无法获知具体失败原因。

## 目标

- JWT 解析异常（过期/签名错误/格式错误/不支持/参数错误）以 **401 UNAUTHORIZED** 返回
- 响应体中携带具体的异常原因，便于前端提示用户
- 非 JWT 异常（Redis 连接异常、JSON 解析异常等）保持现有 500 行为不变

## 设计方案

### 修改文件

| 文件                                                                              | 改动                                                  |
|---------------------------------------------------------------------------------|-----------------------------------------------------|
| `iam-session/src/main/java/com/wkclz/iam/session/filter/SessionAuthFilter.java` | 在 `doFilterInternal` 中捕获 `IllegalArgumentException` |

### 代码变更

在 `SessionAuthFilter.doFilterInternal()` 中，将 `sessionManager.validateAndRefresh(token)` 调用包装 try-catch：

```java
// 原有 null 检查之前，外层加 try-catch
try{
Session session = sessionManager.validateAndRefresh(token);
    if(session ==null||session.

getUserIdentity() ==null){
    log.

warn("Invalid session for URI: {}",requestUri);

writeUnauthorized(response, "会话无效或已过期");
        return;
            }
            }catch(
IllegalArgumentException e){
    log.

warn("Token validation failed for URI: {}, reason: {}",requestUri, e.getMessage());

writeUnauthorized(response, e.getMessage());
    return;
    }
```

### 异常映射表

| 异常场景         | 抛出信息              | 前端收到 message      |
|--------------|-------------------|-------------------|
| Token 过期     | `Token 已过期: ...`  | `Token 已过期: ...`  |
| 签名被篡改        | `Token 签名错误: ...` | `Token 签名错误: ...` |
| Token 格式非法   | `Token 格式错误: ...` | `Token 格式错误: ...` |
| 不支持的 Token   | `不支持的 Token: ...` | `不支持的 Token: ...` |
| 参数错误（null 等） | `Token 参数错误: ...` | `Token 参数错误: ...` |

### 响应格式

与现有 `writeUnauthorized` 方法一致：

```json
{
    "message": "Token 已过期: ..."
}
```

HTTP Status: **401**

### 非功能性影响

- **日志**: 异常路径输出 `log.warn`（不是 error），包含 requestUri 和具体原因
- **性能**: 无影响，catch 分支仅在异常路径触发
- **安全**: 异常 message 直接透传前端，不会暴露敏感内部细节（异常信息来自 JJWT 库，不含堆栈）

### 测试

无需新增测试。现有 `TokenServiceTest` 已覆盖各类异常场景的 `IllegalArgumentException` 抛出行为。
