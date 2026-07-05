# Harness 规范总纲

本文档定义了标准 Harness 工程的结构、配置、研发流程与质量门禁规范，所有 Harness 项目必须遵循。

---

## 一、项目结构规范

标准 harness 工程的目录结构如下：

```
project-root/
├── src/                    # 源代码
│   ├── main/              # 主代码（Java）或直接放代码（Node）
│   └── test/              # 测试代码（Java）或独立 test/ 目录（Node）
├── config/                 # 配置文件
├── scripts/                # 构建/部署脚本
├── docs/                   # 项目文档
│   ├── harness-spec.md    # 本规范文档
│   ├── dev-process.md     # 研发过程规范
│   ├── living-docs-technical/ # 技术活文档
│   ├── living-docs-business/  # 业务活文档
│   ├── requirement-template.md  # 需求拆解模板
│   ├── stories/           # Stories 目录
│   ├── coding-standards/  # 代码规范目录
│   ├── standards/         # 开发规范目录
│   └── tech-debts/        # 技术债务目录（含 INDEX.md 索引）
├── .trae/                 # Trae 配置目录
│   ├── skills/            # Trae Skill 定义文件（3 保留 + 20 superpowers）
│   ├── rules/             # superpowers-zh.md 规则文件
│   ├── .ignore            # Trae IDE 忽略规则
│   └── mcp.json           # MCP Server 配置
├── .editorconfig          # 编辑器配置
├── .gitignore             # Git 忽略规则
├── changes/               # 变更记录目录（Skill 工作流使用）
├── AGENTS.md              # AI 索引文件
├── CONTEXT.md             # 项目上下文文件（AI 助手参考）
└── README.md              # 项目说明
```

### 目录职责说明

| 目录/文件                         | 职责                               | 备注                   |
|-------------------------------|----------------------------------|----------------------|
| `src/main/`                   | Java 主代码；Node 项目直接在 `src/` 下组织   | 按语言约定调整              |
| `src/test/`                   | Java 测试代码；Node 项目使用独立 `test/` 目录 | 测试目录与主代码镜像           |
| `config/`                     | 应用配置文件（环境变量模板、特性开关等）             | 不含敏感信息               |
| `scripts/`                    | 构建、部署、数据迁移等运维脚本                  | 必须可独立执行              |
| `docs/`                       | 项目文档                             | 包含规范与模板              |
| `docs/living-docs-technical/` | 技术活文档，随代码同步更新                    | 每次任务完成后更新            |
| `docs/living-docs-business/`  | 业务活文档，随代码同步更新                    | 每次任务完成后更新            |
| `docs/stories/`               | 业务逻辑 Stories，含 mermaid 流程图       | 每个 story 独立文件        |
| `docs/coding-standards/`      | 代码规范文档（按语言）                      | 从 sh-harness 复制      |
| `docs/standards/`             | 开发规范文档（前端、后端、数据库、API 等）          | 从 sh-harness 复制      |
| `docs/tech-debts/`            | 技术债务记录与跟踪                        | 每条债务一个文件，INDEX.md 汇总 |
| `AGENTS.md`                   | AI 索引文件，供 AI 快速理解项目              | 简洁精炼                 |

### .trae/ 目录

- `.trae/skills/` — Trae Skill 定义文件目录，harness 初始化时部署 **3 个保留 Skill + 20 个 Superpowers Skill**：
    - 保留 Skill（由 `copySkills` 从 `templates/common/skills/` 拷贝）：`coding-skill`、`zoom-out`、`handoff`
    - Superpowers Skill（20 个，覆盖需求探索、计划、TDD、验证、审查、调试、收尾等研发链路）
- `.trae/rules/` — superpowers 规则文件目录，存放 `superpowers-zh.md`（superpowers 技能框架的总入口与核心规则）
- `.trae/.ignore` — Trae IDE 忽略规则，排除不应被处理的文件
- `.trae/mcp.json` — MCP Server 配置文件，定义项目级 MCP Server 连接

#### Superpowers 初始化行为

harness 初始化时默认执行 superpowers 部署（除非向 `harness-init` 传入 `--no-superpowers` 参数）：

1. **在线优先**：执行 `npx superpowers-zh --tool trae`，将 20 个 superpowers skill 部署到 `.trae/skills/`，规则文件
   `superpowers-zh.md` 部署到 `.trae/rules/`。
2. **本地回退**：在线初始化失败时，从 sh-harness 自身的 `.trae/rules/` 与 `.trae/skills/` 拷贝 superpowers 文件到目标项目。

传入 `--no-superpowers` 时跳过 superpowers 部署，目标项目仅包含 3 个保留 Skill。详见 [Skill 配置指引](skill-setup.md)
与 [CLI 使用说明](cli-usage.md)。

---

## 二、配置规范

每种语言项目必须包含以下配置项：

| 配置项           | 说明          | 必须 |
|---------------|-------------|----|
| Lint 配置       | 代码风格与静态检查规则 | 是  |
| Test 配置       | 测试框架与覆盖率规则  | 是  |
| .editorconfig | 统一编辑器格式     | 是  |
| .gitignore    | Git 忽略规则    | 是  |

### Java 项目配置

- **构建工具**：Maven 或 Gradle
- **Lint**：Checkstyle（规则文件 `checkstyle.xml`，置于 `config/` 目录）
- **测试**：JUnit 5 + Mockito
- **覆盖率**：JaCoCo，最低覆盖率 80%

### Node 项目配置

- **构建工具**：npm 或 pnpm
- **Lint**：ESLint（配置文件 `eslint.config.js`，flat config 格式）
- **格式化**：Prettier（配置文件 `.prettierrc.*`）
- **测试**：Jest（配置文件 `jest.config.*`）
- **类型系统**：TypeScript（可选，推荐启用）

---

## 三、研发流程规范

### 需求阶段

- 使用 [需求拆解模板](requirement-template.md)，将需求分解为可执行任务
- 每个任务应可在 1-2 天内完成
- 任务必须包含：描述、验收标准、影响范围

### 编码阶段

- 遵循编码规范（详见 [研发过程规范](dev-process.md)）
- 使用 lint 工具自动检查代码风格
- 提交前执行格式化
- 遵循核心编码规则（详见下方）

### 核心编码规则

所有项目必须遵循以下 5 条强制规则：

1. **禁止调用系统资源**：仅能使用当前目录下的代码资源，不得调用系统级命令或外部系统资源
2. **保留已有注释**：不要移除已添加的注释，除非相关代码块已变动
3. **关键位置加日志**：实现业务逻辑时，在关键位置添加 log 日志打印
4. **更新文档**：任务完成后，必须更新 AGENTS.md 以及相关的故事文件
5. **Req/Resp 封装**：所有请求参数封装 Req 对象（除非参数只有一个值），所有返回内容封装 Resp 对象（除非返回只有一个值）

### 测试阶段

- 单元测试覆盖率不低于 80%
- 集成测试覆盖核心业务流程
- 测试命名规范：`should_预期行为_when_条件`

### 提交阶段

- 代码必须通过所有质量门禁（见下方）
- 提交信息遵循 Conventional Commits 规范
- 合并请求必须关联需求编号

---

## 四、质量门禁规范

### 代码提交前（Pre-commit）

必须通过以下检查：

- [ ] Lint 检查通过，无 error 级别告警
- [ ] 单元测试全部通过
- [ ] 类型检查通过（如适用：TypeScript / Java 编译）

### 合并前（Pre-merge）

必须通过以下检查：

- [ ] 构建验证通过（build 成功）
- [ ] 完整测试套件通过（单元测试 + 集成测试）
- [ ] 代码审查通过（至少一人 approve）
- [ ] 无未解决的冲突

### CI 流水线

CI 流水线必须按以下顺序执行：

```
lint → 类型检查 → test → build
```

任一阶段失败，后续阶段不再执行，整体流水线标记为失败。

#### 流水线阶段说明

| 阶段    | 说明                                       | 失败处理   |
|-------|------------------------------------------|--------|
| lint  | 代码风格与静态分析（Java: Checkstyle，Node: ESLint） | 阻断后续阶段 |
| 类型检查  | Java 类型校验 / TypeScript 编译                | 阻断后续阶段 |
| test  | 运行全部测试                                   | 阻断后续阶段 |
| build | 编译/构建项目                                  | 阻断后续阶段 |
