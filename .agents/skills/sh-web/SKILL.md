---
name: "sh-web"
description: "sh-framework Web模块知识库。包含ErrorHandler全局异常处理(8种异常+兜底+邮件告警)、IpHelper IP解析、RequestHelper请求工具、ResponseHelper响应工具、RestHelper接口扫描、LocalThreadHelper线程上下文。当涉及全局异常处理、IP获取、请求响应工具、REST接口元数据扫描时调用。"
---

# sh-web 模块知识库

sh-web 是 sh-framework 的 Web 层基础设施模块，提供全局异常处理、IP 地址工具、请求/响应工具、REST 接口扫描和线程上下文管理。

## 包结构

```
com.wkclz.web
├── ShWebAutoConfig            # 自动配置
├── rest/
│   └── ErrorHandler           # 全局异常处理器（@RestControllerAdvice）
├── bean/
│   └── RestInfo               # REST接口信息Bean
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

| 拦截异常类型 | HTTP状态码 | 返回内容 |
|-------------|-----------|---------|
| HttpMediaTypeNotSupportedException | 415 | 状态码+原因短语 |
| HttpRequestMethodNotSupportedException | 405 | 状态码+原因短语 |
| NoResourceFoundException | 404 | 状态码+原因短语 |
| SQLSyntaxErrorException | 500 | 状态码+原因短语（不暴露SQL细节） |
| BadSqlGrammarException | 500 | 状态码+原因短语 |
| UncategorizedSQLException | 500 | 状态码+原因短语 |
| MysqlDataTruncation | 500 | 状态码+原因短语 |
| CommonException | 500 | code=-1, 业务异常消息 |
| Exception（兜底） | 500 | 提取CommonException或安全化消息 |

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

| 方法 | 说明 |
|------|------|
| `match(rule, uri)` | Ant风格URL匹配（支持*、**、?） |
| `getIdsFromBaseModel(entity)` | 从BaseEntity提取ID列表（合并ids集合和id字段） |
| `getParamsFromRequest(req)` | 请求参数转Map（多值逗号拼接） |
| `getRequestUrl()` | 获取当前请求完整URL |
| `getRequest()` | 获取当前HttpServletRequest（双源：RequestContextHolder→LocalThreadHelper） |
| `getFrontDomain(req)` | 获取前端域名（Origin→Referer→当前URL） |
| `getFrontPortalDomainPort(req)` | 组装前端完整地址（协议+域名+端口，自动省略默认端口） |

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

**RestInfo字段**：clazz, appCode, code, module, method, uri, name, desc, writeFlag

**扫描功能**：
- 读取类级别和方法级别@RequestMapping/@GetMapping/@PostMapping/@PutMapping/@DeleteMapping
- 从@Desc和@ApiDesc注解获取接口描述
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

引入 sh-web 依赖后，ErrorHandler 等组件自动生效。

**依赖**：sh-spring, spring-boot-starter-web, spring-boot-starter-actuator, mysql-connector-j(optional), spring-jdbc(optional)
