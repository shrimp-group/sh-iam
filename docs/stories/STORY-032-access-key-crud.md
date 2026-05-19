# STORY-032 — 访问密钥 CRUD 管理

| 属性 | 值 |
|------|-----|
| Story ID | STORY-032 |
| 所属 Epic | 管理后台 - 访问密钥管理 |
| 所属模块 | iam-admin |
| 优先级 | P1 |
| 状态 | 待开发 |

## 用户故事

**作为** 系统管理员，**我希望** 在管理后台对访问密钥（AK/SK）进行增删改查操作，**以便** 管理服务间调用的身份凭证。

## 验收标准

1. `GET /iam-admin/access-key/page` — AK 分页查询
2. `GET /iam-admin/access-key/info` — 按 ID 查 AK 详情
3. `POST /iam-admin/access-key/create` — 创建 AK
4. `POST /iam-admin/access-key/update` — 更新 AK
5. `POST /iam-admin/access-key/remove` — 删除 AK
6. 参数校验：appCode 必填；更新时 version 必填
7. AK 包含 enableStatus + enableStart/enableStop 时间窗口控制

## 技术实现要点

- AK/SK 机制用于服务间调用鉴权
- accessKey 为公钥（标识身份），secretKey 为私钥（用于签名）
- enableStatus + enableStart/enableStop 实现时间窗口内的密钥生效控制
- AK 通过 IamAccessKeyApi 关联可访问的 API 列表

## 依赖故事

- STORY-001（IamAccessKey 实体定义）

## 涉及文件

| 文件 | 路径 |
|------|------|
| AccessKeyRest | iam-admin/src/main/java/com/wkclz/iam/admin/rest/AccessKeyRest.java |
| IamAccessKeyService | iam-admin/src/main/java/com/wkclz/iam/admin/service/IamAccessKeyService.java |
| IamAccessKeyMapper | iam-admin/src/main/java/com/wkclz/iam/admin/mapper/IamAccessKeyMapper.java |
