---
name: "sh-xxljob"
description: "sh-framework XXL-Job定时任务模块知识库。提供XXL-Job执行器自动配置，基于xxl-job-core，支持@XxlJob注解开发任务处理器，appName默认取spring.application.name。当涉及定时任务开发、XXL-Job配置、任务Handler编写时调用。"
---

# sh-xxljob 模块知识库

sh-xxljob 是 sh-framework 的 XXL-Job 分布式定时任务集成模块，为应用提供开箱即用的执行器自动配置。

## 包结构

```
com.wkclz.xxljob
├── XxlJobAutoConfigure    # 自动配置（@AutoConfiguration + @ComponentScan）
├── config/
│   └── XxlJobConfig       # 执行器配置与XxlJobSpringExecutor Bean注册
└── demo/
    └── XxlJobDemo         # 示例Job Handler
```

## XxlJobConfig — 执行器配置

### 配置项

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| xxl.job.admin.addresses | 空 | 调度中心地址（集群逗号分隔；空则关闭自动注册） |
| xxl.job.admin.accessToken | 空 | 通讯令牌 |
| xxl.job.admin.timeout | 3 | 通讯超时（秒） |
| xxl.job.executor.appname | ${spring.application.name} | 执行器AppName（心跳注册分组依据） |
| xxl.job.executor.address | 空 | 执行器注册地址（优先使用，空则自动IP:PORT） |
| xxl.job.executor.ip | 空 | 执行器IP（多网卡时可手动设置） |
| xxl.job.executor.port | 9999 | 执行器端口（多执行器需不同） |
| xxl.job.executor.logpath | ./xxl-job/jobhandler | 日志存储路径 |
| xxl.job.executor.logretentiondays | 30 | 日志保留天数（>=3生效，-1关闭清理） |

### 核心Bean

```java
@Bean
public XxlJobSpringExecutor xxlJobExecutor() {
    XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
    executor.setAdminAddresses(adminAddresses);
    executor.setAppname(appName);
    executor.setTimeout(timeout);
    // ... 设置其他属性
    return executor;
}
```

## 编写Job Handler

```java
@Component
public class MyJobHandler {

    @XxlJob("myBusinessJob")
    public void myBusinessJob() {
        XxlJobHelper.log("开始执行业务任务...");
        // 业务逻辑
        XxlJobHelper.log("业务任务执行完成");
    }

    @XxlJob("myDataSyncJob")
    public ReturnT<String> myDataSyncJob() {
        XxlJobHelper.log("数据同步开始");
        // 业务逻辑
        return ReturnT.SUCCESS;
    }
}
```

**要点**：
- `@XxlJob("handlerName")` 的名称需与调度中心配置一致
- 使用 `XxlJobHelper.log()` 输出日志（同步到调度中心查看）
- 无返回值方法默认SUCCESS

## 配置示例

```yaml
xxl:
  job:
    admin:
      addresses: http://xxl-job-admin:8080/xxl-job-admin
      accessToken: your-token
    executor:
      appname: my-service
      port: 9999
      logpath: ./logs/xxl-job
      logretentiondays: 30
```

## 自动配置原理

```
引入sh-xxljob依赖
  → 读取AutoConfiguration.imports
  → 加载XxlJobAutoConfigure
  → @ComponentScan扫描com.wkclz.xxljob包
  → XxlJobConfig注册为Bean
  → xxlJobExecutor()创建XxlJobSpringExecutor
  → XxlJobDemo注册为Bean（示例Handler）
  → 执行器启动，向调度中心注册
```

## 注意事项

1. **XxlJobDemo会自动注册**：如不需要，需排除扫描或移除@Component
2. **appName智能默认**：默认取spring.application.name，无需重复配置
3. **最小配置**：仅需 `xxl.job.admin.addresses` 一项即可运行
4. **配置元数据为空**：使用@Value而非@ConfigurationProperties，IDE无自动补全
5. **配置前缀为xxl.job**：与框架shrimp.cloud前缀不同，保持XXL-Job官方约定
