# STORY-033 — AK-API 关联管理

| 属性 | 值 |
|------|-----|
| Story ID | STORY-033 |
| 所属 Epic | 管理后台 - 访问密钥管理 |
| 所属模块 | iam-admin |
| 优先级 | P1 |
| 状态 | 待开发 |

## 用户故事

**作为** 系统管理员，**我希望** 管理访问密钥与 API 的关联关系，**以便** 控制每个 AK 可以访问哪些 API 接口。

## 验收标准

1. `GET /iam-admin/access-key-api/list` — 按 appId 查询 AK 关联的 API 列表（appId 必填）
2. `POST /iam-admin/access-key-api/bind` — 绑定 AK 与 API（appCode、appId、apiCode 必填）
3. `POST /iam-admin/access-key-api/unbind` — 解绑 AK 与 API（按 ID 删除）
4. 绑定时校验 appCode、appId、apiCode 非空

## 技术实现要点

- 纯关联表操作，建立 AK 密钥与 API 路由的多对多关系
- appId + apiCode 联合确定某个 AK 可以访问哪些 API
- appCode 冗余存储，便于按应用维度查询
- 在 AK 签名鉴权流程中，用于校验请求的 API 是否在 AK 授权范围内

## 依赖故事

- STORY-032（访问密钥 CRUD）
- STORY-030（API CRUD 管理）

## 涉及文件

| 文件 | 路径 |
|------|------|
| AccessKeyApiRest | iam-admin/src/main/java/com/wkclz/iam/admin/rest/AccessKeyApiRest.java |
| IamAccessKeyApiService | iam-admin/src/main/java/com/wkclz/iam/admin/service/IamAccessKeyApiService.java |
| IamAccessKeyApiMapper | iam-admin/src/main/java/com/wkclz/iam/admin/mapper/IamAccessKeyApiMapper.java |
