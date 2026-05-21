# STORY-027 — 角色 CRUD 管理

| 属性 | 值 |
|------|-----|
| Story ID | STORY-027 |
| 所属 Epic | 管理后台 - 角色管理 |
| 所属模块 | iam-admin |
| 优先级 | P0 |
| 状态 | 待开发 |

## 用户故事

**作为** 系统管理员，**我希望** 在管理后台对角色进行增删改查操作，**以便** 管理系统中的角色定义。

## 验收标准

1. `GET /iam-admin/role/list` — 按条件查询角色列表（appCode 必填）
2. `GET /iam-admin/role/info` — 按 ID 查角色详情
3. `POST /iam-admin/role/create` — 创建角色
4. `POST /iam-admin/role/update` — 更新角色
5. `POST /iam-admin/role/remove` — 删除角色
6. 删除前检查是否有子角色（parentCode = 当前 roleCode），有则拒绝删除
7. 参数校验：appCode、roleName 必填；更新时 version 必填

## 技术实现要点

- 角色通过 appCode 与应用绑定，实现应用级别的角色隔离
- parentCode 支持角色继承树，顶级角色 parentCode 为空或 "0"
- tenantCode 提供多租户隔离能力
- 删除保护：检查子角色存在性

## 依赖故事

- STORY-001（IamRole 实体定义）

## 涉及文件

| 文件 | 路径 |
|------|------|
| RoleRest | iam-admin/src/main/java/com/wkclz/iam/admin/rest/RoleRest.java |
| IamRoleService | iam-admin/src/main/java/com/wkclz/iam/admin/service/IamRoleService.java |
| IamRoleMapper | iam-admin/src/main/java/com/wkclz/iam/admin/mapper/IamRoleMapper.java |
