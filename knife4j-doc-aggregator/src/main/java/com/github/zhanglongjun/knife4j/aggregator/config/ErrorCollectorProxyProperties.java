package com.github.zhanglongjun.knife4j.aggregator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 错误收集器代理配置
 * 开箱即用模式：无需配置 Token，自动拦截错误响应
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "knife4j.error-collector-proxy")
public class ErrorCollectorProxyProperties {

    /**
     * 是否启用错误收集代理
     */
    private boolean enabled = true;

    /**
     * 响应头名称（ErrorId）
     * 微服务通过此响应头返回 errorId
     */
    private String errorIdHeaderName = "X-Error-Id";

    /**
     * 请求超时时间
     */
    private Duration timeout = Duration.ofSeconds(30);

    /**
     * 内部接口路径前缀
     */
    private String internalApiPrefix = "/internal/error-collector";

    /**
     * 请求上下文缓存 TTL（分钟）
     */
    private int requestCacheTtlMinutes = 30;

    /**
     * 最大缓存请求数量
     */
    private int maxCachedRequests = 1000;

    // ===================== 集中存储配置 =====================

    /**
     * 集中存储 TTL（小时），默认 24 小时
     */
    private int centralStoreTtlHours = 24;

    /**
     * 最大存储的错误数量
     */
    private int maxCentralStoredErrors = 50000;

    /**
     * 清理过期数据的间隔（分钟）
     */
    private int cleanupIntervalMinutes = 5;

    /**
     * 响应头名称：携带编码后的错误详情
     * 业务服务通过此响应头返回编码后的 ErrorDetail
     */
    private String errorDetailHeaderName = "X-Error-Detail";

    /**
     * 错误详情响应头的最大长度（字节）
     * 超过此长度会截断 stackTrace
     */
    private int errorDetailHeaderMaxLength = 8192;

}


