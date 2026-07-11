---
name: "micro-wxmp"
description: "微信公众号模块。管理公众号配置、OAuth2登录、消息路由/回复、菜单/素材管理、用户关注/取关、JSAPI签名。修改 com.wkclz.micro.wxmp 包下代码时触发。"
---

# Micro-Wxmp 模块

## 1. 适用场景

- 管理微信公众号配置（appId/appSecret/token/aesKey/菜单/欢迎语）
- 处理微信公众号消息回调（关注/取关/菜单点击/客服消息/扫码/地理位置）
- 微信公众号 OAuth2 网页授权登录
- 微信公众号 JSAPI 签名（H5 页面调用微信 JS-SDK）
- 微信公众号素材管理、菜单设置/删除
- 客服消息记录查询
- 微信用户信息管理（关注/取关状态、用户资料同步）
- 实现 SPI 扩展点自定义消息回复逻辑（LogSpi/MenuSpi/MsgSpi/SubscribeSpi）
- 修改 `com.wkclz.micro.wxmp` 包下任何代码

## 2. 架构概览

```
┌──────────────────────────────────────────────────────────────────┐
│                        REST 控制器层                              │
│                                                                    │
│  WxPortalRest        ─ 微信消息回调入口(GET验签/POST消息路由)       │
│  WxmpLoginRest       ─ OAuth2 网页授权登录                         │
│  WxSignRest          ─ JSAPI 签名                                  │
│  WxMaterialRest      ─ 素材管理/菜单设置                           │
│  WxmpConfigRest      ─ 公众号配置 CRUD                             │
│  WxmpKfMsgRest       ─ 客服消息查询                                │
│  WxUserRest          ─ H5 用户基本信息                              │
└───────────────────────────┬──────────────────────────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────────────┐
│                      Service 层                                   │
│                                                                    │
│  WxmpConfigService   ─ 公众号配置管理(含敏感字段脱敏/重复校验)       │
│  WxmpUserService     ─ 用户管理(关注/取关/初始化/RedisId生成)       │
│  WxmpKfMsgService    ─ 客服消息管理                                │
│  WxmpLoginLogService ─ 登录日志管理                                │
└───────────────────────────┬──────────────────────────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────────────┐
│                      Mapper 层                                   │
│                                                                    │
│  WxmpConfigMapper    ─ getWxMpAppInfo / getConfigList             │
│  WxmpUserMapper      ─ getWxmpUserByOpenId / getWxmpUserByUserCode│
│  WxmpKfMsgMapper     ─ getKfMsgList (JOIN wxmp_user 获取昵称)     │
│  WxmpLoginLogMapper  ─ 继承 BaseMapper                            │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                   消息路由 & Handler 层                            │
│                                                                    │
│  WxMpConfiguration.messageRouter() 构建消息路由链:                  │
│                                                                    │
│  LogHandler(异步) → KfSessionHandler → StoreCheckNotifyHandler     │
│  → MenuHandler → NullHandler(VIEW) → SubscribeHandler             │
│  → UnsubscribeHandler → LocationHandler → ScanHandler             │
│  → MsgHandler(默认)                                                │
│                                                                    │
│  Builder 层:                                                       │
│  AbstractBuilder ← TextBuilder / ImageBuilder                     │
│                                                                    │
│  SPI 扩展点 (可选注入, @Autowired(required=false)):                │
│  LogSpi / MenuSpi / MsgSpi / SubscribeSpi                         │
└──────────────────────────────────────────────────────────────────┘
```

## 3. 核心组件速查

### 自动配置

| 类 | 说明 |
|---|---|
| `WxmpAutoConfig` | `@ComponentScan("com.wkclz.micro.wxmp")` + `@MapperScan("com.wkclz.micro.wxmp.mapper")` |
| `WxMpConfiguration` | WxMpService 工厂(按 appid 懒加载缓存)、WxMpMessageRouter 路由链构建 |

### 实体 (Entity)

| 类 | 表名 | 核心字段 |
|---|---|---|
| `WxmpConfig` | `wxmp_config` | appId, appSecret, mpToken, aesKey, certPem, keyPem, mpMenuJson, mpTalkReplyMap, welcomeMsg |
| `WxmpUser` | `wxmp_user` | userCode, openId, unionId, appId, nickname, subscribeStatus, mobile, gender, avatar, loginTimes |
| `WxmpKfMsg` | `wxmp_kf_msg` | appId, msgType, fromUser, toUser, content, msgId, msgTime |
| `WxmpLoginLog` | `wxmp_login_log` | userCode, openId, loginIp |

### DTO / VO

| 类 | 说明 |
|---|---|
| `WxmpConfigDto` | 继承 WxmpConfig，无扩展字段 |
| `WxmpKfMsgDto` | 继承 WxmpKfMsg，扩展 fromUserNickname / toUserNickname |
| `WxmpLoginLogDto` | 继承 WxmpLoginLog，无扩展字段 |
| `WxmpUserDto` | 继承 WxmpUser，无扩展字段 |
| `WxMpAppInfo` | 公众号配置 VO: tenantCode, appId, appSecret, mpToken, aesKey |
| `WxMpAppUserLoginVo` | 登录请求 VO: appId, code, encryptedData, iv, rawData, signature |

### Handler (消息处理器)

| 类 | 触发条件 | SPI 扩展点 | 说明 |
|---|---|---|---|
| `LogHandler` | 所有消息(异步) | `LogSpi` | 日志记录，可扩展自动回复 |
| `SubscribeHandler` | 关注事件 | `SubscribeSpi` | 同步用户信息 + 调用 SPI 回复 |
| `UnsubscribeHandler` | 取消关注事件 | — | 更新 subscribeStatus=0 |
| `MenuHandler` | 菜单 CLICK 事件 | `MenuSpi` | 转发到 SPI 自定义回复 |
| `MsgHandler` | 默认(其他消息) | `MsgSpi` | 保存客服消息记录 + SPI 回复 |
| `KfSessionHandler` | 客服会话事件 | — | 预留，TODO |
| `LocationHandler` | 地理位置消息/事件 | — | 回复确认文本 |
| `ScanHandler` | 扫码事件 | — | 预留，TODO |
| `StoreCheckNotifyHandler` | 门店审核事件 | — | 预留，TODO |
| `NullHandler` | 菜单 VIEW 事件 | — | 返回 null，不回复 |

### SPI 接口

| 接口 | 方法签名 | 用途 |
|---|---|---|
| `LogSpi` | `autoReply(WxMpXmlMessage, Map, WxMpService, WxSessionManager): WxMpXmlOutMessage` | 日志处理时自定义自动回复 |
| `MenuSpi` | `autoReply(WxMpXmlMessage, Map, WxMpService, WxSessionManager): WxMpXmlOutMessage` | 菜单点击自定义回复 |
| `MsgSpi` | `autoReply(WxMpXmlMessage, Map, WxMpService, WxSessionManager): WxMpXmlOutMessage` | 普通消息自定义回复 |
| `SubscribeSpi` | `autoReply(WxMpXmlMessage, Map, WxMpService, WxSessionManager): WxMpXmlOutMessage` | 关注事件自定义回复 |

### Builder (消息构建器)

| 类 | 说明 |
|---|---|
| `AbstractBuilder` | 抽象基类，定义 `build(content, wxMessage, service)` |
| `TextBuilder` | 构建文本回复 `WxMpXmlOutMessage.TEXT()` |
| `ImageBuilder` | 构建图片回复 `WxMpXmlOutMessage.IMAGE()` |

### REST 控制器

| 类 | 路径前缀 | 核心接口 |
|---|---|---|
| `WxPortalRest` | `/micro-wxmp` | `GET /public/portal/{appid}` 验签, `POST /public/portal/{appid}` 消息路由 |
| `WxmpLoginRest` | `/micro-wxmp` | `GET /public/login/{appid}?code=xxx` OAuth2 登录 |
| `WxSignRest` | `/micro-wxmp` | `GET /h5/wx/sign?appid=&url=` JSAPI 签名 |
| `WxMaterialRest` | `/micro-wxmp` | `GET /material/batchget_material/{appid}`, `POST /menu/update/{appid}`, `POST /menu/delete/{appid}` |
| `WxmpConfigRest` | `/micro-wxmp` | `/config/page`, `/config/info`, `/config/create`, `/config/update`, `/config/remove` |
| `WxmpKfMsgRest` | `/micro-wxmp` | `/kf/msg/page`, `/kf/msg/info` |
| `WxUserRest` | `/micro-wxmp` | `GET /h5/mine/userinfo` |

### Route 常量

| 常量 | 路径 | 说明 |
|---|---|---|
| `WXMP_CONFIG_PAGE` | `/config/page` | 配置分页 |
| `WXMP_CONFIG_INFO` | `/config/info` | 配置详情 |
| `WXMP_CONFIG_CREATE` | `/config/create` | 配置创建 |
| `WXMP_CONFIG_UPDATE` | `/config/update` | 配置更新 |
| `WXMP_CONFIG_REMOVE` | `/config/remove` | 配置删除 |
| `WXMP_KF_MSG_PAGE` | `/kf/msg/page` | 客服消息分页 |
| `WXMP_KF_MSG_INFO` | `/kf/msg/info` | 客服消息详情 |
| `WXMP_MATERIAL_BATCHGET_MATERIAL` | `/material/batchget_material/{appid}` | 获取永久素材 |
| `WXMP_MENU_UPDATE` | `/menu/update/{appid}` | 设置菜单 |
| `WXMP_MENU_DELETE` | `/menu/delete/{appid}` | 删除菜单 |
| `PUBLIC_WXMP_PORTAL_APPID` | `/public/portal/{appid}` | 验签/消息回调 |
| `PUBLIC_WXMP_LOGIN_APPID` | `/public/login/{appid}` | OAuth2 登录 |
| `H5_MINE_USERINFO` | `/h5/mine/userinfo` | H5 用户信息 |
| `H5_WX_SIGN` | `/h5/wx/sign` | JSAPI 签名 |

## 4. 核心工作流

### 4.1 微信消息回调处理

微信服务器将用户消息/事件推送到 `WxPortalRest`，经 `WxMpMessageRouter` 路由到对应 Handler：

```java
// WxPortalRest - 消息入口
@PostMapping(value = Route.PUBLIC_WXMP_PORTAL_APPID, produces = "text/plain;charset=utf-8")
public String wxmpPortalAppidPost(@PathVariable String appid, @RequestBody String requestBody, ...) {
    WxMpService mpService = wxMpConfiguration.getMpService(appid);
    // 明文消息
    WxMpXmlMessage inMessage = WxMpXmlMessage.fromXml(requestBody);
    WxMpXmlOutMessage outMessage = this.route(inMessage, appid);
    return outMessage.toXml();
}

// WxMpConfiguration - 路由链构建
public WxMpMessageRouter messageRouter(WxMpService mpService) {
    newRouter.rule().handler(this.logHandler).next();           // 异步日志
    newRouter.rule().msgType(EVENT).event(SUBSCRIBE).handler(this.subscribeHandler).end(); // 关注
    newRouter.rule().async(false).handler(this.msgHandler).end(); // 默认消息
    // ...
}
```

### 4.2 关注/取关用户同步

```java
// SubscribeHandler - 关注时同步用户信息
WxMpUser userWxInfo = weixinService.getUserService().userInfo(wxMessage.getFromUser(), null);
WxmpUser user = new WxmpUser();
user.setAppId(appId);
user.setOpenId(openId);
user.setUnionId(unionId);
wxmpUserService.userSubscribe(user); // 新用户 insert，老用户更新 subscribeStatus=1

// UnsubscribeHandler - 取关时更新状态
wxmpUserService.userUnSbscribe(openId); // subscribeStatus=0
```

### 4.3 OAuth2 网页授权登录

```java
// WxmpLoginRest - 登录流程
WxOAuth2AccessToken accessToken = mpService.getOAuth2Service().getAccessToken(code);
WxOAuth2UserInfo user = mpService.getOAuth2Service().getUserInfo(accessToken, "zh_CN");
WxmpUser u = wxmpUserService.initUser(u); // 初始化/更新用户
// 调用 SsoFacadeContract 远程创建会话
SsoFacadeContract.login(user.getUserCode(), user.getNickname(), "WXMP");
// 记录登录日志
WxmpLoginLog log = new WxmpLoginLog();
log.setUserCode(user.getUserCode());
log.setOpenId(user.getOpenId());
log.setLoginIp(IpHelper.getOriginIp(req));
wxmpLoginLogMapper.insert(log);
```

### 4.4 SPI 扩展自定义回复

业务系统实现 SPI 接口即可自定义消息回复，模块通过 `@Autowired(required = false)` 可选注入：

```java
// 实现示例 - 自定义消息回复
@Component
public class MyMsgSpi implements MsgSpi {
    @Override
    public WxMpXmlOutMessage autoReply(WxMpXmlMessage wxMessage,
                                        Map<String, Object> context,
                                        WxMpService weixinService,
                                        WxSessionManager sessionManager) {
        String content = "自定义回复内容";
        return new TextBuilder().build(content, wxMessage, weixinService);
    }
}
```

### 4.5 JSAPI 签名

```java
// WxSignRest - H5 页面调用微信 JS-SDK 前获取签名
WxMpService mpService = wxMpConfiguration.getMpService(appid);
WxJsapiSignature ws = mpService.createJsapiSignature(url);
return R.ok(ws);
```

### 4.6 公众号配置敏感字段处理

```java
// WxmpConfigService - 查询时脱敏
config.setAppSecret("******");
config.setMpToken("******");
config.setAesKey("******");

// 更新时，若前端传回 "******" 则不覆盖原值
if (StringUtils.isBlank(entity.getAppSecret()) || "******".equals(entity.getAppSecret())) {
    entity.setAppSecret(null); // copyIfNotNull 不会覆盖旧值
}
```

## 5. 配置项

模块无独立 application.yml 配置项。公众号配置存储在数据库表 `wxmp_config` 中，运行时按 appid 懒加载到 `WxMpConfiguration.MP_SERVICES` 缓存。

| 数据库字段 | 说明 |
|---|---|
| `app_id` | 公众号 AppID |
| `app_secret` | 公众号 AppSecret |
| `mp_token` | 消息服务器 Token |
| `aes_key` | 消息加密 AESKey |
| `cert_pem` | 证书文件 cert |
| `key_pem` | 证书文件 key |
| `mp_menu_json` | 预设菜单 JSON（用于一键同步到微信） |
| `mp_talk_reply_map` | 对话回复映射（default 为默认回复） |
| `welcome_msg` | 关注欢迎信息 |

## 6. 依赖

### Maven 依赖

| 依赖 | 说明 |
|---|---|
| `micro-fileos` | 文件签名 API（头像 URL 签名） |
| `weixin-java-mp` | WxJava 微信公众号 SDK |
| `iam-contract-api` | IAM 认证契约（SsoFacadeContract 远程创建会话、PrincipalContext） |
| `sh-mybatis` | BaseMapper / BaseService / PageQuery |
| `sh-redis` | RedisIdGenerator（用户编码生成） |

### 框架依赖传递

| 依赖 | 用途 |
|---|---|
| `sh-core` | BaseEntity、R、ValidationException、UserContext |
| `sh-web` | IpHelper（登录 IP 获取） |

## 7. 常见问题

| 问题 | 解决 |
|---|---|
| 公众号消息回调验签失败 | 检查 `wxmp_config` 表中 `mp_token` 和 `aes_key` 是否与微信公众平台配置一致 |
| OAuth2 登录返回 null | 确认 `app_secret` 配置正确，且公众号已获得网页授权作用域 |
| SPI 自定义回复不生效 | 确认实现类已加 `@Component` 且在 `com.wkclz.micro.wxmp` 的 ComponentScan 范围内 |
| 用户关注后未同步信息 | 错误码 48001 表示公众号无获取用户信息权限，需在公众平台开通用户信息接口 |
| 配置更新后不生效 | `WxMpConfiguration` 使用内存缓存 `MP_SERVICES`，新增 appid 会懒加载，但修改已有配置需重启或清除缓存 |
| 客服消息未记录 | `MsgHandler` 仅在 `msgType != EVENT` 时保存记录，事件类消息不记录 |
| H5 用户信息头像无法访问 | `WxUserRest` 使用 `fileosSignApi.sign()` 对头像 URL 签名，需确保 micro-fileos 模块已引入且配置正确 |
| 菜单设置返回错误 | 检查 `wxmp_config.mp_menu_json` 是否为合法的微信菜单 JSON 格式 |
| 新增公众号配置 appId 重复 | `WxmpConfigService.duplicateCheck()` 按 appId 唯一校验，同一 appId 不允许创建多条 |
