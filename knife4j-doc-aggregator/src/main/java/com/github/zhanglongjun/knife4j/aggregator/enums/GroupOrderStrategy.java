/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.enums;

/**
 * 排序规则
 */
public enum GroupOrderStrategy {
    
    /**
     * 默认排序规则，官方swagger-ui默认实现
     */
    alpha,
    /**
     * Knife4j提供的增强排序规则，开发者可扩展x-order，根据数值来自定义排序
     */
    order
}

