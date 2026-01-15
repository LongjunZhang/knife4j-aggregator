/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * API 路径缓存服务
 * 
 * 负责：
 * - 解析 OpenAPI 文档中的 paths
 * - 缓存每个服务版本的 API 路径列表
 * - 验证请求路径是否存在于指定版本中
 */
@Service
public class ApiPathCacheService {
    
    private static final Logger log = LoggerFactory.getLogger(ApiPathCacheService.class);
    
    private final DocVersionService versionService;
    private final ObjectMapper objectMapper;
    
    /**
     * 缓存结构：serviceName:version -> Set<ApiPathInfo>
     * ApiPathInfo 包含 path pattern 和 HTTP methods
     */
    private final Map<String, Set<ApiPathInfo>> pathCache = new ConcurrentHashMap<>();
    
    public ApiPathCacheService(DocVersionService versionService) {
        this.versionService = versionService;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 验证请求路径是否存在于指定版本的 API 文档中
     * 
     * @param serviceName 服务名
     * @param version     版本号
     * @param requestPath 请求路径（不含服务前缀，如 /user/123）
     * @param httpMethod  HTTP 方法
     * @return Mono<Boolean> 是否存在
     */
    public Mono<Boolean> isPathExistsInVersion(String serviceName, String version, 
                                                String requestPath, String httpMethod) {
        String cacheKey = buildCacheKey(serviceName, version);
        
        // 先检查缓存
        Set<ApiPathInfo> cachedPaths = pathCache.get(cacheKey);
        if (cachedPaths != null) {
            boolean exists = matchPath(cachedPaths, requestPath, httpMethod);
            log.debug("路径校验[缓存命中] service={}, version={}, path={}, method={}, exists={}", 
                    serviceName, version, requestPath, httpMethod, exists);
            return Mono.just(exists);
        }
        
        // 缓存未命中，从数据库加载
        return loadAndCachePaths(serviceName, version)
                .map(paths -> {
                    boolean exists = matchPath(paths, requestPath, httpMethod);
                    log.debug("路径校验[从DB加载] service={}, version={}, path={}, method={}, exists={}", 
                            serviceName, version, requestPath, httpMethod, exists);
                    return exists;
                })
                .defaultIfEmpty(false);
    }
    
    /**
     * 从数据库加载版本文档并缓存路径
     */
    private Mono<Set<ApiPathInfo>> loadAndCachePaths(String serviceName, String version) {
        return versionService.getVersion(serviceName, version)
                .map(docVersion -> {
                    Set<ApiPathInfo> paths = parsePathsFromDoc(docVersion.getContent());
                    String cacheKey = buildCacheKey(serviceName, version);
                    pathCache.put(cacheKey, paths);
                    log.info("已缓存服务 {} 版本 {} 的 API 路径，共 {} 个", 
                            serviceName, version, paths.size());
                    return paths;
                });
    }
    
    /**
     * 解析 OpenAPI 文档中的所有 API 路径
     */
    private Set<ApiPathInfo> parsePathsFromDoc(String content) {
        Set<ApiPathInfo> paths = new HashSet<>();
        
        try {
            JsonNode root = objectMapper.readTree(content);
            JsonNode pathsNode = root.get("paths");
            
            if (pathsNode == null || !pathsNode.isObject()) {
                return paths;
            }
            
            Iterator<String> pathIterator = pathsNode.fieldNames();
            while (pathIterator.hasNext()) {
                String path = pathIterator.next();
                JsonNode pathNode = pathsNode.get(path);
                
                if (pathNode != null && pathNode.isObject()) {
                    Set<String> methods = new HashSet<>();
                    Iterator<String> methodIterator = pathNode.fieldNames();
                    
                    while (methodIterator.hasNext()) {
                        String method = methodIterator.next();
                        if (isHttpMethod(method)) {
                            methods.add(method.toUpperCase());
                        }
                    }
                    
                    if (!methods.isEmpty()) {
                        paths.add(new ApiPathInfo(path, methods));
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析 API 路径失败", e);
        }
        
        return paths;
    }
    
    /**
     * 匹配请求路径是否在 API 路径列表中
     * 支持 Path Parameter 模式匹配，如 /user/{id} 匹配 /user/123
     */
    private boolean matchPath(Set<ApiPathInfo> paths, String requestPath, String httpMethod) {
        String normalizedMethod = httpMethod.toUpperCase();
        
        for (ApiPathInfo pathInfo : paths) {
            if (pathInfo.methods.contains(normalizedMethod) && 
                matchPathPattern(pathInfo.path, requestPath)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 匹配路径模式
     * 将 OpenAPI 路径模式（如 /user/{id}）转换为正则表达式并匹配
     */
    private boolean matchPathPattern(String pathPattern, String requestPath) {
        // 精确匹配
        if (pathPattern.equals(requestPath)) {
            return true;
        }
        
        // 转换 {param} 为正则表达式
        // /user/{id} -> ^/user/[^/]+$
        // /order/{orderId}/item/{itemId} -> ^/order/[^/]+/item/[^/]+$
        String regex = "^" + pathPattern
                .replaceAll("\\{[^}]+\\}", "[^/]+")  // 将 {xxx} 替换为匹配非斜杠字符
                .replace("/", "\\/")                   // 转义斜杠
                + "$";
        
        try {
            Pattern pattern = Pattern.compile(regex);
            return pattern.matcher(requestPath).matches();
        } catch (Exception e) {
            log.warn("路径模式匹配失败: pattern={}, request={}", pathPattern, requestPath);
            return false;
        }
    }
    
    /**
     * 判断是否为 HTTP 方法
     */
    private boolean isHttpMethod(String method) {
        String upper = method.toUpperCase();
        return "GET".equals(upper) || "POST".equals(upper) || "PUT".equals(upper) 
                || "DELETE".equals(upper) || "PATCH".equals(upper) || "HEAD".equals(upper) 
                || "OPTIONS".equals(upper) || "TRACE".equals(upper);
    }
    
    /**
     * 构建缓存 Key
     */
    private String buildCacheKey(String serviceName, String version) {
        return serviceName + ":" + version;
    }
    
    /**
     * 清除指定服务版本的缓存
     */
    public void evictCache(String serviceName, String version) {
        String cacheKey = buildCacheKey(serviceName, version);
        pathCache.remove(cacheKey);
        log.info("已清除服务 {} 版本 {} 的路径缓存", serviceName, version);
    }
    
    /**
     * 清除指定服务的所有版本缓存
     */
    public void evictAllVersions(String serviceName) {
        pathCache.keySet().removeIf(key -> key.startsWith(serviceName + ":"));
        log.info("已清除服务 {} 的所有版本路径缓存", serviceName);
    }
    
    /**
     * 清除所有缓存
     */
    public void evictAll() {
        pathCache.clear();
        log.info("已清除所有路径缓存");
    }
    
    /**
     * 获取缓存统计信息
     */
    public Map<String, Integer> getCacheStats() {
        Map<String, Integer> stats = new HashMap<>();
        pathCache.forEach((key, paths) -> stats.put(key, paths.size()));
        return stats;
    }
    
    /**
     * API 路径信息
     */
    private static class ApiPathInfo {
        final String path;
        final Set<String> methods;
        
        ApiPathInfo(String path, Set<String> methods) {
            this.path = path;
            this.methods = methods;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ApiPathInfo that = (ApiPathInfo) o;
            return Objects.equals(path, that.path);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(path);
        }
    }
}

