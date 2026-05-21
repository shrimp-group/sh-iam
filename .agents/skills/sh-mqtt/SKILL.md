---
name: "sh-mqtt"
description: "sh-framework MQTT消息模块知识库。基于Eclipse Paho MQTT v3，提供注解驱动消息处理(@MqttController+@MqttTopicMapping)、MqttProducer发布(即时/延时/批量)、MqttSubscribe订阅分发、SSL/TLS单向认证、断线自动重连重订阅、远程调用基础设施(MqttMessage/MqttResponse)。当涉及MQTT消息收发、IoT设备通信、MQTT配置时调用。"
---

# sh-mqtt 模块知识库

sh-mqtt 是 sh-framework 的 MQTT 消息模块，基于 Eclipse Paho MQTT v3 客户端，提供注解驱动的消息处理、消息发布、自动订阅、SSL/TLS 支持和断线重连等能力。

## 包结构

```
com.wkclz.mqtt
├── MqttAutoConfigure              # 自动配置
├── annotation/
│   ├── MqttController             # MQTT控制器注解（@Component，指定父Topic）
│   └── MqttTopicMapping           # Topic映射注解（指定子Topic）
├── bean/
│   └── MqttHexMsg                 # MQTT消息封装Bean
├── client/
│   └── MqttProducer               # 消息发布者（@Component）
├── config/
│   ├── MqttConfig                 # MQTT核心配置+MqttAsyncClient Bean
│   ├── MqttApplicationListener    # 容器就绪后订阅所有Topic
│   ├── MqttBeanPostProcessor      # 扫描@MqttController注册Processor
│   └── MqttSubscribe              # 消息订阅与分发
├── enums/
│   └── Qos                        # QoS枚举（QOS_0/QOS_1/QOS_2）
├── exception/
│   ├── MqttBeansException         # Bean相关异常（Topic重复定义）
│   ├── MqttRemoteException        # 远程调用异常
│   ├── MqttSendException          # 发送异常
│   └── MqttTimeoutException       # 超时异常（MqttRemoteException子类）
├── handler/
│   └── MqttHandlerFactory         # 处理器注册中心（ConcurrentHashMap）
├── remote/
│   ├── MqttMessage                # 远程调用请求（syncFlag, mId, replyTopic, data）
│   └── MqttResponse               # 远程调用响应（OK/TIMEOUT/CANCEL, mId, messageResult）
└── demo/
    ├── MqttConsumerDemo           # 消费者示例
    └── MqttProducerDemo           # 生产者示例（定时心跳）
```

## 注解驱动消息处理

### @MqttController + @MqttTopicMapping

```java
@MqttController("keepalive")        // 父Topic = keepalive（自动注册为@Component）
public class MqttConsumerDemo {
    
    @MqttTopicMapping("breath")      // 完整Topic = keepalive/breath
    public void breath(MqttHexMsg msg) {
        String data = new String(msg.getPayload(), StandardCharsets.UTF_8);
        logger.info("breath message: {}", data);
    }
    
    @MqttTopicMapping                 // 子Topic为空，订阅 keepalive/#
    public void handleAll(MqttHexMsg msg) {
        // 处理keepalive下所有子Topic
    }
}
```

### 消息处理流程

1. **Bean后置处理**：`MqttBeanPostProcessor` 扫描 `@MqttController` Bean，将 `@MqttTopicMapping` 方法注册到 `MqttHandlerFactory`
2. **订阅**：`MqttApplicationListener` 在容器就绪后调用 `MqttSubscribe.subscribeTopics()`，按父Topic以 `parentTopic/#` 通配订阅
3. **消息到达**：`MqttSubscribe.messageArrivedHandle()` 从 `MqttHandlerFactory` 查找处理器，构建 `MqttHexMsg`，通过反射调用

### Topic解析策略

优先按完整Topic查找Handler；找不到时截取第一个`/`前部分拼接`/#`通配查找。

## MqttProducer — 消息发布

```java
@Autowired(required = false)
private MqttProducer mqttProducer;

// 发送对象消息（自动JSON序列化，默认QOS_1）
mqttProducer.send("keepalive/breath", map);

// 发送对象，指定QoS
mqttProducer.send("keepalive/breath", map, Qos.QOS_0);

// 发送原始字节
mqttProducer.send("device/cmd", bytes, Qos.QOS_2);

// 延迟发送（默认500ms延时）
mqttProducer.sendDelay("keepalive/breath", "hello");

// 延迟发送，指定延时
mqttProducer.sendDelay("keepalive/breath", "hello", 1000, Qos.QOS_1);

// 延迟批量发送（每条消息间隔递增）
mqttProducer.sendDelay("device/cmd", messageList, 200, Qos.QOS_1);
```

**特性**：
- 共享ScheduledExecutorService（核心2线程，前缀mqtt-delay-）
- 延时发送通过 `schedule()` 实现，批量消息间隔递增
- `mqttAsyncClient` 为null（MQTT未启用）时抛出 `MqttBeansException`

## MqttHexMsg — 消息封装

| 字段 | 类型 | 说明 |
|------|------|------|
| topic | String | 完整Topic |
| parentTopic | String | 父Topic |
| subTopic | String | 子Topic |
| id | Integer | MQTT消息ID |
| qos | Integer | QoS等级 |
| payload | byte[] | 消息载荷（原始字节） |

## Qos 枚举

| 枚举 | 值 | 说明 |
|------|---|------|
| QOS_0 | 0 | 最多一次 |
| QOS_1 | 1 | 至少一次 |
| QOS_2 | 2 | 恰好一次 |

## MQTT配置

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| shrimp.cloud.mqtt.enabled | true | 是否启用MQTT |
| shrimp.cloud.mqtt.username | 空 | 用户名 |
| shrimp.cloud.mqtt.password | 空 | 密码 |
| shrimp.cloud.mqtt.ca-path | 空 | CA证书classpath路径 |
| shrimp.cloud.mqtt.end-point | 空 | Broker地址（必填） |
| shrimp.cloud.mqtt.client-id-prefix | 空 | 客户端ID前缀（默认server） |
| shrimp.cloud.mqtt.keep-alive-interval | 60 | 心跳间隔（秒） |
| shrimp.cloud.mqtt.keep-alive-task | 0 | 保活任务（1=启动） |
| shrimp.cloud.mqtt.instance-id | 空 | 阿里云实例ID |
| shrimp.cloud.mqtt.access-key | 空 | 阿里云AccessKey |
| shrimp.cloud.mqtt.secret-key | 空 | 阿里云SecretKey |

### SSL/TLS配置

endPoint以`ssl`开头且caPath非空时，自动加载classpath下的CA证书，使用BouncyCastle实现SSL单向认证。

### 连接选项

- cleanSession = true（不保留会话）
- connectionTimeout = 30秒
- automaticReconnect = true（自动重连）
- 客户端ID：{clientIdPrefix}@{服务器IP}

### 断线重连

`MqttReconnectCallback` 实现 `MqttCallbackExtended`：
- `connectComplete()` → 重连成功后自动重新订阅所有Topic
- `connectionLost()` → 打印错误日志

## MqttHandlerFactory — 处理器注册中心

| 数据结构 | 说明 |
|---------|------|
| mqttControllers: ConcurrentHashMap<String, Object> | Topic→处理类Bean |
| mqttHandlers: ConcurrentHashMap<String, Method> | Topic→处理方法 |
| parentTopicSet: ConcurrentHashMap.KeySetView | 父Topic集合 |

## 远程调用（预留）

`MqttMessage` 和 `MqttResponse` 定义了请求-响应模型，但**完整同步调用流程尚未实现**：
- MqttMessage：syncFlag, mId(UUID), replyTopic, data
- MqttResponse：OK(20000)/TIMEOUT(40001)/CANCEL(40002), mId, messageResult, errorMessage

## 自动配置

`MqttAutoConfigure`：@AutoConfiguration + @ComponentScan("com.wkclz.mqtt")

引入 sh-mqtt 依赖后，所有标注 @Component 的类自动注册（MqttConfig, MqttProducer, MqttBeanPostProcessor, MqttApplicationListener, MqttConsumerDemo 等）。

## 完整架构流程

```
启动 → MqttAutoConfigure扫描注册
  → MqttConfig创建MqttAsyncClient连接Broker
  → MqttBeanPostProcessor扫描@MqttController注册到MqttHandlerFactory
  → MqttApplicationListener容器就绪后subscribeTopics()
  → MqttSubscribe按parentTopic/#订阅
  → 消息到达 → messageArrivedHandle()
  → MqttHandlerFactory查找Handler
  → 构建MqttHexMsg → 反射调用方法
  → 断线重连 → MqttReconnectCallback → 重新订阅
```
