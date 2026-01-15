package com.github.zhanglongjun.knife4j.aggregator.service;

import com.github.zhanglongjun.knife4j.aggregator.config.ErrorCollectorProxyProperties;
import com.github.zhanglongjun.knife4j.aggregator.config.Knife4jAggregatorProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 错误收集器代理服务
 * 负责调用微服务的内部接口获取完整错误详情
 * 开箱即用模式：无需 Token 验证
 */
@Slf4j
@Service
public class ErrorCollectorProxyService {

    private final ErrorCollectorProxyProperties properties;
    private final Knife4jAggregatorProperties aggregatorProperties;
    private final DiscoveryClient discoveryClient;
    private final WebClient webClient;

    public ErrorCollectorProxyService(
            ErrorCollectorProxyProperties properties,
            Knife4jAggregatorProperties aggregatorProperties,
            DiscoveryClient discoveryClient) {
        this.properties = properties;
        this.aggregatorProperties = aggregatorProperties;
        this.discoveryClient = discoveryClient;
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 从微服务获取完整错误详情
     *
     * @param serviceName 服务名称
     * @param errorId     错误 ID
     * @return 完整错误详情
     */
    public Mono<Map<String, Object>> fetchErrorDetail(String serviceName, String errorId) {
        if (!properties.isEnabled()) {
            return Mono.error(new IllegalStateException("Error collector proxy is disabled"));
        }

        return getServiceBaseUrl(serviceName)
                .flatMap(baseUrl -> {
                    String contextPath = aggregatorProperties.getDiscover().getContextPath(serviceName);
                    String url = buildErrorDetailUrl(baseUrl, contextPath, errorId);

                    log.debug("Fetching error detail from: {}", url);

                    return webClient.get()
                            .uri(url)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .timeout(properties.getTimeout())
                            .map(response -> (Map<String, Object>) response)
                            .doOnSuccess(detail -> log.debug("Fetched error detail: errorId={}", errorId))
                            .doOnError(e -> log.error("Failed to fetch error detail: errorId={}", errorId, e));
                });
    }

    /**
     * 获取微服务的错误收集器状态
     */
    public Mono<Map<String, Object>> getServiceStatus(String serviceName) {
        if (!properties.isEnabled()) {
            return Mono.error(new IllegalStateException("Error collector proxy is disabled"));
        }

        return getServiceBaseUrl(serviceName)
                .flatMap(baseUrl -> {
                    String contextPath = aggregatorProperties.getDiscover().getContextPath(serviceName);
                    String url = buildStatusUrl(baseUrl, contextPath);

                    return webClient.get()
                            .uri(url)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .timeout(properties.getTimeout())
                            .map(response -> (Map<String, Object>) response);
                });
    }

    /**
     * 获取服务实例的基础 URL
     */
    private Mono<String> getServiceBaseUrl(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);

        if (instances == null || instances.isEmpty()) {
            return Mono.error(new IllegalStateException(
                    "No available instances for service: " + serviceName));
        }

        // 使用第一个实例
        ServiceInstance instance = instances.get(0);
        String baseUrl = String.format("http://%s:%d", instance.getHost(), instance.getPort());

        return Mono.just(baseUrl);
    }

    /**
     * 构建错误详情 URL
     */
    private String buildErrorDetailUrl(String baseUrl, String contextPath, String errorId) {
        StringBuilder url = new StringBuilder(baseUrl);
        if (contextPath != null && !contextPath.isEmpty()) {
            url.append(contextPath);
        }
        url.append(properties.getInternalApiPrefix());
        url.append("/errors/").append(errorId);
        return url.toString();
    }

    /**
     * 构建状态 URL
     */
    private String buildStatusUrl(String baseUrl, String contextPath) {
        StringBuilder url = new StringBuilder(baseUrl);
        if (contextPath != null && !contextPath.isEmpty()) {
            url.append(contextPath);
        }
        url.append(properties.getInternalApiPrefix());
        url.append("/status");
        return url.toString();
    }

}
