---
name: "micro-form"
description: "表单规则管理模块。管理动态表单定义/输入项CRUD、表单校验规则/字段验证器配置、AOP规则拦截自动校验。修改 micro-form 包下代码时触发。"
---

# Micro-Form 模块

## 1. 适用场景

当用户需要以下操作时触发此 Skill：

- 管理动态表单定义（MdmForm）及表单输入项（MdmFormItem）的 CRUD
- 配置表单校验规则（MdmFormRule），绑定 API 方法+路径实现 AOP 自动拦截校验
- 管理校验字段（MdmFormRuleField）和字段验证器（MdmFormRuleFieldValidator）
- 管理验证器模板（MdmFormRuleValidatorTemplate），复用正则/函数校验逻辑
- 新增自定义验证器（实现 IValidator 接口，注册到 ValidatorFactory）
- 修改 `com.wkclz.micro.form` 包下任何代码
- 排查表单校验 AOP 拦截不生效、缓存不一致等问题

## 2. 架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                        REST 控制器层                             │
│  FormRest  CommonFormRest  FormRuleRest  CommonFormRuleRest     │
│  FormRuleValidatorRest  FormRuleValidatorTemplateRest           │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                        Service 层                                │
│  MdmFormService          MdmFormItemService                     │
│  MdmFormRuleService      MdmFormRuleFieldService                │
│  MdmFormRuleFieldValidatorService                                │
│  MdmFormRuleValidatorTemplateService  FormTableInfoService      │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                        Mapper 层                                 │
│  MdmFormMapper  MdmFormItemMapper  MdmFormRuleMapper            │
│  MdmFormRuleFieldMapper  MdmFormRuleFieldValidatorMapper        │
│  MdmFormRuleValidatorTemplateMapper                              │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                     AOP + 缓存 + 验证器                          │
│                                                                  │
│  FormRuleAop ──► FormRuleCache ──► ValidatorFactory             │
│                     FormCache         └─ IValidator 实现类       │
│                                        (22 种验证器)             │
└─────────────────────────────────────────────────────────────────┘
```

**核心工作流：AOP 自动校验**

```
HTTP 请求 → FormRuleAop(@Around) → FormRuleCache.getFormRule(method, uri)
                                        │
                                  有规则 ──► JSONPath 提取字段值
                                        │         │
                                        │   ValidatorFactory.getValidator(type)
                                        │         │
                                        │   IValidator.validate(value, dto)
                                        │         │
                                        │   验证失败 → R.error(msg)
                                        │   验证通过 → point.proceed()
                                  无规则 ──► point.proceed()
```

## 3. 核心组件速查

### 实体类（bean/entity）

| 类名 | 表名 | 核心字段 | 说明 |
|------|------|----------|------|
| `MdmForm` | mdm_form | formCode, formName | 表单定义 |
| `MdmFormItem` | mdm_form_item | formCode, itemCode, itemName, inputType, fieldType, dictType, label, min, max, minLength, maxLength, placeholder, required, defaultValue, rules, clearable | 表单输入项 |
| `MdmFormRule` | mdm_form_rule | formRuleCode, formRuleName, apiMethod, apiUri | 表单校验规则（绑定 API） |
| `MdmFormRuleField` | mdm_form_rule_field | formRuleCode, fieldCode, fieldName | 校验规则字段 |
| `MdmFormRuleFieldValidator` | mdm_form_rule_field_validator | formRuleCode, fieldCode, validatorType, validatorPattern, validatorFunction, templateCode, msgTemplate | 字段验证器 |
| `MdmFormRuleValidatorTemplate` | mdm_form_rule_validator_template | templateCode, templateName, validatorPattern, validatorFunction | 验证器模板（复用） |

### DTO 类（bean/dto）

| 类名 | 扩展自 | 扩展字段 | 说明 |
|------|--------|----------|------|
| `MdmFormDto` | MdmForm | itemCount, List\<MdmFormItem\> items | 表单详情（含输入项） |
| `MdmFormItemDto` | MdmFormItem | — | 输入项 DTO |
| `MdmFormRuleDto` | MdmFormRule | itemCount, List\<MdmFormRuleFieldDto\> fields | 规则详情（含字段+验证器） |
| `MdmFormRuleFieldDto` | MdmFormRuleField | List\<MdmFormRuleFieldValidatorDto\> validators | 字段详情（含验证器） |
| `MdmFormRuleFieldValidatorDto` | MdmFormRuleFieldValidator | fieldName | 验证器详情（冗余 fieldName） |
| `MdmFormRuleValidatorTemplateDto` | MdmFormRuleValidatorTemplate | — | 模板 DTO |

### 枚举

| 枚举名 | 值 | 说明 |
|--------|----|------|
| `ValidatorTypeEnum` | REQUIRED, INTEGER_GT/GE/LT/LE, FLOAT_GT/GE/LT/LE, STRING_GT/GE/LT/LE, DATE, DATETIME, TIME, EMAIL, MOBILE, URL, DOMAIN, IP, ID_CARD, JSON, DIY, TEMPLATE | 22 种验证器类型 |

### 验证器接口与实现

| 接口/类 | 方法 | 说明 |
|---------|------|------|
| `IValidator` | `String validate(String value, MdmFormRuleFieldValidatorDto validator)` | 验证器接口，null=通过，否则返回错误信息 |
| `ValidatorFactory` | `getValidator(ValidatorTypeEnum)` / `registerValidator(ValidatorTypeEnum, IValidator)` | 验证器工厂，静态注册+动态注册 |
| `RequiredValidator` | — | 必填校验 |
| `IntegerGtValidator` / `IntegerGeValidator` / `IntegerLtValidator` / `IntegerLeValidator` | validatorPattern 为比较值 | 整数比较 |
| `FloatGtValidator` / `FloatGeValidator` / `FloatLtValidator` / `FloatLeValidator` | validatorPattern 为比较值 | 浮点数比较 |
| `StringGtValidator` / `StringGeValidator` / `StringLtValidator` / `StringLeValidator` | validatorPattern 为长度限制值 | 字符串长度比较 |
| `DateValidator` | yyyy-MM-dd | 日期格式校验 |
| `DatetimeValidator` | yyyy-MM-ddTHH:mm:ss | 日期时间格式校验 |
| `TimeValidator` | HH:mm:ss | 时间格式校验 |
| `EmailValidator` | 使用 RegularTool.isEmail | 邮箱校验 |
| `MobileValidator` | 使用 RegularTool.isMobile | 手机号校验 |
| `UrlValidator` | — | URL 校验 |
| `DomainValidator` | — | 域名校验 |
| `IpValidator` | — | IP 地址校验 |
| `IdCardValidator` | 正则 15/18 位 | 身份证号校验 |
| `JsonValidator` | fastjson2 JSON.parse | JSON 格式校验 |
| `DiyValidator` | validatorPattern(正则) + validatorFunction(JS 函数) | 自定义校验（正则+JS 引擎） |
| `TemplateValidator` | validatorPattern(正则) | 模板校验（正则匹配） |

### 缓存组件

| 类名 | Redis Key | 定时刷新 | 说明 |
|------|-----------|----------|------|
| `FormCache` | `sh:micro:form:cache:time` | 12s 间隔 / 32s 延迟 | 表单+输入项缓存，key=formCode |
| `FormRuleCache` | `sh:micro:formRule:cache:time` | 12s 间隔 / 32s 延迟 | 校验规则缓存，key=method:uri |

### AOP 组件

| 类名 | 切点 | 顺序 | 说明 |
|------|------|------|------|
| `FormRuleAop` | @Controller/@RestController（排除 org.springframework） | `@Order(Integer.MIN_VALUE)` | 环绕通知，拦截所有业务 Controller |

### 自动配置

| 类名 | 注解 | 说明 |
|------|------|------|
| `FormAutoConfig` | @ComponentScan("com.wkclz.micro.form") + @MapperScan("com.wkclz.micro.form.mapper") | 自动配置类 |

## 4. 核心工作流

### 4.1 表单定义管理

创建表单 + 输入项，formCode 自动生成（前缀 `form_`），itemCode 自动生成（前缀 `form_item_`）：

```java
// FormRest: POST /micro-form/form/create
MdmFormDto dto = new MdmFormDto();
dto.setFormName("用户信息表");
List<MdmFormItem> items = new ArrayList<>();
MdmFormItem item = new MdmFormItem();
item.setItemName("username");
item.setInputType("input");
item.setFieldType("string");
item.setLabel("用户名");
item.setRequired(1);
items.add(item);
dto.setItems(items);
// Service 内部: formCode = redisIdGenerator.generateIdWithPrefix("form_")
//              itemCode = redisIdGenerator.generateIdWithPrefix("form_item_")
```

更新表单时，Service 层自动对比新旧 items 实现增量更新（新增/修改/删除）。

### 4.2 校验规则配置

创建校验规则，绑定 API 方法+路径：

```java
// FormRuleRest: POST /micro-form/form/rule/create
MdmFormRule rule = new MdmFormRule();
rule.setFormRuleName("用户创建校验");
rule.setApiMethod("POST");
rule.setApiUri("/api/user/create");
// formRuleCode 自动生成: redisIdGenerator.generateIdWithPrefix("form_rule_")
```

配置字段+验证器（批量保存）：

```java
// FormRuleValidatorRest: POST /micro-form/form/rule/field_and_validator/save
List<MdmFormRuleFieldValidatorDto> validators = new ArrayList<>();

// 必填校验
MdmFormRuleFieldValidatorDto v1 = new MdmFormRuleFieldValidatorDto();
v1.setFormRuleCode("form_rule_xxx");
v1.setFieldCode("username");
v1.setFieldName("用户名");
v1.setValidatorType("REQUIRED");
validators.add(v1);

// 字符串长度校验
MdmFormRuleFieldValidatorDto v2 = new MdmFormRuleFieldValidatorDto();
v2.setFormRuleCode("form_rule_xxx");
v2.setFieldCode("username");
v2.setFieldName("用户名");
v2.setValidatorType("STRING_GE");
v2.setValidatorPattern("3");  // 最小长度3
validators.add(v2);

// 邮箱校验
MdmFormRuleFieldValidatorDto v3 = new MdmFormRuleFieldValidatorDto();
v3.setFormRuleCode("form_rule_xxx");
v3.setFieldCode("email");
v3.setFieldName("邮箱");
v3.setValidatorType("EMAIL");
v3.setMsgTemplate("邮箱格式不正确");
validators.add(v3);

// 自定义JS函数校验
MdmFormRuleFieldValidatorDto v4 = new MdmFormRuleFieldValidatorDto();
v4.setFormRuleCode("form_rule_xxx");
v4.setFieldCode("amount");
v4.setFieldName("金额");
v4.setValidatorType("DIY");
v4.setValidatorFunction("if(amount > 1000) { '金额不能超过1000' } else { true }");
validators.add(v4);
```

### 4.3 AOP 自动拦截校验

`FormRuleAop` 拦截所有 Controller 请求，根据 HTTP method + URI 匹配缓存中的校验规则，使用 JSONPath 提取请求体字段值，逐字段执行验证器：

```java
// FormRuleAop 核心流程
MdmFormRuleDto formRule = formRuleCache.getFormRule(method, uri);
// 无规则 → 直接放行
// 有规则 → JSONPath.eval(args, fieldCode) 提取值
//         → ValidatorFactory.getValidator(validatorType) 获取验证器
//         → validator.validate(value, validatorDto) 执行校验
//         → 失败返回 R.error(msg)，成功继续执行 Controller
```

### 4.4 新增自定义验证器

1. 实现 `IValidator` 接口
2. 在 `ValidatorTypeEnum` 添加枚举值
3. 在 `ValidatorFactory` 静态块注册，或调用 `ValidatorFactory.registerValidator()` 动态注册

```java
// 1. 新增枚举
public enum ValidatorTypeEnum {
    // ...existing
    MY_CUSTOM("自定义描述"),
    ;
}

// 2. 实现验证器
public class MyCustomValidator implements IValidator {
    @Override
    public String validate(String value, MdmFormRuleFieldValidatorDto validator) {
        // 校验逻辑
        return null; // 通过返回 null，失败返回错误信息
    }
}

// 3. 注册到工厂
ValidatorFactory.registerValidator(ValidatorTypeEnum.MY_CUSTOM, new MyCustomValidator());
```

### 4.5 客户端接入接口

```java
// 获取表单下拉选项列表
// GET /micro-form/common/form/list
List<MdmForm> list = mdmFormService.getFormOptions();

// 根据 formCode 获取表单详情（含 items）
// GET /micro-form/common/form/detail?formCode=xxx
MdmFormDto detail = mdmFormService.getCustomFormDetail(entity);

// 根据 apiMethod + apiUri 获取校验规则
// GET /micro-form/common/form/rule?apiMethod=POST&apiUri=/api/user/create
List<MdmFormRuleFieldValidatorDto> validators = service.getFormRuleFieldValidatorList4Check(method, uri);
```

## 5. 配置项

本模块无独立 application.yml 配置项，依赖以下框架级配置：

| 配置 | 来源 | 说明 |
|------|------|------|
| Spring Redis | sh-redis | FormCache / FormRuleCache 使用 StringRedisTemplate |
| Spring AOP | spring-boot-starter-aop | FormRuleAop 环绕通知 |
| PageHelper | sh-mybatis | 分页查询 |
| Jackson ObjectMapper | Spring Boot 自动配置 | FormRuleAop 序列化请求参数 |

缓存刷新机制：
- 定时刷新：`@Scheduled(fixedDelay = 12_000, initialDelay = 32_000)` 每 12 秒检查 Redis 标识
- 主动刷新：数据变更时调用 `clearCache()`，写入 Redis 时间戳，其他实例通过定时任务感知
- 防抖：5 秒内不重复初始化（`init()` 方法内 synchronized + 时间判断）

## 6. 依赖

### Maven 依赖

| 依赖 | 说明 |
|------|------|
| `sh-mybatis` | BaseMapper / BaseService / PageQuery / TableInfoService |
| `sh-redis` | StringRedisTemplate / RedisIdGenerator |
| `spring-boot-starter-aop` | AOP 支持 |

### 模块间依赖

| 依赖模块 | 说明 |
|----------|------|
| `micro-dict` | MdmFormItem.dictType 引用字典类型（表单输入项字典选项） |

### 内部使用的关键工具

| 工具 | 来源 | 用途 |
|------|------|------|
| `JSONPath` (fastjson2) | fastjson2 | FormRuleAop 提取请求体字段值 |
| `JsUtil` | sh-tool | DiyValidator 执行 JS 函数 |
| `RegularTool` | sh-tool | EmailValidator / MobileValidator 正则校验 |
| `RedisIdGenerator` | sh-redis | 生成 formCode / itemCode / formRuleCode |

## 7. 常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| AOP 校验不生效 | FormRuleCache 未加载规则 | 检查 mdm_form_rule 表中 apiMethod + apiUri 是否与实际请求匹配；确认 FormAutoConfig 被 Spring 扫描到 |
| 校验规则修改后不生效 | 缓存未刷新 | 确认 clearCache() 被调用；检查 Redis Pub/Sub 通信是否正常；等待 12 秒定时刷新 |
| JSONPath 提取字段值为 null | fieldCode 格式不对 | fieldCode 不以 `$` 开头时自动补 `$.` 前缀；确认请求体 JSON 结构与 fieldCode 路径匹配 |
| 自定义验证器不生效 | 未注册到 ValidatorFactory | 在 ValidatorFactory 静态块注册，或调用 registerValidator() |
| DiyValidator JS 函数执行失败 | validatorFunction 语法错误 | JsUtil 执行 JS，返回 "true" 表示通过，其他值作为错误信息 |
| 表单编码重复 | formCode 唯一约束 | MdmFormService.duplicateCheck() 校验 formCode 唯一性 |
| 删除规则后关联数据残留 | 需级联删除 | MdmFormRuleService.customRemove() 已处理级联删除 Field + FieldValidator |
| FormRuleAop 拦截了框架内部接口 | 切点排除不足 | 切点已排除 `org.springframework..*`，若需排除其他包需修改 POINT_CUT |
| 缓存频繁刷新 | 多实例同时 clearCache | 5 秒防抖 + Redis 时间戳比较（1 秒容差）避免重复初始化 |
