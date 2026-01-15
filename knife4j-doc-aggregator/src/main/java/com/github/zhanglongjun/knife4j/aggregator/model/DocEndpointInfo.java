/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.model;

import com.github.zhanglongjun.knife4j.aggregator.enums.OpenApiVersion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档端点信息
 * 
 * 记录服务的 API 文档端点路径和 OpenAPI 版本
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocEndpointInfo {
    
    /**
     * 文档端点路径，如 /v3/api-docs 或 /v2/api-docs
     */
    private String docPath;
    
    /**
     * OpenAPI 规范版本
     */
    private OpenApiVersion version;
    
    /**
     * 创建 OpenAPI 3.0 端点信息
     */
    public static DocEndpointInfo openApi3(String docPath) {
        return new DocEndpointInfo(docPath, OpenApiVersion.OpenAPI3);
    }
    
    /**
     * 创建 Swagger 2.0 端点信息
     */
    public static DocEndpointInfo swagger2(String docPath) {
        return new DocEndpointInfo(docPath, OpenApiVersion.Swagger2);
    }
    
    /**
     * 默认 OpenAPI 3.0 端点
     */
    public static DocEndpointInfo defaultOpenApi3() {
        return new DocEndpointInfo("/v3/api-docs", OpenApiVersion.OpenAPI3);
    }
    
    /**
     * 默认 Swagger 2.0 端点
     */
    public static DocEndpointInfo defaultSwagger2() {
        return new DocEndpointInfo("/v2/api-docs", OpenApiVersion.Swagger2);
    }
    
    /**
     * 是否为 Swagger 2.0
     */
    public boolean isSwagger2() {
        return OpenApiVersion.Swagger2.equals(version);
    }
    
    /**
     * 是否为 OpenAPI 3.0
     */
    public boolean isOpenApi3() {
        return OpenApiVersion.OpenAPI3.equals(version);
    }
}

