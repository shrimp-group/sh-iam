# micro-dbview Skill

## 描述

micro-dbview 是数据库管理工具微应用模块。当需要实现数据源管理、元数据查询、SQL 执行、DDL 操作、数据库权限控制，或修改 com.wkclz.micro.dbview 包下任何代码时调用。

## 核心知识

### 模块结构

- **bean/entity/**: DbviewDatasource, DbviewDatasourcePermission, DbviewSqlHistory
- **bean/dto/**: TableInfo, ColumnInfo, IndexInfo, TableDetail, SqlExecuteRequest, SqlResult, DdlRequest, DdlPreview, ColumnDefinition, IndexDefinition
- **bean/enums/**: SqlType, PermissionLevel, DdlType
- **config/**: DbviewConfig, DbviewDataSourceFactory
- **service/**: DbviewConnectionService, DbviewDatasourceService, DbviewDatasourcePermissionService, DbviewMetadataService, DbviewSqlService, DbviewDdlService, DbviewSqlHistoryService
- **cache/**: DatasourceCache
- **rest/**: DatasourceRest, PermissionRest, MetadataRest, SqlRest, DdlRest

### 关键设计

1. **sh-dynamicdb 集成**: DbviewDataSourceFactory 实现 DynamicDataSourceFactory，通过 DynamicDataSourceHolder 在独立线程中切换数据源
2. **三级权限模型**: READ_ONLY → READ_WRITE → DDL，按用户+数据源粒度控制
3. **元数据查询**: 使用 JDBC 直接查询 INFORMATION_SCHEMA，不走 MyBatis
4. **SQL 执行**: 权限校验 → 类型解析 → 危险检测 → 执行 → 历史记录
5. **DDL 操作**: 表单驱动，生成 DDL 语句后执行，支持预览
6. **密码加密**: AES 对称加密，密钥通过 sh.dbview.aes-key 配置

### 配置项

- sh.dbview.max-rows (默认 1000)
- sh.dbview.max-rows-limit (默认 10000)
- sh.dbview.sql-timeout-seconds (默认 30)
- sh.dbview.metadata-cache-ttl (默认 300)
- sh.dbview.aes-key
- sh.dbview.history-retain-days (默认 30)

### 数据库表

- dbview_datasource: 数据源配置
- dbview_datasource_permission: 权限映射
- dbview_sql_history: 执行历史
