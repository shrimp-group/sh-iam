# SHA-013：缓存基础设施

## 故事描述

**作为** 接入方开发者
**我想要** AuthCacheManager 管理三层 LoadingCache，自动加载和缓存授权数据
**以便** 认证和授权判断减少对数据库的查询，提升系统吞吐

## 验收标准

1. `AuthCacheManager` 位于 `com.wkclz.auth.cache` 包，作为 Spring Bean 管理
2. 注入 `AuthMetadataService authMetadataService`
3. 管理三层 Guava `LoadingCache`：
   - **metadataCache** — 全局元数据缓存（单例，key=`"METADATA"`），加载方法调用 `authMetadataService.loadMetadata()`，返回 `AuthMetadata`
     - 过期策略：refreshAfterWrite=N 分钟，expireAfterWrite=2N 分钟
     - refresh 监听器：当 `metadataCache` 异步刷新时，级联调用 `resolvedAuthCache.invalidateAll()` 清空计算结果缓存
   - **subjectAuthCache** — 用户级授权缓存（per-user，key=`subjectId`），加载方法调用 `authMetadataService.loadSubjectAuth(principal)`，返回 `SubjectAuthorization`
     - 过期策略：refreshAfterWrite=N 分钟，expireAfterWrite=2N 分钟
   - **resolvedAuthCache** — 计算结果缓存（key=`subjectId:tenantCode:appCode`），加载方法合并 `metadataCache` + `subjectAuthCache` 数据后计算生成 `ResolvedAuthorization`
     - 包含字段：`apiPermissions: Set<AuthPermission>`（可访问的 API 权限集合）、`fieldPermissions: Map<String, List<FieldPermission>>`（API URI → 字段权限列表）、`dataScopes: Map<String, List<DataScope>>`（维度编码 → 数据范围列表）
     - 过期策略：短过期（例如 5 分钟），或手动触发 invalidate
4. 提供以下缓存操作方法：
   - `getMetadata() → AuthMetadata` — 获取全局元数据
   - `getSubjectAuth(Principal) → SubjectAuthorization` — 获取用户授权
   - `getResolvedAuthorization(Principal, String tenantCode, String appCode) → ResolvedAuthorization` — 获取计算结果
   - `refreshMetadata()` — 刷新元数据缓存（主动触发）
   - `evictSubject(String subjectId)` — 清理指定用户的 subjectAuthCache + resolvedAuthCache
   - `evictAll()` — 清空所有缓存
5. `metadataCache` 的 `CacheLoader.load()` 调用 `authMetadataService.loadMetadata()`
6. `subjectAuthCache` 的 `CacheLoader.load()` 调用 `authMetadataService.loadSubjectAuth(principal)`
7. `resolvedAuthCache` 的 `CacheLoader.load()` 先获取 `metadataCache.get()` 和 `subjectAuthCache.get()`，合并计算生成 `ResolvedAuthorization`

## 技术要点

- 使用 Guava `LoadingCache`（框架已引入 guava 依赖）
- `metadataCache.refresh()` 后的 `RemovalListener` 中执行 `resolvedAuthCache.invalidateAll()`
- `resolvedAuthCache` 的计算逻辑：获取用户的所有角色 → 查询角色关联的菜单 API → 与 metadataCache 中的 apiResources 求交集 → 生成 apiPermissions
- 缓存容量设置合理上限（避免内存溢出），metadataCache 设较小容量，subjectAuthCache 按用户数估算
- 通过 `AuthProperties` 配置缓存参数（容量、过期时间等），提供默认值

## 关联故事

- 依赖：SHA-001, SHA-003, SHA-006
- 被依赖：SHA-014
