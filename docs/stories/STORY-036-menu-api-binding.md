# STORY-036 — 菜单-API 关联管理

| 属性 | 值 |
|------|-----|
| Story ID | STORY-036 |
| 所属 Epic | 管理后台 - 关联关系管理 |
| 所属模块 | iam-admin |
| 优先级 | P0 |
| 状态 | 待开发 |

## 用户故事

**作为** 系统管理员，**我希望** 管理菜单与 API 的关联关系，**以便** 将前端菜单权限与后端 API 权限统一管理。

## 验收标准

1. `GET /iam-admin/menu-api/list` — 按 menuCode 查询菜单关联的 API 列表（menuCode 必填）
2. `POST /iam-admin/menu-api/bind` — 绑定菜单与 API（appCode、menuCode、apiCode 必填）
3. `POST /iam-admin/menu-api/unbind` — 解绑菜单与 API（按 ID 删除）
4. 绑定时校验 appCode、menuCode、apiCode 非空

## 技术实现要点

- 菜单-API 为多对多关系，通过 iam_menu_api 中间表关联
- 前端菜单权限（MENU/BUTTON）通过此关联映射到后端 API 权限
- 用户拥有某菜单权限 → 角色关联菜单 → 菜单关联 API → 用户可访问对应 API
- appCode 冗余存储，便于按应用维度查询

## 依赖故事

- STORY-028（菜单 CRUD）
- STORY-030（API CRUD）

## 涉及文件

| 文件 | 路径 |
|------|------|
| MenuApiRest | iam-admin/src/main/java/com/wkclz/iam/admin/rest/MenuApiRest.java |
| IamMenuApiService | iam-admin/src/main/java/com/wkclz/iam/admin/service/IamMenuApiService.java |
| IamMenuApiMapper | iam-admin/src/main/java/com/wkclz/iam/admin/mapper/IamMenuApiMapper.java |
