---
name: "sh-core"
description: "sh-framework 核心基础模块知识库。包含实体体系(DbColumnEntity/BaseEntity/PageData/UserInfo)、异常体系(CommonException及7个子类)、ResultCode枚举、R统一响应、UserContext用户上下文、自定义注解。当涉及实体定义、异常处理、响应封装、用户上下文、结果码时调用。"
---

# sh-core 模块知识库

sh-core 是 sh-framework 的核心模块，提供实体体系、异常体系、统一响应、用户上下文、自定义注解和日志脱敏等基础能力。所有业务模块均依赖此模块。

## 包结构

```
com.wkclz.core
├── annotation/
│   └── Router             # 路由注解（module + prefix），仅用于类
├── base/
│   ├── DbColumnEntity     # 数据库规范字段基类
│   ├── BaseEntity         # 业务实体基类（继承DbColumnEntity）
│   ├── PageData<T>        # 分页数据封装
│   ├── UserInfo           # 用户基础信息
│   └── R<T>               # 统一响应结果类
├── enums/
│   ├── ResultCode         # 结果码枚举（28个值）
│   └── EnvType            # 系统环境类型（DEV/SIT/UAT/PROD）
├── exception/
│   ├── CommonException    # 业务异常基类（RuntimeException）
│   ├── ApiException       # API调用异常
│   ├── ApplicationException # 应用级业务异常
│   ├── NotFoundException  # 资源未找到异常
│   ├── SystemException    # 系统级异常
│   ├── UnauthorizedException # 未授权异常
│   ├── UserException      # 用户操作异常
│   └── ValidationException # 数据校验异常
├── log/
│   └── MaskingPatternLayout # Logback日志脱敏布局器
└── user/
    └── UserContext         # 基于ThreadLocal的用户上下文
```

## 实体体系

### 继承关系

```
Serializable
  └── DbColumnEntity（数据库规范字段）
        └── BaseEntity（业务实体基类，所有业务实体必须继承）
```

### DbColumnEntity — 数据库规范字段

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键ID |
| sort | Integer | 排序号（越大越往后） |
| createTime | LocalDateTime | 创建时间 |
| createBy | String | 创建人code |
| updateTime | LocalDateTime | 更新时间 |
| updateBy | String | 更新人code |
| remark | String | 备注 |
| version | Integer | 数据版本（乐观锁） |

**隐含字段**（sh-mybatis层使用）：deleted（逻辑删除标记）

### BaseEntity — 业务实体基类

在 DbColumnEntity 基础上新增：

**用户/租户字段**：userCode(String), tenantCode(String)

**查询辅助字段**：orderBy(String), ids(List\<Long\>), keyword(String), timeFrom(LocalDateTime), timeTo(LocalDateTime)

**分页辅助字段**：current(Long, 默认1), size(Long, 默认10), offset(Long), total(Long), count(Long)

**关键方法**：
- `init()` — 初始化分页参数（current/size默认值，计算offset）
- `copy(source, target)` — 全属性拷贝
- `copyIfNotNull(source, target)` — 非空属性拷贝

### PageData\<T\> — 分页数据封装

字段：current, size, offset, total, count, records(List\<T\>)

常用方法：`of()`, `empty()`, `fromEntity()`, `convert()`

## 异常体系

### 继承关系

```
RuntimeException
  └── CommonException（code字段，默认500）
        ├── ApiException        — API调用异常
        ├── ApplicationException — 应用级业务异常
        ├── NotFoundException   — 资源未找到
        ├── SystemException     — 系统级异常
        ├── UnauthorizedException — 未授权
        ├── UserException       — 用户操作异常
        └── ValidationException — 数据校验失败
```

### 使用方式

**推荐使用静态工厂方法**（所有7个子类均有相同的of方法签名）：

```java
// 模板消息格式化（支持{}占位符）
throw SystemException.of("操作失败: {}", errorMsg);
throw UserException.of("用户 {} 不存在", username);
throw ValidationException.of("参数校验失败: {}", fieldName);

// 自定义code
throw CommonException.of(40001, "数据版本不匹配");

// ResultCode枚举
throw UnauthorizedException.of(ResultCode.UNAUTHORIZED);
```

### 各异常适用场景

| 异常 | 场景 |
|------|------|
| SystemException | 系统级未预期错误（配置缺失、网络异常等），日志会打印完整堆栈+邮件告警 |
| UserException | 用户操作相关业务异常（密码错误、余额不足等），日志仅打印biz error，不打印堆栈 |
| ValidationException | 参数校验失败（必填为空、格式不正确等） |
| NotFoundException | 资源查询不到（根据ID查不到数据） |
| UnauthorizedException | 未授权访问（token无效、权限不足） |
| ApiException | 调用外部API失败 |
| ApplicationException | 其他应用级业务异常 |

## ResultCode 枚举（28个值）

| 分类 | 码段 | 示例 |
|------|------|------|
| HTTP标准 | 200/400/401/403/404/500 | SUCCESS, VALIDATION_ERROR, UNAUTHORIZED, ERROR |
| Token/登录 | 10001-10102 | TOKEN_NULL(10001), TOKEN_ERROR(10002), LOGIN_TIMEOUT(10007), TENANT_NULL(10102) |
| 跨域/路由 | 20001-20004 | CLIENT_CHANGE(20001), API_CORS(20002), ERROR_ROUTER(20004) |
| 登录/验证码 | 30001-30005 | USERNAME_PASSWORD_ERROR(30001), CAPTCHA_ERROR(30002) |
| 数据操作 | 40001-40006 | UPDATE_NO_VERSION(40001), RECORD_NOT_EXIST(40003), RECORD_DUPLICATE(40006) |
| 网络 | 50001-50003 | NETWORK_ERROR(50001), NO_AVAILABLE_SERVER(50002) |
| 订单 | 60001-60003 | ORDER_TIMEOUT(60001), ORDER_PAYED(60002) |

## R\<T\> — 统一响应结果

```java
R.ok()                           // 成功（code=200, data=null）
R.ok(data)                       // 成功（code=200, 带数据）
R.warn("参数不完整")              // 警告（code=400）
R.warn("缺少参数: {}", paramName) // 警告（模板消息）
R.error("系统异常")              // 错误（code=500）
R.error(commonException)         // 从CommonException提取code和message
R.error(40001, "版本不匹配")     // 自定义code
```

字段：code(int), msg(String), data(T), requestTime(LocalDateTime), responseTime(LocalDateTime), costTime(Long)

## UserContext — 用户上下文

基于 ThreadLocal\<UserInfo\> 实现，请求线程内存储登录用户信息。

```java
// 设置用户信息（通常在登录拦截器/过滤器中）
UserContext.setUserInfo(userinfo);

// 获取当前用户编码（可能返回null）
String userCode = UserContext.getUserCode();

// 获取当前租户编码（可能返回null）
String tenantCode = UserContext.getTenantCode();

// 清除上下文（必须在请求结束时调用，防止内存泄漏）
UserContext.clear();
```

## 自定义注解

- `@Router(module="模块名", prefix="路由前缀")` — 标注路由控制器类，用于RestHelper扫描

## OpenAPI 注解

- `@Schema(description="描述")` — 标注实体字段、枚举类的描述（替代已移除的 @FieldDesc）

## MaskingPatternLayout — 日志脱敏

继承Logback的PatternLayout，通过`addMaskPattern(String)`添加正则脱敏规则，日志输出时自动将命中内容替换为`*`。使用volatile保证多线程可见性。

## 依赖关系

sh-core 依赖 sh-tool（使用 StringFormat 进行消息格式化、BeanUtil 进行属性拷贝），不依赖 Spring 框架，是纯 Java 基础模块。
