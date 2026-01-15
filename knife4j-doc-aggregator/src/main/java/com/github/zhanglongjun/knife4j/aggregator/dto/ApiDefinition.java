/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * API 最小定义 DTO
 * 
 * 用于从 OpenAPI 文档中提取单个接口的最小定义，
 * 避免前端上传整份 OpenAPI，保证请求轻量且可控。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiDefinition {
    
    /**
     * 服务名称
     */
    private String serviceName;
    
    /**
     * 文档版本
     */
    private String docVersion;
    
    /**
     * HTTP 方法（GET/POST/PUT/DELETE 等）
     */
    private String method;
    
    /**
     * 接口路径
     */
    private String path;
    
    /**
     * 接口摘要
     */
    private String summary;
    
    /**
     * 接口描述
     */
    private String description;
    
    /**
     * Content-Type
     */
    private String contentType;
    
    /**
     * 参数列表（path/query/header）
     */
    private List<ParameterDef> parameters;
    
    /**
     * 请求体 Schema
     */
    private Map<String, Object> requestBodySchema;
    
    /**
     * 响应定义
     */
    private Map<String, Object> responses;
    
    /**
     * 参数定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParameterDef {
        
        /**
         * 参数位置：path, query, header
         */
        private String in;
        
        /**
         * 参数名称
         */
        private String name;
        
        /**
         * 是否必填
         */
        private boolean required;
        
        /**
         * 参数 Schema
         */
        private Map<String, Object> schema;
        
        /**
         * 参数描述
         */
        private String description;
    }
}

