---
name: "micro-pay"
description: "支付集成模块，支持微信支付V3/支付宝/模拟支付的下单、回调、退款、超时取消。修改 micro-pay 包(com.wkclz.micro.pay)下代码时触发"
---

# Micro-Pay 模块

## 1. 适用场景

当需要以下操作时触发此 Skill：
- 修改 `com.wkclz.micro.pay` 包下任何代码
- 实现微信支付/支付宝支付的下单、回调、退款流程
- 配置支付参数（微信支付配置、支付宝配置）
- 处理支付异步通知、退款异步通知
- 实现支付订单状态同步、超时自动取消
- 扩展 `PayNoticeSpi` 支付通知回调接口
- 集成积分支付（下单时积分消费 `consume` / 支付失败补偿 `releaseConsume` / 退款时总单 `releaseConsume` 释放全部积分或子单 `pointsRefundService.refund` 按子单 points 退还）
- 排查支付流程问题（回调未收到、金额不一致、状态异常等）

---

## 2. 架构概览

```
┌──────────────────────────────────────────────────────────────────┐
│                         REST 层                                  │
│                                                                  │
│  WxpayConfigRest      AlipayConfigRest      CustomPayOrderRest  │
│  (微信支付配置CRUD)    (支付宝配置CRUD)       (模拟支付/订单状态)   │
│                                                                  │
│  PayorderRefundRest   WxpayNotifyRest       AlipayNotifyRest    │
│  (退款申请)           (微信支付/退款回调)     (支付宝回调/验签)    │
└──────────┬───────────────┬───────────────┬───────────────────────┘
           │               │               │
           ▼               ▼               ▼
┌──────────────────────────────────────────────────────────────────┐
│                        Service 层                                │
│                                                                  │
│  PayWxpayConfigService    PayAlipayConfigService    PayOrderService
│  (微信配置CRUD)           (支付宝配置CRUD)           (订单CRUD/状态)
│                                                                  │
│  ShopOrderService                                                │
│  (下单/退款编排：参数校验→旧单处理→调用Helper→返回支付信息)        │
└──────────┬───────────────┬───────────────┬───────────────────────┘
           │               │               │
           ▼               ▼               ▼
┌──────────────────────────────────────────────────────────────────┐
│                      Helper + Cache 层                           │
│                                                                  │
│  WxpayHelper              AlipayHelper                          │
│  (微信下单/回调/退款/关单)  (支付宝下单/回调/关单/验签)            │
│                                                                  │
│  WxpayClientCache         AlipayClientCache                     │
│  (WxPayService多租户缓存)  (AlipayClient多租户缓存)               │
└──────────┬───────────────┬───────────────┬───────────────────────┘
           │               │               │
           ▼               ▼               ▼
┌──────────────────────────────────────────────────────────────────┐
│                    Mapper + SPI + Schedule 层                    │
│                                                                  │
│  PayWxpayConfigMapper  PayAlipayConfigMapper  PayOrderMapper    │
│  PayNoticeSpi          WxpayOrderSchedule     WxPayTimeoutSchedule
│  (支付通知SPI)          (微信支付状态同步)      (超时订单自动取消)  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 3. 核心组件速查

### 实体 (Entity)

| 类名 | 表名 | 说明 |
|------|------|------|
| `PayWxpayConfig` | `pay_wxpay_config` | 微信支付配置：appId, mchId, mchV3Key, apiclientKey/Cert, notifyUrl, returnUrl, refundNotifyUrl, verifySign |
| `PayAlipayConfig` | `pay_alipay_config` | 支付宝配置：appId, merchantPrivateKey, alipayPublicKey, appPublicKey, notifyUrl, returnUrl, signType, charset, isProd |
| `PayOrder` | `pay_order` | 支付订单：outTradeNo, orderNo, totalAmount, discountAmount, paymentAmount, payStatus, payMethod, payFlowNo, payTime, terminalType, **points**（本次订单使用积分数量，`Long`，默认 `0L`）, **refundedAmount**（已退款金额，`BigDecimal`，默认 `0`）等 |

### DTO

| 类名 | 继承 | 扩展字段 |
|------|------|----------|
| `PayOrderDto` | `PayOrder` | aliPayBody, wxpayUrl, prepayId, jsapiResult, timeoutMinute |
| `OrderInfoForPay` | — (implements Serializable) | 订单支付信息（从订单模块经 SPI 获取）：orderNo, userCode, tenantCode, totalAmount, discountAmount, paymentAmount, **points**（本次订单使用的积分数量，`Long`，可空）, orderDesc, orderStatus |
| `PayWxpayConfigDto` | `PayWxpayConfig` | tenantName |
| `PayAlipayConfigDto` | `PayAlipayConfig` | tenantName |

### 枚举 (Enum)

| 类名 | 值 | 说明 |
|------|------|------|
| `PayMethod` | `ALI_PAY`, `WX_PAY`, `UNION_PAY`, `MOCK_PAY` | 支付方式 |
| `PayStatus` | `NEW`, `PAYING`, `PAYERROR`, `ORDERNOTEXIST`, `CLOSED`, `CANCEL`, `PAID`, `REFUNDING`, `REFUNDED`, `FINISHED` | 支付状态 |
| `TerminalType` | `PC`, `H5`, `APP`, `WX`, `MINIAPP` | 终端类型 |

### VO

| 类名 | 说明 |
|------|------|
| `AlipayNotify` | 支付宝异步通知数据结构（gmtCreate, tradeStatus, outTradeNo, totalAmount, tradeNo 等） |

### Req

| 类名 | 说明 |
|------|------|
| `PayOrderRefundReq` | 退款请求：orderNo（必填）, reason（退款原因）, **subOrderNo**（子单号，`String`，可空，`null`/空 → 总单退款，非空 → 子单退款）, **refundAmount**（本次退款金额，`BigDecimal`，可空，`null` 表示全单退款）, **refundNo**（本次退款单号，`String`，可空，部分退款时必传，为空时自动生成 `outTradeNo-R+timestamp`） |

### Service

| 类名 | 继承 | 核心方法 |
|------|------|----------|
| `PayWxpayConfigService` | `BaseService<PayWxpayConfig, PayWxpayConfigMapper>` | `getWxpayConfigPage()`, `getDetail()`, `create()`, `update()` |
| `PayAlipayConfigService` | `BaseService<PayAlipayConfig, PayAlipayConfigMapper>` | `getAlipayConfigPage()`, `getDetail()`, `create()`, `update()` |
| `PayOrderService` | `BaseService<PayOrder, PayOrderMapper>` | `getActivePayOrder()`, `getPayOrderStatus2Custom()`, `getPayOrderByOutTradeNo()`, `mockPay()`, `create()`, `update()` |
| `ShopOrderService` | — | `createPayOrder()`（outTradeNo 生成 + payOrder 持久化后，若 `points > 0` 调 `consumeService.consume`；支付 helper 调用 try-catch，失败调 `releaseConsume` 补偿后向上抛出）, `managerPayRefund(PayOrderRefundReq req)`（总单退款 `subOrderNo==null` 调 `releaseConsume` 释放全部积分；子单退款 `subOrderNo!=null` 调 `pointsRefundService.refund` 退还子单积分，积分取自 `payOrderSpi.getOrderInfoForPay(subOrderNo).getPoints()`）, `mockPayWithOrderInfo()`（mock 回调外层 try-catch，失败调 `releaseConsume`） |

### Helper

| 类名 | 核心方法 |
|------|----------|
| `WxpayHelper` | `pay()` — 微信JSAPI下单; `payClose()` — 关闭微信订单; `payNotify()` — 微信支付回调处理; `wxTradeRefund(PayOrder, BigDecimal refundAmount, String refundNo, String reason)` — 微信退款（`refundAmount==null` 全单退款，非空部分退款；`outRefundNo` 使用 `refundNo`）; `wxRefundNotify()` — 微信退款回调; `getRequestHeader()` — 解析微信通知签名头 |
| `AlipayHelper` | `pay()` — 支付宝PC/H5下单; `payClose()` — 关闭支付宝订单; `payNotify()` — 支付宝回调处理; `signVerifie()` — 支付宝验签; `printBack()` — 响应输出 |

### Cache

| 类名 | 核心方法 | 缓存策略 |
|------|----------|----------|
| `WxpayClientCache` | `getClient()`, `getConfig()`, `clearCache()` | 本地Map缓存WxPayService, Redis Pub/Sub通知刷新, 12s轮询检测 |
| `AlipayClientCache` | `getClient()`, `getConfig()`, `clearCache()` | 本地Map缓存AlipayClient, Redis Pub/Sub通知刷新, 12s轮询检测 |

### SPI

| 类名 | 方法 | 说明 |
|------|------|------|
| `PayNoticeSpi` | `payidNotice(PayOrder)` | 支付成功通知 |
| | `payTimeout(PayOrder)` | 支付超时通知 |
| | `refoundNotice(PayOrder)` | 退款成功通知 |

### Schedule

| 类名 | 方法 | 周期 | 说明 |
|------|------|------|------|
| `WxpayOrderSchedule` | `wxOrderPayStatusSync()` | 5min | 主动查询微信支付中订单的状态并同步 |
| `WxPayTimeoutSchedule` | `wxOrderPayStatusSync()` | 5min | 超时未支付订单自动取消(NEW/PAYING状态) |

### Mapper

| 类名 | 自定义方法 |
|------|----------|
| `PayWxpayConfigMapper` | `getWxpayConfigList(PayWxpayConfigDto)` |
| `PayAlipayConfigMapper` | `getAlipayConfigList(PayAlipayConfigDto)` |
| `PayOrderMapper` | `getActivePayOrder(PayOrder)`, `getOrderStatus(PayOrder)`, `getPayOrderByOutTradeNo(String)`, `getPayingOrders()`, `getTimeoutPayingOrders(PayOrderDto)` |

### Config

| 类名 | 配置项 |
|------|--------|
| `PayConfig` | `pay.wxpay.pay-status-sync.enable` (默认1), `pay.pay-timeout-cancel.enable` (默认1), `pay.pay-timeout-cancel.minute` (默认1440) |

---

## 4. 核心工作流

### 4.1 创建支付订单

```java
// ShopOrderService.createPayOrder() 核心流程
@Transactional(rollbackFor = Exception.class)
public PayOrderDto createPayOrder(PayOrder model, HttpServletRequest req, HttpServletResponse rep) {
    // 1. 参数校验(金额合法性、支付方式、终端类型、下单人一致性)
    paramCheck(model);
    PayMethod payMethod = PayMethod.valueOf(model.getPayMethod());

    // 2. 查询同一orderNo的历史支付单
    List<PayOrder> oldPayOrders = payOrderService.getActivePayOrder(lastOrder);

    // 3. 若存在旧单且更换支付方式 → 取消旧单(微信/支付宝关单)
    // 4. 若存在旧单且未更换支付方式 → 保留旧单
    // 5. 生成 outTradeNo (支持重试序号: orderNo-1, orderNo-2)
    // 6. 设置状态为 PAYING，插入/更新订单（含 points / refundedAmount 字段）

    // 6.5 积分消费（outTradeNo 生成且 payOrder 持久化后、调用支付 helper 前）
    //     若 model.getPoints() > 0，调 consumeService.consume（orderNo=outTradeNo，幂等键 CONSUME:outTradeNo）
    if (model.getPoints() != null && model.getPoints() > 0) {
        PointsConsumeReq consumeReq = new PointsConsumeReq();
        consumeReq.setTenantCode(model.getTenantCode());
        consumeReq.setUserCode(model.getUserCode());
        consumeReq.setPoints(model.getPoints());
        consumeReq.setOrderNo(model.getOutTradeNo());  // 用 outTradeNo 作为积分消费 orderNo
        consumeReq.setReason("订单支付");
        consumeService.consume(consumeReq);  // 冻结积分（FROZEN）→ 异步扣减
    }

    // 7. 按支付方式调用对应Helper（try-catch 包裹，失败时调 releaseConsume 补偿后向上抛出）
    try {
        if (PayMethod.ALI_PAY == payMethod) {
            return alipayHelper.pay(model, req, rep);  // 返回含aliPayBody的DTO
        }
        if (PayMethod.WX_PAY == payMethod) {
            return wxpayHelper.pay(model, req, rep);   // 返回含jsapiResult的DTO
        }
        if (PayMethod.MOCK_PAY == payMethod) {
            return PayOrderDto.copy(model);             // 模拟支付直接返回
        }
    } catch (Exception e) {
        // 支付失败补偿：释放已扣减的积分（幂等键 CANCEL:outTradeNo）
        //   FROZEN → 释放冻结 + 置 CANCELLED；DEDUCTED → 调 refundWithoutLock 退剩余全部
        if (model.getPoints() != null && model.getPoints() > 0) {
            try {
                consumeService.releaseConsume(model.getOutTradeNo(), "支付失败");
            } catch (Exception ex) {
                log.error("支付失败释放积分异常, outTradeNo={}", model.getOutTradeNo(), ex);
            }
        }
        throw e;  // 向上抛出原异常
    }
}
```

**积分消费与补偿要点**：
- 积分消费在 outTradeNo 生成且 payOrder 持久化**之后**、支付 helper 调用**之前**触发，`orderNo=outTradeNo`（与消费幂等键 `CONSUME:outTradeNo` 对齐）
- 支付 helper 失败时调 `releaseConsume(outTradeNo, "支付失败")` 补偿积分，**补偿异常不吞掉原异常**（先记录日志，再向上抛出原支付异常）
- `mockPayWithOrderInfo` 在 `createPayOrder` 后的 mock 回调步骤外层加 try-catch，失败时同样调 `releaseConsume(outTradeNo, "模拟支付回调失败")`

> 注意：`createPayOrder` 与 `mockPayWithOrderInfo` 的支付失败补偿 `releaseConsume` **不受 `pay.points-refund-on-refund.enable` 开关控制**，无论开关状态如何都强制退还积分。

### 4.2 微信支付回调

```java
// WxpayNotifyRest.publicWxpayNotifyTenant() 流程
@PostMapping("/public/wxpay/notify/{tenantCode}/{appid}")
@Transactional(rollbackFor = Exception.class)
public String publicWxpayNotifyTenant(HttpServletRequest req, ...) {
    // 1. 解析微信通知签名头
    SignatureHeader requestHeader = WxpayHelper.getRequestHeader(req);
    // 2. 获取WxPayService并解析通知
    WxPayNotifyV3Result notifyResult = wxPayService.parseOrderNotifyV3Result(jsonData, requestHeader);
    // 3. 校验outTradeNo和tradeState
    // 4. 查询本地订单
    PayOrder payOrder = payOrderService.getPayOrderByOutTradeNo(outTradeNo);
    // 5. 调用Helper处理回调(金额校验、状态更新、SPI通知)
    payOrder = wxpayHelper.payNotify(payOrder, result);
    // 6. 更新订单
    payOrderService.update(payOrder);
    return WxPayNotifyResponse.success("success");
}
```

### 4.3 支付宝支付回调

```java
// AlipayNotifyRest.alipayNotify() 流程
@PostMapping("/public/alipay/notify/{tenantCode}/{appid}")
@Transactional(rollbackFor = Exception.class)
public void alipayNotify(HttpServletRequest req, HttpServletResponse rep, ...) {
    // 1. 获取请求参数
    Map<String, String> params = RequestHelper.getParamsFromRequest(req);
    // 2. RSA验签
    boolean b = alipayHelper.signVerifie(rep, params, tenantCode);
    // 3. 解析AlipayNotify
    AlipayNotify notify = JSONObject.parseObject(paramsString, AlipayNotify.class);
    // 4. 查询本地订单
    PayOrder payOrder = payOrderService.getPayOrderByOutTradeNo(outTradeNo);
    // 5. 处理回调(TRADE_FINISHED→FINISHED, TRADE_SUCCESS→PAID)
    payOrder = alipayHelper.payNotify(req, rep, payOrder, notify);
    // 6. 更新订单 + SPI通知
    payOrderService.update(payOrder);
    if (payNoticeSpi != null) { payNoticeSpi.payidNotice(payOrder); }
}
```

### 4.4 退款流程

```java
// ShopOrderService.managerPayRefund() 核心流程
// 签名：managerPayRefund(PayOrderRefundReq req)
// req.subOrderNo 为空 → 总单退款；非空 → 子单退款
public String managerPayRefund(PayOrderRefundReq req) {
    // 1. 校验订单存在且状态为 PAID/FINISHED
    // 2. 防御性初始化 refundedAmount / points（null → 0）
    // 3. refundNo 自动生成（子单退款场景，refundNo 为空时自动生成 outTradeNo-R+timestamp）

    String subOrderNo = req.getSubOrderNo();

    if (StringUtils.isBlank(subOrderNo)) {
        // ===== 总单退款分支（subOrderNo 为空） =====
        // 1. 积分全单回退：points 取自 payOrder.getPoints()
        //    调 consumeService.releaseConsume(outTradeNo, reason)
        //    （处理 FROZEN 释放 + DEDUCTED 退回，不重复调 pointsRefundService.refund 避免重复退款）
        //    幂等键 CANCEL:outTradeNo（releaseConsume 内部处理）
        if (payOrder.getPoints() > 0) {
            consumeService.releaseConsume(payOrder.getOutTradeNo(), reasonStr);
        }
        // 2. 支付通道退款（refundAmount=null 全单）：
        //    MOCK_PAY → 状态置 REFUNDED, refundedAmount = paymentAmount
        //    WX_PAY   → wxpayHelper.wxTradeRefund(payOrder, null, refundNo, reason)
        //               refundedAmount = paymentAmount
        //               (V3 接口，退款结果通过回调异步通知)
        return result;
    }

    // ===== 子单退款分支（subOrderNo 非空） =====
    // 1. 通过 SPI 获取子单信息，points 取自 payOrderSpi.getOrderInfoForPay(subOrderNo).getPoints()
    //    （积分计算由订单模块拆单过程完成，micro-pay 只按 points 退还）
    OrderInfoForPay subOrderInfo = payOrderSpi.getOrderInfoForPay(subOrderNo);
    long refundPoints = subOrderInfo.getPoints() == null ? 0L : subOrderInfo.getPoints();

    // 2. 校验退款金额不超额（refundedAmount + refundAmount <= paymentAmount）
    // 3. 若 refundPoints > 0 调 pointsRefundService.refund
    //    （orderNo=outTradeNo, refundNo=本次退款单号，幂等键 REFUND:refundNo）
    if (refundPoints > 0) {
        PointsRefundReq refundReq = new PointsRefundReq();
        refundReq.setOrderNo(payOrder.getOutTradeNo());  // 关键：用 outTradeNo，与消费时一致
        refundReq.setRefundNo(refundNo);
        refundReq.setPoints(refundPoints);
        pointsRefundService.refund(refundReq);
    }

    // 4. 支付通道退款（refundAmount 非空 部分退款）：
    //    MOCK_PAY → 状态置 REFUNDED, refundedAmount += refundAmount
    //    WX_PAY   → wxpayHelper.wxTradeRefund(payOrder, refundAmount, refundNo, reason)
    //               refundedAmount += refundAmount
    //               (V3 接口，outRefundNo 使用 refundNo)
    return result;
}
```

**总单退款 vs 子单退款积分处理对比**：

| 场景 | subOrderNo | points 来源 | 积分处理 | 幂等键 | 支付通道调用 |
|------|------------|-------------|----------|--------|--------------|
| 总单退款 | 空（`null`/空串） | `payOrder.getPoints()` | `releaseConsume(outTradeNo)`（FROZEN 释放 + DEDUCTED 退回，**不**重复调 `pointsRefundService.refund`） | `CANCEL:outTradeNo` | `wxTradeRefund(payOrder, null, refundNo, reason)` 全单 |
| 子单退款 | 非空 | `payOrderSpi.getOrderInfoForPay(subOrderNo).getPoints()` | 调 `pointsRefundService.refund`（按子单 points 退还） | `REFUND:refundNo` | `wxTradeRefund(payOrder, refundAmount, refundNo, reason)` 部分 |

**退款积分退还开关**：

`pay.points-refund-on-refund.enable`（默认 `1`）控制退款时是否退还积分：

- **启用（=1，默认）**：按上述行为退还积分
- **关闭（=0）**：跳过 `releaseConsume` / `pointsRefundService.refund` 调用，仅退现金并更新 `refundedAmount`；跳过时打印 log.info

> 开关**只控制退款环节**。`createPayOrder` 与 `mockPayWithOrderInfo` 的支付失败补偿 `releaseConsume` **不受开关控制**，强制退还。

**关键设计**：
- **总单退款不调 `pointsRefundService.refund`**：`releaseConsume` 已统一处理 FROZEN（释放冻结）与 DEDUCTED（调 `refundWithoutLock` 退剩余全部）两种状态，重复调 refund 会导致重复退款
- **积分计算由订单模块拆单过程完成**：micro-pay 只按 `points` 退还，不参与积分比例计算；总单退款取 `payOrder.getPoints()`，子单退款取 `payOrderSpi.getOrderInfoForPay(subOrderNo).getPoints()`
- **`orderNo` 始终用 `outTradeNo`**：与消费时一致（消费幂等键 `CONSUME:outTradeNo`），refund 的 `orderNo` 也用 `outTradeNo` 查找原消费记录、计算超额防护
- **`refundNo` 自动生成**：子单退款时为空则自动生成 `outTradeNo-R+timestamp`，作为 `REFUND:refundNo` 幂等键，支持同一订单多次子单退款
- **`PayorderRefundRest` 传递 subOrderNo / refundAmount / refundNo / reason** 到 `managerPayRefund`，由前端控制总单退款（不传 subOrderNo）或子单退款（传 subOrderNo）

### 4.5 微信退款回调

```java
// WxpayHelper.wxRefundNotify() 流程
public String wxRefundNotify(WxPayRefundNotifyV3Result refundNotifyResult) {
    // 1. 判断退款状态 SUCCESS/FAIL
    // 2. SUCCESS → 查询订单 → 更新状态为 REFUNDED → SPI通知
    // 3. FAIL → 返回失败
}
```

### 4.6 定时任务

```java
// WxpayOrderSchedule — 微信支付状态同步(5分钟)
// 查询所有 PAYING 状态的订单 → 主动向微信查询支付结果 → 同步更新

// WxPayTimeoutSchedule — 超时自动取消(5分钟)
// 查询超过配置时间(默认1440分钟)的 NEW/PAYING 订单 → 状态改为 TIMEOUT_CANCEL → SPI通知
```

---

## 5. 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `pay.wxpay.pay-status-sync.enable` | `1` | 是否启用微信支付状态同步定时任务(1=启用) |
| `pay.pay-timeout-cancel.enable` | `1` | 是否启用超时自动取消定时任务(1=启用) |
| `pay.pay-timeout-cancel.minute` | `1440` | 超时取消阈值(分钟)，最小10，最大1440 |
| `pay.points-refund-on-refund.enable` | `1` | 退款时是否退还积分（1=启用，0=关闭，默认 1）；仅控制退款环节 |

---

## 6. 依赖

### 框架依赖

| 模块 | 用途 |
|------|------|
| `sh-mybatis` | BaseMapper, BaseService, PageQuery |
| `sh-redis` | RedisIdGenerator(支付流水号生成) |
| `iam-contract-api` | PrincipalContext(获取tenantCode/userCode) |

### 模块间依赖（硬依赖）

| 模块 | 用途 |
|------|------|
| `micro-points` | 积分支付集成（pom.xml 已引入依赖）。下单时调 `PointsConsumeService.consume` 冻结积分；支付失败调 `releaseConsume` 补偿；退款时调 `releaseConsume`（总单退款）或 `PointsRefundService.refund`（子单退款，积分通过 `PayOrderSpi.getOrderInfoForPay(subOrderNo)` 获取子单 points） |

### 第三方依赖

| 依赖 | 用途 |
|------|------|
| `alipay-sdk-java` | 支付宝支付SDK(AlipayClient, AlipaySignature) |
| `weixin-java-pay` | 微信支付SDK(WxPayService, V3接口) |

### SPI 扩展

`PayNoticeSpi` 为可选注入(`@Autowired(required = false)`)，业务系统实现此接口即可接收：
- `payidNotice(PayOrder)` — 支付成功通知
- `payTimeout(PayOrder)` — 支付超时通知
- `refoundNotice(PayOrder)` — 退款成功通知

---

## 7. 常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| 微信支付回调未收到 | notifyUrl 配置错误或网络不通 | 检查 notifyUrl 是否包含 `{tenantCode}/{appid}` 占位符并正确替换 |
| 支付宝验签失败 | alipayPublicKey 配置错误或 charset/signType 不匹配 | 检查 PayAlipayConfig 的 alipayPublicKey、charset(默认UTF-8)、signType(默认RSA2) |
| 支付金额不一致异常 | 回调金额与订单金额不匹配(元→分转换) | 确认 paymentAmount 精度，回调时乘以100后与整数比较 |
| 配置更新后不生效 | 本地缓存未刷新 | 配置更新/删除时已调用 `clearCache()`，Redis Pub/Sub 12s 内同步 |
| 重复支付 | 同一 outTradeNo 重复回调 | payNotify() 中已判断 PAID/FINISHED 状态直接返回 null |
| 模拟支付在生产环境可用 | 未做环境校验 | ShopOrderService.paramCheck() 已禁止 PROD 环境使用 MOCK_PAY |
| 订单一直 PAYING | 微信回调丢失 | WxpayOrderSchedule 每5分钟主动同步微信支付状态 |
| 退款后状态未更新 | 微信退款为异步处理 | 退款结果通过 WxpayNotifyRest 的退款回调接口更新 |
| 微信域名验证失败 | verifySign 未配置 | 在 PayWxpayConfig 中配置 verifySign，通过 `/wxpay/config/verify/MP_verify_{verifySign}.txt` 验证 |
| 支付宝沙箱环境 | isProd=0 时使用沙箱网关 | AlipayClientCache.init() 根据 isProd 选择 `openapi-sandbox.dl.alipaydev.com` 或 `openapi.alipay.com` |
