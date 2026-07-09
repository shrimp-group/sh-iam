---
name: "micro-liteflow"
description: "LiteFlow 规则引擎模块。当需要管理规则链(Chain)和脚本(Script)的 CRUD、配置 LiteFlow SQL 数据源、编写 NodeComponent 节点、或修改 micro-liteflow 包下代码时触发。"
---

# Micro-LiteFlow 模块

## 1. 适用场景

- 对 LiteFlow 规则链（Chain）进行增删改查管理
- 对 LiteFlow 脚本（Script）进行增删改查管理（支持 Groovy / JS / Aviator）
- 编写自定义 NodeComponent 节点组件
- 配置 LiteFlow SQL 数据源轮询与规则自动刷新
- 修改 `com.wkclz.micro.liteflow` 包下的任何代码

## 2. 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│                        REST 层                               │
│  LiteflowChainRest    LiteflowScriptRest   CommonLiteFlowRest│
│  (规则链 CRUD)         (脚本 CRUD)           (扩展预留)        │
└──────────────┬────────────────┬──────────────────┬──────────┘
               │                │                  │
┌──────────────▼────────────────▼──────────────────▼──────────┐
│                       Service 层                             │
│  LiteflowChainService              LiteflowScriptService     │
│  (extends BaseService<LiteflowChain,                         │
│         LiteflowChainMapper>)       (extends BaseService<    │
│                                     LiteflowScript,          │
│                                     LiteflowScriptMapper>)   │
└──────────────┬────────────────┬─────────────────────────────┘
               │                │
┌──────────────▼────────────────▼─────────────────────────────┐
│                     Mapper 层                                │
│  LiteflowChainMapper               LiteflowScriptMapper      │
│  (extends BaseMapper<LiteflowChain>)                         │
│                                     (extends BaseMapper<     │
│                                      LiteflowScript>)        │
└─────────────────────────────────────────────────────────────┘
               │                │
┌──────────────▼────────────────▼─────────────────────────────┐
│                    LiteFlow 引擎层                            │
│  FlowExecutor ← liteflow-rule-sql (数据库规则源)              │
│  NodeComponent 子类 (ACmp / BCmp / CCmp 等业务节点)          │
│  脚本引擎: Groovy / Javax(JS) / Aviator                      │
└─────────────────────────────────────────────────────────────┘
```

## 3. 核心组件速查

### 实体类

| 类名 | 表名 | 核心字段 | 说明 |
|------|------|----------|------|
| `LiteflowChain` | `liteflow_chain` | chainName, chainDesc, elData, route, namespace, enable | 规则链定义，elData 存储 EL 表达式 |
| `LiteflowScript` | `liteflow_script` | scriptId, scriptName, scriptData, scriptType, scriptLanguage, enable | 脚本定义，scriptData 存储脚本内容 |

### DTO 类

| 类名 | 父类 | 说明 |
|------|------|------|
| `LiteflowChainDto` | `LiteflowChain` | 规则链扩展 DTO，提供 `copy(LiteflowChain)` 静态转换 |
| `LiteflowScriptDto` | `LiteflowScript` | 脚本扩展 DTO，提供 `copy(LiteflowScript)` 静态转换 |

### Mapper 接口

| 类名 | 继承 | 自定义方法 |
|------|------|-----------|
| `LiteflowChainMapper` | `BaseMapper<LiteflowChain>` | `getLiteflowChainList4Page(LiteflowChain)` |
| `LiteflowScriptMapper` | `BaseMapper<LiteflowScript>` | `getLiteflowScriptList4Page(LiteflowScript)` |

### Service 类

| 类名 | 继承 | 核心方法 |
|------|------|----------|
| `LiteflowChainService` | `BaseService<LiteflowChain, LiteflowChainMapper>` | `getLiteflowChainPage`, `create`, `update`, `duplicateCheck` |
| `LiteflowScriptService` | `BaseService<LiteflowScript, LiteflowScriptMapper>` | `getLiteflowScriptPage`, `create`, `update`, `duplicateCheck` |

### REST 控制器

| 类名 | 路由 | 端点 |
|------|------|------|
| `LiteflowChainRest` | `/micro-liteflow` | GET `/chain/page`, GET `/chain/info`, POST `/chain/create`, POST `/chain/update`, POST `/chain/remove` |
| `LiteflowScriptRest` | `/micro-liteflow` | GET `/script/page`, GET `/script/info`, POST `/script/create`, POST `/script/update`, POST `/script/remove` |
| `CommonLiteFlowRest` | `/micro-liteflow` | 扩展预留（当前为空） |
| `LiteFlowTestRest` | `/micro-liteflow` | GET `/public/liteflow/test` (演示用) |

### Demo 组件

| 类名 | 继承 | @Component 名 | 说明 |
|------|------|---------------|------|
| `ACmp` | `NodeComponent` | `"a"` | 演示节点 A |
| `BCmp` | `NodeComponent` | `"b"` | 演示节点 B |
| `CCmp` | `NodeComponent` | `"c"` | 演示节点 C |
| `LiteFlowDemo` | — | — | 演示执行器，注入 `FlowExecutor` 执行 chain1 |

### 自动配置

| 类名 | 注解 | 说明 |
|------|------|------|
| `LiteFlowAutoConfig` | `@Configuration` + `@ComponentScan("com.wkclz.micro.liteflow")` + `@MapperScan("com.wkclz.micro.liteflow.mapper")` | 模块自动配置入口 |

## 4. 核心工作流

### 4.1 规则链 CRUD

```java
// 创建规则链（elData 为 LiteFlow EL 表达式）
LiteflowChain chain = new LiteflowChain();
chain.setChainName("orderProcess");
chain.setChainDesc("订单处理流程");
chain.setElData("THEN(a, b, c)");  // EL 表达式
chain.setRoute("/order");
chain.setNamespace("default");
chain.setEnable(1);
liteflowChainService.create(chain);

// 分页查询
LiteflowChain query = new LiteflowChain();
query.setChainName("order");
PageData<LiteflowChain> page = liteflowChainService.getLiteflowChainPage(query);

// 更新（需传 id + version）
chain.setId(1L);
chain.setVersion(0);
chain.setElData("THEN(a, WHEN(b, c))");
liteflowChainService.update(chain);

// 删除
liteflowChainService.deleteById(chain);
```

### 4.2 脚本 CRUD

```java
// 创建脚本
LiteflowScript script = new LiteflowScript();
script.setScriptId("discount_script");
script.setScriptName("折扣计算");
script.setScriptData("def discount(price) { return price * 0.9 }");
script.setScriptType("script");       // script / if_script / for_script / switch_script 等
script.setScriptLanguage("groovy");   // groovy / js / aviator
script.setEnable(1);
liteflowScriptService.create(script);

// 分页查询
LiteflowScript query = new LiteflowScript();
query.setScriptLanguage("groovy");
PageData<LiteflowScript> page = liteflowScriptService.getLiteflowScriptPage(query);
```

### 4.3 自定义 NodeComponent 节点

```java
@Component("myNode")
public class MyNodeCmp extends NodeComponent {
    @Override
    public void process() {
        // 业务逻辑
        log.info("MyNode executed!");
    }
}
```

在 EL 表达式中引用：`THEN(myNode, ...)` 或 `WHEN(myNode, ...)`

### 4.4 FlowExecutor 执行规则链

```java
@Autowired
private FlowExecutor flowExecutor;

public void execute(String chainId, Object param) {
    LiteflowResponse response = flowExecutor.execute2Resp(chainId, param);
    if (response.isSuccess()) {
        log.info("规则链 {} 执行成功", chainId);
    } else {
        log.error("规则链 {} 执行失败: {}", chainId, response.getMessage());
    }
}
```

### 4.5 参数校验规则

- **LiteflowChain**: `chainName` 必填，`enable` 默认为 1
- **LiteflowScript**: `scriptId` + `scriptName` 必填，`enable` 默认为 1
- **更新操作**: 必须传 `id` + `version`（乐观锁）
- **删除操作**: 必须传 `id`

## 5. 配置项

### application.yml

```yaml
liteflow:
  print-banner: false
  rule-source-ext-data-map:
    applicationName: demo
    sqlLogEnabled: false
    # SQL 数据轮询自动刷新（生产环境建议开启）
    pollingEnabled: true
    pollingIntervalSeconds: 59
    pollingStartSeconds: 59

    # 规则链表映射
    chainTableName: liteflow_chain
    chainNameField: chain_name
    elDataField: el_data
    routeField: route
    namespaceField: namespace
    chainCustomSql: >-
      SELECT chain_name, chain_desc, el_data, route, namespace
      FROM liteflow_chain WHERE deleted = 0 AND enable = 1

    # 脚本表映射
    scriptTableName: liteflow_script
    scriptIdField: script_id
    scriptNameField: script_name
    scriptDataField: script_data
    scriptTypeField: script_type
    scriptLanguageField: script_language
    scriptCustomSql: >-
      SELECT script_id, script_name, script_data, script_type, script_language
      FROM liteflow_script WHERE deleted = 0 AND enable = 1
```

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `liteflow.print-banner` | `false` | 是否打印 LiteFlow Banner |
| `liteflow.rule-source-ext-data-map.applicationName` | `demo` | 应用名称标识 |
| `liteflow.rule-source-ext-data-map.sqlLogEnabled` | `false` | 是否开启 SQL 日志 |
| `liteflow.rule-source-ext-data-map.pollingEnabled` | `true` | 是否开启轮询自动刷新 |
| `liteflow.rule-source-ext-data-map.pollingIntervalSeconds` | `59` | 轮询间隔（秒） |
| `liteflow.rule-source-ext-data-map.pollingStartSeconds` | `59` | 首次轮询延迟（秒） |
| `liteflow.rule-source-ext-data-map.chainCustomSql` | — | 自定义 Chain 查询 SQL（过滤已删除和禁用） |
| `liteflow.rule-source-ext-data-map.scriptCustomSql` | — | 自定义 Script 查询 SQL（过滤已删除和禁用） |

## 6. 依赖

### Maven 依赖

| groupId | artifactId | 版本 | 说明 |
|---------|-----------|------|------|
| `com.wkclz.framework` | `sh-mybatis` | 父 POM 管理 | ORM 框架（BaseMapper / BaseService） |
| `com.yomahub` | `liteflow-spring-boot-starter` | `2.12.4.1` | LiteFlow Spring Boot Starter |
| `com.yomahub` | `liteflow-rule-sql` | `2.12.4.1` | LiteFlow SQL 规则源（从数据库加载规则） |
| `com.yomahub` | `liteflow-script-groovy` | `2.12.4.1` | Groovy 脚本引擎 |
| `com.yomahub` | `liteflow-script-javax` | `2.12.4.1` | JS 脚本引擎（javax.script） |
| `com.yomahub` | `liteflow-script-aviator` | `2.12.4.1` | Aviator 表达式引擎 |

### 框架模块依赖

- **sh-core**: BaseEntity, R, ValidationException, ResultCode
- **sh-mybatis**: BaseMapper(14 方法), BaseService, PageQuery

## 7. 常见问题

| 问题 | 解决方案 |
|------|----------|
| 规则链修改后未生效 | 检查 `pollingEnabled` 是否为 `true`，轮询间隔是否合理；也可手动重启应用 |
| 脚本执行报 ClassNotFoundException | 确认对应脚本引擎依赖已引入（groovy / javax / aviator） |
| NodeComponent 未被 LiteFlow 识别 | 确认类在 `com.wkclz.micro.liteflow` 包下且标注了 `@Component("nodeId")` |
| LiteFlow 未从数据库加载规则 | 检查 `chainCustomSql` / `scriptCustomSql` 是否正确，确认 `deleted=0 AND enable=1` 条件匹配 |
| 分页查询返回空 | 检查 Mapper XML 中动态 SQL 条件，确认 `deleted=0` 过滤和查询参数 |
| 乐观锁更新失败 | 前端必须回传 `version` 字段，Service 层 `update` 方法会校验 |
| duplicateCheck 总是通过 | 当前 `duplicateCheck` 方法中 `if(true) return;` 为占位逻辑，需根据业务设置唯一条件 |
| `scriptType` 取值 | LiteFlow 标准类型：`script` / `if_script` / `for_script` / `switch_script` / `boolean_script` |
| `scriptLanguage` 取值 | 支持 `groovy` / `js` / `aviator`，取决于引入的脚本引擎依赖 |
