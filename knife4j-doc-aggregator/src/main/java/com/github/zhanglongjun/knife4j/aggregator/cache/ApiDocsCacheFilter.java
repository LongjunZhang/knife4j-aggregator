/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.zhanglongjun.knife4j.aggregator.config.Knife4jAggregatorProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API 文档缓存过滤器
 * 
 * 拦截路径：/{serviceName}/v3/api-docs 或 /{serviceName}/v2/api-docs
 */
public class ApiDocsCacheFilter implements WebFilter, Ordered {
    
    private static final Logger log = LoggerFactory.getLogger(ApiDocsCacheFilter.class);
    
    /** 匹配 API 文档路径的正则表达式 */
    private static final Pattern DOC_PATTERN = Pattern.compile("^/([\\w-]+)/v[23]/api-docs.*$");
    
    private final ApiDocsCacheManager cacheManager;
    private final Knife4jAggregatorProperties properties;
    private final ObjectMapper objectMapper;
    
    public ApiDocsCacheFilter(
            ApiDocsCacheManager cacheManager, 
            Knife4jAggregatorProperties properties) {
        this.cacheManager = cacheManager;
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        log.info("API 文档缓存过滤器已初始化");
    }
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!properties.getCache().isEnabled()) {
            return chain.filter(exchange);
        }
        
        String path = exchange.getRequest().getPath().value();
        Matcher matcher = DOC_PATTERN.matcher(path);
        
        if (!matcher.matches()) {
            return chain.filter(exchange);
        }
        
        String serviceName = matcher.group(1);
        String canonicalServiceName = normalizeServiceName(serviceName);
        log.info("拦截到文档请求，服务: {} (canonical: {}), 路径: {}", serviceName, canonicalServiceName, path);
        
        if (!cacheManager.hasCached(canonicalServiceName)) {
            log.warn("服务 {} 没有缓存数据，尝试从后端获取", canonicalServiceName);
        }
        
        return cacheManager.getApiDoc(canonicalServiceName)
            .flatMap(content -> {
                String rewrittenContent = rewriteApiDoc(canonicalServiceName, content);
                log.info("返回服务 {} 的缓存文档", canonicalServiceName);
                return writeResponse(exchange, rewrittenContent);
            })
            .onErrorResume(ServiceUnavailableException.class, e -> {
                log.warn("服务 {} 不可用且无可用缓存", canonicalServiceName);
                return writeErrorResponse(exchange, e);
            })
            .onErrorResume(Exception.class, e -> {
                log.error("处理文档请求时发生错误，服务: {}", canonicalServiceName, e);
                return writeErrorResponse(exchange, 
                    new ServiceUnavailableException(canonicalServiceName, e));
            });
    }
    
    private Mono<Void> writeResponse(ServerWebExchange exchange, String content) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        
        return response.writeWith(Mono.just(buffer));
    }
    
    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, ServiceUnavailableException e) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        String errorJson = String.format(
            "{\"error\":\"Service Unavailable\",\"message\":\"服务 %s 不可用且无可用缓存\",\"service\":\"%s\"}",
            e.getServiceName(), e.getServiceName()
        );
        
        byte[] bytes = errorJson.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 将路径中的 serviceName 规范化为服务 ID（根据 context-path 反查）
     */
    private String normalizeServiceName(String pathServiceName) {
        Map<String, String> ctxMap = properties.getDiscover().getServiceContextPaths();
        if (ctxMap != null) {
            String pathSegment = pathServiceName.startsWith("/") ? pathServiceName : "/" + pathServiceName;
            for (Map.Entry<String, String> entry : ctxMap.entrySet()) {
                String ctx = entry.getValue();
                if (ctx == null || ctx.isEmpty()) {
                    continue;
                }
                String normalizedCtx = ctx.startsWith("/") ? ctx : "/" + ctx;
                if (normalizedCtx.equalsIgnoreCase(pathSegment)) {
                    return entry.getKey();
                }
            }
        }
        return pathServiceName;
    }
    
    /**
     * 改写 API 文档，添加正确的 contextPath/basePath
     */
    private String rewriteApiDoc(String serviceName, String content) {
        String contextPath = properties.getDiscover().getContextPath(serviceName);
        
        if (contextPath == null || contextPath.isEmpty()) {
            return content;
        }
        
        try {
            JsonNode rootNode = objectMapper.readTree(content);
            
            if (rootNode.has("openapi")) {
                return rewriteOpenApi3(rootNode, contextPath);
            } else if (rootNode.has("swagger")) {
                return rewriteSwagger2(rootNode, contextPath);
            } else {
                return content;
            }
        } catch (Exception e) {
            log.error("改写服务 {} 的文档失败: {}", serviceName, e.getMessage());
            return content;
        }
    }
    
    private String rewriteOpenApi3(JsonNode rootNode, String contextPath) throws Exception {
        ObjectNode root = (ObjectNode) rootNode;
        
        JsonNode existingServers = root.get("servers");
        
        if (existingServers != null && existingServers.isArray() && existingServers.size() > 0) {
            JsonNode firstServer = existingServers.get(0);
            if (firstServer != null && firstServer.has("url")) {
                String existingUrl = firstServer.get("url").asText();
                if (existingUrl != null && !existingUrl.isEmpty() && !"/".equals(existingUrl)) {
                    return objectMapper.writeValueAsString(root);
                }
            }
        }
        
        ArrayNode serversArray = objectMapper.createArrayNode();
        ObjectNode serverNode = objectMapper.createObjectNode();
        serverNode.put("url", contextPath);
        serverNode.put("description", "Aggregator 代理路径");
        serversArray.add(serverNode);
        
        root.set("servers", serversArray);
        
        return objectMapper.writeValueAsString(root);
    }
    
    private String rewriteSwagger2(JsonNode rootNode, String contextPath) throws Exception {
        ObjectNode root = (ObjectNode) rootNode;
        root.put("basePath", contextPath);
        return objectMapper.writeValueAsString(root);
    }
    
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

