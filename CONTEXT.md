# sh-iam 项目上下文

> 本文件为 AI 助手提供项目全局上下文，帮助 AI 快速理解项目全貌。

## 项目概述

sh-iam 是基于 Spring Boot 3.x 的身份认证与访问管理 (IAM) 系统，提供 SSO 登录、JWT 鉴权、用户/角色/菜单/应用/密钥管理等完整能力。项目版本
5.0.0-SNAPSHOT，Java 25，继承框架父 POM sh-parent。

## 技术栈

- 语言：Java 25
- 运行时：JDK 25
- 框架：Spring Boot 3.x
- 构建：Maven
- 测试：JUnit 5
- ORM：MyBatis (sh-mybatis)
- 缓存：Redis (sh-redis + Lettuce)
- 定时任务：XXL-Job
- 前端：Vue3 (iam-admin-ui, iam-sso-ui)

## 核心业务领域

- **身份认证**：用户名密码登录、LDAP 登录、SSO 单点登录
- **访问控制**：基于角色的访问控制 (RBAC)，用户-角色-菜单权限链
- **菜单资源管理**：菜单树管理、菜单-API 绑定、若依格式菜单树适配
- **应用管理**：多应用隔离，应用级角色/菜单/API/AK 管理
- **密钥管理**：访问密钥 (AK/SK) 管理，AK-API 绑定
- **数据权限**：角色-数据维度关联，行级数据权限控制
- **审计日志**：登录日志、请求日志采集与查询

## 关键约束

- 密码加密采用 MD5+salt 方式，前端密码通过 RSA 加密传输
- JWT 使用 HS256 签名，默认 24 小时过期，支持时钟偏差容忍
- 所有表使用逻辑删除 (deleted 字段) 和乐观锁 (version 字段)
- Redis Key 命名遵循 `iam:模块:业务:标识` 模式
- REST API 路由必须在 Route 接口中定义常量
- 请求参数封装 Req 对象，返回内容封装 Resp 对象
- 统一使用 R 封装响应

## 外部依赖

- **sh-parent (BOM)**：统一管理第三方依赖版本 (guava, fastjson2, jjwt 等)
- **sh-framework**：sh-web, sh-mybatis, sh-redis, sh-xxljob, sh-spring, sh-tool, sh-core, sh-bom, sh-dynamicdb, sh-mqtt
- **micro-dict**：字典服务
- **XXL-Job**：分布式定时任务调度
- **Redis**：会话存储、验证码、分布式锁、ID 生成
- **MySQL**：主数据库
