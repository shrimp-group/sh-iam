---
name: "sh-tool"
description: "sh-framework 底层工具模块知识库。提供加密(AES/DES/RSA/MD5/SHA/Base64)、字符串格式化、日期、Bean操作、文件IO、网络、雪花ID、验证码、二维码、JS引擎等工具。当涉及 sh-tool 模块的代码编写、工具选型、类使用时调用。"
---

# sh-tool 模块知识库

sh-tool 是 sh-framework 最底层的工具模块，**无任何框架内部依赖**，为上层模块提供基础工具能力。

## 包结构

```
com.wkclz.tool
├── bean/
│   ├── JavaField          # 字段描述Bean（fieldName, columnName, getter, setter, notNull, clazz）
│   └── SystemBaseInfo     # 系统基础信息（10个静态内部类：Disk, Memory, GC, Thread等）
├── tools/                  # 加密工具集
│   ├── AesTool            # AES对称加密（128/192/256位，SHA1PRNG密钥，ECB/PKCS5Padding）
│   ├── DesTool            # DES对称加密（56位密钥）
│   ├── RsaTool            # RSA非对称加密（基于Hutool SecureUtil.rsa()，支持1024/2048/4096位）
│   ├── Md5Tool            # MD5哈希（16/32位大小写，正则校验）
│   ├── ShaTool            # SHA哈希（SHA-1/256/384/512，算法白名单校验）
│   ├── Base64Tool         # Base64编解码（被AesTool/DesTool/RsaTool依赖）
│   └── RegularTool        # 正则工具（9种预编译常量：手机号/邮箱/IP/URL等，Pattern缓存）
└── utils/                  # 通用工具集
    ├── StringFormat        # 字符串格式化（{}占位符 + ${var}命名变量 + ${var}[条件渲染]）
    ├── StringUtil          # 字符串转换（下划线↔驼峰，首字母大小写，特殊字符清理）
    ├── DateUtil            # 日期工具（字符串转Date，获取今天零点，时间差计算）
    ├── JsonUtil            # JSON文件读写（基于fastjson2，自定义格式化）
    ├── MapUtil             # Map与对象互转（obj2Map, map2Obj, key下划线转驼峰, URL参数序列化）
    ├── BeanUtil            # Bean属性操作（深拷贝cpAll/cpNotNull, 空属性清理, 字段元信息缓存）
    ├── FileUtil            # 文件操作（读写删除，临时目录，文件大小格式化，不允许覆盖已有文件）
    ├── CompressUtil        # ZIP压缩解压（保留目录结构，防ZIP Slip路径穿越攻击）
    ├── NetworkUtil         # 网络工具（服务器IP获取，排除lo/docker接口，内网IP判断）
    ├── SnowflakeIdWorker   # 雪花ID生成器（64位：1符号+41时间+5数据中心+5机器+12序列，synchronized线程安全）
    ├── SecretUtil          # 安全综合工具（AES密码加解密，验证码生成，UUID，AES密钥生成）
    ├── CheckPwdUtil        # 密码强度校验（加权评分0-100，连续/重复字符减分，多种类型加分）
    ├── ClassUtil           # 类扫描工具（包下Class扫描，接口实现类查找，含父类方法获取）
    ├── IntegerUtil         # 整数列表转换（字符串按分隔符转Integer/Long列表，仅保留正整数）
    ├── JsUtil              # JS执行引擎（Rhino，JS函数MD5缓存，支持String/JSONObject/Map参数）
    ├── QrCodeUtil          # 二维码/条码生成（ZXing，QR_CODE/CODE_39，Base64输出）
    ├── ValidateCode        # 图形验证码（7种类型：数字/字母/混合，干扰线，自定义颜色）
    ├── PropertiesUtil      # Properties文件操作（读写，转Map，转对象反射赋值）
    ├── ServerStateUtil     # JVM运行状态采集（JMX：类加载/JIT/OS/线程/内存/GC/磁盘）
    ├── AreaUtil            # [已注释] 行政区划爬虫
    └── EnumUtil            # [已注释] 枚举字典工具
```

## 核心类使用指南

### StringFormat — 字符串格式化（框架核心推荐使用）

```java
// {} 占位符格式化（SLF4J风格）
StringFormat.of("用户 {} 操作了 {}", "张三", "删除")  // "用户 张三 操作了 删除"

// ${var} 命名变量格式化
Map<String, Object> params = new HashMap<>();
params.put("name", "张三");
StringFormat.of("用户 ${name} 已登录", params)  // "用户 张三 已登录"

// ${var}[content] 条件渲染：var非空时渲染[]内内容，为空时跳过
params.put("reason", "密码过期");
StringFormat.of("登录失败${reason}[，原因：${reason}]", params)  // "登录失败，原因：密码过期"
params.remove("reason");
StringFormat.of("登录失败${reason}[，原因：${reason}]", params)  // "登录失败"
```

### BeanUtil — Bean属性操作

```java
// 深拷贝（含null值）
TargetObj copy = BeanUtil.cpAll(source);

// 深拷贝（忽略null值，只覆盖有值的属性）
TargetObj copy = BeanUtil.cpNotNull(source);

// 清理空字符串属性（将空字符串设为null）
BeanUtil.removeBlank(obj);

// List级别拷贝
List<TargetObj> list = BeanUtil.cp(sourceList, TargetObj.class);
```

### 加密工具使用

```java
// AES加密/解密
String encrypted = AesTool.encrypt("明文", "种子");
String decrypted = AesTool.decrypt(encrypted, "种子");

// RSA密钥对生成与加解密
String[] keyPair = RsaTool.genKeyPair(2048);  // [privateKey, publicKey]
String encrypted = RsaTool.encryptByPublicKey("明文", publicKey);
String decrypted = RsaTool.decryptByPrivateKey(encrypted, privateKey);

// MD5哈希
String hash = Md5Tool.md5lowerCase32("内容");  // 32位小写

// 密码加解密（推荐带盐方式）
String encryptPwd = SecretUtil.getEncryptPassword("密码", "盐值");
String decryptPwd = SecretUtil.getDecryptPassword(encryptPwd, "盐值");
```

### SnowflakeIdWorker — 雪花ID

```java
// 需要实例化使用，workerId和datacenterId范围0~31
SnowflakeIdWorker idWorker = new SnowflakeIdWorker(0, 0);
long id = idWorker.nextId();  // synchronized线程安全
```

## 类间依赖关系

```
Base64Tool ◄── AesTool / DesTool / RsaTool（加解密结果的Base64编解码）
AesTool ◄── SecretUtil（密码加解密）
Md5Tool ◄── SecretUtil（密钥生成）/ JsUtil（JS脚本MD5缓存指纹）
RegularTool ◄── IntegerUtil（正整数校验）
StringUtil ◄── MapUtil（Map key下划线转驼峰）
FileUtil ◄── JsonUtil（读取JSON文件）
MapUtil ◄── PropertiesUtil（Properties/Map转换和排序）
JavaField ◄── BeanUtil（字段元信息Bean）
SystemBaseInfo ◄── ServerStateUtil（JVM指标数据结构）
```

## 设计模式与约定

- **缓存模式**：BeanUtil、RegularTool、JsUtil均使用ConcurrentHashMap缓存反射/编译结果
- **工具类规范**：StringFormat为final类+私有构造函数；ValidateCode私有构造函数
- **线程安全**：SnowflakeIdWorker.nextId()使用synchronized；BeanUtil/JsUtil的缓存初始化使用synchronized
- **安全设计**：CompressUtil防ZIP Slip路径穿越；SecretUtil默认盐方法@Deprecated；FileUtil.writeFile()不允许覆盖
- **异常处理**：统一使用RuntimeException包装受检异常
