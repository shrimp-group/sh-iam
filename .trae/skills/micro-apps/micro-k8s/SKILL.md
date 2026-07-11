---
name: "micro-k8s"
description: "K8s集群管理模块。管理多集群kubeConfig配置、通过Kind策略模式查询/创建/更新/删除K8s资源。修改com.wkclz.micro.k8s包下代码时触发。"
---

# Micro-K8s 模块

## 1. 适用场景

当需要完成以下任务时触发此 Skill：

- 管理多 K8s 集群配置（kubeConfig CRUD）
- 查询集群 Node / Namespace 信息
- 通过 Kind 策略模式对 K8s 资源执行 list / yaml / create / update / delete 操作
- 新增 Kind 资源类型实现（添加 `K8sApi` 实现类）
- 修改 `com.wkclz.micro.k8s` 包下任何代码
- 排查 ApiClient 缓存、SSL 证书、kubeConfig 解析等问题

---

## 2. 架构概览

```
┌──────────────────────────────────────────────────────────────────┐
│                         REST 层                                  │
│  K8sConfigRest ── K8sRest ── K8sKindRest                        │
│  (配置CRUD)     (集群查询)   (Kind资源CRUD)                       │
└────────┬───────────────┬───────────────┬────────────────────────┘
         │               │               │
         ▼               ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────────────────────┐
│K8sConfigService│ │KubeConfigHelper│ │K8sHelper.getImplByKind(kind)│
│ (配置业务逻辑) │ │(ApiClient缓存) │ │  → 按kind路由到对应Impl      │
└──────┬───────┘ └──────┬───────┘ └──────────────┬───────────────┘
       │                │                        │
       ▼                ▼                        ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────────────────────┐
│K8sConfigMapper│ │ApiClient缓存 │ │ K8sApi 接口 (策略模式)        │
│ (MyBatis)    │ │(10min+DCL)  │ │  list/yaml/create/update/delete│
└──────────────┘ └──────────────┘ └──────────────┬───────────────┘
                                                  │
                    ┌─────────────────────────────┼─────────────────────────────┐
                    │                             │                             │
                    ▼                             ▼                             ▼
          ┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
          │ CoreV1Api 系    │         │ AppsV1Api 系    │         │ 其他 API 系      │
          │ Pod/Service/    │         │ Deployment/     │         │ Batch/Networking │
          │ ConfigMap/Secret│         │ StatefulSet/    │         │ Rbac/Policy/     │
          │ Namespace/...   │         │ DaemonSet/...   │         │ Apiextensions    │
          └─────────────────┘         └─────────────────┘         └─────────────────┘
```

---

## 3. 核心组件速查

### 3.1 自动配置 & 路由

| 类 | 说明 |
|---|---|
| `K8sAutoConfig` | `@ComponentScan("com.wkclz.micro.k8s")` + `@MapperScan("com.wkclz.micro.k8s.mapper")` |
| `Route` | 路由常量，前缀 `/micro-k8s`，定义 Config / Cluster / Kind 三组路由 |

### 3.2 Bean 层

| 类 | 说明 |
|---|---|
| `K8sConfig` | 实体，extends BaseEntity，字段：`clusterName`(集群名称)、`kubeConfig`(kubeConfig YAML) |
| `K8sConfigDto` | DTO，extends K8sConfig，提供 `copy(K8sConfig)` 静态方法 |
| `K8sParam` | K8s 操作参数，字段：`clusterName`、`namespace`、`kind`、`name`、`yaml` |
| `Kind` | 枚举，31 种 K8s 资源类型（Pod/Deployment/Service/Namespace/...），每种含中文描述 |

### 3.3 核心接口 & Helper

| 类 | 说明 |
|---|---|
| `K8sApi` | 策略接口，5 个方法：`list(K8sParam)`、`yaml(K8sParam)`、`create(K8sParam)`、`update(K8sParam)`、`delete(K8sParam)` |
| `K8sHelper` | 静态工具，`getImplByKind(String kind)` → 按 `"k8s" + kind + "Impl"` 从 SpringContextHolder 获取 Bean |
| `KubeConfigHelper` | ApiClient 工厂 + 缓存，10 分钟缓存 + DCL 双重检查锁，支持 Token / 客户端证书 / CA 三种认证模式 |
| `YamlUtil` | YAML 解析工具，`yamlToK8sObject(String yaml)` / `yamlToK8sObject(String yaml, Class<T> clazz)` |

### 3.4 Mapper & Service

| 类 | 说明 |
|---|---|
| `K8sConfigMapper` | extends BaseMapper\<K8sConfig\>，自定义方法：`getClusterList(K8sConfig)`、`getClusterOptions()` |
| `K8sConfigService` | extends BaseService\<K8sConfig, K8sConfigMapper\>，方法：`getClusterPage`、`getClusterOptions`、`create`、`update`、`save`、`remove`，含 clusterName 唯一性校验 |

### 3.5 REST 控制器

| 类 | 路由 | 说明 |
|---|---|---|
| `K8sConfigRest` | `/micro-k8s/config/*` | 集群配置 CRUD：page/info/create/update/remove/options |
| `K8sRest` | `/micro-k8s/cluster/*` | 集群查询：nodes / namespaces / namespaces/briefly |
| `K8sKindRest` | `/micro-k8s/cluster/kind/*` | Kind 资源 CRUD：list/yaml/create/update/delete |

### 3.6 Kind 实现类（策略模式）

**Bean 命名规则**：`"k8s" + Kind枚举名 + "Impl"`，如 `@Service("k8sDeploymentImpl")`

| 实现类 | K8s API 组 | 是否需要 namespace |
|---|---|---|
| `K8sPodImpl` | CoreV1Api | 是（支持按 app label 过滤） |
| `K8sDeploymentImpl` | AppsV1Api | 是 |
| `K8sServiceImpl` | CoreV1Api | 是 |
| `K8sNamespaceImpl` | CoreV1Api | **否**（集群级资源） |
| `K8sConfigMapImpl` | CoreV1Api | 是 |
| `K8sSecretImpl` | CoreV1Api | 是 |
| `K8sIngressImpl` | NetworkingV1Api | 是 |
| `K8sDaemonSetImpl` | AppsV1Api | 是 |
| `K8sReplicaSetImpl` | AppsV1Api | 是 |
| `K8sStatefulSetImpl` | AppsV1Api | 是 |
| `K8sCronJobImpl` | BatchV1Api | 是 |
| `K8sJobImpl` | BatchV1Api | 是 |
| `K8sServiceAccountImpl` | CoreV1Api | 是 |
| `K8sPersistentVolumeClaimImpl` | CoreV1Api | 是 |
| `K8sNetworkPolicyImpl` | NetworkingV1Api | 是 |
| `K8sCustomResourceDefinitionImpl` | ApiextensionsV1Api | **否**（集群级资源） |
| `K8sClusterRoleImpl` | RbacAuthorizationV1Api | **否**（集群级资源） |
| `K8sClusterRoleBindingImpl` | RbacAuthorizationV1Api | **否**（集群级资源） |
| `K8sRoleImpl` | RbacAuthorizationV1Api | 是 |
| `K8sRoleBindingImpl` | RbacAuthorizationV1Api | 是 |
| `K8sEndpointsImpl` | CoreV1Api | 是 |
| `K8sEventImpl` | CoreV1Api | 是 |
| `K8sPodDisruptionBudgetImpl` | PolicyV1Api | 是 |
| `K8sResourceQuotaImpl` | CoreV1Api | 是 |
| `K8sStorageClassImpl` | — | TODO 暂不支持 |
| `K8sPersistentVolumeImpl` | — | TODO 暂不支持 |
| `K8sHorizontalPodAutoscalerImpl` | — | TODO 暂不支持 |
| `K8sPodSecurityPolicyImpl` | — | TODO 暂不支持 |
| `K8sPriorityClassImpl` | — | TODO 暂不支持 |
| `K8sVolumeImpl` | — | TODO 暂不支持 |
| `K8sVolumeSnapshotImpl` | — | TODO 暂不支持 |
| `K8sServiceMonitorImpl` | — | TODO 暂不支持 |

**无需 namespace 的 Kind**（在 `K8sKindRest.NO_NAMESPACE_KIND` 中定义）：`ClusterRoleBinding`、`ClusterRole`、`CustomResourceDefinition`、`Namespace`

---

## 4. 核心工作流

### 4.1 集群配置管理

```java
// 新增集群配置
K8sConfig config = new K8sConfig();
config.setClusterName("prod-cluster");
config.setKubeConfig("apiVersion: v1\nclusters:\n  ..."); // kubeConfig YAML
k8sConfigService.create(config); // 含 clusterName 唯一性校验

// 查询集群选项列表
List<String> options = k8sConfigService.getClusterOptions();
```

### 4.2 查询集群 Node / Namespace

```java
// 获取集群节点
K8sParam param = new K8sParam();
param.setClusterName("prod-cluster");
CoreV1Api api = kubeConfigHelper.getCoreV1Api(param.getClusterName());
V1NodeList nodeList = api.listNode().execute();

// 获取 Namespace 列表（简要）
V1NamespaceList nsList = api.listNamespace().execute();
List<String> nsNames = nsList.getItems().stream()
    .map(item -> item.getMetadata().getName()).toList();
```

### 4.3 Kind 资源 CRUD（策略模式）

```java
// 通过 K8sHelper 获取对应 Kind 的实现
K8sApi api = K8sHelper.getImplByKind("Deployment");

// 列表查询（namespace 为空则查全命名空间）
K8sParam param = new K8sParam();
param.setClusterName("prod-cluster");
param.setNamespace("default");
param.setKind("Deployment");
KubernetesListObject list = api.list(param);

// 获取 YAML
param.setName("my-app");
String yaml = api.yaml(param);

// 创建资源
param.setYaml(deploymentYaml);
String result = api.create(param);

// 更新资源（从 yaml 中解析 name/namespace）
param.setYaml(updatedYaml);
String updated = api.update(param);

// 删除资源
param.setName("my-app");
String deleted = api.delete(param);
```

### 4.4 新增 Kind 实现类

```java
@Service("k8sXxxImpl") // 命名必须为 "k8s" + Kind枚举名 + "Impl"
public class K8sXxxImpl implements K8sApi {

    @Autowired
    private KubeConfigHelper kubeConfigHelper;

    public XxxV1Api getApi(K8sParam param) {
        return kubeConfigHelper.getXxxV1Api(param.getClusterName());
    }

    @Override
    public KubernetesListObject list(K8sParam param) throws ApiException {
        XxxV1Api api = getApi(param);
        return StringUtils.isBlank(param.getNamespace()) ?
            api.listXxxForAllNamespaces().execute()
            : api.listNamespacedXxx(param.getNamespace()).execute();
    }

    @Override
    public String yaml(K8sParam param) throws ApiException { /* ... */ }

    @Override
    public String create(K8sParam param) throws ApiException { /* ... */ }

    @Override
    public String update(K8sParam param) throws ApiException, IOException { /* ... */ }

    @Override
    public String delete(K8sParam param) throws ApiException { /* ... */ }
}
```

同时需在 `Kind` 枚举中添加对应条目，并在 `K8sKindRest.NO_NAMESPACE_KIND` 中添加（如果是集群级资源）。

---

## 5. 配置项

| 配置 | 说明 | 默认值 |
|---|---|---|
| `KubeConfigHelper.CACHE_TIME` | ApiClient 缓存时间 | 10 分钟 (`10 * 60 * 1000L`) |
| kubeConfig 临时文件路径 | `{user.dir}/tmp/kube/{clusterName}` | 运行时生成 |
| `K8sConfigMapper` SQL | 查询时 `deleted = 0` 过滤，clusterName 模糊匹配 | — |

**K8sConfig 数据库表** (`k8s_config`)：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | bigint | 主键 |
| `cluster_name` | varchar | 集群名称（唯一） |
| `kube_config` | text | kubeConfig YAML 内容 |
| `sort` | int | 排序 |
| `create_time` / `update_time` | datetime | 时间戳 |
| `create_by` / `update_by` | varchar(31) | 操作人 |
| `remark` | varchar(255) | 备注 |
| `version` | int | 乐观锁 |
| `deleted` | varchar(24) | 逻辑删除 |

---

## 6. 依赖

### Maven 依赖

| groupId | artifactId | 说明 |
|---|---|---|
| `io.kubernetes` | `client-java` | Kubernetes Java 客户端（版本由 sh-bom 管理） |
| `com.wkclz.framework` | `sh-mybatis` | MyBatis 基础框架（BaseMapper/BaseService） |

> 此模块不依赖 IAM 契约层。

### 框架模块依赖

| 模块 | 使用点 |
|---|---|
| `sh-core` | BaseEntity、R、ValidationException、UserException、ResultCode、PageData |
| `sh-mybatis` | BaseMapper、BaseService、PageQuery |
| `sh-spring` | SpringContextHolder（K8sHelper 获取 Bean） |

### K8s API 组映射

| KubeConfigHelper 方法 | K8s API 类 | 使用的 Kind 实现 |
|---|---|---|
| `getCoreV1Api()` | CoreV1Api | Pod, Service, Namespace, ConfigMap, Secret, ServiceAccount, PVC, Endpoints, Event, ResourceQuota |
| `getAppsV1Api()` | AppsV1Api | Deployment, DaemonSet, ReplicaSet, StatefulSet |
| `getBatchV1Api()` | BatchV1Api | CronJob, Job |
| `getNetworkingV1Api()` | NetworkingV1Api | Ingress, NetworkPolicy |
| `getRbacAuthorizationV1Api()` | RbacAuthorizationV1Api | ClusterRole, ClusterRoleBinding, Role, RoleBinding |
| `getPolicyV1Api()` | PolicyV1Api | PodDisruptionBudget |
| `getApiextensionsV1Api()` | ApiextensionsV1Api | CustomResourceDefinition |
| `getStorageV1Api()` | StorageV1Api | （StorageClass 待实现） |

---

## 7. 常见问题

| 问题 | 原因 | 解决 |
|---|---|---|
| `kind不能为空` / `不支持的 k8s Kind 类型` | 传入的 kind 为空或不在 Kind 枚举中 | 确认 kind 值与 Kind 枚举名完全一致（区分大小写） |
| `当前客户端不支持的 kind: Xxx` | Kind 枚举有值但无对应 Spring Bean | 检查 `@Service("k8sXxxImpl")` 是否注册，或该 Kind 为 TODO 暂不支持 |
| `clusterName 不能为空` | K8sParam 未传 clusterName | 所有 K8s 操作必须指定集群名称 |
| `namespace 不能为空` | 命名空间级资源未传 namespace | 仅为 ClusterRoleBinding/ClusterRole/CRD/Namespace 四种可省略 |
| ApiClient 过期 / 连接失败 | 缓存超过 10 分钟自动刷新 | 检查 kubeConfig 是否有效，KubeConfigHelper 会 DCL 重建 |
| SSL 证书验证失败 | kubeConfig 中证书格式不正确 | KubeConfigHelper 支持 Base64/PEM 格式，自动尝试 RSA/EC/PKCS1 私钥解析 |
| `创建集群 ApiClient 失败` | kubeConfig YAML 解析异常 | 检查 kubeConfig 内容完整性，确保包含 server/证书/token 信息 |
| `yaml 解析异常` | 传入 YAML 格式不合法或类型不匹配 | YamlUtil 使用 `io.kubernetes.client.util.Yaml.load()` 解析，确保 YAML 格式正确 |
| 新增 Kind 后不生效 | Bean 命名不符合规则 | 必须为 `"k8s" + Kind枚举名 + "Impl"`，且 Kind 枚举中有对应条目 |
| `{} 不存在，请先完成配置` | 数据库中无该集群的 kubeConfig | 先通过 `/micro-k8s/config/create` 添加集群配置 |
