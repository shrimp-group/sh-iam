# Token 解析异常以 401 返回 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans
> 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** `SessionAuthFilter` 中 JWT 解析异常（过期/签名错误/格式错误）目前返回 500，改为以 401 + 具体原因返回前端。

**架构：** 在 `SessionAuthFilter.doFilterInternal()` 中，在 `sessionManager.validateAndRefresh(token)` 调用外层添加
try-catch，捕获 `IllegalArgumentException` 后调用现有 `writeUnauthorized(response, e.getMessage())` 返回 401。

**技术栈：** Java 25, Spring Boot 3.x, Jakarta Servlet

**注意：** 此实现计划不包含 git commit 步骤，用户将自行完成提交。

---

### 任务 1：SessionAuthFilter 添加异常捕获

**文件：**

- 修改：`iam-session/src/main/java/com/wkclz/iam/session/filter/SessionAuthFilter.java:67`

- [ ] **步骤 1：在 validateAndRefresh 外层添加 try-catch**

修改位置：`doFilterInternal` 方法中，`String token = tokenResolver.resolve(request);` 之后。

原代码（第 67 行附近）：

```java
// 3. 验证会话
Session session = sessionManager.validateAndRefresh(token);
if (session == null || session.getUserIdentity() == null) {
    log.warn("Invalid session for URI: {}", requestUri);
    writeUnauthorized(response, "会话无效或已过期");
    return;
}
```

改为：

```java
// 3. 验证会话（含 JWT 解析异常处理）
try {
    Session session = sessionManager.validateAndRefresh(token);
    if (session == null || session.getUserIdentity() == null) {
        log.warn("Invalid session for URI: {}", requestUri);
        writeUnauthorized(response, "会话无效或已过期");
        return;
    }
} catch (IllegalArgumentException e) {
    log.warn("Token validation failed for URI: {}, reason: {}", requestUri, e.getMessage());
    writeUnauthorized(response, e.getMessage());
    return;
}
```

- [ ] **步骤 2：添加日志注解（如已存在则跳过）**

文件顶部 `Logger` 声明已存在（第 37 行），无需新增。

验证内容：

```java
private static final Logger log = LoggerFactory.getLogger(SessionAuthFilter.class);
```

- [ ] **步骤 3：编译验证**

运行编译命令确认无语法错误：

```bash
mvn compile -pl iam-session -am -q
```

预期输出：BUILD SUCCESS（无输出即为成功）

### 任务 2：运行现有测试

**文件：**

- 测试：`iam-session/src/test/java/com/wkclz/iam/session/service/TokenServiceTest.java`

- [ ] **步骤 1：运行 TokenServiceTest 验证异常行为不变**

```bash
mvn test -pl iam-session -Dtest=TokenServiceTest -am
```

预期：

- `expiredTokenShouldFail` — PASS（抛 IllegalArgumentException）
- `tamperedTokenShouldFail` — PASS（抛 IllegalArgumentException）
- `nullTokenShouldFail` — PASS（抛 IllegalArgumentException）
- 其余测试全部 PASS
