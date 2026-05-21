# STORY-035 — 角色-用户与用户-角色关联管理

| 属性 | 值 |
|------|-----|
| Story ID | STORY-035 |
| 所属 Epic | 管理后台 - 关联关系管理 |
| 所属模块 | iam-admin |
| 优先级 | P0 |
| 状态 | 待开发 |

## 用户故事

**作为** 系统管理员，**我希望** 管理角色与用户的关联关系，**以便** 为用户分配角色或为角色分配用户。

## 验收标准

1. **用户-角色视角**（UserRoleRest）：
   - `GET /iam-admin/user-role/list` — 按 userCode 查询用户关联的角色列表（userCode 必填）
   - `POST /iam-admin/user-role/bind` — 绑定用户与角色（userCode + roleCode 必填）
   - `POST /iam-admin/user-role/unbind` — 解绑用户与角色（按 ID 删除）
2. **角色-用户视角**（RoleUserRest）：
   - `GET /iam-admin/role-user/list` — 按 roleCode 查询角色下的用户列表（roleCode 必填）
   - `POST /iam-admin/role-user/bind` — 绑定角色与用户（roleCode + userCode 必填）
   - `POST /iam-admin/role-user/unbind` — 解绑角色与用户（按 ID 删除）
3. 两个 REST 控制器操作同一张表 `iam_user_role`，但视角不同

## 技术实现要点

- 用户-角色和角色-用户是同一张表的两个视角
- UserRoleRest 以用户为主体查角色
- RoleUserRest 以角色为主体查用户
- 绑定/解绑操作本质相同，只是参数校验侧重点不同

## 依赖故事

- STORY-025（用户 CRUD）
- STORY-027（角色 CRUD）

## 涉及文件

| 文件 | 路径 |
|------|------|
| UserRoleRest | iam-admin/src/main/java/com/wkclz/iam/admin/rest/UserRoleRest.java |
| IamUserRoleService | iam-admin/src/main/java/com/wkclz/iam/admin/service/IamUserRoleService.java |
| IamUserRoleMapper | iam-admin/src/main/java/com/wkclz/iam/admin/mapper/IamUserRoleMapper.java |
| RoleUserRest | iam-admin/src/main/java/com/wkclz/iam/admin/rest/RoleUserRest.java |
