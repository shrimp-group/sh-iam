---
name: "sh-redis"
description: "sh-framework Redis模块知识库。包含RedisHelper全数据类型操作(String/Hash/List/Set/ZSet)、RedisLock分布式锁(SETNX+Lua原子释放)、RedisIdGenerator时间戳+Redis自增ID生成、RedisMessageQueue消息队列、Lettuce保活配置。当涉及Redis缓存操作、分布式锁、ID生成、消息队列时调用。"
---

# sh-redis 模块知识库

sh-redis 是基于 Spring Boot 4.0 + Lettuce 的 Redis 集成模块，提供缓存操作、分布式锁、ID 生成、消息队列等能力。

## 包结构

```
com.wkclz.redis
├── ShRedisAutoConfig          # 自动配置入口
├── config/
│   ├── RedisConfig            # Redis连接属性（spring.data.redis.*）
│   ├── RedisTemplateConfig    # RedisTemplate序列化配置（全部StringRedisSerializer）
│   └── RedisKeepAliveConfig   # Lettuce TCP保活配置
├── helper/
│   ├── RedisHelper            # Redis操作工具（@Component，全数据类型）
│   ├── RedisLock              # 分布式锁（@Component）
│   └── RedisIdGenerator       # ID生成器（@Component）
└── queue/
    ├── RedisMessageQueue<T>   # 消息队列接口
    ├── RedisMessageQueueImpl<T> # 基于List的消息队列实现
    ├── RedisMessageQueueManager # 消息队列管理器（@Component）
    ├── MessageListener<T>     # 消息监听器接口
    └── RedisMessageQueueExample # 示例（@Component已注释）
```

## RedisHelper — Redis操作工具

`@Component`，注入 `RedisTemplate<String, Object>`。所有方法均用 try-catch 包裹，**异常不向上传播**，返回安全默认值。

### String 操作

```java
boolean set(String key, Object value)                           // 保存，无过期
boolean set(String key, Object value, long timeout, TimeUnit)   // 保存+过期
boolean setIfAbsent(String key, Object value, long timeout, TimeUnit) // SETNX+EXPIRE原子操作
Long increment(String key)                                      // 自增
Object get(String key)                                          // 获取
boolean delete(String key)                                      // 删除
long delete(Set<String> keys)                                   // 批量删除
```

### Hash 操作

```java
boolean hSet(String key, String hashKey, Object value)
Object hGet(String key, String hashKey)
Map<Object, Object> hGetAll(String key)
```

### List 操作

```java
long lPush(String key, Object value)
Object rPop(String key)
Object lPop(String key)
Object bLPop(String key, long timeout, TimeUnit)  // 阻塞式弹出
long lLen(String key)
List<Object> lRange(String key, long start, long end)
```

### Set 操作

```java
long sAdd(String key, Object... values)
Set<Object> sMembers(String key)
```

### ZSet 操作

```java
boolean zAdd(String key, Object value, double score)
Set<Object> zRange(String key, long start, long end, boolean isDesc)
```

### 通用操作

```java
boolean expire(String key, long timeout, TimeUnit unit)     // 设置过期时间
long getExpire(String key, TimeUnit unit)                    // 获取剩余过期时间
boolean hasKey(String key)                                   // 检查键是否存在
```

## RedisLock — 分布式锁

`@Component`，核心是 **SETNX + Lua脚本原子释放**。

### 使用方式

```java
@Autowired
private RedisLock redisLock;

// 尝试获取锁，成功返回requestId，失败返回null
String requestId = redisLock.tryLock("order:123", 30, TimeUnit.SECONDS);
if (requestId != null) {
    try {
        // 业务逻辑
    } finally {
        // 释放锁（Lua脚本保证原子性，只有持有者才能释放）
        redisLock.releaseLock("order:123", requestId);
    }
}

// 带重试的获取锁
String requestId = redisLock.tryLockWithRetry("order:123", 30, TimeUnit.SECONDS, 3, 100, TimeUnit.MILLISECONDS);
```

### 设计要点

- **加锁**：`RedisHelper.setIfAbsent()` = `SET key value NX EX timeout`
- **释放锁**：Lua脚本原子操作 — `if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end`
- **防误删**：requestId(UUID)标识锁持有者，只有持有者才能释放
- **DCL懒加载**：Lua脚本的DefaultRedisScript实例双重检查锁初始化
- **中断处理**：tryLockWithRetry中线程被中断时恢复中断标志位

## RedisIdGenerator — ID生成器

`@Component`，基于 **时间戳 + Redis自增序列号** 的分布式ID生成器，考虑时间回拨，62进制编码输出短ID。

### 使用方式

```java
@Autowired
private RedisIdGenerator idGenerator;

// 生成带前缀的ID（前缀不能为空）
String orderId = idGenerator.generateIdWithPrefix("ORD");  // ORD1a2b3c

// 生成指定业务类型的ID
String userId = idGenerator.generateIdWithType("user");     // 1a2b3c
String defaultId = idGenerator.generateIdWithType(null);    // 使用"default"
```

### ID结构

```
| 相对时间戳(高位) | 机器标识(6位) | 序列号(14位) |
| (timestamp - 2024-01-01) | machineId | sequence |
```

- 基础时间：2024-01-01 00:00:00
- 序列号：14位，每毫秒最多16384个ID
- 机器标识：6位，最多64台机器（IP后两字节 & 0x3F）
- Redis Key：`id:generator:{businessType}`，5秒过期
- **Redis降级**：不可用时回退到 `generateLocalId()` 纯本地生成
- **时间回拨**：使用上一次时间戳，拒绝生成"过去"的ID
- **序列号溢出**：超过16383时自旋等待下一毫秒

## RedisMessageQueue — 消息队列

### 接口定义

```java
public interface RedisMessageQueue<T> {
    boolean sendMessage(T message);
    T receiveMessage() throws InterruptedException;               // 阻塞
    T receiveMessageNonBlocking();                                 // 非阻塞
    T receiveMessage(long timeout, TimeUnit timeUnit);            // 带超时阻塞
    long getMessageCount();
    void clear();
}
```

基于Redis List实现：lPush发送，lPop/rPop/bLPop接收。

### RedisMessageQueueManager — 管理器

```java
@Autowired
private RedisMessageQueueManager manager;

// 获取或创建队列
RedisMessageQueue<OrderMsg> queue = manager.getQueue("order_queue", OrderMsg.class);

// 订阅队列（每个队列只能有一个监听器）
manager.subscribe("order_queue", new MessageListener<OrderMsg>() {
    @Override
    public void onMessage(OrderMsg message) { /* 处理消息 */ }
    @Override
    public Class<OrderMsg> getMessageType() { return OrderMsg.class; }
});

// 发送消息
manager.sendMessage("order_queue", orderMsg);

// 取消订阅
manager.unsubscribe("order_queue");
```

**消费线程**：
- 线程池：核心4，最大16，有界队列1024，CallerRunsPolicy拒绝策略
- 监听器移除后消费线程自动退出
- 监听器异常不中断消费线程，仅记录错误日志

## 配置

### Redis连接参数

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| spring.data.redis.host | localhost | Redis主机 |
| spring.data.redis.port | 6379 | Redis端口 |
| spring.data.redis.password | 空 | Redis密码（空字符串时不设置） |
| spring.data.redis.database | 0 | 数据库编号 |

### Lettuce保活配置

- TCP Keep-Alive：启用
- TCP NoDelay：启用（禁用Nagle算法）
- 连接超时：10秒
- 命令超时：5秒

### 序列化

所有Key/Value统一使用 `StringRedisSerializer`，Value需业务方自行处理对象与字符串的转换。

## 自动配置

`ShRedisAutoConfig`：@AutoConfiguration + @ComponentScan("com.wkclz.redis")

自动注册：RedisHelper, RedisLock, RedisIdGenerator, RedisMessageQueueManager, RedisConfig, RedisKeepAliveConfig

不注册：RedisMessageQueueImpl, RedisMessageQueueExample（@Component已注释）
