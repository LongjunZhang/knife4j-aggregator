package com.github.zhanglongjun.knife4j.error.extractor;

import com.github.zhanglongjun.knife4j.error.config.ErrorCollectorProperties;
import com.github.zhanglongjun.knife4j.error.model.CauseChainItem;
import com.github.zhanglongjun.knife4j.error.model.ErrorDetail;
import com.github.zhanglongjun.knife4j.error.model.ErrorMetadata;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ErrorMetadata 构建器
 * 负责从异常构建结构化的错误元数据
 */
@Slf4j
public class ErrorMetadataBuilder {

    private final ErrorCollectorProperties properties;
    private final StackExtractor stackExtractor;

    public ErrorMetadataBuilder(ErrorCollectorProperties properties, StackExtractor stackExtractor) {
        this.properties = properties;
        this.stackExtractor = stackExtractor;
    }

    /**
     * 从异常构建完整的 ErrorDetail
     */
    public ErrorDetail buildErrorDetail(
            Throwable throwable,
            String serviceName,
            String instanceId,
            String traceId,
            String spanId,
            Integer httpStatus,
            String requestPath,
            String requestMethod,
            Map<String, String> requestHeaders,
            Map<String, String> requestParams,
            String requestBodySummary) {
        return buildErrorDetail(throwable, serviceName, instanceId, traceId, spanId,
                httpStatus, requestPath, requestMethod, requestHeaders, requestParams,
                requestBodySummary, null, null, null);
    }

    /**
     * 从异常构建完整的 ErrorDetail（包含目标处理器信息）
     */
    public ErrorDetail buildErrorDetail(
            Throwable throwable,
            String serviceName,
            String instanceId,
            String traceId,
            String spanId,
            Integer httpStatus,
            String requestPath,
            String requestMethod,
            Map<String, String> requestHeaders,
            Map<String, String> requestParams,
            String requestBodySummary,
            String targetController,
            String targetMethod,
            String targetMethodSignature) {

        String errorId = generateErrorId();
        Throwable rootCause = stackExtractor.getRootCause(throwable);
        List<CauseChainItem> causeChain = stackExtractor.extractCauseChain(throwable);

        return ErrorDetail.builder()
                .errorId(errorId)
                .traceId(traceId)
                .spanId(spanId)
                .serviceName(serviceName)
                .instanceId(instanceId)
                .exceptionClass(throwable.getClass().getName())
                .rootCauseClass(rootCause.getClass().getName())
                .originalMessage(throwable.getMessage())
                .maskedMessage(stackExtractor.maskSensitiveData(throwable.getMessage()))
                .causeChain(causeChain)
                .fullStackTrace(stackExtractor.getFullStackTrace(throwable))
                .filteredStackTrace(stackExtractor.getFilteredStackTrace(throwable))
                .stackFingerprint(stackExtractor.generateStackFingerprint(throwable))
                .timestamp(Instant.now())
                .expiresAt(Instant.now().plusSeconds(properties.getTtlMinutes() * 60L))
                .httpStatus(httpStatus)
                .requestPath(requestPath)
                .requestMethod(requestMethod)
                .requestHeaders(maskHeaders(requestHeaders))
                .requestParams(maskParams(requestParams))
                .requestBodySummary(stackExtractor.maskSensitiveData(requestBodySummary))
                .targetController(targetController)
                .targetMethod(targetMethod)
                .targetMethodSignature(targetMethodSignature)
                .build();
    }

    /**
     * 从 ErrorDetail 构建返回给调用方的 ErrorMetadata
     */
    public ErrorMetadata buildMetadata(ErrorDetail detail) {
        String stackSnippet = properties.isIncludeStackSnippet()
                ? detail.getFilteredStackTrace()
                : null;

        return detail.toMetadata(stackSnippet);
    }

    /**
     * 生成唯一的错误 ID
     */
    private String generateErrorId() {
        return "err_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 脱敏请求头
     */
    private Map<String, String> maskHeaders(Map<String, String> headers) {
        if (headers == null) {
            return null;
        }

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (isSensitiveHeader(key)) {
                entry.setValue(properties.getMaskReplacement());
            }
        }
        return headers;
    }

    private boolean isSensitiveHeader(String headerName) {
        return headerName.contains("authorization")
                || headerName.contains("cookie")
                || headerName.contains("token")
                || headerName.contains("secret")
                || headerName.contains("api-key")
                || headerName.contains("x-api-key");
    }

    /**
     * 脱敏请求参数
     */
    private Map<String, String> maskParams(Map<String, String> params) {
        if (params == null) {
            return null;
        }

        for (Map.Entry<String, String> entry : params.entrySet()) {
            entry.setValue(stackExtractor.maskSensitiveData(entry.getValue()));
        }
        return params;
    }

}

