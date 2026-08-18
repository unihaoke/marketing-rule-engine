# syntax=docker/dockerfile:1
# ====================================================================
# 营销规则引擎 - 后端镜像（多阶段构建）
# JDK 21 / Spring Boot 3.2 / Maven 构建
# ====================================================================

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# 先复制所有 pom 文件，利用 Docker 层缓存加速依赖下载
# （pom 未变时，后续 RUN 命中缓存，跳过重复下载依赖）
COPY pom.xml ./
COPY rule-engine-core/pom.xml        rule-engine-core/
COPY rule-engine-infrastructure/pom.xml rule-engine-infrastructure/
COPY rule-engine-application/pom.xml rule-engine-application/
COPY rule-engine-web/pom.xml        rule-engine-web/

# 预下载依赖（联网环境生效；离线环境会失败但忽略，后续构建阶段会重试）
RUN mvn -B -q dependency:go-offline -pl rule-engine-web -am || true

# 复制全部源码
COPY . .

# 构建（跳过测试），输出 rule-engine-web/target/rule-engine-web-1.0.0.jar
RUN mvn -B -q clean package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# 复制构建产物
COPY --from=builder /build/rule-engine-web/target/rule-engine-web-1.0.0.jar app.jar

# 创建函数 Jar 上传目录（对应 application.yml 的 rule-engine.function-jar-dir）
RUN mkdir -p /app/data/functions

EXPOSE 8080

# 数据库连接参数通过环境变量注入（与 application.yml 占位符对应）
ENV JAVA_OPTS="" \
    DB_HOST=mysql \
    DB_PORT=3306 \
    DB_NAME=rule_engine \
    DB_USERNAME=root \
    DB_PASSWORD=tiger \
    TZ=Asia/Shanghai

# JAVA_OPTS 用于追加 JVM 参数（如 -Xmx -Xms 等）
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
