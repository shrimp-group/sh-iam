# 活文档更新规则

本项目活文档分为两大类：**活文档-技术** 和 **活文档-业务**，分别存放于 `docs/living-docs-technical/` 和
`docs/living-docs-business/` 目录。

## 活文档结构

```
docs/
├── living-docs-technical/     # 活文档-技术
│   ├── README.md            # 技术活文档索引
│   ├── architecture/        # 架构文档
│   │   ├── overview.md      # 架构概览
│   │   └── deployment.md    # 部署架构
│   ├── modules/             # 模块索引
│   ├── api/                 # API 索引
│   ├── data-models/          # 数据模型索引
│   └── dependencies/         # 依赖索引
│
└── living-docs-business/    # 活文档-业务
    ├── README.md            # 业务活文档索引
    ├── modules/             # 业务模块索引
    ├── processes/           # 业务流程索引
    ├── rules/               # 业务规则索引
    └── glossary/            # 业务术语表
```

---

## 活文档-技术 更新规范

### 何时更新
- 新增/删除技术模块时 → 更新 `docs/living-docs-technical/modules/README.md`
- 新增/修改 API 时 → 更新 `docs/living-docs-technical/api/README.md`
- 新增/修改数据模型时 → 更新 `docs/living-docs-technical/data-models/README.md`
- 架构变更时 → 更新 `docs/living-docs-technical/architecture/` 目录
- 依赖变更时 → 更新 `docs/living-docs-technical/dependencies/README.md`

### 更新格式
- 模块列表格式：| 模块名 | 职责 | 技术栈 | 入口路径 |
- API 列表格式：| 方法 | 路径 | 说明 | 模块 |
- 数据模型格式：| 实体名 | 说明 | 关联模块 |
- 依赖列表格式：| 依赖名 | 版本 | 用途 |

---

## 活文档-业务 更新规范

### 何时更新
- 新增/修改业务模块时 → 更新 `docs/living-docs-business/modules/README.md`
- 业务流程变更时 → 更新 `docs/living-docs-business/processes/README.md`
- 业务规则变更时 → 更新 `docs/living-docs-business/rules/README.md`
- 术语定义变更时 → 更新 `docs/living-docs-business/glossary/README.md`

### 更新格式
- 业务模块列表格式：| 模块名 | 业务职责 | 关联技术模块 |
- 业务流程列表格式：| 流程名 | 触发条件 | 业务价值 |
- 业务规则列表格式：| 规则名 | 规则描述 | 适用范围 |
- 术语列表格式：| 术语名 | 定义 | 英文名 | 分类 |

---

## 通用更新原则
- 文档与代码必须同步，禁止文档滞后
- 活文档只记录"当前状态"，不记录历史决策
- 架构图使用 mermaid 语法，保持简洁
- 每次更新后修改"最后更新"日期
- 日期格式：YYYY-MM-DD

---

## AI 更新指引

当 AI 完成一个任务后，应：

### 技术活文档更新
1. 检查本次变更影响了哪些技术模块/API/数据模型/依赖
2. 更新 `docs/living-docs-technical/` 中对应的章节
3. 更新"最后更新"日期

### 业务活文档更新
1. 检查本次变更影响了哪些业务流程/规则/术语
2. 更新 `docs/living-docs-business/` 中对应的章节
