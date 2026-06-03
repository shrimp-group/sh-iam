---
name: "sh-bom"
description: "sh-framework BOM依赖版本管理模块知识库。统一管理所有第三方依赖版本，无Java代码，仅pom.xml的dependencyManagement。微信模块排除旧BouncyCastle冲突，Druid使用Spring Boot 4适配版，Redis版本继承Spring Boot BOM。当涉及依赖版本查询、新增依赖版本管理、版本冲突排查时调用。"
---

# sh-bom 模块知识库

sh-bom 是框架的 BOM（Bill of Materials，物料清单）模块，通过 `<dependencyManagement>` 统一管理所有第三方依赖的版本，不包含任何 Java 代码。

## 受管依赖版本清单

### 通用工具

| GroupId | ArtifactId | 版本 | 说明 |
|---------|-----------|------|------|
| org.apache.commons | commons-collections4 | 4.5.0 | 集合工具 |
| com.google.guava | guava | 33.5.0-jre | Google核心工具库 |
| com.alibaba.fastjson2 | fastjson2 | 2.0.60 | JSON序列化 |
| cn.hutool | hutool-all | 5.8.42 | 国产工具库 |
| org.projectlombok | lombok | 1.18.42 | 编译期代码生成 |

### 二维码

| GroupId | ArtifactId | 版本 |
|---------|-----------|------|
| com.google.zxing | core | 3.5.4 |
| com.google.zxing | javase | 3.5.4 |

### JWT

| GroupId | ArtifactId | 版本 |
|---------|-----------|------|
| io.jsonwebtoken | jjwt-api | 0.13.0 |
| io.jsonwebtoken | jjwt-impl | 0.13.0 |
| io.jsonwebtoken | jjwt-jackson | 0.13.0 |

### 微信（排除旧BouncyCastle）

| GroupId | ArtifactId | 版本 | 排除 |
|---------|-----------|------|------|
| com.github.binarywang | weixin-java-miniapp | 4.8.0 | bcpkix-jdk15on |
| com.github.binarywang | weixin-java-pay | 4.8.0 | bcprov-jdk15on |
| com.github.binarywang | weixin-java-open | 4.8.0 | — |
| com.github.binarywang | weixin-java-mp | 4.8.0 | — |
| com.github.binarywang | weixin-java-cp | 4.8.0 | — |

### 定时任务

| GroupId | ArtifactId | 版本 |
|---------|-----------|------|
| com.xuxueli | xxl-job-core | 3.3.1 |

### 加密

| GroupId | ArtifactId | 版本 |
|---------|-----------|------|
| org.bouncycastle | bcprov-jdk18on | 1.83 |

### MQTT

| GroupId | ArtifactId | 版本 |
|---------|-----------|------|
| org.eclipse.paho | org.eclipse.paho.client.mqttv3 | 1.2.5 |

### 邮件

| GroupId | ArtifactId | 版本 |
|---------|-----------|------|
| com.sun.mail | jakarta.mail | 2.0.2 |

### 数据库

| GroupId | ArtifactId | 版本 | 说明 |
|---------|-----------|------|------|
| com.alibaba | druid-spring-boot-4-starter | 1.2.28-SNAPSHOT | Spring Boot 4适配版 |
| com.mysql | mysql-connector-j | 9.5.0 | compile scope |
| org.mybatis.spring.boot | mybatis-spring-boot-starter | 4.0.0 | — |
| com.github.pagehelper | pagehelper-spring-boot-starter | 2.1.1 | — |

### AOP

| GroupId | ArtifactId | 版本 |
|---------|-----------|------|
| org.springframework.boot | spring-boot-starter-aop | 3.5.9 |

### Redis（版本继承Spring Boot BOM）

| GroupId | ArtifactId | 说明 |
|---------|-----------|------|
| org.springframework.boot | spring-boot-starter-data-redis | 版本继承自Spring Boot |
| redis.clients | jedis | 版本继承自Spring Boot |
| io.lettuce | lettuce-core | 版本继承自Spring Boot |

### 对象存储

| GroupId | ArtifactId | 版本 |
|---------|-----------|------|
| software.amazon.awssdk | s3 | 2.41.14 |
| com.aliyun.oss | aliyun-sdk-oss | 3.18.4 |

### 支付

| GroupId | ArtifactId | 版本 |
|---------|-----------|------|
| com.alipay.sdk | alipay-sdk-java | 4.40.630.ALL |

### 脚本引擎

| GroupId | ArtifactId | 版本 |
|---------|-----------|------|
| org.mozilla | rhino | 1.9.0 |

## 关键设计要点

1. **排除冲突**：微信模块显式排除旧版BouncyCastle，框架统一使用bcprov-jdk18on(1.83)
2. **Spring Boot BOM继承**：Redis相关依赖版本继承自Spring Boot，保证兼容性
3. **AOP版本特殊**：spring-boot-starter-aop明确3.5.9，而非继承Spring Boot BOM
4. **Druid SNAPSHOT**：druid-spring-boot-4-starter使用1.2.28-SNAPSHOT，Spring Boot 4适配开发版
5. **MySQL compile scope**：mysql-connector-j显式设置compile scope，确保随项目打包
6. **微信模块排除**：weixin-java-miniapp排除bcpkix-jdk15on，weixin-java-pay排除bcprov-jdk15on

## 使用方式

在业务项目的pom.xml中引入：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.wkclz.framework</groupId>
            <artifactId>sh-bom</artifactId>
            <version>${revision}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

引入后，子模块中声明依赖时**不需要指定版本号**，版本由sh-bom统一管理。
