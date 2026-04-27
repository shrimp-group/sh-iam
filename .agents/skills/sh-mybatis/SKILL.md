---
name: "sh-mybatis"
description: "sh-framework MyBatis ORM模块知识库。包含BaseMapper通用CRUD(14个方法)、SQL Provider动态SQL生成、BaseService分页查询、MyBatis拦截器(自动填充/空字符串清理)、@Blob大字段分离、逻辑删除、乐观锁、表元数据查询。当涉及数据库操作、Mapper编写、SQL生成、实体映射时调用。"
---

# sh-mybatis 模块知识库

sh-mybatis 是 sh-framework 的 ORM 核心模块，基于 MyBatis + PageHelper + Druid + MySQL 构建单表 CRUD 通用能力。采用注解 + SQL Provider 方式动态生成 SQL，配合拦截器实现自动填充和查询增强。

## 包结构

```
com.wkclz.mybatis
├── ShMyBatisAutoConfig          # 自动配置（@ComponentScan + @MapperScan）
├── config/
│   └── ShMyBatisConfig          # 配置（dataLengthCheck, tableSchema解析）
├── annotation/
│   └── Blob                     # 大字段标记注解
├── exception/
│   └── MyBatisException         # MyBatis操作异常（继承CommonException）
├── bean/
│   ├── DbEntityProperty         # 实体元数据（字段映射、忽略规则、SQL构建辅助）
│   ├── TableInfo                # 表信息Bean
│   ├── ColumnInfo               # 列信息Bean
│   ├── IndexInfo                # 索引信息Bean
│   ├── ColumnQuery              # 列查询参数
│   ├── DataSourceInfo           # 数据源信息（Druid连接池管理）
│   ├── UpdateInfo               # 更新信息
│   └── DataTypeEnum             # 数据类型枚举（36个值，DB→Java/TS/InputType映射）
├── mapper/
│   ├── BaseMapper<T>            # 通用Mapper接口（14个方法）
│   ├── TableInfoMapper          # 表元数据Mapper
│   └── impl/                    # 15个SQL Provider实现
├── interceptor/
│   ├── MyBatisQueryInterceptor  # 查询拦截器（空字符串→null）
│   └── MyBatisUpdateInterceptor # 更新拦截器（自动填充createBy/updateBy）
├── service/
│   ├── BaseService<T,M>         # 通用Service抽象类
│   └── TableInfoService         # 表元数据Service
└── helper/
    └── PageQuery                # PageHelper分页工具
```

## BaseMapper — 14个通用CRUD方法

### 插入操作

| 方法 | 说明 | 特性 |
|------|------|------|
| `int insert(T entity)` | 插入单条 | 自增主键回填(@Options)；跳过null字段；@FieldDesc(notNull=true)校验 |
| `int insertBatch(List<T> entities)` | 批量插入 | 全量字段插入（含null）；sort默认0；OGNL:`entities[i].field` |

### 删除操作（逻辑删除）

| 方法 | 说明 |
|------|------|
| `int deleteById(Long id)` | 根据ID删除 |
| `int deleteByIdEntity(T entity)` | 根据实体ID删除（可带updateBy） |
| `int deleteByIds(List<Long> ids)` | 根据ID列表批量删除 |
| `int deleteByIdsEntity(T entity)` | 根据实体ids列表批量删除 |

**逻辑删除SQL**：`UPDATE table SET deleted = DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s%m'), version = version+1 WHERE ... AND deleted = 0`

### 更新操作

| 方法 | 说明 | 特性 |
|------|------|------|
| `int updateById(T entity)` | 全字段更新 | 乐观锁`AND version=#{version}`；version自增；非空字段校验 |
| `int updateByIdSelective(T entity)` | 选择性更新 | 只更新非null字段；乐观锁；version自增 |
| `int updateBatch(T entity)` | 批量更新 | 按ids IN条件；**不带乐观锁**；跳过null和空字符串 |

### 查询操作

| 方法 | 说明 | 特性 |
|------|------|------|
| `T selectById(Long id)` | ID查询单条 | 含Blob字段(selectObjFields) |
| `List<T> selectByIds(List<Long> ids)` | ID列表查询 | 不含Blob(selectListFields) |
| `List<T> selectAll()` | 查询所有 | 默认ORDER BY id DESC |
| `List<T> selectByEntity(T entity)` | 条件查询 | 动态WHERE + 安全ORDER BY |
| `List<T> selectByEntityWithLimit(T entity)` | 带分页条件查询 | LIMIT offset, size |
| `long selectCountByEntity(T entity)` | 条件计数 | SELECT COUNT(*) |
| `T selectOneByEntity(T entity)` | 查询单条 | 含Blob + LIMIT 1 |

## SQL Provider 核心 — BaseMapperProvider

所有Provider的父类，提供核心SQL构建方法：

### 实体元数据缓存

使用 `ConcurrentHashMap<Class<?>, DbEntityProperty>` 缓存实体元数据，避免重复反射。

### 字段忽略规则（DbEntityProperty常量）

| 常量 | 忽略字段 | 用途 |
|------|---------|------|
| BASE_IGNORE_FIELDS | ids | 查询时始终忽略 |
| INSERT_IGNORE_FIELDS | id, createTime, updateTime, version | 插入时忽略 |
| UPDATE_IGNORE_FIELDS | id, createBy, createTime, updateTime, version | 更新时忽略 |
| BLOB_FIELDS | TEXT, MEDIUMTEXT, TINYTEXT, LONGTEXT, JSON | 列表查询时忽略 |

### 字段分类

| 分类 | 说明 | 使用方法 |
|------|------|---------|
| insertFields | 插入字段（排除INSERT_IGNORE+BASE_IGNORE） | insert, insertBatch |
| updateFields | 更新字段（排除UPDATE_IGNORE+BASE_IGNORE） | updateById, updateByIdSelective |
| selectListFields | 列表查询字段（排除@Blob+ids） | selectByIds, selectAll, selectByEntity |
| selectObjFields | 对象查询字段（含所有字段） | selectById, selectOneByEntity |

### 动态WHERE条件构建

固定追加 `deleted = 0`，然后根据实体字段：
- String: `column = #{field}`（空字符串跳过）
- List: `column IN (...)`（空列表跳过）
- 其他: `column = #{field}`
- 时间范围: `create_time >= #{timeFrom}` / `<= #{timeTo}`

### 安全ORDER BY

- 校验排序字段必须在实体字段名集合中（驼峰和下划线均可）
- 仅允许ASC/DESC方向关键字
- 非法字段被忽略并记录warn日志

## @Blob 注解

标记实体的Blob/大文本字段。被标注的字段**不出现在列表查询中**，只在单条详情查询中包含。

```java
public class Article extends BaseEntity {
    private String title;
    
    @Blob
    private String content;  // 大文本，列表查询不加载
    
    @Blob  
    private String extra;    // JSON大字段，列表查询不加载
}
```

**影响**：selectByIds、selectAll、selectByEntity 不含 @Blob 字段；selectById、selectOneByEntity 包含。

## 拦截器

### MyBatisQueryInterceptor（查询拦截器）

拦截 `Executor.query`，将参数中的**空字符串替换为null**，防止 `column = ''` 命中意外数据。递归处理 ParamMap、Collection、数组和普通Bean。

### MyBatisUpdateInterceptor（更新拦截器）

拦截 `Executor.update`（INSERT/UPDATE/DELETE），自动填充操作人和清空时间：

| 操作 | createBy | updateBy | createTime | updateTime |
|------|---------|---------|-----------|-----------|
| INSERT | 设置为userCode | 设置为userCode | 清空(null) | 清空(null) |
| 其他 | 清空(null) | 设置为userCode | 清空(null) | 清空(null) |

**前提**：UserContext 中有用户信息，否则直接放行不处理。

## BaseService — 通用Service

```java
@Service
@Transactional
public abstract class BaseService<T extends BaseEntity, M extends BaseMapper<T>> {
    // 批量大小
    static final int BATCH_SIZE = 1000;
    
    // 核心方法
    PageData<T> selectPage(T entity);  // 分页查询：init→count→数据列表→PageData
    int insertBatch(List<T> entities); // 按1000条分批插入
    // ... 其余方法均委托BaseMapper
}
```

### selectPage 分页流程

1. `entity.init()` 初始化分页参数
2. `selectCountByEntity(entity)` 查总数
3. 若 count > 0，`selectByEntityWithLimit(entity)` 查数据列表
4. 封装为 `PageData<T>` 返回

## 表元数据功能 (TableInfoService)

通过查询 `information_schema` 获取数据库元信息：

- `getTables(TableInfo)` — 表信息列表（支持tableName模糊/IN查询）
- `getColumns(TableInfo)` — 列信息（含类型、长度、默认值、注释等）
- `getIndexs(TableInfo)` — 索引信息
- `getColumnInfos4Options(ColumnQuery)` — 字段出现次数统计

## 使用范式

```java
// 1. 实体定义（继承BaseEntity）
@Data
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {
    private String userCode;
    private String username;
}

// 2. Mapper定义（继承BaseMapper）
@Mapper
public interface UserMapper extends BaseMapper<User> {}

// 3. Service定义（继承BaseService）
@Service
public class UserService extends BaseService<User, UserMapper> {}

// 4. 自动获得的CRUD能力
// insert, insertBatch, deleteById, deleteByIds, 
// updateById, updateByIdSelective, updateBatch,
// selectById, selectByIds, selectAll, selectByEntity,
// selectOneByEntity, selectCountByEntity, selectPage
```

## PageQuery — PageHelper分页工具

```java
PageData<User> page = PageQuery.page(param, p -> userMapper.selectByEntity(p));
```

## 自动配置

- `ShMyBatisAutoConfig`：@ComponentScan("com.wkclz.mybatis") + @MapperScan("com.wkclz.mybatis.mapper")
- 注册文件：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- 兼容注册：`META-INF/spring.factories`（Spring Boot 2.x兼容）
