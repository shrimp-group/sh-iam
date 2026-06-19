# iam-common 模块知识库

sh-iam 公共基础模块，定义了 20 个实体类、20 个 DTO 类和 2 个工具类，为所有后端模块（iam-sdk、iam-sso、iam-admin）提供统一的数据模型和基础工具。

## 关键类

### 实体类 (entity)

| 类名                  | 对应表                    | 关键字段                                                                                                                                                                                       | 说明              |
|---------------------|------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------|
| IamUser             | iam_user               | userCode, username, nickname, email, phone, avatar, userStatus(1启用/2禁用/3锁定)                                                                                                                | 用户实体            |
| IamRole             | iam_role               | tenantCode, appCode, parentCode, roleCode, roleName, applicable(1可申请/0仅树节点)                                                                                                                | 角色实体            |
| IamMenu             | iam_menu               | appCode, parentCode, menuCode, menuName, icon, menuType(MENU/BUTTON/FIELDS), routePath, component, buttonCode, hidden                                                                      | 菜单实体            |
| IamApi              | iam_api                | module, appCode, apiCode, apiMethod, apiUri, apiName, writeFlag                                                                                                                            | API路由映射实体       |
| IamApp              | iam_app                | appCode, appName, domain, authType, appIcon, loginBgp                                                                                                                                      | 应用实体            |
| IamAccessKey        | iam_access_key         | appCode, appId, accessKey, secretKey, enableStatus, enableStart, enableStop                                                                                                                | AK密钥实体          |
| IamAccessKeyApi     | iam_access_key_api     | appCode, appId, apiCode                                                                                                                                                                    | AK-API关联实体      |
| IamUserAuth         | iam_user_auth          | userCode, authType(PASSWORD/LDAP), authIdentifier, lastAuthTime, authStatus(0禁用/1启用), lastLoginTime, lastLoginIp, loginCount                                                               | 用户认证关系实体        |
| IamUserAuthPassword | iam_user_auth_password | userCode, password, salt, lastChangedTime                                                                                                                                                  | 密码认证实体          |
| IamUserPasswordHis  | iam_user_password_his  | userCode, password, salt                                                                                                                                                                   | 密码变更历史实体        |
| IamUserRole         | iam_user_role          | tenantCode, appCode, userCode, roleCode, startTime, endTime, enableStatus                                                                                                                  | 用户-角色关联实体(含有效期) |
| IamRoleMenu         | iam_role_menu          | appCode, roleCode, menuCode                                                                                                                                                                | 角色-菜单关联实体       |
| IamRoleData         | iam_role_data          | appCode, roleCode, dimensionCode, dataCode                                                                                                                                                 | 角色-数据维度关联实体     |
| IamMenuApi          | iam_menu_api           | appCode, menuCode, apiCode                                                                                                                                                                 | 菜单-API关联实体      |
| IamMenuField        | iam_menu_field         | appCode, menuCode, apiCode, fieldCode                                                                                                                                                      | 菜单字段关系实体        |
| IamApiField         | iam_api_field          | appCode, apiCode, fieldCode, fieldName, jsonPath, action(HIDDEN/MASK/READ_ONLY), maskRule, description                                                                                     | API字段权限实体       |
| IamDataDimension    | iam_data_dimension     | appCode, dimensionCode, dimensionName, dimensionDataJson, dimensionScript                                                                                                                  | 数据权限维度实体        |
| IamLoginLog         | iam_login_log          | authIdentifier, userCode, username, authType, loginStatus, message, ipAddress, userAgent                                                                                                   | 登录日志实体          |
| IamRequestLog       | iam_request_log        | tenantCode, appCode, userAgent, browserName, remoteAddr, method, requestUri, requestBody, responseBody, httpStatus, location, isp, token, userCode, username, nickname, costTime, errorMsg | 请求日志实体(30+字段)   |
| IamTenant           | iam_tenant             | tenantCode, tenantName, enableFlag, enableBegin, enableEnd                                                                                                                                 | 租户实体            |

### DTO 类 (dto)

| 类名                     | 继承                  | 扩展字段                                         | 说明                  |
|------------------------|---------------------|----------------------------------------------|---------------------|
| IamUserDto             | IamUser             | password                                     | 用户DTO，用于创建用户时传递密码   |
| IamRoleDto             | IamRole             | childrenCount, children(List), userBindCount | 角色DTO，支持树形结构和绑定数量   |
| IamMenuDto             | IamMenu             | childrenCount, children(List), apiBindCount  | 菜单DTO，支持树形结构和接口绑定数量 |
| IamAppDto              | IamApp              | -                                            | 应用DTO               |
| IamApiDto              | IamApi              | -                                            | API DTO             |
| IamAccessKeyDto        | IamAccessKey        | -                                            | AK密钥DTO             |
| IamAccessKeyApiDto     | IamAccessKeyApi     | -                                            | AK-API关联DTO         |
| IamUserAuthDto         | IamUserAuth         | -                                            | 用户认证DTO             |
| IamUserAuthPasswordDto | IamUserAuthPassword | -                                            | 密码认证DTO             |
| IamUserPasswordHisDto  | IamUserPasswordHis  | -                                            | 密码历史DTO             |
| IamUserRoleDto         | IamUserRole         | -                                            | 用户角色关联DTO           |
| IamRoleMenuDto         | IamRoleMenu         | -                                            | 角色菜单关联DTO           |
| IamRoleDataDto         | IamRoleData         | -                                            | 角色数据关联DTO           |
| IamMenuApiDto          | IamMenuApi          | -                                            | 菜单API关联DTO          |
| IamMenuFieldDto        | IamMenuField        | -                                            | 菜单字段关联DTO           |
| IamApiFieldDto         | IamApiField         | -                                            | API字段权限DTO          |
| IamDataDimensionDto    | IamDataDimension    | -                                            | 数据维度DTO             |
| IamLoginLogDto         | IamLoginLog         | -                                            | 登录日志DTO             |
| IamRequestLogDto       | IamRequestLog       | -                                            | 请求日志DTO             |
| IamTenantDto           | IamTenant           | -                                            | 租户DTO               |

### 工具类 (helper)

| 类名                 | 方法                                                                                                                                                 | 说明                                                           |
|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------|
| PasswordHelper     | `generatePassword(password, salt)` → MD5加密, `validatePassword(password, salt, md5)` → 校验, `isPasswordInHistory(newPassword, historyList)` → 密码历史检查 | 密码加密校验工具，使用 MD5(password+salt) 方式                            |
| IpLocalCacheHelper | `offerQueue(remoteAddr)` → 写入IP队列, `pollQueue()` → 取出IP, `getLocation(remoteAddr)` → 解析IP归属地                                                       | IP归属地缓存工具，基于ConcurrentHashMap+ConcurrentLinkedQueue，异步解析IP地址 |

## 核心模式

### 实体类通用模式

- 所有实体继承 `BaseEntity`（含 id, sort, createTime, createBy, updateTime, updateBy, remark, version 字段）
- 使用 `@Data` + `@EqualsAndHashCode(callSuper = false)` + `@FieldDesc` 注解
- 每个实体提供 `copy(source, target)` 和 `copyIfNotNull(source, target)` 静态方法
- DTO 继承对应 Entity，扩展业务字段，提供 `copy(source)` 静态方法

### 数据库关系

```
iam_app → iam_role, iam_menu, iam_api, iam_data_dimension, iam_access_key
iam_user → iam_user_auth → iam_user_auth_password
iam_user → iam_user_password_his, iam_user_role
iam_role → iam_role_menu, iam_role_data
iam_menu → iam_menu_api, iam_menu_field
iam_api → iam_api_field
iam_access_key → iam_access_key_api
```

## 使用方式

当涉及以下场景时调用此技能：

- 新增或修改 IAM 实体/DTO 定义
- 查询实体字段、表结构、关联关系
- 密码加密校验逻辑（PasswordHelper）
- IP归属地缓存逻辑（IpLocalCacheHelper）
- 理解数据库表设计和实体继承体系
