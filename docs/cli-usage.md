# harness-init CLI 使用说明

## 安装

### 全局安装

```bash
npm install -g sh-harness
harness-init --lang java --target ./my-project
```

### 使用 npx（无需安装）

```bash
npx sh-harness --lang node
```

### 在现有项目中使用

```bash
npm init sh-harness
# 或
npm run init
```

## 命令行参数

```
Usage: harness-init [options]

Options:
  --lang <java|node>    Language template to use
  --target <path>       Target project directory (default: current directory)
  --help                Show this help message
```

| 参数         | 说明                      | 必填 | 默认值       |
|------------|-------------------------|----|-----------|
| `--lang`   | 语言模板，可选 `java` 或 `node` | 否  | 无（进入交互模式） |
| `--target` | 目标项目路径                  | 否  | 当前目录      |
| `--help`   | 显示帮助信息                  | 否  | -         |

## 交互模式

当不指定 `--lang` 参数时，CLI 将进入交互模式，逐步引导你完成项目初始化：

1. **选择语言** - 从 Java / Node.js 中选择
2. **输入项目名称** - 必填，用于替换模板中的 `project-name` 占位符
3. **选择包管理器**（仅 Node.js） - 从 npm / yarn / pnpm 中选择，默认 npm
4. **输入 Java 包名**（仅 Java） - 默认 `com.example`，用于替换模板中的包路径和包声明

## 使用示例

### 非交互模式

```bash
# 初始化 Java 项目到指定目录
harness-init --lang java --target ./my-java-project

# 初始化 Node.js 项目到当前目录
harness-init --lang node

# 查看帮助
harness-init --help
```

### 交互模式

```bash
harness-init
# 按提示依次选择语言、输入项目名称等
```

### 通过 npm scripts

```bash
# 交互模式
npm run init

# 直接指定语言
npm run init:java
npm run init:node
```

## 支持的语言模板

### Java

包含以下标准化工程结构：

- Maven 项目配置（`pom.xml`）
- JUnit 5 单元测试
- Checkstyle 代码规范检查
- JaCoCo 代码覆盖率
- GitHub Actions CI 工作流
- 统一的编辑器配置

模板占位符替换：

| 占位符            | 替换为           |
|----------------|---------------|
| `project-name` | 用户输入的项目名称     |
| `com.example`  | 用户输入的 Java 包名 |

### Node.js

包含以下标准化工程结构：

- TypeScript 支持
- Jest 单元测试
- ESLint 代码检查
- Prettier 代码格式化
- GitHub Actions CI 工作流
- 统一的编辑器配置

模板占位符替换：

| 占位符            | 替换为       |
|----------------|-----------|
| `project-name` | 用户输入的项目名称 |

## 注意事项

- 模板中的 `README.md` 不会被复制到目标目录，以避免覆盖已有项目的说明文档
- 如果目标文件已存在，该文件会被跳过（不会覆盖），CLI 会列出所有跳过的文件
- 项目名称会替换模板中所有 `project-name` 占位符，包括 `pom.xml`、`package.json`、`application.properties` 等
- Java 包名替换会影响源码目录结构（如 `com/example/` → `com/yourpkg/`）和 Java 文件中的 `package` 声明
- 本脚本仅使用 Node.js 内置模块，无需安装额外依赖

## 初始化后生成的额外文件

除了语言模板文件外，harness-init 还会自动生成以下文件和目录：

| 文件/目录            | 说明                                                      |
|------------------|---------------------------------------------------------|
| `.trae/.ignore`  | Trae AI 忽略规则，基于 `templates/common/trae-ignore.template` |
| `.trae/mcp.json` | MCP 服务器配置（空配置）                                          |
| `changes/`       | 变更记录目录，含 README.md                                      |
| `CONTEXT.md`     | 项目上下文文件，基于 `templates/common/CONTEXT.md.template`       |
| `docs/stories/`  | 用户故事目录                                                  |
| `AGENTS.md`      | AI 编码规范文件                                               |

## 初始化后自动执行

- **Git 初始化**：如果目标目录尚未初始化 Git，将自动执行 `git init` 并创建初始提交
- **验证检查**：初始化完成后自动验证关键文件是否存在，以及 AGENTS.md 和 CONTEXT.md 中是否残留未替换的占位符
