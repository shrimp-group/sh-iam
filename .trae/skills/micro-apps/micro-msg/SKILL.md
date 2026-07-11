---
name: "micro-msg"
description: "消息通知模块。当需要实现消息发送/接收、消息模板管理、用户消息记录与已读标记、用户消息配置时触发。适用于修改 com.wkclz.micro.msg 包下任何代码。"
---

# Micro-Msg 模块

## 1. 适用场景

- 需要向指定用户或用户列表发送站内消息通知
- 需要管理消息模板（创建/修改/删除/查询）
- 需要查询用户个人消息列表、消息详情、标记已读
- 需要管理用户消息偏好设置（事件消息/系统消息配置）
- 需要修改 `com.wkclz.micro.msg` 包下任何代码
- 其他模块需要通过 API 发送消息（调用 `MsgApi`）

---

## 2. 架构概览

```
┌─────────────────────────────────────────────────────────────────────┐
│  REST 层 (rest/)                                                    │
│                                                                      │
│  ManagerNotificationRest    ── 管理员：消息分页/发布/详情/阅读记录     │
│  ManagerMsgTemplateRest     ── 管理员：模板分页/详情/创建/修改/删除     │
│  PersonalUserRecordRest     ── 个人：消息列表/分页/详情/批量已读       │
│  PersonalUserSettingsRest   ── 个人：获取/保存消息配置                │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│  API 层 (api/)                                                       │
│                                                                      │
│  MsgApi ── 对外发送消息入口（4个 sentMsg 重载方法）                    │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│  Service 层 (service/)                                               │
│                                                                      │
│  MsgNotificationService  ── 消息通知：创建通知+批量生成用户记录        │
│  MsgTemplateService      ── 消息模板：CRUD + 重复校验                 │
│  MsgUserRecordService    ── 用户记录：查询/阅读/标记已读              │
│  MsgUserSettingsService  ── 用户设置：获取/保存（自动初始化）          │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│  Mapper 层 (mapper/)                                                 │
│                                                                      │
│  MsgNotificationMapper  ── BaseMapper + getNotificationList          │
│  MsgTemplateMapper      ── BaseMapper + example                     │
│  MsgUserRecordMapper    ── BaseMapper + 5个自定义SQL                  │
│  MsgUserSettingsMapper  ── BaseMapper + example                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3. 核心组件速查

### 实体类 (bean/entity/)

| 类名 | 数据库表 | 核心字段 | 说明 |
|------|----------|----------|------|
| `MsgNotification` | `msg_notification` | `noticeNo`, `userCode`, `title`, `content`, `extUrl` | 消息通知主体，noticeNo 由 RedisIdGenerator 生成 |
| `MsgTemplate` | `msg_template` | `templateCode`, `templateName`, `title`, `content` | 消息模板，templateCode 自动生成 |
| `MsgUserRecord` | `msg_user_record` | `userCode`, `noticeNo`, `readStatus`, `readTime`, `showTimes` | 用户消息记录，readStatus: 0=未读 |
| `MsgUserSettings` | `msg_user_settings` | `userCode`, `notifyEvent`, `notifySystem` | 用户消息偏好，JSON 格式存储 |

### DTO 类 (bean/dto/)

| 类名 | 继承 | 扩展字段 | 说明 |
|------|------|----------|------|
| `MsgNotificationDto` | `MsgNotification` | `recordCount`, `readCount`, `sentToUser`, `sentToUsers` | 通知分页查询含统计；发送时指定目标用户 |
| `MsgTemplateDto` | `MsgTemplate` | 无 | 模板扩展预留 |
| `MsgUserRecordDto` | `MsgUserRecord` | `title`, `sender`, `content`, `extUrl` | 关联查询通知详情字段 |
| `MsgUserSettingsDto` | `MsgUserSettings` | 无 | 设置扩展预留 |

### Service 类

| 类名 | 继承 | 核心方法 | 说明 |
|------|------|----------|------|
| `MsgNotificationService` | `BaseService<MsgNotification, MsgNotificationMapper>` | `getNotificationPage(dto)`, `createNotification(dto)`, `update(entity)` | 创建通知时自动生成 noticeNo + 批量插入用户记录 |
| `MsgTemplateService` | `BaseService<MsgTemplate, MsgTemplateMapper>` | `create(entity)`, `update(entity)`, `save(entity)`, `remove(entity)` | save 自动判断新增/更新；update 时 templateCode 不可修改 |
| `MsgUserRecordService` | `BaseService<MsgUserRecord, MsgUserRecordMapper>` | `getPersonalRecordList(dto)`, `getPersonalRecordPage(dto)`, `getNotice(noticeNo)`, `userMarkRecodeReaded(entity)` | getNotice 自动+1 showTimes 并设置 readTime |
| `MsgUserSettingsService` | `BaseService<MsgUserSettings, MsgUserSettingsMapper>` | `getUserSettings()`, `getUserSettings(userCode)`, `setUserSettings(settings)` | 首次获取自动初始化（notifyEvent/notifySystem 默认 `{}`） |

### Mapper 自定义方法

| Mapper | 方法 | SQL 类型 | 说明 |
|--------|------|----------|------|
| `MsgNotificationMapper` | `getNotificationList(dto)` | SELECT | LEFT JOIN msg_user_record 统计 recordCount/readCount |
| `MsgUserRecordMapper` | `getPersonalRecordList(dto)` | SELECT | INNER JOIN msg_notification，LIMIT #{size} |
| `MsgUserRecordMapper` | `getPersonalRecordList4Page(dto)` | SELECT | INNER JOIN msg_notification，分页用 |
| `MsgUserRecordMapper` | `getNoticeInfo(dto)` | SELECT | INNER JOIN，返回含 content/extUrl 的完整信息 |
| `MsgUserRecordMapper` | `updateShowTimes(id)` | UPDATE | show_times+1，read_deleted=0，首次阅读设 readTime |
| `MsgUserRecordMapper` | `markRecodeAsReaded(entity)` | UPDATE | 按 ids + userCode 批量标记已读 |

### API 对外接口

| 类名 | 方法签名 | 说明 |
|------|----------|------|
| `MsgApi` | `sentMsg(String title, String content, String toUserCode)` | 单人发送，无跳转URL |
| `MsgApi` | `sentMsg(String title, String content, List<String> toUserCodes)` | 多人发送，无跳转URL |
| `MsgApi` | `sentMsg(String title, String content, String extUrl, String toUserCode)` | 单人发送，含跳转URL |
| `MsgApi` | `sentMsg(String title, String content, String extUrl, List<String> toUserCodes)` | 多人发送，含跳转URL |

---

## 4. 核心工作流

### 4.1 发送消息通知（通过 MsgApi）

其他模块注入 `MsgApi` 即可发送消息，发送人自动从 `PrincipalContext.getUserCode()` 获取：

```java
@Autowired
private MsgApi msgApi;

// 单人发送
msgApi.sentMsg("审批通知", "您有一条待审批单据", "zhangsan");

// 多人发送 + 跳转URL
msgApi.sentMsg("系统公告", "系统将于今晚维护", "https://app.example.com/notice",
    List.of("zhangsan", "lisi", "wangwu"));
```

内部流程：`MsgApi.sentMsg()` → `MsgNotificationService.createNotification(dto)`：
1. 合并 `sentToUser` + `sentToUsers` 并去重
2. `RedisIdGenerator.generateIdWithPrefix("msg_")` 生成 noticeNo
3. 插入 `msg_notification` 记录
4. 批量插入 `msg_user_record`（readStatus=0, showTimes=0）

### 4.2 管理员发布通知（REST）

```
POST /micro-msg/manager/notification/sent
Body: { "title": "...", "content": "...", "extUrl": "...", "sentToUser": "zhangsan" }
或   { "title": "...", "content": "...", "sentToUsers": ["zhangsan","lisi"] }
```

### 4.3 用户查看消息

```
GET /micro-msg/personal/list          → 最多100条，前端展示 99+
GET /micro-msg/personal/page          → 分页查询
GET /micro-msg/personal/info?noticeNo=xxx  → 查看详情（自动+1展示次数）
POST /micro-msg/personal/readed       → 批量标记已读 { "ids": [1,2,3] }
```

### 4.4 消息模板管理

```
POST /micro-msg/manager/template/create  → 创建模板（templateCode 自动生成）
POST /micro-msg/manager/template/update  → 更新模板（templateCode 不可修改）
GET  /micro-msg/manager/template/page    → 分页查询
GET  /micro-msg/manager/template/info?id=1 → 详情
POST /micro-msg/manager/template/remove  → 删除
```

### 4.5 用户消息配置

```
GET  /micro-msg/personal/settings      → 获取配置（首次自动初始化）
POST /micro-msg/personal/settings/save → 保存配置
Body: { "notifyEvent": "{...}", "notifySystem": "{...}" }
```

---

## 5. 配置项

模块无独立配置项。自动配置通过 Spring Boot AutoConfiguration 机制注册：

- 自动配置类：`com.wkclz.micro.msg.MsgAutoConfig`
- 注册文件：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- `@ComponentScan(basePackages = {"com.wkclz.micro.msg"})`
- `@MapperScan({"com.wkclz.micro.msg.mapper"})`

运行时依赖：
- Redis（`RedisIdGenerator` 生成 noticeNo / templateCode 前缀 `msg_`）
- IAM SDK（`PrincipalContext.getUserCode()` 获取当前用户）

---

## 6. 依赖

### Maven 依赖

| 依赖 | 用途 |
|------|------|
| `com.wkclz.iam:iam-contract-api` | PrincipalContext 获取当前用户 userCode |
| `com.wkclz.framework:sh-mybatis` | BaseMapper / BaseService / PageQuery |
| `com.wkclz.framework:sh-redis` | RedisIdGenerator 生成业务编码 |

### 数据库表

| 表名 | 说明 |
|------|------|
| `msg_notification` | 消息通知主体 |
| `msg_template` | 消息模板 |
| `msg_user_record` | 用户消息记录（关联 notice_no） |
| `msg_user_settings` | 用户消息偏好设置 |

### 表关系

```
msg_notification (1) ──notice_no── (N) msg_user_record
msg_notification.user_code = 发送人
msg_user_record.user_code = 接收人
```

---

## 7. 常见问题

| 问题 | 原因/解决 |
|------|-----------|
| MsgApi 注入失败 | 检查 `MsgAutoConfig` 是否被 Spring 扫描到，确认 AutoConfiguration.imports 文件存在 |
| 发送消息时 userCode 为空 | `PrincipalContext.getUserCode()` 依赖 IAM 上下文，确保请求携带有效 Token |
| noticeNo 重复 | `RedisIdGenerator.generateIdWithPrefix("msg_")` 基于时间戳+Redis自增，极端并发下检查 Redis 连接 |
| 个人消息列表超过 100 条 | `personalMsgList` 默认 size=100，前端需展示 99+；完整数据用 `personalMsgPage` 分页查询 |
| 查看消息详情后 showTimes 未增加 | `getNotice()` 内部调用 `updateShowTimes`，检查 mapper XML 中 `updateShowTimes` SQL 是否正确 |
| 标记已读不生效 | `markRecodeAsReaded` SQL 同时校验 `userCode`，确保当前用户与记录归属一致 |
| 用户设置首次获取返回空 | `getUserSettings()` 会自动初始化（insert 默认 `{}`），检查事务注解是否生效 |
| 模板更新后 templateCode 变了 | `MsgTemplateService.update()` 显式 `setTemplateCode(null)` 阻止修改，确认代码未被覆盖 |
| 管理员通知分页查询慢 | `getNotificationList` 使用 LEFT JOIN + GROUP BY，数据量大时需优化索引 |
