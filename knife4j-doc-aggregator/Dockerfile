# ========================================
# Knife4j Doc Aggregator 镜像构建
# 包含：后端服务 + 前端 UI
# ========================================

# 全局 ARG：镜像源前缀
ARG REGISTRY_MIRROR=

# ----------------------------------------
# 阶段1: 构建前端
# ----------------------------------------
FROM ${REGISTRY_MIRROR}node:18-alpine AS frontend-builder

WORKDIR /app/frontend

# 配置 npm 镜像源
RUN npm config set registry https://registry.npmmirror.com

# 复制所有前端源代码
COPY knife4j-ui/ .

# 删除可能存在的 node_modules 并安装依赖
RUN rm -rf node_modules && \
    if [ -f pnpm-lock.yaml ]; then \
      npm install -g pnpm && pnpm install; \
    elif [ -f yarn.lock ]; then \
      yarn install; \
    else \
      npm ci || npm install; \
    fi

# 构建
RUN npm run build

# ----------------------------------------
# 阶段2: 构建后端（独立构建，不依赖父 pom）
# ----------------------------------------
ARG REGISTRY_MIRROR=
FROM ${REGISTRY_MIRROR}maven:3.9-eclipse-temurin-17 AS backend-builder

WORKDIR /app

# 配置 Maven 阿里云镜像
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?><settings><mirrors><mirror><id>aliyun</id><mirrorOf>*</mirrorOf><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' > /root/.m2/settings.xml

# 复制独立构建用的 pom
COPY knife4j-doc-aggregator/pom-docker.xml ./pom.xml

# 复制源代码
COPY knife4j-doc-aggregator/src ./src

# 复制前端构建产物到静态资源目录
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static

# 构建后端（跳过测试）
RUN mvn clean package -DskipTests

# ----------------------------------------
# 阶段3: 运行
# ----------------------------------------
ARG REGISTRY_MIRROR=
FROM ${REGISTRY_MIRROR}eclipse-temurin:17-jre

LABEL maintainer="zhanglongjun"
LABEL version="1.0.0"
LABEL description="Knife4j Doc Aggregator Service - API文档聚合服务"

WORKDIR /app

# 安装 curl 用于健康检查，设置时区
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

# 复制构建产物
COPY --from=backend-builder /app/target/*.jar /app/app.jar

# 暴露端口
EXPOSE 9090

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:9090/actuator/health || exit 1

# 启动命令（支持环境变量覆盖配置）
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]
