# 业务术语表

最后更新：2026-06-19

## 术语列表

| 术语名   | 定义                                     | 英文名                     | 分类   |
|-------|----------------------------------------|-------------------------|------|
| 应用    | 独立的业务系统，拥有独立的角色/菜单/API 体系              | Application             | 通用   |
| 角色    | 权限集合，可关联菜单和数据维度                        | Role                    | 访问控制 |
| 菜单    | 前端页面和按钮的抽象，分为 MENU 和 BUTTON 两种类型       | Menu                    | 访问控制 |
| API   | 后端接口路由，包含方法、路径、模块等信息                   | API                     | 访问控制 |
| 访问密钥  | 用于第三方应用调用的密钥对 (AK/SK)                  | Access Key              | 安全   |
| 数据维度  | 行级数据权限的维度定义                            | Data Dimension          | 数据权限 |
| 认证方式  | 用户登录的认证类型，支持 PASSWORD 和 LDAP           | Auth Type               | 身份认证 |
| 用户编码  | 用户的唯一标识，由 RedisIdGenerator 生成，前缀 user_ | User Code               | 用户管理 |
| 角色编码  | 角色的唯一标识                                | Role Code               | 角色管理 |
| 菜单编码  | 菜单的唯一标识                                | Menu Code               | 菜单管理 |
| SSO   | 单点登录，用户一次登录即可访问所有关联应用                  | Single Sign-On          | 身份认证 |
| JWT   | JSON Web Token，用于无状态身份认证的令牌            | JSON Web Token          | 安全   |
| AK/SK | Access Key / Secret Key，用于 API 调用的密钥对  | Access Key / Secret Key | 安全   |

## 缩写说明

| 缩写   | 全称          | 英文名                                   | 说明        |
|------|-------------|---------------------------------------|-----------|
| IAM  | 身份认证与访问管理   | Identity and Access Management        | 核心业务领域    |
| SSO  | 单点登录        | Single Sign-On                        | 一次登录访问多系统 |
| JWT  | JSON Web 令牌 | JSON Web Token                        | 无状态认证令牌   |
| RBAC | 基于角色的访问控制   | Role-Based Access Control             | 权限模型      |
| AK   | 访问密钥        | Access Key                            | API 调用标识  |
| SK   | 密钥          | Secret Key                            | API 调用密钥  |
| LDAP | 轻量目录访问协议    | Lightweight Directory Access Protocol | 企业目录认证    |
