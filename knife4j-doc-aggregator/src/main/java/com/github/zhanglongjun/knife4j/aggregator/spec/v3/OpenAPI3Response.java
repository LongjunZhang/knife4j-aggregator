/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.spec.v3;

import java.io.Serializable;
import java.util.List;

/**
 * OpenAPI 3 响应对象
 */
public class OpenAPI3Response implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * ConfigUrl，eg: /v3/api-docs/swagger-config
     */
    private String configUrl;
    
    /**
     * oauth2RedirectUrl
     */
    private String oauth2RedirectUrl;
    
    /**
     * operation接口排序规则
     */
    private String operationsSorter = "alpha";
    
    /**
     * tag排序规则
     */
    private String tagsSorter = "alpha";
    
    /**
     * group
     */
    @SuppressWarnings("java:S1948")
    private List<?> urls;
    
    /**
     * validatorUrl
     */
    private String validatorUrl;
    
    // Getters and Setters
    
    public String getConfigUrl() {
        return configUrl;
    }
    
    public void setConfigUrl(String configUrl) {
        this.configUrl = configUrl;
    }
    
    public String getOauth2RedirectUrl() {
        return oauth2RedirectUrl;
    }
    
    public void setOauth2RedirectUrl(String oauth2RedirectUrl) {
        this.oauth2RedirectUrl = oauth2RedirectUrl;
    }
    
    public String getOperationsSorter() {
        return operationsSorter;
    }
    
    public void setOperationsSorter(String operationsSorter) {
        this.operationsSorter = operationsSorter;
    }
    
    public String getTagsSorter() {
        return tagsSorter;
    }
    
    public void setTagsSorter(String tagsSorter) {
        this.tagsSorter = tagsSorter;
    }
    
    public List<?> getUrls() {
        return urls;
    }
    
    public void setUrls(List<?> urls) {
        this.urls = urls;
    }
    
    public String getValidatorUrl() {
        return validatorUrl;
    }
    
    public void setValidatorUrl(String validatorUrl) {
        this.validatorUrl = validatorUrl;
    }
}

