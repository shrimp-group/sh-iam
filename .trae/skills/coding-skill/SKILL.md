---
name: coding-skill
description: 分层编码实现规范，覆盖 API 路由→Service→Domain→Repository→Client 全链路
license: MIT
compatibility: opencode
---

## 分层架构总览

```
API Router → Service → Domain → Repository → DB
    ↓
  Client → 外部服务
```

编码须严格遵守每层的职责边界，禁止跨层调用或职责混淆。

---

## 分层编码规范

### API 路由层

- 使用框架路由机制（Java: Spring RestController / Node: Express/Fastify Router）
- 请求/响应模型做校验和序列化（Java: Bean Validation / Node: Zod）
- 异常统一由全局异常处理器转换，路由层不 catch
- 不写业务逻辑

**Java 示例**

```java
// ✅ 正确：Controller 只做路由和校验
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResp> getOrder(@PathVariable Long orderId) {
        log.info("getOrder called, orderId={}", orderId);
        OrderResp resp = orderService.getOrder(orderId);
        return ResponseEntity.ok(resp);
    }
}
```

**Controller 参数转换职责**

Controller 层负责参数转换和基本检查，不写业务逻辑：

- **参数转换**：将 HTTP 请求的原始参数转换为目标类型（如字符串转日期、ID 类型转换等）
- **基本检查**：通过 `@Valid` 触发 Bean Validation，完成参数格式校验
- **禁止业务逻辑**：Controller 不包含任何业务判断或编排逻辑

```java
// ✅ 正确：Controller 负责参数转换，通过 @Valid 触发校验
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResp> createOrder(@Valid @RequestBody CreateOrderReq req) {
        log.info("createOrder called, userId={}", req.getUserId());
        OrderResp resp = orderService.createOrder(req);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/search")
    public ResponseEntity<PageResp<OrderResp>> searchOrders(@Valid SearchOrderReq req) {
        log.info("searchOrders called, keyword={}", req.getKeyword());
        // Controller 负责参数转换：将字符串日期转换为 LocalDate
        if (StringUtils.hasText(req.getStartDateStr())) {
            req.setStartDate(LocalDate.parse(req.getStartDateStr()));
        }
        PageResp<OrderResp> resp = orderService.searchOrders(req);
        return ResponseEntity.ok(resp);
    }
}
```

**Req/Resp 对象继承约束**

Req 和 Resp 对象必须继承公共父类，放置在指定包下：

- **Req 对象**：放在 `bean/req` 包下，必须继承公共父类（`IdReq`、`PageReq`、`UpdateReq`、`RemoveReq`）
- **Resp 对象**：放在 `bean/resp` 包下，必须继承公共父类（`EntityResp`）

```java
// ✅ 正确：Req 对象继承公共父类
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryOrderReq extends PageReq {

    @NotBlank(message = "订单类型不能为空")
    private String orderType;

    private String keyword;
}

@Data
@EqualsAndHashCode(callSuper = true)
public class RemoveOrderReq extends RemoveReq {
    // RemoveReq 已包含 id 字段，无需重复定义
}

// ✅ 正确：Resp 对象继承公共父类
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderResp extends EntityResp {

    private Long userId;
    private BigDecimal totalAmount;
    private String status;
}
```

**参数校验规范：禁止 Assert，统一使用 @Valid**

参数校验统一使用 `@Valid` + Bean Validation 注解，禁止使用 `Assert.notNull()`、`Assert.hasText()` 等 Spring Assert 方法。

```java
// ✅ 正确：使用 @Valid + Bean Validation 注解
@Data
public class CreateOrderReq extends UpdateReq {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "订单名称不能为空")
    private String orderName;

    @NotEmpty(message = "商品列表不能为空")
    private List<OrderItemReq> items;
}

// ❌ 错误：禁止使用 Spring Assert
@PostMapping
public ResponseEntity<OrderResp> createOrder(@RequestBody CreateOrderReq req) {
    Assert.notNull(req.getUserId(), "用户ID不能为空");    // 禁止
    Assert.hasText(req.getOrderName(), "订单名称不能为空"); // 禁止
    // ...
}
```

**Node 示例**

```typescript
// ✅ 正确：Router 只做路由和校验
import { Router } from "express";
import { z } from "zod";

const router = Router();
const getOrderReqSchema = z.object({ orderId: z.coerce.number().int().positive() });

router.get("/:orderId", async (req, res, next) => {
  try {
    const { orderId } = getOrderReqSchema.parse({ orderId: req.params.orderId });
    logger.info("getOrder called, orderId=%d", orderId);
    const resp = await orderService.getOrder(orderId);
    res.json(resp);
  } catch (err) {
    next(err);
  }
});
```

### Service 层

- 业务逻辑编排，事务控制
- 一个方法对应一个完整的业务用例
- 跨 Domain 对象的协调在此层完成

**Java 示例**

```java
// ✅ 正确：Service 编排业务逻辑
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentClient paymentClient;

    @Transactional
    public OrderResp createOrder(CreateOrderReq req) {
        log.info("createOrder called, userId={}", req.getUserId());
        BigDecimal total = req.getItems().stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQty())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order(req.getUserId(), total, OrderStatus.PENDING);
        orderRepository.save(order);
        log.info("order created, orderId={}", order.getId());

        paymentClient.charge(req.getUserId(), total);
        return OrderResp.from(order);
    }
}
```

**Node 示例**

```typescript
// ✅ 正确：Service 编排业务逻辑
import { Decimal } from "decimal.js";

export class OrderService {
  constructor(private orderRepo: OrderRepository, private paymentClient: PaymentClient) {}

  async createOrder(req: CreateOrderReq): Promise<OrderResp> {
    logger.info("createOrder called, userId=%d", req.userId);
    const total = req.items.reduce(
      (sum, item) => sum.add(new Decimal(item.price).mul(item.qty)),
      new Decimal(0)
    );

    const order = new Order(req.userId, total, OrderStatus.PENDING);
    await this.orderRepo.save(order);
    logger.info("order created, orderId=%d", order.id);

    await this.paymentClient.charge(req.userId, total);
    return OrderResp.from(order);
  }
}
```

### Domain 层

- 核心业务逻辑和领域规则
- 业务校验（状态机转换、金额计算）
- 不感知外部服务、不处理 HTTP 细节

**Java 示例**

```java
// ✅ 正确：Domain 专注业务规则
@Entity
@Table(name = "orders")
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private BigDecimal totalAmount;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    protected Order() {} // JPA

    public Order(Long userId, BigDecimal totalAmount, OrderStatus status) {
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public boolean canCancel() {
        return status == OrderStatus.PENDING || status == OrderStatus.PAID;
    }

    public void applyDiscount(BigDecimal rate) {
        if (rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("discount rate must be between 0 and 1");
        }
        this.totalAmount = this.totalAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
```

**Node 示例**

```typescript
// ✅ 正确：Domain 专注业务规则
import { Decimal } from "decimal.js";

export class Order {
  id?: number;
  userId: number;
  totalAmount: Decimal;
  status: OrderStatus;

  constructor(userId: number, totalAmount: Decimal, status: OrderStatus) {
    this.userId = userId;
    this.totalAmount = totalAmount;
    this.status = status;
  }

  canCancel(): boolean {
    return this.status === OrderStatus.PENDING || this.status === OrderStatus.PAID;
  }

  applyDiscount(rate: Decimal): void {
    if (rate.lte(0) || rate.gte(1)) {
      throw new Error("discount rate must be between 0 and 1");
    }
    this.totalAmount = this.totalAmount.mul(rate).toDecimalPlaces(2);
  }
}
```

### Repository 层

- 数据持久化操作
- Repository 封装 ORM/数据访问细节，对外提供领域友好的接口

**Java 示例**

```java
// ✅ 正确：Repository 封装持久化细节
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    @Query("SELECT o FROM OrderEntity o WHERE o.userId = :userId ORDER BY o.id DESC")
    List<OrderEntity> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id")
    Optional<OrderEntity> findById(@Param("id") Long id);
}
```

**Node 示例**

```typescript
// ✅ 正确：Repository 封装持久化细节（Prisma 示例）
import { PrismaClient } from "@prisma/client";

export class OrderRepository {
  constructor(private prisma: PrismaClient) {}

  async findById(id: number): Promise<OrderRecord | null> {
    return this.prisma.order.findUnique({ where: { id } });
  }

  async findByUserId(userId: number, limit = 20): Promise<OrderRecord[]> {
    return this.prisma.order.findMany({
      where: { userId },
      orderBy: { id: "desc" },
      take: limit,
    });
  }

  async save(order: OrderRecord): Promise<void> {
    await this.prisma.order.upsert({
      where: { id: order.id ?? -1 },
      update: order,
      create: order,
    });
  }
}
```

### Client 层（适配层）

- 外部 HTTP/gRPC 服务调用的封装
- 超时设置、重试策略、降级方案

**Java 示例**

```java
// ✅ 正确：Client 封装外部调用
@Component
@RequiredArgsConstructor
public class PaymentClient {

    private final RestTemplate restTemplate;

    @Value("${payment.base-url}")
    private String baseUrl;

    public PaymentResp charge(Long userId, BigDecimal amount) {
        log.info("charge called, userId={}, amount={}", userId, amount);
        try {
            var req = new ChargeReq(userId, amount);
            var resp = restTemplate.postForObject(baseUrl + "/pay", req, PaymentResp.class);
            log.info("charge success, userId={}", userId);
            return resp;
        } catch (ResourceAccessException e) {
            log.warn("charge timeout, userId={}", userId, e);
            return new PaymentResp("fallback", "payment service timeout");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.warn("charge failed, userId={}, status={}", userId, e.getStatusCode(), e);
            return new PaymentResp("failed", "payment rejected");
        }
    }
}
```

**Node 示例**

```typescript
// ✅ 正确：Client 封装外部调用
import axios, { AxiosError } from "axios";

export class PaymentClient {
  private client;

  constructor(baseUrl: string, timeout = 3000) {
    this.client = axios.create({ baseURL: baseUrl, timeout });
  }

  async charge(userId: number, amount: Decimal): Promise<PaymentResp> {
    logger.info("charge called, userId=%d, amount=%s", userId, amount.toString());
    try {
      const resp = await this.client.post("/pay", { userId, amount: amount.toString() });
      logger.info("charge success, userId=%d", userId);
      return resp.data;
    } catch (err) {
      if (err instanceof AxiosError && err.code === "ECONNABORTED") {
        logger.warn("charge timeout, userId=%d", userId);
        return { status: "fallback", message: "payment service timeout" };
      }
      logger.warn("charge failed, userId=%d", userId, err);
      return { status: "failed", message: "payment rejected" };
    }
  }
}
```

---

## 通用约束

- **价格字段**：Java 使用 `BigDecimal`，Node 使用 `decimal.js` / `BigInt`，禁止 `float` / `double`
- **外部调用**：必须设置超时（≤5s）和降级方案
- **异步任务**：Java 使用 `@Async` 或消息队列（RabbitMQ/Kafka），Node 使用 BullMQ 或消息队列
- **日志**：Java 使用 SLF4J，Node 使用 winston/pino；关键链路必须打印日志，异常场景打印完整堆栈
- **类型注解**：Java 所有方法参数和返回值必须标注类型；Node 使用 TypeScript 严格模式，所有函数参数和返回值必须标注类型
- **对象复制**：数据库映射类（Entity）使用类内复制方法（如 `copy()` 方法），其他对象转换统一使用 `BeanUtil.cp`

```java
// ✅ 正确：Entity 使用类内 copy 方法
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private BigDecimal totalAmount;
    private String status;

    public OrderEntity copy() {
        OrderEntity copy = new OrderEntity();
        copy.id = this.id;
        copy.userId = this.userId;
        copy.totalAmount = this.totalAmount;
        copy.status = this.status;
        return copy;
    }
}

// ✅ 正确：其他对象转换使用 BeanUtil.cp
public OrderResp toResp(OrderEntity entity) {
    OrderResp resp = BeanUtil.cp(entity, OrderResp.class);
    resp.setStatus(entity.getStatus().name());
    return resp;
}
```

---

## 产出物

- 代码变更（按 tasks.md 逐项完成）
- 编码报告：`coding/coding_report_v1.md`
    - 变更文件列表
    - 每个文件的变更摘要
    - 未完成的 Task 及其原因
