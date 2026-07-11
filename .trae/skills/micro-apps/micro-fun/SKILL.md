---
name: "micro-fun"
description: "函数管理模块，支持函数分类树形管理、多语言脚本引擎(JavaScript/Python/Groovy/QLExpress/Ruby)动态执行，适用于函数定义CRUD、脚本在线测试与运行时调用场景"
---

# Micro-Fun 模块

## 1. 适用场景

当用户需要以下操作时触发此 skill：

- 对 `com.wkclz.micro.fun` 包下的代码进行增删改查
- 实现函数分类（FunCategory）的树形管理、创建、修改、删除
- 实现函数体（FunFunction）的 CRUD、分页查询、选项查询
- 开发或调试多语言脚本引擎（JavaScript / Python / Groovy / QLExpress / Ruby）
- 通过 `ScriptService` 在运行时动态执行脚本函数
- 新增脚本语言引擎实现
- 排查函数执行、脚本引擎相关问题

---

## 2. 架构概览

```
┌──────────────────────────────────────────────────────────────────┐
│                        REST 层                                    │
│  FunCategoryRest (/micro-fun/category/*)                         │
│  FunFunctionRest (/micro-fun/function/*)                         │
└──────────┬───────────────────────────────┬───────────────────────┘
           │                               │
           ▼                               ▼
┌──────────────────────┐    ┌──────────────────────────────────────┐
│    Service 层         │    │         Engine 层                     │
│ FunCategoryService   │    │  ScriptService (引擎路由 + 缓存)       │
│   ├─ 分类树构建       │    │    ├─ JavaScriptEngineImpl (GraalVM)  │
│   ├─ 父子级校验       │    │    ├─ PythonEngineImpl (GraalVM)     │
│   └─ 关联删除检查     │    │    ├─ RubyEngineImpl (GraalVM)       │
│                      │    │    ├─ GroovyEngineImpl (GroovyShell)  │
│ FunFunctionService   │    │    ├─ QLExpressEngineImpl (QLExpress) │
│   ├─ 分页查询        │    │    └─ NonEngineImpl (空实现占位)       │
│   ├─ 唯一性校验       │    │                                       │
│   └─ funCode 查询    │    │  ScriptEngine (抽象基类)               │
└──────────┬───────────┘    └──────────────────────────────────────┘
           │
           ▼
┌──────────────────────┐
│ Mapper 层            │
│ FunCategoryMapper    │
│ FunFunctionMapper    │
└──────────────────────┘
```

---

## 3. 核心组件速查

### 实体类

| 类名 | 表名 | 说明 |
|------|------|------|
| `FunCategory` | `fun_category` | 函数分类实体，extends BaseEntity。字段：`pcode`(父类Code, "0"为顶级)、`categoryCode`、`categoryName`、`description`、`visible` |
| `FunFunction` | `fun_function` | 函数体实体，extends BaseEntity。字段：`categoryCode`、`funCode`、`funName`、`funParams`、`funLanguage`、`funBody`、`funReturn`、`funDesc`、`funMockData`、`visible`、`defaultFlag` |

### DTO 类

| 类名 | 继承 | 扩展字段 | 说明 |
|------|------|----------|------|
| `FunCategoryDto` | `FunCategory` | `children: List<FunCategoryDto>` | 分类树节点，含子级列表 |
| `FunFunctionDto` | `FunFunction` | `param: String` | 函数执行参数，用于测试接口传参 |

### Mapper 接口

| 类名 | 继承 | 自定义方法 |
|------|------|-----------|
| `FunCategoryMapper` | `BaseMapper<FunCategory>` | `getFunCategoryList(entity)`、`getFunCategoryOptions(entity)` |
| `FunFunctionMapper` | `BaseMapper<FunFunction>` | `getFunctionList(dto)`、`getFunctionOption(dto)` |

### Service 类

| 类名 | 继承 | 核心方法 |
|------|------|----------|
| `FunCategoryService` | `BaseService<FunCategory, FunCategoryMapper>` | `getFunCategoryList()`、`getFunCategoryTree()`、`getFunCategoryOptions()`、`create()`、`update()`、`customDelete()` |
| `FunFunctionService` | `BaseService<FunFunction, FunFunctionMapper>` | `getFunctionPage()`、`getFunction(funCode)`、`create()`、`update()`、`getFunctionOption()` |

### 引擎体系

| 类名 | 语言 | 执行引擎 | 说明 |
|------|------|----------|------|
| `ScriptEngine` | — | — | 抽象基类，持有 funLanguage/funCode/funParams/funBody/funReturn/script，定义 `exec(param)` |
| `JavaScriptEngineImpl` | JavaScript | GraalVM Polyglot Context | 用 `function %s(%s){ %s }` 模板生成脚本，通过 `context.getBindings("js")` 执行 |
| `PythonEngineImpl` | Python | GraalVM Polyglot Context | 用 `def %s(%s): %s` 模板，通过 `context.getBindings("python")` 执行 |
| `RubyEngineImpl` | Ruby | GraalVM Polyglot Context | 用 `def %s(%s) %s end` 模板，通过 `context.getBindings("ruby")` 执行 |
| `GroovyEngineImpl` | Groovy | GroovyShell | 直接解析 funBody，通过 `Binding.setVariable` 传参，`script.run()` 执行 |
| `QLExpressEngineImpl` | QLExpress | ExpressRunner | 用 `ExpressRunner.execute(funBody, context, ...)` 执行，参数放入 DefaultContext |
| `NonEngineImpl` | — | — | 空实现占位，当 funCode 查不到函数时使用 |
| `GraalvmLanguage` | — | — | 工具类，列出 GraalVM 可用语言 |
| `ScriptService` | — | — | 引擎路由器，ConcurrentMap 缓存引擎实例，按 funLanguage 分发到对应实现 |

### REST 控制器

| 类名 | 路径前缀 | 说明 |
|------|----------|------|
| `FunCategoryRest` | `/micro-fun` | 分类 CRUD + 树 + 选项 |
| `FunFunctionRest` | `/micro-fun` | 函数体 CRUD + 分页 + 选项 + 测试执行 |

---

## 4. 核心工作流

### 4.1 函数分类树形管理

分类通过 `pcode` 字段构建树，`pcode="0"` 为顶级节点。

```java
// 创建分类（categoryCode 由 RedisIdGenerator 自动生成，前缀 "ctg_"）
FunCategory category = new FunCategory();
category.setPcode("0");
category.setCategoryName("数学函数");
category.setVisible(1);
FunCategory created = funCategoryService.create(category);

// 获取分类树
FunCategory param = new FunCategory();
param.setVisible(1);
List<FunCategoryDto> tree = funCategoryService.getFunCategoryTree(param);
```

**更新分类时的父子级校验**：`checPcode()` 递归检查，不允许将自身或自身子节点设为新父级。

**删除分类时的关联检查**：`customDelete()` 检查是否有 FunFunction 关联此 categoryCode，有则拒绝删除。

### 4.2 函数体 CRUD

```java
// 创建函数
FunFunction fun = new FunFunction();
fun.setCategoryCode("ctg_xxx");
fun.setFunCode("add");
fun.setFunName("加法");
fun.setFunParams("a, b");
fun.setFunLanguage("JavaScript");
fun.setFunBody("return Number(a) + Number(b);");
fun.setFunReturn("Integer");
FunFunction created = funFunctionService.create(fun);

// 分页查询
FunFunctionDto dto = new FunFunctionDto();
dto.setCategoryCode("ctg_xxx");
dto.setFunLanguage("JavaScript");
PageData<FunFunctionDto> page = funFunctionService.getFunctionPage(dto);
```

**唯一性校验**：`duplicateCheck()` 按 `funCode` 检查重复。

### 4.3 脚本引擎执行

`ScriptService` 是核心入口，通过 `ConcurrentMap<String, ScriptEngine>` 缓存引擎实例。

```java
// 运行时按 funCode 获取引擎并执行
@Autowired
private ScriptService scriptService;

ScriptEngine engine = scriptService.getEngine("add");
Object result = engine.exec("1,2");

// 测试接口（不依赖数据库，直接传入函数定义执行）
FunFunctionDto dto = new FunFunctionDto();
dto.setFunCode("sayHello");
dto.setFunName("打招呼");
dto.setFunLanguage("JavaScript");
dto.setFunParams("name");
dto.setFunBody("return 'Hello, ' + name + '!';");
dto.setFunReturn("String");
dto.setParam("World");

ScriptEngine testEngine = scriptService.getEngineTest(dto);
Object testResult = testEngine.exec(dto.getParam());
// testResult = "Hello, World!"
```

**语言路由逻辑**（`ScriptService.getScriptEngine()`）：

| funLanguage 值 | 引擎实现 |
|----------------|----------|
| `"JavaScript"` | `JavaScriptEngineImpl` |
| `"Python"` | `PythonEngineImpl` |
| `"Groovy"` | `GroovyEngineImpl` |
| `"QLExpress"` | `QLExpressEngineImpl` |
| `"Ruby"` | `RubyEngineImpl` |
| 其他 | 抛出 `ValidationException` |

**返回类型映射**（`ScriptEngine` 构造函数）：

| funReturn 值 | Java Class |
|-------------|------------|
| `"String"` | `String.class` |
| `"Integer"` | `Integer.class` |
| `"Long"` | `Long.class` |
| `"Double"` | `Double.class` |
| `"BigDecimal"` | `BigDecimal.class` |

### 4.4 REST API 速查

**分类接口**：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/micro-fun/category/list` | 分类列表（平铺） |
| GET | `/micro-fun/category/tree` | 分类树 |
| GET | `/micro-fun/category/info?id=` | 分类详情 |
| POST | `/micro-fun/category/create` | 创建分类 |
| POST | `/micro-fun/category/update` | 修改分类（需 id + version） |
| POST | `/micro-fun/category/remove` | 删除分类（检查函数关联） |
| GET | `/micro-fun/category/options` | 分类下拉选项（visible=1 的树） |

**函数接口**：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/micro-fun/function/page` | 函数分页查询 |
| GET | `/micro-fun/function/info?id=` | 函数详情（含 funBody） |
| POST | `/micro-fun/function/create` | 创建函数 |
| POST | `/micro-fun/function/update` | 修改函数（需 id + version） |
| POST | `/micro-fun/function/remove` | 删除函数 |
| GET | `/micro-fun/function/options` | 函数下拉选项（visible=1） |
| POST | `/micro-fun/function/test` | 测试执行函数（传完整定义 + param） |

---

## 5. 配置项

| 配置 | 说明 | 默认值 |
|------|------|--------|
| `graalvm.version` | GraalVM Polyglot 版本（pom.xml property） | `23.1.2` |
| GraalVM JS 版本 | JavaScript 引擎独立版本 | `23.0.7` |
| QLExpress 版本 | 阿里巴巴表达式引擎版本 | `3.3.4` |
| Groovy 版本 | Groovy 脚本引擎版本 | `3.0.9` |

模块无 application.yml 专属配置项，自动配置通过 `FunAutoConfig` 完成（`@ComponentScan("com.wkclz.micro.fun")` + `@MapperScan("com.wkclz.micro.fun.mapper")`）。

---

## 6. 依赖

### 框架依赖

| 依赖 | 用途 |
|------|------|
| `sh-mybatis` | BaseMapper / BaseService / PageQuery 分页 |
| `sh-redis` | RedisIdGenerator（分类编码自动生成，前缀 `ctg_`） |

> 此模块不依赖 IAM 契约层。

### 脚本引擎依赖

| 依赖 | 用途 |
|------|------|
| `org.graalvm.polyglot:polyglot` | GraalVM Polyglot 核心（JS/Python/Ruby 共用） |
| `org.graalvm.sdk:graal-sdk` | GraalVM SDK |
| `org.graalvm.truffle:truffle-api` | Truffle API |
| `org.graalvm.js:js` | JavaScript 语言实现 |
| `org.graalvm.polyglot:python-community` | Python 语言实现 |
| `org.graalvm.polyglot:ruby-community` | Ruby 语言实现 |
| `com.alibaba:QLExpress` | 阿里巴巴 QLExpress 表达式引擎 |
| `org.codehaus.groovy:groovy` | Groovy 脚本引擎 |

---

## 7. 常见问题

| 问题 | 原因 / 解决 |
|------|-------------|
| 脚本执行报 "暂不支持的语言" | `ScriptService.getScriptEngine()` 仅支持 JavaScript/Python/Groovy/QLExpress/Ruby，需新增 `ScriptEngine` 子类并注册到 switch 分支 |
| 函数缓存未更新 | `ScriptService` 使用 `ConcurrentMap` 缓存引擎实例，更新函数体后需重启或清除缓存 |
| 删除分类报 "此分类已被如下函数关联" | `FunCategoryService.customDelete()` 检查了 FunFunction 的 categoryCode 关联，需先移除或修改关联函数 |
| 分类更新报 "请不要选择自身或子节点" | `checPcode()` 递归校验，pcode 不能指向自身或自身子节点 |
| GraalVM Python/Ruby 执行失败 | 需确保运行时环境安装了对应的 GraalVM 语言组件，python-community / ruby-community 依赖已引入 |
| funCode 重复创建失败 | `FunFunctionService.duplicateCheck()` 按 funCode 唯一校验，funCode 不可重复 |
| 分类 categoryCode 如何生成 | 创建时由 `RedisIdGenerator.generateIdWithPrefix("ctg_")` 自动生成，无需手动指定 |
| 函数测试接口如何使用 | POST `/micro-fun/function/test`，需传 funCode/funName/funLanguage/funBody/funReturn + param 参数 |
| QLExpress 如何传参 | 参数通过 `DefaultContext.put("param", param)` 传入，脚本中直接引用 `param` 变量 |
| Groovy 如何传参 | 通过 `Binding.setVariable(funParams, param)` 传入，funParams 字段值作为变量名 |
