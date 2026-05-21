# STORY-004 — IP 归属地缓存工具

| 属性 | 值 |
|------|-----|
| Story ID | STORY-004 |
| 所属 Epic | 公共基础模块 |
| 所属模块 | iam-common |
| 优先级 | P1 |
| 状态 | 待开发 |

## 用户故事

**作为** IAM 系统运维人员，**我希望** 系统能自动解析用户登录 IP 的地理位置和运营商信息，**以便** 在登录日志和请求日志中展示 IP 归属地，辅助安全审计。

## 验收标准

1. `IpLocalCacheHelper.offerQueue(remoteAddr)` 将 IP 写入待解析队列
2. `IpLocalCacheHelper.pollQueue()` 从队列取出待解析 IP
3. `IpLocalCacheHelper.getLocation(remoteAddr)` 解析 IP 归属地
4. 缓存命中时直接返回，不重复请求外部 API
5. 局域网 IP 直接标记为"本地局域网"
6. 公网 IP 通过百度开放 API 解析地理位置和运营商
7. 解析结果缓存到本地 ConcurrentHashMap
8. 线程安全：使用 ConcurrentHashMap + ConcurrentLinkedQueue

## 技术实现要点

- 使用 `ConcurrentHashMap<String, IamRequestLog>` 作为 IP 归属地缓存
- 使用 `ConcurrentLinkedQueue<String>` 作为待解析 IP 队列
- 百度 API 地址：`http://opendata.baidu.com/api.php?resource_id=6006&query=`
- 使用 `NetworkUtil.isInnerAddress()` 判断内网 IP
- 地址拆分：百度返回的 location 格式为 "省份 运营商"，通过空格分隔
- 异常处理：API 返回异常时标记为"未知"
- `offerQueue()` 和 `getLocation()` 使用 `synchronized` 保证原子性
- 已有优化版本 `IpLocalCacheHelperOptimized` 和 `IpLocalCacheAdapter`

## 依赖故事

- STORY-002（IamRequestLog 实体定义）

## 涉及文件

| 文件 | 路径 |
|------|------|
| IpLocalCacheHelper | iam-common/src/main/java/com/wkclz/iam/common/helper/IpLocalCacheHelper.java |
| IpLocalCacheHelperOptimized | iam-common/src/main/java/com/wkclz/iam/common/helper/IpLocalCacheHelperOptimized.java |
| IpLocalCacheAdapter | iam-common/src/main/java/com/wkclz/iam/common/helper/IpLocalCacheAdapter.java |
