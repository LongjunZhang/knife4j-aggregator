# Knife4j AI Service

基于 Spring AI + Ollama 的 AI 服务，为 Knife4j 提供智能参数生成和错误解释能力。

## 快速开始

### 1. 启动 Ollama

```bash
# 使用 docker-compose 启动 Ollama 并拉取模型
cd knife4j-ai-service
docker-compose up -d

# 等待模型下载完成（首次约需 2-3 分钟）
docker-compose logs -f ollama-init
```

### 2. 启动 AI 服务

**方式一：IDE 运行**

直接运行 `AiServiceApplication.java`

**方式二：Maven 运行**

```bash
cd knife4j-ai-service
mvn spring-boot:run
```

### 3. 验证服务

```bash
# 健康检查
curl http://localhost:9100/actuator/health

# 测试参数生成
curl -X POST http://localhost:9100/api/ai/generate-params \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "user-service",
    "path": "/user/{id}",
    "method": "GET",
    "scenario": "normal"
  }'
```

## 配置说明

### application.yml

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: qwen2.5:3b-instruct
          temperature: 0.7

knife4j:
  ai:
    provider: ollama
    ollama:
      base-url: http://localhost:11434
      model: qwen2.5:3b-instruct
```

### 环境变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama 服务地址 |
| `OLLAMA_MODEL` | `qwen2.5:3b-instruct` | 使用的模型 |

## API 文档

### POST /api/ai/generate-params

生成 API 测试参数

### POST /api/ai/explain-error

解释 API 错误原因

### GET /actuator/health

健康检查

## 资源需求

- **内存**：4-8GB（取决于模型大小）
- **CPU**：建议 4 核以上
- **磁盘**：约 3GB（模型文件）





