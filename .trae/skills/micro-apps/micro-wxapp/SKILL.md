---
name: "micro-wxapp"
description: "微信小程序模块。小程序登录/用户管理/手机号绑定/配置CRUD/媒体上传下载。修改 com.wkclz.micro.wxapp 包下代码时触发"
---

# Micro-Wxapp 模块

## 1. 适用场景

当用户需要完成以下任务时触发此 Skill：

- 实现微信小程序登录（code2Session、用户信息解密）
- 管理小程序用户信息（查询/更新/手机号绑定）
- 管理小程序配置（appId/appSecret/token/aesKey 的 CRUD）
- 上传/下载微信临时素材
- 微信消息验签与消息路由
- 修改 `com.wkclz.micro.wxapp` 包下任何代码

---

## 2. 架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                         REST 层                                  │
│  WxMaUserRest        WxMaMediaRest       WxappConfigRest        │
│  (登录/用户/手机号)    (素材上传下载)       (配置 CRUD)            │
│  WxAppRest (微信验签)                                            │
└──────────────┬──────────────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────────────┐
│                       Service 层                                 │
│  ┌─── 基础 Service (单表 CRUD) ───────────────────────────┐     │
│  │  WxappConfigService   WxappUserService                 │     │
│  │  WxappLoginLogService                                   │     │
│  └─────────────────────────────────────────────────────────┘     │
│  ┌─── 自定义 Service (业务逻辑) ──────────────────────────┐     │
│  │  WxMiniappService    (登录/用户信息/更新)               │     │
│  │  WxappLoginService   (JWT 生成 + Redis 会话)           │     │
│  └─────────────────────────────────────────────────────────┘     │
└──────────────┬──────────────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────────────┐
│                       Mapper 层                                  │
│  WxappConfigMapper    WxappUserMapper    WxappLoginLogMapper    │
│  (extends BaseMapper) (extends BaseMapper) (extends BaseMapper) │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    配置 & 辅助层                                  │
│  WxMaConfiguration  (多租户 WxMaService 管理 + 消息路由)         │
│  Checker            (appId 格式校验)                             │
│  MicroAppAutoConfig (自动配置 @ComponentScan + @MapperScan)      │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. 核心组件速查

### 实体类 (bean/entity)

| 类名 | 表名 | 说明 | 关键字段 |
|------|------|------|----------|
| `WxappConfig` | `wxapp_config` | 小程序配置 | `tenantCode`, `appId`, `appSecret`, `certPem`, `keyPem`, `appToken`, `aesKey` |
| `WxappUser` | `wxapp_user` | 小程序用户 | `userCode`, `nickname`, `appId`, `openId`, `unionId`, `mobile`, `email`, `gender`, `avatar`, `loginTimes` |
| `WxappLoginLog` | `wxapp_login_log` | 登录日志 | `userCode`, `openId`, `loginIp` |

### VO 类 (bean/vo)

| 类名 | 说明 | 关键字段 |
|------|------|----------|
| `WxMaAppInfo` | 小程序配置查询参数/结果 | `tenantCode`, `appId`, `appSecret`, `certPem`, `keyPem`, `appToken`, `aesKey` |
| `WxMaAppUserLoginVo` | 小程序登录请求体 | `appId`, `code`, `encryptedData`, `iv`, `rawData`, `signature` |

### DTO 类 (bean/dto)

| 类名 | 说明 |
|------|------|
| `WxappConfigDto` | `WxappConfig` 扩展，代码重新生成不覆盖 |
| `WxappUserDto` | `WxappUser` 扩展，代码重新生成不覆盖 |
| `WxappLoginLogDto` | `WxappLoginLog` 扩展，代码重新生成不覆盖 |

### Mapper 接口

| 接口 | 自定义方法 |
|------|-----------|
| `WxappConfigMapper` | `getWxMaAppInfo(WxMaAppInfo)`, `getConfigList(WxappConfig)` |
| `WxappUserMapper` | `getWxappUserByOpenId(String)`, `getWxappUserByUserCode(String)` |
| `WxappLoginLogMapper` | `example()` (示例，可删除) |

### Service 类

| 类 | 继承 | 说明 |
|----|------|------|
| `WxappConfigService` | `BaseService<WxappConfig, WxappConfigMapper>` | 配置 CRUD + 敏感字段脱敏 + appId 唯一校验 |
| `WxappUserService` | `BaseService<WxappUser, WxappUserMapper>` | 用户 CRUD |
| `WxappLoginLogService` | `BaseService<WxappLoginLog, WxappLoginLogMapper>` | 登录日志 CRUD |
| `WxMiniappService` | — | 核心业务：登录、用户信息查询/更新 |
| `WxappLoginService` | — | JWT 生成 + Redis 会话缓存 |

### REST 控制器

| 类 | 路由 | 说明 |
|----|------|------|
| `WxMaUserRest` | `/micro-wxapp` | 登录/用户信息/更新/手机号绑定/获取手机号 |
| `WxMaMediaRest` | `/micro-wxapp` | 临时素材上传/下载 |
| `WxappConfigRest` | `/micro-wxapp` | 配置分页/详情/创建/更新/删除 |
| `WxAppRest` | `/micro-wxapp` | 微信验签（GET 验证） |

### 配置 & 辅助类

| 类 | 说明 |
|----|------|
| `WxMaConfiguration` | 多租户 WxMaService 管理：按 appid/tenantCode 懒加载初始化，消息路由（log/text/pic/qrcode/template） |
| `Checker` | appId 格式校验：正则 `^wx[a-fA-F0-9]{16}$` |
| `MicroAppAutoConfig` | 自动配置类：`@ComponentScan("com.wkclz.micro.wxapp")` + `@MapperScan("com.wkclz.micro.wxapp.mapper")` |

---

## 4. 核心工作流

### 4.1 小程序登录

```
前端 → POST /micro-wxapp/public/miniapp/login
     → WxMaUserRest.customerMiniappLogin()
     → WxMiniappService.miniappLogin()
         1. 校验 appId 格式 (Checker.isValidWxAppId)
         2. 校验用户信息参数完整性 (encryptedData/iv/rawData/signature 要么全传要么不传)
         3. 获取 WxMaService → jscode2session 获取 openid/unionid/sessionKey
         4. 若传了用户信息 → 校验签名 + 解密用户信息
         5. 查询/创建 WxappUser (首次登录自动注册)
         6. 记录 WxappLoginLog
         7. 调用 WxappLoginService.login() 生成 JWT + Redis 会话
     → 返回 LoginResponse (token)
```

关键代码：

```java
// WxMiniappService.miniappLogin()
WxMaService wxService = configuration.getMaService(vo.getAppId());
WxMaJscode2SessionResult session = wxService.getUserService().getSessionInfo(vo.getCode());
String openid = session.getOpenid();

WxappUser user = wxappUserMapper.getWxappUserByOpenId(openid);
if (user == null) {
    user = new WxappUser();
    user.setAppId(vo.getAppId());
    user.setOpenId(openid);
    user.setUserCode(redisIdGenerator.generateIdWithPrefix("wxapp_"));
    wxappUserMapper.insert(user);
}
return wxappLoginService.login(user);
```

### 4.2 登录会话创建

```java
// WxappLoginService.login()
SessionCreateReq req = new SessionCreateReq();
req.setUserCode(user.getUserCode());
req.setUsername(user.getUserCode());
req.setNickname(user.getNickname());
req.setAvatar(user.getAvatar());
req.setAuthType("WXAPP");
req.setAuthIdentifier(user.getOpenId());

LoginResp loginResp = ssoFacadeContract.login(req);
String jwtToken = loginResp.getToken();
```

### 4.3 手机号绑定

```
前端 → POST /micro-wxapp/miniapp/mobile/bind {code, appId}
     → WxMaUserRest.miniappMobileBind()
         1. 获取 WxMaService → getPhoneNumber(code)
         2. 校验手机号格式 (RegularTool.isMobile)
         3. 更新 WxappUser.mobile
```

### 4.4 配置管理

```
POST /micro-wxapp/config/create  → WxappConfigRest.wxappConfigCreate()
    → paramCheck: appId 格式校验 + appSecret 非空
    → WxappConfigService.create(): appId 唯一校验 + insert

POST /micro-wxapp/config/update  → WxappConfigRest.wxappConfigUpdate()
    → WxappConfigService.update(): 敏感字段为 "******" 时保留原值，copyIfNotNull 合并更新

GET  /micro-wxapp/config/info     → 敏感字段脱敏返回 (appSecret/appToken/aesKey/certPem/keyPem → "******")
```

### 4.5 微信验签

```
GET /micro-wxapp/public/miniapp/portal?signature=&timestamp=&nonce=&echostr=
→ WxAppRest.authGet()
→ wxService.checkSignature(timestamp, nonce, signature)
→ 验证通过返回 echostr
```

### 4.6 临时素材上传/下载

```
POST /micro-wxapp/customer/wx/media/upload
→ WxMaMediaRest.uploadMedia()
→ 解析 MultipartFile → wxService.getMediaService().uploadMedia()

GET /micro-wxapp/customer/wx/media/download/{mediaId}
→ WxMaMediaRest.getMedia()
→ wxService.getMediaService().getMedia(mediaId)
```

---

## 5. 配置项

### 数据库表

| 表名 | 说明 |
|------|------|
| `wxapp_config` | 小程序配置（appId/appSecret/token/aesKey/cert） |
| `wxapp_user` | 小程序用户（openId/unionId/nickname/avatar/mobile） |
| `wxapp_login_log` | 登录日志（userCode/openId/loginIp） |

### WxMaConfiguration 多租户管理

- `WxMaService` 按 `tenantCode` 缓存在 `MA_TENANT_SERVICES` Map 中
- `appid → tenantCode` 映射缓存在 `MA_APPID_TENANT` Map 中
- 首次访问时通过 `WxappConfigService.getWxMaAppInfo()` 从数据库加载配置并初始化
- 消息格式默认 `JSON`（`config.setMsgDataFormat("JSON")`）

### 消息路由规则

| 关键字 | Handler | 说明 |
|--------|---------|------|
| (全部) | `logHandler` | 打印消息 + 回复收到的内容 |
| "模板" | `templateMsgHandler` | 模板消息（TODO: 新版本不再支持） |
| "文本" | `textHandler` | 回复文本消息 |
| "图片" | `picHandler` | 上传图片素材并回复 |
| "二维码" | `qrcodeHandler` | 生成二维码并回复 |

---

## 6. 依赖

### Maven 依赖

| 依赖 | 说明 |
|------|------|
| `micro-fileos` | 文件签名（头像 URL 签名：`fileosSignApi.sign()`） |
| `weixin-java-miniapp` | WxJava 微信小程序 SDK（`WxMaService`、`WxMaMessageRouter` 等） |
| `iam-contract-api` | IAM 契约层（`PrincipalContext`、`SsoFacadeContract`、`LoginResp`） |
| `sh-mybatis` | ORM（`BaseMapper`、`BaseService`、`PageQuery`） |
| `sh-redis` | Redis（`RedisIdGenerator` 生成用户编码前缀 `wxapp_`） |
| `sh-web` | Web 工具（`IpHelper` 获取登录 IP） |

### 模块间依赖

```
micro-fileos ← micro-wxapp (头像 URL 签名)
iam-contract-api      ← micro-wxapp (JWT 生成、Session 管理、用户上下文)
```

---

## 7. 常见问题

| 问题 | 原因/解决 |
|------|----------|
| 小程序登录报 "appId 格式错误" | appId 须匹配正则 `^wx[a-fA-F0-9]{16}$`，由 `Checker.isValidWxAppId()` 校验 |
| 配置详情返回 "******" | `getConfigInfo()` 对敏感字段（appSecret/appToken/aesKey/certPem/keyPem）脱敏，前端需原值时需单独获取 |
| 更新配置后敏感字段被清空 | 前端传 "******" 时 `update()` 会保留原值；传空则置 null。确保前端回传脱敏占位符 |
| WxMaService 未初始化 | `WxMaConfiguration` 懒加载，首次访问自动从 `wxapp_config` 表加载；确认表中已配置对应 tenantCode/appId |
| "没有找到小程序配置" | `wxapp_config` 表中无对应记录，检查 tenantCode 和 appId 是否匹配 |
| 手机号绑定失败 | 需传 `code`（微信 getPhoneNumber 返回的 code）和 `appId`；code 仅一次有效 |
| 微信验签失败 | 检查 `wxapp_config` 中 `appToken` 和 `aesKey` 是否与微信后台配置一致 |
| 用户首次登录 nickname 为 wxapp_xxx | 未传用户信息参数时，用 `RedisIdGenerator` 生成临时编码作为昵称 |
| 素材上传返回空列表 | 请求须为 `multipart/form-data` 格式，非 multipart 请求直接返回空列表 |
| `@MapperScan` 未扫描到 | 确认 `MicroAppAutoConfig` 注册在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` |
