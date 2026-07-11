# micro-autotest 模块知识库

micro-autotest 是 sh-microapp 的自动化 REST API 测试模块，基于 Spring Boot 4.x，能在运行时扫描项目中的 RESTful 接口，实时生成测试用例并执行，自动 Mock 外部依赖（MyBatis Mapper、Redis、外部 API），最后生成 MD 和 HTML 格式的测试报告。

## 包结构

```
com.wkclz.auto
├── AutoTestAutoConfig              # 自动配置（@ComponentScan("com.wkclz.auto")）
├── bean/
│   ├── ApiInfo                     # 接口信息（controllerClass, method, uri, params, returnType）
│   ├── ApiParamInfo                # 参数信息（name, type, requestBody, pathVariable, requestParam）
│   ├── TestCaseResult              # 测试用例结果（uri, method, httpStatus, costTimeMs, errorMessage）
│   └── TestReport                  # 测试报告（startTime, endTime, totalApiCount, successCount, results）
├── scanner/
│   └── ApiScanner                  # 运行时接口扫描器
├── mock/
│   ├── TestDataGenerator           # 智能测试数据生成器（反射递归生成）
│   ├── AutoMockBeanPostProcessor   # Bean 级自动 Mock（BeanDefinitionRegistryPostProcessor）
│   └── MockHelper                  # Mock 辅助工具（控制器依赖 Mock + 参数值生成）
├── executor/
│   └── TestExecutor                # 测试执行器（Spring MockMvc）
├── report/
│   └── ReportGenerator             # 报告生成器（MD + HTML）
└── rest/
    ├── Route                       # 路由常量（/micro-autotest）
    └── AutoTestRest                # REST API 控制器
```

## REST API

| 端点 | 方法 | 说明 |
|------|------|------|
| `/micro-autotest/api/list` | GET | 扫描并列出所有 REST 接口（可选参数 `packagePath`） |
| `/micro-autotest/run` | POST | 执行自动化测试（可选参数 `packagePath`, `reportDir`） |
| `/micro-autotest/report` | GET | 获取最新测试报告（JSON） |
| `/micro-autotest/report/md` | GET | 获取最新测试报告（Markdown） |
| `/micro-autotest/report/html` | GET | 获取最新测试报告（HTML） |

## 核心组件

### ApiScanner — 运行时接口扫描器

- `scan()` — 扫描默认包（主类 @SpringBootApplication 所在包）
- `scan(String packagePath)` — 扫描指定包
- 使用 `ClassUtil.getClasses()` 获取包下所有类
- 过滤 `@RestController` / `@Controller` 注解的类
- 解析 `@RequestMapping` / `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping`
- 分析方法参数的 `@RequestBody` / `@PathVariable` / `@RequestParam` 注解
- 通过 `@Router` / `@Desc` / `@ApiDesc` 注解补充接口描述

### TestDataGenerator — 智能测试数据生成

| 类型 | 生成值 |
|------|--------|
| boolean / Boolean | true |
| byte / short / int / long | 1 / 1 / 1 / 1L |
| float / double | 1.0f / 1.0 |
| char / Character | 'a' |
| String | "" |
| LocalDateTime / LocalDate / LocalTime | now() |
| Date | new Date() |
| Enum | enumConstants[0] |
| Array | 空数组 |
| List / Set / Map | 空集合 |
| POJO | 递归反射构造，深度限制 3 层 |

跳过字段：`id`, `version`, `deleted`, `serialVersionUID`，以及 static/final 字段。

### AutoMockBeanPostProcessor — Bean 级自动 Mock

- 实现 `BeanDefinitionRegistryPostProcessor`
- 需通过 `AutoMockBeanPostProcessor.enableMock()` 手动开启
- 开启后自动将 `.mapper.` 包下的 Bean 替换为 Mockito mock
- 使用 `GenericBeanDefinition` + `setInstanceSupplier(() -> Mockito.mock(beanClass))`

### MockHelper — Mock 辅助工具

- `mockControllerDependencies(Class<?>)` — 查找控制器字段的 Spring Bean 并创建 Mockito mock
- `generateParamValue(ApiParamInfo)` — 委托 TestDataGenerator 生成参数值
- `resetAll()` — 重置所有活跃 mock

### TestExecutor — 测试执行器

- 使用 `MockMvcBuilders.webAppContextSetup(webApplicationContext).build()` 构建 MockMvc
- 对每个 ApiInfo：Mock 依赖 → 构建请求 → 执行 → 记录结果 → 重置 Mock
- GET 请求：递归展开 POJO 字段为查询参数（`field.subField` 格式），深度限制 3 层
- POST/PUT 请求：`@RequestBody` 参数序列化为 JSON，使用 `JSONWriter.Feature.WriteNulls`
- 结果分类：success(2xx), fail(4xx), error(5xx)

### ReportGenerator — 报告生成器

- `generateMd(TestReport)` — Markdown 格式报告（Summary 表格 + Details 表格 + Failed Cases 详情）
- `generateHtml(TestReport)` — HTML 格式报告（CSS 内联，响应式布局，颜色编码）
- `saveReport(TestReport, String dirPath)` — 保存 `autotest_report_yyyyMMdd_HHmmss.md` 和 `.html` 到指定目录

## 自动配置

- `AutoTestAutoConfig`：@ComponentScan("com.wkclz.auto")
- 注册文件：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- 内容：`com.wkclz.auto.AutoTestAutoConfig`

## 依赖

| 依赖 | 用途 |
|------|------|
| sh-web | RestHelper / RestInfo 基础 |
| sh-mybatis | MyBatis Mapper 自动 Mock |
| sh-redis | Redis 自动 Mock |
| spring-test | MockMvc 运行时支持 |
| mockito-core | Mockito 运行时 Mock（compile scope） |
| spring-boot-starter-test | 测试框架（test scope） |
| wiremock-standalone | 外部 HTTP API Mock |

## 使用范式

```java
// 1. 引入依赖
// <dependency>
//     <groupId>com.wkclz.microapp</groupId>
//     <artifactId>micro-autotest</artifactId>
// </dependency>

// 2. 启动应用后，调用 REST API
// GET  /micro-autotest/api/list              — 查看扫描到的接口
// POST /micro-autotest/run                   — 执行测试
// POST /micro-autotest/run?reportDir=/tmp    — 执行测试并保存报告
// GET  /micro-autotest/report                — 获取 JSON 报告
// GET  /micro-autotest/report/md             — 获取 Markdown 报告
// GET  /micro-autotest/report/html           — 获取 HTML 报告

// 3. 如需在容器启动时自动 Mock Mapper，在启动前调用
// AutoMockBeanPostProcessor.enableMock();
```
