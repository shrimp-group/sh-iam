---
name: "micro-audit"
description: "数据变更审计模块。记录实体增删改的变更日志，支持字段级差异对比。当需要审计数据变更、查询变更记录、或修改 micro-audit 包下代码时触发。"
---

# Micro-Audit 模块

## 1. 适用场景

- 需要记录业务数据的创建、修改、删除操作日志
- 需要对比数据变更前后的字段级差异
- 需要按批次号、表名、数据ID、操作类型等条件查询变更记录
- 需要修改 `com.wkclz.micro.audit` 包下的任何代码
- 需要在业务 Service 中集成审计能力（通过 AuditApi）

## 2. 架构概览

```
┌─────────────────────────────────────────────────────────┐
│  REST 层 (ChangeLogRest)                                │
│  GET /micro-audit/change/log/page  → 分页查询变更记录    │
│  GET /micro-audit/change/log/info  → 查询变更记录详情    │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│  Service 层 (MdmChangeLogService)                       │
│  extends BaseService<MdmChangeLog, MdmChangeLogMapper>  │
│  - getChangeLogPage()                                   │
│  - create() / update()                                  │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│  Mapper 层 (MdmChangeLogMapper)                         │
│  extends BaseMapper<MdmChangeLog>                       │
│  - getChangeLogList()  [XML 实现]                       │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  API 层 (AuditApi → AuditImpl)  ← 供其他模块调用        │
│  - create()   记录新增审计                              │
│  - modify()   记录修改审计（含字段差异对比）             │
│  - delete()   记录删除审计                              │
│  - getLogPage() 查询变更日志（含字段级差异项）           │
│  - getBatchNo() 生成批次号                              │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  工具层 (AuditCompareUtil)                              │
│  - getTableName()   实体类名 → 蛇形表名                 │
│  - getDataValueJson()  实体 → JSON 字符串               │
└─────────────────────────────────────────────────────────┘
```

## 3. 核心组件速查

| 类 | 包路径 | 说明 |
|---|---|---|
| `AuditAutoConfig` | `com.wkclz.micro.audit` | 自动配置类，@ComponentScan + @MapperScan |
| `AuditApi` | `com.wkclz.micro.audit.api` | 对外 API 接口，定义 create/modify/delete/getLogPage |
| `AuditImpl` | `com.wkclz.micro.audit.api.impl` | AuditApi 实现，核心审计逻辑 |
| `MdmChangeLog` | `com.wkclz.micro.audit.bean.entity` | 变更记录实体，对应表 mdm_change_log |
| `MdmChangeLogDto` | `com.wkclz.micro.audit.bean.dto` | 实体扩展 DTO |
| `ChangeLog<T>` | `com.wkclz.micro.audit.bean.dto` | 变更日志查询 DTO，泛型 T 指定实体类型，含 items 差异项 |
| `ChangeLogItem` | `com.wkclz.micro.audit.bean.dto` | 字段级差异项：columnName / columnDesc / oldValue / newValue |
| `ChangeLogMap` | `com.wkclz.micro.audit.bean.dto` | 变更记录 Map 视图，将 dataFrom/dataTo JSON 解析为 Map |
| `MdmChangeLogMapper` | `com.wkclz.micro.audit.mapper` | Mapper 接口，extends BaseMapper，自定义 getChangeLogList |
| `MdmChangeLogService` | `com.wkclz.micro.audit.service` | Service 层，extends BaseService |
| `ChangeLogRest` | `com.wkclz.micro.audit.rest` | REST 控制器，分页查询 + 详情 |
| `Route` | `com.wkclz.micro.audit.rest` | 路由常量，PREFIX = `/micro-audit` |
| `AuditCompareUtil` | `com.wkclz.micro.audit.utils` | 审计对比工具，提取实体表名和字段值 |

### MdmChangeLog 实体字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `batchNo` | String | 批次号（同一业务操作的多条记录共享） |
| `tableName` | String | 表名（实体类名蛇形转换） |
| `dataId` | Long | 数据 ID |
| `dataVersion` | Integer | 数据版本号 |
| `operateType` | String | 操作类型：INSERT / UPDATE / DELETE |
| `dataFrom` | String | 原数据 JSON |
| `dataTo` | String | 目标数据 JSON |

## 4. 核心工作流

### 4.1 记录新增审计

```java
@Autowired
private AuditApi auditApi;

// 单条新增审计
auditApi.create(entity);

// 批量新增审计
auditApi.create(entityList);

// 指定批次号的新增审计
String batchNo = auditApi.getBatchNo();
auditApi.create(entity, batchNo);
auditApi.create(entityList, batchNo);
```

内部逻辑：`operateType = "INSERT"`，`dataFrom = null`，`dataTo = 实体JSON`，`dataVersion = 0`。

### 4.2 记录修改审计

```java
// 修改前先查询旧数据
MdmXxx oldEntity = service.selectById(id);
MdmXxx newEntity = new MdmXxx();
// ... 设置新值 ...

// 单条修改审计（from=旧值, to=新值）
auditApi.modify(oldEntity, newEntity);

// 批量修改审计（froms 和 tos 必须等长，且 id 一一对应）
auditApi.modify(oldList, newList);

// 指定批次号
auditApi.modify(oldEntity, newEntity, batchNo);
```

内部逻辑：`operateType = "UPDATE"`，`dataFrom = 旧实体JSON`，`dataTo = 新实体JSON`，`dataVersion = from.version + 1`。

### 4.3 记录删除审计

```java
// 单条删除审计
auditApi.delete(entity);

// 批量删除审计
auditApi.delete(entityList);

// 指定批次号
auditApi.delete(entity, batchNo);
```

内部逻辑：`operateType = "DELETE"`，`dataFrom = 实体JSON`，`dataTo = null`，`dataVersion = from.version + 1`。

### 4.4 查询变更日志（含字段级差异）

```java
ChangeLog<MdmXxx> dto = new ChangeLog<>();
dto.setClazz(MdmXxx.class);
dto.setDataId(123L);
dto.setOperateType("UPDATE");
dto.setBatchNo("audit_xxx");
dto.setCreateBy("admin");
dto.setTimeFrom(timeFrom);
dto.setTimeTo(timeTo);
dto.setKeyword("关键字");

PageData<ChangeLog> page = auditApi.getLogPage(dto);
// 每条 ChangeLog 包含 items 列表，每个 ChangeLogItem 含:
// columnName, columnDesc, oldValue, newValue
```

内部逻辑：通过 `TableInfoService` 获取表列信息，通过 `BeanUtil.getJavaField()` 反射获取 getter，逐字段对比 from/to 的值，生成差异项列表。

### 4.5 REST 接口查询

```
GET /micro-audit/change/log/page?tableName=xxx&dataId=123&operateType=UPDATE
GET /micro-audit/change/log/info?id=456
```

分页和详情接口返回 `ChangeLogMap`，将 `dataFrom`/`dataTo` JSON 字符串解析为 `Map<String, Object>` 对象（`dataFromEntity`/`dataToEntity`）。

### 4.6 批次号机制

```java
// 批次号由 RedisIdGenerator 生成，前缀 "audit_"
String batchNo = auditApi.getBatchNo();
// 例: "audit_1717000000001"

// 同一批次的多条审计记录共享 batchNo，用于关联同一业务操作
auditApi.create(entity1, batchNo);
auditApi.create(entity2, batchNo);
auditApi.modify(oldEntity3, newEntity3, batchNo);
```

### 4.7 表名推导规则

`AuditCompareUtil.getTableName()` 将实体类简单名称转为蛇形表名：
- `MdmXxx` → `mdm_xxx`
- `SysUser` → `sys_user`

## 5. 配置项

本模块无独立配置项。依赖以下框架组件的配置：

| 配置 | 来源 | 说明 |
|------|------|------|
| Spring Data Source | sh-mybatis | 数据库连接，mdm_change_log 表 |
| Redis | sh-redis | RedisIdGenerator 生成批次号 |
| PageHelper | sh-mybatis | 分页查询 |

### 数据库表

表名 `mdm_change_log`，核心字段：

```sql
CREATE TABLE mdm_change_log (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_no     VARCHAR(64)   COMMENT '批次号',
    table_name   VARCHAR(128)  COMMENT '表名',
    data_id      BIGINT        COMMENT '数据ID',
    data_version INT           COMMENT '数据版本',
    operate_type VARCHAR(16)   COMMENT '操作类型(INSERT/UPDATE/DELETE)',
    data_from    TEXT          COMMENT '原数据JSON',
    data_to      TEXT          COMMENT '目标数据JSON',
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
| `sh-mybatis` | BaseMapper、BaseService、PageQuery、TableInfoService |
| `sh-redis` | RedisIdGenerator（批次号生成） |

> 此模块不依赖 IAM 契约层。

### 框架组件使用

| 框架类 | 用途 |
|--------|------|
| `BaseEntity` | 审计实体基类，提供 id/version 等基础字段 |
| `BaseMapper` | 通用 CRUD Mapper |
| `BaseService` | 通用 CRUD Service |
| `PageQuery` | 分页查询 |
| `TableInfoService` | 获取表列元数据（列名、注释） |
| `RedisIdGenerator` | 生成带前缀的唯一批次号 |
| `BeanUtil` | 反射获取 Java 字段和 getter |
| `StringUtil` | 驼峰/下划线转换 |
| `ValidationException` | 参数校验异常 |

## 7. 常见问题

| 问题 | 解决 |
|------|------|
| AuditApi 注入失败 | 检查 `AuditAutoConfig` 是否被 Spring 扫描到，确认 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件存在且内容正确 |
| getLogPage 返回 items 为空 | 确认 `TableInfoService` 能查到对应表的列信息，且实体类字段名与数据库列名匹配（驼峰转下划线） |
| 表名推导不正确 | `AuditCompareUtil` 基于实体类简单名称转蛇形，如 `MdmXxx` → `mdm_xxx`。若实际表名不符，需确认实体类命名 |
| modify 时 id 不一致报错 | `from.getId()` 必须等于 `to.getId()`，且 froms 和 tos 列表长度必须一致 |
| 批次号为空 | 不传 batchNo 时会自动通过 `RedisIdGenerator.generateIdWithPrefix("audit_")` 生成 |
| dataFrom/dataTo 存储格式 | 以 JSON 字符串存储，通过 `AuditCompareUtil.getDataValueJson()` 将实体所有 getter 返回值序列化 |
| REST 接口返回 JSON 而非对象 | `/change/log/page` 和 `/change/log/info` 会将 dataFrom/dataTo JSON 解析为 `dataFromEntity`/`dataToEntity` Map 对象 |
