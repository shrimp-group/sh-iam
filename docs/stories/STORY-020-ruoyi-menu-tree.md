# STORY-020 — 若依格式菜单树适配

| 属性 | 值 |
|------|-----|
| Story ID | STORY-020 |
| 所属 Epic | SSO 登录认证模块 |
| 所属模块 | iam-sso |
| 优先级 | P1 |
| 状态 | 待开发 |

## 用户故事

**作为** 使用若依前端框架的应用，**我希望** 获取符合若依 Vue Router 格式的菜单树，**以便** 前端路由能够正确渲染。

## 验收标准

1. API 端点：`GET /iam-sso/user/menu/tree/ruoyi`
2. 先获取标准菜单树，再转换为若依 Vue Router 格式
3. 只保留 `menuType=MENU` 的菜单项
4. 递归构建 `path/name/hidden/redirect/component/alwaysShow/children/meta` 结构
5. `path2Name()` 将路由路径转为 PascalCase 命名，处理重名（追加 `_序号`）
6. 从子菜单中提取 `menuType=BUTTON` 的 `buttonCode` 列表，按路由路径映射到 `buttonsMap`
7. meta 包含 title、icon、noCache、link 等字段

## 技术实现要点

- 转换逻辑在 `UserInfoRest` 的私有方法 `menuTreeVueRouterTree()` 中
- 只保留 MENU 类型，BUTTON 类型提取为权限标识
- path2Name：将 `/system/user` 转为 `SystemUser`，重名追加 `_序号`
- buttonsMap：key 为路由路径，value 为该菜单下所有 BUTTON 的 buttonCode 列表
- VueRouterMenu / VueRouterMeta 为 SSO 模块内部实体

## 依赖故事

- STORY-019（用户菜单树查询）

## 涉及文件

| 文件 | 路径 |
|------|------|
| UserInfoRest | iam-sso/src/main/java/com/wkclz/iam/sso/rest/UserInfoRest.java |
| VueRouterMenu | iam-sso/src/main/java/com/wkclz/iam/sso/entity/VueRouterMenu.java |
| VueRouterMeta | iam-sso/src/main/java/com/wkclz/iam/sso/entity/VueRouterMeta.java |
