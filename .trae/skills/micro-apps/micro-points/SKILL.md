---
name: "micro-points"
description: "积分账户模块。提供积分钱包、发放、试算、消费(冻结→异步扣减两阶段)、回退、过期、对账能力。修改 micro-points 包(com.wkclz.micro.points)下代码时触发。"
---

# Micro-Points 模块

## 1. 适用场景

当需要以下操作时触发此 Skill：
- 修改 `com.wkclz.micro.points` 包下任何代码
- 实现积分钱包（可用/冻结/历史总额）维护
- 实现积分发放（业务方 `ISSUANCE` / 管理员 `ADMIN_ISSUE`）
- 实现积分试算（100:1 现金换算，只读）
- 实现积分消费（两阶段：同步冻结 `FROZEN` → 异步扣减 `@Async`）
- 实现积分消费取消 / 支付失败补偿（`releaseConsume`，处理 FROZEN/DEDUCTED/CANCELLED 三种状态）
- 实现积分回退（`REFUND`，含原单据校验与超额防护）
- 实现积分过期定时处理（XxlJob 模拟消费流程）
- 实现对账（消费流水 vs COMPLETED 动作记录一致性核对）
- 排查积分流程问题（幂等冲突、用户锁死锁、异步扣减失败、PARTIAL 异常等）
- 实现管理端查询（钱包/获取流水/消费流水/扣减明细 4 类接口）
- 实现 C 端查询（基于登录态 PrincipalContext 的钱包与流水分页）

---

## 2. 架构概览

```
┌──────────────────────────────────────────────────────────────────┐
│                         REST 层（前缀 /micro-points）             │
│                                                                  │
│  PointsRest(C端，只读)            PointsAdminRest(运营端)        │
│  GET  /wallet                     POST /admin/issue              │
│  GET  /earn/page                  GET  /admin/wallet              │
│  GET  /consume/page               GET  /admin/earn/page          │
│                                   GET  /admin/consume/page       │
│                                   GET  /admin/consume/deduction/page│
│                                   GET  /admin/reconcile          │
└──────────┬───────────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────────────┐
│                  对内 Service 层（供业务方调用）                   │
│                                                                  │
│  PointsIssueService     PointsTrialService    PointsConsumeService│
│  (发放，业务方+管理员)   (试算，只读)         (消费，冻结阶段/取消)  │
│                                                                  │
│  PointsRefundService    PointsAsyncDeductService(@Async)         │
│  (回退，含原单据校验)    (异步扣减，第二阶段)                       │
│                                                                  │
│  PointsReconcileService          PointsWalletService             │
│  (对账)                          (钱包，extends BaseService)      │
└──────────┬───────────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────────────┐
│                Helper 层（基于 Redis 的幂等 + 用户锁）              │
│                                                                  │
│  PointsIdempotentHelper           PointsLockHelper               │
│  (结果TTL 24h, 处理中TTL 30s)     (TTL 30s, 重试3次/100ms)        │
└──────────┬───────────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────────────┐
│             Mapper 层 (MyBatis，4 个 Mapper + XML)                │
│                                                                  │
│  PointsWalletMapper       PointsEarnRecordMapper                 │
│  PointsConsumeRecordMapper       PointsDeductionRecordMapper     │
└──────────┬───────────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────────────┐
│             Job 层（XxlJob 定时任务，积分过期回收）                 │
│                                                                  │
│  PointsExpireJob (@XxlJob "pointsExpireHandler")                 │
│  扫描 expire_time < now 且 available_points > 0 的获取流水         │
│  模拟消费流程 → 触发冻结→异步扣减                                   │
└──────────────────────────────────────────────────────────────────┘
```

---

## 3. 核心组件速查

### 实体 (Entity)

| 类名 | 表名 | 说明 |
|------|------|------|
| `PointsWallet` | `points_wallet` | 积分钱包：tenantCode, userCode, availablePoints, frozenPoints, totalEarnedPoints；uk(tenant_code, user_code) |
| `PointsEarnRecord` | `points_earn_record` | 获取流水：flowNo(唯一), points, expireTime(DB 默认 2099-12-31 23:59:59), usedPoints, availablePoints, isUsedUp, pointSourceType, sourceNo |
| `PointsConsumeRecord` | `points_consume_record` | 消费流水：flowNo(唯一), points, orderNo(唯一), status(FROZEN/DEDUCTED) |
| `PointsDeductionRecord` | `points_deduction_record` | 扣减记录（双类型）：flowNo(唯一), orderNo, earnFlowNo(任务记录NULL/动作记录非空), deductionPoints, status(PENDING/PROCESSED/COMPLETED/PARTIAL) |

### 枚举 (Enum)

| 类名 | 值 | 说明 |
|------|------|------|
| `PointsSourceType` | `ISSUANCE`, `REFUND`, `ADMIN_ISSUE` | 积分来源类型（获取流水） |
| `PointsConsumeStatus` | `FROZEN`, `DEDUCTED`, `CANCELLED` | 消费流水状态（`CANCELLED` 仅 `releaseConsume` FROZEN 分支置） |
| `PointsDeductionStatus` | `PENDING`, `PROCESSED`, `COMPLETED`, `PARTIAL`, `CANCELLED` | 扣减记录状态（`CANCELLED` 仅任务记录，`releaseConsume` FROZEN 分支置） |

### Req / Resp

| Req | Resp | 说明 |
|-----|------|------|
| `PointsIssueReq` | `PointsIssueResp` | 发放（flowNo/points/availablePoints/totalEarnedPoints） |
| `PointsTrialReq` | `PointsTrialResp` | 试算（availablePoints/deductAmount/requiredPoints） |
| `PointsConsumeReq` | `PointsConsumeResp` | 消费（flowNo/status=FROZEN/points） |
| `PointsRefundReq` | `PointsRefundResp` | 回退（flowNo/points） |
| `PointsWalletQueryReq` | `PointsWalletResp` | 钱包查询（available/frozen/totalEarned） |
| `PointsEarnPageReq` | `PointsEarnRecordResp` | 获取流水分页 |
| `PointsConsumePageReq` | `PointsConsumeRecordResp` | 消费流水分页 |
| `PointsDeductionPageReq` | `PointsConsumeDeductionResp` | 消费扣减明细（对账） |
| `PointsReconcileReq` | `PointsReconcileResp` | 对账（consumeFlowNo/points/deductedSum/diff/status） |

### Service

| 类名 | 继承 | 核心方法 |
|------|------|----------|
| `PointsWalletService` | `BaseService<PointsWallet, PointsWalletMapper>` | `getOrCreateWallet()`, `addAvailable()`, `freeze()`, `releaseFrozen()` |
| `PointsIssueService` | — | `issuePoints(PointsIssueReq)`（业务方 + 管理员），`doIssue()`（事务内） |
| `PointsTrialService` | — | `trial(PointsTrialReq)`（只读，100:1 换算） |
| `PointsConsumeService` | — | `consume(PointsConsumeReq)`（冻结阶段），`doConsume()`（事务内），`releaseConsume(orderNo, reason)`（支付失败补偿：FROZEN 释放冻结置 CANCELLED；DEDUCTED 调 `refundWithoutLock` 退剩余全部；幂等键 `CANCEL:orderNo`） |
| `PointsAsyncDeductService` | — | `triggerAsyncDeduct(deductionFlowNo)`（@Async），`processAllPending()`（批量兜底），`processOnePending()`，`processOnePendingLocked()`，`doDeduct()`（单一事务） |
| `PointsRefundService` | — | `refund(PointsRefundReq)`，`doRefund()`（事务内，含原单据校验），`refundWithoutLock(PointsRefundReq, tenantCode)`（**包级可见**，供 `releaseConsume` 在已持用户锁场景调用，避免 `RedisLock` 非可重入导致死锁） |
| `PointsReconcileService` | — | `reconcile(PointsReconcileReq)`，`reconcileOne()`，`determineStatus()` |

### Helper

| 类名 | 核心方法 | 说明 |
|------|----------|------|
| `PointsIdempotentHelper` | `tryIdempotent(bizType, bizNo)`, `markProcessing(bizType, bizNo)`, `cacheResult(bizType, bizNo, resultJson)` | 幂等检测，结果键 TTL 24h，处理中键 TTL 30s（SETNX）。`IdempotentBizType` 枚举值：`ISSUE` / `CONSUME` / `REFUND` / `ADMIN_ISSUE` / `CANCEL`（支付失败补偿，bizNo=消费 orderNo） |
| `PointsLockHelper` | `executeWithUserLock(userCode, Supplier)`, `executeWithUserLock(userCode, Runnable)` | 用户级串行锁，TTL 30s，重试 3 次间隔 100ms |

### Mapper

| 类名 | 关键自定义方法 |
|------|---------------|
| `PointsWalletMapper` | `selectByUserCode()`, `updatePointsByVersion()` |
| `PointsEarnRecordMapper` | `selectAvailableBatchByExpireTime()`, `selectExpiredAvailable()`, `sumRefundPointsBySourceNo()` |
| `PointsConsumeRecordMapper` | `selectByOrderNo()`, `selectByUserCodeAndTimeRange()` |
| `PointsDeductionRecordMapper` | `selectPendingTaskRecords()`, `sumCompletedDeductionPointsByOrderNo()`, `selectTaskRecordByOrderNo()`, `selectCompletedActionsByOrderNo()`, `updateStatusByVersion()` |

### Job

| 类名 | Handler 名称 | 说明 |
|------|--------------|------|
| `PointsExpireJob` | `pointsExpireHandler` | 扫描过期流水（每页 100），调用消费服务模拟消费（`order_no=EXPIRY+flowNo`） |

### 常量

| 类名 | 常量 | 值 |
|------|------|------|
| `PointsConstants` | `POINTS_TO_CASH_RATE` | 100（100 积分 = 1 元） |
| | `DEFAULT_EXPIRE_TIME` | "2099-12-31 23:59:59" |
| | `IDEMPOTENT_KEY_PREFIX` | "points:idempotent:" |
| | `LOCK_KEY_PREFIX` | "points:lock:" |

### Route 路由常量

| 常量 | 值 | 说明 |
|---|---|---|
| `PREFIX` | `/micro-points` | 模块前缀 |
| `WALLET` | `/wallet` | C 端钱包查询 |
| `EARN_PAGE` | `/earn/page` | C 端获取流水分页 |
| `CONSUME_PAGE` | `/consume/page` | C 端消费流水分页 |
| `ADMIN_ISSUE` | `/admin/issue` | 管理员手动发放 |
| `ADMIN_WALLET` | `/admin/wallet` | 运营端钱包查询 |
| `ADMIN_EARN_PAGE` | `/admin/earn/page` | 运营端获取流水分页 |
| `ADMIN_CONSUME_PAGE` | `/admin/consume/page` | 运营端消费流水分页 |
| `ADMIN_CONSUME_DEDUCTION_PAGE` | `/admin/consume/deduction/page` | 消费扣减明细 |
| `ADMIN_RECONCILE` | `/admin/reconcile` | 对账查询 |

### 流水号前缀

| 前缀 | 用途 |
|------|------|
| `PI` | 获取流水（发放 `ISSUANCE` / 管理员 `ADMIN_ISSUE` / 回退 `REFUND`） |
| `PC` | 消费流水 |
| `PD` | 扣减记录（任务记录 + 动作记录） |

---

## 4. 核心工作流

### 4.1 积分发放（业务方 / 管理员手动）

```java
// PointsIssueService.issuePoints() 核心流程
public PointsIssueResp issuePoints(PointsIssueReq req) {
    // 1. 参数校验（userCode/points/sourceNo 非空）
    // 2. 解析 pointSourceType（ISSUANCE / ADMIN_ISSUE），决定幂等 bizType
    // 3. 幂等检测（tryIdempotent）+ 标记处理中（markProcessing, SETNX）
    // 4. 用户锁 + 编程式事务（TransactionTemplate）
    //    4.1 getOrCreateWallet（按需创建）
    //    4.2 写 points_earn_record（available=points, used=0, is_used_up=0）
    //    4.3 钱包累加 available + total_earned（乐观锁）
    // 5. 事务提交后缓存幂等结果（cacheResult）
    // 返回 flowNo/points/availablePoints/totalEarnedPoints
}
```

- 流水号前缀 `PI`，由 `RedisIdGenerator.generateIdWithPrefix` 生成
- 管理员手动发放的完整工作流见 §4.8

### 4.2 积分试算（只读）

```java
// PointsTrialService.trial() 核心流程
public PointsTrialResp trial(PointsTrialReq req) {
    // 1. 查询钱包 available（getOrCreateWallet）
    // 2. availableCash = floor(available / 100)  // BigDecimal, RoundingMode.FLOOR
    // 3. 比较 availableCash 与 paymentAmount:
    //    - availableCash >= paymentAmount: 全额抵扣
    //      deductAmount = paymentAmount
    //      requiredPoints = paymentAmount * 100
    //    - availableCash < paymentAmount: 部分抵扣
    //      deductAmount = availableCash
    //      requiredPoints = available - (available % 100)
    // 返回 availablePoints/deductAmount/requiredPoints
}
```

只读操作：不修改任何数据，不获取用户锁，不开启事务。

### 4.3 积分消费（第一阶段：冻结）

```java
// PointsConsumeService.consume() 核心流程
public PointsConsumeResp consume(PointsConsumeReq req) {
    // 1. 参数校验
    // 2. 幂等检测 CONSUME:orderNo + markProcessing
    // 3. 用户锁 + 编程式事务：
    //    3.1 查询钱包，校验 available >= points（不足抛 ValidationException）
    //    3.2 写 points_consume_record（status=FROZEN, order_no 唯一）
    //    3.3 钱包冻结：available -= points, frozen += points（乐观锁）
    //    3.4 写 points_deduction_record 任务记录
    //        （earn_flow_no=NULL, status=PENDING, order_no=消费orderNo, deduction_points=points）
    //    3.5 通过 AtomicReference 回传 deductionFlowNo 给外层
    // 4. 事务提交后缓存幂等结果
    // 5. 锁外触发异步扣减：asyncDeductService.triggerAsyncDeduct(deductionFlowNo)
    // 返回 flowNo/status=FROZEN/points
}
```

异步扣减在**锁外触发**，避免异步任务等待同一用户锁而死锁。

### 4.4 异步扣减（第二阶段）

```java
// PointsAsyncDeductService.doDeduct() 核心流程（单一事务 all-or-nothing）
private Void doDeduct(PointsDeductionRecord task) {
    long need = task.getDeductionPoints();
    long accumulatedPoints = 0;
    int batchN = 1, offset = 0;
    while (true) {
        // 1. 批量拉取可用获取流水（available > 0, expire_time ASC）
        int batchSize = computeBatchSize(batchN);  // 2^(n-1), 上限 1024
        List<PointsEarnRecord> batch = earnMapper.selectAvailableBatchByExpireTime(...);
        if (batch 为空) break;

        // 2. 遍历批次，逐条扣减
        for (earn : batch) {
            if (accumulatedPoints >= need) break;
            long deduct = Math.min(earn.availablePoints, need - accumulatedPoints);
            // 2.1 写 COMPLETED 动作记录（earn_flow_no 非空）
            // 2.2 更新 earn：usedPoints += deduct, availablePoints -= deduct, isUsedUp 视情况
            //     （乐观锁 updateByIdSelective，失败抛异常回滚）
            accumulatedPoints += deduct;
        }
        offset += batch.size();
        if (accumulatedPoints >= need) break;
        batchN++;
    }

    // 3. 处理结果
    if (accumulatedPoints >= need) {
        // 任务记录置 PROCESSED（乐观锁）
        // 钱包 releaseFrozen(need)
        // 消费流水 FROZEN → DEDUCTED
    } else {
        // 任务记录置 PARTIAL（乐观锁）
        // 钱包 releaseFrozen(accumulatedPoints)（实际扣减额）
        // 告警日志
    }
}

// 批量大小计算：2^(n-1)，n>=12 时封顶 1024
private int computeBatchSize(int batchN) {
    if (batchN <= 1) return 1;
    int shift = batchN - 1;
    if (shift >= 10) return 1024;  // MAX_BATCH_SIZE
    return 1 << shift;
}
```

**触发方式**：
- `triggerAsyncDeduct(deductionFlowNo)`：消费后 `@Async` 触发单条 PENDING
- `processAllPending()`：定时任务批量扫描所有 PENDING（兜底/重试，每页 200 条）

**用户锁非可重入**：`processAllPending` 外层已按用户加锁，遍历用户任务时直接调用 `processOnePendingLocked`，不重复获取锁。

### 4.5 积分回退

```java
// PointsRefundService.doRefund() 核心流程（事务内）
private PointsRefundResp doRefund(PointsRefundReq req, String tenantCode) {
    // 1. 按 orderNo 查原消费记录（consumeMapper.selectByOrderNo）
    // 2. 校验：原消费存在且 status=DEDUCTED（否则抛异常）
    // 3. 计算可退回额：
    //    total_deducted = sum(COMPLETED 动作记录 deduction_points where order_no=原orderNo)
    //    already_refunded = sum(REFUND 获取流水 points where source_no=原orderNo)
    //    refundable = total_deducted - already_refunded
    // 4. 超额防护：refund_points > refundable 时抛异常
    // 5. 写 points_earn_record（point_source_type=REFUND, source_no=原orderNo, 新 expire_time）
    // 6. 钱包累加 available + total_earned（乐观锁）
    // 返回 flowNo/points
}
```

- 回退**不调用 `PointsIssueService.issuePoints`**（外层已做幂等/锁，重复获取锁会死锁）
- **不更新原消费记录状态**（保持 `DEDUCTED`）
- **幂等键动态决定**：`refundNo` 非空时为 `REFUND:refundNo`（支持同一 orderNo 多次部分退款），为空时为 `REFUND:orderNo`（全额退款，向后兼容）
- `orderNo` 始终用于查找原消费记录、超额防护计算、回退获取流水的 `source_no`（不受 refundNo 影响）
- `doRefund` 方法内部不引入 refundNo，仅 refund 方法外层根据 refundNo 计算幂等 bizNo

### 4.6 积分过期定时任务

```java
// PointsExpireJob.execute() (@XxlJob "pointsExpireHandler")
public void execute() {
    LocalDateTime now = LocalDateTime.now();
    int offset = 0;
    while (true) {
        // 分页扫描过期且可用的获取流水（每页 100）
        List<PointsEarnRecord> expiredList = earnMapper.selectExpiredAvailable(now, offset, 100);
        if (expiredList 为空) break;

        for (earn : expiredList) {
            try {
                // 构造过期消费请求：points=available, orderNo=EXPIRY+earnFlowNo
                PointsConsumeReq req = new PointsConsumeReq();
                req.setPoints(earn.getAvailablePoints());
                req.setReason("积分过期：" + earn.getFlowNo());
                req.setOrderNo("EXPIRY" + earn.getFlowNo());
                consumeService.consume(req);  // 触发冻结→异步扣减
            } catch (Exception e) {
                // 余额不足是正常情况（已冻结的过期积分），跳过不报错
                log.warn("过期处理失败, 原因: {}", e.getMessage());
            }
        }
        offset += 100;
    }
}
```

### 4.7 对账

```java
// PointsReconcileService.reconcile() 核心流程
public List<PointsReconcileResp> reconcile(PointsReconcileReq req) {
    // 1. 参数校验（tenantCode/userCode 非空）
    // 2. 查询用户消费流水（按 userCode，可选时间范围）
    // 3. 遍历每条消费流水，调用 reconcileOne:
    //    3.1 聚合 COMPLETED 动作记录（earn_flow_no 非空）deduction_points 之和
    //    3.2 diff = consume.points - deductedSum
    //    3.3 查询任务记录（earn_flow_no IS NULL）判断 PENDING / PARTIAL
    //    3.4 determineStatus:
    //        - PARTIAL 任务记录 → "异常待处理"（优先级最高）
    //        - DEDUCTED 消费: diff==0 → "一致", 否则 → "不一致"
    //        - FROZEN 消费: 存在 PENDING → "冻结中", 否则 → "异常"
    // 返回 List<PointsReconcileResp>
}
```

仅统计 COMPLETED 动作记录（`earn_flow_no` 非空），不统计任务记录。

### 4.8 管理员手动发放

```java
// PointsAdminRest.adminIssue() 核心流程
@PostMapping("/admin/issue")
public R<PointsIssueResp> adminIssue(@Valid @RequestBody PointsIssueReq req) {
    // 1. 强制 pointSourceType=ADMIN_ISSUE（REST 层控制，忽略入参传入值）
    req.setPointSourceType(PointsSourceType.ADMIN_ISSUE.name());
    // 2. 租户编码取管理员登录态（不允许跨租户发放）
    req.setTenantCode(PrincipalContext.getTenantCode());
    log.info("管理员手动发放积分, tenantCode={}, userCode={}, points={}, sourceNo={}", ...);
    // 3. 复用 PointsIssueService.issuePoints，与业务方发放走同一服务（见 §4.1）
    //    幂等 bizType=ADMIN_ISSUE（与 ISSUANCE 区分，避免业务方与管理员同时发放互相冲突）
    PointsIssueResp resp = issueService.issuePoints(req);
    return R.ok(resp);
}
```

- 入口：`POST /micro-points/admin/issue`，入参 `PointsIssueReq`（userCode/points/sourceNo/reason/expireTime）
- 强制 `pointSourceType=ADMIN_ISSUE`（REST 层覆盖入参，防止业务方误传 `ISSUANCE`）
- 复用 `PointsIssueService.issuePoints`，与业务方发放走同一服务（见 §4.1）
- 幂等检测：`bizType=ADMIN_ISSUE`，与 `ISSUANCE` 区分（避免业务方与管理员同时发放互相冲突）
- 租户编码取管理员登录态（`PrincipalContext.getTenantCode`），不允许跨租户发放
- `createBy` 由框架 `MyBatisUpdateInterceptor` 自动填充管理员账号
- 流水号前缀 `PI`，由 `RedisIdGenerator.generateIdWithPrefix` 生成
- 不在 REST 层获取用户锁，由 `PointsIssueService` 内部 `PointsLockHelper.executeWithUserLock` 统一管理

### 4.9 管理端查询

管理端查询接口均为**只读**，无幂等检测、无用户锁、无事务。租户编码取管理员登录态，userCode 由入参指定（运营人员可查询任意用户）。

| 接口 | 路径 | 入参 | 实现 |
|------|------|------|------|
| 钱包查询 | `GET /micro-points/admin/wallet` | `PointsWalletQueryReq`（userCode） | `PointsWalletService.getOrCreateWallet(tenantCode, userCode)` → 转换为 `PointsWalletResp`（available/frozen/totalEarned） |
| 获取流水分页 | `GET /micro-points/admin/earn/page` | `PointsEarnPageReq`（userCode + 分页） | `BeanUtil.cp(req, PointsEarnRecord.class)` + 强制 tenantCode/userCode → `PageQuery.page(query, earnMapper::selectByEntity)` → `convert(PointsEarnRecordResp.class)` |
| 消费流水分页 | `GET /micro-points/admin/consume/page` | `PointsConsumePageReq`（userCode + 分页） | `BeanUtil.cp(req, PointsConsumeRecord.class)` + 强制 tenantCode/userCode → `PageQuery.page(query, consumeMapper::selectByEntity)` → `convert(PointsConsumeRecordResp.class)` |
| 消费扣减明细分页 | `GET /micro-points/admin/consume/deduction/page` | `PointsConsumePageReq`（userCode + 分页） | 1. 分页查询消费流水；2. 对每条消费流水调用 `deductionMapper.selectCompletedActionsByOrderNo(tenantCode, orderNo)` 关联查询 COMPLETED 扣减动作记录（earn_flow_no 非空）；3. 计算 `deductedSum`（动作记录 deduction_points 之和）；4. 组装 `PointsConsumeDeductionResp`（consume 信息 + deductions + deductedSum） |

**只读特性**：
- 不调用幂等检测（`PointsIdempotentHelper`）
- 不获取用户锁（`PointsLockHelper`）
- 不开启事务（无 `@Transactional` / `TransactionTemplate`）
- 不修改任何数据

### 4.10 C 端查询

C 端查询接口基于登录态 userCode（`PrincipalContext.getTenantCode/getUserCode`），均为**只读**，无幂等检测、无用户锁、无事务。

接口路径加 `/custom` 前缀（与 micro-pay 的 `CustomPayOrderRest` 约定对齐）：完整路径形如 `/micro-points/custom/wallet`。

**强制以登录态为准**：所有 C 端接口在 REST 层强制覆盖入参中的 `tenantCode` 和 `userCode`，防止越权查询他人积分。

| 接口 | 路径 | 入参 | 实现 |
|------|------|------|------|
| 钱包查询 | `GET /micro-points/custom/wallet` | 无 | `PrincipalContext.getTenantCode/getUserCode` → `PointsWalletService.getOrCreateWallet(tenantCode, userCode)` → 转换为 `PointsWalletResp`（available/frozen/totalEarned） |
| 获取流水分页 | `GET /micro-points/custom/earn/page` | `PointsEarnPageReq`（仅分页参数） | `BeanUtil.cp(req, PointsEarnRecord.class)` + 强制覆盖 `tenantCode/userCode` 为登录态 → `PageQuery.page(query, earnMapper::selectByEntity)` → `convert(PointsEarnRecordResp.class)` |
| 消费流水分页 | `GET /micro-points/custom/consume/page` | `PointsConsumePageReq`（仅分页参数） | `BeanUtil.cp(req, PointsConsumeRecord.class)` + 强制覆盖 `tenantCode/userCode` 为登录态 → `PageQuery.page(query, consumeMapper::selectByEntity)` → `convert(PointsConsumeRecordResp.class)` |

**只读特性**：
- 不调用幂等检测（`PointsIdempotentHelper`）
- 不获取用户锁（`PointsLockHelper`）
- 不开启事务（无 `@Transactional` / `TransactionTemplate`）
- 不修改任何数据
- 忽略入参中的 `userCode/tenantCode`，防止越权查询

### 4.11 积分消费取消（支付失败补偿）

```java
// PointsConsumeService.releaseConsume() 核心流程
// 用途：支付失败补偿（micro-pay 集成场景，支付 helper 调用失败时调用）
public PointsConsumeResp releaseConsume(String orderNo, String reason) {
    // 1. 参数校验
    // 2. 幂等检测 CANCEL:orderNo + markProcessing
    // 3. 查询原消费记录（按 orderNo），获取 userCode/tenantCode/points/status
    // 4. 用户锁 + 编程式事务：
    //    4.1 FROZEN 分支：releaseFrozen 释放冻结积分；消费流水置 CANCELLED；PENDING 任务记录置 CANCELLED
    //    4.2 DEDUCTED 分支：计算 refundable = consume.points - already_refunded
    //                       若 refundable > 0 调 refundWithoutLock（refundNo=null，幂等键 REFUND:orderNo）
    //    4.3 CANCELLED 分支：幂等返回（不应到达）
    // 5. 事务提交后缓存幂等结果
}
```

- **触发场景**：micro-pay `ShopOrderService.createPayOrder` 在 outTradeNo 生成、积分消费成功、payOrder 持久化后调用支付 helper（微信/支付宝/模拟支付）失败时，调 `releaseConsume(outTradeNo, "支付失败")` 补偿并向上抛出原异常；`mockPayWithOrderInfo` 在 mock 回调外层 try-catch 失败时同样调用
- **幂等键** `CANCEL:orderNo`：基于消费单据号，重复调用直接返回首次结果（避免支付重试导致重复释放）
- **FROZEN 分支**：仅释放冻结（`releaseFrozen`），不触发 refund；消费流水与 PENDING 任务记录均置 `CANCELLED`，后续异步扣减即使触发也会因任务记录非 PENDING 而跳过
- **DEDUCTED 分支**：调 `PointsRefundService.refundWithoutLock`（包级可见，不重复获取用户锁），退剩余全部积分（`refundable = consume.points - already_refunded`）；不更新原消费记录状态，由 refund 写回退获取流水（`source_no=原 orderNo`）
- **`refundWithoutLock` 的存在原因**：`RedisLock` 非可重入，`releaseConsume` 已持用户锁，若再调公开 `refund` 会重复获取锁导致死锁；故 `refundWithoutLock` 跳过锁获取直接执行 `doRefund` 事务逻辑
- **CANCELLED 分支**：幂等返回（理论不应到达，仅作防御性处理）

---

## 5. 数据模型

### points_wallet（积分钱包）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| tenant_code | varchar(31) | 租户编码 |
| user_code | varchar(31) | 用户编码 |
| available_points | bigint | 可用积分 |
| frozen_points | bigint | 冻结积分 |
| total_earned_points | bigint | 历史总获得积分 |
| 基础字段 | — | sort/create_time/create_by/update_time/update_by/remark/version/deleted |
| 索引 | — | uk(tenant_code, user_code) |

### points_earn_record（积分获取流水）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| tenant_code, user_code | varchar(31) | 租户/用户 |
| flow_no | varchar(64) | 流水号（系统生成，唯一） |
| earn_time | datetime | 获取时间 |
| points | bigint | 获取积分数 |
| reason | varchar(255) | 获取原因 |
| expire_time | datetime | 到期时间（DB 默认 2099-12-31 23:59:59） |
| used_points | bigint | 已使用积分数 |
| available_points | bigint | 可用积分数 |
| is_used_up | tinyint | 是否已使用完(0/1) |
| point_source_type | varchar(16) | 来源类型（`PointsSourceType`：ISSUANCE/REFUND/ADMIN_ISSUE） |
| source_no | varchar(64) | 来源单据号（发放为业务单据号；回退为原消费 order_no） |
| 基础字段 | — | 同上 |
| 索引 | — | uk(flow_no), idx(user_code, expire_time), idx(source_no) |

### points_consume_record（积分消费流水）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| tenant_code, user_code | varchar(31) | 租户/用户 |
| flow_no | varchar(64) | 流水号（唯一） |
| consume_time | datetime | 使用时间 |
| points | bigint | 使用积分数 |
| reason | varchar(255) | 使用原因 |
| order_no | varchar(64) | 关联单据号（业务单据，唯一） |
| status | varchar(16) | FROZEN 冻结 / DEDUCTED 已扣减 |
| 基础字段 | — | 同上 |
| 索引 | — | uk(order_no), idx(user_code, consume_time), idx(flow_no) |

### points_deduction_record（积分扣减记录，双类型）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| tenant_code, user_code | varchar(31) | 租户/用户 |
| flow_no | varchar(64) | 扣减流水号（唯一） |
| order_no | varchar(64) | 关联消费单据号（= 消费流水 order_no） |
| earn_flow_no | varchar(64) | 获取流水号（任务记录 NULL / 动作记录非空） |
| deduction_points | bigint | 扣减金额 |
| status | varchar(16) | PENDING/PROCESSED/COMPLETED/PARTIAL |
| 基础字段 | — | 同上 |
| 索引 | — | idx(status, user_code), idx(order_no), idx(earn_flow_no) |

**两类记录**：
- 任务记录（`earn_flow_no = NULL`）：消费时创建，`status=PENDING`，处理完置 `PROCESSED` 或 `PARTIAL`
- 动作记录（`earn_flow_no` 非空）：异步扣减时创建，`status=COMPLETED`

---

## 6. 配置项

本模块无额外配置项。依赖 Spring Boot 自动配置：

- 自动配置类：`com.wkclz.micro.points.PointsAutoConfig`（含 `@EnableAsync`）
- 注册文件：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Mapper XML 路径：`classpath:mapper/**/*.xml`
- XxlJob appName：默认取 `spring.application.name`，Handler 名称 `pointsExpireHandler`

---

## 7. 依赖

### Maven 依赖

| groupId | artifactId | 说明 |
|---|---|---|
| `com.wkclz.framework` | `sh-core` | BaseEntity、R、ValidationException、UserContext、注解 |
| `com.wkclz.framework` | `sh-mybatis` | BaseService、BaseMapper、PageQuery、MyBatis 拦截器 |
| `com.wkclz.framework` | `sh-redis` | RedisHelper、RedisLock、RedisIdGenerator |
| `com.wkclz.framework` | `sh-spring` | SpringContextHolder |
| `com.wkclz.framework` | `sh-xxljob` | @XxlJob 注解 |
| `com.wkclz.framework` | `sh-web` | ErrorHandler、RestHelper |
| `com.wkclz.iam` | `iam-contract-api` | PrincipalContext（获取 tenantCode/userCode） |

### 模块间依赖

- 无硬依赖其他 micro-* 模块（积分模块独立运行）

---

## 8. 注意事项

### 幂等检测
- 幂等键 `points:idempotent:{bizType}:{bizNo}`（结果键 TTL 24h）
- 处理中键 `points:idempotent:proc:{bizType}:{bizNo}`（TTL 30s，SETNX 防并发抢占）
- 幂等检测使用业务单据号（`source_no` / `order_no` / `refund_no`），**不依赖 `flow_no`**
- 命中已处理：直接返回首次结果 JSON 反序列化，不重复变更数据
- **回退幂等键动态决定**：`refundNo` 非空时 bizNo=`refundNo`（`REFUND:refundNo`，支持同一 orderNo 多次部分退款）；为空时 bizNo=`orderNo`（`REFUND:orderNo`，全额退款向后兼容）
- **支付失败补偿幂等键**：`CANCEL:orderNo`（bizType=`CANCEL`，bizNo=消费单据号）。`releaseConsume` 重复调用直接返回首次结果，避免支付重试导致重复释放冻结或重复 refund。`releaseConsume` 在 DEDUCTED 分支内部调 `refundWithoutLock` 时传 `refundNo=null`，复用 `REFUND:orderNo` 幂等键（与全额退款向后兼容，全额退款仅可执行一次）

### 用户级串行锁
- 锁键 `points:lock:{userCode}`（TTL 30s 硬上限，避免持锁线程异常导致死锁）
- 重试 3 次，间隔 100ms（平滑瞬时并发）
- `RedisLock` **非可重入**：嵌套调用必须使用 `*Locked` 变体方法（如 `processOnePendingLocked` / `PointsRefundService.refundWithoutLock`），避免重复获取锁导致死锁
- 发放、消费、回退、异步扣减、支付失败补偿（`releaseConsume`）均获取用户锁

### 事务
- 所有积分写操作使用 `@Transactional(rollbackFor = Exception.class)` 或 `TransactionTemplate` 编程式事务
- 发放/消费/回退采用 `TransactionTemplate` 包裹 `doXxx` 方法（避免 `@Transactional` 同类自调用失效）
- 异步扣减：每条 PENDING 记录处理为**单一事务**（all-or-nothing），批量拉取仅用于 SELECT 优化
- `PointsWalletService` 的写方法（`addAvailable`/`freeze`/`releaseFrozen`）不加 `@Transactional`，由调用方统一管理事务

### 异步扣减
- `@Async` 必须由外部 Bean 调用才能使 Spring AOP 代理生效（`PointsConsumeService` 调用 `PointsAsyncDeductService.triggerAsyncDeduct` 是跨 Bean 调用）
- 异步任务异常不向上传播，PENDING 保留供下次 `processAllPending` 重试
- `processAllPending` 外层已按用户加锁，遍历用户任务时直接调用 `processOnePendingLocked`，不重复获取锁

### 批量拉取策略
- 批次大小 `2^(n-1)`：1 → 2 → 4 → 8 → ... → 1024
- 达到 1024 后**保持 1024** 继续拉取
- 每批按 `expire_time ASC` 排序（FIFO，最近到期优先扣减）
- 拉取够后批量更新涉及的获取流水，所有 UPDATE 在同一事务内

### 扣减记录双类型
- 任务记录（`earn_flow_no = NULL`）：消费时创建，`PENDING` → `PROCESSED` / `PARTIAL`
- 动作记录（`earn_flow_no` 非空）：异步扣减时创建，`COMPLETED`
- 对账时**仅统计 COMPLETED 动作记录**，不统计任务记录

### 回退校验
- 原消费必须存在且 `status=DEDUCTED`（`FROZEN` 不可回退）
- `refund_points ≤ refundable = total_deducted - already_refunded`
- `total_deducted` = COMPLETED 动作记录 deduction_points 之和
- `already_refunded` = REFUND 获取流水（`source_no=orderNo`）points 之和
- 不更新原消费记录状态（保持 `DEDUCTED`）
- 同一 orderNo 仅支持单次完整回退

### 其他
- 积分使用 `long` 类型，现金换算用 `BigDecimal`（仅展示）
- `expire_time` DB 默认 `2099-12-31 23:59:59`（即默认永不过期）
- `point_source_type` 在代码中对应枚举 `PointsSourceType`
- 过期与消费冻结竞态：已冻结的过期积分，过期消费因钱包余额校验失败跳过，不报错

---

## 9. 常见问题

| 问题 | 原因 | 解决 |
|---|---|---|
| 报"用户积分操作处理中，请稍后重试" | 同一用户积分操作正在处理，RedisLock 被占用 | 稍后重试，TTL 30 秒自动释放 |
| 重复调用发放返回首次结果 | 幂等检测正常行为（命中 `ISSUANCE:sourceNo`） | 业务设计如此，避免网络重试导致重复发放 |
| 异步扣减一直 PENDING | `triggerAsyncDeduct` 失败或 `@Async` 未生效 | 检查 `@EnableAsync`，`processAllPending` 兜底重试 |
| 消费报"可用积分不足" | `wallet.availablePoints < points` | 事务回滚不写数据，前端提示用户 |
| 回退报"原消费未完成扣减" | 原消费 `status=FROZEN`（异步扣减未完成） | 等异步扣减完成（`DEDUCTED`）后再回退 |
| 回退报"退回积分超过原单据扣减积分" | 超额防护触发 | `refundable = total_deducted - already_refunded`，多次部分回退累计不超过 total_deducted |
| 钱包累加报"钱包更新冲突" | 乐观锁 `version` 冲突 | 并发场景重试即可 |
| `@Async` 不生效 | 同类自调用导致 Spring AOP 代理失效 | `PointsConsumeService` 调用 `PointsAsyncDeductService` 是跨 Bean，正常生效；确认 `PointsAutoConfig` 上有 `@EnableAsync` |
| 对账发现 `PARTIAL` 任务记录 | 获取流水积分不足（正常流程不应发生） | 数据不一致的防御性处理，人工介入排查 |
| 对账发现"不一致" | COMPLETED 动作记录之和 ≠ 消费 points | 人工排查扣减记录与消费流水 |
| 模块未被 Spring 扫描 | AutoConfiguration.imports 缺失或包路径错误 | 检查 imports 文件和 `@ComponentScan` / `@MapperScan` 包路径 |
