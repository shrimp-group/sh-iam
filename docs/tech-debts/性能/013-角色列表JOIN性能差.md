# TD-013: 角色列表查询使用 LEFT JOIN + GROUP BY

## 元信息

| 字段   | 值                 |
|------|-------------------|
| ID   | TD-013            |
| 分类   | PERF / slow-query |
| 严重程度 | medium            |
| 状态   | open              |
| 发现日期 | 2026-06-19        |

## 描述

IamRoleMapper.getAppRoleList 使用 LEFT JOIN + GROUP BY 统计 children_count 和 user_bind_count，数据量大时性能差。

## 影响范围

- iam-admin (IamRoleMapper)
- 角色列表查询接口

## 复现条件

1. 角色数量和关联数据量大
2. LEFT JOIN + GROUP BY 导致全表扫描
3. 查询响应缓慢

## 当前解决方案

功能正确但性能差。

## 建议解决方案

考虑使用子查询或拆分为独立查询，避免 GROUP BY 导致的全表扫描。

## 关联模块

iam-admin (IamRoleMapper)

## 关联 Story

STORY-027 (角色 CRUD 管理)
