/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 文档聚合服务启动类
 * 
 * 功能：
 * 1. 从 Nacos 自动发现微服务
 * 2. 提供 swagger-config 接口供 UI 获取服务列表
 * 3. 代理微服务的 OpenAPI 文档请求
 * 4. 文档缓存与离线访问支持
 * 5. 托管 Knife4j UI 静态资源
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class DocAggregatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocAggregatorApplication.class, args);
    }
}

