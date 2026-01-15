# Knife4j Aggregator

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-green.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-brightgreen.svg)](https://spring.io/projects/spring-boot)

**零侵入的 API 文档聚合网关，支持 AI 错误分析**

Non-invasive API documentation aggregation gateway with AI-powered error analysis.

## 特性

- **零侵入聚合**: 基于 Spring Cloud Gateway，无需修改业务服务代码即可聚合所有微服务的 API 文档
- **多版本支持**: 同时支持 Swagger 2.0 和 OpenAPI 3.0 规范，自动转换
- **AI 错误分析**: 集成 Ollama，提供智能参数生成和错误堆栈分析
- **API 版本管理**: 自动检测 API 变更，支持历史版本对比
- **文档缓存**: 智能缓存机制，提升文档加载性能
- **错误收集器**: 提供 SDK，一行代码集成错误收集能力

## 项目结构

```
knife4j-aggregator/
├── knife4j-doc-aggregator/     # 文档聚合网关服务
├── knife4j-ai-service/         # AI 分析服务
├── knife4j-error-collector/    # 错误收集器 SDK（发布到 Maven Central）
├── knife4j-ui/                 # 前端 UI（Vue3）
├── examples/                   # 示例服务
│   ├── spring-boot-2-examples/ # Spring Boot 2.x 示例
│   └── spring-boot-3-examples/ # Spring Boot 3.x 示例
└── docker/                     # Docker 部署文件
```

## 快速开始

### 使用 Docker Compose（推荐）

```bash
# 进入 docker 目录
cd docker

# 复制环境变量配置
cp env.example .env

# 启动服务
docker-compose up -d
```

访问: http://localhost:9090/doc.html

### 前置依赖

- **Nacos**: 服务注册与发现
- **MongoDB**: API 文档版本存储（可选）
- **Ollama**: AI 模型服务（可选，用于 AI 功能）

## 模块说明

### knife4j-doc-aggregator

API 文档聚合网关，核心功能：

- 自动发现 Nacos 注册的微服务
- 聚合各服务的 OpenAPI/Swagger 文档
- 提供统一的 API 文档访问入口
- API 版本管理与变更检测

### knife4j-ai-service

AI 分析服务，提供：

- 智能参数生成：根据 API 定义自动生成请求参数
- 错误分析：解析错误堆栈，提供修复建议

### knife4j-error-collector

错误收集器 SDK，特性：

- 支持 Spring Boot 2.x (javax) 和 3.x (jakarta)
- 自动收集请求上下文和错误堆栈
- 与 AI 服务联动，提供智能分析

**Maven 依赖**:

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

### knife4j-ui

基于 Vue3 的前端界面，特性：

- 现代化 UI 设计
- API 文档浏览与调试
- AI 辅助功能集成
- API 版本切换

## 架构图

```
                    ┌─────────────────┐
                    │   knife4j-ui    │
                    │   (Vue3 前端)    │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ doc-aggregator  │
                    │  (API 网关)      │
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│ user-service  │   │ order-service │   │message-service│
│ (Spring Boot  │   │ (Spring Boot  │   │ (Spring Boot  │
│     3.x)      │   │     2.x)      │   │     2.x)      │
└───────────────┘   └───────────────┘   └───────────────┘
        │                    │                    │
        └────────────────────┼────────────────────┘
                             │
                    ┌────────▼────────┐
                    │  ai-service     │
                    │  (AI 分析)      │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │     Ollama      │
                    │   (本地 LLM)    │
                    └─────────────────┘
```

## 配置说明

详细配置请参考 [docker/README.md](docker/README.md)

## 贡献指南

欢迎提交 Issue 和 Pull Request！

## 许可证

[Apache License 2.0](LICENSE)

## 相关项目

- [Knife4j](https://github.com/xiaoymin/knife4j) - Swagger2 和 OpenAPI3 增强解决方案
