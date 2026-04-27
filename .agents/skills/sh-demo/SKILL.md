---
name: "sh-demo"
description: "sh-framework 示例模块知识库。展示基于sh-framework搭建CRUD服务的四步标准范式：定义实体(extends BaseEntity)→定义Mapper(extends BaseMapper)→定义Service(extends BaseService)→定义REST控制器。包含完整User CRUD示例和application.yml配置。当需要参考框架使用方式、编写新业务模块、了解RESTful接口规范时调用。"
---

# sh-demo 模块知识库

sh-demo 是 sh-framework 的演示模块，展示了一个典型的业务应用如何基于 sh-framework 快速搭建 CRUD 服务。

## 包结构

```
com.wkclz.demo
├── DemoApplication      # 启动类
├── entity/
│   └── User             # 用户实体（extends BaseEntity）
├── mapper/
│   └── UserMapper       # 用户Mapper（extends BaseMapper<User>）
├── service/
│   └── UserService       # 用户Service（extends BaseService<User, UserMapper>）
└── rest/
    └── UserRest          # 用户REST接口（完整CRUD）
```

## 四步标准范式

### 1. 定义实体

```java
@Data
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {
    private String userCode;
    private String username;
    private String nickname;
    private Integer userStatus;
}
```

继承 `BaseEntity` 自动获得：id, sort, createTime, createBy, updateTime, updateBy, remark, version, userCode, tenantCode, orderBy, ids, keyword, timeFrom, timeTo, current, size, offset, total, count, debug

### 2. 定义Mapper

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {}
```

零代码获得14个CRUD方法：insert, insertBatch, deleteById, deleteByIdEntity, deleteByIds, deleteByIdsEntity, updateById, updateByIdSelective, updateBatch, selectById, selectByIds, selectAll, selectByEntity, selectOneByEntity, selectCountByEntity

### 3. 定义Service

```java
@Service
public class UserService extends BaseService<User, UserMapper> {}
```

零代码获得BaseService全部方法，包括 selectPage 分页查询。

### 4. 定义REST接口

```java
@RestController
@RequestMapping("/user")
public class UserRest {
    @Autowired
    private UserService userService;
    // ... CRUD接口
}
```

## RESTful接口规范

| HTTP方法 | 路径 | 方法名 | 功能 | 请求体/参数 | 返回类型 |
|---------|------|--------|------|-----------|---------|
| POST | /user | addUser | 新增 | User JSON | User |
| POST | /user/batch | addUsers | 批量新增 | List\<User\> | List\<User\> |
| DELETE | /user/{id} | deleteUser | ID删除 | PathVariable id | Integer |
| DELETE | /user | deleteUser | 批量删除 | User（含ids） | Integer |
| PUT | /user | updateUser | 全字段更新 | User JSON | Integer |
| PATCH | /user | updateUserSelective | 选择性更新 | User JSON | Integer |
| GET | /user/{id} | getUserById | ID查询 | PathVariable id | User |
| GET | /user/ids | getUsersByIds | ID列表查询 | RequestParam ids | List\<User\> |
| GET | /user | getAllUsers | 查询全部 | 无 | List\<User\> |
| POST | /user/list | getUsersByCondition | 条件查询 | User JSON | List\<User\> |
| POST | /user/page | getUsersByPage | 分页查询 | User JSON | PageData\<User\> |

### 语义约定

- **PUT vs PATCH**：PUT=updateById（全字段），PATCH=updateByIdSelective（非null字段）
- **POST /list**：条件查询（使用selectByEntity，支持keyword/timeFrom/timeTo）
- **POST /page**：分页查询（使用selectPage，current/size来自BaseEntity）
- **DELETE**：ID删除和批量删除使用不同参数形式

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

按需引入其他模块可获得更多能力：sh-redis(缓存/锁)、sh-web(全局异常处理)、sh-mqtt(MQTT消息)、sh-xxljob(定时任务)、sh-dynamicdb(动态数据源)。
