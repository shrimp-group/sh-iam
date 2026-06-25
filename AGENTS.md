# AGENTS.md — sh-iam 项目导航

sh-iam 是基于 Spring Boot 3.x 的身份认证与访问管理 (IAM) 系统。

## 项目结构

```
sh-iam/
├── iam-common          # 公共实体、DTO、工具类
├── iam-sdk             # SDK 模块（Filter/JWT/Session）
├── iam-sso             # SSO 登录核心业务
├── iam-sso-starter     # SSO 启动器
├── iam-admin           # 管理后台业务
├── iam-admin-starter   # Admin 启动器
└── iam-admin-ui/       # Vue3 管理后台前端
```

## 知识库导航

- [架构文档](docs/architecture/) - 前后端分层、数据流、隐性契约
- [产品规则](docs/product/) - 业务逻辑、功能说明
- [规范文档](docs/standards/) - 编码规范、数据库规范、测试规范

## Harness 配置

- [.harness/agents/](.harness/agents/) - Agent 角色定义
- [.harness/rules/](.harness/rules/) - 规则体系
- [.harness/skills/](.harness/skills/) - 技能体系
- [.harness/changes/](.harness/changes/) - 变更审计追踪

## 核心功能

| 模块 | 功能 |
|------|------|
| SSO | 登录/登出、验证码、注册、JWT鉴权 |
| Admin | 用户/角色/菜单/应用/API/密钥管理 |
| SDK | 鉴权过滤器、Session管理、AK签名 |

## 快速入口

- [CLAUDE.md](CLAUDE.md) - 系统级提示词
- [REVIEW.md](REVIEW.md) - 评审代理提示词
- [scripts/](scripts/) - 自动化验证脚本
- [docs/stories/](docs/stories/) - 详细故事文档