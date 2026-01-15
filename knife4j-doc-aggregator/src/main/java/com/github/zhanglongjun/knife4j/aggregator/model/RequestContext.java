package com.github.zhanglongjun.knife4j.aggregator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * 请求上下文
 * 缓存接口调用的请求和响应信息，用于 AI 分析
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestContext {

    /**
     * 唯一请求 ID
     */
    private String requestId;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 请求路径
     */
    private String path;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 请求头（脱敏后）
     */
    private Map<String, String> requestHeaders;

    /**
     * 请求参数
     */
    private Map<String, String> requestParams;

    /**
     * 请求体（脱敏后，限长）
     */
    private String requestBody;

    /**
     * 响应状态码
     */
    private Integer responseStatus;

    /**
     * 响应体（限长）
     */
    private String responseBody;

    /**
     * 是否发生错误
     */
    private boolean hasError;

    /**
     * 错误 ID（用于获取完整错误详情）
     */
    private String errorId;

    /**
     * 错误摘要（从 _errorMeta 提取）
     */
    private Map<String, Object> errorMeta;

    /**
     * 请求时间
     */
    private Instant timestamp;

    /**
     * 过期时间
     */
    private Instant expiresAt;

}

