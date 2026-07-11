---
name: "sh-spring"
description: "sh-framework Spring扩展模块知识库。包含SpringContextHolder上下文持有器、SnowflakeHelper雪花ID、Sys系统初始化与环境管理、SystemConfig配置、MailUtil邮件发送、FreeMarkerTemplateUtil模板渲染。当涉及Spring上下文获取Bean、ID生成、邮件发送、模板渲染、系统环境判断时调用。"
---

# sh-spring 模块知识库

sh-spring 是 sh-framework 的 Spring 扩展模块，基于 sh-core 构建，提供 Spring 上下文工具、雪花 ID 生成、邮件发送和 FreeMarker 模板渲染等能力。

## 包结构

```
com.wkclz.spring
├── ShSpringAutoConfig         # 自动配置（@AutoConfiguration + @ComponentScan）
├── config/
│   ├── SpringContextHolder     # Spring上下文静态持有器（@Component）
│   ├── Sys                     # 系统启动初始化与环境管理（@Component, ApplicationRunner）
│   └── SystemConfig            # 系统配置属性（@Configuration）
├── helper/
│   └── SnowflakeHelper         # 雪花ID辅助类（非Bean，静态方法）
└── utils/
    ├── FreeMarkerTemplateUtil  # FreeMarker模板工具（非Bean，静态方法）
    └── MailUtil                # 邮件发送工具（非Bean，实例化使用）
```

## SpringContextHolder — Spring上下文持有器

允许在非Spring管理的类中获取Bean和ApplicationContext。

```java
// 获取ApplicationContext
ApplicationContext ctx = SpringContextHolder.getApplicationContext();

// 按名称获取Bean（自动转型）
MyBean bean = SpringContextHolder.getBean("myBean");

// 按Class类型获取Bean
MyBean bean = SpringContextHolder.getBean(MyBean.class);
```

**实现细节**：
- 实现 `ApplicationContextAware`（自动注入）+ `DisposableBean`（容器关闭时清理）
- `applicationContext` 使用 `volatile` 修饰，保证多线程可见性
- 标注 `@Component` + `@Lazy(false)`，确保容器启动时立即实例化
- 未注入时调用抛出 `RuntimeException`

## SnowflakeHelper — 雪花ID辅助类

```java
// 生成唯一雪花ID（线程安全，synchronized）
long id = SnowflakeHelper.getSnowflakeId();
```

**ID生成策略**：
- 内部封装 `SnowflakeIdWorker`（来自sh-tool）
- Worker ID：本机所有网络接口toString()的hashCode % 31
- Datacenter ID：当前环境`EnvType`的hashCode % 31（不同环境产生不同数据中心ID）
- 懒初始化：首次调用时创建 `SnowflakeIdWorker` 实例
- 线程安全：`getSnowflakeId()` 使用 `synchronized`

## Sys — 系统启动初始化

实现 `ApplicationRunner`，在Spring Boot完全启动后自动执行。

```java
// 获取当前运行环境
EnvType env = Sys.getCurrentEnv();  // DEV/SIT/UAT/PROD

// 获取系统启动时间
Long startupDate = Sys.getStartupDate();

// 系统是否启动完成
boolean confirmed = Sys.getSystemStartUpConfirm();
```

**环境判断逻辑**：从 `spring.profiles.active` 读取激活Profile，按 PROD > UAT > SIT > DEV 优先级匹配 EnvType。

**三个原子变量**：
- `CURRENT_ENV` — 当前环境（默认DEV）
- `STARTUP_DATE` — 启动时间戳
- `SYSTEM_START_UP_CONFIRM` — 启动完成标志

## SystemConfig — 系统配置

| 属性 | 配置键 | 默认值 | 说明 |
|------|--------|--------|------|
| applicationName | spring.application.name | APP | 应用名称 |
| profiles | spring.profiles.active | dev | 激活的Profile |
| configDecryptAesKey | sh.config.decrypt-aes-key | 空 | 配置解密AES密钥 |
| alarmEmailEnabled | alarm.email.enabled | false | 告警邮件启用 |
| alarmEmailHost | alarm.email.host | 空 | SMTP主机 |
| alarmEmailFrom | alarm.email.from | 空 | 发件人 |
| alarmEmailPassword | alarm.email.password | 空 | 邮件密码 |
| alarmEmailTo | alarm.email.to | 空 | 收件人 |

## MailUtil — 邮件发送

**非Spring Bean**，需实例化使用：

```java
MailUtil mailUtil = new MailUtil();
mailUtil.setEmailHost("smtp.example.com");
mailUtil.setEmailFrom("sender@example.com");
mailUtil.setEmailPassword("password");
mailUtil.setToEmails("receiver@example.com");
mailUtil.setSubject("告警通知");
mailUtil.setContent("<h1>系统异常</h1><p>详情...</p>");
mailUtil.sendEmail();
```

**特性**：
- 支持 HTML 内容、内嵌图片（CID）、附件
- 多收件人（逗号/分号/竖线分隔）
- SSL加密连接（MailSSLSocketFactory）
- `toString()` 中密码显示为 `******`
- 使用独立Properties（不污染全局System属性）

## FreeMarkerTemplateUtil — 模板渲染

```java
// 从classpath:/templates/加载模板
Template template = FreeMarkerTemplateUtil.getTemplate("email-template.ftl");

// 从自定义目录加载
Template template = FreeMarkerTemplateUtil.getTemplate("template.ftl", "/path/to/templates");

// 字符串模板渲染
Map<String, Object> params = new HashMap<>();
params.put("name", "张三");
String result = FreeMarkerTemplateUtil.parseString("你好 ${name}", params);

// 清除模板缓存
FreeMarkerTemplateUtil.clearCache();
```

**特性**：
- 默认模板路径：classpath:/templates/
- 编码UTF-8，异常RETHROW_HANDLER，NullCacheStorage（不缓存）
- 使用 `ReentrantLock` 保护模板加载器切换（线程安全）
- 所有IOException转为SystemException

## 哪些是Spring Bean

| 类 | 是否Bean | 使用方式 |
|----|---------|---------|
| SpringContextHolder | 是 | 自动注入或静态方法 |
| Sys | 是 | 静态方法访问 |
| SystemConfig | 是 | 可注入 |
| SnowflakeHelper | 否 | 静态方法调用 |
| FreeMarkerTemplateUtil | 否 | 静态方法调用 |
| MailUtil | 否 | 实例化+setter+sendEmail() |

## 依赖链

```
sh-tool (SnowflakeIdWorker, StringFormat)
  ↑
sh-core (SystemException, EnvType, CommonException)
  ↑
sh-spring (SpringContextHolder, Sys, SystemConfig, SnowflakeHelper, FreeMarkerTemplateUtil, MailUtil)
```
