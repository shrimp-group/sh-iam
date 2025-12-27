FROM maven:3-eclipse-temurin-25-alpine AS builder

WORKDIR /apps

COPY . .
RUN mvn -s settings.xml clean -Dmaven.test.skip=true -pl iam-admin-starter -am package

# 基础镜像使用java
FROM eclipse-temurin:25-jdk-alpine AS runner

# 作者
MAINTAINER wangkc <wkclz@qq.com>

# 工作目录
WORKDIR /apps

# VOLUME 指定了临时文件目录为/tmp。
# 其效果是在主机 /var/lib/docker 目录下创建了一个临时文件，并链接到容器的/tmp
VOLUME /tmp

# 将jar包添加到容器中并更名为app.jar
COPY --from=builder /apps/iam-admin-starter/target/*.jar app.jar


# 运行jar包
# ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","./app.jar",'--spring.profiles.active=sit']
CMD java -server ${JVM_OPTIONS} -jar app.jar
