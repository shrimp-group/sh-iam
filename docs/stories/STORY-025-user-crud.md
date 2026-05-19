# STORY-025 — 用户 CRUD 管理

| 属性 | 值 |
|------|-----|
| Story ID | STORY-025 |
| 所属 Epic | 管理后台 - 用户管理 |
| 所属模块 | iam-admin |
| 优先级 | P0 |
| 状态 | 待开发 |

## 用户故事

**作为** 系统管理员，**我希望** 在管理后台对用户进行增删改查操作，**以便** 管理系统中的用户账号。

## 验收标准

1. `GET /iam-admin/user/page` — 用户分页查询
2. `GET /iam-admin/user/info` — 按 ID 查用户详情
3. `POST /iam-admin/user/create` — 创建用户（调用 customCreate 完整流程）
4. `POST /iam-admin/user/update` — 更新用户信息
5. `POST /iam-admin/user/remove` — 逻辑删除用户
6. 创建用户完整流程（customCreate）：
   - 用户名唯一性校验
   - RedisIdGenerator 生成 userCode（前缀 `user_`）
   - iam_user 入库
   - iam_user_auth 入库（authType=PASSWORD）
   - iam_user_auth_password 入库（salt + MD5）
   - iam_user_password_his 入库
7. 创建后返回结果中清空密码字段
8. 参数校验：用户名、昵称必填；创建时密码必填；更新时 version 必填

## 技术实现要点

- Service 继承 `BaseService<IamUser, IamUserMapper>`
- ID 生成：`RedisIdGenerator.generateIdWithPrefix("user_")`
- 密码加密：`PasswordHelper.generatePasswordWithSalt()`
- 逻辑删除：通过 BaseEntity 的 deleted 字段
- 乐观锁：通过 version 字段
- 属性拷贝：`copyIfNotNull()` 避免覆盖已有值
- 参数校验：`Assert.notNull()` + 自定义异常

## 依赖故事

- STORY-001（IamUser、IamUserDto 实体定义）
- STORY-003（密码加密校验工具）

## 涉及文件

| 文件 | 路径 |
|------|------|
| UserRest | iam-admin/src/main/java/com/wkclz/iam/admin/rest/UserRest.java |
| IamUserService | iam-admin/src/main/java/com/wkclz/iam/admin/service/IamUserService.java |
| IamUserMapper | iam-admin/src/main/java/com/wkclz/iam/admin/mapper/IamUserMapper.java |
| IamUserMapper.xml | iam-admin/src/main/resources/mapper/IamUserMapper.xml |
