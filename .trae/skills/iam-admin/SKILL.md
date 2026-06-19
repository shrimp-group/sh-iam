# iam-admin 模块知识库

sh-iam 管理后台业务模块，提供用户/角色/菜单/应用/API/访问密钥/数据权限/日志等完整 CRUD 管理能力，以及 API
自动扫描、用户角色过期定时任务、实体字段分析等高级功能。路由前缀 `/iam-admin`。

## 关键类

### REST 控制器 (rest)

| 类名                | 说明                                   |
|-------------------|--------------------------------------|
| UserRest          | 用户管理：分页/详情/创建/修改/删除                  |
| UserAuthRest      | 用户认证方式：列表/详情/创建/修改/删除/重置密码           |
| RoleRest          | 角色管理：列表/详情/创建/修改/删除/树                |
| MenuRest          | 菜单管理：列表/树/详情/创建/修改/删除/详情页/绑定角色/绑定用户  |
| AppRest           | 应用管理：分页/详情/创建/修改/删除/选项               |
| ApiRest           | API管理：分页/详情/创建/修改/删除/选项/同步/复制/粘贴/详情页 |
| ApiFieldRest      | API字段权限：按API查询/创建/修改/删除              |
| AccessKeyRest     | 访问密钥：分页/详情/创建/修改/删除                  |
| AccessKeyApiRest  | AK-API关联：列表/绑定/解绑                    |
| UserRoleRest      | 用户角色：列表/绑定/解绑/角色树/菜单来源               |
| RoleUserRest      | 角色用户：分页/绑定/解绑                        |
| RoleMenuRest      | 角色菜单：列表/保存/已绑定角色列表                   |
| MenuApiRest       | 菜单API：列表/绑定/解绑/已绑定列表                 |
| MenuFieldRest     | 菜单字段：列表/绑定/批量保存/解绑                   |
| DataDimensionRest | 数据权限维度：分页/详情/创建/修改/删除/选项             |
| RoleDataRest      | 角色数据：列表/绑定/解绑                        |
| UserMenuRest      | 用户菜单：列表/树                            |
| LoginLogRest      | 登录日志：分页/详情                           |
| RequestLogRest    | 请求日志：分页/详情                           |
| EntityFieldRest   | 实体字段：根据API定位实体类字段树                   |

### 请求参数类 (bean.req)

| 分组    | 类名                                                                              | 说明            |
|-------|---------------------------------------------------------------------------------|---------------|
| 用户    | UserPageReq, UserCreateReq, UserUpdateReq                                       | 用户分页/创建/更新参数  |
| 用户认证  | UserAuthListReq, UserAuthCreateReq, UserAuthUpdateReq, UserAuthResetPasswordReq | 认证方式管理参数      |
| 角色    | RoleListReq, RoleCreateReq, RoleUpdateReq                                       | 角色列表/创建/更新参数  |
| 菜单    | MenuListReq, MenuCreateReq, MenuUpdateReq                                       | 菜单列表/创建/更新参数  |
| 应用    | AppPageReq, AppCreateReq, AppUpdateReq                                          | 应用分页/创建/更新参数  |
| API   | ApiPageReq, ApiCreateReq, ApiUpdateReq, ApiListReq, ApiPasteReq                 | API管理参数       |
| API字段 | ApiFieldCreateReq, ApiFieldUpdateReq                                            | API字段权限参数     |
| 访问密钥  | AccessKeyPageReq, AccessKeyCreateReq, AccessKeyUpdateReq, AccessKeyApiBindReq   | AK管理参数        |
| 用户角色  | UserRoleBindReq, UserMenuListReq                                                | 用户角色绑定/菜单来源参数 |
| 角色用户  | RoleUserPageReq, RoleUserBindReq, RoleUserUnbindReq                             | 角色用户管理参数      |
| 角色菜单  | RoleMenuSaveReq                                                                 | 角色菜单保存参数      |
| 菜单API | MenuApiBindReq, MenuApiListReq                                                  | 菜单API绑定参数     |
| 菜单字段  | MenuFieldBindReq, MenuFieldSaveReq                                              | 菜单字段绑定参数      |
| 数据维度  | DataDimPageReq, DataDimCreateReq, DataDimUpdateReq                              | 数据维度管理参数      |
| 角色数据  | RoleDataListReq, RoleDataBindReq                                                | 角色数据权限参数      |
| 日志    | LoginLogPageReq, RequestLogPageReq                                              | 日志查询参数        |

### 响应类 (bean.resp)

| 类名                                             | 说明                                                                   |
|------------------------------------------------|----------------------------------------------------------------------|
| UserResp, RoleResp, MenuResp, AppResp, ApiResp | 基础业务响应                                                               |
| ApiDetailResp                                  | API详情（含已绑定菜单全路径）                                                     |
| ApiBoundResp                                   | API绑定信息                                                              |
| ApiFieldResp                                   | API字段权限响应                                                            |
| AccessKeyResp, AccessKeyApiResp                | AK管理响应                                                               |
| UserAuthResp                                   | 用户认证方式响应                                                             |
| UserRoleResp, RoleUserResp                     | 用户角色/角色用户响应                                                          |
| UserMenuSourceResp                             | 用户菜单来源响应（含来源角色及有效期）                                                  |
| MenuDetailResp                                 | 菜单详情响应                                                               |
| MenuApiResp, MenuRoleResp, MenuUserResp        | 菜单关联响应                                                               |
| MenuFieldResp                                  | 菜单字段响应                                                               |
| RoleBoundResp, RoleDataResp                    | 角色绑定/数据权限响应                                                          |
| DataDimResp                                    | 数据维度响应                                                               |
| LoginLogResp, RequestLogResp                   | 日志响应                                                                 |
| EntityFieldNode                                | 实体字段树节点(fieldName, fieldDesc, fieldType, jsonPath, isList, children) |

### 服务类 (service)

| 类名                                                                        | 说明                                               |
|---------------------------------------------------------------------------|--------------------------------------------------|
| IamUserService                                                            | 用户服务：分页查询、创建(customCreate含用户+认证+密码+历史4表入库)、更新、删除 |
| IamRoleService                                                            | 角色服务：列表、树形构建、创建(含roleCode生成+重复检查)、更新、删除(含子角色校验)  |
| IamMenuService                                                            | 菜单服务：列表、树、创建(含路由路径校验)、更新、删除(含子菜单校验)、详情页          |
| IamAppService                                                             | 应用服务：标准CRUD + 选项列表                               |
| IamApiService                                                             | API服务：标准CRUD + 选项 + 同步 + 复制/粘贴                   |
| IamApiFieldService                                                        | API字段权限服务：按API查询、创建、修改、删除                        |
| IamAccessKeyService, IamAccessKeyApiService                               | AK管理服务                                           |
| IamUserRoleService                                                        | 用户角色服务：列表、绑定、解绑、角色树、菜单来源、刷新有效期状态                 |
| IamRoleMenuService                                                        | 角色菜单服务：列表、保存、已绑定角色                               |
| IamMenuApiService, IamMenuFieldService                                    | 菜单关联服务                                           |
| IamUserMenuService                                                        | 用户菜单服务：列表、树、菜单绑定角色/用户查询                          |
| IamDataDimensionService, IamRoleDataService                               | 数据权限服务                                           |
| IamUserAuthService, IamUserAuthPasswordService, IamUserPasswordHisService | 用户认证服务                                           |
| IamLoginLogService, IamRequestLogService                                  | 日志查询服务                                           |
| IamTenantService                                                          | 租户服务                                             |

### Mapper 接口 (mapper)

20 个 Mapper 接口，对应 20 张表。位于 `com.wkclz.iam.admin.mapper` 包下，均继承 BaseMapper。

### 辅助类

| 包      | 类名                       | 说明                                                                              |
|--------|--------------------------|---------------------------------------------------------------------------------|
| helper | EntityFieldAnalyzer      | 实体字段分析器：反射解析实体类 @FieldDesc 注解，递归解析嵌套对象和 List 泛型，生成字段树；解析 R/PageData 泛型信息定位业务实体类 |
| init   | RestfulScan              | 启动时 API 自动扫描：对比数据库与 @Router 注解路由，自动增删改 API 记录                                   |
| job    | UserRoleExpireJobHandler | 用户角色有效期定时任务：XXL-Job + Spring @Scheduled 双触发，RedisLock 分布式锁                      |
| config | IamAdminConfig           | Admin 配置：`iam.api.scan.enabled`(0/1)、`iam.api.scan.insert`(0/1)                 |

### 路由常量 (Route)

所有路由定义在 `Route` 接口中，使用 `@Router(module="iam-admin", prefix="/iam-admin")` + `@ApiDesc` 注解，供 RestfulScan
自动扫描。

## 核心流程

### 用户创建 (IamUserService.customCreate)

```
1. 用户名唯一性校验
2. RedisIdGenerator 生成 userCode (前缀 user_)
3. iam_user 入库
4. iam_user_auth 入库 (authType=PASSWORD, authStatus=1)
5. iam_user_auth_password 入库 (salt + MD5加密)
6. iam_user_password_his 入库
```

### 角色树构建 (IamRoleService.roleTree)

```
1. 查询应用下所有角色
2. 构建 roleCode → IamRoleDto Map
3. parentCode="0" 为顶级节点
4. 其他节点挂载到父节点的 children 列表
```

### API 自动扫描 (RestfulScan)

```
1. 启动时执行（ApplicationRunner）
2. 受 iam.api.scan.enabled 开关控制
3. 对比数据库 API 与 @Router 注解路由
4. 新增路由 → 插入（受 iam.api.scan.insert 控制）
5. 属性变更 → 更新
6. 已删除路由 → 日志输出删除SQL
```

### 实体字段分析 (EntityFieldAnalyzer)

```
1. 通过 RestInfo.returnGenericInfo 解析 R<T>/PageData<T> 泛型
2. 递归解包定位业务实体类
3. 反射读取 @FieldDesc 注解生成字段树
4. 支持 List/Set 泛型、嵌套对象、JSONPath 生成
5. 过滤 BaseEntity 系统字段
```

## 使用方式

当涉及以下场景时调用此技能：

- 用户/角色/菜单/应用/API/AK 等 CRUD 管理接口开发
- 用户角色绑定/解绑、角色菜单绑定、菜单API绑定等关联管理
- API 自动扫描与同步
- 实体字段分析与字段权限配置
- 用户角色有效期定时任务
- Admin 路由常量定义
- Req/Resp 类设计模式参考
