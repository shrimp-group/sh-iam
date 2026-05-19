# STORY-028 — 菜单 CRUD 与树形管理

| 属性 | 值 |
|------|-----|
| Story ID | STORY-028 |
| 所属 Epic | 管理后台 - 菜单管理 |
| 所属模块 | iam-admin |
| 优先级 | P0 |
| 状态 | 待开发 |

## 用户故事

**作为** 系统管理员，**我希望** 在管理后台对菜单进行增删改查操作，并支持树形结构展示，**以便** 管理系统的菜单和按钮权限。

## 验收标准

1. `GET /iam-admin/menu/list` — 查询菜单列表（appCode 必填）
2. `GET /iam-admin/menu/tree` — 查询菜单树形结构（appCode 必填）
3. `GET /iam-admin/menu/info` — 按 ID 查菜单详情
4. `POST /iam-admin/menu/create` — 创建菜单
5. `POST /iam-admin/menu/update` — 更新菜单
6. `POST /iam-admin/menu/remove` — 删除菜单
7. 支持两种菜单类型：MENU（菜单）和 BUTTON（按钮）
8. 一级路由（parentCode="0"）的 routePath 必须以 `/` 开头
9. 参数校验：appCode、menuName、menuType 必填；更新时 version 必填

## 技术实现要点

- 菜单支持两级类型：MENU（页面级）和 BUTTON（按钮级）
- BUTTON 类型通过 buttonCode 实现细粒度权限控制
- parentCode 支持菜单树形结构，与前端路由嵌套对应
- routePath + component 定义前端路由映射
- hidden 控制菜单是否在前端导航中显示
- 一级路由 routePath 校验：必须以 `/` 开头，否则抛 ValidationException

## 依赖故事

- STORY-001（IamMenu 实体定义）

## 涉及文件

| 文件 | 路径 |
|------|------|
| MenuRest | iam-admin/src/main/java/com/wkclz/iam/admin/rest/MenuRest.java |
| IamMenuService | iam-admin/src/main/java/com/wkclz/iam/admin/service/IamMenuService.java |
| IamMenuMapper | iam-admin/src/main/java/com/wkclz/iam/admin/mapper/IamMenuMapper.java |
