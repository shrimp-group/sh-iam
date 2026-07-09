---
name: micro-fileos
description: 当需要操作 micro-fileos 模块时使用此技能 —— 这是一个支持阿里云 OSS、AWS S3、S3 兼容协议的完整文件存储服务。当用户需要实现文件上传/下载、签名链接、分片上传、Hash去重、目录管理、Bucket 管理、图片处理参数、添加新 OSS 提供商、排查文件存储问题，或修改 micro-fileos 包（com.wkclz.micro.fileos）下任何代码时触发。
---

# Micro-Fileos 模块

Spring Boot 完整文件存储服务，基于策略模式支持阿里云 OSS、AWS S3、S3 兼容协议三种存储后端，提供简单上传、分片上传、预签名直传、签名链接、文件下载、Hash 去重、目录管理、图片处理等功能。

## 适用场景

- 修改 `micro-fileos` 包下任何 Java 文件
- 实现文件上传（简单/分片/预签名）、签名 URL、文件下载、文件删除功能
- 添加新的 OSS 提供商支持
- 调试文件存储、缓存、Hash 去重、分片上传、目录管理相关问题
- 理解 Bucket 路由、文件类型校验、图片处理、安全机制

## 架构概览

```
Controller      FileosUploadRest / FileosSignRest / FileosDownloadRest / FileosDeleteRest
                FileosPresignRest / FileosBucketRest / FileosDirectoryRest / FileosRecordRest
    ↓
API 接口        FileosUploadApi / FileosSignApi / FileosDownloadApi / FileosDeleteApi / FileosPresignUploadApi
    ↓
API 实现        *ApiImpl（继承 AbstractFileosApi）
    ↓
Service         FileosService → AliOssServiceImpl / S3ServiceImpl
                MdmFileosBucketService / MdmFileosRecordService / MdmFileosDirectoryService / MdmFileosMultipartService
    ↓
Mapper          MdmFileosBucketMapper / MdmFileosRecordMapper / MdmFileosDirectoryMapper / MdmFileosMultipartMapper
    ↓
缓存            BucketCache（Redis Pub/Sub）
Helper          PathHelper / DirectoryHelper / FileHashHelper / FileTypeHelper / ImageProcessHelper
```

**设计模式：**
- **策略模式**：`FileosService` 接口 + 两种实现，通过 `bucket.oss_sp` 配合 `OssSpEnum` 动态路由
- **模板方法**：`AbstractFileosApi` 封装 Bucket 查找、服务路由、URL 解析、文件校验、目录维护
- **缓存**：`BucketCache`，Redis Pub/Sub + 本地 Map，3 秒防抖

完整架构、数据库表结构、配置详情见 `micro-fileos/AGENTS.md`。

## 核心工作流

### 1. 简单上传

注入 `FileosUploadApi`，调用以下重载方法：

```java
@Autowired FileosUploadApi uploadApi;

uploadApi.upload(file);                                    // 默认 bucket
uploadApi.upload(file, "avatar");                          // 指定业务分类
uploadApi.upload(file, "avatar", "my-bucket");             // 指定 bucket
uploadApi.upload(file, "avatar", "my-bucket", true);       // 指定 bucket + 公开

FileosUploadRequest request = new FileosUploadRequest();
request.setCategory("avatar");
request.setImageProcess("{\"resize\":{\"width\":200}}");
uploadApi.upload(file, request);
```

上传流程：校验文件（Magic Bytes、扩展名白名单、大小） → Hash 去重检查 → 解析 Bucket → 路由到 OSS 服务 → 上传到存储 → 持久化记录到 `mdm_fileos_record` → 异步更新目录统计。

### 2. 签名链接（临时访问）

注入 `FileosSignApi`，为私有文件生成临时签名 URL：

```java
@Autowired FileosSignApi signApi;

signApi.sign(fileId);                                      // 默认 10 分钟过期
signApi.sign(fileId, 30, TimeUnit.MINUTES);                // 自定义过期
signApi.sign(fileIds);                                     // 批量签名
signApi.sign(user, User::getAvatarUrl, User::setAvatarUrl);// 泛型签名
signApi.sign(users, User::getAvatarUrl, User::setAvatarUrl);// 批量泛型签名
signApi.signContent(htmlContent);                           // 富文本 img src 签名
```

签名 URL 通过 OSS SDK 生成，若记录含 `imageProcess`，自动附加阿里云 OSS 图片处理参数。

### 3. 文件下载

注入 `FileosDownloadApi`，获取文件输入流：

```java
@Autowired FileosDownloadApi downloadApi;

InputStream is = downloadApi.download(fileId);              // 全量下载
InputStream is = downloadApi.download(fileId, offset, len); // 范围下载（断点续传）
```

### 4. 文件删除

注入 `FileosDeleteApi`，同时删除存储中的文件、数据库记录，并异步更新目录统计：

```java
@Autowired FileosDeleteApi deleteApi;

deleteApi.delete(fileId);    // 单个删除
deleteApi.delete(fileIds);   // 批量删除
```

### 5. 预签名上传（前端直传）

注入 `FileosPresignUploadApi`，生成预签名 URL 供前端直传 OSS：

```java
@Autowired FileosPresignUploadApi presignApi;

// 简单预签名
PresignUploadResponse resp = presignApi.presignUpload(request);
// 前端使用 resp.getPresignUrl() 直传到 OSS
// 前端上传完成后，后端确认
presignApi.presignComplete(completeRequest);

// 批量预签名
presignApi.presignUploadBatch(requests);
presignApi.presignCompleteBatch(completeRequests);

// 预签名分片上传
MultipartUploadInitResponse initResp = presignApi.initMultipartUpload(multipartInitReq);
presignApi.completeMultipartUpload(multipartCompleteReq);
presignApi.abortMultipartUpload(uploadId, fileId, bucketName, ossSp);
```

### 6. 分片上传（服务端代理）

注入 `FileosUploadApi`，服务端代理分片上传：

```java
@Autowired FileosUploadApi uploadApi;

MultipartUploadInitResponse initResp = uploadApi.initMultipartUpload(initReq);
uploadApi.completeMultipartUpload(completeReq);
uploadApi.abortMultipartUpload(uploadId, fileId, bucketName, ossSp);
```

### 7. 目录管理

目录由 `DirectoryHelper` 自动维护，上传/删除文件时异步更新 `mdm_fileos_directory` 表的 `fileCount` 和 `totalSize`。通过 REST API 查询：

| 端点 | 说明 |
|------|------|
| `/directory/list` | 目录列表 |
| `/directory/tree` | 目录树 |
| `/directory/info` | 目录详情 |

## 核心组件速查

### OssSpEnum → Bean 路由

```java
ALI_OSS("AliOssService")       → AliOssServiceImpl   // com.aliyun.oss.OSS
AWS_S3("S3Service")            → S3ServiceImpl        // S3Client + S3Presigner
S3_COMPATIBLE("S3Service")     → S3ServiceImpl        // 复用 S3 协议
```

### UploadTypeEnum

```java
SIMPLE    — 简单上传（服务端代理）
MULTIPART — 分片上传
PRESIGN   — 预签名上传（前端直传）
```

### UploadStatusEnum

```java
UPLOADING — 上传中
COMPLETED — 已完成
ABORTED   — 已中止
```

### BucketCache（Redis Pub/Sub）

- 频道：`shrimp:micro:fileos:bucket:cache:refresh`
- 两级缓存：`CACHE_BUCKET_DEFAULT` + `CACHE_BUCKET`（Map<bucketName, Bucket>）
- `clearCache()` → PUBLISH → 所有实例 `onMessage()` → `loadCache()`
- `synchronized` + 3 秒防抖

### PathHelper 路径规则

```
{system}/{env}/{category}/{day}/[public/]{timestamp}_{seq}_{safeFilename}
```

### FileHashHelper

- 默认算法：SHA-256
- 上传时计算文件 Hash，若 `sh.fileos.hash.enabled=true`，查询 `mdm_fileos_record` 是否已存在相同 Hash
- 去重命中时复用已有 `fileId`，但创建新的 `mdm_fileos_record` 记录

### ImageProcessHelper

- 支持 resize（缩放）、crop（裁剪）、watermark（水印）
- 仅阿里云 OSS 生效，签名时附加 `x-oss-process` 参数
- 参数存储在 `mdm_fileos_record.image_process` 字段，JSON 格式

### MultipartCleanupJob

- XXL-Job Handler：`fileosMultipartCleanup`
- 清理过期分片上传记录，中止 OSS 侧的分片上传，更新状态为 ABORTED

## 安全特性

| 机制 | 实现 |
|------|------|
| Magic Bytes 校验 | JPG/PNG/GIF/WebP/PDF/ZIP/MP4 等，按扩展名匹配校验文件头 |
| 路径穿越防护 | `OssUtil.sanitizeFileName()` 过滤 `/` `\` `..` `()` `+` `;` `&` |
| 大小限制 | 图片 ≤10MB / 视频 ≤500MB / 其他 ≤50MB（可配置） |
| 扩展名白名单 | 图片和视频分别维护白名单 |
| 租户隔离 | Bucket 和文件记录含 `tenant_code`，操作校验归属 |
| Hash 去重 | SHA-256 文件摘要，相同内容复用存储 |

## 配置项

```yaml
sh:
  fileos:
    max-size-mb: 50
    image:
      max-size-mb: 10
      extension-names: jpg,jpeg,png,gif,webp,svg,bmp
    video:
      max-size-mb: 500
      extension-names: mp4,mpeg,avi,mov,wmv,rm,rmvb,mkv,flv
    presign:
      expire-minutes: 30
      multipart:
        expire-minutes: 60
        default-part-size-mb: 5
    multipart:
      max-age-hours: 24
    hash:
      enabled: true
      algorithm: SHA-256
```

## 重要约束

1. **`BucketCache` 依赖 `RedisMessageListenerContainer`** Bean — 确保全局已配置
2. **业务异常**统一使用 `ValidationException.of("消息")` 抛出
3. **默认 Bucket**：未指定 bucket 参数时，使用 `default_flag=1` 的 Bucket
4. **Hash 去重**默认开启（`sh.fileos.hash.enabled=true`），去重命中时复用 `fileId` 但创建新记录
5. **分片上传清理**需配置 XXL-Job 任务 `fileosMultipartCleanup`
6. **图片处理**仅阿里云 OSS 支持，其他服务商忽略 `imageProcess` 参数
7. **目录统计异步**：`DirectoryHelper` 使用 `@Async` 异步更新，可能有短暂延迟
8. **预签名上传**：前端直传后必须调用 `presignComplete` 确认，否则记录状态为 `UPLOADING`

## 依赖

- `sh-core`：BaseEntity、ValidationException、R 返回对象、UserContext
- `sh-mybatis`：BaseService、BaseMapper、PageQuery
- `sh-redis`：StringRedisTemplate、RedisMessageListenerContainer
- `sh-spring`：Sys（环境信息）
- `sh-web`：ErrorHandler
- `sh-xxljob`：@XxlJob（分片上传清理任务）
- `aliyun-sdk-oss` / `software.amazon.awssdk:s3`

## 常见问题

| 问题 | 原因/解决 |
|------|----------|
| 上传失败"bucket 未配置" | 检查 `mdm_fileos_bucket` 表，或设置 `default_flag=1` |
| 签名链接无法访问 | 确认 Bucket 为私有读写，检查 OSS/S3 权限 |
| 多实例缓存不一致 | 确认 Redis 频道 `shrimp:micro:fileos:bucket:cache:refresh` 正常通信 |
| 文件类型校验失败 | Magic Bytes 校验的是实际内容 —— 检查文件是否损坏 |
| Hash 去重导致 fileId 相同 | 预期行为，相同内容共享存储路径，各自有独立记录 |
| 分片上传记录堆积 | 检查 XXL-Job 中 `fileosMultipartCleanup` 任务是否正常 |
| 图片处理参数不生效 | 确认 OSS 服务商为 `ALI_OSS`，其他服务商暂不支持 |
| 预签名上传后状态为 UPLOADING | 前端上传完成后需调用 `presignComplete` 确认 |
