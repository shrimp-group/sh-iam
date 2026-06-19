---
name: code-review
description: 代码审查 SOP，检查编码规范、类型安全、异常处理和测试覆盖
license: MIT
compatibility: opencode
---

### 审查清单

#### 类型与接口

* Java: 方法签名是否包含完整参数和返回类型，Bean Validation 注解是否正确，Controller 的请求/响应 DTO 是否匹配
* Node: 函数签名是否包含 TypeScript 类型注解，Zod/Joi schema 是否与接口定义一致，API 路由的请求/响应类型是否匹配

#### 错误处理

* Java: 外部调用是否包裹 try-catch，异常是否区分业务异常(BusinessException)和系统异常(SystemException)，是否避免空 catch 块
* Node: 异步操作是否使用 try-catch 包裹，错误是否区分业务错误和系统错误，是否避免了未处理的 Promise rejection

#### 安全

* Both: 用户输入是否经过校验，SQL 是否使用参数化查询（无字符串拼接），敏感信息是否未出现在日志中

#### 测试

* Java: 新增/修改的接口有 JUnit 测试用例，测试覆盖正常流程和异常边界，Mock 外部依赖（Mockito）
* Node: 新增/修改的接口有 Jest 测试用例，测试覆盖正常流程和异常边界，Mock 外部依赖

#### Req/Resp 规范

* Java: Req 对象是否放在 `bean/req` 包下并继承公共父类（IdReq、PageReq、UpdateReq、RemoveReq），Resp 对象是否放在 `bean/resp`
  包下并继承公共父类（EntityResp）

#### 参数校验

* Java: 参数校验是否使用了 `Assert.notNull()`、`Assert.hasText()` 等 Spring Assert 方法，应替换为 `@Valid` + Bean
  Validation 注解（如 `@NotNull`、`@NotBlank` 等）

#### 对象复制

* Java: 数据库映射类（Entity）是否使用了类内复制方法，其他对象转换是否统一使用了 `BeanUtil.cp`

#### Controller 层职责

* Java: Controller 层是否仅负责参数转换和基本检查，是否包含了不应有的业务逻辑（业务逻辑应下沉到 Service 层）

#### Swagger/OpenAPI 配置

* Java: 是否配置了 `OpenApiConfig.java`，Controller 是否使用了 `@Tag`、`@Operation` 等 OpenAPI 注解

#### 风格

* Java: 遵循项目编码规范（Checkstyle/SpotBugs），无死代码，import 按规范排序
* Node: 遵循项目编码规范（ESLint/Prettier），无死代码，import 按规范排序
