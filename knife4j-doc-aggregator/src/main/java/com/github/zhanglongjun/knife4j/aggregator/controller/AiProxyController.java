package com.github.zhanglongjun.knife4j.aggregator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zhanglongjun.knife4j.aggregator.config.AiProxyProperties;
import com.github.zhanglongjun.knife4j.aggregator.dto.ApiDefinition;
import com.github.zhanglongjun.knife4j.aggregator.service.ApiDefinitionExtractor;
import com.github.zhanglongjun.knife4j.aggregator.service.ErrorDetailCentralStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * AI 代理控制器
 * 
 * 将 /api/ai/* 请求代理到 knife4j-ai-service
 * 在转发前从 Mongo 提取 ApiDefinition 并附加到请求中
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@ConditionalOnProperty(name = "knife4j.ai.enabled", havingValue = "true", matchIfMissing = true)
public class AiProxyController {
    
    private final WebClient webClient;
    private final AiProxyProperties properties;
    private final ApiDefinitionExtractor apiDefinitionExtractor;
    private final ErrorDetailCentralStore errorDetailCentralStore;
    private final ObjectMapper objectMapper;
    
    public AiProxyController(AiProxyProperties properties, 
                             ApiDefinitionExtractor apiDefinitionExtractor,
                             ErrorDetailCentralStore errorDetailCentralStore) {
        this.properties = properties;
        this.apiDefinitionExtractor = apiDefinitionExtractor;
        this.errorDetailCentralStore = errorDetailCentralStore;
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.builder()
            .baseUrl(properties.getServiceUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
        log.info("AI Proxy initialized, service-url: {}", properties.getServiceUrl());
    }
    
    /**
     * 流式解释错误 (SSE 代理)
     * 
     * 真正的 Server-Sent Events 流式转发，像 ChatGPT 一样逐字返回。
     * 从 knife4j-ai-service 接收 SSE 流并透传给前端。
     */
    @PostMapping(value = "/explain-error/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> explainErrorStream(@RequestBody Map<String, Object> request) {
        log.info("Proxying SSE explain-error request");
        
        // 脱敏处理
        sanitizeRequest(request);
        
        // 检查是否有 errorId，如果有则从集中存储获取详情
        String errorId = getStringValue(request, "errorId", null);
        if (errorId != null && !errorId.isEmpty()) {
            var errorDetail = errorDetailCentralStore.get(errorId);
            if (errorDetail.isPresent()) {
                // 打印从缓存获取的原始 error-collector 数据
                try {
                    String errorDetailJson = objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(errorDetail.get());
                    log.info("====== [ERROR-COLLECTOR-SSE] 从缓存获取的原始错误信息 (errorId={}) ======", errorId);
                    log.info("{}", errorDetailJson);
                    log.info("====== [ERROR-COLLECTOR-SSE] END ======");
                } catch (Exception e) {
                    log.warn("打印 errorDetail JSON 失败", e);
                }
                enrichRequestWithErrorDetail(request, errorDetail.get());
            } else {
                log.warn("Error detail not found in central store: errorId={}", errorId);
            }
        } else {
            log.warn("No errorId provided in request, cannot retrieve error detail from central store");
        }
        
        // 判断业务响应是否正常，正常则直接返回固定提示
        if (isBusinessSuccess(request)) {
            log.info("Business response is successful, returning fixed message");
            String successJson = "{\"success\":true,\"analysis\":{\"errorType\":\"无错误\",\"rootCause\":\"业务接口调用正常，没有错误信息\",\"suggestion\":\"无需处理\",\"confidence\":1.0,\"relatedApis\":[],\"nextSteps\":[]}}";
            return Flux.just(
                ServerSentEvent.<String>builder().data(successJson).build(),
                ServerSentEvent.<String>builder().event("done").data("[DONE]").build()
            );
        }
        
        // 提取定位信息
        String serviceName = getStringValue(request, "serviceName", "unknown");
        String docVersion = getStringValue(request, "docVersion", null);
        String path = getStringValue(request, "path", "");
        String method = getStringValue(request, "method", "GET");
        
        // 如果未指定版本，使用 "latest" 表示最新版本
        if (docVersion == null || docVersion.isEmpty()) {
            docVersion = "latest";
        }
        
        final String finalDocVersion = docVersion;
        final String finalServiceName = serviceName;
        final String finalPath = path;
        final String finalMethod = method;
        
        // 提取 ApiDefinition 后流式转发
        return apiDefinitionExtractor.extract(finalServiceName, finalDocVersion, finalPath, finalMethod)
            .flatMapMany(apiDefinition -> {
                enrichRequestWithApiDefinition(request, apiDefinition);
                log.debug("Enriched SSE request with ApiDefinition for: {} {} {}", 
                    finalServiceName, finalMethod, finalPath);
                return forwardSseToAiService("/api/ai/explain-error/stream", request);
            })
            .onErrorResume(e -> {
                log.warn("Error extracting ApiDefinition for SSE, forwarding without enrichment: {}", e.getMessage());
                return forwardSseToAiService("/api/ai/explain-error/stream", request);
            });
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Object>> health() {
        return webClient.get()
            .uri("/api/ai/health")
            .retrieve()
            .bodyToMono(String.class)
            .map(body -> ResponseEntity.ok((Object) Map.of("status", "UP", "aiService", "UP")))
            .timeout(properties.getTimeout())
            .onErrorResume(e -> {
                log.warn("AI service health check failed", e);
                return Mono.just(ResponseEntity.ok(
                    Map.of("status", "UP", "aiService", "DOWN", "error", e.getMessage())
                ));
            });
    }
    
    /**
     * 流式转发 SSE 请求到 AI 服务
     * 
     * 从 knife4j-ai-service 接收 SSE 流并透传给前端，
     * 实现真正的逐字流式输出效果。
     */
    private Flux<ServerSentEvent<String>> forwardSseToAiService(String uri, Map<String, Object> request) {
        return webClient.post()
            .uri(uri)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
            .doOnNext(event -> log.trace("SSE event received: {}", event.data()))
            .doOnComplete(() -> log.debug("SSE stream completed"))
            .doOnError(e -> log.error("SSE stream error", e))
            .timeout(properties.getTimeout())
            .onErrorResume(e -> {
                log.error("AI 服务调用失败: {}", e.getMessage());
                return createAiServiceErrorResponse(e);
            })
            .switchIfEmpty(createAiServiceErrorResponse(new RuntimeException("AI 服务返回空响应")));
    }
    
    /**
     * 创建 AI 服务不可用的错误响应
     * 
     * 返回一个格式化的 JSON 错误响应，前端可以正确解析并显示
     */
    private Flux<ServerSentEvent<String>> createAiServiceErrorResponse(Throwable e) {
        String errorMessage = e.getMessage();
        String userFriendlyMessage;
        
        // 根据错误类型生成友好提示
        if (errorMessage != null && (errorMessage.contains("Connection refused") 
                || errorMessage.contains("connection refused"))) {
            userFriendlyMessage = "AI 服务 (knife4j-ai-service) 连接失败，请检查服务是否已启动";
        } else if (errorMessage != null && errorMessage.contains("timeout")) {
            userFriendlyMessage = "AI 服务响应超时，请稍后重试或检查 ai-service 服务状态";
        } else if (errorMessage != null && errorMessage.contains("空响应")) {
            userFriendlyMessage = "AI 服务返回空响应，请检查 ai-service 服务是否正常运行";
        } else {
            userFriendlyMessage = "AI 服务不可用: " + (errorMessage != null ? errorMessage : "未知错误");
        }
        
        // 构建符合前端期望格式的错误响应
        String errorJson = String.format(
            "{\"success\":false,\"error\":true,\"analysis\":{" +
            "\"errorType\":\"AI 服务不可用\"," +
            "\"rootCause\":\"%s\"," +
            "\"suggestion\":\"请确保 knife4j-ai-service 服务已部署并正常运行。可以通过访问 /api/ai/health 检查服务状态。\"," +
            "\"confidence\":0," +
            "\"relatedApis\":[]," +
            "\"nextSteps\":[" +
            "\"检查 knife4j-ai-service 是否已启动\"," +
            "\"检查 application.yml 中 knife4j.ai.service-url 配置是否正确\"," +
            "\"查看 ai-service 服务日志排查问题\"" +
            "]}}",
            escapeJson(userFriendlyMessage)
        );
        
        return Flux.just(
            ServerSentEvent.<String>builder().data(errorJson).build(),
            ServerSentEvent.<String>builder().event("done").data("[DONE]").build()
        );
    }
    
    /**
     * 转义 JSON 字符串中的特殊字符
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * 将 ApiDefinition 附加到请求中
     */
    @SuppressWarnings("unchecked")
    private void enrichRequestWithApiDefinition(Map<String, Object> request, ApiDefinition apiDefinition) {
        try {
            // 将 ApiDefinition 转换为 Map
            Map<String, Object> apiDefMap = objectMapper.convertValue(apiDefinition, Map.class);
            request.put("apiDefinition", apiDefMap);
            
            // 如果请求中缺少某些字段，从 ApiDefinition 中补充
            if (!request.containsKey("summary") && apiDefinition.getSummary() != null) {
                request.put("summary", apiDefinition.getSummary());
            }
            if (!request.containsKey("description") && apiDefinition.getDescription() != null) {
                request.put("description", apiDefinition.getDescription());
            }
            if (!request.containsKey("contentType") && apiDefinition.getContentType() != null) {
                request.put("contentType", apiDefinition.getContentType());
            }
            
            // 如果请求中没有 requestBodySchema，使用提取的
            if (!request.containsKey("requestBodySchema") && apiDefinition.getRequestBodySchema() != null) {
                request.put("requestBodySchema", apiDefinition.getRequestBodySchema());
            }
            
            // 如果请求中没有 parameters，使用提取的
            if (!request.containsKey("parameters") && apiDefinition.getParameters() != null) {
                request.put("parameters", objectMapper.convertValue(
                    apiDefinition.getParameters(), java.util.List.class));
            }
            
        } catch (Exception e) {
            log.warn("Failed to enrich request with ApiDefinition", e);
        }
    }
    
    /**
     * 脱敏请求中的敏感信息
     */
    @SuppressWarnings("unchecked")
    private void sanitizeRequest(Map<String, Object> request) {
        // 处理 request.headers
        Object requestObj = request.get("request");
        if (requestObj instanceof Map) {
            Map<String, Object> requestMap = (Map<String, Object>) requestObj;
            Object headersObj = requestMap.get("headers");
            if (headersObj instanceof Map) {
                Map<String, Object> headers = (Map<String, Object>) headersObj;
                sanitizeHeaders(headers);
            }
        }
    }
    
    private void sanitizeHeaders(Map<String, Object> headers) {
        if (headers == null) return;
        
        for (String sensitiveHeader : properties.getSafety().getRedactHeaders()) {
            headers.entrySet().removeIf(entry -> 
                entry.getKey().toLowerCase().equals(sensitiveHeader));
        }
    }
    
    /**
     * 判断业务响应是否成功
     * 
     * 根据配置的 successField 和 successValue 判断响应体中的业务状态码
     * 支持数字和字符串的互相转换比较（如配置 "200" 匹配响应中的 200）
     */
    @SuppressWarnings("unchecked")
    private boolean isBusinessSuccess(Map<String, Object> request) {
        if (!properties.getBusinessResponse().isEnabled()) {
            log.debug("Business response check disabled");
            return false;
        }
        
        Object responseObj = request.get("response");
        if (responseObj == null) {
            log.debug("Business response check: request.response is null");
            return false;
        }
        if (!(responseObj instanceof Map)) {
            log.debug("Business response check: request.response is not a Map, actual type: {}", 
                responseObj.getClass().getName());
            return false;
        }
        
        Map<String, Object> response = (Map<String, Object>) responseObj;
        Object bodyObj = response.get("body");
        if (bodyObj == null) {
            log.debug("Business response check: response.body is null");
            return false;
        }
        if (!(bodyObj instanceof Map)) {
            log.debug("Business response check: response.body is not a Map, actual type: {}, value: {}", 
                bodyObj.getClass().getName(), truncate(String.valueOf(bodyObj), 200));
            return false;
        }
        
        Map<String, Object> body = (Map<String, Object>) bodyObj;
        String fieldName = properties.getBusinessResponse().getSuccessField();
        String expectedValue = properties.getBusinessResponse().getSuccessValue();
        
        Object actualValue = body.get(fieldName);
        if (actualValue == null) {
            log.debug("Business response check: body.{} is null, available keys: {}", 
                fieldName, body.keySet());
            return false;
        }
        
        // 支持数字和字符串的互相转换比较
        boolean isSuccess = isValueEqual(expectedValue, actualValue);
        log.info("Business response check: field={}, expected={}, actual={}, actualType={}, isSuccess={}", 
            fieldName, expectedValue, actualValue, actualValue.getClass().getSimpleName(), isSuccess);
        return isSuccess;
    }
    
    /**
     * 比较期望值和实际值是否相等
     * 支持数字和字符串的互相转换（如 "200" 等于 200）
     */
    private boolean isValueEqual(String expected, Object actual) {
        if (expected == null || actual == null) {
            return false;
        }
        
        String actualStr = String.valueOf(actual);
        
        // 直接字符串比较
        if (expected.equals(actualStr)) {
            return true;
        }
        
        // 尝试数值比较（处理 "200" vs 200 或 "200.0" vs 200 的情况）
        try {
            // 移除可能的小数点后的零（如 200.0 -> 200）
            double expectedNum = Double.parseDouble(expected);
            double actualNum;
            if (actual instanceof Number) {
                actualNum = ((Number) actual).doubleValue();
            } else {
                actualNum = Double.parseDouble(actualStr);
            }
            // 比较数值（整数比较）
            return (long) expectedNum == (long) actualNum;
        } catch (NumberFormatException e) {
            // 非数值类型，返回字符串比较结果
            return false;
        }
    }
    
    /**
     * 从 Map 中获取字符串值
     */
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }
    
    /**
     * 将错误详情附加到请求中，构建符合 AI 提示词期望的 _errorMeta 格式
     * 
     * 这样 AI 服务可以看到真正的错误信息，而不是固定的 {code:500, message:"系统异常"}
     */
    @SuppressWarnings("unchecked")
    private void enrichRequestWithErrorDetail(Map<String, Object> request, Map<String, Object> errorDetail) {
        try {
            // 构建符合 explain-error.system.txt 提示词期望的 _errorMeta 格式
            Map<String, Object> errorMeta = new java.util.LinkedHashMap<>();
            errorMeta.put("errorId", errorDetail.get("errorId"));
            errorMeta.put("traceId", errorDetail.get("traceId"));
            errorMeta.put("spanId", errorDetail.get("spanId"));
            errorMeta.put("serviceName", errorDetail.get("serviceName"));
            errorMeta.put("instanceId", errorDetail.get("instanceId"));
            errorMeta.put("exceptionClass", errorDetail.get("exceptionClass"));
            errorMeta.put("rootCauseClass", errorDetail.get("rootCauseClass"));
            errorMeta.put("message", errorDetail.get("originalMessage"));
            errorMeta.put("causeChain", errorDetail.get("causeChain"));
            errorMeta.put("stackFingerprint", errorDetail.get("stackFingerprint"));
            errorMeta.put("stackSnippet", errorDetail.get("filteredStackTrace"));
            errorMeta.put("timestamp", errorDetail.get("timestamp"));
            errorMeta.put("httpStatus", errorDetail.get("httpStatus"));
            errorMeta.put("requestPath", errorDetail.get("requestPath"));
            errorMeta.put("requestMethod", errorDetail.get("requestMethod"));
            errorMeta.put("targetController", errorDetail.get("targetController"));
            errorMeta.put("targetMethod", errorDetail.get("targetMethod"));
            errorMeta.put("targetMethodSignature", errorDetail.get("targetMethodSignature"));
            
            // 将 _errorMeta 直接附加到请求顶层
            request.put("_errorMeta", errorMeta);
            
            log.debug("Enriched request with _errorMeta: exceptionClass={}, message={}", 
                errorDetail.get("exceptionClass"), 
                truncate(String.valueOf(errorDetail.get("originalMessage")), 100));
            
        } catch (Exception e) {
            log.warn("Failed to enrich request with error detail", e);
        }
    }
    
    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }
}
