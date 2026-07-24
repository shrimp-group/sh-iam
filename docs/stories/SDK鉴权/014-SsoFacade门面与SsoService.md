# STORY-014 — SsoFacade 门面与 SsoService 接口

| 属性       | 值           |
|----------|-------------|
| Story ID | STORY-014   |
| 所属 Epic  | SDK 鉴权与安全模块 |
| 所属模块     | iam-sdk     |
| 优先级      | P0          |
| 状态       | 待开发         |

## 用户故事

**作为** 第三方应用开发者，**我希望** SDK 提供 SsoFacade 和 IamSsoService 接口及默认实现，**以便** 在仅引入 SDK 时能通过远程调用与
SSO 服务端交互，同时支持 iam-sso 模块覆盖为本地实现。

## 验收标准

1. `SsoFacade` 接口定义 `saveLog(RequestRecord)` 方法
2. SDK 默认实现 `SsoFacadeImpl`：通过 HTTP POST 将日志发送到 SSO 服务端
    - 请求路径：`{serverUrl}/sign/saveLog`
    - 认证方式：请求头携带 `app-id` + `sign`（AkSignHelper 生成 AK 签名）
   - 请求体：RequestRecord 的 JSON 序列化
3. `IamSsoService` 接口定义 `tokenCheck(token, authIdentifier)` 方法
4. SDK 默认实现：远程调用 SSO 服务端校验 Token
5. `IamSdkConfig` 中通过 `@Bean @ConditionalOnMissingBean` 注册 SsoFacade 默认实现
6. iam-sso 模块的 `SsoFacadeImpl` 和 `IamSsoServiceImpl` 会自动覆盖默认实现

## 技术实现要点

- **可替换设计**：SDK 提供默认空实现/远程实现，iam-sso 提供本地直连实现
- `@ConditionalOnMissingBean` 确保只有当容器中没有其他实现时才注册默认 Bean
- SsoFacade 默认实现使用 Hutool 的 `HttpUtil` 发送 HTTP 请求
- AK 签名认证：`AkSignHelper.sign(appId, appSecret)` 生成请求签名
- 当第三方应用仅引入 iam-sdk 时，请求日志通过远程调用保存；引入 iam-sso 后，请求日志自动切换为本地直接写库

## 依赖故事

- STORY-011（AK 签名工具）
- STORY-013（SDK 配置类）

## 涉及文件

| 文件                  | 路径                                                                     |
|---------------------|------------------------------------------------------------------------|
| SsoFacade           | iam-sdk/src/main/java/com/wkclz/iam/sdk/facade/SsoFacade.java          |
| SsoFacadeImpl (SDK) | iam-sdk/src/main/java/com/wkclz/iam/sdk/facade/impl/SsoFacadeImpl.java |
| IamSsoService       | iam-sdk/src/main/java/com/wkclz/iam/sdk/service/IamSsoService.java     |
