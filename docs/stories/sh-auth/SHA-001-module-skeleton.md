# SHA-001：模块骨架与自动配置

## 故事描述

**作为** 框架开发者
**我想要** 创建 sh-auth Maven 模块骨架，包含 pom.xml、目录结构、配置类和自动配置入口
**以便** 后续的 SPI 契约、过滤器、缓存等组件可以在此基础上构建

## 验收标准

1. `sh-auth/pom.xml` 存在，parent 为 `sh-parent:5.0.0-SNAPSHOT`，版本 `5.0.1-SNAPSHOT`
2. 依赖：sh-web、sh-redis(optional)、jakarta-servlet-api(provided)、spring-boot-autoconfigure、slf4j-api、lombok(provided)、spring-boot-starter-test(test)
3. 目录结构：`com/wkclz/auth/` 下含 config/context/enums/exception/bean/contract/{auth,authz,infra}/filter/cache 子包
4. `AuthConstants.java` — 常量定义（Token 头名称、Redis Key 前缀）
5. `AuthProperties.java` — `@ConfigurationProperties(prefix = "sh.auth")` 配置（会话/密码/MFA/频控/白名单/CORS）
6. `ShAuthAutoConfiguration.java` — `@AutoConfiguration`，`@ConditionalOnProperty(sh.auth.enabled=true)`，`@ComponentScan("com.wkclz.auth")`
7. `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 包含配置类全限定名
8. `mvn compile -pl sh-auth -am` 通过

## 技术要点

- 零 IAM 依赖：pom.xml 中不得出现任何 `iam-*` 依赖
- sh-redis 标记为 optional
- Controller 扫描路径为 `com.wkclz.auth`

## 关联故事

- 依赖：无
- 被依赖：SHA-002~SHA-014 全部
