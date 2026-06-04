# STORY-034 — 角色-菜单关联管理

| 属性       | 值             |
|----------|---------------|
| Story ID | STORY-034     |
| 所属 Epic  | 管理后台 - 关联关系管理 |
| 所属模块     | iam-admin     |
| 优先级      | P0            |
| 状态       | 已完成           |

## 用户故事

**作为** 系统管理员，**我希望** 管理角色与菜单的关联关系，**以便** 为角色分配菜单和按钮权限。

## 验收标准

1. `GET /iam-admin/role-menu/list` — 按 roleCode 查询角色已绑定的菜单编码列表（roleCode 必填，返回 `List<String>`
   menuCodes）
2. `GET /iam-admin/role-menu/bound-list` — 按 roleCode 查询角色已绑定的菜单详情列表（含 menuName, menuType 等）
3. `GET /iam-admin/role-menu/bound-roles` — 按 menuCode 查询菜单被哪些角色绑定
4. `POST /iam-admin/role-menu/save` — 批量保存角色-菜单绑定（roleCode + menuCodes[] 必填，全量覆盖；appCode 从角色信息自动获取）
5. ~~`POST /iam-admin/role-menu/bind`~~ — 已移除，由 save 接口替代
6. ~~`POST /iam-admin/role-menu/unbind`~~ — 已移除，由 save 接口替代
7. IamRole 增加 `applicable` 字段（1=可申请/0=仅树节点），当为否时角色仅为树节点角色
8. IamRoleDto 增加 `userBindCount` 字段，角色列表展示已绑定用户数
9. 角色管理前端页面支持层级导航、编辑（含 applicable）、角色-菜单绑定弹窗
10. 菜单详情页展示已绑定角色列表

## 技术实现要点

- 角色-菜单为多对多关系，通过 iam_role_menu 中间表关联
- 角色通过菜单关联获得页面和按钮权限
- 菜单通过 IamMenuApi 关联后端 API，实现前后端权限统一
- IamRole.applicable 字段区分"可申请角色"和"仅树节点角色"，applicable=0 时不可用于权限分配
- IamRoleMenuService.duplicateCheck 校验 roleCode + menuCode 唯一性，防止重复绑定
- 角色列表 SQL 通过 LEFT JOIN iam_user_role 统计 userBindCount
- 角色-菜单绑定弹窗采用 el-tree 勾选模式（menuTree API 获取菜单树，roleMenuBoundList 获取已绑定菜单，roleMenuSave 批量保存）
- 菜单详情页通过 roleMenuBoundRoles 接口查询已绑定角色

## 依赖故事

- STORY-027（角色 CRUD）
- STORY-028（菜单 CRUD）

## 涉及文件

| 文件                            | 路径                                                                          |
|-------------------------------|-----------------------------------------------------------------------------|
| IamRole                       | iam-common/src/main/java/com/wkclz/iam/common/entity/IamRole.java           |
| IamRoleDto                    | iam-common/src/main/java/com/wkclz/iam/common/dto/IamRoleDto.java           |
| RoleMenuRest                  | iam-admin/src/main/java/com/wkclz/iam/admin/rest/RoleMenuRest.java          |
| RoleMenuSaveReq               | iam-common/src/main/java/com/wkclz/iam/common/bean/req/RoleMenuSaveReq.java |
| IamRoleMenuService            | iam-admin/src/main/java/com/wkclz/iam/admin/service/IamRoleMenuService.java |
| IamRoleMenuMapper             | iam-admin/src/main/java/com/wkclz/iam/admin/mapper/IamRoleMenuMapper.java   |
| IamRoleMenuMapper.xml         | iam-admin/src/main/resources/mapper/IamRoleMenuMapper.xml                   |
| IamRoleMapper.xml             | iam-admin/src/main/resources/mapper/IamRoleMapper.xml                       |
| MenuBoundResp                 | iam-admin/src/main/java/com/wkclz/iam/admin/bean/resp/MenuBoundResp.java    |
| RoleBoundResp                 | iam-admin/src/main/java/com/wkclz/iam/admin/bean/resp/RoleBoundResp.java    |
| Route                         | iam-admin/src/main/java/com/wkclz/iam/admin/Route.java                      |
| role-menu.js                  | iam-admin-ui/src/api/system/role-menu.js                                    |
| role/index.vue                | iam-admin-ui/src/views/system/role/index.vue                                |
| role/components/edit.vue      | iam-admin-ui/src/views/system/role/components/edit.vue                      |
| role/components/menu-bind.vue | iam-admin-ui/src/views/system/role/components/menu-bind.vue                 |
| menu/components/detail.vue    | iam-admin-ui/src/views/system/menu/components/detail.vue                    |
