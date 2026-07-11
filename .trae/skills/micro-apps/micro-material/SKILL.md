---
name: "micro-material"
description: "物料/素材管理模块。当需要实现素材CRUD、分组树管理、版本替换、引用绑定/解绑、所有权转移、选择器查询，或修改 micro-material 包下代码时触发。"
---

# Micro-Material 模块

## 1. 适用场景

当用户需要以下操作时触发此 Skill：

- 实现素材（物料）的上传创建、批量创建、链接引入、修改、删除、恢复
- 管理素材分组树（创建/修改/删除/移动/排序），限制最大层级 5 级
- 素材文件替换与版本管理（自动保留最近 10 个版本）
- 素材引用绑定/解绑（跨业务关联素材）
- 素材所有权转移与转移日志查询
- 素材可见性控制（PRIVATE/PUBLIC）
- 链接有效性检测（HEAD 请求校验）
- 素材选择器接口（面向客户侧，含分组树+素材列表）
- 素材统计（热门排行、类型分布）
- 修改 `com.wkclz.micro.material` 包下任何代码

---

## 2. 架构概览

```
┌─────────────────────────────────────────────────────────────────────┐
│                         REST Controllers                            │
│  MaterialRest  MaterialGroupRest  MaterialRefRest  MaterialVersionRest│
│  MaterialTransferRest  MaterialStatsRest  MaterialPickerRest        │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                          Service Layer                               │
│  MdmMaterialService  MdmMaterialGroupService  MdmMaterialRefService │
│  MdmMaterialVersionService  MdmMaterialTransferLogService           │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                          Mapper Layer                                │
│  MdmMaterialMapper  MdmMaterialGroupMapper  MdmMaterialRefMapper    │
│  MdmMaterialVersionMapper  MdmMaterialTransferLogMapper             │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                     Infrastructure                                   │
│  MaterialGroupCache (Redis Pub/Sub)  MaterialApi (对外API)          │
│  FileosSignApi / FileosDeleteApi (micro-fileos)                     │
│  RedisIdGenerator (编码生成)  PrincipalContext (会话)                   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3. 核心组件速查

### 实体 (Entity)

| 类                        | 表名                          | 说明   | 关键字段                                                                                                                                                                                                   |
|--------------------------|-----------------------------|------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `MdmMaterial`            | `mdm_material`              | 素材主表 | materialCode, materialType(IMAGE/VIDEO/AUDIO/DOCUMENT/OTHER), sourceType(UPLOAD/LINK), groupCode, fileId, linkUrl, linkStatus(VALID/INVALID/UNKNOWN), visibility(PRIVATE/PUBLIC), userCode, tenantCode |
| `MdmMaterialGroup`       | `mdm_material_group`        | 素材分组 | groupCode, parentCode(顶级为"0"), groupName, groupType(SYSTEM/PERSONAL), userCode, tenantCode                                                                                                             |
| `MdmMaterialRef`         | `mdm_material_ref`          | 素材引用 | materialCode, bizType, bizCode, refDesc, userCode, tenantCode                                                                                                                                          |
| `MdmMaterialVersion`     | `mdm_material_version`      | 素材版本 | materialCode, versionNo, fileId, fileName, fileSize, userCode, tenantCode                                                                                                                              |
| `MdmMaterialTransferLog` | `mdm_material_transfer_log` | 转移日志 | materialCode, fromUserCode, toUserCode, operatorCode, userCode, tenantCode                                                                                                                             |

### DTO

| 类                           | 说明                            |
|-----------------------------|-------------------------------|
| `MdmMaterialDto`            | MdmMaterial 扩展 DTO            |
| `MdmMaterialGroupDto`       | MdmMaterialGroup 扩展 DTO       |
| `MdmMaterialRefDto`         | MdmMaterialRef 扩展 DTO         |
| `MdmMaterialVersionDto`     | MdmMaterialVersion 扩展 DTO     |
| `MdmMaterialTransferLogDto` | MdmMaterialTransferLog 扩展 DTO |

### 请求对象 (Req)

| 类                        | 用途                      | 关键字段                                                                                       |
|--------------------------|-------------------------|--------------------------------------------------------------------------------------------|
| `MaterialCreateReq`      | 上传创建素材                  | fileId, fileName, fileSize, materialName, materialType, groupCode, visibility, description |
| `MaterialBatchCreateReq` | 批量创建素材                  | items(List\<MaterialCreateItem\>), materialType, groupCode, visibility                     |
| `MaterialLinkCreateReq`  | 链接引入素材                  | materialName, materialType, linkUrl, groupCode, visibility, description                    |
| `MaterialUpdateReq`      | 修改素材(extends UpdateReq) | materialName, description                                                                  |
| `MaterialReplaceFileReq` | 替换文件(extends UpdateReq) | fileId, fileName, fileSize                                                                 |
| `MaterialPageReq`        | 分页查询(extends PageReq)   | materialName, materialType, sourceType, groupCode, visibility, userCode                    |
| `MaterialGroupCreateReq` | 创建分组                    | parentCode, groupName, groupType                                                           |
| `MaterialGroupUpdateReq` | 修改分组(extends UpdateReq) | groupName                                                                                  |

### 响应对象 (Resp)

| 类                          | 说明                                |
|----------------------------|-----------------------------------|
| `MaterialResp`             | 素材详情(含 signedUrl, refs, versions) |
| `MaterialPageResp`         | 素材分页列表(含 signedUrl)               |
| `MaterialGroupTreeResp`    | 分组树节点(含 children 递归)              |
| `MaterialVersionResp`      | 版本信息                              |
| `MaterialRefResp`          | 引用信息                              |
| `MaterialDistributionResp` | 类型分布统计(materialType, count)       |
| `MaterialTransferLogResp`  | 转移日志                              |

### Service

| 类                               | 继承                                                                  | 核心方法                                                                                                                                                  |
|---------------------------------|---------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| `MdmMaterialService`            | `BaseService<MdmMaterial, MdmMaterialMapper>`                       | getPage, getInfo, create, batchCreate, linkCreate, update, remove, restore, move, replaceFile, updateVisibility, checkLink, getHotPage, getPickerPage |
| `MdmMaterialGroupService`       | `BaseService<MdmMaterialGroup, MdmMaterialGroupMapper>`             | getTree, getPickerTree, create, update, remove, move, sort                                                                                            |
| `MdmMaterialRefService`         | `BaseService<MdmMaterialRef, MdmMaterialRefMapper>`                 | bind, unbind, listByMaterialCode, check                                                                                                               |
| `MdmMaterialVersionService`     | `BaseService<MdmMaterialVersion, MdmMaterialVersionMapper>`         | listByMaterialCode                                                                                                                                    |
| `MdmMaterialTransferLogService` | `BaseService<MdmMaterialTransferLog, MdmMaterialTransferLogMapper>` | transfer, listByMaterialCode                                                                                                                          |

### Mapper

| 类                              | 继承                                   | 自定义方法                                                                                       |
|--------------------------------|--------------------------------------|---------------------------------------------------------------------------------------------|
| `MdmMaterialMapper`            | `BaseMapper<MdmMaterial>`            | getMaterialList4Page, getByGroupCodeWithChildren, getHotMaterialList, getPickerMaterialList |
| `MdmMaterialGroupMapper`       | `BaseMapper<MdmMaterialGroup>`       | getGroupTree, getGroups4Cache, getChildGroupCodes, getPickerGroupTree                       |
| `MdmMaterialRefMapper`         | `BaseMapper<MdmMaterialRef>`         | getByMaterialCode, countByMaterialCode, deleteByBiz                                         |
| `MdmMaterialVersionMapper`     | `BaseMapper<MdmMaterialVersion>`     | getByMaterialCode, deleteOldestVersions                                                     |
| `MdmMaterialTransferLogMapper` | `BaseMapper<MdmMaterialTransferLog>` | getByMaterialCode                                                                           |

### 缓存

| 类                    | 说明                                                                                                                            |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------|
| `MaterialGroupCache` | 分组缓存，实现 `MessageListener`，频道 `sh:micro:material:group:cache:refresh`，3 秒防抖，按 `tenantCode:userCode` / `tenantCode:SYSTEM` 分组缓存 |

### 对外 API

| 类                      | 方法                                                                                                               |
|------------------------|------------------------------------------------------------------------------------------------------------------|
| `MaterialApi` (接口)     | `bind(materialCode, bizType, bizCode, refDesc)`, `unbind(materialCode, bizType, bizCode)`, `check(materialCode)` |
| `MaterialApiImpl` (实现) | 委托 `MdmMaterialRefService`                                                                                       |

---

## 4. 核心工作流

### 4.1 上传创建素材

```java
// MaterialRest.create → MdmMaterialService.create
MdmMaterial material = mdmMaterialService.create(
    fileId, fileName, fileSize,
    materialName, materialType, groupCode, visibility, description
);
// 内部流程:
// 1. RedisIdGenerator 生成 materialCode (前缀 "m_")
// 2. sourceType 固定为 "UPLOAD"
// 3. 从 PrincipalContext 获取 tenantCode/userCode
// 4. insert 到数据库
```

### 4.2 链接引入素材

```java
// MaterialRest.linkCreate → MdmMaterialService.linkCreate
MdmMaterial material = mdmMaterialService.linkCreate(
    materialName, materialType, linkUrl, groupCode, visibility, description
);
// 内部流程:
// 1. checkLinkStatus() 发送 HEAD 请求检测链接有效性
// 2. sourceType 固定为 "LINK"，linkStatus 为 VALID/INVALID/UNKNOWN
// 3. 生成 materialCode 后 insert
```

### 4.3 替换文件（版本管理）

```java
// MaterialRest.replaceFile → MdmMaterialService.replaceFile
MdmMaterial material = mdmMaterialService.replaceFile(id, version, fileId, fileName, fileSize);
// 内部流程:
// 1. 校验编辑权限 (checkEditPermission)
// 2. 保存当前文件信息到 mdm_material_version (版本号自增)
// 3. 删除旧文件 (fileosDeleteApi.delete)
// 4. 更新素材的 fileId/fileName/fileSize
// 5. cleanupOldVersions 保留最近 10 个版本
```

### 4.4 分组管理

```java
// MaterialGroupRest.create → MdmMaterialGroupService.create
MdmMaterialGroup group = mdmMaterialGroupService.create(entity);
// 内部流程:
// 1. checkDepth 校验层级不超过 MAX_DEPTH(5)
// 2. RedisIdGenerator 生成 groupCode (前缀 "mg_")
// 3. insert 后 clearCache 广播 Redis Pub/Sub 刷新
```

### 4.5 引用绑定/解绑

```java
// 通过 REST 或 MaterialApi 调用
Integer result = mdmMaterialRefService.bind(materialCode, bizType, bizCode, refDesc);
Integer result = mdmMaterialRefService.unbind(materialCode, bizType, bizCode);
Map<String, Object> check = mdmMaterialRefService.check(materialCode);
// check 返回 {referenced: true/false, count: N}
```

### 4.6 素材转移

```java
// MaterialTransferRest.create → MdmMaterialTransferLogService.transfer
Integer count = mdmMaterialTransferLogService.transfer(ids, toUserCode);
// 内部流程:
// 1. 遍历素材列表，更新 userCode 为 toUserCode
// 2. 每条素材写入一条 mdm_material_transfer_log 记录
```

### 4.7 选择器查询（客户侧）

```java
// MaterialPickerRest.list → MdmMaterialService.getPickerPage
// SQL 可见性过滤: visibility = 'PUBLIC' OR user_code = #{userCode}
// MaterialPickerRest.groups → MdmMaterialGroupService.getPickerTree
// 分组过滤: group_type = 'SYSTEM' OR user_code = #{userCode}
```

---

## 5. 配置项

本模块无独立配置项，依赖以下外部配置：

| 配置来源                          | 说明                                                      |
|-------------------------------|---------------------------------------------------------|
| `micro-fileos`                | FileosSignApi / FileosDeleteApi，素材文件存储与签名               |
| `iam-contract-api`            | PrincipalContext 获取 tenantCode / userCode               |
| `sh-redis`                    | RedisIdGenerator (编码前缀 `m_` / `mg_`)、Redis Pub/Sub 缓存频道 |
| Spring Boot AutoConfiguration | `com.wkclz.micro.material.MaterialAutoConfig`           |

---

## 6. 依赖

### Maven 依赖

| groupId               | artifactId         | 用途                                            |
|-----------------------|--------------------|-----------------------------------------------|
| `com.wkclz.iam`       | `iam-contract-api` | 会话上下文 (PrincipalContext)                      |
| `com.wkclz.framework` | `sh-mybatis`       | BaseMapper / BaseService / PageQuery          |
| `com.wkclz.framework` | `sh-redis`         | RedisIdGenerator / Redis Pub/Sub              |
| `com.wkclz.microapp`  | `micro-fileos`     | 文件签名 (FileosSignApi) / 文件删除 (FileosDeleteApi) |

### 模块间依赖关系

```
micro-fileos ← micro-material (文件存储与签名)
iam-contract-api      ← micro-material (会话上下文)
```

---

## 7. 常见问题

| 问题                         | 解决                                                                                                                                       |
|----------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| 模块未被 Spring 扫描到            | 检查 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 是否包含 `com.wkclz.micro.material.MaterialAutoConfig` |
| Mapper 无法注入                | 检查 `@MapperScan({"com.wkclz.micro.material.mapper"})` 配置                                                                                 |
| 分页查询报 timeFrom/timeTo 不能为空 | MaterialRest.page 和 MaterialPickerRest.list 要求 timeFrom/timeTo 必传                                                                        |
| 分组缓存不一致                    | 确认 Redis Pub/Sub 频道 `sh:micro:material:group:cache:refresh` 正常通信，3 秒防抖                                                                   |
| 替换文件后旧版本丢失                 | 默认保留最近 10 个版本，`cleanupOldVersions` 会清理更早的版本                                                                                              |
| 分组层级超限                     | `MdmMaterialGroupService.MAX_DEPTH = 5`，创建/移动时递归校验                                                                                       |
| 删除分组失败                     | 分组下存在子分组或素材时无法删除，需先移走                                                                                                                    |
| 素材无权操作                     | 私有素材仅所有者可查看/编辑，编辑权限通过 `checkEditPermission` 校验                                                                                           |
| 链接状态检测慢                    | `checkLinkStatus` 使用 HEAD 请求，超时 5 秒，异常时返回 UNKNOWN                                                                                        |
| 版本回滚提示需通过替换文件实现            | `MaterialVersionRest.rollback` 当前返回提示信息，未实现直接回滚                                                                                          |
