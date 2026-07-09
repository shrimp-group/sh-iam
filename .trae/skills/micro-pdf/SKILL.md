---
name: "micro-pdf"
description: "当需要操作 micro-pdf 模块时使用此技能 —— 这是一个基于 Thymeleaf + Flying Saucer 的 PDF 模板生成服务。当用户需要实现 PDF 模板 CRUD、模板渲染生成 PDF、Mock 预览、缓存管理，或修改 micro-pdf 包（com.wkclz.micro.pdf）下任何代码时触发。"
---

# Micro-PDF 模块

## 1. 适用场景

- PDF 模板的增删改查管理
- 基于 Thymeleaf 模板引擎 + HTML 渲染 PDF 文件
- 通过 `PdfApi` 对外提供 PDF 生成能力（供其他模块调用）
- PDF 模板 Mock 预览（在线调试模板效果）
- PDF 模板缓存管理（Redis Pub/Sub 多实例同步）
- 修改 `com.wkclz.micro.pdf` 包下任何代码

## 2. 架构概览

```
┌─────────────────────────────────────────────────────────┐
│  PdfTemplateRest (REST 控制器)                           │
│  /micro-pdf/template/{page|info|create|update|remove|mock}│
└──────────────┬──────────────────────────────────────────┘
               │
       ┌───────┴───────┐
       ▼               ▼
┌──────────────┐ ┌──────────────┐
│ MdmPdfTemplate│ │  PdfHelper   │  (静态工具: Thymeleaf渲染、PDF响应)
│   Service    │ │              │
└──────┬───────┘ └──────┬───────┘
       │                │
       ▼                ▼
┌──────────────┐ ┌──────────────┐
│ MdmPdfTemplate│ │  PdfConfig   │  (字体路径配置)
│   Mapper     │ └──────────────┘
└──────┬───────┘
       │
       ▼
┌──────────────┐
│PdfTemplateCache│ (本地缓存 + Redis 同步)
└──────────────┘

┌──────────────┐
│   PdfApi     │  (对外 API，供其他模块调用)
│  writePdf()  │──→ PdfTemplateCache → PdfHelper
│  responsePdf()│──→ PdfHelper.pdfResponse()
└──────────────┘
```

**核心渲染流程**: Thymeleaf 模板 + JSON 数据 → HTML → Flying Saucer (iText5) → PDF 文件

## 3. 核心组件速查

| 类 | 包路径 | 职责 |
|---|--------|------|
| `PdfAutoConfig` | `com.wkclz.micro.pdf` | 自动配置类，@ComponentScan + @MapperScan |
| `PdfApi` | `com.wkclz.micro.pdf.api` | 对外 API，提供 `writePdf()` 和 `responsePdf()` 两个方法供其他模块调用 |
| `PdfTemplateCache` | `com.wkclz.micro.pdf.cache` | 模板本地缓存，12秒轮询 Redis 检测变更，5秒防抖初始化，支持 `clearCache()` 广播刷新 |
| `PdfConfig` | `com.wkclz.micro.pdf.config` | 配置类，读取 `shrimp.pdf.simsun.path` 字体路径 |
| `MdmPdfTemplateMapper` | `com.wkclz.micro.pdf.mapper` | Mapper 接口，继承 BaseMapper，自定义 `getPdfTemplateList()` 和 `get4Cache()` |
| `PdfHelper` | `com.wkclz.micro.pdf.helper` | 核心渲染工具，包含 `getContext()`、`thymeleafRenderer()`、`pdfRenderer()`、`pdfResponse()` |
| `Route` | `com.wkclz.micro.pdf.rest` | 路由常量接口，定义 6 个 API 路径 |
| `PdfTemplateRest` | `com.wkclz.micro.pdf.rest` | REST 控制器，模板 CRUD + Mock 预览 |
| `MdmPdfTemplate` | `com.wkclz.micro.pdf.bean.entity` | 实体类，继承 BaseEntity，字段: templateCode/templateName/templateContext/mockData |
| `MdmPdfTemplateDto` | `com.wkclz.micro.pdf.bean.dto` | DTO 类，继承 MdmPdfTemplate，提供 `copy()` 转换方法 |
| `MdmPdfTemplateService` | `com.wkclz.micro.pdf.service` | 服务类，继承 BaseService，提供分页/新增/修改/删除 + 唯一性校验 |

## 4. 核心工作流

### 4.1 通过 PdfApi 生成 PDF（供其他模块调用）

```java
@Autowired
private PdfApi pdfApi;

// 方式一：生成 PDF 文件路径
String pdfPath = pdfApi.writePdf("CONTRACT_TEMPLATE", "{\"name\":\"张三\",\"amount\":10000}");

// 方式二：直接写入 HttpServletResponse 响应流
pdfApi.responsePdf("CONTRACT_TEMPLATE", "{\"name\":\"张三\",\"amount\":10000}", response);
```

内部流程：
1. `PdfTemplateCache.getPdfTemplate(templateCode)` 从本地缓存获取模板
2. `PdfHelper.getContext(data)` 将 JSON 字符串解析为 Thymeleaf Context
3. `PdfHelper.thymeleafRenderer(templateContext, context)` 渲染 HTML（自动注入 SimSun 字体样式）
4. `pdfHelper.pdfRenderer(htmlContent)` 使用 Flying Saucer 生成 PDF 文件到临时目录
5. `PdfHelper.pdfResponse()` 将 PDF 写入响应流（仅 responsePdf 场景）

### 4.2 模板 CRUD

```java
// 分页查询
GET /micro-pdf/template/page?templateCode=xxx&templateName=xxx

// 详情
GET /micro-pdf/template/info?id=1

// 新增（templateName/templateContext 必填，templateCode 唯一性校验）
POST /micro-pdf/template/create
Body: {"templateCode":"TPL_001","templateName":"合同模板","templateContext":"<html>...</html>","mockData":"{\"key\":\"value\"}"}

// 修改（id/version 必填，templateCode 不可修改会被置 null）
POST /micro-pdf/template/update
Body: {"id":1,"version":0,"templateName":"新名称","templateContext":"<html>...</html>"}

// 删除
POST /micro-pdf/template/remove
Body: {"id":1}
```

### 4.3 Mock 预览

```java
// 在线预览模板效果，无需先保存模板
POST /micro-pdf/template/mock
Body: {"templateContext":"<html><body><span th:text=\"${name}\"></span></body></html>","mockData":"{\"name\":\"测试\"}"}
// 返回: PDF 文件流（inline 展示）
```

### 4.4 缓存机制

- **本地缓存**: `PdfTemplateCache` 维护 `Map<String, MdmPdfTemplate>` 内存缓存
- **Redis 同步**: 写入 Redis key `sh:micro:pdf:cache:time` 标记变更时间
- **轮询检测**: 每 12 秒检测 Redis 变更时间，1 分钟内变更触发本地缓存刷新
- **防抖**: `init()` 方法 5 秒内不重复加载
- **主动刷新**: `clearCache()` 方法更新 Redis 时间戳 + 立即刷新本地缓存

## 5. 配置项

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| `shrimp.pdf.simsun.path` | `/usr/share/fonts/zh/simsun.ttf` | 宋体字体文件路径，仅 Unix/Linux 环境生效，用于 PDF 中文渲染 |

## 6. 依赖

### 框架依赖

| 模块 | 用途 |
|------|------|
| `sh-mybatis` | BaseMapper / BaseService / PageQuery |
| `sh-redis` | StringRedisTemplate（缓存同步） |

> 此模块不依赖 IAM 契约层。

### 第三方依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| `flying-saucer-pdf-itext5` | 9.7.2 | HTML → PDF 渲染引擎 |
| `thymeleaf` | 由 sh-bom 管理 | 模板引擎，将模板 + 数据渲染为 HTML |

### 模块间依赖

- `micro-file` ← `micro-pdf`（PDF 文件存储，架构层级上 micro-pdf 依赖 micro-file）

## 7. 常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| PDF 中文乱码 | Linux 环境缺少宋体字体 | 配置 `shrimp.pdf.simsun.path` 指向正确的 simsun.ttf 路径，确保字体文件存在 |
| 模板渲染报 "mock 数据不是 json 格式" | mockData 不是合法 JSON | 确保 mockData 为标准 JSON 字符串 |
| 模板编码重复 | templateCode 已存在 | templateCode 唯一性校验，新增/修改时不可与已有记录冲突 |
| 修改模板后缓存未更新 | 本地缓存 12 秒轮询延迟 | 调用 `PdfTemplateCache.clearCache()` 主动刷新 |
| PDF 生成失败 "生成 pdf 失败" | HTML 格式不合法或字体加载异常 | 检查 templateContext 是否为合法 HTML，检查字体路径是否正确 |
| Mock 预览无响应 | templateContext 为空 | Mock 接口要求 templateContext 必填 |
| 更新时 templateCode 被清空 | 设计如此，update 时 templateCode 置 null 不可修改 | 更新操作不允许修改 templateCode，需删除重建 |
