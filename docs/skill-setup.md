# Skill 配置指引

harness 工程的 Skill 体系由两部分组成：

- **3 个保留 Skill**：harness 专属、无 superpowers 等价物的能力，初始化时由 `copySkills` 从 `templates/common/skills/`
  自动拷贝到目标项目的 `.trae/skills/` 目录。
- **20 个 Superpowers Skill**：通用研发工作流技能集，初始化时通过 superpowers-zh 在线初始化（失败则回退本地拷贝），部署到
  `.trae/skills/` 和 `.trae/rules/`。

## 保留 Skill 列表

这 3 个 Skill 是 harness 工程特有、superpowers 未覆盖的能力：

| Skill 名称     | 说明                                                           |
|--------------|--------------------------------------------------------------|
| coding-skill | 分层架构编码规范（API→Service→Domain→Repository→Client），约束项目代码分层与依赖方向 |
| zoom-out     | 模块全景地图，快速梳理当前模块的边界、依赖与职责，供 AI 建立全局认知                         |
| handoff      | 会话交接文档，结构化记录当前进度与待办，便于在 AI 会话之间或人机之间交接上下文                    |

## Superpowers Skills

superpowers-zh 技能框架包含 20 个 Skill，覆盖需求探索、计划、编码、测试、审查、调试、收尾等完整研发链路。初始化后部署到目标项目的
`.trae/skills/` 目录，规则文件 `superpowers-zh.md` 部署到 `.trae/rules/`。

| Skill                          | 触发条件                                                                                          |
|--------------------------------|-----------------------------------------------------------------------------------------------|
| brainstorming                  | 在任何创造性工作之前必须使用此技能——创建功能、构建组件、添加功能或修改行为。在实现之前先探索用户意图、需求和设计。                                    |
| chinese-code-review            | 中文 review 沟通参考——话术模板、分级标注、国内团队常见反模式应对。仅在用户显式 /chinese-code-review 时调用。                        |
| chinese-commit-conventions     | 中文 commit 与 changelog 配置参考——Conventional Commits 中文适配。仅在用户显式 /chinese-commit-conventions 时调用。 |
| chinese-documentation          | 中文文档排版参考——中英文空格、全半角标点、术语保留。仅在用户显式 /chinese-documentation 时调用。                                 |
| chinese-git-workflow           | 国内 Git 平台配置参考——Gitee、Coding.net、极狐 GitLab、CNB 接入差异。仅在用户显式 /chinese-git-workflow 时调用。          |
| dispatching-parallel-agents    | 当面对 2 个以上可以独立进行、无共享状态或顺序依赖的任务时使用                                                              |
| executing-plans                | 当你有一份书面实现计划需要在单独的会话中执行，并设有审查检查点时使用                                                            |
| finishing-a-development-branch | 当实现完成、所有测试通过、需要决定如何集成工作时使用                                                                    |
| mcp-builder                    | MCP 服务器构建方法论——系统化构建生产级 MCP 工具                                                                 |
| receiving-code-review          | 收到代码审查反馈后、实施建议之前使用，需要技术严谨性而非敷衍附和                                                              |
| requesting-code-review         | 完成任务、实现重要功能或合并前使用，用于验证工作成果是否符合要求                                                              |
| subagent-driven-development    | 当在当前会话中执行包含独立任务的实现计划时使用                                                                       |
| systematic-debugging           | 遇到任何 bug、测试失败或异常行为时使用，在提出修复方案之前执行                                                             |
| test-driven-development        | 在实现任何功能或修复 bug 时使用，在编写实现代码之前                                                                  |
| using-git-worktrees            | 当需要开始与当前工作区隔离的功能开发，或在执行实现计划之前使用                                                               |
| using-superpowers              | 在开始任何对话时使用——确立如何查找和使用技能                                                                       |
| verification-before-completion | 在宣称工作完成、已修复或测试通过之前使用，必须运行验证命令并确认输出                                                            |
| workflow-runner                | 在 Claude Code / OpenClaw / Cursor 中直接运行 agency-orchestrator YAML 工作流                          |
| writing-plans                  | 当你有规格说明或需求用于多步骤任务时使用，在动手写代码之前                                                                 |
| writing-skills                 | 当创建新技能、编辑现有技能或在部署前验证技能是否有效时使用                                                                 |

## Superpowers 初始化策略

目标工程初始化时，superpowers 部署遵循"在线优先、本地回退"策略：

1. **在线初始化（优先）**：执行 `npx superpowers-zh --tool trae`，从远端拉取最新版 superpowers 技能集，部署到目标项目的
   `.trae/skills/` 和 `.trae/rules/`。
2. **本地回退（兜底）**：若在线初始化失败（网络不可达、npx 不可用、远端异常等），从 sh-harness 自身的 `.trae/rules/` 和
   `.trae/skills/` 拷贝 superpowers-zh 规则文件与 20 个 Skill 目录到目标项目。

如需在初始化时跳过 superpowers 部署，可向 `harness-init` 传入 `--no-superpowers` 参数（参见 [CLI 使用说明](cli-usage.md)
）。此时仅部署 3 个保留 Skill，目标项目不包含 superpowers 能力。

## 快速开始

- **需求与设计**：`brainstorming`（探索意图与方案）→ `writing-plans`（写入实现计划）
- **编码实现**：`test-driven-development`（红绿重构，逐 task 推进）
- **验证收尾**：`verification-before-completion`（运行验证命令）→ `requesting-code-review`（请求审查）→
  `finishing-a-development-branch`（决定集成方式）
- **全局视角**：保留 Skill `zoom-out`（模块全景）与 `coding-skill`（分层规范）随时可用

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

| 命令      | 说明                                                | 安装方式                           |
|---------|---------------------------------------------------|--------------------------------|
| `/hunt` | 根因诊断，可与 superpowers 的 `systematic-debugging` 互补使用 | `bunx skills add -g tw93/Waza` |
