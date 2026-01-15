/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.service;

import com.github.zhanglongjun.knife4j.aggregator.config.Knife4jAggregatorProperties;
import com.github.zhanglongjun.knife4j.aggregator.enums.OpenApiVersion;
import com.github.zhanglongjun.knife4j.aggregator.model.DocEndpointInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档端点探测服务
 * 
 * 功能：
 * 1. 自动探测服务的 API 文档端点（优先 /v3/api-docs，回退 /v2/api-docs）
 * 2. 支持手动配置覆盖自动探测结果
 * 3. 缓存探测结果避免重复探测
 */
@Service
public class DocEndpointProbeService {
    
    private static final Logger log = LoggerFactory.getLogger(DocEndpointProbeService.class);
    
    /** OpenAPI 3 默认端点 */
    private static final String OAS3_ENDPOINT = "/v3/api-docs";
    
    /** Swagger 2 默认端点 */
    private static final String SWAGGER2_ENDPOINT = "/v2/api-docs";
    
    private final Knife4jAggregatorProperties properties;
    private final WebClient webClient;
    
    /** 端点信息缓存：serviceName -> DocEndpointInfo */
    private final Map<String, DocEndpointInfo> endpointCache = new ConcurrentHashMap<>();
    
    public DocEndpointProbeService(Knife4jAggregatorProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder.build();
        log.info("DocEndpointProbeService 已初始化，自动探测: {}", properties.getDiscover().isAutoProbeEndpoint());
    }
    
    /**
     * 获取服务的文档端点信息
     * 
     * 优先级：
     * 1. 手动配置（serviceEndpoints）
     * 2. 缓存的探测结果
     * 3. 默认配置（docPath）
     * 
     * @param serviceName 服务名
     * @return 文档端点信息
     */
    public DocEndpointInfo getEndpointInfo(String serviceName) {
        // 1. 优先使用手动配置
        String configuredEndpoint = properties.getDiscover().getServiceEndpoint(serviceName);
        if (configuredEndpoint != null && !configuredEndpoint.isEmpty()) {
            OpenApiVersion version = inferVersionFromPath(configuredEndpoint);
            log.debug("服务 {} 使用手动配置的端点: {} ({})", serviceName, configuredEndpoint, version);
            return new DocEndpointInfo(configuredEndpoint, version);
        }
        
        // 2. 检查缓存
        DocEndpointInfo cached = endpointCache.get(serviceName);
        if (cached != null) {
            log.debug("服务 {} 使用缓存的端点: {} ({})", serviceName, cached.getDocPath(), cached.getVersion());
            return cached;
        }
        
        // 3. 返回默认配置
        String defaultPath = properties.getDiscover().getDocPath();
        OpenApiVersion defaultVersion = properties.getDiscover().getVersion();
        log.debug("服务 {} 使用默认端点: {} ({})", serviceName, defaultPath, defaultVersion);
        return new DocEndpointInfo(defaultPath, defaultVersion);
    }
    
    /**
     * 探测服务的文档端点
     * 
     * 探测顺序：
     * 1. 尝试 /v3/api-docs
     * 2. 回退到 /v2/api-docs
     * 
     * @param serviceName 服务名
     * @param instance 服务实例
     * @return 探测到的端点信息
     */
    public Mono<DocEndpointInfo> probeEndpoint(String serviceName, ServiceInstance instance) {
        // 如果有手动配置，直接返回
        String configuredEndpoint = properties.getDiscover().getServiceEndpoint(serviceName);
        if (configuredEndpoint != null && !configuredEndpoint.isEmpty()) {
            DocEndpointInfo info = new DocEndpointInfo(configuredEndpoint, inferVersionFromPath(configuredEndpoint));
            endpointCache.put(serviceName, info);
            return Mono.just(info);
        }
        
        // 如果禁用自动探测，使用默认配置
        if (!properties.getDiscover().isAutoProbeEndpoint()) {
            DocEndpointInfo info = new DocEndpointInfo(
                    properties.getDiscover().getDocPath(),
                    properties.getDiscover().getVersion()
            );
            endpointCache.put(serviceName, info);
            return Mono.just(info);
        }
        
        String baseUrl = String.format("http://%s:%d", instance.getHost(), instance.getPort());
        Duration timeout = Duration.ofMillis(properties.getDiscover().getProbeTimeout());
        
        // 优先尝试 /v3/api-docs
        return isEndpointAvailable(baseUrl, OAS3_ENDPOINT, timeout)
                .flatMap(available -> {
                    if (available) {
                        log.info("服务 {} 探测到 OpenAPI 3 端点: {}", serviceName, OAS3_ENDPOINT);
                        DocEndpointInfo info = DocEndpointInfo.openApi3(OAS3_ENDPOINT);
                        endpointCache.put(serviceName, info);
                        return Mono.just(info);
                    }
                    
                    // 回退到 /v2/api-docs
                    return isEndpointAvailable(baseUrl, SWAGGER2_ENDPOINT, timeout)
                            .map(swagger2Available -> {
                                if (swagger2Available) {
                                    log.info("服务 {} 探测到 Swagger 2 端点: {}", serviceName, SWAGGER2_ENDPOINT);
                                    DocEndpointInfo info = DocEndpointInfo.swagger2(SWAGGER2_ENDPOINT);
                                    endpointCache.put(serviceName, info);
                                    return info;
                                }
                                
                                // 都不可用，使用默认配置
                                log.warn("服务 {} 无法探测到文档端点，使用默认配置", serviceName);
                                DocEndpointInfo info = new DocEndpointInfo(
                                        properties.getDiscover().getDocPath(),
                                        properties.getDiscover().getVersion()
                                );
                                endpointCache.put(serviceName, info);
                                return info;
                            });
                });
    }
    
    /**
     * 检查端点是否可用
     */
    private Mono<Boolean> isEndpointAvailable(String baseUrl, String endpoint, Duration timeout) {
        String url = baseUrl + endpoint;
        
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(timeout)
                .map(body -> {
                    // 检查响应是否像 API 文档
                    return body != null && !body.isEmpty() && 
                           (body.contains("\"openapi\"") || body.contains("\"swagger\"") || body.contains("\"paths\""));
                })
                .onErrorReturn(false)
                .defaultIfEmpty(false);
    }
    
    /**
     * 根据路径推断 OpenAPI 版本
     */
    private OpenApiVersion inferVersionFromPath(String path) {
        if (path == null) {
            return OpenApiVersion.OpenAPI3;
        }
        if (path.contains("/v2/") || path.endsWith("/v2")) {
            return OpenApiVersion.Swagger2;
        }
        return OpenApiVersion.OpenAPI3;
    }
    
    /**
     * 清除服务的端点缓存
     */
    public void clearCache(String serviceName) {
        endpointCache.remove(serviceName);
        log.debug("已清除服务 {} 的端点缓存", serviceName);
    }
    
    /**
     * 清除所有端点缓存
     */
    public void clearAllCache() {
        endpointCache.clear();
        log.info("已清除所有端点缓存");
    }
    
    /**
     * 获取缓存的端点信息
     */
    public DocEndpointInfo getCachedEndpoint(String serviceName) {
        return endpointCache.get(serviceName);
    }
    
    /**
     * 检查是否有缓存的端点信息
     */
    public boolean hasCachedEndpoint(String serviceName) {
        return endpointCache.containsKey(serviceName);
    }
}

