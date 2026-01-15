/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.controller;

import com.github.zhanglongjun.knife4j.aggregator.config.Knife4jAggregatorProperties;
import com.github.zhanglongjun.knife4j.aggregator.enums.AggregatorStrategy;
import com.github.zhanglongjun.knife4j.aggregator.service.AggregatorDiscoveryService;
import com.github.zhanglongjun.knife4j.aggregator.spec.v2.OpenAPI2Resource;
import com.github.zhanglongjun.knife4j.aggregator.spec.v3.OpenAPI3Response;
import com.github.zhanglongjun.knife4j.aggregator.utils.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * OpenAPI 端点
 * 
 * 提供 /v3/api-docs/swagger-config 端点供 Knife4j UI 获取服务列表
 */
@RestController
public class OpenAPIEndpoint {
    
    private static final Logger log = LoggerFactory.getLogger(OpenAPIEndpoint.class);
    
    private final Knife4jAggregatorProperties properties;
    
    @Autowired(required = false)
    private AggregatorDiscoveryService discoveryService;
    
    public OpenAPIEndpoint(Knife4jAggregatorProperties properties) {
        this.properties = properties;
    }
    
    /**
     * OpenAPI Group Endpoint
     */
    @GetMapping("/v3/api-docs/swagger-config")
    public Mono<ResponseEntity<OpenAPI3Response>> swaggerConfig(ServerHttpRequest request) {
        OpenAPI3Response response = new OpenAPI3Response();
        final String basePath = PathUtils.getDefaultContextPath(request);
        log.debug("base-path:{}", basePath);
        
        response.setConfigUrl("/v3/api-docs/swagger-config");
        response.setOauth2RedirectUrl(this.properties.getDiscover().getOas3().getOauth2RedirectUrl());
        response.setValidatorUrl(this.properties.getDiscover().getOas3().getValidatorUrl());
        response.setTagsSorter(this.properties.getTagsSorter().name());
        response.setOperationsSorter(this.properties.getOperationsSorter().name());
        
        log.debug("forward-path:{}", basePath);
        
        if (properties.getStrategy() == AggregatorStrategy.MANUAL) {
            log.debug("manual strategy.");
            List<Object> sortedSet = new LinkedList<>();
            List<Knife4jAggregatorProperties.Router> routers = properties.getRoutes();
            if (routers != null && !routers.isEmpty()) {
                routers.sort(Comparator.comparing(Knife4jAggregatorProperties.Router::getOrder));
                for (Knife4jAggregatorProperties.Router router : routers) {
                    OpenAPI2Resource copyRouter = new OpenAPI2Resource(router);
                    copyRouter.setUrl(PathUtils.append(basePath, copyRouter.getUrl()));
                    copyRouter.setContextPath(PathUtils.processContextPath(PathUtils.append(basePath, copyRouter.getContextPath())));
                    log.debug("api-resources:{}", copyRouter);
                    sortedSet.add(copyRouter);
                }
                response.setUrls(sortedSet);
            }
        } else {
            log.debug("discover strategy.");
            if (discoveryService != null) {
                response.setUrls(discoveryService.getResources(basePath));
            }
        }
        return Mono.just(ResponseEntity.ok().body(response));
    }
}
