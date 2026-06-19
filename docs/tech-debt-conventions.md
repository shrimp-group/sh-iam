# 技术债务目录规范

## 目录结构

```
docs/tech-debts/
├── INDEX.md                    # 债务索引（汇总所有债务状态）
├── 001-perf-slow-query.md      # 债务记录示例
├── 002-mem-memory-leak.md      # 债务记录示例
└── ...
```

## 文件命名规范

- 格式：`{序号}-{分类简称}-{简述}.md`
- 序号：3 位数字，从 001 开始递增
- 分类简称：
    - `perf` - 性能风险
    - `mem` - 内存隐患
    - `conc` - 线程与并发风险
    - `sec` - 安全风险
    - `other` - 其他潜在风险
- 简述：英文短横线分隔，2-4 个单词描述
- 示例：`001-perf-slow-query.md`, `005-sec-sql-injection.md`

## 状态流转规则

### 状态定义

| 状态          | 含义         | 颜色标记 |
|-------------|------------|------|
| open        | 待处理，尚未开始解决 | 🔴   |
| in-progress | 正在处理中      | 🟡   |
| resolved    | 已解决        | 🟢   |

### 状态流转

```
open → in-progress → resolved
```

- open → in-progress：开始处理债务时更新
- in-progress → resolved：债务解决后更新
- 不允许回退状态（resolved 不可变为 open 或 in-progress）
- 如果已解决的债务再次出现，创建新的债务记录

### 状态更新操作

1. 修改债务文件中的"状态"字段
2. 填写"解决日期"（resolved 时）
3. 填写"当前解决方案"或"建议解决方案"
4. 更新 INDEX.md 中的对应条目
5. 更新活文档（docs/living-docs-business/changelog.md）的变更日志

## 新增债务流程

1. 确定下一个可用序号（查看 INDEX.md 中最大序号 +1）
2. 使用 tech-debt.md.template 模板创建债务文件
3. 填写所有必填字段
4. 更新 INDEX.md，添加新条目到 Open 分组
5. 更新统计概览数据

## 解决债务流程

1. 修改债务文件：状态改为 resolved，填写解决日期和解决方案
2. 更新 INDEX.md：将条目从 Open/In Progress 移到 Resolved 分组
3. 更新统计概览数据
4. 在活文档变更日志中添加记录
