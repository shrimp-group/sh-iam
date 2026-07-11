---
name: micro-dict
description: 当需要操作 micro-dict 模块时使用此技能 —— 这是一个基于 Spring Boot 的数据字典管理服务。当用户需要实现字典类型 CRUD、字典项批量保存、跨环境 Copy/Paste 迁移、多字典查询、缓存刷新，或修改 micro-dict 包（com.wkclz.micro.dict）下任何代码时触发。也适用于 DictCache Redis Pub/Sub 缓存、dictType 命名规范、级联更新、diff 增量写入等场景。
---

# Micro-Dict 模块

Spring Boot 数据字典管理服务，提供字典类型和字典项的 CRUD 管理，支持跨环境 Copy/Paste 迁移，通过 Redis Pub/Sub 实现多实例缓存一致性。

## 适用场景

- 修改 `micro-dict` 包下任何 Java 文件
- 实现字典类型/字典项的增删改查
- 实现跨环境字典迁移（Copy/Paste）
- 调试字典缓存、多实例一致性、级联更新问题
- 理解 dictItemSave diff 逻辑、DictCache 机制、dictType 命名转换

## 架构概览

```
Controller      DictRest / DictItemRest / CommonDictRest
    ↓
Service         MdmDictService / MdmDictItemService
                (extends BaseService，继承通用 CRUD)
    ↓
Mapper          MdmDictMapper / MdmDictItemMapper
    ↓
缓存            DictCache（Redis Pub/Sub + 内存 Map）
```

**设计模式：**
- **缓存模式**：`DictCache`，Redis Pub/Sub + 本地 `Map<dictType, Map<dictValue, dictLabel>>`，3 秒防抖
- **Diff 模式**：`dictItemSave` 全量覆盖对比，计算 inserts/updates/deletes
- **增量更新**：`paste` 方法以 dictType+dictValue 为唯一键做 diff

完整架构、数据库表结构、配置详情见 `references/architecture.md`。

## 核心工作流

### 1. 字典类型 CRUD

注入 `MdmDictService`，通过 REST 端点操作：

```java
@Autowired MdmDictService dictService;

// 分页查询（支持 dictCtg/dictType/description 模糊查询）
PageData<MdmDict> page = dictService.getDictPage(entity);

// 新增（dictType 唯一校验）
dictService.insert(entity);

// 修改（含 dictType 变更时级联更新子表）
dictService.dictUpdate(entity);

// 删除（存在字典项时拒绝）
dictService.deleteById(entity);
```

字典类型操作后均需调用 `dictCache.clearCache()` 刷新缓存。

### 2. 字典项批量保存（diff 模式）

通过 `DictItemRest.dictItemSave` 端点，前端一次性提交所有字典项，后端自动 diff：

```java
@Autowired MdmDictItemService itemService;

MdmDictDto dto = new MdmDictDto();
dto.setDictType("GENDER");
dto.setItems(items);  // 全量提交
Integer modifys = itemService.dictItemSave(dto);
```

diff 流程：
1. 获取历史所有 items → `oldValues` Set
2. 新提交 items → `newValues` Set
3. **inserts** = newItems 中有而 oldValues 中没有的
4. **deletes** = oldItems 中有而 newValues 中没有的
5. **updates** = 两者都有但字段发生变化的
6. 事务内执行 insertBatch + updateByIdSelective + deleteByIds

### 3. 公共字典查询

无需鉴权，供前端下拉框等场景使用：

```java
// 单字典查询（驼峰自动转大写蛇形）
GET /micro-dict/common/dict/list?dictType=GENDER
GET /micro-dict/common/dict/list?dictType=gender  // 自动转为 GENDER

// 多字典查询（逗号分隔，上限 50）
GET /micro-dict/common/dicts/list?dictType=GENDER,STATUS,ROLE_TYPE
```

### 4. 跨环境迁移（Copy/Paste）

```java
// 源环境复制
GET /micro-dict/dict/copy?dictType=GENDER
// 返回 JSON 数据（不含 ID，只含 dictType + dictValue 识别信息）

// 目标环境粘贴（增量写入）
POST /micro-dict/dict/paste
Body: [上一步返回的 JSON 数组]
```

paste 逻辑：以 `dictType` + `dictType:dictValue` 为唯一键，与新旧数据做 diff，只 insert/update 差异部分。

### 5. DictCache 缓存使用

```java
@Autowired DictCache dictCache;

// 获取单个字典标签
String label = dictCache.get("GENDER", "MALE");  // → "男"

// 获取整个字典 Map（用于下拉框）
Map<String, String> map = dictCache.get("GENDER"); // → {"MALE":"男", "FEMALE":"女"}

// 手动刷新缓存（所有实例同步刷新）
dictCache.clearCache();
```

## 核心组件速查

### DictCache（Redis Pub/Sub）

- 频道：`shrimp:micro:dict:cache:refresh`
- 缓存结构：`Map<dictType, Map<dictValue, dictLabel>>`，仅含 `enable_flag=1` 的项
- `clearCache()` → PUBLISH → 所有实例 `onMessage()` → `loadCache()`
- `synchronized` + 3 秒防抖
- `volatile` 修饰 `CACHE_DICT` 和 `CACHE_TIME` 保证多线程可见性

### dictType 命名规范

- 存储：大写蛇形（如 `ORDER_STATUS`）
- 接口输入：支持驼峰自动转换（`orderStatus` → `ORDER_STATUS`）
- 转换工具：`StringUtil.camelToUnderline(dictType).toUpperCase()`

### 级联更新

修改 `MdmDict.dictType` 时，自动调用 `MdmDictItemMapper.updateDictTypeBatch(oldDictType, newDictType)` 批量更新子表。

### 缓存刷新时机

以下操作后均会调用 `dictCache.clearCache()`：
- `dictCreate` — 创建字典
- `dictUpdate` — 修改字典
- `dictRemove` — 删除字典
- `dictItemSave` — 批量保存字典项（有变更时）
- `paste` — 粘贴导入

## REST API 端点

所有端点前缀：`/micro-dict`

### 字典类型（DictRest）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/dict/page` | GET | 分页查询 |
| `/dict/info` | GET | 详情 |
| `/dict/create` | POST | 创建 |
| `/dict/update` | POST | 修改 |
| `/dict/remove` | POST | 删除 |
| `/dict/copy` | GET | 复制为 JSON |
| `/dict/paste` | POST | 粘贴导入 |
| `/dict/options` | GET | 所有字典类型选项 |

### 字典项（DictItemRest）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/dict/item/list` | GET | 按 dictType 查询 |
| `/dict/item/save` | POST | 批量保存（diff） |

### 公共查询（CommonDictRest）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/common/dict/list` | GET | 单字典查询 |
| `/common/dicts/list` | GET | 多字典查询（上限 50） |

## 重要约束

1. **dictType 唯一** — 创建和修改时校验 dictType 不可重复
2. **删除前置条件** — 必须先删除所有字典项（`dictItemSave` 传入空列表）才能删除字典类型
3. **dictItemSave 是全量覆盖** — 不在提交列表中的项将被删除
4. **`DictCache` 依赖 `RedisMessageListenerContainer`** Bean — 确保全局已配置
5. **业务异常**统一使用 `ValidationException.of("消息")` 抛出
6. **事务注解** — Rest 层 `create`/`update`/`remove`/`dictItemSave` 加 `@Transactional`，Service 层 `paste` 加 `@Transactional`
7. **Copy/Paste 不处理 ID** — 复制时不导出 ID，粘贴时以 dictType+dictValue 为唯一键做匹配

## 依赖

- `sh-core`：BaseEntity、ValidationException、R 返回对象
- `sh-mybatis`：BaseService、BaseMapper、PageQuery
- `sh-redis`：StringRedisTemplate、RedisMessageListenerContainer
- `iam-contract-api`：PrincipalContext（paste 操作获取当前用户）

## 常见问题

| 问题 | 原因/解决 |
|------|-----------|
| common/dict/list 返回空 | 检查 dictType 是否为大写，接口会自动转驼峰为大写蛇形 |
| 多实例缓存不一致 | 确认 Redis 频道 `shrimp:micro:dict:cache:refresh` 正常通信 |
| paste 未生效 | paste 是增量更新，只更新有变化的字段；检查数据格式是否与 copy 返回一致 |
| 修改 dictType 后子表丢失 | `dictUpdate` 会自动调用 `updateDictTypeBatch` 级联更新子表 |
| 删除字典类型报错"请先删除字典枚举" | `mdm_dict_item` 中仍存在该 dictType 的记录，需先通过 dictItemSave 清空 |
