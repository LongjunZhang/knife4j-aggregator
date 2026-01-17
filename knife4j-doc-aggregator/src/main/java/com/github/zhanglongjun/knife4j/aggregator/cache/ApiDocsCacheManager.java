/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.cache;

import com.github.zhanglongjun.knife4j.aggregator.config.Knife4jAggregatorProperties;
import com.github.zhanglongjun.knife4j.aggregator.model.DocEndpointInfo;
import com.github.zhanglongjun.knife4j.aggregator.service.DocEndpointProbeService;
import com.github.zhanglongjun.knife4j.aggregator.service.DocPersistenceService;
import com.github.zhanglongjun.knife4j.aggregator.service.Swagger2ToOAS3Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API 文档缓存管理器
 * 
 * 负责管理微服务 API 文档的缓存
 * 
 * M3 改造：集成 MongoDB 持久化，调整读取优先级：
 * 1. MemoryCache 未硬过期 → 返回
 * 2. Mongo 最新版本存在 → 返回并回填 MemoryCache
 * 3. fetch 微服务成功 → 写 Mongo 版本 + Diff → 回填 cache → 返回
 * 4. fetch 失败但 Mongo 有旧版本 → 返回旧版本
 * 5. 无回退 → 503
 */
public class ApiDocsCacheManager {
    
    private static final Logger log = LoggerFactory.getLogger(ApiDocsCacheManager.class);
    
    /** 文档缓存：serviceName -> CachedApiDoc */
    private final ConcurrentHashMap<String, CachedApiDoc> cache = new ConcurrentHashMap<>();
    
    private final WebClient webClient;
    private final DiscoveryClient discoveryClient;
    private final Knife4jAggregatorProperties properties;
    
    /** Swagger 2.0 到 OpenAPI 3.0 转换器 */
    private final Swagger2ToOAS3Converter converter;
    
    /** 文档端点探测服务（可选依赖） */
    private DocEndpointProbeService endpointProbeService;
    
    /** 文档持久化服务（可选依赖，M3 阶段启用） */
    private DocPersistenceService persistenceService;
    
    public ApiDocsCacheManager(
            DiscoveryClient discoveryClient,
            Knife4jAggregatorProperties properties,
            Swagger2ToOAS3Converter converter) {
        this.discoveryClient = discoveryClient;
        this.properties = properties;
        this.converter = converter;
        this.webClient = WebClient.builder().build();
        log.info("API 文档缓存管理器已初始化，Swagger 转换: 已启用");
    }
    
    /**
     * 设置端点探测服务（延迟注入，避免循环依赖）
     */
    public void setEndpointProbeService(DocEndpointProbeService endpointProbeService) {
        this.endpointProbeService = endpointProbeService;
        log.info("端点探测服务已注入到缓存管理器");
    }
    
    /**
     * 设置持久化服务（延迟注入，避免循环依赖）
     */
    public void setPersistenceService(DocPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
        log.info("文档持久化服务已注入到缓存管理器");
    }
    
    /**
     * 检查持久化服务是否可用
     */
    private boolean isPersistenceEnabled() {
        return persistenceService != null;
    }
    
    /**
     * 获取服务的 API 文档
     * 
     * 读取优先级（M3 调整后）：
     * 1. MemoryCache 未硬过期 → 返回
     * 2. Mongo 最新版本存在 → 返回并回填 MemoryCache
     * 3. fetch 微服务成功 → 写 Mongo 版本 + Diff → 回填 cache → 返回
     * 4. fetch 失败但 Mongo 有旧版本 → 返回旧版本
     * 5. 无回退 → 503
     */
    public Mono<String> getApiDoc(String serviceName) {
        CachedApiDoc cached = cache.get(serviceName);
        
        // 优先级 1: MemoryCache 未硬过期
        if (cached != null && !cached.isHardExpired()) {
            log.debug("优先级1: 缓存命中，服务: {}，缓存年龄: {}s", serviceName, cached.getAgeInSeconds());
            return Mono.just(cached.getContent());
        }
        
        // 优先级 2: 尝试从 MongoDB 获取最新版本
        if (isPersistenceEnabled()) {
            return getFromMongoOrFetch(serviceName, cached);
        }
        
        // 无持久化服务，走原来的逻辑
        return fetchAndCache(serviceName, cached);
    }
    
    /**
     * 从 MongoDB 获取或从微服务拉取
     */
    private Mono<String> getFromMongoOrFetch(String serviceName, CachedApiDoc cached) {
        return persistenceService.getLatestContent(serviceName)
                .flatMap(mongoContent -> {
                    // 优先级 2: MongoDB 有最新版本，回填缓存并返回
                    log.debug("优先级2: 从 MongoDB 获取文档，服务: {}", serviceName);
                    updateCache(serviceName, mongoContent);
                    
                    // 异步尝试从微服务拉取最新版本（后台更新）
                    asyncFetchAndPersist(serviceName);
                    
                    return Mono.just(mongoContent);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // MongoDB 无数据，尝试从微服务拉取
                    log.debug("MongoDB 无数据，尝试从微服务拉取，服务: {}", serviceName);
                    return fetchPersistAndCache(serviceName, cached);
                }));
    }
    
    /**
     * 从微服务拉取、持久化并缓存（优先级 3）
     */
    private Mono<String> fetchPersistAndCache(String serviceName, CachedApiDoc cached) {
        return fetchFromService(serviceName)
                .flatMap(content -> {
                    log.info("优先级3: 成功从服务 {} 拉取文档", serviceName);
                    
                    // 更新内存缓存
                    updateCache(serviceName, content);
                    
                    // 持久化到 MongoDB
                    if (isPersistenceEnabled()) {
                        return persistenceService.persistDocument(serviceName, content)
                                .doOnNext(result -> {
                                    if (result.hasNewVersion()) {
                                        log.info("服务 {} 文档已持久化，新版本: v{}，变更: {}", 
                                                serviceName, result.getNewVersion(), result.getChangeSummary());
                                    } else {
                                        log.debug("服务 {} 文档内容未变化", serviceName);
                                    }
                                })
                                .doOnError(e -> log.error("持久化服务 {} 文档失败: {}", serviceName, e.getMessage()))
                                .thenReturn(content)
                                .onErrorReturn(content); // 持久化失败不影响返回
                    }
                    
                    return Mono.just(content);
                })
                .onErrorResume(e -> handleFetchError(serviceName, cached, e));
    }
    
    /**
     * 处理拉取失败的情况（优先级 4 和 5）
     */
    private Mono<String> handleFetchError(String serviceName, CachedApiDoc cached, Throwable e) {
        log.warn("从服务 {} 拉取文档失败: {}", serviceName, e.getMessage());
        
        // 优先级 4: 尝试从 MongoDB 获取旧版本
        if (isPersistenceEnabled()) {
            return persistenceService.getLatestContent(serviceName)
                    .doOnNext(content -> {
                        log.info("优先级4: 服务 {} 不可用，使用 MongoDB 持久化文档", serviceName);
                        updateCache(serviceName, content);
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        // MongoDB 也没有，检查内存缓存（即使过期）
                        if (cached != null) {
                            log.info("优先级4: 服务 {} 不可用，使用过期的内存缓存", serviceName);
                            return Mono.just(cached.getContent());
                        }
                        // 优先级 5: 无任何回退
                        log.error("优先级5: 服务 {} 无任何可用文档回退", serviceName);
                        return Mono.error(new ServiceUnavailableException(serviceName, e));
                    }));
        }
        
        // 无持久化服务，检查内存缓存
        if (cached != null) {
            log.info("服务 {} 不可用，使用缓存文档", serviceName);
            return Mono.just(cached.getContent());
        }
        
        return Mono.error(new ServiceUnavailableException(serviceName, e));
    }
    
    /**
     * 原有的拉取并缓存逻辑（无持久化时使用）
     */
    private Mono<String> fetchAndCache(String serviceName, CachedApiDoc cached) {
        return fetchFromService(serviceName)
                .doOnNext(content -> {
                    updateCache(serviceName, content);
                    log.info("成功从服务 {} 拉取并更新文档缓存", serviceName);
                })
                .onErrorResume(e -> {
                    log.warn("从服务 {} 拉取文档失败: {}", serviceName, e.getMessage());
                    
                    if (cached != null && !cached.isHardExpired()) {
                        log.info("服务 {} 不可用，使用缓存文档", serviceName);
                        return Mono.just(cached.getContent());
                    }
                    
                    return Mono.error(new ServiceUnavailableException(serviceName, e));
                });
    }
    
    /**
     * 异步从微服务拉取并持久化（后台更新）
     */
    private void asyncFetchAndPersist(String serviceName) {
        fetchFromService(serviceName)
                .flatMap(content -> {
                    updateCache(serviceName, content);
                    
                    if (isPersistenceEnabled()) {
                        return persistenceService.persistDocument(serviceName, content)
                                .doOnNext(result -> {
                                    if (result.hasNewVersion()) {
                                        log.info("后台更新: 服务 {} 新版本 v{}，变更: {}", 
                                                serviceName, result.getNewVersion(), result.getChangeSummary());
                                    }
                                });
                    }
                    return Mono.empty();
                })
                .subscribe(
                        result -> {},
                        error -> log.debug("后台更新服务 {} 失败: {}", serviceName, error.getMessage())
                );
    }
    
    /**
     * 从微服务拉取 API 文档
     * 
     * 流程：
     * 1. 根据端点探测服务获取文档路径
     * 2. 拉取原始文档
     * 3. 如果是 Swagger 2.0，自动转换为 OpenAPI 3.0
     */
    private Mono<String> fetchFromService(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        
        if (instances.isEmpty()) {
            return Mono.error(new ServiceUnavailableException(serviceName));
        }
        
        ServiceInstance instance = instances.get(0);
        DocEndpointInfo endpointInfo = getEndpointInfo(serviceName);
        String docUrl = buildDocUrl(instance, endpointInfo);
        
        log.debug("从 {} 拉取 API 文档，端点类型: {}", docUrl, endpointInfo.getVersion());
        
        return webClient.get()
                .uri(docUrl)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(properties.getCache().getFetchTimeout()))
                .map(content -> convertIfNeeded(serviceName, content));
    }
    
    /**
     * 获取服务的端点信息
     */
    private DocEndpointInfo getEndpointInfo(String serviceName) {
        if (endpointProbeService != null) {
            return endpointProbeService.getEndpointInfo(serviceName);
        }
        // 回退到默认配置
        return new DocEndpointInfo(
                properties.getDiscover().getDocPath(),
                properties.getDiscover().getVersion()
        );
    }
    
    /**
     * 如果是 Swagger 2.0，转换为 OpenAPI 3.0，并删除 paths 中的 contextPath 前缀
     * 
     * 策略 A：统一存储口径，paths 不带 contextPath 前缀
     * 例如：/messageService/message -> /message
     */
    private String convertIfNeeded(String serviceName, String content) {
        if (converter == null) {
            return content;
        }
        
        // 获取服务的 contextPath
        String contextPath = properties.getDiscover().getContextPath(serviceName);
        
        if (converter.isSwagger2(content)) {
            log.info("服务 {} 返回 Swagger 2.0 文档，自动转换为 OpenAPI 3.0 并规范化 paths", serviceName);
            // 使用带 contextPath 参数的转换方法，会自动删除 paths 中的 contextPath 前缀
            String converted = converter.convert(content, contextPath);
            log.debug("服务 {} 文档转换完成，原始长度: {}, 转换后: {}", 
                    serviceName, content.length(), converted.length());
            return converted;
        }
        
        // 已经是 OpenAPI 3.0，规范化并删除 contextPath 前缀（如果有）
        if (converter.isOpenApi3(content)) {
            String normalized = converter.normalizeJson(content);
            // 对于 OpenAPI 3.0 文档，也需要检查并删除 paths 中的 contextPath 前缀
            if (contextPath != null && !contextPath.isEmpty() && !"/".equals(contextPath)) {
                normalized = converter.stripContextPathFromPaths(normalized, contextPath);
            }
            return normalized;
        }
        
        return content;
    }
    
    /**
     * 构建文档 URL
     */
    private String buildDocUrl(ServiceInstance instance, DocEndpointInfo endpointInfo) {
        String scheme = instance.isSecure() ? "https" : "http";
        String host = instance.getHost();
        int port = instance.getPort();
        String serviceName = instance.getServiceId();
        String contextPath = properties.getDiscover().getContextPath(serviceName);
        String docPath = endpointInfo.getDocPath();
        
        return String.format("%s://%s:%d%s%s", scheme, host, port, contextPath, docPath);
    }
    
    /**
     * 更新缓存
     */
    private void updateCache(String serviceName, String content) {
        CachedApiDoc cached = cache.get(serviceName);
        
        if (cached != null) {
            cached.updateContent(content);
        } else {
            cached = new CachedApiDoc(serviceName, content, properties.getCache().getHardTtl());
            cache.put(serviceName, cached);
        }
    }
    
    /**
     * 刷新指定服务的文档缓存
     */
    public void refreshServiceDoc(String serviceName) {
        log.info("刷新服务 {} 的文档缓存...", serviceName);
        
        try {
            String content = fetchFromService(serviceName)
                    .block(Duration.ofMillis(properties.getCache().getFetchTimeout()));
            
            if (content != null) {
                updateCache(serviceName, content);
                
                // 同步持久化
                if (isPersistenceEnabled()) {
                    persistenceService.persistDocument(serviceName, content)
                            .doOnNext(result -> {
                                if (result.hasNewVersion()) {
                                    log.info("服务 {} 文档刷新并持久化，新版本: v{}", 
                                            serviceName, result.getNewVersion());
                                }
                            })
                            .subscribe(
                                    result -> {},
                                    error -> log.warn("服务 {} 文档持久化失败: {}", serviceName, error.getMessage())
                            );
                }
                
                log.info("服务 {} 文档缓存刷新成功", serviceName);
            }
        } catch (Exception e) {
            log.warn("刷新服务 {} 文档缓存失败: {}", serviceName, e.getMessage());
        }
    }
    
    /**
     * 异步刷新指定服务的文档缓存
     */
    public void refreshServiceDocAsync(String serviceName) {
        log.debug("异步刷新服务 {} 的文档缓存...", serviceName);
        
        fetchFromService(serviceName)
                .doOnNext(content -> {
                    updateCache(serviceName, content);
                    log.info("服务 {} 文档缓存异步刷新成功", serviceName);
                    
                    // 异步持久化
                    if (isPersistenceEnabled()) {
                        persistenceService.persistDocument(serviceName, content)
                                .subscribe(
                                        result -> {},
                                        error -> log.debug("服务 {} 异步持久化失败: {}", serviceName, error.getMessage())
                                );
                    }
                })
                .subscribe(
                        content -> {},
                        error -> log.warn("异步刷新服务 {} 文档缓存失败: {}", serviceName, error.getMessage())
                );
    }
    
    /**
     * 刷新所有已缓存服务的文档
     */
    public void refreshAllDocs() {
        log.info("刷新所有服务的文档缓存，共 {} 个服务", cache.size());
        cache.keySet().forEach(this::refreshServiceDocAsync);
    }
    
    /**
     * 标记服务下线
     */
    public void markServiceOffline(String serviceName) {
        CachedApiDoc cached = cache.get(serviceName);
        if (cached != null) {
            cached.markOffline();
            log.info("服务 {} 已标记为下线，保留缓存文档", serviceName);
        }
    }
    
    /**
     * 标记服务上线
     */
    public void markServiceOnline(String serviceName) {
        CachedApiDoc cached = cache.get(serviceName);
        if (cached != null) {
            cached.markOnline();
        }
    }
    
    /**
     * 预热缓存
     */
    public void warmUpCache(Set<String> serviceNames) {
        if (!properties.getCache().isWarmUpOnStartup()) {
            log.info("缓存预热已禁用，跳过");
            return;
        }
        
        log.info("开始预热 API 文档缓存，共 {} 个服务...", serviceNames.size());
        
        for (String serviceName : serviceNames) {
            try {
                refreshServiceDoc(serviceName);
            } catch (Exception e) {
                log.warn("预热服务 {} 文档缓存失败: {}", serviceName, e.getMessage());
            }
        }
        
        log.info("API 文档缓存预热完成");
    }
    
    /**
     * 获取缓存状态
     */
    public Map<String, Object> getCacheStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", properties.getCache().isEnabled());
        status.put("totalCached", cache.size());
        status.put("hardTtlMs", properties.getCache().getHardTtl());
        status.put("persistenceEnabled", isPersistenceEnabled());
        
        List<Map<String, Object>> services = new ArrayList<>();
        for (Map.Entry<String, CachedApiDoc> entry : cache.entrySet()) {
            CachedApiDoc doc = entry.getValue();
            Map<String, Object> serviceStatus = new LinkedHashMap<>();
            serviceStatus.put("serviceName", doc.getServiceName());
            serviceStatus.put("contentLength", doc.getContent() != null ? doc.getContent().length() : 0);
            serviceStatus.put("cachedAt", doc.getCachedAt());
            serviceStatus.put("ageInSeconds", doc.getAgeInSeconds());
            serviceStatus.put("hardExpired", doc.isHardExpired());
            serviceStatus.put("serviceOnline", doc.isServiceOnline());
            services.add(serviceStatus);
        }
        status.put("services", services);
        
        return status;
    }
    
    /**
     * 检查是否有指定服务的缓存
     */
    public boolean hasCached(String serviceName) {
        return cache.containsKey(serviceName);
    }
    
    /**
     * 获取缓存的服务名列表
     */
    public Set<String> getCachedServiceNames() {
        return new HashSet<>(cache.keySet());
    }
}
