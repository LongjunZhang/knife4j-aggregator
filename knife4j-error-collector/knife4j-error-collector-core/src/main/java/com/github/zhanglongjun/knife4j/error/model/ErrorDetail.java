package com.github.zhanglongjun.knife4j.error.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 完整错误详情
 * 存储在内存中，通过 errorId 查询获取
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 唯一错误 ID
     */
    private String errorId;

    /**
     * 链路追踪 ID
     */
    private String traceId;

    /**
     * Span ID
     */
    private String spanId;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务实例 ID
     */
    private String instanceId;

    /**
     * 顶层异常类名
     */
    private String exceptionClass;

    /**
     * 根因异常类名
     */
    private String rootCauseClass;

    /**
     * 原始异常消息（未脱敏，仅内部使用）
     */
    private String originalMessage;

    /**
     * 脱敏后的异常消息
     */
    private String maskedMessage;

    /**
     * 完整异常链
     */
    private List<CauseChainItem> causeChain;

    /**
     * 完整堆栈跟踪字符串
     */
    private String fullStackTrace;

    /**
     * 过滤后的堆栈跟踪（只包含业务包）
     */
    private String filteredStackTrace;

    /**
     * 堆栈指纹
     */
    private String stackFingerprint;

    /**
     * 错误发生时间
     */
    private Instant timestamp;

    /**
     * 过期时间（TTL）
     */
    private Instant expiresAt;

    /**
     * HTTP 状态码
     */
    private Integer httpStatus;

    /**
     * 请求路径
     */
    private String requestPath;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求头（脱敏后）
     */
    private Map<String, String> requestHeaders;

    /**
     * 请求参数（脱敏后）
     */
    private Map<String, String> requestParams;

    /**
     * 请求体摘要（脱敏后，限长）
     */
    private String requestBodySummary;

    /**
     * 目标 Controller 类全名
     */
    private String targetController;

    /**
     * 目标方法名
     */
    private String targetMethod;

    /**
     * 目标方法签名 (如: testArithmeticException(Integer divisor))
     */
    private String targetMethodSignature;

    /**
     * 转换为精简的 ErrorMetadata
     */
    public ErrorMetadata toMetadata(String stackSnippet) {
        return ErrorMetadata.builder()
                .errorId(this.errorId)
                .traceId(this.traceId)
                .spanId(this.spanId)
                .serviceName(this.serviceName)
                .instanceId(this.instanceId)
                .exceptionClass(this.exceptionClass)
                .rootCauseClass(this.rootCauseClass)
                .message(this.maskedMessage)
                .causeChain(this.causeChain)
                .stackFingerprint(this.stackFingerprint)
                .stackSnippet(stackSnippet)
                .timestamp(this.timestamp)
                .httpStatus(this.httpStatus)
                .requestPath(this.requestPath)
                .requestMethod(this.requestMethod)
                .targetController(this.targetController)
                .targetMethod(this.targetMethod)
                .targetMethodSignature(this.targetMethodSignature)
                .build();
    }

}

