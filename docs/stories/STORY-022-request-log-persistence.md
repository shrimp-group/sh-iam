# STORY-022 — 请求日志持久化服务

| 属性 | 值 |
|------|-----|
| Story ID | STORY-022 |
| 所属 Epic | SSO 登录认证模块 |
| 所属模块 | iam-sso |
| 优先级 | P1 |
| 状态 | 待开发 |

## 用户故事

**作为** 系统运维人员，**我希望** SSO 服务端能将 SDK 采集的请求日志持久化到数据库，**以便** 在管理后台查询和分析请求日志。

## 验收标准

1. `SsoFacadeImpl` 实现 `SsoFacade` 接口，覆盖 SDK 默认实现
2. `saveLog(RequestLog)` 委托给 `IamRequestService.insertLog()`
3. `IamRequestService.insertLog(RequestLog)` 将 RequestLog 转为 IamRequestLog 并入库
4. 字段映射：将 SDK RequestLog 的 30+ 个字段逐一映射到数据库实体
5. IP 归属地解析：调用 `IpLocalCacheAdapter.offerQueue()` 异步获取
6. 设置审计字段：若 userCode 不为空，设置 createBy/updateBy
7. 调用 `ssoRequestLogMapper.insertLog()` 入库

## 技术实现要点

- 可替换设计：SDK 默认通过远程调用保存日志，SSO 服务端直接写库
- `@Component` 注册，自动覆盖 SDK 中 `@ConditionalOnMissingBean` 的默认实现
- 字段映射涵盖：租户/应用信息、浏览器信息、系统信息、HTTP 请求信息、用户信息、性能信息、响应信息
- IP 归属地异步解析，不阻塞请求处理

## 依赖故事

- STORY-014（SsoFacade 接口定义）
- STORY-004（IP 归属地缓存工具）
- STORY-002（IamRequestLog 实体定义）

## 涉及文件

| 文件 | 路径 |
|------|------|
| SsoFacadeImpl (SSO) | iam-sso/src/main/java/com/wkclz/iam/sso/service/SsoFacadeImpl.java |
| IamRequestService | iam-sso/src/main/java/com/wkclz/iam/sso/service/IamRequestService.java |
| SsoRequestLogMapper | iam-sso/src/main/java/com/wkclz/iam/sso/mapper/SsoRequestLogMapper.java |
