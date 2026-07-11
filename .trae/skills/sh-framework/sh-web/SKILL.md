---
name: "sh-web"
description: "sh-framework Web模块知识库。包含ErrorHandler全局异常处理(8种异常+兜底+邮件告警)、IpHelper IP解析、RequestHelper请求工具、ResponseHelper响应工具、RestHelper接口扫描、LocalThreadHelper线程上下文、UserNameBodyAdvice用户名自动填充、AtLeastOneNotNull校验注解、标准Request/Response Bean。当涉及全局异常处理、IP获取、请求响应工具、REST接口元数据扫描、用户名自动填充、参数校验时调用。"
---

# sh-web 模块知识库

sh-web 是 sh-framework 的 Web 层基础设施模块，提供全局异常处理、IP 地址工具、请求/响应工具、REST
接口扫描、线程上下文管理、用户名自动填充、参数校验注解和标准请求响应 Bean。

## 包结构

```
com.wkclz.web
├── ShWebAutoConfig            # 自动配置
├── annotation/
│   ├── AtLeastOneNotNull      # 校验注解（至少一个字段不为空）
│   └── validator/
│       └── AtLeastOneNotNullValidator  # 注解实现
├── rest/
│   ├── ErrorHandler           # 全局异常处理器（@RestControllerAdvice）
│   └── UserNameBodyAdvice     # 响应体用户名自动填充
├── bean/
│   ├── EntityResp             # 实体响应Bean
│   ├── IdReq                  # ID请求Bean
│   ├── PageReq                # 分页请求Bean
│   ├── RemoveReq              # 删除请求Bean
│   ├── RestField              # REST接口字段结构描述
│   ├── RestInfo               # REST接口信息Bean
│   ├── RestParam              # REST接口参数元数据
│   └── UpdateReq              # 更新请求Bean
└── helper/
    ├── IpHelper               # IP地址工具
    ├── RequestHelper          # 请求工具
    ├── ResponseHelper         # 响应工具
    ├── RestHelper             # REST接口扫描工具
    └── LocalThreadHelper      # 线程上下文工具
```

## ErrorHandler — 全局异常处理

使用 `@RestControllerAdvice`，实现全局异常拦截与统一响应。

### 异常处理器映射

| 拦截异常类型                                 | HTTP状态码 | 返回内容                    |
|----------------------------------------|---------|-------------------------|
| HttpMediaTypeNotSupportedException     | 415     | 状态码+原因短语                |
| HttpRequestMethodNotSupportedException | 405     | 状态码+原因短语                |
| NoResourceFoundException               | 404     | 状态码+原因短语                |
| SQLSyntaxErrorException                | 500     | 状态码+原因短语（不暴露SQL细节）      |
| BadSqlGrammarException                 | 500     | 状态码+原因短语                |
| UncategorizedSQLException              | 500     | 状态码+原因短语                |
| MysqlDataTruncation                    | 500     | 状态码+原因短语                |
| CommonException                        | 500     | code=-1, 业务异常消息         |
| Exception（兜底）                          | 500     | 提取CommonException或安全化消息 |

### 核心处理逻辑

1. **CommonException提取**：沿异常链向下查找最多3层，寻找CommonException（Spring事务异常包装的业务异常也能识别）
2. **安全考虑**：兜底Exception处理器将null/空消息替换为"Internal Server Error"，避免暴露堆栈
3. **日志策略**：UserException仅打印biz error（不带堆栈），其他异常打印完整堆栈
4. **邮件告警**：非UserException触发HTML邮件告警（受SystemConfig.alarmEmailEnabled控制）
5. **线程上下文传递**：错误信息存入LocalThreadHelper（key=REQUEST_ERROR）

### 异常处理流程

```
Controller抛出异常
  → ErrorHandler拦截
  → 精确匹配异常类型 → 返回对应R.error()
  → 兜底Exception处理
    → 从异常链提取CommonException(最多3层)
    → 找到 → R.error(commonException)
    → 未找到 → 安全化message → R.error(message)
  → 统一日志处理
  → UserException: log.error("biz error: ...")
  → 其他: log.error("sys request: ...", e) + 邮件告警
  → LocalThreadHelper.set(REQUEST_ERROR, errorMsg)
  → 返回R<T>统一响应
```

## UserNameBodyAdvice — 用户名自动填充

响应体自动填充 `createByName` 和 `updateByName` 字段，基于 `UserNameProvider` SPI 接口。

### 工作原理

```java
// 实现 UserNameProvider 接口
@Component
public class MyUserNameProvider implements UserNameProvider {
    @Override
    public Map<String, String> getNamesByUserCodes(Set<String> userCodes) {
        // 从数据库或缓存查询用户编码到姓名的映射
        return userService.getUserNamesByCodes(userCodes);
    }
}
```

### 核心特性

1. **自动扫描**：通过 SpringContextHolder 自动发现 UserNameProvider Bean
2. **递归收集**：深度遍历响应体（最多8层），收集所有 BaseEntity 对象
3. **字段缓存**：Class 字段反射结果缓存，提升性能
4. **并发安全**：使用 ConcurrentHashMap 和双重检查锁
5. **容错处理**：填充失败仅打印 warn 日志，不影响正常响应

### 支持的数据结构

- `BaseEntity` 对象及其子类
- `R<T>` 包装的响应
- `List/Set/Iterable` 集合
- 数组
- Map（递归处理 value）
- 自定义对象（递归处理字段）

## 校验注解

### @AtLeastOneNotNull

类级校验注解，确保指定的多个字段中至少有一个不为 null。

```java
@Data
@Schema(description = "删除请求")
@AtLeastOneNotNull(fields = {"id", "ids"}, message = "id 或 ids 必须填写其中一个")
public class RemoveReq implements Serializable {
    @Schema(description = "主键ID（与 ids 二选一）")
    private Long id;
    @Schema(description = "主键ID清单（与 id 二选一）")
    private List<Long> ids;
}
```

## 标准 Request/Response Bean

### IdReq - ID请求

```java
@Data
@Schema(description = "ID请求")
public class IdReq implements Serializable {

    @NotNull(message = "主键ID不能为空")
    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
}
```

### PageReq - 分页请求

实现 `Pageable` 接口，`init()` 方法校验并计算 offset。

```java
@Data
@Slf4j
@Schema(description = "分页请求")
public class PageReq implements Pageable, Serializable {

    @Schema(description = "分页页码")
    private Long current;

    @Schema(description = "分页大小")
    private Long size;

    @Schema(description = "偏移量", hidden = true)
    private Long offset;

    @Override
    public void init() {
        Long current = getCurrent();
        if (current == null || current < 1) {
            log.debug("分页参数 current 为空或非法值: {}, 设置为默认值: {}", current, DEFAULT_CURRENT);
            setCurrent(DEFAULT_CURRENT);
        }

        Long size = getSize();
        if (size == null || size < 1) {
            log.debug("分页参数 size 为空或非法值: {}, 设置为默认值: {}", size, DEFAULT_SIZE);
            setSize(DEFAULT_SIZE);
        }

        // 计算偏移量：(current - 1) * size
        long offset = (getCurrent() - 1) * getSize();
        setOffset(offset);
        log.debug("分页参数初始化完成, current: {}, size: {}, offset: {}", getCurrent(), getSize(), offset);
    }
}
```

### RemoveReq - 删除请求

类级 `@AtLeastOneNotNull` 校验，字段上**无** `@NotNull` 注解（避免冲突）。

```java
@Data
@Schema(description = "删除请求")
@AtLeastOneNotNull(fields = {"id", "ids"}, message = "id 或 ids 必须填写其中一个")
public class RemoveReq implements Serializable {

    @Schema(description = "主键ID（与 ids 二选一）")
    private Long id;

    @Schema(description = "主键ID清单（与 id 二选一）")
    private List<Long> ids;
}
```

### UpdateReq - 更新请求

```java
@Data
@Schema(description = "更新请求")
public class UpdateReq implements Serializable {

    @NotNull(message = "主键ID不能为空")
    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotNull(message = "数据版本version不能为空")
    @Schema(description = "数据版本", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer version;
}
```

### EntityResp - 实体响应

`createByName` / `updateByName` 由 `UserNameBodyAdvice` 通过 SPI 自动填充。

```java
@Data
@Schema(description = "实体返回")
public class EntityResp implements Serializable {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人code")
    private String createBy;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "更新人code")
    private String updateBy;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "数据版本")
    private Integer version;

    @Schema(description = "创建人姓名")
    private String createByName;

    @Schema(description = "更新人姓名")
    private String updateByName;
}
```

## REST 接口元数据 Bean

由 `RestHelper` 扫描生成，描述 REST 接口的完整元数据，包括接口信息、参数元数据和复杂类型的字段结构。

### RestInfo - REST 接口信息 Bean

REST 接口信息 Bean，由 `RestHelper` 扫描生成，包含接口的完整元数据（类、URI、方法、参数、返回类型、Swagger 注解信息等）。

```java
@Data
public class RestInfo implements Serializable {
    private Class<?> clazz;
    private String appCode;
    private String code;
    private String module;
    private String method;
    private String uri;
    private String name;
    private String desc;
    private Integer writeFlag;
    private List<RestParam> parameters;      // 接口参数列表
    private String returnType;               // 返回类型（完整类名）
    private String returnGenericInfo;        // 返回类型泛型信息（JSON 格式）
    private String tag;                      // 类级别 @Tag 描述
    private String operationSummary;         // 方法级别 @Operation(summary)
    private String operationDescription;     // 方法级别 @Operation(description)
    private Boolean deprecated;              // 接口是否废弃 @Operation(deprecated)
    private String returnSchema;             // 返回值完整结构（JSON 格式，包含字段注释、示例值）
    private String[] consumes;               // 请求 Content-Type（@RequestMapping.consumes）
    private String[] produces;               // 响应 Content-Type（@RequestMapping.produces）
}
```

### RestParam - REST 接口参数元数据

REST 接口参数元数据，描述每个参数的名称、类型、注解、是否必需、默认值、泛型信息和字段结构。

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestParam implements Serializable {
    private String name;                     // 参数名称
    private String type;                     // 参数类型（完整类名）
    private String annotationType;           // 参数注解类型（如 RequestBody、PathVariable、RequestParam）
    private Boolean required;                // 是否必需
    private String defaultValue;             // 参数默认值（仅 @RequestParam 支持）
    private List<String> genericTypes;       // 泛型参数类型列表（用于复杂类型）
    private String description;              // 参数描述（来自 @Schema.description）
    private String example;                  // 参数示例值（来自 @Schema.example）
    private String requiredMode;             // 参数必填模式（来自 @Schema.requiredMode）
    private List<RestField> fields;          // 复杂参数类型的字段结构列表（递归扫描）
}
```

### RestField - REST 接口字段结构描述

REST 接口字段结构描述，用于描述复杂类型的字段结构，支持递归嵌套（`fields`）、泛型参数（`genericTypes`）、自引用检测（
`selfReferencing`）和简单类型标记（`simpleType`）。

```java
@Data
public class RestField implements Serializable {
    private String name;                     // 字段名称
    private String type;                     // 字段类型（完整类名，简单类型如 java.lang.String 或复杂类型类名）
    private String description;              // 字段描述（来自 @Schema.description）
    private String example;                  // 示例值（来自 @Schema.example）
    private Boolean required;                // 是否必填（来自 @Schema.requiredMode）
    private List<String> genericTypes;       // 泛型参数类型列表（如 List<String> 中的 String）
    private List<RestField> fields;          // 子字段（如果是非简单类型，递归扫描其字段）
    private Boolean selfReferencing;         // 是否为自引用类型（用于防止无限递归）
    private Boolean simpleType;              // 是否为简单类型

    public RestField() {
        this.genericTypes = new ArrayList<>();
        this.fields = new ArrayList<>();
    }
}
```

## IpHelper — IP地址工具

```java
// 获取客户端真实IP（穿透代理链，按优先级解析）
// X-Forwarded-For → Proxy-Client-IP → WL-Proxy-Client-IP → remoteAddr
String ip = IpHelper.getOriginIp(request);

// 获取上游IP（TCP直连IP，可能是代理服务器）
String ip = IpHelper.getUpstreamIp(request);
```

**特殊处理**：127.0.0.1/IPv6回环地址时，通过 `InetAddress.getLocalHost()` 获取本机真实IP。多级代理取X-Forwarded-For第一个IP。

## RequestHelper — 请求工具

| 方法                              | 说明                                                                |
|---------------------------------|-------------------------------------------------------------------|
| `match(rule, uri)`              | Ant风格URL匹配（支持*、**、?）                                              |
| `getIdsFromBaseModel(entity)`   | 从BaseEntity提取ID列表（合并ids集合和id字段）                                   |
| `getParamsFromRequest(req)`     | 请求参数转Map（多值逗号拼接）                                                  |
| `getRequestUrl()`               | 获取当前请求完整URL                                                       |
| `getRequest()`                  | 获取当前HttpServletRequest（双源：RequestContextHolder→LocalThreadHelper） |
| `getFrontDomain(req)`           | 获取前端域名（Origin→Referer→当前URL）                                      |
| `getFrontPortalDomainPort(req)` | 组装前端完整地址（协议+域名+端口，自动省略默认端口）                                       |

## ResponseHelper — 响应工具

```java
// 写出JSON错误响应（清除requestTime/responseTime/costTime）
ResponseHelper.responseError(response, R.error("异常信息"));

// Excel文件下载（RFC 5987中文文件名，8KB缓冲区流式写入）
ResponseHelper.responseExcel(response, file);
ResponseHelper.responseExcel(response, "/path/to/file.xlsx");
```

## RestHelper — REST接口扫描

运行时扫描指定包下的Controller类，提取接口元数据生成 `RestInfo` 列表。

```java
// 扫描默认包（com.wkclz）下所有接口
List<RestInfo> rests = RestHelper.getMapping();

// 扫描指定包
List<RestInfo> rests = RestHelper.getMapping("com.wkclz.demo");

// 带appCode和过滤器
List<RestInfo> rests = RestHelper.getMapping("com.wkclz.demo", "MY_APP", uri -> uri.startsWith("/api"));

// 返回JSON字符串
String json = RestHelper.getMappingStr("com.wkclz.demo");
```

**RestInfo字段**：详见上方「REST 接口元数据 Bean」章节（共 19 个字段，含 parameters、returnType、returnSchema、consumes、produces
等）

**扫描功能**：

- 读取类级别和方法级别@RequestMapping/@GetMapping/@PostMapping/@PutMapping/@DeleteMapping
- 从@Operation.summary获取接口描述
- 扫描@Router注解补充uri与描述映射
- URI以/public开头设置writeFlag=1（公开），否则writeFlag=0（需鉴权）

## LocalThreadHelper — 线程上下文

基于 `ThreadLocal<ConcurrentHashMap<String, Object>>` 的多Key泛型线程上下文工具。

```java
// 设置
LocalThreadHelper.set("key", value);

// 获取
MyType val = LocalThreadHelper.get("key");

// 获取（带默认值）
MyType val = LocalThreadHelper.getOrElse("key", () -> defaultValue);

// 检查key
boolean exists = LocalThreadHelper.contains("key");

// 删除指定key
LocalThreadHelper.remove("key");

// 清除所有（必须在请求结束时调用，防内存泄漏！）
LocalThreadHelper.clear();
```

**模块内使用场景**：

- ErrorHandler：存入错误信息(key=REQUEST_ERROR)和请求日志(key=REQUEST_LOG)
- RequestHelper：作为getRequest()的回退源(key=HttpServletRequest.class.getName())
- 外部Filter：存入请求对象、请求日志等，请求结束时调用clear()

## 自动配置

`ShWebAutoConfig`：@AutoConfiguration + @ComponentScan("com.wkclz.web")

引入 sh-web 依赖后，ErrorHandler、UserNameBodyAdvice 等组件自动生效。

**依赖**：sh-spring, spring-boot-starter-web, spring-boot-starter-actuator, mysql-connector-j(optional), spring-jdbc(
optional)

---

## API 设计参考

> 来源: [API 规范](https://doc.wkclz.com/backend/standard-backend/api.html)，结合 sh-framework Web 层代码整理

### 接口命名规范

#### 请求方法

- 只能定义 **GET**、**POST** 请求
    - **GET**：用于读取数据，天然幂等
    - **POST**：用于写入逻辑，需要考虑幂等

#### URI 规范

| 规则  | 说明                        |
|-----|---------------------------|
| 自解释 | 从接口命名即可猜测接口用途             |
| 一级  | 后端模块名，如 `sys`、`iam`、`cms` |
| 免鉴权 | 二级为 `public`              |
| 后缀  | 按功能使用标准后缀（见下表）            |

**URI 后缀规范**：

| 后缀         | 含义                 | 请求方法 | 对应框架Bean                  |
|------------|--------------------|------|---------------------------|
| `/page`    | 分页查询               | GET  | `PageReq` → `PageData<T>` |
| `/list`    | 列表查询               | GET  | —                         |
| `/info`    | 详情查询               | GET  | `IdReq` → `EntityResp`    |
| `/save`    | 保存（新增修改一体，依赖于id判断） | POST | —                         |
| `/create`  | 新增（仅有新增需求时）        | POST | `XxxCreateReq`            |
| `/update`  | 修改（仅有修改需求时）        | POST | `UpdateReq` + 业务字段        |
| `/remove`  | 删除（需要考虑单个及批量）      | POST | `RemoveReq`               |
| `/options` | 列表查询（信息简洁，用于下拉菜单）  | GET  | —                         |

**URI 示例**：

| 操作        | URI                               | 方法   |
|-----------|-----------------------------------|------|
| 获取用户分页    | GET `/iam/user/page`              | GET  |
| 修改用户信息    | POST `/iam/user/update`           | POST |
| 登录获取图片验证码 | GET `/iam/public/picture/captcha` | GET  |

> 去除一级、public 级和后缀，其余部分可视为接口组。

#### 框架中的 Route 定义范式

框架使用 `@Router` 注解在 Route 接口中集中定义 URI 常量：

```java
@Router(module = Route.PREFIX, prefix = Route.PREFIX)
public interface Route {
    String PREFIX = "/sh-demo";

    String USER_PAGE = "/user/page";
    String USER_INFO = "/user/info";
    String USER_CREATE = "/user/create";
    String USER_UPDATE = "/user/update";
    String USER_REMOVE = "/user/remove";
}
```

Controller 通过 `@RequestMapping(Route.PREFIX)` + `@GetMapping(Route.USER_PAGE)` 引用 URI 常量，确保路径集中管理。

### 接口参数规范

若接口不支持对应参数，可忽略。

| 参数名称     | 参数含义             | 类型                  | 默认值     | 来源                                       |
|----------|------------------|---------------------|---------|------------------------------------------|
| token    | [header] 用户token | String              | —       | 请求头 Authorization                        |
| current  | 页码               | Integer             | 1       | `PageReq.current` / `BaseEntity.current` |
| size     | 每页数              | Integer             | 10      | `PageReq.size` / `BaseEntity.size`       |
| orderBy  | 排序字段             | String              | id desc | `BaseEntity.orderBy`                     |
| timeFrom | 时间范围开始           | yyyy-MM-dd HH:mm:ss | —       | `BaseEntity.timeFrom`                    |
| timeTo   | 时间范围结束           | yyyy-MM-dd HH:mm:ss | —       | `BaseEntity.timeTo`                      |

**框架分页参数映射**：

```
前端参数(camelCase)     Java字段              所属类
─────────────────────────────────────────────────────
current              → current             → PageReq / BaseEntity
size                 → size                → PageReq / BaseEntity
orderBy              → orderBy             → BaseEntity
timeFrom             → timeFrom            → BaseEntity
timeTo               → timeTo              → BaseEntity
keyword              → keyword             → BaseEntity(模糊查询)
```

### 接口返回值规范

框架通过 `R<T>` 统一响应类实现标准化：

```java
@Data
public class R<T> implements Serializable {
    private int code;              // 状态码
    private String msg;            // 提示信息
    private T data;                // 业务数据
    private LocalDateTime requestTime;   // 请求进入Controller的时间
    private LocalDateTime responseTime;  // Controller设置结果的时间
    private Long costTime;         // Controller内消耗时间(毫秒)
}
```

**返回值字段说明**：

| 参数名称         | 参数值/类型 | 参数含义                        |
|--------------|--------|-----------------------------|
| code         | `-1`   | 系统错误，需要开发人员处理               |
| code         | `0`    | 业务提示，只需反馈给页面                |
| code         | `1`    | 功能无异常，有业务状态返回（框架中对应 200）    |
| code         | `> 1`  | 请看 code 对照表                 |
| msg          | 异常消息   | code 不为 1 时有返回              |
| requestTime  | 时间     | 请求进入 Controller 的时间         |
| responseTime | 时间     | 请求在 Controller 进行 set 结果的时间 |
| costTime     | Long   | 在 Controller 内消耗的时间，单位毫秒    |
| data         | 详情     | 正常返回时为业务数据（无具体业务数据时返回 true） |

**分页查询额外返回（data 内的 PageData 结构）**：

| 参数名称    | 类型    | 含义      |
|---------|-------|---------|
| current | Long  | 页码      |
| size    | Long  | 每页数     |
| offset  | Long  | 偏移量     |
| total   | Long  | 总数据条数   |
| count   | Long  | 统计数     |
| records | Array | 具体的业务数据 |

**框架创建统一响应的方式**：

```java
R.ok(data)                          // 成功：code=200, msg="Success"
R.error("异常消息")                  // 系统错误：code=500
R.error(commonException)            // 业务异常：code=exception.code
R.warn("提示消息")                   // 业务提示：code=0
```

### 返回码 (ResultCode) 对照表

框架在 `ResultCode` 枚举中定义了标准 code，与 API 规范完全对应：

| code                       | 枚举值                               | 提示语                         | 说明                   |
|----------------------------|-----------------------------------|-----------------------------|----------------------|
| **HTTP 标准**                |                                   |                             |                      |
| 200                        | `SUCCESS`                         | Success                     | 请求成功                 |
| 400                        | `VALIDATION_ERROR`                | Parameter validation error  | 参数校验失败               |
| 401                        | `UNAUTHORIZED`                    | Unauthorized                | 未授权                  |
| 403                        | `FORBIDDEN`                       | Forbidden                   | 禁止访问                 |
| 404                        | `NOT_FOUND`                       | Resource Not Found          | 资源不存在                |
| 500                        | `ERROR`                           | Internal Server Error       | 系统错误                 |
| **Token/登录 (10001-10102)** |                                   |                             |                      |
| 10001                      | `TOKEN_NULL`                      | token 为空！                   | 需要 header 附带正确 token |
| 10002                      | `TOKEN_ERROR`                     | token 不正确或已失效！              | token 不正确或已失效        |
| 10003                      | `TOKEN_ILLEGAL_TRANSFER`          | 非法传输 token！                 | token 放在了不合理的位置      |
| 10004                      | `TOKEN_ILLEGAL_LENGTH`            | 非法长度的 token！                | token 出现干扰字符         |
| 10005                      | `TOKEN_SIGN_FAILED`               | token 签名校验失败！               | token 内容被修改          |
| 10006                      | `TOKEN_NOT_RIGHT`                 | token 签发者不正确！               | 确认 token 来源          |
| 10007                      | `LOGIN_TIMEOUT`                   | 登录已失效，请重新登录！                | —                    |
| 10009                      | `LOGIN_FORCE_TIMEOUT`             | 登录时间过长，强制失效！                | —                    |
| 10101                      | `APP_CODE_NULL`                   | 无法识别应用编码！                   | 设置请求头或应用域名           |
| 10102                      | `TENANT_NULL`                     | 无法识别租户编码！                   | 设置请求头或租户域名           |
| **跨域/路由 (20001-20004)**    |                                   |                             |                      |
| 20001                      | `CLIENT_CHANGE`                   | 用户登录环境改变！                   | 终端改变，需重新登录           |
| 20002                      | `API_CORS`                        | api url can not be cors     | 接口地址不被允许             |
| 20003                      | `ORIGIN_CORS`                     | origin url can not be cors  | 前端域名不被允许             |
| 20004                      | `ERROR_ROUTER`                    | err routers, check the uri! | 错误的路由                |
| **登录/验证码 (30001-30005)**   |                                   |                             |                      |
| 30001                      | `USERNAME_PASSWORD_ERROR`         | 登录名或密码错误                    | 用户名密码错误使用同一提示        |
| 30002                      | `CAPTCHA_ERROR`                   | 图片验证码错误                     | —                    |
| 30003                      | `CAPTCHA_NEED`                    | 需要图片验证码                     | —                    |
| 30004                      | `MOBILE_CAPTCHA_ERROR`            | 验证码错误                       | 手机验证码                |
| 30005                      | `EMAIL_CAPTCHA_ERROR`             | 验证码错误                       | 邮箱验证码                |
| **数据操作 (40001-40006)**     |                                   |                             |                      |
| 40001                      | `UPDATE_NO_VERSION`               | 操作需要带数据版本号！                 | 需提交 version 字段       |
| 40002                      | `RECORD_NOT_EXIST_OR_OUT_OF_DATE` | 数据不存在或已不是最新的！               | id 不正确或已被更新          |
| 40003                      | `RECORD_NOT_EXIST`                | 数据不存在！                      | —                    |
| 40004                      | `PARAM_NO_ID`                     | ID 不存在！                     | 需要带 id 操作            |
| 40005                      | `PARAM_NULL`                      | 参数不存在！                      | —                    |
| 40006                      | `RECORD_DUPLICATE`                | 数据重复，唯一性校验失败！               | —                    |
| **网络 (50001-50003)**       |                                   |                             |                      |
| 50001                      | `NETWORK_ERROR`                   | network error！              | —                    |
| 50002                      | `NO_AVAILABLE_SERVER`             | no available server！        | —                    |
| 50003                      | `UNKNOWN_RIBBON_ERROR`            | unknown ribbon error！       | —                    |
| **订单 (60001-60003)**       |                                   |                             |                      |
| 60001                      | `ORDER_TIMEOUT`                   | 订单支付超时已自动取消，请重新下单！          | —                    |
| 60002                      | `ORDER_PAYED`                     | 订单已完成支付，请不要重复支付！            | —                    |
| 60003                      | `ORDER_ERROR`                     | 订单状态异常，不能支付                 | —                    |

### 权限校验

- 所有需要验证权限的接口，都需要验证 token，需要在 header 中传值
- 对于只能使用一次的 token，请从临时 token 接口获取。临时 token 只能获取一次，只能 `?` 传参
- 后端确定前端为哪个站点，通过 headers 里面的 `Origin` 获取，可选在 `?` 上附加此参数

### 标准 CRUD 接口范式（框架实践）

```java
@Tag(name = "1.用户管理", description = "用户增删改查接口")
@RestController
@RequestMapping(Route.PREFIX)
public class UserRest {

    @Autowired
    private UserService userService;

    // 分页查询 — GET + PageReq → PageData
    @GetMapping(Route.USER_PAGE)
    public R<PageData<UserPageResp>> userPage(@Valid UserPageReq req) { ... }

    // 详情查询 — GET + IdReq → EntityResp
    @GetMapping(Route.USER_INFO)
    public R<UserResp> userInfo(@Valid IdReq req) { ... }

    // 新增 — POST + @RequestBody CreateReq → EntityResp
    @PostMapping(Route.USER_CREATE)
    public R<UserResp> userCreate(@Valid @RequestBody UserCreateReq req) { ... }

    // 更新 — POST + @RequestBody UpdateReq(含version) → Integer
    @PostMapping(Route.USER_UPDATE)
    public R<Integer> userUpdate(@Valid @RequestBody UserUpdateReq req) { ... }

    // 删除 — POST + @RequestBody RemoveReq → Integer
    @PostMapping(Route.USER_REMOVE)
    public R<Integer> userRemove(@Valid @RequestBody RemoveReq req) { ... }
}
```

**VO 分层规范**：

| VO 类型 | 命名             | 继承                       | 用途     |
|-------|----------------|--------------------------|--------|
| 分页请求  | `XxxPageReq`   | `PageReq`                | 分页查询参数 |
| 创建请求  | `XxxCreateReq` | — (Serializable)         | 新增业务字段 |
| 更新请求  | `XxxUpdateReq` | `UpdateReq`(含id+version) | 修改业务字段 |
| 分页响应  | `XxxPageResp`  | `EntityResp`             | 列表返回字段 |
| 详情响应  | `XxxResp`      | `EntityResp`             | 详情返回字段 |
