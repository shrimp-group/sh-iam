# sh-auth 用户故事索引

sh-auth 是框架级公共认证-授权-鉴权模块，零 IAM 依赖。通过 SPI 契约支持多种认证方式和授权体系。

| Story ID | 故事名称 | 优先级 | 状态 |
|----------|---------|--------|------|
| SHA-001 | 模块骨架与自动配置 | P0 | ❌ 未实现 |
| SHA-002 | 枚举与异常体系 | P0 | ❌ 未实现 |
| SHA-003 | 核心数据模型（bean） | P0 | ❌ 未实现 |
| SHA-004 | SecurityContext 安全上下文 | P0 | ❌ 未实现 |
| SHA-005 | 认证域 SPI 契约 | P0 | ❌ 未实现 |
| SHA-006 | 授权域 SPI 契约 | P0 | ❌ 未实现 |
| SHA-007 | LoginService 登录模板方法 | P0 | ❌ 未实现 |
| SHA-008 | 过滤器链 — RequestWrapperFilter | P0 | ❌ 未实现 |
| SHA-009 | 过滤器链 — RequestRecordFilter | P0 | ❌ 未实现 |
| SHA-010 | 过滤器链 — AuthenticationFilter | P0 | ❌ 未实现 |
| SHA-011 | 过滤器链 — AuthorizationFilter | P0 | ❌ 未实现 |
| SHA-012 | 过滤器链 — SecurityHeaderFilter | P1 | ❌ 未实现 |
| SHA-013 | 缓存基础设施 — 三层缓存体系 | P0 | ❌ 未实现 |
| SHA-014 | 缓存刷新事件机制 | P1 | ❌ 未实现 |

### 依赖关系

```
SHA-001 (模块骨架)
  ├── SHA-002 (枚举异常) ──→ SHA-003 (数据模型)
  │                              │
  │         ┌────────────────────┤
  │         │                    │
  │    SHA-004 (SecurityContext) │
  │         │                    │
  │    SHA-005 (认证SPI) ──── SHA-006 (授权SPI)
  │         │                    │
  │    SHA-007 (LoginService)    │
  │                              │
  ├── SHA-008 (RequestWrapper)   │
  ├── SHA-009 (RequestRecord)    │
  ├── SHA-010 (Authentication)   │
  ├── SHA-011 (Authorization)    │
  └── SHA-012 (SecurityHeader)   │
                                  │
                         SHA-013 (缓存)
                                  │
                         SHA-014 (缓存刷新)
```
