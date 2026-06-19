# 研发过程规范

本文档详细定义了 Harness 项目的研发过程，涵盖项目初始化、需求拆解、编码规范与规范检查。

---

## 项目结构初始化

### 初始化方式

1. **使用 harness-init 脚本初始化**：运行 `npm run init` 或 `harness-init` 命令，按提示选择语言模板
2. **使用 AI 指引初始化**：向 AI 描述项目需求，由 AI 生成符合规范的项目骨架

### 初始化步骤

1. 选择对应语言模板（Java / Node）
2. 根据项目特点调整目录结构
3. 配置 lint、test、CI/CD 等工具链
4. 初始化 Git 仓库并创建初始提交
5. 创建 `docs/` 目录并放入规范文档

### 初始化检查清单

- [ ] 目录结构符合 [harness-spec.md](harness-spec.md) 定义
- [ ] lint 配置已就绪
- [ ] test 配置已就绪
- [ ] CI/CD 流水线已配置
- [ ] .editorconfig 已配置
- [ ] .gitignore 已配置
- [ ] README.md 已创建

---

## 业务需求拆解

### 拆解原则

- 每个需求必须使用 [需求拆解模板](requirement-template.md)
- 拆解粒度：每个任务应可在 **1-2 天**内完成
- 任务必须包含：**描述**、**验收标准**、**影响范围**

### 拆解流程

1. 阅读需求，理解背景与目标
2. 识别涉及的功能模块与接口
3. 将需求拆解为独立可执行的任务
4. 为每个任务设定优先级（P0 / P1 / P2）
5. 评估每个任务的工时与风险
6. 产出技术方案与风险应对

### 优先级定义

| 优先级 | 含义         | 时间要求  |
|-----|------------|-------|
| P0  | 必须完成，阻塞性任务 | 当天完成  |
| P1  | 重要，非阻塞     | 本迭代完成 |
| P2  | 可选优化       | 视情况排期 |

---

## 编码规范

### 通用规范

#### 命名规范

| 类型    | 规范                       | 示例                 |
|-------|--------------------------|--------------------|
| 类名    | 大驼峰（PascalCase）          | `UserService`      |
| 方法名   | 小驼峰（camelCase）           | `getUserById`      |
| 常量    | 全大写下划线（UPPER_SNAKE_CASE） | `MAX_RETRY_COUNT`  |
| 变量    | 小驼峰（camelCase）           | `userName`         |
| 包名/目录 | 全小写，点号分隔                 | `com.harness.user` |
| 文件名   | 与主类/模块名一致                | `UserService.java` |

#### 注释规范

- 公共类和公共方法必须有 JSDoc / Javadoc 注释
- 注释应说明"为什么"而非"做什么"
- 复杂逻辑必须添加行内注释
- 禁止保留被注释掉的代码，使用版本控制管理历史

#### 错误处理规范

- 禁止吞掉异常（空 catch 块）
- 错误信息必须包含上下文，便于排查
- 区分业务异常与系统异常
- 对外接口统一错误响应格式

### Java 规范

#### 包命名

- 格式：`com.harness.{模块}.{子模块}`
- 示例：`com.harness.user.service`

#### 类命名

- 实体类：`{名词}`，如 `User`
- 服务类：`{名词}Service`，如 `UserService`
- 控制器：`{名词}Controller`，如 `UserController`
- 工具类：`{名词}Utils`，如 `DateUtils`
- 配置类：`{名词}Config`，如 `RedisConfig`

#### 方法命名

- 查询：`get{名词}` / `find{名词}` / `list{名词}`
- 新增：`create{名词}` / `add{名词}`
- 修改：`update{名词}`
- 删除：`delete{名词}` / `remove{名词}`
- 判断：`is{条件}` / `has{条件}`

#### 异常处理

- 业务异常继承 `BusinessException`
- 系统异常继承 `SystemException`
- 使用全局异常处理器统一捕获与响应
- 异常日志必须包含堆栈信息

### Node 规范

#### 模块规范

- 一个文件一个模块，文件名与导出名一致
- 使用 ES Module（`import/export`），避免 CommonJS（`require/module.exports`）
- 默认导出使用 `export default`，命名导出使用 `export`
- 循环依赖必须通过接口解耦

#### 异步编程规范

- 优先使用 `async/await`，避免回调嵌套
- 必须使用 `try/catch` 包裹异步操作
- 并发操作使用 `Promise.all`，注意错误处理
- 长时间运行任务需设置超时机制

#### TypeScript 类型规范

- 禁止使用 `any`，必须提供具体类型
- 接口使用 `interface`，类型别名使用 `type`
- 泛型参数使用有意义的命名（`TResponse` 而非 `T`）
- 严格模式开启：`strict: true`

---

## 规范检查

### 提交前检查（Pre-commit）

通过 pre-commit hook 自动执行：

- Lint 检查与自动修复
- 代码格式化（Prettier / google-java-format）
- 类型检查（如适用）

配置示例（Husky + lint-staged）：

```json
{
  "lint-staged": {
    "*.java": ["checkstyle", "google-java-format"],
    "*.ts": ["eslint --fix", "prettier --write"],
    "*.js": ["eslint --fix", "prettier --write"]
  }
}
```

### CI 检查

CI 流水线规范详见 [harness-spec.md](harness-spec.md#四质量门禁规范)，阶段顺序为：Lint → 类型检查 → Test →
Build，任一阶段失败即阻断流水线。

### 代码审查

- 合并请求必须至少一人 review 通过
- 审查要点：
    - 代码逻辑正确性
    - 是否符合编码规范
    - 测试覆盖是否充分
    - 是否存在安全风险
    - 是否影响现有功能
- 审查反馈必须在 1 个工作日内响应

### 活文档更新

每次完成任务后，必须按照 [活文档更新规则](living-docs-guide.md) 更新 docs/living-docs-technical/ 和
docs/living-docs-business/：

- 新增/删除模块 → 更新"模块说明"和"架构概览"
- 新增/修改 API → 更新"API 列表"
- 新增/修改数据模型 → 更新"数据模型"
- 新增/升级依赖 → 更新"外部依赖"
- 任何变更 → 在"变更日志"添加记录
