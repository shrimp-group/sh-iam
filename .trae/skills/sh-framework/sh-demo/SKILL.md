---
name: "sh-demo"
description: "sh-framework 示例模块知识库。展示基于sh-framework搭建CRUD服务的标准范式：定义实体(extends BaseEntity)→定义Mapper(extends BaseMapper)→定义Service(extends BaseService)→定义VO类(Req/Resp)→定义Route路由→定义REST控制器。包含完整User CRUD示例、VO分层、路由管理和application.yml配置。当需要参考框架使用方式、编写新业务模块、了解RESTful接口规范时调用。"
---

# sh-demo 模块知识库

sh-demo 是 sh-framework 的演示模块，展示了一个典型的业务应用如何基于 sh-framework 快速搭建 CRUD 服务。

## 包结构

```
com.wkclz.demo
├── DemoApplication           # 启动类
├── bean/
│   ├── entity/
│   │   └── User              # 用户实体（extends BaseEntity）
│   └── vo/
│       └── user/
│           ├── UserCreateReq    # 创建用户请求
│           ├── UserUpdateReq    # 更新用户请求
│           ├── UserPageReq      # 分页查询请求
│           ├── UserResp         # 用户详情响应
│           └── UserPageResp     # 分页列表响应
├── mapper/
│   └── UserMapper          # 用户Mapper（extends BaseMapper<User>）
├── service/
│   └── UserService         # 用户Service（extends BaseService<User, UserMapper>）
└── rest/
    ├── Route               # 路由常量定义
    └── UserRest            # 用户REST接口
```

## 六步标准范式

### 1. 定义实体

```java
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户实体")
public class User extends BaseEntity {
    @Schema(description = "用户编码", example = "U001")
    private String userCode;
    
    @Schema(description = "用户名", example = "admin")
    private String username;
    
    @Schema(description = "昵称", example = "管理员")
    private String nickname;
    
    @Schema(description = "用户状态：0-禁用，1-启用", example = "1")
    private Integer userStatus;
}
```

继承 `BaseEntity` 自动获得：id, sort, createTime, createBy, updateTime, updateBy, remark, version, userCode, tenantCode, orderBy, ids, keyword, timeFrom, timeTo, current, size, offset, total, count, debug

### 2. 定义 Mapper

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {}
```

零代码获得14个CRUD方法：insert, insertBatch, deleteById, deleteByIdEntity, deleteByIds, deleteByIdsEntity, updateById, updateByIdSelective, updateBatch, selectById, selectByIds, selectAll, selectByEntity, selectOneByEntity, selectCountByEntity

### 3. 定义 Service

```java
@Service
public class UserService extends BaseService<User, UserMapper> {}
```

零代码获得BaseService全部方法，包括 selectPage 分页查询。

### 4. 定义 VO 类

**请求 VO - UserCreateReq**

```java
@Data
@Schema(description = "创建用户请求")
public class UserCreateReq implements Serializable {
    @NotBlank(message = "用户编码不能为空")
    @Schema(description = "用户编码", example = "U001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userCode;
    
    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;
    
    @Schema(description = "昵称", example = "管理员")
    private String nickname;
    
    @NotNull(message = "用户状态不能为空")
    @Schema(description = "用户状态：0-禁用，1-启用", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer userStatus;
}
```

**请求 VO - UserUpdateReq**

```java
@Data
@Schema(description = "更新用户请求")
public class UserUpdateReq extends UpdateReq {
    @Schema(description = "用户编码", example = "U001")
    private String userCode;
    
    @Schema(description = "用户名", example = "admin")
    private String username;
    
    @Schema(description = "昵称", example = "管理员")
    private String nickname;
    
    @Schema(description = "用户状态：0-禁用，1-启用", example = "1")
    private Integer userStatus;
}
```

**请求 VO - UserPageReq**

```java
@Data
@Schema(description = "用户分页查询请求")
public class UserPageReq extends PageReq {
    @Schema(description = "用户编码,模糊搜索", example = "U001")
    private String userCode;
    
    @Schema(description = "用户名,模糊搜索", example = "admin")
    private String username;
    
    @Schema(description = "昵称2,模糊搜索", example = "管理员")
    private String nickname;
    
    @Schema(description = "用户状态：0-禁用，1-启用", example = "1")
    private Integer userStatus;
    
    @Schema(description = "页码，默认1", example = "1")
    private Long current = 1L;
    
    @Schema(description = "每页条数，默认10", example = "10")
    private Long size = 10L;
}
```

**响应 VO - UserResp**

```java
@Data
@Schema(description = "用户响应")
public class UserResp extends EntityResp {
    @Schema(description = "用户编码", example = "U001")
    private String userCode;
    
    @Schema(description = "用户名", example = "admin")
    private String username;
    
    @Schema(description = "昵称", example = "管理员")
    private String nickname;
    
    @Schema(description = "用户状态：0-禁用，1-启用", example = "1")
    private Integer userStatus;
}
```

**响应 VO - UserPageResp**

```java
@Data
@Schema(description = "用户分页响应")
public class UserPageResp extends EntityResp {
    @Schema(description = "用户编码", example = "U001")
    private String userCode;
    
    @Schema(description = "用户名", example = "admin")
    private String username;
    
    @Schema(description = "昵称", example = "管理员")
    private String nickname;
    
    @Schema(description = "用户状态：0-禁用，1-启用", example = "1")
    private Integer userStatus;
}
```

### 5. 定义路由常量

```java
@Router(module = Route.PREFIX, prefix = Route.PREFIX)
public interface Route {
    String PREFIX = "/sh-demo";

    String USER_PAGE = "/user/page";
    String USER_INFO = "/user/info";
    String USER_CREATE = "/user/create";
    String USER_CREATE_BATCH = "/user/create/batch";
    String USER_UPDATE = "/user/update";
    String USER_REMOVE = "/user/remove";
}
```

### 6. 定义 REST 接口

```java
@Tag(name = "1.用户管理", description = "用户增删改查接口")
@RestController
@RequestMapping(Route.PREFIX)
public class UserRest {
    @Autowired
    private UserService userService;
    
    @Operation(summary = "1.用户-分页查询", description = "根据条件分页查询用户列表")
    @GetMapping(Route.USER_PAGE)
    public R<PageData<UserPageResp>> userPage(@Valid UserPageReq req) {
        setLoginUser();
        User user = new User();
        BeanUtils.copyProperties(req, user);
        PageData<User> page = userService.selectPage(user);
        List<UserPageResp> list = page.getRecords().stream().map(t -> {
            UserPageResp resp = new UserPageResp();
            BeanUtils.copyProperties(t, resp);
            return resp;
        }).toList();
        PageData<UserPageResp> convert = PageData.convert(page, list);
        return R.ok(convert);
    }
    
    @Operation(summary = "2.用户-详情", description = "根据ID查询用户详情")
    @GetMapping(Route.USER_INFO)
    public R<UserResp> userInfo(@Valid IdReq req) {
        setLoginUser();
        User user = userService.selectById(req.getId());
        if (user == null) {
            throw NotFoundException.of("用户不存在，ID: {}", req.getId());
        }
        UserResp resp = new UserResp();
        BeanUtils.copyProperties(user, resp);
        return R.ok(resp);
    }
    
    @Operation(summary = "3.用户-创建", description = "创建新用户")
    @PostMapping(Route.USER_CREATE)
    public R<UserResp> userCreate(@Valid @RequestBody UserCreateReq req) {
        setLoginUser();
        User user = new User();
        BeanUtils.copyProperties(req, user);
        userService.insert(user);
        UserResp resp = new UserResp();
        BeanUtils.copyProperties(user, resp);
        return R.ok(resp);
    }
    
    @Operation(summary = "4.用户-更新", description = "更新用户信息（需要版本号）")
    @PostMapping(Route.USER_UPDATE)
    public R<Integer> userUpdate(@Valid @RequestBody UserUpdateReq req) {
        setLoginUser();
        User user = new User();
        BeanUtils.copyProperties(req, user);
        int i = userService.updateByIdSelective(user);
        return R.ok(i);
    }
    
    @Operation(summary = "5.用户-删除", description = "根据ID删除用户")
    @PostMapping(Route.USER_REMOVE)
    public R<Integer> userRemove(@Valid @RequestBody RemoveReq req) {
        setLoginUser();
        int i = userService.deleteById(req.getId());
        return R.ok(i);
    }
    
    private void setLoginUser() {
        UserInfo userinfo = new UserInfo();
        userinfo.setUserCode("userCode");
        userinfo.setUsername("username");
        UserContext.setUserInfo(userinfo);
    }
}
```

## RESTful 接口规范

| HTTP方法 | 路径 | 方法名 | 功能 | 请求 | 响应 |
|---------|------|--------|------|------|------|
| GET | /sh-demo/user/page | userPage | 分页查询 | UserPageReq | PageData<UserPageResp> |
| GET | /sh-demo/user/info | userInfo | 查询详情 | IdReq | UserResp |
| POST | /sh-demo/user/create | userCreate | 创建用户 | UserCreateReq | UserResp |
| POST | /sh-demo/user/update | userUpdate | 更新用户 | UserUpdateReq | Integer |
| POST | /sh-demo/user/remove | userRemove | 删除用户 | RemoveReq | Integer |

### 语义约定

- **统一前缀**：所有接口统一使用 `/sh-demo` 前缀
- **路径规范**：使用 `/user/page` 而非 `/userPage` 的 REST 风格
- **HTTP方法**：查询用 GET，创建/更新/删除用 POST
- **参数校验**：使用 `@Valid` 配合 jakarta.validation 注解
- **响应包装**：统一返回 `R<T>` 包装对象
- **Bean转换**：使用 `BeanUtils.copyProperties` 进行对象转换
- **分页转换**：使用 `PageData.convert(page, list)` 转换分页数据

## UserContext 使用

Demo 中每个接口调用 `setLoginUser()` 模拟登录用户，因为 MyBatisUpdateInterceptor 会从 UserContext 获取 userCode 填充 createBy/updateBy：

```java
private void setLoginUser() {
    UserInfo userinfo = new UserInfo();
    userinfo.setUserCode("userCode");
    userinfo.setUsername("username");
    UserContext.setUserInfo(userinfo);
}
```

生产环境中，这些信息通常由认证拦截器自动设置，不需要手动调用。

## 配置示例

### application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: sh-demo
  profiles:
    active: local
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
  jackson:
    default-property-inclusion: non_null

mybatis:
  mapper-locations: classpath*:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true

pagehelper:
  helperDialect: mysql
  reasonable: true
  supportMethodsArguments: true
  params: count=countSql
```

### application-local.yml

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/sh_demo?useUnicode&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root
```

## 隐式获得的框架能力

仅引入 `sh-mybatis` 即可自动获得：

- MyBatis拦截器自动填充 createBy/updateBy
- 逻辑删除支持（deleted字段自动过滤）
- 分页查询支持（PageHelper集成）
- 下划线-驼峰自动映射
- 乐观锁支持（version字段）
- @Blob大字段分离查询
- SQL注入防护（ORDER BY白名单校验）

按需引入其他模块可获得更多能力：sh-redis(缓存/锁)、sh-web(全局异常处理/用户名自动填充/标准Bean)、sh-mqtt(MQTT消息)、sh-xxljob(定时任务)、sh-dynamicdb(动态数据源)。
