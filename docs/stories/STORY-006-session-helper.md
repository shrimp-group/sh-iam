# STORY-006 — 用户会话上下文管理

| 属性 | 值 |
|------|-----|
| Story ID | STORY-006 |
| 所属 Epic | SDK 鉴权与安全模块 |
| 所属模块 | iam-sdk |
| 优先级 | P0 |
| 状态 | 待开发 |

## 用户故事

**作为** 业务开发者，**我希望** 在请求处理过程中能方便地获取当前登录用户的信息（userCode、username、tenantCode 等），**以便** 在业务逻辑中使用用户上下文进行权限判断和数据过滤。

## 验收标准

1. `SessionHelper.cacheUserInfo(request, userJwt, userSession)` 将用户信息存入请求属性和 UserContext
2. `SessionHelper.getUserCode()` 从当前请求获取 userCode
3. `SessionHelper.getTenantCode()` 获取租户编码
4. `SessionHelper.getUserJwt()` 从当前请求获取 UserJwt
5. `SessionHelper.getUserSession()` 从当前请求获取 UserSession
6. `SessionHelper.getToken(request)` 从请求头获取 Token（支持 Authorization 和 token 两种方式）
7. `SessionHelper.getAppCode(request)` 从请求头 `app-code` 获取应用编码
8. `SessionHelper.match(rule, uri)` Ant 风格路径匹配

## 技术实现要点

- 数据流向：IamAuthFilter 调用 `cacheUserInfo()` → 存入 `request.setAttribute` + `UserContext.setUserInfo()` → 业务代码通过 `getUserJwt()` / `getUserSession()` 读取
- Token 获取优先从 `Authorization` 头（去掉 `Bearer ` 前缀），其次从 `token` 头
- `getTenantCode()` 当前硬编码返回 `"default"`，待后续多租户实现
- `match()` 方法使用 Ant 风格路径匹配，用于公开路径判断

## 依赖故事

- STORY-005（JWT 令牌工具）

## 涉及文件

| 文件 | 路径 |
|------|------|
| SessionHelper | iam-sdk/src/main/java/com/wkclz/iam/sdk/helper/SessionHelper.java |
| UserSession | iam-sdk/src/main/java/com/wkclz/iam/sdk/model/UserSession.java |
