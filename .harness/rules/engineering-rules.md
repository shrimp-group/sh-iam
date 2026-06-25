# 工程结构规则

## 模块划分规则

### 模块职责边界

| 模块 | 职责 | 边界 |
|------|------|------|
| iam-common | 公共实体、DTO、工具类 | 不包含业务逻辑 |
| iam-sdk | SDK 能力封装 | 提供给第三方应用使用 |
| iam-sso | SSO 核心业务 | 登录、认证、Token管理 |
| iam-admin | 管理后台业务 | 用户、角色、菜单等CRUD |

### 依赖关系规则

- **禁止循环依赖**：模块之间不能形成循环依赖
- **单向依赖**：高层模块依赖低层模块，反之不行
- **公共依赖**：共用代码应提取到 iam-common

## 目录结构规则

### 标准目录结构

```
module/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/wkclz/iam/module/
│       │       ├── config/      # 配置类
│       │       ├── controller/  # REST控制器
│       │       ├── service/     # 业务服务
│       │       ├── mapper/      # 数据访问
│       │       ├── entity/      # 实体类
│       │       └── dto/         # 数据传输对象
│       └── resources/
│           ├── META-INF/        # 元数据
│           └── mapper/          # MyBatis XML
└── pom.xml                      # Maven配置
```

## 代码组织规则

### 包命名规范

| 包名 | 用途 | 示例 |
|------|------|------|
| config | 配置类 | IamSdkConfig |
| controller/rest | REST控制器 | UserRest |
| service | 业务服务 | IamUserService |
| mapper | 数据访问 | IamUserMapper |
| entity | 实体类 | IamUser |
| dto | 数据传输对象 | IamUserDto |
| helper | 工具辅助类 | PasswordHelper |
| util | 通用工具类 | JwtUtil |
| filter | 过滤器 | IamAuthFilter |
| model | 数据模型 | UserJwt |
| enums | 枚举类 | AuthType |

### 文件命名规则

- **实体类**：`Iam` + 业务名
- **DTO类**：`Iam` + 业务名 + `Dto`
- **Service类**：`Iam` + 业务名 + `Service`
- **Mapper类**：`Iam` + 业务名 + `Mapper`
- **Controller类**：业务名 + `Rest`

## 配置管理规则

### 配置文件组织

```
resources/
├── config/
│   └── application.yml      # 应用配置
└── META-INF/
    └── spring/
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

### 配置项命名规范

| 层级 | 说明 | 示例 |
|------|------|------|
| 模块前缀 | 模块标识 | iam.sso |
| 功能分类 | 功能标识 | iam.sso.password |
| 配置项 | 具体配置 | iam.sso.password.expire-days |

### 自动配置规则

- 使用 `@AutoConfiguration` 注解
- 使用 `@ConditionalOnProperty` 条件装配
- 使用 `@ConditionalOnMissingBean` 允许覆盖

## 安全规则

### 敏感信息保护

- 禁止硬编码敏感信息
- 使用配置文件管理敏感信息
- 日志中禁止打印敏感信息

### 密码安全

- 密码传输使用 RSA 加密
- 密码存储使用 MD5 + salt
- 禁止明文存储密码

### 访问控制

- 公开接口必须在 `/public/` 路径下
- 非公开接口必须进行 Token 校验
- 权限校验基于角色进行

## 数据库规则

### 表命名规则

- 表名使用小写
- 使用下划线分隔
- 前缀为 `iam_`

### 字段命名规则

- 字段名使用小写
- 使用下划线分隔
- 避免使用保留字

### 主键规则

- 使用自增主键
- 类型为 BIGINT UNSIGNED
- 字段名为 `id`

### 通用字段

| 字段名 | 类型 | 说明 |
|--------|------|------|
| create_time | DATETIME | 创建时间 |
| create_by | VARCHAR(64) | 创建人 |
| update_time | DATETIME | 更新时间 |
| update_by | VARCHAR(64) | 更新人 |
| remark | VARCHAR(500) | 备注 |
| version | INT | 乐观锁版本 |
| deleted | TINYINT(1) | 逻辑删除标识 |

## 错误处理规则

### 异常类型

| 异常 | 用途 | HTTP状态码 |
|------|------|-----------|
| ValidationException | 参数校验异常 | 400 |
| UserException | 用户业务异常 | 400 |
| NotFoundException | 资源不存在 | 404 |
| SystemException | 系统异常 | 500 |

### 错误响应格式

```json
{
  "code": 400,
  "message": "错误描述",
  "data": null
}
```

## 测试规则

### 测试覆盖率

- 单元测试覆盖率 >= 80%
- 核心业务必须有测试
- 新增功能必须添加测试

### 测试命名规范

- 测试类：业务名 + `Test`
- 测试方法：`test` + 方法名 + 场景描述

### 测试数据管理

- 使用测试专用数据库
- 测试数据独立管理
- 测试完成后清理数据