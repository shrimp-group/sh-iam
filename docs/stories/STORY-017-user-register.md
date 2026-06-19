# STORY-017 — 用户注册接口

| 属性 | 值 |
|------|-----|
| Story ID | STORY-017 |
| 所属 Epic | SSO 登录认证模块 |
| 所属模块 | iam-sso |
| 优先级 | P2 |
| 状态 | 待开发 |

## 用户故事

**作为** 新用户，**我希望** 通过注册接口创建账号，**以便** 能够登录并使用系统功能。

## 验收标准

1. API 端点：`POST /iam-sso/public/sso/register`
2. 接收 `RegisterReq` 请求体
3. 当前为占位实现，直接返回 `R.ok()`
4. 预留注册验证接口：`POST /iam-sso/public/sso/register/verify`
5. 路由常量已在 Route 接口中定义

## 技术实现要点

- 当前仅为空实现，属于预留接口
- 待后续实现完整的注册流程（用户名唯一性校验、密码加密、认证方式创建等）
- 可参考 `IamUserService.customCreate()` 的用户创建流程

## 依赖故事

- STORY-001（IamUser 实体定义）
- STORY-003（密码加密校验工具）

## 涉及文件

| 文件           | 路径                                                             |
|--------------|----------------------------------------------------------------|
| RegisterRest | iam-sso/src/main/java/com/wkclz/iam/sso/rest/RegisterRest.java |
| RegisterReq  | iam-sdk/src/main/java/com/wkclz/iam/sdk/model/RegisterReq.java |
