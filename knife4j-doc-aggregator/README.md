# Doc Aggregator 文档聚合服务

独立部署的 API 文档聚合服务，与 Gateway 解耦。

## 功能特性

- **服务自动发现**：基于 Nacos 自动发现微服务
- **文档代理**：代理微服务的 OpenAPI 文档请求
- **静态 UI 托管**：托管 Knife4j UI 静态资源

## 快速开始

### 1. 构建前端 UI

```bash
# 方式一：使用构建脚本
chmod +x build-ui.sh
./build-ui.sh

# 方式二：手动构建
cd ../knife4j-vue3
pnpm install  # 或 npm install
pnpm build    # 或 npm run build

# 复制构建产物
cp -r dist/* ../doc-aggregator/src/main/resources/static/
```

### 2. 启动服务

确保 Nacos 已启动（默认 127.0.0.1:8848），然后：

```bash
mvn spring-boot:run
```

### 3. 访问文档

- UI 界面：http://localhost:9090/doc.html
- Swagger Config：http://localhost:9090/v3/api-docs/swagger-config
- 服务列表：http://localhost:9090/api/services

## 配置说明

```yaml
doc-aggregator:
  discovery:
    enabled: true                    # 是否启用服务发现
    excluded-services:               # 排除的服务
      - doc-aggregator
    doc-path: /v3/api-docs          # 微服务文档路径
  fetch:
    timeout: 10                      # 请求超时（秒）
    retry: 2                         # 重试次数
```

## API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/v3/api-docs/swagger-config` | GET | 获取服务列表（兼容 springdoc） |
| `/{serviceName}/v3/api-docs` | GET | 获取指定服务的文档 |
| `/api/services` | GET | 获取服务详情列表 |
| `/api/services/refresh` | POST | 手动刷新服务发现 |

