---
name: expert-reviewer
description: 专家评审技能，含计划评审和执行评审两阶段，MUST FIX / LOW / INFO 三级分类
license: MIT
compatibility: opencode
---

## 评审流程

### 1. 计划评审（Plan Review）

**对象**：`spec.md` + `tasks.md`
**检查清单**：

- [ ] 需求目标是否清晰、无歧义
- [ ] 功能描述是否完整覆盖原始需求
- [ ] 影响范围是否准确（模块、接口、数据表、配置）
- [ ] 验收标准是否可量化验证
- [ ] 任务拆分是否合理（粒度适中、无遗漏）
- [ ] 各 Task 的依赖关系是否正确
- [ ] 是否有隐含的技术债务或风险
  **循环上限**：最多 3 轮，超出后升级到人工决策。
  **产出**：`request_analysis/review/review_v1.md`

### 2. 执行评审（Execution Review）

**对象**：代码变更（相对于基线分支的 diff）
**检查清单**：

- [ ] 是否满足 spec.md 中所有功能要求
- [ ] 是否遵循分层架构规范
- [ ] 异常边界是否覆盖（None、越界、非法参数）
- [ ] 是否处理了失败/降级场景
- [ ] 金额字段类型和精度是否正确（Java: BigDecimal，Node: decimal.js 或以分为单位的整数）
- [ ] 外部调用是否设置了超时和降级
- [ ] 日志是否完整（关键链路、异常堆栈）
- [ ] 国际化/多环境是否需要同步修改
- [ ] 是否存在安全风险（SQL 注入、越权等）
  **循环上限**：最多 2 轮，超出后升级到人工决策。
  **产出**：`coding/review/code_review_v1.md`

---

## 评审意见分级

```
[MUST FIX] 问题描述
原因：为什么必须修复
建议：如何修复
位置：文件:行号

[LOW] 问题描述
建议：可选优化方式

[INFO] 问题描述
仅供知悉，无需修改
```

- **MUST FIX** — 不修复会导致线上故障、数据错误或严重 Bug
- **LOW** — 建议优化，不影响功能正确性
- **INFO** — 仅供知悉

---

## 评审报告模板

```markdown
# {类型}评审报告 v{版本号}

## 概述
- 评审对象：{spec.md / 代码变更}
- 评审日期：{YYYY-MM-DD}
- 评审结论：{APPROVED / REVISION REQUIRED}
- MUST FIX：{数量} | LOW：{数量} | INFO：{数量}
- 评审轮次：{N}

## 评审意见

### MUST FIX
...

### LOW
...

### INFO
...
```
