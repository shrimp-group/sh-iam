# STORY-010 — HTTP 安全过滤器

| 属性 | 值 |
|------|-----|
| Story ID | STORY-010 |
| 所属 Epic | SDK 鉴权与安全模块 |
| 所属模块 | iam-sdk |
| 优先级 | P1 |
| 状态 | 待开发 |

## 用户故事

**作为** 系统安全负责人，**我希望** 所有 HTTP 响应自动添加安全头（X-Frame-Options、CSP 等），并验证 CORS 跨域请求的合法性，**以便** 防止点击劫持、XSS 攻击和未授权的跨域访问。

## 验收标准

1. 继承 `OncePerRequestFilter`，Order = `Integer.MIN_VALUE + 2`
2. 添加 6 个安全响应头：
   - X-Frame-Options: DENY
   - X-Content-Type-Options: nosniff
   - X-XSS-Protection: 1; mode=block
   - Referrer-Policy: strict-origin-when-cross-origin
   - Content-Security-Policy: default-src 'self'; ...
   - Permissions-Policy: geolocation=(), microphone=(), camera=()
3. CORS 校验逻辑：
   - CSRF 未启用时直接放行
   - 无 Origin 头视为同源请求，放行
   - 未配置 allowedOrigins 则拒绝
   - Origin 不在白名单且不含 `*` 则拒绝
4. 公开路径和健康检查路径跳过安全检查
5. 安全头和 CORS 均可通过配置开关控制

## 技术实现要点

- 执行顺序：在 RequestWrapperFilter 和 LoggingFilter 之后
- 跳过过滤的路径：包含 `/public/` 的路径、`/actuator/health`、`/health`
- CORS 校验委托给 `SecurityConfig.validateOrigin()`
- 配置项：
  - `iam.sdk.cors.allowed-origins`：允许的跨域来源
  - `iam.sdk.cors.allowed-methods`：允许的 HTTP 方法
  - `iam.sdk.cors.allowed-headers`：允许的请求头
  - `iam.sdk.cors.allow-credentials`：是否允许携带凭证
  - `iam.sdk.cors.max-age`：预检请求缓存时间
  - `iam.sdk.csrf.enabled`：CSRF 校验开关
  - `iam.sdk.security.headers.enabled`：安全头开关

## 依赖故事

- STORY-013（SDK 配置类）

## 涉及文件

| 文件 | 路径 |
|------|------|
| SecurityFilter | iam-sdk/src/main/java/com/wkclz/iam/sdk/filter/SecurityFilter.java |
| SecurityConfig | iam-sdk/src/main/java/com/wkclz/iam/sdk/config/SecurityConfig.java |
