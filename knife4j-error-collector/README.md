# Knife4j Error Collector Spring Boot Starter

微服务异常堆栈收集器，用于收集微服务中的异常信息并提供给 doc-aggregator 进行 AI 错误分析。

## 特性

- ✅ 默认关闭，需显式启用
- ✅ 内部 Token 鉴权，只对来自 doc-aggregator 的请求收集错误
- ✅ 同时支持 Spring Boot 2.x (Javax) 和 3.x (Jakarta)
- ✅ 结构化错误元数据输出（`_errorMeta`）
- ✅ 内存 TTL 存储，自动过期清理
- ✅ 堆栈信息脱敏（密码、token、手机号等）
- ✅ 包名白名单过滤，只保留业务包的栈帧

## 模块结构

```
knife4j-error-collector/
├── knife4j-error-collector-core/                    # 核心逻辑（无 Servlet API 依赖）
├── knife4j-error-collector-spring-boot-starter/     # Spring Boot 2.x (javax.servlet)
└── knife4j-error-collector-jakarta-spring-boot-starter/  # Spring Boot 3.x (jakarta.servlet)
```

## 快速开始

### 1. 添加依赖

**Spring Boot 3.x 项目：**

```xml
<dependency>
    <groupId>com.github.zhanglongjun</groupId>
    <artifactId>knife4j-error-collector-jakarta-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Spring Boot 2.x 项目：**

```xml
<dependency>
    <groupId>com.github.zhanglongjun</groupId>
    <artifactId>knife4j-error-collector-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置

在 `application.yml` 中添加配置：

```yaml
knife4j:
  error-collector:
    # 启用错误收集器（默认 false）
    enabled: true
    # 内部鉴权 token（需要与 doc-aggregator 配置一致）
    internal-token: ${ERROR_COLLECTOR_TOKEN:dev-token-123}
    # Token 请求头名称
    token-header-name: X-Error-Collector-Token
    # 异常链最大层数
    stack-max-depth: 5
    # 堆栈片段最大帧数
    stack-snippet-max-frames: 10
    # 错误详情 TTL（分钟）
    ttl-minutes: 30
    # 包名白名单（只保留这些包的栈帧）
    package-whitelist:
      - com.example
      - com.github.zhanglongjun
    # 脱敏正则表达式
    mask-patterns:
      - "(?i)(password|token|secret)=.*"
      - "1[3-9]\\d{9}"  # 手机号
```

## 工作原理

### 数据流

```
┌─────────────┐     ┌────────────────┐     ┌─────────────┐
│ Knife4j UI  │────▶│ DocAggregator  │────▶│  微服务     │
└─────────────┘     └────────────────┘     └─────────────┘
                           │                      │
                           │ X-Error-Collector-Token
                           │                      │
                           ▼                      ▼
                    ┌────────────────┐     ┌─────────────┐
                    │  请求上下文缓存  │     │ ErrorStore  │
                    └────────────────┘     └─────────────┘
                           │                      │
                           └──────────┬───────────┘
                                      │
                                      ▼
                               ┌────────────┐
                               │ AI Service │
                               └────────────┘
```

### 响应格式

当发生异常且请求来自 doc-aggregator 时，响应会包含 `_errorMeta` 字段：

```json
{
  "code": 500,
  "message": "系统异常",
  "data": null,
  "_errorMeta": {
    "errorId": "err_abc123def456",
    "serviceName": "user-service",
    "exceptionClass": "java.lang.NullPointerException",
    "rootCauseClass": "java.lang.NullPointerException",
    "message": "Cannot invoke method on null",
    "causeChain": [
      {
        "exceptionClass": "java.lang.NullPointerException",
        "message": "Cannot invoke method on null",
        "level": 0,
        "location": "com.example.user.service.UserService.getUser:45"
      }
    ],
    "stackFingerprint": "a1b2c3d4e5f6...",
    "stackSnippet": "at com.example.user.service.UserService.getUser(UserService.java:45)\n...",
    "timestamp": "2024-01-01T12:00:00Z"
  }
}
```

### 内部接口

微服务会暴露内部接口供 doc-aggregator 查询完整错误详情：

```
GET /internal/error-collector/errors/{errorId}
Header: X-Error-Collector-Token: {token}

GET /internal/error-collector/status
Header: X-Error-Collector-Token: {token}
```

## 安全说明

1. **默认关闭**：`enabled=false`，必须显式开启
2. **Token 鉴权**：只有携带正确 token 的请求才会收集错误
3. **网关剥离**：建议在网关配置剥离 `X-Error-Collector-Token` 请求头，防止外部伪造
4. **数据脱敏**：密码、token、手机号等敏感信息会自动脱敏
5. **包名过滤**：堆栈信息只保留业务包，排除框架代码

## 配置项说明

| 配置项 | 类型 | 默认值 | 说明 |
|-------|------|-------|------|
| `enabled` | boolean | false | 是否启用错误收集器 |
| `internal-token` | string | - | 内部鉴权 token |
| `token-header-name` | string | X-Error-Collector-Token | Token 请求头名称 |
| `stack-max-depth` | int | 5 | 异常链最大层数 |
| `stack-snippet-max-frames` | int | 10 | 堆栈片段最大帧数 |
| `stack-snippet-max-length` | int | 2048 | 堆栈片段最大字符数 |
| `package-whitelist` | List<String> | [] | 包名白名单 |
| `ttl-minutes` | int | 30 | 错误详情 TTL |
| `cleanup-interval-minutes` | int | 5 | 过期清理间隔 |
| `mask-patterns` | List<String> | 预置 | 脱敏正则表达式 |
| `mask-replacement` | string | [MASKED] | 脱敏替换文本 |
| `include-stack-snippet` | boolean | true | 是否在响应中包含堆栈片段 |
| `internal-api-prefix` | string | /internal/error-collector | 内部接口路径前缀 |
| `max-stored-errors` | int | 10000 | 最大存储错误数量 |

## 与 doc-aggregator 集成

在 doc-aggregator 的配置中添加：

```yaml
knife4j:
  error-collector-proxy:
    enabled: true
    internal-token: ${ERROR_COLLECTOR_TOKEN:dev-token-123}
    token-header-name: X-Error-Collector-Token
    timeout: 30s
```

## 构建

```bash
cd knife4j-error-collector
mvn clean install
```

