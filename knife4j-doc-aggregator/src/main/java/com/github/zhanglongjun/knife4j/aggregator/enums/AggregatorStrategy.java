/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.enums;

/**
 * 文档聚合策略
 */
public enum AggregatorStrategy {
    /**
     * 服务发现(自动聚合)
     */
    DISCOVER,
    /**
     * 手动配置路由
     */
    MANUAL
}

