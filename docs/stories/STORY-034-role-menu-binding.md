# STORY-034 — 角色-菜单关联管理

| 属性 | 值 |
|------|-----|
| Story ID | STORY-034 |
| 所属 Epic | 管理后台 - 关联关系管理 |
| 所属模块 | iam-admin |
| 优先级 | P0 |
| 状态 | 待开发 |

## 用户故事

**作为** 系统管理员，**我希望** 管理角色与菜单的关联关系，**以便** 为角色分配菜单和按钮权限。

## 验收标准

1. `GET /iam-admin/role-menu/list` — 按 roleCode 查询角色关联的菜单列表（roleCode 必填）
2. `POST /iam-admin/role-menu/bind` — 绑定角色与菜单（roleCode + menuCode 必填）
3. `POST /iam-admin/role-menu/unbind` — 解绑角色与菜单（按 ID 删除）
4. 绑定时校验 roleCode、menuCode 非空

## 技术实现要点

- 角色-菜单为多对多关系，通过 iam_role_menu 中间表关联
- 角色通过菜单关联获得页面和按钮权限
- 菜单通过 IamMenuApi 关联后端 API，实现前后端权限统一

## 依赖故事

- STORY-027（角色 CRUD）
- STORY-028（菜单 CRUD）

## 涉及文件

| 文件 | 路径 |
|------|------|
| RoleMenuRest | iam-admin/src/main/java/com/wkclz/iam/admin/rest/RoleMenuRest.java |
| IamRoleMenuService | iam-admin/src/main/java/com/wkclz/iam/admin/service/IamRoleMenuService.java |
| IamRoleMenuMapper | iam-admin/src/main/java/com/wkclz/iam/admin/mapper/IamRoleMenuMapper.java |
