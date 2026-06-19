# TD-006: 请求日志记录完整 Token 值

## 元信息

| 字段   | 值               |
|------|-----------------|
| ID   | TD-006          |
| 分类   | SEC / data-leak |
| 严重程度 | medium          |
| 状态   | open            |
| 发现日期 | 2026-06-19      |

## 描述

请求日志中记录了完整的 Token 值（截断至 511 字符），存在敏感信息泄露风险。JWT Token 通常包含用户身份信息，且可用于冒充用户。

## 影响范围

- iam-sdk (LoggingFilter)
- iam-sso (IamRequestService)
- 日志存储和查询系统

## 复现条件

1. 用户发起请求携带 Token
2. LoggingFilter 记录请求日志
3. Token 值被完整记录到数据库
4. 有日志查询权限的用户可获取他人 Token

## 当前解决方案

Token 值截断至 511 字符，但仍为完整可用的 Token。

## 建议解决方案

1. 对 Token 值进行脱敏处理，仅保留前后几位（如前 8 位 + ... + 后 4 位）
2. 在 LoggingFilter 中增加 Token 脱敏逻辑
3. 对密码字段已有脱敏处理，Token 应同等对待

## 关联模块

iam-sdk (LoggingFilter), iam-sso (IamRequestService)

## 关联 Story

STORY-008 (请求日志采集过滤器)
