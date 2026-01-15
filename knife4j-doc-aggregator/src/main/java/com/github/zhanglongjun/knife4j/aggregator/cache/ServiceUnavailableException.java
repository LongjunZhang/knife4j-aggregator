/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.cache;

/**
 * 服务不可用异常
 * 
 * 当服务不可用且没有可用缓存时抛出
 */
public class ServiceUnavailableException extends RuntimeException {
    
    private final String serviceName;
    
    public ServiceUnavailableException(String serviceName) {
        super("服务不可用: " + serviceName);
        this.serviceName = serviceName;
    }
    
    public ServiceUnavailableException(String serviceName, Throwable cause) {
        super("服务不可用: " + serviceName, cause);
        this.serviceName = serviceName;
    }
    
    public String getServiceName() {
        return serviceName;
    }
}

