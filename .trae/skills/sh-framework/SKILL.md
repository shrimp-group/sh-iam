---
name: sh-framework
description: sh-framework 框架知识总索引——涉及框架编码、模块选择、依赖管理时调用。自动触发做轻量提示，手动调用展开完整索引。
---

# sh-framework 框架知识总索引

## 核心规则

1. 当用户涉及 sh-framework 框架编码时，自动加载此 skill，根据用户意图匹配子技能
2. 当用户显式调用 `sh-framework` 时，展开完整索引并引导选择
3. 此 SKILL.md 只做路由——具体知识在子目录 `sh-*/SKILL.md` 中
4. 匹配到子技能后，调用对应子 skill（如 `Skill: sh-tool`）获取详细知识

## 可用子技能

| 子技能                     | 路径                                 | 适用场景                                                                                                                                                          |
|-------------------------|------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| sh-tool                 | `sh-tool/SKILL.md`                 | 涉及加密（AES/DES/RSA/MD5/SHA/Base64）、字符串格式化、日期、Bean 操作、文件 IO、网络、雪花 ID、验证码、二维码、JS 引擎等工具类操作时                                                                        |
| sh-core                 | `sh-core/SKILL.md`                 | 涉及实体体系（DbColumnEntity/BaseEntity/PageData/UserInfo）、异常体系（CommonException 及 7 个子类）、ResultCode 枚举、R 统一响应、UserContext 用户上下文、自定义注解时                               |
| sh-mybatis              | `sh-mybatis/SKILL.md`              | 涉及数据库操作、Mapper 编写、SQL 生成、BaseMapper/BaseService 使用、拦截器、逻辑删除、乐观锁、@Blob 大字段、分页查询时                                                                               |
| sh-spring               | `sh-spring/SKILL.md`               | 涉及 Spring 上下文获取 Bean、雪花 ID 生成、邮件发送、FreeMarker 模板渲染、系统环境判断、敏感配置加解密时                                                                                            |
| sh-redis                | `sh-redis/SKILL.md`                | 涉及 Redis 缓存操作（String/Hash/List/Set/ZSet）、分布式锁、ID 生成、消息队列时                                                                                                     |
| sh-web                  | `sh-web/SKILL.md`                  | 涉及全局异常处理、IP 解析、请求/响应工具、REST 接口元数据扫描、用户名自动填充、@AtLeastOneNotNull 校验时                                                                                            |
| sh-dynamicdb            | `sh-dynamicdb/SKILL.md`            | 涉及多数据源切换、运行时动态添加/销毁数据源、租户隔离数据源时                                                                                                                               |
| sh-mqtt                 | `sh-mqtt/SKILL.md`                 | 涉及 MQTT 消息收发、IoT 设备通信、@MqttController 注解驱动订阅/分发时                                                                                                              |
| sh-xxljob               | `sh-xxljob/SKILL.md`               | 涉及 XXL-Job 定时任务开发、@XxlJob 注解任务处理器编写时                                                                                                                          |
| sh-iam-contract-api     | `sh-iam-contract-api/SKILL.md`     | 涉及 IAM 契约接口（AuthContract/AuthzContract/AkSignContract/SsoFacadeContract）、Principal/Session/AuthResult 等模型、PrincipalContext、LoginFailType 枚举、AuthException 异常时 |
| sh-iam-contract-default | `sh-iam-contract-default/SKILL.md` | 涉及 IAM 默认实现（DefaultAuthFilter）、@ConditionalOnMissingBean 替换默认实现、IAM 自动配置时                                                                                     |
| sh-bom                  | `sh-bom/SKILL.md`                  | 涉及依赖版本管理、新增第三方依赖、版本冲突排查时                                                                                                                                      |
| sh-demo                 | `sh-demo/SKILL.md`                 | 需要参考框架标准使用范式（Entity→Mapper→Service→VO→Route→Controller CRUD 全链路）时                                                                                             |

## 模块依赖层级

```
第0层: sh-tool（无内部依赖）
第1层: sh-core → sh-tool
第2层: sh-mybatis → sh-core, sh-spring → sh-core
第3层: sh-dynamicdb → sh-mybatis + sh-spring
       sh-redis → sh-core
       sh-web / sh-mqtt / sh-xxljob → sh-spring
       sh-iam-contract（api 零内部依赖；default → api）
第4层: sh-demo → sh-mybatis + sh-web
```

## 触发行为

### 自动触发（轻量模式）

当用户消息涉及 sh-framework 框架编码时：

1. 扫描用户意图，匹配子技能适用场景
2. 匹配到 1-2 个 → 直接调用对应子 skill
3. 匹配模糊 → 简短提示调用 `sh-framework` 查看完整索引

### 手动调用（完整模式）

用户显式 `Use Skill: sh-framework` 时，展示完整索引表和依赖层级，引导选择子技能。
