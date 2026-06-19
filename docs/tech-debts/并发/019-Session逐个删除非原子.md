# TD-019: 密码修改后逐个删除 Session 非原子操作

## 元信息

| 字段   | 值                     |
|------|-----------------------|
| ID   | TD-019                |
| 分类   | CONC / context-switch |
| 严重程度 | low                   |
| 状态   | open                  |
| 发现日期 | 2026-06-19            |

## 描述

IamLoginService.changePassword() 中密码修改后调用 invalidateAllSessions()，逐个删除 Redis Session，非原子操作。大量 Session
时效率低。

## 影响范围

- iam-sso (IamLoginService)

## 复现条件

1. 用户修改密码
2. 系统逐个删除所有 Session
3. 大量 Session 时耗时较长

## 当前解决方案

逐个删除 Session。

## 建议解决方案

使用 Redis Pipeline 批量删除，减少网络往返次数。

## 关联模块

iam-sso (IamLoginService)

## 关联 Story

STORY-015 (用户名密码登录)
