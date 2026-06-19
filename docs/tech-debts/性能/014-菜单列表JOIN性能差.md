# TD-014: 菜单列表查询使用 LEFT JOIN + GROUP BY

## 元信息

| 字段   | 值                 |
|------|-------------------|
| ID   | TD-014            |
| 分类   | PERF / slow-query |
| 严重程度 | low               |
| 状态   | open              |
| 发现日期 | 2026-06-19        |

## 描述

IamMenuMapper.getAppMenuList 同样使用 LEFT JOIN + GROUP BY 统计 children_count 和 api_bind_count，与 TD-013 同类问题。

## 影响范围

- iam-admin (IamMenuMapper)
- 菜单列表查询接口

## 复现条件

1. 菜单数量和关联数据量大
2. LEFT JOIN + GROUP BY 导致全表扫描

## 当前解决方案

功能正确但性能差。

## 建议解决方案

同 TD-013，考虑使用子查询或拆分为独立查询。

## 关联模块

iam-admin (IamMenuMapper)

## 关联 Story

STORY-028 (菜单 CRUD 与树形管理)
