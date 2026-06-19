# Changes

本目录用于记录当前正在进行的变更，由 Skill 工作流（grill → plan → plan-to-tasks → write-code → check-work）自动管理。

## 目录结构

```
changes/
├── README.md              # 本文件
├── {change-id}/           # 变更目录，以动词开头的短横线命名
│   ├── spec.md            # 变更规格说明
│   ├── tasks.md           # 任务列表
│   └── ...                # 其他变更相关文件
```

## 命名规范

- 变更目录名使用动词开头的短横线命名，如 `add-user-auth`、`fix-login-bug`、`refactor-api-layer`
- 每个变更目录包含 spec.md（变更规格）和 tasks.md（任务列表）

## 生命周期

1. **创建**：grill Skill 分析需求后创建变更目录
2. **规划**：plan Skill 生成 spec.md 和 tasks.md
3. **执行**：write-code Skill 按 tasks.md 逐项实现
4. **审查**：check-work Skill 审查实现结果
5. **完成**：变更合并后，变更目录可归档或删除
