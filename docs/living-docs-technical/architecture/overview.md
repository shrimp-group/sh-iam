# 架构概览

## 系统架构图

```mermaid
graph TB
    subgraph 前端
        AdminUI[iam-admin-ui<br/>Vue3 管理后台]
        SsoUI[iam-sso-ui<br/>Vue3 SSO 登录]
    end

    subgraph 应用层
        AdminStarter[iam-admin-starter<br/>Admin 启动器]
        SsoStarter[iam-sso-starter<br/>SSO 启动器]
    end

    subgraph 业务层
        Admin[iam-admin<br/>管理后台业务]
        Sso[iam-sso<br/>SSO 核心业务]
    end

    subgraph 基础层
        Sdk[iam-sdk<br/>SDK 鉴权/日志]
        Common[iam-common<br/>公共实体/DTO]
    end

    subgraph 基础设施
        Redis[(Redis)]
        MySQL[(MySQL)]
        XXLJob[XXL-Job]
    end

    AdminUI --> AdminStarter
    SsoUI --> SsoStarter
    AdminStarter --> Admin
    SsoStarter --> Sso
    Admin --> Sso
    Sso --> Sdk
    Sso --> Common
    Admin --> Common
    Sdk --> Common
    Sso --> Redis
    Sso --> MySQL
    Admin --> MySQL
    Admin --> XXLJob
```

## 架构描述

sh-iam 采用分层架构设计，分为前端层、应用层、业务层和基础层：

- **前端层**：Vue3 SPA 应用，管理后台和 SSO 登录各自独立
- **应用层**：Spring Boot 可部署启动器，分别对应 Admin 和 SSO 两个独立服务
- **业务层**：核心业务逻辑，Admin 依赖 SSO，SSO 依赖 SDK 和 Common
- **基础层**：SDK 提供鉴权/日志过滤器供第三方应用集成，Common 提供公共实体和工具

## 核心技术组件说明

| 组件     | 说明               | 技术选型                      |
|--------|------------------|---------------------------|
| 鉴权过滤器  | JWT + Redis 会话校验 | IamAuthFilter             |
| 请求日志   | AK 签名 + 远程保存     | LoggingFilter + SsoFacade |
| 密码加密   | MD5 + salt       | PasswordHelper            |
| JWT 工具 | HS256 签名         | JwtUtil                   |
| 会话管理   | Redis 存储         | SessionHelper             |
| ID 生成  | 时间戳 + Redis 自增   | RedisIdGenerator          |
| API 扫描 | @Router 注解扫描     | RestfulScan               |

## 组件交互流程

请求进入后依次经过 RequestWrapperFilter → LoggingFilter → IamAuthFilter，鉴权通过后进入 Controller → Service → Mapper
处理链路。
