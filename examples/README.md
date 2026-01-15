# Examples - 示例服务

本目录包含演示如何使用 knife4j-aggregator 的示例服务。

## 目录结构

```
examples/
├── spring-boot-2-examples/     # Spring Boot 2.x 示例
│   ├── message-service/        # 消息服务（Springfox Swagger 2.0）
│   └── order-service/          # 订单服务（Springdoc OpenAPI 3.0）
└── spring-boot-3-examples/     # Spring Boot 3.x 示例
    └── user-service/           # 用户服务（Knife4j OpenAPI 3.0）
```

## 示例说明

### message-service (Spring Boot 2.7.x)

- **API 规范**: Swagger 2.0
- **文档框架**: Springfox Boot Starter 3.0.0
- **错误收集器**: knife4j-error-collector-spring-boot-starter

### order-service (Spring Boot 2.7.x)

- **API 规范**: OpenAPI 3.0
- **文档框架**: Springdoc OpenAPI UI
- **错误收集器**: knife4j-error-collector-spring-boot-starter

### user-service (Spring Boot 3.2.x)

- **API 规范**: OpenAPI 3.0
- **文档框架**: Knife4j OpenAPI3 Jakarta
- **错误收集器**: knife4j-error-collector-jakarta-spring-boot-starter

## 运行示例

### 前置条件

1. 启动 Nacos 服务（默认地址：localhost:8848）
2. （可选）启动 MongoDB（用于 API 版本管理）

### 构建与运行

```bash
# 进入 Spring Boot 2.x 示例目录
cd spring-boot-2-examples

# 构建
mvn clean package -DskipTests

# 运行 message-service
java -jar message-service/target/message-service-*.jar

# 运行 order-service
java -jar order-service/target/order-service-*.jar
```

```bash
# 进入 Spring Boot 3.x 示例目录
cd spring-boot-3-examples

# 构建
mvn clean package -DskipTests

# 运行 user-service
java -jar user-service/target/user-service-*.jar
```

## 验证

启动 doc-aggregator 后，访问 http://localhost:9090/doc.html 即可看到所有示例服务的 API 文档。

## 错误收集器使用示例

在业务服务中引入依赖后，错误收集器会自动：

1. 收集所有 HTTP 请求的上下文信息
2. 捕获异常堆栈并关联请求上下文
3. 通过 `/internal/error-collector/errors/{id}` 接口暴露错误详情
4. 与 AI 服务联动，提供智能错误分析
