# iam-sso 模块知识库

sh-iam SSO 登录认证核心模块，提供用户名密码登录、验证码、注册、用户信息查询、菜单资源获取、会话管理、请求日志持久化等 SSO
核心业务能力。路由前缀 `/iam-sso`。

## 关键类

### REST 控制器 (rest)

| 类名           | 路由         | 方法                                                                                                                     | 说明               |
|--------------|------------|------------------------------------------------------------------------------------------------------------------------|------------------|
| LoginRest    | `/iam-sso` | `publicSsoLogin(POST)` → 用户登录, `publicSsoLogout(GET)` → 用户登出                                                           | SSO 登录认证接口       |
| CaptchaRest  | `/iam-sso` | `getCaptcha(GET)` → 获取图形验证码                                                                                            | 验证码接口，验证码存 Redis |
| RegisterRest | `/iam-sso` | `publicSsoRegister(POST)` → 用户注册                                                                                       | 注册接口（待实现）        |
| UserInfoRest | `/iam-sso` | `userInfo(GET)` → 用户信息, `userMenuTree(GET)` → 用户菜单树, `userMenuTreeRuoyi(GET)` → 若依格式菜单树, `changePassword(POST)` → 修改密码 | 用户信息与菜单资源接口      |

### 服务类 (service)

| 类名                   | 说明                                                                 |
|----------------------|--------------------------------------------------------------------|
| IamLoginService      | SSO 登录核心业务：登录验证（7种状态）、登出、修改密码、登录日志记录                               |
| IamSsoServiceImpl    | 实现 IamSsoService 接口，Token 校验：Redis 获取 Session + 会话列表幽灵条目清理 + 被踢出检测 |
| IamSessionService    | 会话管理：invalidateAllSessions 使某用户所有会话失效（修改密码后调用）                     |
| SsoResourceService   | 菜单资源服务：获取用户菜单列表/树、若依格式菜单树转换（IamMenuDto → VueRouterMenu）            |
| SsoFacadeImpl        | 实现 SsoFacade 接口，请求日志持久化：将 RequestLog 转为 IamRequestLog 入库           |
| IamRequestService    | 请求日志服务：RequestLog → IamRequestLog 转换 + IP归属地缓存填充 + 入库              |
| UsernameCacheService | 用户名缓存服务：Guava LoadingCache(userCode→nickname)，支持批量查询、防穿透、自动过期      |

### 实体类 (entity)

| 类名            | 说明                                                                            |
|---------------|-------------------------------------------------------------------------------|
| VueRouterMenu | 若依格式路由菜单(path, name, hidden, redirect, component, alwaysShow, meta, children) |
| VueRouterMeta | 路由元数据(title, icon, noCache)                                                   |

### Mapper 接口 (mapper)

| 类名                  | 说明                                   |
|---------------------|--------------------------------------|
| SsoLoginMapper      | 登录数据访问：跨三表JOIN查用户、更新登录信息、密码查询、密码历史查询 |
| SsoLoginLogMapper   | 登录日志：插入日志、查询1小时内失败次数                 |
| SsoRequestLogMapper | 请求日志：插入日志、更新IP归属地                    |
| SsoResourceMapper   | 资源查询：获取用户菜单列表                        |

### 配置类 (config)

| 类名           | 配置项                                                                                                                 | 说明     |
|--------------|---------------------------------------------------------------------------------------------------------------------|--------|
| IamSsoConfig | `iam.sso.password-expire-days`(默认180), `iam.login.public-key/private-key`, `iam.sso.max-concurrent-sessions`(默认0不限) | SSO 配置 |

### 其他

| 类名                   | 说明                                                                                                   |
|----------------------|------------------------------------------------------------------------------------------------------|
| UserNameProviderImpl | UserNameProvider SPI 实现，通过 UsernameCacheService 批量查询用户昵称，供 BaseEntity 自动回填 createByName/updateByName |
| Ip2LocationScheduler | 启动时异步线程消费 IP 队列，解析 IP 归属地并批量更新数据库                                                                    |
| Route                | 路由常量接口，定义所有 SSO 路由（public/user/portal 三组）                                                            |
| IamSsoAutoConfig     | 自动配置：@AutoConfiguration + @ComponentScan + @MapperScan                                               |

## 核心流程

### 登录流程 (IamLoginService.loginByUsernameAndPassword)

```
1. RSA 解密前端密码（privateKey 非空且密码长度>32时）
2. 检查是否需要验证码（1小时内失败次数>0）
3. 验证码校验（Redis getAndDelete，一次性）
4. 跨三表JOIN查询用户（SsoLoginMapper.getUserAuth4PasswordByUsername）
5. 用户不存在 → 返回 USER_NOT_FOUND
6. 认证方式禁用(authStatus=0) → EXPIRED_ACCOUNT
7. 用户锁定(userStatus=3) → ACCOUNT_LOCKED
8. 用户禁用(userStatus=2) → ACCOUNT_DISABLED
9. 密码校验(MD5+salt) → INVALID_CREDENTIALS
10. 密码过期检查(passwordExpireDays) → EXPIRED_PASSWORD
11. 登录成功：生成JWT + Redis Session + 会话列表注册 + 并发会话控制
```

### 并发会话控制

- 登录时将 tokenMd5 加入 Redis ZSet（score=登录时间戳）
- 若超过 maxConcurrentSessions，按 score 升序踢出最早会话
- Token 校验时同步检查会话列表，被踢出的 Token 自动失效

### 修改密码流程

```
1. RSA 解密新旧密码
2. 校验旧密码
3. 检查新密码不能与最近3次密码相同
4. 生成新 salt + MD5 加密
5. 更新密码 + 插入历史记录
6. 使该用户所有会话失效（invalidateAllSessions）
```

### 若依菜单树转换

- IamMenuDto 树 → VueRouterMenu 树
- 仅转换 menuType=MENU 的节点
- 按钮编码(buttonCode)收集到 buttonsMap，按路由路径映射
- 路由路径转 PascalCase 组件名，防重复命名

## 使用方式

当涉及以下场景时调用此技能：

- SSO 登录/登出/修改密码逻辑
- 验证码生成与校验
- 用户菜单树/若依菜单树获取
- Token 校验与会话管理
- 请求日志持久化
- 用户名缓存（UserNameProvider SPI）
- IP归属地解析
- SSO 路由定义
