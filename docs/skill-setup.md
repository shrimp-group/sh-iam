# Skill 配置指引

harness 工程标配 13 个 Trae Skill，初始化时自动部署到目标项目的 `.trae/skills/` 目录，无需手动安装。

## 标配 Skill 列表

### 核心流水线（变更驱动工作流）

执行 Agent 和审查 Agent 交替工作，通过审查文件同步状态。

| 命令               | Skill 名称      | 说明                               |
|------------------|---------------|----------------------------------|
| `/grill`         | grill         | 追问+领域对齐，澄清变更范围和用语                |
| `/plan`          | plan          | 基于 grill 结论写入变更方案                |
| `/plan-to-tasks` | plan-to-tasks | 将 plan 拆解为可独立验证的子任务              |
| `/write-code`    | write-code    | TDD 红绿重构，逐 task 推进编码             |
| `/check-work`    | check-work    | 审查 Agent，对照 plan/tasks/code 审查产物 |

### 辅助技能

| 命令          | Skill 名称 | 说明        |
|-------------|----------|-----------|
| `/zoom-out` | zoom-out | 模块全景地图    |
| `/grill-me` | grill-me | 纯追问，不绑定变更 |
| `/handoff`  | handoff  | 会话交接文档    |

### 编码与审查规范

| 命令 | Skill 名称         | 说明                                             |
|----|------------------|------------------------------------------------|
| —  | coding-skill     | 分层架构编码规范（API→Service→Domain→Repository→Client） |
| —  | request-analysis | 需求分析与任务拆解 SOP                                  |
| —  | code-review      | 代码审查 SOP                                       |
| —  | expert-reviewer  | 专家评审（计划评审+执行评审）                                |

### 工作流全景

| 命令 | Skill 名称          | 说明               |
|----|-------------------|------------------|
| —  | cnife-pi-workflow | 工作流全景，流水线顺序和技能关系 |

## 快速开始

1. 执行 Agent：`/grill` → `/plan` → `/plan-to-tasks` → `/write-code`
2. 审查 Agent：`/check-work`（在每个阶段审查）

## MCP Server 配置

推荐安装以下 MCP Server 增强 AI 能力：

### Context7 — 技术文档查询

```json
{
  "mcpServers": {
    "context7": {
      "command": "npx",
      "args": ["-y", "@upstash/context7-mcp"]
    }
  }
}
```

### Desktop Commander — 终端操作

```json
{
  "mcpServers": {
    "desktop-commander": {
      "command": "npx",
      "args": ["-y", "@wonderwhy-er/desktop-commander"]
    }
  }
}
```

### Playwright — E2E 测试

```json
{
  "mcpServers": {
    "playwright": {
      "command": "npx",
      "args": ["-y", "@playwright/mcp"]
    }
  }
}
```

### GitHub — PR/CI/CD 管理

```json
{
  "mcpServers": {
    "github": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-github"],
      "env": {
        "GITHUB_PERSONAL_ACCESS_TOKEN": "<your-token>"
      }
    }
  }
}
```

前置条件：需要 GitHub Personal Access Token，在 https://github.com/settings/tokens 创建。

### security-mcp — 安全扫描

```json
{
  "mcpServers": {
    "security-mcp": {
      "command": "npx",
      "args": ["-y", "security-mcp"]
    }
  }
}
```

### 安装 MCP Server

1. 将上述配置合并到项目的 `.trae/mcp.json` 文件中
2. 在 Trae IDE 设置中启用「项目级 MCP」
3. 重启 Trae IDE 或重新加载窗口

## 外部技能（可选）

| 命令       | 说明                  | 安装方式                           |
|----------|---------------------|--------------------------------|
| `/hunt`  | 根因诊断                | `bunx skills add -g tw93/Waza` |
| `/check` | 审查技能（check-work 依赖） | `bunx skills add -g tw93/Waza` |
