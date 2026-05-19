# STORY-019 — 用户信息与菜单资源查询

| 属性 | 值 |
|------|-----|
| Story ID | STORY-019 |
| 所属 Epic | SSO 登录认证模块 |
| 所属模块 | iam-sso |
| 优先级 | P0 |
| 状态 | 待开发 |

## 用户故事

**作为** 已登录用户，**我希望** 查询自己的用户信息和菜单权限，**以便** 前端能够渲染导航菜单和展示用户信息。

## 验收标准

1. API 端点：`GET /iam-sso/user/info` — 获取当前用户 Session 信息
2. API 端点：`GET /iam-sso/user/menu/tree` — 获取用户菜单树（标准格式）
3. 用户信息从 `SessionHelper` 获取当前请求的 `UserSession`
4. 菜单树从 Header 获取 `appCode`，调用 `SsoResourceService.getUserMenuTree()`
5. 菜单树构建：parentCode="0" 的作为根节点，其余根据 parentCode 找到父节点
6. 返回 `List<IamMenuDto>` 树形结构

## 技术实现要点

- 用户信息：从 `SessionHelper.getUserSession()` 获取
- 菜单查询：`SsoResourceMapper.getUserMenu(appCode)` 查询数据库
- 树形构建：`makeTree()` 方法，O(n²) 双层循环，适用于菜单数量不大的场景
- appCode 从请求头 `app-code` 获取，确定查询哪个应用的菜单

## 依赖故事

- STORY-006（用户会话上下文）
- STORY-001（IamMenu 实体定义）

## 涉及文件

| 文件 | 路径 |
|------|------|
| UserInfoRest | iam-sso/src/main/java/com/wkclz/iam/sso/rest/UserInfoRest.java |
| SsoResourceService | iam-sso/src/main/java/com/wkclz/iam/sso/service/SsoResourceService.java |
| SsoResourceMapper | iam-sso/src/main/java/com/wkclz/iam/sso/mapper/SsoResourceMapper.java |
