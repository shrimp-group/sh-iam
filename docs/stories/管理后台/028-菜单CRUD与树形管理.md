# STORY-028 — 菜单 CRUD 与树形管理

| 属性       | 值              |
|----------|----------------|
| Story ID | STORY-028      |
| 所属 Epic  | 管理后台 - 菜单管理    |
| 所属模块     | iam-admin      |
| 优先级      | P0             |
| 状态       | 已完成（前端树形视图已实现） |

## 用户故事

**作为** 系统管理员，**我希望** 在管理后台对菜单进行增删改查操作，并支持树形结构展示，**以便** 管理系统的菜单和按钮权限。

## 验收标准

1. `GET /iam-admin/menu/list` — 查询菜单列表（appCode 必填）
2. `GET /iam-admin/menu/tree` — 查询菜单树形结构（appCode 必填），节点返回 level 字段（根节点=1，子级逐级+1）
3. `GET /iam-admin/menu/info` — 按 ID 查菜单详情
4. `POST /iam-admin/menu/create` — 创建菜单
5. `POST /iam-admin/menu/update` — 更新菜单
6. `POST /iam-admin/menu/remove` — 删除菜单
7. 支持三种菜单类型：MENU（菜单）、BUTTON（按钮）和 FIELDS（字段组）
8. 一级路由（parentCode="0"）的 routePath 必须以 `/` 开头
9. FIELDS 类型与 BUTTON 同级，parentCode 必须指向 MENU 类型
10. FIELDS 类型不需要 routePath 和 component 字段，buttonCode 可选填（作为权限标识符）
11. 删除 FIELDS 类型菜单时级联清理 iam_menu_field 绑定记录
12. 参数校验：appCode、menuName、menuType 必填；更新时 version 必填

## 技术实现要点

- 菜单支持三种类型：MENU（页面级）、BUTTON（按钮级）和 FIELDS（字段组级）
- BUTTON 类型通过 buttonCode 实现细粒度权限控制
- FIELDS 类型与 BUTTON 同级，parentCode 必须指向 MENU 类型，用于字段级权限控制
- FIELDS 类型不需要 routePath 和 component，buttonCode 可选填
- 删除 FIELDS 类型菜单时级联清理 iam_menu_field 绑定记录
- parentCode 支持菜单树形结构，与前端路由嵌套对应
- routePath + component 定义前端路由映射
- hidden 控制菜单是否在前端导航中显示
- 一级路由 routePath 校验：必须以 `/` 开头，否则抛 ValidationException

## 依赖故事

- STORY-001（IamMenu 实体定义）

## 涉及文件

| 文件                  | 路径                                                                           |
|---------------------|------------------------------------------------------------------------------|
| MenuRest            | iam-admin/src/main/java/com/wkclz/iam/admin/rest/MenuRest.java               |
| IamMenuService      | iam-admin/src/main/java/com/wkclz/iam/admin/service/IamMenuService.java      |
| IamMenuFieldService | iam-admin/src/main/java/com/wkclz/iam/admin/service/IamMenuFieldService.java |
| IamMenuMapper       | iam-admin/src/main/java/com/wkclz/iam/admin/mapper/IamMenuMapper.java        |
| 菜单管理前端页面            | iam-admin-ui/src/views/system/menu/index.vue                                 |

## 前端树形视图实现说明

菜单管理页面 (`index.vue`) 采用树形视图+列表视图双 Tab 设计：

- **树形视图**（默认）：调用 `menuTree` API 获取树形数据，el-table 树形模式展示完整菜单层级
    - 层级缩进：【路由地址】列按 `(level-1)*2ch` 缩进展示层级（level 来自 tree 接口）
    - 工具栏：搜索框（前端实时过滤，匹配节点及祖先保留，300ms 防抖）+ 一键展开/折叠
    - 自定义节点模板：图标 + 菜单名称 + 类型标签 + 操作按钮（详情/新增/编辑/删除）
    - 祖先路径构建：遍历树数据回溯父级路径，为编辑弹窗提供 parents 参数
- **列表视图**：面包屑导航 + 扁平列表逐层浏览，首次切到列表 Tab 才懒加载数据，切换应用时自动刷新
- 渲染性能优化：
    - `menuTreeData` 使用 `shallowRef`，避免大树深层响应式代理开销
    - 搜索关键词 300ms 防抖，过滤函数复用未变更节点对象
    - 一键展开/折叠改为重挂载表格（`:key` 递增）+ `:default-expand-all` 单次渲染，替代逐节点 `toggleRowExpansion`

> 变更记录：2026-10-31 树形视图默认化 + level 层级缩进 + 渲染性能优化。
