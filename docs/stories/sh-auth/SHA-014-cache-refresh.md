# SHA-014：缓存刷新事件

## 故事描述

**作为** IAM admin 开发者
**我想要** 在管理后端的 CRUD 操作后发布缓存刷新事件，通知 sh-auth 框架自动刷新对应层级的缓存
**以便** 角色、菜单、权限变更后立即生效，无需手动重启服务

## 验收标准

1. 定义 `AuthCacheRefreshEvent` 事件类，位于 `com.wkclz.auth.cache.event` 包：
   - 继承 `ApplicationEvent`
   - 包含字段：`scope: RefreshScope`（METADATA/SUBJECT/ALL）、`subjectId: String`（仅 SUBJECT 事件时携带）、`source: Object`
   - 提供静态工厂方法：`metadataEvent(Object source)`、`subjectEvent(Object source, String subjectId)`、`allEvent(Object source)`
2. 定义 `AuthCacheRefreshListener` 监听器，位于 `com.wkclz.auth.cache.listener` 包：
   - 实现 `ApplicationListener<AuthCacheRefreshEvent>`
   - 注入 `AuthCacheManager authCacheManager`
   - `onApplicationEvent()` 方法根据 `scope` 分发：
     - `METADATA` → `authCacheManager.refreshMetadata()`（级联清空 `resolvedAuthCache`）
     - `SUBJECT` → `authCacheManager.evictSubject(event.getSubjectId())`（仅清理指定用户）
     - `ALL` → `authCacheManager.evictAll()`（清空所有缓存）
3. 事件发布方（IAM admin 模块）通过 `ApplicationEventPublisher.publishEvent(event)` 发布事件：
   - 角色 CRUD（创建/更新/删除）→ 发布 `METADATA` 事件
   - 菜单 CRUD → 发布 `METADATA` 事件
   - API CRUD → 发布 `METADATA` 事件
   - 角色-菜单绑定 → 发布 `METADATA` 事件
   - 角色-用户绑定 → 发布 `SUBJECT(subjectId)` 事件
   - 角色-数据权限绑定 → 发布 `METADATA` 事件
   - 用户状态变更 → 发布 `SUBJECT(subjectId)` 事件
   - 批量用户操作 → 发布 `ALL` 事件或逐个发布 `SUBJECT` 事件
4. `METADATA` 事件 → 触发 `refreshMetadata()`，内部级联清空 `resolvedAuthCache`
5. `SUBJECT` 事件 → 仅清理 `subjectAuthCache` 和 `resolvedAuthCache` 中该用户的条目，`metadataCache` 不受影响
6. 监听器方法异步执行（使用 `@Async` 或 `@EventListener` + `@Async`），不阻塞 CRUD 主流程

## 技术要点

- 使用 Spring 事件机制：`ApplicationEventPublisher` + `@EventListener` 或 `ApplicationListener`
- 事件发布方位于 IAM admin 模块（或接入方），sh-auth 只定义事件类和监听器
- 监听器必须异步执行，避免影响 CRUD 接口响应时间
- `SUBJECT` 事件携带具体 `subjectId`，避免误清其他用户缓存
- `METADATA` 事件频繁时可考虑防抖（debounce），但本期不实现

## 关联故事

- 依赖：SHA-001, SHA-002, SHA-013
- 被依赖：无（为 IAM admin CRUD 故事提供缓存刷新能力）
