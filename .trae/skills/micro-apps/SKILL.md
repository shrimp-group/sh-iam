---
name: micro-apps
description: 微应用模块知识总索引——涉及微应用编码、模块选择、依赖管理时调用。自动触发做轻量提示，手动调用展开完整索引。
---

# micro-apps 微应用知识总索引

## 核心规则

1. 当用户涉及 micro-* 微应用模块编码时，自动加载此 skill，根据用户意图匹配子技能
2. 当用户显式调用 `micro-apps` 时，展开完整索引并引导选择
3. 此 SKILL.md 只做路由——具体知识在子目录 `micro-*/SKILL.md` 中
4. 匹配到子技能后，调用对应子 skill（如 `Skill: micro-dict`）获取详细知识

## 可用子技能

### 数据管理类

| 子技能 | 路径 | 适用场景 |
|-------|------|---------|
| micro-dict | `micro-dict/SKILL.md` | 数据字典管理服务。字典类型 CRUD、字典项批量保存、跨环境 Copy/Paste 迁移、多字典查询、缓存刷新、DictCache Redis Pub/Sub 缓存、dictType 命名规范、级联更新、diff 增量写入 |
| micro-seq | `micro-seq/SKILL.md` | 序列号生成模块。按 prefix 生成唯一业务序列号，支持批量生成、自动补零、SERIALIZABLE 隔离防重复 |
| micro-material | `micro-material/SKILL.md` | 物料/素材管理模块。素材 CRUD、分组树管理、版本替换、引用绑定/解绑、所有权转移、选择器查询 |
| micro-form | `micro-form/SKILL.md` | 表单规则管理模块。动态表单定义/输入项 CRUD、表单校验规则/字段验证器配置、AOP 规则拦截自动校验 |

### 文件与文档类

| 子技能 | 路径 | 适用场景 |
|-------|------|---------|
| micro-fileos | `micro-fileos/SKILL.md` | 统一文件对象存储服务。支持阿里云 OSS、AWS S3、S3 兼容协议。文件上传/下载、签名链接、分片上传、Hash 去重、目录管理、Bucket 管理、图片处理参数、添加新 OSS 提供商 |
| micro-pdf | `micro-pdf/SKILL.md` | PDF 模板管理与渲染服务。基于 Thymeleaf + Flying Saucer。PDF 模板 CRUD、模板渲染生成 PDF、Mock 预览、缓存管理 |

### 消息与通知类

| 子技能 | 路径 | 适用场景 |
|-------|------|---------|
| micro-msg | `micro-msg/SKILL.md` | 消息通知模块。消息发送/接收、消息模板管理、用户消息记录与已读标记、用户消息配置 |
| micro-mask | `micro-mask/SKILL.md` | 数据脱敏模块。管理脱敏规则（正则/JS 脚本/兜底），通过 ResponseBodyAdvice 自动对响应 JSON 按 JSONPath 脱敏 |

### 审计与校验类

| 子技能 | 路径 | 适用场景 |
|-------|------|---------|
| micro-audit | `micro-audit/SKILL.md` | 数据变更审计模块。记录实体增删改的变更日志，支持字段级差异对比 |
| micro-rmcheck | `micro-rmcheck/SKILL.md` | 删除合规校验模块。管理删除检查规则与检查项，删除前自动校验数据是否被其他表引用 |

### 支付类

| 子技能 | 路径 | 适用场景 |
|-------|------|---------|
| micro-pay | `micro-pay/SKILL.md` | 支付集成模块。支持微信支付 V3/支付宝/模拟支付的下单、回调、退款、超时取消。含积分支付集成 |

### 积分类

| 子技能 | 路径 | 适用场景 |
|-------|------|---------|
| micro-points | `micro-points/SKILL.md` | 积分账户模块。积分钱包、发放、试算、消费（冻结→异步扣减两阶段）、回退、过期、对账 |

### 微信类

| 子技能 | 路径 | 适用场景 |
|-------|------|---------|
| micro-wxapp | `micro-wxapp/SKILL.md` | 微信小程序模块。小程序登录/用户管理/手机号绑定/配置 CRUD/媒体上传下载 |
| micro-wxmp | `micro-wxmp/SKILL.md` | 微信公众号模块。公众号配置、OAuth2 登录、消息路由/回复、菜单/素材管理、用户关注/取关、JSAPI 签名 |

### 基础设施类

| 子技能 | 路径 | 适用场景 |
|-------|------|---------|
| micro-liteflow | `micro-liteflow/SKILL.md` | LiteFlow 规则引擎模块。规则链（Chain）和脚本（Script）CRUD、配置 SQL 数据源、编写 NodeComponent 节点 |
| micro-fun | `micro-fun/SKILL.md` | 函数管理模块。函数分类树形管理、多语言脚本引擎（JavaScript/Python/Groovy/QLExpress/Ruby）动态执行，函数定义 CRUD、脚本在线测试与运行时调用 |
| micro-k8s | `micro-k8s/SKILL.md` | K8s 集群管理模块。多集群 kubeConfig 配置、通过 Kind 策略模式查询/创建/更新/删除 K8s 资源 |
| micro-autotest | `micro-autotest/SKILL.md` | REST API 自动化测试引擎。运行时扫描 REST 接口、自动 Mock 外部依赖、生成 MD+HTML 测试报告 |
| micro-dbview | `micro-dbview/SKILL.md` | 数据库管理工具。数据源管理、元数据查询、SQL 执行、DDL 操作、数据库权限控制 |

## 模块依赖关系

```
micro-dict      ← micro-form, micro-mask, micro-seq
micro-fileos    ← micro-pdf, micro-wxapp
micro-points    ← micro-pay
```

## 触发行为

### 自动触发（轻量模式）

当用户消息涉及微应用模块编码时：

1. 扫描用户意图，匹配子技能适用场景
2. 匹配到 1-2 个 → 直接调用对应子 skill
3. 匹配模糊 → 简短提示调用 `micro-apps` 查看完整索引

### 手动调用（完整模式）

用户显式 `Use Skill: micro-apps` 时，展示完整索引表和依赖关系，引导选择子技能。
