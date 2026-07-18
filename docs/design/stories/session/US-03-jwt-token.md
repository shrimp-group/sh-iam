# US-03：JWT 令牌生成、验证与刷新

> **模块**：iam-session（会话管理层）
> **依赖**：US-01（UserIdentity）
> **来源设计**：[session-design.md](../../session-design.md) — SES-03, SES-04, SES-14

## 用户故事

**作为** 开发者
**我想要** JWT HS256 令牌的生成（claims 仅含 userCode/username，TTL 可配置默认 24h）、签名+过期校验、以及基于旧令牌签发新令牌的刷新能力
**以便** 会话可通过令牌进行标识、验证和延期

## 包含功能点

| ID     | 功能       | 说明                                                             |
|--------|----------|----------------------------------------------------------------|
| SES-03 | Token 生成 | JWT HS256 签名，claims 仅含 userCode/username（最小信息集），TTL 可配置，默认 24h |
| SES-04 | Token 验证 | JWT 签名 + 过期校验，解析出 userCode/username                            |
| SES-14 | Token 刷新 | 基于旧 Token 签发新 Token（新 iat/exp），同步更新 Session 中的 Token 绑定        |

## 明确不包含

- 不做 Token 从 HTTP 请求中提取（属于 US-09）
- 不做 Session 查活（属于 US-06）
- 不做 Token 与 Session 的关联维护（属于 US-05 的 createSession）

## 输入

- US-01：`UserIdentity`（claims 中引用 userCode/username）

## 输出

- `TokenService`（或 `JwtService`）— 生成/验证/刷新方法
- 配置项：`iam.session.token.ttl`（默认 24h）、`iam.session.jwt.secret-key`（HS256 密钥，最低 32 字符）

## 核心方法（概念）

```java
interface TokenService {
    // 生成 Token
    String generateToken(String userCode, String username);

    // 验证 Token，返回 claims 中的 userCode + username
    TokenInfo verifyToken(String token);

    // 刷新 Token，返回新 Token
    String refreshToken(String oldToken);
}

// 从 Token 中解析出的最小信息
class TokenInfo {
    String userCode;
    String username;
}
```

## JWT Claims 设计

| Claim    | 说明       | 示例            |
|----------|----------|---------------|
| sub      | userCode | `user_abc123` |
| username | 用户名      | `zhangsan`    |
| iat      | 签发时间     | epoch 秒       |
| exp      | 过期时间     | iat + TTL 秒   |

## 验收标准

- [ ] 生成 Token 的 claims 仅含 userCode、username、iat、exp，不包含权限信息
- [ ] Token 签名使用 HS256 算法，密钥长度最低 32 字符
- [ ] TTL 可通过 `iam.session.token.ttl` 配置，默认 86400 秒（24h）
- [ ] 过期 Token 验证失败，返回明确错误
- [ ] 被篡改的 Token 验证失败
- [ ] Token 刷新产生新 Token（新 iat/exp），旧 Token 仍可解析出原始信息
- [ ] `jwt-secret-key` 配置为空或长度不足时启动报错
