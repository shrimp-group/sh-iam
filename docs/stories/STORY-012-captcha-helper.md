# STORY-012 — 图形验证码生成

| 属性 | 值 |
|------|-----|
| Story ID | STORY-012 |
| 所属 Epic | SDK 鉴权与安全模块 |
| 所属模块 | iam-sdk |
| 优先级 | P0 |
| 状态 | 待开发 |

## 用户故事

**作为** SSO 登录系统，**我希望** 提供图形验证码生成能力，**以便** 在用户登录失败多次后要求验证码，防止暴力破解。

## 验收标准

1. `CaptchaHelper.getCaptcha()` 生成完整验证码响应
2. 返回 `PictureCaptchaResponse`（含 captchaId、captchaCode、captchaImage、expireTime）
3. 验证码字符数 4 位
4. 图片尺寸 120x40 px，字体大小 20
5. 字符集排除易混淆字符（0/O/1/I/l）
6. 包含 5 条灰色干扰线
7. 包含 50 个随机颜色/大小噪点
8. 字符随机颜色、随机旋转 -10°~10°
9. 验证码有效期 5 分钟
10. Redis Key 格式：`iam:captcha:{captchaId}`
11. 图片返回 `data:image/png;base64,...` 格式

## 技术实现要点

- 使用 Java AWT（BufferedImage + Graphics2D）生成验证码图片
- 字符集：`23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz`
- captchaId 使用 UUID 生成
- 图片转 Base64 编码返回前端
- 验证码存储在 Redis，设置 TTL（过期时间 - 当前时间 + 10 秒缓冲）
- 返回给前端时 captchaCode 置空，不暴露答案

## 依赖故事

无

## 涉及文件

| 文件 | 路径 |
|------|------|
| CaptchaHelper | iam-sdk/src/main/java/com/wkclz/iam/sdk/helper/CaptchaHelper.java |
| PictureCaptchaResponse | iam-sdk/src/main/java/com/wkclz/iam/sdk/model/PictureCaptchaResponse.java |
