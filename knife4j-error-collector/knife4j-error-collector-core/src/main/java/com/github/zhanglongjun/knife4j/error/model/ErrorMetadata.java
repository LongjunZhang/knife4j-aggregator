package com.github.zhanglongjun.knife4j.error.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * 结构化错误元数据
 * 返回给 doc-aggregator 的精简错误信息，不包含完整堆栈
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 唯一错误 ID，用于后续查询完整堆栈
     */
    private String errorId;

    /**
     * 链路追踪 ID（可选，如果有集成 Sleuth/Micrometer）
     */
    private String traceId;

    /**
     * Span ID（可选）
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
     * 脱敏后的异常消息
     */
    private String message;

    /**
     * 异常链（最多 N 层，由配置控制）
     */
    private List<CauseChainItem> causeChain;

    /**
     * 堆栈指纹，用于聚合相同类型的错误
     * 基于异常类 + 关键栈帧生成的 hash
     */
    private String stackFingerprint;

    /**
     * 精简堆栈片段（可选）
     * 只包含业务包名范围内的栈帧，限长限帧数
     */
    private String stackSnippet;

    /**
     * 错误发生时间戳
     */
    private Instant timestamp;

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

}

