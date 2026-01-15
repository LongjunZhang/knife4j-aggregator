/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.filter;

import com.github.zhanglongjun.knife4j.aggregator.config.Knife4jAggregatorProperties;
import com.github.zhanglongjun.knife4j.aggregator.service.ApiPathCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * API 版本校验过滤器
 * 
 * 拦截业务 API 请求，校验请求的接口是否存在于用户当前查看的 API 文档版本中。
 * 
 * 工作流程：
 * 1. 检查请求头中是否包含 X-Doc-Version 和 X-Doc-Service
 * 2. 如果不包含，放行请求（兼容无版本控制的场景）
 * 3. 如果包含，从 MongoDB 加载对应版本的 API 文档
 * 4. 解析文档中的 paths，校验请求路径是否存在
 * 5. 不存在则返回 404 错误
 */
public class ApiVersionValidationFilter implements WebFilter, Ordered {
    
    private static final Logger log = LoggerFactory.getLogger(ApiVersionValidationFilter.class);
    
    /** 版本号请求头 */
    public static final String HEADER_DOC_VERSION = "X-Doc-Version";
    
    /** 服务名请求头 */
    public static final String HEADER_DOC_SERVICE = "X-Doc-Service";
    
    /** 
     * 匹配业务 API 路径的正则表达式
     * 排除 API 文档路径（/xxx/v2/api-docs, /xxx/v3/api-docs）
     * 匹配 /{contextPath}/{businessPath}
     */
    private static final Pattern API_DOC_PATTERN = Pattern.compile("^/[\\w-]+/v[23]/api-docs.*$");
    
    /** 静态资源路径正则 */
    private static final Pattern STATIC_RESOURCE_PATTERN = Pattern.compile(".*\\.(js|css|html|ico|png|jpg|gif|svg|woff|woff2|ttf|eot)$");
    
    private final ApiPathCacheService pathCacheService;
    private final Knife4jAggregatorProperties properties;
    
    public ApiVersionValidationFilter(
            ApiPathCacheService pathCacheService,
            Knife4jAggregatorProperties properties) {
        this.pathCacheService = pathCacheService;
        this.properties = properties;
        log.info("API 版本校验过滤器已初始化");
    }
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();
        
        // 1. 排除 API 文档请求
        if (API_DOC_PATTERN.matcher(path).matches()) {
            return chain.filter(exchange);
        }
        
        // 2. 排除静态资源
        if (STATIC_RESOURCE_PATTERN.matcher(path).matches()) {
            return chain.filter(exchange);
        }
        
        // 3. 排除非业务路径（如 /api/docs, /v3/api-docs/swagger-config 等）
        if (path.startsWith("/api/") || path.startsWith("/v3/api-docs") || 
            path.startsWith("/doc.html") || path.startsWith("/webjars/")) {
            return chain.filter(exchange);
        }
        
        // 4. 获取版本信息请求头
        HttpHeaders headers = request.getHeaders();
        String docVersion = headers.getFirst(HEADER_DOC_VERSION);
        String docServiceRaw = headers.getFirst(HEADER_DOC_SERVICE);
        
        // 5. 如果没有版本信息，放行请求（兼容无版本控制的场景）
        if (docVersion == null || docVersion.isEmpty() || 
            docServiceRaw == null || docServiceRaw.isEmpty()) {
            return chain.filter(exchange);
        }
        
        // 6. 规范化服务名：将 context-path 段（如 orderService）转为 serviceId（如 order-service）
        String docService = normalizeServiceName(docServiceRaw);
        
        // 7. 解析服务名和业务路径
        String contextPath = properties.getDiscover().getContextPath(docService);
        String businessPath = extractBusinessPath(path, contextPath);
        
        if (businessPath == null || businessPath.isEmpty()) {
            // 无法解析业务路径，放行
            return chain.filter(exchange);
        }
        
        log.debug("版本校验: service(raw)={}, service(norm)={}, version={}, path={}, method={}", 
                docServiceRaw, docService, docVersion, businessPath, method);
        
        // 8. 校验路径是否存在于指定版本
        return pathCacheService.isPathExistsInVersion(docService, docVersion, businessPath, method)
                .flatMap(exists -> {
                    if (exists) {
                        return chain.filter(exchange);
                    }

                    // 兼容 Swagger2 -> OpenAPI3 转换场景：
                    // 某些服务（通常是 /v2/api-docs）落库的 paths 已经包含 contextPath 前缀（如 /messageService/message），
                    // 但这里 businessPath 已经剥离成 /message，导致匹配失败。此处做一次兜底匹配。
                    String normalizedCtx = contextPath;
                    if (normalizedCtx != null && !normalizedCtx.isEmpty() && !normalizedCtx.startsWith("/")) {
                        normalizedCtx = "/" + normalizedCtx;
                    }
                    final String prefixedPath;
                    if (normalizedCtx != null && !normalizedCtx.isEmpty() && !"/".equals(normalizedCtx)) {
                        String bp = businessPath.startsWith("/") ? businessPath : ("/" + businessPath);
                        prefixedPath = normalizedCtx.endsWith("/") ? (normalizedCtx.substring(0, normalizedCtx.length() - 1) + bp) : (normalizedCtx + bp);
                    } else {
                        prefixedPath = businessPath;
                    }

                    if (!prefixedPath.equals(businessPath)) {
                        return pathCacheService.isPathExistsInVersion(docService, docVersion, prefixedPath, method)
                                .flatMap(exists2 -> {
                                    if (exists2) {
                                        log.debug("版本校验兜底命中: service(raw)={}, service(norm)={}, version={}, path={} -> {}, method={}",
                                                docServiceRaw, docService, docVersion, businessPath, prefixedPath, method);
                                        return chain.filter(exchange);
                                    }
                                    log.warn("API 路径不存在于版本 {} 中: service(raw)={}, service(norm)={}, path={}, method={}",
                                            docVersion, docServiceRaw, docService, businessPath, method);
                                    return writeNotFoundResponse(exchange, docServiceRaw, docVersion, businessPath, method);
                                });
                    }

                    log.warn("API 路径不存在于版本 {} 中: service(raw)={}, service(norm)={}, path={}, method={}",
                            docVersion, docServiceRaw, docService, businessPath, method);
                    return writeNotFoundResponse(exchange, docServiceRaw, docVersion, businessPath, method);
                })
                .onErrorResume(e -> {
                    // 发生错误时，记录日志但放行请求（不阻断业务）
                    log.error("版本校验失败，放行请求: service(raw)={}, service(norm)={}, version={}, path={}", 
                            docServiceRaw, docService, docVersion, businessPath, e);
                    return chain.filter(exchange);
                });
    }
    
    /**
     * 将传入的 serviceName 规范化为服务 ID（根据 context-path 反查）
     * 例如：orderService -> order-service, messageService -> message-service
     * 
     * @param pathServiceName 前端传入的服务名（通常是 context-path 段）
     * @return 规范化后的服务 ID
     */
    private String normalizeServiceName(String pathServiceName) {
        if (pathServiceName == null || pathServiceName.isEmpty()) {
            return pathServiceName;
        }
        Map<String, String> ctxMap = properties.getDiscover().getServiceContextPaths();
        if (ctxMap != null) {
            String pathSeg = pathServiceName.startsWith("/") ? pathServiceName : "/" + pathServiceName;
            for (Map.Entry<String, String> e : ctxMap.entrySet()) {
                String ctx = e.getValue();
                if (ctx == null || ctx.isEmpty()) {
                    continue;
                }
                String normCtx = ctx.startsWith("/") ? ctx : "/" + ctx;
                if (normCtx.equalsIgnoreCase(pathSeg)) {
                    return e.getKey(); // 返回 serviceId（如 order-service）
                }
            }
        }
        return pathServiceName; // 回退：如果本来就是 order-service，也能正常使用
    }
    
    /**
     * 从完整请求路径中提取业务路径
     * 
     * @param fullPath 完整请求路径，如 /userService/user/123
     * @param contextPath 服务 context-path，如 /userService
     * @return 业务路径，如 /user/123
     */
    private String extractBusinessPath(String fullPath, String contextPath) {
        if (contextPath == null || contextPath.isEmpty()) {
            // 没有配置 contextPath，尝试从路径中提取
            // 假设格式为 /{serviceName}/{businessPath}
            int firstSlash = fullPath.indexOf('/', 1);
            if (firstSlash > 0) {
                return fullPath.substring(firstSlash);
            }
            return fullPath;
        }
        
        // 确保 contextPath 以 / 开头
        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }
        
        // 移除 contextPath 前缀
        if (fullPath.startsWith(contextPath)) {
            return fullPath.substring(contextPath.length());
        }
        
        return fullPath;
    }
    
    /**
     * 写入 404 响应
     */
    private Mono<Void> writeNotFoundResponse(ServerWebExchange exchange, 
                                              String serviceName, String version, 
                                              String path, String method) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.NOT_FOUND);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        String errorJson = String.format(
            "{\"error\":\"API Not Found In Version\","
            + "\"message\":\"接口 [%s] %s 在版本 %s 中不存在\","
            + "\"service\":\"%s\","
            + "\"version\":\"%s\","
            + "\"path\":\"%s\","
            + "\"method\":\"%s\","
            + "\"hint\":\"请切换到正确的版本后再调试该接口\"}",
            method, path, version, serviceName, version, path, method
        );
        
        byte[] bytes = errorJson.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        
        return response.writeWith(Mono.just(buffer));
    }
    
    @Override
    public int getOrder() {
        // 在 ApiDocsCacheFilter 之后执行，但在 Gateway 路由之前
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}

