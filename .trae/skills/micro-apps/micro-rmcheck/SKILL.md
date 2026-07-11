---
name: "micro-rmcheck"
description: "删除合规校验模块。管理删除检查规则与检查项，删除前自动校验数据是否被其他表引用。当需要实现删除前依赖检查、管理校验规则、或修改 micro-rmcheck 包下代码时触发。"
---

# Micro-Rmcheck 模块

## 1. 适用场景

- 需要在删除数据前校验该数据是否被其他表引用（外键级联检查）
- 需要管理删除检查规则（Rule）和检查项（RuleItem）的 CRUD
- 需要在业务 Service 中集成删除校验能力（通过 RmCheckApi）
- 需要修改 `com.wkclz.micro.rmcheck` 包下的任何代码

## 2. 架构概览

```
┌───────────────────────────────────────────────────────────────┐
│  REST 层                                                      │
│  RmCheckRuleRest     → 规则 CRUD (分页/详情/新增/更新/移除)    │
│  RmCheckRuleItemRest → 检查项 CRUD (列表/详情/新增/更新/移除)  │
│  PREFIX = /micro-rmcheck                                      │
└───────────────────────────┬───────────────────────────────────┘
                            │
┌───────────────────────────▼───────────────────────────────────┐
│  Service 层                                                   │
│  RmCheckRuleService     extends BaseService<RmCheckRule,      │
│                         RmCheckRuleMapper>                    │
│    - getRmCheckRulePage()  分页查询（含表注释、字段注释、     │
│      检查项数量）                                              │
│    - create()              新增规则（含重复校验、表存在校验）  │
│    - update()              更新规则                            │
│    - customRemove()        移除规则（先调用 RmCheckApi.check   │
│      自检，防止删除仍被依赖的规则）                            │
│                                                               │
│  RmCheckRuleItemService extends BaseService<RmCheckRuleItem,  │
│                         RmCheckRuleItemMapper>                │
│    - getRmCheckRuleItemList()  列表查询（含表/字段注释）       │
│    - create()                  新增检查项（含重复校验、        │
│      字段存在校验）                                            │
│    - update()                  更新检查项                      │
└───────────────────────────┬───────────────────────────────────┘
                            │
┌───────────────────────────▼───────────────────────────────────┐
│  Mapper 层                                                    │
│  RmCheckRuleMapper     extends BaseMapper<RmCheckRule>        │
│    - getRmCheckRuleList()      分页查询 [XML]                 │
│    - getRmCheckRules4Check()   校验用查询 [XML]               │
│                                                               │
│  RmCheckRuleItemMapper extends BaseMapper<RmCheckRuleItem>    │
│    - getRmCheckRuleItem()      列表查询 [XML]                 │
│    - getRmCheckRuleItem4Check() 校验用查询 [XML]              │
│                                                               │
│  CheckMapper                                                   │
│    - rmCheck()                 动态表字段检查 [XML]            │
└───────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────┐
│  API 层 (RmCheckApi)  ← 供其他模块调用                        │
│    - check(Class<P> clazz, P data)  删除前合规校验            │
│      1. 根据实体类名推导表名，查询启用的规则                   │
│      2. 反射获取规则指定字段的值                               │
│      3. 遍历规则项，动态查询被引用表是否存在关联数据           │
│      4. 存在关联则抛出 ValidationException                    │
└───────────────────────────────────────────────────────────────┘
```

## 3. 核心组件速查

| 类 | 包路径 | 说明                                                                     |
|---|---|------------------------------------------------------------------------|
| `RmcheckAutoConfig` | `com.wkclz.micro.rmcheck` | 自动配置类，@ComponentScan + @MapperScan("mapper")                           |
| `RmCheckApi` | `com.wkclz.micro.rmcheck.api` | 对外 API，提供 `check()` 删除前校验                                              |
| `RmCheckRule` | `com.wkclz.micro.rmcheck.bean.entity` | 删除检查规则实体，对应表 rm_check_rule                                             |
| `RmCheckRuleItem` | `com.wkclz.micro.rmcheck.bean.entity` | 删除检查规则-检查项实体，对应表 rm_check_rule_item                                    |
| `RmCheckRuleDto` | `com.wkclz.micro.rmcheck.bean.dto` | 规则 DTO，扩展 tableSchema/itemCount/tableComment/columnComment             |
| `RmCheckRuleItemDto` | `com.wkclz.micro.rmcheck.bean.dto` | 检查项 DTO，扩展 tableSchema/checkTableComment/checkColumnComment/checkValue |
| `RmCheckRuleMapper` | `com.wkclz.micro.rmcheck.mapper` | 规则 Mapper，extends BaseMapper                                           |
| `RmCheckRuleItemMapper` | `com.wkclz.micro.rmcheck.mapper` | 检查项 Mapper，extends BaseMapper                                          |
| `CheckMapper` | `com.wkclz.micro.rmcheck.mapper` | 校验 Mapper，动态 SQL 检查引用                                                  |
| `RmCheckRuleService` | `com.wkclz.micro.rmcheck.service` | 规则 Service，extends BaseService                                         |
| `RmCheckRuleItemService` | `com.wkclz.micro.rmcheck.service` | 检查项 Service，extends BaseService                                        |
| `RmCheckRuleRest` | `com.wkclz.micro.rmcheck.rest` | 规则 REST 控制器                                                            |
| `RmCheckRuleItemRest` | `com.wkclz.micro.rmcheck.rest` | 检查项 REST 控制器                                                           |
| `Route` | `com.wkclz.micro.rmcheck.rest` | 路由常量，PREFIX = `/micro-rmcheck`                                         |

### RmCheckRule 实体字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `ruleCode` | String | 规则编码（RedisIdGenerator 生成，前缀 `rm_rule_`） |
| `tableName` | String | 被检查的表名（蛇形命名） |
| `columnName` | String | 被检查的字段名（蛇形命名） |
| `enableFlag` | Integer | 启用状态（1=启用，默认1） |

唯一约束：`tableName` + `columnName`

### RmCheckRuleItem 实体字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `ruleCode` | String | 关联的规则编码 |
| `checkTableName` | String | 被检查引用的表名 |
| `checkColumnName` | String | 被检查引用的字段名 |
| `noticeMessage` | String | 校验不通过时的提示信息（默认："您存在删除的内容仍然被依赖，请处理完成后再删除!"） |
| `enableFlag` | Integer | 启用状态（1=启用，默认1） |

唯一约束：`ruleCode` + `checkTableName` + `checkColumnName`

## 4. 核心工作流

### 4.1 删除前校验（RmCheckApi.check）

```java
@Autowired
private RmCheckApi rmCheckApi;

// 在删除操作前调用，传入实体类和待删除数据
rmCheckApi.check(MdmXxx.class, entity);
// 如果数据被其他表引用，抛出 ValidationException
// 如果未被引用，正常返回
```

内部执行流程：

1. 将实体类简单名称转为蛇形表名（如 `MdmXxx` → `mdm_xxx`）
2. 查询 `rm_check_rule` 中 `table_name` 匹配且 `enable_flag=1` 的规则
3. 对每条规则，通过反射调用实体 getter 获取 `column_name` 对应的字段值
4. 查询 `rm_check_rule_item` 中 `rule_code` 匹配且 `enable_flag=1` 的检查项
5. 对每条检查项，执行动态 SQL：`SELECT id FROM ${checkTableName} WHERE deleted=0 AND ${checkColumnName}=#{checkValue} LIMIT 1`
6. 如果查询结果不为空，抛出 `ValidationException`，消息为检查项的 `noticeMessage`

### 4.2 规则自检删除（customRemove）

```java
// RmCheckRuleService.customRemove 内部逻辑
public Integer customRemove(RmCheckRule rmCheckRule) {
    RmCheckRule rule = mapper.selectById(rmCheckRule.getId());
    // 删除规则前，先检查规则自身是否被其他数据依赖
    rmCheckApi.check(RmCheckRule.class, rule);
    return deleteById(rmCheckRule);
}
```

### 4.3 新增规则

```java
// POST /micro-rmcheck/rule/create
// Body:
{
    "tableName": "mdm_material",
    "columnName": "material_code",
    "enableFlag": 1
}
```

内部逻辑：
- 重复校验：`tableName` + `columnName` 组合唯一
- 表存在校验：通过 `TableInfoService.getTables()` 验证表名在数据库中存在
- 自动生成 `ruleCode`：`RedisIdGenerator.generateIdWithPrefix("rm_rule_")`

### 4.4 新增检查项

```java
// POST /micro-rmcheck/rule/item/create
// Body:
{
    "ruleCode": "rm_rule_1717000000001",
    "checkTableName": "mdm_order",
    "checkColumnName": "material_code",
    "noticeMessage": "该物料仍被订单引用，请先处理订单后再删除!",
    "enableFlag": 1
}
```

内部逻辑：
- 重复校验：`ruleCode` + `checkTableName` + `checkColumnName` 组合唯一
- 字段存在校验：通过 `TableInfoService.getColumnInfos4Options()` 验证 `checkTableName.checkColumnName` 在数据库中存在

### 4.5 分页查询规则

```
GET /micro-rmcheck/rule/page?tableName=mdm_material&columnName=material_code&enableFlag=1
```

返回结果包含：
- `tableComment`：表注释（通过 information_schema.TABLES 关联）
- `columnComment`：字段注释（通过 information_schema.COLUMNS 关联）
- `itemCount`：该规则下的检查项数量

### 4.6 查询检查项列表

```
GET /micro-rmcheck/rule/item/list?ruleCode=rm_rule_1717000000001
```

返回结果包含：
- `checkTableComment`：被检查表注释
- `checkColumnComment`：被检查字段注释

### 4.7 完整使用示例

```java
// 1. 定义规则：mdm_material 表的 material_code 字段需要删除检查
// POST /micro-rmcheck/rule/create
// { "tableName": "mdm_material", "columnName": "material_code" }

// 2. 定义检查项：检查 mdm_order 表的 material_code 字段
// POST /micro-rmcheck/rule/item/create
// { "ruleCode": "rm_rule_xxx", "checkTableName": "mdm_order",
//   "checkColumnName": "material_code",
//   "noticeMessage": "该物料仍被订单引用，无法删除!" }

// 3. 在业务删除逻辑中调用校验
@Autowired
private RmCheckApi rmCheckApi;

public void deleteMaterial(Long id) {
    MdmMaterial material = materialService.selectById(id);
    rmCheckApi.check(MdmMaterial.class, material);
    // 校验通过后执行删除
    materialService.deleteById(material);
}
```

## 5. 配置项

本模块无独立配置项。依赖以下框架组件的配置：

| 配置 | 来源 | 说明 |
|------|------|------|
| Spring Data Source | sh-mybatis | 数据库连接，rm_check_rule / rm_check_rule_item 表 |
| Redis | sh-redis | RedisIdGenerator 生成规则编码 |
| PageHelper | sh-mybatis | 分页查询 |
| ShMyBatisConfig | sh-mybatis | 获取 tableSchema 用于 information_schema 查询 |

### 数据库表

表名 `rm_check_rule`：

```sql
CREATE TABLE rm_check_rule (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_code    VARCHAR(64)   COMMENT '规则编码',
    table_name   VARCHAR(128)  COMMENT '被检查表名',
    column_name  VARCHAR(128)  COMMENT '被检查字段名',
    enable_flag  INT           COMMENT '启用状态(1=启用)',
    -- BaseEntity 标准字段
    sort         INT,
    create_time  DATETIME,
    create_by    VARCHAR(31),
    update_time  DATETIME,
    update_by    VARCHAR(31),
    remark       VARCHAR(255),
    version      INT,
    deleted      VARCHAR(24) DEFAULT '0'
);
```

表名 `rm_check_rule_item`：

```sql
CREATE TABLE rm_check_rule_item (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_code         VARCHAR(64)   COMMENT '规则编码',
    check_table_name  VARCHAR(128)  COMMENT '被检查引用的表名',
    check_column_name VARCHAR(128)  COMMENT '被检查引用的字段名',
    notice_message    VARCHAR(512)  COMMENT '校验不通过提示信息',
    enable_flag       INT           COMMENT '启用状态(1=启用)',
    -- BaseEntity 标准字段
    sort         INT,
    create_time  DATETIME,
    create_by    VARCHAR(31),
    update_time  DATETIME,
    update_by    VARCHAR(31),
    remark       VARCHAR(255),
    version      INT,
    deleted      VARCHAR(24) DEFAULT '0'
);
```

## 6. 依赖

### Maven 依赖

| 依赖 | 说明 |
|------|------|
| `sh-mybatis` | BaseMapper、BaseService、PageQuery、TableInfoService、ShMyBatisConfig |
| `sh-redis` | RedisIdGenerator（规则编码生成） |
> 此模块不依赖 IAM 契约层。

### 框架组件使用

| 框架类 | 用途 |
|--------|------|
| `BaseEntity` | 实体基类，提供 id/version 等基础字段 |
| `BaseMapper` | 通用 CRUD Mapper |
| `BaseService` | 通用 CRUD Service |
| `PageQuery` | 分页查询 |
| `TableInfoService` | 获取表/列元数据（表名验证、字段验证、注释展示） |
| `ShMyBatisConfig` | 获取 tableSchema 用于 information_schema 关联查询 |
| `RedisIdGenerator` | 生成带前缀的唯一规则编码 `rm_rule_` |
| `StringUtil` | 驼峰/下划线转换（实体类名→表名、字段名→getter） |
| `ValidationException` | 校验异常（重复数据、表不存在、字段不存在、删除依赖） |

## 7. 常见问题

| 问题 | 解决 |
|------|------|
| RmCheckApi 注入失败 | 检查 `RmcheckAutoConfig` 是否被 Spring 扫描到，确认 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件存在且内容正确 |
| check() 方法反射获取字段值失败 | 确认 `RmCheckRule.columnName` 配置的是数据库蛇形字段名（如 `material_code`），模块会自动转为驼峰 getter（如 `getMaterialCode`）。实体类必须有对应的 getter 方法 |
| check() 未触发校验 | 确认规则的 `enableFlag=1`，且 `tableName` 与实体类名蛇形转换后一致（如 `MdmMaterial` → `mdm_material`） |
| CheckMapper 动态 SQL 报错 | `checkTableName` 和 `checkColumnName` 使用 `${}` 拼接（非预编译），确保配置值合法，防止 SQL 注入。仅允许通过管理后台配置，不接受用户直接输入 |
| 分页查询表注释为空 | 确认 `ShMyBatisConfig.tableSchema` 配置正确，模块依赖 information_schema 查询表/列注释 |
| 新增规则报"表不存在" | `TableInfoService.getTables()` 验证表名在当前数据库中存在，检查表名拼写和数据库连接 |
| 新增检查项报"字段不存在" | `TableInfoService.getColumnInfos4Options()` 验证字段存在，检查 checkTableName 和 checkColumnName 拼写 |
| 删除规则时自身被校验拦截 | `customRemove()` 会调用 `rmCheckApi.check(RmCheckRule.class, rule)` 自检，如果其他规则项引用了 rm_check_rule 表则无法删除 |
| 规则编码如何生成 | 通过 `RedisIdGenerator.generateIdWithPrefix("rm_rule_")` 生成，创建后不可修改（update 时 setRuleCode(null)） |
