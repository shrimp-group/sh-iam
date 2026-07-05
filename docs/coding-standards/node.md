# Node/JavaScript/TypeScript 编码规范

## 1. 命名规范

| 类型       | 规则                            | 示例                                          |
|----------|-------------------------------|---------------------------------------------|
| 类名/接口/类型 | 大驼峰 PascalCase                | `UserService`, `IOrderRepository`           |
| 函数/方法    | 小驼峰 camelCase                 | `getUserById()`, `calculateTotal()`         |
| 常量       | UPPER_SNAKE_CASE              | `MAX_RETRY_COUNT`                           |
| 变量       | 小驼峰 camelCase                 | `userName`, `orderList`                     |
| 文件名      | kebab-case                    | `user-service.ts`, `order-controller.ts`    |
| 目录名      | kebab-case                    | `user-service/`, `api-handlers/`            |
| 私有成员     | `#` 前缀或 `_` 前缀                | `#privateField`, `_internalMethod`          |
| 枚举类名     | PascalCase                    | `UserStatus`                                |
| 枚举值      | UPPER_SNAKE_CASE 或 PascalCase | `UserStatus.ACTIVE` 或 `UserStatus.Active`   |
| 布尔变量     | is/has/should 前缀              | `isVisible`, `hasPermission`, `shouldRetry` |
| 事件处理     | on 前缀                         | `onClick`, `onSubmit`                       |

**核心原则**：每个命名都要有业务含义，禁止 `a`, `b`, `c`, `tmp` 等无意义命名。

```typescript
// 反例
let a = 0;
const tmp = user.getName();
const list1: string[] = [];

// 正例
let retryCount = 0;
const userName = user.getName();
const pendingOrderIds: string[] = [];
```

---

## 2. 代码风格

### 缩进与行宽

- 缩进：**2 空格**，禁止 Tab
- 行宽：最大 **100 字符**，超长行在逻辑断点处换行

```typescript
// 超长行换行示例
const orderDetail = await orderService.queryDetailByOrderIdAndUserId(
  orderId,
  userId,
  queryParam,
);
```

### 分号与引号

- 分号：**必须加分号**
- 引号：单引号（`'string'`），模板字符串用反引号

```typescript
// 正确
const name = 'zhangsan';
const greeting = `Hello, ${name}`;

// 错误
const name = "zhangsan";  // 双引号
const greeting = 'Hello, ' + name;  // 应使用模板字符串
```

### 大括号与尾逗号

- 左大括号不换行
- 多行时加尾逗号（trailing comma: all）

```typescript
// 正确
const config = {
  host: 'localhost',
  port: 3000,
  timeout: 5000,  // 尾逗号
};

// 错误
const config = {
  host: 'localhost',
  port: 3000,
  timeout: 5000   // 缺少尾逗号
};
```

### 空行

- 函数之间：1 空行
- 逻辑块之间：1 空行

### import 顺序

按组排列，组间空行：

1. Node.js 内置模块
2. 第三方库
3. 项目内模块

```typescript
import fs from 'fs';
import path from 'path';

import express from 'express';
import lodash from 'lodash';

import { UserService } from '@/services/user-service';
import { OrderController } from '@/controllers/order-controller';
import { logger } from '@/utils/logger';
```

---

## 3. 模块规范

### ES Module 优先

- **使用 ES Module（import/export）**，禁止 CommonJS（require/module.exports）

```typescript
// 正确
import { UserService } from './user-service';
export class OrderService { }

// 错误
const UserService = require('./user-service');
module.exports = OrderService;
```

### 导出规则

- 一个文件一个主要导出
- 默认导出使用 `export default`，命名导出使用 `export`

```typescript
// 默认导出：文件只有一个核心导出时
export default class UserService { }

// 命名导出：工具函数、常量等
export function formatDate(date: Date): string { }
export const MAX_RETRY = 3;
```

### 循环依赖

- 循环依赖必须通过接口解耦

```typescript
// 反例：A 直接依赖 B，B 直接依赖 A → 循环依赖

// 正确：提取公共接口到独立文件
// types/common.ts
export interface IUser { id: string; name: string; }

// service-a.ts
import type { IUser } from '@/types/common';

// service-b.ts
import type { IUser } from '@/types/common';
```

### barrel 文件（index.ts）

- 仅做重导出，**不包含逻辑**

```typescript
// 正确：index.ts 仅重导出
export { UserService } from './user-service';
export { OrderService } from './order-service';
export type { UserDTO } from './types';

// 错误：index.ts 包含业务逻辑
export const userService = new UserService(); // 不允许
```

### 避免深层嵌套导入

- 导入路径不超过 3 层

```typescript
// 正确
import { helper } from '@/utils/helper';

// 反例：过深的导入路径
import { helper } from '@/modules/user/services/internal/utils/helper';
```

---

## 4. 异步编程

### async/await 优先

- **优先使用 async/await**，禁止回调嵌套

```typescript
// 正确
async function getUser(userId: string): Promise<User> {
  const user = await userRepository.findById(userId);
  return user;
}

// 错误：回调嵌套
function getUser(userId: string, callback: (user: User) => void): void {
  userRepository.findById(userId, (user) => {
    callback(user);
  });
}
```

### 必须用 try/catch 包裹 await

```typescript
// 正确
async function processOrder(orderId: string): Promise<void> {
  try {
    const order = await orderService.findById(orderId);
    await paymentService.charge(order);
  } catch (error) {
    logger.error('订单处理失败', { orderId, error });
    throw error;
  }
}

// 错误：未捕获异步错误
async function processOrder(orderId: string): Promise<void> {
  const order = await orderService.findById(orderId); // 可能抛出未捕获异常
  await paymentService.charge(order);
}
```

### 并发操作使用 Promise.all

```typescript
// 正确：并发请求
async function getUserWithOrders(userId: string): Promise<UserWithOrders> {
  const [user, orders] = await Promise.all([
    userService.findById(userId),
    orderService.findByUserId(userId),
  ]);
  return { user, orders };
}

// 错误：串行等待
async function getUserWithOrders(userId: string): Promise<UserWithOrders> {
  const user = await userService.findById(userId);
  const orders = await orderService.findByUserId(userId); // 不必要的串行
  return { user, orders };
}
```

### 长时间运行任务设置超时

```typescript
function withTimeout<T>(promise: Promise<T>, ms: number): Promise<T> {
  const timeout = new Promise<never>((_, reject) =>
    setTimeout(() => reject(new Error(`操作超时: ${ms}ms`)), ms),
  );
  return Promise.race([promise, timeout]);
}

// 使用
const result = await withTimeout(externalService.call(), 5000);
```

### 避免在循环中使用 await

```typescript
// 反例：串行处理
for (const userId of userIds) {
  await sendNotification(userId); // 逐个等待
}

// 正确：并发处理
await Promise.all(userIds.map((userId) => sendNotification(userId)));

// 正确：限制并发数
async function mapConcurrent<T, R>(
  items: T[],
  fn: (item: T) => Promise<R>,
  concurrency: number,
): Promise<R[]> {
  const results: R[] = [];
  for (let i = 0; i < items.length; i += concurrency) {
    const batch = items.slice(i, i + concurrency);
    const batchResults = await Promise.all(batch.map(fn));
    results.push(...batchResults);
  }
  return results;
}
```

### 事件循环理解

- microtask（Promise.then, queueMicrotask）优先于 macrotask（setTimeout, setInterval）
- 避免在 microtask 中递归，否则会阻塞 macrotask

---

## 5. TypeScript 类型规范

### 禁止 any

- **禁止使用 `any`**，必须提供具体类型
- **禁止使用 `@ts-ignore`**，使用 `@ts-expect-error` 并注释原因

```typescript
// 反例
const data: any = response.data;
// @ts-ignore
data.doSomething();

// 正确
const data: UserResponse = response.data;

// 必须抑制错误时：注释原因
// @ts-expect-error 第三方库类型定义缺失，已提 PR 等待合并
externalLib.untypedMethod();
```

### interface vs type

- 接口用 `interface`：定义对象形状、可扩展
- 类型别名用 `type`：联合类型、交叉类型、工具类型

```typescript
// interface：对象形状
interface User {
  id: string;
  name: string;
  email: string;
}

// type：联合类型、工具类型
type Status = 'active' | 'inactive' | 'suspended';
type Nullable<T> = T | null;
type UserDTO = Pick<User, 'id' | 'name'>;
```

### 泛型参数命名

- 使用有意义的命名，而非单字母

```typescript
// 反例
function transform<T, U>(input: T): U { }

// 正确
function transform<TInput, TOutput>(input: TInput): TOutput { }
```

### 严格模式

- `strict: true` 必须开启

```jsonc
// tsconfig.json
{
  "compilerOptions": {
    "strict": true
  }
}
```

### 使用 unknown 而非 any

```typescript
// 反例
function parse(input: unknown): any {
  return JSON.parse(input as string);
}

// 正确
function parse(input: unknown): unknown {
  if (typeof input !== 'string') {
    throw new TypeError('Expected string input');
  }
  return JSON.parse(input);
}
```

### 枚举使用 const enum 或 union type

```typescript
// 推荐：union type（零运行时开销）
type OrderStatus = 'pending' | 'paid' | 'shipped' | 'completed' | 'cancelled';

// 也可：const enum
const enum OrderStatus {
  Pending = 'pending',
  Paid = 'paid',
  Shipped = 'shipped',
  Completed = 'completed',
  Cancelled = 'cancelled',
}
```

### 类型守卫

- 使用 type predicates 或 discriminated unions
- **避免类型断言（as）**，优先类型守卫

```typescript
// discriminated union
interface SuccessResult {
  success: true;
  data: User;
}

interface ErrorResult {
  success: false;
  error: Error;
}

type Result = SuccessResult | ErrorResult;

function handleResult(result: Result): User {
  if (result.success) {
    return result.data; // TypeScript 自动收窄类型
  }
  throw result.error;
}

// type predicate
function isUser(value: unknown): value is User {
  return (
    typeof value === 'object'
    && value !== null
    && 'id' in value
    && 'name' in value
  );
}
```

---

## 6. 错误处理

### 自定义错误类

- 继承 `Error`，区分业务错误和系统错误

```typescript
// 业务错误
class BusinessError extends Error {
  constructor(
    message: string,
    public readonly code: string,
    public readonly context?: Record<string, unknown>,
  ) {
    super(message);
    this.name = 'BusinessError';
  }
}

// 系统错误
class SystemError extends Error {
  constructor(
    message: string,
    public readonly cause?: Error,
  ) {
    super(message);
    this.name = 'SystemError';
  }
}
```

### 核心规则

1. **错误信息必须包含上下文**

```typescript
// 反例
throw new BusinessError('订单创建失败');

// 正确
throw new BusinessError('订单创建失败', 'ORDER_CREATE_FAILED', {
  userId,
  productId,
  amount,
});
```

2. **禁止吞掉错误（空 catch 块）**

```typescript
// 反例
try {
  await sendNotification(user);
} catch (e) {
  // 吞掉错误
}

// 正确
try {
  await sendNotification(user);
} catch (error) {
  logger.warn('通知发送失败', { userId: user.id, error });
}
```

3. **异步错误必须捕获**

```typescript
// 反例：未处理的 Promise
function processData(data: Data): void {
  processAsync(data).then((result) => {
    saveResult(result);
  }); // 缺少 .catch
}

// 正确
async function processData(data: Data): Promise<void> {
  try {
    const result = await processAsync(data);
    await saveResult(result);
  } catch (error) {
    logger.error('数据处理失败', { dataId: data.id, error });
    throw error;
  }
}
```

4. **错误传播：使用 error.cause 链式传递**

```typescript
try {
  await db.query(sql);
} catch (originalError) {
  throw new SystemError('数据库查询失败', originalError); // cause 链
}
```

5. **API 统一错误响应格式**

```typescript
interface ApiErrorResponse {
  success: false;
  error: {
    code: string;
    message: string;
    details?: unknown;
  };
  requestId: string;
  timestamp: string;
}
```

---

## 7. 日志规范

### 结构化日志

- 使用**结构化日志（JSON 格式）**

```typescript
// 正确：结构化日志
logger.info('订单创建成功', {
  orderId: order.id,
  userId: order.userId,
  amount: order.amount,
});

// 输出：
// {"level":"info","message":"订单创建成功","orderId":"123","userId":"456","amount":99.9,"timestamp":"2024-01-01T00:00:00Z"}
```

### 日志级别

| 级别    | 使用场景        | 示例            |
|-------|-------------|---------------|
| error | 系统错误，需要立即处理 | 数据库连接失败、未捕获异常 |
| warn  | 业务异常，需要关注   | 参数校验失败、降级处理   |
| info  | 关键业务流程      | 用户登录、订单创建     |
| debug | 调试信息        | 请求/响应详情、中间变量  |

### 核心规则

1. **禁止 `console.log`**——使用 logger 替代

```typescript
// 反例
console.log('用户登录:', userId);
console.error('出错了:', error);

// 正例
logger.info('用户登录', { userId });
logger.error('系统异常', { error });
```

2. **日志必须包含上下文**

```typescript
// 反例
logger.error('创建订单失败');

// 正确
logger.error('创建订单失败', { userId, productId, amount, error });
```

3. **生产环境不输出 debug 日志**

```typescript
// 正确：根据环境设置日志级别
const logLevel = process.env.NODE_ENV === 'production' ? 'info' : 'debug';
```

4. **敏感信息脱敏**

```typescript
// 反例
logger.info('用户登录', { password: '123456', token: 'abc...' });

// 正确
logger.info('用户登录', {
  userId: user.id,
  password: '******',
  token: maskToken(token), // 仅显示前4位 + ****
});

function maskToken(token: string): string {
  if (token.length <= 4) return '****';
  return token.slice(0, 4) + '****';
}
```

---

## 8. 最佳实践

### 不可变数据优先

- `const` > `let`，**禁止 `var`**

```typescript
// 正确
const MAX_SIZE = 100;
const users = await getUsers();
let retryCount = 0; // 仅在需要重新赋值时使用 let

// 错误
var name = 'test'; // 禁止使用 var
```

### 纯函数优先

- 避免副作用，相同输入始终返回相同输出

```typescript
// 正确：纯函数
function calculateTotal(items: Item[]): number {
  return items.reduce((sum, item) => sum + item.price * item.quantity, 0);
}

// 反例：有副作用
let total = 0;
function calculateTotal(items: Item[]): void {
  total = items.reduce((sum, item) => sum + item.price * item.quantity, 0);
}
```

### 防御性编程

- 校验函数入参

```typescript
function createUser(params: CreateUserParams): User {
  if (!params.name || params.name.trim().length === 0) {
    throw new BusinessError('用户名不能为空', 'INVALID_NAME');
  }
  if (params.age < 0 || params.age > 150) {
    throw new BusinessError('年龄不合法', 'INVALID_AGE', { age: params.age });
  }
  // ...
}
```

### 代码规模限制

| 指标   | 限制                    |
|------|-----------------------|
| 函数长度 | 不超过 **50 行**          |
| 文件长度 | 不超过 **300 行**         |
| 参数个数 | 不超过 **4 个**，超过则使用对象参数 |

```typescript
// 反例：参数过多
function createUser(
  name: string,
  email: string,
  age: number,
  role: string,
  department: string,
): User { }

// 正确：使用对象参数
interface CreateUserParams {
  name: string;
  email: string;
  age: number;
  role: string;
  department: string;
}

function createUser(params: CreateUserParams): User { }
```

### 优先组合而非继承

```typescript
// 反例：继承
class AdminUser extends User { }

// 正确：组合
class AdminUser {
  private user: User;
  private role: AdminRole;

  constructor(user: User, role: AdminRole) {
    this.user = user;
    this.role = role;
  }
}
```

### 使用解构赋值

```typescript
// 正确
const { id, name, email } = user;
const [first, second, ...rest] = items;

// 函数参数解构
function createUser({ name, email }: CreateUserParams): User { }
```

### 使用可选链和空值合并

```typescript
// 正确
const cityName = user?.address?.city ?? '未知';
const displayName = user.nickname ?? user.name;

// 反例
const cityName = user && user.address && user.address.city
  ? user.address.city
  : '未知';
```

---

## 核心编码规则

以下规则为 harness 工程强制规范，所有项目必须遵循：

### 规则 1：禁止调用系统资源

仅能使用当前目录下的代码资源，不得调用系统级命令（如 `child_process.exec()`、`execSync`）或外部系统资源。

```typescript
// ❌ 禁止
import { execSync } from 'child_process';
execSync('ls');

// ✅ 正确：使用项目内代码资源
import { readdir } from 'fs/promises';
await readdir('./data');
```

### 规则 2：保留已有注释

不要移除已添加的注释，除非相关代码块已变动。注释是代码知识的重要载体，删除注释可能导致上下文丢失。

### 规则 3：关键位置加日志

实现业务逻辑时，在关键位置添加 log 日志打印。至少在以下位置添加日志：

- 函数入口（记录入参）
- 业务分支判断点
- 异常捕获点
- 外部调用前后

```typescript
const logger = require('./logger');

async function createOrder(req: CreateOrderReq): Promise<OrderResp> {
  logger.info('创建订单开始', { userId: req.userId, itemCount: req.items.length });
  // ... 业务逻辑
  logger.info('创建订单完成', { orderId: order.id });
  return { order };
}
```

### 规则 4：更新 AGENTS.md 和故事

任务完成后，必须更新 AGENTS.md 以及相关的故事文件，确保文档与代码同步。

### 规则 5：Req/Resp 封装

- 所有请求参数封装 Req 对象（TypeScript interface/type），除非参数只有一个值
- 所有返回内容封装 Resp 对象（TypeScript interface/type），除非返回只有一个值

```typescript
// ❌ 禁止：多参数裸传
app.post('/orders', (userId: string, items: Item[]) => { ... });

// ✅ 正确：封装 Req 对象
interface CreateOrderReq {
  userId: string;
  items: Item[];
}
app.post('/orders', (req: CreateOrderReq) => { ... });

// ❌ 禁止：多字段裸返回
function getOrder(orderId: string): { id: string; status: string; amount: number } { ... }

// ✅ 正确：封装 Resp 对象
interface OrderResp {
  id: string;
  status: string;
  amount: number;
}
function getOrder(orderId: string): OrderResp { ... }

// 例外：只有一个参数/返回值时无需封装
function deleteOrder(id: string): void { ... }  // 单参数，无需 Req
function countOrders(): number { ... }  // 单返回值，无需 Resp
```
