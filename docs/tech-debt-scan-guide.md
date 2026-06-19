# 技术债务扫描指引

## 扫描概述

在 harness 初始化时，AI 应系统性扫描目标项目的技术债务，识别潜在风险并记录到 docs/tech-debts/ 目录。

## 扫描步骤

1. 读取项目所有源码文件
2. 按分类逐项检查
3. 对发现的每条债务，使用 tech-debt.md.template 模板创建记录
4. 更新 INDEX.md 索引

## 分类扫描方法

### 1. 性能风险（PERF）

- **高并发瓶颈**：查找同步锁、单线程处理、无缓存的重复计算
- **数据库查询效率**：查找 N+1 查询、缺少索引的查询、全表扫描、大事务
- **IO 阻塞**：查找同步文件操作、无超时的网络调用、串行 IO
- **资源竞争**：查找连接池耗尽、文件锁竞争、共享资源争用

### 2. 内存隐患（MEM）

- **内存泄漏**：查找未关闭的资源（连接、流）、静态集合持续增长、监听器未注销、缓存无淘汰策略
- **内存溢出**：查找无限制的集合增长、大数组分配、递归无终止条件
- **大对象分配**：查找一次性加载大文件/数据、大字符串拼接、图片/媒体未压缩
- **GC 压力**：查找频繁创建短生命周期对象、装箱拆箱、finalizer 使用

### 3. 线程与并发风险（CONC）

- **死锁**：查找嵌套锁、锁顺序不一致、synchronized 嵌套
- **竞态条件**：查找 check-then-act 模式、非原子复合操作、共享可变状态无同步
- **线程池配置不当**：查找无界队列、核心线程数=0、拒绝策略不当、new Thread() 直接创建
- **上下文切换开销**：查找过多线程、频繁 wait/notify、锁粒度过大

### 4. 安全风险（SEC）

- **OWASP Top 10**：查找 SQL 拼接、XSS（未转义输出）、CSRF 缺失、反序列化漏洞、SSRF
- **敏感数据泄露**：查找硬编码密码/密钥、日志中打印敏感信息、未加密传输、敏感数据明文存储
- **权限校验缺失**：查找未鉴权的 API、越权访问、IDOR（不安全直接对象引用）

### 5. 其他潜在风险（OTHER）

- **可观测性与运维**：查找缺少日志的关键路径、无健康检查、无监控指标、无告警配置
- **业务连续性与容错**：查找无熔断/降级的外部调用、单点故障、无重试机制、无超时设置
- **数据一致性与可靠性**：查找分布式事务无补偿、消息无幂等、无备份策略、数据迁移无回滚
- **合规与法律风险**：查找个人信息未脱敏、开源许可证冲突、未授权的第三方数据使用
- **代码质量与可维护性**：查找高耦合模块、无测试覆盖的核心逻辑、过时依赖、缺失文档

## 识别标志速查表

| 分类    | 子类                          | 典型关键词/模式                                                            |
|-------|-----------------------------|---------------------------------------------------------------------|
| PERF  | 高并发瓶颈(bottleneck)           | `synchronized`、`ReentrantLock`、单线程 Executor、无缓存重复调用                 |
| PERF  | 数据库查询效率(slow-query)         | `SELECT *`、循环内查询、无 `WHERE` 条件、`@Transactional` 大方法、缺少 `INDEX`       |
| PERF  | IO 阻塞(io-blocking)          | 同步 `read/write`、无 `timeout` 的 HTTP 调用、串行文件处理                        |
| PERF  | 资源竞争(resource-contention)   | 连接池满、`maxConnections` 过小、文件锁 `FileLock`、共享队列                        |
| MEM   | 内存泄漏(memory-leak)           | 未 `close()` 的 Stream/Connection、静态 `Map/List` 只增不减、未注销 Listener     |
| MEM   | 内存溢出(oom)                   | 无上限集合、`new byte[HUGE]`、递归无终止、`ArrayList` 无初始容量                      |
| MEM   | 大对象分配(large-object)         | 一次性 `readAllBytes`、大字符串 `+` 拼接、未压缩图片加载                              |
| MEM   | GC 压力(gc-pressure)          | 循环内 `new Object`、频繁装箱 `Integer.valueOf`、`finalize()` 方法             |
| CONC  | 死锁(deadlock)                | 嵌套 `synchronized`、多锁不一致顺序、`lock()` 嵌套                               |
| CONC  | 竞态条件(race-condition)        | `if-then-act` 无同步、`check-then-update`、共享变量无 `volatile`              |
| CONC  | 线程池配置不当(thread-pool)        | `newCachedThreadPool`、`corePoolSize=0`、`AbortPolicy`、`new Thread()` |
| CONC  | 上下文切换开销(context-switch)     | 过多 `Thread` 创建、频繁 `wait/notify`、大范围锁块                               |
| SEC   | OWASP Top 10(owasp)         | SQL 字符串拼接、`innerHTML`、无 CSRF Token、`ObjectInputStream`、未校验 URL      |
| SEC   | 敏感数据泄露(data-leak)           | 硬编码 `password`/`secret`、`log.info(userInfo)`、`http://` 传输、明文存储      |
| SEC   | 权限校验缺失(auth-missing)        | 无 `@Auth` 注解的 API、直接 ID 访问、无角色检查                                    |
| OTHER | 可观测性与运维(observability)      | 关键方法无 `log`、无 `/health` 端点、无 Metrics、无告警规则                          |
| OTHER | 业务连续性与容错(resilience)        | 外部调用无 `CircuitBreaker`、单实例部署、无 `retry`、无 `timeout`                  |
| OTHER | 数据一致性与可靠性(data-consistency) | 分布式写无补偿、消息消费无幂等键、无 DB 备份、迁移无 rollback                               |
| OTHER | 合规与法律风险(compliance)         | 手机号/身份证明文、GPL 依赖在商业项目、未授权第三方 API                                    |
| OTHER | 代码质量与可维护性(code-quality)     | 上帝类、核心逻辑 0% 覆盖率、废弃版本依赖、公共 API 无文档                                   |

## 产出要求

- 每条债务一个独立 .md 文件
- 文件命名：{序号}-{分类简称}-{简述}.md
- 所有债务汇总到 INDEX.md
- 严重程度为 critical 的债务必须在初始化报告中特别标注
