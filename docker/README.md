# Knife4j 文档聚合系统 - Docker 部署指南

## 概述

本系统包含两个 Docker 容器：

| 容器 | 说明 | 端口 |
|------|------|------|
| `knife4j-doc-aggregator` | 文档聚合服务（包含前端 UI） | 9090 |
| `knife4j-ai-service` | AI 服务（参数生成、错误解释） | 9100 |

**外部依赖**（需要用户自行安装）：
- Nacos：服务注册与发现
- MongoDB：文档版本存储
- Ollama：本地 LLM 模型服务

## 快速开始

### 前提条件

确保以下服务已安装并运行：

```bash
# 1. Nacos（默认端口 8848）
# 下载: https://nacos.io/download/nacos-server/

# 2. MongoDB（默认端口 27017）
# 下载: https://www.mongodb.com/try/download/community

# 3. Ollama（默认端口 11434）
# 安装: https://ollama.ai/download
# 拉取模型:
ollama pull qwen2.5-coder:3b
```

### 启动服务

```bash
# 1. 下载配置文件
curl -O https://raw.githubusercontent.com/zhanglongjun/knife4j-doc-aggregator/main/docker-compose.yml
curl -O https://raw.githubusercontent.com/zhanglongjun/knife4j-doc-aggregator/main/.env.example

# 2. 配置环境变量
cp .env.example .env
# 按需修改 .env 文件

# 3. 启动服务
docker-compose up -d

# 4. 查看日志
docker-compose logs -f
```

### 访问服务

- **文档聚合 UI**: http://localhost:9090/doc.html
- **AI 服务健康检查**: http://localhost:9100/actuator/health
- **文档聚合健康检查**: http://localhost:9090/actuator/health

## 配置说明

### 环境变量

所有配置通过环境变量注入，可在 `.env` 文件中修改：

#### Nacos 配置

```env
NACOS_SERVER_ADDR=host.docker.internal:8848
NACOS_USERNAME=nacos
NACOS_PASSWORD=nacos
NACOS_NAMESPACE=public
```

#### MongoDB 配置

```env
MONGODB_URI=mongodb://admin:password123@host.docker.internal:27017/doc_aggregator?authSource=admin
```

#### Ollama 配置

```env
OLLAMA_BASE_URL=http://host.docker.internal:11434
OLLAMA_MODEL=qwen2.5-coder:3b
```

#### 服务端口

```env
DOC_AGGREGATOR_PORT=9090
AI_SERVICE_PORT=9100
```

### host.docker.internal

`host.docker.internal` 是 Docker 提供的特殊 DNS 名称，用于从容器内部访问宿主机服务。

- **macOS/Windows**: 默认支持
- **Linux**: 需要在 docker-compose.yml 中添加 `extra_hosts` 配置（已配置）

## 常用命令

```bash
# 启动服务
docker-compose up -d

# 停止服务
docker-compose down

# 查看日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f doc-aggregator
docker-compose logs -f knife4j-ai-service

# 重启服务
docker-compose restart

# 更新镜像
docker-compose pull
docker-compose up -d
```

## 故障排查

### 1. 容器无法启动

```bash
# 查看容器状态
docker-compose ps

# 查看详细日志
docker-compose logs doc-aggregator
```

### 2. 无法连接 Nacos

检查 Nacos 是否运行：
```bash
curl http://localhost:8848/nacos/v1/console/health/liveness
```

检查容器是否能访问宿主机：
```bash
docker exec -it knife4j-doc-aggregator curl http://host.docker.internal:8848/nacos/v1/console/health/liveness
```

### 3. 无法连接 MongoDB

检查 MongoDB 是否运行：
```bash
mongosh --eval "db.adminCommand('ping')"
```

### 4. AI 功能不工作

检查 Ollama 是否运行：
```bash
curl http://localhost:11434/api/tags
```

检查模型是否已拉取：
```bash
ollama list
```

### 5. Linux 下 host.docker.internal 不生效

确保 docker-compose.yml 中有以下配置：
```yaml
extra_hosts:
  - "host.docker.internal:host-gateway"
```

## 开发者指南

### 构建镜像

```bash
# 设置环境变量
export DOCKER_USERNAME=your-username
export VERSION=1.0.0

# 构建镜像
./build-and-push.sh ${VERSION} build

# 构建并推送
./build-and-push.sh ${VERSION} push
```

### 从源码构建（开发测试）

```bash
# 构建 doc-aggregator
docker build -f doc-aggregator/Dockerfile -t knife4j-doc-aggregator:dev .

# 构建 ai-service
docker build -f knife4j-ai-service/Dockerfile -t knife4j-ai-service:dev ./knife4j-ai-service
```

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0.0 | 2024-xx-xx | 初始版本 |

## 支持

如有问题，请提交 Issue：https://github.com/zhanglongjun/knife4j-doc-aggregator/issues
