package com.github.zhanglongjun.knife4j.error.jakarta.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zhanglongjun.knife4j.error.config.ErrorCollectorProperties;
import com.github.zhanglongjun.knife4j.error.extractor.ErrorMetadataBuilder;
import com.github.zhanglongjun.knife4j.error.jakarta.context.ErrorCollectorContext;
import com.github.zhanglongjun.knife4j.error.model.CauseChainItem;
import com.github.zhanglongjun.knife4j.error.model.ErrorDetail;
import com.github.zhanglongjun.knife4j.error.store.ErrorDetailStore;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

/**
 * 全局异常处理器 (Jakarta 版本)
 * 捕获 Controller 层抛出的异常，收集错误信息
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private final ErrorCollectorProperties properties;
    private final ErrorMetadataBuilder metadataBuilder;
    private final ErrorDetailStore errorStore;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:unknown}")
    private String serviceName;

    @Value("${spring.cloud.client.ip-address:unknown}:${server.port:8080}")
    private String instanceId;

    public GlobalExceptionHandler(
            ErrorCollectorProperties properties,
            ErrorMetadataBuilder metadataBuilder,
            ErrorDetailStore errorStore) {
        this.properties = properties;
        this.metadataBuilder = metadataBuilder;
        this.errorStore = errorStore;
        this.objectMapper = new ObjectMapper();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(
            Exception ex,
            HttpServletRequest request,
            HandlerMethod handlerMethod) {
        
        // 提取目标处理器信息
        String targetController = null;
        String targetMethod = null;
        String targetMethodSignature = null;
        
        if (handlerMethod != null) {
            targetController = handlerMethod.getBeanType().getName();
            targetMethod = handlerMethod.getMethod().getName();
            targetMethodSignature = buildMethodSignature(handlerMethod);
        }

        // 增强日志输出
        if (properties.isDebugLog()) {
            log.info("[ErrorCollector] ==========================================");
            log.info("[ErrorCollector] 异常捕获: {}", ex.getClass().getSimpleName());
            log.info("[ErrorCollector] 异常消息: {}", ex.getMessage());
            log.info("[ErrorCollector] 请求路径: {} {}", request.getMethod(), request.getRequestURI());
            if (targetController != null) {
                log.info("[ErrorCollector] 目标处理器: {}.{}()", targetController, targetMethod);
                log.info("[ErrorCollector] 方法签名: {}", targetMethodSignature);
            }
            log.info("[ErrorCollector] 错误收集已启用: {}", properties.isEnabled());
            log.info("[ErrorCollector] ==========================================");
        }

        // 基础错误日志（包含完整堆栈）
        log.error("[ErrorCollector] Exception caught: {} | 请求: {} {} | 目标: {}\n[ErrorCollector] 完整堆栈信息:",
                ex.getClass().getSimpleName(),
                request.getMethod(),
                request.getRequestURI(),
                targetMethodSignature != null ? targetMethodSignature : "unknown",
                ex);  // 传入异常对象以打印完整堆栈

        // 构建干净的响应体（不包含错误详情）
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("message", "系统异常");
        response.put("data", null);

        // 准备响应头
        HttpHeaders headers = new HttpHeaders();
        
        // 只有在启用错误收集时才收集（开箱即用，异常发生即收集）
        if (properties.isEnabled()) {
            try {
                ErrorDetail detail = collectError(ex, request, 
                        targetController, targetMethod, targetMethodSignature);
                
                // 通过响应头返回 errorId，不在响应体中暴露
                headers.set(properties.getErrorIdHeaderName(), detail.getErrorId());
                
                // 响应头传输模式：将 ErrorDetail 编码后放入响应头，不存储到本地
                if (properties.isHeaderTransferEnabled()) {
                    String encodedDetail = encodeErrorDetail(detail);
                    if (encodedDetail != null) {
                        headers.set(properties.getErrorDetailHeaderName(), encodedDetail);
                        if (properties.isDebugLog()) {
                            log.info("[ErrorCollector] 响应头传输模式已启用，ErrorDetail 编码后放入响应头");
                            log.info("[ErrorCollector]   - 响应头名称: {}", properties.getErrorDetailHeaderName());
                            log.info("[ErrorCollector]   - 编码后长度: {} bytes", encodedDetail.length());
                        }
                    }
                } else {
                    // 传统模式：存储到本地
                    errorStore.store(detail);
                    if (properties.isDebugLog()) {
                        log.info("[ErrorCollector] 传统模式：ErrorDetail 存储到本地内存");
                    }
                }
                
                if (properties.isDebugLog()) {
                    log.info("[ErrorCollector] 错误元数据已收集:");
                    log.info("[ErrorCollector]   - errorId: {} (通过响应头 {} 返回)", 
                            detail.getErrorId(), properties.getErrorIdHeaderName());
                    log.info("[ErrorCollector]   - exceptionClass: {}", detail.getExceptionClass());
                    log.info("[ErrorCollector]   - rootCauseClass: {}", detail.getRootCauseClass());
                    log.info("[ErrorCollector]   - originalMessage: {}", detail.getOriginalMessage());
                    log.info("[ErrorCollector]   - targetController: {}", detail.getTargetController());
                    log.info("[ErrorCollector]   - targetMethod: {}", detail.getTargetMethod());
                }
            } catch (Exception e) {
                log.error("[ErrorCollector] Failed to collect error metadata", e);
            }
        } else {
            if (properties.isDebugLog()) {
                log.info("[ErrorCollector] 跳过错误收集 - enabled={}", properties.isEnabled());
            }
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .headers(headers)
                .body(response);
    }

    /**
     * 构建方法签名字符串
     * 例如: testArithmeticException(Integer divisor)
     */
    private String buildMethodSignature(HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();
        String methodName = method.getName();
        
        String params = Stream.of(method.getParameters())
                .map(this::formatParameter)
                .collect(Collectors.joining(", "));
        
        return methodName + "(" + params + ")";
    }

    /**
     * 格式化参数为 "类型 参数名" 格式
     */
    private String formatParameter(Parameter param) {
        String typeName = param.getType().getSimpleName();
        String paramName = param.getName();
        return typeName + " " + paramName;
    }

    /**
     * 收集错误信息，构建 ErrorDetail
     */
    private ErrorDetail collectError(
            Exception ex, 
            HttpServletRequest request,
            String targetController,
            String targetMethod,
            String targetMethodSignature) {
        
        // 构建 ErrorDetail（包含目标处理器信息）
        ErrorDetail detail = metadataBuilder.buildErrorDetail(
                ex,
                serviceName,
                instanceId,
                ErrorCollectorContext.getTraceId(),
                ErrorCollectorContext.getSpanId(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getRequestURI(),
                request.getMethod(),
                extractHeaders(request),
                extractParams(request),
                null, // requestBodySummary - 需要额外处理
                targetController,
                targetMethod,
                targetMethodSignature
        );

        // 打印收集到的完整错误对象用于调试
        if (properties.isDebugLog()) {
            log.info("====== [DEBUG] ErrorCollector 收集到的错误对象 ======");
            log.info("[DEBUG] errorId: {}", detail.getErrorId());
            log.info("[DEBUG] serviceName: {}", detail.getServiceName());
            log.info("[DEBUG] exceptionClass: {}", detail.getExceptionClass());
            log.info("[DEBUG] rootCauseClass: {}", detail.getRootCauseClass());
            log.info("[DEBUG] originalMessage: {}", detail.getOriginalMessage());
            log.info("[DEBUG] targetMethodSignature: {}", detail.getTargetMethodSignature());
            log.info("====== [DEBUG] ErrorCollector END ======");
        }

        return detail;
    }

    /**
     * 将 ErrorDetail 编码为响应头可传输的格式
     * 格式：JSON -> GZIP -> Base64
     * 
     * @param detail 错误详情
     * @return Base64 编码后的字符串，如果编码失败返回 null
     */
    private String encodeErrorDetail(ErrorDetail detail) {
        try {
            // 创建精简版，只保留 AI 分析需要的关键字段
            Map<String, Object> simplified = new LinkedHashMap<>();
            simplified.put("errorId", detail.getErrorId());
            simplified.put("serviceName", detail.getServiceName());
            simplified.put("exceptionClass", detail.getExceptionClass());
            simplified.put("rootCauseClass", detail.getRootCauseClass());
            simplified.put("originalMessage", detail.getOriginalMessage());
            simplified.put("maskedMessage", detail.getMaskedMessage());
            simplified.put("httpStatus", detail.getHttpStatus());
            simplified.put("requestPath", detail.getRequestPath());
            simplified.put("requestMethod", detail.getRequestMethod());
            simplified.put("targetController", detail.getTargetController());
            simplified.put("targetMethod", detail.getTargetMethod());
            simplified.put("targetMethodSignature", detail.getTargetMethodSignature());
            
            // 精简 causeChain
            if (detail.getCauseChain() != null && !detail.getCauseChain().isEmpty()) {
                List<Map<String, String>> simplifiedChain = new ArrayList<>();
                for (CauseChainItem item : detail.getCauseChain()) {
                    Map<String, String> chainItem = new LinkedHashMap<>();
                    chainItem.put("exceptionClass", item.getExceptionClass());
                    chainItem.put("message", truncate(item.getMessage(), 200));
                    simplifiedChain.add(chainItem);
                }
                simplified.put("causeChain", simplifiedChain);
            }
            
            // 截断 stackTrace
            String stackTrace = detail.getFilteredStackTrace();
            if (stackTrace != null) {
                simplified.put("filteredStackTrace", truncate(stackTrace, 2000));
            }
            
            // JSON 序列化
            String json = objectMapper.writeValueAsString(simplified);
            
            // GZIP 压缩
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipOs = new GZIPOutputStream(baos)) {
                gzipOs.write(json.getBytes(StandardCharsets.UTF_8));
            }
            byte[] compressed = baos.toByteArray();
            
            // Base64 编码
            String encoded = Base64.getEncoder().encodeToString(compressed);
            
            // 检查长度限制
            if (encoded.length() > properties.getErrorDetailHeaderMaxLength()) {
                log.warn("[ErrorCollector] ErrorDetail 编码后超过最大长度限制 ({} > {})，尝试截断 stackTrace",
                        encoded.length(), properties.getErrorDetailHeaderMaxLength());
                // 移除 stackTrace 后重试
                simplified.remove("filteredStackTrace");
                json = objectMapper.writeValueAsString(simplified);
                baos = new ByteArrayOutputStream();
                try (GZIPOutputStream gzipOs = new GZIPOutputStream(baos)) {
                    gzipOs.write(json.getBytes(StandardCharsets.UTF_8));
                }
                encoded = Base64.getEncoder().encodeToString(baos.toByteArray());
            }
            
            return encoded;
            
        } catch (Exception e) {
            log.error("[ErrorCollector] 编码 ErrorDetail 失败", e);
            return null;
        }
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...[truncated]";
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            // 跳过敏感头和大型头
            if (!isSensitiveHeader(name) && !isLargeHeader(name)) {
                headers.put(name, request.getHeader(name));
            }
        }
        return headers;
    }

    private boolean isSensitiveHeader(String name) {
        String lower = name.toLowerCase();
        return lower.contains("authorization")
                || lower.contains("cookie")
                || lower.contains("token");
    }

    private boolean isLargeHeader(String name) {
        String lower = name.toLowerCase();
        return lower.equals("accept")
                || lower.equals("accept-encoding")
                || lower.equals("accept-language")
                || lower.equals("user-agent");
    }

    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return params;
    }

}
