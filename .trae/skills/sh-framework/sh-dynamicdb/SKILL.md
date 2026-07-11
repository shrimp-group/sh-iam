---
name: "sh-dynamicdb"
description: "sh-framework 动态数据源模块知识库。实现运行时动态切换数据源，基于AbstractRoutingDataSource扩展，支持运行时添加/销毁数据源、DCL双重检查锁缓存、异步创建防死循环、AOP自动清理ThreadLocal。当涉及多数据源切换、动态数据源配置、租户隔离数据源时调用。"
---

# sh-dynamicdb 模块知识库

sh-dynamicdb 是 sh-framework 的动态数据源模块，提供运行时动态切换数据源能力。基于 Spring `AbstractRoutingDataSource` 扩展，支持动态添加/销毁数据源，配合 AOP 自动清理 ThreadLocal。

## 包结构

```
com.wkclz.dynamicdb
├── ShDynamicdbAutoConfig              # 自动配置入口
├── AbstractShrimpRoutingDataSource    # 路由数据源基类（扩展AbstractRoutingDataSource）
├── DynamicDataSource                  # 实际数据源路由实现
├── DynamicDataSourceFactory           # 数据源工厂接口（策略模式）
├── DynamicDataSourceHolder            # ThreadLocal数据源key管理
├── aop/
│   └── DynamicDataSourceAop           # Mapper方法AOP切面（自动清理ThreadLocal）
├── bean/
│   └── DefaultDataSourceConfig        # 默认数据源Druid连接池参数
└── config/
    ├── DynamicDataSourceConfig        # 配置（cache-second缓存过期时间）
    └── DynamicDataSourceAutoConfig    # 条件性自动配置（@ConditionalOnBean）
```

## 核心概念与工作流程

### 端到端流程

```
1. 业务代码设置数据源key
   DynamicDataSourceHolder.set("tenant_001");

2. MyBatis执行SQL → getConnection()
   → DynamicDataSource.determineCurrentLookupKey()
     → 从ThreadLocal读取key
     → 检查缓存有效性(hasCreateDataSource)
     → 未缓存/已过期 → DCL同步块
       → 销毁旧数据源连接池
       → CompletableFuture.supplyAsync()异步创建
         → DynamicDataSourceFactory.createDataSource(key) → DataSourceInfo
         → 基于DefaultDataSourceConfig + 新url/username/password
         → DruidDataSourceFactory.createDataSource(map)
         → addDataSource(key, dataSource)注册
       → 更新缓存时间戳
     → 返回key
   → 从resolvedDataSources取出DataSource
   → 获取Connection执行SQL

3. Mapper方法执行完毕
   → DynamicDataSourceAop (@Around)
   → finally { DynamicDataSourceHolder.clear(); }  // 清理ThreadLocal
```

## DynamicDataSourceHolder — ThreadLocal管理

```java
DynamicDataSourceHolder.set("key");    // 设置当前线程的数据源key
DynamicDataSourceHolder.get();         // 获取当前线程的数据源key
DynamicDataSourceHolder.clear();       // 清除当前线程的数据源key（防止泄漏）
```

## DynamicDataSourceFactory — 数据源工厂接口

```java
public interface DynamicDataSourceFactory {
    DataSourceInfo createDataSource(String key);
}
```

**使用者必须实现此接口**，根据key从数据库配置表/配置中心/API等获取数据源连接信息。`DataSourceInfo` 包含 url、driverClassName(默认com.mysql.cj.jdbc.Driver)、username、password。

## DynamicDataSource — 路由核心

继承 `AbstractShrimpRoutingDataSource`，实现 `determineCurrentLookupKey()`：

**关键设计**：
- **DCL双重检查锁**：缓存过期判断使用synchronized+二次检查，避免并发重复创建
- **异步创建**：`CompletableFuture.supplyAsync()` 在独立线程中创建，防止"用默认数据源管理第三方数据源信息"时的死循环
- **缓存过期**：通过 `hasCreateDataSource`（ConcurrentHashMap<String, Long>）记录创建时间戳，配合 `sh.dynamicdb.cache-second`（默认60秒）判断是否过期
- **主动销毁**：`destroyDataSource(key)` 方法支持显式销毁

## DynamicDataSourceAop — AOP切面

```java
@Aspect
@Component
public class DynamicDataSourceAop {
    @Around("@within(org.apache.ibatis.annotations.Mapper)")
    public Object doAroundAdvice(ProceedingJoinPoint point) throws Throwable {
        try {
            return point.proceed();
        } finally {
            DynamicDataSourceHolder.clear();  // 无论成功失败都清理
        }
    }
}
```

**注意**：此AOP仅负责**清理**，不负责**设置**。数据源key的设置由业务代码完成。

## DefaultDataSourceConfig — 连接池参数复用

动态创建的数据源复用主数据源的Druid连接池参数，仅替换连接信息：

| 参数 | 配置键 | 默认值 |
|------|--------|--------|
| name | spring.datasource.name | default |
| url/username/password | spring.datasource.* | 空 |
| initialSize | spring.datasource.druid.initialSize | 0 |
| maxActive | spring.datasource.druid.maxActive | 8 |
| minIdle | spring.datasource.druid.minIdle | 0 |
| maxWait | spring.datasource.druid.maxWait | -1 |
| filters | spring.datasource.druid.filters | stat,wall,slf4j |

## 自动配置机制

### 条件激活

`@ConditionalOnBean({DynamicDataSourceFactory.class})` — 只有注册了 `DynamicDataSourceFactory` 实现Bean时才激活动态数据源功能。未实现时原有默认DataSource不受影响。

### DynamicDataSource注册

激活后，`DynamicDataSource` Bean以 `@Primary` 身份替换默认DataSource，接管所有数据库连接路由。

### 配置项

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| sh.dynamicdb.cache-second | 60 | 数据源缓存过期时间（秒） |

## 使用示例

```java
// 1. 实现DynamicDataSourceFactory
@Component
public class MyDataSourceFactory implements DynamicDataSourceFactory {
    @Override
    public DataSourceInfo createDataSource(String key) {
        // 根据key查询数据源配置
        TenantConfig config = configMapper.selectByKey(key);
        DataSourceInfo info = new DataSourceInfo();
        info.setUrl(config.getUrl());
        info.setUsername(config.getUsername());
        info.setPassword(config.getPassword());
        return info;
    }
}

// 2. 业务代码中使用
@Service
public class OrderService extends BaseService<Order, OrderMapper> {
    public List<Order> getOrders(String tenantCode) {
        DynamicDataSourceHolder.set(tenantCode);
        try {
            return selectByEntity(new Order());
        } finally {
            DynamicDataSourceHolder.clear();
        }
    }
}
```

## 设计亮点

1. **策略模式**：DynamicDataSourceFactory接口将数据源信息获取逻辑交给使用者
2. **条件激活**：未实现工厂接口的项目完全不受影响
3. **异步创建防死循环**：用独立线程创建数据源，避免自引用问题
4. **DCL防并发重复创建**：synchronized+二次检查
5. **AOP自动清理**：Mapper方法执行后自动清除ThreadLocal，防止线程复用时数据源泄漏
6. **连接池参数复用**：动态数据源和主数据源保持一致的行为
7. **主动销毁+被动过期**：双重保障数据源配置变更后的清理
