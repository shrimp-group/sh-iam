# TD-007: IP 解析 API 使用 HTTP 而非 HTTPS

## 元信息

| 字段   | 值               |
|------|-----------------|
| ID   | TD-007          |
| 分类   | SEC / data-leak |
| 严重程度 | low             |
| 状态   | open            |
| 发现日期 | 2026-06-19      |

## 描述

IpLocalCacheHelper 使用 HTTP 调用百度开放 API 解析 IP 地址，使用 HTTP 而非 HTTPS，存在中间人攻击风险。攻击者可拦截请求并返回伪造的
IP 归属地信息。

## 影响范围

- iam-common (IpLocalCacheHelper)

## 复现条件

1. 系统解析 IP 归属地
2. 使用 HTTP 协议调用百度 API
3. 中间人可拦截并篡改响应

## 当前解决方案

使用本地缓存减少 API 调用频率。

## 建议解决方案

1. 将 API 地址从 HTTP 改为 HTTPS
2. 验证 API 响应的合法性

## 关联模块

iam-common (IpLocalCacheHelper)

## 关联 Story

STORY-004 (IP 归属地缓存工具)
