# AI 初始化指引 - 将项目改造为 Harness 工程

## 概述
本文档为 AI 助手提供标准化的操作指引，用于将一个原始项目改造为符合 harness 规范的标准化工程。

## 前置条件
- 目标项目目录已存在
- AI 可以读写目标项目目录
- 已获取 sh-harness 模板项目的内容

## 初始化流程

### 步骤 1：项目分析
1. 扫描项目根目录，识别：
    - 编程语言（Java / Node.js / 其他）
    - 构建工具（Maven / Gradle / npm / yarn / pnpm）
    - 框架（Spring Boot / Express / Koa / 无框架）
    - 现有目录结构
    - 现有配置文件
2. 判断项目类型：
    - Web 应用 / API 服务 / 库 / 工具 / 其他
3. 输出分析报告，包含：
    - 语言和版本
    - 构建工具
    - 框架
    - 现有结构概览
    - 建议使用的模板（Java / Node）

### 步骤 2：选择模板
根据步骤 1 的分析结果，选择对应语言模板：
- Java 项目 → 使用 `templates/java/`
- Node.js 项目 → 使用 `templates/node/`
- 其他语言 → 参考现有模板结构，创建对应语言模板

### 步骤 3：目录结构调整
1. 创建标准目录结构（根据所选模板）：
    - Java：确保 src/main/java, src/test/java, src/main/resources, config/, scripts/, docs/ 存在
    - Node：确保 src/, test/, config/, scripts/, docs/ 存在
2. 迁移现有代码到标准目录：
    - 将散落的源码文件移入 src/ 对应位置
    - 将散落的测试文件移入 test/ 对应位置
    - 将配置文件移入 config/ 或根目录（按模板约定）
3. 保留项目原有业务逻辑，仅调整位置

### 步骤 4：配置文件生成
1. 根据模板生成配置文件：
    - Java：pom.xml 或 build.gradle, checkstyle.xml, editorconfig, gitignore
    - Node：package.json, tsconfig.json（如使用 TS）, eslint.config.js, .prettierrc, editorconfig, gitignore
2. 替换模板中的占位符：
    - 项目名称
    - groupId / package 名（Java）
    - 描述信息
3. 合并而非覆盖：
    - 如果项目已有 package.json / pom.xml，合并脚本和依赖，不覆盖已有配置
    - 如果项目已有 .gitignore，合并忽略规则

### 步骤 5：文档生成
1. 创建 docs/ 目录（如不存在）
2. 复制规范文档：
    - docs/harness-spec.md
    - docs/dev-process.md
    - docs/requirement-template.md
3. 更新或创建 README.md

### 步骤 6：生成 AGENTS.md
1. 使用 templates/common/AGENTS.md.template 模板
2. 根据项目分析结果填充占位符
3. 确保模板中的"编码规则"章节完整写入（5 条强制规则：禁止调用系统资源、保留已有注释、关键位置加日志、更新文档、Req/Resp 封装）
4. 生成到项目根目录 AGENTS.md

### 步骤 8：生成活文档
1. 使用 templates/common/docs/living-docs-technical/ 和 living-docs-business/ 模板
2. 根据项目当前状态填充架构概览、模块说明等
3. 生成到 docs/living-docs-technical/ 和 docs/living-docs-business/

### 步骤 9：拆解 Stories
1. 分析项目所有业务逻辑
2. 按照 docs/stories-guide.md 的规则拆解为 stories
3. 按业务域/模块对 stories 进行分组
4. 使用 templates/common/story.md.template 模板生成每个 story
5. 为每个 story 编写 mermaid 流程图
6. 将 stories 按分组放入 docs/stories/{分组名称}/ 目录，文件使用中文命名（如 001-用户注册.md）
7. 为每个分组创建 README.md 索引文件

### 步骤 9：复制代码规范
1. 根据项目语言，从 docs/coding-standards/ 复制对应规范文档
2. Java 项目 → 复制 java.md
3. Node 项目 → 复制 node.md
4. 放入目标项目的 docs/coding-standards/ 目录

### 步骤 10：扫描技术债务
1. 按照 docs/tech-debt-scan-guide.md 的指引，系统性扫描项目的技术债务
2. 对发现的每条债务，使用 templates/common/tech-debt.md.template 模板创建记录
3. 文件放入 docs/tech-debts/ 目录，命名遵循 docs/tech-debt-conventions.md 规范
4. 更新 docs/tech-debts/INDEX.md 索引
5. 严重程度为 critical 的债务必须在初始化报告中特别标注

### 步骤 11：部署 Skill 与初始化 Superpowers

Skill 体系由 3 个保留 Skill 和 20 个 Superpowers Skill 组成，分两步部署：

**11.1 部署保留 Skill（copySkills 自动完成）**

1. 将 `templates/common/skills/` 下的 3 个保留 skill 目录复制到目标项目的 `.trae/skills/` 目录：
    - `coding-skill` — 分层架构编码规范
    - `zoom-out` — 模块全景地图
    - `handoff` — 会话交接文档
2. 这一步由 `copySkills` 自动完成，无需手动操作。

**11.2 初始化 Superpowers（在线优先，本地回退）**

默认执行 superpowers 初始化（除非用户传入 `--no-superpowers` 参数）：

1. **在线初始化（优先）**：在目标项目根目录执行 `npx superpowers-zh --tool trae`，将 20 个 superpowers skill 部署到
   `.trae/skills/`，规则文件 `superpowers-zh.md` 部署到 `.trae/rules/`。
2. **本地回退（兜底）**：若在线初始化失败（网络不可达、npx 不可用、远端异常等），从 sh-harness 自身的
   `.trae/rules/superpowers-zh.md` 与 `.trae/skills/` 下的 20 个 superpowers skill 目录拷贝到目标项目对应位置。
3. 若用户传入 `--no-superpowers` 参数，跳过本步骤，目标项目仅包含 3 个保留 Skill。

**11.3 配置与说明**

1. 生成 `.trae/mcp.json` 配置文件（从模板或默认配置）
2. 向用户说明 skill 的使用方式：
    - 需求与设计：`brainstorming` → `writing-plans`
    - 编码实现：`test-driven-development`
    - 验证收尾：`verification-before-completion` → `requesting-code-review` → `finishing-a-development-branch`
    - 保留 Skill：`coding-skill`（分层规范）、`zoom-out`（模块全景）、`handoff`（会话交接）
3. 引导用户安装推荐的 MCP Server（参见 docs/skill-setup.md）

### 步骤 12：复制开发规范
1. 将 docs/standards/ 目录下的所有规范文档复制到目标项目
2. 放入目标项目的 docs/standards/ 目录
3. 这些规范包括：前端、后端、数据库、Git、运维、API、安全、文档、架构、日志、AI编程范式、AI编程实践、Harness工程

### 步骤 13：验证
1. 构建验证：
    - Java：`mvn clean verify` 或 `./gradlew build`
    - Node：`npm run build`
2. 测试验证：
    - Java：`mvn test`
    - Node：`npm test`
3. 规范检查验证：
    - Java：`mvn checkstyle:check`
    - Node：`npm run lint && npm run typecheck`
4. 如果验证失败，修复问题后重新验证
5. 所有验证通过后，输出初始化完成报告

## 注意事项
- 不要删除项目已有的业务代码
- 不要覆盖已有的配置，而是合并
- 保留项目的 Git 历史
- 如果项目结构特殊，灵活调整而非强制套用模板
- 每个步骤完成后向用户确认

## 初始化完成报告模板
```
## Harness 初始化完成报告

### 项目信息
- 项目名称：xxx
- 语言：xxx
- 构建工具：xxx

### 已完成的改造
- [x] 目录结构调整
- [x] 配置文件生成
- [x] 文档生成
- [x] AGENTS.md 生成
- [x] 活文档生成
- [x] Stories 拆解
- [x] 代码规范复制
- [x] 技术债务扫描
- [x] 部署保留 Skill（3 个）
- [x] 初始化 Superpowers（20 个 Skill）
- [x] 开发规范复制

### 验证结果
- [x] 构建通过
- [x] 测试通过
- [x] 规范检查通过

### 需要手动处理的事项
- [ ] 事项1
- [ ] 事项2
```
