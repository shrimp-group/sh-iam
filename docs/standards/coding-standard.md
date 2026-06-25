# 编码规范

## 命名规范

### 实体类命名

| 类型 | 模式 | 示例 |
|------|------|------|
| 实体类 | Iam + 业务名 | IamUser, IamRole |
| DTO类 | Iam + 业务名 + Dto | IamUserDto |
| Service类 | Iam + 业务名 + Service | IamUserService |
| Mapper类 | Iam + 业务名 + Mapper | IamUserMapper |
| Controller类 | 业务名 + Rest | UserRest, RoleRest |

### 文件命名

| 类型 | 模式 | 示例 |
|------|------|------|
| 表名 | iam_ + 下划线业务名 | iam_user, iam_user_auth |
| 路由常量 | 业务名 + PATH | USER_PAGE_PATH |

### 变量命名

| 类型 | 模式 | 示例 |
|------|------|------|
| 普通变量 | 小驼峰 | userName, userCode |
| 常量 | 全大写+下划线 | MAX_RETRY_TIMES |
| 类成员 | 小驼峰 | private String userCode |

## 代码风格

### 注解使用

```java
@Data
@EqualsAndHashCode(callSuper = false)
public class IamUser extends BaseEntity {
    @Desc("用户编码")
    private String userCode;
}
```

### 服务层规范

```java
@Service
public class IamUserService extends BaseService<IamUser, IamUserMapper> {
    // 业务方法
}
```

### 控制器规范

```java
@RestController
@RequestMapping(Route.PREFIX)
public class UserRest {
    @Autowired
    private IamUserService iamUserService;
}
```

### 异常处理

```java
Assert.notNull(user, "用户不存在");
if (StringUtils.isEmpty(userCode)) {
    throw new ValidationException("用户编码不能为空");
}
```

## 路由规范

| 模块 | 前缀 | 说明 |
|------|------|------|
| SSO公开接口 | /iam-sso/public/** | 无需鉴权 |
| SSO用户接口 | /iam-sso/user/** | 需鉴权 |
| Admin接口 | /iam-admin/** | 需鉴权 |

### 路由命名

| 操作 | 路径 | 方法 |
|------|------|------|
| 分页查询 | /{resource}/page | GET |
| 单个查询 | /{resource}/{id} | GET |
| 新增 | /{resource} | POST |
| 更新 | /{resource} | PUT |
| 删除 | /{resource}/{id} | DELETE |
| 绑定 | /{resource}/bind | POST |
| 解绑 | /{resource}/unbind | POST |

## 数据库操作规范

### 实体字段约定

所有实体必须包含以下字段：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 主键 |
| sort | Integer | 排序 |
| createTime | Date | 创建时间 |
| createBy | String | 创建人 |
| updateTime | Date | 更新时间 |
| updateBy | String | 更新人 |
| remark | String | 备注 |
| version | Integer | 乐观锁 |
| deleted | Boolean | 逻辑删除 |

### SQL编写规范

- 使用 MyBatis XML 编写复杂 SQL
- 参数使用 `#{}` 方式防止 SQL 注入
- 逻辑删除条件自动拼接
- 使用 `@Blob` 注解处理大字段

## 安全规范

### 密码安全

- 前端使用 RSA 加密传输密码
- 后端使用 MD5 + salt 存储密码
- 禁止明文存储密码

### 输入验证

- 使用 `@Valid` 注解进行参数校验
- 对用户输入进行严格校验
- 防止 XSS 攻击

### Token管理

- JWT Token 存储在 Authorization Header
- Token 过期时间配置化
- 支持 Token 刷新机制

## 日志规范

### 日志级别

| 级别 | 用途 |
|------|------|
| DEBUG | 开发调试信息 |
| INFO | 业务流程日志 |
| WARN | 警告信息 |
| ERROR | 错误信息 |

### 日志格式

```java
log.info("用户登录成功, userCode={}, ip={}", userCode, ip);
log.error("用户登录失败, username={}, error={}", username, e.getMessage(), e);
```

## 注释规范

### 类注释

```java
/**
 * 用户服务类
 * 负责用户的CRUD操作和业务逻辑处理
 */
@Service
public class IamUserService {
}
```

### 方法注释

```java
/**
 * 创建用户
 * @param dto 用户DTO
 * @return 创建后的用户实体
 */
public IamUser create(IamUserDto dto) {
}
```

### 字段注释

```java
@Desc("用户编码")
private String userCode;
```

## Git提交规范

### 提交格式

```
类型(模块): 描述

详细说明
```

### 类型说明

| 类型 | 说明 |
|------|------|
| feat | 新增功能 |
| fix | 修复bug |
| docs | 文档更新 |
| style | 代码格式 |
| refactor | 代码重构 |
| test | 测试代码 |
| chore | 构建/工具 |

### 示例

```
feat(iam-sso): 新增短信验证码登录功能

- 添加短信验证码发送接口
- 添加短信验证码登录逻辑
- 更新登录请求DTO
```

## 错误处理规范

### 异常类型

| 异常 | 用途 |
|------|------|
| ValidationException | 参数校验异常 |
| UserException | 用户业务异常 |
| SystemException | 系统异常 |
| NotFoundException | 资源不存在 |

### 错误响应

```json
{
  "code": 400,
  "message": "参数校验失败",
  "data": null
}
```

## 性能规范

### 缓存策略

- 使用 Guava LoadingCache 缓存高频访问数据
- Redis 缓存 Session 和验证码
- 设置合理的过期时间

### 查询优化

- 避免 N+1 查询问题
- 使用批量操作
- 添加适当索引

### 异步处理

- 耗时操作使用异步执行
- 日志记录异步写入
- 使用线程池管理