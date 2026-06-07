# STORY-035 — 角色-用户与用户-角色关联管理

| 属性       | 值             |
|----------|---------------|
| Story ID | STORY-035     |
| 所属 Epic  | 管理后台 - 关联关系管理 |
| 所属模块     | iam-admin     |
| 优先级      | P0            |
| 状态       | 已完成           |

## 用户故事

**作为** 系统管理员，**我希望** 管理角色与用户的关联关系（含有效期），**以便** 为用户分配角色或为角色分配用户，并控制绑定的生效时间范围。

## 验收标准

1. **用户-角色视角**（UserRoleRest）：
    - `GET /iam-admin/user-role/list` — 按 userCode 查询用户关联的角色列表（含有效期、角色名称、enableStatus）
    - `POST /iam-admin/user-role/bind` — 绑定用户与角色（含有效期 startTime/endTime，自动计算 enableStatus）
   - `POST /iam-admin/user-role/unbind` — 解绑用户与角色（按 ID 删除）
    - `GET /iam-admin/user-role/role-tree` — 查询用户在某应用下的角色树（含绑定数量标记 bindCount）
    - `GET /iam-admin/user-role/menu-source` — 查询用户菜单来源角色信息（含有效期）
2. **角色-用户视角**（RoleUserRest）：
    - `GET /iam-admin/role-user/page` — 按 roleCode 分页查询角色下的用户列表（含有效期、enableStatus、createTime），支持
      username 精确匹配、nickname 模糊搜索，分页参数 current/size
    - `POST /iam-admin/role-user/bind` — 批量绑定用户到角色（含有效期）
    - `POST /iam-admin/role-user/unbind` — 批量解绑（按 ID 列表删除）
3. **菜单视角**（MenuRest）：
    - `GET /iam-admin/menu/bound-roles` — 查询菜单绑定的角色列表（只读）
    - `GET /iam-admin/menu/bound-users` — 查询菜单关联的用户列表（含来源角色、有效期，只读）
4. **有效期管理**：
    - 绑定时根据 startTime/endTime 自动计算 enableStatus（当前时间在有效期内则启用，否则禁用）
    - 同一 userCode+roleCode 可重复绑定（不同有效期）
    - 定时任务 `refreshEnableStatus()` 扫描并批量更新过期/生效绑定状态
5. **定时任务**（UserRoleExpireJobHandler）：
    - XXL-Job 入口：`@XxlJob("userRoleExpireJob")`
    - Spring @Scheduled 入口：受 `iam.job.user-role-expire.enabled` 开关控制（默认关闭），使用 RedisLock 分布式锁保证集群单实例执行
6. 两个 REST 控制器操作同一张表 `iam_user_role`，但视角不同

## 技术实现要点

- 用户-角色和角色-用户是同一张表的两个视角
- UserRoleRest 以用户为主体查角色
- RoleUserRest 以角色为主体查用户
- MenuRest 扩展菜单视角查询（绑定角色、关联用户），只读
- IamUserMenuService 扩展菜单来源查询（getUserMenuSourceList、getMenuBoundRoles、getMenuBoundUsers）
- 绑定操作自动从 IamRoleService 查询角色的 tenantCode 和 appCode
- 批量绑定（batchBind）一次查询角色信息，批量构建实体后 insertBatch
- 角色树构建（buildRoleTree）遵循 IamMenuService.buildMenuTree 相同模式，parentCode="0" 为顶级
- computeEnableStatus 根据当前时间与有效期计算绑定状态（1=启用/0=禁用）
- refreshEnableStatus 调用 mapper 的 enableExpiredBindings/disableExpiredBindings 批量更新
- 定时任务双触发入口：XXL-Job（@XxlJob）和 Spring @Scheduled（@Value 开关 + RedisLock 分布式锁）

## 依赖故事

- STORY-025（用户 CRUD）
- STORY-027（角色 CRUD）

## 涉及文件

| 文件                       | 路径                                                                                                    |
|--------------------------|-------------------------------------------------------------------------------------------------------|
| UserRoleRest             | iam-admin/src/main/java/com/wkclz/iam/admin/rest/UserRoleRest.java                                    |
| RoleUserRest             | iam-admin/src/main/java/com/wkclz/iam/admin/rest/RoleUserRest.java                                    |
| MenuRest                 | iam-admin/src/main/java/com/wkclz/iam/admin/rest/MenuRest.java                                        |
| IamUserRoleService       | iam-admin/src/main/java/com/wkclz/iam/admin/service/IamUserRoleService.java                           |
| IamUserMenuService       | iam-admin/src/main/java/com/wkclz/iam/admin/service/IamUserMenuService.java                           |
| IamUserRoleMapper        | iam-admin/src/main/java/com/wkclz/iam/admin/mapper/IamUserRoleMapper.java                             |
| IamUserRoleMapper.xml    | iam-admin/src/main/resources/mapper/IamUserRoleMapper.xml                                             |
| UserRoleExpireJobHandler | iam-admin/src/main/java/com/wkclz/iam/admin/job/UserRoleExpireJobHandler.java                         |
| UserRoleBindReq          | iam-admin/src/main/java/com/wkclz/iam/admin/bean/req/UserRoleBindReq.java                             |
| RoleUserBindReq          | iam-admin/src/main/java/com/wkclz/iam/admin/bean/req/RoleUserBindReq.java                             |
| RoleUserUnbindReq        | iam-admin/src/main/java/com/wkclz/iam/admin/bean/req/RoleUserUnbindReq.java                           |
| UserRoleResp             | iam-admin/src/main/java/com/wkclz/iam/admin/bean/resp/UserRoleResp.java                               |
| RoleUserResp             | iam-admin/src/main/java/com/wkclz/iam/admin/bean/resp/RoleUserResp.java                               |
| MenuRoleResp             | iam-admin/src/main/java/com/wkclz/iam/admin/bean/resp/MenuRoleResp.java                               |
| MenuUserResp             | iam-admin/src/main/java/com/wkclz/iam/admin/bean/resp/MenuUserResp.java                               |
| UserMenuSourceResp       | iam-admin/src/main/java/com/wkclz/iam/admin/bean/resp/UserMenuSourceResp.java                         |
| IamUserRoleDto           | iam-common/src/main/java/com/wkclz/iam/common/dto/IamUserRoleDto.java                                 |
| role-user.vue            | iam-admin-ui/src/views/user/role/components/role-user.vue                                             |
| role/index.vue           | iam-admin-ui/src/views/user/role/index.vue (新增"用户"按钮及 role-user 组件引用)                                 |
| role-user.js             | iam-admin-ui/src/api/user/role-user.js                                                                |
| user-menu-source.vue     | iam-admin-ui/src/views/user/user/components/user-menu-source.vue (树形表格展示菜单层级, 来源角色用 popover 展示有效时间范围) |
