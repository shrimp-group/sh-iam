# TD-023: BeanUtils.copyProperties 与 BeanUtil.cp() 混用

## 元信息

| 字段   | 值                    |
|------|----------------------|
| ID   | TD-023               |
| 分类   | OTHER / code-quality |
| 严重程度 | medium               |
| 状态   | open                 |
| 发现日期 | 2026-06-19           |

## 描述

IamUserService.customCreate() 使用 BeanUtils.copyProperties() 而非项目约定的 BeanUtil.cp()，违反编码约定。

## 影响范围

- iam-admin (IamUserService)

## 复现条件

1. 查看代码发现混用两种 Bean 拷贝方式
2. 行为可能不一致

## 当前解决方案

功能正常但不符合规范。

## 建议解决方案

统一使用项目约定的 BeanUtil.cp()。

## 关联模块

iam-admin (IamUserService)

## 关联 Story

STORY-025 (用户 CRUD 管理)
