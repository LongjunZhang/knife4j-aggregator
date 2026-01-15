package com.github.zhanglongjun.knife4j.ai.dto;

import lombok.Data;

import java.util.Map;

/**
 * 解释错误请求 DTO
 */
@Data
public class ExplainErrorRequest {
    
    /**
     * 服务名称
     */
    private String serviceName;
    
    /**
     * 文档版本
     */
    private String docVersion;
    
    /**
     * 接口路径
     */
    private String path;
    
    /**
     * HTTP 方法
     */
    private String method;
    
    /**
     * 接口摘要
     */
    private String summary;
    
    /**
     * 请求信息
     */
    private RequestInfo request;
    
    /**
     * 响应信息
     */
    private ResponseInfo response;
    
    /**
     * 网关上下文（可选）
     */
    private GatewayRef gatewayRef;
    
    /**
     * API 定义（由 doc-aggregator 从 OpenAPI 提取）
     */
    private ApiDefinition apiDefinition;
    
    /**
     * error-collector 收集的原始错误元数据（由 doc-aggregator 从缓存获取并附加）
     * 格式符合 explain-error.system.txt 提示词期望
     */
    private Map<String, Object> _errorMeta;
    
    @Data
    public static class RequestInfo {
        /**
         * 请求 URL
         */
        private String url;
        
        /**
         * Content-Type
         */
        private String contentType;
        
        /**
         * 请求头（已脱敏）
         */
        private Map<String, String> headers;
        
        /**
         * 查询参数
         */
        private Map<String, Object> query;
        
        /**
         * 请求体
         */
        private Object body;
    }
    
    @Data
    public static class ResponseInfo {
        /**
         * 状态码
         */
        private int status;
        
        /**
         * 响应头
         */
        private Map<String, String> headers;
        
        /**
         * 响应体
         */
        private Object body;
    }
    
    @Data
    public static class GatewayRef {
        /**
         * 网关请求ID
         */
        private String requestId;
    }
}





