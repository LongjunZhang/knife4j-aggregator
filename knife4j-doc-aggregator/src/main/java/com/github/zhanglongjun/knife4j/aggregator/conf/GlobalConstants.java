/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.conf;

import java.time.Duration;

/**
 * 全局常量定义
 */
public class GlobalConstants {
    
    /**
     * 默认阻塞时间
     */
    public static Duration DEFAULT_BLOCK_TIME = Duration.ofSeconds(5L);
    
    public static final String DEFAULT_API_PATH_PREFIX = "/";
    
    /**
     * 默认排序值
     */
    public static final Integer DEFAULT_ORDER = 0;
    
    /**
     * swagger2接口默认待拼接的地址
     */
    public static final String DEFAULT_SWAGGER2_APPEND_PATH = "/v2/api-docs?group=";
    
    /**
     * 默认分组名称
     */
    public static final String DEFAULT_GROUP_NAME = "default";
    
    @SuppressWarnings("java:S1075")
    public static final String DEFAULT_OPEN_API_V2_PATH = "/v2/api-docs?group=default";
    
    @SuppressWarnings("java:S1075")
    public static final String DEFAULT_OPEN_API_V3_PATH = "/v3/api-docs";
    
    /**
     * basic auth验证
     */
    public static final String KNIFE4J_BASIC_AUTH_SESSION = "KNIFE4J_BASIC_AUTH_SESSION";
    
    /**
     * 校验Basic请求头
     */
    public static final String AUTH_HEADER_NAME = "Authorization";
    
    /**
     * HTTP Schema
     */
    public static final String PROTOCOL_HTTP = "http://";
    
    /**
     * HTTPS Schema
     */
    public static final String PROTOCOL_HTTPS = "https://";
    
    /**
     * 空字符串
     */
    public static final String EMPTY_STR = "";
    
    /**
     * Knife4j provider default username with basic auth.
     */
    public static final String BASIC_DEFAULT_USERNAME = "admin";
    
    /**
     * Knife4j provider default password with basic auth.
     */
    public static final String BASIC_DEFAULT_PASSWORD = "123321";
    
    /**
     * Cors max_age
     */
    public static final Long CORS_MAX_AGE = 10000L;
    
    /**
     * 响应HTTP状态码
     */
    public static final Integer BASIC_SECURITY_RESPONSE_CODE = 401;
    
    /**
     * 生产环境屏蔽后自定义响应HTTP状态码
     */
    public static final Integer FORBIDDEN_CUSTOM_CODE = 200;
    
    private GlobalConstants() {
    }
}

