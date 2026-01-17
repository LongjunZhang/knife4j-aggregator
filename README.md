# Knife4j Aggregator

<p align="center">
  <b>零侵入 API 文档聚合 + AI 智能分析</b>
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-Apache%202.0-blue.svg" alt="License"></a>
  <a href="https://openjdk.org/"><img src="https://img.shields.io/badge/Java-17+-green.svg" alt="Java"></a>
  <a href="https://spring.io/projects/spring-boot"><img src="https://img.shields.io/badge/Spring%20Boot-3.2.x-brightgreen.svg" alt="Spring Boot"></a>
  <a href="https://github.com/xiaoymin/knife4j"><img src="https://img.shields.io/badge/Knife4j-4.x-orange.svg" alt="Knife4j"></a>
</p>

---

## 为什么选择 Knife4j Aggregator？

在微服务架构中，API 文档管理面临诸多挑战：**服务下线文档 404**、**无版本追溯**、**调试效率低**。

Knife4j Aggregator 提供了一套完整的解决方案：

```
+------------------------------------------------------------------+
|                                                                  |
|   "微服务下线了，文档还能看吗？"                                    |
|                                                                  |
|   官方方案:  用户 --> Gateway --> 微服务(下线) --> 404 Not Found   |
|                                                                  |
|   Aggregator: 用户 --> 内存缓存 --> MongoDB --> 历史版本文档 OK!    |
|                                                                  |
+------------------------------------------------------------------+
```

---

## 功能对比

| 特性                   | knife4j-aggregation | knife4j-gateway | knife4j-insight | **Knife4j Aggregator** |
|:---------------------|:-------------------:|:---------------:|:---------------:|:----------------------:|
| **独立部署**             | ❌ 嵌入宿主应用 | ❌ 依赖 Gateway | ✅ |           ✅            |
| **离线可读**             | ❌ | ❌ | ❌ |       ✅ 服务下线仍可访问       |
| **内存缓存**             | ❌ | ❌ | ❌ |        ✅ 毫秒级响应         |
| **持久化存储**            | ❌ | ❌ | ❌ |       ✅ MongoDB        |
| **版本管理**         | ❌ | ❌ | ❌ |      ✅ API 变更完整追溯      |
| **Swagger 2.0 自动转换** | ❌ | ❌ | ❌ |    ✅ 转 OpenAPI 3.0     |
| **AI 参数生成**          | ❌ | ❌ | ❌ |       ✅ 一键生成测试数据       |
| **AI 错误分析**          | ❌ | ❌ | ❌ |      ✅ 智能诊断+修复建议       |
| **Nacos 服务发现**       | ✅ | ✅ | ✅ |           ✅            |
| **Nacos 事件订阅**       | ❌ | ✅ | ❌ |       ✅ 实时感知上下线        |
| **开源免费**             | ✅ | ✅ | ❌ 商业版 |      ✅ Apache 2.0      |
| **业务代码侵入**           | 需要改代码 | 需要改代码 | 需要改代码 |         ✅ 零侵入          |

---

## AI 智能分析

集成本地 AI 能力（基于 Ollama），为 API 调试提供智能辅助：

### AI 参数生成

根据 API Schema 自动生成符合规范的测试数据，告别手动构造复杂 JSON：

```
+------------------------------------------+
|  POST /api/users                         |
|  +------------------------------------+  |
|  | {                                  |  |
|  |   "name": "张三",          <-- AI   |  |
|  |   "email": "zhangsan@test.com",    |  |
|  |   "age": 28,                       |  |
|  |   "address": {                     |  |
|  |     "city": "北京",                 |  |
|  |     "street": "朝阳区建国路88号"     |  |
|  |   }                                |  |
|  | }                                  |  |
|  +------------------------------------+  |
|              [ 一键生成 ]                 |
+------------------------------------------+
```

### AI 错误分析

捕获完整请求上下文 + 错误堆栈，AI 解读错误原因并给出修复建议：

```json
{
  "requestMethod": "GET",
  "httpStatus": 500,
  "api": "/messageService/message/exception/arithmetic",
  "timestamp": "2026-01-17T03:16:20.012038Z",
  "serviceName": "message-service",
  "instanceId": "198.18.0.1:4083",
  "errorReason": "ArithmeticException:/byzero",
  "rootError": "com.example.message.controller.MessageController.testArithmeticException:218",
  "analysis": "在处理请求时，由于除数为零导致了ArithmeticException异常。",
  "solution": [
    "检查请求参数，确保被除数不为零。"
  ]
}
```

**核心字段说明：**

| 字段 | 说明 |
|------|------|
| `errorReason` | 抛出错误的原因 |
| `rootError` | 发生错误的代码位置 |
| `analysis` | AI 对错误日志的智能分析 |
| `solution` | AI 给出的修复步骤建议 |

---

## 快速开始

### 方式一：Docker Compose（推荐）

```bash
# 1. 克隆项目
git clone https://github.com/zhanglongjun/knife4j-aggregator.git
cd knife4j-aggregator/docker

# 2. 配置环境变量
cp env.example .env
# 编辑 .env，配置 Nacos 地址等

# 3. 在 Nacos 配置中心创建路由配置
#    登录 Nacos 控制台 -> 配置管理 -> 配置列表 -> 新建配置
#    - Data ID: doc-aggregator.yaml
#    - Group: DEFAULT_GROUP
#    - 配置内容: 参考 nacos-config-example.yaml

# 4. 启动服务
docker-compose up -d

# 5. 访问文档
open http://localhost:9090/doc.html
```

### 方式二：源码构建

```bash
# 构建 doc-aggregator
cd knife4j-doc-aggregator
mvn clean package -DskipTests

# 启动（需要配置 Nacos 地址）
java -jar target/knife4j-doc-aggregator-*.jar \
  --NACOS_SERVER_ADDR=localhost:8848
```

### 前置依赖

| 组件 | 必须 | 说明 |
|------|:--:|------|
| Nacos | ✅  | 服务注册发现 + **配置中心（路由配置）** |
| MongoDB | 可选 | 不配置时仅内存缓存（重启后数据丢失），**版本切换功能需要 MongoDB** |
| Ollama |  ✅  | AI 功能支持，本地运行大语言模型 |

### 路由配置说明

**重要**: 业务服务的路由需要在 **Nacos 配置中心** 定义，支持动态刷新（无需重启）。

```yaml
# Nacos 配置示例 (Data ID: doc-aggregator.yaml)
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/userService/**

knife4j:
  aggregator:
    discover:
      service-context-paths:
        user-service: /userService
```

详细配置参考: [docker/nacos-config-example.yaml](docker/nacos-config-example.yaml)

---

## 架构设计

```
                              +-----------------+
                              |   Knife4j UI    |
                              |   (Vue3 前端)    |
                              +--------+--------+
                                       |
                                       v
+------------------------------------------------------------------------------+
|                         Knife4j Doc Aggregator                               |
|  +------------------------------------------------------------------------+  |
|  |                           API Gateway Layer                            |  |
|  |  +------------------+  +------------------+  +----------------------+  |  |
|  |  | /doc.html        |  | /v3/api-docs/*   |  | /api/ai/*            |  |  |
|  |  | 静态资源          |  | 文档代理          |  | AI 功能代理            |  |  |
|  |  +------------------+  +------------------+  +----------------------+  |  |
|  +------------------------------------------------------------------------+  |
|  |                           Core Services                                |  |
|  |  +------------------+  +------------------+  +----------------------+  |  |
|  |  | Discovery        |  | Cache Manager    |  | Version Manager      |  |  |
|  |  | Nacos 服务发现    |  | 内存缓存          |  | 版本管理 + Diff        |  |  |
|  |  +------------------+  +------------------+  +----------------------+  |  |
|  +------------------------------------------------------------------------+  |
|  |                           Storage Layer                                |  |
|  |  +------------------------------------------------------------------+  |  |
|  |  |                         MongoDB (可选)                            |  |  |
|  |  |   api_doc_versions  |  api_changes  |  service_info  |  sync_log |  |  |
|  |  +------------------------------------------------------------------+  |  |
|  +------------------------------------------------------------------------+  |
+------------------------------------------------------------------------------+
          |                                                     |
          |   +----------------------------------------+        |
          |   |             Knife4j AI Service         |        |
          |   |  +----------------+  +---------------+ |        |
          |   |  |  Spring AI     |  | Ollama LLM    | |        |
          |   |  |  参数生成       |   | 错误分析       | |        |
          |   |  +----------------+  +---------------+ |        |
          |   +-----------------------------------------+       |
          |                                                     |
          v                                                     v
+------------------------------------------------------------------------------+
|                        业务服务 (Business Services)                          |
|  +------------------------+  +------------------------+  +-----------------+ |
|  |     user-service       |  |     order-service      |  |  message-service| |
|  |  +------------------+  |  |  +------------------+  |  | +-------------+ | |
|  |  | Spring Boot 3.x  |  |  |  | Spring Boot 2.x  |  |  | |Spring Boot 2| | |
|  |  | OpenAPI 3.0      |  |  |  | Swagger 2.0      |  |  | |Swagger 2.0  | | |
|  |  +------------------+  |  |  +------------------+  |  | +-------------+ | |
|  +------------------------+  +------------------------+  +-----------------+ |
+------------------------------------------------------------------------------+
```

---

## 项目结构

```
knife4j-aggregator/
├── knife4j-doc-aggregator/         # 核心：文档聚合网关
├── knife4j-ai-service/             # AI 服务（可选部署）
├── knife4j-error-collector/        # 错误收集器 SDK
│   ├── knife4j-error-collector-core/
│   ├── knife4j-error-collector-spring-boot-starter/          # Spring Boot 2.x
│   └── knife4j-error-collector-jakarta-spring-boot-starter/  # Spring Boot 3.x
├── knife4j-ui/                     # 前端 UI（Vue3）
├── examples/                       # 示例服务
│   ├── spring-boot-2-examples/     # message-service, order-service
│   └── spring-boot-3-examples/     # user-service
└── docker/                         # Docker 部署
```

---

## 错误收集器 SDK

一行依赖，零配置，自动收集错误上下文：

```xml
<!-- Spring Boot 3.x -->
<dependency>
    <groupId>com.github.zhanglongjun</groupId>
    <artifactId>knife4j-error-collector-jakarta-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Spring Boot 2.x -->
<dependency>
    <groupId>com.github.zhanglongjun</groupId>
    <artifactId>knife4j-error-collector-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 配置参考

```yaml
knife4j:
  aggregator:
    enabled: true
    discover:
      enabled: true
      excluded-services:
        - doc-aggregator
    cache:
      enabled: true
      hard-ttl: 86400000   # 24小时
  ai:
    enabled: true
    service-url: http://localhost:9100
```

更多配置请参考 [docker/README.md](docker/README.md)

---

## 许可证

[Apache License 2.0](LICENSE)

---

## 致谢

- [Knife4j](https://github.com/xiaoymin/knife4j) - 优秀的 API 文档增强方案
- [Spring AI](https://spring.io/projects/spring-ai) - Spring 官方 AI 框架
- [Ollama](https://ollama.ai/) - 本地大语言模型运行时

---

<p align="center">
  <b>如果这个项目对你有帮助，请给个 Star 支持一下！</b>
</p>
