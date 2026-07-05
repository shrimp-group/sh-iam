# STORY-029 — 应用 CRUD 管理

| 属性       | 值           |
|----------|-------------|
| Story ID | STORY-029   |
| 所属 Epic  | 管理后台 - 应用管理 |
| 所属模块     | iam-admin   |
| 优先级      | P0          |
| 状态       | 待开发         |

## 用户故事

**作为** 系统管理员，**我希望** 在管理后台对应用进行增删改查操作，**以便** 管理接入 IAM 的各个应用。

## 验收标准

1. `GET /iam-admin/app/page` — 应用分页查询
2. `GET /iam-admin/app/info` — 按 ID 查应用详情
3. `POST /iam-admin/app/create` — 创建应用
4. `POST /iam-admin/app/update` — 更新应用
5. `POST /iam-admin/app/remove` — 删除应用
6. `GET /iam-admin/app/options` — 获取应用选项列表（下拉选择用）
7. 参数校验：appCode、appName、domain、authType 必填；更新时 version 必填

## 技术实现要点

- appCode 是整个 IAM 系统的核心维度，角色、菜单、API、AK、数据维度均通过 appCode 归属到应用
- domain 用于 SSO 跨域登录时的回调地址校验
- authType 定义该应用的鉴权方式
- loginBgp 和 appIcon 支持每个应用定制登录页外观
- options 接口无参数，返回所有应用的精简列表供下拉选择

## 依赖故事

- STORY-001（IamApp 实体定义）

## 涉及文件

| 文件            | 路径                                                                     |
|---------------|------------------------------------------------------------------------|
| AppRest       | iam-admin/src/main/java/com/wkclz/iam/admin/rest/AppRest.java          |
| IamAppService | iam-admin/src/main/java/com/wkclz/iam/admin/service/IamAppService.java |
| IamAppMapper  | iam-admin/src/main/java/com/wkclz/iam/admin/mapper/IamAppMapper.java   |
