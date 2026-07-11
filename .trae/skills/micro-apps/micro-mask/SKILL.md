---
name: "micro-mask"
description: "数据脱敏模块。管理脱敏规则(正则/JS脚本/兜底)，通过 ResponseBodyAdvice 自动对响应 JSON 按 JSONPath 脱敏。修改 micro-mask 包代码时触发。"
---

# Micro-Mask 模块

## 1. 适用场景

- 需要对 REST API 响应中的敏感字段（手机号、身份证等）进行自动脱敏
- 需要新增、修改、删除脱敏规则（`MdmMaskRule`）
- 需要调试脱敏效果（测试/验证接口）
- 修改 `com.wkclz.micro.mask` 包下任何代码时
- 排查脱敏不生效、脱敏规则匹配异常等问题

---

## 2. 架构概览

```
请求响应流程:
  Client → Controller → Service → DB
                                    ↓ 返回
  ResponseBodyAdvice (MaskResponseAdvice)
      ↓ 拦截响应
  序列化为 JSON → 匹配脱敏规则(JSONPath) → 执行脱敏(正则/JS/兜底) → 返回脱敏后数据

组件层级:
  ┌─────────────────────────────────────────────────┐
  │  REST 层                                        │
  │  MaskRuleRest (CRUD)    MaskMockRest (测试/验证) │
  └──────────────┬──────────────────┬───────────────┘
                 ↓                  ↓
  ┌─────────────────────────────────────────────────┐
  │  Service 层                                     │
  │  MdmMaskRuleService (extends BaseService)       │
  └──────────────┬──────────────────────────────────┘
                 ↓
  ┌─────────────────────────────────────────────────┐
  │  Mapper 层                                      │
  │  MdmMaskRuleMapper (extends BaseMapper)         │
  └──────────────┬──────────────────────────────────┘
                 ↓
  ┌─────────────────────────────────────────────────┐
  │  缓存层                                         │
  │  MaskCache (本地缓存 + Redis Pub/Sub 同步)       │
  └─────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────┐
  │  拦截层 (核心)                                   │
  │  MaskResponseAdvice (ResponseBodyAdvice)         │
  │  - Guava Cache 按用户+方法+URI 缓存规则匹配结果  │
  │  - 递归遍历 JSON，按 JSONPath 定位字段           │
  │  - 三种脱敏策略: 正则 > JS脚本 > 兜底            │
  └─────────────────────────────────────────────────┘
```

---

## 3. 核心组件速查

| 类                    | 包路径                    | 说明                                                             |
|----------------------|------------------------|----------------------------------------------------------------|
| `MaskAutoConfig`     | `com.wkclz.micro.mask` | 自动配置类，`@ComponentScan` + `@MapperScan`                         |
| `MdmMaskRule`        | `bean.entity`          | 脱敏规则实体，对应表 `mdm_mask_rule`，extends BaseEntity                  |
| `MdmMaskRuleDto`     | `bean.dto`             | 规则 DTO，扩展 `maskValue`(脱敏结果) 和 `maskType`(脱敏方式描述)               |
| `MdmMaskRuleMapper`  | `mapper`               | Mapper 接口，extends BaseMapper，含 `getMaskRuleList`、`rules4Cache` |
| `MdmMaskRuleService` | `service`              | 业务服务，extends BaseService，含 CRUD + 唯一性校验                        |
| `MaskCache`          | `cache`                | 本地规则缓存，12s 定时轮询 Redis 检测变更，5s 防抖                               |
| `MaskResponseAdvice` | `config`               | 核心！`@ControllerAdvice` 实现 `ResponseBodyAdvice`，自动脱敏响应          |
| `MaskRuleRest`       | `rest`                 | 脱敏规则 CRUD 接口                                                   |
| `MaskMockRest`       | `rest`                 | 脱敏测试/验证接口                                                      |
| `Route`              | `rest`                 | 路由常量定义，prefix = `/micro-mask`                                  |

### MdmMaskRule 关键字段

| 字段                | 类型      | 说明                             |
|-------------------|---------|--------------------------------|
| `maskRuleCode`    | String  | 脱敏规则编码（唯一，自动生成前缀 `mask_rule_`） |
| `maskRuleName`    | String  | 脱敏规则名称                         |
| `requestMethod`   | String  | 匹配的 HTTP 方法（GET/POST 等）        |
| `requestUri`      | String  | 匹配的请求路径，支持 AntPathMatcher 模式   |
| `maskJsonPath`    | String  | 脱敏字段的 JSONPath 表达式             |
| `maskRuleRegular` | String  | 脱敏正则表达式（优先级最高）                 |
| `maskRuleScript`  | String  | JS 脱敏脚本（优先级次之）                 |
| `enableFlag`      | Integer | 启用状态（1=启用）                     |
| `mockValue`       | String  | 示例值（用于测试）                      |

---

## 4. 核心工作流

### 4.1 脱敏规则 CRUD

```java
// 新增规则（必填: maskRuleName, requestMethod, requestUri, maskJsonPath）
MdmMaskRule rule = new MdmMaskRule();
rule.setMaskRuleName("手机号脱敏");
rule.setRequestMethod("GET");
rule.setRequestUri("/api/user/**");
rule.setMaskJsonPath("$.data.mobile");
rule.setMaskRuleRegular("(\\d{3})\\d{4}(\\d{4})");
// rule.setMaskRuleScript("function mask(v){ return v.substring(0,3)+'****'+v.substring(7); }");
rule.setEnableFlag(1);
mdmMaskRuleService.create(rule);
// maskRuleCode 若为空则自动生成: "mask_rule_xxx"
```

### 4.2 响应自动脱敏流程

`MaskResponseAdvice.beforeBodyWrite()` 执行逻辑：

1. **跳过基础类型** — 原始类型、包装类、BigDecimal、Date 等直接返回
2. **构建缓存 Key** — `userCode:METHOD:uri`，Guava Cache 缓存规则匹配结果（12h 过期）
3. **匹配规则** — 遍历 `MaskCache` 中所有规则，匹配 `requestMethod` + `requestUri`（AntPathMatcher）
4. **序列化 + JSONPath 定位** — 将响应序列化为 JSON，按 `maskJsonPath` 递归定位目标字段
5. **执行脱敏** — 优先级：`maskRuleRegular` > `maskRuleScript` > 兜底规则
6. **写回** — 通过 `JSONPath.set()` 将脱敏值写回 JSON 对象

### 4.3 三种脱敏策略

```java
// 1. 正则脱敏（优先级最高）— 匹配部分替换为等长 *
// 例: (\\d{3})\\d{4}(\\d{4}) → 匹配 "13812342222" → 替换为 "138**********" (等长*)
String result = MaskResponseAdvice.maskByString("13812342222", rule);

// 2. JS 脚本脱敏（优先级次之）— 通过 JsUtil.exec 执行
// rule.setMaskRuleScript("function mask(v){ return v.substring(0,3)+'****'+v.substring(7); }");
String result = MaskResponseAdvice.maskByString("13812342222", rule);

// 3. 兜底脱敏 — 长度<4 返回 "***"，长度<8 返回 首字符+"****"，否则 前3位+"****"
String result = MaskResponseAdvice.maskByString("13812342222", rule);
// → "138****"
```

### 4.4 缓存同步机制

```
MaskCache (规则缓存):
  - 本地 Map<String, MdmMaskRule> CACHE_ITEM，key = maskRuleCode
  - 12s 定时轮询 Redis key "sh:micro:mask:cache:time"
  - 本地时间 > Redis 时间 1s 以上 → 不更新
  - 超过 60s 的变更通知 → 不更新
  - init() 加 synchronized，5s 防抖

MaskResponseAdvice (规则匹配缓存):
  - Guava Cache<String, List<MdmMaskRule>>，key = "userCode:METHOD:uri"
  - 最大 1024 条，12h 过期
  - 由 MaskCache.CLEAR_FLAG 驱动清理（12s 定时检查）

CRUD 操作后手动触发:
  maskCache.clearCache();        // 写 Redis 时间戳 + 立即 init
  MaskResponseAdvice.clearCache(); // 设置 CLEAR_FLAG，下次定时清理 Guava Cache
```

### 4.5 测试与验证接口

```java
// 测试: POST /micro-mask/rule/test
MdmMaskRuleDto dto = new MdmMaskRuleDto();
dto.setMockValue("13812342222");
dto.setMaskRuleRegular("(\\d{3})\\d{4}(\\d{4})");
// 返回 dto.maskValue = 脱敏结果, dto.maskType = "使用正则表达式进行匹配脱敏！"

// 验证: GET /micro-mask/rule/verify
// 返回内置 mock JSON，可配合已配置的规则验证脱敏效果
```

---

## 5. 配置项

本模块无独立配置文件，依赖以下基础设施配置：

| 配置                   | 来源                            | 说明                                       |
|----------------------|-------------------------------|------------------------------------------|
| Redis 连接             | `spring.data.redis.*`         | MaskCache 使用 StringRedisTemplate 同步缓存变更  |
| MyBatis 配置           | `sh-mybatis`                  | BaseMapper 自动配置                          |
| Jackson ObjectMapper | `spring-boot-starter-jackson` | MaskResponseAdvice 使用默认 ObjectMapper 序列化 |
| IAM SDK              | `iam-contract-api`            | PrincipalContext 获取 userCode 用于缓存 Key    |
| AntPathMatcher       | `sh-web RequestHelper`        | URI 模式匹配                                 |

缓存参数（硬编码）：

| 参数               | 值    | 位置                                |
|------------------|------|-----------------------------------|
| 规则缓存轮询间隔         | 12s  | `MaskCache.autoReflash()`         |
| 规则缓存防抖           | 5s   | `MaskCache.init()`                |
| Redis 缓存 Key TTL | 1min | `MaskCache.clearCache()`          |
| 匹配缓存最大条目         | 1024 | `MaskResponseAdvice.CACHE`        |
| 匹配缓存过期           | 12h  | `MaskResponseAdvice.CACHE`        |
| 匹配缓存清理轮询         | 12s  | `MaskResponseAdvice.clearCache()` |

---

## 6. 依赖

### 框架依赖

| 依赖                            | 用途                                                    |
|-------------------------------|-------------------------------------------------------|
| `sh-mybatis`                  | BaseMapper、BaseService、PageQuery                      |
| `sh-redis`                    | RedisIdGenerator（生成 maskRuleCode）、StringRedisTemplate |
| `sh-web`                      | RequestHelper.match（AntPathMatcher）、RequestHelper     |
| `iam-contract-api`            | PrincipalContext.getUserCode（获取当前用户标识）                |
| `spring-boot-starter-jackson` | ObjectMapper 序列化响应                                    |

### 模块间依赖

- 无直接依赖其他 micro-* 模块

### 数据库表

- `mdm_mask_rule` — 脱敏规则表，继承 BaseEntity 标准字段

---

## 7. 常见问题

| 问题                | 原因                                               | 解决                                                                                     |
|-------------------|--------------------------------------------------|----------------------------------------------------------------------------------------|
| 脱敏不生效             | 规则 `enableFlag` 未设为 1                            | 确保 `enable_flag = 1`，`rules4Cache` 只查启用规则                                              |
| 新增规则后未立即生效        | Guava Cache 有 12h 过期                             | CRUD 操作已自动调用 `maskCache.clearCache()` + `MaskResponseAdvice.clearCache()`，最多等 12s 轮询清理 |
| JSONPath 匹配不到字段   | 路径写法错误或不支持                                       | 数组响应必须以 `$[*]` 开头；使用 `/rule/verify` 接口验证                                               |
| 正则脱敏结果不符合预期       | 正则匹配的是子串，替换为等长 `*`                               | 检查正则是否正确匹配目标部分，用 `/rule/test` 接口调试                                                     |
| JS 脚本脱敏报错         | 脚本语法错误                                           | JsUtil.exec 执行 JS，确保脚本函数签名正确                                                           |
| 兜底脱敏截断过多          | 默认策略：`<4` 返回 `***`，`<8` 返回首字符+`****`，否则前3+`****` | 配置正则或 JS 脚本实现自定义脱敏逻辑                                                                   |
| URI 匹配不生效         | `requestUri` 支持 AntPathMatcher                   | 使用 `**`、`*`、`?` 通配符，如 `/api/user/**`                                                   |
| 响应是基础类型未被脱敏       | `MaskResponseAdvice` 跳过原始类型和包装类                  | 这是设计行为，基础类型无需脱敏                                                                        |
| 缓存不一致             | 多实例部署时 Redis Pub/Sub 延迟                          | `MaskCache` 每 12s 轮询 Redis 时间戳，最多 12s 延迟                                               |
| `maskRuleCode` 重复 | 手动指定了已存在的编码                                      | `MdmMaskRuleService.duplicateCheck()` 会抛出 `RECORD_DUPLICATE`；留空则自动生成                   |
