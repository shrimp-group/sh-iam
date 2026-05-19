# STORY-030 — API 路由 CRUD 管理

| 属性 | 值 |
|------|-----|
| Story ID | STORY-030 |
| 所属 Epic | 管理后台 - API 管理 |
| 所属模块 | iam-admin |
| 优先级 | P0 |
| 状态 | 待开发 |

## 用户故事

**作为** 系统管理员，**我希望** 在管理后台对 API 路由进行增删改查操作，**以便** 管理系统的 API 权限配置。

## 验收标准

1. `GET /iam-admin/api/page` — API 分页查询
2. `GET /iam-admin/api/info` — 按 ID 查 API 详情
3. `POST /iam-admin/api/create` — 手动创建 API
4. `POST /iam-admin/api/update` — 更新 API
5. `POST /iam-admin/api/remove` — 删除 API
6. `GET /iam-admin/api/options` — 获取 API 选项列表（下拉选择用）
7. `GET /iam-admin/api/copy` — 获取可复制的 API 列表（返回 JSON）
8. `POST /iam-admin/api/paste` — 批量粘贴 API（从 JSON 导入）
9. 参数校验：apiUri、apiMethod、appCode 必填；更新时 version 必填

## 技术实现要点

- apiMethod + apiUri 唯一确定一个后端接口
- writeFlag 是关键鉴权字段：1=白名单免鉴权
- module 标识 API 所属模块（如 iam-sso、iam-admin）
- apiCode 由 RedisIdGenerator 生成（前缀 `api_`）
- copy/paste 功能支持跨应用复制 API 配置

## 依赖故事

- STORY-001（IamApi 实体定义）

## 涉及文件

| 文件 | 路径 |
|------|------|
| ApiRest | iam-admin/src/main/java/com/wkclz/iam/admin/rest/ApiRest.java |
| IamApiService | iam-admin/src/main/java/com/wkclz/iam/admin/service/IamApiService.java |
| IamApiMapper | iam-admin/src/main/java/com/wkclz/iam/admin/mapper/IamApiMapper.java |
