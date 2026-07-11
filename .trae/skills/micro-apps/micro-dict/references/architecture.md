# micro-dict 架构参考

micro-dict 模块的完整架构、代码结构和实现细节。

## 包结构

```
micro-dict/src/main/java/com/wkclz/micro/dict/
├── DictAutoConfig.java              # @ComponentScan + @MapperScan
├── bean/
│   ├── entity/
│   │   ├── MdmDict.java             # 字典类型实体（dictCtg, dictType, description）
│   │   └── MdmDictItem.java         # 字典项实体（dictType, dictValue, dictLabel, elType, enableFlag）
│   └── dto/
│       ├── MdmDictDto.java          # 字典类型 DTO（扩展 items、dictTypes）
│       └── MdmDictItemDto.java      # 字典项 DTO（扩展 dictTypes、typeDescription）
├── cache/
│   └── DictCache.java               # Redis Pub/Sub 缓存监听
├── mapper/
│   ├── MdmDictMapper.java           # 字典 Mapper（分页/缓存/Copy/Paste 查询）
│   └── MdmDictItemMapper.java       # 字典项 Mapper（含 updateDictTypeBatch）
├── rest/
│   ├── DictRest.java                # 字典类型 CRUD + Copy/Paste
│   ├── DictItemRest.java            # 字典项批量保存
│   ├── CommonDictRest.java          # 公共字典查询（无需权限）
│   └── Route.java                   # 路由常量定义（前缀 /micro-dict）
└── service/
    ├── MdmDictService.java          # 字典类型服务（含 paste 核心逻辑）
    └── MdmDictItemService.java      # 字典项服务（含 dictItemSave diff 逻辑）
```

## REST API 端点

所有端点前缀为 `/micro-dict`：

### 字典类型（DictRest）
- `GET /dict/page` — 分页查询（支持 dictCtg/dictType/description 模糊查询）
- `GET /dict/info` — 按 ID 查详情
- `POST /dict/create` — 创建（dictType 唯一校验）
- `POST /dict/update` — 修改（含 dictType 变更时级联更新子表）
- `POST /dict/remove` — 删除（存在字典项时拒绝）
- `GET /dict/copy` — 复制字典结构为 JSON
- `POST /dict/paste` — 粘贴导入（自动 diff 增量写入）
- `GET /dict/options` — 获取所有字典类型选项列表

### 字典项（DictItemRest）
- `GET /dict/item/list` — 按 dictType 查询字典项列表（不分页）
- `POST /dict/item/save` — 批量保存（自动计算新增/修改/删除）

### 公共查询（CommonDictRest）
- `GET /common/dict/list` — 单字典查询
- `GET /common/dicts/list` — 多字典查询（逗号分隔，上限 50）

## DictCache 实现

```java
@Component
public class DictCache implements MessageListener {

    private static final String DICT_CACHE_CHANNEL = "sh:micro:dict:cache:refresh";
    private static final long DEBOUNCE_MS = 3000;

    private static volatile long CACHE_TIME = 0;
    private static volatile Map<String, Map<String, String>> CACHE_DICT = null;
    // key: dictType, value: Map<dictValue, dictLabel>

    public void clearCache() {
        // PUBLISH 到 Redis 频道 → 触发所有实例的 onMessage()
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        // synchronized 块 + 3 秒防抖 → loadCache()
    }

    public String get(String dictType, String dictKey) { ... }
    public Map<String, String> get(String dictType) { ... }
}
```

缓存加载流程：
1. `mdmDictMapper.dicts4Cache()` — 查询所有启用的字典类型
2. `mdmDictItemMapper.dictItems4Cache()` — 查询所有启用的字典项
3. `Collectors.groupingBy(MdmDictItem::getDictType)` — 按 dictType 分组
4. 组装 `Map<dictType, Map<dictValue, dictLabel>>`

## MdmDictService — Paste 核心逻辑

```java
@Transactional(rollbackFor = Exception.class)
public Integer paste(List<MdmDictDto> dtos) {
    // 1. 获取当前用户（PrincipalContext）
    // 2. 收集所有导入的 dictTypes
    // 3. 查询数据库中已有的 dict 和 item
    // 4. 构建 Map<dictType, MdmDictDto> 和 Map<dictType:dictValue, MdmDictItem>
    // 5. 遍历导入数据，与已有数据逐字段对比
    //    - dict 不存在 → dicts2Insert
    //    - dict 存在但字段变化 → dicts2Update
    //    - item 不存在 → items2Insert
    //    - item 存在但字段变化 → items2Update
    // 6. 批量执行 insertBatch / updateByIdSelective
}
```

性能优化点：
- 使用 `Collectors.toMap` 构建 Map，O(1) 查找替代 O(n²) 嵌套循环
- 逐字段对比，只更新真正变化的记录
- 事务内批量执行

## MdmDictItemService — dictItemSave Diff 逻辑

```java
@Transactional(rollbackFor = Exception.class)
public Integer dictItemSave(MdmDictDto dto) {
    // 1. 获取历史所有 items → oldItems, oldValues Set
    // 2. 新提交 items → newItems, newValues Set
    // 3. inserts = newItems 中有而 oldValues 中没有的
    // 4. deletes = oldItems 中有而 newValues 中没有的
    // 5. updates = 两者都有但字段发生变化的
    //    - 对比 dictLabel/description/elType/enableFlag/sort 字段
    //    - 构建 oldItemMap (Map<dictValue, MdmDictItem>) 做 O(1) 查找
    // 6. 执行 insertBatch + updateByIdSelective + deleteByIds
}
```

## 数据库表

### mdm_dict（字典类型）

| 列 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键（继承 BaseEntity） |
| dict_ctg | varchar | 字典分类（分组） |
| dict_type | varchar | 字典类型（大写蛇形，唯一） |
| description | varchar | 类型描述 |
| sort | int | 排序 |
| version | int | 乐观锁版本号 |
| deleted | int | 逻辑删除（0=正常） |
| create_time | datetime | 创建时间 |
| create_by | varchar | 创建人 |
| update_time | datetime | 更新时间 |
| update_by | varchar | 更新人 |
| remark | varchar | 备注 |

### mdm_dict_item（字典项）

| 列 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键（继承 BaseEntity） |
| dict_type | varchar | 所属字典类型（关联 mdm_dict） |
| dict_value | varchar | 字典值（key） |
| dict_label | varchar | 字典标签（显示值） |
| el_type | varchar | 前端元素类型 |
| description | varchar | 描述 |
| enable_flag | int | 生效状态（1=启用，0=禁用） |
| sort | int | 排序 |
| version | int | 乐观锁版本号 |
| deleted | int | 逻辑删除（0=正常） |
| create_time | datetime | 创建时间 |
| create_by | varchar | 创建人 |
| update_time | datetime | 更新时间 |
| update_by | varchar | 更新人 |
| remark | varchar | 备注 |

## Mapper SQL 说明

### MdmDictMapper.xml

| 方法 | 说明 |
|------|------|
| `getDictList` | 分页查询，LEFT JOIN mdm_dict_item 统计 count，支持 dictCtg/dictType/description 模糊查询 |
| `dicts4Cache` | 缓存加载，仅查询 dict_type 和 description，按 sort ASC |
| `dicts4Copy` | Copy 查询，按 dictTypes 列表 IN 查询，不含 ID |
| `dicts4Update` | Paste 对比查询，查全部字段 |
| `dictOptions` | 选项列表，LEFT JOIN 统计 count |

### MdmDictItemMapper.xml

| 方法 | 说明 |
|------|------|
| `getDictItemList` | 按 dictType 查询列表，按 sort ASC |
| `getAllDictItem` | 关联查询 mdm_dict 获取 type_description |
| `getDictItemsByDictTypes` | 按 dictTypes 列表 IN 查询（公共接口） |
| `dictItems4Cache` | 缓存加载，仅含 dict_type/dict_value/dict_label/enable_flag |
| `dictItems4Copy` | Copy 查询，不含 ID |
| `dictItems4Update` | Paste 对比查询，查全部字段 |
| `updateDictTypeBatch` | 级联更新子表 dictType（修改字典类型时调用） |

## dictType 命名转换

接口支持驼峰输入自动转换为大写蛇形：

```java
if (!dictType.equals(dictType.toUpperCase())) {
    dictType = StringUtil.camelToUnderline(dictType).toUpperCase();
}
```

应用位置：
- `DictItemRest.dictItemList` — 字典项列表查询
- `CommonDictRest.commonDictList` — 单字典查询
- `CommonDictRest.commonDictsList` — 多字典查询（每个 item 均 toUpperCase）

## 级联更新机制

修改 `MdmDict.dictType` 时，`MdmDictService.dictUpdate()` 自动判断：

```java
if (!mdmDict.getDictType().equals(entity.getDictType())) {
    dictItemMapper.updateDictTypeBatch(mdmDict.getDictType(), entity.getDictType());
}
```

SQL：
```sql
UPDATE mdm_dict_item
SET dict_type = #{newDictType}
WHERE dict_type = #{oldDictType}
  AND deleted = 0
```

## 缓存刷新链路

```
操作（create/update/remove/dictItemSave/paste）
    ↓
dictCache.clearCache()
    ↓
stringRedisTemplate.convertAndSend("sh:micro:dict:cache:refresh", timestamp)
    ↓
所有实例 onMessage() → loadCache()
    ↓
synchronized + 3 秒防抖 → 查询 DB → 重建 CACHE_DICT
```

## 依赖关系

| 依赖 | 用途 |
|------------|---------|
| sh-core | BaseEntity、ValidationException、R 返回对象 |
| sh-mybatis | BaseService、BaseMapper、PageQuery |
| sh-redis | StringRedisTemplate、RedisMessageListenerContainer |
| iam-contract-api | PrincipalContext（paste 操作获取当前用户编码） |
