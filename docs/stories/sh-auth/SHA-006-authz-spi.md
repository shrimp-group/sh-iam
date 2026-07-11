# SHA-006：授权域 SPI + 基础设施 SPI

## 故事描述

**作为** SPI 接入方
**我想要** 定义授权域和基础设施域的所有 SPI 接口，覆盖权限判断、菜单获取、数据范围、字段权限、请求日志和安全头注入等扩展点
**以便** 接入方可按需实现授权逻辑和基础设施功能，框架层只依赖接口

## 验收标准

1. 授权域 SPI 接口定义在 `com.wkclz.auth.contract.authz` 包，共 4 个接口：
   - `AccessControlProvider` — 访问控制提供者（`hasPermission(Principal, String apiMethod, String apiUri, String tenantCode, String appCode) → boolean`）
   - `MenuProvider` — 菜单提供者（`getMenus(Principal, String appCode) → List<MenuNode>`，`getMenuTree(Principal, String appCode) → List<MenuNode>`）
   - `DataScopeProvider` — 数据范围提供者（`getDataScopes(Principal, String tenantCode) → List<DataScope>`）
   - `FieldPermissionProvider` — 字段权限提供者（`getFieldPermissions(Principal, String apiUri) → List<FieldPermission>`）
2. 基础设施 SPI 接口定义在 `com.wkclz.auth.contract.infra` 包，共 3 个接口：
   - `RequestLogger` — 请求日志记录（`save(RequestRecord)`，异步调用）
   - `SecurityHeaderProvider` — 安全响应头提供者（`getSecurityHeaders() → SecurityHeaders`）
   - `AuthMetadataService` — 认证元数据服务，提供两个核心方法：
     - `loadMetadata() → AuthMetadata` — 加载全局元数据（角色/菜单/API/数据维度/字段权限），供 `metadataCache` 缓存加载
     - `loadSubjectAuth(Principal) → SubjectAuthorization` — 加载用户级授权（角色编码/菜单编码/API权限/数据范围/字段权限），供 `subjectAuthCache` 缓存加载
3. 所有 SPI 接口使用 `@FunctionalInterface` 或单方法接口风格（除 `AuthMetadataService` 外）
4. `RequestLogger.save()` 标注为异步执行（方法本身无返回值，实现方需保证异步处理）

## 技术要点

- 授权域 SPI 与认证域 SPI 分离到不同子包（`contract.auth` vs `contract.authz` vs `contract.infra`）
- `AccessControlProvider` 支持多个实现共存，鉴权时任一拒绝即返回 403
- `AuthMetadataService.loadMetadata()` 返回全局不变的元数据；`loadSubjectAuth()` 返回与特定用户关联的授权数据
- 授权 SPI 的缓存由 SHA-013 的 `AuthCacheManager` 统一管理

## 关联故事

- 依赖：SHA-001, SHA-002, SHA-003
- 被依赖：SHA-009, SHA-011, SHA-012, SHA-013
