# Knife4j Doc Aggregator

**零侵入 API 文档聚合网关**

## 镜像说明

本镜像为 Knife4j Aggregator 项目的核心文档聚合网关服务，提供以下核心能力：

- **离线可读**: 微服务下线后文档仍可访问
- **内存缓存**: 毫秒级响应速度
- **持久化存储**: 基于 MongoDB 的文档存储
- **版本管理**: 完整的 API 变更追溯
- **Swagger 2.0 自动转换**: 自动转换为 OpenAPI 3.0
- **Nacos 服务发现**: 实时感知微服务上下线

## 快速启动

```bash
docker run -d \
  --name knife4j-doc-aggregator \
  -p 9090:9090 \
  -e NACOS_SERVER_ADDR=your-nacos-host:8848 \
  -e NACOS_NAMESPACE=your-namespace \
  longjunzhang/knife4j-doc-aggregator:1.0.0
```

## 环境变量

| 变量 | 说明 | 必填 |
|------|------|:----:|
| `NACOS_SERVER_ADDR` | Nacos 服务地址 | ✅ |
| `NACOS_NAMESPACE` | Nacos 命名空间 | ✅ |
| `MONGODB_URI` | MongoDB 连接地址 | 可选 |
| `AI_SERVICE_URL` | AI 服务地址 | 可选 |

## 访问地址

启动后访问: `http://localhost:9090/doc.html`

## 详细文档

更多配置和使用说明，请访问 GitHub 仓库：

https://github.com/LongjunZhang/knife4j-aggregator

## 许可证

Apache License 2.0
