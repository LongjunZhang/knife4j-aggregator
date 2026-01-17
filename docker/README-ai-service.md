# Knife4j AI Service

**AI 智能分析服务**

## 镜像说明

本镜像为 Knife4j Aggregator 项目的 AI 智能分析服务，集成本地 AI 能力（基于 Ollama），为 API 调试提供智能辅助：

### 核心功能

- **AI 参数生成**: 根据 API Schema 自动生成符合规范的测试数据，告别手动构造复杂 JSON
- **AI 错误分析**: 捕获完整请求上下文 + 错误堆栈，AI 解读错误原因并给出修复建议

## 快速启动

```bash
docker run -d \
  --name knife4j-ai-service \
  -p 9100:9100 \
  -e OLLAMA_BASE_URL=http://your-ollama-host:11434 \
  -e OLLAMA_MODEL=qwen2.5:7b \
  longjunzhang/knife4j-ai-service:1.0.0
```

## 环境变量

| 变量 | 说明 | 必填 | 默认值 |
|------|------|:----:|:------:|
| `OLLAMA_BASE_URL` | Ollama 服务地址 | ✅ | - |
| `OLLAMA_MODEL` | 使用的模型名称 | 可选 | `qwen2.5:7b` |
| `SERVER_PORT` | 服务端口 | 可选 | `9100` |

## 前置依赖

需要先部署 Ollama 服务：

```bash
# 安装 Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# 拉取模型
ollama pull qwen2.5:7b
```

## 使用示例

AI 参数生成请求：
```bash
curl -X POST http://localhost:9100/ai/generate-params \
  -H "Content-Type: application/json" \
  -d '{"schema": {...}}'
```

AI 错误分析请求：
```bash
curl -X POST http://localhost:9100/ai/analyze-error \
  -H "Content-Type: application/json" \
  -d '{"error": "...", "stackTrace": "..."}'
```

## 详细文档

更多配置和使用说明，请访问 GitHub 仓库：

https://github.com/LongjunZhang/knife4j-aggregator

## 许可证

Apache License 2.0
