---
name: "micro-seq"
description: "序列号生成模块。按 prefix 生成唯一业务序列号，支持批量生成、自动补零、SERIALIZABLE 隔离防重复。修改 micro-seq 包下代码时触发。"
---

# Micro-Seq 模块

## 1. 适用场景

- 需要生成带前缀的唯一业务编码（如订单号、工单号等）
- 需要批量生成连续序列号
- 需要查询/修改序列号配置（prefix、codeLength 等）
- 修改 `com.wkclz.micro.seq` 包下任何代码时触发

## 2. 架构概览

```
┌─────────────────────────────────────────────────────────┐
│  REST 层 (SequenceRest)                                  │
│  GET  /micro-seq/sequence/page   分页查询                │
│  GET  /micro-seq/sequence/info   详情查询                │
│  POST /micro-seq/sequence/update 修改配置                │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│  API 层 (SeqApi) — 供其他模块调用                         │
│  genSequence(prefix)                                     │
│  genSequence(prefix, seqName)                            │
│  genSequence(prefix, length, seqName)                    │
│  genSequences(prefix, size, seqName)                     │
│  genSequences(prefix, size, length, seqName)             │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│  Service 层 (MdmSequenceService extends BaseService)     │
│  getSequencePage()   分页查询                            │
│  create()            新增序列配置                         │
│  update()            修改序列配置                         │
│  genSequences()      核心序列生成 (SERIALIZABLE + sync)   │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│  Mapper 层 (MdmSequenceMapper extends BaseMapper)       │
│  getSequenceList()       分页列表查询                     │
│  getSequenceInfo()       按 prefix 查询序列              │
│  insertSequenceInfo()    插入新序列配置                   │
│  updateSequenceInfo()    更新序列号(乐观锁)               │
│  + BaseMapper 14 个通用方法                               │
└─────────────────────────────────────────────────────────┘
```

## 3. 核心组件速查

| 类 | 路径 | 说明 |
|---|---|---|
| `SeqAutoConfig` | `com.wkclz.micro.seq` | 自动配置类，@ComponentScan + @MapperScan |
| `SeqApi` | `com.wkclz.micro.seq.api` | 对外 API，供其他模块注入调用，返回 `prefix + 补零序列` |
| `MdmSequenceService` | `com.wkclz.micro.seq.service` | 核心服务，继承 BaseService，含序列生成核心逻辑 |
| `MdmSequenceMapper` | `com.wkclz.micro.seq.mapper` | Mapper 接口，继承 BaseMapper，含自定义 SQL 方法 |
| `MdmSequence` | `com.wkclz.micro.seq.bean.entity` | 数据库实体，表 `mdm_sequence`，继承 BaseEntity |
| `MdmSequenceDto` | `com.wkclz.micro.seq.bean.dto` | DTO 扩展类，继承 MdmSequence |
| `Route` | `com.wkclz.micro.seq.rest` | 路由常量接口，@Router(module="micro-seq") |
| `SequenceRest` | `com.wkclz.micro.seq.rest` | REST 控制器，提供 page/info/update 三个端点 |

### MdmSequence 实体字段

| 数据库字段 | Java 字段 | 类型 | 说明 |
|---|---|---|---|
| `id` | `id` | bigint | 主键 (继承) |
| `seq_name` | `seqName` | String | 序列名称 |
| `prefix` | `prefix` | String | 序列前缀 (唯一) |
| `sequence` | `sequence` | Integer | 当前序列值 |
| `code_length` | `codeLength` | Integer | 序列长度(不计前缀) |
| `sort` | `sort` | int | 排序 (继承) |
| `version` | `version` | int | 乐观锁 (继承) |
| `deleted` | — | varchar(24) | 逻辑删除 (继承) |

### Route 路由常量

| 常量 | 值 | 说明 |
|---|---|---|
| `PREFIX` | `/micro-seq` | 模块前缀 |
| `SEQUENCE_PAGE` | `/sequence/page` | 分页查询 |
| `SEQUENCE_INFO` | `/sequence/info` | 详情查询 |
| `SEQUENCE_UPDATE` | `/sequence/update` | 修改配置 |

## 4. 核心工作流

### 4.1 生成单个序列号（其他模块调用）

```java
@Autowired
private SeqApi seqApi;

// 最简调用：prefix=ORD, length=4(默认), seqName=prefix(默认)
String seq = seqApi.genSequence("ORD");
// 返回: "ORD0001"

// 指定名称
String seq = seqApi.genSequence("ORD", "订单序列");
// 返回: "ORD0001"

// 指定长度和名称
String seq = seqApi.genSequence("ORD", 6, "订单序列");
// 返回: "ORD000001"
```

### 4.2 批量生成序列号

```java
@Autowired
private SeqApi seqApi;

// 生成 5 个序列号，length=4(默认)
List<String> seqs = seqApi.genSequences("ORD", 5, "订单序列");
// 返回: ["ORD0001", "ORD0002", "ORD0003", "ORD0004", "ORD0005"]

// 指定长度
List<String> seqs = seqApi.genSequences("ORD", 3, 6, "订单序列");
// 返回: ["ORD000001", "ORD000002", "ORD000003"]
```

### 4.3 序列生成核心流程 (MdmSequenceService.genSequences)

```
1. 校验 prefix 非空
2. 查询 mdm_sequence 表 (按 prefix)
3. 若不存在 → 自动创建 (seqName 默认取 prefix, length 默认 4)
4. 按 size 循环递增 sequence，补零到 codeLength
5. 乐观锁更新 sequence (AND version = #{version})
6. 更新失败 → 抛出竞争异常，要求重试
```

关键机制：
- **SERIALIZABLE 隔离级别** + **synchronized** 双重保障防并发重复
- **乐观锁**：`updateSequenceInfo` 使用 `AND version = #{version}`，更新行数 < 1 时抛异常
- **自动补零**：`fitLength` 方法用 `"0".repeat()` 左补零
- **prefix 唯一**：`duplicateCheck` 方法校验 prefix 不重复

### 4.4 REST 接口调用

```bash
# 分页查询
GET /micro-seq/sequence/page?seqName=订单&prefix=ORD

# 详情查询
GET /micro-seq/sequence/info?id=1

# 修改配置 (需 id + version + 必填字段)
POST /micro-seq/sequence/update
{
    "id": 1,
    "seqName": "订单序列",
    "prefix": "ORD",
    "sequence": 100,
    "codeLength": 6,
    "version": 1
}
```

## 5. 配置项

本模块无额外配置项。依赖 Spring Boot 自动配置：

- 自动配置类：`com.wkclz.micro.seq.SeqAutoConfig`
- 注册文件：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Mapper XML 路径：`classpath:mapper/**/*.xml`

## 6. 依赖

### Maven 依赖

| 依赖 | 说明 |
|---|---|
| `com.wkclz.framework` | `sh-mybatis` | MyBatis 基础框架 (BaseMapper/BaseService/PageQuery) |

> 此模块不依赖 IAM 契约层，序列号生成通过数据库 SERIALIZABLE 隔离级别 + synchronized + 乐观锁保证并发安全。

### 模块间依赖

- `micro-dict` ← `micro-seq`：序列号字典类型（项目架构层面，代码中无直接 import）

### 数据库表

- `mdm_sequence`：序列配置表，核心字段为 `prefix`(唯一)、`sequence`(当前值)、`code_length`(补零长度)

## 7. 常见问题

| 问题 | 解决 |
|---|---|
| 序列号生成报"编码生成竞争失败" | 并发冲突导致乐观锁更新失败，重新提交即可。SERIALIZABLE + synchronized 已最大限度避免此问题 |
| 序列号长度不够，溢出 codeLength | `fitLength` 在序列值超过 codeLength 时直接返回数字字符串（不截断），需手动增大 codeLength |
| prefix 重复创建报错 | `duplicateCheck` 校验 prefix 唯一，不可创建相同 prefix 的序列配置 |
| 新 prefix 首次生成序列号 | `genSequences` 会自动创建记录，seqName 默认取 prefix，length 默认 4 |
| 批量生成中间失败 | 事务 SERIALIZABLE 隔离，整体回滚，不会出现部分生成 |
| SeqApi 注入失败 | 检查 `SeqAutoConfig` 是否被 Spring 扫描到，确认 `@ComponentScan` 包路径正确 |
| Mapper 无法注入 | 检查 `@MapperScan("com.wkclz.micro.seq.mapper")` 配置 |
