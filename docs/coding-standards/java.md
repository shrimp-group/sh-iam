# Java 编码规范

## 1. 命名规范

| 类型   | 规则               | 示例                                     |
|------|------------------|----------------------------------------|
| 类名   | 大驼峰 PascalCase   | `UserService`, `OrderController`       |
| 方法名  | 小驼峰 camelCase    | `getUserById()`, `calculateTotal()`    |
| 常量   | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT`, `DEFAULT_PAGE_SIZE` |
| 变量   | 小驼峰 camelCase    | `userName`, `orderList`                |
| 包名   | 全小写点号分隔          | `com.example.user.service`             |
| 枚举类名 | PascalCase       | `UserStatus`                           |
| 枚举值  | UPPER_SNAKE_CASE | `UserStatus.ACTIVE`                    |
| 接口   | 不加 I 前缀          | `UserService`（而非 `IUserService`）       |
| 抽象类  | 加 Abstract 前缀    | `AbstractBaseService`                  |
| 测试类  | 被测类名 + Test      | `UserServiceTest`                      |

**核心原则**：每个命名都要有业务含义，禁止 `a`, `b`, `c`, `tmp` 等无意义命名。

```java
// 反例
int a = 0;
String tmp = user.getName();
List<String> list1 = new ArrayList<>();

// 正例
int retryCount = 0;
String userName = user.getName();
List<String> pendingOrderIds = new ArrayList<>();
```

---

## 2. 代码风格

### 缩进与行宽

- 缩进：**4 空格**，禁止 Tab
- 行宽：最大 **120 字符**，超长行在逻辑断点处换行并对齐

```java
// 超长行换行示例
UserOrderDTO orderDto = userOrderService.queryOrderDetailByOrderIdAndUserId(
        orderId, userId, queryParam);
```

### 大括号

- 左大括号不换行（K&R 风格）

```java
// 正确
if (condition) {
    doSomething();
}

// 错误
if (condition)
{
    doSomething();
}
```

### 空行

- 方法之间：1 空行
- 类成员之间：1 空行
- 逻辑块之间：1 空行

### import 规范

- **禁止通配符 import**（`import java.util.*`）
- 按包分组，组间空行，顺序如下：
    1. `java.*`
    2. `javax.*`
    3. 第三方库
    4. 项目内包

```java
import java.time.LocalDate;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.user.model.User;
import com.example.user.service.UserService;
```

### 修饰符顺序

```
public > protected > private > abstract > static > final > transient > volatile > synchronized > native > strictfp
```

```java
// 正确
public static final int MAX_SIZE = 100;

// 错误
static public final int MAX_SIZE = 100;
```

### 注解

每个注解独占一行：

```java
// 正确
@Override
@Transactional(readOnly = true)
public User getUserById(Long id) {

// 错误
@Override @Transactional(readOnly = true)
public User getUserById(Long id) {
```

---

## 3. 注释规范

### Javadoc 要求

- 公共类和公共方法**必须有 Javadoc**
- 格式：先一句话概述，空行后详细描述，然后 `@param`, `@return`, `@throws`

```java
/**
 * 根据用户ID查询用户信息.
 *
 * <p>从主库查询用户基本信息，若主库不可用则降级到从库。
 * 查询结果会缓存 5 分钟。</p>
 *
 * @param userId 用户ID，不能为 null
 * @return 用户信息，若用户不存在返回 Optional.empty()
 * @throws IllegalArgumentException 当 userId 为 null 时抛出
 */
public Optional<User> getUserById(Long userId) {
```

### 注释原则

- 注释说明**"为什么"**而非**"做什么"**——代码本身应说明做什么

```java
// 反例：重复代码逻辑
// 增加 count
count++;

// 正例：解释原因
// 重试计数器递增，用于判断是否超过最大重试次数
count++;
```

### 其他规则

- **禁止保留注释掉的代码**——用版本控制系统管理历史
- TODO 格式：`// TODO(作者): 描述`

```java
// TODO(zhangsan): 切换到新的支付渠道后移除此兼容逻辑
```

- 复杂逻辑必须添加行内注释

---

## 4. 异常处理

### 异常体系

- 业务异常继承 `BusinessException`，系统异常继承 `SystemException`

```java
// 业务异常：可预期的业务错误
public class OrderNotFoundException extends BusinessException {
    public OrderNotFoundException(Long orderId) {
        super("订单不存在: orderId=" + orderId);
    }
}

// 系统异常：不可预期的技术错误
public class DatabaseAccessException extends SystemException {
    public DatabaseAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### 核心规则

1. **禁止空 catch 块**——至少记录日志

```java
// 反例
try {
    sendNotification(user);
} catch (NotificationException e) {
}

// 正例
try {
    sendNotification(user);
} catch (NotificationException e) {
    log.warn("通知发送失败, userId={}", user.getId(), e);
}
```

2. **禁止用异常控制流程**——异常用于异常情况，不用于正常业务分支

```java
// 反例：用异常判断用户是否存在
try {
    User user = userService.getById(userId);
} catch (UserNotFoundException e) {
    // 处理用户不存在
}

// 正例：用返回值判断
Optional<User> userOpt = userService.findById(userId);
if (userOpt.isEmpty()) {
    // 处理用户不存在
}
```

3. **异常信息必须包含上下文**——便于排查问题

```java
// 反例
throw new BusinessException("订单创建失败");

// 正例
throw new BusinessException("订单创建失败, userId=" + userId + ", productId=" + productId);
```

4. **使用全局异常处理器统一响应**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(SystemException.class)
    public ResponseEntity<ApiResponse<Void>> handleSystem(SystemException e) {
        log.error("系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("SYSTEM_ERROR", "系统繁忙，请稍后重试"));
    }
}
```

5. **受检异常 vs 非受检异常的选择原则**
    - 受检异常（checked）：调用方可以合理恢复的情况
    - 非受检异常（unchecked）：编程错误或系统级故障，调用方无法有效恢复

6. **try-with-resources 管理资源**

```java
// 正确：自动关闭资源
try (Connection conn = dataSource.getConnection();
     PreparedStatement ps = conn.prepareStatement(sql)) {
    // ...
}

// 错误：手动关闭
Connection conn = null;
try {
    conn = dataSource.getConnection();
    // ...
} finally {
    if (conn != null) {
        conn.close();
    }
}
```

---

## 5. 集合使用

### 核心规则

1. **接口类型声明变量**

```java
// 正确
List<String> names = new ArrayList<>();
Map<String, Object> config = new HashMap<>();

// 错误
ArrayList<String> names = new ArrayList<>();
HashMap<String, Object> config = new HashMap<>();
```

2. **空集合用 `Collections.emptyList()` 而非返回 null**

```java
// 正确
public List<Order> getOrders(Long userId) {
    if (userId == null) {
        return Collections.emptyList();
    }
    return orderMapper.selectByUserId(userId);
}

// 错误
public List<Order> getOrders(Long userId) {
    if (userId == null) {
        return null; // 调用方需要判空，容易 NPE
    }
    return orderMapper.selectByUserId(userId);
}
```

3. **集合大小已知时指定初始容量**

```java
// 正确：避免扩容开销
List<String> result = new ArrayList<>(sourceList.size());
Map<String, User> userMap = new HashMap<>(expectedSize * 4 / 3 + 1);
```

4. **遍历时使用 for-each，需要索引时用 fori**

```java
// 无需索引
for (User user : userList) {
    process(user);
}

// 需要索引
for (int i = 0; i < items.size(); i++) {
    System.out.println("第 " + i + " 项: " + items.get(i));
}
```

5. **Stream API 优先用于集合转换/过滤**

```java
// 正确：简洁清晰
List<String> activeUserNames = users.stream()
        .filter(User::isActive)
        .map(User::getName)
        .collect(Collectors.toList());

// 避免在 Stream 中写复杂逻辑
// 如果 lambda 超过 3 行，提取为方法引用
List<String> activeUserNames = users.stream()
        .filter(this::isActiveUser)
        .map(User::getName)
        .collect(Collectors.toList());
```

6. **注意 ConcurrentModificationException**

```java
// 反例：遍历时删除
for (Order order : orders) {
    if (order.isCancelled()) {
        orders.remove(order); // ConcurrentModificationException!
    }
}

// 正确：使用 Iterator
Iterator<Order> it = orders.iterator();
while (it.hasNext()) {
    if (it.next().isCancelled()) {
        it.remove();
    }
}

// 正确：使用 Stream 过滤
List<Order> activeOrders = orders.stream()
        .filter(o -> !o.isCancelled())
        .collect(Collectors.toList());
```

---

## 6. 并发编程

### 核心规则

1. **优先使用 `java.util.concurrent` 包**

```java
// 正确
private final AtomicInteger counter = new AtomicInteger(0);
private final ConcurrentHashMap<String, User> userCache = new ConcurrentHashMap<>();
private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

// 错误
private volatile int counter = 0; // 非原子操作不安全
```

2. **共享变量必须使用 volatile 或锁保护**

```java
// 正确：volatile 保证可见性
private volatile boolean running = true;

// 正确：锁保护复合操作
private final Object lock = new Object();
private int count = 0;

public void increment() {
    synchronized (lock) {
        count++;
    }
}
```

3. **线程池使用 `ThreadPoolExecutor`，禁止 `new Thread()`**

```java
// 正确：使用线程池
private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(
        4, 8, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1000),
        new ThreadPoolExecutor.CallerRunsPolicy()
);

// 错误
new Thread(() -> doSomething()).start();
```

4. **SimpleDateFormat 线程不安全，使用 DateTimeFormatter**

```java
// 正确：DateTimeFormatter 是线程安全的
private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

String formatted = LocalDateTime.now().format(FORMATTER);

// 错误：SimpleDateFormat 线程不安全
private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
```

5. **锁粒度最小化**

```java
// 正确：只锁必要的代码段
public void updateUser(User user) {
    validateUser(user);           // 无需加锁
    synchronized (user.getId().toString().intern()) {
        userMapper.update(user);  // 只锁更新操作
    }
    publishEvent(user);           // 无需加锁
}
```

6. **避免死锁：固定加锁顺序**

```java
// 反例：不同顺序加锁可能死锁
public void transfer(Account from, Account to, BigDecimal amount) {
    synchronized (from) {
        synchronized (to) {
            from.debit(amount);
            to.credit(amount);
        }
    }
}

// 正确：按固定顺序加锁
public void transfer(Account from, Account to, BigDecimal amount) {
    Account first = from.getId() < to.getId() ? from : to;
    Account second = from.getId() < to.getId() ? to : from;
    synchronized (first) {
        synchronized (second) {
            from.debit(amount);
            to.credit(amount);
        }
    }
}
```

---

## 7. 日志规范

### 框架选择

- 使用 **SLF4J + Logback**，禁止其他日志框架直接使用

### 核心规则

1. **禁止 `System.out.println`**——使用 logger

2. **日志级别定义**

| 级别    | 使用场景         | 示例              |
|-------|--------------|-----------------|
| ERROR | 系统错误，需要立即处理  | 数据库连接失败、外部服务不可用 |
| WARN  | 业务异常，需要关注    | 参数校验失败、降级处理     |
| INFO  | 关键业务流程       | 用户登录、订单创建、支付完成  |
| DEBUG | 调试信息，生产环境不输出 | SQL 参数、请求/响应详情  |

3. **禁止在循环中打日志**（除非 DEBUG 级别且有条件判断）

```java
// 反例
for (Order order : orders) {
    log.info("处理订单: {}", order.getId()); // 大量日志
}

// 正确
log.info("开始处理订单, 总数={}", orders.size());
for (Order order : orders) {
    process(order);
}
log.info("订单处理完成, 成功={}, 失败={}", successCount, failCount);

// 正确：DEBUG 级别 + 条件判断
for (Order order : orders) {
    if (log.isDebugEnabled()) {
        log.debug("处理订单: {}", order.getId());
    }
    process(order);
}
```

4. **日志必须包含上下文信息**

```java
// 反例
log.error("创建订单失败");

// 正例
log.error("创建订单失败, userId={}, productId={}, amount={}", userId, productId, amount, e);
```

5. **异常日志必须包含堆栈**

```java
// 反例：丢失堆栈
log.error("处理失败: " + e.getMessage());

// 正确：异常对象作为最后一个参数
log.error("处理失败, orderId={}", orderId, e);
```

---

## 8. 最佳实践

### 不可变对象优先

```java
// 正确：不可变对象
public final class UserDTO {
    private final Long id;
    private final String name;

    public UserDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    // 只有 getter，没有 setter
    public Long getId() { return id; }
    public String getName() { return name; }
}
```

### Optional 替代 null 返回值

```java
// 正确
public Optional<User> findById(Long id) {
    return Optional.ofNullable(userMapper.selectById(id));
}

// 调用方
userService.findById(userId)
        .map(User::getName)
        .orElse("未知用户");
```

### 枚举替代魔法数字

```java
// 反例
if (status == 1) { ... }

// 正确
public enum OrderStatus {
    PENDING, PAID, SHIPPED, COMPLETED, CANCELLED
}

if (order.getStatus() == OrderStatus.PAID) { ... }
```

### Builder 模式处理多参数构造

```java
// 正确：Builder 模式
Order order = Order.builder()
        .userId(userId)
        .productId(productId)
        .amount(amount)
        .status(OrderStatus.PENDING)
        .build();
```

### 代码规模限制

| 指标   | 限制                   |
|------|----------------------|
| 方法长度 | 不超过 **80 行**         |
| 类长度  | 不超过 **500 行**        |
| 参数个数 | 不超过 **5 个**，超过则封装为对象 |

### 优先组合而非继承

```java
// 反例：继承实现复用
public class AdminUser extends User { ... }

// 正确：组合实现复用
public class AdminUser {
    private final User user;       // 组合
    private final AdminRole role;  // 组合
}
```

---

## 核心编码规则

以下规则为 harness 工程强制规范，所有项目必须遵循：

### 规则 1：禁止调用系统资源

仅能使用当前目录下的代码资源，不得调用系统级命令（如 `Runtime.exec()`、`ProcessBuilder`）或外部系统资源。

```java
// ❌ 禁止
Runtime.getRuntime().exec("ls");

// ✅ 正确：使用项目内代码资源
Files.list(Path.of("./data"));
```

### 规则 2：保留已有注释

不要移除已添加的注释，除非相关代码块已变动。注释是代码知识的重要载体，删除注释可能导致上下文丢失。

### 规则 3：关键位置加日志

实现业务逻辑时，在关键位置添加 log 日志打印。至少在以下位置添加日志：

- 方法入口（记录入参）
- 业务分支判断点
- 异常捕获点
- 外部调用前后

```java
@Slf4j
public class OrderService {
    public Order createOrder(CreateOrderReq req) {
        log.info("创建订单开始, userId={}, items={}", req.getUserId(), req.getItems().size());
        // ... 业务逻辑
        log.info("创建订单完成, orderId={}", order.getId());
        return order;
    }
}
```

### 规则 4：更新 AGENTS.md 和故事

任务完成后，必须更新 AGENTS.md 以及相关的故事文件，确保文档与代码同步。

### 规则 5：Req/Resp 封装

- 所有请求参数封装 Req 对象，除非参数只有一个值
- 所有返回内容封装 Resp 对象，除非返回只有一个值
- Req 对象统一放在 `bean/req` 包下，Resp 对象统一放在 `bean/resp` 包下
- Req 必须继承 sh-web 提供的公共父类，Resp 必须继承 sh-web 提供的公共父类

**Req 继承规范：**

| 基础类         | 包路径                            | 适用场景       |
|-------------|--------------------------------|------------|
| `IdReq`     | `com.wkclz.web.bean.IdReq`     | 根据 ID 查询详情 |
| `PageReq`   | `com.wkclz.web.bean.PageReq`   | 分页查询       |
| `UpdateReq` | `com.wkclz.web.bean.UpdateReq` | 更新操作（含乐观锁） |
| `RemoveReq` | `com.wkclz.web.bean.RemoveReq` | 删除操作（支持批量） |

> 新增接口根据业务需求自定义 Req，无需继承基础类。

**Resp 继承规范：**

| 基础类          | 包路径                             | 适用场景        |
|--------------|---------------------------------|-------------|
| `EntityResp` | `com.wkclz.web.bean.EntityResp` | 实体响应（含审计字段） |

```java
// ❌ 禁止：多参数裸传
@PostMapping("/orders")
public Order createOrder(@RequestParam Long userId, @RequestBody List<Item> items) { ... }

// ✅ 正确：封装 Req 对象
@PostMapping("/orders")
public OrderResp createOrder(@RequestBody CreateOrderReq req) { ... }

// ❌ 禁止：多字段裸返回
public Map<String, Object> getOrder(Long orderId) { ... }

// ✅ 正确：封装 Resp 对象
public OrderResp getOrder(Long orderId) { ... }

// 例外：只有一个参数/返回值时无需封装
@DeleteMapping("/orders/{id}")
public void deleteOrder(@PathVariable Long id) { ... }  // 单参数，无需 Req

@GetMapping("/orders/count")
public Long countOrders() { ... }  // 单返回值，无需 Resp
```

**Req 继承示例：**

```java
// 更新请求：继承 UpdateReq（自动包含 id 和 version）
@Data
public class UserUpdateReq extends UpdateReq {

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String email;

    private String phone;
}

// 分页查询请求：继承 PageReq
@Data
public class UserQueryReq extends PageReq {

    private String username;

    private Integer status;
}

// 删除请求：直接使用 RemoveReq（支持单条和批量）
@DeleteMapping("/remove")
public R<Void> removeUser(@Valid @RequestBody RemoveReq req) { ... }
```

**Resp 继承示例：**

```java
// 实体响应：继承 EntityResp（自动包含审计字段）
@Data
public class UserResp extends EntityResp {

    private String userCode;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String statusName;
}
```

### 规则 6：参数校验强制 @Valid，禁止 Assert

参数校验统一使用 `@Valid` + Bean Validation 注解方式，禁止使用 `Assert.notNull()`、`Assert.hasText()` 等 Spring Assert 方法。

```java
// ❌ 禁止：使用 Assert 校验参数
public void createUser(UserCreateReq req) {
    Assert.notNull(req.getUsername(), "用户名不能为空");
    Assert.hasText(req.getEmail(), "邮箱不能为空");
    // ...
}

// ✅ 正确：使用 @Valid + 校验注解
@Data
public class UserCreateReq {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
}

@PostMapping("/users")
public R<Void> createUser(@Valid @RequestBody UserCreateReq req) {
    // 校验由 @Valid 自动触发，无需手动 Assert
    userService.create(req);
    return R.ok();
}
```

### 规则 7：对象复制规范

对象复制分为两种场景，需遵循以下规则：

1. **数据库映射类（Entity）**：使用类内复制方法（如 `copy()` 方法），确保字段可控
2. **其他对象转换**（如 Req → Entity、Entity → Resp）：统一使用 `BeanUtil.cp`

**Entity 类内复制方法示例：**

```java
@Data
@TableName("sys_user")
public class UserEntity {
    private Long id;
    private String userCode;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private Integer status;

    /**
     * 类内复制方法，用于从更新请求复制字段到当前实体
     */
    public UserEntity copy(UserUpdateReq req) {
        if (req.getUsername() != null) {
            this.username = req.getUsername();
        }
        if (req.getEmail() != null) {
            this.email = req.getEmail();
        }
        if (req.getPhone() != null) {
            this.phone = req.getPhone();
        }
        return this;
    }
}
```

**BeanUtil.cp 使用示例：**

```java
// Req → Entity
UserEntity entity = BeanUtil.cp(req, UserEntity.class);

// Entity → Resp
UserResp resp = BeanUtil.cp(entity, UserResp.class);

// 批量转换：List<Entity> → List<Resp>
List<UserResp> respList = BeanUtil.cp(entityList, UserResp.class);
```
