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
@Component
class TokenService {
    // 生成 Token
    String generateToken(String userCode, String username, String nickname);

    // 验证 Token，返回 claims 中的 userCode + username + nickname
    TokenInfo verifyToken(String token);

    // 刷新 Token，返回新 Token
    String refreshToken(String oldToken);
}

// 从 Token 中解析出的最小信息（不含 avatar）
class TokenInfo {
    String userCode;
    String username;
    String nickname;
}
```

## 配置属性

| 配置项                            | 说明                      | 默认值        |
|--------------------------------|-------------------------|------------|
| `iam.session.token.secret-key` | HS256 签名密钥（必填，最低 32 字符） | —          |
| `iam.session.token.ttl`        | Token TTL（秒）            | 86400（24h） |

## JWT Claims 设计

| Claim    | 说明       | 示例            |
|----------|----------|---------------|
| sub      | userCode | `user_abc123` |
| username | 用户名      | `zhangsan`    |
| nickname | 昵称       | `张三`          |
| iat      | 签发时间     | epoch 秒       |
| exp      | 过期时间     | iat + TTL 秒   |

> avatar 不放入 claims，存储在 Redis Session 的 userIdentity（JSON）字段中。

## 验收标准

- [ ] 生成 Token 的 claims 仅含 userCode、username、nickname、iat、exp，不包含 avatar 等权限信息
- [ ] Token 签名使用 HS256 算法，密钥长度最低 32 字符
- [ ] TTL 可通过 `iam.session.token.ttl` 配置，默认 86400 秒（24h）
- [ ] 过期 Token 验证失败，返回明确错误
- [ ] 被篡改的 Token 验证失败
- [ ] Token 刷新产生新 Token（新 iat/exp），旧 Token 仍可解析出原始信息
- [ ] `jwt-secret-key` 配置为空或长度不足时启动报错

## 开发进度

### 2026-07-18 — 实现完成

**创建文件：**

| 文件                                      | 说明                                          |
|-----------------------------------------|---------------------------------------------|
| `config/IamSessionTokenProperties.java` | 配置类 `iam.session.token.secret-key` / `ttl`  |
| `bean/TokenInfo.java`                   | 解析结果（userCode/username/nickname）            |
| `service/TokenService.java`             | `@Service`，HS256 签名，generate/verify/refresh |
| `service/TokenServiceTest.java`         | 9 个测试用例（生成/验证/过期/篡改/刷新/配置校验）                |

**变更：**

- `iam-session/pom.xml` 新增 jjwt-api/impl/jackson 依赖（sh-bom 管理版本）
- `IamSessionAutoConfig.java` 新增 `@EnableConfigurationProperties`
- `iam-sdk/util/JwtUtil.java` 标记 `@Deprecated`

**验收状态：** 满足全部 7 条验收标准，IDE 诊断零错误。
