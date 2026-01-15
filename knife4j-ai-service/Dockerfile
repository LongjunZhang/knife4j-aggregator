# ========================================
# Knife4j AI Service 镜像构建
# ========================================

# 全局 ARG：镜像源前缀
ARG REGISTRY_MIRROR=

# ----------------------------------------
# 阶段1: 构建
# ----------------------------------------
FROM ${REGISTRY_MIRROR}maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 配置 Maven 阿里云镜像
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?><settings><mirrors><mirror><id>aliyun</id><mirrorOf>*</mirrorOf><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' > /root/.m2/settings.xml

# 复制 pom 和源代码（相对于构建上下文根目录）
COPY knife4j-ai-service/pom.xml ./
COPY knife4j-ai-service/src ./src

# 构建（跳过测试）
RUN mvn clean package -DskipTests

# ----------------------------------------
# 阶段2: 运行
# ----------------------------------------
ARG REGISTRY_MIRROR=
FROM ${REGISTRY_MIRROR}eclipse-temurin:17-jre

LABEL maintainer="zhanglongjun"
LABEL version="1.0.0"
LABEL description="Knife4j AI Service - AI参数生成与错误解释服务"

WORKDIR /app

# 安装 curl 用于健康检查，设置时区
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

# 复制构建产物
COPY --from=builder /app/target/*.jar /app/app.jar

# 暴露端口
EXPOSE 9100

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:9100/actuator/health || exit 1

# 启动命令（支持环境变量覆盖配置）
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]
